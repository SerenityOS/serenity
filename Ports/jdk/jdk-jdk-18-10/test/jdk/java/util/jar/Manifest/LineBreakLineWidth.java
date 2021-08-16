/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.jar.Manifest;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * @test
 * @bug 6372077
 * @run testng LineBreakLineWidth
 * @summary write valid manifests with respect to line breaks
 *          and read any line width
 */
public class LineBreakLineWidth {

    /**
     * maximum header name length from {@link Name#isValid(String)}
     * not including the name-value delimiter <code>": "</code>
     */
    final static int MAX_HEADER_NAME_LENGTH = 70;

    /**
     * range of one..{@link #TEST_WIDTH_RANGE} covered in this test that
     * exceeds the range of allowed header name lengths or line widths
     * in order to also cover invalid cases beyond the valid boundaries
     * and to keep it somewhat independent from the actual manifest width.
     * <p>
     * bigger than 72 (maximum manifest header line with in bytes (not utf-8
     * encoded characters) but otherwise arbitrarily chosen
     */
    final static int TEST_WIDTH_RANGE = 123;

    /**
     * tests if only valid manifest files can written depending on the header
     * name length or that an exception occurs already on the attempt to write
     * an invalid one otherwise and that no invalid manifest can be written.
     * <p>
     * due to bug JDK-6372077 it was possible to write a manifest that could
     * not be read again. independent of the actual manifest line width, such
     * a situation should never happen, which is the subject of this test.
     */
    @Test
    public void testWriteValidManifestOrException() throws IOException {
        /*
         * multi-byte utf-8 characters cannot occur in header names,
         * only in values which are not subject of this test here.
         * hence, each character in a header name uses exactly one byte and
         * variable length utf-8 character encoding doesn't affect this test.
         */

        String name = "";
        for (int l = 1; l <= TEST_WIDTH_RANGE; l++) {
            name += "x";
            System.out.println("name = " + name + ", "
                    + "name.length = " + name.length());

            if (l <= MAX_HEADER_NAME_LENGTH) {
                writeValidManifest(name, "somevalue");
            } else {
                writeInvalidManifestThrowsException(name, "somevalue");
            }
        }
    }

    static void writeValidManifest(String name, String value)
            throws IOException {
        byte[] mfBytes = writeManifest(name, value);
        Manifest mf = new Manifest(new ByteArrayInputStream(mfBytes));
        assertMainAndSectionValues(mf, name, value);
    }

    static void writeInvalidManifestThrowsException(String name, String value)
            throws IOException {
        try {
            writeManifest(name, value);
        } catch (IllegalArgumentException e) {
            // no invalid manifest was produced which is considered acceptable
            return;
        }

        fail("no error writing manifest considered invalid");
    }

    /**
     * tests that manifest files can be read even if the line breaks are
     * placed in different positions than where the current JDK's
     * {@link Manifest#write(java.io.OutputStream)} would have put it provided
     * the manifest is valid otherwise.
     * <p>
     * the <a href="{@docRoot}/../specs/jar/jar.html#Notes_on_Manifest_and_Signature_Files">
     * "Notes on Manifest and Signature Files" in the "JAR File
     * Specification"</a> state that "no line may be longer than 72 bytes
     * (not characters), in its utf8-encoded form." but allows for earlier or
     * additional line breaks.
     * <p>
     * the most important purpose of this test case is probably to make sure
     * that manifest files broken at 70 bytes line width written with the
     * previous version of {@link Manifest} before this fix still work well.
     */
    @Test
    public void testReadDifferentLineWidths() throws IOException {
        /*
         * uses only one-byte utf-8 encoded characters as values.
         * correctly breaking multi-byte utf-8 encoded characters
         * would be subject of another test if there was one such.
         */

        // w: line width
        // 6 minimum required for section names starting with "Name: "
        for (int w = 6; w <= TEST_WIDTH_RANGE; w++) {

            // ln: header name length
            String name = "";
            // - 2 due to the delimiter ": " that has to fit on the same
            // line as the name
            for (int ln = 1; ln <= w - 2; ln++) {
                name += "x";

                // lv: value length
                String value = "";
                for (int lv = 1; lv <= TEST_WIDTH_RANGE; lv++) {
                    value += "y";
                }

                System.out.println("lineWidth = " + w);
                System.out.println("name = " + name + ""
                        + ", name.length = " + name.length());
                System.out.println("value = " + value + ""
                        + ", value.length = " + value.length());

                readSpecificLineWidthManifest(name, value, w);
            }
        }
    }

