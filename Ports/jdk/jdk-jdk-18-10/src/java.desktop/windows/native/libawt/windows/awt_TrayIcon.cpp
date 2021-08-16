/*
 * Copyright (c) 2005, 2014, Oracle and/or its affiliates. All rights reserved.
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

#include "awt.h"
#include <windowsx.h>
#include <shellapi.h>
#include <shlwapi.h>

#include "awt_Toolkit.h"
#include "awt_TrayIcon.h"
#include "awt_AWTEvent.h"

#include <java_awt_event_InputEvent.h>

/***********************************************************************/
// Struct for _SetToolTip() method
struct SetToolTipStruct {
    jobject trayIcon;
    jstring tooltip;
};
// Struct for _SetIcon() method
struct SetIconStruct {
    jobject trayIcon;
    HICON hIcon;
};
// Struct for _UpdateIcon() method
struct UpdateIconStruct {
    jobject trayIcon;
    jboolean update;
};
// Struct for _DisplayMessage() method
struct DisplayMessageStruct {
    jobject trayIcon;
    jstring caption;
    jstring text;
    jstring msgType;
};

typedef struct tagBitmapheader  {
    BITMAPV5HEADER bmiHeader;
    DWORD            dwMasks[256];
} Bitmapheader, *LPBITMAPHEADER;


/************************************************************************
 * AwtTrayIcon fields
 */

jfieldID AwtTrayIcon::idID;
jfieldID AwtTrayIcon::actionCommandID;

HWND AwtTrayIcon::sm_msgWindow = NULL;
AwtTrayIcon::TrayIconListItem* AwtTrayIcon::sm_trayIconList = NULL;
int AwtTrayIcon::sm_instCount = 0;

/************************************************************************
 * AwtTrayIcon methods
 */

AwtTrayIcon::AwtTrayIcon() {
    ::ZeroMemory(&m_nid, sizeof(m_nid));

    if (sm_instCount++ == 0 && AwtTrayIcon::sm_msgWindow == NULL) {
        sm_msgWindow = AwtTrayIcon::CreateMessageWindow();
    }
    m_mouseButtonClickAllowed = 0;
}

AwtTrayIcon::~AwtTrayIcon() {
}

void AwtTrayIcon::Dispose() {
    SendTrayMessage(NIM_DELETE);

    // Destroy the icon to avoid leak of GDI objects
    if (m_nid.hIcon != NULL) {
        ::DestroyIcon(m_nid.hIcon);
    }

    UnlinkObjects();

    if (--sm_instCount == 0) {
        AwtTrayIcon::DestroyMessageWindow();
    }

    AwtObject::Dispose();
}

LPCTSTR AwtTrayIcon::GetClassName() {
    return TEXT("SunAwtTrayIcon");
}

void AwtTrayIcon::FillClassInfo(WNDCLASS *lpwc)
{
    lpwc->style         = 0L;
    lpwc->lpfnWndProc   = (WNDPROC)TrayWindowProc;
    lpwc->cbClsExtra    = 0;
    lpwc->cbWndExtra    = 0;
    lpwc->hInstance     = AwtToolkit::GetInstance().GetModuleHandle(),
    lpwc->hIcon         = AwtToolkit::GetInstance().GetAwtIcon();
    lpwc->hCursor       = NULL;
    lpwc->hbrBackground = NULL;
    lpwc->lpszMenuName  = NULL;
    lpwc->lpszClassName = AwtTrayIcon::GetClassName();
}

void AwtTrayIcon::RegisterClass()
{
    WNDCLASS  wc;

    ::ZeroMemory(&wc, sizeof(wc));

    if (!::GetClassInfo(AwtToolkit::GetInstance().GetModuleHandle(),
                        AwtTrayIcon::GetClassName(), &wc))
    {
        AwtTrayIcon::FillClassInfo(&wc);
        ATOM atom = ::RegisterClass(&wc);
        DASSERT(atom != 0);
    }
}

void AwtTrayIcon::UnregisterClass()
{
    ::UnregisterClass(AwtTrayIcon::GetClassName(), AwtToolkit::GetInstance().GetModuleHandle());
}

HWND AwtTrayIcon::CreateMessageWindow()
{
    AwtTrayIcon::RegisterClass();

    HWND hWnd = ::CreateWindow(AwtTrayIcon::GetClassName(), TEXT("TrayMessageWindow"),
                               0, 0, 0, 0, 0, NULL, NULL,
                               AwtToolkit::GetInstance().GetModuleHandle(), NULL);
    return hWnd;
}

void AwtTrayIcon::DestroyMessageWindow()
{
    ::DestroyWindow(AwtTrayIcon::sm_msgWindow);
    AwtTrayIcon::sm_msgWindow = NULL;
    AwtTrayIcon::UnregisterClass();
}

