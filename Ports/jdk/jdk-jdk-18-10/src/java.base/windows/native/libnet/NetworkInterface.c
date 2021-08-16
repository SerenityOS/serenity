/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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

#include "java_net_InetAddress.h"
#include "java_net_NetworkInterface.h"

/*
 * Windows implementation of the java.net.NetworkInterface native methods.
 * This module provides the implementations of getAll, getByName, getByIndex,
 * and getByAddress.
 *
 * Interfaces and addresses are enumerated using the IP helper routines
 * GetIfTable, GetIfAddrTable resp. These routines are available on Windows
 * 98, NT SP+4, 2000, and XP. They are also available on Windows 95 if
 * IE is upgraded to 5.x.
 *
 * Windows does not have any standard for device names so we are forced
 * to use our own convention which is based on the normal Unix naming
 * convention ("lo" for the loopback, eth0, eth1, .. for ethernet devices,
 * tr0, tr1, .. for token ring, and so on). This convention gives us
 * consistency across multiple Windows editions and also consistency with
 * Solaris/Linux device names. Note that we always enumerate in index
 * order and this ensures consistent device number across invocations.
 */

/* various JNI ids */

jclass ni_class;            /* NetworkInterface */

jmethodID ni_ctor;          /* NetworkInterface() */

jfieldID ni_indexID;        /* NetworkInterface.index */
jfieldID ni_addrsID;        /* NetworkInterface.addrs */
jfieldID ni_bindsID;        /* NetworkInterface.bindings */
jfieldID ni_nameID;         /* NetworkInterface.name */
jfieldID ni_displayNameID;  /* NetworkInterface.displayName */
jfieldID ni_childsID;       /* NetworkInterface.childs */

jclass ni_ibcls;            /* InterfaceAddress */
jmethodID ni_ibctrID;       /* InterfaceAddress() */
jfieldID ni_ibaddressID;        /* InterfaceAddress.address */
jfieldID ni_ibbroadcastID;      /* InterfaceAddress.broadcast */
jfieldID ni_ibmaskID;           /* InterfaceAddress.maskLength */

/*
 * Support routines to free netif and netaddr lists
 */
void free_netif(netif *netifP) {
    netif *curr = netifP;
    while (curr != NULL) {
        if (curr->name != NULL)
            free(curr->name);
        if (curr->displayName != NULL)
            free(curr->displayName);
        if (curr->addrs != NULL)
            free_netaddr (curr->addrs);
        netifP = netifP->next;
        free(curr);
        curr = netifP;
    }
}

void free_netaddr(netaddr *netaddrP) {
    netaddr *curr = netaddrP;
    while (curr != NULL) {
        netaddrP = netaddrP->next;
        free(curr);
        curr = netaddrP;
    }
}

/*
 * Returns the interface structure from the table with the matching index.
 */
MIB_IFROW *getIF(jint index) {
    MIB_IFTABLE *tableP;
    MIB_IFROW *ifrowP, *ret = NULL;
    ULONG size;
    DWORD i, count;
    jint ifindex;

    /*
     * Ask the IP Helper library to enumerate the adapters
     */
    size = sizeof(MIB_IFTABLE);
    tableP = (MIB_IFTABLE *)malloc(size);
    if(tableP == NULL)
        return NULL;

    count = GetIfTable(tableP, &size, TRUE);
    if (count == ERROR_INSUFFICIENT_BUFFER || count == ERROR_BUFFER_OVERFLOW) {
        MIB_IFTABLE* newTableP =  (MIB_IFTABLE *)realloc(tableP, size);
        if (newTableP == NULL) {
            free(tableP);
            return NULL;
        }
        tableP = newTableP;

        count = GetIfTable(tableP, &size, TRUE);
    }

    if (count != NO_ERROR) {
        free(tableP);
        return NULL;
    }

    {
    ifrowP = tableP->table;
    for (i=0; i<tableP->dwNumEntries; i++) {
    /*
     * Warning: the real index is obtained by GetFriendlyIfIndex()
    */
        ifindex = GetFriendlyIfIndex(ifrowP->dwIndex);
        if (ifindex == index) {
          /*
           * Create a copy of the entry so that we can free the table.
           */
            ret = (MIB_IFROW *) malloc(sizeof(MIB_IFROW));
            if (ret == NULL) {
                free(tableP);
                return NULL;
            }
            memcpy(ret, ifrowP, sizeof(MIB_IFROW));
            break;
        }

        /* onto the next interface */
        ifrowP++;
      }
      free(tableP);
    }
    return ret;
}

