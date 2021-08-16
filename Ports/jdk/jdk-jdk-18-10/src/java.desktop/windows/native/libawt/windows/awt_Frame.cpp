/*
 * Copyright (c) 1996, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include "awt_Toolkit.h"
#include "awt_Frame.h"
#include "awt_MenuBar.h"
#include "awt_Dialog.h"
#include "awt_IconCursor.h"
#include "awt_Win32GraphicsDevice.h"
#include "ComCtl32Util.h"

#include <windowsx.h>

#include <java_lang_Integer.h>
#include <sun_awt_windows_WEmbeddedFrame.h>
#include <sun_awt_windows_WEmbeddedFramePeer.h>


/* IMPORTANT! Read the README.JNI file for notes on JNI converted AWT code.
 */

/***********************************************************************/
// Struct for _SetState() method
struct SetStateStruct {
    jobject frame;
    jint state;
};
// Struct for _SetMaximizedBounds() method
struct SetMaximizedBoundsStruct {
    jobject frame;
    jint x, y;
    jint width, height;
};
// Struct for _SetMenuBar() method
struct SetMenuBarStruct {
    jobject frame;
    jobject menubar;
};

// Struct for _SetIMMOption() method
struct SetIMMOptionStruct {
    jobject frame;
    jstring option;
};
// Struct for _SynthesizeWmActivate() method
struct SynthesizeWmActivateStruct {
    jobject frame;
    jboolean doActivate;
};
// Struct for _NotifyModalBlocked() method
struct NotifyModalBlockedStruct {
    jobject frame;
    jobject peer;
    jobject blockerPeer;
    jboolean blocked;
};
// Information about thread containing modal blocked embedded frames
struct BlockedThreadStruct {
    int framesCount;
    HHOOK mouseHook;
    HHOOK modalHook;
};


// Communication with plugin control

// The value must be the same as in AxControl.h
#define WM_AX_REQUEST_FOCUS_TO_EMBEDDER (WM_USER + 197)

static bool SetFocusToPluginControl(HWND hwndPlugin);

/************************************************************************
 * AwtFrame fields
 */

jfieldID AwtFrame::handleID;

jfieldID AwtFrame::undecoratedID;
jmethodID AwtFrame::getExtendedStateMID;

jmethodID AwtFrame::activateEmbeddingTopLevelMID;
jfieldID AwtFrame::isEmbeddedInIEID;

Hashtable AwtFrame::sm_BlockedThreads("AWTBlockedThreads");

/************************************************************************
 * AwtFrame methods
 */

AwtFrame::AwtFrame() {
    m_parentWnd = NULL;
    menuBar = NULL;
    m_isEmbedded = FALSE;
    m_isEmbeddedInIE = FALSE;
    m_isLightweight = FALSE;
    m_ignoreWmSize = FALSE;
    m_isMenuDropped = FALSE;
    m_isInputMethodWindow = FALSE;
    m_isUndecorated = FALSE;
    m_imeTargetComponent = NULL;
    m_actualFocusedWindow = NULL;
    m_iconic = FALSE;
    m_zoomed = FALSE;
    m_maxBoundsSet = FALSE;
    m_forceResetZoomed = FALSE;

    isInManualMoveOrSize = FALSE;
    grabbedHitTest = 0;
}

AwtFrame::~AwtFrame()
{
}

void AwtFrame::Dispose()
{
    AwtWindow::Dispose();
}

LPCTSTR AwtFrame::GetClassName() {
    return AWT_FRAME_WINDOW_CLASS_NAME;
}

/*
 * Create a new AwtFrame object and window.
 */
AwtFrame* AwtFrame::Create(jobject self, jobject parent)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    if (env->EnsureLocalCapacity(1) < 0) {
        return NULL;
    }

    PDATA pData;
    HWND hwndParent = NULL;
    AwtFrame* frame = NULL;
    jclass cls = NULL;
    jclass inputMethodWindowCls = NULL;
    jobject target = NULL;

    try {
        target = env->GetObjectField(self, AwtObject::targetID);
        JNI_CHECK_NULL_GOTO(target, "target", done);

        if (parent != NULL) {
            JNI_CHECK_PEER_GOTO(parent, done);
            {
                AwtFrame* parent = (AwtFrame *)pData;
                HWND oHWnd = parent->GetOverriddenHWnd();
                hwndParent = oHWnd ? oHWnd : parent->GetHWnd();
            }
        }

        frame = new AwtFrame();

        {
            /*
             * A variation on Netscape's hack for embedded frames: the client
             * area of the browser is a Java Frame for parenting purposes, but
             * really a Windows child window
             */
            BOOL isEmbeddedInstance = FALSE;
            BOOL isEmbedded = FALSE;
            cls = env->FindClass("sun/awt/EmbeddedFrame");

            if (cls) {
                isEmbeddedInstance = env->IsInstanceOf(target, cls);
            } else {
                throw std::bad_alloc();
            }
            INT_PTR handle;
            if (isEmbeddedInstance) {
                handle = static_cast<INT_PTR>(env->GetLongField(target, AwtFrame::handleID));
                if (handle != 0) {
                    isEmbedded = TRUE;
                }
            }
            frame->m_isEmbedded = isEmbedded;

            BOOL isLightweight = FALSE;
            cls = env->FindClass("sun/awt/LightweightFrame");
            if (cls) {
                isLightweight = env->IsInstanceOf(target, cls);
            } else {
                throw std::bad_alloc();
            }
            frame->m_isLightweight = isLightweight;

            if (isEmbedded) {
                hwndParent = (HWND)handle;

                // JDK-8056915: Handle focus communication between plugin and frame
                frame->m_isEmbeddedInIE = IsEmbeddedInIE(hwndParent);
                if (frame->m_isEmbeddedInIE) {
                    env->SetBooleanField(target, isEmbeddedInIEID, JNI_TRUE);
                }

                RECT rect;
                ::GetClientRect(hwndParent, &rect);
                //Fix for 6328675: SWT_AWT.new_Frame doesn't occupy client area under JDK6
                frame->m_isUndecorated = true;
                /*
                 * Fix for BugTraq ID 4337754.
                 * Initialize m_peerObject before the first call
                 * to AwtFrame::GetClassName().
                 */
                frame->m_peerObject = env->NewGlobalRef(self);
                frame->RegisterClass();
                DWORD exStyle = WS_EX_NOPARENTNOTIFY;

                if (GetRTL()) {
                    exStyle |= WS_EX_RIGHT | WS_EX_LEFTSCROLLBAR;
                    if (GetRTLReadingOrder())
                        exStyle |= WS_EX_RTLREADING;
                }

                frame->m_hwnd = ::CreateWindowEx(exStyle,
                                                 frame->GetClassName(), TEXT(""),
                                                 WS_CHILD | WS_CLIPCHILDREN,
                                                 0, 0,
                                                 rect.right, rect.bottom,
                                                 hwndParent, NULL,
                                                 AwtToolkit::GetInstance().GetModuleHandle(),
                                                 NULL);
                frame->LinkObjects(env, self);
                frame->SubclassHWND();

                // Update target's dimensions to reflect this embedded window.
                ::GetClientRect(frame->m_hwnd, &rect);
                ::MapWindowPoints(frame->m_hwnd, hwndParent, (LPPOINT)&rect, 2);

                env->SetIntField(target, AwtComponent::xID, rect.left);
                env->SetIntField(target, AwtComponent::yID, rect.top);
                env->SetIntField(target, AwtComponent::widthID,
                                 rect.right-rect.left);
                env->SetIntField(target, AwtComponent::heightID,
                                 rect.bottom-rect.top);
                frame->InitPeerGraphicsConfig(env, self);
                AwtToolkit::GetInstance().RegisterEmbedderProcessId(hwndParent);
            } else if (isLightweight) {
                frame->m_isUndecorated = true;
                frame->m_peerObject = env->NewGlobalRef(self);
                frame->RegisterClass();

                DWORD exStyle = 0;
                DWORD style = WS_POPUP;

                frame->CreateHWnd(env, L"",
                                  style,
                                  exStyle,
                                  0, 0, 0, 0,
                                  0,
                                  NULL,
                                  ::GetSysColor(COLOR_WINDOWTEXT),
                                  ::GetSysColor(COLOR_WINDOWFRAME),
                                  self);
            } else {
                jint state = env->CallIntMethod(self, AwtFrame::getExtendedStateMID);
                DWORD exStyle;
                DWORD style;

               // for input method windows, use minimal decorations
               inputMethodWindowCls = env->FindClass("sun/awt/im/InputMethodWindow");
               if (inputMethodWindowCls == NULL) {
                   throw std::bad_alloc();
               }

               if (env->IsInstanceOf(target, inputMethodWindowCls)) {
                   //for below-the-spot composition window, use no decoration
                   if (env->GetBooleanField(target, AwtFrame::undecoratedID) == JNI_TRUE){
                        exStyle = 0;
                        style = WS_POPUP|WS_CLIPCHILDREN;
                        frame->m_isUndecorated = TRUE;
                   } else {
                        exStyle = WS_EX_PALETTEWINDOW;
                        style = WS_CLIPCHILDREN;
                   }
                   frame->m_isInputMethodWindow = TRUE;
                } else if (env->GetBooleanField(target, AwtFrame::undecoratedID) == JNI_TRUE) {
                    exStyle = 0;
                    style = WS_POPUP | WS_SYSMENU | WS_CLIPCHILDREN |
                        WS_MAXIMIZEBOX | WS_MINIMIZEBOX;
                  if (state & java_awt_Frame_ICONIFIED) {
                      frame->setIconic(TRUE);
                  }
                    frame->m_isUndecorated = TRUE;
                } else {
                    exStyle = WS_EX_WINDOWEDGE;
                    style = WS_OVERLAPPEDWINDOW | WS_CLIPCHILDREN;
                  if (state & java_awt_Frame_ICONIFIED) {
                      frame->setIconic(TRUE);
                  }
                }

                if (GetRTL()) {
                    exStyle |= WS_EX_RIGHT | WS_EX_LEFTSCROLLBAR;
                    if (GetRTLReadingOrder())
                        exStyle |= WS_EX_RTLREADING;
                }

                jint x = env->GetIntField(target, AwtComponent::xID);
                jint y = env->GetIntField(target, AwtComponent::yID);
                jint width = env->GetIntField(target, AwtComponent::widthID);
                jint height = env->GetIntField(target, AwtComponent::heightID);

                frame->CreateHWnd(env, L"",
                                  style,
                                  exStyle,
                                  x, y, width, height,
                                  hwndParent,
                                  NULL,
                                  ::GetSysColor(COLOR_WINDOWTEXT),
                                  ::GetSysColor(COLOR_WINDOWFRAME),
                                  self);
                frame->RecalcNonClient();
            }
        }
    } catch (...) {
        env->DeleteLocalRef(target);
        env->DeleteLocalRef(cls);
        env->DeleteLocalRef(inputMethodWindowCls);
        throw;
    }

