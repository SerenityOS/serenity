/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include <arpa/inet.h>
#include <errno.h>
#include <net/if.h>
#include <net/if_arp.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>

#if defined(_AIX)
#include <netinet/in6_var.h>
#include <sys/ndd_var.h>
#include <sys/kinfo.h>
#include <strings.h>
#endif

#if defined(_ALLBSD_SOURCE)
#include <net/ethernet.h>
#include <net/if_dl.h>
#include <ifaddrs.h>
#endif

#include "net_util.h"

#include "java_net_InetAddress.h"

#if defined(__linux__)
    #define _PATH_PROCNET_IFINET6 "/proc/net/if_inet6"
#endif

#ifdef LIFNAMSIZ
    #define IFNAMESIZE LIFNAMSIZ
#else
    #define IFNAMESIZE IFNAMSIZ
#endif

#define CHECKED_MALLOC3(_pointer, _type, _size) \
    do { \
        _pointer = (_type)malloc(_size); \
        if (_pointer == NULL) { \
            JNU_ThrowOutOfMemoryError(env, "Native heap allocation failed"); \
            return ifs; /* return untouched list */ \
        } \
    } while(0)

typedef struct _netaddr  {
    struct sockaddr *addr;
    struct sockaddr *brdcast;
    short mask;
    int family; /* to make searches simple */
    struct _netaddr *next;
} netaddr;

typedef struct _netif {
    char *name;
    int index;
    char virtual;
    netaddr *addr;
    struct _netif *childs;
    struct _netif *next;
} netif;

/************************************************************************
 * NetworkInterface
 */

#include "java_net_NetworkInterface.h"

/************************************************************************
 * NetworkInterface
 */
jclass ni_class;
jfieldID ni_nameID;
jfieldID ni_indexID;
jfieldID ni_descID;
jfieldID ni_addrsID;
jfieldID ni_bindsID;
jfieldID ni_virutalID;
jfieldID ni_childsID;
jfieldID ni_parentID;
jfieldID ni_defaultIndexID;
jmethodID ni_ctrID;

static jclass ni_ibcls;
static jmethodID ni_ibctrID;
static jfieldID ni_ibaddressID;
static jfieldID ni_ib4broadcastID;
static jfieldID ni_ib4maskID;

/** Private methods declarations **/
static jobject createNetworkInterface(JNIEnv *env, netif *ifs);
static int     getFlags0(JNIEnv *env, jstring  ifname);

static netif  *enumInterfaces(JNIEnv *env);
static netif  *enumIPv4Interfaces(JNIEnv *env, int sock, netif *ifs);
static netif  *enumIPv6Interfaces(JNIEnv *env, int sock, netif *ifs);

static netif  *addif(JNIEnv *env, int sock, const char *if_name, netif *ifs,
                     struct sockaddr *ifr_addrP,
                     struct sockaddr *ifr_broadaddrP,
                     int family, short prefix);
static void    freeif(netif *ifs);

static int     openSocket(JNIEnv *env, int proto);
static int     openSocketWithFallback(JNIEnv *env, const char *ifname);

static short   translateIPv4AddressToPrefix(struct sockaddr_in *addr);
static short   translateIPv6AddressToPrefix(struct sockaddr_in6 *addr);

static int     getIndex(int sock, const char *ifname);
static int     getFlags(int sock, const char *ifname, int *flags);
static int     getMacAddress(JNIEnv *env, const char *ifname,
                             const struct in_addr *addr, unsigned char *buf);
static int     getMTU(JNIEnv *env, int sock, const char *ifname);

/******************* Java entry points *****************************/

/*
 * Class:     java_net_NetworkInterface
 * Method:    init
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_java_net_NetworkInterface_init
  (JNIEnv *env, jclass cls)
{
    ni_class = (*env)->FindClass(env, "java/net/NetworkInterface");
    CHECK_NULL(ni_class);
    ni_class = (*env)->NewGlobalRef(env, ni_class);
    CHECK_NULL(ni_class);
    ni_nameID = (*env)->GetFieldID(env, ni_class, "name", "Ljava/lang/String;");
    CHECK_NULL(ni_nameID);
    ni_indexID = (*env)->GetFieldID(env, ni_class, "index", "I");
    CHECK_NULL(ni_indexID);
    ni_addrsID = (*env)->GetFieldID(env, ni_class, "addrs",
                                    "[Ljava/net/InetAddress;");
    CHECK_NULL(ni_addrsID);
    ni_bindsID = (*env)->GetFieldID(env, ni_class, "bindings",
                                    "[Ljava/net/InterfaceAddress;");
    CHECK_NULL(ni_bindsID);
    ni_descID = (*env)->GetFieldID(env, ni_class, "displayName",
                                   "Ljava/lang/String;");
    CHECK_NULL(ni_descID);
    ni_virutalID = (*env)->GetFieldID(env, ni_class, "virtual", "Z");
    CHECK_NULL(ni_virutalID);
    ni_childsID = (*env)->GetFieldID(env, ni_class, "childs",
                                     "[Ljava/net/NetworkInterface;");
    CHECK_NULL(ni_childsID);
    ni_parentID = (*env)->GetFieldID(env, ni_class, "parent",
                                     "Ljava/net/NetworkInterface;");
    CHECK_NULL(ni_parentID);
    ni_ctrID = (*env)->GetMethodID(env, ni_class, "<init>", "()V");
    CHECK_NULL(ni_ctrID);
    ni_ibcls = (*env)->FindClass(env, "java/net/InterfaceAddress");
    CHECK_NULL(ni_ibcls);
    ni_ibcls = (*env)->NewGlobalRef(env, ni_ibcls);
    CHECK_NULL(ni_ibcls);
    ni_ibctrID = (*env)->GetMethodID(env, ni_ibcls, "<init>", "()V");
    CHECK_NULL(ni_ibctrID);
    ni_ibaddressID = (*env)->GetFieldID(env, ni_ibcls, "address",
                                        "Ljava/net/InetAddress;");
    CHECK_NULL(ni_ibaddressID);
    ni_ib4broadcastID = (*env)->GetFieldID(env, ni_ibcls, "broadcast",
                                           "Ljava/net/Inet4Address;");
    CHECK_NULL(ni_ib4broadcastID);
    ni_ib4maskID = (*env)->GetFieldID(env, ni_ibcls, "maskLength", "S");
    CHECK_NULL(ni_ib4maskID);
    ni_defaultIndexID = (*env)->GetStaticFieldID(env, ni_class, "defaultIndex",
                                                 "I");
    CHECK_NULL(ni_defaultIndexID);
    initInetAddressIDs(env);
}

/*
 * Class:     java_net_NetworkInterface
 * Method:    getByName0
 * Signature: (Ljava/lang/String;)Ljava/net/NetworkInterface;
 */
JNIEXPORT jobject JNICALL Java_java_net_NetworkInterface_getByName0
  (JNIEnv *env, jclass cls, jstring name)
{
    netif *ifs, *curr;
    jboolean isCopy;
    const char* name_utf;
    char *colonP;
    jobject obj = NULL;

    if (name != NULL) {
        name_utf = (*env)->GetStringUTFChars(env, name, &isCopy);
    } else {
        JNU_ThrowNullPointerException(env, "network interface name is NULL");
        return NULL;
    }

    if (name_utf == NULL) {
        if (!(*env)->ExceptionCheck(env))
            JNU_ThrowOutOfMemoryError(env, NULL);
        return NULL;
    }

    ifs = enumInterfaces(env);
    if (ifs == NULL) {
        (*env)->ReleaseStringUTFChars(env, name, name_utf);
        return NULL;
    }

    // search the list of interfaces based on name,
    // if it is virtual sub interface search with parent first.
    colonP = strchr(name_utf, ':');
    size_t limit = colonP != NULL ? (size_t)(colonP - name_utf) : strlen(name_utf);
    curr = ifs;
    while (curr != NULL) {
        if (strlen(curr->name) == limit && memcmp(name_utf, curr->name, limit) == 0) {
            break;
        }
        curr = curr->next;
    }

    // search the child list
    if (colonP != NULL && curr != NULL) {
        curr = curr->childs;
        while (curr != NULL) {
            if (strcmp(name_utf, curr->name) == 0) {
                break;
            }
            curr = curr->next;
        }
    }

    // if found create a NetworkInterface
    if (curr != NULL) {
        obj = createNetworkInterface(env, curr);
    }

    // release the UTF string and interface list
    (*env)->ReleaseStringUTFChars(env, name, name_utf);
    freeif(ifs);

    return obj;
}

