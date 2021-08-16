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

import static jdk.jfr.internal.LogLevel.DEBUG;
import static jdk.jfr.internal.LogLevel.INFO;
import static jdk.jfr.internal.LogTag.JFR;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import jdk.jfr.internal.JVM;
import jdk.jfr.internal.JVMSupport;
import jdk.jfr.internal.Logger;
import jdk.jfr.internal.MetadataRepository;
import jdk.jfr.internal.Options;
import jdk.jfr.internal.PlatformRecorder;
import jdk.jfr.internal.PlatformRecording;
import jdk.jfr.internal.Repository;
import jdk.jfr.internal.RequestEngine;
import jdk.jfr.internal.Utils;

/**
 * Class for accessing, controlling, and managing Flight Recorder.
 * <p>
 * This class provides the methods necessary for creating, starting, stopping,
 * and destroying recordings.
 *
 * @since 9
 */
public final class FlightRecorder {
    private static volatile FlightRecorder platformRecorder;
    private static volatile boolean initialized;
    private final PlatformRecorder internal;

    private FlightRecorder(PlatformRecorder internal) {
        this.internal = internal;
    }

    /**
     * Returns an immutable list of the available recordings.
     * <p>
     * A recording becomes available when it is created. It becomes unavailable when it
     * is in the {@code CLOSED} state, typically after a call to
     * {@link Recording#close()}.
     *
     * @return a list of recordings, not {@code null}
     */
    public List<Recording> getRecordings() {
        List<Recording> recs = new ArrayList<>();
        for (PlatformRecording r : internal.getRecordings()) {
            recs.add(r.getRecording());
        }
        return Collections.unmodifiableList(recs);
    }

    /**
     * Creates a snapshot of all available recorded data.
     * <p>
     * A snapshot is a synthesized recording in a {@code STOPPED} state. If no data is
     * available, a recording with size {@code 0} is returned.
     * <p>
     * A snapshot provides stable access to data for later operations (for example,
     * operations to change the interval or to reduce the data size).
     * <p>
     * The following example shows how to create a snapshot and write a subset of the data to a file.
     *
     * <pre>{@literal
     * try (Recording snapshot = FlightRecorder.getFlightRecorder().takeSnapshot()) {
     *   if (snapshot.getSize() > 0) {
     *     snapshot.setMaxSize(100_000_000);
     *     snapshot.setMaxAge(Duration.ofMinutes(5));
     *     snapshot.dump(Paths.get("snapshot.jfr"));
     *   }
     * }
     * }</pre>
     *
     * The caller must close the recording when access to the data is no longer
     * needed.
     *
     * @return a snapshot of all available recording data, not {@code null}
     */
    public Recording takeSnapshot() {
        Recording snapshot = new Recording();
        snapshot.setName("Snapshot");
        internal.fillWithRecordedData(snapshot.getInternal(), null);
        return snapshot;
    }

    /**
     * Registers an event class.
     * <p>
     * If the event class is already registered, then the invocation of this method is
     * ignored.
     *
     * @param eventClass the event class to register, not {@code null}
     *
     * @throws IllegalArgumentException if class is abstract or not a subclass
     *         of {@link Event}
     * @throws SecurityException if a security manager exists and the caller
     *         does not have {@code FlightRecorderPermission("registerEvent")}
     */
    public static void register(Class<? extends Event> eventClass) {
        Objects.requireNonNull(eventClass);
        if (JVMSupport.isNotAvailable()) {
            return;
        }
        Utils.ensureValidEventSubclass(eventClass);
        MetadataRepository.getInstance().register(eventClass);
    }

