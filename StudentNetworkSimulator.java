import java.util.*;
import java.io.*;
import java.lang.System;

/* StudentNetworkSimulator.java
 * Brandon Charlesworth
 * bjcworth@bu.edu
 * U20809812
 */


public class StudentNetworkSimulator extends NetworkSimulator
{
    /*
     * Predefined Constants (static member variables):
     *
     *   int MAXDATASIZE : the maximum size of the Message data and
     *                     Packet payload
     *
     *   int A           : a predefined integer that represents entity A
     *   int B           : a predefined integer that represents entity B 
     *
     * Predefined Member Methods:
     *
     *  void stopTimer(int entity): 
     *       Stops the timer running at "entity" [A or B]
     *  void startTimer(int entity, double increment): 
     *       Starts a timer running at "entity" [A or B], which will expire in
     *       "increment" time units, causing the interrupt handler to be
     *       called.  You should only call this with A.
     *  void toLayer3(int callingEntity, Packet p)
     *       Puts the packet "p" into the network from "callingEntity" [A or B]
     *  void toLayer5(String dataSent)
     *       Passes "dataSent" up to layer 5
     *  double getTime()
     *       Returns the current time in the simulator.  Might be useful for
     *       debugging.
     *  int getTraceLevel()
     *       Returns TraceLevel
     *  void printEventList()
     *       Prints the current event list to stdout.  Might be useful for
     *       debugging, but probably not.
     *
     *
     *  Predefined Classes:
     *
     *  Message: Used to encapsulate a message coming from layer 5
     *    Constructor:
     *      Message(String inputData): 
     *          creates a new Message containing "inputData"
     *    Methods:
     *      boolean setData(String inputData):
     *          sets an existing Message's data to "inputData"
     *          returns true on success, false otherwise
     *      String getData():
     *          returns the data contained in the message
     *  Packet: Used to encapsulate a packet
     *    Constructors:
     *      Packet (Packet p):
     *          creates a new Packet that is a copy of "p"
     *      Packet (int seq, int ack, int check, String newPayload)
     *          creates a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and a
     *          payload of "newPayload"
     *      Packet (int seq, int ack, int check)
     *          create a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and
     *          an empty payload
     *    Methods:
     *      boolean setSeqnum(int n)
     *          sets the Packet's sequence field to "n"
     *          returns true on success, false otherwise
     *      boolean setAcknum(int n)
     *          sets the Packet's ack field to "n"
     *          returns true on success, false otherwise
     *      boolean setChecksum(int n)
     *          sets the Packet's checksum to "n"
     *          returns true on success, false otherwise
     *      boolean setPayload(String newPayload)
     *          sets the Packet's payload to "newPayload"
     *          returns true on success, false otherwise
     *      int getSeqnum()
     *          returns the contents of the Packet's sequence field
     *      int getAcknum()
     *          returns the contents of the Packet's ack field
     *      int getChecksum()
     *          returns the checksum of the Packet
     *      String getPayload()
     *          returns the Packet's payload
     *
     */

    /*   Please use the following variables in your routines.
     *   int WindowSize  : the window size
     *   double RxmtInterval   : the retransmission timeout
     *   int LimitSeqNo  : when sequence number reaches this value, it wraps around
     */

    public static final int FirstSeqNo = 1;
    private int WindowSize;
    private double RxmtInterval;
    private int LimitSeqNo;
    
    // Add any necessary class variables here.  Remember, you cannot use
    // these variables to send messages error free!  They can only hold
    // state information for A or B.
    // Also add any necessary methods (e.g. checksum of a String)
    
    
    // window buffer for tracking sent packets
    Packet[] buffer;
    
    // double list for storing communication times
    LinkedList<Double> rtt = new LinkedList<Double>();
    // double array for tracking sent and ack times
    double[] tsent;
    double[] tackd;
   // int var for tracking next seq # A will send to B
    private int nextseq = FirstSeqNo;
    // int var for keeping track of base of the sending window
    private int base = 1;
    // int var for tracking next seq # B expects to rcv
    private int eseq = 1;
    // init new pkt for impending ack transmission later
	Packet ackpkt = new Packet(0, 0, 0);
	// int var for tracking num pkts sent
    private int sent = 0; 
    // int var tracking num retransmissions
    private int retrans = 0;
    // int var tracking num pkts rcv'd 
    private int rcvd = 0;
    // int var tracking num pkts lost
    private int numlost = 0;
    // int var tracking num corrupted pkts rcv'd
    private int numcrpt = 0;
	
