/*
 * Copyright (c) 2006, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6404388
 * @library /test/lib
 * @summary VISTA: Socket setTcpNoDelay & setKeepAlive working incorrectly
 * @run main TestTcpNoDelay
 * @run main/othervm -Djava.net.preferIPv4Stack=true TestTcpNoDelay
 */

import java.net.*;
import java.io.IOException;
import jdk.test.lib.net.IPSupport;

public class TestTcpNoDelay
{
    public static void main(String[] args) {
        IPSupport.throwSkippedExceptionIfNonOperational();

        try {
            Socket socket = new Socket();
            boolean on = socket.getTcpNoDelay();
            System.out.println("Get TCP_NODELAY = " + on);

            boolean opposite = on ? false: true;
            System.out.println("Set TCP_NODELAY to " + opposite);
            socket.setTcpNoDelay(opposite);

            boolean noDelay = socket.getTcpNoDelay();
            System.out.println("Get TCP_NODELAY = " + noDelay);

            if (noDelay != opposite)
                throw new RuntimeException("setTcpNoDelay no working as expected");

        } catch (IOException e){
            e.printStackTrace();
        }
    }

}
