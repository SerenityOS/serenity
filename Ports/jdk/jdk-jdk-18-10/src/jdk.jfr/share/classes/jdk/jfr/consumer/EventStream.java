/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.consumer;

import java.io.IOException;
import java.nio.file.Path;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Consumer;

import jdk.jfr.internal.SecuritySupport;
import jdk.jfr.internal.Utils;
import jdk.jfr.internal.consumer.EventDirectoryStream;
import jdk.jfr.internal.consumer.EventFileStream;
import jdk.jfr.internal.consumer.FileAccess;

/**
 * Represents a stream of events.
 * <p>
 * A stream is a sequence of events and the way to interact with a stream is to
 * register actions. The {@code EventStream} interface is not to be implemented
 * and future versions of the JDK may prevent this completely.
 * <p>
 * To receive a notification when an event arrives, register an action using the
 * {@link #onEvent(Consumer)} method. To filter the stream for an event with a
 * specific name, use {@link #onEvent(String, Consumer)} method.
 * <p>
 * By default, the same {@code RecordedEvent} object can be used to
 * represent two or more distinct events. That object can be delivered
 * multiple times to the same action as well as to other actions. To use an
 * event object after the action is completed, the
 * {@link #setReuse(boolean)} method should be set to {@code false} so a
 * new object is allocated for each event.
 * <p>
 * Events are delivered in batches. To receive a notification when a batch is
 * complete, register an action using the {@link #onFlush(Runnable)} method.
 * This is an opportunity to aggregate or push data to external systems while
 * the Java Virtual Machine (JVM) is preparing the next batch.
 * <p>
 * Events within a batch are sorted chronologically by their end time.
 * Well-ordering of events is only maintained for events available to the JVM at
 * the point of flush, i.e. for the set of events delivered as a unit in a
 * single batch. Events delivered in a batch could therefore be out-of-order
 * compared to events delivered in a previous batch, but never out-of-order with
 * events within the same batch. If ordering is not a concern, sorting can be
 * disabled using the {@link #setOrdered(boolean)} method.
 * <p>
 * To dispatch events to registered actions, the stream must be started. To
 * start processing in the current thread, invoke the {@link #start()} method.
 * To process actions asynchronously in a separate thread, invoke the
 * {@link #startAsync()} method. To await completion of the stream, use the
 * awaitTermination {@link #awaitTermination()} or the
 * {@link #awaitTermination(Duration)} method.
 * <p>
 * When a stream ends it is automatically closed. To manually stop processing of
 * events, close the stream by invoking the {@link #close()} method. A stream
 * can also be automatically closed in exceptional circumstances, for example if
 * the JVM that is being monitored exits. To receive a notification in any of
 * these occasions, use the {@link #onClose(Runnable)} method to register an
 * action.
 * <p>
 * If an unexpected exception occurs in an action, it is possible to catch the
 * exception in an error handler. An error handler can be registered using the
 * {@link #onError(Consumer)} method. If no error handler is registered, the
 * default behavior is to print the exception and its backtrace to the standard
 * error stream.
 * <p>
 * The following example shows how an {@code EventStream} can be used to listen
 * to events on a JVM running Flight Recorder
 *
 * <pre>{@literal
 * try (var es = EventStream.openRepository()) {
 *   es.onEvent("jdk.CPULoad", event -> {
 *     System.out.println("CPU Load " + event.getEndTime());
 *     System.out.println(" Machine total: " + 100 * event.getFloat("machineTotal") + "%");
 *     System.out.println(" JVM User: " + 100 * event.getFloat("jvmUser") + "%");
 *     System.out.println(" JVM System: " + 100 * event.getFloat("jvmSystem") + "%");
 *     System.out.println();
 *   });
 *   es.onEvent("jdk.GarbageCollection", event -> {
 *     System.out.println("Garbage collection: " + event.getLong("gcId"));
 *     System.out.println(" Cause: " + event.getString("cause"));
 *     System.out.println(" Total pause: " + event.getDuration("sumOfPauses"));
 *     System.out.println(" Longest pause: " + event.getDuration("longestPause"));
 *     System.out.println();
 *   });
 *   es.start();
 * }
 * }</pre>
 * <p>
 * To start recording together with the stream, see {@link RecordingStream}.
 *
 * @since 14
 */
