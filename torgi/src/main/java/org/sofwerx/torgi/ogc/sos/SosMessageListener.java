package org.sofwerx.torgi.ogc.sos;

public interface SosMessageListener {
    void onSosOperationReceived(AbstractSosOperation operation);
    void onSosError(String message);
    void onSosConfigurationSuccess();
}
