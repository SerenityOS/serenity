/*
 * Copyright (c) 1996, 2000, Oracle and/or its affiliates. All rights reserved.
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

/*
 * (C) Copyright Taligent, Inc. 1996, 1997 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996, 1997 - All Rights Reserved
 *
 *   The original version of this source code and documentation is copyrighted
 * and owned by Taligent, Inc., a wholly-owned subsidiary of IBM. These
 * materials are provided under terms of a License Agreement between Taligent
 * and Sun. This technology is protected by multiple US and International
 * patents. This notice and attribution to Taligent may not be removed.
 *   Taligent is a registered trademark of Taligent, Inc.
 *
 */

package java.text;

import java.lang.Character;

/**
 * Utility class for normalizing and merging patterns for collation.
 * This is to be used with MergeCollation for adding patterns to an
 * existing rule table.
 * @see        MergeCollation
 * @author     Mark Davis, Helena Shih
 */

class PatternEntry {
    /**
     * Gets the current extension, quoted
     */
    public void appendQuotedExtension(StringBuffer toAddTo) {
        appendQuoted(extension,toAddTo);
    }

    /**
     * Gets the current chars, quoted
     */
    public void appendQuotedChars(StringBuffer toAddTo) {
        appendQuoted(chars,toAddTo);
    }

    /**
     * WARNING this is used for searching in a Vector.
     * Because Vector.indexOf doesn't take a comparator,
     * this method is ill-defined and ignores strength.
     */
    public boolean equals(Object obj) {
        if (obj == null) return false;
        PatternEntry other = (PatternEntry) obj;
        boolean result = chars.equals(other.chars);
        return result;
    }

    public int hashCode() {
        return chars.hashCode();
    }

    /**
     * For debugging.
     */
    public String toString() {
        StringBuffer result = new StringBuffer();
        addToBuffer(result, true, false, null);
        return result.toString();
    }

    /**
     * Gets the strength of the entry.
     */
    final int getStrength() {
        return strength;
    }

    /**
     * Gets the expanding characters of the entry.
     */
    final String getExtension() {
        return extension;
    }

    /**
     * Gets the core characters of the entry.
     */
    final String getChars() {
        return chars;
    }

    // ===== privates =====

    void addToBuffer(StringBuffer toAddTo,
                     boolean showExtension,
                     boolean showWhiteSpace,
                     PatternEntry lastEntry)
    {
        if (showWhiteSpace && toAddTo.length() > 0)
            if (strength == Collator.PRIMARY || lastEntry != null)
                toAddTo.append('\n');
            else
                toAddTo.append(' ');
        if (lastEntry != null) {
            toAddTo.append('&');
            if (showWhiteSpace)
                toAddTo.append(' ');
            lastEntry.appendQuotedChars(toAddTo);
            appendQuotedExtension(toAddTo);
            if (showWhiteSpace)
                toAddTo.append(' ');
        }
        var c = switch (strength) {
            case Collator.IDENTICAL -> '=';
            case Collator.TERTIARY  -> ',';
            case Collator.SECONDARY -> ';';
            case Collator.PRIMARY   -> '<';
            case RESET              -> '&';
            case UNSET              -> '?';

            default -> throw new IllegalStateException("Unexpected value: " + strength);
        };
        toAddTo.append(c);

        if (showWhiteSpace)
            toAddTo.append(' ');
        appendQuoted(chars,toAddTo);
        if (showExtension && !extension.isEmpty()) {
            toAddTo.append('/');
            appendQuoted(extension,toAddTo);
        }
    }

    static void appendQuoted(String chars, StringBuffer toAddTo) {
        boolean inQuote = false;
        char ch = chars.charAt(0);
        if (Character.isSpaceChar(ch)) {
            inQuote = true;
            toAddTo.append('\'');
        } else {
          if (PatternEntry.isSpecialChar(ch)) {
                inQuote = true;
                toAddTo.append('\'');
            } else {
                switch (ch) {
                    case 0x0010: case '\f': case '\r':
                    case '\t': case '\n':  case '@':
                    inQuote = true;
                    toAddTo.append('\'');
                    break;
                case '\'':
                    inQuote = true;
                    toAddTo.append('\'');
                    break;
                default:
                    if (inQuote) {
                        inQuote = false; toAddTo.append('\'');
                    }
                    break;
                }
           }
        }
        toAddTo.append(chars);
        if (inQuote)
            toAddTo.append('\'');
    }