done:
    env->DeleteLocalRef(target);
    env->DeleteLocalRef(cls);
    env->DeleteLocalRef(inputMethodWindowCls);

    return frame;
}

/*
 * Returns true if the frame is embedded into Internet Explorer.
 * The function checks the class name of the parent window of the embedded frame.
 */
BOOL AwtFrame::IsEmbeddedInIE(HWND hwndParent)
{
    const char *pluginClass = "Java Plug-in Control Window";
    #define PARENT_CLASS_BUFFER_SIZE 64
    char parentClass[PARENT_CLASS_BUFFER_SIZE];

    return (::GetClassNameA(hwndParent, parentClass, PARENT_CLASS_BUFFER_SIZE) > 0)
           && (strncmp(parentClass, pluginClass, PARENT_CLASS_BUFFER_SIZE) == 0);
}


LRESULT AwtFrame::ProxyWindowProc(UINT message, WPARAM wParam, LPARAM lParam, MsgRouting &mr)
{
    LRESULT retValue = 0L;

    AwtComponent *focusOwner = NULL;
    AwtComponent *imeTargetComponent = NULL;

    // IME and input language related messages need to be sent to a window
    // which has the Java input focus
    switch (message) {
        case WM_IME_STARTCOMPOSITION:
        case WM_IME_ENDCOMPOSITION:
        case WM_IME_COMPOSITION:
        case WM_IME_SETCONTEXT:
        case WM_IME_NOTIFY:
        case WM_IME_CONTROL:
        case WM_IME_COMPOSITIONFULL:
        case WM_IME_SELECT:
        case WM_IME_CHAR:
        case WM_IME_REQUEST:
        case WM_IME_KEYDOWN:
        case WM_IME_KEYUP:
        case WM_INPUTLANGCHANGEREQUEST:
        case WM_INPUTLANGCHANGE:
            if (message == WM_IME_STARTCOMPOSITION) {
                SetImeTargetComponent(sm_focusOwner);
            }
            imeTargetComponent = AwtComponent::GetComponent(GetImeTargetComponent());
            if (imeTargetComponent != NULL &&
                imeTargetComponent != this) // avoid recursive calls
            {
                retValue = imeTargetComponent->WindowProc(message, wParam, lParam);
                mr = mrConsume;
            }
            if (message == WM_IME_ENDCOMPOSITION) {
                SetImeTargetComponent(NULL);
            }
            break;
        case WM_SETFOCUS:
            if (sm_inSynthesizeFocus) break; // pass it up the WindowProc chain

            if (!sm_suppressFocusAndActivation) {
                if (IsLightweightFrame() || IsEmbeddedFrame()) {
                    AwtSetActiveWindow();
                }
            }
            mr = mrConsume;
            break;
        case WM_KILLFOCUS:
            if (sm_inSynthesizeFocus) break; // pass it up the WindowProc chain

            if (!sm_suppressFocusAndActivation) {
                if (IsLightweightFrame() || IsEmbeddedFrame()) {
                    HWND oppositeToplevelHWnd = AwtComponent::GetTopLevelParentForWindow((HWND)wParam);
                    if (oppositeToplevelHWnd != AwtComponent::GetFocusedWindow()) {
                        AwtWindow::SynthesizeWmActivate(FALSE, GetHWnd(), NULL);
                    }
                }
            } else if (sm_restoreFocusAndActivation) {
                if (AwtComponent::GetFocusedWindow() != NULL) {
                    AwtWindow *focusedWindow = (AwtWindow*)GetComponent(AwtComponent::GetFocusedWindow());
                    if (focusedWindow != NULL) {
                        // Will just silently restore native focus & activation.
                        focusedWindow->AwtSetActiveWindow();
                    }
                }
            }
            mr = mrConsume;
            break;
        case 0x0127: // WM_CHANGEUISTATE
        case 0x0128: // WM_UPDATEUISTATE
            mr = mrConsume;
            break;
    }

    return retValue;
}

LRESULT AwtFrame::WindowProc(UINT message, WPARAM wParam, LPARAM lParam)
{
    MsgRouting mr = mrDoDefault;
    LRESULT retValue = 0L;

    retValue = ProxyWindowProc(message, wParam, lParam, mr);

    if (mr != mrConsume) {
        retValue = AwtWindow::WindowProc(message, wParam, lParam);
    }
    return retValue;
}

MsgRouting AwtFrame::WmShowWindow(BOOL show, UINT status)
{
    /*
     * Fix for 6492970.
     * When a non-focusable toplevel is shown alone the Java process
     * is not foreground. If one shows another (focusable) toplevel
     * the native platform not always makes it foreground (see the CR).
     * Even worse, sometimes it sends the newly shown toplevel WM_ACTIVATE
     * message. This breaks Java focus. To workaround the problem we
     * set the toplevel being shown foreground programmatically.
     * The fix is localized to non-foreground process case only.
     * (See also: 6599270)
     */
    if (!IsEmbeddedFrame() && show == TRUE && status == 0) {
        HWND fgHWnd = ::GetForegroundWindow();
        if (fgHWnd != NULL) {
            DWORD fgProcessID;
            ::GetWindowThreadProcessId(fgHWnd, &fgProcessID);

            if (fgProcessID != ::GetCurrentProcessId()) {
                AwtWindow* window = (AwtWindow*)GetComponent(GetHWnd());

                if (window != NULL &&
                    window->IsFocusableWindow() &&
                    window->IsAutoRequestFocus() &&
                    !::IsWindowVisible(GetHWnd()) && // the window is really showing
                    !::IsWindow(GetModalBlocker(GetHWnd())))
                {
                    // When the Java process is not allowed to set the foreground window
                    // (see MSDN) the request below will just have no effect.
                    ::SetForegroundWindow(GetHWnd());
                }
            }
        }
    }
    return AwtWindow::WmShowWindow(show, status);
}

