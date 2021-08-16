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

#define _JNI_IMPLEMENTATION_

#include "awt.h"
#include <signal.h>
#include <windowsx.h>
#include <process.h>
#include <shellapi.h>
#include <shlwapi.h>

#include "awt_DrawingSurface.h"
#include "awt_AWTEvent.h"
#include "awt_Component.h"
#include "awt_Canvas.h"
#include "awt_Clipboard.h"
#include "awt_Frame.h"
#include "awt_Dialog.h"
#include "awt_Font.h"
#include "awt_Cursor.h"
#include "awt_InputEvent.h"
#include "awt_KeyEvent.h"
#include "awt_List.h"
#include "awt_Palette.h"
#include "awt_PopupMenu.h"
#include "awt_Toolkit.h"
#include "awt_DesktopProperties.h"
#include "awt_FileDialog.h"
#include "CmdIDList.h"
#include "awt_new.h"
#include "debug_trace.h"
#include "debug_mem.h"

#include "ComCtl32Util.h"
#include "DllUtil.h"

#include "D3DPipelineManager.h"

#include <awt_DnDDT.h>
#include <awt_DnDDS.h>

#include <java_awt_Toolkit.h>
#include <java_awt_event_InputMethodEvent.h>

extern void initScreens(JNIEnv *env);
extern "C" void awt_dnd_initialize();
extern "C" void awt_dnd_uninitialize();
extern "C" void awt_clipboard_uninitialize(JNIEnv *env);
extern "C" BOOL g_bUserHasChangedInputLang;

extern CriticalSection windowMoveLock;
extern BOOL windowMoveLockHeld;

// Needed by JAWT: see awt_DrawingSurface.cpp.
extern jclass jawtVImgClass;
extern jclass jawtVSMgrClass;
extern jclass jawtComponentClass;
extern jfieldID jawtPDataID;
extern jfieldID jawtSDataID;
extern jfieldID jawtSMgrID;

jobject reasonUnspecified;
jobject reasonConsole;
jobject reasonRemote;
jobject reasonLock;

extern jobject GetStaticObject(JNIEnv *env, jclass wfClass, const char *fieldName,
                        const char *signature);

extern BOOL isSuddenTerminationEnabled;

extern void DWMResetCompositionEnabled();

/************************************************************************
 * Utilities
 */

/* Initialize the Java VM instance variable when the library is
   first loaded */
JavaVM *jvm = NULL;

JNIEXPORT jint JNICALL
DEF_JNI_OnLoad(JavaVM *vm, void *reserved)
{
    TRY;

    jvm = vm;
    return JNI_VERSION_1_2;

    CATCH_BAD_ALLOC_RET(0);
}

extern "C" JNIEXPORT jboolean JNICALL AWTIsHeadless() {
    static JNIEnv *env = NULL;
    static jboolean isHeadless;
    jmethodID headlessFn;
    jclass graphicsEnvClass;

    if (env == NULL) {
        env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
        graphicsEnvClass = env->FindClass(
            "java/awt/GraphicsEnvironment");
        if (graphicsEnvClass == NULL) {
            return JNI_TRUE;
        }
        headlessFn = env->GetStaticMethodID(
            graphicsEnvClass, "isHeadless", "()Z");
        if (headlessFn == NULL) {
            return JNI_TRUE;
        }
        isHeadless = env->CallStaticBooleanMethod(graphicsEnvClass,
            headlessFn);
    }
    return isHeadless;
}

#define IDT_AWT_MOUSECHECK 0x101

static LPCTSTR szAwtToolkitClassName = TEXT("SunAwtToolkit");

static const int MOUSE_BUTTONS_WINDOWS_SUPPORTED = 5; //three standard buttons + XBUTTON1 + XBUTTON2.

UINT AwtToolkit::GetMouseKeyState()
{
    static BOOL mbSwapped = ::GetSystemMetrics(SM_SWAPBUTTON);
    UINT mouseKeyState = 0;

    if (HIBYTE(::GetKeyState(VK_CONTROL)))
        mouseKeyState |= MK_CONTROL;
    if (HIBYTE(::GetKeyState(VK_SHIFT)))
        mouseKeyState |= MK_SHIFT;
    if (HIBYTE(::GetKeyState(VK_LBUTTON)))
        mouseKeyState |= (mbSwapped ? MK_RBUTTON : MK_LBUTTON);
    if (HIBYTE(::GetKeyState(VK_RBUTTON)))
        mouseKeyState |= (mbSwapped ? MK_LBUTTON : MK_RBUTTON);
    if (HIBYTE(::GetKeyState(VK_MBUTTON)))
        mouseKeyState |= MK_MBUTTON;
    return mouseKeyState;
}

//
// Normal ::GetKeyboardState call only works if current thread has
// a message pump, so provide a way for other threads to get
// the keyboard state
//
void AwtToolkit::GetKeyboardState(PBYTE keyboardState)
{
    CriticalSection::Lock       l(AwtToolkit::GetInstance().m_lockKB);
    DASSERT(!IsBadWritePtr(keyboardState, KB_STATE_SIZE));
    memcpy(keyboardState, AwtToolkit::GetInstance().m_lastKeyboardState,
           KB_STATE_SIZE);
}

void AwtToolkit::SetBusy(BOOL busy) {

    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    static jclass awtAutoShutdownClass = NULL;
    static jmethodID notifyBusyMethodID = NULL;
    static jmethodID notifyFreeMethodID = NULL;

    if (awtAutoShutdownClass == NULL) {
        jclass awtAutoShutdownClassLocal = env->FindClass("sun/awt/AWTAutoShutdown");
        DASSERT(awtAutoShutdownClassLocal != NULL);
        if (!awtAutoShutdownClassLocal) throw std::bad_alloc();

        awtAutoShutdownClass = (jclass)env->NewGlobalRef(awtAutoShutdownClassLocal);
        env->DeleteLocalRef(awtAutoShutdownClassLocal);
        if (!awtAutoShutdownClass) throw std::bad_alloc();

        notifyBusyMethodID = env->GetStaticMethodID(awtAutoShutdownClass,
                                                    "notifyToolkitThreadBusy", "()V");
        DASSERT(notifyBusyMethodID != NULL);
        if (!notifyBusyMethodID) throw std::bad_alloc();

        notifyFreeMethodID = env->GetStaticMethodID(awtAutoShutdownClass,
                                                    "notifyToolkitThreadFree", "()V");
        DASSERT(notifyFreeMethodID != NULL);
        if (!notifyFreeMethodID) throw std::bad_alloc();
    } /* awtAutoShutdownClass == NULL*/

    if (busy) {
        env->CallStaticVoidMethod(awtAutoShutdownClass,
                                  notifyBusyMethodID);
    } else {
        env->CallStaticVoidMethod(awtAutoShutdownClass,
                                  notifyFreeMethodID);
    }

    if (!JNU_IsNull(env, safe_ExceptionOccurred(env))) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }
}

BOOL AwtToolkit::activateKeyboardLayout(HKL hkl) {
    // This call should succeed in case of one of the following:
    // 1. Win 9x
    // 2. NT with that HKL already loaded
    HKL prev = ::ActivateKeyboardLayout(hkl, 0);

    // If the above call fails, try loading the layout in case of NT
    if (!prev) {
        // create input locale string, e.g., "00000409", from hkl.
        TCHAR inputLocale[9];
        TCHAR buf[9];
        _tcscpy_s(inputLocale, 9, TEXT("00000000"));

    // 64-bit: ::LoadKeyboardLayout() is such a weird API - a string of
    // the hex value you want?!  Here we're converting our HKL value to
    // a string.  Hopefully there is no 64-bit trouble.
        _i64tot(reinterpret_cast<INT_PTR>(hkl), buf, 16);
        size_t len = _tcslen(buf);
        memcpy(&inputLocale[8-len], buf, len);

        // load and activate the keyboard layout
        hkl = ::LoadKeyboardLayout(inputLocale, 0);
        if (hkl != 0) {
            prev = ::ActivateKeyboardLayout(hkl, 0);
        }
    }

    return (prev != 0);
}

/************************************************************************
 * Exported functions
 */

extern "C" BOOL APIENTRY DllMain(HANDLE hInstance, DWORD ul_reason_for_call,
                                 LPVOID)
{
    // Don't use the TRY and CATCH_BAD_ALLOC_RET macros if we're detaching
    // the library. Doing so causes awt.dll to call back into the VM during
    // shutdown. This crashes the HotSpot VM.
    switch (ul_reason_for_call) {
    case DLL_PROCESS_ATTACH:
        TRY;
        AwtToolkit::GetInstance().SetModuleHandle((HMODULE)hInstance);
        CATCH_BAD_ALLOC_RET(FALSE);
        break;
    case DLL_PROCESS_DETACH:
#ifdef DEBUG
        DTrace_DisableMutex();
        DMem_DisableMutex();
#endif DEBUG
        break;
    }
    return TRUE;
}

/************************************************************************
 * AwtToolkit fields
 */

AwtToolkit AwtToolkit::theInstance;

/* ids for WToolkit fields accessed from native code */
jmethodID AwtToolkit::windowsSettingChangeMID;
jmethodID AwtToolkit::displayChangeMID;

jmethodID AwtToolkit::userSessionMID;
jmethodID AwtToolkit::systemSleepMID;
/* ids for Toolkit methods */
jmethodID AwtToolkit::getDefaultToolkitMID;
jmethodID AwtToolkit::getFontMetricsMID;
jmethodID AwtToolkit::insetsMID;

/************************************************************************
 * AwtToolkit methods
 */

AwtToolkit::AwtToolkit() {
    m_localPump = FALSE;
    m_mainThreadId = 0;
    m_toolkitHWnd = NULL;
    m_inputMethodHWnd = NULL;
    m_verbose = FALSE;
    m_isActive = TRUE;
    m_isDisposed = FALSE;

    m_vmSignalled = FALSE;

    m_isDynamicLayoutSet = FALSE;
    m_areExtraMouseButtonsEnabled = TRUE;

    m_isWin8OrLater = FALSE;
    m_touchKbrdAutoShowIsEnabled = FALSE;
    m_touchKbrdExeFilePath = NULL;
    m_pRegisterTouchWindow = NULL;
    m_pGetTouchInputInfo = NULL;
    m_pCloseTouchInputHandle = NULL;

    m_verifyComponents = FALSE;
    m_breakOnError = FALSE;

    m_breakMessageLoop = FALSE;
    m_messageLoopResult = 0;

    m_lastMouseOver = NULL;
    m_mouseDown = FALSE;

    m_hGetMessageHook = 0;
    m_hMouseLLHook = 0;
    m_lastWindowUnderMouse = NULL;
    m_timer = 0;

    m_cmdIDs = new AwtCmdIDList();
    m_pModalDialog = NULL;
    m_peer = NULL;
    m_dllHandle = NULL;

    m_displayChanged = FALSE;
    m_embedderProcessID = 0;

    // XXX: keyboard mapping should really be moved out of AwtComponent
    AwtComponent::InitDynamicKeyMapTable();

    // initialize kb state array
    ::GetKeyboardState(m_lastKeyboardState);

    m_waitEvent = ::CreateEvent(NULL, FALSE, FALSE, NULL);
    m_inputMethodWaitEvent = ::CreateEvent(NULL, FALSE, FALSE, NULL);
    isInDoDragDropLoop = FALSE;
    eventNumber = 0;
}

AwtToolkit::~AwtToolkit() {
/*
 *  The code has been moved to AwtToolkit::Dispose() method.
 */
}

HWND AwtToolkit::CreateToolkitWnd(LPCTSTR name)
{
    HWND hwnd = CreateWindow(
        szAwtToolkitClassName,
        (LPCTSTR)name,                    /* window name */
        WS_DISABLED,                      /* window style */
        -1, -1,                           /* position of window */
        0, 0,                             /* width and height */
        NULL, NULL,                       /* hWndParent and hWndMenu */
        GetModuleHandle(),
        NULL);                            /* lpParam */
    DASSERT(hwnd != NULL);
    return hwnd;
}

