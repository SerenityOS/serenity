/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8009977 8186884 8194486 8201627
 * @summary A test to launch multiple Java processes using either Java GSS
 *          or native GSS
 * @library /test/lib
 * @compile -XDignore.symbol.file BasicProc.java
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm -Djdk.net.hosts.file=TestHosts BasicProc launcher
 */

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.PropertyPermission;
import java.util.Set;

import jdk.test.lib.Asserts;
import jdk.test.lib.Platform;
import jdk.test.lib.process.Proc;
import org.ietf.jgss.Oid;
import sun.security.krb5.Config;

import javax.security.auth.PrivateCredentialPermission;

/**
 * Run this test automatically and test Java GSS with embedded KDC.
 *
 * Run with customized native.krb5.libs to test interop between Java GSS
 * and native GSS, and native.kdc.path with a native KDC. For example,
 * run the following command to test interop among Java, default native,
 * MIT, and Heimdal krb5 libraries with the Heimdal KDC:
 *
 *    jtreg -Dnative.krb5.libs=j=,
 *                             n=,
 *                             k=/usr/local/krb5/lib/libgssapi_krb5.so,
 *                             h=/space/install/heimdal/lib/libgssapi.so \
 *          -Dnative.kdc.path=/usr/local/heimdal \
 *          BasicProc.java
 *
 * Note: The first 4 lines should be concatenated to make a long system
 * property value with no blank around ",". This comma-separated value
 * has each element being name=libpath. The special name "j" means the
 * Java library and libpath is ignored. Otherwise it means a native library,
 * and libpath (can be empty) will be the value for the sun.security.jgss.lib
 * system property. If this system property is not set, only the Java
 * library will be tested.
 */

public class BasicProc {

    private static final String CONF = "krb5.conf";
    private static final String KTAB_S = "server.ktab";
    private static final String KTAB_B = "backend.ktab";

    private static final String HOST = "localhost";
    private static final String SERVER = "server/" + HOST;
    private static final String BACKEND = "backend/" + HOST;
    private static final String USER = "user";
    private static final char[] PASS = "password".toCharArray();
    private static final String REALM = "REALM";

    private static final int MSGSIZE = 1024;
    private static final byte[] MSG = new byte[MSGSIZE];

    public static void main(String[] args) throws Exception {

        Oid oid = new Oid("1.2.840.113554.1.2.2");
        byte[] token, msg;

        switch (args[0]) {
            case "launcher":
                KDC kdc = KDC.create(REALM, HOST, 0, true);
                try {
                    kdc.addPrincipal(USER, PASS);
                    kdc.addPrincipalRandKey("krbtgt/" + REALM);
                    kdc.addPrincipalRandKey(SERVER);
                    kdc.addPrincipalRandKey(BACKEND);

                    // Native lib might do some name lookup
                    KDC.saveConfig(CONF, kdc,
                            "dns_lookup_kdc = no",
                            "ticket_lifetime = 1h",
                            "dns_lookup_realm = no",
                            "dns_canonicalize_hostname = false",
                            "forwardable = true");
                    System.setProperty("java.security.krb5.conf", CONF);
                    Config.refresh();
                    kdc.writeKtab(KTAB_S, false, SERVER);
                    kdc.writeKtab(KTAB_B, false, BACKEND);

                    String[] tmp = System.getProperty("native.krb5.libs", "j=")
                            .split(",");

                    // Library paths. The 1st one is always null which means
                    // Java, "" means the default native lib.
                    String[] libs = new String[tmp.length];

                    // Names for each lib above. Use in file names.
                    String[] names = new String[tmp.length];

                    boolean hasNative = false;

                    for (int i = 0; i < tmp.length; i++) {
                        if (tmp[i].isEmpty()) {
                            throw new Exception("Invalid native.krb5.libs");
                        }
                        String[] pair = tmp[i].split("=", 2);
                        names[i] = pair[0];
                        if (!pair[0].equals("j")) {
                            libs[i] = pair.length > 1 ? pair[1] : "";
                            hasNative = true;
                        }
                    }

                    if (hasNative) {
                        kdc.kinit(USER, "base.ccache");
                    }

                    // Try the same lib first
                    for (int i = 0; i < libs.length; i++) {
                        once(names[i] + names[i] + names[i],
                                libs[i], libs[i], libs[i]);
                    }

                    for (int i = 0; i < libs.length; i++) {
                        for (int j = 0; j < libs.length; j++) {
                            for (int k = 0; k < libs.length; k++) {
                                if (i != j || i != k) {
                                    once(names[i] + names[j] + names[k],
                                            libs[i], libs[j], libs[k]);
                                }
                            }
                        }
                    }
                } finally {
                    kdc.terminate();
                }
                break;
            case "client":
                Context c = args[1].equals("n") ?
                        Context.fromThinAir() :
                        Context.fromUserPass(USER, PASS, false);
                c.startAsClient(SERVER, oid);
                c.x().requestCredDeleg(true);
                c.x().requestMutualAuth(true);
                Proc.binOut(c.take(new byte[0])); // AP-REQ
                c.take(Proc.binIn()); // AP-REP
                Proc.binOut(c.wrap(MSG, true));
                Proc.binOut(c.getMic(MSG));
                break;
            case "server":
                Context s = args[1].equals("n") ?
                        Context.fromThinAir() :
                        Context.fromUserKtab(SERVER, KTAB_S, true);
                s.startAsServer(oid);
                token = Proc.binIn(); // AP-REQ
                Proc.binOut(s.take(token)); // AP-REP
                msg = s.unwrap(Proc.binIn(), true);
                Asserts.assertTrue(Arrays.equals(msg, MSG));
                s.verifyMic(Proc.binIn(), msg);
                Context s2 = s.delegated();
                s2.startAsClient(BACKEND, oid);
                s2.x().requestMutualAuth(false);
                Proc.binOut(s2.take(new byte[0])); // AP-REQ
                msg = s2.unwrap(Proc.binIn(), true);
                Asserts.assertTrue(Arrays.equals(msg, MSG));
                s2.verifyMic(Proc.binIn(), msg);
                break;
            case "backend":
                Context b = args[1].equals("n") ?
                        Context.fromThinAir() :
                        Context.fromUserKtab(BACKEND, KTAB_B, true);
                b.startAsServer(oid);
                token = b.take(Proc.binIn()); // AP-REQ
                Asserts.assertTrue(token == null);
                Proc.binOut(b.wrap(MSG, true));
                Proc.binOut(b.getMic(MSG));
                break;
        }
    }

