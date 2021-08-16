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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Attributes.Name;
import java.util.stream.Collectors;

import sun.security.util.ManifestDigester;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.*;

/**
 * @test
 * @bug 8217375
 * @modules java.base/sun.security.util
 * @compile ../../tools/jarsigner/Utils.java
 * @run testng DigestInput
 * @summary Checks that the manifest main attributes and entry digests are the
 * same as before resolution of bug 8217375 which means they treat some white
 * space different for oldStyle or digestWorkaround except for the blank line
 * at the end of the manifest file for digestWorkaround.
 */
public class DigestInput {

    /**
     * Filters some test cases for calibrating expected digests with previous
     * implementation. TODO: Delete this after calibrating with old sources.
     */
    static final boolean FIXED_8217375 = true; // FIXME

    /**
     * {@link ManifestDigester.Entry#digestWorkaround} should not feed the
     * trailing blank line into the digester. Before resolution of 8217375 it
     * fed the trailing blank line into the digest if the second line break
     * was at the end of the file due to <pre>
     * if (allBlank || (i == len-1)) {
     *     if (i == len-1)
     *         pos.endOfSection = i;
     *     else
     *         pos.endOfSection = last;
     * </pre> in {@link ManifestDigester#findSection}. In that case at the end
     * of the manifest file, {@link ManifestDigester.Entry#digestWorkaround}
     * would have produced the same digest as
     * {@link ManifestDigester.Entry#digest} which was wrong and without effect
     * at best.
     * <p>
     * Once this fix is accepted, this flag can be removed along with
     * {@link #assertDigestEqualsCatchWorkaroundBroken}.
     */
    static final boolean FIXED_8217375_EOF_ENDOFSECTION = FIXED_8217375;

    static final String SECTION_NAME = "some individual section name";

    @DataProvider(name = "parameters")
    public static Object[][] parameters() {
        List<Object[]> tests = new ArrayList<>();
        for (String lineBreak : new String[] { "\n", "\r", "\r\n" }) {
            if ("\r".equals(lineBreak) && !FIXED_8217375) continue;
            for (int addLB = 0; addLB <= 4; addLB++) {
                for (int numSecs = 0; numSecs <= 4; numSecs++) {
                    for (boolean otherSec : new Boolean[] { false, true }) {
                        for (boolean oldStyle : new Boolean[] { false, true }) {
                            for (boolean workaround :
                                    new Boolean[] { false, true }) {
                                tests.add(new Object[] {
                                    lineBreak, addLB, numSecs, otherSec,
                                            oldStyle, workaround
                                });
                            }
                        }
                    }
                }
            }
        }
        return tests.toArray(new Object[tests.size()][]);
    }

    @Factory(dataProvider = "parameters")
    public static Object[] createTests(
            String lineBreak, int additionalLineBreaks,
            int numberOfSections, boolean hasOtherSection,
            boolean oldStyle, boolean digestWorkaround) {
        return new Object[] { new DigestInput(lineBreak,
                additionalLineBreaks, numberOfSections, hasOtherSection,
                oldStyle, digestWorkaround)
        };
    }

    final String lineBreak;
    final int additionalLineBreaks; // number of blank lines delimiting section
    final int numberOfSections;
    final boolean hasOtherSection;
    final boolean oldStyle;
    final boolean digestWorkaround;

    public DigestInput(
            String lineBreak, int additionalLineBreaks,
            int numberOfSections, boolean hasOtherSection,
            boolean oldStyle, boolean digestWorkaround) {
        this.lineBreak = lineBreak;
        this.additionalLineBreaks = additionalLineBreaks;
        this.numberOfSections = numberOfSections;
        this.hasOtherSection = hasOtherSection;
        this.oldStyle = oldStyle;
        this.digestWorkaround = digestWorkaround;
    }

