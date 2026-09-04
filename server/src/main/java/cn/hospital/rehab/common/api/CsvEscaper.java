package cn.hospital.rehab.common.api;

public final class CsvEscaper {
    private CsvEscaper(){}
    public static String value(Object input){
        if(input==null)return "";String value=String.valueOf(input);
        if(!value.isEmpty()&&"=+-@".indexOf(value.charAt(0))>=0)value="'"+value;
        if(value.indexOf(',')>=0||value.indexOf('"')>=0||value.indexOf('\n')>=0||value.indexOf('\r')>=0)
            return "\""+value.replace("\"","\"\"")+"\"";
        return value;
    }
}
