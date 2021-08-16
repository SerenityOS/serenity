/*
 * Copyright (c) 2007, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package com.sun.media.sound;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

/**
 * A jitter corrector to be used with SoftAudioPusher.
 *
 * @author Karl Helgason
 */
public final class SoftJitterCorrector extends AudioInputStream {

    private static class JitterStream extends InputStream {

        static int MAX_BUFFER_SIZE = 1048576;
        boolean active = true;
        Thread thread;
        AudioInputStream stream;
        // Cyclic buffer
        int writepos = 0;
        int readpos = 0;
        byte[][] buffers;
        private final Object buffers_mutex = new Object();

        // Adapative Drift Statistics
        int w_count = 1000;
        int w_min_tol = 2;
        int w_max_tol = 10;
        int w = 0;
        int w_min = -1;
        // Current read buffer
        int bbuffer_pos = 0;
        int bbuffer_max = 0;
        byte[] bbuffer = null;

        public byte[] nextReadBuffer() {
            synchronized (buffers_mutex) {
                if (writepos > readpos) {
                    int w_m = writepos - readpos;
                    if (w_m < w_min)
                        w_min = w_m;

                    int buffpos = readpos;
                    readpos++;
                    return buffers[buffpos % buffers.length];
                }
                w_min = -1;
                w = w_count - 1;
            }
            while (true) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                    return null;
                }
                synchronized (buffers_mutex) {
                    if (writepos > readpos) {
                        w = 0;
                        w_min = -1;
                        w = w_count - 1;
                        int buffpos = readpos;
                        readpos++;
                        return buffers[buffpos % buffers.length];
                    }
                }
            }
        }

        public byte[] nextWriteBuffer() {
            synchronized (buffers_mutex) {
                return buffers[writepos % buffers.length];
            }
        }

        public void commit() {
            synchronized (buffers_mutex) {
                writepos++;
                if ((writepos - readpos) > buffers.length) {
                    int newsize = (writepos - readpos) + 10;
                    newsize = Math.max(buffers.length * 2, newsize);
                    buffers = new byte[newsize][buffers[0].length];
                }
            }
        }

        JitterStream(AudioInputStream s, int buffersize,
                int smallbuffersize) {
            this.w_count = 10 * (buffersize / smallbuffersize);
            if (w_count < 100)
                w_count = 100;
            this.buffers
                    = new byte[(buffersize/smallbuffersize)+10][smallbuffersize];
            this.bbuffer_max = MAX_BUFFER_SIZE / smallbuffersize;
            this.stream = s;


            Runnable runnable = new Runnable() {

                @Override
                public void run() {
                    AudioFormat format = stream.getFormat();
                    int bufflen = buffers[0].length;
                    int frames = bufflen / format.getFrameSize();
                    long nanos = (long) (frames * 1000000000.0
                                            / format.getSampleRate());
                    long now = System.nanoTime();
                    long next = now + nanos;
                    int correction = 0;
                    while (true) {
                        synchronized (JitterStream.this) {
                            if (!active)
                                break;
                        }
                        int curbuffsize;
                        synchronized (buffers) {
                            curbuffsize = writepos - readpos;
                            if (correction == 0) {
                                w++;
                                if (w_min != Integer.MAX_VALUE) {
                                    if (w == w_count) {
                                        correction = 0;
                                        if (w_min < w_min_tol) {
                                            correction = (w_min_tol + w_max_tol)
                                                            / 2 - w_min;
                                        }
                                        if (w_min > w_max_tol) {
                                            correction = (w_min_tol + w_max_tol)
                                                            / 2 - w_min;
                                        }
                                        w = 0;
                                        w_min = Integer.MAX_VALUE;
                                    }
                                }
                            }
                        }
                        while (curbuffsize > bbuffer_max) {
                            synchronized (buffers) {
                                curbuffsize = writepos - readpos;
                            }
                            synchronized (JitterStream.this) {
                                if (!active)
                                    break;
                            }
                            try {
                                Thread.sleep(1);
                            } catch (InterruptedException e) {
                                //e.printStackTrace();
                            }
                        }

                        if (correction < 0)
                            correction++;
                        else {
                            byte[] buff = nextWriteBuffer();
                            try {
                                int n = 0;
                                while (n != buff.length) {
                                    int s = stream.read(buff, n, buff.length
                                            - n);
                                    if (s < 0)
                                        throw new EOFException();
                                    if (s == 0)
                                        Thread.yield();
                                    n += s;
                                }
                            } catch (IOException e1) {
                                //e1.printStackTrace();
                            }
                            commit();
                        }

                        if (correction > 0) {
                            correction--;
                            next = System.nanoTime() + nanos;
                            continue;
                        }
                        long wait = next - System.nanoTime();
                        if (wait > 0) {
                            try {
                                Thread.sleep(wait / 1000000L);
                            } catch (InterruptedException e) {
                                //e.printStackTrace();
                            }
                        }
                        next += nanos;
                    }
                }
            };

            thread = new Thread(null, runnable, "JitterCorrector", 0, false);
            thread.setDaemon(true);
            thread.setPriority(Thread.MAX_PRIORITY);
            thread.start();
        }

        @Override
        public void close() throws IOException {
            synchronized (this) {
                active = false;
            }
            try {
                thread.join();
            } catch (InterruptedException e) {
                //e.printStackTrace();
            }
            stream.close();
        }

        @Override
        public int read() throws IOException {
            byte[] b = new byte[1];
            if (read(b) == -1)
                return -1;
            return b[0] & 0xFF;
        }

        public void fillBuffer() {
            bbuffer = nextReadBuffer();
            bbuffer_pos = 0;
        }

        @Override
        public int read(byte[] b, int off, int len) {
            if (bbuffer == null)
                fillBuffer();
            int bbuffer_len = bbuffer.length;
            int offlen = off + len;
            while (off < offlen) {
                if (available() == 0)
                    fillBuffer();
                else {
                    byte[] bbuffer = this.bbuffer;
                    int bbuffer_pos = this.bbuffer_pos;
                    while (off < offlen && bbuffer_pos < bbuffer_len)
                        b[off++] = bbuffer[bbuffer_pos++];
                    this.bbuffer_pos = bbuffer_pos;
                }
            }
            return len;
        }

        @Override
        public int available() {
            return bbuffer.length - bbuffer_pos;
        }
    }

    public SoftJitterCorrector(AudioInputStream stream, int buffersize,
            int smallbuffersize) {
        super(new JitterStream(stream, buffersize, smallbuffersize),
                stream.getFormat(), stream.getFrameLength());
    }
}