MsgRouting AwtFrame::WmMouseUp(UINT flags, int x, int y, int button) {
    if (isInManualMoveOrSize) {
        isInManualMoveOrSize = FALSE;
        ::ReleaseCapture();
        return mrConsume;
    }
    return AwtWindow::WmMouseUp(flags, x, y, button);
}

MsgRouting AwtFrame::WmMouseMove(UINT flags, int x, int y) {
    /**
     * If this Frame is non-focusable then we should implement move and size operation for it by
     * ourselfves because we don't dispatch appropriate mouse messages to default window procedure.
     */
    if (!IsFocusableWindow() && isInManualMoveOrSize) {
        DWORD curPos = ::GetMessagePos();
        x = GET_X_LPARAM(curPos);
        y = GET_Y_LPARAM(curPos);
        RECT r;
        ::GetWindowRect(GetHWnd(), &r);
        POINT mouseLoc = {x, y};
        mouseLoc.x -= savedMousePos.x;
        mouseLoc.y -= savedMousePos.y;
        savedMousePos.x = x;
        savedMousePos.y = y;
        if (grabbedHitTest == HTCAPTION) {
            ::SetWindowPos(GetHWnd(), NULL, r.left+mouseLoc.x, r.top+mouseLoc.y,
                           r.right-r.left, r.bottom-r.top,
                           SWP_NOACTIVATE | SWP_NOSIZE | SWP_NOZORDER);
        } else {
            switch (grabbedHitTest) {
            case HTTOP:
                r.top += mouseLoc.y;
                break;
            case HTBOTTOM:
                r.bottom += mouseLoc.y;
                break;
            case HTRIGHT:
                r.right += mouseLoc.x;
                break;
            case HTLEFT:
                r.left += mouseLoc.x;
                break;
            case HTTOPLEFT:
                r.left += mouseLoc.x;
                r.top += mouseLoc.y;
                break;
            case HTTOPRIGHT:
                r.top += mouseLoc.y;
                r.right += mouseLoc.x;
                break;
            case HTBOTTOMLEFT:
                r.left += mouseLoc.x;
                r.bottom += mouseLoc.y;
                break;
            case HTBOTTOMRIGHT:
            case HTSIZE:
                r.right += mouseLoc.x;
                r.bottom += mouseLoc.y;
                break;
            }

            ::SetWindowPos(GetHWnd(), NULL, r.left, r.top,
                           r.right-r.left, r.bottom-r.top,
                           SWP_NOACTIVATE | SWP_NOZORDER |
                           SWP_NOCOPYBITS | SWP_DEFERERASE);
        }
        return mrConsume;
    } else {
        return AwtWindow::WmMouseMove(flags, x, y);
    }
}

MsgRouting AwtFrame::WmNcMouseUp(WPARAM hitTest, int x, int y, int button) {
    if (!IsFocusableWindow() && (button & LEFT_BUTTON)) {
        /*
         * Fix for 6399659.
         * The native system shouldn't activate the next window in z-order
         * when minimizing non-focusable window.
         */
        if (hitTest == HTMINBUTTON) {
            ::ShowWindow(GetHWnd(), SW_SHOWMINNOACTIVE);
            return mrConsume;
        }
        /**
         * If this Frame is non-focusable then we should implement move and size operation for it by
         * ourselfves because we don't dispatch appropriate mouse messages to default window procedure.
         */
        if ((button & DBL_CLICK) && hitTest == HTCAPTION) {
            // Double click on caption - maximize or restore Frame.
            if (IsResizable()) {
                if (::IsZoomed(GetHWnd())) {
                    ::ShowWindow(GetHWnd(), SW_SHOWNOACTIVATE);
                } else {
                    ::ShowWindow(GetHWnd(), SW_MAXIMIZE);
                }
            }
            return mrConsume;
        }
        switch (hitTest) {
        case HTMAXBUTTON:
            if (IsResizable()) {
                if (::IsZoomed(GetHWnd())) {
                    ::ShowWindow(GetHWnd(), SW_SHOWNOACTIVATE);
                } else {
                    ::ShowWindow(GetHWnd(), SW_MAXIMIZE);
                }
            }
            return mrConsume;
        default:
            return mrDoDefault;
        }
    }
    return AwtWindow::WmNcMouseUp(hitTest, x, y, button);
}

MsgRouting AwtFrame::WmNcMouseDown(WPARAM hitTest, int x, int y, int button) {
    // By Swing request, click on the Frame's decorations (even on
    // grabbed Frame) should generate UngrabEvent
    if (m_grabbedWindow != NULL/* && !m_grabbedWindow->IsOneOfOwnersOf(this)*/) {
        m_grabbedWindow->Ungrab();
    }
    if (!IsFocusableWindow() && (button & LEFT_BUTTON)) {
        switch (hitTest) {
        case HTTOP:
        case HTBOTTOM:
        case HTLEFT:
        case HTRIGHT:
        case HTTOPLEFT:
        case HTTOPRIGHT:
        case HTBOTTOMLEFT:
        case HTBOTTOMRIGHT:
        case HTSIZE:
            // Zoomed or non-resizable unfocusable frames should not be resizable.
            if (isZoomed() || !IsResizable()) {
                return mrConsume;
            }
        case HTCAPTION:
            // We are going to perform default mouse action on non-client area of this window
            // Grab mouse for this purpose and store coordinates for motion vector calculation
            savedMousePos.x = x;
            savedMousePos.y = y;
            ::SetCapture(GetHWnd());
            isInManualMoveOrSize = TRUE;
            grabbedHitTest = hitTest;
            return mrConsume;
        default:
            return mrDoDefault;
        }
    }
    return AwtWindow::WmNcMouseDown(hitTest, x, y, button);
}

// Override AwtWindow::Reshape() to handle minimized/maximized
// frames (see 6525850, 4065534)
void AwtFrame::Reshape(int x, int y, int w, int h)
{
    if (isIconic()) {
    // normal AwtComponent::Reshape will not work for iconified windows so...
        POINT pt = {x + w / 2, y + h / 2};
        Devices::InstanceAccess devices;
        HMONITOR monitor = MonitorFromPoint(pt, MONITOR_DEFAULTTONEAREST);
        int screen = AwtWin32GraphicsDevice::GetScreenFromHMONITOR(monitor);
        AwtWin32GraphicsDevice *device = devices->GetDevice(screen);
        // Try to set the correct size and jump to the correct location, even if
        // it is on the different monitor. Note that for the "size" we use the
        // current monitor, so the WM_DPICHANGED will adjust it for the "target"
        // monitor.
        MONITORINFO *miInfo = AwtWin32GraphicsDevice::GetMonitorInfo(screen);
        x = device == NULL ? x : device->ScaleUpAbsX(x);
        y = device == NULL ? y : device->ScaleUpAbsY(y);
        w = ScaleUpX(w);
        h = ScaleUpY(h);
        // SetWindowPlacement takes workspace coordinates, but if taskbar is at
        // top/left of screen, workspace coords != screen coords, so offset by
        // workspace origin
        x = x - (miInfo->rcWork.left - miInfo->rcMonitor.left);
        y = y - (miInfo->rcWork.top - miInfo->rcMonitor.top);
        WINDOWPLACEMENT wp;
        ::ZeroMemory(&wp, sizeof(WINDOWPLACEMENT));
        // set the window size for when it is not-iconified
        wp.length = sizeof(wp);
        wp.flags = WPF_SETMINPOSITION;
        wp.showCmd = IsVisible() ? SW_SHOWMINIMIZED : SW_HIDE;
        wp.ptMinPosition = {x, y};
        wp.ptMaxPosition = {0, 0};
        wp.rcNormalPosition = {x, y, x + w, y + h};

        // If the call is not guarded with ignoreWmSize,
        // a regression for bug 4851435 appears.
        // Having this call guarded also prevents
        // changing the iconified state of the frame
        // while calling the Frame.setBounds() method.
        m_ignoreWmSize = TRUE;
        ::SetWindowPlacement(GetHWnd(), &wp);
        m_ignoreWmSize = FALSE;

        return;
    }

    if (isZoomed()) {
    // setting size of maximized window, we remove the
    // maximized state bit (matches Motif behaviour)
    // (calling ShowWindow(SW_RESTORE) would fire an
    //  activation event which we don't want)
        HWND hWnd = GetHWnd();
        if (hWnd != NULL && ::IsWindowVisible(hWnd)) {
            LONG style = GetStyle();
            DASSERT(style & WS_MAXIMIZE);
            style ^= WS_MAXIMIZE;
            SetStyle(style);
        }
    }

    AwtWindow::Reshape(x, y, w, h);
}


