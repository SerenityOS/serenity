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

import nsk.share.Consts;
import nsk.share.TestBug;
import nsk.share.jpda.AbstractDebuggeeTest;
import java.io.*;
import java.util.*;
import jdk.test.lib.Utils;

/*
 * This class serial executes several JDI tests based on nsk.share.jdi.TestDebuggerType2 in single VM
 * SerialExecutionDebugger is used together with SerialExecutionDebuggee, execution process is following:
 *
 *  - SerialExecutionDebugger reads tests to execute from input file, test description is debugger class name and test's parameters,
 *  if 'shuffle' option is specified in input file debugger executes tests in random order (input file should contain line "OPTIONS:shuffle").
 *  SerialExecutionDebugger can execute tests several times in loop, number of iterations should be specified in input file in following manner:
 *  OPTIONS:iterations <iterations_number>.
 *
 *  - SerialExecutionDebugger starts debuggee VM with main class SerialExecutionDebuggee,
 *  initializes IOPipe and 'debuggee' object which represents debuggee VM
 *
 *  - for each test from input file:
 *
 *      - SerialExecutionDebugger creates object of current debugger and initializes it with already created pipe and debuggee
 *      - SerialExecutionDebugger sends command to SerialExecutionDebuggee:  'COMMAND_EXECUTE_DEBUGGEE <CurrentDebuggeeName>'
 *      (CurrentDebuggeeName name should provide current debugger), and waits READY signal from debuggee
 *      - SerialExecutionDebuggee parses received command, extracts debugee name, creates object of current debuggee, which should be
 *      subclass of nsk.share.jpda.AbstractDebuggeeTestName
 *      - SerialExecutionDebuggee executes current debuggee's method 'doTest()', in this method debuggee sends signal READY
 *      and waits debugger command
 *      - SerialExecutionDebugger receives signal READY and executes current debugger's method 'doTest()', in
 *      this method debugger should perform test
 *      - when debugger method doTest() finishes SerialExecutionDebugger checks is this test passed or failed and
 *      sends command QUIT to the current debuggee, and when current debuggee finishes sends command 'COMMAND_CLEAR_DEBUGGEE' to the SerialExecutionDebuggee,
 *      after this command SerialExecutionDebugger and SerialExecutionDebuggee ready to execute next test
 *
 *  - when all tests was executed SerialExecutionDebugger sends command QUIT to the SerialExecutionDebuggee and exits
 *
 * SerialExecutionDebugger requires "-configFile <ini-file>" parameter, <ini-file> - file with list of tests for execution
 */
public class SerialExecutionDebugger extends TestDebuggerType2 {
    static public void main(String[] args) {
        System.exit(Consts.JCK_STATUS_BASE + new SerialExecutionDebugger().runIt(args, System.out));
    }

    public String debuggeeClassName() {
        return SerialExecutionDebuggee.class.getName();
    }

    // contains test's debugger class name and test parameters
    static class Test {
        public Test(String debuggerClassName, String[] arguments) {
            this.debuggerClassName = debuggerClassName;
            this.arguments = arguments;
        }

        public String argumentsToString() {
            String result = "";

            for (String argument : arguments)
                result += argument + " ";

            return result;
        }

        String debuggerClassName;

        String arguments[];
    }

    private Test tests[];

    // how many times execute tests
    private int iterations = 1;

