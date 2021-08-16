/*
 * Copyright (c) 2002, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     4715206
 * @summary Ensure that addAll method can cope with underestimate by size().
 * @author  Josh Bloch
 */

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.WeakHashMap;

public class AddAll {
    public static void main(String[] args) {
        for (int j = 0; j < 1; j++) {
            Map m = new WeakHashMap(100000);
            for (int i = 0; i < 100000; i++)
                m.put(new Object(), Boolean.TRUE);
            new ArrayList().addAll(m.keySet());
        }

        for (int j = 0; j < 1; j++) {
            Map m = new WeakHashMap(100000);
            for (int i = 0; i < 100000; i++)
                m.put(new Object(), Boolean.TRUE);
            new LinkedList().addAll(m.keySet());
        }

        for (int j = 0; j < 1; j++) {
            Map m = new WeakHashMap(100000);
            for (int i = 0; i < 100000; i++)
                m.put(new Object(), Boolean.TRUE);
            new Vector().addAll(m.keySet());
        }

        for (int j = 0; j < 1; j++) {
            Map m = new WeakHashMap(100000);
            for (int i = 0; i < 100000; i++)
                m.put(new Object(), Boolean.TRUE);
            List list = new ArrayList();
            list.add("inka"); list.add("dinka"); list.add("doo");
            list.addAll(1, m.keySet());
        }

        for (int j = 0; j < 1; j++) {
            Map m = new WeakHashMap(100000);
            for (int i = 0; i < 100000; i++)
                m.put(new Object(), Boolean.TRUE);
            List list = new LinkedList();
            list.add("inka"); list.add("dinka"); list.add("doo");
            list.addAll(1, m.keySet());
        }

        for (int j = 0; j < 1; j++) {
            Map m = new WeakHashMap(100000);
            for (int i = 0; i < 100000; i++)
                m.put(new Object(), Boolean.TRUE);
            List list = new ArrayList();
            list.add("inka"); list.add("dinka"); list.add("doo");
            list.addAll(1, m.keySet());
        }
    }
}
