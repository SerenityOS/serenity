/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import jdk.jfr.internal.PlatformRecorder;
import jdk.jfr.internal.PlatformRecording;
import jdk.jfr.internal.Type;
import jdk.jfr.internal.Utils;
import jdk.jfr.internal.WriteableUserPath;

/**
 * Provides means to configure, start, stop and dump recording data to disk.
 * <p>
 * The following example shows how configure, start, stop and dump recording data to disk.
 *
 * <pre>{@literal
 *   Configuration c = Configuration.getConfiguration("default");
 *   Recording r = new Recording(c);
 *   r.start();
 *   System.gc();
 *   Thread.sleep(5000);
 *   r.stop();
 *   r.dump(Files.createTempFile("my-recording", ".jfr"));
 * }</pre>
 *
 * @since 9
 */
public final class Recording implements Closeable {

    private static class RecordingSettings extends EventSettings {

        private final Recording recording;
        private final String identifier;

        RecordingSettings(Recording r, String identifier) {
            this.recording = r;
            this.identifier = identifier;
        }

        RecordingSettings(Recording r, Class<? extends Event> eventClass) {
            Utils.ensureValidEventSubclass(eventClass);
            this.recording = r;
            this.identifier = String.valueOf(Type.getTypeId(eventClass));
        }

        @Override
        public EventSettings with(String name, String value) {
            Objects.requireNonNull(value);
            recording.setSetting(identifier + "#" + name, value);
            return this;
        }

        @Override
        public Map<String, String> toMap() {
            return recording.getSettings();
        }
    }

    private final PlatformRecording internal;

    /**
     * Creates a recording with settings from a map of name-value pairs.
     * <p>
     * A newly created recording is in the {@link RecordingState#NEW} state. To start
     * the recording, invoke the {@link Recording#start()} method.
     *
     * @param settings settings as a map of name-value pairs, not {@code null}
     *
     * @throws IllegalStateException if Flight Recorder can't be created (for
     *         example, if the Java Virtual Machine (JVM) lacks Flight Recorder
     *         support, or if the file repository can't be created or accessed)
     *
     * @throws SecurityException If a security manager is used and
     *         FlightRecorderPermission "accessFlightRecorder" is not set.
     *
     * @see jdk.jfr
     */
    public Recording(Map<String, String> settings) {
        Objects.requireNonNull(settings);
        Map<String, String> sanitized = Utils.sanitizeNullFreeStringMap(settings);
        PlatformRecorder r = FlightRecorder.getFlightRecorder().getInternal();
        synchronized (r) {
            this.internal = r.newRecording(sanitized);
            this.internal.setRecording(this);
            if (internal.getRecording() != this) {
                throw new InternalError("Internal recording not properly setup");
            }
        }
    }

    /**
     * Creates a recording without any settings.
     * <p>
     * A newly created recording is in the {@link RecordingState#NEW} state. To start
     * the recording, invoke the {@link Recording#start()} method.
     *
     * @throws IllegalStateException if Flight Recorder can't be created (for
     *         example, if the Java Virtual Machine (JVM) lacks Flight Recorder
     *         support, or if the file repository can't be created or accessed)
     *
     * @throws SecurityException If a security manager is used and
     *         FlightRecorderPermission "accessFlightRecorder" is not set.
     */
    public Recording() {
        this(Map.of());
     }

    /**
     * Creates a recording with settings from a configuration.
     * <p>
     * The following example shows how create a recording that uses a predefined configuration.
     *
     * <pre>{@literal
     * Recording r = new Recording(Configuration.getConfiguration("default"));
     * }</pre>
     *
     * The newly created recording is in the {@link RecordingState#NEW} state. To
     * start the recording, invoke the {@link Recording#start()} method.
     *
     * @param configuration configuration that contains the settings to be use, not
     *        {@code null}
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
    public Recording(Configuration configuration) {
        this(configuration.getSettings());
    }

    /**
     * Starts this recording.
     * <p>
     * It's recommended that the recording options and event settings are configured
     * before calling this method. The benefits of doing so are a more consistent
     * state when analyzing the recorded data, and improved performance because the
     * configuration can be applied atomically.
     * <p>
     * After a successful invocation of this method, this recording is in the
     * {@code RUNNING} state.
     *
     * @throws IllegalStateException if recording is already started or is in the
     *         {@code CLOSED} state
     */
    public void start() {
        internal.start();
    }

    /**
     * Starts this recording after a delay.
     * <p>
     * After a successful invocation of this method, this recording is in the
     * {@code DELAYED} state.
     *
     * @param delay the time to wait before starting this recording, not
     *        {@code null}
     * @throws IllegalStateException if the recording is not it the {@code NEW} state
     */
    public void scheduleStart(Duration delay) {
        Objects.requireNonNull(delay);
        internal.scheduleStart(delay);
    }

