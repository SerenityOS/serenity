/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import javax.naming.Context;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;

/*
 * @test
 * @bug 8044627
 * @summary Examines different ways JNDI providers can hook up themselves and
 *          become available. Each case mimics the most straightforward way of
 *          executing scenarios.
 */
public class InitialContextTest {

    public static void main(String[] args) throws Throwable {
        unknownInitialContextFactory();
        initialContextFactoryInAJar();
        initialContextFactoryAsService();
    }

    private static void unknownInitialContextFactory() throws Throwable {

        // This is a parameter of this test case, it should work for any value
        // of it, provided a class with this FQN is not available in a runtime.
        // So pick any name you like.
        String factoryClassFqn =
                "net.java.openjdk.test.UnknownInitialContextFactory";

        Path tmp = Files.createDirectory(Paths.get("InitialContextTest-1"));

        Path src = templatesHome().resolve("test.template");
        Path dst = tmp.resolve("Test.java");
        Files.copy(src, dst);

        Path build = Files.createDirectory(tmp.resolve("build"));

        javac(build, dst);

        Map<String, String> props
                = singletonMap(Context.INITIAL_CONTEXT_FACTORY, factoryClassFqn);

        Result r = java(props, singleton(build), "Test");

        if (r.exitValue == 0 || !r.output.startsWith(
                stackTraceStringForClassNotFound(factoryClassFqn))) {
            throw new RuntimeException(
                    "Expected a different kind of failure: " + r.output);
        }
    }

    private static String stackTraceStringForClassNotFound(String fqn) {
        return String.format(
                "Exception in thread \"main\" javax.naming.NoInitialContextException: "
                        + "Cannot instantiate class: %s "
                        + "[Root exception is java.lang.ClassNotFoundException: %s]",
                fqn, fqn);
    }

    private static void initialContextFactoryInAJar() throws Throwable {

        String factoryClassFqn =
                "net.java.openjdk.test.DummyInitialContextFactory";

        Path tmp = Files.createDirectory(Paths.get("InitialContextTest-2"));

        Path src = templatesHome().resolve("test.template");
        Path dst = tmp.resolve("Test.java");
        Files.copy(src, dst);

        Path dst1 = createFactoryFrom(templatesHome().resolve("factory.template"),
                factoryClassFqn, tmp);

        Path build = Files.createDirectory(tmp.resolve("build"));

        javac(build, dst);
        Path explodedJar = Files.createDirectory(tmp.resolve("exploded-jar"));
        javac(explodedJar, dst1);
        jar(tmp.resolve("test.jar"), explodedJar);

        Files.copy(tmp.resolve("test.jar"), build.resolve("test.jar"));

        Map<String, String> props
                = singletonMap(Context.INITIAL_CONTEXT_FACTORY, factoryClassFqn);

        Result r = java(props, asList(build.resolve("test.jar"), build), "Test");

        if (r.exitValue != 0 || !r.output.isEmpty())
            throw new RuntimeException(r.output);
    }


    private static Path createFactoryFrom(Path srcTemplate,
                                          String factoryFqn,
                                          Path dstFolder) throws IOException {

        String factorySimpleName, packageName;
        int i = factoryFqn.lastIndexOf('.');
        if (i < 0) {
            packageName = "";
            factorySimpleName = factoryFqn;
        } else {
            packageName = factoryFqn.substring(0, i);
            factorySimpleName = factoryFqn.substring(i + 1);
        }

        Path result = dstFolder.resolve(factorySimpleName + ".java");
        File dst = result.toFile();
        File src = srcTemplate.toFile();
        try (BufferedReader r = new BufferedReader(new FileReader(src));
             BufferedWriter w = new BufferedWriter(new FileWriter(dst))) {

            List<String> lines = processTemplate(packageName, factorySimpleName,
                    r.lines()).collect(Collectors.toList());

            Iterator<String> it = lines.iterator();
            if (it.hasNext())
                w.write(it.next());
            while (it.hasNext()) {
                w.newLine();
                w.write(it.next());
            }
        }
        return result;
    }

    private static Stream<String> processTemplate(String packageName,
                                                  String factorySimpleName,
                                                  Stream<String> lines) {
        Function<String, String> pckg;

        if (packageName.isEmpty()) {
            pckg = s -> s.contains("$package") ? "" : s;
        } else {
            pckg = s -> s.replaceAll("\\$package", packageName);
        }

        Function<String, String> factory
                = s -> s.replaceAll("\\$factoryName", factorySimpleName);

        return lines.map(pckg).map(factory);
    }

