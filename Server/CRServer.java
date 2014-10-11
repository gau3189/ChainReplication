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


import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
public class CRServer {

       
        private  String successor;
        private  String predecessor;
        private  String myAddress;
        private  String masterAddress;
        private  String bankName;

        private  int udpPortNo;
        private  int tcpPortNo;
        private  int succPortNo;
        private  int predPortNo;
        private  int masterPortNo;
        private  int chainLength;

        private  String hostName;
        private  String configFile;

    
        public CRServer(String configFile, String bankname)
        {
            this.configFile = configFile;
            this.bankName = bankname;
                    
        }
        public static void main(String[] args) throws IOException {
         
        if (args.length != 2) {
            System.err.println("Usage: java CRServer <config File> <Bank Name>");
            System.exit(1);
        }
        
        CRServer server = new CRServer(args[0], args[1]);
        
        int result = server.init();

        System.out.println("master addr = "+ server.masterAddress);
        System.out.println("master port = "+ server.masterPortNo);
        System.out.println("MY address = "+ server.myAddress);
        System.out.println("successor port = "+ server.succPortNo);
        System.out.println("server Count = "+ server.chainLength);  
        System.out.println("server udpPortNo  = "+ server.udpPortNo);
        System.out.println("server tcpPortNo  = "+ server.tcpPortNo);
        
        String inputLine, outputLine;
        DatagramSocket clientSocket = null;
        
        /*
            Creating ServerSocket for listening from other servers
        */
            
        try 
        { 
                clientSocket = new DatagramSocket(server.udpPortNo);
                new CRMClientServerThread(clientSocket, server.hostName, server.succPortNo).start();
        } 
        catch (Exception e) 
        {
            System.err.println("Could not listen on port " + server.udpPortNo);
            System.exit(-1);
        }

        Socket skt = null;
        PrintWriter out = null;
        
               
        try (
                ServerSocket serverSocket = new ServerSocket(server.tcpPortNo);
                Socket sSocket = serverSocket.accept();
                PrintWriter temp_out = new PrintWriter(sSocket.getOutputStream(), true);
                BufferedReader temp_in = new BufferedReader(new InputStreamReader(sSocket.getInputStream()));

            )
            { 
                System.out.println("Created TCP port for lsitening peer server port no="+ server.tcpPortNo);
                /*System.out.println(sSocket.isBound());
                System.out.println(sSocket.isConnected());
                System.out.println(sSocket.isInputShutdown());
                System.out.println(sSocket.isClosed());
                */
               

                byte[] buf = new byte[256];
                while ((inputLine = temp_in.readLine()) != null)
                {
                    System.out.println("input Line="+ inputLine);
                    if(server.succPortNo!=0 && skt == null)
                    {
                        System.out.println("Socket creation");
                        skt = new Socket(server.hostName, server.succPortNo);
                        out = new PrintWriter(skt.getOutputStream(), true);
                    }
                    if( server.succPortNo!=0 && out != null)
                    {
                        System.out.println("Sending to other server" + inputLine);
                        out.println(inputLine);
                    }
                    else
                    {
                        System.out.println("Sending client = "+ inputLine);
                        String []retval = inputLine.split(":");
                    
                        buf = retval[0].getBytes();
                        InetAddress address =  InetAddress.getByName(retval[1]);
                        int port = Integer.parseInt(retval[2]);

                        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
                        clientSocket.send(packet);
                    }
                }
                System.out.println("Closing");
                if(skt != null)
                    skt.close();
                sSocket.close();

            } 
            catch (IOException e) 
            {
                System.err.println("Could not listen on port " + server.tcpPortNo);
                System.exit(-1);
            }
        
    }

