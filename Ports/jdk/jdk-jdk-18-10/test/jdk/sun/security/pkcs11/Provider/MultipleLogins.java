/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import sun.security.pkcs11.SunPKCS11;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.*;
import java.util.Iterator;
import java.util.PropertyPermission;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import jdk.test.lib.util.ForceGC;

public class MultipleLogins {
    private static final String KS_TYPE = "PKCS11";
    private static final int NUM_PROVIDERS = 20;
    private static final SunPKCS11[] providers = new SunPKCS11[NUM_PROVIDERS];
    static final Policy DEFAULT_POLICY = Policy.getPolicy();

    public static void main(String[] args) throws Exception {
        String nssConfig = PKCS11Test.getNssConfig();
        if (nssConfig == null) {
            // No test framework support yet. Ignore
            System.out.println("No NSS config found. Skipping.");
            return;
        }

        for (int i =0; i < NUM_PROVIDERS; i++) {
            // loop to set up test without security manger
            providers[i] = (SunPKCS11)PKCS11Test.newPKCS11Provider();
        }

        if (args.length > 0) {
            Policy.setPolicy(new SimplePolicy());
            System.setSecurityManager(new SecurityManager());
        }

        for (int i =0; i < NUM_PROVIDERS; i++) {
            providers[i] = (SunPKCS11)providers[i].configure(nssConfig);
            Security.addProvider(providers[i]);
            test(providers[i]);
        }

        WeakReference<SunPKCS11>[] weakRef = new WeakReference[NUM_PROVIDERS];
        for (int i =0; i < NUM_PROVIDERS; i++) {
            weakRef[i] = new WeakReference<>(providers[i]);
            providers[i].logout();

            if (i == 0) {
                // one provider stays for use with clean up thread
                continue;
            }

            try {
                providers[i].login(new Subject(), new PasswordCallbackHandler());
                throw new RuntimeException("Expected LoginException");
            } catch (LoginException le) {
                // expected
            }

            Security.removeProvider(providers[i].getName());
            providers[i] = null;

            ForceGC gc = new ForceGC();
            int finalI = i;
            gc.await(() -> weakRef[finalI].get() == null);
            if (!weakRef[i].refersTo(null)) {
                throw new RuntimeException("Expected SunPKCS11 Provider to be GC'ed..");
            }
        }
    }

    private static void test(SunPKCS11 p) throws Exception {
        KeyStore ks = KeyStore.getInstance(KS_TYPE, p);
        p.setCallbackHandler(new PasswordCallbackHandler());
        try {
            ks.load(null, (char[]) null);
        } catch (IOException e) {
            if (!e.getMessage().contains("load failed")) {
                // we expect the keystore load to fail
                throw new RuntimeException("unexpected exception", e);
            }
        }

        p.logout();

        try {
            ks.load(null, (char[]) null);
        } catch (IOException e) {
            if (e.getCause() instanceof LoginException &&
                    e.getCause().getMessage().contains("No token present")) {
                // expected
            } else {
                throw new RuntimeException("Token was present", e);
            }
        }
    }

    static final class SimplePolicy extends Policy {

        final Permissions perms = new Permissions();
        SimplePolicy() {
            perms.add(new PropertyPermission("*", "read, write"));
            perms.add(new SecurityPermission("authProvider.*"));
            perms.add(new SecurityPermission("insertProvider.*"));
            perms.add(new SecurityPermission("removeProvider.*"));
        }

        @Override
        public boolean implies(ProtectionDomain domain, Permission permission) {
            return perms.implies(permission) ||
                    DEFAULT_POLICY.implies(domain, permission);
        }
    }

    public static class PasswordCallbackHandler implements CallbackHandler {
        public void handle(Callback[] callbacks)
                throws IOException, UnsupportedCallbackException {
            if (!(callbacks[0] instanceof PasswordCallback)) {
                throw new UnsupportedCallbackException(callbacks[0]);
            }
            PasswordCallback pc = (PasswordCallback)callbacks[0];
            pc.setPassword(null);
        }
    }
}
