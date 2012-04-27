package pl.shockah.audio;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;

public class Sample extends AudioWave implements LineListener {
	protected Clip clip;
	protected boolean paused = false, looping = false;
	protected float frequency, pitch = 1;
	
	public Sample(File file) {
		try {
			clip = AudioSystem.getClip(mixer.getMixerInfo());
			clip.open(AudioSystem.getAudioInputStream(file));
			frequency = clip.getFormat().getSampleRate();
			clip.addLineListener(this);
		} catch (Exception e) {e.printStackTrace();}
	}
	public Sample(URL url) {
		try {
			clip = AudioSystem.getClip(mixer.getMixerInfo());
			clip.open(AudioSystem.getAudioInputStream(url));
			frequency = clip.getFormat().getSampleRate();
			clip.addLineListener(this);
		} catch (Exception e) {e.printStackTrace();}
	}
	public Sample(InputStream is) {
		try {
			clip = AudioSystem.getClip(mixer.getMixerInfo());
			clip.open(AudioSystem.getAudioInputStream(is));
			frequency = clip.getFormat().getSampleRate();
			clip.addLineListener(this);
		} catch (Exception e) {e.printStackTrace();}
	}
	public Sample(AudioInputStream ais) {
		try {
			clip = AudioSystem.getClip(mixer.getMixerInfo());
			clip.open(ais);
			frequency = clip.getFormat().getSampleRate();
			clip.addLineListener(this);
		} catch (Exception e) {e.printStackTrace();}
	}
	
	public void play() {play(false);}
	public void loop() {play(true);}
	public void play(boolean loop) {
		looping = loop;
		if (!isPaused()) stop();
		paused = false;
		if (loop) clip.loop(Clip.LOOP_CONTINUOUSLY);
		else clip.start();
	}
	public void pause() {
		paused = true;
		clip.stop();
	}
	public void stop() {
		paused = false;
		clip.stop();
		clip.setFramePosition(0);
	}
	public boolean isPlaying() {
		return clip.isRunning();
	}
	public boolean isPaused() {
		return paused;
	}
	public boolean isStopped() {
		return !isPaused() && !isPlaying();
	}
	public boolean isLooping() {
		return looping;
	}
	
	public float getPitch() {
		return pitch;
	}
	public void setPitch(float pitch) {
		this.pitch = pitch;
		((FloatControl)clip.getControl(FloatControl.Type.SAMPLE_RATE)).setValue(frequency*pitch);
	}

	public void update(LineEvent event) {
		if (event.getType() == LineEvent.Type.START) paused = false;
	}
}