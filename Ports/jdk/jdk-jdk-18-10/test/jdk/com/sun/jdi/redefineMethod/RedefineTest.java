/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4628726
 * @summary Test class redefinition - method data line numbers and local vars,
 * @author Robert Field
 *
 * @library ..
 * @library /test/lib
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g RedefineTest.java
 * @run driver RedefineTest -repeat 3
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
    public static void main(String[] args){
        RedefineSubTarg.stemcp();
        RedefineSubTarg sub = new RedefineSubTarg();
        sub.bottom();
        RedefineSubTarg.stnemcp();
        RedefineSubTarg.stemcp();
    }
}

    /********** test program **********/

public class RedefineTest extends TestScaffold {
    static int redefineRepeat = 1;
    int bpCnt = 0;

    // isObsolete, linenumber, lv name, lv value, lv isArg
    String[] before = {
    "+ 3",
    "+ 6 eights 888 T",
    "+ 11 rot 4 F",
    "+ 15",
    "+ 20 myArg 56 T paramy 12 F",
    "+ 24",
    "+ 28",
    "+ 33" };
    String[] after = {
    "+ 5",
    "O",
    "O",
    "+ 16",
    "+ 21 whoseArg 56 T parawham 12 F",
    "+ 25",
    "O",
    "+ 34" };
    String[] shorter = {
    "+ 5",
    "+ 9 eights 88 T",
    "+ 13",
    "+ 16",
    "+ 21 whoseArg 56 T parawham 12 F",
    "+ 25" };
    String[] refresh = {
    "+ 5",
    "+ 9 eights 88 T",
    "+ 13",
    "+ 16",
    "+ 21 whoseArg 56 T parawham 12 F",
    "+ 25",
    "+ 29",
    "+ 34" };
    int[] bps = {7, 12, 16, 21, 25, 30, 34};
    String[][] bpPlaces = {
        {"+ 16"},
        {"+ 21 myArg 56 T paramy 12 F"},
        {"+ 25"},
        {"+ 34"} };

    static String[] processArgs(String args[]) {
        if (args.length > 0 && args[0].equals("-repeat")) {
            redefineRepeat = Integer.decode(args[1]).intValue();
            String[] args2 = new String[args.length - 2];
            System.arraycopy(args, 2, args2, 0, args.length - 2);
            return args2;
        } else {
            return args;
        }
    }

    RedefineTest (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        new RedefineTest(processArgs(args)).startTests();
    }


    /********** event handlers **********/

    public void breakpointReached(BreakpointEvent event) {
        println("Got BreakpointEvent - " + event);
        try {
            checkFrames(event.thread(), bpPlaces[bpCnt++]);
            if (bpCnt >= bpPlaces.length) {
                eventRequestManager().deleteAllBreakpoints();
            }
        } catch (Exception exc) {
            failure("FAIL: breakpoint checking threw " + exc);
        }
    }

    /********** test assists **********/

