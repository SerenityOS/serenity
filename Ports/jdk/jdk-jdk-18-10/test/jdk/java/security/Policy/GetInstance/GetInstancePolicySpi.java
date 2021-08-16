/*
 * Copyright (c) 2005, 2007, Oracle and/or its affiliates. All rights reserved.
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

import java.security.*;
import java.net.URL;
import java.net.MalformedURLException;
import sun.security.provider.*;

public class GetInstancePolicySpi extends PolicySpi {

    private Policy p;

    public GetInstancePolicySpi(final Policy.Parameters params) {
        p = AccessController.doPrivileged
            (new PrivilegedAction<Policy>() {
            public Policy run() {
                if (params instanceof URIParameter) {
                    URIParameter uriParam = (URIParameter)params;
                    try {
                        URL url = uriParam.getURI().toURL();
                        return new PolicyFile(url);
                    } catch (MalformedURLException mue) {
                        throw new IllegalArgumentException(mue);
                    }
                }
                return new PolicyFile();
            }
        });
    }

    public boolean engineImplies(ProtectionDomain domain, Permission perm) {

        /**
         * Note there is no need to capture own protection domain and
         * return immediately if we are performing a check against ourself
         * (a task normally needed for custom policy implementations).
         *
         * We simply call PolicyFile.implies - any doPrivileged
         * that PolicyFile performs will truncate us from the current ACC.
         */

        return p.implies(domain, perm);
    }
}
