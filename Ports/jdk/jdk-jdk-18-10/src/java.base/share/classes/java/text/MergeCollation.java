/*
 * Copyright (c) 1996, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;

/**
 * Utility class for normalizing and merging patterns for collation.
 * Patterns are strings of the form <entry>*, where <entry> has the
 * form:
 * <pattern> := <entry>*
 * <entry> := <separator><chars>{"/"<extension>}
 * <separator> := "=", ",", ";", "<", "&"
 * <chars>, and <extension> are both arbitrary strings.
 * unquoted whitespaces are ignored.
 * 'xxx' can be used to quote characters
 * One difference from Collator is that & is used to reset to a current
 * point. Or, in other words, it introduces a new sequence which is to
 * be added to the old.
 * That is: "a < b < c < d" is the same as "a < b & b < c & c < d" OR
 * "a < b < d & b < c"
 * XXX: make '' be a single quote.
 * @see PatternEntry
 * @author             Mark Davis, Helena Shih
 */

final class MergeCollation {

    /**
     * Creates from a pattern
     * @throws    ParseException If the input pattern is incorrect.
     */
    public MergeCollation(String pattern) throws ParseException
    {
        for (int i = 0; i < statusArray.length; i++)
            statusArray[i] = 0;
        setPattern(pattern);
    }

    /**
     * recovers current pattern
     */
    public String getPattern() {
        return getPattern(true);
    }

    /**
     * recovers current pattern.
     * @param withWhiteSpace puts spacing around the entries, and \n
     * before & and <
     */
    public String getPattern(boolean withWhiteSpace) {
        StringBuffer result = new StringBuffer();
        PatternEntry tmp = null;
        ArrayList<PatternEntry> extList = null;
        int i;
        for (i = 0; i < patterns.size(); ++i) {
            PatternEntry entry = patterns.get(i);
            if (!entry.extension.isEmpty()) {
                if (extList == null)
                    extList = new ArrayList<>();
                extList.add(entry);
            } else {
                if (extList != null) {
                    PatternEntry last = findLastWithNoExtension(i-1);
                    for (int j = extList.size() - 1; j >= 0 ; j--) {
                        tmp = extList.get(j);
                        tmp.addToBuffer(result, false, withWhiteSpace, last);
                    }
                    extList = null;
                }
                entry.addToBuffer(result, false, withWhiteSpace, null);
            }
        }
        if (extList != null) {
            PatternEntry last = findLastWithNoExtension(i-1);
            for (int j = extList.size() - 1; j >= 0 ; j--) {
                tmp = extList.get(j);
                tmp.addToBuffer(result, false, withWhiteSpace, last);
            }
            extList = null;
        }
        return result.toString();
    }

    private final PatternEntry findLastWithNoExtension(int i) {
        for (--i;i >= 0; --i) {
            PatternEntry entry = patterns.get(i);
            if (entry.extension.isEmpty()) {
                return entry;
            }
        }
        return null;
    }

    /**
     * emits the pattern for collation builder.
     * @return emits the string in the format understable to the collation
     * builder.
     */
    public String emitPattern() {
        return emitPattern(true);
    }

