/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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

import javax.sound.sampled.AudioPermission;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * @test
 * @bug 4932835
 * @summary REGRESSION: IAE for supported line in AudioSystem.getLine()
 */
public class GetLine {

    static boolean isSoundAccessDenied = false;
    static {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            try {
                securityManager.checkPermission(new AudioPermission("*"));
            } catch (SecurityException e) {
                isSoundAccessDenied = true;
            }
        }
    }

    static final int STATUS_PASSED = 0;
    static final int STATUS_FAILED = 2;
    static final int STATUS_TEMP = 95;
    static java.io.PrintStream log = System.err;

    public static void main(String argv[]) throws Exception {
        if (run(argv, System.out) == STATUS_FAILED) {
            throw new Exception("Test FAILED");
        }
        System.out.println("Test passed.");
    }

    public static int run(String argv[], java.io.PrintStream out) {
        String testCaseID = "LineListener2001";

        log.println("===== " + testCaseID + " =====");

        boolean failed = false;
        Line l = null;



        // get the default SourceDataLine

        DataLine.Info s_info = new DataLine.Info(SourceDataLine.class, null);
        Line.Info infos[] = AudioSystem.getSourceLineInfo( s_info );

        if( infos.length < 1 ) {
            log.println("Line.Info array == 0");
            return STATUS_PASSED;
        }
        try {
            l = AudioSystem.getLine(infos[0]);
        } catch(SecurityException lue) {
            log.println("SecurityException");
            return STATUS_PASSED;
        } catch (LineUnavailableException e1) {
            log.println("LUE");
            return STATUS_PASSED;
        } catch (IllegalArgumentException iae) {
            log.println("IllegalArgumentException should not be thrown "
                     + "for supported line");
            iae.printStackTrace(log);
            return STATUS_FAILED;
        }
        out.println("Passed.");
        return STATUS_PASSED;
    }

}
