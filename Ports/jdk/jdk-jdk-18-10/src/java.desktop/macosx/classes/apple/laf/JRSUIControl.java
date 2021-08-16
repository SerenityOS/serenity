/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

package apple.laf;

import java.nio.*;
import java.util.*;

import apple.laf.JRSUIConstants.*;

public final class JRSUIControl {
    private static native int initNativeJRSUI();

    private static native long getPtrOfBuffer(ByteBuffer byteBuffer);
    private static native long getCFDictionary(boolean flipped);
    private static native void disposeCFDictionary(long cfDictionaryPtr);

    private static native int syncChanges(long cfDictionaryPtr, long byteBufferPtr);

//    private static native int paint(long cfDictionaryPtr, long oldProperties, long newProperties, OSXSurfaceData osxsd, double x, double y, double w, double h);
//    private static native int paintChanges(long cfDictionaryPtr, long byteBufferPtr, long oldProperties, long newProperties, OSXSurfaceData osxsd, double x, double y, double w, double h);

    private static native int paintToCGContext                    (long cgContext,    long cfDictionaryPtr, long oldProperties, long newProperties, double x, double y, double w, double h);
    private static native int paintChangesToCGContext            (long cgContext,    long cfDictionaryPtr, long oldProperties, long newProperties, double x, double y, double w, double h, long byteBufferPtr);

    private static native int paintImage        (int[] data, int imgW, int imgH,    long cfDictionaryPtr, long oldProperties, long newProperties, double x, double y, double w, double h);
    private static native int paintChangesImage    (int[] data, int imgW, int imgH,    long cfDictionaryPtr, long oldProperties, long newProperties, double x, double y, double w, double h, long byteBufferPtr);

    private static native int getNativeHitPart(                            long cfDictionaryPtr, long oldProperties, long newProperties, double x, double y, double w, double h, double hitX, double hitY);
    private static native void getNativePartBounds(final double[] rect,    long cfDictionaryPtr, long oldProperties, long newProperties, double x, double y, double w, double h, int part);
    private static native double getNativeScrollBarOffsetChange(        long cfDictionaryPtr, long oldProperties, long newProperties, double x, double y, double w, double h, int offset, int visibleAmount, int extent);

    private static final int INCOHERENT = 2;
    private static final int NOT_INIT = 1;
    private static final int SUCCESS = 0;
    private static final int NULL_PTR = -1;
    private static final int NULL_CG_REF = -2;

    private static int nativeJRSInitialized = NOT_INIT;


    public static void initJRSUI() {
        if (nativeJRSInitialized == SUCCESS) return;
        nativeJRSInitialized = initNativeJRSUI();
        if (nativeJRSInitialized != SUCCESS) throw new RuntimeException("JRSUI could not be initialized (" + nativeJRSInitialized + ").");
    }

    private static final int NIO_BUFFER_SIZE = 128;
    private static class ThreadLocalByteBuffer {
        final ByteBuffer buffer;
        final long ptr;

        public ThreadLocalByteBuffer() {
            buffer = ByteBuffer.allocateDirect(NIO_BUFFER_SIZE);
            buffer.order(ByteOrder.nativeOrder());
            ptr = getPtrOfBuffer(buffer);
        }
    }

    private static final ThreadLocal<ThreadLocalByteBuffer> threadLocal = new ThreadLocal<ThreadLocalByteBuffer>();
    private static ThreadLocalByteBuffer getThreadLocalBuffer() {
        ThreadLocalByteBuffer byteBuffer = threadLocal.get();
        if (byteBuffer != null) return byteBuffer;

        byteBuffer = new ThreadLocalByteBuffer();
        threadLocal.set(byteBuffer);
        return byteBuffer;
    }

    private final HashMap<Key, DoubleValue> nativeMap;
    private final HashMap<Key, DoubleValue> changes;
    private long cfDictionaryPtr;

    private long priorEncodedProperties;
    private long currentEncodedProperties;
    private final boolean flipped;

    public JRSUIControl(final boolean flipped){
        this.flipped = flipped;
        cfDictionaryPtr = getCFDictionary(flipped);
        if (cfDictionaryPtr == 0) throw new RuntimeException("Unable to create native representation");
        nativeMap = new HashMap<Key, DoubleValue>();
        changes = new HashMap<Key, DoubleValue>();
    }

    JRSUIControl(final JRSUIControl other) {
        flipped = other.flipped;
        cfDictionaryPtr = getCFDictionary(flipped);
        if (cfDictionaryPtr == 0) throw new RuntimeException("Unable to create native representation");
        nativeMap = new HashMap<Key, DoubleValue>();
        changes = new HashMap<Key, DoubleValue>(other.nativeMap);
        changes.putAll(other.changes);
    }

    @SuppressWarnings("deprecation")
    protected synchronized void finalize() throws Throwable {
        if (cfDictionaryPtr == 0) return;
        disposeCFDictionary(cfDictionaryPtr);
        cfDictionaryPtr = 0;
    }


