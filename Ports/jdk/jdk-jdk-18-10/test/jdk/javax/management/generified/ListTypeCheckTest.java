/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6250772
 * @summary Test that *List objects are checked after asList is called.
 * @author Eamonn McManus
 *
 * @run clean ListTypeCheckTest
 * @run build ListTypeCheckTest
 * @run main ListTypeCheckTest
 */

import java.lang.reflect.*;
import java.util.*;
import javax.management.*;
import javax.management.relation.*;

/* For compatibility reasons, the classes AttributeList, RoleList,
 * and RoleUnresolvedList all extend ArrayList<Object> even though
 * logically they should extend ArrayList<Attribute> etc.  They are
 * all specified to have a method asList() with return type
 * List<Attribute> etc, and to refuse to add any object other than
 * an Attribute etc once this method has been called, but not before.
 */
public class ListTypeCheckTest {
    public static void main(String[] args) throws Exception {
        Class[] classes = {
            AttributeList.class, RoleList.class, RoleUnresolvedList.class,
        };
        for (Class c : classes)
            test((Class<? extends ArrayList>) c);
    }

    private static void test(Class<? extends ArrayList> c) throws Exception {
        System.out.println("Testing " + c.getName());
        ArrayList al = c.newInstance();
        test(al);
    }

    private static void test(ArrayList al) throws Exception {
        test(al, true);
        al.clear();
        Method m = al.getClass().getMethod("asList");
        m.invoke(al);
        test(al, false);
    }

    private static void test(ArrayList al, boolean allowsBogus) throws Exception {
        for (int i = 0; i < 5; i++) {
            try {
                switch (i) {
                    case 0:
                        al.add("yo");
                        break;
                    case 1:
                        al.add(0, "yo");
                        break;
                    case 2:
                        al.addAll(Arrays.asList("foo", "bar"));
                        break;
                    case 3:
                        al.addAll(0, Arrays.asList("foo", "bar"));
                        break;
                    case 4:
                        al.set(0, "foo");
                        break;
                    default:
                        throw new Exception("test wrong");
                }
                if (!allowsBogus)
                    throw new Exception("op allowed but should fail");
            } catch (IllegalArgumentException e) {
                if (allowsBogus)
                    throw new Exception("got exception but should not", e);
            }
        }
    }
}
