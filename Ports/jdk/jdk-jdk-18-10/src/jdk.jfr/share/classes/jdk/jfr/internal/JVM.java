/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;

import jdk.internal.vm.annotation.IntrinsicCandidate;
import jdk.jfr.Event;
import jdk.jfr.internal.handlers.EventHandler;

/**
 * Interface against the JVM.
 *
 */
public final class JVM {
    private static final JVM jvm = new JVM();

    // JVM signals file changes by doing Object#notify on this object
    static final Object FILE_DELTA_CHANGE = new Object();

    static final long RESERVED_CLASS_ID_LIMIT = 500;

    private volatile boolean nativeOK;

    private static native void registerNatives();

    static {
        registerNatives();
        for (LogTag tag : LogTag.values()) {
            subscribeLogLevel(tag, tag.id);
        }
        Options.ensureInitialized();
    }

    /**
     * Get the one and only JVM.
     *
     * @return the JVM
     */
    public static JVM getJVM() {
        return jvm;
    }

    private JVM() {
    }

    /**
     * Marks current chunk as final
     * <p>
     * This allows streaming clients to read the chunk header and
     * close the stream when no more data will be written into
     * the current repository.
     */
    public native void markChunkFinal();

    /**
     * Begin recording events
     *
     * Requires that JFR has been started with {@link #createNativeJFR()}
     */
    public native void beginRecording();

    /**
     * Return true if the JVM is recording
     */
    public native boolean isRecording();

    /**
     * End recording events, which includes flushing data in thread buffers
     *
     * Requires that JFR has been started with {@link #createNativeJFR()}
     *
     */
    public native void endRecording();

    /**
     * Return ticks
     *
     * @return the time, in ticks
     *
     */
    @IntrinsicCandidate
    public static native long counterTime();

    /**
     * Emits native periodic event.
     *
     * @param eventTypeId type id
     *
     * @param timestamp commit time for event
     * @param when when it is being done {@link Periodic.When}
     *
     * @return true if the event was committed
     */
    public native boolean emitEvent(long eventTypeId, long timestamp, long when);

    /**
     * Return a list of all classes deriving from {@link jdk.internal.event.Event}
     *
     * @return list of event classes.
     */
    public native List<Class<? extends jdk.internal.event.Event>> getAllEventClasses();

    /**
     * Return a count of the number of unloaded classes deriving from {@link Event}
     *
     * @return number of unloaded event classes.
     */
    public native long getUnloadedEventClassCount();

    /**
     * Return a unique identifier for a class. The class is marked as being
     * "in use" in JFR.
     *
     * @param clazz clazz
     *
     * @return a unique class identifier
     */
    @IntrinsicCandidate
    public static native long getClassId(Class<?> clazz);

    /**
     * Return process identifier.
     *
     * @return process identifier
     */
    public native String getPid();

    /**
     * Return unique identifier for stack trace.
     *
     * Requires that JFR has been started with {@link #createNativeJFR()}
     *
     * @param skipCount number of frames to skip
     * @return a unique stack trace identifier
     */
    public native long getStackTraceId(int skipCount);

    /**
     * Return identifier for thread
     *
     * @param t thread
     * @return a unique thread identifier
     */
    public native long getThreadId(Thread t);

    /**
     * Frequency, ticks per second
     *
     * @return frequency
     */
    public native long getTicksFrequency();

    /**
     * Write message to log. Should swallow null or empty message, and be able
     * to handle any Java character and not crash with very large message
     *
     * @param tagSetId the tagset id
     * @param level on level
     * @param message log message
     *
     */
    public static native void log(int tagSetId, int level, String message);

    /**
     * Log an event to jfr+event or jfr+event+system.
     * <p>
     * Caller should ensure that message is not null or too large to handle.
     *
     * @param level log level
     * @param lines lines to log
     * @param system if lines should be written to jfr+event+system
     */
    public static native void logEvent(int level, String[] lines, boolean system);

    /**
     * Subscribe to LogLevel updates for LogTag
     *
     * @param lt the log tag to subscribe
     * @param tagSetId the tagset id
     */
    public static native void subscribeLogLevel(LogTag lt, int tagSetId);

    /**
     * Call to invoke event tagging and retransformation of the passed classes
     *
     * @param classes
     *
     * @throws IllegalStateException if wrong JVMTI phase.
     */
    public native synchronized void retransformClasses(Class<?>[] classes);

    /**
     * Enable event
     *
     * @param eventTypeId event type id
     *
     * @param enabled enable event
     */
    public native void setEnabled(long eventTypeId, boolean enabled);

    /**
     * Interval at which the JVM should notify on {@link #FILE_DELTA_CHANGE}
     *
     * @param delta number of bytes, reset after file rotation
     */
    public native void setFileNotification(long delta);

