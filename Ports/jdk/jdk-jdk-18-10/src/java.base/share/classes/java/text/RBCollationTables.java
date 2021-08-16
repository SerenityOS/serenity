/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * (C) Copyright IBM Corp. 1996-1998 - All Rights Reserved
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

import java.util.Vector;
import sun.text.UCompactIntArray;
import sun.text.IntHashtable;

/**
 * This class contains the static state of a RuleBasedCollator: The various
 * tables that are used by the collation routines.  Several RuleBasedCollators
 * can share a single RBCollationTables object, easing memory requirements and
 * improving performance.
 */
final class RBCollationTables {
    //===========================================================================================
    //  The following diagram shows the data structure of the RBCollationTables object.
    //  Suppose we have the rule, where 'o-umlaut' is the unicode char 0x00F6.
    //  "a, A < b, B < c, C, ch, cH, Ch, CH < d, D ... < o, O; 'o-umlaut'/E, 'O-umlaut'/E ...".
    //  What the rule says is, sorts 'ch'ligatures and 'c' only with tertiary difference and
    //  sorts 'o-umlaut' as if it's always expanded with 'e'.
    //
    // mapping table                     contracting list           expanding list
    // (contains all unicode char
    //  entries)                   ___    ____________       _________________________
    //  ________                +>|_*_|->|'c' |v('c') |  +>|v('o')|v('umlaut')|v('e')|
    // |_\u0001_|-> v('\u0001') | |_:_|  |------------|  | |-------------------------|
    // |_\u0002_|-> v('\u0002') | |_:_|  |'ch'|v('ch')|  | |             :           |
    // |____:___|               | |_:_|  |------------|  | |-------------------------|
    // |____:___|               |        |'cH'|v('cH')|  | |             :           |
    // |__'a'___|-> v('a')      |        |------------|  | |-------------------------|
    // |__'b'___|-> v('b')      |        |'Ch'|v('Ch')|  | |             :           |
    // |____:___|               |        |------------|  | |-------------------------|
    // |____:___|               |        |'CH'|v('CH')|  | |             :           |
    // |___'c'__|----------------         ------------   | |-------------------------|
    // |____:___|                                        | |             :           |
    // |o-umlaut|----------------------------------------  |_________________________|
    // |____:___|
    //
    // Noted by Helena Shih on 6/23/97
    //============================================================================================

    public RBCollationTables(String rules, int decmp) throws ParseException {
        this.rules = rules;

        RBTableBuilder builder = new RBTableBuilder(new BuildAPI());
        builder.build(rules, decmp); // this object is filled in through
                                            // the BuildAPI object
    }

    final class BuildAPI {
        /**
         * Private constructor.  Prevents anyone else besides RBTableBuilder
         * from gaining direct access to the internals of this class.
         */
        private BuildAPI() {
        }

        /**
         * This function is used by RBTableBuilder to fill in all the members of this
         * object.  (Effectively, the builder class functions as a "friend" of this
         * class, but to avoid changing too much of the logic, it carries around "shadow"
         * copies of all these variables until the end of the build process and then
         * copies them en masse into the actual tables object once all the construction
         * logic is complete.  This function does that "copying en masse".
         * @param f2ary The value for frenchSec (the French-secondary flag)
         * @param swap The value for SE Asian swapping rule
         * @param map The collator's character-mapping table (the value for mapping)
         * @param cTbl The collator's contracting-character table (the value for contractTable)
         * @param eTbl The collator's expanding-character table (the value for expandTable)
         * @param cFlgs The hash table of characters that participate in contracting-
         *              character sequences (the value for contractFlags)
         * @param mso The value for maxSecOrder
         * @param mto The value for maxTerOrder
         */
        void fillInTables(boolean f2ary,
                          boolean swap,
                          UCompactIntArray map,
                          Vector<Vector<EntryPair>> cTbl,
                          Vector<int[]> eTbl,
                          IntHashtable cFlgs,
                          short mso,
                          short mto) {
            frenchSec = f2ary;
            seAsianSwapping = swap;
            mapping = map;
            contractTable = cTbl;
            expandTable = eTbl;
            contractFlags = cFlgs;
            maxSecOrder = mso;
            maxTerOrder = mto;
        }
    }

    /**
     * Gets the table-based rules for the collation object.
     * @return returns the collation rules that the table collation object
     * was created from.
     */
    public String getRules()
    {
        return rules;
    }

