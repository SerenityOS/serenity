/*
 * Copyright (c) 1999, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.loops;

public final class RenderCache {
    final class Entry {
        private SurfaceType src;
        private CompositeType comp;
        private SurfaceType dst;
        private Object value;

        public Entry(SurfaceType src,
                     CompositeType comp,
                     SurfaceType dst,
                     Object value)
        {
            this.src = src;
            this.comp = comp;
            this.dst = dst;
            this.value = value;
        }

        public boolean matches(SurfaceType src,
                               CompositeType comp,
                               SurfaceType dst)
        {
            // bug 4725045: using equals() causes different SurfaceType
            // objects with the same strings to match in the cache, which is
            // not the behavior we want.  Constrain the match to succeed only
            // on object matches instead.
            return ((this.src == src) &&
                    (this.comp == comp) &&
                    (this.dst == dst));
        }

        public Object getValue() {
            return value;
        }
    }

    private Entry[] entries;

    public RenderCache(int size) {
        entries = new Entry[size];
    }

    public synchronized Object get(SurfaceType src,
                      CompositeType comp,
                      SurfaceType dst)
    {
        int max = entries.length - 1;
        for (int i = max; i >= 0; i--) {
            Entry e = entries[i];
            if (e == null) {
                break;
            }
            if (e.matches(src, comp, dst)) {
                if (i < max - 4) {
                    System.arraycopy(entries, i+1, entries, i, max - i);
                    entries[max] = e;
                }
                return e.getValue();
            }
        }

        return null;
    }

    public synchronized void put(SurfaceType src,
                    CompositeType comp,
                    SurfaceType dst,
                    Object value)
    {
        Entry e = new Entry(src, comp, dst, value);

        int num = entries.length;
        System.arraycopy(entries, 1, entries, 0, num - 1);
        entries[num - 1] = e;
    }
}