    /**
     * Unregisters an event class.
     * <p>
     * If the event class is not registered, then the invocation of this method is
     * ignored.
     *
     * @param eventClass the event class to unregistered, not {@code null}
     * @throws IllegalArgumentException if a class is abstract or not a subclass
     *         of {@link Event}
     *
     * @throws SecurityException if a security manager exists and the caller
     *         does not have {@code FlightRecorderPermission("registerEvent")}
     */
    public static void unregister(Class<? extends Event> eventClass) {
        Objects.requireNonNull(eventClass);
        if (JVMSupport.isNotAvailable()) {
            return;
        }
        Utils.ensureValidEventSubclass(eventClass);
        MetadataRepository.getInstance().unregister(eventClass);
    }

    /**
     * Returns the Flight Recorder for the platform.
     *
     * @return a Flight Recorder instance, not {@code null}
     *
     * @throws IllegalStateException if Flight Recorder can't be created (for
     *         example, if the Java Virtual Machine (JVM) lacks Flight Recorder
     *         support, or if the file repository can't be created or accessed)
     *
     * @throws SecurityException if a security manager exists and the caller does
     *         not have {@code FlightRecorderPermission("accessFlightRecorder")}
     */
    public static FlightRecorder getFlightRecorder() throws IllegalStateException, SecurityException {
        synchronized (PlatformRecorder.class) {
            Utils.checkAccessFlightRecorder();
            JVMSupport.ensureWithIllegalStateException();
            if (platformRecorder == null) {
                try {
                    platformRecorder = new FlightRecorder(new PlatformRecorder());
                } catch (IllegalStateException ise) {
                    throw ise;
                } catch (Exception e) {
                    throw new IllegalStateException("Can't create Flight Recorder. " + e.getMessage(), e);
                }
                // Must be in synchronized block to prevent instance leaking out
                // before initialization is done
                initialized = true;
                Logger.log(JFR, INFO, "Flight Recorder initialized");
                if (Logger.shouldLog(JFR, DEBUG)) {
                    Logger.log(JFR, DEBUG, "maxchunksize: " + Options.getMaxChunkSize()+ " bytes");
                    Logger.log(JFR, DEBUG, "memorysize: " + Options.getMemorySize()+ " bytes");
                    Logger.log(JFR, DEBUG, "globalbuffersize: " + Options.getGlobalBufferSize()+ " bytes");
                    Logger.log(JFR, DEBUG, "globalbuffercount: " + Options.getGlobalBufferCount());
                    Logger.log(JFR, DEBUG, "dumppath: " + Options.getDumpPath());
                    Logger.log(JFR, DEBUG, "samplethreads: " + Options.getSampleThreads());
                    Logger.log(JFR, DEBUG, "stackdepth: " + Options.getStackDepth());
                    Logger.log(JFR, DEBUG, "threadbuffersize: " + Options.getThreadBufferSize());
                }
                if (Logger.shouldLog(JFR, INFO)) {
                    Logger.log(JFR, INFO, "Repository base directory: " + Repository.getRepository().getBaseLocation());
                }
                PlatformRecorder.notifyRecorderInitialized(platformRecorder);
            }
        }
        return platformRecorder;
    }

    /**
     * Adds a hook for a periodic event.
     * <p>
     * The implementation of the hook should return as soon as possible, to
     * avoid blocking other Flight Recorder operations. The hook should emit
     * one or more events of the specified type. When a hook is added, the
     * interval at which the call is invoked is configurable using the
     * {@code "period"} setting.
     *
     * @param eventClass the class that the hook should run for, not {@code null}
     * @param hook the hook, not {@code null}
     * @throws IllegalArgumentException if a class is not a subclass of
     *         {@link Event}, is abstract, or the hook is already added
     * @throws IllegalStateException if the event class has the
     *         {@code Registered(false)} annotation and is not registered manually
     * @throws SecurityException if a security manager exists and the caller
     *         does not have {@code FlightRecorderPermission("registerEvent")}
     */
    public static void addPeriodicEvent(Class<? extends Event> eventClass, Runnable hook) throws SecurityException {
        Objects.requireNonNull(eventClass);
        Objects.requireNonNull(hook);
        if (JVMSupport.isNotAvailable()) {
            return;
        }

        Utils.ensureValidEventSubclass(eventClass);
        Utils.checkRegisterPermission();
        @SuppressWarnings("removal")
        AccessControlContext acc = AccessController.getContext();
        RequestEngine.addHook(acc, EventType.getEventType(eventClass).getPlatformEventType(), hook);
    }

