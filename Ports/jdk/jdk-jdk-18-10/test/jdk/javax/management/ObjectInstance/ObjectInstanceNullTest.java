/*
 * Copyright (c) 2004, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5015663
 * @summary Test ObjectInstance(name,null).hashCode() and .equals()
 * @author Daniel Fuchs
 *
 * @run clean ObjectInstanceNullTest
 * @run build ObjectInstanceNullTest
 * @run main ObjectInstanceNullTest
 */

import javax.management.*;

public class ObjectInstanceNullTest {

    public static void testEquals(ObjectInstance n1, ObjectInstance n2) {
        try {
            if (!n1.equals(n2) || !n2.equals(n1)) {
                System.err.println("Equals yields false for: "+
                                   "["+n1.getObjectName()+" , "+
                                   n1.getClassName()+"] ["+
                                   n2.getObjectName()+" , "+
                                   n2.getClassName()+"]");
                System.exit(1);
            }
        } catch (Exception x) {
            System.err.println("Equals failed for: "+
                               "["+n1.getObjectName()+" , "+
                               n1.getClassName()+"] ["+
                               n2.getObjectName()+" , "+
                               n2.getClassName()+"]: " + x);
            x.printStackTrace();
            System.exit(2);
        }
        try {
            if (n1.hashCode() != n2.hashCode()) {
                System.err.println("Different hashCode() for: "+
                                   "["+n1.getObjectName()+" , "+
                                   n1.getClassName()+"] ["+
                                   n2.getObjectName()+" , "+
                                   n2.getClassName()+"]");
                System.exit(3);
            }
        } catch (Exception x) {
            System.err.println("Hashcode failed for: "+
                               "["+n1.getObjectName()+" , "+
                               n1.getClassName()+"] ["+
                               n2.getObjectName()+" , "+
                               n2.getClassName()+"]: " + x);
            x.printStackTrace();
            System.exit(4);
        }
    }

    public static void testNotEquals(ObjectInstance n1, ObjectInstance n2) {
        try {
            if (n1.equals(n2) || n2.equals(n1)) {
                System.err.println("Equals yields true for: "+
                                   "["+n1.getObjectName()+" , "+
                                   n1.getClassName()+"] ["+
                                   n2.getObjectName()+" , "+
                                   n2.getClassName()+"]");
                System.exit(5);
            }
        } catch (Exception x) {
            System.err.println("!Equals failed for: "+
                               "["+n1.getObjectName()+" , "+
                               n1.getClassName()+"] ["+
                               n2.getObjectName()+" , "+
                               n2.getClassName()+"]: " + x);
            x.printStackTrace();
            System.exit(6);
        }
        try {
            if (n1.hashCode() == n2.hashCode()) {
                System.out.println("Warning: Same hashCode() for: "+
                                   "["+n1.getObjectName()+" , "+
                                   n1.getClassName()+"] ["+
                                   n2.getObjectName()+" , "+
                                   n2.getClassName()+"]");
            }
        } catch (Exception x) {
            System.err.println("Hashcode failed for: "+
                               "["+n1.getObjectName()+" , "+
                               n1.getClassName()+"] ["+
                               n2.getObjectName()+" , "+
                               n2.getClassName()+"]: " + x);
            x.printStackTrace();
            System.exit(7);
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Test ObjectInstance(name,null).equals() and " +
                           "ObjectInstance(name,null).hashCode()");
        try {
            ObjectName toto1  = new ObjectName("Toto:foo=bar");
            ObjectName toto2  = new ObjectName("Toto:bar=foo");
            ObjectName clone1 = new ObjectName("Toto:bar=foo,cloned=yes");
            ObjectName clone2 = new ObjectName("Toto:cloned=yes,bar=foo");
            ObjectInstance n1 = new ObjectInstance(toto1,null);
            ObjectInstance n2 = new ObjectInstance(toto1,null);
            testEquals(n1,n1);
            testEquals(n1,n2);
            ObjectInstance n3 = new ObjectInstance(toto1,"Object");
            ObjectInstance n4 = new ObjectInstance(toto1,"Object");
            testEquals(n3,n3);
            testEquals(n3,n4);
            testNotEquals(n1,n3);
            ObjectInstance n5 = new ObjectInstance(toto2,null);
            ObjectInstance n6 = new ObjectInstance(toto2,"Object");
            testEquals(n5,n5);
            testEquals(n6,n6);
            testNotEquals(n5,n1);
            testNotEquals(n5,n3);
            testNotEquals(n6,n1);
            testNotEquals(n6,n3);
            testNotEquals(n5,n6);
            ObjectInstance n7 = new ObjectInstance(clone1,null);
            ObjectInstance n8 = new ObjectInstance(clone2,null);
            testEquals(n7,n8);
            testNotEquals(n7,n1);
            testNotEquals(n7,n5);
            ObjectInstance n9  = new ObjectInstance(clone1,"Object");
            ObjectInstance n10 = new ObjectInstance(clone2,"Object");
            testEquals(n9,n10);
            testNotEquals(n9,n1);
            testNotEquals(n9,n5);
            testNotEquals(n9,n7);
            testNotEquals(n9,n8);
            testNotEquals(n10,n1);
            testNotEquals(n10,n5);
            testNotEquals(n10,n7);
            testNotEquals(n10,n8);
        } catch( Exception x) {
            System.err.println("Unexpected exception: " + x);
            x.printStackTrace();
            System.exit(8);
        }
    }

}
