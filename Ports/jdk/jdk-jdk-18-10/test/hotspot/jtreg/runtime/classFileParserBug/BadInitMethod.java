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
 * @bug 8130669
 * @summary VM prohibits <clinit> methods with return values
 * @compile nonvoidClinit.jasm
 * @compile clinitNonStatic.jasm
 * @compile clinitArg.jasm
 * @compile clinitArg51.jasm
 * @compile badInit.jasm
 * @run main/othervm -Xverify:all BadInitMethod
 */

// Test that non-void <clinit>, non-static <clinit>, and non-void
// <init> methods cause ClassFormatException's to be thrown.
public class BadInitMethod {
    public static void main(String args[]) throws Throwable {

        System.out.println("Regression test for bug 8130669");
        try {
            Class newClass = Class.forName("nonvoidClinit");
            throw new RuntimeException(
                "Expected ClassFormatError exception for non-void <clinit> not thrown");
        } catch (java.lang.ClassFormatError e) {
            System.out.println("Test BadInitMethod passed for non-void <clinit>");
        }

        try {
            Class newClass = Class.forName("clinitNonStatic");
            throw new RuntimeException(
                "Expected ClassFormatError exception for non-static <clinit> not thrown");
        } catch (java.lang.ClassFormatError e) {
            System.out.println("Test BadInitMethod passed for non-static <clinit>");
        }

        // <clinit> with args is allowed in class file version < 51.
        try {
            Class newClass = Class.forName("clinitArg");
        } catch (java.lang.ClassFormatError e) {
            throw new RuntimeException(
                "Unexpected ClassFormatError exception for <clinit> with argument in class file < 51");
        }

        // <clinit> with args is not allowed in class file version >= 51.
        try {
            Class newClass = Class.forName("clinitArg51");
            throw new RuntimeException(
                "Expected ClassFormatError exception for <clinit> with argument not thrown");
        } catch (java.lang.ClassFormatError e) {
            System.out.println("Test BadInitMethod passed for <clinit> with argument");
        }

        try {
            Class newClass = Class.forName("badInit");
            throw new RuntimeException(
                "Expected ClassFormatError exception for non-void <init> not thrown");
        } catch (java.lang.ClassFormatError e) {
            System.out.println("Test BadInitMethod passed for non-void <init>");
        }
    }
}
