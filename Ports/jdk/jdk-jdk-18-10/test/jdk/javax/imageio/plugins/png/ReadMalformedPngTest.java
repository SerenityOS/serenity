/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug     6945174
 * @summary Test verifies that PNG image readr throw correct exception
 *          if image contains a chunk with incorrect length.
 * @run     main ReadMalformedPngTest
 */

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.IIOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import org.w3c.dom.Node;

public class ReadMalformedPngTest {

    public static void main(String[] args) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(createTestPng());

        IIOException expected = null;
        try {
            ImageIO.read(bais);
        } catch (IIOException e) {
            expected = e;
        } catch (Throwable e) {
            throw new RuntimeException("Test failed!", e);
        }

        if (expected == null) {
            throw new RuntimeException("Test failed.");
        }

        System.out.println("Test passed.");
    }

    private static byte[] createTestPng() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        BufferedImage img = createTestImage();

        try {
            ImageOutputStream ios = ImageIO.createImageOutputStream(baos);

            ImageWriter w = ImageIO.getImageWritersByFormatName("PNG").next();

            w.setOutput(ios);

            ImageWriteParam p = w.getDefaultWriteParam();

            ImageTypeSpecifier t = ImageTypeSpecifier.createFromRenderedImage(img);

            IIOMetadata m = w.getDefaultImageMetadata(t, p);

            String nativeMetadataFormat = m.getNativeMetadataFormatName();

            Node root = m.getAsTree(nativeMetadataFormat);

            IIOMetadataNode textEntry = new IIOMetadataNode("tEXtEntry");
            textEntry.setAttribute("keyword", "comment");
            textEntry.setAttribute("value", "This is a test image for JDK-6945174");

            IIOMetadataNode text = new IIOMetadataNode("tEXt");
            text.appendChild(textEntry);

            root.appendChild(text);

            m.mergeTree(nativeMetadataFormat, root);

            IIOImage iio_img = new IIOImage(img, null, m);

            w.write(iio_img);

            w.dispose();
            ios.flush();
            ios.close();
        } catch (IOException e) {
            throw new RuntimeException("Test failed.", e);
        }

        baos.flush();

        byte[] data = baos.toByteArray();

        adjustCommentLength(Integer.MAX_VALUE + 0x1000, data);

        return data;
    }

    private static void adjustCommentLength(int v, byte[] data) {
        final int pos = getCommentPos(data);
        data[pos + 3] = (byte) (v & 0xFF);
        v = v >> 8;
        data[pos + 2] = (byte) (v & 0xFF);
        v = v >> 8;
        data[pos + 1] = (byte) (v & 0xFF);
        v = v >> 8;
        data[pos + 0] = (byte) (v & 0xFF);
    }

    private static int getCommentPos(byte[] d) {
        int p = 8;
        while (p + 8 < d.length) {
            if (d[p + 4] == (byte) 0x74 && d[p + 5] == (byte) 0x45 &&
                d[p + 6] == (byte) 0x58 && d[p + 7] == (byte) 0x74)
            {
                return p;
            }
            p++;
        }
        throw new RuntimeException("Test chunk was not found!");
    }

    private static BufferedImage createTestImage() {
        final int w = 128;
        final int h = 128;

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = img.createGraphics();
        g.setPaint(new GradientPaint(0, 0, Color.blue,
                w, h, Color.red));
        g.fillRect(0, 0, w, h);
        g.dispose();
        return img;
    }
}
