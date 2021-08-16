/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import javax.imageio.ImageIO;
import sun.awt.SunHints;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.image.ImageObserver;
import javax.swing.JPanel;
import jdk.test.lib.Platform;
import java.awt.image.MultiResolutionImage;

/**
 * @test
 * @bug 8011059
 * @key headful
 * @author Alexander Scherbatiy
 * @summary [macosx] Make JDK demos look perfect on retina displays
 * @library /test/lib
 * @build jdk.test.lib.Platform
 * @requires (os.family == "mac")
 * @modules java.desktop/sun.awt
 *          java.desktop/sun.awt.image
 *          java.desktop/sun.lwawt.macosx:open
 * @run main MultiResolutionImageTest TOOLKIT_PREPARE
 * @run main MultiResolutionImageTest TOOLKIT_LOAD
 * @run main MultiResolutionImageTest TOOLKIT
 */

public class MultiResolutionImageTest {

    private static final int IMAGE_WIDTH = 300;
    private static final int IMAGE_HEIGHT = 200;
    private static final Color COLOR_1X = Color.GREEN;
    private static final Color COLOR_2X = Color.BLUE;
    private static final String IMAGE_NAME_1X = "image.png";
    private static final String IMAGE_NAME_2X = "image@2x.png";

    public static void main(String[] args) throws Exception {

        System.out.println("args: " + args.length);

        if (args.length == 0) {
            throw new RuntimeException("Not found a test");
        }
        String test = args[0];
        System.out.println("TEST: " + test);

        // To automatically pass the test if the test is not run using JTReg.
        if (!Platform.isOSX()) {
            System.out.println("Non-Mac platform detected. Passing the test");
            return;
        }
        switch (test) {
            case "TOOLKIT_PREPARE":
                testToolkitMultiResolutionImagePrepare();
                break;
            case "TOOLKIT_LOAD":
                testToolkitMultiResolutionImageLoad();
                break;
            case "TOOLKIT":
                testToolkitMultiResolutionImage();
                testImageNameTo2xParsing();
                break;
            default:
                throw new RuntimeException("Unknown test: " + test);
        }
        System.out.println("Test passed.");
    }

    static void testToolkitMultiResolutionImagePrepare() throws Exception {

        generateImages();

        File imageFile = new File(IMAGE_NAME_1X);
        String fileName = imageFile.getAbsolutePath();

        Image image = Toolkit.getDefaultToolkit().getImage(fileName);

        Toolkit toolkit = Toolkit.getDefaultToolkit();
        toolkit.prepareImage(image, IMAGE_WIDTH, IMAGE_HEIGHT,
            new LoadImageObserver(image));

        testToolkitMultiResolutionImageLoad(image);
    }

    static void testToolkitMultiResolutionImageLoad() throws Exception {

        generateImages();

        File imageFile = new File(IMAGE_NAME_1X);
        String fileName = imageFile.getAbsolutePath();
        Image image = Toolkit.getDefaultToolkit().getImage(fileName);
        testToolkitMultiResolutionImageLoad(image);
    }

    static void testToolkitMultiResolutionImageLoad(Image image)
        throws Exception {

        MediaTracker tracker = new MediaTracker(new JPanel());
        tracker.addImage(image, 0);
        tracker.waitForID(0);
        if (tracker.isErrorAny()) {
            throw new RuntimeException("Error during image loading");
        }
        tracker.removeImage(image, 0);

        testImageLoaded(image);

        int w = image.getWidth(null);
        int h = image.getHeight(null);

        Image resolutionVariant = ((MultiResolutionImage) image)
            .getResolutionVariant(2 * w, 2 * h);

        if (image == resolutionVariant) {
            throw new RuntimeException("Resolution variant is not loaded");
        }

        testImageLoaded(resolutionVariant);
    }

    static void testImageLoaded(Image image) {

        Toolkit toolkit = Toolkit.getDefaultToolkit();

        int flags = toolkit.checkImage(image, IMAGE_WIDTH, IMAGE_WIDTH,
            new SilentImageObserver());
        if ((flags & (ImageObserver.FRAMEBITS | ImageObserver.ALLBITS)) == 0) {
            throw new RuntimeException("Image is not loaded!");
        }
    }

    static class SilentImageObserver implements ImageObserver {

        @Override
        public boolean imageUpdate(Image img, int infoflags, int x, int y,
            int width, int height) {
            throw new RuntimeException("Observer should not be called!");
        }
    }

    static class LoadImageObserver implements ImageObserver {

        Image image;

        public LoadImageObserver(Image image) {
            this.image = image;
        }

