/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8146486 8172432
 * @summary Fail to create a MR modular JAR with a versioned entry in
 *          base-versioned empty package
 * @modules java.base/jdk.internal.module
 *          jdk.compiler
 *          jdk.jartool
 * @library /test/lib
 * @build jdk.test.lib.Platform
 *        jdk.test.lib.util.FileUtils
 * @run testng Basic
 */

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

import jdk.internal.module.ModuleInfoExtender;
import jdk.test.lib.util.FileUtils;

public class Basic {
    private static final ToolProvider JAR_TOOL = ToolProvider.findFirst("jar")
           .orElseThrow(() -> new RuntimeException("jar tool not found"));
    private static final ToolProvider JAVAC_TOOL = ToolProvider.findFirst("javac")
            .orElseThrow(() -> new RuntimeException("javac tool not found"));
    private final String linesep = System.lineSeparator();
    private final Path testsrc;
    private final Path userdir;
    private final ByteArrayOutputStream outbytes = new ByteArrayOutputStream();
    private final PrintStream out = new PrintStream(outbytes, true);
    private final ByteArrayOutputStream errbytes = new ByteArrayOutputStream();
    private final PrintStream err = new PrintStream(errbytes, true);

    public Basic() throws IOException {
        testsrc = Paths.get(System.getProperty("test.src"));
        userdir = Paths.get(System.getProperty("user.dir", "."));

        // compile the classes directory
        Path source = testsrc.resolve("src").resolve("classes");
        Path destination = Paths.get("classes");
        javac(source, destination);

        // compile the mr9 directory including module-info.java
        source = testsrc.resolve("src").resolve("mr9");
        destination = Paths.get("mr9");
        javac(source, destination);

        // move module-info.class for later use
        Files.move(destination.resolve("module-info.class"),
                Paths.get("module-info.class"));
    }

    private void javac(Path source, Path destination) throws IOException {
        String[] args = Stream.concat(
                Stream.of("-d", destination.toString()),
                Files.walk(source)
                        .map(Path::toString)
                        .filter(s -> s.endsWith(".java"))
        ).toArray(String[]::new);
        JAVAC_TOOL.run(System.out, System.err, args);
    }

    private int jar(String cmd) {
        outbytes.reset();
        errbytes.reset();
        return JAR_TOOL.run(out, err, cmd.split(" +"));
    }

    @AfterClass
    public void cleanup() throws IOException {
        Files.walk(userdir, 1)
                .filter(p -> !p.equals(userdir))
                .forEach(p -> {
                    try {
                        if (Files.isDirectory(p)) {
                            FileUtils.deleteFileTreeWithRetry(p);
                        } else {
                            FileUtils.deleteFileIfExistsWithRetry(p);
                        }
                    } catch (IOException x) {
                        throw new UncheckedIOException(x);
                    }
                });
    }

    // updates a valid multi-release jar with a new public class in
    // versioned section and fails
    @Test
    public void test1() {
        // successful build of multi-release jar
        int rc = jar("-cf mmr.jar -C classes . --release 9 -C mr9 p/Hi.class");
        Assert.assertEquals(rc, 0);

        jar("-tf mmr.jar");

        Set<String> actual = lines(outbytes);
        Set<String> expected = Set.of(
                "META-INF/",
                "META-INF/MANIFEST.MF",
                "p/",
                "p/Hi.class",
                "META-INF/versions/9/p/Hi.class"
        );
        Assert.assertEquals(actual, expected);

        // failed build because of new public class
        rc = jar("-uf mmr.jar --release 9 -C mr9 p/internal/Bar.class");
        Assert.assertEquals(rc, 1);

        String s = new String(errbytes.toByteArray());
        Assert.assertTrue(Message.NOT_FOUND_IN_BASE_ENTRY.match(s, "p/internal/Bar.class"));
    }

