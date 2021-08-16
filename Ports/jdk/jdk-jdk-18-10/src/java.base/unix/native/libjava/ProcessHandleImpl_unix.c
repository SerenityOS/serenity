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
#include "jni_util.h"
#include "java_lang_ProcessHandleImpl.h"
#include "java_lang_ProcessHandleImpl_Info.h"

#include "ProcessHandleImpl_unix.h"


#include <stdio.h>
#include <errno.h>
#include <fcntl.h>
#include <signal.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <dirent.h>
#include <ctype.h>
#include <limits.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/wait.h>

#include <pwd.h>

#if defined(_AIX)
  #include <sys/procfs.h>
  #define DIR DIR64
  #define dirent dirent64
  #define opendir opendir64
  #define readdir readdir64
  #define closedir closedir64
#endif

/**
 * This file contains the implementation of the native ProcessHandleImpl
 * functions which are common to all Unix variants.
 *
 * The currently supported Unix variants are Solaris, Linux, MaxOS X and AIX.
 * The various similarities and differences between these systems make it hard
 * to find a clear boundary between platform specific and shared code.
 *
 * In order to ease code sharing between the platforms while still keeping the
 * code as clean as possible (i.e. free of preprocessor macros) we use the
 * following source code layout (remember that ProcessHandleImpl_unix.c will
 * be compiled on EVERY Unix platform while ProcessHandleImpl_<os>.c will be
 * only compiled on the specific OS):
 *
 * - all the JNI wrappers for the ProcessHandleImpl functions go into this file
 * - if their implementation is common on ALL the supported Unix platforms it
 *   goes right into the JNI wrappers
 * - if the whole function or substantial parts of it are platform dependent,
 *   the implementation goes into os_<function_name> functions in
 *   ProcessHandleImpl_<os>.c
 * - if at least two platforms implement an os_<function_name> function in the
 *   same way, this implementation is factored out into unix_<function_name>,
 *   placed into this file and called from the corresponding os_<function_name>
 *   function.
 * - For convenience, all the os_ and unix_ functions are declared in
 *   ProcessHandleImpl_unix.h which is included into every
 *   ProcessHandleImpl_<os>.c file.
 *
 * Example 1:
 * ----------
 * The implementation of Java_java_lang_ProcessHandleImpl_initNative()
 * is the same on all platforms except on Linux where it initilizes one
 * additional field. So we place the implementation right into
 * Java_java_lang_ProcessHandleImpl_initNative() but add call to
 * os_init() at the end of the function which is empty on all platforms
 * except Linux where it performs the additionally initializations.
 *
 * Example 2:
 * ----------
 * The implementation of Java_java_lang_ProcessHandleImpl_00024Info_info0 is the
 * same on Solaris and AIX but different on Linux and MacOSX. We therefore simply
 * call the helpers os_getParentPidAndTimings() and os_getCmdlineAndUserInfo().
 * The Linux and MaxOS X versions of these functions (in the corresponding files
 * ProcessHandleImpl_linux.c and ProcessHandleImpl_macosx.c) directly contain
 * the platform specific implementations while the Solaris and AIX
 * implementations simply call back to unix_getParentPidAndTimings() and
 * unix_getCmdlineAndUserInfo() which are implemented right in this file.
 *
 * The term "same implementation" is still a question of interpretation. It my
 * be acceptable to have a few ifdef'ed lines if that allows the sharing of a
 * huge function. On the other hand, if the platform specific code in a shared
 * function grows over a certain limit, it may be better to refactor that
 * functionality into corresponding, platform-specific os_ functions.
 */


#ifndef WIFEXITED
#define WIFEXITED(status) (((status)&0xFF) == 0)
#endif

#ifndef WEXITSTATUS
#define WEXITSTATUS(status) (((status)>>8)&0xFF)
#endif

#ifndef WIFSIGNALED
#define WIFSIGNALED(status) (((status)&0xFF) > 0 && ((status)&0xFF00) == 0)
#endif

#ifndef WTERMSIG
#define WTERMSIG(status) ((status)&0x7F)
#endif

/* The child exited because of a signal.
 * The best value to return is 0x80 + signal number,
 * because that is what all Unix shells do, and because
 * it allows callers to distinguish between process exit and
 * process death by signal.
 */
#define WTERMSIG_RETURN(status) (WTERMSIG(status) + 0x80)

#define RESTARTABLE(_cmd, _result) do { \
  do { \
    _result = _cmd; \
  } while((_result == -1) && (errno == EINTR)); \
} while(0)

