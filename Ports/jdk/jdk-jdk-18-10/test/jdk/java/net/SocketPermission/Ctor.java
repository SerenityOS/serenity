/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4391898 8230407
 * @summary SocketPermission(":",...) throws ArrayIndexOutOfBoundsException
 *          SocketPermission constructor argument checks
 * @run testng Ctor
 */

import java.net.SocketPermission;
import org.testng.annotations.Test;
import static java.lang.System.out;
import static org.testng.Assert.*;

public class Ctor {

    static final Class<NullPointerException> NPE = NullPointerException.class;
    static final Class<IllegalArgumentException> IAE = IllegalArgumentException.class;

    @Test
    public void positive() {
        // ArrayIndexOutOfBoundsException is the bug, 4391898, exists
        SocketPermission sp1 =  new SocketPermission(":", "connect");
    }

    @Test
    public void npe() {
        NullPointerException e;
        e = expectThrows(NPE, () -> new SocketPermission(null, null));
        out.println("caught expected NPE: " + e);
        e = expectThrows(NPE, () -> new SocketPermission("foo", null));
        out.println("caught expected NPE: " + e);
        e = expectThrows(NPE, () -> new SocketPermission(null, "connect"));
        out.println("caught expected NPE: " + e);
    }

    @Test
    public void iae() {
        IllegalArgumentException e;
        // host
        e = expectThrows(IAE, () -> new SocketPermission("1:2:3:4", "connect"));
        out.println("caught expected IAE: " + e);
        e = expectThrows(IAE, () -> new SocketPermission("foo:5-4", "connect"));
        out.println("caught expected IAE: " + e);

        // actions
        e = expectThrows(IAE, () -> new SocketPermission("foo", ""));
        out.println("caught expected IAE: " + e);
        e = expectThrows(IAE, () -> new SocketPermission("foo", "badAction"));
        out.println("caught expected IAE: " + e);
        e = expectThrows(IAE, () -> new SocketPermission("foo", "badAction,connect"));
        out.println("caught expected IAE: " + e);
        e = expectThrows(IAE, () -> new SocketPermission("foo", "badAction,,connect"));
        out.println("caught expected IAE: " + e);
        e = expectThrows(IAE, () -> new SocketPermission("foo", ",connect"));
        out.println("caught expected IAE: " + e);
        e = expectThrows(IAE, () -> new SocketPermission("foo", ",,connect"));
        out.println("caught expected IAE: " + e);
        e = expectThrows(IAE, () -> new SocketPermission("foo", "connect,"));
        out.println("caught expected IAE: " + e);
        e = expectThrows(IAE, () -> new SocketPermission("foo", "connect,,"));
        out.println("caught expected IAE: " + e);
    }
}
