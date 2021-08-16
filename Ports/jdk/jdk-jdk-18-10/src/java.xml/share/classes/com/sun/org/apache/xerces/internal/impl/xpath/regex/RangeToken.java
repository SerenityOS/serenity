/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xerces.internal.impl.xpath.regex;

/**
 * This class represents a character class such as [a-z] or a period.
 *
 * @xerces.internal
 *
 */
final class RangeToken extends Token implements java.io.Serializable {

    private static final long serialVersionUID = 3257568399592010545L;

    int[] ranges;
    boolean sorted;
    boolean compacted;
    RangeToken icaseCache = null;
    int[] map = null;
    int nonMapIndex;

    RangeToken(int type) {
        super(type);
        this.setSorted(false);
    }

    // for RANGE or NRANGE
    protected void addRange(int start, int end) {
        this.icaseCache = null;
        //System.err.println("Token#addRange(): "+start+" "+end);
        int r1, r2;
        if (start <= end) {
            r1 = start;
            r2 = end;
        } else {
            r1 = end;
            r2 = start;
        }

        int pos = 0;
        if (this.ranges == null) {
            this.ranges = new int[2];
            this.ranges[0] = r1;
            this.ranges[1] = r2;
            this.setSorted(true);
        } else {
            pos = this.ranges.length;
            if (this.ranges[pos-1]+1 == r1) {
                this.ranges[pos-1] = r2;
                return;
            }
            int[] temp = new int[pos+2];
            System.arraycopy(this.ranges, 0, temp, 0, pos);
            this.ranges = temp;
            if (this.ranges[pos-1] >= r1)
                this.setSorted(false);
            this.ranges[pos++] = r1;
            this.ranges[pos] = r2;
            if (!this.sorted)
                this.sortRanges();
        }
    }

    private final boolean isSorted() {
        return this.sorted;
    }
    private final void setSorted(boolean sort) {
        this.sorted = sort;
        if (!sort)  this.compacted = false;
    }
    private final boolean isCompacted() {
        return this.compacted;
    }
    private final void setCompacted() {
        this.compacted = true;
    }

    protected void sortRanges() {
        if (this.isSorted())
            return;
        if (this.ranges == null)
            return;
        //System.err.println("Do sorting: "+this.ranges.length);

                                                // Bubble sort
                                                // Why? -- In many cases,
                                                //         this.ranges has few elements.
        for (int i = this.ranges.length-4;  i >= 0;  i -= 2) {
            for (int j = 0;  j <= i;  j += 2) {
                if (this.ranges[j] > this.ranges[j+2]
                    || this.ranges[j] == this.ranges[j+2] && this.ranges[j+1] > this.ranges[j+3]) {
                    int tmp;
                    tmp = this.ranges[j+2];
                    this.ranges[j+2] = this.ranges[j];
                    this.ranges[j] = tmp;
                    tmp = this.ranges[j+3];
                    this.ranges[j+3] = this.ranges[j+1];
                    this.ranges[j+1] = tmp;
                }
            }
        }
        this.setSorted(true);
    }

