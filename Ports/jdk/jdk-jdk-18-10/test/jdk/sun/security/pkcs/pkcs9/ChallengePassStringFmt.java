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
 * @bug 8239094
 * @summary PKCS#9 ChallengePassword attribute does not allow for the
 *          UTF8String type
 * @modules java.base/sun.security.pkcs10
 */

import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import sun.security.pkcs10.PKCS10;

public class ChallengePassStringFmt {

    static final Map<String, String> TEST_INPUT = Map.of(
        "PKCS10 with password as TeletexString",
        "MIIBBzCBrQIBADAoMQ0wCwYDVQQKEwRUZXN0MRcwFQYDVQQDEw5UZXN0IFQ2MVN0\n" +
        "cmluZzBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABH9MshRAqmpY6o80JJY2DCA6\n" +
        "uN6GoG79khaIkdjEw0HHAkQDJ0aLPsZ87lqqba7NvmLw8wi/AXiyTLdBHOhy2n+g\n" +
        "IzAhBgkqhkiG9w0BCQcxFBQSVDYxU3RyaW5nIXBhc3N3b3JkMAoGCCqGSM49BAMC\n" +
        "A0kAMEYCIQDv6sj5Jf1yocHEiD8sZ6F8YMP3lcyzrhwrfZ16xN9azwIhAL7GJGgQ\n" +
        "LmAbXTm59gWL7uofniwX22vv55J4nWt7a3jI",

        "PKCS10 with password as PrintableString",
        "MIIBDzCBtQIBADAuMQ0wCwYDVQQKEwRUZXN0MR0wGwYDVQQDExRUZXN0IFByaW50\n" +
        "YWJsZVN0cmluZzBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABD4JaJAehTeZy4Fx\n" +
        "jxNUZqhzwywIUjoK8kzYenEFRLiqCpDynhixE3zEsnFEzXsF1V7BX5HhU8NE2xrH\n" +
        "GrDPY/agJTAjBgkqhkiG9w0BCQcxFhMUUHJpbnRhYmxlU3RyaW5nIHBhc3MwCgYI\n" +
        "KoZIzj0EAwIDSQAwRgIhANmqfVcArwm0+C/5MJqUpbGqryYzGlHunmUpbKxTrt9T\n" +
        "AiEAiAmSSLvyfoXms8f6+1q2NElVNIj6ULherOEuU13Hd8U=",

        "PKCS10 with password as BMPString",
        "MIIBGDCBvwIBADAoMQ0wCwYDVQQKEwRUZXN0MRcwFQYDVQQDEw5UZXN0IEJNUFN0\n" +
        "cmluZzBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABEC6gpNha74xeCabdi647rXq\n" +
        "5unD5FTgA5RGGUd+uixOjwrRrCWTQbZ1//1MrioKbzWB1BjKmJI0N2plwIBmbAGg\n" +
        "NTAzBgkqhkiG9w0BCQcxJh4kAEIATQBQAFMAdAByAGkAbgBnACEAcABhAHMAcwB3\n" +
        "AG8AcgBkMAoGCCqGSM49BAMCA0gAMEUCIDN2n8G+jzKamNmTJkSixgiq3ysR1GHY\n" +
        "5e0J5zRjtMtHAiEAy3me/gRIXa2OecrXGC+UjYJ1bLKkr1xadiolFv+1fkQ=",

        "PKCS10 with password as UniversalString",
        "MIIBPzCB5QIBADAuMQ0wCwYDVQQKDARUZXN0MR0wGwYDVQQDDBRUZXN0IFVuaXZl\n" +
        "cnNhbFN0cmluZzBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABFe/CetRtzHEeN7J\n" +
        "fDi020fGb96RqMHFr/yuwcVmao3tZzSbYDZwLrMdk77PeX25GJG9vB6fgepaOXHS\n" +
        "uVJlB6ugVTBTBgkqhkiG9w0BCQcxRhxEAAAAVQAAAG4AAABpAAAAdgAAAGUAAABy\n" +
        "AAAAcwAAAGEAAABsAAAAUAAAAGEAAABzAAAAcwAAAHcAAABvAAAAcgAAAGQwCgYI\n" +
        "KoZIzj0EAwIDSQAwRgIhAJIQmTp0HyHHzGbdr68wG7N5twevt+3VipsU8Kk10LiX\n" +
        "AiEAkU/X7dDtjlIM6BHdssIlRlS/KFhmsLDq6RgREFmdjBQ=",

        "PKCS10 with password as UTF8String",
        "MIH7MIGiAgEAMCMxDTALBgNVBAoMBFRlc3QxEjAQBgNVBAMMCVRlc3QgVVRGODBZ\n" +
        "MBMGByqGSM49AgEGCCqGSM49AwEHA0IABCvIy9BZ/hvmP9WdeIVnqPmbhcTl9IDy\n" +
        "9uzWh5PH04u4LXUWWPedQL7DWBK9pRlV5HgvuPll0mMmC6goewqOC6SgHTAbBgkq\n" +
        "hkiG9w0BCQcxDgwMdXRmOHBhc3N3b3JkMAoGCCqGSM49BAMCA0gAMEUCIQD396fy\n" +
        "H2maO/rAj0EIWyNs9dFrDGf/IN08+qj8YFn0jgIgEJ5sXV2GLKX5CqfeyTWyu02f\n" +
        "WEf4+EIuvcItbM4jhbs="
    );

    public static void main(String[] args) throws Exception {
        int failedTests = 0;

        for (Map.Entry<String, String> entry : TEST_INPUT.entrySet()) {
            try {
                System.out.print("Test - " + entry.getKey() + ": ");

                // If the PKCS9 challengePassword attribute cannot accept the
                // DirectoryString encoding for the password, parsing the
                // PKCS10 should fail.
                PKCS10 req = new PKCS10(Base64.getMimeDecoder().
                        decode(entry.getValue()));

                System.out.println("PASS");
            } catch (IOException ioe) {
                failedTests++;
                System.out.println("FAIL: " + ioe);
                ioe.printStackTrace(System.out);
                System.out.println();
            }
        }

        if (failedTests > 0) {
            throw new RuntimeException(
                    "One or more test cases failed, see output");
        }
    }
}
