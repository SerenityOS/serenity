/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.security.Principal;
import java.util.Arrays;
import java.util.Locale;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.ChoiceCallback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.LanguageCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextInputCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

/*
 * @test
 * @bug 8048138
 * @summary Checks if JAAS login works fine with standard callbacks
 * @compile DefaultHandlerModule.java
 * @run main/othervm StandardCallbacks
 */
public class StandardCallbacks {

    private static final String USERNAME = "username";
    private static final char[] PASSWORD = "password".toCharArray();

    public static void main(String[] args) throws LoginException {
        System.setProperty("java.security.auth.login.config",
                System.getProperty("test.src")
                        + System.getProperty("file.separator")
                        + "custom.config");

        CustomCallbackHandler handler = new CustomCallbackHandler(USERNAME);
        LoginContext context = new LoginContext("StandardCallbacks", handler);

        handler.setPassword(PASSWORD);
        System.out.println("Try to login with correct password, "
                + "successful authentication is expected");
        context.login();
        System.out.println("Authentication succeeded!");

        Subject subject = context.getSubject();
        System.out.println("Authenticated user has the following principals ["
                + subject.getPrincipals().size() + " ]:");
        boolean found = true;
        for (Principal principal : subject.getPrincipals()) {
            System.out.println("principal: " + principal);
            if (principal instanceof CustomLoginModule.TestPrincipal) {
                CustomLoginModule.TestPrincipal testPrincipal =
                        (CustomLoginModule.TestPrincipal) principal;
                if (USERNAME.equals(testPrincipal.getName())) {
                    System.out.println("Found test principal: "
                            + testPrincipal);
                    found = true;
                    break;
                }
            }
        }

        if (!found) {
            throw new RuntimeException("TestPrincipal not found");
        }

        // check if all expected text output callbacks have been called
        if (!handler.info) {
            throw new RuntimeException("TextOutputCallback.INFO not called");
        }

        if (!handler.warning) {
            throw new RuntimeException("TextOutputCallback.WARNING not called");
        }

        if (!handler.error) {
            throw new RuntimeException("TextOutputCallback.ERROR not called");
        }

        System.out.println("Authenticated user has the following public "
                + "credentials [" + subject.getPublicCredentials().size()
                + "]:");
        subject.getPublicCredentials().stream().
                forEach((o) -> {
                    System.out.println("public credential: " + o);
        });

        context.logout();

        System.out.println("Test passed");
    }

    private static class CustomCallbackHandler implements CallbackHandler {

        private final String username;
        private char[] password;
        private boolean info = false;
        private boolean warning = false;
        private boolean error = false;

        CustomCallbackHandler(String username) {
            this.username = username;
        }

        void setPassword(char[] password) {
            this.password = password;
        }

        @Override
        public void handle(Callback[] callbacks)
                throws UnsupportedCallbackException {
            for (Callback callback : callbacks) {
                if (callback instanceof TextOutputCallback) {
                    TextOutputCallback toc = (TextOutputCallback) callback;
                    switch (toc.getMessageType()) {
                        case TextOutputCallback.INFORMATION:
                            System.out.println("INFO: " + toc.getMessage());
                            info = true;
                            break;
                        case TextOutputCallback.ERROR:
                            System.out.println("ERROR: " + toc.getMessage());
                            error = true;
                            break;
                        case TextOutputCallback.WARNING:
                            System.out.println("WARNING: " + toc.getMessage());
                            warning = true;
                            break;
                        default:
                            throw new UnsupportedCallbackException(toc,
                                    "Unsupported message type: "
                                            + toc.getMessageType());
                    }
                } else if (callback instanceof TextInputCallback) {
                    TextInputCallback tic = (TextInputCallback) callback;
                    System.out.println(tic.getPrompt());
                    tic.setText(CustomLoginModule.HELLO);
                } else if (callback instanceof LanguageCallback) {
                    LanguageCallback lc = (LanguageCallback) callback;
                    lc.setLocale(Locale.GERMANY);
                } else if (callback instanceof ConfirmationCallback) {
                    ConfirmationCallback cc = (ConfirmationCallback) callback;
                    System.out.println(cc.getPrompt());
                    cc.setSelectedIndex(ConfirmationCallback.YES);
                } else if (callback instanceof ChoiceCallback) {
                    ChoiceCallback cc = (ChoiceCallback) callback;
                    System.out.println(cc.getPrompt()
                            + Arrays.toString(cc.getChoices()));
                    cc.setSelectedIndex(0);
                } else if (callback instanceof NameCallback) {
                    NameCallback nc = (NameCallback) callback;
                    System.out.println(nc.getPrompt());
                    nc.setName(username);
                } else if (callback instanceof PasswordCallback) {
                    PasswordCallback pc = (PasswordCallback) callback;
                    System.out.println(pc.getPrompt());
                    pc.setPassword(password);
                } else {
                    throw new UnsupportedCallbackException(callback,
                            "Unknown callback");
                }
            }
        }

    }

}