void AwtToolkit::InitTouchKeyboardExeFilePath() {
    enum RegistryView { WOW64_32BIT, WOW64_64BIT };
    const TCHAR tabTipCoKeyName[] = _T("SOFTWARE\\Classes\\CLSID\\")
        _T("{054AAE20-4BEA-4347-8A35-64A533254A9D}\\LocalServer32");
    HKEY hTabTipCoKey = NULL;
    RegistryView regViewWithTabTipCoKey = WOW64_32BIT;

    if (::RegOpenKeyEx(HKEY_LOCAL_MACHINE, tabTipCoKeyName, 0,
            KEY_READ | KEY_WOW64_32KEY, &hTabTipCoKey) != ERROR_SUCCESS) {
        if (::RegOpenKeyEx(HKEY_LOCAL_MACHINE, tabTipCoKeyName, 0,
                KEY_READ | KEY_WOW64_64KEY, &hTabTipCoKey) != ERROR_SUCCESS) {
            return;
        } else {
            regViewWithTabTipCoKey = WOW64_64BIT;
        }
    }

    DWORD keyValType = 0;
    DWORD bytesCopied = 0;
    if ((::RegQueryValueEx(hTabTipCoKey, NULL, NULL, &keyValType, NULL,
            &bytesCopied) != ERROR_SUCCESS) ||
        ((keyValType != REG_EXPAND_SZ) && (keyValType != REG_SZ))) {
        if (hTabTipCoKey != NULL) {
            ::RegCloseKey(hTabTipCoKey);
        }
        return;
    }

    // Increase the buffer size for 1 additional null-terminating character.
    bytesCopied += sizeof(TCHAR);
    TCHAR* tabTipFilePath = new TCHAR[bytesCopied / sizeof(TCHAR)];
    ::memset(tabTipFilePath, 0, bytesCopied);

    DWORD oldBytesCopied = bytesCopied;
    if (::RegQueryValueEx(hTabTipCoKey, NULL, NULL, NULL,
            (LPBYTE)tabTipFilePath, &bytesCopied) == ERROR_SUCCESS) {
        const TCHAR searchedStr[] = _T("%CommonProgramFiles%");
        const size_t searchedStrLen = ::_tcslen(searchedStr);
        int searchedStrStartIndex = -1;

        TCHAR* commonFilesDirPath = NULL;
        DWORD commonFilesDirPathLen = 0;

        // Check, if '%CommonProgramFiles%' string is present in the defined
        // path of the touch keyboard executable.
        TCHAR* const searchedStrStart = ::_tcsstr(tabTipFilePath, searchedStr);
        if (searchedStrStart != NULL) {
            searchedStrStartIndex = searchedStrStart - tabTipFilePath;

            // Get value of 'CommonProgramFiles' environment variable, if the
            // file path of the touch keyboard executable was found in 32-bit
            // registry view, otherwise get value of 'CommonProgramW6432'.
            const TCHAR envVar32BitName[] = _T("CommonProgramFiles");
            const TCHAR envVar64BitName[] = _T("CommonProgramW6432");
            const TCHAR* envVarName = (regViewWithTabTipCoKey == WOW64_32BIT ?
                envVar32BitName : envVar64BitName);

            DWORD charsStored = ::GetEnvironmentVariable(envVarName, NULL, 0);
            if (charsStored > 0) {
                commonFilesDirPath = new TCHAR[charsStored];
                ::memset(commonFilesDirPath, 0, charsStored * sizeof(TCHAR));

                DWORD oldCharsStored = charsStored;
                if (((charsStored = ::GetEnvironmentVariable(envVarName,
                        commonFilesDirPath, charsStored)) > 0) &&
                    (charsStored <= oldCharsStored)) {
                    commonFilesDirPathLen = charsStored;
                } else {
                    delete[] commonFilesDirPath;
                    commonFilesDirPath = NULL;
                }
            }
        }

        // Calculate 'm_touchKbrdExeFilePath' length in characters including
        // the null-terminating character.
        DWORD exeFilePathLen = oldBytesCopied / sizeof(TCHAR);
        if (commonFilesDirPathLen > 0) {
            exeFilePathLen = exeFilePathLen - searchedStrLen +
                commonFilesDirPathLen;
        }

        if (m_touchKbrdExeFilePath != NULL) {
            delete[] m_touchKbrdExeFilePath;
            m_touchKbrdExeFilePath = NULL;
        }
        m_touchKbrdExeFilePath = new TCHAR[exeFilePathLen];
        ::memset(m_touchKbrdExeFilePath, 0, exeFilePathLen * sizeof(TCHAR));

        if (commonFilesDirPathLen > 0) {
            ::_tcsncpy_s(m_touchKbrdExeFilePath, exeFilePathLen, tabTipFilePath,
                searchedStrStartIndex);
            DWORD charsCopied = searchedStrStartIndex;

            ::_tcsncpy_s(m_touchKbrdExeFilePath + charsCopied,
                exeFilePathLen - charsCopied, commonFilesDirPath,
                commonFilesDirPathLen);
            charsCopied += commonFilesDirPathLen;

            ::_tcsncpy_s(m_touchKbrdExeFilePath + charsCopied,
                exeFilePathLen - charsCopied, searchedStrStart + searchedStrLen,
                bytesCopied / sizeof(TCHAR) -
                    (searchedStrStartIndex + searchedStrLen));
        } else {
            ::_tcsncpy_s(m_touchKbrdExeFilePath, exeFilePathLen, tabTipFilePath,
                bytesCopied / sizeof(TCHAR));
        }

        // Remove leading and trailing quotation marks.
        ::StrTrim(m_touchKbrdExeFilePath, _T("\""));

        // Verify that a file with the path 'm_touchKbrdExeFilePath' exists.
        DWORD fileAttrs = ::GetFileAttributes(m_touchKbrdExeFilePath);
        DWORD err = ::GetLastError();
        if ((fileAttrs == INVALID_FILE_ATTRIBUTES) ||
            (fileAttrs & FILE_ATTRIBUTE_DIRECTORY)) {
            delete[] m_touchKbrdExeFilePath;
            m_touchKbrdExeFilePath = NULL;
        }

        if (commonFilesDirPath != NULL) {
            delete[] commonFilesDirPath;
        }
    }

    if (tabTipFilePath != NULL) {
        delete[] tabTipFilePath;
    }
    if (hTabTipCoKey != NULL) {
        ::RegCloseKey(hTabTipCoKey);
    }
}

HWND AwtToolkit::GetTouchKeyboardWindow() {
    const TCHAR wndClassName[] = _T("IPTip_Main_Window");
    HWND hwnd = ::FindWindow(wndClassName, NULL);
    if ((hwnd != NULL) && ::IsWindow(hwnd) && ::IsWindowEnabled(hwnd)) {
        return hwnd;
    }
    return NULL;
}


struct ToolkitThreadProc_Data {
    bool result;
    HANDLE hCompleted;

    jobject thread;
    jobject threadGroup;
};

void ToolkitThreadProc(void *param)
{
    ToolkitThreadProc_Data *data = (ToolkitThreadProc_Data *)param;

    bool bNotified = false;

    JNIEnv *env;
    JavaVMAttachArgs attachArgs;
    attachArgs.version  = JNI_VERSION_1_2;
    attachArgs.name     = "AWT-Windows";
    attachArgs.group    = data->threadGroup;

    jint res = jvm->AttachCurrentThreadAsDaemon((void **)&env, &attachArgs);
    if (res < 0) {
        return;
    }

    jobject thread = env->NewGlobalRef(data->thread);
    if (thread != NULL) {
        jclass cls = env->GetObjectClass(thread);
        if (cls != NULL) {
            jmethodID runId = env->GetMethodID(cls, "run", "()V");
            if (runId != NULL) {
                data->result = true;
                ::SetEvent(data->hCompleted);
                bNotified = true;

                env->CallVoidMethod(thread, runId);

                if (env->ExceptionCheck()) {
                    env->ExceptionDescribe();
                    env->ExceptionClear();
                    // TODO: handle
                }
            }
            env->DeleteLocalRef(cls);
        }
        env->DeleteGlobalRef(thread);
    }
    if (!bNotified) {
        ::SetEvent(data->hCompleted);
    }

    jvm->DetachCurrentThread();
}

/*
 * Class:     sun_awt_windows_WToolkit
 * Method:    startToolkitThread
 * Signature: (Ljava/lang/Runnable;Ljava/lang/ThreadGroup)Z
 */
JNIEXPORT jboolean JNICALL
Java_sun_awt_windows_WToolkit_startToolkitThread(JNIEnv *env, jclass cls, jobject thread, jobject threadGroup)
{
    AwtToolkit& tk = AwtToolkit::GetInstance();

    ToolkitThreadProc_Data data;
    data.result = false;
    data.thread = env->NewGlobalRef(thread);
    data.threadGroup = env->NewGlobalRef(threadGroup);
    if (data.thread == NULL || data.threadGroup == NULL) {
        return JNI_FALSE;
    }
    data.hCompleted = ::CreateEvent(NULL, FALSE, FALSE, NULL);

    bool result = tk.GetPreloadThread()
                    .InvokeAndTerminate(ToolkitThreadProc, &data);

    if (result) {
        ::WaitForSingleObject(data.hCompleted, INFINITE);
        result = data.result;
    } else {
        // no awt preloading
        // return back to the usual toolkit way
    }
    ::CloseHandle(data.hCompleted);

    env->DeleteGlobalRef(data.thread);
    env->DeleteGlobalRef(data.threadGroup);

    return result ? JNI_TRUE : JNI_FALSE;
}

BOOL AwtToolkit::Initialize(BOOL localPump) {
    AwtToolkit& tk = AwtToolkit::GetInstance();

    if (!tk.m_isActive || tk.m_mainThreadId != 0) {
        /* Already initialized. */
        return FALSE;
    }

    // This call is moved here from AwtToolkit constructor. Having it
    // there led to the bug 6480630: there could be a situation when
    // ComCtl32Util was constructed but not disposed
    ComCtl32Util::GetInstance().InitLibraries();

    if (!localPump) {
        // if preload thread was run, terminate it
        preloadThread.Terminate(true);
    }

    /* Register this toolkit's helper window */
    VERIFY(tk.RegisterClass() != NULL);

    // Set up operator new/malloc out of memory handler.
    NewHandler::init();

        //\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
        // Bugs 4032109, 4047966, and 4071991 to fix AWT
        //      crash in 16 color display mode.  16 color mode is supported.  Less
        //      than 16 color is not.
        // creighto@eng.sun.com 1997-10-07
        //
        // Check for at least 16 colors
    HDC hDC = ::GetDC(NULL);
        if ((::GetDeviceCaps(hDC, BITSPIXEL) * ::GetDeviceCaps(hDC, PLANES)) < 4) {
                ::MessageBox(NULL,
                             TEXT("Sorry, but this release of Java requires at least 16 colors"),
                             TEXT("AWT Initialization Error"),
                             MB_ICONHAND | MB_APPLMODAL);
                ::DeleteDC(hDC);
                JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
                JNU_ThrowByName(env, "java/lang/InternalError",
                                "unsupported screen depth");
                return FALSE;
        }
    ::ReleaseDC(NULL, hDC);
        ///////////////////////////////////////////////////////////////////////////

    tk.m_localPump = localPump;
    tk.m_mainThreadId = ::GetCurrentThreadId();

    /*
     * Create the one-and-only toolkit window.  This window isn't
     * displayed, but is used to route messages to this thread.
     */
    tk.m_toolkitHWnd = tk.CreateToolkitWnd(TEXT("theAwtToolkitWindow"));
    DASSERT(tk.m_toolkitHWnd != NULL);

    /*
     * Setup a GetMessage filter to watch all messages coming out of our
     * queue from PreProcessMsg().
     */
    tk.m_hGetMessageHook = ::SetWindowsHookEx(WH_GETMESSAGE,
                                              (HOOKPROC)GetMessageFilter,
                                              0, tk.m_mainThreadId);

    awt_dnd_initialize();

    /*
     * Initialization of the touch keyboard related variables.
     */
    tk.m_isWin8OrLater = IS_WIN8;

    TRY;

    JNIEnv* env = AwtToolkit::GetEnv();
    jclass sunToolkitCls = env->FindClass("sun/awt/SunToolkit");
    DASSERT(sunToolkitCls != 0);
    CHECK_NULL_RETURN(sunToolkitCls, FALSE);

    jmethodID isTouchKeyboardAutoShowEnabledMID = env->GetStaticMethodID(
        sunToolkitCls, "isTouchKeyboardAutoShowEnabled", "()Z");
    DASSERT(isTouchKeyboardAutoShowEnabledMID != 0);
    CHECK_NULL_RETURN(isTouchKeyboardAutoShowEnabledMID, FALSE);

    tk.m_touchKbrdAutoShowIsEnabled = env->CallStaticBooleanMethod(
        sunToolkitCls, isTouchKeyboardAutoShowEnabledMID);

    CATCH_BAD_ALLOC_RET(FALSE);

    if (tk.m_isWin8OrLater && tk.m_touchKbrdAutoShowIsEnabled) {
        tk.InitTouchKeyboardExeFilePath();
        HMODULE hUser32Dll = ::LoadLibrary(_T("user32.dll"));
        if (hUser32Dll != NULL) {
            tk.m_pRegisterTouchWindow = (RegisterTouchWindowFunc)
                ::GetProcAddress(hUser32Dll, "RegisterTouchWindow");
            tk.m_pGetTouchInputInfo = (GetTouchInputInfoFunc)
                ::GetProcAddress(hUser32Dll, "GetTouchInputInfo");
            tk.m_pCloseTouchInputHandle = (CloseTouchInputHandleFunc)
                ::GetProcAddress(hUser32Dll, "CloseTouchInputHandle");
        }

        if ((tk.m_pRegisterTouchWindow == NULL) ||
            (tk.m_pGetTouchInputInfo == NULL) ||
            (tk.m_pCloseTouchInputHandle == NULL)) {
            tk.m_pRegisterTouchWindow = NULL;
            tk.m_pGetTouchInputInfo = NULL;
            tk.m_pCloseTouchInputHandle = NULL;
        }
    }
    /*
     * End of the touch keyboard related initialization code.
     */

    return TRUE;
}

BOOL AwtToolkit::Dispose() {
    DTRACE_PRINTLN("In AwtToolkit::Dispose()");

    AwtToolkit& tk = AwtToolkit::GetInstance();

    if (!tk.m_isActive || tk.m_mainThreadId != ::GetCurrentThreadId()) {
        return FALSE;
    }

    tk.m_isActive = FALSE;

    // dispose Direct3D-related resources. This should be done
    // before AwtObjectList::Cleanup() as the d3d will attempt to
    // shutdown when the last of its windows is disposed of
    D3DInitializer::GetInstance().Clean();

    AwtObjectList::Cleanup();

    awt_dnd_uninitialize();
    awt_clipboard_uninitialize((JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2));

    if (tk.m_touchKbrdExeFilePath != NULL) {
        delete[] tk.m_touchKbrdExeFilePath;
        tk.m_touchKbrdExeFilePath = NULL;
    }
    tk.m_pRegisterTouchWindow = NULL;
    tk.m_pGetTouchInputInfo = NULL;
    tk.m_pCloseTouchInputHandle = NULL;

    if (tk.m_inputMethodHWnd != NULL) {
        ::SendMessage(tk.m_inputMethodHWnd, WM_IME_CONTROL, IMC_OPENSTATUSWINDOW, 0);
    }
    tk.m_inputMethodHWnd = NULL;

    // wait for any messages to be processed, in particular,
    // all WM_AWT_DELETEOBJECT messages that delete components; no
    // new messages will appear as all the windows except toolkit
    // window are unsubclassed and destroyed
    MSG msg;
    while (::GetMessage(&msg, NULL, 0, 0)) {
        ::TranslateMessage(&msg);
        ::DispatchMessage(&msg);
    }

    AwtFont::Cleanup();

    HWND toolkitHWndToDestroy = tk.m_toolkitHWnd;
    tk.m_toolkitHWnd = 0;
    VERIFY(::DestroyWindow(toolkitHWndToDestroy) != NULL);

    tk.UnregisterClass();

    ::UnhookWindowsHookEx(tk.m_hGetMessageHook);
    UninstallMouseLowLevelHook();

    tk.m_mainThreadId = 0;

    delete tk.m_cmdIDs;

    ::CloseHandle(m_waitEvent);
    ::CloseHandle(m_inputMethodWaitEvent);

    tk.m_isDisposed = TRUE;

    return TRUE;
}