    enum BufferState {
        NO_CHANGE,
        ALL_CHANGES_IN_BUFFER,
        SOME_CHANGES_IN_BUFFER,
        CHANGE_WONT_FIT_IN_BUFFER;
    }

    private BufferState loadBufferWithChanges(final ThreadLocalByteBuffer localByteBuffer) {
        final ByteBuffer buffer = localByteBuffer.buffer;
        buffer.rewind();

        for (final JRSUIConstants.Key key : new HashSet<JRSUIConstants.Key>(changes.keySet())) {
            final int changeIndex = buffer.position();
            final JRSUIConstants.DoubleValue value = changes.get(key);

            try {
                buffer.putLong(key.getConstantPtr());
                buffer.put(value.getTypeCode());
                value.putValueInBuffer(buffer);
            } catch (final BufferOverflowException e) {
                return handleBufferOverflow(buffer, changeIndex);
            } catch (final RuntimeException e) {
                System.err.println(this);
                throw e;
            }

            if (buffer.position() >= NIO_BUFFER_SIZE - 8) {
                return handleBufferOverflow(buffer, changeIndex);
            }

            changes.remove(key);
            nativeMap.put(key, value);
        }

        buffer.putLong(0);
        return BufferState.ALL_CHANGES_IN_BUFFER;
    }

    private BufferState handleBufferOverflow(final ByteBuffer buffer, final int changeIndex) {
        if (changeIndex == 0) {
            buffer.putLong(0, 0);
            return BufferState.CHANGE_WONT_FIT_IN_BUFFER;
        }

        buffer.putLong(changeIndex, 0);
        return BufferState.SOME_CHANGES_IN_BUFFER;
    }

    private synchronized void set(final JRSUIConstants.Key key, final JRSUIConstants.DoubleValue value) {
        final JRSUIConstants.DoubleValue existingValue = nativeMap.get(key);

        if (existingValue != null && existingValue.equals(value)) {
            changes.remove(key);
            return;
        }

        changes.put(key, value);
    }

    public void set(final JRSUIState state) {
        state.apply(this);
    }

    void setEncodedState(final long state) {
        currentEncodedProperties = state;
    }

    void set(final JRSUIConstants.Key key, final double value) {
        set(key, new JRSUIConstants.DoubleValue(value));
    }

//    private static final Color blue = new Color(0x00, 0x00, 0xFF, 0x40);
//    private static void paintDebug(Graphics2D g, double x, double y, double w, double h) {
//        final Color prev = g.getColor();
//        g.setColor(blue);
//        g.drawRect((int)x, (int)y, (int)w, (int)h);
//        g.setColor(prev);
//    }

//    private static int paintsWithNoChange = 0;
//    private static int paintsWithChangesThatFit = 0;
//    private static int paintsWithChangesThatOverflowed = 0;

    public void paint(final int[] data, final int imgW, final int imgH, final double x, final double y, final double w, final double h) {
        paintImage(data, imgW, imgH, x, y, w, h);
        priorEncodedProperties = currentEncodedProperties;
    }

    private synchronized int paintImage(final int[] data, final int imgW, final int imgH, final double x, final double y, final double w, final double h) {
        if (changes.isEmpty()) {
//            paintsWithNoChange++;
            return paintImage(data, imgW, imgH, cfDictionaryPtr, priorEncodedProperties, currentEncodedProperties, x, y, w, h);
        }

        final ThreadLocalByteBuffer localByteBuffer = getThreadLocalBuffer();
        BufferState bufferState = loadBufferWithChanges(localByteBuffer);

        // fast tracking this, since it's the likely scenario
        if (bufferState == BufferState.ALL_CHANGES_IN_BUFFER) {
//            paintsWithChangesThatFit++;
            return paintChangesImage(data, imgW, imgH, cfDictionaryPtr, priorEncodedProperties, currentEncodedProperties, x, y, w, h, localByteBuffer.ptr);
        }

        while (bufferState == BufferState.SOME_CHANGES_IN_BUFFER) {
            final int status = syncChanges(cfDictionaryPtr, localByteBuffer.ptr);
            if (status != SUCCESS) throw new RuntimeException("JRSUI failed to sync changes into the native buffer: " + this);
            bufferState = loadBufferWithChanges(localByteBuffer);
        }

        if (bufferState == BufferState.CHANGE_WONT_FIT_IN_BUFFER) {
            throw new RuntimeException("JRSUI failed to sync changes to the native buffer, because some change was too big: " + this);
        }

        // implicitly ALL_CHANGES_IN_BUFFER, now that we sync'd the buffer down to native a few times
//        paintsWithChangesThatOverflowed++;
        return paintChangesImage(data, imgW, imgH, cfDictionaryPtr, priorEncodedProperties, currentEncodedProperties, x, y, w, h, localByteBuffer.ptr);
    }

    public void paint(final long cgContext, final double x, final double y, final double w, final double h) {
        paintToCGContext(cgContext, x, y, w, h);
        priorEncodedProperties = currentEncodedProperties;
    }

