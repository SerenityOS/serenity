/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6958836 8002168 8182765
 * @summary javadoc should support -Xmaxerrs and -Xmaxwarns
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 */

import java.io.*;
import java.util.*;

public class Test {
    public static void main(String... args) throws Exception {
        new Test().run();
    }

    void run() throws Exception {
        javadoc("errs",  list(),                   10,  0);
        javadoc("errs",  list("-Xmaxerrs",   "0"), 10,  0);
        javadoc("errs",  list("-Xmaxerrs",   "2"),  2,  0);
        javadoc("errs",  list("-Xmaxerrs",   "4"),  4,  0);
        javadoc("errs",  list("-Xmaxerrs",  "20"), 10,  0);

        javadoc("warns", list(),                    0, 10);
        javadoc("warns", list("-Xmaxwarns",  "0"),  0, 10);
        javadoc("warns", list("-Xmaxwarns",  "2"),  0,  2);
        javadoc("warns", list("-Xmaxwarns",  "4"),  0,  4);
        javadoc("warns", list("-Xmaxwarns", "20"),  0, 10);

        if (errors > 0)
            throw new Exception(errors + " errors occurred.");
    }

    void javadoc(String pkg, List<String> testOpts,
                int expectErrs, int expectWarns) {
        System.err.println("Test " + (++count) + ": " + pkg + " " + testOpts);
        File testOutDir = new File("test" + count);

        List<String> opts = new ArrayList<String>();
        // Force en_US locale in lieu of something like -XDrawDiagnostics.
        // For some reason, this must be the first option when used.
        opts.addAll(list("-locale", "en_US"));
        opts.add("-Xdoclint:none");
        opts.addAll(list("-classpath", System.getProperty("test.src")));
        opts.addAll(list("-d", testOutDir.getPath()));
        opts.addAll(testOpts);
        opts.add(pkg);

        StringWriter errSW = new StringWriter();
        PrintWriter errPW = new PrintWriter(errSW);
        int rc = jdk.javadoc.internal.tool.Main.execute(
                opts.toArray(new String[opts.size()]),
                errPW);
        System.err.println("rc: " + rc);

        errPW.close();
        String errOut = errSW.toString();
        System.err.println("Errors:\n" + errOut);

        check(errOut, "Errors.java", expectErrs);
        check(errOut, " warning: ", expectWarns); // requires -locale en_US
    }

    void check(String text, String expectText, int expectCount) {
        int foundCount = 0;
        for (String line: text.split("[\r\n]+")) {
            if (line.contains(expectText))
                foundCount++;
        }
        if (foundCount != expectCount) {
            error("incorrect number of matches found: " + foundCount
                  + ", expected: " + expectCount);
        }
    }

    private List<String> list(String... args) {
        return Arrays.asList(args);
    }

    void error(String msg) {
        System.err.println(msg);
        errors++;
    }

    int count;
    int errors;
}
