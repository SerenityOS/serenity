/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
package jdk.test.lib.cds;

import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import jdk.test.lib.Utils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jtreg.SkippedException;

// This class contains common test utilities for testing CDS
public class CDSTestUtils {
    public static final String MSG_RANGE_NOT_WITHIN_HEAP =
        "UseSharedSpaces: Unable to allocate region, range is not within java heap.";
    public static final String MSG_RANGE_ALREADT_IN_USE =
        "Unable to allocate region, java heap range is already in use.";
    public static final String MSG_DYNAMIC_NOT_SUPPORTED =
        "DynamicDumpSharedSpaces is unsupported when base CDS archive is not loaded";
    public static final boolean DYNAMIC_DUMP = Boolean.getBoolean("test.dynamic.cds.archive");

    public interface Checker {
        public void check(OutputAnalyzer output) throws Exception;
    }

    /*
     * INTRODUCTION
     *
     * When testing various CDS functionalities, we need to launch JVM processes
     * using a "launch method" (such as TestCommon.run), and analyze the results of these
     * processes.
     *
     * While typical jtreg tests would use OutputAnalyzer in such cases, due to the
     * complexity of CDS failure modes, we have added the CDSTestUtils.Result class
     * to make the analysis more convenient and less error prone.
     *
     * A Java process can end in one of the following 4 states:
     *
     *    1: Unexpected error - such as JVM crashing. In this case, the "launch method"
     *                          will throw a RuntimeException.
     *    2: Mapping Failure  - this happens when the OS (intermittently) fails to map the
     *                          CDS archive, normally caused by Address Space Layout Randomization.
     *                          We usually treat this as "pass".
     *    3: Normal Exit      - the JVM process has finished without crashing, and the exit code is 0.
     *    4: Abnormal Exit    - the JVM process has finished without crashing, and the exit code is not 0.
     *
     * In most test cases, we need to check the JVM process's output in cases 3 and 4. However, we need
     * to make sure that our test code is not confused by case 2.
     *
     * For example, a JVM process is expected to print the string "Hi" and exit with 0. With the old
     * CDSTestUtils.runWithArchive API, the test may be written as this:
     *
     *     OutputAnalyzer out = CDSTestUtils.runWithArchive(args);
     *     out.shouldContain("Hi");
     *
     * However, if the JVM process fails with mapping failure, the string "Hi" will not be in the output,
     * and your test case will fail intermittently.
     *
     * Instead, the test case should be written as
     *
     *      CDSTestUtils.run(args).assertNormalExit("Hi");
     *
     * EXAMPLES/HOWTO
     *
     * 1. For simple substring matching:
     *
     *      CDSTestUtils.run(args).assertNormalExit("Hi");
     *      CDSTestUtils.run(args).assertNormalExit("a", "b", "x");
     *      CDSTestUtils.run(args).assertAbnormalExit("failure 1", "failure2");
     *
     * 2. For more complex output matching: using Lambda expressions
     *
     *      CDSTestUtils.run(args)
     *         .assertNormalExit(output -> output.shouldNotContain("this should not be printed");
     *      CDSTestUtils.run(args)
     *         .assertAbnormalExit(output -> {
     *             output.shouldNotContain("this should not be printed");
     *             output.shouldHaveExitValue(123);
     *           });
     *
     * 3. Chaining several checks:
     *
     *      CDSTestUtils.run(args)
     *         .assertNormalExit(output -> output.shouldNotContain("this should not be printed")
     *         .assertNormalExit("should have this", "should have that");
     *
     * 4. [Rare use case] if a test sometimes exit normally, and sometimes abnormally:
     *
     *      CDSTestUtils.run(args)
     *         .ifNormalExit("ths string is printed when exiting with 0")
     *         .ifAbNormalExit("ths string is printed when exiting with 1");
     *
     *    NOTE: you usually don't want to write your test case like this -- it should always
     *    exit with the same exit code. (But I kept this API because some existing test cases
     *    behave this way -- need to revisit).
     */
    public static class Result {
        private final OutputAnalyzer output;
        private final CDSOptions options;
        private final boolean hasNormalExit;
        private final String CDS_DISABLED = "warning: CDS is disabled when the";

