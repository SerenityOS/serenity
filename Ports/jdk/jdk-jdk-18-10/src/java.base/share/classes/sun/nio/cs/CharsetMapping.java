/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.cs;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.*;
import java.security.*;

public class CharsetMapping {
    public static final char UNMAPPABLE_DECODING = '\uFFFD';
    public static final int  UNMAPPABLE_ENCODING = 0xFFFD;

    char[] b2cSB;                //singlebyte b->c
    char[] b2cDB1;               //dobulebyte b->c /db1
    char[] b2cDB2;               //dobulebyte b->c /db2

    int    b2Min, b2Max;         //min/max(start/end) value of 2nd byte
    int    b1MinDB1, b1MaxDB1;   //min/Max(start/end) value of 1st byte/db1
    int    b1MinDB2, b1MaxDB2;   //min/Max(start/end) value of 1st byte/db2
    int    dbSegSize;

    char[] c2b;
    char[] c2bIndex;

    // Supplementary
    char[] b2cSupp;
    char[] c2bSupp;

    // Composite
    Entry[] b2cComp;
    Entry[] c2bComp;

    public char decodeSingle(int b) {
        return b2cSB[b];
    }

    public char decodeDouble(int b1, int b2) {
        if (b2 >= b2Min && b2 < b2Max) {
            b2 -= b2Min;
            if (b1 >= b1MinDB1 && b1 <= b1MaxDB1) {
                b1 -= b1MinDB1;
                return b2cDB1[b1 * dbSegSize + b2];
            }
            if (b1 >= b1MinDB2 && b1 <= b1MaxDB2) {
                b1 -= b1MinDB2;
                return b2cDB2[b1 * dbSegSize + b2];
            }
        }
        return UNMAPPABLE_DECODING;
    }

    // for jis0213 all supplementary characters are in 0x2xxxx range,
    // so only the xxxx part is now stored, should actually store the
    // codepoint value instead.
    public char[] decodeSurrogate(int db, char[] cc) {
        int end = b2cSupp.length / 2;
        int i = Arrays.binarySearch(b2cSupp, 0, end, (char)db);
        if (i >= 0) {
            Character.toChars(b2cSupp[end + i] + 0x20000, cc, 0);
            return cc;
        }
        return null;
    }

    public char[] decodeComposite(Entry comp, char[] cc) {
        int i = findBytes(b2cComp, comp);
        if (i >= 0) {
            cc[0] = (char)b2cComp[i].cp;
            cc[1] = (char)b2cComp[i].cp2;
            return cc;
        }
        return null;
    }

    public int encodeChar(char ch) {
        int index = c2bIndex[ch >> 8];
        if (index == 0xffff)
            return UNMAPPABLE_ENCODING;
        return c2b[index + (ch & 0xff)];
    }

    public int encodeSurrogate(char hi, char lo) {
        int cp = Character.toCodePoint(hi, lo);
        if (cp < 0x20000 || cp >= 0x30000)
            return UNMAPPABLE_ENCODING;
        int end = c2bSupp.length / 2;
        int i = Arrays.binarySearch(c2bSupp, 0, end, (char)cp);
        if (i >= 0)
            return c2bSupp[end + i];
        return UNMAPPABLE_ENCODING;
    }

    public boolean isCompositeBase(Entry comp) {
        if (comp.cp <= 0x31f7 && comp.cp >= 0xe6) {
            return (findCP(c2bComp, comp) >= 0);
        }
        return false;
    }

    public int encodeComposite(Entry comp) {
        int i = findComp(c2bComp, comp);
        if (i >= 0)
            return c2bComp[i].bs;
        return UNMAPPABLE_ENCODING;
    }

    // init the CharsetMapping object from the .dat binary file
    @SuppressWarnings("removal")
    public static CharsetMapping get(final InputStream is) {
        return AccessController.doPrivileged(new PrivilegedAction<>() {
            public CharsetMapping run() {
                return new CharsetMapping().load(is);
            }
        });
    }

    public static class Entry {
        public int bs;   //byte sequence reps
        public int cp;   //Unicode codepoint
        public int cp2;  //CC of composite
    }

    static Comparator<Entry> comparatorBytes =
        new Comparator<Entry>() {
            public int compare(Entry m1, Entry m2) {
                return m1.bs - m2.bs;
            }
            public boolean equals(Object obj) {
                return this == obj;
            }
    };

    static Comparator<Entry> comparatorCP =
        new Comparator<Entry>() {
            public int compare(Entry m1, Entry m2) {
                return m1.cp - m2.cp;
            }
            public boolean equals(Object obj) {
                return this == obj;
            }
    };

    static Comparator<Entry> comparatorComp =
        new Comparator<Entry>() {
            public int compare(Entry m1, Entry m2) {
                 int v = m1.cp - m2.cp;
                 if (v == 0)
                   v = m1.cp2 - m2.cp2;
                 return v;
            }
            public boolean equals(Object obj) {
                return this == obj;
            }
    };

    static int findBytes(Entry[] a, Entry k) {
        return Arrays.binarySearch(a, 0, a.length, k, comparatorBytes);
    }

    static int findCP(Entry[] a, Entry k) {
        return Arrays.binarySearch(a, 0, a.length, k, comparatorCP);
    }

    static int findComp(Entry[] a, Entry k) {
        return Arrays.binarySearch(a, 0, a.length, k, comparatorComp);
    }

