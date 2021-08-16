/*
 * Copyright 2009 Google, Inc.  All Rights Reserved.
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
 * @bug      5093723
 * @summary  REGRESSION: ClassCastException in SingleIndexWriter
 * @library  ../../lib
 * @modules  jdk.javadoc/jdk.javadoc.internal.tool
 * @build    javadoc.tester.*
 * @run main T5093723
 */

import javadoc.tester.JavadocTester;

public class T5093723 extends JavadocTester {

    public static void main(String... args) throws Exception {
        T5093723 tester = new T5093723();
        tester.runTests();
    }

    @Test
    public void test() {
        setAutomaticCheckLinks(false); // @ignore JDK-8202617
        javadoc("-d", "out",
                "-Xdoclint:none",
                testSrc("DocumentedClass.java"),
                testSrc("UndocumentedClass.java"));
        checkExit(Exit.OK);
    }
}