/*
 * Enumerate network interfaces using IP Helper Library routine GetIfTable.
 * We use GetIfTable rather than other IP helper routines because it's
 * available on 98 & NT SP4+.
 *
 * Returns the number of interfaces found or -1 if error. If no error
 * occurs then netifPP be returned as list of netif structures or NULL
 * if no interfaces are found.
 */
int enumInterfaces(JNIEnv *env, netif **netifPP)
{
    MIB_IFTABLE *tableP;
    MIB_IFROW *ifrowP;
    ULONG size;
    DWORD ret;
    int count;
    netif *netifP;
    DWORD i;
    int lo=0, eth=0, tr=0, fddi=0, ppp=0, sl=0, wlan=0, net=0, wlen=0;

    /*
     * Ask the IP Helper library to enumerate the adapters
     */
    size = sizeof(MIB_IFTABLE);
    tableP = (MIB_IFTABLE *)malloc(size);
    if (tableP == NULL) {
        JNU_ThrowOutOfMemoryError(env, "Native heap allocation failure");
        return -1;
    }

    ret = GetIfTable(tableP, &size, TRUE);
    if (ret == ERROR_INSUFFICIENT_BUFFER || ret == ERROR_BUFFER_OVERFLOW) {
        MIB_IFTABLE * newTableP = (MIB_IFTABLE *)realloc(tableP, size);
        if (newTableP == NULL) {
            free(tableP);
            JNU_ThrowOutOfMemoryError(env, "Native heap allocation failure");
            return -1;
        }
        tableP = newTableP;
        ret = GetIfTable(tableP, &size, TRUE);
    }

    if (ret != NO_ERROR) {
        free(tableP);

        JNU_ThrowByName(env, "java/lang/Error",
                "IP Helper Library GetIfTable function failed");
        // this different error code is to handle the case when we call
        // GetIpAddrTable in pure IPv6 environment
        return -2;
    }

    /*
     * Iterate through the list of adapters
     */
    count = 0;
    netifP = NULL;

    ifrowP = tableP->table;
    for (i=0; i<tableP->dwNumEntries; i++) {
        char dev_name[8];
        netif *curr;

        /*
         * Generate a name for the device as Windows doesn't have any
         * real concept of a device name.
         */
        switch (ifrowP->dwType) {
            case MIB_IF_TYPE_ETHERNET:
                _snprintf_s(dev_name, 8, _TRUNCATE, "eth%d", eth++);
                break;

            case MIB_IF_TYPE_TOKENRING:
                _snprintf_s(dev_name, 8, _TRUNCATE, "tr%d", tr++);
                break;

            case MIB_IF_TYPE_FDDI:
                _snprintf_s(dev_name, 8, _TRUNCATE, "fddi%d", fddi++);
                break;

            case MIB_IF_TYPE_LOOPBACK:
                /* There should only be only IPv4 loopback address */
                if (lo > 0) {
                    continue;
                }
                strncpy_s(dev_name, 8, "lo", _TRUNCATE);
                lo++;
                break;

            case MIB_IF_TYPE_PPP:
                _snprintf_s(dev_name, 8, _TRUNCATE, "ppp%d", ppp++);
                break;

            case MIB_IF_TYPE_SLIP:
                _snprintf_s(dev_name, 8, _TRUNCATE, "sl%d", sl++);
                break;

            case IF_TYPE_IEEE80211:
                _snprintf_s(dev_name, 8, _TRUNCATE, "wlan%d", wlan++);
                break;

            default:
                _snprintf_s(dev_name, 8, _TRUNCATE, "net%d", net++);
        }

        /*
         * Allocate a netif structure and space for the name and
         * display name (description in this case).
         */
        curr = (netif *)calloc(1, sizeof(netif));
        if (curr != NULL) {
            wlen = MultiByteToWideChar(CP_OEMCP, 0, ifrowP->bDescr,
                       ifrowP->dwDescrLen, NULL, 0);
            if(wlen == 0) {
                // MultiByteToWideChar should not fail
                // But in rare case it fails, we allow 'char' to be displayed
                curr->displayName = (char *)malloc(ifrowP->dwDescrLen + 1);
            } else {
                curr->displayName = (wchar_t *)malloc((wlen+1)*sizeof(wchar_t));
            }

            curr->name = (char *)malloc(strlen(dev_name) + 1);

            if (curr->name == NULL || curr->displayName == NULL) {
                if (curr->name) free(curr->name);
                if (curr->displayName) free(curr->displayName);
                free(curr);
                curr = NULL;
            }
        }
        if (curr == NULL) {
            JNU_ThrowOutOfMemoryError(env, "Native heap allocation failure");
            free_netif(netifP);
            free(tableP);
            return -1;
        }

        /*
         * Populate the interface. Note that we need to convert the
         * index into its "friendly" value as otherwise we will expose
         * 32-bit numbers as index values.
         */
        strcpy(curr->name, dev_name);
        if (wlen == 0) {
            // display char type in case of MultiByteToWideChar failure
            strncpy(curr->displayName, ifrowP->bDescr, ifrowP->dwDescrLen);
            curr->displayName[ifrowP->dwDescrLen] = '\0';
        } else {
            // call MultiByteToWideChar again to fill curr->displayName
            // it should not fail, because we have called it once before
            if (MultiByteToWideChar(CP_OEMCP, 0, ifrowP->bDescr,
                   ifrowP->dwDescrLen, curr->displayName, wlen) == 0) {
                JNU_ThrowByName(env, "java/lang/Error",
                       "Cannot get multibyte char for interface display name");
                free_netif(netifP);
                free(tableP);
                free(curr->name);
                free(curr->displayName);
                free(curr);
                return -1;
            } else {
                ((wchar_t *)curr->displayName)[wlen] = L'\0';
                curr->dNameIsUnicode = TRUE;
            }
        }

        curr->dwIndex = ifrowP->dwIndex;
        curr->ifType = ifrowP->dwType;
        curr->index = GetFriendlyIfIndex(ifrowP->dwIndex);

        /*
         * Put the interface at tail of list as GetIfTable(,,TRUE) is
         * returning the interfaces in index order.
         */
        count++;
        if (netifP == NULL) {
            netifP = curr;
        } else {
            netif *tail = netifP;
            while (tail->next != NULL) {
                tail = tail->next;
            }
            tail->next = curr;
        }

        /* onto the next interface */
        ifrowP++;
    }

    /*
     * Free the interface table and return the interface list
     */
    if (tableP != NULL) {
        free(tableP);
    }
    *netifPP = netifP;
    return count;
}

