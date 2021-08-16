/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
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

// in nss.h:
// extern PRBool NSS_VersionCheck(const char *importedVersion);
// extern SECStatus NSS_Initialize(const char *configdir,
//     const char *certPrefix, const char *keyPrefix,
//     const char *secmodName, PRUint32 flags);

typedef int (*FPTR_VersionCheck)(const char *importedVersion);
typedef int (*FPTR_Initialize)(const char *configdir,
        const char *certPrefix, const char *keyPrefix,
        const char *secmodName, unsigned int flags);

#ifdef SECMOD_DEBUG
typedef int (*FPTR_GetError)(void);
#endif //SECMOD_DEBUG

// in secmod.h
//extern SECMODModule *SECMOD_LoadModule(char *moduleSpec,SECMODModule *parent,
//                                                      PRBool recurse);
//char **SECMOD_GetModuleSpecList(SECMODModule *module);
//extern SECMODModuleList *SECMOD_GetDBModuleList(void);

typedef void *(*FPTR_LoadModule)(char *moduleSpec, void *parent, int recurse);
typedef char **(*FPTR_GetModuleSpecList)(void *module);
typedef void *(*FPTR_GetDBModuleList)(void);
