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

/*
 * This test is manually run because it requires an NVRAM device to be
 * mapped as DAX file system or, at least, to be simulated by a
 * volatile RAM mapped file system. Also, on AArch64 it requires an
 * ARMV8.2 CPU which implements the dc CVAP instruction (CPU feature
 * dcpop) and an OS that makes it available from user space.
 *
 * If the test runs on such a host without throwing an exception then
 * that confirms that NVRAM-backed byte buffers can be allocated,
 * updated and forced via cache line writeback.
 */

/*
 * How to run this test:
 *
 * Ideally this test should be run on a x86_64/amd64 or aarch64 host
 * fitted with an NVRAM memory device. The NVRAM should appear as
 * /dev/pmem0 or some equivalent DAX file device. The file device
 * should be mounted at /mnt/pmem with a directory tmp created
 * directly under that mount point with a+rwx access.
 *
 * It is possible to run the test on x86_64 using a volatile RAM
 * backed device to simulate NVRAM, even though this does not provide
 * any guarantee of persistence of data across program runs. For the
 * latter case the following instructions explain how to set up the
 * simulated NVRAM device.
 *
 * https://developers.redhat.com/blog/2016/12/05/configuring-and-using-persistent-memory-rhel-7-3/
 * https://nvdimm.wiki.kernel.org/
 * TL;DR: add "memmap=1G!4G" to /etc/default/grub, eg. GRUB_CMDLINE_LINUX="memmap=1G!4G"
 *        then ("sudo" may required)
 *          for RHEL(BIOS-based): grub2-mkconfig -o /boot/grub2/grub.cfg
 *          for RHEL(UEFI-based): grub2-mkconfig -o /boot/efi/EFI/redhat/grub.cfg
 *          for Ubuntu: update-grub2
 *        finally reboot
 *        after the host been rebooted, a new /dev/pmem{N} device should exist,
 *        naming conversion starts at /dev/pmem0
 *
 *  Prepare test directory follow below commands, "sudo" may required
 *  (if ndctl or mkfs.xfs not exist, install ndctl or xfsprogs package first)
 *  (for RHEL8, when call mkfs.xfs, specify the -m reflink=0 option to disable reflink feature)
 *
 *  ndctl create-namespace -f -e namespace0.0 -m memory -M mem
 *  mkdir /mnt/pmem
 *  mkfs.xfs -f /dev/pmem0; mount -o dax /dev/pmem0 /mnt/pmem/
 *  mkdir /mnt/pmem/test; chmod a+rwx /mnt/pmem/test
 *
 * Now run the test program
 *
 *  java PmemTest
 *
 * or
 *
 *  make test TEST=jdk/java/nio/MappedByteBuffer/PmemTest.java
*/

/* @test
 * @summary Testing NVRAM mapped byte buffer support
 * @run main/manual PmemTest
 * @requires (os.family == "linux")
 * @requires ((os.arch == "x86_64")|(os.arch == "amd64")|(os.arch == "aarch64")|(os.arch == "ppc64le"))
 */

import java.io.File;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.List;
import jdk.nio.mapmode.ExtendedMapMode;

import java.lang.management.ManagementFactory;
import java.lang.management.BufferPoolMXBean;

public class PmemTest {

    public static final int K = 1024;
    public static final int NUM_KBS = 16;

    public static void main(String[] args) throws Exception {

        System.out.println("test");

        String dir = "/tmp"; // mapSync should fail
        dir = "/mnt/pmem/test"; // mapSync should work, since fs mount is -o dax

        Path path = new File(dir, "pmemtest").toPath();

        FileChannel fileChannel = (FileChannel) Files
                .newByteChannel(path, EnumSet.of(
                        StandardOpenOption.READ,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.CREATE));

        MappedByteBuffer mappedByteBuffer = fileChannel.map(ExtendedMapMode.READ_WRITE_SYNC, 0, NUM_KBS * K);


        dumpBufferPoolBeans();

        // for (int loops = 0; loops < 1000; loops++) {
        for (int loops = 0; loops < 100; loops++) {
            int base = K * (loops % NUM_KBS);
            for (int i = 0; i < K ; i++) {
                for (int j = 0; j < K ;j++) {
                    testBuffer(mappedByteBuffer, base, (i << 3) + j);
                    commitBuffer(mappedByteBuffer, base);
                }
            }
        }
        dumpBufferPoolBeans();
    }

    public static void testBuffer(MappedByteBuffer mappedByteBuffer, int base, int start) {
        for (int k = 0; k < 8; k++) {
            int idx = (start + k) % K;
            byte z = mappedByteBuffer.get(base + idx);
            z++;
            mappedByteBuffer.put(base + idx, z);
        }
    }

    public static void commitBuffer(MappedByteBuffer mappedByteBuffer, int base)
    {
        mappedByteBuffer.force(base, K);
    }

    public static void dumpBufferPoolBeans()
    {
        List<BufferPoolMXBean> beansList = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class);
        for (BufferPoolMXBean bean : beansList) {
            System.out.println("BufferPoolMXBean {" +
                               "\n\tname:          " + bean.getName() +
                               "\n\tcount:         " + bean.getCount() +
                               "\n\ttotalCapacity: " + bean.getTotalCapacity() +
                               "\n\tmemoryUsed:    " + bean.getMemoryUsed() +
                               "\n}");
        }
    }
}
