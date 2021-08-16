/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5082756
 * @summary ensure that boolean attributes follow ( "TRUE" | "FALSE" )
 *          including correct (i.e. upper) case
 *
 * @run main BooleanAttributes
 */

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class BooleanAttributes {

    private static TransformerFactory transformerFactory =
        TransformerFactory.newInstance();

    private static XPath xpathEngine = XPathFactory.newInstance().newXPath();

    public static void main(String[] args) throws Exception {
        test("image/png", false, "<javax_imageio_1.0 />",
             "Chroma/BlackIsZero/@value",
             "Compression/Lossless/@value");

        test("image/png", false,
             "<javax_imageio_png_1.0>" +
             "<iTXt><iTXtEntry keyword='Comment' compressionFlag='TRUE' " +
             "compressionMethod='0' languageTag='en' " +
             "translatedKeyword='comment' text='foo'/></iTXt>" +
             "</javax_imageio_png_1.0>",
             "iTXt/iTXtEntry/@compressionFlag");

        test("image/png", false,
             "<javax_imageio_png_1.0>" +
             "<iTXt><iTXtEntry keyword='Comment' compressionFlag='FALSE' " +
             "compressionMethod='0' languageTag='en' " +
             "translatedKeyword='comment' text='foo'/></iTXt>" +
             "</javax_imageio_png_1.0>",
             "iTXt/iTXtEntry/@compressionFlag");

        test("image/gif", false, "<javax_imageio_1.0 />",
             "Chroma/BlackIsZero/@value",
             "Compression/Lossless/@value");

        test("image/gif", false,
             "<javax_imageio_gif_image_1.0>" +
             "<ImageDescriptor imageLeftPosition='0' imageTopPosition='0' " +
             "imageWidth='16' imageHeight='16' interlaceFlag='TRUE' />" +
             "<LocalColorTable sizeOfLocalColorTable='2' " +
             "backgroundColorIndex='1' sortFlag='TRUE'>" +
             "<ColorTableEntry index='0' red='0' green='0' blue='0' />" +
             "<ColorTableEntry index='1' red='255' green='255' blue='255' />" +
             "</LocalColorTable>" +
             "<GraphicControlExtension disposalMethod='doNotDispose' " +
             "userInputFlag='FALSE' transparentColorFlag='TRUE' " +
             "delayTime='100' transparentColorIndex='1' />" +
             "</javax_imageio_gif_image_1.0>",
             "ImageDescriptor/@interlaceFlag",
             "LocalColorTable/@sortFlag",
             "GraphicControlExtension/@userInputFlag",
             "GraphicControlExtension/@transparentColorFlag");

        test("image/gif", true,
             "<javax_imageio_gif_stream_1.0>" +
             "<GlobalColorTable sizeOfGlobalColorTable='2' " +
             "backgroundColorIndex='1' sortFlag='TRUE'>" +
             "<ColorTableEntry index='0' red='0' green='0' blue='0' />" +
             "<ColorTableEntry index='1' red='255' green='255' blue='255' />" +
             "</GlobalColorTable>" +
             "</javax_imageio_gif_stream_1.0>",
             "GlobalColorTable/@sortFlag");

        test("image/jpeg", false, "<javax_imageio_1.0 />",
             "Compression/Lossless/@value");
    }

    private static void transform(Source src, Result dst)
        throws Exception
    {
        transformerFactory.newTransformer().transform(src, dst);
    }

    private static void verify(Node meta, String[] xpaths, boolean required)
        throws Exception
    {
        for (String xpath: xpaths) {
            NodeList list = (NodeList)
                xpathEngine.evaluate(xpath, meta, XPathConstants.NODESET);
            if (list.getLength() == 0 && required)
                throw new AssertionError("Missing value: " + xpath);
            for (int i = 0; i < list.getLength(); ++i) {
                String value = list.item(i).getNodeValue();
                if (!(value.equals("TRUE") || value.equals("FALSE")))
                    throw new AssertionError(xpath + " has value " + value);
            }
        }
    }

    public static void test(String mimeType, boolean useStreamMeta,
                            String metaXml, String... boolXpaths)
        throws Exception
    {
        BufferedImage img =
            new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        ImageWriter iw = ImageIO.getImageWritersByMIMEType(mimeType).next();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageOutputStream ios = new MemoryCacheImageOutputStream(os);
        iw.setOutput(ios);
        ImageWriteParam param = null;
        IIOMetadata streamMeta = iw.getDefaultStreamMetadata(param);
        IIOMetadata imageMeta =
            iw.getDefaultImageMetadata(new ImageTypeSpecifier(img), param);
        IIOMetadata meta = useStreamMeta ? streamMeta : imageMeta;
        Source src = new StreamSource(new StringReader(metaXml));
        DOMResult dst = new DOMResult();
        transform(src, dst);
        Document doc = (Document)dst.getNode();
        Element node = doc.getDocumentElement();
        String metaFormat = node.getNodeName();

        // Verify that the default metadata gets formatted correctly.
        verify(meta.getAsTree(metaFormat), boolXpaths, false);

        meta.mergeTree(metaFormat, node);

        // Verify that the merged metadata gets formatte correctly.
        verify(meta.getAsTree(metaFormat), boolXpaths, true);

        iw.write(streamMeta, new IIOImage(img, null, imageMeta), param);
        iw.dispose();
        ios.close();
        ImageReader ir = ImageIO.getImageReader(iw);
        byte[] bytes = os.toByteArray();
        if (bytes.length == 0)
            throw new AssertionError("Zero length image file");
        ByteArrayInputStream is = new ByteArrayInputStream(bytes);
        ImageInputStream iis = new MemoryCacheImageInputStream(is);
        ir.setInput(iis);
        if (useStreamMeta) meta = ir.getStreamMetadata();
        else meta = ir.getImageMetadata(0);

        // Verify again after writing and re-reading the image
        verify(meta.getAsTree(metaFormat), boolXpaths, true);
    }

    public static void xtest(Object... eatAnyArguments) {
        System.err.println("Disabled test! Change xtest back into test!");
    }

}
