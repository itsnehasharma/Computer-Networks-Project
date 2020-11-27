import java.io.*;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Peer {
    //Variables
    private static int Chunks;      //Stores chunk quantity (No. of chunks that the file was split into)
    private static int ServerPort;  //Self Server Port, When this peer acts as a server
    private static int ClientPort;  //Self Client Port, When this peer acts as a client
    private static HashSet<String> recvdChunks = new HashSet<>(); //Chunks that have been received by the Peer
    private static String path;



    public static void main(String[] args) throws IOException {
        //Code to start both upload and download
        //int ownerPort = Integer.parseInt(args[0]);
        int ownerPort = 2000;
        Socket fileOwner; //Socket to get data from teh fileowner
        Socket socket = null;  //Socket to get data from another peer
        ServerSocket serverSocket;
        ObjectOutputStream objectOutputStream = null;   //Stream to write to the socket
        ObjectInputStream objectInputStream = null;     //Stream to be read from socket
        DataInputStream dataInputStream = null;
        FileOutputStream fileOutputStream = null;

        //ServerPort = 2001;
        //ClientPort = 2002;

        ServerPort = Integer.parseInt(args[1]);
        ClientPort = Integer.parseInt(args[2]);

        try {
            fileOwner = new Socket("localhost", ownerPort);
            System.out.println("Connected to Owner at port: " + ownerPort);
            objectInputStream = new ObjectInputStream(fileOwner.getInputStream());
            System.out.println("Debug 1");
            objectOutputStream = new ObjectOutputStream(fileOwner.getOutputStream());
            System.out.println("Debug 2");
            String chunkNum;

            path = (String) objectInputStream.readObject();
            System.out.println("Debug 3");
            Chunks = Integer.parseInt((String)objectInputStream.readObject());
            System.out.println("Debug 4");
            ArrayList<String> Parts = (ArrayList<String >)objectInputStream.readObject();
            System.out.println("Debug 5");
            String[] SizeList = (String[]) objectInputStream.readObject();
            System.out.println("Debug 6");

            for (int i = 0; i<Parts.size(); i++){
                chunkNum = Parts.get(i);
                int size = Integer.parseInt(SizeList[i]);
                recvdChunks.add(Parts.get(i));
                fileOutputStream = new FileOutputStream(chunkNum);
                dataInputStream = new DataInputStream(fileOwner.getInputStream());
                byte[] buffer = new byte[i];
                int read;
                for (int j = 0; j<size; j++) {
                    read = dataInputStream.read(buffer,0,buffer.length);
                    if (read > 0) {
                        fileOutputStream.write(buffer,0,read);
                    }
                }
                fileOutputStream.flush();
            }
            fileOwner.close();

            PrintWriter printWriter = new PrintWriter("ReceivedChunks.txt"); //File where we store the chunk ids that have been received by the peer
            System.out.println("Received Chunks File Created");
            for (String ch: recvdChunks) {
                printWriter.println(ch);;
            }printWriter.close();

            serverSocket = new ServerSocket(ServerPort);
            boolean connect = false;

            try {
                while (true) {
                    try {
                        if (!connect) {
                            socket = new Socket("localhost", ClientPort);
                            new Client(socket).start();
                            connect = true;
                        }
                        new Server(serverSocket.accept()).start();
                        if (recvdChunks.size() == Chunks) {
                            break;
                        }
                    } catch (ConnectException e) {
                        System.out.println("Cannot Connect to peer");
                    } catch (UnknownHostException e) {
                        System.out.println("Unknown Host");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("Enters merging");
            } finally {
                serverSocket.close();
                socket.close();
            }
        } catch (ConnectException e) {
            System.out.println("Connection Refused");
        } catch (UnknownHostException e) {
            System.out.println("You're connecting to an unknown host");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.out.println("Class Not Found");
        } finally {
            if (objectInputStream != null) objectInputStream.close();
            if (objectOutputStream != null) objectOutputStream.close();
            if (fileOutputStream != null) fileOutputStream.close();
            if (dataInputStream != null) dataInputStream.close();
        }
    }

    //Implementing a client in the peer to receive missing file chunks
    public static class Client extends Thread {
        private Socket Client_Socket;
        private static ObjectOutputStream objectOutputStream;   //Output Stream to the socket
        private static ObjectInputStream objectInputStream;     //Input Stream from the socket
        private static OutputStream outputStream;
        private static InputStream inputStream;

        //Constructor
        public Client(Socket clientSocket) {
            this.Client_Socket = clientSocket;
        }

        @Override
        public void run() {
            byte[] buffer;
            int read;
            String random_String;

            System.out.println("Client Started Running");

            try {
                objectInputStream = new ObjectInputStream(Client_Socket.getInputStream());
                objectOutputStream = new ObjectOutputStream(Client_Socket.getOutputStream());

                while (recvdChunks.size() != Chunks) {
                    Random random = new Random();
                    HashSet<String> downloadNeighborChunks = (HashSet<String>) objectInputStream.readObject();
                    System.out.println("Peer" + ServerPort + "has received Chunks list of peer" + ClientPort);
                    HashSet<String> pendingChunks = downloadNeighborChunks; //This will be used to download these chunks from one peer and the chain reaction will cause us to download all files
                    pendingChunks.removeAll(recvdChunks);

                    if (pendingChunks.size() == 0){
                        Message("Equal Set", objectOutputStream);
                        continue;
                    } else {
                        Message("Not Equal", objectOutputStream);
                    }

                    sleep(600);
                    int index = random.nextInt(pendingChunks.size());
                    Iterator<String> iterator = pendingChunks.iterator();
                    for (int i = 0; i<index; i++){
                        iterator.next();
                    }random_String = iterator.next();

                    Message(random_String, objectOutputStream);
                    System.out.println("Peer" + ServerPort + "requested" + random_String);

                    sleep(600);
                    recvdChunks.add(random_String);
                    updateFile(random_String);

                    inputStream = Client_Socket.getInputStream();
                    outputStream = new FileOutputStream("./" + random_String);

                    int fileSize = Integer.parseInt((String)objectInputStream.readObject());

                    buffer = new byte[1];

                    for (int i = 0; i<fileSize; i++) {
                        read = inputStream.read(buffer);
                        outputStream.write(buffer,0,read);
                    }
                    outputStream.flush();
                    System.out.println("Peer " + ServerPort + " received chunk " + random_String
                            + " from " + ClientPort);

                    //When all the chunks of the file have been received,
                    if (recvdChunks.size() == Chunks) {
                        System.out.println("Entire file received :)");
                        List<Integer> range = IntStream.rangeClosed(1, recvdChunks.size()).boxed().collect(Collectors.toList());

                        List<File> ListFile = new ArrayList();
                        for (int r : range) {
                            File file = new File("./" + r);
                            ListFile.add(file);
                        }

                        try (FileOutputStream fos = new FileOutputStream("./" + path); BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                            for (File file : ListFile) {
                                Files.copy(file.toPath(), bos);
                            }
                        }
                        sleep(600);
                    } //else {
                       // System.out.println("Not yet done.");
                    //}
                }

            } catch (IOException | ClassNotFoundException | InterruptedException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (objectInputStream!=null){
                        objectInputStream.close();
                    }
                    if (objectOutputStream!=null){
                        objectOutputStream.close();
                    }
                    if (inputStream!=null){
                        inputStream.close();
                    }
                    if (outputStream!=null){
                        outputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //Implementing a server in the peer to send file chunks to peers
    public static class Server extends Thread {
        private Socket Server_socket;
        private static ObjectOutputStream objectOutputStream;
        private static ObjectInputStream objectInputStream;
        private static OutputStream outputStream;
        private static InputStream inputStream;

        public Server(Socket server_socket) {
            this.Server_socket = server_socket;
        }

        @Override
        public void run() {
            System.out.println("Client Started Running");
            try {
                objectOutputStream = new ObjectOutputStream(Server_socket.getOutputStream());
                objectInputStream = new ObjectInputStream(Server_socket.getInputStream());
                HashSet<String> avlChunks;

                while (true) {
                    sleep(600);
                    byte[] buffer;
                    int read;
                    String random_String;
                    avlChunks = getChunks();

                    objectOutputStream.writeObject(avlChunks);
                    System.out.println("Peer " + ServerPort + " sent available chunks to " + ClientPort);
                    objectOutputStream.flush();
                    sleep(600);
                    if (((String)objectInputStream.readObject()).equals("Equal Set")) {
                        continue;
                    }

                    random_String = (String)objectInputStream.readObject();
                    System.out.println("Received request for chunks from :" + ClientPort);
                    sleep(600);
                    File file = new File(random_String);
                    Message(Long.toString(file.length()), objectOutputStream);

                    inputStream = new FileInputStream(file);
                    outputStream = Server_socket.getOutputStream();
                    buffer = new byte[100*1024];

                    while ((read = inputStream.read(buffer))>0) {
                        outputStream.write(buffer,0,read);
                    }
                    System.out.println("Chunk :" + random_String + " sent to " + ClientPort);
                    outputStream.flush();
                }

            } catch (IOException | ClassNotFoundException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        private HashSet<String> getChunks() {
            HashSet<String> chunks = new HashSet<>();
            File file  = new File("ReceivedChunks.txt");
            try {
                Scanner scanner = new Scanner(file);
                while (scanner.hasNextLine()){
                    chunks.add(scanner.nextLine());
                }
            } catch (FileNotFoundException e) {
                System.out.println("File not found");
            }
            return chunks;
        }
    }

    //Method to send messages from the server of this peer to the client of another peer
    private static void Message(String string, ObjectOutputStream objectOutputStream) {
        try {
            objectOutputStream.writeObject(string);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Method to update the contents of the Received chunks file when a chunk has been received
    public static void updateFile(String s){
        try {
            Files.write(Paths.get("ReceivedChunks.txt"), (s + "\n").getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.out.println("File not found");
        }
    }
}


/*
Client
private Socket socket_c;
        private ObjectOutputStream objectOutputStream_c;  //Writing to the socket
        private ObjectInputStream objectInputStream_c;    //Reading from the socket
        private OutputStream outputStream_c;
        private InputStream inputStream_c;

        public Client(Socket socket_c) {
            this.socket_c = socket_c;
        }

        @Override
        public void run() {
            try{
                objectOutputStream_c = new ObjectOutputStream(socket_c.getOutputStream());
                objectOutputStream_c = new ObjectOutputStream(socket_c.getOutputStream());

            } catch (IOException e){

            }
        }
 */