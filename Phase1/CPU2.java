//Name: Oluwatobiloba Adeyeye
//Course Number: CS 4323
//Assignment Title:Phase I Job Scheduler
//Due Date: 03-13-2018

import java.util.*;
import java.io.*;

public class CPU2{
	//incomingJob stores the information of an arriving job
	PCB incomingJob = new PCB();
	LinkedList <PCB> disk = new LinkedList<PCB>();
	LinkedList <PCB> readyQueue = new LinkedList<PCB>();
	MemoryManager memoryManager = new MemoryManager();
	Clock clock = new Clock();
	//store the information to be used to create PCB
	int jobId, jobClass, requiredMemory, processingTime,arrivalTime; 
	Scanner scan = null;
	//Creates output file
	File file = new File("OSOutput.txt");
	PrintWriter out = null;
	//Following VARIABLES STORE FINAL EXECUTION STATISTICS 
	int total=0, totalCPU=0, totalIO=0, totalBal=0, avgTurnTime=0;
	int avgWaitTime=0; 
	
	//main method starts the CPU
	public static void main (String [] args){
			
		CPU2 job = new CPU2();
		job.startSystem();
		
	}
	//Routine is responsible for initializing the System.
	public  void startSystem(){
		
		try{
		 	printStat("INDIVIDUAL TERMINATION STATISTICS");
			String x,y;
			x ="Id    Class  Arrival  LoadTime Termination";
			y=" Processing Turnaround Waiting"; 
			printStat(x+y);
			scan = new Scanner(
			// new File("/home/opsys/OS-I/18Sp-jobs"));
			new File("OSjobs"));
			//While loop reads in arriving jobs
			while (scan.hasNextLine() && scan.hasNextInt()){ 
				readJob();
				//Calls JDispatch When memory is full
				//or there is O input
				if(jobId == 0 || readyQueue.size()==26){
					
					if(jobId ==0){
						//Maximizes memory when 
						//JobId == 0 and Memory is not full
						checkDisk();
						J_DISPATCH();
					}
					else J_DISPATCH();
				}				
				else{
					callJ_SCHED();
					
					
				}
			}
			J_DISPATCH();
			int clockVal = clock.getTime();
			printRejectedJobs();
			printFinalStat(clockVal);
			scan.close();
			
		}
		catch(FileNotFoundException ex){
			ex.printStackTrace();
		}
	}
	//J_SCHED 		: job arrival routine
	//Approach To Maintaining a Balanced Job Mix----
	//Assumptions: CPU-Bound Jobs have higher priority
	//over balanced and I/0-Bound jobs
	//My strategy is to allow CPU-bound jobs to make up at most
	//50% of the 26 memory slots 
	//and any combination of the other two classes.
	//This is implemented in the method isOptimal(int Job Class)
	//isOptimal() is called in the J_SCHED routine
	public void J_SCHED(int jobId, int Class,int ReqMem, int procTime
	,int arrTime){
		incomingJob = new PCB(jobId, Class, ReqMem, procTime, arrTime);
		//gets the assigned memory slot if one is available
		String memorySlot = requestMemory(incomingJob.ReqMem);
		if(memorySlot!=""){
			//After checking if there is available memory, 
			//it checks if it will maintain job mix;
			if(isOptimal(incomingJob.JobClass)){
				incomingJob.MemorySlot = memorySlot;
				incomingJob.LoadTime = clock.getTime();
				addToReadyQueue(incomingJob);
			}
			else{
				memoryManager.freeMemory(memorySlot);
				addToDisk(incomingJob);
			}
		}
		else{
			addToDisk(incomingJob);
		}
		
	}
	//J_DISPATCH 	: job start routine
	//Instead of simply passing the address of the next job
	//to be processed to another routine,
	//J_DISPATCH is responsible for incrementing the clock 
	//and calling J_Term
	public void J_DISPATCH(){
		
		while(readyQueue.size()>0){
			total++;
			clock.incrementTime(readyQueue.get(0).ProcTime);
			int termTime = clock.getTime();
			int _arrtime = readyQueue.get(0).ArrivalTime;
			int turnTime = termTime - _arrtime;
			int procTime = readyQueue.get(0).ProcTime;
			
			readyQueue.get(0).TermTime = termTime;
			readyQueue.get(0).TurnTime = turnTime;
			readyQueue.get(0).WaitTime = turnTime - procTime;
			avgWaitTime += readyQueue.get(0).WaitTime;
			avgTurnTime += readyQueue.get(0).TurnTime;
			if(readyQueue.get(0).JobClass == 1) totalCPU++;
			else if(readyQueue.get(0).JobClass == 2) totalBal++;
			else totalIO++;
			J_TERM(readyQueue.get(0));
			readyQueue.remove(0);
			//after a job executes, it trys to load a job from disk
			checkDisk();
		}
	}
	//J_TERM  		: job termination routine
	//Prints termination statistics and 
	//calls the memoryManager to free memory
	public void J_TERM(PCB job){
		String a,b,c,d,w, x, y,z;
		a = String.format("%-6d",job.JobId);
		b = String.format("%-7d",job.JobClass);
		c = String.format("%-9d", job.ArrivalTime);
		d = String.format("%-9d",job.LoadTime);
		w = String.format("%-12d", job.TermTime);
		x = String.format("%-11d",job.ProcTime);
		y = String.format("%-11d",job.TurnTime);
		z = String.format("%-8d",job.WaitTime);
		String response = a+b+c+d+w+x+y+z;
		memoryManager.freeMemory(job.MemorySlot);
		if((total%25) == 0)printStat(response);
		if(total%400 == 0){
		  String l,m,n,o;
		  l = "\n\n\n";
		  m ="Id    Class  Arrival  LoadTime Termination";
		  n=" Processing Turnaround Waiting";
		  o= l+m+n;
		  printStat(o);
		}
	}
	//isOptimal is responsible for maintaining a balanced JOB MIX 
	//it takes a jobClass as an argument and it is called by J_SCHED
	public boolean isOptimal(int jobClass){
		
		int cpuboundcount=0;
		int cpubound =1;
		
		if(readyQueue.size()==0){
			return true;
		}
		else{
			for(int i=0; i<readyQueue.size(); i++){
				if(readyQueue.get(i).JobClass==cpubound){
					cpuboundcount++;
				} 
				
			}
			if(cpuboundcount < 13){
				return true;
			}
			else if(cpuboundcount==13 && jobClass == cpubound){
				return false;
			}
		}
		return true;
	}
	
