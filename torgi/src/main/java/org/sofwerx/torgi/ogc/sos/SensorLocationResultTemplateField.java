package org.sofwerx.torgi.ogc.sos;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class SensorLocationResultTemplateField extends SensorResultTemplateField {
    public SensorLocationResultTemplateField(String name, String quantityDefinition, String unitOfMeasure) { super(null,null,null); }

    @Override
    public void addToElement(Document doc, Element element) {
        if ((doc == null) || (element == null)) {
            Log.e(SosIpcTransceiver.TAG,"Neither doc nor element can be null in SensorResultTemplateField.addToElement()");
            return;
        }
        Element field = doc.createElement("swe:field");
        field.setAttribute("name","location");
        element.appendChild(field);
        Element vector = doc.createElement("swe:Vector");
        field.appendChild(vector);
        vector.setAttribute("definition","http://www.opengis.net/def/property/OGC/0/SensorLocation");
        vector.setAttribute("referenceFrame","http://www.opengis.net/def/crs/EPSG/0/4979");
        Element label = doc.createElement("swe:label");
        vector.appendChild(label);
        label.setTextContent("Location");

        Element coordinateLat = doc.createElement("swe:coordinate");
        vector.appendChild(coordinateLat);
        coordinateLat.setAttribute("name","lat");
        Element quantityLat = doc.createElement("swe:Quantity");
        coordinateLat.appendChild(quantityLat);
        quantityLat.setAttribute("axisID","Lat");
        Element labelLat = doc.createElement("swe:label");
        quantityLat.appendChild(labelLat);
        labelLat.setTextContent("Geodetic Latitude");
        Element uomLat = doc.createElement("swe:uom");
        quantityLat.appendChild(uomLat);
        uomLat.setAttribute("code","deg");

        Element coordinateLng = doc.createElement("swe:coordinate");
        vector.appendChild(coordinateLng);
        coordinateLng.setAttribute("name","lon");
        Element quantityLng = doc.createElement("swe:Quantity");
        coordinateLng.appendChild(quantityLng);
        quantityLng.setAttribute("axisID","Long");
        Element labelLng = doc.createElement("swe:label");
        quantityLng.appendChild(labelLng);
        labelLng.setTextContent("Longitude");
        Element uomLng = doc.createElement("swe:uom");
        quantityLng.appendChild(uomLng);
        uomLng.setAttribute("code","deg");

        Element coordinateAlt = doc.createElement("swe:coordinate");
        vector.appendChild(coordinateAlt);
        coordinateAlt.setAttribute("name","alt");
        Element quantityAlt = doc.createElement("swe:Quantity");
        coordinateAlt.appendChild(quantityAlt);
        quantityAlt.setAttribute("axisID","Alt");
        Element labelAlt = doc.createElement("swe:label");
        quantityAlt.appendChild(labelAlt);
        labelAlt.setTextContent("Altitude");
        Element uomAlt = doc.createElement("swe:uom");
        quantityAlt.appendChild(uomAlt);
        uomAlt.setAttribute("code","m");
    }

    /**
     * Does this measurement have all required fields
     * @return
     */
    @Override
    public boolean isValid() { return true; }
}