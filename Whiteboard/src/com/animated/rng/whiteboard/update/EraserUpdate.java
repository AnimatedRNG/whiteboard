package com.animated.rng.whiteboard.update;

import com.animated.rng.whiteboard.Whiteboard;
import com.animated.rng.whiteboard.util.ScaledPoint;

/**
 * An update that erases a mark at a certain position with a certain radius
 * 
 * @author Srinivas Kaza
 */
public class EraserUpdate extends WhiteboardUpdate {

	private static final double RADIUS = 0.025;
	private ScaledPoint pos;
	
	public EraserUpdate() {
		this.pos = new ScaledPoint();
	}
	
	/**
	 * @param pos the position upon which to perform the erasure
	 */
	public EraserUpdate(ScaledPoint pos) {
		this.pos = pos;
	}
	
	@Override
	public void onUpdate(Whiteboard whiteboard) {
		whiteboard.eraseMarks(pos, RADIUS);
	}
}
