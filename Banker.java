import java.util.*;
import java.io.*;

public class Banker{
	static int cycle = 0;
	static int numTasks = 0;
	static int numResources = 0;
	static int[] unitsReturned;
	static ArrayList<Task> ready = new ArrayList<Task>();
	static ArrayList<Task> blocked = new ArrayList<Task>();
	static ArrayList<Task> done = new ArrayList<Task>();

	
	static ArrayList<ArrayList<String>> data = new ArrayList<ArrayList<String>>();
	static ArrayList<String> info = new ArrayList<String>();
	static ArrayList<Resource> resources = new ArrayList<Resource>();
	static ArrayList<Task> tasks = new ArrayList<Task>();

	/*
	 * I decided to create two objects Task and Resource to easily and accurately maintain the information throughout each cycle.
	 */
	static class Task{
		int id = 0;
		int time = 0;
		int[] claims = new int[numResources];
		int[] requests = new int[numResources];
		int delay = 0;
		int timeWait = 0;
		int timeFinished = 0;
		boolean aborted = false;
		Task(int A){
			id = A;
		}
	}
	static class Resource{
		int id = 0;
		int units = 0;
		Resource(int name, int num){
			id = name;
			units = num;
		}
	}
	public static void main(String [] args){
		File file = new File("");
		
		//If there is no input file, print error message and exit.
		if(args.length == 0){
			System.err.println("Unable to read from file");
			System.exit(-1);
		}
		//Else the file is the inputted file
		else file = new File(args[0]);
		
		//check to see if the file exists
		try{
			//Scanner uses the inputted file.
			Scanner scan = new Scanner(file);
			//Stored the first two numbers in global variables.
			numTasks = scan.nextInt();
			numResources = scan.nextInt();
			//Reads the rest of the info into an array list.
			while(scan.hasNext()){
				info.add(scan.next());
			}

			reset();
			algorithm(true);
			reset();
			algorithm(false);
		}
		//If the file does not exists print an error message and exit.
		catch(FileNotFoundException e){
			System.err.println("Unable to read from file");
			System.exit(-1);
		}
	}

	public static void algorithm(boolean optimistic){
		//This method actually runs the algorithm, both for FIFO and Bankers. The difference is due to a parameter called optimistic. This tells the program
		//whether or not the algorithm is FIFO or Bankers. The method first checks blocked processes, then ready processes, then (in the case of FIFO) deadlock,
		//and finally increments wait time of the blocked processes. It then prints out the correct output at the end of the method.
		while(done.size() != numTasks){
			cycle++;
			int numBlocked = blocked.size();
			if((data.get(0).size() > 0) && ((data.get(0).get(0)).equals("initiate"))) initiate(optimistic);
			else{
				int numReady = ready.size();
				//Check Blocked
				for(int i = 0; i < numBlocked; i++){
					int index = blocked.get(0).id - 1;
					if((data.get(index).get(0)).equals("request")) request(index, true, optimistic);
				}
				//Check Ready
				for(int i = 0; i < numReady; i++){
					int index = ready.get(0).id - 1;
					if((data.get(index).get(0)).equals("request")) request(index, false, optimistic);
					else if((data.get(index).get(0)).equals("release")) release(index);
							
					if((data.get(index).get(0)).equals("terminate")) terminate(index);
				}
				//If FIFO check Deadlock
				if(optimistic){
					if((done.size() != numTasks) && (blocked.size() == numTasks - done.size())){
						deadlockCheck();
					}
				}
				//Return units gained this cycle.
				for(int i = 0; i < numResources; i++){
					resources.get(i).units += unitsReturned[i];
					unitsReturned[i] = 0;
				}
			}
			//Increment wait time
			numBlocked = blocked.size();
			for(int i = 0; i < numBlocked; i++){
				if(blocked.get(i).delay <= 0) blocked.get(i).timeWait += 1;
			}
		}
		//Print information.
		print(optimistic);
	}

