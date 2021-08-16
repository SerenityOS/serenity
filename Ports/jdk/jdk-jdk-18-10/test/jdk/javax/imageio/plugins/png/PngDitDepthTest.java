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

/*
* @test
* @bug 4991647
* @summary PNGMetadata.getAsTree() sets bitDepth to invalid value
* @run main PngDitDepthTest
*/

import org.w3c.dom.Node;

import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.util.Iterator;

public class PngDitDepthTest {

    public static void main(String[] args) throws IIOInvalidTreeException {

        // getting the writer for the png format
        Iterator iter = ImageIO.getImageWritersByFormatName("png");
        ImageWriter writer = (ImageWriter) iter.next();

        // creating a color model
        ColorModel colorModel = ColorModel.getRGBdefault();

        // creating a sample model
        SampleModel sampleModel = colorModel.createCompatibleSampleModel(640, 480);

        // creating a default metadata object
        IIOMetadata metaData = writer.getDefaultImageMetadata(new ImageTypeSpecifier(colorModel, sampleModel), null);
        String formatName = metaData.getNativeMetadataFormatName();

        // first call
        Node metaDataNode = metaData.getAsTree(formatName);
        try {
            metaData.setFromTree(formatName, metaDataNode);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // second call (bitdepht is already set to an invalid value)
        metaDataNode = metaData.getAsTree(formatName);

        metaData.setFromTree(formatName, metaDataNode);

    }
}
