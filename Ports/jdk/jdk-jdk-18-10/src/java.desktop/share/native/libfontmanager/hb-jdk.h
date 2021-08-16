/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

#ifndef HB_JDK_H
#define HB_JDK_H

#include "hb.h"
#include <jni.h>
#include <sunfontids.h>

# ifdef __cplusplus
extern "C" {
#endif

typedef struct JDKFontInfo_Struct {
    JNIEnv* env;
    jobject font2D;
    jobject fontStrike;
    float matrix[4];
    float ptSize;
    float xPtSize;
    float yPtSize;
    float devScale; // How much applying the full glyph tx scales x distance.
} JDKFontInfo;


// Use 16.16 for better precision than 26.6
#define HBFloatToFixedScale ((float)(1 << 16))
#define HBFloatToFixed(f) ((unsigned int)((f) * HBFloatToFixedScale))

/*
 * Note:
 *
 * Set face size on ft-face before creating hb-font from it.
 * Otherwise hb-ft would NOT pick up the font size correctly.
 */

hb_face_t *
hb_jdk_face_create(JDKFontInfo*   jdkFontInfo,
                   hb_destroy_func_t destroy);
hb_font_t *
hb_jdk_font_create(hb_face_t* hbFace,
                   JDKFontInfo*   jdkFontInfo,
                   hb_destroy_func_t destroy);


/* Makes an hb_font_t use JDK internally to implement font functions. */
void
hb_jdk_font_set_funcs(hb_font_t *font);


# ifdef __cplusplus
}
#endif

#endif /* HB_JDK_H */
