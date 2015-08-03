package com.animated.rng.whiteboard.update;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

import com.animated.rng.whiteboard.Whiteboard;
import com.animated.rng.whiteboard.util.ImageMark;

/**
 * Updates {@link Whiteboard}'s {@link ImageMark}s
 * 
 * @author Srinivas Kaza
 */
public class ImageUpdate extends WhiteboardUpdate {

	private ImageMark imageMark;
	
	public ImageUpdate() {
		
	}
	
	/**
	 * @param imageMark {@link ImageMark} to use
	 */
	public ImageUpdate(ImageMark imageMark) {
		this.imageMark = imageMark;
	}
	
	@Override
	public void onUpdate(Whiteboard whiteboard) {
		Image image;
		try {
			image = ImageDescriptor.createFromURL(new URL(this.imageMark.getImageURL())).createImage();
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return;
		}
		whiteboard.addImage(this.imageMark.id, image);
		whiteboard.addImageMark(this.imageMark);
	}

}
