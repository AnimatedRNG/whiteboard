package com.animated.rng.whiteboard.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.apache.commons.codec.binary.Base64;

import com.animated.rng.whiteboard.Whiteboard;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.minlog.Log;

/**
 * Manages the {@link Client} and logging in. Also sets up the {@link ClientListener}
 * 
 * @author Srinivas Kaza
 */
public class WhiteboardClient {

	public static String SERVER_IP;
	public static final int TIMEOUT = 5000;
	public static final int OBJECT_BUFFER_SIZE = 131072;
	public static final int WRITE_BUFFER_SIZE = 131072;
	
	private Client client;
	
	/**
	 * @param whiteboard {@link Whiteboard} object to send to {@link ClientListener}
	 */
	public WhiteboardClient(Whiteboard whiteboard) {
		this.client = new Client(OBJECT_BUFFER_SIZE, WRITE_BUFFER_SIZE);
		Registrar.registerUnencryptedClasses(this.client.getKryo());
		Registrar.registerEncryptedClasses(this.client.getKryo(), "encryption_example");
		WhiteboardClient.SERVER_IP = this.getServerIPAddress();
		this.client.start();
		this.client.addListener(new ClientListener(this, whiteboard));
	}
	
	/**
	 * @return {@link Client} used by {@link WhiteboardClient}
	 */
	public Client getClient() {
		return client;
	}

	/**
	 * Attempts to connect with server
	 * 
	 * @return whether the connection was successful or not
	 */
	public boolean connect() {
		try {
			this.client.connect(TIMEOUT, WhiteboardClient.SERVER_IP, WhiteboardServer.PORT_NUMBER);
		} catch (IOException e) {
			return false;
		}
		
		return true;
	}
	
	/**
	 * Disconnects client from server
	 */
	public void disconnect() {
		this.client.close();
	}
	
	/**
	 * Sends {@link LoginRequest} to server
	 * 
	 * @param name username to use
	 * @param pass password to use
	 * @param admin whether or not to encrypt the username (don't do this if you're not admin)
	 * @throws Exception if login request failed
	 */
	public void sendLoginRequest(String name, String pass, boolean admin) throws Exception {
		LoginRequest loginRequest = new LoginRequest();
		if (admin)
			loginRequest.name = encrypt(name, name);
		else
			loginRequest.name = name;
		loginRequest.pass = encrypt(pass, pass);
		this.client.sendTCP(loginRequest);
	}
	
	/**
	 * Encrypts a String. See {@link ForwardingListener} as to why this is a poor security measure 
	 * 
	 * @param text text to encrypt
	 * @param password password to use
	 * @return encrypted string
	 * @throws Exception if encryption failed
	 */
	private String encrypt(String text, String password) throws Exception {
		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
        SecretKey key = keyFactory.generateSecret(new PBEKeySpec(password.toCharArray()));
        Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
        pbeCipher.init(Cipher.ENCRYPT_MODE, key, new PBEParameterSpec(ForwardingListener.SALT, 20));
        return Base64.encodeBase64String(pbeCipher.doFinal(text.getBytes("UTF-8")));
	}
	
	/**
	 * Gets IP address from file (and does DNS lookup if necessary)
	 * 
	 * @return IP address of server
	 */
	private String getServerIPAddress() {
		String serverIPAddress = null;
		try {
			serverIPAddress = new String(Files.readAllBytes(Paths.get("assets/serverIP.txt")));
		} catch (IOException e) {
			Log.error("WhiteboardClient", "Failed to open serverIP.txt", e);
		}
		
		if (!validIP(serverIPAddress))
		{
			try {
				return InetAddress.getByName(serverIPAddress).getHostAddress();
			} catch (UnknownHostException e) {
				Log.error("WhiteboardClient", "DNS lookup failed");
				return null;
			}
		}
		else
			return serverIPAddress;
	}
		
	/**
	 * @param ip IPv4 address
	 * @return false if invalid
	 */
	public static boolean validIP (String ip) {
	    try {
	        if (ip == null || ip.isEmpty()) {
	            return false;
	        }
	
	        String[] parts = ip.split( "\\." );
	        if ( parts.length != 4 ) {
	            return false;
	        }
	
	        for ( String s : parts ) {
	            int i = Integer.parseInt( s );
	            if ( (i < 0) || (i > 255) ) {
	                return false;
	            }
	        }
	        if(ip.endsWith(".")) {
	                return false;
	        }
	
	        return true;
	    } catch (NumberFormatException nfe) {
	        return false;
	    }
	}
}