/*
 * Class:     java_net_NetworkInterface
 * Method:    getByIndex0
 * Signature: (Ljava/lang/String;)Ljava/net/NetworkInterface;
 */
JNIEXPORT jobject JNICALL Java_java_net_NetworkInterface_getByIndex0
  (JNIEnv *env, jclass cls, jint index)
{
    netif *ifs, *curr;
    jobject obj = NULL;

    if (index <= 0) {
        return NULL;
    }

    ifs = enumInterfaces(env);
    if (ifs == NULL) {
        return NULL;
    }

    // search the list of interfaces based on index
    curr = ifs;
    while (curr != NULL) {
        if (index == curr->index) {
            break;
        }
        curr = curr->next;
    }

    // if found create a NetworkInterface
    if (curr != NULL) {
        obj = createNetworkInterface(env, curr);
    }

    // release the interface list
    freeif(ifs);

    return obj;
}

// Return the interface in ifs that iaObj is bound to, if any - otherwise NULL
static netif* find_bound_interface(JNIEnv *env, netif* ifs, jobject iaObj, int family) {
    netif* curr = ifs;
    while (curr != NULL) {
        netaddr *addrP = curr->addr;

        // iterate through each address on the interface
        while (addrP != NULL) {

            if (family == addrP->family) {
                if (family == AF_INET) {
                    int address1 = htonl(
                        ((struct sockaddr_in *)addrP->addr)->sin_addr.s_addr);
                    int address2 = getInetAddress_addr(env, iaObj);
                    if ((*env)->ExceptionCheck(env)) {
                        return NULL;
                    }
                    if (address1 == address2) {
                        return curr;
                    }
                } else if (family == AF_INET6) {
                    jbyte *bytes = (jbyte *)&(
                        ((struct sockaddr_in6*)addrP->addr)->sin6_addr);
                    jbyte caddr[16];
                    int i;
                    unsigned int scopeid;
                    getInet6Address_ipaddress(env, iaObj, (char *)caddr);
                    scopeid = (unsigned int)getInet6Address_scopeid(env, iaObj);
                    if (scopeid != 0 && scopeid != ((struct sockaddr_in6*)addrP->addr)->sin6_scope_id)
                        break;
                    i = 0;
                    while (i < 16) {
                        if (caddr[i] != bytes[i]) {
                            break;
                        }
                        i++;
                    }
                    if (i >= 16) {
                        return curr;
                    }
                }
            }

            addrP = addrP->next;
        }
        curr = curr->next;
    }

    return NULL;
}

/*
 * Class:     java_net_NetworkInterface
 * Method:    boundInetAddress0
 * Signature: (Ljava/net/InetAddress;)boundInetAddress;
 */
JNIEXPORT jboolean JNICALL Java_java_net_NetworkInterface_boundInetAddress0
    (JNIEnv *env, jclass cls, jobject iaObj)
{
    netif *ifs = NULL;
    jboolean bound = JNI_FALSE;
    int sock;

    int family = getInetAddress_family(env, iaObj);
    JNU_CHECK_EXCEPTION_RETURN(env, JNI_FALSE);

    if (family == java_net_InetAddress_IPv4) {
        family = AF_INET;
    } else if (family == java_net_InetAddress_IPv6) {
        family = AF_INET6;
    } else {
        return JNI_FALSE; // Invalid family
    }

    if (family == AF_INET) {
        sock = openSocket(env, AF_INET);
        if (sock < 0 && (*env)->ExceptionOccurred(env)) {
            return JNI_FALSE;
        }

        // enumerate IPv4 addresses
        if (sock >= 0) {
            ifs = enumIPv4Interfaces(env, sock, ifs);
            close(sock);

            if ((*env)->ExceptionOccurred(env)) {
                goto cleanup;
            }
        }
        if (find_bound_interface(env, ifs, iaObj, family) != NULL)
            bound = JNI_TRUE;
    } else if (ipv6_available()) {
        // If IPv6 is available then enumerate IPv6 addresses.
        // User can disable ipv6 explicitly by -Djava.net.preferIPv4Stack=true,
        // so we have to call ipv6_available()
        sock = openSocket(env, AF_INET6);
        if (sock < 0) {
            return JNI_FALSE;
        }

        ifs = enumIPv6Interfaces(env, sock, ifs);
        close(sock);

        if ((*env)->ExceptionOccurred(env)) {
            goto cleanup;
        }

        if (find_bound_interface(env, ifs, iaObj, family) != NULL)
            bound = JNI_TRUE;
    }

cleanup:
    freeif(ifs);

    return bound;
}

/*
 * Class:     java_net_NetworkInterface
 * Method:    getByInetAddress0
 * Signature: (Ljava/net/InetAddress;)Ljava/net/NetworkInterface;
 */
JNIEXPORT jobject JNICALL Java_java_net_NetworkInterface_getByInetAddress0
  (JNIEnv *env, jclass cls, jobject iaObj)
{
    netif *ifs, *curr;
    jobject obj = NULL;
    int family = getInetAddress_family(env, iaObj);
    JNU_CHECK_EXCEPTION_RETURN(env, NULL);

    if (family == java_net_InetAddress_IPv4) {
        family = AF_INET;
    } else if (family == java_net_InetAddress_IPv6) {
        family = AF_INET6;
    } else {
        return NULL; // Invalid family
    }
    ifs = enumInterfaces(env);
    if (ifs == NULL) {
        return NULL;
    }

    curr = find_bound_interface(env, ifs, iaObj, family);

    // if found create a NetworkInterface
    if (curr != NULL) {
        obj = createNetworkInterface(env, curr);
    }

    // release the interface list
    freeif(ifs);

    return obj;
}

/*
 * Class:     java_net_NetworkInterface
 * Method:    getAll
 * Signature: ()[Ljava/net/NetworkInterface;
 */
JNIEXPORT jobjectArray JNICALL Java_java_net_NetworkInterface_getAll
  (JNIEnv *env, jclass cls)
{
    netif *ifs, *curr;
    jobjectArray netIFArr;
    jint arr_index, ifCount;

    ifs = enumInterfaces(env);
    if (ifs == NULL) {
        return NULL;
    }

    // count the interfaces
    ifCount = 0;
    curr = ifs;
    while (curr != NULL) {
        ifCount++;
        curr = curr->next;
    }

    // allocate a NetworkInterface array
    netIFArr = (*env)->NewObjectArray(env, ifCount, cls, NULL);
    if (netIFArr == NULL) {
        freeif(ifs);
        return NULL;
    }

    // iterate through the interfaces, create a NetworkInterface instance
    // for each array element and populate the object
    curr = ifs;
    arr_index = 0;
    while (curr != NULL) {
        jobject netifObj;

        netifObj = createNetworkInterface(env, curr);
        if (netifObj == NULL) {
            freeif(ifs);
            return NULL;
        }

        // put the NetworkInterface into the array
        (*env)->SetObjectArrayElement(env, netIFArr, arr_index++, netifObj);
        (*env)->DeleteLocalRef(env, netifObj);

        curr = curr->next;
    }

    // release the interface list
    freeif(ifs);

    return netIFArr;
}

