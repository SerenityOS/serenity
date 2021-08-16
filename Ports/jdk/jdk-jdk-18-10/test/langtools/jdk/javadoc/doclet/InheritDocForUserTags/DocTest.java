/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8008768
 * @summary Using {@inheritDoc} in simple tag defined via -tag fails
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main DocTest
 */

import javadoc.tester.JavadocTester;

/**
 * DocTest documentation.
 *
 * @apiNote DocTest API note.
 * @implSpec DocTest implementation spec.
 * @implNote DocTest implementation note.
 */
public class DocTest extends JavadocTester {
    public static void main(String... args) throws Exception {
        DocTest tester = new DocTest();
        tester.runTests();
    }

    @Test
    public void test() {
        javadoc("-verbose",
                "-d", "DocTest",
                "-sourcepath", System.getProperty("test.src.path"),
                "-tag", "apiNote:optcm:<em>API Note</em>",
                "-tag", "implSpec:optcm:<em>Implementation Requirements</em>:",
                "-tag", "implNote:optcm:<em>Implementation Note</em>:",
                "-package",
                testSrc("DocTest.java")
        );
        checkExit(Exit.OK);

        // javadoc does not report an exit code for an internal exception (!)
        // so monitor stderr for stack dumps.
        checkOutput(Output.STDERR, false, "at com.sun");
    }

    /**
     * DocTest() documentation.
     *
     * @apiNote DocTest() API note.
     * @implSpec DocTest() implementation spec.
     * @implNote DocTest() implementation note.
     */
    public DocTest() {
    }

    /**
     * DocTest.testMethod() documentation.
     *
     * @apiNote DocTest.testMethod() API note.
     * @implSpec DocTest.testMethod() implementation spec.
     * @implNote DocTest.testMethod() implementation note.
     */
    public void testMethod() {
    }
}

/**
 * DocTestWithTags documentation.
 *
 * @apiNote DocTestWithTags API note.
 * <pre>
 *    DocTestWithTags API note code sample.
 * </pre>
 * @implSpec DocTestWithTags implementation spec.
 * <pre>
 *    DocTestWithTags implementation spec code sample.
 * </pre>
 * @implNote DocTestWithTags implementation note.
 * <pre>
 *    DocTestWithTags implementation note code sample.
 * </pre>
 */
class DocTestWithTags {

    /**
     * DocTestWithTags() documentation.
     *
     * @apiNote DocTestWithTags() API note.
     * <pre>
     *    DocTestWithTags() API note code sample.
     * </pre>
     * @implSpec DocTestWithTags() implementation spec.
     * <pre>
     *    DocTestWithTags() implementation spec code sample.
     * </pre>
     * @implNote DocTest() implementation note.
     * <pre>
     *    DocTest() implementation note code sample.
     * </pre>
     */
    public DocTestWithTags() {
    }

    /**
     * DocTest.testMethod() documentation.
     *
     * @apiNote DocTestWithTags.testMethod() API note.
     * <pre>
     *    DocTestWithTags.testMethod() API note code sample.
     * </pre>
     * @implSpec DocTestWithTags.testMethod() implementation spec.
     * <pre>
     *    DocTestWithTags.testMethod() API implementation spec code sample.
     * </pre>
     * @implNote DocTest.testMethod() implementation note.
     * <pre>
     *    DocTest.testMethod() API implementation code sample.
     * </pre>
     */
    public void testMethod() {
    }
}

class MinimallyExtendsDocTest extends DocTest {
}

/**
 * SimpleExtendsDocTest documentation.
 */
class SimpleExtendsDocTest extends DocTest {

    /**
     * SimpleExtendsDocTest() documentation.
     */
    public SimpleExtendsDocTest() {

    }

    /**
     * SimpleExtendsDocTest.testMethod() documenation.
     */
    @java.lang.Override
    public void testMethod() {
    }
}

/**
 * {@inheritDoc}
 */
class SimpleInheritDocDocTest extends DocTest {

    /**
     * {@inheritDoc}
     */
    public SimpleInheritDocDocTest() {
    }

    /**
     * {@inheritDoc}
     */
    @java.lang.Override
    public void testMethod() {
    }
}

/**
 * {@inheritDoc}
 *
 * @apiNote {@inheritDoc}
 * @implSpec {@inheritDoc}
 * @implNote {@inheritDoc}
 */
class FullInheritDocDocTest extends DocTest {

    /**
     * {@inheritDoc}
     *
     * @apiNote {@inheritDoc}
     * @implSpec {@inheritDoc}
     * @implNote {@inheritDoc}
     */
    public FullInheritDocDocTest() {

    }

    /**
     * {@inheritDoc}
     *
     * @apiNote {@inheritDoc}
     * @implSpec {@inheritDoc}
     * @implNote {@inheritDoc}
     */
    @java.lang.Override
    public void testMethod() {
    }
}

/**
 * {@inheritDoc} and FullInheritDocPlusDocTest documentation.
 *
 * @implSpec {@inheritDoc} and FullInheritDocPlusDocTest API note.
 * @implNote {@inheritDoc} and FullInheritDocPlusDocTest implementation specification.
 * @apiNote {@inheritDoc} and FullInheritDocPlusDocTest implementation note.
 */
class FullInheritDocPlusDocTest extends DocTest {

    /**
     * {@inheritDoc} and FullInheritDocPlusDocTest() documentation.
     *
     * @implSpec {@inheritDoc} and FullInheritDocPlusDocTest() API note.
     * @implNote {@inheritDoc} and FullInheritDocPlusDocTest() implementation specification.
     * @apiNote {@inheritDoc} and FullInheritDocPlusDocTest() implementation note.
     */
    public FullInheritDocPlusDocTest() {

    }

    /**
     * {@inheritDoc} and FullInheritDocPlusDocTest.testMethod() documentation.
     *
     * @implSpec {@inheritDoc} and FullInheritDocPlusDocTest.testMethod() API note.
     * @implNote {@inheritDoc} and FullInheritDocPlusDocTest.testMethod() implementation specification.
     * @apiNote {@inheritDoc} and FullInheritDocPlusDocTest.testMethod() implementation note.
     */
    @java.lang.Override
    public void testMethod() {
    }
}

