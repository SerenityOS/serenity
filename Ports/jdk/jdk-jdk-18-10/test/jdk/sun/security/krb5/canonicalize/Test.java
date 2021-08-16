/*
 * Copyright (c) 2009, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6682516 8149521
 * @summary SPNEGO_HTTP_AUTH/WWW_KRB and SPNEGO_HTTP_AUTH/WWW_SPNEGO failed on all non-windows platforms
 * @modules java.security.jgss/sun.security.krb5
 * @run main/othervm -Djdk.net.hosts.file=${test.src}/TestHosts
 *      -Djava.security.krb5.realm=THIS.REALM
 *      -Djava.security.krb5.kdc=localhost
 *      -Djava.security.krb5.conf=krb5.conf Test
 */

import sun.security.krb5.PrincipalName;

public class Test {
    public static void main(String[] args) throws Exception {
        // add using canonicalized name
        check("c1", "c1.this.domain");
        check("c1.this", "c1.this.domain");
        check("c1.this.domain", "c1.this.domain");
        check("c1.this.domain.", "c1.this.domain");

        // canonicalized name goes IP, reject
        check("c2", "c2");
        check("c2.", "c2");

        // canonicalized name goes strange, reject
        check("c3", "c3");

        // unsupported
        check("c4", "c4");
    }

    static void check(String input, String output) throws Exception {
        System.out.println(input + " -> " + output);
        PrincipalName pn = new PrincipalName("host/"+input,
                PrincipalName.KRB_NT_SRV_HST);
        if (!pn.getNameStrings()[1].equals(output)) {
            throw new Exception("Output is " + pn);
        }
    }
}
