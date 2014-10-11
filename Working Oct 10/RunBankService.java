import java.io.IOException;
import java.lang.ProcessBuilder;
import java.io.*;

public class RunBankService {
    public static void main(String[] args) throws IOException {

	// Create ProcessBuilder.
    	 Process[] p = new Process[4];
    	  while(true)
    	  {
    	  	System.out.println("[1] Start Process");

    	  	System.out.println("[2] Stop Process");
    	  	System.out.println("[3] Exit");

    	  	BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    	  	int ch = Integer.parseInt(br.readLine());
    	  	switch(ch)
    	  	{

    	  		case 1:	
			    	for (int i=0;i <4;i++)
			    	{
			    		System.out.println(i);
			    		 p[i] = (new ProcessBuilder("java", "CRServer", "Bank.Config", "CITI")).start();
			    	}
			    		System.out.println("Started Services");
					
					break;

				case 2:	
					for (int i=0;i <4;i++)
			    	{
			    		p[i].destroy();
					}
					break;

				case 3: System.exit(1);
			}

		}
    }
}