/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

import jdk.test.lib.util.FileUtils;
import jdk.test.lib.JDKToolFinder;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static java.lang.String.format;
import static java.lang.System.out;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/*
 * @test
 * @bug 8170952
 * @library /lib/testlibrary /test/lib
 * @build jdk.test.lib.Platform
 *        jdk.test.lib.util.FileUtils
 *        jdk.test.lib.JDKToolFinder
 * @run testng CLICompatibility
 * @summary Basic test for compatibility of CLI options
 */

public class CLICompatibility {
    static final Path TEST_CLASSES = Paths.get(System.getProperty("test.classes", "."));
    static final Path USER_DIR = Paths.get(System.getProperty("user.dir"));

    static final String TOOL_VM_OPTIONS = System.getProperty("test.tool.vm.opts", "");

    final boolean legacyOnly;  // for running on older JDK's ( test validation )

    // Resources we know to exist, that can be used for creating jar files.
    static final String RES1 = "CLICompatibility.class";
    static final String RES2 = "CLICompatibility$Result.class";

    @BeforeTest
    public void setupResourcesForJar() throws Exception {
        // Copy the files that we are going to use for creating/updating test
        // jar files, so that they can be referred to without '-C dir'
        Files.copy(TEST_CLASSES.resolve(RES1), USER_DIR.resolve(RES1));
        Files.copy(TEST_CLASSES.resolve(RES2), USER_DIR.resolve(RES2));
    }

    static final IOConsumer<InputStream> ASSERT_CONTAINS_RES1 = in -> {
        try (JarInputStream jin = new JarInputStream(in)) {
            assertTrue(jarContains(jin, RES1), "Failed to find " + RES1);
        }
    };
    static final IOConsumer<InputStream> ASSERT_CONTAINS_RES2 = in -> {
        try (JarInputStream jin = new JarInputStream(in)) {
            assertTrue(jarContains(jin, RES2), "Failed to find " + RES2);
        }
    };
    static final IOConsumer<InputStream> ASSERT_CONTAINS_MAINFEST = in -> {
        try (JarInputStream jin = new JarInputStream(in)) {
            assertTrue(jin.getManifest() != null, "No META-INF/MANIFEST.MF");
        }
    };
    static final IOConsumer<InputStream> ASSERT_DOES_NOT_CONTAIN_MAINFEST = in -> {
        try (JarInputStream jin = new JarInputStream(in)) {
            assertTrue(jin.getManifest() == null, "Found unexpected META-INF/MANIFEST.MF");
        }
    };

    static final FailCheckerWithMessage FAIL_TOO_MANY_MAIN_OPS =
        new FailCheckerWithMessage("You may not specify more than one '-cuxtid' options",
        /* legacy */ "{ctxui}[vfmn0Me] [jar-file] [manifest-file] [entry-point] [-C dir] files");

    // Create

    @Test
    public void createBadArgs() {
        final FailCheckerWithMessage FAIL_CREATE_NO_ARGS = new FailCheckerWithMessage(
                "'c' flag requires manifest or input files to be specified!");

        jar("c")
            .assertFailure()
            .resultChecker(FAIL_CREATE_NO_ARGS);

        jar("-c")
            .assertFailure()
            .resultChecker(FAIL_CREATE_NO_ARGS);

        if (!legacyOnly)
            jar("--create")
                .assertFailure()
                .resultChecker(FAIL_CREATE_NO_ARGS);

        jar("ct")
            .assertFailure()
            .resultChecker(FAIL_TOO_MANY_MAIN_OPS);

        jar("-ct")
            .assertFailure()
            .resultChecker(FAIL_TOO_MANY_MAIN_OPS);

        if (!legacyOnly)
            jar("--create --list")
                .assertFailure()
                .resultChecker(FAIL_TOO_MANY_MAIN_OPS);
    }

    @Test
    public void createWriteToFile() throws IOException {
        Path path = Paths.get("createJarFile.jar");  // for creating
        String jn = path.toString();
        for (String opts : new String[]{"cf " + jn, "-cf " + jn, "--create --file=" + jn}) {
            if (legacyOnly && opts.startsWith("--"))
                continue;

            jar(opts, RES1)
                .assertSuccess()
                .resultChecker(r -> {
                    ASSERT_CONTAINS_RES1.accept(Files.newInputStream(path));
                    ASSERT_CONTAINS_MAINFEST.accept(Files.newInputStream(path));
                });
        }
        FileUtils.deleteFileIfExistsWithRetry(path);
    }

