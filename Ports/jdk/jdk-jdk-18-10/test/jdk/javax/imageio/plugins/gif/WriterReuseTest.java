/*
 * Copyright (c) 2005, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6283089
 * @summary Test verifies that abort flag is cleared by the next write() call
 */

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.event.IIOWriteProgressListener;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageOutputStream;

public class WriterReuseTest implements IIOWriteProgressListener {

    boolean isFirst = true;
    boolean isWritingCompleted = false;
    boolean isWritingAborted = false;

    public static void main(String[] args) throws IOException {
        doTest(false);
        doTest(true);
    }

    public static void doTest(boolean writeSequence) throws IOException {
        String format = "GIF";
        ImageWriter writer =
                ImageIO.getImageWritersByFormatName(format).next();
        if (writer == null) {
            throw new RuntimeException("No writer available for " + format);
        }

        BufferedImage img = createTestImage();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
        writer.setOutput(ios);

        WriterReuseTest t = new WriterReuseTest();
        writer.addIIOWriteProgressListener(t);

        ImageWriteParam param = writer.getDefaultWriteParam();
        IIOMetadata streamMetadata = writer.getDefaultStreamMetadata(param);
        IIOImage iioImg = new IIOImage(img, null, null);
        if (writeSequence) {
            writer.prepareWriteSequence(streamMetadata);
            writer.writeToSequence(iioImg, param);
        } else {
            writer.write(img);
        }

        if (!t.isWritingAborted || t.isWritingCompleted) {
            throw new RuntimeException("Test failed.");
        }
        t.reset();

        // next attempt after abort
        ImageOutputStream ios2 =
             ImageIO.createImageOutputStream(new ByteArrayOutputStream());
        writer.setOutput(ios2);
        if (writeSequence) {
            writer.writeToSequence(iioImg, param);
        } else {
            writer.write(img);
        }

        if (t.isWritingAborted || !t.isWritingCompleted) {
            throw new RuntimeException("Test failed.");
        }
        System.out.println("Test passed.");
    }

    public static BufferedImage createTestImage() {
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_BYTE_INDEXED);
        Graphics g = img.createGraphics();
        g.setColor(Color.black);
        g.fillRect(0, 0, 100, 100);

        g.setColor(Color.white);
        g.fillRect(10, 10, 80, 80);

        return img;
    }

    public WriterReuseTest() {
        isFirst = true;
        reset();
    }

    public void reset() {
        isWritingAborted = false;
        isWritingCompleted = false;
    }

    public void imageComplete(ImageWriter source) {
        System.out.println("Image Completed");
        this.isWritingCompleted = true;
    }

    public void imageProgress(ImageWriter source, float percentageDone) {
        System.out.println("Image Progress "+percentageDone);
        if (percentageDone > 50 && isFirst) {
            isFirst = false;
            source.abort();
        }
    }

    public void imageStarted(ImageWriter source, int imageIndex) {
        System.out.println("Image Started "+imageIndex);
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
        this.isWritingAborted = true;
    }
}