/* Show the frame in it's current state */
void
AwtFrame::Show()
{
    m_visible = true;
    HWND hwnd = GetHWnd();
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    if (IsLightweightFrame()) {
        return;
    }

    DTRACE_PRINTLN3("AwtFrame::Show:%s%s%s",
                  m_iconic ? " iconic" : "",
                  m_zoomed ? " zoomed" : "",
                  m_iconic || m_zoomed ? "" : " normal");

    BOOL locationByPlatform = env->GetBooleanField(GetTarget(env), AwtWindow::locationByPlatformID);

    if (locationByPlatform) {
         moveToDefaultLocation();
    }
    EnableTranslucency(TRUE);

    BOOL autoRequestFocus = IsAutoRequestFocus();

    if (m_iconic) {
        if (m_zoomed) {
            // This whole function could probably be rewritten to use
            // ::SetWindowPlacement but MS docs doesn't tell if
            // ::SetWindowPlacement is a proper superset of
            // ::ShowWindow.  So let's be conservative and only use it
            // here, where we really do need it.
            DTRACE_PRINTLN("AwtFrame::Show(SW_SHOWMINIMIZED, WPF_RESTORETOMAXIMIZED");
            WINDOWPLACEMENT wp;
            ::ZeroMemory(&wp, sizeof(WINDOWPLACEMENT));
            wp.length = sizeof(WINDOWPLACEMENT);
            ::GetWindowPlacement(hwnd, &wp);
            if (!IsFocusableWindow() || !autoRequestFocus) {
                wp.showCmd = SW_SHOWMINNOACTIVE;
            } else {
                wp.showCmd = SW_SHOWMINIMIZED;
            }
            wp.flags |= WPF_RESTORETOMAXIMIZED;
            ::SetWindowPlacement(hwnd, &wp);
        }
        else {
            DTRACE_PRINTLN("AwtFrame::Show(SW_SHOWMINIMIZED)");
            if (!IsFocusableWindow() || !autoRequestFocus) {
                ::ShowWindow(hwnd, SW_SHOWMINNOACTIVE);
            } else {
                ::ShowWindow(hwnd, SW_SHOWMINIMIZED);
            }
        }
    }
    else if (m_zoomed) {
        DTRACE_PRINTLN("AwtFrame::Show(SW_SHOWMAXIMIZED)");
        if (!autoRequestFocus) {

            m_filterFocusAndActivation = TRUE;
            ::ShowWindow(hwnd, SW_MAXIMIZE);
            m_filterFocusAndActivation = FALSE;

        } else if (!IsFocusableWindow()) {
            ::ShowWindow(hwnd, SW_MAXIMIZE);
        } else {
            ::ShowWindow(hwnd, SW_SHOWMAXIMIZED);
        }
    }
    else if (m_isInputMethodWindow) {
        // Don't activate input methow window
        DTRACE_PRINTLN("AwtFrame::Show(SW_SHOWNA)");
        ::ShowWindow(hwnd, SW_SHOWNA);

        // After the input method window shown, we have to adjust the
        // IME candidate window position. Here is why.
        // Usually, when IMM opens the candidate window, it sends WM_IME_NOTIFY w/
        // IMN_OPENCANDIDATE message to the awt component window. The
        // awt component makes a Java call to acquire the text position
        // in order to show the candidate window just below the input method window.
        // However, by the time it acquires the position, the input method window
        // hasn't been displayed yet, the position returned is just below
        // the composed text and when the input method window is shown, it
        // will hide part of the candidate list. To fix this, we have to
        // adjust the candidate window position after the input method window
        // is shown. See bug 5012944.
        AdjustCandidateWindowPos();
    }
    else {
        // Nor iconic, nor zoomed (handled above) - so use SW_RESTORE
        // to show in "normal" state regardless of whatever stale
        // state might the invisible window still has.
        DTRACE_PRINTLN("AwtFrame::Show(SW_RESTORE)");
        if (!IsFocusableWindow() || !autoRequestFocus) {
            ::ShowWindow(hwnd, SW_SHOWNOACTIVATE);
        } else {
            ::ShowWindow(hwnd, SW_RESTORE);
        }
    }
}

void
AwtFrame::ClearMaximizedBounds()
{
    m_maxBoundsSet = FALSE;
}

void AwtFrame::AdjustCandidateWindowPos()
{
    // This method should only be called if the current frame
    // is the input method window frame.
    if (!m_isInputMethodWindow) {
        return;
    }

    RECT inputWinRec, focusWinRec;
    AwtComponent *comp = AwtComponent::GetComponent(AwtComponent::sm_focusOwner);
    if (comp == NULL) {
        return;
    }

    ::GetWindowRect(GetHWnd(), &inputWinRec);
    ::GetWindowRect(sm_focusOwner, &focusWinRec);

    LPARAM candType = comp->GetCandidateType();
    HWND defaultIMEWnd = ::ImmGetDefaultIMEWnd(GetHWnd());
    if (defaultIMEWnd == NULL) {
        return;
    }
    UINT bits = 1;
    // adjusts the candidate window position
    for (int iCandType = 0; iCandType < 32; iCandType++, bits<<=1) {
        if (candType & bits) {
            CANDIDATEFORM cf;
            cf.dwIndex = iCandType;
            cf.dwStyle = CFS_CANDIDATEPOS;
            // Since the coordinates are relative to the containing window,
            // we have to calculate the coordinates as below.
            cf.ptCurrentPos.x = inputWinRec.left - focusWinRec.left;
            cf.ptCurrentPos.y = inputWinRec.bottom - focusWinRec.top;

            // sends IMC_SETCANDIDATEPOS to IMM to move the candidate window.
            ::SendMessage(defaultIMEWnd, WM_IME_CONTROL, IMC_SETCANDIDATEPOS, (LPARAM)&cf);
        }
    }
}

void
AwtFrame::SetMaximizedBounds(int x, int y, int w, int h)
{
    m_maxPos.x  = x;
    m_maxPos.y  = y;
    m_maxSize.x = w;
    m_maxSize.y = h;
    m_maxBoundsSet = TRUE;
}

MsgRouting AwtFrame::WmGetMinMaxInfo(LPMINMAXINFO lpmmi)
{
    //Firstly call AwtWindow's function
    MsgRouting r = AwtWindow::WmGetMinMaxInfo(lpmmi);

    //Then replace maxPos & maxSize if necessary
    if (!m_maxBoundsSet) {
        return r;
    }

    if (m_maxPos.x != java_lang_Integer_MAX_VALUE)
        lpmmi->ptMaxPosition.x = m_maxPos.x;
    if (m_maxPos.y != java_lang_Integer_MAX_VALUE)
        lpmmi->ptMaxPosition.y = m_maxPos.y;
    if (m_maxSize.x != java_lang_Integer_MAX_VALUE)
        lpmmi->ptMaxSize.x = m_maxSize.x;
    if (m_maxSize.y != java_lang_Integer_MAX_VALUE)
        lpmmi->ptMaxSize.y = m_maxSize.y;
    return mrConsume;
}

MsgRouting AwtFrame::WmWindowPosChanging(LPARAM windowPos) {
    if (::IsZoomed(GetHWnd()) && m_maxBoundsSet) {
        // Limits the size of the maximized window, effectively cuts the
        // adjustments added by the window manager
        WINDOWPOS *wp = (WINDOWPOS *) windowPos;
        if (m_maxSize.x < java_lang_Integer_MAX_VALUE && wp->cx > m_maxSize.x) {
            wp->cx = m_maxSize.x;
        }
        if (m_maxSize.y < java_lang_Integer_MAX_VALUE && wp->cy > m_maxSize.y) {
            wp->cy = m_maxSize.y;
        }
    }
    return AwtWindow::WmWindowPosChanging(windowPos);
}

