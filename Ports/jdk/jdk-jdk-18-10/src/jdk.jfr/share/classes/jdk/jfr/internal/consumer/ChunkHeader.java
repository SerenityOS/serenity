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

import jdk.jfr.internal.LogLevel;
import jdk.jfr.internal.LogTag;
import jdk.jfr.internal.Logger;
import jdk.jfr.internal.MetadataDescriptor;
import jdk.jfr.internal.Utils;

public final class ChunkHeader {
    static final long HEADER_SIZE = 68;
    static final byte UPDATING_CHUNK_HEADER = (byte) 255;
    static final long CHUNK_SIZE_POSITION = 8;
    static final long DURATION_NANOS_POSITION = 40;
    static final long FILE_STATE_POSITION = 64;
    static final long FLAG_BYTE_POSITION = 67;
    static final long METADATA_TYPE_ID = 0;
    static final byte[] FILE_MAGIC = { 'F', 'L', 'R', '\0' };
    static final int MASK_FINAL_CHUNK = 1 << 1;

    private final short major;
    private final short minor;
    private final long chunkStartTicks;
    private final long ticksPerSecond;
    private final long chunkStartNanos;
    private final long absoluteChunkStart;
    private final RecordingInput input;
    private final long id;
    private long absoluteEventStart;
    private long chunkSize = 0;
    private long constantPoolPosition = 0;
    private long metadataPosition = 0;
    private long durationNanos;
    private long absoluteChunkEnd;
    private boolean isFinished;
    private boolean finished;
    private boolean finalChunk;

    public ChunkHeader(RecordingInput input) throws IOException {
        this(input, 0, 0);
    }

    private ChunkHeader(RecordingInput input, long absoluteChunkStart, long id) throws IOException {
        this.absoluteChunkStart = absoluteChunkStart;
        this.absoluteEventStart = absoluteChunkStart + HEADER_SIZE;
        if (input.getFileSize() < HEADER_SIZE) {
            throw new IOException("Not a complete Chunk header");
        }
        input.setValidSize(absoluteChunkStart + HEADER_SIZE);
        input.position(absoluteChunkStart);
        if (input.position() >= input.size()) {
           throw new IOException("Chunk contains no data");
        }
        verifyMagic(input);
        this.input = input;
        this.id = id;
        Logger.log(LogTag.JFR_SYSTEM_PARSER, LogLevel.INFO, "Chunk: " + id);
        Logger.log(LogTag.JFR_SYSTEM_PARSER, LogLevel.INFO, "Chunk: file=" + input.getFilename());
        Logger.log(LogTag.JFR_SYSTEM_PARSER, LogLevel.INFO, "Chunk: startPosition=" + absoluteChunkStart);
        major = input.readRawShort();
        Logger.log(LogTag.JFR_SYSTEM_PARSER, LogLevel.INFO, "Chunk: major=" + major);
        minor = input.readRawShort();
        Logger.log(LogTag.JFR_SYSTEM_PARSER, LogLevel.INFO, "Chunk: minor=" + minor);
        if (major != 1 && major != 2) {
            throw new IOException("File version " + major + "." + minor + ". Only Flight Recorder files of version 1.x and 2.x can be read by this JDK.");
        }
        long c = input.readRawLong(); // chunk size
        Logger.log(LogTag.JFR_SYSTEM_PARSER, LogLevel.INFO, "Chunk: chunkSize=" + c);
        input.readRawLong(); // constant pool position
        Logger.log(LogTag.JFR_SYSTEM_PARSER, LogLevel.INFO, "Chunk: constantPoolPosition=" + constantPoolPosition);
        input.readRawLong(); // metadata position
        Logger.log(LogTag.JFR_SYSTEM_PARSER, LogLevel.INFO, "Chunk: metadataPosition=" + metadataPosition);
        chunkStartNanos = input.readRawLong(); // nanos since epoch
        Logger.log(LogTag.JFR_SYSTEM_PARSER, LogLevel.INFO, "Chunk: startNanos=" + chunkStartNanos);
        durationNanos = input.readRawLong(); // duration nanos, not used
        Logger.log(LogTag.JFR_SYSTEM_PARSER, LogLevel.INFO, "Chunk: durationNanos=" + durationNanos);
        chunkStartTicks = input.readRawLong();
        Logger.log(LogTag.JFR_SYSTEM_PARSER, LogLevel.INFO, "Chunk: startTicks=" + chunkStartTicks);
        ticksPerSecond = input.readRawLong();
        Logger.log(LogTag.JFR_SYSTEM_PARSER, LogLevel.INFO, "Chunk: ticksPerSecond=" + ticksPerSecond);
        input.readRawInt(); // ignore file state and flag bits
        refresh();
        input.position(absoluteEventStart);
    }

