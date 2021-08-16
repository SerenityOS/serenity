/*
 * Copyright (c) 1997, 2015, Oracle and/or its affiliates. All rights reserved.
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

#include <assert.h>
#include "java_lang_ProcessImpl.h"

#include "jni.h"
#include "jvm.h"
#include "jni_util.h"
#include "io_util.h"
#include "io_util_md.h"
#include <windows.h>
#include <io.h>

/* We try to make sure that we can read and write 4095 bytes (the
 * fixed limit on Linux) to the pipe on all operating systems without
 * deadlock.  Windows 2000 inexplicably appears to need an extra 24
 * bytes of slop to avoid deadlock.
 */
#define PIPE_SIZE (4096+24)

/* We have THREE locales in action:
 * 1. Thread default locale - dictates UNICODE-to-8bit conversion
 * 2. System locale that defines the message localization
 * 3. The file name locale
 * Each locale could be an extended locale, that means that text cannot be
 * mapped to 8bit sequence without multibyte encoding.
 * VM is ready for that, if text is UTF-8.
 * Here we make the work right from the beginning.
 */
size_t os_error_message(int errnum, WCHAR* utf16_OSErrorMsg, size_t maxMsgLength) {
    size_t n = (size_t)FormatMessageW(
            FORMAT_MESSAGE_FROM_SYSTEM|FORMAT_MESSAGE_IGNORE_INSERTS,
            NULL,
            (DWORD)errnum,
            0,
            utf16_OSErrorMsg,
            (DWORD)maxMsgLength,
            NULL);
    if (n > 3) {
        // Drop final '.', CR, LF
        if (utf16_OSErrorMsg[n - 1] == L'\n') --n;
        if (utf16_OSErrorMsg[n - 1] == L'\r') --n;
        if (utf16_OSErrorMsg[n - 1] == L'.') --n;
        utf16_OSErrorMsg[n] = L'\0';
    }
    return n;
}

#define MESSAGE_LENGTH (256 + 100)
#define ARRAY_SIZE(x) (sizeof(x)/sizeof(*x))

static void
win32Error(JNIEnv *env, const WCHAR *functionName)
{
    WCHAR utf16_OSErrorMsg[MESSAGE_LENGTH - 100];
    WCHAR utf16_javaMessage[MESSAGE_LENGTH];
    /*Good suggestion about 2-bytes-per-symbol in localized error reports*/
    char  utf8_javaMessage[MESSAGE_LENGTH*2];
    const int errnum = (int)GetLastError();
    size_t n = os_error_message(errnum, utf16_OSErrorMsg, ARRAY_SIZE(utf16_OSErrorMsg));
    n = (n > 0)
        ? swprintf(utf16_javaMessage, MESSAGE_LENGTH, L"%s error=%d, %s", functionName, errnum, utf16_OSErrorMsg)
        : swprintf(utf16_javaMessage, MESSAGE_LENGTH, L"%s failed, error=%d", functionName, errnum);

    if (n > 0) /*terminate '\0' is not a part of conversion procedure*/
        n = WideCharToMultiByte(
            CP_UTF8,
            0,
            utf16_javaMessage,
            (int)n, /*by creation n <= MESSAGE_LENGTH*/
            utf8_javaMessage,
            MESSAGE_LENGTH*2,
            NULL,
            NULL);

    /*no way to die*/
    {
        const char *errorMessage = "Secondary error while OS message extraction";
        if (n > 0) {
            utf8_javaMessage[min(MESSAGE_LENGTH*2 - 1, n)] = '\0';
            errorMessage = utf8_javaMessage;
        }
        JNU_ThrowIOException(env, errorMessage);
    }
}

static void
closeSafely(HANDLE handle)
{
    if (handle != INVALID_HANDLE_VALUE)
        CloseHandle(handle);
}

