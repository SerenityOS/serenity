/*
 * Copyright (c) 2021, Red Hat, Inc.
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

import java.security.AllPermission;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.security.Provider;
import java.security.Security;
import java.security.spec.X509EncodedKeySpec;

/*
 * @test
 * @bug 8259319
 * @library /test/lib ..
 * @run main/othervm -Djava.security.manager=allow IllegalPackageAccess
 */

public class IllegalPackageAccess extends PKCS11Test {

    private static Policy policy = Policy.getPolicy();
    private static RuntimePermission accessPerm =
            new RuntimePermission("accessClassInPackage.com.sun.crypto.provider");

    private static class MyPolicy extends Policy {
        @Override
        public PermissionCollection getPermissions(ProtectionDomain domain) {
            PermissionCollection perms = new Permissions();
            perms.add(new AllPermission());
            return perms;
        }

        @Override
        public boolean implies(ProtectionDomain domain, Permission permission) {
            if (permission.equals(accessPerm)) {
                return policy.implies(domain, permission);
            }
            return super.implies(domain, permission);
        }
    }

    public static void main(String[] args) throws Exception {
        main(new IllegalPackageAccess(), args);
        System.out.println("TEST PASS - OK");
    }

    @Override
    public void main(Provider p) throws Exception {
        Policy.setPolicy(null);
        Policy.setPolicy(new MyPolicy());
        System.setSecurityManager(new SecurityManager());

        // Remove all security providers so a fallback scheme
        // that creates class instances is forced.
        for (Provider provider : Security.getProviders()) {
            Security.removeProvider(provider.getName());
        }

        KeyPair kp = KeyPairGenerator.getInstance("DH", p)
                .generateKeyPair();
        byte[] encPubKey = kp.getPublic().getEncoded();
        KeyFactory kf = KeyFactory.getInstance("DH", p);

        // Requires access to a SunJCE class that parses
        // the encoded key.
        kf.generatePublic(new X509EncodedKeySpec(encPubKey));

        System.setSecurityManager(null);
        Policy.setPolicy(policy);
    }

}
