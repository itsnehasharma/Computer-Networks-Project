import java.io.*;
import java.net.*;

public class Main {
    public static void main(String[] args) {
        try(ServerSocket serverSocket = new ServerSocket(5000)) {


            while (true) {
//                Socket socket = serverSocket.accept();
//                Echoer echoer = new Echoer(socket);
//                echoer.start(); //These three linesof code are the same as the single line below
                new Echoer(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