    /**
     * Set the number of global buffers to use
     *
     * @param count
     *
     * @throws IllegalArgumentException if count is not within a valid range
     * @throws IllegalStateException if value can't be changed
     */
    public native void setGlobalBufferCount(long count) throws IllegalArgumentException, IllegalStateException;

    /**
     * Set size of a global buffer
     *
     * @param size
     *
     * @throws IllegalArgumentException if buffer size is not within a valid
     *         range
     */
    public native void setGlobalBufferSize(long size) throws IllegalArgumentException;

    /**
     * Set overall memory size
     *
     * @param size
     *
     * @throws IllegalArgumentException if memory size is not within a valid
     *         range
     */
    public native void setMemorySize(long size) throws IllegalArgumentException;

    /**
     * Set interval for method samples, in milliseconds.
     *
     * Setting interval to 0 turns off the method sampler.
     *
     * @param intervalMillis the sampling interval
     */
    public native void setMethodSamplingInterval(long type, long intervalMillis);

    /**
     * Sets the file where data should be written.
     *
     * Requires that JFR has been started with {@link #createNativeJFR()}
     *
     * <pre>
     * Recording  Previous  Current  Action
     * ==============================================
     *    true     null      null     Ignore, keep recording in-memory
     *    true     null      file1    Start disk recording
     *    true     file      null     Copy out metadata to disk and continue in-memory recording
     *    true     file1     file2    Copy out metadata and start with new File (file2)
     *    false     *        null     Ignore, but start recording to memory with {@link #beginRecording()}
     *    false     *        file     Ignore, but start recording to disk with {@link #beginRecording()}
     *
     * </pre>
     *
     * recording can be set to true/false with {@link #beginRecording()}
     * {@link #endRecording()}
     *
     * @param file the file where data should be written, or null if it should
     *        not be copied out (in memory).
     */
    public native void setOutput(String file);

    /**
     * Controls if a class deriving from jdk.jfr.Event should
     * always be instrumented on class load.
     *
     * @param force, true to force initialization, false otherwise
     */
    public native void setForceInstrumentation(boolean force);

    /**
     * Turn on/off thread sampling.
     *
     * @param sampleThreads true if threads should be sampled, false otherwise.
     *
     * @throws IllegalStateException if state can't be changed.
     */
    public native void setSampleThreads(boolean sampleThreads) throws IllegalStateException;

    /**
     * Turn on/off compressed integers.
     *
     * @param compressed true if compressed integers should be used, false
     *        otherwise.
     *
     * @throws IllegalStateException if state can't be changed.
     */
    public native void setCompressedIntegers(boolean compressed) throws IllegalStateException;

    /**
     * Set stack depth.
     *
     * @param depth
     *
     * @throws IllegalArgumentException if not within a valid range
     * @throws IllegalStateException if depth can't be changed
     */
    public native void setStackDepth(int depth) throws IllegalArgumentException, IllegalStateException;

    /**
     * Turn on stack trace for an event
     *
     * @param eventTypeId the event id
     *
     * @param enabled if stack traces should be enabled
     */
    public native void setStackTraceEnabled(long eventTypeId, boolean enabled);

    /**
     * Set thread buffer size.
     *
     * @param size
     *
     * @throws IllegalArgumentException if size is not within a valid range
     * @throws IllegalStateException if size can't be changed
     */
    public native void setThreadBufferSize(long size) throws IllegalArgumentException, IllegalStateException;

    /**
     * Set threshold for event,
     *
     * Long.MAXIMUM_VALUE = no limit
     *
     * @param eventTypeId the id of the event type
     * @param ticks threshold in ticks,
     * @return true, if it could be set
     */
    public native boolean setThreshold(long eventTypeId, long ticks);

    /**
     * Store the metadata descriptor that is to be written at the end of a
     * chunk, data should be written after GMT offset and size of metadata event
     * should be adjusted
     *
     * Requires that JFR has been started with {@link #createNativeJFR()}
     *
     * @param bytes binary representation of metadata descriptor
     */
    public native void storeMetadataDescriptor(byte[] bytes);

    /**
     * If the JVM supports JVM TI and retransformation has not been disabled this
     * method will return true. This flag can not change during the lifetime of
     * the JVM.
     *
     * @return if transform is allowed
     */
    public native boolean getAllowedToDoEventRetransforms();

    /**
     * Set up native resources, data structures, threads etc. for JFR
     *
     * @param simulateFailure simulate a initialization failure and rollback in
     *        native, used for testing purposes
     *
     * @throws IllegalStateException if native part of JFR could not be created.
     *
     */
    private native boolean createJFR(boolean simulateFailure) throws IllegalStateException;

    /**
     * Destroys native part of JFR. If already destroy, call is ignored.
     *
     * Requires that JFR has been started with {@link #createNativeJFR()}
     *
     * @return if an instance was actually destroyed.
     *
     */
    private native boolean destroyJFR();

