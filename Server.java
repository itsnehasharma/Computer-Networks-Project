import java.net.*;
import java.io.*;
import java.util.*;
import java.lang.*;

public class Server {

    public static void main(String[] args) throws IOException {

        ServerSocket serverSocket = new ServerSocket(15123);

        //wait for client connection, and then accept connection 
        Socket socket = serverSocket.accept();
        System.out.println("Accepted connection: " + socket);

        //create new file 
        // File transferFile;
        String fileName = "";
        int pieceSize = 0;

        try (InputStream input = new FileInputStream("config.properties")) {

            Properties prop = new Properties();

            // load a properties file
            prop.load(input);

            // get the property value and print it out
            System.out.println(prop.getProperty("FileName"));
            fileName = prop.getProperty("FileName");
            pieceSize = Integer.valueOf(prop.getProperty("PieceSize"));
            

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        File transferFile = new File(fileName);

        //contains temporary data
        byte [] bytearray = new byte [(int)transferFile.length()];
        
        //read bytes from file into the byte array
        FileInputStream fin = new FileInputStream(transferFile);
        BufferedInputStream bin = new BufferedInputStream(fin); 

        bin.read(bytearray,0,bytearray.length); 

        //output stream provides a channel to communicate with the client side
        OutputStream os = socket.getOutputStream(); 
        System.out.println("Sending Files..."); 

        //write the data from bytearray onto the output stream
        // os.write(bytearray,0,bytearray.length); 
        os.write(bytearray,0,pieceSize); //only sending part of the file

        //close objects
        os.flush(); 
        socket.close(); 
        System.out.println("File transfer complete");



        
    }
}