package org.sofwerx.torgi.ogc.sos;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

public class OperationInsertResultTemplate extends AbstractSosOperation {
    public final static String NAMESPACE = "InsertResultTemplate";
    public final static String FIELD_SEPERATOR = ",";
    private SosSensor sosSensor;

    public OperationInsertResultTemplate() { super(); }

    public OperationInsertResultTemplate(SosSensor sosSensor) {
        super();
        this.sosSensor = sosSensor;
    }


    @Override
    protected void parse(Element element) {
        //TODO
    }

    @Override
    public Document toXML() throws ParserConfigurationException {
        if (sosSensor == null) {
            Log.e(SosIpcTransceiver.TAG,"SosSensor cannot be null in InsertResultTemplate operation");
            return null;
        }
        if (sosSensor.getAssignedOffering() == null) {
            Log.e(SosIpcTransceiver.TAG,"SosSensor assigned offering cannot be null in InsertResultTemplate operation");
            return null;
        }
        ArrayList<SensorMeasurement> measurements = sosSensor.getSensorMeasurements();
        if ((measurements == null) || measurements.isEmpty()) {
            Log.e(SosIpcTransceiver.TAG,"SosSensor needs to have some measurement templates to report in InsertResultTemplate operation");
            return null;
        }
        if (!sosSensor.isMeasurmentsFieldsValid()) {
            Log.e(SosIpcTransceiver.TAG,"not all SensorMeasurements have valid field descriptions so they cannot be used in InsertResultTemplate operation");
            return null;
        }
        Document doc = super.toXML();
        Element insertResultTemplate = doc.createElement(NAMESPACE);
        doc.appendChild(insertResultTemplate);
        insertResultTemplate.setAttribute("xmlns:sos","http://www.opengis.net/sos/2.0");
        insertResultTemplate.setAttribute("xmlns:swe","http://www.opengis.net/swe/2.0");
        insertResultTemplate.setAttribute("xmlns:om","http://www.opengis.net/om/2.0");
        insertResultTemplate.setAttribute("xmlns:gml","http://www.opengis.net/gml/3.2");
        insertResultTemplate.setAttribute("service","SOS");
        insertResultTemplate.setAttribute("version","2.0.0");
        Element proposedTemplate = doc.createElement("sos:proposedTemplate");
        insertResultTemplate.appendChild(proposedTemplate);
        Element resultTemplate = doc.createElement("sos:ResultTemplate");
        proposedTemplate.appendChild(resultTemplate);
        Element offering = doc.createElement("sos:offering");
        resultTemplate.appendChild(offering);
        offering.setTextContent(sosSensor.getAssignedOffering());
        Element resultStructure = doc.createElement("sos:resultStructure");
        resultTemplate.appendChild(resultStructure);
        Element dataRecord = doc.createElement("swe:DataRecord");
        resultStructure.appendChild(dataRecord);
        for (SensorMeasurement measurement:measurements) {
            if (measurement.getFormat() != null)
                measurement.getFormat().addToElement(doc,dataRecord);
            else
                Log.e(SosIpcTransceiver.TAG,"SensorMeasurement does not have an assigned format and will be dropped from InsertResultTemplate operation");
        }
        Element resultEncoding = doc.createElement("sos:resultEncoding");
        resultTemplate.appendChild(resultEncoding);
        Element textEncoding = doc.createElement("swe:TextEncoding");
        resultEncoding.appendChild(textEncoding);
        textEncoding.setAttribute("tokenSeparator",FIELD_SEPERATOR);
        textEncoding.setAttribute("blockSeparator","@@");
        return doc;
    }
}