MsgRouting AwtFrame::WmSize(UINT type, int w, int h)
{
    currentWmSizeState = type;
    if (currentWmSizeState == SIZE_MINIMIZED) {
        UpdateSecurityWarningVisibility();
    }

    if (m_ignoreWmSize) {
        return mrDoDefault;
    }

    DTRACE_PRINTLN6("AwtFrame::WmSize: %dx%d,%s visible, state%s%s%s",
                  w, h,
                  ::IsWindowVisible(GetHWnd()) ? "" : " not",
                  m_iconic ? " iconic" : "",
                  m_zoomed ? " zoomed" : "",
                  m_iconic || m_zoomed ? "" : " normal");

    BOOL iconify = type == SIZE_MINIMIZED;

    // Note that zoom may be set to TRUE in several cases:
    //    1. type == SIZE_MAXIMIZED means that either the user or
    //       the developer (via setExtendedState(MAXIMIZED_BOTH)
    //       maximizes the frame.
    //    2. type == SIZE_MINIMIZED && isZoomed() means that a maximized
    //       frame is to be minimized. If the user minimizes a maximized
    //       frame, we need to keep the zoomed property TRUE. However,
    //       if the developer calls setExtendedState(ICONIFIED), i.e.
    //       w/o combining the ICONIFIED state with the MAXIMIZED state,
    //       we MUST RESET the zoomed property.
    //       The flag m_forceResetZoomed identifies the latter case.
    BOOL zoom =
        (
         type == SIZE_MAXIMIZED
         ||
         (type == SIZE_MINIMIZED && isZoomed())
        )
        && !m_forceResetZoomed;

    // Set the new state and send appropriate Java event
    jint oldState = java_awt_Frame_NORMAL;
    if (isIconic()) {
        oldState |= java_awt_Frame_ICONIFIED;
    }
    if (isZoomed()) {
        oldState |= java_awt_Frame_MAXIMIZED_BOTH;
    }

    jint newState = java_awt_Frame_NORMAL;
    if (iconify) {
        newState |= java_awt_Frame_ICONIFIED;
    }
    if (zoom) {
        newState |= java_awt_Frame_MAXIMIZED_BOTH;
    }

    setIconic(iconify);
    setZoomed(zoom);

    jint changed = oldState ^ newState;
    if (changed != 0) {
        NotifyWindowStateChanged(oldState, newState);
    }

    // If window is in iconic state, do not send COMPONENT_RESIZED event
    if (isIconic()) {
        return mrDoDefault;
    }

    return AwtWindow::WmSize(type, w, h);
}

MsgRouting AwtFrame::WmActivate(UINT nState, BOOL fMinimized, HWND opposite)
{
    jint type;

    if (nState != WA_INACTIVE) {
        if (::IsWindow(AwtWindow::GetModalBlocker(GetHWnd())) ||
            CheckActivateActualFocusedWindow(opposite))
        {
            return mrConsume;
        }
        type = java_awt_event_WindowEvent_WINDOW_GAINED_FOCUS;
        AwtComponent::SetFocusedWindow(GetHWnd());

    } else {
        if (::IsWindow(AwtWindow::GetModalBlocker(opposite))) {
            return mrConsume;
        } else {
            // If deactivation happens because of press on grabbing
            // window - this is nonsense, since grabbing window is
            // assumed to have focus and watch for deactivation.  But
            // this can happen - if grabbing window is proxied Window,
            // with Frame keeping real focus for it.
            if (m_grabbedWindow != NULL) {
                if (m_grabbedWindow->GetHWnd() == opposite) {
                    // Do nothing
                } else {
                    // Normally, we would rather check that this ==
                    // grabbed window, and focus is leaving it -
                    // ungrab.  But since we know about proxied
                    // windows, we simply assume this is one of the
                    // known cases.
                    if (!m_grabbedWindow->IsOneOfOwnersOf((AwtWindow*)AwtComponent::GetComponent(opposite))) {
                        m_grabbedWindow->Ungrab();
                    }
                }
            }
            CheckRetainActualFocusedWindow(opposite);

            type = java_awt_event_WindowEvent_WINDOW_LOST_FOCUS;
            AwtComponent::SetFocusedWindow(NULL);
            sm_focusOwner = NULL;
        }
    }

    SendWindowEvent(type, opposite);
    return mrConsume;
}

BOOL AwtFrame::CheckActivateActualFocusedWindow(HWND deactivatedOpositeHWnd)
{
    if (m_actualFocusedWindow != NULL) {
        HWND hwnd = m_actualFocusedWindow->GetHWnd();
        if (hwnd != NULL && ::IsWindowVisible(hwnd)) {
            SynthesizeWmActivate(TRUE, hwnd, deactivatedOpositeHWnd);
            return TRUE;
        }
        m_actualFocusedWindow = NULL;
    }
    return FALSE;
}

void AwtFrame::CheckRetainActualFocusedWindow(HWND activatedOpositeHWnd)
{
    // If actual focused window is not this Frame
    if (AwtComponent::GetFocusedWindow() != GetHWnd()) {
        // Make sure the actual focused window is an owned window of this frame
        AwtWindow *focusedWindow = (AwtWindow *)AwtComponent::GetComponent(AwtComponent::GetFocusedWindow());
        if (focusedWindow != NULL && focusedWindow->GetOwningFrameOrDialog() == this) {

            // Check that the opposite window is not this frame, nor an owned window of this frame
            if (activatedOpositeHWnd != NULL) {
                AwtWindow *oppositeWindow = (AwtWindow *)AwtComponent::GetComponent(activatedOpositeHWnd);
                if (oppositeWindow && oppositeWindow != this &&
                    oppositeWindow->GetOwningFrameOrDialog() != this)
                {
                    m_actualFocusedWindow = focusedWindow;
                }
            } else {
                 m_actualFocusedWindow = focusedWindow;
            }
        }
    }
}

BOOL AwtFrame::AwtSetActiveWindow(BOOL isMouseEventCause, UINT hittest)
{
    if (hittest == HTCLIENT) {
        // Don't let the actualFocusedWindow to steal focus if:
        // a) the frame is clicked in its client area;
        // b) focus is requested to some of the frame's child.
        m_actualFocusedWindow = NULL;
    }
    if (IsLightweightFrame()) {
        return TRUE;
    }
    if (isMouseEventCause && IsEmbeddedFrame() && m_isEmbeddedInIE) {
        HWND hwndProxy = GetProxyFocusOwner();
        // Do nothing if this frame is focused already
        if (::GetFocus() != hwndProxy) {
            // Fix for JDK-8056915:
            // If window activated with mouse, set focus to plugin control window
            // first to preserve focus owner inside browser window
            if (SetFocusToPluginControl(::GetParent(GetHWnd()))) {
                return TRUE;
            }
            // Plugin control window is already focused, so do normal processing
        }
    }
    return AwtWindow::AwtSetActiveWindow(isMouseEventCause);
}

MsgRouting AwtFrame::WmEnterMenuLoop(BOOL isTrackPopupMenu)
{
    if ( !isTrackPopupMenu ) {
        m_isMenuDropped = TRUE;
    }
    return mrDoDefault;
}

MsgRouting AwtFrame::WmExitMenuLoop(BOOL isTrackPopupMenu)
{
    if ( !isTrackPopupMenu ) {
        m_isMenuDropped = FALSE;
    }
    return mrDoDefault;
}

AwtMenuBar* AwtFrame::GetMenuBar()
{
    return menuBar;
}

void AwtFrame::SetMenuBar(AwtMenuBar* mb)
{
    if (menuBar) {
        menuBar->SetFrame(NULL);
    }
    menuBar = mb;
    if (mb == NULL) {
        // Remove existing menu bar, if any.
        ::SetMenu(GetHWnd(), NULL);
    } else {
        AwtFrame* oldFrame = menuBar->GetFrame();
        if (oldFrame && oldFrame != this) {
            oldFrame->SetMenuBar(NULL);
        }
        menuBar->SetFrame(this);
        if (menuBar->GetHMenu() != NULL) {
            ::SetMenu(GetHWnd(), menuBar->GetHMenu());
        }
    }
}

MsgRouting AwtFrame::WmDrawItem(UINT ctrlId, DRAWITEMSTRUCT& drawInfo)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    // if the item to be redrawn is the menu bar, then do it
    AwtMenuBar* awtMenubar = GetMenuBar();
    if (drawInfo.CtlType == ODT_MENU && (awtMenubar != NULL) &&
        (::GetMenu( GetHWnd() ) == (HMENU)drawInfo.hwndItem) )
        {
                awtMenubar->DrawItem(drawInfo);
                return mrConsume;
    }

        return AwtComponent::WmDrawItem(ctrlId, drawInfo);
}