/*
 * Enumerate all addresses using the IP helper library
 */
int lookupIPAddrTable(JNIEnv *env, MIB_IPADDRTABLE **tablePP)
{
    MIB_IPADDRTABLE *tableP;
    ULONG size;
    DWORD ret;
    /*
     * Use GetIpAddrTable to enumerate the IP Addresses
     */
    size = sizeof(MIB_IPADDRTABLE);
    tableP = (MIB_IPADDRTABLE *)malloc(size);
    if (tableP == NULL) {
        JNU_ThrowOutOfMemoryError(env, "Native heap allocation failure");
        return -1;
    }

    ret = GetIpAddrTable(tableP, &size, FALSE);
    if (ret == ERROR_INSUFFICIENT_BUFFER || ret == ERROR_BUFFER_OVERFLOW) {
        MIB_IPADDRTABLE * newTableP = (MIB_IPADDRTABLE *)realloc(tableP, size);
        if (newTableP == NULL) {
            free(tableP);
            JNU_ThrowOutOfMemoryError(env, "Native heap allocation failure");
            return -1;
        }
        tableP = newTableP;

        ret = GetIpAddrTable(tableP, &size, FALSE);
    }
    if (ret != NO_ERROR) {
        if (tableP != NULL) {
            free(tableP);
        }
        JNU_ThrowByName(env, "java/lang/Error",
                "IP Helper Library GetIpAddrTable function failed");
        // this different error code is to handle the case when we call
        // GetIpAddrTable in pure IPv6 environment
        return -2;
    }
    *tablePP = tableP;
    return 0;
}

/*
 * Enumerate the IP addresses on an interface, given an IP address table
 * and matching based on index.
 *
 * Returns the count of addresses, or -1 if error. If no error occurs then
 * netaddrPP will return a list of netaddr structures with the IP addresses.
 */
