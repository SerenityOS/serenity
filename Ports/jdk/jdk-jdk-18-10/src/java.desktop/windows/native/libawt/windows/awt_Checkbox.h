/*
 * Copyright (c) 1996, 2006, Oracle and/or its affiliates. All rights reserved.
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

#ifndef AWT_CHECKBOX_H
#define AWT_CHECKBOX_H

#include "awt_Component.h"

#include "java_awt_Checkbox.h"
#include "sun_awt_windows_WCheckboxPeer.h"


/************************************************************************
 * Component class for system provided Checkboxes
 */

class AwtCheckbox : public AwtComponent {
public:

    /* check size in Windows is always the same */
    static const int CHECK_SIZE;

    /* java.awt.Checkbox field ids */
    static jfieldID labelID;
    static jfieldID groupID;
    static jfieldID stateID;

    AwtCheckbox();

    virtual LPCTSTR GetClassName();

    /* Create a new AwtCheckbox object and window.       */
    static AwtCheckbox* Create(jobject self, jobject hParent);

    /* get state of multifont checkbox */
    BOOL GetState();

    /* get check mark size */
    static int GetCheckSize();

    /*  Windows message handler functions */
    MsgRouting WmMouseUp(UINT flags, int x, int y, int button);
    MsgRouting WmMouseDown(UINT flags, int x, int y, int button);
    MsgRouting WmNotify(UINT notifyCode);
    MsgRouting OwnerDrawItem(UINT ctrlId, DRAWITEMSTRUCT& drawInfo);
    MsgRouting WmPaint(HDC hDC);

    MsgRouting HandleEvent(MSG *msg, BOOL synthetic);

    BOOL IsFocusingMouseMessage(MSG *pMsg);
    BOOL IsFocusingKeyMessage(MSG *pMsg);

    // called on Toolkit thread from JNI
    static void _SetLabel(void *param);
    static void _SetCheckboxGroup(void *param);
    static void _SetState(void *param);

#ifdef DEBUG
    virtual void VerifyState(); /* verify checkbox and peer are in sync. */
#endif

private:
    /* for state of LButtonDown */
    BOOL m_fLButtonDowned;
};

#endif /* AWT_CHECKBOX_H */
