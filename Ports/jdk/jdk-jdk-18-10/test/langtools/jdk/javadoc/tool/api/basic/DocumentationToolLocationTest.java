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
 * @bug 8025844
 * @summary test DocumentationTool.Location methods
 * @modules java.compiler
 *          jdk.compiler
 * @build APITest
 * @run main DocumentationToolLocationTest
 */

import javax.tools.DocumentationTool;
import java.util.Objects;

/**
 * Test for DocumentationTool.Location methods.
 */
public class DocumentationToolLocationTest extends APITest {
    public static void main(String[] args) throws Exception {
        new DocumentationToolLocationTest().run();
    }

    /**
     * Test getName() method
     */
    @Test
    public void testGetName() throws Exception {
        // getName() returns name(). This is for test coverage of getName.
        for (DocumentationTool.Location dl: DocumentationTool.Location.values()) {
            String expect = dl.name();
            String found = dl.getName();
            if (!Objects.equals(expect, found))
                throw new Exception("mismatch for " + dl + "; expected " + expect + ", found " + found);
        }
    }

    /**
     * Test generated enum methods values() and valueOf()
     */
    @Test
    public void testEnumMethods() throws Exception {
        DocumentationTool.Location[] values = DocumentationTool.Location.values();
        if (values.length != 3)
            throw new Exception("unexpected number of values returned");

        for (DocumentationTool.Location dl: values) {
            DocumentationTool.Location expect = dl;
            DocumentationTool.Location found = DocumentationTool.Location.valueOf(dl.name());
            if (!Objects.equals(expect, found))
                throw new Exception("mismatch for " + dl + "; expected " + expect + ", found " + found);
        }
    }
}
