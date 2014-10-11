import java.io.*;
import java.net.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Random;


import java.io.BufferedReader;
import java.io.BufferedWriter;

public class CClientThread extends Thread {

    private String headAddress;
    private String masterAddress;
    private String tailAddress;
    
    private String bankName;

    private int masterPortNo;
    private int headPortNo;
    private int tailPortNo;
    private int accountNumber;

    private int id;
    private int waitTime;
    private int seed;

    private int numMessages;
    private int dpMessage;
    private int wdMessage;
    private int gbMessage;
    private int trMessage;

    private String testReq;

    private String requestInfo;


    public CClientThread(String masterAddress,String headAddress,String tailAddress,int masterPortNo,
                        int headPortNo, int tailPortNo,String info,String bankName,int waitTime,int id)
    {
        this.masterAddress = masterAddress;
        this.headAddress = headAddress;
        this.tailAddress = tailAddress;
        this.bankName = bankName;
        this.masterPortNo = masterPortNo;
        this.headPortNo = headPortNo;
        this.tailPortNo = tailPortNo;
        this.requestInfo = info;
        this.id = id;
        this.waitTime = waitTime;

        Random randAmount = new Random();
        this.accountNumber = randAmount.nextInt(20000); 

    }
    public void decode(String info)
    {
        String [] rval = info.trim().split(",");
        this.seed = Integer.parseInt(rval[0].trim());
        this.numMessages = Integer.parseInt(rval[1].trim());
        this.gbMessage = (int)Math.round(this.numMessages * Float.parseFloat(rval[2].trim()));
        this.dpMessage = (int)Math.round(this.numMessages * Float.parseFloat(rval[3].trim()));
        this.wdMessage = (int)Math.round(this.numMessages * Float.parseFloat(rval[4].trim()));
        this.trMessage = (int)Math.round(this.numMessages * Float.parseFloat(rval[5].trim()));
    }


