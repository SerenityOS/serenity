/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 4199068 4738465 4937983 4930681 4926230 4931433 4932663 4986689
 *      5026830 5023243 5070673 4052517 4811767 6192449 6397034 6413313
 *      6464154 6523983 6206031 4960438 6631352 6631966 6850957 6850958
 *      4947220 7018606 7034570 4244896 5049299 8003488 8054494 8058464
 *      8067796 8224905 8263729 8265173
 * @key intermittent
 * @summary Basic tests for Process and Environment Variable code
 * @modules java.base/java.lang:open
 * @library /test/lib
 * @run main/othervm/timeout=300 -Djava.security.manager=allow Basic
 * @run main/othervm/timeout=300 -Djava.security.manager=allow -Djdk.lang.Process.launchMechanism=fork Basic
 * @author Martin Buchholz
 */

/*
 * @test
 * @modules java.base/java.lang:open
 * @requires (os.family == "linux")
 * @library /test/lib
 * @run main/othervm/timeout=300 -Djava.security.manager=allow -Djdk.lang.Process.launchMechanism=posix_spawn Basic
 */

import java.lang.ProcessBuilder.Redirect;
import java.lang.ProcessHandle;
import static java.lang.ProcessBuilder.Redirect.*;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.security.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import static java.lang.System.getenv;
import static java.lang.System.out;
import static java.lang.Boolean.TRUE;
import static java.util.AbstractMap.SimpleImmutableEntry;

import jdk.test.lib.Platform;

public class Basic {

    /* used for Windows only */
    static final String systemRoot = System.getenv("SystemRoot");

    /* used for Mac OS X only */
    static final String cfUserTextEncoding = System.getenv("__CF_USER_TEXT_ENCODING");

    /* used for AIX only */
    static final String libpath = System.getenv("LIBPATH");

    /* Used for regex String matching for long error messages */
    static final String PERMISSION_DENIED_ERROR_MSG = "(Permission denied|error=13)";
    static final String NO_SUCH_FILE_ERROR_MSG = "(No such file|error=2)";

