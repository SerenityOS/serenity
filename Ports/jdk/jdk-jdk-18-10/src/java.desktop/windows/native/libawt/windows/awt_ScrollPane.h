/*
 * Copyright (c) 1996, 2009, Oracle and/or its affiliates. All rights reserved.
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

#ifndef AWT_SCROLLPANE_H
#define AWT_SCROLLPANE_H

#include "awt_Canvas.h"

#include "java_awt_ScrollPane.h"
#include "java_awt_Insets.h"
#include "sun_awt_windows_WScrollPanePeer.h"


/************************************************************************
 * AwtScrollPane class
 */

class AwtScrollPane : public AwtCanvas {
public:

    /* java.awt.ScrollPane fields */
    static jfieldID scrollbarDisplayPolicyID;
    static jfieldID hAdjustableID;
    static jfieldID vAdjustableID;

    /* java.awt.ScrollPaneAdjustable fields */
    static jfieldID unitIncrementID;
    static jfieldID blockIncrementID;

    /* sun.awt.windows.WScrollPanePeer methods */
    static jmethodID postScrollEventID;

    AwtScrollPane();

    virtual LPCTSTR GetClassName();

    static AwtScrollPane* Create(jobject self, jobject hParent);

    void SetInsets(JNIEnv *env);
    void RecalcSizes(int parentWidth, int parentHeight,
                     int childWidth, int childHeight);
    virtual void Show(JNIEnv *env);
    virtual void Reshape(int x, int y, int w, int h);
    virtual void BeginValidate() {}
    virtual void EndValidate() {}

    /*
     * Fix for bug 4046446
     * Returns scroll position for the appropriate scrollbar.
     */
    int GetScrollPos(int orient);

    /*
     * Windows message handler functions
     */
    virtual MsgRouting WmNcHitTest(UINT x, UINT y, LRESULT& retVal);
    virtual MsgRouting WmHScroll(UINT scrollCode, UINT pos, HWND hScrollBar);
    virtual MsgRouting WmVScroll(UINT scrollCode, UINT pos, HWND hScrollBar);

    virtual MsgRouting HandleEvent(MSG *msg, BOOL synthetic);

    // some methods invoked on Toolkit thread
    static jint _GetOffset(void *param);
    static void _SetInsets(void *param);
    static void _SetScrollPos(void *param);
    static void _SetSpans(void *param);

#ifdef DEBUG
    virtual void VerifyState(); /* verify target and peer are in sync. */
#endif

private:
    void PostScrollEvent(int orient, int scrollCode, int pos);
    void SetScrollInfo(int orient, int max, int page, BOOL disableNoScroll);
};

#endif /* AWT_SCROLLPANE_H */
