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
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import sun.security.util.ManifestDigester;

import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.*;

/**
 * @test
 * @bug 8217375
 * @modules java.base/sun.security.util
 * @run testng FindHeaderEndVsManifestDigesterFindFirstSection
 * @summary Checks that {@link JarSigner#findHeaderEnd} (moved to now
 * {@link #findHeaderEnd} in this test) can be replaced with
 * {@link ManifestDigester#findSection}
 * (first invocation will identify main attributes)
 * without making a difference.
 */
/*
 * Note to future maintainer:
 * While it might look at first glance like this test ensures backwards-
 * compatibility between JarSigner.findHeaderEnd and
 * ManifestDigester.findSection's first invocation that find the main
 * attributes section, at the time of that change, this test continues to
 * verify main attributes digestion now with ManifestDigester.findSection as
 * opposed to previous implementation in JarSigner.findHeaderEnd.
 * Before completely removing this test, make sure that main attributes
 * digestion is covered appropriately with tests. After JarSigner.findHeaderEnd
 * has been removed digests should still continue to match.
 *
 * See also
 * - jdk/test/jdk/sun/security/tools/jarsigner/PreserveRawManifestEntryAndDigest.java
 * for some end-to-end tests utilizing the jarsigner tool,
 * - jdk/test/jdk/sun/security/util/ManifestDigester/FindSection.java and
 * - jdk/test/jdk/sun/security/util/ManifestDigester/DigestInput.java
 * for much more detailed tests at api level
 *
 * Both test mentioned above, however, originally were created when removing
 * confusion of "Manifest-Main-Attributes" individual section with actual main
 * attributes whereas the test here is about changes related to raw manifest
 * reproduction and in the end test pretty much the same behavior.
 */
public class FindHeaderEndVsManifestDigesterFindFirstSection {

    static final boolean FIXED_8217375 = true; // FIXME

    /**
     * from former {@link JarSigner#findHeaderEnd}, subject to verification if
     * it can be replaced with {@link ManifestDigester#findSection}
     */
    @SuppressWarnings("fallthrough")
    private int findHeaderEnd(byte[] bs) {
        // Initial state true to deal with empty header
        boolean newline = true;     // just met a newline
        int len = bs.length;
        for (int i = 0; i < len; i++) {
            switch (bs[i]) {
                case '\r':
                    if (i < len - 1 && bs[i + 1] == '\n') i++;
                    // fallthrough
                case '\n':
                    if (newline) return i + 1;    //+1 to get length
                    newline = true;
                    break;
                default:
                    newline = false;
            }
        }
        // If header end is not found, it means the MANIFEST.MF has only
        // the main attributes section and it does not end with 2 newlines.
        // Returns the whole length so that it can be completely replaced.
        return len;
    }

    @DataProvider(name = "parameters")
    public static Object[][] parameters() {
        List<Object[]> tests = new ArrayList<>();
        for (String lineBreak : new String[] { "\n", "\r", "\r\n" }) {
            if ("\r".equals(lineBreak) && !FIXED_8217375) continue;
            for (int numLBs = 0; numLBs <= 3; numLBs++) {
                for (String addSection : new String[] { null, "Ignore" }) {
                    tests.add(new Object[] { lineBreak, numLBs, addSection });
                }
            }
        }
        return tests.toArray(new Object[tests.size()][]);
    }

    @Factory(dataProvider = "parameters")
    public static Object[] createTests(String lineBreak, int numLineBreaks,
            String individualSectionName) {
        return new Object[]{new FindHeaderEndVsManifestDigesterFindFirstSection(
                lineBreak, numLineBreaks, individualSectionName
        )};
    }

    final String lineBreak;
    final int numLineBreaks; // number of line breaks after main attributes
    final String individualSectionName; // null means only main attributes
    final byte[] rawBytes;

    FindHeaderEndVsManifestDigesterFindFirstSection(String lineBreak,
            int numLineBreaks, String individualSectionName) {
        this.lineBreak = lineBreak;
        this.numLineBreaks = numLineBreaks;
        this.individualSectionName = individualSectionName;

        rawBytes = (
            "oldStyle: trailing space " + lineBreak +
            "newStyle: no trailing space" + lineBreak.repeat(numLineBreaks) +
            // numLineBreaks < 2 will not properly delimit individual section
            // but it does not hurt to test that anyway
            (individualSectionName != null ?
                    "Name: " + individualSectionName + lineBreak +
                    "Ignore: nothing here" + lineBreak +
                    lineBreak
                : "")
        ).getBytes(UTF_8);
    }

    @BeforeMethod
    public void verbose() {
        System.out.println("lineBreak = " + stringToIntList(lineBreak));
        System.out.println("numLineBreaks = " + numLineBreaks);
        System.out.println("individualSectionName = " + individualSectionName);
    }

    @FunctionalInterface
    interface Callable {
        void call() throws Exception;
    }

