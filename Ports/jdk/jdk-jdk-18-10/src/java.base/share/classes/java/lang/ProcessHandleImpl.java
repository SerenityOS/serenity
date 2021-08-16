/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.annotation.Native;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * ProcessHandleImpl is the implementation of ProcessHandle.
 *
 * @see Process
 * @since 9
 */
@jdk.internal.ValueBased
final class ProcessHandleImpl implements ProcessHandle {
    /**
     * Default size of stack for reaper processes.
     */
    private static long REAPER_DEFAULT_STACKSIZE = 128 * 1024;

    /**
     * Return value from waitForProcessExit0 indicating the process is not a child.
     */
    @Native
    private static final int NOT_A_CHILD = -2;

    /**
     * Cache the ProcessHandle of this process.
     */
    private static final ProcessHandleImpl current;

    /**
     * Map of pids to ExitCompletions.
     */
    private static final ConcurrentMap<Long, ExitCompletion>
            completions = new ConcurrentHashMap<>();

    static {
        initNative();
        long pid = getCurrentPid0();
        current = new ProcessHandleImpl(pid, isAlive0(pid));
    }

    private static native void initNative();

    /**
     * The thread pool of "process reaper" daemon threads.
     */
    @SuppressWarnings("removal")
    private static final Executor processReaperExecutor =
            AccessController.doPrivileged((PrivilegedAction<Executor>) () -> {
                // Initialize ThreadLocalRandom now to avoid using the smaller stack
                // of the processReaper threads.
                ThreadLocalRandom.current();

                ThreadGroup tg = Thread.currentThread().getThreadGroup();
                while (tg.getParent() != null) tg = tg.getParent();
                ThreadGroup systemThreadGroup = tg;

                // For a debug build, the stack shadow zone is larger;
                // Increase the total stack size to avoid potential stack overflow.
                int debugDelta = "release".equals(System.getProperty("jdk.debug")) ? 0 : (4*4096);
                final long stackSize = Boolean.getBoolean("jdk.lang.processReaperUseDefaultStackSize")
                        ? 0 : REAPER_DEFAULT_STACKSIZE + debugDelta;

                ThreadFactory threadFactory = grimReaper -> {
                    Thread t = new Thread(systemThreadGroup, grimReaper,
                            "process reaper", stackSize, false);
                    t.setDaemon(true);
                    // A small attempt (probably futile) to avoid priority inversion
                    t.setPriority(Thread.MAX_PRIORITY);
                    return t;
                };

                return Executors.newCachedThreadPool(threadFactory);
            });

    private static class ExitCompletion extends CompletableFuture<Integer> {
        final boolean isReaping;

        ExitCompletion(boolean isReaping) {
            this.isReaping = isReaping;
        }
    }

    /**
     * Returns a CompletableFuture that completes with process exit status when
     * the process completes.
     *
     * @param shouldReap true if the exit value should be reaped
     */
    static CompletableFuture<Integer> completion(long pid, boolean shouldReap) {
        // check canonicalizing cache 1st
        ExitCompletion completion = completions.get(pid);
        // re-try until we get a completion that shouldReap => isReaping
        while (completion == null || (shouldReap && !completion.isReaping)) {
            ExitCompletion newCompletion = new ExitCompletion(shouldReap);
            if (completion == null) {
                completion = completions.putIfAbsent(pid, newCompletion);
            } else {
                completion = completions.replace(pid, completion, newCompletion)
                    ? null : completions.get(pid);
            }
            if (completion == null) {
                // newCompletion has just been installed successfully
                completion = newCompletion;
                // spawn a thread to wait for and deliver the exit value
                processReaperExecutor.execute(new Runnable() {
                    // Use inner class to avoid lambda stack overhead
                    public void run() {
                        int exitValue = waitForProcessExit0(pid, shouldReap);
                        if (exitValue == NOT_A_CHILD) {
                            // pid not alive or not a child of this process
                            // If it is alive wait for it to terminate
                            long sleep = 300;     // initial milliseconds to sleep
                            int incr = 30;        // increment to the sleep time

                            long startTime = isAlive0(pid);
                            long origStart = startTime;
                            while (startTime >= 0) {
                                try {
                                    Thread.sleep(Math.min(sleep, 5000L)); // no more than 5 sec
                                    sleep += incr;
                                } catch (InterruptedException ie) {
                                    // ignore and retry
                                }
                                startTime = isAlive0(pid);  // recheck if it is alive
                                if (startTime > 0 && origStart > 0 && startTime != origStart) {
                                    // start time changed (and is not zero), pid is not the same process
                                    break;
                                }
                            }
                            exitValue = 0;
                        }
                        newCompletion.complete(exitValue);
                        // remove from cache afterwards
                        completions.remove(pid, newCompletion);
                    }
                });
            }
        }
        return completion;
    }