    /**
     * emits the pattern for collation builder.
     * @param withWhiteSpace puts spacing around the entries, and \n
     * before & and <
     * @return emits the string in the format understable to the collation
     * builder.
     */
    public String emitPattern(boolean withWhiteSpace) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < patterns.size(); ++i)
        {
            PatternEntry entry = patterns.get(i);
            if (entry != null) {
                entry.addToBuffer(result, true, withWhiteSpace, null);
            }
        }
        return result.toString();
    }

    /**
     * sets the pattern.
     */
    public void setPattern(String pattern) throws ParseException
    {
        patterns.clear();
        addPattern(pattern);
    }

    /**
     * adds a pattern to the current one.
     * @param pattern the new pattern to be added
     */
    public void addPattern(String pattern) throws ParseException
    {
        if (pattern == null)
            return;

        PatternEntry.Parser parser = new PatternEntry.Parser(pattern);

        PatternEntry entry = parser.next();
        while (entry != null) {
            fixEntry(entry);
            entry = parser.next();
        }
    }

    /**
     * gets count of separate entries
     * @return the size of pattern entries
     */
    public int getCount() {
        return patterns.size();
    }

    /**
     * gets count of separate entries
     * @param index the offset of the desired pattern entry
     * @return the requested pattern entry
     */
    public PatternEntry getItemAt(int index) {
        return patterns.get(index);
    }

    //============================================================
    // privates
    //============================================================
    ArrayList<PatternEntry> patterns = new ArrayList<>(); // a list of PatternEntries

    private transient PatternEntry saveEntry = null;
    private transient PatternEntry lastEntry = null;

    // This is really used as a local variable inside fixEntry, but we cache
    // it here to avoid newing it up every time the method is called.
    private transient StringBuffer excess = new StringBuffer();

    //
    // When building a MergeCollation, we need to do lots of searches to see
    // whether a given entry is already in the table.  Since we're using an
    // array, this would make the algorithm O(N*N).  To speed things up, we
    // use this bit array to remember whether the array contains any entries
    // starting with each Unicode character.  If not, we can avoid the search.
    // Using BitSet would make this easier, but it's significantly slower.
    //
    private transient byte[] statusArray = new byte[8192];
    private final byte BITARRAYMASK = (byte)0x1;
    private final int  BYTEPOWER = 3;
    private final int  BYTEMASK = (1 << BYTEPOWER) - 1;

    /*
      If the strength is RESET, then just change the lastEntry to
      be the current. (If the current is not in patterns, signal an error).
      If not, then remove the current entry, and add it after lastEntry
      (which is usually at the end).
      */
    private final void fixEntry(PatternEntry newEntry) throws ParseException
    {
        // check to see whether the new entry has the same characters as the previous
        // entry did (this can happen when a pattern declaring a difference between two
        // strings that are canonically equivalent is normalized).  If so, and the strength
        // is anything other than IDENTICAL or RESET, throw an exception (you can't
        // declare a string to be unequal to itself).       --rtg 5/24/99
        if (lastEntry != null && newEntry.chars.equals(lastEntry.chars)
                && newEntry.extension.equals(lastEntry.extension)) {
            if (newEntry.strength != Collator.IDENTICAL
                && newEntry.strength != PatternEntry.RESET) {
                    throw new ParseException("The entries " + lastEntry + " and "
                            + newEntry + " are adjacent in the rules, but have conflicting "
                            + "strengths: A character can't be unequal to itself.", -1);
            } else {
                // otherwise, just skip this entry and behave as though you never saw it
                return;
            }
        }

        boolean changeLastEntry = true;
        if (newEntry.strength != PatternEntry.RESET) {
            int oldIndex = -1;

            if ((newEntry.chars.length() == 1)) {

                char c = newEntry.chars.charAt(0);
                int statusIndex = c >> BYTEPOWER;
                byte bitClump = statusArray[statusIndex];
                byte setBit = (byte)(BITARRAYMASK << (c & BYTEMASK));

                if (bitClump != 0 && (bitClump & setBit) != 0) {
                    oldIndex = patterns.lastIndexOf(newEntry);
                } else {
                    // We're going to add an element that starts with this
                    // character, so go ahead and set its bit.
                    statusArray[statusIndex] = (byte)(bitClump | setBit);
                }
            } else {
                oldIndex = patterns.lastIndexOf(newEntry);
            }
            if (oldIndex != -1) {
                patterns.remove(oldIndex);
            }

            excess.setLength(0);
            int lastIndex = findLastEntry(lastEntry, excess);

            if (excess.length() != 0) {
                newEntry.extension = excess + newEntry.extension;
                if (lastIndex != patterns.size()) {
                    lastEntry = saveEntry;
                    changeLastEntry = false;
                }
            }
            if (lastIndex == patterns.size()) {
                patterns.add(newEntry);
                saveEntry = newEntry;
            } else {
                patterns.add(lastIndex, newEntry);
            }
        }
        if (changeLastEntry) {
            lastEntry = newEntry;
        }
    }

    private final int findLastEntry(PatternEntry entry,
                              StringBuffer excessChars) throws ParseException
    {
        if (entry == null)
            return 0;

        if (entry.strength != PatternEntry.RESET) {
            // Search backwards for string that contains this one;
            // most likely entry is last one

            int oldIndex = -1;
            if ((entry.chars.length() == 1)) {
                int index = entry.chars.charAt(0) >> BYTEPOWER;
                if ((statusArray[index] &
                    (BITARRAYMASK << (entry.chars.charAt(0) & BYTEMASK))) != 0) {
                    oldIndex = patterns.lastIndexOf(entry);
                }
            } else {
                oldIndex = patterns.lastIndexOf(entry);
            }
            if ((oldIndex == -1))
                throw new ParseException("couldn't find last entry: "
                                          + entry, oldIndex);
            return oldIndex + 1;
        } else {
            int i;
            for (i = patterns.size() - 1; i >= 0; --i) {
                PatternEntry e = patterns.get(i);
                if (e.chars.regionMatches(0,entry.chars,0,
                                              e.chars.length())) {
                    excessChars.append(entry.chars, e.chars.length(),
                            entry.chars.length());
                    break;
                }
            }
            if (i == -1)
                throw new ParseException("couldn't find: " + entry, i);
            return i + 1;
        }
    }
}
