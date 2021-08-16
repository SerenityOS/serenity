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

/*
 * @test
 * @bug 4847239
 * @summary Verify directory parameter behavior in File.createTempFile(String,String,File)
 */

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TargetDirectory {
    public static void main(String[] args) throws Exception {
        // Target directory exists and is writable
        Path dir = Path.of("target");
        File target = Files.createDirectory(dir).toFile();
        File tmp = File.createTempFile("passes", null, target);
        if (!Files.exists(tmp.toPath())) {
            throw new RuntimeException("Temp file not created");
        }
        tmp.delete();

        // Make target directory read-only
        if (Files.getFileStore(dir).supportsFileAttributeView("posix")) {
            PosixFileAttributeView view =
                Files.getFileAttributeView(dir, PosixFileAttributeView.class);
            Set<PosixFilePermission> perms = new HashSet<>();
            perms.add(PosixFilePermission.valueOf("OWNER_READ"));
            view.setPermissions(perms);
        } else if (Files.getFileStore(dir).supportsFileAttributeView("acl")) {
            AclFileAttributeView view = Files.getFileAttributeView(dir,
                AclFileAttributeView.class);
            List<AclEntry> entries = new ArrayList<>();
            for (AclEntry entry : view.getAcl()) {
                Set<AclEntryPermission> perms =
                    new HashSet<>(entry.permissions());
                perms.remove(AclEntryPermission.ADD_FILE);
                entries.add(AclEntry.newBuilder().setType(entry.type())
                    .setPrincipal(entry.principal()).setPermissions(perms)
                    .build());
            }
            view.setAcl(entries);
        } else {
            throw new RuntimeException("Required attribute view not supported");
        }

        // Target directory exists but is read-only
        try {
            File.createTempFile("readonly", null, target);
            throw new RuntimeException("Exception not thrown for read-only target directory");
        } catch (IOException expected) {
        } finally {
            target.delete();
        }

        // Target directory does not exist
        try {
            File.createTempFile("nonexistent", null, new File("void"));
            throw new RuntimeException("Exception not thrown for non-existent target directory");
        } catch (IOException expected) {
        }

        // Target is a file, not a directory
        target = Files.createFile(Path.of("file")).toFile();
        try {
            File.createTempFile("file", null, target);
            throw new RuntimeException("Exception not thrown for file target");
        } catch (IOException expected) {
        } finally {
            target.delete();
        }
    }
}
