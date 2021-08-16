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

import java.io.*;
import java.util.*;
import java.security.*;
import javax.security.auth.callback.*;

/**
 * @test
 * @bug 8130648
 * @summary make sure IllegalStateException is thrown for uninitialized
 * SunPKCS11 provider instance
 */
public class LoginISE {

    public static void main(String[] args) throws Exception {

        Provider p = Security.getProvider("SunPKCS11");
        if (p == null) {
            System.out.println("No un-initialized PKCS11 provider available; skip");
            return;
        }
        if (!(p instanceof AuthProvider)) {
            throw new RuntimeException("Error: expect AuthProvider!");
        }
        AuthProvider ap = (AuthProvider) p;
        if (ap.isConfigured()) {
            throw new RuntimeException("Fail: isConfigured() should return false");
        }
        try {
            ap.login(null, null);
            throw new RuntimeException("Fail: expected ISE not thrown!");
        } catch (IllegalStateException ise) {
            System.out.println("Expected ISE thrown for login call");
        }
        try {
            ap.logout();
            throw new RuntimeException("Fail: expected ISE not thrown!");
        } catch (IllegalStateException ise) {
            System.out.println("Expected ISE thrown for logout call");
        }
        try {
            ap.setCallbackHandler(new PasswordCallbackHandler());
            throw new RuntimeException("Fail: expected ISE not thrown!");
        } catch (IllegalStateException ise) {
            System.out.println("Expected ISE thrown for logout call");
        }

        System.out.println("Test Passed");
    }

    public static class PasswordCallbackHandler implements CallbackHandler {
        public void handle(Callback[] callbacks)
                throws IOException, UnsupportedCallbackException {
        }
    }
}
