/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.internal;

import jdk.internal.misc.Unsafe;
import jdk.jfr.internal.consumer.StringParser;

/**
 * Class must reside in a package with package restriction.
 *
 * Users should not have direct access to underlying memory.
 *
 */
public final class EventWriter {

    // Event may not exceed size for a padded integer
    private static final long MAX_EVENT_SIZE = (1 << 28) -1;
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static final JVM jvm = JVM.getJVM();

    // The JVM needs access to these values. Don't remove
    private final long threadID;
    private long startPosition;
    private long startPositionAddress;
    private long currentPosition;
    private long maxPosition;
    private boolean valid;
    boolean notified; // Not private to avoid being optimized away

    private PlatformEventType eventType;
    private boolean started;
    private boolean flushOnEnd;
    private boolean largeSize = false;

    public static EventWriter getEventWriter() {
        EventWriter ew = (EventWriter)JVM.getEventWriter();
        return ew != null ? ew : JVM.newEventWriter();
    }

    public void putBoolean(boolean i) {
        if (isValidForSize(Byte.BYTES)) {
            currentPosition += Bits.putBoolean(currentPosition, i);
        }
    }

    public void putByte(byte i) {
        if (isValidForSize(Byte.BYTES)) {
            unsafe.putByte(currentPosition, i);
            ++currentPosition;
        }
    }

    public void putChar(char v) {
        if (isValidForSize(Character.BYTES + 1)) {
            putUncheckedLong(v);
        }
    }

    private void putUncheckedChar(char v) {
        putUncheckedLong(v);
    }

    public void putShort(short v) {
        if (isValidForSize(Short.BYTES + 1)) {
            putUncheckedLong(v & 0xFFFF);
        }
    }

    public void putInt(int v) {
        if (isValidForSize(Integer.BYTES + 1)) {
            putUncheckedLong(v & 0x00000000ffffffffL);
        }
    }

    private void putUncheckedInt(int v) {
        putUncheckedLong(v & 0x00000000ffffffffL);
    }

    public void putFloat(float i) {
        if (isValidForSize(Float.BYTES)) {
            currentPosition += Bits.putFloat(currentPosition, i);
        }
    }

    public void putLong(long v) {
        if (isValidForSize(Long.BYTES + 1)) {
            putUncheckedLong(v);
        }
    }

    public void putDouble(double i) {
        if (isValidForSize(Double.BYTES)) {
            currentPosition += Bits.putDouble(currentPosition, i);
        }
    }

    public void putString(String s, StringPool pool) {
        if (s == null) {
            putByte(StringParser.Encoding.NULL.byteValue());
            return;
        }
        int length = s.length();
        if (length == 0) {
            putByte(StringParser.Encoding.EMPTY_STRING.byteValue());
            return;
        }
        if (length > StringPool.MIN_LIMIT && length < StringPool.MAX_LIMIT) {
            long l = StringPool.addString(s);
            if (l > 0) {
                putByte(StringParser.Encoding.CONSTANT_POOL.byteValue());
                putLong(l);
                return;
            }
        }
        putStringValue(s);
        return;
    }

    private void putStringValue(String s) {
        int length = s.length();
        if (isValidForSize(1 + 5 + 3 * length)) {
            putUncheckedByte(StringParser.Encoding.CHAR_ARRAY.byteValue()); // 1 byte
            putUncheckedInt(length); // max 5 bytes
            for (int i = 0; i < length; i++) {
                putUncheckedChar(s.charAt(i)); // max 3 bytes
            }
        }
    }

    public void putEventThread() {
        putLong(threadID);
    }

    public void putThread(Thread athread) {
        if (athread == null) {
            putLong(0L);
        } else {
            putLong(jvm.getThreadId(athread));
        }
    }

    public void putClass(Class<?> aClass) {
        if (aClass == null) {
            putLong(0L);
        } else {
            putLong(JVM.getClassId(aClass));
        }
    }

    public void putStackTrace() {
        if (eventType.getStackTraceEnabled()) {
            putLong(jvm.getStackTraceId(eventType.getStackTraceOffset()));
        } else {
            putLong(0L);
        }
    }

    private void reserveEventSizeField() {
        this.largeSize = eventType.isLargeSize();
        if (largeSize) {
            if (isValidForSize(Integer.BYTES)) {
                currentPosition +=  Integer.BYTES;
            }
        } else {
            if (isValidForSize(1)) {
                currentPosition += 1;
            }
        }
    }

    public void reset() {
        currentPosition = startPosition;
        if (flushOnEnd) {
            flushOnEnd = flush();
        }
        valid = true;
        started = false;
    }

    private boolean isValidForSize(int requestedSize) {
        if (!valid) {
            return false;
        }
        if (currentPosition + requestedSize > maxPosition) {
            flushOnEnd = flush(usedSize(), requestedSize);
            // retry
            if (!valid) {
                return false;
            }
        }
        return true;
    }

