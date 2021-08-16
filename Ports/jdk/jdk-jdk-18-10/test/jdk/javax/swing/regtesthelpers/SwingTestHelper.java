/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
import java.beans.*;
import java.lang.reflect.*;
import java.util.*;

import javax.swing.*;

/**
 * SwingTestHelper is a utility class for writing AWT/Swing regression
 * tests that require interacting with the UI.  Typically such tests
 * consist of executing a chunk of code, waiting on an event, executing
 * more code ...  This is painful in that you typically have to use various
 * invokeLaters and threading to handle that interaction.  SwingTestHelper
 * strealines this process.
 * <p>
 * SwingTestHelper uses reflection to invoke all methods starting with
 * the name <code>onEDT</code> on the EDT and all methods starting with
 * <code>onBackgroundThread</code> on a background thread.  Between each method
 * invocation all pending events on the EDT are processed.  The methods
 * are first sorted based on an integer after the method names and invoked
 * in that order.  For example, the following subclass:
 * <pre>
 * class Test extends SwingTestHelper {
 *   private void onEDT10();
 *   private void onBackgroundThread20();
 *   private void onBackgroundThread30();
 *   private void onEDT40();
 *   private void onBackgroundThread50();
 * }
 * </pre>
 * Will have the methods invoked in the order <code>onEDT10</code>,
 * <code>onBackgroundThread20</code>, <code>onBackgroundThread30</code>,
 * <code>onEDT40</code>, <code>onBackgroundThread50</code>.
 * <p>
 * If you're not happy with method mangling you can also use annotations.
 * The following gives the same result as the previous example:
 * <pre>
 * class Test extends SwingTestHelper {
 *   &#064;Test(10)
 *   private void foo(); // Was onEDT10
 *
 *   &#064;Test(value=20, onEDT=false)
 *   private void bar(); // Was onBackgroundThread20
 *
 *   &#064;Test(value=30, onEDT=false)
 *   private void baz(); // Was onBackgroundThread30
 *
 *   &#064;Test(40)
 *   private void zed(); // Was onEDT40
 *
 *   &#064;Test(value=50, onEDT=false)
 *   private void onBackgroundThread50(); // Was onBackgroundThread50
 * }
 * </pre>
 * <p>
 * It is recommended that you increment the value in increments of
 * 10.  This makes it easier to add methods at a later date without
 * having to change all method names/annotations after the newly added
 * method.
 * <p>
 * Between each of the methods, all pending events (native and Java)
 * are processed.
 * <p>
 * Failure of the test is signaled by any method throwing
 * an exception, directly invoking <code>fail</code> or one of the
 * <code>assert</code> variants.  If no methods throw an exception the test is
 * assumed to have passed.
 * <p>
 * Often times it is necessary to block until focus has been gained on a
 * particular widget.  This can be handled by the
 * <code>requestAndWaitForFocus</code> method.  It will invoke
 * <code>requestFocus</code> and block the test (not the EDT) until focus
 * has been granted to the widget.
 * <p>
 * Care must be taken when using <code>Robot</code> directly.  For
 * example, it's tempting to flood <code>Robot</code> with events and
 * assume they will be received after some delay.  Depending upon the
 * machine you may need to increase the delay.  Instead it's
 * preferrable to block test execution until the event has been
 * received and processed.  This can be done using the method
 * <code>waitForEvent</code>.  For example, to block until a key typed
 * event has been processed do the following:
 * <pre>
 *   private void onEDT() {
 *     robot.moveMouseTo(myComponent);
 *     robot.mousePress(xxx);
 *     robot.mouseRelease(xxx);
 *     waitForEvent(myComponent, MouseEvent.MOUSE_RELEASED);
 *   }
 * </pre>
 * <p>
 * Waiting for focus and events are specific examples of a more
 * general problem.  Often times you need the EDT to continue processing
 * events, but want to block test execution until something happens.
 * In the case of focus you want to block test execution until focus
 * is gained.  The method <code>waitForCondition</code> can be used to
 * block test execution until the supplied <code>Runnable</code> returns.  The
 * <code>Runnable</code> is invoked on the background thread.
 * <p>
 * To use this class you will need to do the following:
 * <ol>
 * <li>Override the method <code>createContentPane</code>.  All of your logic
 *     for setting up the test environment should go here.  This method is
 *     invoked on the EDT.
 * <li>Implement the necessary <code>onEDTXX</code> and
 *     <code>onBackgroundThreadXXX</code> methods to do the actual testing.
 * <li>Make your <code>main</code> method look like:
 *     <code>new MySwingTestHelper().run(args)</code>.  This will block
 *     until the test fails or succeeds.
 * <li>To use this with jtreg you'll need to have something like:
 *   <pre>
 *     &#064;library ../../../regtesthelpers
 *     &#064;build Test JRobot Assert SwingTestHelper
 *     &#064;run main MySwingTestHelper
 *     * </pre>
 * </ol>
 * <p>
 * Here's a complete example:
 * <pre>
 * public class bug4852305 extends SwingTestHelper {
 *     private JTable table;
 *
 *     public static void main(String[] args) throws Throwable {
 *         new bug4852305().run(args);
 *     }
 *
 *     protected Component createContentPane() {
 *         DefaultTableModel model = new DefaultTableModel(1, 2);
 *         model.setValueAt("x", 0, 0);
 *         model.setValueAt("z", 0, 1);
 *         table = new JTable(model);
 *         table.setDefaultEditor(Object.class, new DefaultCellEditor(new JTextField()) {
 *             public boolean isCellEditable(EventObject anEvent) {
 *                 if ((anEvent instanceof KeyEvent) ||
 *                         (anEvent instanceof ActionEvent)) {
 *                     return false;
 *                 }
 *                 return true;
 *             }
 *         });
 *         return new JScrollPane(table);
 *     }
 *
 *     private void onEDT10() {
 *         requestAndWaitForFocus(table);
 *     }
 *
 *     private void onEDT20() {
 *         robot.keyPress(KeyEvent.VK_A);
 *         robot.keyRelease(KeyEvent.VK_A);
 *         waitForEvent(table, KeyEvent.KEY_RELEASED);
 *     }
 *
 *     private void onEDT30() {
 *         if (table.isEditing()) {
 *             fail("Should not be editing");
 *         }
 *     }
 * }
 * </pre>
 *
 *
 * @author Scott Violet
 */
