/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8139133
 * @summary Verify ability to set time attributes of socket files with no device
 * @requires os.family == "linux"
 */

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

public class UnixSocketFile {
    private static final String TEST_SUB_DIR = "UnixSocketFile";
    private static final String SOCKET_FILE_NAME = "mysocket";
    private static final String CMD_BASE = "nc -lU";

    public static void main(String[] args)
        throws InterruptedException, IOException {

        // Use 'which' to verify that 'nc' is available and skip the test
        // if it is not.
        Process proc = Runtime.getRuntime().exec("which nc");
        InputStream stdout = proc.getInputStream();
        int b = stdout.read();
        proc.destroy();
        if (b == -1) {
            System.err.println("Netcat command unavailable; skipping test.");
            return;
        }

        // Verify that 'nc' accepts '-U' for Unix domain sockets.
        // Skip the test if it is not.
        Process procHelp = Runtime.getRuntime().exec(CMD_BASE + " -h");
        if (procHelp.waitFor() != 0) {
            System.err.println("Netcat does not accept required options; skipping test.");
            return;
        }

        // Create a new sub-directory of the nominal test directory in which
        // 'nc' will create the socket file.
        String testSubDir = System.getProperty("test.dir", ".")
            + File.separator + TEST_SUB_DIR;
        Path socketTestDir = Paths.get(testSubDir);
        Files.createDirectory(socketTestDir);

        // Set the path of the socket file.
        String socketFilePath = testSubDir + File.separator
            + SOCKET_FILE_NAME;

        // Create a process which executes the nc (netcat) utility to create
        // a socket file at the indicated location.
        FileSystem fs = FileSystems.getDefault();
        try (WatchService ws = fs.newWatchService()) {
            // Watch the test sub-directory to receive notification when an
            // entry, i.e., the socket file, is added to the sub-directory.
            WatchKey wk = socketTestDir.register(ws,
                    StandardWatchEventKinds.ENTRY_CREATE);

            // Execute the 'nc' command.
            proc = Runtime.getRuntime().exec(CMD_BASE + " " + socketFilePath);

            // Wait until the socket file is created.
            WatchKey key = ws.take();
            if (key != wk) {
                throw new RuntimeException("Unknown entry created - expected: "
                    + wk.watchable() + ", actual: " + key.watchable());
            }
            wk.cancel();
        }

        // Verify that the socket file in fact exists.
        Path socketPath = fs.getPath(socketFilePath);
        if (!Files.exists(socketPath)) {
            throw new RuntimeException("Socket file " + socketFilePath
                + " was not created by \"nc\" command.");
        }

        // Retrieve the most recent access and modification times of the
        // socket file; print the values.
        BasicFileAttributeView attributeView = Files.getFileAttributeView(
                socketPath, BasicFileAttributeView.class);
        BasicFileAttributes oldAttributes = attributeView.readAttributes();
        FileTime oldAccessTime = oldAttributes.lastAccessTime();
        FileTime oldModifiedTime = oldAttributes.lastModifiedTime();
        System.out.println("Old times: " + oldAccessTime
            + " " + oldModifiedTime);

        // Calculate the time to which the access and modification times of the
        // socket file will be changed.
        FileTime newFileTime =
            FileTime.fromMillis(oldAccessTime.toMillis() + 1066);

        try {
            // Set the access and modification times of the socket file.
            attributeView.setTimes(newFileTime, newFileTime, null);

            // Retrieve the updated access and modification times of the
            // socket file; print the values.
            FileTime newAccessTime = null;
            FileTime newModifiedTime = null;
            BasicFileAttributes newAttributes = attributeView.readAttributes();
            newAccessTime = newAttributes.lastAccessTime();
            newModifiedTime = newAttributes.lastModifiedTime();
            System.out.println("New times: " + newAccessTime + " "
                + newModifiedTime);

            // Verify that the updated times have the expected values.
            if ((newAccessTime != null && !newAccessTime.equals(newFileTime))
                || (newModifiedTime != null
                    && !newModifiedTime.equals(newFileTime))) {
                throw new RuntimeException("Failed to set correct times.");
            }
        } finally {
            // Destry the process running netcat and delete the socket file.
            proc.destroy();
            Files.delete(socketPath);
        }
    }
}
