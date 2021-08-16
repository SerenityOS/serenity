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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import jdk.jfr.consumer.RecordedEvent;

/**
 * Implementation of an event stream that operates against a recording file.
 *
 */
public final class EventFileStream extends AbstractEventStream {
    private static final Comparator<? super RecordedEvent> EVENT_COMPARATOR = JdkJfrConsumer.instance().eventComparator();

    private final RecordingInput input;

    private ChunkParser currentParser;
    private RecordedEvent[] cacheSorted;

    public EventFileStream(@SuppressWarnings("removal") AccessControlContext acc, Path path) throws IOException {
        super(acc, null, Collections.emptyList());
        Objects.requireNonNull(path);
        this.input = new RecordingInput(path.toFile(), FileAccess.UNPRIVILEGED);
    }

    @Override
    public void start() {
        start(0);
    }

    @Override
    public void startAsync() {
        startAsync(0);
    }

    @Override
    public void close() {
        setClosed(true);
        dispatcher().runCloseActions();
        try {
            input.close();
        } catch (IOException e) {
            // ignore
        }
    }

    @Override
    protected void process() throws IOException {
        Dispatcher disp = dispatcher();
        long start = 0;
        long end = Long.MAX_VALUE;
        if (disp.startTime != null) {
            start = disp.startNanos;
        }
        if (disp.endTime != null) {
            end = disp.endNanos;
        }

        currentParser = new ChunkParser(input, disp.parserConfiguration);
        while (!isClosed()) {
            onMetadata(currentParser);
            if (currentParser.getStartNanos() > end) {
                close();
                return;
            }
            disp = dispatcher();
            disp.parserConfiguration.filterStart = start;
            disp.parserConfiguration.filterEnd = end;
            currentParser.updateConfiguration(disp.parserConfiguration, true);
            if (disp.parserConfiguration.isOrdered()) {
                processOrdered(disp);
            } else {
                processUnordered(disp);
            }
            currentParser.resetCache();
            if (isClosed() || currentParser.isLastChunk()) {
                return;
            }
            currentParser = currentParser.nextChunkParser();
        }
    }

    private void processOrdered(Dispatcher c) throws IOException {
        if (cacheSorted == null) {
            cacheSorted = new RecordedEvent[10_000];
        }
        RecordedEvent event;
        int index = 0;
        while (!currentParser.isChunkFinished()) {
            while ((event = currentParser.readStreamingEvent()) != null) {
                if (index == cacheSorted.length) {
                    RecordedEvent[] tmp = cacheSorted;
                    cacheSorted = new RecordedEvent[2 * tmp.length];
                    System.arraycopy(tmp, 0, cacheSorted, 0, tmp.length);
                }
                cacheSorted[index++] = event;
            }
            dispatchOrdered(c, index);
            index = 0;
        }
    }

    private void dispatchOrdered(Dispatcher c, int index) {
        onMetadata(currentParser);
        Arrays.sort(cacheSorted, 0, index, EVENT_COMPARATOR);
        for (int i = 0; i < index; i++) {
            c.dispatch(cacheSorted[i]);
        }
        onFlush();
    }

    private void processUnordered(Dispatcher c) throws IOException {
        onMetadata(currentParser);
        while (!isClosed()) {
            RecordedEvent event = currentParser.readStreamingEvent();
            if (event == null) {
                onFlush();
                if (currentParser.isChunkFinished()) {
                    return;
                }
                continue;
            }
            onMetadata(currentParser);
            c.dispatch(event);
        }
    }
}
