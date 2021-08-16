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

/**
 * @test @summary Test that default methods in Checksum works as expected
 * @build ChecksumBase
 * @run main TestChecksum
 */
import java.util.zip.CRC32C;
import java.util.zip.Checksum;

public class TestChecksum {

    public static void main(String[] args) {
        ChecksumBase.testAll(new MyCRC32C(), 0xE3069283L);
    }

    /**
     * Only implementing required methods
     */
    private static class MyCRC32C implements Checksum {

        private final CRC32C crc32c = new CRC32C();

        @Override
        public void update(int b) {
            crc32c.update(b);
        }

        @Override
        public void update(byte[] b, int off, int len) {
            crc32c.update(b, off, len);
        }

        @Override
        public long getValue() {
            return crc32c.getValue();
        }

        @Override
        public void reset() {
            crc32c.reset();
        }

    }
}
