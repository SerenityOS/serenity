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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Frames Encoder
 *
 * Encode framed into ByteBuffers.
 * The class is stateless.
 */
public class FramesEncoder {


    public FramesEncoder() {
    }

    public List<ByteBuffer> encodeFrames(List<HeaderFrame> frames) {
        List<ByteBuffer> bufs = new ArrayList<>(frames.size() * 2);
        for (HeaderFrame f : frames) {
            bufs.addAll(encodeFrame(f));
        }
        return bufs;
    }

    public ByteBuffer encodeConnectionPreface(byte[] preface, SettingsFrame frame) {
        final int length = frame.length();
        ByteBuffer buf = getBuffer(Http2Frame.FRAME_HEADER_SIZE + length + preface.length);
        buf.put(preface);
        putSettingsFrame(buf, frame, length);
        buf.flip();
        return buf;
    }

    public List<ByteBuffer> encodeFrame(Http2Frame frame) {
        return switch (frame.type()) {
            case DataFrame.TYPE ->          encodeDataFrame((DataFrame) frame);
            case HeadersFrame.TYPE ->       encodeHeadersFrame((HeadersFrame) frame);
            case PriorityFrame.TYPE ->      encodePriorityFrame((PriorityFrame) frame);
            case ResetFrame.TYPE ->         encodeResetFrame((ResetFrame) frame);
            case SettingsFrame.TYPE ->      encodeSettingsFrame((SettingsFrame) frame);
            case PushPromiseFrame.TYPE ->   encodePushPromiseFrame((PushPromiseFrame) frame);
            case PingFrame.TYPE ->          encodePingFrame((PingFrame) frame);
            case GoAwayFrame.TYPE ->        encodeGoAwayFrame((GoAwayFrame) frame);
            case WindowUpdateFrame.TYPE ->  encodeWindowUpdateFrame((WindowUpdateFrame) frame);
            case ContinuationFrame.TYPE ->  encodeContinuationFrame((ContinuationFrame) frame);

            default -> throw new UnsupportedOperationException("Not supported frame " + frame.type() + " (" + frame.getClass().getName() + ")");
        };
    }

    private static final int NO_FLAGS = 0;
    private static final int ZERO_STREAM = 0;

    private List<ByteBuffer> encodeDataFrame(DataFrame frame) {
        // non-zero stream
        assert frame.streamid() != 0;
        ByteBuffer buf = encodeDataFrameStart(frame);
        if (frame.getFlag(DataFrame.PADDED)) {
            return joinWithPadding(buf, frame.getData(), frame.getPadLength());
        } else {
            return join(buf, frame.getData());
        }
    }

    private ByteBuffer encodeDataFrameStart(DataFrame frame) {
        boolean isPadded = frame.getFlag(DataFrame.PADDED);
        final int length = frame.getDataLength() + (isPadded ? (frame.getPadLength() + 1) : 0);
        ByteBuffer buf = getBuffer(Http2Frame.FRAME_HEADER_SIZE + (isPadded ? 1 : 0));
        putHeader(buf, length, DataFrame.TYPE, frame.getFlags(), frame.streamid());
        if (isPadded) {
            buf.put((byte) frame.getPadLength());
        }
        buf.flip();
        return buf;
    }

    private List<ByteBuffer> encodeHeadersFrame(HeadersFrame frame) {
        // non-zero stream
        assert frame.streamid() != 0;
        ByteBuffer buf = encodeHeadersFrameStart(frame);
        if (frame.getFlag(HeadersFrame.PADDED)) {
            return joinWithPadding(buf, frame.getHeaderBlock(), frame.getPadLength());
        } else {
            return join(buf, frame.getHeaderBlock());
        }
    }

    private ByteBuffer encodeHeadersFrameStart(HeadersFrame frame) {
        boolean isPadded = frame.getFlag(HeadersFrame.PADDED);
        boolean hasPriority = frame.getFlag(HeadersFrame.PRIORITY);
        final int length = frame.getHeaderLength() + (isPadded ? (frame.getPadLength() + 1) : 0) + (hasPriority ? 5 : 0);
        ByteBuffer buf = getBuffer(Http2Frame.FRAME_HEADER_SIZE + (isPadded ? 1 : 0) + (hasPriority ? 5 : 0));
        putHeader(buf, length, HeadersFrame.TYPE, frame.getFlags(), frame.streamid());
        if (isPadded) {
            buf.put((byte) frame.getPadLength());
        }
        if (hasPriority) {
            putPriority(buf, frame.getExclusive(), frame.getStreamDependency(), frame.getWeight());
        }
        buf.flip();
        return buf;
    }

    private List<ByteBuffer> encodePriorityFrame(PriorityFrame frame) {
        // non-zero stream; no flags
        assert frame.streamid() != 0;
        final int length = 5;
        ByteBuffer buf = getBuffer(Http2Frame.FRAME_HEADER_SIZE + length);
        putHeader(buf, length, PriorityFrame.TYPE, NO_FLAGS, frame.streamid());
        putPriority(buf, frame.exclusive(), frame.streamDependency(), frame.weight());
        buf.flip();
        return List.of(buf);
    }

    private List<ByteBuffer> encodeResetFrame(ResetFrame frame) {
        // non-zero stream; no flags
        assert frame.streamid() != 0;
        final int length = 4;
        ByteBuffer buf = getBuffer(Http2Frame.FRAME_HEADER_SIZE + length);
        putHeader(buf, length, ResetFrame.TYPE, NO_FLAGS, frame.streamid());
        buf.putInt(frame.getErrorCode());
        buf.flip();
        return List.of(buf);
    }

