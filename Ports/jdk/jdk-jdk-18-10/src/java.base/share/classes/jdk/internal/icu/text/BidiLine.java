/*
 * Copyright (c) 2009, 2021, Oracle and/or its affiliates. All rights reserved.
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
*******************************************************************************
*   Copyright (C) 2001-2014, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*/
/* Written by Simon Montagu, Matitiahu Allouche
 * (ported from C code written by Markus W. Scherer)
 */

package jdk.internal.icu.text;

import java.text.Bidi;
import java.util.Arrays;

final class BidiLine {

    /*
     * General remarks about the functions in this file:
     *
     * These functions deal with the aspects of potentially mixed-directional
     * text in a single paragraph or in a line of a single paragraph
     * which has already been processed according to
     * the Unicode 3.0 Bidi algorithm as defined in
     * <a href="http://www.unicode.org/reports/tr9/">Unicode Standard Annex #9:
     * Unicode Bidirectional Algorithm</a>, version 13,
     * also described in The Unicode Standard, Version 4.0.1 .
     *
     * This means that there is a Bidi object with a levels
     * and a dirProps array.
     * paraLevel and direction are also set.
     * Only if the length of the text is zero, then levels==dirProps==NULL.
     *
     * The overall directionality of the paragraph
     * or line is used to bypass the reordering steps if possible.
     * Even purely RTL text does not need reordering there because
     * the getLogical/VisualIndex() methods can compute the
     * index on the fly in such a case.
     *
     * The implementation of the access to same-level-runs and of the reordering
     * do attempt to provide better performance and less memory usage compared to
     * a direct implementation of especially rule (L2) with an array of
     * one (32-bit) integer per text character.
     *
     * Here, the levels array is scanned as soon as necessary, and a vector of
     * same-level-runs is created. Reordering then is done on this vector.
     * For each run of text positions that were resolved to the same level,
     * only 8 bytes are stored: the first text position of the run and the visual
     * position behind the run after reordering.
     * One sign bit is used to hold the directionality of the run.
     * This is inefficient if there are many very short runs. If the average run
     * length is <2, then this uses more memory.
     *
     * In a further attempt to save memory, the levels array is never changed
     * after all the resolution rules (Xn, Wn, Nn, In).
     * Many methods have to consider the field trailingWSStart:
     * if it is less than length, then there is an implicit trailing run
     * at the paraLevel,
     * which is not reflected in the levels array.
     * This allows a line Bidi object to use the same levels array as
     * its paragraph parent object.
     *
     * When a Bidi object is created for a line of a paragraph, then the
     * paragraph's levels and dirProps arrays are reused by way of setting
     * a pointer into them, not by copying. This again saves memory and forbids to
     * change the now shared levels for (L1).
     */

    /* handle trailing WS (L1) -------------------------------------------------- */

    /*
     * setTrailingWSStart() sets the start index for a trailing
     * run of WS in the line. This is necessary because we do not modify
     * the paragraph's levels array that we just point into.
     * Using trailingWSStart is another form of performing (L1).
     *
     * To make subsequent operations easier, we also include the run
     * before the WS if it is at the paraLevel - we merge the two here.
     *
     * This method is called only from setLine(), so paraLevel is
     * set correctly for the line even when contextual multiple paragraphs.
     */

    static void setTrailingWSStart(BidiBase bidiBase)
    {
        byte[] dirProps = bidiBase.dirProps;
        byte[] levels = bidiBase.levels;
        int start = bidiBase.length;
        byte paraLevel = bidiBase.paraLevel;

        /* If the line is terminated by a block separator, all preceding WS etc...
           are already set to paragraph level.
           Setting trailingWSStart to pBidi->length will avoid changing the
           level of B chars from 0 to paraLevel in getLevels when
           orderParagraphsLTR==TRUE
        */
        if (dirProps[start - 1] == BidiBase.B) {
            bidiBase.trailingWSStart = start;   /* currently == bidiBase.length */
            return;
        }
        /* go backwards across all WS, BN, explicit codes */
        while (start > 0 &&
                (BidiBase.DirPropFlag(dirProps[start - 1]) & BidiBase.MASK_WS) != 0) {
            --start;
        }

        /* if the WS run can be merged with the previous run then do so here */
        while (start > 0 && levels[start - 1] == paraLevel) {
            --start;
        }

        bidiBase.trailingWSStart=start;
    }

