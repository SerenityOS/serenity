/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6541476 6782079
 * @summary Write and read a PNG file including an non-latin1 iTXt chunk
 *          Test also verifies that trunkated png images does not cause
 *          an OoutOfMemory error.
 *
 * @run main ItxtUtf8Test
 *
 * @run main/othervm/timeout=10 -Xmx6m ItxtUtf8Test truncate
 */

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import javax.imageio.IIOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

public class ItxtUtf8Test {

    public static final String
    TEXT = "\u24c9\u24d4\u24e7\u24e3" +
      "\ud835\udc13\ud835\udc1e\ud835\udc31\ud835\udc2d" +
      "\u24c9\u24d4\u24e7\u24e3", // a repetition for compression
    VERBATIM = "\u24e5\u24d4\u24e1\u24d1\u24d0\u24e3\u24d8\u24dc",
    COMPRESSED = "\u24d2\u24de\u24dc\u24df\u24e1\u24d4\u24e2\u24e2\u24d4\u24d3";

    public static final byte[]
    VBYTES = {
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x56, // chunk length
        (byte)0x69, (byte)0x54, (byte)0x58, (byte)0x74, // chunk type "iTXt"
        (byte)0x76, (byte)0x65, (byte)0x72, (byte)0x62,
        (byte)0x61, (byte)0x74, (byte)0x69, (byte)0x6d, // keyword "verbatim"
        (byte)0x00, // separator terminating keyword
        (byte)0x00, // compression flag
        (byte)0x00, // compression method, must be zero
        (byte)0x78, (byte)0x2d, (byte)0x63, (byte)0x69,
        (byte)0x72, (byte)0x63, (byte)0x6c, (byte)0x65,
        (byte)0x64, // language tag "x-circled"
        (byte)0x00, // separator terminating language tag
        (byte)0xe2, (byte)0x93, (byte)0xa5, // '\u24e5'
        (byte)0xe2, (byte)0x93, (byte)0x94, // '\u24d4'
        (byte)0xe2, (byte)0x93, (byte)0xa1, // '\u24e1'
        (byte)0xe2, (byte)0x93, (byte)0x91, // '\u24d1'
        (byte)0xe2, (byte)0x93, (byte)0x90, // '\u24d0'
        (byte)0xe2, (byte)0x93, (byte)0xa3, // '\u24e3'
        (byte)0xe2, (byte)0x93, (byte)0x98, // '\u24d8'
        (byte)0xe2, (byte)0x93, (byte)0x9c, // '\u24dc'
        (byte)0x00, // separator terminating the translated keyword
        (byte)0xe2, (byte)0x93, (byte)0x89, // '\u24c9'
        (byte)0xe2, (byte)0x93, (byte)0x94, // '\u24d4'
        (byte)0xe2, (byte)0x93, (byte)0xa7, // '\u24e7'
        (byte)0xe2, (byte)0x93, (byte)0xa3, // '\u24e3'
        (byte)0xf0, (byte)0x9d, (byte)0x90, (byte)0x93, // '\ud835\udc13'
        (byte)0xf0, (byte)0x9d, (byte)0x90, (byte)0x9e, // '\ud835\udc1e'
        (byte)0xf0, (byte)0x9d, (byte)0x90, (byte)0xb1, // '\ud835\udc31'
        (byte)0xf0, (byte)0x9d, (byte)0x90, (byte)0xad, // '\ud835\udc2d'
        (byte)0xe2, (byte)0x93, (byte)0x89, // '\u24c9'
        (byte)0xe2, (byte)0x93, (byte)0x94, // '\u24d4'
        (byte)0xe2, (byte)0x93, (byte)0xa7, // '\u24e7'
        (byte)0xe2, (byte)0x93, (byte)0xa3, // '\u24e3'
        (byte)0xb5, (byte)0xcc, (byte)0x97, (byte)0x56 // CRC
    },
    CBYTES = {
        // we don't want to check the chunk length,
        // as this might depend on implementation.
        (byte)0x69, (byte)0x54, (byte)0x58, (byte)0x74, // chunk type "iTXt"
        (byte)0x63, (byte)0x6f, (byte)0x6d, (byte)0x70,
        (byte)0x72, (byte)0x65, (byte)0x73, (byte)0x73,
        (byte)0x65, (byte)0x64, // keyword "compressed"
        (byte)0x00, // separator terminating keyword
        (byte)0x01, // compression flag
        (byte)0x00, // compression method, 0=deflate
        (byte)0x78, (byte)0x2d, (byte)0x63, (byte)0x69,
        (byte)0x72, (byte)0x63, (byte)0x6c, (byte)0x65,
        (byte)0x64, // language tag "x-circled"
        (byte)0x00, // separator terminating language tag
        // we don't want to check the actual compressed data,
        // as this might depend on implementation.
    };
/*
*/

