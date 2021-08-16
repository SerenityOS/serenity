/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.security.auth.RefreshFailedException;
import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosTicket;
import javax.security.auth.login.LoginContext;

/*
 * @test
 * @bug 6857795 8075299 8194486
 * @summary Checks Kerberos ticket properties
 * @library /test/lib
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm -Djdk.net.hosts.file=TestHosts KrbTicket
 */
public class KrbTicket {

    private static final String REALM = "TEST.REALM";
    private static final String HOST = "localhost";
    private static final String USER = "TESTER";
    private static final String USER_PRINCIPAL = USER + "@" + REALM;
    private static final String PASSWORD = "password";
    private static final String KRBTGT_PRINCIPAL = "krbtgt/" + REALM;
    private static final String KRB5_CONF_FILENAME = "krb5.conf";
    private static final String JAAS_CONF = "jaas.conf";
    private static final long TICKET_LIFTETIME = 5 * 60 * 1000; // 5 mins

    public static void main(String[] args) throws Exception {
        // define principals
        Map<String, String> principals = new HashMap<>();
        principals.put(USER_PRINCIPAL, PASSWORD);
        principals.put(KRBTGT_PRINCIPAL, null);

        System.setProperty("java.security.krb5.conf", KRB5_CONF_FILENAME);

        // start a local KDC instance
        KDC kdc = KDC.startKDC(HOST, null, REALM, principals, null, null);
        KDC.saveConfig(KRB5_CONF_FILENAME, kdc,
                "forwardable = true", "proxiable = true");

        // create JAAS config
        Files.write(Paths.get(JAAS_CONF), Arrays.asList(
                "Client {",
                "    com.sun.security.auth.module.Krb5LoginModule required;",
                "};"
        ));
        System.setProperty("java.security.auth.login.config", JAAS_CONF);
        System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");

        long startTime = Instant.now().getEpochSecond() * 1000;

        LoginContext lc = new LoginContext("Client",
                new Helper.UserPasswordHandler(USER, PASSWORD));
        lc.login();

        Subject subject = lc.getSubject();
        System.out.println("subject: " + subject);

        Set creds = subject.getPrivateCredentials(
                KerberosTicket.class);

        if (creds.size() > 1) {
            throw new RuntimeException("Multiple credintials found");
        }

        Object o = creds.iterator().next();
        if (!(o instanceof KerberosTicket)) {
            throw new RuntimeException("Instance of KerberosTicket expected");
        }
        KerberosTicket krbTkt = (KerberosTicket) o;

        System.out.println("forwardable = " + krbTkt.isForwardable());
        System.out.println("proxiable   = " + krbTkt.isProxiable());
        System.out.println("renewable   = " + krbTkt.isRenewable());
        System.out.println("current     = " + krbTkt.isCurrent());

        if (!krbTkt.isForwardable()) {
            throw new RuntimeException("Forwardable ticket expected");
        }

        if (!krbTkt.isProxiable()) {
            throw new RuntimeException("Proxiable ticket expected");
        }

        if (!krbTkt.isCurrent()) {
            throw new RuntimeException("Ticket is not current");
        }

        if (krbTkt.isRenewable()) {
            throw new RuntimeException("Not renewable ticket expected");
        }
        try {
            krbTkt.refresh();
            throw new RuntimeException(
                    "Expected RefreshFailedException not thrown");
        } catch(RefreshFailedException e) {
            System.out.println("Expected exception: " + e);
        }

        if (!checkTime(krbTkt, startTime)) {
            throw new RuntimeException("Wrong ticket life time");
        }

        krbTkt.destroy();
        if (!krbTkt.isDestroyed()) {
            throw new RuntimeException("Ticket not destroyed");
        }

        System.out.println("Test passed");
    }

    private static boolean checkTime(KerberosTicket krbTkt, long startTime) {
        long ticketEndTime = krbTkt.getEndTime().getTime();
        long roughLifeTime = ticketEndTime - startTime;
        System.out.println("start time            = " + startTime);
        System.out.println("end time              = " + ticketEndTime);
        System.out.println("rough life time       = " + roughLifeTime);
        return roughLifeTime >= TICKET_LIFTETIME;
    }
}
