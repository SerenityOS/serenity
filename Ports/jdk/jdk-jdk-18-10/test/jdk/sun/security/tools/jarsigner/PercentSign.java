/*
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6522933
 * @summary jarsigner fails in a directory with a path containing a % sign
 * @author Wang Weijun
 * @library /test/lib
 */

import jdk.test.lib.SecurityTools;

import java.nio.file.Files;
import java.nio.file.Path;

public class PercentSign {
    public static void main(String[] args) throws Exception {

        //  copy jar file into writeable location
        Files.copy(Path.of(System.getProperty("test.src"), "AlgOptions.jar"),
                Path.of("AlgOptionsTmp.jar"));

        SecurityTools.jarsigner("-keystore "
                + Path.of(System.getProperty("test.src"), "a%b", "percent.keystore")
                + " -storepass changeit AlgOptionsTmp.jar ok")
                .shouldHaveExitValue(0);
    }
}
