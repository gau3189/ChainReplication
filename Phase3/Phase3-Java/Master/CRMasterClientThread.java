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
import java.util.HashMap;
import java.util.Map;



public class CRMasterClientThread extends Thread {

  private DatagramSocket socket = null;
  private Map<String, List<ClientInfo>> clientList;
  private Map<String,BankChain> bankList;
  private Map<String, String> map;
  private final Logger LOGGER ;

  public CRMasterClientThread(DatagramSocket socket, Map<String, String> map, Map<String,List<ClientInfo>> list,Map<String,BankChain> bankList,Logger LOGGER)  
  {
        super("CRMasterClientThread");
        System.out.println("In CRMasterClientThread");
        this.socket = socket;
        this.clientList = list;
        this.bankList = bankList;
        this.map = map;
        this.LOGGER = LOGGER;
  }


/*
  After every T sec check for any failures occured or not and update the client accordingly
*/
  public void run()
  {
            ClientInfo newClient = null;
            DatagramPacket packet;
            byte[] buf ;
            ByteArrayInputStream byteStream = null;
            ObjectInputStream is = null;
            RequestReply clientMessage = null, response = null;
            String bank = null;
            int banklen = 0;
            while (true)
            {
                /* receive request */
                try
                {
                    buf = new byte[700];
                    packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);
                    int byteCount = packet.getLength();                  
                    byteStream = new ByteArrayInputStream(packet.getData());
                    is = new ObjectInputStream(new BufferedInputStream(byteStream));
                    clientMessage = (RequestReply) is.readObject();
                    is.close();
                    
                    LOGGER.info("CRMasterClientThread  :: Recieved Request from " + clientMessage.getSender());

                    String key = clientMessage.getBankName().toUpperCase();
                    if (clientMessage.getSender().toLowerCase().equals("server"))
                    {
                        int serverPortNo = clientMessage.getHostPortNo();
                        String serverAddress = clientMessage.getHostAddress();

                        if(bankList.containsKey(key))
                          if(!bankList.get(key).failedServers.contains(serverPortNo))
                              bankList.get(key).failedServers.add(serverPortNo);

                    }
                    else
                    {
                      LOGGER.info("CRMasterClientThread  :: Client Detail Message");   
                        
                      if(clientList.containsKey(key))
                      {
                        newClient = new ClientInfo(packet.getAddress(),packet.getPort());
                        if (!clientList.get(key).contains(newClient))
                          clientList.get(key).add(newClient);

                        bank = getKeyFromValue(map, key).split("_")[0].trim();
                      }

                      banklen = Integer.parseInt(map.get(bank+"_LENGTH"));

                      response  = new RequestReply();

                      response.setHeadAddress(map.get(bank+"_HOST_ADDRESS_1"));
                      response.setTailAddress(map.get(bank+"_HOST_ADDRESS_"+banklen));

                      response.setHeadPortNo(Integer.parseInt(map.get(bank+"_UDP_PORT_NUMBER_1")));
                      response.setTailPortNo(Integer.parseInt(map.get(bank+"_UDP_PORT_NUMBER_"+banklen)));
                      response.setSender("master");
                      
                      LOGGER.info("CRMasterClientThread  :: ResponseMessage = " + response.readClientMessage());

                      ByteArrayOutputStream bStream = new ByteArrayOutputStream();
                      ObjectOutput oo = new ObjectOutputStream(bStream); 
                      oo.writeObject(response);
                      oo.close();

                      buf = bStream.toByteArray();

                      InetAddress address = packet.getAddress();
                      int port = packet.getPort();
                      packet = new DatagramPacket(buf, buf.length, address, port);
                      socket.send(packet);
                    }
               }
               catch(Exception e)
               {
                  //LOGGER.severe(" CRMasterClientThread Exception: " + e);  
                    // System.out.println("Exception in Master Client Thread"+e);
               }
                
            }
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