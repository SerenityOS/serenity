/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8177280 8262992 8259499
 * @summary see and link tag syntax should allow generic types
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main TestGenericTypeLink
 */

import javadoc.tester.JavadocTester;

public class TestGenericTypeLink extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestGenericTypeLink test = new TestGenericTypeLink();
        test.runTests();
    }

    /**
     * Test valid internal and external links to generic types.
     */
    @Test
    public void testValidLinks() {
        javadoc("-d", "out1",
                "-sourcepath", testSrc,
                "-linkoffline", "http://example.com/docs/api/", testSrc,
                "-package", "pkg1");
        checkExit(Exit.OK);
        checkOutput("pkg1/A.html", true,
                """
                    <div class="block"><code><a href="http://example.com/docs/api/java.base/java/util/L\
                    ist.html" title="class or interface in java.util" class="external-link">List</a>&lt\
                    ;<a href="http://example.com/docs/api/java.base/java/lang/String.html" title="class\
                     or interface in java.lang" class="external-link">String</a>&gt;</code>
                     <a href="http://example.com/docs/api/java.base/java/util/List.html" title="class o\
                    r interface in java.util" class="external-link">List</a>&lt;? extends <a href="http\
                    ://example.com/docs/api/java.base/java/lang/CharSequence.html" title="class or inte\
                    rface in java.lang" class="external-link">CharSequence</a>&gt;
                     <a href="#someMethod(java.util.List,int)"><code>someMethod(ArrayList&lt;Integer&gt\
                    ;, int)</code></a>
                     <a href="#otherMethod(java.util.Map,double)"><code>otherMethod(Map&lt;String, Stri\
                    ngBuilder&gt;, double)</code></a></div>
                    """,

                """
                    <dl class="notes">
                    <dt>See Also:</dt>
                    <dd>
                    <ul class="see-list-long">
                    <li><code><a href="http://example.com/docs/api/java.base/java/util/Map.html" title="\
                    class or interface in java.util" class="external-link">Map</a>&lt;<a href="http://ex\
                    ample.com/docs/api/java.base/java/lang/String.html" title="class or interface in jav\
                    a.lang" class="external-link">String</a>,<wbr>? extends <a href="http://example.com/\
                    docs/api/java.base/java/lang/CharSequence.html" title="class or interface in java.la\
                    ng" class="external-link">CharSequence</a>&gt;</code></li>
                    <li><code><a href="http://example.com/docs/api/java.base/java/util/Map.html" title="\
                    class or interface in java.util" class="external-link">Map</a>&lt;<a href="http://ex\
                    ample.com/docs/api/java.base/java/lang/String.html" title="class or interface in jav\
                    a.lang" class="external-link">String</a>,<wbr>? super <a href="A.html" title="class \
                    in pkg1">A</a>&lt;<a href="http://example.com/docs/api/java.base/java/lang/String.ht\
                    ml" title="class or interface in java.lang" class="external-link">String</a>,<wbr>? \
                    extends <a href="http://example.com/docs/api/java.base/java/lang/RuntimeException.ht\
                    ml" title="class or interface in java.lang" class="external-link">RuntimeException</\
                    a>&gt;&gt;</code></li>
                    <li><a href="#someMethod(java.util.List,int)"><code>someMethod(List&lt;Number&gt;, i\
                    nt)</code></a></li>
                    <li><a href="#otherMethod(java.util.Map,double)"><code>otherMethod(Map&lt;String, ? \
                    extends CharSequence&gt;, double)</code></a></li>
                    </ul>
                    </dd>
                    </dl>""");
        checkOutput("pkg1/A.SomeException.html", true,
                """
                    <div class="block"><code><a href="A.html" title="class in pkg1">A</a>&lt;<a href="h\
                    ttp://example.com/docs/api/java.base/java/lang/String.html" title="class or interfa\
                    ce in java.lang" class="external-link">String</a>,<wbr><a href="A.SomeException.htm\
                    l" title="class in pkg1">A.SomeException</a>&gt;</code>
                     <a href="http://example.com/docs/api/java.base/java/util/Map.html" title="class or\
                     interface in java.util" class="external-link">link to generic type with label</a>\
                    </div>""",
                """
                    <dl class="notes">
                    <dt>See Also:</dt>
                    <dd>
                    <ul class="see-list-long">
                    <li><code><a href="A.html" title="class in pkg1">A</a>&lt;<a href="http://example.c\
                    om/docs/api/java.base/java/lang/String.html" title="class or interface in java.lang\
                    " class="external-link">String</a>,<wbr><a href="A.SomeException.html" title="class\
                     in pkg1">A.SomeException</a>&gt;</code></li>
                    <li><a href="http://example.com/docs/api/java.base/java/util/List.html" title="clas\
                    s or interface in java.util" class="external-link"><code>Link to generic type with \
                    label</code></a></li>
                    </ul>
                    </dd>
                    </dl>"""
                );
        checkOutput("pkg1/A.Inner.html", true,
                """
                    <dl class="notes">
                    <dt>See Also:</dt>
                    <dd>
                    <ul class="see-list-long">
                    <li><code><a href="A.html" title="class in pkg1">A</a>&lt;<a href="http://exampl\
                    e.com/docs/api/java.base/java/lang/String.html" title="class or interface in jav\
                    a.lang" class="external-link">String</a>,<wbr><a href="http://example.com/docs/a\
                    pi/java.base/java/lang/RuntimeException.html" title="class or interface in java.\
                    lang" class="external-link">RuntimeException</a>&gt;.<a href="A.Inner.html" titl\
                    e="class in pkg1">Inner</a></code></li>
                    <li><code><a href="A.html" title="class in pkg1">A</a>&lt;<a href="A.html" title\
                    ="class in pkg1">A</a>&lt;<a href="http://example.com/docs/api/java.base/java/la\
                    ng/String.html" title="class or interface in java.lang" class="external-link">St\
                    ring</a>,<wbr><a href="http://example.com/docs/api/java.base/java/lang/RuntimeEx\
                    ception.html" title="class or interface in java.lang" class="external-link">Runt\
                    imeException</a>&gt;.<a href="A.Inner.html" title="class in pkg1">Inner</a>,<wbr\
                    ><a href="A.SomeException.html" title="class in pkg1">A.SomeException</a>&gt;</c\
                    ode></li>
                    </ul>
                    </dd>
                    </dl>""");

        checkOutput("pkg1/C.html", true,
                """
                    Description copied from class:&nbsp;<code><a href="A.html#overriddenMethod()">A<\
                    /a></code></span></div>
                    <div class="block">Here's a generic link: <code><a href="A.html" title="class in\
                     pkg1">A</a>&lt;<a href="http://example.com/docs/api/java.base/java/lang/Object.\
                    html" title="class or interface in java.lang" class="external-link">Object</a>,<\
                    wbr><a href="http://example.com/docs/api/java.base/java/lang/RuntimeException.ht\
                    ml" title="class or interface in java.lang" class="external-link">RuntimeExcepti\
                    on</a>&gt;.<a href="A.Inner.html" title="class in pkg1">Inner</a>""");
    }

    /**
     * Test invalid links to generic types.
     */
    @Test
    public void testInvalidLinks() {
        javadoc("-d", "out2",
                "-sourcepath", testSrc,
                "-linkoffline", "http://example.com/docs/api/", testSrc,
                "-package", "pkg2");
        checkExit(Exit.ERROR);
        checkOutput("pkg2/B.html", true,
                """
                    <div class="block"><code>java.util.Foo&lt;String&gt;</code>
                     Baz&lt;Object&gt;
                     <code>#b(List&lt;Integer&gt;)</code></div>""",

                """
                    <dl class="notes">
                    <dt>See Also:</dt>
                    <dd>
                    <ul class="see-list-long">
                    <li><code>java.util.List&lt;Bar&gt;</code></li>
                    <li><code>Baz&lt;Object, String&gt;</code></li>
                    <li><code>B#b(List&lt;Baz&gt;)</code></li>
                    </ul>
                    </dd>
                    </dl>""");
    }
}

