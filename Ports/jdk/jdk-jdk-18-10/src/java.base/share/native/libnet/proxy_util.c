/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

#include "proxy_util.h"

jclass proxy_class;
jclass isaddr_class;
jclass ptype_class;
jmethodID isaddr_createUnresolvedID;
jmethodID proxy_ctrID;
jfieldID pr_no_proxyID;
jfieldID ptype_httpID;
jfieldID ptype_socksID;

int initJavaClass(JNIEnv *env) {
    jclass proxy_cls = NULL;
    jclass ptype_cls = NULL;
    jclass isaddr_cls = NULL;

    // Proxy initialization
    proxy_cls = (*env)->FindClass(env,"java/net/Proxy");
    CHECK_NULL_RETURN(proxy_cls, 0);
    proxy_class = (*env)->NewGlobalRef(env, proxy_cls);
    CHECK_NULL_RETURN(proxy_class, 0);
    proxy_ctrID = (*env)->GetMethodID(env, proxy_class, "<init>",
            "(Ljava/net/Proxy$Type;Ljava/net/SocketAddress;)V");
    CHECK_NULL_RETURN(proxy_ctrID, 0);

    // Proxy$Type initialization
    ptype_cls = (*env)->FindClass(env,"java/net/Proxy$Type");
    CHECK_NULL_RETURN(ptype_cls, 0);
    ptype_class = (*env)->NewGlobalRef(env, ptype_cls);
    CHECK_NULL_RETURN(ptype_class, 0);
    ptype_httpID = (*env)->GetStaticFieldID(env, ptype_class, "HTTP",
                                            "Ljava/net/Proxy$Type;");
    CHECK_NULL_RETURN(ptype_httpID, 0);
    ptype_socksID = (*env)->GetStaticFieldID(env, ptype_class, "SOCKS",
                                             "Ljava/net/Proxy$Type;");
    CHECK_NULL_RETURN(ptype_socksID, 0);

    // NO_PROXY
    pr_no_proxyID = (*env)->GetStaticFieldID(env, proxy_class, "NO_PROXY",
                                             "Ljava/net/Proxy;");
    CHECK_NULL_RETURN(pr_no_proxyID, 0);

    // InetSocketAddress initialization
    isaddr_cls = (*env)->FindClass(env, "java/net/InetSocketAddress");
    CHECK_NULL_RETURN(isaddr_cls, 0);
    isaddr_class = (*env)->NewGlobalRef(env, isaddr_cls);
    CHECK_NULL_RETURN(isaddr_class, 0);
    isaddr_createUnresolvedID = (*env)->GetStaticMethodID(env, isaddr_class,
            "createUnresolved",
            "(Ljava/lang/String;I)Ljava/net/InetSocketAddress;");

    return isaddr_createUnresolvedID != NULL ? 1 : 0;
}

jobject createProxy(JNIEnv *env, jfieldID ptype_ID, const char* phost, unsigned short pport) {
    jobject jProxy = NULL;
    jobject type_proxy = NULL;
    type_proxy = (*env)->GetStaticObjectField(env, ptype_class, ptype_ID);
    if (type_proxy) {
        jstring jhost = NULL;
        jhost = (*env)->NewStringUTF(env, phost);
        if (jhost) {
            jobject isa = NULL;
            isa = (*env)->CallStaticObjectMethod(env, isaddr_class,
                    isaddr_createUnresolvedID, jhost, pport);
            if (isa) {
                jProxy = (*env)->NewObject(env, proxy_class, proxy_ctrID,
                                          type_proxy, isa);
            }
        }
    }
    return jProxy;
}
