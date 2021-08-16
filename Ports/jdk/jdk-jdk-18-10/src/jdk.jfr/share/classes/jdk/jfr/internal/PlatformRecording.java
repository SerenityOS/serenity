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

package jdk.jfr.internal;

import static jdk.jfr.internal.LogLevel.DEBUG;
import static jdk.jfr.internal.LogLevel.WARN;
import static jdk.jfr.internal.LogTag.JFR;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TimerTask;
import java.util.TreeMap;

import jdk.jfr.Configuration;
import jdk.jfr.FlightRecorderListener;
import jdk.jfr.Recording;
import jdk.jfr.RecordingState;
import jdk.jfr.internal.SecuritySupport.SafePath;

public final class PlatformRecording implements AutoCloseable {

    private final PlatformRecorder recorder;
    private final long id;
    // Recording settings
    private Map<String, String> settings = new LinkedHashMap<>();
    private Duration duration;
    private Duration maxAge;
    private long maxSize;

    private WriteableUserPath destination;

    private boolean toDisk = true;
    private String name;
    private boolean dumpOnExit;
    private SafePath dumpOnExitDirectory = new SafePath(".");
    // Timestamp information
    private Instant stopTime;
    private Instant startTime;

    // Misc, information
    private RecordingState state = RecordingState.NEW;
    private long size;
    private final LinkedList<RepositoryChunk> chunks = new LinkedList<>();
    private volatile Recording recording;
    private TimerTask stopTask;
    private TimerTask startTask;
    @SuppressWarnings("removal")
    private AccessControlContext noDestinationDumpOnExitAccessControlContext;
    private boolean shouldWriteActiveRecordingEvent = true;
    private Duration flushInterval = Duration.ofSeconds(1);
    private long finalStartChunkNanos = Long.MIN_VALUE;
    private long startNanos = -1;

    @SuppressWarnings("removal")
    PlatformRecording(PlatformRecorder recorder, long id) {
        // Typically the access control context is taken
        // when you call dump(Path) or setDestination(Path),
        // but if no destination is set and dumpOnExit=true
        // the control context of the recording is taken when the
        // Recording object is constructed. This works well for
        // -XX:StartFlightRecording and JFR.dump
        this.noDestinationDumpOnExitAccessControlContext = AccessController.getContext();
        this.id = id;
        this.recorder = recorder;
        this.name = String.valueOf(id);
    }

    public long start() {
        RecordingState oldState;
        RecordingState newState;
        synchronized (recorder) {
            oldState = getState();
            if (!Utils.isBefore(state, RecordingState.RUNNING)) {
                throw new IllegalStateException("Recording can only be started once.");
            }
            if (startTask != null) {
                startTask.cancel();
                startTask = null;
                startTime = null;
            }
            startNanos = recorder.start(this);
            if (Logger.shouldLog(LogTag.JFR, LogLevel.INFO)) {
                // Only print non-default values so it easy to see
                // which options were added
                StringJoiner options = new StringJoiner(", ");
                if (!toDisk) {
                    options.add("disk=false");
                }
                if (maxAge != null) {
                    options.add("maxage=" + Utils.formatTimespan(maxAge, ""));
                }
                if (maxSize != 0) {
                    options.add("maxsize=" + Utils.formatBytesCompact(maxSize));
                }
                if (dumpOnExit) {
                    options.add("dumponexit=true");
                }
                if (duration != null) {
                    options.add("duration=" + Utils.formatTimespan(duration, ""));
                }
                if (destination != null) {
                    options.add("filename=" + destination.getRealPathText());
                }
                String optionText = options.toString();
                if (optionText.length() != 0) {
                    optionText = "{" + optionText + "}";
                }
                Logger.log(LogTag.JFR, LogLevel.INFO,
                        "Started recording \"" + getName() + "\" (" + getId() + ") " + optionText);
            };
            newState = getState();
        }
        notifyIfStateChanged(oldState, newState);

        return startNanos;
    }

