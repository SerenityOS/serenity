/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8074064
 * @summary OCSPResponse.SingleResponse objects do not parse singleExtensions
 * @modules java.base/sun.security.x509
 *          java.base/sun.security.provider.certpath
 * @run main/othervm OCSPSingleExtensions
 */

import java.io.*;
import java.util.*;
import java.security.cert.*;

import sun.security.x509.SerialNumber;
import sun.security.provider.certpath.*;

/*
 * Tester note:
 * For this test, all input files should be co-located with the test source
 * code.  All test input data should be in PEM format, and may be commented
 * with the '#' character at the beginning of any comment line.  Most tests were
 * generated using the "openssl ocsp" utility in server mode and used the same
 * utility as a client to drive the responses.  In rare cases
 * (ocsp-good-witharchcut.resp, etc.) the test input was manually modified
 * because openssl's ocsp could not generate data in that format (e.g. a
 * "good" response with singleExtensions in the SingleResponse structure.)
 * These tests were created to force the code to walk codepaths reached only
 * with invalid OCSP data or legal formats that are not easily generated using
 * the tools at hand.  These hand-modified test cases will not verify.
 */

public class OCSPSingleExtensions {
    public static CertificateFactory CF;
    public static final File testDir =
            new File(System.getProperty("test.src", "."));
    public static final Base64.Decoder B64D = Base64.getMimeDecoder();

    public static void main(String [] args) throws Exception {
        // Get a CertificateFactory for various tests
        CF = CertificateFactory.getInstance("X509");
        ByteArrayInputStream bais =
                new ByteArrayInputStream(readFile("int.crt").getBytes());
        X509Certificate intCA = (X509Certificate)CF.generateCertificate(bais);
        System.out.println("Successfully instantiated CA cert \"" +
                intCA.getSubjectX500Principal() + "\"");

        CertId cid0x1500 = new CertId(intCA, new SerialNumber(0x1500));
        boolean noFailures = true;

        OCSPResponse.SingleResponse sr =
                getSRByFilename("ocsp-good-nonext.resp", cid0x1500);
        noFailures &= checkSingleExts(sr, 0);

        if (sr.getRevocationTime() != null) {
            throw new RuntimeException("Oops. revocationTime is non-null " +
                    sr.getRevocationTime());
        } else if (sr.getRevocationReason() != null) {
            throw new RuntimeException("Oops. revocationReason is non-null " +
                    sr.getRevocationReason());
        }

        sr = getSRByFilename("ocsp-good-withnext.resp", cid0x1500);
        noFailures &= checkSingleExts(sr, 0);

        sr = getSRByFilename("ocsp-good-witharchcut.resp", cid0x1500);
        noFailures &= checkSingleExts(sr, 1);

        sr = getSRByFilename("ocsp-rev-nocerts.resp", cid0x1500);
        noFailures &= checkSingleExts(sr, 1);

        sr = getSRByFilename("ocsp-rev-nonext-noinv.resp", cid0x1500);
        noFailures &= checkSingleExts(sr, 0);

        sr = getSRByFilename("ocsp-rev-withnext-noinv.resp", cid0x1500);
        noFailures &= checkSingleExts(sr, 0);

        sr = getSRByFilename("ocsp-rev-nonext-withinv.resp", cid0x1500);
        noFailures &= checkSingleExts(sr, 1);

        sr = getSRByFilename("ocsp-rev-withnext-withinv.resp", cid0x1500);
        noFailures &= checkSingleExts(sr, 1);

        try {
            sr = getSRByFilename("ocsp-rev-twonext.resp", cid0x1500);
            System.out.println("FAIL: Allowed two nextUpdate fields");
            noFailures = false;
        } catch (IOException ioe) {
            System.out.println("Caught expected exception: " + ioe);
        }

        try {
            sr = getSRByFilename("ocsp-rev-bad-sr-tag.resp", cid0x1500);
            System.out.println("FAIL: Allowed invalid singleResponse item");
            noFailures = false;
        } catch (IOException ioe) {
            System.out.println("Caught expected exception: " + ioe);
        }

        try {
            sr = getSRByFilename("ocsp-rev-sr-cont-reverse.resp", cid0x1500);
            System.out.println("FAIL: Allowed reversed " +
                    "nextUpdate/singleExtensions");
            noFailures = false;
        } catch (IOException ioe) {
            System.out.println("Caught expected exception: " + ioe);
        }

        if (!noFailures) {
            throw new RuntimeException("One or more tests failed");
        }
    }

    private static OCSPResponse.SingleResponse getSRByFilename(String fileName,
            CertId cid) throws IOException {
        byte[] respDER = B64D.decode(readFile(fileName));
        OCSPResponse or = new OCSPResponse(respDER);
        OCSPResponse.SingleResponse sr = or.getSingleResponse(cid);
        return sr;
    }

    private static String readFile(String fileName) throws IOException {
        String filePath = testDir + "/" + fileName;
        StringBuilder sb = new StringBuilder();

        try (FileReader fr = new FileReader(filePath);
                BufferedReader br = new BufferedReader(fr)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().startsWith("#")) {
                    sb.append(line).append("\n");
                }
            }
        }

        System.out.println("Successfully read " + fileName);
        return sb.toString();
    }

    private static boolean checkSingleExts(OCSPResponse.SingleResponse sr,
            int singleExtCount) {
        Map<String, Extension> singleExts;
        try {
            singleExts = sr.getSingleExtensions();
        } catch (NullPointerException npe) {
            System.out.println(
                    "Warning: Sent null singleResponse into checkSingleExts");
            return false;
        }

        for (String key : singleExts.keySet()) {
            System.out.println("singleExtension: " + singleExts.get(key));
        }

        if (singleExts.size() != singleExtCount) {
            System.out.println("Single Extension count mismatch, " +
                    "expected " + singleExtCount + ", got " +
                    singleExts.size());
            return false;
        } else {
            return true;
        }
    }
}
