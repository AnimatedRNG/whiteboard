package com.animated.rng.whiteboard.util;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.DeflateSerializer;

/**
 * Does what BlowfishSerializer would have done, except in a less buggy way. <br>
 * <i> Currently doesn't do anything </i>
 * 
 * @author Srinivas Kaza
 */
@SuppressWarnings("rawtypes")
public final class EncryptSerializer extends Serializer {
	private final Serializer serializer;
	private SecretKeySpec keySpec;
	
	private static Cipher decryptCipher;
	private static Cipher encryptCipher;
	
	/**
	 * @param serializer {@link Serializer} to wrap
	 * @param key key to use for encryption/decryption
	 */
	public EncryptSerializer(Serializer serializer, byte[] key) {
        this.serializer = new DeflateSerializer(serializer);
        this.keySpec = new SecretKeySpec(key, "Blowfish");
        try {
			decryptCipher = Cipher.getInstance("Blowfish");
			encryptCipher = Cipher.getInstance("Blowfish");
			decryptCipher.init(Cipher.DECRYPT_MODE, keySpec);
			encryptCipher.init(Cipher.ENCRYPT_MODE, keySpec);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Object read(Kryo kryo, Input input, Class type) {
		/*CipherInputStream cipherInput = new CipherInputStream(new InputChunked(input, 256), decryptCipher);
		Object obj = kryo.readObject(new Input(cipherInput, 256), type, serializer);
		return obj;
		System.out.println("READING ENCRYPTED OBJECT");*/
		
		return kryo.readObject(input, type, serializer);
	}
	
	@Override
	public void write(Kryo kryo, Output output, Object object) {
		/*CipherOutputStream cipherStream = new CipherOutputStream(new OutputChunked(output, 256), encryptCipher);
		Output cipherOutput = new Output(cipherStream, 256) {
			
			public void close() throws KryoException {
				
			}
		};
		
		kryo.writeObject(cipherOutput, object, serializer);
		cipherOutput.flush();
		try {
			cipherStream.close();
		} catch (IOException ex) {
			throw new KryoException(ex);
		}
		System.out.println("WRITING ENCRYPTED OBJECT");*/
		
		kryo.writeObject(output, object, serializer);
	}
}