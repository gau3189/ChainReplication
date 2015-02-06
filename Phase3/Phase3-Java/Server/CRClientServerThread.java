    /*
     *  PHASE 3: FAULT-TOLERANT SERVICE
     *  TEAM MEMBERS: 
     * 
     *  GAUTHAM REDDY KUNTA ID: 109596312
     *  NAFEES AHMED ABDUL ID: 109595182
     *
     *
     *  CRClientServerThread.java
     *
     */

    import java.net.*;
    import java.io.*;
    import java.util.*;

    import java.util.logging.Logger;
    import java.util.logging.Level;

    import java.sql.Timestamp;
    import java.util.Date;
     
    public class CRClientServerThread extends Thread {
        
        private DatagramSocket socket = null;
        private Socket sSocket = null;
        private int succPortNo;
        private int lifeTime;
        private String successor;
        private Map<String,Account> accountList;
        private Map<String,ServerNInfo> serverNeighbors;
        private Map<String,List <ServerMessage>> transactions;

        private final Logger LOGGER ;
        private int seqNo;

        private ObjectInputStream in;
        private ObjectOutputStream out;

        public CRClientServerThread( DatagramSocket socket, Map<String,ServerNInfo> serverInfo, 
                                    Map<String,Account> accountList,Map<String,List <ServerMessage>> transactions ,int lifetime,Logger LOGGER) 
        {
            super("CRClientServerThread");
            System.out.println("in CRClientServerThread");
            this.socket = socket;
            this.succPortNo = serverInfo.get("succ").portNo;
            this.successor = serverInfo.get("succ").addr; 
            this.accountList = accountList;
            this.serverNeighbors = serverInfo;
            this.in = null;
            this.out = null;
            this.seqNo = 0;
            this.lifeTime = lifetime;
            this.transactions = transactions;
            this.LOGGER = LOGGER;
        }

        public CRClientServerThread(DatagramSocket socket,Socket sSocket,ObjectInputStream ins, ObjectOutputStream outs,  
                    Map<String,ServerNInfo> serverInfo, Map<String,Account> accountList, 
                    Map<String,List <ServerMessage>> transactions,int lifetime,int seqNo,Logger LOGGER) 
        {
            super("CRClientServerThread");
            System.out.println("in CRClientServerThread");
            this.socket = socket;
            this.sSocket = sSocket;
            this.succPortNo = serverInfo.get("succ").portNo;
            this.successor = serverInfo.get("succ").addr; 
            this.accountList = accountList;
            this.serverNeighbors = serverInfo;
            this.in = ins;
            this.out = outs;
            this.seqNo = seqNo;
            this.lifeTime = lifetime;
            this.transactions = transactions;
            this.LOGGER = LOGGER;
        }
         


        /*
            Function        :  run
            Description     :  it is called when the thread is created for this class
        */ 
        public void run() {
     
            
            RequestReply response = new RequestReply();
            ServerMessage receivedMessage = new ServerMessage();
            
            MasterMessage masterData ;
            LOGGER.info("IN CRClientServerThread WAITING FOR CLIENT");
            System.out.println("IN CRClientServerThread WAITING FOR CLIENT");
             
            try
            { 
            socket.setSoTimeout(2000); 
            }catch(Exception e){}  
            while (true)
            {

                if (lifeTime <= seqNo)
                            break;
                try  
                {
                    byte[] buf = new byte[700];  
    
                    /* receive request */
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    System.out.println("Waiting for client request");
                    socket.receive(packet);

                    int byteCount = packet.getLength();
                    
                    ByteArrayInputStream byteStream = new ByteArrayInputStream(packet.getData());
                    ObjectInputStream is = new ObjectInputStream(new BufferedInputStream(byteStream));
                
                    RequestReply clientMessage = (RequestReply) is.readObject();
                    is.close();

                    LOGGER.info("Recieved Request from client = " + clientMessage.showRequest());

                    receivedMessage = processRequest(clientMessage);
                    receivedMessage.setPortNumber(packet.getPort());
                    receivedMessage.setHostAddress(packet.getAddress().getHostAddress());

                    if(!(receivedMessage.getOperation().toLowerCase().equals("gb")) &&  serverNeighbors.get("succ").portNo!=0 )
                    {
                        LOGGER.info("Comparing Socket creation"+
                                    " current succ PortNo " + serverNeighbors.get("succ").portNo+
                                    " old succ PortNo " + succPortNo);

                        System.out.println("Comparing Socket creation");
                        System.out.println("serverNeighbors portNo = "+serverNeighbors.get("succ").portNo);

                        System.out.println("succPortNo= "+succPortNo);
                        if(succPortNo != serverNeighbors.get("succ").portNo)
                        {

                            LOGGER.info("Successor updated creating new socket");
                            System.out.println("serverNeighbors addr = "+serverNeighbors.get("succ").addr);
                            sSocket = new Socket(InetAddress.getByName(serverNeighbors.get("succ").addr), serverNeighbors.get("succ").portNo);
                            sSocket.setSoTimeout(3000);
                            out = new ObjectOutputStream(sSocket.getOutputStream());
                            in = new ObjectInputStream(sSocket.getInputStream());
                            System.out.println(out); 
                            seqNo++;
                            receivedMessage.setSequenceNumber(seqNo);
                            out.writeObject(receivedMessage);
                            succPortNo = serverNeighbors.get("succ").portNo;
                            System.out.println("serverNeighbors done");

                        }   
                        else
                        { 
                           System.out.println("In else");
                           if(succPortNo!=0 && sSocket == null)
                            {
                                System.out.println("CRClientServerThread Socket creation");
                                LOGGER.info("CRClientServer :: CRClientServerThread Socket creation");
                                
                                this.sSocket = new Socket(InetAddress.getByName(successor), succPortNo);
                                out = new ObjectOutputStream(sSocket.getOutputStream());
                                sSocket.setSoTimeout(3000); 
                                in = new ObjectInputStream(sSocket.getInputStream()); 
                                System.out.println(out);
                                
                            }
                            if( succPortNo!=0 && out != null)
                            {
                                System.out.println("Sending Update to other server = " + receivedMessage);
                                LOGGER.info("CRClientServer :: Sending Update to other server = " + receivedMessage);

                                LOGGER.fine("CRClientServer :: Messages to sync :: " +transactions.get("updateTrans").size());
                                for(ServerMessage mesg : transactions.get("updateTrans"))
                                {
                                        mesg.setMessage("sync");
                                        LOGGER.fine("CRClientServer :: Sending Sync Mesg :: " + mesg);
                                        out.writeObject(mesg);
                                }        
                                transactions.get("updateTrans").clear(); 
                                LOGGER.fine("CRClientServer :: Messages to sync Done:: ");

                                seqNo++;
                                System.out.println("SentTrans = " +  seqNo);
                                receivedMessage.setSequenceNumber(seqNo);
                                transactions.get("sentTrans").add(receivedMessage);
                                System.out.println("SentTrans Size = " + transactions.get("sentTrans").size());
                                out.writeObject(receivedMessage);
                            }
                        }
                    }
                    else
                    {
                        java.util.Date date= new java.util.Date();
                        System.out.println(new Timestamp(date.getTime()));
                        System.out.println("Sending Response to client = "+ receivedMessage);
                        response.setTimeStamp(new Timestamp(date.getTime()));
                        response.setReqID(receivedMessage.getReqID());
                        response.setBalance(receivedMessage.getBalance());
                        response.setOutcome(receivedMessage.getOutcome());
                        response.setOperation(receivedMessage.getOperation());
                        response.setAccountNumber(receivedMessage.getAccountNumber());
                        response.setSender("server");
                        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
                        ObjectOutput oo = new ObjectOutputStream(bStream); 
                        oo.writeObject(response);
                        oo.close();

                        buf = bStream.toByteArray();

                        InetAddress address = packet.getAddress();
                        int port = packet.getPort();
                        LOGGER.info("Sending Response to client = " + receivedMessage);
                        packet = new DatagramPacket(buf, buf.length, address, port);
                        socket.send(packet);
                       
                    }
                }
                catch (Exception e) {
                    LOGGER.severe("CRClientServer :: Exception"+e);
                        //System.out.println("exception"+e);
                        //e.printStackTrace();
                        //socket.close();
                        //System.exit(1);
                }   

                try {
                        //System.out.println("checking in ");
                        if(in!=null)
                        {
                            receivedMessage = (ServerMessage) in.readObject();

                            System.out.println("got  ack message ");
                            LOGGER.info("got ack message ");
                            if (receivedMessage!= null )
                            {
                                System.out.println("received ack from succesor for " + receivedMessage);
                                System.out.println("Ack SeqNo = " + receivedMessage.getSequenceNumber());
                                ackSentTrans(receivedMessage);
                                System.out.println("At Head Updated Ack");
                                receivedMessage = null;
                            }
                        }
                }
                catch(Exception e){

                    LOGGER.severe("CRClientServer :: Exception"+e);
                    //System.out.println(e);
                }
            }
            try
            {
                if(sSocket!=null)
                        sSocket.close();
                if(socket!=null)
                    socket.close();
            }
            catch(Exception e){}
            System.exit(1);
            
        }

        /*
            Function        :  processRequest
            Input           :  RequestReply message which contains the request sent by the client
            returnValue     :  ServerMessage object is returned for transferring the update to next successor processor  
        */
        public ServerMessage processRequest(RequestReply message)
        {
            LOGGER.info("Process Request");
            System.out.println("Process Request");
            ServerMessage receivedMessage = new ServerMessage();
            
            switch(message.getOperation().toLowerCase())
            {
                case "gb":  receivedMessage = getBalance(message);
                            break;
                case "dp":  receivedMessage = deposit(message);
                            break;
                case "wd":  receivedMessage = withdraw(message);
                            break;

                default:    break;
            }
            return receivedMessage;
        }


        public void ackSentTrans(ServerMessage message)
        {
            
            LOGGER.info("In ackSentTrans");
            LOGGER.info("message " + message);
            LOGGER.info("SentTrans Before = " + transactions.get("sentTrans"));
            System.out.println("SentTrans Before = " + transactions.get("sentTrans").size());
            System.out.println("SeqNo = " + message.getSequenceNumber());

            List<ServerMessage> tempMessage = new ArrayList<ServerMessage>();
            for (ServerMessage item : transactions.get("sentTrans"))
            {
                System.out.println("Sent--"+ item);
                System.out.println("seq no ="+item.getSequenceNumber()); 
                if(item.getSequenceNumber() > message.getSequenceNumber())
                     tempMessage.add(item);
            }

            transactions.put("sentTrans", tempMessage);
            LOGGER.info("SentTrans After = " + transactions.get("sentTrans"));
            LOGGER.info("SentTrans After = " + transactions.get("sentTrans").size());
            System.out.println("SentTrans After = " + transactions.get("sentTrans").size());   
        }
        /*
            Function        :  getBalance
            Input           :  RequestReply message which contains the request (contains current balance) sent by the client 
            returnValue     :  ServerMessage object is returned for transferring the update to next successor processor  
        */
        public ServerMessage getBalance(RequestReply message)
        {
            System.out.println("In get balance");
            String accountNumber = message.getAccountNumber();

            Account currAccount = null;
            ServerMessage receivedMessage = new ServerMessage();

            receivedMessage.setReqID(message.getReqID());
            receivedMessage.setAccountNumber(accountNumber);
            receivedMessage.setOperation("GB");
            
            if (this.accountList.containsKey(accountNumber)) 
            {
                currAccount = this.accountList.get(accountNumber);
            }
            else {
                currAccount = new Account();
                this.accountList.put(accountNumber,currAccount);
            }

            receivedMessage.setBalance(currAccount.balance);
            receivedMessage.setOutcome(Outcome.Processed); 

            System.out.println(receivedMessage);
            return receivedMessage;
        }

        /*
            Function        :  deposit
            Input           :  RequestReply message which contains the request sent by the client
            returnValue     :  ServerMessage object is returned for transferring the update to next successor processor  
        */

        public ServerMessage deposit(RequestReply message)
        {
            System.out.println("In Deposit");

            String accountNumber = message.getAccountNumber();
            String reqID = message.getReqID();
            float amount = message.getAmount();

            Account currAccount = null;
            ServerMessage receivedMessage = new ServerMessage();

            //transaction which is added to processedTrans
            String trans = "DP," + reqID + "," + amount;
            Boolean isProcessed = false;


            receivedMessage.setReqID(reqID);
            receivedMessage.setAccountNumber(accountNumber);
            receivedMessage.setOperation("DP");
            receivedMessage.setAmount(amount);

            /*
                Check if given account number is present in the accountList.
                    IF yes check for inconsistency or duplicate request if yes we set the outcome appropriately
                        and create the ServerMessage to pass it to other servers.
                    Else
                        if account is present and transaction is new transaction perform the transaction and set outcome
                        to Processed and create the ServerMessage to pass it to other servers.

                 If account number not present create and perform the deposit and set outcome to Processed 
                 and create the ServerMessage to pass it to other servers.
            */

            if (this.accountList.containsKey(accountNumber)) {

                currAccount = this.accountList.get(accountNumber);
                if (currAccount.processedTrans.contains(trans))
                {
                    receivedMessage.setBalance(currAccount.balance);
                    receivedMessage.setOutcome(Outcome.Processed); 
                    
                    isProcessed = true;
                }
                else
                {
                    for (String s : currAccount.processedTrans)
                        if (s.contains(reqID))
                        {
                            receivedMessage.setBalance(currAccount.balance);
                            receivedMessage.setOutcome(Outcome.InconsistentWithHistory);

                            isProcessed = true;
                            break;
                        }
                }
                if (!isProcessed)
                {
                    currAccount.balance += amount;
                    currAccount.processedTrans.add(trans);
                    this.accountList.put(accountNumber,currAccount);
                    receivedMessage.setBalance(currAccount.balance);
                    receivedMessage.setOutcome(Outcome.Processed);
                }

            } else {
                currAccount = new Account();
                currAccount.balance += amount;
                
                currAccount.processedTrans.add(trans);
                this.accountList.put(accountNumber,currAccount);
            
                receivedMessage.setBalance(currAccount.balance);
                receivedMessage.setOutcome(Outcome.Processed);
            }


            LOGGER.info("For account Number = " + String.valueOf(accountNumber));
            LOGGER.info("Processed Trans = " + Arrays.toString(currAccount.processedTrans.toArray()));
            System.out.println(receivedMessage);

            return receivedMessage;
        }

        /*
            Function        :  withdraw
            Input           :  RequestReply message which contains the request sent by the client
            returnValue     :  ServerMessage object is returned for transferring the update to next successor processor  
        */
        public ServerMessage withdraw(RequestReply message)
        {
            System.out.println("In withdraw");
            String accountNumber = message.getAccountNumber();
            String reqID = message.getReqID();
            float amount = message.getAmount();

            Account currAccount = null;
            ServerMessage receivedMessage = new ServerMessage();

            //transaction which is added to processedTrans
            String trans = "WD," + reqID + "," + amount;
            Boolean isProcessed = false;

            receivedMessage.setReqID(reqID);
            receivedMessage.setAccountNumber(accountNumber);
            receivedMessage.setOperation("WD");
            receivedMessage.setAmount(amount);

            /*
                Check if given account number is present in the accountList.
                    IF yes check for inconsistency or duplicate request if yes we set the outcome appropriately
                        and create the ServerMessage to pass it to other servers.
                    Else
                        if account is present and transaction is new transaction perform the transaction and set outcome
                        to Processed or insufficient funds respectively and create the ServerMessage to pass it to other servers.

                 If account number not present create and set outcome to insufficient funds and create the 
                 ServerMessage to pass it to other servers.
            */

            if (this.accountList.containsKey(accountNumber)) {

                currAccount = this.accountList.get(accountNumber);
                if (currAccount.processedTrans.contains(trans))
                {
                    isProcessed = true;

                    receivedMessage.setBalance(currAccount.balance);
                    receivedMessage.setOutcome(Outcome.Processed);
                }
                else
                {
                    for (String s : currAccount.processedTrans)
                    { 
                        if (s.contains(reqID))
                        {
                            isProcessed = true;

                            receivedMessage.setBalance(currAccount.balance);
                            receivedMessage.setOutcome(Outcome.InconsistentWithHistory);
                            break;
                        }
                    }
                }
                if (!isProcessed)
                {
                    if (currAccount.balance >= amount)
                    {
                        currAccount.balance -= amount;
                        currAccount.processedTrans.add(trans);
            
                        this.accountList.put(accountNumber,currAccount);
                        receivedMessage.setBalance(currAccount.balance);
                        receivedMessage.setOutcome(Outcome.Processed);
                    }
                    else
                    {
                        receivedMessage.setBalance(currAccount.balance);
                        receivedMessage.setOutcome( Outcome.InsufficientFunds);
                    }
                }

            } else {
                currAccount = new Account();
                this.accountList.put(accountNumber,currAccount);
                receivedMessage.setBalance(currAccount.balance);
                receivedMessage.setOutcome( Outcome.InsufficientFunds);
            }

            LOGGER.info("For account Number = " + String.valueOf(accountNumber));
            LOGGER.info("Processed Trans = " + Arrays.toString(currAccount.processedTrans.toArray()));
            System.out.println(receivedMessage);

            return receivedMessage;
        }
    }