#define RESTARTABLE_RETURN_PTR(_cmd, _result) do { \
  do { \
    _result = _cmd; \
  } while((_result == NULL) && (errno == EINTR)); \
} while(0)


/* Field id for jString 'command' in java.lang.ProcessHandleImpl.Info */
jfieldID ProcessHandleImpl_Info_commandID;

/* Field id for jString 'commandLine' in java.lang.ProcessHandleImpl.Info */
jfieldID ProcessHandleImpl_Info_commandLineID;

/* Field id for jString[] 'arguments' in java.lang.ProcessHandleImpl.Info */
jfieldID ProcessHandleImpl_Info_argumentsID;

/* Field id for jlong 'totalTime' in java.lang.ProcessHandleImpl.Info */
jfieldID ProcessHandleImpl_Info_totalTimeID;

/* Field id for jlong 'startTime' in java.lang.ProcessHandleImpl.Info */
jfieldID ProcessHandleImpl_Info_startTimeID;

/* Field id for jString 'user' in java.lang.ProcessHandleImpl.Info */
jfieldID ProcessHandleImpl_Info_userID;

/* Size of password or group entry when not available via sysconf */
#define ENT_BUF_SIZE   1024
/* The value for the size of the buffer used by getpwuid_r(). The result of */
/* sysconf(_SC_GETPW_R_SIZE_MAX) if available or ENT_BUF_SIZE otherwise. */
static long getpw_buf_size;

/**************************************************************
 * Static method to initialize field IDs and the ticks per second rate.
 *
 * Class:     java_lang_ProcessHandleImpl_Info
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_java_lang_ProcessHandleImpl_00024Info_initIDs(JNIEnv *env, jclass clazz) {

    CHECK_NULL(ProcessHandleImpl_Info_commandID =
            (*env)->GetFieldID(env, clazz, "command", "Ljava/lang/String;"));
    CHECK_NULL(ProcessHandleImpl_Info_commandLineID =
            (*env)->GetFieldID(env, clazz, "commandLine", "Ljava/lang/String;"));
    CHECK_NULL(ProcessHandleImpl_Info_argumentsID =
            (*env)->GetFieldID(env, clazz, "arguments", "[Ljava/lang/String;"));
    CHECK_NULL(ProcessHandleImpl_Info_totalTimeID =
            (*env)->GetFieldID(env, clazz, "totalTime", "J"));
    CHECK_NULL(ProcessHandleImpl_Info_startTimeID =
            (*env)->GetFieldID(env, clazz, "startTime", "J"));
    CHECK_NULL(ProcessHandleImpl_Info_userID =
            (*env)->GetFieldID(env, clazz, "user", "Ljava/lang/String;"));
}

/***********************************************************
 * Static method to initialize platform dependent constants.
 *
 * Class:     java_lang_ProcessHandleImpl
 * Method:    initNative
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_java_lang_ProcessHandleImpl_initNative(JNIEnv *env, jclass clazz) {
    getpw_buf_size = sysconf(_SC_GETPW_R_SIZE_MAX);
    if (getpw_buf_size == -1) {
        getpw_buf_size = ENT_BUF_SIZE;
    }
    os_initNative(env, clazz);
}

/* Block until a child process exits and return its exit code.
 * Note, can only be called once for any given pid if reapStatus = true.
 *
 * Class:     java_lang_ProcessHandleImpl
 * Method:    waitForProcessExit0
 * Signature: (JZ)I
 */
