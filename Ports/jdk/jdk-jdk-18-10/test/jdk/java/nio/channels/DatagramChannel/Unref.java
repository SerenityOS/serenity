/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8212132
 * @summary Test that DatagramChannel does not leak file descriptors
 * @requires os.family != "windows"
 * @modules jdk.management
 * @library /test/lib
 * @run main/othervm Unref
 */

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import com.sun.management.UnixOperatingSystemMXBean;

import jtreg.SkippedException;
import jdk.test.lib.net.IPSupport;

public class Unref {

    interface DatagramChannelSupplier {
        DatagramChannel get() throws IOException;
    }

    public static void main(String[] args) throws Exception {
        if (unixOperatingSystemMXBean() == null)
            throw new SkippedException("This test requires UnixOperatingSystemMXBean");

        test(DatagramChannel::open);
        if (IPSupport.hasIPv4())
            test(() -> DatagramChannel.open(StandardProtocolFamily.INET));
        if (IPSupport.hasIPv6())
            test(() -> DatagramChannel.open(StandardProtocolFamily.INET6));
    }

    static void test(DatagramChannelSupplier supplier) throws Exception {
        openAndClose(supplier); // warm-up

        try (Selector sel = Selector.open()) {
            long count = fileDescriptorCount();

            // open+close
            openAndClose(supplier);
            assertEquals(fileDescriptorCount(), count);

            // open+unref, file descriptor should be closed by cleaner
            openAndUnref(supplier);
            assertEquals(waitForFileDescriptorCount(count), count);

            // open+register+close+flush
            openRegisterAndClose(supplier, sel);
            assertEquals(fileDescriptorCount(), count);

            // open+register+flush, file descriptor should be closed by cleaner
            openRegisterAndUnref(supplier, sel);
            assertEquals(waitForFileDescriptorCount(count), count);
        }
    }

    /**
     * Create a DatagramChannel and closes it.
     */
    static void openAndClose(DatagramChannelSupplier supplier) throws IOException {
        System.out.println("openAndClose ...");
        DatagramChannel dc = supplier.get();
        dc.close();
    }

    /**
     * Create a DatagramChannel and exits without closing the channel.
     */
    static void openAndUnref(DatagramChannelSupplier supplier) throws IOException {
        System.out.println("openAndUnref ...");
        DatagramChannel dc = supplier.get();
    }

    /**
     * Create a DatagramChannel, register it with a Selector, close the channel
     * while register, and then finally flush the channel from the Selector.
     */
    static void openRegisterAndClose(DatagramChannelSupplier supplier, Selector sel)
        throws IOException
    {
        System.out.println("openRegisterAndClose ...");
        try (DatagramChannel dc = supplier.get()) {
            dc.bind(new InetSocketAddress(0));
            dc.configureBlocking(false);
            dc.register(sel, SelectionKey.OP_READ);
            sel.selectNow();
        }

        // flush, should close channel
        sel.selectNow();
    }

    /**
     * Creates a DatagramChannel, registers with a Selector, cancels the key
     * and flushes the channel from the Selector. This method exits without
     * closing the channel.
     */
    static void openRegisterAndUnref(DatagramChannelSupplier supplier, Selector sel)
        throws IOException
    {
        System.out.println("openRegisterAndUnref ...");
        DatagramChannel dc = supplier.get();
        dc.bind(new InetSocketAddress(0));
        dc.configureBlocking(false);
        SelectionKey key = dc.register(sel, SelectionKey.OP_READ);
        sel.selectNow();
        key.cancel();
        sel.selectNow();
    }

    /**
     * If the file descriptor count is higher than the given count then invoke
     * System.gc() and wait for the file descriptor count to drop.
     */
    static long waitForFileDescriptorCount(long target) throws InterruptedException {
        long actual = fileDescriptorCount();
        if (actual > target) {
            System.gc();
            while ((actual = fileDescriptorCount()) > target) {
                Thread.sleep(10);
            }
        }
        return actual;
    }

    static UnixOperatingSystemMXBean unixOperatingSystemMXBean() {
        return ManagementFactory.getPlatformMXBean(UnixOperatingSystemMXBean.class);
    }

    static long fileDescriptorCount() {
        return unixOperatingSystemMXBean().getOpenFileDescriptorCount();
    }

    static void assertEquals(long actual, long expected) {
        if (actual != expected)
            throw new RuntimeException("actual=" + actual + ", expected=" + expected);
    }
}

