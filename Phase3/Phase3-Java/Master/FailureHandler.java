/*
 *  PHASE 3: FAULT-TOLERANT SERVICE
 *  TEAM MEMBERS: 
 * 
 *  GAUTHAM REDDY KUNTA ID: 109596312
 *  NAFEES AHMED ABDUL ID: 109595182
 *
 *
 *  MAsterClientThread.java
 *
 */

import java.net.*;
import java.io.*;
import java.util.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.sql.Timestamp;
import java.util.Date;


public class FailureHandler extends Thread {

  private  Map<String, String> configMap;
  private Map<String,BankChain> bankList;
  private Map<String, List<ClientInfo>> clientList;
  private int serverPortNo;
  private String serverAddress;
  private String bankName;
  private DatagramSocket socket;
  private final Logger LOGGER ;

  public FailureHandler(DatagramSocket socket, Map<String, String> map, Map<String,List<ClientInfo>> list,Map<String,BankChain> bankList,Logger LOGGER)
  {
      super("FailureHandler");
      System.out.println("In Failure Handler ");
      this.configMap = map;
      this.bankList = bankList;
      this.clientList = list;
      this.socket = socket;
      this.LOGGER = LOGGER;
  }

  public void run()
  {
        MasterMessage responseMessage;  
        try {Thread.sleep(10000);}
        catch(Exception e){} 
          
        while(true)
        {
          try 
          {
              Thread.sleep(3000);
              int size = 0;
                      
              for (Map.Entry<String,BankChain> entry :bankList.entrySet())
              {
                  LOGGER.info("FailureHandler :: Currently UP servers " + entry.getValue().failedServers);

                  //System.out.println(entry.getValue().failedServers);
                  size = entry.getValue().chainLength;
                
                  if(entry.getValue().failedServers.size() > 0 && entry.getValue().failedServers.size() < size)
                  {
                      responseMessage = informOtherServers(entry);
                      for(int portNo: entry.getValue().bankServers.keySet())
                      {
                        if(!entry.getValue().failedServers.contains(portNo))
                        {                       
                          if (entry.getValue().head == portNo)
                             entry.getValue().head = responseMessage.getSuccPortNo();
                          else if (entry.getValue().tail == portNo)
                             entry.getValue().tail = responseMessage.getPredPortNo();

                           LOGGER.info("FailureHandler :: Removing Failed Server " +portNo);
                          // System.out.println("removing element  "+portNo);
                          entry.getValue().bankServers.remove(portNo);
                          break;
                        }
                      }
                    }
                    entry.getValue().chainLength -= size - entry.getValue().failedServers.size();
                    entry.getValue().failedServers.clear();
              } 
          }
          catch (Exception e) 
          {
            LOGGER.severe(" FailureHandler Exception: " + e); 
          }
       } 
        
  }
    public MasterMessage informOtherServers(Map.Entry<String,BankChain> entry)
    { 

      LOGGER.info("FailureHandler :: informOtherServers() ");
      MasterMessage responseMessage = null;
      MasterMessage temp = null;
      
      String name = entry.getKey();
      BankChain bankChain = entry.getValue();
      
      ObjectOutputStream out = null;
      RequestReply response = null;
     
      int succPortNo = 0, predPortNo = 0;
      
      for(int portNo: bankChain.bankServers.keySet())
      {
        if( !bankChain.failedServers.contains(portNo))
        {
          // System.out.println("portNo"+portNo);

          responseMessage = getServerDetails(bankChain.bankServers.get(portNo), portNo,name,configMap);
          succPortNo = responseMessage.getSuccPortNo();
          predPortNo = responseMessage.getPredPortNo();
          
          while(succPortNo!=0)
            if(!bankChain.failedServers.contains(succPortNo))
            { 
              temp = getServerDetails(bankChain.bankServers.get(succPortNo), succPortNo,name,configMap);
              succPortNo = temp.getSuccPortNo();
              responseMessage.setSuccPortNo(succPortNo); 
              responseMessage.setSuccUdpPortNo(temp.getSuccUdpPortNo());
            }
            else
              break;
   
          while(predPortNo!=0)
            if(!bankChain.failedServers.contains(predPortNo))
            {
              temp = getServerDetails(bankChain.bankServers.get(predPortNo), predPortNo,name,configMap);
              predPortNo = temp.getPredPortNo();
              // System.out.println("FIrst predPortNo"+predPortNo);
              responseMessage.setPredPortNo(predPortNo);
              responseMessage.setPredUdpPortNo(temp.getPredUdpPortNo());
            }
            else
              break;
        // System.out.println("succPortNo"+succPortNo);
        // System.out.println("predPortNo"+predPortNo);
        

        LOGGER.info("FailureHandler :: failed port No: "      + portNo);
        LOGGER.info("FailureHandler :: successor No: "        + succPortNo);
        LOGGER.info("FailureHandler :: predecessor port No: " + predPortNo);
        
        try
        { 
            if(succPortNo > 0 && predPortNo > 0)
            {
                hanldeInternalServer(succPortNo,predPortNo,responseMessage, bankChain);
            }
            else
            {  
              if(succPortNo > 0)
              {
                // System.out.println("Creating output stream");
                //out = new ObjectOutputStream(bankChain.socketList.get(succPortNo).getOutputStream());
                
                out = bankChain.outStreamList.get(succPortNo);
                // System.out.println("Done Creating output stream");
                responseMessage.setMessage("updatepred");
                out.writeObject(responseMessage);
              }


              if (predPortNo > 0)
              {
                out = bankChain.outStreamList.get(predPortNo);
                // System.out.println("Done Creating output stream");
                
                responseMessage.setMessage("updatesucc");
                out.writeObject(responseMessage); 
              }
            }

            // System.out.println("head"+bankChain.head);
            // System.out.println("tail"+bankChain.tail);
            LOGGER.info("FailureHandler :: Current Head = " + bankChain.head +
                        ":: Current Tail = " + bankChain.tail);
            if (bankChain.head == portNo)
            {
              LOGGER.info("FailureHandler :: Sent New Head to clients : ");
              response  = new RequestReply();
              response.setHeadAddress(responseMessage.getSuccAddress());
              response.setHeadPortNo(responseMessage.getSuccUdpPortNo());
              System.out.println("new head"+responseMessage.getSuccUdpPortNo());
        
              response.setMessage("sethead");
              response.setSender("master"); 
              informClient(name, response);

              LOGGER.info("FailureHandler :: Sent New Head to clients : " + responseMessage.getSuccUdpPortNo()); 
            }
            else if (bankChain.tail == portNo)
            {
              LOGGER.info("FailureHandler :: Sent New Head to clients : ");
              response  = new RequestReply();
              response.setTailAddress(responseMessage.getPredAddress());
              response.setTailPortNo(responseMessage.getPredUdpPortNo());
              response.setMessage("settail");
              response.setSender("master"); 
              informClient(name, response);

              LOGGER.info("FailureHandler :: Sent New Tail to clients : " + responseMessage.getPredUdpPortNo());
            }

          }
          catch(Exception e){
            LOGGER.severe(" FailureHandler Exception: " + e);
            System.out.println(e);
          }
        }     
      }
      return responseMessage;       
    }

