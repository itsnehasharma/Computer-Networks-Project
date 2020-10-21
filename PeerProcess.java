import java.io.IOException;

public class PeerProcess {

    public static void main(String[] args) throws IOException{
        Server server = new Server(15123);
        Client client1 = new Client(15123, args[0]);
        Client client2 = new Client(15123, args[1]);
        Client client3 = new Client(15123, args[2]);

    }
    
}
