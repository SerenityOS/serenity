/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5100561
 * @bug 6273812
 * @summary Can not explicitly create a java.security.Policy object from a file
 * @modules java.base/sun.security.provider
 * @build GetInstancePolicySpi GetInstanceProvider
 * @run main/othervm/policy=GetInstance.policy GetInstance
 */

import java.security.*;

import java.io.File;
import java.net.URI;

public class GetInstance {

    private static final String JAVA_POLICY = "JavaPolicy";

    private static class BadParam implements Policy.Parameters { }

    public static void main(String[] args) throws Exception {

        int testnum = 1;
        GetInstance gi = new GetInstance();

        testnum = gi.testDefault(testnum);
        testnum = gi.testStringProvider(testnum);
        testnum = gi.testProvider(testnum);
        testnum = gi.testCustomImpl(testnum);
        testnum = gi.testBadParam(testnum);

        // make this go last because we don't want to leave its policy set
        // for other tests
        testnum = gi.testURIParam(testnum);
    }

    private int testDefault(int testnum) throws Exception {
        // get an instance of the default PolicySpiFile
        Policy p = Policy.getInstance(JAVA_POLICY, null);
        doTest(p, testnum++);
        Policy.setPolicy(p);
        doTestSM(testnum++);

        // get an instance of FooPolicy
        try {
            p = Policy.getInstance("FooPolicy", null);
            throw new SecurityException("test " + testnum++ + " failed");
        } catch (NoSuchAlgorithmException nsae) {
            // good
            System.out.println("test " + testnum++ + " passed");
        }

        return testnum;
    }

    private int testStringProvider(int testnum) throws Exception {
        // get an instance of JavaPolicy from SUN
        Policy p = Policy.getInstance(JAVA_POLICY, null, "SUN");
        doTest(p, testnum++);
        Policy.setPolicy(p);
        doTestSM(testnum++);

        // get an instance of JavaPolicy from SunRsaSign
        try {
            p = Policy.getInstance(JAVA_POLICY, null, "SunRsaSign");
            throw new SecurityException("test " + testnum++ + " failed");
        } catch (NoSuchAlgorithmException nsae) {
            // good
            System.out.println("test " + testnum++ + " passed");
        }

        // get an instance of JavaPolicy from FOO
        try {
            p = Policy.getInstance(JAVA_POLICY, null, "FOO");
            throw new SecurityException("test " + testnum++ + " failed");
        } catch (NoSuchProviderException nspe) {
            // good
            System.out.println("test " + testnum++ + " passed");
        }

        return testnum;
    }

    private int testProvider(int testnum) throws Exception {
        // get an instance of JavaPolicy from SUN
        Policy p = Policy.getInstance(JAVA_POLICY,
                                null,
                                Security.getProvider("SUN"));
        doTest(p, testnum++);
        Policy.setPolicy(p);
        doTestSM(testnum++);

        // get an instance of JavaPolicy from SunRsaSign
        try {
            p = Policy.getInstance(JAVA_POLICY,
                                null,
                                Security.getProvider("SunRsaSign"));
            throw new SecurityException("test " + testnum++ + " failed");
        } catch (NoSuchAlgorithmException nsae) {
            // good
            System.out.println("test " + testnum++ + " passed");
        }

        return testnum;
    }

    private int testBadParam(int testnum) throws Exception {

        // pass bad param

        try {
            Policy p = Policy.getInstance(JAVA_POLICY,
                                new BadParam());
            throw new SecurityException("test " + testnum++ + " failed");
        } catch (IllegalArgumentException iae) {
            // good
            System.out.println("test " + testnum++ + " passed");
        }

        try {
            Policy p = Policy.getInstance(JAVA_POLICY,
                                new BadParam(),
                                "SUN");
            throw new SecurityException("test " + testnum++ + " failed");
        } catch (IllegalArgumentException iae) {
            // good
            System.out.println("test " + testnum++ + " passed");
        }

        try {
            Policy p = Policy.getInstance(JAVA_POLICY,
                                new BadParam(),
                                Security.getProvider("SUN"));
            throw new SecurityException("test " + testnum++ + " failed");
        } catch (IllegalArgumentException iae) {
            // good
            System.out.println("test " + testnum++ + " passed");
        }

        return testnum;
    }

    private int testURIParam(int testnum) throws Exception {
        // get an instance of JavaPolicy from SUN and have it read from the URL

        File file = new File(System.getProperty("test.src", "."),
                                "GetInstance.policyURL");
        URI uri = file.toURI();
        Policy p = Policy.getInstance(JAVA_POLICY, new URIParameter(uri));

        doTest(p, testnum++);
        Policy.setPolicy(p);
        doTestSM(testnum++);

        return testnum;
    }

    private int testCustomImpl(int testnum) throws Exception {
        Provider customProvider = new GetInstanceProvider();
        Policy p = Policy.getInstance("GetInstancePolicySpi",
                                null,
                                customProvider);

        // doTest has a case that will not work with custom policies,
        // so do not call it
        //
        // doTest(p, testnum++);

        Policy.setPolicy(p);
        doTestSM(testnum++);

        return testnum;
    }

    private void doTest(Policy p, int testnum) throws Exception {

        // check granted perm
        if (p.implies(this.getClass().getProtectionDomain(),
                        new SecurityPermission("GetInstanceTest"))) {
            System.out.println("test " + testnum + ".1 passed");
        } else {
            throw new SecurityException("test " + testnum + ".1 failed");
        }

        // check perm not granted
        if (p.implies(this.getClass().getProtectionDomain(),
                        new SecurityPermission("NotGranted"))) {
            throw new SecurityException("test " + testnum + ".2 failed");
        } else {
            System.out.println("test " + testnum + ".2 passed");
        }

        // test getProvider
        if ("SUN".equals(p.getProvider().getName())) {
            System.out.println("test " + testnum + ".3 passed");
        } else {
            throw new SecurityException("test " + testnum + ".3 failed");
        }

        // test getType
        if (JAVA_POLICY.equals(p.getType())) {
            System.out.println("test " + testnum + ".4 passed");
        } else {
            throw new SecurityException("test " + testnum + ".4 failed");
        }
    }

    private void doTestSM(int testnum) throws Exception {

        // check granted perm
        System.getSecurityManager().checkPermission
                        (new SecurityPermission("GetInstanceTest"));
        System.out.println("test " + testnum + ".1 passed");

        // check perm not granted
        try {
            System.getSecurityManager().checkPermission
                        (new SecurityPermission("NotGranted"));
            throw new RuntimeException("test " + testnum + ".2 failed");
        } catch (SecurityException se) {
            System.out.println("test " + testnum + ".2 passed");
        }
    }
}
