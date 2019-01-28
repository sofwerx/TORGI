package org.sofwerx.torgi.ogc.sosv2;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
    protected void parse(Element element) {
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
        Element procedureDescriptionFormat = doc.createElement("swes:procedureDescriptionFormat");
        procedureDescriptionFormat.setTextContent("http://www.opengis.net/sensorml/2.0");
        insertSensor.appendChild(procedureDescriptionFormat);
        Element procedureDescription = doc.createElement("swes:procedureDescription");
        insertSensor.appendChild(procedureDescription);
        Element physicalSystem = doc.createElement("sml:PhysicalSystem");
        procedureDescription.appendChild(physicalSystem);
        if (sosSensor.getId() != null)
            physicalSystem.setAttribute("gml:id",sosSensor.getId());
        if (sosSensor.getUniqueId() != null) {
            Element identifier = doc.createElement("gml:identifier");
            identifier.setAttribute("codeSpace","uniqueID");
            identifier.setTextContent(sosSensor.getUniqueId());
            physicalSystem.appendChild(identifier);
        }
        Element identification = doc.createElement("sml:identification");
        physicalSystem.appendChild(identification);
        Element identifierList = doc.createElement("sml:IdentifierList");
        identification.appendChild(identifierList);
        if (sosSensor.getLongName() != null) {
            Element identifier2 = doc.createElement("sml:identifier");
            identifierList.appendChild(identifier2);
            Element term2 = doc.createElement("sml:Term");
            identifier2.appendChild(term2);
            term2.setAttribute("definition", "urn:ogc:def:identifier:OGC:1.0:longName");
            Element label2 = doc.createElement("sml:label");
            term2.appendChild(label2);
            label2.setTextContent("longName");
            Element value2 = doc.createElement("sml:value");
            term2.appendChild(value2);
            value2.setTextContent(sosSensor.getLongName());
        }
        if (sosSensor.getShortName() != null) {
            Element identifier3 = doc.createElement("sml:identifier");
            identifierList.appendChild(identifier3);
            Element term3 = doc.createElement("sml:Term");
            identifier3.appendChild(term3);
            term3.setAttribute("definition", "urn:ogc:def:identifier:OGC:1.0:shortName");
            Element label3 = doc.createElement("sml:label");
            term3.appendChild(label3);
            label3.setTextContent("shortName");
            Element value3 = doc.createElement("sml:value");
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
}