MsgRouting AwtFrame::WmMeasureItem(UINT ctrlId, MEASUREITEMSTRUCT& measureInfo)
{
        JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
        AwtMenuBar* awtMenubar = GetMenuBar();
        if ((measureInfo.CtlType == ODT_MENU) && (awtMenubar != NULL))
        {
                // AwtMenu instance is stored in itemData. Use it to check if this
                // menu is the menu bar.
                AwtMenu * pMenu = (AwtMenu *) measureInfo.itemData;
                DASSERT(pMenu != NULL);
                if ( pMenu == awtMenubar )
                {
                        HWND hWnd = GetHWnd();
                        HDC hDC = ::GetDC(hWnd);
                        DASSERT(hDC != NULL);
                        awtMenubar->MeasureItem(hDC, measureInfo);
                        VERIFY(::ReleaseDC(hWnd, hDC));
                        return mrConsume;
                }
        }

        return AwtComponent::WmMeasureItem(ctrlId, measureInfo);
}

MsgRouting AwtFrame::WmGetIcon(WPARAM iconType, LRESULT& retVal)
{
    //Workaround windows bug:
    //when reseting from specific icon to class icon
    //taskbar is not updated
    if (iconType <= 2 /*ICON_SMALL2*/) {
        retVal = (LRESULT)GetEffectiveIcon(iconType);
        return mrConsume;
    } else {
        return mrDoDefault;
    }
}

void AwtFrame::DoUpdateIcon()
{
    //Workaround windows bug:
    //when reseting from specific icon to class icon
    //taskbar is not updated
    HICON hIcon = GetEffectiveIcon(ICON_BIG);
    HICON hIconSm = GetEffectiveIcon(ICON_SMALL);
    SendMessage(WM_SETICON, ICON_BIG,   (LPARAM)hIcon);
    SendMessage(WM_SETICON, ICON_SMALL, (LPARAM)hIconSm);
}

HICON AwtFrame::GetEffectiveIcon(int iconType)
{
    BOOL smallIcon = ((iconType == ICON_SMALL) || (iconType == 2/*ICON_SMALL2*/));
    HICON hIcon = (smallIcon) ? GetHIconSm() : GetHIcon();
    if (hIcon == NULL) {
        hIcon = (smallIcon) ? AwtToolkit::GetInstance().GetAwtIconSm() :
            AwtToolkit::GetInstance().GetAwtIcon();
    }
    return hIcon;
}

static BOOL keepOnMinimize(jobject peer) {
    static BOOL checked = FALSE;
    static BOOL keep = FALSE;
    if (!checked) {
        keep = (JNU_GetStaticFieldByName(AwtToolkit::GetEnv(), NULL,
            "sun/awt/windows/WFramePeer", "keepOnMinimize", "Z").z) == JNI_TRUE;
        checked = TRUE;
    }
    return keep;
}

MsgRouting AwtFrame::WmSysCommand(UINT uCmdType, int xPos, int yPos)
{
    // ignore any WM_SYSCOMMAND if this window is blocked by modal dialog
    if (::IsWindow(AwtWindow::GetModalBlocker(GetHWnd()))) {
        return mrConsume;
    }

    if (uCmdType == (SYSCOMMAND_IMM & 0xFFF0)){
        JNIEnv* env = AwtToolkit::GetEnv();
        JNU_CallMethodByName(env, NULL, m_peerObject,
            "notifyIMMOptionChange", "()V");
        DASSERT(!safe_ExceptionOccurred(env));
        return mrConsume;
    }
    if ((uCmdType == SC_MINIMIZE) && keepOnMinimize(m_peerObject)) {
        ::ShowWindow(GetHWnd(),SW_SHOWMINIMIZED);
        return mrConsume;
    }
    return AwtWindow::WmSysCommand(uCmdType, xPos, yPos);
}

LRESULT AwtFrame::WinThreadExecProc(ExecuteArgs * args)
{
    switch( args->cmdId ) {
        case FRAME_SETMENUBAR:
        {
            jobject  mbPeer = (jobject)args->param1;

            // cancel any currently dropped down menus
            if (m_isMenuDropped) {
                SendMessage(WM_CANCELMODE);
            }

            if (mbPeer == NULL) {
                // Remove existing menu bar, if any
                SetMenuBar(NULL);
            } else {
                JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
                AwtMenuBar* menuBar = (AwtMenuBar *)JNI_GET_PDATA(mbPeer);
                SetMenuBar(menuBar);
            }
            DrawMenuBar();
            break;
        }

        default:
            AwtWindow::WinThreadExecProc(args);
            break;
    }

    return 0L;
}

void AwtFrame::_SynthesizeWmActivate(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    SynthesizeWmActivateStruct *sas = (SynthesizeWmActivateStruct *)param;
    jobject self = sas->frame;
    jboolean doActivate = sas->doActivate;

    AwtFrame *frame = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    frame = (AwtFrame *)pData;

    SynthesizeWmActivate(doActivate, frame->GetHWnd(), NULL);
ret:
    env->DeleteGlobalRef(self);

    delete sas;
}

jobject AwtFrame::_GetBoundsPrivate(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    jobject self = (jobject)param;

    jobject result = NULL;
    AwtFrame *f = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    f = (AwtFrame *)pData;
    if (::IsWindow(f->GetHWnd()))
    {
        RECT rect;
        ::GetWindowRect(f->GetHWnd(), &rect);
        HWND parent = ::GetParent(f->GetHWnd());
        if (::IsWindow(parent))
        {
            POINT zero;
            zero.x = 0;
            zero.y = 0;
            ::ClientToScreen(parent, &zero);
            ::OffsetRect(&rect, -zero.x, -zero.y);
        }

        result = JNU_NewObjectByName(env, "java/awt/Rectangle", "(IIII)V",
            rect.left, rect.top, rect.bottom-rect.top, rect.right-rect.left);
    }
ret:
    env->DeleteGlobalRef(self);

    if (result != NULL)
    {
        jobject resultGlobalRef = env->NewGlobalRef(result);
        env->DeleteLocalRef(result);
        return resultGlobalRef;
    }
    else
    {
        return NULL;
    }
}

void AwtFrame::_SetState(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    SetStateStruct *sss = (SetStateStruct *)param;
    jobject self = sss->frame;
    jint state = sss->state;

    AwtFrame *f = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    f = (AwtFrame *)pData;
    HWND hwnd = f->GetHWnd();
    if (::IsWindow(hwnd))
    {
        DASSERT(!IsBadReadPtr(f, sizeof(AwtFrame)));

        BOOL iconify = (state & java_awt_Frame_ICONIFIED) != 0;
        BOOL zoom = (state & java_awt_Frame_MAXIMIZED_BOTH)
                        == java_awt_Frame_MAXIMIZED_BOTH;

        DTRACE_PRINTLN4("WFramePeer.setState:%s%s ->%s%s",
                  f->isIconic() ? " iconic" : "",
                  f->isZoomed() ? " zoomed" : "",
                  iconify       ? " iconic" : "",
                  zoom          ? " zoomed" : "");

        if (::IsWindowVisible(hwnd)) {
            BOOL focusable = f->IsFocusableWindow();

            WINDOWPLACEMENT wp;
            ::ZeroMemory(&wp, sizeof(wp));
            wp.length = sizeof(wp);
            ::GetWindowPlacement(hwnd, &wp);

            // Iconify first.
            // If both iconify & zoom are TRUE, handle this case
            // with wp.flags field below.
            if (iconify) {
                wp.showCmd = focusable ? SW_MINIMIZE : SW_SHOWMINNOACTIVE;
            } else if (zoom) {
                wp.showCmd = focusable ? SW_SHOWMAXIMIZED : SW_MAXIMIZE;
            } else { // zoom == iconify == FALSE
                wp.showCmd = focusable ? SW_RESTORE : SW_SHOWNOACTIVATE;
            }
            if (zoom && iconify) {
                wp.flags |= WPF_RESTORETOMAXIMIZED;
            } else {
                wp.flags &= ~WPF_RESTORETOMAXIMIZED;
            }

            if (!zoom) {
                f->m_forceResetZoomed = TRUE;
            }

            // The SetWindowPlacement() causes the WmSize() invocation
            //  which, in turn, actually updates the m_iconic & m_zoomed flags
            //  as well as sends Java event (WINDOW_STATE_CHANGED.)
            ::SetWindowPlacement(hwnd, &wp);

            f->m_forceResetZoomed = FALSE;
        } else {
            DTRACE_PRINTLN("  not visible, just recording the requested state");

            f->setIconic(iconify);
            f->setZoomed(zoom);
        }
    }
ret:
    env->DeleteGlobalRef(self);

    delete sss;
}

