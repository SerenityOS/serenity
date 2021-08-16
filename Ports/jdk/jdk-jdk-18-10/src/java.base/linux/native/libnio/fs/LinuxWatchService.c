/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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
#include "jvm.h"
#include "jlong.h"

#include <stdlib.h>
#include <dlfcn.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <poll.h>
#include <sys/inotify.h>

#include "sun_nio_fs_LinuxWatchService.h"

static void throwUnixException(JNIEnv* env, int errnum) {
    jobject x = JNU_NewObjectByName(env, "sun/nio/fs/UnixException",
        "(I)V", errnum);
    if (x != NULL) {
        (*env)->Throw(env, x);
    }
}

JNIEXPORT jint JNICALL
Java_sun_nio_fs_LinuxWatchService_eventSize(JNIEnv *env, jclass clazz)
{
    return (jint)sizeof(struct inotify_event);
}

JNIEXPORT jintArray JNICALL
Java_sun_nio_fs_LinuxWatchService_eventOffsets(JNIEnv *env, jclass clazz)
{
    jintArray result = (*env)->NewIntArray(env, 5);
    if (result != NULL) {
        jint arr[5];
        arr[0] = (jint)offsetof(struct inotify_event, wd);
        arr[1] = (jint)offsetof(struct inotify_event, mask);
        arr[2] = (jint)offsetof(struct inotify_event, cookie);
        arr[3] = (jint)offsetof(struct inotify_event, len);
        arr[4] = (jint)offsetof(struct inotify_event, name);
        (*env)->SetIntArrayRegion(env, result, 0, 5, arr);
    }
    return result;
}


JNIEXPORT jint JNICALL
Java_sun_nio_fs_LinuxWatchService_inotifyInit
    (JNIEnv* env, jclass clazz)
{
    int ifd = inotify_init();
    if (ifd == -1) {
        throwUnixException(env, errno);
    }
    return (jint)ifd;
}

JNIEXPORT jint JNICALL
Java_sun_nio_fs_LinuxWatchService_inotifyAddWatch
    (JNIEnv* env, jclass clazz, jint fd, jlong address, jint mask)
{
    int wfd = -1;
    const char* path = (const char*)jlong_to_ptr(address);

    wfd = inotify_add_watch((int)fd, path, mask);
    if (wfd == -1) {
        throwUnixException(env, errno);
    }
    return (jint)wfd;
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_LinuxWatchService_inotifyRmWatch
    (JNIEnv* env, jclass clazz, jint fd, jint wd)
{
    int err = inotify_rm_watch((int)fd, (int)wd);
    if (err == -1)
        throwUnixException(env, errno);
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_LinuxWatchService_configureBlocking
    (JNIEnv* env, jclass clazz, jint fd, jboolean blocking)
{
    int flags = fcntl(fd, F_GETFL);

    if ((blocking == JNI_FALSE) && !(flags & O_NONBLOCK))
        fcntl(fd, F_SETFL, flags | O_NONBLOCK);
    else if ((blocking == JNI_TRUE) && (flags & O_NONBLOCK))
        fcntl(fd, F_SETFL, flags & ~O_NONBLOCK);
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_LinuxWatchService_socketpair
    (JNIEnv* env, jclass clazz, jintArray sv)
{
    int sp[2];
    if (socketpair(PF_UNIX, SOCK_STREAM, 0, sp) == -1) {
        throwUnixException(env, errno);
    } else {
        jint res[2];
        res[0] = (jint)sp[0];
        res[1] = (jint)sp[1];
        (*env)->SetIntArrayRegion(env, sv, 0, 2, &res[0]);
    }
}

JNIEXPORT jint JNICALL
Java_sun_nio_fs_LinuxWatchService_poll
    (JNIEnv* env, jclass clazz, jint fd1, jint fd2)
{
    struct pollfd ufds[2];
    int n;

    ufds[0].fd = fd1;
    ufds[0].events = POLLIN;
    ufds[1].fd = fd2;
    ufds[1].events = POLLIN;

    n = poll(&ufds[0], 2, -1);
    if (n == -1) {
        if (errno == EINTR) {
            n = 0;
        } else {
            throwUnixException(env, errno);
        }
     }
    return (jint)n;
}
