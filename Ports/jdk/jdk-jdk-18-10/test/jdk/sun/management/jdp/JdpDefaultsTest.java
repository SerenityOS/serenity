/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * A JVM with JDP on should send multicast JDP packets regularly.
 *
 * @author Alex Schenkman
 */

/*
 * @test JdpDefaultsTest
 * @summary Assert that we can read JDP packets from a multicast socket connection, on default IP and port.
 *
 * @library /test/lib
 *
 * @build ClientConnection JdpTestUtil JdpTestCase JdpOnTestCase DynamicLauncher
 * @run main/othervm JdpDefaultsTest
 */

public class JdpDefaultsTest extends DynamicLauncher {

    final String testName = "JdpOnTestCase";

    public static void main(String[] args) throws Exception {
        DynamicLauncher launcher = new JdpDefaultsTest();
        launcher.run();
    }

    /**
     * Send Jdp multicast packets to the default IP and port, 224.0.23.178:7095
     */
    protected String[] options() {
        String[] options = {
                "-Dcom.sun.management.jmxremote.authenticate=false",
                "-Dcom.sun.management.jmxremote.ssl=false",
                "-Dcom.sun.management.jmxremote=true",
                "-Dcom.sun.management.jmxremote.port=" + String.valueOf(jmxPort),
                "-Dcom.sun.management.jmxremote.autodiscovery=true",
                "-Dcom.sun.management.jdp.pause=1",
                "-Dcom.sun.management.jdp.name=" + jdpName,
                "-Djava.util.logging.SimpleFormatter.format='%1$tF %1$tT %4$-7s %5$s %n'",
                testName
        };
        return options;
    }
}
