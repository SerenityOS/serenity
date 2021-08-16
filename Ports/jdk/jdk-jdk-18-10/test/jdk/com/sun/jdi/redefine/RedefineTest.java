/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4287595
 * @bug 4462989
 * @bug 4531511
 * @summary Test class redefinition
 *
 * @author Robert Field
 *
 * @library ..
 * @library /test/lib
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g RedefineTest.java
 * @run driver RedefineTest
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import jdk.test.lib.Utils;
import jdk.test.lib.compiler.InMemoryJavaCompiler;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.io.*;

    /********** target program **********/

class RedefineTarg {
    public static void show(String where){
        System.out.println("Returned: " + where);
    }

    public static void lastly(String where){
    }

    public static void main(String[] args){
        RedefineSubTarg sub = new RedefineSubTarg();
        String where = "";
        for (int i = 0; i < 5; ++i) {
            where = sub.foo(where);
            show(where);
        }
        lastly(where);
    }
}

    /********** test program **********/

public class RedefineTest extends TestScaffold {
    ReferenceType targetClass;
    static final String expected ="Boring Boring Different Boring Different ";
    int repetitionCount = 0;
    boolean beforeRedefine = true;

    RedefineTest (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        new RedefineTest(args).startTests();
    }

    /********** event handlers **********/

    public void methodEntered(MethodEntryEvent event) {
        Method meth = event.location().method();
        ThreadReference thread = event.thread();

        if (meth.name().equals("foo")) {
            ++repetitionCount;
            beforeRedefine = true;
            try {
                expectNonObsolete(thread);
                inspectLineNumber(event, thread.frame(0));

                doRedefine(thread);
                beforeRedefine = false;

                switch (repetitionCount) {
                case 1:
                case 5:
                    expectNonObsolete(thread);
                    inspectLineNumber(event, thread.frame(0));
                    break;
                case 2:
                case 3:
                case 4:
                    expectObsolete(thread);
                    inspectLineNumber(event, thread.frame(0));
                    break;
                }


            } catch (Exception exc) {
                failure("Test Failure: unexpected exception - " + exc);
                exc.printStackTrace();
            }
        }
    }

    public void breakpointReached(BreakpointEvent event) {
        ThreadReference thread = event.thread();
        try {
            StackFrame frame = thread.frame(0);
            LocalVariable lv = frame.visibleVariableByName("where");
            Value vWhere = frame.getValue(lv);
            String remoteWhere = ((StringReference)vWhere).value();
            println("Value of where: " + remoteWhere);
            if (!remoteWhere.equals(expected)) {
                failure("FAIL: expected result string: '" + expected +
                        "' got: '" + remoteWhere + "'");
            }
        } catch (Exception thr) {
            failure("Test Failure: unexpected exception: " + thr);
        }
    }

    /********** test assists **********/

    void expectNonObsolete(ThreadReference thread) throws Exception {
        if (isObsolete(thread)) {
            failure("FAIL: Method should NOT be obsolete");
        } else {
            println("as it should be, not obsolete");
        }
    }

    void expectObsolete(ThreadReference thread) throws Exception {
        if (isObsolete(thread)) {
            println("obsolete like it should be");
        } else {
            failure("FAIL: Method should be obsolete");
        }
    }

    void inspectLineNumber(LocatableEvent event, StackFrame frame) throws Exception {
        /*
         * For each value of repetitionCount, use the beforeRedefine
         * boolean to distinguish the time before and after the actual
         * redefinition takes place.  Line numbers are inspected both
         * before and after each redefine.
         */
        int n = -1;
        int expectedLine = -1;
        switch (repetitionCount) {
        case 1:
            expectedLine = 4;
            break;
        case 2:
            expectedLine = beforeRedefine ? 4:21;
            break;
        case 3:
            expectedLine = beforeRedefine ? 21:4;
            break;
        case 4:
            expectedLine = beforeRedefine ? 4:21;
            break;
        case 5:
            /* The class won't be redefined on this iteration (look
             * for a java.lang.UnsupportedOperationException instead)
             * so expected line stays the same as last successful
             * redefine.
             */
            expectedLine = 21;
            break;
        }
        Method method = event.location().method();
        if (frame.location().method().isObsolete()) {
            /*
             * Then skip. Obsolete methods are not interesting to
             * inspect.
             */
            println("inspectLineNumber skipping obsolete method " + method.name());
        } else {
            n = method.location().lineNumber();
            int m = frame.location().lineNumber();
            if ((n != expectedLine) || (n != m)) {
                failure("Test Failure: line number disagreement: " +
                        n + " (event) versus " + m + " (frame) versus " + expectedLine +
                        " (expected)");
            } else {
                println("inspectLineNumber in method " + method.name() + " at line " + n);
            }
        }
    }

