import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Scanner;

public class Server {

	// not used in implementation
	// private static final int sPort = 8000; // The server will be listening on this port number

	public static void main(String[] args) throws Exception {
		System.out.println("The server is running.");

		int portNum = Integer.valueOf(args[0]);
		// int peerIDInt = Integer.valueOf(args[1]);

		// ServerSocket listener = new ServerSocket(sPort);
		ServerSocket listener = new ServerSocket(portNum);
		int clientNum = 1;

		try {
			while (true) {
				new Handler(listener.accept(), clientNum).start();
				// new Handler(listener.accept(), peerIDInt).start();
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
		private ObjectInputStream in; // stream read from the socket
		private ObjectOutputStream out; // stream write to the socket
		private DataOutputStream dout;
		private DataInputStream din;
		private int no; // The index number of the client
		private boolean recHandshake = false;
		private int peerIDInt = -1;
		private byte[] msg = new byte[32];
		private String fileName = "testFile.txt"; //this will be changed to be taken from properties
		private int pieceSize = 32; //this will be changed to be taken from properties 

		// public Handler(Socket connection, int no) {
		// this.connection = connection;
		// this.no = no;
		// }

		public Handler(Socket connection, int peerIDInt) {
			this.connection = connection;
			this.peerIDInt = peerIDInt;
		}

		public void run() {

			try {

				// initialize Input and Output streams
				dout = new DataOutputStream(connection.getOutputStream());
				din = new DataInputStream(connection.getInputStream());
				boolean serverLoop = true;
				while (serverLoop) {

					if (!recHandshake) {
						// client sends the first handshake message
						System.out.println("server waiting for handshake");

						din.read(msg); // read message into the msg 32 byte buffer

						System.out.println("Received message from client " + peerIDInt + ": " + Arrays.toString(msg));

						ByteArrayOutputStream byteOS = new ByteArrayOutputStream();

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

						sendMessage(handshake); //send handshake message to client 

						msg = new byte[12]; //setting a new empty 

					} else {

						File file = new File("alice.txt");
						byte [] fileInBytes = new byte[(int)file.length()];

						FileInputStream fis = new FileInputStream(file);
						BufferedInputStream bis = new BufferedInputStream(fis);

						//fileInBytes contains the file alice.txt in bytes
						bis.read(fileInBytes, 0, fileInBytes.length);

						boolean quit = false;
						Scanner sc = new Scanner(System.in);

						while (!quit) {
							System.out.println("starting byte: ");
							int start = sc.nextInt();
							System.out.println("ending byte");
							int end = sc.nextInt();
			
							//writes to the output stream a chunk from starting byte to ending byte 
							dout.write(fileInBytes,start,end);
			
							//this needs to change from user entering quit to the process itself sending messages 
							System.out.println("quit?");
							quit = sc.nextBoolean();
						}

						dout.flush();
						bis.close();
						sc.close();
						System.out.println("File Transfer Complete.");

						serverLoop = false; //this is just here for the purpose of testing

					}
					recHandshake = true;
				}
			} catch (IOException ioException) {
				System.out.println("Disconnect with Client " + no);
			}
			finally {
				// Close connections
				try {
					in.close();
					out.close();
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