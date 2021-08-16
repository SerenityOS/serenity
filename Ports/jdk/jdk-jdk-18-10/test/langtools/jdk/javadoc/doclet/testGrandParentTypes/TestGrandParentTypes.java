/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug      8182108
 * @summary  Verify that grand parent interface types are correct, and
 *           various interface related sections are correctly generated.
 * @library  ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    javadoc.tester.*
 * @run main TestGrandParentTypes
 */

import javadoc.tester.JavadocTester;

public class TestGrandParentTypes extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestGrandParentTypes tester = new TestGrandParentTypes();
        tester.runTests();
    }

    @Test
    public void test1() {
        javadoc("-d", "out-1",
                "--no-platform-links",
                "-package",
                "-sourcepath", testSrc,
                "pkg1");

        checkExit(Exit.OK);

        checkOrder("pkg1/A.SupplierWithAList.html",
                "All Superinterfaces:",
                "A.AList",
                "java.util.Collection&lt;java.lang.Object&gt;",
                "java.lang.Iterable&lt;java.lang.Object&gt;",
                "java.util.List&lt;java.lang.Object&gt;");

        checkOrder("pkg1/A.AList.html",
                "All Superinterfaces:",
                "java.util.Collection&lt;java.lang.Object&gt;",
                "java.lang.Iterable&lt;java.lang.Object&gt;",
                "java.util.List&lt;java.lang.Object&gt;");

        checkOrder("pkg1/TEnum.html",
                "All Implemented Interfaces:",
                "java.io.Serializable",
                "java.lang.Comparable");

        checkOrder("pkg1/TError.html",
                "All Implemented Interfaces:",
                "java.io.Serializable");

        checkOrder("pkg1/TException.html",
                "All Implemented Interfaces:",
                "java.io.Serializable");

        checkOrder("pkg1/MList.html",
                "All Implemented Interfaces:",
                "java.io.Serializable",
                "java.lang.Cloneable",
                "java.lang.Iterable&lt;java.lang.String&gt;",
                "java.util.Collection&lt;java.lang.String&gt;",
                "java.util.List&lt;java.lang.String&gt;",
                "java.util.RandomAccess");
    }
}


