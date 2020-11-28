import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;
import java.util.Scanner;

public class Server {

	public static void main(String[] args) throws Exception {
		System.out.println("The server is running.");

		int portNum = Integer.valueOf(args[0]);
		int peerIDInt = Integer.valueOf(args[1]);

		// int portNum = 8000;
		// int peerIDInt = 1001;

		ServerSocket listener = new ServerSocket(portNum);
		int clientNum = 1;

		try {
			while (true) {
				// new Handler(listener.accept(), clientNum).start();
				new Handler(listener.accept(), peerIDInt).start();
				System.out.println("Client " + clientNum + " is connected!");
				clientNum++;
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
		private DataOutputStream dout;
		private DataInputStream din;
		private int no; // The index number of the client
		private boolean recHandshake = false;
		private boolean sentBitfield = false;

		private byte[] incomingMsg = new byte[32]; // 32 set for handshake message
		// private String fileName = "alice.txt"; // for testing
		private String fileName = "";
		// private int pieceSize = 64; // for testing

		private int peerIDInt = -1;
		private int fileSize = -1;
		private int pieceSize = -1;

		Scanner sc = new Scanner(System.in);
		HashMap<String, byte[]> messageTypeMap = createMessageHashMap();
		HashMap<Integer, byte[]> pieceMap = new HashMap<>();

		byte[] messageLength;
		byte[] messageType;
		byte[] indexField;

		ByteArrayOutputStream byteOS = new ByteArrayOutputStream();

		public Handler(Socket connection, int peerIDInt) {
			this.connection = connection;
			this.peerIDInt = peerIDInt;
		}

		public void run() {

			// loading properties from config file
			try (InputStream input = new FileInputStream("config.properties")) {

				Properties prop = new Properties();

				// load a properties file
				prop.load(input);

				// get properties
				this.fileName = prop.getProperty("FileName");
				this.pieceSize = Integer.valueOf(prop.getProperty("PieceSize"));
				this.fileSize = Integer.valueOf(prop.getProperty("FileSize"));

			} catch (IOException ex) {
				ex.printStackTrace();
			}

			// over here put file into map

			try {

				// initialize Input and Output streams
				dout = new DataOutputStream(connection.getOutputStream());
				din = new DataInputStream(connection.getInputStream());
				boolean serverLoop = true;

				// creating a byte array of the file
				File file = new File(fileName);
				byte[] fileInBytes = new byte[fileSize];

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

						din.read(incomingMsg); // read message into the msg 32 byte buffer

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
						sentBitfield = true;
					} else {
						// might need to change the size of message				

						while (din.read(incomingMsg) > -1) {
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
								System.out.println("request message received ");
								byte[] pieceNumToSend = Arrays.copyOfRange(incomingMsg, 5, 9);
								int pieceNumInt = ByteBuffer.wrap(pieceNumToSend).getInt();
								System.out.println("server recieved index from client:" + pieceNumInt);
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
									dout.flush();
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

		void sendPiece(int pieceNumInt) throws IOException {
			// 9 byte header and 128 byte message payload
			int payload = 9 + 128;
			messageLength = ByteBuffer.allocate(4).putInt(payload).array();
			messageType = ByteBuffer.allocate(1).put(messageTypeMap.get("piece")).array();
			indexField = ByteBuffer.allocate(4).putInt(pieceNumInt).array(); // index of starting point

			byteOS.reset(); // make sure byteOS is empty
			byteOS.write(messageLength);
			byteOS.write(messageType); // should equal binary 7 for "piece"
			byteOS.write(indexField);
			// byteOS.write(fileInBytes, index, pieceSize); // writing the contents of the
			// file
			System.out.println("sending piece " + pieceNumInt);
			byte[] pieceBuffer = pieceMap.get(pieceNumInt);
			byteOS.write(pieceBuffer);

			byte[] sendMessage = byteOS.toByteArray();
			sendMessage(sendMessage); // sending the piece message

		}

		// map used for message typing
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

/// IGNORE: old testing code

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

// this is used for testing. instead we should have a map with all the types of
// messages
// byte six = 0b110;
// byte[] requestMsg = ByteBuffer.allocate(1).put(six).array();
// System.out.println("request message: " + Arrays.toString(requestMsg));

// System.out.println("message type: " + Arrays.toString(messageType));

// if (Arrays.equals(requestMsg, messageType)) {
// System.out.println("request message received ");
// byte[] indexToSend = Arrays.copyOfRange(msg, 8, 12);
// int index = ByteBuffer.wrap(indexToSend).getInt();

// dout.write(fileInBytes, index, pieceSize); // change this to use the send
// message method

// System.out.println("quit?");
// quit = sc.nextBoolean();

// if (quit) {
// dout.flush();
// bis.close();
// sc.close();
// System.out.println("File Transfer Complete.");
// serverLoop = false;
// }
// } else {
// System.out.println("wrong message type received.");
// serverLoop = false; // this is just here for the purpose of testing
// }