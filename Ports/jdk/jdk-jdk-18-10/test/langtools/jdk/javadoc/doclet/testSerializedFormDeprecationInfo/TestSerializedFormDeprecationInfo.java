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
 * @bug 6802694 8025633 8026567 8183511 8074407 8182765 8232644
 * @summary This test verifies deprecation info in serialized-form.html.
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main TestSerializedFormDeprecationInfo
 */

import javadoc.tester.JavadocTester;

public class TestSerializedFormDeprecationInfo extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestSerializedFormDeprecationInfo tester = new TestSerializedFormDeprecationInfo();
        tester.runTests();
    }

    @Test
    public void testDefault() {
        javadoc("-d", "out-default",
                "-sourcepath", testSrc,
                "--no-platform-links",
                "pkg1");
        checkExit(Exit.OK);

        checkCommentDeprecated(true);
        checkNoComment(false);
    }

    @Test
    public void testNoComment() {
        javadoc("-d", "out-nocmnt",
                "-nocomment",
                "-sourcepath", testSrc,
                "pkg1");
        checkExit(Exit.OK);

        checkNoComment(true);
        checkCommentDeprecated(false);
    }

    @Test
    public void testNoDeprecated() {
        javadoc("-d", "out-nodepr",
                "-nodeprecated",
                "-sourcepath", testSrc,
                "--no-platform-links",
                "pkg1");
        checkExit(Exit.OK);

        checkNoDeprecated(true);
        checkNoCommentNoDeprecated(false);
    }

    @Test
    public void testNoCommentNoDeprecated() {
        javadoc("-d", "out-nocmnt-nodepr",
                "-nocomment",
                "-nodeprecated",
                "-sourcepath", testSrc,
                "pkg1");
        checkExit(Exit.OK);
        checkNoCommentNoDeprecated(true);
        checkNoDeprecated(false);
    }

    // Test for normal run of javadoc. The serialized-form.html should
    // display the inline comments, tags and deprecation information if any.
    void checkCommentDeprecated(boolean expectFound) {
        checkOutput("serialized-form.html", expectFound,
                """
                    <dl class="notes">
                    <dt>Throws:</dt>
                    <dd><code>java.io.IOException</code> - on error</dd>
                    <dt>See Also:</dt>
                    <dd>
                    <ul class="see-list">
                    <li><a href="pkg1/C1.html#setUndecorated(boolean)"><code>C1.setUndecorated(boolean)</code></a></li>
                    </ul>
                    </dd>
                    </dl>""",
                """
                    <span class="deprecated-label">Deprecated.</span>
                    <div class="deprecation-comment">As of JDK version 1.5, replaced by
                     <a href="pkg1/C1.html#setUndecorated(boolean)"><code>setUndecorated(boolean)</code></a>.</div>
                    </div>
                    <div class="block">This field indicates whether the C1 is undecorated.</div>
                    <dl class="notes">
                    <dt>Since:</dt>
                    <dd>1.4</dd>
                    <dt>See Also:</dt>
                    <dd>
                    <ul class="see-list">
                    <li><a href="pkg1/C1.html#setUndecorated(boolean)"><code>C1.setUndecorated(boolean)</code></a></li>
                    </ul>
                    </dd>
                    </dl>""",
                """
                    <span class="deprecated-label">Deprecated.</span>
                    <div class="deprecation-comment">As of JDK version 1.5, replaced by
                     <a href="pkg1/C1.html#setUndecorated(boolean)"><code>setUndecorated(boolean)</code></a>.</div>
                    </div>
                    <div class="block">Reads the object stream.</div>
                    <dl class="notes">
                    <dt>Parameters:</dt>
                    <dd><code>s</code> - ObjectInputStream</dd>
                    <dt>Throws:</dt>
                    <dd><code>java.io.IOException</code> - on error</dd>
                    </dl>""",
                """
                    <span class="deprecated-label">Deprecated.</span></div>
                    <div class="block">The name for this class.</div>""");
    }

    // Test with -nocomment option. The serialized-form.html should
    // not display the inline comments and tags but should display deprecation
    // information if any.
    void checkNoComment(boolean expectFound) {
        checkOutput("serialized-form.html", expectFound,
                """
                    <pre>boolean undecorated</pre>
                    <div class="deprecation-block"><span class="deprecated-label">Deprecated.</span>
                    <div class="deprecation-comment">As of JDK version 1.5, replaced by
                     <a href="pkg1/C1.html#setUndecorated(boolean)"><code>setUndecorated(boolean)</code></a>.</div>
                    </div>
                    </li>""",
                """
                    <span class="deprecated-label">Deprecated.</span>
                    <div class="deprecation-comment">As of JDK version 1.5, replaced by
                     <a href="pkg1/C1.html#setUndecorated(boolean)"><code>setUndecorated(boolean)</code></a>.</div>
                    </div>
                    </li>""");
    }

    // Test with -nodeprecated option. The serialized-form.html should
    // ignore the -nodeprecated tag and display the deprecation info. This
    // test is similar to the normal run of javadoc in which inline comment, tags
    // and deprecation information will be displayed.
    void checkNoDeprecated(boolean expectFound) {
        checkCommentDeprecated(expectFound);
    }

    // Test with -nodeprecated and -nocomment options. The serialized-form.html should
    // ignore the -nodeprecated tag and display the deprecation info but should not
    // display the inline comments and tags. This test is similar to the test with
    // -nocomment option.
    void checkNoCommentNoDeprecated(boolean expectFound) {
        checkNoComment(expectFound);
    }
}
