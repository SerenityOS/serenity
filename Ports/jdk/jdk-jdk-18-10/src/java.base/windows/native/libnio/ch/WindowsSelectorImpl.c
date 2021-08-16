/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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

/*
 */

/* Maximum number of sockets per select() */
/* This number should be equal to WindowsSelectorImpl.MAX_SELECTABLE_FDS */
/* This definition MUST precede the inclusion of winsock2.h */

#define FD_SETSIZE 1024

#include <limits.h>
#include <stdlib.h>
#include <winsock2.h>

#include "jvm.h"
#include "jni.h"
#include "jni_util.h"
#include "nio.h"
#include "sun_nio_ch_WindowsSelectorImpl.h"
#include "sun_nio_ch_PollArrayWrapper.h"

#include "nio_util.h" /* Needed for POLL* constants (includes "winsock2.h") */

typedef struct {
    jint fd;
    jshort events;
} pollfd;

#define WAKEUP_SOCKET_BUF_SIZE 16


JNIEXPORT jint JNICALL
Java_sun_nio_ch_WindowsSelectorImpl_00024SubSelector_poll0(JNIEnv *env, jobject this,
                                   jlong pollAddress, jint numfds,
                                   jintArray returnReadFds, jintArray returnWriteFds,
                                   jintArray returnExceptFds, jlong timeout, jlong fdsBuffer)
{
    DWORD result = 0;
    pollfd *fds = (pollfd *) pollAddress;
    int i;
    FD_SET *readfds = (FD_SET *) jlong_to_ptr(fdsBuffer);
    FD_SET *writefds = (FD_SET *) jlong_to_ptr(fdsBuffer + sizeof(FD_SET));
    FD_SET *exceptfds = (FD_SET *) jlong_to_ptr(fdsBuffer + sizeof(FD_SET) * 2);
    struct timeval timevalue, *tv;
    static struct timeval zerotime = {0, 0};
    int read_count = 0, write_count = 0, except_count = 0;

#ifdef _WIN64
    int resultbuf[FD_SETSIZE + 1];
#endif

    if (timeout == 0) {
        tv = &zerotime;
    } else if (timeout < 0) {
        tv = NULL;
    } else {
        jlong sec = timeout / 1000;
        tv = &timevalue;
        //
        // struct timeval members are signed 32-bit integers so the
        // signed 64-bit jlong needs to be clamped
        //
        if (sec > INT_MAX) {
            tv->tv_sec  = INT_MAX;
            tv->tv_usec = 0;
        } else {
            tv->tv_sec  = (long)sec;
            tv->tv_usec = (long)((timeout % 1000) * 1000);
        }
    }

    /* Set FD_SET structures required for select */
    for (i = 0; i < numfds; i++) {
        if (fds[i].events & POLLIN) {
           readfds->fd_array[read_count] = fds[i].fd;
           read_count++;
        }
        if (fds[i].events & POLLOUT) {
           writefds->fd_array[write_count] = fds[i].fd;
           write_count++;
        }
        exceptfds->fd_array[except_count] = fds[i].fd;
        except_count++;
    }

    readfds->fd_count = read_count;
    writefds->fd_count = write_count;
    exceptfds->fd_count = except_count;

    /* Call select */
    if ((result = select(0 , readfds, writefds, exceptfds, tv))
                                                             == SOCKET_ERROR) {
        JNU_ThrowIOExceptionWithLastError(env, "Select failed");
        return IOS_THROWN;
    }

    /* Return selected sockets. */
    /* Each Java array consists of sockets count followed by sockets list */

#ifdef _WIN64
    resultbuf[0] = readfds->fd_count;
    for (i = 0; i < (int)readfds->fd_count; i++) {
        resultbuf[i + 1] = (int)readfds->fd_array[i];
    }
    (*env)->SetIntArrayRegion(env, returnReadFds, 0,
                              readfds->fd_count + 1, resultbuf);

    resultbuf[0] = writefds->fd_count;
    for (i = 0; i < (int)writefds->fd_count; i++) {
        resultbuf[i + 1] = (int)writefds->fd_array[i];
    }
    (*env)->SetIntArrayRegion(env, returnWriteFds, 0,
                              writefds->fd_count + 1, resultbuf);

    resultbuf[0] = exceptfds->fd_count;
    for (i = 0; i < (int)exceptfds->fd_count; i++) {
        resultbuf[i + 1] = (int)exceptfds->fd_array[i];
    }
    (*env)->SetIntArrayRegion(env, returnExceptFds, 0,
                              exceptfds->fd_count + 1, resultbuf);
#else
    (*env)->SetIntArrayRegion(env, returnReadFds, 0,
                              readfds->fd_count + 1, (jint *)readfds);

    (*env)->SetIntArrayRegion(env, returnWriteFds, 0,
                              writefds->fd_count + 1, (jint *)writefds);
    (*env)->SetIntArrayRegion(env, returnExceptFds, 0,
                              exceptfds->fd_count + 1, (jint *)exceptfds);
#endif
    return 0;
}

JNIEXPORT void JNICALL
Java_sun_nio_ch_WindowsSelectorImpl_setWakeupSocket0(JNIEnv *env, jclass this,
                                                jint scoutFd)
{
    /* Write one byte into the pipe */
    const char byte = 1;
    send(scoutFd, &byte, 1, 0);
}

JNIEXPORT void JNICALL
Java_sun_nio_ch_WindowsSelectorImpl_resetWakeupSocket0(JNIEnv *env, jclass this,
                                                jint scinFd)
{
    char bytes[WAKEUP_SOCKET_BUF_SIZE];
    long bytesToRead;

    /* Drain socket */
    /* Find out how many bytes available for read */
    ioctlsocket (scinFd, FIONREAD, &bytesToRead);
    if (bytesToRead == 0) {
        return;
    }
    /* Prepare corresponding buffer if needed, and then read */
    if (bytesToRead > WAKEUP_SOCKET_BUF_SIZE) {
        char* buf = (char*)malloc(bytesToRead);
        if (buf == NULL) {
            JNU_ThrowOutOfMemoryError(env, NULL);
            return;
        }
        recv(scinFd, buf, bytesToRead, 0);
        free(buf);
    } else {
        recv(scinFd, bytes, WAKEUP_SOCKET_BUF_SIZE, 0);
    }
}