    /**
     * this.ranges is sorted.
     */
    protected void compactRanges() {
        boolean DEBUG = false;
        if (this.ranges == null || this.ranges.length <= 2)
            return;
        if (this.isCompacted())
            return;
        int base = 0;                           // Index of writing point
        int target = 0;                         // Index of processing point

        while (target < this.ranges.length) {
            if (base != target) {
                this.ranges[base] = this.ranges[target++];
                this.ranges[base+1] = this.ranges[target++];
            } else
                target += 2;
            int baseend = this.ranges[base+1];
            while (target < this.ranges.length) {
                if (baseend+1 < this.ranges[target])
                    break;
                if (baseend+1 == this.ranges[target]) {
                    if (DEBUG)
                        System.err.println("Token#compactRanges(): Compaction: ["+this.ranges[base]
                                           +", "+this.ranges[base+1]
                                           +"], ["+this.ranges[target]
                                           +", "+this.ranges[target+1]
                                           +"] -> ["+this.ranges[base]
                                           +", "+this.ranges[target+1]
                                           +"]");
                    this.ranges[base+1] = this.ranges[target+1];
                    baseend = this.ranges[base+1];
                    target += 2;
                } else if (baseend >= this.ranges[target+1]) {
                    if (DEBUG)
                        System.err.println("Token#compactRanges(): Compaction: ["+this.ranges[base]
                                           +", "+this.ranges[base+1]
                                           +"], ["+this.ranges[target]
                                           +", "+this.ranges[target+1]
                                           +"] -> ["+this.ranges[base]
                                           +", "+this.ranges[base+1]
                                           +"]");
                    target += 2;
                } else if (baseend < this.ranges[target+1]) {
                    if (DEBUG)
                        System.err.println("Token#compactRanges(): Compaction: ["+this.ranges[base]
                                           +", "+this.ranges[base+1]
                                           +"], ["+this.ranges[target]
                                           +", "+this.ranges[target+1]
                                           +"] -> ["+this.ranges[base]
                                           +", "+this.ranges[target+1]
                                           +"]");
                    this.ranges[base+1] = this.ranges[target+1];
                    baseend = this.ranges[base+1];
                    target += 2;
                } else {
                    throw new RuntimeException("Token#compactRanges(): Internel Error: ["
                                               +this.ranges[base]
                                               +","+this.ranges[base+1]
                                               +"] ["+this.ranges[target]
                                               +","+this.ranges[target+1]+"]");
                }
            } // while
            base += 2;
        }

        if (base != this.ranges.length) {
            int[] result = new int[base];
            System.arraycopy(this.ranges, 0, result, 0, base);
            this.ranges = result;
        }
        this.setCompacted();
    }

    protected void mergeRanges(Token token) {
        RangeToken tok = (RangeToken)token;
        this.sortRanges();
        tok.sortRanges();
        if (tok.ranges == null)
            return;
        this.icaseCache = null;
        this.setSorted(true);
        if (this.ranges == null) {
            this.ranges = new int[tok.ranges.length];
            System.arraycopy(tok.ranges, 0, this.ranges, 0, tok.ranges.length);
            return;
        }
        int[] result = new int[this.ranges.length+tok.ranges.length];
        for (int i = 0, j = 0, k = 0;  i < this.ranges.length || j < tok.ranges.length;) {
            if (i >= this.ranges.length) {
                result[k++] = tok.ranges[j++];
                result[k++] = tok.ranges[j++];
            } else if (j >= tok.ranges.length) {
                result[k++] = this.ranges[i++];
                result[k++] = this.ranges[i++];
            } else if (tok.ranges[j] < this.ranges[i]
                       || tok.ranges[j] == this.ranges[i] && tok.ranges[j+1] < this.ranges[i+1]) {
                result[k++] = tok.ranges[j++];
                result[k++] = tok.ranges[j++];
            } else {
                result[k++] = this.ranges[i++];
                result[k++] = this.ranges[i++];
            }
        }
        this.ranges = result;
    }

