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

/*
 * Test that a string that is the same as its hidden class name does not
 * get clobbered when the JVM changes the hidden class's name.
 */
public class NameInString implements Test {

    private String realTest() {
        return "NameInString";
    }

    public void test() {
        String result = realTest();
        // Make sure that the Utf8 constant pool entry for "NameInString" is okay.
        if (!result.substring(0, 6).equals("NameIn") ||
            !result.substring(6).equals("String")) {
            throw new RuntimeException("'NameInString is bad: " + result);
        }

    }
}
