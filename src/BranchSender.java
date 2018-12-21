import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

public class BranchSender extends Thread{

	public Branch branch;
	public ServerSocket serverSocket;
	public boolean waitSender = false;
	//public Socket socket;
	
	public BranchSender(Branch branch) {
		// TODO Auto-generated constructor stub
		this.branch = branch;
	}
		
	
	@Override
	public void run() {
		try {
			Thread.sleep(5000);
			while (true) {
				
					Random random = new Random();
					Thread.sleep(random.nextInt(5)*1000);				
					
					int branchIndex = random.nextInt(branch.neighborBranchList.size());
					Bank.InitBranch.Branch destBranch = branch.neighborBranchList.get(branchIndex);
					if(!waitSender) {
						Socket clientSocket = new Socket(destBranch.getIp(), destBranch.getPort());					
						Bank.Transfer.Builder transferMsg = Bank.Transfer.newBuilder();
						transferMsg.setBranchName(branch.myBranchNm);
						Bank.BranchMessage.Builder message = Bank.BranchMessage.newBuilder();
						int sendMoney = branch.set_balance(0, true);
						if(sendMoney == 0)
							continue;
						if(branch.branchSnapMap.containsKey(branch.currentsnapshot)) {
					        Bank.ReturnSnapshot.LocalSnapshot.Builder localSnapshot = branch.branchSnapMap.get(branch.currentsnapshot);
					        localSnapshot.setBalance(localSnapshot.getBalance()-sendMoney);
						}
						transferMsg.setMoney(sendMoney);
						message.setTransfer(transferMsg);
	
						message.build().writeDelimitedTo(clientSocket.getOutputStream());
						clientSocket.getOutputStream().flush();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

}
