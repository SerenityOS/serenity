/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "NetworkInterface.h"

#include "java_net_NetworkInterface.h"

/*
 * Windows implementation of the java.net.NetworkInterface native methods.
 * This module provides the implementations of getAll, getByName, getByIndex,
 * and getByAddress.
 */

extern int enumAddresses_win_ipaddrtable(JNIEnv *env, netif *netifP, netaddr **netaddrPP, MIB_IPADDRTABLE *tableP);
extern int enumAddresses_win(JNIEnv *env, netif *netifP, netaddr **netaddrPP);
extern int lookupIPAddrTable(JNIEnv *env, MIB_IPADDRTABLE **tablePP);
int getAddrsFromAdapter(IP_ADAPTER_ADDRESSES *ptr, netaddr **netaddrPP);

#ifdef DEBUG
void printnif (netif *nif) {
#ifdef _WIN64
        printf ("nif:0x%I64x name:%s\n", (UINT_PTR)nif, nif->name);
#else
        printf ("nif:0x%x name:%s\n", (UINT_PTR)nif, nif->name);
#endif
        if (nif->dNameIsUnicode) {
            printf ("dName:%S index:%d ", (unsigned short *)nif->displayName,
                nif->index);
        } else {
            printf ("dName:%s index:%d ", nif->displayName, nif->index);
        }
        printf ("naddrs:%d\n", nif->naddrs);
}

void printnifs (netif *netifPP, char *str) {
    netif *nif;
    printf ("%s\n", str);
    for (nif=netifPP; nif!=NULL; nif=nif->next) {
        printnif (nif);
    }
    printf("-----------------\n");
}

#endif

const ULONG BUFF_SIZE = 15360;
const int MAX_TRIES = 3;

/*
 * return an array of IP_ADAPTER_ADDRESSES containing one element
 * for each adapter on the system. Returned in *adapters.
 * Buffer is malloc'd and must be freed (unless error returned)
 */
int getAdapters (JNIEnv *env, int flags, IP_ADAPTER_ADDRESSES **adapters) {
    DWORD ret;
    IP_ADAPTER_ADDRESSES *adapterInfo;
    ULONG len;
    int try;

    adapterInfo = (IP_ADAPTER_ADDRESSES *) malloc(BUFF_SIZE);
    if (adapterInfo == NULL) {
        JNU_ThrowByName(env, "java/lang/OutOfMemoryError",
            "Native heap allocation failure");
        return -1;
    }

    len = BUFF_SIZE;
    ret = GetAdaptersAddresses(AF_UNSPEC, flags, NULL, adapterInfo, &len);

    for (try = 0; ret == ERROR_BUFFER_OVERFLOW && try < MAX_TRIES; ++try) {
        IP_ADAPTER_ADDRESSES * newAdapterInfo = NULL;
        if (len < (ULONG_MAX - BUFF_SIZE)) {
            len += BUFF_SIZE;
        }
        newAdapterInfo =
            (IP_ADAPTER_ADDRESSES *) realloc (adapterInfo, len);
        if (newAdapterInfo == NULL) {
            free(adapterInfo);
            JNU_ThrowByName(env, "java/lang/OutOfMemoryError",
                "Native heap allocation failure");
            return -1;
        }

        adapterInfo = newAdapterInfo;

        ret = GetAdaptersAddresses(AF_UNSPEC, flags, NULL, adapterInfo, &len);
    }

    if (ret != ERROR_SUCCESS) {
        free (adapterInfo);
        if (ret == ERROR_INSUFFICIENT_BUFFER) {
            JNU_ThrowByName(env, "java/lang/Error",
                "IP Helper Library GetAdaptersAddresses function failed "
                "with ERROR_INSUFFICIENT_BUFFER");
        } else if (ret == ERROR_ADDRESS_NOT_ASSOCIATED ) {
            JNU_ThrowByName(env, "java/lang/Error",
                "IP Helper Library GetAdaptersAddresses function failed "
                "with ERROR_ADDRESS_NOT_ASSOCIATED");
        } else {
            char error_msg_buf[100];
            int _sr;
            _sr = _snprintf_s(error_msg_buf, sizeof(error_msg_buf),
                _TRUNCATE, "IP Helper Library GetAdaptersAddresses "
                            "function failed with error == %d", ret);
            if (_sr != -1) {
                JNU_ThrowByName(env, "java/lang/Error", error_msg_buf);
            } else {
                JNU_ThrowByName(env, "java/lang/Error",
                    "IP Helper Library GetAdaptersAddresses function failure");
            }
        }
        return -1;
    }
    *adapters = adapterInfo;
    return ERROR_SUCCESS;
}

