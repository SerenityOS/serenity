/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8268720
 * @summary Constant pool NameAndType entries with valid but incompatible method
 *          name and signature shouldn't cause an exception until referenced by
 *          a method_ref.
 * @compile nonVoidInitSig.jcod
 * @run main/othervm -Xverify:remote NameAndTypeSig
 */

// Test constant pool NameAndType descriptors with valid but incompatible method
// names and signatures.
public class NameAndTypeSig {
    public static void main(String args[]) throws Throwable {

        // Test that an unreferenced NameAndType with a valid name and signature
        // is allowed even for name and signature pairs such as <init>()D.
        Class newClass = Class.forName("nonVoidInitSig");

        // Test that a NameAndType with a valid name and signature is allowed for
        // name and signature pairs such as <init>()D, but not allowed by a cp
        // Method_ref.
        try {
            Class newClass2 = Class.forName("nonVoidInitSigCFE");
            throw new RuntimeException("Expected ClassFormatError exception not thrown");
        } catch (java.lang.ClassFormatError e) {
            if (!e.getMessage().contains("Method \"<init>\" in class nonVoidInitSigCFE has illegal signature")) {
                throw new RuntimeException("Wrong ClassFormatError exception: " + e.getMessage());
            }
        }

        // Test that a NameAndType with a valid name and invalid signature throws a
        // ClassFormatError exception with a message containing the name <init> and
        // the bad signature.
        try {
            Class newClass2 = Class.forName("voidInitBadSig");
            throw new RuntimeException("Expected ClassFormatError exception not thrown");
        } catch (java.lang.ClassFormatError e) {
            if (!e.getMessage().contains("Method \"<init>\" in class voidInitBadSig has illegal signature \"()))V\"")) {
                throw new RuntimeException("Wrong ClassFormatError exception: " + e.getMessage());
            }
        }
        System.out.println("Test NameAndTypeSig passed.");
    }
}
