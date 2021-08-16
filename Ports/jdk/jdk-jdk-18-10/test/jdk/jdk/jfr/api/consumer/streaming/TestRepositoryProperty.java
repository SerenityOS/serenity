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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

import jdk.jfr.Recording;
import jdk.test.lib.dcmd.CommandExecutor;
import jdk.test.lib.dcmd.PidJcmdExecutor;

/**
 * @test
 * @summary Verifies that it is possible to access JFR repository from a system
 *          property
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @modules jdk.attach
 *          jdk.jfr
 * @run main/othervm -Djdk.attach.allowAttachSelf=true jdk.jfr.api.consumer.streaming.TestRepositoryProperty
 */
public class TestRepositoryProperty {

    private final static String JFR_REPOSITORY_LOCATION_PROPERTY = "jdk.jfr.repository";

    public static void main(String... args) throws Exception {
        testBeforeInitialization();
        testAfterInitialization();
        testFromAgent();
        testAfterChange();
    }

    private static void testFromAgent() throws AttachNotSupportedException, IOException {
        String pidText = String.valueOf(ProcessHandle.current().pid());
        VirtualMachine vm = VirtualMachine.attach(pidText);
        Properties p = vm.getSystemProperties();
        String location = (String) p.get(JFR_REPOSITORY_LOCATION_PROPERTY);
        if (location == null) {
            throw new AssertionError("Could not find repository path in agent properties");
        }
        Path path = Path.of(location);
        if (!Files.isDirectory(path)) {
            throw new AssertionError("Repository path doesn't point to directory");
        }
    }

    private static void testAfterChange() {
        Path newRepository = Path.of(".").toAbsolutePath();

        String cmd = "JFR.configure repository=" +  newRepository.toString();
        CommandExecutor executor = new PidJcmdExecutor();
        executor.execute(cmd);
        String location = System.getProperty(JFR_REPOSITORY_LOCATION_PROPERTY);
        if (newRepository.toString().equals(location)) {
            throw new AssertionError("Repository path not updated after it has been changed");
        }
    }

    private static void testAfterInitialization() {
        try (Recording r = new Recording()) {
            r.start();
            String location = System.getProperty(JFR_REPOSITORY_LOCATION_PROPERTY);
            if (location == null) {
                throw new AssertionError("Repository path should exist after JFR is initialized");
            }
            System.out.println("repository=" + location);
            Path p = Path.of(location);
            if (!Files.isDirectory(p)) {
                throw new AssertionError("Repository path doesn't point to directory");
            }
        }

    }

    private static void testBeforeInitialization() {
        String location = System.getProperty(JFR_REPOSITORY_LOCATION_PROPERTY);
        if (location != null) {
            throw new AssertionError("Repository path should not exist before JFR is initialized");
        }
    }
}
