/*
 * Copyright (c) 2005, 2008, Oracle and/or its affiliates. All rights reserved.
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

#include "awt_Component.h"

#include <commctrl.h>

#ifndef _COMCTL32UTIL_H
#define _COMCTL32UTIL_H

class ComCtl32Util
{
    public:
        static ComCtl32Util &GetInstance() {
            static ComCtl32Util theInstance;
            return theInstance;
        }

        void InitLibraries();

        INLINE BOOL IsToolTipControlInitialized() {
            return m_bToolTipControlInitialized;
        }

        WNDPROC SubclassHWND(HWND hwnd, WNDPROC _WindowProc);
        // DefWindowProc is the same as returned from SubclassHWND
        void UnsubclassHWND(HWND hwnd, WNDPROC _WindowProc, WNDPROC _DefWindowProc);
        // DefWindowProc is the same as returned from SubclassHWND or NULL
        LRESULT DefWindowProc(WNDPROC _DefWindowProc, HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam);

    private:
        ComCtl32Util();
        ~ComCtl32Util();

        BOOL m_bToolTipControlInitialized;

        // comctl32.dll version 6 window proc
        static LRESULT CALLBACK SharedWindowProc(HWND hwnd, UINT message,
                                                 WPARAM wParam, LPARAM lParam,
                                                 UINT_PTR uIdSubclass, DWORD_PTR dwRefData);
};

#endif // _COMCTL32UTIL_H
