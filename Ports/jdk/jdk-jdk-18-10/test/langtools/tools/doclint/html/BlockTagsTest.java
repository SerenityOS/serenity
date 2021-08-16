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
 * @bug 8006251
 * @summary test block tags
 * @library ..
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -Xmsgs:-accessibility BlockTagsTest.java
 */

/** */
public class BlockTagsTest {
    /**
     *  <blockquote> abc </blockquote>
     *  <center> abc </center>
     *  <div> abc </div>
     *  <dl> <dt> abc <dd> def </dl>
     *  <div> abc </div>
     *  <h1> abc </h1>
     *  <h2> abc </h2>
     *  <h3> abc </h3>
     *  <h4> abc </h4>
     *  <h5> abc </h5>
     *  <h6> abc </h6>
     *  <hr>
     *  <menu> <li> abc </menu>
     *  <noscript> </noscript>
     *  <ol> <li> abc </ol>
     *  <p> abc </p>
     *  <pre> abc </pre>
     *  <table summary="abc"> <tr> <td> </table>
     *  <ul> <li> abc </ul>
     */
    public void supportedTags() { }
}
