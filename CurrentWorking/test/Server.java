import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Server {

	private static final int sPort = 8000; // The server will be listening on this port number

	public static void main(String[] args) throws Exception {
		System.out.println("The server is running.");
		ServerSocket listener = new ServerSocket(sPort);
		int clientNum = 1;

		try {
			while (true) {
				new Handler(listener.accept(), clientNum).start();
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
		private String message; // message received from the client
		private String MESSAGE; // uppercase message send to the client
		private Socket connection;
		private ObjectInputStream in; // stream read from the socket
		private ObjectOutputStream out; // stream write to the socket
		private int no; // The index number of the client
		private boolean recHandshake = false;
		// private byte[] handshakeMessage;
		private InputStream is;
		private int peerID = 1001;
		private byte[] zeroBits = {0,0,0,0,0,0,0,0,0,0};
		private byte[] msg = new byte[32];

		public Handler(Socket connection, int no) {
			this.connection = connection;
			this.no = no;
		}

		public void run() {

			try {
				// initialize Input and Output streams
				out = new ObjectOutputStream(connection.getOutputStream());
				out.flush();
				in = new ObjectInputStream(connection.getInputStream());
				is = connection.getInputStream();
				// byteIn = new ByteArrayInputStream(connection.getInputStream());
				// byteIn = new input

				while (true) {

					// receive the message sent from the client
					// message = (String) in.readObject();

					if (!recHandshake) {
						System.out.println("server waiting for handshake");
						is.read(msg, 0, msg.length);
						String msgStr = new String(msg, StandardCharsets.UTF_8);
						// System.out.println("Receive message: " + message + " from client " + no);
						System.out.println("Receive message: " + msgStr + " from client " + no);


						String s = new String(msg, StandardCharsets.UTF_8);
						System.out.println("handshake: " + s);

						ByteArrayOutputStream byteOS = new ByteArrayOutputStream();
						// byte zeroBits[] = new byte[8];
						byteOS.write("P2PFILESHARINGPROJ".getBytes());
						byteOS.write(zeroBits);
						byte[] peer = ByteBuffer.allocate(4).putInt(peerID).array();
						String i = new String(peer, StandardCharsets.UTF_8);
						System.out.println(i);
						byteOS.write(peer);

						byte[] handshake = byteOS.toByteArray();
						String hand = new String(handshake, StandardCharsets.UTF_8);
						System.out.println("hand: " + hand + " length: " + hand.length());
						sendMessage(handshake);

						recHandshake = true;
						msg = new byte[120000];

					} else {
						is.read(msg, 0, msg.length);
						// show the message to the user
						String msgStr = new String(msg, StandardCharsets.UTF_8);
						// System.out.println("Receive message: " + message + " from client " + no);
						System.out.println("Receive message: " + msgStr + " from client " + no);
						// Capitalize all letters in the message
						// MESSAGE = message.toUpperCase();
						MESSAGE = msgStr.toUpperCase();
						// send MESSAGE back to the client
						sendMessage(MESSAGE);

					}

				}
			} catch (IOException ioException) {
				System.out.println("Disconnect with Client " + no);
			}
			// catch (ClassNotFoundException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }
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

		// send a message to the output stream
		public void sendMessage(String msg) {
			try {
				out.writeObject(msg);
				out.flush();
				System.out.println("Send message: " + msg + " to Client " + no);
			} catch (IOException ioException) {
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

		// public void handshake() throws ClassNotFoundException, IOException {
		// 	System.out.println("server waiting for handshake");
		// 	byte[] h = new byte[32];
		// 	is.read(h, 0, 32);
		// 	// String handshake = in.readObject().toString();
		// 	System.out.println("handshake: " + h);

		// 	ByteArrayOutputStream byteOS = new ByteArrayOutputStream();
		// 	byte zeroBits[] = new byte[8];
		// 	byteOS.write("P2PFILESHARINGPROJ".getBytes());
		// 	byteOS.write(zeroBits);
		// 	byteOS.write(peerID);

		// 	byte[] handshake = byteOS.toByteArray();
		// 	sendMessage(handshake);
		// }

		// public void handshake(byte[] msg) throws ClassNotFoundException, IOException {

		// 	// byte[] h = new byte[32];
		// 	// is.read(h, 0, 32);
		// 	// // String handshake = in.readObject().toString();
		// 	// System.out.println("handshake: " + h);

		// 	ByteArrayOutputStream byteOS = new ByteArrayOutputStream();
		// 	byte zeroBits[] = new byte[8];
		// 	byteOS.write("P2PFILESHARINGPROJ".getBytes());
		// 	byteOS.write(zeroBits);
		// 	byteOS.write(peerID);

		// 	byte[] handshake = byteOS.toByteArray();
		// 	sendMessage(handshake);
		// }

	}

}
