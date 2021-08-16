/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8258851
 * @library /test/lib ..
 * @modules jdk.crypto.cryptoki
 * @run main CheckRegistration
 * @summary Ensure SunPKCS11 provider service registration matches the actual
 *     impl class
 */

import java.security.Provider;
import java.util.Set;

public class CheckRegistration extends PKCS11Test {

    public static void main(String[] args) throws Exception {
        main(new CheckRegistration(), args);
    }

    @Override
    public void main(Provider p) throws Exception {
        Set<Provider.Service> services = p.getServices();

        for (Provider.Service s : services) {
            String key = s.getType() + "." + s.getAlgorithm();
            Object val = p.get(key);
            System.out.println("Checking " + key + " : " + s.getClassName());
            if (val == null) {
                throw new RuntimeException("Missing mapping");
            }
            if (!s.getClassName().equals(val)) {
                System.out.println("Mapping value: " + val);
                throw new RuntimeException("Mapping mismatches");
            }
            Object o = s.newInstance(null);
            if (!s.getClassName().equals(o.getClass().getName())) {
                System.out.println("Actual impl: " + o.getClass().getName());
                throw new RuntimeException("Impl class mismatches");
            }
        }
        System.out.println("Test Passed");
    }
}
