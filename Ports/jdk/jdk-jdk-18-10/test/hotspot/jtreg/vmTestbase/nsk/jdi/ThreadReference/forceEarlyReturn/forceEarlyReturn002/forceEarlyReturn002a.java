/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
package nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn002;

import nsk.share.jdi.*;

//    THIS TEST IS LINE NUMBER SENSITIVE

/*
 * Methods using TestClass1 were moved to the special class ClassUsingTestClass because of when these methods were in
 * the forceEarlyReturn002a class TestClass1 were loaded together with forceEarlyReturn002a but
 * this test expects that TestClass1 isn't loaded until method createExpectedValue() isn't called (see 6758252).
 *
 * NOTE: edit this file carefully, breakpoint line number is hardcoded
 */
class ClassUsingTestClass {

    public static final int breakpointLine = 45;

    public static final String breakpointMethodName = "testClassMethod";

    static Object expectedValue;

    TestClass1 testClassMethod() {
        System.out.println("Inside testClassMethod()"); // breakpointLine

        return null;
    }

     void createExpectedValue() {
        expectedValue = new TestClass1();
    }
}

public class forceEarlyReturn002a extends AbstractJDIDebuggee {

    private ClassUsingTestClass classUsingTestClass = new ClassUsingTestClass();

    public String[] doInit(String args[]) {
        args = super.doInit(args);

        Thread.currentThread().setName(mainThreadName);

        return args;
    }

    public static String mainThreadName = "MainThread";

    // call testClassMethod()
    public final static String COMMAND_CALL_OBJECT_METHOD = "callObjectMethod";

    // load TestClass1 and call testClassMethod()
    public final static String COMMAND_LOAD_CLASS_AND_CALL_OBJECT_METHOD = "testObjectMethod";

    public boolean parseCommand(String command) {
        if (super.parseCommand(command))
            return true;

        if (command.equals(COMMAND_CALL_OBJECT_METHOD)) {
            classUsingTestClass.testClassMethod();
            return true;
        } else if (command.equals(COMMAND_LOAD_CLASS_AND_CALL_OBJECT_METHOD)) {
            classUsingTestClass.createExpectedValue();
            Object value = classUsingTestClass.testClassMethod();

            if (ClassUsingTestClass.expectedValue != value) {
                setSuccess(false);
                log.complain("Unexpected result of testClassMethod: " + value);
            }

            return true;
        }

        return false;
    }

    public static void main(String[] args) {
        new forceEarlyReturn002a().doTest(args);
    }
}
