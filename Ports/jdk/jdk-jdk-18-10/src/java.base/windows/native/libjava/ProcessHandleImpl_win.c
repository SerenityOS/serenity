/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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


#include "jni.h"
#include "jvm.h"
#include "jni_util.h"
#include "java_lang_ProcessHandleImpl.h"
#include "java_lang_ProcessHandleImpl_Info.h"

#include <windows.h>
#include <tlhelp32.h>
#include <sddl.h>

static void getStatInfo(JNIEnv *env, HANDLE handle, jobject jinfo);
static void getCmdlineInfo(JNIEnv *env, HANDLE handle, jobject jinfo);
static void procToUser(JNIEnv *env, HANDLE handle, jobject jinfo);

/**************************************************************
 * Implementation of ProcessHandleImpl_Info native methods.
 */

/* Field id for jString 'command' in java.lang.ProcessHandle.Info */
static jfieldID ProcessHandleImpl_Info_commandID;

/* Field id for jString 'commandLine' in java.lang.ProcessHandleImpl.Info */
static jfieldID ProcessHandleImpl_Info_commandLineID;

/* Field id for jString[] 'arguments' in java.lang.ProcessHandle.Info */
static jfieldID ProcessHandleImpl_Info_argumentsID;

/* Field id for jlong 'totalTime' in java.lang.ProcessHandle.Info */
static jfieldID ProcessHandleImpl_Info_totalTimeID;

/* Field id for jlong 'startTime' in java.lang.ProcessHandle.Info */
static jfieldID ProcessHandleImpl_Info_startTimeID;

/* Field id for jString 'accountName' in java.lang.ProcessHandleImpl.UserPrincipal */
static jfieldID ProcessHandleImpl_Info_userID;

/**************************************************************
 * Static method to initialize field IDs.
 *
 * Class:     java_lang_ProcessHandleImpl_Info
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_java_lang_ProcessHandleImpl_00024Info_initIDs(JNIEnv *env, jclass clazz) {

    CHECK_NULL(ProcessHandleImpl_Info_commandID = (*env)->GetFieldID(env,
        clazz, "command", "Ljava/lang/String;"));
    CHECK_NULL(ProcessHandleImpl_Info_commandLineID = (*env)->GetFieldID(env,
        clazz, "commandLine", "Ljava/lang/String;"));
    CHECK_NULL(ProcessHandleImpl_Info_argumentsID = (*env)->GetFieldID(env,
        clazz, "arguments", "[Ljava/lang/String;"));
    CHECK_NULL(ProcessHandleImpl_Info_totalTimeID = (*env)->GetFieldID(env,
        clazz, "totalTime", "J"));
    CHECK_NULL(ProcessHandleImpl_Info_startTimeID = (*env)->GetFieldID(env,
        clazz, "startTime", "J"));
    CHECK_NULL(ProcessHandleImpl_Info_userID = (*env)->GetFieldID(env,
        clazz, "user", "Ljava/lang/String;"));
}
/**************************************************************
 * Static method to initialize native.
 *
 * Class:     java_lang_ProcessHandleImpl
 * Method:    initNative
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_java_lang_ProcessHandleImpl_initNative(JNIEnv *env, jclass clazz) {
}

/*
 * Block until a child process exits and return its exit code.
 */
