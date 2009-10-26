package be.echo.clock;

import java.io.*;
import java.net.URL;
import javax.sound.sampled.*;
import javax.swing.*;

import static be.echo.clock.Log.log;

public class SoundClip extends Thread {

	private final String fileName;

	private Clip clip;

	private long soundLength;

	private URL url;

	private AudioInputStream audioIn;

	public SoundClip(String fileName) {
		try {
			this.fileName = fileName;
			log("loading " + fileName);
			url = this.getClass().getClassLoader().getResource(fileName);
			clip = AudioSystem.getClip();

		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void play() {
		Player p = new Player();
		p.start();
	}

	private class Player extends Thread {
		public void run() {
			try {
				audioIn = AudioSystem.getAudioInputStream(url);
				clip.open(audioIn);
				soundLength = clip.getMicrosecondLength();
				clip.start();
				log("started " + fileName);
				Thread.sleep(soundLength / 1000);
				clip.close();
				log("closed " + fileName);

			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e) {
				throw new RuntimeException("Failed playing " + fileName, e);
			}
		}
	}


	/** Tests playing sound clip. */
	public static void main(String[] args) throws Exception {
		String fileName = "buzz.wav";
		if (args.length > 0) {
			fileName = args[0];
		}
		SoundClip clip = new SoundClip(fileName);
		clip.play();
		System.out.println("waiting to play second time ..");
		Thread.sleep(5000);
		clip.play();
		System.out.println("waiting to close  ..");
		Thread.sleep(5000);
	}
}
