/*
 * Copyright (c) 2004, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5037004
 * @summary Frivolous ClassCastExceptions thrown by SubjectCodeSource.implies
 * @modules java.base/sun.security.provider
 * @run main/othervm Comparator
 *
 * Note:  if you want to see the java.security.debug output,
 *        you can not simply set the system property.
 *        you must run this test by hand and pass -Djava.security.debug=...
 */

import java.io.*;
import java.security.*;
import java.util.PropertyPermission;
import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;

import sun.security.provider.PolicyFile;
import com.sun.security.auth.UnixPrincipal;
import com.sun.security.auth.NTUserPrincipal;

public class Comparator {

    private static final PropertyPermission FOO =
                new PropertyPermission("foo", "read");
    private static final PropertyPermission BAR =
                new PropertyPermission("bar", "read");
    private static final PropertyPermission FOOBAR =
                new PropertyPermission("foobar", "read");
    private static final PropertyPermission HELLO =
                new PropertyPermission("hello", "read");
    private static final PropertyPermission WORLD =
                new PropertyPermission("world", "read");

    private static final CodeSource cs =
                new CodeSource(null, (java.security.cert.Certificate[])null);

    private static final Principal[] p1 = new Principal[] {
                                new UnixPrincipal("1") };

    private static final Principal[] p2 = new Principal[] {
                                new X500Principal("cn=2"),
                                new NTUserPrincipal("2") };

    private static final Principal[] p3 = new Principal[] {
                                new UnixPrincipal("1"),
                                new X500Principal("cn=2"),
                                new NTUserPrincipal("2") };

    private static final Principal[] p4 = new Principal[] {
                                new UnixPrincipal("1"),
                                new NTUserPrincipal("4") };

    private static final Principal[] p5 = new Principal[] {
                                new UnixPrincipal("1"),
                                new X500Principal("cn=2"),
                                new NTUserPrincipal("2"),
                                new X500Principal("cn=x500") };

    private static final Principal[] p6 = new Principal[] {
                                new UnixPrincipal("1"),
                                new NTUserPrincipal("4"),
                                new X500Principal("cn=x500") };

    private static final Principal[] badP = new Principal[] {
                                new UnixPrincipal("bad") };

    public static class PCompare1 implements Principal {

        private String name;

        public PCompare1(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean implies (Subject subject) {
            if (subject.getPrincipals().contains(p1[0])) {
                return true;
            }
            return false;
        }
    }

    public static class PCompare2 implements Principal {
        private String name;

        public PCompare2(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean implies (Subject subject) {
            if (subject.getPrincipals().contains(p2[0]) &&
                subject.getPrincipals().contains(p2[1])) {
                return true;
            }
            return false;
        }
    }

    public static class PCompare3 implements Principal {
        private String name;

        public PCompare3(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean implies (Subject subject) {
            return false;
        }
    }

    public static void main(String[] args) throws Exception {

        int testnum = 1;

        // in case we run standalone
        String policyDir = System.getProperty("test.src");
        if (policyDir == null) {
            policyDir = ".";
        }

        // do principal-only tests
        System.setProperty("java.security.policy",
                        "=" +
                        policyDir +
                        File.separatorChar +
                        "Comparator.Principal.Policy");
        PolicyFile policy = new PolicyFile();
        testnum = doPrincipalTest(policy, testnum);
        System.out.println("============ Principal Test Passed ============");

        // do comparator-only tests
        System.setProperty("java.security.policy",
                        "=" +
                        policyDir +
                        File.separatorChar +
                        "Comparator.Comparator.Policy");
        policy = new PolicyFile();
        testnum = doComparatorTest(policy, testnum);
        System.out.println("============ Comparator Test Passed ============");

        // combined principal/comparator tests
        System.setProperty("java.security.policy",
                        "=" +
                        policyDir +
                        File.separatorChar +
                        "Comparator.Combined.Policy");
        policy = new PolicyFile();
        testnum = doCombinedTest(policy, testnum);
        System.out.println("============ Combined Test Passed ============");
    }

    private static int doBadTest(PolicyFile policy, int testnum) {

        // this principal is not in policy - should not match any policy grants
        ProtectionDomain pd = new ProtectionDomain(cs, null, null, badP);
        if (policy.implies(pd, FOO)) {
            throw new SecurityException("test." + testnum + " failed");
        }
        testnum++;

        // this principal is not in policy - should not match any policy grants
        if (policy.implies(pd, BAR)) {
            throw new SecurityException("test." + testnum + " failed");
        }
        testnum++;

        // this principal is not in policy - should not match any policy grants
        if (policy.implies(pd, FOOBAR)) {
            throw new SecurityException("test." + testnum + " failed");
        }
        testnum++;

        return testnum;
    }

