/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 4990346 8200478
 * @summary Verify autoboxed values are cached as required.
 * @author Joseph D. Darcy
 */

public class BoxingCaching {

    static boolean verifyBooleanCaching() {
        boolean cached = true;

        Boolean results[] = new Boolean[2];

        results[0] = false;
        results[1] = true;

        Boolean B;

        B = false;
        if (B != results[0]) {
                cached = false;
                System.err.println("Boolean value " + B +
                                   " is not cached appropriately.");
        }

        B = true;
        if (B != results[1]) {
                cached = false;
                System.err.println("Boolean value " + B +
                                   " is not cached appropriately.");
        }

        return cached;
    }

    static boolean verifyByteCaching() {
        boolean cached = true;

        Byte results[] = new Byte[-(-128) + 127 +1];
        for(int i = 0; i < results.length; i++)
            results[i] = (byte)(i-128);

        for(int i = 0; i < results.length; i++) {
            Byte B = (byte)(i-128);
            if (B != results[i]) {
                cached = false;
                System.err.println("Byte value " + B +
                                   " is not cached appropriately.");
            }
        }

        for(int i = Byte.MIN_VALUE; i < Byte.MAX_VALUE; i++) {
            Byte B;
            B = (byte)i;
            if (B.byteValue() != i) {
                cached = false;
                System.err.println("Erroneous autoboxing conversion for " +
                                   "byte value " + i + " .");
            }
        }

        return cached;
    }

    static boolean verifyCharacterCaching() {
        boolean cached = true;

        Character results[] = new Character[127 +1];
        for(int i = 0; i < results.length; i++)
            results[i] = (char)i;

        for(int i = 0; i < results.length; i++) {
            Character C = (char)i;
            if (C != results[i]) {
                cached = false;
                System.err.println("Char value " + C +
                                   " is not cached appropriately.");
            }
        }

        for(int i = Character.MIN_VALUE; i < Character.MAX_VALUE; i++) {
            Character C;
            C = (char)i;
            if (C.charValue() != i) {
                cached = false;
                System.err.println("Erroneous autoboxing conversion for " +
                                   "char value " + i + " .");
            }
        }

        return cached;
    }

    static boolean verifyIntegerCaching() {
        boolean cached = true;

        Integer results[] = new Integer[-(-128) + 127 +1];
        for(int i = 0; i < results.length; i++)
            results[i] = (i-128);

        for(int i = 0; i < results.length; i++) {
            Integer I = (i-128);
            if (I != results[i]) {
                cached = false;
                System.err.println("Integer value " + I +
                                   " is not cached appropriately.");
            }
        }

        for(int i = -256; i < 255; i++) {
            Integer I;
            I = i;
            if (I.intValue() != i) {
                cached = false;
                System.err.println("Erroneous autoboxing conversion for " +
                                   "int value " + i + " .");
            }
        }

        return cached;
    }

    static boolean verifyLongCaching() {
        boolean cached = true;

        Long results[] = new Long[-(-128) + 127 +1];
        for(int i = 0; i < results.length; i++)
            results[i] = (long)(i-128);

        for(int i = 0; i < results.length; i++) {
            Long L = (long)(i-128);
            if (L != results[i]) {
                cached = false;
                System.err.println("Long value " + L +
                                   " is not cached appropriately.");
            }
        }

        for(int i = -256; i < 255; i++) {
            Integer L;
            L = i;
            if (L.longValue() != i) {
                cached = false;
                System.err.println("Erroneous autoboxing conversion for " +
                                   "int value " + i + " .");
            }
        }

        return cached;
    }

    static boolean verifyShortCaching() {
        boolean cached = true;

        Short results[] = new Short[-(-128) + 127 +1];
        for(int i = 0; i < results.length; i++)
            results[i] = (short)(i-128);

        for(int i = 0; i < results.length; i++) {
            Short S = (short)(i-128);
            if (S != results[i]) {
                cached = false;
                System.err.println("Short value " + S +
                                   " is not cached appropriately.");
            }
        }

        for(int i = Short.MIN_VALUE; i < Short.MAX_VALUE; i++) {
            Short S;
            S = (short)i;
            if (S.shortValue() != i) {
                cached = false;
                System.err.println("Erroneous autoboxing conversion for " +
                                   "short value " + i + " .");
            }
        }

        return cached;
    }

    public static void main(String argv[]) {
        boolean cached = true;

        cached &= verifyBooleanCaching();
        cached &= verifyByteCaching();
        cached &= verifyCharacterCaching();
        cached &= verifyIntegerCaching();
        cached &= verifyLongCaching();
        cached &= verifyShortCaching();

        if (!cached)
            throw new RuntimeException("Values not cached appropriately.");
    }
}
