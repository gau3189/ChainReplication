/*
 *  PHASE 3: FAULT-TOLERANT SERVICE
 *  TEAM MEMBERS: 
 * 
 *  GAUTHAM REDDY KUNTA ID: 109596312
 *  NAFEES AHMED ABDUL ID: 109595182
 *
 *
 *  CRMasterServerThread.java
 *
 */

import java.net.*;
import java.io.*;
import java.util.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.sql.Timestamp;
import java.util.Date;


public class CRMasterServerThread extends Thread {

  private DatagramSocket udpsocket = null;
  private Socket socket = null;

  private Map<String, String> configMap;
  private Map<String,BankChain> bankList;
  private Map<String, List<ClientInfo>> clientList;
  
  private int serverPortNo;
  private String serverAddress;
  private String bankName;
  private Boolean isExtending;
  private int waitTime;
  
  private final Logger LOGGER ;


  public CRMasterServerThread(Socket socket, DatagramSocket udpsocket ,Map<String, String> map, Map<String,BankChain> bankList,
                              Map<String,List<ClientInfo>> list,Logger LOGGER) 
    {
        super("CRMasterServerThread");
        
        System.out.println("In CRMasterServerThread ");
        
        this.socket = socket;
        this.udpsocket = udpsocket;
        this.configMap = map;
        this.bankList = bankList;
        this.clientList = list;
        this.isExtending = false;
        this.LOGGER = LOGGER;
        this.waitTime = 0;
    }

