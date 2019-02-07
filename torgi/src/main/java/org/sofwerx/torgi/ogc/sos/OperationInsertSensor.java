package org.sofwerx.torgi.ogc.sos;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.ParserConfigurationException;

public class OperationInsertSensor extends AbstractSosOperation {
    public final static String NAMESPACE = "swes:InsertSensor";
    private SosSensor sosSensor;

    public OperationInsertSensor() {
        super();
        this.sosSensor = null;
    }

    public OperationInsertSensor(SosSensor sosSensor) {
        super();
        this.sosSensor = sosSensor;
    }


    @Override
    public boolean isValid() {
        return (sosSensor != null) && sosSensor.isReadyToRegisterSensor();
    }

    private final static String TAG_PROCEDURE_DESCRIPTION_FORMAT = "swes:procedureDescriptionFormat";
    private final static String TAG_PROCEDURE_DESCRIPTION = "swes:procedureDescription";
    private final static String TAG_PHYSICAL_SYSTEM = "sml:PhysicalSystem";
    private final static String NAME_ID = "gml:id";
    private final static String TAG_SML_IDENTIFICATION = "sml:identification";
    private final static String TAG_SML_IDENTIFIER_LIST = "sml:IdentifierList";
    private final static String TAG_SML_TERM = "sml:Term";
    private final static String TAG_GML_IDENTIFIER = "gml:identifier";
    private final static String TAG_SML_IDENTIFIER = "sml:identifier";
    private final static String NAME_CODESPACE = "codeSpace";
    private final static String NAME_VALUE = "sml:value";
    private final static String NAME_SML_LABEL = "sml:label";
    private final static String VALUE_CODESPACE = "uniqueID";
    private final static String TEXT_CONTENT_SHORTNAME = "shortName";
    private final static String TEXT_CONTENT_LONGNAME = "longName";

