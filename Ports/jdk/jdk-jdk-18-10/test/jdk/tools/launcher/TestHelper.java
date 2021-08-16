/*
 * Copyright (c) 2008, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.OutputStream;
import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.regex.Pattern;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.Set;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Arrays;
import java.util.spi.ToolProvider;

import static java.nio.file.StandardCopyOption.*;
import static java.nio.file.StandardOpenOption.*;

/**
 * This class provides some common utilities for the launcher tests.
 */
public class TestHelper {
    // commonly used jtreg constants
    static final File TEST_CLASSES_DIR;
    static final File TEST_SOURCES_DIR;

    static final String JAVAHOME = System.getProperty("java.home");
    static final String JAVA_BIN;
    static final String JAVA_LIB;
    static final String javaCmd;
    static final String javawCmd;
    static final String javacCmd;
    static final String jarCmd;
    static final boolean haveServerVM;
    static final boolean haveClientVM;

    static final ToolProvider compiler = ToolProvider.findFirst("javac").orElse(null);

    static final boolean debug = Boolean.getBoolean("TestHelper.Debug");
    static final boolean isWindows =
            System.getProperty("os.name", "unknown").startsWith("Windows");
    static final boolean isMacOSX =
            System.getProperty("os.name", "unknown").contains("OS X");
    static final boolean is64Bit =
            System.getProperty("sun.arch.data.model").equals("64");
    static final boolean is32Bit =
            System.getProperty("sun.arch.data.model").equals("32");
    static final boolean isLinux =
            System.getProperty("os.name", "unknown").startsWith("Linux");
    static final boolean isAIX =
            System.getProperty("os.name", "unknown").startsWith("AIX");
    static final String LIBJVM = isWindows
                        ? "jvm.dll"
                        : "libjvm" + (isMacOSX ? ".dylib" : ".so");

    // make a note of the golden default locale
    static final Locale DefaultLocale = Locale.getDefault();

    static final String JAVA_FILE_EXT   = ".java";
    static final String CLASS_FILE_EXT  = ".class";
    static final String JAR_FILE_EXT    = ".jar";
    static final String EXE_FILE_EXT    = ".exe";
    static final String MAC_DSYM_EXT    = ".dsym";
    static final String NIX_DBGINFO_EXT = ".debuginfo";
    static final String JLDEBUG_KEY     = "_JAVA_LAUNCHER_DEBUG";
    static final String EXPECTED_MARKER = "TRACER_MARKER:About to EXEC";
    static final String TEST_PREFIX     = "###TestError###: ";

    static int testExitValue = 0;