/*
 * return an array of IP_ADAPTER_ADDRESSES containing one element
 * for each adapter on the system. Returned in *adapters.
 * Buffer is malloc'd and must be freed (unless error returned)
 */
IP_ADAPTER_ADDRESSES *getAdapter (JNIEnv *env,  jint index) {
    DWORD flags, val;
    IP_ADAPTER_ADDRESSES *adapterInfo, *ptr, *ret;
    ULONG len;
    int try;
    adapterInfo = (IP_ADAPTER_ADDRESSES *) malloc(BUFF_SIZE);
    if (adapterInfo == NULL) {
        JNU_ThrowByName(env, "java/lang/OutOfMemoryError",
            "Native heap allocation failure");
        return NULL;
    }
    len = BUFF_SIZE;
    flags = GAA_FLAG_SKIP_DNS_SERVER;
    flags |= GAA_FLAG_SKIP_MULTICAST;
    flags |= GAA_FLAG_INCLUDE_PREFIX;
    val = GetAdaptersAddresses(AF_UNSPEC, flags, NULL, adapterInfo, &len);
    for (try = 0; val == ERROR_BUFFER_OVERFLOW && try < MAX_TRIES; ++try) {
        IP_ADAPTER_ADDRESSES * newAdapterInfo = NULL;
        if (len < (ULONG_MAX - BUFF_SIZE)) {
            len += BUFF_SIZE;
        }
        newAdapterInfo =
                (IP_ADAPTER_ADDRESSES *) realloc (adapterInfo, len);
        if (newAdapterInfo == NULL) {
            free(adapterInfo);
            JNU_ThrowByName(env, "java/lang/OutOfMemoryError",
                "Native heap allocation failure");
            return NULL;
        }

        adapterInfo = newAdapterInfo;

        val = GetAdaptersAddresses(AF_UNSPEC, flags, NULL, adapterInfo, &len);
    }

    if (val != ERROR_SUCCESS) {
        free (adapterInfo);
        if (val == ERROR_INSUFFICIENT_BUFFER) {
            JNU_ThrowByName(env, "java/lang/Error",
                "IP Helper Library GetAdaptersAddresses function failed "
                "with ERROR_INSUFFICIENT_BUFFER");
        } else if (val == ERROR_ADDRESS_NOT_ASSOCIATED ) {
            JNU_ThrowByName(env, "java/lang/Error",
                "IP Helper Library GetAdaptersAddresses function failed "
                "with ERROR_ADDRESS_NOT_ASSOCIATED");
        } else {
            char error_msg_buf[100];
            int _sr;
            _sr = _snprintf_s(error_msg_buf, sizeof(error_msg_buf),
                _TRUNCATE, "IP Helper Library GetAdaptersAddresses function failed "
                           "with error == %d", val);
            if (_sr != -1) {
                JNU_ThrowByName(env, "java/lang/Error", error_msg_buf);
            } else {
                JNU_ThrowByName(env, "java/lang/Error",
                    "IP Helper Library GetAdaptersAddresses function failure");
            }
        }
        return NULL;
    }

    ptr = adapterInfo;
    ret = NULL;
    while (ptr != NULL) {
      // in theory the IPv4 index and the IPv6 index can be the same
      // where an interface is enabled for v4 and v6
      // IfIndex == 0 IPv4 not available on this interface
      // Ipv6IfIndex == 0 IPv6 not available on this interface
      if (((ptr->IfIndex != 0)&&(ptr->IfIndex == index)) ||
          ((ptr->Ipv6IfIndex !=0) && (ptr->Ipv6IfIndex == index))) {
        ret = (IP_ADAPTER_ADDRESSES *) malloc(sizeof(IP_ADAPTER_ADDRESSES));
        if (ret == NULL) {
            free(adapterInfo);
            JNU_ThrowByName(env, "java/lang/OutOfMemoryError",
                "Native heap allocation failure");
            return NULL;
        }

        //copy the memory and break out of the while loop.
        memcpy(ret, ptr, sizeof(IP_ADAPTER_ADDRESSES));
        break;

      }
      ptr=ptr->Next;
    }
    free(adapterInfo);
    return ret;
}