static BOOL hasInheritFlag(HANDLE handle)
{
    DWORD mask;
    if (GetHandleInformation(handle, &mask)) {
        return mask & HANDLE_FLAG_INHERIT;
    }
    return FALSE;
}

#define HANDLE_STORAGE_SIZE 6
#define OFFSET_READ  0
#define OFFSET_WRITE 1
//long signed version of INVALID_HANDLE_VALUE
#define JAVA_INVALID_HANDLE_VALUE ((jlong) -1)
#define OPPOSITE_END(offset) (offset==OFFSET_READ ? OFFSET_WRITE : OFFSET_READ)

/* Pipe holder structure */
typedef struct _STDHOLDER {
    HANDLE  pipe[2];
    int     offset;
} STDHOLDER;

/* Responsible for correct initialization of the [pHolder] structure
   (that is used for handles recycling) if needs,
   and appropriate setup of IOE handle [phStd] for child process based
   on created pipe or Java handle. */
static BOOL initHolder(
    JNIEnv *env,
    jlong *pjhandles,   /* IN OUT - the handle form Java,
                                    that can be a file, console or undefined */
    STDHOLDER *pHolder, /* OUT    - initialized structure that holds pipe
                                    handles */
    HANDLE *phStd       /* OUT    - initialized handle for child process */
) {
    /* Here we test the value from Java against invalid
       handle value. We are not using INVALID_HANDLE_VALUE macro
       due to double signed/unsigned and 32/64bit ambiguity.
       Otherwise it will be easy to get the wrong
       value   0x00000000FFFFFFFF
       instead 0xFFFFFFFFFFFFFFFF. */
    if (*pjhandles != JAVA_INVALID_HANDLE_VALUE) {
        /* Java file or console redirection */
        *phStd = (HANDLE) *pjhandles;
        /* Here we set the related Java stream (Process.getXXXXStream())
           to [ProcessBuilder.NullXXXXStream.INSTANCE] value.
           The initial Java handle [*pjhandles] will be closed in
           ANY case. It is not a handle leak. */
        *pjhandles = JAVA_INVALID_HANDLE_VALUE;
    } else {
        /* Creation of parent-child pipe */
        if (!CreatePipe(
            &pHolder->pipe[OFFSET_READ],
            &pHolder->pipe[OFFSET_WRITE],
            NULL, /* we would like to inherit
                     default process access,
                     instead of 'Everybody' access */
            PIPE_SIZE))
        {
            win32Error(env, L"CreatePipe");
            return FALSE;
        } else {
            /* [thisProcessEnd] has no the inherit flag because
               the [lpPipeAttributes] param of [CreatePipe]
               had the NULL value. */
            HANDLE thisProcessEnd = pHolder->pipe[OPPOSITE_END(pHolder->offset)];
            *phStd = pHolder->pipe[pHolder->offset];
            *pjhandles = (jlong) thisProcessEnd;
        }
    }
    /* Pipe handle will be closed in the [releaseHolder] call,
       file handle will be closed in Java.
       The long-live handle need to restore the inherit flag,
       we do it later in the [prepareIOEHandleState] call. */
    SetHandleInformation(
        *phStd,
        HANDLE_FLAG_INHERIT, HANDLE_FLAG_INHERIT);
    return TRUE;
}

/* Smart recycling of pipe handles in [pHolder]. For the failed
   create process attempts, both ends of pipe need to be released.
   The [complete] has the [TRUE] value in the failed attempt. */
static void releaseHolder(BOOL complete, STDHOLDER *pHolder) {
    closeSafely(pHolder->pipe[pHolder->offset]);
    if (complete) {
        /* Error occur, close this process pipe end */
        closeSafely(pHolder->pipe[OPPOSITE_END(pHolder->offset)]);
    }
}

/* Stores and drops the inherit flag of handles that should not
   be shared with the child process by default, but can hold the
   inherit flag due to MS process birth specific. */
