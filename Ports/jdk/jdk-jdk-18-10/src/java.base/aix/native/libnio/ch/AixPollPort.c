/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2012 SAP SE. All rights reserved.
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

#include "sun_nio_ch_AixPollPort.h"

#include <unistd.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <poll.h>
#include <sys/pollset.h>
#include <fcntl.h>
#include <stddef.h>
#include <dlfcn.h>
#include <errno.h>

/* Initially copied from src/solaris/native/sun/nio/ch/nio_util.h */
#define RESTARTABLE(_cmd, _result) do { \
  do { \
    _result = _cmd; \
  } while((_result == -1) && (errno == EINTR)); \
} while(0)

typedef pollset_t pollset_create_func(int maxfd);
typedef int pollset_destroy_func(pollset_t ps);
typedef int pollset_ctl_func(pollset_t ps, struct poll_ctl *pollctl_array, int array_length);
typedef int pollset_poll_func(pollset_t ps, struct pollfd *polldata_array, int array_length, int timeout);
static pollset_create_func* _pollset_create = NULL;
static pollset_destroy_func* _pollset_destroy = NULL;
static pollset_ctl_func* _pollset_ctl = NULL;
static pollset_poll_func* _pollset_poll = NULL;

JNIEXPORT void JNICALL
Java_sun_nio_ch_AixPollPort_init(JNIEnv* env, jclass this) {
    _pollset_create = (pollset_create_func*) dlsym(RTLD_DEFAULT, "pollset_create");
    _pollset_destroy = (pollset_destroy_func*) dlsym(RTLD_DEFAULT, "pollset_destroy");
    _pollset_ctl = (pollset_ctl_func*) dlsym(RTLD_DEFAULT, "pollset_ctl");
    _pollset_poll = (pollset_poll_func*) dlsym(RTLD_DEFAULT, "pollset_poll");
    if (_pollset_create == NULL || _pollset_destroy == NULL ||
        _pollset_ctl == NULL || _pollset_poll == NULL) {
        JNU_ThrowInternalError(env, "unable to get address of pollset functions");
    }
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_AixPollPort_eventSize(JNIEnv* env, jclass this) {
    return sizeof(struct pollfd);
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_AixPollPort_eventsOffset(JNIEnv* env, jclass this) {
    return offsetof(struct pollfd, events);
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_AixPollPort_reventsOffset(JNIEnv* env, jclass this) {
    return offsetof(struct pollfd, revents);
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_AixPollPort_fdOffset(JNIEnv* env, jclass this) {
    return offsetof(struct pollfd, fd);
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_AixPollPort_pollsetCreate(JNIEnv *env, jclass c) {
    /* pollset_create can take the maximum number of fds, but we
     * cannot predict this number so we leave it at OPEN_MAX. */
    pollset_t ps = _pollset_create(-1);
    if (ps < 0) {
       JNU_ThrowIOExceptionWithLastError(env, "pollset_create failed");
    }
    return (int)ps;
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_AixPollPort_pollsetCtl(JNIEnv *env, jclass c, jint ps,
                                       jint opcode, jint fd, jint events) {
    struct poll_ctl event;
    int res;

    event.cmd = opcode;
    event.events = events;
    event.fd = fd;

    RESTARTABLE(_pollset_ctl((pollset_t)ps, &event, 1 /* length */), res);

    return (res == 0) ? 0 : errno;
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_AixPollPort_pollsetPoll(JNIEnv *env, jclass c,
                                        jint ps, jlong address, jint numfds) {
    struct pollfd *events = jlong_to_ptr(address);
    int res;

    RESTARTABLE(_pollset_poll(ps, events, numfds, -1), res);
    if (res < 0) {
        JNU_ThrowIOExceptionWithLastError(env, "pollset_poll failed");
    }
    return res;
}

JNIEXPORT void JNICALL
Java_sun_nio_ch_AixPollPort_pollsetDestroy(JNIEnv *env, jclass c, jint ps) {
    int res;
    RESTARTABLE(_pollset_destroy((pollset_t)ps), res);
}

JNIEXPORT void JNICALL
Java_sun_nio_ch_AixPollPort_socketpair(JNIEnv* env, jclass clazz, jintArray sv) {
    int sp[2];
    if (socketpair(PF_UNIX, SOCK_STREAM, 0, sp) == -1) {
        JNU_ThrowIOExceptionWithLastError(env, "socketpair failed");
    } else {
        jint res[2];
        res[0] = (jint)sp[0];
        res[1] = (jint)sp[1];
        (*env)->SetIntArrayRegion(env, sv, 0, 2, &res[0]);
    }
}

JNIEXPORT void JNICALL
Java_sun_nio_ch_AixPollPort_interrupt(JNIEnv *env, jclass c, jint fd) {
    int res;
    int buf[1];
    buf[0] = 1;
    RESTARTABLE(write(fd, buf, 1), res);
    if (res < 0) {
        JNU_ThrowIOExceptionWithLastError(env, "write failed");
    }
}

JNIEXPORT void JNICALL
Java_sun_nio_ch_AixPollPort_drain1(JNIEnv *env, jclass cl, jint fd) {
    int res;
    char buf[1];
    RESTARTABLE(read(fd, buf, 1), res);
    if (res < 0) {
        JNU_ThrowIOExceptionWithLastError(env, "drain1 failed");
    }
}

JNIEXPORT void JNICALL
Java_sun_nio_ch_AixPollPort_close0(JNIEnv *env, jclass c, jint fd) {
    int res;
    RESTARTABLE(close(fd), res);
}
