package org.sofwerx.torgi.ogc.sos;

import android.content.Context;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import org.sofwerx.torgi.Config;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

/**
 * A service to process the outside-TORGI comms in SOS format
 */
public class SosService implements SosMessageListener {
    private HandlerThread sosThread; //the MANET itself runs on this thread where possible
    private Handler handler;
    private SosMessageListener listener;
    private String serverURL;
    private Context context;
    private SosSensor sosSensor;
    private SosIpcTransceiver transceiver;
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private AtomicBoolean sendSensorReadingWhenReady = new AtomicBoolean(false);
    private boolean ipcBroadcast;

    /**
     * Creates a new SosService
     * @param context
     * @param sosSensor
     * @param sosServerURL
     * @param turnOn true == the service will start running immediately; false measn the service will initiate but will not start sending/receiving
     */
    public SosService(Context context, SosSensor sosSensor, String sosServerURL, final boolean turnOn) {
        if (context == null)
            Log.e(SosIpcTransceiver.TAG,"SosService should not be passed a null context");
        this.context = context;
        this.sosSensor = sosSensor;
        if (context instanceof SosMessageListener)
            listener = (SosMessageListener)context;
        this.serverURL = sosServerURL;
        sosThread = new HandlerThread("SosService") {
            @Override
            protected void onLooperPrepared() {
                Log.i(SosIpcTransceiver.TAG,"SosService started");
                handler = new Handler(sosThread.getLooper());
                setOn(turnOn);
            }
        };
        sosThread.start();
        ipcBroadcast = Config.isIpcBroadcastEnabled(context);
    }

    /**
     * Toggles between on (active/running/transmitting and receiving) and off (paused)
     * @param on true = on/active/transmitting and receiving
     */
    public void setOn(boolean on) {
        if (on != isRunning.get()) {
            if (on) {
                if (context != null) {
                    Log.i(SosIpcTransceiver.TAG,"SosService turned ON");
                    transceiver = new SosIpcTransceiver(this);
                    IntentFilter intentFilter = new IntentFilter(SosIpcTransceiver.ACTION_SOS);
                    context.registerReceiver(transceiver, intentFilter);
                    broadcastSensorReadings();
                }
            } else {
                Log.i(SosIpcTransceiver.TAG,"SosService turned OFF");
                if (transceiver != null) {
                    context.unregisterReceiver(transceiver);
                    transceiver = null;
                }
            }
            isRunning.set(on);
        }
    }

    public void broadcastSensorReadings() {
        Log.d(SosIpcTransceiver.TAG,"Trying to broadcast sensor readings");
        if (sosSensor != null) {
            if (sosSensor.isReadyToSendResults()) {
                OperationInsertResult operation = new OperationInsertResult(sosSensor);
                if (operation.isValid())
                    broadcast(operation);
            } else {
                registerSensor();
                sendSensorReadingWhenReady.set(true);
            }
        } else {
            if (listener != null)
                listener.onSosError("Cannot send sensor readings as no sensor has been set. Call setSosSensor first");
            Log.d(SosIpcTransceiver.TAG, "...but SosSensor is null");
        }
    }

