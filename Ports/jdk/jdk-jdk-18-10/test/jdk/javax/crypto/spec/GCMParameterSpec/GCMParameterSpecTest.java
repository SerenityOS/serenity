/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7031343
 * @summary Provide API changes to support GCM AEAD ciphers
 * @author Brad Wetmore
 */

import javax.crypto.AEADBadTagException;
import javax.crypto.spec.GCMParameterSpec;
import java.util.Arrays;

public class GCMParameterSpecTest {

    // 16 elements
    private static byte[] bytes = new byte[] {
        0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17,
        0x18, 0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f };

    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        newGCMParameterSpecFail(-1, bytes);
        newGCMParameterSpecFail(128, null);
        newGCMParameterSpecPass(128, bytes);

        newGCMParameterSpecFail(-1, bytes, 2, 4);
        newGCMParameterSpecFail(128, null, 2, 4);
        newGCMParameterSpecFail(128, bytes, -2, 4);
        newGCMParameterSpecFail(128, bytes, 2, -4);
        newGCMParameterSpecFail(128, bytes, 2, 15);  // one too many

        newGCMParameterSpecPass(128, bytes, 2, 14);  // ok.
        newGCMParameterSpecPass(96, bytes, 4, 4);
        newGCMParameterSpecPass(96, bytes, 0, 0);

        // Might as well check the Exception constructors.
        try {
            new AEADBadTagException();
            new AEADBadTagException("Bad Tag Seen");
        } catch (Exception e) {
            e.printStackTrace();
            failed++;
        }

        if (failed != 0) {
            throw new Exception("Test(s) failed");
        }
    }

    private static void newGCMParameterSpecPass(
            int tLen, byte[] src) {
        try {
            GCMParameterSpec gcmps = new GCMParameterSpec(tLen, src);
            if (gcmps.getTLen() != tLen) {
                throw new Exception("tLen's not equal");
            }
            if (!Arrays.equals(gcmps.getIV(), src)) {
                throw new Exception("IV's not equal");
            }
        } catch (Exception e) {
            e.printStackTrace();
            failed++;
        }
    }

    private static void newGCMParameterSpecFail(
            int tLen, byte[] src) {
        try {
            new GCMParameterSpec(tLen, src);
            new Exception("Didn't Fail as Expected").printStackTrace();
            failed++;
        } catch (IllegalArgumentException e) {
            // swallow
        }
    }

    private static void newGCMParameterSpecPass(
            int tLen, byte[] src, int offset, int len) {
        try {
            GCMParameterSpec gcmps =
                new GCMParameterSpec(tLen, src, offset, len);
            if (gcmps.getTLen() != tLen) {
                throw new Exception("tLen's not equal");
            }
            if (!Arrays.equals(gcmps.getIV(),
                    Arrays.copyOfRange(src, offset, offset + len))) {
                System.out.println(offset + " " + len);
                System.out.println(Arrays.copyOfRange(src, offset, len)[0]);
                throw new Exception("IV's not equal");
            }
        } catch (Exception e) {
            e.printStackTrace();
            failed++;
        }
    }

    private static void newGCMParameterSpecFail(
            int tLen, byte[] src, int offset, int len) {
        try {
            new GCMParameterSpec(tLen, src, offset, len);
            new Exception("Didn't Fail as Expected").printStackTrace();
            failed++;
        } catch (IllegalArgumentException e) {
            // swallow
        }
    }
}
