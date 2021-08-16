/*
 * Copyright (c) 2006, 2020, Oracle and/or its affiliates. All rights reserved.
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
package nsk.share.jdi;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import nsk.share.Consts;
import sun.hotspot.WhiteBox;

/*
 * This class is intended for execution several JDI tests in single VM and used together with nsk.share.jdi.SerialExecutionDebugger
 *
 * SerialExecutionDebuggee handles 2 commands:
 *  - COMMAND_EXECUTE_DEBUGGEE:<debuggee_class_name> :
 *  initialize 'currentDebuggee' with instance of class 'debuggee_class_name'(this class should
 *  be subclass of nsk.share.jpda.AbstractDebugeeTest) and execute it's method doTest()
 *
 *  - COMMAND_CLEAR_DEBUGGEE
 *  set 'currentDebuggee' to null
 *
 *  For more detailed description of serial test execution see SerialExecutionDebugger
 */
public class SerialExecutionDebuggee extends AbstractJDIDebuggee {
    private final WhiteBox WB = WhiteBox.getWhiteBox();

    public static void main(String args[]) {
        new SerialExecutionDebuggee().doTest(args);
    }

    // command:<debuggee_class_name>[ debugee_parameters]
    public static final String COMMAND_EXECUTE_DEBUGGEE = "COMMAND_EXECUTE_DEBUGGEE";

    public static final String COMMAND_CLEAR_DEBUGGEE = "COMMAND_CLEAR_DEBUGGEE";

    private AbstractJDIDebuggee currentDebuggee;

    public boolean parseCommand(String command) {
        if (super.parseCommand(command))
            return true;

        if (command.startsWith(COMMAND_EXECUTE_DEBUGGEE)) {
            String debuggeeClassName = null;

            try {
                StreamTokenizer tokenizer = new StreamTokenizer(new StringReader(command));
                tokenizer.resetSyntax();
                tokenizer.wordChars(Integer.MIN_VALUE, Integer.MAX_VALUE);
                tokenizer.whitespaceChars(':', ':');

                tokenizer.nextToken();

                if (tokenizer.nextToken() != StreamTokenizer.TT_WORD) {
                    log.complain("Invalid command format: " + command);
                    System.exit(Consts.JCK_STATUS_BASE + Consts.TEST_FAILED);
                }

                debuggeeClassName = tokenizer.sval;
            } catch (IOException e) {
                log.complain("Invalid command format: " + command);
                System.exit(Consts.JCK_STATUS_BASE + Consts.TEST_FAILED);
            }

            try {
                // parse debuggee parameters
                String[] debuggeeParameters = {};

                int index = debuggeeClassName.indexOf(' ');

                if (index > 0) {
                    debuggeeParameters = debuggeeClassName.substring(index).split(" ");
                    log.display("Debuggee parameters: " + debuggeeClassName.substring(index));
                    debuggeeClassName = debuggeeClassName.substring(0, index);
                }

                // create debuggee object
                Class debuggeeClass = Class.forName(debuggeeClassName);

                if (!AbstractJDIDebuggee.class.isAssignableFrom(debuggeeClass)) {
                    log.complain("Invalid debugee class: " + debuggeeClass);
                    System.exit(Consts.JCK_STATUS_BASE + Consts.TEST_FAILED);
                }

                currentDebuggee = (AbstractJDIDebuggee) debuggeeClass.newInstance();

                // pass to the current debuggee already created objects:
                // argHandler, log, pipe
                currentDebuggee.initDebuggee(argHandler, log, pipe, debuggeeParameters, false);
            } catch (Exception e) {
                log.complain("Unexpected exception during debuggee initialization: " + e);
                e.printStackTrace(log.getOutStream());
                System.exit(Consts.JCK_STATUS_BASE + Consts.TEST_FAILED);
            }

            try {
                // current debuggee performs test
                currentDebuggee.doTest();

                if (currentDebuggee.getSuccess()) {
                    log.display("Debuggee " + currentDebuggee + " finished successfully");
                } else {
                    setSuccess(false);
                    log.complain("Debuggee " + currentDebuggee + "finished with errors");
                }

                return true;
            } catch (Exception e) {
                log.complain("Unexpected exception during debuggee execution: " + e);
                e.printStackTrace(log.getOutStream());
                System.exit(Consts.JCK_STATUS_BASE + Consts.TEST_FAILED);
            }
        } else if (command.equals(COMMAND_CLEAR_DEBUGGEE)) {
            currentDebuggee = null;

            // The debuggee can intentionally create inflated monitors.
            // With async deflation, this can pin a StateTestThread object
            // until the next deflation cycle. This can confuse tests run
            // by nsk/jdi/stress/serial/mixed002/TestDescription.java that
            // expect only one StateTestThread object to exist in each
            // of the debugger tests that mixed002 runs serially in the
            // same VM.
            WB.deflateIdleMonitors();

            return true;
        }

        return false;
    }

}
