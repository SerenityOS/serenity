/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
#include "jni.h"
#include "native_thread.h"
#ifdef _WIN32
#include <windows.h>
#include <process.h>
#include <vdmdbg.h>
#include <dbghelp.h>
#else /* _WIN32 */
#include <unistd.h>
#include <signal.h>
#endif /* _WIN32 */
#include "jni_tools.h"

extern "C" {

/*
 * Class:     vm_share_ProcessUtils
 * Method:    sendSignal
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_vm_share_ProcessUtils_sendSignal
(JNIEnv *env, jclass klass, jint signalNum) {
#ifdef _WIN32
/* TODO TODO TODO
        int dw;
        LPVOID lpMsgBuf;
        if (!GenerateConsoleCtrlEvent(CTRL_BREAK_EVENT, 0)) {
                dw = GetLastError();
                FormatMessage(FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM,
                                NULL,
                                dw,
                                MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
                                (LPTSTR) &lpMsgBuf,
                                0,
                                NULL
                             );
                printf("%s\n", (LPTSTR)lpMsgBuf);
                LocalFree(lpMsgBuf);
                return JNI_FALSE;
        }
        */
        return JNI_TRUE;
#else /* _WIN32 */
        if (kill(getpid(), signalNum) < 0)
                return JNI_FALSE;
        return JNI_TRUE;
#endif /* _WIN32 */
}

/*
 * Class:     vm_share_ProcessUtils
 * Method:    sendCtrlBreak
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_vm_share_ProcessUtils_sendCtrlBreak
(JNIEnv *env, jclass klass) {
#ifdef _WIN32
        int dw;
        LPVOID lpMsgBuf;
        if (!GenerateConsoleCtrlEvent(CTRL_BREAK_EVENT, 0)) {
                dw = GetLastError();
                FormatMessage(FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM,
                                NULL,
                                dw,
                                MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
                                (LPTSTR) &lpMsgBuf,
                                0,
                                NULL
                             );
                printf("%s\n", (LPTSTR)lpMsgBuf);
                LocalFree(lpMsgBuf);
                return JNI_FALSE;
        }
        return JNI_TRUE;
#else /* _WIN32 */
        if (kill(getpid(), SIGQUIT) < 0)
                return JNI_FALSE;
        return JNI_TRUE;
#endif /* _WIN32 */
}

#ifdef _WIN32
static BOOL  (WINAPI *_MiniDumpWriteDump)  (HANDLE, DWORD, HANDLE, MINIDUMP_TYPE, PMINIDUMP_EXCEPTION_INFORMATION,
                                            PMINIDUMP_USER_STREAM_INFORMATION, PMINIDUMP_CALLBACK_INFORMATION);
void reportLastError(const char *msg) {
        long errcode = GetLastError();
        if (errcode != 0) {
                DWORD len = 0;
                char *buf;
                size_t n = (size_t)FormatMessage(
                                FORMAT_MESSAGE_FROM_SYSTEM|FORMAT_MESSAGE_IGNORE_INSERTS|FORMAT_MESSAGE_ALLOCATE_BUFFER,
                                NULL,
                                errcode,
                                0,
                                (LPSTR) &buf,
                                (DWORD)len,
                                NULL);
                if (n > 3) {
                        /* Drop final '.', CR, LF */
                        if (buf[n - 1] == '\n') n--;
                        if (buf[n - 1] == '\r') n--;
                        if (buf[n - 1] == '.') n--;
                        buf[n] = '\0';
                }
                printf("%s: %s\n", msg, buf);
                LocalFree(buf);
        }
}

#endif /* _WIN32 */

