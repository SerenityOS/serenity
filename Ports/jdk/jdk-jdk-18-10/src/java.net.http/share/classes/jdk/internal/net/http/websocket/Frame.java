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

import java.nio.ByteBuffer;

import static jdk.internal.net.http.common.Utils.dump;
import static jdk.internal.net.http.websocket.Frame.Opcode.ofCode;

/*
 * A collection of utilities for reading, writing, and masking frames.
 */
final class Frame {

    private Frame() { }

    static final int MAX_HEADER_SIZE_BYTES = 2 + 8 + 4;
    static final int MAX_CONTROL_FRAME_PAYLOAD_LENGTH = 125;

    enum Opcode {

        CONTINUATION   (0x0),
        TEXT           (0x1),
        BINARY         (0x2),
        NON_CONTROL_0x3(0x3),
        NON_CONTROL_0x4(0x4),
        NON_CONTROL_0x5(0x5),
        NON_CONTROL_0x6(0x6),
        NON_CONTROL_0x7(0x7),
        CLOSE          (0x8),
        PING           (0x9),
        PONG           (0xA),
        CONTROL_0xB    (0xB),
        CONTROL_0xC    (0xC),
        CONTROL_0xD    (0xD),
        CONTROL_0xE    (0xE),
        CONTROL_0xF    (0xF);

        private static final Opcode[] opcodes;

        static {
            Opcode[] values = values();
            opcodes = new Opcode[values.length];
            for (Opcode c : values) {
                opcodes[c.code] = c;
            }
        }

        private final byte code;

        Opcode(int code) {
            this.code = (byte) code;
        }

        boolean isControl() {
            return (code & 0x8) != 0;
        }

        static Opcode ofCode(int code) {
            return opcodes[code & 0xF];
        }
    }

    /*
     * A utility for masking frame payload data.
     */
    static final class Masker {

        // Exploiting ByteBuffer's ability to read/write multi-byte integers
        private final ByteBuffer acc = ByteBuffer.allocate(8);
        private final int[] maskBytes = new int[4];
        private int offset;
        private long maskLong;

        /*
         * Reads all remaining bytes from the given input buffer, masks them
         * with the supplied mask and writes the resulting bytes to the given
         * output buffer.
         *
         * The source and the destination buffers may be the same instance.
         */
        static void transferMasking(ByteBuffer src, ByteBuffer dst, int mask) {
            if (src.remaining() > dst.remaining()) {
                throw new IllegalArgumentException(dump(src, dst));
            }
            new Masker().mask(mask).transferMasking(src, dst);
        }

        /*
         * Clears this instance's state and sets the mask.
         *
         * The behaviour is as if the mask was set on a newly created instance.
         */
        Masker mask(int value) {
            acc.clear().putInt(value).putInt(value).flip();
            for (int i = 0; i < maskBytes.length; i++) {
                maskBytes[i] = acc.get(i);
            }
            offset = 0;
            maskLong = acc.getLong(0);
            return this;
        }

        /*
         * Reads as many remaining bytes as possible from the given input
         * buffer, masks them with the previously set mask and writes the
         * resulting bytes to the given output buffer.
         *
         * The source and the destination buffers may be the same instance. If
         * the mask hasn't been previously set it is assumed to be 0.
         */
        Masker transferMasking(ByteBuffer src, ByteBuffer dst) {
            begin(src, dst);
            loop(src, dst);
            end(src, dst);
            return this;
        }

        /*
         * Applies up to 3 remaining from the previous pass bytes of the mask.
         */
        private void begin(ByteBuffer src, ByteBuffer dst) {
            if (offset == 0) { // No partially applied mask from the previous invocation
                return;
            }
            int i = src.position(), j = dst.position();
            final int srcLim = src.limit(), dstLim = dst.limit();
            for (; offset < 4 && i < srcLim && j < dstLim; i++, j++, offset++)
            {
                dst.put(j, (byte) (src.get(i) ^ maskBytes[offset]));
            }
            offset &= 3; // Will become 0 if the mask has been fully applied
            src.position(i);
            dst.position(j);
        }

        /*
         * Gallops one long (mask + mask) at a time.
         */
        private void loop(ByteBuffer src, ByteBuffer dst) {
            int i = src.position();
            int j = dst.position();
            final int srcLongLim = src.limit() - 7, dstLongLim = dst.limit() - 7;
            for (; i < srcLongLim && j < dstLongLim; i += 8, j += 8) {
                dst.putLong(j, src.getLong(i) ^ maskLong);
            }
            if (i > src.limit()) {
                src.position(i - 8);
            } else {
                src.position(i);
            }
            if (j > dst.limit()) {
                dst.position(j - 8);
            } else {
                dst.position(j);
            }
        }

