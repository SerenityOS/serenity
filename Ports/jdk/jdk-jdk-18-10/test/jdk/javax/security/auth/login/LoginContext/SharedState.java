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

import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

/**
 * @test
 * @bug 8048138
 * @summary Check if shared state is passed to login module
 * @run main/othervm SharedState
 */
public class SharedState {

    static final String NAME = "name";
    static final String VALUE = "shared";

    public static void main(String[] args) throws LoginException {
        System.setProperty("java.security.auth.login.config",
                System.getProperty("test.src")
                        + System.getProperty("file.separator")
                        + "shared.config");

        new LoginContext("SharedState").login();
    }

    public static abstract class Module implements LoginModule {

        @Override
        public boolean login() throws LoginException {
            return true;
        }

        @Override
        public boolean commit() throws LoginException {
            return true;
        }

        @Override
        public boolean abort() throws LoginException {
            return true;
        }

        @Override
        public boolean logout() throws LoginException {
            return true;
        }
    }

    public static class FirstModule extends Module {

        @Override
        public void initialize(Subject subject, CallbackHandler callbackHandler,
                            Map<String,?> sharedState, Map<String,?> options) {
            ((Map)sharedState).put(NAME, VALUE);
        }

    }

    public static class SecondModule extends Module {

        @Override
        public void initialize(Subject subject, CallbackHandler callbackHandler,
                            Map<String,?> sharedState, Map<String,?> options) {
            // check shared object
            Object shared = sharedState.get(NAME);
            if (!VALUE.equals(shared)) {
                throw new RuntimeException("Unexpected shared object: "
                        + shared);
            }
        }

    }
}
