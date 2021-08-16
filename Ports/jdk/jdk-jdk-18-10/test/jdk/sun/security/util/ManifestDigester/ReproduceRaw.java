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
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.jar.Manifest;

import sun.security.util.ManifestDigester;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.*;

/**
 * @test
 * @bug 8217375
 * @modules java.base/sun.security.util
 * @compile ../../tools/jarsigner/Utils.java
 * @run testng ReproduceRaw
 * @summary Verifies that {@link ManifestDigester} can reproduce parts of
 * manifests in their binary form so that {@link JarSigner} can rely on
 * {@link ManifestDigester.Entry#reproduceRaw} to write in a map view
 * unmodified entries back also unmodified in their binary form.
 * <p>
 * See also<ul>
 * <li>{@link PreserveRawManifestEntryAndDigest} with end to end tests
 * with {@code jarsigner} tool and</li>
 * <li>{@link FindHeaderEndVsManifestDigesterFindFirstSection} about
 * identifying the binary portion of only main attributes and more extensive
 * main attributes digesting tests while this one test here is more about
 * reproducing individual sections and that they result in the same
 * digests.</li>
 * </ul>
 */
public class ReproduceRaw {

    static final boolean VERBOSE = false;

    @DataProvider(name = "parameters")
    public static Object[][] parameters() {
        List<Object[]> tests = new ArrayList<>();
        for (String lineBreak : new String[] { "\n", "\r", "\r\n" }) {
            for (boolean oldStyle : new Boolean[] { false, true }) {
                for (boolean workaround : new Boolean[] { false, true }) {
                    tests.add(new Object[] { lineBreak, oldStyle, workaround });
                }
            }
        }
        return tests.toArray(new Object[tests.size()][]);
    }

    @Factory(dataProvider = "parameters")
    public static Object[] createTests(String lineBreak,
            boolean oldStyle, boolean digestWorkaround) {
        return new Object[]{
                new ReproduceRaw(lineBreak, oldStyle, digestWorkaround)
        };
    }

    final String lineBreak;
    final boolean oldStyle;
    final boolean digestWorkaround;

    public ReproduceRaw(String lineBreak,
            boolean oldStyle, boolean digestWorkaround) {
        this.lineBreak = lineBreak;
        this.oldStyle = oldStyle;
        this.digestWorkaround = digestWorkaround;
    }

    @BeforeMethod
    public void verbose() {
        System.out.println("lineBreak = " +
                Utils.escapeStringWithNumbers(lineBreak));
        System.out.println("oldStyle = " + oldStyle);
        System.out.println("digestWorkaround = " + digestWorkaround);
    }

    class EchoMessageDigest extends MessageDigest {

        ByteArrayOutputStream buf;

        EchoMessageDigest() {
            super("echo");
        }

        @Override
        protected void engineReset() {
            buf = new ByteArrayOutputStream();
        }

        @Override
        protected void engineUpdate(byte input) {
            buf.write(input);
        }

        @Override
        protected void engineUpdate(byte[] i, int o, int l) {
            buf.write(i, o, l);
        }

        @Override protected byte[] engineDigest() {
            return buf.toByteArray();
        }

    }

    /**
     * similar to corresponding part of {@link JarSigner#sign0}
     * (stripped down to the code for reproducing the old manifest entry by
     * entry which was too difficult to achieve using the real JarSigner code
     * in the test here)
     */
    byte[] reproduceRawManifest(byte[] mfRawBytes,
            boolean mainAttsProperlyDelimited,
            boolean sectionProperlyDelimited) throws IOException {
        Manifest manifest = new Manifest(new ByteArrayInputStream(mfRawBytes));
        ManifestDigester oldMd = new ManifestDigester(mfRawBytes);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // main attributes
        assertEquals(oldMd.getMainAttsEntry().isProperlyDelimited(),
                mainAttsProperlyDelimited);
        oldMd.getMainAttsEntry().reproduceRaw(baos);

        // individual sections
        for (String key : manifest.getEntries().keySet()) {
            assertEquals(oldMd.get(key).isProperlyDelimited(),
                    sectionProperlyDelimited);
            oldMd.get(key).reproduceRaw(baos);
        }

        return baos.toByteArray();
    }

    static String regExscape(String expr) {
        for (int i = 0; i < expr.length(); i++) {
            if (expr.charAt(i) == '\r' || expr.charAt(i) == '\n') {
                expr = expr.substring(0, i) + "\\" + expr.substring(i++);
            }
        }
        return expr;
    }

    byte[] digest(byte[] manifest, String section) {
        MessageDigest digester = new EchoMessageDigest();
        ManifestDigester md = new ManifestDigester(manifest);
        ManifestDigester.Entry entry = section == null ?
                md.getMainAttsEntry(oldStyle) : md.get(section, oldStyle);
        return digestWorkaround ?
                entry.digestWorkaround(digester) :
                entry.digest(digester);
    }

