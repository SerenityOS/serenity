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
 * @summary Test basic functionality of X500Principal(String, Map) constructor
 * @author Sean Mullan
 */
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.security.auth.x500.X500Principal;

public class KeywordMap {

    public static void main(String[] args) throws Exception {

        X500Principal p = null;
        Map<String, String> m = null;

        // test null keywordMap
        try {
            p = new X500Principal("CN=user", null);
            throw new Exception
                ("expected NullPointerException for null keywordMap");
        } catch (NullPointerException npe) {}

        // test improperly specified OID
        m = Collections.singletonMap("FOO", "FOO");
        try {
            p = new X500Principal("FOO=user", m);
            throw new Exception
                ("expected IllegalArgumentException for bad OID");
        } catch (IllegalArgumentException iae) {}

        // ignore improperly specified keyword
        m = Collections.singletonMap("?*&", "FOO");
        p = new X500Principal("CN=user", m);

        // throw exception if no mapping for keyword
        m = Collections.singletonMap("BAR", "1.2.3");
        try {
            p = new X500Principal("FOO=user", m);
            throw new Exception
                ("expected IllegalArgumentExc for keyword with no mapping");
        } catch (IllegalArgumentException iae) {}

        // don't match keyword in lower-case
        m = Collections.singletonMap("foo", "1.2.3");
        try {
            p = new X500Principal("FOO=user", m);
            throw new Exception
                ("expected IllegalArgumentExc for wrong-case keyword mapping");
        } catch (IllegalArgumentException iae) {}

        // allow duplicate OID mappings
        m = new HashMap<String, String>();
        m.put("FOO", "1.2.3");
        m.put("BAR", "1.2.3");
        p = new X500Principal("BAR=user", m);

        // override builtin keywords
        m = Collections.singletonMap("CN", "1.2.3");
        p = new X500Principal("CN=user", m);
        if (!p.getName().startsWith("1.2.3")) {
            throw new Exception("mapping did not override builtin keyword");
        }

        // override builtin OIDs
        m = Collections.singletonMap("FOO", "2.5.4.3");
        p = new X500Principal("FOO=sean", m);
        if (!p.getName().startsWith("CN")) {
            throw new Exception("mapping did not override builtin OID");
        }
    }
}
