/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.parser;

import java.util.Arrays;

import com.sun.tools.javac.resources.CompilerProperties.Errors;
import com.sun.tools.javac.util.Log;

import static com.sun.tools.javac.util.LayoutCharacters.EOI;
import static com.sun.tools.javac.util.LayoutCharacters.tabulate;

/**
 * The unicode character reader used by the javac/javadoc lexer/tokenizer, returns characters
 * one by one as contained in the input stream, handling unicode escape sequences accordingly.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b></p>
 */
public class UnicodeReader {
    /**
     * Buffer containing characters from source file. May contain extraneous characters
     * beyond this.length.
     */
    private final char[] buffer;

    /**
     * Length of meaningful content in buffer.
     */
    private final int length;

    /**
     * Character buffer index of character currently being observed.
     */
    private int position;

    /**
     * Number of characters combined to provide character currently being observed. Typically
     * one, but may be more when combinations of surrogate pairs and unicode escape sequences
     * are read.
     */
    private int width;

    /**
     * Character currently being observed. If a surrogate pair is read then will be the high
     * member of the pair.
     */
    private char character;

    /**
     * Codepoint of character currently being observed. Typically equivalent to the character
     * but will have a value greater that 0xFFFF when a surrogate pair.
     */
    private int codepoint;

    /**
     * true if the last character was a backslash. This is used to handle the special case
     * when a backslash precedes an unicode escape. In that case, the second backslash
     * is treated as a backslash and not part of an unicode escape.
     */
    private boolean wasBackslash;

    /**
     * true if the last character was derived from an unicode escape sequence.
     */
    private boolean wasUnicodeEscape;

    /**
     * Log for error reporting.
     */
    private final Log log;

    /**
     * Constructor.
     *
     * @param sf      scan factory.
     * @param array   array containing contents of source.
     * @param length  length of meaningful content in buffer.
     */
    protected UnicodeReader(ScannerFactory sf, char[] array, int length) {
        this.buffer = array;
        this.length = length;
        this.position = 0;
        this.width = 0;
        this.character = '\0';
        this.codepoint = 0;
        this.wasBackslash = false;
        this.wasUnicodeEscape = false;
        this.log = sf.log;

        nextCodePoint();
    }

    /**
     * Returns the length of the buffer. This is length of meaningful content in buffer and
     * not the length of the buffer array.
     *
     * @return length of the buffer.
     */
    protected int length() {
        return length;
    }

    /**
     * Return true if current position is within the meaningful part of the buffer.
     *
     * @return true if current position is within the meaningful part of the buffer.
     */
    protected boolean isAvailable() {
        return position < length;
    }

    /**
     * Fetches the next 16-bit character from the buffer and places it in this.character.
     */
    private void nextCodeUnit() {
        // Index of next character in buffer.
        int index = position + width;

        // If past end of buffer.
        if (length <= index) {
            // End of file is marked with EOI.
            character = EOI;
        } else {
            // Next character in buffer.
            character = buffer[index];
            // Increment length of codepoint.
            width++;
        }
    }

    /**
     * Fetches the next 16-bit character from the buffer. If an unicode escape
     * is detected then converts the unicode escape to a character.
     */
    private void nextUnicodeInputCharacter() {
        // Position to next codepoint.
        position += width;
        // Codepoint has no characters yet.
        width = 0;

        // Fetch next character.
        nextCodeUnit();

        if (character == '\\' && (!wasBackslash || wasUnicodeEscape)) {
            // Is a backslash and may be an unicode escape.
            switch (unicodeEscape()) {
                case BACKSLASH -> {
                    wasUnicodeEscape = false;
                    wasBackslash = !wasBackslash;
                }
                case VALID_ESCAPE -> {
                    wasUnicodeEscape = true;
                    wasBackslash = character == '\\' && !wasBackslash;
                }
                case BROKEN_ESCAPE -> nextUnicodeInputCharacter(); //skip broken unicode escapes
            }
        } else {
            wasBackslash = false;
            wasUnicodeEscape = false;
        }

        // Codepoint and character match if not surrogate.
        codepoint = (int)character;
    }

    /**
     * Fetches the nextcode point from the buffer. If an unicode escape is recognized
     * then converts unicode escape to a character. If two characters are a surrogate pair
     * then converts to a codepoint.
     */
    private void nextCodePoint() {
        // Next unicode character.
        nextUnicodeInputCharacter();

        // Return early if ASCII or not a surrogate pair.
        if (isASCII() || !Character.isHighSurrogate(character)) {
            return;
        }

        // Capture high surrogate and position.
        char hi = character;
        int savePosition = position;
        int saveWidth = width;

        // Get potential low surrogate.
        nextUnicodeInputCharacter();
        char lo = character;

        if (Character.isLowSurrogate(lo)) {
            // Start codepoint at start of high surrogate.
            position = savePosition;
            width += saveWidth;
            // Compute codepoint.
            codepoint = Character.toCodePoint(hi, lo);
        } else {
            // Restore to treat high surrogate as just a character.
            position = savePosition;
            width = saveWidth;
            character = hi;
            codepoint = (int)hi;
            // Could potential report an error here (old code did not.)
        }
    }

