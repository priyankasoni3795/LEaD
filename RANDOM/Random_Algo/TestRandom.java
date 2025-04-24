package Random_Algo;




import java.util.*;
import java.lang.Math;

public class TestRandom
{
  
  static class fogNode
  {
    double computational_power_of_fogNode;
    double inputSize;
    double computational_demand;
    double energy;
    double quota;
  }
  static class iotDevice
  {
    double computational_demand;
    int Number_of_depenedent_device = 0;
    double energy;
    double latency;
  }

//  public static void MatchingAlgo (iotDevice[]iot, fogNode[]fog)
//  {
//    //we are implementing one to many like every iot task will be having multiple options to go 
//    int total_number_of_iot_tasks = iot.length;
//    //storing the matching of iot, fog 
//    int total_number_of_fog_nodes = fog.length;
//    HashMap < Integer, Integer > mapping = new HashMap < Integer, Integer > ();
//    //to check weatrher the task went to any fog device or any fog assigned or not to that particular task
//    int flag = 0;
//    for (int i = 0; i < total_number_of_iot_tasks; i++)
//	 {
//		flag = 0;		//reinitializing the flag 0 to track again
//	
//		for (int j = 0; j < total_number_of_fog_nodes; j++)
//		  {
//		    if (fog[j].quota > 0)
//		      {
//				fog[j].quota = fog[j].quota - 1;
//				mapping.put (i+1, j+1);
//				flag = 1;	//to track that is the iot task assigned to a fog node or node
//		      }
//		    if (flag == 1)	//if task is assigned according to preference then breaking the loop and going on next task
//		      break;
//	
//		  }
//		if (flag == 0)
//		  {
//			mapping.put (i+1, -1);
//		  }
//     }
//  }
  
  public static void MatchingAlgo (iotDevice[]iot, fogNode[]fog) {
	    int total_number_of_iot_tasks = iot.length;
	    int total_number_of_fog_nodes = fog.length;
	    HashMap <Integer, Integer> mapping = new HashMap <Integer, Integer>();
	    int outages = 0; //initialize the outages counter to 0
	    for (int i = 0; i < total_number_of_iot_tasks; i++) {
	        boolean is_assigned = false; //initialize the flag for each task to false
	        for (int j = 0; j < total_number_of_fog_nodes; j++) {
	        	int devices = iot[i].Number_of_depenedent_device;
	        	System.out.println(i +" "+ devices);
	            if (fog[j].quota > 0 && fog[j].quota > devices ) {
	            	if(devices > 0) {
	            		fog[j].quota = fog[j].quota - devices;
	            	}
	            	else {
	            		fog[j].quota = fog[j].quota - 1;
	            	}
	                
	                mapping.put(i+1, j+1);
	                is_assigned = true; //set the flag to true if task is assigned to a fog node
	                break;
	            }
	        }
	        if (!is_assigned) { //if the task is not assigned, increment the outages counter
	            mapping.put(i+1, -1);
	            outages++;
	        }
	    }
	    System.out.println("Number of outages: " + outages);
	}

  

  public static fogNode[] calculationFog (double output_size, double capacity_of_VRU, double bandwidth)
  {
    fogNode[]fog;
    Scanner sc = new Scanner (System.in);	//System.in is a standard input stream  
    System.out.println ("Enter the number of fogNodes");
    int n = sc.nextInt ();
    fog = new fogNode[n];
    for (int i = 0; i < n; i++){
		fog[i] = new fogNode ();
		System.out.println ("enter the following for fog node:" + (i + 1));
		System.out.println ("computational power  of fognode");
		fog[i].computational_power_of_fogNode = sc.nextDouble ();
		System.out.println ("enter quota of the fognde:: ");
		fog[i].quota = sc.nextDouble ();
	
		double upload_latency = fog[i].inputSize / (bandwidth * 1000);
		double download_latency = output_size / (bandwidth * 1000);
		double computational_latency = (fog[i].computational_demand ) / capacity_of_VRU;
		fog[i].energy = (upload_latency + download_latency + computational_latency) * fog[i].computational_power_of_fogNode;
    }
    return fog;
  }


