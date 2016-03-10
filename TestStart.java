package it.isislab.dmason.sim.app.DPanicSpreading;

import it.isislab.dmason.sim.engine.DistributedState;
import it.isislab.dmason.sim.field.continuous.DContinuous2DFactory;
import it.isislab.dmason.tools.batch.data.GeneralParam;
import it.isislab.dmason.util.connection.ConnectionType;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import sim.display.Console;

public class TestStart {

	private static boolean graphicsOn=false; //with or without graphics?
	private static int numSteps = 100; //only graphicsOn=false
	private static int areaOfInterest = 10;
	private static int rows = 1; //number of rows
	private static int columns = 4; //number of columns
	private static int MAX_DISTANCE=1; //max distance
	private static int NUM_AGENTS=1000; //number of agents
	private static int WIDTH=1000; //field width
	private static int HEIGHT=1000; //field height
	private static String ip="127.0.0.1"; //ip of activemq
	private static String port="61616"; //port of activemq

	private static AtomicInteger cc = new AtomicInteger(0);

	//don't modify this...
	private static int MODE = (rows==1 || columns==1)? 
			DContinuous2DFactory.HORIZONTAL_DISTRIBUTION_MODE : DContinuous2DFactory.SQUARE_DISTRIBUTION_MODE; 
	//private static int MODE = (rows==1 || columns==1)? 
	//DContinuous2DFactory.HORIZONTAL_BALANCED_DISTRIBUTION_MODE :
	//DContinuous2DFactory.SQUARE_BALANCED_DISTRIBUTION_MODE; 

	public static void main(String[] args) 
	{		

		class worker extends Thread
		{
			private DistributedState ds;
			public worker(DistributedState ds, AtomicInteger cc) {
				this.ds=ds;
				ds.start();
			}
			@Override
			public void run() {

				int i=0;
				while(i!=numSteps)
				{
					System.out.println(i);
					ds.schedule.step(ds);
					i++;
				}
				cc.incrementAndGet();
			}
		}

		if(args.length != 1) {
			System.out.println("Usage error");
			System.exit(1);
		}

		BufferedReader br = new BufferedReader(new FileReader(args[0]));
		BufferedWriter bw = new BufferedWriter(new FileWriter(args[0]+"_results"));
		String line;
		while((line = br.readLine()) != null) {
			if(line.contains("test")) {
				bw.write(line + " results:\n");
			} else {
				String params[] = line.split(" ");
				numSteps = Integer.parseInt(params[0]);
				areaOfInterest = Integer.parseInt(params[1]);
				NUM_AGENTS = Integer.parseInt(params[2]);
				
				DPanicSpreading sim = null;
				
				ArrayList<worker> myWorker = new ArrayList<worker>();
				for (int i = 0; i < rows; i++) {
					for (int j = 0; j < columns; j++) {

						GeneralParam genParam = new GeneralParam(WIDTH, HEIGHT, MAX_DISTANCE, rows,columns,NUM_AGENTS, MODE,ConnectionType.pureActiveMQ); 
						genParam.setI(i);
						genParam.setJ(j);
						genParam.setIp(ip);
						genParam.setPort(port);
						//				if(graphicsOn)
						//				{
						////					DParticlesWithUI sim =new DParticlesWithUI(genParam);
						//					((Console)sim.createController()).pressPause();
						//				}
						//				else
						//				{
						sim = new DPanicSpreading(genParam,numSteps,areaOfInterest); 
						worker a = new worker(sim, cc);
						myWorker.add(a);
						//				}
					}
				}
				if(!graphicsOn)
					for (worker w : myWorker) {
						w.start();
					}

				while(cc.get() != rows*columns) {

				}

				bw.write("Alive nodes: " + sim.getAliveNodes() + "\n");
			}

		}
	}
}