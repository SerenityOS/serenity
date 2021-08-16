/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test UnixDomainSocketAddress constructor
 * @library /test/lib
 * @run testng/othervm LengthTest
 */

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static java.lang.System.out;
import static java.net.StandardProtocolFamily.UNIX;
import static jdk.test.lib.Asserts.assertTrue;

import java.net.UnixDomainSocketAddress;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;

public class LengthTest {
    final int namelen = 100;    // length close to max

    @DataProvider(name = "strings")
    public Object[][] strings() {
        if (namelen == -1)
            return new Object[][] {new String[]{""}};

        return new Object[][]{
                {""},
                {new String(new char[100]).replaceAll("\0", "x")},
                {new String(new char[namelen]).replaceAll("\0", "x")},
                {new String(new char[namelen-1]).replaceAll("\0", "x")},
        };
    }

    @Test(dataProvider = "strings")
    public void expectPass(String s) {
        var addr = UnixDomainSocketAddress.of(s);
        assertTrue(addr.getPath().toString().equals(s), "getPathName.equals(s)");
        var p = Path.of(s);
        addr = UnixDomainSocketAddress.of(p);
        assertTrue(addr.getPath().equals(p), "getPath.equals(p)");
    }

    @Test
    public void expectNPE() {
        try {
            String s = null;
            UnixDomainSocketAddress.of(s);
            throw new RuntimeException("Expected NPE");
        } catch (NullPointerException npe) {
            out.println("\tCaught expected exception: " + npe);
        }
        try {
            Path p = null;
            UnixDomainSocketAddress.of(p);
            throw new RuntimeException("Expected NPE");
        } catch (NullPointerException npe) {
            out.println("\tCaught expected exception: " + npe);
        }
    }
}
