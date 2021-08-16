/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.net.http.frame;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import jdk.internal.net.http.common.Log;
import jdk.internal.net.http.common.Logger;
import jdk.internal.net.http.common.Utils;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Frames Decoder
 * <p>
 * collect buffers until frame decoding is possible,
 * all decoded frames are passed to the FrameProcessor callback in order of decoding.
 *
 * It's a stateful class due to the fact that FramesDecoder stores buffers inside.
 * Should be allocated only the single instance per connection.
 */
public class FramesDecoder {

    static final Logger debug =
            Utils.getDebugLogger("FramesDecoder"::toString, Utils.DEBUG);

    @FunctionalInterface
    public interface FrameProcessor {
        void processFrame(Http2Frame frame) throws IOException;
    }

    private final FrameProcessor frameProcessor;
    private final int maxFrameSize;

    private ByteBuffer currentBuffer; // current buffer either null or hasRemaining

    private final ArrayDeque<ByteBuffer> tailBuffers = new ArrayDeque<>();
    private int tailSize = 0;

    private boolean slicedToDataFrame = false;

    private final List<ByteBuffer> prepareToRelease = new ArrayList<>();

    // if true  - Frame Header was parsed (9 bytes consumed) and subsequent fields have meaning
    // otherwise - stopped at frames boundary
    private boolean frameHeaderParsed = false;
    private int frameLength;
    private int frameType;
    private int frameFlags;
    private int frameStreamid;
    private boolean closed;

    /**
     * Creates Frame Decoder
     *
     * @param frameProcessor - callback for decoded frames
     */
    public FramesDecoder(FrameProcessor frameProcessor) {
        this(frameProcessor, 16 * 1024);
    }

    /**
     * Creates Frame Decoder
     * @param frameProcessor - callback for decoded frames
     * @param maxFrameSize - maxFrameSize accepted by this decoder
     */
    public FramesDecoder(FrameProcessor frameProcessor, int maxFrameSize) {
        this.frameProcessor = frameProcessor;
        this.maxFrameSize = Math.min(Math.max(16 * 1024, maxFrameSize), 16 * 1024 * 1024 - 1);
    }

    /** Threshold beyond which data is no longer copied into the current buffer,
     * if that buffer has enough unused space. */
    private static final int COPY_THRESHOLD = 8192;

    /**
     * Adds the data from the given buffer, and performs frame decoding if
     * possible.   Either 1) appends the data from the given buffer to the
     * current buffer ( if there is enough unused space ), or 2) adds it to the
     * next buffer in the queue.
     *
     * If there is enough data to perform frame decoding then, all buffers are
     * decoded and the FrameProcessor is invoked.
     */
    public void decode(ByteBuffer inBoundBuffer) throws IOException {
        if (closed) {
            if (debug.on())
                debug.log("closed: ignoring buffer (%s bytes)",
                          inBoundBuffer.remaining());
            inBoundBuffer.position(inBoundBuffer.limit());
            return;
        }
        int remaining = inBoundBuffer.remaining();
        if (debug.on()) debug.log("decodes: %d", remaining);
        if (remaining > 0) {
            if (currentBuffer == null) {
                currentBuffer = inBoundBuffer;
            } else {
                ByteBuffer b = currentBuffer;
                if (!tailBuffers.isEmpty()) {
                    b = tailBuffers.getLast();
                }

                int limit = b.limit();
                int freeSpace = b.capacity() - limit;
                if (remaining <= COPY_THRESHOLD && freeSpace >= remaining) {
                    // append the new data to the unused space in the current buffer
                    int position = b.position();
                    b.position(limit);
                    b.limit(limit + inBoundBuffer.remaining());
                    b.put(inBoundBuffer);
                    b.position(position);
                    if (b != currentBuffer)
                        tailSize += remaining;
                    if (debug.on()) debug.log("copied: %d", remaining);
                } else {
                    if (debug.on()) debug.log("added: %d", remaining);
                    tailBuffers.add(inBoundBuffer);
                    tailSize += remaining;
                }
            }
        }
        if (debug.on())
            debug.log("Tail size is now: %d, current=", tailSize,
                      (currentBuffer == null ? 0 : currentBuffer.remaining()));
        Http2Frame frame;
        while ((frame = nextFrame()) != null) {
            if (debug.on()) debug.log("Got frame: %s", frame);
            frameProcessor.processFrame(frame);
            frameProcessed();
        }
    }

