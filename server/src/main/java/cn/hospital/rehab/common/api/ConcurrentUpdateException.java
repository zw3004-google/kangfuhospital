package cn.hospital.rehab.common.api;

public class ConcurrentUpdateException extends RuntimeException {
    public ConcurrentUpdateException(String message) { super(message); }
}
