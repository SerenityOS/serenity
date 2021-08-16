/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4947001 4954369 4954409 4954410
 * @summary Test that "Boolean isX()" and "int isX()" define operations
 * @author Eamonn McManus
 *
 * @run clean IsMethodTest
 * @run build IsMethodTest
 * @run main IsMethodTest
 */

import javax.management.*;

/*
   This regression test covers a slew of bugs in Standard MBean
   reflection.  Lots of corner cases were incorrect:

   In the MBeanInfo for a Standard MBean:
   - Boolean isX() defined an attribute as if it were boolean isX()
   - int isX() defined neither an attribute nor an operation

   When calling MBeanServer.getAttribute:
   - int get() and void getX() were considered attributes even though they
     were operations in MBeanInfo

   When calling MBeanServer.invoke:
   - Boolean isX() could not be called because it was (consistently with
     MBeanInfo) considered an attribute, not an operation
*/
public class IsMethodTest {
    public static void main(String[] args) throws Exception {
        System.out.println("Test that Boolean isX() and int isX() both " +
                           "define operations not attributes");

        MBeanServer mbs = MBeanServerFactory.createMBeanServer();
        Object mb = new IsMethod();
        ObjectName on = new ObjectName("a:b=c");
        mbs.registerMBean(mb, on);
        MBeanInfo mbi = mbs.getMBeanInfo(on);

        boolean ok = true;

        MBeanAttributeInfo[] attrs = mbi.getAttributes();
        if (attrs.length == 0)
            System.out.println("OK: MBean defines 0 attributes");
        else {
            ok = false;
            System.out.println("TEST FAILS: MBean should define 0 attributes");
            for (int i = 0; i < attrs.length; i++) {
                System.out.println("  " + attrs[i].getType() + " " +
                                   attrs[i].getName());
            }
        }

        MBeanOperationInfo[] ops = mbi.getOperations();
        if (ops.length == 4)
            System.out.println("OK: MBean defines 4 operations");
        else {
            ok = false;
            System.out.println("TEST FAILS: MBean should define 4 operations");
        }
        for (int i = 0; i < ops.length; i++) {
            System.out.println("  " + ops[i].getReturnType() + " " +
                               ops[i].getName());
        }

        final String[] bogusAttrNames = {"", "Lost", "Thingy", "Whatsit"};
        for (int i = 0; i < bogusAttrNames.length; i++) {
            final String bogusAttrName = bogusAttrNames[i];
            try {
                mbs.getAttribute(on, bogusAttrName);
                ok = false;
                System.out.println("TEST FAILS: getAttribute(\"" +
                                   bogusAttrName + "\") should not work");
            } catch (AttributeNotFoundException e) {
                System.out.println("OK: getAttribute(" + bogusAttrName +
                                   ") got exception as expected");
            }
        }

        final String[] opNames = {"get", "getLost", "isThingy", "isWhatsit"};
        for (int i = 0; i < opNames.length; i++) {
            final String opName = opNames[i];
            try {
                mbs.invoke(on, opName, new Object[0], new String[0]);
                System.out.println("OK: invoke(\"" + opName + "\") worked");
            } catch (Exception e) {
                ok = false;
                System.out.println("TEST FAILS: invoke(" + opName +
                                   ") got exception: " + e);
            }
        }

        if (ok)
            System.out.println("Test passed");
        else {
            System.out.println("TEST FAILED");
            System.exit(1);
        }
    }

    public static interface IsMethodMBean {
        public int get();
        public void getLost();
        public Boolean isThingy();
        public int isWhatsit();
    }

    public static class IsMethod implements IsMethodMBean {
        public int get() {
            return 0;
        }

        public void getLost() {
        }

        public Boolean isThingy() {
            return Boolean.TRUE;
        }

        public int isWhatsit() {
            return 0;
        }
    }
}
