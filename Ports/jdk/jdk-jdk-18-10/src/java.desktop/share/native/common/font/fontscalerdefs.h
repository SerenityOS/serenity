/*
 * Copyright (c) 2003, 2013, Oracle and/or its affiliates. All rights reserved.
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

#ifndef FontScalerDefsIncludesDefined
#define FontScalerDefsIncludesDefined

#include "AccelGlyphCache.h"

#ifdef  __cplusplus
extern "C" {
#endif

#ifdef _LP64
typedef unsigned int            UInt32;
typedef int                     Int32;
#else
typedef unsigned long           UInt32;
typedef long                    Int32;
#endif
typedef unsigned short          UInt16;
typedef short                   Int16;
typedef unsigned char           UInt8;

typedef UInt8                   Byte;
typedef Int32                   hsFixed;
typedef Int32                   hsFract;
typedef UInt32                  Bool32;

#ifndef  __cplusplus
#ifndef false
         #define false           0
#endif

#ifndef true
        #define true            1
#endif
#endif

  /* managed: 1 means the glyph has a hardware cached
   * copy, and its freeing is managed by the usual
   * 2D disposer code.
   * A value of 0 means its either unaccelerated (and so has no cellInfos)
   * or we want to free this in a different way.
   * The field uses previously unused padding, so doesn't enlarge
   * the structure.
   */
#define UNMANAGED_GLYPH 0
#define MANAGED_GLYPH   1
typedef struct GlyphInfo {
    float        advanceX;
    float        advanceY;
    UInt16       width;
    UInt16       height;
    UInt16       rowBytes;
    UInt8         managed;
    float        topLeftX;
    float        topLeftY;
    void         *cellInfo;
    UInt8        *image;
} GlyphInfo;

  /* We use fffe and ffff as meaning invisible glyphs which have no
   * image, or advance and an empty outline.
   * Since there are no valid glyphs with this great a value (watch out for
   * large fonts in the future!) we can safely use check for >= this value
   */
#define INVISIBLE_GLYPHS 0xfffe

#include "sunfontids.h"

/* If font is malformed then scaler context created by particular scaler
 * will be replaced by null scaler context.
 * Note that this context is not compatible with structure of the context
 * object used by particular scaler. Therefore, before using context
 * scaler has to check if it is NullContext.
 *
 * Note that in theory request with NullContext should not even reach native
 * scaler.
 *
 * It seems that the only reason to support NullContext is to simplify
 * FileFontStrike logic - presence of context is used as marker to
 * free the memory.
*/
JNIEXPORT int isNullScalerContext(void *context);

#ifdef  __cplusplus
}
#endif

#endif
