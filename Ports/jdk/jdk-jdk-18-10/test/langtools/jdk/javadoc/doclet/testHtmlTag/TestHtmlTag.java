/*
 * Copyright (c) 2009, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6786682 4649116 8182765 8261976
 * @summary This test verifies the use of lang attribute by <HTML>.
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main TestHtmlTag
 */

import java.util.Locale;

import javadoc.tester.JavadocTester;

public class TestHtmlTag extends JavadocTester {
    private static final String defaultLanguage = Locale.getDefault().getLanguage();
    public static void main(String... args) throws Exception {
        TestHtmlTag tester = new TestHtmlTag();
        tester.runTests();
    }
    @Test
    public void test_default() {
        javadoc("-locale", defaultLanguage,
                "-d", "out-default",
                "-sourcepath", testSrc,
                "pkg1");

        checkExit(Exit.OK);

        checkOutput("pkg1/C1.html", true,
            "<html lang=\"" + defaultLanguage + "\">");

        checkOutput("pkg1/package-summary.html", true,
            "<html lang=\"" + defaultLanguage + "\">");

        checkOutput("pkg1/C1.html", false,
                "<html>");
    }

    @Test
    public void test_ja() {
        // TODO: why does this test need/use pkg2; why can't it use pkg1
        // like the other two tests, so that we can share the check methods?
        javadoc("-locale", "ja",
                "-d", "out-ja",
                "-sourcepath", testSrc,
                "pkg2");
        checkExit(Exit.OK);

        checkOutput("pkg2/C2.html", true,
                "<html lang=\"ja\">");

        checkOutput("pkg2/package-summary.html", true,
                "<html lang=\"ja\">");

        checkOutput("pkg2/C2.html", false,
                "<html>");
    }

    @Test
    public void test_en_US() {
        javadoc("-locale", "en_US",
                "-d", "out-en_US",
                "-sourcepath", testSrc,
                "pkg1");
        checkExit(Exit.OK);

        checkOutput("pkg1/C1.html", true,
                "<html lang=\"en\">");

        checkOutput("pkg1/package-summary.html", true,
                "<html lang=\"en\">");

        checkOutput("pkg1/C1.html", false,
                "<html>");
    }

    @Test
    public void test_other() {
        javadoc("-locale", "en_US",
                "--no-platform-links",
                "-d", "out-other",
                "-sourcepath", testSrc,
                "pkg3");
        checkExit(Exit.OK);

        checkOutput("pkg3/package-summary.html", true,
                """
                    <section class="package-description" id="package-description">
                    <div class="block"><p>This is the first line. Note the newlines before the &lt;p&gt; is relevant.</div>
                    </section>""");

        checkOutput("pkg3/A.DatatypeFactory.html", true,
                """
                    <div class="block"><p>
                     Factory that creates new <code>javax.xml.datatype</code>
                     <code>Object</code>s that map XML to/from Java <code>Object</code>s.</p>

                     <p id="DatatypeFactory.newInstance">
                     A new instance of the <code>DatatypeFactory</code> is created through the
                     <a href="#newInstance()"><code>newInstance()</code></a> method that uses the following implementation
                     resolution mechanisms to determine an implementation:</p>
                     <ol>
                     <li>
                     If the system property specified by <a href="#DATATYPEFACTORY_PROPERTY"><code>DATATYPEFACTORY_PROPERTY</code></a>,
                     "<code>javax.xml.datatype.DatatypeFactory</code>", exists, a class with
                     the name of the property value is instantiated. Any Exception thrown
                     during the instantiation process is wrapped as a
                     <code>IllegalStateException</code>.
                     </li>
                     <li>
                     If the file ${JAVA_HOME}/lib/jaxp.properties exists, it is loaded in a
                     <code>Properties</code> <code>Object</code>. The
                     <code>Properties</code> <code>Object </code> is then queried for the
                     property as documented in the prior step and processed as documented in
                     the prior step.
                     </li>
                     <li>
                     Uses the service-provider loading facilities, defined by the
                     <code>ServiceLoader</code> class, to attempt to locate and load an
                     implementation of the service using the default loading mechanism:
                     the service-provider loading facility will use the current thread's context class loader
                     to attempt to load the service. If the context class loader is null, the system class loader will be used.
                     <br>
                     In case of <code>service configuration error</code> a
                     <code>DatatypeConfigurationException</code> will be thrown.
                     </li>
                     <li>
                     The final mechanism is to attempt to instantiate the <code>Class</code>
                     specified by <a href="#DATATYPEFACTORY_IMPLEMENTATION_CLASS"><code>DATATYPEFACT\
                    ORY_IMPLEMENTATION_CLASS</code></a>. Any Exception
                     thrown during the instantiation process is wrapped as a
                     <code>IllegalStateException</code>.
                     </li>
                     </ol></div>""");

        checkOutput("pkg3/A.ActivationDesc.html", true,
                """
                    <div class="type-signature"><span class="modifiers">public class </span><span cl\
                    ass="element-name type-name-label">A.ActivationDesc</span>
                    <span class="extends-implements">extends java.lang.Object
                    implements java.io.Serializable</span></div>
                    <div class="block">An activation descriptor contains the information necessary to activate
                     an object: <ul>
                     <li> the object's group identifier,
                     <li> the object's fully-qualified class name,
                     <li> the object's code location (the location of the class), a codebase
                     URL path,
                     <li> the object's restart "mode", and,
                     <li> a "marshalled" object that can contain object specific
                     initialization data. </ul>

                     <p>
                     A descriptor registered with the activation system can be used to
                     recreate/activate the object specified by the descriptor. The
                     <code>MarshalledObject</code> in the object's descriptor is passed as the
                     second argument to the remote object's constructor for object to use
                     during reinitialization/activation.</div>""");

        checkOutput("pkg3/A.ActivationGroupID.html", true,
                """
                    <div class="type-signature"><span class="modifiers">public class </span><span cl\
                    ass="element-name type-name-label">A.ActivationGroupID</span>
                    <span class="extends-implements">extends java.lang.Object
                    implements java.io.Serializable</span></div>
                    <div class="block">The identifier for a registered activation group serves several purposes:
                     <ul>
                     <li>identifies the group uniquely within the activation system, and
                     <li>contains a reference to the group's activation system so that the
                     group can contact its activation system when necessary.</ul><p>

                     The <code>ActivationGroupID</code> is returned from the call to
                     <code>ActivationSystem.registerGroup</code> and is used to identify the
                     group within the activation system. This group id is passed as one of the
                     arguments to the activation group's special constructor when an
                     activation group is created/recreated.</div>
                    <dl class="notes">""");
    }
}
