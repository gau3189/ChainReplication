/*
 *  PHASE 2: NON-FAULT-TOLERANT SERVICE
 *  
 *	TEAM MEMBERS: 
 *  GAUTHAM REDDY KUNTA ID: 109596312
 *  NAFEES AHMED ABDUL ID: 109595182
 *
 */

PROGRAMMING LANGUAGE :  JAVA (1.7.0_67)

CLIENT-SIDE FILES:
	1. CRClient.java

		Reads the configuration file for the specific bank and gets all the required information for client
		such as number of clients, head and tail address and port numbers, client random request information or itemized requests for specific
		client,client waiting time, number of resends for client.

		Spawns clients thread equivalent to number of clients for given bank with respective itemized request or random request.
		
	2. ClientThread.java

		This class is equivalent to client sending and receiveing requests from server. It extends thread class and CRClient class spawns
		this class for each client.

SERVER-SIDE FILES:
	1. CRServer.java
		
		Reads the configuration file for the specific bank and gets all the required information for server
		such as tcp and udp port number, successor address and tco port number,server startup and life time.

		It maintains the account information for all the clients account numbers.

		It spawns CRClientServerThread to listen at a give udp port for client requests.

		It creates a tcp socket for listening to server - server update requests. For any updates it changes its state with the
		given update and its propogates to next server i.e its successor in the chain. If its the tail it sends reply to client.

	2. CRClientServerThread.java

		It listens at a udp port for client request and once it gets the update request it performs the specific operation from client and 
		propogates the update to its successor server through tcp communication. 

		If its a query request directly reply back to client.

	3. ServerMessage.java

		It contains the information of the updates that propagates in the chain i.e. for communication between servers.
	
COMMON FILES FOR BOTH SERVER & CLIENT:	
	1. RequestReply.java 

		Common class for both server and client to send request and recieve reply during client-server communucation.
		It contains information required for sending and recieving request such as reqID,bankName,operation, 
		accountNumber,balance,amount,Outcome.

	2. Outcome.java

		Contains enum for status of the request i.e processed, insufficient funds, inconsistent with history.

COMPILING THE CODE:
	
	In the cmdline or terminal we need to execute the following commands

Client-Side: 

	javac CRClient.java
	javac ClientThread.java
	javac RequestReply.java
	javac Outcome.java

Server-Side: 

	javac CRServer.java
	javac CRClientServerThread.java
	javac ServerMessage.java
	javac RequestReply.java
	javac Outcome.java

RUNNING THE CODE:
	
Client-Side:
	For running the clients for a bank we follow the following. For each bank we need to execute the below command with the specific bank name.

	java CRClient <CONFIG FILE> <BANK_NAME>

Server-Side:
	For each bank in the bank chain we execute the following command.
	
	java CRServer <CONFIG FILE> <BANK_NAME>


	<CONFIG FILE> - contains the information for both clients and servers for each bank. It contains details for all the banks.
				  - It represents a test case and this file is must be same for both server and client.

    <BANK_NAME> - name of the bank. It must be present in the CONFIG_FILE.

OTHER COMMENTS:
	
	Currently, we need to manually run the each server in the chain for a specific bank. I have created a script which takes the 
	<CONFIG FILE> & <BANK_NAME>  and creates process for each bank in the bank chain. But there is some problem in mac OS X because of
	which it unable to create multiple process automatically.

	