    private boolean isNotified() {
        return notified;
    }

    private void resetNotified() {
        notified = false;
    }

    private void resetStringPool() {
        StringPool.reset();
    }

    private int usedSize() {
        return (int) (currentPosition - startPosition);
    }

    private boolean flush() {
        return flush(usedSize(), 0);
    }

    private boolean flush(int usedSize, int requestedSize) {
        return JVM.flush(this, usedSize, requestedSize);
    }

    public boolean beginEvent(PlatformEventType eventType) {
        if (started) {
            // recursive write attempt
            return false;
        }
        started = true;
        this.eventType = eventType;
        reserveEventSizeField();
        putLong(eventType.getId());
        return true;
    }

    public boolean endEvent() {
        if (!valid) {
            reset();
            return true;
        }
        final int eventSize = usedSize();
        if (eventSize > MAX_EVENT_SIZE) {
            reset();
            return true;
        }

        if (largeSize) {
            Bits.putInt(startPosition, makePaddedInt(eventSize));
        } else {
            if (eventSize < 128) {
                Bits.putByte(startPosition, (byte) eventSize);
            } else {
                eventType.setLargeSize();
                reset();
                // returning false will trigger restart of the
                // event write attempt
                return false;
            }
        }

        if (isNotified()) {
            resetNotified();
            resetStringPool();
            reset();
            // returning false will trigger restart of the event write attempt
            return false;
        }
        startPosition = currentPosition;
        unsafe.storeStoreFence();
        unsafe.putAddress(startPositionAddress, currentPosition);
        // the event is now committed
        if (flushOnEnd) {
            flushOnEnd = flush();
        }
        started = false;
        return true;
    }

    private EventWriter(long startPos, long maxPos, long startPosAddress, long threadID, boolean valid) {
        startPosition = currentPosition = startPos;
        maxPosition = maxPos;
        startPositionAddress = startPosAddress;
        this.threadID = threadID;
        started = false;
        flushOnEnd = false;
        this.valid = valid;
        notified = false;
    }

    private static int makePaddedInt(int v) {
        // bit  0-6 + pad => bit 24 - 31
        long b1 = (((v >>> 0) & 0x7F) | 0x80) << 24;

        // bit  7-13 + pad => bit 16 - 23
        long b2 = (((v >>> 7) & 0x7F) | 0x80) << 16;

        // bit 14-20 + pad => bit  8 - 15
        long b3 = (((v >>> 14) & 0x7F) | 0x80) << 8;

        // bit 21-28       => bit  0 -  7
        long b4 = (((v >>> 21) & 0x7F)) << 0;

        return (int) (b1 + b2 + b3 + b4);
    }

    private void putUncheckedLong(long v) {
        if ((v & ~0x7FL) == 0L) {
            putUncheckedByte((byte) v); // 0-6
            return;
        }
        putUncheckedByte((byte) (v | 0x80L)); // 0-6
        v >>>= 7;
        if ((v & ~0x7FL) == 0L) {
            putUncheckedByte((byte) v); // 7-13
            return;
        }
        putUncheckedByte((byte) (v | 0x80L)); // 7-13
        v >>>= 7;
        if ((v & ~0x7FL) == 0L) {
            putUncheckedByte((byte) v); // 14-20
            return;
        }
        putUncheckedByte((byte) (v | 0x80L)); // 14-20
        v >>>= 7;
        if ((v & ~0x7FL) == 0L) {
            putUncheckedByte((byte) v); // 21-27
            return;
        }
        putUncheckedByte((byte) (v | 0x80L)); // 21-27
        v >>>= 7;
        if ((v & ~0x7FL) == 0L) {
            putUncheckedByte((byte) v); // 28-34
            return;
        }
        putUncheckedByte((byte) (v | 0x80L)); // 28-34
        v >>>= 7;
        if ((v & ~0x7FL) == 0L) {
            putUncheckedByte((byte) v); // 35-41
            return;
        }
        putUncheckedByte((byte) (v | 0x80L)); // 35-41
        v >>>= 7;
        if ((v & ~0x7FL) == 0L) {
            putUncheckedByte((byte) v); // 42-48
            return;
        }
        putUncheckedByte((byte) (v | 0x80L)); // 42-48
        v >>>= 7;

        if ((v & ~0x7FL) == 0L) {
            putUncheckedByte((byte) v); // 49-55
            return;
        }
        putUncheckedByte((byte) (v | 0x80L)); // 49-55
        putUncheckedByte((byte) (v >>> 7)); // 56-63, last byte as is.
    }

    private void putUncheckedByte(byte i) {
        unsafe.putByte(currentPosition, i);
        ++currentPosition;
    }
}