	public static void initiate(boolean optimistic){
		/*
		 * The initiate method initiates in one shot all tasks and their claims for that particular resource. Let's say we have the call
		 * initiate 1 0 1 4, this method initiates resource 1 for ALL tasks, not just task 1. It then adds it into the correct place on the 
		 * ready arraylist.
		 */ 
		for(int i = 0; i < numTasks; i++){
			int initialClaim = Integer.parseInt(data.get(i).get(4));
			int resourceType = Integer.parseInt(data.get(i).get(3)) - 1;
			//Only for Banker's : Checks for initial claim being more than what is available. If so abort.
			if(!optimistic && initialClaim > resources.get(resourceType).units){
				System.out.printf("Banker aborts task %d before run begins:\n\tclaim for resource %d (%d) exceeds number of units present (%d)\n", tasks.get(i).id, (resourceType + 1), initialClaim, resources.get(resourceType).units);
				//Set the aborted field true and move to the done list.
				tasks.get(i).aborted = true;
				done.add(tasks.get(i));
			}
			else{
				tasks.get(i).claims[resourceType] = initialClaim;
				if(ready.size() < numTasks) ready.add(tasks.get(i));
				else {
					ready.remove(0);
					ready.add(tasks.get(i));
				}
			}
			for(int k = 0; k < 5; k++){
				data.get(i).remove(0);
			}
		}
	}
	

	public static void request(int i, boolean block, boolean optimistic){
		/*
		 * The request method is split into two conditional statements. The first being FIFO and the latter being Bankers. In the first if block, we first check
		 * to make sure that the amount we are requesting is available. If it is not available, move it to the back of the blocked list (and remove from the
		 * correct list depending on blocked or ready state). If it is available move it to the back of the ready list. (decrement the delay when first 
		 * entering the method).
		 *
		 * In the second if block we do the bankers algorithm. This follows the same algorithm as optimistic however instead of checking to make sure that 
		 * the amount we are requesting is available, we check to see if the state would remain safe (see method safeState()).
		 *
		 */
		int numRequested = Integer.parseInt(data.get(i).get(4));
		int resourceType = Integer.parseInt(data.get(i).get(3)) - 1;
		//Only for Banker's : Checks to see if what is being requested exceeds the process's initial claim. If so abort.
		if(!optimistic && tasks.get(i).requests[resourceType] + numRequested > tasks.get(i).claims[resourceType]){
			System.out.printf("During cycle %d-%d of Banker's algorithms\n\tTask %d's request exceeds its claim; aborted;\n\t", (cycle - 1), cycle, tasks.get(i).id);
			for(int k = 0; k < numResources; k++){
				System.out.printf("%d units (of Resource Type %d) available next cycle.\n\t", tasks.get(i).requests[k], (k+1));
				unitsReturned[k] += tasks.get(i).requests[k];
				tasks.get(i).requests[k] = 0;
			}
			System.out.println();
			//Set the aborted field true and move to the done list.
			tasks.get(i).aborted = true;				
			done.add(tasks.get(i));
		}
		else{
			boolean safe;
			//Sets a "safe" condition to avoid a repetition of code. If optimistic the safe condition is whether or not what we are asking for is 
			//available. If Banker's then the safe condition is whether the request leaves us in a safe state.
			if(optimistic) safe = numRequested <= resources.get(resourceType).units;
			else safe = safeState(i);
			
			//Decrement the delay. Once delay reaches 0, process the request.
			if(tasks.get(i).delay == 0) tasks.get(i).delay = Integer.parseInt(data.get(i).get(2));
			else tasks.get(i).delay -= 1;
			if(tasks.get(i).delay == 0) tasks.get(i).delay = -1;

			if(safe){
				if(tasks.get(i).delay <= 0) {
					tasks.get(i).delay = 0;
					tasks.get(i).requests[resourceType] += numRequested;
					resources.get(resourceType).units -= numRequested;
					if(block) ready.add(blocked.remove(0));
					else ready.add(ready.remove(0));
					for(int k = 0; k < 5; k++){
						data.get(i).remove(0);
					}
				}
				else{
					//If the delay isn't 0, move to the back of the list it is in.
					if(block) blocked.add(blocked.remove(0));
					else ready.add(ready.remove(0));
				}
			}
			else{
				//If the state isn't "safe" block the process.
				if(block) blocked.add(blocked.remove(0));
				else blocked.add(ready.remove(0));
			}	
		}
	}