        @Override
        public boolean imageUpdate(Image img, int infoflags, int x, int y,
            int width, int height) {

            if (image != img) {
                throw new RuntimeException("Original image is not passed "
                    + "to the observer");
            }

            if ((infoflags & ImageObserver.WIDTH) != 0) {
                if (width != IMAGE_WIDTH) {
                    throw new RuntimeException("Original width is not passed "
                        + "to the observer");
                }
            }

            if ((infoflags & ImageObserver.HEIGHT) != 0) {
                if (height != IMAGE_HEIGHT) {
                    throw new RuntimeException("Original height is not passed "
                        + "to the observer");
                }
            }

            return (infoflags & ALLBITS) == 0;
        }

    }

    static void testToolkitMultiResolutionImage() throws Exception {

        generateImages();

        File imageFile = new File(IMAGE_NAME_1X);
        String fileName = imageFile.getAbsolutePath();
        URL url = imageFile.toURI().toURL();
        testToolkitMultiResolutionImageChache(fileName, url);

        Image image = Toolkit.getDefaultToolkit().getImage(fileName);
        testToolkitImageObserver(image);
        testToolkitMultiResolutionImage(image, false);
        testToolkitMultiResolutionImage(image, true);

        image = Toolkit.getDefaultToolkit().getImage(url);
        testToolkitImageObserver(image);
        testToolkitMultiResolutionImage(image, false);
        testToolkitMultiResolutionImage(image, true);
    }

    static void testToolkitMultiResolutionImageChache(String fileName,
        URL url) {

        Image img1 = Toolkit.getDefaultToolkit().getImage(fileName);
        if (!(img1 instanceof MultiResolutionImage)) {
            throw new RuntimeException("Not a MultiResolutionImage");
        }

        Image img2 = Toolkit.getDefaultToolkit().getImage(fileName);
        if (img1 != img2) {
            throw new RuntimeException("Image is not cached");
        }

        img1 = Toolkit.getDefaultToolkit().getImage(url);
        if (!(img1 instanceof MultiResolutionImage)) {
            throw new RuntimeException("Not a MultiResolutionImage");
        }

        img2 = Toolkit.getDefaultToolkit().getImage(url);
        if (img1 != img2) {
            throw new RuntimeException("Image is not cached");
        }
    }

