/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import jdk.jfr.internal.consumer.ChunkHeader;
import jdk.jfr.internal.consumer.FileAccess;
import jdk.jfr.internal.consumer.RecordingInput;

final class Disassemble extends Command {

    @Override
    public String getName() {
        return "disassemble";
    }

    @Override
    public List<String> getOptionSyntax() {
        List<String> list = new ArrayList<>();
        list.add("[--output <directory>]");
        list.add("[--max-chunks <chunks>]");
        list.add("[--max-size <size>]");
        list.add("<file>");
        return list;
    }

    @Override
    public void displayOptionUsage(PrintStream stream) {
        stream.println(" --output <directory>    The location to write the disassembled file,");
        stream.println("                         by default the current directory");
        stream.println("");
        stream.println(" --max-chunks <chunks>   Maximum number of chunks per disassembled file,");
        stream.println("                         by default 5. The chunk size varies, but is ");
        stream.println("                         typically around 15 MB.");
        stream.println("");
        stream.println(" --max-size <size>       Maximum number of bytes per file.");
        stream.println("");
        stream.println("  <file>                 Location of the recording file (.jfr)");
    }

    @Override
    public String getDescription() {
        return "Disassemble a recording file into smaller files/chunks";
    }

    @Override
    public void execute(Deque<String> options) throws UserSyntaxException, UserDataException {
        if (options.isEmpty()) {
            throw new UserSyntaxException("missing file");
        }
        Path file = getJFRInputFile(options);
        int maxChunks = Integer.MAX_VALUE;
        int maxsize = Integer.MAX_VALUE;
        String output = System.getProperty("user.dir");
        int optionCount = options.size();
        while (optionCount > 0) {
            if (acceptOption(options, "--output")) {
                output = options.pop();
            }
            if (acceptOption(options, "--max-size")) {
                String value = options.pop();
                try {
                    maxsize = Integer.parseInt(value);
                    if (maxsize < 1) {
                        throw new UserDataException("max size must be at least 1");
                    }
                } catch (NumberFormatException nfe) {
                    throw new UserDataException("not a valid value for --max-size.");
                }
            }
            if (acceptOption(options, "--max-chunks")) {
                String value = options.pop();
                try {
                    maxChunks = Integer.parseInt(value);
                    if (maxChunks < 1) {
                        throw new UserDataException("max chunks must be at least 1.");
                    }
                } catch (NumberFormatException nfe) {
                    throw new UserDataException("not a valid value for --max-size.");
                }
            }
            if (optionCount == options.size()) {
                // No progress made
                throw new UserSyntaxException("unknown option " + options.peek());
            }
            optionCount = options.size();
        }
        Path outputPath = getDirectory(output);

        println();
        println("Examining recording " + file + " ...");
        List<Long> sizes;
        if (maxsize != Integer.MAX_VALUE && maxChunks == Integer.MAX_VALUE) {
            try {
                long fileSize = Files.size(file);
                if (maxsize >=fileSize) {
                    println();
                    println("File size (" + fileSize +") does not exceed max size (" + maxsize + ")");
                    return;
                }
            } catch (IOException e) {
                throw new UserDataException("unexpected i/o error when determining file size" + e.getMessage());
            }
        }
        if (maxsize == Integer.MAX_VALUE && maxChunks == Integer.MAX_VALUE) {
            maxChunks = 5;
        }

        try {
            sizes = findChunkSizes(file);
        } catch (IOException e) {
            throw new UserDataException("unexpected i/o error. " + e.getMessage());
        }
        if (maxsize == Integer.MAX_VALUE == sizes.size() <= maxChunks) {
            throw new UserDataException("number of chunks in recording (" + sizes.size() + ") doesn't exceed max chunks (" + maxChunks + ")");
        }
        println();
        if (sizes.size() > 0) {
            List<Long> combinedSizes = combineChunkSizes(sizes, maxChunks, maxsize);
            print("File consists of " + sizes.size() + " chunks. The recording will be split into ");
            println(combinedSizes.size() + " files");
            println();
            splitFile(outputPath, file, combinedSizes);
        } else {
            throw new UserDataException("no JFR chunks found in file.");
        }
    }

    private List<Long> findChunkSizes(Path p) throws IOException {
        try (RecordingInput input = new RecordingInput(p.toFile(), FileAccess.UNPRIVILEGED)) {
            List<Long> sizes = new ArrayList<>();
            ChunkHeader ch = new ChunkHeader(input);
            sizes.add(ch.getSize());
            while (!ch.isLastChunk()) {
                ch = ch.nextHeader();
                sizes.add(ch.getSize());
            }
            return sizes;
        }
    }

    private List<Long> combineChunkSizes(List<Long> sizes, int maxChunks, long maxSize) {
        List<Long> reduced = new ArrayList<Long>();
        int chunks = 1;
        long fileSize = sizes.get(0);
        for (int i = 1; i < sizes.size(); i++) {
            long size = sizes.get(i);
            if (fileSize + size > maxSize || chunks == maxChunks) {
                reduced.add(fileSize);
                chunks = 1;
                fileSize = size;
                continue;
            }
            fileSize += size;
            chunks++;
        }
        if (fileSize != 0) {
            reduced.add(fileSize);
        }
        return reduced;
    }

    private void splitFile(Path directory, Path file, List<Long> splitPositions) throws UserDataException {
        int padAmountZeros = String.valueOf(splitPositions.size() - 1).length();
        String fileName = file.getFileName().toString();
        String fileFormatter = fileName.subSequence(0, fileName.length() - 4) + "_%0" + padAmountZeros + "d.jfr";
        for (int i = 0; i < splitPositions.size(); i++) {
            String formattedFilename = String.format(fileFormatter, i);
            try {
                Path p = directory.resolve(formattedFilename);
                if (Files.exists(p)) {
                    throw new UserDataException("can't create disassembled file " + p + ", a file with that name already exist");
                }
            } catch (InvalidPathException ipe) {
                throw new UserDataException("can't construct path with filename" + formattedFilename);
            }
        }

        try (DataInputStream stream = new DataInputStream(new BufferedInputStream(new FileInputStream(file.toFile())))) {
            for (int i = 0; i < splitPositions.size(); i++) {
                Long l = splitPositions.get(i);
                byte[] bytes = readBytes(stream, l.intValue());
                String formattedFilename = String.format(fileFormatter, i);
                Path p = directory.resolve(formattedFilename);
                File splittedFile = p.toFile();
                println("Writing " + splittedFile + " ... " + bytes.length);
                FileOutputStream fos = new FileOutputStream(splittedFile);
                fos.write(bytes);
                fos.close();
            }
        } catch (IOException ioe) {
            throw new UserDataException("i/o error writing file " + file);
        }
    }

    private byte[] readBytes(InputStream stream, int count) throws UserDataException, IOException {
        byte[] data = new byte[count];
        int totalRead = 0;
        while (totalRead < data.length) {
            int read = stream.read(data, totalRead, data.length - totalRead);
            if (read == -1) {
                throw new UserDataException("unexpected end of data");
            }
            totalRead += read;
        }
        return data;
    }
}
