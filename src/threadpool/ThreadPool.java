package threadpool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPool {
	private int numberWorkingThreads = 5;
	private ExecutorService executor;
	private boolean executorStarted = false;
	
	public ThreadPool(int numberWorkingThreads) {
		this.numberWorkingThreads = numberWorkingThreads;
	}
	
	public void launchThreadPool() {
		if(this.executorStarted == false) {
	        this.executor = Executors.newFixedThreadPool(numberWorkingThreads);
	        this.executorStarted = true;
		}
	}
	
	
	public void shutdownThreadPool() {
		if(this.executorStarted == true) {
	        executor.shutdown();
	        while (!executor.isTerminated()) {}
	        System.out.println("Finished all threads");
		}
	}
	
	
	// Passa qua i thread worker con dentro le richieste
	public void executeTask(Runnable worker)
	{
		if(worker == null) throw new NullPointerException();   
		executor.execute(worker);
	}
	
}
