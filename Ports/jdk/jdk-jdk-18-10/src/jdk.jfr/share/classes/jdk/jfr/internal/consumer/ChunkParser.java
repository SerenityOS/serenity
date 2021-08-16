/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

import jdk.jfr.EventType;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedObject;
import jdk.jfr.internal.LogLevel;
import jdk.jfr.internal.LogTag;
import jdk.jfr.internal.Logger;
import jdk.jfr.internal.LongMap;
import jdk.jfr.internal.MetadataDescriptor;
import jdk.jfr.internal.Type;
import jdk.jfr.internal.Utils;

/**
 * Parses a chunk.
 *
 */
public final class ChunkParser {

    static final class ParserConfiguration {
        private final boolean reuse;
        private final boolean ordered;
        private final ParserFilter eventFilter;

        long filterStart;
        long filterEnd;

        ParserConfiguration(long filterStart, long filterEnd, boolean reuse, boolean ordered, ParserFilter filter) {
            this.filterStart = filterStart;
            this.filterEnd = filterEnd;
            this.reuse = reuse;
            this.ordered = ordered;
            this.eventFilter = filter;
        }

        public ParserConfiguration() {
            this(0, Long.MAX_VALUE, false, false, ParserFilter.ACCEPT_ALL);
        }

        public boolean isOrdered() {
            return ordered;
        }
    }

    private enum CheckPointType {
        // Checkpoint that finishes a flush segment
        FLUSH(1),
        // Checkpoint contains chunk header information in the first pool
        CHUNK_HEADER(2),
        // Checkpoint contains only statics that will not change from chunk to chunk
        STATICS(4),
        // Checkpoint contains thread related information
        THREAD(8);
        private final int mask;
        private CheckPointType(int mask) {
            this.mask = mask;
        }

        private boolean is(int flags) {
            return (mask & flags) != 0;
        }
    }
    public static final RecordedEvent FLUSH_MARKER = JdkJfrConsumer.instance().newRecordedEvent(null, null, 0L, 0L);

    private static final long CONSTANT_POOL_TYPE_ID = 1;
    private final RecordingInput input;
    private final ChunkHeader chunkHeader;
    private final TimeConverter timeConverter;

    private final LongMap<ConstantLookup> constantLookups;

    private LongMap<Type> typeMap;
    private LongMap<Parser> parsers;
    private boolean chunkFinished;

    private ParserConfiguration configuration;
    private volatile boolean closed;
    private MetadataDescriptor previousMetadata;
    private MetadataDescriptor metadata;
    private boolean staleMetadata = true;

    public ChunkParser(RecordingInput input) throws IOException {
        this(input, new ParserConfiguration());
    }

    ChunkParser(RecordingInput input, ParserConfiguration pc) throws IOException {
       this(new ChunkHeader(input), null, pc);
    }

    private ChunkParser(ChunkParser previous) throws IOException {
        this(new ChunkHeader(previous.input), previous, new ParserConfiguration());
     }

    private ChunkParser(ChunkHeader header, ChunkParser previous, ParserConfiguration pc) throws IOException {
        this.configuration = pc;
        this.input = header.getInput();
        this.chunkHeader = header;
        if (previous == null) {
            this.constantLookups = new LongMap<>();
            this.previousMetadata = null;
        } else {
            this.constantLookups = previous.constantLookups;
            this.previousMetadata = previous.metadata;
            this.configuration = previous.configuration;
        }
        this.metadata = header.readMetadata(previousMetadata);
        this.timeConverter = new TimeConverter(chunkHeader, metadata.getGMTOffset());
        if (metadata != previousMetadata) {
            ParserFactory factory = new ParserFactory(metadata, constantLookups, timeConverter);
            parsers = factory.getParsers();
            typeMap = factory.getTypeMap();
            updateConfiguration();
        } else {
            parsers = previous.parsers;
            typeMap = previous.typeMap;
        }
        constantLookups.forEach(c -> c.newPool());
        fillConstantPools(0);
        constantLookups.forEach(c -> c.getLatestPool().setResolving());
        constantLookups.forEach(c -> c.getLatestPool().resolve());
        constantLookups.forEach(c -> c.getLatestPool().setResolved());

        input.position(chunkHeader.getEventStart());
    }

