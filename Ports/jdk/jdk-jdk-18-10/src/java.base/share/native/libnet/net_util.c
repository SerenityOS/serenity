/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include "net_util.h"

#include "java_net_InetAddress.h"

int IPv4_supported();
int IPv6_supported();
int reuseport_supported();

static int IPv4_available;
static int IPv6_available;
static int REUSEPORT_available;

JNIEXPORT jint JNICALL ipv4_available()
{
    return IPv4_available;
}

JNIEXPORT jint JNICALL ipv6_available()
{
    return IPv6_available;
}

JNIEXPORT jint JNICALL reuseport_available()
{
    return REUSEPORT_available;
}

JNIEXPORT jint JNICALL
DEF_JNI_OnLoad(JavaVM *vm, void *reserved)
{
    JNIEnv *env;
    jclass iCls;
    jmethodID mid;
    jstring s;
    jint preferIPv4Stack;
    if ((*vm)->GetEnv(vm, (void**) &env, JNI_VERSION_1_2) != JNI_OK) {
        return JNI_EVERSION; /* JNI version not supported */
    }

    iCls = (*env)->FindClass(env, "java/lang/Boolean");
    CHECK_NULL_RETURN(iCls, JNI_VERSION_1_2);
    mid = (*env)->GetStaticMethodID(env, iCls, "getBoolean", "(Ljava/lang/String;)Z");
    CHECK_NULL_RETURN(mid, JNI_VERSION_1_2);
    s = (*env)->NewStringUTF(env, "java.net.preferIPv4Stack");
    CHECK_NULL_RETURN(s, JNI_VERSION_1_2);
    preferIPv4Stack = (*env)->CallStaticBooleanMethod(env, iCls, mid, s);

    /*
     * Since we have initialized and loaded the socket library we will
     * check now whether we have IPv6 on this platform and if the
     * supporting socket APIs are available
     */
    IPv4_available = IPv4_supported();
    IPv6_available = IPv6_supported() & (!preferIPv4Stack);

    /* check if SO_REUSEPORT is supported on this platform */
    REUSEPORT_available = reuseport_supported();
    platformInit();

    return JNI_VERSION_1_2;
}

static int initialized = 0;

JNIEXPORT void JNICALL initInetAddressIDs(JNIEnv *env) {
    if (!initialized) {
        Java_java_net_InetAddress_init(env, 0);
        JNU_CHECK_EXCEPTION(env);
        Java_java_net_Inet4Address_init(env, 0);
        JNU_CHECK_EXCEPTION(env);
        Java_java_net_Inet6Address_init(env, 0);
        JNU_CHECK_EXCEPTION(env);
        initialized = 1;
    }
}

/* The address, and family fields used to be in InetAddress
 * but are now in an implementation object. So, there is an extra
 * level of indirection to access them now.
 */

extern jclass iac_class;
extern jfieldID ia_holderID;
extern jfieldID iac_addressID;
extern jfieldID iac_familyID;

/**
 * set_ methods return JNI_TRUE on success JNI_FALSE on error
 * get_ methods that return +ve int return -1 on error
 * get_ methods that return objects return NULL on error.
 */
jboolean setInet6Address_scopeifname(JNIEnv *env, jobject iaObj, jobject scopeifname) {
    jobject holder = (*env)->GetObjectField(env, iaObj, ia6_holder6ID);
    CHECK_NULL_RETURN(holder, JNI_FALSE);
    (*env)->SetObjectField(env, holder, ia6_scopeifnameID, scopeifname);
    (*env)->DeleteLocalRef(env, holder);
    return JNI_TRUE;
}

unsigned int getInet6Address_scopeid(JNIEnv *env, jobject iaObj) {
    unsigned int id;
    jobject holder = (*env)->GetObjectField(env, iaObj, ia6_holder6ID);
    CHECK_NULL_RETURN(holder, 0);
    id = (unsigned int)(*env)->GetIntField(env, holder, ia6_scopeidID);
    (*env)->DeleteLocalRef(env, holder);
    return id;
}

jboolean setInet6Address_scopeid(JNIEnv *env, jobject iaObj, int scopeid) {
    jobject holder = (*env)->GetObjectField(env, iaObj, ia6_holder6ID);
    CHECK_NULL_RETURN(holder, JNI_FALSE);
    (*env)->SetIntField(env, holder, ia6_scopeidID, scopeid);
    if (scopeid > 0) {
        (*env)->SetBooleanField(env, holder, ia6_scopeidsetID, JNI_TRUE);
    }
    (*env)->DeleteLocalRef(env, holder);
    return JNI_TRUE;
}

