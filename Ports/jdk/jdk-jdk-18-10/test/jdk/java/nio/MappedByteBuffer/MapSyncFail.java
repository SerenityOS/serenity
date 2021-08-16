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

/* @test
 * @summary Test failure paths for MAP_SYNC FileChannel.map of non-DAX files
 * @modules java.base/jdk.internal.misc
 * @run main MapSyncFail true
 * @run main MapSyncFail false
 */

import java.io.*;
import java.nio.*;
import java.util.*;
import java.nio.channels.*;
import jdk.nio.mapmode.*;
import jdk.internal.misc.Unsafe;

public class MapSyncFail {

    public static final int K = 1024;

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new Exception("Expected true or false as argument");
        }
        boolean is_rw = Boolean.valueOf(args[0]);
        FileChannel.MapMode mode = (is_rw ? ExtendedMapMode.READ_WRITE_SYNC : ExtendedMapMode.READ_ONLY_SYNC);
        // it is assumed that /tmp is not a DAX file system
        File file = File.createTempFile("MapSyncFail", null);
        file.deleteOnExit();
        long filesize = (8 * K);
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.setLength(filesize);
            FileChannel fc = raf.getChannel();
            MappedByteBuffer mbb = fc.map(mode, 0, filesize);
        } catch(IOException ioe) {
            // when writeback is enabled for the current os/cpu
            // combination the underlying mmap should be attempted and
            // the map call should fail with IOException
            if (!Unsafe.isWritebackEnabled()) {
                throw new Exception("IOException not expected");
            }
            System.out.println("caught " + ioe);
            ioe.printStackTrace();
            return;
        } catch (UnsupportedOperationException uoe) {
            // when writeback is not enabled for the current os/cpu
            // combination the mmap should not be attempted and the
            // map call should fail with UnsupportedOperationException

            if (Unsafe.isWritebackEnabled()) {
                throw new Exception("UnsupportedOperationException not expected");
            }
            System.out.println("caught " + uoe);
            uoe.printStackTrace();
            return;
        }

        throw new Exception("expected IOException or UnsupportedOperationException");
    }
}
