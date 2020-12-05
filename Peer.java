import java.util.*;
import java.io.*;
import java.net.*;
import java.nio.*;

public class Peer {

    public static void main(String[] args) throws Exception {

        System.out.println("The server is running.");

        int serverPortNum = Integer.valueOf(args[0]);
        int clientPortNum = Integer.valueOf(args[1]);
        int peerIDInt = Integer.valueOf(args[2]);
        int isOwner = Integer.valueOf(args[3]);

        System.out.println("Serving at: " + serverPortNum + "\nlistening at: "
            + clientPortNum + "\npeer id: " + peerIDInt + "\nis owner: " + isOwner);
        ServerSocket listener = new ServerSocket(serverPortNum);
        int clientNum = 1;
        
        
        boolean clientConnect = false;

		try {
			while (true) {                
                // new Handler(listener.accept(), clientNum).start();
                if (isOwner == 1){
                    new Server.Handler(listener.accept(), peerIDInt).start();
				    System.out.println("Client " + clientNum + " is connected!");
				    clientNum++;
                } else {
                    
                    if (!clientConnect) {
                        Client client = new Client(clientPortNum, peerIDInt);
                        client.run();
                        clientConnect = true;
                    }
                    new Server.Handler(listener.accept(), peerIDInt).start();
                }
            }
		} finally {
			listener.close();
        }       
    }

    // class Server {

    /**
         * A handler thread class. Handlers are spawned from the listening loop and are
         * responsible for dealing with a single client's requests.
         */
        // class Handler extends Thread {
        //     private Socket connection;
        //     private DataOutputStream dout;
        //     private DataInputStream din;
        //     private int no; // The index number of the client
        //     private boolean recHandshake = false;
        //     private boolean sentBitfield = false;

        //     private byte[] incomingMsg = new byte[32]; // 32 set for handshake message
        //     // private String fileName = "alice.txt"; // for testing
        //     private String fileName = "";
        //     // private int pieceSize = 64; // for testing

        //     private int peerIDInt = -1;
        //     private int fileSize = -1;
        //     private int pieceSize = -1;

        //     Scanner sc = new Scanner(System.in);
        //     HashMap<String, byte[]> messageTypeMap = createMessageHashMap();
        //     HashMap<Integer, byte[]> pieceMap = new HashMap<>();

        //     byte[] messageLength;
        //     byte[] messageType;
        //     byte[] indexField;

        //     ByteArrayOutputStream byteOS = new ByteArrayOutputStream();

        //     public Handler(Socket connection, int peerIDInt) {
        //         this.connection = connection;
        //         this.peerIDInt = peerIDInt;
        //     }

        //     public void run() {

        //         // loading properties from config file
        //         try (InputStream input = new FileInputStream("config.properties")) {

        //             Properties prop = new Properties();

        //             // load a properties file
        //             prop.load(input);

        //             // get properties
        //             this.fileName = prop.getProperty("FileName");
        //             this.pieceSize = Integer.valueOf(prop.getProperty("PieceSize"));
        //             this.fileSize = Integer.valueOf(prop.getProperty("FileSize"));

        //         } catch (IOException ex) {
        //             ex.printStackTrace();
        //         }

        //         // over here put file into map

        //         try {

        //             // initialize Input and Output streams
        //             dout = new DataOutputStream(connection.getOutputStream());
        //             din = new DataInputStream(connection.getInputStream());
        //             boolean serverLoop = true;

        //             // creating a byte array of the file
        //             File file = new File(fileName);
        //             // byte[] fileInBytes = new byte[fileSize];

        //             // ByteArrayOutputStream byteOS = new ByteArrayOutputStream();

        //             // FileInputStream fis = new FileInputStream(file);
        //             // BufferedInputStream bis = new BufferedInputStream(fis);
        //             // fileInBytes contains the file alice.txt in bytes
        //             // bis.read(fileInBytes, 0, fileInBytes.length);

        //             byte[] buffer = new byte[pieceSize];
        //             int counter = 1;

        //             // putting contents of the file into a map
        //             try (FileInputStream fileInputStream = new FileInputStream(file);
        //                     BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream)) {

        //                 // add each piece of the file to a map
        //                 while ((bufferedInputStream.read(buffer)) > 0) {

        //                     byteOS.write(buffer, 0, pieceSize);
        //                     byte[] piece = byteOS.toByteArray();
        //                     pieceMap.put(counter, piece);
        //                     byteOS.flush();
        //                     byteOS.reset();
        //                     counter++;
        //                 }
        //             }

        //             boolean quit = false;

        //             while (serverLoop) {

        //                 if (!recHandshake) {

        //                     // client sends the first handshake message
        //                     // System.out.println("server waiting for handshake"); testing

