/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.api.recording.dump;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordingFile;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.CommonHelper;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.VoidFunction;

/**
 * @test
 * @summary Test copyTo and parse file
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.recording.dump.TestDumpInvalid
 */
public class TestDumpInvalid {

    public static void main(String[] args) throws Throwable {
        Recording r = new Recording();
        r.enable(EventNames.OSInformation);
        r.start();
        r.stop();

        verifyNullPointer(()->{r.dump(null);}, "No NullPointerException");

        Path pathNotExists = Paths.get(".", "dirNotExists", "my.jfr");
        verifyFileNotFound(()->{r.dump(pathNotExists);}, "No Exception with missing dir");

        Path pathEmpty = Paths.get("");
        verifyFileNotFound(()->{r.dump(pathEmpty);}, "No Exception with empty path");

        Path pathDir = Paths.get(".", "newdir");
        Files.createDirectory(pathDir);
        verifyFileNotFound(()->{r.dump(pathDir);}, "No Exception with dir");

        // Verify that copyTo() works after all failed attempts.
        Path pathOk = Paths.get(".", "newdir", "my.jfr");
        r.dump(pathOk);
        Asserts.assertTrue(Files.exists(pathOk), "Recording file does not exist: " + pathOk);
        Asserts.assertFalse(RecordingFile.readAllEvents(pathOk).isEmpty(), "No events found");

        r.close();
    }

    private static void verifyFileNotFound(VoidFunction f, String msg) throws Throwable {
        CommonHelper.verifyException(f, msg, IOException.class);
    }

    private static void verifyNullPointer(VoidFunction f, String msg) throws Throwable {
        CommonHelper.verifyException(f, msg, NullPointerException.class);
    }

}