jint AwtFrame::_GetState(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    jobject self = (jobject)param;

    jint result = java_awt_Frame_NORMAL;
    AwtFrame *f = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    f = (AwtFrame *)pData;
    if (::IsWindow(f->GetHWnd()))
    {
        DASSERT(!::IsBadReadPtr(f, sizeof(AwtFrame)));
        if (f->isIconic()) {
            result |= java_awt_Frame_ICONIFIED;
        }
        if (f->isZoomed()) {
            result |= java_awt_Frame_MAXIMIZED_BOTH;
        }

        DTRACE_PRINTLN2("WFramePeer.getState:%s%s",
                  f->isIconic() ? " iconic" : "",
                  f->isZoomed() ? " zoomed" : "");
    }
ret:
    env->DeleteGlobalRef(self);

    return result;
}

void AwtFrame::_SetMaximizedBounds(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    SetMaximizedBoundsStruct *smbs = (SetMaximizedBoundsStruct *)param;
    jobject self = smbs->frame;
    int x = smbs->x;
    int y = smbs->y;
    int width = smbs->width;
    int height = smbs->height;

    AwtFrame *f = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    f = (AwtFrame *)pData;
    if (::IsWindow(f->GetHWnd()))
    {
        DASSERT(!::IsBadReadPtr(f, sizeof(AwtFrame)));
        f->SetMaximizedBounds(x, y, width, height);
    }
ret:
    env->DeleteGlobalRef(self);

    delete smbs;
}

void AwtFrame::_ClearMaximizedBounds(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    jobject self = (jobject)param;

    AwtFrame *f = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    f = (AwtFrame *)pData;
    if (::IsWindow(f->GetHWnd()))
    {
        DASSERT(!::IsBadReadPtr(f, sizeof(AwtFrame)));
        f->ClearMaximizedBounds();
    }
ret:
    env->DeleteGlobalRef(self);
}

void AwtFrame::_SetMenuBar(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    SetMenuBarStruct *smbs = (SetMenuBarStruct *)param;
    jobject self = smbs->frame;
    jobject menubar = smbs->menubar;

    AwtFrame *f = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    f = (AwtFrame *)pData;
    if (::IsWindow(f->GetHWnd()))
    {
        ExecuteArgs args;
        args.cmdId = FRAME_SETMENUBAR;
        args.param1 = (LPARAM)menubar;
        f->WinThreadExecProc(&args);
    }
ret:
    env->DeleteGlobalRef(self);
    env->DeleteGlobalRef(menubar);

    delete smbs;
}

void AwtFrame::_SetIMMOption(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    SetIMMOptionStruct *sios = (SetIMMOptionStruct *)param;
    jobject self = sios->frame;
    jstring option = sios->option;

    int badAlloc = 0;
    LPCTSTR coption;
    LPCTSTR empty = TEXT("InputMethod");
    AwtFrame *f = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    JNI_CHECK_NULL_GOTO(option, "IMMOption argument", ret);

    f = (AwtFrame *)pData;
    if (::IsWindow(f->GetHWnd()))
    {
        coption = JNU_GetStringPlatformChars(env, option, NULL);
        if (coption == NULL)
        {
            badAlloc = 1;
        }
        if (!badAlloc)
        {
            HMENU hSysMenu = ::GetSystemMenu(f->GetHWnd(), FALSE);
            ::AppendMenu(hSysMenu,  MF_STRING, SYSCOMMAND_IMM, coption);

            if (coption != empty)
            {
                JNU_ReleaseStringPlatformChars(env, option, coption);
            }
        }
    }
ret:
    env->DeleteGlobalRef(self);
    env->DeleteGlobalRef(option);

    delete sios;

    if (badAlloc)
    {
        throw std::bad_alloc();
    }
}

void AwtFrame::_NotifyModalBlocked(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    NotifyModalBlockedStruct *nmbs = (NotifyModalBlockedStruct *)param;
    jobject self = nmbs->frame;
    jobject peer = nmbs->peer;
    jobject blockerPeer = nmbs->blockerPeer;
    jboolean blocked = nmbs->blocked;

    PDATA pData;

    JNI_CHECK_PEER_GOTO(peer, ret);
    AwtFrame *f = (AwtFrame *)pData;

    // dialog here may be NULL, for example, if the blocker is a native dialog
    // however, we need to install/unistall modal hooks anyway
    JNI_CHECK_PEER_GOTO(blockerPeer, ret);
    AwtDialog *d = (AwtDialog *)pData;

    if ((f != NULL) && ::IsWindow(f->GetHWnd()))
    {
        // get an HWND of the toplevel window this embedded frame is within
        HWND fHWnd = f->GetHWnd();
        while (::GetParent(fHWnd) != NULL) {
            fHWnd = ::GetParent(fHWnd);
        }
        // we must get a toplevel hwnd here, however due to some strange
        // behaviour of Java Plugin (a bug?) when running in IE at
        // this moment the embedded frame hasn't been placed into the
        // browser yet and fHWnd is not a toplevel, so we shouldn't install
        // the hook here
        if ((::GetWindowLong(fHWnd, GWL_STYLE) & WS_CHILD) == 0) {
            // if this toplevel is created in another thread, we should install
            // the modal hook into it to track window activation and mouse events
            DWORD fThread = ::GetWindowThreadProcessId(fHWnd, NULL);
            if (fThread != AwtToolkit::GetInstance().MainThread()) {
                // check if this thread has been already blocked
                BlockedThreadStruct *blockedThread = (BlockedThreadStruct *)sm_BlockedThreads.get((void *)((intptr_t)fThread));
                if (blocked) {
                    if (blockedThread == NULL) {
                        blockedThread = new BlockedThreadStruct;
                        blockedThread->framesCount = 1;
                        blockedThread->modalHook = ::SetWindowsHookEx(WH_CBT, (HOOKPROC)AwtDialog::ModalFilterProc,
                                                                      0, fThread);
                        blockedThread->mouseHook = ::SetWindowsHookEx(WH_MOUSE, (HOOKPROC)AwtDialog::MouseHookProc_NonTT,
                                                                      0, fThread);
                        sm_BlockedThreads.put((void *)((intptr_t)fThread), blockedThread);
                    } else {
                        blockedThread->framesCount++;
                    }
                } else {
                    // see the comment above: if Java Plugin behaviour when running in IE
                    // was right, blockedThread would be always not NULL here
                    if (blockedThread != NULL) {
                        DASSERT(blockedThread->framesCount > 0);
                        if ((blockedThread->framesCount) == 1) {
                            ::UnhookWindowsHookEx(blockedThread->modalHook);
                            ::UnhookWindowsHookEx(blockedThread->mouseHook);
                            sm_BlockedThreads.remove((void *)((intptr_t)fThread));
                            delete blockedThread;
                        } else {
                            blockedThread->framesCount--;
                        }
                    }
                }
            }
        }
    }
ret:
    env->DeleteGlobalRef(self);
    env->DeleteGlobalRef(peer);
    env->DeleteGlobalRef(blockerPeer);

    delete nmbs;
}

/************************************************************************
 * WFramePeer native methods
 */

