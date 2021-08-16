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
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;
import java.util.List;
import java.util.ArrayList;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * @test
 * @bug 8066619
 * @run testng ValueUtf8Coding
 * @summary Tests encoding and decoding manifest header values to and from
 * UTF-8 with the complete Unicode character set.
 */ /*
 * see also "../tools/launcher/UnicodeTest.java" for manifest attributes
 * parsed during launch
 */
public class ValueUtf8Coding {

    /**
     * Maximum number of bytes of UTF-8 encoded characters in one header value.
     * <p>
     * There are too many different Unicode code points (more than one million)
     * to fit all into one manifest value. The specifications state:
     * <q>Implementations should support 65535-byte (not character) header
     * values, and 65535 headers per file. They might run out of memory,
     * but there should not be hard-coded limits below these values.</q>
     *
     * @see <a
     * href="{@docRoot}/../specs/jar/jar.html#Notes_on_Manifest_and_Signature_Files">
     * Notes on Manifest and Signature Files</a>
     */
    static final int SUPPORTED_VALUE_LENGTH = 65535;

    /**
     * Returns {@code true} if {@code codePoint} is known not to be a supported
     * character in manifest header values. Explicitly forbidden in manifest
     * header values are according to a statement from the specifications:
     * <q>otherchar: any UTF-8 character except NUL, CR and LF</q>.
     * {@code NUL} ({@code 0x0}), however, works just fine and might have been
     * used and might still be.
     *
     * @see <a href="{@docRoot}/../specs/jar/jar.html#Section-Specification">
     * Jar File Specification</a>
     */
    static boolean isUnsupportedManifestValueCharacter(int codePoint) {
        return codePoint == '\r' /* CR */ || codePoint == '\n' /* LF */;
    };

    /**
     * Produces a list of strings with all Unicode characters except those
     * explicitly invalid in manifest header values.
     * Each string is filled with as many characters as fit into
     * {@link #SUPPORTED_VALUE_LENGTH} bytes with UTF-8 encoding except the
     * last string which contains the remaining characters. Each of those
     * strings becomes a header value the number of which 65535 should be
     * supported per file.
     *
     * @see <a
     * href="{@docRoot}/../specs/jar/jar.html#Notes_on_Manifest_and_Signature_Files">
     * Notes on Manifest and Signature Files</a>
     */
    static List<String> produceValuesWithAllUnicodeCharacters() {
        ArrayList<String> values = new ArrayList<>();
        byte[] valueBuf = new byte[SUPPORTED_VALUE_LENGTH];
        int pos = 0;
        for (int codePoint = Character.MIN_CODE_POINT;
                codePoint <= Character.MAX_CODE_POINT; codePoint++) {
            if (isUnsupportedManifestValueCharacter(codePoint)) {
                continue;
            }

            byte[] charBuf = Character.toString(codePoint).getBytes(UTF_8);
            if (pos + charBuf.length > valueBuf.length) {
                values.add(new String(valueBuf, 0, pos, UTF_8));
                pos = 0;
            }
            System.arraycopy(charBuf, 0, valueBuf, pos, charBuf.length);
            pos += charBuf.length;
        }
        if (pos > 0) {
            values.add(new String(valueBuf, 0, pos, UTF_8));
        }
        // minimum number of headers supported is the same as the minimum size
        // of each header value in bytes
        assertTrue(values.size() <= SUPPORTED_VALUE_LENGTH);
        return values;
    }

    /**
     * Returns simple, valid, short, and distinct manifest header names.
     * The returned name cannot collide with "{@code Manifest-Version}" because
     * the returned string does not contain "{@code -}".
     */
    static Name azName(int seed) {
        StringBuffer name = new StringBuffer();
        do {
            name.insert(0, (char) (seed % 26 + (seed < 26 ? 'A' : 'a')));
            seed = seed / 26 - 1;
        } while (seed >= 0);
        return new Name(name.toString());
    }

    /**
     * Writes and reads a manifest with the complete Unicode character set.
     * The characters are grouped into manifest header values with about as
     * many bytes as allowed each, utilizing a single big manifest.
     * <p>
     * This test assumes that a manifest is encoded and decoded correctly if
     * writing and then reading it again results in a manifest with identical
     * values as the original. The test is not about other aspects of writing
     * and reading manifests than only that, given the fact and the way it
     * works for some characters such as the most widely and often used ones,
     * it also works for the complete Unicode character set just the same.
     * <p>
     * Only header values are tested. The set of allowed characters for header
     * names are much more limited and are a different topic entirely and most
     * simple ones are used here as necessary just to get valid and different
     * ones (see {@link #azName}).
     * <p>
     * Because the current implementation under test uses different portions
     * of code depending on where the value occurs to read or write, each
     * character is tested in each of the three positions:<ul>
     * <li>main attribute header,</li>
     * <li>named section name, and</li>
     * <li>named sections header values</li>
     * </ul>
     * Implementation of writing the main section headers in
     * {@link Attributes#writeMain(java.io.DataOutputStream)} differs from the
     * one writing named section headers in
     * {@link Attributes#write(java.io.DataOutputStream)} regarding the special
     * order of {@link Name#MANIFEST_VERSION} and
     * {@link Name#SIGNATURE_VERSION} and also
     * {@link Manifest#read(java.io.InputStream)} at least potentially reads
     * main sections differently than reading named sections names headers in
     * {@link Attributes#read(Manifest.FastInputStream, byte[])}.
     */
    @Test
    public void testCompleteUnicodeCharacterSet() throws IOException {
        Manifest mf = new Manifest();
        mf.getMainAttributes().put(Name.MANIFEST_VERSION, "1.0");

        List<String> values = produceValuesWithAllUnicodeCharacters();
        for (int i = 0; i < values.size(); i++) {
            Name name = azName(i);
            String value = values.get(i);

            mf.getMainAttributes().put(name, value);
            Attributes attributes = new Attributes();
            mf.getEntries().put(value, attributes);
            attributes.put(name, value);
        }

        mf = writeAndRead(mf);

        for (int i = 0; i < values.size(); i++) {
            String value = values.get(i);
            Name name = azName(i);

            assertEquals(mf.getMainAttributes().getValue(name), value,
                    "main attributes header value");
            Attributes attributes = mf.getAttributes(value);
            assertNotNull(attributes, "named section");
            assertEquals(attributes.getValue(name), value,
                    "named section attributes value");
        }
    }

    static Manifest writeAndRead(Manifest mf) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mf.write(out);
        byte[] mfBytes = out.toByteArray();

        System.out.println("-".repeat(72));
        System.out.print(new String(mfBytes, UTF_8));
        System.out.println("-".repeat(72));

        ByteArrayInputStream in = new ByteArrayInputStream(mfBytes);
        return new Manifest(in);
    }

}
