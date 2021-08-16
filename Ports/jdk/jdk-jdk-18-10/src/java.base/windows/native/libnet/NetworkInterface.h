/*
 * Copyright (c) 2002, 2016, Oracle and/or its affiliates. All rights reserved.
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

#ifndef NETWORK_INTERFACE_H
#define NETWORK_INTERFACE_H

#include "net_util.h"

/*
 * Structures used when enumerating interfaces and addresses
 */
typedef struct _netaddr  {
    SOCKETADDRESS    addr;                  /* IPv4 or IPv6 address */
    SOCKETADDRESS    brdcast;
    short            mask;
    struct _netaddr *next;
} netaddr;

typedef struct _netif {
    char *name;
    char *displayName;
    DWORD dwIndex;              /* Internal index */
    DWORD ifType;               /* Interface type */
    int index;                  /* Friendly index */
    struct _netif *next;

    /* Following fields used on Windows XP when IPv6 is used only */
    jboolean hasIpv6Address;    /* true when following fields valid */
    jboolean dNameIsUnicode;    /* Display Name is Unicode */
    int naddrs;                 /* Number of addrs */
    DWORD ipv6Index;
    struct _netaddr *addrs;     /* addr list for interfaces */
} netif;

extern void free_netif(netif *netifP);
extern void free_netaddr(netaddr *netaddrP);

/* various JNI ids */
extern jclass ni_class;             /* NetworkInterface */

extern jmethodID ni_ctor;           /* NetworkInterface() */

extern jfieldID ni_indexID;         /* NetworkInterface.index */
extern jfieldID ni_addrsID;         /* NetworkInterface.addrs */
extern jfieldID ni_bindsID;         /* NetworkInterface.bindings */
extern jfieldID ni_nameID;          /* NetworkInterface.name */
extern jfieldID ni_displayNameID;   /* NetworkInterface.displayName */
extern jfieldID ni_childsID;        /* NetworkInterface.childs */

extern jclass ni_ibcls;             /* InterfaceAddress */
extern jmethodID ni_ibctrID;        /* InterfaceAddress() */
extern jfieldID ni_ibaddressID;     /* InterfaceAddress.address */
extern jfieldID ni_ibbroadcastID;   /* InterfaceAddress.broadcast */
extern jfieldID ni_ibmaskID;        /* InterfaceAddress.maskLength */

int enumInterfaces(JNIEnv *env, netif **netifPP);

// Windows Visa (and later) only.....
#ifndef IF_TYPE_IEEE80211
#define IF_TYPE_IEEE80211     71
#endif

#endif