/*
 * Class:     java_net_NetworkInterface
 * Method:    isUp0
 * Signature: (Ljava/lang/String;I)Z
 */
JNIEXPORT jboolean JNICALL Java_java_net_NetworkInterface_isUp0
  (JNIEnv *env, jclass cls, jstring name, jint index)
{
    int ret = getFlags0(env, name);
    return ((ret & IFF_UP) && (ret & IFF_RUNNING)) ? JNI_TRUE :  JNI_FALSE;
}

/*
 * Class:     java_net_NetworkInterface
 * Method:    isP2P0
 * Signature: (Ljava/lang/String;I)Z
 */
JNIEXPORT jboolean JNICALL Java_java_net_NetworkInterface_isP2P0
  (JNIEnv *env, jclass cls, jstring name, jint index)
{
    int ret = getFlags0(env, name);
    return (ret & IFF_POINTOPOINT) ? JNI_TRUE :  JNI_FALSE;
}

/*
 * Class:     java_net_NetworkInterface
 * Method:    isLoopback0
 * Signature: (Ljava/lang/String;I)Z
 */
JNIEXPORT jboolean JNICALL Java_java_net_NetworkInterface_isLoopback0
  (JNIEnv *env, jclass cls, jstring name, jint index)
{
    int ret = getFlags0(env, name);
    return (ret & IFF_LOOPBACK) ? JNI_TRUE :  JNI_FALSE;
}

/*
 * Class:     java_net_NetworkInterface
 * Method:    supportsMulticast0
 * Signature: (Ljava/lang/String;I)Z
 */
JNIEXPORT jboolean JNICALL Java_java_net_NetworkInterface_supportsMulticast0
  (JNIEnv *env, jclass cls, jstring name, jint index)
{
    int ret = getFlags0(env, name);
    return (ret & IFF_MULTICAST) ? JNI_TRUE :  JNI_FALSE;
}

/*
 * Class:     java_net_NetworkInterface
 * Method:    getMacAddr0
 * Signature: ([bLjava/lang/String;I)[b
 */
JNIEXPORT jbyteArray JNICALL Java_java_net_NetworkInterface_getMacAddr0
  (JNIEnv *env, jclass cls, jbyteArray addrArray, jstring name, jint index)
{
    jint addr;
    jbyte caddr[4];
    struct in_addr iaddr;
    jbyteArray ret = NULL;
    unsigned char mac[16];
    int len;
    jboolean isCopy;
    const char *name_utf;

    if (name != NULL) {
        name_utf = (*env)->GetStringUTFChars(env, name, &isCopy);
    } else {
        JNU_ThrowNullPointerException(env, "network interface name is NULL");
        return NULL;
    }

    if (name_utf == NULL) {
        if (!(*env)->ExceptionCheck(env))
            JNU_ThrowOutOfMemoryError(env, NULL);
        return NULL;
    }

    if (!IS_NULL(addrArray)) {
        (*env)->GetByteArrayRegion(env, addrArray, 0, 4, caddr);
        addr = ((caddr[0]<<24) & 0xff000000);
        addr |= ((caddr[1] <<16) & 0xff0000);
        addr |= ((caddr[2] <<8) & 0xff00);
        addr |= (caddr[3] & 0xff);
        iaddr.s_addr = htonl(addr);
        len = getMacAddress(env, name_utf, &iaddr, mac);
    } else {
        len = getMacAddress(env, name_utf, NULL, mac);
    }

    if (len > 0) {
        ret = (*env)->NewByteArray(env, len);
        if (!IS_NULL(ret)) {
            (*env)->SetByteArrayRegion(env, ret, 0, len, (jbyte *)(mac));
        }
    }

    // release the UTF string and interface list
    (*env)->ReleaseStringUTFChars(env, name, name_utf);

    return ret;
}

/*
 * Class:       java_net_NetworkInterface
 * Method:      getMTU0
 * Signature:   ([bLjava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_java_net_NetworkInterface_getMTU0
  (JNIEnv *env, jclass cls, jstring name, jint index)
{
    jboolean isCopy;
    int sock, ret = -1;
    const char* name_utf = NULL;

    if (name != NULL) {
        name_utf = (*env)->GetStringUTFChars(env, name, &isCopy);
    } else {
        JNU_ThrowNullPointerException(env, "network interface name is NULL");
        return ret;
    }

    if (name_utf == NULL) {
        if (!(*env)->ExceptionCheck(env))
            JNU_ThrowOutOfMemoryError(env, NULL);
        return ret;
    }

    if ((sock = openSocketWithFallback(env, name_utf)) < 0) {
        (*env)->ReleaseStringUTFChars(env, name, name_utf);
        return JNI_FALSE;
    }

    ret = getMTU(env, sock, name_utf);

    (*env)->ReleaseStringUTFChars(env, name, name_utf);

    close(sock);
    return ret;
}

/*** Private methods definitions ****/

static int getFlags0(JNIEnv *env, jstring name) {
    jboolean isCopy;
    int ret, sock, flags = 0;
    const char *name_utf;

    if (name != NULL) {
        name_utf = (*env)->GetStringUTFChars(env, name, &isCopy);
    } else {
        JNU_ThrowNullPointerException(env, "network interface name is NULL");
        return -1;
    }

    if (name_utf == NULL) {
        if (!(*env)->ExceptionCheck(env))
            JNU_ThrowOutOfMemoryError(env, NULL);
        return -1;
    }
    if ((sock = openSocketWithFallback(env, name_utf)) < 0) {
        (*env)->ReleaseStringUTFChars(env, name, name_utf);
        return -1;
    }

    ret = getFlags(sock, name_utf, &flags);

    close(sock);
    (*env)->ReleaseStringUTFChars(env, name, name_utf);

    if (ret < 0) {
        JNU_ThrowByNameWithMessageAndLastError
            (env, JNU_JAVANETPKG "SocketException", "getFlags() failed");
        return -1;
    }

    return flags;
}

/*
 * Creates a NetworkInterface object, populates the name, the index, and
 * populates the InetAddress array based on the IP addresses for this
 * interface.
 */
