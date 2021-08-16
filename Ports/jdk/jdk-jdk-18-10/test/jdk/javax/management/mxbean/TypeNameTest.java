/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6757225 6763051
 * @summary Test that type names in MXBeans match their spec.
 * @author Eamonn McManus
 */

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularType;

public class TypeNameTest {
    public static interface TestMXBean {
        public int getInt();
        public String IntName = "int";

        public Map<String, Integer> getMapSI();
        public String MapSIName = "java.util.Map<java.lang.String, java.lang.Integer>";

        public Map<String, int[]> getMapSInts();
        public String MapSIntsName = "java.util.Map<java.lang.String, int[]>";

        public List<List<int[]>> getListListInts();
        public String ListListIntsName = "java.util.List<java.util.List<int[]>>";
    }

    private static InvocationHandler nullIH = new InvocationHandler() {
        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            return null;
        }
    };

    static volatile String failure;

    public static void main(String[] args) throws Exception {
        TestMXBean testImpl = (TestMXBean) Proxy.newProxyInstance(
                TestMXBean.class.getClassLoader(), new Class<?>[] {TestMXBean.class}, nullIH);
        Object mxbean = new StandardMBean(testImpl, TestMXBean.class, true);
        MBeanServer mbs = MBeanServerFactory.newMBeanServer();
        ObjectName name = new ObjectName("a:b=c");
        mbs.registerMBean(mxbean, name);
        MBeanInfo mbi = mbs.getMBeanInfo(name);
        MBeanAttributeInfo[] mbais = mbi.getAttributes();
        boolean sawTabular = false;
        for (MBeanAttributeInfo mbai : mbais) {
            String attrName = mbai.getName();
            String attrTypeName = (String) mbai.getDescriptor().getFieldValue("originalType");
            String fieldName = attrName + "Name";
            Field nameField = TestMXBean.class.getField(fieldName);
            String expectedTypeName = (String) nameField.get(null);

            if (expectedTypeName.equals(attrTypeName)) {
                System.out.println("OK: " + attrName + ": " + attrTypeName);
            } else {
                fail("For attribute " + attrName + " expected type name \"" +
                        expectedTypeName + "\", found type name \"" + attrTypeName +
                        "\"");
            }

            if (mbai.getType().equals(TabularData.class.getName())) {
                sawTabular = true;
                TabularType tt = (TabularType) mbai.getDescriptor().getFieldValue("openType");
                if (tt.getTypeName().equals(attrTypeName)) {
                    System.out.println("OK: TabularType name for " + attrName);
                } else {
                    fail("For attribute " + attrName + " expected TabularType " +
                            "name \"" + attrTypeName + "\", found \"" +
                            tt.getTypeName());
                }
            }
        }

        if (!sawTabular)
            fail("Test bug: did not test TabularType name");

        if (failure == null)
            System.out.println("TEST PASSED");
        else
            throw new Exception("TEST FAILED: " + failure);
    }

    private static void fail(String why) {
        System.out.println("FAIL: " + why);
        failure = why;
    }
}