  public static iotDevice[] calculationIoT (double bandwidth,  double capacity_of_VRU)
  {
    Scanner sc = new Scanner (System.in);
    double power_of_fogDevice = 1.5;
    double computational_power_of_fognode = 0.35;
    iotDevice[]iot;
    
    System.out.println ("\nEnter the number of devices : ");
    int n = sc.nextInt ();
    iot = new iotDevice[n];
//    for (int i = 0; i < n; i++)
//	 {
//		iot[i] = new iotDevice ();
//		System.out.println ("enter computational demand of  task :: " + (i + 1));
//		iot[i].computational_demand = sc.nextDouble ();
//		System.out.println ("enter inputSize of " + " task:: " + (i + 1));
//		iot[i].inputSize = sc.nextDouble ();
//		iot[i].latency = (iot[i].inputSize / (bandwidth * 1000) +	//upload
//				      (iot[i].computational_demand) /  capacity_of_VRU +	//computational
//				      (10 / (bandwidth * 1000)));	//download
//		iot[i].energy = power_of_fogDevice * (iot[i].inputSize /
//					    (bandwidth * 1000) +
//					    10 / (bandwidth * 1000)) +
//		      computational_power_of_fognode * (iot[i].computational_demand) /  capacity_of_VRU;
//	}
//    
    double power_of_iot_device = 0.75; 
    
    //For Dependent Tasks
    for (int i = 0; i < 4; i++)
    {	

    	iot[i] = new iotDevice ();
    	System.out.println ("enter computational demand of " + (i + 1) + " task in Million cycles ::" );
    	iot[i].computational_demand = sc.nextDouble ();
    	System.out.println ("Enter Number of depenedent devices" + (i + 1) + " task ::" );
    	iot[i].Number_of_depenedent_device = sc.nextInt ();
    	
        double computationTime = (iot[i].computational_demand) / (capacity_of_VRU);
        double transmissionTime = 2*iot[i].computational_demand / (bandwidth*8); 			
        
    	iot[i].latency = transmissionTime + computationTime;
    	iot[i].energy = power_of_iot_device * iot[i].latency;
	}
    
    //for Independent tasks
    for (int i = 4; i < n; i++)
    {	

    	iot[i] = new iotDevice ();
    	System.out.println ("enter computational demand of " + (i + 1) + " task in Million cycles ::" );
    	iot[i].computational_demand = sc.nextDouble ();
    	
        double computationTime = (iot[i].computational_demand) / (capacity_of_VRU);
        double transmissionTime = 2*iot[i].computational_demand / (bandwidth*8); 			
        
    	iot[i].latency = transmissionTime + computationTime;
    	iot[i].energy = power_of_iot_device * iot[i].latency;
	}
    return iot;
  }

  public static void main (String[]args)
	{
	    Scanner sc = new Scanner (System.in);
	    
	    System.out.println ("enter channel frequency(bandwidth) in megahetz::");
	    double bandwidth = 10; //sc.nextDouble ();
	    double fog_node_capacity = 6 * Math.pow(10, 9);
	    double temp_quota = 50.0;
	    double capacity_of_VRU = fog_node_capacity/temp_quota ;
	    
	    //calling function to rank fognodes 
	    fogNode[]fog;
	    iotDevice[]iot;
	    //calling fog preferrence to get the order of fog nodes 
	    fog = calculationFog (10, capacity_of_VRU, bandwidth);
	    //calling iot preferrence to get the order of iot devices
	    iot = calculationIoT (bandwidth, capacity_of_VRU);
	    
	    //Calling Matching Theory algorithm
	    MatchingAlgo (iot, fog);
	
	
	    //printing total energies
	//    System.out.println("Printing Energy values for Fog Nodes  ---------->");
	    double total_energy = 0.0;
	    int no_fog_nodes = fog.length;
	    for(int i=0; i<no_fog_nodes; i++) {
	    	total_energy += fog[i].energy;
	    }
	
	    int no_iots = iot.length;
	    System.out.println("No of IoT : "+no_iots);
	    for(int i=0; i<no_iots; i++) {
	    	total_energy += iot[i].energy;
	    }
	    System.out.println("\nTotal Energy for " + no_iots +" devices = "+ total_energy);
	    
	    double total_latency = 0.0;
	    for(int i=0; i<no_iots; i++) {
	    	total_latency += iot[i].latency;
	    }
	    System.out.println("\nTotal Latency for " + no_iots +" devices = "+ total_latency);
	    
	    sc.close();
	  }
}