/*
 * Copyright (c) 2004, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5097015 8130181
 * @summary make sure we correctly treat Provider string entries as case insensitive
 * @author Andreas Sterbenz
 */

import java.security.*;
import java.security.Provider.*;

public class CaseSensitiveServices extends Provider {
    CaseSensitiveServices() {
        super("Foo", "1.0", null);
        put("MessageDigest.Foo", "com.Foo");
        put("mESSAGEdIGEST.fOO xYz", "aBc");
        put("ALg.aliaS.MESSAGEdigest.Fu", "FoO");
        put("messageDigest.Bar", "com.Bar");
        put("MESSAGEDIGEST.BAZ", "com.Baz");
    }

    public static void main(String[] args) throws Exception {
        Provider p = new CaseSensitiveServices();
        System.out.println(p.getServices());
        if (p.getServices().size() != 3) {
            throw new Exception("services.size() should be 3");
        }
        Service s = testService(p, "MessageDigest", "fOO");
        String val = s.getAttribute("Xyz");
        if ("aBc".equals(val) == false) {
            throw new Exception("Wrong value: " + val);
        }
        testService(p, "MessageDigest", "fU");
        testService(p, "MessageDigest", "BAR");
        testService(p, "MessageDigest", "baz");
        System.out.println("OK");
    }

    private static Service testService(Provider p, String type, String alg) throws Exception {
        System.out.println("Getting " + type + "." + alg + "...");
        Service s = p.getService(type, alg);
        System.out.println(s);
        if (s == null) {
            throw new Exception("Lookup failed for: " + type + "." + alg);
        }
        return s;
    }

}
