import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;
import java.util.Scanner;
import java.io.*;

public class Client {
	Socket requestSocket; // socket connect to the server
	DataInputStream din;
	DataOutputStream dout;
	String message; // message send to the server
	String MESSAGE; // capitalized message read from the server
	boolean sentHandshake = false;
	boolean recHandshake = false;
	int peerIDInt = -1;
	int portNum = -1;
	int serverId = -1; // will be used to determine if the server is correct
	// private InputStream is;
	ByteArrayOutputStream byteOS = new ByteArrayOutputStream();
	String hostname = "localhost";
	byte[] messageLength;
	byte[] messageType;
	byte[] indexField = new byte[4];
	Scanner sc = new Scanner(System.in);
	// int fileSize = 148481; // change this to pull from config properties
	int fileSize = -1;
	byte[] finalFileInBytes;
	// int pieceSize = 64;
	int pieceSize = -1;
	HashMap<String, byte[]> messageTypeMap = createMessageHashMap();
	private String fileName = "";

	// main method
	public static void main(String args[]) {

		// this will all be commented out, main will not be run when peer process runs

		// int portNum = Integer.valueOf(args[0]);
		int portNum = 8000;
		System.out.println(portNum);
		// int peerIDInt = Integer.valueOf(args[1]);
		int peerIDInt = 1002;
		Client client = new Client(portNum, peerIDInt);
		client.run();
	}

	public Client(int portNum, int peerIDInt) { // used when running java Client
		this.portNum = portNum;
		this.peerIDInt = peerIDInt;

	}

	public Client(int peerIDInt, String hostname, int portNum) { // used when running java PeerProcess
		this.peerIDInt = peerIDInt;
		this.hostname = hostname;
		this.portNum = portNum;
	}

	//write a method to get information from config file 

