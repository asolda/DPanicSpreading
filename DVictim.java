package it.isislab.dmason.sim.app.DPanicSpreading;

import it.isislab.dmason.sim.engine.DistributedState;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import sim.engine.SimState;
import sim.portrayal.DrawInfo2D;
import sim.util.Bag;
import sim.util.Double2D;
import sim.util.Int2D;

public class DVictim extends RemoteVictim<Int2D> {

	private static final long serialVersionUID = 3676850848049449617L;

	int x, y;
	private double health;
	private int AREA_OF_INTEREST;
	private Int2D event;
	
	Color[] healthColors = new Color[11];
	private Map<Int2D, Integer[]> dangerNews = new HashMap<Int2D, Integer[]>();


	private static final boolean RANDOM_MOVEMENTS = false; //the agent move randomly when not in danger
	private int MEMORY; //how long does it remember old informations
	private Double SPEED;

	
	
	public DVictim(DistributedState<Int2D> state, double s, int aoi,int m, int x2, int y2) {
		super(state);
		health = 1.0;
		x=x2;
		y=y2;
		SPEED = s;
		AREA_OF_INTEREST = aoi;
		MEMORY = m;
		event = null;

		for (int i = 0; i < 11; i++) {
			healthColors[i] = new Color(240-i*24,i*24,0);
		}
	}

	@Override
	public void step(SimState state) {
		final DPanicSpreading sim = (DPanicSpreading) state;
		Int2D location = sim.agentGrid.getObjectLocation(this);
		x = location.x;
		y = location.y;
		
		if(x<0 || y < 0 || x > sim.GRID_WIDTH-1 || y > sim.GRID_HEIGHT-1){
			System.out.println("I definitely shouldn't be here");
		}

		if(health <= 0){
			health=0;
			return;
		}



		int zone = sim.dangerGrid.field[x][y];
		if(zone != 0) { 
			Integer[] info = {zone,0};
			dangerNews.put(location, info);
			//gets damaged
			double danger = (double)zone;
			if(sim.random.nextInt(50) < zone) { //prob. inversamente prop. alla distanza
				if (health - danger/10 < 0){
					//sim.deadCounter++;
					health = 0;
				}
				else 
					health -= danger/10; //danno proporzionale alla zona
			}

			//alert neighbours
			Bag neighbours = sim.getNeighbourhood(x, y, AREA_OF_INTEREST);
			for (int n = 0; n < neighbours.size(); n++) {
				((DVictim) neighbours.get(n)).dangerNews.put(location, info);
			}
		}

		int x_mov = 0;
		int y_mov = 0;

		if (!dangerNews.isEmpty()){ //compute escape point
			int resultX = 0;
			int resultY = 0;
			int totalDanger = 0;
			
			Iterator<Entry<Int2D, Integer[]>> iter = dangerNews.entrySet().iterator();
			while(iter.hasNext()){
				Entry<Int2D, Integer[]> entry = iter.next();
				
				resultX+= entry.getKey().x*entry.getValue()[0];
				resultY+= entry.getKey().y*entry.getValue()[0];
				totalDanger+=entry.getValue()[0];
				
				entry.getValue()[1]++;
				if ( entry.getValue()[1]>MEMORY )
					iter.remove();
				else
					dangerNews.put(entry.getKey(), entry.getValue());
			}

			resultX/=totalDanger;
			resultY/=totalDanger;



			//Compute escape direction
			if(resultX - x <= 0) {
				x_mov = (int) (health*SPEED);
			} else {
				x_mov = (int) (-1 * (health*SPEED));
			}
			if(resultY - y <= 0) {
				y_mov = (int) (health*SPEED);
			} else {
				y_mov = (int) (-1 * (health*SPEED));
			}
		} else if (RANDOM_MOVEMENTS){ //Random movements
			if(sim.random.nextBoolean()) {
				x_mov = SPEED.intValue();
			} else {
				x_mov = -1 * SPEED.intValue();
			}
			if(sim.random.nextBoolean()) {
				y_mov = SPEED.intValue();
			} else {
				y_mov = -1 * SPEED.intValue();
			}
		}

		//prevent from going outside borders
		if(x+x_mov < 0 || x+x_mov >= sim.GRID_WIDTH)
			x_mov = 0;
		if(y+y_mov < 0 || y+y_mov >= sim.GRID_HEIGHT)
			y_mov = 0;
		
		
		//move
		sim.agentGrid.setObjectLocation(this, (x= x + x_mov), (y= y + y_mov));
		//clears the map
	}

	public boolean isAware() {
		return event != null;
	}
	
	public boolean isAlive(){
		if (health >0)
			return true;
		else 
			return false;
	}

	public void setAware(int x, int y) {
		event = new Int2D(x, y);
	}

	public final void draw(Object object, Graphics2D graphics, DrawInfo2D info) {
		graphics.setColor(healthColors[(int)(health*10)]);

		int x = (int)(info.draw.x - info.draw.width / 2.0);
		int y = (int)(info.draw.y - info.draw.height / 2.0);

		Double2D a = info.fieldPortrayal.getScale(info);
		int width = (int)(info.draw.width);
		int height = (int)(info.draw.height);
		graphics.fillOval(x,y,5, 5);
	}

}