static jobject createNetworkInterface(JNIEnv *env, netif *ifs) {
    jobject netifObj;
    jobject name;
    jobjectArray addrArr;
    jobjectArray bindArr;
    jobjectArray childArr;
    netaddr *addrs;
    jint addr_index, addr_count, bind_index;
    jint child_count, child_index;
    netaddr *addrP;
    netif *childP;
    jobject tmp;

    // create a NetworkInterface object and populate it
    netifObj = (*env)->NewObject(env, ni_class, ni_ctrID);
    CHECK_NULL_RETURN(netifObj, NULL);
    name = (*env)->NewStringUTF(env, ifs->name);
    CHECK_NULL_RETURN(name, NULL);
    (*env)->SetObjectField(env, netifObj, ni_nameID, name);
    (*env)->SetObjectField(env, netifObj, ni_descID, name);
    (*env)->SetIntField(env, netifObj, ni_indexID, ifs->index);
    (*env)->SetBooleanField(env, netifObj, ni_virutalID,
                            ifs->virtual ? JNI_TRUE : JNI_FALSE);

    // count the number of addresses on this interface
    addr_count = 0;
    addrP = ifs->addr;
    while (addrP != NULL) {
        addr_count++;
        addrP = addrP->next;
    }

    // create the array of InetAddresses
    addrArr = (*env)->NewObjectArray(env, addr_count, ia_class, NULL);
    if (addrArr == NULL) {
        return NULL;
    }

    bindArr = (*env)->NewObjectArray(env, addr_count, ni_ibcls, NULL);
    if (bindArr == NULL) {
       return NULL;
    }
    addrP = ifs->addr;
    addr_index = 0;
    bind_index = 0;
    while (addrP != NULL) {
        jobject iaObj = NULL;
        jobject ibObj = NULL;

        if (addrP->family == AF_INET) {
            iaObj = (*env)->NewObject(env, ia4_class, ia4_ctrID);
            if (iaObj) {
                setInetAddress_addr(env, iaObj, htonl(
                    ((struct sockaddr_in*)addrP->addr)->sin_addr.s_addr));
                JNU_CHECK_EXCEPTION_RETURN(env, NULL);
            } else {
                return NULL;
            }
            ibObj = (*env)->NewObject(env, ni_ibcls, ni_ibctrID);
            if (ibObj) {
                (*env)->SetObjectField(env, ibObj, ni_ibaddressID, iaObj);
                if (addrP->brdcast) {
                    jobject ia2Obj = NULL;
                    ia2Obj = (*env)->NewObject(env, ia4_class, ia4_ctrID);
                    if (ia2Obj) {
                        setInetAddress_addr(env, ia2Obj, htonl(
                            ((struct sockaddr_in*)addrP->brdcast)->sin_addr.s_addr));
                        JNU_CHECK_EXCEPTION_RETURN(env, NULL);
                        (*env)->SetObjectField(env, ibObj, ni_ib4broadcastID, ia2Obj);
                        (*env)->DeleteLocalRef(env, ia2Obj);
                    } else {
                        return NULL;
                    }
                }
                (*env)->SetShortField(env, ibObj, ni_ib4maskID, addrP->mask);
                (*env)->SetObjectArrayElement(env, bindArr, bind_index++, ibObj);
                (*env)->DeleteLocalRef(env, ibObj);
            } else {
                return NULL;
            }
        }
        if (addrP->family == AF_INET6) {
            int scope=0;
            iaObj = (*env)->NewObject(env, ia6_class, ia6_ctrID);
            if (iaObj) {
                jboolean ret = setInet6Address_ipaddress(env, iaObj,
                    (char *)&(((struct sockaddr_in6*)addrP->addr)->sin6_addr));
                if (ret == JNI_FALSE) {
                    return NULL;
                }

                scope = ((struct sockaddr_in6*)addrP->addr)->sin6_scope_id;

                if (scope != 0) { /* zero is default value, no need to set */
                    setInet6Address_scopeid(env, iaObj, scope);
                    setInet6Address_scopeifname(env, iaObj, netifObj);
                }
            } else {
                return NULL;
            }
            ibObj = (*env)->NewObject(env, ni_ibcls, ni_ibctrID);
            if (ibObj) {
                (*env)->SetObjectField(env, ibObj, ni_ibaddressID, iaObj);
                (*env)->SetShortField(env, ibObj, ni_ib4maskID, addrP->mask);
                (*env)->SetObjectArrayElement(env, bindArr, bind_index++, ibObj);
                (*env)->DeleteLocalRef(env, ibObj);
            } else {
                return NULL;
            }
        }

        (*env)->SetObjectArrayElement(env, addrArr, addr_index++, iaObj);
        (*env)->DeleteLocalRef(env, iaObj);
        addrP = addrP->next;
    }

    // see if there is any virtual interface attached to this one.
    child_count = 0;
    childP = ifs->childs;
    while (childP) {
        child_count++;
        childP = childP->next;
    }

    childArr = (*env)->NewObjectArray(env, child_count, ni_class, NULL);
    if (childArr == NULL) {
        return NULL;
    }

    // create the NetworkInterface instances for the sub-interfaces as well
    child_index = 0;
    childP = ifs->childs;
    while(childP) {
        tmp = createNetworkInterface(env, childP);
        if (tmp == NULL) {
            return NULL;
        }
        (*env)->SetObjectField(env, tmp, ni_parentID, netifObj);
        (*env)->SetObjectArrayElement(env, childArr, child_index++, tmp);
        childP = childP->next;
    }
    (*env)->SetObjectField(env, netifObj, ni_addrsID, addrArr);
    (*env)->SetObjectField(env, netifObj, ni_bindsID, bindArr);
    (*env)->SetObjectField(env, netifObj, ni_childsID, childArr);

    (*env)->DeleteLocalRef(env, name);
    (*env)->DeleteLocalRef(env, addrArr);
    (*env)->DeleteLocalRef(env, bindArr);
    (*env)->DeleteLocalRef(env, childArr);

    // return the NetworkInterface
    return netifObj;
}

/*
 * Enumerates all interfaces
 */
static netif *enumInterfaces(JNIEnv *env) {
    netif *ifs = NULL;
    int sock;

    sock = openSocket(env, AF_INET);
    if (sock < 0 && (*env)->ExceptionOccurred(env)) {
        return NULL;
    }

    // enumerate IPv4 addresses
    if (sock >= 0) {
        ifs = enumIPv4Interfaces(env, sock, ifs);
        close(sock);

        if ((*env)->ExceptionOccurred(env)) {
            freeif(ifs);
            return NULL;
        }
    }

    // If IPv6 is available then enumerate IPv6 addresses.
    // User can disable ipv6 explicitly by -Djava.net.preferIPv4Stack=true,
    // so we have to call ipv6_available()
    if (ipv6_available()) {
        sock = openSocket(env, AF_INET6);
        if (sock < 0) {
            freeif(ifs);
            return NULL;
        }

        ifs = enumIPv6Interfaces(env, sock, ifs);
        close(sock);

        if ((*env)->ExceptionOccurred(env)) {
            freeif(ifs);
            return NULL;
        }
    }

    return ifs;
}

/*
 * Frees an interface list (including any attached addresses).
 */
static void freeif(netif *ifs) {
    netif *currif = ifs;
    netif *child = NULL;

    while (currif != NULL) {
        netaddr *addrP = currif->addr;
        while (addrP != NULL) {
            netaddr *next = addrP->next;
            free(addrP);
            addrP = next;
        }

        // don't forget to free the sub-interfaces
        if (currif->childs != NULL) {
            freeif(currif->childs);
        }

        ifs = currif->next;
        free(currif);
        currif = ifs;
    }
}

