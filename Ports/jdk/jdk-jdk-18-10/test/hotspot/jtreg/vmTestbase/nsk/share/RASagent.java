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

package nsk.share;

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Class used as an agent for Java serviceability reliability testing (RAS).
 * It sets different RAS options and/or modes for a special agent which
 * actually performs the specified RAS testing.<br>
 * The agent recognizes arguments, started with ''<code>-ras.</code>''. They
 * may be as follows:<p>
 * <li><code>-ras.help</code> - print usage message and exit
 * <li><code>-ras.verbose</code> - verbose mode
 * <li><code>-ras.invoke_run</code> - invoke the method <i>run(String[],PrintStream)</i>
 * of the test instead of <i>main(String[])</i> which is invoked by default.
 * <li><code>-ras.hotswap=&lt;stress_level&gt;</code> - enable JVMTI hotswap of
 * the currently running test classes. Here are the possible HotSwap stress
 * levels:<br>
 * 0 - HotSwap off<br>
 * 2 - HotSwap tested class in every JVMTI method entry event of running test
 * (default mode)<br>
 * 20 - HotSwap tested class in every JVMTI method entry event of every class<br>
 * 3 - HotSwap tested class in every JVMTI single step event of running test<br>
 * 4 - HotSwap tested class in every JVMTI exception event of running test<br>
 * 40 - HotSwap tested class in every JVMTI exception event of every class<p>
 */
public class RASagent {
    static final int HOTSWAP_OFF = 0;
    static final int HOTSWAP_EVERY_METHOD_ENTRY = 2;
    static final int HOTSWAP_EVERY_METHOD_ENTRY_FOR_EVERY_CLASS = 20;
    static final int HOTSWAP_EVERY_SINGLE_STEP = 3;
    static final int HOTSWAP_EVERY_EXCEPTION = 4;
    static final int HOTSWAP_EVERY_EXCEPTION_FOR_EVERY_CLASS = 40;

    // path to the directory with class files of the invoked test
    static String clfBasePath = null;

    private static boolean verbose = false;

    private static PrintStream out;

