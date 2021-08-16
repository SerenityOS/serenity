/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import jdk.jfr.Configuration;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordingFile;
import jdk.test.lib.process.OutputAnalyzer;

/**
 * @test
 * @bug 8253050
 * @summary Test jfr split
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.tool.TestDisassemble
 */
public class TestDisassemble {

    public static void main(String[] args) throws Throwable {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        String dateText = formatter.format(new Date());

        Path recordingFileA = Paths.get("many-chunks-A-" + dateText + ".jfr");
        Path recordingFileB = Paths.get("many-chunks-B-" + dateText + ".jfr");
        Path recordingFileC = Paths.get("many-chunks-C-" + dateText + ".jfr");
        Path recordingFileD = Paths.get("many-chunks-D-" + dateText + ".jfr");
        makeRecordingWithChunks(6, recordingFileA);
        Files.copy(recordingFileA, recordingFileB);
        Files.copy(recordingFileA, recordingFileC);
        Files.copy(recordingFileA, recordingFileD);

        String fileAText = recordingFileA.toAbsolutePath().toString();
        String fileBText = recordingFileB.toAbsolutePath().toString();
        String fileCText = recordingFileC.toAbsolutePath().toString();
        String fileDText = recordingFileD.toAbsolutePath().toString();

        OutputAnalyzer output = ExecuteHelper.jfr("disassemble");
        output.shouldContain("missing file");

        output = ExecuteHelper.jfr("disassemble", "--wrongOption", fileAText);
        output.shouldContain("unknown option");

        output = ExecuteHelper.jfr("disassemble", "--wrongOption", "1", fileAText);
        output.shouldContain("unknown option");

        output = ExecuteHelper.jfr("disassemble", "--max-chunks", "-3", fileAText);
        output.shouldContain("max chunks must be at least 1");

        output = ExecuteHelper.jfr("disassemble", "--max-chunks", "1000", fileAText);
        output.shouldContain("number of chunks in recording");
        output.shouldContain("doesn't exceed max chunks");
        output = ExecuteHelper.jfr("disassemble", fileAText); // maxchunks is 5 by
                                                        // default
        System.out.println(output.getOutput());
        System.out.println(fileAText);
        verifyRecording(fileAText.substring(0, fileAText.length() - 4) + "_1.jfr");
        verifyRecording(fileAText.substring(0, fileAText.length() - 4) + "_2.jfr");

        output = ExecuteHelper.jfr("disassemble", "--max-chunks", "2", fileBText);

        verifyRecording(fileBText.substring(0, fileBText.length() - 4) + "_1.jfr");
        verifyRecording(fileBText.substring(0, fileBText.length() - 4) + "_2.jfr");
        verifyRecording(fileBText.substring(0, fileBText.length() - 4) + "_3.jfr");

        output = ExecuteHelper.jfr("disassemble", "--max-chunks", "2", fileBText);
        output.shouldContain("file with that name already exist");

        // sanity check
        output = ExecuteHelper.jfr("disassemble", "--max-size", "10000", fileCText);
        verifyRecording(fileCText.substring(0, fileCText.length() - 4) + "_01.jfr");

        // test JDK-8253050
        output = ExecuteHelper.jfr("disassemble", "--max-chunks", "1", fileDText);
        String chunks = output.firstMatch("File consists of (\\d+) chunks", 1);
        output.shouldContain("The recording will be split into " + chunks + " files");
        String chunkFilePrefix = fileDText.substring(0, fileDText.length() - 4) + "_";
        for (long i = 0; i < Long.parseLong(chunks); i++) {
            verifyRecording(chunkFilePrefix + String.format("%0" + chunks.length() + "d", i) + ".jfr");
        }
    }

    private static void verifyRecording(String name) throws IOException {
        System.out.println("Disassembling: " + name);
        try (RecordingFile rf = new RecordingFile(Paths.get(name))) {
            rf.readEvent();
        }
    }

    // Will create at least 2 * count + 1 chunks.
    private static void makeRecordingWithChunks(int count, Path file) throws IOException, ParseException {
        Recording main = new Recording(Configuration.getConfiguration("default"));
        main.setToDisk(true);
        main.start();
        for (int i = 0; i < count; i++) {
            Recording r = new Recording();
            r.setToDisk(true);
            r.start();
            r.stop();
            r.close();
        }
        main.stop();
        main.dump(file);
        main.close();
    }
}
