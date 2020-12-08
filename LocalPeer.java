import java.util.*;

import java.util.logging.*;

import javax.print.attribute.standard.NumberUpSupported;

import java.io.*;
import java.net.*;
import java.nio.*;
// import java.nio.file.Files;

public class LocalPeer {

    static Logger logger = Logger.getLogger("BitTorrentLog");
    static FileHandler fh;
    static File newDir;

    // keep track of all neighbors and what they have
    static HashMap<Integer, HashMap<Integer, Boolean>> neighborsPieceMap = new HashMap<Integer, HashMap<Integer, Boolean>>();

    // keep track of which file pieces self has
    static HashMap<Integer, byte[]> pieceMap = new HashMap<>();

    // keep track of neighbor download rates
    static HashMap<Integer, Integer> downloadRates = new HashMap<>();

    // keep track of which peers are done
    static HashMap<Integer, Boolean> peersDone = new HashMap<>();

    // connection information about all peers
    static HashMap<Integer, String[]> peerInfoMap = new HashMap<Integer, String[]>();

    static ByteArrayOutputStream byteOS = new ByteArrayOutputStream();

    // to be changed with config file
    static String fileName = "";
    static int pieceSize = -1;
    static int fileSize = -1;
    static int numPieces = -1;

    // to be changed with peer info
    static int portNum = -1;
    static int isOwner = -1;

    // will be updated as we collect pieces
    static int piecesOwned = 0;

    // used for checking message types
    static HashMap<String, byte[]> messageTypeMap = createMessageHashMap();

    static ArrayList<Socket> clientList = new ArrayList<Socket>();

