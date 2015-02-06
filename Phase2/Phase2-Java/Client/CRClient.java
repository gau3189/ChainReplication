/*
 *  PHASE 2: NON-FAULT-TOLERANT SERVICE
 *  TEAM MEMBERS: 
 * 
 *  GAUTHAM REDDY KUNTA ID: 109596312
 *  NAFEES AHMED ABDUL ID: 109595182
 *
 *  CRClient.java
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

 
public class CRClient {

    /*
        Each data member is a configuration information for the client present in configuration file.
    */

    private String headAddress;
    private String masterAddress;
    private String tailAddress;

    private int masterPortNo;
    private int headPortNo;
    private int tailPortNo;

    private int clientCount;
    private int clientWaitTime;
    private int clientResend;
    private int chainLength;

    private String bankName;
    private String configFile;

    /*
        Contains the request for each client incase of itemized request else contains the single random 
        request information for generating random requests which common for each client.
    */
    private Map<String, List<String>> requestList;
    private final static Logger LOGGER = Logger.getLogger(CRClient.class.getName());
    private static FileHandler fh;

    public CRClient(String configFile, String bankname)
    {
        this.bankName = bankname;
        this.configFile = configFile;
        this.requestList = new HashMap<String,List<String>>();
    }

    public static void main(String[] args) throws IOException {
         
            if (args.length != 2) {
                System.err.println(
                    "Usage: java CRClient <config file> <bank name>");
                System.exit(1);
            }
            
            CRClient client = new CRClient(args[0], args[1]);
                
            try
            {
                int result = client.init();
                
                fh = new FileHandler("./Logs/client@"+ client.bankName + ".log");  
                LOGGER.addHandler(fh);
                LOGGER.setUseParentHandlers(false);
                LOGGER.setLevel(Level.FINE);
                SimpleFormatter formatter = new SimpleFormatter();  
                fh.setFormatter(formatter);  
        
                LOGGER.info("################################ < Intial Configuration > ###############################");

                LOGGER.config("master addr = "+ client.masterAddress);
                LOGGER.config("master port = "+ client.masterPortNo);
                LOGGER.config("head address = "+ client.headAddress);
                LOGGER.config("head port = "+ client.headPortNo);
                LOGGER.config("tail address = "+ client.tailAddress);  
                LOGGER.config("tail port = "+ client.tailPortNo);
                LOGGER.config("client Count = "+ client.clientCount);  
                LOGGER.config("client Wait Time  = "+ client.clientWaitTime);
                LOGGER.config("client Resends   = "+ client.clientResend);
                LOGGER.config("client requests  = "+ client.requestList);

                LOGGER.info("################################ </ Intial Configuration >###############################");
                

               /* Debugging Messages 
                    System.out.println("master addr = "+ client.masterAddress);
                    System.out.println("master port = "+ client.masterPortNo);
                    System.out.println("head address = "+ client.headAddress);
                    System.out.println("head port = "+ client.headPortNo);

                    System.out.println("tail address = "+ client.tailAddress);
                    System.out.println("tail port = "+ client.tailPortNo);
                    System.out.println("client Count = "+ client.clientCount);  
                    System.out.println("client Request Info  = "+ client.clientRequestInfo);
                    System.out.println("client Wait Time  = "+ client.clientWaitTime);
                    System.out.println("client Resends   = "+ client.clientResend);
                    System.out.println("requests  = "+ client.requests);
                    System.out.println("size="+ client.requestList.size());
                    
                */
                if(client.requestList.size() == 1)
                {
                    for (int i = 1; i <= client.clientCount; i++)
                        new ClientThread(client.masterAddress,client.headAddress,client.tailAddress,
                                        client.masterPortNo,client.headPortNo,client.tailPortNo,
                                        client.requestList.get("REQUEST_INFO"),client.bankName,client.clientResend,
                                        client.clientWaitTime, i, LOGGER,true).start();
                }
                else
                {
                    for (int i = 1; i <= client.clientCount; i++)
                    {
                        String key = "REQUEST_" + i;
                        new ClientThread(client.masterAddress,client.headAddress,client.tailAddress,
                                        client.masterPortNo,client.headPortNo,client.tailPortNo,
                                        client.requestList.get(key),client.bankName,client.clientResend,
                                        client.clientWaitTime, i, LOGGER,false).start();
                    }
                }
            }
            catch(Exception e)
            {
                System.out.println( "Exception" + e);
            }
       }

   /*
        Function        :  init
        Description     :  1. Read Configuration File to get the head and tail port number
                           2. Master IP address and port no
                           3. Get all client related information such as wait time, number of resend messages etc.

    */
    public int init()
    {   
        List<String> requests;        
        try
        {   
            System.out.println(configFile);
            Path path = Paths.get(configFile);
            BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
            String line = null;
            int count = 0;
            Boolean isCorrectBank = false;
            while ((line = reader.readLine()) != null ) {
                if(!line.isEmpty()) 
                {
                    if (count == 3 && !isCorrectBank)
                        break;
                    
                    if (line.trim().toLowerCase().contains("</bank"))
                        isCorrectBank = false;
                   
                    String []val = line.split(":");
                    if (val.length == 2)
                    {
                        if (val[0].trim().equals("MASTER_IP_ADDRESS"))
                        {
                           masterAddress = val[1].trim();
                           count++;
                        }
                        if (val[0].trim().equals("MASTER_UDP_PORTNO"))
                        {
                            masterPortNo = Integer.parseInt(val[1].trim());
                            count++;
                        }

                        if (!isCorrectBank)
                        {
                            if (val[1].trim().equals(bankName))
                            {   
                                isCorrectBank = true;
                                count++;
                            }
                            else
                                isCorrectBank = false;
                            
                        }
                        if (isCorrectBank && val[0].trim().equals("CLIENTS"))
                            clientCount = Integer.parseInt(val[1].trim());
                         

                        if (isCorrectBank && val[0].trim().equals("LENGTH"))
                            chainLength= Integer.parseInt(val[1].trim());
                        
                        if (isCorrectBank && val[0].trim().equals("CLIENT_WAIT_TIME") )
                            clientWaitTime = Integer.parseInt(val[1].trim());

                        if (isCorrectBank && val[0].trim().equals("CLIENT_RESEND") )
                            clientResend = Integer.parseInt(val[1].trim());
                        
                        if (isCorrectBank && val[0].trim().equals("HOST_ADDRESS_1") )
                            headAddress = val[1].trim();
                        
                        if (isCorrectBank && val[0].trim().equals("UDP_PORT_NUMBER_1") )
                            headPortNo = Integer.parseInt(val[1].trim());
                         

                        if (isCorrectBank && val[0].trim().equals("HOST_ADDRESS_"+chainLength) )
                            tailAddress = val[1].trim();
                        
                        if (isCorrectBank && val[0].trim().equals("UDP_PORT_NUMBER_"+chainLength) )
                            tailPortNo = Integer.parseInt(val[1].trim());
                         

                        if (isCorrectBank && val[0].trim().contains("REQUEST_"))
                        {
                            requests = new ArrayList<String>();
                            requests.add(val[1].trim());
                            if (requestList.containsKey(val[0].trim()))
                            {
                                (requestList.get(val[0].trim())).add(val[1].trim());
                            }
                            else
                            {
                                requestList.put(val[0].trim(),requests);
                            }
                            
                          
                        }
                        
                    } 
                }    
            }
        }
        catch(Exception e)
        {
            System.out.println("Exception = " + e);
            return 0;
        }
        
        return 1;
    }
}