AwtTrayIcon* AwtTrayIcon::Create(jobject self, jobject parent)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    jobject target = NULL;
    AwtTrayIcon* awtTrayIcon = NULL;

    target  = env->GetObjectField(self, AwtObject::targetID);
    DASSERT(target);

    awtTrayIcon = new AwtTrayIcon();
    awtTrayIcon->LinkObjects(env, self);
    awtTrayIcon->InitNID(env->GetIntField(target, AwtTrayIcon::idID));
    awtTrayIcon->AddTrayIconItem(awtTrayIcon->GetID());

    env->DeleteLocalRef(target);
    return awtTrayIcon;
}

void AwtTrayIcon::InitNID(UINT uID)
{
    // fix for 6271589: we MUST set the size of the structure to match
    // the shell version, otherwise some errors may occur (like missing
    // balloon messages on win2k)
    DLLVERSIONINFO dllVersionInfo;
    dllVersionInfo.cbSize = sizeof(DLLVERSIONINFO);
    int shellVersion = 5; // WIN_2000
    // MSDN: DllGetVersion should not be implicitly called, but rather
    // loaded using GetProcAddress
    HMODULE hShell = JDK_LoadSystemLibrary("Shell32.dll");
    if (hShell != NULL) {
        DLLGETVERSIONPROC proc = (DLLGETVERSIONPROC)GetProcAddress(hShell, "DllGetVersion");
        if (proc != NULL) {
            if (proc(&dllVersionInfo) == NOERROR) {
                shellVersion = dllVersionInfo.dwMajorVersion;
            }
        }
    }
    FreeLibrary(hShell);
    switch (shellVersion) {
        case 5: // WIN_2000
            m_nid.cbSize = (BYTE *)(&m_nid.guidItem) - (BYTE *)(&m_nid.cbSize);
            break;
        case 6: // WIN_XP
            m_nid.cbSize = (BYTE *)(&m_nid.hBalloonIcon) - (BYTE *)(&m_nid.cbSize);
            break;
        default: // WIN_VISTA
            m_nid.cbSize = sizeof(m_nid);
            break;
    }
    m_nid.hWnd = AwtTrayIcon::sm_msgWindow;
    m_nid.uID = uID;
    m_nid.uFlags = NIF_ICON | NIF_MESSAGE | NIF_TIP;
    m_nid.uCallbackMessage = WM_AWT_TRAY_NOTIFY;
    m_nid.hIcon = AwtToolkit::GetInstance().GetAwtIcon();
    m_nid.szTip[0] = '\0';
    m_nid.uVersion = NOTIFYICON_VERSION;
}

BOOL AwtTrayIcon::SendTrayMessage(DWORD dwMessage)
{
    return Shell_NotifyIcon(dwMessage, (PNOTIFYICONDATA)&m_nid);
}

static UINT lastMessage = WM_NULL;

LRESULT CALLBACK AwtTrayIcon::TrayWindowProc(HWND hwnd, UINT uMsg, WPARAM wParam, LPARAM lParam)
{
    LRESULT retValue = 0;
    MsgRouting mr = mrDoDefault;
    static UINT s_msgTaskbarCreated;

    switch(uMsg)
    {
        case WM_CREATE:
            // Fix for CR#6369062
            s_msgTaskbarCreated = ::RegisterWindowMessage(TEXT("TaskbarCreated"));
            break;
        case WM_AWT_TRAY_NOTIFY:
            if (hwnd == AwtTrayIcon::sm_msgWindow) {
                AwtTrayIcon* trayIcon = AwtTrayIcon::SearchTrayIconItem((UINT)wParam);
                if (trayIcon != NULL) {
                    mr = trayIcon->WmAwtTrayNotify(wParam, lParam);
                }
            }
            break;
        default:
            if(uMsg == s_msgTaskbarCreated) {
                if (hwnd == AwtTrayIcon::sm_msgWindow) {
                    mr = WmTaskbarCreated();
                }
            }
            break;
    }

    if (mr != mrConsume) {
        retValue = ::DefWindowProc(hwnd, uMsg, wParam, lParam);
    }
    return retValue;
}

/*
 * This function processes callback messages for taskbar icons.
 */
