package com.animated.rng.whiteboard.network;

import java.net.URL;
import java.util.ArrayList;

import com.animated.rng.whiteboard.update.EraserUpdate;
import com.animated.rng.whiteboard.update.ImageUpdate;
import com.animated.rng.whiteboard.update.PencilUpdate;
import com.animated.rng.whiteboard.update.PointerUpdate;
import com.animated.rng.whiteboard.update.StateUpdate;
import com.animated.rng.whiteboard.update.TextUpdate;
import com.animated.rng.whiteboard.update.WhiteboardUpdate;
import com.animated.rng.whiteboard.util.ImageMark;
import com.animated.rng.whiteboard.util.PointerMark;
import com.animated.rng.whiteboard.util.ScaledPoint;
import com.animated.rng.whiteboard.util.TextMark;
import com.esotericsoftware.kryo.Kryo;

/**
 * Registers classes with {@link Kryo}
 * 
 * @author Srinivas Kaza
 */
public abstract class Registrar {

	/**
	 * Register classes which don't need encryption
	 * 
	 * @param kryo kryo with which to register objects
	 */
	public static final void registerUnencryptedClasses(Kryo kryo) {
		kryo.register(LoginRequest.class);
		kryo.register(LoginResponse.class);
	}
	
	/**
	 * Register classes which require encryption
	 * 
	 * @param kryo kryo with which to register objects
	 * @param pass password for PBE
	 */
	public static final void registerEncryptedClasses(Kryo kryo, String pass) {
		/*byte[] key = null;
		try {
			SecretKeySpec keySpec = new SecretKeySpec(pass.getBytes("UTF8"), "Blowfish");
			key = keySpec.getEncoded();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		kryo.register(ScaledPoint.class, new EncryptSerializer(kryo.getDefaultSerializer(ScaledPoint.class), key));
		kryo.register(WhiteboardUpdate.class, new EncryptSerializer(kryo.getDefaultSerializer(WhiteboardUpdate.class), key));
		kryo.register(PencilUpdate.class, new EncryptSerializer(kryo.getDefaultSerializer(PencilUpdate.class), key));
		kryo.register(EraserUpdate.class, new EncryptSerializer(kryo.getDefaultSerializer(EraserUpdate.class), key));
		kryo.register(StateUpdate.class,  new EncryptSerializer(kryo.getDefaultSerializer(StateUpdate.class), key));
		kryo.register(ArrayList.class, new EncryptSerializer(kryo.getDefaultSerializer(ArrayList.class), key));
		kryo.register(double[].class, new EncryptSerializer(kryo.getDefaultSerializer(double[].class), key));
		kryo.register(TextUpdate.class, new EncryptSerializer(kryo.getDefaultSerializer(TextUpdate.class), key));
		kryo.register(TextMark.class, new EncryptSerializer(kryo.getDefaultSerializer(TextMark.class), key));
		kryo.register(URL.class, new EncryptSerializer(kryo.getDefaultSerializer(URL.class), key));
		kryo.register(ImageUpdate.class, new EncryptSerializer(kryo.getDefaultSerializer(ImageUpdate.class), key));
		kryo.register(ImageMark.class, new EncryptSerializer(kryo.getDefaultSerializer(ImageMark.class), key));
		kryo.register(PointerUpdate.class, new EncryptSerializer(kryo.getDefaultSerializer(PointerUpdate.class), key));
		kryo.register(PointerMark.class, new EncryptSerializer(kryo.getDefaultSerializer(PointerMark.class), key));*/
		
		kryo.register(ScaledPoint.class, kryo.getDefaultSerializer(ScaledPoint.class));
		kryo.register(WhiteboardUpdate.class, kryo.getDefaultSerializer(WhiteboardUpdate.class));
		kryo.register(PencilUpdate.class, kryo.getDefaultSerializer(PencilUpdate.class));
		kryo.register(EraserUpdate.class, kryo.getDefaultSerializer(EraserUpdate.class));
		kryo.register(StateUpdate.class,  kryo.getDefaultSerializer(StateUpdate.class));
		kryo.register(ArrayList.class, kryo.getDefaultSerializer(ArrayList.class));
		kryo.register(double[].class, kryo.getDefaultSerializer(double[].class));
		kryo.register(TextUpdate.class, kryo.getDefaultSerializer(TextUpdate.class));
		kryo.register(TextMark.class, kryo.getDefaultSerializer(TextMark.class));
		kryo.register(URL.class, kryo.getDefaultSerializer(URL.class));
		kryo.register(ImageUpdate.class, kryo.getDefaultSerializer(ImageUpdate.class));
		kryo.register(ImageMark.class, kryo.getDefaultSerializer(ImageMark.class));
		kryo.register(PointerUpdate.class, kryo.getDefaultSerializer(PointerUpdate.class));
		kryo.register(PointerMark.class, kryo.getDefaultSerializer(PointerMark.class));
	}
}
