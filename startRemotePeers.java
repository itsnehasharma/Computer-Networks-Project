import java.io.IOException;

public class startRemotePeers {

    public static void main(String[] args) throws IOException {
        
        
        String hostname = "shasharm@thunder.cise.ufl.edu";

        
        Runtime.getRuntime().exec("ssh " + hostname + "Vivace2998");

        // String workingDir = System.getProperty("user.dir");
        // System.out.println(workingDir);

        String workingDir = "/cise/homes/shasharm/hashtag-counter";
        
        
        Runtime.getRuntime().exec("cd " + workingDir + "; java hashtagCounter input.txt");

    }
    
}
