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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import sun.security.util.ManifestDigester;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.*;

/**
 * @test
 * @bug 8217375
 * @modules java.base/sun.security.util
 * @compile ../../tools/jarsigner/Utils.java
 * @run testng LineBreaks
 * @summary Verify {@code ManifestDigester} reads different line breaks well.
 * The specifications state:
 * <q><pre>newline: CR LF | LF | CR (not followed by LF)</pre></q>.
 * This test does not verify that the digests are correct.
 */
public class LineBreaks {

    static final String KEY = "Key";
    static final String VALUE = "Value";
    static final String SECTION = "Section";
    static final String FOO = "Foo";
    static final String BAR = "Bar";

    static final String EXCEED_LINE_WIDTH_LIMIT = "x".repeat(71);

    String breakAndContinue(String str, String lineBreak) {
        // assert no multi-byte UTF-8 encoded characters in this test
        assertEquals(str.getBytes(UTF_8).length, str.length());

        int p = 1;
        while (p + 71 < str.length()) {
            p += 71;
            str = str.substring(0, p) + lineBreak + " " + str.substring(p);
            p += lineBreak.length() + 1;
        }
        return str;
    }

    byte[] createTestManifest(String lineBreak, boolean onlyMainAttrs,
            String excess) throws IOException {
        System.out.println("lineBreak = "
                + Utils.escapeStringWithNumbers(lineBreak));
        System.out.println("onlyMainAttrs = " + onlyMainAttrs);
        String mf = "";
        mf += breakAndContinue(
                KEY + ": " + VALUE + excess, lineBreak) + lineBreak;
        mf += lineBreak;
        if (!onlyMainAttrs) {
            mf += breakAndContinue(
                    "Name: " + SECTION + excess, lineBreak) + lineBreak;
            mf += breakAndContinue(
                    FOO + ": " + BAR + excess, lineBreak) + lineBreak;
            mf += lineBreak;
        }
        byte[] mfBytes = mf.getBytes(UTF_8);
        Utils.echoManifest(mfBytes, "binary manifest");
        return mfBytes;
    }

    @DataProvider(name = "parameters")
    public static Object[][] parameters() {
        List<Object[]> tests = new ArrayList<>();
        for (String lineBreak : new String[] { "\n", "\r", "\r\n" }) {
            for (boolean onlyMainAttrs : new boolean[] { false, true }) {
                for (int numLbs = 0; numLbs < 3; numLbs++) {
                    tests.add(new Object[]{ lineBreak, onlyMainAttrs, numLbs });
                }
            }
        }
        return tests.toArray(new Object[tests.size()][]);
    }

    @Test(dataProvider = "parameters")
    public void test(String lineBreak, boolean onlyMainAttrs, int numLbs)
            throws IOException {
        String excess = EXCEED_LINE_WIDTH_LIMIT.repeat(numLbs);
        byte[] mfBytes = createTestManifest(lineBreak, onlyMainAttrs, excess);

        // self-test: make sure the manifest is valid and represents the
        // values as expected before attempting to digest it
        Manifest mf = new Manifest(new ByteArrayInputStream(mfBytes));
        assertEquals(mf.getMainAttributes().getValue(KEY), VALUE + excess);
        Attributes section = mf.getAttributes(SECTION + excess);
        if (onlyMainAttrs) {
            assertNull(section);
        } else {
            assertEquals(section.getValue(FOO), BAR + excess);
        }

        // verify that ManifestDigester has actually found the individual
        // section if and only if it was present thereby also implying based
        // on ManifestDigester implementation that the main attributes were
        // found before
        ManifestDigester md = new ManifestDigester(mfBytes);
        assertTrue((md.get(SECTION + excess, false) != null) != onlyMainAttrs);
    }

    static List<Integer> stringToIntList(String string) {
        byte[] bytes = string.getBytes(UTF_8);
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < bytes.length; i++) {
            list.add((int) bytes[i]);
        }
        return list;
    }

}
