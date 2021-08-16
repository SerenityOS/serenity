/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package jdk.jfr.internal.consumer;

import java.io.IOException;
import java.nio.charset.Charset;

public final class StringParser extends Parser {

    public enum Encoding {
        NULL(0),
        EMPTY_STRING(1),
        CONSTANT_POOL(2),
        UT8_BYTE_ARRAY(3),
        CHAR_ARRAY(4),
        LATIN1_BYTE_ARRAY(5);

        private byte byteValue;

        private Encoding(int byteValue) {
            this.byteValue = (byte) byteValue;
        }

        public byte byteValue() {
            return byteValue;
        }

        public boolean is(byte value) {
            return value == byteValue;
        }

    }
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final Charset LATIN1 = Charset.forName("ISO-8859-1");

    private static final class CharsetParser extends Parser {
        private final Charset charset;
        private int lastSize;
        private byte[] buffer = new byte[16];
        private String lastString;

        CharsetParser(Charset charset) {
            this.charset = charset;
        }

        @Override
        public Object parse(RecordingInput input) throws IOException {
            int size = input.readInt();
            ensureSize(size);
            if (lastSize == size) {
                boolean equalsLastString = true;
                for (int i = 0; i < size; i++) {
                    // TODO: No need to read byte per byte
                    byte b = input.readByte();
                    if (buffer[i] != b) {
                        equalsLastString = false;
                        buffer[i] = b;
                    }
                }
                if (equalsLastString) {
                    return lastString;
                }
            } else {
                for (int i = 0; i < size; i++) {
                    buffer[i] = input.readByte();
                }
            }
            lastString = new String(buffer, 0, size, charset);
            lastSize = size;
            return lastString;
        }

        @Override
        public void skip(RecordingInput input) throws IOException {
            int size = input.readInt();
            input.skipBytes(size);
        }

        private void ensureSize(int size) {
            if (buffer.length < size) {
                buffer = new byte[size];
            }
        }
    }

    private static final class CharArrayParser extends Parser {
        private char[] buffer = new char[16];
        private int lastSize = -1;
        private String lastString = null;

        @Override
        public Object parse(RecordingInput input) throws IOException {
            int size = input.readInt();
            ensureSize(size);
            if (lastSize == size) {
                boolean equalsLastString = true;
                for (int i = 0; i < size; i++) {
                    char c = input.readChar();
                    if (buffer[i] != c) {
                        equalsLastString = false;
                        buffer[i] = c;
                    }
                }
                if (equalsLastString) {
                    return lastString;
                }
            } else {
                for (int i = 0; i < size; i++) {
                    buffer[i] = input.readChar();
                }
            }
            lastString = new String(buffer, 0, size);
            lastSize = size;
            return lastString;
        }

        @Override
        public void skip(RecordingInput input) throws IOException {
            int size = input.readInt();
            for (int i = 0; i < size; i++) {
                input.readChar();
            }
        }

        private void ensureSize(int size) {
            if (buffer.length < size) {
                buffer = new char[size];
            }
        }
    }

    private final ConstantLookup stringLookup;
    private final CharArrayParser charArrayParser = new CharArrayParser();
    private final CharsetParser utf8parser = new CharsetParser(UTF8);
    private final CharsetParser latin1parser = new CharsetParser(LATIN1);
    private final boolean event;

    public StringParser(ConstantLookup stringLookup, boolean event) {
        this.stringLookup = stringLookup;
        this.event = event;
    }

    @Override
    public Object parse(RecordingInput input) throws IOException {
        byte encoding = input.readByte();
        if (Encoding.CONSTANT_POOL.is(encoding)) {
            long key = input.readLong();
            if (event) {
                return stringLookup.getCurrentResolved(key);
            } else {
                return stringLookup.getCurrent(key);
            }
        }
        if (Encoding.NULL.is(encoding)) {
            return null;
        }
        if (Encoding.EMPTY_STRING.is(encoding)) {
            return "";
        }
        if (Encoding.CHAR_ARRAY.is(encoding)) {
            return charArrayParser.parse(input);
        }
        if (Encoding.UT8_BYTE_ARRAY.is(encoding)) {
            return utf8parser.parse(input);
        }
        if (Encoding.LATIN1_BYTE_ARRAY.is(encoding)) {
            return latin1parser.parse(input);
        }
        throw new IOException("Unknown string encoding " + encoding);
    }

    @Override
    public void skip(RecordingInput input) throws IOException {
        byte encoding = input.readByte();
        if (Encoding.CONSTANT_POOL.is(encoding)) {
            input.readLong();
            return;
        }
        if (Encoding.EMPTY_STRING.is(encoding)) {
            return;
        }
        if (Encoding.NULL.is(encoding)) {
            return;
        }
        if (Encoding.CHAR_ARRAY.is(encoding)) {
            charArrayParser.skip(input);
            return;
        }
        if (Encoding.UT8_BYTE_ARRAY.is(encoding)) {
            utf8parser.skip(input);
            return;
        }
        if (Encoding.LATIN1_BYTE_ARRAY.is(encoding)) {
            latin1parser.skip(input);
            return;
        }
        throw new IOException("Unknown string encoding " + encoding);
    }
}
