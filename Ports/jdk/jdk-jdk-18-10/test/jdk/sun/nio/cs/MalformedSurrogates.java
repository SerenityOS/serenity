/*
 * Copyright (c) 2002, 2013, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4153987
 * @summary Malformed surrogates should be handled by the converter in
 * substitution mode.
 */
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.CharBuffer;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;
import java.util.SortedMap;

public class MalformedSurrogates {

    private static final String PREFIX = "abc";
    private static final String SUFFIX = "efgh";
    private static final String MALFORMED_SURROGATE = PREFIX + "\uD800\uDB00" + SUFFIX;
    private static final String NORMAL_SURROGATE = PREFIX + "\uD800\uDC00" + SUFFIX;
    private static final String REVERSED_SURROGATE = PREFIX + "\uDC00\uD800" + SUFFIX;
    private static final String SOLITARY_HIGH_SURROGATE = PREFIX + "\uD800" + SUFFIX;
    private static final String SOLITARY_LOW_SURROGATE = PREFIX + "\uDC00" + SUFFIX;

    public static void main(String[] args) throws IOException {
        SortedMap<String, Charset> map = Charset.availableCharsets();
        for (String name : map.keySet()) {
            Charset charset = map.get(name);
            if (charset.canEncode() && !charset.name().equals("x-COMPOUND_TEXT")) {
                testNormalSurrogate(charset, NORMAL_SURROGATE);
                testMalformedSurrogate(charset, MALFORMED_SURROGATE);
                testMalformedSurrogate(charset, REVERSED_SURROGATE);
                testMalformedSurrogate(charset, SOLITARY_HIGH_SURROGATE);
                testMalformedSurrogate(charset, SOLITARY_LOW_SURROGATE);
                testSurrogateWithReplacement(charset, NORMAL_SURROGATE);
                testSurrogateWithReplacement(charset, MALFORMED_SURROGATE);
                testSurrogateWithReplacement(charset, REVERSED_SURROGATE);
                testSurrogateWithReplacement(charset, SOLITARY_HIGH_SURROGATE);
                testSurrogateWithReplacement(charset, SOLITARY_LOW_SURROGATE);
            }
        }
    }

    public static void testMalformedSurrogate(Charset cs, String surrogate) throws IOException {
        CharsetEncoder en = cs.newEncoder();
        if (en.canEncode(surrogate)) {
            throw new RuntimeException("testMalformedSurrogate failed with charset " + cs.name());
        }

        try {
            en.encode(CharBuffer.wrap(surrogate));
            throw new RuntimeException("Should throw MalformedInputException or UnmappableCharacterException");
        } catch (MalformedInputException | UnmappableCharacterException ex) {
        } finally {
            en.reset();
        }

        try (OutputStreamWriter osw = new OutputStreamWriter(new ByteArrayOutputStream(), en)) {
            osw.write(surrogate);
            throw new RuntimeException("Should throw MalformedInputException or UnmappableCharacterException");
        } catch (MalformedInputException | UnmappableCharacterException ex) {
        }
    }

    public static void testNormalSurrogate(Charset cs, String surrogate) throws IOException {
        CharsetEncoder en = cs.newEncoder();
        try {
            en.encode(CharBuffer.wrap(surrogate));
        } catch (UnmappableCharacterException ex) {
        } finally {
            en.reset();
        }

        try (OutputStreamWriter osw = new OutputStreamWriter(new ByteArrayOutputStream(), en)) {
            osw.write(surrogate);
        } catch (UnmappableCharacterException ex) {
        }
    }

    public static void testSurrogateWithReplacement(Charset cs, String surrogate) throws IOException {
        CharsetEncoder en = cs.newEncoder();
        CharsetDecoder de = cs.newDecoder();
        if (!en.canEncode(NORMAL_SURROGATE)) {
            return;
        }
        String expected = null;
        String replace = new String(en.replacement(), cs);
        switch (surrogate) {
            case MALFORMED_SURROGATE:
            case REVERSED_SURROGATE:
                expected = PREFIX + replace + replace + SUFFIX;
                break;
            case SOLITARY_HIGH_SURROGATE:
            case SOLITARY_LOW_SURROGATE:
                expected = PREFIX + replace + SUFFIX;
                break;
            default:
                expected = NORMAL_SURROGATE;
        }

        try {
            en.onMalformedInput(CodingErrorAction.REPLACE);
            en.onUnmappableCharacter(CodingErrorAction.REPLACE);
            ByteBuffer bbuf = en.encode(CharBuffer.wrap(surrogate));
            CharBuffer cbuf = de.decode(bbuf);
            if (!cbuf.toString().equals(expected)) {
                throw new RuntimeException("charset " + cs.name() + " (en)decoded the surrogate " + surrogate + " to " + cbuf.toString() + " which is not same as the expected " + expected);
            }
        } finally {
            en.reset();
            de.reset();
        }

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                OutputStreamWriter osw = new OutputStreamWriter(bos, en);) {
            osw.write(surrogate);
            osw.flush();
            try (InputStreamReader isr = new InputStreamReader(new ByteArrayInputStream(bos.toByteArray()), de)) {
                CharBuffer cbuf = CharBuffer.allocate(expected.length());
                isr.read(cbuf);
                cbuf.rewind();
                if (!cbuf.toString().equals(expected)) {
                    throw new RuntimeException("charset " + cs.name() + " (en)decoded the surrogate " + surrogate + " to " + cbuf.toString() + " which is not same as the expected " + expected);
                }
            }
        }
    }
}
