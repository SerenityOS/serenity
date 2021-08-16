/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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

#include <windows.h>
#include <Sddl.h>
#include <string.h>

#include "sun_tools_attach_VirtualMachineImpl.h"

/* kernel32 */
typedef HINSTANCE (WINAPI* GetModuleHandleFunc) (LPCTSTR);
typedef FARPROC (WINAPI* GetProcAddressFunc)(HMODULE, LPCSTR);

/* only on Windows 64-bit or 32-bit application running under WOW64 */
typedef BOOL (WINAPI *IsWow64ProcessFunc) (HANDLE, PBOOL);

static GetModuleHandleFunc _GetModuleHandle;
static GetProcAddressFunc _GetProcAddress;
static IsWow64ProcessFunc _IsWow64Process;

/* psapi */
typedef BOOL  (WINAPI *EnumProcessModulesFunc)  (HANDLE, HMODULE *, DWORD, LPDWORD );
typedef DWORD (WINAPI *GetModuleFileNameExFunc) ( HANDLE, HMODULE, LPTSTR, DWORD );

/* exported function in target VM */
typedef jint (WINAPI* EnqueueOperationFunc)
    (const char* cmd, const char* arg1, const char* arg2, const char* arg3, const char* pipename);

/* OpenProcess with SE_DEBUG_NAME privilege */
static HANDLE
doPrivilegedOpenProcess(DWORD dwDesiredAccess, BOOL bInheritHandle, DWORD dwProcessId);

/* convert jstring to C string */
static void jstring_to_cstring(JNIEnv* env, jstring jstr, char* cstr, int len);


/*
 * Data copied to target process
 */

#define MAX_LIBNAME_LENGTH      16
#define MAX_FUNC_LENGTH         32
#define MAX_CMD_LENGTH          16
#define MAX_ARG_LENGTH          1024
#define MAX_ARGS                3
#define MAX_PIPE_NAME_LENGTH    256

typedef struct {
   GetModuleHandleFunc _GetModuleHandle;
   GetProcAddressFunc _GetProcAddress;
   char jvmLib[MAX_LIBNAME_LENGTH];         /* "jvm.dll" */
   char func1[MAX_FUNC_LENGTH];
   char func2[MAX_FUNC_LENGTH];
   char cmd[MAX_CMD_LENGTH];                /* "load", "dump", ...      */
   char arg[MAX_ARGS][MAX_ARG_LENGTH];      /* arguments to command     */
   char pipename[MAX_PIPE_NAME_LENGTH];
} DataBlock;

/*
 * Return codes from enqueue function executed in target VM
 */
#define ERR_OPEN_JVM_FAIL           200
#define ERR_GET_ENQUEUE_FUNC_FAIL   201

/*
 * Declare library specific JNI_Onload entry if static build
 */
DEF_STATIC_JNI_OnLoad

/*
 * Code copied to target process
 */
#pragma check_stack (off)
/* Switch off all runtime checks (checks caused by /RTC<x>). They cause the
 * generated code to contain relative jumps to check functions which make
 * the code position dependent. */
#pragma runtime_checks ("scu", off)
DWORD WINAPI jvm_attach_thread_func(DataBlock *pData)
{
    HINSTANCE h;
    EnqueueOperationFunc addr;

    h = pData->_GetModuleHandle(pData->jvmLib);
    if (h == NULL) {
        return ERR_OPEN_JVM_FAIL;
    }

    addr = (EnqueueOperationFunc)(pData->_GetProcAddress(h, pData->func1));
    if (addr == NULL) {
        addr = (EnqueueOperationFunc)(pData->_GetProcAddress(h, pData->func2));
    }
    if (addr == NULL) {
        return ERR_GET_ENQUEUE_FUNC_FAIL;
    }

    /* "null" command - does nothing in the target VM */
    if (pData->cmd[0] == '\0') {
        return 0;
    } else {
        return (*addr)(pData->cmd, pData->arg[0], pData->arg[1], pData->arg[2], pData->pipename);
    }
}

/* This function marks the end of jvm_attach_thread_func. */
void jvm_attach_thread_func_end (void) {
}
#pragma check_stack
#pragma runtime_checks ("scu", restore)

