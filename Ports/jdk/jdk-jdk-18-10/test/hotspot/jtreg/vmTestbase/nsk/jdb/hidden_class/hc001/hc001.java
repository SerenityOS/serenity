/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary JDB test for hidden classes
 *
 * @library /vmTestbase
 *          /test/lib
 * @modules jdk.jdi
 *          jdk.jdwp.agent
 * @build nsk.jdb.hidden_class.hc001.hc001a
 *
 * @run main/othervm
 *      nsk.jdb.hidden_class.hc001.hc001
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -jdb=${test.jdk}/bin/jdb
 *      -java.options="${test.vm.opts} ${test.java.opts}"
 *      -workdir=.
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdb.hidden_class.hc001;

import java.io.*;
import java.util.*;
import nsk.share.*;
import nsk.share.jdb.*;

public class hc001 extends JdbTest {
    static final String DEBUGGEE_CLASS    = hc001a.class.getTypeName();
    static final String HC_NAME_FIELD     = DEBUGGEE_CLASS + ".hcName";
    static final String MAIN_METHOD_NAME  = DEBUGGEE_CLASS + ".main";
    static final String EMPTY_METHOD_NAME = DEBUGGEE_CLASS + ".emptyMethod";
    static final String HC_METHOD_NAME    = "hcMethod";
    static final String HC_FIELD_NAME     = "hcField";
    static final int    MAX_SLEEP_CNT     = 3;

    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        debuggeeClass = DEBUGGEE_CLASS; // needed for JdbTest.runTest
        firstBreak = MAIN_METHOD_NAME;  // needed for JdbTest.runTest
        return new hc001().runTest(argv, out);
    }

    static boolean checkPattern(String[] arr, String pattern) {
        for (int idx = 0; idx < arr.length; idx++) {
            String str = arr[idx];
            if (str.indexOf(pattern) != -1) {
                return true;
            }
        }
        return false;
    }

    static void throwFailure(String msg) throws Failure {
        throw new Failure(msg);
    }

    /* Make a required cooperated setup with the debuggee:
     *  - transition the debuggee's execution to expected execution point
     *    (emptyMethod start) at which hidden class has been already loaded
     *  - get the hidden class name from the debuggee
     *  Return the hidden class name.
     */
    private String runPrologue() {
        String[] reply = null;

        log.println("\n### Debugger: runPrologue");

        // uncomment this line to enable verbose output from jdb
        // log.enableVerbose(true);

        // run jdb command "stop in"
        jdb.setBreakpointInMethod(EMPTY_METHOD_NAME);
        log.println("\nDebugger: breakpoint is set at:\n\t" + EMPTY_METHOD_NAME);

        // run jdb command "cont"
        reply = jdb.receiveReplyFor(JdbCommand.cont);
        if (!jdb.isAtBreakpoint(reply, EMPTY_METHOD_NAME)) {
            throwFailure("Debugger: Missed breakpoint at:\n\t" + EMPTY_METHOD_NAME);
        }
        log.println("\nDebugger: breakpoint is hit at:\n\t" + EMPTY_METHOD_NAME);

        // run jdb command "eval" for hidden class field HC_NAME_FIELD
        reply = jdb.receiveReplyFor(JdbCommand.eval + HC_NAME_FIELD);
        int beg = reply[0].indexOf('"') + 1;
        int end = reply[0].lastIndexOf('"');
        if (end == -1 || beg > end) {
            log.println("\nDebugger: the jdb command:\n\t" + JdbCommand.eval + HC_NAME_FIELD);
            log.println("\treturned bad reply:\n\t" + reply[0]);
            throwFailure("Debugger: failed to evaluate debuggee field:\n\t" + HC_NAME_FIELD);
        }
        String hiddenClassName = reply[0].substring(beg, end); // we know the hidden class name now
        log.println("\nDebugger: jdb command eval returned hidden class name:\n\t" + hiddenClassName);

        return hiddenClassName;
    }

    /* Test jdb commands "classes" and "class" for hidden class. */
    private void testClassCommands(String hcName) {
        String[] reply = null;

        log.println("\n### Debugger: testClassCommands");

        // run jdb command "classes"
        reply = jdb.receiveReplyFor(JdbCommand.classes);
        if (!checkPattern(reply, hcName)) {
            throwFailure("Debugger: expected jdb command classes to list hidden class:\n\t" + hcName);
        }
        log.println("\nDebugger: found matched class in jdb command classes reply:\n\t" + hcName);

        // run jdb command "class" for hidden class
        reply = jdb.receiveReplyFor(JdbCommand._class + hcName);
        if (!checkPattern(reply, hcName)) {
            throwFailure("Debugger: expected hiddenclass name in jdb command class reply: " + hcName);
        }
        log.println("\nDebugger: found matched class in jdb command class reply:\n\t" + hcName);
    }

    /* Transition the debuggee's execution to the hidden class method start. */
    private void stopInHiddenClassMethod(String hcName) {
        String hcMethodName = hcName + "." + HC_METHOD_NAME;
        String[] reply = null;

        log.println("\n### Debugger: stopInHiddenClassMethod");

        // set a breakpoint in hidden class method hcMethodName()
        jdb.setBreakpointInMethod(hcMethodName);
        log.println("\nDebugger: breakpoint is set at:\n\t" + hcMethodName);

        // run jdb command "clear": should list breakpoint in hcMethodName
        reply = jdb.receiveReplyFor(JdbCommand.clear);
        if (!checkPattern(reply, hcMethodName)) {
            throwFailure("Debugger: expected jdb clear command to list breakpoint: " + hcMethodName);
        }
        log.println("\nDebugger: jdb command clear lists breakpoint at:\n\t" + hcMethodName);

        // run jdb command "cont"
        jdb.receiveReplyFor(JdbCommand.cont);
        log.println("\nDebugger: executed jdb command cont");
    }

    /* Test the jdb commands "up" and "where" for hidden class. */
    private void testUpWhereCommands(String hcName) {
        String hcMethodName = hcName + "." + HC_METHOD_NAME;
        String[] reply = null;

        log.println("\n### Debugger: testUpWhereCommands");

        // run jdb command "where": should list hcMethodName frame
        reply = jdb.receiveReplyFor(JdbCommand.where);
        if (!checkPattern(reply, hcMethodName)) {
            throwFailure("Debugger: jdb command where does not show expected frame: " + hcMethodName);
        }
        log.println("\nDebugger: jdb command where showed expected frame:\n\t" + hcMethodName);

        // run jdb command "up"
        jdb.receiveReplyFor(JdbCommand.up);
        log.println("\nDebugger: executed jdb command up");

        // run jdb command "where": should not list hcMethodName frame
        reply = jdb.receiveReplyFor(JdbCommand.where);
        if (checkPattern(reply, hcMethodName)) {
            throwFailure("Debugger: jdb command where showed unexpected frame: " + hcMethodName);
        }
        log.println("\nDebugger: jdb command where does not show unexpected frame:\n\t" + hcMethodName);
    }

    /* Test the jdb commands "down" and "where" for hidden class. */
    private void testDownWhereCommands(String hcName) {
        String hcMethodName = hcName + "." + HC_METHOD_NAME;
        String[] reply = null;

        log.println("\n### Debugger: testDownWhereCommands");

        // run jdb command "down"
        jdb.receiveReplyFor(JdbCommand.down);
        log.println("\nDebugger: executed jdb command down");

        // run jdb command "where": should list hcMethodName frame again
        reply = jdb.receiveReplyFor(JdbCommand.where);
        if (!checkPattern(reply, hcMethodName)) {
            throwFailure("Debugger: jdb command where does not show expected frame: " + hcMethodName);
        }
        log.println("\nDebugger: jdb command where showed expected frame:\n\t" + hcMethodName);
    }

    /* Test the jdb commands "fields" and "methods" for hidden class. */
    private void testFieldsMethods(String hcName) {
        String[] reply = null;

        log.println("\n### Debugger: testFieldsMethods");

        // run jdb command "methods" for hidden class
        reply = jdb.receiveReplyFor(JdbCommand.methods + hcName);
        if (!checkPattern(reply, hcName)) {
            throwFailure("Debugger: no expected hidden class name in its methods: " + hcName);
        }
        log.println("\nDebugger: jdb command \"methods\" showed expected method:\n\t" + HC_METHOD_NAME);

        // run jdb command "fields" for hidden class
        reply = jdb.receiveReplyFor(JdbCommand.fields + hcName);
        if (!checkPattern(reply, HC_FIELD_NAME)) {
            throwFailure("Debugger: no expected hidden class field in its fields: " + HC_FIELD_NAME);
        }
        log.println("\nDebugger: jdb command \"fields\" showed expected field:\n\t" + HC_FIELD_NAME);
    }

    /* Test the jdb commands "watch" and "unwatch" for hidden class. */
    private void testWatchCommands(String hcName) {
        String hcFieldName = hcName + "." + HC_FIELD_NAME;
        String[] reply = null;

        log.println("\n### Debugger: testWatchCommands");

        // run jdb command "watch" for hidden class field HC_FIELD_NAME
        reply = jdb.receiveReplyFor(JdbCommand.watch + hcFieldName);
        if (!checkPattern(reply, HC_FIELD_NAME)) {
            throwFailure("Debugger: was not able to set watch point: " + hcFieldName);
        }
        log.println("\nDebugger: jdb command \"watch\" added expected field to watch:\n\t" + hcFieldName);

        // run jdb command "cont"
        jdb.receiveReplyFor(JdbCommand.cont);
        jdb.receiveReplyFor(JdbCommand.next);

        // run jdb command "unwatch" for hidden class field HC_FIELD_NAME
        reply = jdb.receiveReplyFor(JdbCommand.unwatch + hcFieldName);
        if (!checkPattern(reply, HC_FIELD_NAME)) {
            throwFailure("Debugger: expect field name in unwatch reply: " + hcFieldName);
        }
        log.println("\nDebugger: jdb command \"unwatch\" removed expected field from watch:\n\t" + hcFieldName);
    }

    /* Test the jdb commands "eval", "print" and "dump" for hidden class. */
    private void testEvalCommands(String hcName) {
        String hcFieldName = hcName + "." + HC_FIELD_NAME;
        String[] reply = null;

        log.println("\n### Debugger: testEvalCommands");

        // run jdb command "eval" for hidden class field HC_FIELD_NAME
        reply = jdb.receiveReplyFor(JdbCommand.eval + hcFieldName);
        if (!checkPattern(reply, hcFieldName)) {
            throwFailure("Debugger: expected field name in jdb command eval field reply: " + hcFieldName);
        }
        log.println("\nDebugger: jdb command \"eval\" showed expected hidden class field name:\n\t" + hcFieldName);

        // run jdb command "print" for hidden class field HC_FIELD_NAME
        reply = jdb.receiveReplyFor(JdbCommand.print + hcFieldName);
        if (!checkPattern(reply, hcFieldName)) {
            throwFailure("Debugger: expected field name in jdb command print field reply: " + hcFieldName);
        }
        log.println("\nDebugger: jdb command \"print\" showed expected hidden class field name:\n\t" + hcFieldName);

        // execute jdb command "dump" for hidden class field HC_FIELD_NAME
        reply = jdb.receiveReplyFor(JdbCommand.dump + hcFieldName);
        if (!checkPattern(reply, hcFieldName)) {
            throwFailure("Debugger: expected field name in jdb command dump field reply: " + hcFieldName);
        }
        log.println("\nDebugger: jdb command \"dump\" showed expected hidden class field name:\n\t" + hcFieldName);
    }

    /* Test the jdb command "watch" with an invalid class name. */
    private void testInvWatchCommand(String hcName) {
        String hcFieldName = hcName + "." + HC_FIELD_NAME;
        String MsgBase = "\nDebugger: jdb command \"watch\" with invalid field " + hcFieldName;
        String[] reply = null;

        // run jdb command "watch" with an invalid class name
        reply = jdb.receiveReplyFor(JdbCommand.watch + hcFieldName);
        if (checkPattern(reply, "Deferring watch modification")) {
            throwFailure(MsgBase + " must not set deferred watch point");
        }
        log.println(MsgBase + " did not set deferred watch point");
    }

    /* Test the jdb command "eval" with an invalid class name. */
    private void testInvEvalCommand(String hcName) {
        String hcFieldName = hcName + "." + HC_FIELD_NAME;
        String MsgBase = "\nDebugger: jdb command \"eval\" with invalid field " + hcFieldName;
        String[] reply = null;

        // run jdb command "eval" with an invalid class name
        reply = jdb.receiveReplyFor(JdbCommand.eval + hcFieldName);
        if (!checkPattern(reply, "ParseException")) {
            throwFailure(MsgBase + " must be rejected with ParseException");
        }
        log.println(MsgBase + " was rejected with ParseException");
    }

    /* Test the jdb commands "watch" and "eval" with various invalid class names. */
    private void testInvalidCommands() {
        String className = null;
        String[] invClassNames = {
            "xx.yyy/0x111/0x222",
            "xx./0x111.0x222",
            "xx.yyy.zzz/"
        };

        log.println("\n### Debugger: testInvalidCommands");

        // run jdb commands "watch" and "eval" with invalid class names
        for (int idx = 0; idx < invClassNames.length; idx++) {
            className = invClassNames[idx];
            testInvWatchCommand(className + "." + HC_FIELD_NAME);
            testInvEvalCommand(className + "." + HC_FIELD_NAME);
        }
    }

    /* Main testing method. */
    protected void runCases() {
        String hcName = runPrologue();

        testClassCommands(hcName);
        stopInHiddenClassMethod(hcName);

        testUpWhereCommands(hcName);
        testDownWhereCommands(hcName);

        testFieldsMethods(hcName);
        testWatchCommands(hcName);

        testEvalCommands(hcName);
        testInvalidCommands();

        jdb.contToExit(1);
    }
}
