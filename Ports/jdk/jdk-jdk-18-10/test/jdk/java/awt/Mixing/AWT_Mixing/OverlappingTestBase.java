/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import java.awt.event.*;
import java.awt.peer.ComponentPeer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import javax.swing.*;

import sun.awt.AWTAccessor;
import sun.awt.EmbeddedFrame;
import java.io.*;
import test.java.awt.regtesthelpers.Util;

/**
 * <p>This class provides basis for AWT Mixing testing.
 * <p>It provides all standard test machinery and should be used by
 * extending and overriding next methods:
 * <li> {@link OverlappingTestBase#prepareControls()} - setup UI components
 * <li> {@link OverlappingTestBase#performTest()} -  run particular test
 * Those methods would be run in the loop for each AWT component.
 * <p>Current AWT component should be added to the tested UI by {@link OverlappingTestBase#propagateAWTControls(java.awt.Container) ()}.
 * There AWT components are prepared to be tested as being overlayed by other (e.g. Swing) components - they are colored to
 * {@link OverlappingTestBase#AWT_BACKGROUND_COLOR} and throws failure on catching mouse event.
 * <p> Validation of component being overlayed should be tested by {@link OverlappingTestBase#clickAndBlink(java.awt.Robot, java.awt.Point) }
 * See each method javadoc for more details.
 *
 * <p>Due to test machinery limitations all test should be run from their own main() by calling next coe
 * <code>
 *     public static void main(String args[]) throws InterruptedException {
 *        instance = new YourTestInstance();
 *        OverlappingTestBase.doMain(args);
 *     }
 * </code>
 *
 * @author Sergey Grinev
 */
public abstract class OverlappingTestBase {
    // working variables
    private static volatile boolean wasHWClicked = false;
    private static volatile boolean passed = true;
    // constants
    /**
     * Default color for AWT component used for validate correct drawing of overlapping. <b>Never</b> use it for lightweight components.
     */
    protected static final Color AWT_BACKGROUND_COLOR = new Color(21, 244, 54);
    protected static Color AWT_VERIFY_COLOR = AWT_BACKGROUND_COLOR;
    protected static final int ROBOT_DELAY = 500;
    private static final String[] simpleAwtControls = {"Button", "Checkbox", "Label", "TextArea"};
    /**
     * Generic strings array. To be used for population of List based controls.
     */
    protected static final String[] petStrings = {"Bird", "Cat", "Dog", "Rabbit", "Rhynocephalia Granda", "Bear", "Tiger", "Mustang"};
    // "properties"
    /**
     * Tests customization. Set this variable to test only control from java.awt
     * <p>Usage of this variable should be marked with CR being the reason.
     * <p>Do not use this variable simultaneously with {@link OverlappingTestBase#skipClassNames}
     */
    protected String onlyClassName = null;
    /**
     * For customizing tests. List classes' simple names to skip them from testings.
     * <p>Usage of this variable should be marked with CR being the reason.
     */
    protected String[] skipClassNames = null;
    /**
     * Set to false to avoid event delivery validation
     * @see OverlappingTestBase#clickAndBlink(java.awt.Robot, java.awt.Point, boolean)
     */
    protected boolean useClickValidation = true;
    /**
     * Set to false if test doesn't supposed to verify EmbeddedFrame
     */
    protected boolean testEmbeddedFrame = false;
    /**
     * Set this variable to true if testing embedded frame is impossible (on Mac, for instance, for now).
     * The testEmbeddedFrame is explicitly set to true in dozen places.
     */
    protected boolean skipTestingEmbeddedFrame = false;

    public static final boolean isMac = System.getProperty("os.name").toLowerCase().contains("os x");
    private boolean isFrameBorderCalculated;
    private int borderShift;

    {    if (Toolkit.getDefaultToolkit().getClass().getName().matches(".*L.*Toolkit")) {
             // No EmbeddedFrame in LWToolkit/LWCToolkit, yet
             // And it should be programmed some other way, too, in any case
             //System.err.println("skipTestingEmbeddedFrame");
             //skipTestingEmbeddedFrame = true;
         }else {
             System.err.println("do not skipTestingEmbeddedFrame");
         }
    }

