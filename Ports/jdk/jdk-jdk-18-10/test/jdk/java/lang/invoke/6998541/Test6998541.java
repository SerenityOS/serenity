/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 6998541
 * @summary JSR 292 implement missing return-type conversion for OP_RETYPE_RAW
 *
 * @run main/othervm -Xbatch
 *    -XX:+UnlockDiagnosticVMOptions -XX:ScavengeRootsInCode=2
 *       -DTest6998541.N=100000 -DTest6998541.KIND=cast Test6998541
 * @run main/othervm -Xbatch
 *    -XX:+UnlockDiagnosticVMOptions -XX:ScavengeRootsInCode=2
 *       -DTest6998541.N=100000 -DTest6998541.KIND=normal Test6998541
 */

import java.util.*;

import java.lang.invoke.*;
import static java.lang.invoke.MethodHandles.*;

public class Test6998541 {
    private static final Class  CLASS = Test6998541.class;
    private static final String NAME  = "identity";
    private static final int    N     = Math.max(2, Integer.getInteger(CLASS.getSimpleName()+".N", 10000));
    private static final String KIND  = System.getProperty(CLASS.getSimpleName()+".KIND", "cast");
    private static final int    BITS  = 0x00000201;

    private static final boolean DO_CASTS = !KIND.equals("normal");

    public static void main(String[] args) throws Throwable {
        System.out.println("KIND="+KIND+" DO_CASTS="+DO_CASTS+" N="+N);
        doboolean();
        dobyte();
        dochar();
        doshort();
        doint();
        dolong();
        dofloat();
        dodouble();
        dovoid();
    }

    private static void doboolean() throws Throwable {
        for (int i = 0; i < N; i++) {
            boolean2prim(false);
            boolean2prim(true);
        }
        boolean2prim_invalid(true);
    }
    private static void dobyte() throws Throwable {
        byte x = Byte.MIN_VALUE;
        for (int i = 0; i < N; i++, x++)
            byte2prim(x);
        byte2prim_invalid(x);
    }
    private static void dochar() throws Throwable {
        char x = Character.MIN_VALUE;
        for (int i = 0; i < N; i++, x++)
            char2prim(x);
        char2prim_invalid(x);
    }
    private static void doshort() throws Throwable {
        short x = Short.MIN_VALUE;
        for (int i = 0; i < N; i++, x++)
            short2prim(x);
        short2prim_invalid(x);
    }
    private static void doint() throws Throwable {
        int x = Integer.MIN_VALUE;
        int D = Integer.MAX_VALUE / (N / 2) | BITS;
        for (int i = 0; i < N; i++, x += D) {
            int2prim(x);
        }
        int2prim_invalid(x);
    }
    private static void dolong() throws Throwable {
        long x = Long.MIN_VALUE;
        long D = Long.MAX_VALUE / ((long) (N / 2)) | BITS;
        for (int i = 0; i < N; i++, x += D)
            long2prim(x);
        long2prim_invalid(x);
    }
    private static void dofloat() throws Throwable {
        float x = Float.MIN_VALUE;
        float D = Float.MAX_VALUE / ((float) (N / 2));
        for (int i = 0; i < N; i++, x += D)
            float2prim(x);
        float2prim_invalid(x);
    }
    private static void dodouble() throws Throwable {
        double x = Double.MIN_VALUE;
        double D = Double.MAX_VALUE / ((double) (N / 2));
        for (int i = 0; i < N; i++, x += D)
            double2prim(x);
        double2prim_invalid(x);
    }
    private static void dovoid() throws Throwable {
        for (int i = 0; i < N; i++) {
            void2prim(i);
        }
        void2prim_invalid(0);
        // do the other direction here also:
        for (int i = 0; i < N; i++) {
            prim2void(i);
        }
        prim2void_invalid(0);
    }

