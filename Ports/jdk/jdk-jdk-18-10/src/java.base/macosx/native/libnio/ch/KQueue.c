/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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

#include <strings.h>
#include <sys/types.h>
#include <sys/event.h>
#include <sys/time.h>

#include "jni.h"
#include "jni_util.h"
#include "jlong.h"
#include "nio.h"
#include "nio_util.h"

#include "sun_nio_ch_KQueue.h"

JNIEXPORT jint JNICALL
Java_sun_nio_ch_KQueue_keventSize(JNIEnv* env, jclass clazz)
{
    return sizeof(struct kevent);
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_KQueue_identOffset(JNIEnv* env, jclass clazz)
{
    return offsetof(struct kevent, ident);
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_KQueue_filterOffset(JNIEnv* env, jclass clazz)
{
    return offsetof(struct kevent, filter);
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_KQueue_flagsOffset(JNIEnv* env, jclass clazz)
{
    return offsetof(struct kevent, flags);
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_KQueue_create(JNIEnv *env, jclass clazz) {
    int kqfd = kqueue();
    if (kqfd < 0) {
        JNU_ThrowIOExceptionWithLastError(env, "kqueue failed");
        return IOS_THROWN;
    }
    return kqfd;
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_KQueue_register(JNIEnv *env, jclass clazz, jint kqfd,
                                jint fd, jint filter, jint flags)

{
    struct kevent changes[1];
    int res;

    EV_SET(&changes[0], fd, filter, flags, 0, 0, 0);
    RESTARTABLE(kevent(kqfd, &changes[0], 1, NULL, 0, NULL), res);
    return (res == -1) ? errno : 0;
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_KQueue_poll(JNIEnv *env, jclass clazz, jint kqfd, jlong address,
                            jint nevents, jlong timeout)
{
    struct kevent *events = jlong_to_ptr(address);
    int res;
    struct timespec ts;
    struct timespec *tsp;

    if (timeout >= 0) {
        ts.tv_sec = timeout / 1000;
        ts.tv_nsec = (timeout % 1000) * 1000000;
        tsp = &ts;
    } else {
        tsp = NULL;
    }

    res = kevent(kqfd, NULL, 0, events, nevents, tsp);
    if (res < 0) {
        if (errno == EINTR) {
            return IOS_INTERRUPTED;
        } else {
            JNU_ThrowIOExceptionWithLastError(env, "kqueue failed");
            return IOS_THROWN;
        }
    }
    return res;
}