JNIEXPORT jint JNICALL
Java_java_lang_ProcessHandleImpl_waitForProcessExit0(JNIEnv* env,
                                                     jclass junk,
                                                     jlong jpid,
                                                     jboolean reapStatus) {
    DWORD pid = (DWORD)jpid;
    DWORD exitValue = -1;
    HANDLE handle = OpenProcess(SYNCHRONIZE | PROCESS_QUERY_LIMITED_INFORMATION,
                                FALSE, pid);
    if (handle == NULL) {
        return exitValue;          // No process with that pid is alive
    }
    do {
        if (!GetExitCodeProcess(handle, &exitValue)) {
            JNU_ThrowByNameWithLastError(env,
                "java/lang/RuntimeException", "GetExitCodeProcess");
            break;
        }
        if (exitValue == STILL_ACTIVE) {
            HANDLE events[2];
            events[0] = handle;
            events[1] = JVM_GetThreadInterruptEvent();

            if (WaitForMultipleObjects(sizeof(events)/sizeof(events[0]), events,
                                       FALSE,    /* Wait for ANY event */
                                       INFINITE) /* Wait forever */
                == WAIT_FAILED) {
                JNU_ThrowByNameWithLastError(env,
                    "java/lang/RuntimeException", "WaitForMultipleObjects");
                break;
            }
        }
    } while (exitValue == STILL_ACTIVE);
    CloseHandle(handle);         // Ignore return code
    return exitValue;
}

/*
 * Returns the pid of the caller.
 *
 * Class:     java_lang_ProcessHandleImpl
 * Method:    getCurrentPid0
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_java_lang_ProcessHandleImpl_getCurrentPid0(JNIEnv *env, jclass clazz) {
    DWORD  pid = GetCurrentProcessId();
    return (jlong)pid;
}

/*
 * Returns the parent pid of the requested pid.
 *
 * Class:     java_lang_ProcessHandleImpl
 * Method:    parent0
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL
Java_java_lang_ProcessHandleImpl_parent0(JNIEnv *env,
                                         jclass clazz,
                                         jlong jpid,
                                         jlong startTime) {
    DWORD ppid = 0;
    DWORD wpid = (DWORD)jpid;
    PROCESSENTRY32 pe32;
    HANDLE hProcessSnap;
    jlong start;

    start = Java_java_lang_ProcessHandleImpl_isAlive0(env, clazz, jpid);
    if (start != startTime && start != 0 && startTime != 0) {
        return -1;
    }

    // Take a snapshot of all processes in the system.
    hProcessSnap = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
    if (hProcessSnap == INVALID_HANDLE_VALUE) {
        JNU_ThrowByName(env,
            "java/lang/RuntimeException", "snapshot not available");
        return -1;
    }

    // Retrieve information about the first process,
    pe32.dwSize = sizeof (PROCESSENTRY32);
    if (Process32First(hProcessSnap, &pe32)) {
        // Now walk the snapshot of processes, and
        do {
            if (wpid == pe32.th32ProcessID) {
                // The parent PID may be stale if that process has exited
                // and may have been reused.
                // A valid parent's start time is the same or before the child's
                jlong ppStartTime = Java_java_lang_ProcessHandleImpl_isAlive0(env,
                        clazz, pe32.th32ParentProcessID);
                if (ppStartTime > 0 && ppStartTime <= startTime) {
                    ppid = pe32.th32ParentProcessID;
                }
                break;
            }
        } while (Process32Next(hProcessSnap, &pe32));
    } else {
        JNU_ThrowByName(env,
            "java/lang/RuntimeException", "snapshot not available");
        ppid = (DWORD)-1;
    }
    CloseHandle(hProcessSnap); // Ignore return code
    return (jlong)ppid;
}

/*
 * Returns the children of the requested pid and optionally each parent.
 *
 * Class:     java_lang_ProcessHandleImpl
 * Method:    getChildPids
 * Signature: (J[J[J)I
 */
