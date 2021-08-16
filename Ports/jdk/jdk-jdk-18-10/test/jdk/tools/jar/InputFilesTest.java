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
 * @bug 8165944
 * @summary test several jar tool input file scenarios with variations on -C
 *          options with/without a --release option.  Some input files are
 *          duplicates that sometimes cause exceptions and other times do not,
 *          demonstrating identical behavior to JDK 8 jar tool.
 * @library /test/lib
 * @modules jdk.jartool
 * @build jdk.test.lib.Platform
 *        jdk.test.lib.util.FileUtils
 * @run testng InputFilesTest
 */

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;
import java.util.zip.ZipException;

import jdk.test.lib.util.FileUtils;

public class InputFilesTest {
    private static final ToolProvider JAR_TOOL = ToolProvider.findFirst("jar")
        .orElseThrow(() ->
            new RuntimeException("jar tool not found")
        );

    private final String nl = System.lineSeparator();
    private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    private final PrintStream out = new PrintStream(baos);
    private Runnable onCompletion;

    @BeforeMethod
    public void reset() {
        onCompletion = null;
    }

    @AfterMethod
    public void run() {
        if (onCompletion != null) {
            onCompletion.run();
        }
    }

    @Test
    public void test1() throws IOException {
        mkdir("test1 test2");
        touch("test1/testfile1 test2/testfile2");
        jar("cf test.jar -C test1 . -C test2 .");
        jar("tf test.jar");
        println();
        String output = "META-INF/" + nl +
                "META-INF/MANIFEST.MF" + nl +
                "testfile1" + nl +
                "testfile2" + nl;
        rm("test.jar test1 test2");
        Assert.assertEquals(baos.toByteArray(), output.getBytes());
    }

    @Test
    public void test2() throws IOException {
        mkdir("test1 test2 test3 test4");
        touch("test1/testfile1 test2/testfile2 test3/testfile3 test4/testfile4");
        jar("cf test.jar -C test1 . -C test2 . --release 9 -C test3 . -C test4 .");
        jar("tf test.jar");
        println();
        String output = "META-INF/" + nl +
                "META-INF/MANIFEST.MF" + nl +
                "testfile1" + nl +
                "testfile2" + nl +
                "META-INF/versions/9/" + nl +
                "META-INF/versions/9/testfile3" + nl +
                "META-INF/versions/9/testfile4" + nl;
        rm("test.jar test1 test2 test3 test4");
        Assert.assertEquals(baos.toByteArray(), output.getBytes());
    }

    @Test
    public void test3() throws IOException {
        touch("test");
        jar("cf test.jar test test");
        jar("tf test.jar");
        println();
        String output = "META-INF/" + nl +
                "META-INF/MANIFEST.MF" + nl +
                "test" + nl;
        rm("test.jar test");
        Assert.assertEquals(baos.toByteArray(), output.getBytes());
    }

    @Test
    public void test4() throws IOException {
        mkdir("a");
        touch("a/test");
        jar("cf test.jar -C a test -C a test");
        jar("tf test.jar");
        println();
        String output = "META-INF/" + nl +
                "META-INF/MANIFEST.MF" + nl +
                "test" + nl;
        rm("test.jar a");
        Assert.assertEquals(baos.toByteArray(), output.getBytes());
    }

    @Test(expectedExceptions = {ZipException.class})
    public void test5() throws IOException {
        mkdir("a");
        touch("test a/test");
        onCompletion = () -> rm("test a");
        jar("cf test.jar -C a test test");
    }

    @Test(expectedExceptions = {ZipException.class})
    public void test6() throws IOException {
        mkdir("test1 test2");
        touch("test1/a test2/a");
        onCompletion = () -> rm("test1 test2");
        jar("cf test.jar --release 9 -C test1 a -C test2 a");
    }

    private Stream<Path> mkpath(String... args) {
        return Arrays.stream(args).map(d -> Paths.get(".", d.split("/")));
    }

    private void mkdir(String cmdline) {
        System.out.println("mkdir -p " + cmdline);
        mkpath(cmdline.split(" +")).forEach(p -> {
            try {
                Files.createDirectories(p);
            } catch (IOException x) {
                throw new UncheckedIOException(x);
            }
        });
    }

    private void touch(String cmdline) {
        System.out.println("touch " + cmdline);
        mkpath(cmdline.split(" +")).forEach(p -> {
            try {
                Files.createFile(p);
            } catch (IOException x) {
                throw new UncheckedIOException(x);
            }
        });
    }

    private void rm(String cmdline) {
        System.out.println("rm -rf " + cmdline);
        mkpath(cmdline.split(" +")).forEach(p -> {
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

    private void jar(String cmdline) throws IOException {
        System.out.println("jar " + cmdline);
        baos.reset();

        // the run method catches IOExceptions, we need to expose them
        ByteArrayOutputStream baes = new ByteArrayOutputStream();
        PrintStream err = new PrintStream(baes);
        PrintStream saveErr = System.err;
        System.setErr(err);
        int rc = JAR_TOOL.run(out, err, cmdline.split(" +"));
        System.setErr(saveErr);
        if (rc != 0) {
            String s = baes.toString();
            if (s.startsWith("java.util.zip.ZipException: duplicate entry: ")) {
                throw new ZipException(s);
            }
            throw new IOException(s);
        }
    }

    private void println() throws IOException {
        System.out.println(new String(baos.toByteArray()));
    }
}
