/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.Assert;
import org.testng.annotations.Test;
import java.io.IOException;
import java.net.ProtocolFamily;
import java.net.http.HttpClient;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Pipe;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.Channel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;

/*
 * @test
 * @bug 8248006
 * @summary The test checks if UncheckedIOException is thrown
 * @build  HttpClientExceptionTest
 * @run testng/othervm -Djava.nio.channels.spi.SelectorProvider=HttpClientExceptionTest$CustomSelectorProvider
 *                      HttpClientExceptionTest
 */

public class HttpClientExceptionTest {

    static final int ITERATIONS = 10;

    @Test
    public void testHttpClientException() {
        for(int i = 0; i < ITERATIONS; i++) {
            Assert.assertThrows(HttpClient.newBuilder()::build);
            Assert.assertThrows(HttpClient::newHttpClient);
        }
    }

    public static class CustomSelectorProvider extends SelectorProvider {

        @Override
        public DatagramChannel openDatagramChannel() throws IOException {
            throw new IOException();
        }

        @Override
        public DatagramChannel openDatagramChannel(ProtocolFamily family) throws IOException {
            throw new IOException();
        }

        @Override
        public Pipe openPipe() throws IOException {
            throw new IOException();
        }

        @Override
        public ServerSocketChannel openServerSocketChannel() throws IOException {
            throw new IOException();
        }

        @Override
        public SocketChannel openSocketChannel() throws IOException {
            throw new IOException();
        }

        @Override
        public Channel inheritedChannel() throws IOException {
            return super.inheritedChannel();
        }

        @Override
        public SocketChannel openSocketChannel(ProtocolFamily family) throws IOException {
            return super.openSocketChannel(family);
        }

        @Override
        public ServerSocketChannel openServerSocketChannel(ProtocolFamily family) throws IOException {
            return super.openServerSocketChannel(family);
        }

        @Override
        public AbstractSelector openSelector() throws IOException {
            throw new IOException();
        }
    }
}
