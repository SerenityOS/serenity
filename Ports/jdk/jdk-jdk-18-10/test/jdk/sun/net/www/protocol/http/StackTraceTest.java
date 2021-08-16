/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4773417 5003746
 * @library /test/lib
 * @build jdk.test.lib.Utils
 * @run main StackTraceTest
 * @summary  HttpURLConnection.getInputStream() produces IOException with
 *           bad stack trace; HttpURLConnection.getInputStream loses
 *           exception message, exception class
 */
import java.net.*;
import java.io.IOException;
import jdk.test.lib.Utils;

public class StackTraceTest {
    public static void main(String[] args) throws Exception {
        InetSocketAddress refusing = Utils.refusingEndpoint();
        int port = refusing.getPort();
        String host = refusing.getAddress().getHostAddress();
        if (host.contains(":"))
            host = "[" + host + "]";
        URL url = URI.create("http://" + host + ":" + port + "/").toURL();
        System.out.println("URL: " + url);

        URLConnection uc = url.openConnection();

        // Trigger implicit connection by trying to retrieve bogus
        // response header, and force remembered exception
        uc.getHeaderFieldKey(20);

        try {
            uc.getInputStream();  // expect to throw
            throw new RuntimeException("Expected getInputStream to throw");
        } catch (IOException ioe) {
            if (!(ioe instanceof ConnectException))
                throw new RuntimeException("Expect ConnectException, got " + ioe);
            if (ioe.getMessage() == null)
                throw new RuntimeException("Exception message is null");
            if (ioe.getCause() == null)
                throw new RuntimeException("Excepting a chained exception, but got: ",
                                           ioe);
        }
    }
}
