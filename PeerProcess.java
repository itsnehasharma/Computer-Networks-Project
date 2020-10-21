import java.io.IOException;

public class PeerProcess {

    public static void main(String[] args) throws IOException{
        Server server = new Server(15123);
        Client client1 = new Client(15123, "client1");
        Client client2 = new Client(15123, "client2");
        Client client3 = new Client(15123, "client3");

    }
    
}
