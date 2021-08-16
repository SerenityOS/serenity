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

#ifndef AWT_TEXTFIELD_H
#define AWT_TEXTFIELD_H

#include "awt_TextComponent.h"

#include "java_awt_TextField.h"
#include "sun_awt_windows_WTextFieldPeer.h"

#include <ole2.h>
#include <richedit.h>
#include <richole.h>

/************************************************************************
 * AwtTextField class
 */

class AwtTextField : public AwtTextComponent {
public:
    AwtTextField();

    static AwtTextField* Create(jobject self, jobject parent);

    /*
     *  Windows message handler functions
     */
    MsgRouting HandleEvent(MSG *msg, BOOL synthetic);

    virtual LRESULT WindowProc(UINT message, WPARAM wParam, LPARAM lParam);
    // invoked on Toolkit thread
    static void _SetEchoChar(void *param);

protected:

private:
    void EditSetSel(CHARRANGE &cr);

};

#endif /* AWT_TEXTFIELD_H */