void AwtToolkit::SetDynamicLayout(BOOL dynamic) {
    m_isDynamicLayoutSet = dynamic;
}

BOOL AwtToolkit::IsDynamicLayoutSet() {
    return m_isDynamicLayoutSet;
}

BOOL AwtToolkit::IsDynamicLayoutSupported() {
    // SPI_GETDRAGFULLWINDOWS is only supported on Win95 if
    // Windows Plus! is installed.  Otherwise, box frame resize.
    BOOL fullWindowDragEnabled = FALSE;
    int result = 0;
    result = ::SystemParametersInfo(SPI_GETDRAGFULLWINDOWS, 0,
                                  &fullWindowDragEnabled, 0);

    return (fullWindowDragEnabled && (result != 0));
}

BOOL AwtToolkit::IsDynamicLayoutActive() {
    return (IsDynamicLayoutSet() && IsDynamicLayoutSupported());
}

ATOM AwtToolkit::RegisterClass() {
    WNDCLASS  wc;

    wc.style         = 0;
    wc.lpfnWndProc   = (WNDPROC)WndProc;
    wc.cbClsExtra    = 0;
    wc.cbWndExtra    = 0;
    wc.hInstance     = AwtToolkit::GetInstance().GetModuleHandle(),
    wc.hIcon         = AwtToolkit::GetInstance().GetAwtIcon();
    wc.hCursor       = NULL;
    wc.hbrBackground = (HBRUSH)(COLOR_WINDOW+1);
    wc.lpszMenuName  = NULL;
    wc.lpszClassName = szAwtToolkitClassName;

    ATOM ret = ::RegisterClass(&wc);
    DASSERT(ret != NULL);
    return ret;
}

void AwtToolkit::UnregisterClass() {
    VERIFY(::UnregisterClass(szAwtToolkitClassName, AwtToolkit::GetInstance().GetModuleHandle()));
}

/*
 * Structure holding the information to create a component. This packet is
 * sent to the toolkit window.
 */
struct ComponentCreatePacket {
    void* hComponent;
    void* hParent;
    void (*factory)(void*, void*);
};

/*
 * Create an AwtXxxx component using a given factory function
 * Implemented by sending a message to the toolkit window to invoke the
 * factory function from that thread
 */
void AwtToolkit::CreateComponent(void* component, void* parent,
                                 ComponentFactory compFactory, BOOL isParentALocalReference)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    /* Since Local references are not valid in another Thread, we need to
       create a global reference before we send this to the Toolkit thread.
       In some cases this method is called with parent being a native
       malloced struct so we cannot and do not need to create a Global
       Reference from it. This is indicated by isParentALocalReference */

    jobject gcomponent = env->NewGlobalRef((jobject)component);
    jobject gparent;
    if (isParentALocalReference) gparent = env->NewGlobalRef((jobject)parent);
    ComponentCreatePacket ccp = { gcomponent,
                                  isParentALocalReference == TRUE ?  gparent : parent,
                                   compFactory };
    AwtToolkit::GetInstance().SendMessage(WM_AWT_COMPONENT_CREATE, 0,
                                          (LPARAM)&ccp);
    env->DeleteGlobalRef(gcomponent);
    if (isParentALocalReference) env->DeleteGlobalRef(gparent);
}

/*
 * Destroy an HWND that was created in the toolkit thread. Can be used on
 * Components and the toolkit window itself.
 */
void AwtToolkit::DestroyComponentHWND(HWND hwnd)
{
    if (!::IsWindow(hwnd)) {
        return;
    }

    AwtToolkit& tk = AwtToolkit::GetInstance();
    if ((tk.m_lastMouseOver != NULL) &&
        (tk.m_lastMouseOver->GetHWnd() == hwnd))
    {
        tk.m_lastMouseOver = NULL;
    }

    ::SetWindowLongPtr(hwnd, GWLP_USERDATA, (LONG_PTR)NULL);
    tk.SendMessage(WM_AWT_DESTROY_WINDOW, (WPARAM)hwnd, 0);
}

#ifndef SPY_MESSAGES
#define SpyWinMessage(hwin,msg,str)
#else
void SpyWinMessage(HWND hwnd, UINT message, LPCTSTR szComment);
#endif

/*
 * An AwtToolkit window is just a means of routing toolkit messages to here.
 */
