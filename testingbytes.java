import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class testingbytes {

    public static void main(String[] args) throws IOException {
        
        byte [] zerobits = new byte[10];
        Arrays.fill(zerobits, (byte) 0);

        int peerIDInt = 1001;

        String headerStr = "P2PFILESHARINGPROJ";
        byte[] peerID = ByteBuffer.allocate(4).putInt(peerIDInt).array();
        byte[] header = headerStr.getBytes();

        System.out.println(Arrays.toString(header));
        System.out.println(Arrays.toString(zerobits));
        System.out.println(Arrays.toString(peerID));

        ByteArrayOutputStream byteOS = new ByteArrayOutputStream();
        byteOS.write(header);
        byteOS.write(zerobits);
        byteOS.write(peerID);

        byte[] handshake = byteOS.toByteArray();

        System.out.println(Arrays.toString(handshake));




        
    }
}