    protected void subtractRanges(Token token) {
        if (token.type == NRANGE) {
            this.intersectRanges(token);
            return;
        }
        RangeToken tok = (RangeToken)token;
        if (tok.ranges == null || this.ranges == null)
            return;
        this.icaseCache = null;
        this.sortRanges();
        this.compactRanges();
        tok.sortRanges();
        tok.compactRanges();

        //System.err.println("Token#substractRanges(): Entry: "+this.ranges.length+", "+tok.ranges.length);

        int[] result = new int[this.ranges.length+tok.ranges.length];
        int wp = 0, src = 0, sub = 0;
        while (src < this.ranges.length && sub < tok.ranges.length) {
            int srcbegin = this.ranges[src];
            int srcend = this.ranges[src+1];
            int subbegin = tok.ranges[sub];
            int subend = tok.ranges[sub+1];
            if (srcend < subbegin) {            // Not overlapped
                                                // src: o-----o
                                                // sub:         o-----o
                                                // res: o-----o
                                                // Reuse sub
                result[wp++] = this.ranges[src++];
                result[wp++] = this.ranges[src++];
            } else if (srcend >= subbegin
                       && srcbegin <= subend) { // Overlapped
                                                // src:    o--------o
                                                // sub:  o----o
                                                // sub:      o----o
                                                // sub:          o----o
                                                // sub:  o------------o
                if (subbegin <= srcbegin && srcend <= subend) {
                                                // src:    o--------o
                                                // sub:  o------------o
                                                // res: empty
                                                // Reuse sub
                    src += 2;
                } else if (subbegin <= srcbegin) {
                                                // src:    o--------o
                                                // sub:  o----o
                                                // res:       o-----o
                                                // Reuse src(=res)
                    this.ranges[src] = subend+1;
                    sub += 2;
                } else if (srcend <= subend) {
                                                // src:    o--------o
                                                // sub:          o----o
                                                // res:    o-----o
                                                // Reuse sub
                    result[wp++] = srcbegin;
                    result[wp++] = subbegin-1;
                    src += 2;
                } else {
                                                // src:    o--------o
                                                // sub:      o----o
                                                // res:    o-o    o-o
                                                // Reuse src(=right res)
                    result[wp++] = srcbegin;
                    result[wp++] = subbegin-1;
                    this.ranges[src] = subend+1;
                    sub += 2;
                }
            } else if (subend < srcbegin) {
                                                // Not overlapped
                                                // src:          o-----o
                                                // sub: o----o
                sub += 2;
            } else {
                throw new RuntimeException("Token#subtractRanges(): Internal Error: ["+this.ranges[src]
                                           +","+this.ranges[src+1]
                                           +"] - ["+tok.ranges[sub]
                                           +","+tok.ranges[sub+1]
                                           +"]");
            }
        }
        while (src < this.ranges.length) {
            result[wp++] = this.ranges[src++];
            result[wp++] = this.ranges[src++];
        }
        this.ranges = new int[wp];
        System.arraycopy(result, 0, this.ranges, 0, wp);
                                                // this.ranges is sorted and compacted.
    }

    /**
     * @param tok Ignore whether it is NRANGE or not.
     */
    protected void intersectRanges(Token token) {
        RangeToken tok = (RangeToken)token;
        if (tok.ranges == null || this.ranges == null)
            return;
        this.icaseCache = null;
        this.sortRanges();
        this.compactRanges();
        tok.sortRanges();
        tok.compactRanges();

        int[] result = new int[this.ranges.length+tok.ranges.length];
        int wp = 0, src1 = 0, src2 = 0;
        while (src1 < this.ranges.length && src2 < tok.ranges.length) {
            int src1begin = this.ranges[src1];
            int src1end = this.ranges[src1+1];
            int src2begin = tok.ranges[src2];
            int src2end = tok.ranges[src2+1];
            if (src1end < src2begin) {          // Not overlapped
                                                // src1: o-----o
                                                // src2:         o-----o
                                                // res:  empty
                                                // Reuse src2
                src1 += 2;
            } else if (src1end >= src2begin
                       && src1begin <= src2end) { // Overlapped
                                                // src1:    o--------o
                                                // src2:  o----o
                                                // src2:      o----o
                                                // src2:          o----o
                                                // src2:  o------------o
                if (src2begin <= src1begin && src1end <= src2end) {
                                                // src1:    o--------o
                                                // src2:  o------------o
                                                // res:     o--------o
                                                // Reuse src2
                    result[wp++] = src1begin;
                    result[wp++] = src1end;
                    src1 += 2;
                } else if (src2begin <= src1begin) {
                                                // src1:    o--------o
                                                // src2:  o----o
                                                // res:     o--o
                                                // Reuse the rest of src1
                    result[wp++] = src1begin;
                    result[wp++] = src2end;
                    this.ranges[src1] = src2end+1;
                    src2 += 2;
                } else if (src1end <= src2end) {
                                                // src1:    o--------o
                                                // src2:          o----o
                                                // res:           o--o
                                                // Reuse src2
                    result[wp++] = src2begin;
                    result[wp++] = src1end;
                    src1 += 2;
                } else {
                                                // src1:    o--------o
                                                // src2:      o----o
                                                // res:       o----o
                                                // Reuse the rest of src1
                    result[wp++] = src2begin;
                    result[wp++] = src2end;
                    this.ranges[src1] = src2end+1;
                    src2 += 2;
                }
            } else if (src2end < src1begin) {
                                                // Not overlapped
                                                // src1:          o-----o
                                                // src2: o----o
                src2 += 2;
            } else {
                throw new RuntimeException("Token#intersectRanges(): Internal Error: ["
                                           +this.ranges[src1]
                                           +","+this.ranges[src1+1]
                                           +"] & ["+tok.ranges[src2]
                                           +","+tok.ranges[src2+1]
                                           +"]");
            }
        }
        this.ranges = new int[wp];
        System.arraycopy(result, 0, this.ranges, 0, wp);
                                                // this.ranges is sorted and compacted.
    }

