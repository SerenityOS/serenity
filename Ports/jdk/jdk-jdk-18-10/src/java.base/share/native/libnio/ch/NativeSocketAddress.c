/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 #include "net_util.h"

 #include "sun_nio_ch_NativeSocketAddress.h"

 JNIEXPORT jint JNICALL
 Java_sun_nio_ch_NativeSocketAddress_AFINET(JNIEnv* env, jclass clazz)
 {
     return AF_INET;
 }

 JNIEXPORT jint JNICALL
 Java_sun_nio_ch_NativeSocketAddress_AFINET6(JNIEnv* env, jclass clazz)
 {
     return AF_INET6;
 }

JNIEXPORT jint JNICALL
Java_sun_nio_ch_NativeSocketAddress_sizeofSockAddr4(JNIEnv* env, jclass clazz)
{
    return sizeof(struct sockaddr_in);
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_NativeSocketAddress_sizeofSockAddr6(JNIEnv* env, jclass clazz)
{
    return sizeof(struct sockaddr_in6);
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_NativeSocketAddress_sizeofFamily(JNIEnv* env, jclass clazz)
{
    // sizeof(struct sockaddr, sa_family)
    return sizeof(((struct sockaddr *)0)->sa_family);
}

 JNIEXPORT jint JNICALL
 Java_sun_nio_ch_NativeSocketAddress_offsetFamily(JNIEnv* env, jclass clazz)
 {
     return offsetof(struct sockaddr, sa_family);
 }

 JNIEXPORT jint JNICALL
 Java_sun_nio_ch_NativeSocketAddress_offsetSin4Port(JNIEnv* env, jclass clazz)
 {
     return offsetof(struct sockaddr_in, sin_port);
 }

 JNIEXPORT jint JNICALL
 Java_sun_nio_ch_NativeSocketAddress_offsetSin4Addr(JNIEnv* env, jclass clazz)
 {
     return offsetof(struct sockaddr_in, sin_addr);
 }

 JNIEXPORT jint JNICALL
 Java_sun_nio_ch_NativeSocketAddress_offsetSin6Port(JNIEnv* env, jclass clazz)
 {
     return offsetof(struct sockaddr_in6, sin6_port);
 }

 JNIEXPORT jint JNICALL
 Java_sun_nio_ch_NativeSocketAddress_offsetSin6Addr(JNIEnv* env, jclass clazz)
 {
     return offsetof(struct sockaddr_in6, sin6_addr);
 }

 JNIEXPORT jint JNICALL
 Java_sun_nio_ch_NativeSocketAddress_offsetSin6ScopeId(JNIEnv* env, jclass clazz)
 {
     return offsetof(struct sockaddr_in6, sin6_scope_id);
 }

 JNIEXPORT jint JNICALL
 Java_sun_nio_ch_NativeSocketAddress_offsetSin6FlowInfo(JNIEnv* env, jclass clazz)
 {
     return offsetof(struct sockaddr_in6, sin6_flowinfo);
 }