MsgRouting AwtTrayIcon::WmAwtTrayNotify(WPARAM wParam, LPARAM lParam)
{
    MsgRouting mr = mrDoDefault;

    POINT pos = {0, 0};
    ::GetCursorPos(&pos);

    lastMessage = (UINT)lParam;
    UINT flags = AwtToolkit::GetInstance().GetMouseKeyState();

    switch((UINT)lParam)
    {
        case WM_MOUSEMOVE:
            mr = WmMouseMove(flags, pos.x, pos.y);
            break;
        case WM_LBUTTONDBLCLK:
        case WM_LBUTTONDOWN:
            mr = WmMouseDown(flags, pos.x, pos.y, LEFT_BUTTON);
            break;
        case WM_LBUTTONUP:
            mr = WmMouseUp(flags, pos.x, pos.y, LEFT_BUTTON);
            break;
        case WM_RBUTTONDBLCLK:
        case WM_RBUTTONDOWN:
            mr = WmMouseDown(flags, pos.x, pos.y, RIGHT_BUTTON);
            break;
        case WM_RBUTTONUP:
            mr = WmMouseUp(flags, pos.x, pos.y, RIGHT_BUTTON);
            break;
        case WM_MBUTTONDBLCLK:
        case WM_MBUTTONDOWN:
            mr = WmMouseDown(flags, pos.x, pos.y, MIDDLE_BUTTON);
            break;
        case WM_MBUTTONUP:
            mr = WmMouseUp(flags, pos.x, pos.y, MIDDLE_BUTTON);
            break;
        case WM_CONTEXTMENU:
            mr = WmContextMenu(0, pos.x, pos.y);
            break;
        case NIN_KEYSELECT:
            mr = WmKeySelect(0, pos.x, pos.y);
            break;
        case NIN_SELECT:
            mr = WmSelect(0, pos.x, pos.y);
            break;
        case NIN_BALLOONUSERCLICK:
            mr = WmBalloonUserClick(0, pos.x, pos.y);
            break;
    }
    return mr;
}

/* Double-click variables. */
static jlong multiClickTime = ::GetDoubleClickTime();
static int multiClickMaxX = ::GetSystemMetrics(SM_CXDOUBLECLK);
static int multiClickMaxY = ::GetSystemMetrics(SM_CYDOUBLECLK);
static AwtTrayIcon* lastClickTrIc = NULL;
static jlong lastTime = 0;
static int lastClickX = 0;
static int lastClickY = 0;
static int lastButton = 0;
static int clickCount = 0;

MsgRouting AwtTrayIcon::WmMouseDown(UINT flags, int x, int y, int button)
{
    jlong now = ::JVM_CurrentTimeMillis(NULL, 0);
    jint javaModif = AwtComponent::GetJavaModifiers();

    if (lastClickTrIc == this &&
        lastButton == button &&
        (now - lastTime) <= multiClickTime &&
        abs(x - lastClickX) <= multiClickMaxX &&
        abs(y - lastClickY) <= multiClickMaxY)
    {
        clickCount++;
    } else {
        clickCount = 1;
        lastClickTrIc = this;
        lastButton = button;
        lastClickX = x;
        lastClickY = y;
    }
    lastTime = now;
    // it's needed only if WM_LBUTTONUP doesn't come for some reason
    m_mouseButtonClickAllowed |= AwtComponent::GetButtonMK(button);

    MSG msg;
    AwtComponent::InitMessage(&msg, lastMessage, flags, MAKELPARAM(x, y), x, y);

    SendMouseEvent(java_awt_event_MouseEvent_MOUSE_PRESSED, now, x, y,
                   javaModif, clickCount, JNI_FALSE,
                   AwtComponent::GetButton(button), &msg);

    return mrConsume;
}

MsgRouting AwtTrayIcon::WmMouseUp(UINT flags, int x, int y, int button)
{
    MSG msg;
    AwtComponent::InitMessage(&msg, lastMessage, flags, MAKELPARAM(x, y), x, y);

    SendMouseEvent(java_awt_event_MouseEvent_MOUSE_RELEASED, ::JVM_CurrentTimeMillis(NULL, 0),
                   x, y, AwtComponent::GetJavaModifiers(), clickCount,
                   (AwtComponent::GetButton(button) == java_awt_event_MouseEvent_BUTTON3 ?
                    TRUE : FALSE), AwtComponent::GetButton(button), &msg);

    if ((m_mouseButtonClickAllowed & AwtComponent::GetButtonMK(button)) != 0) { // No up-button in the drag-state
        SendMouseEvent(java_awt_event_MouseEvent_MOUSE_CLICKED,
                       ::JVM_CurrentTimeMillis(NULL, 0), x, y, AwtComponent::GetJavaModifiers(),
                       clickCount, JNI_FALSE, AwtComponent::GetButton(button));
    }
    m_mouseButtonClickAllowed &= ~AwtComponent::GetButtonMK(button); // Exclude the up-button from the drag-state

    return mrConsume;
}

MsgRouting AwtTrayIcon::WmMouseMove(UINT flags, int x, int y)
{
    MSG msg;
    static AwtTrayIcon* lastComp = NULL;
    static int lastX = 0;
    static int lastY = 0;

    /*
     * Workaround for CR#6267980
     * Windows sends WM_MOUSEMOVE if mouse is motionless
     */
    if (lastComp != this || x != lastX || y != lastY) {
        lastComp = this;
        lastX = x;
        lastY = y;
        AwtComponent::InitMessage(&msg, lastMessage, flags, MAKELPARAM(x, y), x, y);
        if ((flags & ALL_MK_BUTTONS) != 0) {
            m_mouseButtonClickAllowed = 0;
        } else {
            SendMouseEvent(java_awt_event_MouseEvent_MOUSE_MOVED, ::JVM_CurrentTimeMillis(NULL, 0), x, y,
                           AwtComponent::GetJavaModifiers(), 0, JNI_FALSE,
                           java_awt_event_MouseEvent_NOBUTTON, &msg);
        }
    }
    return mrConsume;
}