    // updates a valid multi-release jar with a module-info class and new
    // concealed public class in versioned section and succeeds
    @Test
    public void test2() {
        // successful build of multi-release jar
        int rc = jar("-cf mmr.jar -C classes . --release 9 -C mr9 p/Hi.class");
        Assert.assertEquals(rc, 0);

        // successful build because of module-info and new public class
        rc = jar("-uf mmr.jar module-info.class --release 9 -C mr9 p/internal/Bar.class");
        Assert.assertEquals(rc, 0);

        String s = new String(errbytes.toByteArray());
        Assert.assertTrue(Message.NEW_CONCEALED_PACKAGE_WARNING.match(s, "p/internal/Bar.class"));

        jar("-tf mmr.jar");

        Set<String> actual = lines(outbytes);
        Set<String> expected = Set.of(
                "META-INF/",
                "META-INF/MANIFEST.MF",
                "p/",
                "p/Hi.class",
                "META-INF/versions/9/p/Hi.class",
                "META-INF/versions/9/p/internal/Bar.class",
                "module-info.class"
        );
        Assert.assertEquals(actual, expected);
    }

    // jar tool fails building mmr.jar because of new public class
    @Test
    public void test3() {
        int rc = jar("-cf mmr.jar -C classes . --release 9 -C mr9 .");
        Assert.assertEquals(rc, 1);

        String s = new String(errbytes.toByteArray());
        Assert.assertTrue(Message.NOT_FOUND_IN_BASE_ENTRY.match(s, "p/internal/Bar.class"));
    }

    // jar tool succeeds building mmr.jar because of concealed package
    @Test
    public void test4() {
        int rc = jar("-cf mmr.jar module-info.class -C classes . " +
                "--release 9 module-info.class -C mr9 .");
        Assert.assertEquals(rc, 0);

        String s = new String(errbytes.toByteArray());
        Assert.assertTrue(Message.NEW_CONCEALED_PACKAGE_WARNING.match(s, "p/internal/Bar.class"));

        jar("-tf mmr.jar");

        Set<String> actual = lines(outbytes);
        Set<String> expected = Set.of(
                "META-INF/",
                "META-INF/MANIFEST.MF",
                "module-info.class",
                "META-INF/versions/9/module-info.class",
                "p/",
                "p/Hi.class",
                "META-INF/versions/9/",
                "META-INF/versions/9/p/",
                "META-INF/versions/9/p/Hi.class",
                "META-INF/versions/9/p/internal/",
                "META-INF/versions/9/p/internal/Bar.class"
        );
        Assert.assertEquals(actual, expected);
    }

    // jar tool does two updates, no exported packages, all concealed.
    // Along with various --describe-module variants
    @Test
    public void test5() throws IOException {
        // compile the mr10 directory
        Path source = testsrc.resolve("src").resolve("mr10");
        Path destination = Paths.get("mr10");
        javac(source, destination);

        // create a directory for this tests special files
        Files.createDirectory(Paths.get("test5"));

        // create an empty module-info.java
        String hi = "module hi {" + linesep + "}" + linesep;
        Path modinfo = Paths.get("test5", "module-info.java");
        Files.write(modinfo, hi.getBytes());

        // and compile it
        javac(modinfo, Paths.get("test5"));

        int rc = jar("--create --file mr.jar -C classes .");
        Assert.assertEquals(rc, 0);

        rc = jar("--update --file mr.jar -C test5 module-info.class"
                + " --release 9 -C mr9 .");
        Assert.assertEquals(rc, 0);

        jar("tf mr.jar");

        Set<String> actual = lines(outbytes);
        Set<String> expected = Set.of(
                "META-INF/",
                "META-INF/MANIFEST.MF",
                "p/",
                "p/Hi.class",
                "META-INF/versions/9/",
                "META-INF/versions/9/p/",
                "META-INF/versions/9/p/Hi.class",
                "META-INF/versions/9/p/internal/",
                "META-INF/versions/9/p/internal/Bar.class",
                "module-info.class"
        );
        Assert.assertEquals(actual, expected);

        jar("-d --file mr.jar");

        String uri = (Paths.get("mr.jar")).toUri().toString();
        uri = "jar:" + uri + "!/module-info.class";

        actual = lines(outbytes);
        expected = Set.of(
                "hi " + uri,
                "requires java.base mandated",
                "contains p",
                "contains p.internal"
        );
        Assert.assertEquals(actual, expected);

        rc = jar("--update --file mr.jar --release 10 -C mr10 .");
        Assert.assertEquals(rc, 0);

        jar("tf mr.jar");

        actual = lines(outbytes);
        expected = Set.of(
                "META-INF/",
                "META-INF/MANIFEST.MF",
                "p/",
                "p/Hi.class",
                "META-INF/versions/9/",
                "META-INF/versions/9/p/",
                "META-INF/versions/9/p/Hi.class",
                "META-INF/versions/9/p/internal/",
                "META-INF/versions/9/p/internal/Bar.class",
                "META-INF/versions/10/",
                "META-INF/versions/10/p/",
                "META-INF/versions/10/p/internal/",
                "META-INF/versions/10/p/internal/bar/",
                "META-INF/versions/10/p/internal/bar/Gee.class",
                "module-info.class"
        );
        Assert.assertEquals(actual, expected);

        jar("-d --file mr.jar");

        actual = lines(outbytes);
        expected = Set.of(
                "hi " + uri,
                "requires java.base mandated",
                "contains p",
                "contains p.internal",
                "contains p.internal.bar"
        );
        Assert.assertEquals(actual, expected);

        for (String release : new String[] {"9" , "10", "100", "1000"}) {
            jar("-d --file mr.jar --release " + release);
            actual = lines(outbytes);
            Assert.assertEquals(actual, expected);
        }
    }

