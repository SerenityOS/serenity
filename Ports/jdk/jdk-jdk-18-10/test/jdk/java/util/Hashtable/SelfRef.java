/*
 * Copyright (c) 2001, 2005, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4421469 6282555
 * @summary Hashtable's toString method should detect self-referential
 *          hash tables rather than throwing a StackOverflowException.
 * @author Josh Bloch, Martin Buchholz
 */

import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SelfRef {
    public static void main(String[] args) {
        testMap(new Hashtable<Object,Object>());
        testMap(new HashMap<Object,Object>());
        testMap(new LinkedHashMap<Object,Object>());
        testMap(new ConcurrentHashMap<Object,Object>());
    }

    private static void testMap(Map<Object,Object> m) {
        if (! (m.toString().equals("{}")))
            throw new Error();
        m.put("Harvey", m);
        if (! (m.toString().equals("{Harvey=(this Map)}")))
            throw new Error();
        m.clear();
        m.put(m, "Harvey");
        if (! (m.toString().equals("{(this Map)=Harvey}")))
            throw new Error();
        m.clear();
        m.hashCode();
    }
}