    //========================================================================
    // Parsing a pattern into a list of PatternEntries....
    //========================================================================

    PatternEntry(int strength,
                 StringBuffer chars,
                 StringBuffer extension)
    {
        this.strength = strength;
        this.chars = chars.toString();
        this.extension = (extension.length() > 0) ? extension.toString()
                                                  : "";
    }

    static class Parser {
        private String pattern;
        private int i;

        public Parser(String pattern) {
            this.pattern = pattern;
            this.i = 0;
        }

        public PatternEntry next() throws ParseException {
            int newStrength = UNSET;

            newChars.setLength(0);
            newExtension.setLength(0);

            boolean inChars = true;
            boolean inQuote = false;
        mainLoop:
            while (i < pattern.length()) {
                char ch = pattern.charAt(i);
                if (inQuote) {
                    if (ch == '\'') {
                        inQuote = false;
                    } else {
                        if (newChars.length() == 0) newChars.append(ch);
                        else if (inChars) newChars.append(ch);
                        else newExtension.append(ch);
                    }
                } else switch (ch) {
                case '=': if (newStrength != UNSET) break mainLoop;
                    newStrength = Collator.IDENTICAL; break;
                case ',': if (newStrength != UNSET) break mainLoop;
                    newStrength = Collator.TERTIARY; break;
                case ';': if (newStrength != UNSET) break mainLoop;
                    newStrength = Collator.SECONDARY; break;
                case '<': if (newStrength != UNSET) break mainLoop;
                    newStrength = Collator.PRIMARY; break;
                case '&': if (newStrength != UNSET) break mainLoop;
                    newStrength = RESET; break;
                case '\t':
                case '\n':
                case '\f':
                case '\r':
                case ' ': break; // skip whitespace TODO use Character
                case '/': inChars = false; break;
                case '\'':
                    inQuote = true;
                    ch = pattern.charAt(++i);
                    if (newChars.length() == 0) newChars.append(ch);
                    else if (inChars) newChars.append(ch);
                    else newExtension.append(ch);
                    break;
                default:
                    if (newStrength == UNSET) {
                        throw new ParseException
                            ("missing char (=,;<&) : " +
                             pattern.substring(i,
                                (i+10 < pattern.length()) ?
                                 i+10 : pattern.length()),
                             i);
                    }
                    if (PatternEntry.isSpecialChar(ch) && (inQuote == false))
                        throw new ParseException
                            ("Unquoted punctuation character : " + Integer.toString(ch, 16), i);
                    if (inChars) {
                        newChars.append(ch);
                    } else {
                        newExtension.append(ch);
                    }
                    break;
                }
                i++;
            }
            if (newStrength == UNSET)
                return null;
            if (newChars.length() == 0) {
                throw new ParseException
                    ("missing chars (=,;<&): " +
                      pattern.substring(i,
                          (i+10 < pattern.length()) ?
                           i+10 : pattern.length()),
                     i);
            }

            return new PatternEntry(newStrength, newChars, newExtension);
        }

        // We re-use these objects in order to improve performance
        private StringBuffer newChars = new StringBuffer();
        private StringBuffer newExtension = new StringBuffer();

    }

    static boolean isSpecialChar(char ch) {
        return ((ch == '\u0020') ||
                ((ch <= '\u002F') && (ch >= '\u0022')) ||
                ((ch <= '\u003F') && (ch >= '\u003A')) ||
                ((ch <= '\u0060') && (ch >= '\u005B')) ||
                ((ch <= '\u007E') && (ch >= '\u007B')));
    }


    static final int RESET = -2;
    static final int UNSET = -1;

    int strength = UNSET;
    String chars = "";
    String extension = "";
}