MsgRouting AwtTrayIcon::WmBalloonUserClick(UINT flags, int x, int y)
{
    // The windows api GetKeyState() when read would provide the key state of the requrested key
    // but it is not guaranteed to receive the same as it is stored in the thread message queue and
    // unless the thread runs faster.
    // Event NIN_BALLOONUSERCLICK is received only upon left mouse click. Hence the additional check
    // is not required.
    MSG msg;
    AwtComponent::InitMessage(&msg, lastMessage, flags, MAKELPARAM(x, y), x, y);
    SendActionEvent(java_awt_event_ActionEvent_ACTION_PERFORMED, ::JVM_CurrentTimeMillis(NULL, 0),
                    AwtComponent::GetActionModifiers(), &msg);
    return mrConsume;
}

MsgRouting AwtTrayIcon::WmKeySelect(UINT flags, int x, int y)
{
    static jlong lastKeySelectTime = 0;
    jlong now = ::JVM_CurrentTimeMillis(NULL, 0);

    // If a user selects a notify icon with the ENTER key,
    // Shell 5.0 sends double NIN_KEYSELECT notification.
    if (lastKeySelectTime != now) {
        MSG msg;
        AwtComponent::InitMessage(&msg, lastMessage, flags, MAKELPARAM(x, y), x, y);
        SendActionEvent(java_awt_event_ActionEvent_ACTION_PERFORMED, ::JVM_CurrentTimeMillis(NULL, 0),
                        AwtComponent::GetActionModifiers(), &msg);
    }
    lastKeySelectTime = now;

    return mrConsume;
}

MsgRouting AwtTrayIcon::WmSelect(UINT flags, int x, int y)
{

    // If a user click on a notify icon with the mouse,
    // Shell 5.0 sends NIN_SELECT notification on every click.
    // To be compatible with JDK6.0 only second click is important.
    if (clickCount == 2) {
        MSG msg;
        AwtComponent::InitMessage(&msg, lastMessage, flags, MAKELPARAM(x, y), x, y);
        SendActionEvent(java_awt_event_ActionEvent_ACTION_PERFORMED, ::JVM_CurrentTimeMillis(NULL, 0),
                        AwtComponent::GetActionModifiers(), &msg);
    }
    return mrConsume;
}

MsgRouting AwtTrayIcon::WmContextMenu(UINT flags, int x, int y)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    jobject peer = GetPeer(env);
    if (peer != NULL) {
        JNU_CallMethodByName(env, NULL, peer, "showPopupMenu",
                             "(II)V", x, y);
    }
    return mrConsume;
}

/*
 * Adds all icons we already have to taskbar.
 * We use this method on taskbar recreation (see 6369062).
 */
MsgRouting AwtTrayIcon::WmTaskbarCreated() {
    TrayIconListItem* item;
    for (item = sm_trayIconList; item != NULL; item = item->m_next) {
        BOOL result = item->m_trayIcon->SendTrayMessage(NIM_ADD);
        // 6270114: Instructs the taskbar to behave according to the Shell version 5.0
        if (result) {
            item->m_trayIcon->SendTrayMessage(NIM_SETVERSION);
        }
    }
    return mrDoDefault;
}

void AwtTrayIcon::SendMouseEvent(jint id, jlong when, jint x, jint y,
                                 jint modifiers, jint clickCount,
                                 jboolean popupTrigger, jint button,
                                 MSG *pMsg)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    if (GetPeer(env) == NULL) {
        /* event received during termination. */
        return;
    }

    static jclass mouseEventCls;
    if (mouseEventCls == NULL) {
        jclass mouseEventClsLocal =
            env->FindClass("java/awt/event/MouseEvent");
        if (!mouseEventClsLocal) {
            /* exception already thrown */
            return;
        }
        mouseEventCls = (jclass)env->NewGlobalRef(mouseEventClsLocal);
        env->DeleteLocalRef(mouseEventClsLocal);
    }

    static jmethodID mouseEventConst;
    if (mouseEventConst == NULL) {
        mouseEventConst =
            env->GetMethodID(mouseEventCls, "<init>",
                             "(Ljava/awt/Component;IJIIIIIIZI)V");
        DASSERT(mouseEventConst);
        CHECK_NULL(mouseEventConst);
    }
    if (env->EnsureLocalCapacity(2) < 0) {
        return;
    }
    jobject target = GetTarget(env);
    jobject mouseEvent = env->NewObject(mouseEventCls, mouseEventConst,
                                        target,
                                        id, when, modifiers,
                                        x, y, // no client area coordinates
                                        x, y,
                                        clickCount, popupTrigger, button);

    if (safe_ExceptionOccurred(env)) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }

    DASSERT(mouseEvent != NULL);
    if (pMsg != 0) {
        AwtAWTEvent::saveMSG(env, pMsg, mouseEvent);
    }
    SendEvent(mouseEvent);

    env->DeleteLocalRef(mouseEvent);
    env->DeleteLocalRef(target);
}

