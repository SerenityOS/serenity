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
import java.nio.file.Path;
import java.security.AccessControlContext;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import jdk.jfr.Configuration;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.internal.JVM;
import jdk.jfr.internal.PlatformRecording;
import jdk.jfr.internal.SecuritySupport;
import jdk.jfr.internal.Utils;
import jdk.jfr.internal.consumer.ChunkParser.ParserConfiguration;

/**
 * Implementation of an {@code EventStream}} that operates against a directory
 * with chunk files.
 *
 */
public class EventDirectoryStream extends AbstractEventStream {

    private static final Comparator<? super RecordedEvent> EVENT_COMPARATOR = JdkJfrConsumer.instance().eventComparator();

    private final RepositoryFiles repositoryFiles;
    private final FileAccess fileAccess;

    private ChunkParser currentParser;
    private long currentChunkStartNanos;
    private RecordedEvent[] sortedCache;
    private int threadExclusionLevel = 0;
    protected volatile long maxSize;
    protected volatile Duration maxAge;

    private volatile Consumer<Long> onCompleteHandler;

    public EventDirectoryStream(
            @SuppressWarnings("removal")
            AccessControlContext acc,
            Path p,
            FileAccess fileAccess,
            PlatformRecording recording,
            List<Configuration> configurations,
            boolean allowSubDirectories) throws IOException {
        super(acc, recording, configurations);
        if (p != null && SecuritySupport.PRIVILEGED == fileAccess) {
            throw new SecurityException("Priviliged file access not allowed with potentially malicious Path implementation");
        }
        this.fileAccess = Objects.requireNonNull(fileAccess);
        this.repositoryFiles = new RepositoryFiles(fileAccess, p, allowSubDirectories);
    }

    @Override
    public void close() {
        setClosed(true);
        dispatcher().runCloseActions();
        repositoryFiles.close();
        if (currentParser != null) {
            currentParser.close();
            onComplete(currentParser.getEndNanos());
        }
    }

    public void setChunkCompleteHandler(Consumer<Long> handler) {
        onCompleteHandler = handler;
    }

    private void onComplete(long epochNanos) {
        Consumer<Long> handler = onCompleteHandler;
        if (handler != null) {
            handler.accept(epochNanos);
        }
    }

    @Override
    public void start() {
        start(Utils.timeToNanos(Instant.now()));
    }

    @Override
    public void startAsync() {
        startAsync(Utils.timeToNanos(Instant.now()));
    }

    @Override
    protected void process() throws IOException {
        JVM jvm = JVM.getJVM();
        Thread t = Thread.currentThread();
        try {
            if (jvm.isExcluded(t)) {
                threadExclusionLevel++;
            } else {
                jvm.exclude(t);
            }
            processRecursionSafe();
        } finally {
            if (threadExclusionLevel > 0) {
                threadExclusionLevel--;
            } else {
                jvm.include(t);
            }
        }
    }

    protected void processRecursionSafe() throws IOException {
        Dispatcher lastDisp = null;
        Dispatcher disp = dispatcher();
        Path path;
        boolean validStartTime = recording != null || disp.startTime != null;
        if (validStartTime) {
            path = repositoryFiles.firstPath(disp.startNanos, true);
        } else {
            path = repositoryFiles.lastPath(true);
        }
        if (path == null) { // closed
            return;
        }
        currentChunkStartNanos = repositoryFiles.getTimestamp(path);
        try (RecordingInput input = new RecordingInput(path.toFile(), fileAccess)) {
            currentParser = new ChunkParser(input, disp.parserConfiguration);
            long segmentStart = currentParser.getStartNanos() + currentParser.getChunkDuration();
            long filterStart = validStartTime ? disp.startNanos : segmentStart;
            long filterEnd = disp.endTime != null ? disp.endNanos : Long.MAX_VALUE;

            while (!isClosed()) {
                onMetadata(currentParser);
                while (!isClosed() && !currentParser.isChunkFinished()) {
                    disp = dispatcher();
                    if (disp != lastDisp) {
                        ParserConfiguration pc = disp.parserConfiguration;
                        pc.filterStart = filterStart;
                        pc.filterEnd = filterEnd;
                        currentParser.updateConfiguration(pc, true);
                        lastDisp = disp;
                    }
                    if (disp.parserConfiguration.isOrdered()) {
                        processOrdered(disp);
                    } else {
                        processUnordered(disp);
                    }
                    currentParser.resetCache();
                    if (currentParser.getStartNanos() + currentParser.getChunkDuration() > filterEnd) {
                        close();
                        return;
                    }
                }
                if (isLastChunk()) {
                    // Recording was stopped/closed externally, and no more data to process.
                    return;
                }

                if (repositoryFiles.hasFixedPath() && currentParser.isFinalChunk()) {
                    // JVM process exited/crashed, or repository migrated to an unknown location
                    return;
                }
                if (isClosed()) {
                    // Stream was closed
                    return;
                }
                long durationNanos = currentParser.getChunkDuration();
                long endChunkNanos = currentParser.getEndNanos();
                if (durationNanos == 0) {
                    // Avoid reading the same chunk again and again if
                    // duration is 0 ns
                    durationNanos++;
                }
                path = repositoryFiles.nextPath(currentChunkStartNanos + durationNanos, true);
                if (path == null) {
                    return; // stream closed
                }
                currentChunkStartNanos = repositoryFiles.getTimestamp(path);
                input.setFile(path);
                onComplete(endChunkNanos);
                currentParser = currentParser.newChunkParser();
                // TODO: Optimization. No need filter when we reach new chunk
                // Could set start = 0;
            }
        }
    }

    private boolean isLastChunk() {
        if (recording == null) {
            return false;
        }
        return recording.getFinalChunkStartNanos() >= currentParser.getStartNanos();
    }

    private void processOrdered(Dispatcher c) throws IOException {
        if (sortedCache == null) {
            sortedCache = new RecordedEvent[100_000];
        }
        int index = 0;
        while (true) {
            RecordedEvent e = currentParser.readStreamingEvent();
            if (e == null) {
                break;
            }
            if (index == sortedCache.length) {
                sortedCache = Arrays.copyOf(sortedCache, sortedCache.length * 2);
            }
            sortedCache[index++] = e;
        }
        onMetadata(currentParser);
        // no events found
        if (index == 0 && currentParser.isChunkFinished()) {
            onFlush();
            return;
        }
        // at least 2 events, sort them
        if (index > 1) {
            Arrays.sort(sortedCache, 0, index, EVENT_COMPARATOR);
        }
        for (int i = 0; i < index; i++) {
            c.dispatch(sortedCache[i]);
        }
        onFlush();
        return;
    }

    private boolean processUnordered(Dispatcher c) throws IOException {
        while (true) {
            RecordedEvent e = currentParser.readStreamingEvent();
            if (e == null) {
                onFlush();
                return true;
            }
            onMetadata(currentParser);
            c.dispatch(e);
        }
    }

    public void setMaxSize(long maxSize)  {
        this.maxSize = maxSize;
    }

    public void setMaxAge(Duration maxAge)  {
        this.maxAge = maxAge;
    }
}