    @BeforeMethod
    public void verbose() {
        System.out.println("-".repeat(72));
        System.out.println("lineBreak = " +
                Utils.escapeStringWithNumbers(lineBreak));
        System.out.println("additionalLineBreaks = " + additionalLineBreaks);
        System.out.println("numberOfSections = " + numberOfSections);
        System.out.println("hasOtherSection = " + hasOtherSection);
        System.out.println("oldStyle = " + oldStyle);
        System.out.println("digestWorkaround = " + digestWorkaround);
        System.out.println("-".repeat(72));
    }

    byte[] rawManifestBytes() {
        return (
            Name.MANIFEST_VERSION + ": 1.0" + lineBreak +
            "OldStyle0: no trailing space" + lineBreak +
            "OldStyle1: trailing space " + lineBreak +
            "OldStyle2: two trailing spaces  " + lineBreak +
                    lineBreak.repeat(additionalLineBreaks) +
            (
                "Name: " + SECTION_NAME + lineBreak +
                "OldStyle0: no trailing space" + lineBreak +
                "OldStyle1: trailing space " + lineBreak +
                "OldStyle2: two trailing spaces  " + lineBreak +
                lineBreak.repeat(additionalLineBreaks)
            ).repeat(numberOfSections) +
            (hasOtherSection ?
                "Name: unrelated trailing section" + lineBreak +
                "OldStyle0: no trailing space" + lineBreak +
                "OldStyle1: trailing space " + lineBreak +
                "OldStyle2: two trailing spaces  " + lineBreak +
                lineBreak.repeat(additionalLineBreaks)
            : "")
        ).getBytes(UTF_8);
    }

    byte[] expectedMainAttrsDigest(boolean digestWorkaround) {
        return (
            Name.MANIFEST_VERSION + ": 1.0" + lineBreak +
            "OldStyle0: no trailing space" + lineBreak +
            "OldStyle1: trailing space" +
                (!oldStyle || !lineBreak.startsWith("\r") || digestWorkaround ?
                    " " : "") + lineBreak +
            "OldStyle2: two trailing spaces " +
                (!oldStyle || !lineBreak.startsWith("\r") || digestWorkaround ?
                        " " : "") + lineBreak +
            (
                     (
                                 !digestWorkaround
                         || (
                                 additionalLineBreaks == 1
                              && numberOfSections == 0
                              && !hasOtherSection
                              && (
                                      digestWorkaround
                                   && !FIXED_8217375_EOF_ENDOFSECTION
                                 )
                         )
                ) && (
                            additionalLineBreaks > 0
                         || numberOfSections > 0
                         || hasOtherSection
                )
            ? lineBreak : "")
        ).getBytes(UTF_8);
    }

