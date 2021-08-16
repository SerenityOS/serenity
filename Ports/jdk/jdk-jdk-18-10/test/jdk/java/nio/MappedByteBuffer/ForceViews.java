/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4833719
 * @summary Verify MappedByteBuffer force on compact, duplicate, and slice views
 * @run testng ForceViews
 */
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.channels.FileChannel;
import static java.nio.channels.FileChannel.MapMode.*;
import java.nio.file.Path;
import static java.nio.file.StandardOpenOption.*;
import java.util.function.BiFunction;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ForceViews {

    static record Segment(int position, int length) {}

    private FileChannel fc;

    @BeforeTest(alwaysRun=true)
    public void openChannel() throws IOException {
        Path file = Path.of(System.getProperty("test.src", "."), "junk");
        fc = FileChannel.open(file, CREATE_NEW, READ, WRITE, DELETE_ON_CLOSE);
        ByteBuffer buf = ByteBuffer.wrap(new byte[1024]);
        fc.write(buf);
        fc.position(0);
    }

    @AfterTest(alwaysRun=true)
    public void closeChannel() throws IOException {
        fc.close();
    }

    @DataProvider
    public Object[][] provider() throws IOException {
        BiFunction<MappedByteBuffer,Segment,MappedByteBuffer> absSlice =
            (m, s) -> { return m.slice(s.position, s.length); };
        BiFunction<MappedByteBuffer,Segment,MappedByteBuffer> relSlice =
            (m, s) -> { m.position(s.position); m.limit(s.position + s.length);
                        return m.slice(); };
        BiFunction<MappedByteBuffer,Segment,MappedByteBuffer> duplicate=
            (m, s) -> { return m.duplicate(); };
        BiFunction<MappedByteBuffer,Segment,MappedByteBuffer> compact =
            (m, s) -> { return m.compact(); };

        Object[][] result = new Object[][] {
            {"Absolute slice", fc, 256, 512, 128, 128, 32, 32, absSlice},
            {"Relative slice", fc, 256, 512, 0, 128, 32, 32, relSlice},
            {"Duplicate", fc, 256, 512, 0, 256, 32, 32, duplicate},
            {"Compact", fc, 256, 512, 0, 256, 32, 32, compact}
        };

        return result;
    }

    @Test(dataProvider = "provider")
    public void test(String tst, FileChannel fc, int mapPosition, int mapLength,
        int sliceIndex, int sliceLength, int regionOffset, int regionLength,
        BiFunction<MappedByteBuffer,Segment,MappedByteBuffer> f)
        throws Exception {
        MappedByteBuffer mbb = fc.map(READ_WRITE, mapPosition, mapLength);
        mbb = f.apply(mbb, new Segment(sliceIndex, sliceLength));
        for (int i = regionOffset; i < regionOffset + regionLength; i++) {
            mbb.put(i, (byte)i);
        }
        mbb.force(regionOffset, regionOffset + regionLength);

        int fcPos = mapPosition + sliceIndex + regionOffset;
        int mbbPos = regionOffset;
        int length = regionLength;

        ByteBuffer buf = ByteBuffer.allocate(length);
        fc.position(fcPos);
        fc.read(buf);
        for (int i = 0; i < length; i++) {
            int fcVal = buf.get(i);
            int mbbVal = mbb.get(mbbPos + i);
            int val = regionOffset + i;
            Assert.assertTrue(fcVal == val && mbbVal == val,
                String.format("%s: i %d, fcVal %d, mbbVal %d, val %d",
                    tst, i, fcVal, mbbVal, val));
        }
    }
}
