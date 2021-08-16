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
 * @bug 4420318 8183341
 * @summary Checks that an IllegalStateException is thrown by getNumImages(true)
 *          when seekForwardOnly is true
 * @modules java.desktop/com.sun.imageio.plugins.gif
 *          java.desktop/com.sun.imageio.plugins.jpeg
 *          java.desktop/com.sun.imageio.plugins.png
 */

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.sun.imageio.plugins.gif.GIFImageReader;
import com.sun.imageio.plugins.jpeg.JPEGImageReader;
import com.sun.imageio.plugins.png.PNGImageReader;

public class AllowSearch {
    private static void test(ImageReader reader, String format)
        throws IOException {
        boolean gotISE = false;
        File f = null;
        ImageInputStream stream = null;
        try {
            f = File.createTempFile("imageio", ".tmp");
            stream = ImageIO.createImageInputStream(f);
            reader.setInput(stream, true);

            try {
                int numImages = reader.getNumImages(true);
            } catch (IOException ioe) {
                gotISE = false;
            } catch (IllegalStateException ise) {
                gotISE = true;
            }
        } finally {
            if (stream != null) {
                stream.close();
            }

            reader.dispose();

            if (f != null) {
                Files.delete(f.toPath());
            }
        }

        if (!gotISE) {
            throw new RuntimeException("Failed to get desired exception for " +
                                       format + " reader!");
        }
    }

    public static void main(String[] args) throws IOException {
        ImageReader gifReader = new GIFImageReader(null);
        ImageReader jpegReader = new JPEGImageReader(null);
        ImageReader pngReader = new PNGImageReader(null);

        test(gifReader, "GIF");
        test(jpegReader, "JPEG");
        test(pngReader, "PNG");
    }
}
