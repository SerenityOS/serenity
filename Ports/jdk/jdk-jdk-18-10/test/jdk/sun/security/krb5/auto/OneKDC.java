/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Security;
import java.util.Locale;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import sun.security.krb5.Config;

/**
 * This class starts a simple KDC with one realm, several typical principal
 * names, generates delete-on-exit krb5.conf and keytab files, and setup
 * system properties for them. There's also a helper method to generate a
 * JAAS login config file that can be used for JAAS or JGSS apps.
 * <p>
 * Just call this line to start everything:
 * <pre>
 * new OneKDC(null).writeJaasConf();
 * </pre>
 */
public class OneKDC extends KDC {

    public static final String USER = "dummy";
    public static final char[] PASS = "bogus".toCharArray();
    public static final String USER2 = "foo";
    public static final char[] PASS2 = "bar".toCharArray();
    public static final String KRB5_CONF = "localkdc-krb5.conf";
    public static final String KTAB = "localkdc.ktab";
    public static final String JAAS_CONF = "localkdc-jaas.conf";
    public static final String REALM = "RABBIT.HOLE";
    public static final String REALM_LOWER_CASE = REALM.toLowerCase(Locale.US);
    public static String SERVER = "server/host." + REALM_LOWER_CASE;
    public static String BACKEND = "backend/host." + REALM_LOWER_CASE;
    public static String KDCHOST = "kdc." + REALM_LOWER_CASE;
    /**
     * Creates the KDC and starts it.
     * @param etype Encryption type, null if not specified
     * @throws java.lang.Exception if there's anything wrong
     */
    public OneKDC(String etype) throws Exception {
        super(REALM, KDCHOST, 0, true);
        addPrincipal(USER, PASS);
        addPrincipal(USER2, PASS2);
        addPrincipalRandKey("krbtgt/" + REALM);
        addPrincipalRandKey(SERVER);
        addPrincipalRandKey(BACKEND);

        String extraConfig = "";
        if (etype != null) {
            extraConfig += "default_tkt_enctypes=" + etype
                    + "\ndefault_tgs_enctypes=" + etype;
            if (etype.startsWith("des")) {
                extraConfig += "\nallow_weak_crypto = true";
            }
        }
        KDC.saveConfig(KRB5_CONF, this,
                "forwardable = true",
                "default_keytab_name = " + KTAB,
                extraConfig);
        System.setProperty("java.security.krb5.conf", KRB5_CONF);
        // Whatever krb5.conf had been loaded before, we reload ours now.
        Config.refresh();

        writeKtab(KTAB);
        Security.setProperty("auth.login.defaultCallbackHandler",
                "OneKDC$CallbackForClient");
    }

    /**
     * Writes a JAAS login config file, which contains as many as useful
     * entries, including JGSS style initiator/acceptor and normal JAAS
     * entries with names using existing OneKDC principals.
     * @throws java.lang.Exception if anything goes wrong
     */
    public OneKDC writeJAASConf() throws IOException {
        System.setProperty("java.security.auth.login.config", JAAS_CONF);
        File f = new File(JAAS_CONF);
        FileOutputStream fos = new FileOutputStream(f);
        fos.write((
                "com.sun.security.jgss.krb5.initiate {\n" +
                "    com.sun.security.auth.module.Krb5LoginModule required;\n};\n" +
                "com.sun.security.jgss.krb5.accept {\n" +
                "    com.sun.security.auth.module.Krb5LoginModule required\n" +
                "    principal=\"*\"\n" +
                "    useKeyTab=true\n" +
                "    isInitiator=false\n" +
                "    storeKey=true;\n};\n" +
                "client {\n" +
                "    com.sun.security.auth.module.Krb5LoginModule required;\n};\n" +
                "server {\n" +
                "    com.sun.security.auth.module.Krb5LoginModule required\n" +
                "    principal=\"" + SERVER + "\"\n" +
                "    useKeyTab=true\n" +
                "    storeKey=true;\n};\n" +
                "backend {\n" +
                "    com.sun.security.auth.module.Krb5LoginModule required\n" +
                "    principal=\"" + BACKEND + "\"\n" +
                "    useKeyTab=true\n" +
                "    storeKey=true\n" +
                "    isInitiator=false;\n};\n"
                ).getBytes());
        fos.close();
        return this;
    }

    /**
     * The default callback handler for JAAS login. Note that this handler is
     * hard coded to provide only info for USER1. If you need to provide info
     * for another principal, please use Context.fromUserPass() instead.
     */
    public static class CallbackForClient implements CallbackHandler {
        public void handle(Callback[] callbacks) {
            String user = OneKDC.USER;
            char[] pass = OneKDC.PASS;
            for (Callback callback : callbacks) {
                if (callback instanceof NameCallback) {
                    System.out.println("Callback for name: " + user);
                    ((NameCallback) callback).setName(user);
                }
                if (callback instanceof PasswordCallback) {
                    System.out.println("Callback for pass: "
                            + new String(pass));
                    ((PasswordCallback) callback).setPassword(pass);
                }
            }
        }
    }
}
