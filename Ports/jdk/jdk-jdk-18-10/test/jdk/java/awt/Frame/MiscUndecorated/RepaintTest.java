/*
 * Copyright (c) 1999, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @summary Make sure that on changing state of Undecorated Frame,
 *          all the components on it are repainted correctly
 * @author Jitender(jitender.singh@eng.sun.com) area=AWT
 * @author yan
 * @library /lib/client
 * @build ExtendedRobot
 * @run main RepaintTest
 */

import java.awt.*;
import java.awt.event.*;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JPanel;
import java.io.*;
import java.awt.image.*;

public class RepaintTest {
    private static int delay = 150;

    private Frame frame;
    private Container panel1, panel2;
    private Component button;
    private Component textField;
    private ExtendedRobot robot;
    private Object buttonLock = new Object();
    private boolean passed = true;
    private boolean buttonClicked = false;
    private int MAX_TOLERANCE_LEVEL = 10;

    public static void main(String[] args) {
        RepaintTest test = new RepaintTest();
        test.doTest(false);
        try {
            Toolkit.getDefaultToolkit().getSystemEventQueue().invokeAndWait(new Runnable() {
                public void run() {
                    test.frame.dispose();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Unexpected Exception occured");
        }
        test.doTest(true);
        try {
            Toolkit.getDefaultToolkit().getSystemEventQueue().invokeAndWait(new Runnable() {
                public void run() {
                    test.frame.dispose();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Unexpected Exception occured");
        }
    }

    /**
     * Do screen capture and save it as image
     */
    private static void captureScreenAndSave() {

        try {
            Robot robot = new Robot();
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Rectangle rectangle = new Rectangle(0, 0, screenSize.width, screenSize.height);
            System.out.println("About to screen capture - " + rectangle);
            BufferedImage image = robot.createScreenCapture(rectangle);
            javax.imageio.ImageIO.write(image, "jpg", new File("ScreenImage.jpg"));
            robot.delay(3000);
        } catch (Throwable t) {
            System.out.println("WARNING: Exception thrown while screen capture!");
            t.printStackTrace();
        }
    }

    private void initializeGUI(boolean swingControl) {
        frame = swingControl ? new JFrame() : new Frame();
        frame.setLayout(new BorderLayout());

        frame.setSize(300, 300);
        frame.setUndecorated(true);

        button = createButton(swingControl, (swingControl ? "Swing Button" : "AWT Button"));
        textField = swingControl ? new JTextField("TextField") : new TextField("TextField");
        panel1 = swingControl ? new JPanel() : new Panel();
        panel2 = swingControl ? new JPanel() : new Panel();
        panel1.add(button);
        panel2.add(textField);
        frame.add(panel2, BorderLayout.SOUTH);
        frame.add(panel1, BorderLayout.NORTH);

        frame.setBackground(Color.green);
        frame.setVisible(true);
        frame.toFront();
    }
    private Component createButton(boolean swingControl, String txt) {
        if(swingControl) {
            JButton jbtn = new JButton(txt);
            jbtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    buttonClicked = true;
                    synchronized (buttonLock) {
                        try {
                            buttonLock.notifyAll();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            });
            return jbtn;
        }else {
            Button btn = new Button(txt);
            btn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    buttonClicked = true;
                    synchronized (buttonLock) {
                        try {
                            buttonLock.notifyAll();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            });
            return btn;
        }
    }

    public void doTest(boolean swingControl) {
        try {
            Toolkit.getDefaultToolkit().getSystemEventQueue().invokeAndWait(new Runnable() {
                public void run() {
                    initializeGUI(swingControl);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Interrupted or unexpected Exception occured");
        }
        try {
            robot = new ExtendedRobot();
            robot.waitForIdle(1000);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Cannot create robot");
        }

        robot.mouseMove(button.getLocationOnScreen().x + button.getSize().width / 2,
                        button.getLocationOnScreen().y + button.getSize().height / 2);
        robot.waitForIdle(delay);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.waitForIdle(delay);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);

        if (! buttonClicked) {
            synchronized (buttonLock) {
                try {
                    buttonLock.wait(delay * 10);
                } catch (Exception e) {
                }
            }
        }
        if (! buttonClicked) {
            passed = false;
            System.err.println("ActionEvent not triggered when " +
                    "button is clicked!");
            throw new RuntimeException("ActionEvent not triggered");
        }

        robot.waitForIdle(delay * 5); // Need to wait until look of the button
                                      // returns to normal undepressed
        passed = paintAndRepaint(button, (swingControl? "J": "")+"Button");
        if( !paintAndRepaint(button, (swingControl? "J": "")+"TextField") ) {
            passed = false;
        }
        if(!passed) {
            throw new RuntimeException("Test failed");
        }
    }
    private boolean paintAndRepaint(Component comp, String prefix) {
        //Capture the component & compare it's dimensions
        //before iconifying & after frame comes back from
        //iconified to normal state
        System.out.println("paintAndRepaint "+prefix);
        Point p = comp.getLocationOnScreen();
        Rectangle bRect = new Rectangle((int)p.getX(), (int)p.getY(),
                                                comp.getWidth(), comp.getHeight());
        BufferedImage capturedImage = robot.createScreenCapture(bRect);

        try {
            Toolkit.getDefaultToolkit().getSystemEventQueue().invokeAndWait(new Runnable() {
                public void run() {
                    frame.setExtendedState(Frame.ICONIFIED);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Exception while setting extended state ICONIFIED");
        }
        robot.waitForIdle(delay * 5);
        try {
            Toolkit.getDefaultToolkit().getSystemEventQueue().invokeAndWait(new Runnable() {
                public void run() {
                    frame.setExtendedState(Frame.NORMAL);
                    frame.toFront();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Exception while setting extended state NORMAL");
        }
        robot.waitForIdle(delay * 5);

        if (! p.equals(comp.getLocationOnScreen())) {
            passed = false;
            System.err.println("FAIL: Frame or component did not get positioned in the same place");
        }

        p = comp.getLocationOnScreen();
        bRect = new Rectangle((int)p.getX(), (int)p.getY(),
                                  comp.getWidth(), comp.getHeight());
        BufferedImage capturedImage2 = robot.createScreenCapture(bRect);

        if (! compareImages(capturedImage, capturedImage2)) {
            passed = false;
            try {
                javax.imageio.ImageIO.write(capturedImage, "jpg", new File(
                                   prefix+"BeforeMinimize.jpg"));
                javax.imageio.ImageIO.write(capturedImage2, "jpg", new File(
                                   prefix+"AfterMinimize.jpg"));
            } catch (Exception e) {
                e.printStackTrace();
            }

            System.err.println("FAIL: The frame or component did not get repainted correctly");
        }
        return passed;
    }

    //method for comparing two images
    public boolean compareImages(BufferedImage capturedImg, BufferedImage realImg) {
        int capturedPixels[], realPixels[];
        int imgWidth, imgHeight;
        boolean comparison = true;
        int toleranceLevel = 0;

        imgWidth = capturedImg.getWidth(null);
        imgHeight = capturedImg.getHeight(null);
        capturedPixels = new int[imgWidth * imgHeight];
        realPixels = new int[imgWidth * imgHeight];

        try {
            PixelGrabber pgCapturedImg = new PixelGrabber(capturedImg, 0, 0,
                              imgWidth, imgHeight, capturedPixels, 0, imgWidth);
            pgCapturedImg.grabPixels();

            PixelGrabber pgRealImg = new PixelGrabber(realImg, 0, 0,
                              imgWidth, imgHeight, realPixels, 0, imgWidth);
            pgRealImg.grabPixels();

            for(int i=0; i<(imgWidth * imgHeight); i++) {
                if(capturedPixels[i] != realPixels[i]) {
                    toleranceLevel++;
                }
            }

            if (toleranceLevel > MAX_TOLERANCE_LEVEL) {
                comparison = false;
            }
        } catch(Exception ie) {
            ie.printStackTrace();
            comparison = false;
        }
        return comparison;
    }
}
