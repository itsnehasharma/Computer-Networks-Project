import java.io.*;
import java.net.Socket;

public class Sender extends Thread{
    private Socket socket;

    public Sender(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            File transfer = new File("D:\\Shreyans\\Java\\File to send\\Document");
            byte [] bytes = new byte[(int)transfer.length()];
            FileInputStream fileInputStream = new FileInputStream(transfer);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
            bufferedInputStream.read(bytes,0,bytes.length);
            OutputStream outputStream = socket.getOutputStream();
            System.out.println("Sending File . . .");
            outputStream.write(bytes,0,bytes.length);
            outputStream.flush();
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
