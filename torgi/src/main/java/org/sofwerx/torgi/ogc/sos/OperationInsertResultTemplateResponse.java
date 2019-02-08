package org.sofwerx.torgi.ogc.sos;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.ParserConfigurationException;

public class OperationInsertResultTemplateResponse extends AbstractSosOperation {
    public final static String NAMESPACE = "InsertResultTemplateResponse";
    private String acceptedTemplate;

    public OperationInsertResultTemplateResponse() {}

    public OperationInsertResultTemplateResponse(String acceptedTemplate) {
        this();
        this.acceptedTemplate = acceptedTemplate;
    }

    @Override
    public boolean isValid() {
        return (acceptedTemplate != null);
    }

    @Override
    protected void parse(Element element) {
        if ((element == null) || !element.hasChildNodes())
            return;
        NodeList items = element.getChildNodes();
        for (int i=0; i<items.getLength();i++) {
            Node item = items.item(i);
            String name = item.getNodeName();
            if (name.contains("acceptedTemplate"))
                acceptedTemplate = item.getTextContent();
        }
    }

    @Override
    public Document toXML() throws ParserConfigurationException {
        if (acceptedTemplate == null) {
            Log.e(SosIpcTransceiver.TAG,"SosSensor assigned template cannot be null in InsertResultTemplate operation");
            return null;
        }
        Document doc = super.toXML();
        Element insertResultTemplate = doc.createElement(NAMESPACE);
        doc.appendChild(insertResultTemplate);
        Element elementAcceptedTemplate = doc.createElement("acceptedTemplate");
        insertResultTemplate.appendChild(elementAcceptedTemplate);
        elementAcceptedTemplate.setTextContent(acceptedTemplate);
        return doc;
    }

    public String getAcceptedTemplate() { return acceptedTemplate; }
}
