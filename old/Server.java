package old;

import java.net.*;
import java.io.*;
import java.util.*;

public class Server implements Runnable{

    private int portNum = 0;
    private ServerSocket serverSocket = null;
    // private Socket socket = null;
    private OutputStream os = null;
    private String fileName = "";
    private int pieceSize = 0;
    private InetAddress ip = null;
    private String host = "";
    Thread t;

    public Server(int portNum, String ip) throws UnknownHostException{
        this.portNum = portNum;
        this.ip = InetAddress.getByName(ip);
        t = new Thread(this, "server");
        try (InputStream input = new FileInputStream("config.properties")) {

            Properties prop = new Properties();

            // load a properties file
            prop.load(input);

            //get properties
            this.fileName = prop.getProperty("FileName");
            this.pieceSize = Integer.valueOf(prop.getProperty("PieceSize"));
            

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        t.start();
    }

    public Server(int portNum) throws UnknownHostException{
        this.portNum = portNum;
        // this.ip = InetAddress.getByName(ip);
        t = new Thread(this, "server");
        try (InputStream input = new FileInputStream("config.properties")) {

            Properties prop = new Properties();

            // load a properties file
            prop.load(input);

            //get properties
            this.fileName = prop.getProperty("FileName");
            this.pieceSize = Integer.valueOf(prop.getProperty("PieceSize"));
            

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        t.start();
    }
 
    public void createServerSocket(int portNum) throws IOException {
        this.serverSocket = new ServerSocket(portNum, 100, ip);
        System.out.println("Created server socket");
    }

    public Socket acceptConnection() throws IOException {
        Socket socket = serverSocket.accept();
        System.out.println("accepted connection on socket: " + socket);

        if (socket.isConnected()) return socket;
        else {
            System.out.println("socket connection failed"); 
            return null;
        }
    }

    public void transferFile(Socket socket) throws IOException{

        File transferFile = new File(this.fileName);

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
        bin.close();
    }

    public void shutdown(Socket socket) throws IOException{
        // this.os.close();
        socket.close();
    }

    public void run(){
        try {
            createServerSocket(portNum);
            while (true) {
                System.out.println("server is waiting for connections on port " + portNum);
                Socket s = acceptConnection();
                while(true){
                    transferFile(s);
                    shutdown(s);

                }
                
            }

        } 
        catch (IOException ioe){
            System.out.println("error at run(), could not accept connection.");
        }
    }

    // public static void main(String[] args) throws IOException {

    //     ServerSocket serverSocket = new ServerSocket(15123);

    //     //wait for client connection, and then accept connection 
    //     Socket socket = serverSocket.accept();
    //     System.out.println("Accepted connection: " + socket);

    //     //create new file 
    //     // File transferFile;
    //     String fileName = "";
    //     int pieceSize = 0;

    //     try (InputStream input = new FileInputStream("config.properties")) {

    //         Properties prop = new Properties();

    //         // load a properties file
    //         prop.load(input);

    //         //get properties
    //         fileName = prop.getProperty("FileName");
    //         pieceSize = Integer.valueOf(prop.getProperty("PieceSize"));
            

    //     } catch (IOException ex) {
    //         ex.printStackTrace();
    //     }

    //     File transferFile = new File(fileName);

    //     //contains temporary data
    //     byte [] bytearray = new byte [(int)transferFile.length()];
        
    //     //read bytes from file into the byte array
    //     FileInputStream fin = new FileInputStream(transferFile);
    //     BufferedInputStream bin = new BufferedInputStream(fin); 

    //     bin.read(bytearray,0,bytearray.length); 

    //     //output stream provides a channel to communicate with the client side
    //     OutputStream os = socket.getOutputStream(); 
    //     System.out.println("Sending Files..."); 

    //     //write the data from bytearray onto the output stream
    //     // os.write(bytearray,0,bytearray.length); 
    //     os.write(bytearray,0,pieceSize); //only sending part of the file

    //     //close objects
    //     os.flush(); 
    //     socket.close(); 
    //     System.out.println("File transfer complete");



        
    // }
}