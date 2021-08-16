/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8167965 8194308
 * @summary Test proper handling of the --release option.
 * @modules
 *      jdk.compiler/com.sun.tools.javac.jvm
 *      jdk.compiler/com.sun.tools.javac.platform
 *      jdk.jdeps/com.sun.tools.jdeprscan
 * @build jdk.jdeprscan.TestRelease
 * @run testng jdk.jdeprscan.TestRelease
 */

package jdk.jdeprscan;

import com.sun.tools.javac.platform.JDKPlatformProvider;
import com.sun.tools.jdeprscan.Main;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class TestRelease {
    static boolean invoke(String arg) {
        System.err.println(">>> invoking Main.call with arguments: --list --release " + arg);
        boolean r = Main.call(System.out, System.err, "--list", "--release", arg);
        System.err.println(">>> Main.call returned " + r);
        return r;
    }

    @Test
    public void testSuccess() {
        for (String target : new JDKPlatformProvider().getSupportedPlatformNames()) {
            assertTrue(invoke(target));
        }
    }

    @Test
    public void testFailure() {
        assertFalse(invoke("5"));
    }
}
