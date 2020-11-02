import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;
import java.util.Scanner;

public class Server {

	// not used in implementation
	// private static final int sPort = 8000; // The server will be listening on
	// this port number

	public static void main(String[] args) throws Exception {
		System.out.println("The server is running.");

		int portNum = Integer.valueOf(args[0]);
		int peerIDInt = Integer.valueOf(args[1]);

		// int portNum = 8000;
		// int peerIDInt = 1001;
		// ServerSocket listener = new ServerSocket(sPort);
		ServerSocket listener = new ServerSocket(portNum);
		int clientNum = 1;

		try {
			while (true) {
				// new Handler(listener.accept(), clientNum).start();
				new Handler(listener.accept(), peerIDInt).start();
				System.out.println("Client " + clientNum + " is connected!");
				// System.out.println("Client " + peerIDInt + " is connected!");
				// clientNum++;
			}
		} finally {
			listener.close();
		}

	}

	/**
	 * A handler thread class. Handlers are spawned from the listening loop and are
	 * responsible for dealing with a single client's requests.
	 */
	private static class Handler extends Thread {
		private Socket connection;
		// private ObjectInputStream in; // stream read from the socket
		// private ObjectOutputStream out; // stream write to the socket
		private DataOutputStream dout;
		private DataInputStream din;
		private int no; // The index number of the client
		private boolean recHandshake = false;
		private int peerIDInt = -1;
		private byte[] incomingMsg = new byte[32]; //change size 
		// private String fileName = "alice.txt"; // this will be changed to be taken from properties
		private String fileName = "";
		// private int pieceSize = 64; // this will be changed to be taken from properties
		private int fileSize = -1;
		private int pieceSize = -1;
		Scanner sc = new Scanner(System.in);
		HashMap<String, byte[]> messageTypeMap = createMessageHashMap();
		byte[] messageLength;
		byte[] messageType;
		byte[] indexField;
		// public Handler(Socket connection, int no) {
		// this.connection = connection;
		// this.no = no;
		// }

		public Handler(Socket connection, int peerIDInt) {
			this.connection = connection;
			this.peerIDInt = peerIDInt;
		}

