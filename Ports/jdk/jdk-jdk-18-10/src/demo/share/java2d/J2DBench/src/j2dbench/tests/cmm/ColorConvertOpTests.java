/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * This source code is provided to illustrate the usage of a given feature
 * or technique and has been deliberately simplified. Additional steps
 * required for a production-quality application, such as security checks,
 * input validation and proper error handling, might not be present in
 * this sample code.
 */

package j2dbench.tests.cmm;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

import javax.imageio.ImageIO;

import j2dbench.Group;
import j2dbench.Option;
import j2dbench.Result;
import j2dbench.TestEnvironment;
import j2dbench.tests.iio.IIOTests;

public class ColorConvertOpTests extends ColorConversionTests {

    private static class ImageContent {
        static ImageContent BLANK = new ImageContent("blank", "Blank (opaque black)");
        static ImageContent RANDOM = new ImageContent("random", "Random");
        static ImageContent VECTOR = new ImageContent("vector", "Vector Art");
        static ImageContent PHOTO= new ImageContent("photo", "Photograph");

        public final String name;
        public final String descr;

        private ImageContent(String name, String descr) {
            this.name = name;
            this.descr = descr;
        }

        public static ImageContent[] values() {
            return new ImageContent[]{BLANK, RANDOM, VECTOR, PHOTO};
        }
    }

    private static class ImageType {
        static ImageType INT_ARGB = new ImageType(BufferedImage.TYPE_INT_ARGB, "INT_ARGB", "TYPE_INT_ARGB");
        static ImageType INT_RGB = new ImageType(BufferedImage.TYPE_INT_RGB, "INT_RGB", "TYPE_INT_RGB");
        static ImageType INT_BGR = new ImageType(BufferedImage.TYPE_INT_BGR, "INT_BGR", "TYPE_INT_BGR");
        static ImageType BYTE_3BYTE_BGR = new ImageType(BufferedImage.TYPE_3BYTE_BGR, "3BYTE_BGR", "TYPE_3BYTE_BGR");
        static ImageType BYTE_4BYTE_ABGR = new ImageType(BufferedImage.TYPE_4BYTE_ABGR, "4BYTE_ABGR", "TYPE_4BYTE_ABGR");
        static ImageType COMPATIBLE_DST = new ImageType(0, "Compatible", "Compatible destination");

        private ImageType(int type, String abbr, String descr) {
            this.type = type;
            this.abbrev = abbr;
            this.descr = descr;
        }

        public final int type;
        public final String abbrev;
        public final String descr;

        public static ImageType[] values() {
            return new ImageType[]{INT_ARGB, INT_RGB, INT_BGR,
                    BYTE_3BYTE_BGR, BYTE_4BYTE_ABGR, COMPATIBLE_DST};
        }
    }

    private static class ListType {
        static ListType SRC = new ListType("srcType", "Source Images");
        static ListType DST = new ListType("dstType", "Destination Images");

        private ListType(String name, String description) {
            this.name = name;
            this.description = description;
        }
        public final String name;
        public final String description;
    }

    public static Option createImageTypeList(ListType listType) {

        ImageType[] allTypes = ImageType.values();

        int num = allTypes.length;
        if (listType == ListType.SRC) {
            num -= 1; // exclude compatible destination
        }

        ImageType[] t = new ImageType[num];
        String[] names = new String[num];
        String[] abbrev = new String[num];
        String[] descr = new String[num];

        for (int i = 0; i < num; i++) {
            t[i] = allTypes[i];
            names[i] = t[i].abbrev;
            abbrev[i] = t[i].abbrev;
            descr[i] = t[i].descr;
        }

        Option list = new Option.ObjectList(opOptionsRoot,
                listType.name, listType.description,
                names, t, abbrev, descr, 1);
        return list;
    }

    protected static Group opConvRoot;

    protected static Group opOptionsRoot;
    protected static Option sizeList;
    protected static Option contentList;

    protected static Option sourceType;

    protected static Option destinationType;

    public static void init() {
        opConvRoot = new Group(colorConvRoot, "ccop", "ColorConvertOp Tests");

        opOptionsRoot = new Group(opConvRoot, "ccopOptions", "Options");

        // size list
        int[] sizes = new int[] {1, 20, 250, 1000, 4000};
        String[] sizeStrs = new String[] {
            "1x1", "20x20", "250x250", "1000x1000", "4000x4000"
        };
        String[] sizeDescs = new String[] {
            "Tiny Images (1x1)",
            "Small Images (20x20)",
            "Medium Images (250x250)",
            "Large Images (1000x1000)",
            "Huge Images (4000x4000)",
        };
        sizeList = new Option.IntList(opOptionsRoot,
                                      "size", "Image Size",
                                      sizes, sizeStrs, sizeDescs, 0x4);
        ((Option.ObjectList) sizeList).setNumRows(5);

        // image content
        ImageContent[] c = ImageContent.values();

        String[] contentStrs = new String[c.length];
        String[] contentDescs = new String[c.length];

        for (int i = 0; i < c.length; i++) {
            contentStrs[i] = c[i].name;
            contentDescs[i] = c[i].descr;
        };

        contentList = new Option.ObjectList(opOptionsRoot,
                                            "content", "Image Content",
                                            contentStrs, c,
                                            contentStrs, contentDescs,
                                            0x8);

        sourceType = createImageTypeList(ListType.SRC);

        destinationType = createImageTypeList(ListType.DST);

        new ConvertImageTest();
        new ConvertRasterTest();
        new DrawImageTest();
    }

