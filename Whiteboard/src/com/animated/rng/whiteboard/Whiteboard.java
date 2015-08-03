package com.animated.rng.whiteboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.animated.rng.whiteboard.update.StateUpdate;
import com.animated.rng.whiteboard.update.TextUpdate;
import com.animated.rng.whiteboard.update.WhiteboardUpdate;
import com.animated.rng.whiteboard.util.ImageMark;
import com.animated.rng.whiteboard.util.Log;
import com.animated.rng.whiteboard.util.PointerMark;
import com.animated.rng.whiteboard.util.ScaledPoint;
import com.animated.rng.whiteboard.util.TextMark;
import com.esotericsoftware.kryonet.Client;

/**
 * Whiteboard class. Used to manipulate whiteboard.
 * 
 * @author Srinivas Kaza
 *
 */

public class Whiteboard implements Runnable {

	public static final int LINE_WIDTH = 5;
	public static final int STATE_UPDATE_TIMEOUT = 3000;
	
	// DON'T SET THIS TO 0 OR A BIG NUMBER
	public static final int UPDATE_STALENESS_COEFFICIENT = 1;
	
	public static Image pointerImage;
	public static Image pointerImageFlipped;
	
	private Canvas canvas;
	private Shell shell;
	private boolean running;
	private Point canvasSize;
	
	private Client client;
	
	private String username;
	private String password;
	private boolean isAdmin;
	
	// x1, x2, y1, y2
	private List<double[]> pencilMarks;
	private List<TextMark> textMarks;
	private List<Text> textFields;
	private List<ImageMark> imageMarks;
	private Map<Long, Image> images;
	private PointerMark pointerMark;

	private Queue<WhiteboardUpdate> pendingUpdates; 
	