    native static int setHotSwapMode(boolean vrb, int stress_lev,
        String shortName);

    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new RASagent().runThis(argv, out);
    }

    private int runThis(String argv[], PrintStream out) {
        int skipArgs = 1; // number of arguments which must be skipped
                          // for the invoked test
        boolean invokeRun = false; // invoke the method "main" by default
        int hotSwapMode = HOTSWAP_EVERY_METHOD_ENTRY; // HotSwap default stress level
        int res;
        String hotSwapModeName = "HOTSWAP_EVERY_METHOD_ENTRY";

        RASagent.out = out;

        if (argv.length != 0) {
            // parse arguments for the RASagent and then skip them
            while(argv[skipArgs-1].startsWith("-ras.")) {
                if (argv[skipArgs-1].equals("-ras.verbose")) {
                    verbose = true;
                } else if (argv[skipArgs-1].equals("-ras.help")) {
                    printHelp();
                    return Consts.TEST_FAILED;
                } else if (argv[skipArgs-1].equals("-ras.invoke_run")) {
                    invokeRun = true;
                } else if (argv[skipArgs-1].startsWith("-ras.hotswap=")) {
                    try {
                        hotSwapMode = Integer.parseInt(
                           argv[skipArgs-1].substring(argv[skipArgs-1].lastIndexOf("=")+1));
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                        out.println("\nERROR: RASagent: specified HotSwap mode \""
                            + hotSwapMode + "\" is not an integer");
                        printHelp();
                        return Consts.TEST_FAILED;
                    }
                    switch(hotSwapMode) {
                        case HOTSWAP_EVERY_METHOD_ENTRY:
                            hotSwapModeName = "HOTSWAP_EVERY_METHOD_ENTRY";
                            break;
                        case HOTSWAP_EVERY_METHOD_ENTRY_FOR_EVERY_CLASS:
                            hotSwapModeName = "HOTSWAP_EVERY_METHOD_ENTRY_FOR_EVERY_CLASS";
                            break;
                        case HOTSWAP_EVERY_SINGLE_STEP:
                            hotSwapModeName = "HOTSWAP_EVERY_SINGLE_STEP";
                            break;
                        case HOTSWAP_EVERY_EXCEPTION:
                            hotSwapModeName = "HOTSWAP_EVERY_EXCEPTION";
                            break;
                        case HOTSWAP_EVERY_EXCEPTION_FOR_EVERY_CLASS:
                            hotSwapModeName = "HOTSWAP_EVERY_EXCEPTION_FOR_EVERY_CLASS";
                            break;
                        default:
                            out.println("\nERROR: RASagent: specified HotSwap mode \""
                                + hotSwapMode + "\" is unrecognized");
                            printHelp();
                            return Consts.TEST_FAILED;
                    }
                }
                skipArgs++;
            }

            String shortTestName = getTestNameAndPath(argv[skipArgs-1]);

            display("\n#### RASagent: setting hotswap mode \""
                + hotSwapModeName + "\" for class \""
                + shortTestName + "\" ...");
            if ((res = setHotSwapMode(verbose, hotSwapMode, shortTestName)) != 0) {
                out.println("\nERROR: RASagent: unable to set HotSwap stress level for \""
                    + shortTestName + "\", exiting");
                return Consts.TEST_FAILED;
            }
            display("\n#### RASagent: ... setting hotswap mode done");

            try {
                Class testCls = Class.forName(argv[skipArgs-1]);
                display("\n#### RASagent: main class \""
                    + testCls.toString() + "\" loaded");

                // copy arguments for the invoked test
                String args[] = new String[argv.length-skipArgs];
                System.arraycopy(argv, skipArgs, args, 0, args.length);

                // invoke the test
                if (invokeRun)
                    return invokeRunMethod(testCls, args);
                else
                    return invokeMainMethod(testCls, args);
            } catch(ClassNotFoundException e) {
                // just pass: the invoked test is already a RAS specific one
                out.println("\nWARNING: the test was not really run due to the following error:"
                    + "\n\tunable to get the Class object for \""
                    + argv[skipArgs-1] + "\"\n\tcaught: " + e);
                return Consts.TEST_PASSED;
            }

        } else {
            out.println("\nERROR: RASagent: required test name is absent in parameters list");
            return Consts.TEST_FAILED;
        }
    }

    /**
     * Verify that test's class file exists with a path given as a parameter
     * and, if so, store that path in the static field "clfBasePath".
     */
    private boolean pathValid(String pathToCheck, String testName) {
        String fullPath = pathToCheck + File.separator
            + testName.replace('.', File.separatorChar) + ".class";
        File classFile = null;

        display("\n#### RASagent: verifying class path\n<RASagent>\t"
            + pathToCheck + " ...");
        try {
            classFile = new File(fullPath);
        } catch (NullPointerException e) {
            e.printStackTrace();
            out.println("\nERROR: RASagent: verification of class file "
                + fullPath + " failed: caught " + e);
            System.exit(Consts.JCK_STATUS_BASE + Consts.TEST_FAILED);
        }

        if (classFile.exists()) {
            clfBasePath = pathToCheck;
            display("<RASagent>\tthe class file exists:\n<RASagent>\t\t"
                + fullPath + "\n<RASagent>\tclass file base directory found:\n"
                + "<RASagent>\t\t" + clfBasePath
                + "\n#### RASagent: ... class path verification done\n");
            return true;
        }
        else {
            display("<RASagent>\tno class file at location :\n\t\t"
                + fullPath
                + "\n#### RASagent: ... class path verification done\n");
            return false;
        }
    }

    /**
     * Get short name of an invoked test (i.e. without package name) and
     * store path to the directory with the test's class files.
     */
    private String getTestNameAndPath(String testName) {
        String shortTestName = testName;
        String packageName = "";

        // if '.' occurs, it means that current test is inside a package
        if (testName.lastIndexOf(".") != -1) {
            shortTestName = testName.substring(testName.lastIndexOf(".")+1);
            packageName = testName.substring(0, testName.lastIndexOf("."));
        }

        StringTokenizer clPathes = new StringTokenizer(
            System.getProperty("java.class.path"), File.pathSeparator);

        while(clPathes.hasMoreTokens()) {
            String clPath = clPathes.nextToken();

            // trying to load a class file defining the current test from
            // this entry of "java.class.path": the class file may locate
            // at the test's work directory or if it's already compiled,
            // at any directory in classpath
            if (pathValid(clPath, testName))
                return shortTestName;
        }

        // directory with the test's class files was not found.
        // Actually, it means that the invoked test has own Java
        // options such as, for example, "-verify"
        out.println("\nWARNING: the test was not really run due to the following reason:"
            + "\n\tthe invoked test has the own Java option: "
            + testName);
        System.exit(Consts.JCK_STATUS_BASE + Consts.TEST_PASSED);

        return null; // fake return for too smart javac
    }

    /**
     * Invoke the method <i>main(String[])</i> of the test.
     */
    private int invokeMainMethod(Class testCls, String args[]) {
        Class[] methType = { String[].class };
        Object[] methArgs = { args };

        return invokeMethod(testCls, "main", methType, methArgs);
    }

    /**
     * Invoke the method <i>run(String[], PrintStream)</i> of the test.
     */
    private int invokeRunMethod(Class testCls, String args[]) {
        Class[] methType = { String[].class, PrintStream.class };
        Object[] methArgs = { args, out };

        return invokeMethod(testCls, "run", methType, methArgs);
    }

    /**
     * Low level invocation of the test.
     */
    private int invokeMethod(Class<?> testCls, String methodName,
            Class methType[], Object methArgs[]) {

        try {
            Method testMeth = testCls.getMethod(methodName, methType);
            display("\n#### RASagent: invoking method \""
                + testMeth.toString() + "\" ...");

            Object result = testMeth.invoke(null, methArgs);

            display("\n#### RASagent: ... invocation of \""
                + testMeth.toString() + "\" done");
            if (result instanceof Integer) {
                Integer retCode = (Integer) result;
                return retCode.intValue();
            }
        } catch(NoSuchMethodException e) {
            e.printStackTrace();
            out.println("\nFAILURE: RASagent: unable to get method \""
                + methodName + "\" in class "
                + testCls + "\n\tcaught " + e);
            return Consts.TEST_FAILED;
        } catch(Exception e) {
            e.printStackTrace();
            out.println("\nFAILURE: RASagent: caught during invokation of the test class "
                + testCls + " " + e);
            return Consts.TEST_FAILED;
        }

        return -1;
    }

    /**
     * Load class bytes for HotSwap.
     */
    static byte[] loadFromClassFile(String signature) {
        String testPath = clfBasePath + File.separator + signature.substring(
                1, signature.length()-1).replace('/', File.separatorChar) + ".class";
        File classFile = null;

        display("\n#### RASagent: looking for class file\n<RASagent>\t"
            + testPath + " ...");

        try {
            classFile = new File(testPath);
        } catch (NullPointerException e) {
            out.println("\nFAILURE: RASagent: path name to the redefining class file is null");
        }

        display("\n#### RASagent: loading " + classFile.length()
            + " bytes from class file "+ testPath + " ...");
        byte[] buf = new byte[(int) classFile.length()];
        try {
            InputStream in = new FileInputStream(classFile);
            in.read(buf);
            in.close();
        } catch(FileNotFoundException e) {
            e.printStackTrace();
            out.println("\nFAILURE: RASagent: loadFromClassFile: file " +
                classFile.getName() + " not found");
            System.exit(Consts.JCK_STATUS_BASE + Consts.TEST_FAILED);
        } catch (Exception e) {
            e.printStackTrace();
            out.println("\nFAILURE: RASagent: unable to load bytes from the file:\n");
            out.println("\t" + testPath + ": caught " + e);
            System.exit(Consts.JCK_STATUS_BASE + Consts.TEST_FAILED);
        }

        display("\n#### RASagent: ... " + classFile.length() + " bytes loaded");

        return buf;
    }

    /**
     * This method is used in verbose mode. It prints paramter string only
     * in case of verbose mode.
     */
    private static void display(String msg) {
        if (verbose)
            out.println(msg);
    }

    /**
     * This method prints out RASagent usage message.
     */
    private static void printHelp() {
        out.println("\nRASagent usage: RASagent [option, ...] test" +
            "\n\t-ras.help                 print this message and exit" +
            "\n\t-ras.verbose              verbose mode (off by default)" +
            "\n\t-ras.hotswap=mode         enable HotSwap of the running test classes" +
            "\n\t\twhere mode is:" +
            "\n\t\t\t" + HOTSWAP_EVERY_METHOD_ENTRY
                + " - hotswap tested class in its every method entry event" +
            "\n\t\t\t" + HOTSWAP_EVERY_METHOD_ENTRY_FOR_EVERY_CLASS
                + " - hotswap tested class in every method entry event for every class" +
            "\n\t\t\t" + HOTSWAP_EVERY_SINGLE_STEP
                + " - hotswap tested class in its every single step event" +
            "\n\t\t\t" + HOTSWAP_EVERY_EXCEPTION
                + " - hotswap tested class in its every exception event" +
            "\n\t\t\t" + HOTSWAP_EVERY_EXCEPTION_FOR_EVERY_CLASS
                + " - hotswap tested class in every exception event for every class\n" +
            "\n\t-ras.invoke_run           invoke the method run() of the test" +
            "\n\t\tinstead of main() by default");
    }
}
