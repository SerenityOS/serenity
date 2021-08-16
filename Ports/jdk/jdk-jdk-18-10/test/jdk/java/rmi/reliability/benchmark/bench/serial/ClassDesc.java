/*
 * Copyright (c) 1999, 2008, Oracle and/or its affiliates. All rights reserved.
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

/*
 *
 */

package bench.serial;

import bench.Benchmark;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;

/**
 * Benchmark for testing speed of class descriptor reads/writes.
 */
public class ClassDesc implements Benchmark {

    static class Dummy0 implements Serializable { Dummy0 i0; }
    static class Dummy1 extends Dummy0 { Dummy1 i1; }
    static class Dummy2 extends Dummy1 { Dummy2 i2; }
    static class Dummy3 extends Dummy2 { Dummy3 i3; }
    static class Dummy4 extends Dummy3 { Dummy4 i4; }
    static class Dummy5 extends Dummy4 { Dummy5 i5; }
    static class Dummy6 extends Dummy5 { Dummy6 i6; }
    static class Dummy7 extends Dummy6 { Dummy7 i7; }
    static class Dummy8 extends Dummy7 { Dummy8 i8; }
    static class Dummy9 extends Dummy8 { Dummy9 i9; }
    static class Dummy10 extends Dummy9 { Dummy10 i10; }
    static class Dummy11 extends Dummy10 { Dummy11 i11; }
    static class Dummy12 extends Dummy11 { Dummy12 i12; }
    static class Dummy13 extends Dummy12 { Dummy13 i13; }
    static class Dummy14 extends Dummy13 { Dummy14 i14; }
    static class Dummy15 extends Dummy14 { Dummy15 i15; }
    static class Dummy16 extends Dummy15 { Dummy16 i16; }
    static class Dummy17 extends Dummy16 { Dummy17 i17; }
    static class Dummy18 extends Dummy17 { Dummy18 i18; }
    static class Dummy19 extends Dummy18 { Dummy19 i19; }
    static class Dummy20 extends Dummy19 { Dummy20 i20; }
    static class Dummy21 extends Dummy20 { Dummy21 i21; }
    static class Dummy22 extends Dummy21 { Dummy22 i22; }
    static class Dummy23 extends Dummy22 { Dummy23 i23; }
    static class Dummy24 extends Dummy23 { Dummy24 i24; }
    static class Dummy25 extends Dummy24 { Dummy25 i25; }
    static class Dummy26 extends Dummy25 { Dummy26 i26; }
    static class Dummy27 extends Dummy26 { Dummy27 i27; }
    static class Dummy28 extends Dummy27 { Dummy28 i28; }
    static class Dummy29 extends Dummy28 { Dummy29 i29; }
    static class Dummy30 extends Dummy29 { Dummy30 i30; }
    static class Dummy31 extends Dummy30 { Dummy31 i31; }
    static class Dummy32 extends Dummy31 { Dummy32 i32; }
    static class Dummy33 extends Dummy32 { Dummy33 i33; }
    static class Dummy34 extends Dummy33 { Dummy34 i34; }
    static class Dummy35 extends Dummy34 { Dummy35 i35; }
    static class Dummy36 extends Dummy35 { Dummy36 i36; }
    static class Dummy37 extends Dummy36 { Dummy37 i37; }
    static class Dummy38 extends Dummy37 { Dummy38 i38; }
    static class Dummy39 extends Dummy38 { Dummy39 i39; }
    static class Dummy40 extends Dummy39 { Dummy40 i40; }
    static class Dummy41 extends Dummy40 { Dummy41 i41; }
    static class Dummy42 extends Dummy41 { Dummy42 i42; }
    static class Dummy43 extends Dummy42 { Dummy43 i43; }
    static class Dummy44 extends Dummy43 { Dummy44 i44; }
    static class Dummy45 extends Dummy44 { Dummy45 i45; }
    static class Dummy46 extends Dummy45 { Dummy46 i46; }
    static class Dummy47 extends Dummy46 { Dummy47 i47; }
    static class Dummy48 extends Dummy47 { Dummy48 i48; }
    static class Dummy49 extends Dummy48 { Dummy49 i49; }
    static class Dummy50 extends Dummy49 { Dummy50 i50; }

    /**
     * Write and read class descriptors to/from a stream.
     * Arguments: <# cycles>
     */
    public long run(String[] args) throws Exception {
        int ncycles = Integer.parseInt(args[0]);
        StreamBuffer sbuf = new StreamBuffer();
        ObjectOutputStream oout =
            new ObjectOutputStream(sbuf.getOutputStream());
        ObjectInputStream oin =
            new ObjectInputStream(sbuf.getInputStream());
        ObjectStreamClass desc = ObjectStreamClass.lookup(Dummy50.class);

        doReps(oout, oin, sbuf, desc, 1);       // warmup

        long start = System.currentTimeMillis();
        doReps(oout, oin, sbuf, desc, ncycles);
        return System.currentTimeMillis() - start;
    }

    /**
     * Run benchmark for given number of cycles.
     */
    void doReps(ObjectOutputStream oout, ObjectInputStream oin,
                StreamBuffer sbuf, ObjectStreamClass desc, int ncycles)
        throws Exception
    {
        for (int i = 0; i < ncycles; i++) {
            sbuf.reset();
            oout.reset();
            oout.writeObject(desc);
            oout.flush();
            oin.readObject();
        }
    }
}
