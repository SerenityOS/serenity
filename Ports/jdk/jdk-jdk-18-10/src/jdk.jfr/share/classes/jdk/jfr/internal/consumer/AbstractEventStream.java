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

package jdk.jfr.internal.consumer;

import java.io.IOException;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import jdk.jfr.Configuration;
import jdk.jfr.EventType;
import jdk.jfr.consumer.EventStream;
import jdk.jfr.consumer.MetadataEvent;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.internal.LogLevel;
import jdk.jfr.internal.LogTag;
import jdk.jfr.internal.Logger;
import jdk.jfr.internal.PlatformRecording;
import jdk.jfr.internal.SecuritySupport;

/*
 * Purpose of this class is to simplify the implementation of
 * an event stream.
 */
public abstract class AbstractEventStream implements EventStream {
    private static final AtomicLong counter = new AtomicLong();

    private final Object terminated = new Object();
    private final Runnable flushOperation = () -> dispatcher().runFlushActions();
    @SuppressWarnings("removal")
    private final AccessControlContext accessControllerContext;
    private final StreamConfiguration streamConfiguration = new StreamConfiguration();
    protected final PlatformRecording recording;
    private final List<Configuration> configurations;

    private volatile Thread thread;
    private Dispatcher dispatcher;

    private volatile boolean closed;

    private boolean daemon = false;

    AbstractEventStream(@SuppressWarnings("removal") AccessControlContext acc, PlatformRecording recording, List<Configuration> configurations) throws IOException {
        this.accessControllerContext = Objects.requireNonNull(acc);
        this.recording = recording;
        this.configurations = configurations;
    }

    @Override
    public abstract void start();

    @Override
    public abstract void startAsync();

    @Override
    public abstract void close();

    protected final Dispatcher dispatcher() {
        if (streamConfiguration.hasChanged()) { // quick check
            synchronized (streamConfiguration) {
                dispatcher = new Dispatcher(streamConfiguration);
                streamConfiguration.setChanged(false);
            }
        }
        return dispatcher;
    }

    @Override
    public final void setOrdered(boolean ordered) {
        streamConfiguration.setOrdered(ordered);
    }

    @Override
    public final void setReuse(boolean reuse) {
        streamConfiguration.setReuse(reuse);
    }

    // Only used if -Xlog:jfr+event* is specified
    public final void setDaemon(boolean daemon) {
        this.daemon = daemon;
    }

    @Override
    public final void setStartTime(Instant startTime) {
        Objects.requireNonNull(startTime);
        synchronized (streamConfiguration) {
            if (streamConfiguration.started) {
                throw new IllegalStateException("Stream is already started");
            }
            if (startTime.isBefore(Instant.EPOCH)) {
                startTime = Instant.EPOCH;
            }
            streamConfiguration.setStartTime(startTime);
        }
    }

    @Override
    public final void setEndTime(Instant endTime) {
        Objects.requireNonNull(endTime);
        synchronized (streamConfiguration) {
            if (streamConfiguration.started) {
                throw new IllegalStateException("Stream is already started");
            }
            streamConfiguration.setEndTime(endTime);
        }
    }

    @Override
    public final void onEvent(Consumer<RecordedEvent> action) {
        Objects.requireNonNull(action);
        streamConfiguration.addEventAction(action);
    }

    @Override
    public final void onEvent(String eventName, Consumer<RecordedEvent> action) {
        Objects.requireNonNull(eventName);
        Objects.requireNonNull(action);
        streamConfiguration.addEventAction(eventName, action);
    }

    @Override
    public final void onFlush(Runnable action) {
        Objects.requireNonNull(action);
        streamConfiguration.addFlushAction(action);
    }

    @Override
    public final void onClose(Runnable action) {
        Objects.requireNonNull(action);
        streamConfiguration.addCloseAction(action);
    }

    @Override
    public final void onError(Consumer<Throwable> action) {
        Objects.requireNonNull(action);
        streamConfiguration.addErrorAction(action);
    }