        //                     din.read(incomingMsg); // read message into the msg 32 byte buffer

        //                     // System.out.println("Received message from client " + no + ": " +
        //                     // Arrays.toString(incomingMsg)); //for tseting

        //                     // set up handshake message to send after receiving one from the client
        //                     String headerStr = "P2PFILESHARINGPROJ"; // header
        //                     byte[] header = headerStr.getBytes(); // header to bytes
        //                     byte[] zerobits = new byte[10]; // 10 byte zero bits
        //                     Arrays.fill(zerobits, (byte) 0);
        //                     byte[] peerID = ByteBuffer.allocate(4).putInt(peerIDInt).array(); // peer ID in byte array

        //                     // write all information to a byte array
        //                     byteOS.write(header);
        //                     byteOS.write(zerobits);
        //                     byteOS.write(peerID);

        //                     byte[] handshake = byteOS.toByteArray();

        //                     // System.out.println("server sending handshake message: " +
        //                     // Arrays.toString(handshake));

        //                     sendMessage(handshake); // send handshake message to client

        //                     incomingMsg = new byte[128]; // reset incoming message buffer, might need to change this
        //                                                  // number

        //                 } else if (!sentBitfield) {
        //                     // iterate through a map of parts and add to the message if it exists

        //                     String bitstring = "";
        //                     int numPieces = (int) Math.ceil(fileSize / pieceSize);
        //                     int bytesNeeded = (int) Math.ceil(numPieces / 7); // each piece represents a bit, first bit
        //                                                                       // is
        //                                                                       // sign
        //                     byte bitfield[] = new byte[bytesNeeded];
        //                     byteOS.reset();

        //                     for (int i = 1; i <= pieceMap.size(); i++) {

        //                         if (i % 7 == 0 && i != 0) { // every 8 bits the bitstring must be written out and reset
        //                             byte b = Byte.parseByte(bitstring, 2);
        //                             byteOS.write(b);
        //                             bitstring = "";
        //                         }

        //                         if (bitstring.equals("")) { // first bit in the bit string must be 0
        //                             bitstring = "0";
        //                         }

        //                         if (pieceMap.containsKey(i)) { // if the map contains the key, add 1, if not add 0
        //                             bitstring += "1";
        //                         } else
        //                             bitstring += "0";

        //                         if (i == pieceMap.size()) { // at the end of the map, all remaining bits are 0
        //                             int bsLength = bitstring.length();
        //                             int j = 7 - bsLength;

        //                             for (int k = 0; k < j; k++) {
        //                                 bitstring += "0";
        //                             }
        //                         }
        //                     }

        //                     bitfield = byteOS.toByteArray();

        //                     // 4 byte message length, //1 byte message type, size of bitfield
        //                     int payload = bitfield.length; // payload is done incorrectly when sending pieces
        //                     messageLength = ByteBuffer.allocate(4).putInt(payload).array();
        //                     messageType = ByteBuffer.allocate(1).put(messageTypeMap.get("bitfield")).array();

        //                     byteOS.reset(); // make sure byteOS is empty
        //                     byteOS.write(messageLength);
        //                     byteOS.write(messageType); // should equal binary 7 for "piece"
        //                     byteOS.write(bitfield);
        //                     byte[] bitfieldMessage = byteOS.toByteArray();
        //                     byteOS.flush();
        //                     byteOS.reset();

        //                     sendMessage(bitfieldMessage);

        //                     sentBitfield = true;
        //                 } else {
        //                     // might need to change the size of message

        //                     while (din.read(incomingMsg) > -1) {
        //                         // System.out.println("waiting for client");
        //                         // System.out.println(din.read(incomingMsg)); // waiting for a client request

        //                         // retrieve message type
        //                         byte[] incomingMessageType = Arrays.copyOfRange(incomingMsg, 4, 5);

        //                         // check the message type
        //                         if (Arrays.equals(incomingMessageType, messageTypeMap.get("interested"))) {

        //                             System.out.println("interested functionality");
        //                             sendPiece(1); // this needs to be changed!
        //                             incomingMsg = new byte[32];
        //                             // din.reset();
        //                         } else if (Arrays.equals(incomingMessageType, messageTypeMap.get("not_interested"))) {
        //                             System.out.println("not_interested functionality");
        //                         } else if (Arrays.equals(incomingMessageType, messageTypeMap.get("have"))) {
        //                             System.out.println("have functionality");
        //                         } else if (Arrays.equals(incomingMessageType, messageTypeMap.get("request"))) {
        //                             // if not choked
        //                             System.out.println("request message received ");
        //                             byte[] pieceNumToSend = Arrays.copyOfRange(incomingMsg, 5, 9);
        //                             int pieceNumInt = ByteBuffer.wrap(pieceNumToSend).getInt();
        //                             System.out.println("server recieved index from client:" + pieceNumInt);
        //                             // System.out.println("index to send from: " + index); //for testing
        //                             sendPiece(pieceNumInt);
        //                             incomingMsg = new byte[32];
        //                             // din.reset();
        //                             // 9 byte header and 128 byte message payload
        //                             // int payload = 9+128;
        //                             // messageLength = ByteBuffer.allocate(4).putInt(payload).array();
        //                             // messageType =
        //                             // ByteBuffer.allocate(1).put(messageTypeMap.get("piece")).array();
        //                             // indexField = ByteBuffer.allocate(4).putInt(pieceNumInt).array(); // index of
        //                             // starting point