		public void run() {

			try (InputStream input = new FileInputStream("config.properties")) {

				Properties prop = new Properties();
	
				// load a properties file
				prop.load(input);
	
				//get properties
				this.fileName = prop.getProperty("FileName");
				this.pieceSize = Integer.valueOf(prop.getProperty("PieceSize"));
				this.fileSize = Integer.valueOf(prop.getProperty("FileSize"));
				
	
			} catch (IOException ex) {
				ex.printStackTrace();
			}

			try {

				// initialize Input and Output streams
				dout = new DataOutputStream(connection.getOutputStream());
				din = new DataInputStream(connection.getInputStream());
				boolean serverLoop = true;

				File file = new File(fileName);
				// byte[] fileInBytes = new byte[(int) file.length()];
				// System.out.println(file.length());
				byte[] fileInBytes = new byte[fileSize];

				FileInputStream fis = new FileInputStream(file);
				BufferedInputStream bis = new BufferedInputStream(fis);

				ByteArrayOutputStream byteOS = new ByteArrayOutputStream();

				// fileInBytes contains the file alice.txt in bytes
				bis.read(fileInBytes, 0, fileInBytes.length);

				boolean quit = false;

				while (serverLoop) {

					if (!recHandshake) {
						// client sends the first handshake message
						System.out.println("server waiting for handshake");

						din.read(incomingMsg); // read message into the msg 32 byte buffer

						//client peer id is wrong
						System.out.println("Received message from client " + no + ": " + Arrays.toString(incomingMsg));
						
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

						System.out.println("server sending handshake message: " + Arrays.toString(handshake));

						sendMessage(handshake); // send handshake message to client
						
						incomingMsg = new byte[128];

					} else {


						// might need to change the size of message
						din.read(incomingMsg); // waiting for a client request

						// retrieve message type 
						byte[] incomingMessageType = Arrays.copyOfRange(incomingMsg, 4, 5);
						if (Arrays.equals(incomingMessageType, messageTypeMap.get("interested"))){
							System.out.println ("interested functionality");
						}
						else if (Arrays.equals(incomingMessageType, messageTypeMap.get("not_interested"))){
							System.out.println ("not_interested functionality");
						}
						else if (Arrays.equals(incomingMessageType, messageTypeMap.get("have"))){
							System.out.println ("have functionality");
						}
						else if (Arrays.equals(incomingMessageType, messageTypeMap.get("request"))){
							System.out.println("request message received ");
							byte[] indexToSend = Arrays.copyOfRange(incomingMsg, 5, 9);
							int index = ByteBuffer.wrap(indexToSend).getInt();
							System.out.println("index to send from: " + index);

							//73 for 9 byte header and 64 byte message payload
							messageLength = ByteBuffer.allocate(4).putInt(73).array();
							messageType = ByteBuffer.allocate(1).put(messageTypeMap.get("piece")).array(); //putting int 6 for request 
							indexField = ByteBuffer.allocate(4).putInt(index).array(); //index of starting point

							byteOS.reset();
							byteOS.write(messageLength);
							byteOS.write(messageType); //should equal binary 7 for "piece" 
							byteOS.write(indexField);
							byteOS.write(fileInBytes, index, pieceSize); //writing the contents of the file
							// dout.write(fileInBytes, index, pieceSize); // change this to use the send message method

							byte [] sendMessage = byteOS.toByteArray();
							sendMessage(sendMessage); //sending the piece message

							System.out.println("quit?");
							quit = sc.nextBoolean();

							if (quit) {
								dout.flush();
								bis.close();
								sc.close();
								System.out.println("File Transfer Complete.");
								serverLoop = false;
							}
						}

						// this is used for testing. instead we should have a map with all the types of
						// messages
						// byte six = 0b110;
						// byte[] requestMsg = ByteBuffer.allocate(1).put(six).array();
						// System.out.println("request message: " + Arrays.toString(requestMsg));


						// System.out.println("message type: " + Arrays.toString(messageType));

						// if (Arrays.equals(requestMsg, messageType)) {
						// 	System.out.println("request message received ");
						// 	byte[] indexToSend = Arrays.copyOfRange(msg, 8, 12);
						// 	int index = ByteBuffer.wrap(indexToSend).getInt();

						// 	dout.write(fileInBytes, index, pieceSize); // change this to use the send message method

						// 	System.out.println("quit?");
						// 	quit = sc.nextBoolean();

						// 	if (quit) {
						// 		dout.flush();
						// 		bis.close();
						// 		sc.close();
						// 		System.out.println("File Transfer Complete.");
						// 		serverLoop = false;
						// 	}
						// } else {
						// 	System.out.println("wrong message type received.");
						// 	serverLoop = false; // this is just here for the purpose of testing
						// }
					}
					recHandshake = true; //received handshake
					
				}
			} catch (IOException ioException) {
				System.out.println("Disconnect with Client " + no);
			} finally {
				// Close connections
				try {
					din.close();
					dout.close();
					connection.close();
				} catch (IOException ioException) {
					System.out.println("Disconnect with Client " + no);
				}
			}
		}

		void sendMessage(byte[] msg) {
			try {
				dout.write(msg);

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

}

/// old testing code

// //space used for all other messages that are not handshake
// File transferFile = new File(this.fileName);

// //temp data
// byte[] byteArr = new byte [(int)transferFile.length()];

// //read bytes from file into byte array
// FileInputStream fin = new FileInputStream(transferFile);
// BufferedInputStream bin = new BufferedInputStream(fin);

// bin.read(byteArr, 0, byteArr.length);

// //output stream provides a channel to communicate with the client side
// OutputStream os = connection.getOutputStream();
// System.out.println("sending files...");

// os.write(byteArr,0,pieceSize);
// bin.close();

// while (!quit) {
// System.out.println("starting byte: ");
// int start = sc.nextInt();
// System.out.println("ending byte");
// int end = sc.nextInt();

// //writes to the output stream a chunk from starting byte to ending byte
// dout.write(fileInBytes,start,end);

// //this needs to change from user entering quit to the process itself sending
// messages

// }