    static {
        String tmp = System.getProperty("test.classes", null);
        if (tmp == null) {
            throw new Error("property test.classes not defined ??");
        }
        TEST_CLASSES_DIR = new File(tmp).getAbsoluteFile();

        tmp = System.getProperty("test.src", null);
        if (tmp == null) {
            throw new Error("property test.src not defined ??");
        }
        TEST_SOURCES_DIR = new File(tmp).getAbsoluteFile();

        if (is64Bit && is32Bit) {
            throw new RuntimeException("arch model cannot be both 32 and 64 bit");
        }
        if (!is64Bit && !is32Bit) {
            throw new RuntimeException("arch model is not 32 or 64 bit ?");
        }

        File binDir = new File(JAVAHOME, "bin");
        JAVA_BIN = binDir.getAbsolutePath();
        File libDir = new File(JAVAHOME, "lib");
        JAVA_LIB = libDir.getAbsolutePath();

        File javaCmdFile = (isWindows)
                ? new File(binDir, "java.exe")
                : new File(binDir, "java");
        javaCmd = javaCmdFile.getAbsolutePath();
        if (!javaCmdFile.canExecute()) {
            throw new RuntimeException("java <" + TestHelper.javaCmd +
                    "> must exist and should be executable");
        }

        File javacCmdFile = (isWindows)
                ? new File(binDir, "javac.exe")
                : new File(binDir, "javac");
        javacCmd = javacCmdFile.getAbsolutePath();

        File jarCmdFile = (isWindows)
                ? new File(binDir, "jar.exe")
                : new File(binDir, "jar");
        jarCmd = jarCmdFile.getAbsolutePath();
        if (!jarCmdFile.canExecute()) {
            throw new RuntimeException("java <" + TestHelper.jarCmd +
                    "> must exist and should be executable");
        }

        if (isWindows) {
            File javawCmdFile = new File(binDir, "javaw.exe");
            javawCmd = javawCmdFile.getAbsolutePath();
            if (!javawCmdFile.canExecute()) {
                throw new RuntimeException("java <" + javawCmd +
                        "> must exist and should be executable");
            }
        } else {
            javawCmd = null;
        }

        if (!javacCmdFile.canExecute()) {
            throw new RuntimeException("java <" + javacCmd +
                    "> must exist and should be executable");
        }

        haveClientVM = haveVmVariant("client");
        haveServerVM = haveVmVariant("server");
    }
    private static boolean haveVmVariant(String type) {
        if (isWindows) {
            File vmDir = new File(JAVA_BIN, type);
            File jvmFile = new File(vmDir, LIBJVM);
            return jvmFile.exists();
        } else {
            File vmDir = new File(JAVA_LIB, type);
            File jvmFile = new File(vmDir, LIBJVM);
            return jvmFile.exists();
        }
    }
    void run(String[] args) throws Exception {
        int passed = 0, failed = 0;
        final Pattern p = (args != null && args.length > 0)
                ? Pattern.compile(args[0])
                : null;
        for (Method m : this.getClass().getDeclaredMethods()) {
            boolean selected = (p == null)
                    ? m.isAnnotationPresent(Test.class)
                    : p.matcher(m.getName()).matches();
            if (selected) {
                try {
                    m.invoke(this, (Object[]) null);
                    System.out.println(m.getName() + ": OK");
                    passed++;
                    System.out.printf("Passed: %d, Failed: %d, ExitValue: %d%n",
                                      passed, failed, testExitValue);
                } catch (Throwable ex) {
                    System.out.printf("Test %s failed: %s %n", m, ex);
                    System.out.println("----begin detailed exceptions----");
                    ex.printStackTrace(System.out);
                    for (Throwable t : ex.getSuppressed()) {
                        t.printStackTrace(System.out);
                    }
                    System.out.println("----end detailed exceptions----");
                    failed++;
                }
            }
        }
        System.out.printf("Total: Passed: %d, Failed %d%n", passed, failed);
        if (failed > 0) {
            throw new RuntimeException("Tests failed: " + failed);
        }
        if (passed == 0 && failed == 0) {
            throw new AssertionError("No test(s) selected: passed = " +
                    passed + ", failed = " + failed + " ??????????");
        }
    }

    /*
     * usually the jre/lib/arch-name is the same as os.arch, except for x86.
     */
    static String getJreArch() {
        String arch = System.getProperty("os.arch");
        return arch.equals("x86") ? "i386" : arch;
    }
    static String getArch() {
        return System.getProperty("os.arch");
    }
    static File getClassFile(File javaFile) {
        String s = javaFile.getAbsolutePath().replace(JAVA_FILE_EXT, CLASS_FILE_EXT);
        return new File(s);
    }

    static File getJavaFile(File classFile) {
        String s = classFile.getAbsolutePath().replace(CLASS_FILE_EXT, JAVA_FILE_EXT);
        return new File(s);
    }

    static String baseName(File f) {
        String s = f.getName();
        return s.substring(0, s.indexOf("."));
    }

    /*
     * A convenience method to create a jar with jar file name and defs
     */
    static void createJar(File jarName, String... mainDefs)
            throws FileNotFoundException{
        createJar(null, jarName, new File("Foo"), mainDefs);
    }

