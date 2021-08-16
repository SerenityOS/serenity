/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package org.netbeans.jemmy;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;

/**
 *
 * Jemmy itself provides a way to create tests. Test should implement
 * org.netbeans.jemmy.Scenario interface.
 *
 * Test can be executed from command line:<BR>
 * {@code java [application options] [jemmy options] org.netbeans.jemmy.Test [full name of test class] [test args]}<BR>
 * Test elso can be executed by one of the run(...) methods or by <BR>
 * {@code new Test([test class name]).startTest([test args]);}<BR>
 *
 * <BR><BR>Timeouts used: <BR>
 * Test.WholeTestTimeout - time for the whole test<BR>
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 */
public class Test extends ActionProducer<Object, Object>
        implements Timeoutable, Outputable, Scenario {

    private final static long WHOLE_TEST_TIMEOUT = 3600000;

    /**
     * Status returned by test if wrong parameter was passed.
     */
    public static int WRONG_PARAMETERS_STATUS = 101;

    /**
     * Status returned by test if exception appeared inside scenario.
     */
    public static int SCENARIO_EXCEPTION_STATUS = 102;

    /**
     * Positive test status.
     */
    public static int TEST_PASSED_STATUS = 0;

    /**
     * Test timeouts.
     */
    protected Timeouts timeouts;

    /**
     * Test output.
     */
    protected TestOut output;

    private Scenario scenario;

    /**
     * Constructor for tests requiring only a class instance. Creates a subclass
     * of {@code ActionProducer} and {@code java.lang.Thread} that
     * runs in a separate thread of execution and waits for execution to finish.
     * The current output stream assignments and timeouts are used.
     *
     * @param testClassName Full test class name
     */
    public Test(String testClassName) {
        super(true);
        setOutput(JemmyProperties.getCurrentOutput());
        setTimeouts(JemmyProperties.getCurrentTimeouts());
        scenario = testForName(testClassName);
    }

    /**
     * Constructor for scenarios that require an instance and might require an
     * argument. Creates a subclass of {@code ActionProducer} and
     * {@code java.lang.Thread} that runs in a separate thread of execution
     * and waits for execution to finish. The current output stream assignments
     * and timeouts are used.
     *
     * @param scenario a test scenario
     * @see org.netbeans.jemmy.Scenario
     */
    public Test(Scenario scenario) {
        super(true);
        setOutput(JemmyProperties.getCurrentOutput());
        setTimeouts(JemmyProperties.getCurrentTimeouts());
        this.scenario = scenario;
    }

    /**
     * No argument constructor. Used by subclasses of this {@code Test}
     * class. Creates a subclass of {@code ActionProducer} and
     * {@code java.lang.Thread} that runs in a separate thread of execution
     * and waits for execution to finish. The current output stream assignments
     * and timeouts are used.
     */
    protected Test() {
        super(true);
        setOutput(JemmyProperties.getCurrentOutput());
        setTimeouts(JemmyProperties.getCurrentTimeouts());
    }

    /**
     * Throws TestCompletedException exception. The exception thrown contains a
     * pass/fail status and a short status {@code java.lang.String}. Can by
     * invoked from test to abort test execution.
     *
     * @param status If 0 - test passed, otherwise failed.
     * @throws TestCompletedException all of the time.
     */
    public static void closeDown(int status) {
        if (status == 0) {
            throw (new TestCompletedException(status, "Test passed"));
        } else {
            throw (new TestCompletedException(status, "Test failed with status "
                    + Integer.toString(status)));
        }
    }

    /**
     * Executes a test.
     *
     * @param argv First element should be a test class name, all others - test
     * args.
     * @return test status.
     */
    public static int run(String[] argv) {
        String[] args = argv;
        JemmyProperties.getProperties().init();
        if (argv.length < 1) {
            JemmyProperties.getCurrentOutput().
                    printErrLine("First element of String array should be test classname");
            return WRONG_PARAMETERS_STATUS;
        }
        JemmyProperties.getCurrentOutput().printLine("Executed test " + argv[0]);
        Test test = new Test(argv[0]);
        if (argv.length >= 1) {
            args = shiftArray(args);
        }
        if (argv.length >= 2) {
            JemmyProperties.getCurrentOutput().printLine("Work directory: " + argv[1]);
            System.setProperty("user.dir", argv[1]);
            args = shiftArray(args);
        }
        int status;
        status = test.startTest(args);
        JemmyProperties.getCurrentOutput().flush();
        return status;
    }

    /**
     * Executes a test.
     *
     * @param argv First element should be a test class name, all others - test
     * args.
     * @param output Stream to put test output and errput into.
     * @return test status.
     */
    public static int run(String[] argv, PrintStream output) {
        JemmyProperties.setCurrentOutput(new TestOut(System.in, output, output));
        return run(argv);
    }

    /**
     * Executes a test.
     *
     * @param argv First element should be a test class name, all others - test
     * args.
     * @param output Stream to put test output into.
     * @param errput Stream to put test errput into.
     * @return test status.
     */
    public static int run(String[] argv, PrintStream output, PrintStream errput) {
        JemmyProperties.setCurrentOutput(new TestOut(System.in, output, errput));
        return run(argv);
    }

    /**
     * Executes a test.
     *
     * @param argv First element should be a test class name, all others - test
     * args.
     * @param output Writer to put test output and errput into.
     * @return test status.
     */
    public static int run(String[] argv, PrintWriter output) {
        JemmyProperties.setCurrentOutput(new TestOut(System.in, output, output));
        return run(argv);
    }

    /**
     * Executes a test.
     *
     * @param argv First element should be a test class name, all others - test
     * args.
     * @param output Writer to put test output into.
     * @param errput Writer to put test errput into.
     * @return test status.
     */
    public static int run(String[] argv, PrintWriter output, PrintWriter errput) {
        JemmyProperties.setCurrentOutput(new TestOut(System.in, output, errput));
        return run(argv);
    }

    /**
     * Invoke this {@code Test}. The call might be directly from the
     * command line.
     *
     * @param argv First element should be a test class name, all others - test
     * args.
     */
    public static void main(String[] argv) {
        System.exit(run(argv, System.out));
    }

    static {
        Timeouts.initDefault("Test.WholeTestTimeout", WHOLE_TEST_TIMEOUT);
    }

    /**
     * Creates an instance of a class named by the parameter.
     *
     * @param testName Full test class name
     * @return an instance of the test {@code Scenario} to launch.
     * @see org.netbeans.jemmy.Scenario
     */
    public Scenario testForName(String testName) {
        try {
            return ((Scenario) (Class.forName(testName).
                    getConstructor(new Class<?>[0]).
                    newInstance()));
        } catch (ClassNotFoundException e) {
            output.printErrLine("Class " + testName + " does not exist!");
            output.printStackTrace(e);
        } catch (NoSuchMethodException e) {
            output.printErrLine("Class " + testName + " has not constructor!");
            output.printStackTrace(e);
        } catch (InvocationTargetException e) {
            output.printErrLine("Exception inside " + testName + " constructor:");
            output.printStackTrace(e.getTargetException());
        } catch (IllegalAccessException e) {
            output.printErrLine("Cannot access to " + testName + " constructor!");
            output.printStackTrace(e);
        } catch (InstantiationException e) {
            output.printErrLine("Cannot instantiate " + testName + " class!");
            output.printStackTrace(e);
        }
        return null;
    }

    /**
     * Set the timeouts used by this {@code Test}.
     *
     * @param timeouts A collection of timeout assignments.
     * @see org.netbeans.jemmy.Timeoutable
     * @see org.netbeans.jemmy.Timeouts
     * @see #getTimeouts
     */
    @Override
    public void setTimeouts(Timeouts timeouts) {
        this.timeouts = timeouts;
        Timeouts times = timeouts.cloneThis();
        times.setTimeout("ActionProducer.MaxActionTime",
                timeouts.getTimeout("Test.WholeTestTimeout"));
        super.setTimeouts(times);
    }

    /**
     * Get the timeouts used by this {@code Test}.
     *
     * @see org.netbeans.jemmy.Timeoutable
     * @see org.netbeans.jemmy.Timeouts
     * @see #setTimeouts
     */
    @Override
    public Timeouts getTimeouts() {
        return timeouts;
    }

    /**
     * Set the streams or writers used for print output.
     *
     * @param out An object used to identify both output and error print
     * streams.
     * @see org.netbeans.jemmy.Outputable
     * @see org.netbeans.jemmy.TestOut
     * @see #getOutput
     */
    @Override
    public void setOutput(TestOut out) {
        output = out;
        super.setOutput(out);
    }

    /**
     * Get the streams or writers used for print output.
     *
     * @return an object containing references to both output and error print
     * streams.
     * @see org.netbeans.jemmy.Outputable
     * @see org.netbeans.jemmy.TestOut
     * @see #setOutput
     */
    @Override
    public TestOut getOutput() {
        return output;
    }

    /**
     * Executes test.
     *
     * @param param Object to be passed into this test's launch(Object) method.
     * @return test status.
     */
    public int startTest(Object param) {
        if (scenario != null) {
            output.printLine("Test " + scenario.getClass().getName()
                    + " has been started");
        } else {
            output.printLine("Test " + getClass().getName()
                    + " has been started");
        }
        try {
            return ((Integer) produceAction(param, "Test.WholeTestTimeout")).intValue();
        } catch (InterruptedException e) {
            output.printErrLine("Test was interrupted.");
            output.printStackTrace(e);
        } catch (TimeoutExpiredException e) {
            output.printErrLine("Test was not finished in "
                    + Long.toString(timeouts.getTimeout("Test.WholeTestTimeout"))
                    + " milliseconds");
            output.printStackTrace(e);
        } catch (Exception e) {
            output.printStackTrace(e);
        }
        return 1;
    }

    /**
     * Launch an action. Pass arguments to and execute a test
     * {@code Scenario}.
     *
     * @param obj An argument object that controls test execution. This might be
     * a {@code java.lang.String[]} containing command line arguments.
     * @see org.netbeans.jemmy.Action
     * @return an Integer containing test status.
     */
    @Override
    public final Object launch(Object obj) {
        setTimeouts(timeouts);
        try {
            if (scenario != null) {
                closeDown(scenario.runIt(obj));
            } else {
                closeDown(runIt(obj));
            }
        } catch (TestCompletedException e) {
            output.printStackTrace(e);
            return e.getStatus();
        } catch (Throwable e) {
            output.printStackTrace(e);
            return SCENARIO_EXCEPTION_STATUS;
        }
        return TEST_PASSED_STATUS;
    }

    /**
     * Supposed to be overridden to print a synopsys into test output.
     */
    public void printSynopsis() {
        output.printLine("Here should be a test synopsis.");
    }

    /**
     * @see org.netbeans.jemmy.Action
     */
    @Override
    public final String getDescription() {
        return "Test " + scenario.getClass().getName() + " finished";
    }

    @Override
    public String toString() {
        return "Test{" + "scenario=" + scenario + '}';
    }

    /**
     * Defines a way to execute this {@code Test}.
     *
     * @param param An object passed to configure the test scenario execution.
     * For example, this parameter might be a      <code>java.lang.String[]<code> object that lists the
     * command line arguments to the Java application corresponding
     * to a test.
     * @return an int that tells something about the execution. For, example, a
     * status code.
     * @see org.netbeans.jemmy.Scenario
     */
    @Override
    public int runIt(Object param) {
        return 0;
    }

    /**
     * Sleeps.
     *
     * @param time The sleep time in milliseconds.
     */
    protected void doSleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
        }
    }

    private static String[] shiftArray(String[] orig) {
        String[] result = new String[orig.length - 1];
        for (int i = 0; i < result.length; i++) {
            result[i] = orig[i + 1];
        }
        return result;
    }

}
