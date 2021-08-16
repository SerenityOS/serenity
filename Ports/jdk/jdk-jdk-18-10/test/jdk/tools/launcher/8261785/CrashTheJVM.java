/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;

public class CrashTheJVM {
    public static void main(String... args) throws IOException {
        System.out.println("Fine 1: from the outer class");

        new Object() {
            public static void main(String... args) throws IOException {
                System.out.println("Crash Before Fix 1: from anonymous nested class");
            }
        };
        class LocalNestedClass {
            public static void main(String... args) throws IOException {
                System.out.println("Crash Before Fix 2: from local nested class");
            }
        }
    }

    public void fromMethod() {
        new Object() {
            public static void main(String... args) throws IOException {
                System.out.println("Crash Before Fix 3: from local anonymous class");
            }
        };
        class LocalInnerClass {
            public static void main(String... args) throws IOException {
                System.out.println("Crash Before Fix 4: from local inner class");
            }
        }
    }

    public class InnerClass {
        public static void main(String... args) throws IOException {
            System.out.println("Fine 2: from inner class");
        }
    }

    public static class NestedClass {
        public static void main(String... args) throws IOException {
            System.out.println("Fine 3: from nested class");
        }
    }

    Object foo = new Object() {
        public static void main(String... args) throws IOException {
            System.out.println("Anonymous inner class");
        }
    };
}
