/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4173717 8243488
 * @summary Check that setReceiveBufferSize and getReceiveBufferSize work as expected
 * @run testng SetGetReceiveBufferSize
 * @run testng/othervm -Djava.net.preferIPv4Stack=true SetGetReceiveBufferSize
 */

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.nio.channels.DatagramChannel;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.expectThrows;

public class SetGetReceiveBufferSize {
    static final Class<SocketException> SE = SocketException.class;
    static final Class<IllegalArgumentException> IAE = IllegalArgumentException.class;

    @FunctionalInterface
    public interface DatagramSocketSupplier {
        DatagramSocket open() throws IOException;
    }
    static DatagramSocketSupplier supplier(DatagramSocketSupplier supplier) { return supplier; }

    @DataProvider
    public Object[][] invariants() {
        return new Object[][]{
                {"DatagramSocket", supplier(() -> new DatagramSocket())},
                {"MulticastSocket", supplier(() -> new MulticastSocket())},
                {"DatagramSocketAdaptor", supplier(() -> DatagramChannel.open().socket())},
        };
    }

    @Test(dataProvider = "invariants")
    public void testSetInvalidBufferSize(String name, DatagramSocketSupplier supplier) throws IOException {
        var invalidArgs = new int[]{ -1, 0 };

        try (var socket = supplier.open()) {
            for (int i : invalidArgs) {
                Exception ex = expectThrows(IAE, () -> socket.setReceiveBufferSize(i));
                System.out.println(name + " got expected exception: " + ex);
            }
        }
    }

    @Test(dataProvider = "invariants")
    public void testSetAndGetBufferSize(String name, DatagramSocketSupplier supplier) throws IOException {
        var validArgs = new int[]{ 1234, 2345, 3456 };

        try (var socket = supplier.open()) {
            for (int i : validArgs) {
                socket.setReceiveBufferSize(i);
                assertEquals(socket.getReceiveBufferSize(), i, name);
            }
        }
    }

    @Test(dataProvider = "invariants")
    public void testSetGetAfterClose(String name, DatagramSocketSupplier supplier) throws IOException {
        var socket = supplier.open();
        socket.close();

        Exception setException = expectThrows(SE, () -> socket.setReceiveBufferSize(2345));
        System.out.println(name + " got expected exception: " + setException);

        Exception getException = expectThrows(SE, () -> socket.getReceiveBufferSize());
        System.out.println(name + " got expected exception: " + getException);
    }
}
