package com.animated.rng.whiteboard.network;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.apache.commons.codec.binary.Base64;

import com.animated.rng.whiteboard.update.StateUpdate;
import com.animated.rng.whiteboard.update.WhiteboardUpdate;
import com.animated.rng.whiteboard.util.Log;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

/**
 * Listener that mostly just relays updates to other connected users. Also handles login.
 * 
 * @author Srinivas Kaza
 */
public class ForwardingListener extends Listener {

	public static final byte[] SALT = {
        (byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12,
        (byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12,
    };
	
	public static final String ADMIN_NAME = "ADMIN";
	
	private Map<String, Connection> connectedUsers;
	private String correctUsername;
	private String correctPass;
	private Connection admin;
	private Server server;
	
	/**
	 * @param server server to use to send messages
	 */
	public ForwardingListener(Server server) {
		this.connectedUsers = new HashMap<String, Connection>();
		this.server = server;
		
		try {
			this.correctUsername = new String(Files.readAllBytes(Paths.get("assets/userPass/adminpass.txt")));
			this.correctPass = new String(Files.readAllBytes(Paths.get("assets/userPass/userpass.txt")));
		} catch (IOException e) {
			Log.error("ForwardingListener", "Failed to access pass file", e);
		}
	}
	
	@Override
	public void connected(Connection connection) {
		Log.info("ForwardingListener", "Connecting to user on connection " + connection.getID());
	}
	
	@Override
	public void disconnected(Connection connection) {
		disconnectUser(connection);
	}
	
	@Override
	public void received(Connection connection, Object object) {
		if (object instanceof LoginRequest)
		{
			String name = ((LoginRequest) object).name;
			String pass = ((LoginRequest) object).pass;
			boolean passwordVerified = verifyPassword(pass);
			boolean adminVerified = verifyAdmin(name);
			
			if (!passwordVerified)
			{
				Log.info("ForwardingListener", "User " + name + " on connection " + connection.getID() + " had an incorrect password");
				sendLoginResponse(connection, false);
			}
			else
			{
				sendLoginResponse(connection, true);
				String uuid = UUID.randomUUID().toString();
				// If the user is trying to connect after already logging in
				if (connectedUsers.containsValue(connection))
				{
					Log.warn("ForwardingListener", "User on connection " + connection.getID() + " already logged in. "
							+ "Deleting entry with connection " + connection.getID());
					disconnectUser(connection);
				}
				if (!adminVerified)
				{
					Log.info("ForwardingListener", "User " + name + " on connection " + connection.getID() + " successfully authenticated");
					if (name != null)
					{
						// If the user is trying to name herself after the admin
						if (name.equals(ADMIN_NAME))
						{
							Log.warn("ForwardingListener", "User tried to name herself " + ADMIN_NAME + ". Nice try. Renaming...");
							this.connectedUsers.put("croesus_" + uuid, connection);
						}
						// If the user is trying to login with the same name of another user
						if (connectedUsers.containsKey(name))
						{
							Log.warn("ForwardingListener", "User " + name + " of connection " + connection.getID() + 
									" tried to masquerade as another user. Renaming....");
							this.connectedUsers.put("clone_user_" + uuid, connection);
						}
						else
							this.connectedUsers.put(name, connection);
					}
					else
						this.connectedUsers.put("null_name_user_" + uuid, connection);
				}
				else
				{
					if (this.admin == null)
					{
						Log.info("ForwardingListener", "User " + name + " on connection " + connection.getID() +
								" successfully authenticated as admin");
						this.admin = connection;
						this.connectedUsers.put(ADMIN_NAME, connection);
					}
					else
					{
						Log.warn("ForwardingListener", "Another user managed to authenticate as admin. Denying authorization.");
						this.connectedUsers.put("fake_admin_" + uuid, connection);
					}
				}
			}
		}
		else if (object instanceof WhiteboardUpdate)
		{
			if (!checkIfUserIsLoggedIn(connection))
				return;
			else
			{
				Log.info("ForwardingListener", "Received " + object + " from connection " + connection.getID());
				if (object instanceof StateUpdate && connection != this.admin)
				{
					Log.warn("ForwardingListener", "Non-admin attempted to issue admin state update. Refusing to broadcast update ...");
					return;
				}
				
				if (object instanceof StateUpdate)
					Log.info("ForwardingListener", "State update sent");
				
				((WhiteboardUpdate) object).connectionID = connection.getID();
				this.sendToAllExcept(object, connection);
			}
		}
	}
	
	@Override
	public void idle(Connection connection) {
		
	}
	
	
	/**
	 * Check if user is logged in
	 * 
	 * @param connection connection to check
	 * @return whether user is logged in or not
	 */
	private boolean checkIfUserIsLoggedIn(Connection connection) {
		return this.connectedUsers.containsValue(connection);
	}
	
	/**
	 * Disconnect user by IP
	 * 
	 * @param connection connection to disconnect
	 */
	private void disconnectUser(Connection connection) {
		for (Map.Entry<String, Connection> entry : this.connectedUsers.entrySet()) {
			if (Objects.equals(connection, entry.getValue())) {
				this.disconnectUser(entry.getKey());
				Log.info("ForwardingListener", "Disconnecting user on connection " + connection.getID());
				break;
			}
		}
	}
	
	/**
	 * Disconnect user by username
	 * 
	 * @param name user to disconnect
	 */
	private void disconnectUser(String name) {
		Connection connection = this.connectedUsers.get(name);
		if (connectedUsers.containsValue(connection))
		{
			Log.info("ForwardingListener", "Disconnecting user with name " + name);
			connectedUsers.remove(name);
		}
		if (connection == admin)
			this.admin = null;
	}
	
	/**
	 * Send a response to a certain connection whether they successfully logged in or not
	 * 
	 * @param connection connection on which to send login response
	 * @param successful whether the login was successful or not
	 */
	private void sendLoginResponse(Connection connection, boolean successful) {
		LoginResponse response = new LoginResponse();
		if (successful)
			response.success = true;
		else
			response.success = false;
		
		this.server.sendToTCP(connection.getID(), response);
	}
	
	/**
	 * Send an object to everyone
	 * 
	 * @param object object to be sent
	 */
	@SuppressWarnings("unused")
	private void sendToAll(Object object) {
		for (Connection connection : this.connectedUsers.values())
			connection.sendTCP(object);
	}
	
	/**
	 * Send an object to everyone except a certain connection
	 * 
	 * @param object object to be sent
	 * @param connection connection to send object on
	 */
	private void sendToAllExcept(Object object, Connection connection) {
		for (Connection c : this.connectedUsers.values())
		{
			if (c.getID() != connection.getID())
				c.sendTCP(object);
		}
	}
	
	/**
	 * Passwords are encrypted by themselves. Really doesn't work well at all
	 * but it's better than sending them in plaintext.
	 * 
	 * @param password password to verify with
	 * @return whether password is valid or not
	 */
	private boolean verifyPassword(String password) {
		String decrypted = null;
		Cipher pbeCipher = null;
		try {
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
	        SecretKey key = keyFactory.generateSecret(new PBEKeySpec(correctPass.toCharArray()));
	        pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
	        pbeCipher.init(Cipher.DECRYPT_MODE, key, new PBEParameterSpec(SALT, 20));
	        
		} catch (Exception e) {
			Log.error("ForwardingListener", "Exception in decryption routine", e);
			return false;
		}
		
		try {
			decrypted = new String(pbeCipher.doFinal(Base64.decodeBase64(password)), "UTF-8");
		} catch (BadPaddingException e) {
			return false;
		} catch (UnsupportedEncodingException e) {
			Log.error("ForwardingListener", "Exception in decryption routine", e);
			return false;
		} catch (IllegalBlockSizeException e) {
			Log.error("ForwardingListener", "Exception in decryption routine", e);
			return false;
		} 
		
		if (decrypted != null && decrypted.equals(correctPass))
			return true;
		else
			return false;
	}
	
	/**
	 * Admin enters a username that is encrypted by itself. Again,
	 * a pretty lousy technique, but it beats sending it in plaintext.
	 * 
	 * @param name admin username
	 * @return whether user is verified or not
	 */
	private boolean verifyAdmin(String name) {
		String decrypted = null;
		Cipher pbeCipher = null;
		try {
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
	        SecretKey key = keyFactory.generateSecret(new PBEKeySpec(correctUsername.toCharArray()));
	        pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
	        pbeCipher.init(Cipher.DECRYPT_MODE, key, new PBEParameterSpec(SALT, 20));
		} catch (Exception e) {
			Log.error("ForwardingListener", "Exception in decryption routine", e);
			return false;
		}
		
		try {
			decrypted = new String(pbeCipher.doFinal(Base64.decodeBase64(name)), "UTF-8");
		} catch (BadPaddingException e) {
			return false;
		} catch (UnsupportedEncodingException e) {
			Log.error("ForwardingListener", "Exception in decryption routine", e);
			return false;
		} catch (IllegalBlockSizeException e) {
			Log.info("Illegal block size exception occurred, probably just a normal user logging in");
			return false;
		} 
		
		if (decrypted != null && decrypted.equals(correctUsername))
			return true;
		else
			return false;
	}
}
