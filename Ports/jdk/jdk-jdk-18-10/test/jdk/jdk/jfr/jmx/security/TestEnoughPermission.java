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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import jdk.jfr.jmx.JmxHelper;

import jdk.management.jfr.ConfigurationInfo;
import jdk.management.jfr.FlightRecorderMXBean;
import jdk.test.lib.Asserts;

/**
 * @test
 * @key jfr
 * @summary Test with minimal needed permissions. All functions should work.
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm/secure=java.lang.SecurityManager/java.security.policy=enough.policy jdk.jfr.jmx.security.TestEnoughPermission
 */
public class TestEnoughPermission {

    public static void main(String[] args) throws Throwable {
        try {
            FlightRecorderMXBean bean = JmxHelper.getFlighteRecorderMXBean();

            System.out.println("AAAAAAAAAAAAAAAAAA");
            Asserts.assertFalse(bean.getEventTypes().isEmpty(), "No EventTypes");
            System.out.println("BBBBBBBBBBBBBBB");
            List<ConfigurationInfo> configs = bean.getConfigurations();
            System.out.println("CCCCCCCCCCCCCCCCC");
            for (ConfigurationInfo config : configs) {
                System.out.println("config.name=" + config.getName() + ": " + config.getContents());
            }

            long recId = testRecording(bean);
            testStream(bean, recId);
            bean.closeRecording(recId);

            //*************** verifySecurityException(() -> bean.getRecordingOptions(dummyId), "getRecordingOptions()");
            //*************** verifySecurityException(() -> bean.getRecordingSettings(dummyId), "getRecordingSettings()");
            //*********** verifySecurityException(() -> bean.setConfiguration(dummyId, "<>"), "setConfiguration()");
            //************* verifySecurityException(() -> bean.setRecordingSettings(dummyId, dummyMap), "setRecordingSettings()");
            //************* verifySecurityException(() -> bean.setRecordingOptions(dummyId, dummyMap), "setRecordingOptions()");
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }
    }

    private static long testRecording(FlightRecorderMXBean bean) throws Exception {
        System.out.println("A");
        long recId = bean.newRecording();
        System.out.println("B");
        bean.setPredefinedConfiguration(recId, "profile");
        System.out.println("C");
        bean.startRecording(recId);
        System.out.println("D");
        Asserts.assertTrue(bean.getRecordings().stream().anyMatch(r -> { return r.getId() == recId; }), "recId not found");
        System.out.println("E");
        bean.stopRecording(recId);

        final Path path = Paths.get(".", "rec" + recId + ".jfr");
        bean.copyTo(recId, path.toString());
        //EventSet events = EventSet.fromFile(path);
        return recId;
    }

    private static void testStream(FlightRecorderMXBean bean, long recId) throws Exception {
        long streamId = bean.openStream(recId, null);
        byte[] buff = bean.readStream(streamId);
        Asserts.assertNotNull(buff, "Stream data was empty");
        while (buff != null) {
            // TODO: write to file and parse.
            System.out.println("buff.length=" + buff.length);
            buff = bean.readStream(streamId);
        }
        bean.closeStream(streamId);
    }
}