    private static int doPrincipalTest(PolicyFile policy, int testnum) {

        // security check against one principal should pass
        ProtectionDomain pd = new ProtectionDomain(cs, null, null, p1);
        if (!policy.implies(pd, FOO)) {
            throw new SecurityException("test." + testnum + " failed");
        }
        testnum++;

        // should not match BAR grant entry in policy
        pd = new ProtectionDomain(cs, null, null, p1);
        if (policy.implies(pd, BAR)) {
            throw new SecurityException("test." + testnum + " failed");
        }
        testnum++;

        // security check against two principals should pass
        pd = new ProtectionDomain(cs, null, null, p2);
        if (!policy.implies(pd, BAR)) {
            throw new SecurityException("test." + testnum + " failed");
        }
        testnum++;

        // should not match FOOBAR grant entry in policy
        pd = new ProtectionDomain(cs, null, null, p1);
        if (policy.implies(pd, FOOBAR)) {
            throw new SecurityException("test." + testnum + " failed");
        }
        testnum++;

        // should not match FOOBAR grant entry in policy
        pd = new ProtectionDomain(cs, null, null, p2);
        if (policy.implies(pd, FOOBAR)) {
            throw new SecurityException("test." + testnum + " failed");
        }
        testnum++;

        testnum = doBadTest(policy, testnum);

        return testnum;
    }

    private static int doComparatorTest(PolicyFile policy, int testnum) {

        // security check against one comparator should pass
        ProtectionDomain pd = new ProtectionDomain(cs, null, null, p1);
        if (!policy.implies(pd, FOO)) {
            throw new SecurityException("test." + testnum + " failed");
        }
        testnum++;

        // should not match BAR grant entry in policy
        pd = new ProtectionDomain(cs, null, null, p1);
        if (policy.implies(pd, BAR)) {
            throw new SecurityException("test." + testnum + " failed");
        }
        testnum++;

        // security check against two comparators should pass for FOO
        pd = new ProtectionDomain(cs, null, null, p3);
        if (!policy.implies(pd, FOO)) {
            throw new SecurityException("test." + testnum + " failed");
        }
        testnum++;

        // security check against two comparators should pass for BAR
        pd = new ProtectionDomain(cs, null, null, p3);
        if (!policy.implies(pd, BAR)) {
            throw new SecurityException("test." + testnum + " failed");
        }
        testnum++;

        // security check should fail against FOOBAR
        pd = new ProtectionDomain(cs, null, null, p3);
        if (policy.implies(pd, FOOBAR)) {
            throw new SecurityException("test." + testnum + " failed");
        }
        testnum++;

        testnum = doBadTest(policy, testnum);

        return testnum;
    }

    private static int doCombinedTest(PolicyFile policy, int testnum) {

        // security check against principal followed by comparator should pass
        ProtectionDomain pd = new ProtectionDomain(cs, null, null, p3);
        if (!policy.implies(pd, FOO)) {
            throw new SecurityException("test." + testnum + " failed");
        }
        testnum++;

        // should not match BAR grant entry in policy
        pd = new ProtectionDomain(cs, null, null, p3);
        if (policy.implies(pd, BAR)) {
            throw new SecurityException("test." + testnum + " failed");
        }
        testnum++;

        // security check against comparator followed by principal should pass
        pd = new ProtectionDomain(cs, null, null, p4);
        if (!policy.implies(pd, BAR)) {
            throw new SecurityException("test." + testnum + " failed");
        }
        testnum++;

        // should not match FOO grant entry in policy
        pd = new ProtectionDomain(cs, null, null, p4);
        if (policy.implies(pd, FOO)) {
            throw new SecurityException("test." + testnum + " failed");
        }
        testnum++;

        // security check against principal-principal-comparator should pass
        pd = new ProtectionDomain(cs, null, null, p5);
        if (!policy.implies(pd, HELLO)) {
            throw new SecurityException("test." + testnum + " failed");
        }
        testnum++;

        // should not match WORLD grant entry in policy
        pd = new ProtectionDomain(cs, null, null, p5);
        if (policy.implies(pd, WORLD)) {
            throw new SecurityException("test." + testnum + " failed");
        }
        testnum++;

        // security check against principal-principal-comparator should pass
        pd = new ProtectionDomain(cs, null, null, p6);
        if (!policy.implies(pd, WORLD)) {
            throw new SecurityException("test." + testnum + " failed");
        }
        testnum++;

        // should not match HELLO grant entry in policy
        pd = new ProtectionDomain(cs, null, null, p6);
        if (policy.implies(pd, HELLO)) {
            throw new SecurityException("test." + testnum + " failed");
        }
        testnum++;

        testnum = doBadTest(policy, testnum);

        return testnum;
    }
}
