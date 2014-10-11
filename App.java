// import java.io.IOException;
// import java.io.InputStream;
// import java.util.Enumeration;
// import java.util.Properties;
 import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import java.util.HashMap;
import java.util.Map;
public class CRMaster {
 
	private String myaddress;
	private int tcp_portNo;
	private int udp_portNo;

	public static void main(String[] args) {
		CRMaster master_server = new CRMaster();
		master_server.readConfigurationFile(args[0]);
		System.out.println("tcpPort = "+tcp_portNo);

		System.out.println("UDPPort = "+udp_portNo);

  	}
 
  private void readConfigurationFile(String FileName) {
 
	//Properties prop = new Properties();
	//InputStream input = null;
 
	/*try {
 
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

     tcp_portNo = Integer.parse.Int(map["MASTER_TCP_PORTNO"]);
     udp_portNo = Integer.parse.Int(map["MASTER_UDP_PORTNO"]);
   }
   catch(Exception e){}

 }
 
}