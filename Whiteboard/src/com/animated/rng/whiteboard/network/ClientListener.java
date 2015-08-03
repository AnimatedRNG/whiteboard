package com.animated.rng.whiteboard.network;

import org.eclipse.jface.dialogs.MessageDialog;

import com.animated.rng.whiteboard.Whiteboard;
import com.animated.rng.whiteboard.update.WhiteboardUpdate;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

/**
 * Listener that schedules updates sent over the network. Also handles login and disconnect.
 * 
 * @author Srinivas Kaza
 */
public class ClientListener extends Listener {
	
	private WhiteboardClient client;
	private Whiteboard whiteboard;
	private boolean connected;
	
	/**
	 * @param client {@link WhiteboardClient} to use
	 * @param whiteboard {@link Whiteboard} to use
	 */
	public ClientListener(WhiteboardClient client, Whiteboard whiteboard) {
		this.client = client;
		this.whiteboard = whiteboard;
	}
	
	@Override
	public void connected(Connection connection) {
		this.connected = true;
	}
	
	@Override
	public void disconnected(Connection connection) {
		this.connected = false;
		this.whiteboard.getCanvas().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				while (whiteboard.isRunning() && !connected)
				{
					MessageDialog dialog = new MessageDialog(whiteboard.getShell(), "Error", null, 
						"Disconnected from server. Retry?", 
						MessageDialog.ERROR, new String[] { "OK", "Cancel" }, 0);
					int result = dialog.open();
					
					// If the user doesn't want to reconnect, shutdown the whiteboard
					if (result == 1)
						whiteboard.shutdown();
					
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					// Attempt to reconnect
					ClientListener.this.connected = client.connect();
					try {
						client.sendLoginRequest(whiteboard.getUsername(), whiteboard.getPassword(), whiteboard.isAdmin());
					} catch (Exception e) {
						MessageDialog.openError(whiteboard.getShell(), "Error", "Failed to perform encryption routine: "
								+ e.getLocalizedMessage());
					}
				}
			}
		});
	}
	
	@Override
	public void received(Connection connection, Object object) {
		// If a login response was received
		if (object instanceof LoginResponse)
		{
			// and they didn't have the right password, then show an error dialog
			if (!((LoginResponse) object).success)
			{
				this.whiteboard.getCanvas().getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						MessageDialog.openError(whiteboard.getShell(), "Login", "Incorrect password");
					}
				});
			}
			// if they did have the right password, then clear the board
			else
			{
				this.whiteboard.getShell().getDisplay().asyncExec(new Runnable() {

					@Override
					public void run() {
						ClientListener.this.whiteboard.eraseAllMarks();
					}
				});
			}
		}
		// If we recieved a WhiteboardUpdate, broadcast it to everyone else (albeit with the broadcast flag turned off,
		// lest we start an infinite feedback loop).
		else if (object instanceof WhiteboardUpdate) {
			this.whiteboard.addUpdate((WhiteboardUpdate) object, false);
		}
	}
	
	@Override
	public void idle(Connection connection) {
		
	}
}
