/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4978925
 * @summary This test verifies that java.lang.reflect.Proxy will work
 * with a proxy interface that contains names that are not valid Java
 * identifiers but that are legal at runtime as of version 49.0 of the
 * class file format.
 * @author Peter Jones
 *
 * @build Test
 * @run main Test
 */

import java.lang.reflect.Proxy;

public class Test {

    private static final String CLASS_NAME = "Test+Interface";

    /**
     * class file for empty interface named "Test+Interface"
     */
    private static final byte[] CLASS_FILE = {
        (byte) 0xca, (byte) 0xfe, (byte) 0xba, (byte) 0xbe,
                                0x00, 0x00, 0x00, 0x31,
        0x00, 0x07, 0x07, 0x00, 0x05, 0x07, 0x00, 0x06,
        0x01, 0x00, 0x0a, 0x53, 0x6f, 0x75, 0x72, 0x63,
        0x65, 0x46, 0x69, 0x6c, 0x65, 0x01, 0x00, 0x13,
        0x54, 0x65, 0x73, 0x74, 0x2b, 0x49, 0x6e, 0x74,
        0x65, 0x72, 0x66, 0x61, 0x63, 0x65, 0x2e, 0x6a,
        0x61, 0x76, 0x61, 0x01, 0x00, 0x0e, 0x54, 0x65,
        0x73, 0x74, 0x2b, 0x49, 0x6e, 0x74, 0x65, 0x72,
        0x66, 0x61, 0x63, 0x65, 0x01, 0x00, 0x10, 0x6a,
        0x61, 0x76, 0x61, 0x2f, 0x6c, 0x61, 0x6e, 0x67,
        0x2f, 0x4f, 0x62, 0x6a, 0x65, 0x63, 0x74, 0x06,
        0x01, 0x00, 0x01, 0x00, 0x02, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x03, 0x00,
        0x00, 0x00, 0x02, 0x00, 0x04
    };

    public static void main(String[] args) throws Exception {
        ClassLoader l = new Loader();
        Class i = Class.forName(CLASS_NAME, false, l);
        System.out.println(i);
        Class p = Proxy.getProxyClass(i.getClassLoader(), new Class[] { i });
        System.out.println(p);
    }

    private static class Loader extends ClassLoader {
        Loader() { }
        protected Class findClass(String name) throws ClassNotFoundException {
            if (name.equals(CLASS_NAME)) {
                return defineClass(name, CLASS_FILE, 0, CLASS_FILE.length);
            } else {
                return super.findClass(name);
            }
        }
    }
}
