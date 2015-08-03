package com.animated.rng.whiteboard.util;

import java.util.UUID;

import org.eclipse.swt.graphics.Point;

/**
 * Represents an image on the canvas, its position, and boundaries. Note that we just
 * send the image URL rather than the byte array for the actual image.
 * 
 * @author Srinivas Kaza
 */
public class ImageMark {

	public static final double MAX_DIMENSION = 0.5;
	public Long id;
	
	private ScaledPoint position;
	private String imageURl;
	private ScaledPoint boundaries;
	
	public ImageMark() {
		
	}
	
	/**
	 * @param position position of the {@link ImageMark} on the canvas
	 * @param imageURL String representation of the URL
	 * @param boundaries boundaries of the {@link ImageMark}
	 */
	public ImageMark(ScaledPoint position, String imageURL, ScaledPoint boundaries) {
		this(position, imageURL, boundaries, UUID.randomUUID().getMostSignificantBits());
	}
	
	/**
	 * @param position position of the {@link ImageMark} on the canvas
	 * @param imageURL String representation of the URL
	 * @param boundaries boundaries of the {@link ImageMark}
	 * @param id ID of the ImageMark (use this instead of a random ID)
	 */
	public ImageMark(ScaledPoint position, String imageURL, ScaledPoint boundaries, Long id) {
		this.setPosition(position);
		this.setImageURL(imageURL);
		this.setBoundaries(boundaries);
		this.id = id;
	}

	/**
	 * @return the position of the {@link ImageMark}
	 */
	public ScaledPoint getPosition() {
		return position;
	}

	/**
	 * @param position the position of the {@link ImageMark}
	 */
	public void setPosition(ScaledPoint position) {
		this.position = position;
	}

	/**
	 * @return the {@link ImageMark} URL
	 */
	public String getImageURL() {
		return imageURl;
	}

	/**
	 * @param imageURL the {@link ImageMark} URL
	 */
	public void setImageURL(String imageURL) {
		this.imageURl = imageURL;
	}
	
	/**
	 * @return boundaries of the {@link ImageMark}
	 */
	public ScaledPoint getBoundaries() {
		return boundaries;
	}

	/**
	 * @param boundaries boundaries of the {@link ImageMark}
	 */
	public void setBoundaries(ScaledPoint boundaries) {
		this.boundaries = boundaries;
	}
	
	/**
	 * @param currentDimensions current dimensions of canvas
	 * @return boundaries of {@link ImageMark} on a canvas with the indicated dimensions
	 */
	public Point getUnscaledBoundaries(Point currentDimensions) {
		return new Point((int) Math.ceil(currentDimensions.x * boundaries.x), (int) Math.ceil(currentDimensions.y * boundaries.y));
	}
	
	/**
	 * Gets ideal dimensions of the {@link ImageMark}, scaling so we don't go over the max size
	 * 
	 * @param currentDimensions current dimensions of canvas
	 * @param boundaries boundaries of the {@code ImageMark}
	 * @return ideal size of the {@code ImageMark}, scaling so that we don't go over the max size
	 */
	public static ScaledPoint getIdealSize(Point currentDimensions, Point boundaries) {
		ScaledPoint scaled = ScaledPoint.toScaledPoint(currentDimensions, boundaries);
		if (scaled.x > MAX_DIMENSION && scaled.x > scaled.y)
			return new ScaledPoint(MAX_DIMENSION, scaled.y / scaled.x);
		else if (scaled.y > MAX_DIMENSION && scaled.y > scaled.x)
			return new ScaledPoint(scaled.x / scaled.y, MAX_DIMENSION);
		
		return scaled;
	}
}
