/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

import jdk.test.lib.Utils;
import jdk.test.lib.BuildHelper;
import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.Platform;
import jdk.test.lib.cds.CDSOptions;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.cds.CDSTestUtils.Result;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import jtreg.SkippedException;
import cdsutils.DynamicDumpHelper;


/**
 * This is a test utility class for common AppCDS test functionality.
 *
 * Various methods use (String ...) for passing VM options. Note that the order
 * of the VM options are important in certain cases. Many methods take arguments like
 *
 *    (String prefix[], String suffix[], String... opts)
 *
 * Note that the order of the VM options is:
 *
 *    prefix + opts + suffix
 */
public class TestCommon extends CDSTestUtils {
    private static final String JSA_FILE_PREFIX = CDSTestUtils.getOutputDir() +
        File.separator;

    private static final SimpleDateFormat timeStampFormat =
        new SimpleDateFormat("HH'h'mm'm'ss's'SSS");

    private static final String timeoutFactor =
        System.getProperty("test.timeout.factor", "1.0");

    private static String currentArchiveName;

    // Call this method to start new archive with new unique name
    public static void startNewArchiveName() {
        deletePriorArchives();
        currentArchiveName = getNewArchiveName();
    }

    // Call this method to get current archive name
    public static String getCurrentArchiveName() {
        return currentArchiveName;
    }

    public static void setCurrentArchiveName(String archiveName) {
        currentArchiveName = archiveName;
    }

    public static String getNewArchiveName() {
        return getNewArchiveName(null);
    }

    public static String getNewArchiveName(String stem) {
        if (stem == null) {
            stem = "appcds";
        }
        return JSA_FILE_PREFIX + stem + "-" +
            timeStampFormat.format(new Date()) + ".jsa";
    }

    // Attempt to clean old archives to preserve space
    // Archives are large artifacts (20Mb or more), and much larger than
    // most other artifacts created in jtreg testing.
    // Therefore it is a good idea to clean the old archives when they are not needed.
    // In most cases the deletion attempt will succeed; on rare occasion the
    // delete operation will fail since the system or VM process still holds a handle
    // to the file; in such cases the File.delete() operation will silently fail, w/o
    // throwing an exception, thus allowing testing to continue.
    public static void deletePriorArchives() {
        File dir = CDSTestUtils.getOutputDirAsFile();
        String files[] = dir.list();
        for (String name : files) {
            if (name.startsWith("appcds-") && name.endsWith(".jsa")) {
                if (!(new File(dir, name)).delete())
                    System.out.println("deletePriorArchives(): delete failed for file " + name);
            }
        }
    }

    // Create AppCDS archive using most common args - convenience method
    // Legacy name preserved for compatibility
    public static OutputAnalyzer dump(String appJar, String classList[],
                                               String... suffix) throws Exception {
        return createArchive(appJar, classList, suffix);
    }

    public static OutputAnalyzer dump(String appJarDir, String appJar, String classList[],
                                               String... suffix) throws Exception {
        return createArchive(appJarDir, appJar, classList, suffix);
    }

    /**
     * Dump the base archive. The JDK's default class list is used (unless otherwise specified
     * in cmdLineSuffix).
     */
    public static OutputAnalyzer dumpBaseArchive(String baseArchiveName, String ... cmdLineSuffix)
        throws Exception
    {
        CDSOptions opts = new CDSOptions();
        opts.setArchiveName(baseArchiveName);
        opts.addSuffix(cmdLineSuffix);
        opts.addSuffix("-Djava.class.path=");
        OutputAnalyzer out = CDSTestUtils.createArchive(opts);
        CDSTestUtils.checkBaseDump(out);
        return out;
    }

    // Create AppCDS archive using most common args - convenience method
    public static OutputAnalyzer createArchive(String appJar, String classList[],
                                               String... suffix) throws Exception {
        AppCDSOptions opts = (new AppCDSOptions()).setAppJar(appJar);
        opts.setClassList(classList);
        opts.addSuffix(suffix);
        return createArchive(opts);
    }

