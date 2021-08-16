/*
 * Copyright (c) 2000, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @author Charlie Lai
 * @bug 4394746
 * @summary     Resources should not be loaded until necessar
 * @run main/othervm/policy=Format.policy -Djava.security.auth.login.config=file:${test.src}/Format.config Format
 *
 */

import javax.security.auth.*;
import javax.security.auth.login.*;
import javax.security.auth.callback.*;
import java.io.*;
import java.util.*;

import com.sun.security.auth.*;

public class Format
    implements java.io.Serializable, java.security.Principal
{

    public String getName() { return null; }

    public static void main(String[] args) {

        try {
            PrivateCredentialPermission pcp = new PrivateCredentialPermission
                ("test", "write");
            throw new SecurityException("test 1 failed");
        } catch (IllegalArgumentException iae) {
            // good
            System.out.println("Test 1: " + iae.getMessage());
        }

        try {
            PrivateCredentialPermission pcp = new PrivateCredentialPermission
                ("Format", "read");
            throw new SecurityException("test 2 failed");
        } catch (IllegalArgumentException iae) {
            // good
            System.out.println("Test 2: " + iae.getMessage());
        }

        try {
            PrivateCredentialPermission pcp = new PrivateCredentialPermission
                ("Format Format", "read");
            throw new SecurityException("test 3 failed");
        } catch (IllegalArgumentException iae) {
            // good
            System.out.println("Test 3: " + iae.getMessage());
        }

        try {
            PrivateCredentialPermission pcp = new PrivateCredentialPermission
                ("Format Format Format", "read");
            throw new SecurityException("test 4 failed");
        } catch (IllegalArgumentException iae) {
            // good
            System.out.println("Test 4: " + iae.getMessage());
        }

        try {
            PrivateCredentialPermission pcp = new PrivateCredentialPermission
                ("Format Format \"Format", "read");
            throw new SecurityException("test 5 failed");
        } catch (IllegalArgumentException iae) {
            // good
            System.out.println("Test 5: " + iae.getMessage());
        }

        try {
            PrivateCredentialPermission pcp = new PrivateCredentialPermission
                ("Format * \"Format\"", "read");
            throw new SecurityException("test 6 failed");
        } catch (IllegalArgumentException iae) {
            // good
            System.out.println("Test 6: " + iae.getMessage());
        }

        try {
            javax.security.auth.x500.X500Principal p =
                new javax.security.auth.x500.X500Principal((String)null);
            throw new SecurityException("test 7 failed");
        } catch (NullPointerException npe) {
            // good
            System.out.println("Test 7: " + npe.getMessage());
        }

        try {
            Subject s = new Subject(false, null, null, null);
            throw new SecurityException("test 8 failed");
        } catch (NullPointerException npe) {
            // good
            System.out.println("Test 8: " + npe.getMessage());
        }

        try {
            Subject.getSubject(null);
            throw new SecurityException("test 9 failed");
        } catch (NullPointerException npe) {
            // good
            System.out.println("Test 9: " + npe.getMessage());
        }

        try {
            Subject.doAs(null, (java.security.PrivilegedAction)null);
            throw new SecurityException("test 10 failed");
        } catch (NullPointerException npe) {
            // good
            System.out.println("Test 10: " + npe.getMessage());
        }

        try {
            Subject s = new Subject();
            s.getPrincipals(null);
            throw new SecurityException("test 11 failed");
        } catch (NullPointerException npe) {
            // good
            System.out.println("Test 11: " + npe.getMessage());
        }

        try {
            Subject s = new Subject();
            s.getPrincipals().add(new javax.security.auth.x500.X500Principal
                                        ("cn=test1"));
            s.getPublicCredentials().add(new Format());
            s.getPrivateCredentials().add(new Format());
            System.out.println("Test 12, s.toString() = " + s.toString());
        } catch (Exception e) {
            throw new SecurityException("test 12 failed: e.toString()", e);
        }

        try {
            Subject s = new Subject();
            s.getPrincipals().add(new javax.security.auth.x500.X500Principal
                                        ("cn=test1"));
            s.setReadOnly();
            java.util.Iterator i = s.getPrincipals().iterator();
            i.next();
            i.remove();
            throw new SecurityException("test 13 failed");
        } catch (IllegalStateException ise) {
            // good
            System.out.println("Test 13: " + ise.getMessage());
        }

        try {
            Subject s = new Subject();
            s.getPrincipals().add(new Format());
            throw new SecurityException("test 14 failed");
        } catch (SecurityException se) {
            // good
            System.out.println("Test 14: " + se.getMessage());
        }

        try {
            Subject s = new Subject();
            s.getPrincipals((Class)Format.class).add(new Subject());
            throw new SecurityException("test 15 failed");
        } catch (SecurityException se) {
            // good
            System.out.println("Test 15: " + se.getMessage());
        }

        try {
            LoginContext lc = new LoginContext(null);
            throw new SecurityException("test 16 failed");
        } catch (LoginException le) {
            // good
            System.out.println("Test 16: " + le.getMessage());
        }

        try {
            LoginContext lc = new LoginContext("nothing");
            throw new SecurityException("test 17 failed");
        } catch (LoginException le) {
            // good
            System.out.println("Test 17: " + le.getMessage());
        }

        try {
            LoginContext lc = new LoginContext("test", (Subject)null);
            throw new SecurityException("test 18 failed");
        } catch (LoginException le) {
            // good
            System.out.println("Test 18: " + le.getMessage());
        }

        try {
            LoginContext lc = new LoginContext("test", (CallbackHandler)null);
            throw new SecurityException("test 19 failed");
        } catch (LoginException le) {
            // good
            System.out.println("Test 19: " + le.getMessage());
        }

        try {
            LoginContext lc = new LoginContext("test");
            lc.logout();
            throw new SecurityException("test 20 failed");
        } catch (LoginException le) {
            // good
            System.out.println("Test 20: " + le.getMessage());
        }

        try {
            LoginContext lc = new LoginContext("test2");
            lc.login();
            throw new SecurityException("test 21 failed");
        } catch (LoginException le) {
            // good
            System.out.println("Test 21: " + le.getMessage());
        }

        /***    TESTS FOR THE COM PACKAGE RESOURCES     ***/

        try {
            NTDomainPrincipal p = new NTDomainPrincipal(null);
            throw new SecurityException("test 22 failed");
        } catch (NullPointerException e) {
            // good
            System.out.println("Test 22: " + e.getMessage());
        }

        try {
            NTDomainPrincipal p = new NTDomainPrincipal("test");
            System.out.println("NTDomainPrincipal = " + p.toString());
        } catch (Exception e) {
            throw new SecurityException("test 23 failed");
        }

        try {
            NTNumericCredential p = new NTNumericCredential(1L);
            System.out.println("NTNumericCredential = " + p.toString());
        } catch (Exception e) {
            throw new SecurityException("test 24 failed");
        }

        try {
            NTSid p = new NTSid("");
            throw new SecurityException("test 25 failed");
        } catch (IllegalArgumentException e) {
            // good
            System.out.println("Test 25: " + e.getMessage());
        }

        try {
            NTSid p = new NTSid("test");
            System.out.println("NTSid = " + p.toString());
        } catch (Exception e) {
            throw new SecurityException("test 26 failed");
        }

        try {
            NTSidDomainPrincipal p = new NTSidDomainPrincipal("test");
            System.out.println("NTSidDomainPrincipal = " + p.toString());
        } catch (Exception e) {
            throw new SecurityException("test 27 failed");
        }

        try {
            NTSidGroupPrincipal p = new NTSidGroupPrincipal("test");
            System.out.println("NTSidGroupPrincipal = " + p.toString());
        } catch (Exception e) {
            throw new SecurityException("test 28 failed");
        }

        try {
            NTSidPrimaryGroupPrincipal p =
                                new NTSidPrimaryGroupPrincipal("test");
            System.out.println("NTSidPrimaryGroupPrincipal = " + p.toString());
        } catch (Exception e) {
            throw new SecurityException("test 29 failed");
        }

        try {
            NTSidUserPrincipal p = new NTSidUserPrincipal("test");
            System.out.println("NTSidUserPrincipal = " + p.toString());
        } catch (Exception e) {
            throw new SecurityException("test 30 failed");
        }

        try {
            NTUserPrincipal p = new NTUserPrincipal("test");
            System.out.println("NTUserPrincipal = " + p.toString());
        } catch (Exception e) {
            throw new SecurityException("test 31 failed");
        }

        try {
            UnixNumericGroupPrincipal p = new UnixNumericGroupPrincipal("0",
                                                                true);
            System.out.println("NTPrimaryGroupPrincipal = " + p.toString());
        } catch (Exception e) {
            throw new SecurityException("test 32 failed");
        }

        try {
            UnixNumericGroupPrincipal p = new UnixNumericGroupPrincipal("1",
                                                                false);
            System.out.println("UnixNumericGroupPrincipal = " + p.toString());
        } catch (Exception e) {
            throw new SecurityException("test 33 failed");
        }

        try {
            UnixNumericUserPrincipal p = new UnixNumericUserPrincipal("2");
            System.out.println("UnixNumericUserPrincipal = " + p.toString());
        } catch (Exception e) {
            throw new SecurityException("test 34 failed");
        }

        try {
            UnixPrincipal p = new UnixPrincipal("test");
            System.out.println("UnixPrincipal = " + p.toString());
        } catch (Exception e) {
            throw new SecurityException("test 35 failed");
        }

        System.out.println("Format test succeeded");
    }
}
