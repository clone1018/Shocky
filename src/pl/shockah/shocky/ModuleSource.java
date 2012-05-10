package pl.shockah.shocky;

public abstract class ModuleSource {
	public final boolean accept(ModuleLoader loader) {
		return loader.accept(this);
	}
	
	public static class FileSource extends ModuleSource {
		public final java.io.File file;
		public FileSource(java.io.File file) {
			this.file = file;
		}
	}
	public static class URLSource extends ModuleSource {
		public final java.net.URL url;
		public URLSource(java.net.URL url) {
			this.url = url;
		}
	}
}