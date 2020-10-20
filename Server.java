import java.net.*;
import java.io.*;

public class Server {

    public static void main(String[] args) throws IOException {

        ServerSocket serverSocket = new ServerSocket(15123);

        //wait for client connection, and then accept connection 
        Socket socket = serverSocket.accept();
        System.out.println("Accepted connection: " + socket);

        //create new file 
        File transferFile = new File ("testFile.txt");

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
        os.write(bytearray,0,bytearray.length); 

        //close objects
        os.flush(); 
        socket.close(); 
        System.out.println("File transfer complete");



        
    }
}