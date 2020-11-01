import java.io.IOException;
import java.net.ServerSocket;

public class Main {
    public static void main(String[] args) {
        try(ServerSocket serverSocket = new ServerSocket(5000)) {
            while (true){
                new Sender(serverSocket.accept()).start();
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
