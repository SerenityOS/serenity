/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *   Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 *
 */

/*
 * @test
 * @requires !(((os.arch == "amd64" | os.arch == "x86_64") & sun.arch.data.model == "64") | os.arch == "aarch64")
 * @modules jdk.incubator.foreign/jdk.internal.foreign
 * @run testng/othervm --enable-native-access=ALL-UNNAMED TestUnsupportedPlatform
 */

import jdk.incubator.foreign.CLinker;
import org.testng.annotations.Test;

import static org.testng.Assert.assertNull;

// tests run on 32-bit platforms, which are currently not supported
public class TestUnsupportedPlatform {

    @Test(expectedExceptions = ExceptionInInitializerError.class)
    public void testNoInitialization() {
        CLinker.getInstance(); // trigger initialization
    }

}
