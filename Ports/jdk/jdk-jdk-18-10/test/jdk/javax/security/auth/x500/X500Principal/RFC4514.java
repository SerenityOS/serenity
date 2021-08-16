/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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

import javax.security.auth.x500.X500Principal;

/**
 * @test
 * @bug 6611991
 * @summary Add support for parsing RFC 4514 DNs to X500Principal
 *
 * Ensure RFC 4514 Distinguished Name Strings can be parsed by X500Principal.
 * RFC 4514 obsoleted RFC 2253 so we should make sure we can parse DNs of
 * that form that contain subtle differences or clarifications in the grammar.
 */
public class RFC4514 {

    private int failed = 0;

    public static void main(String[] args) throws Exception {
        new RFC4514().test();
    }

    private void test() throws Exception {

        /**
         * RFC 4514 allows space to be escaped as '\ '.
         */
        parse("CN=\\ Space\\ ,C=US");
        parse("CN=Sp\\ ace,C=US");
        /**
         * RFC 4514 does not require escaping of '=' characters.
         */
        parse("CN=Eq=uals,C=US");
        /**
         * RFC 4514 requires the null character to be escaped.
         */
        parse("CN=\\00,C=US");
        /**
         * RFC 4514 does not require escaping of non-leading '#' characters.
         */
        parse("CN=Num#ber,C=US");
        /**
         * XMLDSig (http://www.w3.org/TR/2002/REC-xmldsig-core-20020212/)
         * allows implementations to escape trailing whitespace as '\20'.
         */
        parse("CN=Trailing \\20,C=US");
        /**
         * XMLDSig allows implementations to escape ASCII control characters
         * (Unicode range \x00 - \x1f) by replacing them with "\" followed by
         * a two digit hex number showing its Unicode number.
         */
        parse("CN=Con\\09trol,C=US");

        if (failed != 0) {
            throw new Exception("Some RFC4514 tests FAILED");
        }
    }

    public void parse(String dnString) throws Exception {

        System.out.println("Parsing " + dnString);
        X500Principal dn = new X500Principal(dnString);
        String dnString2 = dn.getName();
        X500Principal dn2 = new X500Principal(dnString2);
        if (dn.equals(dn2)) {
            System.out.println("PASSED");
        } else {
            System.out.println("FAILED");
            failed++;
        }
    }
}
