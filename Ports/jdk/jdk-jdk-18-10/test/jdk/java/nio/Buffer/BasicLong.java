/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

/* Type-specific source code for unit test
 *
 * Regenerate the BasicX classes via genBasic.sh whenever this file changes.
 * We check in the generated source files so that the test tree can be used
 * independently of the rest of the source tree.
 */

// -- This file was mechanically generated: Do not edit! -- //





import java.nio.*;








public class BasicLong
    extends Basic
{

    private static final long[] VALUES = {
        Long.MIN_VALUE,
        (long) -1,
        (long) 0,
        (long) 1,
        Long.MAX_VALUE,












    };

    private static void relGet(LongBuffer b) {
        int n = b.capacity();
        for (int i = 0; i < n; i++)
            ck(b, (long)b.get(), (long)((long)ic(i)));
        b.rewind();
    }

    private static void relGet(LongBuffer b, int start) {
        int n = b.remaining();
        for (int i = start; i < n; i++)
            ck(b, (long)b.get(), (long)((long)ic(i)));
        b.rewind();
    }

    private static void absGet(LongBuffer b) {
        int n = b.capacity();
        for (int i = 0; i < n; i++)
            ck(b, (long)b.get(), (long)((long)ic(i)));
        b.rewind();
    }

    private static void bulkGet(LongBuffer b) {
        int n = b.capacity();
        long[] a = new long[n + 7];
        b.get(a, 7, n);
        for (int i = 0; i < n; i++) {
            ck(b, (long)a[i + 7], (long)((long)ic(i)));
        }
    }

    private static void absBulkGet(LongBuffer b) {
        int n = b.capacity();
        int len = n - 7*2;
        long[] a = new long[n + 7];
        b.position(42);
        b.get(7, a, 7, len);
        ck(b, b.position() == 42);
        for (int i = 0; i < len; i++) {
            ck(b, (long)a[i + 7], (long)((long)ic(i)));
        }
    }

    private static void relPut(LongBuffer b) {
        int n = b.capacity();
        b.clear();
        for (int i = 0; i < n; i++)
            b.put((long)ic(i));
        b.flip();
    }

    private static void absPut(LongBuffer b) {
        int n = b.capacity();
        b.clear();
        for (int i = 0; i < n; i++)
            b.put(i, (long)ic(i));
        b.limit(n);
        b.position(0);
    }

    private static void bulkPutArray(LongBuffer b) {
        int n = b.capacity();
        b.clear();
        long[] a = new long[n + 7];
        for (int i = 0; i < n; i++)
            a[i + 7] = (long)ic(i);
        b.put(a, 7, n);
        b.flip();
    }

    private static void bulkPutBuffer(LongBuffer b) {
        int n = b.capacity();
        b.clear();
        LongBuffer c = LongBuffer.allocate(n + 7);
        c.position(7);
        for (int i = 0; i < n; i++)
            c.put((long)ic(i));
        c.flip();
        c.position(7);
        b.put(c);
        b.flip();
        try {
            b.put(b);
            fail("IllegalArgumentException expected for put into same buffer");
        } catch (IllegalArgumentException e) {
            if (e.getMessage() == null) {
                fail("Non-null IllegalArgumentException message expected from"
                     + " put into same buffer");
            }
        }
    }

    private static void absBulkPutArray(LongBuffer b) {
        int n = b.capacity();
        b.clear();
        int lim = n - 7;
        int len = lim - 7;
        b.limit(lim);
        long[] a = new long[len + 7];
        for (int i = 0; i < len; i++)
            a[i + 7] = (long)ic(i);
        b.position(42);
        b.put(7, a, 7, len);
        ck(b, b.position() == 42);
    }

    //6231529
    private static void callReset(LongBuffer b) {
        b.position(0);
        b.mark();

        b.duplicate().reset();
        b.asReadOnlyBuffer().reset();
    }



    // 6221101-6234263

    private static void putBuffer() {
        final int cap = 10;

        LongBuffer direct1 = ByteBuffer.allocateDirect(cap).asLongBuffer();
        LongBuffer nondirect1 = ByteBuffer.allocate(cap).asLongBuffer();
        direct1.put(nondirect1);

        LongBuffer direct2 = ByteBuffer.allocateDirect(cap).asLongBuffer();
        LongBuffer nondirect2 = ByteBuffer.allocate(cap).asLongBuffer();
        nondirect2.put(direct2);

        LongBuffer direct3 = ByteBuffer.allocateDirect(cap).asLongBuffer();
        LongBuffer direct4 = ByteBuffer.allocateDirect(cap).asLongBuffer();
        direct3.put(direct4);

        LongBuffer nondirect3 = ByteBuffer.allocate(cap).asLongBuffer();
        LongBuffer nondirect4 = ByteBuffer.allocate(cap).asLongBuffer();
        nondirect3.put(nondirect4);
    }

















    private static void checkSlice(LongBuffer b, LongBuffer slice) {
        ck(slice, 0, slice.position());
        ck(slice, b.remaining(), slice.limit());
        ck(slice, b.remaining(), slice.capacity());
        if (b.isDirect() != slice.isDirect())
            fail("Lost direction", slice);
        if (b.isReadOnly() != slice.isReadOnly())
            fail("Lost read-only", slice);
    }































































































































































































































































































































    private static void fail(String problem,
                             LongBuffer xb, LongBuffer yb,
                             long x, long y) {
        fail(problem + String.format(": x=%s y=%s", x, y), xb, yb);
    }

    private static void catchNullArgument(Buffer b, Runnable thunk) {
        tryCatch(b, NullPointerException.class, thunk);
    }

    private static void catchIllegalArgument(Buffer b, Runnable thunk) {
        tryCatch(b, IllegalArgumentException.class, thunk);
    }

    private static void catchReadOnlyBuffer(Buffer b, Runnable thunk) {
        tryCatch(b, ReadOnlyBufferException.class, thunk);
    }

    private static void catchIndexOutOfBounds(Buffer b, Runnable thunk) {
        tryCatch(b, IndexOutOfBoundsException.class, thunk);
    }

    private static void catchIndexOutOfBounds(long[] t, Runnable thunk) {
        tryCatch(t, IndexOutOfBoundsException.class, thunk);
    }

    private static void tryCatch(Buffer b, Class<?> ex, Runnable thunk) {
        boolean caught = false;
        try {
            thunk.run();
        } catch (Throwable x) {
            if (ex.isAssignableFrom(x.getClass())) {
                caught = true;
            } else {
                String s = x.getMessage();
                if (s == null)
                    s = x.getClass().getName();
                fail(s + " not expected");
            }
        }
        if (!caught) {
            fail(ex.getName() + " not thrown", b);
        }
    }

    private static void tryCatch(long[] t, Class<?> ex, Runnable thunk) {
        tryCatch(LongBuffer.wrap(t), ex, thunk);
    }

    public static void test(int level, final LongBuffer b, boolean direct) {

        show(level, b);

        if (direct != b.isDirect())
            fail("Wrong direction", b);

        // Gets and puts

        relPut(b);
        relGet(b);
        absGet(b);
        bulkGet(b);

        absPut(b);
        relGet(b);
        absGet(b);
        bulkGet(b);

        bulkPutArray(b);
        relGet(b);

        bulkPutBuffer(b);
        relGet(b);

        absBulkPutArray(b);
        absBulkGet(b);





































        // Compact

        relPut(b);
        b.position(13);
        b.compact();
        b.flip();
        relGet(b, 13);

        // Exceptions

        relPut(b);
        b.limit(b.capacity() / 2);
        b.position(b.limit());

        tryCatch(b, BufferUnderflowException.class, () -> b.get());
        tryCatch(b, BufferOverflowException.class, () -> b.put((long)42));
        // The index must be non-negative and less than the buffer's limit.
        catchIndexOutOfBounds(b, () -> b.get(b.limit()));
        catchIndexOutOfBounds(b, () -> b.get(-1));
        catchIndexOutOfBounds(b, () -> b.put(b.limit(), (long)42));
        tryCatch(b, InvalidMarkException.class,
                () -> b.position(0).mark().compact().reset());

        try {
            b.position(b.limit() + 1);
            fail("IllegalArgumentException expected for position beyond limit");
        } catch (IllegalArgumentException e) {
            if (e.getMessage() == null) {
                fail("Non-null IllegalArgumentException message expected for"
                     + " position beyond limit");
            }
        }

        try {
            b.position(-1);
            fail("IllegalArgumentException expected for negative position");
        } catch (IllegalArgumentException e) {
            if (e.getMessage() == null) {
                fail("Non-null IllegalArgumentException message expected for"
                     + " negative position");
            }
        }

        try {
            b.limit(b.capacity() + 1);
            fail("IllegalArgumentException expected for limit beyond capacity");
        } catch (IllegalArgumentException e) {
            if (e.getMessage() == null) {
                fail("Non-null IllegalArgumentException message expected for"
                     + " limit beyond capacity");
            }
        }

        try {
            b.limit(-1);
            fail("IllegalArgumentException expected for negative limit");
        } catch (IllegalArgumentException e) {
            if (e.getMessage() == null) {
                fail("Non-null IllegalArgumentException message expected for"
                     + " negative limit");
            }
        }

        // Exceptions in absolute bulk and slice operations

        catchNullArgument(b, () -> b.get(7, null, 0, 42));
        catchNullArgument(b, () -> b.put(7, (long[])null, 0, 42));

        long[] tmpa = new long[42];
        catchIndexOutOfBounds(b, () -> b.get(7, tmpa, -1, 42));
        catchIndexOutOfBounds(b, () -> b.get(7, tmpa, 42, 1));
        catchIndexOutOfBounds(b, () -> b.get(7, tmpa, 41, -1));
        catchIndexOutOfBounds(b, () -> b.get(-1, tmpa, 0, 1));
        catchIndexOutOfBounds(b, () -> b.get(b.limit(), tmpa, 0, 1));
        catchIndexOutOfBounds(b, () -> b.get(b.limit() - 41, tmpa, 0, 42));

        catchIndexOutOfBounds(b, () -> b.put(7, tmpa, -1, 42));
        catchIndexOutOfBounds(b, () -> b.put(7, tmpa, 42, 1));
        catchIndexOutOfBounds(b, () -> b.put(7, tmpa, 41, -1));
        catchIndexOutOfBounds(b, () -> b.put(-1, tmpa, 0, 1));
        catchIndexOutOfBounds(b, () -> b.put(b.limit(), tmpa, 0, 1));
        catchIndexOutOfBounds(b, () -> b.put(b.limit() - 41, tmpa, 0, 42));

        catchIndexOutOfBounds(b, () -> b.slice(-1, 7));
        catchIndexOutOfBounds(b, () -> b.slice(b.limit() + 1, 7));
        catchIndexOutOfBounds(b, () -> b.slice(0, -1));
        catchIndexOutOfBounds(b, () -> b.slice(7, b.limit() - 7 + 1));

        // Values

        b.clear();
        b.put((long)0);
        b.put((long)-1);
        b.put((long)1);
        b.put(Long.MAX_VALUE);
        b.put(Long.MIN_VALUE);

















        b.flip();
        ck(b, b.get(), 0);
        ck(b, b.get(), (long)-1);
        ck(b, b.get(), 1);
        ck(b, b.get(), Long.MAX_VALUE);
        ck(b, b.get(), Long.MIN_VALUE);



























        // Comparison
        b.rewind();
        LongBuffer b2 = LongBuffer.allocate(b.capacity());
        b2.put(b);
        b2.flip();
        b.position(2);
        b2.position(2);
        if (!b.equals(b2)) {
            for (int i = 2; i < b.limit(); i++) {
                long x = b.get(i);
                long y = b2.get(i);
                if (x != y






                    ) {
                    out.println("[" + i + "] " + x + " != " + y);
                }
            }
            fail("Identical buffers not equal", b, b2);
        }
        if (b.compareTo(b2) != 0) {
            fail("Comparison to identical buffer != 0", b, b2);
        }
        b.limit(b.limit() + 1);
        b.position(b.limit() - 1);
        b.put((long)99);
        b.rewind();
        b2.rewind();
        if (b.equals(b2))
            fail("Non-identical buffers equal", b, b2);
        if (b.compareTo(b2) <= 0)
            fail("Comparison to shorter buffer <= 0", b, b2);
        b.limit(b.limit() - 1);

        b.put(2, (long)42);
        if (b.equals(b2))
            fail("Non-identical buffers equal", b, b2);
        if (b.compareTo(b2) <= 0)
            fail("Comparison to lesser buffer <= 0", b, b2);

        // Check equals and compareTo with interesting values
        for (long x : VALUES) {
            LongBuffer xb = LongBuffer.wrap(new long[] { x });
            if (xb.compareTo(xb) != 0) {
                fail("compareTo not reflexive", xb, xb, x, x);
            }
            if (!xb.equals(xb)) {
                fail("equals not reflexive", xb, xb, x, x);
            }
            for (long y : VALUES) {
                LongBuffer yb = LongBuffer.wrap(new long[] { y });
                if (xb.compareTo(yb) != - yb.compareTo(xb)) {
                    fail("compareTo not anti-symmetric",
                         xb, yb, x, y);
                }
                if ((xb.compareTo(yb) == 0) != xb.equals(yb)) {
                    fail("compareTo inconsistent with equals",
                         xb, yb, x, y);
                }
                if (xb.compareTo(yb) != Long.compare(x, y)) {






                    fail("Incorrect results for LongBuffer.compareTo",
                         xb, yb, x, y);
                }
                if (xb.equals(yb) != ((x == y) || ((x != x) && (y != y)))) {
                    fail("Incorrect results for LongBuffer.equals",
                         xb, yb, x, y);
                }
            }
        }

        // Sub, dup

        relPut(b);
        relGet(b.duplicate());
        b.position(13);
        relGet(b.duplicate(), 13);
        relGet(b.duplicate().slice(), 13);
        relGet(b.slice(), 13);
        relGet(b.slice().duplicate(), 13);

        // Slice

        b.position(5);
        LongBuffer sb = b.slice();
        checkSlice(b, sb);
        b.position(0);
        LongBuffer sb2 = sb.slice();
        checkSlice(sb, sb2);

        if (!sb.equals(sb2))
            fail("Sliced slices do not match", sb, sb2);
        if ((sb.hasArray()) && (sb.arrayOffset() != sb2.arrayOffset())) {
            fail("Array offsets do not match: "
                 + sb.arrayOffset() + " != " + sb2.arrayOffset(), sb, sb2);
        }

        int bPos = b.position();
        int bLim = b.limit();

        b.position(7);
        b.limit(42);
        LongBuffer rsb = b.slice();
        b.position(0);
        b.limit(b.capacity());
        LongBuffer asb = b.slice(7, 35);
        checkSlice(rsb, asb);

        b.position(bPos);
        b.limit(bLim);
































        // Read-only views

        b.rewind();
        final LongBuffer rb = b.asReadOnlyBuffer();
        if (!b.equals(rb))
            fail("Buffer not equal to read-only view", b, rb);
        show(level + 1, rb);

        catchReadOnlyBuffer(b, () -> relPut(rb));
        catchReadOnlyBuffer(b, () -> absPut(rb));
        catchReadOnlyBuffer(b, () -> bulkPutArray(rb));
        catchReadOnlyBuffer(b, () -> bulkPutBuffer(rb));
        catchReadOnlyBuffer(b, () -> absBulkPutArray(rb));

        // put(LongBuffer) should not change source position
        final LongBuffer src = LongBuffer.allocate(1);
        catchReadOnlyBuffer(b, () -> rb.put(src));
        ck(src, src.position(), 0);

        catchReadOnlyBuffer(b, () -> rb.compact());


























        if (rb.getClass().getName().startsWith("java.nio.Heap")) {
            catchReadOnlyBuffer(b, () -> rb.array());
            catchReadOnlyBuffer(b, () -> rb.arrayOffset());
            if (rb.hasArray()) {
                fail("Read-only heap buffer's backing array is accessible", rb);
            }
        }

        // Bulk puts from read-only buffers

        b.clear();
        rb.rewind();
        b.put(rb);











        relPut(b);                       // Required by testViews






    }



















































    public static void test(final long [] ba) {
        int offset = 47;
        int length = 900;
        final LongBuffer b = LongBuffer.wrap(ba, offset, length);
        show(0, b);
        ck(b, b.capacity(), ba.length);
        ck(b, b.position(), offset);
        ck(b, b.limit(), offset + length);

        // The offset must be non-negative and no larger than <array.length>.
        catchIndexOutOfBounds(ba, () -> LongBuffer.wrap(ba, -1, ba.length));
        catchIndexOutOfBounds(ba, () -> LongBuffer.wrap(ba, ba.length + 1, ba.length));
        catchIndexOutOfBounds(ba, () -> LongBuffer.wrap(ba, 0, -1));
        catchIndexOutOfBounds(ba, () -> LongBuffer.wrap(ba, 0, ba.length + 1));

        // A NullPointerException will be thrown if the array is null.
        tryCatch(ba, NullPointerException.class,
                () -> LongBuffer.wrap((long []) null, 0, 5));
        tryCatch(ba, NullPointerException.class,
                () -> LongBuffer.wrap((long []) null));
    }

    private static void testAllocate() {
        // An IllegalArgumentException will be thrown for negative capacities.
        catchIllegalArgument((Buffer) null, () -> LongBuffer.allocate(-1));
        try {
            LongBuffer.allocate(-1);
        } catch (IllegalArgumentException e) {
            if (e.getMessage() == null) {
                fail("Non-null IllegalArgumentException message expected for"
                     + " attempt to allocate negative capacity buffer");
            }
        }











    }

    public static void testToString() {
        final int cap = 10;










        LongBuffer nondirect1 = LongBuffer.allocate(cap);
        if (!nondirect1.toString().equals(Basic.toString(nondirect1))) {
           fail("Heap buffer toString is incorrect: "
                  + nondirect1.toString() + " vs " + Basic.toString(nondirect1));
        }

    }

    public static void test() {
        testAllocate();
        test(0, LongBuffer.allocate(7 * 1024), false);
        test(0, LongBuffer.wrap(new long[7 * 1024], 0, 7 * 1024), false);
        test(new long[1024]);










        callReset(LongBuffer.allocate(10));



        putBuffer();


        testToString();
    }

}