LRESULT CALLBACK AwtToolkit::WndProc(HWND hWnd, UINT message,
                                     WPARAM wParam, LPARAM lParam)
{
    TRY;

    JNIEnv *env = GetEnv();
    JNILocalFrame lframe(env, 10);

    SpyWinMessage(hWnd, message, TEXT("AwtToolkit"));

    AwtToolkit::GetInstance().eventNumber++;
    /*
     * Awt widget creation messages are routed here so that all
     * widgets are created on the main thread.  Java allows widgets
     * to live beyond their creating thread -- by creating them on
     * the main thread, a widget can always be properly disposed.
     */
    switch (message) {
      case WM_AWT_EXECUTE_SYNC: {
          jobject peerObject = (jobject)wParam;
          AwtObject* object = (AwtObject *)JNI_GET_PDATA(peerObject);
          DASSERT( !IsBadReadPtr(object, sizeof(AwtObject)));
          AwtObject::ExecuteArgs *args = (AwtObject::ExecuteArgs *)lParam;
          DASSERT(!IsBadReadPtr(args, sizeof(AwtObject::ExecuteArgs)));
          LRESULT result = 0;
          if (object != NULL)
          {
              result = object->WinThreadExecProc(args);
          }
          env->DeleteGlobalRef(peerObject);
          return result;
      }
      case WM_AWT_COMPONENT_CREATE: {
          ComponentCreatePacket* ccp = (ComponentCreatePacket*)lParam;
          DASSERT(ccp->factory != NULL);
          DASSERT(ccp->hComponent != NULL);
          (*ccp->factory)(ccp->hComponent, ccp->hParent);
          return 0;
      }
      case WM_AWT_DESTROY_WINDOW: {
          /* Destroy widgets from this same thread that created them */
          VERIFY(::DestroyWindow((HWND)wParam) != NULL);
          return 0;
      }
      case WM_AWT_DISPOSE: {
          if(wParam != NULL) {
              jobject self = (jobject)wParam;
              AwtObject *o = (AwtObject *) JNI_GET_PDATA(self);
              env->DeleteGlobalRef(self);
              if(o != NULL && theAwtObjectList.Remove(o)) {
                  o->Dispose();
              }
          }
          return 0;
      }
      case WM_AWT_DISPOSEPDATA: {
          /*
           * NOTE: synchronization routine (like in WM_AWT_DISPOSE) was omitted because
           * this handler is called ONLY while disposing Cursor and Font objects where
           * synchronization takes place.
           */
          AwtObject *o = (AwtObject *) wParam;
          if(o != NULL && theAwtObjectList.Remove(o)) {
              o->Dispose();
          }
          return 0;
      }
      case WM_AWT_DELETEOBJECT: {
          AwtObject *p = (AwtObject *) wParam;
          if (p->CanBeDeleted()) {
              // all the messages for this component are processed, so
              // it can be deleted
              delete p;
          } else {
              // postpone deletion, waiting for all the messages for this
              // component to be processed
              AwtToolkit::GetInstance().PostMessage(WM_AWT_DELETEOBJECT, wParam, (LPARAM)0);
          }
          return 0;
      }
      case WM_AWT_OBJECTLISTCLEANUP: {
          AwtObjectList::Cleanup();
          return 0;
      }
      case WM_SYSCOLORCHANGE: {

          jclass systemColorClass = env->FindClass("java/awt/SystemColor");
          DASSERT(systemColorClass);
          if (!systemColorClass) throw std::bad_alloc();

          jmethodID mid = env->GetStaticMethodID(systemColorClass, "updateSystemColors", "()V");
          DASSERT(mid);
          if (!mid) throw std::bad_alloc();

          env->CallStaticVoidMethod(systemColorClass, mid);

          /* FALL THROUGH - NO BREAK */
      }

      case WM_SETTINGCHANGE: {
          AwtWin32GraphicsDevice::ResetAllMonitorInfo();
          /* FALL THROUGH - NO BREAK */
      }
// Remove this define when we move to newer (XP) version of SDK.
#define WM_THEMECHANGED                 0x031A
      case WM_THEMECHANGED: {
          /* Upcall to WToolkit when user changes configuration.
           *
           * NOTE: there is a bug in Windows 98 and some older versions of
           * Windows NT (it seems to be fixed in NT4 SP5) where no
           * WM_SETTINGCHANGE is sent when any of the properties under
           * Control Panel -> Display are changed.  You must _always_ query
           * the system for these - you can't rely on cached values.
           */
          jobject peer = AwtToolkit::GetInstance().m_peer;
          if (peer != NULL) {
              env->CallVoidMethod(peer, AwtToolkit::windowsSettingChangeMID);
          }
          return 0;
      }
#ifndef WM_DWMCOMPOSITIONCHANGED
#define WM_DWMCOMPOSITIONCHANGED        0x031E
#define WM_DWMNCRENDERINGCHANGED        0x031F
#define WM_DWMCOLORIZATIONCOLORCHANGED  0x0320
#define WM_DWMWINDOWMAXIMIZEDCHANGED    0x0321
#endif // WM_DWMCOMPOSITIONCHANGED
      case WM_DWMCOMPOSITIONCHANGED: {
          DWMResetCompositionEnabled();
          return 0;
      }

      case WM_TIMER: {
          // 6479820. Should check if a window is in manual resizing process: skip
          // sending any MouseExit/Enter events while inside resize-loop.
          // Note that window being in manual moving process could still
          // produce redundant enter/exit mouse events. In future, they can be
          // made skipped in a similar way.
           if (AwtWindow::IsResizing()) {
               return 0;
           }
          // Create an artifical MouseExit message if the mouse left to
          // a non-java window (bad mouse!)
          POINT pt;
          AwtToolkit& tk = AwtToolkit::GetInstance();
          if (::GetCursorPos(&pt)) {
              HWND hWndOver = ::WindowFromPoint(pt);
              AwtComponent * last_M;
              if ( AwtComponent::GetComponent(hWndOver) == NULL && tk.m_lastMouseOver != NULL ) {
                  last_M = tk.m_lastMouseOver;
                  // translate point from screen to target window
                  MapWindowPoints(HWND_DESKTOP, last_M->GetHWnd(), &pt, 1);
                  last_M->SendMessage(WM_AWT_MOUSEEXIT,
                                      GetMouseKeyState(),
                                      POINTTOPOINTS(pt));
                  tk.m_lastMouseOver = 0;
              }
          }
          if (tk.m_lastMouseOver == NULL && tk.m_timer != 0) {
              VERIFY(::KillTimer(tk.m_toolkitHWnd, tk.m_timer));
              tk.m_timer = 0;
          }
          return 0;
      }
      case WM_DESTROYCLIPBOARD: {
          if (!AwtClipboard::IsGettingOwnership())
              AwtClipboard::LostOwnership((JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2));
          return 0;
      }
      case WM_CLIPBOARDUPDATE: {
          AwtClipboard::WmClipboardUpdate((JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2));
          return 0;
      }
      case WM_AWT_LIST_SETMULTISELECT: {
          jobject peerObject = (jobject)wParam;
          AwtList* list = (AwtList *)JNI_GET_PDATA(peerObject);
          DASSERT( !IsBadReadPtr(list, sizeof(AwtObject)));
          list->SetMultiSelect(static_cast<BOOL>(lParam));
          return 0;
      }

      // Special awt message to call Imm APIs.
      // ImmXXXX() API must be used in the main thread.
      // In other thread these APIs does not work correctly even if
      // it returs with no error. (This restriction is not documented)
      // So we must use thse messages to call these APIs in main thread.
      case WM_AWT_CREATECONTEXT: {
          AwtToolkit& tk = AwtToolkit::GetInstance();
          tk.m_inputMethodData = reinterpret_cast<LRESULT>(
            reinterpret_cast<void*>(ImmCreateContext()));
          ::SetEvent(tk.m_inputMethodWaitEvent);
          return tk.m_inputMethodData;
      }
      case WM_AWT_DESTROYCONTEXT: {
          ImmDestroyContext((HIMC)wParam);
          AwtToolkit& tk = AwtToolkit::GetInstance();
          tk.m_inputMethodData = 0;
          ::SetEvent(tk.m_inputMethodWaitEvent);
          return 0;
      }
      case WM_AWT_ASSOCIATECONTEXT: {
          EnableNativeIMEStruct *data = (EnableNativeIMEStruct*)wParam;

          jobject peer = data->peer;
          jobject self = data->self;
          jint context = data->context;
          jboolean useNativeCompWindow = data->useNativeCompWindow;

          AwtComponent* comp = (AwtComponent*)JNI_GET_PDATA(peer);
          if (comp != NULL)
          {
              comp->SetInputMethod(self, useNativeCompWindow);
              comp->ImmAssociateContext((HIMC)((intptr_t)context));
          }

          if (peer != NULL) {
              env->DeleteGlobalRef(peer);
          }
          if (self != NULL) {
              env->DeleteGlobalRef(self);
          }

          delete data;
          AwtToolkit& tk = AwtToolkit::GetInstance();
          tk.m_inputMethodData = 0;
          ::SetEvent(tk.m_inputMethodWaitEvent);
          return 0;
      }
      case WM_AWT_GET_DEFAULT_IME_HANDLER: {
          LRESULT ret = (LRESULT)FALSE;
          jobject peer = (jobject)wParam;
          AwtToolkit& tk = AwtToolkit::GetInstance();

          AwtComponent* comp = (AwtComponent*)JNI_GET_PDATA(peer);
          if (comp != NULL) {
              HWND defaultIMEHandler = ImmGetDefaultIMEWnd(comp->GetHWnd());
              if (defaultIMEHandler != NULL) {
                  tk.SetInputMethodWindow(defaultIMEHandler);
                  ret = (LRESULT)TRUE;
              }
          }

          if (peer != NULL) {
              env->DeleteGlobalRef(peer);
          }
          tk.m_inputMethodData = ret;
          ::SetEvent(tk.m_inputMethodWaitEvent);
          return ret;
      }
      case WM_AWT_HANDLE_NATIVE_IME_EVENT: {
          jobject peer = (jobject)wParam;
          AwtComponent* comp = (AwtComponent*)JNI_GET_PDATA(peer);
          MSG* msg = (MSG*)lParam;

          long modifiers = comp->GetJavaModifiers();
          if ((comp != NULL) && (msg->message==WM_CHAR || msg->message==WM_SYSCHAR)) {
              WCHAR unicodeChar = (WCHAR)msg->wParam;
              comp->SendKeyEvent(java_awt_event_KeyEvent_KEY_TYPED,
                                 0, //to be fixed nowMillis(),
                                 java_awt_event_KeyEvent_CHAR_UNDEFINED,
                                 unicodeChar,
                                 modifiers,
                                 java_awt_event_KeyEvent_KEY_LOCATION_UNKNOWN, (jlong)0,
                                 msg);
          } else if (comp != NULL) {
              MSG* pCopiedMsg = new MSG;
              *pCopiedMsg = *msg;
              comp->SendMessage(WM_AWT_HANDLE_EVENT, (WPARAM) FALSE,
                                (LPARAM) pCopiedMsg);
          }

          if (peer != NULL) {
              env->DeleteGlobalRef(peer);
          }
          return 0;
      }
      case WM_AWT_ENDCOMPOSITION: {
          /*right now we just cancel the composition string
          may need to commit it in the furture
          Changed to commit it according to the flag 10/29/98*/
          ImmNotifyIME((HIMC)wParam, NI_COMPOSITIONSTR,
                       (lParam ? CPS_COMPLETE : CPS_CANCEL), 0);
          AwtToolkit& tk = AwtToolkit::GetInstance();
          tk.m_inputMethodData = 0;
          ::SetEvent(tk.m_inputMethodWaitEvent);
          return 0;
      }
      case WM_AWT_SETCONVERSIONSTATUS: {
          DWORD cmode;
          DWORD smode;
          ImmGetConversionStatus((HIMC)wParam, (LPDWORD)&cmode, (LPDWORD)&smode);
          ImmSetConversionStatus((HIMC)wParam, (DWORD)LOWORD(lParam), smode);
          AwtToolkit& tk = AwtToolkit::GetInstance();
          tk.m_inputMethodData = 0;
          ::SetEvent(tk.m_inputMethodWaitEvent);
          return 0;
      }
      case WM_AWT_GETCONVERSIONSTATUS: {
          DWORD cmode;
          DWORD smode;
          ImmGetConversionStatus((HIMC)wParam, (LPDWORD)&cmode, (LPDWORD)&smode);
          AwtToolkit& tk = AwtToolkit::GetInstance();
          tk.m_inputMethodData = cmode;
          ::SetEvent(tk.m_inputMethodWaitEvent);
          return cmode;
      }
      case WM_AWT_ACTIVATEKEYBOARDLAYOUT: {
          if (wParam && g_bUserHasChangedInputLang) {
              // Input language has been changed since the last WInputMethod.getNativeLocale()
              // call.  So let's honor the user's selection.
              // Note: we need to check this flag inside the toolkit thread to synchronize access
              // to the flag.
              return FALSE;
          }

          if (lParam == (LPARAM)::GetKeyboardLayout(0)) {
              // already active
              return FALSE;
          }

          // Since ActivateKeyboardLayout does not post WM_INPUTLANGCHANGEREQUEST,
          // we explicitly need to do the same thing here.
          static BYTE keyboardState[AwtToolkit::KB_STATE_SIZE];
          AwtToolkit::GetKeyboardState(keyboardState);
          WORD ignored;
          ::ToAscii(VK_SPACE, ::MapVirtualKey(VK_SPACE, 0),
                    keyboardState, &ignored, 0);

          return (LRESULT)activateKeyboardLayout((HKL)lParam);
      }
      case WM_AWT_OPENCANDIDATEWINDOW: {
          jobject peerObject = (jobject)wParam;
          AwtComponent* p = (AwtComponent*)JNI_GET_PDATA(peerObject);
          DASSERT( !IsBadReadPtr(p, sizeof(AwtObject)));
          // fix for 4805862: use GET_X_LPARAM and GET_Y_LPARAM macros
          // instead of LOWORD and HIWORD
          p->OpenCandidateWindow(GET_X_LPARAM(lParam), GET_Y_LPARAM(lParam));
          env->DeleteGlobalRef(peerObject);
          AwtToolkit& tk = AwtToolkit::GetInstance();
          tk.m_inputMethodData = 0;
          ::SetEvent(tk.m_inputMethodWaitEvent);
          return 0;
      }

      /*
       * send this message via ::SendMessage() and the MPT will acquire the
       * HANDLE synchronized with the sender's thread. The HANDLE must be
       * signalled or deadlock may occur between the MPT and the caller.
       */

      case WM_AWT_WAIT_FOR_SINGLE_OBJECT: {
        return ::WaitForSingleObject((HANDLE)lParam, INFINITE);
      }
      case WM_AWT_INVOKE_METHOD: {
        return (LRESULT)(*(void*(*)(void*))wParam)((void *)lParam);
      }
      case WM_AWT_INVOKE_VOID_METHOD: {
        return (LRESULT)(*(void*(*)(void))wParam)();
      }

      case WM_AWT_SETOPENSTATUS: {
          ImmSetOpenStatus((HIMC)wParam, (BOOL)lParam);
          AwtToolkit& tk = AwtToolkit::GetInstance();
          tk.m_inputMethodData = 0;
          ::SetEvent(tk.m_inputMethodWaitEvent);
          return 0;
      }
      case WM_AWT_GETOPENSTATUS: {
          AwtToolkit& tk = AwtToolkit::GetInstance();
          tk.m_inputMethodData = (DWORD)ImmGetOpenStatus((HIMC)wParam);
          ::SetEvent(tk.m_inputMethodWaitEvent);
          return tk.m_inputMethodData;
      }
      case WM_DISPLAYCHANGE: {
          // Reinitialize screens
          initScreens(env);

          // Notify Java side - call WToolkit.displayChanged()
          jclass clazz = env->FindClass("sun/awt/windows/WToolkit");
          DASSERT(clazz != NULL);
          if (!clazz) throw std::bad_alloc();
          env->CallStaticVoidMethod(clazz, AwtToolkit::displayChangeMID);

          GetInstance().m_displayChanged = TRUE;

          ::PostMessage(HWND_BROADCAST, WM_PALETTEISCHANGING, NULL, NULL);
          break;
      }
      case WM_AWT_SETCURSOR: {
          ::SetCursor((HCURSOR)wParam);
          return TRUE;
      }
      /* Session management */
      case WM_QUERYENDSESSION: {
          /* Shut down cleanly */
          if (!isSuddenTerminationEnabled) {
              return FALSE;
          }
          if (JVM_RaiseSignal(SIGTERM)) {
              AwtToolkit::GetInstance().m_vmSignalled = TRUE;
          }
          return TRUE;
      }
      case WM_ENDSESSION: {
          // Keep pumping messages until the shutdown sequence halts the VM,
          // or we exit the MessageLoop because of a WM_QUIT message
          AwtToolkit& tk = AwtToolkit::GetInstance();

          // if WM_QUERYENDSESSION hasn't successfully raised SIGTERM
          // we ignore the ENDSESSION message
          if (!tk.m_vmSignalled) {
              return 0;
          }
          tk.MessageLoop(AwtToolkit::PrimaryIdleFunc,
                         AwtToolkit::CommonPeekMessageFunc);

          // Dispose here instead of in eventLoop so that we don't have
          // to return from the WM_ENDSESSION handler.
          tk.Dispose();

          // Never return. The VM will halt the process.
          hang_if_shutdown();

          // Should never get here.
          DASSERT(FALSE);
          break;
      }
#ifndef WM_WTSSESSION_CHANGE
#define WM_WTSSESSION_CHANGE        0x02B1
#define WTS_CONSOLE_CONNECT 0x1
#define WTS_CONSOLE_DISCONNECT 0x2
#define WTS_REMOTE_CONNECT 0x3
#define WTS_REMOTE_DISCONNECT 0x4
#define WTS_SESSION_LOGON 0x5
#define WTS_SESSION_LOGOFF 0x6
#define WTS_SESSION_LOCK 0x7
#define WTS_SESSION_UNLOCK 0x8
#define WTS_SESSION_REMOTE_CONTROL 0x9
#endif // WM_WTSSESSION_CHANGE
      case WM_WTSSESSION_CHANGE: {
          jclass clzz = env->FindClass("sun/awt/windows/WDesktopPeer");
          DASSERT(clzz != NULL);
          if (!clzz) throw std::bad_alloc();

          if (wParam == WTS_CONSOLE_CONNECT
                || wParam == WTS_CONSOLE_DISCONNECT
                || wParam == WTS_REMOTE_CONNECT
                || wParam == WTS_REMOTE_DISCONNECT
                || wParam == WTS_SESSION_UNLOCK
                || wParam == WTS_SESSION_LOCK) {

              BOOL activate = wParam == WTS_CONSOLE_CONNECT
                                || wParam == WTS_REMOTE_CONNECT
                                || wParam == WTS_SESSION_UNLOCK;
              jobject reason = reasonUnspecified;

              switch (wParam) {
                  case WTS_CONSOLE_CONNECT:
                  case WTS_CONSOLE_DISCONNECT:
                      reason = reasonConsole;
                      break;
                  case WTS_REMOTE_CONNECT:
                  case WTS_REMOTE_DISCONNECT:
                      reason = reasonRemote;
                      break;
                  case WTS_SESSION_UNLOCK:
                  case WTS_SESSION_LOCK:
                      reason = reasonLock;
              }

              env->CallStaticVoidMethod(clzz, AwtToolkit::userSessionMID,
                                              activate
                                              ? JNI_TRUE
                                              : JNI_FALSE, reason);
          }
          break;
      }
      case WM_POWERBROADCAST: {
          jclass clzz = env->FindClass("sun/awt/windows/WDesktopPeer");
          DASSERT(clzz != NULL);
          if (!clzz) throw std::bad_alloc();

          if (wParam == PBT_APMSUSPEND || wParam == PBT_APMRESUMEAUTOMATIC) {
              env->CallStaticVoidMethod(clzz, AwtToolkit::systemSleepMID,
                                            wParam == PBT_APMRESUMEAUTOMATIC
                                                ? JNI_TRUE
                                                : JNI_FALSE);
          }
          break;
      }
      case WM_SYNC_WAIT:
          SetEvent(AwtToolkit::GetInstance().m_waitEvent);
          break;
    }

    return DefWindowProc(hWnd, message, wParam, lParam);

    CATCH_BAD_ALLOC_RET(0);
}

LRESULT CALLBACK AwtToolkit::GetMessageFilter(int code,
                                              WPARAM wParam, LPARAM lParam)
{
    TRY;

    if (code >= 0 && wParam == PM_REMOVE && lParam != 0) {
       if (AwtToolkit::GetInstance().PreProcessMsg(*(MSG*)lParam) !=
               mrPassAlong) {
           /* PreProcessMsg() wants us to eat it */
           ((MSG*)lParam)->message = WM_NULL;
       }
    }
    return ::CallNextHookEx(AwtToolkit::GetInstance().m_hGetMessageHook, code,
                            wParam, lParam);

    CATCH_BAD_ALLOC_RET(0);
}

void AwtToolkit::InstallMouseLowLevelHook()
{
    // We need the low-level hook since we need to process mouse move
    // messages outside of our windows.
    m_hMouseLLHook = ::SetWindowsHookEx(WH_MOUSE_LL,
            (HOOKPROC)MouseLowLevelHook,
            GetModuleHandle(), NULL);

    // Reset the old value
    m_lastWindowUnderMouse = NULL;
}

void AwtToolkit::UninstallMouseLowLevelHook()
{
    if (m_hMouseLLHook != 0) {
        ::UnhookWindowsHookEx(m_hMouseLLHook);
        m_hMouseLLHook = 0;
    }
}

LRESULT CALLBACK AwtToolkit::MouseLowLevelHook(int code,
        WPARAM wParam, LPARAM lParam)
{
    TRY;

    if (code >= 0 && wParam == WM_MOUSEMOVE) {
        POINT pt = ((MSLLHOOKSTRUCT*)lParam)->pt;

        // We can't use GA_ROOTOWNER since in this case we'll go up to
        // the root Java toplevel, not the actual owned toplevel.
        HWND hwnd = ::GetAncestor(::WindowFromPoint(pt), GA_ROOT);

        AwtToolkit& tk = AwtToolkit::GetInstance();

        if (tk.m_lastWindowUnderMouse != hwnd) {
            AwtWindow *fw = NULL, *tw = NULL;

            if (tk.m_lastWindowUnderMouse) {
                fw = (AwtWindow*)
                    AwtComponent::GetComponent(tk.m_lastWindowUnderMouse);
            }
            if (hwnd) {
                tw = (AwtWindow*)AwtComponent::GetComponent(hwnd);
            }

            tk.m_lastWindowUnderMouse = hwnd;

            if (fw) {
                fw->UpdateSecurityWarningVisibility();
            }
            // ... however, because we use GA_ROOT, we may find the warningIcon
            // which is not a Java windows.
            if (AwtWindow::IsWarningWindow(hwnd)) {
                hwnd = ::GetParent(hwnd);
                if (hwnd) {
                    tw = (AwtWindow*)AwtComponent::GetComponent(hwnd);
                }
                tk.m_lastWindowUnderMouse = hwnd;
            }
            if (tw) {
                tw->UpdateSecurityWarningVisibility();
            }


        }
    }

    return ::CallNextHookEx(AwtToolkit::GetInstance().m_hMouseLLHook, code,
            wParam, lParam);

    CATCH_BAD_ALLOC_RET(0);
}

/*
 * The main message loop
 */

const int AwtToolkit::EXIT_ENCLOSING_LOOP      = 0;
const int AwtToolkit::EXIT_ALL_ENCLOSING_LOOPS = -1;


/**
 * Called upon event idle to ensure that we have released any
 * CriticalSections that we took during window event processing.
 *
 * Note that this gets used more often than you would think; some
 * window moves actually happen over more than one event burst.  So,
 * for example, we might get a WINDOWPOSCHANGING event, then we
 * idle and release the lock here, then eventually we get the
 * WINDOWPOSCHANGED event.
 *
 * This method may be called from WToolkit.embeddedEventLoopIdleProcessing
 * if there is a separate event loop that must do the same CriticalSection
 * check.
 *
 * See bug #4526587 for more information.
 */