    public boolean stop(String reason) {
        RecordingState oldState;
        RecordingState newState;
        synchronized (recorder) {
            oldState = getState();
            if (stopTask != null) {
                stopTask.cancel();
                stopTask = null;
            }
            recorder.stop(this);
            String endText = reason == null ? "" : ". Reason \"" + reason + "\".";
            Logger.log(LogTag.JFR, LogLevel.INFO, "Stopped recording \"" + getName() + "\" (" + getId() + ")" + endText);
            newState = getState();
        }
        WriteableUserPath dest = getDestination();

        if (dest != null) {
            try {
                dumpStopped(dest);
                Logger.log(LogTag.JFR, LogLevel.INFO, "Wrote recording \"" + getName() + "\" (" + getId() + ") to " + dest.getRealPathText());
                notifyIfStateChanged(newState, oldState);
                close(); // remove if copied out
            } catch(IOException e) {
                Logger.log(LogTag.JFR, LogLevel.ERROR,
                           "Unable to complete I/O operation when dumping recording \"" + getName() + "\" (" + getId() + ")");
            }
        } else {
            notifyIfStateChanged(newState, oldState);
        }
        return true;
    }

    public void scheduleStart(Duration delay) {
        synchronized (recorder) {
            ensureOkForSchedule();

            startTime = Instant.now().plus(delay);
            LocalDateTime now = LocalDateTime.now().plus(delay);
            setState(RecordingState.DELAYED);
            startTask = createStartTask();
            try {
                recorder.getTimer().schedule(startTask, delay.toMillis());
                Logger.log(LogTag.JFR, LogLevel.INFO, "Scheduled recording \"" + getName() + "\" (" + getId() + ") to start at " + now);
            } catch (IllegalStateException ise) {
                // This can happen in the unlikely case that a recording
                // is scheduled after the Timer has been cancelled in
                // the shutdown hook. Just ignore.
            }
        }
    }

    private void ensureOkForSchedule() {
        if (getState() != RecordingState.NEW) {
            throw new IllegalStateException("Only a new recording can be scheduled for start");
        }
    }

    private TimerTask createStartTask() {
        // Taking ref. to recording here.
        // Opens up for memory leaks.
        return new TimerTask() {
            @Override
            public void run() {
                synchronized (recorder) {
                    if (getState() != RecordingState.DELAYED) {
                        return;
                    }
                    start();
                }
            }
        };
    }

    void scheduleStart(Instant startTime) {
        synchronized (recorder) {
            ensureOkForSchedule();
            this.startTime = startTime;
            setState(RecordingState.DELAYED);
            startTask = createStartTask();
            recorder.getTimer().schedule(startTask, startTime.toEpochMilli());
        }
    }

    public Map<String, String> getSettings() {
        synchronized (recorder) {
            return settings;
        }
    }

    public long getSize() {
        return size;
    }

    public Instant getStopTime() {
        synchronized (recorder) {
            return stopTime;
        }
    }

    public Instant getStartTime() {
        synchronized (recorder) {
            return startTime;
        }
    }

    public Long getMaxSize() {
        synchronized (recorder) {
            return maxSize;
        }
    }

    public Duration getMaxAge() {
        synchronized (recorder) {
            return maxAge;
        }
    }

    public String getName() {
        synchronized (recorder) {
            return name;
        }
    }

    public RecordingState getState() {
        synchronized (recorder) {
            return state;
        }
    }

    @Override
    public void close() {
        RecordingState oldState;
        RecordingState newState;

        synchronized (recorder) {
            oldState = getState();
            if (RecordingState.CLOSED != getState()) {
                if (startTask != null) {
                    startTask.cancel();
                    startTask = null;
                }
                recorder.finish(this);
                for (RepositoryChunk c : chunks) {
                    removed(c);
                }
                chunks.clear();
                setState(RecordingState.CLOSED);
                Logger.log(LogTag.JFR, LogLevel.INFO, "Closed recording \"" + getName() + "\" (" + getId() + ")");
            }
            newState = getState();
        }
        notifyIfStateChanged(newState, oldState);
    }

