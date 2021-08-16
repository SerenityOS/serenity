/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6531597
 * @summary Tests encoding of arrays of primitives
 * @run main/othervm -Djava.security.manager=allow Test6531597
 * @author Sergey Malenkov
 */

public final class Test6531597 extends AbstractTest {
    public static void main(String[] args) {
        new Test6531597().test(true);
    }

    protected Object getObject() {
        return new Object[] {
                new byte[] {0, 1, 2},
                new short[] {0, 1, 2, 3, 4},
                new int[] {0, 1, 2, 3, 4, 5, 6},
                new long[] {0, 1, 2, 3, 4, 5, 6, 7, 8},
                new float[] {0.0f, 1.1f, 2.2f},
                new double[] {0.0, 1.1, 2.2, 3.3, 4.4},
                new char[] {'a', 'b', 'c'},
                new boolean[] {true, false},
        };
    }
}
