package pl.shockah.shocky;

import java.io.File;
import java.util.ArrayList;
import pl.shockah.Config;
import pl.shockah.FileLine;

public class Data {
	public static final Config config = new Config();
	public static final ArrayList<String> controllers = new ArrayList<String>();
	public static final ArrayList<String> channels = new ArrayList<String>();
	public static final ArrayList<String> blacklistNicks = new ArrayList<String>();
	
	public static synchronized void blank() {}
	public static synchronized void load() {
		File dir = new File("data"); dir.mkdir();
		
		config.load(new File(dir,"config.cfg"));
		if (new File(dir,"controllers.cfg").exists()) {
			controllers.addAll(FileLine.read(new File(dir,"controllers.cfg")));
			channels.addAll(FileLine.read(new File(dir,"channels.cfg")));
			blacklistNicks.addAll(FileLine.read(new File(dir,"blacklistNicks.cfg")));
		} else blank();
	}
	public static synchronized void save() {
		File dir = new File("data"); dir.mkdir();
		
		config.save(new File(dir,"config.cfg"));
		FileLine.write(new File(dir,"controllers.cfg"),controllers);
		FileLine.write(new File(dir,"channels.cfg"),channels);
		FileLine.write(new File(dir,"blacklistNicks.cfg"),blacklistNicks);
		
		for (Module module : Module.getModules(false)) module.onDataSave();
	}
}