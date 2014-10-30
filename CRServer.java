/*
 *  PHASE 2: NON-FAULT-TOLERANT SERVICE
 *  TEAM MEMBERS: 
 * 
 *  GAUTHAM REDDY KUNTA ID: 109596312
 *  NAFEES AHMED ABDUL ID: 109595182
 *
 *
 *  CRServer.java
 *
 */

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
import java.util.concurrent.ConcurrentHashMap;

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

        private int startupTime;
        private int lifeTime;

        private  String configFile;

        /*
            HashMap of accounts and its correspornding account information(balance and processedTrans) present in the bank
        */
        private Map<String,Account> accountList;

        private final static Logger LOGGER = Logger.getLogger(CRServer.class.getName());
        private static FileHandler fh;
       
        public static void main(String[] args) throws IOException {
         
            if (args.length != 2) {
                System.err.println("Usage: java CRServer <config File> <Bank Name>");
                System.exit(1);
            }
            
            CRServer server = new CRServer(args[0], args[1]);
            
            int result = server.init();

            if (server.tcpPortNo ==0 || server.udpPortNo == 0) {
                System.err.println("server port must be non zero");
                System.exit(1);
            }

            fh = new FileHandler("./Logs/server@"+ server.tcpPortNo + ".log");  
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
             LOGGER.config("predecessor port = "+ server.predPortNo);
            LOGGER.config("server Count = "+ server.chainLength);  
            LOGGER.config("server udpPortNo  = "+ server.udpPortNo);
            LOGGER.config("server tcpPortNo  = "+ server.tcpPortNo);
            LOGGER.config("successor address = "+ server.successor);
             LOGGER.config("predecessor address = "+ server.predecessor);
            LOGGER.config("server startup time  = "+ server.startupTime);
            LOGGER.config("server delay time  = "+ server.lifeTime);
            LOGGER.info("################################ </ Intial Configuration >###############################");
            
            LOGGER.info("Account List at StartUp = "+ server.accountList);
            
            /*
              DEBUG MESSAGES
                System.out.println("master addr = "+ server.masterAddress);
                System.out.println("master port = "+ server.masterPortNo);
                System.out.println("My address = "+ server.myAddress);
                System.out.println("successor port = "+ server.succPortNo);
                System.out.println("server Count = "+ server.chainLength);  
                System.out.println("server udpPortNo  = "+ server.udpPortNo);
                System.out.println("server tcpPortNo  = "+ server.tcpPortNo);
                System.out.println("server startup time  = "+ server.startupTime);
                System.out.println("server delay time  = "+ server.lifeTime);

                System.out.println("successor address = "+ server.successor);
            */        

                System.out.println("my port = "+ server.tcpPortNo);
                System.out.println("successor port = "+ server.succPortNo);
                System.out.println("predecessor port = "+ server.predPortNo);

            String inputLine, outputLine;
            DatagramSocket clientSocket = null;
            ServerMessage receivedMessage = null;
            ServerMessage receivedAMessage = null;
            ServerMessage ackMessage = null;
            RequestReply response = new RequestReply();
            
            /*
                Creating DatagramSocket for listening to clients and spawn CRClientServerThread
            */
                
            try 
            { 
                clientSocket = new DatagramSocket(server.udpPortNo, InetAddress.getByName(server.myAddress));
                new CRClientServerThread(clientSocket, server.successor, server.succPortNo, server.accountList, LOGGER).start();
            } 
            catch (Exception e) 
            {
                System.err.println("Could not listen on port " + server.udpPortNo);
                LOGGER.severe("Could not listen on port " + server.udpPortNo);
                System.exit(-1);
            }

            /*
                Creating ServerSocket for listening to other servers and socket for sending update to its successor.
                ObjectInputStream & ObjectOutputStream to recieve and send ServerMessage object from other server.
            */
            Socket sktSucc = null;
            ObjectOutputStream out = null;
            ObjectInputStream in = null;

            Socket sktPred = null;  
            ObjectOutputStream outPred = null;

                 
            try (
                    ServerSocket serverSocket = new ServerSocket(server.tcpPortNo,-1, InetAddress.getByName(server.myAddress));
                    Socket sSocket = serverSocket.accept();
                   
                    ObjectInputStream inStream = new ObjectInputStream(sSocket.getInputStream());
                    ObjectOutputStream outStream = new ObjectOutputStream(sSocket.getOutputStream());


                )
                { 
                    System.out.println("Created TCP port for lsitening peer server port no = "+ server.tcpPortNo);
                    LOGGER.fine("Created TCP port for lsitening peer server port no = "+ server.tcpPortNo);
                    int reply = 0;
                    
                    if (server.predPortNo ==0)
                    {

                        while ((receivedMessage = (ServerMessage) inStream.readObject()) != null )
                        {
                            System.out.println("received from predecessor = "+ receivedMessage);
                                reply = server.update(receivedMessage);
                                if (reply == 0)
                                    System.out.println("failed");
                        }
                    }
                    else
                    {
                        sSocket.setSoTimeout(2000);
                        while (true)
                        {
                            System.out.println("Waiting for message or got message");
                            byte[] buf = new byte[512];

                            try{
                                receivedMessage = (ServerMessage) inStream.readObject();
                            }
                            catch(Exception e)
                                {
                                    System.out.println(" receivedMessage Time out");

                                }
                             if(in!= null)
                             {
                                 System.out.println("in not null");
                                try{
                                 receivedAMessage = (ServerMessage) in.readObject();
                                }
                                catch(Exception e)
                                {
                                    System.out.println("Time out");

                                }

                             }

                           
                            //Update the accountList states with received updates
                            

                            if(server.succPortNo!=0 && sktSucc == null)
                            {
                                System.out.println("Socket creation");
                                LOGGER.fine("Socket creation");

                                sktSucc = new Socket(InetAddress.getByName(server.successor), server.succPortNo);
                                out = new ObjectOutputStream(sktSucc.getOutputStream());
                                in = new ObjectInputStream(sktSucc.getInputStream());
                                sktSucc.setSoTimeout(2000);
                                LOGGER.fine("Socket creation done");
                            }

                            if(server.predPortNo!=0 && sktPred == null)
                            {
                                System.out.println("Socket creation");
                                LOGGER.fine("Socket creation");
                                sktPred = new Socket(InetAddress.getByName(server.predecessor), server.predPortNo);
                                outPred = new ObjectOutputStream(sktPred.getOutputStream());
                                LOGGER.fine("Socket creation done");
                            }
                            System.out.println("Checking to send update or ack");
                            


                            if (receivedAMessage!= null ) //&& receivedAMessage.getMessage().toUpperCase().equals("ACK")
                            {

                                System.out.println("received ack from succesor");
                                

                                 System.out.println("received from succesor = "+ receivedAMessage);

                                 System.out.println("sending message to predecessor");
                                reply = server.update(receivedAMessage);
                            

                                if (server.predPortNo == 5555)
                                {
                                     System.out.println("outPred");
                                     outPred.writeObject(receivedAMessage);
                                 }
                                 else{
                                     System.out.println("outstream");
                                    outStream.writeObject(receivedAMessage);
                                }

                                receivedAMessage = null;
                            }
                            if (receivedMessage!= null ) 
                            {    
                                System.out.println("received from predecessor = "+ receivedMessage);
                                reply = server.update(receivedMessage);
                                if (reply == 0)
                                    System.out.println("failed");

                                LOGGER.fine("Received from predecessor = "+ receivedMessage);

                                if( server.succPortNo!=0 && out != null)
                                {
                                    System.out.println("Sending to other server   " + receivedMessage);
                                    LOGGER.fine("Sending to successor server =  " + receivedMessage + " at port =   " + server.succPortNo);
                                    out.writeObject(receivedMessage);
                                }
                                else
                                {
                                    /*
                                        Sending reply to client.
                                    */
                                    //Thread.sleep(server.lifeTime);    
                                    System.out.println("Sending Response to client = "+ receivedMessage);
                                   

                                    response.setReqID(receivedMessage.getReqID());
                                    response.setBalance(receivedMessage.getBalance());
                                    response.setOutcome(receivedMessage.getOutcome());
                                    response.setOperation(receivedMessage.getOperation());
                                    response.setAccountNumber(receivedMessage.getAccountNumber());

                                    LOGGER.fine("Sending Response to client = "+ response.showReply());

                                    ByteArrayOutputStream bStream = new ByteArrayOutputStream();
                                    ObjectOutput oo = new ObjectOutputStream(bStream); 
                                    oo.writeObject(response);
                                    oo.close();

                                    buf = bStream.toByteArray();
                                    
                                    InetAddress address =  InetAddress.getByName(receivedMessage.getHostAddress());
                                    int port = receivedMessage.getPortNumber();

                                    DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
                                    clientSocket.send(packet);


                                    
                                    
                                    LOGGER.fine("Sending ack to predecessor");
                                    receivedMessage.setOperation("updateTrans");
                                    receivedMessage.setMessage("ACK");
                                    if(outPred!= null)
                                    {
                                        System.out.println("Sending ack to predecessor");
                                        if (server.predPortNo ==0)
                                        {
                                             System.out.println("outPred");
                                             outPred.writeObject(receivedMessage);
                                         }
                                         else{
                                             System.out.println("outstream");
                                            outStream.writeObject(receivedMessage);
                                        }
                                    }
                                    Thread.sleep(5000);
                                    
                                }
                                receivedMessage = null;
                            }
                            
                        }
                  }

                   

                } 
                catch (Exception e) 
                {
                    System.out.println("Closing");
                    LOGGER.info("Server Shutting Down");

                    System.err.println("Could not listen on port " + server.tcpPortNo);
                    LOGGER.severe("Exception Occured " + e);
                    
                    System.exit(-1);
                }

                System.exit(1);
        }

        public CRServer(String configFile, String bankname)
        {
            this.configFile = configFile;
            this.bankName = bankname;

            this.accountList = new ConcurrentHashMap<String,Account>();  
            //myAddress = InetAddress.getLocalHost().getHostAddress();
              myAddress = "127.0.0.1";                
        }

        /*
            Function        :  init()
            Description     :  1. Read Configuration File to get the tcp and udp port number where to run 
                               2. successor IP address and port No along with master details
                               3. Get all server related information such as startup time, life time etc.

        */
        public int init()
        {   
            LOGGER.fine("In Init ()");
            String temp = null;
            Boolean isCorrectBank = false;
            Path path = Paths.get(configFile);
            
            try
            {
                System.out.println("myAddress : " + myAddress);
                
                BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
                String line = null, tcp_temp = "TCP_PORT_NUMBER_", udp_temp = "UDP_PORT_NUMBER_", succAddr = "HOST_ADDRESS_";
                int count = 0;
                int temp_num = 0;
                int temp_succ = 0;
                int temp_pred = 0;

                
                while ((line = reader.readLine()) != null ) {
                    if(!line.isEmpty()) 
                    {
                        if (count == 12)
                            break;

                        if (line.trim().toLowerCase().contains("</bank"))
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
                                    ServerSocket serverSocket = new ServerSocket(tcpPortNo,-1, InetAddress.getByName(myAddress));
                                    serverSocket.close();


                                    temp_succ = temp_num+1;
                                    temp_pred = temp_num-1;

                                    System.out.println("temp_succ" + temp_num);
                                    System.out.println("temp_succ" + temp_succ);
                                    System.out.println("temp_pred" + temp_pred);
                                     
                                    if (temp_pred == 0)
                                    {
                                            predPortNo = 0;
                                            predecessor = null;
                                            count++;
                                            count++;
                                    }
                                }
                                catch(Exception e)
                                {
                                    temp_pred = temp_num;
                                    temp_num ++;
                                    tcpPortNo = 0;

                                    if (isCorrectBank && temp_pred!=0 && val[0].trim().equals(tcp_temp+temp_pred))
                                    {
                                        predPortNo = Integer.parseInt(val[1].trim());
                                        count++;
                                    }

                                    if (isCorrectBank && temp_pred!=0  && val[0].trim().equals(succAddr+temp_pred))
                                    {
                                        predecessor = val[1].trim();
                                        count++;
                                    }

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

                            if (isCorrectBank && tcpPortNo!=0  && val[0].trim().equals(succAddr+temp_succ))
                            {
                                successor = val[1].trim();
                                count++;
                            }

                            
                            
                        
                            if (isCorrectBank && tcpPortNo!=0  && val[0].trim().equals("STARTUP_TIME"))
                            {
                                startupTime = Integer.parseInt(val[1].trim());
                                count++;
                            }

                            if (isCorrectBank && tcpPortNo!=0 && val[0].trim().equals("LIFE_TIME"))
                            {
                                lifeTime = Integer.parseInt(val[1].trim());
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

        LOGGER.fine("Init() Done");    
            
        return 1;
        }

        /*
            Function        :  update
            Input           :  RequestReply message which contains the request sent by the client
            returnValue     :  0 or 1 denoting success or failure of the update 
        */
        public int update(ServerMessage message)
        {
            System.out.println("Update");
            LOGGER.fine("In Update");
            int reply = 0;
            switch(message.getOperation().toLowerCase())
            {
                case "dp":  reply = deposit(message);
                            break;
                case "wd":  reply = withdraw(message);
                            break;
                case "updatetrans": reply = updateTrans(message);
                                    break;

                default :   reply = 0; 
                            break;
            }
            LOGGER.fine("Update Finished");
            return reply;
        }


        /*
            Function        :  deposit
            Input           :  ServerMessage message which contains the update sent by the predecessor
            returnValue     :  1 for successful update  
        */
        public int updateTrans(ServerMessage message)
        {
            String trans1 = null,trans2 = null;
            Account currAccount = null;
            LOGGER.info("In Update");
            LOGGER.info("message " + message);
            trans1 = "DP," + message.getReqID() + "," + message.getAmount();
            trans2 = "WD," + message.getReqID() + "," + message.getAmount();
            
            

            if (this.accountList.containsKey(message.getAccountNumber())) {

                currAccount = this.accountList.get(message.getAccountNumber());
                currAccount.balance = message.getBalance();
                if (message.getOutcome().equals(Outcome.Processed))
                {
                   
                    
                    if (currAccount.currentTrans.contains(trans1))
                    {

                         LOGGER.info("trans " + trans1);
                         currAccount.processedTrans.add(trans1);
                         currAccount.currentTrans.remove(trans1);
                    }
                    else
                    {

                         LOGGER.info("trans " + trans2);    
                        currAccount.processedTrans.add(trans2);
                        currAccount.currentTrans.remove(trans2);
                    }
                }
                this.accountList.put(message.getAccountNumber(),currAccount);
            } 

            // LOGGER.info("For account Number = " + String.valueOf(message.getAccountNumber()));
            // LOGGER.info("Processed Trans = " + Arrays.toString(currAccount.processedTrans.toArray()));
            // LOGGER.info("CUrrent Trans = " + Arrays.toString(currAccount.currentTrans.toArray()));

            return 1;   
        }

        /*
            Function        :  deposit
            Input           :  ServerMessage message which contains the update sent by the predecessor
            returnValue     :  1 for successful update  
        */
        public int deposit(ServerMessage message)
        {
            System.out.println("In Deposit");
            LOGGER.fine("In Deposit");
            Account currAccount = null;

            //transaction which is added to processedTrans
            String trans = "DP," + message.getReqID() + "," + message.getAmount();

            /*
            Check if given account number is present in the accountList and outcome is processed
            then update the balance, add transaction to processedTrans and create the ServerMessage to pass it to other servers
            else dont add the transaction to processed trans.

             If account number not present create and perform the update and add transaction to processedTrans 
             and create the ServerMessage to pass it to other servers.
            */
           
            if (this.accountList.containsKey(message.getAccountNumber())) {

                currAccount = this.accountList.get(message.getAccountNumber());
                currAccount.balance = message.getBalance();
                
                if (message.getOutcome().equals(Outcome.Processed))
                {
                    //currAccount.processedTrans.add(trans);
                    currAccount.currentTrans.add(trans);
                }
                this.accountList.put(message.getAccountNumber(),currAccount);
            } else {
                currAccount = new Account();
                currAccount.balance = message.getBalance();
                
                //currAccount.processedTrans.add(trans);
                currAccount.currentTrans.add(trans);
                this.accountList.put(message.getAccountNumber(),currAccount);
            }

            LOGGER.info("For account Number = " + String.valueOf(message.getAccountNumber()));
            LOGGER.info("Processed Trans = " + Arrays.toString(currAccount.processedTrans.toArray()));
            LOGGER.info("CUrrent Trans = " + Arrays.toString(currAccount.currentTrans.toArray()));
            return 1;
        }

        /*
            Function        :  withdraw
            Input           :  ServerMessage message which contains the update sent by the predecessor
            returnValue     :  1 for successful update  
        */
        public int withdraw(ServerMessage message)
        {
            System.out.println("In Withdraw");
            Account currAccount = null;
            //transaction which is added to processedTrans
            String trans = "DP," + message.getReqID() + "," + message.getAmount();
            
            /*
            Check if given account number is present in the accountList and outcome is processed
            then update the balance, add transaction to processedTrans and create the ServerMessage to pass it to other servers
            else dont add the transaction to processed trans.

             If account number not present create and perform the update and add transaction to processedTrans 
             and create the ServerMessage to pass it to other servers.
            */

            if (this.accountList.containsKey(message.getAccountNumber())) {

                currAccount = this.accountList.get(message.getAccountNumber());
                currAccount.balance = message.getBalance();
                if (message.getOutcome().equals(Outcome.Processed))
                {
                    //currAccount.processedTrans.add(trans);
                    currAccount.currentTrans.add(trans);
                }
                this.accountList.put(message.getAccountNumber(),currAccount);
            } 
            else {
                currAccount = new Account();
                currAccount.balance = message.getBalance();
                
                //currAccount.processedTrans.add(trans);
                currAccount.currentTrans.add(trans);
                this.accountList.put(message.getAccountNumber(),currAccount);
            }

            LOGGER.info("For account Number = " + String.valueOf(message.getAccountNumber()));
            LOGGER.info("Processed Trans = " + Arrays.toString(currAccount.processedTrans.toArray()));
            LOGGER.info("CUrrent Trans = " + Arrays.toString(currAccount.currentTrans.toArray()));
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
    List <String> currentTrans;

    Account()
    {
        this.balance = 0;
        this.processedTrans = new ArrayList<String>();
        this.currentTrans = new ArrayList<String>();
    }

     public String toString() {
        return "< balance = " + balance + " , processedTrans = "+ processedTrans  +
               " , cuurentTrans = "+ currentTrans + " >";
    }

}