    public void refresh() throws IOException {
        while (true) {
            byte fileState1;
            input.positionPhysical(absoluteChunkStart + FILE_STATE_POSITION);
            while ((fileState1 = input.readPhysicalByte()) == UPDATING_CHUNK_HEADER) {
                Utils.takeNap(1);
                input.positionPhysical(absoluteChunkStart + FILE_STATE_POSITION);
            }
            input.positionPhysical(absoluteChunkStart + CHUNK_SIZE_POSITION);
            long chunkSize = input.readPhysicalLong();
            long constantPoolPosition = input.readPhysicalLong();
            long metadataPosition = input.readPhysicalLong();
            input.positionPhysical(absoluteChunkStart + DURATION_NANOS_POSITION);
            long durationNanos = input.readPhysicalLong();
            input.positionPhysical(absoluteChunkStart + FILE_STATE_POSITION);
            byte fileState2 =  input.readPhysicalByte();
            input.positionPhysical(absoluteChunkStart + FLAG_BYTE_POSITION);
            int flagByte = input.readPhysicalByte();
            if (fileState1 == fileState2) { // valid header
                finished = fileState1 == 0;
                if (metadataPosition != 0) {
                    Logger.log(LogTag.JFR_SYSTEM_PARSER, LogLevel.INFO, "Setting input size to " + (absoluteChunkStart + chunkSize));
                    if (finished) {
                        // This assumes that the whole recording
                        // is finished if the first chunk is.
                        // This is a limitation we may want to
                        // remove, but greatly improves performance as
                        // data can be read across chunk boundaries
                        // of multi-chunk files and only once.
                        input.setValidSize(input.getFileSize());
                    } else {
                        input.setValidSize(absoluteChunkStart + chunkSize);
                    }
                    this.chunkSize = chunkSize;
                    Logger.log(LogTag.JFR_SYSTEM_PARSER, LogLevel.INFO, "Chunk: chunkSize=" + chunkSize);
                    this.constantPoolPosition = constantPoolPosition;
                    Logger.log(LogTag.JFR_SYSTEM_PARSER, LogLevel.INFO, "Chunk: constantPoolPosition=" + constantPoolPosition);
                    this.metadataPosition = metadataPosition;
                    Logger.log(LogTag.JFR_SYSTEM_PARSER, LogLevel.INFO, "Chunk: metadataPosition=" + metadataPosition);
                    this.durationNanos = durationNanos;
                    Logger.log(LogTag.JFR_SYSTEM_PARSER, LogLevel.INFO, "Chunk: durationNanos =" + durationNanos);
                    isFinished = fileState2 == 0;
                    Logger.log(LogTag.JFR_SYSTEM_PARSER, LogLevel.INFO, "Chunk: generation=" + fileState2);
                    Logger.log(LogTag.JFR_SYSTEM_PARSER, LogLevel.INFO, "Chunk: finished=" + isFinished);
                    Logger.log(LogTag.JFR_SYSTEM_PARSER, LogLevel.INFO, "Chunk: fileSize=" + input.size());
                    this.finalChunk = (flagByte & MASK_FINAL_CHUNK) != 0;
                    Logger.log(LogTag.JFR_SYSTEM_PARSER, LogLevel.INFO, "Chunk: finalChunk=" + finalChunk);
                    absoluteChunkEnd = absoluteChunkStart + chunkSize;
                    return;
                }
            }
        }
    }

