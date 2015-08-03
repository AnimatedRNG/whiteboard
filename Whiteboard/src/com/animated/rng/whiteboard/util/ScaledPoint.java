package com.animated.rng.whiteboard.util;

import org.eclipse.swt.graphics.Point;

/**
 * ScaledPoint represents a point on an arbitrarily-sized screen. Contains helper methods
 * to convert to SWT's {@link Point} and back. 
 * 
 * @author Srinivas Kaza
 */
public class ScaledPoint {
	
	public double x;
	public double y;
	
	public ScaledPoint() {
		this.x = 0;
		this.y = 0;
	}
	
	/**
	 * @param x x-coordinate
	 * @param y y-coordinate
	 */
	public ScaledPoint(double x, double y) {
		this.x = x;
		this.y = y;
	}
	
	/**
	 * Don't use this constructor unless you know what you're doing
	 */
	public ScaledPoint(Point p) {
		new ScaledPoint(p.x, p.y);
	}
	
	/**
	 * Converts a {@link Point} to a {@link ScaledPoint} using the canvas's dimensions
	 * 
	 * @param currentDimensions current dimensions of the canvas
	 * @param point {@link Point} to convert to {@link ScaledPoint}
	 * @return {@link ScaledPoint} which is now display-independent
	 */
	public static ScaledPoint toScaledPoint(Point currentDimensions, Point point) {
		return new ScaledPoint(((double) point.x) / ((double) currentDimensions.x), ((double) point.y) / ((double) currentDimensions.y));
	}
	
	/**
	 * Converts a {@link ScaledPoint} to a {@link Point} using the canvas's dimensions
	 * 
	 * @param currentDimensions current dimensions of the canvas
	 * @param scaledPoint {@link ScaledPoint} to convert to {@link Point}
	 * @return {@link Point} which is specific to the display
	 */
	public static Point toSWTPoint(Point currentDimensions, ScaledPoint scaledPoint) {
		return new Point((int) (scaledPoint.x * currentDimensions.x), (int) (scaledPoint.y * currentDimensions.y));
	}
}
