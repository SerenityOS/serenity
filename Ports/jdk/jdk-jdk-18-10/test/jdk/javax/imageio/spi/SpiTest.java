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
 * @bug 4395376
 * @summary Performs various sanity checks on Spi class constructors and get
 *          methods
 */

import java.util.Iterator;
import java.util.Locale;

import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.IIOServiceProvider;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ImageReaderWriterSpi;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.spi.ServiceRegistry;

public class SpiTest {

    String vendorName = null;
    String version = null;
    String[] names = null;
    String[] suffixes = null;
    String[] MIMETypes = null;
    String readerClassName = null;
    String writerClassName = null;
    Class[] inputTypes = null;
    Class[] outputTypes = null;
    String[] writerSpiNames = null;
    String[] readerSpiNames = null;
    String nativeStreamMetadataFormatName = null;
    String nativeStreamMetadataFormatClassName = null;
    String[] extraStreamMetadataFormatNames = null;
    String[] extraStreamMetadataFormatClassNames = null;
    String nativeImageMetadataFormatName = null;
    String nativeImageMetadataFormatClassName = null;
    String[] extraImageMetadataFormatNames = null;
    String[] extraImageMetadataFormatClassNames = null;

    private void error(String message) {
        // System.out.println("Error: " + message);
        throw new RuntimeException(message);
    }

    private void testSpi(IIOServiceProvider spi) {
        if (spi.getVendorName() == null) {
            error(spi + " getVendorName == null!");
        }
        if (spi.getVersion() == null) {
            error(spi + " getVersion == null!");
        }
    }

    private void testSpi(ImageReaderWriterSpi spi) {
        testSpi((IIOServiceProvider)spi);
        if (spi.getFormatNames() == null) {
            error("spi.getFormatNames == null!");
        }
        String[] suffixes = spi.getFileSuffixes();
        if (suffixes != null && suffixes.length == 0) {
            error("suffixes.length == 0!");
        }
        String[] MIMETypes = spi.getMIMETypes();
        if (MIMETypes != null && MIMETypes.length == 0) {
            error("MIMETypes.length == 0!");
        }
        if (spi.getPluginClassName() == null) {
            error("spi.getPluginClassName == null!");
        }
        String[] extraStreamMetadataFormatNames =
            spi.getExtraStreamMetadataFormatNames();
        if (extraStreamMetadataFormatNames != null &&
            extraStreamMetadataFormatNames.length == 0) {
            error("extraStreamMetadataFormatNames.length == 0!");
        }
        String[] extraImageMetadataFormatNames =
            spi.getExtraImageMetadataFormatNames();
        if (extraImageMetadataFormatNames != null &&
            extraImageMetadataFormatNames.length == 0) {
            error("extraImageMetadataFormatNames.length == 0!");
        }
    }

    public void testSpi(ImageReaderSpi spi) {
        testSpi((ImageReaderWriterSpi)spi);
        Class[] inputTypes = spi.getInputTypes();
        if (inputTypes == null) {
            error("inputTypes == null!");
        }
        if (inputTypes.length == 0) {
            error("inputTypes.length == 0!");
        }
        String[] writerSpiNames = spi.getImageWriterSpiNames();
        if (writerSpiNames != null && writerSpiNames.length == 0) {
            error("writerSpiNames.length == 0!");
        }
    }

    public void testSpi(ImageWriterSpi spi) {
        testSpi((ImageReaderWriterSpi)spi);
        Class[] outputTypes = spi.getOutputTypes();
        if (outputTypes == null) {
            error("outputTypes == null!");
        }
        if (outputTypes.length == 0) {
            error("outputTypes.length == 0!");
        }
        String[] readerSpiNames = spi.getImageReaderSpiNames();
        if (readerSpiNames != null && readerSpiNames.length == 0) {
            error("readerSpiNames.length == 0!");
        }
    }

