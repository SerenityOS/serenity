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
 * @build DocLintTester
 * @run main DocLintTester -Xmsgs:all OverridesTest.java
 */

/*
 * This is a test that missing comments on methods may be inherited
 * from overridden methods. As such, there should be no errors due
 * to missing comments (or any other types of error) in this test.
 */

/** An interface. */
interface I1 {
    /**
     * A method
     * @param p a param
     * @throws Exception an exception
     * @return an int
     */
    int m(int p) throws Exception;
}

/** An extending interface. */
interface I2 extends I1 { }

/** An abstract class. */
abstract class C1 {
    /**
     * A method
     * @param p a param
     * @throws Exception an exception
     * @return an int
     */
    int m(int p) throws Exception;
}

/** An implementing class. */
class C2 implements I1 {
    int m(int  p) throws Exception { return p; }
}

/** An extending class. */
class C3 extends C1 {
    int m(int  p) throws Exception { return p; }
}

/** An extending and implementing class. */
class C4 extends C1 implements I1 {
    int m(int  p) throws Exception { return p; }
}

/** An implementing class using inheritdoc. */
class C5 implements I1 {
    /** {@inheritDoc} */
    int m(int  p) throws Exception { return p; }
}

/** An implementing class with incomplete documentation. */
class C6 implements I1 {
    /** Overriding method */
    int m(int  p) throws Exception { return p; }
}

/** A class implementing an inherited interface. */
class C7 implements I2 {
    int m(int  p) throws Exception { return p; }
}