void AwtTrayIcon::SendActionEvent(jint id, jlong when, jint modifiers, MSG *pMsg)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    if (GetPeer(env) == NULL) {
        /* event received during termination. */
        return;
    }

    static jclass actionEventCls;
    if (actionEventCls == NULL) {
        jclass actionEventClsLocal =
            env->FindClass("java/awt/event/ActionEvent");
        if (!actionEventClsLocal) {
            /* exception already thrown */
            return;
        }
        actionEventCls = (jclass)env->NewGlobalRef(actionEventClsLocal);
        env->DeleteLocalRef(actionEventClsLocal);
    }

    static jmethodID actionEventConst;
    if (actionEventConst == NULL) {
        actionEventConst =
            env->GetMethodID(actionEventCls, "<init>",
                             "(Ljava/lang/Object;ILjava/lang/String;JI)V");
        DASSERT(actionEventConst);
        CHECK_NULL(actionEventConst);
    }
    if (env->EnsureLocalCapacity(2) < 0) {
        return;
    }
    jobject target = GetTarget(env);
    jstring actionCommand = (jstring)env->GetObjectField(target, AwtTrayIcon::actionCommandID);
    jobject actionEvent = env->NewObject(actionEventCls, actionEventConst,
                                         target, id, actionCommand, when, modifiers);

    if (safe_ExceptionOccurred(env)) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }

    DASSERT(actionEvent != NULL);
    if (pMsg != 0) {
        AwtAWTEvent::saveMSG(env, pMsg, actionEvent);
    }
    SendEvent(actionEvent);

    env->DeleteLocalRef(actionEvent);
    env->DeleteLocalRef(target);
    env->DeleteLocalRef(actionCommand);
}

AwtTrayIcon* AwtTrayIcon::SearchTrayIconItem(UINT id) {
    TrayIconListItem* item;
    for (item = sm_trayIconList; item != NULL; item = item->m_next) {
        if (item->m_ID == id) {
            return item->m_trayIcon;
        }
    }
    /*
     * DASSERT(FALSE);
     * This should not be happend if all tray icons are recorded
     */
    return NULL;
}

void AwtTrayIcon::RemoveTrayIconItem(UINT id) {
    TrayIconListItem* item = sm_trayIconList;
    TrayIconListItem* lastItem = NULL;
    while (item != NULL) {
        if (item->m_ID == id) {
            if (lastItem == NULL) {
                sm_trayIconList = item->m_next;
            } else {
                lastItem->m_next = item->m_next;
            }
            item->m_next = NULL;
            DASSERT(item != NULL);
            delete item;
            return;
        }
        lastItem = item;
        item = item->m_next;
    }
}

void AwtTrayIcon::LinkObjects(JNIEnv *env, jobject peer)
{
    if (m_peerObject == NULL) {
        m_peerObject = env->NewGlobalRef(peer);
    }

    /* Bind JavaPeer -> C++*/
    JNI_SET_PDATA(peer, this);
}

void AwtTrayIcon::UnlinkObjects()
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    if (m_peerObject) {
        JNI_SET_PDATA(m_peerObject, static_cast<PDATA>(NULL));
        env->DeleteGlobalRef(m_peerObject);
        m_peerObject = NULL;
    }
}