    private synchronized int paintToCGContext(final long cgContext, final double x, final double y, final double w, final double h) {
        if (changes.isEmpty()) {
//            paintsWithNoChange++;
            return paintToCGContext(cgContext, cfDictionaryPtr, priorEncodedProperties, currentEncodedProperties, x, y, w, h);
        }

        final ThreadLocalByteBuffer localByteBuffer = getThreadLocalBuffer();
        BufferState bufferState = loadBufferWithChanges(localByteBuffer);

        // fast tracking this, since it's the likely scenario
        if (bufferState == BufferState.ALL_CHANGES_IN_BUFFER) {
//            paintsWithChangesThatFit++;
            return paintChangesToCGContext(cgContext, cfDictionaryPtr, priorEncodedProperties, currentEncodedProperties, x, y, w, h, localByteBuffer.ptr);
        }

        while (bufferState == BufferState.SOME_CHANGES_IN_BUFFER) {
            final int status = syncChanges(cfDictionaryPtr, localByteBuffer.ptr);
            if (status != SUCCESS) throw new RuntimeException("JRSUI failed to sync changes into the native buffer: " + this);
            bufferState = loadBufferWithChanges(localByteBuffer);
        }

        if (bufferState == BufferState.CHANGE_WONT_FIT_IN_BUFFER) {
            throw new RuntimeException("JRSUI failed to sync changes to the native buffer, because some change was too big: " + this);
        }

        // implicitly ALL_CHANGES_IN_BUFFER, now that we sync'd the buffer down to native a few times
//        paintsWithChangesThatOverflowed++;
        return paintChangesToCGContext(cgContext, cfDictionaryPtr, priorEncodedProperties, currentEncodedProperties, x, y, w, h, localByteBuffer.ptr);
    }


    Hit getHitForPoint(final int x, final int y, final int w, final int h, final int hitX, final int hitY) {
        sync();
        // reflect hitY about the midline of the control before sending to native
        final Hit hit = JRSUIConstants.getHit(getNativeHitPart(cfDictionaryPtr, priorEncodedProperties, currentEncodedProperties, x, y, w, h, hitX, 2 * y + h - hitY));
        priorEncodedProperties = currentEncodedProperties;
        return hit;
    }

    void getPartBounds(final double[] rect, final int x, final int y, final int w, final int h, final int part) {
        if (rect == null) throw new NullPointerException("Cannot load null rect");
        if (rect.length != 4) throw new IllegalArgumentException("Rect must have four elements");

        sync();
        getNativePartBounds(rect, cfDictionaryPtr, priorEncodedProperties, currentEncodedProperties, x, y, w, h, part);
        priorEncodedProperties = currentEncodedProperties;
    }

    double getScrollBarOffsetChange(final int x, final int y, final int w, final int h, final int offset, final int visibleAmount, final int extent) {
        sync();
        final double offsetChange = getNativeScrollBarOffsetChange(cfDictionaryPtr, priorEncodedProperties, currentEncodedProperties, x, y, w, h, offset, visibleAmount, extent);
        priorEncodedProperties = currentEncodedProperties;
        return offsetChange;
    }

    private void sync() {
        if (changes.isEmpty()) return;

        final ThreadLocalByteBuffer localByteBuffer = getThreadLocalBuffer();
        BufferState bufferState = loadBufferWithChanges(localByteBuffer);
        if (bufferState == BufferState.ALL_CHANGES_IN_BUFFER) {
            final int status = syncChanges(cfDictionaryPtr, localByteBuffer.ptr);
            if (status != SUCCESS) throw new RuntimeException("JRSUI failed to sync changes into the native buffer: " + this);
            return;
        }

        while (bufferState == BufferState.SOME_CHANGES_IN_BUFFER) {
            final int status = syncChanges(cfDictionaryPtr, localByteBuffer.ptr);
            if (status != SUCCESS) throw new RuntimeException("JRSUI failed to sync changes into the native buffer: " + this);
            bufferState = loadBufferWithChanges(localByteBuffer);
        }

        if (bufferState == BufferState.CHANGE_WONT_FIT_IN_BUFFER) {
            throw new RuntimeException("JRSUI failed to sync changes to the native buffer, because some change was too big: " + this);
        }
    }

    @Override
    public int hashCode() {
        int bits = (int)(currentEncodedProperties ^ (currentEncodedProperties >>> 32));
        bits ^= nativeMap.hashCode();
        bits ^= changes.hashCode();
        return bits;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof JRSUIControl)) return false;
        final JRSUIControl other = (JRSUIControl)obj;
        if (currentEncodedProperties != other.currentEncodedProperties) return false;
        if (!nativeMap.equals(other.nativeMap)) return false;
        if (!changes.equals(other.changes)) return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder("JRSUIControl[inNative:");
        builder.append(Arrays.toString(nativeMap.entrySet().toArray()));
        builder.append(", changes:");
        builder.append(Arrays.toString(changes.entrySet().toArray()));
        builder.append("]");
        return builder.toString();
    }
}
