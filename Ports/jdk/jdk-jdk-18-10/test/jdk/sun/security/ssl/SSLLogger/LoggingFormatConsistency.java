/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8211227
 * @library /test/lib /javax/net/ssl/templates ../../
 * @summary Tests for consistency in logging format of TLS Versions
 * @run main/othervm LoggingFormatConsistency
 */

/*
 * This test runs in another process so we can monitor the debug
 * results. The OutputAnalyzer must see correct debug output to return a
 * success.
 */

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.security.SecurityUtils;

import java.net.InetAddress;

public class LoggingFormatConsistency extends SSLSocketTemplate {

    LoggingFormatConsistency () {
        serverAddress = InetAddress.getLoopbackAddress();
        SecurityUtils.removeFromDisabledTlsAlgs("TLSv1", "TLSv1.1");
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 0) {
            // A non-empty set of arguments occurs when the "runTest" argument
            // is passed to the test via ProcessTools::executeTestJvm.
            //
            // This is done because an OutputAnalyzer is unable to read
            // the output of the current running JVM, and must therefore create
            // a test JVM. When this case occurs, it will inherit all specified
            // properties passed to the test JVM - debug flags, tls version, etc.
            new LoggingFormatConsistency().run();
        } else {
            // We are in the test JVM that the test is being ran in.
            var testSrc = "-Dtest.src=" + System.getProperty("test.src");
            var javaxNetDebug = "-Djavax.net.debug=all";

            var correctTlsVersionsFormat = new String[]{"TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3"};
            var incorrectTLSVersionsFormat = new String[]{"TLS10", "TLS11", "TLS12", "TLS13"};

            for (var i = 0; i < correctTlsVersionsFormat.length; i++) {
                var expectedTLSVersion = correctTlsVersionsFormat[i];
                var incorrectTLSVersion = incorrectTLSVersionsFormat[i];

                System.out.println("TESTING " + expectedTLSVersion);
                var activeTLSProtocol = "-Djdk.tls.client.protocols=" + expectedTLSVersion;
                var output = ProcessTools.executeTestJvm(
                        testSrc,
                        activeTLSProtocol,
                        javaxNetDebug,
                        "LoggingFormatConsistency",
                        "runTest"); // Ensuring args.length is greater than 0 when test JVM starts

                output.asLines()
                        .stream()
                        .filter(line -> line.startsWith("Connecting to"))
                        .forEach(System.out::println); // prints connection info from test jvm output

                if (output.getExitValue() != 0) {
                    output.asLines().forEach(System.out::println);
                    throw new RuntimeException("Test JVM process failed");
                }

                output.shouldContain(expectedTLSVersion);
                output.shouldNotContain(incorrectTLSVersion);
            }
        }
    }
}
