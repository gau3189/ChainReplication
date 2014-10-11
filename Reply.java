import java.io.Serializable;

public class ServerMessage implements Serializable {
    private static final long serialVersionUID = 5950169519310163575L;
    private int id;
    private String name;
    public String operation;
	public String accountNumber;
	public String hostAddress;
	public int portNo;
	public String reqID;
	public float balance;
	public float amount;
	public Outcome outcome;
    public int getId() {
        return id;
    }
    public void setId(int id) {
    	   this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public boolean equals(Object o) 
    {
        return true;
    }
    public int hashCode() {
        return id;
    }
    public String toString() {
     //  return "Id = " + getId() + " ; Name = " + getName();
    	return "operation = " + operation + " ; reqID = "+ reqID+ " ; balance = " + balance + " ; outcome = " + outcome ;
    }
}
