package it.isislab.dmason.sim.app.DPanicSpreading;

import it.isislab.dmason.exception.DMasonException;
import it.isislab.dmason.sim.engine.DistributedMultiSchedule;
import it.isislab.dmason.sim.engine.DistributedState;
import it.isislab.dmason.sim.engine.RemotePositionedAgent;
import it.isislab.dmason.sim.field.DistributedField;
import it.isislab.dmason.sim.field.grid.numeric.DIntGrid2D;
import it.isislab.dmason.sim.field.grid.numeric.DIntGrid2DFactory;
import it.isislab.dmason.sim.field.grid.sparse.DSparseGrid2D;
import it.isislab.dmason.sim.field.grid.sparse.DSparseGrid2DFactory;
import it.isislab.dmason.tools.batch.data.GeneralParam;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.grid.Grid2D;
import sim.util.Bag;
import sim.util.Int2D;
import sim.util.IntBag;

public class DPanicSpreading extends DistributedState<Int2D> {

	private static final long serialVersionUID = -3506386867151262065L;

	//Simulation general parameters
	public static int GRID_HEIGHT = 600;
	public static int GRID_WIDTH = 600;
	public int numAgents =100;

	//Events Paramters
	//startEvent(Int2D eventPosition, int maxDanger, int spread, int timeForSpreading, int nDangerZones, int duration, int delay){
	public ArrayList<Int2D> posistions = new ArrayList<Int2D>();
	public ArrayList<Integer> dangers = new ArrayList<Integer>();
	public ArrayList<Integer> spreads = new ArrayList<Integer>();
	public ArrayList<Integer> timesForSpreading = new ArrayList<Integer>();
	public ArrayList<Integer> nDangerZones = new ArrayList<Integer>();
	public ArrayList<Integer> durations = new ArrayList<Integer>();
	public ArrayList<Integer> delays = new ArrayList<Integer>();


	//Victim parameters
	private int AREA_OF_INTEREST = 10;
	private double VICTIM_SPEED = 5;
	private int MEMORY = 30;
	private int NUM_STEPS;

	public static volatile int deadCounter = 0;


	public static String topicPrefix = "";
	private boolean  checkAgentDuplication = false;
	
	private PrintStream vps = null;
	private boolean verbose = true;
	
	private FileOutputStream file = null;
	private PrintStream ps = null;


	public DSparseGrid2D agentGrid;
	public DSparseGrid2D eventGrid;
	public DIntGrid2D dangerGrid;


	public DPanicSpreading() {
		super();	
	}

