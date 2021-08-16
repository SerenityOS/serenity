/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package java.util.regex;

import java.util.Arrays;

/**
 * A lightweight hashset implementation for positive 'int'. Not safe for
 * concurrent access.
 */
class IntHashSet {
    private int[] entries;
    private int[] hashes;
    private int pos = 0;

    public IntHashSet() {
        this.entries = new int[16 << 1];      // initCapacity = 16;
        this.hashes = new int[(16 / 2) | 1];  // odd -> fewer collisions
        Arrays.fill(this.entries, -1);
        Arrays.fill(this.hashes, -1);
    }

    public boolean contains(int i) {
        int h = hashes[i % hashes.length];
        while (h != -1) {
            if (entries[h] == i)
                return true;
            h = entries[h + 1];
        }
        return false;
    }

    public void add(int i) {
        int h0 = i % hashes.length;
        int next = hashes[h0];
        //  if invoker guarantees contains(i) checked before add(i)
        //  the following check is not needed.
        int next0 = next;
        while (next0 != -1) {
            if (entries[next0 ] == i)
                return;
            next0 = entries[next0 + 1];
        }
        hashes[h0] = pos;
        entries[pos++] = i;
        entries[pos++] = next;
        if (pos == entries.length)
            expand();
    }

    public void clear() {
        Arrays.fill(this.entries, -1);
        Arrays.fill(this.hashes, -1);
        pos = 0;
    }

    private void expand() {
        int[] old = entries;
        int[] es = new int[old.length << 1];
        int hlen = (old.length / 2) | 1;
        int[] hs = new int[hlen];
        Arrays.fill(es, -1);
        Arrays.fill(hs, -1);
        for (int n = 0; n < pos;) {  // re-hashing
            int i = old[n];
            int hsh = i % hlen;
            int next = hs[hsh];
            hs[hsh] = n;
            es[n++] = i;
            es[n++] = next;
        }
        this.entries = es;
        this.hashes = hs;
    }
}
