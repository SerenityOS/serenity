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
 * @bug     8164931
 * @summary Test verifies that if we call ImageWriter.abort() in
 *          IIOWriteProgressListener.imageStarted() or
 *          IIOWriteProgressListener.imageProgress() are we
 *          calling IIOWriteProgressListener.readAborted() for all readers.
 * @run     main WriteAbortTest
 */
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.nio.file.Files;
import javax.imageio.ImageWriter;
import javax.imageio.event.IIOWriteProgressListener;
import javax.imageio.stream.ImageOutputStream;

public class WriteAbortTest implements IIOWriteProgressListener {

    ImageWriter writer = null;
    ImageOutputStream ios = null;
    BufferedImage bimg = null;
    File file;
    boolean startAbort = false;
    boolean startAborted = false;
    boolean progressAbort = false;
    boolean progressAborted = false;
    Color srccolor = Color.red;
    int width = 100;
    int heght = 100;

    public WriteAbortTest(String format) throws Exception {
        try {
            System.out.println("Test for format " + format);
            bimg = new BufferedImage(width, heght,
                    BufferedImage.TYPE_INT_RGB);

            Graphics2D g = bimg.createGraphics();
            g.setColor(srccolor);
            g.fillRect(0, 0, width, heght);
            g.dispose();

            file = File.createTempFile("src_", "." + format, new File("."));
            ImageInputStream ios = ImageIO.createImageOutputStream(file);

            ImageWriter writer =
                    ImageIO.getImageWritersByFormatName(format).next();

            writer.setOutput(ios);
            writer.addIIOWriteProgressListener(this);

            // Abort writing in IIOWriteProgressListener.imageStarted().
            startAbort = true;
            writer.write(bimg);
            startAbort = false;

            // Abort writing in IIOWriteProgressListener.imageProgress().
            progressAbort = true;
            writer.write(bimg);
            progressAbort = false;

            ios.close();
            /*
             * All abort requests from imageStarted,imageProgress
             * from IIOWriteProgressListener should be reached
             * otherwise throw RuntimeException.
             */
            if (!(startAborted
                    && progressAborted)) {
                throw new RuntimeException("All IIOWriteProgressListener abort"
                        + " requests are not processed for format "
                        + format);
            }
        } finally {
            Files.delete(file.toPath());
        }
    }

    /*
     * Abstract methods that we need to implement from
     * IIOWriteProgressListener, and relevant for this test case.
     */
    @Override
    public void imageStarted(ImageWriter source, int imageIndex) {
        System.out.println("imageStarted called");
        if (startAbort) {
            source.abort();
        }
    }

    @Override
    public void imageProgress(ImageWriter source, float percentageDone) {
        System.out.println("imageProgress called");
        if (progressAbort) {
            source.abort();
        }
    }

    @Override
    public void writeAborted(ImageWriter source) {
        System.out.println("writeAborted called");
        // Verify IIOWriteProgressListener.imageStarted() abort request.
        if (startAbort) {
            System.out.println("imageStarted aborted ");
            startAborted = true;
        }

        // Verify IIOWriteProgressListener.imageProgress() abort request.
        if (progressAbort) {
            System.out.println("imageProgress aborted ");
            progressAborted = true;
        }
    }

    public static void main(String args[]) throws Exception {
        final String[] formats = {"bmp", "png", "gif", "jpg", "tif"};
        for (String format : formats) {
            new WriteAbortTest(format);
        }
    }

    /*
     * Remaining abstract methods that we need to implement from
     * IIOWriteProgressListener, but not relevant for this test case.
     */
    @Override
    public void imageComplete(ImageWriter source) {
    }

    @Override
    public void thumbnailStarted(ImageWriter source, int imageIndex,
                                 int thumbnailIndex) {
    }

    @Override
    public void thumbnailProgress(ImageWriter source, float percentageDone) {
    }

    @Override
    public void thumbnailComplete(ImageWriter source) {
    }
}

