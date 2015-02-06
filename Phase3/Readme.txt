/*
 *  PHASE 3: FAULT-TOLERANT SERVICE
 *  
 *	TEAM MEMBERS: 
 *  GAUTHAM REDDY KUNTA ID: 109596312
 *  NAFEES AHMED ABDUL ID: 109595182
 *
 */

PROGRAMMING LANGUAGE :  Dist Algo [1.0.0b8 distribution] and JAVA (1.7.0_67)

INSTRUCTIONS:

COMPILING THE CODE in DIST ALOG:

The programming language used is Dist Algo . The program can be executed by either by installing dist Algo or directly using by dist algo compiler ‘dar’
Once setup is ready , use below command to execute my code.

Ways of executing my code:
1)python3 -m da chainreplication.da [INPUT CONFIG FILE]
2)<DAROOT>/bin/dar chainreplication.da [INPUT CONFIG FILE]

COMPILING THE CODE IN JAVA:
	
	In the cmdline or terminal we need to execute the following commands

Client-Side: 

	javac CRClient.java
	javac ClientThread.java
	javac RequestReply.java
	javac Outcome.java

Server-Side: 

	javac CRServer.java
	javac CRClientServerThread.java
	javac CRServerMaster.java
	javac ServerMessage.java
	javac RequestReply.java
	javac Outcome.java

Mater-Side:
	
	javac CRMaster.java
	javac CRMasterClientThread.java
	javac CRMasterServerThread.java
	javac FailureHandler.java
	javac MasterMessage.java


RUNNING THE CODE:
	
Client-Side:
	For running the clients for a bank we follow the following. For each bank we need to execute the below command with the specific bank name.

	java CRClient <CONFIG FILE> <BANK_NAME>

Server-Side:
	For each bank in the bank chain we execute the following command.
	
	java CRServer <CONFIG FILE> <BANK_NAME>

Master-Side:
	For each bank in the bank chain we execute the following command.
	
	java CRMaster <CONFIG FILE>


FILES:

Dist Algo:

One main file : chainreplication.da
Config files are specified in ~/config folder 
and respective log files are generated into log folder of current working directory.

Java:
CLIENT-SIDE FILES: CRClient.java,ClientThread.java
SERVER-SIDE FILES: CRServer.java,CRClientServerThread.java,ServerMessage.java,CRServerMaster.java
MASTER-SIDE FILES: CRMaster.java, CRMasterServerThread.java, CRMasterClientThread.java,FailureHandler.java
Common files:RequestReply.java,Outcome.java, MasterMessage.java



BUGS AND LIMITATIONS: In Dist Algo , Simulation of specific test cases involve handling specific timing issues and these timings depends on various external parameters like CPU computation speed and RAM capabilities . At specific times , there may be need to execute same/slightly different configuration file(Not our program file) to simulate specific test cases(especially involved with failure of servers) . Being said that regardless of timing sensitivity the algorithm works efficiently and handles all cases appropriately.


Contributions:
Both my team mate and I worked on this project implementation in two languages. Our collaboration and participation is equally balanced.

LANGUAGE COMPARISON:
Our chainreplication project is accomplished using two languages i.e Dist Algo and Java . Here are few comparisions as per our best
knowledge and basing on our experience in building up this project in these languages.
 
 
 Table of comparision based on different aspects           
----------------------------------------------------------------------------------------
				
Code Size:-				Dist Algo:650 lines(excluding comments and blank spaces)
						Java:2500 lines
				
				
				
Time and effort:- 		Relatively coding in java took more time than compared to Dist Algo 
				
Debugging:-				Debugging in both languages is a bit difficult, as this being a multi-thread/processes application
						and to simulate specific instances of failures, sensitive timing factores has to be considered.
				
				
Readability:-			Readability of code is maintained well in both languages . In general readability of the code 
						mostly depends on programmer and not the programming language.
				
				
Similarity with	 pseudo code:-	Most of the times our code follows the initial pseudo code structure with very few exceptions. 
				
                
Strengths of language:- Dist Algo: More work can be done in few lines of code , Sophisticated buildin methods and types
						(because it is build on top of python) . Processes can be created easily . Multi-processing programming is quite comfortable. 

						Java:Very Robust and highly used language in the market , which implies lot of support from huge community of users. Good number of predefined classes and very flexible interfaces. Low level details are exposed , which helps to redesign whenever required. Excellent exception-handling mechanisms.         

Weakness of language:-	Dist Algo:Due to high level of Abstraction it is very difficult to understand the lower level 
						details which prevent us to visualize the way the language works. Not so intuitive error handling and error	messages are quite difficult to interpret. Due to less Documentation, it is not very flexible to understand and learn all features of Dist Algo

						Java: Vast amount of code has to be written to accomplish even the simplest task.It is not 
						dynamically typed language unlike python which is dynamically typed language.

Suggestions:			Detail documentation which help us to understand how Dist Algo is build and which can expose lower
						level details regarding the language could be beneficial .							   				
				
				
				
				



 

