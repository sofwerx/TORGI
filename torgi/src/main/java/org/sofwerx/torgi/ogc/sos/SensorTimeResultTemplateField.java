package org.sofwerx.torgi.ogc.sos;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class SensorTimeResultTemplateField extends SensorResultTemplateField {
    public SensorTimeResultTemplateField() {
        super(null,null,null);
    }

    public void parse(Element element) {
        if (element == null)
            return;
        //FIXME
    }

    public void addToElement(Document doc, Element element) {
        if ((doc == null) || (element == null)) {
            Log.e(SosIpcTransceiver.TAG,"Neither doc nor element can be null in SensorResultTemplateField.addToElement()");
            return;
        }
        Element field = doc.createElement("swe:field");
        field.setAttribute("name","time");
        element.appendChild(field);
        Element quantity = doc.createElement("swe:Time");
        quantity.setAttribute("definition","http://www.opengis.net/def/ogc/SamplingTime");
        field.appendChild(quantity);
        Element uom = doc.createElement("swe:uom");
        uom.setAttribute("code","ISO 8601");
        quantity.appendChild(uom);
    }

    /**
     * Does this measurement have all required fields
     * @return
     */
    @Override
    public boolean isValid() {
        return true;
    }
}