package pl.shockah.tasks;

public abstract class Task extends Thread {
	protected final ITaskCallback callback;
	protected volatile String progressStr = "";
	protected volatile float progress = 0f;
	
	public Task(ITaskCallback callback) {
		this.callback = callback;
	}
	
	public void run() {
		if (callback != null) callback.callTaskFinished(this);
	}
	
	public final String getProgressStr() {
		return progressStr;
	}
	public final float getProgress() {
		return progress;
	}
}