int enumAddresses_win_ipaddrtable(JNIEnv *env, netif *netifP, netaddr **netaddrPP, MIB_IPADDRTABLE *tableP)
{
    DWORD i;
    netaddr *netaddrP;
    int count = 0;
    unsigned long mask;

    /*
     * Iterate through the table to find the addresses with the
     * matching dwIndex. Ignore 0.0.0.0 addresses.
     */
    if (tableP == NULL)
        return 0;
    count = 0;
    netaddrP = NULL;

    i = 0;
    while (i < tableP->dwNumEntries) {
        if (tableP->table[i].dwIndex == netifP->dwIndex &&
            tableP->table[i].dwAddr != 0) {

            netaddr *curr = (netaddr *)malloc(sizeof(netaddr));
            if (curr == NULL) {
                JNU_ThrowOutOfMemoryError(env, "Native heap allocation failure");
                free_netaddr(netaddrP);
                free(tableP);
                return -1;
            }

            curr->addr.sa4.sin_family = AF_INET;
            curr->addr.sa4.sin_addr.s_addr = tableP->table[i].dwAddr;
            /*
             * Get netmask / broadcast address
             */
            switch (netifP->ifType) {
            case MIB_IF_TYPE_ETHERNET:
            case MIB_IF_TYPE_TOKENRING:
            case MIB_IF_TYPE_FDDI:
            case MIB_IF_TYPE_LOOPBACK:
            case IF_TYPE_IEEE80211:
                /**
                 * Contrary to what it seems to indicate, dwBCastAddr doesn't
                 * contain the broadcast address but 0 or 1 depending on whether
                 * the broadcast address should set the bits of the host part
                 * to 0 or 1.
                 * Yes, I know it's stupid, but what can I say, it's MSFTs API.
                 */
                curr->brdcast.sa4.sin_family = AF_INET;
                if (tableP->table[i].dwBCastAddr == 1)
                    curr->brdcast.sa4.sin_addr.s_addr = (tableP->table[i].dwAddr & tableP->table[i].dwMask) | (0xffffffff ^ tableP->table[i].dwMask);
                else
                    curr->brdcast.sa4.sin_addr.s_addr = (tableP->table[i].dwAddr & tableP->table[i].dwMask);
                mask = ntohl(tableP->table[i].dwMask);
                curr->mask = 0;
                while (mask) {
                    mask <<= 1;
                    curr->mask++;
                }
                break;
            case MIB_IF_TYPE_PPP:
            case MIB_IF_TYPE_SLIP:
            default:
                /**
                 * these don't have broadcast/subnet
                 */
                curr->mask = -1;
                    break;
            }

            curr->next = netaddrP;
            netaddrP = curr;
            count++;
        }
        i++;
    }

    *netaddrPP = netaddrP;
    return count;
}


/*
 * Enumerate the IP addresses on an interface, using an IP address table
 * retrieved using GetIPAddrTable and matching based on index.
 *
 * Returns the count of addresses, or -1 if error. If no error occurs then
 * netaddrPP will return a list of netaddr structures with the IP addresses.
 */
int enumAddresses_win(JNIEnv *env, netif *netifP, netaddr **netaddrPP) {
    MIB_IPADDRTABLE *tableP;
    int count;
    int ret = lookupIPAddrTable(env, &tableP);
    if (ret < 0) {
      return NULL;
    }
    count = enumAddresses_win_ipaddrtable(env, netifP, netaddrPP, tableP);
    free(tableP);
    return count;
}


/*
 * Class:     java_net_NetworkInterface
 * Method:    init
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_java_net_NetworkInterface_init(JNIEnv *env, jclass cls)
{
    /*
     * Get the various JNI ids that we require
     */
    ni_class = (*env)->NewGlobalRef(env, cls);
    CHECK_NULL(ni_class);
    ni_nameID = (*env)->GetFieldID(env, ni_class, "name", "Ljava/lang/String;");
    CHECK_NULL(ni_nameID);
    ni_displayNameID = (*env)->GetFieldID(env, ni_class, "displayName", "Ljava/lang/String;");
    CHECK_NULL(ni_displayNameID);
    ni_indexID = (*env)->GetFieldID(env, ni_class, "index", "I");
    CHECK_NULL(ni_indexID);
    ni_addrsID = (*env)->GetFieldID(env, ni_class, "addrs", "[Ljava/net/InetAddress;");
    CHECK_NULL(ni_addrsID);
    ni_bindsID = (*env)->GetFieldID(env, ni_class, "bindings", "[Ljava/net/InterfaceAddress;");
    CHECK_NULL(ni_bindsID);
    ni_childsID = (*env)->GetFieldID(env, ni_class, "childs", "[Ljava/net/NetworkInterface;");
    CHECK_NULL(ni_childsID);
    ni_ctor = (*env)->GetMethodID(env, ni_class, "<init>", "()V");
    CHECK_NULL(ni_ctor);
    ni_ibcls = (*env)->FindClass(env, "java/net/InterfaceAddress");
    CHECK_NULL(ni_ibcls);
    ni_ibcls = (*env)->NewGlobalRef(env, ni_ibcls);
    CHECK_NULL(ni_ibcls);
    ni_ibctrID = (*env)->GetMethodID(env, ni_ibcls, "<init>", "()V");
    CHECK_NULL(ni_ibctrID);
    ni_ibaddressID = (*env)->GetFieldID(env, ni_ibcls, "address", "Ljava/net/InetAddress;");
    CHECK_NULL(ni_ibaddressID);
    ni_ibbroadcastID = (*env)->GetFieldID(env, ni_ibcls, "broadcast", "Ljava/net/Inet4Address;");
    CHECK_NULL(ni_ibbroadcastID);
    ni_ibmaskID = (*env)->GetFieldID(env, ni_ibcls, "maskLength", "S");
    CHECK_NULL(ni_ibmaskID);

    initInetAddressIDs(env);
}

