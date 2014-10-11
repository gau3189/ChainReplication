import java.io.*;
import java.net.*;
import java.util.*;
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

 
public class CClient {

    private String headAddress;
    private String masterAddress;
    private String tailAddress;

    private int masterPortNo;
    private int headPortNo;
    private int tailPortNo;

    private int clientCount;
    private int clientWaitTime;
    private int chainLength;

    private String clientRequestInfo;
    private String bankName;
    private String configFile;

    private final static Logger LOGGER = Logger.getLogger(CClient.class.getName());
    private static FileHandler fh;

    public CClient(String configFile, String bankname)
    {
        this.bankName = bankname;
        this.configFile = configFile;
    }

    public static void main(String[] args) throws IOException {
         
            if (args.length != 2) {
                System.err.println(
                    "Usage: java CClient <config file> <bank name>");
                System.exit(1);
            }
            
            CClient client = new CClient(args[0], args[1]);
                
            try
            {
                int result = client.init();
                
                fh = new FileHandler("./Logs/client@"+ client.bankName + ".log",true);  
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
                LOGGER.config("client Request Info  = "+ client.clientRequestInfo);
                LOGGER.config("client Wait Time  = "+ client.clientWaitTime);


                LOGGER.info("################################ </ Intial Configuration >###############################");
                

                System.out.println("master addr = "+ client.masterAddress);
                System.out.println("master port = "+ client.masterPortNo);
                System.out.println("head address = "+ client.headAddress);
                System.out.println("head port = "+ client.headPortNo);

                System.out.println("tail address = "+ client.tailAddress);
                System.out.println("tail port = "+ client.tailPortNo);
                System.out.println("client Count = "+ client.clientCount);  
                System.out.println("client Request Info  = "+ client.clientRequestInfo);
                System.out.println("client Wait Time  = "+ client.clientWaitTime);

                for (int i = 0; i < client.clientCount; i++)
                    new CClientThread(client.masterAddress,client.headAddress,client.tailAddress,
                                        client.masterPortNo,client.headPortNo,client.tailPortNo,
                                        client.clientRequestInfo,client.bankName,
                                        client.clientWaitTime, i, LOGGER).start();
                 
            }
            catch(Exception e)
            {
                System.out.println( "Exception" + e);
            }
       }

    /* Intialization Function*/
    public int init()
    {
        
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
                    if (count == 11)
                        break;

                    if (line.trim().equals("</Bank1>") || line.trim().equals("</Bank1>") || line.trim().equals("</Bank1>"))
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

                        if (val[1].trim().equals(bankName))
                        {   
                            isCorrectBank = true;
                            count++;
                        }
                        
                        if (isCorrectBank && val[0].trim().equals("CLIENTS"))
                        {

                            clientCount = Integer.parseInt(val[1].trim());
                            count++;
                        } 

                        if (isCorrectBank && val[0].trim().equals("LENGTH"))
                        {

                            chainLength= Integer.parseInt(val[1].trim());
                            count++;
                        } 

                        if (isCorrectBank && val[0].trim().equals("REQUEST_INFO") )
                        {
                            clientRequestInfo = val[1].trim();
                            count++;   
                        }  

                        if (isCorrectBank && val[0].trim().equals("CLIENT_RESEND") )
                        {
                            clientWaitTime = Integer.parseInt(val[1].trim());
                            count++;   
                        } 

                        if (isCorrectBank && val[0].trim().equals("HOST_ADDRESS_1") )
                        {
                            headAddress = val[1].trim();
                            count++;   
                        } 

                        if (isCorrectBank && val[0].trim().equals("UDP_PORT_NUMBER_1") )
                        {
                            headPortNo = Integer.parseInt(val[1].trim());
                            count++;   
                        } 

                        if (isCorrectBank && val[0].trim().equals("HOST_ADDRESS_"+chainLength) )
                        {
                            tailAddress = val[1].trim();
                            count++;   
                        } 

                        if (isCorrectBank && val[0].trim().equals("UDP_PORT_NUMBER_"+chainLength) )
                        {
                            tailPortNo = Integer.parseInt(val[1].trim());
                            count++;   
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