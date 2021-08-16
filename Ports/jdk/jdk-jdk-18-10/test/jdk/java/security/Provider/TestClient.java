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
package c;

import java.security.Provider;
import java.security.Security;
import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * A Test client using different mechanism to search the custom security
 * provider. It uses ClassLoader, ServiceLoader and default mechanism to find
 * a provider registered through "java.security" extension file.
 */
public class TestClient {

    public static void main(String[] args) throws Exception {

        Provider p = null;
        if (args != null && args.length > 1) {
            switch (args[0]) {
                case "CL":
                    p = (Provider) Class.forName(args[1]).newInstance();
                    if (Security.addProvider(p) == -1) {
                        throw new RuntimeException("Failed to add provider");
                    }
                    break;
                case "SL":
                    ServiceLoader<Provider> services
                            = ServiceLoader.load(java.security.Provider.class);
                    Iterator<Provider> iterator = services.iterator();
                    while (iterator.hasNext()) {
                        Provider spr = iterator.next();
                        if (spr.getName().equals(args[1])) {
                            p = spr;
                            if (Security.addProvider(p) == -1) {
                                throw new RuntimeException(
                                        "Failed to add provider");
                            }
                            break;
                        }
                    }
                    break;
                case "SPN":
                case "SPT":
                    p = Security.getProvider(args[1]);
                    break;
                default:
                    throw new RuntimeException("Invalid argument.");
            }
        }
        if (p == null) {
            throw new RuntimeException("Provider TestProvider not found");
        }
        System.out.printf("Client: found provider %s", p.getName());
    }
}
