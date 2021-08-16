/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8028351 8194486
 * @summary JWS doesn't get authenticated when using kerberos auth proxy
 * @library /test/lib
 * @compile -XDignore.symbol.file LoginNoPass.java
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm -Djdk.net.hosts.file=TestHosts LoginNoPass
 */

import sun.security.jgss.GSSUtil;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import java.security.Security;

public class LoginNoPass {

    static boolean kdcTouched = false;
    public static void main(String[] args) throws Exception {

        new OneKDC(null) {
            protected byte[] processAsReq(byte[] in) throws Exception {
                kdcTouched = true;
                return super.processAsReq(in);
            }
        }.writeJAASConf();
        Security.setProperty("auth.login.defaultCallbackHandler",
                "LoginNoPass$CallbackForClient");
        System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");

        try {
            Context c;
            c = Context.fromJAAS("client");
            c.startAsClient(OneKDC.SERVER, GSSUtil.GSS_KRB5_MECH_OID);
            c.take(new byte[0]);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            // OK
        }
        if (kdcTouched) {
            throw new Exception("Failed");
        }
    }
    public static class CallbackForClient implements CallbackHandler {
        public void handle(Callback[] callbacks) {
            // Do nothing
        }
    }
}