    /**
     * for RANGE: Creates complement.
     * for NRANGE: Creates the same meaning RANGE.
     */
    static Token complementRanges(Token token) {
        if (token.type != RANGE && token.type != NRANGE)
            throw new IllegalArgumentException("Token#complementRanges(): must be RANGE: "+token.type);
        RangeToken tok = (RangeToken)token;
        tok.sortRanges();
        tok.compactRanges();
        int len = tok.ranges.length+2;
        if (tok.ranges[0] == 0)
            len -= 2;
        int last = tok.ranges[tok.ranges.length-1];
        if (last == UTF16_MAX)
            len -= 2;
        RangeToken ret = Token.createRange();
        ret.ranges = new int[len];
        int wp = 0;
        if (tok.ranges[0] > 0) {
            ret.ranges[wp++] = 0;
            ret.ranges[wp++] = tok.ranges[0]-1;
        }
        for (int i = 1;  i < tok.ranges.length-2;  i += 2) {
            ret.ranges[wp++] = tok.ranges[i]+1;
            ret.ranges[wp++] = tok.ranges[i+1]-1;
        }
        if (last != UTF16_MAX) {
            ret.ranges[wp++] = last+1;
            ret.ranges[wp] = UTF16_MAX;
        }
        ret.setCompacted();
        return ret;
    }

    synchronized RangeToken getCaseInsensitiveToken() {
        if (this.icaseCache != null)
            return this.icaseCache;

        RangeToken uppers = this.type == Token.RANGE ? Token.createRange() : Token.createNRange();
        for (int i = 0;  i < this.ranges.length;  i += 2) {
            for (int ch = this.ranges[i];  ch <= this.ranges[i+1];  ch ++) {
                if (ch > 0xffff)
                    uppers.addRange(ch, ch);
                else {
                    char uch = Character.toUpperCase((char)ch);
                    uppers.addRange(uch, uch);
                }
            }
        }
        RangeToken lowers = this.type == Token.RANGE ? Token.createRange() : Token.createNRange();
        for (int i = 0;  i < uppers.ranges.length;  i += 2) {
            for (int ch = uppers.ranges[i];  ch <= uppers.ranges[i+1];  ch ++) {
                if (ch > 0xffff)
                    lowers.addRange(ch, ch);
                else {
                    char lch = Character.toLowerCase((char)ch);
                    lowers.addRange(lch, lch);
                }
            }
        }
        lowers.mergeRanges(uppers);
        lowers.mergeRanges(this);
        lowers.compactRanges();

        this.icaseCache = lowers;
        return lowers;
    }

    void dumpRanges() {
        System.err.print("RANGE: ");
        if (this.ranges == null) {
            System.err.println(" NULL");
            return;
        }
        for (int i = 0;  i < this.ranges.length;  i += 2) {
            System.err.print("["+this.ranges[i]+","+this.ranges[i+1]+"] ");
        }
        System.err.println("");
    }

    boolean match(int ch) {
        if (this.map == null)  this.createMap();
        boolean ret;
        if (this.type == RANGE) {
            if (ch < MAPSIZE)
                return (this.map[ch/32] & (1<<(ch&0x1f))) != 0;
            ret = false;
            for (int i = this.nonMapIndex;  i < this.ranges.length;  i += 2) {
                if (this.ranges[i] <= ch && ch <= this.ranges[i+1])
                    return true;
            }
        } else {
            if (ch < MAPSIZE)
                return (this.map[ch/32] & (1<<(ch&0x1f))) == 0;
            ret = true;
            for (int i = this.nonMapIndex;  i < this.ranges.length;  i += 2) {
                if (this.ranges[i] <= ch && ch <= this.ranges[i+1])
                    return false;
            }
        }
        return ret;
    }

