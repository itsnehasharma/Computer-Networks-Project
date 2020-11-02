import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
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

	// main method
	public static void main(String args[]) {

		// this will all be commented out, main will not be run when peer process runs

		int portNum = Integer.valueOf(args[0]);
		System.out.println(portNum);
		int peerIDInt = Integer.valueOf(args[1]);
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
			// create a socket to connect to the server
			// different variations of connecting to a host
			// requestSocket = new Socket("storm.cise.ufl.edu", 8000);
			// requestSocket = new Socket("localhost", 8000);
			// requestSocket = new Socket("localhost", portNum);

			requestSocket = new Socket(hostname, portNum); // used in peerprocess.java implementation
			System.out.println("Connected to localhost in port " + portNum);

			// out = new ObjectOutputStream(requestSocket.getOutputStream());
			// out.flush();
			// in = new ObjectInputStream(requestSocket.getInputStream());
			// is = requestSocket.getInputStream();
			byteOS.flush();
			din = new DataInputStream(requestSocket.getInputStream());
			dout = new DataOutputStream(requestSocket.getOutputStream());
			boolean clientLoop = true;

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
					byteOS.flush();

				} else { //every message that is not the handshake 

					int fileSize = 148481; // change this to pull from config properties
					byte[] finalFileInBytes = new byte[fileSize];

					FileOutputStream fos = new FileOutputStream("copy.txt");
					BufferedOutputStream bos = new BufferedOutputStream(fos);

					boolean quit = false;
					Scanner sc = new Scanner(System.in);

					//change this input loop to pass this through in a message 
					while (!quit) {
						System.out.println("starting byte: ");
						int start = sc.nextInt();
						System.out.println("ending byte");
						int end = sc.nextInt();

						// this writes INTO the array final file in bytes at byte[start] until byte[end]
						din.read(finalFileInBytes, start, end);
						System.out.println("quit?");
						quit = sc.nextBoolean();
					}

					bos.write(finalFileInBytes, 0, 160);
					sc.close();
					bos.close();
					clientLoop = false; //this is just here for the purpose of testing 
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