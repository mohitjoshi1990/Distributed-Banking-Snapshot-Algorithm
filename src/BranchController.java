import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class BranchController{
   
   
	private static Map<String, Socket> socketListMap = new HashMap<String, Socket>();
	
   public static void main(String [] args)
   {
	   Bank.BranchMessage.Builder msgBranch = Bank.BranchMessage.newBuilder();	   
	   Bank.InitBranch.Builder initBranch = Bank.InitBranch.newBuilder();
	   int totalBal = Integer.parseInt(args[0].trim());
	   String fileName = args[1];

	   BranchController branchCon = new BranchController();
	   int totalBranches = 0;
	   int branchBal = 0;
	   boolean noBranchExist = true;
	      
	   BufferedReader br = null;
	   FileReader fr = null;

	   try
	   {
		   //br = new BufferedReader(new FileReader(FILENAME));
		   fr = new FileReader(fileName);
		   br = new BufferedReader(fr);
		   String sCurrentLine;
		   while ((sCurrentLine = br.readLine()) != null) 
		   {
			   totalBranches++;
			   Bank.InitBranch.Branch.Builder branch = Bank.InitBranch.Branch.newBuilder();
			   String[] param = sCurrentLine.split(" ");
			   branch.setName(param[0]);
			   branch.setIp(param[1]);
			   branch.setPort(Integer.parseInt(param[2]));
			   initBranch.addAllBranches(branch);
			   noBranchExist = false;
		   }		   

	   } catch (Exception e) {
		   e.printStackTrace();
	   } finally {
		   try 
		   {
			   if (br != null)
				   br.close();
			   if (fr != null)
				   fr.close();
		   }catch (IOException ex) {
			   ex.printStackTrace();

			}
		}
	   
	   if(!noBranchExist) {
		   branchBal = totalBal / totalBranches;
	   }else {
		   System.out.println("No branches to be connected to ");
		   System.exit(0);
	   }
	   try
	   {
		   for( Bank.InitBranch.Branch branch:initBranch.getAllBranchesList()) 
		   {
			   Socket clientSocket = new Socket(branch.getIp(), branch.getPort());
			   
			   initBranch.setBalance(branchBal);
			   msgBranch.setInitBranch(initBranch);
			   msgBranch.build().writeDelimitedTo(clientSocket.getOutputStream());
			   clientSocket.getOutputStream().flush();
			   socketListMap.put(branch.getName(), clientSocket);
		   }
		   int snapsShotId = 1;
		   while(true) {
			   Thread.sleep(5000);
			   branchCon.initSnapShot(initBranch, snapsShotId);

			   Thread.sleep(3000);
			   branchCon.retrieveSnapShot(initBranch, snapsShotId);
			   snapsShotId++;
		   }

	   } catch (Exception e) {
		   e.printStackTrace();
	   } finally {

		}
   }
   
   
   void initSnapShot(Bank.InitBranch.Builder initBranch, int snapShotId) throws UnknownHostException, IOException {
	   Bank.InitSnapshot.Builder initSnapMessage = Bank.InitSnapshot.newBuilder();
	   initSnapMessage.setSnapshotId(snapShotId);
	   Random rand = new Random();
	   int randomBranchIndex = rand.nextInt(initBranch.getAllBranchesCount());

	   //select branch with the index
	   Bank.InitBranch.Branch branch = initBranch.getAllBranches(randomBranchIndex);   
	   Socket clientSocket = socketListMap.get(branch.getName());
	   Bank.BranchMessage.Builder msgBranch = Bank.BranchMessage.newBuilder();
	   msgBranch.setInitSnapshot(initSnapMessage);
	   msgBranch.build().writeDelimitedTo(clientSocket.getOutputStream());
	   clientSocket.getOutputStream().flush();	   
   }
   

   
   
   void retrieveSnapShot(Bank.InitBranch.Builder initBranch, int snapShotId) throws UnknownHostException, IOException {
	   Bank.RetrieveSnapshot.Builder retSnapMessage = Bank.RetrieveSnapshot.newBuilder();
	   retSnapMessage.setSnapshotId(snapShotId);
	   ServerSocket controllerSock = new ServerSocket();
	   System.out.println("snapshot_id:  "+ snapShotId);	   
	   
	   for( Bank.InitBranch.Branch branch:initBranch.getAllBranchesList()) 
	   {
		   Socket clientSocket = socketListMap.get(branch.getName());
		   Bank.BranchMessage.Builder msgBranch = Bank.BranchMessage.newBuilder();
		   msgBranch.setRetrieveSnapshot(retSnapMessage);
		   msgBranch.build().writeDelimitedTo(clientSocket.getOutputStream());
		   
		   Bank.BranchMessage returnMessage = null;
		   while ((returnMessage = Bank.BranchMessage.parseDelimitedFrom(clientSocket.getInputStream())) != null) {
				Bank.ReturnSnapshot.LocalSnapshot localSnap = returnMessage.getReturnSnapshot().getLocalSnapshot();
				
				System.out.print(branch.getName()+": "+localSnap.getBalance()+", ");
				List<Integer> channelList = localSnap.getChannelStateList();
				int i=0;
				for(Bank.InitBranch.Branch localbranch:initBranch.getAllBranchesList()) 
				{
					if(!branch.getName().equalsIgnoreCase(localbranch.getName())){
						int index = Integer.parseInt(localbranch.getName().replaceAll("\\D", ""));;							
						System.out.print(localbranch.getName()+"-->" + branch.getName()+": "+channelList.get(index)+", ");
					}
				}
				System.out.println("");
				break;
		   }
	   }
		System.out.println();
   }
   
   
}