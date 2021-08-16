/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package optim;

public class ForNameTestCase {
    private static final String EXPECTED = "expected";
    public static Class<?> forName() {
        try {
            Class<?> cl = Class.forName("java.lang.String");
            return cl;
        } catch (ClassNotFoundException |
                IllegalArgumentException |
                ClassCastException x) {
            throw new InternalError(x);
        }
    }

    public static Class<?> forName0() throws ClassNotFoundException {
        return Class.forName("java.lang.String");
    }

    public static Class<?> forName1() throws Exception {
        Class<?> clazz = null;
        try {
            clazz = Class.forName("java.lang.String");
        } catch (ClassNotFoundException e) {
            return null;
        }
        return clazz;
    }

    public static void forNameException() throws Exception {
        try {
            Class.forName("java.lang.String");
            throw new Exception(EXPECTED);
        } catch (ClassNotFoundException e) {
            return;
        } catch (RuntimeException e) {
            return;
        }
    }

    public static Class<?> forName2() throws Exception {
        Class<?> clazz = null;
        try {
            clazz = Class.forName("java.lang.String");
            try {
                throw new Exception("das");
            } catch (Exception ex) {
            }
        } catch (ClassNotFoundException e) {
            return null;
        }
        return clazz;
    }

    public static Class<?> forName3() throws Exception {
        Class<?> clazz = null;
        try {
            return clazz = Class.forName("java.lang.String");
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public static Class<?> forName4() throws Exception {
        Class<?> clazz = null;
        try {
            clazz = Class.forName("java.lang.String");
        } catch (ClassNotFoundException e) {
            return null;
        } catch (RuntimeException e) {
            return null;
        }
        return clazz;
    }

    public static Class<?> forName5() {
        Class<?> clazz = null;
        try {
            clazz = Class.forName("java.lang.String");
        } catch (ClassNotFoundException e) {
        }
        int i;
        try {
            i = 0;
        } catch (Exception e) {
        }
        return clazz;
    }

    public static Class<?> forName6() {
        Class<?> clazz = null;
        try {
            return Class.forName("java.lang.String");
        } catch (ClassNotFoundException e) {
        }

        try {
                // This one is removed because no more reachable when
            // Class.forName is removed
            int k = 0;
        } catch (RuntimeException e) {
        }

        int i;
        try {
                // This one is removed because no more reachable when
            // Class.forName is removed
            return Class.forName("TOTO");
        } catch (ClassNotFoundException e) {
        }
        try {
                // This one is removed because no more reachable when
            // Class.forName is removed
            return Class.forName("TOTO");
        } catch (ClassNotFoundException e) {
        }
        try {
                // This one is removed because no more reachable when
            // Class.forName is removed
            return Class.forName("TOTO");
        } catch (ClassNotFoundException e) {
        }
        try {
                // This one is removed because no more reachable when
            // Class.forName is removed
            return Class.forName("TOTO");
        } catch (ClassNotFoundException e) {
        }
        return clazz;
    }

    public static Class<?> forName7() {
        Class<?> clazz = null;
        try {
            clazz = Class.forName("optim.AType");
        } catch (ClassNotFoundException e) {
        }
        return clazz;
    }

    public static Class<?> negativeforName() {
        Class<?> clazz = null;
        try {
            clazz = Class.forName("jdk.internal.jimage.BasicImageReader");
        } catch (ClassNotFoundException e) {
        }
        return clazz;
    }
}
