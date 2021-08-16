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
 * @bug 6683213
 * @summary Test that the initial derived gauge is (Integer)0
 * @author Daniel Fuchs
 *
 * @run clean DerivedGaugeMonitorTest
 * @run build DerivedGaugeMonitorTest
 * @run main DerivedGaugeMonitorTest
 */

import java.io.Serializable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.monitor.CounterMonitor;
import javax.management.monitor.GaugeMonitor;

public class DerivedGaugeMonitorTest {

    public static interface Things {
        public long getALong();
        public int getAnInt();
        public double getADouble();
        public short getAShort();
        public byte getAByte();
        public float getAFloat();
    }
    public static interface MyMBean extends Things {
        public Things getAThing();
    }

    public static class MyThings implements Things, Serializable {
        private static final long serialVersionUID = -4333982919572564126L;

        private volatile long along = 0;
        private volatile int anint = 0;
        private volatile short ashort = 0;
        private volatile byte abyte = 0;
        private volatile float afloat = 0;
        private volatile double adouble = 0;

        private volatile transient boolean mutable;

        public MyThings() {
            this(false);
        }

        protected MyThings(boolean mutable) {
            this.mutable=mutable;
        }

        public long getALong() {
            return mutable?along++:along;
        }

        public int getAnInt() {
            return mutable?anint++:anint;
        }

        public double getADouble() {
            return mutable?adouble++:adouble;
        }

        public short getAShort() {
            return mutable?ashort++:ashort;
        }

        public byte getAByte() {
            return mutable?abyte++:abyte;
        }

        public float getAFloat() {
            return mutable?afloat++:afloat;
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            final MyThings other = (MyThings)super.clone();
            other.mutable=false;
            return other;
        }
    }

    public static class My implements MyMBean {

        public final CountDownLatch cdl = new CountDownLatch(6);

        private final MyThings things = new MyThings(true);
        private volatile int count = 0;

        public Things getAThing() {
            count++;
            cdl.countDown();
            try {
                return (Things) things.clone();
            } catch (CloneNotSupportedException ex) {
                return null;
            }
        }

        public long getALong() {
            count++;
            cdl.countDown();
            return things.getALong();
        }

        public int getAnInt() {
            count++;
            cdl.countDown();
            return things.getAnInt();
        }

        public double getADouble() {
            count++;
            cdl.countDown();
            return things.getADouble();
        }

        public short getAShort() {
            count++;
            cdl.countDown();
            return things.getAShort();
        }

        public byte getAByte() {
            count++;
            cdl.countDown();
            return things.getAByte();
        }

        public float getAFloat() {
            count++;
            cdl.countDown();
            return things.getAFloat();
        }

    }


    public static String[] attributes = {
        "AByte","AShort","AnInt","ALong","AFloat","ADouble"
    };
    public static String[] things = {
        "AThing.AByte","AThing.AShort","AThing.AnInt","AThing.ALong",
        "AThing.AFloat","AThing.ADouble"
    };

    public static void check(String attr, MBeanServer server, ObjectName mon,
            ObjectName mbean) throws Exception {
        final Object obj = server.getAttribute(mon, "DerivedGauge");
        check(attr,server,mon,mbean,obj);
    }

    public static void check(String attr, MBeanServer server, ObjectName mon,
            ObjectName mbean, Object obj) throws Exception  {
        if (obj == null)
            throw new Exception("Derived gauge for: " + attr +
                    " ["+mon+", "+mbean+"] is null!");
        if (!Integer.valueOf(0).equals(obj))
            throw new Exception("Derived gauge for: " + attr +
                    " ["+mon+", "+mbean+"] is "+obj);
    }

    public static void check(String attr, MBeanServer server, ObjectName mon,
            ObjectName mbean, long start) throws Exception {
        final Object obj = server.getAttribute(mon, "DerivedGauge");
        final long now = System.currentTimeMillis();
        final long gran = (Long)server.getAttribute(mon, "GranularityPeriod");
        if (now > start +2*gran) {
            throw new Exception(attr+": Can't verify test case: " +
                    "granularity period expired!");
        }
        check(attr,server,mon,mbean,obj);
    }

    public static void test(String attr) throws Exception {
        System.err.println("Testing "+ attr);
        final MBeanServer server = MBeanServerFactory.createMBeanServer();
        final ObjectName mbean = new ObjectName("ugly:type=cr.p");
        final My my = new My();
        final GaugeMonitor mon2 = new GaugeMonitor();
        final ObjectName mon2n = new ObjectName("mon1:type=GaugeMonitor");
        final CounterMonitor mon1 = new CounterMonitor();
        final ObjectName mon1n = new ObjectName("mon2:type=CounterMonitor");

        server.registerMBean(my, mbean);
        server.registerMBean(mon1, mon1n);
        server.registerMBean(mon2, mon2n);

        mon1.addObservedObject(mbean);
        mon1.setGranularityPeriod(60000); // 60 sec...
        mon1.setObservedAttribute(attr);
        mon1.setDifferenceMode(true);
        check(attr,server,mon1n,mbean);

        mon2.addObservedObject(mbean);
        mon2.setGranularityPeriod(60000); // 60 sec...
        mon2.setObservedAttribute(attr);
        mon2.setDifferenceMode(true);
        check(attr,server,mon2n,mbean);

        final long approxStart = System.currentTimeMillis();
        mon1.start();
        mon2.start();

        try {
            check(attr,server,mon1n,mbean,approxStart);
            check(attr,server,mon2n,mbean,approxStart);
            check(attr,server,mon1n,mbean,approxStart);
            check(attr,server,mon2n,mbean,approxStart);


            mon1.setGranularityPeriod(5);
            mon2.setGranularityPeriod(5);

            my.cdl.await(1000, TimeUnit.MILLISECONDS);
            if (my.cdl.getCount() > 0)
                throw new Exception(attr+": Count down not reached!");

            // just check that we don't get an exception now...
            System.err.println(attr+": [" + mon1n+
                    "] DerivedGauge is now "+
                    server.getAttribute(mon1n, "DerivedGauge"));
            System.err.println(attr+": [" + mon2n+
                    "] DerivedGauge is now "+
                    server.getAttribute(mon2n, "DerivedGauge"));
        } finally {
           try {mon1.stop();} catch (Exception x) {}
           try {mon2.stop();} catch (Exception x) {}
        }
    }




    public static void main(String[] args) throws Exception {
        for (String attr:attributes) {
            test(attr);
        }
        for (String attr:things) {
            test(attr);
        }
    }

}
