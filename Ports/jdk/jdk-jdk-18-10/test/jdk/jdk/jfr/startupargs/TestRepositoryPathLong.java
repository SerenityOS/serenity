/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.startupargs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.test.lib.Asserts;

/**
 * @test
 * @summary Set repository path. Verify recording created in repo.
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm -XX:StartFlightRecording:name=myrec,settings=profile -XX:FlightRecorderOptions:repository=./subdirectory/subdirectory1/subdirectory2/subdirectory3/subdirectory4/subdirectory5/subdirectory6/subdirectory7/subdirectory8/subdirectory9/subdirectory10/subdirectory11/subdirectory12/subdirectory13/subdirectory14/subdirectory15 jdk.jfr.startupargs.TestRepositoryPathLong
 */
public class TestRepositoryPathLong {

    public static void main(String[] args) throws Exception {
        final Path repo = Paths.get(
                        "./subdirectory/subdirectory1/subdirectory2/subdirectory3/subdirectory4/subdirectory5/" +
                        "subdirectory6/subdirectory7/subdirectory8/subdirectory9/subdirectory10/subdirectory11/" +
                        "subdirectory12/subdirectory13/subdirectory14/subdirectory15");
        Asserts.assertTrue(Files.isDirectory(repo), "Repo path does not exist: " + repo);

        Path recordingPath = StartupHelper.findRecordingFileInRepo(repo);
        System.out.println("recordingPath: " + recordingPath);
        Asserts.assertNotNull(recordingPath, "Could not find recording file in repository " + repo);
    }

}
