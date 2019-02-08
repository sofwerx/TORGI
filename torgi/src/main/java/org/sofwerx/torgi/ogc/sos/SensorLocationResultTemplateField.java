package org.sofwerx.torgi.ogc.sos;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class SensorLocationResultTemplateField extends SensorResultTemplateField {
    public SensorLocationResultTemplateField() {}
    public SensorLocationResultTemplateField(String name, String quantityDefinition, String unitOfMeasure) { super(null,null,null); }

    @Override
    public void parse(Element field) {
        if (field == null)
            return;
        /*Since we're only handling one format right now, we're skipping over all the definition, reference frame, units, etc
        try {
            Element elementVector = (Element) field.getElementsByTagName(LOC_TAG_NAME_VECTOR).item(0);
            NodeList vect3 = elementVector.getChildNodes();
            for (int i=0; i<3; i++) {
                Element
            }
        } catch (Exception e) {
            Log.e(SosIpcTransceiver.TAG,"Parsing error for SensorLocationResultTemplateField: "+e.getMessage());
        }*/
    }

    protected final static String LOC_TAG_NAME_COORDINATE = "swe:coordinate";
    protected final static String LOC_TAG_NAME_VECTOR = "swe:Vector";
    protected final static String LOC_VALUE_NAME = "location";
    protected final static String LOC_VALUE_DEFINITION = "http://www.opengis.net/def/property/OGC/0/SensorLocation";
    protected final static String LOC_VALUE_REFERENCEFRAME = "http://www.opengis.net/def/crs/EPSG/0/4979";
    protected final static String LOC_VALUE_LABEL = "Location";
    protected final static String LOC_NAME_AXISID = "axisID";
    protected final static String LOC_VALUE_CODE_LAT_LNG = "deg";
    protected final static String LOC_VALUE_CODE_ALT = "m";
    protected final static String LOC_VALUE_LABEL_LAT = "Geodetic Latitude";
    protected final static String LOC_NAME_VALUE_LAT = "lat";
    protected final static String LOC_VALUE_AXISID_LAT = "Lat";
    protected final static String LOC_VALUE_LABEL_LNG = "Longitude";
    protected final static String LOC_NAME_VALUE_LNG = "lon";
    protected final static String LOC_VALUE_AXISID_LNG = "Long";
    protected final static String LOC_VALUE_LABEL_ALT = "Altitude";
    protected final static String LOC_NAME_VALUE_ALT = "alt";
    protected final static String LOC_VALUE_AXISID_ALT = "Alt";

    @Override
    public void addToElement(Document doc, Element element) {
        if ((doc == null) || (element == null)) {
            Log.e(SosIpcTransceiver.TAG,"Neither doc nor element can be null in SensorResultTemplateField.addToElement()");
            return;
        }
        Element field = doc.createElement(TAG_NAME_FIELD);
        field.setAttribute(NAME_NAME,LOC_VALUE_NAME);
        element.appendChild(field);
        Element vector = doc.createElement(LOC_TAG_NAME_VECTOR);
        field.appendChild(vector);
        vector.setAttribute(NAME_DEFINITION,LOC_VALUE_DEFINITION);
        vector.setAttribute(NAME_REFERENCE_FRAME,LOC_VALUE_REFERENCEFRAME);
        Element label = doc.createElement(TAG_NAME_LABEL);
        vector.appendChild(label);
        label.setTextContent(LOC_VALUE_LABEL);

        Element coordinateLat = doc.createElement(LOC_TAG_NAME_COORDINATE);
        vector.appendChild(coordinateLat);
        coordinateLat.setAttribute(NAME_NAME,LOC_NAME_VALUE_LAT);
        Element quantityLat = doc.createElement(TAG_NAME_QUANTITY);
        coordinateLat.appendChild(quantityLat);
        quantityLat.setAttribute(LOC_NAME_AXISID,LOC_VALUE_AXISID_LAT);
        Element labelLat = doc.createElement(TAG_NAME_LABEL);
        quantityLat.appendChild(labelLat);
        labelLat.setTextContent(LOC_VALUE_LABEL_LAT);
        Element uomLat = doc.createElement(TAG_NAME_UOM);
        quantityLat.appendChild(uomLat);
        uomLat.setAttribute(NAME_CODE,LOC_VALUE_CODE_LAT_LNG);

        Element coordinateLng = doc.createElement(LOC_TAG_NAME_COORDINATE);
        vector.appendChild(coordinateLng);
        coordinateLng.setAttribute(NAME_NAME,LOC_NAME_VALUE_LNG);
        Element quantityLng = doc.createElement(TAG_NAME_QUANTITY);
        coordinateLng.appendChild(quantityLng);
        quantityLng.setAttribute(LOC_NAME_AXISID,LOC_VALUE_AXISID_LNG);
        Element labelLng = doc.createElement(TAG_NAME_LABEL);
        quantityLng.appendChild(labelLng);
        labelLng.setTextContent(LOC_VALUE_LABEL_LNG);
        Element uomLng = doc.createElement(TAG_NAME_UOM);
        quantityLng.appendChild(uomLng);
        uomLng.setAttribute(NAME_CODE,LOC_VALUE_CODE_LAT_LNG);

        Element coordinateAlt = doc.createElement(LOC_TAG_NAME_COORDINATE);
        vector.appendChild(coordinateAlt);
        coordinateAlt.setAttribute(NAME_NAME,LOC_NAME_VALUE_ALT);
        Element quantityAlt = doc.createElement(TAG_NAME_QUANTITY);
        coordinateAlt.appendChild(quantityAlt);
        quantityAlt.setAttribute(LOC_NAME_AXISID,LOC_VALUE_AXISID_ALT);
        Element labelAlt = doc.createElement(TAG_NAME_LABEL);
        quantityAlt.appendChild(labelAlt);
        labelAlt.setTextContent(LOC_VALUE_LABEL_ALT);
        Element uomAlt = doc.createElement(TAG_NAME_UOM);
        quantityAlt.appendChild(uomAlt);
        uomAlt.setAttribute(NAME_CODE,LOC_VALUE_CODE_ALT);
    }

    /**
     * Does this measurement have all required fields
     * @return
     */
    @Override
    public boolean isValid() { return true; }
}