    @Test
    public void createWriteToStdout() throws IOException {
        for (String opts : new String[]{"c", "-c", "--create"}) {
            if (legacyOnly && opts.startsWith("--"))
                continue;

            jar(opts, RES1)
                .assertSuccess()
                .resultChecker(r -> {
                    ASSERT_CONTAINS_RES1.accept(r.stdoutAsStream());
                    ASSERT_CONTAINS_MAINFEST.accept(r.stdoutAsStream());
                });
        }
    }

    @Test
    public void createWriteToStdoutNoManifest() throws IOException {
        for (String opts : new String[]{"cM", "-cM", "--create --no-manifest"} ){
            if (legacyOnly && opts.startsWith("--"))
                continue;

            jar(opts, RES1)
                .assertSuccess()
                .resultChecker(r -> {
                    ASSERT_CONTAINS_RES1.accept(r.stdoutAsStream());
                    ASSERT_DOES_NOT_CONTAIN_MAINFEST.accept(r.stdoutAsStream());
                });
        }
    }

    // Update

    @Test
    public void updateBadArgs() {
        final FailCheckerWithMessage FAIL_UPDATE_NO_ARGS = new FailCheckerWithMessage(
                "'u' flag requires manifest, 'e' flag or input files to be specified!");

        jar("u")
            .assertFailure()
            .resultChecker(FAIL_UPDATE_NO_ARGS);

        jar("-u")
            .assertFailure()
            .resultChecker(FAIL_UPDATE_NO_ARGS);

        if (!legacyOnly)
            jar("--update")
                .assertFailure()
                .resultChecker(FAIL_UPDATE_NO_ARGS);

        jar("ut")
            .assertFailure()
            .resultChecker(FAIL_TOO_MANY_MAIN_OPS);

        jar("-ut")
            .assertFailure()
            .resultChecker(FAIL_TOO_MANY_MAIN_OPS);

        if (!legacyOnly)
            jar("--update --list")
                .assertFailure()
                .resultChecker(FAIL_TOO_MANY_MAIN_OPS);
    }

    @Test
    public void updateReadFileWriteFile() throws IOException {
        Path path = Paths.get("updateReadWriteStdout.jar");  // for updating
        String jn = path.toString();

        for (String opts : new String[]{"uf " + jn, "-uf " + jn, "--update --file=" + jn}) {
            if (legacyOnly && opts.startsWith("--"))
                continue;

            createJar(path, RES1);
            jar(opts, RES2)
                .assertSuccess()
                .resultChecker(r -> {
                    ASSERT_CONTAINS_RES1.accept(Files.newInputStream(path));
                    ASSERT_CONTAINS_RES2.accept(Files.newInputStream(path));
                    ASSERT_CONTAINS_MAINFEST.accept(Files.newInputStream(path));
                });
        }
        FileUtils.deleteFileIfExistsWithRetry(path);
    }

    @Test
    public void updateReadStdinWriteStdout() throws IOException {
        Path path = Paths.get("updateReadStdinWriteStdout.jar");

        for (String opts : new String[]{"u", "-u", "--update"}) {
            if (legacyOnly && opts.startsWith("--"))
                continue;

            createJar(path, RES1);
            jarWithStdin(path.toFile(), opts, RES2)
                .assertSuccess()
                .resultChecker(r -> {
                    ASSERT_CONTAINS_RES1.accept(r.stdoutAsStream());
                    ASSERT_CONTAINS_RES2.accept(r.stdoutAsStream());
                    ASSERT_CONTAINS_MAINFEST.accept(r.stdoutAsStream());
                });
        }
        FileUtils.deleteFileIfExistsWithRetry(path);
    }

