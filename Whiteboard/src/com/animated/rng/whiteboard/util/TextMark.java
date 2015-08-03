package com.animated.rng.whiteboard.util;

import java.util.UUID;

import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;

/**
 * Represents a text marking. Scales with the UI as well as it can.
 * 
 * @author Srinivas Kaza
 */
public class TextMark {

	public static final int DEFAULT_LENGTH = 10;
	public static final double PADDING = 0.01;
	public static double fontSize = 0.05;
	
	public Long id;
	
	// This should be the DPI, but SWT's DPI implementation is broken so I just
	// tried a bunch of numbers and this worked.
	private static int MAGIC_CONSTANT = 20;
	
	private ScaledPoint position;
	private String text;
	private ScaledPoint boundaries;
	
	public TextMark() {
		
	}
	
	/**
	 * @param position position of the {@link TextMark}
	 * @param text text to display
	 * @param boundaries width and height of the {@link TextMark}
	 */
	public TextMark(ScaledPoint position, String text, ScaledPoint boundaries) {
		this.position = position;
		this.text = text;
		this.boundaries = boundaries;
		this.id = UUID.randomUUID().getMostSignificantBits();
	}
	
	/**
	 * @param position position of the {@link TextMark}
	 * @param text text to display
	 * @param boundaries width and height of the {@link TextMark}
	 * @param id ID of the TextMark (use this instead of a random ID)
	 */
	public TextMark(ScaledPoint position, String text, ScaledPoint boundaries, Long id) {
		this.position = position;
		this.text = text;
		this.boundaries = boundaries;
		this.id = id;
	}
	
	/**
	 * @return position of {@link TextMark}
	 */
	public ScaledPoint getPosition() {
		return position;
	}

	/**
	 * @param position position of {@link TextMark}
	 */
	public void setPosition(ScaledPoint position) {
		this.position = position;
	}

	/**
	 * @return String contents of {@link TextMark}
	 */
	public String getText() {
		return text;
	}

	/**
	 * @param text set String contents of {@link TextMark}
	 */
	public void setText(String text) {
		this.text = text;
	}
	
	/**
	 * @return boundaries of {@link TextMark}
	 */
	public ScaledPoint getBoundaries() {
		return boundaries;
	}

	/**
	 * @param boundaries of {@link TextMark}
	 */
	public void setBoundaries(ScaledPoint boundaries) {
		this.boundaries = boundaries;
	}

	/**
	 * @param currentDimensions current dimensions of canvas
	 * @return boundaries of {@link TextMark} on a canvas with the indicated dimensions
	 */
	public Point getUnscaledBoundaries(Point currentDimensions) {
		return new Point((int) Math.ceil(currentDimensions.x * boundaries.x), (int) Math.ceil(currentDimensions.y * boundaries.y));
	}
	
	/**
	 * Gets the ideal font size... except that for some reason the vertical DPI doesn't seem to work
	 * so this little hack will have to do.
	 * 
	 * @param currentDimensions current dimensions of canvas
	 * @param device device to get DPI from
	 * @return ideal font size of {@link TextMark}
	 */
	public int getFontSize(Point currentDimensions, Device device) {
		double pixelHeight = currentDimensions.y * fontSize;
		
		return (int) Math.ceil((pixelHeight * MAGIC_CONSTANT) / 72.0);
	}
	
	/**
	 * Changes the Font size to the indicated size
	 * 
	 * @param font current {@link Font}
	 * @param size point size to which the font should be changed
	 * @return resized Font
	 */
	public static Font changeFontSize(Font font, int size) {
	    FontData[] fontData = font.getFontData();
	    for (int i = 0; i < fontData.length; i++) {
	    	fontData[i].setHeight(size);
	    }
	    return new Font(font.getDevice(), fontData);
	}
}