    /**
     * Converts an unicode escape into a character.
     *
     * @return true if was an unicode escape.
     */
    private UnicodeEscapeResult unicodeEscape() {
        // Start of unicode escape (past backslash.)
        int start = position + width;

        // Default to backslash result, unless proven otherwise.
        character = '\\';
        width = 1;

        // Skip multiple 'u'.
        int index;
        for (index = start; index < length; index++) {
            if (buffer[index] != 'u') {
                break;
            }
        }

        // Needs to have been at least one u.
        if (index == start) {
            return UnicodeEscapeResult.BACKSLASH;
        }

        int code = 0;

        for (int i = 0; i < 4; i++) {
            // Translate and merge digit.
            int digit = index < length ? Character.digit(buffer[index], 16) : -1;
            code = code << 4 | digit;

            // If invalid digit.
            if (code < 0) {
                break;
            }

            // On to next character.
            index++;
        }

        // Skip digits even if error.
        width = index - position;

        // If all digits are good.
        if (code >= 0) {
            character = (char)code;
            return UnicodeEscapeResult.VALID_ESCAPE;
        } else {
            log.error(index, Errors.IllegalUnicodeEsc);
            return UnicodeEscapeResult.BROKEN_ESCAPE;
        }
    }

    private enum UnicodeEscapeResult {
        BACKSLASH,
        VALID_ESCAPE,
        BROKEN_ESCAPE;
    }

    /**
     * Return the current position in the character buffer.
     *
     * @return  current position in the character buffer.
     */
    protected int position() {
        return position;
    }


    /**
     * Reset the reader to the specified position.
     * Warning: Do not use when previous character was an ASCII or unicode backslash.
     * @param pos
     */
    protected void reset(int pos) {
        position = pos;
        width = 0;
        wasBackslash = false;
        wasUnicodeEscape = false;
        nextCodePoint();
    }

    /**
     * Return the current character in at the current position.
     *
     * @return current character in at the current position.
     */
    protected char get() {
        return character;
    }

    /**
     * Return the current codepoint in at the current position.
     *
     * @return current codepoint in at the current position.
     */
    protected int getCodepoint() {
        return codepoint;
    }

    /**
     * Returns true if the current codepoint is a surrogate.
     *
     * @return true if the current codepoint is a surrogate.
     */
    protected boolean isSurrogate() {
        return 0xFFFF < codepoint;
    }

    /**
     * Returns true if the current character is ASCII.
     *
     * @return true if the current character is ASCII.
     */
    protected boolean isASCII() {
        return character <= 0x7F;
    }

    /**
     * Advances the current character to the next character.
     *
     * @return next character.
     */
    protected char next() {
        nextCodePoint();

        return character;
    }

    /**
     * Compare character. Returns true if a match.
     *
     * @param ch  character to match.
     *
     * @return true if a match.
     */
    protected boolean is(char ch) {
        return character == ch;
    }

    /**
     * Match one of the arguments. Returns true if a match.
     */
    protected boolean isOneOf(char ch1, char ch2) {
        return is(ch1) || is(ch2);
    }
    protected boolean isOneOf(char ch1, char ch2, char ch3) {
        return is(ch1) || is(ch2) || is(ch3);
    }
    protected boolean isOneOf(char ch1, char ch2, char ch3, char ch4, char ch5, char ch6) {
        return is(ch1) || is(ch2) || is(ch3) || is(ch4) || is(ch5) || is(ch6);
    }

    /**
     * Tests to see if current character is in the range of lo to hi characters (inclusive).
     *
     * @param lo  lowest character in range.
     * @param hi  highest character in range.
     *
     * @return true if the current character is in range.
     */
    protected boolean inRange(char lo, char hi) {
        return lo <= character && character <= hi;
    }

    /**
     * Compare character and advance if a match. Returns true if a match.
     *
     * @param ch  character to match.
     *
     * @return true if a match.
     */
    protected boolean accept(char ch) {
        if (is(ch)) {
            next();

            return true;
        }

        return false;
    }

    /**
     * Match one of the arguments and advance if a match. Returns true if a match.
     */
    protected boolean acceptOneOf(char ch1, char ch2) {
        if (isOneOf(ch1, ch2)) {
            next();

            return true;
        }

        return false;
    }

