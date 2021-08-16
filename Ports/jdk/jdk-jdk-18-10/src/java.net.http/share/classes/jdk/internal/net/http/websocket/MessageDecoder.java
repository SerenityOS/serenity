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

package jdk.internal.net.http.websocket;

import jdk.internal.net.http.common.Logger;
import jdk.internal.net.http.common.Utils;
import jdk.internal.net.http.websocket.Frame.Opcode;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static jdk.internal.net.http.common.Utils.dump;
import static jdk.internal.net.http.websocket.StatusCodes.NO_STATUS_CODE;
import static jdk.internal.net.http.websocket.StatusCodes.isLegalToReceiveFromServer;

/*
 * Consumes frame parts and notifies a message consumer, when there is
 * sufficient data to produce a message, or part thereof.
 *
 * Data consumed but not yet translated is accumulated until it's sufficient to
 * form a message.
 */
/* Exposed for testing purposes */
class MessageDecoder implements Frame.Consumer {

    private static final Logger debug =
            Utils.getWebSocketLogger("[Input]"::toString, Utils.DEBUG_WS);

    private final MessageStreamConsumer output;
    private final UTF8AccumulatingDecoder decoder = new UTF8AccumulatingDecoder();
    private boolean fin;
    private Opcode opcode, originatingOpcode;
    private long payloadLen;
    private long unconsumedPayloadLen;
    private ByteBuffer binaryData;
    private final boolean server;
    private int maskingKey;

    MessageDecoder(MessageStreamConsumer output) {
        this(output, false);
    }

    MessageDecoder(MessageStreamConsumer output, boolean server) {
        this.output = requireNonNull(output);
        this.server = server;
    }

    /* Exposed for testing purposes */
    MessageStreamConsumer getOutput() {
        return output;
    }

    @Override
    public void fin(boolean value) {
        if (debug.on()) {
            debug.log("fin %s", value);
        }
        fin = value;
    }

    @Override
    public void rsv1(boolean value) {
        if (debug.on()) {
            debug.log("rsv1 %s", value);
        }
        if (value) {
            throw new FailWebSocketException("Unexpected rsv1 bit");
        }
    }

    @Override
    public void rsv2(boolean value) {
        if (debug.on()) {
            debug.log("rsv2 %s", value);
        }
        if (value) {
            throw new FailWebSocketException("Unexpected rsv2 bit");
        }
    }

    @Override
    public void rsv3(boolean value) {
        if (debug.on()) {
            debug.log("rsv3 %s", value);
        }
        if (value) {
            throw new FailWebSocketException("Unexpected rsv3 bit");
        }
    }

    @Override
    public void opcode(Opcode v) {
        if (debug.on()) {
            debug.log("opcode %s", v);
        }
        if (v == Opcode.PING || v == Opcode.PONG || v == Opcode.CLOSE) {
            if (!fin) {
                throw new FailWebSocketException("Fragmented control frame  " + v);
            }
            opcode = v;
        } else if (v == Opcode.TEXT || v == Opcode.BINARY) {
            if (originatingOpcode != null) {
                throw new FailWebSocketException(
                        format("Unexpected frame %s (fin=%s)", v, fin));
            }
            opcode = v;
            if (!fin) {
                originatingOpcode = v;
            }
        } else if (v == Opcode.CONTINUATION) {
            if (originatingOpcode == null) {
                throw new FailWebSocketException(
                        format("Unexpected frame %s (fin=%s)", v, fin));
            }
            opcode = v;
        } else {
            throw new FailWebSocketException("Unexpected opcode " + v);
        }
    }

    @Override
    public void mask(boolean value) {
        if (debug.on()) {
            debug.log("mask %s", value);
        }
        if (value && !server) {
            throw new FailWebSocketException("Masked frame received");
        }
        if (!value && server) {
            throw new FailWebSocketException("Masked frame expected");
        }
    }

    @Override
    public void payloadLen(long value) {
        if (debug.on()) {
            debug.log("payloadLen %s", value);
        }
        if (opcode.isControl()) {
            if (value > Frame.MAX_CONTROL_FRAME_PAYLOAD_LENGTH) {
                throw new FailWebSocketException(
                        format("%s's payload length %s", opcode, value));
            }
            assert Opcode.CLOSE.isControl();
            if (opcode == Opcode.CLOSE && value == 1) {
                throw new FailWebSocketException("Incomplete status code");
            }
        }
        payloadLen = value;
        unconsumedPayloadLen = value;
    }