JNIEXPORT jint JNICALL
Java_java_lang_ProcessHandleImpl_getProcessPids0(JNIEnv *env,
                                                 jclass clazz,
                                                 jlong jpid,
                                                 jlongArray jarray,
                                                 jlongArray jparentArray,
                                                 jlongArray jstimesArray) {
    HANDLE hProcessSnap;
    PROCESSENTRY32 pe32;
    DWORD ppid = (DWORD)jpid;
    jlong* pids = NULL;
    jlong* ppids = NULL;
    jlong* stimes = NULL;
    jsize parentArraySize = 0;
    jsize arraySize = 0;
    jsize stimesSize = 0;
    jsize count = 0;

    arraySize = (*env)->GetArrayLength(env, jarray);
    JNU_CHECK_EXCEPTION_RETURN(env, -1);
    if (jparentArray != NULL) {
        parentArraySize = (*env)->GetArrayLength(env, jparentArray);
        JNU_CHECK_EXCEPTION_RETURN(env, -1);

        if (arraySize != parentArraySize) {
            JNU_ThrowIllegalArgumentException(env, "array sizes not equal");
            return 0;
        }
    }
    if (jstimesArray != NULL) {
        stimesSize = (*env)->GetArrayLength(env, jstimesArray);
        JNU_CHECK_EXCEPTION_RETURN(env, -1);

        if (arraySize != stimesSize) {
            JNU_ThrowIllegalArgumentException(env, "array sizes not equal");
            return 0;
        }
    }

    // Take a snapshot of all processes in the system.
    hProcessSnap = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
    if (hProcessSnap == INVALID_HANDLE_VALUE) {
        JNU_ThrowByName(env,
            "java/lang/RuntimeException", "snapshot not available");
        return 0;
    }

    // Retrieve information about the first process,
    pe32.dwSize = sizeof (PROCESSENTRY32);
    if (Process32First(hProcessSnap, &pe32)) {
        do { // Block to break out of on Exception
            pids = (*env)->GetLongArrayElements(env, jarray, NULL);
            if (pids == NULL) {
                break;
            }
            if (jparentArray != NULL) {
                ppids = (*env)->GetLongArrayElements(env, jparentArray, NULL);
                if (ppids == NULL) {
                    break;
                }
            }
            if (jstimesArray != NULL) {
                stimes = (*env)->GetLongArrayElements(env, jstimesArray, NULL);
                if (stimes == NULL) {
                    break;
                }
            }
            // Now walk the snapshot of processes, and
            // save information about each process in turn
            do {
                if (ppid == 0 ||
                    (pe32.th32ParentProcessID > 0
                    && (pe32.th32ParentProcessID == ppid))) {
                    if (count < arraySize) {
                        // Only store if it fits
                        pids[count] = (jlong) pe32.th32ProcessID;
                        if (ppids != NULL) {
                            // Store the parentPid
                            ppids[count] = (jlong) pe32.th32ParentProcessID;
                        }
                        if (stimes != NULL) {
                            // Store the process start time
                            stimes[count] =
                                    Java_java_lang_ProcessHandleImpl_isAlive0(env,
                                            clazz, (jlong) pe32.th32ProcessID);
                        }
                    }
                    count++;    // Count to tabulate size needed
                }
            } while (Process32Next(hProcessSnap, &pe32));
        } while (0);

        if (pids != NULL) {
            (*env)->ReleaseLongArrayElements(env, jarray, pids, 0);
        }
        if (ppids != NULL) {
            (*env)->ReleaseLongArrayElements(env, jparentArray, ppids, 0);
        }
        if (stimes != NULL) {
            (*env)->ReleaseLongArrayElements(env, jstimesArray, stimes, 0);
        }
    } else {
        JNU_ThrowByName(env,
            "java/lang/RuntimeException", "snapshot not available");
        count = 0;
    }
    CloseHandle(hProcessSnap);
    // If more pids than array had size for;  count will be greater than array size
    return (jint)count;
}

/**
 * Assemble a 64 bit value from two 32 bit values.
 */
static jlong jlong_from(jint high, jint low) {
    jlong result = 0;
    result = ((jlong)high << 32) | ((0x000000000ffffffff) & (jlong)low);
    return result;
}

/*
 * Get the start time in ms from 1970 from the handle.
 */
static jlong getStartTime(HANDLE handle) {
    FILETIME CreationTime, ExitTime, KernelTime, UserTime;
    if (GetProcessTimes(handle, &CreationTime, &ExitTime, &KernelTime, &UserTime)) {
        jlong start = jlong_from(CreationTime.dwHighDateTime,
                                 CreationTime.dwLowDateTime) / 10000;
        start -= 11644473600000L; // Rebase Epoch from 1601 to 1970
        return start;
    } else {
        return 0;
    }
}

