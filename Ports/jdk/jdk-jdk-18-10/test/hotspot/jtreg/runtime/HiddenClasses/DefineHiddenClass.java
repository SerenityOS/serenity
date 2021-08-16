/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @build jdk.test.lib.JDKToolLauncher
 *        jdk.test.lib.process.ProcessTools
 *        jdk.test.lib.Utils
 * @run main/othervm -Xverify:remote DefineHiddenClass
 */

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import static java.lang.invoke.MethodHandles.Lookup.ClassOption.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import jdk.internal.misc.Unsafe;

import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.Utils;

/* package-private */ interface Test {
  void test();
}


public class DefineHiddenClass {

    static final Class<?> klass = DefineHiddenClass.class;
    static final Path SRC_DIR = Paths.get(Utils.TEST_SRC, "hidden");
    static final Path CLASSES_DIR = Paths.get(Utils.TEST_CLASSES, "hidden");

    static void compileSources(String sourceFile) throws Throwable {
        JDKToolLauncher launcher = JDKToolLauncher.createUsingTestJDK("javac");
        launcher.addToolArg("-cp")
                .addToolArg(Utils.TEST_CLASSES.toString())
                .addToolArg("-d")
                .addToolArg(CLASSES_DIR.toString())
            .addToolArg(Paths.get(SRC_DIR.toString(), sourceFile).toString());

        int exitCode = ProcessTools.executeCommand(launcher.getCommand())
                                   .getExitValue();
        if (exitCode != 0) {
            throw new RuntimeException("Compilation of the test failed. "
                    + "Unexpected exit code: " + exitCode);
        }
    }

    static byte[] readClassFile(String classFileName) throws Exception {
        File classFile = new File(CLASSES_DIR + File.separator + classFileName);
        try (FileInputStream in = new FileInputStream(classFile);
             ByteArrayOutputStream out = new ByteArrayOutputStream())
        {
            int b;
            while ((b = in.read()) != -1) {
                out.write(b);
            }
            return out.toByteArray();
        }
    }

    public static void main(String[] args) throws Throwable {
        compileSources("NameInString.java");
        Lookup lookup = MethodHandles.lookup();
        byte[] bytes = readClassFile("NameInString.class");
        Class<?> c = lookup.defineHiddenClass(bytes, true, NESTMATE).lookupClass();
        Test t = (Test) c.newInstance();
        t.test();
    }

}