    public void run() { 
         
        decode(this.requestInfo); 

        System.out.println("seed = "+this.seed);
        System.out.println("numMessages = "+this.numMessages);
        System.out.println("gbMessage= "+ this.gbMessage);
        System.out.println("dpMessage= "+ this.dpMessage);
        System.out.println("wdMessage= "+ this.wdMessage);
        System.out.println("trMessage= "+this.trMessage);
        
        
        DatagramSocket socket = null;
        String hostName = "localhost";
        try 
        {

            socket = new DatagramSocket();
            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
           
            RequestReply sendRequest = null;
            int mCount = 0;
            
            while (true) {

                    mCount = this.gbMessage + this.dpMessage +this.wdMessage;
                    int ch = getRandomChoice();
                    int sendPortNo = 0;
                    // System.out.println("choice for id="+this.id + "\t choice = " + ch);
                    //int ch = 1 + rand.nextInt(3);
                    //System.out.println("choice for id="+this.id + "\t choice = " + ch);

                    if( mCount <= 0)
                    {
                        System.out.println("Done With all messages");
                        break;
                    }   
                    switch(ch)
                    {
                        case 1: sendRequest = getDepositDetails();
                                sendPortNo = headPortNo;
                                break;

                        case 2: sendRequest = getWithdrawDetails();
                                sendPortNo = headPortNo;
                                break;

                        case 3: sendRequest = getCheckBalaceDetails();
                                sendPortNo = tailPortNo;
                                break;

                        default: break;
                    }
                
                    if (sendRequest == null)
                            continue;

                    byte[] buf = new byte[256];
                    byte[] rbuf = new byte[5000];
                    
                    ByteArrayOutputStream bStream = new ByteArrayOutputStream();
                    ObjectOutput oo = new ObjectOutputStream(bStream); 
                    oo.writeObject(sendRequest);
                    oo.close();

                    buf = bStream.toByteArray();

                    InetAddress address = InetAddress.getByName(hostName);
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, address, sendPortNo);
                    socket.send(packet);
                 
                    packet = new DatagramPacket(rbuf, rbuf.length);
                    socket.receive(packet);
             
                    
                    System.out.println("receiveing class object");
                    int byteCount = packet.getLength();
                    ByteArrayInputStream byteStream = new ByteArrayInputStream(rbuf);
                    
                    System.out.println(byteStream );
                    ObjectInputStream is = new ObjectInputStream(new BufferedInputStream(byteStream));
                    RequestReply response = (RequestReply) is.readObject();
                    
                    is.close();
                    //System.out.println("received = <" + response.reqID + "," +
                    //                    response.outcome + "," + response.balance +"> \t" + "for client = " + id);
                    System.out.println("received = " + response.showReply()+" \t" + "for client = " + id);
                    
                    
                    //System.out.println("Client: " + fromUser);
                    Thread.sleep(5000);
                }
            
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + hostName);
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Exception " + e);
            e.printStackTrace();
            System.exit(1);
        }

        socket.close();
    }

    public int getRandomChoice()
    {
        if (gbMessage > 0)
            return 3;
        if (dpMessage > 0)
            return 1;
        if (wdMessage > 0)
            return 2;
    
        return 0;
    }

    public RequestReply getDepositDetails()
    {
        RequestReply message = new RequestReply();
        String id = null;
        
        dpMessage--;
        if (dpMessage < 0)
                return null;

        // if (dpMessage < 2)
        //  {  
        //     System.out.println(testReq);
        //     return testReq;
        // }
        // else if (dpMessage < 1)
        //  {  
        //     System.out.println(testReq);
        //     String []val = testReq.split(";");

        //     return val[0]+";"+val[1]+";"+val[2]+";"+"1234";
        // }
        else
        {
            /*generate deposit req*/
            Random randAmount = new Random();
            Random randSeq = new Random();
            int amount = randAmount.nextInt(1000);
            int reqID = randAmount.nextInt(seed);

            System.out.println("ID : " +id+"ReqId= "+reqID);
            System.out.println("ID : " +id+"amount= "+amount);

            id =  this.bankName + "." + this.id + "." + reqID ;

            message.setReqID(id);
            message.setBankName(this.bankName);
            message.setOperation("DP");
            message.setAccountNumber(String.valueOf(this.accountNumber));
            message.setAmount(amount);
            
            //message = "DP" + ";" + this.bankName + "." + this.id + "." + reqID + ";"  + this.accountNumber + ";" + amount;
            //testReq = message;
        }

        return message;
    }

    public  RequestReply getWithdrawDetails()
    {
        RequestReply message = new RequestReply();
        String id = null;
        wdMessage--;
        if (wdMessage < 0)
               return null;
        else
        {
            /*generate deposit req*/

            Random randAmount = new Random();
            Random randSeq = new Random();
            int amount = randAmount.nextInt(1000);
            int reqID = randAmount.nextInt(seed);

            System.out.println("ID : " +id+"ReqId = "+reqID);
            System.out.println("ID : " +id+"amount= "+amount);

            id =  this.bankName + "." + this.id + "." + reqID ;

            message.setReqID(id);
            message.setBankName(this.bankName);
            message.setOperation("WD");
            message.setAccountNumber(String.valueOf(this.accountNumber));
            message.setAmount(amount);

            //message = "WD" + ";" + this.bankName + "." + this.id + "." + reqID + ";"  + this.accountNumber + ";" + amount;
        }
        return message;   
    }

    public RequestReply getCheckBalaceDetails()
    {
        RequestReply message = new RequestReply();
        String id = null;
        gbMessage--;
        if (gbMessage < 0)
                return null;
        else
        {
            /*generate deposit req*/

            Random randSeq = new Random();
            int reqID = randSeq.nextInt(seed);
            System.out.println("ID : " +id+"reqID= "+reqID);

            id =  this.bankName + "." + this.id + "." + reqID ;

            message.setReqID(id);
            message.setOperation("GB");
            message.setAccountNumber(String.valueOf(this.accountNumber));
            message.setBankName(this.bankName);
            //message = "GB" + ";" + this.bankName + "." + this.id + "." + reqID + ";"  + this.accountNumber;
        }    
        return message;
    }

    public  String getTransferDetails()
    {
        trMessage--;
        if (trMessage <= 0)
                return "nop";
        else
        {
            /*generate deposit req*/
            /*
            Random randAmount = new Random();
            Random randSeq = new Random();
            int amount = randAmount.nextInt(1000); 
            int reqID = randAmount.nextInt(seed);

            System.out.println("ID : " +id+"ReqId= "+reqID);
            System.out.println("ID : " +id+"amount= "+amount);
            */
        }
        return "TR,reqId,sr_acc,des_acc,des_bank,amt";
    }
}