static int ipinflen = 2048;

/*
 */
int getAllInterfacesAndAddresses (JNIEnv *env, netif **netifPP)
{
    DWORD ret, flags;
    MIB_IPADDRTABLE *tableP;
    IP_ADAPTER_ADDRESSES *ptr, *adapters=NULL;
    ULONG len=ipinflen, count=0;
    netif *nif=NULL, *dup_nif, *last=NULL, *loopif=NULL, *curr;
    int tun=0, net=0;

    *netifPP = NULL;
   /*
    * Get the IPv4 interfaces. This information is the same
    * as what previous JDK versions would return.
    */

    ret = enumInterfaces(env, netifPP);
    if (ret == -1) {
        return -1;
    } else if( ret == -2){
        if ((*env)->ExceptionCheck(env)) {
            (*env)->ExceptionClear(env);
        }
    } else {
        count = ret;
    }

    /* locate the loopback (and the last) interface */
    for (nif=*netifPP, last=nif; nif!=NULL; nif=nif->next) {
        if (nif->ifType == MIB_IF_TYPE_LOOPBACK) {
            loopif = nif;
        }
        last = nif;
    }

    // Retrieve IPv4 addresses with the IP Helper API
    curr = *netifPP;
    ret = lookupIPAddrTable(env, &tableP);
    if (ret < 0) {
      return -1;
    }
    while (curr != NULL) {
        netaddr *netaddrP;
        ret = enumAddresses_win_ipaddrtable(env, curr, &netaddrP, tableP);
        if (ret == -1) {
            free(tableP);
            return -1;
        } else if (ret == -2) {
            if ((*env)->ExceptionCheck(env)) {
                (*env)->ExceptionClear(env);
            }
            break;
        } else{
            curr->addrs = netaddrP;
            curr->naddrs += ret;
            curr = curr->next;
        }
    }
    free(tableP);

    flags = GAA_FLAG_SKIP_DNS_SERVER;
    flags |= GAA_FLAG_SKIP_MULTICAST;
    flags |= GAA_FLAG_INCLUDE_PREFIX;
    ret = getAdapters (env, flags, &adapters);
    if (ret != ERROR_SUCCESS) {
        goto err;
    }

    /* Now get the IPv6 information. This includes:
     *  (a)  IPv6 information associated with interfaces already found
     *  (b)  IPv6 information for IPv6 only interfaces (probably tunnels)
     *
     * For compatibility with previous releases we use the naming
     * information gotten from enumInterfaces() for (a) entries
     * However, the index numbers are taken from the new API.
     *
     * The procedure is to go through the list of adapters returned
     * by the new API looking for entries that correspond to IPv4 interfaces
     * already found.
     */

    ptr = adapters;
    while (ptr != NULL) {
        int c;
        netif *nif0;
        if (ptr->IfType == IF_TYPE_SOFTWARE_LOOPBACK && (loopif != NULL)) {
            c = getAddrsFromAdapter(ptr, &loopif->addrs);
            if (c == -1) {
                goto err;
            }
            loopif->naddrs += c;
            loopif->ipv6Index = ptr->Ipv6IfIndex;
        } else {
            int index = ptr->IfIndex;
            if (index != 0) {
                /* This entry is associated with an IPv4 interface */
                for (nif=*netifPP; nif!=NULL; nif=nif->next) {
                    if (nif->index == index) {
                        /* found the interface entry
                         * set the index to the IPv6 index and add the
                         * IPv6 addresses
                         */
                        nif->ipv6Index = ptr->Ipv6IfIndex;
                        c = getAddrsFromAdapter(ptr, &nif->addrs);
                        nif->naddrs += c;
                        break;
                    }
                }
            } else {
                /* This entry is IPv6 only */
                char newname [128];
                int c;

                /* Windows allocates duplicate adapter entries
                 * for tunnel interfaces when there are multiple
                 * physical adapters. Need to check
                 * if this is a duplicate (ipv6Index is the same)
                 */
                dup_nif = 0;
                for (nif0=*netifPP; nif0!=NULL; nif0=nif0->next) {
                    if (nif0->hasIpv6Address &&
                                ptr->Ipv6IfIndex == nif0->ipv6Index) {
                        dup_nif = nif0;
                        break;
                    }
                }
                if (dup_nif == 0) {
                    /* new interface */
                        nif = (netif *) calloc (1, sizeof(netif));
                        if (nif == 0) {
                            goto err;
                        }
                        if (ptr->IfType == IF_TYPE_TUNNEL) {
                                sprintf (newname, "tun%d", tun);
                                tun ++;
                        } else {
                                sprintf (newname, "net%d", net);
                                net ++;
                        }
                        nif->name = malloc (strlen(newname)+1);
                        nif->displayName = malloc (wcslen(ptr->FriendlyName)*2+2);
                        if (nif->name == 0 || nif->displayName == 0) {
                                goto err;
                        }
                        strcpy (nif->name, newname);
                        wcscpy ((PWCHAR)nif->displayName, ptr->FriendlyName);
                        nif->dNameIsUnicode = TRUE;

                        // the java.net.NetworkInterface abstraction only has index
                        // so the Ipv6IfIndex needs to map onto index
                        nif->index = ptr->Ipv6IfIndex;
                        nif->ipv6Index = ptr->Ipv6IfIndex;
                        nif->hasIpv6Address = TRUE;

                        last->next = nif;
                        last = nif;
                        count++;
                        c = getAddrsFromAdapter(ptr, &nif->addrs);
                        if (c == -1) {
                                goto err;
                        }
                        nif->naddrs += c;
                 } else {
                        /* add the addresses from this adapter to the
                         * original (dup_nif)
                         */
                        c = getAddrsFromAdapter(ptr, &dup_nif->addrs);
                        if (c == -1) {
                                goto err;
                        }
                        dup_nif->naddrs += c;
                }
            }
        }
        ptr=ptr->Next;
    }

    free (adapters);
    return count;

err:
    if (*netifPP) {
        free_netif (*netifPP);
    }
    if (adapters) {
        free (adapters);
    }
    return -1;
}

