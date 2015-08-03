package com.animated.rng.whiteboard.update;

import com.animated.rng.whiteboard.Whiteboard;
import com.animated.rng.whiteboard.util.PointerMark;

/**
 * Updates the {@link Whiteboard}'s {@link PointerMark}
 * 
 * @author Srinivas Kaza
 */
public class PointerUpdate extends WhiteboardUpdate {

	private PointerMark mark;
	
	public PointerUpdate() {
		
	}
	
	/**
	 * @param mark {@link PointerMark} to use
	 */
	public PointerUpdate(PointerMark mark) {
		this.mark = mark;
	}
	
	@Override
	public void onUpdate(Whiteboard whiteboard) {
		whiteboard.setPointer(mark);
	}

}
