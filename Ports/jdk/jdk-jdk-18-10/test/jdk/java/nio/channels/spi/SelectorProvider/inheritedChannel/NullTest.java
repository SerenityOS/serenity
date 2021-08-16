/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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

/* @test 1.1 03/07/25
 * @summary Unit test for inetd feature
 * @bug 4673940
 */

import java.nio.channels.Channel;
import java.nio.channels.spi.SelectorProvider;
import java.io.IOException;

public class NullTest {

    public static void main(String args[]) {

        // test the assertion that SelectorProvider.inheritedChannel()
        // and System.inheritedChannel return null when standard input
        // is not connected to a socket

        Channel c1, c2;
        try {
            c1 = SelectorProvider.provider().inheritedChannel();
            c2 = System.inheritedChannel();
        } catch (IOException ioe) {
            throw new RuntimeException("Unexpected IOException: " + ioe);
        }
        if (c1 != null || c2 != null) {
            throw new RuntimeException("Channel returned - unexpected");
        }
    }

}
