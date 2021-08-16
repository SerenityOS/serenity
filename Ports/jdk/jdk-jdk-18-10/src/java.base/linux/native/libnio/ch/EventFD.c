/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

 #include <sys/eventfd.h>

#include "jni.h"
#include "jni_util.h"
#include "jvm.h"
#include "jlong.h"
#include "nio.h"
#include "nio_util.h"

#include "sun_nio_ch_EventFD.h"

JNIEXPORT jint JNICALL
Java_sun_nio_ch_EventFD_eventfd0(JNIEnv *env, jclass klazz)
{
    int efd = eventfd((uint64_t)0, 0);
    if (efd == -1) {
        JNU_ThrowIOExceptionWithLastError(env, "eventfd failed");
        return IOS_THROWN;
    }
    return efd;
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_EventFD_set0(JNIEnv *env, jclass klazz, jint efd)
{
    uint64_t one = 1ULL;
    return convertReturnVal(env, write(efd, (void*)&one, sizeof(uint64_t)),
        JNI_FALSE);
}
