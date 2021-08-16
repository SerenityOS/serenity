/*
 * Copyright (c) 1998, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * IntlTest is a base class for tests that can be run conveniently from
 * the command line as well as under the Java test harness.
 * <p>
 * Sub-classes implement a set of public void methods named "Test*" or
 * "test*" with no arguments. Each of these methods performs some
 * test. Test methods should indicate errors by calling either err() or
 * errln().  This will increment the errorCount field and may optionally
 * print a message to the log.  Debugging information may also be added to
 * the log via the log and logln methods.  These methods will add their
 * arguments to the log only if the test is being run in verbose mode.
 */
public abstract class IntlTest {

    //------------------------------------------------------------------------
    // Everything below here is boilerplate code that makes it possible
    // to add a new test by simply adding a method to an existing class.
    //------------------------------------------------------------------------

    protected IntlTest() {
        // Populate testMethods with all the test methods.
        Method[] methods = getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (Modifier.isPublic(method.getModifiers())
                && method.getReturnType() == void.class
                && method.getParameterCount() == 0) {
                String name = method.getName();
                if (name.length() > 4) {
                    if (name.startsWith("Test") || name.startsWith("test")) {
                        testMethods.put(name, method);
                    }
                }
            }
        }
    }

    protected void run(String[] args) throws Exception
    {
        // Set up the log and reference streams.  We use PrintWriters in order to
        // take advantage of character conversion.  The JavaEsc converter will
        // convert Unicode outside the ASCII range to Java's \\uxxxx notation.
        log = new PrintWriter(System.out, true);

        // Parse the test arguments.  They can be either the flag
        // "-verbose" or names of test methods. Create a list of
        // tests to be run.
        List<Method> testsToRun = new ArrayList<>(args.length);
        for (String arg : args) {
            switch (arg) {
            case "-verbose":
                verbose = true;
                break;
            case "-prompt":
                prompt = true;
                break;
            case "-nothrow":
                nothrow = true;
                break;
            case "-exitcode":
                exitCode = true;
                break;
            default:
                Method m = testMethods.get(arg);
                if (m == null) {
                    System.out.println("Method " + arg + ": not found");
                    usage();
                    return;
                }
                testsToRun.add(m);
                break;
            }
        }

        // If no test method names were given explicitly, run them all.
        if (testsToRun.isEmpty()) {
            testsToRun.addAll(testMethods.values());
        }

        System.out.println(getClass().getName() + " {");
        indentLevel++;

        // Run the list of tests given in the test arguments
        for (Method testMethod : testsToRun) {
            int oldCount = errorCount;

            writeTestName(testMethod.getName());

            try {
                testMethod.invoke(this, new Object[0]);
            } catch (IllegalAccessException e) {
                errln("Can't acces test method " + testMethod.getName());
            } catch (InvocationTargetException e) {
                errln("Uncaught exception thrown in test method "
                        + testMethod.getName());
                e.getTargetException().printStackTrace(this.log);
            }
            writeTestResult(errorCount - oldCount);
        }
        indentLevel--;
        writeTestResult(errorCount);

        if (prompt) {
            System.out.println("Hit RETURN to exit...");
            try {
                System.in.read();
            } catch (IOException e) {
                System.out.println("Exception: " + e.toString() + e.getMessage());
            }
        }
        if (nothrow) {
            if (exitCode) {
                System.exit(errorCount);
            }
            if (errorCount > 0) {
                throw new IllegalArgumentException("encountered " + errorCount + " errors");
            }
        }
    }

    /**
     * Adds the given message to the log if we are in verbose mode.
     */
    protected void log(String message) {
        logImpl(message, false);
    }

    protected void logln(String message) {
        logImpl(message, true);
    }

    protected void logln() {
        logImpl(null, true);
    }

    private void logImpl(String message, boolean newline) {
        if (verbose) {
            if (message != null) {
                indent(indentLevel + 1);
                log.print(message);
            }
            if (newline) {
                log.println();
            }
        }
    }

    protected void err(String message) {
        errImpl(message, false);
    }

    protected void errln(String message) {
        errImpl(message, true);
    }

    private void errImpl(String message, boolean newline) {
        errorCount++;
        indent(indentLevel + 1);
        log.print(message);
        if (newline) {
            log.println();
        }
        log.flush();

        if (!nothrow) {
            throw new RuntimeException(message);
        }
    }

    protected int getErrorCount() {
        return errorCount;
    }

    protected void writeTestName(String testName) {
        indent(indentLevel);
        log.print(testName);
        log.flush();
        needLineFeed = true;
    }

    protected void writeTestResult(int count) {
        if (!needLineFeed) {
            indent(indentLevel);
            log.print("}");
        }
        needLineFeed = false;

        if (count != 0) {
            log.println(" FAILED");
        } else {
            log.println(" Passed");
        }
    }

    /*
     * Returns a spece-delimited hex String.
     */
    protected static String toHexString(String s) {
        StringBuilder sb = new StringBuilder(" ");

        for (int i = 0; i < s.length(); i++) {
            sb.append(Integer.toHexString(s.charAt(i)));
            sb.append(' ');
        }

        return sb.toString();
    }

    private void indent(int distance) {
        if (needLineFeed) {
            log.println(" {");
            needLineFeed = false;
        }
        log.print(SPACES.substring(0, distance * 2));
    }

    /**
     * Print a usage message for this test class.
     */
    void usage() {
        System.out.println(getClass().getName() +
                            ": [-verbose] [-nothrow] [-exitcode] [-prompt] [test names]");

        System.out.println("  Available test names:");
        for (String methodName : testMethods.keySet()) {
            System.out.println("\t" + methodName);
        }
    }

    private boolean     prompt;
    private boolean     nothrow;
    protected boolean   verbose;
    private boolean     exitCode;
    private PrintWriter log;
    private int         indentLevel;
    private boolean     needLineFeed;
    private int         errorCount;

    private final Map<String, Method> testMethods = new LinkedHashMap<>();

    private static final String SPACES = "                                          ";
}