  public void run()
  {
    MasterMessage receivedMessage = null, responseMessage = null;
    
    ObjectInputStream inStream = null;
    ObjectOutputStream outStream = null;
    ByteArrayInputStream byteStream = null;
    ObjectOutputStream outS = null;
    
    DatagramPacket packet = null;
    byte []buf;
    
    try
    {
        inStream = new ObjectInputStream(socket.getInputStream());
        outStream = new ObjectOutputStream(socket.getOutputStream());
        socket.setSoTimeout(3000);
        int count = 0;

        while(true)
        {
          try{
                receivedMessage = (MasterMessage) inStream.readObject();
                serverPortNo    = receivedMessage.getHostPortNo();
                serverAddress   = receivedMessage.getHostAddress();
                bankName        = receivedMessage.getBankName().toUpperCase();

                if(receivedMessage!= null)
                    LOGGER.info("Received Message - "+receivedMessage.getMessage());

                if(receivedMessage.getMessage().toLowerCase().equals("details"))
                { 
                  /*  Filling up the server instream and outstream info for failure notification by FailureHandler*/

                  LOGGER.info(" CRMasterServerThread :: Request from Server for successor and predecessor details");  

                  responseMessage = getServerDetails(serverAddress, serverPortNo,receivedMessage.getBankName(),configMap);

                  if(bankList.containsKey(bankName))
                  {
                    if(!bankList.get(bankName).inStreamList.containsKey(serverPortNo))
                      bankList.get(bankName).inStreamList.put( serverPortNo,inStream);

                    if(!bankList.get(bankName).outStreamList.containsKey(serverPortNo))
                       bankList.get(bankName).outStreamList.put( serverPortNo,outStream);

                    if (!bankList.get(bankName).bankServers.containsKey(serverPortNo)) 
                    {
                        
                        LOGGER.info("CRMasterServerThread :: Extending New Server to the bank chain of " + bankName);
                        System.out.println("Extending new server");
                        outS = bankList.get(bankName).outStreamList.get( bankList.get(bankName).tail);
                        receivedMessage.setSender("master");
                        receivedMessage.setMessage("newtail");
                        responseMessage.setMessage("extending");
                        isExtending = true;
                    }
                    else
                    {
                      if(responseMessage.getPredPortNo() == 0)
                        bankList.get(bankName).head = serverPortNo;
                      
                      if(responseMessage.getSuccPortNo() == 0)
                        bankList.get(bankName).tail = serverPortNo;
                    }  
                  }

                  outStream.writeObject(responseMessage);
                  if(outS != null)
                    outS.writeObject(receivedMessage);

                  if(!isExtending)
                     break;
                }
                else if(receivedMessage.getMessage().toLowerCase().equals("retry"))
                {
                  LOGGER.info("retry new chain request");
                  Thread.sleep(1000);
                  if (!bankList.get(bankName).bankServers.containsKey(serverPortNo)) 
                    {
                        
                        LOGGER.info("CRMasterServerThread :: Extending New Server to the bank chain of " + bankName);
                        System.out.println("Extending new server");
                        outS = bankList.get(bankName).outStreamList.get( bankList.get(bankName).tail);

                        LOGGER.info("Current tail is: "+bankList.get(bankName).tail);
                        receivedMessage.setSender("master");
                        receivedMessage.setMessage("newtail");
                        responseMessage.setMessage("extending");
                        outS.writeObject(receivedMessage);
                        isExtending = true;
                    }

                }
                else if(receivedMessage.getMessage().toLowerCase().equals("done"))
                {
                    LOGGER.info("CRMasterServerThread :: Done with Extending new server for bank" + bankName);
                    System.out.println("Extending new server has been completed");

                    responseMessage = getServerDetails(receivedMessage.getHostAddress(), receivedMessage.getHostPortNo(),receivedMessage.getBankName(),configMap);
                  
                    if (!bankList.get(bankName).bankServers.containsKey(serverPortNo))
                    {
                      bankList.get(bankName).bankServers.put(receivedMessage.getHostPortNo(),receivedMessage.getHostAddress());
                      bankList.get(bankName).chainLength++;
                    }

                    LOGGER.info("CRMasterServerThread :: New Server Chain"+bankList.get(bankName).bankServers);
                    System.out.println("new server chain"+bankList.get(bankName).bankServers);

                    if(!bankList.get(bankName).inStreamList.containsKey(serverPortNo))
                    {
                      bankList.get(bankName).inStreamList.put( serverPortNo,inStream);
                     
                      if(responseMessage.getPredPortNo() == 0)
                        bankList.get(bankName).head = serverPortNo;
                      
                      if(responseMessage.getSuccPortNo() == 0)
                        bankList.get(bankName).tail = serverPortNo;
                    }
                    if(!bankList.get(bankName).outStreamList.containsKey(serverPortNo))
                       bankList.get(bankName).outStreamList.put( serverPortNo,outStream);

                    LOGGER.info(" CRMasterServerThread :: Sending client the New Tail Info:" + serverPortNo);
                    System.out.println(" CRMasterServerThread sending client new tail with portno "+ serverPortNo); 
                    RequestReply response = new RequestReply();
                    response.setTailAddress(serverAddress);
                    response.setTailPortNo(receivedMessage.getHostUdpPortNo());
                    response.setMessage("settail");
                    response.setSender("master");
                    informClient(bankName,response);
                    isExtending = false;
                    break;
                }
              }
              catch(Exception e){
                LOGGER.info("Wait time for server" + this.waitTime);
                if (isExtending)
                {
                    if (count > waitTime)
                    {
                      outS = bankList.get(bankName).outStreamList.get( bankList.get(bankName).tail);

                      LOGGER.info("Current tail is: "+bankList.get(bankName).tail);
                      receivedMessage = new MasterMessage();
                      receivedMessage.setSender("master");
                      receivedMessage.setMessage("reset");
                      receivedMessage.setSuccAddress("");
                      receivedMessage.setSuccPortNo(0);
                      receivedMessage.setSuccUdpPortNo(0);
                      outS.writeObject(receivedMessage);
                      isExtending = false;
                      break;
                    }
                    count++;
                  }
              }
        }

    }
    catch(Exception e){
      LOGGER.severe(" CRMasterServerThread Exception: " + e);
      System.out.println("Exception"+e);
    }

  }

  public void informClient(String name, RequestReply response)
    {

      DatagramPacket packet;
      byte []buf;

      try{
          System.out.println("Client List" + clientList.get(name));
          LOGGER.info(" Client List: " + clientList.get(name));
          for(ClientInfo client:clientList.get(name))
          {
            buf = new byte[700];
          
            ByteArrayOutputStream bStream = new ByteArrayOutputStream();
            ObjectOutput oo = new ObjectOutputStream(bStream); 
            oo.writeObject(response);
            oo.close();

            buf = bStream.toByteArray();
          
            InetAddress address =client.address;
            int port = client.portNo;
            packet = new DatagramPacket(buf, buf.length, address, port);
            udpsocket.send(packet);
          }
      }
      catch(Exception e)
      {
       // LOGGER.severe(" CRMasterServerThread Exception: " + e);
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

      int length = Integer.parseInt(map.get(getKeyFromValue(map, bankName).split("_")[0].trim()+ "_LENGTH"));
      System.out.println("length"+length);
      
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

      this.waitTime = Integer.parseInt(map.get("MASTER_WAIT_TIME"));
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
      if(map.get(succ) != null && succ_val <= length)
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

