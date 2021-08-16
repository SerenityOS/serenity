/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @test Test7160757.java
 * @bug 7160757
 * @summary Tests that superclass initialization is not skipped
 */

public class Test7160757 {

    public static void main(String args[]) throws Exception {

        ClassLoader loader = new SLoader();
        try {
            Class.forName("S", true, loader);
            System.out.println("FAILED");
            throw new Exception("Should have thrown a VerifyError.");
        } catch (VerifyError e) {
            System.out.println(e);
            System.out.println("PASSED");
        }
    }

    static class SLoader extends ClassLoader {

        /**
         * public class S extends Throwable {
         *     public S() {
         *         aload_0
         *         invokespecial Object.<init>()
         *         return
         *     }
         * }
         */
        static byte b(int i) { return (byte)i; }
        static byte S_class[] = {
            b(0xca), b(0xfe), b(0xba), b(0xbe), 0x00, 0x00, 0x00, 0x32,
            0x00, 0x0c, 0x0a, 0x00, 0x0b, 0x00, 0x07, 0x07,
            0x00, 0x08, 0x07, 0x00, 0x09, 0x01, 0x00, 0x06,
            0x3c, 0x69, 0x6e, 0x69, 0x74, 0x3e, 0x01, 0x00,
            0x03, 0x28, 0x29, 0x56, 0x01, 0x00, 0x04, 0x43,
            0x6f, 0x64, 0x65, 0x0c, 0x00, 0x04, 0x00, 0x05,
            0x01, 0x00, 0x01, 0x53, 0x01, 0x00, 0x13, 0x6a,
            0x61, 0x76, 0x61, 0x2f, 0x6c, 0x61, 0x6e, 0x67,
            0x2f, 0x54, 0x68, 0x72, 0x6f, 0x77, 0x61, 0x62,
            0x6c, 0x65, 0x01, 0x00, 0x10, 0x6a, 0x61, 0x76,
            0x61, 0x2f, 0x6c, 0x61, 0x6e, 0x67, 0x2f, 0x4f,
            0x62, 0x6a, 0x65, 0x63, 0x74, 0x07, 0x00, 0x0a,
            0x00, 0x21, 0x00, 0x02, 0x00, 0x03, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x04,
            0x00, 0x05, 0x00, 0x01, 0x00, 0x06, 0x00, 0x00,
            0x00, 0x11, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00,
            0x00, 0x05, 0x2a, b(0xb7), 0x00, 0x01, b(0xb1), 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00
        };

        public Class findClass(String name) throws ClassNotFoundException {
            return defineClass(name, S_class, 0, S_class.length);
        }
    }
}
