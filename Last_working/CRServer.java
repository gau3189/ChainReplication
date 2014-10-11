import java.io.*;
import java.net.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CRServer {

        private static String successor;
        private static String predecessor;
        private static String myadrress;
        private static String masterAddress;
        private static String bank_Name;

        private static int my_udp_portNo;
        private static int my_tcp_portNo;
        private static int succ_tcp_portNo;
        private static int master_tcp_portNo;
        private static String hostName;

        private static String configFile;

        /* TCP and UDP Sockets */
        DatagramSocket clientSocket;
        public static void main(String[] args) throws IOException {
         
        if (args.length != 2) {
            System.err.println("Usage: java CRServer <config File> <Bank Name>");
            System.exit(1);
        }
        
        
        //System.exit(1);
        /* Moving it to init()*/
       // my_udp_portNo = Integer.parseInt(args[0]);
       // my_tcp_portNo = Integer.parseInt(args[1]);
        bank_Name = args[1];
        configFile = args[0];
        succ_tcp_portNo= 5555;
        int result = init();
        String hostName = "localhost";
        String message = myadrress + ";" + my_tcp_portNo + ";" + "GET_PRED_SUCC";
        String inputLine, outputLine;
        DatagramSocket clientSocket = null;
        if (my_tcp_portNo == succ_tcp_portNo)
        {
            succ_tcp_portNo = 5554;
        }

                try
                {
                       Socket master_Socket = new Socket(hostName, master_tcp_portNo);
                        PrintWriter master_out = new PrintWriter(master_Socket.getOutputStream(), true);
                        BufferedReader master_in = new BufferedReader(
                                new InputStreamReader(master_Socket.getInputStream()));
                        //master_out.println(message+"succ_predecessor_address");

                        master_out.println(message);
                        System.out.println("Sent Master a request");

                        //master_out.println(message+"succ_predecessor_address");
                        while ((inputLine = master_in.readLine()) != null)
                        {
                            System.out.println("inputLine = "+ inputLine);
                            break;
                        }
                    
                }
                catch(Exception e)
                {
                    System.out.println("Exception"+ e);
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
                clientSocket = new DatagramSocket(my_udp_portNo);
                new CRMClientServerThread(clientSocket, hostName, succ_tcp_portNo).start();
                //new CRMClientServerThread(udp_portNo, hostName, succ_tcp_portNo).start();
                //System.out.println("Created TCP port for lsitening peer server");
        } 
        catch (Exception e) 
        {
            System.err.println("Could not listen on port " + my_udp_portNo);
            System.exit(-1);
        }

    /*    try (
                ServerSocket serverSocket = new ServerSocket(my_tcp_portNo);
                Socket sSocket = serverSocket.accept();
                PrintWriter temp_out = new PrintWriter(sSocket.getOutputStream(), true);
                BufferedReader temp_in = new BufferedReader(new InputStreamReader(sSocket.getInputStream()));
            )
            { 
               System.out.println("Created TCP port for lsitening peer server port no="+ my_tcp_portNo);
                
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
                System.err.println("Could not listen on port " + my_tcp_portNo);
                System.exit(-1);
            }
        */
    }

    /* Intialization Function*/
    public static int init()
    {
        /*
            1. Read Configuration File and get the port number where to run 
            2. Master IP address and port No

        */
        String temp = "";
        Path path = Paths.get(configFile);
        
        //my_udp_portNo = 4444;
        //my_tcp_portNo = 5554;
        succ_tcp_portNo= 5555;
        try
        {
            InetAddress temp_ip = InetAddress.getLocalHost();
            String temp_hostname = temp_ip.getHostName();
            System.out.println("Your current IP address : " + temp_ip.getHostAddress());
            System.out.println("Your current Hostname : " + temp_hostname);

            myadrress = InetAddress.getLocalHost().getHostAddress();
            hostName = InetAddress.getLocalHost().getHostName();

            BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
            String line = "", tcp_temp="", udp_temp = "";
            int count = 0;

            while ((line = reader.readLine()) != null ) {
                if(!line.isEmpty()) 
                {
                    if (count == 4)
                        break;

                    String []val = line.split(" :");
                    if (val.length == 2)
                    {
                        if (val[0].trim().equals("MASTER_IP_ADDRESS"))
                        {
                           masterAddress = val[1];
                           count++;
                        }
                        if (val[0].trim().equals("MASTER_TCP_PORTNO"))
                        {
                            master_tcp_portNo = Integer.parseInt(val[1].trim());
                            count++;
                        }
                        if (val[1].trim().equals(myadrress))
                        {
                            tcp_temp = "TCP_PORT_NUMBER_" + val[0].trim().split("_")[2];
                            udp_temp = "UDP_PORT_NUMBER_" + val[0].trim().split("_")[2];
                        }

                        if (val[0].trim().equals(tcp_temp))
                        {
                            
                            my_tcp_portNo = Integer.parseInt(val[1].trim());
                             try 
                            {
                                ServerSocket serverSocket = new ServerSocket(my_tcp_portNo);
                                serverSocket.close();
                            }
                            catch(Exception e)
                            {
                                continue;
                            }
                            count++;
                        }

                        if (val[0].trim().equals(udp_temp))
                        {
                            if(count != 3)
                                continue;
                            else
                            {
                                my_udp_portNo = Integer.parseInt(val[1].trim());
                                count++;
                            }
                        }   
                    } 
                }    
            }

        }
        catch(Exception e)
        {
            System.out.println("Exception = " + e);
        }
        
        return 1;
    }

    public static void validateClientRequest()
    {
            //If the account is not present already create a new account 
            // IF invalid bankname don't update anything
    }

}
