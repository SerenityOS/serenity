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

/*
 * @test
 * @bug 8042660
 * @summary Constant pool NameAndType entries must point to non-zero length Utf8 strings
 * @compile emptySigUtf8.jcod
 * @compile emptyNameUtf8.jcod
 * @run main/othervm -Xverify:all BadNameAndType
 */

// Test that a constant pool NameAndType descriptor_index and/or name_index
// that points to a zero length Utf8 string causes a ClassFormatError.
public class BadNameAndType {
    public static void main(String args[]) throws Throwable {

        System.out.println("Regression test for bug 8042660");

        // Test descriptor_index pointing to zero-length string.
        try {
            Class newClass = Class.forName("emptySigUtf8");
            throw new RuntimeException("Expected ClassFormatError exception not thrown");
        } catch (java.lang.ClassFormatError e) {
            System.out.println("Test BadNameAndType passed test case emptySigUtf8");
        }

        // Test name_index pointing to zero-length string.
        try {
            Class newClass = Class.forName("emptyNameUtf8");
            throw new RuntimeException("Expected ClassFormatError exception not thrown");
        } catch (java.lang.ClassFormatError e) {
            System.out.println("Test BadNameAndType passed test case emptyNameUtf8");
        }
    }
}
