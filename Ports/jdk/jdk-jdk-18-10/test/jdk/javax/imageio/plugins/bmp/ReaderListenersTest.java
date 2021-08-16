/*
 * Copyright (c) 2003, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4924507
 * @summary Test that listeners of bmp reader receive correct events in case of
 *          BI_JPEG and BI_PNG compression types
 */

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.event.IIOReadProgressListener;
import javax.imageio.event.IIOReadUpdateListener;

public class ReaderListenersTest {
    public static final String[] compTypes = { "BI_JPEG", "BI_PNG" };

    public static void main(String[] args) {
        for (int i=0; i< compTypes.length; i++) {
            doTest(compTypes[i]);
        }
    }

    private static void doTest(String compression) {
        try {
            BufferedImage img = createTestImage();

            ImageWriter iw = (ImageWriter)
                ImageIO.getImageWritersByFormatName("bmp").next();
            if (iw == null) {
                throw new RuntimeException("No writers for bmp format."
                                           + " Test failed.");
            }


            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            iw.setOutput(ImageIO.createImageOutputStream(baos));
            ImageWriteParam param = iw.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionType(compression);

            iw.write(null, new IIOImage(img, null, null), param);
            baos.close();

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

            ImageReader ir = (ImageReader)
                ImageIO.getImageReadersByFormatName("bmp").next();
            if (ir == null) {
                throw new RuntimeException("No readers for bmp format."
                                           + " Test failed.");
            }

            IIOReadUpdateAdapter updateAdapter = new IIOReadUpdateAdapter();
            IIOReadProgressAdapter progressAdapter = new IIOReadProgressAdapter();
            ir.addIIOReadProgressListener(progressAdapter);
            ir.addIIOReadUpdateListener(updateAdapter);
            ir.setInput(ImageIO.createImageInputStream(bais));
            BufferedImage dst = ir.read(0);

            progressAdapter.checkResults();

            if (!updateAdapter.isImageUpdateUsed) {
                throw new RuntimeException("imageUpdate was not used."
                                           + " Test failed.");
            }
        } catch(IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Test failed");
        }
    }

    protected static BufferedImage createTestImage() {
        BufferedImage res = new BufferedImage(100, 100,
                                              BufferedImage.TYPE_INT_RGB);
        Graphics2D g = res.createGraphics();
        g.setColor(Color.red);
        g.fillRect(0,0, 100,100);
        return res;
    }

    static class IIOReadProgressAdapter implements IIOReadProgressListener {
        List progress = new ArrayList();
        public boolean isTestPassed = false;
        private boolean isImageStarted = false;
        private boolean isImageComplete = false;
        private boolean isSequenceComplete = false;
        private boolean isSequenceStarted = false;

        public void imageComplete(ImageReader source) {
            System.out.println("Image completed");
            if (!isImageComplete) {
                isImageComplete = true;
            } else {
                throw new RuntimeException("The imageComplete() is called twice."
                                           + " Test failed.");
            }
            checkProgress();
        }

        public void imageProgress(ImageReader source, float percentageDone) {
            System.out.println("Image Progress "+percentageDone);
            progress.add(new Float(percentageDone));
        }

        public void imageStarted(ImageReader source, int imageIndex) {
            System.out.println("Image Started "+imageIndex);
            if (!isImageStarted) {
                isImageStarted = true;
            } else {
                throw new RuntimeException("The imageStarted() was called twice. "
                                           + " Test failed.");
            }
            progress.clear();
        }

        public void thumbnailComplete(ImageReader source)  {
            System.out.println("Thubnail completed");
        }

        public void thumbnailProgress(ImageReader source,
                                      float percentageDone)
        {
            System.out.println("Thubnail Progress " + percentageDone);
        }

        public void thumbnailStarted(ImageReader source,
                                     int imageIndex, int thumbnailIndex)
        {
            System.out.println("Thubnail started " + imageIndex);
        }

        public void sequenceComplete(ImageReader source) {
            if (!isSequenceComplete) {
                isSequenceComplete = true;
            } else {
                throw new RuntimeException("The imageComplete() is called twice."
                                           + " Test failed.");
            }
        }

        public void sequenceStarted(ImageReader source, int minIndex) {
            if (!isSequenceStarted) {
                isSequenceStarted = true;
            } else {
                throw new RuntimeException("The imageComplete() is called twice."
                                           + " Test failed.");
            }
        }

        public void readAborted(ImageReader source) {
            System.out.println("read Aborted");
            checkProgress();
        }

        private void checkProgress() {
            Iterator i = progress.iterator();
            if (!i.hasNext()) {
                throw new RuntimeException("progress values list is empty!");
            }
            float val = ((Float)i.next()).floatValue();
            while(i.hasNext()) {
                float next = ((Float)i.next()).floatValue();
                if (val >= next) {
                    throw new RuntimeException("progress values do not increase!");
                }
                val = next;
            }
            isTestPassed = true;
            System.out.println("Test passed.");
        }

        public void checkResults() {
            if (isImageStarted && !isImageComplete) {
                throw new RuntimeException("The imageCompleted was not called."
                                           + " Test failed.");
            }
        }
    }

    static class IIOReadUpdateAdapter implements IIOReadUpdateListener {
        boolean isImageUpdateUsed = false;
        public void imageUpdate(ImageReader source, BufferedImage theImage,
                                int minX, int minY, int width, int height,
                                int periodX, int periodY, int[] bands)
        {
            System.out.println("imageUpdate");
            isImageUpdateUsed = true;
        }
        public void passComplete(ImageReader source, BufferedImage theImage) {
            System.out.println("passComplete");
        }
        public void passStarted(ImageReader source, BufferedImage theImage,
                                int pass, int minPass, int maxPass,
                                int minX, int minY, int periodX, int periodY,
                                int[] bands)
        {
            System.out.println("passStarted");
        }
        public void thumbnailPassComplete(ImageReader source,
                                          BufferedImage theThumbnail)
        {
            System.out.println("thumbnailPassComplete");
        }
        public void thumbnailPassStarted(ImageReader source,
                                         BufferedImage theThumbnail,
                                         int pass, int minPass, int maxPass,
                                         int minX, int minY,
                                         int periodX, int periodY,
                                         int[] bands)
        {
            System.out.println("thumbnailPassStarted");
        }
        public void thumbnailUpdate(ImageReader source,
                                    BufferedImage theThumbnail,
                                    int minX, int minY,
                                    int width, int height,
                                    int periodX, int periodY, int[] bands)
        {
            System.out.println("thumbnailUpdate");
        }
    }
}