    @Test
    public void updateReadStdinWriteStdoutNoManifest() throws IOException {
        Path path = Paths.get("updateReadStdinWriteStdoutNoManifest.jar");

        for (String opts : new String[]{"uM", "-uM", "--update --no-manifest"} ){
            if (legacyOnly && opts.startsWith("--"))
                continue;

            createJar(path, RES1);
            jarWithStdin(path.toFile(), opts, RES2)
                .assertSuccess()
                .resultChecker(r -> {
                    ASSERT_CONTAINS_RES1.accept(r.stdoutAsStream());
                    ASSERT_CONTAINS_RES2.accept(r.stdoutAsStream());
                    ASSERT_DOES_NOT_CONTAIN_MAINFEST.accept(r.stdoutAsStream());
                });
        }
        FileUtils.deleteFileIfExistsWithRetry(path);
    }

    // List

    @Test
    public void listBadArgs() {
        jar("tx")
            .assertFailure()
            .resultChecker(FAIL_TOO_MANY_MAIN_OPS);

        jar("-tx")
            .assertFailure()
            .resultChecker(FAIL_TOO_MANY_MAIN_OPS);

        if (!legacyOnly)
            jar("--list --extract")
                .assertFailure()
                .resultChecker(FAIL_TOO_MANY_MAIN_OPS);
    }

    @Test
    public void listReadFromFileWriteToStdout() throws IOException {
        Path path = Paths.get("listReadFromFileWriteToStdout.jar");  // for listing
        createJar(path, RES1);
        String jn = path.toString();

        for (String opts : new String[]{"tf " + jn, "-tf " + jn, "--list --file " + jn}) {
            if (legacyOnly && opts.startsWith("--"))
                continue;

            jar(opts)
                .assertSuccess()
                .resultChecker(r ->
                    assertTrue(r.output.contains("META-INF/MANIFEST.MF") && r.output.contains(RES1),
                               "Failed, got [" + r.output + "]")
                );
        }
        FileUtils.deleteFileIfExistsWithRetry(path);
    }

    @Test
    public void listReadFromStdinWriteToStdout() throws IOException {
        Path path = Paths.get("listReadFromStdinWriteToStdout.jar");
        createJar(path, RES1);

        for (String opts : new String[]{"t", "-t", "--list"} ){
            if (legacyOnly && opts.startsWith("--"))
                continue;

            jarWithStdin(path.toFile(), opts)
                .assertSuccess()
                .resultChecker(r ->
                    assertTrue(r.output.contains("META-INF/MANIFEST.MF") && r.output.contains(RES1),
                               "Failed, got [" + r.output + "]")
                );
        }
        FileUtils.deleteFileIfExistsWithRetry(path);
    }

    // Extract

    @Test
    public void extractBadArgs() {
        jar("xi")
            .assertFailure()
            .resultChecker(FAIL_TOO_MANY_MAIN_OPS);

        jar("-xi")
            .assertFailure()
            .resultChecker(FAIL_TOO_MANY_MAIN_OPS);

        if (!legacyOnly) {
            jar("--extract --generate-index")
                .assertFailure()
                .resultChecker(new FailCheckerWithMessage(
                                   "option --generate-index requires an argument"));

            jar("--extract --generate-index=foo")
                .assertFailure()
                .resultChecker(FAIL_TOO_MANY_MAIN_OPS);
        }
    }

    @Test
    public void extractReadFromStdin() throws IOException {
        Path path = Paths.get("extract");
        Path jarPath = path.resolve("extractReadFromStdin.jar"); // for extracting
        createJar(jarPath, RES1);

        for (String opts : new String[]{"x" ,"-x", "--extract"}) {
            if (legacyOnly && opts.startsWith("--"))
                continue;

            jarWithStdinAndWorkingDir(jarPath.toFile(), path.toFile(), opts)
                .assertSuccess()
                .resultChecker(r ->
                    assertTrue(Files.exists(path.resolve(RES1)),
                               "Expected to find:" + path.resolve(RES1))
                );
            FileUtils.deleteFileIfExistsWithRetry(path.resolve(RES1));
        }
        FileUtils.deleteFileTreeWithRetry(path);
    }