    /**
     * Stops this recording.
     * <p>
     * When a recording is stopped it can't be restarted. If this
     * recording has a destination, data is written to that destination and
     * the recording is closed. After a recording is closed, the data is no longer
     * available.
     * <p>
     * After a successful invocation of this method, this recording will be
     * in the {@code STOPPED} state.
     *
     * @return {@code true} if recording is stopped, {@code false} otherwise
     *
     * @throws IllegalStateException if the recording is not started or is already stopped
     *
     * @throws SecurityException if a security manager exists and the caller
     *         doesn't have {@code FilePermission} to write to the destination
     *         path
     *
     * @see #setDestination(Path)
     *
     */
    public boolean stop() {
        return internal.stop("Stopped by user");
    }

    /**
     * Returns settings used by this recording.
     * <p>
     * Modifying the returned {@code Map} will not change the settings for this recording.
     * <p>
     * If no settings are set for this recording, an empty {@code Map} is
     * returned.
     *
     * @return recording settings, not {@code null}
     */
    public Map<String, String> getSettings() {
        return new HashMap<>(internal.getSettings());
    }

    /**
     * Returns the current size of this recording in the disk repository,
     * measured in bytes.
     * <p>
     * The size is updated when recording buffers are flushed. If the recording is
     * not written to the disk repository the returned size is always {@code 0}.
     *
     * @return amount of recorded data, measured in bytes, or {@code 0} if the
     *         recording is not written to the disk repository
     */
    public long getSize() {
        return internal.getSize();
    }

    /**
     * Returns the time when this recording was stopped.
     *
     * @return the time, or {@code null} if this recording is not stopped
     */
    public Instant getStopTime() {
        return internal.getStopTime();
    }

    /**
     * Returns the time when this recording was started.
     *
     * @return the time, or {@code null} if this recording is not started
     */
    public Instant getStartTime() {
        return internal.getStartTime();
    }

    /**
     * Returns the maximum size, measured in bytes, at which data is no longer kept in the disk repository.
     *
     * @return maximum size in bytes, or {@code 0} if no maximum size is set
     */
    public long getMaxSize() {
        return internal.getMaxSize();
    }

    /**
     * Returns the length of time that the data is kept in the disk repository
     * before it is removed.
     *
     * @return maximum length of time, or {@code null} if no maximum length of time
     *         has been set
     */
    public Duration getMaxAge() {
        return internal.getMaxAge();
    }

    /**
     * Returns the name of this recording.
     * <p>
     * By default, the name is the same as the recording ID.
     *
     * @return the recording name, not {@code null}
     */
    public String getName() {
        return internal.getName();
    }

    /**
     * Replaces all settings for this recording.
     * <p>
     * The following example shows how to set event settings for a recording.
     *
     * <pre>{@literal
     *     Map<String, String> settings = new HashMap<>();
     *     settings.putAll(EventSettings.enabled("jdk.CPUSample").withPeriod(Duration.ofSeconds(2)).toMap());
     *     settings.putAll(EventSettings.enabled(MyEvent.class).withThreshold(Duration.ofSeconds(2)).withoutStackTrace().toMap());
     *     settings.put("jdk.ExecutionSample#period", "10 ms");
     *     recording.setSettings(settings);
     * }</pre>
     *
     * The following example shows how to merge settings.
     *
     * <pre>{@literal
     *     Map<String, String> settings = recording.getSettings();
     *     settings.putAll(additionalSettings);
     *     recording.setSettings(settings);
     * }</pre>
     *
     * @param settings the settings to set, not {@code null}
     */
    public void setSettings(Map<String, String> settings) {
        Objects.requireNonNull(settings);
        Map<String, String> sanitized = Utils.sanitizeNullFreeStringMap(settings);
        internal.setSettings(sanitized);
    }

    /**
     * Returns the recording state that this recording is currently in.
     *
     * @return the recording state, not {@code null}
     *
     * @see RecordingState
     */
    public RecordingState getState() {
        return internal.getState();
    }

    /**
     * Releases all data that is associated with this recording.
     * <p>
     * After a successful invocation of this method, this recording is in the
     * {@code CLOSED} state.
     */
    @Override
    public void close() {
        internal.close();
    }

    /**
     * Returns a clone of this recording, with a new recording ID and name.
     *
     * Clones are useful for dumping data without stopping the recording. After
     * a clone is created, the amount of data to copy is constrained
     * with the {@link #setMaxAge(Duration)} method and the {@link #setMaxSize(long)}method.
     *
     * @param stop {@code true} if the newly created copy should be stopped
     *        immediately, {@code false} otherwise
     * @return the recording copy, not {@code null}
     */
    public Recording copy(boolean stop) {
        return internal.newCopy(stop);
    }

