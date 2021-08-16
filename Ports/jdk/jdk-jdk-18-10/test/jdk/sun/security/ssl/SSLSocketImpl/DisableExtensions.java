/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8217633
 * @library /javax/net/ssl/templates
 * @summary Configurable extensions with system properties
 * @run main/othervm DisableExtensions supported_versions TLSv1.3 fail
 * @run main/othervm DisableExtensions supported_versions TLSv1.2
 */

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLException;

public class DisableExtensions extends SSLSocketTemplate {

    private final String[] protocols;

    public DisableExtensions(String[] protocols) {
        this.protocols = protocols;
    }

    @Override
    protected void configureClientSocket(SSLSocket socket) {
        socket.setEnabledProtocols(protocols);
    }

    // Run the test case.
    //
    // Check that the extension could be disabled, and the impact may be
    // different for different protocols.
    public static void main(String[] args) throws Exception {
        System.setProperty("jdk.tls.client.disableExtensions", args[0]);

        boolean shouldSuccess = (args.length != 3);

        try {
            (new DisableExtensions(new String[] {args[1]})).run();
        } catch (SSLException | IllegalStateException ssle) {
            if (shouldSuccess) {
                throw new RuntimeException(
                        "The extension " + args[0] + " is disabled");
            }

            return;
        }

        if (!shouldSuccess) {
            throw new RuntimeException(
                    "The extension " + args[0] +
                    " should be disabled and the connection should fail");
        }
    }
}
