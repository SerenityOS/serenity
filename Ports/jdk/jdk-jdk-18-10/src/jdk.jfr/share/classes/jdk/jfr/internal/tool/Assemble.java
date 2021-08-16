/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.internal.tool;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

final class Assemble extends Command {

    @Override
    public String getName() {
        return "assemble";
    }

    @Override
    public List<String> getOptionSyntax() {
        return Collections.singletonList("<repository> <file>");
    }

    @Override
    public String getDescription() {
        return "Assemble leftover chunks from a disk repository into a recording file";
    }

    @Override
    public void displayOptionUsage(PrintStream stream) {
        stream.println("  <repository>   Directory where the repository is located");
        stream.println();
        stream.println("  <file>         Name of the recording file (.jfr) to create");
    }

    @Override
    public void execute(Deque<String> options) throws UserSyntaxException, UserDataException {
        ensureMinArgumentCount(options, 2);
        ensureMaxArgumentCount(options, 2);
        Path repository = getDirectory(options.pop());

        Path file = Paths.get(options.pop());
        ensureFileDoesNotExist(file);
        ensureFileExtension(file, ".jfr");

        try (FileOutputStream fos = new FileOutputStream(file.toFile())) {
            List<Path> files = listJFRFiles(repository);
            if (files.isEmpty()) {
                throw new UserDataException("no *.jfr files found at " + repository);
            }
            println();
            println("Assembling files... ");
            println();
            transferTo(files, file, fos.getChannel());
            println();
            println("Finished.");
        } catch (IOException e) {
            throw new UserDataException("could not open destination file " + file + ". " + e.getMessage());
        }
    }

    private List<Path> listJFRFiles(Path path) throws UserDataException {
        try {
            List<Path> files = new ArrayList<>();
            if (Files.isDirectory(path)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, "*.jfr")) {
                    for (Path p : stream) {
                        if (!Files.isDirectory(p) && Files.isReadable(p)) {
                            files.add(p);
                        }
                    }
                }
            }
            files.sort((u, v) -> u.getFileName().compareTo(v.getFileName()));
            return files;
        } catch (IOException ioe) {
            throw new UserDataException("could not list *.jfr for directory " + path + ". " + ioe.getMessage());
        }
    }

    private void transferTo(List<Path> sourceFiles, Path output, FileChannel out) throws UserDataException {
        long pos = 0;
        for (Path p : sourceFiles) {
            println(" " + p.toString());
            try (FileChannel sourceChannel = FileChannel.open(p)) {
                long rem = Files.size(p);
                while (rem > 0) {
                    long n = Math.min(rem, 1024 * 1024);
                    long w = out.transferFrom(sourceChannel, pos, n);
                    pos += w;
                    rem -= w;
                }
            } catch (IOException ioe) {
                throw new UserDataException("could not copy recording chunk " + p + " to new file. " + ioe.getMessage());
            }
        }
    }
}
