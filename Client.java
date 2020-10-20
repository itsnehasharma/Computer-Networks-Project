import java.net.*;
import java.io.*;

public class Client {

    public static void main(String[] args) throws IOException{
        
        int filesize=2022386; 
        int bytesRead; //contains the current statistics of the bytes read from input channel ie 
        int currentTot = 0; //total number of bytes read

        //define a new socket on local ip 
        Socket socket = new Socket("127.0.0.1", 15123);

        //create a new byte array for the expected file size to hold temporary data 
        byte [] bytearray = new byte[filesize];

        //create a new input stread to read from the socket 
        InputStream is = socket.getInputStream();

        //take from the socket into a file output to copy.doc
        FileOutputStream fos = new FileOutputStream("copy.txt");
        BufferedOutputStream bos = new BufferedOutputStream(fos);

        //total number of bytes read in the input stream
        bytesRead = is.read(bytearray, 0, bytearray.length);
        currentTot = bytesRead; //initially total is the number of bytes read

        //continue to read from the input stream until there is not data left on the stream
        do {
            bytesRead = 
                is.read(bytearray, currentTot, (bytearray.length-currentTot));
            if (bytesRead >= 0) currentTot += bytesRead;

        } while (bytesRead > -1);

        bos.write(bytearray, 0, currentTot);
        bos.flush();
        bos.close();
        socket.close();


        
    }
}