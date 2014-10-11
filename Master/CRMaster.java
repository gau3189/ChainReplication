
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
import java.util.HashMap;
import java.util.Map;


public class CRMaster {
 
	private String myaddress;
	private int tcp_portNo;
	private int udp_portNo;

	public static void main(String[] args) {
		
		CRMaster master_server = new CRMaster();
		Map<String, String> map = new HashMap<String, String>();

		map = master_server.readConfigurationFile(args[0]);
		
		System.out.println("tcpPort = " + master_server.tcp_portNo);
		System.out.println("UDPPort = " + master_server.udp_portNo);
		System.out.println("Master Listening at port no=" + master_server.tcp_portNo);

		try 
            { 
            	ServerSocket serverSocket = new ServerSocket(master_server.tcp_portNo);
                Socket sSocket = serverSocket.accept();
                PrintWriter temp_out = new PrintWriter(sSocket.getOutputStream(), true);
                BufferedReader temp_in = new BufferedReader(new InputStreamReader(sSocket.getInputStream()));
                System.out.println("Master Listening at port no="+ master_server.tcp_portNo);

                String inputLine, outputLine;
                byte[] buf = new byte[256];	
                String clientAddress = null;
                int portNo = 0;
                String result = null, request = null;
                while ((inputLine = temp_in.readLine()) != null)
                {
                    System.out.println("inputLine = "+ inputLine);
                    String []rval = inputLine.split(";");
                    clientAddress = rval[0];
                    portNo = Integer.parseInt(rval[1]);
                    request = rval[2];
					if (request.equals("GET_PRED_SUCC")){

						result = master_server.get_succ_pred(clientAddress, portNo, map);
						
					}
					else
						result = "notified";

                    System.out.println("clientAddress = "+ clientAddress);
                    System.out.println("client portNo = "+ portNo);
                    //master_server.get_succ_pred(temp_out, inputLine, map);
                    temp_out.println(result);
                    //break;

                }
                sSocket.close();

            } 
            catch (Exception e) 
            {
                System.err.println("Exception = " + e);
                System.exit(-1);
            }
  	}
 

  	public String get_succ_pred(String addr, int portNo, Map<String, String> map){

  		String key = getKeyFromValue(map, String.valueOf(portNo));
  		String reply = null;
  		String []rval = key.split("_");

  		System.out.println(key);
  		for (String r :rval)
  			System.out.println(r);

  		int temp = Integer.parseInt(rval[4]);
  		int succ_val = temp+1;
  		int pred_val = temp - 1;
  		String temp_key = rval[0] + "_" + rval[1] + "_" + rval[2] + "_" + rval[3];

  		String succ =  temp_key + "_" + succ_val;
  		String pred = temp_key + "_" + pred_val;

		System.out.println(map.get(succ) + "\n" + map.get(pred));
		System.out.println(succ + "\n" + pred);
		
  		// if (pred > 0)
  		// 	reply = map.get()

  		
  		//out.println(key);	
  		reply = map.get(succ) + ";" + map.get(pred);

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

	private Map<String, String>  readConfigurationFile(String FileName) {
 
	//Properties prop = new Properties();
	//InputStream input = null;
 
	/*
	try {
 
		String filename = "Bank1.Config";
		input = getClass().getClassLoader().getResourceAsStream(filename);
		if (input == null) {
			System.out.println("Sorry, unable to find " + filename);
			return;
		}
 
		prop.load(input);
 
		Enumeration<?> e = prop.propertyNames();
		while (e.hasMoreElements()) {
			String key = (String) e.nextElement();
			String value = prop.getProperty(key);
			System.out.println("Key : " + key + ", Value : " + value);
		}
 
	} catch (IOException ex) {
		ex.printStackTrace();
	} finally {
		if (input != null) {
			try {
				input.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
 	*/

 	Map<String, String> map = new HashMap<String, String>();
 	String temp = "";
 	Boolean isBank = true;
 	Path path = Paths.get(FileName);
    try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)){
      String line = "";
      while ((line = reader.readLine()) != null ) {

      	if(!line.isEmpty()) 
      	{
      		if(isBank)
      		{
      			System.out.println("Comparing isBank="+isBank);
	      		if (line.trim().equals("<BANK1>"))
	      		{
	      			temp = "Bank1_";
	      			isBank = false;
	      		}
	      		else if (line.trim().equals("<BANK2>"))
	      		{
	      			temp = "Bank2_";
	      			isBank = false;
	      		}
	      		else if (line.trim().equals("<BANK3>"))
	      		{
	      			temp = "Bank3_";
	      			isBank = false;
	      		}
	      		System.out.println("temp="+temp);
	      	}
	      	if (line.trim().equals("</BANK1>") || line.trim().equals("</BANK2>") || line.trim().equals("</BANK3>"))
	      		isBank = true;

      		String []val = line.split(" :");
      		if (val.length == 2)
      				map.put(temp+val[0].trim(),val[1].trim());
      		
		}    
	}
   	for (Map.Entry<String,String> entry : map.entrySet())
    {
      	String key = entry.getKey();
      	String value = entry.getValue();
      	System.out.println("Key = " + key +" value = " + value );
     }    

     tcp_portNo = Integer.parseInt(map.get("MASTER_TCP_PORTNO"));
     udp_portNo = Integer.parseInt(map.get("MASTER_UDP_PORTNO"));
   }
   catch(Exception e){}

   	return map;
 }
 
}