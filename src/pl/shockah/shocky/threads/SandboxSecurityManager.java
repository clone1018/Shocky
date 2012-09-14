package pl.shockah.shocky.threads;

import java.io.File;
import java.io.FilePermission;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.util.PropertyPermission;

public class SandboxSecurityManager extends SecurityManager 
{
	public final SandboxThreadGroup group;
	public final File readonlyDirectory;
	public boolean enabled = false;
	private final PermissionCollection allowed = new Permissions();
	private final PermissionCollection disallowed = new Permissions();
	private String[] libs = new String[] {
			"/libs/commons-lang3-3.1.jar",
			"/libs/LOLCODE-0.11.jar",
			"/libs/luaj-jse-2.0.2.jar",
			"/libs/pircbotx-1.8.jar",
	};
	
	public SandboxSecurityManager(SandboxThreadGroup group) {
		this(group,null);
	}
	
	public SandboxSecurityManager(SandboxThreadGroup group, File readonlyDirectory) {
		super();
		this.group = group;
		this.readonlyDirectory = readonlyDirectory;
		
		allowed.add(new FilePermission(System.getProperty("java.home")+"/-","read"));
		
		String cd = '/'+System.getProperty("user.dir").replace('\\','/');
		for (int i = 0; i <libs.length; i++) {
			String lib = cd+libs[i];
			allowed.add(new FilePermission(lib,"read"));
			allowed.add(new FilePermission(lib+"/-","read"));
		}
		
		if (this.readonlyDirectory != null) {
			allowed.add(new FilePermission(this.readonlyDirectory.getPath(),"read"));
			allowed.add(new FilePermission(this.readonlyDirectory.getPath()+"/-","read"));
		}
		allowed.setReadOnly();
		
		disallowed.add(new PropertyPermission("*","write"));
	
		disallowed.add(new FilePermission("<<ALL FILES>>" ,"read,write,execute,delete"));
		
		disallowed.add(new RuntimePermission("setSecurityManager"));
		disallowed.add(new RuntimePermission("shutdownHooks"));
		
		disallowed.setReadOnly();
	}
	
	@Override
	public void checkPermission(Permission perm) 
	{
		if (!enabled || getThreadGroup() != group)
			return;
		boolean allow = allowed.implies(perm);
		boolean disallow = disallowed.implies(perm);
		System.out.format("%s: A:%s D:%s", perm, allow, disallow);
		System.out.println();
		if (disallow) {
			if (!allow)
				throw new SecurityException("Not allowed: "+perm.toString());
		}
	}
	
	@Override
	public void checkExit(int status) {
		throw new SecurityException();
	}

	@Override
	public void checkPackageAccess(String pkg) {
		if (enabled && getThreadGroup() == group) {
			if (
					pkg.startsWith("java.util.concurrent")||
					pkg.startsWith("java.lang.reflect")
				)
				throw new SecurityException(pkg+" package is not allowed.");
		}
		super.checkPackageAccess(pkg);
	}
}
