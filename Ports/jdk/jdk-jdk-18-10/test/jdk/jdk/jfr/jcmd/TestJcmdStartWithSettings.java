/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.jcmd;

import java.io.File;

import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThread;
import jdk.jfr.consumer.RecordingFile;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;
import jdk.test.lib.process.OutputAnalyzer;

/**
 * @test
 * @summary The test verifies that recording can be started with setting file(s)
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.jcmd.TestJcmdStartWithSettings
 */
public class TestJcmdStartWithSettings {

    private static final String DIR = System.getProperty("test.src", ".");
    private static final File SETTINGS = new File(DIR, "jcmd-testsettings.jfc");
    private static final File SETTINGS2 = new File(DIR, "jcmd-testsettings.2.jfc");

    public static void main(String[] args) throws Exception {
        testSingleSettingFile();
        testManySettingFiles();
        testPresetSettings();
        testNonExistingSettingFile();
    }

    private static void testSingleSettingFile() throws Exception {
        String name = "testSingleSettingFile";
        File recording = new File(name + ".jfr");

        OutputAnalyzer output = JcmdHelper.jcmd("JFR.start",
                "name=" + name,
                "duration=1h",
                "settings=" + SETTINGS.getCanonicalPath(),
                "filename=" + recording.getCanonicalPath());
        JcmdAsserts.assertRecordingHasStarted(output);
        JcmdHelper.waitUntilRunning(name);
        output = JcmdHelper.jcmdCheck(name, true);
        JcmdAsserts.assertThreadSleepThresholdIsSet(output);

        Thread.sleep(100);
        JcmdHelper.stopAndCheck(name);
        assertHasEvent(recording, EventNames.ThreadSleep, Thread.currentThread().getName());
    }

    /**
     * Start a recording with two setting files and
     * verify Java Thread Sleep and Java Monitor Wait events have been recorded.
     */
    private static void testManySettingFiles() throws Exception {
        String name = "testManySettingFiles";
        File recording = new File(name + ".jfr");

        OutputAnalyzer output = JcmdHelper.jcmd("JFR.start",
                "name=" + name,
                "duration=1h",
                "settings=" + SETTINGS.getCanonicalPath(),
                "settings=" + SETTINGS2.getCanonicalPath(),
                "filename=" + recording.getCanonicalPath());
        JcmdAsserts.assertRecordingHasStarted(output);
        JcmdHelper.waitUntilRunning(name);
        output = JcmdHelper.jcmdCheck(name, true);
        JcmdAsserts.assertThreadSleepThresholdIsSet(output);
        JcmdAsserts.assertMonitorWaitThresholdIsSet(output);

        // Generate Monitor Wait event
        ThreadWait threadWait = new ThreadWait();
        threadWait.start();
        Thread.sleep(300);
        threadWait.join();

        JcmdHelper.stopAndCheck(name);
        assertHasEvent(recording, EventNames.ThreadSleep, Thread.currentThread().getName());
        assertHasEvent(recording, EventNames.JavaMonitorWait, threadWait.getName());
    }

    /**
     * It should be possible to use "profile" as non-path preset,
     * both with and without '.jfc'
     */
    private static void testPresetSettings() throws Exception {
        String name = "testPresetSettingsJfc";
        OutputAnalyzer output = JcmdHelper.jcmd("JFR.start",
                "name=" + name,
                "settings=profile.jfc");
        JcmdAsserts.assertRecordingHasStarted(output);
        JcmdHelper.waitUntilRunning(name);
        JcmdHelper.stopAndCheck(name);

        name = "testPresetSettingsNoJfc";
        output = JcmdHelper.jcmd("JFR.start",
                "name=" + name,
                "settings=profile");
        JcmdAsserts.assertRecordingHasStarted(output);
        JcmdHelper.waitUntilRunning(name);
        JcmdHelper.stopAndCheck(name);
    }

    /**
     * It should not be possible to start a recording
     * with a non-existing setting file
     */
    private static void testNonExistingSettingFile() throws Exception {
        String name = "testNonExistingSettingFile";
        OutputAnalyzer output = JcmdHelper.jcmd("JFR.start",
                "name=" + name,
                "settings=nonexisting.jfc");
        JcmdAsserts.assertNotAbleToFindSettingsFile(output);
        JcmdHelper.assertRecordingNotExist(name);
    }

    private static void assertHasEvent(File file, String eventType, String threadName) throws Exception {
        for (RecordedEvent event : RecordingFile.readAllEvents(file.toPath())) {
            if (Events.isEventType(event, eventType)) {
                System.out.println(event);
                RecordedThread t = event.getThread();
                if (t == null) {
                    throw new Exception("Thread null for event " + eventType);
                }
                if (threadName.equals(t.getJavaName())) {
                    System.out.println("Found event: " + event);
                    return;
                }
            }
        }
        Asserts.fail("No events of type " + eventType);
    }

    static class ThreadWait extends Thread {

        public ThreadWait() {
            setName("ThreadWait");
        }

        @Override
        public void run() {
            try {
                synchronized (this) {
                    wait(100);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
