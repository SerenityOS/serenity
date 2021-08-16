/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4481957
 * @summary Tests that applet-supplied ImageReader, ImageWriter, and
 *          IIOMetadataFormat implementations do not throw unexpected exceptions
 *          when indirectly attempting to access ResourceBundles
 * @run main AppletResourceTest
 */

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Iterator;
import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Vector;

import javax.imageio.IIOException;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;

import org.w3c.dom.Node;

public class AppletResourceTest {

    public static void main(String[] argv) {
        new AppletResourceTest().init();
    }

    public void init() {
        DummyImageReaderImpl reader;
        MyReadWarningListener listener = new MyReadWarningListener();
        Locale[] locales = {new Locale("ru"),
                            new Locale("fr"),
                            new Locale("uk")};

        reader = new DummyImageReaderImpl(new DummyImageReaderSpiImpl());
        reader.setAvailableLocales(locales);
        reader.setLocale(new Locale("fr"));
        reader.addIIOReadWarningListener(listener);

        String baseName = "AppletResourceTest$BugStats";
        try {
            reader.processWarningOccurred("WarningMessage");
            reader.processWarningOccurred(baseName, "water");
        } catch (MissingResourceException mre) {
            throw new RuntimeException("Test failed: couldn't load resource");
        }


    }

    private class MyReadWarningListener implements IIOReadWarningListener {
        public void warningOccurred(ImageReader source,
                                    String warning)
            {
                System.out.println("warning occurred: " + warning);
            }
    }

    public static class BugStats extends ListResourceBundle {

        public Object[][] getContents(){
            return contents;
        }

        private Object[][] contents = {
            {"coffee", new String("coffee from Stats class")},
            {"tea", new String("tea from Stats class")},
            {"water", new String("water from Stats class")}
        };
    }


    public static class DummyImageReaderImpl extends ImageReader {

        public DummyImageReaderImpl(ImageReaderSpi originatingProvider) {
            super(originatingProvider);
        }

        public int getNumImages(boolean allowSearch) throws IOException {
            return 5;
        }

        public int getWidth(int imageIndex) throws IOException {
            if (input == null)
                throw new IllegalStateException();
            Objects.checkIndex(imageIndex, 5);

            return 10;
        }

        public int getHeight(int imageIndex) throws IOException {
            if (input == null)
                throw new IllegalStateException();
            Objects.checkIndex(imageIndex, 5);

            return 15;
        }

        public Iterator getImageTypes(int imageIndex) throws IOException {
            if (input == null)
                throw new IllegalStateException();
            Objects.checkIndex(imageIndex, 5);

            Vector imageTypes = new Vector();
            imageTypes.add(ImageTypeSpecifier.createFromBufferedImageType
                           (BufferedImage.TYPE_BYTE_GRAY ));
            return imageTypes.iterator();
        }

        public IIOMetadata getStreamMetadata() throws IOException {
            return new DummyIIOMetadataImpl(true, null, null, null, null);
        }

        public IIOMetadata getImageMetadata(int imageIndex)
          throws IOException {

            if (input == null)
                throw new IllegalStateException();
            Objects.checkIndex(imageIndex, 5);
            if (seekForwardOnly) {
                if (imageIndex < minIndex)
                    throw new IndexOutOfBoundsException();
                minIndex = imageIndex;
            }
            return new DummyIIOMetadataImpl(true, null, null, null, null);
        }


        public BufferedImage read(int imageIndex, ImageReadParam param)
          throws IOException {
            if (input == null)
                throw new IllegalStateException();
            Objects.checkIndex(imageIndex, 5);
            if (seekForwardOnly) {
                if (imageIndex < minIndex)
                    throw new IndexOutOfBoundsException();
                minIndex = imageIndex;
            }

            return getDestination(param, getImageTypes(imageIndex), 10, 15);
        }

// protected  methods - now public

        public  boolean abortRequested() {
            return super.abortRequested();
        }

        public  void clearAbortRequest() {
            super.clearAbortRequest();
        }

        public  void processImageComplete() {
            super.processImageComplete();
        }

        public  void processImageProgress(float percentageDone) {
            super.processImageProgress(percentageDone);
        }

        public  void processImageStarted(int imageIndex) {
            super.processImageStarted(imageIndex);
        }

        public  void processImageUpdate(BufferedImage theImage,
                                        int minX,
                                        int minY,
                                        int width,
                                        int height,
                                        int periodX,
                                        int periodY,
                                        int[] bands) {
            super.processImageUpdate(theImage,
                                     minX,
                                     minY,
                                     width,
                                     height,
                                     periodX,
                                     periodY,
                                     bands);
        }

        public  void processPassComplete(BufferedImage theImage) {
            super. processPassComplete(theImage);
        }

        public  void processPassStarted(BufferedImage theImage,
                                        int pass, int minPass,
                                        int maxPass,
                                        int minX,
                                        int minY,
                                        int periodX,
                                        int periodY,
                                        int[] bands) {
            super.processPassStarted(theImage,
                                     pass,
                                     minPass,
                                     maxPass,
                                     minX,
                                     minY,
                                     periodX,
                                     periodY,
                                     bands);
        }