void VerifyWindowMoveLockReleased()
{
    if (windowMoveLockHeld) {
        windowMoveLockHeld = FALSE;
        windowMoveLock.Leave();
    }
}

UINT
AwtToolkit::MessageLoop(IDLEPROC lpIdleFunc,
                        PEEKMESSAGEPROC lpPeekMessageFunc)
{
    DTRACE_PRINTLN("AWT event loop started");

    DASSERT(lpIdleFunc != NULL);
    DASSERT(lpPeekMessageFunc != NULL);

    m_messageLoopResult = 0;
    while (!m_breakMessageLoop) {

        (*lpIdleFunc)();

        PumpWaitingMessages(lpPeekMessageFunc); /* pumps waiting messages */

        // Catch problems with windowMoveLock critical section.  In case we
        // misunderstood the way windows processes window move/resize
        // events, we don't want to hold onto the windowMoveLock CS forever.
        // If we've finished processing events for now, release the lock
        // if held.
        VerifyWindowMoveLockReleased();
    }
    if (m_messageLoopResult == EXIT_ALL_ENCLOSING_LOOPS)
        ::PostQuitMessage(EXIT_ALL_ENCLOSING_LOOPS);
    m_breakMessageLoop = FALSE;

    DTRACE_PRINTLN("AWT event loop ended");

    return m_messageLoopResult;
}

/*
 * Exit the enclosing message loop(s).
 *
 * The message will be ignored if Windows is currently is in an internal
 * message loop (such as a scroll bar drag). So we first send IDCANCEL and
 * WM_CANCELMODE messages to every Window on the thread.
 */
static BOOL CALLBACK CancelAllThreadWindows(HWND hWnd, LPARAM)
{
    TRY;

    ::SendMessage(hWnd, WM_COMMAND, MAKEWPARAM(IDCANCEL, 0), (LPARAM)hWnd);
    ::SendMessage(hWnd, WM_CANCELMODE, 0, 0);

    return TRUE;

    CATCH_BAD_ALLOC_RET(FALSE);
}

static void DoQuitMessageLoop(void* param) {
    int status = *static_cast<int*>(param);

    AwtToolkit::GetInstance().QuitMessageLoop(status);
}

void AwtToolkit::QuitMessageLoop(int status) {
    /*
     * Fix for 4623377.
     * Reinvoke QuitMessageLoop on the toolkit thread, so that
     * m_breakMessageLoop is accessed on a single thread.
     */
    if (!AwtToolkit::IsMainThread()) {
        InvokeFunction(DoQuitMessageLoop, &status);
        return;
    }

    /*
     * Fix for BugTraq ID 4445747.
     * EnumThreadWindows() is very slow during dnd on Win9X/ME.
     * This call is unnecessary during dnd, since we postpone processing of all
     * messages that can enter internal message loop until dnd is over.
     */
      if (status == EXIT_ALL_ENCLOSING_LOOPS) {
          ::EnumThreadWindows(MainThread(), (WNDENUMPROC)CancelAllThreadWindows,
                              0);
      }

    /*
     * Fix for 4623377.
     * Modal loop may not exit immediatelly after WM_CANCELMODE, so it still can
     * eat WM_QUIT message and the nested message loop will never exit.
     * The fix is to use AwtToolkit instance variables instead of WM_QUIT to
     * guarantee that we exit from the nested message loop when any possible
     * modal loop quits. In this case CancelAllThreadWindows is needed only to
     * ensure that the nested message loop exits quickly and doesn't wait until
     * a possible modal loop completes.
     */
    m_breakMessageLoop = TRUE;
    m_messageLoopResult = status;

    /*
     * Fix for 4683602.
     * Post an empty message, to wake up the toolkit thread
     * if it is currently in WaitMessage(),
     */
    PostMessage(WM_NULL);
}

/*
 * Called by the message loop to pump the message queue when there are
 * messages waiting. Can also be called anywhere to pump messages.
 */
BOOL AwtToolkit::PumpWaitingMessages(PEEKMESSAGEPROC lpPeekMessageFunc)
{
    MSG  msg;
    BOOL foundOne = FALSE;

    DASSERT(lpPeekMessageFunc != NULL);

    while (!m_breakMessageLoop && (*lpPeekMessageFunc)(msg)) {
        foundOne = TRUE;
        ProcessMsg(msg);
    }
    return foundOne;
}

void AwtToolkit::PumpToDestroy(class AwtComponent* p)
{
    MSG  msg;

    DASSERT(AwtToolkit::PrimaryIdleFunc != NULL);
    DASSERT(AwtToolkit::CommonPeekMessageFunc != NULL);

    while (p->IsDestroyPaused() && !m_breakMessageLoop) {

        PrimaryIdleFunc();

        while (p->IsDestroyPaused() && !m_breakMessageLoop && CommonPeekMessageFunc(msg)) {
            ProcessMsg(msg);
        }
    }
}

void AwtToolkit::ProcessMsg(MSG& msg)
{
    if (msg.message == WM_QUIT) {
        m_breakMessageLoop = TRUE;
        m_messageLoopResult = static_cast<UINT>(msg.wParam);
        if (m_messageLoopResult == EXIT_ALL_ENCLOSING_LOOPS)
            ::PostQuitMessage(static_cast<int>(msg.wParam));  // make sure all loops exit
    }
    else if (msg.message != WM_NULL) {
        /*
        * The AWT in standalone mode (that is, dynamically loaded from the
        * Java VM) doesn't have any translation tables to worry about, so
        * TranslateAccelerator isn't called.
        */

        ::TranslateMessage(&msg);
        ::DispatchMessage(&msg);
    }
}

VOID CALLBACK
AwtToolkit::PrimaryIdleFunc() {
    AwtToolkit::SetBusy(FALSE);
    ::WaitMessage();               /* allow system to go idle */
    AwtToolkit::SetBusy(TRUE);
}

VOID CALLBACK
AwtToolkit::SecondaryIdleFunc() {
    ::WaitMessage();               /* allow system to go idle */
}

BOOL
AwtToolkit::CommonPeekMessageFunc(MSG& msg) {
    return ::PeekMessage(&msg, NULL, 0, 0, PM_REMOVE);
}

/*
 * Perform pre-processing on a message before it is translated &
 * dispatched.  Returns true to eat the message
 */
BOOL AwtToolkit::PreProcessMsg(MSG& msg)
{
    /*
     * Offer preprocessing first to the target component, then call out to
     * specific mouse and key preprocessor methods
     */
    AwtComponent* p = AwtComponent::GetComponent(msg.hwnd);
    if (p && p->PreProcessMsg(msg) == mrConsume)
        return TRUE;

    if ((msg.message >= WM_MOUSEFIRST && msg.message <= WM_MOUSELAST) ||
        (msg.message >= WM_NCMOUSEMOVE && msg.message <= WM_NCMBUTTONDBLCLK)) {
        if (PreProcessMouseMsg(p, msg)) {
            return TRUE;
        }
    }
    else if (msg.message >= WM_KEYFIRST && msg.message <= WM_KEYLAST) {
        if (PreProcessKeyMsg(p, msg))
            return TRUE;
    }
    return FALSE;
}

BOOL AwtToolkit::PreProcessMouseMsg(AwtComponent* p, MSG& msg)
{
    WPARAM mouseWParam;
    LPARAM mouseLParam;

    /*
     * Fix for BugTraq ID 4395290.
     * Do not synthesize mouse enter/exit events during drag-and-drop,
     * since it messes up LightweightDispatcher.
     */
    if (AwtDropTarget::IsLocalDnD()) {
        return FALSE;
    }

    if (msg.message >= WM_MOUSEFIRST && msg.message <= WM_MOUSELAST) {
        mouseWParam = msg.wParam;
        mouseLParam = msg.lParam;
    } else {
        mouseWParam = GetMouseKeyState();
    }

    /*
     * Get the window under the mouse, as it will be different if its
     * captured.
     */
    DWORD dwCurPos = ::GetMessagePos();
    DWORD dwScreenPos = dwCurPos;
    POINT curPos;
    // fix for 4805862
    // According to MSDN: do not use LOWORD and HIWORD macros to extract x and
    // y coordinates because these macros return incorrect results on systems
    // with multiple monitors (signed values are treated as unsigned)
    curPos.x = GET_X_LPARAM(dwCurPos);
    curPos.y = GET_Y_LPARAM(dwCurPos);
    HWND hWndFromPoint = ::WindowFromPoint(curPos);
    // hWndFromPoint == 0 if mouse is over a scrollbar
    AwtComponent* mouseComp =
        AwtComponent::GetComponent(hWndFromPoint);
    // Need extra copies for non-client area issues
    HWND hWndForWheel = hWndFromPoint;

    // If the point under the mouse isn't in the client area,
    // ignore it to maintain compatibility with Solaris (#4095172)
    RECT windowRect;
    ::GetClientRect(hWndFromPoint, &windowRect);
    POINT topLeft;
    topLeft.x = 0;
    topLeft.y = 0;
    ::ClientToScreen(hWndFromPoint, &topLeft);
    windowRect.top += topLeft.y;
    windowRect.bottom += topLeft.y;
    windowRect.left += topLeft.x;
    windowRect.right += topLeft.x;
    if ((curPos.y < windowRect.top) ||
        (curPos.y >= windowRect.bottom) ||
        (curPos.x < windowRect.left) ||
        (curPos.x >= windowRect.right)) {
        mouseComp = NULL;
        hWndFromPoint = NULL;
    }

    /*
     * Look for mouse transitions between windows & create
     * MouseExit & MouseEnter messages
     */
    // 6479820. Should check if a window is in manual resizing process: skip
    // sending any MouseExit/Enter events while inside resize-loop.
    // Note that window being in manual moving process could still
    // produce redundant enter/exit mouse events. In future, they can be
    // made skipped in a similar way.
    if (mouseComp != m_lastMouseOver && !AwtWindow::IsResizing()) {
        /*
         * Send the messages right to the windows so that they are in
         * the right sequence.
         */
        if (m_lastMouseOver) {
            dwCurPos = dwScreenPos;
            curPos.x = LOWORD(dwCurPos);
            curPos.y = HIWORD(dwCurPos);
            ::MapWindowPoints(HWND_DESKTOP, m_lastMouseOver->GetHWnd(),
                              &curPos, 1);
            mouseLParam = MAKELPARAM((WORD)curPos.x, (WORD)curPos.y);
            m_lastMouseOver->SendMessage(WM_AWT_MOUSEEXIT, mouseWParam,
                                         mouseLParam);
        }
        if (mouseComp) {
                dwCurPos = dwScreenPos;
                curPos.x = LOWORD(dwCurPos);
                curPos.y = HIWORD(dwCurPos);
                ::MapWindowPoints(HWND_DESKTOP, mouseComp->GetHWnd(),
                                  &curPos, 1);
                mouseLParam = MAKELPARAM((WORD)curPos.x, (WORD)curPos.y);
            mouseComp->SendMessage(WM_AWT_MOUSEENTER, mouseWParam,
                                   mouseLParam);
        }
        m_lastMouseOver = mouseComp;
    }

    /*
     * For MouseWheelEvents, hwnd must be changed to be the Component under
     * the mouse, not the Component with the input focus.
     */

    if (msg.message == WM_MOUSEWHEEL || msg.message == WM_MOUSEHWHEEL) {
            //i.e. mouse is over client area for this window
            DWORD hWndForWheelProcess;
            DWORD hWndForWheelThread = ::GetWindowThreadProcessId(hWndForWheel, &hWndForWheelProcess);
            if (::GetCurrentProcessId() == hWndForWheelProcess) {
                if (AwtToolkit::MainThread() == hWndForWheelThread) {
                    msg.hwnd = hWndForWheel;
                } else {
                    // Interop mode, redispatch the event to another toolkit.
                    ::SendMessage(hWndForWheel, msg.message, mouseWParam, mouseLParam);
                    return TRUE;
                }
            }
    }

    /*
     * Make sure we get at least one last chance to check for transitions
     * before we sleep
     */
    if (m_lastMouseOver && !m_timer) {
        m_timer = ::SetTimer(m_toolkitHWnd, IDT_AWT_MOUSECHECK, 200, 0);
    }
    return FALSE;  /* Now go ahead and process current message as usual */
}

BOOL AwtToolkit::PreProcessKeyMsg(AwtComponent* p, MSG& msg)
{
    // get keyboard state for use in AwtToolkit::GetKeyboardState
    CriticalSection::Lock       l(m_lockKB);
    ::GetKeyboardState(m_lastKeyboardState);
    return FALSE;
}

void *AwtToolkit::SyncCall(void *(*ftn)(void *), void *param) {
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    if (!IsMainThread()) {
        CriticalSection::Lock l(GetSyncCS());
        return (*ftn)(param);
    } else {
        return (*ftn)(param);
    }
}

void AwtToolkit::SyncCall(void (*ftn)(void *), void *param) {
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    if (!IsMainThread()) {
        CriticalSection::Lock l(GetSyncCS());
        (*ftn)(param);
    } else {
        (*ftn)(param);
    }
}

void *AwtToolkit::SyncCall(void *(*ftn)(void)) {
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    if (!IsMainThread()) {
        CriticalSection::Lock l(GetSyncCS());
        return (*ftn)();
    } else {
        return (*ftn)();
    }
}

void AwtToolkit::SyncCall(void (*ftn)(void)) {
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    if (!IsMainThread()) {
        CriticalSection::Lock l(GetSyncCS());
        (*ftn)();
    } else {
        (*ftn)();
    }
}

jboolean AwtToolkit::isFreeIDAvailable()
{
    return m_cmdIDs->isFreeIDAvailable();
}

UINT AwtToolkit::CreateCmdID(AwtObject* object)
{
    return m_cmdIDs->Add(object);
}

void AwtToolkit::RemoveCmdID(UINT id)
{
    m_cmdIDs->Remove(id);
}

AwtObject* AwtToolkit::LookupCmdID(UINT id)
{
    return m_cmdIDs->Lookup(id);
}