    public static OutputAnalyzer createArchive(String appJarDir, String appJar, String classList[],
                                               String... suffix) throws Exception {
        AppCDSOptions opts = (new AppCDSOptions()).setAppJar(appJar);
        opts.setAppJarDir(appJarDir);
        opts.setClassList(classList);
        opts.addSuffix(suffix);
        return createArchive(opts);
    }

    // Simulate -Xshare:dump with -XX:ArchiveClassesAtExit. See comments around patchJarForDynamicDump()
    private static final Class tmp = DynamicDumpHelper.class;

    // name of the base archive to be used for dynamic dump
    private static String tempBaseArchive = null;

    private static void captureVerifyOpts(ArrayList<String> opts, ArrayList<String> verifyOpts) {
        boolean addedDiagnosticOpt = false;
        for (String s : opts) {
            if (s.startsWith("-XX:-BytecodeVerification")) {
                if (!addedDiagnosticOpt) {
                    verifyOpts.add("-XX:+UnlockDiagnosticVMOptions");
                    addedDiagnosticOpt = true;
                }
                verifyOpts.add(s);
            }
            if (s.startsWith("-Xverify")) {
                verifyOpts.add(s);
            }
        }
    }

    // Create AppCDS archive using appcds options
    public static OutputAnalyzer createArchive(AppCDSOptions opts)
        throws Exception {
        ArrayList<String> cmd = new ArrayList<String>();
        ArrayList<String> verifyOpts = new ArrayList<String>();
        startNewArchiveName();

        for (String p : opts.prefix) cmd.add(p);

        if (opts.appJar != null) {
            cmd.add("-cp");
            cmd.add(opts.appJar);
            File jf = new File(opts.appJar);
            if (DYNAMIC_DUMP && !jf.isDirectory()) {
                patchJarForDynamicDump(opts.appJar);
            }
        } else {
            cmd.add("-Djava.class.path=");
        }

        if (opts.archiveName == null) {
            opts.archiveName = getCurrentArchiveName();
        }

        if (DYNAMIC_DUMP) {
            File baseArchive = null;
            captureVerifyOpts(opts.suffix, verifyOpts);
            int size = verifyOpts.size();
            if (tempBaseArchive == null || !(new File(tempBaseArchive)).isFile() || size > 0) {
                tempBaseArchive = getNewArchiveName("tempBaseArchive");
                if (size == 0) {
                    dumpBaseArchive(tempBaseArchive);
                } else {
                    dumpBaseArchive(tempBaseArchive, verifyOpts.toArray(new String[size]));
                }
            }
            cmd.add("-Xshare:on");
            cmd.add("-XX:SharedArchiveFile=" + tempBaseArchive);
            cmd.add("-XX:ArchiveClassesAtExit=" + opts.archiveName);
            cmd.add("-Xlog:cds");
            cmd.add("-Xlog:cds+dynamic");
            boolean mainModuleSpecified = false;
            boolean patchModuleSpecified = false;
            for (String s : opts.suffix) {
                if (s.length() == 0) {
                    continue;
                }
                if (s.equals("-m")) {
                    mainModuleSpecified = true;
                }
                if (s.startsWith("--patch-module=")) {
                    patchModuleSpecified = true;
                }
                cmd.add(s);
            }

            if (opts.appJar != null) {
                // classlist is supported only when we have a Jar file to patch (to insert
                // cdsutils.DynamicDumpHelper)
                if (opts.classList == null) {
                    throw new RuntimeException("test.dynamic.dump requires classList file");
                }

                if (!mainModuleSpecified && !patchModuleSpecified) {
                    cmd.add("cdsutils.DynamicDumpHelper");
                    File classListFile = makeClassList(opts.classList);
                    cmd.add(classListFile.getPath());
                }
            } else {
                if (!mainModuleSpecified && !patchModuleSpecified) {
                    // If you have an empty classpath, you cannot specify a classlist!
                    if (opts.classList != null && opts.classList.length > 0) {
                        throw new RuntimeException("test.dynamic.dump is not supported with an empty classpath while the classlist is not empty");
                    }
                    cmd.add("-version");
                }
            }
        } else {
            // static dump
            cmd.add("-Xshare:dump");
            cmd.add("-Xlog:cds");
            cmd.add("-XX:SharedArchiveFile=" + opts.archiveName);

            if (opts.classList != null) {
                File classListFile = makeClassList(opts.classList);
                cmd.add("-XX:ExtraSharedClassListFile=" + classListFile.getPath());
            }
            for (String s : opts.suffix) {
                cmd.add(s);
            }
        }

        ProcessBuilder pb = ProcessTools.createTestJvm(cmd);
        if (opts.appJarDir != null) {
            pb.directory(new File(opts.appJarDir));
        }

        OutputAnalyzer output = executeAndLog(pb, "dump");
        if (DYNAMIC_DUMP && isUnableToMap(output)) {
            throw new SkippedException(UnableToMapMsg);
        }
        return output;
    }

