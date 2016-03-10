package it.isislab.dmason.sim.app.DPanicSpreading;

public class DangerEvent {
	
	public int danger;
	public int startTime;
	public int stopTime;
	
	public DangerEvent(int danger, int start,int stop){
		this.danger = danger;
		startTime = start;
		stopTime = stop;
	}	
}
