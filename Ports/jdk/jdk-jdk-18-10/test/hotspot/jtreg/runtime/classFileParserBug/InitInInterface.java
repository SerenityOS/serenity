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
 * @bug 8139069
 * @summary Check that any method named <init> in an interface causes ClassFormatError
 * @compile nonvoidinit.jasm voidinit.jasm
 * @run main InitInInterface
 */

// Test that an <init> method is not allowed in interfaces.
public class InitInInterface {
    public static void main(String args[]) throws Throwable {

        System.out.println("Regression test for bug 8130183");
        try {
            Class newClass = Class.forName("nonvoidinit");
            throw new RuntimeException(
                 "ClassFormatError not thrown for non-void <init> in an interface");
        } catch (java.lang.ClassFormatError e) {
            if (!e.getMessage().contains("Interface cannot have a method named <init>")) {
                throw new RuntimeException("Unexpected exception nonvoidint: " + e.getMessage());
            }
        }
        try {
            Class newClass = Class.forName("voidinit");
            throw new RuntimeException(
                 "ClassFormatError not thrown for void <init> in an interface");
        } catch (java.lang.ClassFormatError e) {
            if (!e.getMessage().contains("Interface cannot have a method named <init>")) {
                throw new RuntimeException("Unexpected exception voidint: " + e.getMessage());
            }
        }
    }
}
