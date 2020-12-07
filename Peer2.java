import java.util.*;
import java.io.*;
import java.net.*;
import java.nio.*;
// import java.nio.file.Files;

public class Peer2 {

    HashMap<Integer, Boolean> neighborBitfieldMap = new HashMap<Integer, Boolean>(); // will need to be changed to be a
                                                                                     // list of lists
    static HashMap<Integer, byte[]> pieceMap = new HashMap<>();
    // static DataOutputStream dout;
    // static DataInputStream din;
    static ByteArrayOutputStream byteOS = new ByteArrayOutputStream();
    static String fileName = "";
    static Scanner sc = new Scanner(System.in);
    static HashMap<String, byte[]> messageTypeMap = createMessageHashMap();
    static String filename = "";
    static int pieceSize = -1;
    static int fileSize = -1;
    static int numPieces = -1;

    public static void main(String[] args) throws Exception {

        int peerIDInt = Integer.valueOf(args[0]);

        //get necessary properties from config
        try (InputStream input = new FileInputStream("config.properties")) {

            Properties prop = new Properties();

            // load a properties file
            prop.load(input);

            // get properties
            fileName = prop.getProperty("FileName");
            pieceSize = Integer.valueOf(prop.getProperty("PieceSize"));
            fileSize = Integer.valueOf(prop.getProperty("FileSize"));
            numPieces = (int) Math.ceil(fileSize / pieceSize);

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        BufferedReader reader = new BufferedReader(new FileReader("P.txt"));

        String line = reader.readLine();

        HashMap<Integer, String[]> peerInfoMap = new HashMap<Integer, String[]>();

        while (line != null) {

            // System.out.println(line);
            String lineArr[] = line.split(" ");
            int tempPeerID = Integer.valueOf(lineArr[0]);
            String peerInfo[] = Arrays.copyOfRange(lineArr, 1, 4);
            peerInfoMap.put(tempPeerID, peerInfo);

            line = reader.readLine();
        }

        for (int id : peerInfoMap.keySet()) {
            System.out.println("[" + id + "] " + Arrays.toString(peerInfoMap.get(id)));

        }

        reader.close();

        String[] myInfo = peerInfoMap.get(peerIDInt);
        int portNum = Integer.valueOf(myInfo[1]);
        int isOwner = Integer.valueOf(myInfo[2]);

        System.out.println("I am " + peerIDInt);

        // int serverPortNum = Integer.valueOf(args[0]);
        // int clientPortNum = Integer.valueOf(args[1]);
        // int peerIDInt = Integer.valueOf(args[2]);
        // int isOwner = Integer.valueOf(args[3]);
        // String host = args[4];

        // System.out.println("Serving at: " + serverPortNum + "\nlistening at: " + clientPortNum + "\npeer id: "
        //         + peerIDInt + "\nis owner: " + isOwner + "\nconnecting to host: " + host);
        ServerSocket listener = new ServerSocket(portNum);
        int clientNum = 1;

        boolean clientConnect = false;

        try {
            while (true) {
                // new Handler(listener.accept(), clientNum).start();
                if (isOwner == 1) {
                    // new Server.Handler(listener.accept(), peerIDInt).start();
                    new Handler(listener.accept(), peerIDInt).start();
                    System.out.println("Client " + clientNum + " is connected!");
                    clientNum++;
                } else {

                    if (!clientConnect) {
                        // new Client(clientPortNum, peerIDInt).run();
                        
                        for (int id : peerInfoMap.keySet()) {
                            System.out.println("[" + id + "] " + Arrays.toString(peerInfoMap.get(id)));
                            if (id < peerIDInt){

                                String[] peerInfo = peerInfoMap.get(id);
                                int connectToPort = Integer.valueOf(peerInfo[1]);
                                String connectToHost = peerInfo[0];
                                System.out.println("I," + peerIDInt + " need to connect to: [" + id + "] " + Arrays.toString(peerInfoMap.get(id)));
                                new Client(connectToPort, connectToHost, peerIDInt).start();
                            }

                    }
                                                // client.run();
                        clientConnect = true;
                    }
                    // new Server.Handler(listener.accept(), peerIDInt).start();
                    new Handler(listener.accept(), peerIDInt).start();
                }
            }
        } finally {
            listener.close();
        }
    }

    static class Handler extends Thread {
        private Socket connection;
        private DataOutputStream server_dout;
        private DataInputStream server_din;
        private int no; // The index number of the client
        private boolean recHandshake = false;
        private boolean sentBitfield = false;

        private byte[] incomingMsg = new byte[32]; // 32 set for handshake message
        // private String fileName = "alice.txt"; // for testing
        // private String fileName = "";
        // private int pieceSize = 64; // for testing

        private int peerIDInt = -1;
        // private int fileSize = -1;
        // private int pieceSize = -1;

        // Scanner sc = new Scanner(System.in);
        // HashMap<String, byte[]> messageTypeMap = createMessageHashMap();
        // HashMap<Integer, byte[]> pieceMap = new HashMap<>();

        byte[] messageLength;
        byte[] messageType;
        byte[] indexField;

        // ByteArrayOutputStream byteOS = new ByteArrayOutputStream();

        public Handler(Socket connection, int peerIDInt) {
            this.connection = connection;
            this.peerIDInt = peerIDInt;
        }

        public void run() {

            // // loading properties from config file
            // try (InputStream input = new FileInputStream("config.properties")) {

            //     Properties prop = new Properties();

            //     // load a properties file
            //     prop.load(input);

            //     // get properties
            //     this.fileName = prop.getProperty("FileName");
            //     this.pieceSize = Integer.valueOf(prop.getProperty("PieceSize"));
            //     this.fileSize = Integer.valueOf(prop.getProperty("FileSize"));

            // } catch (IOException ex) {
            //     ex.printStackTrace();
            // }

            // over here put file into map

            try {

                // initialize Input and Output streams
                server_dout = new DataOutputStream(connection.getOutputStream());
                server_din = new DataInputStream(connection.getInputStream());
                boolean serverLoop = true;

                // creating a byte array of the file
                File file = new File(fileName);
                // byte[] fileInBytes = new byte[fileSize];

                // ByteArrayOutputStream byteOS = new ByteArrayOutputStream();

                // FileInputStream fis = new FileInputStream(file);
                // BufferedInputStream bis = new BufferedInputStream(fis);
                // fileInBytes contains the file alice.txt in bytes
                // bis.read(fileInBytes, 0, fileInBytes.length);

                byte[] buffer = new byte[pieceSize];
                int counter = 1;

                // putting contents of the file into a map
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

                boolean quit = false;

                while (serverLoop) {

                    if (!recHandshake) {

                        // client sends the first handshake message
                        // System.out.println("server waiting for handshake"); testing

                        server_din.read(incomingMsg); // read message into the msg 32 byte buffer

                        // System.out.println("Received message from client " + no + ": " +
                        // Arrays.toString(incomingMsg)); //for tseting

                        // set up handshake message to send after receiving one from the client
                        String headerStr = "P2PFILESHARINGPROJ"; // header
                        byte[] header = headerStr.getBytes(); // header to bytes
                        byte[] zerobits = new byte[10]; // 10 byte zero bits
                        Arrays.fill(zerobits, (byte) 0);
                        byte[] peerID = ByteBuffer.allocate(4).putInt(peerIDInt).array(); // peer ID in byte array

                        // write all information to a byte array
                        byteOS.write(header);
                        byteOS.write(zerobits);
                        byteOS.write(peerID);

                        byte[] handshake = byteOS.toByteArray();

                        // System.out.println("server sending handshake message: " +
                        // Arrays.toString(handshake));

                        sendMessage(handshake); // send handshake message to client

                        incomingMsg = new byte[128]; // reset incoming message buffer, might need to change this number

                    } else if (!sentBitfield) {
                        // iterate through a map of parts and add to the message if it exists

                        String bitstring = "";
                        // int numPieces = (int) Math.ceil(fileSize / pieceSize);
                        int bytesNeeded = (int) Math.ceil(numPieces / 7); // each piece represents a bit, first bit is
                                                                          // sign

                        System.out.println("num pieces: " + numPieces + "\nfile size: " + fileSize);
                        byte bitfield[] = new byte[bytesNeeded];
                        byteOS.reset();

                        for (int i = 1; i <= pieceMap.size(); i++) {

                            if (i % 7 == 0 && i != 0) { // every 8 bits the bitstring must be written out and reset
                                byte b = Byte.parseByte(bitstring, 2);
                                byteOS.write(b);
                                bitstring = "";
                            }

                            if (bitstring.equals("")) { // first bit in the bit string must be 0
                                bitstring = "0";
                            }

                            if (pieceMap.containsKey(i)) { // if the map contains the key, add 1, if not add 0
                                bitstring += "1";
                            } else
                                bitstring += "0";

                            if (i == pieceMap.size()) { // at the end of the map, all remaining bits are 0
                                int bsLength = bitstring.length();
                                int j = 7 - bsLength;

                                for (int k = 0; k < j; k++) {
                                    bitstring += "0";
                                }
                            }
                        }

                        bitfield = byteOS.toByteArray();

                        // 4 byte message length, //1 byte message type, size of bitfield
                        int payload = bitfield.length; // payload is done incorrectly when sending pieces
                        messageLength = ByteBuffer.allocate(4).putInt(payload).array();
                        messageType = ByteBuffer.allocate(1).put(messageTypeMap.get("bitfield")).array();

                        byteOS.reset(); // make sure byteOS is empty
                        byteOS.write(messageLength);
                        byteOS.write(messageType); // should equal binary 7 for "piece"
                        byteOS.write(bitfield);
                        byte[] bitfieldMessage = byteOS.toByteArray();
                        byteOS.flush();
                        byteOS.reset();

                        sendMessage(bitfieldMessage);

                        sentBitfield = true;
                    } else {
                        // might need to change the size of message

                        while (server_din.read(incomingMsg) > -1) {
                            // System.out.println("waiting for client");
                            // System.out.println(din.read(incomingMsg)); // waiting for a client request

                            // retrieve message type
                            byte[] incomingMessageType = Arrays.copyOfRange(incomingMsg, 4, 5);

                            // check the message type
                            if (Arrays.equals(incomingMessageType, messageTypeMap.get("interested"))) {

                                System.out.println("interested functionality");
                                sendPiece(1); // this needs to be changed!
                                incomingMsg = new byte[32];
                                // din.reset();
                            } else if (Arrays.equals(incomingMessageType, messageTypeMap.get("not_interested"))) {
                                System.out.println("not_interested functionality");
                            } else if (Arrays.equals(incomingMessageType, messageTypeMap.get("have"))) {
                                System.out.println("have functionality");
                            } else if (Arrays.equals(incomingMessageType, messageTypeMap.get("request"))) {
                                // if not choked
                                // System.out.println("request message received ");
                                byte[] pieceNumToSend = Arrays.copyOfRange(incomingMsg, 5, 9);
                                int pieceNumInt = ByteBuffer.wrap(pieceNumToSend).getInt();
                                // System.out.println("server recieved index from client:" + pieceNumInt);
                                // System.out.println("index to send from: " + index); //for testing
                                sendPiece(pieceNumInt);
                                incomingMsg = new byte[32];
                                // din.reset();
                                // 9 byte header and 128 byte message payload
                                // int payload = 9+128;
                                // messageLength = ByteBuffer.allocate(4).putInt(payload).array();
                                // messageType =
                                // ByteBuffer.allocate(1).put(messageTypeMap.get("piece")).array();
                                // indexField = ByteBuffer.allocate(4).putInt(pieceNumInt).array(); // index of
                                // starting point

                                // byteOS.reset(); // make sure byteOS is empty
                                // byteOS.write(messageLength);
                                // byteOS.write(messageType); // should equal binary 7 for "piece"
                                // byteOS.write(indexField);
                                // // byteOS.write(fileInBytes, index, pieceSize); // writing the contents of
                                // the file
                                // System.out.println("sending piece " + pieceNumInt);
                                // byte[] pieceBuffer = pieceMap.get(pieceNumInt);
                                // byteOS.write(pieceBuffer);

                                // byte[] sendMessage = byteOS.toByteArray();
                                // sendMessage(sendMessage); // sending the piece message

                                // System.out.println("quit?");
                                // quit = sc.nextBoolean();

                                if (quit) {
                                    server_dout.flush();
                                    // bis.close();
                                    sc.close();
                                    System.out.println("File Transfer Complete.");
                                    serverLoop = false;
                                }
                            }

                        }

                    }
                    recHandshake = true; // received handshake

                }
            } catch (IOException ioException) {
                System.out.println("Disconnect with Client " + no);
            } finally {
                // Close connections
                try {
                    server_din.close();
                    server_dout.close();
                    connection.close();
                } catch (IOException ioException) {
                    System.out.println("Disconnect with Client " + no);
                }
            }
        }

        void sendMessage(byte[] msg) {
            try {
                server_dout.write(msg);

            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }

        void sendPiece(int pieceNumInt) throws IOException {
            // 9 byte header and 128 byte message payload
            int payload = 9 + 128; //this needs to be changed! 
            messageLength = ByteBuffer.allocate(4).putInt(payload).array();
            messageType = ByteBuffer.allocate(1).put(messageTypeMap.get("piece")).array();
            indexField = ByteBuffer.allocate(4).putInt(pieceNumInt).array(); // index of starting point

            byteOS.reset(); // make sure byteOS is empty
            byteOS.write(messageLength);
            byteOS.write(messageType); // should equal binary 7 for "piece"
            byteOS.write(indexField);
            // byteOS.write(fileInBytes, index, pieceSize); // writing the contents of the
            // file
            // System.out.println("sending piece " + pieceNumInt);
            byte[] pieceBuffer = pieceMap.get(pieceNumInt);
            byteOS.write(pieceBuffer);

            byte[] sendMessage = byteOS.toByteArray();
            sendMessage(sendMessage); // sending the piece message

        }

    }

    static class Client extends Thread{
        Socket requestSocket;
        DataInputStream client_din;
        DataOutputStream client_dout;

        boolean recHandshake = false;
        boolean start = false;
        boolean sentBitfield = false; // will need to be changed once we connect peers in a different way

        // will be set from config and setup
        int peerIDInt = -1;
        int portNum = -1;
        int serverId = -1; // will be used to determine if the server is correct

        // ByteArrayOutputStream byteOS = new ByteArrayOutputStream();
        String hostname = "localhost"; // can be changed through constructor

        byte[] messageLength;
        byte[] messageType;
        byte[] indexField = new byte[4];

        // Scanner sc = new Scanner(System.in);
        // int fileSize = 148481; // for testing
        int fileSize = -1;

        // byte[] finalFileInBytes;

        // int pieceSize = 64; // for testing
        // int pieceSize = -1;
        // int numPieces = 0;

        int piecesReceived = 0;

        
        HashMap<Integer, Boolean> neighborBitfieldMap = new HashMap<Integer, Boolean>(); // will need to be changed to
                                                                                         // be a list of lists
        // HashMap<Integer, ArrayList<Integer>> neighborBitfieldMap = new
        // HashMap<Integer, ArrayList<Integer>>();
        // private String fileName = ""; // get from config

        // public static void main(String args[]) {

        // // main will not be run when peer process runs

        // int portNum = Integer.valueOf(args[0]);
        // // int portNum = Integer.valueOf(args[2]);
        // // int portNum = 8000; // for testing

        // int peerIDInt = Integer.valueOf(args[1]);
        // // int peerIDInt = 1002; //for testing

        // Client client = new Client(portNum, peerIDInt);
        // client.run();
        // }

        public Client(int portNum, int peerIDInt) { // used when running java Client
            this.portNum = portNum;
            this.peerIDInt = peerIDInt;

        }

        public Client(int portNum, String hostname, int peerIDInt) { // used when running java PeerProcess
            this.peerIDInt = peerIDInt;
            this.hostname = hostname;
            this.portNum = portNum;
           
        }

        private void startClient() throws UnknownHostException, IOException {
            requestSocket = new Socket(hostname, portNum);
        }

        public void run() {
            try {
                // System.out.println("created a new client");
                startClient();

                System.out.println(
                        "I am peer " + peerIDInt + " trying to connect to " + hostname + " at port " + portNum);

                 
                System.out.println("trying to connect to port " + portNum);
                System.out.println("Connected to localhost in port " + portNum);

                // try (InputStream input = new FileInputStream("config.properties")) {

                // Properties prop = new Properties();

                // // load a properties file
                // prop.load(input);

                // // get properties
                // this.fileName = prop.getProperty("FileName");
                // this.pieceSize = Integer.valueOf(prop.getProperty("PieceSize"));
                // this.fileSize = Integer.valueOf(prop.getProperty("FileSize"));
                // finalFileInBytes = new byte[fileSize];
                // numPieces = (int) Math.ceil(fileSize / pieceSize);

                // } catch (IOException ex) {
                // ex.printStackTrace();
                // }

                byteOS.reset();
                client_din = new DataInputStream(requestSocket.getInputStream());
                client_dout = new DataOutputStream(requestSocket.getOutputStream());
                client_dout.flush();
                boolean clientLoop = true;

                // setting up folder & file for copy to be made in
                // File newDir = new File(System.getProperty("user.dir") + "/" + peerIDInt);
                // newDir.mkdir();

                // String pathname = newDir.getAbsolutePath() + "/" + fileName;

                // FileOutputStream fos = new FileOutputStream(pathname);
                // BufferedOutputStream bos = new BufferedOutputStream(fos);
                // int largestByte = 0;
                HashMap<Integer, byte[]> pieceMap = new HashMap<>(); // will hold all of the pieces
                byte[] buffer = new byte[pieceSize];

                while (clientLoop) {

                    if (!recHandshake) { // first time connection, need to send handshake

                        // creating a byte array message to send as the handshake

                        String headerStr = "P2PFILESHARINGPROJ"; // header
                        byte[] header = headerStr.getBytes(); // header to bytes
                        byte[] zerobits = new byte[10]; // 10 byte zero bits
                        Arrays.fill(zerobits, (byte) 0);
                        byte[] peerID = ByteBuffer.allocate(4).putInt(peerIDInt).array(); // peer ID in byte array
                                                                                          // format

                        byte[] serverPeerID = ByteBuffer.allocate(4).putInt(1001).array(); // used for testing only

                        // write all information to a byte array
                        byteOS.write(header);
                        byteOS.write(zerobits);
                        byteOS.write(peerID);

                        byte[] handshake = byteOS.toByteArray();

                        // System.out.println("client sending handshake message: " +
                        // Arrays.toString(handshake));

                        sendMessage(handshake); // client sends handshake message to server

                        // System.out.println("client waiting for handshake");

                        byte[] incomingHandshake = new byte[32]; // empty byte array for incoming handshake

                        client_din.read(incomingHandshake); // read in the incoming handshake

                        // System.out.println("Received message from server: " +
                        // Arrays.toString(incomingHandshake));

                        // checking to make sure the correct peerID has been connected
                        byte[] checkServerID = Arrays.copyOfRange(incomingHandshake, 28, 32);
                        // int checkServerIDInt = ByteBuffer.wrap(checkServerID).getInt();

                        // System.out.println("Receieved peer id:" + Arrays.toString(checkServerID));
                        // System.out.println("Expected peer id:" + Arrays.toString(serverPeerID));
                        if (Arrays.equals(serverPeerID, checkServerID)) {
                            System.out.println("peer ID confirmed.");
                            // ArrayList<Integer> tempList = new ArrayList<Integer>();
                            // neighborBitfieldMap.put(checkServerIDInt, tempList);

                        } else {
                            System.out.println("incorrect peerID received");
                            // if this is the case the handshake needs to be redone
                            // have not figured this part out yet
                        }
                        recHandshake = true; // handshake received, do not do this part again
                        byteOS.reset();

                    } else { // every message that is not the handshake
                        boolean quit = false;

                        // client sends the first message, for now, an interested message
                        if (!start) {
                            messageLength = ByteBuffer.allocate(4).putInt(128).array();
                            messageType = ByteBuffer.allocate(1).put(messageTypeMap.get("interested")).array();

                            byteOS.reset();
                            byteOS.write(messageLength);
                            byteOS.write(messageType);

                            byte[] msg = byteOS.toByteArray();
                            sendMessage(msg); // requesting the piece
                            System.out.println("sending interested");

                            start = true;
                        }
                        // this is the request functionality only for now.

                        // inst ead of taking user input, the p2p process will use the bittorrent
                        // protocol to request the specific bytes
                        // all of this needs to be moved into after the bitfield

                        // waiting for a reply from the server
                        byte[] incomingMessage = new byte[130]; // will need to change the size of this
                        // System.out.println("waiting for server reply");

                        // read from 0 to 8 for the header, 9 exclusive
                        // [0 - 3] message length
                        // [4] message type
                        // [5 - 8] index field
                        client_din.read(incomingMessage, 0, 9);

                        byte[] messageType = Arrays.copyOfRange(incomingMessage, 4, 5); // getting the message type

                        if (Arrays.equals(messageType, messageTypeMap.get("choke"))) {
                            System.out.println("choke functionality");
                        } else if (Arrays.equals(messageType, messageTypeMap.get("bitfield"))) {
                            System.out.println("client received bitfield message");
                            byte[] bitfieldLength = Arrays.copyOfRange(incomingMessage, 0, 4);
                            System.out.println(bitfieldLength.length);
                            int bMsize = ByteBuffer.wrap(bitfieldLength).getInt();
                            System.out.println(bMsize);
                            byte[] bitfieldMessage = new byte[ByteBuffer.wrap(bitfieldLength).getInt()];

                            client_din.read(bitfieldMessage);
                            int counter = 1;

                            for (int i = 0; i < bitfieldMessage.length; i++) {
                                String bs = Integer.toBinaryString(bitfieldMessage[i]);
                                for (int j = 0; j < bs.length(); j++) {
                                    if (bs.charAt(j) == '0') {
                                        // System.out.println("server does not have piece " + counter);
                                        neighborBitfieldMap.put(counter, false);
                                    } else if (bs.charAt(j) == '1') {
                                        // System.out.println("server has piece " + counter);
                                        neighborBitfieldMap.put(counter, true);
                                    }
                                    counter++;
                                }
                            }

                            // for (int piece : neighborBitfieldMap.keySet()) {
                            // System.out.println(piece + " " + neighborBitfieldMap.get(piece));
                            // }

                        } else if (Arrays.equals(messageType, messageTypeMap.get("unchoke"))) {
                            System.out.println("unchoke functionality");
                        } else if (Arrays.equals(messageType, messageTypeMap.get("have"))) {
                            // in response to this, send back interested or not interested
                            System.out.println("have functionality");
                        } else if (Arrays.equals(messageType, messageTypeMap.get("piece"))) {

                            // System.out.println ("piece functionality");

                            byte[] fileIndex = Arrays.copyOfRange(incomingMessage, 5, 9);
                            int index = ByteBuffer.wrap(fileIndex).getInt(); // number corresponds to the number in the
                                                                             // map
                            // System.out.println("recieved piece " + index);
                            // System.out.println("client recieved index from server: " + index);
                            // din.read(finalFileInBytes, index, pieceSize); //should read into file byte
                            // array from specified index
                            // if (index+pieceSize > largestByte){
                            // largestByte = index+pieceSize; //only up to the largest byte will be exported
                            // to the file.
                            // }
                            client_din.read(buffer, 0, pieceSize);
                            piecesReceived++;
                            // System.out.println("bytes read: " + bytesRead);
                            byte[] newPiece = new byte[pieceSize];
                            newPiece = buffer.clone();
                            pieceMap.put(index, newPiece);
                            // send another request message
                            // System.out.print("request index: ");

                            // int start = sc.nextInt();
                            // int pieceNum = sc.nextInt();
                            // System.out.println("num pieces: " +numPieces);
                            if (piecesReceived < numPieces) {
                                int pieceNum = index + 1; // request the next index
                                // System.out.println("requesting piece " + pieceNum);
                                messageLength = ByteBuffer.allocate(4).putInt(128).array();
                                messageType = ByteBuffer.allocate(1).put(messageTypeMap.get("request")).array(); // should
                                                                                                                 // be 6
                                // indexField = ByteBuffer.allocate(4).putInt(start).array(); //index of
                                // starting point
                                indexField = ByteBuffer.allocate(4).putInt(pieceNum).array(); // index of piece num

                                // writing request message to the byte output stream
                                byteOS.reset();
                                byteOS.write(messageLength);
                                byteOS.write(messageType);
                                byteOS.write(indexField);

                                byte[] msg = byteOS.toByteArray();
                                sendMessage(msg); // requesting the piece
                            } else {
                                quit = true;
                            }

                        }

                        // request message has been sent, now wait for response of piece

                        // System.out.println("quit?");
                        // boolean quit = sc.nextBoolean();

                        if (quit) {
                            // when done, we need to write to the client's folder
                            // bos.write(finalFileInBytes, 0, largestByte); //writes final into
                            // peerid/alice.txt

                            // for (Map.Entry<Integer,byte[]> entry: pieceMap.entrySet()) {
                            // byteOS.write(entry.getValue());
                            // }
                            System.out.println("Client is gathering file pieces to combine.");
                            byteOS.reset();
                            for (int i = 1; i <= pieceMap.size(); i++) {
                                // System.out.println("piece number " + i);
                                byteOS.write(pieceMap.get(i)); // write all pieces from map into byteOS
                            }
                            byte[] finalFile = byteOS.toByteArray();

                            File newDir = new File(System.getProperty("user.dir") + "/" + peerIDInt);
                            newDir.mkdir();
                            // String pathname = newDir.getAbsolutePath() + "/" + fileName;
                            String pathname = newDir.getAbsolutePath() + "/" + "copy.txt";
                            File copiedFile = new File(pathname); // get the directory for this
                            try (FileOutputStream fos = new FileOutputStream(copiedFile)) {
                                fos.write(finalFile);
                            }

                            System.out.println("Client: final file has been written");
                            byteOS.flush();
                            byteOS.reset();
                            sc.close();
                            // bos.close();
                            client_din.close();
                            client_dout.flush();
                            client_dout.close();
                            clientLoop = false; // this is just here for the purpose of testing
                        }
                    }

                }
            } catch (ConnectException e) {
                System.err.println("Connection refused. You need to initiate a server first.");
            } catch (UnknownHostException unknownHost) {
                System.err.println("You are trying to connect to an unknown host!");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            } finally {
                // Close connections
                try {
                    requestSocket.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        }

        void sendMessage(byte[] msg) {
            try {
                client_dout.write(msg);

            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    // map used for message typing
    static HashMap<String, byte[]> createMessageHashMap() {
        HashMap<String, byte[]> map = new HashMap<String, byte[]>();
        byte zero = 0b000;
        byte one = 0b001;
        byte two = 0b010;
        byte three = 0b011;
        byte four = 0b100;
        byte five = 0b101;
        byte six = 0b110;
        byte seven = 0b111;
        // byte zeroArr[] = ByteBuffer.allocate(1).put(zero).array();
        map.put("choke", ByteBuffer.allocate(1).put(zero).array());
        map.put("unchoke", ByteBuffer.allocate(1).put(one).array());
        map.put("interested", ByteBuffer.allocate(1).put(two).array());
        map.put("not_interested", ByteBuffer.allocate(1).put(three).array());
        map.put("have", ByteBuffer.allocate(1).put(four).array());
        map.put("bitfield", ByteBuffer.allocate(1).put(five).array());
        map.put("request", ByteBuffer.allocate(1).put(six).array());
        map.put("piece", ByteBuffer.allocate(1).put(seven).array());

        return map;
    }

    // static void sendMessage(byte[] msg) {
    //     try {
    //         dout.write(msg);

    //     } catch (IOException ioException) {
    //         ioException.printStackTrace();
    //     }
    // }

}