    // To be used internally when doing dumps.
    // Caller must have recorder lock and close recording before releasing lock
    public PlatformRecording newSnapshotClone(String reason, Boolean pathToGcRoots) throws IOException {
        if(!Thread.holdsLock(recorder)) {
            throw new InternalError("Caller must have recorder lock");
        }
        RecordingState state = getState();
        if (state == RecordingState.CLOSED) {
            throw new IOException("Recording \"" + name + "\" (id=" + id + ") has been closed, no content to write");
        }
        if (state == RecordingState.DELAYED || state == RecordingState.NEW) {
            throw new IOException("Recording \"" + name + "\" (id=" + id + ") has not started, no content to write");
        }
        if (state == RecordingState.STOPPED) {
            PlatformRecording clone = recorder.newTemporaryRecording();
            for (RepositoryChunk r : chunks) {
                clone.add(r);
            }
            return clone;
        }

        // Recording is RUNNING, create a clone
        PlatformRecording clone = recorder.newTemporaryRecording();
        clone.setShouldWriteActiveRecordingEvent(false);
        clone.setName(getName());
        clone.setToDisk(true);
        clone.setMaxAge(getMaxAge());
        clone.setMaxSize(getMaxSize());
        // We purposely don't clone settings here, since
        // a union a == a
        if (!isToDisk()) {
            // force memory contents to disk
            clone.start();
        } else {
            // using existing chunks on disk
            for (RepositoryChunk c : chunks) {
                clone.add(c);
            }
            clone.setState(RecordingState.RUNNING);
            clone.setStartTime(getStartTime());
        }
        if (pathToGcRoots == null) {
            clone.setSettings(getSettings()); // needed for old object sample
            clone.stop(reason); // dumps to destination path here
        } else {
            // Risk of violating lock order here, since
            // clone.stop() will take recorder lock inside
            // metadata lock, but OK if we already
            // have recorder lock when we entered metadata lock
            synchronized (MetadataRepository.getInstance()) {
                clone.setSettings(OldObjectSample.createSettingsForSnapshot(this, pathToGcRoots));
                clone.stop(reason);
            }
        }
        return clone;
    }

    public boolean isToDisk() {
        synchronized (recorder) {
            return toDisk;
        }
    }

    public void setMaxSize(long maxSize) {
        synchronized (recorder) {
            if (getState() == RecordingState.CLOSED) {
                throw new IllegalStateException("Can't set max size when recording is closed");
            }
            this.maxSize = maxSize;
            trimToSize();
        }
    }

    public void setDestination(WriteableUserPath userSuppliedPath) throws IOException {
        synchronized (recorder) {
            checkSetDestination(userSuppliedPath);
            this.destination = userSuppliedPath;
        }
    }

    public void checkSetDestination(WriteableUserPath userSuppliedPath) throws IOException {
        synchronized (recorder) {
            if (Utils.isState(getState(), RecordingState.STOPPED, RecordingState.CLOSED)) {
                throw new IllegalStateException("Destination can't be set on a recording that has been stopped/closed");
            }
        }
    }

    public WriteableUserPath getDestination() {
        synchronized (recorder) {
            return destination;
        }
    }

    void setState(RecordingState state) {
        synchronized (recorder) {
            this.state = state;
        }
    }

    void setStartTime(Instant startTime) {
        synchronized (recorder) {
            this.startTime = startTime;
        }
    }

    void setStopTime(Instant timeStamp) {
        synchronized (recorder) {
            stopTime = timeStamp;
        }
    }

    public long getId() {
        synchronized (recorder) {
            return id;
        }
    }

    public void setName(String name) {
        synchronized (recorder) {
            ensureNotClosed();
            this.name = name;
        }
    }

    private void ensureNotClosed() {
        if (getState() == RecordingState.CLOSED) {
            throw new IllegalStateException("Can't change name on a closed recording");
        }
    }

    public void setDumpOnExit(boolean dumpOnExit) {
        synchronized (recorder) {
            this.dumpOnExit = dumpOnExit;
        }
    }

