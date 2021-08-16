/*
 * Copyright (c) 2000, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4031762
 * @summary Test the primitive wrappers static toString()
 * @key randomness
 */

import java.util.Random;

public class ToString {

    private static Random generator = new Random();

    public static void main(String args[]) throws Exception {
        // boolean wrapper
        boolean b = false;
        Boolean B = new Boolean(b);
        if (!B.toString().equals(Boolean.toString(b)))
            throw new RuntimeException("Boolean wrapper toString() failure.");
        b = true;
        B = new Boolean(b);
        if (!B.toString().equals(Boolean.toString(b)))
            throw new RuntimeException("Boolean wrapper toString() failure.");

        // char wrapper
        for(int x=0; x<100; x++) {
            char c = (char)generator.nextInt();
            Character C = new Character(c);
            if (!C.toString().equals(Character.toString(c)))
                throw new RuntimeException("Character wrapper toString() failure.");
        }

        // byte wrapper
        for(int x=0; x<100; x++) {
            byte y = (byte)generator.nextInt();
            Byte Y = new Byte(y);
            if (!Y.toString().equals(Byte.toString(y)))
                throw new RuntimeException("Byte wrapper toString() failure.");
        }

        // short wrapper
        for(int x=0; x<100; x++) {
            short s = (short)generator.nextInt();
            Short S = new Short(s);
            if (!S.toString().equals(Short.toString(s)))
                throw new RuntimeException("Short wrapper toString() failure.");
        }

        // int wrapper
        for(int x=0; x<100; x++) {
            int i = generator.nextInt();
            Integer I = new Integer(i);
            if (!I.toString().equals(Integer.toString(i)))
                throw new RuntimeException("Integer wrapper toString() failure.");
        }

        // long wrapper
        for(int x=0; x<100; x++) {
            long l = generator.nextLong();
            Long L = new Long(l);
            if (!L.toString().equals(Long.toString(l)))
                throw new RuntimeException("Long wrapper toString() failure.");
        }

        // float wrapper
        for(int x=0; x<100; x++) {
            float f = generator.nextFloat();
            Float F = new Float(f);
            if (!F.toString().equals(Float.toString(f)))
                throw new RuntimeException("Float wrapper toString() failure.");
        }

        // double wrapper
        for(int x=0; x<100; x++) {
            double d = generator.nextDouble();
            Double D = new Double(d);
            if (!D.toString().equals(Double.toString(d)))
                throw new RuntimeException("Double wrapper toString() failure.");
        }

    }
}
