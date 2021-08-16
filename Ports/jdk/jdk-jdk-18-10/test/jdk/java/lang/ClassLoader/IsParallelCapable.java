/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8165793 8169435
 * @summary Test ClassLoader.isRegisteredAsParallelCapable() method
 * @run main IsParallelCapable
 */

import java.util.stream.Stream;

public class IsParallelCapable {
    public abstract static class TestCL extends ClassLoader {
        static {
            ClassLoader.registerAsParallelCapable();
        }
        public abstract boolean expectCapable();
        public Class findClass(String name) throws ClassNotFoundException {
            throw new ClassNotFoundException("Why are you using this?");
        }
    }

    public static class ParaCL extends TestCL {
        static {
            ClassLoader.registerAsParallelCapable();
        }
        @Override
        public boolean expectCapable() { return true; }
    }

    public static class NonParaCL extends TestCL {
        @Override
        public boolean expectCapable() {
            // Doesn't call registerAsParallelCapable()
            return false;
        }
    }

    public static class NonParaSubCL1 extends ParaCL {
        @Override
        public boolean expectCapable() {
            // Doesn't call registerAsParallelCapable()
            return false;
        }
    }

    public static class NonParaSubCL2 extends NonParaCL {
        static {
            ClassLoader.registerAsParallelCapable();
        }
        @Override
        public boolean expectCapable() {
            // Superclass is not parallel capable
            return false;
        }
    }

    public static class ParaSubCL extends ParaCL {
        static {
            ClassLoader.registerAsParallelCapable();
        }
        @Override
        public boolean expectCapable() { return true; }
    }

    public static void main(String[] args) throws Exception {
        if (!ClassLoader.getSystemClassLoader().isRegisteredAsParallelCapable()) {
            throw new RuntimeException("System classloader not parallel capable!?");
        }

        Stream.of(ParaCL.class,
                  NonParaCL.class,
                  NonParaSubCL1.class,
                  NonParaSubCL2.class,
                  ParaSubCL.class)
                .forEach(IsParallelCapable::testClassLoaderClass);
    }

    private static void testClassLoaderClass(Class<? extends TestCL> klazz) {
        try {
            TestCL cl = (TestCL)klazz.newInstance();
            if (cl.expectCapable() != cl.isRegisteredAsParallelCapable()) {
                throw new RuntimeException(klazz + " expectCapable: " +
                        cl.expectCapable() + ", isRegisteredAsParallelCapable: " +
                        cl.isRegisteredAsParallelCapable());
            } else {
                System.out.println(klazz + " passed");
            }
        } catch (InstantiationException |  IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
