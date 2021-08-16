/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7043847 7043860 7043882 7043938 7043959
 * @summary NTML impl of SaslServer conformance errors
 */
import java.io.IOException;
import javax.security.sasl.*;
import java.util.*;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

public class Conformance {

    public static void main(String[] args) throws Exception {
        try {
            Sasl.createSaslClient(new String[] {"NTLM"}, "abc", "ldap",
                    "server", new HashMap<String, Object>(), null);
        } catch (SaslException se) {
            System.out.println(se);
        }
        try {
            Sasl.createSaslServer("NTLM", "ldap",
                    "server", new HashMap<String, Object>(), null);
        } catch (SaslException se) {
            System.out.println(se);
        }
        try {
            Sasl.createSaslClient(new String[] {"NTLM"}, "abc", "ldap",
                    "server", null, new CallbackHandler() {
                        @Override
                        public void handle(Callback[] callbacks) throws
                                IOException, UnsupportedCallbackException {  }
                    });
        } catch (SaslException se) {
            System.out.println(se);
        }
        try {
            SaslServer saslServer =
                    Sasl.createSaslServer("NTLM", "ldap", "abc", null, new CallbackHandler() {
                        @Override
                        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {  }
                    });
            System.err.println("saslServer = " + saslServer);
            System.err.println("saslServer.isComplete() = " + saslServer.isComplete());
            // IllegalStateException is expected here
            saslServer.getNegotiatedProperty("prop");
            System.err.println("No IllegalStateException");
        } catch (IllegalStateException se) {
            System.out.println(se);
        }
        try {
            SaslServer saslServer =
                    Sasl.createSaslServer("NTLM", "ldap", "abc", null, new CallbackHandler() {
                        @Override
                        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {  }
                    });
            System.err.println("saslServer = " + saslServer);
            System.err.println("saslServer.isComplete() = " + saslServer.isComplete());
            // IllegalStateException is expected here
            saslServer.getAuthorizationID();
            System.err.println("No IllegalStateException");
        } catch (IllegalStateException se) {
            System.out.println(se);
        }
        try {
            SaslServer saslServer =
                    Sasl.createSaslServer("NTLM", "ldap", "abc", null, new CallbackHandler() {
                        @Override
                        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {  }
                    });
            System.err.println("saslServer = " + saslServer);
            System.err.println("saslServer.isComplete() = " + saslServer.isComplete());
            // IllegalStateException is expected here
            saslServer.wrap(new byte[0], 0, 0);
            System.err.println("No IllegalStateException");
        } catch (IllegalStateException se) {
            System.out.println(se);
        }
    }
}