extern "C" {

/*
 * Class:     java_awt_Frame
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_java_awt_Frame_initIDs(JNIEnv *env, jclass cls)
{
    TRY;

    AwtFrame::undecoratedID = env->GetFieldID(cls,"undecorated","Z");
    DASSERT(AwtFrame::undecoratedID != NULL);

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WFramePeer
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WFramePeer_initIDs(JNIEnv *env, jclass cls)
{
    TRY;

    AwtFrame::getExtendedStateMID = env->GetMethodID(cls, "getExtendedState", "()I");
    DASSERT(AwtFrame::getExtendedStateMID);

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WFramePeer
 * Method:    setState
 * Signature: (I)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WFramePeer_setState(JNIEnv *env, jobject self,
    jint state)
{
    TRY;

    SetStateStruct *sss = new SetStateStruct;
    sss->frame = env->NewGlobalRef(self);
    sss->state = state;

    AwtToolkit::GetInstance().SyncCall(AwtFrame::_SetState, sss);
    // global ref and sss are deleted in _SetState()

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WFramePeer
 * Method:    getState
 * Signature: ()I
 */
JNIEXPORT jint JNICALL
Java_sun_awt_windows_WFramePeer_getState(JNIEnv *env, jobject self)
{
    TRY;

    jobject selfGlobalRef = env->NewGlobalRef(self);

    return static_cast<jint>(reinterpret_cast<INT_PTR>(AwtToolkit::GetInstance().SyncCall(
        (void*(*)(void*))AwtFrame::_GetState,
        (void *)selfGlobalRef)));
    // selfGlobalRef is deleted in _GetState()

    CATCH_BAD_ALLOC_RET(java_awt_Frame_NORMAL);
}


/*
 * Class:     sun_awt_windows_WFramePeer
 * Method:    setMaximizedBounds
 * Signature: (IIII)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WFramePeer_setMaximizedBounds(JNIEnv *env, jobject self,
    jint x, jint y, jint width, jint height)
{
    TRY;

    SetMaximizedBoundsStruct *smbs = new SetMaximizedBoundsStruct;
    smbs->frame = env->NewGlobalRef(self);
    smbs->x = x;
    smbs->y = y;
    smbs->width = width;
    smbs->height = height;

    AwtToolkit::GetInstance().SyncCall(AwtFrame::_SetMaximizedBounds, smbs);
    // global ref and smbs are deleted in _SetMaximizedBounds()

    CATCH_BAD_ALLOC;
}


/*
 * Class:     sun_awt_windows_WFramePeer
 * Method:    clearMaximizedBounds
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WFramePeer_clearMaximizedBounds(JNIEnv *env, jobject self)
{
    TRY;

    jobject selfGlobalRef = env->NewGlobalRef(self);

    AwtToolkit::GetInstance().SyncCall(AwtFrame::_ClearMaximizedBounds,
        (void *)selfGlobalRef);
    // selfGlobalRef is deleted in _ClearMaximizedBounds()

    CATCH_BAD_ALLOC;
}


/*
 * Class:     sun_awt_windows_WFramePeer
 * Method:    setMenuBar0
 * Signature: (Lsun/awt/windows/WMenuBarPeer;)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WFramePeer_setMenuBar0(JNIEnv *env, jobject self,
                                            jobject mbPeer)
{
    TRY;

    SetMenuBarStruct *smbs = new SetMenuBarStruct;
    smbs->frame = env->NewGlobalRef(self);
    smbs->menubar = env->NewGlobalRef(mbPeer);

    AwtToolkit::GetInstance().SyncCall(AwtFrame::_SetMenuBar, smbs);
    // global refs ans smbs are deleted in _SetMenuBar()

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WFramePeer
 * Method:    create
 * Signature: (Lsun/awt/windows/WComponentPeer;)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WFramePeer_createAwtFrame(JNIEnv *env, jobject self,
                                               jobject parent)
{
    TRY;

    AwtToolkit::CreateComponent(self, parent,
                                (AwtToolkit::ComponentFactory)
                                AwtFrame::Create);

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WFramePeer
 * Method:    getSysMenuHeight
 * Signature: ()I
 */
JNIEXPORT jint JNICALL
Java_sun_awt_windows_WFramePeer_getSysMenuHeight(JNIEnv *env, jclass self)
{
    TRY;

    return ::GetSystemMetrics(SM_CYMENUSIZE);

    CATCH_BAD_ALLOC_RET(0);
}

/*
 * Class:     sun_awt_windows_WFramePeer
 * Method:    pSetIMMOption
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WFramePeer_pSetIMMOption(JNIEnv *env, jobject self,
                                               jstring option)
{
    TRY;

    SetIMMOptionStruct *sios = new SetIMMOptionStruct;
    sios->frame = env->NewGlobalRef(self);
    sios->option = (jstring)env->NewGlobalRef(option);

    AwtToolkit::GetInstance().SyncCall(AwtFrame::_SetIMMOption, sios);
    // global refs and sios are deleted in _SetIMMOption()

    CATCH_BAD_ALLOC;
}

} /* extern "C" */


/************************************************************************
 * WEmbeddedFrame native methods
 */

extern "C" {

/*
 * Class:     sun_awt_windows_WFramePeer
 * Method:    initIDs
 * Signature: (Lsun/awt/windows/WMenuBarPeer;)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WEmbeddedFrame_initIDs(JNIEnv *env, jclass cls)
{
    TRY;

    AwtFrame::handleID = env->GetFieldID(cls, "handle", "J");
    DASSERT(AwtFrame::handleID != NULL);
    CHECK_NULL(AwtFrame::handleID);

    AwtFrame::activateEmbeddingTopLevelMID = env->GetMethodID(cls, "activateEmbeddingTopLevel", "()V");
    DASSERT(AwtFrame::activateEmbeddingTopLevelMID != NULL);
    CHECK_NULL(AwtFrame::activateEmbeddingTopLevelMID);

    AwtFrame::isEmbeddedInIEID = env->GetFieldID(cls, "isEmbeddedInIE", "Z");
    DASSERT(AwtFrame::isEmbeddedInIEID != NULL);

    CATCH_BAD_ALLOC;
}

JNIEXPORT void JNICALL
Java_sun_awt_windows_WEmbeddedFrame_notifyModalBlockedImpl(JNIEnv *env,
                                                           jobject self,
                                                           jobject peer,
                                                           jobject blockerPeer,
                                                           jboolean blocked)
{
    TRY;

    NotifyModalBlockedStruct *nmbs = new NotifyModalBlockedStruct;
    nmbs->frame = env->NewGlobalRef(self);
    nmbs->peer = env->NewGlobalRef(peer);
    nmbs->blockerPeer = env->NewGlobalRef(blockerPeer);
    nmbs->blocked = blocked;

    AwtToolkit::GetInstance().SyncCall(AwtFrame::_NotifyModalBlocked, nmbs);
    // global refs and nmbs are deleted in _NotifyModalBlocked()

    CATCH_BAD_ALLOC;
}

} /* extern "C" */


/************************************************************************
 * WEmbeddedFramePeer native methods
 */

extern "C" {

JNIEXPORT void JNICALL
Java_sun_awt_windows_WEmbeddedFramePeer_create(JNIEnv *env, jobject self,
                                               jobject parent)
{
    TRY;

    JNI_CHECK_NULL_RETURN(self, "peer");
    AwtToolkit::CreateComponent(self, parent,
                                (AwtToolkit::ComponentFactory)
                                AwtFrame::Create);

    CATCH_BAD_ALLOC;
}

JNIEXPORT jobject JNICALL
Java_sun_awt_windows_WEmbeddedFramePeer_getBoundsPrivate(JNIEnv *env, jobject self)
{
    TRY;

    jobject result = (jobject)AwtToolkit::GetInstance().SyncCall(
        (void *(*)(void *))AwtFrame::_GetBoundsPrivate,
        env->NewGlobalRef(self));
    // global ref is deleted in _GetBoundsPrivate

    if (result != NULL)
    {
        jobject resultLocalRef = env->NewLocalRef(result);
        env->DeleteGlobalRef(result);
        return resultLocalRef;
    }
    else
    {
        return NULL;
    }

    CATCH_BAD_ALLOC_RET(NULL);
}

JNIEXPORT void JNICALL
Java_sun_awt_windows_WFramePeer_synthesizeWmActivate(JNIEnv *env, jobject self, jboolean doActivate)
{
    TRY;

    SynthesizeWmActivateStruct *sas = new SynthesizeWmActivateStruct;
    sas->frame = env->NewGlobalRef(self);
    sas->doActivate = doActivate;

    /*
     * WARNING: invoking this function without synchronization by m_Sync CriticalSection.
     * Taking this lock results in a deadlock.
     */
    AwtToolkit::GetInstance().InvokeFunction(AwtFrame::_SynthesizeWmActivate, sas);
    // global ref and sas are deleted in _SynthesizeWmActivate()

    CATCH_BAD_ALLOC;
}

} /* extern "C" */

static bool SetFocusToPluginControl(HWND hwndPlugin)
{
    HWND hwndFocus = ::GetFocus();

    if (hwndFocus == hwndPlugin) {
        return false;
    }

    ::SetFocus(hwndPlugin);
    DWORD dwError = ::GetLastError();
    if (dwError != ERROR_SUCCESS) {
        // If direct call failed, use a special message to set focus
        return (::SendMessage(hwndPlugin, WM_AX_REQUEST_FOCUS_TO_EMBEDDER, 0, 0) == 0);
    }
    return true;
}
