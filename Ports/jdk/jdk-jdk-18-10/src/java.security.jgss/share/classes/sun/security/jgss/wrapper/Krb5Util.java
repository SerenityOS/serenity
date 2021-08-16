/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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
package sun.security.jgss.wrapper;

import org.ietf.jgss.*;
import javax.security.auth.kerberos.ServicePermission;

/**
 * This class is an utility class for Kerberos related stuff.
 * @author Valerie Peng
 * @since 1.6
 */
class Krb5Util {

    // Return the Kerberos TGS principal name using the domain
    // of the specified <code>name</code>
    static String getTGSName(GSSNameElement name)
        throws GSSException {
        String krbPrinc = name.getKrbName();
        int atIndex = krbPrinc.indexOf('@');
        String realm = krbPrinc.substring(atIndex + 1);
        StringBuilder sb = new StringBuilder("krbtgt/");
        sb.append(realm).append('@').append(realm);
        return sb.toString();
    }

    // Perform the Service Permission check using the specified
    // <code>target</code> and <code>action</code>
    static void checkServicePermission(String target, String action) {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            SunNativeProvider.debug("Checking ServicePermission(" +
                                    target + ", " + action + ")");
            ServicePermission perm =
                new ServicePermission(target, action);
            sm.checkPermission(perm);
        }
    }
}