    /*
     * A convenience method to create a java file, compile and jar it up, using
     * the sole class file name in the jar, as the Main-Class attribute value.
     */
    static void createJar(File jarName, File mainClass, String... mainDefs)
            throws FileNotFoundException {
            createJar(null, jarName, mainClass, mainDefs);
    }

    /*
     * A convenience method to compile java files.
     */
    static void compile(String... compilerArgs) {
        if (compiler.run(System.out, System.err, compilerArgs) != 0) {
            String sarg = "";
            for (String x : compilerArgs) {
                sarg.concat(x + " ");
            }
            throw new Error("compilation failed: " + sarg);
        }
    }

    /*
     * A generic jar file creator to create a java file, compile it
     * and jar it up, a specific Main-Class entry name in the
     * manifest can be specified or a null to use the sole class file name
     * as the Main-Class attribute value.
     */
    static void createJar(String mEntry, File jarName, File mainClass,
            String... mainDefs) throws FileNotFoundException {
        if (jarName.exists()) {
            jarName.delete();
        }
        try (PrintStream ps = new PrintStream(new FileOutputStream(mainClass + ".java"))) {
            ps.println("public class Foo {");
            if (mainDefs != null) {
                for (String x : mainDefs) {
                    ps.println(x);
                }
            }
            ps.println("}");
        }

        String compileArgs[] = {
            mainClass + ".java"
        };
        if (compiler.run(System.out, System.err, compileArgs) != 0) {
            throw new RuntimeException("compilation failed " + mainClass + ".java");
        }
        if (mEntry == null) {
            mEntry = mainClass.getName();
        }
        String jarArgs[] = {
            (debug) ? "cvfe" : "cfe",
            jarName.getAbsolutePath(),
            mEntry,
            mainClass.getName() + ".class"
        };
        createJar(jarArgs);
    }

   static void createJar(String... args) {
        List<String> cmdList = new ArrayList<>();
        cmdList.add(jarCmd);
        cmdList.addAll(Arrays.asList(args));
        doExec(cmdList.toArray(new String[cmdList.size()]));
   }