HBITMAP AwtTrayIcon::CreateBMP(HWND hW,int* imageData,int nSS, int nW, int nH)
{
    Bitmapheader    bmhHeader = {0};
    HDC             hDC;
    char            *ptrImageData;
    HBITMAP         hbmpBitmap;
    HBITMAP         hBitmap;
    int             nNumChannels    = 4;

    if (!hW) {
        hW = ::GetDesktopWindow();
    }
    hDC = ::GetDC(hW);
    if (!hDC) {
        return NULL;
    }

    bmhHeader.bmiHeader.bV5Size              = sizeof(BITMAPV5HEADER);
    bmhHeader.bmiHeader.bV5Width             = nW;
    bmhHeader.bmiHeader.bV5Height            = -nH;
    bmhHeader.bmiHeader.bV5Planes            = 1;

    bmhHeader.bmiHeader.bV5BitCount          = 32;
    bmhHeader.bmiHeader.bV5Compression       = BI_BITFIELDS;

    // The following mask specification specifies a supported 32 BPP
    // alpha format for Windows XP.
    bmhHeader.bmiHeader.bV5RedMask   =  0x00FF0000;
    bmhHeader.bmiHeader.bV5GreenMask =  0x0000FF00;
    bmhHeader.bmiHeader.bV5BlueMask  =  0x000000FF;
    bmhHeader.bmiHeader.bV5AlphaMask =  0xFF000000;

    hbmpBitmap = ::CreateDIBSection(hDC, (BITMAPINFO*)&(bmhHeader),
                                    DIB_RGB_COLORS,
                                    (void**)&(ptrImageData),
                                    NULL, 0);
    int  *srcPtr = imageData;
    char *dstPtr = ptrImageData;
    if (!dstPtr) {
        ReleaseDC(hW, hDC);
        return NULL;
    }
    for (int nOutern = 0; nOutern < nH; nOutern++) {
        for (int nInner = 0; nInner < nSS; nInner++) {
            dstPtr[3] = (*srcPtr >> 0x18) & 0xFF;
            dstPtr[2] = (*srcPtr >> 0x10) & 0xFF;
            dstPtr[1] = (*srcPtr >> 0x08) & 0xFF;
            dstPtr[0] = *srcPtr & 0xFF;

            srcPtr++;
            dstPtr += nNumChannels;
        }
    }

    // convert it into DDB to make CustomCursor work on WIN95
    hBitmap = CreateDIBitmap(hDC,
                             (BITMAPINFOHEADER*)&bmhHeader,
                             CBM_INIT,
                             (void *)ptrImageData,
                             (BITMAPINFO*)&bmhHeader,
                             DIB_RGB_COLORS);

    ::DeleteObject(hbmpBitmap);
    ::ReleaseDC(hW, hDC);
//  ::GdiFlush();
    return hBitmap;
}

void AwtTrayIcon::SetToolTip(LPCTSTR tooltip)
{
    if (tooltip == NULL) {
        m_nid.szTip[0] = '\0';
    } else if (lstrlen(tooltip) >= TRAY_ICON_TOOLTIP_MAX_SIZE) {
        _tcsncpy(m_nid.szTip, tooltip, TRAY_ICON_TOOLTIP_MAX_SIZE);
        m_nid.szTip[TRAY_ICON_TOOLTIP_MAX_SIZE - 1] = '\0';
    } else {
        _tcscpy_s(m_nid.szTip, TRAY_ICON_TOOLTIP_MAX_SIZE, tooltip);
    }

    SendTrayMessage(NIM_MODIFY);
}

void AwtTrayIcon::_SetToolTip(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    SetToolTipStruct *sts = (SetToolTipStruct *)param;
    jobject self = sts->trayIcon;
    jstring jtooltip = sts->tooltip;
    AwtTrayIcon *trayIcon = NULL;
    LPCTSTR tooltipStr = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    trayIcon = (AwtTrayIcon *)pData;

    if (jtooltip == NULL) {
        trayIcon->SetToolTip(NULL);
        goto ret;
    }

    tooltipStr = JNU_GetStringPlatformChars(env, jtooltip, (jboolean *)NULL);
    if (env->ExceptionCheck()) goto ret;
    trayIcon->SetToolTip(tooltipStr);
    JNU_ReleaseStringPlatformChars(env, jtooltip, tooltipStr);
ret:
    env->DeleteGlobalRef(self);
    env->DeleteGlobalRef(jtooltip);
    delete sts;
}

void AwtTrayIcon::SetIcon(HICON hIcon)
{
    ::DestroyIcon(m_nid.hIcon);
    m_nid.hIcon = hIcon;
}

void AwtTrayIcon::_SetIcon(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    SetIconStruct *sis = (SetIconStruct *)param;
    jobject self = sis->trayIcon;
    HICON hIcon = sis->hIcon;
    AwtTrayIcon *trayIcon = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    trayIcon = (AwtTrayIcon *)pData;

    trayIcon->SetIcon(hIcon);

ret:
    env->DeleteGlobalRef(self);
    delete sis;
}

void AwtTrayIcon::_UpdateIcon(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    UpdateIconStruct *uis = (UpdateIconStruct *)param;
    jobject self = uis->trayIcon;
    jboolean jupdate = uis->update;
    AwtTrayIcon *trayIcon = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    trayIcon = (AwtTrayIcon *)pData;

    BOOL result = trayIcon->SendTrayMessage(jupdate == JNI_TRUE ? NIM_MODIFY : NIM_ADD);
    // 6270114: Instructs the taskbar to behave according to the Shell version 5.0
    if (result && jupdate == JNI_FALSE) {
        trayIcon->SendTrayMessage(NIM_SETVERSION);
    }
ret:
    env->DeleteGlobalRef(self);
    delete uis;
}

