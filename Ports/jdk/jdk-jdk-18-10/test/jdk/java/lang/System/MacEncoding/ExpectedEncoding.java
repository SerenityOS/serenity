/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * Check that the value of file.encoding and sun.jnu.encoding match the expected
 * values passed in on the command-line.
 */
public class ExpectedEncoding {
    public static void main(String[] args) {
        boolean failed = false;
        if (args.length != 2) {
            System.out.println("Usage:");
            System.out.println("$ java ExpectedEncoding <expected file.encoding> <expected sun.jnu.encoding>");
            System.out.println("$   use \"skip\" to skip checking property's value");
            System.exit(1);
        }
        String expectFileEnc = args[0];
        String expectSunJnuEnc = args[1];

        String fileEnc = System.getProperty("file.encoding");
        String jnuEnc = System.getProperty("sun.jnu.encoding");

        if ("skip".equals(expectFileEnc)) {
            System.err.println("Expected file.encoding is \"skip\", ignoring");
        } else {
            if (fileEnc == null || !fileEnc.equals(expectFileEnc)) {
                System.err.println("Expected file.encoding: " + expectFileEnc);
                System.err.println("Actual file.encoding: " + fileEnc);
                failed = true;
            }
        }
        if ("skip".equals(expectSunJnuEnc)) {
            System.err.println("Expected sun.jnu.encoding is \"skip\", ignoring");
        } else {
            if (jnuEnc == null || !jnuEnc.equals(expectSunJnuEnc)) {
                System.err.println("Expected sun.jnu.encoding: " + expectSunJnuEnc);
                System.err.println("Actual sun.jnu.encoding: " + jnuEnc);
                failed = true;
            }
        }

        if (failed) {
            throw new RuntimeException("Test Failed");
        }
    }
}
