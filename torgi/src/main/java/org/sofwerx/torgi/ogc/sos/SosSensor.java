package org.sofwerx.torgi.ogc.sos;

import android.util.Log;

import java.util.ArrayList;

/**
 * This is the overall device/sensor that will be reporting to the SOS. This sensor can make report
 * multiple measurements.
 */
public class SosSensor {
    private String assignedOffering;
    private String assignedProcedure;
    private String assignedTemplate;
    private String id;
    private String uniqueId;
    private String longName;
    private String shortName;
    private ArrayList<SensorMeasurement> measurements;

    public SosSensor() {
        assignedOffering = null;
        assignedProcedure = null;
        assignedTemplate = null;
    }

    /**
     * Constructor for a Sensor
     * @param id the display ID on most systems (i.e. "TORGI WISKEY 35")
     * @param uniqueId the unique ID for this sensor (i.e. "http://www.sofwerx.org/torgi/wiskey35")
     * @param shortName the short name for what this sensor is (i.e. "TORGI")
     * @param longName the long name for this sensor (i.e. "")
     */
    public SosSensor(String id, String uniqueId, String shortName, String longName) {
        this();
        this.id = id;
        this.uniqueId = uniqueId;
        this.shortName = shortName;
        this.longName = longName;
    }

    public ArrayList<SensorMeasurement> getSensorMeasurements() { return measurements; }

    /**
     * Adds a measurement to the sensor
     * @param measurement
     */
    public void addMeasurement(SensorMeasurement measurement) {
        if (measurement == null)
            return;
        if ((measurement.getFormat() == null) && (measurement.toString() != null)) {
            Log.e(SosIpcTransceiver.TAG, "Cannot add a measurement to the sensor without specifying the measurement format");
            return;
        }
        if (measurements == null)
            measurements = new ArrayList<>();
        measurements.add(measurement);
    }

    /**
     * Gets the assigned procedure ID provided by the SOS
     * (i.e. "http://www.sofwerx.org/torgi/wiskey35")
     * @return
     */
    public String getAssignedProcedure() { return assignedProcedure; }

    /**
     * Sets the assigned procedure ID provided by the SOS
     * (i.e. "http://www.sofwerx.org/torgi/wiskey35")
     * @param assignedProcedure
     */
    public void setAssignedProcedure(String assignedProcedure) { this.assignedProcedure = assignedProcedure; }

    /**
     * Gets the assigned offering ID provided by the SOS
     * (i.e. "http://www.sofwerx.org/torgi/wiskey35-sos")
     * @return
     */
    public String getAssignedOffering() { return assignedOffering; }

    /**
     * Sets the assigned offering ID provided by the SOS
     * (i.e. "http://www.sofwerx.org/torgi/wiskey35-sos")
     * @param assignedOffering
     */
    public void setAssignedOffering(String assignedOffering) { this.assignedOffering = assignedOffering; }

    /**
     * Gets the person-readable ID for this sensor (i.e. "TORGI Wiskey 35")
     * @return
     */
    public String getId() { return id; }

    /**
     * Sets the person-readable ID for this sensor (i.e. "TORGI Wiskey 35")
     * @param id
     */
    public void setId(String id) { this.id = id; }

    /**
     * Gets the unique ID for SOS purposes (i.e. "http://www.sofwerx.org/torgi/wiskey35")
     * @return
     */
    public String getUniqueId() { return uniqueId; }

    /**
     * Sets the unique ID for SOS purposes (i.e. "http://www.sofwerx.org/torgi/wiskey35")
     * @param uniqueId
     */
    public void setUniqueId(String uniqueId) { this.uniqueId = uniqueId; }

    /**
     * Gets the long name for this type of sensor
     * (i.e. "Tactical Observation of RF and GNSS Interference sensor")
     * @return
     */
    public String getLongName() { return longName; }

    /**
     * Gets the long name for this type of sensor
     * (i.e. "Tactical Observation of RF and GNSS Interference sensor")
     * @param longName
     */
    public void setLongName(String longName) { this.longName = longName; }

    /**
     * Gets the short name for this type of sensor (i.e. "TORGI")
     * @return
     */
    public String getShortName() { return shortName; }

    /**
     * Sets the short name for this type of sensor (i.e. "TORGI")
     * @param shortName
     */
    public void setShortName(String shortName) { this.shortName = shortName; }

    /**
     * Gets the template ID assigned by the SOS. This is received in response
     * to an InsertResultTemplate operation and required for use in an
     * InsertResult operation. This assigned template looks like
     * "http://www.sofwerx.org/torgi/wiskey35#output0"
     * @return
     */
    public String getAssignedTemplate() { return assignedTemplate; }

    /**
     * Sets the template ID assigned by the SOS. This is received in response
     * to an InsertResultTemplate operation and required for use in an
     * InsertResult operation. This assigned template looks like
     * "http://www.sofwerx.org/torgi/wiskey35#output0"
     * @param assignedTemplate
     */
    public void setAssignedTemplate(String assignedTemplate) { this.assignedTemplate = assignedTemplate; }

    /**
     * Does the sensor have measurements and do all of the measurements have the required template
     * @return
     */
    public boolean isMeasurmentsFieldsValid() {
        if ((measurements == null) || measurements.isEmpty())
            return false;
        boolean passed = true;
        for(SensorMeasurement measurement:measurements) {
            if ((measurement.getFormat() == null) || !measurement.getFormat().isValid())
                return false;
        }
        return passed;
    }

    /**
     * Does this sensor have enough information to begin the registration process with the SOS server
     * @return
     */
    public boolean isReadyToRegisterSensor() {
        return ((id != null) && (uniqueId != null) && (longName != null) && (shortName != null));
    }

    /**
     * Does this sensor have enough information to register a result template
     * @return
     */
    public boolean isReadyToRegisterResultTemplate() {
        return ((assignedProcedure != null) && (assignedOffering != null)
        && (measurements != null) && !measurements.isEmpty());
    }

    /**
     * Does this sensor have the registration information to begin sending results
     * @return
     */
    public boolean isReadyToSendResults() {
        return (assignedTemplate != null);
    }


    public boolean isSame(SosSensor other) {
        if (other == null)
            return false;
        if ((uniqueId == null) || (other.uniqueId == null))
            return false;
        return uniqueId.equalsIgnoreCase(other.uniqueId);
    }
}
