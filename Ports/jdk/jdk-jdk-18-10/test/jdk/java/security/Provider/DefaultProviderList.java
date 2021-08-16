/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 7191662 8157469 8157489
 * @summary Ensure non-java.base providers can be found by ServiceLoader
 * @author Valerie Peng
 */

import java.security.Provider;
import java.security.Security;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ServiceLoader;

public class DefaultProviderList {

    public static void main(String[] args) throws Exception {
        Provider[] defaultProvs = Security.getProviders();
        System.out.println("Providers: " + Arrays.asList(defaultProvs));
        System.out.println();

        ServiceLoader<Provider> sl = ServiceLoader.load(Provider.class);
        boolean failed = false;

        Module baseMod = Object.class.getModule();

        // Test#1: check that all non-base security providers can be found
        // through ServiceLoader
        for (Provider p : defaultProvs) {
            String pName = p.getName();
            Class pClass = p.getClass();

            if (pClass.getModule() != baseMod) {
                String pClassName = pClass.getName();
                Iterator<Provider> provIter = sl.iterator();
                boolean found = false;
                while (provIter.hasNext()) {
                    Provider pFromSL = provIter.next();

                    // check for match by class name because PKCS11 provider
                    // will have a different name after being configured.
                    if (pFromSL.getClass().getName().equals(pClassName)) {
                        found = true;
                        System.out.println("SL found provider " + pName);
                        break;
                    }
                }
                if (!found) {
                    failed = true;
                    System.out.println("Error: SL cannot find provider " +
                        pName);
                }
            }
        }

        // Test#2: check that all security providers found through ServiceLoader
        // are not from base module
        Iterator<Provider> provIter = sl.iterator();
        while (provIter.hasNext()) {
            Provider pFromSL = provIter.next();
            if (pFromSL.getClass().getModule() == baseMod) {
                failed = true;
                System.out.println("Error: base provider " +
                    pFromSL.getName() + " loaded by SL");
            }
        }

        if (!failed) {
            System.out.println("Test Passed");
        } else {
            throw new Exception("One or more tests failed");
        }
    }
}
