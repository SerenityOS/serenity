/*
 * Copyright (c) 2006, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageInputStreamSpi;
import javax.imageio.stream.FileCacheImageInputStream;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;

import j2dbench.Group;
import j2dbench.Option;
import j2dbench.Result;
import j2dbench.TestEnvironment;

abstract class InputTests extends IIOTests {

    protected static final int INPUT_FILE        = 1;
    protected static final int INPUT_URL         = 2;
    protected static final int INPUT_ARRAY       = 3;
    protected static final int INPUT_FILECHANNEL = 4;

    protected static ImageInputStreamSpi fileChannelIISSpi;
    static {
        if (hasImageIO) {
            ImageIO.scanForPlugins();
            IIORegistry registry = IIORegistry.getDefaultInstance();
            java.util.Iterator spis =
                registry.getServiceProviders(ImageInputStreamSpi.class, false);
            while (spis.hasNext()) {
                ImageInputStreamSpi spi = (ImageInputStreamSpi)spis.next();
                String klass = spi.getClass().getName();
                if (klass.endsWith("ChannelImageInputStreamSpi")) {
                    fileChannelIISSpi = spi;
                    break;
                }
            }
        }
    }

    protected static Group inputRoot;
    protected static Group inputOptRoot;

    protected static Group generalOptRoot;
    protected static Group.EnableSet generalSourceRoot;
    protected static Option sourceFileOpt;
    protected static Option sourceUrlOpt;
    protected static Option sourceByteArrayOpt;

    protected static Group imageioGeneralOptRoot;
    protected static Option sourceFileChannelOpt;
    protected static Option useCacheTog;

    public static void init() {
        inputRoot = new Group(iioRoot, "input", "Input Benchmarks");
        inputRoot.setTabbed();

        // Options
        inputOptRoot = new Group(inputRoot, "opts", "Options");

        // General Options
        generalOptRoot = new Group(inputOptRoot,
                                   "general", "General Options");
        generalSourceRoot = new Group.EnableSet(generalOptRoot,
                                                "source", "Sources");
        sourceFileOpt = new InputType("file", "File", INPUT_FILE);
        sourceUrlOpt = new InputType("url", "URL", INPUT_URL);
        sourceByteArrayOpt = new InputType("byteArray", "byte[]", INPUT_ARRAY);

        if (hasImageIO) {
            // Image I/O Options
            imageioGeneralOptRoot = new Group(inputOptRoot,
                                              "imageio", "Image I/O Options");
            if (fileChannelIISSpi != null) {
                sourceFileChannelOpt =
                    new InputType("fileChannel", "FileChannel",
                                  INPUT_FILECHANNEL);
            }
            useCacheTog = new Option.Toggle(imageioGeneralOptRoot, "useCache",
                                            "ImageIO.setUseCache()",
                                            Option.Toggle.Off);
        }

        InputImageTests.init();
        if (hasImageIO) {
            InputStreamTests.init();
        }
    }

    protected InputTests(Group parent, String nodeName, String description) {
        super(parent, nodeName, description);
    }

    protected static class InputType extends Option.Enable {
        private int type;

        public InputType(String nodeName, String description, int type) {
            super(generalSourceRoot, nodeName, description, false);
            this.type = type;
        }

        public int getType() {
            return type;
        }

        public String getAbbreviatedModifierDescription(Object value) {
            return getModifierValueName(value);
        }

        public String getModifierValueName(Object val) {
            return getNodeName();
        }
    }

    protected abstract static class Context {
        int size;
        Object input;
        int inputType;
        InputStream origStream;

        Context(TestEnvironment env, Result result) {
            size = env.getIntValue(sizeList);
            if (hasImageIO) {
                if (env.getModifier(useCacheTog) != null) {
                    ImageIO.setUseCache(env.isEnabled(useCacheTog));
                }
            }

            InputType t = (InputType)env.getModifier(generalSourceRoot);
            inputType = t.getType();
        }

        void initInput() {
            if ((inputType == INPUT_FILE) ||
                (inputType == INPUT_URL) ||
                (inputType == INPUT_FILECHANNEL))
            {
                try {
                    // REMIND: this approach will fail for GIF on pre-1.6 VM's
                    //         (since earlier releases do not include a
                    //         GIFImageWriter in the core JDK)
                    File inputfile = File.createTempFile("iio", ".tmp");
                    inputfile.deleteOnExit();
                    initContents(inputfile);
                    if (inputType == INPUT_FILE) {
                        input = inputfile;
                    } else if (inputType == INPUT_FILECHANNEL) {
                        input = inputfile;
                    } else { // inputType == INPUT_URL
                        try {
                            input = inputfile.toURI().toURL();
                        } catch (Exception e) {
                            System.err.println("error creating URL");
                        }
                    }
                } catch (IOException e) {
                    System.err.println("error creating image file");
                    e.printStackTrace();
                }
            } else {
                ByteArrayOutputStream out;
                try {
                    out = new ByteArrayOutputStream();
                    initContents(out);
                } catch (IOException e) {
                    System.err.println("error creating image array");
                    e.printStackTrace();
                    return;
                }
                input = out.toByteArray();
            }
        }

        abstract void initContents(File f) throws IOException;
        abstract void initContents(OutputStream out) throws IOException;

        ImageInputStream createImageInputStream() throws IOException {
            ImageInputStream iis;
            BufferedInputStream bis;
            switch (inputType) {
            case INPUT_FILE:
                iis = new FileImageInputStream((File)input);
                break;
            case INPUT_URL:
                origStream = ((URL)input).openStream();
                bis = new BufferedInputStream(origStream);
                if (ImageIO.getUseCache()) {
                    iis = new FileCacheImageInputStream(bis, null);
                } else {
                    iis = new MemoryCacheImageInputStream(bis);
                }
                break;
            case INPUT_ARRAY:
                origStream = new ByteArrayInputStream((byte[])input);
                bis = new BufferedInputStream(origStream);
                if (ImageIO.getUseCache()) {
                    iis = new FileCacheImageInputStream(bis, null);
                } else {
                    iis = new MemoryCacheImageInputStream(bis);
                }
                break;
            case INPUT_FILECHANNEL:
                FileInputStream fis = new FileInputStream((File)input);
                origStream = fis;
                java.nio.channels.FileChannel fc = fis.getChannel();
                iis = fileChannelIISSpi.createInputStreamInstance(fc, false,
                                                                  null);
                break;
            default:
                iis = null;
                break;
            }
            return iis;
        }

        void closeOriginalStream() throws IOException {
            if (origStream != null) {
                origStream.close();
                origStream = null;
            }
        }

        void cleanup(TestEnvironment env) {
        }
    }
}