    private static final int MAPSIZE = 256;
    private void createMap() {
        int asize = MAPSIZE/32;                 // 32 is the number of bits in `int'.
        int [] map = new int[asize];
        int nonMapIndex = this.ranges.length;
        for (int i = 0; i < asize; ++i) {
            map[i] = 0;
        }
        for (int i = 0; i < this.ranges.length;  i += 2) {
            int s = this.ranges[i];
            int e = this.ranges[i+1];
            if (s < MAPSIZE) {
                for (int j = s; j <= e && j < MAPSIZE; j++) {
                    map[j/32] |= 1<<(j&0x1f); // s&0x1f : 0-31
                }
            }
            else {
                nonMapIndex = i;
                break;
            }
            if (e >= MAPSIZE) {
                nonMapIndex = i;
                break;
            }
        }
        this.map = map;
        this.nonMapIndex = nonMapIndex;
        //for (int i = 0;  i < asize;  i ++)  System.err.println("Map: "+Integer.toString(this.map[i], 16));
    }

    public String toString(int options) {
        String ret;
        if (this.type == RANGE) {
            if (this == Token.token_dot)
                ret = ".";
            else if (this == Token.token_0to9)
                ret = "\\d";
            else if (this == Token.token_wordchars)
                ret = "\\w";
            else if (this == Token.token_spaces)
                ret = "\\s";
            else {
                StringBuilder sb = new StringBuilder();
                sb.append('[');
                for (int i = 0;  i < this.ranges.length;  i += 2) {
                    if ((options & RegularExpression.SPECIAL_COMMA) != 0 && i > 0)  sb.append(',');
                    if (this.ranges[i] == this.ranges[i+1]) {
                        sb.append(escapeCharInCharClass(this.ranges[i]));
                    } else {
                        sb.append(escapeCharInCharClass(this.ranges[i]));
                        sb.append('-');
                        sb.append(escapeCharInCharClass(this.ranges[i+1]));
                    }
                }
                sb.append(']');
                ret = sb.toString();
            }
        } else {
            if (this == Token.token_not_0to9)
                ret = "\\D";
            else if (this == Token.token_not_wordchars)
                ret = "\\W";
            else if (this == Token.token_not_spaces)
                ret = "\\S";
            else {
                StringBuffer sb = new StringBuffer();
                sb.append("[^");
                for (int i = 0;  i < this.ranges.length;  i += 2) {
                    if ((options & RegularExpression.SPECIAL_COMMA) != 0 && i > 0)  sb.append(',');
                    if (this.ranges[i] == this.ranges[i+1]) {
                        sb.append(escapeCharInCharClass(this.ranges[i]));
                    } else {
                        sb.append(escapeCharInCharClass(this.ranges[i]));
                        sb.append('-');
                        sb.append(escapeCharInCharClass(this.ranges[i+1]));
                    }
                }
                sb.append(']');
                ret = sb.toString();
            }
        }
        return ret;
    }

    private static String escapeCharInCharClass(int ch) {
        String ret;
        switch (ch) {
          case '[':  case ']':  case '-':  case '^':
          case ',':  case '\\':
            ret = "\\"+(char)ch;
            break;
          case '\f':  ret = "\\f";  break;
          case '\n':  ret = "\\n";  break;
          case '\r':  ret = "\\r";  break;
          case '\t':  ret = "\\t";  break;
          case 0x1b:  ret = "\\e";  break;
          //case 0x0b:  ret = "\\v";  break;
          default:
            if (ch < 0x20) {
                String pre = "0"+Integer.toHexString(ch);
                ret = "\\x"+pre.substring(pre.length()-2, pre.length());
            } else if (ch >= 0x10000) {
                String pre = "0"+Integer.toHexString(ch);
                ret = "\\v"+pre.substring(pre.length()-6, pre.length());
            } else
                ret = ""+(char)ch;
        }
        return ret;
    }

}
