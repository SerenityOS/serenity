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








public class BasicDouble
    extends Basic
{

    private static final double[] VALUES = {
        Double.MIN_VALUE,
        (double) -1,
        (double) 0,
        (double) 1,
        Double.MAX_VALUE,







        Double.NEGATIVE_INFINITY,
        Double.POSITIVE_INFINITY,
        Double.NaN,
        (double) -0.0,

    };

    private static void relGet(DoubleBuffer b) {
        int n = b.capacity();
        for (int i = 0; i < n; i++)
            ck(b, (long)b.get(), (long)((double)ic(i)));
        b.rewind();
    }

    private static void relGet(DoubleBuffer b, int start) {
        int n = b.remaining();
        for (int i = start; i < n; i++)
            ck(b, (long)b.get(), (long)((double)ic(i)));
        b.rewind();
    }

    private static void absGet(DoubleBuffer b) {
        int n = b.capacity();
        for (int i = 0; i < n; i++)
            ck(b, (long)b.get(), (long)((double)ic(i)));
        b.rewind();
    }

    private static void bulkGet(DoubleBuffer b) {
        int n = b.capacity();
        double[] a = new double[n + 7];
        b.get(a, 7, n);
        for (int i = 0; i < n; i++) {
            ck(b, (long)a[i + 7], (long)((double)ic(i)));
        }
    }

    private static void absBulkGet(DoubleBuffer b) {
        int n = b.capacity();
        int len = n - 7*2;
        double[] a = new double[n + 7];
        b.position(42);
        b.get(7, a, 7, len);
        ck(b, b.position() == 42);
        for (int i = 0; i < len; i++) {
            ck(b, (long)a[i + 7], (long)((double)ic(i)));
        }
    }

    private static void relPut(DoubleBuffer b) {
        int n = b.capacity();
        b.clear();
        for (int i = 0; i < n; i++)
            b.put((double)ic(i));
        b.flip();
    }

    private static void absPut(DoubleBuffer b) {
        int n = b.capacity();
        b.clear();
        for (int i = 0; i < n; i++)
            b.put(i, (double)ic(i));
        b.limit(n);
        b.position(0);
    }

    private static void bulkPutArray(DoubleBuffer b) {
        int n = b.capacity();
        b.clear();
        double[] a = new double[n + 7];
        for (int i = 0; i < n; i++)
            a[i + 7] = (double)ic(i);
        b.put(a, 7, n);
        b.flip();
    }

    private static void bulkPutBuffer(DoubleBuffer b) {
        int n = b.capacity();
        b.clear();
        DoubleBuffer c = DoubleBuffer.allocate(n + 7);
        c.position(7);
        for (int i = 0; i < n; i++)
            c.put((double)ic(i));
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

    private static void absBulkPutArray(DoubleBuffer b) {
        int n = b.capacity();
        b.clear();
        int lim = n - 7;
        int len = lim - 7;
        b.limit(lim);
        double[] a = new double[len + 7];
        for (int i = 0; i < len; i++)
            a[i + 7] = (double)ic(i);
        b.position(42);
        b.put(7, a, 7, len);
        ck(b, b.position() == 42);
    }

    //6231529
    private static void callReset(DoubleBuffer b) {
        b.position(0);
        b.mark();

        b.duplicate().reset();
        b.asReadOnlyBuffer().reset();
    }



    // 6221101-6234263

    private static void putBuffer() {
        final int cap = 10;

        DoubleBuffer direct1 = ByteBuffer.allocateDirect(cap).asDoubleBuffer();
        DoubleBuffer nondirect1 = ByteBuffer.allocate(cap).asDoubleBuffer();
        direct1.put(nondirect1);

        DoubleBuffer direct2 = ByteBuffer.allocateDirect(cap).asDoubleBuffer();
        DoubleBuffer nondirect2 = ByteBuffer.allocate(cap).asDoubleBuffer();
        nondirect2.put(direct2);

        DoubleBuffer direct3 = ByteBuffer.allocateDirect(cap).asDoubleBuffer();
        DoubleBuffer direct4 = ByteBuffer.allocateDirect(cap).asDoubleBuffer();
        direct3.put(direct4);

        DoubleBuffer nondirect3 = ByteBuffer.allocate(cap).asDoubleBuffer();
        DoubleBuffer nondirect4 = ByteBuffer.allocate(cap).asDoubleBuffer();
        nondirect3.put(nondirect4);
    }

















    private static void checkSlice(DoubleBuffer b, DoubleBuffer slice) {
        ck(slice, 0, slice.position());
        ck(slice, b.remaining(), slice.limit());
        ck(slice, b.remaining(), slice.capacity());
        if (b.isDirect() != slice.isDirect())
            fail("Lost direction", slice);
        if (b.isReadOnly() != slice.isReadOnly())
            fail("Lost read-only", slice);
    }































































































































































































































































































































    private static void fail(String problem,
                             DoubleBuffer xb, DoubleBuffer yb,
                             double x, double y) {
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

    private static void catchIndexOutOfBounds(double[] t, Runnable thunk) {
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

    private static void tryCatch(double[] t, Class<?> ex, Runnable thunk) {
        tryCatch(DoubleBuffer.wrap(t), ex, thunk);
    }

    public static void test(int level, final DoubleBuffer b, boolean direct) {

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
        tryCatch(b, BufferOverflowException.class, () -> b.put((double)42));
        // The index must be non-negative and less than the buffer's limit.
        catchIndexOutOfBounds(b, () -> b.get(b.limit()));
        catchIndexOutOfBounds(b, () -> b.get(-1));
        catchIndexOutOfBounds(b, () -> b.put(b.limit(), (double)42));
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
        catchNullArgument(b, () -> b.put(7, (double[])null, 0, 42));

        double[] tmpa = new double[42];
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
        b.put((double)0);
        b.put((double)-1);
        b.put((double)1);
        b.put(Double.MAX_VALUE);
        b.put(Double.MIN_VALUE);









        b.put(-Double.MAX_VALUE);
        b.put(-Double.MIN_VALUE);
        b.put(Double.NEGATIVE_INFINITY);
        b.put(Double.POSITIVE_INFINITY);
        b.put(Double.NaN);
        b.put(0.5121609353879392);      // Changes value if incorrectly swapped


        b.flip();
        ck(b, b.get(), 0);
        ck(b, b.get(), (double)-1);
        ck(b, b.get(), 1);
        ck(b, b.get(), Double.MAX_VALUE);
        ck(b, b.get(), Double.MIN_VALUE);














        double v;
        ck(b, b.get(), -Double.MAX_VALUE);
        ck(b, b.get(), -Double.MIN_VALUE);
        ck(b, b.get(), Double.NEGATIVE_INFINITY);
        ck(b, b.get(), Double.POSITIVE_INFINITY);
        if (Double.doubleToRawLongBits(v = b.get())
            != Double.doubleToRawLongBits(Double.NaN)) {
            fail(b, (long)Double.NaN, (long)v);
        }
        ck(b, b.get(), 0.5121609353879392);



        // Comparison
        b.rewind();
        DoubleBuffer b2 = DoubleBuffer.allocate(b.capacity());
        b2.put(b);
        b2.flip();
        b.position(2);
        b2.position(2);
        if (!b.equals(b2)) {
            for (int i = 2; i < b.limit(); i++) {
                double x = b.get(i);
                double y = b2.get(i);
                if (x != y

                    || Double.compare(x, y) != 0




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
        b.put((double)99);
        b.rewind();
        b2.rewind();
        if (b.equals(b2))
            fail("Non-identical buffers equal", b, b2);
        if (b.compareTo(b2) <= 0)
            fail("Comparison to shorter buffer <= 0", b, b2);
        b.limit(b.limit() - 1);

        b.put(2, (double)42);
        if (b.equals(b2))
            fail("Non-identical buffers equal", b, b2);
        if (b.compareTo(b2) <= 0)
            fail("Comparison to lesser buffer <= 0", b, b2);

        // Check equals and compareTo with interesting values
        for (double x : VALUES) {
            DoubleBuffer xb = DoubleBuffer.wrap(new double[] { x });
            if (xb.compareTo(xb) != 0) {
                fail("compareTo not reflexive", xb, xb, x, x);
            }
            if (!xb.equals(xb)) {
                fail("equals not reflexive", xb, xb, x, x);
            }
            for (double y : VALUES) {
                DoubleBuffer yb = DoubleBuffer.wrap(new double[] { y });
                if (xb.compareTo(yb) != - yb.compareTo(xb)) {
                    fail("compareTo not anti-symmetric",
                         xb, yb, x, y);
                }
                if ((xb.compareTo(yb) == 0) != xb.equals(yb)) {
                    fail("compareTo inconsistent with equals",
                         xb, yb, x, y);
                }
                if (xb.compareTo(yb) != Double.compare(x, y)) {




                    if (x == 0.0 && y == 0.0) continue;

                    fail("Incorrect results for DoubleBuffer.compareTo",
                         xb, yb, x, y);
                }
                if (xb.equals(yb) != ((x == y) || ((x != x) && (y != y)))) {
                    fail("Incorrect results for DoubleBuffer.equals",
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
        DoubleBuffer sb = b.slice();
        checkSlice(b, sb);
        b.position(0);
        DoubleBuffer sb2 = sb.slice();
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
        DoubleBuffer rsb = b.slice();
        b.position(0);
        b.limit(b.capacity());
        DoubleBuffer asb = b.slice(7, 35);
        checkSlice(rsb, asb);

        b.position(bPos);
        b.limit(bLim);
































        // Read-only views

        b.rewind();
        final DoubleBuffer rb = b.asReadOnlyBuffer();
        if (!b.equals(rb))
            fail("Buffer not equal to read-only view", b, rb);
        show(level + 1, rb);

        catchReadOnlyBuffer(b, () -> relPut(rb));
        catchReadOnlyBuffer(b, () -> absPut(rb));
        catchReadOnlyBuffer(b, () -> bulkPutArray(rb));
        catchReadOnlyBuffer(b, () -> bulkPutBuffer(rb));
        catchReadOnlyBuffer(b, () -> absBulkPutArray(rb));

        // put(DoubleBuffer) should not change source position
        final DoubleBuffer src = DoubleBuffer.allocate(1);
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



















































    public static void test(final double [] ba) {
        int offset = 47;
        int length = 900;
        final DoubleBuffer b = DoubleBuffer.wrap(ba, offset, length);
        show(0, b);
        ck(b, b.capacity(), ba.length);
        ck(b, b.position(), offset);
        ck(b, b.limit(), offset + length);

        // The offset must be non-negative and no larger than <array.length>.
        catchIndexOutOfBounds(ba, () -> DoubleBuffer.wrap(ba, -1, ba.length));
        catchIndexOutOfBounds(ba, () -> DoubleBuffer.wrap(ba, ba.length + 1, ba.length));
        catchIndexOutOfBounds(ba, () -> DoubleBuffer.wrap(ba, 0, -1));
        catchIndexOutOfBounds(ba, () -> DoubleBuffer.wrap(ba, 0, ba.length + 1));

        // A NullPointerException will be thrown if the array is null.
        tryCatch(ba, NullPointerException.class,
                () -> DoubleBuffer.wrap((double []) null, 0, 5));
        tryCatch(ba, NullPointerException.class,
                () -> DoubleBuffer.wrap((double []) null));
    }

    private static void testAllocate() {
        // An IllegalArgumentException will be thrown for negative capacities.
        catchIllegalArgument((Buffer) null, () -> DoubleBuffer.allocate(-1));
        try {
            DoubleBuffer.allocate(-1);
        } catch (IllegalArgumentException e) {
            if (e.getMessage() == null) {
                fail("Non-null IllegalArgumentException message expected for"
                     + " attempt to allocate negative capacity buffer");
            }
        }











    }

    public static void testToString() {
        final int cap = 10;










        DoubleBuffer nondirect1 = DoubleBuffer.allocate(cap);
        if (!nondirect1.toString().equals(Basic.toString(nondirect1))) {
           fail("Heap buffer toString is incorrect: "
                  + nondirect1.toString() + " vs " + Basic.toString(nondirect1));
        }

    }

    public static void test() {
        testAllocate();
        test(0, DoubleBuffer.allocate(7 * 1024), false);
        test(0, DoubleBuffer.wrap(new double[7 * 1024], 0, 7 * 1024), false);
        test(new double[1024]);










        callReset(DoubleBuffer.allocate(10));



        putBuffer();


        testToString();
    }

}
