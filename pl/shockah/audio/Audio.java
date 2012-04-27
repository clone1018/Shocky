package pl.shockah.audio;

public abstract class Audio {
	public abstract void play();
	public abstract void loop();
	public abstract void pause();
	public abstract void stop();
	public abstract boolean isPlaying();
	public abstract boolean isPaused();
	public abstract boolean isStopped();
	public abstract boolean isLooping();
}