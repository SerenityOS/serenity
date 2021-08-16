/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.security.auth.callback;

/* JAAS imports */
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;        // javadoc
import javax.security.auth.callback.PasswordCallback;    // javadoc
import javax.security.auth.callback.UnsupportedCallbackException;

/* Java imports */
import java.io.IOException;

import sun.security.util.ConsoleCallbackHandler;

/**
 * Prompts and reads from the command line for answers to authentication
 * questions.
 * This can be used by a JAAS application to instantiate a
 * CallbackHandler
 * @see javax.security.auth.callback
 */

public class TextCallbackHandler implements CallbackHandler {
    private final CallbackHandler consoleHandler;

    /**
     * Creates a callback handler that prompts and reads from the
     * command line for answers to authentication questions.
     * This can be used by JAAS applications to instantiate a
     * CallbackHandler.
     */
    public TextCallbackHandler() {
        this.consoleHandler = new ConsoleCallbackHandler();
    }

    /**
     * Handles the specified set of callbacks.
     *
     * @param callbacks the callbacks to handle
     * @throws IOException if an input or output error occurs.
     * @throws UnsupportedCallbackException if the callback is not an
     * instance of NameCallback or PasswordCallback
     */
    public void handle(Callback[] callbacks)
        throws IOException, UnsupportedCallbackException
    {
        // delegate to console handler
        consoleHandler.handle(callbacks);
    }
}