        public Result(CDSOptions opts, OutputAnalyzer out) throws Exception {
            checkMappingFailure(out);
            this.options = opts;
            this.output = out;
            hasNormalExit = (output.getExitValue() == 0);

            if (hasNormalExit) {
                if ("on".equals(options.xShareMode) &&
                    output.getStderr().contains("java version") &&
                    !output.getStderr().contains(CDS_DISABLED)) {
                    // "-showversion" is always passed in the command-line by the execXXX methods.
                    // During normal exit, we require that the VM to show that sharing was enabled.
                    output.shouldContain("sharing");
                }
            }
        }

        public Result assertNormalExit(Checker checker) throws Exception {
            checker.check(output);
            output.shouldHaveExitValue(0);
            return this;
        }

        public Result assertAbnormalExit(Checker checker) throws Exception {
            checker.check(output);
            output.shouldNotHaveExitValue(0);
            return this;
        }

        // When {--limit-modules, --patch-module, and/or --upgrade-module-path}
        // are specified, CDS is silently disabled for both -Xshare:auto and -Xshare:on.
        public Result assertSilentlyDisabledCDS(Checker checker) throws Exception {
            // this comes from a JVM warning message.
            output.shouldContain(CDS_DISABLED);
            checker.check(output);
            return this;
        }

        public Result assertSilentlyDisabledCDS(int exitCode, String... matches) throws Exception {
            return assertSilentlyDisabledCDS((out) -> {
                out.shouldHaveExitValue(exitCode);
                checkMatches(out, matches);
                   });
        }

        public Result ifNormalExit(Checker checker) throws Exception {
            if (hasNormalExit) {
                checker.check(output);
            }
            return this;
        }

        public Result ifAbnormalExit(Checker checker) throws Exception {
            if (!hasNormalExit) {
                checker.check(output);
            }
            return this;
        }

        public Result ifNoMappingFailure(Checker checker) throws Exception {
            checker.check(output);
            return this;
        }


        public Result assertNormalExit(String... matches) throws Exception {
            checkMatches(output, matches);
            output.shouldHaveExitValue(0);
            return this;
        }

        public Result assertAbnormalExit(String... matches) throws Exception {
            checkMatches(output, matches);
            output.shouldNotHaveExitValue(0);
            return this;
        }
    }

    // A number to be included in the filename of the stdout and the stderr output file.
    static int logCounter = 0;

    private static int getNextLogCounter() {
        return logCounter++;
    }

    // By default, stdout of child processes are logged in files such as
    // <testname>-0000-exec.stdout. If you want to also include the stdout
    // inside jtr files, you can override this in the jtreg command line like
    // "jtreg -Dtest.cds.copy.child.stdout=true ...."
    public static final boolean copyChildStdoutToMainStdout =
        Boolean.getBoolean("test.cds.copy.child.stdout");

    // This property is passed to child test processes
    public static final String TestTimeoutFactor = System.getProperty("test.timeout.factor", "1.0");

    public static final String UnableToMapMsg =
        "Unable to map shared archive: test did not complete";

    // Create bootstrap CDS archive,
    // use extra JVM command line args as a prefix.
    // For CDS tests specifying prefix makes more sense than specifying suffix, since
    // normally there are no classes or arguments to classes, just "-version"
    // To specify suffix explicitly use CDSOptions.addSuffix()
    public static OutputAnalyzer createArchive(String... cliPrefix)
        throws Exception {
        return createArchive((new CDSOptions()).addPrefix(cliPrefix));
    }

    // Create bootstrap CDS archive
    public static OutputAnalyzer createArchive(CDSOptions opts)
        throws Exception {

        startNewArchiveName();

        ArrayList<String> cmd = new ArrayList<String>();

        for (String p : opts.prefix) cmd.add(p);

        cmd.add("-Xshare:dump");
        cmd.add("-Xlog:cds,cds+hashtables");
        if (opts.archiveName == null)
            opts.archiveName = getDefaultArchiveName();
        cmd.add("-XX:SharedArchiveFile=" + opts.archiveName);

        if (opts.classList != null) {
            File classListFile = makeClassList(opts.classList);
            cmd.add("-XX:ExtraSharedClassListFile=" + classListFile.getPath());
        }

        for (String s : opts.suffix) cmd.add(s);

        String[] cmdLine = cmd.toArray(new String[cmd.size()]);
        ProcessBuilder pb = ProcessTools.createTestJvm(cmdLine);
        return executeAndLog(pb, "dump");
    }