    public boolean getDumpOnExit() {
        synchronized (recorder) {
            return dumpOnExit;
        }
    }

    public void setToDisk(boolean toDisk) {
        synchronized (recorder) {
            if (Utils.isState(getState(), RecordingState.NEW, RecordingState.DELAYED)) {
                this.toDisk = toDisk;
            } else {
                throw new IllegalStateException("Recording option disk can't be changed after recording has started");
            }
        }
    }

    public void setSetting(String id, String value) {
        synchronized (recorder) {
            this.settings.put(id, value);
            if (getState() == RecordingState.RUNNING) {
                recorder.updateSettings();
            }
        }
    }

    public void setSettings(Map<String, String> settings) {
        setSettings(settings, true);
    }

    private void setSettings(Map<String, String> settings, boolean update) {
        if (Logger.shouldLog(LogTag.JFR_SETTING, LogLevel.INFO) && update) {
            TreeMap<String, String> ordered = new TreeMap<>(settings);
            Logger.log(LogTag.JFR_SETTING, LogLevel.INFO, "New settings for recording \"" + getName() + "\" (" + getId() + ")");
            for (Map.Entry<String, String> entry : ordered.entrySet()) {
                String text = entry.getKey() + "=\"" + entry.getValue() + "\"";
                Logger.log(LogTag.JFR_SETTING, LogLevel.INFO, text);
            }
        }
        synchronized (recorder) {
            this.settings = new LinkedHashMap<>(settings);
            if (getState() == RecordingState.RUNNING && update) {
                recorder.updateSettings();
            }
        }
    }

    private void notifyIfStateChanged(RecordingState newState, RecordingState oldState) {
        if (oldState == newState) {
            return;
        }
        for (FlightRecorderListener cl : PlatformRecorder.getListeners()) {
            try {
                // Skip internal recordings
                if (recording != null) {
                    cl.recordingStateChanged(recording);
                }
            } catch (RuntimeException re) {
                Logger.log(JFR, WARN, "Error notifying recorder listener:" + re.getMessage());
            }
        }
    }

    public void setRecording(Recording recording) {
        this.recording = recording;
    }

    public Recording getRecording() {
        return recording;
    }

    @Override
    public String toString() {
        return getName() + " (id=" + getId() + ") " + getState();
    }

    public void setConfiguration(Configuration c) {
        setSettings(c.getSettings());
    }

    public void setMaxAge(Duration maxAge) {
        synchronized (recorder) {
            if (getState() == RecordingState.CLOSED) {
                throw new IllegalStateException("Can't set max age when recording is closed");
            }
            this.maxAge = maxAge;
            if (maxAge != null) {
                trimToAge(Instant.now().minus(maxAge));
            }
        }
    }

    void appendChunk(RepositoryChunk chunk) {
        if (!chunk.isFinished()) {
            throw new Error("not finished chunk " + chunk.getStartTime());
        }
        synchronized (recorder) {
            if (!toDisk) {
                return;
            }
            if (maxAge != null) {
                trimToAge(chunk.getEndTime().minus(maxAge));
            }
            chunks.addLast(chunk);
            added(chunk);
            trimToSize();
        }
    }

    private void trimToSize() {
        if (maxSize == 0) {
            return;
        }
        while (size > maxSize && chunks.size() > 1) {
            RepositoryChunk c = chunks.removeFirst();
            removed(c);
        }
    }

    private void trimToAge(Instant oldest) {
        while (!chunks.isEmpty()) {
            RepositoryChunk oldestChunk = chunks.peek();
            if (oldestChunk.getEndTime().isAfter(oldest)) {
                return;
            }
            chunks.removeFirst();
            removed(oldestChunk);
        }
    }

    void add(RepositoryChunk c) {
        chunks.add(c);
        added(c);
    }

    private void added(RepositoryChunk c) {
        c.use();
        size += c.getSize();
        if (Logger.shouldLog(JFR, DEBUG)) {
            Logger.log(JFR, DEBUG, "Recording \"" + name + "\" (" + id + ") added chunk " + c.toString() + ", current size=" + size);
        }
    }