    private void resetConstructorArguments() {
        vendorName = null;
        version = null;
        names = null;
        suffixes = null;
        MIMETypes = null;
        readerClassName = null;
        inputTypes = null;
        outputTypes = null;
        writerSpiNames = null;
        readerSpiNames = null;
        nativeStreamMetadataFormatName = null;
        nativeStreamMetadataFormatClassName = null;
        extraStreamMetadataFormatNames = null;
        extraStreamMetadataFormatClassNames = null;
        nativeImageMetadataFormatName = null;
        nativeImageMetadataFormatClassName = null;
        extraImageMetadataFormatNames = null;
        extraImageMetadataFormatClassNames = null;
    }

    private ImageReaderSpi constructImageReaderSpi() {
        return new ImageReaderSpi(vendorName,
                                  version,
                                  names,
                                  suffixes,
                                  MIMETypes,
                                  readerClassName,
                                  inputTypes,
                                  writerSpiNames,
                                  false,
                                  nativeStreamMetadataFormatName,
                                  nativeStreamMetadataFormatClassName,
                                  extraStreamMetadataFormatNames,
                                  extraStreamMetadataFormatClassNames,
                                  false,
                                  nativeImageMetadataFormatName,
                                  nativeImageMetadataFormatClassName,
                                  extraImageMetadataFormatNames,
                                  extraImageMetadataFormatClassNames) {

                public String getDescription(Locale locale) {
                    return null;
                }

                public boolean canDecodeInput(Object source) {
                    return false;
                }

                public ImageReader createReaderInstance(Object extension) {
                    return null;
                }
         };
    }

    private ImageWriterSpi constructImageWriterSpi() {
        return new ImageWriterSpi(vendorName,
                                  version,
                                  names,
                                  suffixes,
                                  MIMETypes,
                                  writerClassName,
                                  outputTypes,
                                  readerSpiNames,
                                  false,
                                  nativeStreamMetadataFormatName,
                                  nativeStreamMetadataFormatClassName,
                                  extraStreamMetadataFormatNames,
                                  extraStreamMetadataFormatClassNames,
                                  false,
                                  nativeImageMetadataFormatName,
                                  nativeImageMetadataFormatClassName,
                                  extraImageMetadataFormatNames,
                                  extraImageMetadataFormatClassNames) {

                public String getDescription(Locale locale) {
                    return null;
                }

                public boolean canEncodeImage(ImageTypeSpecifier type) {
                    return false;
                }

                public ImageWriter createWriterInstance(Object extension) {
                    return null;
                }
         };
    }

    private void checkImageReaderSpiConstructor(boolean shouldFail) {
        boolean gotIAE = false;
        try {
            constructImageReaderSpi();
        } catch (Exception e) {
            if (!(e instanceof IllegalArgumentException)) {
                error("Got exception " + e);
            } else {
                gotIAE = true;
            }
        }
        if (gotIAE != shouldFail) {
            if (gotIAE) {
                error("ImageReaderSpi constructor threw an IAE!");
            } else {
                error("ImageReaderSpi constructor didn't throw an IAE!");
            }
        }
    }

    private void checkImageWriterSpiConstructor(boolean shouldFail) {
        boolean gotIAE = false;
        try {
            constructImageWriterSpi();
        } catch (Exception e) {
            if (!(e instanceof IllegalArgumentException)) {
                error("Got exception " + e);
            } else {
                gotIAE = true;
            }
        }
        if (gotIAE != shouldFail) {
            if (gotIAE) {
                error("ImageWriterSpi constructor threw an IAE!");
            } else {
                error("ImageWriterSpi constructor didn't throw an IAE!");
            }
        }
    }

