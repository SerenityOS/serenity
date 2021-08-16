/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;

import jdk.jfr.Event;
import jdk.jfr.Recording;
import jdk.jfr.consumer.EventStream;
import jdk.jfr.jcmd.JcmdHelper;

/**
 * @test
 * @summary Verifies that is possible to stream from an in-process repository
 *          that is being moved.
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.api.consumer.streaming.TestInProcessMigration
 */
public class TestInProcessMigration {
    static class MigrationEvent extends Event {
        int id;
    }

    public static void main(String... args) throws Exception {
        Path newRepository = Paths.get("new-repository");
        CountDownLatch event1 = new CountDownLatch(1);
        CountDownLatch event2 = new CountDownLatch(1);

        try (EventStream es = EventStream.openRepository()) {
            es.setStartTime(Instant.EPOCH);
            es.onEvent(e -> {
                System.out.println(e);
                if (e.getInt("id") == 1) {
                    event1.countDown();
                }
                if (e.getInt("id") == 2) {
                    event2.countDown();
                }
            });
            es.startAsync();
            System.out.println("Started es.startAsync()");

            try (Recording r = new Recording()) {
                r.start();
                // Chunk in default repository
                MigrationEvent e1 = new MigrationEvent();
                e1.id = 1;
                e1.commit();
                event1.await();
                System.out.println("Passed the event1.await()");
                JcmdHelper.jcmd("JFR.configure", "repositorypath=" + newRepository.toAbsolutePath());
                // Chunk in new repository
                MigrationEvent e2 = new MigrationEvent();
                e2.id = 2;
                e2.commit();
                r.stop();
                event2.await();
                System.out.println("Passed the event2.await()");
                // Verify that it happened in new repository
                if (!Files.exists(newRepository)) {
                    throw new AssertionError("Could not find repository " + newRepository);
                }
                System.out.println("Listing contents in new repository:");
                boolean empty = true;
                for (Path p : Files.newDirectoryStream(newRepository)) {
                    System.out.println(p.toAbsolutePath());
                    empty = false;
                }
                System.out.println();
                if (empty) {
                    throw new AssertionError("Could not find contents in new repository location " + newRepository);
                }
            }
        }
    }

}
