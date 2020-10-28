import java.io.IOException;

public class startServer {

    public static void main(String[] args) throws IOException{

        int portNum = Integer.valueOf(args[0]);
        Server server = new Server(portNum);
        
    }
}
    