/*
 * Create a NetworkInterface object, populate the name and index, and
 * populate the InetAddress array based on the IP addresses for this
 * interface.
 */
jobject createNetworkInterface
    (JNIEnv *env, netif *ifs, int netaddrCount, netaddr *netaddrP)
{
    jobject netifObj;
    jobject name, displayName;
    jobjectArray addrArr, bindsArr, childArr;
    netaddr *addrs;
    jint addr_index;
    jint bind_index;

    /*
     * Create a NetworkInterface object and populate it
     */
    netifObj = (*env)->NewObject(env, ni_class, ni_ctor);
    CHECK_NULL_RETURN(netifObj, NULL);
    name = (*env)->NewStringUTF(env, ifs->name);
    CHECK_NULL_RETURN(name, NULL);
    if (ifs->dNameIsUnicode) {
        displayName = (*env)->NewString(env, (PWCHAR)ifs->displayName,
                                       (jsize)wcslen ((PWCHAR)ifs->displayName));
    } else {
        displayName = (*env)->NewStringUTF(env, ifs->displayName);
    }
    CHECK_NULL_RETURN(displayName, NULL);
    (*env)->SetObjectField(env, netifObj, ni_nameID, name);
    (*env)->SetObjectField(env, netifObj, ni_displayNameID, displayName);
    (*env)->SetIntField(env, netifObj, ni_indexID, ifs->index);

    /*
     * Get the IP addresses for this interface if necessary
     * Note that 0 is a valid number of addresses.
     */
    if (netaddrCount < 0) {
        netaddrCount = enumAddresses_win(env, ifs, &netaddrP);
        if (netaddrCount < 0) {
            return NULL;
        }
    }
    addrArr = (*env)->NewObjectArray(env, netaddrCount, ia_class, NULL);
    if (addrArr == NULL) {
        free_netaddr(netaddrP);
        return NULL;
    }

    bindsArr = (*env)->NewObjectArray(env, netaddrCount, ni_ibcls, NULL);
    if (bindsArr == NULL) {
      free_netaddr(netaddrP);
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
                free_netaddr(netaddrP);
                return NULL;
            }
            /* default ctor will set family to AF_INET */

            setInetAddress_addr(env, iaObj, ntohl(addrs->addr.sa4.sin_addr.s_addr));
            if ((*env)->ExceptionCheck(env)) {
                free_netaddr(netaddrP);
                return NULL;
            }
            if (addrs->mask != -1) {
              ibObj = (*env)->NewObject(env, ni_ibcls, ni_ibctrID);
              if (ibObj == NULL) {
                free_netaddr(netaddrP);
                return NULL;
              }
              (*env)->SetObjectField(env, ibObj, ni_ibaddressID, iaObj);
              ia2Obj = (*env)->NewObject(env, ia4_class, ia4_ctrID);
              if (ia2Obj == NULL) {
                free_netaddr(netaddrP);
                return NULL;
              }
              setInetAddress_addr(env, ia2Obj, ntohl(addrs->brdcast.sa4.sin_addr.s_addr));
              if ((*env)->ExceptionCheck(env)) {
                  free_netaddr(netaddrP);
                  return NULL;
              }
              (*env)->SetObjectField(env, ibObj, ni_ibbroadcastID, ia2Obj);
              (*env)->DeleteLocalRef(env, ia2Obj);
              (*env)->SetShortField(env, ibObj, ni_ibmaskID, addrs->mask);
              (*env)->SetObjectArrayElement(env, bindsArr, bind_index++, ibObj);
              (*env)->DeleteLocalRef(env, ibObj);
            }
        } else /* AF_INET6 */ {
            int scope;
            iaObj = (*env)->NewObject(env, ia6_class, ia6_ctrID);
            if (iaObj) {
                jboolean ret = setInet6Address_ipaddress(env, iaObj,  (jbyte *)&(addrs->addr.sa6.sin6_addr.s6_addr));
                if (ret == JNI_FALSE) {
                    free_netaddr(netaddrP);
                    return NULL;
                }

                scope = addrs->addr.sa6.sin6_scope_id;
                if (scope != 0) { /* zero is default value, no need to set */
                    setInet6Address_scopeid(env, iaObj, scope);
                    setInet6Address_scopeifname(env, iaObj, netifObj);
                }
                ibObj = (*env)->NewObject(env, ni_ibcls, ni_ibctrID);
                if (ibObj == NULL) {
                  free_netaddr(netaddrP);
                  return NULL;
                }
                (*env)->SetObjectField(env, ibObj, ni_ibaddressID, iaObj);
                (*env)->SetShortField(env, ibObj, ni_ibmaskID, addrs->mask);
                (*env)->SetObjectArrayElement(env, bindsArr, bind_index++, ibObj);
                (*env)->DeleteLocalRef(env, ibObj);
            }
        }
        (*env)->SetObjectArrayElement(env, addrArr, addr_index, iaObj);
        (*env)->DeleteLocalRef(env, iaObj);
        addrs = addrs->next;
        addr_index++;
    }
    (*env)->SetObjectField(env, netifObj, ni_addrsID, addrArr);
    (*env)->SetObjectField(env, netifObj, ni_bindsID, bindsArr);

    free_netaddr(netaddrP);
    (*env)->DeleteLocalRef(env, name);
    (*env)->DeleteLocalRef(env, displayName);
    (*env)->DeleteLocalRef(env, addrArr);
    (*env)->DeleteLocalRef(env, bindsArr);

    /*
     * Windows doesn't have virtual interfaces, so child array
     * is always empty.
     */
    childArr = (*env)->NewObjectArray(env, 0, ni_class, NULL);
    if (childArr == NULL) {
      return NULL;
    }
    (*env)->SetObjectField(env, netifObj, ni_childsID, childArr);
    (*env)->DeleteLocalRef(env, childArr);

    /* return the NetworkInterface */
    return netifObj;
}

