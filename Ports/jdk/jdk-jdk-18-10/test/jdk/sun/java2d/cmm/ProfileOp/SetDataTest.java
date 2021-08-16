/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     7042594 8028206
 * @summary Test verifies that ICC_Profile.setData() conforms the spec.
 *
 * @run     main SetDataTest
 */


import java.util.ArrayList;
import java.util.List;
import java.awt.color.ICC_Profile;
import static java.awt.color.ICC_ColorSpace.CS_GRAY;
import static java.awt.color.ICC_Profile.icSigGrayTRCTag;
import static java.awt.color.ICC_Profile.icSigRedTRCTag;
import static java.awt.color.ICC_Profile.icSigGreenTRCTag;

public class SetDataTest {

    static class TestCase {

        static ICC_Profile profile;
        static byte[] validTRCdata;
        static byte[] invalidTRCData;

        static {
            profile = ICC_Profile.getInstance(CS_GRAY);
            validTRCdata = profile.getData(icSigGrayTRCTag);
            invalidTRCData = new byte[]{0x42, 0x42, 0x42, 0x42, 1, 3, 4, 6,};
        }
        String desciption;
        int signature;
        boolean useValidData;
        Throwable err;
        boolean isIAEexpected;

        public TestCase(String descr, int sig,
                boolean useValidData,
                boolean isIAEexpected) {
            this.desciption = descr;
            this.signature = sig;

            this.useValidData = useValidData;
            this.isIAEexpected = isIAEexpected;

        }

        public void doTest() {
            System.out.println(desciption);
            byte[] data = useValidData
                    ? validTRCdata : invalidTRCData;
            err = null;
            try {
                profile.setData(signature, data);
            } catch (Throwable e) {
                err = e;
                System.out.println("Got exception: " +
                        e.getClass().getName() + ": " +
                        e.getMessage());
            }

            if (isIAEexpected) {
                if (err == null) {
                    throw new RuntimeException(
                            "Test failed: expected exception was not thrown");
                }
                if (!(err instanceof IllegalArgumentException)) {
                    throw new RuntimeException(
                            "Unexpected exception was thrown: " +
                            err.getMessage(), err);
                }
            } else {
                if (err != null) {
                    throw new RuntimeException(
                            "Unexpected exception was thrown: " +
                            err.getMessage(), err);
                }
            }
            System.out.println("Testcase PASSED");
        }
    }

    public static void main(String[] args) {
        List<TestCase> tests = new ArrayList<TestCase>();

        TestCase selfupdate = new TestCase(
                "Selfupdate: update grayTRC with the same data.",
                icSigGrayTRCTag, true, false);
        tests.add(selfupdate);

        TestCase newValdTag = new TestCase(
                "Insert new valid tag",
                icSigRedTRCTag,
                true, false);
        tests.add(newValdTag);

        TestCase newInvalidTag = new TestCase(
                "Insert new tag with invalid contet",
                icSigGreenTRCTag,
                false, true);
        tests.add(newInvalidTag);

        TestCase newUnknowInvalidTag = new TestCase(
                "Insert new tag with invalid data and unknown signature",
                0x41414141,
                false, true);
        tests.add(newUnknowInvalidTag);

        TestCase newUnknownValidTag = new TestCase(
                "Insert new tag with valid data and unknown signatiure",
                0x41414141,
                true, true);
        tests.add(newUnknownValidTag);

        for (TestCase t: tests) {
            t.doTest();
        }
        System.out.println("Test passed!.");
    }
}
