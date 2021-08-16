/*
 * Copyright (c) 1998, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4030384
 * @summary Verify inner class can be used in class literal.
 * Also includes a few other sanity checks.
 * @author William Maddox (maddox)
 *
 * @compile InnerClassLiterals.java
 * @run main InnerClassLiterals
 */

public class InnerClassLiterals {

    // Should not generate access errors.
    public static void main(String[] args) {
        Class x1 = int.class;
        Class x2 = float.class;
        Class x3 = void.class;
        Class x4 = String.class;
        Class x5 = Integer.class;
        Class x6 = InnerClassLiterals.class;
        // Bug 4030384: Compiler did not allow this.
        Class x7 = InnerClassLiterals.Inner1.class;
        Class x8 = InnerClassLiterals.Inner2.class;
    }

    class Inner1 {}

    static class Inner2 {}
}
