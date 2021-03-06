//Name: Oluwatobiloba Adeyeye
//Course Number: CS 4323
//Assignment Title:Phase 2 Job Scheduler
//Due Date: 03-13-2018

import java.util.*;
import java.io.*;

public class CPU2{
	//incomingJob stores the information of an arriving job
	PCB incomingJob = new PCB();
	LinkedList <PCB> rejected = new LinkedList<PCB>();
	LinkedList <PCB> disk = new LinkedList<PCB>();
	LinkedList <PCB> IOQueue = new LinkedList<PCB>();
	LinkedList <PCB> balancedQueue = new LinkedList<PCB>();
	LinkedList <PCB> CPUQueue = new LinkedList<PCB>();
	MemoryManager memoryManager = new MemoryManager();
	Clock clock = new Clock();
	//store the information to be used to create PCB
	int jobId, jobClass, requiredMemory, processingTime,arrivalTime;
	int internalFrag=0,externalFrag=0; 
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
			y=" Processing Turnaround Waiting Priority Traffic"; 
			printStat(x+y);
			scan = new Scanner(
			// new File("/home/opsys/OS-I/18Sp-jobs"));
			new File("OSjobs"));
			//While loop reads in arriving jobs
			while (scan.hasNextLine() && scan.hasNextInt()){ 
				readJob();
				//Calls JDispatch When memory is full
				//or there is O input
				if(jobId == 0 || (IOQueue.size()+CPUQueue.size()+balancedQueue.size())==26){
					
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
			System.out.println(" Before JDispatch");
			memoryManager.printMem();
			//memoryManager.freeMemory("128k");
			finalShutdown(disk);
			System.out.println("\n\n\n\n After JDispatch");
			memoryManager.printMem();
			int clockVal = clock.getTime();
			printRejectedJobs();
			printFinalStat(clockVal);
			scan.close();
			
		}
		catch(FileNotFoundException ex){
			ex.printStackTrace();
		}
	}
	
	public void J_SCHED(int jobId, int Class,int ReqMem, int procTime
	,int arrTime){
		incomingJob = new PCB(jobId, Class, ReqMem, procTime, arrTime);
		//gets the assigned memory slot if one is available
		String memorySlot = memoryManager.checkMemory(incomingJob.ReqMem);
		if(memorySlot!=""){
			incomingJob.Traffic = 1;
			incomingJob.Priority = setPriority(Class);
			incomingJob.MemorySlot = memorySlot;
			incomingJob.LoadTime = clock.getTime();
			incomingJob.Frag = memoryManager.calcFrag(memorySlot)-ReqMem;
			addToQueue(incomingJob);
		}
		else{
			if(ReqMem > 128){
				addToRejected(incomingJob);
			}else{
				addToDisk(incomingJob);
			}
		}
		
	}
	public int setPriority(int Class){
		if(Class == 3){
			return 5;
		}
		else if(Class == 2){
			return 1;
		}
		return 0;
	}
	//J_DISPATCH 	: job start routine
	//Instead of simply passing the address of the next job
	//to be processed to another routine,
	//J_DISPATCH is responsible for incrementing the clock 
	//and calling J_Term
	public void J_DISPATCH(){
		
		if(IOQueue.size()>0){
			runIOQueue();

		}
		if (balancedQueue.size()>0){
			runBalancedQueue();
		}
		if(CPUQueue.size()>0){
			runCPUQueue();
		}

		
	}
	//J_TERM  		: job termination routine
	//Prints termination statistics and 
	//calls the memoryManager to free memory
	/**
	*EXTERNAL DOCUMENTATION FOR METHOD CALLCPUROUTINE
	*This method calculates implemenst the RR Scheme
	*/
	public void ageQueue(LinkedList <PCB> queue){
		int count =0;
		while(count < queue.size()){
			
				if(queue.get(count).RemTime == queue.get(count).ProcTime){
					if(queue == CPUQueue){	
						if((clock.getTime()-queue.get(count).LoadTime)>=(queue.get(count).Multiplier * 600)){
							queue.get(count).Priority+=1;
							queue.get(count).Traffic+=1;
							queue.get(count).Multiplier+=1;
							balancedQueue.addLast(queue.get(count));
							queue.remove(count);
						}
						else{
							count++;
						}
					}
					if(queue == balancedQueue){
						if((clock.getTime()-queue.get(count).LoadTime)>=(queue.get(count).Multiplier * 400)){
							if(queue.get(count).Priority < 5){
								queue.get(count).Priority+=1;
								queue.get(count).Traffic+=1;
								queue.get(count).Multiplier+=1;
							}
							else{
								IOQueue.addLast(queue.get(count));
								queue.remove(count);
							}
							
							
						}
						else{
							count++;
						}
					}
				}
				else{
					count++;
				}
			
			
		}
	}
	
