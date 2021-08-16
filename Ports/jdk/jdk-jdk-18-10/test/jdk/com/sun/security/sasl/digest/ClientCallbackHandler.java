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
import java.util.*;
import java.io.*;
import javax.security.sasl.RealmCallback;
import javax.security.sasl.RealmChoiceCallback;
import com.sun.security.auth.callback.TextCallbackHandler;

public final class ClientCallbackHandler extends TextCallbackHandler {
    private String username = "john";
    private String password = "test123";
    private boolean auto;

    public ClientCallbackHandler(boolean auto) {
        super();
        this.auto = auto;
    }

    public void handle(Callback[] callbacks) throws UnsupportedCallbackException,
    IOException {
        NameCallback ncb = null;
        PasswordCallback pcb = null;
        RealmChoiceCallback rccb = null;

        List namePw = new ArrayList(3);

        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof NameCallback) {
                if (auto) {
                    ((NameCallback)callbacks[i]).setName(username);
                } else {
                    // To be processed by TextCallbackHandler
                    namePw.add(callbacks[i]);
                }
            } else if (callbacks[i] instanceof PasswordCallback) {
                if (auto) {
                    ((PasswordCallback)callbacks[i]).setPassword(
                        password.toCharArray());
                } else {
                    // To be processed by TextCallbackHandler
                    namePw.add(callbacks[i]);
                }
            } else if (callbacks[i] instanceof RealmChoiceCallback) {
                RealmChoiceCallback rcb = (RealmChoiceCallback) callbacks[i];
                if (!auto) {
                    System.err.println(rcb.getPrompt());
                }

                String[] choices = rcb.getChoices();

                if (!auto) {
                    for (int j=0; j < choices.length; j++) {
                        System.err.println(j + ":" + choices[j]);
                    }
                }

                int selection;
                if (auto) {
                    selection = 0;
                } else {
                    System.err.print("Enter choice number: ");
                    String result = readLine();
                    if (result.equals("")) {
                        selection = rcb.getDefaultChoice();
                    } else {
                        selection = Integer.parseInt(result);
                    }
                }
                rcb.setSelectedIndex(selection);

            } else if (callbacks[i] instanceof RealmCallback) {
                RealmCallback rcb = (RealmCallback) callbacks[i];
                String realm = rcb.getDefaultText();

                if (auto) {
                    if (realm != null) {
                        rcb.setText(realm);
                    }
                } else {
                    if (realm == null) {
                        System.err.print(rcb.getPrompt());
                    } else {
                        System.err.print(rcb.getPrompt() + " [" + realm + "] ");
                    }

                    System.err.flush();

                    String result = readLine();
                    if (result.equals("")) {
                        result = realm;
                    }
                    rcb.setText(result);
                }
            } else {
                throw new UnsupportedCallbackException(callbacks[i]);
            }
        }

        // Process name/password callbacks using superclass
        if (namePw.size() > 0) {
            Callback[] np = new Callback[namePw.size()];
            namePw.toArray(np);

            super.handle(np);
        }
    }

    /* Reads a line of input */
    private String readLine() throws IOException {
        return new BufferedReader
            (new InputStreamReader(System.in)).readLine();
    }
}
