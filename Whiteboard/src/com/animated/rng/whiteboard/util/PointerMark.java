package com.animated.rng.whiteboard.util;


/**
 * Represents the pointer at a certain position on the screen, facing either left or right
 * 
 * @author Srinivas Kaza
 */
public class PointerMark {

	private boolean direction;
	private ScaledPoint position;
	
	public PointerMark() {
		
	}
	
	/**
	 * @param direction whether pointer is flipped or not
	 * @param position position of pointer on canvas
	 */
	public PointerMark(boolean direction, ScaledPoint position) {
		this.setFlipped(direction);
		this.position = position;
	}

	/**
	 * @return whether pointer is flipped or not
	 */
	public boolean isFlipped() {
		return direction;
	}

	/**
	 * @param flipped whether pointer is flipped or not
	 */
	public void setFlipped(boolean flipped) {
		this.direction = flipped;
	}

	/**
	 * @return the position of the pointer
	 */
	public ScaledPoint getPosition() {
		return position;
	}

	/**
	 * @param position the position of the pointer
	 */
	public void setPosition(ScaledPoint position) {
		this.position = position;
	}
}