	public DPanicSpreading(GeneralParam params, int numSteps,int aoi)
	{    	
		super(params,new DistributedMultiSchedule<Int2D>(),topicPrefix,params.getConnectionType());
		this.MODE=params.getMode();
		GRID_WIDTH=params.getWidth();
		GRID_HEIGHT=params.getHeight();
		NUM_STEPS = numSteps;
		AREA_OF_INTEREST = aoi;
		numAgents=params.getNumAgents();
		//((DistributedMultiSchedule)schedule).setThresholdMerge(1);
		//((DistributedMultiSchedule)schedule).setThresholdSplit(5);
		if(verbose)
		{
			try {
				file = new FileOutputStream(super.TYPE+"_aoi"+AREA_OF_INTEREST+"_na"+numAgents+"_ns"+NUM_STEPS+"_"+GRID_WIDTH+"x"+GRID_WIDTH+".txt");
				ps = new PrintStream(file);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if(checkAgentDuplication)
		{
			try {
				file = new FileOutputStream("0) "+super.TYPE+".txt");
				ps = new PrintStream(file);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	} 

	public Bag getNeighbourhood(int x, int y, int d){
		IntBag xPos = new IntBag();

		IntBag yPos = new IntBag();
		Bag result = new Bag();
		agentGrid.getRadialNeighbors(x, y, d, Grid2D.BOUNDED, false, result, xPos, yPos);
		return result;
	}

	public void initializeEvents(){
		int offsetX = 0, offsetY = 0;
		int posX = 0, posY = 0; 

		String s = "100 100 10 400 30 10 100 10\n"
				+ "100 -100 10 400 30 10 100 10\n"
				+ "-100 100 10 400 30 10 100 10\n"
				+ "-100 -100 10 400 30 10 100 10";
		

		String line[] = s.split("\n");

		for (int i = 0; i < line.length; i++) {
			String[] infos = line[i].split(" ");
			offsetX = Integer.parseInt(infos[0]);
			offsetY = Integer.parseInt(infos[1]);
			if(offsetX > 0)
				posX = offsetX;
			else
				posX = GRID_WIDTH + offsetX;
			if(offsetY > 0)
				posY = offsetY;
			else
				posY = GRID_WIDTH + offsetY;

			posistions.add(new Int2D(posX,posY));
			dangers.add(Integer.parseInt(infos[2]));
			spreads.add(Integer.parseInt(infos[3]));
			timesForSpreading.add(Integer.parseInt(infos[4]));
			nDangerZones.add(Integer.parseInt(infos[5]));
			durations.add(Integer.parseInt(infos[6]));
			delays.add(Integer.parseInt(infos[7]));
		}
	}

	public void start() {
		super.start();
		try{
			agentGrid = DSparseGrid2DFactory.createDSparseGrid2D(GRID_WIDTH, GRID_HEIGHT, this, super.MAX_DISTANCE, TYPE.pos_i, TYPE.pos_j, super.rows, super.columns, MODE, "agents",topicPrefix,false);
			eventGrid = DSparseGrid2DFactory.createDSparseGrid2D(GRID_WIDTH, GRID_HEIGHT, this, super.MAX_DISTANCE, TYPE.pos_i, TYPE.pos_j, super.rows, super.columns, MODE, "events",topicPrefix,false);
			dangerGrid = DIntGrid2DFactory.createDIntGrid2D(GRID_WIDTH, GRID_HEIGHT, this, super.MAX_DISTANCE, TYPE.pos_i, TYPE.pos_j, super.rows, super.columns, MODE, 0, true, "danger", topicPrefix, false);
			
		} catch (DMasonException e){ e.printStackTrace();}
		init_connection();



		initializeEvents();

		//schedule all agents
		for(int i = 0; i < numAgents; i++) {
			int x = random.nextInt(GRID_WIDTH);
			int y = random.nextInt(GRID_HEIGHT);
			DVictim a = new DVictim(this,VICTIM_SPEED, AREA_OF_INTEREST,MEMORY,x,y);
			agentGrid.setObjectLocation(a,x,y);
			schedule.scheduleRepeating(a);
		}

		//updates the dangerGrid
		schedule.scheduleRepeating(new Steppable()
		{
			public void step(SimState state) {
				int totalDanger;
				double timeStep =state.schedule.getTime();
				for (int x = 0; x < GRID_WIDTH; x++) {
					for (int y = 0; y < GRID_HEIGHT; y++) {
						Bag events = eventGrid.getObjectsAtLocation(x,y);
						totalDanger = 0;
						if( events!=null){
							for (int e = 0; e < events.size(); e++) { //iterate through all events on location

								DangerEvent de = (DangerEvent)events.get(e);
								if (timeStep > de.stopTime) { //if event is passed, remove it
									//eventGrid.removeObjectAtLocation(de, new Int2D(x, y));
								} else if (timeStep > de.startTime) { //if event is happening, increase sum
									totalDanger+= de.danger;
								}
								//else event has not started yet nor it has finished
							}
						}
						dangerGrid.set(x, y, totalDanger);
					}	
				}

				//if(state.schedule.getTime()%100==0)
				//	System.out.println("Step "+state.schedule.getTime()+" | Alive Nodes "+(numAgents-deadCounter)+"/"+numAgents);
				
			}
		});	
		schedule.scheduleOnce(NUM_STEPS-1, new Steppable() {
			@Override
			public void step(SimState arg0) {
				int deadC = 0;
				Bag b = agentGrid.getAllObjects();
				for (int i = 0; i < b.size(); i++) {
					if (!((DVictim)b.get(i)).isAlive())
						deadC++;
						
				}
				//System.out.println(deadC+"/"+b.size());
				ps.println(deadC+"/"+b.size());
			}
		});
		for (int i = 0; i < posistions.size(); i++) {
			startEvent(posistions.get(i), dangers.get(i), spreads.get(i), timesForSpreading.get(i), nDangerZones.get(i), durations.get(i), delays.get(i));
		}


	}


	public void startEvent(Int2D eventPosition, int maxDanger, int spread, int timeForSpreading, int nDangerZones, int duration, int delay){
		int dangerZone = 0;
		int distanceFromEvent = 0;
		int start = 0, end =0;
		for (int yCoord = 0; yCoord < GRID_HEIGHT; yCoord++) {
			for (int xCoord = 0; xCoord < GRID_WIDTH; xCoord++) {
				Double dfe = Math.sqrt(Math.pow(Math.abs(xCoord-eventPosition.x),2) +Math.pow(Math.abs(yCoord-eventPosition.y),2));
				distanceFromEvent = dfe.intValue();
				//distanceFromEvent = Math.abs(xCoord-eventX) + Math.abs(yCoord - eventY);
				if (distanceFromEvent < spread) {
					dangerZone =maxDanger- distanceFromEvent / (spread/nDangerZones);
					start = (int) (delay+this.schedule.getSteps()+timeForSpreading*(nDangerZones-dangerZone));
					end = start+ duration*dangerZone;
					DangerEvent de = new DangerEvent(dangerZone, start, end);
					eventGrid.setObjectLocation(de, xCoord, yCoord);

					//System.out.println(xCoord+","+yCoord+" with danger "+de.danger+" from "+start+" to "+(start+duration));
				}
			}
		}
	}

	public static void main(String[] args)
	{
		doLoop(DPanicSpreading.class, args);
		System.exit(0);
	}

	@Override
	public DistributedField<Int2D> getField() {
		return agentGrid;
	}

	@Override
	public void addToField(RemotePositionedAgent<Int2D> rm, Int2D loc) {
		agentGrid.setObjectLocation(rm, loc);
	}

	@Override
	public SimState getState() {
		return this;
	}

	@Override
	public boolean setPortrayalForObject(Object o) {
		return false;
	}
	
	public int getAliveNodes() {
		int deadC = 0;
		Bag b = agentGrid.getAllObjects(); 
		
		for(int i = 0; i < b.size(); i++) {
			if(!(DVictim)b.get(i).isAlive())
				deadC++;
		}
		
		return deadC;
	}
}