    // root and versioned module-info entries have different main-class, version
    // attributes
    @Test
    public void test6() throws IOException {
        // create a directory for this tests special files
        Files.createDirectory(Paths.get("test6"));
        Files.createDirectory(Paths.get("test6-v9"));

        // compile the classes directory
        Path src = testsrc.resolve("src").resolve("classes");
        Path dst = Paths.get("test6");
        javac(src, dst);

        byte[] mdBytes = Files.readAllBytes(Paths.get("module-info.class"));

        ModuleInfoExtender mie = ModuleInfoExtender.newExtender(
            new ByteArrayInputStream(mdBytes));

        mie.mainClass("p.Main");
        mie.version(Version.parse("1.0"));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        mie.write(baos);
        Files.write(Paths.get("test6", "module-info.class"), baos.toByteArray());
        Files.write(Paths.get("test6-v9", "module-info.class"), baos.toByteArray());

        int rc = jar("--create --file mmr.jar -C test6 . --release 9 -C test6-v9 .");
        Assert.assertEquals(rc, 0);


        // different main-class
        mie = ModuleInfoExtender.newExtender(new ByteArrayInputStream(mdBytes));
        mie.mainClass("p.Main2");
        mie.version(Version.parse("1.0"));
        baos.reset();
        mie.write(baos);
        Files.write(Paths.get("test6-v9", "module-info.class"), baos.toByteArray());

        rc = jar("--create --file mmr.jar -C test6 . --release 9 -C test6-v9 .");
        Assert.assertEquals(rc, 1);

        Assert.assertTrue(Message.CONTAINS_DIFFERENT_MAINCLASS.match(
            new String(errbytes.toByteArray()),
            "META-INF/versions/9/module-info.class"));

        // different version
        mie = ModuleInfoExtender.newExtender(new ByteArrayInputStream(mdBytes));
        mie.mainClass("p.Main");
        mie.version(Version.parse("2.0"));
        baos.reset();
        mie.write(baos);
        Files.write(Paths.get("test6-v9", "module-info.class"), baos.toByteArray());

        rc = jar("--create --file mmr.jar -C test6 . --release 9 -C test6-v9 .");
        Assert.assertEquals(rc, 1);

        Assert.assertTrue(Message.CONTAINS_DIFFERENT_VERSION.match(
            new String(errbytes.toByteArray()),
            "META-INF/versions/9/module-info.class"));

    }

