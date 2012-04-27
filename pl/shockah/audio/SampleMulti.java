package pl.shockah.audio;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;

public class SampleMulti extends AudioWave implements LineListener {
	protected ArrayList<Sample> samples = new ArrayList<Sample>();
	protected AudioInputStream ais;
	
	public SampleMulti(File file) {
		try {
			ais = AudioSystem.getAudioInputStream(file);
		} catch (Exception e) {e.printStackTrace();}
	}
	public SampleMulti(URL url) {
		try {
			ais = AudioSystem.getAudioInputStream(url);
		} catch (Exception e) {e.printStackTrace();}
	}
	
	public ArrayList<Sample> getSamples() {
		return new ArrayList<Sample>(samples);
	}

	public void play() {
		Sample sample = new Sample(ais);
		sample.clip.addLineListener(this);
		samples.add(sample);
		sample.play();
	}
	public void loop() {
		Sample sample = new Sample(ais);
		sample.clip.addLineListener(this);
		samples.add(sample);
		sample.loop();
	}
	public void pause() {
		for (Sample sample : samples) sample.pause();
	}
	public void stop() {
		for (Sample sample : samples) {
			sample.stop();
			sample.clip.removeLineListener(this);
		}
		samples.clear();
	}
	public boolean isPlaying() {
		for (Sample sample : samples) if (sample.isPlaying()) return true;
		return false;
	}
	public boolean isPaused() {
		for (Sample sample : samples) if (!sample.isPaused()) return false;
		return true;
	}
	public boolean isStopped() {
		for (Sample sample : samples) if (!sample.isStopped()) return false;
		return true;
	}
	public boolean isLooping() {
		for (Sample sample : samples) if (sample.isLooping()) return true;
		return false;
	}
	
	public float getPitch() {
		float p = 0; int n = 0;
		for (Sample sample : samples) {p += sample.getPitch(); n++;}
		if (n == 0) return 1;
		return p/n;
	}
	public void setPitch(float pitch) {
		for (Sample sample : samples) sample.setPitch(pitch);
	}
	
	public void update(LineEvent event) {
		if (event.getType() == LineEvent.Type.STOP) {
			for (int i = 0; i < samples.size(); i++) if (event.getSource() == samples.get(i)) {
				if (!samples.get(i).isPaused()) {
					samples.get(i).clip.removeLineListener(this);
					samples.remove(i);
					return;
				}
			}
		}
	}
}