    public boolean createFailedNativeJFR() throws IllegalStateException {
        return createJFR(true);
    }

    public void createNativeJFR() {
        nativeOK = createJFR(false);
    }

    public boolean destroyNativeJFR() {
        boolean result = destroyJFR();
        nativeOK = !result;
        return result;
    }

    public boolean hasNativeJFR() {
        return nativeOK;
    }

    /**
     * Cheap test to check if JFR functionality is available.
     *
     * @return
     */
    public native boolean isAvailable();

    /**
     * To convert ticks to wall clock time.
     */
    public native double getTimeConversionFactor();

    /**
     * Return a unique identifier for a class. Compared to {@link #getClassId(Class)},
     * this method does not tag the class as being "in-use".
     *
     * @param clazz class
     *
     * @return a unique class identifier
     */
    public native long getTypeId(Class<?> clazz);

    /**
     * Fast path fetching the EventWriter using VM intrinsics
     *
     * @return thread local EventWriter
     */
    @IntrinsicCandidate
    public static native Object getEventWriter();

    /**
     * Create a new EventWriter
     *
     * @return thread local EventWriter
     */
    public static native EventWriter newEventWriter();

    /**
     * Flushes the EventWriter for this thread.
     */
    public static native boolean flush(EventWriter writer, int uncommittedSize, int requestedSize);

    /**
     * Flushes all thread buffers to disk and the constant pool data needed to read
     * them.
     * <p>
     * When the method returns, the chunk header should be updated with valid
     * pointers to the metadata event, last check point event, correct file size and
     * the generation id.
     *
     */
    public native void flush();

    /**
     * Sets the location of the disk repository, to be used at an emergency
     * dump.
     *
     * @param dirText
     */
    public native void setRepositoryLocation(String dirText);

   /**
    * Access to VM termination support.
    *
    * @param errorMsg descriptive message to be include in VM termination sequence
    */
    public native void abort(String errorMsg);

    /**
     * Adds a string to the string constant pool.
     *
     * If the same string is added twice, two entries will be created.
     *
     * @param id identifier associated with the string, not negative
     *
     * @param s string constant to be added, not null
     *
     * @return true, if the string was successfully added.
     */
    public static native boolean addStringConstant(long id, String s);

    public native void uncaughtException(Thread thread, Throwable t);

    /**
     * Sets cutoff for event.
     *
     * Determines how long the event should be allowed to run.
     *
     * Long.MAXIMUM_VALUE = no limit
     *
     * @param eventTypeId the id of the event type
     * @param cutoffTicks cutoff in ticks,
     * @return true, if it could be set
     */
    public native boolean setCutoff(long eventTypeId, long cutoffTicks);

    /**
     * Sets the event emission rate in event sample size per time unit.
     *
     * Determines how events are throttled.
     *
     * @param eventTypeId the id of the event type
     * @param eventSampleSize event sample size
     * @param period_ms time period in milliseconds
     * @return true, if it could be set
     */
    public native boolean setThrottle(long eventTypeId, long eventSampleSize, long period_ms);

    /**
     * Emit old object sample events.
     *
     * @param cutoff the cutoff in ticks
     * @param emitAll emit all samples in old object queue
     * @param skipBFS don't use BFS when searching for path to GC root
     */
    public native void emitOldObjectSamples(long cutoff, boolean emitAll, boolean skipBFS);

    /**
     * Test if a chunk rotation is warranted.
     *
     * @return if it is time to perform a chunk rotation
     */
    public native boolean shouldRotateDisk();

    /**
     * Exclude a thread from the jfr system
     *
     */
    public native void exclude(Thread thread);

    /**
     * Include a thread back into the jfr system
     *
     */
    public native void include(Thread thread);

    /**
     * Test if a thread ius currently excluded from the jfr system.
     *
     * @return is thread currently excluded
     */
    public native boolean isExcluded(Thread thread);

    /**
     * Get the start time in nanos from the header of the current chunk
     *
     * @return start time of the recording in nanos, -1 in case of in-memory
     */
    public native long getChunkStartNanos();

    /**
     * Stores an EventHandler to the eventHandler field of an event class.
     *
     * @param eventClass the class, not {@code null}
     *
     * @param handler the handler, may be {@code null}
     *
     * @return if the field could be set
     */
    public native boolean setHandler(Class<? extends jdk.internal.event.Event> eventClass, EventHandler handler);

    /**
     * Retrieves the EventHandler for an event class.
     *
     * @param eventClass the class, not {@code null}
     *
     * @return the handler, may be {@code null}
     */
    public native Object getHandler(Class<? extends jdk.internal.event.Event> eventClass);

    /**
     * Returns the id for the Java types defined in metadata.xml.
     *
     * @param name the name of the type
     *
     * @return the id, or a negative value if it does not exists.
     */
    public native long getTypeId(String name);
}
