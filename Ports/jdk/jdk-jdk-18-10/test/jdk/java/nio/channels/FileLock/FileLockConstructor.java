/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.StandardOpenOption;

/*
 * @test
 * @bug 6880737
 * @summary Test FileLock constructor parameter validation.
 */
public class FileLockConstructor {
    public static void main(String[] args) throws IOException {
        FileLock fileLock = null;
        int failures = 0;

        // null FileChannel
        boolean exceptionThrown = false;
        try {
            fileLock = new FileLockSub((FileChannel)null, 0, 0, false);
        } catch (NullPointerException npe) {
            exceptionThrown = true;
        }
        if (!exceptionThrown) {
            System.err.println("FileLock constructor did not throw NPE for null FileChannel");
            failures++;
        }

        // null AsynchronousFileChannel
        exceptionThrown = false;
        try {
            fileLock = new FileLockSub((AsynchronousFileChannel)null, 0, 0, true);
        } catch (NullPointerException npe) {
            exceptionThrown = true;
        }
        if (!exceptionThrown) {
            System.err.println("FileLock constructor did not throw NPE for null AsynchronousFileChannel");
            failures++;
        }

        // create temporary file
        File tmpFile = File.createTempFile("FileLock", "tmp");
        tmpFile.deleteOnExit();

        // position and size preconditions
        long[][] posAndSize = new long[][] {
            {0, 42},            // valid
            {-1, 42},           // invalid: position < 0
            {0, -1},            // invalid: size < 0
            {Long.MAX_VALUE, 1} // invalid: position + size < 0
        };

        // test position and size preconditions for FileChannel case
        try (FileChannel syncChannel = FileChannel.open(tmpFile.toPath(),
                StandardOpenOption.READ, StandardOpenOption.WRITE)) {

            for (int i = 0; i < posAndSize.length; i++) {
                boolean preconditionsHold = i == 0;
                exceptionThrown = false;
                try {
                    fileLock = new FileLockSub(syncChannel, posAndSize[i][0],
                            posAndSize[i][1], true);
                } catch (IllegalArgumentException iae) {
                    exceptionThrown = true;
                } catch (Exception e) {
                    System.err.println("Unexpected exception \"" + e + "\" caught"
                            + " for position " + posAndSize[i][0] + " and size "
                            + posAndSize[i][1] + " for FileChannel variant");
                    failures++;
                    continue;
                }
                if (preconditionsHold && exceptionThrown) {
                    System.err.println("FileLock constructor incorrectly threw IAE"
                            + " for position " + posAndSize[i][0] + " and size "
                            + posAndSize[i][1] + " for FileChannel variant");
                    failures++;
                } else if (!preconditionsHold && !exceptionThrown) {
                    System.err.println("FileLock constructor did not throw IAE"
                            + " for position " + posAndSize[i][0] + " and size "
                            + posAndSize[i][1] + " for FileChannel variant");
                    failures++;
                }
            }
        }

        // test position and size preconditions for AsynchronousFileChannel case
        try (AsynchronousFileChannel asyncChannel
                = AsynchronousFileChannel.open(tmpFile.toPath(),
                        StandardOpenOption.READ, StandardOpenOption.WRITE)) {
            for (int i = 0; i < posAndSize.length; i++) {
                boolean preconditionsHold = i == 0;
                exceptionThrown = false;
                try {
                    fileLock = new FileLockSub(asyncChannel, posAndSize[i][0],
                            posAndSize[i][1], true);
                } catch (IllegalArgumentException iae) {
                    exceptionThrown = true;
                } catch (Exception e) {
                    System.err.println("Unexpected exception \"" + e + "\" caught"
                            + " for position " + posAndSize[i][0] + " and size "
                            + posAndSize[i][1] + " for AsynchronousFileChannel variant");
                    failures++;
                    continue;
                }
                if (preconditionsHold && exceptionThrown) {
                    System.err.println("FileLock constructor incorrectly threw IAE"
                            + " for position " + posAndSize[i][0] + " and size "
                            + posAndSize[i][1] + " for AsynchronousFileChannel variant");
                    failures++;
                } else if (!preconditionsHold && !exceptionThrown) {
                    System.err.println("FileLock constructor did not throw IAE"
                            + " for position " + posAndSize[i][0] + " and size "
                            + posAndSize[i][1] + " for AsynchronousFileChannel variant");
                    failures++;
                }
            }
        }

        if (failures > 0) {
            throw new RuntimeException("Incurred " + failures +
                                       " failures while testing FileLock.");
        }
    }
}

class FileLockSub extends FileLock {
    FileLockSub(FileChannel channel, long position, long size, boolean shared) {
        super(channel, position, size, shared);
    }

    FileLockSub(AsynchronousFileChannel channel, long position, long size,
                boolean shared) {
        super(channel, position, size, shared);
    }

    @Override
    public boolean isValid() {
        return false;
    }

    @Override
    public void release() throws IOException {
        // do nothing
    }
}
