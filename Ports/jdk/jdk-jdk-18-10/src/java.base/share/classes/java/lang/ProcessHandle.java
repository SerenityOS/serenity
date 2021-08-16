/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * ProcessHandle identifies and provides control of native processes. Each
 * individual process can be monitored for liveness, list its children,
 * get information about the process or destroy it.
 * By comparison, {@link java.lang.Process Process} instances were started
 * by the current process and additionally provide access to the process
 * input, output, and error streams.
 * <p>
 * The native process ID is an identification number that the
 * operating system assigns to the process.
 * The range for process id values is dependent on the operating system.
 * For example, an embedded system might use a 16-bit value.
 * Status information about a process is retrieved from the native system
 * and may change asynchronously; processes may be created or terminate
 * spontaneously.
 * The time between when a process terminates and the process id
 * is reused for a new process is unpredictable.
 * Race conditions can exist between checking the status of a process and
 * acting upon it. When using ProcessHandles avoid assumptions
 * about the liveness or identity of the underlying process.
 * <p>
 * Each ProcessHandle identifies and allows control of a process in the native
 * system. ProcessHandles are returned from the factory methods {@link #current()},
 * {@link #of(long)},
 * {@link #children}, {@link #descendants}, {@link #parent()} and
 * {@link #allProcesses()}.
 * <p>
 * The {@link Process} instances created by {@link ProcessBuilder} can be queried
 * for a ProcessHandle that provides information about the Process.
 * ProcessHandle references should not be freely distributed.
 *
 * <p>
 * A {@link java.util.concurrent.CompletableFuture} available from {@link #onExit}
 * can be used to wait for process termination, and possibly trigger dependent
 * actions.
 * <p>
 * The factory methods limit access to ProcessHandles using the
 * SecurityManager checking the {@link RuntimePermission RuntimePermission("manageProcess")}.
 * The ability to control processes is also restricted by the native system,
 * ProcessHandle provides no more access to, or control over, the native process
 * than would be allowed by a native application.
 *
 * @implSpec
 * In the case where ProcessHandles cannot be supported then the factory
 * methods must consistently throw {@link java.lang.UnsupportedOperationException}.
 * The methods of this class throw {@link java.lang.UnsupportedOperationException}
 * if the operating system does not allow access to query or kill a process.
 *
 * <p>
 * The {@code ProcessHandle} static factory methods return instances that are
 * <a href="{@docRoot}/java.base/java/lang/doc-files/ValueBased.html">value-based</a>,
 * immutable and thread-safe. Programmers should treat instances that are
 * {@linkplain #equals(Object) equal} as interchangeable and should not
 * use instances for synchronization, or unpredictable behavior may occur.
 * For example, in a future release, synchronization may fail.
 * Use the {@code equals} or {@link #compareTo(ProcessHandle) compareTo} methods
 * to compare ProcessHandles.
 *
 * @see Process
 * @since 9
 */
@jdk.internal.ValueBased
public interface ProcessHandle extends Comparable<ProcessHandle> {

    /**
     * Returns the native process ID of the process. The native process ID is an
     * identification number that the operating system assigns to the process.
     * The operating system may reuse the process ID after a process terminates.
     * Use {@link #equals(Object) equals} or
     * {@link #compareTo(ProcessHandle) compareTo} to compare ProcessHandles.
     *
     * @return the native process ID of the process
     * @throws UnsupportedOperationException if the implementation
     *         does not support this operation
     */
    long pid();

    /**
     * Returns an {@code Optional<ProcessHandle>} for an existing native process.
     *
     * @param pid a native process ID
     * @return an {@code Optional<ProcessHandle>} of the PID for the process;
     *         the {@code Optional} is empty if the process does not exist
     * @throws SecurityException if a security manager has been installed and
     *         it denies RuntimePermission("manageProcess")
     * @throws UnsupportedOperationException if the implementation
     *         does not support this operation
     */
    public static Optional<ProcessHandle> of(long pid) {
        return ProcessHandleImpl.get(pid);
    }

    /**
     * Returns a ProcessHandle for the current process. The ProcessHandle cannot be
     * used to destroy the current process, use {@link System#exit System.exit} instead.
     *
     * @return a ProcessHandle for the current process
     * @throws SecurityException if a security manager has been installed and
     *         it denies RuntimePermission("manageProcess")
     * @throws UnsupportedOperationException if the implementation
     *         does not support this operation
     */
    public static ProcessHandle current() {
        return ProcessHandleImpl.current();
    }

    /**
     * Returns an {@code Optional<ProcessHandle>} for the parent process.
     * Note that Processes in a zombie state usually don't have a parent.
     *
     * @return an {@code Optional<ProcessHandle>} of the parent process;
     *         the {@code Optional} is empty if the child process does not have a parent
     *         or if the parent is not available, possibly due to operating system limitations
     * @throws SecurityException if a security manager has been installed and
     *         it denies RuntimePermission("manageProcess")
     */
    Optional<ProcessHandle> parent();

    /**
     * Returns a snapshot of the current direct children of the process.
     * The {@link #parent} of a direct child process is the process.
     * Typically, a process that is {@link #isAlive not alive} has no children.
     * <p>
     * <em>Note that processes are created and terminate asynchronously.
     * There is no guarantee that a process is {@link #isAlive alive}.
     * </em>
     *
     * @return a sequential Stream of ProcessHandles for processes that are
     *         direct children of the process
     * @throws SecurityException if a security manager has been installed and
     *         it denies RuntimePermission("manageProcess")
     */
    Stream<ProcessHandle> children();

    /**
     * Returns a snapshot of the descendants of the process.
     * The descendants of a process are the children of the process
     * plus the descendants of those children, recursively.
     * Typically, a process that is {@link #isAlive not alive} has no children.
     * <p>
     * <em>Note that processes are created and terminate asynchronously.
     * There is no guarantee that a process is {@link #isAlive alive}.
     * </em>
     *
     * @return a sequential Stream of ProcessHandles for processes that
     *         are descendants of the process
     * @throws SecurityException if a security manager has been installed and
     *         it denies RuntimePermission("manageProcess")
     */
    Stream<ProcessHandle> descendants();

    /**
     * Returns a snapshot of all processes visible to the current process.
     * <p>
     * <em>Note that processes are created and terminate asynchronously. There
     * is no guarantee that a process in the stream is alive or that no other
     * processes may have been created since the inception of the snapshot.
     * </em>
     *
     * @return a Stream of ProcessHandles for all processes
     * @throws SecurityException if a security manager has been installed and
     *         it denies RuntimePermission("manageProcess")
     * @throws UnsupportedOperationException if the implementation
     *         does not support this operation
     */
    static Stream<ProcessHandle> allProcesses() {
        return ProcessHandleImpl.children(0);
    }

    /**
     * Returns a snapshot of information about the process.
     *
     * <p> A {@link ProcessHandle.Info} instance has accessor methods that return
     * information about the process if it is available.
     *
     * @return a snapshot of information about the process, always non-null
     */
    Info info();

    /**
     * Information snapshot about the process.
     * The attributes of a process vary by operating system and are not available
     * in all implementations.  Information about processes is limited
     * by the operating system privileges of the process making the request.
     * The return types are {@code Optional<T>} allowing explicit tests
     * and actions if the value is available.
     * @since 9
     */
    public interface Info {
        /**
         * Returns the executable pathname of the process.
         *
         * @return an {@code Optional<String>} of the executable pathname
         *         of the process
         */
        public Optional<String> command();

        /**
         * Returns the command line of the process.
         * <p>
         * If {@link #command command()} and  {@link #arguments arguments()} return
         * non-empty optionals, this is simply a convenience method which concatenates
         * the values of the two functions separated by spaces. Otherwise it will return a
         * best-effort, platform dependent representation of the command line.
         *
         * @apiNote Note that the returned executable pathname and the
         *          arguments may be truncated on some platforms due to system
         *          limitations.
         *          <p>
         *          The executable pathname may contain only the
         *          name of the executable without the full path information.
         *          It is undecideable whether white space separates different
         *          arguments or is part of a single argument.
         *
         * @return an {@code Optional<String>} of the command line
         *         of the process
         */
        public Optional<String> commandLine();

        /**
         * Returns an array of Strings of the arguments of the process.
         *
         * @apiNote On some platforms, native applications are free to change
         *          the arguments array after startup and this method may only
         *          show the changed values.
         *
         * @return an {@code Optional<String[]>} of the arguments of the process
         */
        public Optional<String[]> arguments();

        /**
         * Returns the start time of the process.
         *
         * @return an {@code Optional<Instant>} of the start time of the process
         */
        public Optional<Instant> startInstant();

        /**
         * Returns the total cputime accumulated of the process.
         *
         * @return an {@code Optional<Duration>} for the accumulated total cputime
         */
        public Optional<Duration> totalCpuDuration();

        /**
         * Return the user of the process.
         *
         * @return an {@code Optional<String>} for the user of the process
         */
        public Optional<String> user();
    }

    /**
     * Returns a {@code CompletableFuture<ProcessHandle>} for the termination
     * of the process.
     * The {@link java.util.concurrent.CompletableFuture} provides the ability
     * to trigger dependent functions or actions that may be run synchronously
     * or asynchronously upon process termination.
     * When the process has terminated the CompletableFuture is
     * {@link java.util.concurrent.CompletableFuture#complete completed} regardless
     * of the exit status of the process.
     * The {@code onExit} method can be called multiple times to invoke
     * independent actions when the process exits.
     * <p>
     * Calling {@code onExit().get()} waits for the process to terminate and returns
     * the ProcessHandle. The future can be used to check if the process is
     * {@link java.util.concurrent.CompletableFuture#isDone done} or to
     * {@link java.util.concurrent.Future#get() wait} for it to terminate.
     * {@link java.util.concurrent.Future#cancel(boolean) Cancelling}
     * the CompleteableFuture does not affect the Process.
     * @apiNote
     * The process may be observed to have terminated with {@link #isAlive}
     * before the ComputableFuture is completed and dependent actions are invoked.
     *
     * @return a new {@code CompletableFuture<ProcessHandle>} for the ProcessHandle
     *
     * @throws IllegalStateException if the process is the current process
     */
    CompletableFuture<ProcessHandle> onExit();

    /**
     * Returns {@code true} if the implementation of {@link #destroy}
     * normally terminates the process.
     * Returns {@code false} if the implementation of {@code destroy}
     * forcibly and immediately terminates the process.
     *
     * @return {@code true} if the implementation of {@link #destroy}
     *         normally terminates the process;
     *         otherwise, {@link #destroy} forcibly terminates the process
     */
    boolean supportsNormalTermination();

    /**
     * Requests the process to be killed.
     * Whether the process represented by this {@code ProcessHandle} object is
     * {@link #supportsNormalTermination normally terminated} or not is
     * implementation dependent.
     * Forcible process destruction is defined as the immediate termination of the
     * process, whereas normal termination allows the process to shut down cleanly.
     * If the process is not alive, no action is taken.
     * The operating system access controls may prevent the process
     * from being killed.
     * <p>
     * The {@link java.util.concurrent.CompletableFuture} from {@link #onExit} is
     * {@link java.util.concurrent.CompletableFuture#complete completed}
     * when the process has terminated.
     * <p>
     * Note: The process may not terminate immediately.
     * For example, {@code isAlive()} may return true for a brief period
     * after {@code destroy()} is called.
     *
     * @return {@code true} if termination was successfully requested,
     *         otherwise {@code false}
     * @throws IllegalStateException if the process is the current process
     */
    boolean destroy();

    /**
     * Requests the process to be killed forcibly.
     * The process represented by this {@code ProcessHandle} object is
     * forcibly terminated.
     * Forcible process destruction is defined as the immediate termination of the
     * process, whereas normal termination allows the process to shut down cleanly.
     * If the process is not alive, no action is taken.
     * The operating system access controls may prevent the process
     * from being killed.
     * <p>
     * The {@link java.util.concurrent.CompletableFuture} from {@link #onExit} is
     * {@link java.util.concurrent.CompletableFuture#complete completed}
     * when the process has terminated.
     * <p>
     * Note: The process may not terminate immediately.
     * For example, {@code isAlive()} may return true for a brief period
     * after {@code destroyForcibly()} is called.
     *
     * @return {@code true} if termination was successfully requested,
     *         otherwise {@code false}
     * @throws IllegalStateException if the process is the current process
     */
    boolean destroyForcibly();

    /**
     * Tests whether the process represented by this {@code ProcessHandle} is alive.
     * Process termination is implementation and operating system specific.
     * The process is considered alive as long as the PID is valid.
     *
     * @return {@code true} if the process represented by this
     *         {@code ProcessHandle} object has not yet terminated
     */
    boolean isAlive();

    /**
     * Returns a hash code value for this ProcessHandle.
     * The hashcode value follows the general contract for {@link Object#hashCode()}.
     * The value is a function of the {@link #pid pid()} value and
     * may be a function of additional information to uniquely identify the process.
     * If two ProcessHandles are equal according to the {@link #equals(Object) equals}
     * method, then calling the hashCode method on each of the two objects
     * must produce the same integer result.
     *
     * @return a hash code value for this object
     */
    @Override
    int hashCode();

    /**
     * Returns {@code true} if {@code other} object is non-null, is of the
     * same implementation, and represents the same system process;
     * otherwise it returns {@code false}.
     * @implNote
     * It is implementation specific whether ProcessHandles with the same PID
     * represent the same system process. ProcessHandle implementations
     * should contain additional information to uniquely identify the process.
     * For example, the start time of the process could be used
     * to determine if the PID has been re-used.
     * The implementation of {@code equals} should return {@code true} for two
     * ProcessHandles with the same PID unless there is information to
     * distinguish them.
     *
     * @param other another object
     * @return {@code true} if the {@code other} object is non-null,
     *         is of the same implementation class and represents
     *         the same system process; otherwise returns {@code false}
     */
    @Override
    boolean equals(Object other);

    /**
     * Compares this ProcessHandle with the specified ProcessHandle for order.
     * The order is not specified, but is consistent with {@link Object#equals},
     * which returns {@code true} if and only if two instances of ProcessHandle
     * are of the same implementation and represent the same system process.
     * Comparison is only supported among objects of same implementation.
     * If attempt is made to mutually compare two different implementations
     * of {@link ProcessHandle}s, {@link ClassCastException} is thrown.
     *
     * @param other the ProcessHandle to be compared
     * @return a negative integer, zero, or a positive integer as this object
     * is less than, equal to, or greater than the specified object.
     * @throws NullPointerException if the specified object is null
     * @throws ClassCastException if the specified object is not of same class
     *         as this object
     */
    @Override
    int compareTo(ProcessHandle other);

}
