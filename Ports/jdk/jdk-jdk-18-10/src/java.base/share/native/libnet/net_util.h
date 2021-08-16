/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef NET_UTILS_H
#define NET_UTILS_H

#include "jvm.h"
#include "jni_util.h"
#include "net_util_md.h"

/************************************************************************
 * Macros and misc constants
 */

#define MAX_PACKET_LEN 65536

#define NET_WAIT_READ    0x01
#define NET_WAIT_WRITE   0x02
#define NET_WAIT_CONNECT 0x04

/************************************************************************
 * Cached field IDs
 *
 * The naming convention for field IDs is
 *      <class abbrv>_<fieldName>ID
 */
extern jclass ia_class;
extern jfieldID iac_addressID;
extern jfieldID iac_familyID;
extern jfieldID iac_hostNameID;
extern jfieldID iac_origHostNameID;
extern jfieldID ia_preferIPv6AddressID;

JNIEXPORT void JNICALL initInetAddressIDs(JNIEnv *env);

/** (Inet6Address accessors)
 * set_ methods return JNI_TRUE on success JNI_FALSE on error
 * get_ methods that return int/boolean, return -1 on error
 * get_ methods that return objects return NULL on error.
 */
extern jboolean setInet6Address_scopeifname(JNIEnv *env, jobject ia6Obj, jobject scopeifname);
extern unsigned int getInet6Address_scopeid(JNIEnv *env, jobject ia6Obj);
extern jboolean setInet6Address_scopeid(JNIEnv *env, jobject ia6Obj, int scopeid);
extern jboolean getInet6Address_ipaddress(JNIEnv *env, jobject ia6Obj, char *dest);
extern jboolean setInet6Address_ipaddress(JNIEnv *env, jobject ia6Obj, char *address);

extern void setInetAddress_addr(JNIEnv *env, jobject iaObj, int address);
extern void setInetAddress_family(JNIEnv *env, jobject iaObj, int family);
extern void setInetAddress_hostName(JNIEnv *env, jobject iaObj, jobject h);
extern int getInetAddress_addr(JNIEnv *env, jobject iaObj);
extern int getInetAddress_family(JNIEnv *env, jobject iaObj);

extern jclass ia4_class;
extern jmethodID ia4_ctrID;

/* NetworkInterface fields */
extern jclass ni_class;
extern jfieldID ni_nameID;
extern jfieldID ni_indexID;
extern jfieldID ni_addrsID;
extern jfieldID ni_descID;
extern jmethodID ni_ctrID;

/* DatagramPacket fields */
extern jfieldID dp_addressID;
extern jfieldID dp_portID;
extern jfieldID dp_bufID;
extern jfieldID dp_offsetID;
extern jfieldID dp_lengthID;
extern jfieldID dp_bufLengthID;

/* Inet6Address fields */
extern jclass ia6_class;
extern jfieldID ia6_holder6ID;
extern jfieldID ia6_ipaddressID;
extern jfieldID ia6_scopeidID;
extern jfieldID ia6_scopeidsetID;
extern jfieldID ia6_scopeifnameID;
extern jmethodID ia6_ctrID;

/************************************************************************
 *  Utilities
 */
JNIEXPORT void JNICALL Java_java_net_InetAddress_init(JNIEnv *env, jclass cls);
JNIEXPORT void JNICALL Java_java_net_Inet4Address_init(JNIEnv *env, jclass cls);
JNIEXPORT void JNICALL Java_java_net_Inet6Address_init(JNIEnv *env, jclass cls);
JNIEXPORT void JNICALL Java_java_net_NetworkInterface_init(JNIEnv *env, jclass cls);

JNIEXPORT void JNICALL NET_ThrowNew(JNIEnv *env, int errorNum, char *msg);

void NET_ThrowCurrent(JNIEnv *env, char *msg);

jfieldID NET_GetFileDescriptorID(JNIEnv *env);

JNIEXPORT jint JNICALL ipv4_available();
JNIEXPORT jint JNICALL ipv6_available();

JNIEXPORT jint JNICALL reuseport_available();

/**
 * This function will fill a SOCKETADDRESS structure from an InetAddress
 * object.
 *
 * The parameter 'sa' must point to valid storage of size
 * 'sizeof(SOCKETADDRESS)'.
 *
 * The parameter 'len' is a pointer to an int and is used for returning
 * the actual sockaddr length, e.g. 'sizeof(struct sockaddr_in)' or
 * 'sizeof(struct sockaddr_in6)'.
 *
 * If the type of the InetAddress object is IPv6, the function will fill a
 * sockaddr_in6 structure. IPv6 must be available in that case, otherwise an
 * exception is thrown.
 * In the case of an IPv4 InetAddress, when IPv6 is available and
 * v4MappedAddress is TRUE, this method will fill a sockaddr_in6 structure
 * containing an IPv4 mapped IPv6 address. Otherwise a sockaddr_in
 * structure will be filled.
 */
JNIEXPORT int JNICALL
NET_InetAddressToSockaddr(JNIEnv *env, jobject iaObj, int port,
                          SOCKETADDRESS *sa, int *len,
                          jboolean v4MappedAddress);

JNIEXPORT jobject JNICALL
NET_SockaddrToInetAddress(JNIEnv *env, SOCKETADDRESS *sa, int *port);

void platformInit();

JNIEXPORT jint JNICALL NET_GetPortFromSockaddr(SOCKETADDRESS *sa);

JNIEXPORT jboolean JNICALL
NET_SockaddrEqualsInetAddress(JNIEnv *env, SOCKETADDRESS *sa, jobject iaObj);

int NET_IsIPv4Mapped(jbyte* caddr);

int NET_IPv4MappedToIPv4(jbyte* caddr);

int NET_IsEqual(jbyte* caddr1, jbyte* caddr2);

int NET_IsZeroAddr(jbyte* caddr);

/* Socket operations
 *
 * These work just like the system calls, except that they may do some
 * platform-specific pre/post processing of the arguments and/or results.
 */

JNIEXPORT int JNICALL
NET_SocketAvailable(int fd, int *pbytes);

JNIEXPORT int JNICALL
NET_GetSockOpt(int fd, int level, int opt, void *result, int *len);

JNIEXPORT int JNICALL
NET_SetSockOpt(int fd, int level, int opt, const void *arg, int len);

JNIEXPORT int JNICALL
NET_Bind(int fd, SOCKETADDRESS *sa, int len);

JNIEXPORT int JNICALL
NET_MapSocketOption(jint cmd, int *level, int *optname);

JNIEXPORT int JNICALL
NET_MapSocketOptionV6(jint cmd, int *level, int *optname);

JNIEXPORT jint JNICALL
NET_EnableFastTcpLoopback(int fd);

unsigned short in_cksum(unsigned short *addr, int len);

jint NET_Wait(JNIEnv *env, jint fd, jint flags, jint timeout);

#endif /* NET_UTILS_H */