    private Http2Frame nextFrame() throws IOException {
        while (true) {
            if (currentBuffer == null) {
                return null; // no data at all
            }
            long available = currentBuffer.remaining() + tailSize;
            if (!frameHeaderParsed) {
                if (available >= Http2Frame.FRAME_HEADER_SIZE) {
                    parseFrameHeader();
                    if (frameLength > maxFrameSize) {
                        // connection error
                        return new MalformedFrame(ErrorFrame.FRAME_SIZE_ERROR,
                                "Frame type("+frameType+") "
                                +"length("+frameLength
                                +") exceeds MAX_FRAME_SIZE("
                                + maxFrameSize+")");
                    }
                    frameHeaderParsed = true;
                } else {
                    if (debug.on())
                        debug.log("Not enough data to parse header, needs: %d, has: %d",
                                  Http2Frame.FRAME_HEADER_SIZE, available);
                    return null;
                }
            }
            available = currentBuffer == null ? 0 : currentBuffer.remaining() + tailSize;
            if ((frameLength == 0) ||
                    (currentBuffer != null && available >= frameLength)) {
                Http2Frame frame = parseFrameBody();
                frameHeaderParsed = false;
                // frame == null means we have to skip this frame and try parse next
                if (frame != null) {
                    return frame;
                }
            } else {
                if (debug.on())
                    debug.log("Not enough data to parse frame body, needs: %d,  has: %d",
                              frameLength, available);
                return null;  // no data for the whole frame header
            }
        }
    }

    private void frameProcessed() {
        prepareToRelease.clear();
    }

    private void parseFrameHeader() throws IOException {
        int x = getInt();
        this.frameLength = (x >>> 8) & 0x00ffffff;
        this.frameType = x & 0xff;
        this.frameFlags = getByte();
        this.frameStreamid = getInt() & 0x7fffffff;
        // R: A reserved 1-bit field.  The semantics of this bit are undefined,
        // MUST be ignored when receiving.
    }

    // move next buffer from tailBuffers to currentBuffer if required
    private void nextBuffer() {
        if (!currentBuffer.hasRemaining()) {
            if (!slicedToDataFrame) {
                prepareToRelease.add(currentBuffer);
            }
            slicedToDataFrame = false;
            currentBuffer = tailBuffers.poll();
            if (currentBuffer != null) {
                tailSize -= currentBuffer.remaining();
            }
        }
    }

    public int getByte() {
        int res = currentBuffer.get() & 0xff;
        nextBuffer();
        return res;
    }

    public int getShort() {
        if (currentBuffer.remaining() >= 2) {
            int res = currentBuffer.getShort() & 0xffff;
            nextBuffer();
            return res;
        }
        int val = getByte();
        val = (val << 8) + getByte();
        return val;
    }

    public int getInt() {
        if (currentBuffer.remaining() >= 4) {
            int res = currentBuffer.getInt();
            nextBuffer();
            return res;
        }
        int val = getByte();
        val = (val << 8) + getByte();
        val = (val << 8) + getByte();
        val = (val << 8) + getByte();
        return val;

    }

    public byte[] getBytes(int n) {
        byte[] bytes = new byte[n];
        int offset = 0;
        while (n > 0) {
            int length = Math.min(n, currentBuffer.remaining());
            currentBuffer.get(bytes, offset, length);
            offset += length;
            n -= length;
            nextBuffer();
        }
        return bytes;

    }

    private List<ByteBuffer> getBuffers(boolean isDataFrame, int bytecount) {
        List<ByteBuffer> res = new ArrayList<>();
        while (bytecount > 0) {
            int remaining = currentBuffer.remaining();
            int extract = Math.min(remaining, bytecount);
            ByteBuffer extractedBuf;
            if (isDataFrame) {
                extractedBuf = Utils.sliceWithLimitedCapacity(currentBuffer, extract)
                                    .asReadOnlyBuffer();
                slicedToDataFrame = true;
            } else {
                // Header frames here
                // HPACK decoding should performed under lock and immediately after frame decoding.
                // in that case it is safe to release original buffer,
                // because of sliced buffer has a very short life
                extractedBuf = Utils.sliceWithLimitedCapacity(currentBuffer, extract);
            }
            res.add(extractedBuf);
            bytecount -= extract;
            nextBuffer();
        }
        return res;
    }

    public void close(String msg) {
        closed = true;
        tailBuffers.clear();
        int bytes = tailSize;
        ByteBuffer b = currentBuffer;
        if (b != null) {
            bytes += b.remaining();
            b.position(b.limit());
        }
        tailSize = 0;
        currentBuffer = null;
        if (debug.on()) debug.log("closed %s, ignoring %d bytes", msg, bytes);
    }

