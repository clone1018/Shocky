package pl.shockah.shocky.threads;

import java.io.File;
import java.io.FilePermission;
import java.security.Permission;

public class SandboxSecurityManager extends SecurityManager 
{
	public final File readonlyDirectory;
	
	public SandboxSecurityManager() {
		this(null);
	}
	
	public SandboxSecurityManager(File readonlyDirectory) {
		super();
		this.readonlyDirectory = readonlyDirectory;
	}
	
	@Override
	public void checkPermission(Permission perm) 
	{
		if (!(getThreadGroup() instanceof SandboxThreadGroup))
			return;
		if (perm instanceof RuntimePermission)
		{
			RuntimePermission runtime = (RuntimePermission)perm;
			if (runtime.getName().contentEquals("shutdownHooks") ||
				runtime.getName().contentEquals("setSecurityManager"))
				throw new SecurityException(perm.getName()+" is not allowed.");
		}
		if (perm instanceof FilePermission) {
			FilePermission fperm = (FilePermission)perm;
			String file = fperm.getName();
			String actions = fperm.getActions();
			if (readonlyDirectory != null && actions.contentEquals("read"))
			{
				if (file.startsWith(this.readonlyDirectory.getPath()))
					return;
			}
			throw new SecurityException(perm.getName()+" is not allowed.");
		}
	}
	@Override
	public void checkExit(int status) {
		throw new SecurityException();
	}
}
