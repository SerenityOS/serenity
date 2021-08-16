/*
 * Copyright 2015 Red Hat, Inc.
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
 * @bug 8144071
 * @run main/othervm MarkTryFinallyReproducer
 * @summary Test that call to canDecodeInput in ImageIO don't corrupt
 *           mark/reset stack in ImageInputStream
 * @author Jiri Vanek
 */

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.IIOByteBuffer;
import javax.imageio.stream.ImageInputStream;


public class MarkTryFinallyReproducer {

    private static final byte[] bmp = new byte[]{
        127,127, 66, 77, -86, 0, 0, 0, 0, 0, 0, 0,
        122, 0, 0, 0, 108, 0, 0, 0, 4, 0, 0, 0, 4,
        0, 0, 0, 1, 0, 24, 0, 0, 0, 0, 0, 48, 0, 0,
        0, 19, 11, 0, 0, 19, 11, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 66, 71, 82, 115, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, 0, -1, -1, -1,
        -1, -1, -1, 0, -1, 0, -1, -1, -1, -1, 0, 0, 0, -17,
        0, -1, -1, -1, -1, -1, -1, 0, 0, 0, 0, 0, -1, -1, -1,
        -1, 0, 0, 0, -1, -1, -1, -1, -1, -1, 0, 0, -1
    };
    //first two are evil, we are skipping them later. Others are normal BMP

    private static class NotClosingImageInputStream implements ImageInputStream {

        private final ImageInputStream src;

        private NotClosingImageInputStream(ImageInputStream createImageInputStream) {
            this.src = createImageInputStream;
        }

        @Override
        public void setByteOrder(ByteOrder byteOrder) {
            src.setByteOrder(byteOrder);
        }

        @Override
        public ByteOrder getByteOrder() {
            return src.getByteOrder();
        }

        @Override
        public int read() throws IOException {
            return src.read();
        }

        @Override
        public int read(byte[] b) throws IOException {
            return src.read(b);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return src.read(b, off, len);
        }

        @Override
        public void readBytes(IIOByteBuffer buf, int len) throws IOException {
            src.readBytes(buf, len);
        }

        @Override
        public boolean readBoolean() throws IOException {
            return src.readBoolean();
        }

        @Override
        public byte readByte() throws IOException {
            return src.readByte();
        }

        @Override
        public int readUnsignedByte() throws IOException {
            return src.readUnsignedByte();
        }

        @Override
        public short readShort() throws IOException {
            return src.readShort();
        }

        @Override
        public int readUnsignedShort() throws IOException {
            return src.readUnsignedShort();
        }

        @Override
        public char readChar() throws IOException {
            return src.readChar();
        }

        @Override
        public int readInt() throws IOException {
            return src.readInt();
        }

        @Override
        public long readUnsignedInt() throws IOException {
            return src.readUnsignedInt();
        }

        @Override
        public long readLong() throws IOException {
            return src.readLong();
        }

        @Override
        public float readFloat() throws IOException {
            return src.readFloat();
        }

        @Override
        public double readDouble() throws IOException {
            return src.readDouble();
        }

        @Override
        public String readLine() throws IOException {
            return src.readLine();
        }

        @Override
        public String readUTF() throws IOException {
            return src.readUTF();
        }

        @Override
        public void readFully(byte[] b, int off, int len) throws IOException {
            src.readFully(b, off, len);
        }

        @Override
        public void readFully(byte[] b) throws IOException {
            src.readFully(b);
        }

        @Override
        public void readFully(short[] s, int off, int len) throws IOException {
            src.readFully(s, off, len);
        }

        @Override
        public void readFully(char[] c, int off, int len) throws IOException {
            src.readFully(c, off, len);
        }

        @Override
        public void readFully(int[] i, int off, int len) throws IOException {
            src.readFully(i, off, len);
        }

        @Override
        public void readFully(long[] l, int off, int len) throws IOException {
            src.readFully(l, off, len);
        }

