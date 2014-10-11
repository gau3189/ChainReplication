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
            PrintWriter out = null;
            
            
            
            while (true)
            {

                byte[] buf = new byte[256];
 
                // receive request
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
 
                String receivedMessage = new String(buf);
                System.out.println("Recieved = \n" + receivedMessage);

                reply = update(receivedMessage);
                /*String []retval = receivedMessage.split(";");
                for (String rval: retval)
                         System.out.println(rval);

                switch(retval[0])
                {
                    case "WD":  reply = "withdraw Successful";
                                break;
                    case "DP":  reply = receivedMessage;
                                break;
                    case "GB":  reply = "Check balance SUCCESSFUL" ;
                                break;
                    default:    reply = "NOP";
                                break;
                }*/
                // figure out response
                //reply ="UDP server mess";
 
                buf = reply.getBytes();
 
                // send the response to the client at "address" and "port"
                reply = reply + ":" + packet.getAddress().getHostAddress()+ ":" + packet.getPort();
                System.out.println("reply = "+ reply); 

                if(succPortNo!=0 && sSocket == null)
                {
                    System.out.println("CRMClientServerThread Socket creation");
                    this.sSocket = new Socket(hostName, succPortNo);
                    out = new PrintWriter(sSocket.getOutputStream(), true);
                }

               
                if( succPortNo!=0 && out != null)
                {
                    System.out.println("Sending to other server" + reply);
                    out.println(reply);
                }
                else
                {
                    InetAddress address = packet.getAddress();
                    int port = packet.getPort();

                    packet = new DatagramPacket(buf, buf.length, address, port);
                    socket.send(packet);
                   
                }   
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String update(String message)
    {
       // GB;CITI.0.0;11897;
        System.out.println("Update");
        String reply = null;
        String []retval = message.split(";");
                

        switch(retval[0])
        {
            case "GB":  reply = getBalance(retval[1], retval[2]);
                        break;
            case "DP":  reply = deposit(retval[1], retval[2], Float.parseFloat(retval[3]));
                        break;
            case "WD":  reply = withdraw(retval[1], retval[2], Float.parseFloat(retval[3]));
                        break;
            default:    reply = "NOP";
                        break;
        }
        return reply;
    }

    public String getBalance(String reqID, String accountNumber)
    {
        String reply = null;
        
        //String []val = reqID.split(".");
        //if (!val[0].equals(this.bankName))
        //    return "invalidRequest";

        Account currAccount = null;

        if (this.accountList.containsKey(accountNumber)) 
        {
            currAccount = this.accountList.get(accountNumber);
        }
        else {
            currAccount = new Account();
            this.accountList.put(accountNumber,currAccount);
        }

        reply = "<"+ reqID + "," + "Processed, " + currAccount.balance + ">";

        System.out.println(reply);
        return reply;

    }

    public String deposit(String reqID, String accountNumber, float amount)
    {
        //String []val = reqID.split(".");
        // if (!val[0].equals(this.bankName))
        //     return "invalidRequest";

        Account currAccount = null;
        String trans = "DP," + reqID + "," + amount;;
        String reply = null;
        Boolean isProcessed = false;
        if (this.accountList.containsKey(accountNumber)) {

            currAccount = this.accountList.get(accountNumber);
            if (currAccount.processedTrans.contains(trans))
            {
                reply = "<"+ reqID + "," + "Processed, " + currAccount.balance + ">";
                isProcessed = true;
            }
            else
            {
                for (String s : currAccount.processedTrans)
                    if (s.contains(reqID))
                    {
                        reply = "<"+ reqID + "," + "InconsistentWithHistory, " + currAccount.balance + ">";
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
            }

        } else {
            currAccount = new Account();
            currAccount.balance += amount;
            
            currAccount.processedTrans.add(trans);
            this.accountList.put(accountNumber,currAccount);
            reply = "<"+ reqID + "," + "Processed, " + currAccount.balance + ">";
        }

        System.out.println(reply);
        return reply;
    }

    public String withdraw(String reqID, String accountNumber, float amount)
    {
        Account currAccount = null;
        String trans = "WD," + reqID + "," + amount;;
        String reply = null;
        Boolean isProcessed = false;
        if (this.accountList.containsKey(accountNumber)) {

            currAccount = this.accountList.get(accountNumber);
            if (currAccount.processedTrans.contains(trans))
            {
                reply = "<"+ reqID + "," + "Processed, " + currAccount.balance + ">";
                isProcessed = true;
            }
            else
            {
                for (String s : currAccount.processedTrans)
                    if (s.contains(reqID))
                    {
                        reply = "<"+ reqID + "," + "InconsistentWithHistory, " + currAccount.balance + ">";
                        isProcessed = true;
                        break;
                    }
            }
            if (!isProcessed && currAccount.balance >= amount)
            {
                System.out.println("Account List = "+currAccount.balance);
                currAccount.balance -= amount;
                currAccount.processedTrans.add(trans);
                reply = "<"+ reqID + "," + "Processed, " + currAccount.balance + ">";
            }
            else
            {
                reply = "<"+ reqID + "," + "InsufficientFunds, " + currAccount.balance + ">";
            }


        } else {
            currAccount = new Account();
            this.accountList.put(accountNumber,currAccount);
            reply = "<"+ reqID + "," + "InsufficientFunds, " + currAccount.balance + ">";
        }

        System.out.println(reply);
        return reply;
    }
}