/*
 * Class:     sun_tools_attach_VirtualMachineImpl
 * Method:    init
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_tools_attach_VirtualMachineImpl_init
  (JNIEnv *env, jclass cls)
{
    // All following APIs exist on Windows XP with SP2/Windows Server 2008
    _GetModuleHandle = (GetModuleHandleFunc)GetModuleHandle;
    _GetProcAddress = (GetProcAddressFunc)GetProcAddress;
    _IsWow64Process = (IsWow64ProcessFunc)IsWow64Process;
}


/*
 * Class:     sun_tools_attach_VirtualMachineImpl
 * Method:    generateStub
 * Signature: ()[B
 */
JNIEXPORT jbyteArray JNICALL Java_sun_tools_attach_VirtualMachineImpl_generateStub
  (JNIEnv *env, jclass cls)
{
    /*
     * We should replace this with a real stub generator at some point
     */
    DWORD len;
    jbyteArray array;

    len = (DWORD)((LPBYTE) jvm_attach_thread_func_end - (LPBYTE) jvm_attach_thread_func);
    array= (*env)->NewByteArray(env, (jsize)len);
    if (array != NULL) {
        (*env)->SetByteArrayRegion(env, array, 0, (jint)len, (jbyte*)&jvm_attach_thread_func);
    }
    return array;
}

/*
 * Class:     sun_tools_attach_VirtualMachineImpl
 * Method:    openProcess
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_sun_tools_attach_VirtualMachineImpl_openProcess
  (JNIEnv *env, jclass cls, jint pid)
{
    HANDLE hProcess = NULL;

    if (pid == (jint) GetCurrentProcessId()) {
        /* process is attaching to itself; get a pseudo handle instead */
        hProcess = GetCurrentProcess();
        /* duplicate the pseudo handle so it can be used in more contexts */
        if (DuplicateHandle(hProcess, hProcess, hProcess, &hProcess,
                PROCESS_ALL_ACCESS, FALSE, 0) == 0) {
            /*
             * Could not duplicate the handle which isn't a good sign,
             * but we'll try again with OpenProcess() below.
             */
            hProcess = NULL;
        }
    }

    if (hProcess == NULL) {
        /*
         * Attempt to open process. If it fails then we try to enable the
         * SE_DEBUG_NAME privilege and retry.
         */
        hProcess = OpenProcess(PROCESS_ALL_ACCESS, FALSE, (DWORD)pid);
        if (hProcess == NULL && GetLastError() == ERROR_ACCESS_DENIED) {
            hProcess = doPrivilegedOpenProcess(PROCESS_ALL_ACCESS, FALSE,
                           (DWORD)pid);
        }

        if (hProcess == NULL) {
            if (GetLastError() == ERROR_INVALID_PARAMETER) {
                JNU_ThrowIOException(env, "no such process");
            } else {
                char err_mesg[255];
                /* include the last error in the default detail message */
                sprintf(err_mesg, "OpenProcess(pid=%d) failed; LastError=0x%x",
                    (int)pid, (int)GetLastError());
                JNU_ThrowIOExceptionWithLastError(env, err_mesg);
            }
            return (jlong)0;
        }
    }

    /*
     * On Windows 64-bit we need to handle 32-bit tools trying to attach to 64-bit
     * processes (and visa versa). X-architecture attaching is currently not supported
     * by this implementation.
     */
    if (_IsWow64Process != NULL) {
        BOOL isCurrent32bit, isTarget32bit;
        (*_IsWow64Process)(GetCurrentProcess(), &isCurrent32bit);
        (*_IsWow64Process)(hProcess, &isTarget32bit);

        if (isCurrent32bit != isTarget32bit) {
            CloseHandle(hProcess);
            #ifdef _WIN64
              JNU_ThrowByName(env, "com/sun/tools/attach/AttachNotSupportedException",
                  "Unable to attach to 32-bit process running under WOW64");
            #else
              JNU_ThrowByName(env, "com/sun/tools/attach/AttachNotSupportedException",
                  "Unable to attach to 64-bit process");
            #endif
        }
    }

    return (jlong)hProcess;
}