    static Bidi setLine(BidiBase paraBidi,
                              Bidi newBidi, BidiBase lineBidi,
                              int start, int limit) {
        int length;

        /* set the values in lineBidi from its paraBidi parent */
        /* class members are already initialized to 0 */
        // lineBidi.paraBidi = null;        /* mark unfinished setLine */
        // lineBidi.flags = 0;
        // lineBidi.controlCount = 0;

        length = lineBidi.length = lineBidi.originalLength =
                lineBidi.resultLength = limit - start;

        lineBidi.text = new char[length];
        System.arraycopy(paraBidi.text, start, lineBidi.text, 0, length);
        lineBidi.paraLevel = paraBidi.GetParaLevelAt(start);
        lineBidi.paraCount = paraBidi.paraCount;
        lineBidi.runs = new BidiRun[0];
        lineBidi.reorderingMode = paraBidi.reorderingMode;
        lineBidi.reorderingOptions = paraBidi.reorderingOptions;
        if (paraBidi.controlCount > 0) {
            int j;
            for (j = start; j < limit; j++) {
                if (BidiBase.IsBidiControlChar(paraBidi.text[j])) {
                    lineBidi.controlCount++;
                }
            }
            lineBidi.resultLength -= lineBidi.controlCount;
        }
        /* copy proper subset of DirProps */
        lineBidi.getDirPropsMemory(length);
        lineBidi.dirProps = lineBidi.dirPropsMemory;
        System.arraycopy(paraBidi.dirProps, start, lineBidi.dirProps, 0,
                         length);
        /* copy proper subset of Levels */
        lineBidi.getLevelsMemory(length);
        lineBidi.levels = lineBidi.levelsMemory;
        System.arraycopy(paraBidi.levels, start, lineBidi.levels, 0,
                         length);
        lineBidi.runCount = -1;

        if (paraBidi.direction != BidiBase.MIXED) {
            /* the parent is already trivial */
            lineBidi.direction = paraBidi.direction;

            /*
             * The parent's levels are all either
             * implicitly or explicitly ==paraLevel;
             * do the same here.
             */
            if (paraBidi.trailingWSStart <= start) {
                lineBidi.trailingWSStart = 0;
            } else if (paraBidi.trailingWSStart < limit) {
                lineBidi.trailingWSStart = paraBidi.trailingWSStart - start;
            } else {
                lineBidi.trailingWSStart = length;
            }
        } else {
            byte[] levels = lineBidi.levels;
            int i, trailingWSStart;
            byte level;

            setTrailingWSStart(lineBidi);
            trailingWSStart = lineBidi.trailingWSStart;

            /* recalculate lineBidiBase.direction */
            if (trailingWSStart == 0) {
                /* all levels are at paraLevel */
                lineBidi.direction = (byte)(lineBidi.paraLevel & 1);
            } else {
                /* get the level of the first character */
                level = (byte)(levels[0] & 1);

                /* if there is anything of a different level, then the line
                   is mixed */
                if (trailingWSStart < length &&
                    (lineBidi.paraLevel & 1) != level) {
                    /* the trailing WS is at paraLevel, which differs from
                       levels[0] */
                    lineBidi.direction = BidiBase.MIXED;
                } else {
                    /* see if levels[1..trailingWSStart-1] have the same
                       direction as levels[0] and paraLevel */
                    for (i = 1; ; i++) {
                        if (i == trailingWSStart) {
                            /* the direction values match those in level */
                            lineBidi.direction = level;
                            break;
                        } else if ((levels[i] & 1) != level) {
                            lineBidi.direction = BidiBase.MIXED;
                            break;
                        }
                    }
                }
            }

            switch(lineBidi.direction) {
                case Bidi.DIRECTION_LEFT_TO_RIGHT:
                    /* make sure paraLevel is even */
                    lineBidi.paraLevel = (byte)
                        ((lineBidi.paraLevel + 1) & ~1);

                    /* all levels are implicitly at paraLevel (important for
                       getLevels()) */
                    lineBidi.trailingWSStart = 0;
                    break;
                case Bidi.DIRECTION_RIGHT_TO_LEFT:
                    /* make sure paraLevel is odd */
                    lineBidi.paraLevel |= 1;

                    /* all levels are implicitly at paraLevel (important for
                       getLevels()) */
                    lineBidi.trailingWSStart = 0;
                    break;
                default:
                    break;
            }
        }

        lineBidi.paraBidi = paraBidi;     /* mark successful setLine */

        return newBidi;
    }

