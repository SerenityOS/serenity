/*
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

/*
 *
 *  (C) Copyright IBM Corp. 1999 All Rights Reserved.
 *  Copyright 1997 The Open Group Research Institute.  All rights reserved.
 */

package sun.security.krb5.internal.ccache;

/**
 * Constants used by file-based credential cache classes.
 *
 * @author Yanni Zhang
 *
 */
public interface FileCCacheConstants {
    /*
     * FCC version 2 contains type information for principals.  FCC
     * version 1 does not.
     *
     * FCC version 3 contains keyblock encryption type information, and is
     * architecture independent.  Previous versions are not. */
    public final int KRB5_FCC_FVNO_1 = 0x501;
    public final int KRB5_FCC_FVNO_2 = 0x502;
    public final int KRB5_FCC_FVNO_3 = 0x503;
    public final int KRB5_FCC_FVNO_4 = 0x504;
    public final int FCC_TAG_DELTATIME = 1;
    public final int KRB5_NT_UNKNOWN = 0;
    public final int TKT_FLG_FORWARDABLE = 0x40000000;
    public final int TKT_FLG_FORWARDED  =  0x20000000;
    public final int TKT_FLG_PROXIABLE   = 0x10000000;
    public final int TKT_FLG_PROXY        = 0x08000000;
    public final int TKT_FLG_MAY_POSTDATE  = 0x04000000;
    public final int TKT_FLG_POSTDATED     = 0x02000000;
    public final int TKT_FLG_INVALID        = 0x01000000;
    public final int TKT_FLG_RENEWABLE     = 0x00800000;
    public final int TKT_FLG_INITIAL       = 0x00400000;
    public final int TKT_FLG_PRE_AUTH      = 0x00200000;
    public final int TKT_FLG_HW_AUTH       = 0x00100000;
}
