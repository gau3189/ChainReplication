/*
 *  PHASE 2: NON-FAULT-TOLERANT SERVICE
 *  TEAM MEMBERS: 
 * 
 *  GAUTHAM REDDY KUNTA ID: 109596312
 *  NAFEES AHMED ABDUL ID: 109595182
 *
 *
 *  ClientThread.java
 *
 */

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Random;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.logging.Logger;
import java.util.logging.Level;

public class ClientThread extends Thread {

    private String headAddress;
    private String masterAddress;
    private String tailAddress;
    
    private String bankName;

    private int masterPortNo;
    private int headPortNo;
    private int tailPortNo;
    private int accountNumber;
    

    private int id;
    private int resendTrails;
    private int seed;
    private int waitTime;

    /*
        For random request generation total number of messages to be generated.
        isRandomRequest specifies whether client generates random request or itemized request.
    */
    private int numMessages;
    private Boolean isRandomRequest;

    private List<String> requests;

    //For Logging the information
    private final  Logger LOGGER ;
    Random randNumber;
    
    public ClientThread(String masterAddress,String headAddress,String tailAddress,int masterPortNo,
                        int headPortNo, int tailPortNo,List<String> requests,String bankName,
                        int resendTrails,int waitTime,int id, Logger LOGGER,Boolean isRandomRequest)
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
        this.resendTrails = resendTrails;
        this.isRandomRequest = isRandomRequest;