        @Override
        public void readFully(float[] f, int off, int len) throws IOException {
            src.readFully(f, off, len);
        }

        @Override
        public void readFully(double[] d, int off, int len) throws IOException {
            src.readFully(d, off, len);
        }

        @Override
        public long getStreamPosition() throws IOException {
            return src.getStreamPosition();
        }

        @Override
        public int getBitOffset() throws IOException {
            return src.getBitOffset();
        }

        @Override
        public void setBitOffset(int bitOffset) throws IOException {
            src.setBitOffset(bitOffset);
        }

        @Override
        public int readBit() throws IOException {
            return src.readBit();
        }

        @Override
        public long readBits(int numBits) throws IOException {
            return src.readBits(numBits);
        }

        @Override
        public long length() throws IOException {
            return src.length();
        }

        @Override
        public int skipBytes(int n) throws IOException {
            return src.skipBytes(n);
        }

        @Override
        public long skipBytes(long n) throws IOException {
            return src.skipBytes(n);
        }

        @Override
        public void seek(long pos) throws IOException {
            src.seek(pos);
        }

        @Override
        public void mark() {
            src.mark();
        }

        @Override
        public void reset() throws IOException {
            src.reset();
        }

        @Override
        public void flushBefore(long pos) throws IOException {
            src.flushBefore(pos);
        }

        @Override
        public void flush() throws IOException {
            src.flush();
        }

        @Override
        public long getFlushedPosition() {
            return src.getFlushedPosition();
        }

        @Override
        public boolean isCached() {
            return src.isCached();
        }

        @Override
        public boolean isCachedMemory() {
            return src.isCachedMemory();
        }

        @Override
        public boolean isCachedFile() {
            return src.isCachedFile();
        }

        @Override
        public void close() throws IOException {
            //the only important one. nothing
        }
    }

    static final String readerClassName
            = MarkTryFinallyReproducerSpi.class.getName();
    static final String[] localNames = {"myNames"};
    static final String[] localSuffixes = {"mySuffixes"};
    static final String[] localMIMETypes = {"myMimes"};

    public static class MarkTryFinallyReproducerSpi extends ImageReaderSpi {

        public MarkTryFinallyReproducerSpi() {
            super("MarkTryFinallyReproducerSpi",
                    "1.0",
                    localNames,
                    localSuffixes,
                    localMIMETypes,
                    readerClassName,
                    new Class[]{ImageInputStream.class},
                    new String[0],
                    false,
                    null,
                    null,
                    new String[0],
                    new String[0],
                    false,
                    null,
                    null,
                    new String[0],
                    new String[0]);
        }

        @Override
        public String getDescription(Locale locale) {
            return "";
        }

        @Override
        public boolean canDecodeInput(Object input) throws IOException {
            throw new IOException("Bad luck");
        }

        @Override
        public ImageReader createReaderInstance(Object extension) {
            return null;
        }
    }

    public static void main(String[] args) throws IOException {
        MarkTryFinallyReproducerSpi spi = new MarkTryFinallyReproducerSpi();
        IIORegistry.getDefaultInstance().registerServiceProvider(spi);

        ImageInputStream iis1 =
          new NotClosingImageInputStream(ImageIO.createImageInputStream(new ByteArrayInputStream(bmp)));
        iis1.readByte();
        iis1.mark();
        long p1 = iis1.getStreamPosition();
        iis1.readByte();
        iis1.mark();
        long p2 = iis1.getStreamPosition();
        BufferedImage bi1 = ImageIO.read(iis1);
        iis1.reset();
        long pn2 = iis1.getStreamPosition();
        iis1.reset();
        long pn1 = iis1.getStreamPosition();
        if (p1 != pn1 || p2!= pn2) {
            throw new RuntimeException("Exception from call to canDecodeInput in ImageIO. " +
                                       "Corrupted stack in ImageInputStream");
        }

    }

}
