package org.sofwerx.torgi.ogc.sosv2;

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
    protected void parse(Element element) {
        //TODO
    }

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
        Element template = doc.createElement("sos:template");
        insertResult.appendChild(template);
        template.setTextContent(sosSensor.getAssignedTemplate()); //FIXME placeholder
        Element resultValues = doc.createElement("sos:resultValues");
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
                value = measurement.getValue();
                out.append(value);
            }
        }

        resultValues.setTextContent(out.toString());
        return doc;
    }
}