    // This allows you to run the AppCDS tests with JFR enabled at runtime (though not at
    // dump time, as that's uncommon for typical AppCDS users).
    //
    // To run in this special mode, add the following to your jtreg command-line
    //    -Dtest.cds.run.with.jfr=true
    //
    // Some AppCDS tests are not compatible with this mode. See the group
    // hotspot_appcds_with_jfr in ../../TEST.ROOT for details.
    private static final boolean RUN_WITH_JFR = Boolean.getBoolean("test.cds.run.with.jfr");
    // This method simulates -Xshare:dump with -XX:ArchiveClassesAtExit. This way, we
    // can re-use many tests (outside of the ./dynamicArchive directory) for testing
    // general features of JDK-8215311 (JEP 350: Dynamic CDS Archives).
    //
    // We insert the cdsutils/DynamicDumpHelper.class into the first Jar file in
    // the classpath. We use this class to load all the classes specified in the classlist.
    //
    // There's no need to change the run-time command-line: in this special mode, two
    // archives are involved. The command-line specifies only the top archive. However,
    // the location of the base archive is recorded in the top archive, so it can be
    // determined by the JVM at runtime start-up.
    //
    // To run in this special mode, specify the following in your jtreg command-line
    //    -Dtest.dynamic.cds.archive=true
    //
    // Note that some tests are not compatible with this special mode, including
    //    + Tests in ./dynamicArchive: these tests are specifically written for
    //      dynamic archive, and do not use TestCommon.createArchive(), which works
    //      together with patchJarForDynamicDump().
    //    + Tests related to cached objects and shared strings: dynamic dumping
    //      does not support these.
    //    + Custom loader tests: DynamicDumpHelper doesn't support the required
    //      classlist syntax. (FIXME).
    //    + Extra symbols and extra strings.
    // See the hotspot_appcds_dynamic in ../../TEST.ROOT for details.
    //
    // To run all tests that are compatible with this mode:
    //    cd test/hotspot/jtreg
    //    jtreg -Dtest.dynamic.cds.archive=true :hotspot_appcds_dynamic
    //
    private static void patchJarForDynamicDump(String cp) throws Exception {
        System.out.println("patchJarForDynamicDump: classpath = " + cp);
        String firstJar = cp;
        int n = firstJar.indexOf(File.pathSeparator);
        if (n > 0) {
            firstJar = firstJar.substring(0, n);
        }
        String classDir = System.getProperty("test.classes");
        String expected = getOutputDir() + File.separator;

        if (!firstJar.startsWith(expected)) {
            throw new RuntimeException("FIXME: jar file not at a supported location ('"
                                       + expected + "'): " + firstJar);
        }

        String replaceJar = firstJar + ".tmp";
        String patchClass = "cdsutils/DynamicDumpHelper.class";
        ZipFile zipFile = new ZipFile(firstJar);
        byte[] buf = new byte[1024];
        int len;
        if (zipFile.getEntry(patchClass) == null) {
            FileOutputStream fout = new FileOutputStream(replaceJar);
            final ZipOutputStream zos = new ZipOutputStream(fout);

            zos.putNextEntry(new ZipEntry(patchClass));
            InputStream is = new FileInputStream(classDir + File.separator + patchClass);
            while ((len = (is.read(buf))) > 0) {
                zos.write(buf, 0, len);
            }
            zos.closeEntry();
            is.close();

            for (Enumeration e = zipFile.entries(); e.hasMoreElements(); ) {
                ZipEntry entryIn = (ZipEntry) e.nextElement();
                zos.putNextEntry(entryIn);
                is = zipFile.getInputStream(entryIn);
                while ((len = is.read(buf)) > 0) {
                    zos.write(buf, 0, len);
                }
                zos.closeEntry();
                is.close();
            }

            zos.close();
            fout.close();
            zipFile.close();

            File oldFile = new File(firstJar);
            File newFile = new File(replaceJar);
            oldFile.delete();
            newFile.renameTo(oldFile);
            System.out.println("firstJar = " + firstJar + " Modified");
        } else {
            zipFile.close();
            System.out.println("firstJar = " + firstJar);
        }
    }