public abstract class SwingTestHelper {
    private static final String ON_EDT_METHOD_NAME = "onEDT";
    private static final String IN_BACKGROUND_METHOD_NAME = "onBackgroundThread";

    // Whether or not we've installed a PropertyChangeListener on the
    // KeyboardFocusManager
    private boolean installedFocusListener;
    // Component currently blocking on until focus has been received.
    private Component componentWaitingForFocus;

    // Set to true when done.
    private boolean done;
    // If failed, this gives the exception.  Only the first exception is
    // kept.
    private Throwable error;

    // List of methods to invoke
    private java.util.List<Method> methods;

    // The conditions the background thread is blocked on.
    private java.util.List<Runnable> conditions;

    // Whether or not we've installed the AWTEventListener
    private boolean installedEventListener;

    /**
     * Instance of <code>Robot</code> returned from <code>createRobot</code>.
     */
    protected JRobot robot;

    /**
     * <code>Window</code> returned from <code>createWindow</code>.
     */
    protected Window window;

    // Listens for the first paint event
    private AWTEventListener paintListener;
    // Whether or not we've received a paint event.
    private boolean receivedFirstPaint;

    // used if the user wants to slow down method processing
    private PauseCondition delay = null;

    private boolean showProgress;
    private JProgressBar progBar;


