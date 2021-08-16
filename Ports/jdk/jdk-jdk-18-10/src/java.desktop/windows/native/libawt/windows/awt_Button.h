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

#ifndef AWT_BUTTON_H
#define AWT_BUTTON_H

#include "awt_Component.h"

#include "java_awt_Button.h"
#include "sun_awt_windows_WButtonPeer.h"


/************************************************************************
 * AwtButton class
 */

class AwtButton : public AwtComponent {
public:
    /* java.awt.Button label field ID */
    static jfieldID labelID;

    AwtButton();

    virtual LPCTSTR GetClassName();

    static AwtButton* Create(jobject self, jobject hParent);

    /* Windows message handler functions */
    MsgRouting WmMouseDown(UINT flags, int x, int y, int button);
    MsgRouting WmMouseUp(UINT flags, int x, int y, int button);
    MsgRouting OwnerDrawItem(UINT ctrlId, DRAWITEMSTRUCT& drawInfo);
    MsgRouting WmPaint(HDC hDC);

    MsgRouting HandleEvent(MSG *msg, BOOL synthetic);

    BOOL IsFocusingMouseMessage(MSG *pMsg);
    BOOL IsFocusingKeyMessage(MSG *pMsg);

    // called on Toolkit thread from JNI
    static void _SetLabel(void *param);
private:
    // 4530087: variable to keep track of left mouse press
    BOOL leftButtonDown;
    void NotifyListeners();
};

#endif // AWT_BUTTON_H
