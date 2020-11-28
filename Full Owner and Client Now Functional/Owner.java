import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;

public class Owner {
    private static int port;
    private static HashSet<String> chunks = new HashSet<>();
    private static String path;

    Owner(){

    }

    public static void main(String[] args) throws Exception {
        port = Integer.parseInt(args[0]);
        //port = 2000;

        System.out.println("File Owner is now running");
        System.out.println("Please enter filepath :");

        Scanner scanner = new Scanner(System.in);
        path = scanner.nextLine();
        File file = new File(path); //Taking file as input

        int size = 1024*100; //This is the size of the chunks that the file will be split into
        //1024 Bytes * 100 = 100 KB
        byte[] buffer = new byte[size]; //Byte array

        int counter = 1;

        try(FileInputStream fileInputStream = new FileInputStream(file);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream)) {

            int bytes = 0; //Var for naming files
            //Code to write each chunk in a different file with "counter" value as name of file
            while ((bytes = bufferedInputStream.read(buffer))>0){
                String partName = Integer.toString(counter++);
                chunks.add(partName);//Hashmap that keeps track of all files, to be used when transferring files
                //Each peer will have this and they will keep track of the chunks they receive in this hashmap
                //Will be written to a file that can be sent to peers for requesting more files

                File generated = new File(file.getParent(), partName);//Creating nre chunk by the name of iterator
                try (FileOutputStream fileOutputStream = new FileOutputStream(generated)){
                    //Writing chunk data to file
                    fileOutputStream.write(buffer,0,bytes);
                }
            }
        }

        Iterator<String> iterator = chunks.iterator();
        int ChunksPerPeer = chunks.size()/5; //We divide by 5 because that's the number of peers we have
        ArrayList<String>[] Parts = new ArrayList[5]; //Array of arraylists to save details of chunks being transferred

        for (int i = 0; i<5; i++){
            Parts[i] = new ArrayList<>();
        }

        int count = 0;
        //Storing chunkIDs in different array lists that will be transmitted to each peer
        for (int i = 0; i<5 ; i++){
            for (int j = 0; j< ChunksPerPeer; j++){
                Parts[i].add(iterator.next());
                count++;
            }
        }

        //Adding chunks to the first peer in case there are any leftover chunks that have not yet been allocated to any peers
        if (count<chunks.size()){
            int difference = chunks.size() - count;
            for (int i = 0; i<difference; i++){
                Parts[0].add(iterator.next());
            }
        }

        ServerSocket serverSocket = new ServerSocket(port);
        int peerNum = 1;
        try {
            while (true) {
                new Server(serverSocket.accept(),peerNum,Parts).start();
                System.out.println("Peer " + peerNum + " Connected");
                peerNum++;
            }
        } finally {
            serverSocket.close();
        }


    }

    private static class Server extends Thread {
        private Socket socket;
        private ObjectInputStream objectInputStream;
        private ObjectOutputStream objectOutputStream;
        private InputStream inputStream;
        private int peerNum;
        private ArrayList<String>[] Parts;
        private FileInputStream fileInputStream;
        private DataOutputStream dataOutputStream;

        public Server(Socket socket, int peerNum, ArrayList<String>[] Parts) {
            this.socket = socket;
            this.peerNum = peerNum;
            this.Parts = Parts;
        }

        @Override
        public void run() {
            try {

                objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                objectInputStream = new ObjectInputStream(socket.getInputStream());

                Message(path); //Sending file path to peers
                Message(Integer.toString(chunks.size())); //Sending number of chunks to peers

                String[] SizeList = new String[Parts[peerNum-1].size()];
                objectOutputStream.writeObject(Parts[peerNum-1]);

                for (int i = 0; i<Parts[peerNum-1].size(); i++){
                    File file = new File(Parts[peerNum-1].get(i));
                    SizeList[i] = Long.toString(file.length());
                }

                objectOutputStream.writeObject(SizeList); //Sending File Lengths to peers
                objectOutputStream.flush();

                for (int i = 0; i<Parts[peerNum-1].size(); i++){
                    File file = new File(Parts[peerNum-1].get(i));
                    fileInputStream = new FileInputStream(file);
                    dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    byte[] buffer = new byte[100*1024]; //Byte array of the same size as chunks

                    while (fileInputStream.read(buffer)>0){
                        dataOutputStream.write(buffer);
                    }
                    dataOutputStream.flush();
                }
            }catch (IOException ioException){
                ioException.printStackTrace();
            } finally { //Finally block to close all opened connections that may still be open
                if (objectInputStream != null) {
                    try {
                        objectInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (objectOutputStream != null) {
                    try {
                        objectOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (dataOutputStream != null) {
                    try {
                        dataOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void Message(String string) {
            try {
                objectOutputStream.writeObject(string);
                objectOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
