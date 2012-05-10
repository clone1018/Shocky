package pl.shockah.shocky;

public class ModuleSource {
	public final boolean accept(ModuleLoader loader) {
		return loader.accept(this);
	}
	
	public static class File extends ModuleSource {
		public final java.io.File file;
		public File(java.io.File file) {
			this.file = file;
		}
	}
	public static class URL extends ModuleSource {
		public final java.net.URL url;
		public URL(java.net.URL url) {
			this.url = url;
		}
	}
}