    boolean isObsolete(ThreadReference thread) throws Exception {
        StackFrame frame = thread.frame(0);
        Method meth = frame.location().method();
        return meth.isObsolete();
    }

    void doRedefine(ThreadReference thread) throws Exception {
        Exception receivedException = null;
        String fileName = "notThis";

        switch (repetitionCount) {
        case 1:
            fileName = "RedefineSubTarg.class";
            break;
        case 2:
            fileName = "Different_RedefineSubTarg.class";
            break;
        case 3:
            fileName = "RedefineSubTarg.class";
            break;
        case 4:
            fileName = "Different_RedefineSubTarg.class";
            break;
        case 5:
            fileName = "SchemaChange_RedefineSubTarg.class";
            break;
        }
        File phyl = new File(fileName);
        byte[] bytes = new byte[(int)phyl.length()];
        InputStream in = new FileInputStream(phyl);
        in.read(bytes);
        in.close();

        Map map = new HashMap();
        map.put(findReferenceType("RedefineSubTarg"), bytes);

        println(System.getProperty("line.separator") + "Iteration # " + repetitionCount +
                " ------ Redefine as: " + fileName);
        try {
            vm().redefineClasses(map);
        } catch (Exception thr) {
            receivedException = thr;
        }
        switch (repetitionCount) {
        case 5:
            if (receivedException == null) {
                failure("FAIL: no exception; expected: UnsupportedOperationException");
            } else if (receivedException instanceof UnsupportedOperationException) {
                println("Received expected exception: " + receivedException);
            } else {
                failure("FAIL: got exception: " + receivedException +
                        ", expected: UnsupportedOperationException");
            }
            break;
        default:
            if (receivedException != null) {
                failure("FAIL: unexpected exception: " +
                        receivedException);
            }
            break;
        }
        return;
    }

    // prepares .class file for redefined RedefineSubTarg class:
    // - compiles <fileName>.java from test source dir;
    // - saves compiled class <fileName>.class.
    protected void prepareRedefinedClass(String fileName) throws Exception {
        Path srcJavaFile = Paths.get(Utils.TEST_SRC).resolve(fileName + ".java");
        Path dstClassFile = Paths.get(".").resolve(fileName + ".class");
        byte[] compiledData = InMemoryJavaCompiler.compile("RedefineSubTarg",
                new String(Files.readAllBytes(srcJavaFile), StandardCharsets.UTF_8),
                "-g");
        Files.write(dstClassFile, compiledData);
    }

    /********** test core **********/

    protected void runTests() throws Exception {
        // prepare redefined .class files
        prepareRedefinedClass("Different_RedefineSubTarg");
        prepareRedefinedClass("SchemaChange_RedefineSubTarg");
        prepareRedefinedClass("RedefineSubTarg");

        BreakpointEvent bpe = startToMain("RedefineTarg");
        targetClass = bpe.location().declaringType();
        EventRequestManager erm = vm().eventRequestManager();

        /*
         * Method entry in sub targ
         */
        MethodEntryRequest mee = erm.createMethodEntryRequest();
        mee.addClassFilter("RedefineSubTarg");
        mee.enable();

        /*
         * BP at end to get value
         */
        List lastlys = targetClass.methodsByName("lastly");
        if (lastlys.size() != 1) {
            throw new Exception ("TestFailure: Expected one 'lastly' method, found: " +
                                 lastlys);
        }
        Location loc = ((Method)(lastlys.get(0))).location();
        EventRequest req = erm.createBreakpointRequest(loc);
        req.enable();

        // Allow application to complete and shut down
        listenUntilVMDisconnect();

        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("RedefineTest: passed");
        } else {
            throw new Exception("RedefineTest: failed");
        }
    }
}
