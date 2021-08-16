/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;

import java.io.DataInputStream;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.Arrays;
import java.util.Locale;
import java.util.zip.InflaterInputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;

class CharacterName {

    private static SoftReference<CharacterName> refCharName;

    // codepoint -> bkIndex -> lookup -> offset/len
    private final byte[] strPool;
    private final int[] lookup;      // code point -> offset/len in strPool
    private final int[] bkIndices;   // code point -> lookup index

    // name -> hash -> hsIndices -> cpEntries -> code point
    private final int[] cpEntries;   // code points that have name in strPool
    private final int[] hsIndices;   // chain heads, hash indices into "cps"

    private CharacterName()  {
        try (@SuppressWarnings("removal") DataInputStream dis = new DataInputStream(new InflaterInputStream(
            AccessController.doPrivileged(new PrivilegedAction<>() {
                public InputStream run() {
                    return getClass().getResourceAsStream("uniName.dat");
                }
            })))) {

            int total = dis.readInt();
            int bkNum = dis.readInt();
            int cpNum = dis.readInt();
            int cpEnd = dis.readInt();
            byte ba[] = new byte[cpEnd];
            lookup = new int[bkNum * 256];
            bkIndices = new int[(Character.MAX_CODE_POINT + 1) >> 8];
            strPool = new byte[total - cpEnd];
            cpEntries = new int[cpNum * 3];
            hsIndices = new int[(cpNum / 2) | 1];
            Arrays.fill(bkIndices, -1);
            Arrays.fill(hsIndices, -1);
            dis.readFully(ba);
            dis.readFully(strPool);

            int nameOff = 0;
            int cpOff = 0;
            int cp = 0;
            int bk = -1;
            int prevBk = -1;   // prev bkNo;
            int idx = 0;
            int next = -1;
            int hash = 0;
            int hsh = 0;
            do {
                int len = ba[cpOff++] & 0xff;
                if (len == 0) {
                    len = ba[cpOff++] & 0xff;
                    // always big-endian
                    cp = ((ba[cpOff++] & 0xff) << 16) |
                         ((ba[cpOff++] & 0xff) <<  8) |
                         ((ba[cpOff++] & 0xff));
                }  else {
                    cp++;
                }
                // cp -> name
                int hi = cp >> 8;
                if (prevBk != hi) {
                    bk++;
                    bkIndices[hi] = bk;
                    prevBk = hi;
                }
                lookup[(bk << 8) + (cp & 0xff)] = (nameOff << 8) | len;
                // name -> cp
                hash = hashN(strPool, nameOff, len);
                hsh = (hash & 0x7fffffff) % hsIndices.length;
                next = hsIndices[hsh];
                hsIndices[hsh] = idx;
                idx = addCp(idx, hash, next, cp);
                nameOff += len;
            } while (cpOff < cpEnd);
        } catch (Exception x) {
            throw new InternalError(x.getMessage(), x);
        }
    }

    private static final int hashN(byte[] a, int off, int len) {
        int h = 1;
        while (len-- > 0) {
            h = 31 * h + a[off++];
        }
        return h;
    }

    private int addCp(int idx, int hash, int next, int cp) {
        cpEntries[idx++] = hash;
        cpEntries[idx++] = next;
        cpEntries[idx++] = cp;
        return idx;
    }

    private int getCpHash(int idx) { return cpEntries[idx]; }
    private int getCpNext(int idx) { return cpEntries[idx + 1]; }
    private int getCp(int idx)  { return cpEntries[idx + 2]; }

    public static CharacterName getInstance() {
        SoftReference<CharacterName> ref = refCharName;
        CharacterName cname = null;
        if (ref == null || (cname = ref.get()) == null) {
            cname = new CharacterName();
            refCharName = new SoftReference<>(cname);
        }
        return cname;
    }

    public String getName(int cp) {
        int off = 0;
        int bk = bkIndices[cp >> 8];
        if (bk == -1 || (off = lookup[(bk << 8) + (cp & 0xff)]) == 0)
            return null;
        @SuppressWarnings("deprecation")
        String result = new String(strPool, 0, off >>> 8, off & 0xff);  // ASCII
        return result;
    }

    public int getCodePoint(String name) {
        byte[] bname = name.getBytes(sun.nio.cs.ISO_8859_1.INSTANCE);
        int hsh = hashN(bname, 0, bname.length);
        int idx = hsIndices[(hsh & 0x7fffffff) % hsIndices.length];
        while (idx != -1) {
            if (getCpHash(idx) == hsh) {
                int cp = getCp(idx);
                int off = -1;
                int bk = bkIndices[cp >> 8];
                if (bk != -1 && (off = lookup[(bk << 8) + (cp & 0xff)]) != 0) {
                    int len = off & 0xff;
                    off = off >>> 8;
                    if (bname.length == len) {
                        int i = 0;
                        while (i < len && bname[i] == strPool[off++]) {
                            i++;
                        }
                        if (i == len) {
                            return cp;
                        }
                    }
                 }
            }
            idx = getCpNext(idx);
        }
        return -1;
    }
}
