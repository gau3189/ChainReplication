import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Random;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.logging.Logger;
import java.util.logging.Level;

public class CClientThread extends Thread {

    private String headAddress;
    private String masterAddress;
    private String tailAddress;
    
    private String bankName;

    private int masterPortNo;
    private int headPortNo;
    private int tailPortNo;
    private int accountNumber;
    private List<String> requests;

    private int id;
    private int waitTime;
    private int seed;

    private int numMessages;
    private int dpMessage;
    private int wdMessage;
    private int gbMessage;
    private int trMessage;

    private String testReq;

    //private String requestInfo;
    private final  Logger LOGGER ;

    public CClientThread(String masterAddress,String headAddress,String tailAddress,int masterPortNo,
                        int headPortNo, int tailPortNo,List<String> requests,String bankName,int waitTime,int id, Logger LOGGER)
    {
        this.masterAddress = masterAddress;
        this.headAddress = headAddress;
        this.tailAddress = tailAddress;
        this.bankName = bankName;
        this.masterPortNo = masterPortNo;
        this.headPortNo = headPortNo;
        this.tailPortNo = tailPortNo;
        this.requests = requests;
        this.id = id;
        this.waitTime = waitTime;

        Random randAmount = new Random();
        this.accountNumber = randAmount.nextInt(20000); 
        this.LOGGER = LOGGER;

    }
    