    static void readSpecificLineWidthManifest(String name, String value,
            int lineWidth) throws IOException {
        /*
         * breaking header names is not allowed and hence cannot be reasonably
         * tested. it cannot easily be helped, that invalid manifest files
         * written by the previous Manifest version implementation are illegal
         * if the header name is 69 or 70 bytes and in that case the name/value
         * delimiter ": " was broken on a new line.
         *
         * changing the line width in Manifest#make72Safe(StringBuffer),
         * however, also affects at which positions values are broken across
         * lines (should always have affected values only and never header
         * names or the delimiter) which is tested here.
         *
         * ideally, any previous Manifest implementation would have been used
         * here to provide manifest files to test reading but these are no
         * longer available in this version's sources and there might as well
         * be other libraries writing manifests. Therefore, in order to be able
         * to test any manifest file considered valid with respect to line
         * breaks that could not possibly be produced with the current Manifest
         * implementation, this test provides its own manifests in serialized
         * form.
         */
        String lineBrokenSectionName = breakLines(lineWidth, "Name: " + name);
        String lineBrokenNameAndValue = breakLines(lineWidth, name + ": " + value);

        ByteArrayOutputStream mfBuf = new ByteArrayOutputStream();
        mfBuf.write("Manifest-Version: 1.0".getBytes(UTF_8));
        mfBuf.write("\r\n".getBytes(UTF_8));
        mfBuf.write(lineBrokenNameAndValue.getBytes(UTF_8));
        mfBuf.write("\r\n".getBytes(UTF_8));
        mfBuf.write("\r\n".getBytes(UTF_8));
        mfBuf.write(lineBrokenSectionName.getBytes(UTF_8));
        mfBuf.write("\r\n".getBytes(UTF_8));
        mfBuf.write(lineBrokenNameAndValue.getBytes(UTF_8));
        mfBuf.write("\r\n".getBytes(UTF_8));
        mfBuf.write("\r\n".getBytes(UTF_8));
        byte[] mfBytes = mfBuf.toByteArray();
        printManifest(mfBytes);

        boolean nameValid = name.length() <= MAX_HEADER_NAME_LENGTH;

        Manifest mf;
        try {
            mf = new Manifest(new ByteArrayInputStream(mfBytes));
        } catch (IOException e) {
            if (!nameValid &&
                    e.getMessage().startsWith("invalid header field")) {
                // expected because the name is not valid
                return;
            }

            throw new AssertionError(e.getMessage(), e);
        }

        assertTrue(nameValid, "failed to detect invalid manifest");

        assertMainAndSectionValues(mf, name, value);
    }

    static String breakLines(int lineWidth, String nameAndValue) {
        String lineBrokenNameAndValue = "";
        int charsOnLastLine = 0;
        for (int i = 0; i < nameAndValue.length(); i++) {
            lineBrokenNameAndValue += nameAndValue.substring(i, i + 1);
            charsOnLastLine++;
            if (0 < i && i < nameAndValue.length() - 1
                    && charsOnLastLine == lineWidth) {
                lineBrokenNameAndValue += "\r\n ";
                charsOnLastLine = 1;
            }
        }
        return lineBrokenNameAndValue;
    }

    static byte[] writeManifest(String name, String value) throws IOException {
        /*
         * writing manifest main headers is implemented separately from
         * writing named sections manifest headers:
         * - java.util.jar.Attributes.writeMain(DataOutputStream)
         * - java.util.jar.Attributes.write(DataOutputStream)
         * which is why this is also covered separately in this test by
         * always adding the same value twice, in the main attributes as
         * well as in a named section (using the header name also as the
         * section name).
         */

        Manifest mf = new Manifest();
        mf.getMainAttributes().put(Name.MANIFEST_VERSION, "1.0");
        mf.getMainAttributes().putValue(name, value);

        Attributes section = new Attributes();
        section.putValue(name, value);
        mf.getEntries().put(name, section);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mf.write(out);
        byte[] mfBytes = out.toByteArray();
        printManifest(mfBytes);
        return mfBytes;
    }

    private static void printManifest(byte[] mfBytes) {
        final String sepLine = "----------------------------------------------"
                + "---------------------|-|-|"; // |-positions: ---68-70-72
        System.out.println(sepLine);
        System.out.print(new String(mfBytes, UTF_8));
        System.out.println(sepLine);
    }

    private static void assertMainAndSectionValues(Manifest mf, String name,
            String value) {
        String mainValue = mf.getMainAttributes().getValue(name);
        String sectionValue = mf.getAttributes(name).getValue(name);

        assertEquals(value, mainValue, "value different in main section");
        assertEquals(value, sectionValue, "value different in named section");
    }

}