    public void hanldeInternalServer(int succPortNo,int predPortNo, MasterMessage responseMessage, BankChain bankChain)
    {

      LOGGER.info("FailureHandler :: hanldeInternalServer() ");
      ObjectOutputStream out = null;
      ObjectInputStream in = null;
      
        try
        {
          in =  bankChain.inStreamList.get(succPortNo);
          out = bankChain.outStreamList.get(succPortNo);

          //Inform Successor
          LOGGER.info("FailureHandler :: Informing Successor [ S+ ] at port = " + succPortNo);
          out = bankChain.outStreamList.get(succPortNo);
          responseMessage.setMessage("updatepred");
          out.writeObject(responseMessage);

          System.out.println("Before"+responseMessage.getSequenceNumber());

          System.out.println(in);
          responseMessage = (MasterMessage) in.readObject();

          System.out.println("After"+responseMessage.getSequenceNumber());

          LOGGER.info("FailureHandler :: Recieved from Successor [ S+ ] sequence Number =  " 
                                + responseMessage.getSequenceNumber());

        }
        catch(Exception e)
        {
          LOGGER.severe(" FailureHandler Successor Could not reply Exception: " + e);
          System.out.println(e);
        }
      //Inform Predecessor        
        try
        {
          
          LOGGER.info("FailureHandler :: Informing Predecessor [ S- ] at port = " + predPortNo + " with sn = " 
                          +responseMessage.getSequenceNumber());

          out = bankChain.outStreamList.get(predPortNo);
          responseMessage.setMessage("updatesucc");
          out.writeObject(responseMessage); 
        }
        catch(Exception e)
        {
          LOGGER.severe(" FailureHandler Predecessor Could not reply Exception: " + e);
          System.out.println(e);
        }
    }