    static void testToolkitMultiResolutionImage(Image image,
        boolean enableImageScaling) throws Exception {

        MediaTracker tracker = new MediaTracker(new JPanel());
        tracker.addImage(image, 0);
        tracker.waitForID(0);
        if (tracker.isErrorAny()) {
            throw new RuntimeException("Error during image loading");
        }

        final BufferedImage bufferedImage1x = new BufferedImage(IMAGE_WIDTH,
            IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g1x = (Graphics2D) bufferedImage1x.getGraphics();
        setImageScalingHint(g1x, false);
        g1x.drawImage(image, 0, 0, null);
        checkColor(bufferedImage1x.getRGB(3 * IMAGE_WIDTH / 4,
            3 * IMAGE_HEIGHT / 4), false);

        Image resolutionVariant = ((MultiResolutionImage) image).
            getResolutionVariant(2 * IMAGE_WIDTH, 2 * IMAGE_HEIGHT);

        if (resolutionVariant == null) {
            throw new RuntimeException("Resolution variant is null");
        }

        MediaTracker tracker2x = new MediaTracker(new JPanel());
        tracker2x.addImage(resolutionVariant, 0);
        tracker2x.waitForID(0);
        if (tracker2x.isErrorAny()) {
            throw new RuntimeException("Error during scalable image loading");
        }

        final BufferedImage bufferedImage2x = new BufferedImage(2 * IMAGE_WIDTH,
            2 * IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2x = (Graphics2D) bufferedImage2x.getGraphics();
        setImageScalingHint(g2x, enableImageScaling);
        g2x.drawImage(image, 0, 0, 2 * IMAGE_WIDTH,
            2 * IMAGE_HEIGHT, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT, null);
        checkColor(bufferedImage2x.getRGB(3 * IMAGE_WIDTH / 2,
            3 * IMAGE_HEIGHT / 2), enableImageScaling);

        if (!(image instanceof MultiResolutionImage)) {
            throw new RuntimeException("Not a MultiResolutionImage");
        }

        MultiResolutionImage multiResolutionImage
            = (MultiResolutionImage) image;

        Image image1x = multiResolutionImage.getResolutionVariant(
            IMAGE_WIDTH, IMAGE_HEIGHT);
        Image image2x = multiResolutionImage.getResolutionVariant(
            2 * IMAGE_WIDTH, 2 * IMAGE_HEIGHT);

        if (image1x.getWidth(null) * 2 != image2x.getWidth(null)
            || image1x.getHeight(null) * 2 != image2x.getHeight(null)) {
            throw new RuntimeException("Wrong resolution variant size");
        }
    }

    static void testToolkitImageObserver(final Image image) {

        ImageObserver observer = new ImageObserver() {

            @Override
            public boolean imageUpdate(Image img, int infoflags, int x, int y,
                int width, int height) {

                if (img != image) {
                    throw new RuntimeException("Wrong image in observer");
                }

                if ((infoflags & (ImageObserver.ERROR | ImageObserver.ABORT))
                    != 0) {
                    throw new RuntimeException("Error during image loading");
                }

                return (infoflags & ImageObserver.ALLBITS) == 0;

            }
        };

        final BufferedImage bufferedImage2x = new BufferedImage(2 * IMAGE_WIDTH,
            2 * IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2x = (Graphics2D) bufferedImage2x.getGraphics();
        setImageScalingHint(g2x, true);

        g2x.drawImage(image, 0, 0, 2 * IMAGE_WIDTH, 2 * IMAGE_HEIGHT, 0, 0,
            IMAGE_WIDTH, IMAGE_HEIGHT, observer);

    }

    static void setImageScalingHint(Graphics2D g2d,
        boolean enableImageScaling) {
        g2d.setRenderingHint(SunHints.KEY_RESOLUTION_VARIANT, enableImageScaling
            ? RenderingHints.VALUE_RESOLUTION_VARIANT_DEFAULT
            : RenderingHints.VALUE_RESOLUTION_VARIANT_BASE);
    }

    static void checkColor(int rgb, boolean isImageScaled) {

        if (!isImageScaled && COLOR_1X.getRGB() != rgb) {
            throw new RuntimeException("Wrong 1x color: " + new Color(rgb));
        }

        if (isImageScaled && COLOR_2X.getRGB() != rgb) {
            throw new RuntimeException("Wrong 2x color" + new Color(rgb));
        }
    }

    static void generateImages() throws Exception {
        if (!new File(IMAGE_NAME_1X).exists()) {
            generateImage(1);
        }

        if (!new File(IMAGE_NAME_2X).exists()) {
            generateImage(2);
        }
    }

    static void generateImage(int scale) throws Exception {
        BufferedImage image = new BufferedImage(
            scale * IMAGE_WIDTH, scale * IMAGE_HEIGHT,
            BufferedImage.TYPE_INT_RGB);
        Graphics g = image.getGraphics();
        g.setColor(scale == 1 ? COLOR_1X : COLOR_2X);
        g.fillRect(0, 0, scale * IMAGE_WIDTH, scale * IMAGE_HEIGHT);
        File file = new File(scale == 1 ? IMAGE_NAME_1X : IMAGE_NAME_2X);
        ImageIO.write(image, "png", file);
    }

    static void testImageNameTo2xParsing() throws Exception {

        for (String[] testNames : TEST_FILE_NAMES) {
            String testName = testNames[0];
            String goldenName = testNames[1];
            String resultName = getTestScaledImageName(testName);

            if (!isValidPath(testName) && resultName == null) {
                continue;
            }

            if (goldenName.equals(resultName)) {
                continue;
            }

            throw new RuntimeException("Test name " + testName
                + ", result name: " + resultName);
        }

        for (URL[] testURLs : TEST_URLS) {
            URL testURL = testURLs[0];
            URL goldenURL = testURLs[1];
            URL resultURL = getTestScaledImageURL(testURL);

            if (!isValidPath(testURL.getPath()) && resultURL == null) {
                continue;
            }

            if (goldenURL.equals(resultURL)) {
                continue;
            }

            throw new RuntimeException("Test url: " + testURL
                + ", result url: " + resultURL);
        }

    }

    static URL getTestScaledImageURL(URL url) throws Exception {
        Method method = getScalableImageMethod("getScaledImageURL", URL.class);
        return (URL) method.invoke(null, url);
    }

    static String getTestScaledImageName(String name) throws Exception {
        Method method = getScalableImageMethod(
            "getScaledImageName", String.class);
        return (String) method.invoke(null, name);
    }

    private static boolean isValidPath(String path) {
        return !path.isEmpty() && !path.endsWith("/") && !path.endsWith(".")
            && !path.contains("@2x");
    }

    private static Method getScalableImageMethod(String name,
        Class... parameterTypes) throws Exception {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Method method = toolkit.getClass()
            .
            getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return method;
    }
    private static final String[][] TEST_FILE_NAMES;
    private static final URL[][] TEST_URLS;

    static {
        TEST_FILE_NAMES = new String[][]{
            {"", null},
            {".", null},
            {"..", null},
            {"/", null},
            {"/.", null},
            {"dir/", null},
            {"dir/.", null},
            {"aaa@2x.png", null},
            {"/dir/aaa@2x.png", null},
            {"image", "image@2x"},
            {"image.ext", "image@2x.ext"},
            {"image.aaa.ext", "image.aaa@2x.ext"},
            {"dir/image", "dir/image@2x"},
            {"dir/image.ext", "dir/image@2x.ext"},
            {"dir/image.aaa.ext", "dir/image.aaa@2x.ext"},
            {"dir/aaa.bbb/image", "dir/aaa.bbb/image@2x"},
            {"dir/aaa.bbb/image.ext", "dir/aaa.bbb/image@2x.ext"},
            {"dir/aaa.bbb/image.ccc.ext", "dir/aaa.bbb/image.ccc@2x.ext"},
            {"/dir/image", "/dir/image@2x"},
            {"/dir/image.ext", "/dir/image@2x.ext"},
            {"/dir/image.aaa.ext", "/dir/image.aaa@2x.ext"},
            {"/dir/aaa.bbb/image", "/dir/aaa.bbb/image@2x"},
            {"/dir/aaa.bbb/image.ext", "/dir/aaa.bbb/image@2x.ext"},
            {"/dir/aaa.bbb/image.ccc.ext", "/dir/aaa.bbb/image.ccc@2x.ext"}
        };
        try {
            TEST_URLS = new URL[][]{
                // file
                {new URL("file:/aaa"), new URL("file:/aaa@2x")},
                {new URL("file:/aaa.ext"), new URL("file:/aaa@2x.ext")},
                {new URL("file:/aaa.bbb.ext"), new URL("file:/aaa.bbb@2x.ext")},
                {new URL("file:/ccc/aaa.bbb.ext"),
                    new URL("file:/ccc/aaa.bbb@2x.ext")},
                {new URL("file:/ccc.ddd/aaa.bbb.ext"),
                    new URL("file:/ccc.ddd/aaa.bbb@2x.ext")},
                {new URL("file:///~/image"), new URL("file:///~/image@2x")},
                {new URL("file:///~/image.ext"),
                    new URL("file:///~/image@2x.ext")},
                // http
                {new URL("http://www.test.com"), null},
                {new URL("http://www.test.com/"), null},
                {new URL("http://www.test.com///"), null},
                {new URL("http://www.test.com/image"),
                    new URL("http://www.test.com/image@2x")},
                {new URL("http://www.test.com/image.ext"),
                    new URL("http://www.test.com/image@2x.ext")},
                {new URL("http://www.test.com/dir/image"),
                    new URL("http://www.test.com/dir/image@2x")},
                {new URL("http://www.test.com:80/dir/image.aaa.ext"),
                    new URL("http://www.test.com:80/dir/image.aaa@2x.ext")},
                {new URL("http://www.test.com:8080/dir/image.aaa.ext"),
                    new URL("http://www.test.com:8080/dir/image.aaa@2x.ext")},
                // jar
                {new URL("jar:file:/dir/Java2D.jar!/image"),
                    new URL("jar:file:/dir/Java2D.jar!/image@2x")},
                {new URL("jar:file:/dir/Java2D.jar!/image.aaa.ext"),
                    new URL("jar:file:/dir/Java2D.jar!/image.aaa@2x.ext")},
                {new URL("jar:file:/dir/Java2D.jar!/images/image"),
                    new URL("jar:file:/dir/Java2D.jar!/images/image@2x")},
                {new URL("jar:file:/dir/Java2D.jar!/images/image.ext"),
                    new URL("jar:file:/dir/Java2D.jar!/images/image@2x.ext")},
                {new URL("jar:file:/aaa.bbb/Java2D.jar!/images/image.ext"),
                    new URL("jar:file:/aaa.bbb/Java2D.jar!/"
                    + "images/image@2x.ext")},
                {new URL("jar:file:/dir/Java2D.jar!/aaa.bbb/image.ext"),
                    new URL("jar:file:/dir/Java2D.jar!/"
                    + "aaa.bbb/image@2x.ext")},};
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static class PreloadedImageObserver implements ImageObserver {

        @Override
        public boolean imageUpdate(Image img, int infoflags, int x, int y,
            int width, int height) {
            throw new RuntimeException("Image should be already preloaded");
        }
    }
}
