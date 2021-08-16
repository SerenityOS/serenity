/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8027530
 * @summary test -public, -protected, -package, -private options
 * @modules jdk.jdeps/com.sun.tools.javap
 */

import java.io.*;
import java.util.*;
import java.lang.StringBuilder;

public class AccessModifiers {
    public int errorCount;
    protected String protectedField;
    String packageField;
    private String privateField;

    public static void main(String[] args) throws Exception {
        new AccessModifiers().run();
    }

    private void run() throws Exception {
        List<String> pubMembers = new ArrayList<String>();
        pubMembers.add("public int errorCount");
        pubMembers.add("public AccessModifiers");
        pubMembers.add("public static void main");

        List<String> proMembers = new ArrayList<String>();
        proMembers.add("protected java.lang.String protectedField");
        proMembers.add("protected java.lang.String runJavap");

        List<String> pkgMembers = new ArrayList<String>();
        pkgMembers.add("java.lang.String packageField");
        pkgMembers.add("boolean verify");
        pkgMembers.add("void error");

        List<String> priMembers = new ArrayList<String>();
        priMembers.add("private java.lang.String privateField");
        priMembers.add("private void run() throws java.lang.Exception");
        priMembers.add("private void test");

        List<String> expectedList = new ArrayList<String>();

        expectedList.addAll(pubMembers);
        test("-public", expectedList);

        expectedList.addAll(proMembers);
        test("-protected", expectedList);

        expectedList.addAll(pkgMembers);
        test("-package", expectedList);

        expectedList.addAll(priMembers);
        test("-private", expectedList);

        if (errorCount > 0)
            throw new Exception(errorCount + " errors received");
    }

    private void test(String option, List<String> expectedStrs) throws Exception {
        String output = runJavap(0, option);
        if (verify(output, expectedStrs))
            System.out.println(option + " test passed");
    }

    protected String runJavap(int expect, String... options) {
        // convert the varargs to a list in order to add class name
        List<String> optlist = new ArrayList<String>();
        optlist.addAll(Arrays.asList(options));
        optlist.add("AccessModifiers");
        String[] newoptions = optlist.toArray(new String[optlist.size()]);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        System.out.printf("\nRun javap " + optlist + "\n\n");
        int rc = com.sun.tools.javap.Main.run(newoptions, pw);
        pw.close();
        System.out.println(sw);
        if (rc != expect)
           throw new Error("Expect to return " + expect + ", but return " + rc);
        return sw.toString();
    }

    boolean verify(String output, List<String> expects) {
        boolean pass = true;
        for (String expect: expects) {
            if (!output.contains(expect)) {
                error(expect + " not found");
                pass = false;
            }
        }
        return pass;
    }

    void error(String msg) {
        System.err.println(msg);
        errorCount++;
    }
}
