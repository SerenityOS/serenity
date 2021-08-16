/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8239950
 * @summary Update PKCS9 Attributes to PKCS#9 v2.0 Encodings
 * @library /test/lib
 * @modules java.base/sun.security.pkcs
            java.base/sun.security.util
 */

import java.io.IOException;
import java.util.*;
import sun.security.pkcs.PKCS9Attribute;
import sun.security.util.DerValue;
import jdk.test.lib.Utils;

public class PKCS9AttrTypeTests {

    static final Map<String, String> TEST_INPUT_GOOD =
            new LinkedHashMap<String, String>() {{

                // Challenge Password
                put("challengePasssword as TeletexString",
                    "301e06092a864886f70d010907311114" +
                    "0f5468697349734150617373776f7264");
                put("challengePasssword as PrintableString",
                    "301e06092a864886f70d010907311113" +
                    "0f5468697349734150617373776f7264");
                put("challengePasssword as UniversalString",
                    "304b06092a864886f70d010907313e1c" +
                    "3c000000540000006800000069000000" +
                    "73000000490000007300000041000000" +
                    "50000000610000007300000073000000" +
                    "770000006f0000007200000064");
                put("challengePasssword as UTF8String",
                    "301e06092a864886f70d01090731110c" +
                    "0f5468697349734150617373776f7264");
                put("challengePasssword as BMPString",
                    "302d06092a864886f70d01090731201e" +
                    "1e005400680069007300490073004100" +
                    "500061007300730077006f00720064");

                // Unstructured Name
                put("unstructuredName as IA5String",
                    "301b06092a864886f70d010902310e16" +
                    "0c5468697349734d794e616d65");
                put("unstructuredName as TeletexString",
                    "301b06092a864886f70d010902310e14" +
                    "0c5468697349734d794e616d65");
                put("unstructuredName as PrintableString",
                    "301b06092a864886f70d010902310e13" +
                    "0c5468697349734d794e616d65");
                put("unstructuredName as UniversalString",
                    "303f06092a864886f70d01090231321c" +
                    "30000000540000006800000069000000" +
                    "7300000049000000730000004d000000" +
                    "790000004e000000610000006d000000" +
                    "65");
                put("unstructuredName as UTF8String",
                    "301b06092a864886f70d010902310e0c" +
                    "0c5468697349734d794e616d65");
                put("unstructuredName as BMPString",
                    "302706092a864886f70d010902311a1e" +
                    "18005400680069007300490073004d00" +
                    "79004e0061006d0065");
                put("unstructuredName as Multi-Valued",
                    "302d06092a864886f70d010902312013" +
                    "10546869734973416c736f4d794e616d" +
                    "65160c5468697349734d794e616d65");

                // Unstructured Address
                put("unstructuredAddress as TeletexString",
                    "301e06092a864886f70d010908311114" +
                    "0f5468697349734d7941646472657373");
                put("unstructuredAddress as PrintableString",
                    "301e06092a864886f70d010908311113" +
                    "0f5468697349734d7941646472657373");
                put("unstructuredAddress as UniversalString",
                    "304b06092a864886f70d010908313e1c" +
                    "3c000000540000006800000069000000" +
                    "7300000049000000730000004d000000" +
                    "79000000410000006400000064000000" +
                    "72000000650000007300000073");
                put("unstructuredAddress as UTF8String",
                    "301e06092a864886f70d01090831110c" +
                    "0f5468697349734d7941646472657373");
                put("unstructuredAddress as BMPString",
                    "302d06092a864886f70d01090831201e" +
                    "1e005400680069007300490073004d00" +
                    "790041006400640072006500730073");
                put("unstructuredAddress as Multi-Valued",
                    "303306092a864886f70d01090831260c" +
                    "13546869734973416c736f4d79416464" +
                    "72657373130f5468697349734d794164" +
                    "6472657373");

                // Signing Time
                put("signingTime as UTCTime",
                    "301c06092a864886f70d010905310f17" +
                    "0d3635303533313132303030305a");
                put("signingTime as GeneralizedTime",
                    "301e06092a864886f70d010905311118" +
                    "0f32303530303533313132303030305a");
            }};

    static final Map<String, String> TEST_INPUT_BAD =
            new LinkedHashMap<String, String>() {{

                // Challenge Password
                put("challengePasssword as IA5String",
                    "301e06092a864886f70d010907311116" +
                    "0f5468697349734150617373776f7264");
                put("challengePassword as Multi-Valued",
                    "303306092a864886f70d01090731260c" +
                    "13546869734973416c736f4150617373" +
                    "776f7264130f54686973497341506173" +
                    "73776f7264");

                // Unstructured Name
                put("unstructuredName as UTCTime",
                    "301c06092a864886f70d010902310f17" +
                    "0d3635303533313132303030305a");

                // Unstructured Address
                put("unstructuredAddress as IA5String",
                    "301e06092a864886f70d010908311116" +
                    "0f5468697349734d7941646472657373");

                // Signing Time
                put("signingTime as Multi-Valued",
                    "302b06092a864886f70d010905311e17" +
                    "0d3230303432383035303030305a170d" +
                    "3635303533313132303030305a");              // DKN
            }};

    public static void main(String[] args) throws Exception {
        int failedTests = 0;

        for (Map.Entry<String, String> entry : TEST_INPUT_GOOD.entrySet()) {
            try {
                System.out.print("Test - " + entry.getKey() + ": ");

                // Decode each Base64 test vector into DER and place into
                // a DerValue object to be consumed by PKCS9Attribute.
                PKCS9Attribute p9Attr = new PKCS9Attribute(
                        new DerValue(Utils.toByteArray(entry.getValue())));
                System.out.println("PASS");
                System.out.println("---------------");
                System.out.println(p9Attr);
                System.out.println("---------------");
            } catch (IOException ioe) {
                failedTests++;
                System.out.println("FAIL:");
                ioe.printStackTrace(System.out);
                System.out.println();
            }
        }

        for (Map.Entry<String, String> entry : TEST_INPUT_BAD.entrySet()) {
            try {
                System.out.print("Test - " + entry.getKey() + ": ");

                // Decode each Base64 test vector into DER and place into
                // a DerValue object to be consumed by PKCS9Attribute.
                PKCS9Attribute p9Attr = new PKCS9Attribute(
                        new DerValue(
                            Base64.getMimeDecoder().decode(entry.getValue())));
                failedTests++;
                System.out.println("FAIL: Expected IOException did not occur");
            } catch (IOException ioe) {
                System.out.println("PASS\n" + ioe);
            }

        }

        if (failedTests > 0) {
            throw new RuntimeException(
                    "One or more test cases failed, see output");
        }
    }
}
