/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include "jni_util.h"
#include "awt.h"
#include <jni.h>
#include <shellapi.h>
#include <float.h>
#include <shlobj.h>
#include "awt_Toolkit.h"

#define BUFFER_LIMIT   MAX_PATH+1

#define NOTIFY_FOR_ALL_SESSIONS 1
#define NOTIFY_FOR_THIS_SESSION 0

typedef BOOL (WINAPI *WTSRegisterSessionNotification)(HWND,DWORD);
static WTSRegisterSessionNotification fn_WTSRegisterSessionNotification;

BOOL isSuddenTerminationEnabled = TRUE;

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class:     sun_awt_windows_WDesktopPeer
 * Method:    init
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_awt_windows_WDesktopPeer_init
  (JNIEnv *, jclass) {
    static HMODULE libWtsapi32 = NULL;
    if (libWtsapi32 == NULL) {
        libWtsapi32 = JDK_LoadSystemLibrary("Wtsapi32.dll");
        if (libWtsapi32) {
            fn_WTSRegisterSessionNotification = (WTSRegisterSessionNotification)
                    GetProcAddress(libWtsapi32, "WTSRegisterSessionNotification");
            if (fn_WTSRegisterSessionNotification) {
                HWND hwnd = AwtToolkit::GetInstance().GetHWnd();
                //used for UserSessionListener
                fn_WTSRegisterSessionNotification(hwnd, NOTIFY_FOR_THIS_SESSION);
            }
        }
    }
}

/*
 * Class:     sun_awt_windows_WDesktopPeer
 * Method:    ShellExecute
 * Signature: (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_sun_awt_windows_WDesktopPeer_ShellExecute
  (JNIEnv *env, jclass cls, jstring fileOrUri_j, jstring verb_j)
{
    LPCWSTR fileOrUri_c = JNU_GetStringPlatformChars(env, fileOrUri_j, JNI_FALSE);
    CHECK_NULL_RETURN(fileOrUri_c, NULL);
    LPCWSTR verb_c = JNU_GetStringPlatformChars(env, verb_j, JNI_FALSE);
    if (verb_c == NULL) {
        JNU_ReleaseStringPlatformChars(env, fileOrUri_j, fileOrUri_c);
        return NULL;
    }

    // 6457572: ShellExecute possibly changes FPU control word - saving it here
    unsigned oldcontrol87 = _control87(0, 0);
    HRESULT hr = ::CoInitializeEx(NULL, COINIT_APARTMENTTHREADED |
                                        COINIT_DISABLE_OLE1DDE);
    HINSTANCE retval;
    DWORD error;
    if (SUCCEEDED(hr)) {
        retval = ::ShellExecute(NULL, verb_c, fileOrUri_c, NULL, NULL,
                                SW_SHOWNORMAL);
        error = ::GetLastError();
        ::CoUninitialize();
    }
    _control87(oldcontrol87, 0xffffffff);

    JNU_ReleaseStringPlatformChars(env, fileOrUri_j, fileOrUri_c);
    JNU_ReleaseStringPlatformChars(env, verb_j, verb_c);

    if (FAILED(hr)) {
        return JNU_NewStringPlatform(env, L"CoInitializeEx() failed.");
    }
    if ((int)((intptr_t)retval) <= 32) {
        // ShellExecute failed.
        LPTSTR buffer = NULL;
        int len = ::FormatMessage(
                    FORMAT_MESSAGE_ALLOCATE_BUFFER |
                    FORMAT_MESSAGE_FROM_SYSTEM  |
                    FORMAT_MESSAGE_IGNORE_INSERTS,
                    NULL,
                    error,
                    MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT), // Default language
                    (LPTSTR)&buffer,
                    0,
                    NULL );

        if (buffer) {
            jstring errmsg = JNU_NewStringPlatform(env, buffer);
            LocalFree(buffer);
            return errmsg;
        }
    }

    return NULL;
}

/*
 * Class:     sun_awt_windows_WDesktopPeer
 * Method:    moveToTrash
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_sun_awt_windows_WDesktopPeer_moveToTrash
  (JNIEnv *env, jclass, jstring jpath)
{
    LPCTSTR pathStr = JNU_GetStringPlatformChars(env, jpath, (jboolean *)NULL);
    if (pathStr) {
        try {
            LPTSTR fileBuffer = new TCHAR[BUFFER_LIMIT];
            memset(fileBuffer, 0, BUFFER_LIMIT * sizeof(TCHAR));
            // the fileBuffer is double null terminated string
            _tcsncpy(fileBuffer, pathStr, BUFFER_LIMIT - 2);

            SHFILEOPSTRUCT fop;
            memset(&fop, 0, sizeof(SHFILEOPSTRUCT));
            fop.hwnd = NULL;
            fop.wFunc = FO_DELETE;
            fop.pFrom = fileBuffer;
            fop.fFlags = FOF_ALLOWUNDO | FOF_NOCONFIRMATION | FOF_NOERRORUI;

            int res = SHFileOperation(&fop);

            delete[] fileBuffer;
            JNU_ReleaseStringPlatformChars(env, jpath, pathStr);

            return !res ? JNI_TRUE : JNI_FALSE;
        } catch (std::bad_alloc&) {
            JNU_ReleaseStringPlatformChars(env, jpath, pathStr);
        }
    }
    return JNI_FALSE;
}

/*
 * Class:     sun_awt_windows_WDesktopPeer
 * Method:    setSuddenTerminationEnabled
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_sun_awt_windows_WDesktopPeer_setSuddenTerminationEnabled
  (JNIEnv *, jclass, jboolean enabled)
{
    isSuddenTerminationEnabled = enabled;
}

#ifdef __cplusplus
}
#endif