    /*****************************************************************************/
    // tags of different charset mapping tables
    private static final int MAP_SINGLEBYTE      = 0x1; // 0..256  : c
    private static final int MAP_DOUBLEBYTE1     = 0x2; // min..max: c
    private static final int MAP_DOUBLEBYTE2     = 0x3; // min..max: c [DB2]
    private static final int MAP_SUPPLEMENT      = 0x5; //           db,c
    private static final int MAP_SUPPLEMENT_C2B  = 0x6; //           c,db
    private static final int MAP_COMPOSITE       = 0x7; //           db,base,cc
    private static final int MAP_INDEXC2B        = 0x8; // index table of c->bb

    private static final boolean readNBytes(InputStream in, byte[] bb, int N)
        throws IOException
    {
        int off = 0;
        while (N > 0) {
            int n = in.read(bb, off, N);
            if (n == -1)
                return false;
            N = N - n;
            off += n;
        }
        return true;
    }

    int off = 0;
    byte[] bb;
    private char[] readCharArray() {
        // first 2 bytes are the number of "chars" stored in this table
        int size  = ((bb[off++]&0xff)<<8) | (bb[off++]&0xff);
        char [] cc = new char[size];
        for (int i = 0; i < size; i++) {
            cc[i] = (char)(((bb[off++]&0xff)<<8) | (bb[off++]&0xff));
        }
        return cc;
    }

    void readSINGLEBYTE() {
        char[] map = readCharArray();
        for (int i = 0; i < map.length; i++) {
            char c = map[i];
            if (c != UNMAPPABLE_DECODING) {
                c2b[c2bIndex[c >> 8] + (c&0xff)] = (char)i;
            }
        }
        b2cSB = map;
    }

    void readINDEXC2B() {
        char[] map = readCharArray();
        for (int i = map.length - 1; i >= 0; i--) {
            if (c2b == null && map[i] != -1) {
                c2b = new char[map[i] + 256];
                Arrays.fill(c2b, (char)UNMAPPABLE_ENCODING);
                break;
            }
        }
        c2bIndex = map;
    }

    char[] readDB(int b1Min, int b2Min, int segSize) {
        char[] map = readCharArray();
        for (int i = 0; i < map.length; i++) {
            char c = map[i];
            if (c != UNMAPPABLE_DECODING) {
                int b1 = i / segSize;
                int b2 = i % segSize;
                int b = (b1 + b1Min)* 256 + (b2 + b2Min);
                //System.out.printf("    DB %x\t%x%n", b, c & 0xffff);
                c2b[c2bIndex[c >> 8] + (c&0xff)] = (char)(b);
            }
        }
        return map;
    }

    void readDOUBLEBYTE1() {
        b1MinDB1 = ((bb[off++]&0xff)<<8) | (bb[off++]&0xff);
        b1MaxDB1 = ((bb[off++]&0xff)<<8) | (bb[off++]&0xff);
        b2Min =    ((bb[off++]&0xff)<<8) | (bb[off++]&0xff);
        b2Max =    ((bb[off++]&0xff)<<8) | (bb[off++]&0xff);
        dbSegSize = b2Max - b2Min + 1;
        b2cDB1 = readDB(b1MinDB1, b2Min, dbSegSize);
    }

    void readDOUBLEBYTE2() {
        b1MinDB2 = ((bb[off++]&0xff)<<8) | (bb[off++]&0xff);
        b1MaxDB2 = ((bb[off++]&0xff)<<8) | (bb[off++]&0xff);
        b2Min =    ((bb[off++]&0xff)<<8) | (bb[off++]&0xff);
        b2Max =    ((bb[off++]&0xff)<<8) | (bb[off++]&0xff);
        dbSegSize = b2Max - b2Min + 1;
        b2cDB2 = readDB(b1MinDB2, b2Min, dbSegSize);
    }

    void readCOMPOSITE() {
        char[] map = readCharArray();
        int mLen = map.length/3;
        b2cComp = new Entry[mLen];
        c2bComp = new Entry[mLen];
        for (int i = 0, j= 0; i < mLen; i++) {
            Entry m = new Entry();
            m.bs = map[j++];
            m.cp = map[j++];
            m.cp2 = map[j++];
            b2cComp[i] = m;
            c2bComp[i] = m;
        }
        Arrays.sort(c2bComp, 0, c2bComp.length, comparatorComp);
    }

    CharsetMapping load(InputStream in) {
        try {
            // The first 4 bytes are the size of the total data followed in
            // this .dat file.
            int len = ((in.read()&0xff) << 24) | ((in.read()&0xff) << 16) |
                      ((in.read()&0xff) << 8) | (in.read()&0xff);
            bb = new byte[len];
            off = 0;
            //System.out.printf("In : Total=%d%n", len);
            // Read in all bytes
            if (!readNBytes(in, bb, len))
                throw new RuntimeException("Corrupted data file");
            in.close();

            while (off < len) {
                int type = ((bb[off++]&0xff)<<8) | (bb[off++]&0xff);
                switch(type) {
                case MAP_INDEXC2B:
                    readINDEXC2B();
                    break;
                case MAP_SINGLEBYTE:
                    readSINGLEBYTE();
                    break;
                case MAP_DOUBLEBYTE1:
                    readDOUBLEBYTE1();
                    break;
                case MAP_DOUBLEBYTE2:
                    readDOUBLEBYTE2();
                    break;
                case MAP_SUPPLEMENT:
                    b2cSupp = readCharArray();
                    break;
                case MAP_SUPPLEMENT_C2B:
                    c2bSupp = readCharArray();
                    break;
                case MAP_COMPOSITE:
                    readCOMPOSITE();
                    break;
                default:
                    throw new RuntimeException("Corrupted data file");
                }
            }
            bb = null;
            return this;
        } catch (IOException x) {
            x.printStackTrace();
            return null;
        }
    }
}
