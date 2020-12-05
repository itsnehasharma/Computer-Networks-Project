import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.io.*;

public class Client {
	Socket requestSocket; // socket connect to the server
	DataInputStream din;
	DataOutputStream dout;

	boolean recHandshake = false;
	boolean start = false;

	//will be set from config and setup 
	int peerIDInt = -1;
	int portNum = -1;
	int serverId = -1; // will be used to determine if the server is correct

	ByteArrayOutputStream byteOS = new ByteArrayOutputStream();
	String hostname = "localhost"; //can be changed through constructor

	byte[] messageLength;
	byte[] messageType;
	byte[] indexField = new byte[4];

	Scanner sc = new Scanner(System.in);
	// int fileSize = 148481; // for testing
	int fileSize = -1;

	byte[] finalFileInBytes;

	// int pieceSize = 64; // for testing 
	int pieceSize = -1;
	int numPieces = 0;
	int piecesReceived = 0;

	HashMap<String, byte[]> messageTypeMap = createMessageHashMap();
	HashMap<Integer, ArrayList<Integer>> neighborBitfieldMap = new HashMap<Integer, ArrayList<Integer>>();
	private String fileName = ""; //get from config

	public static void main(String args[]) {

		// main will not be run when peer process runs

		int portNum = Integer.valueOf(args[0]);
		// int portNum = 8000; // for testing

		int peerIDInt = Integer.valueOf(args[1]);
		// int peerIDInt = 1002; //for testing

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

	void run() {
		try {	
			// System.out.println("created a new client");
			// System.out.println("I am peer " + peerIDInt + " trying to connect to " + hostname + " at port " + portNum);

			requestSocket = new Socket(hostname, portNum);
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
				numPieces = (int) Math.ceil(fileSize/pieceSize);
				
	
			} catch (IOException ex) {
				ex.printStackTrace();
			}

			byteOS.reset();
			din = new DataInputStream(requestSocket.getInputStream());
			dout = new DataOutputStream(requestSocket.getOutputStream());
			dout.flush();
			boolean clientLoop = true;

			//setting up folder & file for copy to be made in 
			// File newDir = new File(System.getProperty("user.dir") + "/" + peerIDInt);
			// newDir.mkdir();

			// String pathname = newDir.getAbsolutePath() + "/" + fileName;

			// FileOutputStream fos = new FileOutputStream(pathname);
			// BufferedOutputStream bos = new BufferedOutputStream(fos);
			int largestByte = 0;
			HashMap<Integer, byte[]> pieceMap = new HashMap<>(); //will hold all of the pieces
			byte[] buffer = new byte[pieceSize];


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

					// System.out.println("client sending handshake message: " + Arrays.toString(handshake));

					sendMessage(handshake); // client sends handshake message to server

					// System.out.println("client waiting for handshake");

					byte[] incomingHandshake = new byte[32]; // empty byte array for incoming handshake

					din.read(incomingHandshake); // read in the incoming handshake

					// System.out.println("Received message from server: " + Arrays.toString(incomingHandshake));

					//checking to make sure the correct peerID has been connected
					byte[] checkServerID = Arrays.copyOfRange(incomingHandshake, 28, 32);
					int checkServerIDInt = ByteBuffer.wrap(checkServerID).getInt();

					// System.out.println("Receieved peer id:" + Arrays.toString(checkServerID));
					// System.out.println("Expected peer id:" + Arrays.toString(serverPeerID));
					if (Arrays.equals(serverPeerID, checkServerID)) {
						System.out.println("peer ID confirmed.");
						ArrayList<Integer> tempList = new ArrayList<Integer>();
						neighborBitfieldMap.put(checkServerIDInt, tempList);
						
					} else {
						System.out.println("incorrect peerID received");
						// if this is the case the handshake needs to be redone
						// have not figured this part out yet 
					}			
					recHandshake = true; // handshake received, do not do this part again
					byteOS.reset();

				} else { //every message that is not the handshake 
					boolean quit = false;

					//client sends the first message, for now, an interested message
					if (!start){
						messageLength = ByteBuffer.allocate(4).putInt(128).array();
						messageType = ByteBuffer.allocate(1).put(messageTypeMap.get("interested")).array(); 


						byteOS.reset();
						byteOS.write(messageLength);
						byteOS.write(messageType);

						byte [] msg = byteOS.toByteArray();
						sendMessage(msg); //requesting the piece
						System.out.println("sending interested");

						start = true;
					}
					// this is the request functionality only for now.

					// inst	ead of taking user input, the p2p process will use the bittorrent protocol to request the specific bytes
					// all of this needs to be moved into after the bitfield 
					
					//waiting for a reply from the server
					byte[] incomingMessage = new byte[130]; //will need to change the size of this 
					// System.out.println("waiting for server reply");

					//read from 0 to 8 for the header, 9 exclusive 
					// [0 - 3] message length
					// [4] message type
					// [5 - 8] index field 
					din.read(incomingMessage, 0, 9); 

					byte[] messageType = Arrays.copyOfRange(incomingMessage, 4, 5); //getting the message type

					if (Arrays.equals(messageType, messageTypeMap.get("choke"))){
						System.out.println ("choke functionality");
					}
					else if (Arrays.equals(messageType, messageTypeMap.get("unchoke"))){
						System.out.println ("unchoke functionality");
					}
					else if (Arrays.equals(messageType, messageTypeMap.get("have"))){
						// in response to this, send back interested or not interested
						System.out.println ("have functionality");
					}
					else if (Arrays.equals(messageType, messageTypeMap.get("piece"))){
						
						// System.out.println ("piece functionality");

						byte[] fileIndex = Arrays.copyOfRange(incomingMessage, 5, 9);
						int index = ByteBuffer.wrap(fileIndex).getInt(); //number corresponds to the number in the map 
						System.out.println("recieved piece " + index);
						// System.out.println("client recieved index from server: " + index);
						// din.read(finalFileInBytes, index, pieceSize); //should read into file byte array from specified index
						// if (index+pieceSize > largestByte){
						// 	largestByte = index+pieceSize; //only up to the largest byte will be exported to the file.
						// }
						int bytesRead = din.read(buffer, 0, pieceSize);
						piecesReceived++;
						// System.out.println("bytes read: " + bytesRead);
						byte[] newPiece = new byte[pieceSize];
						newPiece = buffer.clone();
						pieceMap.put(index, newPiece);
						//send another request message
						// System.out.print("request index: ");

						// int start = sc.nextInt();
						// int pieceNum = sc.nextInt();
						if (piecesReceived < numPieces) {
							int pieceNum = index+1; //request the next index
							System.out.println("requesting piece " + pieceNum);
							messageLength = ByteBuffer.allocate(4).putInt(128).array();
							messageType = ByteBuffer.allocate(1).put(messageTypeMap.get("request")).array(); //should be 6
							// indexField = ByteBuffer.allocate(4).putInt(start).array(); //index of starting point
							indexField = ByteBuffer.allocate(4).putInt(pieceNum).array(); //index of piece num

							//writing request message to the byte output stream 
							byteOS.reset();
							byteOS.write(messageLength);
							byteOS.write(messageType);
							byteOS.write(indexField);

							byte [] msg = byteOS.toByteArray();
							sendMessage(msg); //requesting the piece 
						} else {
							quit = true;
						}
						
					}

					//request message has been sent, now wait for response of piece 

					// System.out.println("quit?");
					// boolean quit = sc.nextBoolean();

					if (quit) {
						//when done, we need to write to the client's folder 
						// bos.write(finalFileInBytes, 0, largestByte); //writes final into peerid/alice.txt
						
						// for (Map.Entry<Integer,byte[]> entry: pieceMap.entrySet()) {
						// 	byteOS.write(entry.getValue());
						// }
						System.out.println("Client is gathering file pieces to combine.");
						byteOS.reset();
						for (int i = 1; i <= pieceMap.size(); i++){
							// System.out.println("piece number " + i);
            				byteOS.write(pieceMap.get(i)); //write all pieces from map into byteOS
        				}
						byte[] finalFile = byteOS.toByteArray();

						File newDir = new File(System.getProperty("user.dir") + "/" + peerIDInt);
						newDir.mkdir();
						// String pathname = newDir.getAbsolutePath() + "/" + fileName;
						String pathname = newDir.getAbsolutePath() + "/" + "copy.txt";
						File copiedFile = new File (pathname); //get the directory for this 
						try (FileOutputStream fos = new FileOutputStream(copiedFile)){
							fos.write(finalFile);
						}

						System.out.println("Client: final file has been written");
						byteOS.flush();
						byteOS.reset();
						sc.close();
						// bos.close();
						din.close();
						dout.flush();
						dout.close();
						clientLoop = false; //this is just here for the purpose of testing 
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

	// send a message to the output stream
	void sendMessage(byte[] msg) {
		try {
			dout.write(msg);
			dout.flush();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
	}

	//map for message types 
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

///IGNORE: old test code

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