/*
 * Class:     java_net_NetworkInterface
 * Method:    getByName0
 * Signature: (Ljava/lang/String;)Ljava/net/NetworkInterface;
 */
JNIEXPORT jobject JNICALL Java_java_net_NetworkInterface_getByName0
    (JNIEnv *env, jclass cls, jstring name)
{
    netif *ifList, *curr;
    jboolean isCopy;
    const char *name_utf;
    jobject netifObj = NULL;

    // Retained for now to support IPv4 only stack, java.net.preferIPv4Stack
    if (ipv6_available()) {
        return Java_java_net_NetworkInterface_getByName0_XP (env, cls, name);
    }

    /* get the list of interfaces */
    if (enumInterfaces(env, &ifList) < 0) {
        return NULL;
    }

    /* get the name as a C string */
    name_utf = (*env)->GetStringUTFChars(env, name, &isCopy);
    if (name_utf != NULL) {

        /* Search by name */
        curr = ifList;
        while (curr != NULL) {
            if (strcmp(name_utf, curr->name) == 0) {
                break;
            }
            curr = curr->next;
        }

        /* if found create a NetworkInterface */
        if (curr != NULL) {;
            netifObj = createNetworkInterface(env, curr, -1, NULL);
        }

        /* release the UTF string */
        (*env)->ReleaseStringUTFChars(env, name, name_utf);
    } else {
        if (!(*env)->ExceptionCheck(env))
            JNU_ThrowOutOfMemoryError(env, NULL);
    }

    /* release the interface list */
    free_netif(ifList);

    return netifObj;
}

/*
 * Class:     NetworkInterface
 * Method:    getByIndex0
 * Signature: (I)LNetworkInterface;
 */
JNIEXPORT jobject JNICALL Java_java_net_NetworkInterface_getByIndex0
  (JNIEnv *env, jclass cls, jint index)
{
    netif *ifList, *curr;
    jobject netifObj = NULL;

    // Retained for now to support IPv4 only stack, java.net.preferIPv4Stack
    if (ipv6_available()) {
        return Java_java_net_NetworkInterface_getByIndex0_XP (env, cls, index);
    }

    /* get the list of interfaces */
    if (enumInterfaces(env, &ifList) < 0) {
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
        netifObj = createNetworkInterface(env, curr, -1, NULL);
    }

    /* release the interface list */
    free_netif(ifList);

    return netifObj;
}


/*
 * Class:     java_net_NetworkInterface
 * Method:    boundInetAddress0
 * Signature: (Ljava/net/InetAddress;)Z
 */