/* If *netaddrPP is null, then the addresses are allocated and the beginning
 * of the allocated chain is returned in *netaddrPP.
 * If *netaddrPP is not null, then the addresses allocated here are appended
 * to the existing chain.
 *
 * Returns count of addresses or -1 on error.
 */

static int getAddrsFromAdapter(IP_ADAPTER_ADDRESSES *ptr, netaddr **netaddrPP) {
        LPSOCKADDR sock;
        int        count = 0;
        netaddr    *curr, *start = NULL, *prev = NULL;
        PIP_ADAPTER_UNICAST_ADDRESS uni_addr;
        PIP_ADAPTER_ANYCAST_ADDRESS any_addr;
        PIP_ADAPTER_PREFIX prefix;

        /* If chain passed in, find end */
        if (*netaddrPP != NULL) {
            for (start=*netaddrPP; start->next!=NULL; start=start->next)
                ;

            prev=start;
        }

        prefix = ptr->FirstPrefix;
        /* Unicast */
        uni_addr = ptr->FirstUnicastAddress;
        while (uni_addr != NULL) {
        /* address is only usable if dad state is preferred or deprecated */
                if (uni_addr->DadState == IpDadStateDeprecated ||
                                uni_addr->DadState == IpDadStatePreferred) {
                        sock = uni_addr->Address.lpSockaddr;

                        // IPv4 addresses already retrieved with enumAddresses_win
                        if (sock->sa_family == AF_INET) {
                                uni_addr = uni_addr->Next;
                                continue;
                        }

            curr = (netaddr *)calloc (1, sizeof (netaddr));

            if (curr == NULL)
                goto freeAllocatedMemory;

            if (start == NULL)
                start = curr;

            if (prev != NULL)
               prev->next = curr;

            prev = curr;
            SOCKETADDRESS_COPY (&curr->addr, sock);
            if (prefix != NULL) {
              curr->mask = (short)prefix->PrefixLength;
              prefix = prefix->Next;
            }
            count ++;
        }
        uni_addr = uni_addr->Next;
    }
    /* Anycast */
    any_addr = ptr->FirstAnycastAddress;
    while (any_addr != NULL) {
        curr = (netaddr *)calloc (1, sizeof (netaddr));

        if (curr == NULL)
            goto freeAllocatedMemory;

        if (start == NULL)
            start = curr;

        if (prev != NULL)
            prev->next = curr;

        prev = curr;
        sock = any_addr->Address.lpSockaddr;
        SOCKETADDRESS_COPY (&curr->addr, sock);
        count ++;
        any_addr = any_addr->Next;
    }
    if (*netaddrPP == NULL) {
        *netaddrPP = start;
    }
    return count;

freeAllocatedMemory:

    if (*netaddrPP != NULL) {
        //N.B. the variable "start" cannot be NULL at this point because we started with an
        //existing list.
        curr=start->next;
        start->next = NULL;
        start = curr;
    }
    // otherwise, "start" points to the beginning of an incomplete list that we must deallocate.

    while (start != NULL) {
        curr = start->next;
        free(start);
        start = curr;
    }

    return -1;
}

