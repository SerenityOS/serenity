/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6876541
 * @summary Test Files.walkFileTree in the presence of a security manager
 * @build WalkWithSecurity
 * @run main/othervm -Djava.security.manager=allow WalkWithSecurity grantAll.policy pass
 * @run main/othervm -Djava.security.manager=allow WalkWithSecurity denyAll.policy fail
 * @run main/othervm -Djava.security.manager=allow WalkWithSecurity grantTopOnly.policy top_only
 */

import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.io.IOException;

public class WalkWithSecurity {

    public static void main(String[] args) throws IOException {
        String policyFile = args[0];
        ExpectedResult expectedResult = ExpectedResult.valueOf(args[1].toUpperCase());

        String here = System.getProperty("user.dir");
        String testSrc = System.getProperty("test.src");
        if (testSrc == null)
            throw new RuntimeException("This test must be run by jtreg");
        Path dir = Paths.get(testSrc);

        // Sanity check the environment
        if (Files.isSameFile(Paths.get(here), dir))
            throw new RuntimeException("Working directory cannot be " + dir);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            if (!stream.iterator().hasNext())
                throw new RuntimeException(testSrc + " is empty");
        }

        // Install security manager with the given policy file
        System.setProperty("java.security.policy",
            dir.resolve(policyFile).toString());
        System.setSecurityManager(new SecurityManager());

        // Walk the source tree
        CountingVisitor visitor = new CountingVisitor();
        SecurityException exception = null;
        try {
            Files.walkFileTree(dir, visitor);
        } catch (SecurityException se) {
            exception = se;
        }

        // Check result
        switch (expectedResult) {
            case PASS:
                if (exception != null) {
                    exception.printStackTrace();
                    throw new RuntimeException("SecurityException not expected");
                }
                if (visitor.count() == 0)
                    throw new RuntimeException("No files visited");
                break;
            case FAIL:
                if (exception == null)
                    throw new RuntimeException("SecurityException expected");
                if (visitor.count() > 0)
                    throw new RuntimeException("Files were visited");
                break;
            case TOP_ONLY:
                if (exception != null) {
                    exception.printStackTrace();
                    throw new RuntimeException("SecurityException not expected");
                }
                if (visitor.count() == 0)
                    throw new RuntimeException("Starting file not visited");
                if (visitor.count() > 1)
                    throw new RuntimeException("More than starting file visited");
                break;
            default:
                throw new RuntimeException("Should not get here");
        }
    }

    static enum ExpectedResult {
        PASS,
        FAIL,
        TOP_ONLY;
    }

    static class CountingVisitor extends SimpleFileVisitor<Path> {
        private int count;

        int count() {
            return count;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            System.out.println(dir);
            count++;
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            System.out.println(file);
            count++;
            return FileVisitResult.CONTINUE;
        }
    }
}
