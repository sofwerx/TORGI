package org.sofwerx.torgi.ogc.sosv2;

/**
 * Holds an actual measurement and the measurements template for a sensor
 */
public class SensorMeasurement {
    protected SensorResultTemplateField format;
    private String value;

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
    public String getValue() { return value; }

    /**
     * Sets the value of this measurement
     * @param value
     */
    public void setValue(String value) { this.value = value; }

    /**
     * Sets the value of this measurement
     * @param value
     */
    public void setValue(double value) {
        if (Double.isNaN(value)) //prefer 0 to Double.NaN for easier SOS parsing
            this.value = "0.0";
        else
            this.value = Double.toString(value); }

    /**
     * Sets the value of this measurement
     * @param value
     */
    public void setValue(float value) {
        if (Float.isNaN(value)) //prefer 0 to Double.NaN for easier SOS parsing
            this.value = "0.0";
        else
            this.value = Float.toString(value);
    }

    /**
     * Sets the value of this measurement
     * @param value
     */
    public void setValue(int value) { this.value = Integer.toString(value); }

    /**
     * Sets the value of this measurement
     * @param value
     */
    public void setValue(long value) { this.value = Long.toString(value); }
}
