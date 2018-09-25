package org.sofwerx.torgi.ogc.sos;

import org.json.JSONObject;

/**
 * Handles the parsing logic to interpret JSON requests
 */
public abstract class AbstractOperation {
    protected final static long DEFAULT_TIMESPAN = 1000l * 10l; //in milliseconds

    public AbstractOperation() {}
    public AbstractOperation(JSONObject obj) {}

    public abstract JSONObject toJSON();

    public static AbstractOperation getOperation(JSONObject obj) throws UnsupportedOperationException {
        if (obj != null) {
            String operationString = obj.optString("request",null);
            String serviceString = obj.optString("service",null);
            if ((serviceString != null) && !"SOS".equalsIgnoreCase(serviceString))
                throw new UnsupportedOperationException("TORGI service currently only supports SOS");
            String versionString = obj.optString("version",null);
            if ((versionString != null) && !"2.0.0".equalsIgnoreCase(versionString) && !"2.0".equalsIgnoreCase(versionString))
                throw new UnsupportedOperationException("TORGI service currently only supports SOS version 2.0.0");
            if (operationString != null) {
                if ("GetObservation".equalsIgnoreCase(operationString))
                    return new GetObservations(obj);
                else if ("DescribeSensor".equalsIgnoreCase(operationString))
                    return new DescribeSensor(obj);
                else if ("GetCapabilities".equalsIgnoreCase(operationString))
                    return new GetCapabilities(obj);
            }
        } else
            throw new UnsupportedOperationException("TORGI only currently responds to JSON operations");
        throw new UnsupportedOperationException("TORGI does not support SOS Operation "+obj.optString("request",null));
    }
}