	/**
	 * Creates a new Whiteboard with a given canvas and shell
	 * 
	 * @param canvas canvas to use
	 * @param shell shell to use
	 */
	public Whiteboard(Canvas canvas, Shell shell) {
		this.setShell(shell);
		this.setUsername(new String());
		this.setPassword(new String());
		this.setAdmin(false);
		this.running = true;
		this.pencilMarks = new ArrayList<double[]>();
		this.setTextMarks(new ArrayList<TextMark>());
		this.setTextFields(new ArrayList<Text>());
		this.setImageMarks(new ArrayList<ImageMark>());
		this.setImages(new HashMap<Long, Image>());
		this.canvas = canvas;
		this.pendingUpdates = new LinkedList<WhiteboardUpdate>();
		this.canvasSize = this.getCanvas().getSize();
		Whiteboard.pointerImage = new Image(Display.getCurrent(), "assets/arrow.png");
		Whiteboard.pointerImageFlipped = new Image(Display.getCurrent(), "assets/arrow_flipped.png");
		
		this.canvas.addPaintListener(new PaintListener() {

			@Override
			public void paintControl(PaintEvent e) {
				e.gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
				e.gc.fillRectangle(0, 0, Whiteboard.this.getCanvasSize().x, Whiteboard.this.getCanvasSize().y);
				
				// Draw images
				synchronized (imageMarks) {
					for (ImageMark mark : Whiteboard.this.imageMarks)
					{
						Image image = Whiteboard.this.getImage(mark.id);
						Point position = ScaledPoint.toSWTPoint(Whiteboard.this.getCanvasSize(), mark.getPosition());
						Point bounds = mark.getUnscaledBoundaries(Whiteboard.this.getCanvasSize());
						e.gc.drawImage(image, 0, 0, image.getBounds().width, image.getBounds().height, position.x, position.y, 
								bounds.x, bounds.y);
					}
				}
				
				// Draw pencil marks
				synchronized (pencilMarks) {
					e.gc.setLineWidth(LINE_WIDTH);
					for (double[] line : Whiteboard.this.pencilMarks)
					{
						Point lastPosPoint = ScaledPoint.toSWTPoint(Whiteboard.this.getCanvasSize(), new ScaledPoint(line[0], line[2]));
						Point newPosPoint = ScaledPoint.toSWTPoint(Whiteboard.this.getCanvasSize(), new ScaledPoint(line[1], line[3]));
						e.gc.drawLine(lastPosPoint.x, lastPosPoint.y, newPosPoint.x, newPosPoint.y);
					}
				}
				
				// Draw pointer, assuming it's there
				if (pointerMark != null)
				{
					synchronized (pointerMark) {
						Point pos = ScaledPoint.toSWTPoint(Whiteboard.this.getCanvasSize(), pointerMark.getPosition());
						if (pointerMark.isFlipped())
							e.gc.drawImage(Whiteboard.pointerImageFlipped, pos.x, pos.y);
						else
							e.gc.drawImage(Whiteboard.pointerImage, pos.x, pos.y);
					}
				}
			}
		});
		
		// On resize, change our internal representation of the dimensions and reposition text
		this.canvas.addListener(SWT.Resize, new Listener() {

			@Override
			public void handleEvent(Event event) {
				Whiteboard.this.setCanvasSize(Whiteboard.this.getCanvas().getSize());
				
				Whiteboard.this.repositionText();
			}
		});
		
		// This thread repeatedly sends state updates, if the user is admin.
		// There is also server-side verification.
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (running) {
					try {
						Thread.sleep(Whiteboard.STATE_UPDATE_TIMEOUT);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					if (isAdmin() && client.isConnected())
					{
						synchronized (Whiteboard.this.client) {
							Whiteboard.this.client.sendTCP(new StateUpdate(Whiteboard.this.getPencilMarks(), 
									Whiteboard.this.getTextMarks(), Whiteboard.this.getImageMarks(), Whiteboard.this.getPointer()));
						}
					}
				}
			}
		}).start();
	}
	
	/**
	 * Adds update to update queue
	 * 
	 * @param update update to add/broadcast
	 * @param broadcast if true, update will be sent to server and broadcasted to all other clients
	 */
	public void addUpdate(WhiteboardUpdate update, boolean broadcast) {
		
		if (broadcast)
		{
			//Log.info("Whiteboard", "Sending update " + update);
			
			// Hacky force redraw on other client end
			if (update instanceof TextUpdate)
				((TextUpdate) update).setRedraw(true);
			
			this.client.sendTCP(update);
			
			// Now disable it so that we don't update ourselves
			if (update instanceof TextUpdate)
				((TextUpdate) update).setRedraw(false);
		}
		
		synchronized (pendingUpdates) {
			this.pendingUpdates.add(update);
		}
	}
	
	/**
	 * Adds pencil mark to {@link Whiteboard}
	 * 
	 * @param line pencil line to be added
	 */
	public void addPencilMark(double[] line) {
		synchronized (pencilMarks) {
			this.pencilMarks.add(line);
		}
		updateWhiteboard();
	}
	
	/**
	 * Simulates using the eraser tool at point {@code p} with radius {@code radius}
	 * 
	 * @param p point of erasure
	 * @param radius radius of erasure
	 */
	public void eraseMarks(ScaledPoint p, double radius) {
		Point currentDimensions = this.getCanvasSize();
		
		// Erase image if we're inside its bounding rectangle
		synchronized (imageMarks) {
			for (Iterator<ImageMark> iter = this.imageMarks.listIterator(); iter.hasNext(); )
			{
				ImageMark mark = iter.next();
				Point position = ScaledPoint.toSWTPoint(currentDimensions, mark.getPosition());
				Point boundaries = mark.getUnscaledBoundaries(currentDimensions);
				Rectangle rect = new Rectangle(position.x, position.y, boundaries.x, boundaries.y);
				
				if (rect.contains(ScaledPoint.toSWTPoint(currentDimensions, p)))
					this.shell.getDisplay().asyncExec(new AsyncImageEraser(mark.id));
			}
		}
		
		// Eraser pencil mark if it's within radius
		synchronized (pencilMarks) {
			for (Iterator<double[]> iter = this.pencilMarks.listIterator(); iter.hasNext(); )
			{
				double[] line = iter.next();
				
				if (Whiteboard.distance(new ScaledPoint(line[0], line[2]), p) < radius ||
						Whiteboard.distance(new ScaledPoint(line[1], line[3]), p) < radius)
					iter.remove();
			}
		}
		
		updateWhiteboard();
	}
	
	/**
	 * Erases all marks on {@link Whiteboard}
	 * 
	 */
	public void eraseAllMarks() {
		synchronized (pencilMarks) {
			this.pencilMarks.clear();
			
			int numImageMarks = this.imageMarks.size();
			for (int i = 0; i < numImageMarks; i++)
				this.removeImage(this.imageMarks.get(0).id);
		}
		
		this.setPointer(null);
		
		List<Long> textMarkIds = new ArrayList<Long>(this.textMarks.size());
		synchronized (textMarks) {
			for (TextMark mark : this.textMarks)
				textMarkIds.add(mark.id);
		}
		
		for (Long textMarkID : textMarkIds)
		{
			Log.info("Whiteboard", "Deleting mark " + textMarkID);
			this.deleteText(textMarkID);
		}
		
		updateWhiteboard();
	}
	
	/**
	 * Unfocuses all text
	 *  
	 */
	public void unfocusAllText() {
		this.shell.setFocus();
	}
	
	/**
	 * @return shell used by {@link Whiteboard}
	 */
	public Shell getShell() {
		return shell;
	}

	/**
	 * @param shell used by {@link Whiteboard}
	 */
	public void setShell(Shell shell) {
		this.shell = shell;
	}

	/**
	 * @return canvas used by {@link Whiteboard}
	 */
	public Canvas getCanvas() {
		return canvas;
	}

	/**
	 * @param canvas used by {@link Whiteboard}
	 */
	public void setCanvas(Canvas canvas) {
		this.canvas = canvas;
	}
	
	/**
	 * @return client used by {@link Whiteboard}
	 */
	public Client getClient() {
		return client;
	}

	/**
	 * @param client used by {@link Whiteboard}
	 */
	public void setClient(Client client) {
		this.client = client;
	}

	/**
	 * @return dimensions of canvas
	 */
	public Point getCanvasSize() {
		return canvasSize;
	}

	/**
	 * @param canvasSize dimensions of canvas
	 */
	public void setCanvasSize(Point canvasSize) {
		this.canvasSize = canvasSize;
	}

	/**
	 * @return username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @param username field used to represent username
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * @return password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password field used to represent password
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @return whether user is admin or not
	 */
	public boolean isAdmin() {
		return isAdmin;
	}

	/**
	 * @param isAdmin whether user is admin or not
	 */
	public void setAdmin(boolean isAdmin) {
		this.isAdmin = isAdmin;
	}
	
	/**
	 * @return all pencil markings
	 */
	public List<double[]> getPencilMarks() {
		return pencilMarks;
	}

	/**
	 * Sets all pencil markings and updates {@link Whiteboard}
	 * 
	 * @param pencilMarks all pencil markings.
	 */
	public void setPencilMarks(List<double[]> pencilMarks) {
		this.pencilMarks = pencilMarks;
		updateWhiteboard();
	}
	
	/**
	 * @param id ID of {@link TextMark}
	 * @return {@code TextMark} with specified ID, otherwise null
	 */
	public TextMark getTextMark(Long id) {
		for (TextMark mark : this.textMarks)
			if(mark.id.equals(id))
				return mark;
		return null;
	}

	/**
	 * @return all {@link TextMark}s
	 */
	public List<TextMark> getTextMarks() {
		return textMarks;
	}

	/**
	 * @param textMarks {@link TextMark}s to be used by {@link Whiteboard}
	 */
	public void setTextMarks(List<TextMark> textMarks) {
		this.textMarks = textMarks;
	}
	
	/**
	 * @param textField {@link Text} to be added to {@link Whiteboard} 
	 */
	public void addTextField(Text textField) {
		synchronized (this.textFields) {
			this.textFields.add(textField);
		}
	}

	/**
	 * @return all {@link Text} fields
	 */
	public List<Text> getTextFields() {
		return textFields;
	}
	
	/**
	 * @param id ID of {@link Text} field
	 * @return {@code Text} with specified ID, otherwise null
	 */
	public Text getTextField(Long id) {
		for (Text field : this.textFields)
			if (field.getData().equals(id))
				return field;
		return null;
	}

	/**
	 * @param textFields {@link Text}s to be used by {@link Whiteboard} 
	 */
	public void setTextFields(List<Text> textFields) {
		this.textFields = textFields;
	}
	
	/**
	 * Deletes {@link Text} and {@link TextMark} specified by ID. Must be
	 * run on UI thread.
	 * 
	 * @param id ID of {@code TextMark} (and {@code Text}) to be deleted
	 */
	public void deleteText(Long id) {
		Text deletedField = this.getTextField(id);
		
		if (deletedField == null)
			return;
		
		List<Text> modifiedFields = this.getTextFields();
		modifiedFields.remove(deletedField);
		deletedField.dispose();
		
		synchronized (this.textFields) {
			this.setTextFields(modifiedFields);
		}
		
		TextMark deletedMark = this.getTextMark(id);
		
		if (deletedMark == null)
			return;
		
		List<TextMark> modifiedMarks = this.getTextMarks();
		modifiedMarks.remove(deletedMark);
		
		synchronized (this.textMarks) {
			this.setTextMarks(textMarks);
		}
	}

	/**
	 * @param mark {@link ImageMark} to be added to {@link Whiteboard}
	 */
	public void addImageMark(ImageMark mark) {
		synchronized (imageMarks) {
			this.imageMarks.add(mark);
		}
		updateWhiteboard();
	}
	
	/**
	 * @return {@link ImageMark}s used by {@link Whiteboard}
	 */
	public List<ImageMark> getImageMarks() {
		return imageMarks;
	}
	
	/**
	 * @param id ID of {@link ImageMark} to be returned
	 * @return {@link ImageMark} with the specified ID, otherwise null
	 */
	public ImageMark getImageMark(long id) {
		for (ImageMark field : this.imageMarks)
			if (field.id.equals(id))
				return field;
		return null;
	}
	
	/**
	 * @param id ID of {@link ImageMark} to be removed
	 */
	public void removeImageMark(Long id) {
		for (Iterator<ImageMark> iter = this.imageMarks.listIterator(); iter.hasNext(); )
			if (iter.next().id.equals(id))
				iter.remove();
	}

	/**
	 * @param imageMarks {@link ImageMark}s to be used by {@link Whiteboard}
	 */
	public void setImageMarks(List<ImageMark> imageMarks) {
		this.imageMarks = imageMarks;
	}
	
	/**
	 * @param id ID of {@link Image} to add
	 * @param image {@link Image} to add to {@link Whiteboard}
	 */
	public void addImage(Long id, Image image) {
		synchronized (images) {
			this.images.put(id, image);
		}
		updateWhiteboard();
	}
	
	/**
	 * @param id ID of specified {@link Image}
	 * @return {@code Image} with specified ID
	 */
	public Image getImage(Long id) {
		return this.images.get(id);
	}

	/**
	 * @param images images to be used by {@link Whiteboard}
	 */
	public void setImages(Map<Long, Image> images) {
		this.images = images;
	}
	
	/**
	 * Removes {@link Image} and {@link ImageMark} from {@link Whiteboard} and disposes resources
	 * 
	 * @param id ID of {@code Image} to delete
	 */
	public void removeImage(long id) {
		if (this.getImageMark(id) != null) {
			this.removeImageMark(id);
			Image image = this.images.remove(id);
			if (!image.isDisposed())
				image.dispose();
		}
		this.updateWhiteboard();
	}
	
	/**
	 * @return {@link PointerMark} that is used by {@link Whiteboard}
	 */
	public PointerMark getPointer() {
		return this.pointerMark;
	}
	
	/**
	 * @param mark {@link PointerMark} to be used by {@link Whiteboard}
	 */
	public void setPointer(PointerMark mark) {
		this.pointerMark = mark;
		
		this.updateWhiteboard();
	}

	/**
	 * @return whether the {@link Whiteboard} is still running 
	 */
	public boolean isRunning() {
		return this.running;
	}

	/**
	 * Redraws the {@link Whiteboard} asynchronously
	 */
	public void updateWhiteboard() {
		Display.getDefault().asyncExec(new Runnable() {

			@Override
			public void run() {
				canvas.redraw();
			}
		});
	}
	
	/**
	 * Repositions all the text on the {@link Whiteboard}
	 */
	private void repositionText() {
		for (TextMark mark : this.textMarks)
			this.addUpdate(new TextUpdate(mark, true, false), false);
	}
	
	@Override
	public void run() {
		while (running) {
			synchronized (this.pendingUpdates) {
				// Some of the TextUpdates are added to the update queue after this loop runs (thanks to synchronization),
				// so we often have to run it twice to check if there's any updates remaining
				for (int i = 0; i < UPDATE_STALENESS_COEFFICIENT + 1 && !this.pendingUpdates.isEmpty(); i++)
				{
					// Keep running updates from our pending updates queue
					for (WhiteboardUpdate update : new LinkedList<WhiteboardUpdate>(this.pendingUpdates))
					{
						//Log.info("Whiteboard", update);
						if (!update.usesGUI())
							update.update(this);
						else
							Display.getDefault().asyncExec(new AsyncUpdateHandler(update, this));
					}
				}
				this.pendingUpdates.clear();
			}
		}
	}
	
	/**
	 * Shuts down whiteboard and disposes of resources
	 */
	public void shutdown() {
		this.running = false;
		
		if (!shell.isDisposed()) {
			Display display = shell.getDisplay();
			
			for (Text text : this.textFields)
				text.dispose();
			for (Image image : this.images.values())
				image.dispose();
			
			WhiteboardWindow.font.dispose();
			
			Whiteboard.pointerImage.dispose();
			Whiteboard.pointerImageFlipped.dispose();
			shell.dispose();
			display.dispose();
		}
	}
	
	/**
	 * Gets the distance between two {@link Point}s
	 * 
	 * @param first first {@code Point}
	 * @param second second {@code Point}
	 * @return distance between the points
	 */
	public static double distance(Point first, Point second) {
		return Math.abs(Math.sqrt((first.x - second.x) * (first.x - second.x) + (first.y - second.y) * (first.y - second.y)));
	}
	
	/**
	 * Gets the distance between two {@link ScaledPoint}s
	 * 
	 * @param first first {@code ScaledPoint}
	 * @param second second {@code ScaledPoint}
	 * @return distance between the points
	 */
	public static double distance(ScaledPoint first, ScaledPoint second) {
		return Math.abs(Math.sqrt((first.x - second.x) * (first.x - second.x) + (first.y - second.y) * (first.y - second.y)));
	}
	
	/**
	 * Handler for {@link WhiteboardUpdate}s that need to be run on the GUI thread
	 * 
	 * @author Srinivas Kaza
	 */
	private class AsyncUpdateHandler implements Runnable {

		private WhiteboardUpdate update;
		private Whiteboard whiteboard;
		
		public AsyncUpdateHandler(WhiteboardUpdate u, Whiteboard whiteboard) {
			this.update = u;
			this.whiteboard = whiteboard;
		}
		
		@Override
		public void run() {
			this.update.update(this.whiteboard);
		}
	}
	
	/**
	 * Handler for erasing images on the GUI thread
	 * 
	 * @author Srinivas Kaza
	 */
	private class AsyncImageEraser implements Runnable {

		private Long id;
		
		public AsyncImageEraser(Long id) {
			this.id = id;
		}
		
		@Override
		public void run() {
			synchronized (imageMarks) {
				removeImage(id);
			}
		}
	}
}