    static byte getLevelAt(BidiBase bidiBase, int charIndex)
    {
        /* return paraLevel if in the trailing WS run, otherwise the real level */
        if (bidiBase.direction != BidiBase.MIXED || charIndex >= bidiBase.trailingWSStart) {
            return bidiBase.GetParaLevelAt(charIndex);
        } else {
            return bidiBase.levels[charIndex];
        }
    }

    static byte[] getLevels(BidiBase bidiBase)
    {
        int start = bidiBase.trailingWSStart;
        int length = bidiBase.length;

        if (start != length) {
            /* the current levels array does not reflect the WS run */
            /*
             * After the previous if(), we know that the levels array
             * has an implicit trailing WS run and therefore does not fully
             * reflect itself all the levels.
             * This must be a Bidi object for a line, and
             * we need to create a new levels array.
             */
            /* bidiBase.paraLevel is ok even if contextual multiple paragraphs,
               since bidiBase is a line object                                     */
            Arrays.fill(bidiBase.levels, start, length, bidiBase.paraLevel);

            /* this new levels array is set for the line and reflects the WS run */
            bidiBase.trailingWSStart = length;
        }
        if (length < bidiBase.levels.length) {
            byte[] levels = new byte[length];
            System.arraycopy(bidiBase.levels, 0, levels, 0, length);
            return levels;
        }
        return bidiBase.levels;
    }

    static BidiRun getVisualRun(BidiBase bidiBase, int runIndex) {
        int start = bidiBase.runs[runIndex].start;
        int limit;
        byte level = bidiBase.runs[runIndex].level;

        if (runIndex > 0) {
            limit = start +
                    bidiBase.runs[runIndex].limit -
                    bidiBase.runs[runIndex - 1].limit;
        } else {
            limit = start + bidiBase.runs[0].limit;
        }
        return new BidiRun(start, limit, level);
    }

    /* in trivial cases there is only one trivial run; called by getRuns() */
    private static void getSingleRun(BidiBase bidiBase, byte level) {
        /* simple, single-run case */
        bidiBase.runs = bidiBase.simpleRuns;
        bidiBase.runCount = 1;

        /* fill and reorder the single run */
        bidiBase.runs[0] = new BidiRun(0, bidiBase.length, level);
    }

    /* reorder the runs array (L2) ---------------------------------------------- */