	public void runIOQueue(){
		
		while(IOQueue.size()!=0){
			//gets the time quantum for the process
			int quantum = getQuantum(IOQueue.get(0).JobClass); 
			if(IOQueue.get(0).JobId == 1594){
				System.out.println("1594 Got here" );
			}
			if(IOQueue.get(0).RemTime < quantum || IOQueue.get(0).RemTime == quantum){
				total++;
				if(IOQueue.get(0).RemTime < quantum){
					clock.incrementTime(IOQueue.get(0).RemTime);
				}
				if(IOQueue.get(0).RemTime == quantum){
					clock.incrementTime(quantum);
				}
				
				
				
				int termTime = clock.getTime();
				int _arrtime = IOQueue.get(0).ArrivalTime;
				int turnTime = termTime - _arrtime;
				int procTime = IOQueue.get(0).ProcTime;
				
				IOQueue.get(0).TermTime = termTime;
				IOQueue.get(0).TurnTime = turnTime;
				IOQueue.get(0).WaitTime = turnTime - procTime;
				avgWaitTime += IOQueue.get(0).WaitTime;
				avgTurnTime += IOQueue.get(0).TurnTime;
				if(IOQueue.get(0).JobClass == 1) totalCPU++;
				else if(IOQueue.get(0).JobClass == 2) totalBal++;
				else totalIO++;
				
				if(IOQueue.get(0).ReqMem > 60){
					System.out.println("Job Id before Termination is "+IOQueue.get(0).JobId+"MemorySlot allocated is"+IOQueue.get(0).MemorySlot);
				}
				J_TERM(IOQueue.get(0));
				
				if(IOQueue.get(0).ReqMem > 60){
					System.out.println(IOQueue.get(0).MemorySlot + " after Termination is" + memoryManager.getMem(IOQueue.get(0).MemorySlot)+"\n\n");
				}
				IOQueue.remove(0);
				//after a job executes, it trys to load a job from disk
				checkDisk();
				ageQueue(balancedQueue);
				ageQueue(CPUQueue);
			}
			else{
				clock.incrementTime(quantum);
				IOQueue.get(0).RemTime -= quantum;
				IOQueue.addLast(IOQueue.get(0));
				IOQueue.remove(0);
				
				ageQueue(balancedQueue);
				ageQueue(CPUQueue);
				
				
				//Decrement priority for balanced and CPUBound jobs
				//Complete ageQueue
				//Compute External Fragmentation
			}
			
			
		}
	}

	public void runCPUQueue(){
		while(CPUQueue.size()!=0){
			//gets the time quantum for the process
			int quantum = getQuantum(CPUQueue.get(0).JobClass); 
			
			if(CPUQueue.get(0).RemTime < quantum || CPUQueue.get(0).RemTime == quantum){
				total++;
				if(CPUQueue.get(0).RemTime < quantum){
					clock.incrementTime(CPUQueue.get(0).RemTime);
				}
				if(CPUQueue.get(0).RemTime == quantum){
					clock.incrementTime(quantum);
				}
				
				int termTime = clock.getTime();
				int _arrtime = CPUQueue.get(0).ArrivalTime;
				int turnTime = termTime - _arrtime;
				int procTime = CPUQueue.get(0).ProcTime;
				
				CPUQueue.get(0).TermTime = termTime;
				CPUQueue.get(0).TurnTime = turnTime;
				CPUQueue.get(0).WaitTime = turnTime - procTime;
				avgWaitTime += CPUQueue.get(0).WaitTime;
				avgTurnTime += CPUQueue.get(0).TurnTime;
				if(CPUQueue.get(0).JobClass == 1) totalCPU++;
				else if(CPUQueue.get(0).JobClass == 2) totalBal++;
				else totalIO++;
				
				if(CPUQueue.get(0).ReqMem > 60){
					System.out.println("Job Id before Termination is "+CPUQueue.get(0).JobId+"MemorySlot allocated is"+CPUQueue.get(0).MemorySlot);
				}
				J_TERM(CPUQueue.get(0));
				if(CPUQueue.get(0).ReqMem > 60){
					System.out.println(CPUQueue.get(0).MemorySlot + " after Termination is" + memoryManager.getMem(CPUQueue.get(0).MemorySlot)+"\n\n");
				}
				
				
				CPUQueue.remove(0);
				//after a job executes, it trys to load a job from disk
				checkDisk();
				ageQueue(balancedQueue);
			}
			else{
				clock.incrementTime(quantum);
				CPUQueue.get(0).RemTime -= quantum;
				if(CPUQueue.get(0).Priority > 0){
					CPUQueue.get(0).Priority -= 1;
				}
				CPUQueue.addLast(CPUQueue.get(0));
				CPUQueue.remove(0);
				ageQueue(balancedQueue);
				
				//Decrement priority for balanced and CPUBound jobs
				//Complete ageQueue
				//Compute External Fragmentation
			}
			
			
		}
	}

