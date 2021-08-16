/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8035063
 * @summary Tests the preparation of javac-arguments.
 *
 * @modules jdk.compiler/com.sun.tools.sjavac.options
 * @build Wrapper
 * @run main Wrapper JavacOptionPrep
 */

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;

import com.sun.tools.sjavac.options.Options;


public class JavacOptionPrep {

    enum TestPath {
        CP1, CP2, SRC1, SRC2, SOURCEPATH1, SOURCEPATH2;

        public String toString() {
            return name().toLowerCase();
        }
    }

    private final static String SEP = File.pathSeparator;

    public static void main(String[] unused) throws IOException {

        for (TestPath p : TestPath.values())
            Files.createDirectory(Paths.get(p.toString()));

        // Test some various cases:
        //  - Paths combined with File.pathSeparator (CP1 / CP2)
        //  - Paths given as duplicate options (SOURCEPATH1 / SOURCEPATH2)
        //  - Sources provided by -src (SRC1)
        //  - Sources provided without preceding option (SRC2)
        //  - An unrecognized option which is to be passed on to javac
        String sjavacArgs = "-cp " + TestPath.CP1 + SEP + TestPath.CP2 +
                            " -d dest" +
                            " -h header" +
                            " -sourcepath " + TestPath.SOURCEPATH1 +
                            " -src " + TestPath.SRC1 +
                            " -s gensrc" +
                            " -sourcepath " + TestPath.SOURCEPATH2 +
                            " " + TestPath.SRC2 +
                            " -unrecognized";

        Options options = Options.parseArgs(sjavacArgs.split(" "));

        // Extract javac-options
        String[] javacArgs = options.prepJavacArgs();

        // Check the result
        boolean destDirFound = false;
        boolean userPathsFirst = false;
        boolean headerDirFound = false;
        boolean gensrcDirFound = false;
        boolean classPathFound = false;
        boolean sourcePathFound = false;
        boolean unrecognizedFound = false;
        boolean implicitNoneFound = false;

        Iterator<String> javacArgIter = Arrays.asList(javacArgs).iterator();
        while (javacArgIter.hasNext()) {

            String option = javacArgIter.next();

            // Ignore this option for now. When the file=... requirement goes
            // away, this will be easier to handle.
            if (option.startsWith("--debug=completionDeps"))
                continue;

            switch (option) {
            case "-classpath":
            case "-cp":
                classPathFound = true;
                assertEquals(TestPath.CP1 + SEP + TestPath.CP2,
                             javacArgIter.next());
                break;

            case "-d":
                destDirFound = true;
                assertEquals(Paths.get("dest").toAbsolutePath().toString(),
                             javacArgIter.next());
                break;

            case "-h":
                headerDirFound = true;
                assertEquals(Paths.get("header").toAbsolutePath().toString(),
                             javacArgIter.next());
                break;

            case "-s":
                gensrcDirFound = true;
                assertEquals(Paths.get("gensrc").toAbsolutePath().toString(),
                             javacArgIter.next());
                break;

            case "-sourcepath":
                sourcePathFound = true;
                assertEquals(TestPath.SRC1 + SEP +
                             TestPath.SRC2 + SEP +
                             TestPath.SOURCEPATH1 + SEP +
                             TestPath.SOURCEPATH2,
                             javacArgIter.next());
                break;

            case "-unrecognized":
                unrecognizedFound = true;
                break;

            case "-implicit:none":
                implicitNoneFound = true;
                break;

                // Note that *which* files to actually compile is not dealt
                // with by prepJavacArgs.

            default:
                throw new AssertionError("Unexpected option found: " + option);
            }
        }

        if (!destDirFound)
            throw new AssertionError("Dest directory not found.");

        if (!headerDirFound)
            throw new AssertionError("Header directory not found.");

        if (!gensrcDirFound)
            throw new AssertionError("Generated source directory not found.");

        if (!classPathFound)
            throw new AssertionError("Class path not found.");

        if (!sourcePathFound)
            throw new AssertionError("Source path not found.");

        if (!unrecognizedFound)
            throw new AssertionError("\"-unrecognized\" not found.");

        if (!implicitNoneFound)
            throw new AssertionError("\"-implicit:none\" not found.");
    }

    static void assertEquals(Object expected, Object actual) {
        if (!expected.equals(actual))
            throw new AssertionError("Expected " + expected + " but got " + actual);
    }
}