void AwtTrayIcon::DisplayMessage(LPCTSTR caption, LPCTSTR text, LPCTSTR msgType)
{
    m_nid.uFlags |= NIF_INFO;
    m_nid.uTimeout = 10000;

    if (lstrcmp(msgType, TEXT("ERROR")) == 0) {
        m_nid.dwInfoFlags = NIIF_ERROR;
    } else if (lstrcmp(msgType, TEXT("WARNING")) == 0) {
        m_nid.dwInfoFlags = NIIF_WARNING;
    } else if (lstrcmp(msgType, TEXT("INFO")) == 0) {
        m_nid.dwInfoFlags = NIIF_INFO;
    } else if (lstrcmp(msgType, TEXT("NONE")) == 0) {
        m_nid.dwInfoFlags = NIIF_NONE;
    } else {
        m_nid.dwInfoFlags = NIIF_NONE;
    }

    if (caption[0] == '\0') {
        m_nid.szInfoTitle[0] = '\0';

    } else if (lstrlen(caption) >= TRAY_ICON_BALLOON_TITLE_MAX_SIZE) {

        _tcsncpy(m_nid.szInfoTitle, caption, TRAY_ICON_BALLOON_TITLE_MAX_SIZE);
        m_nid.szInfoTitle[TRAY_ICON_BALLOON_TITLE_MAX_SIZE - 1] = '\0';

    } else {
        _tcscpy_s(m_nid.szInfoTitle, TRAY_ICON_BALLOON_TITLE_MAX_SIZE, caption);
    }

    if (text[0] == '\0') {
        m_nid.szInfo[0] = ' ';
        m_nid.szInfo[1] = '\0';

    } else if (lstrlen(text) >= TRAY_ICON_BALLOON_INFO_MAX_SIZE) {

        _tcsncpy(m_nid.szInfo, text, TRAY_ICON_BALLOON_INFO_MAX_SIZE);
        m_nid.szInfo[TRAY_ICON_BALLOON_INFO_MAX_SIZE - 1] = '\0';

    } else {
        _tcscpy_s(m_nid.szInfo, TRAY_ICON_BALLOON_INFO_MAX_SIZE, text);
    }

    SendTrayMessage(NIM_MODIFY);
    m_nid.uFlags &= ~NIF_INFO;
}

void AwtTrayIcon::_DisplayMessage(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    DisplayMessageStruct *dms = (DisplayMessageStruct *)param;
    jobject self = dms->trayIcon;
    jstring jcaption = dms->caption;
    jstring jtext = dms-> text;
    jstring jmsgType = dms->msgType;
    AwtTrayIcon *trayIcon = NULL;
    LPCTSTR captionStr = NULL;
    LPCTSTR textStr = NULL;
    LPCTSTR msgTypeStr = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    trayIcon = (AwtTrayIcon *)pData;

    captionStr = JNU_GetStringPlatformChars(env, jcaption, (jboolean *)NULL);
    if (env->ExceptionCheck()) goto ret;
    textStr = JNU_GetStringPlatformChars(env, jtext, (jboolean *)NULL);
    if (env->ExceptionCheck()) {
        JNU_ReleaseStringPlatformChars(env, jcaption, captionStr);
        goto ret;
    }
    msgTypeStr = JNU_GetStringPlatformChars(env, jmsgType, (jboolean *)NULL);
    if (env->ExceptionCheck()) {
        JNU_ReleaseStringPlatformChars(env, jcaption, captionStr);
        JNU_ReleaseStringPlatformChars(env, jtext, textStr);
        goto ret;
    }
    trayIcon->DisplayMessage(captionStr, textStr, msgTypeStr);

    JNU_ReleaseStringPlatformChars(env, jcaption, captionStr);
    JNU_ReleaseStringPlatformChars(env, jtext, textStr);
    JNU_ReleaseStringPlatformChars(env, jmsgType, msgTypeStr);
ret:
    env->DeleteGlobalRef(self);
    env->DeleteGlobalRef(jcaption);
    env->DeleteGlobalRef(jtext);
    env->DeleteGlobalRef(jmsgType);
    delete dms;
}

/************************************************************************
 * TrayIcon native methods
 */