	public void runBalancedQueue(){
		
		while(balancedQueue.size()!=0){
			//gets the time quantum for the process
			int quantum = getQuantum(balancedQueue.get(0).JobClass); 
			
			if(balancedQueue.get(0).RemTime < quantum || balancedQueue.get(0).RemTime == quantum){
				total++;
				if(balancedQueue.get(0).RemTime < quantum){
					clock.incrementTime(balancedQueue.get(0).RemTime);
				}
				if(balancedQueue.get(0).RemTime == quantum){
					clock.incrementTime(quantum);
				}
							
				
				int termTime = clock.getTime();
				int _arrtime = balancedQueue.get(0).ArrivalTime;
				int turnTime = termTime - _arrtime;
				int procTime = balancedQueue.get(0).ProcTime;
				
				balancedQueue.get(0).TermTime = termTime;
				balancedQueue.get(0).TurnTime = turnTime;
				balancedQueue.get(0).WaitTime = turnTime - procTime;
				avgWaitTime += balancedQueue.get(0).WaitTime;
				avgTurnTime += balancedQueue.get(0).TurnTime;
				if(balancedQueue.get(0).JobClass == 1) totalCPU++;
				else if(balancedQueue.get(0).JobClass == 2) totalBal++;
				else totalIO++;

				if(balancedQueue.get(0).ReqMem > 60){
					System.out.println("Job Id before Termination is "+balancedQueue.get(0).JobId+"MemorySlot allocated is"+balancedQueue.get(0).MemorySlot);
				}	
				J_TERM(balancedQueue.get(0));
				if(balancedQueue.get(0).ReqMem > 60){
					System.out.println(balancedQueue.get(0).MemorySlot + " after Termination is" + memoryManager.getMem(balancedQueue.get(0).MemorySlot)+"\n\n");
				}

				
				balancedQueue.remove(0);
				//after a job executes, it trys to load a job from disk
				checkDisk();
				ageQueue(CPUQueue);
			}
			else{
				clock.incrementTime(quantum);
				balancedQueue.get(0).RemTime -= quantum;
				if(balancedQueue.get(0).Priority > 1){
					balancedQueue.get(0).Priority -= 1;
				}
				balancedQueue.addLast(balancedQueue.get(0));
				balancedQueue.remove(0);
				ageQueue(CPUQueue);
				
				//Decrement priority for balanced and CPUBound jobs
				//Complete ageQueue
				//Compute External Fragmentation
			}
			
			
		}
	}
	
	public int getQuantum(int Class){
		if(Class == 3) return 20;
		if(Class == 2) return 40;
		if(Class == 1) return 75;
		return 0;
	}