/*
 * Create a NetworkInterface object, populate the name and index, and
 * populate the InetAddress array based on the IP addresses for this
 * interface.
 */
static jobject createNetworkInterfaceXP(JNIEnv *env, netif *ifs)
{
    jobject netifObj;
    jobject name, displayName;
    jobjectArray addrArr, bindsArr, childArr;
    netaddr *addrs;
    jint addr_index;
    int netaddrCount = ifs->naddrs;
    netaddr *netaddrP = ifs->addrs;
    netaddr *netaddrPToFree = NULL;
    jint bind_index;

    /*
     * Create a NetworkInterface object and populate it
     */
    netifObj = (*env)->NewObject(env, ni_class, ni_ctor);
    if (netifObj == NULL) {
        return NULL;
    }
    name = (*env)->NewStringUTF(env, ifs->name);
    if (name == NULL) {
        return NULL;
    }
    if (ifs->dNameIsUnicode) {
        displayName = (*env)->NewString(env, (PWCHAR)ifs->displayName,
                                        (jsize)wcslen ((PWCHAR)ifs->displayName));
    } else {
        displayName = (*env)->NewStringUTF(env, ifs->displayName);
    }
    if (displayName == NULL) {
        return NULL;
    }
    (*env)->SetObjectField(env, netifObj, ni_nameID, name);
    (*env)->SetObjectField(env, netifObj, ni_displayNameID, displayName);
    (*env)->SetIntField(env, netifObj, ni_indexID, ifs->index);
    /*
     * Get the IP addresses for this interface if necessary
     * Note that 0 is a valid number of addresses.
     */
    if (netaddrCount < 0) {
        netaddrCount = enumAddresses_win(env, ifs, &netaddrPToFree);
        if (netaddrCount == -1) {
            return NULL;
        }
        if (netaddrCount == -2) {
            // Clear the exception and continue.
            if ((*env)->ExceptionCheck(env)) {
                (*env)->ExceptionClear(env);
            }
        }
        netaddrP = netaddrPToFree;
    }

    addrArr = (*env)->NewObjectArray(env, netaddrCount, ia_class, NULL);
    if (addrArr == NULL) {
        free_netaddr(netaddrPToFree);
        return NULL;
    }

    bindsArr = (*env)->NewObjectArray(env, netaddrCount, ni_ibcls, NULL);
    if (bindsArr == NULL) {
        free_netaddr(netaddrPToFree);
        return NULL;
    }

    addrs = netaddrP;
    addr_index = 0;
    bind_index = 0;
    while (addrs != NULL) {
        jobject iaObj, ia2Obj;
        jobject ibObj = NULL;
        if (addrs->addr.sa.sa_family == AF_INET) {
            iaObj = (*env)->NewObject(env, ia4_class, ia4_ctrID);
            if (iaObj == NULL) {
                free_netaddr(netaddrPToFree);
                return NULL;
            }
            /* default ctor will set family to AF_INET */

            setInetAddress_addr(env, iaObj, ntohl(addrs->addr.sa4.sin_addr.s_addr));
            if ((*env)->ExceptionCheck(env)) {
                free_netaddr(netaddrPToFree);
                return NULL;
            }
            ibObj = (*env)->NewObject(env, ni_ibcls, ni_ibctrID);
            if (ibObj == NULL) {
                free_netaddr(netaddrPToFree);
                return NULL;
            }
            (*env)->SetObjectField(env, ibObj, ni_ibaddressID, iaObj);
            ia2Obj = (*env)->NewObject(env, ia4_class, ia4_ctrID);
            if (ia2Obj == NULL) {
                free_netaddr(netaddrPToFree);
                return NULL;
            }
            setInetAddress_addr(env, ia2Obj, ntohl(addrs->brdcast.sa4.sin_addr.s_addr));
            if ((*env)->ExceptionCheck(env)) {
                free_netaddr(netaddrPToFree);
                return NULL;
            }
            (*env)->SetObjectField(env, ibObj, ni_ibbroadcastID, ia2Obj);
            (*env)->SetShortField(env, ibObj, ni_ibmaskID, addrs->mask);
            (*env)->SetObjectArrayElement(env, bindsArr, bind_index++, ibObj);
        } else /* AF_INET6 */ {
            int scope;
            jboolean ret;
            iaObj = (*env)->NewObject(env, ia6_class, ia6_ctrID);
            if (iaObj == NULL) {
                free_netaddr(netaddrPToFree);
                return NULL;
            }
            ret = setInet6Address_ipaddress(env, iaObj, (jbyte *)&(addrs->addr.sa6.sin6_addr.s6_addr));
            if (ret == JNI_FALSE) {
                free_netaddr(netaddrPToFree);
                return NULL;
            }
            scope = addrs->addr.sa6.sin6_scope_id;
            if (scope != 0) { /* zero is default value, no need to set */
                setInet6Address_scopeid(env, iaObj, scope);
                setInet6Address_scopeifname(env, iaObj, netifObj);
            }
            ibObj = (*env)->NewObject(env, ni_ibcls, ni_ibctrID);
            if (ibObj == NULL) {
                free_netaddr(netaddrPToFree);
                return NULL;
            }
            (*env)->SetObjectField(env, ibObj, ni_ibaddressID, iaObj);
            (*env)->SetShortField(env, ibObj, ni_ibmaskID, addrs->mask);
            (*env)->SetObjectArrayElement(env, bindsArr, bind_index++, ibObj);
        }
        (*env)->SetObjectArrayElement(env, addrArr, addr_index, iaObj);
        addrs = addrs->next;
        addr_index++;
    }
    (*env)->SetObjectField(env, netifObj, ni_addrsID, addrArr);
    (*env)->SetObjectField(env, netifObj, ni_bindsID, bindsArr);

    free_netaddr(netaddrPToFree);

    /*
     * Windows doesn't have virtual interfaces, so child array
     * is always empty.
     */
    childArr = (*env)->NewObjectArray(env, 0, ni_class, NULL);
    if (childArr == NULL) {
      return NULL;
    }
    (*env)->SetObjectField(env, netifObj, ni_childsID, childArr);

    /* return the NetworkInterface */
    return netifObj;
}

