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
 * @bug 6334663
 * @summary Test that TabularDataSupport preserves the order elements were added
 * @author Eamonn McManus
 * @modules java.management/javax.management.openmbean:open
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

public class TabularDataOrderTest {
    private static String failure;

    private static final String COMPAT_PROP_NAME = "jmx.tabular.data.hash.map";

    private static final String[] intNames = {
        "unus", "duo", "tres", "quatuor", "quinque", "sex", "septem",
        "octo", "novem", "decim",
    };
    private static final Map<String, Integer> stringToValue =
            new LinkedHashMap<String, Integer>();
    static {
        for (int i = 0; i < intNames.length; i++)
            stringToValue.put(intNames[i], i + 1);
    }

    public static interface TestMXBean {
        public Map<String, Integer> getMap();
    }

    public static class TestImpl implements TestMXBean {
        public Map<String, Integer> getMap() {
            return stringToValue;
        }
    }

    private static final CompositeType ct;
    private static final TabularType tt;
    static {
        try {
            ct = new CompositeType(
                    "a.b.c", "name and int",
                    new String[] {"name", "int"},
                    new String[] {"name of integer", "value of integer"},
                    new OpenType<?>[] {SimpleType.STRING, SimpleType.INTEGER});
            tt = new TabularType(
                    "d.e.f", "name and int indexed by name", ct,
                    new String[] {"name"});
        } catch (OpenDataException e) {
            throw new AssertionError(e);
        }
    }

    private static TabularData makeTable() throws OpenDataException {
        TabularData td = new TabularDataSupport(tt);
        for (Map.Entry<String, Integer> entry : stringToValue.entrySet()) {
            CompositeData cd = new CompositeDataSupport(
                    ct,
                    new String[] {"name", "int"},
                    new Object[] {entry.getKey(), entry.getValue()});
            td.put(cd);
        }
        return td;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Testing standard behaviour");
        TabularData td = makeTable();
        System.out.println(td);

        // Test that a default TabularData has the order keys were added in
        int last = 0;
        boolean ordered = true;
        for (Object x : td.values()) {
            CompositeData cd = (CompositeData) x;
            String name = (String) cd.get("name");
            int value = (Integer) cd.get("int");
            System.out.println(name + " = " + value);
            if (last + 1 != value)
                ordered = false;
            last = value;
        }
        if (!ordered)
            fail("Order not preserved");

        // Now test the undocumented property that causes HashMap to be used
        // instead of LinkedHashMap, in case serializing to a 1.3 client.
        // We serialize and deserialize in case the implementation handles
        // this at serialization time.  Then we look at object fields; that's
        // not guaranteed to work but at worst it will fail spuriously and
        // we'll have to update the test.
        System.out.println("Testing compatible behaviour");
        System.setProperty(COMPAT_PROP_NAME, "true");
        td = makeTable();
        System.out.println(td);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(bout);
        oout.writeObject(td);
        oout.close();
        byte[] bytes = bout.toByteArray();
        ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
        ObjectInputStream oin = new ObjectInputStream(bin);
        td = (TabularData) oin.readObject();
        boolean found = false;
        for (Field f : td.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()))
                continue;
            f.setAccessible(true);
            Object x = f.get(td);
            if (x != null && x.getClass() == HashMap.class) {
                found = true;
                System.out.println(
                        x.getClass().getName() + " TabularDataSupport." +
                        f.getName() + " = " + x);
                break;
            }
        }
        if (!found) {
            fail("TabularDataSupport does not contain HashMap though " +
                    COMPAT_PROP_NAME + "=true");
        }
        System.clearProperty(COMPAT_PROP_NAME);

        System.out.println("Testing MXBean behaviour");
        MBeanServer mbs = MBeanServerFactory.newMBeanServer();
        ObjectName name = new ObjectName("a:b=c");
        mbs.registerMBean(new TestImpl(), name);
        TestMXBean proxy = JMX.newMXBeanProxy(mbs, name, TestMXBean.class);
        Map<String, Integer> map = proxy.getMap();
        List<String> origNames = new ArrayList<String>(stringToValue.keySet());
        List<String> proxyNames = new ArrayList<String>(map.keySet());
        if (!origNames.equals(proxyNames))
            fail("Order mangled after passage through MXBean: " + proxyNames);

        if (failure == null)
            System.out.println("TEST PASSED");
        else
            throw new Exception("TEST FAILED: " + failure);
    }

    private static void fail(String why) {
        System.out.println("FAILED: " + why);
        failure = why;
    }
}
