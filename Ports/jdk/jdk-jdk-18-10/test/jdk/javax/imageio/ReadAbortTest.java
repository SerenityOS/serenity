/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     4924727
 * @summary Test verifies that if we call ImageReader.abort() in
 *          IIOReadProgressListener.imageStarted() or
 *          IIOReadProgressListener.imageProgress() are we
 *          calling IIOReadProgressListener.readAborted() for all readers.
 * @run     main ReadAbortTest
 */
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.event.IIOReadProgressListener;
import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.nio.file.Files;

public class ReadAbortTest implements IIOReadProgressListener {

    ImageReader reader = null;
    ImageInputStream iis = null;
    BufferedImage bimg = null;
    File file;
    boolean startAbort = false;
    boolean startAborted = false;
    boolean progressAbort = false;
    boolean progressAborted = false;
    Color srccolor = Color.red;
    int width = 100;
    int heght = 100;

    public ReadAbortTest(String format) throws Exception {
        try {
            System.out.println("Test for format " + format);
            bimg = new BufferedImage(width, heght,
                    BufferedImage.TYPE_INT_RGB);

            Graphics2D g = bimg.createGraphics();
            g.setColor(srccolor);
            g.fillRect(0, 0, width, heght);
            g.dispose();

            file = File.createTempFile("src_", "." + format, new File("."));
            ImageIO.write(bimg, format, file);
            ImageInputStream iis = ImageIO.createImageInputStream(file);

            Iterator iter = ImageIO.getImageReaders(iis);
            while (iter.hasNext()) {
                reader = (ImageReader) iter.next();
                break;
            }
            reader.setInput(iis);
            reader.addIIOReadProgressListener(this);

            // Abort reading in IIOReadProgressListener.imageStarted().
            startAbort = true;
            bimg = reader.read(0);
            startAbort = false;

            // Abort reading in IIOReadProgressListener.imageProgress().
            progressAbort = true;
            bimg = reader.read(0);
            progressAbort = false;

            iis.close();
            /*
             * All abort requests from imageStarted,imageProgress and
             * imageComplete from IIOReadProgressListener should be reached
             * otherwise throw RuntimeException.
             */
            if (!(startAborted
                    && progressAborted)) {
                throw new RuntimeException("All IIOReadProgressListener abort"
                        + " requests are not processed for format "
                        + format);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            Files.delete(file.toPath());
        }
    }

    /*
     * Abstract methods that we need to implement from
     * IIOReadProgressListener, and relevant for this test case.
     */
    @Override
    public void imageStarted(ImageReader source, int imageIndex) {
        System.out.println("imageStarted called");
        if (startAbort) {
            source.abort();
        }
    }

    @Override
    public void imageProgress(ImageReader source, float percentageDone) {
        System.out.println("imageProgress called");
        if (progressAbort) {
            source.abort();
        }
    }

    @Override
    public void readAborted(ImageReader source) {
        System.out.println("readAborted called");
        // Verify IIOReadProgressListener.imageStarted() abort request.
        if (startAbort) {
            System.out.println("imageStarted aborted ");
            startAborted = true;
        }

        // Verify IIOReadProgressListener.imageProgress() abort request.
        if (progressAbort) {
            System.out.println("imageProgress aborted ");
            progressAborted = true;
        }
    }

    public static void main(String args[]) throws Exception {
        final String[] formats = {"bmp", "png", "gif", "jpg", "tif"};
        for (String format : formats) {
            new ReadAbortTest(format);
        }
    }

    /*
     * Remaining abstract methods that we need to implement from
     * IIOReadProgressListener, but not relevant for this test case.
     */
    @Override
    public void imageComplete(ImageReader source) {
    }

    @Override
    public void sequenceStarted(ImageReader reader, int i) {
    }

    @Override
    public void sequenceComplete(ImageReader reader) {
    }

    @Override
    public void thumbnailStarted(ImageReader reader, int i, int i1) {
    }

    @Override
    public void thumbnailProgress(ImageReader reader, float f) {
    }

    @Override
    public void thumbnailComplete(ImageReader reader) {
    }
}