static void prepareIOEHandleState(
    HANDLE *stdIOE,
    BOOL *inherit)
{
    int i;
    for (i = 0; i < HANDLE_STORAGE_SIZE; ++i) {
        HANDLE hstd = stdIOE[i];
        if (INVALID_HANDLE_VALUE != hstd && hasInheritFlag(hstd)) {
            /* FALSE by default */
            inherit[i] = TRUE;
            /* Java does not need implicit inheritance for IOE handles,
               so we drop inherit flag that probably was installed by
               previous CreateProcess call that launched current process.
               We will return the handle state back after CreateProcess call.
               By clearing inherit flag we prevent "greedy grandchild" birth.
               The explicit inheritance for child process IOE handles is
               implemented in the [initHolder] call. */
            SetHandleInformation(hstd, HANDLE_FLAG_INHERIT, 0);
        }
    }
}

/* Restores the inheritance flag of handles from stored values. */
static void restoreIOEHandleState(
    const HANDLE *stdIOE,
    const BOOL *inherit)
{
    /* The set of current process standard IOE handles and
       the set of child process IOE handles can intersect.
       To restore the inherit flag right, we use backward
       array iteration. */
    int i;
    for (i = HANDLE_STORAGE_SIZE - 1; i >= 0; --i)
        if (INVALID_HANDLE_VALUE != stdIOE[i]) {
           /* Restore inherit flag for any case.
              The handle can be changed by explicit inheritance.*/
            SetHandleInformation(stdIOE[i],
                HANDLE_FLAG_INHERIT,
                inherit[i] ? HANDLE_FLAG_INHERIT : 0);
        }
}

/*
 * Class:     java_lang_ProcessImpl
 * Method:    getProcessId0
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_java_lang_ProcessImpl_getProcessId0
  (JNIEnv *env, jclass clazz, jlong handle) {
    DWORD pid = GetProcessId((HANDLE) jlong_to_ptr(handle));
    return (jint)pid;
}

/* Please, read about the MS inheritance problem
   http://support.microsoft.com/kb/315939
   and critical section/synchronized block solution. */