    @Test
    public void extractReadFromFile() throws IOException {
        Path path = Paths.get("extract");
        String jn = "extractReadFromFile.jar";
        Path jarPath = path.resolve(jn);
        createJar(jarPath, RES1);

        for (String opts : new String[]{"xf "+jn ,"-xf "+jn, "--extract --file "+jn}) {
            if (legacyOnly && opts.startsWith("--"))
                continue;

            jarWithStdinAndWorkingDir(null, path.toFile(), opts)
                .assertSuccess()
                .resultChecker(r ->
                    assertTrue(Files.exists(path.resolve(RES1)),
                               "Expected to find:" + path.resolve(RES1))
                );
            FileUtils.deleteFileIfExistsWithRetry(path.resolve(RES1));
        }
        FileUtils.deleteFileTreeWithRetry(path);
    }

    // Basic help

    @Test
    public void helpBadOptionalArg() {
        if (legacyOnly)
            return;

        jar("--help:")
            .assertFailure();

        jar("--help:blah")
            .assertFailure();
    }

    @Test
    public void help() {
        if (legacyOnly)
            return;

        jar("-h")
            .assertSuccess()
            .resultChecker(r ->
                assertTrue(r.output.startsWith("Usage: jar [OPTION...] [ [--release VERSION] [-C dir] files]"),
                           "Failed, got [" + r.output + "]")
            );

        jar("--help")
            .assertSuccess()
            .resultChecker(r -> {
                assertTrue(r.output.startsWith("Usage: jar [OPTION...] [ [--release VERSION] [-C dir] files]"),
                           "Failed, got [" + r.output + "]");
                assertFalse(r.output.contains("--do-not-resolve-by-default"));
                assertFalse(r.output.contains("--warn-if-resolved"));
            });

        jar("--help:compat")
            .assertSuccess()
            .resultChecker(r ->
                assertTrue(r.output.startsWith("Compatibility Interface:"),
                           "Failed, got [" + r.output + "]")
            );

        jar("--help-extra")
            .assertSuccess()
            .resultChecker(r -> {
                assertTrue(r.output.startsWith("Usage: jar [OPTION...] [ [--release VERSION] [-C dir] files]"),
                           "Failed, got [" + r.output + "]");
                assertTrue(r.output.contains("--do-not-resolve-by-default"));
                assertTrue(r.output.contains("--warn-if-resolved"));
            });
    }

    // -- Infrastructure

    static boolean jarContains(JarInputStream jis, String entryName)
        throws IOException
    {
        JarEntry e;
        boolean found = false;
        while((e = jis.getNextJarEntry()) != null) {
            if (e.getName().equals(entryName))
                return true;
        }
        return false;
    }

    /* Creates a simple jar with entries of size 0, good enough for testing */
    static void createJar(Path path, String... entries) throws IOException {
        FileUtils.deleteFileIfExistsWithRetry(path);
        Path parent = path.getParent();
        if (parent != null)
            Files.createDirectories(parent);
        try (OutputStream out = Files.newOutputStream(path);
             JarOutputStream jos = new JarOutputStream(out)) {
            JarEntry je = new JarEntry("META-INF/MANIFEST.MF");
            jos.putNextEntry(je);
            jos.closeEntry();

            for (String entry : entries) {
                je = new JarEntry(entry);
                jos.putNextEntry(je);
                jos.closeEntry();
            }
        }
    }

    static class FailCheckerWithMessage implements Consumer<Result> {
        final String[] messages;
        FailCheckerWithMessage(String... m) {
            messages = m;
        }
        @Override
        public void accept(Result r) {
            //out.printf("%s%n", r.output);
            boolean found = false;
            for (String m : messages) {
                if (r.output.contains(m)) {
                    found = true;
                    break;
                }
            }
            assertTrue(found,
                       "Excepted out to contain one of: " + Arrays.asList(messages)
                           + " but got: " + r.output);
        }
    }

    static Result jar(String... args) {
        return jarWithStdinAndWorkingDir(null, null, args);
    }

    static Result jarWithStdin(File stdinSource, String... args) {
        return jarWithStdinAndWorkingDir(stdinSource, null, args);
    }

