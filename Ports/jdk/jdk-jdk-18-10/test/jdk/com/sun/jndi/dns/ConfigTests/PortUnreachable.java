/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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

import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.directory.InitialDirContext;

/*
 * @test
 * @bug 8200151
 * @summary Tests that when a DNS server is unreachable and an ICMP Destination
 *          Unreachable packet is received, we fail quickly and don't wait for
 *          the full timeout interval. This could be caused, for example, by a
 *          dead DNS server or a flakey router.
 *          On AIX, no ICMP Destination Unreachable is received, so skip test.
 * @requires os.family != "aix"
 * @library ../lib/
 * @modules java.base/sun.security.util
 * @run main/othervm -Djdk.net.usePlainDatagramSocketImpl=false PortUnreachable
 */

public class PortUnreachable extends DNSTestBase {

    // Port 25 is the SMTP port, used here to simulate a dead DNS server.
    private static final int PORT = 25;

    // Threshold in ms for elapsed time of request failed. Normally, it should
    // be very quick, but consider to different platform and test machine
    // performance, here we define 3000 ms as threshold which acceptable for
    // this test.
    private static final int THRESHOLD = 3000;

    private long startTime;

    public PortUnreachable() {
        setLocalServer(false);
    }

    public static void main(String[] args) throws Exception {
        new PortUnreachable().run(args);
    }

    /*
     * Tests that when a DNS server is unreachable and an ICMP Destination
     * Unreachable packet is received, we fail quickly and don't wait for
     * the full timeout interval.
     */
    @Override
    public void runTest() throws Exception {
        String deadServerUrl = "dns://localhost:" + PORT;
        env().put(Context.PROVIDER_URL, deadServerUrl);
        setContext(new InitialDirContext(env()));

        // Any request should fail quickly.
        startTime = System.currentTimeMillis();
        context().getAttributes("");

        // You're running a DNS server on your SMTP port?
        throw new RuntimeException(
                "Failed: getAttributes succeeded unexpectedly");
    }

    @Override
    public boolean handleException(Exception e) {
        if (e instanceof CommunicationException) {
            long elapsedTime = System.currentTimeMillis() - startTime;

            Throwable cause = ((CommunicationException) e).getRootCause();
            if (!(cause instanceof java.net.PortUnreachableException)) {
                DNSTestUtils.debug("Bug 7164518 can cause this failure on mac");
                return false;
            }

            DNSTestUtils.debug("Elapsed (ms):  " + elapsedTime);

            // Check that elapsed time is less than defined threshold.
            if (elapsedTime < THRESHOLD) {
                return true;
            }

            throw new RuntimeException("Failed: call took " + elapsedTime
                    + " ms, expected less than " + THRESHOLD + " ms");
        }

        return super.handleException(e);
    }
}
