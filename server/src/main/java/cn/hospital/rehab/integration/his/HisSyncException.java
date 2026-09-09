package cn.hospital.rehab.integration.his;

public class HisSyncException extends IllegalArgumentException {
    private final String batchNo;
    public HisSyncException(String batchNo,String message,Throwable cause){super(message,cause);this.batchNo=batchNo;}
    public String getBatchNo(){return batchNo;}
}