    public ChunkParser nextChunkParser() throws IOException {
        return new ChunkParser(chunkHeader.nextHeader(), this, configuration);
    }

    private void updateConfiguration() {
        updateConfiguration(configuration, false);
    }

    void updateConfiguration(ParserConfiguration configuration, boolean resetEventCache) {
        this.configuration = configuration;
        parsers.forEach(p -> {
            if (p instanceof EventParser ep) {
                if (resetEventCache) {
                    ep.resetCache();
                }
                String name = ep.getEventType().getName();
                ep.setOrdered(configuration.ordered);
                ep.setReuse(configuration.reuse);
                ep.setFilterStart(configuration.filterStart);
                ep.setFilterEnd(configuration.filterEnd);
                long threshold = configuration.eventFilter.getThreshold(name);
                if (threshold >= 0) {
                    ep.setEnabled(true);
                    ep.setThresholdNanos(threshold);
                } else {
                    ep.setEnabled(false);
                    ep.setThresholdNanos(Long.MAX_VALUE);
                }
            }
        });
    }

    /**
     * Reads an event and returns null when segment or chunk ends.
     */
    RecordedEvent readStreamingEvent() throws IOException {
        long absoluteChunkEnd = chunkHeader.getEnd();
        RecordedEvent event = readEvent();
        if (event == ChunkParser.FLUSH_MARKER) {
            return null;
        }
        if (event != null) {
            return event;
        }
        long lastValid = absoluteChunkEnd;
        long metadataPosition = chunkHeader.getMetataPosition();
        long constantPosition = chunkHeader.getConstantPoolPosition();
        chunkFinished = awaitUpdatedHeader(absoluteChunkEnd, configuration.filterEnd);
        if (chunkFinished) {
            Logger.log(LogTag.JFR_SYSTEM_PARSER, LogLevel.INFO, "At chunk end");
            return null;
        }
        absoluteChunkEnd = chunkHeader.getEnd();
        // Read metadata and constant pools for the next segment
        if (chunkHeader.getMetataPosition() != metadataPosition) {
            Logger.log(LogTag.JFR_SYSTEM_PARSER, LogLevel.INFO, "Found new metadata in chunk. Rebuilding types and parsers");
            this.previousMetadata = this.metadata;
            this.metadata = chunkHeader.readMetadata(previousMetadata);
            ParserFactory factory = new ParserFactory(metadata, constantLookups, timeConverter);
            parsers = factory.getParsers();
            typeMap = factory.getTypeMap();
            updateConfiguration();
            setStaleMetadata(true);
        }
        if (constantPosition != chunkHeader.getConstantPoolPosition()) {
            Logger.log(LogTag.JFR_SYSTEM_PARSER, LogLevel.INFO, "Found new constant pool data. Filling up pools with new values");
            constantLookups.forEach(c -> c.getLatestPool().setAllResolved(false));
            fillConstantPools(constantPosition + chunkHeader.getAbsoluteChunkStart());
            constantLookups.forEach(c -> c.getLatestPool().setResolving());
            constantLookups.forEach(c -> c.getLatestPool().resolve());
            constantLookups.forEach(c -> c.getLatestPool().setResolved());
        }
        input.position(lastValid);
        return null;
    }

