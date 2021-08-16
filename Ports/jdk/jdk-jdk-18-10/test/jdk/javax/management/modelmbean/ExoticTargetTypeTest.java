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
 * @bug 4954062
 * @summary Test that a custom ModelMBean implementation can have custom
 * targetType values in its ModelMBeanOperationInfo descriptors.
 * @author Eamonn McManus
 *
 * @run clean ExoticTargetTypeTest
 * @run build ExoticTargetTypeTest
 * @run main ExoticTargetTypeTest
 */

import javax.management.*;
import javax.management.modelmbean.*;

public class ExoticTargetTypeTest {
    public static void main(String[] args) throws Exception {
        System.out.println("Testing that ModelMBeanOperationInfo can contain" +
                           " a nonstandard targetType provided that the " +
                           "ModelMBean understands it");

        boolean ok = true;

        final Descriptor noddyDescr =
            new DescriptorSupport(new String[] {
                "name=noddy",
                "descriptorType=operation",
                "role=operation",
                "targetType=noddy",
            });
        final ModelMBeanOperationInfo opInfo =
            new ModelMBeanOperationInfo("noddy",
                                        "noddy description",
                                        new MBeanParameterInfo[0],
                                        "void",
                                        ModelMBeanOperationInfo.ACTION,
                                        noddyDescr);
        final ModelMBeanInfo mmbInfo =
            new ModelMBeanInfoSupport(ExoticModelMBean.class.getName(),
                                      "Noddy ModelMBean description",
                                      null, null,
                                      new ModelMBeanOperationInfo[] {opInfo},
                                      null);

        System.out.println("Testing nonstandard ModelMBean with nonstandard " +
                           "ModelMBeanOperationInfo...");
        final ExoticModelMBean exoticMMB = new ExoticModelMBean(mmbInfo);
        try {
            test(exoticMMB);
            if (exoticMMB.noddyCalled)
                System.out.println("...OK");
            else {
                System.out.println("...TEST FAILS: invoke worked but did " +
                                   "not invoke method?!");
                ok = false;
            }
        } catch (Exception e) {
            System.out.println("...TEST FAILS: exception:");
            e.printStackTrace(System.out);
            ok = false;
        }

        System.out.println("Testing standard ModelMBean with nonstandard " +
                           "ModelMBeanOperationInfo...");
        final ModelMBean standardMMB = new RequiredModelMBean(mmbInfo);
        // RequiredModelMBean's constructor could legitimately throw
        // an exception here because of the nonstandard descriptor.
        // If some day it does, we will have to update this test.
        try {
            test(standardMMB);
            System.out.println("...TEST FAILS: invoke worked but should not");
            ok = false;
        } catch (MBeanException e) {
            Throwable cause = e.getCause();
            if (cause instanceof InvalidTargetObjectTypeException) {
                System.out.println("...OK: got exception MBeanException/" +
                                   "InvalidTargetObjectTypeException: " +
                                   cause.getMessage());
            } else {
                System.out.println("...TEST FAILS: got MBeanException with " +
                                   "wrong cause: " + cause);
                ok = false;
            }
        } catch (Exception e) {
            System.out.println("...TEST FAILS: exception:");
            e.printStackTrace(System.out);
            ok = false;
        }

        if (ok)
            System.out.println("Test passed");
        else {
            System.out.println("TEST FAILED");
            System.exit(1);
        }
    }

    private static void test(ModelMBean mmb) throws Exception {
        final MBeanServer mbs = MBeanServerFactory.createMBeanServer();
        final ObjectName name = new ObjectName("d:k=v");
        mbs.registerMBean(mmb, name);
        try {
            mbs.invoke(name, "noddy", null, null);
        } finally {
            mbs.unregisterMBean(name);
        }
    }

    public static class ExoticModelMBean extends RequiredModelMBean {
        public ExoticModelMBean(ModelMBeanInfo mmbInfo) throws MBeanException {
            super();
            this.mmbInfo = mmbInfo;
        }

        public Object invoke(String opName, Object[] opArgs, String[] sig)
                throws MBeanException, ReflectionException {
            if (opName.equals("noddy")) {
                noddyCalled = true;
                return null;
            } else
                throw new IllegalArgumentException("Not noddy: " + opName);
        }

        private boolean noddyCalled;
        private final ModelMBeanInfo mmbInfo;
    }
}