static jlong processCreate(
    JNIEnv *env,
    const jchar *pcmd,
    const jchar *penvBlock,
    const jchar *pdir,
    jlong *handles,
    jboolean redirectErrorStream)
{
    jlong ret = 0L;
    STARTUPINFOW si = {sizeof(si)};

    /* Handles for which the inheritance flag must be restored. */
    HANDLE stdIOE[HANDLE_STORAGE_SIZE] = {
        /* Current process standard IOE handles: JDK-7147084 */
        INVALID_HANDLE_VALUE, INVALID_HANDLE_VALUE, INVALID_HANDLE_VALUE,
        /* Child process IOE handles: JDK-6921885 */
        (HANDLE)handles[0], (HANDLE)handles[1], (HANDLE)handles[2]};
    BOOL inherit[HANDLE_STORAGE_SIZE] = {
        FALSE, FALSE, FALSE,
        FALSE, FALSE, FALSE};

    /* These three should not be closed by CloseHandle! */
    stdIOE[0] = GetStdHandle(STD_INPUT_HANDLE);
    stdIOE[1] = GetStdHandle(STD_OUTPUT_HANDLE);
    stdIOE[2] = GetStdHandle(STD_ERROR_HANDLE);

    prepareIOEHandleState(stdIOE, inherit);
    {
        /* Input */
        STDHOLDER holderIn = {{INVALID_HANDLE_VALUE, INVALID_HANDLE_VALUE}, OFFSET_READ};
        if (initHolder(env, &handles[0], &holderIn, &si.hStdInput)) {

            /* Output */
            STDHOLDER holderOut = {{INVALID_HANDLE_VALUE, INVALID_HANDLE_VALUE}, OFFSET_WRITE};
            if (initHolder(env, &handles[1], &holderOut, &si.hStdOutput)) {

                /* Error */
                STDHOLDER holderErr = {{INVALID_HANDLE_VALUE, INVALID_HANDLE_VALUE}, OFFSET_WRITE};
                BOOL success;
                if (redirectErrorStream) {
                    si.hStdError = si.hStdOutput;
                    /* Here we set the error stream to [ProcessBuilder.NullInputStream.INSTANCE]
                       value. That is in accordance with Java Doc for the redirection case.
                       The Java file for the [ handles[2] ] will be closed in ANY case. It is not
                       a handle leak. */
                    handles[2] = JAVA_INVALID_HANDLE_VALUE;
                    success = TRUE;
                } else {
                    success = initHolder(env, &handles[2], &holderErr, &si.hStdError);
                }

                if (success) {
                    PROCESS_INFORMATION pi;
                    DWORD processFlag = CREATE_NO_WINDOW | CREATE_UNICODE_ENVIRONMENT;

                    /* If the standard I/O is inherited, CREATE_NO_WINDOW must not be used. */
                    if (GetConsoleWindow() != NULL &&
                        (si.hStdInput  == stdIOE[0] ||
                         si.hStdOutput == stdIOE[1] ||
                         si.hStdError  == (redirectErrorStream ? stdIOE[1] : stdIOE[2])))
                    {
                        processFlag &= ~CREATE_NO_WINDOW;
                    }

                    si.dwFlags = STARTF_USESTDHANDLES;
                    if (!CreateProcessW(
                        NULL,             /* executable name */
                        (LPWSTR)pcmd,     /* command line */
                        NULL,             /* process security attribute */
                        NULL,             /* thread security attribute */
                        TRUE,             /* inherits system handles */
                        processFlag,      /* selected based on exe type */
                        (LPVOID)penvBlock,/* environment block */
                        (LPCWSTR)pdir,    /* change to the new current directory */
                        &si,              /* (in)  startup information */
                        &pi))             /* (out) process information */
                    {
                        win32Error(env, L"CreateProcess");
                    } else {
                        closeSafely(pi.hThread);
                        ret = (jlong)pi.hProcess;
                    }
                }
                releaseHolder(ret == 0, &holderErr);
                releaseHolder(ret == 0, &holderOut);
            }
            releaseHolder(ret == 0, &holderIn);
        }
    }
    restoreIOEHandleState(stdIOE, inherit);

    return ret;
}

JNIEXPORT jlong JNICALL
Java_java_lang_ProcessImpl_create(JNIEnv *env, jclass ignored,
                                  jstring cmd,
                                  jstring envBlock,
                                  jstring dir,
                                  jlongArray stdHandles,
                                  jboolean redirectErrorStream)
{
    jlong ret = 0;
    if (cmd != NULL && stdHandles != NULL) {
        const jchar *pcmd = (*env)->GetStringChars(env, cmd, NULL);
        if (pcmd != NULL) {
            const jchar *penvBlock = (envBlock != NULL)
                ? (*env)->GetStringChars(env, envBlock, NULL)
                : NULL;
            if (!(*env)->ExceptionCheck(env)) {
                const jchar *pdir = (dir != NULL)
                    ? (*env)->GetStringChars(env, dir, NULL)
                    : NULL;
                if (!(*env)->ExceptionCheck(env)) {
                    jlong *handles = (*env)->GetLongArrayElements(env, stdHandles, NULL);
                    if (handles != NULL) {
                        ret = processCreate(
                            env,
                            pcmd,
                            penvBlock,
                            pdir,
                            handles,
                            redirectErrorStream);
                        (*env)->ReleaseLongArrayElements(env, stdHandles, handles, 0);
                    }
                    if (pdir != NULL)
                        (*env)->ReleaseStringChars(env, dir, pdir);
                }
                if (penvBlock != NULL)
                    (*env)->ReleaseStringChars(env, envBlock, penvBlock);
            }
            (*env)->ReleaseStringChars(env, cmd, pcmd);
        }
    }
    return ret;
}