    public static void main(String[] args) throws Exception {
        List argList = Arrays.asList(args);
        if (argList.contains("truncate")) {
            try {
                runTest(false, true);
                throw new AssertionError("Expect an error for truncated file");
            }
            catch (IIOException e) {
                // expected an error for a truncated image file.
            }
        }
        else {
            runTest(argList.contains("dump"), false);
        }
    }

    public static void runTest(boolean dump, boolean truncate)
        throws Exception
    {
        String format = "javax_imageio_png_1.0";
        BufferedImage img =
            new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        ImageWriter iw = ImageIO.getImageWritersByMIMEType("image/png").next();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageOutputStream ios = new MemoryCacheImageOutputStream(os);
        iw.setOutput(ios);
        IIOMetadata meta =
            iw.getDefaultImageMetadata(new ImageTypeSpecifier(img), null);
        DOMImplementationRegistry registry;
        registry = DOMImplementationRegistry.newInstance();
        DOMImplementation impl = registry.getDOMImplementation("XML 3.0");
        Document doc = impl.createDocument(null, format, null);
        Element root, itxt, entry;
        root = doc.getDocumentElement();
        root.appendChild(itxt = doc.createElement("iTXt"));
        itxt.appendChild(entry = doc.createElement("iTXtEntry"));
        entry.setAttribute("keyword", "verbatim");
        entry.setAttribute("compressionFlag", "false");
        entry.setAttribute("compressionMethod", "0");
        entry.setAttribute("languageTag", "x-circled");
        entry.setAttribute("translatedKeyword", VERBATIM);
        entry.setAttribute("text", TEXT);
        itxt.appendChild(entry = doc.createElement("iTXtEntry"));
        entry.setAttribute("keyword", "compressed");
        entry.setAttribute("compressionFlag", "true");
        entry.setAttribute("compressionMethod", "0");
        entry.setAttribute("languageTag", "x-circled");
        entry.setAttribute("translatedKeyword", COMPRESSED);
        entry.setAttribute("text", TEXT);
        meta.mergeTree(format, root);
        iw.write(new IIOImage(img, null, meta));
        iw.dispose();

        byte[] bytes = os.toByteArray();
        if (dump)
            System.out.write(bytes);
        if (findBytes(VBYTES, bytes) < 0)
            throw new AssertionError("verbatim block not found");
        if (findBytes(CBYTES, bytes) < 0)
            throw new AssertionError("compressed block not found");
        int length = bytes.length;
        if (truncate)
            length = findBytes(VBYTES, bytes) + 32;

        ImageReader ir = ImageIO.getImageReader(iw);
        ByteArrayInputStream is = new ByteArrayInputStream(bytes, 0, length);
        ImageInputStream iis = new MemoryCacheImageInputStream(is);
        ir.setInput(iis);
        meta = ir.getImageMetadata(0);
        Node node = meta.getAsTree(format);
        for (node = node.getFirstChild();
             !"iTXt".equals(node.getNodeName());
             node = node.getNextSibling());
        boolean verbatimSeen = false, compressedSeen = false;
        for (node = node.getFirstChild();
             node != null;
             node = node.getNextSibling()) {
            entry = (Element)node;
            String keyword = entry.getAttribute("keyword");
            String translatedKeyword = entry.getAttribute("translatedKeyword");
            String text = entry.getAttribute("text");
            if ("verbatim".equals(keyword)) {
                if (verbatimSeen) throw new AssertionError("Duplicate");
                verbatimSeen = true;
                if (!VERBATIM.equals(translatedKeyword))
                    throw new AssertionError("Wrong translated keyword");
                if (!TEXT.equals(text))
                    throw new AssertionError("Wrong text");
            }
            else if ("compressed".equals(keyword)) {
                if (compressedSeen) throw new AssertionError("Duplicate");
                compressedSeen = true;
                if (!COMPRESSED.equals(translatedKeyword))
                    throw new AssertionError("Wrong translated keyword");
                if (!TEXT.equals(text))
                    throw new AssertionError("Wrong text");
            }
            else {
                throw new AssertionError("Unexpected keyword");
            }
        }
        if (!(verbatimSeen && compressedSeen))
            throw new AssertionError("Missing chunk");
    }

    private static final int findBytes(byte[] needle, byte[] haystack) {
        HAYSTACK: for (int h = 0; h <= haystack.length - needle.length; ++h) {
            for (int n = 0; n < needle.length; ++n) {
                if (needle[n] != haystack[h + n]) {
                    continue HAYSTACK;
                }
            }
            return h;
        }
        return -1;
    }

}
