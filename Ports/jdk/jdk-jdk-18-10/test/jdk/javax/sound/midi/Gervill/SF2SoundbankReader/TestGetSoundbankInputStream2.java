/*
 * Copyright (c) 2007, 2015, Oracle and/or its affiliates. All rights reserved.
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
 @summary Test SF2SoundbankReader getSoundbank(InputStream) method using
     very bad InputStream which can only read 1 byte at time
 @modules java.desktop/com.sun.media.sound
 */

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.midi.Patch;
import javax.sound.midi.Soundbank;

import com.sun.media.sound.SF2SoundbankReader;

public class TestGetSoundbankInputStream2 {

    private static class BadInputStream extends InputStream
    {

        InputStream is;

        public BadInputStream(InputStream is)
        {
            this.is = is;
        }

        public int read() throws IOException {
            return is.read();
        }

        public int read(byte[] b, int off, int len) throws IOException {
            if(len > 1) len = 1;
            return is.read(b, off, len);
        }

        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        public long skip(long n) throws IOException {
            if(n > 1) n = 1;
            return is.skip(n);
        }

        public int available() throws IOException {
            int avail = is.available();
            if(avail > 1) avail = 1;
            return avail;
        }

        public void close() throws IOException {
            is.close();
        }

        public synchronized void mark(int readlimit) {
            is.mark(readlimit);
        }

        public boolean markSupported() {
            return is.markSupported();
        }

        public synchronized void reset() throws IOException {
            is.reset();
        }

    }

    private static void assertTrue(boolean value) throws Exception
    {
        if(!value)
            throw new RuntimeException("assertTrue fails!");
    }

    public static void main(String[] args) throws Exception {
        File file = new File(System.getProperty("test.src", "."), "ding.sf2");
        FileInputStream fis = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(fis);
        try
        {
            InputStream badis = new BadInputStream(bis);
            Soundbank sf2 = new SF2SoundbankReader().getSoundbank(badis);
            assertTrue(sf2.getInstruments().length == 1);
            Patch patch = sf2.getInstruments()[0].getPatch();
            assertTrue(patch.getProgram() == 0);
            assertTrue(patch.getBank() == 0);
        }
        finally
        {
            bis.close();
        }
    }
}
