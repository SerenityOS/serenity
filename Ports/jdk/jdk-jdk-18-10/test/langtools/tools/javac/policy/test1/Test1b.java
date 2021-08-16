/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @test 6420151
 * @summary Compile a group of files and validate the set of class files produced
 * @modules jdk.compiler
 * @run main Test1b -XDcompilePolicy=byfile A.java B.java D.java
 */

/*
 * @test 6420151
 * @summary Compile a group of files and validate the set of class files produced
 * @run main Test1b -XDcompilePolicy=byfile A.java C.java D.java
 */

/*
 * @test 6420151
 * @summary Compile a group of files and validate the set of class files produced
 * @run main Test1b -XDcompilePolicy=simple A.java B.java D.java
 */

/*
 * @test 6420151
 * @summary Compile a group of files and validate the set of class files produced
 * @run main Test1b -XDcompilePolicy=simple A.java C.java D.java
 */

// These test cases should be uncommented when the default compile policy is
// changed to "byfile".  While the default policy is "bytodo", the test cases fail
///*
// * @test 6420151
// * @summary Compile a group of files and validate the set of class files produced
// * @run main Test1b A.java B.java D.java
// */
//
///*
// * @test 6420151
// * @summary Compile a group of files and validate the set of class files produced
// * @run main Test1b A.java C.java D.java
// */

// These test cases are retained for debugging; if uncommented, they show that
// to bytodo mode fails the "all or none" class file test
///*
// * @test 6420151
// * @summary Compile a group of files and validate the set of class files produced
// * @run main Test1b -XDcompilePolicy=bytodo A.java B.java D.java
// */
//
///*
// * @test 6420151
// * @summary Compile a group of files and validate the set of class files produced
// * @run main Test1b -XDcompilePolicy=bytodo A.java C.java D.java
// */

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Test1b
{
    public static void main(String... args) throws Exception {
        new Test1b().run(args);
    }

    void run(String... args) throws Exception {
        File testSrcDir = new File(System.getProperty("test.src"));
        File tmpClassDir = new File(".");
        List<String> l = new ArrayList<String>();
        l.add("-d");
        l.add(tmpClassDir.getPath());
        for (String a: args) {
            if (a.endsWith(".java"))
                l.add(new File(testSrcDir, a).getPath());
            else
                l.add(a);
        }

        StringWriter sw = new StringWriter();
        int rc = com.sun.tools.javac.Main.compile(l.toArray(new String[l.size()]), new PrintWriter(sw));
        System.err.println(sw);

        Pattern p = Pattern.compile("([A-Z]+).*");
        for (String name: tmpClassDir.list()) {
            if (name.endsWith(".class")) {
                Matcher m = p.matcher(name);
                if (m.matches()) {
                    found(m.group(1), name);
                }
            }
        }

        // for all classes that might have been compiled, check that
        // all the classes in the source file get generated, or none do.
        check("A", 3);
        check("B", 3);
        check("C", 3);
        check("D", 3);

        if (errors > 0)
            throw new Exception(errors + " errors");
    }

    void check(String prefix, int expect) {
        List<String> names = map.get(prefix);
        int found = (names == null ? 0 : names.size());
        if (found == 0 || found == expect) {
            System.err.println("Found " + found + " files for " + prefix + ": OK");
            return;
        }
        error("Found " + found + " files for " + prefix + ": expected 0 or " + expect + " " + names);
    }

    void found(String prefix, String name) {
        List<String> names = map.get(prefix);
        if (names == null) {
            names = new ArrayList<String>();
            map.put(prefix, names);
        }
        names.add(name);
    }

    void error(String message) {
        System.err.println(message);
        errors++;
    }

    Map<String,List<String>> map = new HashMap<String,List<String>>();
    int errors;
}
