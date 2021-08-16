/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @summary Verify that definite assignment works (legal code)
 * @compile DefiniteAssignment1.java
 * @run main DefiniteAssignment1
 */
public class DefiniteAssignment1 {
    public static void main(String[] args) {
        int a = 0;

        {
        int x;

        switch(a) {
            case 0: x = 0; break;
            default: x = 1; break;
        }

        if (x != 0)
            throw new IllegalStateException("Unexpected value.");
        }

        {
        int x;

        switch(a) {
            case 1: x = 1; break;
            case 0:
            default: x = 0; break;
        }

        if (x != 0)
            throw new IllegalStateException("Unexpected value.");
        }

        {
        int x;

        switch(a) {
            case 1: x = 1; break;
            case 0:
            default: x = 0;
        }

        if (x != 0)
            throw new IllegalStateException("Unexpected value.");
        }

        {
        int x;

        switch(a) {
            case 0 -> x = 0;
            default -> x = 1;
        }

        if (x != 0)
            throw new IllegalStateException("Unexpected value.");
        }

        {
        int x;

        try {
            switch(a) {
                case 1: x = 1; break;
                case 0:
                default: throw new UnsupportedOperationException();
            }

            throw new IllegalStateException("Unexpected value: " + x);
            } catch (UnsupportedOperationException ex) {
                //OK
            }
        }

        {
        int x;

        switch(a) {
            case 0 -> x = 0;
            default -> throw new IllegalStateException();
        }

        if (x != 0)
            throw new IllegalStateException("Unexpected value.");
        }
    }

    enum E {
        A, B, C;
    }
}
