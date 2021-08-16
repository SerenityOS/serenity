/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.FileWriter;
import java.io.Reader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.SequenceInputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import jdk.test.lib.util.FileUtils;
import jdk.test.lib.JDKToolFinder;
import static java.lang.String.format;
import static java.util.Arrays.asList;

/*
 * @test
 * @bug 8064924
 * @modules jdk.compiler
 * @summary Basic test for URLStreamHandlerProvider
 * @library /test/lib
 * @build jdk.test.lib.Platform
 *        jdk.test.lib.util.FileUtils
 *        jdk.test.lib.JDKToolFinder
 * @compile Basic.java Child.java
 * @run main Basic
 */

public class Basic {

    static final Path TEST_SRC = Paths.get(System.getProperty("test.src", "."));
    static final Path TEST_CLASSES = Paths.get(System.getProperty("test.classes", "."));

    public static void main(String[] args) throws Throwable {
        unknownProtocol("foo", UNKNOWN);
        unknownProtocol("bar", UNKNOWN);
        viaProvider("baz", KNOWN);
        viaProvider("bert", KNOWN);
        viaProvider("ernie", UNKNOWN, "-Djava.security.manager");
        viaProvider("curly", UNKNOWN, "-Djava.security.manager");
        viaProvider("larry", KNOWN, "-Djava.security.manager",
                "-Djava.security.policy=" + TEST_SRC + File.separator + "basic.policy");
        viaProvider("moe", KNOWN, "-Djava.security.manager",
                "-Djava.security.policy=" + TEST_SRC + File.separator + "basic.policy");
        viaBadProvider("tom", SCE);
        viaBadProvider("jerry", SCE);
    }

    static final String SECURITY_MANAGER_DEPRECATED
            = "WARNING: The Security Manager is deprecated and will be removed in a future release."
                    + System.getProperty("line.separator");

    private static String withoutWarning(String in) {
        return in.lines().filter(s -> !s.startsWith("WARNING:")).collect(Collectors.joining());
    }

    static final Consumer<Result> KNOWN = r -> {
        if (r.exitValue != 0 || !withoutWarning(r.output).isEmpty())
            throw new RuntimeException("[" + r.output + "]");
    };
    static final Consumer<Result> UNKNOWN = r -> {
        if (r.exitValue == 0 ||
            !r.output.contains("java.net.MalformedURLException: unknown protocol")) {
            throw new RuntimeException("exitValue: "+ r.exitValue + ", output:[" +r.output +"]");
        }
    };
    static final Consumer<Result> SCE = r -> {
        if (r.exitValue == 0 ||
            !r.output.contains("java.util.ServiceConfigurationError")) {
            throw new RuntimeException("exitValue: "+ r.exitValue + ", output:[" +r.output +"]");
        }
    };

    static void unknownProtocol(String protocol, Consumer<Result> resultChecker) {
        System.out.println("\nTesting " + protocol);
        Result r = java(Collections.emptyList(), asList(TEST_CLASSES),
                "Child", protocol);
        resultChecker.accept(r);
    }

    static void viaProvider(String protocol, Consumer<Result> resultChecker,
                            String... sysProps)
        throws Exception
    {
        viaProviderWithTemplate(protocol, resultChecker,
                                TEST_SRC.resolve("provider.template"),
                                sysProps);
    }

    static void viaBadProvider(String protocol, Consumer<Result> resultChecker,
                               String... sysProps)
        throws Exception
    {
        viaProviderWithTemplate(protocol, resultChecker,
                                TEST_SRC.resolve("bad.provider.template"),
                                sysProps);
    }

    static void viaProviderWithTemplate(String protocol,
                                        Consumer<Result> resultChecker,
                                        Path template, String... sysProps)
        throws Exception
    {
        System.out.println("\nTesting " + protocol);
        Path testRoot = Paths.get("URLStreamHandlerProvider-" + protocol);
        if (Files.exists(testRoot))
            FileUtils.deleteFileTreeWithRetry(testRoot);
        Files.createDirectory(testRoot);

        Path srcPath = Files.createDirectory(testRoot.resolve("src"));
        Path srcClass = createProvider(protocol, template, srcPath);

        Path build = Files.createDirectory(testRoot.resolve("build"));
        javac(build, srcClass);
        createServices(build, protocol);
        Path testJar = testRoot.resolve("test.jar");
        jar(testJar, build);

        List<String> props = new ArrayList<>();
        for (String p : sysProps)
            props.add(p);

        Result r = java(props, asList(testJar, TEST_CLASSES),
                        "Child", protocol);

        resultChecker.accept(r);
    }

