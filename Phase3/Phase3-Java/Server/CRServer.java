/*
 *  PHASE 3: FAULT-TOLERANT SERVICE
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

import java.sql.Timestamp;
import java.util.Date;

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
        private  Integer succPortNo;
        private  int predPortNo;
        private  int masterPortNo;
        private  int masterUdpPortNo;
        private  int chainLength;

        private int startupTime;
        private int lifeTime;
        private int seqNo;
        private Boolean isExtending;

        private  String configFile;

        /*
            HashMap of accounts and its correspornding account information(balance and processedTrans) present in the bank
        */
        private Map<String,Account> accountList;
        private Map<String,ServerNInfo> serverNeighbors;

        private Map<String,List <ServerMessage>> transactions;

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

            String path = "./Logs";    // path of the folder you want to create
            File folder=new File(path);
            boolean exist=folder.exists();
            if(!exist){
                folder.mkdirs();
            }else{
                System.out.println("folder already exist");
            }
            
            fh = new FileHandler("./Logs/server@"+ server.tcpPortNo + ".log");  
            LOGGER.addHandler(fh);
            LOGGER.setUseParentHandlers(false);
            LOGGER.setLevel(Level.FINE);
            SimpleFormatter formatter = new SimpleFormatter();  
            fh.setFormatter(formatter);  
            
            
            LOGGER.info("Account List at StartUp = "+ server.accountList);
            
            String inputLine, outputLine;
            DatagramSocket clientSocket = null;
            
            ServerMessage receivedMessage = null;
            ServerMessage ackMessage = null;

            RequestReply response = new RequestReply();
            Socket smSocket = null;
            ObjectInputStream ins = null;
            ObjectOutputStream outs = null;
            MasterMessage request = new MasterMessage();
            MasterMessage response1 = null;     
            try{
                
                smSocket = new Socket(InetAddress.getByName(server.masterAddress) , server.masterPortNo);

                request.setHostAddress(server.myAddress); 
                request.setHostPortNo(server.tcpPortNo);
                request.setHostUdpPortNo(server.udpPortNo);
                request.setSender("server");
                request.setMessage("details");
                request.setBankName(server.bankName);

                outs = new ObjectOutputStream(smSocket.getOutputStream());
                outs.writeObject(request);

                ins = new ObjectInputStream(smSocket.getInputStream());  
                response1 = (MasterMessage) ins.readObject();
            
                if(response1.getMessage()!=null && response1.getMessage().equals("extending"))
                    server.isExtending = true;
                
                server.succPortNo   = response1.getSuccPortNo();
                server.predPortNo   = response1.getPredPortNo();
                server.successor    = response1.getSuccAddress();
                server.predecessor  = response1.getPredAddress();

                server.serverNeighbors.put("succ", new ServerNInfo(server.successor,server.succPortNo));
                server.serverNeighbors.put("pred", new ServerNInfo(server.predecessor,server.predPortNo));

                clientSocket = new DatagramSocket(server.udpPortNo, InetAddress.getByName(server.myAddress));
            }
            catch(Exception e)
            {
                System.out.println("Exception while getting succ and pred details from master" + e);
            }
            
            LOGGER.info("################################ < Intial Configuration > ###############################");

            LOGGER.config("master addr = "+ server.masterAddress);
            LOGGER.config("master port = "+ server.masterPortNo);
            LOGGER.config("My address = "+ server.myAddress);
            LOGGER.config("successor port = "+ server.succPortNo);
            LOGGER.config("server Count = "+ server.chainLength);  
            LOGGER.config("server udpPortNo  = "+ server.udpPortNo);
            LOGGER.config("server tcpPortNo  = "+ server.tcpPortNo);
            LOGGER.config("successor address = "+ server.successor);
            LOGGER.config("server startup time  = "+ server.startupTime);
            LOGGER.config("server delay time  = "+ server.lifeTime);
            LOGGER.info("################################ </ Intial Configuration >###############################");
            
            /*
                Creating ServerSocket for listening to other servers and socket for sending update to its successor.
                ObjectInputStream & ObjectOutputStream to recieve and send ServerMessage object from other server.
            */

            System.out.println("In CRServer");    
            System.out.println("master addr = "+ server.masterAddress);
            System.out.println("master port = "+ server.masterPortNo);
            
            try 
            { 
                System.out.println("Strting Thread to communicate with Master ");
                if(!server.isExtending)
                    new CRServerMaster(clientSocket,server.masterAddress,server.masterUdpPortNo,server.myAddress,server.tcpPortNo,server.bankName,LOGGER).start();
            } 
            catch (Exception e) 
            {
                System.err.println("Exception" + e);
                System.exit(-1);
            }
            /*
                Creating DatagramSocket for listening to clients and spawn CRClientServerThread
            */
            
            if (server.predPortNo == 0 || server.succPortNo == 0)    
                try 
                { 
                    if(!server.isExtending)
                        new CRClientServerThread(clientSocket,server.serverNeighbors,server.accountList,
                                                    server.transactions,server.lifeTime, LOGGER).start();
                } 
                catch (Exception e) 
                {
                    System.err.println("Could not listen on port " + server.udpPortNo);
                    LOGGER.severe("Could not listen on port " + server.udpPortNo);
                    System.exit(-1);
                }

            Socket sktSucc = null;
            Socket sSocket = null;
            ServerSocket serverSocket = null;

            ObjectOutputStream out = null;
            ObjectInputStream in = null;
            ObjectInputStream inStream = null;
            ObjectOutputStream outStream = null;

            
            MasterMessage masterData;
            smSocket.setSoTimeout(3000);
            try
            {
                serverSocket = new ServerSocket(server.tcpPortNo,-1, InetAddress.getByName(server.myAddress));
            }
            catch(Exception e){
                System.out.println("Server Socket"+e);
            }
         
            try
            { 
                System.out.println("Waiting for other server request"); 
                if(server.predPortNo > 0)
                {
                    sSocket = serverSocket.accept();
                    System.out.println("Accepted  server request"); 
                    inStream = new ObjectInputStream(sSocket.getInputStream());
                    outStream = new ObjectOutputStream(sSocket.getOutputStream());

                }

                System.out.println("Created TCP port for lsitening peer server port no = "+ server.tcpPortNo);
                LOGGER.fine("Created TCP port for lsitening peer server port no = "+ server.tcpPortNo);
               
                int reply = 0;
                Account currAccount;
               
                if(server.isExtending)
                {
                    System.out.println("server is new and in extending stage");
                    LOGGER.info("CRServer :: In Extending Stage ");
                    while(true)
                    {
                        try
                        {
                            if(sSocket == null)
                            {
                                sSocket = serverSocket.accept();
                                System.out.println("Accepted  server request"); 
                                sSocket.setSoTimeout(3000);
                                inStream = new ObjectInputStream(sSocket.getInputStream());
                                outStream = new ObjectOutputStream(sSocket.getOutputStream());
                            }
                            else
                            {
                                if(outStream == null)
                                    outStream = new ObjectOutputStream(sSocket.getOutputStream());

                                if(inStream == null)
                                    inStream = new ObjectInputStream(sSocket.getInputStream());
            
                                receivedMessage = (ServerMessage) inStream.readObject();
                                if (receivedMessage.getMessage()!=null && receivedMessage.getMessage().equals("updatetrans"))
                                {
                                    LOGGER.info("CRServer :: Update History accountNumber " +receivedMessage.getAccountNumber() +
                                                "processed Trans "+receivedMessage.getProcessedTrans());

                                    if(!server.accountList.containsKey(receivedMessage.getAccountNumber())) {
                                        currAccount = new Account();
                                        System.out.println("UpdateTrans");
                                        currAccount.balance = receivedMessage.getBalance();
                                        currAccount.processedTrans = new ArrayList<String>(receivedMessage.getProcessedTrans());

                                        server.accountList.put(receivedMessage.getAccountNumber(),currAccount); 
                                    }
                                    server.seqNo = 0;
                                    receivedMessage = null;
                                }
                                else if (receivedMessage.getMessage()!=null && receivedMessage.getMessage().equals("sync"))
                                {
                                    System.out.println("receivedMessage"+receivedMessage);
                                    System.out.println("Sync");
                                    receivedMessage.setHostAddress(server.myAddress); 
                                    receivedMessage.setPortNumber(server.tcpPortNo);;
                                    receivedMessage.setMessage("sync");
                                    outStream.writeObject(receivedMessage); 
                                    System.out.println("sent sync to old tail" + outStream);
                                    receivedMessage = null;
                                }
                                else if (receivedMessage.getMessage()!=null && receivedMessage.getMessage().equals("updatesync"))
                                {
                                    LOGGER.info("CRServer :: Updating Sync Messages");
                                 
                                    reply = server.update(receivedMessage);    
                                    server.seqNo = receivedMessage.getSequenceNumber();

                                    if (server.seqNo >= server.lifeTime)
                                        System.exit(1);

                                    receivedMessage = null;
                                    Thread.sleep(3000);
                                }
                                else if (receivedMessage.getMessage()!=null && receivedMessage.getMessage().equals("done"))
                                {
                                    System.out.println("receivedMessage"+receivedMessage.getMessage());
                                    LOGGER.info("CRServer :: Done with updation and sync ");
                                    request = new MasterMessage();
                                    request.setHostAddress(server.myAddress); 
                                    request.setHostPortNo(server.tcpPortNo);

                                    request.setHostUdpPortNo(server.udpPortNo);
                                    request.setSender("server");
                                    request.setMessage("done");
                                    request.setBankName(server.bankName);

                                    outs.writeObject(request);
                                    LOGGER.info("CRServer :: Sending DONE message to master");
                                    System.out.println("sent message to maaster");
                                    new CRServerMaster(clientSocket,server.masterAddress,server.masterUdpPortNo,
                                                        server.myAddress,server.tcpPortNo,server.bankName,LOGGER).start();

                                    new CRClientServerThread(clientSocket,server.serverNeighbors,server.accountList,
                                                            server.transactions,server.lifeTime, LOGGER).start();
                                    server.isExtending = false;
                                    break;
                                }
                            }
                        }
                        // catch(EOFException e) {
                        //     LOGGER.severe("EOFException "+e);
                           
                        // }
                        catch(IOException e) {
                            if (e instanceof SocketTimeoutException){}
                            else
                            {  
                                LOGGER.severe("IOException "+e);
                                Thread.sleep(3000);
                                server.accountList.clear();
                                request = new MasterMessage();
                                request.setHostAddress(server.myAddress); 
                                request.setHostPortNo(server.tcpPortNo);
                                request.setHostUdpPortNo(server.udpPortNo);
                                request.setSender("server");
                                request.setMessage("retry");
                                request.setBankName(server.bankName);

                                System.out.println("request = "+request.readMessage());
                                System.out.println("outstream = "+outs);
                                outs.writeObject(request);
                                if (sSocket != null)
                                    sSocket.close();

                                sSocket = null;
                                inStream = null;
                                outStream = null;
                            }
                            
                        }
                        catch(Exception e)
                        {
                            LOGGER.severe("Extending chain "+e);
                            if(e!=null)
                                System.out.println(e.getMessage());
                        }
                    }
                }
                if(sSocket!=null)
                    sSocket.setSoTimeout(3000);
                    
                while (true)
                {
                        System.out.println("Waiting for message or got message");
                        if (server.lifeTime <= server.seqNo)
                            break;

                        if (server.predPortNo > 0)
                        { 
                            try
                            {   
                               if(sSocket == null){
                                            LOGGER.info("Accepting new predecessor connection");
                                            System.out.println("Accepting new predecessor connection");
                                            sSocket = serverSocket.accept();

                                            inStream =  new ObjectInputStream(sSocket.getInputStream());
                                            outStream = new ObjectOutputStream(sSocket.getOutputStream());
                                            sSocket.setSoTimeout(3000);
                                            System.out.println("Accepted new predecessor connection");
                                            LOGGER.info("Accepted new predecessor connection");
                                    }
                                    else
                                    {
                                        try
                                        {
                                            System.out.println("Server Message succ = "+ server.succPortNo);
                                            System.out.println("Server Message pred = "+ server.predPortNo);

                                            LOGGER.info("Server Message succ = "+ server.succPortNo);
                                            LOGGER.info("Server Message pred = "+ server.predPortNo);

                                            if (inStream == null )
                                                inStream =  new ObjectInputStream(sSocket.getInputStream());

                                            if (outStream == null )
                                                outStream = new ObjectOutputStream(sSocket.getOutputStream());
                                           
                                            receivedMessage = (ServerMessage) inStream.readObject();
                                            System.out.println("receivedMessage"+receivedMessage);

                                            if(server.succPortNo!=0 && sktSucc == null)
                                            {
                                                System.out.println("Successor Socket creation");
                                                LOGGER.fine("Successor Socket creation");

                                                sktSucc = new Socket(InetAddress.getByName(server.successor), server.succPortNo);
                                                out = new ObjectOutputStream(sktSucc.getOutputStream());
                                                
                                                sktSucc.setSoTimeout(2000);
                                                System.out.println(" Successor Socket creation done");
                                                LOGGER.fine("Successor Socket creation done");
                                            }
                                            if (receivedMessage!= null ) 
                                            {   
                                                System.out.println("received from predecessor = "+ receivedMessage);
                                                System.out.println("message"+ receivedMessage.getMessage());

                                                reply = server.update(receivedMessage);
                                                if (reply == 0)
                                                    System.out.println("failed");

                                                LOGGER.fine("Received from predecessor = "+ receivedMessage);

                                                if( server.succPortNo!=0 && out != null)
                                                {
                                                    System.out.println("Sending to other server   " + receivedMessage);
                                                    LOGGER.fine("Sending to successor server =  " + receivedMessage + " at port =   " + server.succPortNo);
                                                    LOGGER.fine("Messages to sync "+server.transactions.get("updateTrans").size());

                                                    System.out.println("Messages to sync "+server.transactions.get("updateTrans").size());

                                                    for(ServerMessage mesg : server.transactions.get("updateTrans"))
                                                    {
                                                            if (server.lifeTime <= server.seqNo)
                                                                     break;

                                                            mesg.setMessage("sync");
                                                            LOGGER.fine("CRClientServer :: Sending Sync Mesg :: " + mesg);
                                                            out.writeObject(mesg);
                                                    }        

                                                    server.transactions.get("updateTrans").clear(); 
                                                    server.seqNo = receivedMessage.getSequenceNumber();
                                                    server.transactions.get("sentTrans").add(receivedMessage);

                                                    if (server.lifeTime <= server.seqNo)
                                                                     break;

                                                    out.writeObject(receivedMessage);
                                                }
                                                else
                                                {
                                                    /*
                                                        Sending reply to client.
                                                    */
                                                        byte[] buf = new byte[512];

                                                    //Thread.sleep(server.lifeTime); 
                                                    System.out.println("Sending Response to client = "+ receivedMessage);
                                                   
                                                    java.util.Date date= new java.util.Date();
                                                    System.out.println(new Timestamp(date.getTime()));

                                                    response.setTimeStamp(new Timestamp(date.getTime()));
                                                    response.setReqID(receivedMessage.getReqID());
                                                    response.setBalance(receivedMessage.getBalance());
                                                    response.setOutcome(receivedMessage.getOutcome());
                                                    response.setOperation(receivedMessage.getOperation());
                                                    response.setAccountNumber(receivedMessage.getAccountNumber());
                                                    response.setSender("server");
                                                    server.seqNo = receivedMessage.getSequenceNumber();
                                                    if (server.isExtending)
                                                        server.transactions.get("sentTrans").add(receivedMessage);
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
                                                    receivedMessage.setOperation("ack");
                                                    if(outStream != null)
                                                    {
                                                        System.out.println("Sending ack to predecessor");
                                                        System.out.println("Sending Ack for message = "+ receivedMessage);
                                                        System.out.println("Ack SeqNo = " + receivedMessage.getSequenceNumber());
                                                        outStream.writeObject(receivedMessage);
                                                    }
                                                    else
                                                        outStream = new ObjectOutputStream(sSocket.getOutputStream());

                                                }
                                                receivedMessage = null;
                                            }
                                        }
                                        catch(Exception e){
                                            LOGGER.severe("Waiting For Exception"+e);
                                        }

                                        if (server.lifeTime <= server.seqNo)
                                                    break;
                                        try
                                        {
                                            System.out.println("checking in and sktSucc");
                                            if (in == null && sktSucc!=null && server.succPortNo!=0)
                                                in = new ObjectInputStream(sktSucc.getInputStream());
                                            
                                            if (server.isExtending)
                                            {   
                                                LOGGER.info("server in Extending Stage :: sync mess");
                                                if (sktSucc!=null && in ==null)      
                                                { 
                                                    in = new ObjectInputStream(sktSucc.getInputStream());
                                                    sktSucc.setSoTimeout(2000);
                                                }

                                                ackMessage = (ServerMessage) in.readObject();
                                                 
                                                LOGGER.info("server ------> Message"+ ackMessage.getMessage());
                                                if (ackMessage.getMessage()!=null && ackMessage.getMessage().equals("sync") )
                                                {
                                                    for( ServerMessage mesg : server.transactions.get("sentTrans"))
                                                    {
                                                            mesg.setMessage("updatesync");
                                                            mesg.setSequenceNumber(server.seqNo);
                                                            System.out.println("Sending to new tail:updatesync message"+mesg);
                                                            out.writeObject(mesg);
                                                    }   
                                                    ServerMessage mesg =new ServerMessage();
                                                    mesg.setMessage("done");
                                                    out.writeObject(mesg);
                                                    LOGGER.info("Sending to new tail:done message"+mesg);
                                                    server.isExtending = false;
                                                }   

                                                server.succPortNo = ackMessage.getPortNumber();
                                                server.successor = ackMessage.getHostAddress(); 
                                                server.serverNeighbors.get("succ").portNo = ackMessage.getPortNumber();
                                                server.serverNeighbors.get("succ").addr = ackMessage.getHostAddress();    
                                            }   
                                            
                                            else if(server.succPortNo!=0)
                                            { 
                                                System.out.println("Waiting for in message");
                                                ackMessage = (ServerMessage) in.readObject();
                                                LOGGER.info("received from succesor = "+ ackMessage);
                                                System.out.println("received from succesor = "+ ackMessage);
                                                System.out.println("sending message to predecessor");
                                                    
                                                reply = server.update(ackMessage);
                                                outStream.writeObject(ackMessage);
                                                ackMessage = null;
                                            }
                                        }
                                        catch(Exception e){
                                            System.out.println("checking in and sktSucc"+e);
                                            System.out.println("message = "+e.getMessage());
                                            if (server.isExtending && e.getMessage().contains("connection reset"))
                                                in = new ObjectInputStream(sktSucc.getInputStream());
                                        }
                                    }

                            }
                            catch(Exception e){
                                LOGGER.severe("New Connection Accept Exception"+e);
                            }
                        }    
                        try
                        {
                            LOGGER.info("CRServer :: Sent trans " + server.transactions.get("sentTrans"));
                            if(ins!=null)
                            {
                                System.out.println("In ins");
                                LOGGER.info("Checking for messages from master");
                                masterData = (MasterMessage) ins.readObject();
                                System.out.println("From Master Failure Notice"+ masterData.readMessage());
                           
                                if (masterData.getMessage()!= null && masterData.getMessage().toLowerCase().equals("updatesucc"))
                                {
                                    LOGGER.info("Updating Successor");
                                    System.out.println("Updating Successor");
                                    System.out.println("server.seqNo = " + masterData.getSequenceNumber());

                                    LOGGER.info("Updating Successor" +
                                                "Address : " + masterData.getSuccAddress() +
                                                "Port No : " + masterData.getSuccPortNo() +
                                                "Sequence Number " + masterData.getSequenceNumber());


                                    server.successor = masterData.getSuccAddress();
                                    server.succPortNo = masterData.getSuccPortNo();
                                    
                                    
                                    //Send the senttrans to successor all the transactions aftet the given sequence no
                                    
                                    for (ServerMessage item : server.transactions.get("sentTrans"))
                                            if(item.getSequenceNumber() > masterData.getSequenceNumber())
                                                server.transactions.get("updateTrans").add(item);
                                    
                                    LOGGER.info("Messages to sync "+server.transactions.get("updateTrans").size());
                                    System.out.println("Messages to sync "+server.transactions.get("updateTrans").size());

                                    server.serverNeighbors.get("succ").portNo = masterData.getSuccPortNo();
                                    server.serverNeighbors.get("succ").addr = masterData.getSuccAddress();

                                    if(sktSucc != null)
                                        sktSucc.close();
                                    
                                    sktSucc = null;
                                    out = null;
                                    in = null;

                                    if (server.succPortNo == 0 && server.predPortNo > 0)
                                    {
                                        new CRClientServerThread(clientSocket,server.serverNeighbors,server.accountList,
                                                                    server.transactions,server.lifeTime,LOGGER).start();
                                    }
                                }
                                if (masterData.getMessage()!= null && masterData.getMessage().toLowerCase().equals("updatepred"))
                                {
                                    masterData.setSequenceNumber(server.seqNo);
                                    System.out.println("server.seqNo = " + server.seqNo);
                                    outs.writeObject(masterData);


                                    System.out.println("Updating Predecessor");
                                    LOGGER.info("Updating Predecessor" +
                                                "Address : "+ masterData.getPredAddress() +
                                                "Port No : " + masterData.getPredPortNo());
                                    server.predecessor = masterData.getPredAddress();
                                    server.predPortNo = masterData.getPredPortNo();
                                    server.serverNeighbors.get("pred").portNo = masterData.getPredPortNo();
                                    server.serverNeighbors.get("pred").addr = masterData.getPredAddress();
                                    if(server.predPortNo == 0 && server.succPortNo > 0)
                                    {
                                        new CRClientServerThread(clientSocket,sktSucc,in,out,server.serverNeighbors,server.accountList,
                                                server.transactions,server.lifeTime,server.seqNo, LOGGER).start();
                                    }
                                    if(sSocket!=null )
                                         sSocket.close();
                                        
                                    sSocket = null;
                                    inStream = null;
                                    outStream = null;
                                 
                                }
                                if (masterData.getMessage()!= null && masterData.getMessage().toLowerCase().equals("newtail"))
                                {
                                    LOGGER.info("Sending transactions to new tail" + 
                                                " Port Number " + masterData.getHostPortNo());
                                    System.out.println("sending to new tail");
                                    System.out.println("server.seqNo = " + masterData.getSequenceNumber());
                                    System.out.println("masterData.getHostPortNo = " + masterData.getHostPortNo());
                                    //Send the senttrans to successor all the transactions aftet the given sequence no
                                    while(true)
                                    {   
                                        try
                                        {
                                            sktSucc = new Socket(InetAddress.getByName(masterData.getHostAddress()), masterData.getHostPortNo());
                                            out = new ObjectOutputStream(sktSucc.getOutputStream()); 
                                            in = null;
                                            break;
                                        }
                                        catch(Exception e){}
                                    }
                                    server.isExtending = true;
                                    System.out.println("creating socket done " +sktSucc);
                                    LOGGER.info("creating new socket for new tail" +sktSucc);
                                    new CRServerMaster(out,server.accountList,
                                                        server.isExtending,server.bankName,LOGGER).start();

                                }

                                if (masterData.getMessage()!= null && masterData.getMessage().toLowerCase().equals("reset"))
                                {
                                    LOGGER.info("Reset Successor :: External server failed during extension");
                                    System.out.println("Reset Successor");
                                    server.successor = masterData.getSuccAddress();
                                    server.succPortNo = masterData.getSuccPortNo();
                                    server.serverNeighbors.get("succ").portNo = masterData.getSuccPortNo();
                                    server.serverNeighbors.get("succ").addr = masterData.getSuccAddress();
                                    
                                    //Send the senttrans to successor all the transactions aftet the given sequence no

                                    server.transactions.get("sentTrans").clear();

                                    sktSucc.close();
                                    sktSucc = null;
                                    out = null;
                                    in =null;
                                    server.isExtending = false;
                                }

                            }
                            LOGGER.info("Updating Predecessor and succesoor successful");    
                            System.out.println("Updating Predecessor and succesoor successful");
                        }
                        catch(Exception e){
                            LOGGER.severe("Master Message Exception"+e);
                            System.out.println("Master Message "+e);
                        }
                    }
                    
            } 
            catch (Exception e) 
            {
                System.out.println("Closing");
                LOGGER.info("Server Shutting Down");

                System.err.println("Could not listen on port " + server.tcpPortNo);
                LOGGER.severe("Exception Occured " + e);
                
                if(sSocket!=null)
                    sSocket.close();

                if(sktSucc!= null)
                    sktSucc.close();

                if(serverSocket!=null)
                    serverSocket.close();

                if(smSocket!=null)
                    smSocket.close();
                
                System.exit(-1);
            }

            try
            {
            System.out.println("Closing All Sockets");
            if(sSocket!=null)
                sSocket.close();

            if(sktSucc!= null)
                sktSucc.close();
            
            if(serverSocket!=null)
                serverSocket.close();

            if(smSocket!=null)
                smSocket.close();
            }
            catch(Exception e){}
            System.exit(1);
        }

        public CRServer(String configFile, String bankname)
        {
            this.configFile = configFile;
            this.bankName = bankname;

            this.accountList = new ConcurrentHashMap<String,Account>(); 
            this.serverNeighbors = new ConcurrentHashMap<String,ServerNInfo>(); 
            this.transactions = new ConcurrentHashMap<String, List<ServerMessage>>();

            transactions.put("sentTrans", new ArrayList<ServerMessage>());
            transactions.put("updateTrans", new ArrayList<ServerMessage>());

            this.isExtending = false;
            //myAddress = InetAddress.getLocalHost().getHostAddress();
            myAddress = "127.0.0.1";                
        }

        /*
            Function        :  ackSentTrans
            Input           :  ServerMessage message which contains the update sent by the predecessor
            returnValue     :  1 for successful update  
        */
        public int ackSentTrans(ServerMessage message)
        {
            
            LOGGER.info("In ackSentTrans");
            LOGGER.info("SentTrans Before = " + transactions.get("sentTrans").size());
            System.out.println("SentTrans Before = " + transactions.get("sentTrans").size());
            System.out.println("SeqNo = " + message.getSequenceNumber());

            List<ServerMessage> tempMessage = new ArrayList<ServerMessage>();
            for (ServerMessage item : transactions.get("sentTrans"))
            {
                //System.out.println("Sent--value =   "+ item);
                System.out.println("seq no ="+item.getSequenceNumber()); 
                if(item.getSequenceNumber() > message.getSequenceNumber())
                     tempMessage.add(item);
            }
            transactions.put("sentTrans", tempMessage);
            LOGGER.info("SentTrans After = " + transactions.get("sentTrans").size());
            System.out.println("SentTrans After = " + transactions.get("sentTrans").size());   
            return 1;
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
                        if (count == 11)
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
                            if (val[0].trim().equals("MASTER_UDP_PORTNO"))
                            {
                                masterUdpPortNo = Integer.parseInt(val[1].trim());
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
          //  System.out.println("Update");
            LOGGER.fine("In Update");
            int reply = 0;
            switch(message.getOperation().toLowerCase())
            {
                case "dp":  reply = deposit(message);
                            break;
                case "wd":  reply = withdraw(message);
                            break;
                case "ack": reply = ackSentTrans(message);
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
        public int deposit(ServerMessage message)
        {
            System.out.println("In Deposit");
            LOGGER.fine("In Deposit");
            Account currAccount = null;

            //transaction which is added to processedTrans
            String trans = "DP," + message.getReqID() + "," + message.getAmount();
            String accountNumber = message.getAccountNumber();


            /*
            Check if given account number is present in the accountList and outcome is processed
            then update the balance, add transaction to processedTrans and create the ServerMessage to pass it to other servers
            else dont add the transaction to processed trans.

             If account number not present create and perform the update and add transaction to processedTrans 
             and create the ServerMessage to pass it to other servers.
            */
           
            if (this.accountList.containsKey(accountNumber)) {

                currAccount = this.accountList.get(accountNumber);
                currAccount.balance = message.getBalance();
                
                if (message.getOutcome().equals(Outcome.Processed))
                {
                    if (!currAccount.processedTrans.contains(trans))
                        currAccount.processedTrans.add(trans);
                }
                this.accountList.put(message.getAccountNumber(),currAccount);
            } else {
                currAccount = new Account();
                currAccount.balance = message.getBalance();
                
                currAccount.processedTrans.add(trans);
                this.accountList.put(accountNumber,currAccount);
            }

            LOGGER.info("For account Number = " + String.valueOf(accountNumber));
            LOGGER.info("Processed Trans = " + Arrays.toString(currAccount.processedTrans.toArray()));
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
            String trans = "WD," + message.getReqID() + "," + message.getAmount();
            String accountNumber = message.getAccountNumber();

            /*
            Check if given account number is present in the accountList and outcome is processed
            then update the balance, add transaction to processedTrans and create the ServerMessage to pass it to other servers
            else dont add the transaction to processed trans.

             If account number not present create and perform the update and add transaction to processedTrans 
             and create the ServerMessage to pass it to other servers.
            */

            if (this.accountList.containsKey(accountNumber)) {

                currAccount = this.accountList.get(accountNumber);
                currAccount.balance = message.getBalance();
                if (message.getOutcome().equals(Outcome.Processed))
                {
                    if (!currAccount.processedTrans.contains(trans))
                        currAccount.processedTrans.add(trans);
                }
                this.accountList.put(accountNumber,currAccount);
            } 
            else {
                currAccount = new Account();
                currAccount.balance = message.getBalance();
                
                currAccount.processedTrans.add(trans);
                this.accountList.put(accountNumber,currAccount);
            }

            LOGGER.info("For account Number = " + String.valueOf(accountNumber));
            LOGGER.info("Processed Trans = " + Arrays.toString(currAccount.processedTrans.toArray()));
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

     public String toString() {
        return "< balance = " + balance + " , processedTrans = "+ processedTrans +" >";
    }

}

/*
        ClassName       :  ServerNInfo
        Variables       :  addr - address
                        :  portNo - portNo.
          
*/
class ServerNInfo 
{
    String addr;
    int portNo;

    ServerNInfo(String addr,int portno)
    {
        this.addr = addr;
        this.portNo = portno;
    }

     public String toString() {
        return "< address = " + addr + " , portNo = "+ portNo +" >";
    }

}

