import java.net.*;
import java.io.*;
 
public class CRMClientServerThread extends Thread {
    private DatagramSocket socket = null;
    private Socket sSocket = null;
    private int tcp_portNo2;
    private String hostName;
    public CRMClientServerThread(DatagramSocket socket,String hostName,int tcp_portNo2) 
    {
         super("CRMClientServerThread");
         System.out.println("in CRMClientServerThread");
        
        try
        {
            //this.socket = new DatagramSocket(udpPortNo);
            this.socket = socket;
            this.tcp_portNo2 = tcp_portNo2;
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
            /*try
            {
                if(sSocket == null)
                {
                    System.out.println("Socket creation");
                    this.sSocket = new Socket(hostName, tcp_portNo2);
                    out = new PrintWriter(sSocket.getOutputStream(), true);
                }
            }
            catch(Exception e)
            {
                System.out.println("Exception"+ e);
            }
                if(out != null)
                {
                    System.out.println("Sending to other server"+dString);
                    out.println(dString);
                
                }
                */
                InetAddress address = packet.getAddress();
                int port = packet.getPort();

                packet = new DatagramPacket(buf, buf.length, address, port);
                socket.send(packet);
                System.out.println(" address"+ address);

            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