    /**
     * Returns the number of milliseconds since time given by
     * startNanoTime, which must have been previously returned from a
     * call to {@link System.nanoTime()}.
     */
    private static long millisElapsedSince(long startNanoTime) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanoTime);
    }

    private static String commandOutput(Reader r) throws Throwable {
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = r.read()) > 0)
            if (c != '\r')
                sb.append((char) c);
        return sb.toString();
    }

    private static String commandOutput(Process p) throws Throwable {
        check(p.getInputStream()  == p.getInputStream());
        check(p.getOutputStream() == p.getOutputStream());
        check(p.getErrorStream()  == p.getErrorStream());
        Reader r = new InputStreamReader(p.getInputStream(),"UTF-8");
        String output = commandOutput(r);
        equal(p.waitFor(), 0);
        equal(p.exitValue(), 0);
        // The debug/fastdebug versions of the VM may write some warnings to stdout
        // (i.e. "Warning:  Cannot open log file: hotspot.log" if the VM is started
        // in a directory without write permissions). These warnings will confuse tests
        // which match the entire output of the child process so better filter them out.
        return output.replaceAll("Warning:.*\\n", "");
    }

    private static String commandOutput(ProcessBuilder pb) {
        try {
            return commandOutput(pb.start());
        } catch (Throwable t) {
            String commandline = "";
            for (String arg : pb.command())
                commandline += " " + arg;
            System.out.println("Exception trying to run process: " + commandline);
            unexpected(t);
            return "";
        }
    }

    private static String commandOutput(String...command) {
        try {
            return commandOutput(Runtime.getRuntime().exec(command));
        } catch (Throwable t) {
            String commandline = "";
            for (String arg : command)
                commandline += " " + arg;
            System.out.println("Exception trying to run process: " + commandline);
            unexpected(t);
            return "";
        }
    }

    private static void checkCommandOutput(ProcessBuilder pb,
                                           String expected,
                                           String failureMsg) {
        String got = commandOutput(pb);
        check(got.equals(expected),
              failureMsg + "\n" +
              "Expected: \"" + expected + "\"\n" +
              "Got: \"" + got + "\"");
    }

    private static String absolutifyPath(String path) {
        StringBuilder sb = new StringBuilder();
        for (String file : path.split(File.pathSeparator)) {
            if (sb.length() != 0)
                sb.append(File.pathSeparator);
            sb.append(new File(file).getAbsolutePath());
        }
        return sb.toString();
    }

    // compare windows-style, by canonicalizing to upper case,
    // not lower case as String.compareToIgnoreCase does
    private static class WindowsComparator
        implements Comparator<String> {
        public int compare(String x, String y) {
            return x.toUpperCase(Locale.US)
                .compareTo(y.toUpperCase(Locale.US));
        }
    }

    private static String sortedLines(String lines) {
        String[] arr = lines.split("\n");
        List<String> ls = new ArrayList<String>();
        for (String s : arr)
            ls.add(s);
        Collections.sort(ls, new WindowsComparator());
        StringBuilder sb = new StringBuilder();
        for (String s : ls)
            sb.append(s + "\n");
        return sb.toString();
    }

    private static void compareLinesIgnoreCase(String lines1, String lines2) {
        if (! (sortedLines(lines1).equalsIgnoreCase(sortedLines(lines2)))) {
            String dashes =
                "-----------------------------------------------------";
            out.println(dashes);
            out.print(sortedLines(lines1));
            out.println(dashes);
            out.print(sortedLines(lines2));
            out.println(dashes);
            out.println("sizes: " + sortedLines(lines1).length() +
                        " " + sortedLines(lines2).length());

            fail("Sorted string contents differ");
        }
    }

    private static final Runtime runtime = Runtime.getRuntime();

    private static final String[] winEnvCommand = {"cmd.exe", "/c", "set"};

    private static String winEnvFilter(String env) {
        return env.replaceAll("\r", "")
            .replaceAll("(?m)^(?:COMSPEC|PROMPT|PATHEXT)=.*\n","");
    }

    private static String unixEnvProg() {
        return new File("/usr/bin/env").canExecute() ? "/usr/bin/env"
            : "/bin/env";
    }

    private static String nativeEnv(String[] env) {
        try {
            if (Windows.is()) {
                return winEnvFilter
                    (commandOutput(runtime.exec(winEnvCommand, env)));
            } else {
                return commandOutput(runtime.exec(unixEnvProg(), env));
            }
        } catch (Throwable t) { throw new Error(t); }
    }

    private static String nativeEnv(ProcessBuilder pb) {
        try {
            if (Windows.is()) {
                pb.command(winEnvCommand);
                return winEnvFilter(commandOutput(pb));
            } else {
                pb.command(new String[]{unixEnvProg()});
                return commandOutput(pb);
            }
        } catch (Throwable t) { throw new Error(t); }
    }

    private static void checkSizes(Map<String,String> environ, int size) {
        try {
            equal(size, environ.size());
            equal(size, environ.entrySet().size());
            equal(size, environ.keySet().size());
            equal(size, environ.values().size());

            boolean isEmpty = (size == 0);
            equal(isEmpty, environ.isEmpty());
            equal(isEmpty, environ.entrySet().isEmpty());
            equal(isEmpty, environ.keySet().isEmpty());
            equal(isEmpty, environ.values().isEmpty());
        } catch (Throwable t) { unexpected(t); }
    }

    private interface EnvironmentFrobber {
        void doIt(Map<String,String> environ);
    }

    private static void testVariableDeleter(EnvironmentFrobber fooDeleter) {
        try {
            Map<String,String> environ = new ProcessBuilder().environment();
            environ.put("Foo", "BAAR");
            fooDeleter.doIt(environ);
            equal(environ.get("Foo"), null);
            equal(environ.remove("Foo"), null);
        } catch (Throwable t) { unexpected(t); }
    }

    private static void testVariableAdder(EnvironmentFrobber fooAdder) {
        try {
            Map<String,String> environ = new ProcessBuilder().environment();
            environ.remove("Foo");
            fooAdder.doIt(environ);
            equal(environ.get("Foo"), "Bahrein");
        } catch (Throwable t) { unexpected(t); }
    }

    private static void testVariableModifier(EnvironmentFrobber fooModifier) {
        try {
            Map<String,String> environ = new ProcessBuilder().environment();
            environ.put("Foo","OldValue");
            fooModifier.doIt(environ);
            equal(environ.get("Foo"), "NewValue");
        } catch (Throwable t) { unexpected(t); }
    }

    private static void printUTF8(String s) throws IOException {
        out.write(s.getBytes("UTF-8"));
    }

    private static String getenvAsString(Map<String,String> environment) {
        StringBuilder sb = new StringBuilder();
        environment = new TreeMap<>(environment);
        for (Map.Entry<String,String> e : environment.entrySet())
            // Ignore magic environment variables added by the launcher
            if (! e.getKey().equals("LD_LIBRARY_PATH"))
                sb.append(e.getKey())
                    .append('=')
                    .append(e.getValue())
                    .append(',');
        return sb.toString();
    }

    static void print4095(OutputStream s, byte b) throws Throwable {
        byte[] bytes = new byte[4095];
        Arrays.fill(bytes, b);
        s.write(bytes);         // Might hang!
    }

    static void checkPermissionDenied(ProcessBuilder pb) {
        try {
            pb.start();
            fail("Expected IOException not thrown");
        } catch (IOException e) {
            String m = e.getMessage();
            if (EnglishUnix.is() &&
                ! matches(m, PERMISSION_DENIED_ERROR_MSG))
                unexpected(e);
        } catch (Throwable t) { unexpected(t); }
    }

    public static class JavaChild {
        public static void main(String args[]) throws Throwable {
            String action = args[0];
            if (action.equals("sleep")) {
                Thread.sleep(10 * 60 * 1000L);
            } else if (action.equals("pid")) {
                System.out.println(ProcessHandle.current().pid());
            } else if (action.equals("testIO")) {
                String expected = "standard input";
                char[] buf = new char[expected.length()+1];
                int n = new InputStreamReader(System.in).read(buf,0,buf.length);
                if (n != expected.length())
                    System.exit(5);
                if (! new String(buf,0,n).equals(expected))
                    System.exit(5);
                System.err.print("standard error");
                System.out.print("standard output");
            } else if (action.equals("testInheritIO")
                    || action.equals("testRedirectInherit")) {
                List<String> childArgs = new ArrayList<String>(javaChildArgs);
                childArgs.add("testIO");
                ProcessBuilder pb = new ProcessBuilder(childArgs);
                if (action.equals("testInheritIO"))
                    pb.inheritIO();
                else
                    redirectIO(pb, INHERIT, INHERIT, INHERIT);
                ProcessResults r = run(pb);
                if (! r.out().equals(""))
                    System.exit(7);
                if (! r.err().equals(""))
                    System.exit(8);
                if (r.exitValue() != 0)
                    System.exit(9);
            } else if (action.equals("System.getenv(String)")) {
                String val = System.getenv(args[1]);
                printUTF8(val == null ? "null" : val);
            } else if (action.equals("System.getenv(\\u1234)")) {
                String val = System.getenv("\u1234");
                printUTF8(val == null ? "null" : val);
            } else if (action.equals("System.getenv()")) {
                printUTF8(getenvAsString(System.getenv()));
            } else if (action.equals("ArrayOOME")) {
                Object dummy;
                switch(new Random().nextInt(3)) {
                case 0: dummy = new Integer[Integer.MAX_VALUE]; break;
                case 1: dummy = new double[Integer.MAX_VALUE];  break;
                case 2: dummy = new byte[Integer.MAX_VALUE][];  break;
                default: throw new InternalError();
                }
            } else if (action.equals("pwd")) {
                printUTF8(new File(System.getProperty("user.dir"))
                          .getCanonicalPath());
            } else if (action.equals("print4095")) {
                print4095(System.out, (byte) '!');
                print4095(System.err, (byte) 'E');
                System.exit(5);
            } else if (action.equals("OutErr")) {
                // You might think the system streams would be
                // buffered, and in fact they are implemented using
                // BufferedOutputStream, but each and every print
                // causes immediate operating system I/O.
                System.out.print("out");
                System.err.print("err");
                System.out.print("out");
                System.err.print("err");
            } else if (action.equals("null PATH")) {
                equal(System.getenv("PATH"), null);
                check(new File("/bin/true").exists());
                check(new File("/bin/false").exists());
                ProcessBuilder pb1 = new ProcessBuilder();
                ProcessBuilder pb2 = new ProcessBuilder();
                pb2.environment().put("PATH", "anyOldPathIgnoredAnyways");
                ProcessResults r;

                for (final ProcessBuilder pb :
                         new ProcessBuilder[] {pb1, pb2}) {
                    pb.command("true");
                    equal(run(pb).exitValue(), True.exitValue());

                    pb.command("false");
                    equal(run(pb).exitValue(), False.exitValue());
                }

                if (failed != 0) throw new Error("null PATH");
            } else if (action.equals("PATH search algorithm")) {
                equal(System.getenv("PATH"), "dir1:dir2:");
                check(new File(TrueExe.path()).exists());
                check(new File(FalseExe.path()).exists());
                String[] cmd = {"prog"};
                ProcessBuilder pb1 = new ProcessBuilder(cmd);
                ProcessBuilder pb2 = new ProcessBuilder(cmd);
                ProcessBuilder pb3 = new ProcessBuilder(cmd);
                pb2.environment().put("PATH", "anyOldPathIgnoredAnyways");
                pb3.environment().remove("PATH");

                for (final ProcessBuilder pb :
                         new ProcessBuilder[] {pb1, pb2, pb3}) {
                    try {
                        // Not on PATH at all; directories don't exist
                        try {
                            pb.start();
                            fail("Expected IOException not thrown");
                        } catch (IOException e) {
                            String m = e.getMessage();
                            if (EnglishUnix.is() &&
                                ! matches(m, NO_SUCH_FILE_ERROR_MSG))
                                unexpected(e);
                        } catch (Throwable t) { unexpected(t); }

                        // Not on PATH at all; directories exist
                        new File("dir1").mkdirs();
                        new File("dir2").mkdirs();
                        try {
                            pb.start();
                            fail("Expected IOException not thrown");
                        } catch (IOException e) {
                            String m = e.getMessage();
                            if (EnglishUnix.is() &&
                                ! matches(m, NO_SUCH_FILE_ERROR_MSG))
                                unexpected(e);
                        } catch (Throwable t) { unexpected(t); }

                        // Can't execute a directory -- permission denied
                        // Report EACCES errno
                        new File("dir1/prog").mkdirs();
                        checkPermissionDenied(pb);

                        // continue searching if EACCES
                        copy(TrueExe.path(), "dir2/prog");
                        equal(run(pb).exitValue(), True.exitValue());
                        new File("dir1/prog").delete();
                        new File("dir2/prog").delete();

                        new File("dir2/prog").mkdirs();
                        copy(TrueExe.path(), "dir1/prog");
                        equal(run(pb).exitValue(), True.exitValue());

                        // Check empty PATH component means current directory.
                        //
                        // While we're here, let's test different kinds of
                        // Unix executables, and PATH vs explicit searching.
                        new File("dir1/prog").delete();
                        new File("dir2/prog").delete();
                        for (String[] command :
                                 new String[][] {
                                     new String[] {"./prog"},
                                     cmd}) {
                            pb.command(command);
                            File prog = new File("./prog");
                            // "Normal" binaries
                            copy(TrueExe.path(), "./prog");
                            equal(run(pb).exitValue(),
                                  True.exitValue());
                            copy(FalseExe.path(), "./prog");
                            equal(run(pb).exitValue(),
                                  False.exitValue());
                            prog.delete();
                            // Interpreter scripts with #!
                            setFileContents(prog, "#!/bin/true\n");
                            prog.setExecutable(true);
                            equal(run(pb).exitValue(),
                                  True.exitValue());
                            prog.delete();
                            setFileContents(prog, "#!/bin/false\n");
                            prog.setExecutable(true);
                            equal(run(pb).exitValue(),
                                  False.exitValue());
                            // Traditional shell scripts without #!
                            setFileContents(prog, "exec /bin/true\n");
                            prog.setExecutable(true);
                            equal(run(pb).exitValue(),
                                  True.exitValue());
                            prog.delete();
                            setFileContents(prog, "exec /bin/false\n");
                            prog.setExecutable(true);
                            equal(run(pb).exitValue(),
                                  False.exitValue());
                            prog.delete();
                        }

                        // Test Unix interpreter scripts
                        File dir1Prog = new File("dir1/prog");
                        dir1Prog.delete();
                        pb.command(new String[] {"prog", "world"});
                        setFileContents(dir1Prog, "#!/bin/echo hello\n");
                        checkPermissionDenied(pb);
                        dir1Prog.setExecutable(true);
                        equal(run(pb).out(), "hello dir1/prog world\n");
                        equal(run(pb).exitValue(), True.exitValue());
                        dir1Prog.delete();
                        pb.command(cmd);

                        // Test traditional shell scripts without #!
                        setFileContents(dir1Prog, "/bin/echo \"$@\"\n");
                        pb.command(new String[] {"prog", "hello", "world"});
                        checkPermissionDenied(pb);
                        dir1Prog.setExecutable(true);
                        equal(run(pb).out(), "hello world\n");
                        equal(run(pb).exitValue(), True.exitValue());
                        dir1Prog.delete();
                        pb.command(cmd);

                        // If prog found on both parent and child's PATH,
                        // parent's is used.
                        new File("dir1/prog").delete();
                        new File("dir2/prog").delete();
                        new File("prog").delete();
                        new File("dir3").mkdirs();
                        copy(TrueExe.path(), "dir1/prog");
                        copy(FalseExe.path(), "dir3/prog");
                        pb.environment().put("PATH","dir3");
                        equal(run(pb).exitValue(), True.exitValue());
                        copy(TrueExe.path(), "dir3/prog");
                        copy(FalseExe.path(), "dir1/prog");
                        equal(run(pb).exitValue(), False.exitValue());

                    } finally {
                        // cleanup
                        new File("dir1/prog").delete();
                        new File("dir2/prog").delete();
                        new File("dir3/prog").delete();
                        new File("dir1").delete();
                        new File("dir2").delete();
                        new File("dir3").delete();
                        new File("prog").delete();
                    }
                }

                if (failed != 0) throw new Error("PATH search algorithm");
            }
            else throw new Error("JavaChild invocation error");
        }
    }

    private static void copy(String src, String dst) throws IOException {
        Files.copy(Paths.get(src), Paths.get(dst),
                   StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
    }

    private static String javaChildOutput(ProcessBuilder pb, String...args) {
        List<String> list = new ArrayList<String>(javaChildArgs);
        for (String arg : args)
            list.add(arg);
        pb.command(list);
        return commandOutput(pb);
    }

    private static String getenvInChild(ProcessBuilder pb) {
        return javaChildOutput(pb, "System.getenv()");
    }

    private static String getenvInChild1234(ProcessBuilder pb) {
        return javaChildOutput(pb, "System.getenv(\\u1234)");
    }

    private static String getenvInChild(ProcessBuilder pb, String name) {
        return javaChildOutput(pb, "System.getenv(String)", name);
    }

    private static String pwdInChild(ProcessBuilder pb) {
        return javaChildOutput(pb, "pwd");
    }

    private static final String javaExe =
        System.getProperty("java.home") +
        File.separator + "bin" + File.separator + "java";

    private static final String classpath =
        System.getProperty("java.class.path");

    private static final List<String> javaChildArgs =
        Arrays.asList(javaExe,
                      "-XX:+DisplayVMOutputToStderr",
                      "-classpath", absolutifyPath(classpath),
                      "Basic$JavaChild");

    private static void testEncoding(String encoding, String tested) {
        try {
            // If round trip conversion works, should be able to set env vars
            // correctly in child.
            if (new String(tested.getBytes()).equals(tested)) {
                out.println("Testing " + encoding + " environment values");
                ProcessBuilder pb = new ProcessBuilder();
                pb.environment().put("ASCIINAME",tested);
                equal(getenvInChild(pb,"ASCIINAME"), tested);
            }
        } catch (Throwable t) { unexpected(t); }
    }

    static class Windows {
        public static boolean is() { return is; }
        private static final boolean is =
            System.getProperty("os.name").startsWith("Windows");
    }

    static class AIX {
        public static boolean is() { return is; }
        private static final boolean is =
            System.getProperty("os.name").equals("AIX");
    }

    static class Unix {
        public static boolean is() { return is; }
        private static final boolean is =
            (! Windows.is() &&
             new File("/bin/sh").exists() &&
             new File("/bin/true").exists() &&
             new File("/bin/false").exists());
    }

    static class UnicodeOS {
        public static boolean is() { return is; }
        private static final String osName = System.getProperty("os.name");
        private static final boolean is =
            // MacOS X would probably also qualify
            osName.startsWith("Windows")   &&
            ! osName.startsWith("Windows 9") &&
            ! osName.equals("Windows Me");
    }

    static class MacOSX {
        public static boolean is() { return is; }
        private static final String osName = System.getProperty("os.name");
        private static final boolean is = osName.contains("OS X");
    }

    static class True {
        public static int exitValue() { return 0; }
    }

    private static class False {
        public static int exitValue() { return exitValue; }
        private static final int exitValue = exitValue0();
        private static int exitValue0() {
            // /bin/false returns an *unspecified* non-zero number.
            try {
                if (! Unix.is())
                    return -1;
                else {
                    int rc = new ProcessBuilder("/bin/false")
                        .start().waitFor();
                    check(rc != 0);
                    return rc;
                }
            } catch (Throwable t) { unexpected(t); return -1; }
        }
    }

    // On Alpine Linux, /bin/true and /bin/false are just links to /bin/busybox.
    // Some tests copy /bin/true and /bin/false to files with a different filename.
    // However, copying the busbox executable into a file with a different name
    // won't result in the expected return codes. As workaround, we create
    // executable files that can be copied and produce the expected return
    // values.

    private static class TrueExe {
        public static String path() { return path; }
        private static final String path = path0();
        private static String path0(){
            if (!Platform.isBusybox("/bin/true")) {
                return "/bin/true";
            } else {
                File trueExe = new File("true");
                setFileContents(trueExe, "#!/bin/true\n");
                trueExe.setExecutable(true);
                return trueExe.getAbsolutePath();
            }
        }
    }

    private static class FalseExe {
        public static String path() { return path; }
        private static final String path = path0();
        private static String path0(){
            if (!Platform.isBusybox("/bin/false")) {
                return "/bin/false";
            } else {
                File falseExe = new File("false");
                setFileContents(falseExe, "#!/bin/false\n");
                falseExe.setExecutable(true);
                return falseExe.getAbsolutePath();
            }
        }
    }

    static class EnglishUnix {
        private static final Boolean is =
            (! Windows.is() && isEnglish("LANG") && isEnglish("LC_ALL"));

        private static boolean isEnglish(String envvar) {
            String val = getenv(envvar);
            return (val == null) || val.matches("en.*") || val.matches("C");
        }

        /** Returns true if we can expect English OS error strings */
        static boolean is() { return is; }
    }

    static class DelegatingProcess extends Process {
        final Process p;

        DelegatingProcess(Process p) {
            this.p = p;
        }

        @Override
        public void destroy() {
            p.destroy();
        }

        @Override
        public int exitValue() {
            return p.exitValue();
        }

        @Override
        public int waitFor() throws InterruptedException {
            return p.waitFor();
        }

        @Override
        public OutputStream getOutputStream() {
            return p.getOutputStream();
        }

        @Override
        public InputStream getInputStream() {
            return p.getInputStream();
        }

        @Override
        public InputStream getErrorStream() {
            return p.getErrorStream();
        }
    }

    private static boolean matches(String str, String regex) {
        return Pattern.compile(regex).matcher(str).find();
    }

    private static String matchAndExtract(String str, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(str);
        if (matcher.find()) {
            return matcher.group();
        } else {
            return "";
        }
    }

    /* Only used for Mac OS X --
     * Mac OS X (may) add the variable __CF_USER_TEXT_ENCODING to an empty
     * environment. The environment variable JAVA_MAIN_CLASS_<pid> may also
     * be set in Mac OS X.
     * Remove them both from the list of env variables
     */
    private static String removeMacExpectedVars(String vars) {
        // Check for __CF_USER_TEXT_ENCODING
        String cleanedVars = vars.replace("__CF_USER_TEXT_ENCODING="
                                            +cfUserTextEncoding+",","");
        // Check for JAVA_MAIN_CLASS_<pid>
        String javaMainClassStr
                = matchAndExtract(cleanedVars,
                                    "JAVA_MAIN_CLASS_\\d+=Basic.JavaChild,");
        return cleanedVars.replace(javaMainClassStr,"");
    }

    /* Only used for AIX --
     * AIX adds the variable AIXTHREAD_GUARDPAGES=0 to the environment.
     * Remove it from the list of env variables
     */
    private static String removeAixExpectedVars(String vars) {
        return vars.replace("AIXTHREAD_GUARDPAGES=0,", "");
    }

    private static String sortByLinesWindowsly(String text) {
        String[] lines = text.split("\n");
        Arrays.sort(lines, new WindowsComparator());
        StringBuilder sb = new StringBuilder();
        for (String line : lines)
            sb.append(line).append("\n");
        return sb.toString();
    }

    private static void checkMapSanity(Map<String,String> map) {
        try {
            Set<String> keySet = map.keySet();
            Collection<String> values = map.values();
            Set<Map.Entry<String,String>> entrySet = map.entrySet();

            equal(entrySet.size(), keySet.size());
            equal(entrySet.size(), values.size());

            StringBuilder s1 = new StringBuilder();
            for (Map.Entry<String,String> e : entrySet)
                s1.append(e.getKey() + "=" + e.getValue() + "\n");

            StringBuilder s2 = new StringBuilder();
            for (String var : keySet)
                s2.append(var + "=" + map.get(var) + "\n");

            equal(s1.toString(), s2.toString());

            Iterator<String> kIter = keySet.iterator();
            Iterator<String> vIter = values.iterator();
            Iterator<Map.Entry<String,String>> eIter = entrySet.iterator();

            while (eIter.hasNext()) {
                Map.Entry<String,String> entry = eIter.next();
                String key   = kIter.next();
                String value = vIter.next();
                check(entrySet.contains(entry));
                check(keySet.contains(key));
                check(values.contains(value));
                check(map.containsKey(key));
                check(map.containsValue(value));
                equal(entry.getKey(), key);
                equal(entry.getValue(), value);
            }
            check(!kIter.hasNext() &&
                    !vIter.hasNext());

        } catch (Throwable t) { unexpected(t); }
    }

    private static void checkMapEquality(Map<String,String> map1,
                                         Map<String,String> map2) {
        try {
            equal(map1.size(), map2.size());
            equal(map1.isEmpty(), map2.isEmpty());
            for (String key : map1.keySet()) {
                equal(map1.get(key), map2.get(key));
                check(map2.keySet().contains(key));
            }
            equal(map1, map2);
            equal(map2, map1);
            equal(map1.entrySet(), map2.entrySet());
            equal(map2.entrySet(), map1.entrySet());
            equal(map1.keySet(), map2.keySet());
            equal(map2.keySet(), map1.keySet());

            equal(map1.hashCode(), map2.hashCode());
            equal(map1.entrySet().hashCode(), map2.entrySet().hashCode());
            equal(map1.keySet().hashCode(), map2.keySet().hashCode());
        } catch (Throwable t) { unexpected(t); }
    }

    static void checkRedirects(ProcessBuilder pb,
                               Redirect in, Redirect out, Redirect err) {
        equal(pb.redirectInput(), in);
        equal(pb.redirectOutput(), out);
        equal(pb.redirectError(), err);
    }

    static void redirectIO(ProcessBuilder pb,
                           Redirect in, Redirect out, Redirect err) {
        pb.redirectInput(in);
        pb.redirectOutput(out);
        pb.redirectError(err);
    }

    static void setFileContents(File file, String contents) {
        try {
            Writer w = new FileWriter(file);
            w.write(contents);
            w.close();
        } catch (Throwable t) { unexpected(t); }
    }

    static String fileContents(File file) {
        try {
            Reader r = new FileReader(file);
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[1024];
            int n;
            while ((n = r.read(buffer)) != -1)
                sb.append(buffer,0,n);
            r.close();
            return new String(sb);
        } catch (Throwable t) { unexpected(t); return ""; }
    }

    @SuppressWarnings("removal")
    static void testIORedirection() throws Throwable {
        final File ifile = new File("ifile");
        final File ofile = new File("ofile");
        final File efile = new File("efile");
        ifile.delete();
        ofile.delete();
        efile.delete();

        //----------------------------------------------------------------
        // Check mutual inequality of different types of Redirect
        //----------------------------------------------------------------
        Redirect[] redirects =
            { PIPE,
              INHERIT,
              DISCARD,
              Redirect.from(ifile),
              Redirect.to(ifile),
              Redirect.appendTo(ifile),
              Redirect.from(ofile),
              Redirect.to(ofile),
              Redirect.appendTo(ofile),
            };
        for (int i = 0; i < redirects.length; i++)
            for (int j = 0; j < redirects.length; j++)
                equal(redirects[i].equals(redirects[j]), (i == j));

        //----------------------------------------------------------------
        // Check basic properties of different types of Redirect
        //----------------------------------------------------------------
        equal(PIPE.type(), Redirect.Type.PIPE);
        equal(PIPE.toString(), "PIPE");
        equal(PIPE.file(), null);

        equal(INHERIT.type(), Redirect.Type.INHERIT);
        equal(INHERIT.toString(), "INHERIT");
        equal(INHERIT.file(), null);

        equal(DISCARD.type(), Redirect.Type.WRITE);
        equal(DISCARD.toString(), "WRITE");
        equal(DISCARD.file(), new File((Windows.is() ? "NUL" : "/dev/null")));

        equal(Redirect.from(ifile).type(), Redirect.Type.READ);
        equal(Redirect.from(ifile).toString(),
              "redirect to read from file \"ifile\"");
        equal(Redirect.from(ifile).file(), ifile);
        equal(Redirect.from(ifile),
              Redirect.from(ifile));
        equal(Redirect.from(ifile).hashCode(),
              Redirect.from(ifile).hashCode());

        equal(Redirect.to(ofile).type(), Redirect.Type.WRITE);
        equal(Redirect.to(ofile).toString(),
              "redirect to write to file \"ofile\"");
        equal(Redirect.to(ofile).file(), ofile);
        equal(Redirect.to(ofile),
              Redirect.to(ofile));
        equal(Redirect.to(ofile).hashCode(),
              Redirect.to(ofile).hashCode());

        equal(Redirect.appendTo(ofile).type(), Redirect.Type.APPEND);
        equal(Redirect.appendTo(efile).toString(),
              "redirect to append to file \"efile\"");
        equal(Redirect.appendTo(efile).file(), efile);
        equal(Redirect.appendTo(efile),
              Redirect.appendTo(efile));
        equal(Redirect.appendTo(efile).hashCode(),
              Redirect.appendTo(efile).hashCode());

        //----------------------------------------------------------------
        // Check initial values of redirects
        //----------------------------------------------------------------
        List<String> childArgs = new ArrayList<String>(javaChildArgs);
        childArgs.add("testIO");
        final ProcessBuilder pb = new ProcessBuilder(childArgs);
        checkRedirects(pb, PIPE, PIPE, PIPE);

        //----------------------------------------------------------------
        // Check inheritIO
        //----------------------------------------------------------------
        pb.inheritIO();
        checkRedirects(pb, INHERIT, INHERIT, INHERIT);

        //----------------------------------------------------------------
        // Check DISCARD for stdout,stderr
        //----------------------------------------------------------------
        redirectIO(pb, INHERIT, DISCARD, DISCARD);
        checkRedirects(pb, INHERIT, DISCARD, DISCARD);

        //----------------------------------------------------------------
        // Check setters and getters agree
        //----------------------------------------------------------------
        pb.redirectInput(ifile);
        equal(pb.redirectInput().file(), ifile);
        equal(pb.redirectInput(), Redirect.from(ifile));

        pb.redirectOutput(ofile);
        equal(pb.redirectOutput().file(), ofile);
        equal(pb.redirectOutput(), Redirect.to(ofile));

        pb.redirectError(efile);
        equal(pb.redirectError().file(), efile);
        equal(pb.redirectError(), Redirect.to(efile));

        THROWS(IllegalArgumentException.class,
               () -> pb.redirectInput(Redirect.to(ofile)),
               () -> pb.redirectOutput(Redirect.from(ifile)),
               () -> pb.redirectError(Redirect.from(ifile)),
               () -> pb.redirectInput(DISCARD));

        THROWS(NullPointerException.class,
                () -> pb.redirectInput((File)null),
                () -> pb.redirectOutput((File)null),
                () -> pb.redirectError((File)null),
                () -> pb.redirectInput((Redirect)null),
                () -> pb.redirectOutput((Redirect)null),
                () -> pb.redirectError((Redirect)null));

        THROWS(IOException.class,
               // Input file does not exist
               () -> pb.start());
        setFileContents(ifile, "standard input");

        //----------------------------------------------------------------
        // Writing to non-existent files
        //----------------------------------------------------------------
        {
            ProcessResults r = run(pb);
            equal(r.exitValue(), 0);
            equal(fileContents(ofile), "standard output");
            equal(fileContents(efile), "standard error");
            equal(r.out(), "");
            equal(r.err(), "");
            ofile.delete();
            efile.delete();
        }

        //----------------------------------------------------------------
        // Both redirectErrorStream + redirectError
        //----------------------------------------------------------------
        {
            pb.redirectErrorStream(true);
            ProcessResults r = run(pb);
            equal(r.exitValue(), 0);
            equal(fileContents(ofile),
                    "standard error" + "standard output");
            equal(fileContents(efile), "");
            equal(r.out(), "");
            equal(r.err(), "");
            ofile.delete();
            efile.delete();
        }

        //----------------------------------------------------------------
        // Appending to existing files
        //----------------------------------------------------------------
        {
            setFileContents(ofile, "ofile-contents");
            setFileContents(efile, "efile-contents");
            pb.redirectOutput(Redirect.appendTo(ofile));
            pb.redirectError(Redirect.appendTo(efile));
            pb.redirectErrorStream(false);
            ProcessResults r = run(pb);
            equal(r.exitValue(), 0);
            equal(fileContents(ofile),
                  "ofile-contents" + "standard output");
            equal(fileContents(efile),
                  "efile-contents" + "standard error");
            equal(r.out(), "");
            equal(r.err(), "");
            ofile.delete();
            efile.delete();
        }

        //----------------------------------------------------------------
        // Replacing existing files
        //----------------------------------------------------------------
        {
            setFileContents(ofile, "ofile-contents");
            setFileContents(efile, "efile-contents");
            pb.redirectOutput(ofile);
            pb.redirectError(Redirect.to(efile));
            ProcessResults r = run(pb);
            equal(r.exitValue(), 0);
            equal(fileContents(ofile), "standard output");
            equal(fileContents(efile), "standard error");
            equal(r.out(), "");
            equal(r.err(), "");
            ofile.delete();
            efile.delete();
        }

        //----------------------------------------------------------------
        // Appending twice to the same file?
        //----------------------------------------------------------------
        {
            setFileContents(ofile, "ofile-contents");
            setFileContents(efile, "efile-contents");
            Redirect appender = Redirect.appendTo(ofile);
            pb.redirectOutput(appender);
            pb.redirectError(appender);
            ProcessResults r = run(pb);
            equal(r.exitValue(), 0);
            equal(fileContents(ofile),
                  "ofile-contents" +
                  "standard error" +
                  "standard output");
            equal(fileContents(efile), "efile-contents");
            equal(r.out(), "");
            equal(r.err(), "");
            ifile.delete();
            ofile.delete();
            efile.delete();
        }

        //----------------------------------------------------------------
        // DISCARDing output
        //----------------------------------------------------------------
        {
            setFileContents(ifile, "standard input");
            pb.redirectOutput(DISCARD);
            pb.redirectError(DISCARD);
            ProcessResults r = run(pb);
            equal(r.exitValue(), 0);
            equal(r.out(), "");
            equal(r.err(), "");
        }

        //----------------------------------------------------------------
        // DISCARDing output and redirecting error
        //----------------------------------------------------------------
        {
            setFileContents(ifile, "standard input");
            setFileContents(ofile, "ofile-contents");
            setFileContents(efile, "efile-contents");
            pb.redirectOutput(DISCARD);
            pb.redirectError(efile);
            ProcessResults r = run(pb);
            equal(r.exitValue(), 0);
            equal(fileContents(ofile), "ofile-contents");
            equal(fileContents(efile), "standard error");
            equal(r.out(), "");
            equal(r.err(), "");
            ofile.delete();
            efile.delete();
        }

        //----------------------------------------------------------------
        // DISCARDing error and redirecting output
        //----------------------------------------------------------------
        {
            setFileContents(ifile, "standard input");
            setFileContents(ofile, "ofile-contents");
            setFileContents(efile, "efile-contents");
            pb.redirectOutput(ofile);
            pb.redirectError(DISCARD);
            ProcessResults r = run(pb);
            equal(r.exitValue(), 0);
            equal(fileContents(ofile), "standard output");
            equal(fileContents(efile), "efile-contents");
            equal(r.out(), "");
            equal(r.err(), "");
            ofile.delete();
            efile.delete();
        }

        //----------------------------------------------------------------
        // DISCARDing output and merging error into output
        //----------------------------------------------------------------
        {
            setFileContents(ifile, "standard input");
            setFileContents(ofile, "ofile-contents");
            setFileContents(efile, "efile-contents");
            pb.redirectOutput(DISCARD);
            pb.redirectErrorStream(true);
            pb.redirectError(efile);
            ProcessResults r = run(pb);
            equal(r.exitValue(), 0);
            equal(fileContents(ofile), "ofile-contents");   // untouched
            equal(fileContents(efile), "");                 // empty
            equal(r.out(), "");
            equal(r.err(), "");
            ifile.delete();
            ofile.delete();
            efile.delete();
            pb.redirectErrorStream(false);                  // reset for next test
        }

        //----------------------------------------------------------------
        // Testing INHERIT is harder.
        // Note that this requires __FOUR__ nested JVMs involved in one test,
        // if you count the harness JVM.
        //----------------------------------------------------------------
        for (String testName : new String[] { "testInheritIO", "testRedirectInherit" } ) {
            redirectIO(pb, PIPE, PIPE, PIPE);
            List<String> command = pb.command();
            command.set(command.size() - 1, testName);
            Process p = pb.start();
            new PrintStream(p.getOutputStream()).print("standard input");
            p.getOutputStream().close();
            ProcessResults r = run(p);
            equal(r.exitValue(), 0);
            equal(r.out(), "standard output");
            equal(r.err(), "standard error");
        }

        //----------------------------------------------------------------
        // Test security implications of I/O redirection
        //----------------------------------------------------------------

        // Read access to current directory is always granted;
        // So create a tmpfile for input instead.
        final File tmpFile = File.createTempFile("Basic", "tmp");
        setFileContents(tmpFile, "standard input");

        final Policy policy = new Policy();
        Policy.setPolicy(policy);
        System.setSecurityManager(new SecurityManager());
        try {
            final Permission xPermission
                = new FilePermission("<<ALL FILES>>", "execute");
            final Permission rxPermission
                = new FilePermission("<<ALL FILES>>", "read,execute");
            final Permission wxPermission
                = new FilePermission("<<ALL FILES>>", "write,execute");
            final Permission rwxPermission
                = new FilePermission("<<ALL FILES>>", "read,write,execute");

            THROWS(SecurityException.class,
                   () -> { policy.setPermissions(xPermission);
                           redirectIO(pb, from(tmpFile), PIPE, PIPE);
                           pb.start();},
                   () -> { policy.setPermissions(rxPermission);
                           redirectIO(pb, PIPE, to(ofile), PIPE);
                           pb.start();},
                   () -> { policy.setPermissions(rxPermission);
                           redirectIO(pb, PIPE, PIPE, to(efile));
                           pb.start();});

            {
                policy.setPermissions(rxPermission);
                redirectIO(pb, from(tmpFile), PIPE, PIPE);
                ProcessResults r = run(pb);
                equal(r.out(), "standard output");
                equal(r.err(), "standard error");
            }

            {
                policy.setPermissions(wxPermission);
                redirectIO(pb, PIPE, to(ofile), to(efile));
                Process p = pb.start();
                new PrintStream(p.getOutputStream()).print("standard input");
                p.getOutputStream().close();
                ProcessResults r = run(p);
                policy.setPermissions(rwxPermission);
                equal(fileContents(ofile), "standard output");
                equal(fileContents(efile), "standard error");
            }

            {
                policy.setPermissions(rwxPermission);
                redirectIO(pb, from(tmpFile), to(ofile), to(efile));
                ProcessResults r = run(pb);
                policy.setPermissions(rwxPermission);
                equal(fileContents(ofile), "standard output");
                equal(fileContents(efile), "standard error");
            }

        } finally {
            policy.setPermissions(new RuntimePermission("setSecurityManager"));
            System.setSecurityManager(null);
            tmpFile.delete();
            ifile.delete();
            ofile.delete();
            efile.delete();
        }
    }

    static void checkProcessPid() {
        ProcessBuilder pb = new ProcessBuilder();
        List<String> list = new ArrayList<String>(javaChildArgs);
        list.add("pid");
        pb.command(list);
        try {
            Process p = pb.start();
            String s = commandOutput(p);
            long actualPid = Long.valueOf(s.trim());
            long expectedPid = p.pid();
            equal(actualPid, expectedPid);
        } catch (Throwable t) {
            unexpected(t);
        }


        // Test the default implementation of Process.getPid
        DelegatingProcess p = new DelegatingProcess(null);
        THROWS(UnsupportedOperationException.class,
                () -> p.pid(),
                () -> p.toHandle(),
                () -> p.supportsNormalTermination(),
                () -> p.children(),
                () -> p.descendants());

    }

    @SuppressWarnings("removal")
    private static void realMain(String[] args) throws Throwable {
        if (Windows.is())
            System.out.println("This appears to be a Windows system.");
        if (Unix.is())
            System.out.println("This appears to be a Unix system.");
        if (UnicodeOS.is())
            System.out.println("This appears to be a Unicode-based OS.");

        try { testIORedirection(); }
        catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // Basic tests for getPid()
        //----------------------------------------------------------------
        checkProcessPid();

        //----------------------------------------------------------------
        // Basic tests for setting, replacing and deleting envvars
        //----------------------------------------------------------------
        try {
            ProcessBuilder pb = new ProcessBuilder();
            Map<String,String> environ = pb.environment();

            // New env var
            environ.put("QUUX", "BAR");
            equal(environ.get("QUUX"), "BAR");
            equal(getenvInChild(pb,"QUUX"), "BAR");

            // Modify env var
            environ.put("QUUX","bear");
            equal(environ.get("QUUX"), "bear");
            equal(getenvInChild(pb,"QUUX"), "bear");
            checkMapSanity(environ);

            // Remove env var
            environ.remove("QUUX");
            equal(environ.get("QUUX"), null);
            equal(getenvInChild(pb,"QUUX"), "null");
            checkMapSanity(environ);

            // Remove non-existent env var
            environ.remove("QUUX");
            equal(environ.get("QUUX"), null);
            equal(getenvInChild(pb,"QUUX"), "null");
            checkMapSanity(environ);
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // Pass Empty environment to child
        //----------------------------------------------------------------
        try {
            ProcessBuilder pb = new ProcessBuilder();
            pb.environment().clear();
            String expected = Windows.is() ? "SystemRoot="+systemRoot+",": "";
            expected = AIX.is() ? "LIBPATH="+libpath+",": expected;
            if (Windows.is()) {
                pb.environment().put("SystemRoot", systemRoot);
            }
            if (AIX.is()) {
                pb.environment().put("LIBPATH", libpath);
            }
            String result = getenvInChild(pb);
            if (MacOSX.is()) {
                result = removeMacExpectedVars(result);
            }
            if (AIX.is()) {
                result = removeAixExpectedVars(result);
            }
            equal(result, expected);
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // System.getenv() is read-only.
        //----------------------------------------------------------------
        THROWS(UnsupportedOperationException.class,
               () -> getenv().put("FOO","BAR"),
               () -> getenv().remove("PATH"),
               () -> getenv().keySet().remove("PATH"),
               () -> getenv().values().remove("someValue"));

        try {
            Collection<Map.Entry<String,String>> c = getenv().entrySet();
            if (! c.isEmpty())
                try {
                    c.iterator().next().setValue("foo");
                    fail("Expected UnsupportedOperationException not thrown");
                } catch (UnsupportedOperationException e) {} // OK
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // System.getenv() always returns the same object in our implementation.
        //----------------------------------------------------------------
        try {
            check(System.getenv() == System.getenv());
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // You can't create an env var name containing "=",
        // or an env var name or value containing NUL.
        //----------------------------------------------------------------
        {
            final Map<String,String> m = new ProcessBuilder().environment();
            THROWS(IllegalArgumentException.class,
                   () -> m.put("FOO=","BAR"),
                   () -> m.put("FOO\u0000","BAR"),
                   () -> m.put("FOO","BAR\u0000"));
        }

        //----------------------------------------------------------------
        // Commands must never be null.
        //----------------------------------------------------------------
        THROWS(NullPointerException.class,
               () -> new ProcessBuilder((List<String>)null),
               () -> new ProcessBuilder().command((List<String>)null));

        //----------------------------------------------------------------
        // Put in a command; get the same one back out.
        //----------------------------------------------------------------
        try {
            List<String> command = new ArrayList<String>();
            ProcessBuilder pb = new ProcessBuilder(command);
            check(pb.command() == command);
            List<String> command2 = new ArrayList<String>(2);
            command2.add("foo");
            command2.add("bar");
            pb.command(command2);
            check(pb.command() == command2);
            pb.command("foo", "bar");
            check(pb.command() != command2 && pb.command().equals(command2));
            pb.command(command2);
            command2.add("baz");
            equal(pb.command().get(2), "baz");
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // Commands must contain at least one element.
        //----------------------------------------------------------------
        THROWS(IndexOutOfBoundsException.class,
               () -> new ProcessBuilder().start(),
               () -> new ProcessBuilder(new ArrayList<String>()).start(),
               () -> Runtime.getRuntime().exec(new String[]{}));

        //----------------------------------------------------------------
        // Commands must not contain null elements at start() time.
        //----------------------------------------------------------------
        THROWS(NullPointerException.class,
               () -> new ProcessBuilder("foo",null,"bar").start(),
               () -> new ProcessBuilder((String)null).start(),
               () -> new ProcessBuilder(new String[]{null}).start(),
               () -> new ProcessBuilder(new String[]{"foo",null,"bar"}).start());

        //----------------------------------------------------------------
        // Command lists are growable.
        //----------------------------------------------------------------
        try {
            new ProcessBuilder().command().add("foo");
            new ProcessBuilder("bar").command().add("foo");
            new ProcessBuilder(new String[]{"1","2"}).command().add("3");
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // Nulls in environment updates generate NullPointerException
        //----------------------------------------------------------------
        try {
            final Map<String,String> env = new ProcessBuilder().environment();
            THROWS(NullPointerException.class,
                   () -> env.put("foo",null),
                   () -> env.put(null,"foo"),
                   () -> env.remove(null),
                   () -> { for (Map.Entry<String,String> e : env.entrySet())
                               e.setValue(null);},
                   () -> Runtime.getRuntime().exec(new String[]{"foo"},
                                                   new String[]{null}));
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // Non-String types in environment updates generate ClassCastException
        //----------------------------------------------------------------
        try {
            final Map<String,String> env = new ProcessBuilder().environment();
            THROWS(ClassCastException.class,
                   () -> env.remove(TRUE),
                   () -> env.keySet().remove(TRUE),
                   () -> env.values().remove(TRUE),
                   () -> env.entrySet().remove(TRUE));
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // Check query operations on environment maps
        //----------------------------------------------------------------
        try {
            List<Map<String,String>> envs =
                new ArrayList<Map<String,String>>(2);
            envs.add(System.getenv());
            envs.add(new ProcessBuilder().environment());
            for (final Map<String,String> env : envs) {
                //----------------------------------------------------------------
                // Nulls in environment queries are forbidden.
                //----------------------------------------------------------------
                THROWS(NullPointerException.class,
                       () -> getenv(null),
                       () -> env.get(null),
                       () -> env.containsKey(null),
                       () -> env.containsValue(null),
                       () -> env.keySet().contains(null),
                       () -> env.values().contains(null));

                //----------------------------------------------------------------
                // Non-String types in environment queries are forbidden.
                //----------------------------------------------------------------
                THROWS(ClassCastException.class,
                       () -> env.get(TRUE),
                       () -> env.containsKey(TRUE),
                       () -> env.containsValue(TRUE),
                       () -> env.keySet().contains(TRUE),
                       () -> env.values().contains(TRUE));

                //----------------------------------------------------------------
                // Illegal String values in environment queries are (grumble) OK
                //----------------------------------------------------------------
                equal(env.get("\u0000"), null);
                check(! env.containsKey("\u0000"));
                check(! env.containsValue("\u0000"));
                check(! env.keySet().contains("\u0000"));
                check(! env.values().contains("\u0000"));
            }

        } catch (Throwable t) { unexpected(t); }

        try {
            final Set<Map.Entry<String,String>> entrySet =
                new ProcessBuilder().environment().entrySet();
            THROWS(NullPointerException.class,
                   () -> entrySet.contains(null));
            THROWS(ClassCastException.class,
                   () -> entrySet.contains(TRUE),
                   () -> entrySet.contains(
                             new SimpleImmutableEntry<Boolean,String>(TRUE,"")));

            check(! entrySet.contains
                  (new SimpleImmutableEntry<String,String>("", "")));
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // Put in a directory; get the same one back out.
        //----------------------------------------------------------------
        try {
            ProcessBuilder pb = new ProcessBuilder();
            File foo = new File("foo");
            equal(pb.directory(), null);
            equal(pb.directory(foo).directory(), foo);
            equal(pb.directory(null).directory(), null);
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // If round-trip conversion works, check envvar pass-through to child
        //----------------------------------------------------------------
        try {
            testEncoding("ASCII",   "xyzzy");
            testEncoding("Latin1",  "\u00f1\u00e1");
            testEncoding("Unicode", "\u22f1\u11e1");
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // A surprisingly large number of ways to delete an environment var.
        //----------------------------------------------------------------
        testVariableDeleter(new EnvironmentFrobber() {
                public void doIt(Map<String,String> environ) {
                    environ.remove("Foo");}});

        testVariableDeleter(new EnvironmentFrobber() {
                public void doIt(Map<String,String> environ) {
                    environ.keySet().remove("Foo");}});

        testVariableDeleter(new EnvironmentFrobber() {
                public void doIt(Map<String,String> environ) {
                    environ.values().remove("BAAR");}});

        testVariableDeleter(new EnvironmentFrobber() {
                public void doIt(Map<String,String> environ) {
                    // Legally fabricate a ProcessEnvironment.StringEntry,
                    // even though it's private.
                    Map<String,String> environ2
                        = new ProcessBuilder().environment();
                    environ2.clear();
                    environ2.put("Foo","BAAR");
                    // Subtlety alert.
                    Map.Entry<String,String> e
                        = environ2.entrySet().iterator().next();
                    environ.entrySet().remove(e);}});

        testVariableDeleter(new EnvironmentFrobber() {
                public void doIt(Map<String,String> environ) {
                    Map.Entry<String,String> victim = null;
                    for (Map.Entry<String,String> e : environ.entrySet())
                        if (e.getKey().equals("Foo"))
                            victim = e;
                    if (victim != null)
                        environ.entrySet().remove(victim);}});

        testVariableDeleter(new EnvironmentFrobber() {
                public void doIt(Map<String,String> environ) {
                    Iterator<String> it = environ.keySet().iterator();
                    while (it.hasNext()) {
                        String val = it.next();
                        if (val.equals("Foo"))
                            it.remove();}}});

        testVariableDeleter(new EnvironmentFrobber() {
                public void doIt(Map<String,String> environ) {
                    Iterator<Map.Entry<String,String>> it
                        = environ.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<String,String> e = it.next();
                        if (e.getKey().equals("Foo"))
                            it.remove();}}});

        testVariableDeleter(new EnvironmentFrobber() {
                public void doIt(Map<String,String> environ) {
                    Iterator<String> it = environ.values().iterator();
                    while (it.hasNext()) {
                        String val = it.next();
                        if (val.equals("BAAR"))
                            it.remove();}}});

        //----------------------------------------------------------------
        // A surprisingly small number of ways to add an environment var.
        //----------------------------------------------------------------
        testVariableAdder(new EnvironmentFrobber() {
                public void doIt(Map<String,String> environ) {
                    environ.put("Foo","Bahrein");}});

        //----------------------------------------------------------------
        // A few ways to modify an environment var.
        //----------------------------------------------------------------
        testVariableModifier(new EnvironmentFrobber() {
                public void doIt(Map<String,String> environ) {
                    environ.put("Foo","NewValue");}});

        testVariableModifier(new EnvironmentFrobber() {
                public void doIt(Map<String,String> environ) {
                    for (Map.Entry<String,String> e : environ.entrySet())
                        if (e.getKey().equals("Foo"))
                            e.setValue("NewValue");}});

        //----------------------------------------------------------------
        // Fiddle with environment sizes
        //----------------------------------------------------------------
        try {
            Map<String,String> environ = new ProcessBuilder().environment();
            int size = environ.size();
            checkSizes(environ, size);

            environ.put("UnLiKeLYeNVIROmtNam", "someVal");
            checkSizes(environ, size+1);

            // Check for environment independence
            new ProcessBuilder().environment().clear();

            environ.put("UnLiKeLYeNVIROmtNam", "someOtherVal");
            checkSizes(environ, size+1);

            environ.remove("UnLiKeLYeNVIROmtNam");
            checkSizes(environ, size);

            environ.clear();
            checkSizes(environ, 0);

            environ.clear();
            checkSizes(environ, 0);

            environ = new ProcessBuilder().environment();
            environ.keySet().clear();
            checkSizes(environ, 0);

            environ = new ProcessBuilder().environment();
            environ.entrySet().clear();
            checkSizes(environ, 0);

            environ = new ProcessBuilder().environment();
            environ.values().clear();
            checkSizes(environ, 0);
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // Check that various map invariants hold
        //----------------------------------------------------------------
        checkMapSanity(new ProcessBuilder().environment());
        checkMapSanity(System.getenv());
        checkMapEquality(new ProcessBuilder().environment(),
                         new ProcessBuilder().environment());


        //----------------------------------------------------------------
        // Check effects on external "env" command.
        //----------------------------------------------------------------
        try {
            Set<String> env1 = new HashSet<String>
                (Arrays.asList(nativeEnv((String[])null).split("\n")));

            ProcessBuilder pb = new ProcessBuilder();
            pb.environment().put("QwErTyUiOp","AsDfGhJk");

            Set<String> env2 = new HashSet<String>
                (Arrays.asList(nativeEnv(pb).split("\n")));

            check(env2.size() == env1.size() + 1);
            env1.add("QwErTyUiOp=AsDfGhJk");
            check(env1.equals(env2));
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // Test Runtime.exec(...envp...)
        // Check for sort order of environment variables on Windows.
        //----------------------------------------------------------------
        try {
            String systemRoot = "SystemRoot=" + System.getenv("SystemRoot");
            // '+' < 'A' < 'Z' < '_' < 'a' < 'z' < '~'
            String[]envp = {"FOO=BAR","BAZ=GORP","QUUX=",
                            "+=+", "_=_", "~=~", systemRoot};
            String output = nativeEnv(envp);
            String expected = "+=+\nBAZ=GORP\nFOO=BAR\nQUUX=\n"+systemRoot+"\n_=_\n~=~\n";
            // On Windows, Java must keep the environment sorted.
            // Order is random on Unix, so this test does the sort.
            if (! Windows.is())
                output = sortByLinesWindowsly(output);
            equal(output, expected);
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // Test Runtime.exec(...envp...)
        // and check SystemRoot gets set automatically on Windows
        //----------------------------------------------------------------
        try {
            if (Windows.is()) {
                String systemRoot = "SystemRoot=" + System.getenv("SystemRoot");
                String[]envp = {"FOO=BAR","BAZ=GORP","QUUX=",
                                "+=+", "_=_", "~=~"};
                String output = nativeEnv(envp);
                String expected = "+=+\nBAZ=GORP\nFOO=BAR\nQUUX=\n"+systemRoot+"\n_=_\n~=~\n";
                equal(output, expected);
            }
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // System.getenv() must be consistent with System.getenv(String)
        //----------------------------------------------------------------
        try {
            for (Map.Entry<String,String> e : getenv().entrySet())
                equal(getenv(e.getKey()), e.getValue());
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // Fiddle with working directory in child
        //----------------------------------------------------------------
        try {
            String canonicalUserDir =
                new File(System.getProperty("user.dir")).getCanonicalPath();
            String[] sdirs = new String[]
                {".", "..", "/", "/bin",
                 "C:", "c:", "C:/", "c:\\", "\\", "\\bin",
                 "c:\\windows  ", "c:\\Program Files", "c:\\Program Files\\" };
            for (String sdir : sdirs) {
                File dir = new File(sdir);
                if (! (dir.isDirectory() && dir.exists()))
                    continue;
                out.println("Testing directory " + dir);
                //dir = new File(dir.getCanonicalPath());

                ProcessBuilder pb = new ProcessBuilder();
                equal(pb.directory(), null);
                equal(pwdInChild(pb), canonicalUserDir);

                pb.directory(dir);
                equal(pb.directory(), dir);
                equal(pwdInChild(pb), dir.getCanonicalPath());

                pb.directory(null);
                equal(pb.directory(), null);
                equal(pwdInChild(pb), canonicalUserDir);

                pb.directory(dir);
            }
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // Working directory with Unicode in child
        //----------------------------------------------------------------
        try {
            if (UnicodeOS.is()) {
                File dir = new File(System.getProperty("test.dir", "."),
                                    "ProcessBuilderDir\u4e00\u4e02");
                try {
                    if (!dir.exists())
                        dir.mkdir();
                    out.println("Testing Unicode directory:" + dir);
                    ProcessBuilder pb = new ProcessBuilder();
                    pb.directory(dir);
                    equal(pwdInChild(pb), dir.getCanonicalPath());
                } finally {
                    if (dir.exists())
                        dir.delete();
                }
            }
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // OOME in child allocating maximally sized array
        // Test for hotspot/jvmti bug 6850957
        //----------------------------------------------------------------
        try {
            List<String> list = new ArrayList<String>(javaChildArgs);
            list.add(1, String.format("-XX:OnOutOfMemoryError=%s -version",
                                      javaExe));
            list.add("ArrayOOME");
            ProcessResults r = run(new ProcessBuilder(list));
            check(r.err().contains("java.lang.OutOfMemoryError:"));
            check(r.err().contains(javaExe));
            check(r.err().contains(System.getProperty("java.version")));
            equal(r.exitValue(), 1);
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // Windows has tricky semi-case-insensitive semantics
        //----------------------------------------------------------------
        if (Windows.is())
            try {
                out.println("Running case insensitve variable tests");
                for (String[] namePair :
                         new String[][]
                    { new String[]{"PATH","PaTh"},
                      new String[]{"home","HOME"},
                      new String[]{"SYSTEMROOT","SystemRoot"}}) {
                    check((getenv(namePair[0]) == null &&
                           getenv(namePair[1]) == null)
                          ||
                          getenv(namePair[0]).equals(getenv(namePair[1])),
                          "Windows environment variables are not case insensitive");
                }
            } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // Test proper Unicode child environment transfer
        //----------------------------------------------------------------
        if (UnicodeOS.is())
            try {
                ProcessBuilder pb = new ProcessBuilder();
                pb.environment().put("\u1234","\u5678");
                pb.environment().remove("PATH");
                equal(getenvInChild1234(pb), "\u5678");
            } catch (Throwable t) { unexpected(t); }


        //----------------------------------------------------------------
        // Test Runtime.exec(...envp...) with envstrings with initial `='
        //----------------------------------------------------------------
        try {
            List<String> childArgs = new ArrayList<String>(javaChildArgs);
            childArgs.add("System.getenv()");
            String[] cmdp = childArgs.toArray(new String[childArgs.size()]);
            String[] envp;
            String[] envpWin = {"=C:=\\", "=ExitValue=3", "SystemRoot="+systemRoot};
            String[] envpOth = {"=ExitValue=3", "=C:=\\"};
            if (Windows.is()) {
                envp = envpWin;
            } else {
                envp = envpOth;
            }
            Process p = Runtime.getRuntime().exec(cmdp, envp);
            String expected = Windows.is() ? "=C:=\\,=ExitValue=3,SystemRoot="+systemRoot+"," : "=C:=\\,";
            expected = AIX.is() ? expected + "LIBPATH="+libpath+",": expected;
            String commandOutput = commandOutput(p);
            if (MacOSX.is()) {
                commandOutput = removeMacExpectedVars(commandOutput);
            }
            if (AIX.is()) {
                commandOutput = removeAixExpectedVars(commandOutput);
            }
            equal(commandOutput, expected);
            if (Windows.is()) {
                ProcessBuilder pb = new ProcessBuilder(childArgs);
                pb.environment().clear();
                pb.environment().put("SystemRoot", systemRoot);
                pb.environment().put("=ExitValue", "3");
                pb.environment().put("=C:", "\\");
                equal(commandOutput(pb), expected);
            }
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // Test Runtime.exec(...envp...) with envstrings without any `='
        //----------------------------------------------------------------
        try {
            String[] cmdp = {"echo"};
            String[] envp = {"Hello", "World"}; // Yuck!
            Process p = Runtime.getRuntime().exec(cmdp, envp);
            equal(commandOutput(p), "\n");
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // Test Runtime.exec(...envp...) with envstrings containing NULs
        //----------------------------------------------------------------
        try {
            List<String> childArgs = new ArrayList<String>(javaChildArgs);
            childArgs.add("System.getenv()");
            String[] cmdp = childArgs.toArray(new String[childArgs.size()]);
            String[] envpWin = {"SystemRoot="+systemRoot, "LC_ALL=C\u0000\u0000", // Yuck!
                             "FO\u0000=B\u0000R"};
            String[] envpOth = {"LC_ALL=C\u0000\u0000", // Yuck!
                             "FO\u0000=B\u0000R"};
            String[] envp;
            if (Windows.is()) {
                envp = envpWin;
            } else {
                envp = envpOth;
            }
            System.out.println ("cmdp");
            for (int i=0; i<cmdp.length; i++) {
                System.out.printf ("cmdp %d: %s\n", i, cmdp[i]);
            }
            System.out.println ("envp");
            for (int i=0; i<envp.length; i++) {
                System.out.printf ("envp %d: %s\n", i, envp[i]);
            }
            Process p = Runtime.getRuntime().exec(cmdp, envp);
            String commandOutput = commandOutput(p);
            if (MacOSX.is()) {
                commandOutput = removeMacExpectedVars(commandOutput);
            }
            if (AIX.is()) {
                commandOutput = removeAixExpectedVars(commandOutput);
            }
            check(commandOutput.equals(Windows.is()
                    ? "LC_ALL=C,SystemRoot="+systemRoot+","
                    : AIX.is()
                            ? "LC_ALL=C,LIBPATH="+libpath+","
                            : "LC_ALL=C,"),
                  "Incorrect handling of envstrings containing NULs");
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // Test the redirectErrorStream property
        //----------------------------------------------------------------
        try {
            ProcessBuilder pb = new ProcessBuilder();
            equal(pb.redirectErrorStream(), false);
            equal(pb.redirectErrorStream(true), pb);
            equal(pb.redirectErrorStream(), true);
            equal(pb.redirectErrorStream(false), pb);
            equal(pb.redirectErrorStream(), false);
        } catch (Throwable t) { unexpected(t); }

        try {
            List<String> childArgs = new ArrayList<String>(javaChildArgs);
            childArgs.add("OutErr");
            ProcessBuilder pb = new ProcessBuilder(childArgs);
            {
                ProcessResults r = run(pb);
                equal(r.out(), "outout");
                equal(r.err(), "errerr");
            }
            {
                pb.redirectErrorStream(true);
                ProcessResults r = run(pb);
                equal(r.out(), "outerrouterr");
                equal(r.err(), "");
            }
        } catch (Throwable t) { unexpected(t); }

        if (Unix.is()) {
            //----------------------------------------------------------------
            // We can find true and false when PATH is null
            //----------------------------------------------------------------
            try {
                List<String> childArgs = new ArrayList<String>(javaChildArgs);
                childArgs.add("null PATH");
                ProcessBuilder pb = new ProcessBuilder(childArgs);
                pb.environment().remove("PATH");
                ProcessResults r = run(pb);
                equal(r.out(), "");
                equal(r.err(), "");
                equal(r.exitValue(), 0);
            } catch (Throwable t) { unexpected(t); }

            //----------------------------------------------------------------
            // PATH search algorithm on Unix
            //----------------------------------------------------------------
            try {
                List<String> childArgs = new ArrayList<String>(javaChildArgs);
                childArgs.add("PATH search algorithm");
                ProcessBuilder pb = new ProcessBuilder(childArgs);
                pb.environment().put("PATH", "dir1:dir2:");
                ProcessResults r = run(pb);
                equal(r.out(), "");
                equal(r.err(), "");
                equal(r.exitValue(), True.exitValue());
            } catch (Throwable t) { unexpected(t); }

            //----------------------------------------------------------------
            // Parent's, not child's PATH is used
            //----------------------------------------------------------------
            try {
                new File("suBdiR").mkdirs();
                copy(TrueExe.path(), "suBdiR/unliKely");
                final ProcessBuilder pb =
                    new ProcessBuilder(new String[]{"unliKely"});
                pb.environment().put("PATH", "suBdiR");
                THROWS(IOException.class, () -> pb.start());
            } catch (Throwable t) { unexpected(t);
            } finally {
                new File("suBdiR/unliKely").delete();
                new File("suBdiR").delete();
            }
        }

        //----------------------------------------------------------------
        // Attempt to start bogus program ""
        //----------------------------------------------------------------
        try {
            new ProcessBuilder("").start();
            fail("Expected IOException not thrown");
        } catch (IOException e) {
            String m = e.getMessage();
            if (EnglishUnix.is() &&
                ! matches(m, NO_SUCH_FILE_ERROR_MSG))
                unexpected(e);
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // Check that attempt to execute program name with funny
        // characters throws an exception containing those characters.
        //----------------------------------------------------------------
        for (String programName : new String[] {"\u00f0", "\u01f0"})
            try {
                new ProcessBuilder(programName).start();
                fail("Expected IOException not thrown");
            } catch (IOException e) {
                String m = e.getMessage();
                Pattern p = Pattern.compile(programName);
                if (! matches(m, programName)
                    || (EnglishUnix.is() &&
                        ! matches(m, NO_SUCH_FILE_ERROR_MSG)))
                    unexpected(e);
            } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // Attempt to start process in nonexistent directory fails.
        //----------------------------------------------------------------
        try {
            new ProcessBuilder("echo")
                .directory(new File("UnLiKeLY"))
                .start();
            fail("Expected IOException not thrown");
        } catch (IOException e) {
            String m = e.getMessage();
            if (! matches(m, "in directory")
                || (EnglishUnix.is() &&
                    ! matches(m, NO_SUCH_FILE_ERROR_MSG)))
                unexpected(e);
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // Attempt to write 4095 bytes to the pipe buffer without a
        // reader to drain it would deadlock, if not for the fact that
        // interprocess pipe buffers are at least 4096 bytes.
        //
        // Also, check that available reports all the bytes expected
        // in the pipe buffer, and that I/O operations do the expected
        // things.
        //----------------------------------------------------------------
        try {
            List<String> childArgs = new ArrayList<String>(javaChildArgs);
            childArgs.add("print4095");
            final int SIZE = 4095;
            final Process p = new ProcessBuilder(childArgs).start();
            print4095(p.getOutputStream(), (byte) '!'); // Might hang!
            p.waitFor();                                // Might hang!
            equal(SIZE, p.getInputStream().available());
            equal(SIZE, p.getErrorStream().available());
            THROWS(IOException.class,
                   () -> { p.getOutputStream().write((byte) '!');
                           p.getOutputStream().flush();});

            final byte[] bytes = new byte[SIZE + 1];
            equal(SIZE, p.getInputStream().read(bytes));
            for (int i = 0; i < SIZE; i++)
                equal((byte) '!', bytes[i]);
            equal((byte) 0, bytes[SIZE]);

            equal(SIZE, p.getErrorStream().read(bytes));
            for (int i = 0; i < SIZE; i++)
                equal((byte) 'E', bytes[i]);
            equal((byte) 0, bytes[SIZE]);

            equal(0, p.getInputStream().available());
            equal(0, p.getErrorStream().available());
            equal(-1, p.getErrorStream().read());
            equal(-1, p.getInputStream().read());

            equal(p.exitValue(), 5);

            p.getInputStream().close();
            p.getErrorStream().close();
            try { p.getOutputStream().close(); } catch (IOException flushFailed) { }

            InputStream[] streams = { p.getInputStream(), p.getErrorStream() };
            for (final InputStream in : streams) {
                Fun[] ops = {
                    () -> in.read(),
                    () -> in.read(bytes),
                    () -> in.available()
                };
                for (Fun op : ops) {
                    try {
                        op.f();
                        fail();
                    } catch (IOException expected) {
                        check(expected.getMessage()
                              .matches("[Ss]tream [Cc]losed"));
                    }
                }
            }
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // Check that reads which are pending when Process.destroy is
        // called, get EOF, or IOException("Stream closed").
        //----------------------------------------------------------------
        try {
            final int cases = 4;
            for (int i = 0; i < cases; i++) {
                final int action = i;
                List<String> childArgs = new ArrayList<>(javaChildArgs);
                final ProcessBuilder pb = new ProcessBuilder(childArgs);
                {
                    // Redirect any child VM error output away from the stream being tested
                    // and to the log file. For background see:
                    // 8231297: java/lang/ProcessBuilder/Basic.java test fails intermittently
                    // Destroying the process may, depending on the timing, cause some output
                    // from the child VM.
                    // This test requires the thread reading from the subprocess be blocked
                    // in the read from the subprocess; there should be no bytes to read.
                    // Modify the argument list shared with ProcessBuilder to redirect VM output.
                    assert (childArgs.get(1).equals("-XX:+DisplayVMOutputToStderr")) : "Expected arg 1 to be \"-XX:+DisplayVMOutputToStderr\"";
                    switch (action & 0x1) {
                        case 0:
                            childArgs.set(1, "-XX:+DisplayVMOutputToStderr");
                            childArgs.add(2, "-Xlog:all=warning:stderr");
                            pb.redirectError(INHERIT);
                            break;
                        case 1:
                            childArgs.set(1, "-XX:+DisplayVMOutputToStdout");
                            childArgs.add(2, "-Xlog:all=warning:stdout");
                            pb.redirectOutput(INHERIT);
                            break;
                        default:
                            throw new Error();
                    }
                }
                childArgs.add("sleep");
                final byte[] bytes = new byte[10];
                final Process p = pb.start();
                final CountDownLatch latch = new CountDownLatch(1);
                final InputStream s;
                switch (action & 0x1) {
                    case 0: s = p.getInputStream(); break;
                    case 1: s = p.getErrorStream(); break;
                    default: throw new Error();
                }
                final Thread thread = new Thread() {
                    public void run() {
                        try {
                            int r;
                            latch.countDown();
                            switch (action & 0x2) {
                                case 0: r = s.read(); break;
                                case 2: r = s.read(bytes); break;
                                default: throw new Error();
                            }
                            if (r >= 0) {
                                // The child sent unexpected output; print it to diagnose
                                System.out.println("Unexpected child output, to: " +
                                        ((action & 0x1) == 0 ? "getInputStream" : "getErrorStream"));
                                System.out.println("Child args: " + childArgs);
                                if ((action & 0x2) == 0) {
                                    System.out.write(r);    // Single character

                                } else {
                                    System.out.write(bytes, 0, r);
                                }
                                for (int c = s.read(); c >= 0; c = s.read())
                                    System.out.write(c);
                                System.out.println("\nEND Child output.");
                            }
                            equal(-1, r);
                        } catch (IOException ioe) {
                            if (!ioe.getMessage().equals("Stream closed")) {
                                // BufferedInputStream may throw IOE("Stream closed").
                                unexpected(ioe);
                            }
                        } catch (Throwable t) { unexpected(t); }}};

                thread.start();
                latch.await();
                Thread.sleep(30);

                if (s instanceof BufferedInputStream) {
                    // Wait until after the s.read occurs in "thread" by
                    // checking when the input stream monitor is acquired
                    // (BufferedInputStream.read is synchronized)
                    while (!isLocked(s, 10)) {
                        Thread.sleep(100);
                    }
                }
                p.destroy();
                thread.join();
            }
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // Check that subprocesses which create subprocesses of their
        // own do not cause parent to hang waiting for file
        // descriptors to be closed.
        //----------------------------------------------------------------
        try {
            if (Unix.is()
                && new File("/bin/bash").exists()
                && new File("/bin/sleep").exists()) {
                // Notice that we only destroy the process created by us (i.e.
                // our child) but not our grandchild (i.e. '/bin/sleep'). So
                // pay attention that the grandchild doesn't run too long to
                // avoid polluting the process space with useless processes.
                // Running the grandchild for 60s should be more than enough.
                final String[] cmd = { "/bin/bash", "-c", "(/bin/sleep 60)" };
                final String[] cmdkill = { "/bin/bash", "-c", "(/usr/bin/pkill -f \"sleep 60\")" };
                final ProcessBuilder pb = new ProcessBuilder(cmd);
                final Process p = pb.start();
                final InputStream stdout = p.getInputStream();
                final InputStream stderr = p.getErrorStream();
                final OutputStream stdin = p.getOutputStream();
                final Thread reader = new Thread() {
                    public void run() {
                        try { stdout.read(); }
                        catch (IOException e) {
                            // Check that reader failed because stream was
                            // asynchronously closed.
                            // e.printStackTrace();
                            String msg = e.getMessage();
                            if (EnglishUnix.is() &&
                                ! (msg.matches(".*Bad file.*") ||
                                        msg.matches(".*Stream closed.*")))
                                unexpected(e);
                        }
                        catch (Throwable t) { unexpected(t); }}};
                reader.setDaemon(true);
                reader.start();
                Thread.sleep(100);
                p.destroy();
                check(p.waitFor() != 0);
                check(p.exitValue() != 0);
                // Subprocess is now dead, but file descriptors remain open.
                // Make sure the test will fail if we don't manage to close
                // the open streams within 30 seconds. Notice that this time
                // must be shorter than the sleep time of the grandchild.
                Timer t = new Timer("test/java/lang/ProcessBuilder/Basic.java process reaper", true);
                t.schedule(new TimerTask() {
                      public void run() {
                          fail("Subprocesses which create subprocesses of " +
                               "their own caused the parent to hang while " +
                               "waiting for file descriptors to be closed.");
                          System.exit(-1);
                      }
                  }, 30000);
                stdout.close();
                stderr.close();
                stdin.close();
                new ProcessBuilder(cmdkill).start();
                // All streams successfully closed so we can cancel the timer.
                t.cancel();
                //----------------------------------------------------------
                // There remain unsolved issues with asynchronous close.
                // Here's a highly non-portable experiment to demonstrate:
                //----------------------------------------------------------
                if (Boolean.getBoolean("wakeupJeff!")) {
                    System.out.println("wakeupJeff!");
                    // Initialize signal handler for INTERRUPT_SIGNAL.
                    new FileInputStream("/bin/sleep").getChannel().close();
                    // Send INTERRUPT_SIGNAL to every thread in this java.
                    String[] wakeupJeff = {
                        "/bin/bash", "-c",
                        "/bin/ps --noheaders -Lfp $PPID | " +
                        "/usr/bin/perl -nale 'print $F[3]' | " +
                        // INTERRUPT_SIGNAL == 62 on my machine du jour.
                        "/usr/bin/xargs kill -62"
                    };
                    new ProcessBuilder(wakeupJeff).start().waitFor();
                    // If wakeupJeff worked, reader probably got EBADF.
                    reader.join();
                }
            }

            //----------------------------------------------------------------
            // Check the Process toString() method
            //----------------------------------------------------------------
            {
                List<String> childArgs = new ArrayList<String>(javaChildArgs);
                childArgs.add("testIO");
                ProcessBuilder pb = new ProcessBuilder(childArgs);
                pb.redirectInput(Redirect.PIPE);
                pb.redirectOutput(DISCARD);
                pb.redirectError(DISCARD);
                final Process p = pb.start();
                // Child process waits until it gets input
                String s = p.toString();
                check(s.contains("not exited"));
                check(s.contains("pid=" + p.pid() + ","));

                new PrintStream(p.getOutputStream()).print("standard input");
                p.getOutputStream().close();

                // Check the toString after it exits
                int exitValue = p.waitFor();
                s = p.toString();
                check(s.contains("pid=" + p.pid() + ","));
                check(s.contains("exitValue=" + exitValue) &&
                        !s.contains("not exited"));
            }
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // Attempt to start process with insufficient permissions fails.
        //----------------------------------------------------------------
        try {
            new File("emptyCommand").delete();
            new FileOutputStream("emptyCommand").close();
            new File("emptyCommand").setExecutable(false);
            new ProcessBuilder("./emptyCommand").start();
            fail("Expected IOException not thrown");
        } catch (IOException e) {
            new File("./emptyCommand").delete();
            String m = e.getMessage();
            if (EnglishUnix.is() &&
                ! matches(m, PERMISSION_DENIED_ERROR_MSG))
                unexpected(e);
        } catch (Throwable t) { unexpected(t); }

        new File("emptyCommand").delete();

        //----------------------------------------------------------------
        // Check for correct security permission behavior
        //----------------------------------------------------------------
        final Policy policy = new Policy();
        Policy.setPolicy(policy);
        System.setSecurityManager(new SecurityManager());

        try {
            // No permissions required to CREATE a ProcessBuilder
            policy.setPermissions(/* Nothing */);
            new ProcessBuilder("env").directory(null).directory();
            new ProcessBuilder("env").directory(new File("dir")).directory();
            new ProcessBuilder("env").command("??").command();
        } catch (Throwable t) { unexpected(t); }

        THROWS(SecurityException.class,
               () -> { policy.setPermissions(/* Nothing */);
                       System.getenv("foo");},
               () -> { policy.setPermissions(/* Nothing */);
                       System.getenv();},
               () -> { policy.setPermissions(/* Nothing */);
                       new ProcessBuilder("echo").start();},
               () -> { policy.setPermissions(/* Nothing */);
                       Runtime.getRuntime().exec("echo");},
               () -> { policy.setPermissions(
                               new RuntimePermission("getenv.bar"));
                       System.getenv("foo");});

        try {
            policy.setPermissions(new RuntimePermission("getenv.foo"));
            System.getenv("foo");

            policy.setPermissions(new RuntimePermission("getenv.*"));
            System.getenv("foo");
            System.getenv();
            new ProcessBuilder().environment();
        } catch (Throwable t) { unexpected(t); }


        final Permission execPermission
            = new FilePermission("<<ALL FILES>>", "execute");

        THROWS(SecurityException.class,
               () -> { // environment permission by itself insufficient
                       policy.setPermissions(new RuntimePermission("getenv.*"));
                       ProcessBuilder pb = new ProcessBuilder("env");
                       pb.environment().put("foo","bar");
                       pb.start();},
               () -> { // exec permission by itself insufficient
                       policy.setPermissions(execPermission);
                       ProcessBuilder pb = new ProcessBuilder("env");
                       pb.environment().put("foo","bar");
                       pb.start();});

        try {
            // Both permissions? OK.
            policy.setPermissions(new RuntimePermission("getenv.*"),
                                  execPermission);
            ProcessBuilder pb = new ProcessBuilder("env");
            pb.environment().put("foo","bar");
            Process p = pb.start();
            closeStreams(p);
        } catch (IOException e) { // OK
        } catch (Throwable t) { unexpected(t); }

        try {
            // Don't need environment permission unless READING environment
            policy.setPermissions(execPermission);
            Runtime.getRuntime().exec("env", new String[]{});
        } catch (IOException e) { // OK
        } catch (Throwable t) { unexpected(t); }

        try {
            // Don't need environment permission unless READING environment
            policy.setPermissions(execPermission);
            new ProcessBuilder("env").start();
        } catch (IOException e) { // OK
        } catch (Throwable t) { unexpected(t); }

        // Restore "normal" state without a security manager
        policy.setPermissions(new RuntimePermission("setSecurityManager"));
        System.setSecurityManager(null);

        //----------------------------------------------------------------
        // Check that Process.isAlive() &
        // Process.waitFor(0, TimeUnit.MILLISECONDS) work as expected.
        //----------------------------------------------------------------
        try {
            List<String> childArgs = new ArrayList<String>(javaChildArgs);
            childArgs.add("sleep");
            final Process p = new ProcessBuilder(childArgs).start();
            long start = System.nanoTime();
            if (!p.isAlive() || p.waitFor(0, TimeUnit.MILLISECONDS)) {
                fail("Test failed: Process exited prematurely");
            }
            long end = System.nanoTime();
            // give waitFor(timeout) a wide berth (2s)
            System.out.printf(" waitFor process: delta: %d%n",(end - start) );

            if ((end - start) > TimeUnit.SECONDS.toNanos(2))
                fail("Test failed: waitFor took too long (" + (end - start) + "ns)");

            p.destroy();
            p.waitFor();

            if (p.isAlive() ||
                !p.waitFor(0, TimeUnit.MILLISECONDS))
            {
                fail("Test failed: Process still alive - please terminate " +
                    p.toString() + " manually");
            }
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // Check that Process.waitFor(timeout, TimeUnit.MILLISECONDS)
        // works as expected.
        //----------------------------------------------------------------
        try {
            List<String> childArgs = new ArrayList<String>(javaChildArgs);
            childArgs.add("sleep");
            final Process p = new ProcessBuilder(childArgs).start();
            long start = System.nanoTime();

            p.waitFor(10, TimeUnit.MILLISECONDS);

            long end = System.nanoTime();
            if ((end - start) < TimeUnit.MILLISECONDS.toNanos(10))
                fail("Test failed: waitFor didn't take long enough (" + (end - start) + "ns)");

            p.destroy();
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // Check that Process.waitFor(timeout, TimeUnit.MILLISECONDS)
        // interrupt works as expected, if interrupted while waiting.
        //----------------------------------------------------------------
        try {
            List<String> childArgs = new ArrayList<String>(javaChildArgs);
            childArgs.add("sleep");
            final Process p = new ProcessBuilder(childArgs).start();
            final long start = System.nanoTime();
            final CountDownLatch aboutToWaitFor = new CountDownLatch(1);

            final Thread thread = new Thread() {
                public void run() {
                    try {
                        aboutToWaitFor.countDown();
                        Thread.currentThread().interrupt();
                        boolean result = p.waitFor(30L * 1000L, TimeUnit.MILLISECONDS);
                        fail("waitFor() wasn't interrupted, its return value was: " + result);
                    } catch (InterruptedException success) {
                    } catch (Throwable t) { unexpected(t); }
                }
            };

            thread.start();
            aboutToWaitFor.await();
            thread.interrupt();
            thread.join(10L * 1000L);
            check(millisElapsedSince(start) < 10L * 1000L);
            check(!thread.isAlive());
            p.destroy();
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // Check that Process.waitFor(Long.MAX_VALUE, TimeUnit.MILLISECONDS)
        // interrupt works as expected, if interrupted while waiting.
        //----------------------------------------------------------------
        try {
            List<String> childArgs = new ArrayList<String>(javaChildArgs);
            childArgs.add("sleep");
            final Process p = new ProcessBuilder(childArgs).start();
            final long start = System.nanoTime();
            final CountDownLatch aboutToWaitFor = new CountDownLatch(1);

            final Thread thread = new Thread() {
                public void run() {
                    try {
                        aboutToWaitFor.countDown();
                        Thread.currentThread().interrupt();
                        boolean result = p.waitFor(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
                        fail("waitFor() wasn't interrupted, its return value was: " + result);
                    } catch (InterruptedException success) {
                    } catch (Throwable t) { unexpected(t); }
                }
            };

            thread.start();
            aboutToWaitFor.await();
            thread.interrupt();
            thread.join(10L * 1000L);
            check(millisElapsedSince(start) < 10L * 1000L);
            check(!thread.isAlive());
            p.destroy();
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // Check that Process.waitFor(timeout, TimeUnit.MILLISECONDS)
        // interrupt works as expected, if interrupted before waiting.
        //----------------------------------------------------------------
        try {
            List<String> childArgs = new ArrayList<String>(javaChildArgs);
            childArgs.add("sleep");
            final Process p = new ProcessBuilder(childArgs).start();
            final long start = System.nanoTime();
            final CountDownLatch threadStarted = new CountDownLatch(1);

            final Thread thread = new Thread() {
                public void run() {
                    try {
                        threadStarted.countDown();
                        do { Thread.yield(); }
                        while (!Thread.currentThread().isInterrupted());
                        boolean result = p.waitFor(30L * 1000L, TimeUnit.MILLISECONDS);
                        fail("waitFor() wasn't interrupted, its return value was: " + result);
                    } catch (InterruptedException success) {
                    } catch (Throwable t) { unexpected(t); }
                }
            };

            thread.start();
            threadStarted.await();
            thread.interrupt();
            thread.join(10L * 1000L);
            check(millisElapsedSince(start) < 10L * 1000L);
            check(!thread.isAlive());
            p.destroy();
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // Check that Process.waitFor(timeout, null) throws NPE.
        //----------------------------------------------------------------
        try {
            List<String> childArgs = new ArrayList<String>(javaChildArgs);
            childArgs.add("sleep");
            final Process p = new ProcessBuilder(childArgs).start();
            THROWS(NullPointerException.class,
                    () ->  p.waitFor(10L, null));
            THROWS(NullPointerException.class,
                    () ->  p.waitFor(0L, null));
            THROWS(NullPointerException.class,
                    () -> p.waitFor(-1L, null));
            // Terminate process and recheck after it exits
            p.destroy();
            p.waitFor();
            THROWS(NullPointerException.class,
                    () -> p.waitFor(10L, null));
            THROWS(NullPointerException.class,
                    () -> p.waitFor(0L, null));
            THROWS(NullPointerException.class,
                    () -> p.waitFor(-1L, null));
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // Check that default implementation of Process.waitFor(timeout, null) throws NPE.
        //----------------------------------------------------------------
        try {
            List<String> childArgs = new ArrayList<String>(javaChildArgs);
            childArgs.add("sleep");
            final Process proc = new ProcessBuilder(childArgs).start();
            final DelegatingProcess p = new DelegatingProcess(proc);

            THROWS(NullPointerException.class,
                    () ->  p.waitFor(10L, null));
            THROWS(NullPointerException.class,
                    () ->  p.waitFor(0L, null));
            THROWS(NullPointerException.class,
                    () ->  p.waitFor(-1L, null));
            // Terminate process and recheck after it exits
            p.destroy();
            p.waitFor();
            THROWS(NullPointerException.class,
                    () -> p.waitFor(10L, null));
            THROWS(NullPointerException.class,
                    () -> p.waitFor(0L, null));
            THROWS(NullPointerException.class,
                    () -> p.waitFor(-1L, null));
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // Check the default implementation for
        // Process.waitFor(long, TimeUnit)
        //----------------------------------------------------------------
        try {
            List<String> childArgs = new ArrayList<String>(javaChildArgs);
            childArgs.add("sleep");
            final Process proc = new ProcessBuilder(childArgs).start();
            DelegatingProcess p = new DelegatingProcess(proc);
            long start = System.nanoTime();

            p.waitFor(1000, TimeUnit.MILLISECONDS);

            long end = System.nanoTime();
            if ((end - start) < 500000000)
                fail("Test failed: waitFor didn't take long enough");

            p.destroy();

            p.waitFor(1000, TimeUnit.MILLISECONDS);
        } catch (Throwable t) { unexpected(t); }
    }

    static void closeStreams(Process p) {
        try {
            p.getOutputStream().close();
            p.getInputStream().close();
            p.getErrorStream().close();
        } catch (Throwable t) { unexpected(t); }
    }

    //----------------------------------------------------------------
    // A Policy class designed to make permissions fiddling very easy.
    //----------------------------------------------------------------
    @SuppressWarnings("removal")
    private static class Policy extends java.security.Policy {
        static final java.security.Policy DEFAULT_POLICY = java.security.Policy.getPolicy();

        private Permissions perms;

        public void setPermissions(Permission...permissions) {
            perms = new Permissions();
            for (Permission permission : permissions)
                perms.add(permission);
        }

        public Policy() { setPermissions(/* Nothing */); }

        public PermissionCollection getPermissions(CodeSource cs) {
            return perms;
        }

        public PermissionCollection getPermissions(ProtectionDomain pd) {
            return perms;
        }

        public boolean implies(ProtectionDomain pd, Permission p) {
            return perms.implies(p) || DEFAULT_POLICY.implies(pd, p);
        }

        public void refresh() {}
    }

    private static class StreamAccumulator extends Thread {
        private final InputStream is;
        private final StringBuilder sb = new StringBuilder();
        private Throwable throwable = null;

        public String result () throws Throwable {
            if (throwable != null)
                throw throwable;
            return sb.toString();
        }

        StreamAccumulator (InputStream is) {
            this.is = is;
        }

        public void run() {
            try {
                Reader r = new InputStreamReader(is);
                char[] buf = new char[4096];
                int n;
                while ((n = r.read(buf)) > 0) {
                    sb.append(buf,0,n);
                }
            } catch (Throwable t) {
                throwable = t;
            } finally {
                try { is.close(); }
                catch (Throwable t) { throwable = t; }
            }
        }
    }

    static ProcessResults run(ProcessBuilder pb) {
        try {
            return run(pb.start());
        } catch (Throwable t) { unexpected(t); return null; }
    }

    private static ProcessResults run(Process p) {
        Throwable throwable = null;
        int exitValue = -1;
        String out = "";
        String err = "";

        StreamAccumulator outAccumulator =
            new StreamAccumulator(p.getInputStream());
        StreamAccumulator errAccumulator =
            new StreamAccumulator(p.getErrorStream());

        try {
            outAccumulator.start();
            errAccumulator.start();

            exitValue = p.waitFor();

            outAccumulator.join();
            errAccumulator.join();

            out = outAccumulator.result();
            err = errAccumulator.result();
        } catch (Throwable t) {
            throwable = t;
        }

        return new ProcessResults(out, err, exitValue, throwable);
    }

    //----------------------------------------------------------------
    // Results of a command
    //----------------------------------------------------------------
    private static class ProcessResults {
        private final String out;
        private final String err;
        private final int exitValue;
        private final Throwable throwable;

        public ProcessResults(String out,
                              String err,
                              int exitValue,
                              Throwable throwable) {
            this.out = out;
            this.err = err;
            this.exitValue = exitValue;
            this.throwable = throwable;
        }

        public String out()          { return out; }
        public String err()          { return err; }
        public int exitValue()       { return exitValue; }
        public Throwable throwable() { return throwable; }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("<STDOUT>\n" + out() + "</STDOUT>\n")
                .append("<STDERR>\n" + err() + "</STDERR>\n")
                .append("exitValue = " + exitValue + "\n");
            if (throwable != null)
                sb.append(throwable.getStackTrace());
            return sb.toString();
        }
    }

    //--------------------- Infrastructure ---------------------------
    static volatile int passed = 0, failed = 0;
    static void pass() {passed++;}
    static void fail() {failed++; Thread.dumpStack();}
    static void fail(String msg) {System.err.println(msg); fail();}
    static void unexpected(Throwable t) {failed++; t.printStackTrace();}
    static void check(boolean cond) {if (cond) pass(); else fail();}
    static void check(boolean cond, String m) {if (cond) pass(); else fail(m);}
    static void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else fail(">'" + x + "'<" + " not equal to " + "'" + y + "'");}

    public static void main(String[] args) throws Throwable {
        try {realMain(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
    interface Fun {void f() throws Throwable;}
    static void THROWS(Class<? extends Throwable> k, Fun... fs) {
        for (Fun f : fs)
            try { f.f(); fail("Expected " + k.getName() + " not thrown"); }
            catch (Throwable t) {
                if (k.isAssignableFrom(t.getClass())) pass();
                else unexpected(t);}}

    static boolean isLocked(final Object monitor, final long millis) throws InterruptedException {
        return new Thread() {
            volatile boolean unlocked;

            @Override
            public void run() {
                synchronized (monitor) { unlocked = true; }
            }

            boolean isLocked() throws InterruptedException {
                start();
                join(millis);
                return !unlocked;
            }
        }.isLocked();
    }
}
