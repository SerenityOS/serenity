/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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

#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <inttypes.h>
#include "gssapi.h"

#ifndef _Included_NATIVE_Util
#define _Included_NATIVE_Util
#ifdef __cplusplus
extern "C" {
#endif
  extern jint getJavaTime(OM_uint32);
  extern OM_uint32 getGSSTime(jint);
  extern void checkStatus(JNIEnv *, jobject, OM_uint32, OM_uint32, char*);
  extern jint checkTime(OM_uint32);
  extern void throwOutOfMemoryError(JNIEnv *, const char*);
  extern void initGSSBuffer(JNIEnv *, jbyteArray, gss_buffer_t);
  extern void resetGSSBuffer(gss_buffer_t);

  extern gss_OID newGSSOID(JNIEnv *, jobject);
  extern void deleteGSSOID(gss_OID);
  extern gss_OID_set newGSSOIDSet(gss_OID);
  extern void deleteGSSOIDSet(gss_OID_set);

  extern jbyteArray getJavaBuffer(JNIEnv *, gss_buffer_t);
  extern jstring getJavaString(JNIEnv *, gss_buffer_t);
  extern jobject getJavaOID(JNIEnv *, gss_OID);
  extern jobjectArray getJavaOIDArray(JNIEnv *, gss_OID_set);

  extern jstring getMinorMessage(JNIEnv *, jobject, OM_uint32);
  extern int sameMech(gss_OID, gss_OID);

  extern int JGSS_DEBUG;

  extern jclass CLS_Object;
  extern jclass CLS_GSSNameElement;
  extern jclass CLS_GSSCredElement;
  extern jclass CLS_NativeGSSContext;
  extern jmethodID MID_MessageProp_getPrivacy;
  extern jmethodID MID_MessageProp_getQOP;
  extern jmethodID MID_MessageProp_setPrivacy;
  extern jmethodID MID_MessageProp_setQOP;
  extern jmethodID MID_MessageProp_setSupplementaryStates;
  extern jmethodID MID_ChannelBinding_getInitiatorAddr;
  extern jmethodID MID_ChannelBinding_getAcceptorAddr;
  extern jmethodID MID_ChannelBinding_getAppData;
  extern jmethodID MID_InetAddress_getAddr;
  extern jmethodID MID_GSSNameElement_ctor;
  extern jmethodID MID_GSSCredElement_ctor;
  extern jmethodID MID_NativeGSSContext_ctor;
  extern jfieldID FID_GSSLibStub_pMech;
  extern jfieldID FID_NativeGSSContext_pContext;
  extern jfieldID FID_NativeGSSContext_srcName;
  extern jfieldID FID_NativeGSSContext_targetName;
  extern jfieldID FID_NativeGSSContext_isInitiator;
  extern jfieldID FID_NativeGSSContext_isEstablished;
  extern jfieldID FID_NativeGSSContext_delegatedCred;
  extern jfieldID FID_NativeGSSContext_flags;
  extern jfieldID FID_NativeGSSContext_lifetime;
  extern jfieldID FID_NativeGSSContext_actualMech;
  #define TRACE0(s) { if (JGSS_DEBUG) { printf("[GSSLibStub:%d] %s\n", __LINE__, s); fflush(stdout); }}
  #define TRACE1(s, p1) { if (JGSS_DEBUG) { printf("[GSSLibStub:%d] "s"\n", __LINE__, p1); fflush(stdout); }}
  #define TRACE2(s, p1, p2) { if (JGSS_DEBUG) { printf("[GSSLibStub:%d] "s"\n", __LINE__, p1, p2); fflush(stdout); }}
  #define TRACE3(s, p1, p2, p3) { if (JGSS_DEBUG) { printf("[GSSLibStub:%d] "s"\n", __LINE__, p1, p2, p3); fflush(stdout); }}


#ifdef __cplusplus
}
#endif
#endif
