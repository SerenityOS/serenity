/*
 * Copyright (c) 2006, 2020, Oracle and/or its affiliates. All rights reserved.
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


package j2dbench.tests.iio;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.event.IIOReadProgressListener;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

import j2dbench.Group;
import j2dbench.Modifier;
import j2dbench.Option;
import j2dbench.Result;
import j2dbench.Test;
import j2dbench.TestEnvironment;

abstract class InputImageTests extends InputTests {

    private static final int TEST_TOOLKIT     = 1;
    private static final int TEST_IMAGEIO     = 2;
    private static final int TEST_IMAGEREADER = 3;

    private static Group imageRoot;

    private static Group toolkitRoot;
    private static Group toolkitOptRoot;
    private static Option toolkitReadFormatList;
    private static Group toolkitTestRoot;

    private static Group imageioRoot;
    private static Group imageioOptRoot;
    private static ImageReaderSpi[] imageioReaderSpis;
    private static String[] imageioReadFormatShortNames;
    private static Option imageioReadFormatList;
    private static Group imageioTestRoot;

    private static Group imageReaderRoot;
    private static Group imageReaderOptRoot;
    private static Option seekForwardOnlyTog;
    private static Option ignoreMetadataTog;
    private static Option installListenerTog;
    private static Group imageReaderTestRoot;

    public static void init() {
        imageRoot = new Group(inputRoot, "image", "Image Reading Benchmarks");
        imageRoot.setTabbed();

        // Toolkit Benchmarks
        toolkitRoot = new Group(imageRoot, "toolkit", "Toolkit");

        toolkitOptRoot = new Group(toolkitRoot, "opts", "Toolkit Options");
        String[] tkFormats = new String[] {"gif", "jpg", "png"};
        toolkitReadFormatList =
            new Option.ObjectList(toolkitOptRoot,
                                  "format", "Image Format",
                                  tkFormats, tkFormats,
                                  tkFormats, tkFormats,
                                  0x0);

        toolkitTestRoot = new Group(toolkitRoot, "tests", "Toolkit Tests");
        new ToolkitCreateImage();

        // Image I/O Benchmarks
        if (hasImageIO) {
            imageioRoot = new Group(imageRoot, "imageio", "Image I/O");

            // Image I/O Options
            imageioOptRoot = new Group(imageioRoot, "opts",
                                       "Image I/O Options");
            initIIOReadFormats();
            imageioReadFormatList =
                new Option.ObjectList(imageioOptRoot,
                                      "format", "Image Format",
                                      imageioReadFormatShortNames,
                                      imageioReaderSpis,
                                      imageioReadFormatShortNames,
                                      imageioReadFormatShortNames,
                                      0x0);

            // Image I/O Tests
            imageioTestRoot = new Group(imageioRoot, "tests",
                                        "Image I/O Tests");
            new ImageIORead();

            // ImageReader Options
            imageReaderRoot = new Group(imageioRoot, "reader",
                                        "ImageReader Benchmarks");
            imageReaderOptRoot = new Group(imageReaderRoot, "opts",
                                           "ImageReader Options");
            seekForwardOnlyTog =
                new Option.Toggle(imageReaderOptRoot,
                                  "seekForwardOnly",
                                  "Seek Forward Only",
                                  Option.Toggle.On);
            ignoreMetadataTog =
                new Option.Toggle(imageReaderOptRoot,
                                  "ignoreMetadata",
                                  "Ignore Metadata",
                                  Option.Toggle.On);
            installListenerTog =
                new Option.Toggle(imageReaderOptRoot,
                                  "installListener",
                                  "Install Progress Listener",
                                  Option.Toggle.Off);

            // ImageReader Tests
            imageReaderTestRoot = new Group(imageReaderRoot, "tests",
                                            "ImageReader Tests");
            new ImageReaderRead();
            new ImageReaderGetImageMetadata();
        }
    }

    private static void initIIOReadFormats() {
        List spis = new ArrayList();
        List shortNames = new ArrayList();

        ImageIO.scanForPlugins();
        IIORegistry registry = IIORegistry.getDefaultInstance();
        java.util.Iterator readerspis =
            registry.getServiceProviders(ImageReaderSpi.class, false);
        while (readerspis.hasNext()) {
            // REMIND: there could be more than one non-core plugin for
            // a particular format, as is the case for JPEG2000 in the JAI
            // IIO Tools package, so we should support that somehow
            ImageReaderSpi spi = (ImageReaderSpi)readerspis.next();
            String klass = spi.getClass().getName();
            String format = spi.getFormatNames()[0].toLowerCase();
            String suffix = spi.getFileSuffixes()[0].toLowerCase();
            if (suffix == null || suffix.equals("")) {
                suffix = format;
            }
            String shortName;
            if (klass.startsWith("com.sun.imageio.plugins")) {
                shortName = "core-" + suffix;
            } else {
                shortName = "ext-" + suffix;
            }
            spis.add(spi);
            shortNames.add(shortName);
        }

        imageioReaderSpis = new ImageReaderSpi[spis.size()];
        imageioReaderSpis = (ImageReaderSpi[])spis.toArray(imageioReaderSpis);
        imageioReadFormatShortNames = new String[shortNames.size()];
        imageioReadFormatShortNames =
            (String[])shortNames.toArray(imageioReadFormatShortNames);
    }

    protected InputImageTests(Group parent,
                              String nodeName, String description)
    {
        super(parent, nodeName, description);
    }

    public void cleanupTest(TestEnvironment env, Object ctx) {
        Context iioctx = (Context)ctx;
        iioctx.cleanup(env);
    }

    private static class Context extends InputTests.Context {
        String format;
        BufferedImage image;
        ImageReader reader;
        boolean seekForwardOnly;
        boolean ignoreMetadata;

        Context(TestEnvironment env, Result result, int testType) {
            super(env, result);

            String content = (String)env.getModifier(contentList);
            if (content == null) {
                content = CONTENT_BLANK;
            }
            // REMIND: add option for non-opaque images
            image = createBufferedImage(size, size, content, false);

            result.setUnits(size*size);
            result.setUnitName("pixel");

            if (testType == TEST_IMAGEIO || testType == TEST_IMAGEREADER) {
                ImageReaderSpi readerspi =
                    (ImageReaderSpi)env.getModifier(imageioReadFormatList);
                format = readerspi.getFileSuffixes()[0].toLowerCase();
                if (testType == TEST_IMAGEREADER) {
                    seekForwardOnly = env.isEnabled(seekForwardOnlyTog);
                    ignoreMetadata = env.isEnabled(ignoreMetadataTog);
                    try {
                        reader = readerspi.createReaderInstance();
                    } catch (IOException e) {
                        System.err.println("error creating reader");
                        e.printStackTrace();
                    }
                    if (env.isEnabled(installListenerTog)) {
                        reader.addIIOReadProgressListener(
                            new ReadProgressListener());
                    }
                }
                if (format.equals("wbmp")) {
                    // REMIND: this is a hack to create an image that the
                    //         WBMPImageWriter can handle (a better approach
                    //         would involve checking the ImageTypeSpecifier
                    //         of the writer's default image param)
                    BufferedImage newimg =
                        new BufferedImage(size, size,
                                          BufferedImage.TYPE_BYTE_BINARY);
                    Graphics g = newimg.createGraphics();
                    g.drawImage(image, 0, 0, null);
                    g.dispose();
                    image = newimg;
                }
            } else if (testType == TEST_TOOLKIT) {
                format = (String)env.getModifier(toolkitReadFormatList);
            } else { // testType == TEST_JPEGCODEC
                format = "jpeg";
            }

            initInput();
        }

        void initContents(File f) throws IOException {
            ImageIO.write(image, format, f);
        }

        void initContents(OutputStream out) throws IOException {
            ImageIO.write(image, format, out);
        }

        void cleanup(TestEnvironment env) {
            super.cleanup(env);
            if (reader != null) {
                reader.dispose();
                reader = null;
            }
        }
    }

    private static class ToolkitCreateImage extends InputImageTests {
        private static final Component canvas = new Component() {};

        public ToolkitCreateImage() {
            super(toolkitTestRoot,
                  "createImage",
                  "Toolkit.createImage()");
            addDependency(generalSourceRoot,
                new Modifier.Filter() {
                    public boolean isCompatible(Object val) {
                        // Toolkit handles FILE, URL, and ARRAY, but
                        // not FILECHANNEL
                        InputType t = (InputType)val;
                        return (t.getType() != INPUT_FILECHANNEL);
                    }
                });
            addDependencies(toolkitOptRoot, true);
        }

        public Object initTest(TestEnvironment env, Result result) {
            return new Context(env, result, TEST_TOOLKIT);
        }

        public void runTest(Object ctx, int numReps) {
            final Context ictx = (Context)ctx;
            final Object input = ictx.input;
            final int inputType = ictx.inputType;
            final Toolkit tk = Toolkit.getDefaultToolkit();
            final MediaTracker mt = new MediaTracker(canvas);
            switch (inputType) {
            case INPUT_FILE:
                String filename = ((File)input).getAbsolutePath();
                do {
                    try {
                        Image img = tk.createImage(filename);
                        mt.addImage(img, 0);
                        mt.waitForID(0, 0);
                        mt.removeImage(img, 0);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } while (--numReps >= 0);
                break;
            case INPUT_URL:
                do {
                    try {
                        Image img = tk.createImage((URL)input);
                        mt.addImage(img, 0);
                        mt.waitForID(0, 0);
                        mt.removeImage(img, 0);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } while (--numReps >= 0);
                break;
            case INPUT_ARRAY:
                do {
                    try {
                        Image img = tk.createImage((byte[])input);
                        mt.addImage(img, 0);
                        mt.waitForID(0, 0);
                        mt.removeImage(img, 0);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } while (--numReps >= 0);
                break;
            default:
                throw new IllegalArgumentException("Invalid input type");
            }
        }
    }

    private static class ImageIORead extends InputImageTests {
        public ImageIORead() {
            super(imageioTestRoot,
                  "imageioRead",
                  "ImageIO.read()");
            addDependency(generalSourceRoot,
                new Modifier.Filter() {
                    public boolean isCompatible(Object val) {
                        // ImageIO.read() handles FILE, URL, and ARRAY, but
                        // not FILECHANNEL (well, I suppose we could create
                        // an ImageInputStream from a FileChannel source,
                        // but that's not a common use case; FileChannel is
                        // better handled by the ImageReader tests below)
                        InputType t = (InputType)val;
                        return (t.getType() != INPUT_FILECHANNEL);
                    }
                });
            addDependencies(imageioOptRoot, true);
        }

        public Object initTest(TestEnvironment env, Result result) {
            return new Context(env, result, TEST_IMAGEIO);
        }

        public void runTest(Object ctx, int numReps) {
            final Context ictx = (Context)ctx;
            final Object input = ictx.input;
            final int inputType = ictx.inputType;
            switch (inputType) {
            case INPUT_FILE:
                do {
                    try {
                        ImageIO.read((File)input);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } while (--numReps >= 0);
                break;
            case INPUT_URL:
                do {
                    try {
                        ImageIO.read((URL)input);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } while (--numReps >= 0);
                break;
            case INPUT_ARRAY:
                do {
                    try {
                        ByteArrayInputStream bais =
                            new ByteArrayInputStream((byte[])input);
                        BufferedInputStream bis =
                            new BufferedInputStream(bais);
                        ImageIO.read(bis);
                        bais.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } while (--numReps >= 0);
                break;
            default:
                throw new IllegalArgumentException("Invalid input type");
            }
        }
    }

    private static class ImageReaderRead extends InputImageTests {
        public ImageReaderRead() {
            super(imageReaderTestRoot,
                  "read",
                  "ImageReader.read()");
            addDependency(generalSourceRoot);
            addDependencies(imageioGeneralOptRoot, true);
            addDependencies(imageioOptRoot, true);
            addDependencies(imageReaderOptRoot, true);
        }

        public Object initTest(TestEnvironment env, Result result) {
            return new Context(env, result, TEST_IMAGEREADER);
        }

        public void runTest(Object ctx, int numReps) {
            final Context ictx = (Context)ctx;
            final ImageReader reader = ictx.reader;
            final boolean seekForwardOnly = ictx.seekForwardOnly;
            final boolean ignoreMetadata = ictx.ignoreMetadata;
            do {
                try {
                    ImageInputStream iis = ictx.createImageInputStream();
                    reader.setInput(iis, seekForwardOnly, ignoreMetadata);
                    reader.read(0);
                    reader.reset();
                    iis.close();
                    ictx.closeOriginalStream();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } while (--numReps >= 0);
        }
    }

    private static class ImageReaderGetImageMetadata extends InputImageTests {
        public ImageReaderGetImageMetadata() {
            super(imageReaderTestRoot,
                  "getImageMetadata",
                  "ImageReader.getImageMetadata()");
            addDependency(generalSourceRoot);
            addDependencies(imageioGeneralOptRoot, true);
            addDependencies(imageioOptRoot, true);
            addDependencies(imageReaderOptRoot, true);
        }

        public Object initTest(TestEnvironment env, Result result) {
            Context ctx = new Context(env, result, TEST_IMAGEREADER);
            // override units since this test doesn't read "pixels"
            result.setUnits(1);
            result.setUnitName("image");
            return ctx;
        }

        public void runTest(Object ctx, int numReps) {
            final Context ictx = (Context)ctx;
            final ImageReader reader = ictx.reader;
            final boolean seekForwardOnly = ictx.seekForwardOnly;
            final boolean ignoreMetadata = ictx.ignoreMetadata;
            do {
                try {
                    ImageInputStream iis = ictx.createImageInputStream();
                    reader.setInput(iis, seekForwardOnly, ignoreMetadata);
                    reader.getImageMetadata(0);
                    reader.reset();
                    iis.close();
                    ictx.closeOriginalStream();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } while (--numReps >= 0);
        }
    }

    private static class ReadProgressListener
        implements IIOReadProgressListener
    {
        public void sequenceStarted(ImageReader source, int minIndex) {}
        public void sequenceComplete(ImageReader source) {}
        public void imageStarted(ImageReader source, int imageIndex) {}
        public void imageProgress(ImageReader source, float percentageDone) {}
        public void imageComplete(ImageReader source) {}
        public void thumbnailStarted(ImageReader source,
                                     int imageIndex, int thumbnailIndex) {}
        public void thumbnailProgress(ImageReader source,
                                      float percentageDone) {}
        public void thumbnailComplete(ImageReader source) {}
        public void readAborted(ImageReader source) {}
    }
}
