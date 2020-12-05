import java.io.*;
import java.net.Socket;

public class Main {
    public static void main(String[] args) {
        int size = 409600;
        int byteRead;
        int currentTotal = 0;
        try (Socket socket = new Socket("localhost",5000)){
            byte []bytes = new byte[size];
            InputStream inputStream = socket.getInputStream();
            FileOutputStream fileOutputStream = new FileOutputStream("received");
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
            byteRead = inputStream.read(bytes,0,bytes.length);
            currentTotal = byteRead;
            do {
                byteRead = inputStream.read(bytes,currentTotal,bytes.length-currentTotal);
                if (byteRead >= 0) currentTotal += byteRead;
            }while (byteRead>-1);
            bufferedOutputStream.write(bytes,0,currentTotal);
            bufferedOutputStream.flush();
            bufferedOutputStream.close();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
}
