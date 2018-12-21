import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class BranchReceiver extends Thread{

	public Branch branch;
	public Socket socket;
	BranchSender sender;
	
	public BranchReceiver(Socket receiverSock, Branch branch, BranchSender sender) {
		this.branch = branch;
		this.socket = receiverSock;
		this.sender = sender;
	}

	@Override
	public void run() {	
		try 
		{
				Bank.BranchMessage message = null;
				InputStream instream = socket.getInputStream();		
				while((message = Bank.BranchMessage.parseDelimitedFrom(instream))!=null)
				{					
					if(message.hasInitBranch()) {
						branch.set_balance(message.getInitBranch().getBalance(), false);
						for(Bank.InitBranch.Branch branchItr: message.getInitBranch().getAllBranchesList()) {
							if(!branchItr.getName().equalsIgnoreCase(branch.myBranchNm)) {
								branch.neighborBranchList.add(branchItr);
							}
						}
						setIncomingChannels();  
						sender.start();
					}
					if (message.hasTransfer()) {
						updateMoney(message);
					}else if(message.hasMarker()) {
						receivedMarkerMsg(message);
					}else if(message.hasRetrieveSnapshot()) {
						retrieveSnapShot(message, socket);
					}else if (message.hasInitSnapshot()) {
						takeLocalSnapShot(message.getInitSnapshot().getSnapshotId(), "");
					}
				}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	synchronized void updateMoney(Bank.BranchMessage message) {

		int recAmt = message.getTransfer().getMoney();
		if(branch.incomingChannelRecordingFlag.get(message.getTransfer().getBranchName())) {
			branch.incomingChannelBranchMap.put(message.getTransfer().getBranchName(), 
					branch.incomingChannelBranchMap.get(message.getTransfer().getBranchName())+ recAmt);
		}
		branch.set_balance(recAmt, false);
	}

	
	synchronized void receivedMarkerMsg(Bank.BranchMessage message) throws UnknownHostException, IOException {		
		if(branch.branchSnapMap.containsKey(message.getMarker().getSnapshotId())){
			branch.incomingChannelRecordingFlag.put(message.getMarker().getBranchName(), false);			
		}
		else {
			takeLocalSnapShot(message.getMarker().getSnapshotId(),message.getMarker().getBranchName());	
		}
	}
	
	void setIncomingChannels() {		
		for(Bank.InitBranch.Branch receivBranch: branch.neighborBranchList) {
			branch.incomingChannelBranchMap.put(receivBranch.getName(), 0);
			branch.incomingChannelRecordingFlag.put(receivBranch.getName(), false);
		}
	}
	
	void retrieveSnapShot(Bank.BranchMessage messageparam, Socket clientSocket) throws IOException{
		int index = 0;
		Bank.BranchMessage.Builder message = Bank.BranchMessage.newBuilder();
		Bank.ReturnSnapshot.Builder returnMsg= Bank.ReturnSnapshot.newBuilder();	
        Bank.ReturnSnapshot.LocalSnapshot.Builder localSnapshot = branch.branchSnapMap.get(messageparam.getRetrieveSnapshot().getSnapshotId());
		for(Bank.InitBranch.Branch receivBranch: branch.neighborBranchList) {
			index = Integer.parseInt(receivBranch.getName().replaceAll("\\D", ""));
			localSnapshot.setChannelState(index, branch.incomingChannelBranchMap.get(receivBranch.getName()));
		}
		returnMsg.setLocalSnapshot(localSnapshot);
		message.setReturnSnapshot(returnMsg);
		message.build().writeDelimitedTo(clientSocket.getOutputStream());
		clientSocket.getOutputStream().flush();
		setIncomingChannels();
		branch.branchSnapMap.remove(messageparam.getRetrieveSnapshot().getSnapshotId());
		return;
	}
	
	
	void takeLocalSnapShot(int snapshotId, String branchName) throws UnknownHostException, IOException {
		sender.waitSender = true;
		Bank.ReturnSnapshot.LocalSnapshot.Builder localSnapshot = Bank.ReturnSnapshot.LocalSnapshot.newBuilder();
		localSnapshot.setSnapshotId(snapshotId);
		branch.currentsnapshot = snapshotId;
		localSnapshot.setBalance(branch.get_balance());
		for(Bank.InitBranch.Branch branchItr: branch.neighborBranchList) {
			Bank.BranchMessage.Builder message = Bank.BranchMessage.newBuilder();
			Bank.Marker.Builder marker = Bank.Marker.newBuilder();
			marker.setSnapshotId(snapshotId);
			message.setMarker(marker);   
			Socket clientSocket = new Socket(branchItr.getIp(), branchItr.getPort());
			message.build().writeDelimitedTo(clientSocket.getOutputStream());
			clientSocket.getOutputStream().flush();
			if(branchName.equalsIgnoreCase(branchItr.getName())) {
				branch.incomingChannelRecordingFlag.put(branchItr.getName(), false);			
			}
			branch.incomingChannelBranchMap.put(branchItr.getName(), 0);
			branch.incomingChannelRecordingFlag.put(branchItr.getName(), true);
			localSnapshot.addChannelState(0);
		}
		localSnapshot.addChannelState(0);
		localSnapshot.addChannelState(0);
		branch.branchSnapMap.put(snapshotId, localSnapshot);
		sender.waitSender = false;
		return;
	}
	
	
}
