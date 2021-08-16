/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8024832
 */

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;
import java.nio.channels.ServerSocketChannel;

public class ServerSocketAdaptorTest {

    public static void main(String[] args) throws IOException {

        String message = null;

        try (ServerSocket s = new ServerSocket()) {
            s.accept();
            throw new AssertionError();
        } catch (IOException e) {
            message = e.getMessage();
        }

        try (ServerSocket ss = ServerSocketChannel.open().socket()) {

            assert !ss.isBound() : "the assumption !ss.isBound() doesn't hold";

            try {
                ss.accept();
                throw new AssertionError();
            } catch (Exception e) {
                if (e instanceof SocketException && message.equals(e.getMessage())) {
                    return;
                } else {
                    throw new AssertionError(
                            "Expected to throw SocketException with a particular message", e);
                }
            }
        }
    }
}
