/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jfr.api.consumer.log;

import java.io.Closeable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jdk.jfr.Event;
import jdk.jfr.Name;

/**
 * @test
 * @summary Tests output from various tag sets and levels
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @build jdk.jfr.api.consumer.log.LogAnalyzer
 * @run main/othervm
 *      -Xlog:jfr+event*=trace:file=trace.log
 *      -XX:StartFlightRecording
 *      jdk.jfr.api.consumer.log.TestVerbosity trace
 * @run main/othervm
 *      -Xlog:jfr+event*=debug:file=debug.log
 *      -XX:StartFlightRecording:jdk.ExecutionSample#enabled=false
 *      jdk.jfr.api.consumer.log.TestVerbosity debug
 * @run main/othervm
 *      -Xlog:jfr+event*=info:file=info.log
 *      -XX:StartFlightRecording
 *      jdk.jfr.api.consumer.log.TestVerbosity info
 */
public class TestVerbosity {

    @Name("UserDefined")
    static class UserEvent extends Event {
    }

    public static void main(String... args) throws Exception {
        String level = args[0];
        var scheduler = Executors.newScheduledThreadPool(1);
        try (Closeable close = scheduler::shutdown) {

            scheduler.scheduleAtFixedRate(() -> {
                method1();
            }, 0, 10, TimeUnit.MILLISECONDS);

            LogAnalyzer la = new LogAnalyzer(level + ".log");
            System.out.println("Testing log level: " + level);
            if (level.equals("trace")) {
                la.await("CPULoad"); // Emitted 1/s
                la.await("UserDefined");
                la.await("method6");
                la.await("method1");
            }
            if (level.equals("debug")) {
                la.await("CPULoad");
                la.await("UserDefined");
                la.await("method6");
                la.shouldNotContain("method1");
            }
            if (level.equals("info")) {
                la.shouldNotContain("CPULoad");
                la.shouldNotContain("UserDefined");
            }
        }
    }

    private static void method1() {
        method2();
    }

    private static void method2() {
        method3();
    }

    private static void method3() {
        method4();
    }

    private static void method4() {
        method5();
    }

    private static void method5() {
        method6();
    }

    private static void method6() {
        UserEvent event = new UserEvent();
        event.commit();
    }
}
