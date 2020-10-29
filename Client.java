import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.io.*;

public class Client {
	Socket requestSocket; // socket connect to the server
	ObjectOutputStream out; // stream write to the socket
	ObjectInputStream in; // stream read from the socket
	DataInputStream din;
	DataOutputStream dout;
	String message; // message send to the server
	String MESSAGE; // capitalized message read from the server
	boolean sentHandshake = false;
	boolean recHandshake = false;
	int peerIDInt = -1;
	int portNum = -1;
	private InputStream is;
	ByteArrayOutputStream byteOS = new ByteArrayOutputStream();


	// main method
	public static void main(String args[]) {

		int portNum = Integer.valueOf(args[0]);
		int peerIDInt = Integer.valueOf(args[1]);
		Client client = new Client(portNum, peerIDInt);
		client.run();
	}


	public Client(int peerIDInt, int portNum) {

		this.peerIDInt = peerIDInt;
		this.portNum = portNum;

	}

	void run() {
		try {
			// create a socket to connect to the server
			// requestSocket = new Socket("storm.cise.ufl.edu", 8000);
			// requestSocket = new Socket("localhost", 8000);
			requestSocket = new Socket("localhost", 8000);
			System.out.println("Connected to localhost in port 8000");

			// out = new ObjectOutputStream(requestSocket.getOutputStream());
			// out.flush();
			// in = new ObjectInputStream(requestSocket.getInputStream());
			// is = requestSocket.getInputStream();
			byteOS.flush();
			din = new DataInputStream(requestSocket.getInputStream());
			dout = new DataOutputStream(requestSocket.getOutputStream());

			// handshake();

			// initialize inputStream and outputStream

			// get Input from standard input
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

			while (true) {

				if (!recHandshake) {
					// ByteArrayOutputStream byteOS = new ByteArrayOutputStream();
					String headerStr = "P2PFILESHARINGPROJ";
					byte[] header = headerStr.getBytes();
					byte [] zerobits = new byte[10];
					Arrays.fill(zerobits, (byte) 0);
					byte[] peerID = ByteBuffer.allocate(4).putInt(peerIDInt).array();

					byteOS.write(header);
       				byteOS.write(zerobits);
        			byteOS.write(peerID);

					byte[] handshake = byteOS.toByteArray();
					
					System.out.println("client sending handshake message: " + Arrays.toString(handshake));

					sendMessage(handshake);


					System.out.println("client waiting for handshake");

					byte[] incomingHandshake = new byte[32];
					// is.read(incomingHandshake);
					din.read(incomingHandshake);
					System.out.println("Received message from server: " + Arrays.toString(incomingHandshake));
					// System.out.println("Receive message: " + message + " from client " + no);
					
					recHandshake = true;
					byteOS.flush();
				} else {

					//if else statements for message types 
					System.out.print("Hello, please input a sentence: ");
					// read a sentence from the standard input
					message = bufferedReader.readLine();
					// Send the sentence to the server
					sendMessage(message);
					// Receive the upperCase sentence from the server
					MESSAGE = (String) in.readObject();
					// show the message to the user
					System.out.println("Receive message: " + MESSAGE);

				}

			}
		} catch (ConnectException e) {
			System.err.println("Connection refused. You need to initiate a server first.");
		} catch (ClassNotFoundException e) {
			System.err.println("Class not found");
		} catch (UnknownHostException unknownHost) {
			System.err.println("You are trying to connect to an unknown host!");
		} catch (IOException ioException) {
			ioException.printStackTrace();
		} finally {
			// Close connections
			try {
				in.close();
				out.close();
				requestSocket.close();
			} catch (IOException ioException) {
				ioException.printStackTrace();
			}
		}
	}

	// send a message to the output stream
	void sendMessage(String msg) {

		try {
			// stream write the message
			out.writeObject(msg);
			out.flush();
		} catch (IOException ioException) {
			System.out.print("caught exception");
			ioException.printStackTrace();
		}
	}

	void sendMessage(byte[] msg) {
		try {
			// stream write the message
			// out.writeObject(msg);
			// out.flush();
			dout.write(msg);
			dout.flush();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
	}

	// void handshake() throws IOException {

	// ByteArrayOutputStream byteOS = new ByteArrayOutputStream();
	// // byte zeroBits[] = new byte[8];
	// byteOS.write("P2PFILESHARINGPROJ".getBytes());
	// // byteOS.write(zeroBits);
	// byteOS.write(peerID);

	// handshake = byteOS.toByteArray();
	// sendMessage(handshake);

	// System.out.println("client waiting for handshake");
	// byte[] h = new byte[32];
	// is.read(h, 0, 32);
	// // String handshake = in.readObject().toString();
	// System.out.println("handshake: " + h);

	// }

}