JNIEXPORT jobject JNICALL Java_java_net_NetworkInterface_getByName0_XP
    (JNIEnv *env, jclass cls, jstring name)
{
    netif *ifList, *curr;
    jboolean isCopy;
    const char *name_utf;
    jobject netifObj = NULL;

    if (getAllInterfacesAndAddresses (env, &ifList) < 0) {
        return NULL;
    }

    /* get the name as a C string */
    name_utf = (*env)->GetStringUTFChars(env, name, &isCopy);

    /* Search by name */
    curr = ifList;
    while (curr != NULL) {
        if (strcmp(name_utf, curr->name) == 0) {
            break;
        }
        curr = curr->next;
    }

    /* if found create a NetworkInterface */
    if (curr != NULL) {
        netifObj = createNetworkInterfaceXP(env, curr);
    }

    /* release the UTF string */
    (*env)->ReleaseStringUTFChars(env, name, name_utf);

    /* release the interface list */
    free_netif(ifList);

    return netifObj;
}

/*
 * Class:     NetworkInterface
 * Method:    getByIndex0_XP
 * Signature: (I)LNetworkInterface;
 */
JNIEXPORT jobject JNICALL Java_java_net_NetworkInterface_getByIndex0_XP
  (JNIEnv *env, jclass cls, jint index)
{
    netif *ifList, *curr;
    jobject netifObj = NULL;

    if (getAllInterfacesAndAddresses (env, &ifList) < 0) {
        return NULL;
    }

    /* search by index */
    curr = ifList;
    while (curr != NULL) {
        if (index == curr->index) {
            break;
        }
        curr = curr->next;
    }

    /* if found create a NetworkInterface */
    if (curr != NULL) {
        netifObj = createNetworkInterfaceXP(env, curr);
    }

    /* release the interface list */
    free_netif(ifList);

    return netifObj;
}

