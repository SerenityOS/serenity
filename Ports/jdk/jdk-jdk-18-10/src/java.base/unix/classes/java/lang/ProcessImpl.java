/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.lang;

import java.lang.ProcessBuilder.Redirect;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Properties;
import jdk.internal.access.JavaIOFileDescriptorAccess;
import jdk.internal.access.SharedSecrets;
import jdk.internal.util.StaticProperty;
import sun.security.action.GetPropertyAction;

/**
 * java.lang.Process subclass in the UNIX environment.
 *
 * @author Mario Wolczko and Ross Knippel.
 * @author Konstantin Kladko (ported to Linux and Bsd)
 * @author Martin Buchholz
 * @author Volker Simonis (ported to AIX)
 * @since   1.5
 */
final class ProcessImpl extends Process {
    private static final JavaIOFileDescriptorAccess fdAccess
        = SharedSecrets.getJavaIOFileDescriptorAccess();

    // Linux platforms support a normal (non-forcible) kill signal.
    static final boolean SUPPORTS_NORMAL_TERMINATION = true;

    private final int pid;
    private final ProcessHandleImpl processHandle;
    private int exitcode;
    private boolean hasExited;

    private /* final */ OutputStream stdin;
    private /* final */ InputStream  stdout;
    private /* final */ InputStream  stderr;

    private static enum LaunchMechanism {
        // order IS important!
        FORK,
        POSIX_SPAWN,
        VFORK
    }

    private static enum Platform {

        LINUX(LaunchMechanism.POSIX_SPAWN, LaunchMechanism.VFORK, LaunchMechanism.FORK),

        BSD(LaunchMechanism.POSIX_SPAWN, LaunchMechanism.FORK),

        AIX(LaunchMechanism.POSIX_SPAWN, LaunchMechanism.FORK);

        final LaunchMechanism defaultLaunchMechanism;
        final Set<LaunchMechanism> validLaunchMechanisms;

        Platform(LaunchMechanism ... launchMechanisms) {
            this.defaultLaunchMechanism = launchMechanisms[0];
            this.validLaunchMechanisms =
                EnumSet.copyOf(Arrays.asList(launchMechanisms));
        }

        @SuppressWarnings("removal")
        LaunchMechanism launchMechanism() {
            return AccessController.doPrivileged(
                (PrivilegedAction<LaunchMechanism>) () -> {
                    String s = System.getProperty(
                        "jdk.lang.Process.launchMechanism");
                    LaunchMechanism lm;
                    if (s == null) {
                        lm = defaultLaunchMechanism;
                        s = lm.name().toLowerCase(Locale.ENGLISH);
                    } else {
                        try {
                            lm = LaunchMechanism.valueOf(
                                s.toUpperCase(Locale.ENGLISH));
                        } catch (IllegalArgumentException e) {
                            lm = null;
                        }
                    }
                    if (lm == null || !validLaunchMechanisms.contains(lm)) {
                        throw new Error(
                            s + " is not a supported " +
                            "process launch mechanism on this platform."
                        );
                    }
                    return lm;
                }
            );
        }

        static Platform get() {
            String osName = GetPropertyAction.privilegedGetProperty("os.name");

            if (osName.equals("Linux")) { return LINUX; }
            if (osName.contains("OS X")) { return BSD; }
            if (osName.equals("AIX")) { return AIX; }

            throw new Error(osName + " is not a supported OS platform.");
        }
    }

    private static final Platform platform = Platform.get();
    private static final LaunchMechanism launchMechanism = platform.launchMechanism();
    private static final byte[] helperpath = toCString(StaticProperty.javaHome() + "/lib/jspawnhelper");

    private static byte[] toCString(String s) {
        if (s == null)
            return null;
        byte[] bytes = s.getBytes();
        byte[] result = new byte[bytes.length + 1];
        System.arraycopy(bytes, 0,
                         result, 0,
                         bytes.length);
        result[result.length-1] = (byte)0;
        return result;
    }

