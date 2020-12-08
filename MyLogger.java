import java.io.IOException;
import java.util.logging.*;

public class MyLogger {
    public static void main(String[] args) {  

        Logger logger = Logger.getLogger("BitTorrentLog");  
        FileHandler fh;  
    
        try {  
    
            // This block configure the logger with handler and formatter  
            fh = new FileHandler("BitTorrent.log");  
            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();  
            fh.setFormatter(formatter);  
    
            // the following statement is used to log any messages  
            logger.info("My first log");  
    
        } catch (SecurityException e) {  
            e.printStackTrace();  
        } catch (IOException e) {  
            e.printStackTrace();  
        }  
    
        logger.info("Hi How r u?");  
    
    }
}