    private List<ByteBuffer> encodeSettingsFrame(SettingsFrame frame) {
        // only zero stream
        assert frame.streamid() == 0;
        final int length = frame.length();
        ByteBuffer buf = getBuffer(Http2Frame.FRAME_HEADER_SIZE + length);
        putSettingsFrame(buf, frame, length);
        buf.flip();
        return List.of(buf);
    }

    private List<ByteBuffer> encodePushPromiseFrame(PushPromiseFrame frame) {
        // non-zero stream
        assert frame.streamid() != 0;
        boolean isPadded = frame.getFlag(PushPromiseFrame.PADDED);
        final int length = frame.getHeaderLength() + (isPadded ? 5 : 4);
        ByteBuffer buf = getBuffer(Http2Frame.FRAME_HEADER_SIZE + (isPadded ? 5 : 4));
        putHeader(buf, length, PushPromiseFrame.TYPE, frame.getFlags(), frame.streamid());
        if (isPadded) {
            buf.put((byte) frame.getPadLength());
        }
        buf.putInt(frame.getPromisedStream());
        buf.flip();

        if (frame.getFlag(PushPromiseFrame.PADDED)) {
            return joinWithPadding(buf, frame.getHeaderBlock(), frame.getPadLength());
        } else {
            return join(buf, frame.getHeaderBlock());
        }
    }

    private List<ByteBuffer> encodePingFrame(PingFrame frame) {
        // only zero stream
        assert frame.streamid() == 0;
        final int length = 8;
        ByteBuffer buf = getBuffer(Http2Frame.FRAME_HEADER_SIZE + length);
        putHeader(buf, length, PingFrame.TYPE, frame.getFlags(), ZERO_STREAM);
        buf.put(frame.getData());
        buf.flip();
        return List.of(buf);
    }

    private List<ByteBuffer> encodeGoAwayFrame(GoAwayFrame frame) {
        // only zero stream; no flags
        assert frame.streamid() == 0;
        byte[] debugData = frame.getDebugData();
        final int length = 8 + debugData.length;
        ByteBuffer buf = getBuffer(Http2Frame.FRAME_HEADER_SIZE + length);
        putHeader(buf, length, GoAwayFrame.TYPE, NO_FLAGS, ZERO_STREAM);
        buf.putInt(frame.getLastStream());
        buf.putInt(frame.getErrorCode());
        if (debugData.length > 0) {
            buf.put(debugData);
        }
        buf.flip();
        return List.of(buf);
    }

    private List<ByteBuffer> encodeWindowUpdateFrame(WindowUpdateFrame frame) {
        // any stream; no flags
        final int length = 4;
        ByteBuffer buf = getBuffer(Http2Frame.FRAME_HEADER_SIZE + length);
        putHeader(buf, length, WindowUpdateFrame.TYPE, NO_FLAGS, frame.streamid);
        buf.putInt(frame.getUpdate());
        buf.flip();
        return List.of(buf);
    }

    private List<ByteBuffer> encodeContinuationFrame(ContinuationFrame frame) {
        // non-zero stream;
        assert frame.streamid() != 0;
        final int length = frame.getHeaderLength();
        ByteBuffer buf = getBuffer(Http2Frame.FRAME_HEADER_SIZE);
        putHeader(buf, length, ContinuationFrame.TYPE, frame.getFlags(), frame.streamid());
        buf.flip();
        return join(buf, frame.getHeaderBlock());
    }

    private List<ByteBuffer> joinWithPadding(ByteBuffer buf, List<ByteBuffer> data, int padLength) {
        int len = data.size();
        if (len == 0) return List.of(buf, getPadding(padLength));
        else if (len == 1) return List.of(buf, data.get(0), getPadding(padLength));
        else if (len == 2) return List.of(buf, data.get(0), data.get(1), getPadding(padLength));
        List<ByteBuffer> res = new ArrayList<>(len+2);
        res.add(buf);
        res.addAll(data);
        res.add(getPadding(padLength));
        return res;
    }

    private List<ByteBuffer> join(ByteBuffer buf, List<ByteBuffer> data) {
        int len = data.size();
        if (len == 0) return List.of(buf);
        else if (len == 1) return List.of(buf, data.get(0));
        else if (len == 2) return List.of(buf, data.get(0), data.get(1));
        List<ByteBuffer> joined = new ArrayList<>(len + 1);
        joined.add(buf);
        joined.addAll(data);
        return joined;
    }

    private void putSettingsFrame(ByteBuffer buf, SettingsFrame frame, int length) {
        // only zero stream;
        assert frame.streamid() == 0;
        putHeader(buf, length, SettingsFrame.TYPE, frame.getFlags(), ZERO_STREAM);
        frame.toByteBuffer(buf);
    }

    private void putHeader(ByteBuffer buf, int length, int type, int flags, int streamId) {
        int x = (length << 8) + type;
        buf.putInt(x);
        buf.put((byte) flags);
        buf.putInt(streamId);
    }

    private void putPriority(ByteBuffer buf, boolean exclusive, int streamDependency, int weight) {
        buf.putInt(exclusive ? (1 << 31) + streamDependency : streamDependency);
        buf.put((byte) weight);
    }

    private ByteBuffer getBuffer(int capacity) {
        return ByteBuffer.allocate(capacity);
    }

    public ByteBuffer getPadding(int length) {
        if (length > 255) {
            throw new IllegalArgumentException("Padding too big");
        }
        return ByteBuffer.allocate(length); // zeroed!
    }

}