static netif *addif(JNIEnv *env, int sock, const char *if_name, netif *ifs,
                    struct sockaddr *ifr_addrP,
                    struct sockaddr *ifr_broadaddrP,
                    int family, short prefix)
{
    netif *currif = ifs, *parent;
    netaddr *addrP;
    char name[IFNAMESIZE], vname[IFNAMESIZE];
    char *name_colonP;
    int isVirtual = 0;
    int addr_size;

    // If the interface name is a logical interface then we remove the unit
    // number so that we have the physical interface (eg: hme0:1 -> hme0).
    // NetworkInterface currently doesn't have any concept of physical vs.
    // logical interfaces.
    strncpy(name, if_name, IFNAMESIZE);
    name[IFNAMESIZE - 1] = '\0';
    *vname = 0;

    // Create and populate the netaddr node. If allocation fails
    // return an un-updated list.

    // Allocate for addr and brdcast at once

    addr_size = (family == AF_INET) ? sizeof(struct sockaddr_in)
                                    : sizeof(struct sockaddr_in6);

    CHECKED_MALLOC3(addrP, netaddr *, sizeof(netaddr) + 2 * addr_size);
    addrP->addr = (struct sockaddr *)((char *)addrP + sizeof(netaddr));
    memcpy(addrP->addr, ifr_addrP, addr_size);

    addrP->family = family;
    addrP->mask = prefix;
    addrP->next = 0;

    // for IPv4 add broadcast address
    if (family == AF_INET && ifr_broadaddrP != NULL) {
        addrP->brdcast = (struct sockaddr *)
                             ((char *)addrP + sizeof(netaddr) + addr_size);
        memcpy(addrP->brdcast, ifr_broadaddrP, addr_size);
    } else {
        addrP->brdcast = NULL;
    }

    // Deal with virtual interface with colon notation e.g. eth0:1
    name_colonP = strchr(name, ':');
    if (name_colonP != NULL) {
        int flags = 0;
        // This is a virtual interface. If we are able to access the parent
        // we need to create a new entry if it doesn't exist yet *and* update
        // the 'parent' interface with the new records.
        *name_colonP = 0;
        if (getFlags(sock, name, &flags) < 0 || flags < 0) {
            // failed to access parent interface do not create parent.
            // We are a virtual interface with no parent.
            isVirtual = 1;
            *name_colonP = ':';
        } else {
            // Got access to parent, so create it if necessary.
            // Save original name to vname and truncate name by ':'
            memcpy(vname, name, sizeof(vname));
            vname[name_colonP - name] = ':';
        }
    }

    // Check if this is a "new" interface. Use the interface name for
    // matching because index isn't supported on Solaris 2.6 & 7.
    while (currif != NULL) {
        if (strcmp(name, currif->name) == 0) {
            break;
        }
        currif = currif->next;
    }

    // If "new" then create a netif structure and insert it into the list.
    if (currif == NULL) {
         CHECKED_MALLOC3(currif, netif *, sizeof(netif) + IFNAMESIZE);
         currif->name = (char *)currif + sizeof(netif);
         strncpy(currif->name, name, IFNAMESIZE);
         currif->name[IFNAMESIZE - 1] = '\0';
         currif->index = getIndex(sock, name);
         currif->addr = NULL;
         currif->childs = NULL;
         currif->virtual = isVirtual;
         currif->next = ifs;
         ifs = currif;
    }

    // Finally insert the address on the interface
    addrP->next = currif->addr;
    currif->addr = addrP;

    parent = currif;

    // Deal with the virtual interface now.
    if (vname[0]) {
        netaddr *tmpaddr;

        currif = parent->childs;

        while (currif != NULL) {
            if (strcmp(vname, currif->name) == 0) {
                break;
            }
            currif = currif->next;
        }

        if (currif == NULL) {
            CHECKED_MALLOC3(currif, netif *, sizeof(netif) + IFNAMESIZE);
            currif->name = (char *)currif + sizeof(netif);
            strncpy(currif->name, vname, IFNAMESIZE);
            currif->name[IFNAMESIZE - 1] = '\0';
            currif->index = getIndex(sock, vname);
            currif->addr = NULL; // Need to duplicate the addr entry?
            currif->virtual = 1;
            currif->childs = NULL;
            currif->next = parent->childs;
            parent->childs = currif;
        }

        CHECKED_MALLOC3(tmpaddr, netaddr *, sizeof(netaddr) + 2 * addr_size);
        memcpy(tmpaddr, addrP, sizeof(netaddr));
        if (addrP->addr != NULL) {
            tmpaddr->addr = (struct sockaddr *)
                ((char*)tmpaddr + sizeof(netaddr));
            memcpy(tmpaddr->addr, addrP->addr, addr_size);
        }

        if (addrP->brdcast != NULL) {
            tmpaddr->brdcast = (struct sockaddr *)
                ((char *)tmpaddr + sizeof(netaddr) + addr_size);
            memcpy(tmpaddr->brdcast, addrP->brdcast, addr_size);
        }

        tmpaddr->next = currif->addr;
        currif->addr = tmpaddr;
    }

    return ifs;
}

/*
 * Determines the prefix value for an AF_INET subnet address.
 */
static short translateIPv4AddressToPrefix(struct sockaddr_in *addr) {
    short prefix = 0;
    unsigned int mask;
    if (addr == NULL) {
        return 0;
    }
    mask = ntohl(addr->sin_addr.s_addr);
    while (mask) {
        mask <<= 1;
        prefix++;
    }
    return prefix;
}

/*
 * Determines the prefix value for an AF_INET6 subnet address.
 */
static short translateIPv6AddressToPrefix(struct sockaddr_in6 *addr) {
    short prefix = 0;
    u_char *addrBytes;
    if (addr == NULL) {
        return 0;
    }
    addrBytes = (u_char *)&(addr->sin6_addr);
    unsigned int byte, bit;

    for (byte = 0; byte < sizeof(struct in6_addr); byte++, prefix += 8) {
        if (addrBytes[byte] != 0xff) {
            break;
        }
    }
    if (byte != sizeof(struct in6_addr)) {
        for (bit = 7; bit != 0; bit--, prefix++) {
            if (!(addrBytes[byte] & (1 << bit))) {
                break;
            }
        }
        for (; bit != 0; bit--) {
            if (addrBytes[byte] & (1 << bit)) {
                prefix = 0;
                break;
            }
        }
        if (prefix > 0) {
            byte++;
            for (; byte < sizeof(struct in6_addr); byte++) {
                if (addrBytes[byte]) {
                    prefix = 0;
                }
            }
        }
    }

    return prefix;
}

/*
 * Opens a socket for further ioct calls. proto is one of AF_INET or AF_INET6.
 */
static int openSocket(JNIEnv *env, int proto) {
    int sock;

    if ((sock = socket(proto, SOCK_DGRAM, 0)) < 0) {
        // If we lack support for this address family or protocol,
        // don't throw an exception.
        if (errno != EPROTONOSUPPORT && errno != EAFNOSUPPORT) {
            JNU_ThrowByNameWithMessageAndLastError
                (env, JNU_JAVANETPKG "SocketException", "Socket creation failed");
        }
        return -1;
    }

    return sock;
}

/** Linux **/
#if defined(__linux__)

/*
 * Opens a socket for further ioctl calls. Tries AF_INET socket first and
 * if it fails return AF_INET6 socket.
 */
static int openSocketWithFallback(JNIEnv *env, const char *ifname) {
    int sock;

    if ((sock = socket(AF_INET, SOCK_DGRAM, 0)) < 0) {
        if (errno == EPROTONOSUPPORT || errno == EAFNOSUPPORT) {
            if ((sock = socket(AF_INET6, SOCK_DGRAM, 0)) < 0) {
                JNU_ThrowByNameWithMessageAndLastError
                    (env, JNU_JAVANETPKG "SocketException", "IPV6 Socket creation failed");
                return -1;
            }
        } else { // errno is not NOSUPPORT
            JNU_ThrowByNameWithMessageAndLastError
                (env, JNU_JAVANETPKG "SocketException", "IPV4 Socket creation failed");
            return -1;
        }
    }

    // Linux starting from 2.6.? kernel allows ioctl call with either IPv4 or
    // IPv6 socket regardless of type of address of an interface.
    return sock;
}

/*
 * Enumerates and returns all IPv4 interfaces on Linux.
 */