    protected int frameBorderCounter() {
        if (!isFrameBorderCalculated) {
            try {
                new FrameBorderCounter(); // force compilation by jtreg
                String JAVA_HOME = System.getProperty("java.home");
                Process p = Runtime.getRuntime().exec(JAVA_HOME + "/bin/java FrameBorderCounter");
                try {
                    p.waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
                if (p.exitValue() != 0) {
                    throw new RuntimeException("FrameBorderCounter exited with not null code!\n" + readInputStream(p.getErrorStream()));
                }
                borderShift = Integer.parseInt(readInputStream(p.getInputStream()).trim());
                isFrameBorderCalculated = true;
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Problem calculating a native border size");
            }
        }
        return borderShift;
    }

    public void getVerifyColor() {
        try {
            final int size = 200;
            final Point[] p = new Point[1];
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run(){
                    JFrame frame = new JFrame("set back");
                    frame.getContentPane().setBackground(AWT_BACKGROUND_COLOR);
                    frame.setSize(size, size);
                    frame.setUndecorated(true);
                    frame.setVisible(true);
                    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                    p[0] = frame.getLocation();
                }
            });
            Robot robot = new Robot();
            robot.waitForIdle();
            Thread.sleep(ROBOT_DELAY);
            AWT_VERIFY_COLOR = robot.getPixelColor(p[0].x+size/2, p[0].y+size/2);
            System.out.println("Color will be compared with " + AWT_VERIFY_COLOR + " instead of " + AWT_BACKGROUND_COLOR);
        } catch (Exception e) {
            System.err.println("Cannot get verify color: "+e.getMessage());
        }
    }

    private String readInputStream(InputStream is) throws IOException {
        byte[] buffer = new byte[4096];
        int len = 0;
        StringBuilder sb = new StringBuilder();
        try (InputStreamReader isr = new InputStreamReader(is)) {
            while ((len = is.read(buffer)) > 0) {
                sb.append(new String(buffer, 0, len));
            }
        }
        return sb.toString();
    }

    private void setupControl(final Component control) {
        if (useClickValidation) {
            control.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    System.err.println("ERROR: " + control.getClass() + " received mouse click.");
                    wasHWClicked = true;
                }
            });
        }
        control.setBackground(AWT_BACKGROUND_COLOR);
        control.setForeground(AWT_BACKGROUND_COLOR);
        control.setPreferredSize(new Dimension(150, 150));
        control.setFocusable(false);
    }

    private void addAwtControl(java.util.List<Component> container, final Component control) {
        String simpleName = control.getClass().getSimpleName();
        if (onlyClassName != null && !simpleName.equals(onlyClassName)) {
            return;
        }
        if (skipClassNames != null) {
            for (String skipMe : skipClassNames) {
                if (simpleName.equals(skipMe)) {
                    return;
                }
            }
        }
        setupControl(control);
        container.add(control);
    }

    private void addSimpleAwtControl(java.util.List<Component> container, String className) {
        try {
            Class definition = Class.forName("java.awt." + className);
            Constructor constructor = definition.getConstructor(new Class[]{String.class});
            java.awt.Component component = (java.awt.Component) constructor.newInstance(new Object[]{"AWT Component " + className});
            addAwtControl(container, component);
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            fail("Setup error, this jdk doesn't have awt conrol " + className);
        }
    }

    /**
     * Adds current AWT control to container
     * <p>N.B.: if testEmbeddedFrame == true this method will also add EmbeddedFrame over Canvas
     * and it should be called <b>after</b> Frame.setVisible(true) call
     * @param container container to hold AWT component
     */
    protected final void propagateAWTControls(Container container) {
        if (currentAwtControl != null) {
            container.add(currentAwtControl);
        } else { // embedded frame
            try {

                //create embedder
                Canvas embedder = new Canvas();
                embedder.setBackground(Color.RED);
                embedder.setPreferredSize(new Dimension(150, 150));
                container.add(embedder);
                container.setVisible(true); // create peer

                long frameWindow = 0;
                String getWindowMethodName = null;
                String eframeClassName = null;
                if (Toolkit.getDefaultToolkit().getClass().getName().contains("XToolkit")) {
                    java.awt.Helper.addExports("sun.awt.X11", OverlappingTestBase.class.getModule());
                    getWindowMethodName = "getWindow";
                    eframeClassName = "sun.awt.X11.XEmbeddedFrame";
                }else if (Toolkit.getDefaultToolkit().getClass().getName().contains(".WToolkit")) {
                    java.awt.Helper.addExports("sun.awt.windows", OverlappingTestBase.class.getModule());
                    getWindowMethodName = "getHWnd";
                    eframeClassName = "sun.awt.windows.WEmbeddedFrame";
                }else if (isMac) {
                    java.awt.Helper.addExports("sun.lwawt", OverlappingTestBase.class.getModule());
                    java.awt.Helper.addExports("sun.lwawt.macosx", OverlappingTestBase.class.getModule());
                    eframeClassName = "sun.lwawt.macosx.CViewEmbeddedFrame";
                }

                ComponentPeer peer = AWTAccessor.getComponentAccessor()
                                                .getPeer(embedder);
                if (!isMac) {
                    Method getWindowMethod = peer.getClass().getMethod(getWindowMethodName);
                    frameWindow = (Long) getWindowMethod.invoke(peer);
                } else {
                    Method m_getPlatformWindowMethod = peer.getClass().getMethod("getPlatformWindow");
                    Object platformWindow = m_getPlatformWindowMethod.invoke(peer);
                    Class classPlatformWindow = Class.forName("sun.lwawt.macosx.CPlatformWindow");

                    Method m_getContentView = classPlatformWindow.getMethod("getContentView");
                    Object contentView = m_getContentView.invoke(platformWindow);
                    Class classContentView = Class.forName("sun.lwawt.macosx.CPlatformView");

                    Method m_getAWTView = classContentView.getMethod("getAWTView");
                    frameWindow = (Long) m_getAWTView.invoke(contentView);
                }

                Class eframeClass = Class.forName(eframeClassName);
                Constructor eframeCtor = eframeClass.getConstructor(long.class);
                EmbeddedFrame eframe = (EmbeddedFrame) eframeCtor.newInstance(frameWindow);
                setupControl(eframe);
                eframe.setSize(new Dimension(150, 150));
                eframe.setVisible(true);
//                System.err.println(eframe.getSize());
            } catch (Exception ex) {
                ex.printStackTrace();
                fail("Failed to instantiate EmbeddedFrame: " + ex.getMessage());
            }
        }
    }
    private static final Font hugeFont = new Font("Arial", Font.BOLD, 70);

    private java.util.List<Component> getAWTControls() {
        java.util.List<Component> components = new ArrayList<Component>();

        for (String clazz : simpleAwtControls) {
            addSimpleAwtControl(components, clazz);
        }

        TextField tf = new TextField();
        tf.setFont(hugeFont);
        addAwtControl(components, tf);

        // more complex controls
        Choice c = new Choice();
        for (int i = 0; i < petStrings.length; i++) {
            c.add(petStrings[i]);
        }
        addAwtControl(components, c);
        c.setPreferredSize(null);
        c.setFont(hugeFont); // to make control bigger as setPrefferedSize don't do his job here

        List l = new List(petStrings.length);
        for (int i = 0; i < petStrings.length; i++) {
            l.add(petStrings[i]);
        }
        addAwtControl(components, l);

        Canvas canvas = new Canvas();
        canvas.setSize(100, 200);
        addAwtControl(components, canvas);

        Scrollbar sb = new Scrollbar(Scrollbar.VERTICAL, 500, 1, 0, 500);
        addAwtControl(components, sb);

        Scrollbar sb2 = new Scrollbar(Scrollbar.HORIZONTAL, 500, 1, 0, 500);
        addAwtControl(components, sb2);

        return components;
    }
    /**
     * Default shift for {@link OverlappingTestBase#clickAndBlink(java.awt.Robot, java.awt.Point) }
     */
    protected static Point shift = new Point(16, 16);

    /**
     * Verifies point using specified AWT Robot. Supposes <code>defaultShift == true</code> for {@link OverlappingTestBase#clickAndBlink(java.awt.Robot, java.awt.Point, boolean) }.
     * This method is used to verify controls by providing just their plain screen coordinates.
     * @param robot AWT Robot. Usually created by {@link Util#createRobot() }
     * @param lLoc point to verify
     * @see OverlappingTestBase#clickAndBlink(java.awt.Robot, java.awt.Point, boolean)
     */
    protected void clickAndBlink(Robot robot, Point lLoc) {
        clickAndBlink(robot, lLoc, true);
    }
    /**
     * Default failure message for color check
     * @see OverlappingTestBase#performTest()
     */
    protected String failMessageColorCheck = "The LW component did not pass pixel color check and is overlapped";
    /**
     * Default failure message event check
     * @see OverlappingTestBase#performTest()
     */
    protected String failMessage = "The LW component did not received the click.";

    private static boolean isValidForPixelCheck(Component component) {
        if ((component instanceof java.awt.Scrollbar) || isMac && (component instanceof java.awt.Button)) {
            return false;
        }
        return true;
    }

    /**
     * Preliminary validation - should be run <b>before</b> overlapping happens to ensure test is correct.
     * @param robot AWT Robot. Usually created by {@link Util#createRobot() }
     * @param lLoc point to validate to be <b>of</b> {@link OverlappingTestBase#AWT_BACKGROUND_COLOR}
     * @param component tested component, should be pointed out as not all components are valid for pixel check.
     */
    protected void pixelPreCheck(Robot robot, Point lLoc, Component component) {
        if (isValidForPixelCheck(component)) {
            int tries = 10;
            Color c = null;
            while (tries-- > 0) {
                c = robot.getPixelColor(lLoc.x, lLoc.y);
                System.out.println("Precheck. color: "+c+" compare with "+AWT_VERIFY_COLOR);
                if (c.equals(AWT_VERIFY_COLOR)) {
                    return;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
            }
            System.err.println(lLoc + ": " + c);
            fail("Dropdown test setup failure, colored part of AWT component is not located at click area");
        }
    }

    /**
     * Verifies point using specified AWT Robot.
     * <p>Firstly, verifies point by color pixel check
     * <p>Secondly, verifies event delivery by mouse click
     * @param robot AWT Robot. Usually created by {@link Util#createRobot() }
     * @param lLoc point to verify
     * @param defaultShift if true verified position will be shifted by {@link OverlappingTestBase#shift }.
     */
    protected void clickAndBlink(Robot robot, Point lLoc, boolean defaultShift) {
        Point loc = lLoc.getLocation();
        //check color
        Util.waitForIdle(robot);
        try{
            Thread.sleep(500);
        }catch(Exception exx){
            exx.printStackTrace();
        }

        if (defaultShift) {
            loc.translate(shift.x, shift.y);
        }
        if (!(System.getProperty("os.name").toLowerCase().contains("os x"))) {
            Color c = robot.getPixelColor(loc.x, loc.y);
            System.out.println("C&B. color: "+c+" compare with "+AWT_VERIFY_COLOR);
            if (c.equals(AWT_VERIFY_COLOR)) {
                fail(failMessageColorCheck);
                passed = false;
            }

            // perform click
            Util.waitForIdle(robot);
        }

        robot.mouseMove(loc.x, loc.y);

        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        Util.waitForIdle(robot);
    }

    /**
     * This method should be overriden with code which setups UI for testing.
     * Code in this method <b>will</b> be called only from AWT thread so Swing operations can be called directly.
     *
     * @see {@link OverlappingTestBase#propagateAWTControls(java.awt.Container) } for instructions about adding tested AWT control to UI
     */
    protected abstract void prepareControls();

    /**
     * This method should be overriden with test execution. It will <b>not</b> be called from AWT thread so all Swing operations should be treated accordingly.
     * @return true if test passed. Otherwise fail with default fail message.
     * @see {@link OverlappingTestBase#failMessage} default fail message
     */
    protected abstract boolean performTest();
    /**
     * This method can be overriden with cleanup routines. It will be called from AWT thread so all Swing operations should be treated accordingly.
     */
    protected void cleanup() {
        // intentionally do nothing
    }
    /**
     * Currect tested AWT Control. Usually shouldn't be accessed directly.
     *
     * @see {@link OverlappingTestBase#propagateAWTControls(java.awt.Container) } for instructions about adding tested AWT control to UI
     */
    protected Component currentAwtControl;

    private void testComponent(Component component) throws InterruptedException, InvocationTargetException {
        Robot robot = null;
        try {
            robot = new Robot();
        }catch(Exception ignorex) {
        }
        currentAwtControl = component;
        System.out.println("Testing " + currentAwtControl.getClass().getSimpleName());
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                prepareControls();
            }
        });
        if (component != null) {
            Util.waitTillShown(component);
        }
        Util.waitForIdle(robot);
        try {
            Thread.sleep(500); // wait for graphic effects on systems like Win7
        } catch (InterruptedException ex) {
        }
        if (!instance.performTest()) {
            fail(failMessage);
            passed = false;
        }
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                cleanup();
            }
        });
    }

    private void testEmbeddedFrame() throws InvocationTargetException, InterruptedException {
        Robot robot = null;
        try {
            robot = new Robot();
        }catch(Exception ignorex) {
        }
        System.out.println("Testing EmbeddedFrame");
        currentAwtControl = null;
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                prepareControls();
            }
        });
        Util.waitForIdle(robot);
        try {
            Thread.sleep(500); // wait for graphic effects on systems like Win7
        } catch (InterruptedException ex) {
        }
        if (!instance.performTest()) {
            fail(failMessage);
            passed = false;
        }
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                cleanup();
            }
        });
    }

    private void testAwtControls() throws InterruptedException {
        try {
            for (Component component : getAWTControls()) {
                testComponent(component);
            }
            if (testEmbeddedFrame && !skipTestingEmbeddedFrame) {
                testEmbeddedFrame();
            }
        } catch (InvocationTargetException ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
    }
    /**
     * Used by standard test machinery. See usage at {@link OverlappingTestBase }
     */
    protected static OverlappingTestBase instance;

    protected OverlappingTestBase() {
        getVerifyColor();
    }

    /*****************************************************
     * Standard Test Machinery Section
     * DO NOT modify anything in this section -- it's a
     * standard chunk of code which has all of the
     * synchronisation necessary for the test harness.
     * By keeping it the same in all tests, it is easier
     * to read and understand someone else's test, as
     * well as insuring that all tests behave correctly
     * with the test harness.
     * There is a section following this for test-
     * classes
     ******************************************************/
    private static void init() throws InterruptedException {
        //System.setProperty("sun.awt.disableMixing", "true");

        instance.testAwtControls();

        if (wasHWClicked) {
            fail("HW component received the click.");
            passed = false;
        }
        if (passed) {
            pass();
        }
    }//End  init()
    private static boolean theTestPassed = false;
    private static boolean testGeneratedInterrupt = false;
    private static String failureMessage = "";
    private static Thread mainThread = null;
    private static int sleepTime = 300000;

    // Not sure about what happens if multiple of this test are
    //  instantiated in the same VM.  Being static (and using
    //  static vars), it aint gonna work.  Not worrying about
    //  it for now.
    /**
     * Starting point for test runs. See usage at {@link OverlappingTestBase }
     * @param args regular main args, not used.
     * @throws InterruptedException
     */
    public static void doMain(String args[]) throws InterruptedException {
        mainThread = Thread.currentThread();
        try {
            init();
        } catch (TestPassedException e) {
            //The test passed, so just return from main and harness will
            // interepret this return as a pass
            return;
        }
        //At this point, neither test pass nor test fail has been
        // called -- either would have thrown an exception and ended the
        // test, so we know we have multiple threads.

        //Test involves other threads, so sleep and wait for them to
        // called pass() or fail()
        try {
            Thread.sleep(sleepTime);
            //Timed out, so fail the test
            throw new RuntimeException("Timed out after " + sleepTime / 1000 + " seconds");
        } catch (InterruptedException e) {
            //The test harness may have interrupted the test.  If so, rethrow the exception
            // so that the harness gets it and deals with it.
            if (!testGeneratedInterrupt) {
                throw e;
            }

            //reset flag in case hit this code more than once for some reason (just safety)
            testGeneratedInterrupt = false;

            if (theTestPassed == false) {
                throw new RuntimeException(failureMessage);
            }
        }

    }//main

    /**
     * Test will fail if not passed after this timeout. Default timeout is 300 seconds.
     * @param seconds timeout in seconds
     */
    public static synchronized void setTimeoutTo(int seconds) {
        sleepTime = seconds * 1000;
    }

    /**
     * Set test as passed. Usually shoudn't be called directly.
     */
    public static synchronized void pass() {
        System.out.println("The test passed.");
        System.out.println("The test is over, hit  Ctl-C to stop Java VM");
        //first check if this is executing in main thread
        if (mainThread == Thread.currentThread()) {
            //Still in the main thread, so set the flag just for kicks,
            // and throw a test passed exception which will be caught
            // and end the test.
            theTestPassed = true;
            throw new TestPassedException();
        }
        theTestPassed = true;
        testGeneratedInterrupt = true;
        mainThread.interrupt();
    }//pass()

    /**
     * Fail test generic message.
     */
    public static synchronized void fail() {
        //test writer didn't specify why test failed, so give generic
        fail("it just plain failed! :-)");
    }

    /**
     * Fail test providing specific reason.
     * @param whyFailed reason
     */
    public static synchronized void fail(String whyFailed) {
        System.out.println("The test failed: " + whyFailed);
        System.out.println("The test is over, hit  Ctl-C to stop Java VM");
        //check if this called from main thread
        if (mainThread == Thread.currentThread()) {
            //If main thread, fail now 'cause not sleeping
            throw new RuntimeException(whyFailed);
        }
        theTestPassed = false;
        testGeneratedInterrupt = true;
        failureMessage = whyFailed;
        mainThread.interrupt();
    }//fail()
}// class LWComboBox
class TestPassedException extends RuntimeException {
}
