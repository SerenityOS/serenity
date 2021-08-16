/*
 * Copyright (c) 1999, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jndi.ldap.sasl;

import javax.security.auth.callback.*;
import javax.security.sasl.RealmCallback;
import javax.security.sasl.RealmChoiceCallback;
import java.io.IOException;

/**
 * DefaultCallbackHandler for satisfying NameCallback and
 * PasswordCallback for an LDAP client.
 * NameCallback is used for getting the authentication ID and is
 * gotten from the java.naming.security.principal property.
 * PasswordCallback is gotten from the java.naming.security.credentials
 * property and must be of type String, char[] or byte[].
 * If byte[], it is assumed to have UTF-8 encoding.
 *
 * If the caller of getPassword() will be using the password as
 * a byte array, then it should encode the char[] array returned by
 * getPassword() into a byte[] using UTF-8.
 *
 * @author Rosanna Lee
 */
final class DefaultCallbackHandler implements CallbackHandler {
    private char[] passwd;
    private String authenticationID;
    private String authRealm;

    DefaultCallbackHandler(String principal, Object cred, String realm)
        throws IOException {
        authenticationID = principal;
        authRealm = realm;
        if (cred instanceof String) {
            passwd = ((String)cred).toCharArray();
        } else if (cred instanceof char[]) {
            passwd = ((char[])cred).clone();
        } else if (cred != null) {
            // assume UTF-8 encoding
            String orig = new String((byte[])cred, "UTF8");
            passwd = orig.toCharArray();
        }
    }

    public void handle(Callback[] callbacks)
        throws IOException, UnsupportedCallbackException {
            for (int i = 0; i < callbacks.length; i++) {
                if (callbacks[i] instanceof NameCallback) {
                    ((NameCallback)callbacks[i]).setName(authenticationID);

                } else if (callbacks[i] instanceof PasswordCallback) {
                    ((PasswordCallback)callbacks[i]).setPassword(passwd);

                } else if (callbacks[i] instanceof RealmChoiceCallback) {
                    /* Deals with a choice of realms */
                    String[] choices =
                        ((RealmChoiceCallback)callbacks[i]).getChoices();
                    int selected = 0;

                    if (authRealm != null && authRealm.length() > 0) {
                        selected = -1; // no realm chosen
                        for (int j = 0; j < choices.length; j++) {
                            if (choices[j].equals(authRealm)) {
                                selected = j;
                            }
                        }
                        if (selected == -1) {
                            StringBuilder allChoices = new StringBuilder();
                            for (int j = 0; j <  choices.length; j++) {
                                allChoices.append(choices[j]).append(',');
                            }
                            throw new IOException("Cannot match " +
                                "'java.naming.security.sasl.realm' property value, '" +
                                authRealm + "' with choices " + allChoices +
                                "in RealmChoiceCallback");
                        }
                    }

                    ((RealmChoiceCallback)callbacks[i]).setSelectedIndex(selected);

                } else if (callbacks[i] instanceof RealmCallback) {
                    /* 1 or 0 realms specified in challenge */
                    RealmCallback rcb = (RealmCallback) callbacks[i];
                    if (authRealm != null) {
                        rcb.setText(authRealm);  // Use what user supplied
                    } else {
                        String defaultRealm = rcb.getDefaultText();
                        if (defaultRealm != null) {
                            rcb.setText(defaultRealm); // Use what server supplied
                        } else {
                            rcb.setText("");  // Specify no realm
                        }
                    }
                } else {
                    throw new UnsupportedCallbackException(callbacks[i]);
                }
            }
    }

    void clearPassword() {
        if (passwd != null) {
            for (int i = 0; i < passwd.length; i++) {
                passwd[i] = '\0';
            }
            passwd = null;
        }
    }

    @SuppressWarnings("deprecation")
    protected void finalize() throws Throwable {
        clearPassword();
    }
}
