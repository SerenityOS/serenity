/*
 * Copyright (c) 2015 SAP SE. All rights reserved.
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
 * @bug 8132475
 * @summary Check that jlink creates executables in the bin directory
 *          that are are executable by all users
 * @run main CheckExecutable
 * @author Volker Simonis
 */

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import static java.nio.file.attribute.PosixFilePermission.*;
import java.util.EnumSet;
import java.util.Set;

public class CheckExecutable {

    // The bin directory may contain non-executable files (see 8132704)
    private static final String IGNORE = "glob:{*.diz,jmc.ini}";

    public static void main(String args[]) throws IOException {
        String JAVA_HOME = System.getProperty("java.home");
        Path bin = Paths.get(JAVA_HOME, "bin");

        PathMatcher matcher = FileSystems.getDefault().getPathMatcher(IGNORE);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(bin)) {
            EnumSet<PosixFilePermission> execPerms
                = EnumSet.of(GROUP_EXECUTE, OTHERS_EXECUTE, OWNER_EXECUTE);

            for (Path entry : stream) {
                Path file = entry.getFileName();
                if (!Files.isRegularFile(entry) || matcher.matches(file)) {
                    continue;
                }

                if (!Files.isExecutable(entry))
                    throw new RuntimeException(entry + " is not executable!");

                try {
                    Set<PosixFilePermission> perm
                        = Files.getPosixFilePermissions(entry);
                    if (!perm.containsAll(execPerms)) {
                        throw new RuntimeException(entry
                            + " has not all executable permissions!\n"
                            + "Should have: " + execPerms + "\nbut has: " + perm);
                    }
                } catch (UnsupportedOperationException uoe) { }

            }

        }
    }
}