jboolean doDumpCore() {
#ifdef _WIN32
        char path[MAX_PATH];
        DWORD size;
        DWORD pathLen = (DWORD) sizeof(path);
        HINSTANCE dbghelp;
        MINIDUMP_EXCEPTION_INFORMATION* pmei;

        HANDLE hProcess = GetCurrentProcess();
        DWORD processId = GetCurrentProcessId();
        HANDLE dumpFile;
        MINIDUMP_TYPE dumpType;
        static const char* cwd;
        static const char* name = "DBGHELP.DLL";

        printf("# TEST: creating Windows minidump...\n");
        size = GetSystemDirectory(path, pathLen);
        if (size > 0) {
                strcat(path, "\\");
                strcat(path, name);
                dbghelp = LoadLibrary(path);
                if (dbghelp == NULL)
                        reportLastError("Load DBGHELP.DLL from system directory");
        } else {
                printf("GetSystemDirectory returned 0\n");
        }

        // try Windows directory
        if (dbghelp == NULL) {
                size = GetWindowsDirectory(path, pathLen);
                if (size > 6) {
                        strcat(path, "\\");
                        strcat(path, name);
                        dbghelp = LoadLibrary(path);
                        if (dbghelp == NULL) {
                                reportLastError("Load DBGHELP.DLL from Windows directory");
                        }
                }
        }
        if (dbghelp == NULL) {
                printf("Failed to load DBGHELP.DLL\n");
                return JNI_FALSE;
        }

        _MiniDumpWriteDump =
                        (BOOL(WINAPI *)(HANDLE, DWORD, HANDLE, MINIDUMP_TYPE, PMINIDUMP_EXCEPTION_INFORMATION,
                                        PMINIDUMP_USER_STREAM_INFORMATION, PMINIDUMP_CALLBACK_INFORMATION))
                                        GetProcAddress(dbghelp, "MiniDumpWriteDump");

        if (_MiniDumpWriteDump == NULL) {
                printf("Failed to find MiniDumpWriteDump() in module dbghelp.dll");
                return JNI_FALSE;
        }
        dumpType = (MINIDUMP_TYPE)(MiniDumpWithFullMemory | MiniDumpWithHandleData);

        // Older versions of dbghelp.h doesn't contain all the dumptypes we want, dbghelp.h with
        // API_VERSION_NUMBER 11 or higher contains the ones we want though
#if API_VERSION_NUMBER >= 11
        dumpType = (MINIDUMP_TYPE)(dumpType | MiniDumpWithFullMemoryInfo | MiniDumpWithThreadInfo |
                        MiniDumpWithUnloadedModules);
#endif

        dumpFile = CreateFile("core.mdmp", GENERIC_WRITE, 0, NULL, CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL);

        if (dumpFile == INVALID_HANDLE_VALUE) {
                reportLastError("Failed to create file for dumping");
                return JNI_FALSE;
        }
        pmei = NULL;


        // Older versions of dbghelp.dll (the one shipped with Win2003 for example) may not support all
        // the dump types we really want. If first call fails, lets fall back to just use MiniDumpWithFullMemory then.
        if (_MiniDumpWriteDump(hProcess, processId, dumpFile, dumpType, pmei, NULL, NULL) == FALSE &&
                        _MiniDumpWriteDump(hProcess, processId, dumpFile, (MINIDUMP_TYPE)MiniDumpWithFullMemory, pmei, NULL, NULL) == FALSE) {
                reportLastError("Call to MiniDumpWriteDump() failed");
                return JNI_FALSE;
        }

        CloseHandle(dumpFile);
        printf("# TEST: minidump created\n");
        // Emulate Unix behaviour - exit process.
        ExitProcess(137);

        return JNI_TRUE;
#else /* _WIN32 */
        if (kill(getpid(), SIGSEGV) < 0)
                return JNI_FALSE;
        return JNI_TRUE;
#endif /* _WIN32 */

}

/*
 * Class:     vm_share_ProcessUtils
 * Method:    dumpCore
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_vm_share_ProcessUtils_dumpCore
  (JNIEnv *env, jclass klass)
{
        return doDumpCore();
}

/*
 * Class:     vm_share_ProcessUtils
 * Method:    getPid
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_vm_share_ProcessUtils_getPid
  (JNIEnv *env, jclass klass) {
#ifdef _WIN32
        return _getpid();
#else /* _WIN32 */
        return getpid();
#endif /* _WIN32 */
}


/*
 * Class:     vm_share_ProcessUtils
 * Method:    getPid
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_vm_share_ProcessUtils_getWindowsPid
  (JNIEnv *env, jclass klass, jlong handle) {
#ifdef _WIN32
        return GetProcessId((HANDLE) handle);
#else /* _WIN32 */
        return -1;
#endif /* _WIN32 */
}

}
