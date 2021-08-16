/*
 * Copyright (c) 2001, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4467564
 * @summary Test the popping of frames in synchronous context
 *          (that is, when stopped at an event)
 * @author Robert Field
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g PopSynchronousTest.java
 * @run driver PopSynchronousTest
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;

    /********** target program **********/

class PopSynchronousTarg {
    static String s;
    static PopSynchronousTarg sole;

    synchronized void a(int y, boolean w) {
        if (y == 6 && w) {
            s += "@";
        } else {
            s += " aArgFail ";
        }
    }
    String b(String h) {
        if (h.equals("yo")) {
            s += "[";
        } else {
            s += " bArgFail ";
        }
        a(6, true);
        s += "]";
        return s;
    }
    long c() {
        s += "<";
        synchronized (s) {
            b("yo");
        }
        s += ">";
        return 17;
    }
    static void p() {
        s += "(";
        if (sole.c() != 17) {
            s += " cReturnFail ";
        }
        s += ")";
    }
    static void report() {
    }
    public static void main(String[] args){
        s = new String();
        sole = new PopSynchronousTarg();
        for (int i = 0; i < 100; ++i) {
            p();
            // System.out.println(s);
            report();
        }
    }
}

    /********** test program **********/

public class PopSynchronousTest extends TestScaffold {
    ReferenceType targetClass;
    ThreadReference mainThread;
    int stateIndex = 0;
    String expected = "";
    static final String[] calls =  {"a", "b", "c", "p", "main"};
    static final int popMax = calls.length - 1;
    static final String[] states =
    {"main-i", "p-e", "p-i", "c-e", "c-i", "b-e", "b-i", "a-e", "a-l", "b-l", "c-l", "p-l", "main-r", "report-e"};
    static final String[] output =
    {"",       "",    "(",   "",    "<",   "",    "[",   "",    "@",   "]",   ">",   ")",   "",       ""};


    PopSynchronousTest (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        new PopSynchronousTest(args).startTests();
    }


    /********** test assist **********/

    StackFrame frameFor(String methodName) throws Exception {
        Iterator it = mainThread.frames().iterator();

        while (it.hasNext()) {
            StackFrame frame = (StackFrame)it.next();
            if (frame.location().method().name().equals(methodName)) {
                return frame;
            }
        }
        failure("FAIL: " + methodName + " not on stack");
        return null;
    }

    String actual() throws Exception {
        Field field = targetClass.fieldByName("s");
        StringReference sr = (StringReference)(targetClass.getValue(field));
        return sr.value();
    }

    void checkExpected() throws Exception {
        if (!actual().equals(expected)) {
            failure("FAIL: expected value: " + expected +
                    " got: " + actual());
        }
    }

    int methodIndex(String methodName) {
        for (int i = 0; i < popMax; ++i) {
            if (methodName.equals(calls[i])) {
                return i;
            }
        }
        return -1;
    }

    boolean isTop(String methodName) throws Exception {
        return mainThread.frame(0).location().method().name().equals(methodName);
    }

    void checkTop(String methodName, boolean atStart) throws Exception {
        Location loc = mainThread.frame(0).location();
        Method meth = loc.method();
        String name = meth.name();
        if (!isTop(methodName)) {
            failure("FAIL: expected " + methodName +
                    " at top of stack, instead: " + name);
        } else if ((meth.location().codeIndex() == loc.codeIndex()) != atStart) {
            failure("FAIL: not at expect position: " + loc.codeIndex());
        }
    }

    void checkState() throws Exception {
        String name = states[stateIndex];
        int dash = name.indexOf('-');
        String methodName = name.substring(0,dash);
        String posName = name.substring(dash+1);
        checkTop(methodName, posName.equals("e"));
        checkExpected();
    }

    void incrementState() {
        stateIndex = (stateIndex + 1) % (states.length);
    }

    void resetState(String stateName) {
        for (int i=0; i < states.length; ++i) {
            if (states[i].equals(stateName)) {
                stateIndex = i;
                return;
            }
        }
        failure("TEST FAILURE: cannot find state: " + stateName);
    }

    void resetExpected() throws Exception {
        println("Current value: " + actual());
        Field field = targetClass.fieldByName("s");
        expected = "";
        ((ClassType)targetClass).setValue(field, vm().mirrorOf(expected));
    }

    void stateTo(String stateName) {
        do {
            incrementState();
            expected += output[stateIndex];
        } while(!states[stateIndex].equals(stateName));
    }

    void resumeTo(String methodName) throws Exception {
        List meths = targetClass.methodsByName(methodName);
        Method meth = (Method)(meths.get(0));
        resumeTo(meth.location());
        stateTo(methodName + "-e");
        checkState();
    }

    void pop(String methodName) throws Exception {
        mainThread.popFrames(frameFor(methodName));
        resetState(methodName + "-e");
        --stateIndex;
        checkState();
    }

    void reenter(String methodName) throws Exception {
        pop(methodName);
        stepIntoInstruction(mainThread);
        incrementState();
        checkState();
    }

    /********** test core **********/

    protected void runTests() throws Exception {
        /*
         * Get to the top of main()
         * to determine targetClass and mainThread
         */
        BreakpointEvent bpe = startToMain("PopSynchronousTarg");
        targetClass = bpe.location().declaringType();
        mainThread = bpe.thread();

        /*
         * Testing
         */

        /* individual tests */
        for (int i = 0; i < popMax; ++i) {
            String from = calls[i];
            for (int j = i; j < popMax; ++j) {
                String to = calls[j];
                String prev = calls[j+1];
                println("TEST pop from '" + from + "' to '" + to + "'");
                resumeTo(from);
                reenter(to);
                resumeTo("report");
                resetExpected();
            }
        }

        /* sequential tests */

        println("TEST pop a b c p");
        resumeTo("a");
        pop("a");
        pop("b");
        pop("c");
        pop("p");
        resumeTo("report");
        resetExpected();

        println("TEST pop a c p");
        resumeTo("a");
        pop("a");
        pop("c");
        pop("p");
        resumeTo("report");
        resetExpected();

        println("TEST stress a");
        resumeTo("a");
        for (int i = 0; i < 100; ++i) {
            reenter("a");
        }
        resumeTo("report");
        resetExpected();

        println("TEST stress c");
        resumeTo("c");
        for (int i = 0; i < 100; ++i) {
            reenter("c");
        }
        resumeTo("report");
        resetExpected();

        /*
         * we are done, get rid of target
         */
        vm().dispose();

        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("PopSynchronousTest: passed");
        } else {
            throw new Exception("PopSynchronousTest: failed");
        }
    }
}