    // Only for use by ProcessBuilder.start()
    static Process start(String[] cmdarray,
                         java.util.Map<String,String> environment,
                         String dir,
                         ProcessBuilder.Redirect[] redirects,
                         boolean redirectErrorStream)
            throws IOException
    {
        assert cmdarray != null && cmdarray.length > 0;

        // Convert arguments to a contiguous block; it's easier to do
        // memory management in Java than in C.
        byte[][] args = new byte[cmdarray.length-1][];
        int size = args.length; // For added NUL bytes
        for (int i = 0; i < args.length; i++) {
            args[i] = cmdarray[i+1].getBytes();
            size += args[i].length;
        }
        byte[] argBlock = new byte[size];
        int i = 0;
        for (byte[] arg : args) {
            System.arraycopy(arg, 0, argBlock, i, arg.length);
            i += arg.length + 1;
            // No need to write NUL bytes explicitly
        }

        int[] envc = new int[1];
        byte[] envBlock = ProcessEnvironment.toEnvironmentBlock(environment, envc);

        int[] std_fds;

        FileInputStream  f0 = null;
        FileOutputStream f1 = null;
        FileOutputStream f2 = null;

        try {
            boolean forceNullOutputStream = false;
            if (redirects == null) {
                std_fds = new int[] { -1, -1, -1 };
            } else {
                std_fds = new int[3];

                if (redirects[0] == Redirect.PIPE) {
                    std_fds[0] = -1;
                } else if (redirects[0] == Redirect.INHERIT) {
                    std_fds[0] = 0;
                } else if (redirects[0] instanceof ProcessBuilder.RedirectPipeImpl) {
                    std_fds[0] = fdAccess.get(((ProcessBuilder.RedirectPipeImpl) redirects[0]).getFd());
                } else {
                    f0 = new FileInputStream(redirects[0].file());
                    std_fds[0] = fdAccess.get(f0.getFD());
                }

                if (redirects[1] == Redirect.PIPE) {
                    std_fds[1] = -1;
                } else if (redirects[1] == Redirect.INHERIT) {
                    std_fds[1] = 1;
                } else if (redirects[1] instanceof ProcessBuilder.RedirectPipeImpl) {
                    std_fds[1] = fdAccess.get(((ProcessBuilder.RedirectPipeImpl) redirects[1]).getFd());
                    // Force getInputStream to return a null stream,
                    // the fd is directly assigned to the next process.
                    forceNullOutputStream = true;
                } else {
                    f1 = new FileOutputStream(redirects[1].file(),
                            redirects[1].append());
                    std_fds[1] = fdAccess.get(f1.getFD());
                }

                if (redirects[2] == Redirect.PIPE) {
                    std_fds[2] = -1;
                } else if (redirects[2] == Redirect.INHERIT) {
                    std_fds[2] = 2;
                } else if (redirects[2] instanceof ProcessBuilder.RedirectPipeImpl) {
                    std_fds[2] = fdAccess.get(((ProcessBuilder.RedirectPipeImpl) redirects[2]).getFd());
                } else {
                    f2 = new FileOutputStream(redirects[2].file(),
                            redirects[2].append());
                    std_fds[2] = fdAccess.get(f2.getFD());
                }
            }

            Process p = new ProcessImpl
                    (toCString(cmdarray[0]),
                            argBlock, args.length,
                            envBlock, envc[0],
                            toCString(dir),
                            std_fds,
                            forceNullOutputStream,
                            redirectErrorStream);
            if (redirects != null) {
                // Copy the fd's if they are to be redirected to another process
                if (std_fds[0] >= 0 &&
                        redirects[0] instanceof ProcessBuilder.RedirectPipeImpl) {
                    fdAccess.set(((ProcessBuilder.RedirectPipeImpl) redirects[0]).getFd(), std_fds[0]);
                }
                if (std_fds[1] >= 0 &&
                        redirects[1] instanceof ProcessBuilder.RedirectPipeImpl) {
                    fdAccess.set(((ProcessBuilder.RedirectPipeImpl) redirects[1]).getFd(), std_fds[1]);
                }
                if (std_fds[2] >= 0 &&
                        redirects[2] instanceof ProcessBuilder.RedirectPipeImpl) {
                    fdAccess.set(((ProcessBuilder.RedirectPipeImpl) redirects[2]).getFd(), std_fds[2]);
                }
            }
            return p;
        } finally {
            // In theory, close() can throw IOException
            // (although it is rather unlikely to happen here)
            try { if (f0 != null) f0.close(); }
            finally {
                try { if (f1 != null) f1.close(); }
                finally { if (f2 != null) f2.close(); }
            }
        }
    }


