import java.io.*;
import java.util.*;

public class PeerProcess {

    public static void main(String[] args) throws FileNotFoundException {

        int peerID = Integer.valueOf(args[0]);
        // int peerID = 1001;
        System.out.println(peerID);

        FileReader fr = new FileReader("PeerInfo.txt");
        // BufferedReader br = new BufferedReader(fr);
        Scanner sc = new Scanner(fr);

        List<String[]> peerInformation = new ArrayList<String[]>();
        while (sc.hasNextLine()) {
            String info = sc.nextLine();
            String[] infoSplit = info.split(" ");
            peerInformation.add(infoSplit);
        }

        //for this to work all servers need to be running  
        for (String[] line : peerInformation) {
            int pid = Integer.valueOf(line[0]);
            System.out.println(pid);
            String hostname = line[1];
            System.out.println(hostname);
            int port = Integer.valueOf(line[2]);
            System.out.println(port);

            //currently does not work correctly 
            if (peerID > pid) {
                Client c = new Client(peerID, hostname, port);
                c.run();
            } else {
                // break;
            }
        }
        sc.close();

    }
}
