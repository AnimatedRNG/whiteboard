package com.animated.rng.whiteboard;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import com.animated.rng.whiteboard.network.WhiteboardClient;
import com.animated.rng.whiteboard.network.WhiteboardServer;
import com.animated.rng.whiteboard.update.EraserUpdate;
import com.animated.rng.whiteboard.update.ImageUpdate;
import com.animated.rng.whiteboard.update.PencilUpdate;
import com.animated.rng.whiteboard.update.PointerUpdate;
import com.animated.rng.whiteboard.update.TextUpdate;
import com.animated.rng.whiteboard.util.CommonLogger;
import com.animated.rng.whiteboard.util.ImageMark;
import com.animated.rng.whiteboard.util.Log;
import com.animated.rng.whiteboard.util.PointerMark;
import com.animated.rng.whiteboard.util.ScaledPoint;
import com.animated.rng.whiteboard.util.TextMark;

public class WhiteboardWindow {
	
	public static final int NOTHING_SELECTED = 0;
	public static final int PENCIL_SELECTED = 1;
	public static final int ERASER_SELECTED = 2;
	public static final int TEXT_SELECTED = 3;
	public static final int IMAGE_SELECTED = 4;
	public static final int LOGIN_SELECTED = 5;
	public static final int ARROW_SELECTED = 6;
	
	public static boolean drag = false;
	public static Font font;
	
	public int state = 0;
	
	protected Shell shell;
	protected Display display;
	
	private Point lastPos;
	private Whiteboard whiteboard;
	private ToolBar toolBar;
	private Canvas canvas;
	private Thread whiteboardThread;
	private WhiteboardClient client;

	private static final Point DEFAULT_DIMENSIONS = new Point(1280, 720);
	
