/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import sun.security.util.ManifestDigester;
import org.testng.annotations.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.*;


/**
 * @test
 * @bug 8217375
 * @modules java.base/sun.security.util
 * @run testng FindSections
 * @summary Check {@link ManifestDigester#ManifestDigester} processing
 * individual sections and particularly identifying their names correctly.
 * Main attributes are not covered in this test.
 * <p>
 * See also {@link FindSection} for the {@link ManifestDigester#findSection}
 * method specifically.
 */
public class FindSections {

    static final String DEFAULT_MANIFEST = "Manifest-Version: 1.0\r\n\r\n";

    void test(String manifest, String expectedSection) {
        ManifestDigester md = new ManifestDigester(manifest.getBytes(UTF_8));
        if (expectedSection != null) {
            assertNotNull(md.get(expectedSection, false));
        } else {
            assertNull(md.get(expectedSection, false));
        }
    }

    @Test
    public void testNameNoSpaceAfterColon() throws Exception {
        test(DEFAULT_MANIFEST + "Name:section\r\n\r\n", null);
    }

    @Test
    public void testNameCase() throws Exception {
        test(DEFAULT_MANIFEST + "nAME: section\r\n\r\n", "section");
    }

    @Test
    public void testEmptyName() throws Exception {
        test(DEFAULT_MANIFEST + "Name: \r\n\r\n", "");
    }

    @Test
    public void testShortestInvalidSection() throws Exception {
        test(DEFAULT_MANIFEST + "Name: ", null);
    }

    @Test
    public void testMinimalValidSection() throws Exception {
        test(DEFAULT_MANIFEST + "Name: \r", "");
    }

    @Test
    public void testNameNotContinued() throws Exception {
        test(DEFAULT_MANIFEST + "Name: FooBar\r\n", "FooBar");
    }

    @Test
    public void testImmediatelyContinuedCrName() throws Exception {
        test(DEFAULT_MANIFEST + "Name: \r FooBar\r\n", "FooBar");
    }

    @Test
    public void testImmediatelyContinuedLfName() throws Exception {
        test(DEFAULT_MANIFEST + "Name: \n FooBar\r\n", "FooBar");
    }

    @Test
    public void testImmediatelyContinuedCrLfName() throws Exception {
        test(DEFAULT_MANIFEST + "Name: \r\n FooBar\r\n", "FooBar");
    }

    @Test
    public void testNameContinuedCr() throws Exception {
        test(DEFAULT_MANIFEST + "Name: FooBar\r \r\n", "FooBar");
    }

    @Test
    public void testNameContinuedLf() throws Exception {
        test(DEFAULT_MANIFEST + "Name: FooBar\n \r\n", "FooBar");
    }

    @Test
    public void testNameContinuedCrLf() throws Exception {
        test(DEFAULT_MANIFEST + "Name: FooBar\r\n \r\n", "FooBar");
    }

    @Test
    public void testNameContinuedCrIgnoreNextChar() throws Exception {
        test(DEFAULT_MANIFEST + "Name: Foo\r: Bar\r\n", "Foo");
    }

    @Test
    public void testNameContinuedCrIgnoreNextCharSpace() throws Exception {
        test(DEFAULT_MANIFEST + "Name: Foo\r  Bar\r\n", "Foo Bar");
    }

    @Test
    public void testNameContinuedContinuedCr() throws Exception {
        test(DEFAULT_MANIFEST + "Name: Fo\r\n oB\r ar\r\n", "FooBar");
    }

    @Test
    public void testNameContinuedContinuedLf() throws Exception {
        test(DEFAULT_MANIFEST + "Name: Fo\r\n oB\n ar\r\n", "FooBar");
    }

    @Test
    public void testNameContinuedContinuedCrLf() throws Exception {
        test(DEFAULT_MANIFEST + "Name: Fo\r\n oB\r\n ar\r\n", "FooBar");
    }

    @Test
    public void testNameContinuedEndCr() throws Exception {
        test(DEFAULT_MANIFEST + "Name: Foo\r\n Bar\r", "FooBar");
    }

    @Test
    public void testNameContinuedEndLf() throws Exception {
        test(DEFAULT_MANIFEST + "Name: Foo\r\n Bar\n", "FooBar");
    }

    @Test
    public void testNameContinuedEndCrLf() throws Exception {
        test(DEFAULT_MANIFEST + "Name: Foo\r\n Bar\r\n", "FooBar");
    }

}