        randNumber = new Random();
        this.LOGGER = LOGGER;
    }
    

    /*
        Function        :  run()
        Description     :  it is called when the thread is created for this class
    */
    public void run()
    { 
        Random choice = new Random(); 
        DatagramSocket udpSocket = null;
        int result = 0;
        int myPortNo = 0;

        /*
            Generating a specific udp socket for the client to communicate with server.
        */
        while(true)
        {
            try 
            {
                myPortNo = randNumber.nextInt(64000);
                udpSocket = new DatagramSocket(myPortNo);
                if (myPortNo!=0)
                    break;
            }
            catch(Exception e)
            {
                System.out.println(e.getMessage());
            }

        }
        LOGGER.config("Client ID :: " + this.id + " UDP Socket PortNo ::  "+ myPortNo);
        try 
        {
            udpSocket.setSoTimeout(this.waitTime);

            RequestReply sendRequest = null;
            int sendPortNo = 0;
            String sendAddress = null;
            if (isRandomRequest) 
            {    

                /*
                    Get the request list to be processed. In this case the request is of the form
                    (seed, numReq, probGetBalance,probDeposit, probWithdraw, probTransfer)

                    Decode the request and get the respective values
                */

                List<String> requestList = decode(this.requests); 

                LOGGER.info("seed = "+this.seed);
                LOGGER.info("numMessages = "+this.numMessages);
                    
                while (true) {

                        if( this.numMessages <= 0)
                        {
                            System.out.println("Done With all messages");
                            LOGGER.info("Done With all messages");
                            break;
                        }  

                        String ch = requestList.get(choice.nextInt(requestList.size()));

                        /*
                            Setting up the RequestReply object for communication.
                        */
                        switch(ch.toLowerCase())
                        {
                            case "dp":  sendRequest = getDepositDetails();
                                        sendPortNo = headPortNo;
                                        sendAddress = headAddress;
                                        break;

                            case "wd":  sendRequest = getWithdrawDetails();
                                        sendPortNo = headPortNo;
                                        sendAddress = headAddress;
                                        break;

                            case "gb":  sendRequest = getCheckBalaceDetails();
                                        sendPortNo = tailPortNo;
                                        sendAddress = tailAddress;
                                        break;

                            default: break;
                        }
                    
                        if (sendRequest == null)
                                continue;


                        result = sendMessage(sendRequest,sendAddress, sendPortNo, udpSocket);
                        
                        if (result < 0)
                            System.exit(1);
                        else if (result == 0)
                        {
                            for (int i =0; i < this.resendTrails; i++)
                            {
                              result =  sendMessage(sendRequest, sendAddress,sendPortNo, udpSocket);
                              if (result < 0 || result ==1)
                                break;
                            }
                            this.numMessages--;
                        }
                        else
                            this.numMessages--;

                        if (result < 0)
                            System.exit(1);

                        sendAddress = null;
                    }
                }
                else
                {
                    /*
                        Serving each of the itemized request read from the configuration file    
                    */
                    LOGGER.info("CLIENT ID :: " + this.id +
                            " message = Serving each of the itemized request read from the configuration file");    
                    for (String request : this.requests)
                    {
                        String []val = request.split(",");

                        /*
                            Setting up the RequestReply object for communication.
                        */
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
                                        sendAddress = headAddress;
                                        break;

                            case "gb":  sendPortNo = tailPortNo;
                                        sendAddress = tailAddress;
                                        break;

                            default: break;
                        }

                        result = sendMessage(sendRequest, sendAddress, sendPortNo, udpSocket);
                        

                        if (result < 0)
                            System.exit(1);
                        else if (result == 0)
                        {
                            for (int i =0; i < this.resendTrails; i++)
                            {
                              LOGGER.info("CLIENT ID :: " + this.id + " Re-Sending Request to server = " +
                               sendRequest.showRequest() + " trail = " + i);
                              result =  sendMessage(sendRequest, sendAddress,sendPortNo, udpSocket);
                              if (result < 0 || result ==1)
                                break;
                            }
                        }
                       
                        if (result < 0)
                            System.exit(1);

                        sendAddress = null;
                    }

                }
            }
            catch (Exception e) {
                System.err.println("Exception " + e);
                LOGGER.severe("Exception " + e);
                e.printStackTrace();
                System.exit(1);
            }
        
            udpSocket.close();
    }
    /*
        Function        :   sendMessage
        Inputs          :   RequestReply
                        :   sendPortNo
                        :   udpSocket

        Description     :  RequestReply for sending request to server at portno sendPortNo using datagram udpSocket. 
    */

    public int sendMessage(RequestReply sendRequest,String sendAddress,int sendPortNo,DatagramSocket udpSocket)
    {
        try 
        {  
            byte[] buf = new byte[256];
            byte[] rbuf = new byte[5000];
            
            System.out.println("Sending Request to server = " + sendRequest.showRequest() +
                                "for client = " + this.id);
            LOGGER.info("CLIENT ID :: " + this.id + " Sending Request to server = " + sendRequest.showRequest() + " at " + sendPortNo);

            ByteArrayOutputStream bStream = new ByteArrayOutputStream();
            ObjectOutput oo = new ObjectOutputStream(bStream); 
            oo.writeObject(sendRequest);
            oo.close();

            buf = bStream.toByteArray();

            InetAddress address = InetAddress.getByName(sendAddress);
            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, sendPortNo);
            udpSocket.send(packet);
         
            packet = new DatagramPacket(rbuf, rbuf.length);
            udpSocket.receive(packet);
            
            int byteCount = packet.getLength();
            ByteArrayInputStream byteStream = new ByteArrayInputStream(rbuf);
            
            ObjectInputStream is = new ObjectInputStream(new BufferedInputStream(byteStream));
            RequestReply response = (RequestReply) is.readObject();
            is.close();
            System.out.println("received message from server = " + response.showReply() +
                                "for client = " + id);
            
            LOGGER.info("CLIENT ID :: " + this.id + " received message = " + response.showReply());

            //Thread.sleep(this.waitTime);
            Thread.sleep(10000);
        } 
        catch (UnknownHostException e) {
                System.err.println("Don't know about host ");
                LOGGER.severe("Don't know about host " );
                return -1;
            }
        catch (Exception e) {

            if (e.getMessage().contains("Receive timed out"))
            {
                System.out.println("Recieved timed out");
                return 0;
            }
            else
            {
                LOGGER.severe("Exception " + e);
                e.printStackTrace();
                return -1;
            }
        }
        return 1;
    }
    /*
        Function        :   decode
        Input           :   string for decoding request message read from the configuration file
                        :   to generate random requests. 
    */
    public List<String> decode(List<String> requests)
    {
        int dpMessage = 0;
        int wdMessage = 0;
        int gbMessage = 0;
        
        List<String> requestList = new ArrayList<String>();
        String [] rval = requests.get(0).split(",");


        this.seed = Integer.parseInt(rval[0].trim());
        this.numMessages = Integer.parseInt(rval[1].trim());
        gbMessage = (int)Math.round(100 * Float.parseFloat(rval[2].trim()));
        dpMessage = (int)Math.round(100 * Float.parseFloat(rval[3].trim()));
        wdMessage = (int)Math.round(100 * Float.parseFloat(rval[4].trim()));
        //this.trMessage = (int)Math.round(this.numMessages * Float.parseFloat(rval[5].trim()));

        for ( int i=0; i< gbMessage; i++)
            requestList.add("GB");

        for ( int i=0; i< dpMessage; i++)
            requestList.add("DP");

        for ( int i=0; i< wdMessage; i++)
            requestList.add("WD");

        return requestList;
    }

    
    /*
        Function        :  getDepositDetails
        returnValue     :  RequestReply message which contains the request for the server
    */
    public RequestReply getDepositDetails()
    {
        RequestReply message = new RequestReply();
        String id = null;
        int amount = randNumber.nextInt(1000);
        int reqID = randNumber.nextInt(seed);
        this.accountNumber = randNumber.nextInt(60);
        
        id =  this.bankName + "." + this.id + "." + reqID ;
        
        /*
            Debug messages
        */
        System.out.println("ID : " +this.id +"ReqId= "+reqID);
        System.out.println("ID : " +this.id +"amount= "+amount);

        message.setReqID(id);
        message.setBankName(this.bankName);
        message.setOperation("DP");
        message.setAccountNumber(String.valueOf(this.accountNumber));
        message.setAmount(amount);
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
        int amount = randNumber.nextInt(1000);
        int reqID = randNumber.nextInt(seed);
        this.accountNumber = randNumber.nextInt(60);

        /*
            Debug messages
        */

        System.out.println("ID : " +this.id+"ReqId = "+reqID);
        System.out.println("ID : " +this.id+"amount= "+amount);

        id =  this.bankName + "." + this.id + "." + reqID ;

        message.setReqID(id);
        message.setBankName(this.bankName);
        message.setOperation("WD");
        message.setAccountNumber(String.valueOf(this.accountNumber));
        message.setAmount(amount);
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
        int reqID = randNumber.nextInt(seed);
        this.accountNumber = randNumber.nextInt(60);
        /*
            Debug messages
        */
        System.out.println("ID : " +this.id+"reqID= "+reqID);

        id =  this.bankName + "." + this.id + "." + reqID ;

        message.setReqID(id);
        message.setOperation("GB");
        message.setAccountNumber(String.valueOf(this.accountNumber));
        message.setBankName(this.bankName);
        return message;
    }
    
}