import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class FileMap {

    public static void main(String[] args) throws IOException {

        String fileName = "";
        int pieceSize = 0;
        int fileSize = 0;
        try (InputStream input = new FileInputStream("config.properties")) {

            Properties prop = new Properties();

            // load a properties file
            prop.load(input);

            // get properties
            fileName = prop.getProperty("FileName");
            pieceSize = Integer.valueOf(prop.getProperty("PieceSize"));
            fileSize = Integer.valueOf(prop.getProperty("FileSize"));

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        int numPieces = (int) Math.ceil(fileSize / pieceSize);
        int counter = 1;

        HashMap<Integer, byte[]> pieceMap = new HashMap<>();

        byte[] buffer = new byte[pieceSize];

        File file = new File(fileName);
        ByteArrayOutputStream byteOS = new ByteArrayOutputStream();

        try (FileInputStream fileInputStream = new FileInputStream(file);
                BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream)) {

            // add each piece of the file to a map
            while ((bufferedInputStream.read(buffer)) > 0) {

                byteOS.write(buffer, 0, pieceSize);
                byte[] piece = byteOS.toByteArray();
                pieceMap.put(counter, piece);
                byteOS.flush();
                byteOS.reset();
                counter++;

            }
        }
        for (int i = 1; i <= pieceMap.size(); i++) {
            byteOS.write(pieceMap.get(i)); // write all pieces from map into byteOS
        }

        byte[] finalFile = byteOS.toByteArray();
        byteOS.flush();
        byteOS.reset();
        File fullFile = new File(file.getParent(), "finalfile.txt");
        try (FileOutputStream fileOutputStream = new FileOutputStream(fullFile)) {
            // Writing chunk data to file
            fileOutputStream.write(finalFile);
        }

        // creating the bitfield message
        String bitstring = "";
        int bytesNeeded = (int) Math.ceil(numPieces / 7); // each piece represents a bit, first bit is sign
        byte bitfield[] = new byte[bytesNeeded];

        for (int i = 1; i <= pieceMap.size(); i++) {
            
            if (i%7 == 0 && i != 0){ //every 8 bits the bitstring must be written out and reset
                byte b = Byte.parseByte(bitstring, 2);
                byteOS.write(b);
                bitstring = "";
            }

            if (bitstring.equals("")) { //first bit in the bit string must be 0 
                bitstring = "0";
            }

            if (pieceMap.containsKey(i)){ //if the map contains the key, add 1, if not add 0
                bitstring += "1";
            } else bitstring += "0";

            if (i == pieceMap.size()){ //at the end of the map, all remaining bits are 0
                int bsLength = bitstring.length();
                int j = 7 - bsLength;

                for (int k = 0; k < j; k++){
                    bitstring += "0";
                }
            }
        }

        bitfield = byteOS.toByteArray();
        byteOS.flush();
        byteOS.reset();

        //how to check what the person has 
        byte b = Byte.parseByte("01010101",2);
        String gettingBits = Integer.toBinaryString(b);
        System.out.println(gettingBits);


    }

}