    @Override
    public final boolean remove(Object action) {
        Objects.requireNonNull(action);
        return streamConfiguration.remove(action);
    }

    @Override
    public final void awaitTermination() throws InterruptedException {
        awaitTermination(Duration.ofMillis(0));
    }

    @Override
    public final void awaitTermination(Duration timeout) throws InterruptedException {
        Objects.requireNonNull(timeout);
        if (timeout.isNegative()) {
            throw new IllegalArgumentException("timeout value is negative");
        }

        long base = System.currentTimeMillis();
        long now = 0;

        long millis;
        try {
            millis = Math.multiplyExact(timeout.getSeconds(), 1000);
        } catch (ArithmeticException a) {
            millis = Long.MAX_VALUE;
        }
        int nanos = timeout.toNanosPart();
        if (nanos == 0 && millis == 0) {
            synchronized (terminated) {
                while (!isClosed()) {
                    terminated.wait(0);
                }
            }
        } else {
            while (!isClosed()) {
                long delay = millis - now;
                if (delay <= 0) {
                    break;
                }
                synchronized (terminated) {
                    terminated.wait(delay, nanos);
                }
                now = System.currentTimeMillis() - base;
            }
        }
    }

    protected abstract void process() throws IOException;

    protected final void setClosed(boolean closed) {
        this.closed = closed;
    }

    protected final boolean isClosed() {
        return closed;
    }

    public final void startAsync(long startNanos) {
        startInternal(startNanos);
        Runnable r = () -> run(accessControllerContext);
        thread = SecuritySupport.createThreadWitNoPermissions(nextThreadName(), r);
        SecuritySupport.setDaemonThread(thread, daemon);
        thread.start();
    }

    public final void start(long startNanos) {
        startInternal(startNanos);
        thread = Thread.currentThread();
        run(accessControllerContext);
    }

    protected final Runnable getFlushOperation() {
        return flushOperation;
    }


    protected final void onFlush() {
       Runnable r = getFlushOperation();
       if (r != null) {
           r.run();
       }
    }

    private void startInternal(long startNanos) {
        synchronized (streamConfiguration) {
            if (streamConfiguration.started) {
                throw new IllegalStateException("Event stream can only be started once");
            }
            if (recording != null && streamConfiguration.startTime == null) {
                streamConfiguration.setStartNanos(startNanos);
            }
            streamConfiguration.setStarted(true);
        }
    }

    private void execute() {
        try {
            process();
        } catch (IOException ioe) {
            // This can happen if a chunk file is removed, or
            // a file is access that has been closed
            // This is "normal" behavior for streaming and the
            // stream will be closed when this happens.
        } finally {
            Logger.log(LogTag.JFR_SYSTEM_STREAMING, LogLevel.DEBUG, "Execution of stream ended.");
            try {
                close();
            } finally {
                synchronized (terminated) {
                    terminated.notifyAll();
                }
            }
        }
    }

    @SuppressWarnings("removal")
    private void run(AccessControlContext accessControlContext) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                execute();
                return null;
            }
        }, accessControlContext);
    }

    private String nextThreadName() {
        return "JFR Event Stream " + counter.incrementAndGet();
    }

    @Override
    public void onMetadata(Consumer<MetadataEvent> action) {
        Objects.requireNonNull(action);
        synchronized (streamConfiguration) {
            if (streamConfiguration.started) {
                throw new IllegalStateException("Stream is already started");
            }
        }
        streamConfiguration.addMetadataAction(action);
    }

    protected final void onMetadata(ChunkParser parser) {
        if (parser.hasStaleMetadata()) {
            if (dispatcher.hasMetadataHandler()) {
                List<EventType> ce = parser.getEventTypes();
                List<EventType> pe = parser.getPreviousEventTypes();
                if (ce != pe) {
                    MetadataEvent me = JdkJfrConsumer.instance().newMetadataEvent(pe, ce, configurations);
                    dispatcher.runMetadataActions(me);
                }
                parser.setStaleMetadata(false);
            }
        }
    }
}
