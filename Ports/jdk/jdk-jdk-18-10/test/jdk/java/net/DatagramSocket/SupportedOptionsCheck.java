/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8234148
 * @library /test/lib
 * @summary checks that the DatagramSocket supportedOptions set contains all
 *  MulticastSocket socket options
 * @run testng SupportedOptionsCheck
 */

import jdk.test.lib.Platform;
import org.testng.annotations.Test;

import java.net.DatagramSocket;
import java.net.StandardSocketOptions;
import java.util.Set;

import static org.testng.Assert.assertTrue;

public class SupportedOptionsCheck {

    @Test
    public void checkMulticastOptionsAreReturned() throws Exception {
        try (DatagramSocket ds = new DatagramSocket())
        {
            Set<?> options = ds.supportedOptions();
            Set<?> multicastOptions = Set.of(
                    StandardSocketOptions.IP_MULTICAST_IF,
                    StandardSocketOptions.IP_MULTICAST_TTL,
                    StandardSocketOptions.IP_MULTICAST_LOOP);

            if (!Platform.isWindows())
                assertTrue(options.containsAll(multicastOptions));
        }
    }
}
