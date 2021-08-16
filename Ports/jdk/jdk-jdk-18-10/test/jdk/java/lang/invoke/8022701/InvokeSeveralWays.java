/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.InvocationTargetException;

/**
 * Tries various ways of ultimately invoking MethodSupplier.m(),
 * except that m has been made inaccessible and some exception should be
 * thrown instead.
 */
public class InvokeSeveralWays {
    public static int test(String args[], Class expected) throws Exception {
        int failures = 0;
        try {
            Class.forName("Invoker").getMethod("invoke").invoke(null);
            System.out.println("FAIL: No exception throw, probably failed to load modified bytecodes for MethodSupplier");
            failures++;
        } catch (InvocationTargetException e) {
            Throwable c = e.getCause();
            if (expected.isInstance(c))
                System.out.println("EXPECTED: " + expected.getName() + ", "+ c);
            else {
                failures++;
                System.out.println("FAIL: Unexpected wrapped exception " + c);
                e.printStackTrace(System.out);
            }
        } catch (Throwable e) {
            failures++;
            System.out.println("FAIL: Unexpected exception has been caught " + e);
            e.printStackTrace(System.out);
        }
        System.out.println();
        try {
            Class.forName("Invoker").getMethod("invoke2").invoke(null);
            System.out.println("FAIL: No exception throw, probably failed to load modified bytecodes for MethodSupplier");
            failures++;
        } catch (InvocationTargetException e) {
            Throwable c = e.getCause();
            if (expected.isInstance(c))
               System.out.println("EXPECTED: " + expected.getName() + ", "+ c);
            else {
               failures++;
               System.out.println("FAIL: Unexpected wrapped exception " + c);
               e.printStackTrace(System.out);
            }
        } catch (Throwable e) {
            failures++;
            System.out.println("FAIL: Unexpected exception has been caught " + e);
            e.printStackTrace(System.out);
        }
        System.out.println();
        try {
            Invoker.invoke();
            System.out.println("FAIL: No exception throw, probably failed to load modified bytecodes for MethodSupplier");
            failures++;
        } catch (Throwable e) {
            if (expected.isInstance(e))
                System.out.println("EXPECTED: " + expected.getName() + ", "+ e);
            else {
                failures++;
                System.out.println("FAIL: Unexpected exception has been caught " + e);
                e.printStackTrace(System.out);
            }
        }
        System.out.println();
        try {
            Invoker.invoke2();
            System.out.println("FAIL: No exception throw, probably failed to load modified bytecodes for MethodSupplier");
            failures++;
         } catch (Throwable e) {
            if (expected.isInstance(e))
               System.out.println("EXPECTED: " + expected.getName() + ", "+ e);
            else {
                failures++;
                System.out.println("FAIL: Unexpected exception has been caught " + e);
                e.printStackTrace(System.out);
            }
        }
        System.out.println();
        if (failures > 0) {
          System.out.println("Saw " + failures + " failures");
        }
        return failures;
    }
}
