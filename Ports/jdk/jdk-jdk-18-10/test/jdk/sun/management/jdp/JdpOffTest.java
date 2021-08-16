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
 * A JVM with JDP off should not send multicast JDP packets at all.
 * com.sun.management.jmxremote.autodiscovery=false should be respected.
 *
 * @author Alex Schenkman
 */

/*
 * @test JdpOffTest.java
 * @summary Assert that no JDP packets are sent to the default address and port.
 *
 * @library /test/lib
 *
 * @build ClientConnection JdpTestUtil JdpTestCase JdpOffTestCase DynamicLauncher
 * @run main/othervm JdpOffTest
 */


public class JdpOffTest extends DynamicLauncher {

    final String testName = "JdpOffTestCase";

    public static void main(String[] args) throws Exception {
        DynamicLauncher launcher = new JdpOffTest();
        launcher.run();
    }

    /**
     * Send Jdp multicast packets to the specified IP and port, 224.0.1.2:1234
     */
    protected String[] options() {
        String[] options = {
                "-Dcom.sun.management.jmxremote.authenticate=false",
                "-Dcom.sun.management.jmxremote.ssl=false",
                "-Dcom.sun.management.jmxremote=true",
                "-Dcom.sun.management.jmxremote.port=" + String.valueOf(jmxPort),
                "-Dcom.sun.management.jmxremote.autodiscovery=false",
                "-Dcom.sun.management.jdp.pause=1",
                "-Dcom.sun.management.jdp.name=" + jdpName,
                "-Djava.util.logging.SimpleFormatter.format='%1$tF %1$tT %4$-7s %5$s %n'",
                testName
        };
        return options;
    }

}
