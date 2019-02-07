package org.sofwerx.torgi.ogc.sos;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

public class OperationInsertResultTemplate extends AbstractSosOperation {
    public final static String NAMESPACE = "InsertResultTemplate";
    public final static String FIELD_SEPERATOR = ",";
    public final static String BLOCK_SEPERATOR = "@@";
    private SosSensor sosSensor;

    public OperationInsertResultTemplate() { super(); }

    public OperationInsertResultTemplate(SosSensor sosSensor) {
        super();
        this.sosSensor = sosSensor;
    }


    @Override
    public boolean isValid() {
        return (sosSensor != null) && sosSensor.isReadyToRegisterResultTemplate();
    }

    @Override
    public void parse(Element element) {
        if (element == null)
            return;
        try {
            if (sosSensor == null)
                sosSensor = new SosSensor();
            //No error checking is put in here aside from the exception catch since this whole structure should fail if any one element is not correct
            Element proposedTemplate = (Element)element.getElementsByTagName(TAG_PROPOSED_TEMPLATE).item(0);
            Element resultTemplate = (Element)proposedTemplate.getElementsByTagName(TAG_RESULT_TEMPLATE).item(0);
            Element offering = (Element)resultTemplate.getElementsByTagName(TAG_OFFERING).item(0);
            sosSensor.setAssignedOffering(offering.getTextContent());
            Element resultStructure = (Element)resultTemplate.getElementsByTagName(TAG_RESULT_STRUCTURE).item(0);
            Element dataRecord = (Element)resultStructure.getElementsByTagName(TAG_DATA_RECORD).item(0);
            ArrayList<SensorResultTemplateField> fields = SensorTimeResultTemplateField.getTemplateFields(dataRecord);
            if ((fields != null) && !fields.isEmpty()) {
                for (SensorResultTemplateField field:fields) {
                    SensorMeasurement sensorMeasurement = SensorMeasurement.newFromResultTemplateField(field);
                    sosSensor.addMeasurement(sensorMeasurement);
                }
            }
        } catch (Exception e) {
            Log.e(SosIpcTransceiver.TAG,"OperationInsertResultTemplate parsing error: "+e.getMessage());
        }
        /*
        Element dataRecord = doc.createElement(TAG_DATA_RECORD);
        resultStructure.appendChild(dataRecord);
        for (SensorMeasurement measurement:measurements) {
         */




        //TODO
    }

    private final static String TAG_PROPOSED_TEMPLATE = "sos:proposedTemplate";
    private final static String TAG_RESULT_TEMPLATE = "sos:ResultTemplate";
    private final static String TAG_OFFERING = "sos:offering";
    private final static String TAG_RESULT_STRUCTURE = "sos:resultStructure";
    private final static String TAG_DATA_RECORD = "swe:DataRecord";
    private final static String TAG_RESULT_ENCODING = "sos:resultEncoding";
    private final static String TAG_TEXT_ENCODING = "swe:TextEncoding";
    private final static String NAME_TOKEN_SEPARATOR = "tokenSeparator";
    private final static String NAME_BLOCK_SEPARATOR = "blockSeparator";

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
        insertResultTemplate.setAttribute("xmlns:xlink","http://www.w3.org/1999/xlink");
        insertResultTemplate.setAttribute("service","SOS");
        insertResultTemplate.setAttribute("version","2.0.0");
        Element proposedTemplate = doc.createElement(TAG_PROPOSED_TEMPLATE);
        insertResultTemplate.appendChild(proposedTemplate);
        Element resultTemplate = doc.createElement(TAG_RESULT_TEMPLATE);
        proposedTemplate.appendChild(resultTemplate);
        Element offering = doc.createElement(TAG_OFFERING);
        resultTemplate.appendChild(offering);
        offering.setTextContent(sosSensor.getAssignedOffering());
        Element resultStructure = doc.createElement(TAG_RESULT_STRUCTURE);
        resultTemplate.appendChild(resultStructure);
        Element dataRecord = doc.createElement(TAG_DATA_RECORD);
        resultStructure.appendChild(dataRecord);
        for (SensorMeasurement measurement:measurements) {
            if (measurement.getFormat() != null)
                measurement.getFormat().addToElement(doc,dataRecord);
            else
                Log.e(SosIpcTransceiver.TAG,"SensorMeasurement does not have an assigned format and will be dropped from InsertResultTemplate operation");
        }
        Element resultEncoding = doc.createElement(TAG_RESULT_ENCODING);
        resultTemplate.appendChild(resultEncoding);
        Element textEncoding = doc.createElement(TAG_TEXT_ENCODING);
        resultEncoding.appendChild(textEncoding);
        textEncoding.setAttribute(NAME_TOKEN_SEPARATOR,FIELD_SEPERATOR);
        textEncoding.setAttribute(NAME_BLOCK_SEPARATOR,BLOCK_SEPERATOR);
        return doc;
    }

    public SosSensor getSosSensor() { return sosSensor; }
}