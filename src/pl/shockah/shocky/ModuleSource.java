package pl.shockah.shocky;

public class ModuleSource<T> {
	public final T source;
	
	public ModuleSource(T source) {
		this.source = source;
	}
	
	public final boolean accept(ModuleLoader loader) {
		return loader.accept(this);
	}
}