/*
 * Destroy the process.
 *
 * Class:     java_lang_ProcessHandleImpl
 * Method:    destroy0
 * Signature: (Z)V
 */
JNIEXPORT jboolean JNICALL
Java_java_lang_ProcessHandleImpl_destroy0(JNIEnv *env,
                                          jclass clazz,
                                          jlong jpid,
                                          jlong startTime,
                                          jboolean force) {
    DWORD pid = (DWORD)jpid;
    jboolean ret = JNI_FALSE;
    HANDLE handle = OpenProcess(PROCESS_TERMINATE | THREAD_QUERY_INFORMATION
                                | PROCESS_QUERY_LIMITED_INFORMATION, FALSE, pid);
    if (handle != NULL) {
        jlong start = getStartTime(handle);
        if (start == startTime || startTime == 0) {
            ret = TerminateProcess(handle, 1) ? JNI_TRUE : JNI_FALSE;
        }
        CloseHandle(handle);         // Ignore return code
    }
    return ret;
}

 /*
 * Check if a process is alive.
 * Return the start time (ms since 1970) if it is available.
 * If the start time is not available return 0.
 * If the pid is invalid, return -1.
 *
 * Class:     java_lang_ProcessHandleImpl
 * Method:    isAlive0
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL
Java_java_lang_ProcessHandleImpl_isAlive0(JNIEnv *env, jclass clazz, jlong jpid) {
    DWORD pid = (DWORD)jpid;

    jlong ret = -1;
    HANDLE handle =
        OpenProcess(THREAD_QUERY_INFORMATION | PROCESS_QUERY_LIMITED_INFORMATION,
                    FALSE, pid);
    if (handle != NULL) {
        DWORD dwExitStatus;

        GetExitCodeProcess(handle, &dwExitStatus);
        if (dwExitStatus == STILL_ACTIVE) {
            ret = getStartTime(handle);
        } else {
            ret = -1;
        }
        CloseHandle(handle); // Ignore return code
   }
   return ret;
}

/*
 * Fill in the Info object from the OS information about the process.
 *
 * Class:     java_lang_ProcessHandleImpl
 * Method:    info0
 * Signature: (J)V
 */
JNIEXPORT void JNICALL
Java_java_lang_ProcessHandleImpl_00024Info_info0(JNIEnv *env,
                                                 jobject jinfo,
                                                 jlong jpid) {
    DWORD pid = (DWORD)jpid;
    int ret = 0;
    HANDLE handle =
        OpenProcess(THREAD_QUERY_INFORMATION | PROCESS_QUERY_LIMITED_INFORMATION,
                    FALSE, pid);
    if (handle == NULL) {
        return;
    }
    getStatInfo(env, handle, jinfo);
    getCmdlineInfo(env, handle, jinfo);
    procToUser(env, handle, jinfo);

    CloseHandle(handle);                // Ignore return code
}

/**
 * Read /proc/<pid>/stat and fill in the fields of the Info object.
 * The executable name, plus the user, system, and start times are gathered.
 */