public interface EventStream extends AutoCloseable {
    /**
     * Creates a stream from the repository of the current Java Virtual Machine
     * (JVM).
     * <p>
     * By default, the stream starts with the next event flushed by Flight
     * Recorder.
     *
     * @return an event stream, not {@code null}
     *
     * @throws IOException if a stream can't be opened, or an I/O error occurs
     *         when trying to access the repository
     *
     * @throws SecurityException if a security manager exists and the caller
     *         does not have
     *         {@code FlightRecorderPermission("accessFlightRecorder")}
     */
    @SuppressWarnings("removal")
    public static EventStream openRepository() throws IOException {
        Utils.checkAccessFlightRecorder();
        return new EventDirectoryStream(
            AccessController.getContext(),
            null,
            SecuritySupport.PRIVILEGED,
            null,
            Collections.emptyList(),
            false
        );
    }

    /**
     * Creates an event stream from a disk repository.
     * <p>
     * By default, the stream starts with the next event flushed by Flight
     * Recorder.
     *
     * @param directory location of the disk repository, not {@code null}
     *
     * @return an event stream, not {@code null}
     *
     * @throws IOException if a stream can't be opened, or an I/O error occurs
     *         when trying to access the repository
     *
     * @throws SecurityException if a security manager exists and its
     *         {@code checkRead} method denies read access to the directory, or
     *         files in the directory.
     */
    public static EventStream openRepository(Path directory) throws IOException {
        Objects.requireNonNull(directory);
        @SuppressWarnings("removal")
        AccessControlContext acc = AccessController.getContext();
        return new EventDirectoryStream(
            acc,
            directory,
            FileAccess.UNPRIVILEGED,
            null,
            Collections.emptyList(),
            true
        );
    }

    /**
     * Creates an event stream from a file.
     * <p>
     * By default, the stream starts with the first event in the file.
     *
     * @param file location of the file, not {@code null}
     *
     * @return an event stream, not {@code null}
     *
     * @throws IOException if the file can't be opened, or an I/O error occurs
     *         during reading
     *
     * @throws SecurityException if a security manager exists and its
     *         {@code checkRead} method denies read access to the file
     */
    @SuppressWarnings("removal")
    static EventStream openFile(Path file) throws IOException {
        return new EventFileStream(AccessController.getContext(), file);
    }

    /**
     * Registers an action to perform when new metadata arrives in the stream.
     *
     * The event type of an event always arrives sometime before the actual event.
     * The action must be registered before the stream is started.
     *
     * @implSpec The default implementation of this method is empty.
     *
     * @param action to perform, not {@code null}
     *
     * @throws IllegalStateException if an action is added after the stream has
     *                               started
     */
     default void onMetadata(Consumer<MetadataEvent> action) {
     }

    /**
     * Registers an action to perform on all events in the stream.
     *
     * @param action an action to perform on each {@code RecordedEvent}, not
     *        {@code null}
     */
    void onEvent(Consumer<RecordedEvent> action);

    /**
     * Registers an action to perform on all events matching a name.
     *
     * @param eventName the name of the event, not {@code null}
     *
     * @param action an action to perform on each {@code RecordedEvent} matching
     *        the event name, not {@code null}
     */
    void onEvent(String eventName, Consumer<RecordedEvent> action);

    /**
     * Registers an action to perform after the stream has been flushed.
     *
     * @param action an action to perform after the stream has been
     *        flushed, not {@code null}
     */
    void onFlush(Runnable action);

    /**
     * Registers an action to perform if an exception occurs.
     * <p>
     * If an action is not registered, an exception stack trace is printed to
     * standard error.
     * <p>
     * Registering an action overrides the default behavior. If multiple actions
     * have been registered, they are performed in the order of registration.
     * <p>
     * If this method itself throws an exception, resulting behavior is
     * undefined.
     *
     * @param action an action to perform if an exception occurs, not
     *        {@code null}
     */
    void onError(Consumer<Throwable> action);