    /**
     * Writes recording data to a file.
     * <p>
     * Recording must be started, but not necessarily stopped.
     *
     * @param destination the location where recording data is written, not
     *        {@code null}
     *
     * @throws IOException if the recording can't be copied to the specified
     *         location
     *
     * @throws SecurityException if a security manager exists and the caller doesn't
     *         have {@code FilePermission} to write to the destination path
     */
    public void dump(Path destination) throws IOException {
        Objects.requireNonNull(destination);
        internal.dump(new WriteableUserPath(destination));

    }

    /**
     * Returns {@code true} if this recording uses the disk repository, {@code false} otherwise.
     * <p>
     * If no value is set, {@code true} is returned.
     *
     * @return {@code true} if the recording uses the disk repository, {@code false}
     *         otherwise
     */
    public boolean isToDisk() {
        return internal.isToDisk();
    }

    /**
     * Determines how much data is kept in the disk repository.
     * <p>
     * To control the amount of recording data that is stored on disk, the maximum
     * amount of data to retain can be specified. When the maximum limit is
     * exceeded, the Java Virtual Machine (JVM) removes the oldest chunk to make
     * room for a more recent chunk.
     * <p>
     * If neither maximum limit or the maximum age is set, the size of the
     * recording may grow indefinitely.
     *
     * @param maxSize the amount of data to retain, {@code 0} if infinite
     *
     * @throws IllegalArgumentException if {@code maxSize} is negative
     *
     * @throws IllegalStateException if the recording is in {@code CLOSED} state
     */
    public void setMaxSize(long maxSize) {
        if (maxSize < 0) {
            throw new IllegalArgumentException("Max size of recording can't be negative");
        }
        internal.setMaxSize(maxSize);
    }

        /**
         * Determines how often events are made available for streaming.
         *
         * @param interval the interval at which events are made available for streaming.
         *
         * @throws IllegalArgumentException if {@code interval} is negative
         *
         * @throws IllegalStateException if the recording is in the {@code CLOSED} state
         *
         * @since 14
         */
        /*package private*/ void setFlushInterval(Duration interval) {
            Objects.requireNonNull(interval);
            if (interval.isNegative()) {
                throw new IllegalArgumentException("Stream interval can't be negative");
            }
            internal.setFlushInterval(interval);
        }

    /**
     * Returns how often events are made available for streaming purposes.
     *
     * @return the flush interval, or {@code null} if no interval has been set
     *
     * @since 14
     */
    /*package private*/ Duration getFlushInterval() {
        return internal.getFlushInterval();
    }

    /**
     * Determines how far back data is kept in the disk repository.
     * <p>
     * To control the amount of recording data stored on disk, the maximum length of
     * time to retain the data can be specified. Data stored on disk that is older
     * than the specified length of time is removed by the Java Virtual Machine (JVM).
     * <p>
     * If neither maximum limit or the maximum age is set, the size of the
     * recording may grow indefinitely.
     *
     * @param maxAge the length of time that data is kept, or {@code null} if infinite
     *
     * @throws IllegalArgumentException if {@code maxAge} is negative
     *
     * @throws IllegalStateException if the recording is in the {@code CLOSED} state
     */
    public void setMaxAge(Duration maxAge) {
        if (maxAge != null && maxAge.isNegative()) {
            throw new IllegalArgumentException("Max age of recording can't be negative");
        }
        internal.setMaxAge(maxAge);
    }

    /**
     * Sets a location where data is written on recording stop, or
     * {@code null} if data is not to be dumped.
     * <p>
     * If a destination is set, this recording is automatically closed
     * after data is successfully copied to the destination path.
     * <p>
     * If a destination is <em>not</em> set, Flight Recorder retains the
     * recording data until this recording is closed. Use the {@link #dump(Path)} method to
     * manually write data to a file.
     *
     * @param destination the destination path, or {@code null} if recording should
     *        not be dumped at stop
     *
     * @throws IllegalStateException if recording is in the {@code STOPPED} or
     *         {@code CLOSED} state.
     *
     * @throws SecurityException if a security manager exists and the caller
     *         doesn't have {@code FilePermission} to read, write, and delete the
     *         {@code destination} file
     *
     * @throws IOException if the path is not writable
     */
    public void setDestination(Path destination) throws IOException {
        internal.setDestination(destination != null ? new WriteableUserPath(destination) : null);
    }

    /**
     * Returns the destination file, where recording data is written when the
     * recording stops, or {@code null} if no destination is set.
     *
     * @return the destination file, or {@code null} if not set.
     */
    public Path getDestination() {
        WriteableUserPath usp = internal.getDestination();
        if (usp == null) {
            return null;
        } else {
            return usp.getPotentiallyMaliciousOriginal();
        }
    }

