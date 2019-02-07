package org.sofwerx.torgi.ogc.sos;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.ParserConfigurationException;

public class OperationInsertResultResponse extends AbstractSosOperation {
    public final static String NAMESPACE = "InsertResultResponse";

    @Override
    public boolean isValid() { return true; }

    @Override
    protected void parse(Element element) {
        if (element == null)
            return;
        //ignore
    }

    @Override
    public Document toXML() throws ParserConfigurationException {
        Document doc = super.toXML();
        Element element = doc.createElement(NAMESPACE);
        doc.appendChild(element);
        return doc;
    }
}