    // Execute JVM using AppCDS archive with specified AppCDSOptions
    public static OutputAnalyzer runWithArchive(AppCDSOptions opts)
        throws Exception {

        ArrayList<String> cmd = new ArrayList<String>();

        for (String p : opts.prefix) cmd.add(p);

        cmd.add("-Xshare:" + opts.xShareMode);
        cmd.add("-showversion");
        cmd.add("-XX:SharedArchiveFile=" + getCurrentArchiveName());
        cmd.add("-Dtest.timeout.factor=" + timeoutFactor);

        if (opts.appJar != null) {
            cmd.add("-cp");
            cmd.add(opts.appJar);
        }

        for (String s : opts.suffix) cmd.add(s);

        if (RUN_WITH_JFR) {
            boolean usesJFR = false;
            for (String s : cmd) {
                if (s.startsWith("-XX:StartFlightRecording") || s.startsWith("-XX:FlightRecorderOptions")) {
                    System.out.println("JFR option might have been specified. Don't interfere: " + s);
                    usesJFR = true;
                    break;
                }
            }
            if (!usesJFR) {
                System.out.println("JFR option not specified. Enabling JFR ...");
                cmd.add(0, "-XX:StartFlightRecording:dumponexit=true");
                System.out.println(cmd);
            }
        }

        ProcessBuilder pb = ProcessTools.createTestJvm(cmd);
        if (opts.appJarDir != null) {
            pb.directory(new File(opts.appJarDir));
        }
        return executeAndLog(pb, "exec");
    }


    public static OutputAnalyzer execCommon(String... suffix) throws Exception {
        AppCDSOptions opts = (new AppCDSOptions());
        opts.addSuffix(suffix);
        return runWithArchive(opts);
    }

    // This is the new API for running a Java process with CDS enabled.
    // See comments in the CDSTestUtils.Result class for how to use this method.
    public static Result run(String... suffix) throws Exception {
        AppCDSOptions opts = (new AppCDSOptions());
        opts.addSuffix(suffix);
        return new Result(opts, runWithArchive(opts));
    }

    public static Result runWithoutCDS(String... suffix) throws Exception {
        AppCDSOptions opts = (new AppCDSOptions());
        opts.addSuffix(suffix).setXShareMode("off");;
        return new Result(opts, runWithArchive(opts));
    }

    public static Result runWithRelativePath(String jarDir, String... suffix) throws Exception {
        AppCDSOptions opts = (new AppCDSOptions());
        opts.setAppJarDir(jarDir);
        opts.addSuffix(suffix);
        return new Result(opts, runWithArchive(opts));
    }

    public static OutputAnalyzer exec(String appJar, String... suffix) throws Exception {
        AppCDSOptions opts = (new AppCDSOptions()).setAppJar(appJar);
        opts.addSuffix(suffix);
        return runWithArchive(opts);
    }

