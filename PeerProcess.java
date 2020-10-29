import java.io.*;
import java.util.*;

public class PeerProcess {

    public static void main(String[] args) throws FileNotFoundException {

        int peerID = Integer.valueOf(args[0]);

        FileReader fr = new FileReader("PeerInfo.txt");
        // BufferedReader br = new BufferedReader(fr);
        Scanner sc = new Scanner(fr);

        List<String[]> peerInformation = new ArrayList<String[]>();
        while (sc.hasNextLine()) {
            String info = sc.nextLine();
            String[] infoSplit = info.split(" ");
            peerInformation.add(infoSplit);
        }

        //this connects clients but it does not work because we first need to start servers . 
        for (String[] line : peerInformation) {
            int pid = Integer.valueOf(line[0]);
            String hostname = line[1];
            int port = Integer.valueOf(line[2]);

            //connects a new client for every previous host in the list 
            if (peerID > pid) {
                Client c = new Client(pid, hostname, port);
                c.run();
            } else {
                break;
            }
        }
        
        sc.close();

    }
}