	//CheckDisk trys to load a job from disk after a O job is read from the 
	public void checkDisk(){
		
		if(disk.size()!=0){
			int i=0;
			while(i<disk.size()){
				//gets the returned Memory slot
				String memorySlot=requestMemory(
				disk.get(i).ReqMem);
				//checks if theres available memory
				if(memorySlot!=""){
					//assigns memory slot
					disk.get(i).MemorySlot = memorySlot; 
					disk.get(i).LoadTime = clock.getTime();
					addToReadyQueue(disk.get(i));
					disk.remove(i);
				}
				else i++;
				
			}
		}
	}
	
	//Adds a job to disk
	public void addToDisk(PCB job){
		
		if(disk.size() < 300){//keeps the disk at size 300
			disk.add(job);
		}
		else{
			String response = "The Job with JobId "
			+ job.JobId + " is rejected because the disk is full";
			printStat(response);
		}
	}
	
	//Adds a job to the ready queue
	public void addToReadyQueue(PCB jobToAdd){
		
		readyQueue.add(jobToAdd);
	}
	
	//requestMemory is called by Jsched it asks memory manager
	//for availability of closest memorySlot 
	//to the required memory of an arriving job
	public String requestMemory(int requiredMemory){
				 
		 if(requiredMemory <= 8){
		 	return memoryManager.checkMemory("8k");
		 }
		 else if(requiredMemory <= 12 && requiredMemory >8){
		 	return memoryManager.checkMemory("12k");
		 }
		 else if(requiredMemory <=18 && requiredMemory > 12){
		 	return memoryManager.checkMemory("18k");
		 }
		 else if(requiredMemory <= 32 && requiredMemory >18){
		 	return memoryManager.checkMemory("32k");
		 }
		 else if(requiredMemory <= 52 && requiredMemory > 32){
		 	return memoryManager.checkMemory("52k");
		 }
		 else if(requiredMemory <= 60 && requiredMemory >52){
		 	return memoryManager.checkMemory("60k");
		 }
		 else if(requiredMemory <= 128 && requiredMemory >60){
		 	return memoryManager.checkMemory("128k");
		 }
		return "";
	}
	