	public static void release(int i){
		/*
		 * The method release simply releases the amount necessary to release (as indicated by the data). The way this works is by removing the number we wish
		 * to release from the number the current task is currently holding and adding it to the array unitsReturned. This number will be added to the total
		 * number of units at the end of each cycle.
		 */
		//Decrement the delay. Once delay reaches 0, process the request.
		if(tasks.get(i).delay == 0) tasks.get(i).delay = Integer.parseInt(data.get(i).get(2));
		else tasks.get(i).delay -= 1;
		if(tasks.get(i).delay == 0) {
			tasks.get(i).requests[Integer.parseInt(data.get(i).get(3)) - 1] -= Integer.parseInt(data.get(i).get(4));
			unitsReturned[Integer.parseInt(data.get(i).get(3)) - 1] += Integer.parseInt(data.get(i).get(4));
			for(int k = 0; k < 5; k++){
				data.get(i).remove(0);
			}
			//If the next thing the task wants to do is terminate, do not move it to the back of the ready list.
			if(!data.get(i).get(0).equals("terminate")){
				ready.add(ready.remove(0));
			}
		}
		//If the delay isn't 0, move to the back of the list it is in.
		else ready.add(ready.remove(0));
	}

	public static void terminate(int i){
		/*
		 * The method terminate finishes the current task and returns all of its resources to the array unitsReturned. It then moves the task into the
		 * done arrayList.
		 */

		//Decrement the delay. Once delay reaches 0, process the request.
		if(tasks.get(i).delay == 0) tasks.get(i).delay = Integer.parseInt(data.get(i).get(2));
		else tasks.get(i).delay -= 1;
			
		if(tasks.get(i).delay == 0) {
			for(int k = 0; k < numResources; k++){
				unitsReturned[k] += tasks.get(i).requests[k];
				tasks.get(i).requests[k] = 0;
			}
			for(int k = 0; k < 5; k++){
				data.get(i).remove(0);
			}
			ready.get(0).timeFinished = cycle;
			done.add(ready.remove(0));
		}
		//If the delay isn't 0, move to the back of the list it is in.
		else ready.add(ready.remove(0));
	}
	
	public static void deadlockCheck(){
		/*
		 * The method deadlockCheck checks for deadlock. Initially sets a boolean deadlocked to true, then checks to see if the process is actually in deadlock.
		 * The way we check for deadlock is by seeing if we add the units in the unitsReturned array to the units the resource holds, would one process be able
		 * to come out of the blocked state. If it can come out, then it is not deadlocked. If it can't, it is deadlocked and we find the smallest task (by ID),
		 * and abort that process. This abort step is the same as the other aborts previously.
		 */
		boolean deadlocked = true;
		while(deadlocked){
			for(int i = 0; i < blocked.size(); i++){
				//For each process:
				int index = blocked.get(i).id - 1;
				if(data.get(index).size() > 0){
					int unitRequested = Integer.parseInt(data.get(index).get(4));
					int resourceNumber = Integer.parseInt(data.get(index).get(3)) - 1;
					//If what we are asking can be satisfied once we return the units, there is no deadlock.
					if(unitRequested <= (resources.get(resourceNumber).units + unitsReturned[resourceNumber])){
						deadlocked = false;
					}
				}
			}
			if(deadlocked){
				//Set minID to some arbitrarily large number. Location is set to 0.
				int minID = Integer.MAX_VALUE;
				int location = 0;	

				for(int i = 0; i < blocked.size(); i++){
					//Everytime we find a smaller ID, set minID to that ID and set the location to that location in the blocked list (i).
					if(blocked.get(i).id < minID){ 
						minID = blocked.get(i).id;
						location = i;
					}
				}
				//Abort the minID task.
				for(int k = 0; k < numResources; k++){
					unitsReturned[k] += tasks.get(minID - 1).requests[k];
					tasks.get(minID - 1).requests[k] = 0;
				}
				blocked.get(location).aborted = true;	
				done.add(blocked.remove(location));	
			}
		}
	}

	public static boolean safeState(int index){
		/*
		 * The method safeState checks to see if the state is currently safe. Initially I thought I would have to check every process to see if granting the
		 * request would result in a safe state. However, the way this method works is, if I can finish, then I will, and the request that was granted to me
		 * will go back into the units of that resource anyway. Basically if I ask for 1 unit of unit R and I can still finish, that unit will be returned
		 * when I finish. So no matter what granting my request will result in a safe state (if I can finish!). 
		 * (This may be due to the linear nature of this lab. In the examples given in class we jump in to the middle of already running processes. But for
		 * the purposes of this lab this algorithm works and is much more efficient).
		 */
		boolean safe = true;
		//For EACH resource (this ensures I can finish):
		for(int i = 0; i < numResources; i++){
			//If my initial claim - my current claims is greater than the number of units available, I can't finish and therefore I'm not safe.
			if((tasks.get(index).claims[i] - tasks.get(index).requests[i]) > resources.get(i).units){
				safe = false;
			}
		}
		return safe;
	}

