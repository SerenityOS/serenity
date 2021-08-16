/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "jlong.h"

#include <unistd.h>
#include <errno.h>

#if defined(__linux__)
#include <sys/sendfile.h>
#elif defined(_ALLBSD_SOURCE)
#include <copyfile.h>
#endif
#include "sun_nio_fs_UnixCopyFile.h"

#define RESTARTABLE(_cmd, _result) do { \
  do { \
    _result = _cmd; \
  } while((_result == -1) && (errno == EINTR)); \
} while(0)

static void throwUnixException(JNIEnv* env, int errnum) {
    jobject x = JNU_NewObjectByName(env, "sun/nio/fs/UnixException",
        "(I)V", errnum);
    if (x != NULL) {
        (*env)->Throw(env, x);
    }
}

#if defined(_ALLBSD_SOURCE)
int fcopyfile_callback(int what, int stage, copyfile_state_t state,
    const char* src, const char* dst, void* cancel)
{
    if (what == COPYFILE_COPY_DATA) {
        if (stage == COPYFILE_ERR
                || (stage == COPYFILE_PROGRESS && *((int*)cancel) != 0)) {
            // errno will be set to ECANCELED if the operation is cancelled,
            // or to the appropriate error number if there is an error,
            // but in either case we need to quit.
            return COPYFILE_QUIT;
        }
    }
    return COPYFILE_CONTINUE;
}
#endif

// Transfer via user-space buffers
void transfer(JNIEnv* env, jint dst, jint src, volatile jint* cancel)
{
    char buf[8192];

    for (;;) {
        ssize_t n, pos, len;
        RESTARTABLE(read((int)src, &buf, sizeof(buf)), n);
        if (n <= 0) {
            if (n < 0)
                throwUnixException(env, errno);
            return;
        }
        if (cancel != NULL && *cancel != 0) {
            throwUnixException(env, ECANCELED);
            return;
        }
        pos = 0;
        len = n;
        do {
            char* bufp = buf;
            bufp += pos;
            RESTARTABLE(write((int)dst, bufp, len), n);
            if (n == -1) {
                throwUnixException(env, errno);
                return;
            }
            pos += n;
            len -= n;
        } while (len > 0);
    }
}

/**
 * Transfer all bytes from src to dst within the kernel if possible (Linux),
 * otherwise via user-space buffers
 */
JNIEXPORT void JNICALL
Java_sun_nio_fs_UnixCopyFile_transfer
    (JNIEnv* env, jclass this, jint dst, jint src, jlong cancelAddress)
{
    volatile jint* cancel = (jint*)jlong_to_ptr(cancelAddress);

#if defined(__linux__)
    // Transfer within the kernel
    const size_t count = cancel != NULL ?
        1048576 :   // 1 MB to give cancellation a chance
        0x7ffff000; // maximum number of bytes that sendfile() can transfer
    ssize_t bytes_sent;
    do {
        RESTARTABLE(sendfile64(dst, src, NULL, count), bytes_sent);
        if (bytes_sent == -1) {
            if (errno == EINVAL || errno == ENOSYS) {
                // Fall back to copying via user-space buffers
                transfer(env, dst, src, cancel);
            } else {
                throwUnixException(env, errno);
            }
            return;
        }
        if (cancel != NULL && *cancel != 0) {
            throwUnixException(env, ECANCELED);
            return;
        }
    } while (bytes_sent > 0);
#elif defined(_ALLBSD_SOURCE)
    copyfile_state_t state;
    if (cancel != NULL) {
        state = copyfile_state_alloc();
        copyfile_state_set(state, COPYFILE_STATE_STATUS_CB, fcopyfile_callback);
        copyfile_state_set(state, COPYFILE_STATE_STATUS_CTX, (void*)cancel);
    } else {
        state = NULL;
    }
    if (fcopyfile(src, dst, state, COPYFILE_DATA) < 0) {
        int errno_fcopyfile = errno;
        if (state != NULL)
            copyfile_state_free(state);
        throwUnixException(env, errno_fcopyfile);
        return;
    }
    if (state != NULL)
        copyfile_state_free(state);
#else
    transfer(env, dst, src, cancel);
#endif
}