    void test(byte[] originalManifest, boolean mainAttsProperlyDelimited,
            boolean sectionProperlyDelimited) throws Exception {
        Utils.echoManifest(originalManifest, "original manifest");
        byte[] reproducedManifest = reproduceRawManifest(originalManifest,
                mainAttsProperlyDelimited, sectionProperlyDelimited);
        Utils.echoManifest(reproducedManifest, "reproduced manifest");

        // The reproduced manifest is not necessarily completely identical to
        // the original if it contained superfluous blank lines.
        // It's sufficient that the digests are equal and as an additional
        // check, the reproduced manifest is here compared to the original
        // without more than double line breaks.
        if (!lineBreak.repeat(2).equals(new String(originalManifest, UTF_8))) {
            assertEquals(
                new String(reproducedManifest, UTF_8),
                new String(originalManifest, UTF_8).replaceAll(
                    regExscape(lineBreak) + "(" + regExscape(lineBreak) + ")+",
                    lineBreak.repeat(2)));
        }

        // compare digests of reproduced manifest entries with digests of
        // original manifest entries
        assertEquals(digest(originalManifest, null),
                digest(reproducedManifest, null));
        for (String key : new Manifest(new ByteArrayInputStream(
                originalManifest)).getEntries().keySet()) {
            assertEquals(digest(originalManifest, key),
                    digest(reproducedManifest, key));
        }

        // parse and compare original and reproduced manifests as manifests
        assertEquals(new Manifest(new ByteArrayInputStream(originalManifest)),
                new Manifest(new ByteArrayInputStream(reproducedManifest)));
    }

    void test(byte[] originalManifest, boolean mainAttsProperlyDelimited)
            throws Exception {
        // assert all individual sections properly delimited particularly useful
        // when no individual sections present
        test(originalManifest, mainAttsProperlyDelimited, true);
    }

    @Test
    public void testManifestStartsWithBlankLine() throws Exception {
        test(lineBreak.getBytes(UTF_8), true);
        test(lineBreak.repeat(2).getBytes(UTF_8), true);
    }

    @Test
    public void testEOFAndNoLineBreakAfterMainAttributes() throws Exception {
        assertThrows(RuntimeException.class, () ->
            test("Manifest-Version: 1.0".getBytes(UTF_8), false)
        );
    }

    @Test
    public void testEOFAndNoBlankLineAfterMainAttributes() throws Exception {
        test(("Manifest-Version: 1.0" + lineBreak).getBytes(UTF_8), false);
    }

    @Test
    public void testNormalMainAttributes() throws Exception {
        test(("Manifest-Version: 1.0" +
                lineBreak.repeat(2)).getBytes(UTF_8), true);
    }

    @Test
    public void testExtraLineBreakAfterMainAttributes() throws Exception {
        test(("Manifest-Version: 1.0" +
                lineBreak.repeat(3)).getBytes(UTF_8), true);
    }

    @Test
    public void testIndividualSectionNoLineBreak() throws Exception {
        assertNull(new ManifestDigester((
                "Manifest-Version: 1.0" + lineBreak +
                lineBreak +
                "Name: Section-Name" + lineBreak +
                "Key: Value"
        ).getBytes(UTF_8)).get("Section-Name"));
    }

    @Test
    public void testIndividualSectionOneLineBreak() throws Exception {
        test((
                "Manifest-Version: 1.0" + lineBreak +
                lineBreak +
                "Name: Section-Name" + lineBreak +
                "Key: Value" + lineBreak
        ).getBytes(UTF_8), true, false);
    }

    @Test
    public void testNormalIndividualSectionTwoLineBreak() throws Exception {
        test((
                "Manifest-Version: 1.0" + lineBreak +
                lineBreak +
                "Name: Section-Name" + lineBreak +
                "Key: Value" + lineBreak.repeat(2)
        ).getBytes(UTF_8), true, true);
    }

    @Test
    public void testExtraLineBreakAfterIndividualSection() throws Exception {
        test((
                "Manifest-Version: 1.0" + lineBreak +
                lineBreak +
                "Name: Section-Name" + lineBreak +
                "Key: Value" + lineBreak.repeat(3)
                ).getBytes(UTF_8), true, true);
    }

    @Test
    public void testIndividualSections() throws Exception {
        test((
                "Manifest-Version: 1.0" + lineBreak +
                lineBreak +
                "Name: Section-Name" + lineBreak +
                "Key: Value" + lineBreak +
                lineBreak +
                "Name: Section-Name" + lineBreak +
                "Key: Value" + lineBreak +
                lineBreak
                ).getBytes(UTF_8), true, true);
    }

    @Test
    public void testExtraLineBreakBetweenIndividualSections() throws Exception {
        test((
                "Manifest-Version: 1.0" + lineBreak +
                lineBreak +
                "Name: Section-Name" + lineBreak +
                "Key: Value" + lineBreak +
                lineBreak.repeat(2) +
                "Name: Section-Name" + lineBreak +
                "Key: Value" + lineBreak +
                lineBreak
                ).getBytes(UTF_8), true, true);
    }

}
