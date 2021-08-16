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

package jdk.internal.net.http.frame;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;
import static java.lang.System.out;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.*;

public class FramesDecoderTest {

    abstract class TestFrameProcessor implements FramesDecoder.FrameProcessor {
        protected volatile int count;
        public int numberOfFramesDecoded() { return count; }
    }

    /**
     * Verifies that a ByteBuffer containing more that one frame, destined
     * to be returned to the user's subscriber, i.e. a data frame, does not
     * inadvertently expose the following frame ( between its limit and
     * capacity ).
     */
    @Test
    public void decodeDataFrameFollowedByAnother() throws Exception {
        // input frames for to the decoder
        List<ByteBuffer> data1 = List.of(ByteBuffer.wrap("XXXX".getBytes(UTF_8)));
        DataFrame dataFrame1 = new DataFrame(1, 0, data1);
        List<ByteBuffer> data2 = List.of(ByteBuffer.wrap("YYYY".getBytes(UTF_8)));
        DataFrame dataFrame2 = new DataFrame(1, 0, data2);

        List<ByteBuffer> buffers = new ArrayList<>();
        FramesEncoder encoder = new FramesEncoder();
        buffers.addAll(encoder.encodeFrame(dataFrame1));
        buffers.addAll(encoder.encodeFrame(dataFrame2));

        ByteBuffer combined = ByteBuffer.allocate(1024);
        buffers.stream().forEach(combined::put);
        combined.flip();

        TestFrameProcessor testFrameProcessor = new TestFrameProcessor() {
            @Override
            public void processFrame(Http2Frame frame) throws IOException {
                assertTrue(frame instanceof DataFrame);
                DataFrame dataFrame = (DataFrame) frame;
                List<ByteBuffer> list = dataFrame.getData();
                assertEquals(list.size(), 1);
                ByteBuffer data = list.get(0);
                byte[] bytes = new byte[data.remaining()];
                data.get(bytes);
                if (count == 0) {
                    assertEquals(new String(bytes, UTF_8), "XXXX");
                    out.println("First data received:" + data);
                    assertEquals(data.position(), data.limit());  // since bytes read
                    assertEquals(data.limit(), data.capacity());
                } else {
                    assertEquals(new String(bytes, UTF_8), "YYYY");
                    out.println("Second data received:" + data);
                }
                count++;
            }
        };
        FramesDecoder decoder = new FramesDecoder(testFrameProcessor);

        out.println("Sending " + combined + " to decoder: ");
        decoder.decode(combined);
        Assert.assertEquals(testFrameProcessor.numberOfFramesDecoded(), 2);
    }


    /**
     * Verifies that a ByteBuffer containing ONLY data one frame, destined
     * to be returned to the user's subscriber, does not restrict the capacity.
     * The complete buffer ( all its capacity ), since no longer used by the
     * HTTP Client, should be returned to the user.
     */
    @Test
    public void decodeDataFrameEnsureNotCapped() throws Exception {
        // input frames for to the decoder
        List<ByteBuffer> data1 = List.of(ByteBuffer.wrap("XXXX".getBytes(UTF_8)));
        DataFrame dataFrame1 = new DataFrame(1, 0, data1);

        List<ByteBuffer> buffers = new ArrayList<>();
        FramesEncoder encoder = new FramesEncoder();
        buffers.addAll(encoder.encodeFrame(dataFrame1));

        ByteBuffer combined = ByteBuffer.allocate(1024);
        buffers.stream().forEach(combined::put);
        combined.flip();

        TestFrameProcessor testFrameProcessor = new TestFrameProcessor() {
            @Override
            public void processFrame(Http2Frame frame) throws IOException {
                assertTrue(frame instanceof DataFrame);
                DataFrame dataFrame = (DataFrame) frame;
                List<ByteBuffer> list = dataFrame.getData();
                assertEquals(list.size(), 1);
                ByteBuffer data = list.get(0);
                byte[] bytes = new byte[data.remaining()];
                data.get(bytes);
                assertEquals(new String(bytes, UTF_8), "XXXX");
                out.println("First data received:" + data);
                assertEquals(data.position(), data.limit());  // since bytes read
                //assertNotEquals(data.limit(), data.capacity());
                assertEquals(data.capacity(), 1024 - 9 /*frame header*/);
                count++;
            }
        };
        FramesDecoder decoder = new FramesDecoder(testFrameProcessor);

        out.println("Sending " + combined + " to decoder: ");
        decoder.decode(combined);
        Assert.assertEquals(testFrameProcessor.numberOfFramesDecoded(), 1);
    }
}
