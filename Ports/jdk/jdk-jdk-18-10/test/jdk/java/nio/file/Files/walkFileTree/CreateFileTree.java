/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.file.*;
import java.io.IOException;
import java.util.*;

/**
 * Creates a file tree with possible cycles caused by symbolic links
 * to ancestor directories.
 */

public class CreateFileTree {

    private static final Random rand = new Random();

    private static boolean supportsLinks(Path dir) {
        Path link = dir.resolve("testlink");
        Path target = dir.resolve("testtarget");
        try {
            Files.createSymbolicLink(link, target);
            Files.delete(link);
            return true;
        } catch (UnsupportedOperationException x) {
            return false;
        } catch (IOException x) {
            return false;
        }
    }

    static Path create() throws IOException {
        Path top = Files.createTempDirectory("tree");
        List<Path> dirs = new ArrayList<Path>();

        // create tree
        Queue<Path> queue = new ArrayDeque<Path>();
        queue.add(top);
        int total = 1 + rand.nextInt(20);
        int n = 0;
        Path dir;
        while (((dir = queue.poll()) != null) && (n < total)) {
            int r = Math.min((total-n), (1+rand.nextInt(3)));
            for (int i=0; i<r; i++) {
                String name = "dir" + (++n);
                Path subdir = Files.createDirectory(dir.resolve(name));
                queue.offer(subdir);
                dirs.add(subdir);
            }
        }

        // create a few regular files in the file tree
        int files = dirs.size() * 3;
        for (int i=0; i<files; i++) {
            String name = "file" + (i+1);
            int x = rand.nextInt(dirs.size());
            Files.createFile(dirs.get(x).resolve(name));
        }

        // create a few sym links in the file tree so as to create cycles
        if (supportsLinks(top)) {
            int links = 1 + rand.nextInt(5);
            for (int i=0; i<links; i++) {
                int x = rand.nextInt(dirs.size());
                int y;
                do {
                    y = rand.nextInt(dirs.size());
                } while (y != x);
                String name = "link" + (i+1);
                Path link = dirs.get(x).resolve(name);
                Path target = dirs.get(y);
                Files.createSymbolicLink(link, target);
            }
        }

         return top;
    }

    public static void main(String[] args) throws IOException {
        Path top = create();
        System.out.println(top);
    }
}