    /*
     * Reorder the same-level runs in the runs array.
     * Here, runCount>1 and maxLevel>=minLevel>=paraLevel.
     * All the visualStart fields=logical start before reordering.
     * The "odd" bits are not set yet.
     *
     * Reordering with this data structure lends itself to some handy shortcuts:
     *
     * Since each run is moved but not modified, and since at the initial maxLevel
     * each sequence of same-level runs consists of only one run each, we
     * don't need to do anything there and can predecrement maxLevel.
     * In many simple cases, the reordering is thus done entirely in the
     * index mapping.
     * Also, reordering occurs only down to the lowest odd level that occurs,
     * which is minLevel|1. However, if the lowest level itself is odd, then
     * in the last reordering the sequence of the runs at this level or higher
     * will be all runs, and we don't need the elaborate loop to search for them.
     * This is covered by ++minLevel instead of minLevel|=1 followed
     * by an extra reorder-all after the reorder-some loop.
     * About a trailing WS run:
     * Such a run would need special treatment because its level is not
     * reflected in levels[] if this is not a paragraph object.
     * Instead, all characters from trailingWSStart on are implicitly at
     * paraLevel.
     * However, for all maxLevel>paraLevel, this run will never be reordered
     * and does not need to be taken into account. maxLevel==paraLevel is only reordered
     * if minLevel==paraLevel is odd, which is done in the extra segment.
     * This means that for the main reordering loop we don't need to consider
     * this run and can --runCount. If it is later part of the all-runs
     * reordering, then runCount is adjusted accordingly.
     */
    private static void reorderLine(BidiBase bidiBase, byte minLevel, byte maxLevel) {

        /* nothing to do? */
        if (maxLevel<=(minLevel|1)) {
            return;
        }

        BidiRun[] runs;
        BidiRun tempRun;
        byte[] levels;
        int firstRun, endRun, limitRun, runCount;

        /*
         * Reorder only down to the lowest odd level
         * and reorder at an odd minLevel in a separate, simpler loop.
         * See comments above for why minLevel is always incremented.
         */
        ++minLevel;

        runs = bidiBase.runs;
        levels = bidiBase.levels;
        runCount = bidiBase.runCount;

        /* do not include the WS run at paraLevel<=old minLevel except in the simple loop */
        if (bidiBase.trailingWSStart < bidiBase.length) {
            --runCount;
        }

        while (--maxLevel >= minLevel) {
            firstRun = 0;

            /* loop for all sequences of runs */
            for ( ; ; ) {
                /* look for a sequence of runs that are all at >=maxLevel */
                /* look for the first run of such a sequence */
                while (firstRun < runCount && levels[runs[firstRun].start] < maxLevel) {
                    ++firstRun;
                }
                if (firstRun >= runCount) {
                    break;  /* no more such runs */
                }

                /* look for the limit run of such a sequence (the run behind it) */
                for (limitRun = firstRun; ++limitRun < runCount &&
                      levels[runs[limitRun].start]>=maxLevel; ) {}

                /* Swap the entire sequence of runs from firstRun to limitRun-1. */
                endRun = limitRun - 1;
                while (firstRun < endRun) {
                    tempRun = runs[firstRun];
                    runs[firstRun] = runs[endRun];
                    runs[endRun] = tempRun;
                    ++firstRun;
                    --endRun;
                }

                if (limitRun == runCount) {
                    break;  /* no more such runs */
                } else {
                    firstRun = limitRun + 1;
                }
            }
        }

        /* now do maxLevel==old minLevel (==odd!), see above */
        if ((minLevel & 1) == 0) {
            firstRun = 0;

            /* include the trailing WS run in this complete reordering */
            if (bidiBase.trailingWSStart == bidiBase.length) {
                --runCount;
            }

            /* Swap the entire sequence of all runs. (endRun==runCount) */
            while (firstRun < runCount) {
                tempRun = runs[firstRun];
                runs[firstRun] = runs[runCount];
                runs[runCount] = tempRun;
                ++firstRun;
                --runCount;
            }
        }
    }

    /* compute the runs array --------------------------------------------------- */

    static int getRunFromLogicalIndex(BidiBase bidiBase, int logicalIndex) {
        BidiRun[] runs = bidiBase.runs;
        int runCount = bidiBase.runCount, visualStart = 0, i, length, logicalStart;

        for (i = 0; i < runCount; i++) {
            length = runs[i].limit - visualStart;
            logicalStart = runs[i].start;
            if ((logicalIndex >= logicalStart) && (logicalIndex < (logicalStart+length))) {
                return i;
            }
            visualStart += length;
        }
        /* we should never get here */
        throw new IllegalStateException("Internal ICU error in getRunFromLogicalIndex");
    }