        public  void processReadAborted() {
            super.processReadAborted();
        }

        public  void processSequenceComplete() {
            super.processSequenceComplete();
        }

        public  void processSequenceStarted(int minIndex) {
            super.processSequenceStarted(minIndex);
        }

        public  void processThumbnailComplete() {
            super.processThumbnailComplete();
        }

        public  void processThumbnailPassComplete(BufferedImage theThumbnail) {
            super.processThumbnailPassComplete(theThumbnail);
        }

        public  void processThumbnailPassStarted(BufferedImage theThumbnail,
                                                 int pass,
                                                 int minPass,
                                                 int maxPass,
                                                 int minX,
                                                 int minY,
                                                 int periodX,
                                                 int periodY,
                                                 int[] bands) {
            super.processThumbnailPassStarted(theThumbnail,
                                              pass,
                                              minPass,
                                              maxPass,
                                              minX,
                                              minY,
                                              periodX,
                                              periodY,
                                              bands);
        }

        public  void processThumbnailProgress(float percentageDone) {
            super.processThumbnailProgress(percentageDone);
        }

        public  void processThumbnailStarted(int imageIndex, int thumbnailIndex) {
            super.processThumbnailStarted(imageIndex, thumbnailIndex);
        }

        public  void processThumbnailUpdate(BufferedImage theThumbnail,
                                            int minX,
                                            int minY,
                                            int width,
                                            int height,
                                            int periodX,
                                            int periodY,
                                            int[] bands) {
            super.processThumbnailUpdate(theThumbnail,
                                         minX,
                                         minY,
                                         width,
                                         height,
                                         periodX,
                                         periodY,
                                         bands);
        }

        public  void processWarningOccurred(String warning) {
            super.processWarningOccurred(warning);
        }



        public static Rectangle getSourceRegion(ImageReadParam param,
                                                int srcWidth,
                                                int srcHeight) {
            return ImageReader.getSourceRegion(param, srcWidth, srcHeight);
        }

        public static void computeRegions(ImageReadParam param,
                                          int srcWidth,
                                          int srcHeight,
                                          BufferedImage image,
                                          Rectangle srcRegion,
                                          Rectangle destRegion) {
            ImageReader.computeRegions(param,
                                       srcWidth,
                                       srcHeight,
                                       image,
                                       srcRegion,
                                       destRegion);
        }

        public static void checkReadParamBandSettings(ImageReadParam param,
                                                      int numSrcBands,
                                                      int numDstBands) {
            ImageReader.checkReadParamBandSettings( param,
                                                    numSrcBands,
                                                    numDstBands);
        }

        public static BufferedImage getDestination(ImageReadParam param,
                                                   Iterator imageTypes,
                                                   int width,
                                                   int height) throws IIOException {
            return ImageReader.getDestination(param,
                                              imageTypes,
                                              width,
                                              height);
        }

        public  void setAvailableLocales(Locale[] locales) {
            if (locales == null || locales.length == 0)
                availableLocales = null;
            else
                availableLocales = (Locale[])locales.clone();
        }

        public  void processWarningOccurred(String baseName, String keyword) {
            super.processWarningOccurred(baseName, keyword);
        }
    }

    public static class DummyIIOMetadataImpl extends IIOMetadata {

        public DummyIIOMetadataImpl() {
            super();
        }

        public DummyIIOMetadataImpl(boolean standardMetadataFormatSupported,
                                    String nativeMetadataFormatName,
                                    String nativeMetadataFormatClassName,
                                    String[] extraMetadataFormatNames,
                                    String[] extraMetadataFormatClassNames) {
            super(standardMetadataFormatSupported,
                  nativeMetadataFormatName,
                  nativeMetadataFormatClassName,
                  extraMetadataFormatNames,
                  extraMetadataFormatClassNames);
        }

        public boolean isReadOnly() {
            return true;
        }

        public Node getAsTree(String formatName) {
            return null;
        }

        public void mergeTree(String formatName, Node root)
          throws IIOInvalidTreeException {
            throw new IllegalStateException();
        }

        public void reset() {
            throw new IllegalStateException();
        }
    }

    public static class DummyImageReaderSpiImpl extends ImageReaderSpi {

        static final String[] names ={ "myformat" };

        public DummyImageReaderSpiImpl() {
            super("vendorName",
                  "version",
                  names,
                  null,
                  null,
                  "DummyImageReaderImpl",
                  STANDARD_INPUT_TYPE,
                  null,
                  true,
                  null,
                  null,
                  null,
                  null,
                  true,
                  null,
                  null,
                  null,
                  null);
        }
        public boolean canDecodeInput(Object source)
          throws IOException {
            return true;
        }
        public ImageReader createReaderInstance(Object extension)
          throws IOException {
            return new DummyImageReaderImpl(this);
        }
        public String getDescription(Locale locale) {
            return "DummyImageReaderSpiImpl";
        }
    }
}
