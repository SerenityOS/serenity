/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4937708
 * @summary Tests that AudioFormat.matches handle NOT_SPECIFIED value in all fields
 * @run main Matches_NOT_SPECIFIED
 * @author Alex Menkov
 *
 */

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioSystem;


public class Matches_NOT_SPECIFIED {

    static boolean success = true;
    static AudioFormat f1;
    static AudioFormat f2;

    public static void main(String[] args) throws Exception {
        AudioFormat f3;
        f1 = new AudioFormat(44100, 16, 2, true, false);
        f2 = new AudioFormat(Encoding.PCM_SIGNED,
                AudioSystem.NOT_SPECIFIED,
                AudioSystem.NOT_SPECIFIED,
                AudioSystem.NOT_SPECIFIED,
                AudioSystem.NOT_SPECIFIED,
                AudioSystem.NOT_SPECIFIED, false);
        test(true);

//        f1 = new AudioFormat(44100, 8, 16, true, false);
        f2 = new AudioFormat(Encoding.PCM_SIGNED,
                AudioSystem.NOT_SPECIFIED,
                AudioSystem.NOT_SPECIFIED,
                AudioSystem.NOT_SPECIFIED,
                AudioSystem.NOT_SPECIFIED,
                AudioSystem.NOT_SPECIFIED, true);
        test(false);

        f1 = new AudioFormat(44100, 8, 8, true, false);
        test(true);

        if (success) {
            out("The test PASSED.");
        } else {
            out("The test FAILED.");
            throw new Exception("The test FAILED");
        }
    }

    static void test(boolean shouldMatch) {
        out("testing:");
        out("  - " + f1.toString());
        out("  - " + f2.toString());
        if (f1.matches(f2)) {
            if (shouldMatch) {
                out("  (OK) MATCHES");
            } else {
                out("  (ERROR) MATCHES");
                success = false;
            }
        } else {
            if (shouldMatch) {
                out("  (ERROR) DOESNT MATCH!");
                success = false;
            } else {
                out("  (OK) DOESNT MATCH!");
            }
        }
    }

    static void out(String s) {
        System.out.println(s);
    }

}