    /*
     * Compute the runs array from the levels array.
     * After getRuns() returns true, runCount is guaranteed to be >0
     * and the runs are reordered.
     * Odd-level runs have visualStart on their visual right edge and
     * they progress visually to the left.
     * If option OPTION_INSERT_MARKS is set, insertRemove will contain the
     * sum of appropriate LRM/RLM_BEFORE/AFTER flags.
     * If option OPTION_REMOVE_CONTROLS is set, insertRemove will contain the
     * negative number of BiDi control characters within this run.
     */
    static void getRuns(BidiBase bidiBase) {
        /*
         * This method returns immediately if the runs are already set. This
         * includes the case of length==0 (handled in setPara)..
         */
        if (bidiBase.runCount >= 0) {
            return;
        }
        if (bidiBase.direction != BidiBase.MIXED) {
            /* simple, single-run case - this covers length==0 */
            /* bidiBase.paraLevel is ok even for contextual multiple paragraphs */
            getSingleRun(bidiBase, bidiBase.paraLevel);
        } else /* BidiBase.MIXED, length>0 */ {
            /* mixed directionality */
            int length = bidiBase.length, limit;
            byte[] levels = bidiBase.levels;
            int i, runCount;
            byte level = -1;    /* initialize with no valid level */
            /*
             * If there are WS characters at the end of the line
             * and the run preceding them has a level different from
             * paraLevel, then they will form their own run at paraLevel (L1).
             * Count them separately.
             * We need some special treatment for this in order to not
             * modify the levels array which a line Bidi object shares
             * with its paragraph parent and its other line siblings.
             * In other words, for the trailing WS, it may be
             * levels[]!=paraLevel but we have to treat it like it were so.
             */
            limit = bidiBase.trailingWSStart;
            /* count the runs, there is at least one non-WS run, and limit>0 */
            runCount = 0;
            for (i = 0; i < limit; ++i) {
                /* increment runCount at the start of each run */
                if (levels[i] != level) {
                    ++runCount;
                    level = levels[i];
                }
            }

            /*
             * We don't need to see if the last run can be merged with a trailing
             * WS run because setTrailingWSStart() would have done that.
             */
            if (runCount == 1 && limit == length) {
                /* There is only one non-WS run and no trailing WS-run. */
                getSingleRun(bidiBase, levels[0]);
            } else /* runCount>1 || limit<length */ {
                /* allocate and set the runs */
                BidiRun[] runs;
                int runIndex, start;
                byte minLevel = BidiBase.MAX_EXPLICIT_LEVEL + 1;
                byte maxLevel=0;

                /* now, count a (non-mergeable) WS run */
                if (limit < length) {
                    ++runCount;
                }

                /* runCount > 1 */
                bidiBase.getRunsMemory(runCount);
                runs = bidiBase.runsMemory;

                /* set the runs */
                /* FOOD FOR THOUGHT: this could be optimized, e.g.:
                 * 464->444, 484->444, 575->555, 595->555
                 * However, that would take longer. Check also how it would
                 * interact with BiDi control removal and inserting Marks.
                 */
                runIndex = 0;

                /* search for the run limits and initialize visualLimit values with the run lengths */
                i = 0;
                do {
                    /* prepare this run */
                    start = i;
                    level = levels[i];
                    if (level < minLevel) {
                        minLevel = level;
                    }
                    if (level > maxLevel) {
                        maxLevel = level;
                    }

                    /* look for the run limit */
                    while (++i < limit && levels[i] == level) {}

                    /* i is another run limit */
                    runs[runIndex] = new BidiRun(start, i - start, level);
                    ++runIndex;
                } while (i < limit);

                if (limit < length) {
                    /* there is a separate WS run */
                    runs[runIndex] = new BidiRun(limit, length - limit, bidiBase.paraLevel);
                    /* For the trailing WS run, bidiBase.paraLevel is ok even
                       if contextual multiple paragraphs.                   */
                    if (bidiBase.paraLevel < minLevel) {
                        minLevel = bidiBase.paraLevel;
                    }
                }

                /* set the object fields */
                bidiBase.runs = runs;
                bidiBase.runCount = runCount;

                reorderLine(bidiBase, minLevel, maxLevel);

                /* now add the direction flags and adjust the visualLimit's to be just that */
                /* this loop will also handle the trailing WS run */
                limit = 0;
                for (i = 0; i < runCount; ++i) {
                    runs[i].level = levels[runs[i].start];
                    limit = (runs[i].limit += limit);
                }

                /* Set the embedding level for the trailing WS run. */
                /* For a RTL paragraph, it will be the *first* run in visual order. */
                /* For the trailing WS run, bidiBase.paraLevel is ok even if
                   contextual multiple paragraphs.                          */
                if (runIndex < runCount) {
                    int trailingRun = ((bidiBase.paraLevel & 1) != 0)? 0 : runIndex;
                    runs[trailingRun].level = bidiBase.paraLevel;
                }
            }
        }

        /* handle insert LRM/RLM BEFORE/AFTER run */
        if (bidiBase.insertPoints.size > 0) {
            BidiBase.Point point;
            int runIndex, ip;
            for (ip = 0; ip < bidiBase.insertPoints.size; ip++) {
                point = bidiBase.insertPoints.points[ip];
                runIndex = getRunFromLogicalIndex(bidiBase, point.pos);
                bidiBase.runs[runIndex].insertRemove |= point.flag;
            }
        }

        /* handle remove BiDi control characters */
        if (bidiBase.controlCount > 0) {
            int runIndex, ic;
            char c;
            for (ic = 0; ic < bidiBase.length; ic++) {
                c = bidiBase.text[ic];
                if (BidiBase.IsBidiControlChar(c)) {
                    runIndex = getRunFromLogicalIndex(bidiBase, ic);
                    bidiBase.runs[runIndex].insertRemove--;
                }
            }
        }
    }

