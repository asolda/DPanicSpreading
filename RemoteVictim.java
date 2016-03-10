package it.isislab.dmason.sim.app.DPanicSpreading;

import it.isislab.dmason.sim.engine.DistributedState;
import it.isislab.dmason.sim.engine.RemotePositionedAgent;

import java.io.Serializable;

public abstract class RemoteVictim<E> implements Serializable, RemotePositionedAgent<E>{
	private static final long serialVersionUID = 1L;
	public String id; //id remote agent.An id uniquely identifies the agent in the distributed-field
	public E pos;     // Location of agents  
	
	public RemoteVictim() {}
	
	public RemoteVictim(DistributedState<E> state) {
		int i = state.nextId();
		this.id = state.getType().toString()+"-"+i;
	}
	
	//getters and setters
    @Override
	public E getPos() { return pos; }
    @Override
	public void setPos(E pos) { this.pos = pos; }
    @Override
	public String getId() {return id;	}
    @Override
	public void setId(String id) {this.id = id;}	
}
