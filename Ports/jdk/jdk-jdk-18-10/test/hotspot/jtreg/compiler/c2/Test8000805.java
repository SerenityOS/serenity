/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8000805
 * @summary JMM issue: short loads are non-atomic
 *
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:-TieredCompilation -Xcomp
 *      -XX:+PrintCompilation
 *      -XX:CompileCommand=compileonly,compiler.c2.Test8000805::load*
 *      compiler.c2.Test8000805
 */

package compiler.c2;
public class Test8000805 {
    static long  loadS2LmaskFF (short[] sa) { return sa[0] & 0xFF; }
    static long _loadS2LmaskFF (short[] sa) { return sa[0] & 0xFF; }

    static long  loadS2Lmask16 (short[] sa) { return sa[0] & 0xFFFE; }
    static long _loadS2Lmask16 (short[] sa) { return sa[0] & 0xFFFE; }

    static long  loadS2Lmask13 (short[] sa) { return sa[0] & 0x0FFF; }
    static long _loadS2Lmask13 (short[] sa) { return sa[0] & 0x0FFF; }

    static int  loadUS_signExt (char[] ca) { return (ca[0] << 16) >> 16; }
    static int _loadUS_signExt (char[] ca) { return (ca[0] << 16) >> 16; }

    static long  loadB2L_mask8 (byte[] ba) { return ba[0] & 0x55; }
    static long _loadB2L_mask8 (byte[] ba) { return ba[0] & 0x55; }

    public static void main(String[] args) {
        for (int i = Byte.MIN_VALUE; i < Byte.MAX_VALUE; i++) {
            byte[] ba = new byte[]  { (byte) i};

            { long v1 =  loadB2L_mask8(ba);
              long v2 = _loadB2L_mask8(ba);
              if (v1 != v2)
              throw new InternalError(String.format("loadB2L_mask8 failed: %x != %x", v1, v2)); }
        }

        for (int i = Short.MIN_VALUE; i < Short.MAX_VALUE; i++) {
            short[] sa = new short[] { (short)i };
            char[] ca = new char[] { (char)i };

            { long v1 =  loadS2LmaskFF(sa);
              long v2 = _loadS2LmaskFF(sa);
              if (v1 != v2)
              throw new InternalError(String.format("loadS2LmaskFF failed: %x != %x", v1, v2)); }

            { long v1 =  loadS2Lmask16(sa);
              long v2 = _loadS2Lmask16(sa);
              if (v1 != v2)
              throw new InternalError(String.format("loadS2Lmask16 failed: %x != %x", v1, v2)); }

            { long v1 =  loadS2Lmask13(sa);
              long v2 = _loadS2Lmask13(sa);
              if (v1 != v2)
              throw new InternalError(String.format("loadS2Lmask13 failed: %x != %x", v1, v2)); }

            { int v1 =  loadUS_signExt(ca);
              int v2 = _loadUS_signExt(ca);
              if (v1 != v2)
                throw new InternalError(String.format("loadUS_signExt failed: %x != %x", v1, v2)); }
        }

        System.out.println("TEST PASSED.");
    }
}
