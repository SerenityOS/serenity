/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7112427 8012295 8025633 8026567 8061305 8081854 8150130 8162363
 *      8167967 8172528 8175200 8178830 8182257 8186332 8182765 8025091
 *      8203791 8184205 8249633 8261976
 * @summary Test of the JavaFX doclet features.
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main TestJavaFX
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javadoc.tester.JavadocTester;

public class TestJavaFX extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestJavaFX tester = new TestJavaFX();
        tester.setAutomaticCheckAccessibility(false);
        tester.setAutomaticCheckLinks(false);
        tester.runTests();
    }

    @Test
    public void test1() {
        javadoc("-d", "out1",
                "-sourcepath", testSrc,
                "-javafx",
                "--disable-javafx-strict-checks",
                "-package",
                "pkg1");
        checkExit(Exit.OK);

        checkOutput("pkg1/C.html", true,
                """
                    <dt>See Also:</dt>
                    <dd>
                    <ul class="see-list">
                    <li><a href="#getRate()"><code>getRate()</code></a></li>
                    <li><a href="#setRate(double)"><code>setRate(double)</code></a></li>
                    </ul>
                    </dd>""",
                """
                    <div class="member-signature"><span class="modifiers">public final</span>&nbsp;<\
                    span class="return-type">void</span>&nbsp;<span class="element-name">setRate</span><wbr>\
                    <span class="parameters">(double&nbsp;value)</span></div>
                    <div class="block">Sets the value of the property rate.</div>
                    <dl class="notes">
                    <dt>Property description:</dt>""",
                """
                    <div class="member-signature"><span class="modifiers">public final</span>&nbsp;<\
                    span class="return-type">double</span>&nbsp;<span class="element-name">getRate</span>()<\
                    /div>
                    <div class="block">Gets the value of the property rate.</div>
                    <dl class="notes">
                    <dt>Property description:</dt>""",
                """
                    <div class="col-first odd-row-color"><code>final <a href="C.DoubleProperty.html" title\
                    ="class in pkg1">C.DoubleProperty</a></code></div>
                    <div class="col-second odd-row-color"><code><a href="#rateProperty" class="membe\
                    r-name-link">rate</a></code></div>
                    <div class="col-last odd-row-color">
                    <div class="block">Defines the direction/speed at which the <code>Timeline</code> is expected to
                     be played.</div>""",
                "<dt>Default value:</dt>",
                """
                    <dt>Since:</dt>
                    <dd>JavaFX 8.0</dd>""",
                "<dt>Property description:</dt>",
                """
                    <div class="col-second even-row-color method-summary-table method-summary-table-\
                    tab2 method-summary-table-tab4"><code><a href="#setTestMethodProperty()" class="\
                    member-name-link">setTestMethodProperty</a>()</code></div>""",
                """
                    <div class="col-second even-row-color"><code><a href="#pausedProperty" class="me\
                    mber-name-link">paused</a></code></div>
                    <div class="col-last even-row-color">
                    <div class="block">Defines if paused.</div>""",
                """
                    <section class="detail" id="pausedProperty">
                    <h3>paused</h3>
                    <div class="member-signature"><span class="modifiers">public final</span>&nbsp;<\
                    span class="return-type"><a href="C.BooleanProperty.html" title="class in pkg1">\
                    C.BooleanProperty</a></span>&nbsp;<span class="element-name">pausedProperty</span></div>
                    <div class="block">Defines if paused. The second line.</div>""",
                """
                    <section class="detail" id="isPaused()">
                    <h3>isPaused</h3>
                    <div class="member-signature"><span class="modifiers">public final</span>&nbsp;<\
                    span class="return-type">double</span>&nbsp;<span class="element-name">isPaused<\
                    /span>()</div>
                    <div class="block">Gets the value of the property paused.</div>""",
                """
                    <section class="detail" id="setPaused(boolean)">
                    <h3>setPaused</h3>
                    <div class="member-signature"><span class="modifiers">public final</span>&nbsp;<\
                    span class="return-type">void</span>&nbsp;<span class="element-name">setPaused</\
                    span><wbr><span class="parameters">(boolean&nbsp;value)</span></div>
                    <div class="block">Sets the value of the property paused.</div>
                    <dl class="notes">
                    <dt>Property description:</dt>
                    <dd>Defines if paused. The second line.</dd>
                    <dt>Default value:</dt>
                    <dd>false</dd>""",
                """
                    <section class="detail" id="isPaused()">
                    <h3>isPaused</h3>
                    <div class="member-signature"><span class="modifiers">public final</span>&nbsp;<\
                    span class="return-type">double</span>&nbsp;<span class="element-name">isPaused<\
                    /span>()</div>
                    <div class="block">Gets the value of the property paused.</div>
                    <dl class="notes">
                    <dt>Property description:</dt>
                    <dd>Defines if paused. The second line.</dd>
                    <dt>Default value:</dt>
                    <dd>false</dd>""",
                """
                    <section class="detail" id="rateProperty">
                    <h3>rate</h3>
                    <div class="member-signature"><span class="modifiers">public final</span>&nbsp;<\
                    span class="return-type"><a href="C.DoubleProperty.html" title="class in pkg1">C\
                    .DoubleProperty</a></span>&nbsp;<span class="element-name">rateProperty</span></div>
                    <div class="block">Defines the direction/speed at which the <code>Timeline</code> is expected to
                     be played. This is the second line.</div>""",
                """
                    <section class="detail" id="setRate(double)">
                    <h3>setRate</h3>
                    <div class="member-signature"><span class="modifiers">public final</span>&nbsp;<\
                    span class="return-type">void</span>&nbsp;<span class="element-name">setRate</sp\
                    an><wbr><span class="parameters">(double&nbsp;value)</span></div>
                    <div class="block">Sets the value of the property rate.</div>
                    <dl class="notes">
                    <dt>Property description:</dt>
                    <dd>Defines the direction/speed at which the <code>Timeline</code> is expected to
                     be played. This is the second line.</dd>
                    <dt>Default value:</dt>
                    <dd>11</dd>
                    <dt>Since:</dt>
                    <dd>JavaFX 8.0</dd>""",
                """
                    <section class="detail" id="getRate()">
                    <h3>getRate</h3>
                    <div class="member-signature"><span class="modifiers">public final</span>&nbsp;<\
                    span class="return-type">double</span>&nbsp;<span class="element-name">getRate</span>()<\
                    /div>
                    <div class="block">Gets the value of the property rate.</div>
                    <dl class="notes">
                    <dt>Property description:</dt>
                    <dd>Defines the direction/speed at which the <code>Timeline</code> is expected to
                     be played. This is the second line.</dd>
                    <dt>Default value:</dt>
                    <dd>11</dd>
                    <dt>Since:</dt>
                    <dd>JavaFX 8.0</dd>""",
                """
                    <section class="property-summary" id="property-summary">
                    <h2>Property Summary</h2>
                    <div class="caption"><span>Properties</span></div>
                    <div class="summary-table three-column-summary">""",
                """
                    <div class="col-first even-row-color"><code>final <a href="C.BooleanProperty.html" title="class in pkg1">C.BooleanProperty</a></code></div>
                    """,
                """
                    <div class="col-first odd-row-color"><code>final <a href="C.DoubleProperty.html" title="class in pkg1">C.DoubleProperty</a></code></div>
                    """);

        checkOutput("pkg1/C.html", false,
                "A()",
                """
                    <h2>Property Summary</h2>
                    <div id="method-summary-table">
                    <div class="table-tabs" role="tablist" aria-orientation="horizontal">\
                    <button id="method-summary-table-tab0" role="tab" aria-selected="true" aria-cont\
                    rols="method-summary-table.tabpanel" tabindex="0" onkeydown="switchTab(event)" o\
                    nclick="show('method-summary-table', 'method-summary-table', 3)" class="active-t\
                    able-tab">All Methods</button>\
                    <button id="method-summary-table-tab2" role="tab" aria-selected="false" aria-con\
                    trols="method-summary-table.tabpanel" tabindex="-1" onkeydown="switchTab(event)"\
                     onclick="show('method-summary-table', 'method-summary-table-tab2', 3)" class="t\
                    able-tab">Instance Methods</button>\
                    <button id="method-summary-table-tab4" role="tab" aria-selected="false" aria-con\
                    trols="method-summary-table.tabpanel" tabindex="-1" onkeydown="switchTab(event)"\
                     onclick="show('method-summary-table', 'method-summary-table-tab4', 3)" class="t\
                    able-tab">Concrete Methods</button>\
                    </div>""",
                """
                    <tr id="i0" class="even-row-color">
                    <td class="col-first"><code><a href="C.BooleanProperty.html" title="class in pkg1">C.BooleanProperty</a></code></td>
                    """,
                """
                    <tr id="i1" class="odd-row-color">
                    <td class="col-first"><code><a href="C.DoubleProperty.html" title="class in pkg1">C.DoubleProperty</a></code></td>
                    """);

        checkOutput("index-all.html", true,
                """
                    <div class="block">Gets the value of the property paused.</div>""",
                """
                    <div class="block">Defines if paused.</div>""");

        checkOutput("pkg1/D.html", true,
                """
                    <h3 id="properties-inherited-from-class-pkg1.C">Properties inherited from class&\
                    nbsp;pkg1.<a href="C.html" title="class in pkg1">C</a></h3>
                    <code><a href="C.html#pausedProperty">paused</a>, <a href="C.html#rateProperty">rate</a></code></div>""");

        checkOutput("pkg1/D.html", false, "shouldNotAppear");
    }

    /*
     * Test with -javafx option enabled, to ensure property getters and setters
     * are treated correctly.
     */
    @Test
    public void test2() {
        javadoc("-d", "out2a",
                "-sourcepath", testSrc,
                "-javafx",
                "--disable-javafx-strict-checks",
                "--no-platform-links",
                "-package",
                "pkg2");
        checkExit(Exit.OK);
        checkOutput("pkg2/Test.html", true,
                """
                    <section class="property-details" id="property-detail">
                    <h2>Property Details</h2>
                    <ul class="member-list">
                    <li>
                    <section class="detail" id="betaProperty">
                    <h3>beta</h3>
                    <div class="member-signature"><span class="modifiers">public</span>&nbsp;<span c\
                    lass="return-type">java.lang.Object</span>&nbsp;<span class="element-name">betaProperty<\
                    /span></div>
                    </section>
                    </li>
                    <li>
                    <section class="detail" id="gammaProperty">
                    <h3>gamma</h3>
                    <div class="member-signature"><span class="modifiers">public final</span>&nbsp;<\
                    span class="return-type">java.util.List&lt;java.lang.String&gt;</span>&nbsp;<spa\
                    n class="element-name">gammaProperty</span></div>
                    </section>
                    </li>
                    <li>
                    <section class="detail" id="deltaProperty">
                    <h3>delta</h3>
                    <div class="member-signature"><span class="modifiers">public final</span>&nbsp;<\
                    span class="return-type">java.util.List&lt;java.util.Set&lt;? super java.lang.Ob\
                    ject&gt;&gt;</span>&nbsp;<span class="element-name">deltaProperty</span></div>
                    </section>
                    </li>
                    </ul>
                    </section>""",
                """
                    <section class="property-summary" id="property-summary">
                    <h2>Property Summary</h2>
                    <div class="caption"><span>Properties</span></div>
                    <div class="summary-table three-column-summary">""");

        checkOutput("pkg2/Test.html", false,
                """
                    <h2>Property Summary</h2>
                    <div id="method-summary-table">
                    <div class="table-tabs" role="tablist" aria-orientation="horizontal">\
                    <button id="method-summary-table-tab0" role="tab" aria-selected="true" aria-cont\
                    rols="method-summary-table.tabpanel" tabindex="0" onkeydown="switchTab(event)" o\
                    nclick="show('method-summary-table', 'method-summary-table', 3)" class="active-t\
                    able-tab">All Methods</button>\
                    <button id="method-summary-table-tab2" role="tab" aria-selected="false" aria-con\
                    trols="method-summary-table.tabpanel" tabindex="-1" onkeydown="switchTab(event)"\
                     onclick="show('method-summary-table', 'method-summary-table-tab2', 3)" class="t\
                    able-tab">Instance Methods</button>\
                    <button id="method-summary-table-tab4" role="tab" aria-selected="false" aria-con\
                    trols="method-summary-table.tabpanel" tabindex="-1" onkeydown="switchTab(event)"\
                     onclick="show('method-summary-table', 'method-summary-table-tab4', 3)" class="t\
                    able-tab">Concrete Methods</button>\
                    </div>""");
    }

    /*
     * Test without -javafx option, to ensure property getters and setters
     * are treated just like any other java method.
     */
    @Test
    public void test3() {
        javadoc("-d", "out2b",
                "-sourcepath", testSrc,
                "--no-platform-links",
                "-package",
                "pkg2");
        checkExit(Exit.OK);
        checkOutput("pkg2/Test.html", false, "<h2>Property Summary</h2>");
        checkOutput("pkg2/Test.html", true,
                """
                    <div class="table-header col-first">Modifier and Type</div>
                    <div class="table-header col-second">Method</div>
                    <div class="table-header col-last">Description</div>
                    <div class="col-first even-row-color method-summary-table method-summary-table-t\
                    ab2 method-summary-table-tab4"><code>&lt;T&gt;&nbsp;java.lang.Object</code></div>
                    <div class="col-second even-row-color method-summary-table method-summary-table-\
                    tab2 method-summary-table-tab4"><code><a href="#alphaProperty(java.util.List)" c\
                    lass="member-name-link">alphaProperty</a><wbr>(java.util.List&lt;T&gt;&nbsp;foo)\
                    </code></div>
                    <div class="col-last even-row-color method-summary-table method-summary-table-ta\
                    b2 method-summary-table-tab4">&nbsp;</div>
                    <div class="col-first odd-row-color method-summary-table method-summary-table-ta\
                    b2 method-summary-table-tab4"><code>java.lang.Object</code></div>
                    <div class="col-second odd-row-color method-summary-table method-summary-table-t\
                    ab2 method-summary-table-tab4"><code><a href="#betaProperty()" class="member-nam\
                    e-link">betaProperty</a>()</code></div>
                    <div class="col-last odd-row-color method-summary-table method-summary-table-tab\
                    2 method-summary-table-tab4">&nbsp;</div>
                    <div class="col-first even-row-color method-summary-table method-summary-table-t\
                    ab2 method-summary-table-tab4"><code>final java.util.List&lt;java.util.Set&lt;? \
                    super java.lang.Object&gt;&gt;</code></div>
                    <div class="col-second even-row-color method-summary-table method-summary-table-\
                    tab2 method-summary-table-tab4"><code><a href="#deltaProperty()" class="member-n\
                    ame-link">deltaProperty</a>()</code></div>
                    <div class="col-last even-row-color method-summary-table method-summary-table-ta\
                    b2 method-summary-table-tab4">&nbsp;</div>
                    <div class="col-first odd-row-color method-summary-table method-summary-table-ta\
                    b2 method-summary-table-tab4"><code>final java.util.List&lt;java.lang.String&gt;\
                    </code></div>
                    <div class="col-second odd-row-color method-summary-table method-summary-table-t\
                    ab2 method-summary-table-tab4"><code><a href="#gammaProperty()" class="member-na\
                    me-link">gammaProperty</a>()</code></div>
                    <div class="col-last odd-row-color method-summary-table method-summary-table-tab\
                    2 method-summary-table-tab4">&nbsp;</div>"""
        );
    }

    /*
     * Force the doclet to emit a warning when processing a synthesized,
     * DocComment, and ensure that the run succeeds, using the newer
     * --javafx flag.
     */
    @Test
    public void test4() {
        javadoc("-d", "out4",
                "--javafx",
                "--disable-javafx-strict-checks",
                "-Xdoclint:none",
                "-sourcepath", testSrc,
                "-package",
                "pkg4");
        checkExit(Exit.OK);

        // make sure the doclet indeed emits the warning
        checkOutput(Output.OUT, true, "C.java:31: warning: invalid input: '<'");
    }

    /*
     * Verify that no warnings are produced on methods that may have synthesized comments.
     */
    @Test
    public void test5() throws IOException {
        Path src5 = Files.createDirectories(Path.of("src5").resolve("pkg"));
        Files.writeString(src5.resolve("MyClass.java"),
                """
                    package pkg;

                    // The following import not required with --disable-javafx-strict-checks
                    // import javafx.beans.property.*;

                    /**
                     * This is my class.
                     */
                    public class MyClass {
                        /**
                         * This is my property that enables something
                         */
                         private BooleanProperty something = new SimpleBooleanProperty(false);

                         public final boolean isSomething() {
                            return something.get();
                         }

                         public final void setSomething(boolean val) {
                            something.set(val);
                         }

                         public final BooleanProperty somethingProperty() {
                            return something;
                         }

                         /** Dummy declaration. */
                         public class BooleanProperty { }
                    }
                    """);

        javadoc("-d", "out5",
                "--javafx",
                "--disable-javafx-strict-checks",
                "--no-platform-links",
                "-Xdoclint:all",
                "--source-path", "src5",
                "pkg");
        checkExit(Exit.OK);

        checkOutput(Output.OUT, false,
                "warning",
                "no comment");
    }
}
