/*
 * Copyright (c) 1997, 2008, Oracle and/or its affiliates. All rights reserved.
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

#ifndef AWT_CURSOR_H
#define AWT_CURSOR_H

#include "ObjectList.h"
#include "awt_Object.h"
#include "awt_Toolkit.h"

class AwtComponent;

/************************************************************************
 * AwtCursor class
 */

class AwtCursor : public AwtObject {
public:
    /* java.awt.Cursor */
    static jmethodID mSetPDataID;
    static jfieldID pDataID;
    static jfieldID typeID;

    /* java.awt.Point */
    static jfieldID pointXID;
    static jfieldID pointYID;

    /* sun.awt.GlobalCursorManager */
    static jclass globalCursorManagerClass;
    static jmethodID updateCursorID;

    AwtCursor(JNIEnv *env, HCURSOR hCur, jobject jCur);
    AwtCursor(JNIEnv *env, HCURSOR hCur, jobject jCur, int xH, int yH,
              int nWid, int nHgt, int nS, int *col, BYTE *hM);
    virtual ~AwtCursor();

    virtual void Dispose();

    INLINE HCURSOR GetHCursor() {
        if (dirty) {
            Rebuild();
        }
        return hCursor;
    }
    static AwtCursor * CreateSystemCursor(jobject jCursor);
    static void UpdateCursor(AwtComponent *comp);
    static HCURSOR  GetCursor(JNIEnv *env, AwtComponent *comp);

    static void setPData(jobject cursor, jlong pdata) {
        JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
        env->CallVoidMethod(cursor, mSetPDataID, pdata);
    }

private:
    void Rebuild();

    HCURSOR hCursor;
    jweak jCursor;

    /* data needed to reconstruct new cursor */
    int xHotSpot;
    int yHotSpot;
    int nWidth;
    int nHeight;
    int nSS;
    int  *cols;
    BYTE *mask;

    BOOL custom;
    BOOL dirty;

    static AwtObjectList customCursors;
};

#endif /* AWT_CURSOR_H */