JNIEXPORT jboolean JNICALL Java_java_net_NetworkInterface_boundInetAddress0
    (JNIEnv *env, jclass cls, jobject iaObj)
{
    jobject netifObj = NULL;
    DWORD i;

    int family = getInetAddress_family(env, iaObj);
    JNU_CHECK_EXCEPTION_RETURN(env, JNI_FALSE);

    if (family == java_net_InetAddress_IPv6) {
        if (!ipv6_available())
            return JNI_FALSE;
        return Java_java_net_NetworkInterface_getByInetAddress0_XP(env, cls, iaObj) != NULL;
    } else if (family == java_net_InetAddress_IPv4) {
        jint addr = getInetAddress_addr(env, iaObj);
        JNU_CHECK_EXCEPTION_RETURN(env, JNI_FALSE);

        jboolean found = JNI_FALSE;
        MIB_IPADDRTABLE *tableP;
        if (lookupIPAddrTable(env, &tableP) >= 0 && tableP != NULL) {
            for (i = 0; i < tableP->dwNumEntries; i++) {
                if (tableP->table[i].dwAddr != 0 &&
                    (unsigned long)addr == ntohl(tableP->table[i].dwAddr)) {
                    found = JNI_TRUE;
                    break;
                }
            }
        }
        if (tableP != NULL) {
          free(tableP);
        }
        return found;
    } else {
      // Unknown address family
      return JNI_FALSE;
    }
}

/*
 * Class:     java_net_NetworkInterface
 * Method:    getByInetAddress0
 * Signature: (Ljava/net/InetAddress;)Ljava/net/NetworkInterface;
 */
JNIEXPORT jobject JNICALL Java_java_net_NetworkInterface_getByInetAddress0
    (JNIEnv *env, jclass cls, jobject iaObj)
{
    netif *ifList, *curr;
    MIB_IPADDRTABLE *tableP;
    jobject netifObj = NULL;
    jint addr = getInetAddress_addr(env, iaObj);
    JNU_CHECK_EXCEPTION_RETURN(env, NULL);

    if (ipv6_available()) {
        return Java_java_net_NetworkInterface_getByInetAddress0_XP (env, cls, iaObj);
    }

    /* get the list of interfaces */
    if (enumInterfaces(env, &ifList) < 0) {
        return NULL;
    }

    /*
     * Enumerate the addresses on each interface until we find a
     * matching address.
     */
    tableP = NULL;
    if (lookupIPAddrTable(env, &tableP) >= 0) {
        curr = ifList;
        while (curr != NULL) {
            int count;
            netaddr *addrList;
            netaddr *addrP;

            /* enumerate the addresses on this interface */
            count = enumAddresses_win_ipaddrtable(env, curr, &addrList, tableP);
            if (count < 0) {
                free_netif(ifList);
                free(tableP);
                return NULL;
            }

            /* iterate through each address */
            addrP = addrList;

            while (addrP != NULL) {
                if ((unsigned long)addr == ntohl(addrP->addr.sa4.sin_addr.s_addr)) {
                    break;
                }
                addrP = addrP->next;
            }

            /*
             * Address matched so create NetworkInterface for this interface
             * and address list.
             */
            if (addrP != NULL) {
                /* createNetworkInterface will free addrList */
                netifObj = createNetworkInterface(env, curr, count, addrList);
                break;
            }

            /* on next interface */
            curr = curr->next;
        }
    }

    /* release the IP address table */
    if (tableP != NULL)
        free(tableP);

    /* release the interface list */
    free_netif(ifList);

    return netifObj;
}

/*
 * Class:     java_net_NetworkInterface
 * Method:    getAll
 * Signature: ()[Ljava/net/NetworkInterface;
 */
JNIEXPORT jobjectArray JNICALL Java_java_net_NetworkInterface_getAll
    (JNIEnv *env, jclass cls)
{
    int count;
    netif *ifList = NULL, *curr;
    jobjectArray netIFArr;
    jint arr_index;

    // Retained for now to support IPv4 only stack, java.net.preferIPv4Stack
    if (ipv6_available()) {
        return Java_java_net_NetworkInterface_getAll_XP (env, cls);
    }

    /*
     * Get list of interfaces
     */
    count = enumInterfaces(env, &ifList);
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

        netifObj = createNetworkInterface(env, curr, -1, NULL);
        if (netifObj == NULL) {
            free_netif(ifList);
            return NULL;
        }

        /* put the NetworkInterface into the array */
        (*env)->SetObjectArrayElement(env, netIFArr, arr_index++, netifObj);
        (*env)->DeleteLocalRef(env, netifObj);

        curr = curr->next;
    }

    /* release the interface list */
    free_netif(ifList);

    return netIFArr;
}

/*
 * Class:     java_net_NetworkInterface
 * Method:    isUp0
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_java_net_NetworkInterface_isUp0
    (JNIEnv *env, jclass cls, jstring name, jint index) {
  jboolean ret = JNI_FALSE;

  // Retained for now to support IPv4 only stack, java.net.preferIPv4Stack
  if (ipv6_available()) {
    return Java_java_net_NetworkInterface_isUp0_XP(env, cls, name, index);
  } else {
    MIB_IFROW *ifRowP;
    ifRowP = getIF(index);
    if (ifRowP != NULL) {
      ret = ifRowP->dwAdminStatus == MIB_IF_ADMIN_STATUS_UP &&
            (ifRowP->dwOperStatus == MIB_IF_OPER_STATUS_OPERATIONAL ||
             ifRowP->dwOperStatus == MIB_IF_OPER_STATUS_CONNECTED);
      free(ifRowP);
    }
  }
    return ret;
}

/*
 * Class:     java_net_NetworkInterface
 * Method:    isP2P0
 * Signature: (Ljava/lang/String;I)Z
 */
