import java.io.*;
import java.net.*;
 
public class CClient {
    public static void main(String[] args) throws IOException {
         
        if (args.length != 2) {
            System.err.println(
                "Usage: java EchoClient <host name> <port number>");
            System.exit(1);
        }
        String hostName = args[0];
        
        int portNumber = Integer.parseInt(args[1]);
        DatagramSocket socket = new DatagramSocket();
 
            // send request
        InetAddress temp_ip = InetAddress.getLocalHost();
        String temp_hostname = temp_ip.getHostName();
        System.out.println("Your current IP address : " + temp_ip.getHostAddress());
        System.out.println("Your current Hostname : " + temp_hostname);
        

       try 
         {
            BufferedReader stdIn =
                new BufferedReader(new InputStreamReader(System.in));
           
            String fromUser = "";
 
            while (true) {

                System.out.println("Choose the operation to be performed");
                System.out.println("[1] Deposit");
                System.out.println("[2] Withdrawl");
                System.out.println("[3] CheckBalace");

                int ch = Integer.parseInt(stdIn.readLine());

                switch(ch)
                {
                    case 1: fromUser = getDepositDetails();
                            break;

                    case 2: fromUser = getWithdrawDetails();
                            break;

                    case 3: fromUser = getCheckBalaceDetails();
                            break;

                    default: System.out.println("Choose one of the above option");
                }
                
                //fromUser = stdIn.readLine();
                if (fromUser.equals("Bye."))
                    break;
                else {
                    byte[] buf = new byte[256];
                    buf = fromUser.getBytes();
                    InetAddress address = InetAddress.getByName(args[0]);
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, address, portNumber);
                    socket.send(packet);
                 
                        // get response

                    packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);
             
                    // display response
                    String received = new String(packet.getData(), 0, packet.getLength());
                    
                    System.out.println("Server: " + received);
                     
                    System.out.println("Client: " + fromUser);
                }
            }
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + hostName);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " +
                hostName);
            System.exit(1);
        }

                socket.close();
    }

    public static String getDepositDetails()
    {
        // generate requeset in the form bankname.clientNumber.sequenceNumber
        //return "DP,reqID,accoutNumber,amount"
        return "DP,reqId,1000";
    }

    public static String getWithdrawDetails()
    {
        return "WD,reqId,500";   
    }

    public static String getCheckBalaceDetails()
    {
        return "CB,reqId,ac12345";
    }

}