	public static void sort(){
		/*
		 * The method sort sorts the done array so we can print the tasks in the correct order.
		 */
		int size = done.size();
		ArrayList<Task> temp = new ArrayList<Task>();
		//Create a temp list of size done, and initialize it to null.
		for(int i = 0; i < size; i++){
			temp.add(null);
		}
		//Put each task in the correct location.
		for(int i = 0; i < size; i++){
			temp.set(done.get(i).id - 1, done.get(i));
		}
		//Transfer it back to the done arrayList.
		for(int i = 0; i < size; i++){
			done.set(i, temp.get(i));
		}
	}

	public static void reset(){
		/*
		 * The method reset resets all of the important data to the correct values. This is used when running Banker's after FIFO, (and of course before FIFO).
		 * It sets all arrayLists to brand new arrayLists with no data in them. Then it adds the correct data into each list.
		 * The instructions String temp = info.remove(0) ... info.add(temp) ensures that our initial data doesn't get corrupted and remains in the correct
		 * order. So now we can reset as many times as we want and have the same information each time.
		 *
		 * I loop through the data and move it into an arrayList of arrayLists. The reason for this is I can very easily access the information for task 3 
		 * simply by accessing data.get(2). 
		 */
		unitsReturned = new int[numResources];
		tasks = new ArrayList<Task>();
		resources = new ArrayList<Resource>();
		done = new ArrayList<Task>();
		data = new ArrayList<ArrayList<String>>();
		cycle = 0;
		//Creates Tasks and Resources with unique ids (and claims for resources).
		for(int i = 1; i < numTasks + 1; i++){
			tasks.add(new Task(i));
		}
		int index = 0;
		for(int i = 0; i < numResources; i++){
			String temp = info.remove(0);
			resources.add(new Resource((i+1), Integer.parseInt(temp)));
			info.add(temp);
			index++;
		}
		//This array list is then sorted to make it easier to read from (removes interleaving).
		for(int i = 0; i < numTasks; i++){
			data.add(new ArrayList<String>());
		}
		while(index != info.size()){
			int taskNum = Integer.parseInt(info.get(1));
			for(int i = 0; i < 5; i++){
				String temp = info.remove(0);
				data.get(taskNum-1).add(temp);
				info.add(temp);
				index++;
			}
		}
	}

	public static void print(boolean optimistic){
		/*
		 * The print method prints out the necessary information for both FIFO and banker's (depending on the parameter optimistic). First it sorts the done
		 * list, the calculates all of the important information and prints it.
		 */
		sort();
		if(optimistic){
			System.out.println("FIFO");
			int totalWait = 0;
			int totalTime = 0;
			for(int i = 0; i < done.size(); i++){
				if(!done.get(i).aborted){
					double percentage = (double)done.get(i).timeWait/(double)done.get(i).timeFinished * 100.0;
					System.out.printf("Task %d : %d %d %d%%\n", done.get(i).id, done.get(i).timeFinished, done.get(i).timeWait, Math.round(percentage));
					totalWait += done.get(i).timeWait;
					totalTime += done.get(i).timeFinished;
				}
				else{
					System.out.printf("Task %d : aborted\n", done.get(i).id);
				}
			}
			double percentage = (double)totalWait/(double)totalTime * 100.0;
			System.out.printf("Total  : %d %d %d%%\n\n", totalTime, totalWait, Math.round(percentage));
	
		}
		else{
			System.out.println("BANKER'S");
			int totalWait = 0;
			int totalTime = 0;
			for(int i = 0; i < done.size(); i++){
				if(!done.get(i).aborted){
					double percentage = (double)done.get(i).timeWait/(double)done.get(i).timeFinished * 100.0;
					System.out.printf("Task %d : %d %d %d%%\n", done.get(i).id, done.get(i).timeFinished, done.get(i).timeWait, Math.round(percentage));
					totalWait += done.get(i).timeWait;
					totalTime += done.get(i).timeFinished;
				}
				else{
					System.out.printf("Task %d : aborted\n", done.get(i).id);
				}
			}
			double percentage = (double)totalWait/(double)totalTime * 100.0;
			System.out.printf("Total  : %d %d %d%%\n\n", totalTime, totalWait, Math.round(percentage));
		}
	}
}
