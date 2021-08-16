/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
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
package org.netbeans.jemmy.operators;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MenuComponent;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.dnd.DropTarget;
import java.awt.event.ComponentListener;
import java.awt.event.FocusListener;
import java.awt.event.InputMethodListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.im.InputContext;
import java.awt.im.InputMethodRequests;
import java.awt.image.ColorModel;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.beans.PropertyChangeListener;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Locale;

import org.netbeans.jemmy.CharBindingMap;
import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.ComponentSearcher;
import org.netbeans.jemmy.EventDispatcher;
import org.netbeans.jemmy.JemmyException;
import org.netbeans.jemmy.JemmyProperties;
import org.netbeans.jemmy.Outputable;
import org.netbeans.jemmy.QueueTool;
import org.netbeans.jemmy.TestOut;
import org.netbeans.jemmy.TimeoutExpiredException;
import org.netbeans.jemmy.Timeoutable;
import org.netbeans.jemmy.Timeouts;
import org.netbeans.jemmy.Waitable;
import org.netbeans.jemmy.Waiter;
import org.netbeans.jemmy.drivers.DriverManager;
import org.netbeans.jemmy.drivers.FocusDriver;
import org.netbeans.jemmy.drivers.KeyDriver;
import org.netbeans.jemmy.drivers.MouseDriver;

import javax.accessibility.AccessibleContext;

