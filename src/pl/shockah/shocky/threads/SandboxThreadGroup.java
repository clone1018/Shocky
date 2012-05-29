package pl.shockah.shocky.threads;

public class SandboxThreadGroup extends ThreadGroup {
    public SandboxThreadGroup(String s)
    {
        this(Thread.currentThread().getThreadGroup(), s);
    }

    public SandboxThreadGroup(ThreadGroup threadgroup, String s)
    {
        super(threadgroup, s);
        setMaxPriority(4);
    }
}