	// translate String 'str' into an int, and add this to seq and ack to generate a checksum
	// then return that checksum
    protected int clc_chksm(int seq1, int ack1, String str) {
    	// generate int representation of our payload
    	int pay = str.hashCode();
    	// return seq+ack+int(payload)
    	return seq1+ack1+pay;
    }
  
    // This is the constructor.  Don't touch!
    public StudentNetworkSimulator(int numMessages,
                                   double loss,
                                   double corrupt,
                                   double avgDelay,
                                   int trace,
                                   int seed,
                                   int winsize,
                                   double delay)
    {
        super(numMessages, loss, corrupt, avgDelay, trace, seed);
	WindowSize = winsize;
	LimitSeqNo = winsize+1;
	RxmtInterval = delay;
    }
    
	

    
    // This routine will be called whenever the upper layer at the sender [A]
    // has a message to send.  It is the job of your protocol to insure that
    // the data in such a message is delivered in-order, and correctly, to
    // the receiving upper layer.
    protected void aOutput(Message message)
    {
    	// store data from message rcvd in String var for use in payload later
    	String rcvd = message.getData();
    	if(nextseq<base+WindowSize) {
		    // generate checksum for impending pkt creation
		 	int chksm = clc_chksm(nextseq,-1,rcvd);
			// public Packet(int seq, int ack, int check, String newPayload)
			Packet pkt = new Packet(nextseq,-1,chksm,rcvd);
			// print String representation of Packet 'p'
	    	System.out.println("aOutput rcvd: " + pkt.toString());
	    	// print seqnum
	    //	System.out.println("nextseq = " + nextseq);
			// if we reached end of array...
			if(nextseq > WindowSize) {
				System.out.println("Wrapping Around");
				// wrap around
				buffer[nextseq%LimitSeqNo] = pkt;
			}// else, just add new packet to sending buffer (will be removed once ack rcv'd)		
			else { buffer[nextseq] = pkt; }
			// send Packet pkt to layer 3 of B
			toLayer3(A, pkt);
			// record start time and add to list
			long time1 = System.nanoTime();
			System.out.println("timesent: " + time1);
			tsent[nextseq] = time1; time1 = 0;
			// increment sent counter
			sent++;
			System.out.println("aOutput snt: " + pkt.toString());
			// if base of window gets slid over but nextseq is not updated...
			if(base==nextseq) {
				// start timeout for A for time specified by delay inputted by user
				// triggering a timeout in which lost pkts are retrans'd
				startTimer(A, RxmtInterval);
				}
			// inc nextseqnum
			nextseq++;
    		}
    	else return;
    	
    }
 
    
    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by a B-side procedure)
    // arrives at the A-side.  "packet" is the (possibly corrupted) packet
    // sent from the B-side.
    protected void aInput(Packet packet)
    {	
    	// record ack time and add to list
		double time2 = System.nanoTime();
		System.out.println("timeakd: "+ time2);
		tackd[nextseq] = time2; time2 = 0;
    	// inc rcvd counter
    	rcvd++;
    	// make copy of Packet 'packet' and store as Packet 'p'
    	Packet p = new Packet(packet);
    	// print String representation of Packet 'p'
    	System.out.println("aInput: " + p.toString());
    	// store fields of 'p'
    	int seq = p.getSeqnum(); int ack = p.getAcknum(); int chks = p.getChecksum();
    	// calculate what chksm should be and store in var 'chk'
    	int chk = seq+ack;
    	// compare this calc'd chksm with that of the pkt rcvd
    	// if no corruption...
    	if (chks == chk) {
    		// update base
    		base = p.getAcknum()+1;
    		// if we have slid the base of our window over to the next seq #...
    		if(base==nextseq) {
    			// stop timer, bc we are where we want to be
    			stopTimer(A);
    		}
    		// otherwise, trigger timeout in which we will retransmit not ack'd pkts
    		else startTimer(A, RxmtInterval);
    	} // increment corrupt counter
    	else { numcrpt++; }
    }
    
    // This routine will be called when A's timer expires (thus generating a 
    // timer interrupt). You'll probably want to use this routine to control 
    // the retransmission of packets. See startTimer() and stopTimer(), above,
    // for how the timer is started and stopped. 
    protected void aTimerInterrupt()
    {
    	// start timer for retransmission
    	startTimer(A, RxmtInterval);
    	// resend all pkts previously sent but not yet ack'd (waiting in send buffer)
    	for(int i=base; i<nextseq; i++) {
    		// if we are past window...
    		if(i>WindowSize) {
    			// print String representation of Packet 
    	    	System.out.println("aTimerInterrupt (Wrap): " + buffer[i%LimitSeqNo].toString());
    	    	// print seqnum
    	    	System.out.println("nextseq = " + nextseq);
    	    	toLayer3(A, buffer[i%LimitSeqNo]);
    	    	// increment sent & retransmission counter
    	    	sent++; retrans++;
    		}
    		
    		else { // print String representation of Packet 'p'
    	    	System.out.println("aTimerInterrupt: " + buffer[i].toString());
    			toLayer3(A, buffer[i]);
    			// increment sent & retransmission counter
    	    	sent++; retrans++;
    			}
    	}
    }
    
    // This routine will be called once, before any of your other A-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity A).
    protected void aInit()
    {
    	// initialize window buffer to WindowSize+1, to allow for wrap around
		buffer = new Packet[LimitSeqNo];
		tsent = new double[1050];
		tackd = new double[1050];
    }
    
    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by an A-side procedure)
    // arrives at the B-side.  "packet" is the (possibly corrupted) packet
    // sent from the A-side.
    protected void bInput(Packet packet)
    {	// increment received count
    	rcvd++;
    	// make copy of Packet 'packet' and store as Packet 'p'
    	Packet p = new Packet(packet);
    	// print String representation of Packet 'p'
    	System.out.println("bInput rcvd: " + p.toString());
    	// print seqnum
    	System.out.println("nextseq = " + nextseq);
    	// print seqnum expected
    	System.out.println("eseq = " + eseq);
    	// store fields of 'p'
    	int seq = p.getSeqnum(); int ack = p.getAcknum(); int chks = p.getChecksum();
    	String pay = p.getPayload();
    	// calculate what chksm should be and store in var 'chk'
    	int chk = clc_chksm(seq,ack,pay);
    	// print this calc'd chksm
    	System.out.println("chksm should be: " + chk);
    	// declare var for storing ack checksum
    	int ackchk;
    	// compare calc'd chksm with that of the pkt rcvd, as well as the seq #
    	// if seq no. and chksm are what we expect...
    	if ((seq == eseq) && (chks == chk)) {
    		// send payload to layer5
    		toLayer5(pay);
    		// generate ack # for ack pkt
    		ack = seq;
    		// generate new chksm for ack pkt
    		ackchk = seq+ack;
    		// update ack pkt
    		ackpkt = new Packet(eseq,ack,ackchk);
    		// print str of ackpkt
    		System.out.println("new ackpkt: " + ackpkt.toString());
    		// send ack to layer3
    		toLayer3(B,ackpkt);
    		// inc sent count & next expected seq #
    		sent++; eseq++;
    		System.out.println("new eseq: " + eseq);
    	}
    	// else
    	else{
    		if (chks != chk) {
    			// increment corruption count
    			numcrpt++;
    		}
    		// if no last pkt rcvd (first msg lost), return
    		if(ackpkt.getSeqnum() <= 0) {
    			return;
    		}
    		else {
	    		// else, print str of last ackpkt and send to layer3
	    		System.out.println("last ackpkt: " + ackpkt.toString());
	    		toLayer3(B,ackpkt);
    		}
    	}
    		
    }
    
    // This routine will be called once, before any of your other B-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity B).
    protected void bInit()
    {

    }

    // Use to print final statistics
    protected void Simulation_done()
    {
    	numlost = sent - rcvd;
    	double sum = 0;
    	double artt;
    	// add all rtt's to rtt list
    	for(int i=0; i<tsent.length-1; i++) {
    		// by computing difference in ack and trans time
    		rtt.add(i, tackd[i]-tsent[i]);
    		// sum the list
    		sum += rtt.get(i);
    	}
    	// divide sum by number of times to get avrg rtt
    	artt = sum/rtt.size();
    	System.out.println("sent: " + sent);
    	System.out.println("retransmitted: " + retrans);
    	System.out.println("received: " + rcvd);
    	System.out.println("lost: " + numlost);
    	System.out.println("corrupt: " + numcrpt);
    	System.out.println("artt: " + artt + " ms");
    	
    }	

}
