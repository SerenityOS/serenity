/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * Helper superclass for launching JDI tests out of the CDS archive.
*/

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

import java.io.*;
import java.util.ArrayList;
import sun.tools.jar.Main;

public class CDSJDITest {
    private static final String classesDir = System.getProperty("test.classes");

    public static void runTest(String testname, String[] jarClasses) throws Exception {
        File jarClasslistFile = makeClassList(jarClasses);
        String appJar = buildJar(testname, jarClasses);

        // These are the arguments passed to createJavaProcessBuilder() to launch
        // the JDI test.
        String[] testArgs = {
        // JVM Args:
            // These first three properties are setup by jtreg, and must be passed
            // to the JDI test subprocess because it needs them in order to
            // pass them to the subprocess it will create for the debuggee. This
            // is how the -javaopts are passed to the debuggee. See
            // VMConnection.getDebuggeeVMOptions().
            getPropOpt("test.classes"),
            getPropOpt("test.java.opts"),
            getPropOpt("test.vm.opts"),
            // Pass -showversion to the JDI test just so we get a bit of trace output.
            "-showversion",
        // Main class:
            testname,
        // Args to the Main Class:
            // These argument all follow the above <testname> argument, and are
            // in fact passed to <testname>.main() as java arguments. <testname> will
            // pass them as JVM arguments to the debuggee process it creates.
            "-Xbootclasspath/a:" + appJar,
            "-XX:+UnlockDiagnosticVMOptions",
            "-Xlog:class+path=info",
            "-XX:SharedArchiveFile=./SharedArchiveFile.jsa",
            "-Xshare:on",
            "-showversion"
        };

        // Dump the archive
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
            "-Xbootclasspath/a:" + appJar,
            "-XX:+UnlockDiagnosticVMOptions", "-XX:SharedArchiveFile=./SharedArchiveFile.jsa",
            "-XX:ExtraSharedClassListFile=" + jarClasslistFile.getPath(),
            "-Xshare:dump", "-Xlog:cds");
        OutputAnalyzer outputDump = executeAndLog(pb, "exec");
        for (String jarClass : jarClasses) {
            outputDump.shouldNotContain("Cannot find " + jarClass);
        }
        outputDump.shouldContain("Loading classes to share");
        outputDump.shouldHaveExitValue(0);

        // Run the test specified JDI test
        pb = ProcessTools.createTestJvm(testArgs);
        OutputAnalyzer outputRun = executeAndLog(pb, "exec");
        try {
            outputRun.shouldContain("sharing");
            outputRun.shouldHaveExitValue(0);
        } catch (RuntimeException e) {
            outputRun.shouldContain("Unable to use shared archive");
            outputRun.shouldHaveExitValue(1);
        }
    }

    public static String getPropOpt(String prop) {
        String propVal = System.getProperty(prop);
        if (propVal == null) propVal = "";
        System.out.println(prop + ": '" + propVal  + "'");
        return "-D" + prop + "=" + propVal;
    }

    public static File makeClassList(String appClasses[]) throws Exception {
        File classList = getOutputFile("test.classlist");
        FileOutputStream fos = new FileOutputStream(classList);
        PrintStream ps = new PrintStream(fos);

        addToClassList(ps, appClasses);

        ps.close();
        fos.close();

        return classList;
    }

    public static OutputAnalyzer executeAndLog(ProcessBuilder pb, String logName) throws Exception {
        long started = System.currentTimeMillis();
        OutputAnalyzer output = ProcessTools.executeProcess(pb);
        writeFile(getOutputFile(logName + ".stdout"), output.getStdout());
        writeFile(getOutputFile(logName + ".stderr"), output.getStderr());
        System.out.println("[ELAPSED: " + (System.currentTimeMillis() - started) + " ms]");
        System.out.println("[STDOUT]\n" + output.getStdout());
        System.out.println("[STDERR]\n" + output.getStderr());
        return output;
    }

    private static void writeFile(File file, String content) throws Exception {
        FileOutputStream fos = new FileOutputStream(file);
        PrintStream ps = new PrintStream(fos);
        ps.print(content);
        ps.close();
        fos.close();
    }

    public static File getOutputFile(String name) {
        File dir = new File(System.getProperty("test.classes", "."));
        return new File(dir, getTestNamePrefix() + name);
    }

    private static void addToClassList(PrintStream ps, String classes[]) throws IOException {
        if (classes != null) {
            for (String s : classes) {
                ps.println(s);
            }
        }
    }

    private static String testNamePrefix;

    private static String getTestNamePrefix() {
        if (testNamePrefix == null) {
            StackTraceElement[] elms = (new Throwable()).getStackTrace();
            if (elms.length > 0) {
                for (StackTraceElement n: elms) {
                    if ("main".equals(n.getMethodName())) {
                        testNamePrefix = n.getClassName() + "-";
                        break;
                    }
                }
            }

            if (testNamePrefix == null) {
                testNamePrefix = "";
            }
        }
        return testNamePrefix;
    }

    private static String buildJar(String jarName, String ...classNames)
        throws Exception {

        String jarFullName = classesDir + File.separator + jarName + ".jar";
        createSimpleJar(classesDir, jarFullName, classNames);
        return jarFullName;
    }

    private static void createSimpleJar(String jarClassesDir, String jarName,
        String[] classNames) throws Exception {

        ArrayList<String> args = new ArrayList<String>();
        args.add("cf");
        args.add(jarName);
        addJarClassArgs(args, jarClassesDir, classNames);
        createJar(args);
    }

    private static void addJarClassArgs(ArrayList<String> args, String jarClassesDir,
        String[] classNames) {

        for (String name : classNames) {
            args.add("-C");
            args.add(jarClassesDir);
            args.add(name + ".class");
        }
    }

    private static void createJar(ArrayList<String> args) {
        Main jarTool = new Main(System.out, System.err, "jar");
        if (!jarTool.run(args.toArray(new String[1]))) {
            throw new RuntimeException("jar operation failed");
        }
    }
}
