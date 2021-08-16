/*
 * Copyright (c) 2009, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * (C) Copyright IBM Corp. and others, 1996-2009 - All Rights Reserved         *
 *                                                                             *
 * The original version of this source code and documentation is copyrighted   *
 * and owned by IBM, These materials are provided under terms of a License     *
 * Agreement between IBM and Sun. This technology is protected by multiple     *
 * US and International patents. This notice and attribution to IBM may not    *
 * to removed.                                                                 *
 *******************************************************************************
 */
/* Written by Simon Montagu, Matitiahu Allouche
 * (ported from C code written by Markus W. Scherer)
 */

package jdk.internal.icu.text;

/**
 * A BidiRun represents a sequence of characters at the same embedding level.
 * The Bidi algorithm decomposes a piece of text into sequences of characters
 * at the same embedding level, each such sequence is called a "run".
 *
 * <p>A BidiRun represents such a run by storing its essential properties,
 * but does not duplicate the characters which form the run.
 *
 * <p>The &quot;limit&quot; of the run is the position just after the
 * last character, i.e., one more than that position.
 *
 * <p>This class has no public constructor, and its members cannot be
 * modified by users.
 *
 * @see com.ibm.icu.text.Bidi
 */
class BidiRun {

    int start;              /* first logical position of the run */
    int limit;              /* last visual position of the run +1 */
    int insertRemove;       /* if >0, flags for inserting LRM/RLM before/after run,
                               if <0, count of bidi controls within run            */
    byte level;

    /*
     * Default constructor
     *
     * Note that members start and limit of a run instance have different
     * meanings depending whether the run is part of the runs array of a Bidi
     * object, or if it is a reference returned by getVisualRun() or
     * getLogicalRun().
     * For a member of the runs array of a Bidi object,
     *   - start is the first logical position of the run in the source text.
     *   - limit is one after the last visual position of the run.
     * For a reference returned by getLogicalRun() or getVisualRun(),
     *   - start is the first logical position of the run in the source text.
     *   - limit is one after the last logical position of the run.
     */
    BidiRun()
    {
        this(0, 0, (byte)0);
    }

    /*
     * Constructor
     */
    BidiRun(int start, int limit, byte embeddingLevel)
    {
        this.start = start;
        this.limit = limit;
        this.level = embeddingLevel;
    }

    /*
     * Copy the content of a BidiRun instance
     */
    void copyFrom(BidiRun run)
    {
        this.start = run.start;
        this.limit = run.limit;
        this.level = run.level;
        this.insertRemove = run.insertRemove;
    }

    /**
     * Get level of run
     */
    byte getEmbeddingLevel()
    {
        return level;
    }

    /**
     * Check if run level is even
     * @return true if the embedding level of this run is even, i.e. it is a
     *  left-to-right run.
     */
    boolean isEvenRun()
    {
        return (level & 1) == 0;
    }

}
