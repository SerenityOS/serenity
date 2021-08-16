/*
 * Copyright (c) 1999, 2016, Oracle and/or its affiliates. All rights reserved.
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

#define _JNI_IMPLEMENTATION_
#include <jawt.h>
#include "jni_util.h"

#include "awt.h"
#include "awt_DrawingSurface.h"

/*
 * Declare library specific JNI_Onload entry if static build
 */
DEF_STATIC_JNI_OnLoad

/*
 * Get the AWT native structure.  This function returns JNI_FALSE if
 * an error occurs.
 */
extern "C" _JNI_IMPORT_OR_EXPORT_ jboolean JNICALL JAWT_GetAWT(JNIEnv* env, JAWT* awt)
{
    if (awt == NULL) {
        return JNI_FALSE;
    }

    if (awt->version != JAWT_VERSION_1_3
        && awt->version != JAWT_VERSION_1_4
        && awt->version != JAWT_VERSION_1_7
        && awt->version != JAWT_VERSION_9) {
        return JNI_FALSE;
    }

    awt->GetDrawingSurface = DSGetDrawingSurface;
    awt->FreeDrawingSurface = DSFreeDrawingSurface;
    if (awt->version >= JAWT_VERSION_1_4) {
        awt->Lock = DSLockAWT;
        awt->Unlock = DSUnlockAWT;
        awt->GetComponent = DSGetComponent;
        if (awt->version >= JAWT_VERSION_9) {
            awt->CreateEmbeddedFrame = awt_CreateEmbeddedFrame;
            awt->SetBounds = awt_SetBounds;
            awt->SynthesizeWindowActivation = awt_SynthesizeWindowActivation;
        }
    }

    return JNI_TRUE;
}
