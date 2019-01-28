package org.sofwerx.torgi.ogc.sosv2;

public interface SosMessageListener {
    void onOperationReceived(AbstractSosOperation operation);
    void onError(String message);
    void onConfigurationSuccess();
}
