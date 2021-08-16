/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.api.consumer.streaming;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import jdk.jfr.consumer.EventStream;
import jdk.test.lib.dcmd.CommandExecutor;
import jdk.test.lib.dcmd.PidJcmdExecutor;

/**
 * @test
 * @summary Verifies that a out-of-process stream is closed when the repository
 *          is changed.
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @modules jdk.jfr jdk.attach java.base/jdk.internal.misc
 * @run main/othervm jdk.jfr.api.consumer.streaming.TestOutOfProcessMigration
 */
public class TestOutOfProcessMigration {
    public static void main(String... args) throws Exception {
        try (TestProcess process = new TestProcess("application"))  {
            AtomicInteger eventCounter = new AtomicInteger();
            Path newRepo = Paths.get("new-repository").toAbsolutePath();
            try (EventStream es = EventStream.openRepository(process.getRepository())) {
                // Start from first event in repository
                es.setStartTime(Instant.EPOCH);
                es.onEvent(e -> {
                    if (eventCounter.incrementAndGet() == TestProcess.NUMBER_OF_EVENTS) {
                        System.out.println("Changing repository to " + newRepo + " ...");
                        CommandExecutor executor = new PidJcmdExecutor(String.valueOf(process.pid()));
                        // This should close stream
                        executor.execute("JFR.configure repositorypath=" + newRepo);
                    }
                });
                es.start();
                process.exit();
                // Wait for process to die, so files are cleaned up
                process.awaitDeath();
            }
        }
    }
}
