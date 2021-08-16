/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include <sys/types.h>

/*
 * Declaration of ProcessHandleImpl functions common on all Unix platforms.
 * 'unix_' functions have a single implementation in ProcessHandleImpl_unix.c
 * 'os_' prefixed functions have different, os-specific implementations in the
 * various ProcessHandleImpl_{linux,macosx,aix}.c files.
 * See ProcessHandleImpl_unix.c for more details.
 */

/* Field id for jString 'command' in java.lang.ProcessHandleImpl.Info */
extern jfieldID ProcessHandleImpl_Info_commandID;

/* Field id for jString 'commandLine' in java.lang.ProcessHandleImpl.Info */
extern jfieldID ProcessHandleImpl_Info_commandLineID;

/* Field id for jString[] 'arguments' in java.lang.ProcessHandleImpl.Info */
extern jfieldID ProcessHandleImpl_Info_argumentsID;

/* Field id for jlong 'totalTime' in java.lang.ProcessHandleImpl.Info */
extern jfieldID ProcessHandleImpl_Info_totalTimeID;

/* Field id for jlong 'startTime' in java.lang.ProcessHandleImpl.Info */
extern jfieldID ProcessHandleImpl_Info_startTimeID;

/* Field id for jString 'user' in java.lang.ProcessHandleImpl.Info */
extern jfieldID ProcessHandleImpl_Info_userID;

/**
 * Return: -1 is fail;  >=  0 is parent pid
 * 'total' will contain the running time of 'pid' in nanoseconds.
 * 'start' will contain the start time of 'pid' in milliseconds since epoch.
 */
extern pid_t unix_getParentPidAndTimings(JNIEnv *env, pid_t pid,
                                         jlong *total, jlong *start);
extern pid_t os_getParentPidAndTimings(JNIEnv *env, pid_t pid,
                                       jlong *total, jlong *start);

extern void unix_getCmdlineAndUserInfo(JNIEnv *env, jobject jinfo, pid_t pid);
extern void os_getCmdlineAndUserInfo(JNIEnv *env, jobject jinfo, pid_t pid);

extern jint unix_getChildren(JNIEnv *env, jlong jpid, jlongArray array,
                             jlongArray jparentArray, jlongArray jstimesArray);
extern jint os_getChildren(JNIEnv *env, jlong jpid, jlongArray array,
                           jlongArray jparentArray, jlongArray jstimesArray);

extern void unix_getUserInfo(JNIEnv* env, jobject jinfo, uid_t uid);
extern void unix_fillArgArray(JNIEnv *env, jobject jinfo, int nargs, char *cp,
                              char *argsEnd, jstring cmdexe, char *cmdline);

extern void os_initNative(JNIEnv *env, jclass clazz);
