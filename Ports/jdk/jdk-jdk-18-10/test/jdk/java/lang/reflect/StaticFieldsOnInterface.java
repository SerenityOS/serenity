/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8186961
 * @run main/othervm StaticFieldsOnInterface C
 * @run main/othervm StaticFieldsOnInterface D
 * @run main/othervm StaticFieldsOnInterface Y
 */

public class StaticFieldsOnInterface {
    /*
            A
           / \
          B  C
          \  /
           D

        Interface A has a public field
        Ensure B, C, D only report exactly one public field

          A
         /
        X A
        |/
        Y

        Ensure class Y, extending class X, reports exactly one public field
     */

    public interface A {
        public static final int CONSTANT = 42;
    }

    public interface B extends A {
    }

    public interface C extends A {
    }

    public interface D extends B, C {
    }

    static class X implements A {}
    static class Y extends X implements A {}

    public static void main(String[] args) throws Exception {
        char first = 'C';
        if (args.length > 0) {
            first = args[0].charAt(0);
        }

        assertOneField(A.class);
        // D first
        if (first == 'D') {
            assertOneField(D.class);
            assertOneField(C.class);
        }
        // C first
        else if (first == 'C') {
            assertOneField(C.class);
            assertOneField(D.class);
        }
        else {
            assertOneField(Y.class);
        }
    }

    static void assertOneField(Class<?> c) {
        int nfs = c.getFields().length;
        if (nfs != 1) {
            throw new AssertionError(String.format(
                    "Class %s does not have exactly one field: %d", c.getName(), nfs));
        }
    }
}
