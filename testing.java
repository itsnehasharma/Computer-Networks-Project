import java.util.HashMap;

public class testing {

    public static void main(String[] args) {
        HashMap<String, Byte> intByteMap = new HashMap<String, Byte>();
        byte zero = 0b000;
        byte one = 0b001;
        byte two = 0b010;
        byte three = 0b011;
        byte four = 0b100;
        byte five = 0b101;
        byte six = 0b110;
        byte seven = 0b111;
        intByteMap.put("choke", zero);
        intByteMap.put("unchoke", one);
        intByteMap.put("interested", two);
        intByteMap.put("not_interested", three);
        intByteMap.put("have", four);
        intByteMap.put("bitfield", five);
        intByteMap.put("request", six);
        intByteMap.put("piece", seven);
        
    }
    
}
