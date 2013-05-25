package pl.shockah.shocky.threads;

import java.io.File;
import java.io.FilePermission;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.util.PropertyPermission;

import sun.security.util.SecurityConstants;

public class SandboxSecurityManager extends SecurityManager 
{
	public final SandboxThreadGroup group;
	public final File[] readonlyFiles;
	private final PermissionCollection allowed = new Permissions();
	private final PermissionCollection disallowed = new Permissions();
	private final String[] libs = new String[] {
			"./libs/commons-lang3-3.1.jar",
			"./libs/LOLCODE-0.11.jar",
			"./libs/luaj-jse-2.0.2.jar",
			"./libs/pircbotx-1.8.jar",
	};
	
	private final File phpData = new File("data", "php").getAbsoluteFile();
	
	public boolean enabled = false;
	
	public SandboxSecurityManager(SandboxThreadGroup group) {
		this(group, new File[0]);
	}
	
	public SandboxSecurityManager(SandboxThreadGroup group, File... readonlyFiles) {
		super();
		this.group = group;
		this.readonlyFiles = readonlyFiles;
		
		allowed.add(new FilePermission(System.getProperty("java.home").replace('\\','/')+"/lib/-","read"));
		
		//String cd = '/'+System.getProperty("user.dir").replace('\\','/');
		for (int i = 0; i <libs.length; i++) {
			String lib = libs[i];
			allowed.add(new FilePermission(lib,"read"));
			allowed.add(new FilePermission(lib+"/-","read"));
		}
		
		if (this.readonlyFiles != null) {
			for (int i = 0; i < this.readonlyFiles.length; i++) {
				allowed.add(new FilePermission(this.readonlyFiles[i].getPath(),"read"));
				allowed.add(new FilePermission(this.readonlyFiles[i].getPath()+"/-","read"));
			}
		}
		
		allowed.add(new FilePermission(phpData.getPath(),"read,write"));
		allowed.add(new FilePermission(phpData.getPath()+"/-","read,write"));
		
		allowed.setReadOnly();
		
		disallowed.add(new PropertyPermission("*","write"));
	
		disallowed.add(new FilePermission("<<ALL FILES>>" ,"read,write,execute,delete"));
		
		disallowed.add(new RuntimePermission("setSecurityManager"));
		disallowed.add(new RuntimePermission("shutdownHooks"));
		disallowed.add(new RuntimePermission("accessClassInPackage.sun.reflect"));
		
		disallowed.add(SecurityConstants.GET_CLASSLOADER_PERMISSION);
		disallowed.add(SecurityConstants.STOP_THREAD_PERMISSION);
		disallowed.add(SecurityConstants.DO_AS_PRIVILEGED_PERMISSION);
		
		disallowed.setReadOnly();
	}
	
	@Override
	public void checkPermission(Permission perm) 
	{
		if (!enabled || getThreadGroup() != group)
			return;
		
		boolean disallow = disallowed.implies(perm);
		boolean allow = allowed.implies(perm);
		
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
