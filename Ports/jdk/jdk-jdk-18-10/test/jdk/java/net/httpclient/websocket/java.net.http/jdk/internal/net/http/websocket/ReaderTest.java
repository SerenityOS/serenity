/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

import org.testng.annotations.Test;
import jdk.internal.net.http.websocket.Frame.Opcode;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;

import static java.util.OptionalInt.empty;
import static java.util.OptionalInt.of;
import static org.testng.Assert.assertEquals;
import static jdk.internal.net.http.websocket.TestSupport.assertThrows;
import static jdk.internal.net.http.websocket.TestSupport.forEachBufferPartition;

public class ReaderTest {

    private long cases, frames;

    @Test
    void notMinimalEncoding01() {
        ByteBuffer h = ByteBuffer.allocate(Frame.MAX_HEADER_SIZE_BYTES);
        h.put((byte) 0b1000_0000).put((byte) 0b0111_1110).putChar((char) 125).flip();
        assertThrows(FailWebSocketException.class,
                ".*(?i)minimally-encoded.*",
                () -> new Frame.Reader().readFrame(h, new MockConsumer()));
    }

    @Test
    void notMinimalEncoding02() {
        ByteBuffer h = ByteBuffer.allocate(Frame.MAX_HEADER_SIZE_BYTES);
        h.put((byte) 0b1000_0000).put((byte) 0b0111_1111).putLong(125).flip();
        assertThrows(FailWebSocketException.class,
                ".*(?i)minimally-encoded.*",
                () -> new Frame.Reader().readFrame(h, new MockConsumer()));
    }

    @Test
    void notMinimalEncoding03() {
        ByteBuffer h = ByteBuffer.allocate(Frame.MAX_HEADER_SIZE_BYTES);
        h.put((byte) 0b1000_0000).put((byte) 0b0111_1111).putLong(65535).flip();
        assertThrows(FailWebSocketException.class,
                ".*(?i)minimally-encoded.*",
                () -> new Frame.Reader().readFrame(h, new MockConsumer()));
    }

    @Test
    public void negativePayload() {
        ByteBuffer h = ByteBuffer.allocate(Frame.MAX_HEADER_SIZE_BYTES);
        h.put((byte) 0b1000_0000).put((byte) 0b0111_1111).putLong(-2L).flip();
        assertThrows(FailWebSocketException.class,
                ".*(?i)negative.*",
                () -> new Frame.Reader().readFrame(h, new MockConsumer()));
    }

    @Test
    public void frameStart() {
        final long[] payloads = {0, 126, 65536, Integer.MAX_VALUE + 1L};
        final OptionalInt[] masks = {empty(), of(-1), of(0), of(0xCAFEBABE),
                of(Integer.MAX_VALUE), of(Integer.MIN_VALUE)};
        for (boolean fin : new boolean[]{true, false}) {
            for (boolean rsv1 : new boolean[]{true, false}) {
                for (boolean rsv2 : new boolean[]{true, false}) {
                    for (boolean rsv3 : new boolean[]{true, false}) {
                        for (Opcode opcode : Opcode.values()) {
                            for (long payloadLen : payloads) {
                                for (OptionalInt mask : masks) {
                                    verifyFrameStart(fin, rsv1, rsv2, rsv3, opcode, payloadLen, mask);
                                }
                            }
                        }
                    }
                }
            }
        }
        System.out.println("Frames: " + frames + ", Total cases: " + cases);
    }

