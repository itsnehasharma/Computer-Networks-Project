package FileChunkTransfer;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Sender extends Thread{
    private Socket socket;

    public Sender(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            File transfer = new File("alice.txt");
            byte [] fileInBytes = new byte[(int)transfer.length()];
            System.out.println(fileInBytes.length);
            FileInputStream fileInputStream = new FileInputStream(transfer);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);

            //fileInBytes contains the entire file in a byte array 
            bufferedInputStream.read(fileInBytes,0,fileInBytes.length); 

            OutputStream outputStream = socket.getOutputStream();
            System.out.println("Sending File . . .");

            boolean quit = false;
            Scanner sc = new Scanner(System.in);

            while (!quit) {
                System.out.println("starting byte: ");
                int start = sc.nextInt();
                System.out.println("ending byte");
                int end = sc.nextInt();

                //writes to the output stream a chunk from starting byte to ending byte 
                outputStream.write(fileInBytes,start,end);

                //this needs to change from user entering quit to the process itself sending messages 
                System.out.println("quit?");
                quit = sc.nextBoolean();
            }
            outputStream.flush();
            bufferedInputStream.close();
            sc.close(); 
            System.out.println("File Transfer Complete.");
        }catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