    /**
     * One test run.
     *
     * @param label test label
     * @param lc lib of client
     * @param ls lib of server
     * @param lb lib of backend
     */
    private static void once(String label, String lc, String ls, String lb)
            throws Exception {

        Proc pc = proc(lc)
                .args("client", lc == null ? "j" : "n")
                .perm(new javax.security.auth.kerberos.ServicePermission(
                        "krbtgt/" + REALM + "@" + REALM, "initiate"))
                .perm(new javax.security.auth.kerberos.ServicePermission(
                        SERVER + "@" + REALM, "initiate"))
                .perm(new javax.security.auth.kerberos.DelegationPermission(
                        "\"" + SERVER + "@" + REALM + "\" " +
                                "\"krbtgt/" + REALM + "@" + REALM + "\""))
                .debug(label + "-C");
        if (lc == null) {
            // for Krb5LoginModule::promptForName
            pc.perm(new PropertyPermission("user.name", "read"));
        } else {
            Files.copy(Paths.get("base.ccache"), Paths.get(label + ".ccache"));
            if (!Platform.isWindows()) {
                Files.setPosixFilePermissions(Paths.get(label + ".ccache"),
                        Set.of(PosixFilePermission.OWNER_READ,
                                PosixFilePermission.OWNER_WRITE));
            }
            pc.env("KRB5CCNAME", "FILE:" + label + ".ccache");
            // Do not try system ktab if ccache fails
            pc.env("KRB5_KTNAME", "none");
        }
        pc.start();

        Proc ps = proc(ls)
                .args("server", ls == null ? "j" : "n")
                .perm(new javax.security.auth.kerberos.ServicePermission(
                        SERVER + "@" + REALM, "accept"))
                .perm(new javax.security.auth.kerberos.ServicePermission(
                        BACKEND + "@" + REALM, "initiate"))
                .debug(label + "-S");
        if (ls == null) {
            ps.perm(new PrivateCredentialPermission(
                    "javax.security.auth.kerberos.KeyTab * \"*\"", "read"))
                .perm(new java.io.FilePermission(KTAB_S, "read"));
        } else {
            ps.env("KRB5_KTNAME", KTAB_S);
        }
        ps.start();

        Proc pb = proc(lb)
                .args("backend", lb == null ? "j" : "n")
                .perm(new javax.security.auth.kerberos.ServicePermission(
                        BACKEND + "@" + REALM, "accept"))
                .debug(label + "-B");
        if (lb == null) {
            pb.perm(new PrivateCredentialPermission(
                    "javax.security.auth.kerberos.KeyTab * \"*\"", "read"))
                .perm(new java.io.FilePermission(KTAB_B, "read"));
        } else {
            pb.env("KRB5_KTNAME", KTAB_B);
        }
        pb.start();

        // Client and server
        ps.println(pc.readData()); // AP-REQ
        pc.println(ps.readData()); // AP-REP

        ps.println(pc.readData()); // KRB-PRIV
        ps.println(pc.readData()); // KRB-SAFE

        // Server and backend
        pb.println(ps.readData()); // AP-REQ

        ps.println(pb.readData()); // KRB-PRIV
        ps.println(pb.readData()); // KRB-SAFE

        if ((pc.waitFor() | ps.waitFor() | pb.waitFor()) != 0) {
            throw new Exception("Process failed");
        }
    }

    /**
     * A Proc for a child process.
     *
     * @param lib the library. Null is Java. "" is default native lib.
     */
    private static Proc proc(String lib) throws Exception {
        Proc p = Proc.create("BasicProc")
                .inheritProp("jdk.net.hosts.file")
                .prop("java.security.manager", "")
                .perm(new javax.security.auth.AuthPermission("doAs"));
        if (lib != null) {
            p.env("KRB5_CONFIG", CONF)
                    .env("KRB5_TRACE", Platform.isWindows() ? "CON" : "/dev/stderr")
                    .prop("sun.security.jgss.native", "true")
                    .prop("sun.security.jgss.lib", lib)
                    .prop("javax.security.auth.useSubjectCredsOnly", "false")
                    .prop("sun.security.nativegss.debug", "true");
            int pos = lib.lastIndexOf('/');
            if (pos > 0) {
                p.env(Platform.sharedLibraryPathVariableName(), lib.substring(0, pos));
            }
        } else {
            p.perm(new java.util.PropertyPermission(
                            "sun.security.krb5.principal", "read"))
                            // For Krb5LoginModule::login.
                    .perm(new javax.security.auth.AuthPermission(
                            "modifyPrincipals"))
                    .perm(new javax.security.auth.AuthPermission(
                            "modifyPrivateCredentials"))
                    .prop("sun.security.krb5.debug", "true")
                    .prop("java.security.krb5.conf", CONF);
        }
        return p;
    }
}