    public static Result runWithModules(String prefix[], String upgrademodulepath, String modulepath,
                                            String mid, String... testClassArgs) throws Exception {
        AppCDSOptions opts = makeModuleOptions(prefix, upgrademodulepath, modulepath,
                                               mid, testClassArgs);
        return new Result(opts, runWithArchive(opts));
    }

    public static OutputAnalyzer execAuto(String... suffix) throws Exception {
        AppCDSOptions opts = (new AppCDSOptions());
        opts.addSuffix(suffix).setXShareMode("auto");
        return runWithArchive(opts);
    }

    public static OutputAnalyzer execOff(String... suffix) throws Exception {
        AppCDSOptions opts = (new AppCDSOptions());
        opts.addSuffix(suffix).setXShareMode("off");
        return runWithArchive(opts);
    }


    private static AppCDSOptions makeModuleOptions(String prefix[], String upgrademodulepath, String modulepath,
                                            String mid, String testClassArgs[]) {
        AppCDSOptions opts = (new AppCDSOptions());

        opts.addPrefix(prefix);
        if (upgrademodulepath == null) {
            opts.addSuffix("-p", modulepath, "-m", mid);
        } else {
            opts.addSuffix("--upgrade-module-path", upgrademodulepath,
                           "-p", modulepath, "-m", mid);
        }
        opts.addSuffix(testClassArgs);
        return opts;
    }

    public static OutputAnalyzer execModule(String prefix[], String upgrademodulepath, String modulepath,
                                            String mid, String... testClassArgs)
        throws Exception {
        AppCDSOptions opts = makeModuleOptions(prefix, upgrademodulepath, modulepath,
                                               mid, testClassArgs);
        return runWithArchive(opts);
    }

    // A common operation: dump, then check results
    public static OutputAnalyzer testDump(String appJar, String classList[],
                                          String... suffix) throws Exception {
        OutputAnalyzer output = dump(appJar, classList, suffix);
        if (DYNAMIC_DUMP) {
            output.shouldContain("Written dynamic archive");
        } else {
            output.shouldContain("Loading classes to share");
        }
        output.shouldHaveExitValue(0);
        return output;
    }

    public static OutputAnalyzer testDump(String appJarDir, String appJar, String classList[],
                                          String... suffix) throws Exception {
        OutputAnalyzer output = dump(appJarDir, appJar, classList, suffix);
        if (DYNAMIC_DUMP) {
            output.shouldContain("Written dynamic archive");
        } else {
            output.shouldContain("Loading classes to share");
        }
        output.shouldHaveExitValue(0);
        return output;
    }

    /**
     * Simple test -- dump and execute appJar with the given classList in classlist.
     */
    public static OutputAnalyzer test(String appJar, String classList[], String... args)
        throws Exception {
        testDump(appJar, classList);

        OutputAnalyzer output = exec(appJar, args);
        return checkExec(output);
    }


    public static OutputAnalyzer checkExecReturn(OutputAnalyzer output, int ret,
                           boolean checkContain, String... matches) throws Exception {
        try {
            for (String s : matches) {
                if (checkContain) {
                    output.shouldContain(s);
                } else {
                    output.shouldNotContain(s);
                }
            }
            output.shouldHaveExitValue(ret);
        } catch (Exception e) {
            checkCommonExecExceptions(output, e);
        }

        return output;
    }

    // Convenience concatenation utils
    public static String[] list(String ...args) {
        return args;
    }


    public static String[] list(String arg, int count) {
        ArrayList<String> stringList = new ArrayList<String>();
        for (int i = 0; i < count; i++) {
            stringList.add(arg);
        }

        String outputArray[] = stringList.toArray(new String[stringList.size()]);
        return outputArray;
    }


    public static String[] concat(String... args) {
        return list(args);
    }


    public static String[] concat(String prefix[], String... extra) {
        ArrayList<String> list = new ArrayList<String>();
        for (String s : prefix) {
            list.add(s);
        }
        for (String s : extra) {
            list.add(s);
        }

        return list.toArray(new String[list.size()]);
    }

    public static String[] concat(String prefix, String[] extra) {
        ArrayList<String> list = new ArrayList<String>();
        list.add(prefix);
        for (String s : extra) {
            list.add(s);
        }

        return list.toArray(new String[list.size()]);
    }

