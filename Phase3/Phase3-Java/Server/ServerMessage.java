/*
 *  PHASE 3: FAULT-TOLERANT SERVICE
 *  TEAM MEMBERS: 
 * 
 *  GAUTHAM REDDY KUNTA ID: 109596312
 *  NAFEES AHMED ABDUL ID: 109595182
 *
 *
 *  ServerMessage.java
 *
 */

import java.io.Serializable;

import java.io.*;
import java.net.*;
import java.util.*;

public class ServerMessage implements Serializable {
    
    private static final long serialVersionUID = 5950169519310163575L;
    private String operation;
    private String accountNumber;
    private String hostAddress;
    private int portNo;
    private int sequenceNo;
    private String reqID;
    private float balance;
    private float amount;
    private Outcome outcome;
    private String message;
    List <String> processedTrans;


    public List <String> getProcessedTrans() {
        return processedTrans;
    }

    public void setProcessedTrans(List <String> processedTrans) {
           this.processedTrans = processedTrans;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
           this.message = message;
    }

    public String getReqID() {
        return reqID;
    }

    public void setReqID(String id) {
           this.reqID = id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }
    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public int getPortNumber() {
        return portNo;
    }
    public void setPortNumber(int portNo) {
        this.portNo = portNo;
    }

    public int getSequenceNumber() {
        return sequenceNo;
    }
    public void setSequenceNumber(int seqNo) {
        this.sequenceNo = seqNo;
    }

    public String getHostAddress() {
        return hostAddress;
    }
    public void setHostAddress(String hostAddress) {
        this.hostAddress = hostAddress;
    }
    
    public float getBalance() {
        return balance;
    }
    
    public void setBalance(float balance) {
           this.balance = balance;
    }

    public float getAmount() {
        return amount;
    }
    
    public void setAmount(float amount) {
           this.amount = amount;
    }

    public String getOperation() {
        return operation;
    }
    
    public void setOperation(String operation) {
           this.operation = operation;
    }

    public Outcome getOutcome() {
        return outcome;
    }
    
    public void setOutcome(Outcome outcome) {
           this.outcome = outcome;
    }

    public boolean equals(Object o) 
    {
        return true;
    }
    public int hashCode() {
        return 234;
    }
    public String toString() {
        return "operation = " + operation + " ; reqID = "+ reqID+ " ; balance = " + balance + " ; outcome = " + outcome +" ; SEQUENCE NO = " + sequenceNo ;
    }
}
