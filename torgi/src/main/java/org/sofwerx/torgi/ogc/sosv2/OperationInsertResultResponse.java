package org.sofwerx.torgi.ogc.sosv2;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.ParserConfigurationException;

public class OperationInsertResultResponse extends AbstractSosOperation {
    public final static String NAMESPACE = "InsertResultResponse";

    @Override
    protected void parse(Element element) {
        if (element == null)
            return;
    }

    @Override
    public Document toXML() throws ParserConfigurationException {
        Document doc = super.toXML();
        Element element = doc.createElement(NAMESPACE);
        doc.appendChild(element);
        return doc;
    }
}