    public static void main(String[] args) throws Exception {

        // self if
        int peerIDInt = Integer.valueOf(args[0]);

        // creating a new log for self
        newDir = new File(System.getProperty("user.dir") + "/" + peerIDInt);
        newDir.mkdir();
        String logPathname = newDir.getAbsolutePath() + "/" + "BitTorrent.log";

        try {

            fh = new FileHandler(logPathname);
            logger.addHandler(fh);
            logger.setUseParentHandlers(false);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);

        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // get necessary properties from config
        try (InputStream input = new FileInputStream("config.properties")) {

            Properties prop = new Properties();

            // load a properties file
            prop.load(input);

            // get properties
            fileName = prop.getProperty("FileName");
            pieceSize = Integer.valueOf(prop.getProperty("PieceSize"));
            fileSize = Integer.valueOf(prop.getProperty("FileSize"));
            numPieces = (int) Math.ceil(fileSize / pieceSize); // total number of pieces we will need to transfer

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        // used to read peer information
        BufferedReader reader = new BufferedReader(new FileReader("P.txt"));

        String line = reader.readLine();

        // store the connection info of each peer
        while (line != null) {
            String lineArr[] = line.split(" ");
            int tempPeerID = Integer.valueOf(lineArr[0]);
            String peerInfo[] = Arrays.copyOfRange(lineArr, 1, 4);
            peerInfoMap.put(tempPeerID, peerInfo);
            line = reader.readLine();
        }

        reader.close();

        // get own info to initiate server
        String[] myInfo = peerInfoMap.get(peerIDInt);
        portNum = Integer.valueOf(myInfo[1]);
        isOwner = Integer.valueOf(myInfo[2]);

        // start a new listener at the port number
        ServerSocket listener = new ServerSocket(portNum);

        // only want to do this during the first iteration of the loop
        boolean clientConnect = false;

        try {
            while (true) { // continue listening for incoming connections
                if (isOwner == 1) { // file owners serve only as uploaders
                    new Handler(listener.accept(), peerIDInt).start(); // start a new Handler for each incoming
                                                                       // connection
                } else {
                    if (!clientConnect) {
                        // new Client(clientPortNum, peerIDInt).run();


                        // pass in the expected peer id to check if it is correct 
                        for (int id : peerInfoMap.keySet()) {
                            // connect to each peer that already has a server running
                            // for this to work, peers need to be established in peerNum order
                            if (id < peerIDInt) {
                            
                                String[] peerInfo = peerInfoMap.get(id);
                                String connectToHost = peerInfo[0];
                                int connectToPort = Integer.valueOf(peerInfo[1]);
                                System.out.println("connecting to peerid " + id + "at host " + connectToHost + " at port " + connectToPort );
                                new Client(connectToPort, connectToHost, peerIDInt).start();
                            }

                        }
                        clientConnect = true; // finished connecting to all open servers
                    }

                    Socket clientConnection = listener.accept();
                    clientList.add(clientConnection); // will be used to communicate with all clients
                    new Handler(clientConnection, peerIDInt).start();

                }
            }
        } finally {
            listener.close();
            for (java.util.logging.Handler fh : logger.getHandlers()) {
                fh.close(); // must call fh.close or a .LCK file will remain.
            }
        }
    }

    static class Handler extends Thread {

        private Socket connection; // new connection

        private DataOutputStream server_dout;
        private DataInputStream server_din;

        // private int no; // The index number of the client

        // whether server has sent/received handshake
        private boolean handshakeDone = false;

        // whether server has sent/received bitfield
        private boolean bitfieldDone = false;

        // temp buffer for incoming messages
        private byte[] incomingMsg = new byte[32]; // 32 set for handshake message

        // set in constructor
        private int peerIDInt = -1;

        // set in handshake
        int clientPeerID = -1;

        // whether to continue running the server
        boolean serverLoop = true;

        // keep track of connected client's pieces
        HashMap<Integer, Boolean> peerPieceMap = new HashMap<Integer, Boolean>();

        // used in sending messages
        byte[] messageLength;
        byte[] messageType;

        // used in sending piece messages, maybe move this
        byte[] indexField;

        public Handler(Socket connection, int peerIDInt) {
            this.connection = connection;
            this.peerIDInt = peerIDInt;
        }

        public void run() {

            try {

                // initialize Input and Output streams
                server_dout = new DataOutputStream(connection.getOutputStream());
                server_din = new DataInputStream(connection.getInputStream());

                // creating a byte array of the file

                if (isOwner == 1) { // if this peer is the owner, they must store the file as chunks in their map
                    File file = new File(fileName);

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

                }

                boolean quit = false; // used at end of program, should delete

                while (serverLoop) {

                    if (!handshakeDone) {

                        // client sends the first handshake message, server is waiting here for the
                        // message

                        server_din.read(incomingMsg); // read message into the msg 32 byte buffer
                        clientPeerID = ByteBuffer.wrap(Arrays.copyOfRange(incomingMsg, 28, 32)).getInt();

                        logConnectionFrom(peerIDInt, clientPeerID);

                        downloadRates.put(clientPeerID, 0); // set up client in map
                        peersDone.put(clientPeerID, false); // set up client in map

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
                        System.out.println("sending my peer id : "+ peerIDInt);
                        sendMessage(handshake); // send handshake message to client

                        byteOS.reset();
                        // server_din.reset();

                    } else if (!bitfieldDone) {

                        // server is waiting for bitfield message from client

                        incomingMsg = new byte[128]; // empty buffer

                        // server received bitfield message
                        server_din.read(incomingMsg, 0, 5); // incoming bitfield from client peer header

                        int bitfieldLength = ByteBuffer.wrap(Arrays.copyOfRange(incomingMsg, 0, 4)).getInt();

                        byte[] bitfieldMessage = new byte[bitfieldLength];

                        server_din.read(bitfieldMessage);
                        int counter = 1; // used to count pieces

                        for (int i = 0; i < bitfieldMessage.length; i++) {
                            String bs = String.format("%7s", Integer.toBinaryString(bitfieldMessage[i])).replace(' ',
                                    '0'); // ensure 0 bits are counted

                            for (int j = 0; j < bs.length(); j++) {
                                if (bs.charAt(j) == '0') {
                                    peerPieceMap.put(counter, false); // local connection map
                                } else if (bs.charAt(j) == '1') {
                                    peerPieceMap.put(counter, true); // local connection map
                                }
                                if (counter == numPieces) {
                                    break; // ignore the final 0 bits in the bitstrings
                                }
                                counter++;
                            }
                        }

                        logBitfieldFrom(peerIDInt, clientPeerID);
                        neighborsPieceMap.put(clientPeerID, peerPieceMap); // global (Peer) connection map

                        // send bitfield message

                        byte bitfield[] = generateBitfield();

                        int payload = bitfield.length; // payload is done incorrectly when sending pieces //check???
                        messageLength = ByteBuffer.allocate(4).putInt(payload).array();
                        messageType = ByteBuffer.allocate(1).put(messageTypeMap.get("bitfield")).array();

                        byteOS.reset(); // make sure byteOS is empty
                        byteOS.write(messageLength);
                        byteOS.write(messageType);
                        byteOS.write(bitfield);
                        bitfieldMessage = byteOS.toByteArray();

                        sendMessage(bitfieldMessage);

                        // empty out byteOS
                        byteOS.flush();
                        byteOS.reset();

                        // from here on, server will start processing regular messages
                        bitfieldDone = true;

                    } else {

                        while (server_din.read(incomingMsg) > -1) { // waiting for input

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
                                    // sc.close();
                                    System.out.println("File Transfer Complete.");
                                    serverLoop = false;
                                }
                            }

                        }

                    }
                    handshakeDone = true; // received handshake

                }
            } catch (IOException ioException) {
                System.out.println("Disconnect with Client " + clientPeerID);
                logger.warning("Disconnection with " + clientPeerID + " due to IOException");
            } finally {
                // Close connections
                try {
                    server_din.close();
                    server_dout.close();
                    connection.close();
                } catch (IOException ioException) {
                    System.out.println("Disconnect with Client " + clientPeerID);
                    logger.warning("Disconnection with " + clientPeerID + " due to IOException");
                }
            }
        }

