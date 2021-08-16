/*
 * Copyright (c) 2000, 2001, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test reading and writing from Pipes
 * @key randomness
 */

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.util.Random;


/**
 * Testing PipeChannel
 */
public class PipeChannel {

   private static Random generator = new Random();

    public static void main(String[] args) throws Exception {
        for (int x=0; x<100; x++) {
            SelectorProvider sp = SelectorProvider.provider();
            Pipe p = sp.openPipe();
            Pipe.SinkChannel sink = p.sink();
            Pipe.SourceChannel source = p.source();

            ByteBuffer outgoingdata = ByteBuffer.allocateDirect(10);
            byte[] someBytes = new byte[10];
            generator.nextBytes(someBytes);
            outgoingdata.put(someBytes);
            outgoingdata.flip();

            int totalWritten = 0;
            while (totalWritten < 10) {
                int written = sink.write(outgoingdata);
                if (written < 0)
                    throw new Exception("Write failed");
                totalWritten += written;
            }

            ByteBuffer incomingdata = ByteBuffer.allocateDirect(10);
            int totalRead = 0;
            do {
                int bytesRead = source.read(incomingdata);
                if (bytesRead > 0)
                    totalRead += bytesRead;
            } while(totalRead < 10);

            for(int i=0; i<10; i++)
                if (outgoingdata.get(i) != incomingdata.get(i))
                    throw new Exception("Pipe failed");
            sink.close();
            source.close();
        }
    }
}
