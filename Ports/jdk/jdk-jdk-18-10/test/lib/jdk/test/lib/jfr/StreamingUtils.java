/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package jdk.test.lib.jfr;

import com.sun.tools.attach.VirtualMachine;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * Utilities for testing JFR streaming.
 *
 */
public class StreamingUtils {
    /**
     * Get path to JFR streaming repository for a given process, try until success or test timeout.
     *
     * @param process process to analyze
     * @return Path to the JFR streaming repository, or null
     */
    public static Path getJfrRepository(Process process) throws Exception {
        while (true) {
            if (!process.isAlive()) {
                String msg = String.format("Process (pid = %d) is no longer alive, exit value = %d",
                                           process.pid(), process.exitValue());
                throw new RuntimeException(msg);
            }

            try {
                VirtualMachine vm = VirtualMachine.attach(String.valueOf(process.pid()));
                String repo = vm.getSystemProperties().getProperty("jdk.jfr.repository");
                if (repo != null) {
                    vm.detach();
                    System.out.println("JFR repository: " + repo);
                    return Paths.get(repo);
                }
            } catch (Exception e) {
                System.out.println("Attach failed: " + e.getMessage());
                System.out.println("Retrying...");
            }
            Thread.sleep(500);
        }
    }

}