    /**
     * Creates a process. Depending on the {@code mode} flag, this is done by
     * one of the following mechanisms:
     * <pre>
     *   1 - fork(2) and exec(2)
     *   2 - posix_spawn(3P)
     *   3 - vfork(2) and exec(2)
     * </pre>
     * @param fds an array of three file descriptors.
     *        Indexes 0, 1, and 2 correspond to standard input,
     *        standard output and standard error, respectively.  On
     *        input, a value of -1 means to create a pipe to connect
     *        child and parent processes.  On output, a value which
     *        is not -1 is the parent pipe fd corresponding to the
     *        pipe which has been created.  An element of this array
     *        is -1 on input if and only if it is <em>not</em> -1 on
     *        output.
     * @return the pid of the subprocess
     */
    private native int forkAndExec(int mode, byte[] helperpath,
                                   byte[] prog,
                                   byte[] argBlock, int argc,
                                   byte[] envBlock, int envc,
                                   byte[] dir,
                                   int[] fds,
                                   boolean redirectErrorStream)
        throws IOException;

    @SuppressWarnings("removal")
    private ProcessImpl(final byte[] prog,
                final byte[] argBlock, final int argc,
                final byte[] envBlock, final int envc,
                final byte[] dir,
                final int[] fds,
                final boolean forceNullOutputStream,
                final boolean redirectErrorStream)
            throws IOException {

        pid = forkAndExec(launchMechanism.ordinal() + 1,
                          helperpath,
                          prog,
                          argBlock, argc,
                          envBlock, envc,
                          dir,
                          fds,
                          redirectErrorStream);
        processHandle = ProcessHandleImpl.getInternal(pid);

        try {
            AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {
                initStreams(fds, forceNullOutputStream);
                return null;
            });
        } catch (PrivilegedActionException ex) {
            throw (IOException) ex.getCause();
        }
    }

    static FileDescriptor newFileDescriptor(int fd) {
        FileDescriptor fileDescriptor = new FileDescriptor();
        fdAccess.set(fileDescriptor, fd);
        return fileDescriptor;
    }

    /**
     * Initialize the streams from the file descriptors.
     * @param fds array of stdin, stdout, stderr fds
     * @param forceNullOutputStream true if the stdout is being directed to
     *        a subsequent process. The stdout stream should be a null output stream .
     * @throws IOException
     */
    void initStreams(int[] fds, boolean forceNullOutputStream) throws IOException {
        switch (platform) {
            case LINUX:
            case BSD:
                stdin = (fds[0] == -1) ?
                        ProcessBuilder.NullOutputStream.INSTANCE :
                        new ProcessPipeOutputStream(fds[0]);

                stdout = (fds[1] == -1 || forceNullOutputStream) ?
                         ProcessBuilder.NullInputStream.INSTANCE :
                         new ProcessPipeInputStream(fds[1]);

                stderr = (fds[2] == -1) ?
                         ProcessBuilder.NullInputStream.INSTANCE :
                         new ProcessPipeInputStream(fds[2]);

                ProcessHandleImpl.completion(pid, true).handle((exitcode, throwable) -> {
                    synchronized (this) {
                        this.exitcode = (exitcode == null) ? -1 : exitcode.intValue();
                        this.hasExited = true;
                        this.notifyAll();
                    }

                    if (stdout instanceof ProcessPipeInputStream)
                        ((ProcessPipeInputStream) stdout).processExited();

                    if (stderr instanceof ProcessPipeInputStream)
                        ((ProcessPipeInputStream) stderr).processExited();

                    if (stdin instanceof ProcessPipeOutputStream)
                        ((ProcessPipeOutputStream) stdin).processExited();

                    return null;
                });
                break;

            case AIX:
                stdin = (fds[0] == -1) ?
                        ProcessBuilder.NullOutputStream.INSTANCE :
                        new ProcessPipeOutputStream(fds[0]);

                stdout = (fds[1] == -1 || forceNullOutputStream) ?
                         ProcessBuilder.NullInputStream.INSTANCE :
                         new DeferredCloseProcessPipeInputStream(fds[1]);

                stderr = (fds[2] == -1) ?
                         ProcessBuilder.NullInputStream.INSTANCE :
                         new DeferredCloseProcessPipeInputStream(fds[2]);

                ProcessHandleImpl.completion(pid, true).handle((exitcode, throwable) -> {
                    synchronized (this) {
                        this.exitcode = (exitcode == null) ? -1 : exitcode.intValue();
                        this.hasExited = true;
                        this.notifyAll();
                    }

                    if (stdout instanceof DeferredCloseProcessPipeInputStream)
                        ((DeferredCloseProcessPipeInputStream) stdout).processExited();

                    if (stderr instanceof DeferredCloseProcessPipeInputStream)
                        ((DeferredCloseProcessPipeInputStream) stderr).processExited();

                    if (stdin instanceof ProcessPipeOutputStream)
                        ((ProcessPipeOutputStream) stdin).processExited();

                    return null;
                });
                break;

            default: throw new AssertionError("Unsupported platform: " + platform);
        }
    }

    public OutputStream getOutputStream() {
        return stdin;
    }

    public InputStream getInputStream() {
        return stdout;
    }

    public InputStream getErrorStream() {
        return stderr;
    }

    public synchronized int waitFor() throws InterruptedException {
        while (!hasExited) {
            wait();
        }
        return exitcode;
    }

    @Override
    public synchronized boolean waitFor(long timeout, TimeUnit unit)
        throws InterruptedException
    {
        long remainingNanos = unit.toNanos(timeout);    // throw NPE before other conditions
        if (hasExited) return true;
        if (timeout <= 0) return false;

        long deadline = System.nanoTime() + remainingNanos;
        do {
            TimeUnit.NANOSECONDS.timedWait(this, remainingNanos);
            if (hasExited) {
                return true;
            }
            remainingNanos = deadline - System.nanoTime();
        } while (remainingNanos > 0);
        return hasExited;
    }

    public synchronized int exitValue() {
        if (!hasExited) {
            throw new IllegalThreadStateException("process hasn't exited");
        }
        return exitcode;
    }

    private void destroy(boolean force) {
        switch (platform) {
            case LINUX:
            case BSD:
            case AIX:
                // There is a risk that pid will be recycled, causing us to
                // kill the wrong process!  So we only terminate processes
                // that appear to still be running.  Even with this check,
                // there is an unavoidable race condition here, but the window
                // is very small, and OSes try hard to not recycle pids too
                // soon, so this is quite safe.
                synchronized (this) {
                    if (!hasExited)
                        processHandle.destroyProcess(force);
                }
                try { stdin.close();  } catch (IOException ignored) {}
                try { stdout.close(); } catch (IOException ignored) {}
                try { stderr.close(); } catch (IOException ignored) {}
                break;

            default: throw new AssertionError("Unsupported platform: " + platform);
        }
    }

    @Override
    public CompletableFuture<Process> onExit() {
        return ProcessHandleImpl.completion(pid, false)
                .handleAsync((unusedExitStatus, unusedThrowable) -> {
                    boolean interrupted = false;
                    while (true) {
                        // Ensure that the concurrent task setting the exit status has completed
                        try {
                            waitFor();
                            break;
                        } catch (InterruptedException ie) {
                            interrupted = true;
                        }
                    }
                    if (interrupted) {
                        Thread.currentThread().interrupt();
                    }
                    return this;
                });
    }

    @Override
    public ProcessHandle toHandle() {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission("manageProcess"));
        }
        return processHandle;
    }

    @Override
    public boolean supportsNormalTermination() {
        return ProcessImpl.SUPPORTS_NORMAL_TERMINATION;
    }

    @Override
    public void destroy() {
        destroy(false);
    }

    @Override
    public Process destroyForcibly() {
        destroy(true);
        return this;
    }

    @Override
    public long pid() {
        return pid;
    }

    @Override
    public synchronized boolean isAlive() {
        return !hasExited;
    }

    /**
     * The {@code toString} method returns a string consisting of
     * the native process ID of the process and the exit value of the process.
     *
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        return new StringBuilder("Process[pid=").append(pid)
                .append(", exitValue=").append(hasExited ? exitcode : "\"not exited\"")
                .append("]").toString();
    }

    private static native void init();

    static {
        init();
    }

    /**
     * A buffered input stream for a subprocess pipe file descriptor
     * that allows the underlying file descriptor to be reclaimed when
     * the process exits, via the processExited hook.
     *
     * This is tricky because we do not want the user-level InputStream to be
     * closed until the user invokes close(), and we need to continue to be
     * able to read any buffered data lingering in the OS pipe buffer.
     */
    private static class ProcessPipeInputStream extends BufferedInputStream {
        private final Object closeLock = new Object();

        ProcessPipeInputStream(int fd) {
            super(new PipeInputStream(newFileDescriptor(fd)));
        }
        private static byte[] drainInputStream(InputStream in)
                throws IOException {
            int n = 0;
            int j;
            byte[] a = null;
            while ((j = in.available()) > 0) {
                a = (a == null) ? new byte[j] : Arrays.copyOf(a, n + j);
                n += in.read(a, n, j);
            }
            return (a == null || n == a.length) ? a : Arrays.copyOf(a, n);
        }

        /** Called by the process reaper thread when the process exits. */
        synchronized void processExited() {
            synchronized (closeLock) {
                try {
                    InputStream in = this.in;
                    // this stream is closed if and only if: in == null
                    if (in != null) {
                        byte[] stragglers = drainInputStream(in);
                        in.close();
                        this.in = (stragglers == null) ?
                            ProcessBuilder.NullInputStream.INSTANCE :
                            new ByteArrayInputStream(stragglers);
                    }
                } catch (IOException ignored) {}
            }
        }

        @Override
        public void close() throws IOException {
            // BufferedInputStream#close() is not synchronized unlike most other
            // methods. Synchronizing helps avoid race with processExited().
            synchronized (closeLock) {
                super.close();
            }
        }
    }

    /**
     * A buffered output stream for a subprocess pipe file descriptor
     * that allows the underlying file descriptor to be reclaimed when
     * the process exits, via the processExited hook.
     */
    private static class ProcessPipeOutputStream extends BufferedOutputStream {
        ProcessPipeOutputStream(int fd) {
            super(new FileOutputStream(newFileDescriptor(fd)));
        }

        /** Called by the process reaper thread when the process exits. */
        synchronized void processExited() {
            OutputStream out = this.out;
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignored) {
                    // We know of no reason to get an IOException, but if
                    // we do, there's nothing else to do but carry on.
                }
                this.out = ProcessBuilder.NullOutputStream.INSTANCE;
            }
        }
    }

    // A FileInputStream that supports the deferment of the actual close
    // operation until the last pending I/O operation on the stream has
    // finished.  This is required on Solaris because we must close the stdin
    // and stdout streams in the destroy method in order to reclaim the
    // underlying file descriptors.  Doing so, however, causes any thread
    // currently blocked in a read on one of those streams to receive an
    // IOException("Bad file number"), which is incompatible with historical
    // behavior.  By deferring the close we allow any pending reads to see -1
    // (EOF) as they did before.
    //
    private static class DeferredCloseInputStream extends PipeInputStream {
        DeferredCloseInputStream(FileDescriptor fd) {
            super(fd);
        }

        private Object lock = new Object();     // For the following fields
        private boolean closePending = false;
        private int useCount = 0;
        private InputStream streamToClose;

        private void raise() {
            synchronized (lock) {
                useCount++;
            }
        }

        private void lower() throws IOException {
            synchronized (lock) {
                useCount--;
                if (useCount == 0 && closePending) {
                    streamToClose.close();
                }
            }
        }

        // stc is the actual stream to be closed; it might be this object, or
        // it might be an upstream object for which this object is downstream.
        //
        private void closeDeferred(InputStream stc) throws IOException {
            synchronized (lock) {
                if (useCount == 0) {
                    stc.close();
                } else {
                    closePending = true;
                    streamToClose = stc;
                }
            }
        }

        public void close() throws IOException {
            synchronized (lock) {
                useCount = 0;
                closePending = false;
            }
            super.close();
        }

        public int read() throws IOException {
            raise();
            try {
                return super.read();
            } finally {
                lower();
            }
        }

        public int read(byte[] b) throws IOException {
            raise();
            try {
                return super.read(b);
            } finally {
                lower();
            }
        }

        public int read(byte[] b, int off, int len) throws IOException {
            raise();
            try {
                return super.read(b, off, len);
            } finally {
                lower();
            }
        }

        public long skip(long n) throws IOException {
            raise();
            try {
                return super.skip(n);
            } finally {
                lower();
            }
        }

        public int available() throws IOException {
            raise();
            try {
                return super.available();
            } finally {
                lower();
            }
        }
    }

    /**
     * A buffered input stream for a subprocess pipe file descriptor
     * that allows the underlying file descriptor to be reclaimed when
     * the process exits, via the processExited hook.
     *
     * This is tricky because we do not want the user-level InputStream to be
     * closed until the user invokes close(), and we need to continue to be
     * able to read any buffered data lingering in the OS pipe buffer.
     *
     * On AIX this is especially tricky, because the 'close()' system call
     * will block if another thread is at the same time blocked in a file
     * operation (e.g. 'read()') on the same file descriptor. We therefore
     * combine 'ProcessPipeInputStream' approach used on Linux and Bsd
     * with the DeferredCloseInputStream approach used on Solaris. This means
     * that every potentially blocking operation on the file descriptor
     * increments a counter before it is executed and decrements it once it
     * finishes. The 'close()' operation will only be executed if there are
     * no pending operations. Otherwise it is deferred after the last pending
     * operation has finished.
     *
     */
    private static class DeferredCloseProcessPipeInputStream
        extends BufferedInputStream {

        private final Object closeLock = new Object();
        private int useCount = 0;
        private boolean closePending = false;

        DeferredCloseProcessPipeInputStream(int fd) {
            super(new PipeInputStream(newFileDescriptor(fd)));
        }

        private InputStream drainInputStream(InputStream in)
                throws IOException {
            int n = 0;
            int j;
            byte[] a = null;
            synchronized (closeLock) {
                if (buf == null) // asynchronous close()?
                    return null; // discard
                j = in.available();
            }
            while (j > 0) {
                a = (a == null) ? new byte[j] : Arrays.copyOf(a, n + j);
                synchronized (closeLock) {
                    if (buf == null) // asynchronous close()?
                        return null; // discard
                    n += in.read(a, n, j);
                    j = in.available();
                }
            }
            return (a == null) ?
                    ProcessBuilder.NullInputStream.INSTANCE :
                    new ByteArrayInputStream(n == a.length ? a : Arrays.copyOf(a, n));
        }

        /** Called by the process reaper thread when the process exits. */
        synchronized void processExited() {
            try {
                InputStream in = this.in;
                if (in != null) {
                    InputStream stragglers = drainInputStream(in);
                    in.close();
                    this.in = stragglers;
                }
            } catch (IOException ignored) { }
        }

        private void raise() {
            synchronized (closeLock) {
                useCount++;
            }
        }

        private void lower() throws IOException {
            synchronized (closeLock) {
                useCount--;
                if (useCount == 0 && closePending) {
                    closePending = false;
                    super.close();
                }
            }
        }

        @Override
        public int read() throws IOException {
            raise();
            try {
                return super.read();
            } finally {
                lower();
            }
        }

        @Override
        public int read(byte[] b) throws IOException {
            raise();
            try {
                return super.read(b);
            } finally {
                lower();
            }
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            raise();
            try {
                return super.read(b, off, len);
            } finally {
                lower();
            }
        }

        @Override
        public long skip(long n) throws IOException {
            raise();
            try {
                return super.skip(n);
            } finally {
                lower();
            }
        }

        @Override
        public int available() throws IOException {
            raise();
            try {
                return super.available();
            } finally {
                lower();
            }
        }

        @Override
        public void close() throws IOException {
            // BufferedInputStream#close() is not synchronized unlike most other
            // methods. Synchronizing helps avoid racing with drainInputStream().
            synchronized (closeLock) {
                if (useCount == 0) {
                    super.close();
                }
                else {
                    closePending = true;
                }
            }
        }
    }
}
