package FileChunkTransfer;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        int fileSize = 148481; //this will be taken from config file in the future 
        // int byteRead = 0;
        // int currentTotal = 0;
        try (Socket socket = new Socket("localhost",5000)){
            byte [] finalFileInBytes = new byte[fileSize];
            InputStream inputStream = socket.getInputStream();

            FileOutputStream fileOutputStream = new FileOutputStream("received.txt");
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
  
            boolean quit = false;
            Scanner sc = new Scanner(System.in);

            while (!quit) {
                System.out.println("starting byte: ");
                int start = sc.nextInt();
                System.out.println("ending byte");
                int end = sc.nextInt();

                //this writes INTO the array final file in bytes at byte[start] until byte[end] 
                inputStream.read(finalFileInBytes,start,end);
                System.out.println("quit?");
                quit = sc.nextBoolean();
            }

            //at the end of the program, write the finalFileInBytes array into a text file. 
            //This is the easiest way to combine chunks into files.
            //note that if all the bytes of the file are not written within that loop ^ the file will look weird and have random bytes
            //if only want to transfer for example, 0 to 160 bytes, change fileSize to 160. 
            bufferedOutputStream.write(finalFileInBytes,0,fileSize);

            bufferedOutputStream.flush();
            sc.close();
            bufferedOutputStream.close();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
}
