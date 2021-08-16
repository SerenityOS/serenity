/*
 * Copyright (c) 2001, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4420342 4421831
 * @summary Checks that IIOWriteProgressListener methods are called in proper
 *          sequence for the JPEG and PNG writers
 */

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.event.IIOWriteProgressListener;
import javax.imageio.stream.ImageOutputStream;

public class WriteProgressListenerTest implements IIOWriteProgressListener {

    final static int UNINITIALIZED = 0;
    final static int IMAGE_STARTED = 1;
    final static int IMAGE_COMPLETE = 2;

    int state = UNINITIALIZED;
    float prevPercentageDone = 0.0F;
    File tempFile = null;

    public WriteProgressListenerTest(String format) throws IOException {
        ImageWriter writer = null;
        Iterator witer = ImageIO.getImageWritersByFormatName(format);
        if (!witer.hasNext()) {
            error("No writer for format " + format + "!");
        }
        writer = (ImageWriter)witer.next();

        System.out.println("Got writer " + writer);
        writer.addIIOWriteProgressListener(this);

        this.tempFile = File.createTempFile("imageio", ".tmp");
        tempFile.deleteOnExit();
        ImageOutputStream stream = ImageIO.createImageOutputStream(tempFile);
        writer.setOutput(stream);

        BufferedImage im =
            new BufferedImage(100, 100, BufferedImage.TYPE_3BYTE_BGR);

        this.state = UNINITIALIZED;

        writer.write(im);

        if (this.state == UNINITIALIZED) {
            error("imageStarted never called!");
        }
        if (this.state != IMAGE_COMPLETE) {
            error("imageComplete not called!");
        }

        print("Passed!");
    }

    private void error(String s) {
        if (tempFile != null) {
            tempFile.delete();
        }
        throw new RuntimeException(s);
    }

    private void print(String s) {
        System.out.println(s);
    }

    public void sequenceStarted(ImageWriter source) {
        error("Obsolete method sequenceStarted was called!");
    }

    public void sequenceComplete(ImageWriter source) {
        error("Obsolete method sequenceComplete was called!");
    }

    public void imageStarted(ImageWriter source, int imageIndex) {
        print("imageStarted: imageIndex = " + imageIndex);

        if (state != UNINITIALIZED) {
            error("imageStarted not called first!");
        }
        state = IMAGE_STARTED;
        prevPercentageDone = 0.0F;
    }

    public void imageProgress(ImageWriter source,
                              float percentageDone) {
        print("imageProgress: percentageDone = " + percentageDone);

        if (state != IMAGE_STARTED) {
            error("imageProgress called without prior imageStarted!");
        }
        if (percentageDone < prevPercentageDone) {
            error("percentageDone did not increase!");
        }
        prevPercentageDone = percentageDone;
    }

    public void imageComplete(ImageWriter source) {
        print("imageComplete");

        if (state != IMAGE_STARTED) {
            error("imageComplete called without imageStarted!");
        }
        if (prevPercentageDone == 0.0F) {
            error("percentageDone was never updated!");
        }
        state = IMAGE_COMPLETE;
    }

    public void thumbnailStarted(ImageWriter source,
                                 int imageIndex, int thumbnailIndex) {
    }

    public void thumbnailProgress(ImageWriter source, float percentageDone) {
    }

    public void thumbnailComplete(ImageWriter source) {
    }

    public void writeAborted(ImageWriter source) {
    }

    public static void main(String[] args) throws IOException {
        new WriteProgressListenerTest("jpeg");
        new WriteProgressListenerTest("png");
    }
}
