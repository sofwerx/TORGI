package org.sofwerx.torgi.ogc.sos;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;

public class SensorResultTemplateField {
    private String name;
    private String quantityDefinition;
    private String unitOfMeasure;

    protected SensorResultTemplateField() {}

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

    protected final static String NAME_NAME = "name";
    protected final static String NAME_DEFINITION = "definition";
    protected final static String NAME_CODE = "code";
    protected final static String TAG_NAME_FIELD = "swe:field";
    protected final static String TAG_NAME_QUANTITY = "swe:Quantity";
    protected final static String TAG_NAME_UOM = "swe:uom";
    protected final static String TAG_NAME_LABEL = "swe:label";
    protected final static String NAME_REFERENCE_FRAME = "referenceFrame";

    /**
     * Gets the NodeList for all of the TemplateField elements within a given parent
     * @param element parent
     * @return NodeList with all of the TemplateFields
     */
    public static NodeList getTemplateFieldsNodeList(Element element) {
        if (element == null)
            return null;
        return element.getElementsByTagName(TAG_NAME_FIELD);
    }

    /**
     * Gets a list of all of the TemplateField elements within a given parent
     * @param element parent
     * @return ArrayList of SensorResultTemplateFields (or null if none found)
     */
    public static ArrayList<SensorResultTemplateField> getTemplateFields(Element element) {
        if (element == null)
            return null;
        ArrayList<SensorResultTemplateField> fields = null;

        NodeList fieldsList = getTemplateFieldsNodeList(element);
        if ((fieldsList != null) && (fieldsList.getLength() > 0)) {
            for (int i=0;i<fieldsList.getLength();i++) {
                try {
                    SensorResultTemplateField field = newFromXML((Element) fieldsList.item(i));
                    if (field != null) {
                        if (fields == null)
                            fields = new ArrayList<>();
                        fields.add(field);
                    }
                } catch (Exception e) {
                    Log.e(SosIpcTransceiver.TAG,"Error extracting field from fields NodeList: "+e.getMessage());
                }
            }
        }

        return fields;
    }

    /**
     * Creates a new SensorResultTemplateField from an XML element
     * @param field the element that is the field
     * @return the SensorResultTemplateField (or null if invalid/missing)
     */
    public static SensorResultTemplateField newFromXML(Element field) {
        SensorResultTemplateField template = null;

        if (field != null) {
            String name = field.getAttribute(NAME_NAME);
            if (name != null) {
                if (SensorTimeResultTemplateField.TIME_VALUE_NAME.equalsIgnoreCase(name))
                    template = new SensorTimeResultTemplateField();
                else if (SensorLocationResultTemplateField.LOC_VALUE_NAME.equalsIgnoreCase(name))
                    template = new SensorLocationResultTemplateField();
                else
                    template = new SensorResultTemplateField();
            } else
                Log.d(SosIpcTransceiver.TAG,"Cannot parse an XML element for SensorResultTemplateField with a null Name atrribute");
        }

        if (template != null) {
            template.parse(field);
            if (!template.isValid()) {
                Log.e(SosIpcTransceiver.TAG,"Parsing SensorTemplateField did not produce a valid template. This template will be ignored.");
                template = null;
            }
        }

        return template;
    }

    public void parse(Element field) {
        if (field == null)
            return;
        name = field.getAttribute(NAME_NAME);
        try {
            Element elementQuantity = (Element) field.getElementsByTagName(TAG_NAME_QUANTITY).item(0);
            quantityDefinition = elementQuantity.getAttribute(NAME_DEFINITION);
            Element elementUom = (Element) elementQuantity.getElementsByTagName(TAG_NAME_UOM).item(0);
            unitOfMeasure = elementUom.getAttribute(NAME_CODE);
        } catch (Exception e) {
            Log.e(SosIpcTransceiver.TAG,"Parsing error: "+e.getMessage());
        }
    }

    public void addToElement(Document doc, Element element) {
        if ((doc == null) || (element == null)) {
            Log.e(SosIpcTransceiver.TAG,"Neither doc nor element can be null in SensorResultTemplateField.addToElement()");
            return;
        }
        Element field = doc.createElement(TAG_NAME_FIELD);
        field.setAttribute(NAME_NAME,name);
        element.appendChild(field);
        Element quantity = doc.createElement(TAG_NAME_QUANTITY);
        quantity.setAttribute(NAME_DEFINITION,quantityDefinition);
        field.appendChild(quantity);
        Element uom = doc.createElement(TAG_NAME_UOM);
        uom.setAttribute(NAME_CODE,unitOfMeasure);
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