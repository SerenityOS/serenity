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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

// Helper class for analyzing log output from a live process
public class LogAnalyzer {
    private final Path path;

    public LogAnalyzer(String filename) throws IOException {
        this.path = Path.of(filename);
    }

    public void shouldNotContain(String text) throws Exception {
        System.out.println("Should not contain: '" + text + "'");
        while (true) {
            try {
                for (String line : Files.readAllLines(path)) {
                    if (line.contains(text)) {
                        throw new Exception("Found unexpected log message: " + line);
                    }
                }
                return;
            } catch (IOException e) {
                System.out.println("Could not read log file " + path.toAbsolutePath());
                e.printStackTrace();
            }
            Thread.sleep(100);
        }
    }

    public void await(String text) throws InterruptedException {
        System.out.println("Awaiting... '" + text + "' ");
        while (true) {
            try {
                for (String line : Files.readAllLines(path)) {
                    if (line.contains(text)) {
                        System.out.println("Found!");
                        return;
                    }
                }
            } catch (IOException e) {
                System.out.println("Could not read log file " + path.toAbsolutePath());
                e.printStackTrace();
            }
            Thread.sleep(100);
        }
    }
}
