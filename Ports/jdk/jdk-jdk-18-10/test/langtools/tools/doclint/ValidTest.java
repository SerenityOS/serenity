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
 * @bug 8004832 8048806
 * @summary Add new doclint package
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester ValidTest.java
 */

class ValidTest {
    /**
     * &lt; &gt; &amp; &#40;
     */
    void entities() { }

    /**
     * <abbr>abbreviation<abbr>
     * <acronym>ABC</acronym>
     * <cite>citation</cite>
     * <code>code</code>
     * <dfn>defining instance</dfn>
     * <em>emphasis</em>
     * <kbd>keyboard<kbd>
     * <samp>sample</samp>
     * <var>variable</var>
     * <strong>strong</strong>
     */
    void phraseElements() { }

    /**
     * <address>1 Main St., USA</address>
     */
    void address() { }

    /**
     * <del>deleted</del>
     * <ins>inserted</del>
     */
    void docChanges() {}

    /**
     * <blockquote>
     * A fine thing.
     * </blockquote>
     * <q>A fine thing.</q>
     */

    /**
     * <h1> ... </h1>
     * <h2> ... </h2>
     * <h3> ... </h3>
     * <h4> ... </h4>
     * <h5> ... </h5>
     * <h6> ... </h6>
     */
    void all_headers() { }

    /**
     * <h1> ... </h1>
     * <h2> ... </h2>
     * <h3> ... </h3>
     * <h1> ... </h1>
     * <h2> ... </h2>
     * <h3> ... </h3>
     * <h2> ... </h2>
     */
    void header_series() { }

    /**
     * <div> <p>   </div>
     */
    void autoclose_tags() { }

    /**
     * @param x
     */
    void method_param(int x) { }

    /**
     * @param <T>
     */
    <T> T method_typaram(T t) { return t; }

    /**
     * @param <T>
     */
    class ClassTyparam<T> { }

    /**
     * @param <T>
     */
    interface InterfaceTyparam<T> { }

    /**
     * @return x
     */
    int return_int() { return 0; }

    /**
     * @exception Exception
     */
    void throws_Exception1() throws Exception { }

    /**
     * @throws Exception
     */
    void throws_Exception2() throws Exception { }

    class X {
        /**
         * @param x
         */
        X(int x) { } // constructor parameter

        /**
         * @param <T>
         */
        <T> X(T t) { } // constructor type parameter
    }

}