    @Override
    public void parse(Element insertSensor) {
        if (insertSensor == null)
            return;
        if (sosSensor == null)
            sosSensor = new SosSensor();
        try {
            //No error checking is put in here aside from the exception catch since this whole structure should fail if any one element is not correct
            //since we're only parsing one particular set of formats, we're ignoring server, version, etc attributes
            Element procedureDescription = (Element)insertSensor.getElementsByTagName(TAG_PROCEDURE_DESCRIPTION).item(0);
            Element physicalSystem = (Element)procedureDescription.getElementsByTagName(TAG_PHYSICAL_SYSTEM).item(0);
            sosSensor.setId(physicalSystem.getAttribute(NAME_ID));
            NodeList listIdentifier = physicalSystem.getElementsByTagName(TAG_GML_IDENTIFIER);
            if ((listIdentifier != null) && (listIdentifier.getLength() > 0)) { //this is an optional element
                Element identifier = (Element)listIdentifier.item(0);
                String uniqueId = identifier.getTextContent();
                if ((uniqueId != null) && (uniqueId.length() > 0))
                    sosSensor.setUniqueId(uniqueId);

            }
            NodeList listIdentification = physicalSystem.getElementsByTagName(TAG_SML_IDENTIFICATION);
            if ((listIdentification != null) && (listIdentification.getLength() > 0)) {
                Element identification = (Element)listIdentification.item(0);
                if (identification != null) {
                    NodeList listIdentifierList = identification.getElementsByTagName(TAG_SML_IDENTIFIER_LIST);
                    if ((listIdentifierList != null) && (listIdentifierList.getLength() > 0)) {
                        Element identifierList = (Element)listIdentifierList.item(0);
                        if (identifierList != null) {
                            NodeList listOfNames = identifierList.getElementsByTagName(TAG_SML_IDENTIFIER);
                            if ((listOfNames != null) && (listOfNames.getLength() > 0)) {
                                for (int i=0;i<listOfNames.getLength();i++) {
                                    try {
                                        Element term = (Element)(((Element)listOfNames.item(i)).getElementsByTagName(TAG_SML_TERM)).item(0);
                                        Element label = (Element)term.getElementsByTagName(NAME_SML_LABEL).item(0);
                                        Element value = (Element)term.getElementsByTagName(NAME_VALUE).item(0);
                                        if (TEXT_CONTENT_LONGNAME.equalsIgnoreCase(label.getTextContent()))
                                            sosSensor.setLongName(value.getTextContent());
                                        else if (TEXT_CONTENT_SHORTNAME.equalsIgnoreCase(label.getTextContent()))
                                            sosSensor.setShortName(value.getTextContent());
                                    } catch (Exception ignore) {
                                        //optional field
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(SosIpcTransceiver.TAG,"OperationInsertSensor parsing error: "+e.getMessage());
        }

            //TODO
    }

    @Override
    public Document toXML() throws ParserConfigurationException {
        if (sosSensor == null)
            return null;
        Document doc = super.toXML();
        Element insertSensor = doc.createElement(NAMESPACE);
        insertSensor.setAttribute("xmlns:swes","http://www.opengis.net/swes/2.0");
        insertSensor.setAttribute("xmlns:swe","http://www.opengis.net/swe/2.0");
        insertSensor.setAttribute("xmlns:sml","http://www.opengis.net/sensorml/2.0");
        insertSensor.setAttribute("xmlns:gml","http://www.opengis.net/gml/3.2");
        insertSensor.setAttribute("xmlns:sos","http://www.opengis.net/sos/2.0");
        insertSensor.setAttribute("xmlns:xlink","http://www.w3.org/1999/xlink");
        insertSensor.setAttribute("service","SOS");
        insertSensor.setAttribute("version","2.0.0");
        doc.appendChild(insertSensor);
        Element procedureDescriptionFormat = doc.createElement(TAG_PROCEDURE_DESCRIPTION_FORMAT);
        procedureDescriptionFormat.setTextContent("http://www.opengis.net/sensorml/2.0");
        insertSensor.appendChild(procedureDescriptionFormat);
        Element procedureDescription = doc.createElement(TAG_PROCEDURE_DESCRIPTION);
        insertSensor.appendChild(procedureDescription);
        Element physicalSystem = doc.createElement(TAG_PHYSICAL_SYSTEM);
        procedureDescription.appendChild(physicalSystem);
        if (sosSensor.getId() != null)
            physicalSystem.setAttribute(NAME_ID,sosSensor.getId());
        if (sosSensor.getUniqueId() != null) {
            Element identifier = doc.createElement(TAG_GML_IDENTIFIER);
            identifier.setAttribute(NAME_CODESPACE,VALUE_CODESPACE);
            identifier.setTextContent(sosSensor.getUniqueId());
            physicalSystem.appendChild(identifier);
        }
        Element identification = doc.createElement(TAG_SML_IDENTIFICATION);
        physicalSystem.appendChild(identification);
        Element identifierList = doc.createElement(TAG_SML_IDENTIFIER_LIST);
        identification.appendChild(identifierList);
        if (sosSensor.getLongName() != null) {
            Element identifier2 = doc.createElement(TAG_SML_IDENTIFIER);
            identifierList.appendChild(identifier2);
            Element term2 = doc.createElement(TAG_SML_TERM);
            identifier2.appendChild(term2);
            term2.setAttribute("definition", "urn:ogc:def:identifier:OGC:1.0:longName");
            Element label2 = doc.createElement(NAME_SML_LABEL);
            term2.appendChild(label2);
            label2.setTextContent(TEXT_CONTENT_LONGNAME);
            Element value2 = doc.createElement(NAME_VALUE);
            term2.appendChild(value2);
            value2.setTextContent(sosSensor.getLongName());
        }
        if (sosSensor.getShortName() != null) {
            Element identifier3 = doc.createElement(TAG_SML_IDENTIFIER);
            identifierList.appendChild(identifier3);
            Element term3 = doc.createElement(TAG_SML_TERM);
            identifier3.appendChild(term3);
            term3.setAttribute("definition", "urn:ogc:def:identifier:OGC:1.0:shortName");
            Element label3 = doc.createElement(NAME_SML_LABEL);
            term3.appendChild(label3);
            label3.setTextContent(TEXT_CONTENT_SHORTNAME);
            Element value3 = doc.createElement(NAME_VALUE);
            term3.appendChild(value3);
            value3.setTextContent(sosSensor.getShortName());
        }
        Element featuresOfInterest = doc.createElement("sml:featuresOfInterest");
        physicalSystem.appendChild(featuresOfInterest);
        Element featureList = doc.createElement("sml:FeatureList");
        featuresOfInterest.appendChild(featureList);
        featureList.setAttribute("definition", SosIpcTransceiver.SOFWERX_LINK_PLACEHOLDER); //TODO placeholder
        Element foiLabel = doc.createElement("swe:label");
        featureList.appendChild(foiLabel);
        foiLabel.setTextContent("featuresOfInterest");
        Element foiFeature = doc.createElement("sml:feature");
        featureList.appendChild(foiFeature);
        foiFeature.setAttribute("xlink:href", SosIpcTransceiver.SOFWERX_LINK_PLACEHOLDER); //TODO placeholder

        if (sosSensor.getUniqueId() != null) {
            Element observableProperty = doc.createElement("swes:observableProperty");
            insertSensor.appendChild(observableProperty);
            observableProperty.setTextContent(sosSensor.getUniqueId()+"_1");
        }

        Element metadata = doc.createElement("swes:metadata");
        insertSensor.appendChild(metadata);
        Element sosInsertionMetadata = doc.createElement("sos:SosInsertionMetadata");
        metadata.appendChild(sosInsertionMetadata);
        Element observationType1 = doc.createElement("sos:observationType");
        sosInsertionMetadata.appendChild(observationType1);
        observationType1.setTextContent("http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement");
        Element observationType2 = doc.createElement("sos:observationType");
        sosInsertionMetadata.appendChild(observationType2);
        observationType2.setTextContent("http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_CategoryObservation");
        Element observationType3 = doc.createElement("sos:observationType");
        sosInsertionMetadata.appendChild(observationType3);
        observationType3.setTextContent("http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_CountObservation");
        Element observationType4 = doc.createElement("sos:observationType");
        sosInsertionMetadata.appendChild(observationType4);
        observationType4.setTextContent("http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_TextObservation");
        Element observationType5 = doc.createElement("sos:observationType");
        sosInsertionMetadata.appendChild(observationType5);
        observationType5.setTextContent("http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_TruthObservation");
        Element observationType6 = doc.createElement("sos:observationType");
        sosInsertionMetadata.appendChild(observationType6);
        observationType6.setTextContent("http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_GeometryObservation");
        Element observationType7 = doc.createElement("sos:observationType");
        sosInsertionMetadata.appendChild(observationType7);
        observationType7.setTextContent("http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_SWEArrayObservation");
        Element featureOfInterestType = doc.createElement("sos:featureOfInterestType");
        sosInsertionMetadata.appendChild(featureOfInterestType);
        featureOfInterestType.setTextContent("http://www.opengis.net/def/samplingFeatureType/OGC-OM/2.0/SF_SamplingPoint");

        return doc;
    }

    public SosSensor getSosSensor() { return sosSensor; }
}