    /* Intialization Function*/
public int init()
{
        /*
            1. Read Configuration File and get the port number where to run 
            2. Master IP address and port No

        */
        String temp = null;
        Boolean isCorrectBank = false;
        Path path = Paths.get(configFile);
        
        try
        {
           

            myAddress = InetAddress.getLocalHost().getHostAddress();
            hostName = InetAddress.getLocalHost().getHostName();

            System.out.println("myAddress : " + myAddress);
            System.out.println("hostName : " + hostName);

            BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
            String line = null, tcp_temp = "TCP_PORT_NUMBER_", udp_temp = "UDP_PORT_NUMBER_";
            int count = 0;
            int temp_num = 0;
            int temp_succ = 0;
            int temp_pred = 0;

            
            while ((line = reader.readLine()) != null ) {
                if(!line.isEmpty()) 
                {
                    if (count == 7)
                        break;

                    if (line.trim().equals("</Bank1>") || line.trim().equals("</Bank1>") || line.trim().equals("</Bank1>"))
                        isCorrectBank = false;

                    String []val = line.split(":");
                    if (val.length == 2)
                    {
                        if (val[0].trim().equals("MASTER_IP_ADDRESS"))
                        {
                           masterAddress = val[1].trim();
                           count++;
                           continue;
                        }
                        if (val[0].trim().equals("MASTER_TCP_PORTNO"))
                        {
                            masterPortNo = Integer.parseInt(val[1].trim());
                            count++;
                        }

                        if (val[0].trim().equals("LENGTH"))
                        {
                            chainLength = Integer.parseInt(val[1].trim());
                            count++;
                        }
                        
                        if (val[1].trim().equals(bankName))
                        {   
                            isCorrectBank = true;
                            count++;
                        }

                        if (tcpPortNo == 0 && val[1].trim().equals(myAddress))
                        {
                            temp_num = Integer.parseInt(val[0].trim().split("_")[2]);
                        }

                        if (val[0].trim().equals(tcp_temp+temp_num))
                        {
                            
                            tcpPortNo = Integer.parseInt(val[1].trim());
                             try 
                            {
                                ServerSocket serverSocket = new ServerSocket(tcpPortNo);
                                serverSocket.close();

                                temp_succ = ++temp_num;
                                temp_pred = temp_num--;
                            }
                            catch(Exception e)
                            {
                                temp_num ++;
                                tcpPortNo = 0;
                                continue;
                            }
                            count++;
                        }

                        if (tcpPortNo!=0 && val[0].trim().equals(udp_temp+temp_num))
                        {
                                udpPortNo = Integer.parseInt(val[1].trim());
                                count++;
                        }

                        if (tcpPortNo!=0  && val[0].trim().equals(tcp_temp+temp_succ))
                        {
                                succPortNo = Integer.parseInt(val[1].trim());
                                count++;
                        }
   
                    } 
                }    
            }

        }
        catch(Exception e)
        {
            System.out.println("INIT Exception = " + e);
            return 0;
        }
        
    return 1;
}

    public int init_old()
    {
        /*
            1. Read Configuration File and get the port number where to run 
            2. Master IP address and port No

        */
        String temp = null;
        Path path = Paths.get(configFile);
        
        try
        {
            InetAddress temp_ip = InetAddress.getLocalHost();
            String temp_hostname = temp_ip.getHostName();
            System.out.println("Your current IP address : " + temp_ip.getHostAddress());
            System.out.println("Your current Hostname : " + temp_hostname);

            myAddress = InetAddress.getLocalHost().getHostAddress();
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
                            masterPortNo = Integer.parseInt(val[1].trim());
                            count++;
                        }
                        if (val[1].trim().equals(myAddress))
                        {
                            tcp_temp = "TCP_PORT_NUMBER_" + val[0].trim().split("_")[2];
                            udp_temp = "UDP_PORT_NUMBER_" + val[0].trim().split("_")[2];
                        }

                        if (val[0].trim().equals(tcp_temp))
                        {
                            
                            tcpPortNo = Integer.parseInt(val[1].trim());
                             try 
                            {
                                ServerSocket serverSocket = new ServerSocket(tcpPortNo);
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
                                udpPortNo = Integer.parseInt(val[1].trim());
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
            return 0;
        }
        
        return 1;
    }

    public static void validateClientRequest()
    {
            //If the account is not present already create a new account 
            // IF invalid bankname don't update anything
    }

}