    // versioned mmr without root module-info.class
    @Test
    public void test7() throws IOException {
        // create a directory for this tests special files
        Files.createDirectory(Paths.get("test7"));
        Files.createDirectory(Paths.get("test7-v9"));
        Files.createDirectory(Paths.get("test7-v10"));

        // compile the classes directory
        Path src = testsrc.resolve("src").resolve("classes");
        Path dst = Paths.get("test7");
        javac(src, dst);

        // move module-info.class to v9 later use
        Files.copy(Paths.get("module-info.class"),
                   Paths.get("test7-v9", "module-info.class"));

        Files.copy(Paths.get("test7-v9", "module-info.class"),
                   Paths.get("test7-v10", "module-info.class"));

        int rc = jar("--create --file mmr.jar --main-class=p.Main -C test7 . --release 9 -C test7-v9 . --release 10 -C test7-v10 .");
        Assert.assertEquals(rc, 0);

        jar("-d --file=mmr.jar");
        Set<String> actual = lines(outbytes);
        Set<String> expected = Set.of(
                "releases: 9 10",
                "No root module descriptor, specify --release"
        );
        Assert.assertEquals(actual, expected);

        String uriPrefix = "jar:" + (Paths.get("mmr.jar")).toUri().toString();

        jar("-d --file=mmr.jar --release 9");
        actual = lines(outbytes);
        expected = Set.of(
                "releases: 9 10",
                "m1 " + uriPrefix + "!/META-INF/versions/9/module-info.class",
                "requires java.base mandated",
                "exports p",
                "main-class p.Main"
        );
        Assert.assertEquals(actual, expected);

        jar("-d --file=mmr.jar --release 10");
        actual = lines(outbytes);
        expected = Set.of(
                "releases: 9 10",
                "m1 " + uriPrefix + "!/META-INF/versions/10/module-info.class",
                "requires java.base mandated",
                "exports p",
                "main-class p.Main"
        );
        Assert.assertEquals(actual, expected);

        for (String release : new String[] {"11", "12", "15", "100"}) {
            jar("-d --file mmr.jar --release " + release);
            actual = lines(outbytes);
            Assert.assertEquals(actual, expected);
        }

        Optional<String> exp = Optional.of("p.Main");
        try (ZipFile zf = new ZipFile("mmr.jar")) {
            Assert.assertTrue(zf.getEntry("module-info.class") == null);

            ModuleDescriptor md = ModuleDescriptor.read(
                zf.getInputStream(zf.getEntry("META-INF/versions/9/module-info.class")));
            Assert.assertEquals(md.mainClass(), exp);

            md = ModuleDescriptor.read(
                zf.getInputStream(zf.getEntry("META-INF/versions/10/module-info.class")));
            Assert.assertEquals(md.mainClass(), exp);
        }
    }

    private static Set<String> lines(ByteArrayOutputStream baos) {
        String s = new String(baos.toByteArray());
        return Arrays.stream(s.split("\\R"))
                     .map(l -> l.trim())
                     .filter(l -> l.length() > 0)
                     .collect(Collectors.toSet());
    }

    static enum Message {
        CONTAINS_DIFFERENT_MAINCLASS(
          ": module-info.class in a versioned directory contains different \"main-class\""
        ),
        CONTAINS_DIFFERENT_VERSION(
          ": module-info.class in a versioned directory contains different \"version\""
        ),
        NOT_FOUND_IN_BASE_ENTRY(
          ", contains a new public class not found in base entries"
        ),
        NEW_CONCEALED_PACKAGE_WARNING(
            " is a public class" +
            " in a concealed package, placing this jar on the class path will result" +
            " in incompatible public interfaces"
        );

        final String msg;
        Message(String msg) {
            this.msg = msg;
        }

        /*
         * Test if the given output contains this message ignoring the line break.
         */
        boolean match(String output, String entry) {
            System.out.println("Expected: " + entry + msg);
            System.out.println("Found: " + output);
            return Arrays.stream(output.split("\\R"))
                         .collect(Collectors.joining(" "))
                         .contains(entry + msg);
        }
    }
}