JNIEXPORT jboolean JNICALL Java_java_net_NetworkInterface_isP2P0
    (JNIEnv *env, jclass cls, jstring name, jint index) {
  MIB_IFROW *ifRowP;
  jboolean ret = JNI_FALSE;

  // Retained for now to support IPv4 only stack, java.net.preferIPv4Stack
  if (ipv6_available()) {
    return Java_java_net_NetworkInterface_isP2P0_XP(env, cls, name, index);
  } else {
    ifRowP = getIF(index);
    if (ifRowP != NULL) {
      switch(ifRowP->dwType) {
      case MIB_IF_TYPE_PPP:
      case MIB_IF_TYPE_SLIP:
        ret = JNI_TRUE;
        break;
      }
      free(ifRowP);
    }
  }
  return ret;
}

/*
 * Class:     java_net_NetworkInterface
 * Method:    isLoopback0
 * Signature: (Ljava/lang/String;I)Z
 */
JNIEXPORT jboolean JNICALL Java_java_net_NetworkInterface_isLoopback0
    (JNIEnv *env, jclass cls, jstring name, jint index) {
  MIB_IFROW *ifRowP;
  jboolean ret = JNI_FALSE;

  // Retained for now to support IPv4 only stack, java.net.preferIPv4Stack
  if (ipv6_available()) {
    return Java_java_net_NetworkInterface_isLoopback0_XP(env, cls, name, index);
  } else {
    ifRowP = getIF(index);
    if (ifRowP != NULL) {
      if (ifRowP->dwType == MIB_IF_TYPE_LOOPBACK)
        ret = JNI_TRUE;
      free(ifRowP);
    }
    return ret;
  }
}

/*
 * Class:     java_net_NetworkInterface
 * Method:    supportsMulticast0
 * Signature: (Ljava/lang/String;I)Z
 */
JNIEXPORT jboolean JNICALL Java_java_net_NetworkInterface_supportsMulticast0
    (JNIEnv *env, jclass cls, jstring name, jint index) {
    return Java_java_net_NetworkInterface_supportsMulticast0_XP(env, cls,
                                                               name, index);
}

/*
 * Class:     java_net_NetworkInterface
 * Method:    getMacAddr0
 * Signature: ([bLjava/lang/String;I)[b
 */
JNIEXPORT jbyteArray JNICALL Java_java_net_NetworkInterface_getMacAddr0
    (JNIEnv *env, jclass class, jbyteArray addrArray, jstring name, jint index) {
  jbyteArray ret = NULL;
  int len;
  MIB_IFROW *ifRowP;

  // Retained for now to support IPv4 only stack, java.net.preferIPv4Stack
  if (ipv6_available()) {
    return Java_java_net_NetworkInterface_getMacAddr0_XP(env, class, name, index);
  } else {
    ifRowP = getIF(index);
    if (ifRowP != NULL) {
      switch(ifRowP->dwType) {
      case MIB_IF_TYPE_ETHERNET:
      case MIB_IF_TYPE_TOKENRING:
      case MIB_IF_TYPE_FDDI:
      case IF_TYPE_IEEE80211:
        len = ifRowP->dwPhysAddrLen;
        if (len > 0) {
            ret = (*env)->NewByteArray(env, len);
            if (!IS_NULL(ret)) {
              (*env)->SetByteArrayRegion(env, ret, 0, len, (jbyte *) ifRowP->bPhysAddr);
            }
        }
        break;
      }
      free(ifRowP);
    }
    return ret;
  }
}

/*
 * Class:       java_net_NetworkInterface
 * Method:      getMTU0
 * Signature:   ([bLjava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_java_net_NetworkInterface_getMTU0
    (JNIEnv *env, jclass class, jstring name, jint index) {
  jint ret = -1;
  MIB_IFROW *ifRowP;

  // Retained for now to support IPv4 only stack, java.net.preferIPv4Stack
  if (ipv6_available()) {
    return Java_java_net_NetworkInterface_getMTU0_XP(env, class, name, index);
  } else {
    ifRowP = getIF(index);
    if (ifRowP != NULL) {
      ret = ifRowP->dwMtu;
      free(ifRowP);
    }
    return ret;
  }
}
