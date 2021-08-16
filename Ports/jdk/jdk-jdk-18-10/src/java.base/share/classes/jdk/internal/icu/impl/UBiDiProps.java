/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 *   Copyright (C) 2004-2014, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *
 *******************************************************************************
 *   file name:  UBiDiProps.java
 *   encoding:   US-ASCII
 *   tab size:   8 (not used)
 *   indentation:4
 *
 *   created on: 2005jan16
 *   created by: Markus W. Scherer
 *
 *   Low-level Unicode bidi/shaping properties access.
 *   Java port of ubidi_props.h/.c.
 */

package jdk.internal.icu.impl;

import jdk.internal.icu.lang.UCharacter;
import jdk.internal.icu.util.VersionInfo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.MissingResourceException;

public final class UBiDiProps {
    // constructors etc. --------------------------------------------------- ***

    // port of ubidi_openProps()
    private UBiDiProps() throws IOException{
        ByteBuffer bytes=ICUBinary.getRequiredData(DATA_FILE_NAME);
        readData(bytes);
    }

    private void readData(ByteBuffer bytes) throws IOException {
        // read the header
        ICUBinary.readHeader(bytes, FMT, new IsAcceptable());

        // read indexes[]
        int i, count;
        count=bytes.getInt();
        if(count<IX_TOP) {
            throw new IOException("indexes[0] too small in "+DATA_FILE_NAME);
        }
        indexes=new int[count];

        indexes[0]=count;
        for(i=1; i<count; ++i) {
            indexes[i]=bytes.getInt();
        }

        // read the trie
        trie=Trie2_16.createFromSerialized(bytes);
        int expectedTrieLength=indexes[IX_TRIE_SIZE];
        int trieLength=trie.getSerializedLength();
        if(trieLength>expectedTrieLength) {
            throw new IOException(DATA_FILE_NAME+": not enough bytes for the trie");
        }
        // skip padding after trie bytes
        ICUBinary.skipBytes(bytes, expectedTrieLength-trieLength);

        // read mirrors[]
        count=indexes[IX_MIRROR_LENGTH];
        if(count>0) {
            mirrors=new int[count];
            for(i=0; i<count; ++i) {
                mirrors[i]=bytes.getInt();
            }
        }

        // read jgArray[]
        count=indexes[IX_JG_LIMIT]-indexes[IX_JG_START];
        jgArray=new byte[count];
        for(i=0; i<count; ++i) {
            jgArray[i]=bytes.get();
        }

        // read jgArray2[]
        count=indexes[IX_JG_LIMIT2]-indexes[IX_JG_START2];
        jgArray2=new byte[count];
        for(i=0; i<count; ++i) {
            jgArray2[i]=bytes.get();
        }
    }

    // implement ICUBinary.Authenticate
    private static final class IsAcceptable implements ICUBinary.Authenticate {
        public boolean isDataVersionAcceptable(byte version[]) {
            return version[0]==2;
        }
    }

    // property access functions ------------------------------------------- ***

    public final int getClass(int c) {
        return getClassFromProps(trie.get(c));
    }

    private final int getMirror(int c, int props) {
        int delta=getMirrorDeltaFromProps(props);
        if(delta!=ESC_MIRROR_DELTA) {
            return c+delta;
        } else {
            /* look for mirror code point in the mirrors[] table */
            int m;
            int i, length;
            int c2;

            length=indexes[IX_MIRROR_LENGTH];

            /* linear search */
            for(i=0; i<length; ++i) {
                m=mirrors[i];
                c2=getMirrorCodePoint(m);
                if(c==c2) {
                    /* found c, return its mirror code point using the index in m */
                    return getMirrorCodePoint(mirrors[getMirrorIndex(m)]);
                } else if(c<c2) {
                    break;
                }
            }

            /* c not found, return it itself */
            return c;
        }
    }

    public final int getMirror(int c) {
        int props=trie.get(c);
        return getMirror(c, props);
    }

    public final int getJoiningType(int c) {
        return (trie.get(c)&JT_MASK)>>JT_SHIFT;
    }

    public final int getJoiningGroup(int c) {
        int start, limit;

        start=indexes[IX_JG_START];
        limit=indexes[IX_JG_LIMIT];
        if(start<=c && c<limit) {
            return (int)jgArray[c-start]&0xff;
        }
        start=indexes[IX_JG_START2];
        limit=indexes[IX_JG_LIMIT2];
        if(start<=c && c<limit) {
            return (int)jgArray2[c-start]&0xff;
        }
        return UCharacter.JoiningGroup.NO_JOINING_GROUP;
    }

    public final int getPairedBracketType(int c) {
        return (trie.get(c)&BPT_MASK)>>BPT_SHIFT;
    }

    public final int getPairedBracket(int c) {
        int props=trie.get(c);
        if((props&BPT_MASK)==0) {
            return c;
        } else {
            return getMirror(c, props);
        }
    }

    // data members -------------------------------------------------------- ***
    private int indexes[];
    private int mirrors[];
    private byte jgArray[];
    private byte jgArray2[];

    private Trie2_16 trie;

    // data format constants ----------------------------------------------- ***
    @SuppressWarnings("deprecation")
    private static final String DATA_FILE_NAME =
            "/jdk/internal/icu/impl/data/icudt" +
            VersionInfo.ICU_DATA_VERSION_PATH +
            "/ubidi.icu";

    /* format "BiDi" */
    private static final int FMT=0x42694469;

    /* indexes into indexes[] */
    private static final int IX_TRIE_SIZE=2;
    private static final int IX_MIRROR_LENGTH=3;

    private static final int IX_JG_START=4;
    private static final int IX_JG_LIMIT=5;
    private static final int IX_JG_START2=6;  /* new in format version 2.2, ICU 54 */
    private static final int IX_JG_LIMIT2=7;

    private static final int IX_TOP=16;

    // definitions for 16-bit bidi/shaping properties word ----------------- ***

                          /* CLASS_SHIFT=0, */     /* bidi class: 5 bits (4..0) */
    private static final int JT_SHIFT=5;           /* joining type: 3 bits (7..5) */

    private static final int BPT_SHIFT=8;          /* Bidi_Paired_Bracket_Type(bpt): 2 bits (9..8) */

    private static final int MIRROR_DELTA_SHIFT=13;        /* bidi mirroring delta: 3 bits (15..13) */

    private static final int CLASS_MASK=    0x0000001f;
    private static final int JT_MASK=       0x000000e0;
    private static final int BPT_MASK=      0x00000300;

    private static final int getClassFromProps(int props) {
        return props&CLASS_MASK;
    }
    private static final boolean getFlagFromProps(int props, int shift) {
        return ((props>>shift)&1)!=0;
    }
    private static final int getMirrorDeltaFromProps(int props) {
        return (short)props>>MIRROR_DELTA_SHIFT;
    }

    private static final int ESC_MIRROR_DELTA=-4;

    // definitions for 32-bit mirror table entry --------------------------- ***

    /* the source Unicode code point takes 21 bits (20..0) */
    private static final int MIRROR_INDEX_SHIFT=21;

    private static final int getMirrorCodePoint(int m) {
        return m&0x1fffff;
    }
    private static final int getMirrorIndex(int m) {
        return m>>>MIRROR_INDEX_SHIFT;
    }


    /*
     * public singleton instance
     */
    public static final UBiDiProps INSTANCE;

    // This static initializer block must be placed after
    // other static member initialization
    static {
        try {
            INSTANCE = new UBiDiProps();
        } catch (IOException e) {
            throw new MissingResourceException(e.getMessage(),DATA_FILE_NAME,"");
        }
    }
}
