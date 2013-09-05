import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Module;

public class ModuleAutoGC extends Module {
	private ScheduledExecutorService timer;
	private ScheduledFuture<?> futureGC = null;
	
	public String name() {return "autogc";}
	public void onEnable(File dir) {
		Data.config.setNotExists("autogc-delay",300);
		int delay = Data.config.getInt("autogc-delay");
		
		timer = Executors.newScheduledThreadPool(1);
		futureGC = timer.scheduleAtFixedRate(new Runnable(){
			public void run() {
				System.gc();
			}
		},delay,delay,TimeUnit.SECONDS);
	}
	public void onDisable() {
		futureGC.cancel(false);
	}
}