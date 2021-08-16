/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jit.graph;

import jdk.test.lib.Utils;
import jtreg.SkippedException;
import nsk.share.TestFailure;
import nsk.share.test.StressOptions;

import java.lang.reflect.InvocationTargetException;
import java.util.Vector;

public class CGT {
    private static StressOptions stressOptions = new StressOptions();
    private static String ClistPath = "";
    private static long finishTime;

    private final Vector summation = new Vector(100000);
    private final Vector idList = new Vector(100000);

    public CGT(String[] args) {
        parse(args);
        Globals.initialize(ClistPath);
        outputStats(args);
    }

    public static void main(String[] args) {
        stressOptions.parseCommandLine(args);
        new CGT(args).run();
    }

    public void outputStats(String[] args) {
        System.out.println("CGT command line options:");
        for (String arg : args) {
            System.out.println("# " + arg);
        }

        System.out.println();

        System.out.println("CGT parameters");
        System.out.println("Seed: " + Utils.SEED);
        System.out.println("Number of Random Loop iterations: " + Globals.RANDOM_LOOP);
        System.out.println("Number of Static Loop iterations: " + Globals.STATIC_LOOP);
        System.out.println("Max number of Methods in the Graph: " + Globals.NUM_TEST_CLASSES);
        System.out.println("Verbose function calls: " + Globals.VERBOSE);

        System.out.println();
    }

    public void run() {
        finishTime = System.currentTimeMillis() + stressOptions.getTime() * 1000;
        Long numFcalls = Globals.RANDOM_LOOP - 1;
        Integer staticFcalls = Globals.STATIC_LOOP;
        MethodData methodCallStr = Globals.nextRandomMethod();
        Globals.addFunctionIDToVector(methodCallStr.id, idList);
        Throwable invocationExcept;

        try {
            methodCallStr.nextMethod.invoke(methodCallStr.instance, summation, idList, numFcalls, staticFcalls);
        } catch (IllegalAccessException e) {
            throw new TestFailure("Illegal Access Exception", e);
        } catch (InvocationTargetException e) {
            System.out.println("Invocation Target Exception");
            invocationExcept = e.getTargetException();
            System.out.println(invocationExcept);
            if (invocationExcept.getClass() == e.getClass()) {
                System.out.println("Processing Exception Invocation Target Exception");
                while (invocationExcept.getClass() == e.getClass()) {
                    invocationExcept = ((InvocationTargetException) invocationExcept).getTargetException();
                }
                System.out.println(invocationExcept);
            }
            if (invocationExcept instanceof StackOverflowError) {
                throw new SkippedException("stack overflow: skipping verification.", invocationExcept);
            } else if (invocationExcept instanceof OutOfMemoryError) {
                throw new SkippedException("test devoured heap ;), skipping verification.", invocationExcept);
            } else {
                throw new TestFailure(invocationExcept);
            }
        }

        verify();
    }

    private void verify() {
        long oldsum = 0;
        long newsum;
        System.out.println("begin call stack validation");
        if (summation.size() != idList.size()) {
            throw new TestFailure("Vector Length's Do Not Match, VERIFY ERROR : Summation Element Count = " + summation.size() + " ID Element Count = " + idList.size());
        }
        long vectorSize = summation.size();

        while (!summation.isEmpty()) {
            if (CGT.shouldFinish()) {
                throw new SkippedException("skipping verification due to timeout");
            }

            newsum = (Long) summation.firstElement();
            summation.removeElementAt(0);

            int functionID = (Integer) idList.firstElement();
            idList.removeElementAt(0);

            if ((newsum - oldsum) != (functionID)) {
                throw new TestFailure("Function Call structure invalid, VERIFY ERROR. Expected = " + (newsum - oldsum) + " Actual = " + functionID);
            }
            oldsum = newsum;
        }

        System.out.println("function call structure validated successfully (" + vectorSize + " calls validated)");
    }

    public static boolean shouldFinish() {
        return System.currentTimeMillis() >= finishTime;
    }

    public void parse(String args[]) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i].toLowerCase();
            switch (arg) {
                case "-help":
                case "-h":
                case "-?": {
                    usage();
                    System.exit(1);
                    break;
                }
                case "-staticloop": {
                    int argIndex = i + 1;
                    if (argIndex < args.length) {
                        try {
                            Globals.STATIC_LOOP = Math.abs(Integer.parseInt(args[argIndex])) * stressOptions.getIterationsFactor();
                        } catch (NumberFormatException e) {
                            usage();
                            throw new Error("TESTBUG: Improper Argument: " + args[i] + " " + args[argIndex], e);
                        }
                        i++;
                    } else {
                        usage();
                        throw new Error("TESTBUG: Improper Argument: " + args[i]);
                    }
                    break;
                }
                case "-randomloop": {
                    int argIndex = i + 1;
                    if (argIndex < args.length) {
                        try {
                            Globals.RANDOM_LOOP = Math.abs(Long.parseLong(args[argIndex])) * stressOptions.getIterationsFactor();
                        } catch (NumberFormatException e) {
                            usage();
                            throw new Error("TESTBUG: Improper Argument: " + args[i] + " " + args[argIndex], e);
                        }
                        i++;
                    } else {
                        usage();
                        throw new Error("TESTBUG: Improper Argument: " + args[i]);

                    }
                    break;
                }
                case "-numtestclass": {
                    int argIndex = i + 1;
                    if (argIndex < args.length) {
                        try {
                            Globals.NUM_TEST_CLASSES = Math.abs(Integer.parseInt(args[argIndex]));
                        } catch (NumberFormatException e) {
                            usage();
                            throw new Error("TESTBUG: Improper Argument: " + args[i] + " " + args[argIndex], e);
                        }
                        i++;
                    } else {
                        usage();
                        throw new Error("TESTBUG: Improper Argument: " + args[i]);
                    }
                    break;
                }
                case "-verbose":
                case "-v": {
                    Globals.VERBOSE = true;
                    break;
                }
                case "-path": {
                    int argIndex = i + 1;
                    if (argIndex < args.length) {
                        ClistPath = args[argIndex];
                        i++;
                    } else {
                        usage();
                        throw new Error("TESTBUG: Improper Argument: " + args[i]);
                    }
                    break;
                }
                default: {
                    if (!arg.startsWith("-stress")) {
                        usage();
                        throw new Error("TESTBUG: Invalid Argument: " + args[i]);
                    }
                }
            }
        }

        if ("".equals(ClistPath)) {
            usage();
            throw new Error("TESTBUG: class list path not defined");
        }
    }

    public void usage() {
        System.out.println("usage: java CGT [options]");
        System.out.println("  -help                               prints out this message");
        System.out.println("  -numTestClass #                     limits the number of \"Test Methods\" to #");
        System.out.println("  -randomcLoop #                      # of random function calls");
        System.out.println("  -staticLoop #                       # of non-random static function calls");
        System.out.println("  -v -verbose                         turn on verbose mode");
        System.out.println("  -path <path to classlist>           required, argument so program can find classes");
    }
}
