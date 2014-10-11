import java.net.*;
import java.io.*;
 
public class CRMServerThread extends Thread {
    private Socket socket = null;
 
    public CRMServerThread(Socket socket) {
        super("CRMServerThread");
         System.out.println("in CRMServerThread");
        this.socket = socket;
    }
     
    public void run() {
 
        try (
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                new InputStreamReader(
                    socket.getInputStream()));
        ) {
            System.out.println("in CRMServerThread");
            String inputLine, outputLine;
           
            while ((inputLine = in.readLine()) != null) {
                System.out.println("whiel loop");
                out.println(inputLine);
                
            }
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
