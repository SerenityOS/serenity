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
 * @bug 8176147
 * @summary Throw ClassFormatError exception for multiple Signature attributes
 * @compile DupSignatureAttrs.jcod
 * @run main TestDupSignatureAttr
 */

public class TestDupSignatureAttr {
    public static void main(String args[]) throws Throwable {

        System.out.println("Regression test for bug 8176147");

        String[] badClasses = new String[] {
            "DupClassSigAttrs",
            "DupMthSigAttrs",
            "DupFldSigAttrs",
        };
        String[] messages = new String[] {
            "Multiple Signature attributes in class file",
            "Multiple Signature attributes for method",
            "Multiple Signature attributes for field",
        };

        for (int x = 0; x < badClasses.length; x++) {
            try {
                Class newClass = Class.forName(badClasses[x]);
                throw new RuntimeException("Expected ClassFormatError exception not thrown");
            } catch (java.lang.ClassFormatError e) {
                if (!e.getMessage().contains(messages[x])) {
                    throw new RuntimeException("Wrong ClassFormatError exception thrown: " +
                                               e.getMessage());
                }
            }
        }

        // Multiple Signature attributes but no duplicates.
        Class newClass = Class.forName("OkaySigAttrs");
    }
}
