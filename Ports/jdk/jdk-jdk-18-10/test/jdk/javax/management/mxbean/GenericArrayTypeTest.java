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
 * @bug 6292705
 * @summary Test support for arrays in parameterized types.
 * @author Luis-Miguel Alventosa
 * @key intermittent
 * @modules java.management.rmi
 * @run clean GenericArrayTypeTest
 * @run build GenericArrayTypeTest
 * @run main GenericArrayTypeTest
 */

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.management.Attribute;
import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

public class GenericArrayTypeTest {
    // A version of java.lang.management.MonitorInfo so we can run this test
    // on JDK 5, where that class didn't exist.
    public static class MonitorInfo {
        private final String className;
        private final int identityHashCode;
        private final int lockedStackDepth;
        private final StackTraceElement lockedStackFrame;

        public MonitorInfo(
                String className, int identityHashCode,
                int lockedStackDepth, StackTraceElement lockedStackFrame) {
            this.className = className;
            this.identityHashCode = identityHashCode;
            this.lockedStackDepth = lockedStackDepth;
            this.lockedStackFrame = lockedStackFrame;
        }

        public static MonitorInfo from(CompositeData cd) {
            try {
                CompositeData stecd = (CompositeData) cd.get("lockedStackFrame");
                StackTraceElement ste = new StackTraceElement(
                        (String) stecd.get("className"),
                        (String) stecd.get("methodName"),
                        (String) stecd.get("fileName"),
                        (Integer) stecd.get("lineNumber"));
                return new MonitorInfo(
                        (String) cd.get("className"),
                        (Integer) cd.get("identityHashCode"),
                        (Integer) cd.get("lockedStackDepth"),
                        ste);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public String getClassName() {
            return className;
        }

        public int getIdentityHashCode() {
            return identityHashCode;
        }

        public int getLockedStackDepth() {
            return lockedStackDepth;
        }

        public StackTraceElement getLockedStackFrame() {
            return lockedStackFrame;
        }
    }


    public interface TestMXBean {

        public String[] getT1();
        public void setT1(String[] v);

        public MonitorInfo[] getT2();
        public void setT2(MonitorInfo[] v);

        public Map<String,String[]> getT3();
        public void setT3(Map<String,String[]> v);

        public Map<String,MonitorInfo[]> getT4();
        public void setT4(Map<String,MonitorInfo[]> v);

        public Set<String[]> getT5();
        public void setT5(Set<String[]> v);

        public Set<MonitorInfo[]> getT6();
        public void setT6(Set<MonitorInfo[]> v);

        public List<String[]> getT7();
        public void setT7(List<String[]> v);

        public List<MonitorInfo[]> getT8();
        public void setT8(List<MonitorInfo[]> v);

        public Set<List<String[]>> getT9();
        public void setT9(Set<List<String[]>> v);

        public Set<List<MonitorInfo[]>> getT10();
        public void setT10(Set<List<MonitorInfo[]>> v);

        public Map<String,Set<List<String[]>>> getT11();
        public void setT11(Map<String,Set<List<String[]>>> v);

        public Map<String,Set<List<MonitorInfo[]>>> getT12();
        public void setT12(Map<String,Set<List<MonitorInfo[]>>> v);
    }

    public static class Test implements TestMXBean {

        public String[] getT1() {
            return t1;
        }
        public void setT1(String[] v) {
            t1 = v;
        }
        private String[] t1;

        public MonitorInfo[] getT2() {
            return t2;
        }
        public void setT2(MonitorInfo[] v) {
            t2 = v;
        }
        private MonitorInfo[] t2;

        public Map<String,String[]> getT3() {
            return t3;
        }
        public void setT3(Map<String,String[]> v) {
            t3 = v;
        }
        private Map<String,String[]> t3;

        public Map<String,MonitorInfo[]> getT4() {
            return t4;
        }
        public void setT4(Map<String,MonitorInfo[]> v) {
            t4 = v;
        }
        private Map<String,MonitorInfo[]> t4;

        public Set<String[]> getT5() {
            return t5;
        }
        public void setT5(Set<String[]> v) {
            t5 = v;
        }
        private Set<String[]> t5;

        public Set<MonitorInfo[]> getT6() {
            return t6;
        }
        public void setT6(Set<MonitorInfo[]> v) {
            t6 = v;
        }
        private Set<MonitorInfo[]> t6;

        public List<String[]> getT7() {
            return t7;
        }
        public void setT7(List<String[]> v) {
            t7 = v;
        }
        private List<String[]> t7;

        public List<MonitorInfo[]> getT8() {
            return t8;
        }
        public void setT8(List<MonitorInfo[]> v) {
            t8 = v;
        }
        private List<MonitorInfo[]> t8;

        public Set<List<String[]>> getT9() {
            return t9;
        }
        public void setT9(Set<List<String[]>> v) {
            t9 = v;
        }
        private Set<List<String[]>> t9;

        public Set<List<MonitorInfo[]>> getT10() {
            return t10;
        }
        public void setT10(Set<List<MonitorInfo[]>> v) {
            t10 = v;
        }
        private Set<List<MonitorInfo[]>> t10;

        public Map<String,Set<List<String[]>>> getT11() {
            return t11;
        }
        public void setT11(Map<String,Set<List<String[]>>> v) {
            t11 = v;
        }
        private Map<String,Set<List<String[]>>> t11;

        public Map<String,Set<List<MonitorInfo[]>>> getT12() {
            return t12;
        }
        public void setT12(Map<String,Set<List<MonitorInfo[]>>> v) {
            t12 = v;
        }
        private Map<String,Set<List<MonitorInfo[]>>> t12;
    }

    public static void main(String[] args) throws Exception {

        int error = 0;
        JMXConnector cc = null;
        JMXConnectorServer cs = null;

        try {
            // Instantiate the MBean server
            //
            echo("\n>>> Create the MBean server");
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

            // Get default domain
            //
            echo("\n>>> Get the MBean server's default domain");
            String domain = mbs.getDefaultDomain();
            echo("\tDefault Domain = " + domain);

            // Register TestMXBean
            //
            echo("\n>>> Register TestMXBean");
            ObjectName name =
                ObjectName.getInstance(domain + ":type=" + TestMXBean.class);
            mbs.createMBean(Test.class.getName(), name);

            // Create an RMI connector server
            //
            echo("\n>>> Create an RMI connector server");
            JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://");
            cs =
                JMXConnectorServerFactory.newJMXConnectorServer(url, null, mbs);

            // Start the RMI connector server
            //
            echo("\n>>> Start the RMI connector server");
            cs.start();

            // Create an RMI connector client
            //
            echo("\n>>> Create an RMI connector client");
            cc = JMXConnectorFactory.connect(cs.getAddress(), null);
            MBeanServerConnection mbsc = cc.getMBeanServerConnection();

            // Create TestMXBean proxy
            //
            echo("\n>>> Create the TestMXBean proxy");
            TestMXBean test = JMX.newMXBeanProxy(mbsc, name, TestMXBean.class);

            // Play with proxy getters and setters
            //
            echo("\n>>> Play with proxy getters and setters");
            String[] t1 = new String[] { "t1" };
            MonitorInfo[] t2 = new MonitorInfo[] {
                new MonitorInfo("dummy", 0, 0,
                    new StackTraceElement("dummy", "dummy", "dummy", 0)) };
            Map<String,String[]> t3 = new HashMap<String,String[]>();
            t3.put("key", t1);
            Map<String,MonitorInfo[]> t4 = new HashMap<String,MonitorInfo[]>();
            t4.put("key", t2);
            Set<String[]> t5 = new HashSet<String[]>();
            t5.add(t1);
            Set<MonitorInfo[]> t6 = new HashSet<MonitorInfo[]>();
            t6.add(t2);
            List<String[]> t7 = new ArrayList<String[]>();
            t7.add(t1);
            List<MonitorInfo[]> t8 = new ArrayList<MonitorInfo[]>();
            t8.add(t2);
            Set<List<String[]>> t9 = new HashSet<List<String[]>>();
            t9.add(t7);
            Set<List<MonitorInfo[]>> t10 = new HashSet<List<MonitorInfo[]>>();
            t10.add(t8);
            Map<String,Set<List<String[]>>> t11 = new HashMap<String,Set<List<String[]>>>();
            t11.put("key", t9);
            Map<String,Set<List<MonitorInfo[]>>> t12 = new HashMap<String,Set<List<MonitorInfo[]>>>();
            t12.put("key", t10);

            test.setT1(t1);
            test.setT2(t2);
            test.setT3(t3);
            test.setT4(t4);
            test.setT5(t5);
            test.setT6(t6);
            test.setT7(t7);
            test.setT8(t8);
            test.setT9(t9);
            test.setT10(t10);
            test.setT11(t11);
            test.setT12(t12);

            String r;
            String e1 = "t1";
            String e2 = "dummy";
            echo("\tT1 = " + test.getT1());
            r = ((String[])test.getT1())[0];
            echo("\tR1 = " + r);
            if (!e1.equals(r)) error++;
            echo("\tT2 = " + test.getT2());
            r = ((MonitorInfo[])test.getT2())[0].getClassName();
            echo("\tR2 = " + r);
            if (!e2.equals(r)) error++;
            echo("\tT3 = " + test.getT3());
            r = ((String[])((Map<String,String[]>)test.getT3()).get("key"))[0];
            echo("\tR3 = " + r);
            if (!e1.equals(r)) error++;
            echo("\tT4 = " + test.getT4());
            r = ((MonitorInfo[])((Map<String,MonitorInfo[]>)test.getT4()).get("key"))[0].getClassName();
            echo("\tR4 = " + r);
            if (!e2.equals(r)) error++;
            echo("\tT5 = " + test.getT5());
            r = ((String[])((Set<String[]>)test.getT5()).iterator().next())[0];
            echo("\tR5 = " + r);
            if (!e1.equals(r)) error++;
            echo("\tT6 = " + test.getT6());
            r = ((MonitorInfo[])((Set<MonitorInfo[]>)test.getT6()).iterator().next())[0].getClassName();
            echo("\tR6 = " + r);
            if (!e2.equals(r)) error++;
            echo("\tT7 = " + test.getT7());
            r = ((String[])((List<String[]>)test.getT7()).get(0))[0];
            echo("\tR7 = " + r);
            if (!e1.equals(r)) error++;
            echo("\tT8 = " + test.getT8());
            r = ((MonitorInfo[])((List<MonitorInfo[]>)test.getT8()).get(0))[0].getClassName();
            echo("\tR8 = " + r);
            if (!e2.equals(r)) error++;
            echo("\tT9 = " + test.getT9());
            r = ((String[])((List<String[]>)((Set<List<String[]>>)test.getT9()).iterator().next()).get(0))[0];
            echo("\tR9 = " + r);
            if (!e1.equals(r)) error++;
            echo("\tT10 = " + test.getT10());
            r = ((MonitorInfo[])((List<MonitorInfo[]>)((Set<List<MonitorInfo[]>>)test.getT10()).iterator().next()).get(0))[0].getClassName();
            echo("\tR10 = " + r);
            if (!e2.equals(r)) error++;
            echo("\tT11 = " + test.getT11());
            r = ((String[])((List<String[]>)((Set<List<String[]>>)((Map<String,Set<List<String[]>>>)test.getT11()).get("key")).iterator().next()).get(0))[0];
            echo("\tR11 = " + r);
            if (!e1.equals(r)) error++;
            echo("\tT12 = " + test.getT12());
            r = ((MonitorInfo[])((List<MonitorInfo[]>)((Set<List<MonitorInfo[]>>)((Map<String,Set<List<MonitorInfo[]>>>)test.getT12()).get("key")).iterator().next()).get(0))[0].getClassName();
            echo("\tR12 = " + r);
            if (!e2.equals(r)) error++;
        } catch (Exception e) {
            e.printStackTrace(System.out);
            error++;
        } finally {
            // Close client
            //
            echo("\n>>> Close the RMI connector client");
            if (cc != null)
                cc.close();

            // Stop server
            //
            echo("\n>>> Stop the RMI connector server");
            if (cs != null)
                cs.stop();

            echo("\n>>> Bye! Bye!");
        }

        if (error > 0) {
            echo("\nTest failed! " + error + " errors.\n");
            throw new IllegalArgumentException("Test failed");
        } else {
            echo("\nTest passed!\n");
        }
    }

    private static void echo(String msg) {
        System.out.println(msg);
    }
}
