/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.jmx.security;

import jdk.jfr.jmx.JmxHelper;

import jdk.management.jfr.FlightRecorderMXBean;
import jdk.test.lib.jfr.CommonHelper;
import jdk.test.lib.jfr.VoidFunction;

/**
 * @test
 * @key jfr
 * @summary Verify we get SecurityExceptions when missing management permission "monitor".
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm/secure=java.lang.SecurityManager/java.security.policy=nomonitor.policy jdk.jfr.jmx.security.TestNoMonitorPermission
 */
public class TestNoMonitorPermission {

    public static void main(String[] args) throws Throwable {
        try {
            FlightRecorderMXBean bean = JmxHelper.getFlighteRecorderMXBean();

            int dummyId = 1;
            verifySecurityException(() -> bean.getRecordings(), "getRecordings()");
            verifySecurityException(() -> bean.getConfigurations(), "getConfigurations()");
            verifySecurityException(() -> bean.getEventTypes(), "getEventTypes()");
            verifySecurityException(() -> bean.getRecordingOptions(dummyId), "getRecordingOptions()");
            verifySecurityException(() -> bean.getRecordingSettings(dummyId), "getRecordingSettings()");
            verifySecurityException(() -> bean.readStream(dummyId), "readStream()");
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }
    }


    private static void verifySecurityException(VoidFunction f, String msg) throws Throwable {
        CommonHelper.verifyException(f, msg, java.lang.SecurityException.class);
    }
}
