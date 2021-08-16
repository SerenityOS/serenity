/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6911951 7150092
 * @library /test/lib
 * @summary NTLM should be a supported Java SASL mechanism
 * @modules java.base/sun.security.util
 *          java.security.sasl
 */
import java.io.IOException;
import javax.security.sasl.*;
import javax.security.auth.callback.*;
import java.util.*;
import jdk.test.lib.hexdump.HexPrinter;

public class NTLMTest {

    private static final String MECH = "NTLM";
    private static final String REALM = "REALM";
    private static final String PROTOCOL = "jmx";
    private static final byte[] EMPTY = new byte[0];

    private static final String USER1 = "dummy";
    private static final char[] PASS1 = "bogus".toCharArray();
    private static final String USER2 = "foo";
    private static final char[] PASS2 = "bar".toCharArray();

    private static final Map<String,char[]> maps =
            new HashMap<String,char[]>();
    static {
        maps.put(USER1, PASS1);
        maps.put(USER2, PASS2);
    }

    static char[] getPass(String d, String u) {
        if (!d.equals(REALM)) return null;
        return maps.get(u);
    }

    public static void main(String[] args) throws Exception {

        checkAuthOnly();
        checkClientNameOverride();
        checkClientDomainOverride();
        checkVersions();
        checkClientHostname();
    }

    static void checkVersions() throws Exception {
        // Server accepts all version
        checkVersion(null, null);
        checkVersion("LM/NTLM", null);
        checkVersion("LM", null);
        checkVersion("NTLM", null);
        checkVersion("NTLM2", null);
        checkVersion("LMv2/NTLMv2", null);
        checkVersion("LMv2", null);
        checkVersion("NTLMv2", null);

        // Client's default version is LMv2
        checkVersion(null, "LMv2");

        // Also works if they specified identical versions
        checkVersion("LM/NTLM", "LM");
        checkVersion("LM", "LM");
        checkVersion("NTLM", "LM");
        checkVersion("NTLM2", "NTLM2");
        checkVersion("LMv2/NTLMv2", "LMv2");
        checkVersion("LMv2", "LMv2");
        checkVersion("NTLMv2", "LMv2");

        // But should not work if different
        try {
            checkVersion("LM/NTLM", "LMv2");
            throw new Exception("Should not succeed");
        } catch (SaslException se) {
            // OK
        }
        try {
            checkVersion("LMv2/NTLMv2", "LM");
            throw new Exception("Should not succeed");
        } catch (SaslException se) {
            // OK
        }

    }

    /**
     * A test on version matching
     * @param vc ntlm version specified for client
     * @param vs ntlm version specified for server
     * @throws Exception
     */
    private static void checkVersion(String vc, String vs) throws Exception {
        Map<String,Object> pc = new HashMap<>();
        pc.put("com.sun.security.sasl.ntlm.version", vc);
        Map<String,Object> ps = new HashMap<>();
        ps.put("com.sun.security.sasl.ntlm.version", vs);
        SaslClient clnt = Sasl.createSaslClient(
                new String[]{MECH}, USER1, PROTOCOL, REALM, pc,
                new CallbackHandler() {
                    public void handle(Callback[] callbacks)
                            throws IOException, UnsupportedCallbackException {
                        for (Callback cb: callbacks) {
                            if (cb instanceof PasswordCallback) {
                                ((PasswordCallback)cb).setPassword(PASS1);
                            }
                        }
                    }
                });

        SaslServer srv = Sasl.createSaslServer(MECH, PROTOCOL, REALM, ps,
                new CallbackHandler() {
                    public void handle(Callback[] callbacks)
                            throws IOException, UnsupportedCallbackException {
                        String domain = null, name = null;
                        PasswordCallback pcb = null;
                        for (Callback cb: callbacks) {
                            if (cb instanceof NameCallback) {
                                name = ((NameCallback)cb).getDefaultName();
                            } else if (cb instanceof RealmCallback) {
                                domain = ((RealmCallback)cb).getDefaultText();
                            } else if (cb instanceof PasswordCallback) {
                                pcb = (PasswordCallback)cb;
                            }
                        }
                        if (pcb != null) {
                            pcb.setPassword(getPass(domain, name));
                        }
                    }
                });

        handshake(clnt, srv);
    }

    private static void checkClientHostname() throws Exception {
        Map<String,Object> pc = new HashMap<>();
        pc.put("com.sun.security.sasl.ntlm.hostname", "this.is.com");
        SaslClient clnt = Sasl.createSaslClient(
                new String[]{MECH}, USER1, PROTOCOL, REALM, pc,
                new CallbackHandler() {
                    public void handle(Callback[] callbacks)
                            throws IOException, UnsupportedCallbackException {
                        for (Callback cb: callbacks) {
                            if (cb instanceof PasswordCallback) {
                                ((PasswordCallback)cb).setPassword(PASS1);
                            }
                        }
                    }
                });

        SaslServer srv = Sasl.createSaslServer(MECH, PROTOCOL, REALM, null,
                new CallbackHandler() {
                    public void handle(Callback[] callbacks)
                            throws IOException, UnsupportedCallbackException {
                        String domain = null, name = null;
                        PasswordCallback pcb = null;
                        for (Callback cb: callbacks) {
                            if (cb instanceof NameCallback) {
                                name = ((NameCallback)cb).getDefaultName();
                            } else if (cb instanceof RealmCallback) {
                                domain = ((RealmCallback)cb).getDefaultText();
                            } else if (cb instanceof PasswordCallback) {
                                pcb = (PasswordCallback)cb;
                            }
                        }
                        if (pcb != null) {
                            pcb.setPassword(getPass(domain, name));
                        }
                    }
                });

        handshake(clnt, srv);
        if (!"this.is.com".equals(
                srv.getNegotiatedProperty("com.sun.security.sasl.ntlm.hostname"))) {
            throw new Exception("Hostname not trasmitted to server");
        }
    }

