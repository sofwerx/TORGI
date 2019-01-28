package org.sofwerx.torgi.ogc.sosv2;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class SensorResultTemplateField {
    private String name;
    private String quantityDefinition;
    private String unitOfMeasure;

    /**
     * Constructor for a sensor field
     * @param name (i.e. "agc" for Automatic Gain Control measurement)
     * @param quantityDefinition definition for this quantity (i.e. "http://www.sofwerx.org/torgi.owl#AGC")
     * @param unitOfMeasure (i.e. "dB")
     */
    public SensorResultTemplateField(String name, String quantityDefinition, String unitOfMeasure) {
        this.name = name;
        this.quantityDefinition = quantityDefinition;
        this.unitOfMeasure = unitOfMeasure;
    }

    /**
     * Gets the field name (i.e. "agc" for Automatic Gain Control measurement)
     * @return
     */
    public String getName() { return name; }

    /**
     * Sets the field name (i.e. "agc" for Automatic Gain Control measurement)
     * @param name
     */
    public void setName(String name) { this.name = name; }

    /**
     * Gets the definition for quantity (i.e. "http://www.sofwerx.org/torgi.owl#AGC")
     * @return
     */
    public String getQuantityDefinition() { return quantityDefinition; }

    /**
     * Sets the definition for quantity (i.e. "http://www.sofwerx.org/torgi.owl#AGC")
     * @param quantityDefinition
     */
    public void setQuantityDefinition(String quantityDefinition) { this.quantityDefinition = quantityDefinition; }

    /**
     * Gets the unit of measure (i.e. "dB")
     * @return
     */
    public String getUnitOfMeasure() { return unitOfMeasure; }

    /**
     * Sets the unit of measure (i.e. "dB")
     * @param unitOfMeasure
     */
    public void setUnitOfMeasure(String unitOfMeasure) { this.unitOfMeasure = unitOfMeasure; }

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
        field.setAttribute("name",name);
        element.appendChild(field);
        Element quantity = doc.createElement("swe:Quantity");
        quantity.setAttribute("definition",quantityDefinition);
        field.appendChild(quantity);
        Element uom = doc.createElement("swe:uom");
        uom.setAttribute("code",unitOfMeasure);
        quantity.appendChild(uom);
    }

    /**
     * Does this measurement have all required fields
     * @return
     */
    public boolean isValid() {
        return (name != null) && (quantityDefinition != null) && (unitOfMeasure != null);
    }
}