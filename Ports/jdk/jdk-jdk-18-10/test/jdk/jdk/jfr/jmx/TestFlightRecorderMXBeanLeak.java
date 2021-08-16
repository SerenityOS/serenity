/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jfr.jmx;

import java.lang.management.ManagementFactory;
import java.net.URL;
import java.net.URLClassLoader;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedClassLoader;
import jdk.jfr.consumer.RecordedEvent;
import jdk.management.jfr.FlightRecorderMXBean;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @key jfr
 * @summary Verifies that attributes in FlightRecorderMXBean can be inspected
 *          without causing a memory leak.
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.jmx.TestFlightRecorderMXBeanLeak
 */
public class TestFlightRecorderMXBeanLeak {

    private static final String CLASS_LOADER_NAME = "Test Leak";
    private static final String TEST_CLASS = "jdk.jfr.jmx.TestFlightRecorderMXBeanLeak$FlightRecorderMXBeanLoader";

    public static void main(String[] args) throws Exception {
        URL url = FlightRecorderMXBeanLoader.class.getProtectionDomain().getCodeSource().getLocation();
        URLClassLoader loader = new URLClassLoader(CLASS_LOADER_NAME, new URL[] { url }, null);
        Class<?> clazz = Class.forName(TEST_CLASS, true, loader);
        clazz.newInstance();
        loader.close();
        clazz = null;
        loader = null;
        System.gc();
        System.gc();

        try (Recording r = new Recording()) {
            r.enable("jdk.ClassLoaderStatistics").with("period", "endChunk");
            r.start();
            r.stop();
            for (RecordedEvent e : Events.fromRecording(r)) {
                RecordedClassLoader o = e.getValue("classLoader");
                if (o != null) {
                    System.out.println("Class loader: type=" + o.getType().getName() + " name=" + o.getName());
                    if (CLASS_LOADER_NAME.equals(o.getName())) {
                        throw new Exception("Memory Leak. Class loader '" + CLASS_LOADER_NAME + "' should not be on the heap!");
                    }
                }
            }
        }
    }

    public static class FlightRecorderMXBeanLoader {
        public FlightRecorderMXBeanLoader() throws Exception {
            ObjectName objectName = new ObjectName(FlightRecorderMXBean.MXBEAN_NAME);
            MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
            MBeanInfo mBeanInfo = platformMBeanServer.getMBeanInfo(objectName);
            System.out.println("Inspecting FlightRecorderMXBeann:");
            for (MBeanAttributeInfo attributeInfo : mBeanInfo.getAttributes()) {
                Object value = platformMBeanServer.getAttribute(objectName, attributeInfo.getName());
                System.out.println(attributeInfo.getName() + " = " + value);
            }
        }
    }
}