static void getStatInfo(JNIEnv *env, HANDLE handle, jobject jinfo) {
    FILETIME CreationTime;
    FILETIME ExitTime;
    FILETIME KernelTime;
    FILETIME UserTime;
    jlong userTime;             // nanoseconds
    jlong totalTime;            // nanoseconds
    jlong startTime;            // nanoseconds
    UserTime.dwHighDateTime = 0;
    UserTime.dwLowDateTime = 0;
    KernelTime.dwHighDateTime = 0;
    KernelTime.dwLowDateTime = 0;
    CreationTime.dwHighDateTime = 0;
    CreationTime.dwLowDateTime = 0;

    if (GetProcessTimes(handle, &CreationTime, &ExitTime, &KernelTime, &UserTime)) {
        userTime = jlong_from(UserTime.dwHighDateTime, UserTime.dwLowDateTime);
        totalTime = jlong_from( KernelTime.dwHighDateTime, KernelTime.dwLowDateTime);
        totalTime = (totalTime + userTime) * 100;  // convert sum to nano-seconds

        startTime = jlong_from(CreationTime.dwHighDateTime,
                               CreationTime.dwLowDateTime) / 10000;
        startTime -= 11644473600000L; // Rebase Epoch from 1601 to 1970

        (*env)->SetLongField(env, jinfo,
                             ProcessHandleImpl_Info_totalTimeID, totalTime);
        JNU_CHECK_EXCEPTION(env);
        (*env)->SetLongField(env, jinfo,
                             ProcessHandleImpl_Info_startTimeID, startTime);
        JNU_CHECK_EXCEPTION(env);
    }
}

static void getCmdlineInfo(JNIEnv *env, HANDLE handle, jobject jinfo) {
    WCHAR exeName[1024];
    WCHAR *longPath;
    DWORD bufsize = sizeof(exeName)/sizeof(WCHAR);
    jstring commandObj = NULL;

    if (QueryFullProcessImageNameW(handle, 0,  exeName, &bufsize)) {
        commandObj = (*env)->NewString(env, (const jchar *)exeName,
                                       (jsize)wcslen(exeName));
    } else if (GetLastError() == ERROR_INSUFFICIENT_BUFFER) {
        bufsize = 32768;
        longPath = (WCHAR*)malloc(bufsize * sizeof(WCHAR));
        if (longPath != NULL) {
            if (QueryFullProcessImageNameW(handle, 0, longPath, &bufsize)) {
                commandObj = (*env)->NewString(env, (const jchar *)longPath,
                                               (jsize)wcslen(longPath));
            }
            free(longPath);
        }
    }
    CHECK_NULL(commandObj);
    (*env)->SetObjectField(env, jinfo,
                           ProcessHandleImpl_Info_commandID, commandObj);
}

static void procToUser(JNIEnv *env, HANDLE handle, jobject jinfo) {
#define TOKEN_LEN 256
    DWORD token_len = TOKEN_LEN;
    char token_buf[TOKEN_LEN];
    TOKEN_USER *token_user = (TOKEN_USER*)token_buf;
    HANDLE tokenHandle;
    WCHAR domain[255 + 1 + 255 + 1];    // large enough to concat with '/' and name
    WCHAR name[255 + 1];
    DWORD domainLen = sizeof(domain) - sizeof(name);
    DWORD nameLen = sizeof(name);
    SID_NAME_USE use;
    jstring s;
    int ret;

    if (!OpenProcessToken(handle, TOKEN_READ, &tokenHandle)) {
        return;
    }

    ret = GetTokenInformation(tokenHandle, TokenUser, token_user,
                              token_len, &token_len);
    CloseHandle(tokenHandle);           // always close handle
    if (!ret) {
        JNU_ThrowByNameWithLastError(env,
            "java/lang/RuntimeException", "GetTokenInformation");
        return;
    }

    if (LookupAccountSidW(NULL, token_user->User.Sid, &name[0], &nameLen,
                          &domain[0], &domainLen, &use) == 0) {
        // Name not available, convert to a String
        LPWSTR str;
        if (ConvertSidToStringSidW(token_user->User.Sid, &str) == 0) {
            return;
        }
        s = (*env)->NewString(env, (const jchar *)str, (jsize)wcslen(str));
        LocalFree(str);
    } else {
        wcscat(domain, L"\\");
        wcscat(domain, name);
        s = (*env)->NewString(env, (const jchar *)domain, (jsize)wcslen(domain));
    }
    CHECK_NULL(s);
    (*env)->SetObjectField(env, jinfo, ProcessHandleImpl_Info_userID, s);
}
