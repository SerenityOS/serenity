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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import jdk.jfr.Configuration;
import jdk.jfr.Event;
import jdk.jfr.EventSettings;
import jdk.jfr.EventType;
import jdk.jfr.Recording;
import jdk.jfr.RecordingState;
import jdk.jfr.internal.PlatformRecording;
import jdk.jfr.internal.PrivateAccess;
import jdk.jfr.internal.SecuritySupport;
import jdk.jfr.internal.Utils;
import jdk.jfr.internal.consumer.EventDirectoryStream;

/**
 * A recording stream produces events from the current JVM (Java Virtual
 * Machine).
 * <p>
 * The following example shows how to record events using the default
 * configuration and print the Garbage Collection, CPU Load and JVM Information
 * event to standard out.
 * <pre>{@literal
 * Configuration c = Configuration.getConfiguration("default");
 * try (var rs = new RecordingStream(c)) {
 *     rs.onEvent("jdk.GarbageCollection", System.out::println);
 *     rs.onEvent("jdk.CPULoad", System.out::println);
 *     rs.onEvent("jdk.JVMInformation", System.out::println);
 *     rs.start();
 * }
 * }</pre>
 *
 * @since 14
 */
public final class RecordingStream implements AutoCloseable, EventStream {

    static final class ChunkConsumer implements Consumer<Long> {

        private final Recording recording;

        ChunkConsumer(Recording recording) {
            this.recording = recording;
        }

        @Override
        public void accept(Long endNanos) {
            Instant t = Utils.epochNanosToInstant(endNanos);
            PlatformRecording p = PrivateAccess.getInstance().getPlatformRecording(recording);
            p.removeBefore(t);
        }
    }

    private final Recording recording;
    private final Instant creationTime;
    private final EventDirectoryStream directoryStream;
    private long maxSize;
    private Duration maxAge;

    /**
     * Creates an event stream for the current JVM (Java Virtual Machine).
     *
     * @throws IllegalStateException if Flight Recorder can't be created (for
     *         example, if the Java Virtual Machine (JVM) lacks Flight Recorder
     *         support, or if the file repository can't be created or accessed)
     *
     * @throws SecurityException if a security manager exists and the caller
     *         does not have
     *         {@code FlightRecorderPermission("accessFlightRecorder")}
     */
    public RecordingStream() {
        Utils.checkAccessFlightRecorder();
        @SuppressWarnings("removal")
        AccessControlContext acc = AccessController.getContext();
        this.recording = new Recording();
        this.creationTime = Instant.now();
        this.recording.setName("Recording Stream: " + creationTime);
        try {
            PlatformRecording pr = PrivateAccess.getInstance().getPlatformRecording(recording);
            this.directoryStream = new EventDirectoryStream(
                acc,
                null,
                SecuritySupport.PRIVILEGED,
                pr,
                configurations(),
                false
            );
        } catch (IOException ioe) {
            this.recording.close();
            throw new IllegalStateException(ioe.getMessage());
        }
    }

