/*
 * Copyright (c) 2005, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6181936
 * @summary Test basic functionality of X500Principal.getName(String, Map)
 * @author Sean Mullan
 */
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.security.auth.x500.X500Principal;

public class OIDMap {

    public static void main(String[] args) throws Exception {

        X500Principal p = null;
        Map<String, String> m1, m2 = null;

        // test null oidMap
        p = new X500Principal("CN=user");
        try {
            p.getName("RFC2253", null);
            throw new Exception
                ("expected NullPointerException for null oidMap");
        } catch (NullPointerException npe) {}

        // test improperly specified keyword
        m1 = Collections.singletonMap("FOO", "1.2.3");
        m2 = Collections.singletonMap("1.2.3", "*&$");
        p = new X500Principal("FOO=user", m1);
        try {
            p.getName("RFC2253", m2);
            throw new Exception
                ("expected IllegalArgumentException for bad keyword");
        } catch (IllegalArgumentException iae) {}
        try {
            m2 = Collections.singletonMap("1.2.3", "1abc");
            p.getName("RFC2253", m2);
            throw new Exception
                ("expected IllegalArgumentException for bad keyword");
        } catch (IllegalArgumentException iae) {}
        try {
            m2 = Collections.singletonMap("1.2.3", "");
            p.getName("RFC2253", m2);
            throw new Exception
                ("expected IllegalArgumentException for bad keyword");
        } catch (IllegalArgumentException iae) {}
        try {
            m2 = Collections.singletonMap("1.2.3", "a1_b)a");
            p.getName("RFC2253", m2);
            throw new Exception
                ("expected IllegalArgumentException for bad keyword");
        } catch (IllegalArgumentException iae) {}

        // ignore improperly specified OID
        m1 = Collections.singletonMap("*&D", "FOO");
        p = new X500Principal("CN=user");
        p.getName("RFC2253", m1);

        // override builtin OIDs
        m1 = Collections.singletonMap("2.5.4.3", "FOO");
        p = new X500Principal("CN=user");
        if (!p.getName("RFC2253", m1).startsWith("FOO")) {
            throw new Exception("mapping did not override builtin OID");
        }

        // disallow CANONICAL format
        try {
            p.getName("CANONICAL", m1);
            throw new Exception
                ("expected IllegalArgumentException for CANONICAL format");
        } catch (IllegalArgumentException iae) {}
        // disallow invalid format
        try {
            p.getName("YABBADABBADOO", m1);
            throw new Exception
                ("expected IllegalArgumentException for invalid format");
        } catch (IllegalArgumentException iae) {}

        // map OIDs
        m1 = Collections.singletonMap("1.1", "BAR");
        p = new X500Principal("1.1=sean");
        System.out.println(p.getName("RFC1779", m1));
        System.out.println(p.getName("RFC2253", m1));
        // FIXME: 1779 format is broken!
        if (!p.getName("RFC1779", m1).startsWith("BAR")) {
            throw new Exception("mapping did not override builtin OID");
        }
    }
}
