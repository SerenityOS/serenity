/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.provider.certpath.ldap;

import java.util.HashMap;
import java.util.List;
import java.security.*;
import java.security.cert.CertStoreParameters;
import static sun.security.util.SecurityConstants.PROVIDER_VER;

/**
 * Provider class for the JdkLDAP provider.
 * Supports LDAP cert store.
 *
 * @since   9
 */
public final class JdkLDAP extends Provider {

    private static final long serialVersionUID = -2279741232933606418L;

    private static final class ProviderService extends Provider.Service {
        ProviderService(Provider p, String type, String algo, String cn,
            List<String> aliases, HashMap<String, String> attrs) {
            super(p, type, algo, cn, aliases, attrs);
        }

        @Override
        public Object newInstance(Object ctrParamObj)
            throws NoSuchAlgorithmException {
            String type = getType();
            String algo = getAlgorithm();
            if (type.equals("CertStore") && algo.equals("LDAP")) {
                if (ctrParamObj != null &&
                    !(ctrParamObj instanceof CertStoreParameters)) {
                    throw new InvalidParameterException
                    ("constructorParameter must be instanceof CertStoreParameters");
                }
                try {
                    return new LDAPCertStore((CertStoreParameters) ctrParamObj);
                } catch (Exception ex) {
                    throw new NoSuchAlgorithmException("Error constructing " +
                        type + " for " + algo + " using JdkLDAP", ex);
                }
            }
            throw new ProviderException("No impl for " + algo + " " + type);
        }
    }

    @SuppressWarnings("removal")
    public JdkLDAP() {
        super("JdkLDAP", PROVIDER_VER, "JdkLDAP Provider (implements LDAP CertStore)");

        final Provider p = this;
        PrivilegedAction<Void> pa = () -> {
            HashMap<String, String> attrs = new HashMap<>(2);
            attrs.put("LDAPSchema", "RFC2587");
            attrs.put("ImplementedIn", "Software");

            /*
             * CertStore
             * attrs: LDAPSchema, ImplementedIn
             */
            putService(new ProviderService(p, "CertStore",
                       "LDAP", "sun.security.provider.certpath.ldap.LDAPCertStore",
                       null, attrs));
            return null;
        };
        AccessController.doPrivileged(pa);
    }
}