    public void skipBytes(int bytecount) {
        while (bytecount > 0) {
            int remaining = currentBuffer.remaining();
            int extract = Math.min(remaining, bytecount);
            currentBuffer.position(currentBuffer.position() + extract);
            bytecount -= remaining;
            nextBuffer();
        }
    }

    private Http2Frame parseFrameBody() throws IOException {
        assert frameHeaderParsed;
        switch (frameType) {
            case DataFrame.TYPE:
                return parseDataFrame(frameLength, frameStreamid, frameFlags);
            case HeadersFrame.TYPE:
                return parseHeadersFrame(frameLength, frameStreamid, frameFlags);
            case PriorityFrame.TYPE:
                return parsePriorityFrame(frameLength, frameStreamid, frameFlags);
            case ResetFrame.TYPE:
                return parseResetFrame(frameLength, frameStreamid, frameFlags);
            case SettingsFrame.TYPE:
                return parseSettingsFrame(frameLength, frameStreamid, frameFlags);
            case PushPromiseFrame.TYPE:
                return parsePushPromiseFrame(frameLength, frameStreamid, frameFlags);
            case PingFrame.TYPE:
                return parsePingFrame(frameLength, frameStreamid, frameFlags);
            case GoAwayFrame.TYPE:
                return parseGoAwayFrame(frameLength, frameStreamid, frameFlags);
            case WindowUpdateFrame.TYPE:
                return parseWindowUpdateFrame(frameLength, frameStreamid, frameFlags);
            case ContinuationFrame.TYPE:
                return parseContinuationFrame(frameLength, frameStreamid, frameFlags);
            default:
                // RFC 7540 4.1
                // Implementations MUST ignore and discard any frame that has a type that is unknown.
                Log.logTrace("Unknown incoming frame type: {0}", frameType);
                skipBytes(frameLength);
                return null;
        }
    }

    private Http2Frame parseDataFrame(int frameLength, int streamid, int flags) {
        // non-zero stream
        if (streamid == 0) {
            return new MalformedFrame(ErrorFrame.PROTOCOL_ERROR,
                                      "zero streamId for DataFrame");
        }
        int padLength = 0;
        if ((flags & DataFrame.PADDED) != 0) {
            padLength = getByte();
            if (padLength >= frameLength) {
                return new MalformedFrame(ErrorFrame.PROTOCOL_ERROR,
                        "the length of the padding is the length of the frame payload or greater");
            }
            frameLength--;
        }
        DataFrame df = new DataFrame(streamid, flags,
                getBuffers(true, frameLength - padLength), padLength);
        skipBytes(padLength);
        return df;

    }

    private Http2Frame parseHeadersFrame(int frameLength, int streamid, int flags) {
        // non-zero stream
        if (streamid == 0) {
            return new MalformedFrame(ErrorFrame.PROTOCOL_ERROR,
                                      "zero streamId for HeadersFrame");
        }
        int padLength = 0;
        if ((flags & HeadersFrame.PADDED) != 0) {
            padLength = getByte();
            frameLength--;
        }
        boolean hasPriority = (flags & HeadersFrame.PRIORITY) != 0;
        boolean exclusive = false;
        int streamDependency = 0;
        int weight = 0;
        if (hasPriority) {
            int x = getInt();
            exclusive = (x & 0x80000000) != 0;
            streamDependency = x & 0x7fffffff;
            weight = getByte();
            frameLength -= 5;
        }
        if(frameLength < padLength) {
            return new MalformedFrame(ErrorFrame.PROTOCOL_ERROR,
                    "Padding exceeds the size remaining for the header block");
        }
        HeadersFrame hf = new HeadersFrame(streamid, flags,
                getBuffers(false, frameLength - padLength), padLength);
        skipBytes(padLength);
        if (hasPriority) {
            hf.setPriority(streamDependency, exclusive, weight);
        }
        return hf;
    }

    private Http2Frame parsePriorityFrame(int frameLength, int streamid, int flags) {
        // non-zero stream; no flags
        if (streamid == 0) {
            return new MalformedFrame(ErrorFrame.PROTOCOL_ERROR,
                    "zero streamId for PriorityFrame");
        }
        if(frameLength != 5) {
            skipBytes(frameLength);
            return new MalformedFrame(ErrorFrame.FRAME_SIZE_ERROR, streamid,
                    "PriorityFrame length is "+ frameLength+", expected 5");
        }
        int x = getInt();
        int weight = getByte();
        return new PriorityFrame(streamid, x & 0x7fffffff, (x & 0x80000000) != 0, weight);
    }

