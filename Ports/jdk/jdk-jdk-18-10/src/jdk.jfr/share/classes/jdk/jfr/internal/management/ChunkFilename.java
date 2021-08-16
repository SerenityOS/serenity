/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jfr.internal.management;

import java.nio.file.Paths;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.io.IOException;

import jdk.jfr.internal.SecuritySupport;
import jdk.jfr.internal.Utils;
import jdk.jfr.internal.consumer.FileAccess;

// Allows a remote streaming client to create chunk files
// with same naming scheme as the JVM.
public final class ChunkFilename {
   private static final int MAX_CHUNK_NAMES = 100_000;
   private static final String FILE_EXTENSION = ".jfr";

   private final Path directory;
   private final FileAccess fileAcess;

   private Path lastPath;
   private int counter;

   public static ChunkFilename newUnpriviliged(Path directory) {
       return new ChunkFilename(directory, FileAccess.UNPRIVILEGED);
   }

   public static ChunkFilename newPriviliged(Path directory) {
       return new ChunkFilename(directory, SecuritySupport.PRIVILEGED);
   }

   private ChunkFilename(Path directory, FileAccess fileAccess) {
       // Avoid malicious implementations of Path interface
       this.directory = Paths.get(directory.toString());
       this.fileAcess = fileAccess;
   }

   public String next(LocalDateTime time) throws IOException {
       String filename = Utils.formatDateTime(time);
       Path p = directory.resolve(filename + FILE_EXTENSION);

       // If less than one file per second (typically case)
       if (lastPath == null || !p.equals(lastPath)) {
           if (!fileAcess.exists(p)) {
               counter = 1; // reset counter
               lastPath = p;
               return p.toString();
           }
       }

       // If more than one file per second
       while (counter < MAX_CHUNK_NAMES) {
           String extendedName = String.format("%s_%02d%s", filename, counter, FILE_EXTENSION);
           p = directory.resolve(extendedName);
           counter++;
           if (!fileAcess.exists(p)) {
               return p.toString();
           }
       }
       throw new IOException("Unable to find unused filename after " + counter + " attempts");
   }
}