    private void removed(RepositoryChunk c) {
        size -= c.getSize();
        if (Logger.shouldLog(JFR, DEBUG)) {
            Logger.log(JFR, DEBUG, "Recording \"" + name + "\" (" + id + ") removed chunk " + c.toString() + ", current size=" + size);
        }
        c.release();
    }

    public List<RepositoryChunk> getChunks() {
        return chunks;
    }

    public InputStream open(Instant start, Instant end) throws IOException {
        synchronized (recorder) {
            if (getState() != RecordingState.STOPPED) {
                throw new IOException("Recording must be stopped before it can be read.");
            }
            List<RepositoryChunk> chunksToUse = new ArrayList<RepositoryChunk>();
            for (RepositoryChunk chunk : chunks) {
                if (chunk.isFinished()) {
                    Instant chunkStart = chunk.getStartTime();
                    Instant chunkEnd = chunk.getEndTime();
                    if (start == null || !chunkEnd.isBefore(start)) {
                        if (end == null || !chunkStart.isAfter(end)) {
                            chunksToUse.add(chunk);
                        }
                    }
                }
            }
            if (chunksToUse.isEmpty()) {
                return null;
            }
            return new ChunkInputStream(chunksToUse);
        }
    }

    public Duration getDuration() {
        synchronized (recorder) {
            return duration;
        }
    }

    void setInternalDuration(Duration duration) {
        this.duration = duration;
    }

    public void setDuration(Duration duration) {
        synchronized (recorder) {
            if (Utils.isState(getState(), RecordingState.STOPPED, RecordingState.CLOSED)) {
                throw new IllegalStateException("Duration can't be set after a recording has been stopped/closed");
            }
            setInternalDuration(duration);
            if (getState() != RecordingState.NEW) {
                updateTimer();
            }
        }
    }

    void updateTimer() {
        if (stopTask != null) {
            stopTask.cancel();
            stopTask = null;
        }
        if (getState() == RecordingState.CLOSED) {
            return;
        }
        if (duration != null) {
            stopTask = createStopTask();
            recorder.getTimer().schedule(stopTask, new Date(startTime.plus(duration).toEpochMilli()));
        }
    }

    TimerTask createStopTask() {
        return new TimerTask() {
            @Override
            public void run() {
                try {
                    stop("End of duration reached");
                } catch (Throwable t) {
                    // Prevent malicious user to propagate exception callback in the wrong context
                    Logger.log(LogTag.JFR, LogLevel.ERROR, "Could not stop recording.");
                }
            }
        };
    }

    public Recording newCopy(boolean stop) {
        return recorder.newCopy(this, stop);
    }

    void setStopTask(TimerTask stopTask) {
        synchronized (recorder) {
            this.stopTask = stopTask;
        }
    }

    void clearDestination() {
        destination = null;
    }

    @SuppressWarnings("removal")
    public AccessControlContext getNoDestinationDumpOnExitAccessControlContext() {
        return noDestinationDumpOnExitAccessControlContext;
    }

    void setShouldWriteActiveRecordingEvent(boolean shouldWrite) {
        this.shouldWriteActiveRecordingEvent = shouldWrite;
    }

    boolean shouldWriteMetadataEvent() {
        return shouldWriteActiveRecordingEvent;
    }

    // Dump running and stopped recordings
    public void dump(WriteableUserPath writeableUserPath) throws IOException {
        synchronized (recorder) {
            try(PlatformRecording p = newSnapshotClone("Dumped by user", null))  {
                p.dumpStopped(writeableUserPath);
            }
        }
    }

