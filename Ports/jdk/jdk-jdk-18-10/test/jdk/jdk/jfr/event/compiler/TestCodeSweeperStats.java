/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.event.compiler;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;

import sun.hotspot.WhiteBox;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.classloader.FilterClassLoader;
import jdk.test.lib.classloader.ParentLastURLClassLoader;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;
import jdk.test.lib.Utils;

/**
 * @test TestCodeSweeperStats
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @requires vm.compMode!="Xint"
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:.
 *     -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *     -XX:CompileOnly=jdk.jfr.event.compiler.TestCodeSweeperStats::dummyMethod
 *     -XX:+SegmentedCodeCache jdk.jfr.event.compiler.TestCodeSweeperStats
 * @run main/othervm -Xbootclasspath/a:.
 *     -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *     -XX:CompileOnly=jdk.jfr.event.compiler.TestCodeSweeperStats::dummyMethod
 *     -XX:-SegmentedCodeCache jdk.jfr.event.compiler.TestCodeSweeperStats
 */
public class TestCodeSweeperStats {
    private static final String EVENT_NAME = EventNames.CodeSweeperStatistics;
    private static final int WAIT_TIME = 10_000;
    private static final String CLASS_METHOD_TO_COMPILE = "dummyMethod";
    private static final int METHODS_TO_COMPILE = Integer.getInteger("compile.methods.count", 10);
    private static final int COMP_LEVEL_SIMPLE = 1;
    private static final int COMP_LEVEL_FULL_OPTIMIZATION = 4;

    public static void main(String[] args) throws Exception {
        Recording recording = new Recording();
        recording.enable(EVENT_NAME).with("period", "endChunk");
        recording.start();
        compileAndSweep();
        recording.stop();

        List<RecordedEvent> events = Events.fromRecording(recording);
        Events.hasEvents(events);
        for (RecordedEvent event : events) {
            Events.assertField(event, "sweepCount").atLeast(1);
            Events.assertField(event, "methodReclaimedCount").equal(METHODS_TO_COMPILE);
            Events.assertField(event, "totalSweepTime").atLeast(0L);
            Events.assertField(event, "peakFractionTime").atLeast(0L);
            Events.assertField(event, "peakSweepTime").atLeast(0L);
        }
    }

    private static void compileAndSweep() throws InterruptedException {
        WhiteBox WB = WhiteBox.getWhiteBox();
        for (int i = 0; i < METHODS_TO_COMPILE; i++) {
            System.out.println("compile " + i);
            compileMethod();
        }

        WB.deoptimizeAll();
        System.out.println("All methods deoptimized");

        // method will be sweeped out of code cache after 5 sweep cycles
        for (int i = 0; i < 5; i++) {
            WB.fullGC();
            WB.forceNMethodSweep();

        }
        // now wait for event(s) to be fired
        Thread.sleep(WAIT_TIME);
    }

    public void dummyMethod() {
        System.out.println("Hello World!");
    }

    protected static void compileMethod() {
        ClassLoader current = TestCodeSweeperStats.class.getClassLoader();
        String[] cpaths = System.getProperty("test.classes", ".").split(File.pathSeparator);
        URL[] urls = new URL[cpaths.length];
        try {
            for (int i = 0; i < cpaths.length; i++) {
                urls[i] = Paths.get(cpaths[i]).toUri().toURL();
            }
        } catch (MalformedURLException e) {
            throw new Error(e);
        }

        String currentClassName = TestCodeSweeperStats.class.getName();
        FilterClassLoader cl = new FilterClassLoader(new ParentLastURLClassLoader(urls, current), ClassLoader.getSystemClassLoader(), (name) -> currentClassName.equals(name));
        Class<?> loadedClass = null;
        String className = currentClassName;
        try {
            loadedClass = cl.loadClass(className);
        } catch (ClassNotFoundException ex) {
            throw new Error("Couldn't load class " + className, ex);
        }
        try {
            Method mtd = loadedClass.getMethod(CLASS_METHOD_TO_COMPILE);
            WhiteBox WB = WhiteBox.getWhiteBox();
            WB.testSetDontInlineMethod(mtd, true);
            String directive = "[{ match: \"" + TestCodeSweeperStats.class.getName().replace('.', '/')
                    + "." + CLASS_METHOD_TO_COMPILE + "\", " + "BackgroundCompilation: false }]";
            WB.addCompilerDirective(directive);
            if (!WB.enqueueMethodForCompilation(mtd, COMP_LEVEL_FULL_OPTIMIZATION)) {
                WB.enqueueMethodForCompilation(mtd, COMP_LEVEL_SIMPLE);
            }
            Utils.waitForCondition(() -> WB.isMethodCompiled(mtd));
        } catch (NoSuchMethodException e) {
            throw new Error("An exception while trying compile method " + e.getMessage(), e);
        }
    }
}
