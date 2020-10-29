package CurrentWorking.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class test {

    public static void main(String[] args) throws IOException {
        byte handshake[] = new byte[32];
        byte zeroBits[] = new byte[8];
        byte peerIDBits[] = new byte[4];

        for (int i = 0; i < zeroBits.length; i++) {
            System.out.println(zeroBits[i]);
        }

        String message = "P2PFILESHARINGPROJ";
        int peerID = 1001;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(message.getBytes());
        out.write(zeroBits);
        out.write(peerID);

        handshake = out.toByteArray();

        System.out.print(handshake);

    }

}
