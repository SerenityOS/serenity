/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4313887
 * @summary Sanity test for JDK-specific sensitivity level watch event modifier
 * @modules jdk.unsupported
 * @library .. /test/lib
 * @build jdk.test.lib.Platform
 * @build jdk.test.lib.RandomFactory
 * @run main/timeout=240 SensitivityModifier
 * @key randomness
 */

import java.nio.file.Files;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import static java.nio.file.StandardWatchEventKinds.*;
import java.io.OutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import com.sun.nio.file.SensitivityWatchEventModifier;
import jdk.test.lib.Platform;
import jdk.test.lib.RandomFactory;

public class SensitivityModifier {
    // on macOS and other platforms, watch services might be based on polling
    // requiring a longer timeout to detect events before returning
    static final long POLL_TIMEOUT_SECONDS =
        Platform.isLinux() || Platform.isWindows() ? 1 : 2;

    static final Random RAND = RandomFactory.getRandom();

    static final Map<Path,Integer> pathToTime = new HashMap<>();

    static void register(Path[] dirs, WatchService watcher) throws IOException {
        pathToTime.clear();
        SensitivityWatchEventModifier[] sensitivities =
            SensitivityWatchEventModifier.values();
        for (int i=0; i<dirs.length; i++) {
            SensitivityWatchEventModifier sensitivity =
                sensitivities[RAND.nextInt(sensitivities.length)];
            Path dir = dirs[i];
            dir.register(watcher, new WatchEvent.Kind<?>[]{ ENTRY_MODIFY },
                sensitivity);
            pathToTime.put(dir, sensitivity.sensitivityValueInSeconds());
        }
    }

    @SuppressWarnings("unchecked")
    static void doTest(Path top) throws Exception {
        FileSystem fs = top.getFileSystem();
        try (WatchService watcher = fs.newWatchService()) {

            // create directories and files
            int nDirs = 5 + RAND.nextInt(20);
            int nFiles = 50 + RAND.nextInt(50);
            Path[] dirs = new Path[nDirs];
            Path[] files = new Path[nFiles];
            for (int i=0; i<nDirs; i++) {
                dirs[i] = Files.createDirectory(top.resolve("dir" + i));
            }
            for (int i=0; i<nFiles; i++) {
                Path dir = dirs[RAND.nextInt(nDirs)];
                files[i] = Files.createFile(dir.resolve("file" + i));
            }

            // register the directories (random sensitivity)
            register(dirs, watcher);

            // sleep a bit here to ensure that modification to the first file
            // can be detected by polling implementations (ie: last modified time
            // may not change otherwise).
            try { Thread.sleep(1000); } catch (InterruptedException e) { }

            // modify files and check that events are received
            for (int i=0; i<10; i++) {
                Path file = files[RAND.nextInt(nFiles)];
                System.out.println("Modify: " + file);
                try (OutputStream out = Files.newOutputStream(file)) {
                    out.write(new byte[100]);
                }

                System.out.println("Waiting for event(s)...");
                boolean eventReceived = false;
                WatchKey key = watcher.take();
                do {
                    for (WatchEvent<?> event: key.pollEvents()) {
                        if (event.kind() != ENTRY_MODIFY)
                            throw new RuntimeException("Unexpected event: " + event);
                        Path name = ((WatchEvent<Path>)event).context();
                        if (name.equals(file.getFileName())) {
                            eventReceived = true;
                            break;
                        }
                    }
                    key.reset();
                    key = watcher.poll(POLL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                } while (key != null);

                // we should have received at least one ENTRY_MODIFY event
                if (eventReceived) {
                    System.out.println("Event OK");
                } else {
                    Path parent = file.getParent();
                    String msg = String.format("No ENTRY_MODIFY event received for %s (dir: %s, sensitivity: %d)",
                            file, parent, pathToTime.get(parent));
                    throw new RuntimeException(msg);
                }

                // re-register the directories to force changing their sensitivity
                // level
                register(dirs, watcher);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Path dir = TestUtil.createTemporaryDirectory();
        try {
            doTest(dir);
        } finally {
            TestUtil.removeAll(dir);
        }
    }
}
