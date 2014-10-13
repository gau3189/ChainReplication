import java.io.Serializable;

public class RequestReply implements Serializable {

    private static final long serialVersionUID = 5950169519310163456L;
    											 
    private String reqID;
	private String bankName;

    private String operation;
	private String accountNumber;
	
	private float balance;
	private float amount;
	private Outcome outcome;
    
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
    	return " <reqID = "+ reqID +  ",outcome = " + outcome + ",balance=" + balance + ",accountNumber=" + accountNumber +  ">";
    }
    public String showRequest() {
     //  return "Id = " + getId() + " ; Name = " + getName();
        return " <reqID = "+ reqID +  ",operation = " + operation + ",amount=" + amount + ",accountNumber=" + accountNumber +  ">";
    }
}