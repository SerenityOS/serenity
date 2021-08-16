/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4856968 7054918 8130181
 * @library ../testlibrary
 * @summary make sure add/insert/removeProvider() work correctly
 * @author Andreas Sterbenz
 */

import java.util.*;

import java.security.*;

public class ChangeProviders extends Provider {

    private ChangeProviders() {
        super("Foo", "47.23", "none");
    }

    private static int plen() {
        return Security.getProviders().length;
    }

    public static void main(String[] args) throws Exception {
        ProvidersSnapshot snapshot = ProvidersSnapshot.create();
        try {
            main0(args);
        } finally {
            snapshot.restore();
        }
    }

    public static void main0(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        Provider p = new ChangeProviders();

        int n = plen();
        Security.addProvider(p);
        if (plen() != n + 1) {
            throw new Exception("Provider not added");
        }
        Security.addProvider(p);
        if (plen() != n + 1) {
            throw new Exception("Provider readded");
        }
        Security.insertProviderAt(p, 1);
        if (plen() != n + 1) {
            throw new Exception("Provider readded");
        }
        Security.removeProvider(p.getName());
        if ((plen() != n) || (Security.getProvider(p.getName()) != null)) {
            throw new Exception("Provider not removed");
        }
        Security.insertProviderAt(p, 1);
        if (plen() != n + 1) {
            throw new Exception("Provider not added");
        }
        if (Security.getProviders()[0] != p) {
            throw new Exception("Provider not at pos 1");
        }

        System.out.println("All tests passed.");
    }

}