    byte[] expectedIndividualSectionDigest(boolean digestWorkaround) {
        if (numberOfSections == 0) return null;
        return (
            (
                "Name: " + SECTION_NAME + lineBreak +
                "OldStyle0: no trailing space" + lineBreak +
                "OldStyle1: trailing space" +
                    (!oldStyle || !lineBreak.startsWith("\r")
                            || digestWorkaround ? " " : "") + lineBreak +
                "OldStyle2: two trailing spaces " +
                    (!oldStyle || !lineBreak.startsWith("\r")
                            || digestWorkaround ? " " : "") + lineBreak +
                (
                    (
                           !digestWorkaround
                    ) && (
                           additionalLineBreaks > 0
                    )
                ? lineBreak : "")
            ).repeat(numberOfSections) +
            (
                   additionalLineBreaks == 1
                && !hasOtherSection
                && digestWorkaround
                && !FIXED_8217375_EOF_ENDOFSECTION
            ? lineBreak : "")
        ).getBytes(UTF_8);
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

    byte[] digestMainAttributes(byte[] mfBytes) throws Exception {
        Utils.echoManifest(mfBytes, "going to digest main attributes of");

        ManifestDigester md = new ManifestDigester(mfBytes);
        ManifestDigester.Entry entry =
                md.get(ManifestDigester.MF_MAIN_ATTRS, oldStyle);
        MessageDigest digester = new EchoMessageDigest();
        return digestWorkaround ?
                entry.digestWorkaround(digester) : entry.digest(digester);
    }

    byte[] digestIndividualSection(byte[] mfBytes) throws Exception {
        Utils.echoManifest(mfBytes,
                "going to digest section " + SECTION_NAME + " of");

        ManifestDigester md = new ManifestDigester(mfBytes);
        ManifestDigester.Entry entry = md.get(SECTION_NAME, oldStyle);
        if (entry == null) {
            return null;
        }
        MessageDigest digester = new EchoMessageDigest();
        return digestWorkaround ?
                entry.digestWorkaround(digester) : entry.digest(digester);
    }


    /**
     * Checks that the manifest main attributes digest is the same as before.
     */
    @Test
    public void testMainAttributesDigest() throws Exception {
        byte[] mfRaw = rawManifestBytes();
        byte[] digest = digestMainAttributes(mfRaw);
        byte[] expectedDigest = expectedMainAttrsDigest(digestWorkaround);

        // the individual section will be digested along with the main
        // attributes if not properly delimited with a blank line
        if (additionalLineBreaks == 0
                && (numberOfSections > 0 || hasOtherSection)) {
            assertNotEquals(digest, expectedDigest);
            return;
        }

        byte[] expectedDigestNoWorkaround = expectedMainAttrsDigest(false);

//        assertDigestEquals(digest, expectedDigest); // FIXME
        assertDigestEqualsCatchWorkaroundBroken(
                digest, expectedDigest, expectedDigestNoWorkaround);
    }

    /**
     * Checks that an individual section digest is the same as before.
     */
    @Test
    public void testIndividualSectionDigest() throws Exception {
        byte[] mfRaw = rawManifestBytes();
        byte[] digest = digestIndividualSection(mfRaw);

        // no digest will be produced for an individual section that is not
        // properly section delimited with a blank line.
        byte[] expectedDigest =
                additionalLineBreaks == 0 ? null :
                    expectedIndividualSectionDigest(digestWorkaround);

        byte[] expectedDigestNoWorkaround =
                additionalLineBreaks == 0 ? null :
                    expectedIndividualSectionDigest(false);

//      assertDigestEquals(digest, expectedDigest); // FIXME
        assertDigestEqualsCatchWorkaroundBroken(
                digest, expectedDigest, expectedDigestNoWorkaround);
    }

    static int firstDiffPos = Integer.MAX_VALUE;

    /**
     * @see FIXED_8217375_EOF_ENDOFSECTION
     */
    void assertDigestEqualsCatchWorkaroundBroken(
            byte[] actual, byte[] expected, byte[] expectedNoWorkaround)
                    throws IOException {
        try {
            assertDigestEquals(actual, expected);
        } catch (AssertionError e) {
            if (digestWorkaround && FIXED_8217375_EOF_ENDOFSECTION &&
                    Arrays.equals(expected, expectedNoWorkaround)) {
                // if digests with and without workaround are the same anyway
                // the workaround has failed and could not have worked with
                // the same digest as produced without workaround before
                // which would not match either because equal.
                return;
            }
            fail("failed also without digestWorkaound", e);
        }
    }

    void assertDigestEquals(byte[] actual, byte[] expected) throws IOException {
        if (actual == null && expected == null) return;
        Utils.echoManifest(actual, "actual digest");
        Utils.echoManifest(expected, "expected digest");
        for (int i = 0; i < actual.length && i < expected.length; i++) {
            if (actual[i] != expected[i]) {
                firstDiffPos = Math.min(firstDiffPos, i);
                verbose();
                fail("found first difference in current test"
                        + " at position " + i);
            }
        }
        if (actual.length != expected.length) {
            int diffPos = Math.min(actual.length, expected.length);
            firstDiffPos = Math.min(firstDiffPos, diffPos);
            verbose();
            fail("found first difference in current test"
                    + " at position " + diffPos + " after one digest end");
        }
        assertEquals(actual, expected);
    }

    @AfterTest
    public void reportFirstDiffPos() {
        System.err.println("found first difference in all tests"
                + " at position " + firstDiffPos);
    }

}
