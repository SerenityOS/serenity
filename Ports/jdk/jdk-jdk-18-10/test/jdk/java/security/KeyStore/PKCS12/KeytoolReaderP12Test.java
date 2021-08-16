/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import jdk.test.lib.process.OutputAnalyzer;
import static java.lang.System.out;
import java.nio.file.Paths;
import java.util.List;

/**
 * @test
 * @bug 8048830
 * @summary Test for PKCS12 keystore list , export commands. Refer README for
 * keystore files information
 * @library ../
 * @library /test/lib
 * @run main KeytoolReaderP12Test
 */
public class KeytoolReaderP12Test {
    private static final String WORKING_DIRECTORY = System.getProperty(
            "test.classes", "."+ File.separator);
    //private static final String KS_PASSWD = "pass";
    private static final String KS_PASSWD = "storepass";
    private static final String CERT_CHAIN_PASSWD = "password";
    private static final String SOURCE_DIRECTORY =
            System.getProperty("test.src", "." + File.separator);

    public static void main(String[] args) throws Exception {
        List<String> expectedValues = null;
        out.println("Self signed test");
        expectedValues = Files.readAllLines(Paths.get(SOURCE_DIRECTORY,
                "api_private_key.p12_expected.data"));
        readTest("api_private_key.p12.data", KS_PASSWD, expectedValues);
        out.println("Self signed test Passed");

        out.println("private key with selfsigned cert, key pair not match");
        expectedValues = Files.readAllLines(Paths.get(SOURCE_DIRECTORY,
                "api_private_key_not_match.p12_expected.data"));
        readTest("api_private_key_not_match.p12.data", KS_PASSWD,
                expectedValues);
        out.println("private key with selfsigned cert, key pair "
                + "not match passed");

        out.println("cert chain test");
        expectedValues = Files.readAllLines(Paths.get(SOURCE_DIRECTORY,
                "api_cert_chain.p12_expected.data"));
        readTest("api_cert_chain.p12.data", CERT_CHAIN_PASSWD, expectedValues);
        out.println("cert chain test passed");

        out.println("IE self test");
        expectedValues = Files.readAllLines(Paths.get(SOURCE_DIRECTORY,
                "ie_self.pfx.pem"));
        exportTest("ie_self.pfx.data", "pkcs12testenduser1",
                KS_PASSWD, expectedValues);
        out.println("IE self test passed");

        out.println("IE chain test");
        expectedValues = Files.readAllLines(Paths.get(SOURCE_DIRECTORY,
                "ie_chain.pfx.pem"));
        exportTest("ie_chain.pfx.data", "servercert",
                CERT_CHAIN_PASSWD, expectedValues);
        out.println("IE chain test passed");

        out.println("Netscape self");
        expectedValues = Files.readAllLines(Paths.get(SOURCE_DIRECTORY,
                "netscape_self.p12.pem"));
        exportTest("netscape_self.p12.data", "pkcs12testenduser1",
                KS_PASSWD, expectedValues);
        out.println("Netscape self passed");

        out.println("Mozilla self test");
        expectedValues = Files.readAllLines(Paths.get(SOURCE_DIRECTORY,
                "mozilla_self.p12.pem"));
        exportTest("mozilla_self.p12.data", "pkcs12testenduser1",
                KS_PASSWD, expectedValues);
        out.println("Mozilla self test passed");

        out.println("Openssl test");
        expectedValues = Files.readAllLines(Paths.get(SOURCE_DIRECTORY,
                "openssl.p12.pem"));
        exportTest("openssl.p12.data", "servercert", CERT_CHAIN_PASSWD, expectedValues);
        out.println("openssl test passed");

        out.println("with different keystore and entrykey password");
        expectedValues = Files.readAllLines(Paths.get(SOURCE_DIRECTORY,
                "api_two_pass.p12_expected.data"));
        readTest("api_two_pass.p12.data", KS_PASSWD,
                expectedValues);
        out.println("two pass test passed");
    }

    private static void readTest(String name, String password,
            List<String> expectedValues)
            throws IOException {
        convertToPFX(name);
        final String[] command = new String[]{"-debug", "-list", "-v",
            "-keystore", WORKING_DIRECTORY + File.separator + name,
            "-storetype", "pkcs12", "-storepass", password};
        runAndValidate(command, expectedValues);
    }

    private static void exportTest(String name, String alias,
            String password, List<String> expectedValues)
            throws IOException {
        convertToPFX(name);
        final String[] command = new String[]{"-debug", "-export", "-alias",
            alias, "-keystore", WORKING_DIRECTORY + File.separator + name,
            "-storepass", password, "-storetype", "pkcs12", "-rfc"};
        runAndValidate(command, expectedValues);
    }

    private static void runAndValidate(String[] command,
            List<String> expectedValues) throws IOException {
        OutputAnalyzer output = Utils.executeKeytoolCommand(command);
        if (expectedValues != null) {
            expectedValues.stream().forEach(line -> {
                output.shouldContain(line);
            });
        }
    }

    /**
     * Decodes the base64 encoded keystore and writes into new file
     * @param name base64 encoded keystore name
     */
    private static void convertToPFX(String name) throws IOException{
        File base64File = new File(SOURCE_DIRECTORY, name);
        File pkcs12File = new File(WORKING_DIRECTORY, name);
        byte[] input = Files.readAllBytes(base64File.toPath());
        Files.write(pkcs12File.toPath(), Base64.getMimeDecoder().
                decode(input), StandardOpenOption.CREATE);
    }
}