extern "C" {

/*
 * Class:     java_awt_TrayIcon
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_java_awt_TrayIcon_initIDs(JNIEnv *env, jclass cls)
{
    TRY;

    /* init field ids */
    AwtTrayIcon::idID = env->GetFieldID(cls, "id", "I");
    DASSERT(AwtTrayIcon::idID != NULL);
    CHECK_NULL(AwtTrayIcon::idID);

    AwtTrayIcon::actionCommandID = env->GetFieldID(cls, "actionCommand", "Ljava/lang/String;");
    DASSERT(AwtTrayIcon::actionCommandID != NULL);
    CHECK_NULL( AwtTrayIcon::actionCommandID);

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WTrayIconPeer
 * Method:    create
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WTrayIconPeer_create(JNIEnv *env, jobject self)
{
    TRY;

    AwtToolkit::CreateComponent(self, NULL,
                                (AwtToolkit::ComponentFactory)
                                AwtTrayIcon::Create);
    PDATA pData;
    JNI_CHECK_PEER_CREATION_RETURN(self);

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WTrayIconPeer
 * Method:    _dispose
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WTrayIconPeer__1dispose(JNIEnv *env, jobject self)
{
    TRY;

    AwtObject::_Dispose(self);

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WTrayIconPeer
 * Method:    _setToolTip
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WTrayIconPeer_setToolTip(JNIEnv *env, jobject self,
                                              jstring tooltip)
{
    TRY;

    SetToolTipStruct *sts = new SetToolTipStruct;
    sts->trayIcon = env->NewGlobalRef(self);
    if (tooltip != NULL) {
        sts->tooltip = (jstring)env->NewGlobalRef(tooltip);
    } else {
        sts->tooltip = NULL;
    }

    AwtToolkit::GetInstance().SyncCall(AwtTrayIcon::_SetToolTip, sts);
    // global ref and sts are deleted in _SetToolTip

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WTrayIconPeer
 * Method:    setNativeIcon
 * Signature: (I[B[IIIII)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WTrayIconPeer_setNativeIcon(JNIEnv *env, jobject self,
                                                 jintArray intRasterData, jbyteArray andMask,
                                                 jint nSS, jint nW, jint nH)
{
    TRY;

    int length = env->GetArrayLength(andMask);
    jbyte *andMaskPtr = new jbyte[length];

    env->GetByteArrayRegion(andMask, 0, length, andMaskPtr);

    HBITMAP hMask = ::CreateBitmap(nW, nH, 1, 1, (BYTE *)andMaskPtr);
//    ::GdiFlush();

    delete[] andMaskPtr;

    /* Copy the raster data because GDI may fail on some Java heap
     * allocated memory.
     */
    length = env->GetArrayLength(intRasterData);
    jint *intRasterDataPtr = new jint[length];
    HBITMAP hColor = NULL;
    try {
        env->GetIntArrayRegion(intRasterData, 0, length, intRasterDataPtr);
        hColor = AwtTrayIcon::CreateBMP(NULL, (int *)intRasterDataPtr, nSS, nW, nH);
    } catch (...) {
        delete[] intRasterDataPtr;
        ::DeleteObject(hMask);
        throw;
    }
    delete[] intRasterDataPtr;

    HICON hIcon = NULL;

    if (hMask && hColor) {
        ICONINFO icnInfo;
        memset(&icnInfo, 0, sizeof(ICONINFO));
        icnInfo.hbmMask = hMask;
        icnInfo.hbmColor = hColor;
        icnInfo.fIcon = TRUE;
        icnInfo.xHotspot = TRAY_ICON_X_HOTSPOT;
        icnInfo.yHotspot = TRAY_ICON_Y_HOTSPOT;

        hIcon = ::CreateIconIndirect(&icnInfo);
    }
    ::DeleteObject(hColor);
    ::DeleteObject(hMask);

    //////////////////////////////////////////

    SetIconStruct *sis = new SetIconStruct;
    sis->trayIcon = env->NewGlobalRef(self);
    sis->hIcon = hIcon;

    AwtToolkit::GetInstance().SyncCall(AwtTrayIcon::_SetIcon, sis);
    // global ref is deleted in _SetIcon

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WTrayIconPeer
 * Method:    updateNativeIcon
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WTrayIconPeer_updateNativeIcon(JNIEnv *env, jobject self,
                                                    jboolean doUpdate)
{
    TRY;

    UpdateIconStruct *uis = new UpdateIconStruct;
    uis->trayIcon = env->NewGlobalRef(self);
    uis->update = doUpdate;

    AwtToolkit::GetInstance().SyncCall(AwtTrayIcon::_UpdateIcon, uis);
    // global ref is deleted in _UpdateIcon

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WTrayIconPeer
 * Method:    displayMessage
 * Signature: ()V;
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WTrayIconPeer__1displayMessage(JNIEnv *env, jobject self,
    jstring caption, jstring text, jstring msgType)
{
    TRY;

    DisplayMessageStruct *dms = new DisplayMessageStruct;
    dms->trayIcon = env->NewGlobalRef(self);
    dms->caption = (jstring)env->NewGlobalRef(caption);
    dms->text = (jstring)env->NewGlobalRef(text);
    dms->msgType = (jstring)env->NewGlobalRef(msgType);

    AwtToolkit::GetInstance().SyncCall(AwtTrayIcon::_DisplayMessage, dms);
    // global ref is deleted in _DisplayMessage

    CATCH_BAD_ALLOC(NULL);
}

} /* extern "C" */