    public static boolean isDynamicArchive() {
        return DYNAMIC_DUMP;
    }

    // check result of 'dump-the-archive' operation, that is "-Xshare:dump"
    public static OutputAnalyzer checkDump(OutputAnalyzer output, String... extraMatches)
        throws Exception {

        if (!DYNAMIC_DUMP) {
            output.shouldContain("Loading classes to share");
        } else {
            output.shouldContain("Written dynamic archive 0x");
        }
        output.shouldHaveExitValue(0);

        for (String match : extraMatches) {
            output.shouldContain(match);
        }

        return output;
    }

    // check result of dumping base archive
    public static OutputAnalyzer checkBaseDump(OutputAnalyzer output) throws Exception {
        output.shouldContain("Loading classes to share");
        output.shouldHaveExitValue(0);
        return output;
    }

    // A commonly used convenience methods to create an archive and check the results
    // Creates an archive and checks for errors
    public static OutputAnalyzer createArchiveAndCheck(CDSOptions opts)
        throws Exception {
        return checkDump(createArchive(opts));
    }


    public static OutputAnalyzer createArchiveAndCheck(String... cliPrefix)
        throws Exception {
        return checkDump(createArchive(cliPrefix));
    }


    // This method should be used to check the output of child VM for common exceptions.
    // Most of CDS tests deal with child VM processes for creating and using the archive.
    // However exceptions that occur in the child process do not automatically propagate
    // to the parent process. This mechanism aims to improve the propagation
    // of exceptions and common errors.
    // Exception e argument - an exception to be re-thrown if none of the common
    // exceptions match. Pass null if you wish not to re-throw any exception.
    public static void checkCommonExecExceptions(OutputAnalyzer output, Exception e)
        throws Exception {
        if (output.getStdout().contains("https://bugreport.java.com/bugreport/crash.jsp")) {
            throw new RuntimeException("Hotspot crashed");
        }
        if (output.getStdout().contains("TEST FAILED")) {
            throw new RuntimeException("Test Failed");
        }
        if (output.getOutput().contains("Unable to unmap shared space")) {
            throw new RuntimeException("Unable to unmap shared space");
        }

        // Special case -- sometimes Xshare:on fails because it failed to map
        // at given address. This behavior is platform-specific, machine config-specific
        // and can be random (see ASLR).
        if (isUnableToMap(output)) {
            throw new SkippedException(UnableToMapMsg);
        }

        if (e != null) {
            throw e;
        }
    }

    public static void checkCommonExecExceptions(OutputAnalyzer output) throws Exception {
        checkCommonExecExceptions(output, null);
    }


    // Check the output for indication that mapping of the archive failed.
    // Performance note: this check seems to be rather costly - searching the entire
    // output stream of a child process for multiple strings. However, it is necessary
    // to detect this condition, a failure to map an archive, since this is not a real
    // failure of the test or VM operation, and results in a test being "skipped".
    // Suggestions to improve:
    // 1. VM can designate a special exit code for such condition.
    // 2. VM can print a single distinct string indicating failure to map an archive,
    //    instead of utilizing multiple messages.
    // These are suggestions to improve testibility of the VM. However, implementing them
    // could also improve usability in the field.
    public static boolean isUnableToMap(OutputAnalyzer output) {
        String outStr = output.getOutput();
        if ((output.getExitValue() == 1) &&
            (outStr.contains(MSG_RANGE_NOT_WITHIN_HEAP) || outStr.contains(MSG_DYNAMIC_NOT_SUPPORTED))) {
            return true;
        }

        return false;
    }

    public static void checkMappingFailure(OutputAnalyzer out) throws SkippedException {
        if (isUnableToMap(out)) {
            throw new SkippedException(UnableToMapMsg);
        }
    }

    public static Result run(String... cliPrefix) throws Exception {
        CDSOptions opts = new CDSOptions();
        opts.setArchiveName(getDefaultArchiveName());
        opts.addPrefix(cliPrefix);
        return new Result(opts, runWithArchive(opts));
    }

    public static Result run(CDSOptions opts) throws Exception {
        return new Result(opts, runWithArchive(opts));
    }

