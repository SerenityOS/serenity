/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.provider;

import java.util.*;
import java.security.*;

import sun.security.rsa.SunRsaSignEntries;
import static sun.security.util.SecurityConstants.PROVIDER_VER;


/**
 * Provider used for verification of signed JAR files *if* the Sun and
 * SunRsaSign main classes have been removed. Otherwise, this provider is not
 * necessary and registers no algorithms. This functionality only exists to
 * support a use case required by a specific customer and is not generally
 * supported.
 *
 * @since  1.7
 * @author Andreas Sterbenz
 */
public final class VerificationProvider extends Provider {

    @java.io.Serial
    private static final long serialVersionUID = 7482667077568930381L;

    private static final boolean ACTIVE;

    static {
        boolean b;
        try {
            Class.forName("sun.security.provider.Sun");
            Class.forName("sun.security.rsa.SunRsaSign");
            b = false;
        } catch (ClassNotFoundException e) {
            b = true;
        }
        ACTIVE = b;
    }

    @SuppressWarnings("removal")
    public VerificationProvider() {
        super("SunJarVerification", PROVIDER_VER, "Jar Verification Provider");
        // register all algorithms normally registered by the Sun and SunRsaSign
        // providers, but only if they are missing
        if (ACTIVE == false) {
            return;
        }

        Provider p = this;
        Iterator<Provider.Service> sunIter = new SunEntries(p).iterator();
        Iterator<Provider.Service> rsaIter =
            new SunRsaSignEntries(p).iterator();

        // if there is no security manager installed, put directly into
        // the provider
        if (System.getSecurityManager() == null) {
            putEntries(sunIter);
            putEntries(rsaIter);
        } else {
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                public Void run() {
                    putEntries(sunIter);
                    putEntries(rsaIter);
                    return null;
                }
            });
        }
    }

    void putEntries(Iterator<Provider.Service> i) {
        while (i.hasNext()) {
            putService(i.next());
        }
    }

}
