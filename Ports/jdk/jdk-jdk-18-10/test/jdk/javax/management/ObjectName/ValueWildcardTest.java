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
 * @bug 4716807
 * @summary Test wildcards in ObjectName key properties value part.
 * @author Luis-Miguel Alventosa
 *
 * @run clean ValueWildcardTest
 * @run build ValueWildcardTest
 * @run main ValueWildcardTest
 */

import java.util.Hashtable;
import javax.management.ObjectName;

public class ValueWildcardTest {

    private static int createObjectName(int i,
                                        String s,
                                        String d,
                                        String k,
                                        String v,
                                        Hashtable<String,String> t,
                                        boolean plp,
                                        boolean pvp)
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
            System.out.println("Got Expected ObjectName = " +
                               on.getCanonicalName());
            boolean isPattern = on.isPattern();
            boolean isDomainPattern = on.isDomainPattern();
            boolean isPropertyPattern = on.isPropertyPattern();
            boolean isPropertyListPattern = on.isPropertyListPattern();
            boolean isPropertyValuePattern = on.isPropertyValuePattern();
            System.out.println("ObjectName.isPattern = " +
                               isPattern);
            System.out.println("ObjectName.isDomainPattern = " +
                               isDomainPattern);
            System.out.println("ObjectName.isPropertyPattern = " +
                               isPropertyPattern);
            System.out.println("ObjectName.isPropertyListPattern = " +
                               isPropertyListPattern);
            System.out.println("ObjectName.isPropertyValuePattern = " +
                               isPropertyValuePattern);
            int error2 = 0;
            if (isDomainPattern) {
                error2++;
                System.out.println("Error: Shouldn't be domain pattern!");
            }
            if (!plp && isPropertyListPattern) {
                error2++;
                System.out.println("Error: Shouldn't be property list pattern!");
            }
            if (!pvp && isPropertyValuePattern) {
                error2++;
                System.out.println("Error: Shouldn't be property value pattern!");
            }
            if (plp &&
                !isPattern && !isPropertyPattern && !isPropertyListPattern) {
                error2++;
                System.out.println("Error: Should be property list pattern!");
            }
            if (pvp &&
                !isPattern && !isPropertyPattern && !isPropertyValuePattern) {
                error2++;
                System.out.println("Error: Should be property value pattern!");
            }
            if (error2 > 0) {
                error++;
                System.out.println("Test failed!");
            } else {
                System.out.println("Test passed!");
            }
        } catch (Exception e) {
            error++;
            System.out.println("Got Unexpected Exception = " + e.toString());
        }
        System.out.println("----------------------------------------------");
        return error;
    }

    private static int createObjectName1(String s,
                                         boolean plp,
                                         boolean pvp)
        throws Exception {
        return createObjectName(1, s, null, null, null, null, plp, pvp);
    }

    private static int createObjectName2(String d,
                                         String k,
                                         String v,
                                         boolean plp,
                                         boolean pvp)
        throws Exception {
        return createObjectName(2, null, d, k, v, null, plp, pvp);
    }

    private static int createObjectName3(String d,
                                         Hashtable<String,String> t,
                                         boolean plp,
                                         boolean pvp)
        throws Exception {
        return createObjectName(3, null, d, null, null, t, plp, pvp);
    }

    public static void main(String[] args) throws Exception {

        int error = 0;

        error += createObjectName1("d:k=*", false, true);
        error += createObjectName1("d:k=a*b", false, true);
        error += createObjectName1("d:k=a*b,*", true, true);
        error += createObjectName1("d:*,k=a*b", true, true);

        error += createObjectName1("d:k=?", false, true);
        error += createObjectName1("d:k=a?b", false, true);
        error += createObjectName1("d:k=a?b,*", true, true);
        error += createObjectName1("d:*,k=a?b", true, true);

        error += createObjectName1("d:k=?*", false, true);
        error += createObjectName1("d:k=a?bc*d", false, true);
        error += createObjectName1("d:k=a?bc*d,*", true, true);
        error += createObjectName1("d:*,k=a?bc*d", true, true);

        error += createObjectName1("d:k1=?,k2=*", false, true);
        error += createObjectName1("d:k1=a?b,k2=c*d", false, true);
        error += createObjectName1("d:k1=a?b,k2=c*d,*", true, true);
        error += createObjectName1("d:*,k1=a?b,k2=c*d", true, true);

        error += createObjectName1("d:k=\"*\"", false, true);
        error += createObjectName1("d:k=\"a*b\"", false, true);
        error += createObjectName1("d:k=\"a*b\",*", true, true);
        error += createObjectName1("d:*,k=\"a*b\"", true, true);

        error += createObjectName1("d:k=\"?\"", false, true);
        error += createObjectName1("d:k=\"a?b\"", false, true);
        error += createObjectName1("d:k=\"a?b\",*", true, true);
        error += createObjectName1("d:*,k=\"a?b\"", true, true);

        error += createObjectName1("d:k=\"?*\"", false, true);
        error += createObjectName1("d:k=\"a?bc*d\"", false, true);
        error += createObjectName1("d:k=\"a?bc*d\",*", true, true);
        error += createObjectName1("d:*,k=\"a?bc*d\"", true, true);

        error += createObjectName1("d:k1=\"?\",k2=\"*\"", false, true);
        error += createObjectName1("d:k1=\"a?b\",k2=\"c*d\"", false, true);
        error += createObjectName1("d:k1=\"a?b\",k2=\"c*d\",*", true, true);
        error += createObjectName1("d:*,k1=\"a?b\",k2=\"c*d\"", true, true);

        error += createObjectName2("d", "k", "*", false, true);
        error += createObjectName2("d", "k", "a*b", false, true);
        error += createObjectName2("d", "k", "?", false, true);
        error += createObjectName2("d", "k", "a?b", false, true);
        error += createObjectName2("d", "k", "?*", false, true);
        error += createObjectName2("d", "k", "a?bc*d", false, true);

        error += createObjectName2("d", "k", "\"*\"", false, true);
        error += createObjectName2("d", "k", "\"a*b\"", false, true);
        error += createObjectName2("d", "k", "\"?\"", false, true);
        error += createObjectName2("d", "k", "\"a?b\"", false, true);
        error += createObjectName2("d", "k", "\"?*\"", false, true);
        error += createObjectName2("d", "k", "\"a?bc*d\"", false, true);

        Hashtable<String,String> h1 = new Hashtable<String,String>();
        h1.put("k", "*");
        error += createObjectName3("d", h1, false, true);
        Hashtable<String,String> h2 = new Hashtable<String,String>();
        h2.put("k", "a*b");
        error += createObjectName3("d", h2, false, true);

        Hashtable<String,String> h3 = new Hashtable<String,String>();
        h3.put("k", "?");
        error += createObjectName3("d", h3, false, true);
        Hashtable<String,String> h4 = new Hashtable<String,String>();
        h4.put("k", "a?b");
        error += createObjectName3("d", h4, false, true);

        Hashtable<String,String> h5 = new Hashtable<String,String>();
        h5.put("k", "?*");
        error += createObjectName3("d", h5, false, true);
        Hashtable<String,String> h6 = new Hashtable<String,String>();
        h6.put("k", "a?bc*d");
        error += createObjectName3("d", h6, false, true);

        Hashtable<String,String> h7 = new Hashtable<String,String>();
        h7.put("k1", "?");
        h7.put("k2", "*");
        error += createObjectName3("d", h7, false, true);
        Hashtable<String,String> h8 = new Hashtable<String,String>();
        h8.put("k1", "a?b");
        h8.put("k2", "c*d");
        error += createObjectName3("d", h8, false, true);

        Hashtable<String,String> h9 = new Hashtable<String,String>();
        h9.put("k", "\"*\"");
        error += createObjectName3("d", h9, false, true);
        Hashtable<String,String> h10 = new Hashtable<String,String>();
        h10.put("k", "\"a*b\"");
        error += createObjectName3("d", h10, false, true);

        Hashtable<String,String> h11 = new Hashtable<String,String>();
        h11.put("k", "\"?\"");
        error += createObjectName3("d", h11, false, true);
        Hashtable<String,String> h12 = new Hashtable<String,String>();
        h12.put("k", "\"a?b\"");
        error += createObjectName3("d", h12, false, true);

        Hashtable<String,String> h13 = new Hashtable<String,String>();
        h13.put("k", "\"?*\"");
        error += createObjectName3("d", h13, false, true);
        Hashtable<String,String> h14 = new Hashtable<String,String>();
        h14.put("k", "\"a?bc*d\"");
        error += createObjectName3("d", h14, false, true);

        Hashtable<String,String> h15 = new Hashtable<String,String>();
        h15.put("k1", "\"?\"");
        h15.put("k2", "\"*\"");
        error += createObjectName3("d", h15, false, true);
        Hashtable<String,String> h16 = new Hashtable<String,String>();
        h16.put("k1", "\"a?b\"");
        h16.put("k2", "\"c*d\"");
        error += createObjectName3("d", h16, false, true);

        if (error > 0) {
            final String msg = "Test FAILED! Got " + error + " error(s)";
            System.out.println(msg);
            throw new IllegalArgumentException(msg);
        } else {
            System.out.println("Test PASSED!");
        }
    }
}