/*
 * Class:     java_net_NetworkInterface
 * Method:    getByInetAddress0
 * Signature: (Ljava/net/InetAddress;)Ljava/net/NetworkInterface;
 */
JNIEXPORT jobject JNICALL Java_java_net_NetworkInterface_getByInetAddress0_XP
    (JNIEnv *env, jclass cls, jobject iaObj)
{
    netif *ifList, *curr;
    jobject netifObj = NULL;

    /* get the list of interfaces */
    if (getAllInterfacesAndAddresses (env, &ifList) < 0) {
        return NULL;
    }

    /*
     * Enumerate the addresses on each interface until we find a
     * matching address.
     */
    curr = ifList;
    while (curr != NULL) {
        netaddr *addrList = curr->addrs;
        netaddr *addrP;

        /* iterate through each address */
        addrP = addrList;

        while (addrP != NULL) {
            if (NET_SockaddrEqualsInetAddress(env,
                                (struct sockaddr*)&addrP->addr, iaObj)) {
                break;
            }
            addrP = addrP->next;
        }

        /*
         * Address matched so create NetworkInterface for this interface
         * and address list.
         */
        if (addrP != NULL) {
            netifObj = createNetworkInterfaceXP(env, curr);
            break;
        }

        /* on next interface */
        curr = curr->next;
    }

    /* release the interface list */
    free_netif(ifList);

    return netifObj;
}

/*
 * Class:     java_net_NetworkInterface
 * Method:    getAll
 * Signature: ()[Ljava/net/NetworkInterface;
 */
JNIEXPORT jobjectArray JNICALL Java_java_net_NetworkInterface_getAll_XP
    (JNIEnv *env, jclass cls)
{
    int count;
    netif *ifList = NULL, *curr;
    jobjectArray netIFArr;
    jint arr_index;

    /*
     * Get list of interfaces
     */
    count = getAllInterfacesAndAddresses (env, &ifList);
    if (count < 0) {
        return NULL;
    }

    /* allocate a NetworkInterface array */
    netIFArr = (*env)->NewObjectArray(env, count, cls, NULL);
    if (netIFArr == NULL) {
        free_netif(ifList);
        return NULL;
    }

    /*
     * Iterate through the interfaces, create a NetworkInterface instance
     * for each array element and populate the object.
     */
    curr = ifList;
    arr_index = 0;
    while (curr != NULL) {
        jobject netifObj;

        netifObj = createNetworkInterfaceXP(env, curr);
        if (netifObj == NULL) {
            free_netif(ifList);
            return NULL;
        }

        /* put the NetworkInterface into the array */
        (*env)->SetObjectArrayElement(env, netIFArr, arr_index++, netifObj);
        curr = curr->next;
    }

    /* release the interface list */
    free_netif(ifList);

    return netIFArr;
}