    /**
     * Registers an action to perform when the stream is closed.
     * <p>
     * If the stream is already closed, the action will be performed immediately
     * in the current thread.
     *
     * @param action an action to perform after the stream is closed, not
     *        {@code null}
     * @see #close()
     */
    void onClose(Runnable action);

    /**
     * Releases all resources associated with this stream.
     * <p>
     * If a stream is started, asynchronously or synchronously, it is stopped
     * immediately or after the next flush. This method does <em>NOT</em>
     * guarantee that all registered actions are completed before return.
     * <p>
     * Closing a previously closed stream has no effect.
     */
    @Override
    void close();

    /**
     * Unregisters an action.
     * <p>
     * If the action has been registered multiple times, all instances are
     * unregistered.
     *
     * @param action the action to unregister, not {@code null}
     *
     * @return {@code true} if the action was unregistered, {@code false}
     *         otherwise
     *
     * @see #onEvent(Consumer)
     * @see #onEvent(String, Consumer)
     * @see #onFlush(Runnable)
     * @see #onClose(Runnable)
     * @see #onError(Consumer)
     */
    boolean remove(Object action);

    /**
     * Specifies that the event object in an {@link #onEvent(Consumer)} action
     * can be reused.
     * <p>
     * If reuse is set to {@code true}, an action should not keep a reference
     * to the event object after the action has completed.
     *
     * @param reuse {@code true} if an event object can be reused, {@code false}
     * otherwise
     */
    void setReuse(boolean reuse);

    /**
     * Specifies that events arrives in chronological order, sorted by the time
     * they were committed to the stream.
     *
     * @param ordered if event objects arrive in chronological order to
     *        {@link #onEvent(Consumer)}
     */
    void setOrdered(boolean ordered);

    /**
     * Specifies the start time of the stream.
     * <p>
     * The start time must be set before starting the stream
     *
     * @param startTime the start time, not {@code null}
     *
     * @throws IllegalStateException if the stream is already started
     *
     * @see #start()
     * @see #startAsync()
     */
    void setStartTime(Instant startTime);

    /**
     * Specifies the end time of the stream.
     * <p>
     * The end time must be set before starting the stream.
     * <p>
     * At end time, the stream is closed.
     *
     * @param endTime the end time, not {@code null}
     *
     * @throws IllegalStateException if the stream is already started
     *
     * @see #start()
     * @see #startAsync()
     */
    void setEndTime(Instant endTime);

    /**
     * Starts processing of actions.
     * <p>
     * Actions are performed in the current thread.
     * <p>
     * To stop the stream, use the {@link #close()} method.
     *
     * @throws IllegalStateException if the stream is already started or closed
     */
    void start();

    /**
     * Starts asynchronous processing of actions.
     * <p>
     * Actions are performed in a single separate thread.
     * <p>
     * To stop the stream, use the {@link #close()} method.
     *
     * @throws IllegalStateException if the stream is already started or closed
     */
    void startAsync();

    /**
     * Blocks until all actions are completed, or the stream is closed, or the
     * timeout occurs, or the current thread is interrupted, whichever happens
     * first.
     *
     * @param timeout the maximum time to wait, not {@code null}
     *
     * @throws IllegalArgumentException if timeout is negative
     * @throws InterruptedException if interrupted while waiting
     *
     * @see #start()
     * @see #startAsync()
     * @see Thread#interrupt()
     */
    void awaitTermination(Duration timeout) throws InterruptedException;

    /**
     * Blocks until all actions are completed, or the stream is closed, or the
     * current thread is interrupted, whichever happens first.
     *
     * @throws InterruptedException if interrupted while waiting
     *
     * @see #start()
     * @see #startAsync()
     * @see Thread#interrupt()
     */
    void awaitTermination() throws InterruptedException;
}