    private List<Configuration> configurations() {
        try {
            return Configuration.getConfigurations();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * Creates a recording stream using settings from a configuration.
     * <p>
     * The following example shows how to create a recording stream that uses a
     * predefined configuration.
     *
     * <pre>{@literal
     * var c = Configuration.getConfiguration("default");
     * try (var rs = new RecordingStream(c)) {
     *   rs.onEvent(System.out::println);
     *   rs.start();
     * }
     * }</pre>
     *
     * @param configuration configuration that contains the settings to use,
     *        not {@code null}
     *
     * @throws IllegalStateException if Flight Recorder can't be created (for
     *         example, if the Java Virtual Machine (JVM) lacks Flight Recorder
     *         support, or if the file repository can't be created or accessed)
     *
     * @throws SecurityException if a security manager is used and
     *         FlightRecorderPermission "accessFlightRecorder" is not set.
     *
     * @see Configuration
     */
    public RecordingStream(Configuration configuration) {
        this();
        recording.setSettings(configuration.getSettings());
    }

    /**
     * Enables the event with the specified name.
     * <p>
     * If multiple events have the same name (for example, the same class is
     * loaded in different class loaders), then all events that match the name
     * are enabled. To enable a specific class, use the {@link #enable(Class)}
     * method or a {@code String} representation of the event type ID.
     *
     * @param name the settings for the event, not {@code null}
     *
     * @return an event setting for further configuration, not {@code null}
     *
     * @see EventType
     */
    public EventSettings enable(String name) {
        return recording.enable(name);
    }

    /**
     * Replaces all settings for this recording stream.
     * <p>
     * The following example records 20 seconds using the "default" configuration
     * and then changes settings to the "profile" configuration.
     *
     * <pre>{@literal
     * Configuration defaultConfiguration = Configuration.getConfiguration("default");
     * Configuration profileConfiguration = Configuration.getConfiguration("profile");
     * try (var rs = new RecordingStream(defaultConfiguration)) {
     *    rs.onEvent(System.out::println);
     *    rs.startAsync();
     *    Thread.sleep(20_000);
     *    rs.setSettings(profileConfiguration.getSettings());
     *    Thread.sleep(20_000);
     * }
     * }</pre>
     *
     * @param settings the settings to set, not {@code null}
     *
     * @see Recording#setSettings(Map)
     */
    public void setSettings(Map<String, String> settings) {
        recording.setSettings(settings);
    };

    /**
     * Enables event.
     *
     * @param eventClass the event to enable, not {@code null}
     *
     * @throws IllegalArgumentException if {@code eventClass} is an abstract
     *         class or not a subclass of {@link Event}
     *
     * @return an event setting for further configuration, not {@code null}
     */
    public EventSettings enable(Class<? extends Event> eventClass) {
        return recording.enable(eventClass);
    }

    /**
     * Disables event with the specified name.
     * <p>
     * If multiple events with same name (for example, the same class is loaded
     * in different class loaders), then all events that match the name are
     * disabled. To disable a specific class, use the {@link #disable(Class)}
     * method or a {@code String} representation of the event type ID.
     *
     * @param name the settings for the event, not {@code null}
     *
     * @return an event setting for further configuration, not {@code null}
     *
     */
    public EventSettings disable(String name) {
        return recording.disable(name);
    }

    /**
     * Disables event.
     *
     * @param eventClass the event to enable, not {@code null}
     *
     * @throws IllegalArgumentException if {@code eventClass} is an abstract
     *         class or not a subclass of {@link Event}
     *
     * @return an event setting for further configuration, not {@code null}
     *
     */
    public EventSettings disable(Class<? extends Event> eventClass) {
        return recording.disable(eventClass);
    }

    /**
     * Determines how far back data is kept for the stream.
     * <p>
     * To control the amount of recording data stored on disk, the maximum
     * length of time to retain the data can be specified. Data stored on disk
     * that is older than the specified length of time is removed by the Java
     * Virtual Machine (JVM).
     * <p>
     * If neither maximum limit or the maximum age is set, the size of the
     * recording may grow indefinitely if events are on
     *
     * @param maxAge the length of time that data is kept, or {@code null} if
     *        infinite
     *
     * @throws IllegalArgumentException if {@code maxAge} is negative
     *
     * @throws IllegalStateException if the recording is in the {@code CLOSED}
     *         state
     */
    public void setMaxAge(Duration maxAge) {
        synchronized (directoryStream) {
            recording.setMaxAge(maxAge);
            this.maxAge = maxAge;
            updateOnCompleteHandler();
        }
    }

    /**
     * Determines how much data is kept for the stream.
     * <p>
     * To control the amount of recording data that is stored on disk, the
     * maximum amount of data to retain can be specified. When the maximum limit
     * is exceeded, the Java Virtual Machine (JVM) removes the oldest chunk to
     * make room for a more recent chunk.
     * <p>
     * If neither maximum limit or the maximum age is set, the size of the
     * recording may grow indefinitely.
     * <p>
     * The size is measured in bytes.
     *
     * @param maxSize the amount of data to retain, {@code 0} if infinite
     *
     * @throws IllegalArgumentException if {@code maxSize} is negative
     *
     * @throws IllegalStateException if the recording is in {@code CLOSED} state
     */
    public void setMaxSize(long maxSize) {
        synchronized (directoryStream) {
            recording.setMaxSize(maxSize);
            this.maxSize = maxSize;
            updateOnCompleteHandler();
        }
    }

    @Override
    public void setReuse(boolean reuse) {
        directoryStream.setReuse(reuse);
    }

    @Override
    public void setOrdered(boolean ordered) {
        directoryStream.setOrdered(ordered);
    }

    @Override
    public void setStartTime(Instant startTime) {
        directoryStream.setStartTime(startTime);
    }

    @Override
    public void setEndTime(Instant endTime) {
        directoryStream.setEndTime(endTime);
    }

    @Override
    public void onEvent(String eventName, Consumer<RecordedEvent> action) {
        directoryStream.onEvent(eventName, action);
    }

    @Override
    public void onEvent(Consumer<RecordedEvent> action) {
        directoryStream.onEvent(action);
    }

    @Override
    public void onFlush(Runnable action) {
        directoryStream.onFlush(action);
    }

    @Override
    public void onClose(Runnable action) {
        directoryStream.onClose(action);
    }

    @Override
    public void onError(Consumer<Throwable> action) {
        directoryStream.onError(action);
    }

    @Override
    public void close() {
        directoryStream.setChunkCompleteHandler(null);
        recording.close();
        directoryStream.close();
    }

    @Override
    public boolean remove(Object action) {
        return directoryStream.remove(action);
    }

    @Override
    public void start() {
        PlatformRecording pr = PrivateAccess.getInstance().getPlatformRecording(recording);
        long startNanos = pr.start();
        updateOnCompleteHandler();
        directoryStream.start(startNanos);
    }

    /**
     * Starts asynchronous processing of actions.
     * <p>
     * Actions are performed in a single separate thread.
     * <p>
     * To stop the stream, use the {@link #close()} method.
     * <p>
     * The following example prints the CPU usage for ten seconds. When
     * the current thread leaves the try-with-resources block the
     * stream is stopped/closed.
     * <pre>{@literal
     * try (var stream = new RecordingStream()) {
     *   stream.enable("jdk.CPULoad").withPeriod(Duration.ofSeconds(1));
     *   stream.onEvent("jdk.CPULoad", event -> {
     *     System.out.println(event);
     *   });
     *   stream.startAsync();
     *   Thread.sleep(10_000);
     * }
     * }</pre>
     *
     * @throws IllegalStateException if the stream is already started or closed
     */
    @Override
    public void startAsync() {
        PlatformRecording pr = PrivateAccess.getInstance().getPlatformRecording(recording);
        long startNanos = pr.start();
        updateOnCompleteHandler();
        directoryStream.startAsync(startNanos);
    }

    /**
     * Writes recording data to a file.
     * <p>
     * The recording stream must be started, but not closed.
     * <p>
     * It's highly recommended that a max age or max size is set before
     * starting the stream. Otherwise, the dump may not contain any events.
     *
     * @param destination the location where recording data is written, not
     *        {@code null}
     *
     * @throws IOException if the recording data can't be copied to the specified
     *         location, or if the stream is closed, or not started.
     *
     * @throws SecurityException if a security manager exists and the caller doesn't
     *         have {@code FilePermission} to write to the destination path
     *
     * @see RecordingStream#setMaxAge(Duration)
     * @see RecordingStream#setMaxSize(long)
     *
     * @since 17
     */
    public void dump(Path destination) throws IOException {
        Objects.requireNonNull(destination);
        Object recorder = PrivateAccess.getInstance().getPlatformRecorder();
        synchronized (recorder) {
            RecordingState state = recording.getState();
            if (state == RecordingState.CLOSED) {
                throw new IOException("Recording stream has been closed, no content to write");
            }
            if (state == RecordingState.NEW) {
                throw new IOException("Recording stream has not been started, no content to write");
            }
            recording.dump(destination);
        }
    }

    @Override
    public void awaitTermination(Duration timeout) throws InterruptedException {
        directoryStream.awaitTermination(timeout);
    }

    @Override
    public void awaitTermination() throws InterruptedException {
        directoryStream.awaitTermination();
    }

    @Override
    public void onMetadata(Consumer<MetadataEvent> action) {
        directoryStream.onMetadata(action);
    }

    private void updateOnCompleteHandler() {
        if (maxAge != null || maxSize != 0) {
            // User has set a chunk removal policy
            directoryStream.setChunkCompleteHandler(null);
        } else {
            directoryStream.setChunkCompleteHandler(new ChunkConsumer(recording));
        }
    }
}
