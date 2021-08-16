/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8260403
 * @summary javap should be more robust in the face of invalid class files
 * @build InvalidSignature
 * @run main T8260403
 * @modules jdk.jdeps/com.sun.tools.javap
 */
import java.io.PrintWriter;

public class T8260403 {

    public static void main(String args[]) throws Exception {
        if (com.sun.tools.javap.Main.run(new String[]{"-c", System.getProperty("test.classes") + "/InvalidSignature.class"},
                new PrintWriter(System.out)) != 0) {
            throw new AssertionError();
        }
    }
}