    // isObsolete, linenumber, lv name, lv value, lv isArg
    // equals: ref type (always), method (not obsolete)
    void checkFrames(ThreadReference thread, String[] matchList) throws Exception {
        for (int i = 0; i < matchList.length; ++i) {
            String match = matchList[i];
            StackFrame frame = thread.frame(i);
            Location loc = frame.location();
            ReferenceType refType = loc.declaringType();
            Method meth = loc.method();
            String errInfo = "\nframe " + i + ": " + loc + "\n  match: " + match;
            if (!findReferenceType("RedefineSubTarg").equals(refType)) {
                 failure("FAIL: Bad reference type - " + errInfo);
                 return; // might be bad class, but might have run past bottom
            }
            StringTokenizer st = new StringTokenizer(match);
            boolean expectObs = st.nextToken().equals("O");
            println("Frame " + i + ": " + meth);
            if (meth.isObsolete()) {
                if (!expectObs) {
                    failure("FAIL: Method should NOT be obsolete - " + errInfo);
                }
            } else {
                if (expectObs) {
                    failure("FAIL: Method should be obsolete - " + errInfo);
                    break; // no more data to read
                }
                if (!findMethod(refType, meth.name(), meth.signature()).equals(meth)) {
                    failure("FAIL: Non matching method - " + errInfo);
                }
                int line = loc.lineNumber();
                if (line != Integer.parseInt(st.nextToken())) {
                    failure("FAIL: Unexpected line number: " + errInfo);
                }
                // local var matching
                int lvCnt = 0;
                while (st.hasMoreTokens()) {
                    ++lvCnt;
                    String lvName = st.nextToken();
                    int lvValue = Integer.parseInt(st.nextToken());
                    boolean isArg = st.nextToken().equals("T");
                    LocalVariable lv = frame.visibleVariableByName(lvName);
                    if (lv == null) {
                        failure("FAIL: local var not found: '" + lvName +
                                "' -- " + errInfo);
                    } else {
                        Value val = frame.getValue(lv);
                        int ival = ((IntegerValue)val).value();
                        if (ival != lvValue) {
                            failure("FAIL: expected value: '" + lvValue +
                                    "' got: '" + ival + "' -- " + errInfo);
                        }
                        if (lv.isArgument() != isArg) {
                            failure("FAIL: expected argument: '" + isArg +
                                    "' got: '" + lv.isArgument() + "' -- " + errInfo);
                        }
                    }
                }
                List locals = frame.visibleVariables();
                if (locals.size() != lvCnt) {
                        failure("FAIL: expected '" + lvCnt +
                                "' locals were '" + locals.size() +
                                "' -- " + errInfo + "' -- " + locals);
                }
            }
        }
    }


    void doRedefine(byte[] compiledClass) throws Exception {
        Map map = new HashMap();
        map.put(findReferenceType("RedefineSubTarg"), compiledClass);

        try {
            for (int i = 0; i < redefineRepeat; ++i) {
                vm().redefineClasses(map);
            }
        } catch (Exception thr) {
            failure("FAIL: unexpected exception: " + thr);
        }
    }

    ThreadReference toTop() {
        BreakpointEvent bpe = resumeTo("RedefineSubTarg", "top", "()V");
        return bpe.thread();
    }

    void setBP(int line) {
        try {
            Location loc = findLocation(findReferenceType("RedefineSubTarg"), line);
            final BreakpointRequest request =
                eventRequestManager().createBreakpointRequest(loc);
            request.enable();
        } catch (Exception exc) {
            failure("FAIL: Attempt to set BP at line " + line + " threw " + exc);
        }
    }

    private byte[] compileRedefinedClass(String srcJavaFile) throws Exception {
        Path src = Paths.get(Utils.TEST_SRC).resolve(srcJavaFile);
        return InMemoryJavaCompiler.compile("RedefineSubTarg",
                new String(Files.readAllBytes(src), StandardCharsets.UTF_8),
                "-g", "-classpath", Utils.TEST_CLASSES);
    }

    /********** test core **********/

    protected void runTests() throws Exception {
        byte[] compiled_Different_RedefineSubTarg = compileRedefinedClass("Different_RedefineSubTarg.java");
        byte[] compiled_RedefineSubTarg = compileRedefinedClass("RedefineSubTarg.java");

        startToMain("RedefineTarg");

        ThreadReference thread = toTop();

        println("------ Before Redefine ------");
        checkFrames(thread, before);

        println("------ After Redefine ------");
        doRedefine(compiled_Different_RedefineSubTarg);
        checkFrames(thread, after);

        println("------ Static 2 ------");
        toTop();
        checkFrames(thread, shorter);

        println("------ Instance ------");
        toTop();
        checkFrames(thread, shorter);

        println("------ Re-entered ------");
        toTop();
        checkFrames(thread, refresh);

        println("------ Breakpoints ------");
        doRedefine(compiled_RedefineSubTarg);
        for (int i = 0; i < bps.length; ++i) {
            setBP(bps[i]);
        }

        /*
         * resume the target listening for events
         */
        listenUntilVMDisconnect();

        if (bpCnt != bpPlaces.length) {
            failure("FAIL: Wrong number of breakpoints encountered: " + bpCnt);
        }

        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("RedefineTest(method): passed");
        } else {
            throw new Exception("RedefineTest(method): failed");
        }
    }
}
