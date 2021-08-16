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

package sun.security.krb5.internal;

import sun.security.util.*;
import java.io.IOException;

/**
 * Implements the ASN.1 KDCOptions type.
 *
 * <pre>{@code
 * KDCOptions   ::= KerberosFlags
 *      -- reserved(0),
 *      -- forwardable(1),
 *      -- forwarded(2),
 *      -- proxiable(3),
 *      -- proxy(4),
 *      -- allow-postdate(5),
 *      -- postdated(6),
 *      -- unused7(7),
 *      -- renewable(8),
 *      -- unused9(9),
 *      -- unused10(10),
 *      -- opt-hardware-auth(11),
 *      -- unused12(12),
 *      -- unused13(13),
 * -- 15 is reserved for canonicalize
 *      -- unused15(15),
 * -- 26 was unused in 1510
 *      -- disable-transited-check(26),
 *      -- renewable-ok(27),
 *      -- enc-tkt-in-skey(28),
 *      -- renew(30),
 *      -- validate(31)
 *
 * KerberosFlags ::= BIT STRING (SIZE (32..MAX))
 *                   -- minimum number of bits shall be sent,
 *                   -- but no fewer than 32
 * }</pre>
 *
 * <p>
 * This definition reflects the Network Working Group RFC 4120
 * specification available at
 * <a href="http://www.ietf.org/rfc/rfc4120.txt">
 * http://www.ietf.org/rfc/rfc4120.txt</a>.
 */

public class LoginOptions extends KDCOptions {

    // Login Options

    public static final int RESERVED        = 0;
    public static final int FORWARDABLE     = 1;
    public static final int PROXIABLE       = 3;
    public static final int ALLOW_POSTDATE  = 5;
    public static final int RENEWABLE       = 8;
    public static final int RENEWABLE_OK    = 27;
    public static final int ENC_TKT_IN_SKEY = 28;
    public static final int RENEW           = 30;
    public static final int VALIDATE        = 31;
    public static final int MAX             = 31;

}