    static int[] prepareReorder(byte[] levels, byte[] pMinLevel, byte[] pMaxLevel)
    {
        int start;
        byte level, minLevel, maxLevel;

        if (levels == null || levels.length <= 0) {
            return null;
        }

        /* determine minLevel and maxLevel */
        minLevel = BidiBase.MAX_EXPLICIT_LEVEL + 1;
        maxLevel = 0;
        for (start = levels.length; start>0; ) {
            level = levels[--start];
            if (level < 0 || level > (BidiBase.MAX_EXPLICIT_LEVEL + 1)) {
                return null;
            }
            if (level < minLevel) {
                minLevel = level;
            }
            if (level > maxLevel) {
                maxLevel = level;
            }
        }
        pMinLevel[0] = minLevel;
        pMaxLevel[0] = maxLevel;

        /* initialize the index map */
        int[] indexMap = new int[levels.length];
        for (start = levels.length; start > 0; ) {
            --start;
            indexMap[start] = start;
        }

        return indexMap;
    }

    static int[] reorderVisual(byte[] levels)
    {
        byte[] aMinLevel = new byte[1];
        byte[] aMaxLevel = new byte[1];
        int start, end, limit, temp;
        byte minLevel, maxLevel;

        int[] indexMap = prepareReorder(levels, aMinLevel, aMaxLevel);
        if (indexMap == null) {
            return null;
        }

        minLevel = aMinLevel[0];
        maxLevel = aMaxLevel[0];

        /* nothing to do? */
        if (minLevel == maxLevel && (minLevel & 1) == 0) {
            return indexMap;
        }

        /* reorder only down to the lowest odd level */
        minLevel |= 1;

        /* loop maxLevel..minLevel */
        do {
            start = 0;

            /* loop for all sequences of levels to reorder at the current maxLevel */
            for ( ; ; ) {
                /* look for a sequence of levels that are all at >=maxLevel */
                /* look for the first index of such a sequence */
                while (start < levels.length && levels[start] < maxLevel) {
                    ++start;
                }
                if (start >= levels.length) {
                    break;  /* no more such runs */
                }

                /* look for the limit of such a sequence (the index behind it) */
                for (limit = start; ++limit < levels.length && levels[limit] >= maxLevel; ) {}

                /*
                 * Swap the entire interval of indexes from start to limit-1.
                 * We don't need to swap the levels for the purpose of this
                 * algorithm: the sequence of levels that we look at does not
                 * move anyway.
                 */
                end = limit - 1;
                while (start < end) {
                    temp = indexMap[start];
                    indexMap[start] = indexMap[end];
                    indexMap[end] = temp;

                    ++start;
                    --end;
                }

                if (limit == levels.length) {
                    break;  /* no more such sequences */
                } else {
                    start = limit + 1;
                }
            }
        } while (--maxLevel >= minLevel);

        return indexMap;
    }