   static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        int n = in.read(buf);
        while (n > 0) {
            out.write(buf, 0, n);
            n = in.read(buf);
        }
    }

   static void copyFile(File src, File dst) throws IOException {
        Path parent = dst.toPath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.copy(src.toPath(), dst.toPath(), COPY_ATTRIBUTES, REPLACE_EXISTING);
    }

    /**
     * Attempt to create a file at the given location. If an IOException
     * occurs then back off for a moment and try again. When a number of
     * attempts fail, give up and throw an exception.
     */
    void createAFile(File aFile, List<String> lines) throws IOException {
        createAFile(aFile, lines, true);
    }

    void createAFile(File aFile, List<String> lines, boolean endWithNewline) throws IOException {
        IOException cause = null;
        for (int attempts = 0; attempts < 10; attempts++) {
            try {
                if (endWithNewline) {
                    Files.write(aFile.getAbsoluteFile().toPath(),
                        lines, Charset.defaultCharset(),
                        CREATE, TRUNCATE_EXISTING, WRITE);
                } else {
                    Files.write(aFile.getAbsoluteFile().toPath(),
                        String.join(System.lineSeparator(), lines).getBytes(Charset.defaultCharset()),
                        CREATE, TRUNCATE_EXISTING, WRITE);
                }
                if (cause != null) {
                    /*
                     * report attempts and errors that were encountered
                     * for diagnostic purposes
                     */
                    System.err.println("Created batch file " +
                                        aFile + " in " + (attempts + 1) +
                                        " attempts");
                    System.err.println("Errors encountered: " + cause);
                    cause.printStackTrace();
                }
                return;
            } catch (IOException ioe) {
                if (cause != null) {
                    // chain the exceptions so they all get reported for diagnostics
                    cause.addSuppressed(ioe);
                } else {
                    cause = ioe;
                }
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException ie) {
                if (cause != null) {
                    // cause should alway be non-null here
                    ie.addSuppressed(cause);
                }
                throw new RuntimeException("Interrupted while creating batch file", ie);
            }
        }
        throw new RuntimeException("Unable to create batch file", cause);
    }

    static void createFile(File outFile, List<String> content) throws IOException {
        Files.write(outFile.getAbsoluteFile().toPath(), content,
                Charset.defaultCharset(), CREATE_NEW);
    }

    static void recursiveDelete(File target) throws IOException {
        if (!target.exists()) {
            return;
        }
        Files.walkFileTree(target.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                try {
                    Files.deleteIfExists(dir);
                } catch (IOException ex) {
                    System.out.println("Error: could not delete: " + dir.toString());
                    System.out.println(ex.getMessage());
                    return FileVisitResult.TERMINATE;
                }
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                try {
                    Files.deleteIfExists(file);
                } catch (IOException ex) {
                    System.out.println("Error: could not delete: " + file.toString());
                    System.out.println(ex.getMessage());
                    return FileVisitResult.TERMINATE;
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    static TestResult doExec(String...cmds) {
        return doExec(null, null, cmds);
    }

    static TestResult doExec(Map<String, String> envToSet, String...cmds) {
        return doExec(envToSet, null, cmds);
    }
    /*
     * A method which executes a java cmd and returns the results in a container
     */
    static TestResult doExec(Map<String, String> envToSet,
                             Set<String> envToRemove, String...cmds) {
        String cmdStr = "";
        for (String x : cmds) {
            cmdStr = cmdStr.concat(x + " ");
        }
        ProcessBuilder pb = new ProcessBuilder(cmds);
        Map<String, String> env = pb.environment();
        if (envToRemove != null) {
            for (String key : envToRemove) {
                env.remove(key);
            }
        }
        if (envToSet != null) {
            env.putAll(envToSet);
        }
        BufferedReader rdr = null;
        try {
            List<String> outputList = new ArrayList<>();
            pb.redirectErrorStream(true);
            Process p = pb.start();
            rdr = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String in = rdr.readLine();
            while (in != null) {
                outputList.add(in);
                in = rdr.readLine();
            }
            p.waitFor();
            p.destroy();

            return new TestHelper.TestResult(cmdStr, p.exitValue(), outputList,
                    env, new Throwable("current stack of the test"));
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex.getMessage());
        }
    }

    static FileFilter createFilter(final String extension) {
        return new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                String name = pathname.getName();
                if (name.endsWith(extension)) {
                    return true;
                }
                return false;
            }
        };
    }

    static boolean isEnglishLocale() {
        return Locale.getDefault().getLanguage().equals("en");
    }

    /**
     * Helper method to initialize a simple module that just prints the passed in arguments
     */
    static void createEchoArgumentsModule(File modulesDir) throws IOException {
        if (modulesDir.exists()) {
            recursiveDelete(modulesDir);
        }

        modulesDir.mkdirs();

        File srcDir = new File(modulesDir, "src");
        File modsDir = new File(modulesDir, "mods");
        File testDir = new File(srcDir, "test");
        File launcherTestDir = new File(testDir, "launcher");
        srcDir.mkdir();
        modsDir.mkdir();
        testDir.mkdir();
        launcherTestDir.mkdir();

        String[] moduleInfoCode = { "module test {}" };
        createFile(new File(testDir, "module-info.java"), Arrays.asList(moduleInfoCode));

        String[] moduleCode = {
            "package launcher;",
            "import java.util.Arrays;",
            "public class Main {",
            "public static void main(String[] args) {",
            "System.out.println(Arrays.toString(args));",
            "}",
            "}"
        };
        createFile(new File(launcherTestDir, "Main.java"), Arrays.asList(moduleCode));
    }

    static class ToolFilter implements FileFilter {
        final List<String> exclude = new ArrayList<>();
        protected ToolFilter(String... exclude) {
            for (String x : exclude) {
                String str = x + ((isWindows) ? EXE_FILE_EXT : "");
                this.exclude.add(str.toLowerCase());
            }
        }

        @Override
        public boolean accept(File pathname) {
            if (!pathname.isFile() || !pathname.canExecute()) {
                return false;
            }
            String name = pathname.getName().toLowerCase();
            if (isWindows) {
                if (!name.endsWith(EXE_FILE_EXT)) {
                    return false;
                }
            } else if (isMacOSX) {
                if (name.endsWith(MAC_DSYM_EXT)) {
                    return false;
                }
            } else {
                if (name.endsWith(NIX_DBGINFO_EXT)) {
                    return false;
                }
            }
            for (String x : exclude) {
                if (name.endsWith(x)) {
                    return false;
                }
            }
            return true;
        }
    }

    /*
     * A class to encapsulate the test results and stuff, with some ease
     * of use methods to check the test results.
     */
    static class TestResult {
        PrintWriter status;
        StringWriter sw;
        int exitValue;
        List<String> testOutput;
        Map<String, String> env;
        Throwable t;
        boolean testStatus;

        public TestResult(String str, int rv, List<String> oList,
                Map<String, String> env, Throwable t) {
            sw = new StringWriter();
            status = new PrintWriter(sw);
            status.println("Executed command: " + str + "\n");
            exitValue = rv;
            testOutput = oList;
            this.env = env;
            this.t = t;
            testStatus = true;
        }

        void appendError(String x) {
            testStatus = false;
            testExitValue++;
            status.println(TEST_PREFIX + x);
        }

        void indentStatus(String x) {
            status.println("  " + x);
        }

        void checkNegative() {
            if (exitValue == 0) {
                appendError("test must not return 0 exit value");
            }
        }

        void checkPositive() {
            if (exitValue != 0) {
                appendError("test did not return 0 exit value");
            }
        }

        boolean isOK() {
            return exitValue == 0;
        }

        boolean isZeroOutput() {
            if (!testOutput.isEmpty()) {
                appendError("No message from cmd please");
                return false;
            }
            return true;
        }

        boolean isNotZeroOutput() {
            if (testOutput.isEmpty()) {
                appendError("Missing message");
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            status.println("++++Begin Test Info++++");
            status.println("Test Status: " + (testStatus ? "PASS" : "FAIL"));
            status.println("++++Test Environment++++");
            for (String x : env.keySet()) {
                indentStatus(x + "=" + env.get(x));
            }
            status.println("++++Test Output++++");
            for (String x : testOutput) {
                indentStatus(x);
            }
            status.println("++++Test Stack Trace++++");
            status.println(t.toString());
            for (StackTraceElement e : t.getStackTrace()) {
                indentStatus(e.toString());
            }
            status.println("++++End of Test Info++++");
            status.flush();
            String out = sw.toString();
            status.close();
            return out;
        }

        boolean contains(String str) {
            for (String x : testOutput) {
                if (x.contains(str)) {
                    return true;
                }
            }
            appendError("string <" + str + "> not found");
            return false;
        }

        boolean notContains(String str) {
            for (String x : testOutput) {
                if (x.contains(str)) {
                    appendError("string <" + str + "> found");
                    return false;
                }
            }
            return true;
        }

        boolean matches(String regexToMatch) {
            for (String x : testOutput) {
                if (x.matches(regexToMatch)) {
                    return true;
                }
            }
            appendError("regex <" + regexToMatch + "> not matched");
            return false;
        }

        boolean notMatches(String regexToMatch) {
            for (String x : testOutput) {
                if (!x.matches(regexToMatch)) {
                    return true;
                }
            }
            appendError("regex <" + regexToMatch + "> matched");
            return false;
        }
    }
    /**
    * Indicates that the annotated method is a test method.
    */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Test {}
}
