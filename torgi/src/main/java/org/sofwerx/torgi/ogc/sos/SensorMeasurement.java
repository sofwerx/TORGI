package org.sofwerx.torgi.ogc.sos;

/**
 * Holds an actual measurement and the measurements template for a sensor
 */
public class SensorMeasurement {
    protected SensorResultTemplateField format;
    protected Object value;

    public SensorMeasurement(SensorResultTemplateField field) {
        this.format = field;
    }

    /**
     * Gets the format used to report this measurement
     * @return
     */
    public SensorResultTemplateField getFormat() { return format; }

    /**
     * Sets the format used to report this measurement
     * @param format
     */
    public void setFormat(SensorResultTemplateField format) { this.format = format; }

    /**
     * Gets the value of this measurement
     * @return
     */
    @Override
    public String toString() {
        if (value != null) {
            if (value instanceof Integer)
                return Integer.toString((Integer)value);
            if (value instanceof Long)
                return Long.toString((Long)value);
            if (value instanceof Float) {
                float valueF = (Float)value;
                if (Float.isNaN(valueF))
                    return "0.0"; //favor 0.0 rather than report NaN to the SOS
                else
                    return Float.toString(valueF);
            }
            if (value instanceof Double) {
                double valueD = (Double)value;
                if (Double.isNaN(valueD))
                    return "0.0"; //favor 0.0 rather than report NaN to the SOS
                else
                    return Double.toString(valueD);
            }
            if (value instanceof String)
                return (String)value;
        }
        return null; //cannot generate a String
    }

    public void parseLong(String in) throws NumberFormatException {
        if (in != null)
            value = Long.parseLong(in);
    }

    public void parseInt(String in) throws NumberFormatException {
        if (in != null)
            value = Integer.parseInt(in);
    }

    public void parseFloat(String in) throws NumberFormatException {
        if (in != null)
            value = Float.parseFloat(in);
    }

    public void parseDouble(String in) throws NumberFormatException {
        if (in != null)
            value = Double.parseDouble(in);
    }

    /**
     * Sets the value of this measurement
     * @param value
     */
    public void setValue(Object value) { this.value = value; }
    public Object getValue() { return value; }
}
