/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6944033
 * @summary Tests that PCM_FLOAT encoding is supported
 * @compile PCM_FLOAT_support.java
 * @run main PCM_FLOAT_support
 * @author Alex Menkov
 *
 */

import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioSystem;


public class PCM_FLOAT_support {

    static Encoding pcmFloatEnc;

    static boolean testFailed = false;

    public static void main(String[] args) throws Exception {
        // 1st checks Encoding.PCM_FLOAT is available
        pcmFloatEnc = Encoding.PCM_FLOAT;

        Encoding[] encodings = AudioSystem.getTargetEncodings(pcmFloatEnc);
        out("conversion from PCM_FLOAT to " + encodings.length + " encodings:");
        for (Encoding e: encodings) {
            out("  - " + e);
        }
        if (encodings.length == 0) {
            testFailed = true;
        }

        test(Encoding.PCM_SIGNED);
        test(Encoding.PCM_UNSIGNED);

        if (testFailed) {
            throw new Exception("test failed");
        }
        out("test passed.");
    }

    static void out(String s) {
        System.out.println(s);
    }

    static boolean test(Encoding enc) {
        out("conversion " + enc + " -> PCM_FLOAT:");
        Encoding[] encodings = AudioSystem.getTargetEncodings(enc);
        for (Encoding e: encodings) {
            if (e.equals(pcmFloatEnc)) {
                out("  - OK");
                return true;
            }
        }
        out("  - FAILED (not supported)");
        testFailed = true;
        return false;
    }

}