    @Override
    public CompletableFuture<ProcessHandle> onExit() {
        if (this.equals(current)) {
            throw new IllegalStateException("onExit for current process not allowed");
        }

        return ProcessHandleImpl.completion(pid(), false)
                .handleAsync((exitStatus, unusedThrowable) -> this);
    }

    /**
     * Wait for the process to exit, return the value.
     * Conditionally reap the value if requested
     * @param pid the processId
     * @param reapvalue if true, the value is retrieved,
     *                   else return the value and leave the process waitable
     *
     * @return the value or -1 if an error occurs
     */
    private static native int waitForProcessExit0(long pid, boolean reapvalue);

    /**
     * The pid of this ProcessHandle.
     */
    private final long pid;

    /**
     * The start time of this process.
     * If STARTTIME_ANY, the start time of the process is not available from the os.
     * If greater than zero, the start time of the process.
     */
    private final long startTime;

    /* The start time should match any value.
     * Typically, this is because the OS can not supply it.
     * The process is known to exist but not the exact start time.
     */
    private final long STARTTIME_ANY = 0L;

    /* The start time of a Process that does not exist. */
    private final long STARTTIME_PROCESS_UNKNOWN = -1;

    /**
     * Private constructor.  Instances are created by the {@code get(long)} factory.
     * @param pid the pid for this instance
     */
    private ProcessHandleImpl(long pid, long startTime) {
        this.pid = pid;
        this.startTime = startTime;
    }

