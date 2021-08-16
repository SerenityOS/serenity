/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6334602
 * @summary Test verifies that when we create GIF image from Opaque PNG images
 *          it should not be transparent.
 * @modules java.desktop/com.sun.imageio.plugins.gif
 * @run     main/manual OpaquePNGToGIFTest
 */

import java.awt.image.BufferedImage;
import com.sun.imageio.plugins.gif.GIFImageMetadata;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public final class OpaquePNGToGIFTest extends Frame {

    Image img = null;
    private Dimension prefImgSize = new Dimension(100, 100);
    private Color canvasColor = new Color(0, 255, 0);
    File outputFile = null;
    ImageCanvas imageCanvas = new ImageCanvas();
    FileOutputStream fos;
    private static GridBagLayout layout;
    private static JPanel mainControlPanel;
    private static JPanel resultButtonPanel;
    private static JPanel canvasPanel;
    private static JLabel instructionText;
    private static JButton startTestButton;
    private static JButton passButton;
    private static JButton failButton;
    private static JDialog dialog;
    private static Frame instructionFrame;
    private static Frame imageFrame;
    Toolkit tk;
    ImageWriter writer = null;
    boolean testPassed, testGeneratedInterrupt, startButtonClicked;
    private static Thread mainThread;

    public OpaquePNGToGIFTest() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                startTest();
            } catch (Exception ex) {
                Logger.getLogger(OpaquePNGToGIFTest.class.getName()).
                        log(Level.SEVERE, null, ex);
            }
        });
        mainThread = Thread.currentThread();
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            if (!testPassed && testGeneratedInterrupt) {
                throw new RuntimeException("Test failed or didnt run"
                        + " properly");
            }
        }
        if (!testGeneratedInterrupt) {
            if (img != null) {
                img.flush();
            }
            instructionFrame.dispose();
            if (startButtonClicked) {
                imageFrame.dispose();
            }
            fos.close();
            Files.delete(outputFile.toPath());
            throw new RuntimeException("user has not executed the test");
        }
    }

    public void startTest() throws Exception {
        instructionFrame = new Frame();
        dialog = new JDialog(instructionFrame);
        dialog.setTitle("Instruction Dialog");
        layout = new GridBagLayout();
        mainControlPanel = new JPanel(layout);
        resultButtonPanel = new JPanel(layout);
        canvasPanel = new JPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        String instructions
                = "<html>    INSTRUCTIONS:<br><br>"
                + "After clicking on Start Test button you will see Red<br> "
                + " circle drawn with light blue background, if the circle<br>"
                + " color changes from Red to Green then press button Fail,<br>"
                + " if it stays Red then press button Pass.<br><br></html>";
        instructionText = new JLabel();
        instructionText.setText(instructions);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainControlPanel.add(instructionText, gbc);
        startTestButton = new JButton("Start Test");
        startTestButton.setActionCommand("Start Test");
        startTestButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    startButtonClicked = true;
                    imageFrame = new Frame();

                    Iterator it = ImageIO.getImageWritersByFormatName("GIF");
                    while (it.hasNext()) {
                        writer = (ImageWriter) it.next();
                        break;
                    }
                    // read input opaque PNG image.
                    String fileName = "opaque_input.png";
                    String sep = System.getProperty("file.separator");
                    String dir = System.getProperty("test.src", ".");
                    System.out.println(dir + "     " + sep);
                    String filePath = dir + sep + fileName;
                    File inputFile = new File(filePath);
                    ImageInputStream iis = ImageIO.
                            createImageInputStream(inputFile);
                    ImageReader reader = null;
                    Iterator readerIter = ImageIO.getImageReaders(iis);
                    while (readerIter.hasNext()) {
                        reader = (ImageReader) readerIter.next();
                        break;
                    }

                    reader.setInput(iis);
                    IIOMetadata imgData = reader.getImageMetadata(0);
                    BufferedImage bi = reader.read(0);

                    //create temporary GIF imageas output
                    File directory = new File(dir + sep);
                    outputFile = File.createTempFile("output",
                            ".gif", directory);
                    createAnimatedImage(bi, imgData,
                            writer, outputFile);
                    writer.dispose();
                    iis.close();
                    if (outputFile == null) {
                        throw new RuntimeException("Unable to create output GIF"
                                + " file");
                    }
                    // extract GIF image using Toolkit and show it on a Panel.
                    tk = Toolkit.getDefaultToolkit();
                    img = tk.getImage(dir + sep + outputFile.getName());
                    directory.delete();
                    imageCanvas.setBackground(canvasColor);
                    imageCanvas.setImage(img);
                    imageCanvas.setPreferredSize(prefImgSize);
                    canvasPanel.doLayout();

                    canvasPanel.add(imageCanvas);
                    imageFrame.add("Center", canvasPanel);
                    imageFrame.setSize(200, 200);
                    imageFrame.setVisible(true);
                    imageFrame.addWindowListener(new WindowAdapter() {
                        @Override
                        public void windowClosing(WindowEvent e) {
                            try {
                                img.flush();
                                instructionFrame.dispose();
                                imageFrame.dispose();
                                fail();
                            } finally {
                                try {
                                    fos.close();
                                    Files.delete(outputFile.toPath());
                                } catch (IOException ex) {
                                    Logger.getLogger(OpaquePNGToGIFTest.class.
                                            getName()).log(Level.SEVERE, null,
                                                    ex);
                                }
                            }
                        }
                    });
                } catch (IOException ex) {
                    Logger.getLogger(OpaquePNGToGIFTest.class.getName()).
                            log(Level.SEVERE, null, ex);
                }
            }
        });
        passButton = new JButton("Pass");
        passButton.setActionCommand("Pass");
        passButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (img != null) {
                        img.flush();
                    }
                    instructionFrame.dispose();
                    if (!startButtonClicked) {
                        fail();
                    } else {
                        imageFrame.dispose();
                        pass();
                    }
                } finally {
                    try {
                        fos.close();
                        Files.delete(outputFile.toPath());
                    } catch (IOException ex) {
                        Logger.getLogger(OpaquePNGToGIFTest.class.
                                getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });
        failButton = new JButton("Fail");
        failButton.setActionCommand("Fail");
        failButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (img != null) {
                        img.flush();
                    }
                    instructionFrame.dispose();
                    if (!startButtonClicked) {
                        fail();
                    } else {
                        imageFrame.dispose();
                        fail();
                    }
                } finally {
                    try {
                        fos.close();
                        Files.delete(outputFile.toPath());
                    } catch (IOException ex) {
                        Logger.getLogger(OpaquePNGToGIFTest.class.
                                getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });
        gbc.gridx = 1;
        gbc.gridy = 0;
        resultButtonPanel.add(startTestButton, gbc);
        gbc.gridx = 2;
        gbc.gridy = 0;
        resultButtonPanel.add(passButton, gbc);
        gbc.gridx = 3;
        gbc.gridy = 0;
        resultButtonPanel.add(failButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        mainControlPanel.add(resultButtonPanel, gbc);

        dialog.add(mainControlPanel);
        dialog.setSize(400, 200);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    if (img != null) {
                        img.flush();
                    }
                    instructionFrame.dispose();
                    if (!startButtonClicked) {
                        fail();
                    } else {
                        imageFrame.dispose();
                        fail();
                    }
                } finally {
                    try {
                        fos.close();
                        Files.delete(outputFile.toPath());
                    } catch (IOException ex) {
                        Logger.getLogger(OpaquePNGToGIFTest.class.
                                getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });
    }

    public synchronized void pass() {
        testPassed = true;
        testGeneratedInterrupt = true;
        mainThread.interrupt();
    }

    public synchronized void fail() {
        testPassed = false;
        testGeneratedInterrupt = true;
        mainThread.interrupt();
    }

    public void createAnimatedImage(BufferedImage bi, IIOMetadata metadata,
            ImageWriter writer, File outputFile) {
        try {

            fos = new FileOutputStream(outputFile);
            ImageOutputStream ios = ImageIO.createImageOutputStream(fos);
            System.out.println(ios);
            writer.setOutput(ios);

            ImageWriteParam param = writer.getDefaultWriteParam();
            IIOMetadata streamData = writer.getDefaultStreamMetadata(param);

            writer.prepareWriteSequence(streamData);
            ImageTypeSpecifier specify = new ImageTypeSpecifier(bi);
            IIOMetadata imgData = writer.convertImageMetadata(
                    (IIOMetadata) metadata, specify, param);
            GIFImageMetadata gifData = setAnimationProperties(imgData);
            IIOImage iim = new IIOImage(bi, null, gifData);
            param.setProgressiveMode(param.MODE_DISABLED);
            writer.writeToSequence(iim, param);
            writer.endWriteSequence();
            ios.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public GIFImageMetadata setAnimationProperties(IIOMetadata data) {
        ArrayList appIDs = new ArrayList();
        appIDs.add(new String("NETSCAPE").getBytes());
        ArrayList authCodes = new ArrayList();
        authCodes.add(new String("2.0").getBytes());
        ArrayList appData = new ArrayList();
        byte[] authData = {1, 0, 0};
        appData.add(authData);

        GIFImageMetadata gifData = (GIFImageMetadata) data;
        gifData.delayTime = 200;
        // If we set disposalMethod to 2 then only the issue is reproduced.
        gifData.disposalMethod = 2;
        gifData.userInputFlag = false;

        gifData.applicationIDs = appIDs;
        gifData.authenticationCodes = authCodes;
        gifData.applicationData = appData;

        return gifData;
    }

    public static void main(String args[]) throws Exception {
        OpaquePNGToGIFTest test = new OpaquePNGToGIFTest();
    }

    class ImageCanvas extends Canvas {

        Image im = null;

        public void setImage(Image img) {
            im = img;
        }

        public void clearImage() {
            im = null;
            repaint();
        }

        public void paint(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;

            if (im != null) {
                g2d.drawImage(im, 1, 1, getWidth(), getHeight(), this);
            }
        }
    }
}

