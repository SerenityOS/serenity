/*
 * Copyright (c) 2000, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4390546
 * @modules jdk.security.auth
 * @summary     performance regression and other bugs in
 *              SubjectDomainCombiner.combine
 *
 * @run main/othervm/policy=Regression.policy -Djava.security.auth.debug=combiner Regression
 */

import java.security.ProtectionDomain;
import java.security.CodeSource;
import java.net.URL;
import java.util.Set;
import java.util.HashSet;
import javax.security.auth.Subject;
import javax.security.auth.SubjectDomainCombiner;

public class Regression {

    public static void main(String[] args) {

        Set principals = new HashSet();
        principals.add(new com.sun.security.auth.NTUserPrincipal("test1"));
        principals.add(new com.sun.security.auth.NTUserPrincipal("test2"));

        Subject subject = new Subject
                (false, principals, new HashSet(), new HashSet());

        SubjectDomainCombiner sdc = new SubjectDomainCombiner(subject);

        URL url1;
        URL url2;
        URL url3;
        URL url4;
        try {
            url1 = new URL("http://one");
            url2 = new URL("http://two");
            url3 = new URL("http://three");
            url4 = new URL("http://four");
        } catch (java.net.MalformedURLException mue) {
            mue.printStackTrace();
            throw new SecurityException("Test failed: " + mue.toString());
        }

        ProtectionDomain d1 = new ProtectionDomain
                                (new CodeSource(url1,
                                    (java.security.cert.Certificate[]) null),
                                null,                   // permissions
                                null,                   // class loader
                                null);                  // principals
        ProtectionDomain d2 = new ProtectionDomain
                                (new CodeSource(url2,
                                    (java.security.cert.Certificate[]) null),
                                null,                   // permissions
                                null,                   // class loader
                                null);                  // principals
        ProtectionDomain d3 = new ProtectionDomain
                                (new CodeSource(url3,
                                    (java.security.cert.Certificate[]) null),
                                null,                   // permissions
                                null,                   // class loader
                                null);                  // principals
        ProtectionDomain d4 = new ProtectionDomain
                                (new CodeSource(url4,
                                    (java.security.cert.Certificate[]) null),
                                null,                   // permissions
                                null,                   // class loader
                                null);                  // principals

        // test 1
        // -- regular combine, make sure we get a proper combination back

        ProtectionDomain currentDomains[] = { d1, d2, d3 };
        ProtectionDomain assignedDomains[] = { d4 };
        ProtectionDomain domains1[] = sdc.combine
                        (currentDomains, assignedDomains);

        if (domains1.length != 4 ||
            domains1[0] == d1 || domains1[1] == d2 || domains1[2] == d3 ||
            domains1[3] != d4 ||
            !domains1[0].implies(new RuntimePermission("queuePrintJob"))) {
            throw new SecurityException("Test failed: combine test 1 failed");
        }

        System.out.println("-------- TEST ONE PASSED --------");

        // test 2
        // -- repeat combine, make sure combiner cachine returned the
        //    same PD's back

        ProtectionDomain domains2[] = sdc.combine
                        (currentDomains, assignedDomains);
        if (domains2.length != 4 ||
            domains2[0] != domains1[0] || domains2[1] != domains1[1] ||
            domains2[2] != domains1[2] ||
            domains2[3] != domains1[3] ||
            !domains2[0].implies(new RuntimePermission("queuePrintJob"))) {
            throw new SecurityException("Test failed: combine test 2 failed");
        }

        System.out.println("-------- TEST TWO PASSED --------");

        // test 3
        // -- mutate the Subject and make sure the combiner cache
        //    got cleared out

        subject.getPrincipals().remove
                (new com.sun.security.auth.NTUserPrincipal("test2"));
        ProtectionDomain domains3[] = sdc.combine
                        (currentDomains, assignedDomains);
        if (domains3.length != 4 ||
            domains3[0] == domains1[0] || domains3[1] == domains1[1] ||
            domains3[2] == domains1[2] ||
            domains3[3] != domains1[3] ||
            !domains3[0].implies(new RuntimePermission("createClassLoader")) ||
            domains3[0].implies(new RuntimePermission("queuePrintJob"))) {
            throw new SecurityException("Test failed: combine test 3 failed");
        }

        System.out.println("-------- TEST THREE PASSED --------");

        System.out.println("Test Passed");
    }
}
