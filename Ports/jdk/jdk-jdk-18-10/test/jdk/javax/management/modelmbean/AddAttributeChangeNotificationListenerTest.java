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
 * @bug 5043762
 * @summary Attribute existence check fails if
 * RequiredModelMBean.addAttributeChangeNotificationListener
 * is called with a non-existent attribute name and there are
 * no other attributes.
 * @author Yves Joan
 * @author Eamonn McManus
 *
 * @run clean AddAttributeChangeNotificationListenerTest
 * @run build AddAttributeChangeNotificationListenerTest
 * @run main AddAttributeChangeNotificationListenerTest
 */

import java.lang.reflect.Method;
import javax.management.AttributeChangeNotification;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.RuntimeOperationsException;
import javax.management.modelmbean.*;

/**
 * We do invoke addAttributeChangeNotificationListener to add
 * a listener on an attribute not defined in the ModelMBeanInfo
 * of the RequiredModelMBean instance used.
 */
public class AddAttributeChangeNotificationListenerTest {

    public static void main(String args[] ) {
        AddAttributeChangeNotificationListenerTest test =
            new AddAttributeChangeNotificationListenerTest();

        try {
            test.run(args);
        } catch(Exception e) {
            System.out.println("FAIL");
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("PASS");
    }

    private void run( String[] args) throws Exception {
        int errCount = 0;
        String testName = "AddAttributeChangeNotificationListenerTest0001";
        ObjectName modelMBeanObjectName = null;
        ModelMBeanInfo modelMBeanInfo = null;
        MBeanServer mbeanserver = MBeanServerFactory.newMBeanServer();
        String modelMBeanName = "RequiredModelMBean";
        String modelMBeanClassName =
            "javax.management.modelmbean.RequiredModelMBean";

        modelMBeanObjectName =
            new ObjectName("AddAttributeChangeNotificationListenerTest:type=" +
            modelMBeanName);

        System.out.println("Build a ModelMBeanInfo without attribute State");
        modelMBeanInfo = createModelMBeanInfo();

        System.out.println("Create and register a RequiredModelMBean " +
            "with that MBeanInfo");
        Object[] params = { modelMBeanInfo };
        String[] sig = { "javax.management.modelmbean.ModelMBeanInfo" };
        mbeanserver.createMBean(modelMBeanClassName,
            modelMBeanObjectName,
            params,
            sig);

        ModelMBeanListener aListener = new ModelMBeanListener();

        // add an attribute change listener
        System.out.println("Add an attribute change listener for State");
        try {
            mbeanserver.invoke(modelMBeanObjectName,
                "addAttributeChangeNotificationListener",
                (new Object[] {aListener, "State", null}),
                    (new String[] {"javax.management.NotificationListener",
                        "java.lang.String",
                        "java.lang.Object"}));
                        System.out.println("NOK: Did not get expected " +
                            "RuntimeOperationsException");
                        errCount++;
        } catch (Exception e) {
            if (e instanceof MBeanException)
                e = ((MBeanException) e).getTargetException();
            if (e instanceof RuntimeOperationsException) {
                RuntimeOperationsException roe =
                    (RuntimeOperationsException) e;
                Exception target = roe.getTargetException();
                System.out.println("OK: Got expected RuntimeOperationsException");

                if ( target instanceof IllegalArgumentException ) {
                    System.out.println("OK: Got expected " +
                        "wrapped IllegalArgumentException");
                } else {
                    System.out.println("NOK: Got wrapped "
                        + target
                        + " as we expect IllegalArgumentException");
                    errCount++;
                }
            } else {
                System.out.println("NOK: Got "
                    + e
                    + " as we expect RuntimeOperationsException");
                errCount++;
            }
        }

        if ( errCount != 0 )
            throw new Exception(errCount
                + " error(s) occured");
    }

    /**
     * Returns a ModelMBeanInfo with two operations:
     * setManagedResource
     * addAttributeChangeNotificationListener
     */
    private ModelMBeanInfo createModelMBeanInfo() throws Exception {
        // operation setManagedResource
        String descriptionOp1Set = "ManagedResource description setter";
        Class[] paramsSet1 = {Class.forName("java.lang.Object"),
            Class.forName("java.lang.String")};
            Method oper1Set =
                RequiredModelMBean.class.getMethod("setManagedResource",
                paramsSet1);
            ModelMBeanOperationInfo operation1Set =
                new  ModelMBeanOperationInfo(descriptionOp1Set,
                oper1Set);

            // operation addAttributeChangeNotificationListener
            String descriptionop2Set =
                "addAttributeChangeNotificationListener description";
            Class [] paramsSet2 =
            {Class.forName("javax.management.NotificationListener"),
                 Class.forName("java.lang.String"),
                 Class.forName("java.lang.Object")};
                 Method oper2Set =
                     RequiredModelMBean.class.getMethod(
                     "addAttributeChangeNotificationListener",
                     paramsSet2);
                 ModelMBeanOperationInfo operation2Set =
                     new  ModelMBeanOperationInfo(descriptionop2Set,
                     oper2Set);

                 // define ModelMBeanInfo
                 String className = "ModelMBeansInfo";
                 String descriptionmodel = "Model MBean Test";
                 ModelMBeanAttributeInfo[] attributes = null;
                 ModelMBeanOperationInfo[] operations = {
                     operation1Set,
                         operation2Set
                 };
                 ModelMBeanNotificationInfo[] notifications = null;
                 ModelMBeanConstructorInfo[] constructors = null;

                 ModelMBeanInfoSupport modelMBeanInfo =
                     new ModelMBeanInfoSupport(className,
                     descriptionmodel,
                     attributes,
                     constructors,
                     operations,
                     notifications);
                 return modelMBeanInfo;
    }

    public static class ModelMBeanListener implements NotificationListener {

        public ModelMBeanListener() {
            tally = 0;
        }

        public void handleNotification(Notification acn, Object handback) {
            tally++;
        }

        public int getCount() {
            return tally;
        }

        public int setCount(int newTally) {
            tally = newTally;
            return tally;
        }

        private int tally = 0;

    }
}
