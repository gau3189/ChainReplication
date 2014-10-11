import java.io.*;
import java.net.*;
import java.util.*;

public class CRServer {

        private String successor;
        private String predecessor;
        private String myadrress;

        private int udp_portNo;
        private int tcp_portNo;
        private int tcp_portNo2;
        private String hostName;

        /* TCP and UDP Sockets */
        DatagramSocket clientSocket;
        public static void main(String[] args) throws IOException {
         
        if (args.length != 2) {
            System.err.println("Usage: java CRServer <udp_port number> <tcp_port number1> <tcp_port number2");
            System.exit(1);
        }
        
        int result = init();
        /* Moving it to init()*/
        udp_portNo = Integer.parseInt(args[0]);
        tcp_portNo = Integer.parseInt(args[1]);
        tcp_portNo2= 5555;
        
        String hostName = "localhost";
        DatagramSocket clientSocket = null;
        if (tcp_portNo == tcp_portNo2)
        {
            tcp_portNo2 = 5554;
        }


        // InetAddress temp_ip = InetAddress.getLocalHost();
        // String temp_hostname = temp_ip.getHostName();
        // System.out.println("Your current IP address : " + temp_ip);
        // System.out.println("Your current Hostname : " + temp_hostname);
        /*
            Creating ServerSocket for listening from other servers
        */
            
        try 
        { 
                clientSocket = new DatagramSocket(udp_portNo);
                new CRMClientServerThread(clientSocket, hostName, tcp_portNo2).start();
                //new CRMClientServerThread(udp_portNo, hostName, tcp_portNo2).start();
                //System.out.println("Created TCP port for lsitening peer server");
        } 
        catch (Exception e) 
        {
            System.err.println("Could not listen on port " + udp_portNo);
            System.exit(-1);
        }

        try (
                ServerSocket serverSocket = new ServerSocket(tcp_portNo);
                Socket sSocket = serverSocket.accept();
                PrintWriter temp_out = new PrintWriter(sSocket.getOutputStream(), true);
                BufferedReader temp_in = new BufferedReader(new InputStreamReader(sSocket.getInputStream()));
            )
            { 
                System.out.println("Created TCP port for lsitening peer server port no="+ tcp_portNo);
                String inputLine, outputLine;
                byte[] buf = new byte[256];

                while ((inputLine = temp_in.readLine()) != null)
                {
                    System.out.println("inputLine = "+ inputLine);

                    String []retval = inputLine.split(":");
                    buf = retval[0].getBytes();
                    InetAddress address =  InetAddress.getByName(retval[1]);
                    int port = Integer.parseInt(retval[2]);

                    DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
                    clientSocket.send(packet);
                }
                    sSocket.close();
            } 
            catch (IOException e) 
            {
                System.err.println("Could not listen on port " + tcp_portNo);
                System.exit(-1);
            }
        
    }

    /* Intialization Function*/
    public static int init()
    {
        /*
            1. Read Configuration File and get the port number where to run 

        */
        /*
            Incase of mutliple IP's    
        Enumeration e1 = NetworkInterface.getNetworkInterfaces();
        while(e1.hasMoreElements())
        {
            NetworkInterface n = (NetworkInterface) e1.nextElement();
            Enumeration ee = n.getInetAddresses();
            while (ee.hasMoreElements())
            {
                InetAddress i = (InetAddress) ee.nextElement();
                System.out.println(i.getHostAddress());
            }
        }
        */
        udp_portNo = 4444;
        tcp_portNo = 5554;
        tcp_portNo2= 5555;

         InetAddress temp_ip = InetAddress.getLocalHost();
        String temp_hostname = temp_ip.getHostName();
        System.out.println("Your current IP address : " + temp_ip.getHostAddress());
        System.out.println("Your current Hostname : " + temp_hostname);
        
        
        myadrress = InetAddress.getLocalHost().getHostAddress();
        hostName = InetAddress.getLocalHost().getHostName();
        return 1;
    }

    public static void validateClientRequest()
    {
            //If the account is not present already create a new account 
            // IF invalid bankname don't update anything
    }

}