    // Dump a classlist using the -XX:DumpLoadedClassList option.
    public static Result dumpClassList(String classListName, String... cli)
        throws Exception {
        CDSOptions opts = (new CDSOptions())
            .setUseVersion(false)
            .setXShareMode("auto")
            .addPrefix("-XX:DumpLoadedClassList=" + classListName)
            .addSuffix(cli);
        Result res = run(opts).assertNormalExit();
        return res;
    }

    // Execute JVM with CDS archive, specify command line args suffix
    public static OutputAnalyzer runWithArchive(String... cliPrefix)
        throws Exception {

        return runWithArchive( (new CDSOptions())
                               .setArchiveName(getDefaultArchiveName())
                               .addPrefix(cliPrefix) );
    }


    // Execute JVM with CDS archive, specify CDSOptions
    public static OutputAnalyzer runWithArchive(CDSOptions opts)
        throws Exception {

        ArrayList<String> cmd = new ArrayList<String>();

        for (String p : opts.prefix) cmd.add(p);

        cmd.add("-Xshare:" + opts.xShareMode);
        cmd.add("-Dtest.timeout.factor=" + TestTimeoutFactor);

        if (!opts.useSystemArchive) {
            if (opts.archiveName == null)
                opts.archiveName = getDefaultArchiveName();
            cmd.add("-XX:SharedArchiveFile=" + opts.archiveName);
        }

        if (opts.useVersion)
            cmd.add("-version");

        for (String s : opts.suffix) cmd.add(s);

        String[] cmdLine = cmd.toArray(new String[cmd.size()]);
        ProcessBuilder pb = ProcessTools.createTestJvm(cmdLine);
        return executeAndLog(pb, "exec");
    }


    // A commonly used convenience methods to create an archive and check the results
    // Creates an archive and checks for errors
    public static OutputAnalyzer runWithArchiveAndCheck(CDSOptions opts) throws Exception {
        return checkExec(runWithArchive(opts));
    }


    public static OutputAnalyzer runWithArchiveAndCheck(String... cliPrefix) throws Exception {
        return checkExec(runWithArchive(cliPrefix));
    }


    public static OutputAnalyzer checkExec(OutputAnalyzer output,
                                     String... extraMatches) throws Exception {
        CDSOptions opts = new CDSOptions();
        return checkExec(output, opts, extraMatches);
    }


    // check result of 'exec' operation, that is when JVM is run using the archive
    public static OutputAnalyzer checkExec(OutputAnalyzer output, CDSOptions opts,
                                     String... extraMatches) throws Exception {
        try {
            if ("on".equals(opts.xShareMode)) {
                output.shouldContain("sharing");
            }
            output.shouldHaveExitValue(0);
        } catch (RuntimeException e) {
            checkCommonExecExceptions(output, e);
            return output;
        }

        checkMatches(output, extraMatches);
        return output;
    }


    public static OutputAnalyzer checkExecExpectError(OutputAnalyzer output,
                                             int expectedExitValue,
                                             String... extraMatches) throws Exception {
        if (isUnableToMap(output)) {
            throw new SkippedException(UnableToMapMsg);
        }

        output.shouldHaveExitValue(expectedExitValue);
        checkMatches(output, extraMatches);
        return output;
    }

    public static OutputAnalyzer checkMatches(OutputAnalyzer output,
                                              String... matches) throws Exception {
        for (String match : matches) {
            output.shouldContain(match);
        }
        return output;
    }

    private static final String outputDir;
    private static final File outputDirAsFile;

    static {
        outputDir = System.getProperty("user.dir", ".");
        outputDirAsFile = new File(outputDir);
    }

    public static String getOutputDir() {
        return outputDir;
    }

    public static File getOutputDirAsFile() {
        return outputDirAsFile;
    }

    // get the file object for the test artifact
    public static File getTestArtifact(String name, boolean checkExistence) {
        File file = new File(outputDirAsFile, name);

        if (checkExistence && !file.exists()) {
            throw new RuntimeException("Cannot find " + file.getPath());
        }

        return file;
    }


    // create file containing the specified class list
    public static File makeClassList(String classes[])
        throws Exception {
        return makeClassList(testName + "-", classes);
    }