    // requires "-configFile <ini-file>" parameter, <ini-file> - file with list
    // of tests for execution
    protected String[] doInit(String args[], PrintStream out) {
        args = super.doInit(args, out);

        String configFileName = null;

        ArrayList<String> standardArgs = new ArrayList<String>();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-configFile") && (i < args.length - 1)) {
                configFileName = args[i + 1];
                i++;
            } else
                standardArgs.add(args[i]);
        }

        if (configFileName == null) {
            throw new TestBug("Config file wasn't specified (use option -configFile <file name>)");
        }

        tests = parseConfigFile(configFileName);

        if (tests.length == 0)
            throw new TestBug("Tests to run were not specified");

        return standardArgs.toArray(new String[] {});
    }

    // read test names and test parameters from ini-file
    private Test[] parseConfigFile(String fileName) {
        List<Test> result = new ArrayList<Test>();

        boolean shuffle = false;

        try {
            File file = new File(fileName);

            LineNumberReader lineReader = new LineNumberReader(new FileReader(file));

            String line = null;

            while ((line = lineReader.readLine()) != null) {
                // skip empty lines and comments started with '#"
                if (line.length() == 0 || line.startsWith("#"))
                    continue;

                if (line.startsWith("OPTIONS:")) {
                    String arguments[] = line.substring(8).split(" ");

                    for (int i = 0; i < arguments.length; i++) {
                        if (arguments[i].equalsIgnoreCase("shuffle"))
                            shuffle = true;
                        else if (arguments[i].equalsIgnoreCase("iterations") && (i < (arguments.length - 1))) {
                            iterations = Integer.parseInt(arguments[i + 1]);
                            i++;
                        }
                    }

                    continue;
                }

                StreamTokenizer tokenizer = new StreamTokenizer(new StringReader(line));
                tokenizer.resetSyntax();
                tokenizer.wordChars(Integer.MIN_VALUE, Integer.MAX_VALUE);
                tokenizer.whitespaceChars(' ', ' ');

                if (tokenizer.nextToken() != StreamTokenizer.TT_WORD)
                    throw new TestBug("Invalid ini file format");

                String testClassName = tokenizer.sval;
                List<String> parameters = new ArrayList<String>();

                int token;
                while ((token = tokenizer.nextToken()) != StreamTokenizer.TT_EOF) {
                    if (token == StreamTokenizer.TT_WORD) {
                        if (tokenizer.sval.equals("$CLASSPATH"))
                            parameters.add(classpath);
                        else
                            parameters.add(tokenizer.sval);
                    }

                    if (token == StreamTokenizer.TT_NUMBER) {
                        parameters.add("" + tokenizer.nval);
                    }
                }

                result.add(new Test(testClassName, parameters.toArray(new String[] {})));
            }

        } catch (IOException e) {
            throw new TestBug("Exception during config file parsing: " + e);
        }

        if (shuffle) {
            if (testWorkDir == null)
                throw new TestBug("Debugger requires -testWorkDir parameter");

            Collections.shuffle(result, Utils.getRandomInstance());

            // save resulted tests sequence in file (to simplify reproducing)
            try {
                File file = new File(testWorkDir + File.separator + "run.tests");
                file.createNewFile();

                PrintWriter writer = new PrintWriter(new FileWriter(file));

                for (Test test : result) {
                    writer.println(test.debuggerClassName + " " + test.argumentsToString());
                }

                writer.close();
            } catch (IOException e) {
                throw new TestBug("Unexpected IOException: " + e);
            }
        }

        System.out.println("Tests execution order: ");
        for (Test test : result) {
            System.out.println(test.debuggerClassName + " " + test.argumentsToString());
        }

        return result.toArray(new Test[] {});
    }

    public void doTest() {

        stresser.start(iterations);

        try {
            if (iterations == 1) {
                /*
                 * Since many test couldn't be run in single VM twice and test config specifies only 1 iteration don't
                 * multiple iterations by iterations factor and execute tests once (not depending on iterations factor)
                 */
                executeTests();
            } else {
                while (stresser.continueExecution()) {
                    if (!executeTests()) {
                        // if error occured stop execution
                        break;
                    }
                }
            }
        } finally {
            stresser.finish();
        }
    }

    boolean executeTests() {
        // maximum execution time of single test
        long maxExecutionTime = 0;

        for (Test test : tests) {
            long testStartTime = System.currentTimeMillis();

            TestDebuggerType2 debugger = null;

            try {
                // create debugger object
                Class debuggerClass = Class.forName(test.debuggerClassName);

                if (!TestDebuggerType2.class.isAssignableFrom(debuggerClass)) {
                    setSuccess(false);
                    log.complain("Invalid debugger class: " + debuggerClass);
                    return false;
                }

                // init test debugger, pass to the debugger already created
                // objects: argHandler, log, pipe, debuggee, vm
                debugger = (TestDebuggerType2) debuggerClass.newInstance();
                debugger.initDebugger(argHandler, log, pipe, debuggee, vm);
                debugger.doInit(test.arguments, System.out);
            } catch (Exception e) {
                setSuccess(false);
                log.complain("Unexpected exception during debugger initialization: " + e);
                e.printStackTrace(log.getOutStream());

                return false;
            }

            log.display("Execute debugger: " + debugger);

            // send command to the SerialExecutionDebuggee (create debuggee
            // object)
            pipe.println(SerialExecutionDebuggee.COMMAND_EXECUTE_DEBUGGEE + ":" + debugger.debuggeeClassName());

            // wait first READY from AbstractDebuggeeTest.doTest() (debuggee
            // sends this command when it was initialized and ready for
            // test)
            if (!isDebuggeeReady())
                return false;

            try {
                // here debuggee should be ready for test and current
                // debugger may perform test
                debugger.doTest();

                if (debugger.getSuccess()) {
                    log.display("Debugger " + debugger + " finished successfully");
                } else {
                    setSuccess(false);
                    log.complain("Debugger " + debugger + " finished with errors");
                }
            } catch (TestBug testBug) {
                setSuccess(false);
                log.complain("Test bug in " + debugger + ": " + testBug);
                testBug.printStackTrace(log.getOutStream());
            } catch (Throwable t) {
                setSuccess(false);
                log.complain("Unexpected exception during test execution(debugger: " + debugger + "): " + t);
                t.printStackTrace(log.getOutStream());
            }

            // send QUIT command to the current debuggee
            pipe.println(AbstractDebuggeeTest.COMMAND_QUIT);

            if (!isDebuggeeReady())
                return false;

            // send command to the SerialExecutionDebuggee
            pipe.println(SerialExecutionDebuggee.COMMAND_CLEAR_DEBUGGEE);

            if (!isDebuggeeReady())
                return false;

            long testExecutionTime = System.currentTimeMillis() - testStartTime;

            if (testExecutionTime > maxExecutionTime)
                maxExecutionTime = testExecutionTime;

            if (maxExecutionTime > stresser.getTimeLeft()) {
                log.display("WARNING: stop test execution because of timeout " +
                                "(max execution time for single test: " + maxExecutionTime + ", time left: " + stresser.getTimeLeft() + ")");
                return false;
            }
        }

        return true;
    }
}
