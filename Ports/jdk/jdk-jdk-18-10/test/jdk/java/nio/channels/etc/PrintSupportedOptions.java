/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @library /test/lib
 * @requires (os.family == "linux" | os.family == "mac" | os.family == "windows")
 * @bug 8209152
 * @run main PrintSupportedOptions
 * @run main/othervm -Djava.net.preferIPv4Stack=true PrintSupportedOptions
 */

import java.io.IOException;
import java.net.SocketOption;
import java.nio.channels.*;
import java.util.*;

import jdk.test.lib.net.IPSupport;

public class PrintSupportedOptions {

    @FunctionalInterface
    interface NetworkChannelSupplier<T extends NetworkChannel> {
        T get() throws IOException;
    }

    public static void main(String[] args) throws IOException {
        IPSupport.throwSkippedExceptionIfNonOperational();

        test(() -> SocketChannel.open());
        test(() -> ServerSocketChannel.open());
        test(() -> DatagramChannel.open());

        test(() -> AsynchronousSocketChannel.open());
        test(() -> AsynchronousServerSocketChannel.open());
    }

    static final Set<String> READ_ONLY_OPTS = Set.of("SO_INCOMING_NAPI_ID");

    @SuppressWarnings("unchecked")
    static <T extends NetworkChannel>
    void test(NetworkChannelSupplier<T> supplier) throws IOException {
        try (T ch = supplier.get()) {
            System.out.println(ch);
            for (SocketOption<?> opt : ch.supportedOptions()) {
                Object value = ch.getOption(opt);
                System.out.format(" %s -> %s%n", opt.name(), value);
                if (!READ_ONLY_OPTS.contains(opt.name())) {
                    if (value != null)
                        ch.setOption((SocketOption<Object>) opt, value);
                }
            }
        }
    }
}
