package pl.shockah.audio;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;

public abstract class AudioWave extends Audio {
	protected static final Mixer mixer;
	
	static {
		mixer = AudioSystem.getMixer(null);
	}
	
	public abstract float getPitch();
	public abstract void setPitch(float pitch);
}