import java.net.*;
import java.io.*;
import java.util.*;
 
public class CRMClientServerThread extends Thread {
    
    private DatagramSocket socket = null;
    private Socket sSocket = null;
    private int succPortNo;
    private String hostName;
    private Map<String,Account> accountList;

    public CRMClientServerThread(DatagramSocket socket,String hostName,int succPortNo, Map<String,Account> accountList) 
    {
        super("CRMClientServerThread");
        System.out.println("in CRMClientServerThread");
        this.socket = socket;
        this.succPortNo = succPortNo;
        this.hostName = hostName; 
        this.accountList = accountList;
    }
     
    public void run() {
 
        try  
        {
            String reply;
            //PrintWriter out = null;

            ObjectOutputStream out = null;
            RequestReply response = new RequestReply();
            ServerMessage receivedMessage = new ServerMessage();
            while (true)
            {
                byte[] buf = new byte[256];
 
                // receive request
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
 
                String rMessage = new String(buf);
                System.out.println("Recieved = \n" + rMessage);

                receivedMessage = update(rMessage);

                receivedMessage.setPortNumber(packet.getPort());
                receivedMessage.setHostAddress(packet.getAddress().getHostAddress());


                if(succPortNo!=0 && sSocket == null)
                {
                    System.out.println("CRMClientServerThread Socket creation");
                    this.sSocket = new Socket(hostName, succPortNo);
                    //out = new PrintWriter(sSocket.getOutputStream(), true);
                    out = new ObjectOutputStream(sSocket.getOutputStream()); 
                }

               
                if( succPortNo!=0 && out != null)
                {
                    System.out.println("Sending to other server" + receivedMessage);
                    out.writeObject(receivedMessage);
                    //out.println(reply);
                }
                else
                {
                    response.setReqId(receivedMessage.reqID);
                    response.setBalance(receivedMessage.balance);
                    response.setOutcome(receivedMessage.outcome);
                    response.setOperation(receivedMessage.operation);

                    ByteArrayOutputStream bStream = new ByteArrayOutputStream();
                    ObjectOutput oo = new ObjectOutputStream(bStream); 
                    oo.writeObject(response);
                    oo.close();

                    buf = bStream.toByteArray();

                    InetAddress address = packet.getAddress();
                    int port = packet.getPort();

                    System.out.println("Sending to client" + receivedMessage);

                    packet = new DatagramPacket(buf, buf.length, address, port);
                    socket.send(packet);
                   
                }   
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ServerMessage update(String message)
    {
       // GB;CITI.0.0;11897;
        System.out.println("Update");
        String reply = null;
        String []retval = message.split(";");

        ServerMessage receivedMessage = new ServerMessage();
                

        switch(retval[0])
        {
            case "GB":  receivedMessage = getBalance(retval[1], retval[2]);
                        break;
            case "DP":  //reply = deposit(retval[1], retval[2], Float.parseFloat(retval[3]));
                        receivedMessage = deposit(retval[1], retval[2], Float.parseFloat(retval[3]));
                        break;
            case "WD":  receivedMessage = withdraw(retval[1], retval[2], Float.parseFloat(retval[3]));
                        break;
            default:    break;
        }
        return receivedMessage;
    }

    public ServerMessage getBalance(String reqID, String accountNumber)
    {
        String reply = null;
        
        //String []val = reqID.split(".");
        //if (!val[0].equals(this.bankName))
        //    return "invalidRequest";

        Account currAccount = null;
        ServerMessage receivedMessage = new ServerMessage();

        receivedMessage.setReqID(reqID);
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
                

        //reply = "<"+ reqID + "," + "Processed, " + currAccount.balance + ">";

        System.out.println(receivedMessage);
        return receivedMessage;

    }

    public ServerMessage deposit(String reqID, String accountNumber, float amount)
    {
        //String []val = reqID.split(".");
        // if (!val[0].equals(this.bankName))
        //     return "invalidRequest";

        Account currAccount = null;
        ServerMessage receivedMessage = new ServerMessage();

        String trans = "DP," + reqID + "," + amount;;
        String reply = null;
        Boolean isProcessed = false;


        receivedMessage.setReqID(reqID);
        receivedMessage.setAccountNumber(accountNumber);
        receivedMessage.setOperation("DP");
        receivedMessage.setAmount(amount);

        if (this.accountList.containsKey(accountNumber)) {

            currAccount = this.accountList.get(accountNumber);
            if (currAccount.processedTrans.contains(trans))
            {
                reply = "<"+ reqID + "," + "Processed, " + currAccount.balance + ">";

                receivedMessage.setBalance(currAccount.balance);
                receivedMessage.setOutcome(Outcome.Processed); 
                
                isProcessed = true;
            }
            else
            {
                for (String s : currAccount.processedTrans)
                    if (s.contains(reqID))
                    {
                        reply = "<"+ reqID + "," + "InconsistentWithHistory, " + currAccount.balance + ">";

                        receivedMessage.setBalance(currAccount.balance);
                        receivedMessage.setOutcome(Outcome.InconsistentWithHistory);

                        isProcessed = true;
                        break;
                    }
            }
            if (!isProcessed)
            {
                System.out.println("Account List = "+currAccount.balance);

                currAccount.balance += amount;
                currAccount.processedTrans.add(trans);
                
                reply = "<"+ reqID + "," + "Processed, " + currAccount.balance + ">";
            
                receivedMessage.setBalance(currAccount.balance);
                receivedMessage.setOutcome(Outcome.Processed);
            }

        } else {
            currAccount = new Account();
            currAccount.balance += amount;
            
            currAccount.processedTrans.add(trans);
            this.accountList.put(accountNumber,currAccount);
            reply = "<"+ reqID + "," + "Processed, " + currAccount.balance + ">";

            receivedMessage.setBalance(currAccount.balance);
            receivedMessage.setOutcome(Outcome.Processed);
        }

        System.out.println(receivedMessage);
        //return reply;
        return receivedMessage;
    }

    public ServerMessage withdraw(String reqID, String accountNumber, float amount)
    {
        Account currAccount = null;
        ServerMessage receivedMessage = new ServerMessage();

        String trans = "WD," + reqID + "," + amount;;
        String reply = null;
        Boolean isProcessed = false;

        receivedMessage.setReqID(reqID);
        receivedMessage.setAccountNumber(accountNumber);
        receivedMessage.setOperation("WD");
        receivedMessage.setAmount(amount);

        if (this.accountList.containsKey(receivedMessage.accountNumber)) {

            currAccount = this.accountList.get(accountNumber);
            if (currAccount.processedTrans.contains(trans))
            {
                reply = "<"+ reqID + "," + "Processed, " + currAccount.balance + ">";
                isProcessed = true;

                receivedMessage.setBalance(currAccount.balance);
                receivedMessage.setOutcome(Outcome.Processed);
            }
            else
            {
                for (String s : currAccount.processedTrans)
                { 
                    System.out.println("s.contains(reqID)"+s.contains(reqID));
                    if (s.contains(reqID))
                    {
                        reply = "<"+ reqID + "," + "InconsistentWithHistory, " + currAccount.balance + ">";
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
                    System.out.println("Account List = "+currAccount.balance);
                    currAccount.balance -= amount;
                    currAccount.processedTrans.add(trans);
                    reply = "<"+ reqID + "," + "Processed, " + currAccount.balance + ">";

                    receivedMessage.setBalance(currAccount.balance);
                    receivedMessage.setOutcome(Outcome.Processed);
                }
                else
                {
                    reply = "<"+ reqID + "," + "InsufficientFunds, " + currAccount.balance + ">";
                    receivedMessage.setBalance(currAccount.balance);
                    receivedMessage.setOutcome( Outcome.InsufficientFunds);
                }
            }

        } else {
            currAccount = new Account();
            this.accountList.put(accountNumber,currAccount);
            reply = "<"+ reqID + "," + "InsufficientFunds, " + currAccount.balance + ">";
            receivedMessage.setBalance(currAccount.balance);
            receivedMessage.setOutcome( Outcome.InsufficientFunds);
        }

        System.out.println(receivedMessage);
        //return reply;
        return receivedMessage;
    }
}




