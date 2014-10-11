import java.io.Serializable;

public class ServerMessage implements Serializable {
    private static final long serialVersionUID = 5950169519310163575L;
    public String operation;
	public String accountNumber;
	public String hostAddress;
	public int portNo;
	public String reqID;
	public float balance;
	public float amount;
	public Outcome outcome;

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
     	return "operation = " + operation + " ; reqID = "+ reqID+ " ; balance = " + balance + " ; outcome = " + outcome ;
    }
}
