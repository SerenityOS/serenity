/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
 *
 * package private helper class which provides decoder (native->ucs)
 * mapping capability for the benefit of compound encoders/decoders
 * whose individual component submappings do not need an association with
 * an enclosing charset
 *
 */


public class DBCSDecoderMapping {

    /* 1rst level index */
    private short[] index1;
    /*
     * 2nd level index, provided by subclass
     * every string has 0x10*(end-start+1) characters.
     */
    private String[] index2;

    protected int start;
    protected int end;

    protected static final char REPLACE_CHAR='\uFFFD';

    public DBCSDecoderMapping(short[] index1, String[] index2,
                             int start, int end) {
        this.index1 = index1;
        this.index2 = index2;
        this.start = start;
        this.end = end;
    }

    /*
     * Can be changed by subclass
     */
    protected char decodeSingle(int b) {
        if (b >= 0)
            return (char) b;
        return REPLACE_CHAR;
    }

    protected char decodeDouble(int byte1, int byte2) {
        if (((byte1 < 0) || (byte1 > index1.length))
            || ((byte2 < start) || (byte2 > end)))
            return REPLACE_CHAR;

        int n = (index1[byte1] & 0xf) * (end - start + 1) + (byte2 - start);
        return index2[index1[byte1] >> 4].charAt(n);
    }
}