    @Override
    public void maskingKey(int value) {
        // `MessageDecoder.mask(boolean)` is where a masked frame is detected and
        // reported on; `MessageDecoder.mask(boolean)` MUST be invoked before
        // this method;
        // So this method (`maskingKey`) is not supposed to be invoked while
        // reading a frame that has came from the server. If this method is
        // invoked, then it's an error in implementation, thus InternalError
        if (!server)
            throw new InternalError();
        maskingKey = value;
    }

    @Override
    public void payloadData(ByteBuffer data) {
        if (debug.on()) {
            debug.log("payload %s", data);
        }
        unconsumedPayloadLen -= data.remaining();
        boolean lastPayloadChunk = unconsumedPayloadLen == 0;
        if (opcode.isControl()) {
            if (binaryData != null) { // An intermediate or the last chunk
                binaryData.put(data);
            } else if (!lastPayloadChunk) { // The first chunk
                int remaining = data.remaining();
                // It shouldn't be 125, otherwise the next chunk will be of size
                // 0, which is not what Reader promises to deliver (eager
                // reading)
                assert remaining < Frame.MAX_CONTROL_FRAME_PAYLOAD_LENGTH
                        : dump(remaining);
                binaryData = ByteBuffer.allocate(
                        Frame.MAX_CONTROL_FRAME_PAYLOAD_LENGTH).put(data);
            } else { // The only chunk
                binaryData = ByteBuffer.allocate(data.remaining()).put(data);
            }
        } else {
            boolean last = fin && lastPayloadChunk;
            boolean text = opcode == Opcode.TEXT || originatingOpcode == Opcode.TEXT;
            if (!text) {
                ByteBuffer slice = data.slice();
                if (server) {
                    unMask(slice);
                }
                output.onBinary(slice, last);
                data.position(data.limit()); // Consume
            } else {
                boolean binaryNonEmpty = data.hasRemaining();
                if (server) {
                    unMask(data);
                }
                CharBuffer textData;
                try {
                    textData = decoder.decode(data, last);
                } catch (CharacterCodingException e) {
                    throw new FailWebSocketException(
                            "Invalid UTF-8 in frame " + opcode,
                            StatusCodes.NOT_CONSISTENT).initCause(e);
                }
                if (!(binaryNonEmpty && !textData.hasRemaining())) {
                    // If there's a binary data, that result in no text, then we
                    // don't deliver anything, otherwise:
                    output.onText(textData, last);
                }
            }
        }
    }

    private void unMask(ByteBuffer src) {
        int pos = src.position();
        int size = src.remaining();
        ByteBuffer temp = ByteBuffer.allocate(size);
        Frame.Masker.transferMasking(src, temp, maskingKey);
        temp.flip();
        src.position(pos);
        src.put(temp);
        src.position(pos).limit(pos+size);
    }

    @Override
    public void endFrame() {
        if (debug.on()) {
            debug.log("end frame");
        }
        if (opcode.isControl()) {
            binaryData.flip();
        }
        switch (opcode) {
            case CLOSE:
                char statusCode = NO_STATUS_CODE;
                String reason = "";
                if (payloadLen != 0) {
                    int len = binaryData.remaining();
                    assert 2 <= len
                            && len <= Frame.MAX_CONTROL_FRAME_PAYLOAD_LENGTH
                            : dump(len, payloadLen);
                    statusCode = binaryData.getChar();
                    if (!isLegalToReceiveFromServer(statusCode)) {
                        throw new FailWebSocketException(
                                "Illegal status code: " + statusCode);
                    }
                    try {
                        reason = UTF_8.newDecoder().decode(binaryData).toString();
                    } catch (CharacterCodingException e) {
                        throw new FailWebSocketException("Illegal close reason")
                                .initCause(e);
                    }
                }
                output.onClose(statusCode, reason);
                break;
            case PING:
                output.onPing(binaryData);
                binaryData = null;
                break;
            case PONG:
                output.onPong(binaryData);
                binaryData = null;
                break;
            default:
                assert opcode == Opcode.TEXT || opcode == Opcode.BINARY
                        || opcode == Opcode.CONTINUATION : dump(opcode);
                if (fin) {
                    // It is always the last chunk:
                    // either TEXT(FIN=TRUE)/BINARY(FIN=TRUE) or CONT(FIN=TRUE)
                    originatingOpcode = null;
                }
                break;
        }
        payloadLen = 0;
        opcode = null;
    }
}