static netif *enumIPv4Interfaces(JNIEnv *env, int sock, netif *ifs) {
    struct ifconf ifc;
    struct ifreq *ifreqP;
    char *buf = NULL;
    unsigned i;

    // do a dummy SIOCGIFCONF to determine the buffer size
    // SIOCGIFCOUNT doesn't work
    ifc.ifc_buf = NULL;
    if (ioctl(sock, SIOCGIFCONF, (char *)&ifc) < 0) {
        JNU_ThrowByNameWithMessageAndLastError
            (env, JNU_JAVANETPKG "SocketException", "ioctl(SIOCGIFCONF) failed");
        return ifs;
    }

    // call SIOCGIFCONF to enumerate the interfaces
    CHECKED_MALLOC3(buf, char *, ifc.ifc_len);
    ifc.ifc_buf = buf;
    if (ioctl(sock, SIOCGIFCONF, (char *)&ifc) < 0) {
        JNU_ThrowByNameWithMessageAndLastError
            (env, JNU_JAVANETPKG "SocketException", "ioctl(SIOCGIFCONF) failed");
        free(buf);
        return ifs;
    }

    // iterate through each interface
    ifreqP = ifc.ifc_req;
    for (i = 0; i < ifc.ifc_len / sizeof(struct ifreq); i++, ifreqP++) {
        struct sockaddr addr, broadaddr, *broadaddrP = NULL;
        short prefix = 0;

        // ignore non IPv4 addresses
        if (ifreqP->ifr_addr.sa_family != AF_INET) {
            continue;
        }

        // save socket address
        memcpy(&addr, &(ifreqP->ifr_addr), sizeof(struct sockaddr));

        // determine broadcast address, if applicable
        if ((ioctl(sock, SIOCGIFFLAGS, ifreqP) == 0) &&
            ifreqP->ifr_flags & IFF_BROADCAST) {

            // restore socket address to ifreqP
            memcpy(&(ifreqP->ifr_addr), &addr, sizeof(struct sockaddr));

            if (ioctl(sock, SIOCGIFBRDADDR, ifreqP) == 0) {
                memcpy(&broadaddr, &(ifreqP->ifr_broadaddr),
                       sizeof(struct sockaddr));
                broadaddrP = &broadaddr;
            }
        }

        // restore socket address to ifreqP
        memcpy(&(ifreqP->ifr_addr), &addr, sizeof(struct sockaddr));

        // determine netmask
        if (ioctl(sock, SIOCGIFNETMASK, ifreqP) == 0) {
            prefix = translateIPv4AddressToPrefix(
                         (struct sockaddr_in *)&(ifreqP->ifr_netmask));
        }

        // add interface to the list
        ifs = addif(env, sock, ifreqP->ifr_name, ifs,
                    &addr, broadaddrP, AF_INET, prefix);

        // in case of exception, free interface list and buffer and return NULL
        if ((*env)->ExceptionOccurred(env)) {
            free(buf);
            freeif(ifs);
            return NULL;
        }
    }

    // free buffer
    free(buf);
    return ifs;
}

/*
 * Enumerates and returns all IPv6 interfaces on Linux.
 */
static netif *enumIPv6Interfaces(JNIEnv *env, int sock, netif *ifs) {
    FILE *f;
    char devname[21], addr6p[8][5];
    int prefix, scope, dad_status, if_idx;

    if ((f = fopen(_PATH_PROCNET_IFINET6, "r")) != NULL) {
        while (fscanf(f, "%4s%4s%4s%4s%4s%4s%4s%4s %08x %02x %02x %02x %20s\n",
                      addr6p[0], addr6p[1], addr6p[2], addr6p[3],
                      addr6p[4], addr6p[5], addr6p[6], addr6p[7],
                      &if_idx, &prefix, &scope, &dad_status, devname) != EOF) {

            char addr6[40];
            struct sockaddr_in6 addr;

            sprintf(addr6, "%s:%s:%s:%s:%s:%s:%s:%s",
                    addr6p[0], addr6p[1], addr6p[2], addr6p[3],
                    addr6p[4], addr6p[5], addr6p[6], addr6p[7]);

            memset(&addr, 0, sizeof(struct sockaddr_in6));
            inet_pton(AF_INET6, addr6, (void*)addr.sin6_addr.s6_addr);

            // set scope ID to interface index
            addr.sin6_scope_id = if_idx;

            // add interface to the list
            ifs = addif(env, sock, devname, ifs, (struct sockaddr *)&addr,
                        NULL, AF_INET6, (short)prefix);

            // if an exception occurred then return the list as is
            if ((*env)->ExceptionOccurred(env)) {
                break;
            }
       }
       fclose(f);
    }
    return ifs;
}

/*
 * Try to get the interface index.
 */
static int getIndex(int sock, const char *name) {
    struct ifreq if2;
    memset((char *)&if2, 0, sizeof(if2));
    strncpy(if2.ifr_name, name, sizeof(if2.ifr_name));
    if2.ifr_name[sizeof(if2.ifr_name) - 1] = 0;

    if (ioctl(sock, SIOCGIFINDEX, (char *)&if2) < 0) {
        return -1;
    }

    return if2.ifr_ifindex;
}

/*
 * Gets the Hardware address (usually MAC address) for the named interface.
 * On return puts the data in buf, and returns the length, in byte, of the
 * MAC address. Returns -1 if there is no hardware address on that interface.
 */
static int getMacAddress
  (JNIEnv *env, const char *ifname, const struct in_addr *addr,
   unsigned char *buf)
{
    struct ifreq ifr;
    int i, sock;

    if ((sock = openSocketWithFallback(env, ifname)) < 0) {
        return -1;
    }

    memset((char *)&ifr, 0, sizeof(ifr));
    strncpy(ifr.ifr_name, ifname, sizeof(ifr.ifr_name) - 1);
    if (ioctl(sock, SIOCGIFHWADDR, &ifr) < 0) {
        JNU_ThrowByNameWithMessageAndLastError
            (env, JNU_JAVANETPKG "SocketException", "ioctl(SIOCGIFHWADDR) failed");
        close(sock);
        return -1;
    }

    close(sock);
    memcpy(buf, &ifr.ifr_hwaddr.sa_data, IFHWADDRLEN);

    // all bytes to 0 means no hardware address
    for (i = 0; i < IFHWADDRLEN; i++) {
        if (buf[i] != 0)
            return IFHWADDRLEN;
    }

    return -1;
}

static int getMTU(JNIEnv *env, int sock, const char *ifname) {
    struct ifreq if2;
    memset((char *)&if2, 0, sizeof(if2));
    strncpy(if2.ifr_name, ifname, sizeof(if2.ifr_name) - 1);

    if (ioctl(sock, SIOCGIFMTU, (char *)&if2) < 0) {
        JNU_ThrowByNameWithMessageAndLastError
            (env, JNU_JAVANETPKG "SocketException", "ioctl(SIOCGIFMTU) failed");
        return -1;
    }

    return if2.ifr_mtu;
}

static int getFlags(int sock, const char *ifname, int *flags) {
    struct ifreq if2;
    memset((char *)&if2, 0, sizeof(if2));
    strncpy(if2.ifr_name, ifname, sizeof(if2.ifr_name));
    if2.ifr_name[sizeof(if2.ifr_name) - 1] = 0;

    if (ioctl(sock, SIOCGIFFLAGS, (char *)&if2) < 0) {
        return -1;
    }

    if (sizeof(if2.ifr_flags) == sizeof(short)) {
        *flags = (if2.ifr_flags & 0xffff);
    } else {
        *flags = if2.ifr_flags;
    }
    return 0;
}

#endif /* __linux__ */

/** AIX **/
#if defined(_AIX)

/* seems getkerninfo is guarded by _KERNEL in the system headers */
/* see net/proto_uipc.h */
int getkerninfo(int, char *, int *, int32long64_t);

/*
 * Opens a socket for further ioctl calls. Tries AF_INET socket first and
 * if it fails return AF_INET6 socket.
 */
static int openSocketWithFallback(JNIEnv *env, const char *ifname) {
    int sock;

    if ((sock = socket(AF_INET, SOCK_DGRAM, 0)) < 0) {
        if (errno == EPROTONOSUPPORT || errno == EAFNOSUPPORT) {
            if ((sock = socket(AF_INET6, SOCK_DGRAM, 0)) < 0) {
                JNU_ThrowByNameWithMessageAndLastError
                    (env, JNU_JAVANETPKG "SocketException", "IPV6 Socket creation failed");
                return -1;
            }
        } else { // errno is not NOSUPPORT
            JNU_ThrowByNameWithMessageAndLastError
                (env, JNU_JAVANETPKG "SocketException", "IPV4 Socket creation failed");
            return -1;
        }
    }

    return sock;
}

/*
 * Enumerates and returns all IPv4 interfaces on AIX.
 */
