package old;

import java.net.*;
import java.io.*;
import java.util.*;
public class Client implements Runnable{

    private int portNum = 0;
    // private String fileName = "";
    // private int pieceSize = 0;
    private int fileSize = 0;
    private String ipOrHostname = "";
    private String name = "";
    Thread t;


    public Client (int portNum, String name, String ip) {
        this.portNum = portNum;
        this.name = name;
        this.ipOrHostname = ip;
        t = new Thread(this, "client");

        try (InputStream input = new FileInputStream("config.properties")) {

            Properties prop = new Properties();

            // load a properties file
            prop.load(input);

            //get properties
            // this.fileName = prop.getProperty("FileName");
            // this.pieceSize = Integer.valueOf(prop.getProperty("PieceSize"));
            this.fileSize = Integer.valueOf(prop.getProperty("FileSize"));
            

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        t.start();
    }

    public Socket createConnection(int portNum) throws IOException{
        // Socket clientSocket = new Socket("127.0.0.1", portNum);
        Socket clientSocket = new Socket(ipOrHostname, portNum);
        System.out.println("Client created connection");

        if (clientSocket.isConnected()) return clientSocket;
        else {
            System.out.println("client connection failed");
            return null;
        }
    }

    public void requestFile() throws IOException {

        int bytesRead; //contains the current statistics of the bytes read from input channel ie 
        int currentTot = 0; 

        //define a new socket on local ip 
        // Socket socket = new Socket("127.0.0.1", 15123);
        Socket socket = new Socket(ipOrHostname, portNum);

        //create a new byte array for the expected file size to hold temporary data 
        byte [] bytearray = new byte[fileSize];

        //create a new input stread to read from the socket 
        InputStream is = socket.getInputStream();

        //take from the socket into a file output to copy.doc
        
        // System.out.println(System.getProperty("user.dir"));
        File newDir = new File(System.getProperty("user.dir") + "/" + name);
        boolean createDir = newDir.mkdir();
        // System.out.println(createDir);
        String pathname = newDir.getAbsolutePath() + "/copy.txt";
        System.out.println(pathname);
        FileOutputStream fos = new FileOutputStream(pathname);
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

    public void run() {

        try {
            // createConnection(portNum);
            requestFile();

        } catch (IOException ioe){
            System.err.println("ioe error");;
        }
        

    }

    
}

//     public static void main(String[] args) throws IOException{
        
//         int filesize=2022386; 
//         int bytesRead; //contains the current statistics of the bytes read from input channel ie 
//         int currentTot = 0; //total number of bytes read

//         //define a new socket on local ip 
//         Socket socket = new Socket("127.0.0.1", 15123);

//         //create a new byte array for the expected file size to hold temporary data 
//         byte [] bytearray = new byte[filesize];

//         //create a new input stread to read from the socket 
//         InputStream is = socket.getInputStream();

//         //take from the socket into a file output to copy.doc
//         FileOutputStream fos = new FileOutputStream("copy.txt");
//         BufferedOutputStream bos = new BufferedOutputStream(fos);

//         //total number of bytes read in the input stream
//         bytesRead = is.read(bytearray, 0, bytearray.length);
//         currentTot = bytesRead; //initially total is the number of bytes read

//         //continue to read from the input stream until there is not data left on the stream
//         do {
//             bytesRead = 
//                 is.read(bytearray, currentTot, (bytearray.length-currentTot));
//             if (bytesRead >= 0) currentTot += bytesRead;

//         } while (bytesRead > -1);

//         bos.write(bytearray, 0, currentTot);
//         bos.flush();
//         bos.close();
//         socket.close();
       
//     }
// }