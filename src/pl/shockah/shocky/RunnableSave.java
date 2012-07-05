package pl.shockah.shocky;

public class RunnableSave implements Runnable {
	
	@Override
	public void run() {
		System.out.println("Saving...");
		long ticks = System.currentTimeMillis();
		Data.save();
		System.out.format("Done! Saving took %d milliseconds.\n", System.currentTimeMillis()-ticks);
	}
}