package org.sofwerx.torgi.ogc.sos;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.StringWriter;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

public class OperationInsertResult extends AbstractSosOperation {
    public final static String NAMESPACE = "InsertResult";
    private SosSensor sosSensor;

    public OperationInsertResult(SosSensor sosSensor) { this.sosSensor = sosSensor; }

    @Override
    public void parse(Element insertResult) {
        if (insertResult == null) {
            Log.d(SosIpcTransceiver.TAG,"OperationInsertResult unable to parse xml element because it is null");
            return;
        }
        if (sosSensor == null) {
            Log.e(SosIpcTransceiver.TAG,"OperationInsertResult cannot be parsed without a sosSensor assigned");
            return;
        }
        try {
            //No error checking is put in here aside from the exception catch since this whole structure should fail if any one element is not correct
            //since we're only parsing one particular set of formats, we're ignoring server, version, etc attributes
            Element template = (Element)insertResult.getElementsByTagName(TAG_TEMPLATE).item(0);
            sosSensor.setAssignedTemplate(template.getTextContent());
            Element resultValues = (Element)insertResult.getElementsByTagName(TAG_RESULT_VALUES).item(0);
            String values = resultValues.getTextContent();
            String value[] = values.split(OperationInsertResultTemplate.FIELD_SEPERATOR);
            ArrayList<SensorMeasurement> sensorMeasurements = sosSensor.getSensorMeasurements();
            int valueIndex = 0;
            for (int i=0;i< sensorMeasurements.size();i++) {
                if (sensorMeasurements.get(i) instanceof SensorMeasurementLocation) {
                    double lat = Double.parseDouble(value[valueIndex]);
                    double lng = Double.parseDouble(value[valueIndex+1]);
                    double alt = Double.parseDouble(value[valueIndex+2]);
                    valueIndex += 3;
                    ((SensorMeasurementLocation)sensorMeasurements.get(i)).setLocation(lat,lng,alt);
                } else if (sensorMeasurements.get(i) instanceof SensorMeasurementTime) {
                    sensorMeasurements.get(i).parseLong(value[valueIndex]);
                    valueIndex++;
                } else {
                    sensorMeasurements.get(i).setValue(value[valueIndex]);
                    valueIndex++;
                }
            }
        } catch (Exception e) {
            Log.e(SosIpcTransceiver.TAG,"OperationInsertResult parsing error: "+e.getMessage());
        }
    }

    private final static String TAG_TEMPLATE = "sos:template";
    private final static String TAG_RESULT_VALUES = "sos:resultValues";

    @Override
    public Document toXML() throws ParserConfigurationException {
        if (sosSensor == null) {
            Log.e(SosIpcTransceiver.TAG,"SosSensor cannot be null for InsertResult operation");
            return null;
        }
        if (sosSensor.getAssignedTemplate() == null) {
            Log.e(SosIpcTransceiver.TAG,"assigned template cannot be null for InsertResult operation");
            return null;
        }
        ArrayList<SensorMeasurement> measurments = sosSensor.getSensorMeasurements();
        if ((measurments == null) || measurments.isEmpty()) {
            Log.e(SosIpcTransceiver.TAG,"SosSensor must have SensorMeasurements for InsertResult operation");
            return null;
        }
        Document doc = super.toXML();
        Element insertResult = doc.createElement(NAMESPACE);
        doc.appendChild(insertResult);
        insertResult.setAttribute("xmlns:sos","http://www.opengis.net/sos/2.0");
        insertResult.setAttribute("service","SOS");
        insertResult.setAttribute("version","2.0.0");
        Element template = doc.createElement(TAG_TEMPLATE);
        insertResult.appendChild(template);
        template.setTextContent(sosSensor.getAssignedTemplate());
        Element resultValues = doc.createElement(TAG_RESULT_VALUES);
        insertResult.appendChild(resultValues);
        StringWriter out = new StringWriter();

        boolean first = true;
        for (SensorMeasurement measurement:measurments) {
            if (first)
                first = false;
            else
                out.append(OperationInsertResultTemplate.FIELD_SEPERATOR);
            String value;
            if (measurement instanceof SensorMeasurementLocation) {
                double[] values = ((SensorMeasurementLocation)measurement).getValues();
                if ((values == null) || (values.length != 3)) {
                    Log.e(SosIpcTransceiver.TAG,"SensorMeasurementLocation.getValues not correct size (this should never happen)");
                    return null;
                }
                //favor providing 0,0,0 over NaN data to SOS
                double lat = values[SensorMeasurementLocation.FIELD_LATITUDE];
                if (Double.isNaN(lat))
                    lat = 0d;
                double lng = values[SensorMeasurementLocation.FIELD_LONGITUDE];
                if (Double.isNaN(lng))
                    lng = 0d;
                double alt = values[SensorMeasurementLocation.FIELD_ALTITUDE];
                if (Double.isNaN(alt))
                    alt = 0d;
                out.append(Double.toString(lat));
                out.append(OperationInsertResultTemplate.FIELD_SEPERATOR);
                out.append(Double.toString(lng));
                out.append(OperationInsertResultTemplate.FIELD_SEPERATOR);
                out.append(Double.toString(alt));
            } else {
                value = measurement.toString();
                out.append(value);
            }
        }

        resultValues.setTextContent(out.toString());
        return doc;
    }

    /**
     * Are all of the data fields complete for this operation
     * @return false == one or more measurement is missing a value
     */
    public boolean isValid() {
        if (sosSensor == null)
            return false;
        ArrayList<SensorMeasurement> measurements = sosSensor.getSensorMeasurements();
        if ((measurements == null) || measurements.isEmpty())
            return false;
        for (SensorMeasurement measurement:measurements) {
            if ((measurement instanceof SensorMeasurementLocation) || (measurement instanceof SensorMeasurementTime))
                continue;
            if (measurement.getValue() == null)
                return false;
        }
        return true;
    }
}