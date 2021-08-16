/*
 * Copyright (c) 2009, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4880672 7031005
 * @summary javap does not output inner interfaces of an interface
 * @modules jdk.jdeps/com.sun.tools.javap
 */

import java.io.*;
import java.util.*;

public class T4880672
{
    public static void main(String... args) {
        new T4880672().run();
    }

    void run() {
        verify("java.util.Map", "public interface java.util.Map$Entry");
        verify("T4880672", "class T4880672$A$B");
        verify("C", ""); // must not give error if no InnerClasses attribute
        if (errors > 0)
            throw new Error(errors + " found.");
    }

    void verify(String className, String... expects) {
        String output = javap(className);
        for (String expect: expects) {
            if (output.indexOf(expect)< 0)
                error(expect + " not found");
        }
    }

    void error(String msg) {
        System.err.println(msg);
        errors++;
    }

    int errors;

    String javap(String className) {
        String testClasses = System.getProperty("test.classes", ".");
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        String[] args = { "-XDinner", "-classpath", testClasses, className };
        int rc = com.sun.tools.javap.Main.run(args, out);
        out.close();
        String output = sw.toString();
        System.out.println("class " + className);
        System.out.println(output);
        if (rc != 0)
            throw new Error("javap failed. rc=" + rc);
        if (output.indexOf("Error:") != -1)
            throw new Error("javap reported error.");
        return output;
    }

    class A {
        class B { }
    }
}

class C { }