HICON AwtToolkit::GetAwtIcon()
{
    return ::LoadIcon(GetModuleHandle(), TEXT("AWT_ICON"));
}

HICON AwtToolkit::GetAwtIconSm()
{
    static HICON defaultIconSm = NULL;
    static int prevSmx = 0;
    static int prevSmy = 0;

    int smx = GetSystemMetrics(SM_CXSMICON);
    int smy = GetSystemMetrics(SM_CYSMICON);

    // Fixed 6364216: LoadImage() may leak memory
    if (defaultIconSm == NULL || smx != prevSmx || smy != prevSmy) {
        defaultIconSm = (HICON)LoadImage(GetModuleHandle(), TEXT("AWT_ICON"), IMAGE_ICON, smx, smy, 0);
        prevSmx = smx;
        prevSmy = smy;
    }
    return defaultIconSm;
}

// The icon at index 0 must be gray. See AwtWindow::GetSecurityWarningIcon()
HICON AwtToolkit::GetSecurityWarningIcon(UINT index, UINT w, UINT h)
{
    //Note: should not exceed 10 because of the current implementation.
    static const int securityWarningIconCounter = 3;

    static HICON securityWarningIcon[securityWarningIconCounter]      = {NULL, NULL, NULL};;
    static UINT securityWarningIconWidth[securityWarningIconCounter]  = {0, 0, 0};
    static UINT securityWarningIconHeight[securityWarningIconCounter] = {0, 0, 0};

    index = AwtToolkit::CalculateWave(index, securityWarningIconCounter);

    if (securityWarningIcon[index] == NULL ||
            w != securityWarningIconWidth[index] ||
            h != securityWarningIconHeight[index])
    {
        if (securityWarningIcon[index] != NULL)
        {
            ::DestroyIcon(securityWarningIcon[index]);
        }

        static const wchar_t securityWarningIconName[] = L"SECURITY_WARNING_";
        wchar_t iconResourceName[sizeof(securityWarningIconName) + 2];
        ::ZeroMemory(iconResourceName, sizeof(iconResourceName));
        wcscpy(iconResourceName, securityWarningIconName);

        wchar_t strIndex[2];
        ::ZeroMemory(strIndex, sizeof(strIndex));
        strIndex[0] = L'0' + index;

        wcscat(iconResourceName, strIndex);

        securityWarningIcon[index] = (HICON)::LoadImage(GetModuleHandle(),
                iconResourceName,
                IMAGE_ICON, w, h, LR_DEFAULTCOLOR);
        securityWarningIconWidth[index] = w;
        securityWarningIconHeight[index] = h;
    }

    return securityWarningIcon[index];
}

void AwtToolkit::SetHeapCheck(long flag) {
    if (flag) {
        printf("heap checking not supported with this build\n");
    }
}

void throw_if_shutdown(void) throw (awt_toolkit_shutdown)
{
    AwtToolkit::GetInstance().VerifyActive();
}
void hang_if_shutdown(void)
{
    try {
        AwtToolkit::GetInstance().VerifyActive();
    } catch (awt_toolkit_shutdown&) {
        // Never return. The VM will halt the process.
        ::WaitForSingleObject(::CreateEvent(NULL, TRUE, FALSE, NULL),
                              INFINITE);
        // Should never get here.
        DASSERT(FALSE);
    }
}

// for now we support only one embedder, but should be ready for future
void AwtToolkit::RegisterEmbedderProcessId(HWND embedder)
{
    if (m_embedderProcessID) {
        // we already set embedder process and do not expect
        // two different processes to embed the same AwtToolkit
        return;
    }

    embedder = ::GetAncestor(embedder, GA_ROOT);
    ::GetWindowThreadProcessId(embedder, &m_embedderProcessID);
}

JNIEnv* AwtToolkit::m_env;
DWORD AwtToolkit::m_threadId;

void AwtToolkit::SetEnv(JNIEnv *env) {
    if (m_env != NULL) { // If already cashed (by means of embeddedInit() call).
        return;
    }
    m_threadId = GetCurrentThreadId();
    m_env = env;
}

JNIEnv* AwtToolkit::GetEnv() {
    return (m_env == NULL || m_threadId != GetCurrentThreadId()) ?
        (JNIEnv*)JNU_GetEnv(jvm, JNI_VERSION_1_2) : m_env;
}

BOOL AwtToolkit::GetScreenInsets(int screenNum, RECT * rect)
{
    /* if primary display */
    if (screenNum == 0) {
        RECT rRW;
        if (::SystemParametersInfo(SPI_GETWORKAREA,0,(void *) &rRW,0) == TRUE) {
            rect->top = rRW.top;
            rect->left = rRW.left;
            rect->bottom = ::GetSystemMetrics(SM_CYSCREEN) - rRW.bottom;
            rect->right = ::GetSystemMetrics(SM_CXSCREEN) - rRW.right;
            return TRUE;
        }
    }
    /* if additional display */
    else {
        MONITORINFO *miInfo;
        miInfo = AwtWin32GraphicsDevice::GetMonitorInfo(screenNum);
        if (miInfo) {
            rect->top = miInfo->rcWork.top    - miInfo->rcMonitor.top;
            rect->left = miInfo->rcWork.left   - miInfo->rcMonitor.left;
            rect->bottom = miInfo->rcMonitor.bottom - miInfo->rcWork.bottom;
            rect->right = miInfo->rcMonitor.right - miInfo->rcWork.right;
            return TRUE;
        }
    }
    return FALSE;
}


void AwtToolkit::GetWindowRect(HWND hWnd, LPRECT lpRect)
{
    try {
        if (S_OK == DwmAPI::DwmGetWindowAttribute(hWnd,
                DwmAPI::DWMWA_EXTENDED_FRAME_BOUNDS,
                lpRect, sizeof(*lpRect)))
        {
            return;
        }
    } catch (const DllUtil::Exception &) {}

    ::GetWindowRect(hWnd, lpRect);
}


/************************************************************************
 * AWT preloading support
 */
bool AwtToolkit::PreloadAction::EnsureInited()
{
    DWORD _initThreadId = GetInitThreadID();
    if (_initThreadId != 0) {
        // already inited
        // ensure the action is inited on correct thread
        PreloadThread &preloadThread
            = AwtToolkit::GetInstance().GetPreloadThread();
        if (_initThreadId == preloadThread.GetThreadId()) {
            if (!preloadThread.IsWrongThread()) {
                return true;
            }
            // inited on preloadThread (wrongThread), not cleaned yet
            // have to wait cleanup completion
            preloadThread.Wait4Finish();
        } else {
            // inited on other thread (Toolkit thread?)
            // consider as correctly inited
            return true;
        }
    }

    // init on Toolkit thread
    AwtToolkit::GetInstance().InvokeFunction(InitWrapper, this);

    return true;
}

DWORD AwtToolkit::PreloadAction::GetInitThreadID()
{
    CriticalSection::Lock lock(initLock);
    return initThreadId;
}

bool AwtToolkit::PreloadAction::Clean()
{
    DWORD _initThreadId = GetInitThreadID();
    if (_initThreadId == ::GetCurrentThreadId()) {
        // inited on this thread
        Clean(false);
        return true;
    }
    return false;
}

/*static*/
void AwtToolkit::PreloadAction::InitWrapper(void *param)
{
    PreloadAction *pThis = (PreloadAction *)param;
    pThis->Init();
}

void AwtToolkit::PreloadAction::Init()
{
    CriticalSection::Lock lock(initLock);
    if (initThreadId == 0) {
        initThreadId = ::GetCurrentThreadId();
        InitImpl();
    }
}

void AwtToolkit::PreloadAction::Clean(bool reInit) {
    CriticalSection::Lock lock(initLock);
    if (initThreadId != 0) {
        //ASSERT(initThreadId == ::GetCurrentThreadId());
        CleanImpl(reInit);
        initThreadId = 0;
    }
}

// PreloadThread implementation
AwtToolkit::PreloadThread::PreloadThread()
    : status(None), wrongThread(false), threadId(0),
    pActionChain(NULL), pLastProcessedAction(NULL),
    execFunc(NULL), execParam(NULL)
{
    hFinished = ::CreateEvent(NULL, TRUE, FALSE, NULL);
    hAwake = ::CreateEvent(NULL, FALSE, FALSE, NULL);
}

AwtToolkit::PreloadThread::~PreloadThread()
{
    //Terminate(false);
    ::CloseHandle(hFinished);
    ::CloseHandle(hAwake);
}

bool AwtToolkit::PreloadThread::AddAction(AwtToolkit::PreloadAction *pAction)
{
    CriticalSection::Lock lock(threadLock);

    if (status > Preloading) {
        // too late - the thread already terminated or run as toolkit thread
        return false;
    }

    if (pActionChain == NULL) {
        // 1st action
        pActionChain = pAction;
    } else {
        // add the action to the chain
        PreloadAction *pChain = pActionChain;
        while (true) {
            PreloadAction *pNext = pChain->GetNext();
            if (pNext == NULL) {
                break;
            }
            pChain = pNext;
        }
        pChain->SetNext(pAction);
    }

    if (status > None) {
        // the thread is already running (status == Preloading)
        AwakeThread();
        return true;
    }

    // need to start thread
    ::ResetEvent(hAwake);
    ::ResetEvent(hFinished);

    HANDLE hThread = (HANDLE)_beginthreadex(NULL, 0x100000, StaticThreadProc,
                                            this, 0, &threadId);

    if (hThread == 0) {
        threadId = 0;
        return false;
    }

    status = Preloading;

    ::CloseHandle(hThread);

    return true;
}

bool AwtToolkit::PreloadThread::Terminate(bool wrongThread)
{
    CriticalSection::Lock lock(threadLock);

    if (status != Preloading) {
        return false;
    }

    execFunc = NULL;
    execParam = NULL;
    this->wrongThread = wrongThread;
    status = Cleaning;
    AwakeThread();

    return true;
}

bool AwtToolkit::PreloadThread::InvokeAndTerminate(void(_cdecl *fn)(void *), void *param)
{
    CriticalSection::Lock lock(threadLock);

    if (status != Preloading) {
        return false;
    }

    execFunc = fn;
    execParam = param;
    status = fn == NULL ? Cleaning : RunningToolkit;
    AwakeThread();

    return true;
}

bool AwtToolkit::PreloadThread::OnPreloadThread()
{
    return GetThreadId() == ::GetCurrentThreadId();
}

/*static*/
unsigned WINAPI AwtToolkit::PreloadThread::StaticThreadProc(void *param)
{
    AwtToolkit::PreloadThread *pThis = (AwtToolkit::PreloadThread *)param;
    return pThis->ThreadProc();
}

unsigned AwtToolkit::PreloadThread::ThreadProc()
{
    void(_cdecl *_execFunc)(void *) = NULL;
    void *_execParam = NULL;
    bool _wrongThread = false;

    // initialization
    while (true) {
        PreloadAction *pAction;
        {
            CriticalSection::Lock lock(threadLock);
            if (status != Preloading) {
                // get invoke parameters
                _execFunc = execFunc;
                _execParam = execParam;
                _wrongThread = wrongThread;
                break;
            }
            pAction = GetNextAction();
        }
        if (pAction != NULL) {
            pAction->Init();
        } else {
            ::WaitForSingleObject(hAwake, INFINITE);
        }
    }

    // call a function from InvokeAndTerminate
    if (_execFunc != NULL) {
        _execFunc(_execParam);
    } else {
        // time to terminate..
    }

    // cleanup
    {
        CriticalSection::Lock lock(threadLock);
        pLastProcessedAction = NULL; // goto 1st action in the chain
        status = Cleaning;
    }
    for (PreloadAction *pAction = GetNextAction(); pAction != NULL;
            pAction = GetNextAction()) {
        pAction->Clean(_wrongThread);
    }

    // don't clear threadId! it is used by PreloadAction::EnsureInited

    {
        CriticalSection::Lock lock(threadLock);
        status = Finished;
    }
    ::SetEvent(hFinished);
    return 0;
}

AwtToolkit::PreloadAction* AwtToolkit::PreloadThread::GetNextAction()
{
    CriticalSection::Lock lock(threadLock);
    PreloadAction *pAction = (pLastProcessedAction == NULL)
                                    ? pActionChain
                                    : pLastProcessedAction->GetNext();
    if (pAction != NULL) {
        pLastProcessedAction = pAction;
    }

    return pAction;
}


extern "C" {

/* Terminates preload thread (if it's still alive
 * - it may occur if the application doesn't use AWT).
 * The function is called from launcher after completion main java thread.
 */
__declspec(dllexport) void preloadStop()
{
    AwtToolkit::GetInstance().GetPreloadThread().Terminate(false);
}

}


/************************************************************************
 * Toolkit native methods
 */