    public ColorConvertOpTests(Group parent, String nodeName, String description) {
        super(parent, nodeName, description);
        addDependencies(opOptionsRoot, true);
    }

    public Object initTest(TestEnvironment env, Result res) {
        return new Context(env, res);
    }

    public void cleanupTest(TestEnvironment env, Object o) {
        Context ctx = (Context)o;
        ctx.cs = null;
        ctx.op_img = null;
        ctx.op_rst = null;
        ctx.dst = null;
        ctx.src = null;
        ctx.graphics = null;
    }

    private static class Context {
        ColorSpace cs;
        Graphics2D graphics;
        ColorConvertOp op_img;
        ColorConvertOp op_rst;

        BufferedImage src;
        BufferedImage dst;

        WritableRaster rsrc;
        WritableRaster rdst;

        public Context(TestEnvironment env, Result res) {

            graphics = (Graphics2D)env.getGraphics();
            cs = getColorSpace(env);

            // TODO: provide rendering hints
            op_img = new ColorConvertOp(cs, null);
            ColorSpace sRGB = ColorSpace.getInstance(ColorSpace.CS_sRGB);
            op_rst = new ColorConvertOp(sRGB, cs, null);

            int size = env.getIntValue(sizeList);

            ImageContent content = (ImageContent)env.getModifier(contentList);
            ImageType srcType = (ImageType)env.getModifier(sourceType);

            src = createBufferedImage(size, size, content, srcType.type);
            rsrc = src.getRaster();

            ImageType dstType = (ImageType)env.getModifier(destinationType);
            if (dstType == ImageType.COMPATIBLE_DST) {
                dst = op_img.createCompatibleDestImage(src, null);
            } else {
                dst = createBufferedImage(size, size, content, dstType.type);
            }
            // raster always has to be comatible
            rdst = op_rst.createCompatibleDestRaster(rsrc);
        }
    }

    private static class ConvertImageTest extends ColorConvertOpTests {
        public ConvertImageTest() {
            super(opConvRoot, "op_img", "op.filetr(BufferedImage)");
        }

        public void runTest(Object octx, int numReps) {
            final Context ctx = (Context)octx;
            final ColorConvertOp op = ctx.op_img;

            final BufferedImage src = ctx.src;
            BufferedImage dst = ctx.dst;
            do {
                try {
                    dst = op.filter(src, dst);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } while (--numReps >= 0);
        }
    }

    private static class ConvertRasterTest extends ColorConvertOpTests {
        public ConvertRasterTest() {
            super(opConvRoot, "op_rst", "op.filetr(Raster)");
        }

        public void runTest(Object octx, int numReps) {
            final Context ctx = (Context)octx;
            final ColorConvertOp op = ctx.op_rst;

            final Raster src = ctx.rsrc;
            WritableRaster dst = ctx.rdst;
            do {
                try {
                    dst = op.filter(src, dst);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } while (--numReps >= 0);
        }
    }

    private static class DrawImageTest extends ColorConvertOpTests {
        public DrawImageTest() {
            super(opConvRoot, "op_draw", "drawImage(ColorConvertOp)");
        }

        public void runTest(Object octx, int numReps) {
            final Context ctx = (Context)octx;
            final ColorConvertOp op = ctx.op_img;

            final Graphics2D g = ctx.graphics;

            final BufferedImage src = ctx.src;

            do {
                g.drawImage(src, op, 0, 0);
            } while (--numReps >= 0);
        }
    }

    /**************************************************************************
     ******                    Helper routines
     *************************************************************************/
    protected static BufferedImage createBufferedImage(int width,
                                                       int height,
                                                       ImageContent contentType,
                                                       int type)
    {
        BufferedImage image;
        image = new BufferedImage(width, height, type);
        boolean hasAlpha = image.getColorModel().hasAlpha();
        if (contentType == ImageContent.RANDOM) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int rgb = (int) (Math.random() * 0xffffff);
                    if (hasAlpha) {
                        rgb |= 0x7f000000;
                    }
                    image.setRGB(x, y, rgb);
                }
            }
        }
        if (contentType == ImageContent.VECTOR) {
            Graphics2D g = image.createGraphics();
            if (hasAlpha) {
                // fill background with a translucent color
                g.setComposite(AlphaComposite.getInstance(
                                   AlphaComposite.SRC, 0.5f));
            }
            g.setColor(Color.blue);
            g.fillRect(0, 0, width, height);
            g.setComposite(AlphaComposite.Src);
            g.setColor(Color.yellow);
            g.fillOval(2, 2, width-4, height-4);
            g.setColor(Color.red);
            g.fillOval(4, 4, width-8, height-8);
            g.setColor(Color.green);
            g.fillRect(8, 8, width-16, height-16);
            g.setColor(Color.white);
            g.drawLine(0, 0, width, height);
            g.drawLine(0, height, width, 0);
            g.dispose();
        }
        if (contentType == ImageContent.PHOTO) {
            Image photo = null;
            try {
                photo = ImageIO.read(
                    IIOTests.class.getResourceAsStream("images/photo.jpg"));
            } catch (Exception e) {
                System.err.println("error loading photo");
                e.printStackTrace();
            }
            Graphics2D g = image.createGraphics();
            if (hasAlpha) {
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC,
                                                          0.5f));
            }
            g.drawImage(photo, 0, 0, width, height, null);
            g.dispose();
        }
        return image;
    }
}
