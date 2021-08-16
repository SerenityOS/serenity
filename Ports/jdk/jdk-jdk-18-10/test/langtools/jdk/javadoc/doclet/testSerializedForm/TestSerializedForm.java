/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4341304 4485668 4966728 8032066 8071982 8192933 8215307 8232644
 * @summary Test that methods readResolve and writeReplace show
 * up in serialized-form.html the same way that readObject and writeObject do.
 * If the doclet includes readResolve and writeReplace in the serialized-form
 * documentation that same way the it includes readObject and writeObject, the
 * test passes.  This also tests that throws tag information is correctly shown
 * in the serialized form page.
 * Make sure see tags work in serialized form.
 * @library ../../lib/
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @build TestSerializedForm
 * @run main TestSerializedForm
 */

import javadoc.tester.JavadocTester;

public class TestSerializedForm extends JavadocTester {
    public static void main(String... args) throws Exception {
        TestSerializedForm tester = new TestSerializedForm();
        tester.runTests();
    }

    @Test
    public void testDefault() {
        javadoc("-d", "out-default", "-serialwarn", "-Xdoclint:none",
                "--no-platform-links", "-sourcepath", testSrc,
                testSrc("SerializedForm.java"), testSrc("ExternalizedForm.java"), "pkg1");
        checkExit(Exit.OK);

        checkOutput("serialized-form.html", true,
                """
                    <span class="modifiers">protected</span>&nbsp;<span class="return-type">java.lan\
                    g.Object</span>&nbsp;<span class="element-name">readResolve</span>()""",
                """
                    <span class="modifiers">protected</span>&nbsp;<span class="return-type">java.lan\
                    g.Object</span>&nbsp;<span class="element-name">writeReplace</span>()""",
                """
                    <span class="modifiers">protected</span>&nbsp;<span class="return-type">java.lan\
                    g.Object</span>&nbsp;<span class="element-name">readObjectNoData</span>()""",
                """
                    <h4>Serialization Overview</h4>
                    <ul class="block-list">
                    <li class="block-list">
                    <div class="deprecation-block"><span class="deprecated-label">Deprecated.</span></div>
                    <dl class="notes">
                    <dt>See Also:</dt>
                    <dd>
                    <ul class="see-list">
                    <li><code>TestSerializedForm</code></li>
                    </ul>
                    </dd>
                    </dl>""",
                """
                    <h3>Class&nbsp;pkg1.NestedInnerClass.InnerClass.ProNestedInnerClass</h3>
                    <div class="type-signature">class ProNestedInnerClass extends java.lang.Object i\
                    mplements java.io.Serializable</div>
                    """,
                """
                    <h3>Class&nbsp;pkg1.PrivateIncludeInnerClass.PriInnerClass</h3>
                    <div class="type-signature">class PriInnerClass extends java.lang.Object impleme\
                    nts java.io.Serializable</div>""",
                """
                    <h3>Class&nbsp;pkg1.ProtectedInnerClass.ProInnerClass</h3>
                    <div class="type-signature">class ProInnerClass extends java.lang.Object impleme\
                    nts java.io.Serializable</div>""");

        checkOutput("serialized-form.html", false,
                """
                    <h3>Class&nbsp;<a href="pkg1/NestedInnerClass.InnerClass.ProNestedInnerClass.htm\
                    l" title="class in pkg1">pkg1.NestedInnerClass.InnerClass.ProNestedInnerClass</a\
                    ></h3>""",
                """
                    <h3>Class&nbsp;<a href="pkg1/PrivateInnerClass.PriInnerClass.html" title="class \
                    in pkg1">pkg1.PrivateInnerClass.PriInnerClass</a></h3>""",
                """
                    <h3>Class&nbsp;<a href="pkg1/ProtectedInnerClass.ProInnerClass.html" title="class in \
                    pkg1">pkg1.ProtectedInnerClass.ProInnerClass</a></h3>""",
                "<h3>Class&nbsp;pkg1.PublicExcludeInnerClass.PubInnerClass</h3>");

        checkOutput("serialized-form.html", true,
                """
                    <h4>Serialized Fields</h4>
                    <ul class="block-list">
                    <li class="block-list">
                    <h5>i</h5>
                    <pre>int i</pre>
                    <div class="block">an int</div>
                    </li>
                    <li class="block-list">
                    <h5>longs</h5>
                    <pre>java.lang.Long[] longs</pre>
                    <div class="block">the longs</div>
                    </li>
                    <li class="block-list">
                    <h5>m</h5>
                    <pre>double[][] m</pre>
                    <div class="block">the doubles</div>
                    </li>
                    <li class="block-list">
                    <h5>name</h5>
                    <pre>java.lang.String name</pre>
                    <div class="block">a test</div>
                    </li>
                    <li class="block-list">
                    <h5>next</h5>
                    <pre><a href="SerializedForm.html" title="class in Unnamed Package">SerializedForm</a> next</pre>
                    <div class="block">a linked reference</div>""");
    }