    public SwingTestHelper() {
        paintListener = new AWTEventListener() {
            public void eventDispatched(AWTEvent ev) {
                if ((ev.getID() & PaintEvent.PAINT) != 0 &&
                        ev.getSource() == window) {
                    synchronized(SwingTestHelper.this) {
                        if (receivedFirstPaint) {
                            return;
                        }
                        receivedFirstPaint = true;
                    }
                    Toolkit.getDefaultToolkit().removeAWTEventListener(
                                   paintListener);
                    startControlLoop();
                }
            }
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(
            paintListener, AWTEvent.PAINT_EVENT_MASK);
    }

    /**
     * Sets whether SwingTestHelper should use {@code SunToolkit.realSync}
     * to wait for events to finish, or {@code Robot.waitForIdle}. The default
     * is to use realSync.
     * Nov 2014: no realSync any more, just robot.waitForIdle which actually
     * _is_ realSync on all platforms but OS X (and thus cannot be used on EDT).
     */
    public void setUseRealSync(boolean useRealSync) {
        //NOOP
    }

    /**
     * Set the amount of time to delay between invoking methods in
     * the control loop. Useful to slow down testing.
     */
    protected void setDelay(int delay) {
        if (delay <= 0) {
            this.delay = null;
        } else {
            this.delay = new PauseCondition(delay);
        }
    }

    /**
     * Sets whether or not progress through the list of methods is
     * shown by a progress bar at the bottom of the window created
     * by {@code createWindow}.
     */
    protected void setShowProgress(boolean showProgress) {
        this.showProgress = showProgress;
    }

    /**
     * Creates and returns the <code>Window</code> for the test.  This
     * implementation returns a JFrame with a default close operation
     * of <code>EXIT_ON_CLOSE</code>.  The <code>Component</code>
     * returned from <code>createContentPane</code> is added the
     * <code>JFrame</code> and the the frame is packed.
     * <p>
     * Typically you only need override <code>createContentPane</code>.
     */
    protected Window createWindow() {
        JFrame frame = new JFrame("Test: " + getClass().getName());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(createContentPane());
        if (showProgress) {
            progBar = new JProgressBar();
            progBar.setString("");
            progBar.setStringPainted(true);
            frame.add(progBar, BorderLayout.SOUTH);
        }
        frame.pack();
        return frame;
    }

    /**
     * Returns the <code>Component</code> to place in the frame.
     * Override this or the <code>createWindow</code> method.
     */
    protected Component createContentPane() {
        return null;
    }

    /**
     * Invokes <code>requestFocus</code> on the passed in component (assuming
     * it doesn't already have focus).  Test execution is blocked until focus
     * has been gained on the component.  This method <b>must</b> be invoked
     * on the EDT, if you do not invoke it from the edt the test will fail.
     *
     * @param c the <code>Component</code> to wait for focus on
     */
    protected void requestAndWaitForFocus(Component c) {
        requestAndWaitForFocus(c, true);
    }

    /**
     * Blocks test execution until focus is gained on the component.
     * This method <b>must</b> be invoked
     * on the EDT, if you do not invoke it from the edt the test will fail.
     */
    protected void waitForFocusGained(Component c) {
        requestAndWaitForFocus(c, false);
    }

    private void requestAndWaitForFocus(Component c, boolean requestFocus) {
        if (!EventQueue.isDispatchThread()) {
            System.out.println(
                "requestAndWaitForFocus should be invoked on EDT");
            throw new RuntimeException();
        }
        if (componentWaitingForFocus != null) {
            System.out.println("Already waiting for focus");
            throw new RuntimeException();
        }
        if (!installedFocusListener) {
            installedFocusListener = true;
            KeyboardFocusManager.getCurrentKeyboardFocusManager().
                addPropertyChangeListener(new FocusListener());
        }
        synchronized(this) {
            if (c.hasFocus()) {
                return;
            }
            componentWaitingForFocus = c;
        }
        if (requestFocus) {
            c.requestFocus();
        }
        waitForCondition(new FocusCondition());
    }

    /**
     * Blocks test execution until the specified event has been received.
     * This method immediately returns and the EDT will continue to
     * process events, but test execution is blocked until
     * the event is received.
     *
     * @param event the event type to wait for
     */
    protected void waitForEvent(int event) {
        waitForEvent(null, event);
    }

    /**
     * Blocks test execution until the specified event has been received.
     * This method immediately returns and the EDT will continue to
     * process events, but test execution is blocked until
     * the event is received.
     *
     * @param target the <code>Component</code> to wait for the event on;
     *               <code>null</code> indicates it does not matter which
     *               component the event is received on
     * @param event the event type to wait for
     */
    protected void waitForEvent(Component target, int event) {
        waitForCondition(new EventCondition(target, event));
        if (!installedEventListener) {
            installedEventListener = true;
            Toolkit.getDefaultToolkit().addAWTEventListener(
                    new EventListener(), 0xFFFFFFFFFFFFFFFFl);
        }
    }

    /**
     * Paused test execution for the specified amount of time.  The caller
     * immediately returns and the EDT can process events.
     *
     * @param time the amount of time, in milliseconds, to pause for
     */
    protected void pause(int time) {
        waitForCondition(new PauseCondition(time));
    }

    /**
     * Schedules a <code>Runnable</code> that will be processed in the
     * background thread.  This method immediately returns, and the
     * EDT is free to continue processing events. Test execution is
     * blocked until the <code>Runnable</code> completes.
     */
    protected void waitForCondition(Runnable runnable) {
        synchronized(this) {
            if (conditions == null) {
                conditions = new LinkedList<Runnable>();
            }
            conditions.add(runnable);
        }
    }

    /**
     * Runs the test.  This method blocks the caller until the test
     * fails or succeeds. Recognized arguments are:
     * <p>
     * "-exit": Causes main to exit when the test is done.
     * "-showProg": Indicate the progress of the test with a
     *              progress bar in the main window. Only works
     *              if the test hasn't overridden {@code createWindow}.
     * "-delay int": Sets the delay between executing methods.
     *               Useful when you want to slow a test to watch it.
     *
     * @param args the arguments from main, it's ok to pass in null
     */
    protected final void run(String[] args) throws Throwable {
        boolean exit = false;
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-exit")) {
                    exit = true;
                } else if (args[i].equals("-delay")) {
                    try {
                        setDelay(Integer.parseInt(args[++i]));
                    } catch (NumberFormatException ne) {
                        throw new RuntimeException("-delay requires an integer value");
                    } catch (ArrayIndexOutOfBoundsException ae) {
                        throw new RuntimeException("-delay requires an integer value");
                    }
                } else if (args[i].equals("-showProg")) {
                    setShowProgress(true);
                } else {
                    throw new RuntimeException("Invalid argument \"" + args[i] + "\"");
                }
            }
        }

        createWindow0();
        synchronized(this) {
            while(!done) {
                wait();
            }
        }
        if (exit) {
            // Not in harness
            if (error != null) {
                System.out.println("FAILED: " + error);
                error.printStackTrace();
            }
            System.exit(0);
        }
        if (error != null) {
            throw error;
        }
    }

    /**
     * Creates the window, on the EDT.
     */
    private void createWindow0() {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                window = createWindow();
                window.show();
            }
        });
    }

    /**
     * Initializes the progress bar if necessary.
     */
    private void initProgressBar(final int size) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                if (progBar != null) {
                    progBar.setMaximum(size);
                }
            }
        });
    }

    /**
     * Starst the control loop.
     */
    private void startControlLoop() {
        robot = createRobot();
        if (robot != null) {
            calculateMethods();
            initProgressBar(methods.size());
            new Thread(new Runnable() {
                public void run() {
                    controlLoop();
                }
            }).start();
        }
    }

    /**
     * Increment the progress bar.
     */
    private void nextProgress(final String name) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                if (progBar != null) {
                    progBar.setString(name);
                    progBar.setValue(progBar.getValue() + 1);
                }
            }
        });
    }

    private synchronized Runnable currentCondition() {
        if (conditions != null && conditions.size() > 0) {
            return conditions.get(0);
        }
        return null;
    }

    private synchronized Runnable nextCondition() {
        return conditions.remove(0);
    }

    private void controlLoop() {
        int methodIndex = 0;
        while (methodIndex < methods.size()) {
            // Wait for any pending conditions
            Runnable condition;
            while ((condition = currentCondition()) != null) {
                try {
                    condition.run();
                } catch (Exception e) {
                    fail(e);
                    return;
                }
                waitForEDTToFinish();
                synchronized(this) {
                    if (done) {
                        return;
                    }
                }
                // Advance to next condition
                nextCondition();
            }

            // Let all events on the EDT finish
            waitForEDTToFinish();

            if (delay != null) {
                delay.run();
            }

            // Invoke the next method
            Method method = methods.get(methodIndex++);
            Test test = method.getAnnotation(Test.class);
            boolean onEDT = true;
            if (test != null) {
                onEDT = test.onEDT();
            }
            else if (!method.getName().startsWith(ON_EDT_METHOD_NAME)) {
                onEDT = false;
            }
            if (onEDT) {
                invokeOnEDT(method);
            }
            else {
                invoke(method);
            }

            // Let all events on the EDT finish
            waitForEDTToFinish();

            nextProgress(method.getName());

            // If done, stop.
            synchronized(this) {
                if (done) {
                    return;
                }
            }
        }

        // No more methods, if we get and done isn't true, set it true
        // so that the main thread wakes up.
        synchronized(this) {
            if (!done) {
                done = true;
                notifyAll();
            }
        }
    }

    private void waitForEDTToFinish() {
            robot.waitForIdle();
        }

    private void invokeOnEDT(final Method method) {
        try {
            EventQueue.invokeAndWait(new Runnable() {
                public void run() {
                    invoke(method);
                }
            });
        } catch (InvocationTargetException ite) {
            fail(ite);
        } catch (InterruptedException ie) {
            fail(ie);
        }
    }

    private void invoke(Method method) {
        System.out.println("invoking: " + method.getName());
        try {
            if (Modifier.isPrivate(method.getModifiers())) {
                method.setAccessible(true);
            }
            method.invoke(this);
        } catch (Exception e) {
            fail(e);
        }
    }

    // Determines the methods to execute.
    private void calculateMethods() {
        // Using a Set avoids duplicating methods returned by both
        // getMethods() and getDeclaredMethods().
        HashSet<Method> allMethods = new HashSet<Method>();
        allMethods.addAll(Arrays.asList(getClass().getMethods()));
        allMethods.addAll(Arrays.asList(getClass().getDeclaredMethods()));

        methods = new ArrayList<Method>();
        for (Method method : allMethods) {
            Test test = method.getAnnotation(Test.class);
            if (test != null) {
                methods.add(method);
            }
            else if (method.getName().startsWith(ON_EDT_METHOD_NAME)) {
                methods.add(method);
            }
            else if (method.getName().startsWith(IN_BACKGROUND_METHOD_NAME)) {
                methods.add(method);
            }
        }
        Comparator<Method> comparator = new Comparator<Method>() {
            public int compare(Method m1, Method m2) {
                int index1 = getIndex(m1);
                int index2 = getIndex(m2);
                return index1 - index2;
            }
            private int getIndex(Method m) {
                String name = m.getName();
                String indexAsString;
                Test test = m.getAnnotation(Test.class);
                if (test != null) {
                    return test.value();
                }
                if (name.startsWith(ON_EDT_METHOD_NAME)) {
                    indexAsString = name.substring(
                            ON_EDT_METHOD_NAME.length());
                }
                else {
                    indexAsString = name.substring(
                            IN_BACKGROUND_METHOD_NAME.length());
                }
                if (indexAsString.length() == 0) {
                    System.out.println(
                            "onEDT and onBackgroundThread must be " +
                            "followed by an integer specifying " +
                            "order.");
                    System.exit(0);
                }
                return Integer.parseInt(indexAsString);
            }
        };
        Collections.sort(methods, comparator);
    }

    /**
     * Invoke if the test should be considered to have failed.  This will
     * stop test execution.
     */
    public void fail(String reason) {
        fail(new RuntimeException(reason));
    }

    /**
     * Invoke if the test should be considered to have failed.  This will
     * stop test execution.
     */
    public void fail(Throwable error) {
        synchronized(this) {
            if (this.error == null) {
                if (error instanceof InvocationTargetException) {
                    this.error = ((InvocationTargetException)error).
                            getCause();
                }
                else {
                    this.error = error;
                }
                this.done = true;
                notifyAll();
            }
        }
    }

    /**
     * Invoke to prematurely stop test execution while there are remaining
     * methods.  You typically don't invoke this, instead if all methods have
     * been executed and fail hasn't been invoked, the test is considered to
     * have passed.
     */
    public void succeeded() {
        synchronized(this) {
            this.done = true;
            notifyAll();
        }
    }

    /**
     * Creates and returns the Robot that will be used.  You generally don't
     * need to override this.
     */
    protected JRobot createRobot() {
        JRobot robot = JRobot.getRobot(false);
        return robot;
    }


    private class FocusListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent e) {
            if (componentWaitingForFocus != null &&
                    "focusOwner".equals(e.getPropertyName()) &&
                    componentWaitingForFocus == e.getNewValue()) {
                synchronized(SwingTestHelper.this) {
                    componentWaitingForFocus = null;
                    SwingTestHelper.this.notifyAll();
                }
            }
        }
    }


    private class EventCondition implements Runnable {
        private Component component;
        private int eventID;
        private boolean received;

        EventCondition(Component component, int eventID) {
            this.component = component;
            this.eventID = eventID;
        }

        public int getEventID() {
            return eventID;
        }

        public Component getComponent() {
            return component;
        }

        public void received() {
            synchronized(SwingTestHelper.this) {
                this.received = true;
                SwingTestHelper.this.notifyAll();
            }
        }

        public boolean isWaiting() {
            return !received;
        }

        public void run() {
            synchronized(SwingTestHelper.this) {
                while (!received) {
                    try {
                        SwingTestHelper.this.wait();
                    } catch (InterruptedException ie) {
                        fail(ie);
                    }
                }
            }
        }
    }


    private class FocusCondition implements Runnable {
        public void run() {
            synchronized(SwingTestHelper.this) {
                while (componentWaitingForFocus != null) {
                    try {
                        SwingTestHelper.this.wait();
                    } catch (InterruptedException ie) {
                        fail(ie);
                    }
                }
            }
        }
    }


    private class PauseCondition implements Runnable {
        private int time;
        PauseCondition(int time) {
            this.time = time;
        }
        public void run() {
            try {
                Thread.sleep(time);
            } catch (InterruptedException ie) {
                fail(ie);
            }
        }
    }


    private class EventListener implements AWTEventListener {
        public void eventDispatched(AWTEvent ev) {
            int eventID = ev.getID();
            synchronized (SwingTestHelper.this) {
                for (Runnable condition : conditions) {
                    if (condition instanceof EventCondition) {
                        EventCondition ec = (EventCondition)condition;
                        if (ec.isWaiting()) {
                            if (eventID == ec.getEventID() &&
                                    (ec.getComponent() == null ||
                                     ev.getSource() == ec.getComponent())) {
                                ec.received();
                            }
                            return;
                        }
                    }
                    else {
                        return;
                    }
                }
            }
        }
    }
}