        //                             // byteOS.reset(); // make sure byteOS is empty
        //                             // byteOS.write(messageLength);
        //                             // byteOS.write(messageType); // should equal binary 7 for "piece"
        //                             // byteOS.write(indexField);
        //                             // // byteOS.write(fileInBytes, index, pieceSize); // writing the contents of
        //                             // the file
        //                             // System.out.println("sending piece " + pieceNumInt);
        //                             // byte[] pieceBuffer = pieceMap.get(pieceNumInt);
        //                             // byteOS.write(pieceBuffer);

        //                             // byte[] sendMessage = byteOS.toByteArray();
        //                             // sendMessage(sendMessage); // sending the piece message

        //                             // System.out.println("quit?");
        //                             // quit = sc.nextBoolean();

        //                             if (quit) {
        //                                 dout.flush();
        //                                 // bis.close();
        //                                 sc.close();
        //                                 System.out.println("File Transfer Complete.");
        //                                 serverLoop = false;
        //                             }
        //                         }

        //                     }

        //                 }
        //                 recHandshake = true; // received handshake

        //             }
        //         } catch (IOException ioException) {
        //             System.out.println("Disconnect with Client " + no);
        //         } finally {
        //             // Close connections
        //             try {
        //                 din.close();
        //                 dout.close();
        //                 connection.close();
        //             } catch (IOException ioException) {
        //                 System.out.println("Disconnect with Client " + no);
        //             }
        //         }
        //     }

        //     void sendMessage(byte[] msg) {
        //         try {
        //             dout.write(msg);

        //         } catch (IOException ioException) {
        //             ioException.printStackTrace();
        //         }
        //     }

        //     void sendPiece(int pieceNumInt) throws IOException {
        //         // 9 byte header and 128 byte message payload
        //         int payload = 9 + 128;
        //         messageLength = ByteBuffer.allocate(4).putInt(payload).array();
        //         messageType = ByteBuffer.allocate(1).put(messageTypeMap.get("piece")).array();
        //         indexField = ByteBuffer.allocate(4).putInt(pieceNumInt).array(); // index of starting point

        //         byteOS.reset(); // make sure byteOS is empty
        //         byteOS.write(messageLength);
        //         byteOS.write(messageType); // should equal binary 7 for "piece"
        //         byteOS.write(indexField);
        //         // byteOS.write(fileInBytes, index, pieceSize); // writing the contents of the
        //         // file
        //         System.out.println("sending piece " + pieceNumInt);
        //         byte[] pieceBuffer = pieceMap.get(pieceNumInt);
        //         byteOS.write(pieceBuffer);

        //         byte[] sendMessage = byteOS.toByteArray();
        //         sendMessage(sendMessage); // sending the piece message

        //     }

        //     // map used for message typing
        //     HashMap<String, byte[]> createMessageHashMap() {
        //         HashMap<String, byte[]> map = new HashMap<String, byte[]>();
        //         byte zero = 0b000;
        //         byte one = 0b001;
        //         byte two = 0b010;
        //         byte three = 0b011;
        //         byte four = 0b100;
        //         byte five = 0b101;
        //         byte six = 0b110;
        //         byte seven = 0b111;
        //         // byte zeroArr[] = ByteBuffer.allocate(1).put(zero).array();
        //         map.put("choke", ByteBuffer.allocate(1).put(zero).array());
        //         map.put("unchoke", ByteBuffer.allocate(1).put(one).array());
        //         map.put("interested", ByteBuffer.allocate(1).put(two).array());
        //         map.put("not_interested", ByteBuffer.allocate(1).put(three).array());
        //         map.put("have", ByteBuffer.allocate(1).put(four).array());
        //         map.put("bitfield", ByteBuffer.allocate(1).put(five).array());
        //         map.put("request", ByteBuffer.allocate(1).put(six).array());
        //         map.put("piece", ByteBuffer.allocate(1).put(seven).array());

        //         return map;
        //     }
        // }
    // }

}