/*
 * Copyright (c) 2001, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4505053
 * @summary make sure X500Principal CANONICAL format escapes leading '#' in
 *       attribute values of type String.
 */
import javax.security.auth.x500.X500Principal;

public class EscapedChars {

    public static void main(String[] args) throws Exception {

        String dn="CN=\\#user";
        X500Principal xp = new X500Principal(dn);

        System.out.println("RFC2253 DN is " +
            xp.getName(X500Principal.RFC2253));
        System.out.println("CANONICAL DN is is " +
            xp.getName(X500Principal.CANONICAL));

        String dn1 = xp.getName(X500Principal.CANONICAL);
        if (!(dn1.substring(3,5).equals("\\#")))
            throw new Exception("Leading # not escaped");

        X500Principal xp1 = new X500Principal(dn1);
        System.out.println("CANONICAL DN is " +
            xp1.getName(X500Principal.CANONICAL));
    }
}
