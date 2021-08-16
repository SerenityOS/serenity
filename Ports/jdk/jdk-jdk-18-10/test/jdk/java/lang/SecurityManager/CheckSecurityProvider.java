/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6997010 7191662
 * @summary Consolidate java.security files into one file with modifications
 * @run main/othervm -Djava.security.manager=allow CheckSecurityProvider
 */

import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
 * The main benefit of this test is to catch merge errors or other types
 * of issues where one or more of the security providers are accidentally
 * removed. With the security manager enabled, this test can also catch
 * scenarios where the default permission policy needs to be updated.
 */
public class CheckSecurityProvider {
    public static void main(String[] args) throws Exception {
        ModuleLayer layer = ModuleLayer.boot();

        System.setSecurityManager(new SecurityManager());

        String os = System.getProperty("os.name");
        /*
         * This array should be updated whenever new security providers
         * are added to the the java.security file.
         * NOTE: it should be in the same order as the java.security file
         */

        List<String> expected = new ArrayList<>();

        // NOTE: the ordering must match what's defined inside java.security
        expected.add("sun.security.provider.Sun");
        expected.add("sun.security.rsa.SunRsaSign");
        layer.findModule("jdk.crypto.ec")
            .ifPresent(m -> expected.add("sun.security.ec.SunEC"));
        expected.add("sun.security.ssl.SunJSSE");
        expected.add("com.sun.crypto.provider.SunJCE");
        layer.findModule("jdk.security.jgss")
            .ifPresent(m -> expected.add("sun.security.jgss.SunProvider"));
        layer.findModule("java.security.sasl")
            .ifPresent(m -> expected.add("com.sun.security.sasl.Provider"));
        layer.findModule("java.xml.crypto")
            .ifPresent(m -> expected.add("org.jcp.xml.dsig.internal.dom.XMLDSigRI"));
        layer.findModule("java.smartcardio")
            .ifPresent(m -> expected.add("sun.security.smartcardio.SunPCSC"));
        layer.findModule("java.naming")
            .ifPresent(m -> expected.add("sun.security.provider.certpath.ldap.JdkLDAP"));
        layer.findModule("jdk.security.jgss")
            .ifPresent(m -> expected.add("com.sun.security.sasl.gsskerb.JdkSASL"));
        if (os.startsWith("Windows")) {
            layer.findModule("jdk.crypto.mscapi")
                .ifPresent(m -> expected.add("sun.security.mscapi.SunMSCAPI"));
        }
        if (os.contains("OS X")) {
            expected.add("apple.security.AppleProvider");
        }
        layer.findModule("jdk.crypto.cryptoki")
            .ifPresent(m -> expected.add("sun.security.pkcs11.SunPKCS11"));

        List<String> actual = Stream.of(Security.getProviders())
            .map(p -> p.getClass().getName())
            .collect(Collectors.toList());

        System.out.println("Expected providers:");
        expected.stream().forEach(System.out::println);
        System.out.println("Actual providers:");
        actual.stream().forEach(System.out::println);

        if (expected.size() != actual.size()) {
            throw new Exception("Unexpected provider count. "
                + "Expected: " + expected.size() + ". Actual: " + actual.size());
        }
        Iterator<String> iter = expected.iterator();
        for (String p: actual) {
            String nextExpected = iter.next();
            if (!nextExpected.equals(p)) {
                throw new Exception("Expected " + nextExpected + ", actual " + p);
            }
        }
    }
}
