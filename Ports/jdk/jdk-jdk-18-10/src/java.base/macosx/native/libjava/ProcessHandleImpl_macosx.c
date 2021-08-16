/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
#include "jni_util.h"
#include "java_lang_ProcessHandleImpl.h"
#include "java_lang_ProcessHandleImpl_Info.h"

#include "ProcessHandleImpl_unix.h"

#include <stdio.h>
#include <errno.h>
#include <signal.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>

#include <sys/sysctl.h>

/**
 * Implementation of native ProcessHandleImpl functions for MAC OS X.
 * See ProcessHandleImpl_unix.c for more details.
 */

void os_initNative(JNIEnv *env, jclass clazz) {}

/*
 * Returns the children of the requested pid and optionally each parent.
 *
 * Use sysctl to accumulate any process whose parent pid is zero or matches.
 * The resulting pids are stored into the array of longs.
 * The number of pids is returned if they all fit.
 * If the parentArray is non-null, store the parent pid.
 * If the array is too short, excess pids are not stored and
 * the desired length is returned.
 */
jint os_getChildren(JNIEnv *env, jlong jpid, jlongArray jarray,
                    jlongArray jparentArray, jlongArray jstimesArray) {
    jlong* pids = NULL;
    jlong* ppids = NULL;
    jlong* stimes = NULL;
    jsize parentArraySize = 0;
    jsize arraySize = 0;
    jsize stimesSize = 0;
    jsize count = 0;
    size_t bufSize = 0;
    pid_t pid = (pid_t) jpid;

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

    // Get buffer size needed to read all processes
    int mib[4] = {CTL_KERN, KERN_PROC, KERN_PROC_ALL, 0};
    if (sysctl(mib, 4, NULL, &bufSize, NULL, 0) < 0) {
        JNU_ThrowByNameWithLastError(env,
            "java/lang/RuntimeException", "sysctl failed");
        return -1;
    }

    // Allocate buffer big enough for all processes
    void *buffer = malloc(bufSize);
    if (buffer == NULL) {
        JNU_ThrowOutOfMemoryError(env, "malloc failed");
        return -1;
    }

    // Read process info for all processes
    if (sysctl(mib, 4, buffer, &bufSize, NULL, 0) < 0) {
        JNU_ThrowByNameWithLastError(env,
            "java/lang/RuntimeException", "sysctl failed");
        free(buffer);
        return -1;
    }

    do { // Block to break out of on Exception
        struct kinfo_proc *kp = (struct kinfo_proc *) buffer;
        unsigned long nentries = bufSize / sizeof (struct kinfo_proc);
        long i;

        pids = (*env)->GetLongArrayElements(env, jarray, NULL);
        if (pids == NULL) {
            break;
        }
        if (jparentArray != NULL) {
            ppids  = (*env)->GetLongArrayElements(env, jparentArray, NULL);
            if (ppids == NULL) {
                break;
            }
        }
        if (jstimesArray != NULL) {
            stimes  = (*env)->GetLongArrayElements(env, jstimesArray, NULL);
            if (stimes == NULL) {
                break;
            }
        }

        // Process each entry in the buffer
        for (i = nentries; --i >= 0; ++kp) {
            if (pid == 0 || kp->kp_eproc.e_ppid == pid) {
                if (count < arraySize) {
                    // Only store if it fits
                    pids[count] = (jlong) kp->kp_proc.p_pid;
                    if (ppids != NULL) {
                        // Store the parentPid
                        ppids[count] = (jlong) kp->kp_eproc.e_ppid;
                    }
                    if (stimes != NULL) {
                        // Store the process start time
                        jlong startTime = kp->kp_proc.p_starttime.tv_sec * 1000 +
                                          kp->kp_proc.p_starttime.tv_usec / 1000;
                        stimes[count] = startTime;
                    }
                }
                count++; // Count to tabulate size needed
            }
        }
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

    free(buffer);
    // If more pids than array had size for; count will be greater than array size
    return count;
}

/**
 * Use sysctl and return the ppid, total cputime and start time.
 * Return: -1 is fail;  >=  0 is parent pid
 * 'total' will contain the running time of 'pid' in nanoseconds.
 * 'start' will contain the start time of 'pid' in milliseconds since epoch.
 */
pid_t os_getParentPidAndTimings(JNIEnv *env, pid_t jpid,
                                jlong *totalTime, jlong *startTime) {

    const pid_t pid = (pid_t) jpid;
    pid_t ppid = -1;
    struct kinfo_proc kp;
    size_t bufSize = sizeof kp;

    // Read the process info for the specific pid
    int mib[4] = {CTL_KERN, KERN_PROC, KERN_PROC_PID, pid};

    if (sysctl(mib, 4, &kp, &bufSize, NULL, 0) < 0) {
        JNU_ThrowByNameWithLastError(env,
            "java/lang/RuntimeException", "sysctl failed");
        return -1;
    }
    if (bufSize > 0 && kp.kp_proc.p_pid == pid) {
        *startTime = (jlong) (kp.kp_proc.p_starttime.tv_sec * 1000 +
                              kp.kp_proc.p_starttime.tv_usec / 1000);
        ppid = kp.kp_eproc.e_ppid;
    }

    // Get cputime if for current process
    if (pid == getpid()) {
        struct rusage usage;
        if (getrusage(RUSAGE_SELF, &usage) == 0) {
          jlong microsecs =
              usage.ru_utime.tv_sec * 1000 * 1000 + usage.ru_utime.tv_usec +
              usage.ru_stime.tv_sec * 1000 * 1000 + usage.ru_stime.tv_usec;
          *totalTime = microsecs * 1000;
        }
    }

    return ppid;

}

/**
 * Return the uid of a process or -1 on error
 */
static uid_t getUID(pid_t pid) {
    struct kinfo_proc kp;
    size_t bufSize = sizeof kp;

    // Read the process info for the specific pid
    int mib[4] = {CTL_KERN, KERN_PROC, KERN_PROC_PID, pid};

    if (sysctl(mib, 4, &kp, &bufSize, NULL, 0) == 0) {
        if (bufSize > 0 && kp.kp_proc.p_pid == pid) {
            return kp.kp_eproc.e_ucred.cr_uid;
        }
    }
    return (uid_t)-1;
}

/**
 * Retrieve the command and arguments for the process and store them
 * into the Info object.
 */
void os_getCmdlineAndUserInfo(JNIEnv *env, jobject jinfo, pid_t pid) {
    int mib[3], maxargs, nargs, i;
    size_t size;
    char *args, *cp, *sp, *np;

    // Get the UID first. This is done here because it is cheap to do it here
    // on other platforms like Linux/Solaris/AIX where the uid comes from the
    // same source like the command line info.
    unix_getUserInfo(env, jinfo, getUID(pid));

    // Get the maximum size of the arguments
    mib[0] = CTL_KERN;
    mib[1] = KERN_ARGMAX;
    size = sizeof(maxargs);
    if (sysctl(mib, 2, &maxargs, &size, NULL, 0) == -1) {
            JNU_ThrowByNameWithLastError(env,
                "java/lang/RuntimeException", "sysctl failed");
        return;
    }

    // Allocate an args buffer and get the arguments
    args = (char *)malloc(maxargs);
    if (args == NULL) {
        JNU_ThrowOutOfMemoryError(env, "malloc failed");
        return;
    }

    do {            // a block to break out of on error
        char *argsEnd;
        jstring cmdexe = NULL;

        mib[0] = CTL_KERN;
        mib[1] = KERN_PROCARGS2;
        mib[2] = pid;
        size = (size_t) maxargs;
        if (sysctl(mib, 3, args, &size, NULL, 0) == -1) {
            if (errno != EINVAL) {
                JNU_ThrowByNameWithLastError(env,
                    "java/lang/RuntimeException", "sysctl failed");
            }
            break;
        }
        memcpy(&nargs, args, sizeof(nargs));

        cp = &args[sizeof(nargs)];      // Strings start after nargs
        argsEnd = &args[size];

        // Store the command executable path
        if ((cmdexe = JNU_NewStringPlatform(env, cp)) == NULL) {
            break;
        }

        // Skip trailing nulls after the executable path
        for (cp = cp + strnlen(cp, argsEnd - cp); cp < argsEnd; cp++) {
            if (*cp != '\0') {
                break;
            }
        }

        unix_fillArgArray(env, jinfo, nargs, cp, argsEnd, cmdexe, NULL);
    } while (0);
    // Free the arg buffer
    free(args);
}