    public void informClient(String name, RequestReply response)
    {

      DatagramPacket packet;
      byte []buf;

      try{
         // System.out.println("Client List" + clientList.get(name));
          LOGGER.info(" FailureHandler :: Sending client failed Notification Info:" + clientList.get(name));
                    
          for(ClientInfo client:clientList.get(name))
          {
            buf = new byte[512];
          
            ByteArrayOutputStream bStream = new ByteArrayOutputStream();
            ObjectOutput oo = new ObjectOutputStream(bStream); 
            oo.writeObject(response);
            oo.close();

            buf = bStream.toByteArray();
          
            InetAddress address =client.address;
            int port = client.portNo;
            packet = new DatagramPacket(buf, buf.length, address, port);
            socket.send(packet);
          }
      }
      catch(Exception e)
      {
        LOGGER.severe(" FailureHandler Exception: " + e);
        System.out.println("Exception"+e);
      }
    }
    
    public MasterMessage getServerDetails(String addr, int portNo, String bankName, Map<String, String> map){

      String key = getKeyFromValue(map, String.valueOf(portNo));
      MasterMessage reply = new MasterMessage();
      String []rval = key.split("_");

      System.out.println(key);
      for (String r :rval)
        System.out.println(r);

      int temp = Integer.parseInt(rval[4]);
      int succ_val = temp+1;
      int pred_val = temp - 1;
      String tempPort = rval[0] + "_" + rval[1] + "_" + rval[2] + "_" + rval[3];
      String tempUPort = rval[0]+"_UDP_" + rval[2] + "_" + rval[3];
      String tempAddr = getKeyFromValue(map, bankName).split("_")[0].trim()+ "_HOST_ADDRESS_";
      String succ =  tempPort + "_" + succ_val;
      String pred = tempPort + "_" + pred_val;

      String usucc =  tempUPort + "_" + succ_val;
      String upred = tempUPort + "_" + pred_val;

      reply.setSuccAddress(map.get(succ));
      if(map.get(pred) != null)
      {
        reply.setPredAddress(map.get(tempAddr+pred_val));
        reply.setPredPortNo(Integer.parseInt(map.get(pred)));
        reply.setPredUdpPortNo(Integer.parseInt(map.get(upred)));
      }
      else  
      {
        reply.setPredAddress("");
        reply.setPredPortNo(0);
        reply.setPredUdpPortNo(0);
      }
      if(map.get(succ) != null)
      {
        reply.setSuccAddress(map.get(tempAddr+succ_val));
        reply.setSuccPortNo(Integer.parseInt(map.get(succ)));

        reply.setSuccUdpPortNo(Integer.parseInt(map.get(usucc)));
      }
      else  
      {
        reply.setSuccAddress("");
        reply.setSuccPortNo(0);
        reply.setSuccUdpPortNo(0);
      }
      reply.setHostPortNo(portNo);
      reply.setHostAddress(addr);
      return reply;

    }

    private static String getKeyFromValue(Map<String, String> hm, String value) {
        for (String o : hm.keySet()) {
            if (hm.get(o).equals(value)) {
              return o;
          }
      }
      return null;
    }

}

