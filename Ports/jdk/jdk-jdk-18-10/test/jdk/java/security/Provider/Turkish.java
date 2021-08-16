/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6220064 7054918 8130181
 * @summary make sure everything works ok in the Turkish local (dotted/dotless i problem)
 * @author Andreas Sterbenz
 */

import java.util.*;

import java.security.*;
import java.security.Provider.Service;
import javax.crypto.*;

public class Turkish {

    public static void main(String[] args) throws Exception {
        Provider p1 = new TProvider("T1");
        System.out.println(p1.getServices()); // trigger service parsing

        Locale loc = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));

            Provider p2 = new TProvider("T2");
            System.out.println(p2.getServices()); // trigger service parsing

            System.out.println(Signature.getInstance("MD5withRSA"));
            System.out.println(Signature.getInstance("md5withrsa"));
            System.out.println(Signature.getInstance("MD5WITHRSA"));
            Service s1, s2;
            s1 = p1.getService("Signature", "MD5withRSA");
            check(s1, null);
            check(s1, p1.getService("Signature", "md5withrsa"));
            check(s1, p1.getService("Signature", "MD5WITHRSA"));
            check(s1, p1.getService("Signature", "MD5RSA"));
            check(s1, p1.getService("Signature", "md5rsa"));
            check(s1, p1.getService("Signature", "MD5rsa"));

            s1 = p1.getService("Signature", "SHAwithRSA");
            check(s1, null);
            check(s1, p1.getService("Signature", "shawithrsa"));
            check(s1, p1.getService("Signature", "SHAWITHRSA"));
            check(s1, p1.getService("Signature", "SHARSA"));
            check(s1, p1.getService("Signature", "sharsa"));
            check(s1, p1.getService("Signature", "SHArsa"));
            check(s1, p1.getService("Signature", "SHA1RSA"));
            check(s1, p1.getService("Signature", "sha1rsa"));
            check(s1, p1.getService("Signature", "SHA1rsa"));

            s1 = p2.getService("Signature", "MD5withRSA");
            check(s1, null);
            check(s1, p2.getService("Signature", "md5withrsa"));
            check(s1, p2.getService("Signature", "MD5WITHRSA"));
            check(s1, p2.getService("Signature", "MD5RSA"));
            check(s1, p2.getService("Signature", "md5rsa"));
            check(s1, p2.getService("Signature", "MD5rsa"));

            s1 = p2.getService("Signature", "SHAwithRSA");
            check(s1, null);
            check(s1, p2.getService("Signature", "shawithrsa"));
            check(s1, p2.getService("Signature", "SHAWITHRSA"));
            check(s1, p2.getService("Signature", "SHARSA"));
            check(s1, p2.getService("Signature", "sharsa"));
            check(s1, p2.getService("Signature", "SHArsa"));
            check(s1, p2.getService("Signature", "SHA1RSA"));
            check(s1, p2.getService("Signature", "sha1rsa"));
            check(s1, p2.getService("Signature", "SHA1rsa"));

            System.out.println("OK");
        } finally {
            Locale.setDefault(loc);
        }
    }

    private static void check(Service s1, Service s2) throws Exception {
        System.out.println(s1);
        if (s1 == null) {
            throw new Exception("service is null");
        }
        if ((s2 != null) && (s1 != s2)) {
            throw new Exception("service does not match");
        }
    }

    private static class TProvider extends Provider {
        TProvider(String name) {
            super(name, "1.0", null);
            put("Signature.MD5withRSA", "com.foo.Sig");
            put("Alg.Alias.Signature.MD5RSA", "MD5withRSA");
            put("sIGNATURE.shaWITHrsa", "com.foo.Sig");
            put("aLG.aLIAS.sIGNATURE.sharsa", "shaWITHrsa");
            put("aLG.aLIAS.sIGNATURE.sha1rsa", "shawithrsa");
        }
    }

}
