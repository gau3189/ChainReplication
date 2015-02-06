/*
 *  PHASE 3: FAULT-TOLERANT SERVICE
 *  TEAM MEMBERS: 
 * 
 *  GAUTHAM REDDY KUNTA ID: 109596312
 *  NAFEES AHMED ABDUL ID: 109595182
 *
 *  CRMaster.java
 */

import java.io.*;
import java.net.*;
import java.util.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;
import java.util.HashMap;
import java.util.Map;


public class CRMaster {

	private String myAddress;
	private int tcpPortNo;
	private int udpPortNo;

  private Map<String, String> map;
  private Map<String,BankChain> bankList;
  private Map<String, List<ClientInfo>> clientList;

  private final static Logger LOGGER = Logger.getLogger(CRServer.class.getName());
  private static FileHandler fh;
       
   
   CRMaster()
   {
        this.myAddress = "127.0.0.1";
        this.map = new HashMap<String, String>();
        this.clientList = new HashMap<String, List<ClientInfo>>();
        this.bankList = new HashMap<String,BankChain>();
   }

	 public static void main(String[] args) throws IOException {
		
        CRMaster master = new CRMaster();
        master.map = master.readConfigurationFile(args[0]);

		    String path = "./Logs";    // path of the folder you want to create
        File folder=new File(path);
        boolean exist=folder.exists();
        if(!exist){
            folder.mkdirs();
        }else{
            System.out.println("folder already exist");
        }
        
        fh = new FileHandler(path + "/master@" + master.tcpPortNo + ".log");  
        LOGGER.addHandler(fh);
        LOGGER.setUseParentHandlers(false);
        LOGGER.setLevel(Level.FINE);
        SimpleFormatter formatter = new SimpleFormatter();  
        fh.setFormatter(formatter);  
		    
        
       

        LOGGER.config("master tcp Port = " + master.tcpPortNo);
        LOGGER.config("master udp Port = " + master.udpPortNo);
        LOGGER.config("Number of banks Chains  "+master.bankList.size());
        

        System.out.println(master.bankList.size());
    		System.out.println("master tcp Port = " + master.tcpPortNo);
    		System.out.println("master udp Port = " + master.udpPortNo);

        String inputLine, outputLine;
        DatagramSocket clientSocket = null;
        
        /*
            Creating DatagramSocket for listening to clients and spawn CRClientServerThread
        */
            
        try 
        { 
          clientSocket = new DatagramSocket(master.udpPortNo, InetAddress.getByName(master.myAddress));

          new FailureHandler(clientSocket, master.map, master.clientList, master.bankList, LOGGER).start();
          new CRMasterClientThread(clientSocket,master.map ,master.clientList,master.bankList, LOGGER).start();

        } 
        catch (Exception e) 
        {
            System.err.println("Could not listen on port " + master.udpPortNo);
            LOGGER.severe("Exception while listening at port " + master.udpPortNo + " Exception: " + e );
        }
      
        System.out.println("In Master");
        try (
            ServerSocket masterSocket = new ServerSocket(master.tcpPortNo,-1, InetAddress.getByName(master.myAddress));
            )
          {
            System.out.println("Created TCP port for listening servers at portNo = "+ master.tcpPortNo);
            while (true)
               new CRMasterServerThread(masterSocket.accept(),clientSocket,master.map,master.bankList,master.clientList, LOGGER).start();
          } 
          catch (Exception e) 
          {
              System.err.println("Exception = " + e);
              LOGGER.severe("Exception = " + e);
              System.exit(-1);
          }
  	}
 

	private Map<String, String> readConfigurationFile(String FileName) {
 
   	Map<String, String> map = new HashMap<String, String>();
   	String temp = "";
   	Boolean isBank = true;

    BankChain tempChain;
   	Path path = Paths.get(FileName);

    int icount = 1;

    try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)){
      String line = "";
      while ((line = reader.readLine()) != null ) {

      	if(!line.isEmpty()) 
      	{
      		if(isBank)
      		{
      			if (line.trim().toLowerCase().contains("<bank"))
	      		{
	      			temp = "Bank"+icount+"_";
	      			isBank = false;
	      		}
	      	}
	      	
          if (line.trim().toLowerCase().contains("</bank"))
          {
	      		icount++;
            clientList.put(map.get(temp+"NAME").toUpperCase(), new ArrayList<ClientInfo>());

            tempChain = new BankChain();
            tempChain.chainLength = Integer.parseInt(map.get(temp+"LENGTH"));
            for (int i = 1; i<= tempChain.chainLength; i++)
            {
              String value = map.get(temp+"HOST_ADDRESS_"+i);
              int key = Integer.parseInt(map.get(temp+"TCP_PORT_NUMBER_"+i));
              tempChain.bankServers.put(key,value);
            }
            bankList.put(map.get(temp+"NAME").toUpperCase(), tempChain);
            isBank = true;
          } 

      		String []val = line.split(":");
      		if (val.length == 2)
      				map.put(temp+(val[0].trim().toUpperCase()), val[1].trim());
      		
		  }    
	  }

   	for (Map.Entry<String,String> entry : map.entrySet())
    {
      	String key = entry.getKey();
      	String value = entry.getValue();
      	System.out.println("Key = " + key +" value = " + value );
        LOGGER.config("Key = " + key +" value = " + value );
     }    

     tcpPortNo = Integer.parseInt(map.get("MASTER_TCP_PORTNO"));
     udpPortNo = Integer.parseInt(map.get("MASTER_UDP_PORTNO"));
   }
   catch(Exception e){
    System.err.println("Exception = " + e);
    LOGGER.severe("Configuration Exception = " + e);
    System.exit(-1);
   }

   	return map;
 }
 
}

/*
        ClassName       :  BankChain
        Variables       :  bankName - represents bank name
                        :  bankServers - dicitionary  containing banks host address and portno.
                        :  chainLength - length of the bank chain
          
*/
class ClientInfo 
{
    InetAddress address;
    int portNo;

    ClientInfo(InetAddress addr, int portNo)
    {
        this.address = addr;
        this.portNo = portNo;
    }
      
    public String toString() {
        return "< Address = "+ address+ " , PortNo = "+ portNo +" >";
    }

    public boolean equals(Object other) {
    if (!(other instanceof ClientInfo)) {
        return false;
    }

    ClientInfo another = (ClientInfo) other;

    // Custom equality check here.
    return this.address.equals(another.address)
        && (this.portNo == another.portNo);
}

}

/*
        ClassName       :  BankChain
        Variables       :  bankName - represents bank name
                        :  bankServers - dicitionary  containing banks host address and portno.
                        :  chainLength - length of the bank chain
          
*/
class BankChain 
{
    int chainLength;
    Map <Integer,String> bankServers;
    List<Integer> failedServers;
    int head;
    int tail;
    Map <Integer,ObjectInputStream> inStreamList;
    Map <Integer,ObjectOutputStream> outStreamList;
   
    BankChain()
    {
        this.chainLength = 0;
        this.bankServers = new HashMap<Integer,String> ();
        this.failedServers = new ArrayList<Integer>();
        this.inStreamList = new HashMap<Integer,ObjectInputStream>();
        this.outStreamList = new HashMap <Integer,ObjectOutputStream>();
    }
     public String toString() {
        return "< bankLength = "+ chainLength + " , ChainList = "+ bankServers +" >";
    }
    public int getFailedServers()
    {
       return failedServers.size();
    }
}