extern "C" {

/*
 * Class:     java_awt_Toolkit
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_java_awt_Toolkit_initIDs(JNIEnv *env, jclass cls) {
    TRY;

    AwtToolkit::getDefaultToolkitMID =
        env->GetStaticMethodID(cls,"getDefaultToolkit","()Ljava/awt/Toolkit;");
    DASSERT(AwtToolkit::getDefaultToolkitMID != NULL);
    CHECK_NULL(AwtToolkit::getDefaultToolkitMID);

    AwtToolkit::getFontMetricsMID =
        env->GetMethodID(cls, "getFontMetrics", "(Ljava/awt/Font;)Ljava/awt/FontMetrics;");
    DASSERT(AwtToolkit::getFontMetricsMID != NULL);
    CHECK_NULL(AwtToolkit::getFontMetricsMID);

    jclass insetsClass = env->FindClass("java/awt/Insets");
    DASSERT(insetsClass != NULL);
    CHECK_NULL(insetsClass);
    AwtToolkit::insetsMID = env->GetMethodID(insetsClass, "<init>", "(IIII)V");
    DASSERT(AwtToolkit::insetsMID != NULL);
    CHECK_NULL(AwtToolkit::insetsMID);

    CATCH_BAD_ALLOC;
}


} /* extern "C" */

/************************************************************************
 * WToolkit native methods
 */

extern "C" {

/*
 * Class:     sun_awt_windows_WToolkit
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WToolkit_initIDs(JNIEnv *env, jclass cls)
{
    TRY;

    AwtToolkit::windowsSettingChangeMID =
        env->GetMethodID(cls, "windowsSettingChange", "()V");
    DASSERT(AwtToolkit::windowsSettingChangeMID != 0);
    CHECK_NULL(AwtToolkit::windowsSettingChangeMID);

    AwtToolkit::displayChangeMID =
    env->GetStaticMethodID(cls, "displayChanged", "()V");
    DASSERT(AwtToolkit::displayChangeMID != 0);
    CHECK_NULL(AwtToolkit::displayChangeMID);

    // Set various global IDs needed by JAWT code.  Note: these
    // variables cannot be set by JAWT code directly due to
    // different permissions that that code may be run under
    // (bug 4796548).  It would be nice to initialize these
    // variables lazily, but given the minimal number of calls
    // for this, it seems simpler to just do it at startup with
    // negligible penalty.
    jclass sDataClassLocal = env->FindClass("sun/java2d/SurfaceData");
    DASSERT(sDataClassLocal != 0);
    CHECK_NULL(sDataClassLocal);

    jclass vImgClassLocal = env->FindClass("sun/awt/image/SunVolatileImage");
    DASSERT(vImgClassLocal != 0);
    CHECK_NULL(vImgClassLocal);

    jclass vSMgrClassLocal =
        env->FindClass("sun/awt/image/VolatileSurfaceManager");
    DASSERT(vSMgrClassLocal != 0);
    CHECK_NULL(vSMgrClassLocal);

    jclass componentClassLocal = env->FindClass("java/awt/Component");
    DASSERT(componentClassLocal != 0);
    CHECK_NULL(componentClassLocal);

    jawtSMgrID = env->GetFieldID(vImgClassLocal, "volSurfaceManager",
                                 "Lsun/awt/image/VolatileSurfaceManager;");
    DASSERT(jawtSMgrID != 0);
    CHECK_NULL(jawtSMgrID);

    jawtSDataID = env->GetFieldID(vSMgrClassLocal, "sdCurrent",
                                  "Lsun/java2d/SurfaceData;");
    DASSERT(jawtSDataID != 0);
    CHECK_NULL(jawtSDataID);

    jawtPDataID = env->GetFieldID(sDataClassLocal, "pData", "J");
    DASSERT(jawtPDataID != 0);
    CHECK_NULL(jawtPDataID);
    // Save these classes in global references for later use
    jawtVImgClass = (jclass)env->NewGlobalRef(vImgClassLocal);
    CHECK_NULL(jawtVImgClass);
    jawtComponentClass = (jclass)env->NewGlobalRef(componentClassLocal);

    jclass dPeerClassLocal = env->FindClass("sun/awt/windows/WDesktopPeer");
    DASSERT(dPeerClassLocal != 0);
    CHECK_NULL(dPeerClassLocal);

    jclass reasonClassLocal    = env->FindClass("java/awt/desktop/UserSessionEvent$Reason");
    CHECK_NULL(reasonClassLocal);

    reasonUnspecified = GetStaticObject(env, reasonClassLocal, "UNSPECIFIED",
                                         "Ljava/awt/desktop/UserSessionEvent$Reason;");
    CHECK_NULL (reasonUnspecified);
    reasonUnspecified = env->NewGlobalRef(reasonUnspecified);

    reasonConsole = GetStaticObject(env, reasonClassLocal, "CONSOLE",
                                         "Ljava/awt/desktop/UserSessionEvent$Reason;");
    CHECK_NULL (reasonConsole);
    reasonConsole = env->NewGlobalRef(reasonConsole);

    reasonRemote = GetStaticObject(env, reasonClassLocal, "REMOTE",
                                         "Ljava/awt/desktop/UserSessionEvent$Reason;");
    CHECK_NULL (reasonRemote);
    reasonRemote = env->NewGlobalRef(reasonRemote);

    reasonLock = GetStaticObject(env, reasonClassLocal, "LOCK",
                                         "Ljava/awt/desktop/UserSessionEvent$Reason;");
    CHECK_NULL (reasonLock);
    reasonLock = env->NewGlobalRef(reasonLock);


    AwtToolkit::userSessionMID =
    env->GetStaticMethodID(dPeerClassLocal, "userSessionCallback",
                            "(ZLjava/awt/desktop/UserSessionEvent$Reason;)V");
    DASSERT(AwtToolkit::userSessionMID != 0);
    CHECK_NULL(AwtToolkit::userSessionMID);

    AwtToolkit::systemSleepMID =
    env->GetStaticMethodID(dPeerClassLocal, "systemSleepCallback", "(Z)V");
    DASSERT(AwtToolkit::systemSleepMID != 0);
    CHECK_NULL(AwtToolkit::systemSleepMID);

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WToolkit
 * Method:    embeddedInit
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL
Java_sun_awt_windows_WToolkit_embeddedInit(JNIEnv *env, jclass cls)
{
    TRY;

    AwtToolkit::SetEnv(env);

    return AwtToolkit::GetInstance().Initialize(FALSE);

    CATCH_BAD_ALLOC_RET(JNI_FALSE);
}

/*
 * Class:     sun_awt_windows_WToolkit
 * Method:    embeddedDispose
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL
Java_sun_awt_windows_WToolkit_embeddedDispose(JNIEnv *env, jclass cls)
{
    TRY;

    BOOL retval = AwtToolkit::GetInstance().Dispose();
    AwtToolkit::GetInstance().SetPeer(env, NULL);
    return retval;

    CATCH_BAD_ALLOC_RET(JNI_FALSE);
}

/*
 * Class:     sun_awt_windows_WToolkit
 * Method:    embeddedEventLoopIdleProcessing
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WToolkit_embeddedEventLoopIdleProcessing(JNIEnv *env,
    jobject self)
{
    VerifyWindowMoveLockReleased();
}


/*
 * Class:     sun_awt_windows_WToolkit
 * Method:    init
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL
Java_sun_awt_windows_WToolkit_init(JNIEnv *env, jobject self)
{
    TRY;

    AwtToolkit::SetEnv(env);

    AwtToolkit::GetInstance().SetPeer(env, self);

    // This call will fail if the Toolkit was already initialized.
    // In that case, we don't want to start another message pump.
    return AwtToolkit::GetInstance().Initialize(TRUE);

    CATCH_BAD_ALLOC_RET(FALSE);
}

/*
 * Class:     sun_awt_windows_WToolkit
 * Method:    eventLoop
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WToolkit_eventLoop(JNIEnv *env, jobject self)
{
    TRY;

    DASSERT(AwtToolkit::GetInstance().localPump());

    AwtToolkit::SetBusy(TRUE);

    AwtToolkit::GetInstance().MessageLoop(AwtToolkit::PrimaryIdleFunc,
                                          AwtToolkit::CommonPeekMessageFunc);

    AwtToolkit::GetInstance().Dispose();

    AwtToolkit::SetBusy(FALSE);

    /*
     * IMPORTANT NOTES:
     *   The AwtToolkit has been destructed by now.
     * DO NOT CALL any method of AwtToolkit!!!
     */

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WToolkit
 * Method:    shutdown
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WToolkit_shutdown(JNIEnv *env, jobject self)
{
    TRY;

    AwtToolkit& tk = AwtToolkit::GetInstance();

    tk.QuitMessageLoop(AwtToolkit::EXIT_ALL_ENCLOSING_LOOPS);

    while (!tk.IsDisposed()) {
        Sleep(100);
    }

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WToolkit
 * Method:    startSecondaryEventLoop
 * Signature: ()V;
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WToolkit_startSecondaryEventLoop(
    JNIEnv *env,
    jclass)
{
    TRY;

    DASSERT(AwtToolkit::MainThread() == ::GetCurrentThreadId());

    AwtToolkit::GetInstance().MessageLoop(AwtToolkit::SecondaryIdleFunc,
                                          AwtToolkit::CommonPeekMessageFunc);

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WToolkit
 * Method:    quitSecondaryEventLoop
 * Signature: ()V;
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WToolkit_quitSecondaryEventLoop(
    JNIEnv *env,
    jclass)
{
    TRY;

    AwtToolkit::GetInstance().QuitMessageLoop(AwtToolkit::EXIT_ENCLOSING_LOOP);

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WToolkit
 * Method:    makeColorModel
 * Signature: ()Ljava/awt/image/ColorModel;
 */
JNIEXPORT jobject JNICALL
Java_sun_awt_windows_WToolkit_makeColorModel(JNIEnv *env, jclass cls)
{
    TRY;

    return AwtWin32GraphicsDevice::GetColorModel(env, JNI_FALSE,
        AwtWin32GraphicsDevice::GetDefaultDeviceIndex());

    CATCH_BAD_ALLOC_RET(NULL);
}

/*
 * Class:     sun_awt_windows_WToolkit
 * Method:    getMaximumCursorColors
 * Signature: ()I
 */
JNIEXPORT jint JNICALL
Java_sun_awt_windows_WToolkit_getMaximumCursorColors(JNIEnv *env, jobject self)
{
    TRY;

    HDC hIC = ::CreateIC(TEXT("DISPLAY"), NULL, NULL, NULL);

    int nColor = 256;
    switch (::GetDeviceCaps(hIC, BITSPIXEL) * ::GetDeviceCaps(hIC, PLANES)) {
        case 1:         nColor = 2;             break;
        case 4:         nColor = 16;            break;
        case 8:         nColor = 256;           break;
        case 16:        nColor = 65536;         break;
        case 24:        nColor = 16777216;      break;
    }
    ::DeleteDC(hIC);
    return nColor;

    CATCH_BAD_ALLOC_RET(0);
}

/*
 * Class:     sun_awt_windows_WToolkit
 * Method:    getSreenInsets
 * Signature: (I)Ljava/awt/Insets;
 */
JNIEXPORT jobject JNICALL
Java_sun_awt_windows_WToolkit_getScreenInsets(JNIEnv *env,
                                              jobject self,
                                              jint screen)
{
    jobject insets = NULL;
    RECT rect;

    TRY;

    if (AwtToolkit::GetScreenInsets(screen, &rect)) {
        jclass insetsClass = env->FindClass("java/awt/Insets");
        DASSERT(insetsClass != NULL);
        CHECK_NULL_RETURN(insetsClass, NULL);
        Devices::InstanceAccess devices;
        AwtWin32GraphicsDevice *device = devices->GetDevice(screen);
        insets = env->NewObject(insetsClass,
                AwtToolkit::insetsMID,
                device == NULL ? rect.top : device->ScaleDownY(rect.top),
                device == NULL ? rect.left : device->ScaleDownX(rect.left),
                device == NULL ? rect.bottom : device->ScaleDownY(rect.bottom),
                device == NULL ? rect.right : device->ScaleDownX(rect.right));
    }

    if (safe_ExceptionOccurred(env)) {
        return 0;
    }
    return insets;

    CATCH_BAD_ALLOC_RET(NULL);
}


/*
 * Class:     sun_awt_windows_WToolkit
 * Method:    nativeSync
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WToolkit_nativeSync(JNIEnv *env, jobject self)
{
    TRY;

    // Synchronize both GDI and DDraw
    VERIFY(::GdiFlush());

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WToolkit
 * Method:    beep
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WToolkit_beep(JNIEnv *env, jobject self)
{
    TRY;

    VERIFY(::MessageBeep(MB_OK));

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WToolkit
 * Method:    getLockingKeyStateNative
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL
Java_sun_awt_windows_WToolkit_getLockingKeyStateNative(JNIEnv *env, jobject self, jint javaKey)
{
    TRY;

    UINT windowsKey, modifiers;
    AwtComponent::JavaKeyToWindowsKey(javaKey, &windowsKey, &modifiers);

    if (windowsKey == 0) {
        JNU_ThrowByName(env, "java/lang/UnsupportedOperationException", "Keyboard doesn't have requested key");
        return JNI_FALSE;
    }

    // low order bit in keyboardState indicates whether the key is toggled
    BYTE keyboardState[AwtToolkit::KB_STATE_SIZE];
    AwtToolkit::GetKeyboardState(keyboardState);
    return keyboardState[windowsKey] & 0x01;

    CATCH_BAD_ALLOC_RET(JNI_FALSE);
}

/*
 * Class:     sun_awt_windows_WToolkit
 * Method:    setLockingKeyStateNative
 * Signature: (IZ)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WToolkit_setLockingKeyStateNative(JNIEnv *env, jobject self, jint javaKey, jboolean state)
{
    TRY;

    UINT windowsKey, modifiers;
    AwtComponent::JavaKeyToWindowsKey(javaKey, &windowsKey, &modifiers);

    if (windowsKey == 0) {
        JNU_ThrowByName(env, "java/lang/UnsupportedOperationException", "Keyboard doesn't have requested key");
        return;
    }

    // if the key isn't in the desired state yet, simulate key events to get there
    // low order bit in keyboardState indicates whether the key is toggled
    BYTE keyboardState[AwtToolkit::KB_STATE_SIZE];
    AwtToolkit::GetKeyboardState(keyboardState);
    if ((keyboardState[windowsKey] & 0x01) != state) {
        ::keybd_event(windowsKey, 0, 0, 0);
        ::keybd_event(windowsKey, 0, KEYEVENTF_KEYUP, 0);
    }

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WToolkit
 * Method:    loadSystemColors
 * Signature: ([I)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WToolkit_loadSystemColors(JNIEnv *env, jobject self,
                                               jintArray colors)
{
    TRY;

    static int indexMap[] = {
        COLOR_DESKTOP, /* DESKTOP */
        COLOR_ACTIVECAPTION, /* ACTIVE_CAPTION */
        COLOR_CAPTIONTEXT, /* ACTIVE_CAPTION_TEXT */
        COLOR_ACTIVEBORDER, /* ACTIVE_CAPTION_BORDER */
        COLOR_INACTIVECAPTION, /* INACTIVE_CAPTION */
        COLOR_INACTIVECAPTIONTEXT, /* INACTIVE_CAPTION_TEXT */
        COLOR_INACTIVEBORDER, /* INACTIVE_CAPTION_BORDER */
        COLOR_WINDOW, /* WINDOW */
        COLOR_WINDOWFRAME, /* WINDOW_BORDER */
        COLOR_WINDOWTEXT, /* WINDOW_TEXT */
        COLOR_MENU, /* MENU */
        COLOR_MENUTEXT, /* MENU_TEXT */
        COLOR_WINDOW, /* TEXT */
        COLOR_WINDOWTEXT, /* TEXT_TEXT */
        COLOR_HIGHLIGHT, /* TEXT_HIGHLIGHT */
        COLOR_HIGHLIGHTTEXT, /* TEXT_HIGHLIGHT_TEXT */
        COLOR_GRAYTEXT, /* TEXT_INACTIVE_TEXT */
        COLOR_3DFACE, /* CONTROL */
        COLOR_BTNTEXT, /* CONTROL_TEXT */
        COLOR_3DLIGHT, /* CONTROL_HIGHLIGHT */
        COLOR_3DHILIGHT, /* CONTROL_LT_HIGHLIGHT */
        COLOR_3DSHADOW, /* CONTROL_SHADOW */
        COLOR_3DDKSHADOW, /* CONTROL_DK_SHADOW */
        COLOR_SCROLLBAR, /* SCROLLBAR */
        COLOR_INFOBK, /* INFO */
        COLOR_INFOTEXT, /* INFO_TEXT */
    };

    jint colorLen = env->GetArrayLength(colors);
    jint* colorsPtr = NULL;
    try {
        colorsPtr = (jint *)env->GetPrimitiveArrayCritical(colors, 0);
        for (int i = 0; i < (sizeof indexMap)/(sizeof *indexMap) && i < colorLen; i++) {
            colorsPtr[i] = DesktopColor2RGB(indexMap[i]);
        }
    } catch (...) {
        if (colorsPtr != NULL) {
            env->ReleasePrimitiveArrayCritical(colors, colorsPtr, 0);
        }
        throw;
    }

    env->ReleasePrimitiveArrayCritical(colors, colorsPtr, 0);

    CATCH_BAD_ALLOC;
}

extern "C" JNIEXPORT jobject JNICALL DSGetComponent
    (JNIEnv* env, void* platformInfo)
{
    TRY;

    HWND hWnd = (HWND)platformInfo;
    if (!::IsWindow(hWnd))
        return NULL;

    AwtComponent* comp = AwtComponent::GetComponent(hWnd);
    if (comp == NULL)
        return NULL;

    return comp->GetTarget(env);

    CATCH_BAD_ALLOC_RET(NULL);
}

JNIEXPORT void JNICALL
Java_sun_awt_windows_WToolkit_postDispose(JNIEnv *env, jclass clazz)
{
#ifdef DEBUG
    TRY_NO_VERIFY;

    // If this method was called, that means runFinalizersOnExit is turned
    // on and the VM is exiting cleanly. We should signal the debug memory
    // manager to generate a leaks report.
    AwtDebugSupport::GenerateLeaksReport();

    CATCH_BAD_ALLOC;
#endif
}

/*
 * Class:     sun_awt_windows_WToolkit
 * Method:    setDynamicLayoutNative
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WToolkit_setDynamicLayoutNative(JNIEnv *env,
  jobject self, jboolean dynamic)
{
    TRY;

    AwtToolkit::GetInstance().SetDynamicLayout(dynamic);

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WToolkit
 * Method:    isDynamicLayoutSupportedNative
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL
Java_sun_awt_windows_WToolkit_isDynamicLayoutSupportedNative(JNIEnv *env,
  jobject self)
{
    TRY;

    return (jboolean) AwtToolkit::GetInstance().IsDynamicLayoutSupported();

    CATCH_BAD_ALLOC_RET(FALSE);
}

/*
 * Class:     sun_awt_windows_WToolkit
 * Method:    printWindowsVersion
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_sun_awt_windows_WToolkit_getWindowsVersion(JNIEnv *env, jclass cls)
{
    TRY;

    WCHAR szVer[128];

    DWORD version = ::GetVersion();
    swprintf(szVer, 128, L"0x%x = %ld", version, version);
    int l = lstrlen(szVer);

    if (IS_WIN2000) {
        if (IS_WINXP) {
            if (IS_WINVISTA) {
                swprintf(szVer + l, 128, L" (Windows Vista)");
            } else {
                swprintf(szVer + l, 128, L" (Windows XP)");
            }
        } else {
            swprintf(szVer + l, 128, L" (Windows 2000)");
        }
    } else {
        swprintf(szVer + l, 128, L" (Unknown)");
    }

    return JNU_NewStringPlatform(env, szVer);

    CATCH_BAD_ALLOC_RET(NULL);
}

JNIEXPORT void JNICALL
Java_sun_awt_windows_WToolkit_showTouchKeyboard(JNIEnv *env, jobject self,
    jboolean causedByTouchEvent)
{
    AwtToolkit& tk = AwtToolkit::GetInstance();
    if (!tk.IsWin8OrLater() || !tk.IsTouchKeyboardAutoShowEnabled()) {
        return;
    }

    if (causedByTouchEvent ||
        (tk.IsTouchKeyboardAutoShowSystemEnabled() &&
            !tk.IsAnyKeyboardAttached())) {
        tk.ShowTouchKeyboard();
    }
}

JNIEXPORT void JNICALL
Java_sun_awt_windows_WToolkit_hideTouchKeyboard(JNIEnv *env, jobject self)
{
    AwtToolkit& tk = AwtToolkit::GetInstance();
    if (!tk.IsWin8OrLater() || !tk.IsTouchKeyboardAutoShowEnabled()) {
        return;
    }
    tk.HideTouchKeyboard();
}

JNIEXPORT jboolean JNICALL
Java_sun_awt_windows_WToolkit_syncNativeQueue(JNIEnv *env, jobject self, jlong timeout)
{
    if (timeout <= 0) {
        return JNI_FALSE;
    }
    AwtToolkit & tk = AwtToolkit::GetInstance();
    DWORD eventNumber = tk.eventNumber;
    tk.PostMessage(WM_SYNC_WAIT, 0, 0);
    for(long t = 2; t < timeout &&
               WAIT_TIMEOUT == ::WaitForSingleObject(tk.m_waitEvent, 2); t+=2) {
        if (tk.isInDoDragDropLoop) {
            break;
        }
    }
    DWORD newEventNumber = tk.eventNumber;
    return (newEventNumber - eventNumber) > 2;
}

} /* extern "C" */

/* Convert a Windows desktop color index into an RGB value. */
COLORREF DesktopColor2RGB(int colorIndex) {
    DWORD sysColor = ::GetSysColor(colorIndex);
    return ((GetRValue(sysColor)<<16) | (GetGValue(sysColor)<<8) |
            (GetBValue(sysColor)) | 0xff000000);
}


/*
 * Class:     sun_awt_SunToolkit
 * Method:    closeSplashScreen
 * Signature: ()V
 */
extern "C" JNIEXPORT void JNICALL
Java_sun_awt_SunToolkit_closeSplashScreen(JNIEnv *env, jclass cls)
{
    typedef void (*SplashClose_t)();
    HMODULE hSplashDll = GetModuleHandle(_T("splashscreen.dll"));
    if (!hSplashDll) {
        return; // dll not loaded
    }
    SplashClose_t splashClose = (SplashClose_t)GetProcAddress(hSplashDll,
        "SplashClose");
    if (splashClose) {
        splashClose();
    }
}

/*
 * accessible from awt_Component
 */
BOOL AwtToolkit::areExtraMouseButtonsEnabled() {
    return m_areExtraMouseButtonsEnabled;
}

/*
 * Class:     sun_awt_windows_WToolkit
 * Method:    setExtraMouseButtonsEnabledNative
 * Signature: (Z)V
 */
extern "C" JNIEXPORT void JNICALL Java_sun_awt_windows_WToolkit_setExtraMouseButtonsEnabledNative
(JNIEnv *env, jclass self, jboolean enable){
    TRY;
    AwtToolkit::GetInstance().setExtraMouseButtonsEnabled(enable);
    CATCH_BAD_ALLOC;
}

void AwtToolkit::setExtraMouseButtonsEnabled(BOOL enable) {
    m_areExtraMouseButtonsEnabled = enable;
}

JNIEXPORT jint JNICALL Java_sun_awt_windows_WToolkit_getNumberOfButtonsImpl
(JNIEnv *, jobject self) {
    return AwtToolkit::GetNumberOfButtons();
}

UINT AwtToolkit::GetNumberOfButtons() {
    return MOUSE_BUTTONS_WINDOWS_SUPPORTED;
}

bool AwtToolkit::IsWin8OrLater() {
    return m_isWin8OrLater;
}

bool AwtToolkit::IsTouchKeyboardAutoShowEnabled() {
    return m_touchKbrdAutoShowIsEnabled;
}

bool AwtToolkit::IsAnyKeyboardAttached() {
    UINT numDevs = 0;
    UINT numDevsRet = 0;
    const UINT devListTypeSize = sizeof(RAWINPUTDEVICELIST);
    if ((::GetRawInputDeviceList(NULL, &numDevs, devListTypeSize) != 0) ||
        (numDevs == 0)) {
        return false;
    }

    RAWINPUTDEVICELIST* pDevList = new RAWINPUTDEVICELIST[numDevs];
    while (((numDevsRet = ::GetRawInputDeviceList(pDevList, &numDevs,
            devListTypeSize)) == (UINT)-1) &&
        (::GetLastError() == ERROR_INSUFFICIENT_BUFFER)) {
        if (pDevList != NULL) {
            delete[] pDevList;
        }
        pDevList = new RAWINPUTDEVICELIST[numDevs];
    }

    bool keyboardIsAttached = false;
    if (numDevsRet != (UINT)-1) {
        for (UINT i = 0; i < numDevsRet; i++) {
            if (pDevList[i].dwType == RIM_TYPEKEYBOARD) {
                keyboardIsAttached = true;
                break;
            }
        }
    }

    if (pDevList != NULL) {
        delete[] pDevList;
    }
    return keyboardIsAttached;
}

bool AwtToolkit::IsTouchKeyboardAutoShowSystemEnabled() {
    const TCHAR tabTipKeyName[] = _T("SOFTWARE\\Microsoft\\TabletTip\\1.7");
    HKEY hTabTipKey = NULL;
    if (::RegOpenKeyEx(HKEY_CURRENT_USER, tabTipKeyName, 0, KEY_READ,
            &hTabTipKey) != ERROR_SUCCESS) {
        return false;
    }

    const TCHAR enableAutoInvokeValName[] = _T("EnableDesktopModeAutoInvoke");
    DWORD keyValType = 0;
    bool autoShowIsEnabled = false;
    if (::RegQueryValueEx(hTabTipKey, enableAutoInvokeValName, NULL,
            &keyValType, NULL, NULL) == ERROR_SUCCESS) {
        if (keyValType == REG_DWORD) {
            DWORD enableAutoInvokeVal = 0;
            DWORD bytesCopied = sizeof(DWORD);
            if (::RegQueryValueEx(hTabTipKey, enableAutoInvokeValName, NULL,
                    NULL, (LPBYTE)(DWORD*)&enableAutoInvokeVal,
                    &bytesCopied) == ERROR_SUCCESS) {
                autoShowIsEnabled = (enableAutoInvokeVal == 0 ? false : true);
            }
        }
    }

    if (hTabTipKey != NULL) {
        ::RegCloseKey(hTabTipKey);
    }
    return autoShowIsEnabled;
}

void AwtToolkit::ShowTouchKeyboard() {
    if (m_isWin8OrLater && m_touchKbrdAutoShowIsEnabled &&
        (m_touchKbrdExeFilePath != NULL)) {
        int retVal = (int)((intptr_t)::ShellExecute(NULL, _T("open"),
            m_touchKbrdExeFilePath, NULL, NULL, SW_SHOW));
        if (retVal <= 32) {
            DTRACE_PRINTLN1("AwtToolkit::ShowTouchKeyboard: Failed"
                ", retVal='%d'", retVal);
        }
    }
}

void AwtToolkit::HideTouchKeyboard() {
    if (m_isWin8OrLater && m_touchKbrdAutoShowIsEnabled) {
        HWND hwnd = GetTouchKeyboardWindow();
        if (hwnd != NULL) {
            ::PostMessage(hwnd, WM_SYSCOMMAND, SC_CLOSE, 0);
        }
    }
}

BOOL AwtToolkit::TIRegisterTouchWindow(HWND hWnd, ULONG ulFlags) {
    if (m_pRegisterTouchWindow == NULL) {
        return FALSE;
    }
    return m_pRegisterTouchWindow(hWnd, ulFlags);
}

BOOL AwtToolkit::TIGetTouchInputInfo(HTOUCHINPUT hTouchInput,
    UINT cInputs, PTOUCHINPUT pInputs, int cbSize) {
    if (m_pGetTouchInputInfo == NULL) {
        return FALSE;
    }
    return m_pGetTouchInputInfo(hTouchInput, cInputs, pInputs, cbSize);
}

BOOL AwtToolkit::TICloseTouchInputHandle(HTOUCHINPUT hTouchInput) {
    if (m_pCloseTouchInputHandle == NULL) {
        return FALSE;
    }
    return m_pCloseTouchInputHandle(hTouchInput);
}

/*
 * The fuction intended for access to an IME API. It posts IME message to the queue and
 * waits untill the message processing is completed.
 *
 * On Windows 10 the IME may process the messages send via SenMessage() from other threads
 * when the IME is called by TranslateMessage(). This may cause an reentrancy issue when
 * the windows procedure processing the sent message call an IME function and leaves
 * the IME functionality in an unexpected state.
 * This function avoids reentrancy issue and must be used for sending of all IME messages
 * instead of SendMessage().
 */
LRESULT AwtToolkit::InvokeInputMethodFunction(UINT msg, WPARAM wParam, LPARAM lParam) {
    /*
     * DND runs on the main thread. So it is  necessary to use SendMessage() to call an IME
     * function once the DND is active; otherwise a hang is possible since DND may wait for
     * the IME completion.
     */
    CriticalSection::Lock lock(m_inputMethodLock);
    if (isInDoDragDropLoop) {
        SendMessage(msg, wParam, lParam);
        ::ResetEvent(m_inputMethodWaitEvent);
        return m_inputMethodData;
    } else {
        if (PostMessage(msg, wParam, lParam)) {
            ::WaitForSingleObject(m_inputMethodWaitEvent, INFINITE);
            return m_inputMethodData;
        }
        return 0;
    }
}