    public void testImageReaderSpiConstructor() {
        resetConstructorArguments();

        checkImageReaderSpiConstructor(true);
        vendorName = "My Vendor";
        checkImageReaderSpiConstructor(true);
        version = "My Version";
        checkImageReaderSpiConstructor(true);
        names = new String[0];
        checkImageReaderSpiConstructor(true);
        names = new String[1];
        names[0] = "My Format Name";
        checkImageReaderSpiConstructor(true);
        readerClassName = "com.mycompany.Reader";
        checkImageReaderSpiConstructor(true);
        inputTypes = new Class[0];
        checkImageReaderSpiConstructor(true);
        inputTypes = new Class[1];
        inputTypes[0] = Object.class;
        // Now it should work
        checkImageReaderSpiConstructor(false);

        // Test normalization of zero-length arrays
        suffixes = new String[0];
        MIMETypes = new String[0];
        writerSpiNames = new String[0];
        extraStreamMetadataFormatNames = new String[0];
        extraImageMetadataFormatNames = new String[0];

        ImageReaderSpi spi = constructImageReaderSpi();
        if (spi.getFileSuffixes() != null) {
            error("Failed to normalize suffixes!");
        }
        if (spi.getMIMETypes() != null) {
            error("Failed to normalize MIMETypes!");
        }
        if (spi.getImageWriterSpiNames() != null) {
            error("Failed to normalize writerSpiNames!");
        }
        if (spi.getExtraStreamMetadataFormatNames() != null) {
            error("Failed to normalize extraStreamMetadataFormatNames!");
        }
        if (spi.getExtraImageMetadataFormatNames() != null) {
            error("Failed to normalize extraImageMetadataFormatNames!");
        }
    }

    public void testImageWriterSpiConstructor() {
        resetConstructorArguments();

        checkImageWriterSpiConstructor(true);
        vendorName = "My Vendor";
        checkImageWriterSpiConstructor(true);
        version = "My Version";
        checkImageWriterSpiConstructor(true);
        names = new String[0];
        checkImageWriterSpiConstructor(true);
        names = new String[1];
        names[0] = "My Format Name";
        checkImageWriterSpiConstructor(true);
        writerClassName = "com.mycompany.Writer";
        checkImageWriterSpiConstructor(true);
        outputTypes = new Class[0];
        checkImageWriterSpiConstructor(true);
        outputTypes = new Class[1];
        outputTypes[0] = Object.class;
        // Now it should work
        checkImageWriterSpiConstructor(false);

        // Test normalization of zero-length arrays
        suffixes = new String[0];
        MIMETypes = new String[0];
        readerSpiNames = new String[0];
        extraStreamMetadataFormatNames = new String[0];
        extraStreamMetadataFormatClassNames = new String[0];
        extraImageMetadataFormatNames = new String[0];
        extraImageMetadataFormatClassNames = new String[0];

        ImageWriterSpi spi = constructImageWriterSpi();
        if (spi.getFileSuffixes() != null) {
            error("Failed to normalize suffixes!");
        }
        if (spi.getMIMETypes() != null) {
            error("Failed to normalize MIMETypes!");
        }
        if (spi.getImageReaderSpiNames() != null) {
            error("Failed to normalize readerSpiNames!");
        }
        if (spi.getExtraStreamMetadataFormatNames() != null) {
            error("Failed to normalize extraStreamMetadataFormatNames!");
        }
        if (spi.getExtraImageMetadataFormatNames() != null) {
            error("Failed to normalize extraImageMetadataFormatNames!");
        }
    }

    public SpiTest() {
        testImageReaderSpiConstructor();
        testImageWriterSpiConstructor();

        ServiceRegistry registry = IIORegistry.getDefaultInstance();
        Iterator readers = registry.getServiceProviders(ImageReaderSpi.class,
                                                        false);
        while (readers.hasNext()) {
            ImageReaderSpi rspi = (ImageReaderSpi)readers.next();
            System.out.println("*** Testing " + rspi.getClass().getName());
            testSpi(rspi);
        }

        Iterator writers = registry.getServiceProviders(ImageWriterSpi.class,
                                                        false);
        while (writers.hasNext()) {
            ImageWriterSpi wspi = (ImageWriterSpi)writers.next();
            System.out.println("*** Testing " + wspi.getClass().getName());
            testSpi(wspi);
        }
    }

    public static void main(String[] args) {
        new SpiTest();
    }
}