    public boolean readHeader(byte[] bytes, int count) throws IOException {
        input.position(absoluteChunkStart);
        for (int i = 0; i< count; i++) {
            bytes[i] = input.readPhysicalByte();
        }
        return bytes[(int)FILE_STATE_POSITION] != UPDATING_CHUNK_HEADER;
    }

    public void awaitFinished() throws IOException {
        if (finished) {
            return;
        }
        long pos = input.position();
        try {
            input.positionPhysical(absoluteChunkStart + FILE_STATE_POSITION);
            while (true) {
                byte filestate = input.readPhysicalByte();
                if (filestate == 0) {
                    finished = true;
                    return;
                }
                Utils.takeNap(1);
            }
        } finally {
            input.position(pos);
        }
    }

    public boolean isLastChunk() throws IOException {
        awaitFinished();
        // streaming files only have one chunk
        return input.getFileSize() == absoluteChunkEnd;
   }

    public boolean isFinalChunk() {
        return finalChunk;
    }

    public boolean isFinished() throws IOException {
        return isFinished;
    }

    public ChunkHeader nextHeader() throws IOException {
        return new ChunkHeader(input, absoluteChunkEnd, id + 1);
    }
    public MetadataDescriptor readMetadata() throws IOException {
        return readMetadata(null);
    }

    public MetadataDescriptor readMetadata(MetadataDescriptor previous) throws IOException {
        input.position(absoluteChunkStart + metadataPosition);
        input.readInt(); // size
        long id = input.readLong(); // event type id
        if (id != METADATA_TYPE_ID) {
            throw new IOException("Expected metadata event. Type id=" + id + ", should have been " + METADATA_TYPE_ID);
        }
        input.readLong(); // start time
        input.readLong(); // duration
        long metadataId = input.readLong();
        if (previous != null && metadataId == previous.metadataId) {
            return previous;
        }
        Logger.log(LogTag.JFR_SYSTEM_PARSER, LogLevel.TRACE, "New metadata id = " + metadataId);
        MetadataDescriptor m =  MetadataDescriptor.read(input);
        m.metadataId = metadataId;
        return m;
    }


    public short getMajor() {
        return major;
    }

    public short getMinor() {
        return minor;
    }

    public long getAbsoluteChunkStart() {
        return absoluteChunkStart;
    }

    public long getAbsoluteEventStart() {
        return absoluteEventStart;
    }
    public long getConstantPoolPosition() {
        return constantPoolPosition;
    }

    public long getMetataPosition() {
        return metadataPosition;
    }
    public long getStartTicks() {
        return chunkStartTicks;
    }
    public long getChunkSize() {
        return chunkSize;
    }

    public double getTicksPerSecond() {
        return ticksPerSecond;
    }

    public long getStartNanos() {
        return chunkStartNanos;
    }

    public long getEnd() {
        return absoluteChunkEnd;
    }

    public long getSize() {
        return chunkSize;
    }

    public long getDurationNanos() {
        return durationNanos;
    }

    public RecordingInput getInput() {
        return input;
    }

    private static void verifyMagic(RecordingInput input) throws IOException {
        for (byte c : FILE_MAGIC) {
            if (input.readByte() != c) {
                throw new IOException("Not a Flight Recorder file");
            }
        }
    }

    public long getEventStart() {
        return absoluteEventStart;
    }

    static long headerSize() {
        return HEADER_SIZE;
    }

    public long getLastNanos() {
        return getStartNanos() + getDurationNanos();
    }
}