    // create file containing the specified class list
    public static File makeClassList(String testCaseName, String classes[])
        throws Exception {

        File classList = getTestArtifact(testCaseName + "test.classlist", false);
        FileOutputStream fos = new FileOutputStream(classList);
        PrintStream ps = new PrintStream(fos);

        addToClassList(ps, classes);

        ps.close();
        fos.close();

        return classList;
    }


    public static void addToClassList(PrintStream ps, String classes[])
        throws IOException
    {
        if (classes != null) {
            for (String s : classes) {
                ps.println(s);
            }
        }
    }

    private static String testName = Utils.TEST_NAME.replace('/', '.');

    private static final SimpleDateFormat timeStampFormat =
        new SimpleDateFormat("HH'h'mm'm'ss's'SSS");

    private static String defaultArchiveName;

    // Call this method to start new archive with new unique name
    public static void startNewArchiveName() {
        defaultArchiveName = testName +
            timeStampFormat.format(new Date()) + ".jsa";
    }

    public static String getDefaultArchiveName() {
        return defaultArchiveName;
    }


    // ===================== FILE ACCESS convenience methods
    public static File getOutputFile(String name) {
        return new File(outputDirAsFile, testName + "-" + name);
    }

    public static String getOutputFileName(String name) {
        return getOutputFile(name).getName();
    }


    public static File getOutputSourceFile(String name) {
        return new File(outputDirAsFile, name);
    }


    public static File getSourceFile(String name) {
        File dir = new File(System.getProperty("test.src", "."));
        return new File(dir, name);
    }


    // ============================= Logging
    public static OutputAnalyzer executeAndLog(ProcessBuilder pb, String logName) throws Exception {
        long started = System.currentTimeMillis();
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        String logFileNameStem =
            String.format("%04d", getNextLogCounter()) + "-" + logName;

        File stdout = getOutputFile(logFileNameStem + ".stdout");
        File stderr = getOutputFile(logFileNameStem + ".stderr");

        writeFile(stdout, output.getStdout());
        writeFile(stderr, output.getStderr());
        System.out.println("[ELAPSED: " + (System.currentTimeMillis() - started) + " ms]");
        System.out.println("[logging stdout to " + stdout + "]");
        System.out.println("[logging stderr to " + stderr + "]");
        System.out.println("[STDERR]\n" + output.getStderr());

        if (copyChildStdoutToMainStdout)
            System.out.println("[STDOUT]\n" + output.getStdout());

        return output;
    }


    private static void writeFile(File file, String content) throws Exception {
        FileOutputStream fos = new FileOutputStream(file);
        PrintStream ps = new PrintStream(fos);
        ps.print(content);
        ps.close();
        fos.close();
    }

    // Format a line that defines an extra symbol in the file specify by -XX:SharedArchiveConfigFile=<file>
    public static String formatArchiveConfigSymbol(String symbol) {
        int refCount = -1; // This is always -1 in the current HotSpot implementation.
        if (isAsciiPrintable(symbol)) {
            return symbol.length() + " " + refCount + ": " + symbol;
        } else {
            StringBuilder sb = new StringBuilder();
            int utf8_length = escapeArchiveConfigString(sb, symbol);
            return utf8_length + " " + refCount + ": " + sb.toString();
        }
    }

    // This method generates the same format as HashtableTextDump::put_utf8() in HotSpot,
    // to be used by -XX:SharedArchiveConfigFile=<file>.
    private static int escapeArchiveConfigString(StringBuilder sb, String s) {
        byte arr[];
        try {
            arr = s.getBytes("UTF8");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("Unexpected", e);
        }
        for (int i = 0; i < arr.length; i++) {
            char ch = (char)(arr[i] & 0xff);
            if (isAsciiPrintable(ch)) {
                sb.append(ch);
            } else if (ch == '\t') {
                sb.append("\\t");
            } else if (ch == '\r') {
                sb.append("\\r");
            } else if (ch == '\n') {
                sb.append("\\n");
            } else if (ch == '\\') {
                sb.append("\\\\");
            } else {
                String hex = Integer.toHexString(ch);
                if (ch < 16) {
                    sb.append("\\x0");
                } else {
                    sb.append("\\x");
                }
                sb.append(hex);
            }
        }

        return arr.length;
    }

    private static boolean isAsciiPrintable(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!isAsciiPrintable(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAsciiPrintable(char ch) {
        return ch >= 32 && ch < 127;
    }
}