    /*
     * Tests whether or not the frame starts properly.
     * That is, a header and the first invocation of payloadData (if any).
     */
    private void verifyFrameStart(boolean fin,
                                  boolean rsv1,
                                  boolean rsv2,
                                  boolean rsv3,
                                  Opcode opcode,
                                  long payloadLen,
                                  OptionalInt mask) {
        frames++;
        Frame.HeaderWriter w = new Frame.HeaderWriter();
        ByteBuffer h = ByteBuffer.allocate(Frame.MAX_HEADER_SIZE_BYTES);
        w.fin(fin).rsv1(rsv1).rsv2(rsv2).rsv3(rsv3).opcode(opcode).payloadLen(payloadLen);
        mask.ifPresentOrElse(w::mask, w::noMask);
        w.write(h);
        h.flip();
        forEachBufferPartition(h,
                buffers -> {
                    cases++;
                    Frame.Reader r = new Frame.Reader();
                    MockConsumer c = new MockConsumer();
                    for (ByteBuffer b : buffers) {
                        r.readFrame(b, c);
                    }
                    assertEquals(fin, c.fin());
                    assertEquals(rsv1, c.rsv1());
                    assertEquals(rsv2, c.rsv2());
                    assertEquals(rsv3, c.rsv3());
                    assertEquals(opcode, c.opcode());
                    assertEquals(mask.isPresent(), c.mask());
                    assertEquals(payloadLen, c.payloadLen());
                    assertEquals(mask, c.maskingKey());
                    assertEquals(payloadLen == 0, c.isEndFrame());
                });
    }

    /*
     * Used to verify the order, the number of invocations as well as the
     * arguments of each individual invocation to Frame.Consumer's methods.
     */
    private static class MockConsumer implements Frame.Consumer {

        private int invocationOrder;

        private Optional<Boolean> fin = Optional.empty();
        private Optional<Boolean> rsv1 = Optional.empty();
        private Optional<Boolean> rsv2 = Optional.empty();
        private Optional<Boolean> rsv3 = Optional.empty();
        private Optional<Opcode> opcode = Optional.empty();
        private Optional<Boolean> mask = Optional.empty();
        private OptionalLong payloadLen = OptionalLong.empty();
        private OptionalInt maskingKey = OptionalInt.empty();

        @Override
        public void fin(boolean value) {
            checkAndSetOrder(0, 1);
            fin = Optional.of(value);
        }

        @Override
        public void rsv1(boolean value) {
            checkAndSetOrder(1, 2);
            rsv1 = Optional.of(value);
        }

        @Override
        public void rsv2(boolean value) {
            checkAndSetOrder(2, 3);
            rsv2 = Optional.of(value);
        }

        @Override
        public void rsv3(boolean value) {
            checkAndSetOrder(3, 4);
            rsv3 = Optional.of(value);
        }

        @Override
        public void opcode(Opcode value) {
            checkAndSetOrder(4, 5);
            opcode = Optional.of(value);
        }

        @Override
        public void mask(boolean value) {
            checkAndSetOrder(5, 6);
            mask = Optional.of(value);
        }

        @Override
        public void payloadLen(long value) {
            checkAndSetOrder(p -> p == 5 || p == 6, n -> 7);
            payloadLen = OptionalLong.of(value);
        }

        @Override
        public void maskingKey(int value) {
            checkAndSetOrder(7, 8);
            maskingKey = of(value);
        }

        @Override
        public void payloadData(ByteBuffer data) {
            checkAndSetOrder(p -> p == 7 || p == 8, n -> 9);
            assert payloadLen.isPresent();
            if (payloadLen.getAsLong() != 0 && !data.hasRemaining()) {
                throw new TestSupport.AssertionFailedException("Artefact of reading");
            }
        }

        @Override
        public void endFrame() {
            checkAndSetOrder(9, 10);
        }

        boolean isEndFrame() {
            return invocationOrder == 10;
        }

        public boolean fin() {
            return fin.get();
        }

        public boolean rsv1() {
            return rsv1.get();
        }

        public boolean rsv2() {
            return rsv2.get();
        }

        public boolean rsv3() {
            return rsv3.get();
        }

        public Opcode opcode() {
            return opcode.get();
        }

        public boolean mask() {
            return mask.get();
        }

        public long payloadLen() {
            return payloadLen.getAsLong();
        }

        public OptionalInt maskingKey() {
            return maskingKey;
        }

        private void checkAndSetOrder(int expectedValue, int newValue) {
            checkAndSetOrder(p -> p == expectedValue, n -> newValue);
        }

        private void checkAndSetOrder(IntPredicate expectedValue,
                                      IntUnaryOperator newValue) {
            if (!expectedValue.test(invocationOrder)) {
                throw new TestSupport.AssertionFailedException(
                        expectedValue + " -> " + newValue);
            }
            invocationOrder = newValue.applyAsInt(invocationOrder);
        }
    }
}
