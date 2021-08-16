/*
 * Copyright (c) 1996, 2012, Oracle and/or its affiliates. All rights reserved.
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

#ifndef AWT_TEXTAREA_H
#define AWT_TEXTAREA_H

#include "awt_TextComponent.h"

#include "java_awt_TextArea.h"
#include "sun_awt_windows_WTextAreaPeer.h"

#include <ole2.h>
#include <richedit.h>
#include <richole.h>

/************************************************************************
 * AwtTextArea class
 */

class AwtTextArea : public AwtTextComponent {

public:

    /* java.awt.TextArea fields ids */
    static jfieldID scrollbarVisibilityID;

    AwtTextArea();
    virtual ~AwtTextArea();

    virtual void Dispose();

    static AwtTextArea* Create(jobject self, jobject parent);

    static size_t CountNewLines(JNIEnv *env, jstring jStr, size_t maxlen);
    static size_t GetALength(JNIEnv* env, jstring jStr, size_t maxlen);

    LRESULT WindowProc(UINT message, WPARAM wParam, LPARAM lParam);

    MsgRouting WmEnable(BOOL fEnabled);
    MsgRouting WmNcHitTest(UINT x, UINT y, LRESULT &retVal);
    MsgRouting HandleEvent(MSG *msg, BOOL synthetic);

    virtual BOOL InheritsNativeMouseWheelBehavior();
    virtual void Reshape(int x, int y, int w, int h);

    virtual LONG getJavaSelPos(LONG orgPos);
    virtual LONG getWin32SelPos(LONG orgPos);
    virtual void SetSelRange(LONG start, LONG end);

    // called on Toolkit thread from JNI
    static void _ReplaceText(void *param);

protected:

    void EditSetSel(CHARRANGE &cr);
  private:
    LONG    m_lHDeltaAccum;
    LONG    m_lVDeltaAccum;


};

#endif /* AWT_TEXTAREA_H */
