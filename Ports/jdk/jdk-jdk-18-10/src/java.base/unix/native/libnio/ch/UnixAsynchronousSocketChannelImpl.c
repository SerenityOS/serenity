/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
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
#include <sys/socket.h>

#include "jni.h"
#include "jni_util.h"
#include "net_util.h"
#include "jlong.h"
#include "sun_nio_ch_UnixAsynchronousSocketChannelImpl.h"
#include "nio_util.h"
#include "nio.h"


JNIEXPORT void JNICALL
Java_sun_nio_ch_UnixAsynchronousSocketChannelImpl_checkConnect(JNIEnv *env,
    jobject this, int fd)
{
    int error = 0;
    socklen_t arglen = sizeof(error);
    int result;

    result = getsockopt(fd, SOL_SOCKET, SO_ERROR, &error, &arglen);
    if (result < 0) {
        JNU_ThrowIOExceptionWithLastError(env, "getsockopt");
    } else {
        if (error)
            handleSocketError(env, error);
    }
}