    /**
     * Reads an event and returns null when the chunk ends
     */
    public RecordedEvent readEvent() throws IOException {
        long absoluteChunkEnd = chunkHeader.getEnd();
        while (input.position() < absoluteChunkEnd) {
            long pos = input.position();
            int size = input.readInt();
            if (size == 0) {
                throw new IOException("Event can't have zero size");
            }
            long typeId = input.readLong();
            Parser p = parsers.get(typeId);
            if (p instanceof EventParser ep) {
                // Fast path
                RecordedEvent event = ep.parse(input);
                if (event != null) {
                    input.position(pos + size);
                    return event;
                }
                // Not accepted by filter
            } else {
                if (typeId == 1) { // checkpoint event
                    if (CheckPointType.FLUSH.is(parseCheckpointType())) {
                        input.position(pos + size);
                        return FLUSH_MARKER;
                    }
                } else {
                    if (typeId != 0) { // Not metadata event
                        Logger.log(LogTag.JFR_SYSTEM_PARSER, LogLevel.INFO, "Unknown event type " + typeId);
                    }
                }
            }
            input.position(pos + size);
        }
        return null;
    }

    private byte parseCheckpointType() throws IOException {
        input.readLong(); // timestamp
        input.readLong(); // duration
        input.readLong(); // delta
        return input.readByte();
    }

    private boolean awaitUpdatedHeader(long absoluteChunkEnd, long filterEnd) throws IOException {
        if (Logger.shouldLog(LogTag.JFR_SYSTEM_PARSER, LogLevel.INFO)) {
            Logger.log(LogTag.JFR_SYSTEM_PARSER, LogLevel.INFO, "Waiting for more data (streaming). Read so far: " + chunkHeader.getChunkSize() + " bytes");
        }
        while (true) {
            if (closed) {
                return true;
            }
            if (chunkHeader.getLastNanos() > filterEnd)  {
              return true;
            }
            chunkHeader.refresh();
            if (absoluteChunkEnd != chunkHeader.getEnd()) {
                return false;
            }
            if (chunkHeader.isFinished()) {
                return true;
            }
            Utils.waitFlush(1000);
        }
    }

    private void fillConstantPools(long abortCP) throws IOException {
        long thisCP = chunkHeader.getConstantPoolPosition() + chunkHeader.getAbsoluteChunkStart();
        long lastCP = -1;
        long delta = -1;
        boolean logTrace = Logger.shouldLog(LogTag.JFR_SYSTEM_PARSER, LogLevel.TRACE);
        while (thisCP != abortCP && delta != 0) {
            input.position(thisCP);
            lastCP = thisCP;
            int size = input.readInt(); // size
            long typeId = input.readLong();
            if (typeId != CONSTANT_POOL_TYPE_ID) {
                throw new IOException("Expected check point event (id = 1) at position " + lastCP + ", but found type id = " + typeId);
            }
            input.readLong(); // timestamp
            input.readLong(); // duration
            delta = input.readLong();
            thisCP += delta;
            boolean flush = input.readBoolean();
            int poolCount = input.readInt();
            final long logLastCP = lastCP;
            final long logDelta = delta;
            if (logTrace) {
                Logger.log(LogTag.JFR_SYSTEM_PARSER, LogLevel.TRACE,
                        "New constant pool: startPosition=" + logLastCP +
                        ", size=" + size + ", deltaToNext=" + logDelta +
                        ", flush=" + flush + ", poolCount=" + poolCount);
            }
            for (int i = 0; i < poolCount; i++) {
                long id = input.readLong(); // type id
                ConstantLookup lookup = constantLookups.get(id);
                Type type = typeMap.get(id);
                if (lookup == null) {
                    if (type == null) {
                        throw new IOException(
                                "Error parsing constant pool type " + getName(id) + " at position " + input.position() + " at check point between [" + lastCP + ", " + (lastCP + size) + "]");
                    }
                    ConstantMap pool = new ConstantMap(ObjectFactory.create(type, timeConverter), type.getName());
                    lookup = new ConstantLookup(pool, type);
                    constantLookups.put(type.getId(), lookup);
                }
                Parser parser = parsers.get(id);
                if (parser == null) {
                    throw new IOException("Could not find constant pool type with id = " + id);
                }
                try {
                    int count = input.readInt();
                    if (count == 0) {
                        throw new InternalError("Pool " + type.getName() + " must contain at least one element ");
                    }
                    if (logTrace) {
                        Logger.log(LogTag.JFR_SYSTEM_PARSER, LogLevel.TRACE, "Constant Pool " + i + ": " + type.getName());
                    }
                    for (int j = 0; j < count; j++) {
                        long key = input.readLong();
                        Object resolved = lookup.getPreviousResolved(key);
                        if (resolved == null) {
                            Object v = parser.parse(input);
                            logConstant(key, v, false);
                            lookup.getLatestPool().put(key, v);
                        } else {
                            parser.skip(input);
                            logConstant(key, resolved, true);
                            lookup.getLatestPool().putResolved(key, resolved);
                        }
                    }
                } catch (Exception e) {
                    throw new IOException("Error parsing constant pool type " + getName(id) + " at position " + input.position() + " at check point between [" + lastCP + ", " + (lastCP + size) + "]",
                            e);
                }
            }
            if (input.position() != lastCP + size) {
                throw new IOException("Size of check point event doesn't match content");
            }
        }
    }

