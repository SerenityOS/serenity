/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import j2dbench.Group;
import j2dbench.Option;
import j2dbench.Result;
import j2dbench.TestEnvironment;

/* This benchmark verifies how changes in cmm library affects image decoding */
public class EmbeddedProfileTests extends ColorConversionTests {

    protected static Group grpRoot;
    protected static Group grpOptionsRoot;

    protected static Option inputImages;

    public static void init() {
        grpRoot = new Group(colorConvRoot, "embed", "Embedded Profile Tests");

        grpOptionsRoot = new Group(grpRoot, "embedOptions", "Options");

        inputImages = createImageList();

        new ReadImageTest();
    }

    private static class IccImageResource {
        static IccImageResource SMALL = new IccImageResource("images/img_icc_small.jpg", "512x512", "Small: 512x512");
        static IccImageResource MEDIUM = new IccImageResource("images/img_icc_medium.jpg", "2048x2048", "Medium: 2048x2048");
        static IccImageResource LARGE = new IccImageResource("images/img_icc_large.jpg", "4096x4096", "Large: 4096x4096");

        private IccImageResource(String file, String name, String description) {
            this.url = CMMTests.class.getResource(file);
            this.abbrev = name;
            this.description = description;
        }

        public final URL url;
        public final String abbrev;
        public final String description;

        public static IccImageResource[] values() {
            return new IccImageResource[]{SMALL, MEDIUM, LARGE};
        }
    }

    private static Option createImageList() {
        IccImageResource[] images = IccImageResource.values();

        int num = images.length;

        String[] names = new String[num];
        String[] abbrev = new String[num];
        String[] descr = new String[num];

        for (int i = 0; i < num; i++) {
            names[i] = images[i].abbrev;
            abbrev[i] = images[i].abbrev;
            descr[i] = images[i].description;
        }

         Option list = new Option.ObjectList(grpOptionsRoot,
                "Images", "Input Images",
                names, images, abbrev, descr, 1);

         return list;
    }

    public EmbeddedProfileTests(Group parent, String nodeName, String description) {
        super(parent, nodeName, description);
        addDependencies(grpOptionsRoot, true);
    }

    private static class Context {
        URL input;

        public Context(TestEnvironment env, Result res) {

            IccImageResource icc_input = (IccImageResource)
                    env.getModifier(inputImages);

            input = icc_input.url;
        }
    }

     public Object initTest(TestEnvironment env, Result res) {
        return new Context(env, res);
    }

    public void cleanupTest(TestEnvironment env, Object o) {
        Context ctx = (Context)o;
        ctx.input = null;
    }

    private static class ReadImageTest extends EmbeddedProfileTests {
        public ReadImageTest() {
            super(grpRoot, "embd_img_read", "ImageReader.read()");
        }

        public void runTest(Object octx, int numReps) {
            final Context ctx = (Context)octx;
            final URL url = ctx.input;
            ImageInputStream iis = null;
            ImageReader reader = null;

            try {
                iis = ImageIO.createImageInputStream(url.openStream());
                reader = (ImageReader) ImageIO.getImageReaders(iis).next();
            } catch (IOException e) {
                throw new RuntimeException("Unable to run the benchmark", e);
            }

            do {
                try {
                    reader.setInput(iis);
                    BufferedImage img = reader.read(0);
                    reader.reset();

                    iis = ImageIO.createImageInputStream(url.openStream());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } while (--numReps >= 0);
        }
    }
}
