/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8058865
 * @summary Checks correct collection of MXBean's class after unregistration
 * @requires vm.opt.final.ClassUnloading
 * @author Olivier Lagneau
 *
 * @library /lib/testlibrary
 *
 * @run main/othervm/timeout=300 MXBeanLoadingTest1
 */

import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import javax.management.Attribute;
import javax.management.JMX;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MXBean;
import javax.management.ObjectName;
import javax.management.loading.PrivateMLet;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

public class MXBeanLoadingTest1 {

    public static void main(String[] args) throws Exception {
        MXBeanLoadingTest1 test = new MXBeanLoadingTest1();
        test.run((Map<String, Object>)null);
    }


    public void run(Map<String, Object> args) {

        System.out.println("MXBeanLoadingTest1::run: Start") ;

        try {
            System.out.println("We ensure no reference is retained on MXBean class"
                    + " after it is unregistered. We take time to perform"
                    + " some little extra check of Descriptors, MBean*Info.");

            ClassLoader myClassLoader = MXBeanLoadingTest1.class.getClassLoader();
            if(myClassLoader == null)
                throw new RuntimeException("Test Failed : Null Classloader for test");
            URL url = myClassLoader.getResource(
                    MXBeanLoadingTest1.class.getCanonicalName()
                            .replace(".", "/") + ".class");
            String clsLoadPath = url.toURI().toString().
                    replaceAll(MXBeanLoadingTest1.class.getSimpleName()
                            + ".class", "");

            URL[] urls = new URL[]{new URL(clsLoadPath)};
            PrivateMLet mlet = new PrivateMLet(urls, null, false);
            Class<?> shadowClass = mlet.loadClass(TestMXBean.class.getName());

            if (shadowClass == TestMXBean.class) {
                String message = "(ERROR) MLet got original TestMXBean, not shadow";
                System.out.println(message);
                throw new RuntimeException(message);
            }
            shadowClass = null;

            MBeanServer mbs = MBeanServerFactory.createMBeanServer();
            ObjectName mletName = new ObjectName("x:type=mlet");
            mbs.registerMBean(mlet, mletName);

            ObjectName testName = new ObjectName("x:type=test");
            mbs.createMBean(Test.class.getName(), testName, mletName);

            // That test fails because the MXBean instance is accessed via
            // a delegate OpenMBean which has
            ClassLoader testLoader = mbs.getClassLoaderFor(testName);

            if (testLoader != mlet) {
                System.out.println("MLet " + mlet);
                String message = "(ERROR) MXBean's class loader is not MLet: "
                        + testLoader;
                System.out.println(message);
                throw new RuntimeException(message);
            }
            testLoader = null;


            // Cycle get/set/get of the attribute of type Luis.
            // We check the set is effective.
            CompositeData cd_B = (CompositeData)mbs.getAttribute(testName, "B");
            CompositeType compType_B = cd_B.getCompositeType();

            CompositeDataSupport cds_B =
                    new CompositeDataSupport(compType_B,
                    new String[]{"something"},
                    new Object[]{Integer.valueOf(13)});
            Attribute myAtt = new Attribute("B",  cds_B);
            mbs.setAttribute(testName, myAtt);

            CompositeData cd_B2 = (CompositeData)mbs.getAttribute(testName, "B");

            if ( ((Integer)cd_B2.get("something")).intValue() != 13 ) {
                String message = "(ERROR) The setAttribute of att B did not work;"
                        + " expect Luis.something = 13 but got "
                        + cd_B2.get("something");
                System.out.println(message);
                throw new RuntimeException(message);
            }

            MBeanInfo info = mbs.getMBeanInfo(testName);
            String mxbeanField =
                    (String)info.getDescriptor().getFieldValue(JMX.MXBEAN_FIELD);

            if ( mxbeanField == null || ! mxbeanField.equals("true")) {
                String message = "(ERROR) Improper mxbean field value "
                        + mxbeanField;
                System.out.println(message);
                throw new RuntimeException(message);
            }

            // Check the 2 attributes.
            MBeanAttributeInfo[] attrs = info.getAttributes();

            if ( attrs.length == 2 ) {
                for (MBeanAttributeInfo mbai : attrs) {
                    String originalTypeFieldValue =
                            (String)mbai.getDescriptor().getFieldValue(JMX.ORIGINAL_TYPE_FIELD);
                    OpenType<?> openTypeFieldValue =
                            (OpenType<?>)mbai.getDescriptor().getFieldValue(JMX.OPEN_TYPE_FIELD);

                    if ( mbai.getName().equals("A") ) {
                        if ( !mbai.isReadable() || !mbai.isWritable()
                        || mbai.isIs()
                        || !mbai.getType().equals("int") ) {
                            String message = "(ERROR) Unexpected MBeanAttributeInfo for A "
                                    + mbai;
                            System.out.println(message);
                            throw new RuntimeException(message);
                        }

                        if ( ! originalTypeFieldValue.equals("int") ) {
                            String message = "(ERROR) Unexpected originalType in Descriptor for A "
                                    + originalTypeFieldValue;
                            System.out.println(message);
                            throw new RuntimeException(message);
                        }

                        if ( ! openTypeFieldValue.equals(SimpleType.INTEGER) ) {
                            String message = "(ERROR) Unexpected openType in Descriptor for A "
                                    + originalTypeFieldValue;
                            System.out.println(message);
                            throw new RuntimeException(message);
                        }
                    } else if ( mbai.getName().equals("B") ) {
                        if ( !mbai.isReadable() || !mbai.isWritable()
                        || mbai.isIs()
                        || !mbai.getType().equals("javax.management.openmbean.CompositeData") ) {
                            String message = "(ERROR) Unexpected MBeanAttributeInfo for B "
                                    + mbai;
                            System.out.println(message);
                            throw new RuntimeException(message);
                        }

                        if ( ! originalTypeFieldValue.equals(Luis.class.getName()) ) {
                            String message = "(ERROR) Unexpected originalType in Descriptor for B "
                                    + originalTypeFieldValue;
                            System.out.println(message);
                            throw new RuntimeException(message);
                        }

                        if ( ! openTypeFieldValue.equals(compType_B) ) {
                            String message = "(ERROR) Unexpected openType in Descriptor for B "
                                    + compType_B;
                            System.out.println(message);
                            throw new RuntimeException(message);
                        }
                    } else {
                        String message = "(ERROR) Unknown attribute name";
                        System.out.println(message);
                        throw new RuntimeException(message);
                    }
                }
            } else {
                String message = "(ERROR) Unexpected MBeanAttributeInfo array"
                        + Arrays.deepToString(attrs);
                System.out.println(message);
                throw new RuntimeException(message);
            }

            // Check the MXBean operation.
            MBeanOperationInfo[] ops = info.getOperations();
            // The impact is ACTION_INFO as for a standard MBean it is UNKNOWN,
            // logged 6320104.
            if (ops.length != 1 || !ops[0].getName().equals("bogus")
            || ops[0].getSignature().length > 0
                    || !ops[0].getReturnType().equals("void")) {
                String message = "(ERROR) Unexpected MBeanOperationInfo array "
                        + Arrays.deepToString(ops);
                System.out.println(message);
                throw new RuntimeException(message);
            }

            String originalTypeFieldValue =
                    (String)ops[0].getDescriptor().getFieldValue(JMX.ORIGINAL_TYPE_FIELD);
            OpenType<?> openTypeFieldValue =
                    (OpenType<?>)ops[0].getDescriptor().getFieldValue(JMX.OPEN_TYPE_FIELD);

            if ( ! originalTypeFieldValue.equals("void") ) {
                String message = "(ERROR) Unexpected originalType in Descriptor for bogus "
                        + originalTypeFieldValue;
                System.out.println(message);
                throw new RuntimeException(message);
            }

            if ( ! openTypeFieldValue.equals(SimpleType.VOID) ) {
                String message = "(ERROR) Unexpected openType in Descriptor for bogus "
                        + originalTypeFieldValue;
                System.out.println(message);
                throw new RuntimeException(message);
            }

            // Check there is 2 constructors.
            if (info.getConstructors().length != 2) {
                String message = "(ERROR) Wrong number of constructors " +
                        "in introspected bean: " +
                        Arrays.asList(info.getConstructors());
                System.out.println(message);
                throw new RuntimeException(message);
            }

            // Check MXBean class name.
            if (!info.getClassName().endsWith("Test")) {
                String message = "(ERROR) Wrong info class name: " +
                        info.getClassName();
                System.out.println(message);
                throw new RuntimeException(message);
            }

            mbs.unregisterMBean(testName);
            mbs.unregisterMBean(mletName);

            WeakReference<PrivateMLet> mletRef =
                    new WeakReference<PrivateMLet>(mlet);
            mlet = null;

            System.out.println("MXBean registered and unregistered, waiting for " +
                    "garbage collector to collect class loader");

            for (int i = 0; i < 10000 && mletRef.get() != null; i++) {
                System.gc();
                Thread.sleep(1);
            }

            if (mletRef.get() == null)
                System.out.println("(OK) class loader was GC'd");
            else {
                String message = "(ERROR) Class loader was not GC'd";
                System.out.println(message);
                throw new RuntimeException(message);
            }
        } catch(Exception e) {
            Utils.printThrowable(e, true) ;
            throw new RuntimeException(e);
        }

        System.out.println("MXBeanLoadingTest1::run: Done without any error") ;
    }


    // I agree the use of the MXBean annotation and the MXBean suffix for the
    // interface name are redundant but however harmless.
    //
    @MXBean(true)
    public static interface TestMXBean {
        public void bogus();
        public int getA();
        public void setA(int a);
        public Luis getB();
        public void setB(Luis mi);
    }


    public static class Test implements TestMXBean {
        private Luis luis = new Luis() ;
        public Test() {}
        public Test(int x) {}

        public void bogus() {}
        public int getA() {return 0;}
        public void setA(int a) {}
        public Luis getB() {return this.luis;}
        public void setB(Luis luis) {this.luis = luis;}
    }


    public static class Luis {
        private int something = 0;
        public Luis() {}
        public int getSomething() {return something;}
        public void setSomething(int v) {something = v;}
        public void doNothing() {}
    }
}