    /*
        Function        :  run
        Description     :  it is called when the thread is created for this class
    */
    public void run() { 
         
        DatagramSocket socket = null;
        try 
        {
            socket = new DatagramSocket();
            RequestReply sendRequest = null;
            int sendPortNo = 0;
            if (this.requests.size() == 1) 
            {    
                decode(this.requests); 

                LOGGER.info("seed = "+this.seed);
                LOGGER.info("numMessages = "+this.numMessages);
                LOGGER.info("gbMessage= "+ this.gbMessage);
                LOGGER.info("dpMessage= "+ this.dpMessage);
                LOGGER.info("wdMessage= "+ this.wdMessage);
                LOGGER.info("trMessage= "+this.trMessage);

                System.out.println("seed = "+this.seed);
                System.out.println("numMessages = "+this.numMessages);
                System.out.println("gbMessage= "+ this.gbMessage);
                System.out.println("dpMessage= "+ this.dpMessage);
                System.out.println("wdMessage= "+ this.wdMessage);
                System.out.println("trMessage= "+this.trMessage);
                
                    
                int mCount = 0;
                
                while (true) {

                        mCount = this.gbMessage + this.dpMessage +this.wdMessage;
                        int ch = getRandomChoice();
                        
                        // System.out.println("choice for id="+this.id + "\t choice = " + ch);
                        //int ch = 1 + rand.nextInt(3);
                        //System.out.println("choice for id="+this.id + "\t choice = " + ch);

                        if( mCount <= 0)
                        {
                            System.out.println("Done With all messages");
                            LOGGER.info("Done With all messages");
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

                        sendMessage(sendRequest, sendPortNo, socket);
                    }
                }
                else
                {
                    //DP, 1.1.1, 4600
                    for (String request : this.requests)
                    {
                        String []val = request.split(",");

                        sendRequest = new RequestReply();
                        sendRequest.setOperation(val[0].trim());
                        sendRequest.setReqID(val[1].trim());


                        sendRequest.setBankName(this.bankName);
                        sendRequest.setAccountNumber(val[2].trim());
                        
                        
                        switch(val[0].trim().toLowerCase())
                        {
                            case "dp":  
                            case "wd":  sendRequest.setAmount(Float.parseFloat(val[3]));
                                        sendPortNo = headPortNo;
                                        break;

                            case "gb":  sendPortNo = tailPortNo;
                                        break;

                            default: break;
                        }
                        sendMessage(sendRequest, sendPortNo, socket);
                    }

                }
            }
            catch (Exception e) {
                System.err.println("Exception " + e);
                LOGGER.severe("Exception " + e);
                e.printStackTrace();
                System.exit(1);
            }
        
            socket.close();
        
    }
    /*
        Function        :  sendMessage
        Input           :  RequestReply for sending request to server at portno sendPortNo using datagram socket. 
    */

    public void sendMessage(RequestReply sendRequest,int sendPortNo,DatagramSocket socket)
    {
        try 
        {  
            String hostName = "localhost";
            byte[] buf = new byte[256];
            byte[] rbuf = new byte[5000];
            
            LOGGER.info("Sending Request to server = " + sendRequest + "at" + sendPortNo);

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
     
            
            int byteCount = packet.getLength();
            ByteArrayInputStream byteStream = new ByteArrayInputStream(rbuf);
            
            ObjectInputStream is = new ObjectInputStream(new BufferedInputStream(byteStream));
            RequestReply response = (RequestReply) is.readObject();
            is.close();
            //System.out.println("received = <" + response.reqID + "," +
            //                    response.outcome + "," + response.balance +"> \t" + "for client = " + id);
            System.out.println("received message from server =" + response.showReply() +
                                "for client = " + id);
            
            LOGGER.info("received message from server =" + response.showReply() +
                        "for client = " + id);

            Thread.sleep(5000);
        } 
        catch (UnknownHostException e) {
                System.err.println("Don't know about host ");
                LOGGER.severe("Don't know about host " );
                System.exit(1);
            }
        catch (Exception e) {
            System.err.println("Exception " + e);
            LOGGER.severe("Exception " + e);
            e.printStackTrace();
            System.exit(1);
        }

    }
    /*
        Function        :  decode
        Input           :  string for decoding request message  read from the configuration file. 
    */
    public void decode(List<String> requests)
    {
        String [] rval = requests.get(0).split(",");
        this.seed = Integer.parseInt(rval[0].trim());
        this.numMessages = Integer.parseInt(rval[1].trim());
        this.gbMessage = (int)Math.round(this.numMessages * Float.parseFloat(rval[2].trim()));
        this.dpMessage = (int)Math.round(this.numMessages * Float.parseFloat(rval[3].trim()));
        this.wdMessage = (int)Math.round(this.numMessages * Float.parseFloat(rval[4].trim()));
        this.trMessage = (int)Math.round(this.numMessages * Float.parseFloat(rval[5].trim()));
    }

    /*
        Function        :  getRandomChoice
        returnValue     :  returns choice for generating either deposit or withdraw or check balance. 
    */
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

    /*
        Function        :  getDepositDetails
        returnValue     :  RequestReply message which contains the request for the server
    */
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
        //else
        {
            /*generate deposit req*/
            Random randAmount = new Random();
            Random randSeq = new Random();
            int amount = randAmount.nextInt(1000);
            int reqID = randAmount.nextInt(seed);

            

            id =  this.bankName + "." + this.id + "." + reqID ;
            System.out.println("ID : " +this.id +"ReqId= "+reqID);
            System.out.println("ID : " +this.id +"amount= "+amount);

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

    /*
        Function        :  getWithdrawDetails
        returnValue     :  RequestReply message which contains the request for the server
    */
    public RequestReply getWithdrawDetails()
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

            System.out.println("ID : " +this.id+"ReqId = "+reqID);
            System.out.println("ID : " +this.id+"amount= "+amount);

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

    /*
        Function        :  getCheckBalaceDetails
        returnValue     :  RequestReply message which contains the request for the server
    */
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
            System.out.println("ID : " +this.id+"reqID= "+reqID);

            id =  this.bankName + "." + this.id + "." + reqID ;

            message.setReqID(id);
            message.setOperation("GB");
            message.setAccountNumber(String.valueOf(this.accountNumber));
            message.setBankName(this.bankName);
            //message = "GB" + ";" + this.bankName + "." + this.id + "." + reqID + ";"  + this.accountNumber;
        }    
        return message;
    }

    /*
        Function        :  getTransferDetails
        returnValue     :  ServerMessage object is returned for transferring the update to next successor processor  
    */
    public  RequestReply getTransferDetails()
    {
        trMessage--;
        RequestReply message = new RequestReply();
        if (trMessage <= 0)
                return null;
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
        return message;
    }
}