	void run() {
		try {
			// create a socket to connect to the server
			// different variations of connecting to a host
			// requestSocket = new Socket("storm.cise.ufl.edu", 8000);
			// requestSocket = new Socket("localhost", 8000);
			// requestSocket = new Socket("localhost", portNum);
			
			System.out.println("created a new client");
			System.out.println("I am peer " + peerIDInt + " trying to connect to " + hostname + " at port " + portNum);
			requestSocket = new Socket(hostname, portNum); // used in peerprocess.java implementation
			System.out.println("Connected to localhost in port " + portNum);

			try (InputStream input = new FileInputStream("config.properties")) {

				Properties prop = new Properties();
	
				// load a properties file
				prop.load(input);
	
				//get properties
				this.fileName = prop.getProperty("FileName");
				this.pieceSize = Integer.valueOf(prop.getProperty("PieceSize"));
				this.fileSize = Integer.valueOf(prop.getProperty("FileSize"));
				finalFileInBytes = new byte[fileSize];
				
	
			} catch (IOException ex) {
				ex.printStackTrace();
			}

			// out = new ObjectOutputStream(requestSocket.getOutputStream());
			// out.flush();
			// in = new ObjectInputStream(requestSocket.getInputStream());
			// is = requestSocket.getInputStream();
			byteOS.reset();
			din = new DataInputStream(requestSocket.getInputStream());
			dout = new DataOutputStream(requestSocket.getOutputStream());
			dout.flush();
			boolean clientLoop = true;

			// FileOutputStream fos = new FileOutputStream("copy.txt");
			// BufferedOutputStream bos = new BufferedOutputStream(fos);

			//setting up folder for copy to be made in 
			File newDir = new File(System.getProperty("user.dir") + "/" + peerIDInt);
			boolean createDir = newDir.mkdir();

			String pathname = newDir.getAbsolutePath() + "/" + fileName;

			FileOutputStream fos = new FileOutputStream(pathname);
			BufferedOutputStream bos = new BufferedOutputStream(fos);

			while (clientLoop) {

				if (!recHandshake) { // first time connection, need to send handshake

					// creating a byte array message to send as the handshake

					String headerStr = "P2PFILESHARINGPROJ"; // header
					byte[] header = headerStr.getBytes(); // header to bytes
					byte[] zerobits = new byte[10]; // 10 byte zero bits
					Arrays.fill(zerobits, (byte) 0);
					byte[] peerID = ByteBuffer.allocate(4).putInt(peerIDInt).array(); // peer ID in byte array format
					

					byte [] serverPeerID = ByteBuffer.allocate(4).putInt(1001).array(); //used for testing only

					// write all information to a byte array
					byteOS.write(header);
					byteOS.write(zerobits);
					byteOS.write(peerID);

					byte[] handshake = byteOS.toByteArray();

					System.out.println("client sending handshake message: " + Arrays.toString(handshake));

					sendMessage(handshake); // client sends handshake message to server

					System.out.println("client waiting for handshake");

					byte[] incomingHandshake = new byte[32]; // empty byte array for incoming handshake

					din.read(incomingHandshake); // read in the incoming handshake

					
					System.out.println("Received message from server: " + Arrays.toString(incomingHandshake));

					byte[] checkServerID = Arrays.copyOfRange(incomingHandshake, 28, 32);
					System.out.println("Receieved peer id:" + Arrays.toString(checkServerID));
					System.out.println("Expected peer id:" + Arrays.toString(serverPeerID));
					if (Arrays.equals(serverPeerID, checkServerID)) {
						System.out.println("peer ID Matches");
						
					} else {
						System.out.println("incorrect peerID received");
						//do not set rec handshake to true, we will have to do the handshake again
						//this probably doesnt work right yet 
					}			
					recHandshake = true; // handshake received, do not do this part again
					byteOS.reset();

				} else { //every message that is not the handshake 

					//client sends the first message
					// this is the request functionality, needs to be placed in some sort of organization

					System.out.println("starting byte: ");
					int start = sc.nextInt();
					messageLength = ByteBuffer.allocate(4).putInt(128).array();
					messageType = ByteBuffer.allocate(1).put(messageTypeMap.get("request")).array(); //should be 6
					indexField = ByteBuffer.allocate(4).putInt(start).array(); //index of starting point

					//writing request message to the byte output stream 
					byteOS.reset();
					byteOS.write(messageLength);
					byteOS.write(messageType);
					byteOS.write(indexField);

					byte [] msg = byteOS.toByteArray();
					sendMessage(msg); //requesting the piece 

					//waiting for a reply from the server
					byte[] incomingMessage = new byte[128]; //will need to change the size of this 
					System.out.println("waiting for server reply");
					// din.read(incomingMessage);
					din.read(incomingMessage, 0, 9); //read from 0 to 8 for the header, 9 exclusive 
					System.out.println("read first 9 bytes");
					// [0 - 3] message length
					// [4] message type
					// [5 - 8] index field 

					byte[] messageType = Arrays.copyOfRange(incomingMessage, 4, 5); //getting the message type

					if (Arrays.equals(messageType, messageTypeMap.get("choke"))){
						System.out.println ("choke functionality");
					}
					else if (Arrays.equals(messageType, messageTypeMap.get("unchoke"))){
						System.out.println ("unchoke functionality");
					}
					else if (Arrays.equals(messageType, messageTypeMap.get("have"))){
						System.out.println ("have functionality");
					}
					else if (Arrays.equals(messageType, messageTypeMap.get("piece"))){
						System.out.println ("piece functionality");

						byte[] fileIndex = Arrays.copyOfRange(incomingMessage, 5, 9);
						int index = ByteBuffer.wrap(fileIndex).getInt();
						din.read(finalFileInBytes, index, pieceSize); //should read into file byte array from specified index
					}

					//request message has been sent, now wait for response of piece 
					// din.read(finalFileInBytes, start, pieceSize); 

					System.out.println("quit?");
					boolean quit = sc.nextBoolean();

					if (quit) {
						//when done, we need to write to the client's folder 
						bos.write(finalFileInBytes, 0, 128); //writes final into copy.txt. change 160 based on chunks sent. 
						sc.close();
						bos.close();
						din.close();
						dout.flush();
						dout.close();
						clientLoop = false; //this is just here for the purpose of testing 
					}

					// boolean quit = false;
					

					// //change this input loop to pass this through in a message 
					// while (!quit) {
					// 	System.out.println("starting byte: ");
					// 	int start = sc.nextInt();
					// 	System.out.println("ending byte");
					// 	int end = sc.nextInt();

					// 	// this writes INTO the array final file in bytes at byte[start] until byte[end]
					// 	din.read(finalFileInBytes, start, end);
					// 	System.out.println("quit?");
					// 	quit = sc.nextBoolean();
					// }
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

	// send a message to the output stream
	void sendMessage(byte[] msg) {
		try {
			dout.write(msg);
			dout.flush();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
	}

	HashMap<String, byte[]> createMessageHashMap() {
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
}

/// old test code

					// this space will be for all messages that are not the handshake

					// //create a new director with the name of the peer
					// File newDir = new File(System.getProperty("user.dir") + "/" +
					// this.peerIDInt);
					// boolean createDir = newDir.mkdir();

					// //full path of file to copy to
					// String pathname = newDir.getAbsolutePath() + "/copy.txt";

					// System.out.println(pathname); //checking

					// FileOutputStream fos = new FileOutputStream(pathname);
					// BufferedOutputStream bos = new BufferedOutputStream(fos);

					// byte[] byteArr = new byte[10000232]; //change this to properties file size

					// //total number of bytes read in thh input stream
					// int bytesRead = din.read(byteArr, 0, byteArr.length);
					// int currentTot = bytesRead;

					// //continue to read from the input stream until there is not data left on the
					// stream
					// do {
					// bytesRead =
					// din.read(byteArr, currentTot, (byteArr.length-currentTot));
					// if (bytesRead >= 0) currentTot += bytesRead;

					// } while (bytesRead > -1);

					// bos.write(byteArr,0,currentTot);
					// bos.flush();
					// bos.close();