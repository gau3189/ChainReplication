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

public class MasterMessage implements Serializable {
	
    private static final long serialVersionUID = 5950169519310163579L;
    private String hostAddress;
    // private String headAddress;
    // private String tailAddress;
	private String succAddress;
    private String predAddress;
    
    // private int headPortNo;
    // private int tailPortNo;
    private int hostPortNo;
    private int hostUdpPortNo;
    private int succPortNo;
    private int predPortNo;
    private int succUdpPortNo;
    private int predUdpPortNo;

    private String bankName;
    private String sender;
    private String message;
    private Boolean isHead;
    private Boolean isTail;
    private int sequenceNo;


    public Boolean getIsHead() {
        return isHead;
    }

    public void setIsHead(Boolean head) {
           this.isHead = head;
    }

    public Boolean getIsTail() {
        return isTail;
    }

    public void setIsTail(Boolean tail) {
           this.isTail = tail;
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

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
           this.bankName = bankName;
    }

    public String getHostAddress() {
        return hostAddress;
    }

    public void setHostAddress(String hostAddress) {
    	   this.hostAddress = hostAddress;
    }

    public int getSequenceNumber() {
        return sequenceNo;
    }
    public void setSequenceNumber(int seqNo) {
        this.sequenceNo = seqNo;
    }
    // public String getHeadAddress() {
    //     return headAddress;
    // }

    // public void setHeadAddress(String headAddress) {
    //        this.headAddress = headAddress;
    // }

    // public String getTailAddress() {
    //     return tailAddress;
    // }

    // public void setTailAddress(String tailAddress) {
    //        this.tailAddress = tailAddress;
    // }

     public String getSuccAddress() {
        return succAddress;
    }

    public void setSuccAddress(String succAddress) {
           this.succAddress = succAddress;
    }

    public String getPredAddress() {
        return predAddress;
    }

    public void setPredAddress(String predAddress) {
           this.predAddress = predAddress;
    }

    
    public int getHostPortNo() {
        return hostPortNo;
    }
    public void setHostPortNo(int portNo) {
        this.hostPortNo = portNo;
    }

    public int getHostUdpPortNo() {
        return hostUdpPortNo;
    }
    public void setHostUdpPortNo(int portNo) {
        this.hostUdpPortNo = portNo;
    }

    public int getSuccUdpPortNo() {
        return succUdpPortNo;
    }
    public void setSuccUdpPortNo(int portNo) {
        this.succUdpPortNo = portNo;
    }

    public int getPredUdpPortNo() {
        return predUdpPortNo;
    }
    public void setPredUdpPortNo(int portNo) {
        this.predUdpPortNo = portNo;
    }

    public int getSuccPortNo() {
        return succPortNo;
    }
    public void setSuccPortNo(int portNo) {
        this.succPortNo = portNo;
    }

    public int getPredPortNo() {
        return predPortNo;
    }
    public void setPredPortNo(int portNo) {
        this.predPortNo = portNo;
    }
    

    public boolean equals(Object o) 
    {
        return true;
    }
    public int hashCode() {
        return 2345;
    }
    public String toString() {
     	return "<hostAddress = " + hostAddress + " ; predAddress = "+ predAddress+ " ; succAddress = " + succAddress +
               "hostPortNo = " + hostPortNo + " ; predPortNo = "+ predPortNo+ " ; succPortNo= " + succPortNo +">";
    }

    public String readMessage() {
        return "sender = "+ sender +" hostAddress = " + hostAddress + " hostPortNo = " + hostPortNo +" message = "+message;
    }

}