JNIEXPORT jint JNICALL
Java_java_lang_ProcessHandleImpl_waitForProcessExit0(JNIEnv* env,
                                                     jclass junk,
                                                     jlong jpid,
                                                     jboolean reapStatus) {
    pid_t pid = (pid_t)jpid;
    errno = 0;

    if (reapStatus != JNI_FALSE) {
        /* Wait for the child process to exit.
         * waitpid() is standard, so use it on all POSIX platforms.
         * It is known to work when blocking to wait for the pid
         * This returns immediately if the child has already exited.
         */
        int status;
        while (waitpid(pid, &status, 0) < 0) {
            switch (errno) {
                case ECHILD:
                    return java_lang_ProcessHandleImpl_NOT_A_CHILD; // No child
                case EINTR: break;
                default: return -1;
            }
        }

        if (WIFEXITED(status)) {
            return WEXITSTATUS(status);
        } else if (WIFSIGNALED(status)) {
            return WTERMSIG_RETURN(status);
        } else {
            return status;
        }
     } else {
        /*
         * Wait for the child process to exit without reaping the exitValue.
         * waitid() is standard on all POSIX platforms.
         * Note: waitid on Mac OS X 10.7 seems to be broken;
         * it does not return the exit status consistently.
         */
        siginfo_t siginfo;
        int options = WEXITED |  WNOWAIT;
        memset(&siginfo, 0, sizeof siginfo);
        while (waitid(P_PID, pid, &siginfo, options) < 0) {
            switch (errno) {
                case ECHILD:
                    return java_lang_ProcessHandleImpl_NOT_A_CHILD; // No child
                case EINTR: break;
                default: return -1;
            }
        }

        if (siginfo.si_code == CLD_EXITED) {
             /*
              * The child exited normally; get its exit code.
              */
             return siginfo.si_status;
        } else if (siginfo.si_code == CLD_KILLED || siginfo.si_code == CLD_DUMPED) {
             return WTERMSIG_RETURN(siginfo.si_status);
        } else {
             /*
              * Unknown exit code; pass it through.
              */
             return siginfo.si_status;
        }
    }
}

/*
 * Class:     java_lang_ProcessHandleImpl
 * Method:    getCurrentPid0
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_java_lang_ProcessHandleImpl_getCurrentPid0(JNIEnv *env, jclass clazz) {
    pid_t pid = getpid();
    return (jlong) pid;
}

/*
 * Class:     java_lang_ProcessHandleImpl
 * Method:    destroy0
 * Signature: (JJZ)Z
 */
JNIEXPORT jboolean JNICALL
Java_java_lang_ProcessHandleImpl_destroy0(JNIEnv *env,
                                          jobject obj,
                                          jlong jpid,
                                          jlong startTime,
                                          jboolean force) {
    pid_t pid = (pid_t) jpid;
    int sig = (force == JNI_TRUE) ? SIGKILL : SIGTERM;
    jlong start = Java_java_lang_ProcessHandleImpl_isAlive0(env, obj, jpid);

    if (start == startTime || start == 0 || startTime == 0) {
        return (kill(pid, sig) < 0) ? JNI_FALSE : JNI_TRUE;
    } else {
        return JNI_FALSE;
    }
}

/*
 * Returns the children of the requested pid and optionally each parent and
 * start time.
 * Accumulates any process who parent pid matches.
 * The resulting pids are stored into the array of longs.
 * The number of pids is returned if they all fit.
 * If the array is too short, the negative of the desired length is returned.
 * Class:     java_lang_ProcessHandleImpl
 * Method:    getProcessPids0
 * Signature: (J[J[J[J)I
 */
JNIEXPORT jint JNICALL
Java_java_lang_ProcessHandleImpl_getProcessPids0(JNIEnv *env,
                                                 jclass clazz,
                                                 jlong jpid,
                                                 jlongArray jarray,
                                                 jlongArray jparentArray,
                                                 jlongArray jstimesArray) {
    return os_getChildren(env, jpid, jarray, jparentArray, jstimesArray);
}

/*
 * Fill in the Info object from the OS information about the process.
 *
 * Class:     java_lang_ProcessHandleImpl_Info
 * Method:    info0
 * Signature: (Ljava/lang/ProcessHandle/Info;J)I
 */
