/*
 *  PHASE 2: NON-FAULT-TOLERANT SERVICE
 *  
 *	TEAM MEMBERS: 
 *  GAUTHAM REDDY KUNTA ID: 109596312
 *  NAFEES AHMED ABDUL ID: 109595182
 *
 */

PROGRAMMING LANGUAGE :  Dist Algo [1.0.0b8 distribution] 

INSTRUCTIONS:

The programming language used is Dist Algo . The program can be executed by either by installing dist Algo or directly using by dist algo compiler ‘dar’
Once setup is ready , use below command to execute my code.

Ways of executing my code:
1)python3 -m da chainreplication.da [INPUT CONFIG FILE]
2)<DAROOT>/bin/dar chainreplication.da [INPUT CONFIG FILE]

Note that Input config file is mandatory

As the way dist Algo works differs when compared to other languages , more specifically lower level sockets creations and other lower level stuff are not considered.
Moreover as all process creations are done in a single file , My implementation consists of using a single file namely chainreplication.da which internally 
contains both client/server functionality.

All are Servers and Clients related to banks are different processes and generate individual log files , this is to provide convenience while debugging . 
To restrict unnecessary noise flowing through log files and console certain necessary settings were made.

Files:
One main file : chainreplication.da
Config files are specified in ~/config folder 
and respective log files are generated into current working directory.

Contributions:
Both my team mate and I worked on this project implementation in two languages(other language being java) . Our collaboration and participation is equally balanced.
This is to get familiar about distributed applications implementation in fundamentally two different language paradigms

Performance:
As of now this task is pending as failure cases are not in effect.



 