    /**
     * Client realm override, but finally overridden by server response
     */
    private static void checkClientDomainOverride() throws Exception {
        SaslClient clnt = Sasl.createSaslClient(
                new String[]{MECH}, USER1, PROTOCOL, "ANOTHERREALM", null,
                new CallbackHandler() {
                    public void handle(Callback[] callbacks)
                            throws IOException, UnsupportedCallbackException {
                        for (Callback cb: callbacks) {
                            if (cb instanceof RealmCallback) {
                                ((RealmCallback)cb).setText(REALM);
                            } else if (cb instanceof PasswordCallback) {
                                ((PasswordCallback)cb).setPassword(PASS1);
                            }
                        }
                    }
                });

        SaslServer srv = Sasl.createSaslServer(MECH, PROTOCOL, REALM, null,
                new CallbackHandler() {
                    public void handle(Callback[] callbacks)
                            throws IOException, UnsupportedCallbackException {
                        String domain = null, name = null;
                        PasswordCallback pcb = null;
                        for (Callback cb: callbacks) {
                            if (cb instanceof NameCallback) {
                                name = ((NameCallback)cb).getDefaultName();
                            } else if (cb instanceof RealmCallback) {
                                domain = ((RealmCallback)cb).getDefaultText();
                            } else if (cb instanceof PasswordCallback) {
                                pcb = (PasswordCallback)cb;
                            }
                        }
                        if (pcb != null) {
                            pcb.setPassword(getPass(domain, name));
                        }
                    }
                });

        handshake(clnt, srv);
    }

    /**
     * Client side user name provided in callback.
     * @throws Exception
     */
    private static void checkClientNameOverride() throws Exception {
        SaslClient clnt = Sasl.createSaslClient(
                new String[]{MECH}, "someone", PROTOCOL, REALM, null,
                new CallbackHandler() {
                    public void handle(Callback[] callbacks)
                            throws IOException, UnsupportedCallbackException {
                        for (Callback cb: callbacks) {
                            if (cb instanceof NameCallback) {
                                NameCallback ncb = (NameCallback) cb;
                                ncb.setName(USER1);
                            } else if (cb instanceof PasswordCallback) {
                                ((PasswordCallback)cb).setPassword(PASS1);
                            }
                        }
                    }
                });

        SaslServer srv = Sasl.createSaslServer(MECH, PROTOCOL, "FAKE", null,
                new CallbackHandler() {
                    public void handle(Callback[] callbacks)
                            throws IOException, UnsupportedCallbackException {
                        String domain = null, name = null;
                        PasswordCallback pcb = null;
                        for (Callback cb: callbacks) {
                            if (cb instanceof NameCallback) {
                                name = ((NameCallback)cb).getDefaultName();
                            } else if (cb instanceof RealmCallback) {
                                domain = ((RealmCallback)cb).getDefaultText();
                            } else if (cb instanceof PasswordCallback) {
                                pcb = (PasswordCallback)cb;
                            }
                        }
                        if (pcb != null) {
                            pcb.setPassword(getPass(domain, name));
                        }
                    }
                });

        handshake(clnt, srv);
    }

    private static void checkAuthOnly() throws Exception {
        Map<String,Object> props = new HashMap<>();
        props.put(Sasl.QOP, "auth-conf");
        try {
            Sasl.createSaslClient(
                    new String[]{MECH}, USER2, PROTOCOL, REALM, props, null);
            throw new Exception("NTLM should not support auth-conf");
        } catch (SaslException se) {
            // Normal
        }
    }

    private static void handshake(SaslClient clnt, SaslServer srv)
            throws Exception {
        if (clnt == null) {
            throw new IllegalStateException(
                    "Unable to find client impl for " + MECH);
        }
        if (srv == null) {
            throw new IllegalStateException(
                    "Unable to find server impl for " + MECH);
        }

        byte[] response = (clnt.hasInitialResponse()
                ? clnt.evaluateChallenge(EMPTY) : EMPTY);
        System.out.println("Initial:");
        HexPrinter.simple().format(response);
        byte[] challenge;

        while (!clnt.isComplete() || !srv.isComplete()) {
            challenge = srv.evaluateResponse(response);
            response = null;
            if (challenge != null) {
                System.out.println("Challenge:");
                HexPrinter.simple().format(challenge);
                response = clnt.evaluateChallenge(challenge);
            }
            if (response != null) {
                System.out.println("Response:");
                HexPrinter.simple().format(response);
            }
        }

        if (clnt.isComplete() && srv.isComplete()) {
            System.out.println("SUCCESS");
            if (!srv.getAuthorizationID().equals(USER1)) {
                throw new Exception("Not correct user");
            }
        } else {
            throw new IllegalStateException(
                    "FAILURE: mismatched state:"
                    + " client complete? " + clnt.isComplete()
                    + " server complete? " + srv.isComplete());
        }

        if (!clnt.getNegotiatedProperty(Sasl.QOP).equals("auth") ||
                !srv.getNegotiatedProperty(Sasl.QOP).equals("auth") ||
                !clnt.getNegotiatedProperty(
                    "com.sun.security.sasl.ntlm.domain").equals(REALM)) {
            throw new Exception("Negotiated property error");
        }
        clnt.dispose();
        srv.dispose();
    }
}
