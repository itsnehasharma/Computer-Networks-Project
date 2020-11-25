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

        int numPieces = (int) Math.ceil(fileSize/pieceSize);
        int counter = 1;

        HashMap<Integer, byte[]> pieceMap = new HashMap<>();

        byte[] buffer = new byte[pieceSize];

        File file = new File(fileName);
        ByteArrayOutputStream byteOS = new ByteArrayOutputStream();

        try(FileInputStream fileInputStream = new FileInputStream(file);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream)) {

        //add each piece of the file to a map 
        while ((bufferedInputStream.read(buffer))>0){
            
            byteOS.write(buffer, 0, pieceSize);
            byte[] piece = byteOS.toByteArray();
            pieceMap.put(counter, piece);
            byteOS.flush();
            byteOS.reset();
            counter++;

        }
    }
        for (int i = 1; i <= pieceMap.size(); i++){
            byteOS.write(pieceMap.get(i)); //write all pieces from map into byteOS
        }
        byteOS.flush();
        byteOS.reset();
        
        byte [] finalFile = byteOS.toByteArray();
        File fullFile = new File(file.getParent(), "finalfile.txt");
        try (FileOutputStream fileOutputStream = new FileOutputStream(fullFile)){
            //Writing chunk data to file
            fileOutputStream.write(finalFile);
        }

        //creating the bitfield message 
        String bitstring = "";
        int bytesNeeded = (int) Math.ceil(numPieces/8); //each piece represents a bit 
        byte bitfield[] = new byte[bytesNeeded];

        //figure out 8 bit issue
    //     for (int i = 0; i < numPieces; i++){
    //         if (i % 8 == 0 && i != 0) { //every 8 bits is a new byte
    //             byte b = Byte.parseByte(bitstring, 2);
    //             byteOS.write(b);
    //             bitstring = "";                               
    //         }            
    //         if (pieceMap.containsKey(i+1)){
    //             bitstring += "1";
    //         } else {
    //             bitstring += "0";
    //         }

    //         if (i == numPieces-1) {
    //             byte b = Byte.parseByte(bitstring, 2);
    //             byteOS.write(b);
    //         }
    //     }

    //     bitfield = byteOS.toByteArray();
    //     byteOS.flush();
    //     byteOS.reset();

    }
    
}
