/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.security.auth.NTUserPrincipal;
import com.sun.security.auth.UnixPrincipal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import javax.security.auth.Subject;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 8050409
 * @modules jdk.security.auth
 * @summary Tests with Subject.getPrivateCredentials to check permission checks with one or more principals.
 * @run testng/othervm/policy=MoreThenOnePrincipals.policy MoreThenOnePrincipals
 */
public class MoreThenOnePrincipals {
    private static final String[] CRED_VALUES =
            new String[]{"testPrivateCredential-1", "testPrivateCredentials-2"};
    private static final HashSet CREDS = new HashSet<>(Arrays.asList(CRED_VALUES));

    /**
     * Policy file grants access to the private Credential,belonging to a
     * Subject with at least two associated Principals:"com.sun.security.auth
     * .NTUserPrincipal", with the name,"NTUserPrincipal-1", and
     * "com.sun.security.auth.UnixPrincipal", with the name, "UnixPrincipals-1".
     *
     * For test1 and test2, subjects are associated with none or only one of
     * principals mentioned above, SecurityException is expected.
     * For test 3 and test 4, subjects are associated with two or more
     * Principals (above principals are included), no exception is expected.
     *
     */

    @Test(dataProvider = "Provider1", expectedExceptions = SecurityException.class)
    public void test1(Subject s) {
        s.getPrivateCredentials(String.class);
    }

    @Test(dataProvider = "Provider1", expectedExceptions = SecurityException.class)
    public void test2(Subject s) {
        s.getPrivateCredentials().iterator().next();
    }

    @Test(dataProvider = "Provider2")
    public void test3(Subject s) {
        s.getPrivateCredentials(String.class);
    }

    @Test(dataProvider = "Provider2")
    public void test4(Subject s) {
        s.getPrivateCredentials().iterator().next();
    }

    @DataProvider
    public Object[][] Provider1() {
        Subject s1 = new Subject(false, Collections.EMPTY_SET, Collections.EMPTY_SET, CREDS);
        s1.getPrincipals().add(new NTUserPrincipal("NTUserPrincipal-2"));
        Subject s2 = new Subject(false, Collections.EMPTY_SET, Collections.EMPTY_SET, CREDS);
        s2.getPrincipals().add(new NTUserPrincipal("NTUserPrincipal-1"));
        return new Object[][]{{s1}, {s2}};
    }

    @DataProvider
    public Object[][] Provider2() {
        Subject s3 = new Subject(false, Collections.EMPTY_SET, Collections.EMPTY_SET, CREDS);
        s3.getPrincipals().add(new NTUserPrincipal("NTUserPrincipal-1"));
        s3.getPrincipals().add(new UnixPrincipal("UnixPrincipals-1"));
        Subject s4 = new Subject(false, Collections.EMPTY_SET, Collections.EMPTY_SET, CREDS);
        s4.getPrincipals().add(new NTUserPrincipal("NTUserPrincipal-1"));
        s4.getPrincipals().add(new UnixPrincipal("UnixPrincipals-1"));
        s4.getPrincipals().add(new UnixPrincipal("UnixPrincipals-2"));
        return new Object[][]{{s3}, {s4}};
    }

}