    /**
     * Returns a unique ID for this recording.
     *
     * @return the recording ID
     */
    public long getId() {
        return internal.getId();
    }

    /**
     * Sets a human-readable name (for example, {@code "My Recording"}).
     *
     * @param name the recording name, not {@code null}
     *
     * @throws IllegalStateException if the recording is in {@code CLOSED} state
     */
    public void setName(String name) {
        Objects.requireNonNull(name);
        internal.setName(name);
    }

    /**
     * Sets whether this recording is dumped to disk when the JVM exits.
     *
     * @param dumpOnExit if this recording should be dumped when the JVM exits
     */
    public void setDumpOnExit(boolean dumpOnExit) {
        internal.setDumpOnExit(dumpOnExit);
    }

    /**
     * Returns whether this recording is dumped to disk when the JVM exits.
     * <p>
     * If dump on exit is not set, {@code false} is returned.
     *
     * @return {@code true} if the recording is dumped on exit, {@code false}
     *         otherwise.
     */
    public boolean getDumpOnExit() {
        return internal.getDumpOnExit();
    }

    /**
     * Determines whether this recording is continuously flushed to the disk
     * repository or data is constrained to what is available in memory buffers.
     *
     * @param disk {@code true} if this recording is written to disk,
     *        {@code false} if in-memory
     *
     */
    public void setToDisk(boolean disk) {
        internal.setToDisk(disk);
    }

    /**
     * Creates a data stream for a specified interval.
     * <p>
     * The stream may contain some data outside the specified range.
     * <p>
     * If the recording is not to disk, a stream can't be created
     * and {@code null} is returned.
     *
     * @param start the start time for the stream, or {@code null} to get data from
     *        start time of the recording
     *
     * @param end the end time for the stream, or {@code null} to get data until the
     *        present time.
     *
     * @return an input stream, or {@code null} if no data is available in the
     *         interval, or the recording was not recorded to disk
     *
     * @throws IllegalArgumentException if {@code end} happens before
     *         {@code start}
     *
     * @throws IOException if a stream can't be opened
     *
     * @see #setToDisk(boolean)
     */
    public InputStream getStream(Instant start, Instant end) throws IOException {
        if (start != null && end != null && end.isBefore(start)) {
            throw new IllegalArgumentException("End time of requested stream must not be before start time");
        }
        return internal.open(start, end);
    }

    /**
     * Returns the specified duration for this recording, or {@code null} if no
     * duration is set.
     * <p>
     * The duration can be set only when the recording is in the
     * {@link RecordingState#NEW} state.
     *
     * @return the desired duration of the recording, or {@code null} if no duration
     *         has been set.
     */
    public Duration getDuration() {
        return internal.getDuration();
    }

    /**
     * Sets a duration for how long a recording runs before it stops.
     * <p>
     * By default, a recording has no duration ({@code null}).
     *
     * @param duration the duration, or {@code null} if no duration is set
     *
     * @throws IllegalStateException if recording is in the {@code STOPPED} or {@code CLOSED} state
     */
    public void setDuration(Duration duration) {
        internal.setDuration(duration);
    }

    /**
     * Enables the event with the specified name.
     * <p>
     * If multiple events have the same name (for example, the same class is loaded
     * in different class loaders), then all events that match the name are enabled. To
     * enable a specific class, use the {@link #enable(Class)} method or a {@code String}
     * representation of the event type ID.
     *
     * @param name the settings for the event, not {@code null}
     *
     * @return an event setting for further configuration, not {@code null}
     *
     * @see EventType
     */
    public EventSettings enable(String name) {
        Objects.requireNonNull(name);
        RecordingSettings rs = new RecordingSettings(this, name);
        rs.with("enabled", "true");
        return rs;
    }

    /**
     * Disables event with the specified name.
     * <p>
     * If multiple events with same name (for example, the same class is loaded
     * in different class loaders), then all events that match the
     * name is disabled. To disable a specific class, use the
     * {@link #disable(Class)} method or a {@code String} representation of the event
     * type ID.
     *
     * @param name the settings for the event, not {@code null}
     *
     * @return an event setting for further configuration, not {@code null}
     *
     */
    public EventSettings disable(String name) {
        Objects.requireNonNull(name);
        RecordingSettings rs = new RecordingSettings(this, name);
        rs.with("enabled", "false");
        return rs;
    }

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
        Objects.requireNonNull(eventClass);
        RecordingSettings rs = new RecordingSettings(this, eventClass);
        rs.with("enabled", "true");
        return rs;
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
        Objects.requireNonNull(eventClass);
        RecordingSettings rs = new RecordingSettings(this, eventClass);
        rs.with("enabled", "false");
        return rs;
    }

    // package private
    PlatformRecording getInternal() {
        return internal;
    }

    private void setSetting(String id, String value) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(value);
        internal.setSetting(id, value);
    }

}
