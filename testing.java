import java.io.*;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class testing {

    public static void main(String[] args) throws IOException {

        FileHandler fileTxt;
        SimpleFormatter formatterTxt;

        final Logger LOGGER = 
                Logger.getLogger(testing.class.getName());
        
        LOGGER.setLevel(Level.INFO);

        int peerID = 1005;
        // get necessary properties from config
        BufferedReader reader = new BufferedReader(new FileReader("PeerInfo.txt"));

        String line = reader.readLine();

        HashMap<Integer, String[]> peerInfoMap = new HashMap<Integer, String[]>();

        while (line != null) {

            // System.out.println(line);
            String lineArr[] = line.split(" ");
            int tempPeerID = Integer.valueOf(lineArr[0]);
            String peerInfo[] = Arrays.copyOfRange(lineArr, 1, 4);
            peerInfoMap.put(tempPeerID, peerInfo);

            line = reader.readLine();
        }

        System.out.println("I am " + peerID);
        for (int id : peerInfoMap.keySet()) {
            // System.out.println("[" + id + "] " + Arrays.toString(peerInfoMap.get(id)));
            if (id < peerID){
                System.out.println("I need to connect to: [" + id + "] " + Arrays.toString(peerInfoMap.get(id)));
            }

            reader.close();
        }
    }
}
