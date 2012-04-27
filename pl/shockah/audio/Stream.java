package pl.shockah.audio;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.SourceDataLine;

public class Stream extends AudioWave implements LineListener {
	protected SourceDataLine sdl;
	protected AudioInputStream ais;
	protected boolean paused = false;
	protected float frequency, pitch = 1;
	
	protected ThreadStream thread = null;
	protected File ctrFile = null;
	protected URL ctrURL = null;
	
	public Stream(File file) {
		try {
			ctrFile = file;
		} catch (Exception e) {e.printStackTrace();}
	}
	public Stream(URL url) {
		try {
			ctrURL = url;
		} catch (Exception e) {e.printStackTrace();}
	}
	public Stream(InputStream is) {
		try {
			ais = AudioSystem.getAudioInputStream(is);
			sdl = AudioSystem.getSourceDataLine(ais.getFormat(),mixer.getMixerInfo());
			sdl.open(ais.getFormat());
			sdl.addLineListener(this);
		} catch (Exception e) {e.printStackTrace();}
	}
	public Stream(AudioInputStream ais) {
		try {
			this.ais = ais;
			sdl = AudioSystem.getSourceDataLine(ais.getFormat(),mixer.getMixerInfo());
			sdl.open(ais.getFormat());
			sdl.addLineListener(this);
		} catch (Exception e) {e.printStackTrace();}
	}
	
	public void play() {play(false);}
	public void loop() {play(true);}
	public void play(boolean loop) {
		if (!isPaused()) stop();
		
		(thread = new ThreadStream(loop){
			public void run() {
				try {
					do {
						if (!isPaused()) restartStream();
						paused = false;
						sdl.start();
						
						int numRead = 0;
						byte[] buf = new byte[sdl.getBufferSize()];
						while ((numRead = ais.read(buf,0,buf.length)) >= 0 && !stopThread) {
							int offset = 0;
							while (offset < numRead) {
								offset += sdl.write(buf,offset,numRead-offset);
								if (stopThread) return;
							}
							if (stopThread) return;
						}
						if (stopThread) return;
						sdl.drain();
						if (stopThread) return;
						sdl.stop();
						sdl.flush();
					} while (isLooping() && !stopThread);
				} catch (Exception e) {e.printStackTrace();}
			}
		}).start();
	}
	public void pause() {
		if (thread == null) return;
		thread.stopThread = true;
		sdl.stop();
		paused = true;
	}
	public void stop() {
		if (thread == null) return;
		thread.stopThread = true;
		thread.looping = false;
		paused = false;
		sdl.stop();
		sdl.flush();
		thread = null;
	}
	public boolean isPlaying() {
		if (sdl == null) return false;
		return sdl.isRunning();
	}
	public boolean isPaused() {
		return paused;
	}
	public boolean isStopped() {
		return !isPaused() && !isPlaying();
	}
	public boolean isLooping() {
		if (thread == null) return false;
		return thread.looping;
	}
	
	public void update(LineEvent event) {
		if (event.getType() == LineEvent.Type.START) paused = false;
		if (event.getType() == LineEvent.Type.STOP && !isLooping() && !isPaused()) stop();
	}
	
	public float getPitch() {
		return pitch;
	}
	public void setPitch(float pitch) {
		this.pitch = pitch;
		((FloatControl)sdl.getControl(FloatControl.Type.SAMPLE_RATE)).setValue(frequency*pitch);
	}
	
	protected void restartStream() {
		try {
			if (ctrFile == null && ctrURL == null) return;
			
			if (sdl != null && sdl.isOpen()) {
				sdl.stop();
				sdl.flush();
				sdl.removeLineListener(this);
				sdl.close();
			}
			
			if (ctrFile != null) ais = AudioSystem.getAudioInputStream(ctrFile);
			else if (ctrURL != null) ais = AudioSystem.getAudioInputStream(ctrURL);
			
			sdl = AudioSystem.getSourceDataLine(ais.getFormat(),mixer.getMixerInfo());
			sdl.open(ais.getFormat());
			sdl.addLineListener(this);
			
			frequency = sdl.getFormat().getSampleRate();
			setPitch(getPitch());
		} catch (Exception e) {e.printStackTrace();}
	}
	
	protected static class ThreadStream extends Thread {
		protected boolean looping;
		protected boolean stopThread = false;
		
		public ThreadStream(boolean looping) {
			this.looping = looping;
		}
	}
}