/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4431318
 * @summary Verify that when serialVersionUID is declared with a type other
 *          than long, values that can be promoted to long will be used, and
 *          those that can't be will be ignored (but will not result in
 *          unchecked exceptions).
 */

import java.io.*;

class Z implements Serializable {
    @SuppressWarnings("serial") /* Incorrect declarations are being tested */
    private static final boolean serialVersionUID = false;
}

class B implements Serializable {
    @SuppressWarnings("serial") /* Incorrect declarations are being tested */
    private static final byte serialVersionUID = 5;
}

class C implements Serializable {
    @SuppressWarnings("serial") /* Incorrect declarations are being tested */
    private static final char serialVersionUID = 5;
}

class S implements Serializable {
    @SuppressWarnings("serial") /* Incorrect declarations are being tested */
    private static final short serialVersionUID = 5;
}

class I implements Serializable {
    @SuppressWarnings("serial") /* Incorrect declarations are being tested */
    private static final int serialVersionUID = 5;
}

class F implements Serializable {
    @SuppressWarnings("serial") /* Incorrect declarations are being tested */
    private static final float serialVersionUID = 5.0F;
}

class D implements Serializable {
    @SuppressWarnings("serial") /* Incorrect declarations are being tested */
    private static final double serialVersionUID = 5.0;
}

class L implements Serializable {
    @SuppressWarnings("serial") /* Incorrect declarations are being tested */
    private static final Object serialVersionUID = "5";
}


public class BadSerialVersionUID {
    public static void main(String[] args) throws Exception {
        Class<?>[] ignore = { Z.class, F.class, D.class, L.class };
        Class<?>[] convert = { B.class, C.class, S.class, I.class };

        for (int i = 0; i < ignore.length; i++) {
            ObjectStreamClass.lookup(ignore[i]).getSerialVersionUID();
        }
        for (int i = 0; i < convert.length; i++) {
            ObjectStreamClass desc = ObjectStreamClass.lookup(convert[i]);
            if (desc.getSerialVersionUID() != 5L) {
                throw new Error();
            }
        }
    }
}
