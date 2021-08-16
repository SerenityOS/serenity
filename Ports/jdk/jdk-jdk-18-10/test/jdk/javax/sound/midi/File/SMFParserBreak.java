/*
 * Copyright (c) 2002, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;

/**
 * @test
 * @bug 4910986
 * @summary MIDI file parser breaks up on http connection
 */
public class SMFParserBreak {

    public static void main(String[] args) throws Exception {

        InputStream is = new ByteArrayInputStream(midifile);
        // create a buffered input stream that seems
        // to be on an unfortunate boundary for the
        // 1.4.2 SMF parser implementation
        is = new ChunkInputStream(is, 32);
        Sequence sequence = MidiSystem.getSequence(is);

        long duration = sequence.getMicrosecondLength() / 10000;
        System.out.println("Duration: "+duration+" deciseconds ");

        // the test is passed if no exception thrown
        System.out.println("Test passed");
    }

    // A MIDI file
    static byte[] midifile = {
        77, 84, 104, 100, 0, 0, 0, 6, 0, 1, 0, 3, -30, 120, 77, 84, 114, 107, 0,
        0, 0, 123, 0, -112, 30, 100, -113, 49, -128, 50, 100, -114, 69, -112, 31,
        100, -114, 33, -128, 51, 100, -114, 55, -112, 32, 100, -114, 120, -128, 52,
        100, -114, 40, -112, 33, 100, -114, 26, -128, 53, 100, -114, 26, -112, 34,
        100, -114, 76, -128, 54, 100, -114, 12, -112, 35, 100, -114, 91, -128, 55,
        100, -114, 69, -112, 36, 100, -114, 33, -128, 56, 100, -114, 55, -112, 37,
        100, -114, 84, -128, 57, 100, -114, 40, -112, 38, 100, -114, 26, -128, 58,
        100, -114, 26, -112, 39, 100, -113, 24, -128, 59, 100, -113, 60, -112, 40,
        100, -113, 110, -128, 60, 100, -113, 96, -112, 41, 100, -113, 39, -128, 61,
        100, 0, -1, 47, 0, 77, 84, 114, 107, 0, 0, 0, 4, 0, -1, 47, 0, 77, 84, 114,
        107, 0, 0, 0, 4, 0, -1, 47, 0
    };
}

/* an input stream that always returns data in chunks */
class ChunkInputStream extends FilterInputStream {
    int chunkSize;
    int p = 0; // position

    public ChunkInputStream(InputStream is, int chunkSize) {
        super(is);
        this.chunkSize = chunkSize;
    }

    // override to increase counter
    public int read() throws IOException {
        int ret = super.read();
        if (ret >= 0) {
            p++;
        }
        return ret;
    }

    // override to make sure that read(byte[], int, int) is used
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    // override to split the data in chunks
    public int read(byte[] b, int off, int len) throws IOException {
        // if we would pass a chunk boundary,
        // only return up to the chunk boundary
        if ( (p / chunkSize) < ( (p+len) / chunkSize)) {
            // p+len is in the next chunk
            len -= ((p+len) % chunkSize);
        }
        int ret = super.read(b, off, len);
        if (ret >= 0) {
            p += ret;
        }
        return ret;
    }
}
