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
 * @bug 6493690
 * @summary javadoc should have a javax.tools.Tool service provider
 * @modules java.compiler
 *          jdk.compiler
 * @build APITest
 * @run main GetSourceVersionsTest
 */

import java.util.EnumSet;
import java.util.Set;
import javax.lang.model.SourceVersion;
import javax.tools.DocumentationTool;
import javax.tools.ToolProvider;

/**
 * Tests for DocumentationTool.getSourceVersions method.
 */
public class GetSourceVersionsTest extends APITest {
    public static void main(String... args) throws Exception {
        new GetSourceVersionsTest().run();
    }

    /**
     * Verify getSourceVersions.
     */
    @Test
    public void testRun() throws Exception {
        DocumentationTool tool = ToolProvider.getSystemDocumentationTool();
        Set<SourceVersion> found = tool.getSourceVersions();
        Set<SourceVersion> expect = EnumSet.range(SourceVersion.RELEASE_3, SourceVersion.latest());
        if (!expect.equals(found)) {
            System.err.println("expect: " + expect);
            System.err.println(" found: " + expect);
            error("unexpected versions");
        }
    }
}