static netif *enumIPv4Interfaces(JNIEnv *env, int sock, netif *ifs) {
    struct ifconf ifc;
    struct ifreq *ifreqP;
    char *buf = NULL;
    unsigned i;

    // call SIOCGSIZIFCONF to get the size of SIOCGIFCONF buffer
    if (ioctl(sock, SIOCGSIZIFCONF, &(ifc.ifc_len)) < 0) {
        JNU_ThrowByNameWithMessageAndLastError
            (env, JNU_JAVANETPKG "SocketException", "ioctl(SIOCGSIZIFCONF) failed");
        return ifs;
    }

    // call CSIOCGIFCONF instead of SIOCGIFCONF where interface
    // records will always have sizeof(struct ifreq) length.
    // Be aware that only IPv4 data is complete this way.
    CHECKED_MALLOC3(buf, char *, ifc.ifc_len);
    ifc.ifc_buf = buf;
    if (ioctl(sock, CSIOCGIFCONF, (char *)&ifc) < 0) {
        JNU_ThrowByNameWithMessageAndLastError
            (env, JNU_JAVANETPKG "SocketException", "ioctl(CSIOCGIFCONF) failed");
        free(buf);
        return ifs;
    }

    // iterate through each interface
    ifreqP = ifc.ifc_req;
    for (i = 0; i < ifc.ifc_len / sizeof(struct ifreq); i++, ifreqP++) {
        struct sockaddr addr, broadaddr, *broadaddrP = NULL;
        short prefix = 0;

        // ignore non IPv4 addresses
        if (ifreqP->ifr_addr.sa_family != AF_INET) {
            continue;
        }

        // save socket address
        memcpy(&addr, &(ifreqP->ifr_addr), sizeof(struct sockaddr));

        // determine broadcast address, if applicable
        if ((ioctl(sock, SIOCGIFFLAGS, ifreqP) == 0) &&
            ifreqP->ifr_flags & IFF_BROADCAST) {

            // restore socket address to ifreqP
            memcpy(&(ifreqP->ifr_addr), &addr, sizeof(struct sockaddr));

            if (ioctl(sock, SIOCGIFBRDADDR, ifreqP) == 0) {
                memcpy(&broadaddr, &(ifreqP->ifr_broadaddr),
                       sizeof(struct sockaddr));
                broadaddrP = &broadaddr;
            }
        }

        // restore socket address to ifreqP
        memcpy(&(ifreqP->ifr_addr), &addr, sizeof(struct sockaddr));

        // determine netmask
        if (ioctl(sock, SIOCGIFNETMASK, ifreqP) == 0) {
            prefix = translateIPv4AddressToPrefix(
                         (struct sockaddr_in *)&(ifreqP->ifr_addr));
        }

        // add interface to the list
        ifs = addif(env, sock, ifreqP->ifr_name, ifs,
                    &addr, broadaddrP, AF_INET, prefix);

        // in case of exception, free interface list and buffer and return NULL
        if ((*env)->ExceptionOccurred(env)) {
            free(buf);
            freeif(ifs);
            return NULL;
        }
    }

    // free buffer
    free(buf);
    return ifs;
}

/*
 * Enumerates and returns all IPv6 interfaces on AIX.
 */
static netif *enumIPv6Interfaces(JNIEnv *env, int sock, netif *ifs) {
    struct ifconf ifc;
    struct ifreq *ifreqP;
    char *buf, *cp, *cplimit;

    // call SIOCGSIZIFCONF to get size for SIOCGIFCONF buffer
    if (ioctl(sock, SIOCGSIZIFCONF, &(ifc.ifc_len)) < 0) {
        JNU_ThrowByNameWithMessageAndLastError
            (env, JNU_JAVANETPKG "SocketException", "ioctl(SIOCGSIZIFCONF) failed");
        return ifs;
    }

    // call SIOCGIFCONF to enumerate the interfaces
    CHECKED_MALLOC3(buf, char *, ifc.ifc_len);
    ifc.ifc_buf = buf;
    if (ioctl(sock, SIOCGIFCONF, (char *)&ifc) < 0) {
        JNU_ThrowByNameWithMessageAndLastError
            (env, JNU_JAVANETPKG "SocketException", "ioctl(SIOCGIFCONF) failed");
        free(buf);
        return ifs;
    }

    // iterate through each interface
    ifreqP = ifc.ifc_req;
    cp = (char *)ifc.ifc_req;
    cplimit = cp + ifc.ifc_len;

    for (; cp < cplimit;
         cp += (sizeof(ifreqP->ifr_name) +
                MAX((ifreqP->ifr_addr).sa_len, sizeof(ifreqP->ifr_addr))))
    {
        ifreqP = (struct ifreq *)cp;
        short prefix = 0;

        // ignore non IPv6 addresses
        if (ifreqP->ifr_addr.sa_family != AF_INET6) {
            continue;
        }

        // determine netmask
        struct in6_ifreq if6;
        memset((char *)&if6, 0, sizeof(if6));
        strncpy(if6.ifr_name, ifreqP->ifr_name, sizeof(if6.ifr_name) - 1);
        memcpy(&(if6.ifr_Addr), &(ifreqP->ifr_addr),
               sizeof(struct sockaddr_in6));
        if (ioctl(sock, SIOCGIFNETMASK6, (char *)&if6) >= 0) {
            prefix = translateIPv6AddressToPrefix(&(if6.ifr_Addr));
        }

        // set scope ID to interface index
        ((struct sockaddr_in6 *)&(ifreqP->ifr_addr))->sin6_scope_id =
            getIndex(sock, ifreqP->ifr_name);

        // add interface to the list
        ifs = addif(env, sock, ifreqP->ifr_name, ifs,
                    (struct sockaddr *)&(ifreqP->ifr_addr),
                    NULL, AF_INET6, prefix);

        // if an exception occurred then free the list
        if ((*env)->ExceptionOccurred(env)) {
            free(buf);
            freeif(ifs);
            return NULL;
        }
    }

    // free buffer
    free(buf);
    return ifs;
}

/*
 * Try to get the interface index.
 */
static int getIndex(int sock, const char *name) {
    int index = if_nametoindex(name);
    return (index == 0) ? -1 : index;
}

/*
 * Gets the Hardware address (usually MAC address) for the named interface.
 * On return puts the data in buf, and returns the length, in byte, of the
 * MAC address. Returns -1 if there is no hardware address on that interface.
 */
static int getMacAddress
  (JNIEnv *env, const char *ifname, const struct in_addr *addr,
   unsigned char *buf)
{
    int size;
    struct kinfo_ndd *nddp;
    void *end;

    size = getkerninfo(KINFO_NDD, 0, 0, 0);
    if (size == 0) {
        return -1;
    }

    if (size < 0) {
        perror("getkerninfo 1");
        return -1;
    }

    nddp = (struct kinfo_ndd *)malloc(size);

    if (!nddp) {
        JNU_ThrowOutOfMemoryError(env,
            "Network interface getMacAddress native buffer allocation failed");
        return -1;
    }

    if (getkerninfo(KINFO_NDD, (char*) nddp, &size, 0) < 0) {
        perror("getkerninfo 2");
        free(nddp);
        return -1;
    }

    end = (void *)nddp + size;
    while ((void *)nddp < end) {
        if (!strcmp(nddp->ndd_alias, ifname) ||
                 !strcmp(nddp->ndd_name, ifname)) {
            bcopy(nddp->ndd_addr, buf, 6);
            free(nddp);
            return 6;
        } else {
            nddp++;
        }
    }

    free(nddp);
    return -1;
}

static int getMTU(JNIEnv *env, int sock, const char *ifname) {
    struct ifreq if2;
    memset((char *)&if2, 0, sizeof(if2));
    strncpy(if2.ifr_name, ifname, sizeof(if2.ifr_name) - 1);

    if (ioctl(sock, SIOCGIFMTU, (char *)&if2) < 0) {
        JNU_ThrowByNameWithMessageAndLastError
            (env, JNU_JAVANETPKG "SocketException", "ioctl(SIOCGIFMTU) failed");
        return -1;
    }

    return if2.ifr_mtu;
}

