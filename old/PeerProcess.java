import java.io.IOException;
import java.net.*;

//as of now this class starts client. please run start server first.
public class PeerProcess {

    public static void main(String[] args) throws IOException{

        int portNum = Integer.valueOf(args[0]);
        String clientName = args[1];
        String hostname = args[2];
        // Socket socket = new Socket("thunder.cise.ufl.edu", portNum);
        
        Client client = new Client (portNum, clientName, hostname);

    }
    
}
