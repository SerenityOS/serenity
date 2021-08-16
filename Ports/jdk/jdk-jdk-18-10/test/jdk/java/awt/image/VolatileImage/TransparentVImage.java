/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4881082 4916294 5002129 8158524 8198613
 * @summary The test verifies whether the rendering operations on transparent
 *          and translucent VolatileImage objects generate identical output
 *          as generated with transparent and translucent BufferedImages.
 * @key headful
 * @run main/othervm -Dsun.java2d.uiScale=1 TransparentVImage
 */
import java.awt.GraphicsConfiguration;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.awt.Color;
import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

/*
 * This test draws the same contents to 4 images: 2 BufferedImages that are
 * transparent and translucent and 2 VolatileImages that are transparent and
 * translucent. It compares the results pixel-by-pixel and fails if the
 * results are not the same.
 */
public class TransparentVImage
    extends JComponent {

    BufferedImage cImgTransparent, cImgTranslucent;
    VolatileImage vImgTransparent, vImgTranslucent;
    Image sprite;
    static final int IMAGE_SIZE = 250;
    static final int WINDOW_SIZE = 600;
    static boolean doneComparing = false;
    static JFrame testFrame = null;

    @Override
    public void paint(Graphics g) {
        if (cImgTransparent == null) {
            GraphicsConfiguration gc = getGraphicsConfiguration();
            // doesn't exist yet: create it (and the other images)
            cImgTransparent = (BufferedImage)
                gc.createCompatibleImage(IMAGE_SIZE, IMAGE_SIZE,
                                         Transparency.BITMASK);
            cImgTranslucent = (BufferedImage)
                gc.createCompatibleImage(IMAGE_SIZE, IMAGE_SIZE,
                                         Transparency.TRANSLUCENT);
            vImgTransparent = gc.createCompatibleVolatileImage(IMAGE_SIZE,
                IMAGE_SIZE, Transparency.BITMASK);
            vImgTranslucent = gc.createCompatibleVolatileImage(IMAGE_SIZE,
                IMAGE_SIZE, Transparency.TRANSLUCENT);

            String fileName = "duke.gif";
            String separator = System.getProperty("file.separator");
            String dirPath = System.getProperty("test.src", ".");
            String filePath = dirPath + separator + fileName;
            sprite = new ImageIcon(filePath).getImage();

            // Now they exist; render to them
            Graphics gImg[] = new Graphics[4];
            gImg[0] = cImgTransparent.getGraphics();
            gImg[1] = cImgTranslucent.getGraphics();
            gImg[2] = vImgTransparent.getGraphics();
            gImg[3] = vImgTranslucent.getGraphics();

            for (int i = 0; i < gImg.length; ++i) {
                /*
                 * VolatileImage utilizes the power of accelerated rendering
                 * using GPU. The GPU drivers for D3d and OpenGL are supplied
                 * by OEM vendors and are external to graphics subsystem. Thus
                 * one cannot guarentee that the drivers will render the
                 * primitives using exactly the same algorithms as those used
                 * by BufferedImage. This could result in minor differences in
                 * pixel values between BufferedImage and VolatileImage.
                 *
                 * The pipelines for D3d and OpenGL adjust the rendering with
                 * fudge factors to align output of GPU rendering with that of
                 * CPU rendering. Some of the draw calls in this paint method
                 * are commented indicating a need to fine tune the fudge
                 * factors in the future. Once they are found to work on all
                 * hardware, the draw calls will be enabled.
                 */
                // rectangular fill
                gImg[i].setColor(Color.blue);
                gImg[i].fillRect(0, 0, IMAGE_SIZE, IMAGE_SIZE);

                /*
                 * Image copy. Copy it 3 times to get any image management
                 * acceleration going
                 */
                int spriteW = sprite.getWidth(null);
                gImg[i].drawImage(sprite, 0, 0, null);
                gImg[i].drawImage(sprite, spriteW, 0, null);
                gImg[i].drawImage(sprite, 2 * spriteW, 0, null);

                // horizontal/vertical/diagonal lines
                gImg[i].setColor(Color.red);
                gImg[i].drawLine(0, 0,
                                 IMAGE_SIZE - 1, IMAGE_SIZE - 1);
                gImg[i].drawLine(IMAGE_SIZE / 2, 0,
                                 IMAGE_SIZE / 2, IMAGE_SIZE - 1);
                //gImg[i].drawLine(IMAGE_SIZE, 0,
                //                 0, IMAGE_SIZE - 1);
                gImg[i].drawLine(0, IMAGE_SIZE / 2,
                                 IMAGE_SIZE - 1, IMAGE_SIZE / 2);

                // filled circle
                //gImg[i].setColor(Color.yellow);
                //gImg[i].fillOval(IMAGE_SIZE / 2 - 20, IMAGE_SIZE / 2 - 20,
                //                 40, 40);
            }

            /*
             * Now everything is drawn: let's compare pixels
             * First, grab the pixel arrays
             */
            int cRgbTransparent[] = new int[IMAGE_SIZE * IMAGE_SIZE];
            int cRgbTranslucent[] = new int[IMAGE_SIZE * IMAGE_SIZE];
            int vRgbTransparent[] = new int[IMAGE_SIZE * IMAGE_SIZE];
            int vRgbTranslucent[] = new int[IMAGE_SIZE * IMAGE_SIZE];
            cImgTransparent.getRGB(0, 0, IMAGE_SIZE, IMAGE_SIZE,
                                   cRgbTransparent, 0, IMAGE_SIZE);
            cImgTranslucent.getRGB(0, 0, IMAGE_SIZE, IMAGE_SIZE,
                                   cRgbTranslucent, 0, IMAGE_SIZE);
            BufferedImage bImgTransparent = vImgTransparent.getSnapshot();
            bImgTransparent.getRGB(0, 0, IMAGE_SIZE, IMAGE_SIZE,
                                   vRgbTransparent, 0, IMAGE_SIZE);
            BufferedImage bImgTranslucent = vImgTranslucent.getSnapshot();
            bImgTranslucent.getRGB(0, 0, IMAGE_SIZE, IMAGE_SIZE,
                                   vRgbTranslucent, 0, IMAGE_SIZE);

            boolean failed = false;
            for (int pixel = 0; pixel < cRgbTransparent.length; ++pixel) {
                if (cRgbTransparent[pixel] != vRgbTransparent[pixel]) {
                    failed = true;
                    System.out.println("Error in transparent image: " +
                        "BI[" + pixel + "] = " +
                        Integer.toHexString(cRgbTransparent[pixel]) +
                        "VI[" + pixel + "] = " +
                        Integer.toHexString(vRgbTransparent[pixel]));
                    break;
                }
                if (cRgbTranslucent[pixel] != vRgbTranslucent[pixel]) {
                    failed = true;
                    System.out.println("Error in translucent image: " +
                        "BI[" + pixel + "] = " +
                        Integer.toHexString(cRgbTranslucent[pixel]) +
                        "VI[" + pixel + "] = " +
                        Integer.toHexString(vRgbTranslucent[pixel]));
                    break;
                }
            }
            if (failed) {
                throw new RuntimeException("Failed: Pixel mis-match observed");
            }
            else {
                System.out.println("Passed");
            }
            doneComparing = true;
        }

        g.drawImage(cImgTransparent, 0, 0, null);
        g.drawImage(cImgTranslucent, getWidth() - IMAGE_SIZE, 0, null);
        g.drawImage(vImgTransparent, 0, getHeight() - IMAGE_SIZE, null);
        g.drawImage(vImgTranslucent, getWidth() - IMAGE_SIZE,
                    getHeight() - IMAGE_SIZE, null);
    }

    private static void constructTestUI() {
        testFrame = new JFrame();
        testFrame.setSize(600, 600);
        testFrame.setResizable(false);
        testFrame.getContentPane().add(new TransparentVImage());

        testFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        testFrame.setLocationRelativeTo(null);
        testFrame.setVisible(true);
    }

    private static void destroyTestUI() {
        testFrame.dispose();
    }

    public static void main(String args[]) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    // Construct the test interface
                    constructTestUI();
                } catch (Exception ex) {
                    // Throw an exception indicating error while creating UI
                    throw new RuntimeException("Test Failed: Error while" +
                        " creating the test interface.");
                }
            }
        });

        /*
         * Wait until the image comparison between VolatileImage and
         * BufferedImage is complete.
         */
        while (!doneComparing) {
            try {
                Thread.sleep(100);
            }
            catch (Exception e) {}
        }

        /*
         * Now sleep just a little longer to let the user see the resulting
         * images in the frame
         */
        try {
            Thread.sleep(5000);
        }
        catch (Exception e) {}

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    // Destroy the test interface
                    destroyTestUI();
                } catch (Exception ex) {
                    // Throw an exception indicating error while deleting UI
                    throw new RuntimeException("Test Failed: Error while" +
                        " deleting the test interface.");
                }
            }
        });
    }
}
