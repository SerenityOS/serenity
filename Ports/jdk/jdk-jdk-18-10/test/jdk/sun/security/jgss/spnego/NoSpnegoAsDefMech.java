/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 * @test
 * @bug 6770883
 * @modules java.security.jgss/sun.security.jgss
 * @run main/othervm NoSpnegoAsDefMech
 * @summary Infinite loop if SPNEGO specified as sun.security.jgss.mechanism
 */

import org.ietf.jgss.*;
import sun.security.jgss.*;

public class NoSpnegoAsDefMech {

    public static void main(String[] argv) throws Exception {
        System.setProperty("sun.security.jgss.mechanism", GSSUtil.GSS_SPNEGO_MECH_OID.toString());
        try {
            GSSManager.getInstance().createName("service@localhost", GSSName.NT_HOSTBASED_SERVICE, new Oid("1.3.6.1.5.5.2"));
        } catch (GSSException e) {
            // This is OK, for example, krb5.conf is missing or other problems
        }
    }
}
