import java.net.*;
import java.nio.charset.StandardCharsets;
import java.io.*;

public class Client {
	Socket requestSocket; // socket connect to the server
	ObjectOutputStream out; // stream write to the socket
	ObjectInputStream in; // stream read from the socket
	String message; // message send to the server
	String MESSAGE; // capitalized message read from the server
	boolean sentHandshake = false;
	boolean recHandshake = false;
	int peerID = 1001;
	private InputStream is;
	byte[] msgLength = new byte[4];
	byte[] msgType = new byte[1];
	ByteArrayOutputStream byteOS = new ByteArrayOutputStream();

	public Client() {
	}

	void run() {
		try {
			// create a socket to connect to the server
			requestSocket = new Socket("localhost", 8000);
			System.out.println("Connected to localhost in port 8000");

			out = new ObjectOutputStream(requestSocket.getOutputStream());
			out.flush();
			in = new ObjectInputStream(requestSocket.getInputStream());
			is = requestSocket.getInputStream();
			byteOS.flush();

			// handshake();

			// initialize inputStream and outputStream

			// get Input from standard input
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

			while (true) {

				if (!recHandshake) {
					// ByteArrayOutputStream byteOS = new ByteArrayOutputStream();
					byteOS.write("P2PFILESHARINGPROJ".getBytes());
					byte[] zeroBits = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
					byteOS.write(zeroBits);
					byteOS.write(peerID);

					byte[] handshake = byteOS.toByteArray();
					sendMessage(handshake);

					System.out.println("client waiting for handshake");
					is.read(handshake, 0, handshake.length);
					String msgStr = new String(handshake, StandardCharsets.UTF_8);
					// System.out.println("Receive message: " + message + " from client " + no);
					System.out.println("Receive message: " + msgStr + " from server.");
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
			out.writeObject(msg);
			out.flush();
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

	// main method
	public static void main(String args[]) {
		Client client = new Client();
		client.run();
	}

}
