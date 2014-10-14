/*
 *  PHASE 2: NON-FAULT-TOLERANT SERVICE
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
 
public class CRClientServerThread extends Thread {
    
    private DatagramSocket socket = null;
    private Socket sSocket = null;
    private int succPortNo;
    private String successor;
    private Map<String,Account> accountList;
    private final  Logger LOGGER ;
    public CRClientServerThread(DatagramSocket socket,String successor,int succPortNo, Map<String,Account> accountList, Logger LOGGER) 
    {
        super("CRClientServerThread");
        System.out.println("in CRClientServerThread");
        this.socket = socket;
        this.succPortNo = succPortNo;
        this.successor = successor; 
        this.accountList = accountList;
        this.LOGGER = LOGGER;
    }
     
    /*
        Function        :  run
        Description     :  it is called when the thread is created for this class
    */ 
    public void run() {
 
        try  
        {
            ObjectOutputStream out = null;
            RequestReply response = new RequestReply();
            ServerMessage receivedMessage = new ServerMessage();

            LOGGER.info("IN CRClientServerThread WAITING FOR CLIENT");
            while (true)
            {
                byte[] buf = new byte[256];
 
                /* receive request */
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
            
                int byteCount = packet.getLength();
                ByteArrayInputStream byteStream = new ByteArrayInputStream(buf);
                
                ObjectInputStream is = new ObjectInputStream(new BufferedInputStream(byteStream));
                RequestReply clientMessage = (RequestReply) is.readObject();
                
                is.close();

                LOGGER.info("Recieved Request from client = " + clientMessage.showRequest());

                receivedMessage = processRequest(clientMessage);

                receivedMessage.setPortNumber(packet.getPort());
                receivedMessage.setHostAddress(packet.getAddress().getHostAddress());

                if(succPortNo!=0 && sSocket == null)
                {
                    System.out.println("CRClientServerThread Socket creation");
                    LOGGER.info("CRClientServerThread Socket creation");
                    
                    this.sSocket = new Socket(InetAddress.getByName(successor), succPortNo);
                    out = new ObjectOutputStream(sSocket.getOutputStream()); 
                }
                if( succPortNo!=0 && out != null)
                {
                    System.out.println("Sending Update to other server = " + receivedMessage);
                    LOGGER.info("Sending Update to other server = " + receivedMessage);
                    out.writeObject(receivedMessage);
                }
                else
                {
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

                    InetAddress address = packet.getAddress();
                    int port = packet.getPort();
                    LOGGER.info("Sending Response to client = " + receivedMessage);
                    packet = new DatagramPacket(buf, buf.length, address, port);
                    socket.send(packet);
                   
                }   
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            socket.close();
            System.exit(1);
        }
    }

    /*
        Function        :  processRequest
        Input           :  RequestReply message which contains the request sent by the client
        returnValue     :  ServerMessage object is returned for transferring the update to next successor processor  
    */
    public ServerMessage processRequest(RequestReply message)
    {
        LOGGER.info("Process Request");
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