    protected boolean acceptOneOf(char ch1, char ch2, char ch3) {
        if (isOneOf(ch1, ch2, ch3)) {
            next();

            return true;
        }

        return false;
    }

    /**
     * Skip over all occurances of character.
     *
     * @param ch character to accept.
     */
    protected void skip(char ch) {
        while (accept(ch)) {
            // next
        }
    }

    /**
     * Skip over ASCII white space characters.
     */
    protected void skipWhitespace() {
        while (acceptOneOf(' ', '\t', '\f')) {
            // next
        }
    }

    /**
     * Skip to end of line.
     */
    protected void skipToEOLN() {
        while (isAvailable()) {
            if (isOneOf('\r', '\n')) {
                break;
            }

            next();
        }

    }

    /**
     * Compare string and advance if a match. Returns true if a match.
     * Warning: Do not use when previous character was a backslash
     * (confuses state of wasBackslash.)
     *
     * @param string string to match character for character.
     *
     * @return true if a match.
     */
    protected boolean accept(String string) {
        // Quick test.
        if (string.length() == 0 || !is(string.charAt(0))) {
            return false;
        }

        // Be prepared to retreat if not a match.
        int savedPosition = position;

        nextCodePoint();

        // Check each character.
        for (int i = 1; i < string.length(); i++) {
            if (!is(string.charAt(i))) {
                // Restart if not a match.
                reset(savedPosition);

                return false;
            }

            nextCodePoint();
        }

        return true;
    }

    /**
     * Convert an ASCII digit from its base (8, 10, or 16) to its value. Does not
     * advance character.
     *
     * @param pos         starting position.
     * @param digitRadix  base of number being converted.
     *
     * @return value of digit.
     */
    protected int digit(int pos, int digitRadix) {
        int result;

        // Just an ASCII digit.
        if (inRange('0', '9')) {
            // Fast common case.
            result = character - '0';

            return result < digitRadix ? result : -1;
        }

        // Handle other digits.
        result = isSurrogate() ? Character.digit(codepoint, digitRadix) :
                                 Character.digit(character, digitRadix);

        if (result >= 0 && !isASCII()) {
            log.error(position(), Errors.IllegalNonasciiDigit);
            character = "0123456789abcdef".charAt(result);
        }

        return result;
    }

    /**
     * Returns the input buffer. Unicode escape sequences are not translated.
     *
     * @return the input buffer.
     */
    public char[] getRawCharacters() {
        return length == buffer.length ? buffer : Arrays.copyOf(buffer, length);
    }

    /**
     * Returns a copy of a character array subset of the input buffer.
     * The returned array begins at the {@code beginIndex} and
     * extends to the character at index {@code endIndex - 1}.
     * Thus the length of the substring is {@code endIndex-beginIndex}.
     * This behavior is like
     * {@code String.substring(beginIndex, endIndex)}.
     * Unicode escape sequences are not translated.
     *
     * @param  beginIndex the beginning index, inclusive.
     * @param  endIndex the ending index, exclusive.
     *
     * @throws ArrayIndexOutOfBoundsException if either offset is outside of the
     *         array bounds
     */
    public char[] getRawCharacters(int beginIndex, int endIndex) {
        return Arrays.copyOfRange(buffer, beginIndex, endIndex);
    }

    /**
     * This is a specialized version of UnicodeReader that keeps track of the
     * column position within a given character stream. Used for Javadoc
     * processing to build a table for mapping positions in the comment string
     * to positions in the source file.
     */
    static class PositionTrackingReader extends UnicodeReader {
        /**
         * Offset from the beginning of the original reader buffer.
         */
        private final int offset;

        /**
         * Current column in the comment.
         */
        private int column;

        /**
         * Constructor.
         *
         * @param sf      Scan factory.
         * @param array   Array containing contents of source.
         * @param offset  Position offset in original source buffer.
         */
        protected PositionTrackingReader(ScannerFactory sf, char[] array, int offset) {
            super(sf, array, array.length);
            this.offset = offset;
            this.column = 0;
        }

        /**
         * Advances the current character to the next character. Tracks column.
         *
         * @return next character.
         */
        @Override
        protected char next() {
            super.next();

            if (isOneOf('\n', '\r', '\f')) {
                column = 0;
            } else if (is('\t')) {
                column = tabulate(column);
            } else {
                column++;
            }

            return get();
        }

        /**
         * Returns the current column.
         *
         * @return  the current column.
         */
        protected int column() {
            return column;
        }

        /**
         * Returns position relative to the original source buffer.
         *
         * @return
         */
        protected int offsetPosition() {
            return position() + offset;
        }
    }

}