    private Http2Frame parseResetFrame(int frameLength, int streamid, int flags) {
        // non-zero stream; no flags
        if (streamid == 0) {
            return new MalformedFrame(ErrorFrame.PROTOCOL_ERROR,
                    "zero streamId for ResetFrame");
        }
        if(frameLength != 4) {
            return new MalformedFrame(ErrorFrame.FRAME_SIZE_ERROR,
                    "ResetFrame length is "+ frameLength+", expected 4");
        }
        return new ResetFrame(streamid, getInt());
    }

    private Http2Frame parseSettingsFrame(int frameLength, int streamid, int flags) {
        // only zero stream
        if (streamid != 0) {
            return new MalformedFrame(ErrorFrame.PROTOCOL_ERROR,
                    "non-zero streamId for SettingsFrame");
        }
        if ((SettingsFrame.ACK & flags) != 0 && frameLength > 0) {
            // RFC 7540 6.5
            // Receipt of a SETTINGS frame with the ACK flag set and a length
            // field value other than 0 MUST be treated as a connection error
            return new MalformedFrame(ErrorFrame.FRAME_SIZE_ERROR,
                    "ACK SettingsFrame is not empty");
        }
        if (frameLength % 6 != 0) {
            return new MalformedFrame(ErrorFrame.FRAME_SIZE_ERROR,
                    "invalid SettingsFrame size: "+frameLength);
        }
        SettingsFrame sf = new SettingsFrame(flags);
        int n = frameLength / 6;
        for (int i=0; i<n; i++) {
            int id = getShort();
            int val = getInt();
            if (id > 0 && id <= SettingsFrame.MAX_PARAM) {
                // a known parameter. Ignore otherwise
                sf.setParameter(id, val); // TODO parameters validation
            }
        }
        return sf;
    }

    private Http2Frame parsePushPromiseFrame(int frameLength, int streamid, int flags) {
        // non-zero stream
        if (streamid == 0) {
            return new MalformedFrame(ErrorFrame.PROTOCOL_ERROR,
                    "zero streamId for PushPromiseFrame");
        }
        int padLength = 0;
        if ((flags & PushPromiseFrame.PADDED) != 0) {
            padLength = getByte();
            frameLength--;
        }
        int promisedStream = getInt() & 0x7fffffff;
        frameLength -= 4;
        if(frameLength < padLength) {
            return new MalformedFrame(ErrorFrame.PROTOCOL_ERROR,
                    "Padding exceeds the size remaining for the PushPromiseFrame");
        }
        PushPromiseFrame ppf = new PushPromiseFrame(streamid, flags, promisedStream,
                getBuffers(false, frameLength - padLength), padLength);
        skipBytes(padLength);
        return ppf;
    }

    private Http2Frame parsePingFrame(int frameLength, int streamid, int flags) {
        // only zero stream
        if (streamid != 0) {
            return new MalformedFrame(ErrorFrame.PROTOCOL_ERROR,
                    "non-zero streamId for PingFrame");
        }
        if(frameLength != 8) {
            return new MalformedFrame(ErrorFrame.FRAME_SIZE_ERROR,
                    "PingFrame length is "+ frameLength+", expected 8");
        }
        return new PingFrame(flags, getBytes(8));
    }

    private Http2Frame parseGoAwayFrame(int frameLength, int streamid, int flags) {
        // only zero stream; no flags
        if (streamid != 0) {
            return new MalformedFrame(ErrorFrame.PROTOCOL_ERROR,
                    "non-zero streamId for GoAwayFrame");
        }
        if (frameLength < 8) {
            return new MalformedFrame(ErrorFrame.FRAME_SIZE_ERROR,
                    "Invalid GoAway frame size");
        }
        int lastStream = getInt() & 0x7fffffff;
        int errorCode = getInt();
        byte[] debugData = getBytes(frameLength - 8);
        if (debugData.length > 0) {
            Log.logError("GoAway debugData " + new String(debugData, UTF_8));
        }
        return new GoAwayFrame(lastStream, errorCode, debugData);
    }

    private Http2Frame parseWindowUpdateFrame(int frameLength, int streamid, int flags) {
        // any stream; no flags
        if(frameLength != 4) {
            return new MalformedFrame(ErrorFrame.FRAME_SIZE_ERROR,
                    "WindowUpdateFrame length is "+ frameLength+", expected 4");
        }
        return new WindowUpdateFrame(streamid, getInt() & 0x7fffffff);
    }

    private Http2Frame parseContinuationFrame(int frameLength, int streamid, int flags) {
        // non-zero stream;
        if (streamid == 0) {
            return new MalformedFrame(ErrorFrame.PROTOCOL_ERROR,
                    "zero streamId for ContinuationFrame");
        }
        return new ContinuationFrame(streamid, flags, getBuffers(false, frameLength));
    }

}