    private void broadcast(AbstractSosOperation operation) {
        if (handler != null) {
            handler.post(() -> {
                Log.d(SosIpcTransceiver.TAG,"Broadcasting "+operation.getClass().getName());
                if (isRunning.get()) {
                    if (ipcBroadcast) {
                        Log.d(SosIpcTransceiver.TAG,"Broadcasting SOS operation over IPC");
                        try {
                            transceiver.broadcast(context, operation);
                        } catch (SosException e) {
                            Log.e(SosIpcTransceiver.TAG,"Unable to broadcast SOS operation: "+e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    if ((serverURL != null) && Config.isSosBroadcastEnabled(context)) {
                        Log.d(SosIpcTransceiver.TAG,"Broadcasting SOS operation to "+serverURL);
                        try {
                            String result = HttpHelper.post(serverURL,SosIpcTransceiver.toString(operation.toXML()));
                            AbstractSosOperation responseOperation = AbstractSosOperation.newFromXmlString(result);
                            if (responseOperation == null) {
                                Log.e(SosIpcTransceiver.TAG,"Unable to parse response from server: "+result);
                                if (listener != null)
                                    listener.onSosError("Unexpected response from SOS server");
                            } else
                                onSosOperationReceived(responseOperation);
                        } catch (IOException | TransformerException | ParserConfigurationException e) {
                            if (listener != null)
                                listener.onSosError("Unable to connect to SOS server: "+e.getMessage());
                        }
                    }
                } else {
                    if (listener != null)
                        listener.onSosError("Cannot send SOS messages as the SosService has not be enabled (call setOn())");
                }
            });
        } else {
            Log.d(SosIpcTransceiver.TAG,"...but handler is not yet ready");
            if (listener != null)
                listener.onSosError("Unable to broadcast yet as the thread and handler SosService need are not yet ready");
        }
    }

    public void shutdown() {
        Log.i(SosIpcTransceiver.TAG,"Shutting down SosServer");
        setOn(false);
        if (sosThread != null) {
            if (handler != null)
                handler.removeCallbacksAndMessages(null);
            sosThread.quitSafely();
            sosThread = null;
            handler = null;
        }
        if (context != null)
            context = null;
    }

    /**
     * Registers the sensor with the SOS server if not already done
     */
    public void registerSensor() {
        Log.d(SosIpcTransceiver.TAG,"Trying to register sensor");
        if (handler != null) {
            handler.post(() -> {
                if (sosSensor.getAssignedProcedure() == null) {
                    if (sosSensor.isReadyToRegisterSensor()) {
                        Log.d(SosIpcTransceiver.TAG,"Sensor has all required info to register; contacting server...");
                        OperationInsertSensor operation = new OperationInsertSensor(sosSensor);
                        broadcast(operation);
                    } else
                        Log.w(SosIpcTransceiver.TAG, "sosSensor does not yet have enough information to register with the SOS server");
                } else if (sosSensor.getAssignedTemplate() == null) {
                    if (sosSensor.isReadyToRegisterResultTemplate()) {
                        OperationInsertResultTemplate operation = new OperationInsertResultTemplate(sosSensor);
                        broadcast(operation);
                    } else
                        Log.w(SosIpcTransceiver.TAG, "sosSensor does not yet have enough information to register a result template with the SOS server");
                } else
                    Log.i(SosIpcTransceiver.TAG, "registerSensor ignored as sosSensor already appears to be registered with the SOS server");
            });
        } else
            Log.d(SosIpcTransceiver.TAG,"... but handler not ready yet");
    }

    public SosMessageListener getListener() { return listener; }
    public void setListener(SosMessageListener listener) { this.listener = listener; }
    public SosSensor getSosSensor() { return sosSensor; }
    public void setSosServerUrl(String serverUrl) { this.serverURL = serverUrl; }

    /**
     * Sets the current sosSensor; if the sosSensor already has enough information
     * to register with the SOS server, start that process. The process will keep going
     * if the sosSensor already has enough information to register a result template
     * @param sosSensor
     */
    public void setSosSensor(SosSensor sosSensor) {
        this.sosSensor = sosSensor;
        if (sosSensor != null)
            registerSensor();
    }

    @Override
    public void onSosOperationReceived(AbstractSosOperation operation) {
        if (operation instanceof OperationInsertSensorResponse) {
            if (sosSensor != null) {
                OperationInsertSensorResponse response = (OperationInsertSensorResponse)operation;
                if ((sosSensor.getUniqueId() != null) && (sosSensor.getUniqueId().equalsIgnoreCase(response.getAssignedProcedure()))) {
                    sosSensor.setAssignedProcedure(response.getAssignedProcedure());
                    sosSensor.setAssignedOffering(response.getAssignedOffering());
                    if (sendSensorReadingWhenReady.get())
                        registerSensor();
                } else
                    Log.i(SosIpcTransceiver.TAG,"InsertSensorResponse received, but it was for sensor "+response.getAssignedProcedure());
            }
        } else if (operation instanceof OperationInsertResultTemplateResponse) {
            if (sosSensor != null) {
                OperationInsertResultTemplateResponse response = (OperationInsertResultTemplateResponse) operation;
                if ((response.getAcceptedTemplate() != null) && (sosSensor.getAssignedProcedure() != null)
                        && response.getAcceptedTemplate().startsWith(sosSensor.getAssignedProcedure())) {
                    sosSensor.setAssignedTemplate(response.getAcceptedTemplate());
                    if (sendSensorReadingWhenReady.get())
                        broadcastSensorReadings();
                    if (listener != null)
                        listener.onSosConfigurationSuccess();
                } else
                    Log.i(SosIpcTransceiver.TAG,"InsertResultTemplateResponse received, but it was for template "+response.getAcceptedTemplate());
            }
        }
        if (listener != null)
            listener.onSosOperationReceived(operation);
    }

    @Override
    public void onSosError(String message) {
        Log.e(SosIpcTransceiver.TAG,message);
        if (listener != null)
            listener.onSosError(message);
    }

    @Override
    public void onSosConfigurationSuccess() {
        Log.i(SosIpcTransceiver.TAG,"Successfully connected to SOS Server");
        if (listener != null)
            listener.onSosConfigurationSuccess();
    }

    public void setIpcBroadcast(boolean broadcast) {
        ipcBroadcast = broadcast;
    }
}