    // ===================== Concatenate paths
    public static String concatPaths(String... paths) {
        String prefix = "";
        String s = "";
        for (String p : paths) {
            s += prefix;
            s += p;
            prefix = File.pathSeparator;
        }
        return s;
    }


    public static String getTestJar(String jar) {
        File jarFile = CDSTestUtils.getTestArtifact(jar, true);
        if (!jarFile.isFile()) {
            throw new RuntimeException("Not a regular file: " + jarFile.getPath());
        }
        return jarFile.getPath();
    }


    public static String getTestDir(String d) {
        File dirFile = CDSTestUtils.getTestArtifact(d, true);
        if (!dirFile.isDirectory()) {
            throw new RuntimeException("Not a directory: " + dirFile.getPath());
        }
        return dirFile.getPath();
    }

    public static boolean checkOutputStrings(String outputString1,
                                             String outputString2,
                                             String split_regex) {
        String[] sa1 = outputString1.split(split_regex);
        String[] sa2 = outputString2.split(split_regex);
        Arrays.sort(sa1);
        Arrays.sort(sa2);

        int i = 0;
        for (String s : sa1) {
            if (!s.equals(sa2[i])) {
                throw new RuntimeException(s + " is different from " + sa2[i]);
            }
            i ++;
        }
        return true;
    }

    static Pattern pattern;

    static void findAllClasses(ArrayList<String> list) throws Exception {
        // Find all the classes in the jrt file system
        pattern = Pattern.compile("/modules/[a-z.]*[a-z]+/([^-]*)[.]class");
        FileSystem fs = FileSystems.getFileSystem(URI.create("jrt:/"));
        Path base = fs.getPath("/modules/");
        findAllClassesAtPath(base, list);
    }

    private static void findAllClassesAtPath(Path p, ArrayList<String> list) throws Exception {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(p)) {
            for (Path entry: stream) {
                Matcher matcher = pattern.matcher(entry.toString());
                if (matcher.find()) {
                    String className = matcher.group(1);
                    list.add(className);
                }
                try {
                    findAllClassesAtPath(entry, list);
                } catch (Exception ex) {}
            }
        }
    }

    public static String composeRelPath(String appJar) {
         int idx = appJar.lastIndexOf(File.separator);
         String jarName = appJar.substring(idx + 1);
         String jarDir = appJar.substring(0, idx);
         String lastDir = jarDir.substring(jarDir.lastIndexOf(File.separator));
         String relPath = jarDir + File.separator + ".." + File.separator + lastDir;
         String newJar = relPath + File.separator + jarName;
         return newJar;
    }


    public static File createSymLink(String appJar) throws Exception {
         int idx = appJar.lastIndexOf(File.separator);
         String jarName = appJar.substring(idx + 1);
         String jarDir = appJar.substring(0, idx);
         File origJar = new File(jarDir, jarName);
         String linkedJarName = "linked_" + jarName;
         File linkedJar = null;
         if (!Platform.isWindows()) {
             linkedJar = new File(jarDir, linkedJarName);
             if (linkedJar.exists()) {
                 linkedJar.delete();
             }
             Files.createSymbolicLink(linkedJar.toPath(), origJar.toPath());
         }
         return linkedJar;
    }

    // Remove all UL log messages from a JVM's STDOUT (such as those printed by -Xlog:cds)
    static Pattern logPattern = Pattern.compile("^\\[[0-9. ]*s\\].*");
    public static String filterOutLogs(String stdout) {
        StringBuilder sb = new StringBuilder();
        String prefix = "";
        for (String line : stdout.split("\n")) {
            if (logPattern.matcher(line).matches()) {
                continue;
            }
            sb.append(prefix);
            sb.append(line);
            prefix = "\n";
        }
        if (stdout.endsWith("\n")) {
            // String.split("A\n") returns {"A"}, not {"A", ""}.
            sb.append("\n");
        }
        return sb.toString();
    }
}
