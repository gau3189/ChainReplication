import java.io.*;
import java.net.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.util.*;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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

        private Map<String,Account> accountList;

        private final static Logger LOGGER = Logger.getLogger(CRServer.class.getName());
        private static FileHandler fh;
        public CRServer(String configFile, String bankname)
        {
            this.configFile = configFile;
            this.bankName = bankname;

            this.accountList = new HashMap<String,Account>();                    
        }

        public static void main(String[] args) throws IOException {
         
        if (args.length != 2) {
            System.err.println("Usage: java CRServer <config File> <Bank Name>");
            System.exit(1);
        }
        
        CRServer server = new CRServer(args[0], args[1]);
        
        int result = server.init();

        fh = new FileHandler("./Logs/server@"+ server.tcpPortNo + ".log",true);  
        LOGGER.addHandler(fh);
        LOGGER.setUseParentHandlers(false);
        LOGGER.setLevel(Level.FINE);
        SimpleFormatter formatter = new SimpleFormatter();  
        fh.setFormatter(formatter);  
        LOGGER.info("################################ < Intial Configuration > ###############################");

        LOGGER.config("master addr = "+ server.masterAddress);
        LOGGER.config("master port = "+ server.masterPortNo);
        LOGGER.config("My address = "+ server.myAddress);
        LOGGER.config("successor port = "+ server.succPortNo);
        LOGGER.config("server Count = "+ server.chainLength);  
        LOGGER.config("server udpPortNo  = "+ server.udpPortNo);
        LOGGER.config("server tcpPortNo  = "+ server.tcpPortNo);

        LOGGER.info("################################ </ Intial Configuration >###############################");
        /*
        System.out.println("master addr = "+ server.masterAddress);
        System.out.println("master port = "+ server.masterPortNo);
        System.out.println("My address = "+ server.myAddress);
        System.out.println("successor port = "+ server.succPortNo);
        System.out.println("server Count = "+ server.chainLength);  
        System.out.println("server udpPortNo  = "+ server.udpPortNo);
        System.out.println("server tcpPortNo  = "+ server.tcpPortNo);
        */

        String inputLine, outputLine;
        DatagramSocket clientSocket = null;
        ServerMessage receivedMessage = null;
        RequestReply response = new RequestReply();
        
        /*
            Creating ServerSocket for listening from other servers
        */
            
        try 
        { 
                clientSocket = new DatagramSocket(server.udpPortNo);
                new CRMClientServerThread(clientSocket, server.hostName, server.succPortNo, server.accountList, LOGGER).start();
        } 
        catch (Exception e) 
        {
            System.err.println("Could not listen on port " + server.udpPortNo);
            LOGGER.severe("Could not listen on port " + server.udpPortNo);
            System.exit(-1);
        }

        Socket skt = null;
        ObjectOutputStream out = null;
             
        try (
                ServerSocket serverSocket = new ServerSocket(server.tcpPortNo);
                Socket sSocket = serverSocket.accept();
                PrintWriter temp_out = new PrintWriter(sSocket.getOutputStream(), true);
                BufferedReader temp_in = new BufferedReader(new InputStreamReader(sSocket.getInputStream()));

                ObjectInputStream inStream = new ObjectInputStream(sSocket.getInputStream());
                ObjectOutputStream outStream = new ObjectOutputStream(sSocket.getOutputStream());

            )
            { 
                System.out.println("Created TCP port for lsitening peer server port no="+ server.tcpPortNo);
                LOGGER.fine("Created TCP port for lsitening peer server port no="+ server.tcpPortNo);
                int reply = 0;
                while ((receivedMessage = (ServerMessage) inStream.readObject()) != null)
                {
                    byte[] buf = new byte[256];
                    System.out.println("input Line="+ receivedMessage);
                    
                    LOGGER.fine("received from predecessor ="+ receivedMessage);
                    reply = server.update(receivedMessage);

                    if (reply == 0)
                        System.out.println("failed");

                    if(server.succPortNo!=0 && skt == null)
                    {
                        System.out.println("Socket creation");
                        LOGGER.fine("Socket creation");
                        skt = new Socket(server.hostName, server.succPortNo);
                        out = new ObjectOutputStream(skt.getOutputStream());
                    }
                    if( server.succPortNo!=0 && out != null)
                    {
                        System.out.println("Sending to other server" + receivedMessage);
                        LOGGER.fine("Sending to successor server" + receivedMessage + "at port" + server.succPortNo);
                        out.writeObject(receivedMessage);
                    }
                    else
                    {
                        System.out.println("Sending client = "+ receivedMessage);
                        LOGGER.fine("Sending client = "+ receivedMessage);

                        response.setReqID(receivedMessage.getReqID());
                        response.setBalance(receivedMessage.getBalance());
                        response.setOutcome(receivedMessage.getOutcome());
                        response.setOperation(receivedMessage.getOperation());
                        response.setAccountNumber(receivedMessage.getAccountNumber());

                        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
                        ObjectOutput oo = new ObjectOutputStream(bStream); 
                        oo.writeObject(response);
                        oo.close();

                        buf = bStream.toByteArray();
                        
                        InetAddress address =  InetAddress.getByName(receivedMessage.getHostAddress());
                        int port = receivedMessage.getPortNumber();
                        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
                        clientSocket.send(packet);
                    }
                }

                System.out.println("Closing");
                LOGGER.info("Server Shutting Down");

                if(skt != null)
                    skt.close();
                if(sSocket != null)
                    sSocket.close();

            } 
            catch (Exception e) 
            {
                System.err.println("Could not listen on port " + server.tcpPortNo);
                LOGGER.severe("Exception Occured " + e);
                
                System.exit(-1);
            }

            System.exit(1);
        
    }

    /*
        Function        :  init
        Description     :  1. Read Configuration File and get the port number where to run 
                           2. Master IP address and port No
                           3. Get all server related information such as delay time, server messages etc.

    */
    public int init()
    {   
        
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

                        if (isCorrectBank && val[0].trim().equals("LENGTH"))
                        {
                            chainLength = Integer.parseInt(val[1].trim());
                            count++;
                        }
                        
                        if (val[1].trim().equals(bankName))
                        {   
                            isCorrectBank = true;
                            count++;
                        }

                        if (isCorrectBank && tcpPortNo == 0 && val[1].trim().equals(myAddress))
                        {
                            temp_num = Integer.parseInt(val[0].trim().split("_")[2]);
                        }

                        if (isCorrectBank && val[0].trim().equals(tcp_temp+temp_num))
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

                        if (isCorrectBank && tcpPortNo!=0 && val[0].trim().equals(udp_temp+temp_num))
                        {
                                udpPortNo = Integer.parseInt(val[1].trim());
                                count++;
                        }

                        if (isCorrectBank && tcpPortNo!=0  && val[0].trim().equals(tcp_temp+temp_succ))
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
            LOGGER.severe("INIT Exception = " + e);
            return 0;
        }
        
    return 1;
    }

    /*
        Function        :  update
        Input           :  RequestReply message which contains the request sent by the client
        returnValue     :  0 or 1 denoting success or failure of the update 
    */
    public int update(ServerMessage message)
    {
      //  System.out.println("Update");
        LOGGER.fine("In Update");
        int reply = 0;
        switch(message.operation)
        {
            case "DP":  reply = deposit(message);
                        break;
            case "WD":  reply = withdraw(message);
                        break;
            default :   reply = 0; 
                        break;
        }

        return reply;
    }

    /*
        Function        :  deposit
        Input           :  ServerMessage message which contains the update sent by the predecessor
        returnValue     :  1 for successful update  
    */
    public int deposit(ServerMessage message)
    {
        Account currAccount = null;
        String trans = "DP," + message.getReqID() + "," + message.getAmount();
        String reply = null;
        if (this.accountList.containsKey(message.getAccountNumber())) {

            currAccount = this.accountList.get(message.getAccountNumber());
            
            System.out.println("Account List = "+currAccount);
            currAccount.balance = message.getAmount();
            if (message.getOutcome().equals(Outcome.Processed))
                currAccount.processedTrans.add(trans);

            reply = "<"+ message.getReqID() + "," + message.getOutcome() + currAccount.balance + ">";
        } else {
            currAccount = new Account();
            currAccount.balance = message.getBalance();
            
            currAccount.processedTrans.add(trans);
            this.accountList.put(message.getAccountNumber(),currAccount);
            reply = "<"+ message.getReqID() + "," + "Processed, " + currAccount.balance + ">";
        }

        System.out.println(reply);
        LOGGER.info(reply);
        return 1;
    }

    /*
        Function        :  withdraw
        Input           :  ServerMessage message which contains the update sent by the predecessor
        returnValue     :  1 for successful update  
    */
    public int withdraw(ServerMessage message)
    {
        Account currAccount = null;
        String trans = "DP," + message.getReqID() + "," + message.getAmount();
        String reply = null;
        if (this.accountList.containsKey(message.getAccountNumber())) {

            currAccount = this.accountList.get(message.getAccountNumber());
            
            System.out.println("Account List = "+currAccount.balance);
            currAccount.balance = message.getAmount();
            if (message.getOutcome().equals(Outcome.Processed))
                currAccount.processedTrans.add(trans);

            reply = "<"+ message.getReqID() + "," + message.getOutcome() + currAccount.balance + ">";
            

        } else {
            currAccount = new Account();
            currAccount.balance = message.getBalance();
            
            currAccount.processedTrans.add(trans);
            this.accountList.put(message.getAccountNumber(),currAccount);
            reply = "<"+ message.getReqID() + "," + "Processed, " + currAccount.balance + ">";
        }

        System.out.println(reply);
        LOGGER.info(reply);
        return 1;
    }
}

/*
        ClassName       :  Account
        Variables       :  float balance - for storing the balance
                        :  processedTrans - list containing processed transactions.
          
*/
class Account
{
    float balance;
    List <String> processedTrans;

    Account()
    {
        this.balance = 0;
        this.processedTrans = new ArrayList<String>();
    }
}