/*
 * Class:     java_net_NetworkInterface
 * Method:    supportsMulticast0
 * Signature: (Ljava/lang/String;I)Z
 */
JNIEXPORT jboolean JNICALL Java_java_net_NetworkInterface_supportsMulticast0_XP
    (JNIEnv *env, jclass cls, jstring name, jint index) {
      IP_ADAPTER_ADDRESSES *ptr;
      jboolean val = JNI_TRUE;

      ptr = getAdapter(env, index);
      if (ptr != NULL) {
        val = ptr->Flags & IP_ADAPTER_NO_MULTICAST ? JNI_FALSE : JNI_TRUE;
        free(ptr);
      }
      return val;
}

/*
 * Class:     java_net_NetworkInterface
 * Method:    isUp0
 * Signature: (Ljava/lang/String;I)Z
 */
JNIEXPORT jboolean JNICALL Java_java_net_NetworkInterface_isUp0_XP
    (JNIEnv *env, jclass cls, jstring name, jint index) {
      IP_ADAPTER_ADDRESSES *ptr;
      jboolean val = JNI_FALSE;

      ptr = getAdapter(env, index);
      if (ptr != NULL) {
        val = ptr->OperStatus == IfOperStatusUp ? JNI_TRUE : JNI_FALSE;
        free(ptr);
      }
      return val;
}

/*
 * Class:     java_net_NetworkInterface
 * Method:    getMacAddr0
 * Signature: (Ljava/lang/String;I)Z
 */
JNIEXPORT jbyteArray JNICALL Java_java_net_NetworkInterface_getMacAddr0_XP
    (JNIEnv *env, jclass cls, jstring name, jint index) {
      IP_ADAPTER_ADDRESSES *ptr;
      jbyteArray ret = NULL;
      int len;

      ptr = getAdapter(env, index);
      if (ptr != NULL) {
        len = ptr->PhysicalAddressLength;
        if (len > 0) {
          ret = (*env)->NewByteArray(env, len);
          if (!IS_NULL(ret)) {
            (*env)->SetByteArrayRegion(env, ret, 0, len,
                                       (jbyte*) ptr->PhysicalAddress);
          }
        }
        free(ptr);
      }
      return ret;
}

/*
 * Class:       java_net_NetworkInterface
 * Method:      getMTU0
 * Signature:   ([bLjava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_java_net_NetworkInterface_getMTU0_XP
    (JNIEnv *env, jclass cls, jstring name, jint index) {
      IP_ADAPTER_ADDRESSES *ptr;
      jint ret = -1;

      ptr = getAdapter(env, index);
      if (ptr != NULL) {
        ret = ptr->Mtu;
        free(ptr);
      }
      return ret;
}

/*
 * Class:     java_net_NetworkInterface
 * Method:    isLoopback0
 * Signature: (Ljava/lang/String;I)Z
 */
JNIEXPORT jboolean JNICALL Java_java_net_NetworkInterface_isLoopback0_XP
    (JNIEnv *env, jclass cls, jstring name, jint index) {
      IP_ADAPTER_ADDRESSES *ptr;
      jboolean val = JNI_FALSE;

      ptr = getAdapter(env, index);
      if (ptr != NULL) {
        val = ptr->IfType == IF_TYPE_SOFTWARE_LOOPBACK ? JNI_TRUE : JNI_FALSE;
        free(ptr);
      }
      return val;
}

/*
 * Class:     java_net_NetworkInterface
 * Method:    isP2P0
 * Signature: (Ljava/lang/String;I)Z
 */
JNIEXPORT jboolean JNICALL Java_java_net_NetworkInterface_isP2P0_XP
    (JNIEnv *env, jclass cls, jstring name, jint index) {
      IP_ADAPTER_ADDRESSES *ptr;
      jboolean val = JNI_FALSE;

      ptr = getAdapter(env, index);
      if (ptr != NULL) {
        if (ptr->IfType == IF_TYPE_PPP || ptr->IfType == IF_TYPE_SLIP ||
           ptr->IfType == IF_TYPE_TUNNEL) {
          val = JNI_TRUE;
        }
        free(ptr);
      }
      return val;
}