    private static void initialContextFactoryAsService() throws Throwable {

        String factoryClassFqn =
                "net.java.openjdk.test.BrokenInitialContextFactory";

        Path tmp = Files.createDirectory(Paths.get("InitialContextTest-3"));

        Path src = templatesHome().resolve("test.template");
        Path dst = tmp.resolve("Test.java");
        Files.copy(src, dst);

        Path dst1 = createFactoryFrom(templatesHome().resolve("broken_factory.template"),
                factoryClassFqn, tmp);

        Path build = Files.createDirectory(tmp.resolve("build"));

        javac(build, dst);

        Path explodedJar = Files.createDirectory(tmp.resolve("exploded-jar"));
        Path services = Files.createDirectories(explodedJar.resolve("META-INF")
                .resolve("services"));

        Path s = services.resolve("javax.naming.spi.InitialContextFactory");
        FileWriter fw = new FileWriter(s.toFile());
        try {
            fw.write(factoryClassFqn);
        } finally {
            fw.close();
        }

        javac(explodedJar, dst1);
        jar(tmp.resolve("test.jar"), explodedJar);

        Files.copy(tmp.resolve("test.jar"), build.resolve("test.jar"));

        Map<String, String> props
                = singletonMap(Context.INITIAL_CONTEXT_FACTORY, factoryClassFqn);

        Result r = java(props, asList(build.resolve("test.jar"), build), "Test");

        if (r.exitValue == 0 || !verifyOutput(r.output, factoryClassFqn))
            throw new RuntimeException(r.output);
    }

    // IMO, that's the easiest way that gives you a fair amount of confidence in
    // that j.u.ServiceLoader is loading a factory rather than Class.forName
    private static boolean verifyOutput(String output, String fqn) {
        String s1 = String.format(
                "Exception in thread \"main\" javax.naming.NoInitialContextException: "
                        + "Cannot load initial context factory '%s' "
                        + "[Root exception is java.util.ServiceConfigurationError: "
                        + "javax.naming.spi.InitialContextFactory: "
                        + "Provider %s could not be instantiated]", fqn, fqn);

        String s2 = String.format("Caused by: java.util.ServiceConfigurationError: "
                + "javax.naming.spi.InitialContextFactory: "
                + "Provider %s could not be instantiated", fqn);

        String s3 = "Caused by: java.lang.RuntimeException: "
                + "This is a broken factory. It is supposed to throw this exception.";

        return output.startsWith(s1) && output.contains(s2)
                && output.contains(s1);
    }

    private static void jar(Path jarName, Path jarRoot) {
        String jar = getJDKTool("jar");
        ProcessBuilder p = new ProcessBuilder(jar, "cf", jarName.toString(),
                "-C", jarRoot.toString(), ".");
        quickFail(run(p));
    }

    private static void javac(Path compilationOutput, Path... sourceFiles) {
        String javac = getJDKTool("javac");
        List<String> commands = new ArrayList<>();
        commands.addAll(asList(javac, "-d", compilationOutput.toString()));
        List<Path> paths = asList(sourceFiles);
        commands.addAll(paths.stream()
                .map(Path::toString)
                .collect(Collectors.toList()));
        quickFail(run(new ProcessBuilder(commands)));
    }

    private static void quickFail(Result r) {
        if (r.exitValue != 0)
            throw new RuntimeException(r.output);
    }

    private static Result java(Map<String, String> properties,
                               Collection<Path> classpath,
                               String classname) {

        String java = getJDKTool("java");

        List<String> commands = new ArrayList<>();
        commands.add(java);
        commands.addAll(properties.entrySet()
                .stream()
                .map(e -> "-D" + e.getKey() + "=" + e.getValue())
                .collect(Collectors.toList()));

        String cp = classpath.stream()
                .map(Path::toString)
                .collect(Collectors.joining(File.pathSeparator));
        commands.add("-cp");
        commands.add(cp);
        commands.add(classname);

        return run(new ProcessBuilder(commands));
    }

    private static Result run(ProcessBuilder b) {
        Process p = null;
        try {
            p = b.start();
        } catch (IOException e) {
            throw new RuntimeException(
                    format("Couldn't start process '%s'", b.command()), e);
        }

        String output;
        try {
            output = toString(p.getInputStream(), p.getErrorStream());
        } catch (IOException e) {
            throw new RuntimeException(
                    format("Couldn't read process output '%s'", b.command()), e);
        }

        try {
            p.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(
                    format("Process hasn't finished '%s'", b.command()), e);
        }

        return new Result(p.exitValue(), output);
    }

    private static String getJDKTool(String name) {
        String testJdk = System.getProperty("test.jdk");
        if (testJdk == null)
            throw new RuntimeException("Please provide test.jdk property at a startup");
        return testJdk + File.separator + "bin" + File.separator + name;
    }

    private static Path templatesHome() {
        String testSrc = System.getProperty("test.src");
        if (testSrc == null)
            throw new RuntimeException("Please provide test.src property at a startup");
        return Paths.get(testSrc);
    }

    private static String toString(InputStream... src) throws IOException {
        StringWriter dst = new StringWriter();
        Reader concatenated =
                new InputStreamReader(
                        new SequenceInputStream(
                                Collections.enumeration(asList(src))));
        copy(concatenated, dst);
        return dst.toString();
    }

    private static void copy(Reader src, Writer dst) throws IOException {
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

    private static class Result {

        final int exitValue;
        final String output;

        private Result(int exitValue, String output) {
            this.exitValue = exitValue;
            this.output = output;
        }
    }
}
