/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6225664 6220618
 * @summary Test for when private transport library outside jdk
 * @comment converted from test/jdk/com/sun/jdi/PrivateTransportTest.sh
 *
 * @library /test/lib
 * @run compile -g HelloWorld.java
 * @build PrivateTransportTest
 * @run main/othervm PrivateTransportTest
 */

import jdk.test.lib.Platform;
import jdk.test.lib.Utils;
import jdk.test.lib.process.ProcessTools;

import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class PrivateTransportTest {

    public static void main(String argv[]) throws Throwable {
        new PrivateTransportTest()
                .test();
    }

    private final Path transportLib;
    private final String pathEnvVar;
    private final String pathSep;

    private static boolean isTransportLib(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return fileName.equals("dt_socket.dll")
                || fileName.equals("libdt_socket.so")
                || fileName.equals("libdt_socket.dylib");
    }

    private PrivateTransportTest() throws IOException {
        Path jdkRoot = Paths.get(Utils.TEST_JDK);
        try (Stream<Path> files = Files.find(jdkRoot, 5, (path, attr) -> isTransportLib(path.getFileName()))) {
            Optional<Path> foundLib = files.findAny();
            if (!foundLib.isPresent()) {
                throw new RuntimeException("cannot find dt_socket lib");
            }
            transportLib = foundLib.get();
        }
        pathEnvVar = Platform.sharedLibraryPathVariableName();
        pathSep    = File.pathSeparator;
    }

    private void test() throws Throwable {
        // copy existing dt_socket lib to <CLASSES>/private_dt_socket
        String libName = transportLib.getFileName().toString().replace("dt_socket", "private_dt_socket");
        Files.copy(transportLib, Paths.get(Utils.TEST_CLASSES).resolve(libName));

        ProcessBuilder pb = ProcessTools.createTestJvm(
                "-agentlib:jdwp=transport=private_dt_socket,server=y,suspend=n",
                "-classpath", Utils.TEST_CLASSES,
                "HelloWorld");
        Map<String, String> env = pb.environment();
        String pathValue = env.get(pathEnvVar);
        if (pathValue == null) {
            pathValue = Utils.TEST_CLASSES;
        } else {
            pathValue = pathValue + pathSep + Utils.TEST_CLASSES;
        }
        env.put(pathEnvVar, pathValue);

        ProcessTools.executeCommand(pb)
                .shouldHaveExitValue(0);
    }
}
