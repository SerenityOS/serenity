/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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

import javax.security.auth.callback.*;
import java.util.Map;
import java.util.Properties;
import java.io.*;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.RealmCallback;

public final class PropertiesFileCallbackHandler implements CallbackHandler {
    private Properties pwDb, namesDb, proxyDb;

    /**
     * Contents of files are in the Properties file format.
     *
     * @param pwFile name of file containing name/password pairs
     * @param namesFile name of file containing name to canonicalized name
     * @param proxyFile name of file containing authname to list of authzids
     */
    public PropertiesFileCallbackHandler(String pwFile, String namesFile,
        String proxyFile) throws IOException {
        String dir = System.getProperty("test.src");
        if (dir == null) {
            dir = ".";
        }
        dir = dir + "/";

        if (pwFile != null) {
            pwDb = new Properties();
            pwDb.load(new FileInputStream(dir+pwFile));
        }

        if (namesFile != null) {
            namesDb = new Properties();
            namesDb.load(new FileInputStream(dir+namesFile));
        }

        if (proxyFile != null) {
            proxyDb = new Properties();
            proxyDb.load(new FileInputStream(dir+proxyFile));
        }
    }

    public void handle(Callback[] callbacks)
        throws UnsupportedCallbackException {
        NameCallback ncb = null;
        PasswordCallback pcb = null;
        AuthorizeCallback acb = null;
        RealmCallback rcb = null;

        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof NameCallback) {
                ncb = (NameCallback) callbacks[i];
            } else if (callbacks[i] instanceof PasswordCallback) {
                pcb = (PasswordCallback) callbacks[i];
            } else if (callbacks[i] instanceof AuthorizeCallback) {
                acb = (AuthorizeCallback) callbacks[i];
            } else if (callbacks[i] instanceof RealmCallback) {
                rcb = (RealmCallback) callbacks[i];
            } else {
                throw new UnsupportedCallbackException(callbacks[i]);
            }
        }

        // Process retrieval of password; can get password iff
        // username is available in NameCallback
        //
        // Ignore realm for now; could potentially use different dbs for
        // different realms

        if (pcb != null && ncb != null) {
            String username = ncb.getDefaultName();
            String pw = pwDb.getProperty(username);
            if (pw != null) {
                char[] pwchars = pw.toCharArray();
                pcb.setPassword(pwchars);
                // Clear pw
                for (int i = 0; i <pwchars.length; i++) {
                    pwchars[i] = 0;
                }

                // Set canonicalized username if any
                String canonAuthid =
                    (namesDb != null? namesDb.getProperty(username) : null);
                if (canonAuthid != null) {
                    ncb.setName(canonAuthid);
                }
            }
        }

        // Check for authorization

        // Ignore realm for now; could potentially use different dbs for
        // different realms

        if (acb != null) {
            String authid = acb.getAuthenticationID();
            String authzid = acb.getAuthorizationID();
            if (authid.equals(authzid)) {
                // Self is always authorized
                acb.setAuthorized(true);

            } else {
                // Check db for allowed authzids
                String authzes = (proxyDb != null ? proxyDb.getProperty(authid)
                    : null);
                if (authzes != null && authzes.indexOf(authzid) >= 0) {
                    // XXX need to search for subtrings or use StringTokenizer
                    // to avoid incorrectly matching subnames
                    acb.setAuthorized(true);
                }
            }

            if (acb.isAuthorized()) {
                // Set canonicalized name
                String canonAuthzid = (namesDb != null ?
                    namesDb.getProperty(authzid) : null);
                if (canonAuthzid != null) {
                    acb.setAuthorizedID(canonAuthzid);
                }
            }
        }
    }
}
