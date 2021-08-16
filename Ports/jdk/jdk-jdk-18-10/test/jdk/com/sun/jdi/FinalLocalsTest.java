/*
 * Copyright (c) 2000, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4326648 4768329
 * @summary Test to verify that table entries are generated for all final
 *          locals when a class is built for debug, even if they could be
 *          inlined otherwise.
 * @author Tim Bell
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g FinalLocalsTest.java
 * @run driver FinalLocalsTest
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;

    /********** target program **********/

class FinalLocalsTarg {
    public void test1 (final int t, int k){
        String s1 = "first";
        final int z = 0;
        if (true) {
            final float r = 10.00f;
            boolean b = true;
            System.out.println(r);
        }
    }
    public void hi(){
        return;
    }
    public static void main(String[] args) {
        System.out.print("in FinalLocalsTarg:");
        new FinalLocalsTarg().hi();
        return;
    }
}

    /********** test program **********/

public class FinalLocalsTest extends TestScaffold {
    ReferenceType targetClass;
    ThreadReference mainThread;

    FinalLocalsTest (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        new FinalLocalsTest(args).startTests();
    }

    /********** test core **********/
    static final int VARIABLES = 1;
    static final int BYNAME = 2;
    static final int ARGUMENTS = 3;
    /*
     * Take a String containing comma separated values
     * and return those values in a TreeSet.
     */
    private TreeSet buildSet(String in) {
        TreeSet result = new TreeSet();
        StringTokenizer tt = new StringTokenizer(in, ",");
        while (tt.hasMoreTokens()) {
            String ss = tt.nextToken();
            if (! result.add(ss)) {
                failure ("Duplicate entry \"" + ss + "\" in string: " +
                         in + " is not allowed");
            }
        }
        return result;
    }

    private void test(Method method, int which, String name, String expected) {
        String got = testCase(method, which);
        System.out.println(" test() comparing expected = " + expected +
                           " to got = " + got);
        TreeSet expectedSet = buildSet(expected);
        TreeSet gotSet = buildSet(got);

        while (! expectedSet.isEmpty()) {
            String ee = (String)expectedSet.first();
            expectedSet.remove(ee);
            if (gotSet.contains(ee)) {
                gotSet.remove(ee);
            } else {
                failure (name + " Expected entry \"" + ee + "\" not found");
            }
        }

        //assert expectedSet.isEmpty() : name + " expected set should have been emptied";

        if (! gotSet.isEmpty()) {
            StringBuffer sb = new StringBuffer();
            Iterator it = gotSet.iterator();
            while (it.hasNext()) {
                sb.append(it.next());
                if (it.hasNext()) {
                    sb.append(",");
                }
            }
            failure (name + " Unexpected entries found: " + sb.toString());
        }
    }

    String testCase(Method method, int which) {
        try {
            List vars;
            switch (which) {
                case VARIABLES:
                    vars = method.variables();
                    break;
                case BYNAME:
                    vars = method.variablesByName("s1");
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

    protected void runTests() throws Exception {
        /*
         * Get to the top of main()
         * to determine targetClass and mainThread
         */
        BreakpointEvent bpe = startToMain("FinalLocalsTarg");
        targetClass = bpe.location().declaringType();
        mainThread = bpe.thread();
        EventRequestManager erm = vm().eventRequestManager();

        /*
         * Get to a point where the classes are loaded.
         */
        BreakpointEvent bp = resumeTo("FinalLocalsTarg", "hi", "()V");

        ReferenceType rt = findReferenceType("FinalLocalsTarg");
        if (rt == null) {
            throw new Exception("FinalLocalsTarg: not loaded");
        }

        /*
         * Inspect the LocalVariableTable attributes for method "test1"
         * NOTE: .class files compiled with some versions of javac will
         * give results listed in different order.  That's OK.
         */
        Method method = findMethod(rt, "test1", "(II)V");
        if (method == null) {
            throw new Exception("Method not found");
        }
        test(method, VARIABLES, "VARIABLES",
             "t,k,s1,z,r,b");
        test(method, BYNAME, "BYNAME",
             "s1");
        test(method, ARGUMENTS, "ARGUMENTS",
             "t,k");

        /*
         * All Done.  Resume the target listening for events
         */
        listenUntilVMDisconnect();

        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("FinalLocalsTest: passed");
        } else {
            throw new Exception("FinalLocalsTest: failed");
        }
    }
}