jboolean getInet6Address_ipaddress(JNIEnv *env, jobject iaObj, char *dest) {
    jobject holder, addr;

    holder = (*env)->GetObjectField(env, iaObj, ia6_holder6ID);
    CHECK_NULL_RETURN(holder, JNI_FALSE);
    addr = (*env)->GetObjectField(env, holder, ia6_ipaddressID);
    CHECK_NULL_RETURN(addr, JNI_FALSE);
    (*env)->GetByteArrayRegion(env, addr, 0, 16, (jbyte *)dest);
    (*env)->DeleteLocalRef(env, addr);
    (*env)->DeleteLocalRef(env, holder);
    return JNI_TRUE;
}

jboolean setInet6Address_ipaddress(JNIEnv *env, jobject iaObj, char *address) {
    jobject holder;
    jbyteArray addr;

    holder = (*env)->GetObjectField(env, iaObj, ia6_holder6ID);
    CHECK_NULL_RETURN(holder, JNI_FALSE);
    addr = (jbyteArray)(*env)->GetObjectField(env, holder, ia6_ipaddressID);
    if (addr == NULL) {
        addr = (*env)->NewByteArray(env, 16);
        CHECK_NULL_RETURN(addr, JNI_FALSE);
        (*env)->SetObjectField(env, holder, ia6_ipaddressID, addr);
    }
    (*env)->SetByteArrayRegion(env, addr, 0, 16, (jbyte *)address);
    (*env)->DeleteLocalRef(env, addr);
    (*env)->DeleteLocalRef(env, holder);
    return JNI_TRUE;
}

void setInetAddress_addr(JNIEnv *env, jobject iaObj, int address) {
    jobject holder = (*env)->GetObjectField(env, iaObj, ia_holderID);
    CHECK_NULL_THROW_NPE(env, holder, "InetAddress holder is null");
    (*env)->SetIntField(env, holder, iac_addressID, address);
    (*env)->DeleteLocalRef(env, holder);
}

void setInetAddress_family(JNIEnv *env, jobject iaObj, int family) {
    jobject holder = (*env)->GetObjectField(env, iaObj, ia_holderID);
    CHECK_NULL_THROW_NPE(env, holder, "InetAddress holder is null");
    (*env)->SetIntField(env, holder, iac_familyID, family);
    (*env)->DeleteLocalRef(env, holder);
}

void setInetAddress_hostName(JNIEnv *env, jobject iaObj, jobject host) {
    jobject holder = (*env)->GetObjectField(env, iaObj, ia_holderID);
    CHECK_NULL_THROW_NPE(env, holder, "InetAddress holder is null");
    (*env)->SetObjectField(env, holder, iac_hostNameID, host);
    (*env)->SetObjectField(env, holder, iac_origHostNameID, host);
    (*env)->DeleteLocalRef(env, holder);
}

int getInetAddress_addr(JNIEnv *env, jobject iaObj) {
    int addr;
    jobject holder = (*env)->GetObjectField(env, iaObj, ia_holderID);
    CHECK_NULL_THROW_NPE_RETURN(env, holder, "InetAddress holder is null", -1);
    addr = (*env)->GetIntField(env, holder, iac_addressID);
    (*env)->DeleteLocalRef(env, holder);
    return addr;
}

int getInetAddress_family(JNIEnv *env, jobject iaObj) {
    int family;
    jobject holder = (*env)->GetObjectField(env, iaObj, ia_holderID);
    CHECK_NULL_THROW_NPE_RETURN(env, holder, "InetAddress holder is null", -1);
    family = (*env)->GetIntField(env, holder, iac_familyID);
    (*env)->DeleteLocalRef(env, holder);
    return family;
}

