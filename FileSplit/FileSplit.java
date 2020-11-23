package FileSplit;

import java.io.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.Scanner;

public class FileSplit{
    public static HashSet<String> fileChunks = new HashSet();
    public static void main(String[] args) throws IOException {

        System.out.println("Please enter the path to the file you wish to split: ");
        Scanner scanner =new Scanner(System.in);
        String path = scanner.nextLine();

        File file = new File(path); //Taking file as input

        int size = 1024*100; //This is the size of the chunks that the file will be split into
        //1024 Bytes * 100 = 100 KB
        byte[] buffer = new byte[size]; //Byte array

        int counter = 1;

        try(FileInputStream fileInputStream = new FileInputStream(file);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream)) {

            int bytes = 0; //Var for naming files
            //Code to write each chunk in a different file with "counter" value as name of file
            while ((bytes=bufferedInputStream.read(buffer))>0){
                String part = Integer.toString(counter++);
                fileChunks.add(part); //Hashmap that keeps track of all files, to be used when transferring files
                //Each peer will have this and they will keep track of the chunks they receive in this hashmap
                //Will be written to a file that can be sent to peers for requesting more files

                File generated = new File(file.getParent(), part);//Creating nre chunk by the name of iterator
                try (FileOutputStream fileOutputStream = new FileOutputStream(generated)){
                    //Writing chunk data to file
                    fileOutputStream.write(buffer,0,bytes);
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        System.out.println("File has been split into chunks");
    }
}