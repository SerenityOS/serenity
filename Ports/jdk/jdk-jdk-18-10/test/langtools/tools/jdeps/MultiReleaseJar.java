/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8153654 8176333
 * @summary Tests for jdeps tool with multi-release jar files
 * @modules jdk.jdeps/com.sun.tools.jdeps
 * @library mrjar mrjar/base mrjar/9 mrjar/10 mrjar/v9 mrjar/v10
 * @build test.* p.* q.* foo/* Main
 * @run testng MultiReleaseJar
 */

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class MultiReleaseJar {
    Path mrjar;
    String testJdk;
    String fileSep;
    Path cmdPath;

    @BeforeClass
    public void initialize() throws Exception {
        String testClassPath = System.getProperty("test.class.path", "");
        mrjar = Stream.of(testClassPath.split(File.pathSeparator))
                      .map(Paths::get)
                      .filter(e -> e.endsWith("mrjar"))
                      .findAny()
                      .orElseThrow(() -> new InternalError("mrjar not found"));
        testJdk = System.getProperty("test.jdk");
        fileSep = System.getProperty("file.separator");
        cmdPath = Paths.get(testJdk, "bin");

        // build Version.jar, Version_9.jar and main.jar
        Result r = run("jar -cf Version.jar -C base test --release 9 -C 9 test --release 10 -C 10 test");
        checkResult(r);

        r = run("jar -cf Version_9.jar -C base test --release 9 -C 9 test");
        checkResult(r);

        r = run("jar -cf Main.jar Main.class");
        checkResult(r);

        r = run("jar -cf Foo.jar -C base p");
        checkResult(r);

        Path foo = Paths.get(System.getProperty("test.classes")).resolve("modules").resolve("foo");
        r = run("jar -uf Foo.jar --release 9 -C " + foo.toString() + " module-info.class --release 10 -C v10 q");
        checkResult(r);
    }

    @Test
    public void basic() throws Exception {
        Result r = run("jdeps --multi-release 9  -v missing.jar");
        checkResult(r, false, "Warning: Path does not exist: missing.jar");

        r = run("jdeps -v Version.jar");
        checkResult(r, false, "--multi-release option is not set");

        r = run("jdeps --multi-release base  -v Version.jar");
        checkResult(r, true,
            "Version.jar ->",
            "test.Version",
            "test.Version"
        );

        r = run("jdeps --multi-release 9  -v Version.jar");
        checkResult(r, true,
            "Version.jar ->",
            "9/test.NonPublic",
            "9/test.NonPublic",
            "9/test.Version",
            "9/test.Version",
            "9/test.Version",
            "9/test.Version"
        );

        r = run("jdeps --multi-release 10  -v Version.jar");
        checkResult(r, true,
            "Version.jar ->",
            "10/test.Version",
            "10/test.Version",
            "10/test.Version",
            "10/test.Version",
            "9/test.NonPublic",
            "9/test.NonPublic"
        );

        r = run("jdeps --multi-release 8  -v Version.jar");
        checkResult(r, false, "Error: invalid argument for option: 8");

        r = run("jdeps --multi-release 9.1  -v Version.jar");
        checkResult(r, false, "Error: invalid argument for option: 9.1");

        runJdeps("Main.class");
        runJdeps("Main.jar");
    }


    private void runJdeps(String path) throws Exception {
        Result r = run("jdeps -v -R -cp Version.jar " + path);
        checkResult(r, false, "--multi-release option is not set");

        r = run("jdeps -v -R -cp Version.jar --module-path Foo.jar -multi-release 9 " + path);
        checkResult(r, false,
            "Error: unknown option: -multi-release",
            "Usage: jdeps <options> <path",
            "use --help"
        );

        r = run("jdeps -v -R -cp Version.jar --module-path Foo.jar --multi-release 9 " + path);

        String name = path;
        int index = path.lastIndexOf('/');
        if (index >= 0) {
            name = path.substring(index + 1, path.length());
        }
        checkResult(r, true,
            name + " -> Version.jar",
            name + " -> foo",
            name + " -> java.base",
            "Main",
            "Main",
            "Main",
            "Main",
            "Version.jar -> java.base",
            "9/test.NonPublic",
            "9/test.NonPublic",
            "9/test.Version",
            "9/test.Version",
            "9/test.Version",
            "9/test.Version",
            "foo",
            "Foo.jar",
            "requires mandated java.base",
            "foo -> java.base",
            "p.Foo"
        );

        r = run("jdeps -v -R -cp Version.jar --module-path Foo.jar --multi-release 10 " + path);
        checkResult(r, true,
            name + " -> Version.jar",
            name + " -> foo",
            name + " -> java.base",
            "Main",
            "Main",
            "Main",
            "Main",
            "Version.jar ->",
            "10/test.Version",
            "10/test.Version",
            "10/test.Version",
            "10/test.Version",
            "9/test.NonPublic",
            "9/test.NonPublic",
            "foo",
            "Foo.jar",
            "requires mandated java.base",
            "foo -> java.base",
            "p.Foo"
        );

        r = run("jdeps -v -R -cp Version.jar --module-path Foo.jar --multi-release base " + path);
        checkResult(r, true,
            name + " -> Version.jar",
            name + " -> foo",
            name + " -> java.base",
            "Main",
            "Main",
            "Main",
            "Main",
            "Version.jar ->",
            "test.Version",
            "test.Version",
            "foo",
            "Foo.jar",
            "requires mandated java.base",
            "foo -> java.base",
            "p.Foo"
        );

        r = run("jdeps -v -R -cp Version.jar --module-path  Foo.jar --multi-release 9.1 " + path);
        checkResult(r, false, "Error: invalid argument for option: 9.1");

        // Version_9.jar does not have any version 10 entry
        r = run("jdeps -v -R -cp Version_9.jar --module-path Foo.jar --multi-release 10 " + path);
        checkResult(r, true,
            name + " -> Version_9.jar",
            name + " -> foo",
            name + " -> java.base",
            "Main",
            "Main",
            "Main",
            "Main",
            "Version_9.jar ->",
            "9/test.NonPublic",
            "9/test.NonPublic",
            "9/test.Version",
            "9/test.Version",
            "9/test.Version",
            "9/test.Version",
            "foo",
            "Foo.jar",
            "requires mandated java.base",
            "foo -> java.base",
            "p.Foo"
        );
    }

    @Test
    public void ps_and_qs() throws Exception {
        // build the jar file
        Result r = run("jar -cf PQ.jar -C base p --release 9 -C v9 p -C v9 q --release 10 -C v10 q");
        checkResult(r);

        r = run("jdeps -v -R -cp PQ.jar --multi-release base PQ.jar");
        checkResult(r, true,
            "PQ.jar -> java.base",
            "p.Foo"
        );

        r = run("jdeps -v -R -cp PQ.jar --multi-release 9 PQ.jar");
        checkResult(r, true,
            "PQ.jar -> java.base",
            "9/p.Foo",
            "9/p.Foo",
            "9/q.Bar"
        );


        r = run("jdeps -v -R -cp PQ.jar --multi-release 10 PQ.jar");
        checkResult(r, true,
            "PQ.jar -> java.base",
            "10/q.Bar",
            "10/q.Bar",
            "10/q.Gee",
            "9/p.Foo",
            "9/p.Foo"
        );
    }

    @Test
    public void modularJar() throws Exception {
        Result r = run("jdeps -v --multi-release 10 --module-path Foo.jar -m foo");
        checkResult(r, true,
            "foo",                   // module name
            "Foo.jar",                      // the path to Foo.jar
            "requires mandated java.base",  // module dependences
            "foo -> java.base",
            "10/q.Bar",
            "10/q.Bar",
            "10/q.Gee",
            "p.Foo"
        );

        r = run("jdeps --multi-release 9 -cp Version.jar --module-path Foo.jar Main.jar");
        checkResult(r, true,
            "Main.jar -> Version.jar",
            "Main.jar -> foo",
            "Main.jar -> java.base",
            "-> java.lang",
            "-> p",
            "-> test"
        );

        r = run("jdeps --multi-release 10 -cp Version.jar --module-path Foo.jar Main.jar");
        checkResult(r, true,
            "Main.jar -> Version.jar",
            "Main.jar -> foo",
            "Main.jar -> java.base",
            "-> java.lang",
            "-> p",
            "-> test"
        );
    }

    static class Result {
        final String cmd;
        final int rc;
        final String out;
        final String err;
        Result(String cmd, int rc, String out, String err) {
            this.cmd = cmd;
            this.rc = rc;
            this.out = out;
            this.err = err;
        }
    }

    Result run(String cmd) throws Exception {
        String[] cmds = cmd.split(" +");
        cmds[0] = cmdPath.resolve(cmds[0]).toString();
        ProcessBuilder pb = new ProcessBuilder(cmds);
        pb.directory(mrjar.toFile());
        Process p = null;
        try {
            p = pb.start();
            p.waitFor();

            String out;
            try (InputStream is = p.getInputStream()) {
                out = new String(is.readAllBytes());
            }
            String err;
            try (InputStream is = p.getErrorStream()) {
                err = new String(is.readAllBytes());
            }
            return new Result(cmd, p.exitValue(), out, err);
        } catch (Throwable t) {
            if (p != null) {
                p.destroyForcibly().waitFor();
            }
            throw t;
        }
    }

    void checkResult(Result r) throws Exception {
        System.out.println(r.cmd);
        System.out.println(r.out);
        if (r.rc != 0) {
            System.out.println(r.err);
            throw new Exception("rc=" + r.rc);
        }
        System.out.println();
    }

    void checkResult(Result r, boolean checkrc, String... lines) throws Exception {
        System.out.println(r.cmd);
        System.out.println(r.out);
        if (checkrc && r.rc != 0) {
            System.out.println(r.err);
            throw new Exception("rc=" + r.rc);
        }
        String[] out = r.out.split("\r?\n");
        Assert.assertEquals(out.length, lines.length);
        int n = 0;
        for (String line : lines) {
            Assert.assertTrue(out[n++].contains(line), "\"" + line + "\"");
        }
        System.out.println();
    }
}
