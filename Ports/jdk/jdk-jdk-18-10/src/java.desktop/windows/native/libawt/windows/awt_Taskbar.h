/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

#ifndef AWT_TASKBAR_H
#define AWT_TASKBAR_H

#include <windows.h>
#include <shlobj.h>


#ifndef __ITaskbarList_INTERFACE_DEFINED__
#define __ITaskbarList_INTERFACE_DEFINED__
extern "C" {
    const GUID CLSID_TaskbarList = {0x56FDF344, 0xFD6D, 0x11D0,
        {0x95, 0x8A, 0x00, 0x60, 0x97, 0xC9, 0xA0, 0x90}};
    const GUID IID_ITaskbarList = {0x56FDF342, 0xFD6D, 0x11D0,
        {0x95, 0x8A, 0x00, 0x60, 0x97, 0xC9, 0xA0, 0x90}};
}

class ITaskbarList : public IUnknown {
public:
    virtual HRESULT STDMETHODCALLTYPE HrInit(void) = 0;
    virtual HRESULT STDMETHODCALLTYPE AddTab(HWND hwnd) = 0;
    virtual HRESULT STDMETHODCALLTYPE DeleteTab(HWND hwnd) = 0;
    virtual HRESULT STDMETHODCALLTYPE ActivateTab(HWND hwnd) = 0;
    virtual HRESULT STDMETHODCALLTYPE SetActiveAlt(HWND hwnd) = 0;
};
#endif  /* ITaskbarList */

#ifndef __ITaskbarList2_INTERFACE_DEFINED__
#define __ITaskbarList2_INTERFACE_DEFINED__

class ITaskbarList2 : public ITaskbarList {
public:
    virtual HRESULT STDMETHODCALLTYPE MarkFullscreenWindow(HWND hwnd, BOOL fFullscreen) = 0;
};
#endif  /* ITaskbarList2 */

#ifndef __ITaskbarList3_INTERFACE_DEFINED__
#define __ITaskbarList3_INTERFACE_DEFINED__

typedef enum THUMBBUTTONFLAGS {
    THBF_ENABLED = 0, THBF_DISABLED = 0x1, THBF_DISMISSONCLICK = 0x2, THBF_NOBACKGROUND = 0x4, THBF_HIDDEN = 0x8, THBF_NONINTERACTIVE = 0x10
} THUMBBUTTONFLAGS;

typedef enum THUMBBUTTONMASK {
    THB_BITMAP = 0x1, THB_ICON = 0x2, THB_TOOLTIP = 0x4, THB_FLAGS = 0x8
} THUMBBUTTONMASK;

typedef struct THUMBBUTTON {
    THUMBBUTTONMASK dwMask;
    UINT iId;
    UINT iBitmap;
    HICON hIcon;
    WCHAR szTip[260];
    THUMBBUTTONFLAGS dwFlags;
} THUMBBUTTON;

typedef enum TBPFLAG {
    TBPF_NOPROGRESS = 0, TBPF_INDETERMINATE = 0x1, TBPF_NORMAL = 0x2, TBPF_ERROR = 0x4, TBPF_PAUSED = 0x8
} TBPFLAG;
#define THBN_CLICKED  0x1800

class ITaskbarList3 : public ITaskbarList2 {
public:
    virtual HRESULT STDMETHODCALLTYPE SetProgressValue(HWND hwnd, ULONGLONG ullCompleted, ULONGLONG ullTotal) = 0;
    virtual HRESULT STDMETHODCALLTYPE SetProgressState(HWND hwnd, TBPFLAG tbpFlags) = 0;
    virtual HRESULT STDMETHODCALLTYPE RegisterTab(HWND hwndTab, HWND hwndMDI) = 0;
    virtual HRESULT STDMETHODCALLTYPE UnregisterTab(HWND hwndTab) = 0;
    virtual HRESULT STDMETHODCALLTYPE SetTabOrder(HWND hwndTab, HWND hwndInsertBefore) = 0;
    virtual HRESULT STDMETHODCALLTYPE SetTabActive(HWND hwndTab, HWND hwndMDI, DWORD dwReserved) = 0;
    virtual HRESULT STDMETHODCALLTYPE ThumbBarAddButtons(HWND hwnd, UINT cButtons, THUMBBUTTON * pButton) = 0;
    virtual HRESULT STDMETHODCALLTYPE ThumbBarUpdateButtons(HWND hwnd, UINT cButtons, THUMBBUTTON * pButton) = 0;
    virtual HRESULT STDMETHODCALLTYPE ThumbBarSetImageList(HWND hwnd, HIMAGELIST himl) = 0;
    virtual HRESULT STDMETHODCALLTYPE SetOverlayIcon(HWND hwnd, HICON hIcon, LPCWSTR pszDescription) = 0;
    virtual HRESULT STDMETHODCALLTYPE SetThumbnailTooltip(HWND hwnd, LPCWSTR pszTip) = 0;
    virtual HRESULT STDMETHODCALLTYPE SetThumbnailClip(HWND hwnd, RECT *prcClip) = 0;
};
#endif  /* ITaskbarList3 */


ITaskbarList3 * m_Taskbar;


#endif /* AWT_TASKBAR_H */

