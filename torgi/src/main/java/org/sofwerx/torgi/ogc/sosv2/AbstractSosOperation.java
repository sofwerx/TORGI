package org.sofwerx.torgi.ogc.sosv2;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public abstract class AbstractSosOperation {
    public static AbstractSosOperation newFromXmlString(String text) {
        if (text != null) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            try {
                DocumentBuilder builder = null;
                builder = factory.newDocumentBuilder();
                Document doc = builder.parse(new InputSource(new StringReader(text)));
                return newFromXML(doc);
            } catch (ParserConfigurationException | IOException | SAXException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static AbstractSosOperation newFromXML(Document doc) {
        AbstractSosOperation operation = null;
        if (doc != null) {
            Element element = doc.getDocumentElement();
            if (element != null) {
                String tagName = element.getTagName();
                if (tagName != null) {
                    if (tagName.contains(OperationInsertSensorResponse.NAMESPACE))
                        operation = new OperationInsertSensorResponse();
                    else if (tagName.contains(OperationInsertSensor.NAMESPACE))
                        operation = new OperationInsertSensor();
                    else if (tagName.contains(OperationInsertResultTemplateResponse.NAMESPACE))
                        operation = new OperationInsertResultTemplateResponse();
                    else if (tagName.contains(OperationInsertResultTemplate.NAMESPACE))
                        operation = new OperationInsertResultTemplate();
                    else if (tagName.contains(OperationInsertResultResponse.NAMESPACE))
                        operation = new OperationInsertResultResponse();
                    else if (tagName.contains(OperationInsertResult.NAMESPACE))
                        operation = new OperationInsertResult(null);
                    if (operation != null)
                        operation.parse(element);
                }
            }
        }
        return operation;
    }

    protected abstract void parse(Element element);
    public Document toXML() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder parser = factory.newDocumentBuilder();
        return parser.newDocument();
    }
}