/*
 * Class:     sun_tools_attach_VirtualMachineImpl
 * Method:    closeProcess
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_sun_tools_attach_VirtualMachineImpl_closeProcess
  (JNIEnv *env, jclass cls, jlong hProcess)
{
    CloseHandle((HANDLE)hProcess);
}


/*
 * Class:     sun_tools_attach_VirtualMachineImpl
 * Method:    createPipe
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_sun_tools_attach_VirtualMachineImpl_createPipe
  (JNIEnv *env, jclass cls, jstring pipename)
{
    HANDLE hPipe;
    char name[MAX_PIPE_NAME_LENGTH];

    SECURITY_ATTRIBUTES sa;
    LPSECURITY_ATTRIBUTES lpSA = NULL;
    // Custom Security Descriptor is required here to "get" Medium Integrity Level.
    // In order to allow Medium Integrity Level clients to open
    // and use a NamedPipe created by an High Integrity Level process.
    TCHAR *szSD = TEXT("D:")                  // Discretionary ACL
                  TEXT("(A;OICI;GRGW;;;WD)")  // Allow read/write to Everybody
                  TEXT("(A;OICI;GA;;;SY)")    // Allow full control to System
                  TEXT("(A;OICI;GA;;;BA)");   // Allow full control to Administrators

    sa.nLength = sizeof(SECURITY_ATTRIBUTES);
    sa.bInheritHandle = FALSE;
    sa.lpSecurityDescriptor = NULL;

    if (ConvertStringSecurityDescriptorToSecurityDescriptor
          (szSD, SDDL_REVISION_1, &(sa.lpSecurityDescriptor), NULL)) {
        lpSA = &sa;
    }

    jstring_to_cstring(env, pipename, name, MAX_PIPE_NAME_LENGTH);

    hPipe = CreateNamedPipe(
          name,                         // pipe name
          PIPE_ACCESS_INBOUND,          // read access
          PIPE_TYPE_BYTE |              // byte mode
            PIPE_READMODE_BYTE |
            PIPE_WAIT,                  // blocking mode
          1,                            // max. instances
          128,                          // output buffer size
          8192,                         // input buffer size
          NMPWAIT_USE_DEFAULT_WAIT,     // client time-out
          lpSA);        // security attributes

    LocalFree(sa.lpSecurityDescriptor);

    if (hPipe == INVALID_HANDLE_VALUE) {
        JNU_ThrowIOExceptionWithLastError(env, "CreateNamedPipe failed");
    }
    return (jlong)hPipe;
}

/*
 * Class:     sun_tools_attach_VirtualMachineImpl
 * Method:    closePipe
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_sun_tools_attach_VirtualMachineImpl_closePipe
  (JNIEnv *env, jclass cls, jlong hPipe)
{
    CloseHandle((HANDLE)hPipe);
}

/*
 * Class:     sun_tools_attach_VirtualMachineImpl
 * Method:    connectPipe
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_sun_tools_attach_VirtualMachineImpl_connectPipe
  (JNIEnv *env, jclass cls, jlong hPipe)
{
    BOOL fConnected;

    fConnected = ConnectNamedPipe((HANDLE)hPipe, NULL) ?
        TRUE : (GetLastError() == ERROR_PIPE_CONNECTED);
    if (!fConnected) {
        JNU_ThrowIOExceptionWithLastError(env, "ConnectNamedPipe failed");
    }
}

/*
 * Class:     sun_tools_attach_VirtualMachineImpl
 * Method:    readPipe
 * Signature: (J[BII)I
 */
JNIEXPORT jint JNICALL Java_sun_tools_attach_VirtualMachineImpl_readPipe
  (JNIEnv *env, jclass cls, jlong hPipe, jbyteArray ba, jint off, jint baLen)
{
    unsigned char buf[128];
    DWORD len, nread, remaining;
    BOOL fSuccess;

    len = sizeof(buf);
    remaining = (DWORD)(baLen - off);
    if (len > remaining) {
        len = remaining;
    }

    fSuccess = ReadFile(
         (HANDLE)hPipe,         // handle to pipe
         buf,                   // buffer to receive data
         len,                   // size of buffer
         &nread,                // number of bytes read
         NULL);                 // not overlapped I/O

    if (!fSuccess) {
        if (GetLastError() == ERROR_BROKEN_PIPE) {
            return (jint)-1;
        } else {
            JNU_ThrowIOExceptionWithLastError(env, "ReadFile");
        }
    } else {
        if (nread == 0) {
            return (jint)-1;        // EOF
        } else {
            (*env)->SetByteArrayRegion(env, ba, off, (jint)nread, (jbyte *)(buf));
        }
    }

    return (jint)nread;
}