JNIEXPORT jint JNICALL
Java_java_lang_ProcessImpl_getExitCodeProcess(JNIEnv *env, jclass ignored, jlong handle)
{
    DWORD exit_code;
    if (GetExitCodeProcess((HANDLE) handle, &exit_code) == 0)
        win32Error(env, L"GetExitCodeProcess");
    return exit_code;
}

JNIEXPORT jint JNICALL
Java_java_lang_ProcessImpl_getStillActive(JNIEnv *env, jclass ignored)
{
    return STILL_ACTIVE;
}

JNIEXPORT void JNICALL
Java_java_lang_ProcessImpl_waitForInterruptibly(JNIEnv *env, jclass ignored, jlong handle)
{
    HANDLE events[2];
    events[0] = (HANDLE) handle;
    events[1] = JVM_GetThreadInterruptEvent();

    if (WaitForMultipleObjects(sizeof(events)/sizeof(events[0]), events,
                               FALSE,    /* Wait for ANY event */
                               INFINITE)  /* Wait forever */
        == WAIT_FAILED)
        win32Error(env, L"WaitForMultipleObjects");
}

JNIEXPORT void JNICALL
Java_java_lang_ProcessImpl_waitForTimeoutInterruptibly(JNIEnv *env,
                                                       jclass ignored,
                                                       jlong handle,
                                                       jlong timeoutMillis)
{
    HANDLE events[2];
    DWORD dwTimeout = (DWORD)timeoutMillis;
    DWORD result;
    events[0] = (HANDLE) handle;
    events[1] = JVM_GetThreadInterruptEvent();
    result = WaitForMultipleObjects(sizeof(events)/sizeof(events[0]), events,
                                    FALSE,    /* Wait for ANY event */
                                    dwTimeout);  /* Wait for dwTimeout */

    if (result == WAIT_FAILED)
        win32Error(env, L"WaitForMultipleObjects");
}

JNIEXPORT void JNICALL
Java_java_lang_ProcessImpl_terminateProcess(JNIEnv *env, jclass ignored, jlong handle)
{
    TerminateProcess((HANDLE) handle, 1);
}

JNIEXPORT jboolean JNICALL
Java_java_lang_ProcessImpl_isProcessAlive(JNIEnv *env, jclass ignored, jlong handle)
{
    DWORD dwExitStatus;
    GetExitCodeProcess((HANDLE) handle, &dwExitStatus);
    return dwExitStatus == STILL_ACTIVE;
}

JNIEXPORT jboolean JNICALL
Java_java_lang_ProcessImpl_closeHandle(JNIEnv *env, jclass ignored, jlong handle)
{
    return (jboolean) CloseHandle((HANDLE) handle);
}

JNIEXPORT jlong JNICALL
Java_java_lang_ProcessImpl_openForAtomicAppend(JNIEnv *env, jclass ignored, jstring path)
{
    const DWORD access = (FILE_GENERIC_WRITE & ~FILE_WRITE_DATA);
    const DWORD sharing = FILE_SHARE_READ | FILE_SHARE_WRITE;
    const DWORD disposition = OPEN_ALWAYS;
    const DWORD flagsAndAttributes = FILE_ATTRIBUTE_NORMAL;
    HANDLE h;
    WCHAR *pathbuf = pathToNTPath(env, path, JNI_FALSE);
    if (pathbuf == NULL) {
        /* Exception already pending */
        return -1;
    }
    h = CreateFileW(
        pathbuf,            /* Wide char path name */
        access,             /* Read and/or write permission */
        sharing,            /* File sharing flags */
        NULL,               /* Security attributes */
        disposition,        /* creation disposition */
        flagsAndAttributes, /* flags and attributes */
        NULL);
    free(pathbuf);
    if (h == INVALID_HANDLE_VALUE) {
        JNU_ThrowIOExceptionWithLastError(env, "CreateFileW");
    }
    return ptr_to_jlong(h);
}