    public void dumpStopped(WriteableUserPath userPath) throws IOException {
        synchronized (recorder) {
                userPath.doPrivilegedIO(() -> {
                    try (ChunksChannel cc = new ChunksChannel(chunks); FileChannel fc = FileChannel.open(userPath.getReal(), StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
                        cc.transferTo(fc);
                        fc.force(true);
                    }
                    return null;
                });
        }
    }

    public void filter(Instant begin, Instant end, Long maxSize) {
        synchronized (recorder) {
            List<RepositoryChunk> result = removeAfter(end, removeBefore(begin, new ArrayList<>(chunks)));
            if (maxSize != null) {
                if (begin != null && end == null) {
                    result = reduceFromBeginning(maxSize, result);
                } else {
                    result = reduceFromEnd(maxSize, result);
                }
            }
            int size = 0;
            for (RepositoryChunk r : result) {
                size += r.getSize();
                r.use();
            }
            this.size = size;
            for (RepositoryChunk r : chunks) {
                r.release();
            }
            chunks.clear();
            chunks.addAll(result);
        }
    }

    private static List<RepositoryChunk> removeBefore(Instant time, List<RepositoryChunk> input) {
        if (time == null) {
            return input;
        }
        List<RepositoryChunk> result = new ArrayList<>(input.size());
        for (RepositoryChunk r : input) {
            if (!r.getEndTime().isBefore(time)) {
                result.add(r);
            }
        }
        return result;
    }

    private static List<RepositoryChunk> removeAfter(Instant time, List<RepositoryChunk> input) {
        if (time == null) {
            return input;
        }
        List<RepositoryChunk> result = new ArrayList<>(input.size());
        for (RepositoryChunk r : input) {
            if (!r.getStartTime().isAfter(time)) {
                result.add(r);
            }
        }
        return result;
    }

    private static List<RepositoryChunk> reduceFromBeginning(Long maxSize, List<RepositoryChunk> input) {
        if (maxSize == null || input.isEmpty()) {
            return input;
        }
        List<RepositoryChunk> result = new ArrayList<>(input.size());
        long total = 0;
        for (RepositoryChunk r : input) {
            total += r.getSize();
            if (total > maxSize) {
                break;
            }
            result.add(r);
        }
        // always keep at least one chunk
        if (result.isEmpty()) {
            result.add(input.get(0));
        }
        return result;
    }

    private static List<RepositoryChunk> reduceFromEnd(Long maxSize, List<RepositoryChunk> input) {
        Collections.reverse(input);
        List<RepositoryChunk> result = reduceFromBeginning(maxSize, input);
        Collections.reverse(result);
        return result;
    }

    public void setDumpOnExitDirectory(SafePath directory) {
       this.dumpOnExitDirectory = directory;
    }

    public SafePath getDumpOnExitDirectory()  {
        return this.dumpOnExitDirectory;
    }

    public void setFlushInterval(Duration interval) {
        synchronized (recorder) {
            if (getState() == RecordingState.CLOSED) {
                throw new IllegalStateException("Can't set stream interval when recording is closed");
            }
            this.flushInterval = interval;
        }
    }

    public Duration getFlushInterval() {
        synchronized (recorder) {
            return flushInterval;
        }
    }

    public long getStreamIntervalMillis() {
        synchronized (recorder) {
            if (flushInterval != null) {
                return flushInterval.toMillis();
            }
            return Long.MAX_VALUE;
        }
    }

    public long getStartNanos() {
        return startNanos;
    }

    public long getFinalChunkStartNanos() {
        return finalStartChunkNanos;
    }

    public void setFinalStartnanos(long chunkStartNanos) {
       this.finalStartChunkNanos = chunkStartNanos;
    }

    public void removeBefore(Instant timestamp) {
        synchronized (recorder) {
            while (!chunks.isEmpty()) {
                RepositoryChunk oldestChunk = chunks.peek();
                if (!oldestChunk.getEndTime().isBefore(timestamp)) {
                    return;
                }
                chunks.removeFirst();
                removed(oldestChunk);
            }
        }

    }

    public void removePath(SafePath path) {
        synchronized (recorder) {
            Iterator<RepositoryChunk> it = chunks.iterator();
            while (it.hasNext()) {
                RepositoryChunk c = it.next();
                if (c.getFile().equals(path)) {
                    it.remove();
                    removed(c);
                    return;
                }
            }
        }
    }
}
