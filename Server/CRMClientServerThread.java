import java.net.*;
import java.io.*;
 
public class CRMClientServerThread extends Thread {
    private DatagramSocket socket = null;
    private Socket sSocket = null;
    private int succPortNo;
    private String hostName;
    public CRMClientServerThread(DatagramSocket socket,String hostName,int succPortNo) 
    {
         super("CRMClientServerThread");
         System.out.println("in CRMClientServerThread");
        
        try
        {
            //this.socket = new DatagramSocket(udpPortNo);
            this.socket = socket;
            this.succPortNo = succPortNo;
            this.hostName = hostName; 
        }
        catch(Exception e)
        {
            System.out.println("Exception"+ e);
        }
    }
     
    public void run() {
 
        try  
        {
            String dString;
            PrintWriter out = null;
            
            
            
            while (true)
            {

                byte[] buf = new byte[256];
 
                // receive request
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
 
                String receivedMessage = new String(buf);
                System.out.println("Recieved = \n" + receivedMessage);

                String []retval = receivedMessage.split(";");
                for (String rval: retval)
                         System.out.println(rval);

                switch(retval[0])
                {
                    case "WD":  dString = "withdraw Successful";
                                break;
                    case "DP":  dString = "deposit successful";
                                break;
                    case "GB":  dString = "Check balance SUCCESSFUL" ;
                                break;
                    default:    dString = "NOP";
                                break;
                }
                // figure out response
                //dString ="UDP server mess";
 
                buf = dString.getBytes();
 
                // send the response to the client at "address" and "port"
                dString = dString + ":" + packet.getAddress().getHostAddress()+ ":" + packet.getPort()  ;
                System.out.println("Dstring = "+ dString); 

                if(sSocket == null)
                {
                    System.out.println("CRMClientServerThread Socket creation");
                    this.sSocket = new Socket(hostName, succPortNo);
                    out = new PrintWriter(sSocket.getOutputStream(), true);
                }

               
                if( succPortNo!=0 && out != null)
                {
                    System.out.println("Sending to other server" + dString);
                    out.println(dString);
                }
                else
                {
                    InetAddress address = packet.getAddress();
                    int port = packet.getPort();

                    packet = new DatagramPacket(buf, buf.length, address, port);
                    socket.send(packet);
                   
                }   
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