	public void J_TERM(PCB job){

		internalFrag += job.Frag;

		String a,b,c,d,w, x, y,z,k,g;
		a = String.format("%-6d",job.JobId);
		b = String.format("%-7d",job.JobClass);
		c = String.format("%-9d", job.ArrivalTime);
		d = String.format("%-9d",job.LoadTime);
		w = String.format("%-12d", job.TermTime);
		x = String.format("%-11d",job.ProcTime);
		y = String.format("%-11d",job.TurnTime);
		z = String.format("%-8d",job.WaitTime);
		k = String.format("%-9d",job.Priority);
		g = String.format("%-9d",job.Traffic);
		String response = a+b+c+d+w+x+y+z+k+g;
		memoryManager.freeMemory(job.MemorySlot);
		if((total%25) == 0){
			printStat(response);
			externalFrag += memoryManager.stats();
		} 
		if(total%400 == 0){
		  String l,m,n,o;
		  l = "\n\n\n";
		  m ="Id    Class  Arrival  LoadTime Termination";
		  n=" Processing Turnaround Waiting Priority Traffic";
		  o= l+m+n;
		  printStat(o);
		}
	}
	//CheckDisk trys to load a job from disk after a O job is read from the 
	public void checkDisk(){
		
		if(disk.size()!=0){
			int i=0;
			while(i<disk.size()){
				//gets the returned Memory slot
				String memorySlot=memoryManager.checkMemory(
				disk.get(i).ReqMem);
				//checks if theres available memory
				if(memorySlot!=""){
					//assigns memory slot
					disk.get(i).Traffic = 1;
					disk.get(i).Priority = setPriority(disk.get(i).JobClass);
					disk.get(i).MemorySlot = memorySlot; 
					disk.get(i).LoadTime = clock.getTime();
					disk.get(i).Frag = memoryManager.calcFrag(memorySlot)-disk.get(i).ReqMem;
					addToQueue(disk.get(i));
					disk.remove(i);

				}
				else {
					i++;
				}
				
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
	public void addToQueue(PCB jobToAdd){
		if(jobToAdd.JobClass == 3){
			IOQueue.add(jobToAdd);
		}
		if(jobToAdd.JobClass == 2){
			balancedQueue.add(jobToAdd);
		}
		if(jobToAdd.JobClass == 1){
			CPUQueue.add(jobToAdd);
		}
		
	}
	public void addToRejected(PCB job){
		rejected.add(job);
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
		String a,b,c,d,e,f,g,h,i,j;
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
		i = String.format("\nTotal Internal Fragmentation: %-10d", internalFrag);
		j = String.format("\nTotal External Fragmentation: %-10d", externalFrag);
		String printFinalStat = a+b+c+d+e+f+g+h+i+j;
		printStat(printFinalStat);

	}
	public void printRejectedJobs(){
		String res;
		printStat("\n\n\n\nRejected Jobs");
		while(rejected.size()!=0){
		res = String.format("Id:%-7d is rejected because its requested memory of %-5d is > 128 ",
		rejected.get(0).JobId,rejected.get(0).ReqMem);
		printStat(res);
		rejected.remove(0);
		}
		printStat("\n\n\n");
	}
	
public void finalShutdown(LinkedList <PCB> queue){
		String memorySlot = "128k";
		while(disk.size()!=0){
			disk.get(0).Traffic = 1;
			disk.get(0).Priority = setPriority(disk.get(0).JobClass);
			disk.get(0).MemorySlot = "128k"; 
			disk.get(0).LoadTime = clock.getTime();
			disk.get(0).Frag = memoryManager.calcFrag(memorySlot)-disk.get(0).ReqMem;
			addToQueue(disk.get(0));
			disk.remove(0);
			
		}
		J_DISPATCH();
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

	public int stats(){
		int sum = 0;
		for(String s : memory.keySet()){
			if(memory.get(s)==true){
				sum += calcFrag(s);
			}
		}
		return sum;
	}
	
	
		//requestMemory is called by Jsched it asks memory manager
	//for availability of closest memorySlot 
	//to the required memory of an arriving job


	public void printMem(){
		for(String name : memory.keySet()){

			System.out.printf("%-7s", name + " " + memory.get(name) + "\n");
			
		}
		
	}
	public boolean getMem(String s){
		return memory.get(s);
	}


	public String requestMemory(int requiredMemory){
				 
		 if(requiredMemory <= 8){
		 	return "8k";
		 }
		 else if(requiredMemory <= 12 && requiredMemory > 8){
		 	return "12k";
		 }
		 else if(requiredMemory <=18 && requiredMemory > 12){
		 	return "18k";
		 }
		 else if(requiredMemory <= 32 && requiredMemory >18){
		 	return "32k";
		 }
		 else if(requiredMemory <= 52 && requiredMemory > 32){
		 	return "52k";
		 }
		 else if(requiredMemory <= 60 && requiredMemory > 52){
		 	return "60k";
		 }
		 else if(requiredMemory <= 128 && requiredMemory > 60){
		 	return "128k";
		 }
		return "";
	}
	//This method computes internal 
	public int calcFrag(String s){
		
		int k_index = s.indexOf('k');
		int value = Integer.parseInt(s.substring(0,k_index));
		return value;
	}
	//Checks for memory
	public String checkMemory(int s){
		String value = requestMemory(s);
		if(value=="") return "";
		for(String name : memory.keySet()){
			if(name.startsWith(value)){
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
		if(s == "128k"){
			System.out.println("128k before releasing memory" + memory.get(s));
		}
		memory.replace(s,true);
		if(s == "128k"){
			System.out.println("128k after releasing memory" + memory.get(s));
		}
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
	int Frag,Priority,Multiplier,Traffic;
	String MemorySlot;
	int RemTime;
	
	public PCB(int jobId,int jobClass,int reqMem,int procTime,
	int arrivalTime){
		JobId = jobId;
		JobClass = jobClass;
		ReqMem = reqMem;
		ProcTime = procTime;
		RemTime = procTime;
		ArrivalTime = arrivalTime;
	}
	public PCB(){}
}