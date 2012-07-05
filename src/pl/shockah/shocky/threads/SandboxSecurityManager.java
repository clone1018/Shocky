package pl.shockah.shocky.threads;

import java.io.FilePermission;
import java.security.Permission;

public class SandboxSecurityManager extends SecurityManager 
{
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
		if (perm instanceof FilePermission)
			throw new SecurityException(perm.getName()+" is not allowed.");
	}
	@Override
	public void checkExit(int status) {
		throw new SecurityException();
	}
}