        /*
         * Applies up to 7 remaining from the "galloping" phase bytes of the
         * mask.
         */
        private void end(ByteBuffer src, ByteBuffer dst) {
            assert Math.min(src.remaining(), dst.remaining()) < 8;
            final int srcLim = src.limit(), dstLim = dst.limit();
            int i = src.position(), j = dst.position();
            for (; i < srcLim && j < dstLim;
                 i++, j++, offset = (offset + 1) & 3) // offset cycles through 0..3
            {
                dst.put(j, (byte) (src.get(i) ^ maskBytes[offset]));
            }
            src.position(i);
            dst.position(j);
        }
    }

    /*
     * A builder-style writer of frame headers.
     *
     * The writer does not enforce any protocol-level rules, it simply writes a
     * header structure to the given buffer. The order of calls to intermediate
     * methods is NOT significant.
     */
    static final class HeaderWriter {

        private char firstChar;
        private long payloadLen;
        private int maskingKey;
        private boolean mask;

        HeaderWriter fin(boolean value) {
            if (value) {
                firstChar |=  0b10000000_00000000;
            } else {
                firstChar &= ~0b10000000_00000000;
            }
            return this;
        }

        HeaderWriter rsv1(boolean value) {
            if (value) {
                firstChar |=  0b01000000_00000000;
            } else {
                firstChar &= ~0b01000000_00000000;
            }
            return this;
        }

        HeaderWriter rsv2(boolean value) {
            if (value) {
                firstChar |=  0b00100000_00000000;
            } else {
                firstChar &= ~0b00100000_00000000;
            }
            return this;
        }

        HeaderWriter rsv3(boolean value) {
            if (value) {
                firstChar |=  0b00010000_00000000;
            } else {
                firstChar &= ~0b00010000_00000000;
            }
            return this;
        }

        HeaderWriter opcode(Opcode value) {
            firstChar = (char) ((firstChar & 0xF0FF) | (value.code << 8));
            return this;
        }

        HeaderWriter payloadLen(long value) {
            if (value < 0) {
                throw new IllegalArgumentException("Negative: " + value);
            }
            payloadLen = value;
            firstChar &= 0b11111111_10000000; // Clear previous payload length leftovers
            if (payloadLen < 126) {
                firstChar |= payloadLen;
            } else if (payloadLen < 65536) {
                firstChar |= 126;
            } else {
                firstChar |= 127;
            }
            return this;
        }

        HeaderWriter mask(int value) {
            firstChar |= 0b00000000_10000000;
            maskingKey = value;
            mask = true;
            return this;
        }

        HeaderWriter noMask() {
            firstChar &= ~0b00000000_10000000;
            mask = false;
            return this;
        }

        /*
         * Writes the header to the given buffer.
         *
         * The buffer must have at least MAX_HEADER_SIZE_BYTES remaining. The
         * buffer's position is incremented by the number of bytes written.
         */
        void write(ByteBuffer buffer) {
            buffer.putChar(firstChar);
            if (payloadLen >= 126) {
                if (payloadLen < 65536) {
                    buffer.putChar((char) payloadLen);
                } else {
                    buffer.putLong(payloadLen);
                }
            }
            if (mask) {
                buffer.putInt(maskingKey);
            }
        }
    }

    /*
     * A consumer of frame parts.
     *
     * Frame.Reader invokes the consumer's methods in the following order:
     *
     *     fin rsv1 rsv2 rsv3 opcode mask payloadLength maskingKey? payloadData+ endFrame
     */
    interface Consumer {

        void fin(boolean value);

        void rsv1(boolean value);

        void rsv2(boolean value);

        void rsv3(boolean value);

        void opcode(Opcode value);

        void mask(boolean value);

        void payloadLen(long value);

        void maskingKey(int value);

        /*
         * Called by the Frame.Reader when a part of the (or a complete) payload
         * is ready to be consumed.
         *
         * The sum of numbers of bytes consumed in each invocation of this
         * method corresponding to the given frame WILL be equal to
         * 'payloadLen', reported to `void payloadLen(long value)` before that.
         *
         * In particular, if `payloadLen` is 0, then there WILL be a single
         * invocation to this method.
         *
         * No unmasking is done.
         */
        void payloadData(ByteBuffer data);

        void endFrame();
    }

    /*
     * A Reader of frames.
     *
     * No protocol-level rules are checked.
     */
    static final class Reader {

        private static final int AWAITING_FIRST_BYTE  =  1;
        private static final int AWAITING_SECOND_BYTE =  2;
        private static final int READING_16_LENGTH    =  4;
        private static final int READING_64_LENGTH    =  8;
        private static final int READING_MASK         = 16;
        private static final int READING_PAYLOAD      = 32;