JNIEXPORT jobject JNICALL
NET_SockaddrToInetAddress(JNIEnv *env, SOCKETADDRESS *sa, int *port) {
    jobject iaObj;
    if (sa->sa.sa_family == AF_INET6) {
        jbyte *caddr = (jbyte *)&sa->sa6.sin6_addr;
        if (NET_IsIPv4Mapped(caddr)) {
            int address;
            iaObj = (*env)->NewObject(env, ia4_class, ia4_ctrID);
            CHECK_NULL_RETURN(iaObj, NULL);
            address = NET_IPv4MappedToIPv4(caddr);
            setInetAddress_addr(env, iaObj, address);
            JNU_CHECK_EXCEPTION_RETURN(env, NULL);
            setInetAddress_family(env, iaObj, java_net_InetAddress_IPv4);
            JNU_CHECK_EXCEPTION_RETURN(env, NULL);
        } else {
            jboolean ret;
            iaObj = (*env)->NewObject(env, ia6_class, ia6_ctrID);
            CHECK_NULL_RETURN(iaObj, NULL);
            ret = setInet6Address_ipaddress(env, iaObj, (char *)&sa->sa6.sin6_addr);
            if (ret == JNI_FALSE)
                return NULL;
            setInetAddress_family(env, iaObj, java_net_InetAddress_IPv6);
            JNU_CHECK_EXCEPTION_RETURN(env, NULL);
            setInet6Address_scopeid(env, iaObj, sa->sa6.sin6_scope_id);
        }
        *port = ntohs(sa->sa6.sin6_port);
    } else {
        iaObj = (*env)->NewObject(env, ia4_class, ia4_ctrID);
        CHECK_NULL_RETURN(iaObj, NULL);
        setInetAddress_family(env, iaObj, java_net_InetAddress_IPv4);
        JNU_CHECK_EXCEPTION_RETURN(env, NULL);
        setInetAddress_addr(env, iaObj, ntohl(sa->sa4.sin_addr.s_addr));
        JNU_CHECK_EXCEPTION_RETURN(env, NULL);
        *port = ntohs(sa->sa4.sin_port);
    }
    return iaObj;
}

JNIEXPORT jboolean JNICALL
NET_SockaddrEqualsInetAddress(JNIEnv *env, SOCKETADDRESS *sa, jobject iaObj)
{
    jint family = getInetAddress_family(env, iaObj) ==
        java_net_InetAddress_IPv4 ? AF_INET : AF_INET6;
    JNU_CHECK_EXCEPTION_RETURN(env, JNI_FALSE);
    if (sa->sa.sa_family == AF_INET6) {
        jbyte *caddrNew = (jbyte *)&sa->sa6.sin6_addr;
        if (NET_IsIPv4Mapped(caddrNew)) {
            int addrNew, addrCur;
            if (family == AF_INET6) {
                return JNI_FALSE;
            }
            addrNew = NET_IPv4MappedToIPv4(caddrNew);
            addrCur = getInetAddress_addr(env, iaObj);
            JNU_CHECK_EXCEPTION_RETURN(env, JNI_FALSE);
            if (addrNew == addrCur) {
                return JNI_TRUE;
            } else {
                return JNI_FALSE;
            }
        } else {
            jbyte caddrCur[16];
            if (family == AF_INET) {
                return JNI_FALSE;
            }
            getInet6Address_ipaddress(env, iaObj, (char *)caddrCur);
            if (NET_IsEqual(caddrNew, caddrCur) &&
                sa->sa6.sin6_scope_id == getInet6Address_scopeid(env, iaObj))
            {
                return JNI_TRUE;
            } else {
                return JNI_FALSE;
            }
        }
    } else {
        int addrNew, addrCur;
        if (family != AF_INET) {
            return JNI_FALSE;
        }
        addrNew = ntohl(sa->sa4.sin_addr.s_addr);
        addrCur = getInetAddress_addr(env, iaObj);
        JNU_CHECK_EXCEPTION_RETURN(env, JNI_FALSE);
        if (addrNew == addrCur) {
            return JNI_TRUE;
        } else {
            return JNI_FALSE;
        }
    }
}

JNIEXPORT jint JNICALL
NET_GetPortFromSockaddr(SOCKETADDRESS *sa) {
    if (sa->sa.sa_family == AF_INET6) {
        return ntohs(sa->sa6.sin6_port);
    } else {
        return ntohs(sa->sa4.sin_port);
    }
}

unsigned short
in_cksum(unsigned short *addr, int len) {
    int nleft = len;
    int sum = 0;
    unsigned short *w = addr;
    unsigned short answer = 0;
    while(nleft > 1) {
        sum += *w++;
        nleft -= 2;
    }

    if (nleft == 1) {
        *(unsigned char *) (&answer) = *(unsigned char *)w;
        sum += answer;
    }

    sum = (sum >> 16) + (sum & 0xffff);
    sum += (sum >> 16);
    answer = ~sum;
    return (answer);
}
