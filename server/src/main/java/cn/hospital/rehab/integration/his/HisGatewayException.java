package cn.hospital.rehab.integration.his;

public class HisGatewayException extends RuntimeException {
    public HisGatewayException(String message) { super(message); }
    public HisGatewayException(String message, Throwable cause) { super(message, cause); }
}