/**
 * Root class for all component operators.
 *
 * Provides basic methods to operate with mouse and keyboard.<BR>
 * <BR>
 * Almost all input methods can throw JemmyInputException or its subclass.<BR>
 *
 * ComponentOperator and its subclasses has a lot of methods which name and
 * parameters just like consistent component has. In this case operator class
 * just invokes consistent component method through AWT Event Queue
 * (invokeAndWait method).
 *
 * <BR><BR>Timeouts used: <BR>
 * ComponentOperator.PushKeyTimeout - time between key pressing and releasing
 * <BR>
 * ComponentOperator.MouseClickTimeout - time between mouse pressing and
 * releasing <BR>
 * ComponentOperator.WaitComponentTimeout - time to wait component displayed
 * <BR>
 * ComponentOperator.WaitComponentEnabledTimeout - time to wait component
 * enabled <BR>
 * ComponentOperator.BeforeDragTimeout - time to sleep before grag'n'drop
 * operations <BR>
 * ComponentOperator.AfterDragTimeout - time to sleep after grag'n'drop
 * operations <BR>
 * ComponentOperator.WaitFocusTimeout - time to wait component focus <BR>
 * ComponentOperator.WaitStateTimeout- time to wait component to be in some
 * state. Typically used from methods like
 * {@code Operator.wait"something happened"(*)}<br>.
 *
 * @see org.netbeans.jemmy.Timeouts
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 */
public class ComponentOperator extends Operator
        implements Timeoutable, Outputable {

    /**
     * Identifier for a name property.
     *
     * @see #getDump
     */
    public static final String NAME_DPROP = "Name:";

    /**
     * Identifier for a name property.
     *
     * @see #getDump
     */
    public static final String ACCESSIBLE_NAME_DPROP = "Accessible name:";

    /**
     * Identifier for a name property.
     *
     * @see #getDump
     */
    public static final String ACCESSIBLE_DESCRIPTION_DPROP = "Accessible description:";

    /**
     * Identifier for a visible property.
     *
     * @see #getDump
     */
    public static final String IS_VISIBLE_DPROP = "Visible";

    /**
     * Identifier for a showing property.
     *
     * @see #getDump
     */
    public static final String IS_SHOWING_DPROP = "Showing";

    /**
     * Identifier for a x coordinate property.
     *
     * @see #getDump
     */
    public static final String X_DPROP = "X";

    /**
     * Identifier for a y coordinate property.
     *
     * @see #getDump
     */
    public static final String Y_DPROP = "Y";

    /**
     * Identifier for a width property.
     *
     * @see #getDump
     */
    public static final String WIDTH_DPROP = "Width";

    /**
     * Identifier for a height property.
     *
     * @see #getDump
     */
    public static final String HEIGHT_DPROP = "Height";

    private static final long PUSH_KEY_TIMEOUT = 0;
    private static final long MOUSE_CLICK_TIMEOUT = 0;
    private static final long BEFORE_DRAG_TIMEOUT = 0;
    private static final long AFTER_DRAG_TIMEOUT = 0;
    private static final long WAIT_COMPONENT_TIMEOUT = 60000;
    private static final long WAIT_COMPONENT_ENABLED_TIMEOUT = 60000;
    private static final long WAIT_FOCUS_TIMEOUT = 60000;
    private static final long WAIT_STATE_TIMEOUT = 60000;

    private final Component source;
    private volatile Timeouts timeouts; // used in invokeSmoothly in clickMouse
    private volatile TestOut output; // used in QueueTool.Locker
    private volatile EventDispatcher dispatcher; // used in JInternalFrameByTitleFinder.checkComponent
    private KeyDriver kDriver;
    private MouseDriver mDriver;
    private FocusDriver fDriver;

    /**
     * Constructor.
     *
     * @param comp a component
     */
    public ComponentOperator(Component comp) {
        super();
        source = comp;
        kDriver = DriverManager.getKeyDriver(getClass());
        mDriver = DriverManager.getMouseDriver(getClass());
        fDriver = DriverManager.getFocusDriver(getClass());
        setEventDispatcher(new EventDispatcher(comp));
    }

    /**
     * Constructs a ComponentOperator object.
     *
     * @param cont container
     * @param chooser a component chooser specifying searching criteria.
     * @param index an index between appropriate ones.
     */
    public ComponentOperator(ContainerOperator<?> cont, ComponentChooser chooser, int index) {
        this(waitComponent((Container) cont.getSource(),
                chooser,
                index, cont.getTimeouts(), cont.getOutput()));
        copyEnvironment(cont);
    }

    /**
     * Constructs a ComponentOperator object.
     *
     * @param cont container
     * @param chooser a component chooser specifying searching criteria.
     */
    public ComponentOperator(ContainerOperator<?> cont, ComponentChooser chooser) {
        this(cont, chooser, 0);
    }

    /**
     * Constructor. Waits for a component in a container to show. The component
     * is iis the {@code index+1}'th {@code java.awt.Component} that
     * shows and that lies below the container in the display containment
     * hierarchy. Uses cont's timeout and output for waiting and to init
     * operator.
     *
     * @param cont Operator for a java.awt.Container.
     * @param index an index between appropriate ones.
     * @throws TimeoutExpiredException
     */
    public ComponentOperator(ContainerOperator<?> cont, int index) {
        this(cont, ComponentSearcher.getTrueChooser("Any component"), index);
    }

    /**
     * Constructor. Waits for a component in a container to show. The component
     * is is the first {@code java.awt.Component} that shows and that lies
     * below the container in the display containment hierarchy. Uses cont's
     * timeout and output for waiting and to init operator.
     *
     * @param cont Operator for a java.awt.Container.
     * @throws TimeoutExpiredException
     */
    public ComponentOperator(ContainerOperator<?> cont) {
        this(cont, 0);
    }

    /**
     * Searches Component in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @param index Ordinal component index.
     * @return Component instance or null if component was not found.
     */
    public static Component findComponent(Container cont, ComponentChooser chooser, int index) {
        return findComponent(cont, chooser, index, false);
    }

    /**
     * Searches Component in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @return Component instance or null if component was not found.
     */
    public static Component findComponent(Container cont, ComponentChooser chooser) {
        return findComponent(cont, chooser, 0);
    }

    /**
     * Waits Component in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @param index Ordinal component index.
     * @return Component instance or null if component was not found.
     * @throws TimeoutExpiredException
     */
    public static Component waitComponent(Container cont, ComponentChooser chooser, int index) {
        return (waitComponent(cont, chooser, index,
                JemmyProperties.getCurrentTimeouts(),
                JemmyProperties.getCurrentOutput()));
    }

    /**
     * Waits Component in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @return Component instance or null if component was not found.
     * @throws TimeoutExpiredException
     */
    public static Component waitComponent(Container cont, ComponentChooser chooser) {
        return waitComponent(cont, chooser, 0);
    }

    /**
     * A method to be used from subclasses. Uses {@code contOper}'s
     * timeouts and output during the waiting.
     *
     * @param contOper Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @param index Ordinal component index.
     * @return Component instance or null if component was not found.
     * @throws TimeoutExpiredException
     */
    protected static Component waitComponent(ContainerOperator<?> contOper,
            ComponentChooser chooser, int index) {
        return (waitComponent((Container) contOper.getSource(),
                chooser, index,
                contOper.getTimeouts(),
                contOper.getOutput()));
    }

    /**
     * A method to be used from subclasses. Uses timeouts and output passed as
     * parameters during the waiting.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @param index Ordinal component index.
     * @param timeouts timeouts to be used during the waiting.
     * @param output an output to be used during the waiting.
     * @return Component instance or null if component was not found.
     * @throws TimeoutExpiredException
     */
    protected static Component waitComponent(final Container cont,
            final ComponentChooser chooser,
            final int index,
            Timeouts timeouts, final TestOut output) {
        try {
            Waiter<Component, Void> waiter = new Waiter<>(new Waitable<Component, Void>() {
                @Override
                public Component actionProduced(Void obj) {
                    return findComponent(cont, new VisibleComponentFinder(chooser), index,
                            output.createErrorOutput());
                }

                @Override
                public String getDescription() {
                    return "Wait " + chooser.getDescription() + " loaded";
                }

                @Override
                public String toString() {
                    return "ComponentOperator.waitComponent.Waitable{description = " + getDescription() + '}';
                }
            });
            waiter.setTimeoutsToCloneOf(timeouts, "ComponentOperator.WaitComponentTimeout");
            waiter.setOutput(output);
            return waiter.waitAction(null);
        } catch (InterruptedException e) {
            return null;
        }
    }

    /**
     * Searches Components in container.
     *
     * @param cont Container to search components in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @return Component array or empty array if component was not found.
     */
    public static Component[] findComponents(Container cont, ComponentChooser chooser) {
        ComponentSearcher searcher = new ComponentSearcher(cont);
        return searcher.findComponents(new VisibleComponentFinder(chooser));
    }

    private static Component findComponent(Container cont, ComponentChooser chooser, int index, TestOut output) {
        ComponentSearcher searcher = new ComponentSearcher(cont);
        searcher.setOutput(output);
        return searcher.findComponent(new VisibleComponentFinder(chooser), index);
    }

    private static Component findComponent(Container cont, ComponentChooser chooser, int index, boolean supressOutout) {
        return findComponent(cont, chooser, index, JemmyProperties.getCurrentOutput().createErrorOutput());
    }

    static {
        Timeouts.initDefault("ComponentOperator.PushKeyTimeout", PUSH_KEY_TIMEOUT);
        Timeouts.initDefault("ComponentOperator.MouseClickTimeout", MOUSE_CLICK_TIMEOUT);
        Timeouts.initDefault("ComponentOperator.BeforeDragTimeout", BEFORE_DRAG_TIMEOUT);
        Timeouts.initDefault("ComponentOperator.AfterDragTimeout", AFTER_DRAG_TIMEOUT);
        Timeouts.initDefault("ComponentOperator.WaitComponentTimeout", WAIT_COMPONENT_TIMEOUT);
        Timeouts.initDefault("ComponentOperator.WaitComponentEnabledTimeout", WAIT_COMPONENT_ENABLED_TIMEOUT);
        Timeouts.initDefault("ComponentOperator.WaitStateTimeout", WAIT_STATE_TIMEOUT);
        Timeouts.initDefault("ComponentOperator.WaitFocusTimeout", WAIT_FOCUS_TIMEOUT);
    }

    /**
     * Returns component.
     */
    @Override
    public Component getSource() {
        return source;
    }

    /**
     * Returns org.netbeans.jemmy.EventDispatcher instance which is used to
     * dispatch events.
     *
     * @return the dispatcher.
     * @see org.netbeans.jemmy.EventDispatcher
     */
    public EventDispatcher getEventDispatcher() {
        return dispatcher;
    }

    ////////////////////////////////////////////////////////
    //Environment                                         //
    ////////////////////////////////////////////////////////
    @Override
    public void setOutput(TestOut out) {
        super.setOutput(out);
        this.output = out;
        if (dispatcher != null) {
            dispatcher.setOutput(output.createErrorOutput());
        }
    }

    @Override
    public TestOut getOutput() {
        return output;
    }

    @Override
    public void setTimeouts(Timeouts timeouts) {
        super.setTimeouts(timeouts);
        this.timeouts = timeouts;
        if (dispatcher != null) {
            dispatcher.setTimeouts(getTimeouts());
        }
    }

    @Override
    public Timeouts getTimeouts() {
        return timeouts;
    }

    @Override
    public void copyEnvironment(Operator anotherOperator) {
        super.copyEnvironment(anotherOperator);
        kDriver = (KeyDriver) DriverManager.
                getDriver(DriverManager.KEY_DRIVER_ID,
                        getClass(),
                        anotherOperator.getProperties());
        mDriver = (MouseDriver) DriverManager.
                getDriver(DriverManager.MOUSE_DRIVER_ID,
                        getClass(),
                        anotherOperator.getProperties());
        fDriver = (FocusDriver) DriverManager.
                getDriver(DriverManager.FOCUS_DRIVER_ID,
                        getClass(),
                        anotherOperator.getProperties());
    }

    ////////////////////////////////////////////////////////
    //Mouse operations
    ////////////////////////////////////////////////////////
    /**
     * Makes mouse click.
     *
     * @param x Horizontal click coordinate
     * @param y Vertical click coordinate
     * @param clickCount Click count
     * @param mouseButton Mouse button (InputEvent.BUTTON1/2/3_MASK value)
     * @param modifiers Modifiers (combination of InputEvent.*_MASK values)
     * @param forPopup signals that click is intended to call popup.
     */
    public void clickMouse(final int x, final int y, final int clickCount, final int mouseButton,
            final int modifiers, final boolean forPopup) {
        getQueueTool().invokeSmoothly(new QueueTool.QueueAction<Void>("Path selecting") {
            @Override
            public Void launch() {
                mDriver.clickMouse(ComponentOperator.this, x, y, clickCount, mouseButton, modifiers,
                        timeouts.create("ComponentOperator.MouseClickTimeout"));
                return null;
            }
        });
    }

    /**
     * Makes mouse click.
     *
     * @param x Horizontal click coordinate
     * @param y Vertical click coordinate
     * @param clickCount Click count
     * @param mouseButton Mouse button (InputEvent.BUTTON1/2/3_MASK value)
     * @param modifiers Modifiers (combination of InputEvent.*_MASK values)
     * @see #clickMouse(int, int, int, int, int, boolean)
     */
    public void clickMouse(int x, int y, int clickCount, int mouseButton, int modifiers) {
        clickMouse(x, y, clickCount, mouseButton, modifiers, false);
    }

    /**
     * Makes mouse click with 0 modifiers.
     *
     * @param x Horizontal click coordinate
     * @param y Vertical click coordinate
     * @param clickCount Click count
     * @param mouseButton Mouse button (InputEvent.BUTTON1/2/3_MASK value)
     * @see #clickMouse(int, int, int, int, int)
     */
    public void clickMouse(int x, int y, int clickCount, int mouseButton) {
        clickMouse(x, y, clickCount, mouseButton, 0);
    }

    /**
     * Makes mouse click by default mouse button with 0 modifiers.
     *
     * @param x Horizontal click coordinate
     * @param y Vertical click coordinate
     * @param clickCount Click count
     * @see #clickMouse(int, int, int, int)
     * @see #getDefaultMouseButton()
     */
    public void clickMouse(int x, int y, int clickCount) {
        clickMouse(x, y, clickCount, getDefaultMouseButton());
    }

    /**
     * Press mouse.
     *
     * @param x Horizontal click coordinate
     * @param y Vertical click coordinate
     */
    public void pressMouse(int x, int y) {
        mDriver.pressMouse(this, x, y, getDefaultMouseButton(), 0);
    }

    /**
     * Releases mouse.
     *
     * @param x Horizontal click coordinate
     * @param y Vertical click coordinate
     */
    public void releaseMouse(int x, int y) {
        mDriver.releaseMouse(this, x, y, getDefaultMouseButton(), 0);
    }

    /**
     * Move mouse over the component.
     *
     * @param x Horisontal destination coordinate.
     * @param y Vertical destination coordinate.
     */
    public void moveMouse(int x, int y) {
        mDriver.moveMouse(this, x, y);
    }

    /**
     * Drag mouse over the component.
     *
     * @param x Horisontal destination coordinate.
     * @param y Vertical destination coordinate.
     * @param mouseButton Mouse button
     * @param modifiers Modifiers
     */
    public void dragMouse(int x, int y, int mouseButton, int modifiers) {
        mDriver.dragMouse(this, x, y, getDefaultMouseButton(), 0);
    }

    /**
     * Drag mouse over the component with 0 modifiers.
     *
     * @param x Horisontal destination coordinate.
     * @param y Vertical destination coordinate.
     * @param mouseButton Mouse button
     * @see #dragMouse(int, int, int, int)
     */
    public void dragMouse(int x, int y, int mouseButton) {
        dragMouse(x, y, mouseButton, 0);
    }

    /**
     * Drag mouse over the component with 0 modifiers and default mose button
     * pressed.
     *
     * @param x Horisontal destination coordinate.
     * @param y Vertical destination coordinate.
     * @see #dragMouse(int, int, int)
     * @see #getDefaultMouseButton()
     */
    public void dragMouse(int x, int y) {
        dragMouse(x, y, getDefaultMouseButton());
    }

    /**
     * Makes drag'n'drop operation.
     *
     * @param start_x Start horizontal coordinate
     * @param start_y Start vertical coordinate
     * @param end_x End horizontal coordinate
     * @param end_y End vertical coordinate
     * @param mouseButton Mouse button
     * @param modifiers Modifiers
     */
    public void dragNDrop(int start_x, int start_y, int end_x, int end_y, int mouseButton, int modifiers) {
        mDriver.dragNDrop(this, start_x, start_y, end_x, end_y, mouseButton, modifiers,
                timeouts.create("ComponentOperator.BeforeDragTimeout"),
                timeouts.create("ComponentOperator.AfterDragTimeout"));
    }

    /**
     * Makes drag'n'drop operation with 0 modifiers.
     *
     * @param start_x Start horizontal coordinate
     * @param start_y Start vertical coordinate
     * @param end_x End horizontal coordinate
     * @param end_y End vertical coordinate
     * @param mouseButton Mouse button
     * @see #dragNDrop(int, int, int, int, int, int)
     */
    public void dragNDrop(int start_x, int start_y, int end_x, int end_y, int mouseButton) {
        dragNDrop(start_x, start_y, end_x, end_y, mouseButton, 0);
    }

    /**
     * Makes drag'n'drop operation by default mouse buttons with 0 modifiers.
     *
     * @param start_x Start horizontal coordinate
     * @param start_y Start vertical coordinate
     * @param end_x End horizontal coordinate
     * @param end_y End vertical coordinate
     * @see #dragNDrop(int, int, int, int, int)
     * @see #getDefaultMouseButton()
     */
    public void dragNDrop(int start_x, int start_y, int end_x, int end_y) {
        dragNDrop(start_x, start_y, end_x, end_y, getDefaultMouseButton(), 0);
    }

    /**
     * Clicks for popup.
     *
     * @param x Horizontal click coordinate.
     * @param y Vertical click coordinate.
     * @param mouseButton Mouse button.
     * @see #clickMouse(int, int, int, int, int, boolean)
     */
    public void clickForPopup(int x, int y, int mouseButton) {
        makeComponentVisible();
        clickMouse(x, y, 1, mouseButton, 0, true);
    }

    /**
     * Clicks for popup by popup mouse button.
     *
     * @param x Horizontal click coordinate.
     * @param y Vertical click coordinate.
     * @see #clickForPopup(int, int, int)
     * @see #getPopupMouseButton()
     */
    public void clickForPopup(int x, int y) {
        clickForPopup(x, y, getPopupMouseButton());
    }

    /**
     * Makes mouse click on the component center with 0 modifiers.
     *
     * @param clickCount Click count
     * @param mouseButton Mouse button (InputEvent.BUTTON1/2/3_MASK value)
     * @see #clickMouse(int, int, int, int)
     */
    public void clickMouse(final int clickCount, final int mouseButton) {
        getQueueTool().invokeSmoothly(new QueueTool.QueueAction<Void>("Clicking the mouse button") {
            @Override
            public Void launch() {
                clickMouse(getCenterXForClick(), getCenterYForClick(), clickCount, mouseButton);
                return null;
            }
        });
    }

    /**
     * Makes mouse click on the component center by default mouse button with 0
     * modifiers.
     *
     * @param clickCount Click count
     * @see #clickMouse(int, int)
     * @see #getDefaultMouseButton()
     */
    public void clickMouse(int clickCount) {
        clickMouse(clickCount, getDefaultMouseButton());
    }

    /**
     * Makes siple mouse click on the component center by default mouse button
     * with 0 modifiers.
     *
     * @see #clickMouse(int)
     * @see #getDefaultMouseButton()
     */
    public void clickMouse() {
        clickMouse(1);
    }

    /**
     * Move mouse inside the component.
     */
    public void enterMouse() {
        mDriver.enterMouse(this);
    }

    /**
     * Move mouse outside the component.
     */
    public void exitMouse() {
        mDriver.exitMouse(this);
    }

    /**
     * Press mouse.
     */
    public void pressMouse() {
        getQueueTool().invokeSmoothly(new QueueTool.QueueAction<Void>("Pressing the mouse button") {
            @Override
            public Void launch() {
                pressMouse(getCenterXForClick(), getCenterYForClick());
                return null;
            }
        });
    }

    /**
     * Releases mouse.
     */
    public void releaseMouse() {
        getQueueTool().invokeSmoothly(new QueueTool.QueueAction<Void>("Releasing the mouse button") {
            @Override
            public Void launch() {
                releaseMouse(getCenterXForClick(), getCenterYForClick());
                return null;
            }
        });
    }

    /**
     * Clicks for popup at the component center.
     *
     * @param mouseButton Mouse button.
     * @see #clickForPopup(int, int)
     */
    public void clickForPopup(int mouseButton) {
        clickForPopup(getCenterXForClick(), getCenterYForClick(), mouseButton);
    }

    /**
     * Clicks for popup by popup mouse button at the component center.
     *
     * @see #clickForPopup(int)
     * @see #getPopupMouseButton()
     */
    public void clickForPopup() {
        clickForPopup(getPopupMouseButton());
    }

    ////////////////////////////////////////////////////////
    //Keyboard operations
    ////////////////////////////////////////////////////////
    /**
     * Press key.
     *
     * @param keyCode Key code (KeyEvent.VK_* value)
     * @param modifiers Modifiers (combination of InputEvent.*_MASK fields)
     */
    public void pressKey(int keyCode, int modifiers) {
        kDriver.pressKey(this, keyCode, modifiers);
    }

    /**
     * Press key with no modifiers.
     *
     * @param keyCode Key code (KeyEvent.VK_* value)
     */
    public void pressKey(int keyCode) {
        pressKey(keyCode, 0);
    }

    /**
     * Typed key.
     *
     * @param keyChar Char to be typed.
     * @param modifiers Modifiers (combination of InputEvent.*_MASK fields)
     */
    public void typedKey(char keyChar, int modifiers) {
        kDriver.typedKey(this, getCharBindingMap().getCharKey(keyChar), keyChar, modifiers);
    }

    /**
     * Releases key.
     *
     * @param keyCode Key code (KeyEvent.VK_* value)
     * @param modifiers Modifiers (combination of InputEvent.*_MASK fields)
     */
    public void releaseKey(int keyCode, int modifiers) {
        kDriver.releaseKey(this, keyCode, modifiers);
    }

    /**
     * Releases key with no modifiers.
     *
     * @param keyCode Key code (KeyEvent.VK_* value)
     */
    public void releaseKey(int keyCode) {
        releaseKey(keyCode, 0);
    }

    /**
     * Pushs key.
     *
     * @param keyCode Key code (KeyEvent.VK_* value)
     * @param modifiers Modifiers (combination of InputEvent.*_MASK fields)
     */
    public void pushKey(int keyCode, int modifiers) {
        kDriver.pushKey(this, keyCode, modifiers, timeouts.create("ComponentOperator.PushKeyTimeout"));
    }

    /**
     * Pushs key.
     *
     * @param keyCode Key code (KeyEvent.VK_* value)
     */
    public void pushKey(int keyCode) {
        pushKey(keyCode, 0);
    }

    /**
     * Types one char.
     *
     * @param keyCode Key code (KeyEvent.VK_* value)
     * @param keyChar Char to be typed.
     * @param modifiers Modifiers (combination of InputEvent.*_MASK fields)
     */
    public void typeKey(int keyCode, char keyChar, int modifiers) {
        kDriver.typeKey(this, keyCode, keyChar, modifiers, timeouts.create("ComponentOperator.PushKeyTimeout"));
    }

    /**
     * Types one char. Uses map defined by setCharBindingMap(CharBindingMap)
     * method to find a key should be pressed.
     *
     * @param keyChar Char to be typed.
     * @param modifiers Modifiers (combination of InputEvent.*_MASK fields)
     * @see org.netbeans.jemmy.CharBindingMap
     * @see #setCharBindingMap(CharBindingMap)
     * @see #typeKey(int, char, int)
     */
    public void typeKey(char keyChar, int modifiers) {
        typeKey(getCharKey(keyChar), keyChar, modifiers | getCharModifiers(keyChar));
    }

    /**
     * Types one char. Uses map defined by setCharBindingMap(CharBindingMap)
     * method to find a key and modifiers should be pressed.
     *
     * @param keyChar Char to be typed.
     * @see #setCharBindingMap(CharBindingMap)
     * @see #typeKey(char, int)
     */
    public void typeKey(char keyChar) {
        typeKey(keyChar, 0);
    }

    ////////////////////////////////////////////////////////
    //Util
    ////////////////////////////////////////////////////////
    /**
     * Activates component's window.
     *
     * @deprecated Use makeComponentVisible() instead.
     * @see #makeComponentVisible()
     */
    @Deprecated
    public void activateWindow() {
        getVisualizer().makeVisible(this);
    }

    /**
     * Prepares component for user input. Uses visualizer defined by
     * setVisualiser() method.
     */
    public void makeComponentVisible() {
        getVisualizer().makeVisible(this);
        /*
        final ComponentOperator compOper = (ComponentOperator)this;
        runMapping(new MapVoidAction("add") {
                public void map() {
                    getVisualizer().makeVisible(compOper);
                }
            });
         */
    }

    /**
     * Gives input focus to the component.
     */
    public void getFocus() {
        fDriver.giveFocus(this);
    }

    /**
     * Return the center x coordinate.
     *
     * @return the center x coordinate.
     */
    public int getCenterX() {
        return getWidth() / 2;
    }

    /**
     * Return the center y coordinate.
     *
     * @return the center y coordinate.
     */
    public int getCenterY() {
        return getHeight() / 2;
    }

    /**
     * Return the x coordinate which should be used for mouse operations by
     * default.
     *
     * @return the center x coordinate of the visible component part.
     */
    public int getCenterXForClick() {
        return getCenterX();
    }

    /**
     * Return the y coordinate which should be used for mouse operations by
     * default.
     *
     * @return the center y coordinate of the visible component part.
     */
    public int getCenterYForClick() {
        return getCenterY();
    }

    /**
     * Waits for the component to be enabled.
     *
     * @throws TimeoutExpiredException
     * @throws InterruptedException
     */
    public void waitComponentEnabled() throws InterruptedException {
        Waiter<Component, Component> waiter = new Waiter<>(new Waitable<Component, Component>() {
            @Override
            public Component actionProduced(Component obj) {
                if (obj.isEnabled()) {
                    return obj;
                } else {
                    return null;
                }
            }

            @Override
            public String getDescription() {
                return ("Component enabled: "
                        + getSource().getClass().toString());
            }

            @Override
            public String toString() {
                return "ComponentOperator.waitComponentEnabled.Waitable{description = " + getDescription() + '}';
            }
        });
        waiter.setOutput(output);
        waiter.setTimeoutsToCloneOf(timeouts, "ComponentOperator.WaitComponentEnabledTimeout");
        waiter.waitAction(getSource());
    }

    /**
     * Waits for the component to be enabled. per request: 37831
     *
     * @throws TimeoutExpiredException
     */
    public void wtComponentEnabled() {
        try {
            waitComponentEnabled();
        } catch (InterruptedException e) {
            throw (new JemmyException("Interrupted!", e));
        }
    }

    /**
     * Returns an array of containers for this component.
     *
     * @return an array of containers
     */
    public Container[] getContainers() {
        int counter = 0;
        Container cont = getSource().getParent();
        if (cont == null) {
            return new Container[0];
        }
        do {
            counter++;
        } while ((cont = cont.getParent()) != null);
        Container[] res = new Container[counter];
        cont = getSource().getParent();
        counter = 0;
        do {
            counter++;
            res[counter - 1] = cont;
        } while ((cont = cont.getParent()) != null);
        return res;
    }

    /**
     * Searches a container.
     *
     * @param chooser a chooser specifying the searching criteria.
     * @return a containers specified by searching criteria.
     */
    public Container getContainer(ComponentChooser chooser) {
        int counter = 0;
        Container cont = getSource().getParent();
        if (cont == null) {
            return null;
        }
        do {
            if (chooser.checkComponent(cont)) {
                return cont;
            }
            counter++;
        } while ((cont = cont.getParent()) != null);
        return null;
    }

    /**
     * Searches the window under component.
     *
     * @return the component window.
     */
    public Window getWindow() {
        if (getSource() instanceof Window) {
            return (Window) getSource();
        }
        Window window = (Window) getContainer(new ComponentChooser() {
            @Override
            public boolean checkComponent(Component comp) {
                return comp instanceof Window;
            }

            @Override
            public String getDescription() {
                return "";
            }

            @Override
            public String toString() {
                return "ComponentOperator.getWindow.ComponentChooser{description = " + getDescription() + '}';
            }
        });
        if (window == null && getSource() instanceof Window) {
            return (Window) getSource();
        } else {
            return window;
        }
    }

    /**
     * Waits for this Component has the keyboard focus.
     *
     * @throws TimeoutExpiredException
     */
    public void waitHasFocus() {
        Waiter<String, Void> focusWaiter = new Waiter<>(new Waitable<String, Void>() {
            @Override
            public String actionProduced(Void obj) {
                return hasFocus() ? "" : null;
            }

            @Override
            public String getDescription() {
                return "Wait component has focus";
            }

            @Override
            public String toString() {
                return "ComponentOperator.waitHasFocus.Waitable{description = " + getDescription() + '}';
            }
        });
        focusWaiter.setTimeoutsToCloneOf(timeouts, "ComponentOperator.WaitFocusTimeout");
        focusWaiter.setOutput(output.createErrorOutput());
        try {
            focusWaiter.waitAction(null);
        } catch (InterruptedException e) {
            output.printStackTrace(e);
        }
    }

    /**
     * Waits for the component to be visible or unvisible.
     *
     * @param visibility required visiblity.
     * @throws TimeoutExpiredException
     */
    public void waitComponentVisible(final boolean visibility) {
        waitState(new ComponentChooser() {
            @Override
            public boolean checkComponent(Component comp) {
                return isVisible() == visibility;
            }

            @Override
            public String getDescription() {
                return "Component is " + (visibility ? "" : " not ") + "visible";
            }

            @Override
            public String toString() {
                return "ComponentOperator.waitComponentVisible.ComponentChooser{description = " + getDescription() + '}';
            }
        });
    }

    public void waitComponentShowing(final boolean visibility) {
        waitState(new ComponentChooser() {
            @Override
            public boolean checkComponent(Component comp) {
                return isShowing() == visibility;
            }

            @Override
            public String getDescription() {
                return "Component is " + (visibility ? "" : " not ") + "showing";
            }

            @Override
            public String toString() {
                return "ComponentOperator.waitComponentShowing.ComponentChooser{description = " + getDescription() + '}';
            }
        });
    }

    /**
     * Wait till the Size of the component becomes as expected.
     *
     * @param exactSize the exact expected size.
     */
    public void waitComponentSize(Dimension exactSize) {
        waitComponentSize(exactSize, exactSize);
    }

    /**
     * Wait till the Size of the component becomes between minSize and maxSize.
     *
     * @param minSize the minimum allowed size.
     * @param maxSize the maximum allowed size.
     */
    public void waitComponentSize(Dimension minSize, Dimension maxSize) {
        waitState(new ComponentChooser() {
            @Override
            public boolean checkComponent(Component comp) {
                Dimension componentSize = comp.getSize();
                return componentSize.height >= minSize.height
                        && componentSize.height <= maxSize.height
                        && componentSize.width >= minSize.width
                        && componentSize.width <= maxSize.width;
            }

            @Override
            public String getDescription() {
                return "Component Size becomes between: " + minSize
                        + "and " + maxSize;
            }

            @Override
            public String toString() {
                return "ComponentOperator.waitComponentSize"
                        + ".Waitable{description = " + getDescription() + '}';
            }
        });
    }

    /**
     * Wait till the component reaches exact location.
     *
     * @param exactlocation exact expected location.
     */
    public void waitComponentLocation(Point exactlocation) {
        waitComponentLocation(exactlocation, exactlocation);
    }

    /**
     * Wait till the component reaches location between minLocation and
     * maxLocation
     *
     * @param minLocation minimum expected location.
     * @param maxLocation maximum expected location.
     */
    public void waitComponentLocation(Point minLocation, Point maxLocation) {
        waitState(new ComponentChooser() {
            @Override
            public boolean checkComponent(Component comp) {
                Point componentLocation = comp.getLocation();
                return componentLocation.x >= minLocation.x
                        && componentLocation.x <= maxLocation.x
                        && componentLocation.y >= minLocation.y
                        && componentLocation.y <= maxLocation.y;
            }

            @Override
            public String getDescription() {
                return "Component reaches location between :" + minLocation
                        + "and " + maxLocation;
            }

            @Override
            public String toString() {
                return "ComponentOperator.waitComponentLocation"
                        + ".Waitable{description = " + getDescription() + '}';
            }
        });
    }

    /**
     * Wait till the component reaches exact location on screen.
     *
     * @param exactlocation exact expected screen location.
     */
    public void waitComponentLocationOnScreen(Point exactlocation) {
        waitComponentLocationOnScreen(exactlocation, exactlocation);
    }

    /**
     * Wait till the component location on screen reaches between minLocation
     * and maxLocation
     *
     * @param minLocation minimum expected location on screen.
     * @param maxLocation maximum expected location on screen.
     */
    public void waitComponentLocationOnScreen(
            final Point minLocation, final Point maxLocation) {
        waitState(new ComponentChooser() {
            @Override
            public boolean checkComponent(Component comp) {
                Point location = comp.getLocationOnScreen();
                return location.x >= minLocation.x
                        && location.x <= maxLocation.x
                        && location.y >= minLocation.y
                        && location.y <= maxLocation.y;
            }

            @Override
            public String getDescription() {
                return "Component location on screen reaches between :"
                        + minLocation + "and " + maxLocation;
            }

            @Override
            public String toString() {
                return "ComponentOperator.waitComponentLocationOnScreen"
                        + ".Waitable{description = " + getDescription() + '}';
            }
        });
    }

    /**
     * Returns information about component.
     */
    @Override
    public Hashtable<String, Object> getDump() {
        Hashtable<String, Object> result = super.getDump();
        if (getSource().getName() != null) {
            result.put(NAME_DPROP, getSource().getName());
        }
        AccessibleContext context = source.getAccessibleContext();
        if(context != null) {
            if(context.getAccessibleName() != null) {
                result.put(ACCESSIBLE_NAME_DPROP, context.getAccessibleName());
            }
            if(context.getAccessibleDescription() != null) {
                result.put(ACCESSIBLE_DESCRIPTION_DPROP, context.getAccessibleDescription());
            }
        }
        result.put(IS_VISIBLE_DPROP, getSource().isVisible() ? "true" : "false");
        result.put(IS_SHOWING_DPROP, getSource().isShowing() ? "true" : "false");
        result.put(X_DPROP, Integer.toString(getSource().getX()));
        result.put(Y_DPROP, Integer.toString(getSource().getY()));
        result.put(WIDTH_DPROP, Integer.toString(getSource().getWidth()));
        result.put(HEIGHT_DPROP, Integer.toString(getSource().getHeight()));
        return result;
    }

    ////////////////////////////////////////////////////////
    //Mapping                                             //
    /**
     * Maps {@code Component.add(PopupMenu)} through queue
     */
    public void add(final PopupMenu popupMenu) {
        runMapping(new MapVoidAction("add") {
            @Override
            public void map() {
                getSource().add(popupMenu);
            }
        });
    }

    /**
     * Maps {@code Component.addComponentListener(ComponentListener)}
     * through queue
     */
    public void addComponentListener(final ComponentListener componentListener) {
        runMapping(new MapVoidAction("addComponentListener") {
            @Override
            public void map() {
                getSource().addComponentListener(componentListener);
            }
        });
    }

    /**
     * Maps {@code Component.addFocusListener(FocusListener)} through queue
     */
    public void addFocusListener(final FocusListener focusListener) {
        runMapping(new MapVoidAction("addFocusListener") {
            @Override
            public void map() {
                getSource().addFocusListener(focusListener);
            }
        });
    }

    /**
     * Maps {@code Component.addInputMethodListener(InputMethodListener)}
     * through queue
     */
    public void addInputMethodListener(final InputMethodListener inputMethodListener) {
        runMapping(new MapVoidAction("addInputMethodListener") {
            @Override
            public void map() {
                getSource().addInputMethodListener(inputMethodListener);
            }
        });
    }

    /**
     * Maps {@code Component.addKeyListener(KeyListener)} through queue
     */
    public void addKeyListener(final KeyListener keyListener) {
        runMapping(new MapVoidAction("addKeyListener") {
            @Override
            public void map() {
                getSource().addKeyListener(keyListener);
            }
        });
    }

    /**
     * Maps {@code Component.addMouseListener(MouseListener)} through queue
     */
    public void addMouseListener(final MouseListener mouseListener) {
        runMapping(new MapVoidAction("addMouseListener") {
            @Override
            public void map() {
                getSource().addMouseListener(mouseListener);
            }
        });
    }

    /**
     * Maps {@code Component.addMouseMotionListener(MouseMotionListener)}
     * through queue
     */
    public void addMouseMotionListener(final MouseMotionListener mouseMotionListener) {
        runMapping(new MapVoidAction("addMouseMotionListener") {
            @Override
            public void map() {
                getSource().addMouseMotionListener(mouseMotionListener);
            }
        });
    }

    /**
     * Maps {@code Component.addNotify()} through queue
     */
    public void addNotify() {
        runMapping(new MapVoidAction("addNotify") {
            @Override
            public void map() {
                getSource().addNotify();
            }
        });
    }

    /**
     * Maps
     * {@code Component.addPropertyChangeListener(PropertyChangeListener)}
     * through queue
     */
    public void addPropertyChangeListener(final PropertyChangeListener propertyChangeListener) {
        runMapping(new MapVoidAction("addPropertyChangeListener") {
            @Override
            public void map() {
                getSource().addPropertyChangeListener(propertyChangeListener);
            }
        });
    }

    /**
     * Maps
     * {@code Component.addPropertyChangeListener(String, PropertyChangeListener)}
     * through queue
     */
    public void addPropertyChangeListener(final String string, final PropertyChangeListener propertyChangeListener) {
        runMapping(new MapVoidAction("addPropertyChangeListener") {
            @Override
            public void map() {
                getSource().addPropertyChangeListener(string, propertyChangeListener);
            }
        });
    }

    /**
     * Maps {@code Component.checkImage(Image, int, int, ImageObserver)}
     * through queue
     */
    public int checkImage(final Image image, final int i, final int i1, final ImageObserver imageObserver) {
        return (runMapping(new MapIntegerAction("checkImage") {
            @Override
            public int map() {
                return getSource().checkImage(image, i, i1, imageObserver);
            }
        }));
    }

    /**
     * Maps {@code Component.checkImage(Image, ImageObserver)} through queue
     */
    public int checkImage(final Image image, final ImageObserver imageObserver) {
        return (runMapping(new MapIntegerAction("checkImage") {
            @Override
            public int map() {
                return getSource().checkImage(image, imageObserver);
            }
        }));
    }

    /**
     * Maps {@code Component.contains(int, int)} through queue
     */
    public boolean contains(final int i, final int i1) {
        return (runMapping(new MapBooleanAction("contains") {
            @Override
            public boolean map() {
                return getSource().contains(i, i1);
            }
        }));
    }

    /**
     * Maps {@code Component.contains(Point)} through queue
     */
    public boolean contains(final Point point) {
        return (runMapping(new MapBooleanAction("contains") {
            @Override
            public boolean map() {
                return getSource().contains(point);
            }
        }));
    }

    /**
     * Maps {@code Component.createImage(int, int)} through queue
     */
    public Image createImage(final int i, final int i1) {
        return (runMapping(new MapAction<Image>("createImage") {
            @Override
            public Image map() {
                return getSource().createImage(i, i1);
            }
        }));
    }

    /**
     * Maps {@code Component.createImage(ImageProducer)} through queue
     */
    public Image createImage(final ImageProducer imageProducer) {
        return (runMapping(new MapAction<Image>("createImage") {
            @Override
            public Image map() {
                return getSource().createImage(imageProducer);
            }
        }));
    }

    /**
     * Maps {@code Component.dispatchEvent(AWTEvent)} through queue
     */
    public void dispatchEvent(final AWTEvent aWTEvent) {
        runMapping(new MapVoidAction("dispatchEvent") {
            @Override
            public void map() {
                getSource().dispatchEvent(aWTEvent);
            }
        });
    }

    /**
     * Maps {@code Component.doLayout()} through queue
     */
    public void doLayout() {
        runMapping(new MapVoidAction("doLayout") {
            @Override
            public void map() {
                getSource().doLayout();
            }
        });
    }

    /**
     * Maps {@code Component.enableInputMethods(boolean)} through queue
     */
    public void enableInputMethods(final boolean b) {
        runMapping(new MapVoidAction("enableInputMethods") {
            @Override
            public void map() {
                getSource().enableInputMethods(b);
            }
        });
    }

    /**
     * Maps {@code Component.getAlignmentX()} through queue
     */
    public float getAlignmentX() {
        return (runMapping(new MapFloatAction("getAlignmentX") {
            @Override
            public float map() {
                return getSource().getAlignmentX();
            }
        }));
    }

    /**
     * Maps {@code Component.getAlignmentY()} through queue
     */
    public float getAlignmentY() {
        return (runMapping(new MapFloatAction("getAlignmentY") {
            @Override
            public float map() {
                return getSource().getAlignmentY();
            }
        }));
    }

    /**
     * Maps {@code Component.getBackground()} through queue
     */
    public Color getBackground() {
        return (runMapping(new MapAction<Color>("getBackground") {
            @Override
            public Color map() {
                return getSource().getBackground();
            }
        }));
    }

    /**
     * Maps {@code Component.getBounds()} through queue
     */
    public Rectangle getBounds() {
        return (runMapping(new MapAction<Rectangle>("getBounds") {
            @Override
            public Rectangle map() {
                return getSource().getBounds();
            }
        }));
    }

    /**
     * Maps {@code Component.getBounds(Rectangle)} through queue
     */
    public Rectangle getBounds(final Rectangle rectangle) {
        return (runMapping(new MapAction<Rectangle>("getBounds") {
            @Override
            public Rectangle map() {
                return getSource().getBounds(rectangle);
            }
        }));
    }

    /**
     * Maps {@code Component.getColorModel()} through queue
     */
    public ColorModel getColorModel() {
        return (runMapping(new MapAction<ColorModel>("getColorModel") {
            @Override
            public ColorModel map() {
                return getSource().getColorModel();
            }
        }));
    }

    /**
     * Maps {@code Component.getComponentAt(int, int)} through queue
     */
    public Component getComponentAt(final int i, final int i1) {
        return (runMapping(new MapAction<Component>("getComponentAt") {
            @Override
            public Component map() {
                return getSource().getComponentAt(i, i1);
            }
        }));
    }

    /**
     * Maps {@code Component.getComponentAt(Point)} through queue
     */
    public Component getComponentAt(final Point point) {
        return (runMapping(new MapAction<Component>("getComponentAt") {
            @Override
            public Component map() {
                return getSource().getComponentAt(point);
            }
        }));
    }

    /**
     * Maps {@code Component.getComponentOrientation()} through queue
     */
    public ComponentOrientation getComponentOrientation() {
        return (runMapping(new MapAction<ComponentOrientation>("getComponentOrientation") {
            @Override
            public ComponentOrientation map() {
                return getSource().getComponentOrientation();
            }
        }));
    }

    /**
     * Maps {@code Component.getCursor()} through queue
     */
    public Cursor getCursor() {
        return (runMapping(new MapAction<Cursor>("getCursor") {
            @Override
            public Cursor map() {
                return getSource().getCursor();
            }
        }));
    }

    /**
     * Maps {@code Component.getDropTarget()} through queue
     */
    public DropTarget getDropTarget() {
        return (runMapping(new MapAction<DropTarget>("getDropTarget") {
            @Override
            public DropTarget map() {
                return getSource().getDropTarget();
            }
        }));
    }

    /**
     * Maps {@code Component.getFont()} through queue
     */
    public Font getFont() {
        return (runMapping(new MapAction<Font>("getFont") {
            @Override
            public Font map() {
                return getSource().getFont();
            }
        }));
    }

    /**
     * Maps {@code Component.getFontMetrics(Font)} through queue
     */
    public FontMetrics getFontMetrics(final Font font) {
        return (runMapping(new MapAction<FontMetrics>("getFontMetrics") {
            @Override
            public FontMetrics map() {
                return getSource().getFontMetrics(font);
            }
        }));
    }

    /**
     * Maps {@code Component.getForeground()} through queue
     */
    public Color getForeground() {
        return (runMapping(new MapAction<Color>("getForeground") {
            @Override
            public Color map() {
                return getSource().getForeground();
            }
        }));
    }

    /**
     * Maps {@code Component.getGraphics()} through queue
     */
    public Graphics getGraphics() {
        return (runMapping(new MapAction<Graphics>("getGraphics") {
            @Override
            public Graphics map() {
                return getSource().getGraphics();
            }
        }));
    }

    /**
     * Maps {@code Component.getHeight()} through queue
     */
    public int getHeight() {
        return (runMapping(new MapIntegerAction("getHeight") {
            @Override
            public int map() {
                return getSource().getHeight();
            }
        }));
    }

    /**
     * Maps {@code Component.getInputContext()} through queue
     */
    public InputContext getInputContext() {
        return (runMapping(new MapAction<InputContext>("getInputContext") {
            @Override
            public InputContext map() {
                return getSource().getInputContext();
            }
        }));
    }

    /**
     * Maps {@code Component.getInputMethodRequests()} through queue
     */
    public InputMethodRequests getInputMethodRequests() {
        return (runMapping(new MapAction<InputMethodRequests>("getInputMethodRequests") {
            @Override
            public InputMethodRequests map() {
                return getSource().getInputMethodRequests();
            }
        }));
    }

    /**
     * Maps {@code Component.getLocale()} through queue
     */
    public Locale getLocale() {
        return (runMapping(new MapAction<Locale>("getLocale") {
            @Override
            public Locale map() {
                return getSource().getLocale();
            }
        }));
    }

    /**
     * Maps {@code Component.getLocation()} through queue
     */
    public Point getLocation() {
        return (runMapping(new MapAction<Point>("getLocation") {
            @Override
            public Point map() {
                return getSource().getLocation();
            }
        }));
    }

    /**
     * Maps {@code Component.getLocation(Point)} through queue
     */
    public Point getLocation(final Point point) {
        return (runMapping(new MapAction<Point>("getLocation") {
            @Override
            public Point map() {
                return getSource().getLocation(point);
            }
        }));
    }

    /**
     * Maps {@code Component.getLocationOnScreen()} through queue
     */
    public Point getLocationOnScreen() {
        return (runMapping(new MapAction<Point>("getLocationOnScreen") {
            @Override
            public Point map() {
                return getSource().getLocationOnScreen();
            }
        }));
    }

    /**
     * Maps {@code Component.getMaximumSize()} through queue
     */
    public Dimension getMaximumSize() {
        return (runMapping(new MapAction<Dimension>("getMaximumSize") {
            @Override
            public Dimension map() {
                return getSource().getMaximumSize();
            }
        }));
    }

    /**
     * Maps {@code Component.getMinimumSize()} through queue
     */
    public Dimension getMinimumSize() {
        return (runMapping(new MapAction<Dimension>("getMinimumSize") {
            @Override
            public Dimension map() {
                return getSource().getMinimumSize();
            }
        }));
    }

    /**
     * Maps {@code Component.getName()} through queue
     */
    public String getName() {
        return (runMapping(new MapAction<String>("getName") {
            @Override
            public String map() {
                return getSource().getName();
            }
        }));
    }

    /**
     * Maps {@code Component.getParent()} through queue
     */
    public Container getParent() {
        return (runMapping(new MapAction<Container>("getParent") {
            @Override
            public Container map() {
                return getSource().getParent();
            }
        }));
    }

    /**
     * Maps {@code Component.getPreferredSize()} through queue
     */
    public Dimension getPreferredSize() {
        return (runMapping(new MapAction<Dimension>("getPreferredSize") {
            @Override
            public Dimension map() {
                return getSource().getPreferredSize();
            }
        }));
    }

    /**
     * Maps {@code Component.getSize()} through queue
     */
    public Dimension getSize() {
        return (runMapping(new MapAction<Dimension>("getSize") {
            @Override
            public Dimension map() {
                return getSource().getSize();
            }
        }));
    }

    /**
     * Maps {@code Component.getSize(Dimension)} through queue
     */
    public Dimension getSize(final Dimension dimension) {
        return (runMapping(new MapAction<Dimension>("getSize") {
            @Override
            public Dimension map() {
                return getSource().getSize(dimension);
            }
        }));
    }

    /**
     * Maps {@code Component.getToolkit()} through queue
     */
    public Toolkit getToolkit() {
        return (runMapping(new MapAction<Toolkit>("getToolkit") {
            @Override
            public Toolkit map() {
                return getSource().getToolkit();
            }
        }));
    }

    /**
     * Maps {@code Component.getTreeLock()} through queue
     */
    public Object getTreeLock() {
        return (runMapping(new MapAction<Object>("getTreeLock") {
            @Override
            public Object map() {
                return getSource().getTreeLock();
            }
        }));
    }

    /**
     * Maps {@code Component.getWidth()} through queue
     */
    public int getWidth() {
        return (runMapping(new MapIntegerAction("getWidth") {
            @Override
            public int map() {
                return getSource().getWidth();
            }
        }));
    }

    /**
     * Maps {@code Component.getX()} through queue
     */
    public int getX() {
        return (runMapping(new MapIntegerAction("getX") {
            @Override
            public int map() {
                return getSource().getX();
            }
        }));
    }

    /**
     * Maps {@code Component.getY()} through queue
     */
    public int getY() {
        return (runMapping(new MapIntegerAction("getY") {
            @Override
            public int map() {
                return getSource().getY();
            }
        }));
    }

    /**
     * Maps {@code Component.hasFocus()} through queue
     */
    public boolean hasFocus() {
        return (runMapping(new MapBooleanAction("hasFocus") {
            @Override
            public boolean map() {
                return getSource().hasFocus();
            }
        }));
    }

    /**
     * Maps {@code Component.imageUpdate(Image, int, int, int, int, int)}
     * through queue
     */
    public boolean imageUpdate(final Image image, final int i, final int i1, final int i2, final int i3, final int i4) {
        return (runMapping(new MapBooleanAction("imageUpdate") {
            @Override
            public boolean map() {
                return getSource().imageUpdate(image, i, i1, i2, i3, i4);
            }
        }));
    }

    /**
     * Maps {@code Component.invalidate()} through queue
     */
    public void invalidate() {
        runMapping(new MapVoidAction("invalidate") {
            @Override
            public void map() {
                getSource().invalidate();
            }
        });
    }

    /**
     * Maps {@code Component.isDisplayable()} through queue
     */
    public boolean isDisplayable() {
        return (runMapping(new MapBooleanAction("isDisplayable") {
            @Override
            public boolean map() {
                return getSource().isDisplayable();
            }
        }));
    }

    /**
     * Maps {@code Component.isDoubleBuffered()} through queue
     */
    public boolean isDoubleBuffered() {
        return (runMapping(new MapBooleanAction("isDoubleBuffered") {
            @Override
            public boolean map() {
                return getSource().isDoubleBuffered();
            }
        }));
    }

    /**
     * Maps {@code Component.isEnabled()} through queue
     */
    public boolean isEnabled() {
        return (runMapping(new MapBooleanAction("isEnabled") {
            @Override
            public boolean map() {
                return getSource().isEnabled();
            }
        }));
    }

    /**
     * Maps {@code Component.isFocusTraversable()} through queue
     */
    @Deprecated
    public boolean isFocusTraversable() {
        return (runMapping(new MapBooleanAction("isFocusTraversable") {
            @Override
            public boolean map() {
                return getSource().isFocusTraversable();
            }
        }));
    }

    /**
     * Maps {@code Component.isLightweight()} through queue
     */
    public boolean isLightweight() {
        return (runMapping(new MapBooleanAction("isLightweight") {
            @Override
            public boolean map() {
                return getSource().isLightweight();
            }
        }));
    }

    /**
     * Maps {@code Component.isOpaque()} through queue
     */
    public boolean isOpaque() {
        return (runMapping(new MapBooleanAction("isOpaque") {
            @Override
            public boolean map() {
                return getSource().isOpaque();
            }
        }));
    }

    /**
     * Maps {@code Component.isShowing()} through queue
     */
    public boolean isShowing() {
        return (runMapping(new MapBooleanAction("isShowing") {
            @Override
            public boolean map() {
                return getSource().isShowing();
            }
        }));
    }

    /**
     * Maps {@code Component.isValid()} through queue
     */
    public boolean isValid() {
        return (runMapping(new MapBooleanAction("isValid") {
            @Override
            public boolean map() {
                return getSource().isValid();
            }
        }));
    }

    /**
     * Maps {@code Component.isVisible()} through queue
     */
    public boolean isVisible() {
        return (runMapping(new MapBooleanAction("isVisible") {
            @Override
            public boolean map() {
                return getSource().isVisible();
            }
        }));
    }

    /**
     * Maps {@code Component.list()} through queue
     */
    public void list() {
        runMapping(new MapVoidAction("list") {
            @Override
            public void map() {
                getSource().list();
            }
        });
    }

    /**
     * Maps {@code Component.list(PrintStream)} through queue
     */
    public void list(final PrintStream printStream) {
        runMapping(new MapVoidAction("list") {
            @Override
            public void map() {
                getSource().list(printStream);
            }
        });
    }

    /**
     * Maps {@code Component.list(PrintStream, int)} through queue
     */
    public void list(final PrintStream printStream, final int i) {
        runMapping(new MapVoidAction("list") {
            @Override
            public void map() {
                getSource().list(printStream, i);
            }
        });
    }

    /**
     * Maps {@code Component.list(PrintWriter)} through queue
     */
    public void list(final PrintWriter printWriter) {
        runMapping(new MapVoidAction("list") {
            @Override
            public void map() {
                getSource().list(printWriter);
            }
        });
    }

    /**
     * Maps {@code Component.list(PrintWriter, int)} through queue
     */
    public void list(final PrintWriter printWriter, final int i) {
        runMapping(new MapVoidAction("list") {
            @Override
            public void map() {
                getSource().list(printWriter, i);
            }
        });
    }

    /**
     * Maps {@code Component.paint(Graphics)} through queue
     */
    public void paint(final Graphics graphics) {
        runMapping(new MapVoidAction("paint") {
            @Override
            public void map() {
                getSource().paint(graphics);
            }
        });
    }

    /**
     * Maps {@code Component.paintAll(Graphics)} through queue
     */
    public void paintAll(final Graphics graphics) {
        runMapping(new MapVoidAction("paintAll") {
            @Override
            public void map() {
                getSource().paintAll(graphics);
            }
        });
    }

    /**
     * Maps {@code Component.prepareImage(Image, int, int, ImageObserver)}
     * through queue
     */
    public boolean prepareImage(final Image image, final int i, final int i1, final ImageObserver imageObserver) {
        return (runMapping(new MapBooleanAction("prepareImage") {
            @Override
            public boolean map() {
                return getSource().prepareImage(image, i, i1, imageObserver);
            }
        }));
    }

    /**
     * Maps {@code Component.prepareImage(Image, ImageObserver)} through queue
     */
    public boolean prepareImage(final Image image, final ImageObserver imageObserver) {
        return (runMapping(new MapBooleanAction("prepareImage") {
            @Override
            public boolean map() {
                return getSource().prepareImage(image, imageObserver);
            }
        }));
    }

    /**
     * Maps {@code Component.print(Graphics)} through queue
     */
    public void print(final Graphics graphics) {
        runMapping(new MapVoidAction("print") {
            @Override
            public void map() {
                getSource().print(graphics);
            }
        });
    }

    /**
     * Maps {@code Component.printAll(Graphics)} through queue
     */
    public void printAll(final Graphics graphics) {
        runMapping(new MapVoidAction("printAll") {
            @Override
            public void map() {
                getSource().printAll(graphics);
            }
        });
    }

    /**
     * Maps {@code Component.remove(MenuComponent)} through queue
     */
    public void remove(final MenuComponent menuComponent) {
        runMapping(new MapVoidAction("remove") {
            @Override
            public void map() {
                getSource().remove(menuComponent);
            }
        });
    }

    /**
     * Maps {@code Component.removeComponentListener(ComponentListener)}
     * through queue
     */
    public void removeComponentListener(final ComponentListener componentListener) {
        runMapping(new MapVoidAction("removeComponentListener") {
            @Override
            public void map() {
                getSource().removeComponentListener(componentListener);
            }
        });
    }

    /**
     * Maps {@code Component.removeFocusListener(FocusListener)} through queue
     */
    public void removeFocusListener(final FocusListener focusListener) {
        runMapping(new MapVoidAction("removeFocusListener") {
            @Override
            public void map() {
                getSource().removeFocusListener(focusListener);
            }
        });
    }

    /**
     * Maps
     * {@code Component.removeInputMethodListener(InputMethodListener)}
     * through queue
     */
    public void removeInputMethodListener(final InputMethodListener inputMethodListener) {
        runMapping(new MapVoidAction("removeInputMethodListener") {
            @Override
            public void map() {
                getSource().removeInputMethodListener(inputMethodListener);
            }
        });
    }

    /**
     * Maps {@code Component.removeKeyListener(KeyListener)} through queue
     */
    public void removeKeyListener(final KeyListener keyListener) {
        runMapping(new MapVoidAction("removeKeyListener") {
            @Override
            public void map() {
                getSource().removeKeyListener(keyListener);
            }
        });
    }

    /**
     * Maps {@code Component.removeMouseListener(MouseListener)} through queue
     */
    public void removeMouseListener(final MouseListener mouseListener) {
        runMapping(new MapVoidAction("removeMouseListener") {
            @Override
            public void map() {
                getSource().removeMouseListener(mouseListener);
            }
        });
    }

    /**
     * Maps
     * {@code Component.removeMouseMotionListener(MouseMotionListener)}
     * through queue
     */
    public void removeMouseMotionListener(final MouseMotionListener mouseMotionListener) {
        runMapping(new MapVoidAction("removeMouseMotionListener") {
            @Override
            public void map() {
                getSource().removeMouseMotionListener(mouseMotionListener);
            }
        });
    }

    /**
     * Maps {@code Component.removeNotify()} through queue
     */
    public void removeNotify() {
        runMapping(new MapVoidAction("removeNotify") {
            @Override
            public void map() {
                getSource().removeNotify();
            }
        });
    }

    /**
     * Maps
     * {@code Component.removePropertyChangeListener(PropertyChangeListener)}
     * through queue
     */
    public void removePropertyChangeListener(final PropertyChangeListener propertyChangeListener) {
        runMapping(new MapVoidAction("removePropertyChangeListener") {
            @Override
            public void map() {
                getSource().removePropertyChangeListener(propertyChangeListener);
            }
        });
    }

    /**
     * Maps
     * {@code Component.removePropertyChangeListener(String, PropertyChangeListener)}
     * through queue
     */
    public void removePropertyChangeListener(final String string, final PropertyChangeListener propertyChangeListener) {
        runMapping(new MapVoidAction("removePropertyChangeListener") {
            @Override
            public void map() {
                getSource().removePropertyChangeListener(string, propertyChangeListener);
            }
        });
    }

    /**
     * Maps {@code Component.repaint()} through queue
     */
    public void repaint() {
        runMapping(new MapVoidAction("repaint") {
            @Override
            public void map() {
                getSource().repaint();
            }
        });
    }

    /**
     * Maps {@code Component.repaint(int, int, int, int)} through queue
     */
    public void repaint(final int i, final int i1, final int i2, final int i3) {
        runMapping(new MapVoidAction("repaint") {
            @Override
            public void map() {
                getSource().repaint(i, i1, i2, i3);
            }
        });
    }

    /**
     * Maps {@code Component.repaint(long)} through queue
     */
    public void repaint(final long l) {
        runMapping(new MapVoidAction("repaint") {
            @Override
            public void map() {
                getSource().repaint(l);
            }
        });
    }

    /**
     * Maps {@code Component.repaint(long, int, int, int, int)} through queue
     */
    public void repaint(final long l, final int i, final int i1, final int i2, final int i3) {
        runMapping(new MapVoidAction("repaint") {
            @Override
            public void map() {
                getSource().repaint(l, i, i1, i2, i3);
            }
        });
    }

    /**
     * Maps {@code Component.requestFocus()} through queue
     */
    public void requestFocus() {
        runMapping(new MapVoidAction("requestFocus") {
            @Override
            public void map() {
                getSource().requestFocus();
            }
        });
    }

    /**
     * Maps {@code Component.setBackground(Color)} through queue
     */
    public void setBackground(final Color color) {
        runMapping(new MapVoidAction("setBackground") {
            @Override
            public void map() {
                getSource().setBackground(color);
            }
        });
    }

    /**
     * Maps {@code Component.setBounds(int, int, int, int)} through queue
     */
    public void setBounds(final int i, final int i1, final int i2, final int i3) {
        runMapping(new MapVoidAction("setBounds") {
            @Override
            public void map() {
                getSource().setBounds(i, i1, i2, i3);
            }
        });
    }

    /**
     * Maps {@code Component.setBounds(Rectangle)} through queue
     */
    public void setBounds(final Rectangle rectangle) {
        runMapping(new MapVoidAction("setBounds") {
            @Override
            public void map() {
                getSource().setBounds(rectangle);
            }
        });
    }

    /**
     * Maps {@code Component.setComponentOrientation(ComponentOrientation)}
     * through queue
     */
    public void setComponentOrientation(final ComponentOrientation componentOrientation) {
        runMapping(new MapVoidAction("setComponentOrientation") {
            @Override
            public void map() {
                getSource().setComponentOrientation(componentOrientation);
            }
        });
    }

    /**
     * Maps {@code Component.setCursor(Cursor)} through queue
     */
    public void setCursor(final Cursor cursor) {
        runMapping(new MapVoidAction("setCursor") {
            @Override
            public void map() {
                getSource().setCursor(cursor);
            }
        });
    }

    /**
     * Maps {@code Component.setDropTarget(DropTarget)} through queue
     */
    public void setDropTarget(final DropTarget dropTarget) {
        runMapping(new MapVoidAction("setDropTarget") {
            @Override
            public void map() {
                getSource().setDropTarget(dropTarget);
            }
        });
    }

    /**
     * Maps {@code Component.setEnabled(boolean)} through queue
     */
    public void setEnabled(final boolean b) {
        runMapping(new MapVoidAction("setEnabled") {
            @Override
            public void map() {
                getSource().setEnabled(b);
            }
        });
    }

    /**
     * Maps {@code Component.setFont(Font)} through queue
     */
    public void setFont(final Font font) {
        runMapping(new MapVoidAction("setFont") {
            @Override
            public void map() {
                getSource().setFont(font);
            }
        });
    }

    /**
     * Maps {@code Component.setForeground(Color)} through queue
     */
    public void setForeground(final Color color) {
        runMapping(new MapVoidAction("setForeground") {
            @Override
            public void map() {
                getSource().setForeground(color);
            }
        });
    }

    /**
     * Maps {@code Component.setLocale(Locale)} through queue
     */
    public void setLocale(final Locale locale) {
        runMapping(new MapVoidAction("setLocale") {
            @Override
            public void map() {
                getSource().setLocale(locale);
            }
        });
    }

    /**
     * Maps {@code Component.setLocation(int, int)} through queue
     */
    public void setLocation(final int i, final int i1) {
        runMapping(new MapVoidAction("setLocation") {
            @Override
            public void map() {
                getSource().setLocation(i, i1);
            }
        });
    }

    /**
     * Maps {@code Component.setLocation(Point)} through queue
     */
    public void setLocation(final Point point) {
        runMapping(new MapVoidAction("setLocation") {
            @Override
            public void map() {
                getSource().setLocation(point);
            }
        });
    }

    /**
     * Maps {@code Component.setName(String)} through queue
     */
    public void setName(final String string) {
        runMapping(new MapVoidAction("setName") {
            @Override
            public void map() {
                getSource().setName(string);
            }
        });
    }

    /**
     * Maps {@code Component.setSize(int, int)} through queue
     */
    public void setSize(final int i, final int i1) {
        runMapping(new MapVoidAction("setSize") {
            @Override
            public void map() {
                getSource().setSize(i, i1);
            }
        });
    }

    /**
     * Maps {@code Component.setSize(Dimension)} through queue
     */
    public void setSize(final Dimension dimension) {
        runMapping(new MapVoidAction("setSize") {
            @Override
            public void map() {
                getSource().setSize(dimension);
            }
        });
    }

    /**
     * Maps {@code Component.setVisible(boolean)} through queue
     */
    public void setVisible(final boolean b) {
        runMapping(new MapVoidAction("setVisible") {
            @Override
            public void map() {
                getSource().setVisible(b);
            }
        });
    }

    /**
     * Maps {@code Component.transferFocus()} through queue
     */
    public void transferFocus() {
        runMapping(new MapVoidAction("transferFocus") {
            @Override
            public void map() {
                getSource().transferFocus();
            }
        });
    }

    /**
     * Maps {@code Component.update(Graphics)} through queue
     */
    public void update(final Graphics graphics) {
        runMapping(new MapVoidAction("update") {
            @Override
            public void map() {
                getSource().update(graphics);
            }
        });
    }

    /**
     * Maps {@code Component.validate()} through queue
     */
    public void validate() {
        runMapping(new MapVoidAction("validate") {
            @Override
            public void map() {
                getSource().validate();
            }
        });
    }

    //End of mapping                                      //
    ////////////////////////////////////////////////////////
    private void setEventDispatcher(EventDispatcher dispatcher) {
        dispatcher.setOutput(getOutput().createErrorOutput());
        dispatcher.setTimeouts(getTimeouts());
        this.dispatcher = dispatcher;
    }

    static class VisibleComponentFinder implements ComponentChooser {

        ComponentChooser subFinder;

        public VisibleComponentFinder(ComponentChooser sf) {
            subFinder = sf;
        }

        @Override
        public boolean checkComponent(Component comp) {
            if (comp.isShowing()) {
                return subFinder.checkComponent(comp);
            }
            return false;
        }

        @Override
        public String getDescription() {
            return subFinder.getDescription();
        }

        @Override
        public String toString() {
            return "VisibleComponentFinder{" + "subFinder=" + subFinder + '}';
        }
    }

}