    void catchNoLineBreakAfterMainAttributes(Callable test) throws Exception {
        // manifests cannot be parsed and digested if the main attributes do
        // not end in a blank line (double line break) or one line break
        // immediately before eof.
        boolean failureExpected = numLineBreaks == 0
                && individualSectionName == null;
        try {
            test.call();
            if (failureExpected) fail("expected an exception");
        } catch (NullPointerException | IllegalStateException e) {
            if (!failureExpected) fail("unexpected " + e.getMessage(), e);
        }
    }

    /**
     * Checks that the beginning of the manifest until position<ol>
     * <li>{@code Jarsigner.findHeaderEnd} in the previous version
     * and</li>
     * <li>{@code ManifestDigester.getMainAttsEntry().sections[0].
     * lengthWithBlankLine} in the new version</li>
     * </ol>produce the same offset (TODO: or the same error).
     * The beginning of the manifest until that offset (range
     * <pre>0 .. (offset - 1)</pre>) will be reproduced if the manifest has
     * not changed.
     * <p>
     * Getting {@code startOfNext} of {@link ManifestDigester#findSection}'s
     * first invokation returned {@link ManifestDigester.Position} which
     * identifies the end offset of the main attributes is difficulted by
     * {@link ManifestDigester#findSection} being private and therefore not
     * directly accessible.
     */
    @Test
    public void startOfNextLengthWithBlankLine() throws Exception {
        catchNoLineBreakAfterMainAttributes(() ->
            assertEquals(lengthWithBlankLine(), findHeaderEnd(rawBytes))
        );
    }

    /**
     * Due to its private visibility,
     * {@link ManifestDigester.Section#lengthWithBlankLine} is not directly
     * accessible. However, calling {@link ManifestDigester.Entry#digest}
     * reveals {@code lengthWithBlankLine} as third parameter in
     * <pre>md.update(sec.rawBytes, sec.offset, sec.lengthWithBlankLine);</pre>
     * on line ManifestDigester.java:212.
     * <p>
     * This value is the same as {@code startOfNext} of
     * {@link ManifestDigester#findSection}'s first invocation returned
     * {@link ManifestDigester.Position} identifying the end offset of the
     * main attributes because<ol>
     * <li>the end offset of the main attributes is assigned to
     * {@code startOfNext} in
     * <pre>pos.startOfNext = i+1;</pre> in ManifestDigester.java:98</li>
     * <li>which is then passed on as the third parameter to the constructor
     * of a new {@link ManifestDigester.Section#Section} by
     * <pre>new Section(0, pos.endOfSection + 1, pos.startOfNext, rawBytes)));</pre>
     * in in ManifestDigester.java:128</li>
     * <li>where it is assigned to
     * {@link ManifestDigester.Section#lengthWithBlankLine} by
     * <pre>this.lengthWithBlankLine = lengthWithBlankLine;</pre>
     * in ManifestDigester.java:241</li>
     * <li>from where it is picked up by {@link ManifestDigester.Entry#digest}
     * in
     * <pre>md.update(sec.rawBytes, sec.offset, sec.lengthWithBlankLine);</pre>
     * in ManifestDigester.java:212</li>
     * </ol>
     * all of which without any modification.
     */
    int lengthWithBlankLine() {
        int[] lengthWithBlankLine = new int[] { 0 };
        new ManifestDigester(rawBytes).get(ManifestDigester.MF_MAIN_ATTRS,
                false).digest(new MessageDigest("lengthWithBlankLine") {
            @Override protected void engineReset() {
                lengthWithBlankLine[0] = 0;
            }
            @Override protected void engineUpdate(byte b) {
                lengthWithBlankLine[0]++;
            }
            @Override protected void engineUpdate(byte[] b, int o, int l) {
                lengthWithBlankLine[0] += l;
            }
            @Override protected byte[] engineDigest() {
                return null;
            }
        });
        return lengthWithBlankLine[0];
    }

    /**
     * Checks that the replacement of {@link JarSigner#findHeaderEnd} is
     * actually used to reproduce manifest main attributes.
     * <p>
     * {@link #startOfNextLengthWithBlankLine} demonstrates that
     * {@link JarSigner#findHeaderEnd} has been replaced successfully with
     * {@link ManifestDigester#findSection} but does not also show that the
     * main attributes are reproduced with the same offset as before.
     * {@link #startOfNextLengthWithBlankLine} uses
     * {@link ManifestDigester.Entry#digest} to demonstrate an equal offset
     * calculated but {@link ManifestDigester.Entry#digest} is not necessarily
     * the same as reproducing, especially when considering
     * {@link ManifestDigester.Entry#oldStyle}.
     */
    @Test(enabled = FIXED_8217375)
    public void reproduceMainAttributes() throws Exception {
        catchNoLineBreakAfterMainAttributes(() -> {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            ManifestDigester md = new ManifestDigester(rawBytes);
            // without 8217375 fixed the following line will not even compile
            // so just remove it and skip the test for regression
            md.getMainAttsEntry().reproduceRaw(buf); // FIXME

            assertEquals(buf.size(), findHeaderEnd(rawBytes));
        });
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
