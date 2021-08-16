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
 * @bug 8167237
 * @summary test that both old style command line options and new gnu style
 *          command line options work with the --release option whether or
 *          not the --release option is preceded by a file name.
 * @library /test/lib
 * @modules jdk.jartool/sun.tools.jar
 * @build jdk.test.lib.Platform
 *        jdk.test.lib.util.FileUtils
 * @run testng ReleaseBeforeFiles
 */

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Stream;

import jdk.test.lib.util.FileUtils;

public class ReleaseBeforeFiles {
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

    @Test  // passes before bug fix
    public void test1() throws IOException {
        mkdir("test1");
        touch("test1/testfile1");
        jar("cf test.jar --release 9 test1");
        jar("tf test.jar");
        rm("test.jar test1");
    }

    @Test  // fails before bug fix
    public void test2() throws IOException {
        System.out.println("=====");
        mkdir("test1");
        touch("test1/testfile1");
        onCompletion = () -> rm("test.jar test1");
        jar("--create --file=test.jar --release 9 test1");
        jar("tf test.jar");
    }

    @Test  // passes before bug fix
    public void test3() throws IOException {
        System.out.println("=====");
        mkdir("test1");
        touch("test1/testfile1");
        jar("-cf test.jar -C test1 .");
        jar("-uf test.jar --release 9 -C test1 .");
        jar("tf test.jar");
        rm("test.jar test1");
    }

    @Test  // fails before bug fix
    public void test4() throws IOException {
        System.out.println("=====");
        mkdir("test1");
        touch("test1/testfile1");
        onCompletion = () -> rm("test.jar test1");
        jar("--create --file=test.jar -C test1 .");
        jar("--update --file=test.jar --release 9 -C test1 .");
        jar("tf test.jar");
    }

    @Test  // passes before bug fix since test2 precedes --release 9
    public void test5() throws IOException {
        System.out.println("=====");
        mkdir("test1 test2");
        touch("test1/testfile1 test2/testfile2");
        jar("--create --file=test.jar -C test1 .");
        jar("--update --file=test.jar test2 --release 9 -C test1 .");
        jar("tf test.jar");
        rm("test.jar test1 test2");
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
        boolean ok = new sun.tools.jar.Main(System.out, System.err, "jar")
                .run(cmdline.split(" +"));
        Assert.assertTrue(ok);
    }
}
