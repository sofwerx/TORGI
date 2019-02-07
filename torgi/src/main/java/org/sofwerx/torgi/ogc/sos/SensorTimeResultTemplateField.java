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
        //This is ignored for now since only one time template field is in use at the moment
    }

    private final static String TIME_TAG_TIME = "swe:Time";
    protected final static String TIME_VALUE_NAME = "time";
    private final static String TIME_VALUE_DEFINITION = "http://www.opengis.net/def/ogc/SamplingTime";
    private final static String TIME_VALUE_REFERENCE_FRAME = "http://www.opengis.net/def/trs/BIPM/0/UTC";
    private final static String TIME_NAME_XLINK_HREF = "xlink:href";
    private final static String TIME_VALUE_XLINK_HREF = "http://www.opengis.net/def/uom/ISO-8601/0/Gregorian";

    public void addToElement(Document doc, Element element) {
        if ((doc == null) || (element == null)) {
            Log.e(SosIpcTransceiver.TAG,"Neither doc nor element can be null in SensorResultTemplateField.addToElement()");
            return;
        }
        Element field = doc.createElement(TAG_NAME_FIELD);
        field.setAttribute(NAME_NAME,TIME_VALUE_NAME);
        element.appendChild(field);
        Element quantity = doc.createElement(TIME_TAG_TIME);
        quantity.setAttribute(NAME_DEFINITION,TIME_VALUE_DEFINITION);
        quantity.setAttribute(NAME_REFERENCE_FRAME,TIME_VALUE_REFERENCE_FRAME);
        field.appendChild(quantity);
        Element uom = doc.createElement(TAG_NAME_UOM);
        uom.setAttribute(TIME_NAME_XLINK_HREF,TIME_VALUE_XLINK_HREF);
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