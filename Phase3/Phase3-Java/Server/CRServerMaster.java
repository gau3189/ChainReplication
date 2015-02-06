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


public class CRServerMaster extends Thread {

  private DatagramSocket udpSocket;
  private String masterAddress;
  private int masterPortNo;
  private String bankName;
  private String myAddress;
  private int myPortNo;
  private final Logger LOGGER ;

  private Socket socket;
  private Boolean isExtending;
  private Map<String,Account> accountList;
  private ObjectOutputStream out;
  public CRServerMaster(DatagramSocket udpSocket, String masterAddress,int masterportNo, String myAddress,int myportNo,  String bankName, Logger LOGGER) 
  {
      super("CRServerMaster");
      System.out.println("in CRServerMaster  Thread");
      this.bankName = bankName;
      this.masterAddress = masterAddress;
      this.masterPortNo = masterportNo;
      this.myAddress = myAddress;
      this.myPortNo = myportNo;
      this.udpSocket = udpSocket;
      this.isExtending =false;
      this.LOGGER = LOGGER;
  }

  public CRServerMaster(ObjectOutputStream out,  Map<String,Account> accountList,Boolean isExtending, String bankName,Logger LOGGER) 
  {
      super("CRServerMaster");
      System.out.println("CRServerMaster for extension");
      this.bankName = bankName;
      this.accountList = new HashMap<String,Account>(accountList);
      this.out = out;
      this.isExtending =isExtending;
      this.LOGGER = LOGGER;
  }

  public void run()
  {
      System.out.println("In RuN"); 
      RequestReply request =null;
      ByteArrayOutputStream bStream = null;
      ObjectOutput oo1 =null;
      InetAddress address = null;
      DatagramPacket packet = null;
      if(!isExtending)
      {
        
        while (true)
        {
          try{
              request = new RequestReply();
              request.setHostAddress(myAddress); 
              request.setHostPortNo(myPortNo);
              request.setSender("server");
              request.setMessage("Ping");
              request.setBankName(bankName);
              byte[] buf = new byte[700];
              byte[] rbuf = new byte[700];

              //System.out.println("Sending message to master"+request);
              bStream = new ByteArrayOutputStream();
              oo1 = new ObjectOutputStream(bStream); 
              oo1.writeObject(request);
              oo1.close();

              buf = bStream.toByteArray();

              address = InetAddress.getByName(masterAddress);
              packet = new DatagramPacket(buf, buf.length, address, masterPortNo);
              udpSocket.send(packet);
              Thread.sleep(1000);
          }
          catch(Exception e){}
        }
      }
      else
      {
          LOGGER.info("Sending to new tail");
          System.out.println("Sending to new tail");
          try
          {
            //ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            Account currAccount = null;  
            ServerMessage updateMessage;
            System.out.println("Sending to new tail:outStream" + out);
            for (String accountNumber : accountList.keySet())
            {
             updateMessage = new ServerMessage(); 
             currAccount =  accountList.get(accountNumber);
             System.out.println("Sending to new tail:accountNumber"+accountNumber);
             System.out.println("Sending to new tail:currAccount.processedTrans"+currAccount.processedTrans);
             updateMessage.setAccountNumber(accountNumber); 
             updateMessage.setBalance(currAccount.balance);
             updateMessage.setProcessedTrans(currAccount.processedTrans);
             updateMessage.setMessage("updatetrans");
             System.out.println("Sending to new tail:message"+updateMessage);
             out.writeObject(updateMessage);
            }
            updateMessage = new ServerMessage();
            updateMessage.setMessage("sync");
            LOGGER.info("Sending to new tail:sync message");
            System.out.println("Sending to new tail:sync message"+updateMessage);
            out.writeObject(updateMessage);
            
          }
          catch(Exception e){
            //System.out.println("Sending new tail Exception"+e);
          }
      }
  }
}