JNIEXPORT void JNICALL
Java_java_lang_ProcessHandleImpl_00024Info_info0(JNIEnv *env,
                                                 jobject jinfo,
                                                 jlong jpid) {
    pid_t pid = (pid_t) jpid;
    pid_t ppid;
    jlong totalTime = -1L;
    jlong startTime = -1L;

    ppid = os_getParentPidAndTimings(env, pid,  &totalTime, &startTime);
    if (ppid >= 0) {
        (*env)->SetLongField(env, jinfo, ProcessHandleImpl_Info_totalTimeID, totalTime);
        JNU_CHECK_EXCEPTION(env);

        (*env)->SetLongField(env, jinfo, ProcessHandleImpl_Info_startTimeID, startTime);
        JNU_CHECK_EXCEPTION(env);
    }
    os_getCmdlineAndUserInfo(env, jinfo, pid);
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
Java_java_lang_ProcessHandleImpl_isAlive0(JNIEnv *env, jobject obj, jlong jpid) {
    pid_t pid = (pid_t) jpid;
    jlong startTime = 0L;
    jlong totalTime = 0L;
    pid_t ppid = os_getParentPidAndTimings(env, pid, &totalTime, &startTime);
    return (ppid < 0) ? -1 : startTime;
}

/*
 * Returns the parent pid of the requested pid.
 * The start time of the process must match (or be ANY).
 *
 * Class:     java_lang_ProcessHandleImpl
 * Method:    parent0
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL
Java_java_lang_ProcessHandleImpl_parent0(JNIEnv *env,
                                        jobject obj,
                                        jlong jpid,
                                        jlong startTime) {
    pid_t pid = (pid_t) jpid;
    pid_t ppid;

    if (pid == getpid()) {
        ppid = getppid();
    } else {
        jlong start = 0L;
        jlong total = 0L;        // unused
        ppid = os_getParentPidAndTimings(env, pid, &total, &start);
        if (start != startTime && start != 0 && startTime != 0) {
            ppid = -1;
        }
    }
    return (jlong) ppid;
}

/**
 * Construct the argument array by parsing the arguments from the sequence
 * of arguments.
 */
void unix_fillArgArray(JNIEnv *env, jobject jinfo, int nargs, char *cp,
                       char *argsEnd, jstring cmdexe, char *cmdline) {
    jobject argsArray;
    int i;

    (*env)->SetObjectField(env, jinfo, ProcessHandleImpl_Info_commandID, cmdexe);
    JNU_CHECK_EXCEPTION(env);

    if (nargs >= 1) {
        // Create a String array for nargs-1 elements
        jclass clazzString = JNU_ClassString(env);
        CHECK_NULL(clazzString);
        argsArray = (*env)->NewObjectArray(env, nargs - 1, clazzString, NULL);
        CHECK_NULL(argsArray);

        for (i = 0; i < nargs - 1; i++) {
            jstring str = NULL;

            cp += strlen(cp) + 1;
            if (cp > argsEnd || *cp == '\0') {
                return;  // Off the end pointer or an empty argument is an error
            }

            CHECK_NULL((str = JNU_NewStringPlatform(env, cp)));

            (*env)->SetObjectArrayElement(env, argsArray, i, str);
            JNU_CHECK_EXCEPTION(env);
        }
        (*env)->SetObjectField(env, jinfo, ProcessHandleImpl_Info_argumentsID, argsArray);
        JNU_CHECK_EXCEPTION(env);
    }
    if (cmdline != NULL) {
        jstring commandLine = NULL;
        CHECK_NULL((commandLine = JNU_NewStringPlatform(env, cmdline)));
        (*env)->SetObjectField(env, jinfo, ProcessHandleImpl_Info_commandLineID, commandLine);
        JNU_CHECK_EXCEPTION(env);
    }
}

void unix_getUserInfo(JNIEnv* env, jobject jinfo, uid_t uid) {
    int result = 0;
    char* pwbuf;
    jstring name = NULL;

    /* allocate buffer for password record */
    pwbuf = (char*)malloc(getpw_buf_size);
    if (pwbuf == NULL) {
        JNU_ThrowOutOfMemoryError(env, "Unable to open getpwent");
    } else {
        struct passwd pwent;
        struct passwd* p = NULL;
        RESTARTABLE(getpwuid_r(uid, &pwent, pwbuf, (size_t)getpw_buf_size, &p), result);

        // Create the Java String if a name was found
        if (result == 0 && p != NULL &&
            p->pw_name != NULL && *(p->pw_name) != '\0') {
            name = JNU_NewStringPlatform(env, p->pw_name);
        }
        free(pwbuf);
    }
    if (name != NULL) {
        (*env)->SetObjectField(env, jinfo, ProcessHandleImpl_Info_userID, name);
    }
}

/*
 * The following functions are common on Solaris, Linux and AIX.
 */

#if defined (__linux__) || defined(_AIX)

/*
 * Returns the children of the requested pid and optionally each parent and
 * start time.
 * Reads /proc and accumulates any process who parent pid matches.
 * The resulting pids are stored into the array of longs.
 * The number of pids is returned if they all fit.
 * If the array is too short, the negative of the desired length is returned.
 */
jint unix_getChildren(JNIEnv *env, jlong jpid, jlongArray jarray,
                      jlongArray jparentArray, jlongArray jstimesArray) {
    DIR* dir;
    struct dirent* ptr;
    pid_t pid = (pid_t) jpid;
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

    /*
     * To locate the children we scan /proc looking for files that have a
     * position integer as a filename.
     */
    if ((dir = opendir("/proc")) == NULL) {
        JNU_ThrowByNameWithLastError(env,
            "java/lang/RuntimeException", "Unable to open /proc");
        return -1;
    }

    do { // Block to break out of on Exception
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

        while ((ptr = readdir(dir)) != NULL) {
            pid_t ppid = 0;
            jlong totalTime = 0L;
            jlong startTime = 0L;

            /* skip files that aren't numbers */
            pid_t childpid = (pid_t) atoi(ptr->d_name);
            if ((int) childpid <= 0) {
                continue;
            }

            // Get the parent pid, and start time
            ppid = os_getParentPidAndTimings(env, childpid, &totalTime, &startTime);
            if (ppid >= 0 && (pid == 0 || ppid == pid)) {
                if (count < arraySize) {
                    // Only store if it fits
                    pids[count] = (jlong) childpid;

                    if (ppids != NULL) {
                        // Store the parentPid
                        ppids[count] = (jlong) ppid;
                    }
                    if (stimes != NULL) {
                        // Store the process start time
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

    closedir(dir);
    // If more pids than array had size for; count will be greater than array size
    return count;
}

#endif // defined (__linux__) || defined(_AIX)

/*
 * The following functions are for AIX.
 */

#if defined(_AIX)

/**
 * Helper function to get the 'psinfo_t' data from "/proc/%d/psinfo".
 * Returns 0 on success and -1 on error.
 */
static int getPsinfo(pid_t pid, psinfo_t *psinfo) {
    FILE* fp;
    char fn[32];
    int ret;

    /*
     * Try to open /proc/%d/psinfo
     */
    snprintf(fn, sizeof fn, "/proc/%d/psinfo", pid);
    fp = fopen(fn, "r");
    if (fp == NULL) {
        return -1;
    }

    ret = fread(psinfo, 1, sizeof(psinfo_t), fp);
    fclose(fp);
    if (ret < sizeof(psinfo_t)) {
        return -1;
    }
    return 0;
}

/**
 * Read /proc/<pid>/psinfo and return the ppid, total cputime and start time.
 * Return: -1 is fail;  >=  0 is parent pid
 * 'total' will contain the running time of 'pid' in nanoseconds.
 * 'start' will contain the start time of 'pid' in milliseconds since epoch.
 */
pid_t unix_getParentPidAndTimings(JNIEnv *env, pid_t pid,
                                  jlong *totalTime, jlong* startTime) {
    psinfo_t psinfo;

    if (getPsinfo(pid, &psinfo) < 0) {
        return -1;
    }

    // Validate the pid before returning the info
    if (kill(pid, 0) < 0) {
        return -1;
    }

    *totalTime = psinfo.pr_time.tv_sec * 1000000000L + psinfo.pr_time.tv_nsec;

    *startTime = psinfo.pr_start.tv_sec * (jlong)1000 +
                 psinfo.pr_start.tv_nsec / 1000000;

    return (pid_t) psinfo.pr_ppid;
}

void unix_getCmdlineAndUserInfo(JNIEnv *env, jobject jinfo, pid_t pid) {
    psinfo_t psinfo;
    char fn[32];
    char exePath[PATH_MAX];
    char prargs[PRARGSZ + 1];
    jstring cmdexe = NULL;
    int ret;

    /*
     * Now try to open /proc/%d/psinfo
     */
    if (getPsinfo(pid, &psinfo) < 0) {
        unix_fillArgArray(env, jinfo, 0, NULL, NULL, cmdexe, NULL);
        return;
    }

    unix_getUserInfo(env, jinfo, psinfo.pr_uid);

    /*
     * Now read psinfo.pr_psargs which contains the first PRARGSZ characters of the
     * argument list (i.e. arg[0] arg[1] ...). Unfortunately, PRARGSZ is usually set
     * to 80 characters only. Nevertheless it's better than nothing :)
     */
    strncpy(prargs, psinfo.pr_psargs, PRARGSZ);
    prargs[PRARGSZ] = '\0';
    if (prargs[0] == '\0') {
        /* If psinfo.pr_psargs didn't contain any strings, use psinfo.pr_fname
         * (which only contains the last component of exec()ed pathname) as a
         * last resort. This is true for AIX kernel processes for example.
         */
        strncpy(prargs, psinfo.pr_fname, PRARGSZ);
        prargs[PRARGSZ] = '\0';
    }
    unix_fillArgArray(env, jinfo, 0, NULL, NULL, cmdexe,
                      prargs[0] == '\0' ? NULL : prargs);
}

#endif // defined(_AIX)
