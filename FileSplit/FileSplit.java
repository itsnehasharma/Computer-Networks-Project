import java.io.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.Scanner;

public class FileSplit{
    public static HashSet<String> fileChunks = new HashSet<String>(); //maybe use a map instead
    public static void main(String[] args) throws IOException {
        System.out.println("Please enter the path to the file you wish to split: ");
        Scanner scanner =new Scanner(System.in);
        String path = scanner.nextLine();
        File file = new File(path);
        int size = 1024*100; // 100 KB
        byte[] buffer = new byte[size];
        int counter = 1;
        try(FileInputStream fileInputStream = new FileInputStream(file);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream)) {
            int bytes = 0;
            while ((bytes=bufferedInputStream.read(buffer))>0){
                String part = Integer.toString(counter++);
                fileChunks.add(part); //this is a map that has all the chunks 
                File generated = new File(file.getParent(), part); //name of the file is part
                //maybe instead of generating a new file we can add all the pieces a map
                try (FileOutputStream fileOutputStream = new FileOutputStream(generated)){
                    fileOutputStream.write(buffer,0,bytes);
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }

    
        System.out.println("File has been split into chunks");
    }
}