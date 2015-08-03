package com.animated.rng.whiteboard.update;

import java.util.ArrayList;
import java.util.List;

import com.animated.rng.whiteboard.Whiteboard;
import com.animated.rng.whiteboard.WhiteboardWindow;
import com.animated.rng.whiteboard.util.ImageMark;
import com.animated.rng.whiteboard.util.Log;
import com.animated.rng.whiteboard.util.PointerMark;
import com.animated.rng.whiteboard.util.ScaledPoint;
import com.animated.rng.whiteboard.util.TextMark;

/**
 * Update the entire state of the {@link Whiteboard}. Sent out at regular intervals by the admin.
 * 
 * @author Srinivas Kaza
 */
public class StateUpdate extends WhiteboardUpdate {

	public static final int COMPRESSION_THRESHOLD = 3000;
	
	private List<double[]> pencilMarks;
	private List<TextMark> textMarks;
	private List<ImageMark> imageMarks;
	private PointerMark pointerMark;
	
	public StateUpdate() {
		this.pencilMarks = new ArrayList<double[]>();
		this.textMarks = new ArrayList<TextMark>();
		this.imageMarks = new ArrayList<ImageMark>();
		this.pointerMark = null;
	}
	
	/**
	 * @param pencilMarks Pencil markings on {@link Whiteboard}
	 * @param textMarks  {@link TextMark}s on {@code Whiteboard}
	 * @param imageMarks {@link ImageMark}s on {@code Whiteboard} 
	 * @param pointer {@link PointerMark} on {@code Whiteboard}
	 */
	public StateUpdate(List<double[]> pencilMarks, List<TextMark> textMarks, List<ImageMark> imageMarks, PointerMark pointer) {
		this.pencilMarks = pencilMarks;
		this.textMarks = textMarks;
		this.imageMarks = imageMarks;
		this.pointerMark = pointer;
		
		// If there are too many PencilMarks, attempt to compress the data 
		if (this.pencilMarks.size() > COMPRESSION_THRESHOLD)
		{
			Log.info("StateUpdate", "Size is " + this.pencilMarks.size());
			compressData(false);
			Log.info("StateUpdate", "Compressed size is " + this.pencilMarks.size());
		}
		
		// If there's still too many, keep compressing until we get under the compression threshold
		while (this.pencilMarks.size() > COMPRESSION_THRESHOLD)
			compressData(true);
		Log.info("StateUpdate", "Final compressed size is " + this.pencilMarks.size());
	}
	
	@Override
	public void onUpdate(Whiteboard whiteboard) {
		if (!WhiteboardWindow.drag)
			whiteboard.setPencilMarks(this.pencilMarks);
		
		List<TextMark> whiteboardTextMarksToDelete = new ArrayList<TextMark>(whiteboard.getTextMarks());
		whiteboardTextMarksToDelete.removeAll(this.textMarks);
		
		// Delete all the text which shouldn't be there
		for (TextMark deleteMark : whiteboardTextMarksToDelete)
			whiteboard.addUpdate(new TextUpdate(deleteMark, true, true), false);
		
		// Update the rest of the TextMarks
		for (TextMark textMark : this.textMarks)
			whiteboard.addUpdate(new TextUpdate(textMark, true, false), false);
		
		// Delete all ImageMarks that shouldn't be there
		for (ImageMark imageMark : whiteboard.getImageMarks())
		{
			boolean delete = true;
			for (ImageMark i : this.imageMarks)
			{
				if (i.id.equals(imageMark.id))
					delete = false;
			}
			
			if (delete)
				whiteboard.addUpdate(new EraserUpdate(new ScaledPoint(imageMark.getPosition().x + 
						imageMark.getBoundaries().x / 2, imageMark.getPosition().y + 
						imageMark.getBoundaries().y / 2)), false);
		}
		
		// Unlike TextMarks, we really can't "update" an ImageMark, so we don't send any whiteboard updates
		// about them unless we need to add new ones
		for (ImageMark imageMark : this.imageMarks)
		{
			if (whiteboard.getImageMark(imageMark.id) == null)
			{
				Log.info("StateUpdate", "Added imageMark " + imageMark.id);
				whiteboard.addUpdate(new ImageUpdate(imageMark), false);
			}
		}
		
		// Set the pointer
		whiteboard.setPointer(this.pointerMark);
	}
	
	/**
	 * Compresses {@link PencilMark}s via a simple algorithm. Can't compress beyond 2x without some
	 * artifacts.
	 * 
	 * @param force if true, lossy compression; if false, attempts lossless compression.
	 */
	private void compressData(boolean force) {
		Log.info("StateUpdate", "Compressing data");
		List<double[]> compressedPencilMarks = new ArrayList<double[]>((int) (pencilMarks.size() / 1.5f));
		
		//double[] last = pencilMarks.get(0);
		for (int i = 1; i < pencilMarks.size(); i += 2)
		{
			double[] first = pencilMarks.get(i - 1);
			double[] second = pencilMarks.get(i);
			
			if (((first[0] == second[0]) && (first[2] == second[2])) || force)
				compressedPencilMarks.add(new double[] {first[0], second[1], first[2], second[3]});
			else
			{
				compressedPencilMarks.add(first);
				compressedPencilMarks.add(second);
			}
			
			// System.arraycopy works inefficiently at these sizes
			//for (int a = 0; a < 4; a++)
			//	last[a] = second[a];
		}
		
		((ArrayList<double[]>) compressedPencilMarks).trimToSize();
		this.pencilMarks = compressedPencilMarks;
	}

}
