/*
 *  PHASE 3: FAULT-TOLERANT SERVICE
 *  TEAM MEMBERS: 
 * 
 *  GAUTHAM REDDY KUNTA ID: 109596312
 *  NAFEES AHMED ABDUL ID: 109595182
 *
 *
 *  RequestReply.java
 *
 */

import java.io.Serializable;
import java.sql.Timestamp;


public class RequestReply implements Serializable {

    private static final long serialVersionUID = 5950169519310163457L;
    											 
    private String reqID;
	private String bankName;
    private String operation;
	private String accountNumber;
	private float  balance;
	private float  amount;
	private Outcome outcome;
    private Timestamp messageTime;

    private int headPortNo;
    private int tailPortNo;
    private int hostPortNo;

    private String hostAddress;
    private String headAddress;
    private String tailAddress;
    private String sender;
    private String message;
    
 
    public int getHostPortNo() {
        return hostPortNo;
    }
    public void setHostPortNo(int portNo) {
        this.hostPortNo = portNo;
    }

    public String getHostAddress() {
        return hostAddress;
    }

    public void setHostAddress(String hostAddress) {
           this.hostAddress = hostAddress;
    }

    public String getHeadAddress() {
        return headAddress;
    }

    public void setHeadAddress(String headAddress) {
           this.headAddress = headAddress;
    }

    public String getTailAddress() {
        return tailAddress;
    }

    public void setTailAddress(String tailAddress) {
           this.tailAddress = tailAddress;
    }
    public int getHeadPortNo() {
        return headPortNo;
    }

    public void setHeadPortNo(int portNo) {
        this.headPortNo = portNo;
    }

    public int getTailPortNo() {
        return tailPortNo;
    }
    public void setTailPortNo(int portNo) {
        this.tailPortNo = portNo;
    }
    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
           this.sender = sender;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
           this.message = message;
    }
    public Timestamp getTimeStamp() {
        return messageTime;
    }

    public void setTimeStamp(Timestamp t) {
           this.messageTime = t;
    }


    public String getReqID() {
        return reqID;
    }

    public void setReqID(String id) {
    	   this.reqID = id;
    }

	public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
    	   this.bankName = bankName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }
    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
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
        return 123;
    }
    public String toString() {
     //  return "Id = " + getId() + " ; Name = " + getName();
    	return " <operation = " + operation + ",reqID = "+ reqID + ",amount=" + amount +  ",outcome = " + outcome + ",balance=" + balance + ">";
    }

    public String showReply() {
     //  return "Id = " + getId() + " ; Name = " + getName();
    	return " <operation = " + operation + ", outcome = " + outcome + ", reqID = "+ reqID + ", balance = " + balance + ", accountNumber = " + accountNumber + ", Timestamp = " + messageTime +  ">";
    }
    public String showRequest() {
     //  return "Id = " + getId() + " ; Name = " + getName();
        return "< Operation = " + operation + ", reqID = "+ reqID  + " , accountNumber= " + accountNumber + " ,amount = " + amount + ", Timestamp = " + messageTime +  ">";
    }

    public String readClientMessage() {
        return "sender = "+ sender +" headAddress = " + headAddress + " headPortNo = " + headPortNo +
                " tailAddress = " + tailAddress + " tailPortNo = " + tailPortNo;
    }
}