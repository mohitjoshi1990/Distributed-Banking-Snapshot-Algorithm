import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Branch{
	
	private int branchBal;
	public String myBranchNm = "";
	public Bank.InitBranch.Branch.Builder selfBranch;
	public List<Bank.InitBranch.Branch> neighborBranchList = new ArrayList<Bank.InitBranch.Branch>();
	private ServerSocket serverSocket;
	public int currentsnapshot = 0;
	

	Map<Integer, Branch> localstateMap = new HashMap<Integer, Branch> ();
	Map<String, Integer> incomingChannelBranchMap = new HashMap<String, Integer>();
	Map<String, Boolean> incomingChannelRecordingFlag = new HashMap<String, Boolean>();
	Map<Integer, Bank.ReturnSnapshot.LocalSnapshot.Builder> branchSnapMap = new HashMap<Integer, Bank.ReturnSnapshot.LocalSnapshot.Builder>();

	   
	public Branch(String branchNm, int port) throws IOException {
		this.branchBal = 0;
		serverSocket = new ServerSocket(port);
		this.myBranchNm = branchNm;
	}
	
	public int get_balance() {	
		synchronized (this) {
			return this.branchBal;
		}
	}
	
	public int set_balance(int balance, boolean branchSentMoney) {	
		synchronized (this) {
			if(branchSentMoney) 
			{
				Random random = new Random();
				int sendMoney = (this.branchBal * (random.nextInt(4)+1))/100;
				if(this.branchBal - sendMoney <= 0){
					return 0;
				}
				this.branchBal -= sendMoney;
				return sendMoney;
			}else {
				this.branchBal += balance;
			}			
			return this.branchBal;
		}
	}
	

	public static void main(String [] args) {
	  String branchName = args[0];
	  int port = Integer.parseInt(args[1]);
	  try
	  {
		  Branch branch = new Branch(branchName,port);
		  BranchSender sender = new BranchSender(branch);
		  while(true) {
				Socket receiveSock = branch.serverSocket.accept();
				BranchReceiver receiver = new BranchReceiver(receiveSock, branch, sender);
				receiver.start();
		  }
	  } catch (Exception e) {
		 e.printStackTrace();
	  }
	}

}
