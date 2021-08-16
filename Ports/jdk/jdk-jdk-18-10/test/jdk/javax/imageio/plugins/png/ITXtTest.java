/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6541476 7059970
 * @summary Test verifies that ImageIO PNG plug-in correctly handles the
 *          iTxt chunk (International textual data).
 *
 * @run     main ITXtTest
 */

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.IIOImage;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.ImageInputStream;

import org.w3c.dom.Node;

public class ITXtTest {
    static public void main(String args[]) {
        ITXtTest t_en = new ITXtTest();
        t_en.description = "xml - en";
        t_en.keyword = "XML:com.adobe.xmp";
        t_en.isCompressed = false;
        t_en.compression = 0;
        t_en.language = "en";
        t_en.trasKeyword = "XML:com.adobe.xmp";
        t_en.text = "<xml>Something</xml>";

        doTest(t_en);

        // check compression case
        t_en.isCompressed = true;
        t_en.description = "xml - en - compressed";

        doTest(t_en);

        ITXtTest t_ru = new ITXtTest();
        t_ru.description = "xml - ru";
        t_ru.keyword = "XML:com.adobe.xmp";
        t_ru.isCompressed = false;
        t_ru.compression = 0;
        t_ru.language = "ru";
        t_ru.trasKeyword = "\u0410\u0410\u0410\u0410\u0410 XML";
        t_ru.text = "<xml>\u042A\u042F\u042F\u042F\u042F\u042F\u042F</xml>";

        doTest(t_ru);

        t_ru.isCompressed = true;
        t_ru.description = "xml - ru - compressed";

        doTest(t_ru);
    }


    String description;

    String keyword;
    boolean isCompressed;
    int compression;
    String language;
    String trasKeyword;
    String text;


    public IIOMetadataNode getNode() {
        IIOMetadataNode iTXt = new IIOMetadataNode("iTXt");
        IIOMetadataNode iTXtEntry = new IIOMetadataNode("iTXtEntry");
        iTXtEntry.setAttribute("keyword", keyword);
        iTXtEntry.setAttribute("compressionFlag",
                               isCompressed ? "true" : "false");
        iTXtEntry.setAttribute("compressionMethod",
                               Integer.toString(compression));
        iTXtEntry.setAttribute("languageTag", language);
        iTXtEntry.setAttribute("translatedKeyword",
                               trasKeyword);
        iTXtEntry.setAttribute("text", text);
        iTXt.appendChild(iTXtEntry);
        return iTXt;
    }

    public static ITXtTest getFromNode(IIOMetadataNode n) {
        ITXtTest t = new ITXtTest();

        if (!"iTXt".equals(n.getNodeName())) {
            throw new RuntimeException("Invalid node");
        }
        IIOMetadataNode e = (IIOMetadataNode)n.getFirstChild();
        if (!"iTXtEntry".equals(e.getNodeName())) {
            throw new RuntimeException("Invalid entry node");
        }
        t.keyword = e.getAttribute("keyword");
        t.isCompressed =
            Boolean.valueOf(e.getAttribute("compressionFlag")).booleanValue();
        t.compression =
            Integer.valueOf(e.getAttribute("compressionMethod")).intValue();
        t.language = e.getAttribute("languageTag");
        t.trasKeyword = e.getAttribute("translatedKeyword");
        t.text = e.getAttribute("text");

        return t;
    }

    @Override
    public boolean equals(Object o) {
        if (! (o instanceof ITXtTest)) {
            return false;
        }
        ITXtTest t = (ITXtTest)o;
        if (!keyword.equals(t.keyword)) { return false; }
        if (isCompressed != t.isCompressed) { return false; }
        if (compression != t.compression) { return false; }
        if (!language.equals(t.language)) { return false; }
        if (!trasKeyword.equals(t.trasKeyword)) { return false; }
        if (!text.equals(t.text)) { return false; }

        return true;
    }



    private static void doTest(ITXtTest src) {

        System.out.println("Test: " + src.description);

        File file = new File("test.png");

        try {
            writeTo(file, src);
            ITXtTest dst = readFrom(file);
            if (dst == null || !dst.equals(src)) {
                throw new RuntimeException("Test failed.");
            }
        } finally {
            file.delete();
        }
        System.out.println("Test passed.");
    }

    private static void writeTo(File f, ITXtTest t) {
        BufferedImage src = createBufferedImage();
        try (ImageOutputStream imageOutputStream =
                ImageIO.createImageOutputStream(f)) {

            ImageTypeSpecifier imageTypeSpecifier =
                new ImageTypeSpecifier(src);
            ImageWriter imageWriter =
                ImageIO.getImageWritersByFormatName("PNG").next();

            imageWriter.setOutput(imageOutputStream);

            IIOMetadata m =
                imageWriter.getDefaultImageMetadata(imageTypeSpecifier, null);

            String format = m.getNativeMetadataFormatName();
            Node root = m.getAsTree(format);

            IIOMetadataNode iTXt = t.getNode();
            root.appendChild(iTXt);
            m.setFromTree(format, root);

            imageWriter.write(new IIOImage(src, null, m));
            System.out.println("Writing done.");
        } catch (Throwable e) {
            throw new RuntimeException("Writing test failed.", e);
        }
    }

    private static ITXtTest readFrom(File f) {
        try (ImageInputStream imageInputStream =
                ImageIO.createImageInputStream(f)) {

            ImageReader r = ImageIO.getImageReaders(imageInputStream).next();
            r.setInput(imageInputStream);

            IIOImage dst = r.readAll(0, null);

            // look for iTXt node
            IIOMetadata m = dst.getMetadata();
            Node root = m.getAsTree(m.getNativeMetadataFormatName());
            Node n = root.getFirstChild();
            while (n != null && !"iTXt".equals(n.getNodeName())) {
                n = n.getNextSibling();
            }
            if (n == null) {
                throw new RuntimeException("No iTXt node!");
            }
            ITXtTest t = ITXtTest.getFromNode((IIOMetadataNode)n);
            return t;
        } catch (Throwable e) {
            throw new RuntimeException("Reading test failed.", e);
        }
    }

    private static BufferedImage createBufferedImage() {
        BufferedImage image = new BufferedImage(128, 128,
                      BufferedImage.TYPE_4BYTE_ABGR_PRE);
        Graphics2D graph = image.createGraphics();
        graph.setPaintMode();
        graph.setColor(Color.orange);
        graph.fillRect(32, 32, 64, 64);
        graph.dispose();
        return image;
    }
}