    /**
     * Returns a ProcessHandle for an existing native process.
     *
     * @param  pid the native process identifier
     * @return The ProcessHandle for the pid if the process is alive;
     *         or {@code null} if the process ID does not exist in the native system.
     * @throws SecurityException if RuntimePermission("manageProcess") is not granted
     */
    static Optional<ProcessHandle> get(long pid) {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission("manageProcess"));
        }
        long start = isAlive0(pid);
        return (start >= 0)
                ? Optional.of(new ProcessHandleImpl(pid, start))
                : Optional.empty();
    }

    /**
     * Returns a ProcessHandle for an existing native process known to be alive.
     * The startTime of the process is retrieved and stored in the ProcessHandle.
     * It does not perform a security check since it is called from ProcessImpl.
     * @param pid of the known to exist process
     * @return a ProcessHandle corresponding to an existing Process instance
     */
    static ProcessHandleImpl getInternal(long pid) {
        return new ProcessHandleImpl(pid, isAlive0(pid));
    }

    /**
     * Returns the native process ID.
     * A {@code long} is used to be able to fit the system specific binary values
     * for the process.
     *
     * @return the native process ID
     */
    @Override
    public long pid() {
        return pid;
    }

    /**
     * Returns the ProcessHandle for the current native process.
     *
     * @return The ProcessHandle for the OS process.
     * @throws SecurityException if RuntimePermission("manageProcess") is not granted
     */
    public static ProcessHandleImpl current() {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission("manageProcess"));
        }
        return current;
    }

    /**
     * Return the pid of the current process.
     *
     * @return the pid of the  current process
     */
    private static native long getCurrentPid0();

    /**
     * Returns a ProcessHandle for the parent process.
     *
     * @return a ProcessHandle of the parent process; {@code null} is returned
     *         if the child process does not have a parent
     * @throws SecurityException           if permission is not granted by the
     *                                     security policy
     */
    public Optional<ProcessHandle> parent() {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission("manageProcess"));
        }
        long ppid = parent0(pid, startTime);
        if (ppid <= 0) {
            return Optional.empty();
        }
        return get(ppid);
    }

    /**
     * Returns the parent of the native pid argument.
     *
     * @param pid the process id
     * @param startTime the startTime of the process
     * @return the parent of the native pid; if any, otherwise -1
     */
    private static native long parent0(long pid, long startTime);

    /**
     * Returns the number of pids filled in to the array.
     * @param pid if {@code pid} equals zero, then all known processes are returned;
     *      otherwise only direct child process pids are returned
     * @param pids an allocated long array to receive the pids
     * @param ppids an allocated long array to receive the parent pids; may be null
     * @param starttimes an allocated long array to receive the child start times; may be null
     * @return if greater than or equal to zero is the number of pids in the array;
     *      if greater than the length of the arrays, the arrays are too small
     */
    private static native int getProcessPids0(long pid, long[] pids,
                                              long[] ppids, long[] starttimes);

    /**
     * Destroy the process for this ProcessHandle.
     * The native code checks the start time before sending the termination request.
     *
     * @param force {@code true} if the process should be terminated forcibly;
     *     else {@code false} for a normal termination
     */
    boolean destroyProcess(boolean force) {
        if (this.equals(current)) {
            throw new IllegalStateException("destroy of current process not allowed");
        }
        return destroy0(pid, startTime, force);
    }

    /**
     * Signal the process to terminate.
     * The process is signaled only if its start time matches the known start time.
     *
     * @param pid  process id to kill
     * @param startTime the start time of the process
     * @param forcibly true to forcibly terminate (SIGKILL vs SIGTERM)
     * @return true if the process was signaled without error; false otherwise
     */
    private static native boolean destroy0(long pid, long startTime, boolean forcibly);

    @Override
    public boolean destroy() {
        return destroyProcess(false);
    }

    @Override
    public boolean destroyForcibly() {
        return destroyProcess(true);
    }


    @Override
    public boolean supportsNormalTermination() {
        return ProcessImpl.SUPPORTS_NORMAL_TERMINATION;
    }

    /**
     * Tests whether the process represented by this {@code ProcessHandle} is alive.
     *
     * @return {@code true} if the process represented by this
     * {@code ProcessHandle} object has not yet terminated.
     * @since 9
     */
    @Override
    public boolean isAlive() {
        long start = isAlive0(pid);
        return (start >= 0 && (start == startTime || start == 0 || startTime == 0));
    }

    /**
     * Returns the process start time depending on whether the pid is alive.
     * This must not reap the exitValue.
     *
     * @param pid the pid to check
     * @return the start time in milliseconds since 1970,
     *         0 if the start time cannot be determined,
     *         -1 if the pid does not exist.
     */
    private static native long isAlive0(long pid);

    @Override
    public Stream<ProcessHandle> children() {
        // The native OS code selects based on matching the requested parent pid.
        // If the original parent exits, the pid may have been re-used for
        // this newer process.
        // Processes started by the original parent (now dead) will all have
        // start times less than the start of this newer parent.
        // Processes started by this newer parent will have start times equal
        // or after this parent.
        return children(pid).filter(ph -> startTime <= ((ProcessHandleImpl)ph).startTime);
    }

    /**
     * Returns a Stream of the children of a process or all processes.
     *
     * @param pid the pid of the process for which to find the children;
     *            0 for all processes
     * @return a stream of ProcessHandles
     */
    static Stream<ProcessHandle> children(long pid) {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission("manageProcess"));
        }
        int size = 100;
        long[] childpids = null;
        long[] starttimes = null;
        while (childpids == null || size > childpids.length) {
            childpids = new long[size];
            starttimes = new long[size];
            size = getProcessPids0(pid, childpids, null, starttimes);
        }

        final long[] cpids = childpids;
        final long[] stimes = starttimes;
        return IntStream.range(0, size).mapToObj(i -> new ProcessHandleImpl(cpids[i], stimes[i]));
    }

    @Override
    public Stream<ProcessHandle> descendants() {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission("manageProcess"));
        }
        int size = 100;
        long[] pids = null;
        long[] ppids = null;
        long[] starttimes = null;
        while (pids == null || size > pids.length) {
            pids = new long[size];
            ppids = new long[size];
            starttimes = new long[size];
            size = getProcessPids0(0, pids, ppids, starttimes);
        }

        int next = 0;       // index of next process to check
        int count = -1;     // count of subprocesses scanned
        long ppid = pid;    // start looking for this parent
        long ppStart = 0;
        // Find the start time of the parent
        for (int i = 0; i < size; i++) {
            if (pids[i] == ppid) {
                ppStart = starttimes[i];
                break;
            }
        }
        do {
            // Scan from next to size looking for ppid with child start time
            // the same or later than the parent.
            // If found, exchange it with index next
            for (int i = next; i < size; i++) {
                if (ppids[i] == ppid &&
                        ppStart <= starttimes[i]) {
                    swap(pids, i, next);
                    swap(ppids, i, next);
                    swap(starttimes, i, next);
                    next++;
                }
            }
            ppid = pids[++count];   // pick up the next pid to scan for
            ppStart = starttimes[count];    // and its start time
        } while (count < next);

        final long[] cpids = pids;
        final long[] stimes = starttimes;
        return IntStream.range(0, count).mapToObj(i -> new ProcessHandleImpl(cpids[i], stimes[i]));
    }

    // Swap two elements in an array
    private static void swap(long[] array, int x, int y) {
        long v = array[x];
        array[x] = array[y];
        array[y] = v;
    }

    @Override
    public ProcessHandle.Info info() {
        return ProcessHandleImpl.Info.info(pid, startTime);
    }

    @Override
    public int compareTo(ProcessHandle other) {
        return Long.compare(pid, ((ProcessHandleImpl) other).pid);
    }

    @Override
    public String toString() {
        return Long.toString(pid);
    }

    @Override
    public int hashCode() {
        return Long.hashCode(pid);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        return (obj instanceof ProcessHandleImpl other)
                && (pid == other.pid)
                && (startTime == other.startTime || startTime == 0 || other.startTime == 0);
    }

    /**
     * Implementation of ProcessHandle.Info.
     * Information snapshot about a process.
     * The attributes of a process vary by operating system and are not available
     * in all implementations.  Additionally, information about other processes
     * is limited by the operating system privileges of the process making the request.
     * If a value is not available, either a {@code null} or {@code -1} is stored.
     * The accessor methods return {@code null} if the value is not available.
     */
    static class Info implements ProcessHandle.Info {
        static {
            initIDs();
        }

        /**
         * Initialization of JNI fieldIDs.
         */
        private static native void initIDs();

        /**
         * Fill in this Info instance with information about the native process.
         * If values are not available the native code does not modify the field.
         * @param pid  of the native process
         */
        private native void info0(long pid);

        String command;
        String commandLine;
        String[] arguments;
        long startTime;
        long totalTime;
        String user;

        Info() {
            command = null;
            commandLine = null;
            arguments = null;
            startTime = -1L;
            totalTime = -1L;
            user = null;
        }

        /**
         * Returns the Info object with the fields from the process.
         * Whatever fields are provided by native are returned.
         * If the startTime of the process does not match the provided
         * startTime then an empty Info is returned.
         *
         * @param pid the native process identifier
         * @param startTime the startTime of the process being queried
         * @return ProcessHandle.Info non-null; individual fields may be null
         *          or -1 if not available.
         */
        public static ProcessHandle.Info info(long pid, long startTime) {
            Info info = new Info();
            info.info0(pid);
            if (startTime != info.startTime) {
                info.command = null;
                info.arguments = null;
                info.startTime = -1L;
                info.totalTime = -1L;
                info.user = null;
            }
            return info;
        }

        @Override
        public Optional<String> command() {
            return Optional.ofNullable(command);
        }

        @Override
        public Optional<String> commandLine() {
            if (command != null && arguments != null) {
                return Optional.of(command + " " + String.join(" ", arguments));
            } else {
                return Optional.ofNullable(commandLine);
            }
        }

        @Override
        public Optional<String[]> arguments() {
            return Optional.ofNullable(arguments);
        }

        @Override
        public Optional<Instant> startInstant() {
            return (startTime > 0)
                    ? Optional.of(Instant.ofEpochMilli(startTime))
                    : Optional.empty();
        }

        @Override
        public Optional<Duration> totalCpuDuration() {
            return (totalTime != -1)
                    ? Optional.of(Duration.ofNanos(totalTime))
                    : Optional.empty();
        }

        @Override
        public Optional<String> user() {
            return Optional.ofNullable(user);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(60);
            sb.append('[');
            if (user != null) {
                sb.append("user: ");
                sb.append(user());
            }
            if (command != null) {
                if (sb.length() != 0) sb.append(", ");
                sb.append("cmd: ");
                sb.append(command);
            }
            if (arguments != null && arguments.length > 0) {
                if (sb.length() != 0) sb.append(", ");
                sb.append("args: ");
                sb.append(Arrays.toString(arguments));
            }
            if (commandLine != null) {
                if (sb.length() != 0) sb.append(", ");
                sb.append("cmdLine: ");
                sb.append(commandLine);
            }
            if (startTime > 0) {
                if (sb.length() != 0) sb.append(", ");
                sb.append("startTime: ");
                sb.append(startInstant());
            }
            if (totalTime != -1) {
                if (sb.length() != 0) sb.append(", ");
                sb.append("totalTime: ");
                sb.append(totalCpuDuration().toString());
            }
            sb.append(']');
            return sb.toString();
        }
    }
}
