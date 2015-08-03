package com.animated.rng.whiteboard.network;

import java.io.IOException;

import com.animated.rng.whiteboard.util.CommonLogger;
import com.animated.rng.whiteboard.util.Log;
import com.esotericsoftware.kryonet.Server;

/**
 * Manages the {@link Server} and sets up the {@link ForwardingListener} 
 * 
 * @author Srinivas Kaza
 */
public class WhiteboardServer {
	
	public static final int PORT_NUMBER = 54668;
	public static final int OBJECT_BUFFER_SIZE = 131072;
	public static final int WRITE_BUFFER_SIZE = 131072;
	
	private Server server;
	
	public WhiteboardServer() {
		this.server = new Server(OBJECT_BUFFER_SIZE, WRITE_BUFFER_SIZE);
		Log.set(Log.LEVEL_TRACE);
		CommonLogger.isServer = true;
		Log.setLogger(new CommonLogger());
		Registrar.registerUnencryptedClasses(this.server.getKryo());
		Registrar.registerEncryptedClasses(this.server.getKryo(), "encryption_example");
		this.server.start();
		
		try {
			this.server.bind(54668);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		this.server.addListener(new ForwardingListener(this.server));
	}
}
