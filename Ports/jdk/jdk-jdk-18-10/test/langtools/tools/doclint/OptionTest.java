/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8004832
 * @summary Add new doclint package
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 */

import jdk.javadoc.internal.doclint.DocLint;

public class OptionTest {
    public static void main(String... args) throws Exception {
        new OptionTest().run();
    }

    String[] positiveTests = {
        "-Xmsgs",
        "-Xmsgs:all",
        "-Xmsgs:none",
        "-Xmsgs:accessibility",
        "-Xmsgs:html",
        "-Xmsgs:missing",
        "-Xmsgs:reference",
        "-Xmsgs:syntax",
        "-Xmsgs:html/public",
        "-Xmsgs:html/protected",
        "-Xmsgs:html/package",
        "-Xmsgs:html/private",
        "-Xmsgs:-html/public",
        "-Xmsgs:-html/protected",
        "-Xmsgs:-html/package",
        "-Xmsgs:-html/private",
        "-Xmsgs:html,syntax",
        "-Xmsgs:html,-syntax",
        "-Xmsgs:-html,syntax",
        "-Xmsgs:-html,-syntax",
        "-Xmsgs:html/public,syntax",
        "-Xmsgs:html,syntax/public",
        "-Xmsgs:-html/public,syntax/public"
    };

    String[] negativeTests = {
        "-typo",
        "-Xmsgs:-all",
        "-Xmsgs:-none",
        "-Xmsgs:typo",
        "-Xmsgs:html/typo",
        "-Xmsgs:html/public,typo",
        "-Xmsgs:html/public,syntax/typo",
    };

    void run() throws Exception {
        test(positiveTests, true);
        test(negativeTests, false);

        if (errors > 0)
            throw new Exception(errors + " errors occurred");
    }

    void test(String[] tests, boolean expect) {
        DocLint docLint = new DocLint();
        for (String test: tests) {
            System.err.println("test: " + test);
            boolean found = docLint.isValidOption(test);
            if (found != expect)
                error("Unexpected result: " + found + ",expected: " + expect);
        }
    }

    void error(String msg) {
        System.err.println("Error: " + msg);
        errors++;
    }

    int errors;
}
