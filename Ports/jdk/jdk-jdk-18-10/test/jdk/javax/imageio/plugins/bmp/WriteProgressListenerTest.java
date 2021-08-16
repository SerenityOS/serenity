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
 * @bug 4897067 4920152
 * @summary Tests that IIOWriteProgressListener receives correct progress
 *          percentage. Also it tests problem described in 4920152: test fails
 *          if imageComplete() or imageStarted() was called twice.
 */

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.event.IIOWriteProgressListener;
import javax.imageio.stream.ImageOutputStream;

public class WriteProgressListenerTest {


    protected static String format = "BMP";

        protected String compression_type;
        protected WriteProgressListener listener;

        public WriteProgressListenerTest(String compression_type) {
        this.compression_type = compression_type;
        listener = new WriteProgressListener();
    }

    public void doTest() {
        try {
            System.out.println("Progress test for " + compression_type);
            BufferedImage bi = new BufferedImage(20, 300, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageOutputStream ios = ImageIO.createImageOutputStream(baos);

            Iterator iter = ImageIO.getImageWritersByFormatName(format);
            if (!iter.hasNext()) {
                throw new RuntimeException("No available writer for " + format);
            }
            ImageWriter writer = (ImageWriter)iter.next();

            writer.setOutput(ios);
            writer.addIIOWriteProgressListener(listener);

            IIOImage iio_img = new IIOImage(bi, null, null);

            ImageWriteParam param = writer.getDefaultWriteParam();

            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionType(compression_type);


            writer.write(null, iio_img, param);

            if (!listener.isTestPassed) {
                throw new RuntimeException("Test for " + compression_type + " does not finish correctly!");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

        public static void main(String args[]) {
        String[] compression_types = new String[] { "BI_RGB",
                                                    "BI_JPEG",
                                                    "BI_PNG"};

        for(int i=0; i<compression_types.length; i++) {
            WriteProgressListenerTest test = new
                WriteProgressListenerTest(compression_types[i]);
            test.doTest();
        }
        }

    static class WriteProgressListener implements IIOWriteProgressListener {
        List progress;
        public boolean isTestPassed = false;
        private boolean isImageStarted = false;
        private boolean isImageComplete = false;

        public WriteProgressListener() {
            progress = new ArrayList();
        }

        public void imageComplete(ImageWriter source) {
            System.out.println("Image Completed");
            if (!isImageComplete) {
                isImageComplete = true;
            } else {
                throw new RuntimeException("The imageComplete() was called twice."
                                           + " Test failed.");
            }

            checkProgress();
        }
        public void imageProgress(ImageWriter source, float percentageDone) {
            System.out.println("Image Progress "+percentageDone);
            progress.add(new Float(percentageDone));
        }

        public void imageStarted(ImageWriter source, int imageIndex) {
            System.out.println("Image Started "+imageIndex);
            if (!isImageStarted) {
                isImageStarted = true;
            } else {
                throw new RuntimeException("The imageStarted() was called twice. "
                                           + " Test failed.");
            }
            progress.clear();
        }

        public void thumbnailComplete(ImageWriter source)  {
            System.out.println("Thubnail completed");
        }

        public void thumbnailProgress(ImageWriter source, float percentageDone) {
            System.out.println("Thubnail Progress " + percentageDone);
        }

        public void thumbnailStarted(ImageWriter source, int imageIndex, int thumbnailIndex) {
            System.out.println("Thubnail started " + imageIndex);
        }

        public void writeAborted(ImageWriter source) {
            System.out.println("Writing Aborted");
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
    }

}
