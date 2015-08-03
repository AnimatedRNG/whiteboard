package com.animated.rng.whiteboard.update;

import com.animated.rng.whiteboard.Whiteboard;
import com.animated.rng.whiteboard.util.ScaledPoint;

/**
 * Updates {@link Whiteboard}'s pencil marks
 * 
 * @author Srinivas Kaza
 */
public class PencilUpdate extends WhiteboardUpdate {

	private ScaledPoint lastPos;
	private ScaledPoint newPos;
	
	public PencilUpdate() {
		this.lastPos = new ScaledPoint();
		this.newPos = new ScaledPoint();
	}
	
	/**
	 * @param lastPos last position of pencil
	 * @param newPos new position of pencil 
	 */
	public PencilUpdate(ScaledPoint lastPos, ScaledPoint newPos) {
		this.lastPos = lastPos;
		this.newPos = newPos;
	}

	@Override
	public void onUpdate(Whiteboard whiteboard) {
		whiteboard.addPencilMark(new double[] {lastPos.x, newPos.x, lastPos.y, newPos.y});
	}
}
