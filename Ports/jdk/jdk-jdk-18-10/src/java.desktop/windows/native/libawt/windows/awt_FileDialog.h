/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
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

#ifndef AWT_FILE_DIALOG_H
#define AWT_FILE_DIALOG_H

#include "stdhdrs.h"
#include <commdlg.h>

#include "awt_Toolkit.h"
#include "awt_Component.h"
#include "awt_Dialog.h"

#include "java_awt_FileDialog.h"
#include "sun_awt_windows_WFileDialogPeer.h"

/************************************************************************
 * AwtFileDialog class
 */

class AwtFileDialog {
public:
    /* sun.awt.windows.WFileDialogPeer field and method ids */
    static jfieldID parentID;
    static jfieldID fileFilterID;
    static jmethodID setHWndMID;
    static jmethodID handleSelectedMID;
    static jmethodID handleCancelMID;
    static jmethodID checkFilenameFilterMID;
    static jmethodID isMultipleModeMID;

    /* java.awt.FileDialog field and method ids */
    static jfieldID modeID;
    static jfieldID dirID;
    static jfieldID fileID;
    static jfieldID filterID;

    static void Initialize(JNIEnv *env, jstring filterDescription);
    static void Show(void *peer);

    virtual BOOL InheritsNativeMouseWheelBehavior();

    // some methods called on Toolkit thread
    static void _DisposeOrHide(void *param);
    static void _ToFront(void *param);
    static void _ToBack(void *param);
    static jobject _GetLocationOnScreen(void *param);

private:
    static UINT GetBufferLength(LPTSTR buffer, UINT limit);
};

#endif /* FILE_DIALOG_H */