    /**
     * Removes a hook for a periodic event.
     *
     * @param hook the hook to remove, not {@code null}
     * @return {@code true} if hook is removed, {@code false} otherwise
     * @throws SecurityException if a security manager exists and the caller
     *         does not have {@code FlightRecorderPermission("registerEvent")}
     */
    public static boolean removePeriodicEvent(Runnable hook) throws SecurityException {
        Objects.requireNonNull(hook);
        Utils.checkRegisterPermission();
        if (JVMSupport.isNotAvailable()) {
            return false;
        }
        return RequestEngine.removeHook(hook);
    }

    /**
     * Returns an immutable list that contains all currently registered events.
     * <p>
     * By default, events are registered when they are first used, typically
     * when an event object is allocated. To ensure an event is visible early,
     * registration can be triggered by invoking the
     * {@link FlightRecorder#register(Class)} method.
     *
     * @return list of events, not {@code null}
     */
    public List<EventType> getEventTypes() {
        return Collections.unmodifiableList(MetadataRepository.getInstance().getRegisteredEventTypes());
    }

    /**
     * Adds a recorder listener and captures the {@code AccessControlContext} to
     * use when invoking the listener.
     * <p>
     * If Flight Recorder is already initialized when the listener is added, then the method
     * {@link FlightRecorderListener#recorderInitialized(FlightRecorder)} method is
     * invoked before returning from this method.
     *
     * @param changeListener the listener to add, not {@code null}
     *
     * @throws SecurityException if a security manager exists and the caller
     *         does not have
     *         {@code FlightRecorderPermission("accessFlightRecorder")}
     */
    public static void addListener(FlightRecorderListener changeListener) {
        Objects.requireNonNull(changeListener);
        Utils.checkAccessFlightRecorder();
        if (JVMSupport.isNotAvailable()) {
            return;
        }
        PlatformRecorder.addListener(changeListener);
    }

    /**
     * Removes a recorder listener.
     * <p>
     * If the same listener is added multiple times, only one instance is
     * removed.
     *
     * @param changeListener listener to remove, not {@code null}
     *
     * @throws SecurityException if a security manager exists and the caller
     *         does not have
     *         {@code FlightRecorderPermission("accessFlightRecorder")}
     *
     * @return {@code true}, if the listener could be removed, {@code false}
     *         otherwise
     */
    public static boolean removeListener(FlightRecorderListener changeListener) {
        Objects.requireNonNull(changeListener);
        Utils.checkAccessFlightRecorder();
        if (JVMSupport.isNotAvailable()) {
            return false;
        }

        return PlatformRecorder.removeListener(changeListener);
    }

    /**
     * Returns {@code true} if the Java Virtual Machine (JVM) has Flight Recorder capabilities.
     * <p>
     * This method can quickly check whether Flight Recorder can be
     * initialized, without actually doing the initialization work. The value may
     * change during runtime and it is not safe to cache it.
     *
     * @return {@code true}, if Flight Recorder is available, {@code false}
     *         otherwise
     *
     * @see FlightRecorderListener for callback when Flight Recorder is
     *      initialized
     */
    public static boolean isAvailable() {
        if (JVMSupport.isNotAvailable()) {
            return false;
        }
        return JVM.getJVM().isAvailable();
    }

    /**
     * Returns {@code true} if Flight Recorder is initialized.
     *
     * @return {@code true}, if Flight Recorder is initialized,
     *         {@code false} otherwise
     *
     * @see FlightRecorderListener for callback when Flight Recorder is
     *      initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }

    PlatformRecorder getInternal() {
        return internal;
    }
}