/*
 * Class:     sun_tools_attach_VirtualMachineImpl
 * Method:    enqueue
 * Signature: (JZLjava/lang/String;[Ljava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_sun_tools_attach_VirtualMachineImpl_enqueue
  (JNIEnv *env, jclass cls, jlong handle, jbyteArray stub, jstring cmd,
   jstring pipename, jobjectArray args)
{
    DataBlock data;
    DataBlock* pData;
    DWORD* pCode;
    DWORD stubLen;
    HANDLE hProcess, hThread;
    jint argsLen, i;
    jbyte* stubCode;
    jboolean isCopy;

    /*
     * Setup data to copy to target process
     */
    data._GetModuleHandle = _GetModuleHandle;
    data._GetProcAddress = _GetProcAddress;

    strcpy(data.jvmLib, "jvm");
    strcpy(data.func1, "JVM_EnqueueOperation");
    strcpy(data.func2, "_JVM_EnqueueOperation@20");

    /*
     * Command and arguments
     */
    jstring_to_cstring(env, cmd, data.cmd, MAX_CMD_LENGTH);
    argsLen = (*env)->GetArrayLength(env, args);

    if (argsLen > 0) {
        if (argsLen > MAX_ARGS) {
            JNU_ThrowInternalError(env, "Too many arguments");
            return;
        }
        for (i=0; i<argsLen; i++) {
            jobject obj = (*env)->GetObjectArrayElement(env, args, i);
            if (obj == NULL) {
                data.arg[i][0] = '\0';
            } else {
                jstring_to_cstring(env, obj, data.arg[i], MAX_ARG_LENGTH);
            }
            if ((*env)->ExceptionOccurred(env)) return;
        }
    }
    for (i = argsLen; i < MAX_ARGS; i++) {
        data.arg[i][0] = '\0';
    }

    /* pipe name */
    jstring_to_cstring(env, pipename, data.pipename, MAX_PIPE_NAME_LENGTH);

    /*
     * Allocate memory in target process for data and code stub
     * (assumed aligned and matches architecture of target process)
     */
    hProcess = (HANDLE)handle;

    pData = (DataBlock*) VirtualAllocEx( hProcess, 0, sizeof(DataBlock), MEM_COMMIT, PAGE_READWRITE );
    if (pData == NULL) {
        JNU_ThrowIOExceptionWithLastError(env, "VirtualAllocEx failed");
        return;
    }
    WriteProcessMemory( hProcess, (LPVOID)pData, (LPCVOID)&data, (SIZE_T)sizeof(DataBlock), NULL );


    stubLen = (DWORD)(*env)->GetArrayLength(env, stub);
    stubCode = (*env)->GetByteArrayElements(env, stub, &isCopy);

    if ((*env)->ExceptionOccurred(env)) return;

    pCode = (PDWORD) VirtualAllocEx( hProcess, 0, stubLen, MEM_COMMIT, PAGE_EXECUTE_READWRITE );
    if (pCode == NULL) {
        JNU_ThrowIOExceptionWithLastError(env, "VirtualAllocEx failed");
        VirtualFreeEx(hProcess, pData, 0, MEM_RELEASE);
        (*env)->ReleaseByteArrayElements(env, stub, stubCode, JNI_ABORT);
        return;
    }
    WriteProcessMemory( hProcess, (LPVOID)pCode, (LPCVOID)stubCode, (SIZE_T)stubLen, NULL );
    (*env)->ReleaseByteArrayElements(env, stub, stubCode, JNI_ABORT);

    /*
     * Create thread in target process to execute code
     */
    hThread = CreateRemoteThread( hProcess,
                                  NULL,
                                  0,
                                  (LPTHREAD_START_ROUTINE) pCode,
                                  pData,
                                  0,
                                  NULL );
    if (hThread != NULL) {
        if (WaitForSingleObject(hThread, INFINITE) != WAIT_OBJECT_0) {
            JNU_ThrowIOExceptionWithLastError(env, "WaitForSingleObject failed");
        } else {
            DWORD exitCode;
            GetExitCodeThread(hThread, &exitCode);
            if (exitCode) {
                switch (exitCode) {
                    case ERR_OPEN_JVM_FAIL :
                        JNU_ThrowIOException(env,
                            "jvm.dll not loaded by target process");
                        break;
                    case ERR_GET_ENQUEUE_FUNC_FAIL :
                        JNU_ThrowIOException(env,
                            "Unable to enqueue operation: the target VM does not support attach mechanism");
                        break;
                    default : {
                        char errmsg[128];
                        sprintf(errmsg, "Remote thread failed for unknown reason (%d)", exitCode);
                        JNU_ThrowInternalError(env, errmsg);
                    }
                }
            }
        }
        CloseHandle(hThread);
    } else {
        if (GetLastError() == ERROR_NOT_ENOUGH_MEMORY) {
            //
            // This error will occur when attaching to a process belonging to
            // another terminal session. See "Remarks":
            // http://msdn.microsoft.com/en-us/library/ms682437%28VS.85%29.aspx
            //
            JNU_ThrowIOException(env,
                "Insufficient memory or insufficient privileges to attach");
        } else {
            JNU_ThrowIOExceptionWithLastError(env, "CreateRemoteThread failed");
        }
    }

    VirtualFreeEx(hProcess, pCode, 0, MEM_RELEASE);
    VirtualFreeEx(hProcess, pData, 0, MEM_RELEASE);
}