    static String platformPath(String p) { return p.replace("/", File.separator); }
    static String binaryName(String name) { return name.replace(".", "/"); }

    static final String SERVICE_IMPL_PREFIX = "net.java.openjdk.test";

    static void createServices(Path dst, String protocol) throws IOException {
        Path services = Files.createDirectories(dst.resolve("META-INF")
                                                   .resolve("services"));

        final String implName =  SERVICE_IMPL_PREFIX + "." + protocol + ".Provider";
        Path s = services.resolve("java.net.spi.URLStreamHandlerProvider");
        FileWriter fw = new FileWriter(s.toFile());
        try {
            fw.write(implName);
        } finally {
            fw.close();
        }
    }

    static Path createProvider(String protocol, Path srcTemplate, Path dst)
        throws IOException
    {
        String pkg = SERVICE_IMPL_PREFIX + "." + protocol;
        Path classDst = dst.resolve(platformPath(binaryName(pkg)));
        Files.createDirectories(classDst);
        Path classPath = classDst.resolve("Provider.java");

        List<String> lines = Files.lines(srcTemplate)
                                  .map(s -> s.replaceAll("\\$package", pkg))
                                  .map(s -> s.replaceAll("\\$protocol", protocol))
                                  .collect(Collectors.toList());
        Files.write(classPath, lines);

        return classPath;
    }

    static void jar(Path jarName, Path jarRoot) { String jar = getJDKTool("jar");
        ProcessBuilder p = new ProcessBuilder(jar, "cf", jarName.toString(),
                "-C", jarRoot.toString(), ".");
        quickFail(run(p));
    }

    static void javac(Path dest, Path... sourceFiles) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fileManager =
                    compiler.getStandardFileManager(null, null, null)) {

            List<File> files = Stream.of(sourceFiles)
                    .map(p -> p.toFile())
                    .collect(Collectors.toList());
            List<File> dests = Stream.of(dest)
                    .map(p -> p.toFile())
                    .collect(Collectors.toList());
            Iterable<? extends JavaFileObject> compilationUnits =
                    fileManager.getJavaFileObjectsFromFiles(files);
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, dests);
            JavaCompiler.CompilationTask task =
                    compiler.getTask(null, fileManager, null, null, null, compilationUnits);
            boolean passed = task.call();
            if (!passed)
                throw new RuntimeException("Error compiling " + files);
        }
    }

    static void quickFail(Result r) {
        if (r.exitValue != 0)
            throw new RuntimeException(r.output);
    }

    static Result java(List<String> sysProps, Collection<Path> classpath,
                       String classname, String arg) {
        String java = getJDKTool("java");

        List<String> commands = new ArrayList<>();
        commands.add(java);
        for (String prop : sysProps)
            commands.add(prop);

        String cp = classpath.stream()
                .map(Path::toString)
                .collect(Collectors.joining(File.pathSeparator));
        commands.add("-cp");
        commands.add(cp);
        commands.add(classname);
        commands.add(arg);

        return run(new ProcessBuilder(commands));
    }

    static Result run(ProcessBuilder pb) {
        Process p = null;
        System.out.println("running: " + pb.command());
        try {
            p = pb.start();
        } catch (IOException e) {
            throw new RuntimeException(
                    format("Couldn't start process '%s'", pb.command()), e);
        }

        String output;
        try {
            output = toString(p.getInputStream(), p.getErrorStream());
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

        return new Result(p.exitValue(), output);
    }

    static final String DEFAULT_IMAGE_BIN = System.getProperty("java.home")
            + File.separator + "bin" + File.separator;

    static String getJDKTool(String name) {
        try {
            return JDKToolFinder.getJDKTool(name);
        } catch (Exception x) {
            return DEFAULT_IMAGE_BIN + name;
        }
    }

    static String toString(InputStream... src) throws IOException {
        StringWriter dst = new StringWriter();
        Reader concatenated =
                new InputStreamReader(
                        new SequenceInputStream(
                                Collections.enumeration(asList(src))));
        copy(concatenated, dst);
        return dst.toString();
    }

    static void copy(Reader src, Writer dst) throws IOException {
        int len;
        char[] buf = new char[1024];
        try {
            while ((len = src.read(buf)) != -1)
                dst.write(buf, 0, len);
        } finally {
            try {
                src.close();
            } catch (IOException ignored1) {
            } finally {
                try {
                    dst.close();
                } catch (IOException ignored2) {
                }
            }
        }
    }

    static class Result {
        final int exitValue;
        final String output;

        private Result(int exitValue, String output) {
            this.exitValue = exitValue;
            this.output = output;
        }
    }
}