    @Test
    public void testPrivate() {
        javadoc("-private",
                "-d", "out-private",
                "-sourcepath", testSrc,
                "--no-platform-links",
                testSrc("SerializedForm.java"), testSrc("ExternalizedForm.java"), "pkg1");
        checkExit(Exit.OK);

        showHeadings("serialized-form.html");

        checkOutput("serialized-form.html", true,
                """
                    <h3>Class&nbsp;<a href="pkg1/NestedInnerClass.InnerClass.ProNestedInnerClass.htm\
                    l" title="class in pkg1">pkg1.NestedInnerClass.InnerClass.ProNestedInnerClass</a\
                    ></h3>
                    <div class="type-signature">class ProNestedInnerClass extends java.lang.Object i\
                    mplements java.io.Serializable</div>""",
                """
                    <h3>Class&nbsp;<a href="pkg1/PrivateIncludeInnerClass.PriInnerClass.html" title=\
                    "class in pkg1">pkg1.PrivateIncludeInnerClass.PriInnerClass</a></h3>
                    <div class="type-signature">class PriInnerClass extends java.lang.Object impleme\
                    nts java.io.Serializable</div>""",
                """
                    <h3>Class&nbsp;<a href="pkg1/ProtectedInnerClass.ProInnerClass.html" title="clas\
                    s in pkg1">pkg1.ProtectedInnerClass.ProInnerClass</a></h3>
                    <div class="type-signature">class ProInnerClass extends java.lang.Object impleme\
                    nts java.io.Serializable</div>""");

        checkOutput("serialized-form.html", false,
                "<h3>Class&nbsp;pkg1.NestedInnerClass.InnerClass.ProNestedInnerClass</h3>",
                "<h3>Class&nbsp;pkg1.PrivateInnerClass.PriInnerClass</h3>",
                "<h3>Class&nbsp;pkg1.ProtectedInnerClass.ProInnerClass</h3>",
                """
                    <h3>Class&nbsp;<a href="pkg1/PublicExcludeInnerClass.PubInnerClass.html" title="class\
                     in pkg1">pkg1.PublicExcludeInnerClass.PubInnerClass</a></h3>""");

        checkOutput("serialized-form.html", true,
                """
                    <h4>Serialized Fields</h4>
                    <ul class="block-list">
                    <li class="block-list">
                    <h5>i</h5>
                    <pre>int i</pre>
                    <div class="block">an int</div>
                    </li>
                    <li class="block-list">
                    <h5>longs</h5>
                    <pre>java.lang.Long[] longs</pre>
                    <div class="block">the longs</div>
                    </li>
                    <li class="block-list">
                    <h5>m</h5>
                    <pre>double[][] m</pre>
                    <div class="block">the doubles</div>
                    </li>
                    <li class="block-list">
                    <h5>name</h5>
                    <pre>java.lang.String name</pre>
                    <div class="block">a test</div>
                    </li>
                    <li class="block-list">
                    <h5>next</h5>
                    <pre><a href="SerializedForm.html" title="class in Unnamed Package">SerializedForm</a> next</pre>
                    <div class="block">a linked reference</div>""");
    }

    @Test
    public void test2() {
        javadoc("-private",
                "-d", "out-2",
                "-sourcepath", testSrc,
                "--no-platform-links",
                "pkg2");
        checkExit(Exit.OK);

        checkOrder("serialized-form.html",
                """
                    int[] a1""",
                """
                    int[][] a2""",
                """
                    <a href="pkg2/Fields.html" title="class in pkg2">Fields</a>[][] doubleArray""",
                """
                    <a href="pkg2/Fields.html" title="class in pkg2">Fields</a>[] singleArray""",
                """
                    java.lang.Class&lt;<a href="pkg2/Fields.html" title="type parameter in Fields">E</a>&gt; someClass""");
    }
}