	//printStat is used to print individual job termination statistics
	public void printStat(String s){
		try{
        	out = new PrintWriter(new FileWriter(file, true));
            out.append(s + "\n");
            out.close();
            // return;
    	}
        catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	//readJob is used to read an arriving Job
	public void readJob(){
		if(scan.hasNextLine() && scan.hasNextInt()){
			arrivalTime = clock.getTime();
			jobId = scan.nextInt();
			jobClass = scan.nextInt();
			requiredMemory = scan.nextInt();
			processingTime = scan.nextInt();
		}
	}
	
	//calls J_SCHED it is called by the startSystem routine		
	public void callJ_SCHED(){
		J_SCHED(jobId,jobClass,requiredMemory,processingTime
		,arrivalTime);
	}
	
	//Function prints final Job statistics
	public void printFinalStat(int finalTime){
		String a,b,c,d,e,f,g,h;
		avgTurnTime = avgTurnTime/total;
		avgWaitTime = avgWaitTime/total;
		a = String.format("\n\nFinal Termination Statistics \n");
		b = String.format("Total Number of Jobs Processed: %-10d"
		, total);
		c = String.format("\nCPU-bound jobs: %-10d",totalCPU);
		d = String.format("\nBalanced Jobs: %-10d", totalBal);
		e = String.format("\nI/O-bound Jobs: %-10d\n",totalIO);
		f = String.format("Average Turnaround Time: %-10d\n"
		,avgTurnTime);
		g = String.format("Clock Value at Termination: %-10d"
		, finalTime);
		h = String.format("\nAverage Waiting Time: %-10d",avgWaitTime);
		String printFinalStat = a+b+c+d+e+f+g+h;
		printStat(printFinalStat);

	}
	
	//Function prints the rejected jobs
	public void printRejectedJobs(){
		String res;
		printStat("\n\n\n\nRejected Jobs");
		while(disk.size()!=0){
		res = String.format("Id:%-7d is rejected because its requested memory of %-5d is > 128 ",
		disk.get(0).JobId,disk.get(0).ReqMem);
		printStat(res);
		disk.remove(0);
		}
		printStat("\n\n\n");
	}
	
}
//Class defines the MemoryManager object
class MemoryManager{
	//Defines the memory partitions
	Map <String, Boolean> memory = new HashMap<String, Boolean>();
	public MemoryManager(){
		memory.put("8k1",true);
		memory.put("8k2",true);
		memory.put("8k3",true);
		memory.put("8k4",true);
		memory.put("12k1",true);	
		memory.put("12k2",true);	
		memory.put("12k3",true);	
		memory.put("12k4",true);
		memory.put("18k1",true);	
		memory.put("18k2",true);
		memory.put("18k3",true);
		memory.put("18k4",true);
		memory.put("18k5",true);
		memory.put("18k6",true);
		memory.put("32k1",true);	
		memory.put("32k2",true);
		memory.put("32k3",true);
		memory.put("32k4",true);
		memory.put("32k5",true);
		memory.put("32k6",true);
		memory.put("52k",true);
		memory.put("60k1",true);
		memory.put("60k2",true);
		memory.put("60k3",true);
		memory.put("60k4",true);
		memory.put("128k",true);
	}
	
	//Checks for memory
	public String checkMemory(String s){
		for(String name : memory.keySet()){
			if(name.startsWith(s)){
				if(memory.get(name)==true){
					memory.replace(name,false);
					return name;
				}
			}
		}
		return "";
	}
	//Frees Memory
	public void freeMemory(String s){
		memory.replace(s,true);
	}
}
//Class defines the clock object
class Clock{
	
	int time = 0;
	public int getTime(){
		return time;
	}
	public void incrementTime(int procTime){
		time += procTime;
	}
}
//Class defines the PCB object.
class PCB{
	
	int JobId,JobClass,ReqMem,ProcTime,ArrivalTime;
	int LoadTime,TermTime,WaitTime,TurnTime;
	String MemorySlot;
	
	public PCB(int jobId,int jobClass,int reqMem,int procTime,
	int arrivalTime){
		JobId = jobId;
		JobClass = jobClass;
		ReqMem = reqMem;
		ProcTime = procTime;
		ArrivalTime = arrivalTime;
	}
	public PCB(){}
}