        // sending message function
        void sendMessage(byte[] msg) {
            try {
                server_dout.flush();
                server_dout.write(msg);
            } catch (IOException ioException) {
                ioException.printStackTrace();
                logger.warning(peerIDInt + " was unable to send a message to " + clientPeerID);
            }
        }

        // method used for sending a specific piece
        void sendPiece(int pieceNumInt) throws IOException {
            // 9 byte header and 128 byte message payload
            int payload = 9 + 128; // this needs to be changed!
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

    static class Client extends Thread {

        Socket requestSocket;
        DataInputStream client_din;
        DataOutputStream client_dout;

        // whether handshake has been completed
        boolean handshakeDone = false;

        // whether bitfield has been completed
        boolean bitfieldDone = false;

        boolean start = false;

        // will be set from config and setup
        int peerIDInt = -1;
        int portNum = -1;
        String hostname = ""; // can be changed through constructor

        // empty buffers
        byte[] messageLength;
        byte[] messageType;
        byte[] indexField = new byte[4];

        // will be changed through handshake
        // int serverId = -1;
        int serverPeerID = -1;

        // each "client" can only connect to one other server, and keeps track of that
        // map
        HashMap<Integer, Boolean> peerPieceMap = new HashMap<Integer, Boolean>();
        // peer piece list (to make it easier to request pieces)
        ArrayList<Integer> peerPieceList = new ArrayList<>();

        // whether to continue running client loop
        boolean clientLoop = true;

        public Client(int portNum, int peerIDInt) {
            this.portNum = portNum;
            this.peerIDInt = peerIDInt;

        }

        public Client(int portNum, String hostname, int peerIDInt) {
            this.peerIDInt = peerIDInt;
            this.hostname = hostname;
            this.portNum = portNum;

        }

        public void run() {
            try {

                // new connection to server
                requestSocket = new Socket(hostname, portNum);

                // just to be safe
                byteOS.reset();

                // input and output streams for client
                client_din = new DataInputStream(requestSocket.getInputStream());
                client_dout = new DataOutputStream(requestSocket.getOutputStream());
                client_dout.flush();

                byte[] buffer = new byte[pieceSize];

                while (clientLoop) {

                    if (!handshakeDone) {

                        // client sends handshake first

                        String headerStr = "P2PFILESHARINGPROJ"; // header
                        byte[] header = headerStr.getBytes(); // header to bytes
                        byte[] zerobits = new byte[10]; // 10 byte zero bits
                        Arrays.fill(zerobits, (byte) 0);
                        byte[] peerID = ByteBuffer.allocate(4).putInt(peerIDInt).array(); // peer ID in byte array
                                                                                          // format

                        // write all information to a byte array
                        byteOS.flush();
                        byteOS.reset();
                        byteOS.write(header);
                        byteOS.write(zerobits);
                        byteOS.write(peerID);

                        byte[] handshake = byteOS.toByteArray();

                        sendMessage(handshake); // client sends handshake message to server

                        // client waiting for handshake mesage from server
                        byte[] incomingHandshake = new byte[32]; // empty byte array for incoming handshake

                        client_din.read(incomingHandshake); // read in the incoming handshake

                        // getting server peerID
                        byte[] checkServerID = Arrays.copyOfRange(incomingHandshake, 28, 32);
                        serverPeerID = ByteBuffer.wrap(checkServerID).getInt();
                        System.out.println("server: " + serverPeerID);
                        logConnectionTo(peerIDInt, serverPeerID);

                        handshakeDone = true; // handshake received, do not do this part again
                        byteOS.reset();

                    } else if (!bitfieldDone) {

                        // client sends bitfield first
                        byte bitfield[] = generateBitfield();

                        int payload = bitfield.length; // payload is done incorrectly when sending pieces /// check ?
                        messageLength = ByteBuffer.allocate(4).putInt(payload).array();
                        messageType = ByteBuffer.allocate(1).put(messageTypeMap.get("bitfield")).array();

                        byteOS.reset(); // make sure byteOS is empty
                        byteOS.write(messageLength);
                        byteOS.write(messageType);
                        byteOS.write(bitfield);
                        byte[] bitfieldMessage = byteOS.toByteArray();

                        sendMessage(bitfieldMessage);

                        bitfieldDone = true;
                        byteOS.flush();
                        byteOS.reset();

                    } else { // every message that is not the handshake

                        byte[] incomingMessage = new byte[130]; // will need to change the size of this

                        // create a method called "look for piece" looking for a piece to send the first
                        // request

                        while (client_din.read(incomingMessage) > -1) {

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
                                // System.out.println("sending interested");

                                start = true;
                            }

                            // retrieve message type
                            byte[] messageType = Arrays.copyOfRange(incomingMessage, 4, 5); // getting the message type

                            if (Arrays.equals(messageType, messageTypeMap.get("choke"))) {
                                System.out.println("choke functionality");
                            } else if (Arrays.equals(messageType, messageTypeMap.get("bitfield"))) {

                                // length is in byte indeces [0 - 3]
                                logBitfieldFrom(peerIDInt, serverPeerID);
                                int bitfieldLength = ByteBuffer.wrap(Arrays.copyOfRange(incomingMessage, 0, 4)).getInt();
                                byte[] bitfieldMessage = new byte[bitfieldLength];

                                // read in the rest of the message
                                client_din.read(bitfieldMessage);
                                int counter = 1;

                                // update map with received bitfield from server
                                for (int i = 0; i < bitfieldMessage.length; i++) {
                                    String bs = String.format("%7s", Integer.toBinaryString(bitfieldMessage[i]))
                                            .replace(' ', '0'); // ensure that 0 bits are counted
                                    for (int j = 0; j < bs.length(); j++) {
                                        if (bs.charAt(j) == '0') {
                                            peerPieceMap.put(counter, false);
                                        } else if (bs.charAt(j) == '1') {
                                            if (!(pieceMap.containsKey(counter))) { // if client doesn't already have
                                                                                    // this piece
                                                peerPieceList.add(counter); // this is used to request pieces
                                            }
                                            peerPieceMap.put(counter, true);
                                        }
                                        if (counter == numPieces) {
                                            break;
                                        }
                                        counter++;
                                    }
                                }

                                neighborsPieceMap.put(serverPeerID, peerPieceMap); // update global map

                            } else if (Arrays.equals(messageType, messageTypeMap.get("unchoke"))) {
                                System.out.println("unchoke functionality");
                            } else if (Arrays.equals(messageType, messageTypeMap.get("have"))) {
                                System.out.println("have functionality");

                                // add the peice to the map, and then check if its done
                            } else if (Arrays.equals(messageType, messageTypeMap.get("piece"))) {

                                // System.out.println ("piece functionality");

                                client_din.read(incomingMessage, 5, 9); // check on this

                                byte[] fileIndex = Arrays.copyOfRange(incomingMessage, 5, 9);
                                int index = ByteBuffer.wrap(fileIndex).getInt(); // number corresponds to the number in
                                                                                 // the
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
                                piecesOwned++;
                                // System.out.println("bytes read: " + bytesRead);
                                byte[] newPiece = new byte[pieceSize];
                                newPiece = buffer.clone();
                                pieceMap.put(index, newPiece);
                                // send another request message
                                // System.out.print("request index: ");

                                // int start = sc.nextInt();
                                // int pieceNum = sc.nextInt();
                                // System.out.println("num pieces: " +numPieces);
                                if (piecesOwned < numPieces) {
                                    int pieceNum = index + 1; // request the next index
                                    // System.out.println("requesting piece " + pieceNum);
                                    messageLength = ByteBuffer.allocate(4).putInt(128).array();
                                    messageType = ByteBuffer.allocate(1).put(messageTypeMap.get("request")).array(); // should
                                                                                                                     // be
                                                                                                                     // 6
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

                                // File newDir = new File(System.getProperty("user.dir") + "/" + peerIDInt);
                                // newDir.mkdir();
                                // String pathname = newDir.getAbsolutePath() + "/" + fileName;
                                String pathname = newDir.getAbsolutePath() + "/" + "copy.txt";
                                File copiedFile = new File(pathname); // get the directory for this
                                try (FileOutputStream fos = new FileOutputStream(copiedFile)) {
                                    fos.write(finalFile);
                                }

                                System.out.println("Client: final file has been written");
                                byteOS.flush();
                                byteOS.reset();
                                // sc.close();
                                // bos.close();
                                client_din.close();
                                client_dout.flush();
                                client_dout.close();
                                clientLoop = false; // this is just here for the purpose of testing
                            }
                        }
                    }
                }
            } catch (ConnectException e) {
                logger.warning("Connection refused. No server initiated.");
                System.err.println("Connection refused. You need to initiate a server first.");
            } catch (UnknownHostException unknownHost) {
                logger.warning("Connection refused. Unknown host.");
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
                // client_dout.flush();
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

    // check that bitfields are generating correctly
    static byte[] generateBitfield() {

        String bitstring = "";

        for (int i = 1; i <= numPieces; i++) {

            if (i % 7 == 0 && i != 0) { // every 8 bits the bitstring must be written out and reset
                byte b = Byte.parseByte(bitstring, 2);
                byteOS.write(b);
                // System.out.println("Bitstring: " + bitstring);
                bitstring = "";
            }

            if (bitstring.equals("")) { // first bit in the bit string must be 0
                bitstring = "0";
            }

            if (pieceMap.containsKey(i)) { // if the map contains the key, add 1, if not add 0
                bitstring += "1";
            } else
                bitstring += "0";

            if (i == numPieces) { // at the end of the map, all remaining bits are 0
                int bsLength = bitstring.length();
                int j = 7 - bsLength;

                for (int k = 0; k < j; k++) {
                    bitstring += "0";
                }
                byte b = Byte.parseByte(bitstring, 2);
                byteOS.write(b);
            }
        }

        byte[] bitfield = byteOS.toByteArray();
        return bitfield;

    }

    static void logConnectionTo(int peerID1, int peerID2) {
        logger.info("Peer [" + peerID1 + "] made a connection to Peer [" + peerID2 + "]");
    }

    // acting as server
    static void logConnectionFrom(int peerID1, int peerID2) {
        logger.info("Peer [" + peerID1 + "] is connected from Peer [" + peerID2 + "]");
    }

    static void logBitfieldFrom(int peerID1, int peerID2) {
        logger.info("Peer [" + peerID1 + "] has received a bitfield message from [" + peerID2 + "]");
    }

    static void logchangeNeighbors(int peerID1, int[] peerList) {
        logger.info("Peer [" + peerID1 + "] has the preferred neighbors [" + Arrays.toString(peerList) + "]");
    }

    static void logchangeOpUnchokeNeighbor(int peerID1, int opUnNeighbor) {
        logger.info("Peer [" + peerID1 + "] has the optimistically unchoked neighbor [" + opUnNeighbor + "]");
    }

    static void logUnchoked(int peerID1, int peerID2) {
        logger.info("Peer [" + peerID1 + "] is unchoked by [" + peerID2 + "]");
    }

    static void logChoked(int peerID1, int peerID2) {
        logger.info("Peer [" + peerID1 + "] is choked by [" + peerID2 + "]");
    }

    static void logHave(int peerID1, int peerID2, int pieceNum) {
        logger.info("Peer [" + peerID1 + "] received the 'have' message from [" + peerID2 + "] for the piece ["
                + pieceNum + "]");
    }

    static void logInterested(int peerID1, int peerID2) {
        logger.info("Peer [" + peerID1 + "] received the 'interested' message from [" + peerID2);
    }

    static void logNotInterested(int peerID1, int peerID2) {
        logger.info("Peer [" + peerID1 + "] received the 'not interested' message from [" + peerID2);
    }

    static void logDownload(int peerID1, int peerID2, int pieceNum) {
        logger.info("Peer [" + peerID1 + "] has downloaded the piece [" + pieceNum + "] from [" + peerID2 + "]. "
                + "Now the number of pieces it has is " + piecesOwned);
    }

    static void logDone(int peerID) {
        logger.info("Peer [" + peerID + "] has downloaded the complete file.");
    }

    static void updatePeersDone(int peerID) {
        boolean done = true;
        peersDone.put(peerID, true);

        for (int peer : peersDone.keySet()) {
            if (peersDone.get(peer) == false) {
                done = false;
            }
        }

        if (done) {
            System.out.println("all peers have recieved the complete file");
            System.exit(0);
        }
    }

}