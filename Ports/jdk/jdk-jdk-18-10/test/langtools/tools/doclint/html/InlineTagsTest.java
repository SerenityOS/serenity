/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8006251 8247957
 * @summary test inline tags
 * @library ..
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -Xmsgs -ref InlineTagTest.out InlineTagsTest.java
 */

/** */
public class InlineTagsTest {
    /**
     *  <a href="#abc"> abc </a>
     *  <b> abc </b>
     *  <big> abc </big>
     *  <br>
     *  <cite> abc </cite>
     *  <code> abc </code>
     *  <dfn> abc </dfn>
     *  <em> abc </em>
     *  <font> abc </font>
     *  <i> abc </i>
     *  <img alt="image" src="image.png">
     *  <small> abc </small>
     *  <span> abc </span>
     *  <strong> abc </strong>
     *  <sub> abc </sub>
     *  <sup> abc </sup>
     *  <tt> abc </tt>
     *  <u> abc </u>
     *  <var> abc </var>
     */
    public void supportedTags() { }
}

