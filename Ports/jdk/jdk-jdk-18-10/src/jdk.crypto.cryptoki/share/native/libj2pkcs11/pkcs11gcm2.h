/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

/* There is a known incompatibility for CK_GCM_PARAMS structure.
 * PKCS#11 v2.40 standard mechanisms specification specifies
 * CK_GCM_PARAMS as
 *     typedef struct CK_GCM_PARAMS {
 *         CK_BYTE_PTR       pIv;
 *         CK_ULONG          ulIvLen;
 *         CK_BYTE_PTR       pAAD;
 *         CK_ULONG          ulAADLen;
 *         CK_ULONG          ulTagBits;
 *     } CK_GCM_PARAMS;
 * However, the official header file of PKCS#11 v2.40 defines the
 * CK_GCM_PARAMS with an extra "ulIvBits" field (type CK_ULONG).
 * NSS uses the spec version while Solaris and SoftHSM2 use the header
 * version. In order to work with both sides, SunPKCS11 provider defines
 * the spec version of CK_GCM_PARAMS as CK_GCM_PARAMS_NO_IVBITS (as in this
 * file) and uses it first before failing over to the header version.
 */
#ifndef _PKCS11GCM2_H_
#define _PKCS11GCM2_H_ 1

/* include the platform dependent part of the header */
typedef struct CK_GCM_PARAMS_NO_IVBITS {
    CK_BYTE_PTR       pIv;
    CK_ULONG          ulIvLen;
    CK_BYTE_PTR       pAAD;
    CK_ULONG          ulAADLen;
    CK_ULONG          ulTagBits;
} CK_GCM_PARAMS_NO_IVBITS;

typedef CK_GCM_PARAMS_NO_IVBITS CK_PTR CK_GCM_PARAMS_NO_IVBITS_PTR;

#endif /* _PKCS11GCM2_H_ */
