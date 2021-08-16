/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test Method.variables() and the like.
 * @author Robert Field
 *
 * @run build TestScaffold VMConnection
 * @run compile -g Vars.java
 * @run driver Vars
 */

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import java.util.*;

/*
 * This class is internal
 */
abstract class AbstractTestVars {
    abstract float test1(String blah, int i);
    native int test2(double k, boolean b);
    String test3(short sh, long lo) {
        String st = "roses";
        return st;
    }
}

/*
 * This class is internal
 */
class TestVars extends AbstractTestVars {
    float test1(String blah, int i) {
        return (float)1.1;
    }

    void hi() {
        return;
    }

    public static void main(String[] args) throws Exception {
        new TestVars().hi();
        return;
    }
}

/*
 * "Vars" test runs TestVars and makes LocalVariable queries
 */
public class Vars extends TestScaffold {

    boolean failed = false;

    Vars(String args[]) {
        super(args);
    }

    public static void main(String[] args) throws Exception {
        new Vars(args).runTests();
    }

    static final int VARIABLES = 1;
    static final int BYNAME = 2;
    static final int ARGUMENTS = 3;

    String testCase(Method method, int which) {
        try {
            List vars;
            switch (which) {
                case VARIABLES:
                    vars = method.variables();
                    break;
                case BYNAME:
                    vars = method.variablesByName("st");
                    break;
                case ARGUMENTS:
                    vars = method.arguments();
                    break;
                default:
                    throw new InternalException("should not happen");
            }
            StringBuffer sb = new StringBuffer();
            for (Iterator it = vars.iterator(); it.hasNext(); ) {
                LocalVariable lv = (LocalVariable)it.next();
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(lv.name());
            }
            return sb.toString();
        } catch (Exception exc) {
            String st = exc.getClass().getName();
            int inx = st.lastIndexOf('.');
            return st.substring(inx+1);
        }
    }

    /**
     * Sets failed if fails.
     */
    void test(Method method, int which, String name, String expected) {
        String got = testCase(method, which);
        if (got.equals(expected)) {
            System.out.println(name + ": got expected: " + got);
        } else {
            failed = true;
            System.out.println(name + ": ERROR expected: " + expected);
            System.out.println("      got: " + got);
        }
    }

    void test2(Method method, int which, String name, String expected, String expected2) {
        String got = testCase(method, which);
        if (got.equals(expected) || got.equals(expected2)) {
            System.out.println(name + ": got expected: " + got);
        } else {
            failed = true;
            System.out.println(name + ": ERROR expected: " + expected);
            System.out.println("      got: " + got);
        }
    }

    protected void runTests() throws Exception {
        /*
         * Get to a point where the classes are loaded.
         */
        BreakpointEvent bp = startTo("TestVars", "hi", "()V");

        /*
         * These classes should have no line numbers, except for
         * one in the implicit constructor.
         */
        ReferenceType rt = findReferenceType("AbstractTestVars");
        if (rt == null) {
            throw new Exception("AbstractTestVars: not loaded");
        }
        Method method = findMethod(rt, "test1", "(Ljava/lang/String;I)F");
        if (method == null) {
            throw new Exception("Method not found");
        }
        test(method, VARIABLES, "abstract/variables",
             "AbsentInformationException");
        test(method, BYNAME, "abstract/variablesByName",
             "AbsentInformationException");
        test(method, ARGUMENTS, "abstract/arguments",
             "AbsentInformationException");

        method = findMethod(rt, "test2", "(DZ)I");
        if (method == null) {
            throw new Exception("Method not found");
        }
        test(method, VARIABLES, "native/variables",
             "AbsentInformationException");
        test(method, BYNAME, "native/variablesByName",
             "AbsentInformationException");
        test(method, ARGUMENTS, "native/arguments",
             "AbsentInformationException");

        method = findMethod(rt, "test3", "(SJ)Ljava/lang/String;");
        if (method == null) {
            throw new Exception("Method not found");
        }
        // javac can put these in whatever order it desires.  hopper
        // does it one way and mantis another.
        test2(method, VARIABLES, "normal/variables", "sh,lo,st", "st,sh,lo");
        test(method, BYNAME, "normal/variablesByName", "st");
        test(method, ARGUMENTS, "normal/arguments", "sh,lo");

        // Allow application to complete
        resumeToVMDisconnect();

        if (failed) {
            throw new Exception("Vars: failed");
        } else {
            System.out.println("Vars: passed");
        }
    }
}