	public static void main(String[] args) {
		try {
			Log.set(Log.LEVEL_TRACE);
			CommonLogger.isServer = false;
			Log.setLogger(new CommonLogger());
			WhiteboardWindow client1 = new WhiteboardWindow();
			client1.open();	
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void open() {
		this.display = Display.getDefault();
		createContents();
		shell.open();
		shell.layout();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	protected void createContents() {
		shell = new Shell();
		shell.setSize(DEFAULT_DIMENSIONS);
		shell.setText("Whiteboard");
		
		GridLayout rowLayout = new GridLayout();
		shell.setLayout(rowLayout);
		
		shell.addListener(SWT.Close, new Listener() {

			@Override
			public void handleEvent(Event event) {
				WhiteboardWindow.this.whiteboard.shutdown();
			}
		});
		
		this.toolBar = new ToolBar(shell, SWT.FLAT | SWT.WRAP | SWT.RIGHT);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		
		toolBar.setLayoutData(gridData);
		
		ToolItem loginItem = new ToolItem(toolBar, SWT.PUSH | SWT.BORDER_SOLID);
		loginItem.setData(LOGIN_SELECTED);
		loginItem.setImage(new Image(this.display, "assets/login.png"));
		
		ToolItem pencilItem = new ToolItem(toolBar, SWT.RADIO | SWT.BORDER_SOLID);
		pencilItem.setData(PENCIL_SELECTED);
		pencilItem.setImage(new Image(this.display, "assets/pencil.png"));
		
		ToolItem eraserItem = new ToolItem(toolBar, SWT.RADIO | SWT.BORDER_SOLID);
		eraserItem.setData(ERASER_SELECTED);
		eraserItem.setImage(new Image(this.display, "assets/eraser.png"));
		
		ToolItem textItem = new ToolItem(toolBar, SWT.RADIO | SWT.BORDER_SOLID);
		textItem.setData(TEXT_SELECTED);
		textItem.setImage(new Image(this.display, "assets/text.png"));
		
		ToolItem imageItem = new ToolItem(toolBar, SWT.RADIO | SWT.BORDER_SOLID);
		imageItem.setData(IMAGE_SELECTED);
		imageItem.setImage(new Image(this.display, "assets/image.png"));
		
		ToolItem arrowItem = new ToolItem(toolBar, SWT.RADIO | SWT.BORDER_SOLID);
		arrowItem.setData(ARROW_SELECTED);
		arrowItem.setImage(new Image(this.display, "assets/arrow_icon.png"));
		
		ToolItem clearItem = new ToolItem(toolBar, SWT.PUSH | SWT.BORDER_SOLID);
		clearItem.setImage(new Image(this.display, "assets/clear_all.png"));
		
		toolBar.pack();
		
		Listener selectionListener = new Listener() {

			@Override
			public void handleEvent(Event event) {
				ToolItem item = (ToolItem) event.widget;
				int id = (Integer) item.getData();
				WhiteboardWindow.this.state = id;
			}
		};
		
		// This listener handles login
		Listener loginClickListener = new Listener() {

			@Override
			public void handleEvent(Event event) {
				PasswordDialog dialog = new PasswordDialog(WhiteboardWindow.this.shell);
				
				if (dialog.open() == Window.OK)
				{
					boolean connected = WhiteboardWindow.this.client.getClient().isConnected();
					if (!connected)
						connected = WhiteboardWindow.this.client.connect();
					if (!connected)
						MessageDialog.openError(shell, "Error", "Failed to access server at " + WhiteboardClient.SERVER_IP
								+ " on port " + WhiteboardServer.PORT_NUMBER);
					else
					{
						try {
							WhiteboardWindow.this.whiteboard.setUsername(dialog.getUser());
							WhiteboardWindow.this.whiteboard.setPassword(dialog.getPassword());
							WhiteboardWindow.this.whiteboard.setAdmin(dialog.isCheckbox());
							WhiteboardWindow.this.client.sendLoginRequest(dialog.getUser(), dialog.getPassword(),
									dialog.isCheckbox());
						} catch (Exception e) {
							MessageDialog.openError(shell, "Error", "Failed to perform encryption routine: "
									+ e.getLocalizedMessage());
						}
					}
				}
			}
			
		};
		
		// This listener handles clearing the whiteboard
		Listener clearAllListener = new Listener() {
			
			@Override
			public void handleEvent(Event event) {
				WhiteboardWindow.this.whiteboard.getShell().getDisplay().asyncExec(new Runnable() {

					@Override
					public void run() {
						WhiteboardWindow.this.whiteboard.eraseAllMarks();
					}
				});
			}
		};
		
		loginItem.addListener(SWT.Selection, loginClickListener);
		pencilItem.addListener(SWT.Selection, selectionListener);
		eraserItem.addListener(SWT.Selection, selectionListener);
		textItem.addListener(SWT.Selection, selectionListener);
		imageItem.addListener(SWT.Selection, selectionListener);
		arrowItem.addListener(SWT.Selection, selectionListener);
		clearItem.addListener(SWT.Selection, clearAllListener);
		
		this.canvas = new Canvas(shell, SWT.NO_BACKGROUND | SWT.DOUBLE_BUFFERED);
		GridData canvasGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		canvas.setLayoutData(canvasGridData);
		canvas.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
		
		// This listener is pretty large and ungainly, but it handles all the MouseDown events on the canvas without
		// repeating code.
		canvas.addListener(SWT.MouseDown, new Listener() {

			@Override
			public void handleEvent(Event event) {
				WhiteboardWindow.this.whiteboard.setPointer(null);
				
				// If we need to turn on drag
				if (WhiteboardWindow.this.state == WhiteboardWindow.PENCIL_SELECTED ||
						WhiteboardWindow.this.state == WhiteboardWindow.ERASER_SELECTED)
				{
					WhiteboardWindow.drag = true;
					
					// If our mouse down determines a significant "last position" for the tool at hand
					if (WhiteboardWindow.this.state == WhiteboardWindow.PENCIL_SELECTED)
						WhiteboardWindow.this.lastPos = new Point(event.x, event.y);
				}
				
				Point currentDimensions = canvas.getSize();
				if (WhiteboardWindow.this.state == WhiteboardWindow.TEXT_SELECTED)
				{
					WhiteboardWindow.this.whiteboard.addUpdate(new TextUpdate(
								new TextMark(ScaledPoint.toScaledPoint(currentDimensions, new Point(event.x, event.y)), "",
										new ScaledPoint(TextMark.fontSize * TextMark.DEFAULT_LENGTH, TextMark.fontSize)), true, false), true);
					deselectAll();
				}
				else
				{
					WhiteboardWindow.this.whiteboard.unfocusAllText();
					
					if (WhiteboardWindow.this.state == WhiteboardWindow.ARROW_SELECTED)
					{
						boolean flipped = (event.button == 1) ? false : true;
						WhiteboardWindow.this.whiteboard.addUpdate(new PointerUpdate(new PointerMark(flipped,
								ScaledPoint.toScaledPoint(currentDimensions, new Point(event.x, event.y)))), true);
					}
					
					if (WhiteboardWindow.this.state == WhiteboardWindow.IMAGE_SELECTED)
					{
						InputDialog urlInput = new InputDialog(Display.getCurrent().getActiveShell(), "URL Selection",
								"Input Image URL", "", null);
						
						if (urlInput.open() == Window.OK)
						{
							URL url = null;
							try {
								url = new URL(urlInput.getValue());
								url.openStream();
							} catch (MalformedURLException e) {
								MessageDialog.openError(Display.getCurrent().getActiveShell(), "Error", "Malformed URL");
								deselectAll();
								return;
							} catch (IOException e) {
								MessageDialog.openError(Display.getCurrent().getActiveShell(), "Error", "Invalid URL");
								deselectAll();
								return;
							} catch (IllegalArgumentException e) {
								MessageDialog.openError(Display.getCurrent().getActiveShell(), "Error", "Invalid URL");
								deselectAll();
								return;
							}
							
							Image image = null;
							
							try {
								image = ImageDescriptor.createFromURL(url).createImage();
							} catch (Exception e) {
								MessageDialog.openError(Display.getCurrent().getActiveShell(), "Error", "Not an image");
								deselectAll();
								return;
							}
							ImageMark imageMark = new ImageMark(ScaledPoint.toScaledPoint(currentDimensions, 
									new Point(event.x, event.y)), url.toExternalForm(), ImageMark.getIdealSize(currentDimensions, 
											new Point(image.getBounds().width, image.getBounds().height)));
							image.dispose();
							WhiteboardWindow.this.whiteboard.addUpdate(new ImageUpdate(imageMark), true);
						}
						deselectAll();
					}
				}
			}
		});
		
		// This listener triggers if the mouse moves. Useful for the pencil and eraser
		canvas.addListener(SWT.MouseMove, new Listener() {

			@Override
			public void handleEvent(Event event) {
				
				if (!drag)
					return;
				
				Point currentDimensions = canvas.getSize();
				switch (state)
				{
				case PENCIL_SELECTED:
					if (WhiteboardWindow.this.lastPos == null)
						WhiteboardWindow.this.lastPos = new Point(event.x, event.y);
					WhiteboardWindow.this.whiteboard.addUpdate(
							new PencilUpdate(ScaledPoint.toScaledPoint(currentDimensions, WhiteboardWindow.this.lastPos),
									ScaledPoint.toScaledPoint(currentDimensions, new Point(event.x, event.y))), true);
					break;
				case ERASER_SELECTED:
					WhiteboardWindow.this.whiteboard.addUpdate(
							new EraserUpdate(ScaledPoint.toScaledPoint(currentDimensions, new Point(event.x, event.y))), true);
					break;
				default:
					return;
				}
				
				lastPos = new Point(event.x, event.y);
			}
		});
		
		// This listner stops the drag for the pencil and the eraser
		canvas.addListener(SWT.MouseUp, new Listener() {

			@Override
			public void handleEvent(Event event) {
				WhiteboardWindow.drag = false;
				WhiteboardWindow.this.lastPos = null;
			}
		});
		
		this.whiteboard = new Whiteboard(canvas, shell);
		this.whiteboardThread = new Thread(this.whiteboard);
		this.client = new WhiteboardClient(this.whiteboard);
		this.whiteboard.setClient(this.client.getClient());
		this.whiteboardThread.start();
		
		if (shell.getDisplay().loadFont("assets/font/liberation_mono.ttf"))
			font = new Font(shell.getDisplay(), "Liberation Mono", 12, SWT.NORMAL);
		else
		{
			MessageDialog.openError(shell, "Error", "Can't find required font in assets");
			whiteboard.shutdown();
		}
	}

	/**
	 * Deselects all the ToolItems
	 */
	private void deselectAll() {
		this.state = WhiteboardWindow.NOTHING_SELECTED;
		ToolItem[] toolItems = this.toolBar.getItems();
		for (ToolItem item : toolItems)
			item.setSelection(false);
	}
}