        // Exploiting ByteBuffer's ability to read multi-byte integers
        private final ByteBuffer accumulator = ByteBuffer.allocate(8);
        private int state = AWAITING_FIRST_BYTE;
        private boolean mask;
        private long remainingPayloadLength;

        /*
         * Reads at most one frame from the given buffer invoking the consumer's
         * methods corresponding to the frame parts found.
         *
         * As much of the frame's payload, if any, is read. The buffer's
         * position is updated to reflect the number of bytes read.
         *
         * Throws FailWebSocketException if detects the frame is malformed.
         */
        void readFrame(ByteBuffer input, Consumer consumer) {
            loop:
            while (true) {
                byte b;
                switch (state) {
                    case AWAITING_FIRST_BYTE:
                        if (!input.hasRemaining()) {
                            break loop;
                        }
                        b = input.get();
                        consumer.fin( (b & 0b10000000) != 0);
                        consumer.rsv1((b & 0b01000000) != 0);
                        consumer.rsv2((b & 0b00100000) != 0);
                        consumer.rsv3((b & 0b00010000) != 0);
                        consumer.opcode(ofCode(b));
                        state = AWAITING_SECOND_BYTE;
                        continue loop;
                    case AWAITING_SECOND_BYTE:
                        if (!input.hasRemaining()) {
                            break loop;
                        }
                        b = input.get();
                        consumer.mask(mask = (b & 0b10000000) != 0);
                        byte p1 = (byte) (b & 0b01111111);
                        if (p1 < 126) {
                            assert p1 >= 0 : p1;
                            consumer.payloadLen(remainingPayloadLength = p1);
                            state = mask ? READING_MASK : READING_PAYLOAD;
                        } else if (p1 < 127) {
                            state = READING_16_LENGTH;
                        } else {
                            state = READING_64_LENGTH;
                        }
                        continue loop;
                    case READING_16_LENGTH:
                        if (!input.hasRemaining()) {
                            break loop;
                        }
                        b = input.get();
                        if (accumulator.put(b).position() < 2) {
                            continue loop;
                        }
                        remainingPayloadLength = accumulator.flip().getChar();
                        if (remainingPayloadLength < 126) {
                            throw notMinimalEncoding(remainingPayloadLength);
                        }
                        consumer.payloadLen(remainingPayloadLength);
                        accumulator.clear();
                        state = mask ? READING_MASK : READING_PAYLOAD;
                        continue loop;
                    case READING_64_LENGTH:
                        if (!input.hasRemaining()) {
                            break loop;
                        }
                        b = input.get();
                        if (accumulator.put(b).position() < 8) {
                            continue loop;
                        }
                        remainingPayloadLength = accumulator.flip().getLong();
                        if (remainingPayloadLength < 0) {
                            throw negativePayload(remainingPayloadLength);
                        } else if (remainingPayloadLength < 65536) {
                            throw notMinimalEncoding(remainingPayloadLength);
                        }
                        consumer.payloadLen(remainingPayloadLength);
                        accumulator.clear();
                        state = mask ? READING_MASK : READING_PAYLOAD;
                        continue loop;
                    case READING_MASK:
                        if (!input.hasRemaining()) {
                            break loop;
                        }
                        b = input.get();
                        if (accumulator.put(b).position() != 4) {
                            continue loop;
                        }
                        consumer.maskingKey(accumulator.flip().getInt());
                        accumulator.clear();
                        state = READING_PAYLOAD;
                        continue loop;
                    case READING_PAYLOAD:
                        // This state does not require any bytes to be available
                        // in the input buffer in order to proceed
                        int deliverable = (int) Math.min(remainingPayloadLength,
                                                         input.remaining());
                        int oldLimit = input.limit();
                        input.limit(input.position() + deliverable);
                        if (deliverable != 0 || remainingPayloadLength == 0) {
                            consumer.payloadData(input);
                        }
                        int consumed = deliverable - input.remaining();
                        if (consumed < 0) {
                            // Consumer cannot consume more than there was available
                            throw new InternalError();
                        }
                        input.limit(oldLimit);
                        remainingPayloadLength -= consumed;
                        if (remainingPayloadLength == 0) {
                            consumer.endFrame();
                            state = AWAITING_FIRST_BYTE;
                        }
                        break loop;
                    default:
                        throw new InternalError(String.valueOf(state));
                }
            }
        }

        private static FailWebSocketException negativePayload(long payloadLength)
        {
            return new FailWebSocketException(
                    "Negative payload length: " + payloadLength);
        }

        private static FailWebSocketException notMinimalEncoding(long payloadLength)
        {
            return new FailWebSocketException(
                    "Not minimally-encoded payload length:" + payloadLength);
        }
    }
}