    static Result jarWithStdinAndWorkingDir(File stdinFrom,
                                            File workingDir,
                                            String... args) {
        String jar = getJDKTool("jar");
        List<String> commands = new ArrayList<>();
        commands.add(jar);
        if (!TOOL_VM_OPTIONS.isEmpty()) {
            commands.addAll(Arrays.asList(TOOL_VM_OPTIONS.split("\\s+", -1)));
        }
        Stream.of(args).map(s -> s.split(" "))
                       .flatMap(Arrays::stream)
                       .forEach(x -> commands.add(x));
        ProcessBuilder p = new ProcessBuilder(commands);
        if (stdinFrom != null)
            p.redirectInput(stdinFrom);
        if (workingDir != null)
            p.directory(workingDir);
        return run(p);
    }

    static Result run(ProcessBuilder pb) {
        Process p;
        byte[] stdout, stderr;
        out.printf("Running: %s%n", pb.command());
        try {
            p = pb.start();
        } catch (IOException e) {
            throw new RuntimeException(
                    format("Couldn't start process '%s'", pb.command()), e);
        }

        String output;
        try {
            stdout = readAllBytes(p.getInputStream());
            stderr = readAllBytes(p.getErrorStream());

            output = toString(stdout, stderr);
        } catch (IOException e) {
            throw new RuntimeException(
                    format("Couldn't read process output '%s'", pb.command()), e);
        }

        try {
            p.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(
                    format("Process hasn't finished '%s'", pb.command()), e);
        }
        return new Result(p.exitValue(), stdout, stderr, output);
    }

    static final Path JAVA_HOME = Paths.get(System.getProperty("java.home"));

    static String getJDKTool(String name) {
        try {
            return JDKToolFinder.getJDKTool(name);
        } catch (Exception x) {
            Path j = JAVA_HOME.resolve("bin").resolve(name);
            if (Files.exists(j))
                return j.toString();
            j = JAVA_HOME.resolve("..").resolve("bin").resolve(name);
            if (Files.exists(j))
                return j.toString();
            throw new RuntimeException(x);
        }
    }

    static String toString(byte[] ba1, byte[] ba2) {
        return (new String(ba1, UTF_8)).concat(new String(ba2, UTF_8));
    }

    static class Result {
        final int exitValue;
        final byte[] stdout;
        final byte[] stderr;
        final String output;

        private Result(int exitValue, byte[] stdout, byte[] stderr, String output) {
            this.exitValue = exitValue;
            this.stdout = stdout;
            this.stderr = stderr;
            this.output = output;
        }

        InputStream stdoutAsStream() { return new ByteArrayInputStream(stdout); }

        Result assertSuccess() { assertTrue(exitValue == 0, output); return this; }
        Result assertFailure() { assertTrue(exitValue != 0, output); return this; }

        Result resultChecker(IOConsumer<Result> r) {
            try {  r.accept(this); return this; }
            catch (IOException x) { throw new UncheckedIOException(x); }
        }

        Result resultChecker(FailCheckerWithMessage c) { c.accept(this); return this; }
    }

    interface IOConsumer<T> { void accept(T t) throws IOException ;  }

    // readAllBytes implementation so the test can be run pre 1.9 ( legacyOnly )
    static byte[] readAllBytes(InputStream is) throws IOException {
        byte[] buf = new byte[8192];
        int capacity = buf.length;
        int nread = 0;
        int n;
        for (;;) {
            // read to EOF which may read more or less than initial buffer size
            while ((n = is.read(buf, nread, capacity - nread)) > 0)
                nread += n;

            // if the last call to read returned -1, then we're done
            if (n < 0)
                break;

            // need to allocate a larger buffer
            capacity = capacity << 1;

            buf = Arrays.copyOf(buf, capacity);
        }
        return (capacity == nread) ? buf : Arrays.copyOf(buf, nread);
    }

    // Standalone entry point for running with, possibly older, JDKs.
    public static void main(String[] args) throws Throwable {
        boolean legacyOnly = false;
        if (args.length != 0 && args[0].equals("legacyOnly"))
            legacyOnly = true;

        CLICompatibility test = new CLICompatibility(legacyOnly);
        for (Method m : CLICompatibility.class.getDeclaredMethods()) {
            if (m.getAnnotation(Test.class) != null) {
                System.out.println("Invoking " + m.getName());
                m.invoke(test);
            }
        }
    }
    CLICompatibility(boolean legacyOnly) { this.legacyOnly = legacyOnly; }
    CLICompatibility() { this.legacyOnly = false; }
}