/*
 * Attempts to enable the SE_DEBUG_NAME privilege and open the given process.
 */
static HANDLE
doPrivilegedOpenProcess(DWORD dwDesiredAccess, BOOL bInheritHandle, DWORD dwProcessId) {
    HANDLE hToken;
    HANDLE hProcess = NULL;
    LUID luid;
    TOKEN_PRIVILEGES tp, tpPrevious;
    DWORD retLength, error;

    /*
     * Get the access token
     */
    if (!OpenThreadToken(GetCurrentThread(),
                         TOKEN_ADJUST_PRIVILEGES|TOKEN_QUERY,
                         FALSE,
                         &hToken)) {
        if (GetLastError() != ERROR_NO_TOKEN) {
            return (HANDLE)NULL;
        }

        /*
         * No access token for the thread so impersonate the security context
         * of the process.
         */
        if (!ImpersonateSelf(SecurityImpersonation)) {
            return (HANDLE)NULL;
        }
        if (!OpenThreadToken(GetCurrentThread(),
                             TOKEN_ADJUST_PRIVILEGES|TOKEN_QUERY,
                             FALSE,
                             &hToken)) {
            return (HANDLE)NULL;
        }
    }

    /*
     * Get LUID for the privilege
     */
    if(!LookupPrivilegeValue(NULL, SE_DEBUG_NAME, &luid)) {
        error = GetLastError();
        CloseHandle(hToken);
        SetLastError(error);
        return (HANDLE)NULL;
    }

    /*
     * Enable the privilege
     */
    ZeroMemory(&tp, sizeof(tp));
    tp.PrivilegeCount = 1;
    tp.Privileges[0].Attributes = SE_PRIVILEGE_ENABLED;
    tp.Privileges[0].Luid = luid;

    error = 0;
    if (AdjustTokenPrivileges(hToken,
                              FALSE,
                              &tp,
                              sizeof(TOKEN_PRIVILEGES),
                              &tpPrevious,
                              &retLength)) {
        /*
         * If we enabled the privilege then attempt to open the
         * process.
         */
        if (GetLastError() == ERROR_SUCCESS) {
            hProcess = OpenProcess(dwDesiredAccess, bInheritHandle, dwProcessId);
            if (hProcess == NULL) {
                error = GetLastError();
            }
        } else {
            error = ERROR_ACCESS_DENIED;
        }

        /*
         * Revert to the previous privileges
         */
        AdjustTokenPrivileges(hToken,
                              FALSE,
                              &tpPrevious,
                              retLength,
                              NULL,
                              NULL);
    } else {
        error = GetLastError();
    }


    /*
     * Close token and restore error
     */
    CloseHandle(hToken);
    SetLastError(error);

    return hProcess;
}

/* convert jstring to C string */
static void jstring_to_cstring(JNIEnv* env, jstring jstr, char* cstr, int len) {
    jboolean isCopy;
    const char* str;

    if (jstr == NULL) {
        cstr[0] = '\0';
    } else {
        str = JNU_GetStringPlatformChars(env, jstr, &isCopy);
        if ((*env)->ExceptionOccurred(env)) return;

        strncpy(cstr, str, len);
        cstr[len-1] = '\0';
        if (isCopy) {
            JNU_ReleaseStringPlatformChars(env, jstr, str);
        }
    }
}
