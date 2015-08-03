package com.animated.rng.whiteboard.update;

import com.animated.rng.whiteboard.Whiteboard;


/**
 * Base class for updates that can be performed on {@Whiteboard}. Can be sent over the network.
 * 
 * @author Srinivas Kaza
 */
public abstract class WhiteboardUpdate {

	public static int UPDATE_NUM = 0;
	
	public int connectionID;
	
	private boolean usesGUI;
	
	/**
	 * @return whether or not the update should be performed on the GUI thread asynchronously
	 */
	public boolean usesGUI() {
		return usesGUI;
	}

	/**
	 * @param usesGUI whether or not the update should be performed on the GUI thread asynchronously
	 */
	public void setUsesGUI(boolean usesGUI) {
		this.usesGUI = usesGUI;
	}

	/**
	 * Performs this update on a {@link Whiteboard}
	 * 
	 * @param whiteboard {@code Whiteboard} upon which to perform this update
	 */
	public void update(Whiteboard whiteboard) {
		this.onUpdate(whiteboard);
		UPDATE_NUM++;
	}
	
	/**
	 * Base classes should use this callback to perform updates. If usesGUI is true,
	 * updates will be performed on the GUI thread rather than on the networking thread.
	 * 
	 * @param whiteboard {@link Whiteboard} upon which to perform the update
	 */
	public abstract void onUpdate(Whiteboard whiteboard);
}