    private void logConstant(long key, Object v, boolean preresolved) {
        if (!Logger.shouldLog(LogTag.JFR_SYSTEM_PARSER, LogLevel.TRACE)) {
            return;
        }
        String valueText;
        if (v.getClass().isArray()) {
            Object[] array = (Object[]) v;
            StringJoiner sj = new StringJoiner(", ", "{", "}");
            for (int i = 0; i < array.length; i++) {
                sj.add(textify(array[i]));
            }
            valueText = sj.toString();
        } else {
            valueText = textify(v);
        }
        String suffix  = preresolved ? " (presolved)" :"";
        Logger.log(LogTag.JFR_SYSTEM_PARSER, LogLevel.TRACE, "Constant: " + key + " = " + valueText + suffix);
    }

    private String textify(Object o) {
        if (o == null) { // should not happen
            return "null";
        }
        if (o instanceof String s) {
            return "\"" + s + "\"";
        }
        if (o instanceof RecordedObject) {
            return o.getClass().getName();
        }
        if (o.getClass().isArray()) {
            Object[] array = (Object[]) o;
            if (array.length > 0) {
                return textify(array[0]) + "[]"; // can it be recursive?
            }
        }
        return String.valueOf(o);
    }

    private String getName(long id) {
        Type type = typeMap.get(id);
        return type == null ? ("unknown(" + id + ")") : type.getName();
    }

    public Collection<Type> getTypes() {
        return metadata.getTypes();
    }

    public List<EventType> getEventTypes() {
        return metadata.getEventTypes();
    }

    public List<EventType> getPreviousEventTypes() {
        if (previousMetadata == null) {
            return Collections.emptyList();
        } else {
            return previousMetadata.getEventTypes();
        }
    }

    public boolean isLastChunk() throws IOException {
        return chunkHeader.isLastChunk();
    }

    ChunkParser newChunkParser() throws IOException {
        return new ChunkParser(this);
    }

    public boolean isChunkFinished() {
        return chunkFinished;
    }

    public long getChunkDuration() {
        return chunkHeader.getDurationNanos();
    }

    public long getStartNanos() {
        return chunkHeader.getStartNanos();
    }

    public boolean isFinalChunk() {
        return chunkHeader.isFinalChunk();
    }

    public void close() {
        this.closed = true;
        try {
            input.close();
        } catch(IOException e) {
           // ignore
        }
        Utils.notifyFlush();
    }

    public long getEndNanos() {
        return getStartNanos() + getChunkDuration();
    }

    public void setStaleMetadata(boolean stale) {
        this.staleMetadata = stale;
    }

    public boolean hasStaleMetadata() {
        return staleMetadata;
    }

    public void resetCache() {
        LongMap<Parser> ps = this.parsers;
        if (ps != null) {
            ps.forEach(p -> {
                if (p instanceof EventParser ep) {
                    ep.resetCache();
                }
            });
        }
    }
}
