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
 * @bug 6229396
 * @summary Test null/empty key/values in ObjectName constructors.
 * @author Luis-Miguel Alventosa
 *
 * @run clean NullEmptyKeyValueTest
 * @run build NullEmptyKeyValueTest
 * @run main NullEmptyKeyValueTest
 */

import java.util.*;
import javax.management.*;

public class NullEmptyKeyValueTest {

    private static int createObjectName(int i,
                                        Class c,
                                        String s,
                                        String d,
                                        String k,
                                        String v,
                                        Hashtable<String,String> t)
        throws Exception {

        System.out.println("----------------------------------------------");
        switch (i) {
        case 1:
            System.out.println("ObjectName = " + s);
            break;
        case 2:
            System.out.println("ObjectName.Domain = " + d);
            System.out.println("ObjectName.Key = " + k);
            System.out.println("ObjectName.Value = " + v);
            break;
        case 3:
            System.out.println("ObjectName.Domain = " + d);
            System.out.println("ObjectName.Hashtable = " + t);
            break;
        default:
            throw new Exception("Test incorrect: case: " + i);
        }
        int error = 0;
        ObjectName on = null;
        try {
            switch (i) {
            case 1:
                on = new ObjectName(s);
                break;
            case 2:
                on = new ObjectName(d, k, v);
                break;
            case 3:
                on = new ObjectName(d, t);
                break;
            default:
                throw new Exception("Test incorrect: case: " + i);
            }
            if (c != null) {
                error++;
                System.out.println("Got Unexpected ObjectName = " +
                           (on == null ? "null" : on.getCanonicalName()));
            } else {
                System.out.println("Got Expected ObjectName = " +
                           (on == null ? "null" : on.getCanonicalName()));
            }
        } catch (Exception e) {
            if (c == null || !c.isInstance(e)) {
                error++;
                System.out.println("Got Unexpected Exception = " +
                                   e.toString());
            } else {
                System.out.println("Got Expected Exception = " +
                                   e.toString());
            }
        }
        System.out.println("----------------------------------------------");
        return error;
    }

    private static int createObjectName1(Class c,
                                         String s)
        throws Exception {
        return createObjectName(1, c, s, null, null, null, null);
    }

    private static int createObjectName2(Class c,
                                         String d,
                                         String k,
                                         String v)
        throws Exception {
        return createObjectName(2, c, null, d, k, v, null);
    }

    private static int createObjectName3(Class c,
                                         String d,
                                         Hashtable<String,String> t)
        throws Exception {
        return createObjectName(3, c, null, d, null, null, t);
    }

    public static void main(String[] args) throws Exception {

        final Class npec = NullPointerException.class;
        final Class monec = MalformedObjectNameException.class;

        int error = 0;

        error += createObjectName1(npec, null);
        error += createObjectName1(null, "d:k=v");
        error += createObjectName1(null, ":k=v");
        error += createObjectName1(monec, "d:=v");
        error += createObjectName1(null, "d:k=");
        error += createObjectName1(null, "d:k1=,k2=v2");
        error += createObjectName1(null, "d:k1=v1,k2=");

        error += createObjectName2(npec, null, null, null);
        error += createObjectName2(null, "d", "k", "v");
        error += createObjectName2(npec, null, "k", "v");
        error += createObjectName2(null, "", "k", "v");
        error += createObjectName2(npec, "d", null, "v");
        error += createObjectName2(monec, "d", "", "v");
        error += createObjectName2(npec, "d", "k", null);
        error += createObjectName2(null, "d", "k", "");

        Hashtable<String,String> h1 = new Hashtable<String,String>();
        h1.put("k", "v");
        Hashtable<String,String> h2 = new Hashtable<String,String>();
        h2.put("", "v");
        Hashtable<String,String> h3 = new Hashtable<String,String>();
        h3.put("k", "");
        Hashtable<String,String> h4 = new Hashtable<String,String>();
        h4.put("k1", "");
        h4.put("k2", "v2");
        Hashtable<String,String> h5 = new Hashtable<String,String>();
        h5.put("k1", "v1");
        h5.put("k2", "");
        error += createObjectName3(npec, null, null);
        error += createObjectName3(null, "d", h1);
        error += createObjectName3(npec, null, h1);
        error += createObjectName3(null, "", h1);
        error += createObjectName3(monec, "d", h2);
        error += createObjectName3(null, "d", h3);
        error += createObjectName3(null, "d", h4);
        error += createObjectName3(null, "d", h5);

        if (error > 0) {
            final String msg = "Test FAILED! Got " + error + " error(s)";
            System.out.println(msg);
            throw new IllegalArgumentException(msg);
        } else {
            System.out.println("Test PASSED!");
        }
    }
}
