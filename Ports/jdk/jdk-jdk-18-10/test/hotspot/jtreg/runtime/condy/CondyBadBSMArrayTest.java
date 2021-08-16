/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8186211
 * @summary CONSTANT_Dynamic_info structure present with various bad BSM index, BSM array attribute checks.
 * @compile CondyBadBSMIndex.jcod
 * @compile CondyEmptyBSMArray1.jcod
 * @compile CondyNoBSMArray.jcod
 * @run main/othervm -Xverify:all CondyBadBSMArrayTest
 */

// Test that a CONSTANT_Dynamic_info structure present with the following issues:
// 1. The CONSTANT_Dynamic_info structure's bootstrap_method_attr_index value is
//    an index outside of the array size.
// 2. An empty BootstrapMethods Attribute array
// 3. No BootstrapMethods Attribute array present.
public class CondyBadBSMArrayTest {
    public static void main(String args[]) throws Throwable {
        // 1. The CONSTANT_Dynamic_info structure's bootstrap_method_attr_index is outside the array size
        try {
            Class newClass = Class.forName("CondyBadBSMIndex");
            throw new RuntimeException("Expected ClassFormatError exception not thrown");
        } catch (java.lang.ClassFormatError e) {
            if (!e.getMessage().contains("Short length on BootstrapMethods in class file")) {
                throw new RuntimeException("ClassFormatError thrown, incorrect message");
            }
            System.out.println("Test CondyBadBSMIndex passed: " + e.getMessage());
        } catch (Throwable e) {
            throw new RuntimeException("Expected ClassFormatError exception not thrown");
        }

        // 2. An empty BootstrapMethods Attribute array - contains zero elements
        try {
            Class newClass = Class.forName("CondyEmptyBSMArray1");
            throw new RuntimeException("Expected ClassFormatError exception not thrown");
        } catch (java.lang.ClassFormatError e) {
            if (!e.getMessage().contains("Short length on BootstrapMethods in class file")) {
                throw new RuntimeException("ClassFormatError thrown, incorrect message");
            }
            System.out.println("Test CondyEmptyBSMArray1 passed: " + e.getMessage());
        } catch (Throwable e) {
            throw new RuntimeException("Expected ClassFormatError exception not thrown");
        }

        // 3. No BootstrapMethods Attribute array present`
        try {
            Class newClass = Class.forName("CondyNoBSMArray");
            throw new RuntimeException("Expected ClassFormatError exception not thrown");
        } catch (java.lang.ClassFormatError e) {
            if (!e.getMessage().contains("Missing BootstrapMethods attribute in class file")) {
                throw new RuntimeException("ClassFormatError thrown, incorrect message");
            }
            System.out.println("Test CondyNoBSMArray passed: " + e.getMessage());
        } catch (Throwable e) {
            throw new RuntimeException("Expected ClassFormatError exception not thrown");
        }
    }
}
