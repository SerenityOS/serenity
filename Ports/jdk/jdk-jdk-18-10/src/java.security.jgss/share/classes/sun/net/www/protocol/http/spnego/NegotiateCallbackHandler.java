/*
 * Copyright (c) 2005, 2017, Oracle and/or its affiliates. All rights reserved.
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

package sun.net.www.protocol.http.spnego;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.Arrays;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import sun.net.www.protocol.http.HttpCallerInfo;
import sun.security.jgss.LoginConfigImpl;

/**
 * @since 1.6
 * Special callback handler used in JGSS for the HttpCaller.
 */
public class NegotiateCallbackHandler implements CallbackHandler {

    private String username;
    private char[] password;

    /**
     * Authenticator asks for username and password in a single prompt,
     * but CallbackHandler checks one by one. So, no matter which callback
     * gets handled first, make sure Authenticator is only called once.
     */
    private boolean answered;

    private final HttpCallerInfo hci;

    public NegotiateCallbackHandler(HttpCallerInfo hci) {
        this.hci = hci;
    }

    private void getAnswer() {
        if (!answered) {
            answered = true;
            Authenticator auth;
            if (hci.authenticator != null) {
                auth = hci.authenticator;
            } else {
                auth = LoginConfigImpl.HTTP_USE_GLOBAL_CREDS ?
                        Authenticator.getDefault() : null;
            }

            if (auth != null) {
                PasswordAuthentication passAuth =
                        auth.requestPasswordAuthenticationInstance(
                                hci.host, hci.addr, hci.port, hci.protocol,
                                hci.prompt, hci.scheme, hci.url, hci.authType);
                /**
                 * To be compatible with existing callback handler implementations,
                 * when the underlying Authenticator is canceled, username and
                 * password are assigned null. No exception is thrown.
                 */
                if (passAuth != null) {
                    username = passAuth.getUserName();
                    password = passAuth.getPassword();
                }
            }
        }
    }

    public void handle(Callback[] callbacks) throws
            UnsupportedCallbackException, IOException {
        for (int i=0; i<callbacks.length; i++) {
            Callback callBack = callbacks[i];

            if (callBack instanceof NameCallback) {
                getAnswer();
                ((NameCallback)callBack).setName(username);
            } else if (callBack instanceof PasswordCallback) {
                getAnswer();
                ((PasswordCallback)callBack).setPassword(password);
                if (password != null) Arrays.fill(password, ' ');
            } else {
                throw new UnsupportedCallbackException(callBack,
                        "Call back not supported");
            }
        }
    }
}