    private static void assertEquals(Object o, Object o2) {
        if (!o.equals(o2))
            throw new AssertionError("expected: " + o + ", found: " + o2);
    }
    private static void fail() {
        throw new AssertionError();
    }

    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();

    private static MethodHandle mh(Class ret, Class... args) {
        try {
            MethodType mt  = MethodType.methodType(ret, args);
            Class lookupRet = (args.length == 0 ? void.class : args[0]);
            MethodHandle mh = lookup.findStatic(CLASS, NAME, mt.changeReturnType(lookupRet));
            if (DO_CASTS)
                return MethodHandles.explicitCastArguments(mh, mt);
            if (canDoAsType(mh.type(), mt))
                return mh.asType(mt);
            try {
                mh.asType(mt);
                throw new AssertionError("asType should not succeed: "+mh+" => "+mt);
            } catch (WrongMethodTypeException ex) {
                // this was a required WMTE
                return mh.asType(mt.generic()).asType(mt);
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
    private static final Class<?>[] NUMERIC_TYPE_WIDENING_ORDER = {
        byte.class, short.class, int.class, long.class, float.class, double.class
    };
    private static boolean canDoAsType(Class<?> src, Class<?> dst) {
        if (src == dst)  return true;
        if (dst == void.class)  return true;
        if (src == void.class)  return true;  // allow void->zero
        if (!src.isPrimitive() || !dst.isPrimitive())  return true;
        // primitive conversion works for asType only when it's widening
        if (src == boolean.class || dst == boolean.class)  return false;
        if (dst == char.class)  return false;
        if (src == char.class)  src = int.class;  // can widen char to int
        for (Class<?> ntype : NUMERIC_TYPE_WIDENING_ORDER) {
            if (src == ntype)  return true;
            if (dst == ntype)  return false;
        }
        throw new AssertionError("should not reach here: "+src+", "+dst);
    }
    private static boolean canDoAsType(MethodType mt0, MethodType mt1) {
        Class<?> rt0 = mt0.returnType();
        Class<?> rt1 = mt1.returnType();
        if (!canDoAsType(rt0, rt1))  return false;
        int argc = mt0.parameterCount();
        if (argc != mt1.parameterCount())  return false;
        for (int i = 0; i < argc; i++) {
            if (!canDoAsType(mt1.parameterType(i), mt0.parameterType(i)))
                return false;
        }
        return true;
    }

    private static MethodHandle mh_z(Class ret) { return mh(ret, boolean.class); }

    private static final MethodHandle mh_zz = mh_z(boolean.class);
    private static final MethodHandle mh_bz = mh_z(byte.class   );
    private static final MethodHandle mh_cz = mh_z(char.class   );
    private static final MethodHandle mh_sz = mh_z(short.class  );
    private static final MethodHandle mh_iz = mh_z(int.class    );
    private static final MethodHandle mh_jz = mh_z(long.class   );
    private static final MethodHandle mh_fz = mh_z(float.class  );
    private static final MethodHandle mh_dz = mh_z(double.class );

    private static void boolean2prim(boolean x) throws Throwable {
        int i = x ? 1 : 0;
        assertEquals(          x, (boolean) mh_zz.invokeExact(x));  // boolean -> boolean
        if (!DO_CASTS)  return;
        assertEquals((byte)    i, (byte)    mh_bz.invokeExact(x));  // boolean -> byte
        assertEquals((char)    i, (char)    mh_cz.invokeExact(x));  // boolean -> char
        assertEquals((short)   i, (short)   mh_sz.invokeExact(x));  // boolean -> short
        assertEquals((int)     i, (int)     mh_iz.invokeExact(x));  // boolean -> int
        assertEquals((long)    i, (long)    mh_jz.invokeExact(x));  // boolean -> long
        assertEquals((float)   i, (float)   mh_fz.invokeExact(x));  // boolean -> float
        assertEquals((double)  i, (double)  mh_dz.invokeExact(x));  // boolean -> double
    }
    private static void boolean2prim_invalid(boolean x) throws Throwable {
        if (DO_CASTS)  return;
        try { byte    y = (byte)    mh_bz.invokeExact(x); fail(); } catch (ClassCastException expected) {}  // boolean -> byte
        try { char    y = (char)    mh_cz.invokeExact(x); fail(); } catch (ClassCastException expected) {}  // boolean -> char
        try { short   y = (short)   mh_sz.invokeExact(x); fail(); } catch (ClassCastException expected) {}  // boolean -> short
        try { int     y = (int)     mh_iz.invokeExact(x); fail(); } catch (ClassCastException expected) {}  // boolean -> int
        try { long    y = (long)    mh_jz.invokeExact(x); fail(); } catch (ClassCastException expected) {}  // boolean -> long
        try { float   y = (float)   mh_fz.invokeExact(x); fail(); } catch (ClassCastException expected) {}  // boolean -> float
        try { double  y = (double)  mh_dz.invokeExact(x); fail(); } catch (ClassCastException expected) {}  // boolean -> double
    }

    private static MethodHandle mh_b(Class ret) { return mh(ret, byte.class); }

    private static final MethodHandle mh_zb = mh_b(boolean.class);
    private static final MethodHandle mh_bb = mh_b(byte.class   );
    private static final MethodHandle mh_cb = mh_b(char.class   );
    private static final MethodHandle mh_sb = mh_b(short.class  );
    private static final MethodHandle mh_ib = mh_b(int.class    );
    private static final MethodHandle mh_jb = mh_b(long.class   );
    private static final MethodHandle mh_fb = mh_b(float.class  );
    private static final MethodHandle mh_db = mh_b(double.class );

    private static void byte2prim(byte x) throws Throwable {
        assertEquals((byte)    x, (byte)    mh_bb.invokeExact(x));  // byte -> byte
        assertEquals((short)   x, (short)   mh_sb.invokeExact(x));  // byte -> short
        assertEquals((int)     x, (int)     mh_ib.invokeExact(x));  // byte -> int
        assertEquals((long)    x, (long)    mh_jb.invokeExact(x));  // byte -> long
        assertEquals((float)   x, (float)   mh_fb.invokeExact(x));  // byte -> float
        assertEquals((double)  x, (double)  mh_db.invokeExact(x));  // byte -> double
        if (!DO_CASTS)  return;
        boolean z = ((x & 1) != 0);
        assertEquals((char)    x, (char)    mh_cb.invokeExact(x));  // byte -> char
        assertEquals((boolean) z, (boolean) mh_zb.invokeExact(x));  // byte -> boolean
    }
    private static void byte2prim_invalid(byte x) throws Throwable {
        if (DO_CASTS)  return;
        try { char    y = (char)    mh_cb.invokeExact(x); fail(); } catch (ClassCastException expected) {}  // byte -> char
        try { boolean y = (boolean) mh_zb.invokeExact(x); fail(); } catch (ClassCastException expected) {}  // byte -> boolean
    }

    private static MethodHandle mh_c(Class ret) { return mh(ret, char.class); }

    private static final MethodHandle mh_zc = mh_c(boolean.class);
    private static final MethodHandle mh_bc = mh_c(byte.class   );
    private static final MethodHandle mh_cc = mh_c(char.class   );
    private static final MethodHandle mh_sc = mh_c(short.class  );
    private static final MethodHandle mh_ic = mh_c(int.class    );
    private static final MethodHandle mh_jc = mh_c(long.class   );
    private static final MethodHandle mh_fc = mh_c(float.class  );
    private static final MethodHandle mh_dc = mh_c(double.class );

    private static void char2prim(char x) throws Throwable {
        assertEquals((char)    x, (char)    mh_cc.invokeExact(x));  // char -> char
        assertEquals((int)     x, (int)     mh_ic.invokeExact(x));  // char -> int
        assertEquals((long)    x, (long)    mh_jc.invokeExact(x));  // char -> long
        assertEquals((float)   x, (float)   mh_fc.invokeExact(x));  // char -> float
        assertEquals((double)  x, (double)  mh_dc.invokeExact(x));  // char -> double
        if (!DO_CASTS)  return;
        boolean z = ((x & 1) != 0);
        assertEquals((boolean) z, (boolean) mh_zc.invokeExact(x));  // char -> boolean
        assertEquals((byte)    x, (byte)    mh_bc.invokeExact(x));  // char -> byte
        assertEquals((short)   x, (short)   mh_sc.invokeExact(x));  // char -> short
    }
    private static void char2prim_invalid(char x) throws Throwable {
        if (DO_CASTS)  return;
        try { boolean y = (boolean) mh_zc.invokeExact(x); fail(); } catch (ClassCastException expected) {}  // char -> boolean
        try { byte    y = (byte)    mh_bc.invokeExact(x); fail(); } catch (ClassCastException expected) {}  // char -> byte
        try { short   y = (short)   mh_sc.invokeExact(x); fail(); } catch (ClassCastException expected) {}  // char -> short
    }

    private static MethodHandle mh_s(Class ret) { return mh(ret, short.class); }

    private static final MethodHandle mh_zs = mh_s(boolean.class);
    private static final MethodHandle mh_bs = mh_s(byte.class   );
    private static final MethodHandle mh_cs = mh_s(char.class   );
    private static final MethodHandle mh_ss = mh_s(short.class  );
    private static final MethodHandle mh_is = mh_s(int.class    );
    private static final MethodHandle mh_js = mh_s(long.class   );
    private static final MethodHandle mh_fs = mh_s(float.class  );
    private static final MethodHandle mh_ds = mh_s(double.class );

    private static void short2prim(short x) throws Throwable {
        assertEquals((short)   x, (short)   mh_ss.invokeExact(x));  // short -> short
        assertEquals((int)     x, (int)     mh_is.invokeExact(x));  // short -> int
        assertEquals((long)    x, (long)    mh_js.invokeExact(x));  // short -> long
        assertEquals((float)   x, (float)   mh_fs.invokeExact(x));  // short -> float
        assertEquals((double)  x, (double)  mh_ds.invokeExact(x));  // short -> double
        if (!DO_CASTS)  return;
        boolean z = ((x & 1) != 0);
        assertEquals((boolean) z, (boolean) mh_zs.invokeExact(x));  // short -> boolean
        assertEquals((byte)    x, (byte)    mh_bs.invokeExact(x));  // short -> byte
        assertEquals((char)    x, (char)    mh_cs.invokeExact(x));  // short -> char
    }
    private static void short2prim_invalid(short x) throws Throwable {
        if (DO_CASTS)  return;
        try { boolean y = (boolean) mh_zs.invokeExact(x); fail(); } catch (ClassCastException expected) {}  // short -> boolean
        try { byte    y = (byte)    mh_bs.invokeExact(x); fail(); } catch (ClassCastException expected) {}  // short -> byte
        try { char    y = (char)    mh_cs.invokeExact(x); fail(); } catch (ClassCastException expected) {}  // short -> char
    }

    private static MethodHandle mh_i(Class ret) { return mh(ret, int.class); }

    private static final MethodHandle mh_zi = mh_i(boolean.class);
    private static final MethodHandle mh_bi = mh_i(byte.class   );
    private static final MethodHandle mh_ci = mh_i(char.class   );
    private static final MethodHandle mh_si = mh_i(short.class  );
    private static final MethodHandle mh_ii = mh_i(int.class    );
    private static final MethodHandle mh_ji = mh_i(long.class   );
    private static final MethodHandle mh_fi = mh_i(float.class  );
    private static final MethodHandle mh_di = mh_i(double.class );

    private static void int2prim(int x) throws Throwable {
        assertEquals((int)     x, (int)     mh_ii.invokeExact(x));  // int -> int
        assertEquals((long)    x, (long)    mh_ji.invokeExact(x));  // int -> long
        assertEquals((float)   x, (float)   mh_fi.invokeExact(x));  // int -> float
        assertEquals((double)  x, (double)  mh_di.invokeExact(x));  // int -> double
        if (!DO_CASTS)  return;
        boolean z = ((x & 1) != 0);
        assertEquals((boolean) z, (boolean) mh_zi.invokeExact(x));  // int -> boolean
        assertEquals((byte)    x, (byte)    mh_bi.invokeExact(x));  // int -> byte
        assertEquals((char)    x, (char)    mh_ci.invokeExact(x));  // int -> char
        assertEquals((short)   x, (short)   mh_si.invokeExact(x));  // int -> short
    }
    private static void int2prim_invalid(int x) throws Throwable {
        if (DO_CASTS)  return;
        try { boolean y = (boolean) mh_zi.invokeExact(x); fail(); } catch (ClassCastException expected) {}  // int -> boolean
        try { byte    y = (byte)    mh_bi.invokeExact(x); fail(); } catch (ClassCastException expected) {}  // int -> byte
        try { char    y = (char)    mh_ci.invokeExact(x); fail(); } catch (ClassCastException expected) {}  // int -> char
        try { short   y = (short)   mh_si.invokeExact(x); fail(); } catch (ClassCastException expected) {}  // int -> short
    }

    private static MethodHandle mh_j(Class ret) { return mh(ret, long.class); }

    private static final MethodHandle mh_zj = mh_j(boolean.class);
    private static final MethodHandle mh_bj = mh_j(byte.class   );
    private static final MethodHandle mh_cj = mh_j(char.class   );
    private static final MethodHandle mh_sj = mh_j(short.class  );
    private static final MethodHandle mh_ij = mh_j(int.class    );
    private static final MethodHandle mh_jj = mh_j(long.class   );
    private static final MethodHandle mh_fj = mh_j(float.class  );
    private static final MethodHandle mh_dj = mh_j(double.class );

    private static void long2prim(long x) throws Throwable {
        assertEquals((long)   x, (long)    mh_jj.invokeExact(x));  // long -> long
        assertEquals((float)  x, (float)   mh_fj.invokeExact(x));  // long -> float
        assertEquals((double) x, (double)  mh_dj.invokeExact(x));  // long -> double
        if (!DO_CASTS)  return;
        boolean z = ((x & 1) != 0);
        assertEquals((boolean)z, (boolean) mh_zj.invokeExact(x));  // long -> boolean
        assertEquals((byte)   x, (byte)    mh_bj.invokeExact(x));  // long -> byte
        assertEquals((char)   x, (char)    mh_cj.invokeExact(x));  // long -> char
        assertEquals((short)  x, (short)   mh_sj.invokeExact(x));  // long -> short
        assertEquals((int)    x, (int)     mh_ij.invokeExact(x));  // long -> int
    }
    private static void long2prim_invalid(long x) throws Throwable {
        if (DO_CASTS)  return;
        try { boolean y = (boolean) mh_zj.invokeExact(x); fail(); } catch (ClassCastException expected) {}  // long -> boolean
        try { byte    y = (byte)    mh_bj.invokeExact(x); fail(); } catch (ClassCastException expected) {}  // long -> byte
        try { char    y = (char)    mh_cj.invokeExact(x); fail(); } catch (ClassCastException expected) {}  // long -> char
        try { short   y = (short)   mh_sj.invokeExact(x); fail(); } catch (ClassCastException expected) {}  // long -> short
        try { int     y = (int)     mh_ij.invokeExact(x); fail(); } catch (ClassCastException expected) {}  // long -> int
    }

    private static MethodHandle mh_f(Class ret) { return mh(ret, float.class); }

    private static final MethodHandle mh_zf = mh_f(boolean.class);
    private static final MethodHandle mh_bf = mh_f(byte.class   );
    private static final MethodHandle mh_cf = mh_f(char.class   );
    private static final MethodHandle mh_sf = mh_f(short.class  );
    private static final MethodHandle mh_if = mh_f(int.class    );
    private static final MethodHandle mh_jf = mh_f(long.class   );
    private static final MethodHandle mh_ff = mh_f(float.class  );
    private static final MethodHandle mh_df = mh_f(double.class );

    private static void float2prim(float x) throws Throwable {
        assertEquals((float)   x, (float)   mh_ff.invokeExact(x));  // float -> float
        assertEquals((double)  x, (double)  mh_df.invokeExact(x));  // float -> double
        if (!DO_CASTS)  return;
        boolean z = (((byte) x & 1) != 0);
        assertEquals((boolean) z, (boolean) mh_zf.invokeExact(x));  // float -> boolean
        assertEquals((byte)    x, (byte)    mh_bf.invokeExact(x));  // float -> byte
        assertEquals((char)    x, (char)    mh_cf.invokeExact(x));  // float -> char
        assertEquals((short)   x, (short)   mh_sf.invokeExact(x));  // float -> short
        assertEquals((int)     x, (int)     mh_if.invokeExact(x));  // float -> int
        assertEquals((long)    x, (long)    mh_jf.invokeExact(x));  // float -> long
    }
    private static void float2prim_invalid(float x) throws Throwable {
        if (DO_CASTS)  return;
        try { boolean y = (boolean) mh_zf.invokeExact(x); fail(); } catch (ClassCastException expected) {}  // float -> boolean
        try { byte    y = (byte)    mh_bf.invokeExact(x); fail(); } catch (ClassCastException expected) {}  // float -> byte
        try { char    y = (char)    mh_cf.invokeExact(x); fail(); } catch (ClassCastException expected) {}  // float -> char
        try { short   y = (short)   mh_sf.invokeExact(x); fail(); } catch (ClassCastException expected) {}  // float -> short
        try { int     y = (int)     mh_if.invokeExact(x); fail(); } catch (ClassCastException expected) {}  // float -> int
        try { long    y = (long)    mh_jf.invokeExact(x); fail(); } catch (ClassCastException expected) {}  // float -> long
    }

    private static MethodHandle mh_d(Class ret) { return mh(ret, double.class); }

    private static final MethodHandle mh_zd = mh_d(boolean.class);
    private static final MethodHandle mh_bd = mh_d(byte.class   );
    private static final MethodHandle mh_cd = mh_d(char.class   );
    private static final MethodHandle mh_sd = mh_d(short.class  );
    private static final MethodHandle mh_id = mh_d(int.class    );
    private static final MethodHandle mh_jd = mh_d(long.class   );
    private static final MethodHandle mh_fd = mh_d(float.class  );
    private static final MethodHandle mh_dd = mh_d(double.class );

    private static void double2prim(double x) throws Throwable {
        assertEquals((double) x, (double)  mh_dd.invokeExact(x));  // double -> double
        if (!DO_CASTS)  return;
        boolean z = (((byte) x & 1) != 0);
        assertEquals((boolean) z, (boolean) mh_zd.invokeExact(x));  // double -> boolean
        assertEquals((byte)    x, (byte)    mh_bd.invokeExact(x));  // double -> byte
        assertEquals((char)    x, (char)    mh_cd.invokeExact(x));  // double -> char
        assertEquals((short)   x, (short)   mh_sd.invokeExact(x));  // double -> short
        assertEquals((int)     x, (int)     mh_id.invokeExact(x));  // double -> int
        assertEquals((long)    x, (long)    mh_jd.invokeExact(x));  // double -> long
        assertEquals((float)   x, (float)   mh_fd.invokeExact(x));  // double -> float
    }
    private static void double2prim_invalid(double x) throws Throwable {
        if (DO_CASTS)  return;
        try { boolean y = (boolean) mh_zd.invokeExact(x); fail(); } catch (ClassCastException expected) {}  // double -> boolean
        try { byte    y = (byte)    mh_bd.invokeExact(x); fail(); } catch (ClassCastException expected) {}  // double -> byte
        try { char    y = (char)    mh_cd.invokeExact(x); fail(); } catch (ClassCastException expected) {}  // double -> char
        try { short   y = (short)   mh_sd.invokeExact(x); fail(); } catch (ClassCastException expected) {}  // double -> short
        try { int     y = (int)     mh_id.invokeExact(x); fail(); } catch (ClassCastException expected) {}  // double -> int
        try { long    y = (long)    mh_jd.invokeExact(x); fail(); } catch (ClassCastException expected) {}  // double -> long
        try { float   y = (float)   mh_fd.invokeExact(x); fail(); } catch (ClassCastException expected) {}  // double -> float
    }

    private static final MethodHandle mh_zv = mh(boolean.class);
    private static final MethodHandle mh_bv = mh(byte.class   );
    private static final MethodHandle mh_cv = mh(char.class   );
    private static final MethodHandle mh_sv = mh(short.class  );
    private static final MethodHandle mh_iv = mh(int.class    );
    private static final MethodHandle mh_jv = mh(long.class   );
    private static final MethodHandle mh_fv = mh(float.class  );
    private static final MethodHandle mh_dv = mh(double.class );

    private static void void2prim(int i) throws Throwable {
        assertEquals(        false, (boolean) mh_zv.invokeExact());  // void -> boolean
        assertEquals((byte)  0,     (byte)    mh_bv.invokeExact());  // void -> byte
        assertEquals((char)  0,     (char)    mh_cv.invokeExact());  // void -> char
        assertEquals((short) 0,     (short)   mh_sv.invokeExact());  // void -> short
        assertEquals(        0,     (int)     mh_iv.invokeExact());  // void -> int
        assertEquals(        0L,    (long)    mh_jv.invokeExact());  // void -> long
        assertEquals(        0.0f,  (float)   mh_fv.invokeExact());  // void -> float
        assertEquals(        0.0d,  (double)  mh_dv.invokeExact());  // void -> double
    }

    private static void void2prim_invalid(double x) throws Throwable {
        // no cases
    }

    private static MethodHandle mh_v(Class arg) { return mh(void.class, arg); }

    private static final MethodHandle mh_vz = mh_v(boolean.class);
    private static final MethodHandle mh_vb = mh_v(byte.class   );
    private static final MethodHandle mh_vc = mh_v(char.class   );
    private static final MethodHandle mh_vs = mh_v(short.class  );
    private static final MethodHandle mh_vi = mh_v(int.class    );
    private static final MethodHandle mh_vj = mh_v(long.class   );
    private static final MethodHandle mh_vf = mh_v(float.class  );
    private static final MethodHandle mh_vd = mh_v(double.class );

    private static void prim2void(int x) throws Throwable {
        boolean z = ((x & 1) != 0);
        mh_vz.invokeExact(         z);  // boolean -> void
        mh_vb.invokeExact((byte)   x);  // byte    -> void
        mh_vc.invokeExact((char)   x);  // char    -> void
        mh_vs.invokeExact((short)  x);  // short   -> void
        mh_vi.invokeExact((int)    x);  // int     -> void
        mh_vj.invokeExact((long)   x);  // long    -> void
        mh_vf.invokeExact((float)  x);  // float   -> void
        mh_vd.invokeExact((double) x);  // double  -> void
    }

    private static void prim2void_invalid(int x) throws Throwable {
        // no cases
    }

    private static boolean identity(boolean v) { return v; }
    private static byte    identity(byte    v) { return v; }
    private static char    identity(char    v) { return v; }
    private static short   identity(short   v) { return v; }
    private static int     identity(int     v) { return v; }
    private static long    identity(long    v) { return v; }
    private static float   identity(float   v) { return v; }
    private static double  identity(double  v) { return v; }
    private static void    identity() {}
}