    public boolean isFrenchSec() {
        return frenchSec;
    }

    public boolean isSEAsianSwapping() {
        return seAsianSwapping;
    }

    // ==============================================================
    // internal (for use by CollationElementIterator)
    // ==============================================================

    /**
     *  Get the entry of hash table of the contracting string in the collation
     *  table.
     *  @param ch the starting character of the contracting string
     */
    Vector<EntryPair> getContractValues(int ch)
    {
        int index = mapping.elementAt(ch);
        return getContractValuesImpl(index - CONTRACTCHARINDEX);
    }

    //get contract values from contractTable by index
    private Vector<EntryPair> getContractValuesImpl(int index)
    {
        if (index >= 0)
        {
            return contractTable.elementAt(index);
        }
        else // not found
        {
            return null;
        }
    }

    /**
     * Returns true if this character appears anywhere in a contracting
     * character sequence.  (Used by CollationElementIterator.setOffset().)
     */
    boolean usedInContractSeq(int c) {
        return contractFlags.get(c) == 1;
    }

    /**
     * Return the maximum length of any expansion sequences that end
     * with the specified comparison order.
     *
     * @param order a collation order returned by previous or next.
     * @return the maximum length of any expansion seuences ending
     *         with the specified order.
     *
     * @see CollationElementIterator#getMaxExpansion
     */
    int getMaxExpansion(int order) {
        int result = 1;

        if (expandTable != null) {
            // Right now this does a linear search through the entire
            // expansion table.  If a collator had a large number of expansions,
            // this could cause a performance problem, but in practise that
            // rarely happens
            for (int i = 0; i < expandTable.size(); i++) {
                int[] valueList = expandTable.elementAt(i);
                int length = valueList.length;

                if (length > result && valueList[length-1] == order) {
                    result = length;
                }
            }
        }

        return result;
    }

    /**
     * Get the entry of hash table of the expanding string in the collation
     * table.
     * @param idx the index of the expanding string value list
     */
    final int[] getExpandValueList(int idx) {
        return expandTable.elementAt(idx - EXPANDCHARINDEX);
    }

    /**
     * Get the comarison order of a character from the collation table.
     * @return the comparison order of a character.
     */
    int getUnicodeOrder(int ch) {
        return mapping.elementAt(ch);
    }

    short getMaxSecOrder() {
        return maxSecOrder;
    }

    short getMaxTerOrder() {
        return maxTerOrder;
    }

    /**
     * Reverse a string.
     */
    //shemran/Note: this is used for secondary order value reverse, no
    //              need to consider supplementary pair.
    static void reverse (StringBuffer result, int from, int to)
    {
        int i = from;
        char swap;

        int j = to - 1;
        while (i < j) {
            swap =  result.charAt(i);
            result.setCharAt(i, result.charAt(j));
            result.setCharAt(j, swap);
            i++;
            j--;
        }
    }

    static final int getEntry(Vector<EntryPair> list, String name, boolean fwd) {
        for (int i = 0; i < list.size(); i++) {
            EntryPair pair = list.elementAt(i);
            if (pair.fwd == fwd && pair.entryName.equals(name)) {
                return i;
            }
        }
        return UNMAPPED;
    }

    // ==============================================================
    // constants
    // ==============================================================
    //sherman/Todo: is the value big enough?????
    static final int EXPANDCHARINDEX = 0x7E000000; // Expand index follows
    static final int CONTRACTCHARINDEX = 0x7F000000;  // contract indexes follow
    static final int UNMAPPED = 0xFFFFFFFF;

    static final int PRIMARYORDERMASK = 0xffff0000;
    static final int SECONDARYORDERMASK = 0x0000ff00;
    static final int TERTIARYORDERMASK = 0x000000ff;
    static final int PRIMARYDIFFERENCEONLY = 0xffff0000;
    static final int SECONDARYDIFFERENCEONLY = 0xffffff00;
    static final int PRIMARYORDERSHIFT = 16;
    static final int SECONDARYORDERSHIFT = 8;

    // ==============================================================
    // instance variables
    // ==============================================================
    private String rules = null;
    private boolean frenchSec = false;
    private boolean seAsianSwapping = false;

    private UCompactIntArray mapping = null;
    private Vector<Vector<EntryPair>> contractTable = null;
    private Vector<int[]> expandTable = null;
    private IntHashtable contractFlags = null;

    private short maxSecOrder = 0;
    private short maxTerOrder = 0;
}
