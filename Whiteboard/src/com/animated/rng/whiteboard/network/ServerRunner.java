package com.animated.rng.whiteboard.network;

/**
 * Runs {@link WhiteboardServer}
 * 
 * @author Srinivas Kaza
 */
public class ServerRunner {

	public static void main(String[] args) {
		new WhiteboardServer();
		
		while (true) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
