/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6286011 6330315
 * @summary Verify that appropriate SelectorProvider is selected.
 */

import java.nio.channels.spi.*;

public class SelProvider {
    public static void main(String[] args) throws Exception {
        String expected = System.getProperty("java.nio.channels.spi.SelectorProvider");
        if (expected == null) {
            String osname = System.getProperty("os.name");
            String osver = System.getProperty("os.version");
            if ("Linux".equals(osname)) {
                expected = "sun.nio.ch.EPollSelectorProvider";
            } else if (osname.contains("OS X")) {
                expected = "sun.nio.ch.KQueueSelectorProvider";
            } else {
                return;
            }
        }
        String cn = SelectorProvider.provider().getClass().getName();
        System.out.println(cn);
        if (!cn.equals(expected))
            throw new Exception("failed");
    }
}
