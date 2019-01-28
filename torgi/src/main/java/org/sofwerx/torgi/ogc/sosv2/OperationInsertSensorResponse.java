package org.sofwerx.torgi.ogc.sosv2;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.ParserConfigurationException;

public class OperationInsertSensorResponse extends AbstractSosOperation {
    public final static String NAMESPACE = "InsertSensorResponse";
    private String assignedProcedure, assignedOffering;

    public OperationInsertSensorResponse() {
        super();
    }

    public OperationInsertSensorResponse(String assignedProcedure, String assignedOffering) {
        super();
        this.assignedProcedure = assignedProcedure;
        this.assignedOffering = assignedOffering;
    }

    @Override
    protected void parse(Element element) {
        if ((element == null) || !element.hasChildNodes())
            return;
        NodeList items = element.getChildNodes();
        for (int i=0; i<items.getLength();i++) {
            Node item = items.item(i);
            String name = item.getNodeName();
            if (name.contains("assignedProcedure"))
                assignedProcedure = item.getTextContent();
            else if (name.contains("assignedOffering"))
                assignedOffering = item.getTextContent();
        }
    }

    @Override
    public Document toXML() throws ParserConfigurationException {
        if ((assignedProcedure == null) || (assignedOffering == null)) {
            Log.e(SosIpcTransceiver.TAG,"Sensor needs an assigned procedure and offering in order to generate a response");
            return null;
        }
        Document doc = super.toXML();
        Element insertSensorResponse = doc.createElement(NAMESPACE);
        doc.appendChild(insertSensorResponse);
        Element elementAssignedProcedure = doc.createElement("swes:assignedProcedure");
        insertSensorResponse.appendChild(elementAssignedProcedure);
        elementAssignedProcedure.setTextContent(assignedProcedure);
        Element elementAssignedOffering = doc.createElement("swes:assignedOffering");
        insertSensorResponse.appendChild(elementAssignedOffering);
        elementAssignedOffering.setTextContent(assignedOffering);

        return doc;
    }

    public String getAssignedProcedure() { return assignedProcedure; }
    public String getAssignedOffering() { return assignedOffering; }
}