    static int[] getVisualMap(BidiBase bidiBase)
    {
        /* fill a visual-to-logical index map using the runs[] */
        BidiRun[] runs = bidiBase.runs;
        int logicalStart, visualStart, visualLimit;
        int allocLength = bidiBase.length > bidiBase.resultLength ? bidiBase.length
                                                          : bidiBase.resultLength;
        int[] indexMap = new int[allocLength];

        visualStart = 0;
        int idx = 0;
        for (int j = 0; j < bidiBase.runCount; ++j) {
            logicalStart = runs[j].start;
            visualLimit = runs[j].limit;
            if (runs[j].isEvenRun()) {
                do { /* LTR */
                    indexMap[idx++] = logicalStart++;
                } while (++visualStart < visualLimit);
            } else {
                logicalStart += visualLimit - visualStart;  /* logicalLimit */
                do { /* RTL */
                    indexMap[idx++] = --logicalStart;
                } while (++visualStart < visualLimit);
            }
            /* visualStart==visualLimit; */
        }

        if (bidiBase.insertPoints.size > 0) {
            int markFound = 0, runCount = bidiBase.runCount;
            int insertRemove, i, j, k;
            runs = bidiBase.runs;
            /* count all inserted marks */
            for (i = 0; i < runCount; i++) {
                insertRemove = runs[i].insertRemove;
                if ((insertRemove & (BidiBase.LRM_BEFORE|BidiBase.RLM_BEFORE)) > 0) {
                    markFound++;
                }
                if ((insertRemove & (BidiBase.LRM_AFTER|BidiBase.RLM_AFTER)) > 0) {
                    markFound++;
                }
            }
            /* move back indexes by number of preceding marks */
            k = bidiBase.resultLength;
            for (i = runCount - 1; i >= 0 && markFound > 0; i--) {
                insertRemove = runs[i].insertRemove;
                if ((insertRemove & (BidiBase.LRM_AFTER|BidiBase.RLM_AFTER)) > 0) {
                    indexMap[--k] = BidiBase.MAP_NOWHERE;
                    markFound--;
                }
                visualStart = i > 0 ? runs[i-1].limit : 0;
                for (j = runs[i].limit - 1; j >= visualStart && markFound > 0; j--) {
                    indexMap[--k] = indexMap[j];
                }
                if ((insertRemove & (BidiBase.LRM_BEFORE|BidiBase.RLM_BEFORE)) > 0) {
                    indexMap[--k] = BidiBase.MAP_NOWHERE;
                    markFound--;
                }
            }
        }
        else if (bidiBase.controlCount > 0) {
            int runCount = bidiBase.runCount, logicalEnd;
            int insertRemove, length, i, j, k, m;
            char uchar;
            boolean evenRun;
            runs = bidiBase.runs;
            visualStart = 0;
            /* move forward indexes by number of preceding controls */
            k = 0;
            for (i = 0; i < runCount; i++, visualStart += length) {
                length = runs[i].limit - visualStart;
                insertRemove = runs[i].insertRemove;
                /* if no control found yet, nothing to do in this run */
                if ((insertRemove == 0) && (k == visualStart)) {
                    k += length;
                    continue;
                }
                /* if no control in this run */
                if (insertRemove == 0) {
                    visualLimit = runs[i].limit;
                    for (j = visualStart; j < visualLimit; j++) {
                        indexMap[k++] = indexMap[j];
                    }
                    continue;
                }
                logicalStart = runs[i].start;
                evenRun = runs[i].isEvenRun();
                logicalEnd = logicalStart + length - 1;
                for (j = 0; j < length; j++) {
                    m = evenRun ? logicalStart + j : logicalEnd - j;
                    uchar = bidiBase.text[m];
                    if (!BidiBase.IsBidiControlChar(uchar)) {
                        indexMap[k++] = m;
                    }
                }
            }
        }
        if (allocLength == bidiBase.resultLength) {
            return indexMap;
        }
        int[] newMap = new int[bidiBase.resultLength];
        System.arraycopy(indexMap, 0, newMap, 0, bidiBase.resultLength);
        return newMap;
    }

}
