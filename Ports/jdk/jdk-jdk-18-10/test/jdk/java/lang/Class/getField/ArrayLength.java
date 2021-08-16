/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5047859
 * @summary verify that for an array type class instance, getField("length")
 *          throws an exception, and getFields() does not contain a Field for
 *          'length'
 */

import java.lang.reflect.Field;

public class ArrayLength {
    public static void main(String [] args) {
        int failed = 0;

        try {
            new String[0].getClass().getField("length");
            failed++;
            System.out.println("getField(\"length\") should throw NoSuchFieldException");
        } catch (NoSuchFieldException e) {
        }
        try {
            new String[0].getClass().getDeclaredField("length");
            failed++;
            System.out.println("getDeclaredField(\"length\") should throw NoSuchFieldException");
        } catch (NoSuchFieldException e) {
        }

        if (new String[0].getClass().getFields().length != 0) {
            failed++;
            System.out.println("getFields() for an array type should return a zero length array");
        }

        if (new String[0].getClass().getDeclaredFields().length != 0) {
            failed++;
            System.out.println("getDeclaredFields() for an array type should return a zero length array");
        }

        if (failed != 0)
            throw new RuntimeException("Test failed see log for details");
    }
}
