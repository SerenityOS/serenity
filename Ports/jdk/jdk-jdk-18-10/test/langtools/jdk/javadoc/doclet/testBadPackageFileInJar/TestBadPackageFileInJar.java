/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4691095 6306394
 * @summary Make sure that Javadoc emits a useful warning
 *          when a bad package.html exists in a JAR archive.
 * @library /tools/lib ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.* toolbox.ToolBox toolbox.JarTask
 * @run main TestBadPackageFileInJar
 */

import toolbox.JarTask;
import toolbox.Task.Result;
import toolbox.ToolBox;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javadoc.tester.JavadocTester;

public class TestBadPackageFileInJar extends JavadocTester {

    final ToolBox tb = new ToolBox();

    public static void main(String... args) throws Exception {
        TestBadPackageFileInJar tester = new TestBadPackageFileInJar();
        tester.runTests();
    }

    @Test
    public void test() throws IOException {
        // create the file
        Path pkgDir = Paths.get("pkg");
        tb.createDirectories(pkgDir);
        Path pkgfilePath = pkgDir.resolve("package.html");
        tb.writeFile(pkgfilePath, "<html>\n\n</html>");

        // create the jar file
        Path jarFile = Paths.get("badPackageFileInJar.jar");
        JarTask jar = new JarTask(tb, "badPackageFileInJar.jar");
        jar.files(pkgDir.toString()).run();

        // clean up to prevent accidental pick up
        tb.cleanDirectory(pkgDir);
        tb.deleteFiles(pkgDir.toString());

        javadoc("-d", "out",
                "-sourcepath", testSrc,
                "-classpath",  jarFile.toString(),
                "pkg");
        checkExit(Exit.OK);

        checkOutput(Output.OUT, true,
                "badPackageFileInJar.jar(/pkg/package.html):1: warning: no comment");
    }
}