static int getFlags(int sock, const char *ifname, int *flags) {
    struct ifreq if2;
    memset((char *)&if2, 0, sizeof(if2));
    strncpy(if2.ifr_name, ifname, sizeof(if2.ifr_name) - 1);

    if (ioctl(sock, SIOCGIFFLAGS, (char *)&if2) < 0) {
        return -1;
    }

    if (sizeof(if2.ifr_flags) == sizeof(short)) {
        *flags = (if2.ifr_flags & 0xffff);
    } else {
        *flags = if2.ifr_flags;
    }
    return 0;
}

#endif /* _AIX */

/** BSD **/
#if defined(_ALLBSD_SOURCE)

/*
 * Opens a socket for further ioctl calls. Tries AF_INET socket first and
 * if it fails return AF_INET6 socket.
 */
static int openSocketWithFallback(JNIEnv *env, const char *ifname) {
    int sock;

    if ((sock = socket(AF_INET, SOCK_DGRAM, 0)) < 0) {
        if (errno == EPROTONOSUPPORT || errno == EAFNOSUPPORT) {
            if ((sock = socket(AF_INET6, SOCK_DGRAM, 0)) < 0) {
                JNU_ThrowByNameWithMessageAndLastError
                    (env, JNU_JAVANETPKG "SocketException", "IPV6 Socket creation failed");
                return -1;
            }
        } else { // errno is not NOSUPPORT
            JNU_ThrowByNameWithMessageAndLastError
                (env, JNU_JAVANETPKG "SocketException", "IPV4 Socket creation failed");
            return -1;
        }
    }

    return sock;
}

/*
 * Enumerates and returns all IPv4 interfaces on BSD.
 */
static netif *enumIPv4Interfaces(JNIEnv *env, int sock, netif *ifs) {
    struct ifaddrs *ifa, *origifa;

    if (getifaddrs(&origifa) != 0) {
        JNU_ThrowByNameWithMessageAndLastError
            (env, JNU_JAVANETPKG "SocketException", "getifaddrs() failed");
        return ifs;
    }

    for (ifa = origifa; ifa != NULL; ifa = ifa->ifa_next) {
        struct sockaddr *broadaddrP = NULL;

        // ignore non IPv4 addresses
        if (ifa->ifa_addr == NULL || ifa->ifa_addr->sa_family != AF_INET)
            continue;

        // set ifa_broadaddr, if there is one
        if ((ifa->ifa_flags & IFF_POINTOPOINT) == 0 &&
            ifa->ifa_flags & IFF_BROADCAST) {
            broadaddrP = ifa->ifa_dstaddr;
        }

        // add interface to the list
        ifs = addif(env, sock, ifa->ifa_name, ifs, ifa->ifa_addr,
                    broadaddrP, AF_INET,
                    translateIPv4AddressToPrefix((struct sockaddr_in *)
                                                 ifa->ifa_netmask));

        // if an exception occurred then free the list
        if ((*env)->ExceptionOccurred(env)) {
            freeifaddrs(origifa);
            freeif(ifs);
            return NULL;
        }
    }

    // free ifaddrs buffer
    freeifaddrs(origifa);
    return ifs;
}

/*
 * Enumerates and returns all IPv6 interfaces on BSD.
 */
static netif *enumIPv6Interfaces(JNIEnv *env, int sock, netif *ifs) {
    struct ifaddrs *ifa, *origifa;

    if (getifaddrs(&origifa) != 0) {
        JNU_ThrowByNameWithMessageAndLastError
            (env, JNU_JAVANETPKG "SocketException", "getifaddrs() failed");
        return ifs;
    }

    for (ifa = origifa; ifa != NULL; ifa = ifa->ifa_next) {
        // ignore non IPv6 addresses
        if (ifa->ifa_addr == NULL || ifa->ifa_addr->sa_family != AF_INET6)
            continue;

        // set scope ID to interface index
        ((struct sockaddr_in6 *)ifa->ifa_addr)->sin6_scope_id =
            getIndex(sock, ifa->ifa_name);

        // add interface to the list
        ifs = addif(env, sock, ifa->ifa_name, ifs, ifa->ifa_addr, NULL,
                    AF_INET6,
                    translateIPv6AddressToPrefix((struct sockaddr_in6 *)
                                                 ifa->ifa_netmask));

        // if an exception occurred then free the list
        if ((*env)->ExceptionOccurred(env)) {
            freeifaddrs(origifa);
            freeif(ifs);
            return NULL;
        }
    }

    // free ifaddrs buffer
    freeifaddrs(origifa);
    return ifs;
}

/*
 * Try to get the interface index.
 */
static int getIndex(int sock, const char *name) {
#if !defined(__FreeBSD__)
    int index = if_nametoindex(name);
    return (index == 0) ? -1 : index;
#else
    struct ifreq if2;
    memset((char *)&if2, 0, sizeof(if2));
    strncpy(if2.ifr_name, name, sizeof(if2.ifr_name) - 1);

    if (ioctl(sock, SIOCGIFINDEX, (char *)&if2) < 0) {
        return -1;
    }

    return if2.ifr_index;
#endif
}

/*
 * Gets the Hardware address (usually MAC address) for the named interface.
 * On return puts the data in buf, and returns the length, in byte, of the
 * MAC address. Returns -1 if there is no hardware address on that interface.
 */
static int getMacAddress
  (JNIEnv *env, const char *ifname, const struct in_addr *addr,
   unsigned char *buf)
{
    struct ifaddrs *ifa0, *ifa;
    struct sockaddr *saddr;
    int i;

    // grab the interface list
    if (!getifaddrs(&ifa0)) {
        // cycle through the interfaces
        for (i = 0, ifa = ifa0; ifa != NULL; ifa = ifa->ifa_next, i++) {
            saddr = ifa->ifa_addr;
            if (saddr != NULL) {
                // link layer contains the MAC address
                if (saddr->sa_family == AF_LINK && !strcmp(ifname, ifa->ifa_name)) {
                    struct sockaddr_dl *sadl = (struct sockaddr_dl *) saddr;
                    // check the address has the correct length
                    if (sadl->sdl_alen == ETHER_ADDR_LEN) {
                        memcpy(buf, (sadl->sdl_data + sadl->sdl_nlen), ETHER_ADDR_LEN);
                        freeifaddrs(ifa0);
                        return ETHER_ADDR_LEN;
                    }
                }
            }
        }
        freeifaddrs(ifa0);
    }

    return -1;
}

static int getMTU(JNIEnv *env, int sock, const char *ifname) {
    struct ifreq if2;
    memset((char *)&if2, 0, sizeof(if2));
    strncpy(if2.ifr_name, ifname, sizeof(if2.ifr_name) - 1);

    if (ioctl(sock, SIOCGIFMTU, (char *)&if2) < 0) {
        JNU_ThrowByNameWithMessageAndLastError
            (env, JNU_JAVANETPKG "SocketException", "ioctl(SIOCGIFMTU) failed");
        return -1;
    }

    return if2.ifr_mtu;
}

static int getFlags(int sock, const char *ifname, int *flags) {
    struct ifreq if2;
    memset((char *)&if2, 0, sizeof(if2));
    strncpy(if2.ifr_name, ifname, sizeof(if2.ifr_name) - 1);

    if (ioctl(sock, SIOCGIFFLAGS, (char *)&if2) < 0) {
        return -1;
    }

    if (sizeof(if2.ifr_flags) == sizeof(short)) {
        *flags = (if2.ifr_flags & 0xffff);
    } else {
        *flags = if2.ifr_flags;
    }
    return 0;
}
#endif /* _ALLBSD_SOURCE */
