/*
 * Copyright (c) 2003, 2006, Oracle and/or its affiliates. All rights reserved.
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

package sun.font;

import java.lang.ref.Reference;
import java.util.concurrent.ConcurrentHashMap;
import sun.java2d.Disposer;
import sun.java2d.DisposerRecord;

/*
 * This keeps track of data that needs to be cleaned up once a
 * strike is freed.
 * a) The native memory that is the glyph image cache.
 * b) removing the "desc" key from the strike's map.
 * This is safe to do because this disposer is invoked only when the
 * reference object has been cleared, which means the value indexed by
 * this key is just an empty reference object.
 * It is possible that a new FontStrike has been created that would
 * be referenced by the same (equals) key. If it is placed in the map
 * before this disposer is executed, then we do not want to remove that
 * object. We should only remove an object where the value is null.
 * So we first verify that the key still points to a cleared reference.
 * Updates to the map thus need to be synchronized.
 *
 * A WeakHashmap will automatically clean up, but we might maintain a
 * reference to the "desc" key in the FontStrike (value) which would
 * prevent the keys from being discarded. And since the strike is the only
 * place is likely we would maintain such a strong reference, then the map
 * entries would be removed much more promptly than we need.
 */

class FontStrikeDisposer
    implements DisposerRecord, Disposer.PollDisposable {

    ConcurrentHashMap<FontStrikeDesc, Reference<FontStrike>> strikeCache;
    FontStrikeDesc desc;
    long[] longGlyphImages;
    int [] intGlyphImages;
    int [][] segIntGlyphImages;
    long[][] segLongGlyphImages;
    long pScalerContext = 0L;
    boolean disposed = false;
    boolean comp = false;

    public FontStrikeDisposer(Font2D font2D, FontStrikeDesc desc,
                              long pContext, int[] images) {
        this.strikeCache = font2D.strikeCache;
        this.desc = desc;
        this.pScalerContext = pContext;
        this.intGlyphImages = images;
    }

    public FontStrikeDisposer(Font2D font2D, FontStrikeDesc desc,
                              long pContext, long[] images) {
        this.strikeCache = font2D.strikeCache;
        this.desc = desc;
        this.pScalerContext = pContext;
        this.longGlyphImages = images;
    }

    public FontStrikeDisposer(Font2D font2D, FontStrikeDesc desc,
                              long pContext) {
        this.strikeCache = font2D.strikeCache;
        this.desc = desc;
        this.pScalerContext = pContext;
    }

    public FontStrikeDisposer(Font2D font2D, FontStrikeDesc desc) {
        this.strikeCache = font2D.strikeCache;
        this.desc = desc;
        this.comp = true;
    }

    public synchronized void dispose() {
        if (!disposed) {
            Reference<FontStrike> ref = strikeCache.get(desc);
            if (ref != null) {
                Object o = ref.get();
                if (o == null) {
                    strikeCache.remove(desc);
                }
            }
            StrikeCache.disposeStrike(this);
            disposed = true;
        }
    }
}
