/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyVetoException;
import java.util.Hashtable;

import javax.swing.Icon;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JInternalFrame.JDesktopIcon;
import javax.swing.JLayeredPane;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.event.InternalFrameListener;
import javax.swing.plaf.InternalFrameUI;

import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.ComponentSearcher;
import org.netbeans.jemmy.JemmyInputException;
import org.netbeans.jemmy.JemmyProperties;
import org.netbeans.jemmy.Outputable;
import org.netbeans.jemmy.TestOut;
import org.netbeans.jemmy.TimeoutExpiredException;
import org.netbeans.jemmy.Timeoutable;
import org.netbeans.jemmy.Timeouts;
import org.netbeans.jemmy.drivers.DriverManager;
import org.netbeans.jemmy.drivers.FrameDriver;
import org.netbeans.jemmy.drivers.InternalFrameDriver;
import org.netbeans.jemmy.drivers.WindowDriver;
import org.netbeans.jemmy.util.EmptyVisualizer;
import org.netbeans.jemmy.util.LookAndFeel;
import org.netbeans.jemmy.util.Platform;

/**
 * Class provides necessary functionality to operate with
 * javax.swing.JInternalFrame component.
 *
 * Some methods can throw WrongInternalFrameStateException exception.
 *
 * <BR><BR>Timeouts used: <BR>
 * ComponentOperator.WaitComponentTimeout - time to wait component displayed
 * <BR>
 * ComponentOperator.MouseClickTimeout - time between mouse pressing and
 * releasing <BR>
 * AbstractButtonOperator.PushButtonTimeout - time between button pressing and
 * releasing<BR>
 * JScrollBarOperator.WholeScrollTimeout - time for the whole scrolling <BR>.
 *
 * @see org.netbeans.jemmy.Timeouts
 * @see WrongInternalFrameStateException
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 */
public class JInternalFrameOperator extends JComponentOperator
        implements Outputable, Timeoutable {

    /**
     * Identifier for a "title" property.
     *
     * @see #getDump
     */
    public static final String TITLE_DPROP = "Title";

    /**
     * Identifier for a "state" property.
     *
     * @see #getDump
     */
    public static final String STATE_DPROP = "State";

    /**
     * Identifier for a "normal" value of "state" property.
     *
     * @see #getDump
     */
    public static final String STATE_NORMAL_DPROP_VALUE = "NORMAL";

    /**
     * Identifier for a "closed" value of "state" property.
     *
     * @see #getDump
     */
    public static final String STATE_CLOSED_DPROP_VALUE = "CLOSED";

    /**
     * Identifier for a "iconified" value of "state" property.
     *
     * @see #getDump
     */
    public static final String STATE_ICONIFIED_DPROP_VALUE = "ICONIFIED";

    /**
     * Identifier for a "maximized" value of "state" property.
     *
     * @see #getDump
     */
    public static final String STATE_MAXIMAZED_DPROP_VALUE = "MAXIMIZED";

    /**
     * Identifier for a "resizable" property.
     *
     * @see #getDump
     */
    public static final String IS_RESIZABLE_DPROP = "Resizable";

    /**
     * Identifier for a "selected" property.
     *
     * @see #getDump
     */
    public static final String IS_SELECTED_DPROP = "Selected";

    /**
     * Maximize button tool tip key
     */
    public static final String MAXIMIZE_BUTTON_TOOLTIP =
            UIManager.getString("InternalFrame.maxButtonToolTip");

    /**
     * Close button tool tip key
     */
    public static final String CLOSE_BUTTON_TOOLTIP =
            UIManager.getString("InternalFrame.closeButtonToolTip");

    /**
     * Minimize button tool tip key
     */
    public static final String MINIMIZE_BUTTON_TOOLTIP =
            UIManager.getString("InternalFrame.iconButtonToolTip");

    /**
     * A minimizing button.
     */
    protected JButtonOperator minOper = null;

    /**
     * A maximizing button.
     */
    protected JButtonOperator maxOper = null;

    /**
     * A close button.
     */
    protected JButtonOperator closeOper = null;

    protected JButtonOperator popupButtonOper = null;

    /**
     * A title operator.
     */
    protected ContainerOperator<?> titleOperator = null;
    private TestOut output;
    private Timeouts timeouts;
    private JDesktopIconOperator iconOperator;

    WindowDriver wDriver;
    FrameDriver fDriver;
    InternalFrameDriver iDriver;

    /**
     * Constructor.
     *
     * @param b a component
     */
    public JInternalFrameOperator(JInternalFrame b) {
        super(b);
        wDriver = DriverManager.getWindowDriver(getClass());
        fDriver = DriverManager.getFrameDriver(getClass());
        iDriver = DriverManager.getInternalFrameDriver(getClass());
    }

    /**
     * Constructs a JInternalFrameOperator object.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     * @param index an index between appropriate ones.
     */
    public JInternalFrameOperator(ContainerOperator<?> cont, ComponentChooser chooser, int index) {
        this(waitJInternalFrame((Container)cont.getSource(),
                chooser, index));
        copyEnvironment(cont);
    }

    /**
     * Constructs a JInternalFrameOperator object.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     */
    public JInternalFrameOperator(ContainerOperator<?> cont, ComponentChooser chooser) {
        this(cont, chooser, 0);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont a container
     * @param text Button text.
     * @param index Ordinal component index.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     *
     */
    public JInternalFrameOperator(ContainerOperator<?> cont, String text, int index) {
        this(findOne(cont, text, index));
        copyEnvironment(cont);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont a container
     * @param text Button text.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     *
     */
    public JInternalFrameOperator(ContainerOperator<?> cont, String text) {
        this(cont, text, 0);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont a container
     * @param index Ordinal component index.
     *
     */
    public JInternalFrameOperator(ContainerOperator<?> cont, int index) {
        this(waitJInternalFrame((Container)cont.getSource(),
                new JInternalFrameFinder(),
                index));
        copyEnvironment(cont);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont a container
     *
     */
    public JInternalFrameOperator(ContainerOperator<?> cont) {
        this(cont, 0);
    }

    /**
     * Searches JInternalframe in container.
     *
     * @param cont Container to search component in.
     * @param chooser a component chooser specifying searching criteria.
     * @param index Ordinal component index.
     * @return JInternalframe instance or null if component was not found.
     */
    public static JInternalFrame findJInternalFrame(Container cont, ComponentChooser chooser, int index) {
        Component res = findComponent(cont, new JInternalFrameFinder(chooser), index);
        if (res instanceof JInternalFrame) {
            return (JInternalFrame) res;
        } else if (res instanceof JInternalFrame.JDesktopIcon) {
            return ((JInternalFrame.JDesktopIcon) res).getInternalFrame();
        } else {
            return null;
        }
    }

    /**
     * Searches JInternalframe in container.
     *
     * @param cont Container to search component in.
     * @param chooser a component chooser specifying searching criteria.
     * @return JInternalframe instance or null if component was not found.
     */
    public static JInternalFrame findJInternalFrame(Container cont, ComponentChooser chooser) {
        return findJInternalFrame(cont, chooser, 0);
    }

    /**
     * Searches JInternalframe by title.
     *
     * @param cont Container to search component in.
     * @param text Component text.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @param index Ordinal component index.
     * @return JInternalframe instance or null if component was not found.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     */
    public static JInternalFrame findJInternalFrame(Container cont, String text, boolean ce, boolean ccs, int index) {
        return (findJInternalFrame(cont,
                new JInternalFrameByTitleFinder(text,
                        new DefaultStringComparator(ce, ccs)),
                index));
    }

    /**
     * Searches JInternalframe by title.
     *
     * @param cont Container to search component in.
     * @param text Component text.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @return JInternalframe instance or null if component was not found.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     */
    public static JInternalFrame findJInternalFrame(Container cont, String text, boolean ce, boolean ccs) {
        return findJInternalFrame(cont, text, ce, ccs, 0);
    }

    /**
     * Searches JInternalFrame object which component lies on.
     *
     * @param comp Component to find JInternalFrame under.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @return JInternalFrame instance or null if component was not found.
     */
    public static JInternalFrame findJInternalFrameUnder(Component comp, ComponentChooser chooser) {
        return (JInternalFrame) findContainerUnder(comp, new JInternalFrameFinder(chooser));
    }

    /**
     * Searches JInternalFrame object which component lies on.
     *
     * @param comp Component to find JInternalFrame under.
     * @return JInternalFrame instance or null if component was not found.
     */
    public static JInternalFrame findJInternalFrameUnder(Component comp) {
        return findJInternalFrameUnder(comp, new JInternalFrameFinder());
    }

    /**
     * Waits JInternalframe in container.
     *
     * @param cont Container to search component in.
     * @param chooser a component chooser specifying searching criteria.
     * @param index Ordinal component index.
     * @return JInternalframe instance.
     *
     */
    public static JInternalFrame waitJInternalFrame(final Container cont, final ComponentChooser chooser, final int index) {
        Component res = waitComponent(cont, new JInternalFrameFinder(chooser), index);
        if (res instanceof JInternalFrame) {
            return (JInternalFrame) res;
        } else if (res instanceof JInternalFrame.JDesktopIcon) {
            return ((JInternalFrame.JDesktopIcon) res).getInternalFrame();
        } else {
            throw (new TimeoutExpiredException(chooser.getDescription()));
        }
    }

    /**
     * Waits JInternalframe in container.
     *
     * @param cont Container to search component in.
     * @param chooser a component chooser specifying searching criteria.
     * @return JInternalframe instance.
     *
     */
    public static JInternalFrame waitJInternalFrame(Container cont, ComponentChooser chooser) {
        return waitJInternalFrame(cont, chooser, 0);
    }

    /**
     * Waits JInternalframe by title.
     *
     * @param cont Container to search component in.
     * @param text Component text.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @param index Ordinal component index.
     * @return JInternalframe instance.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     *
     */
    public static JInternalFrame waitJInternalFrame(Container cont, String text, boolean ce, boolean ccs, int index) {
        return (waitJInternalFrame(cont,
                new JInternalFrameByTitleFinder(text,
                        new DefaultStringComparator(ce, ccs)),
                index));
    }

    /**
     * Waits JInternalframe by title.
     *
     * @param cont Container to search component in.
     * @param text Component text.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @return JInternalframe instance.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     *
     */
    public static JInternalFrame waitJInternalFrame(Container cont, String text, boolean ce, boolean ccs) {
        return waitJInternalFrame(cont, text, ce, ccs, 0);
    }

    @Override
    public void setOutput(TestOut out) {
        output = out;
        super.setOutput(output.createErrorOutput());
    }

    @Override
    public TestOut getOutput() {
        return output;
    }

    @Override
    public void setTimeouts(Timeouts times) {
        timeouts = times;
        super.setTimeouts(timeouts);
    }

    @Override
    public Timeouts getTimeouts() {
        return timeouts;
    }

    /**
     * Iconifies frame. Note: frame should not be iconified and should be
     * iconifiable.
     *
     * @throws WrongInternalFrameStateException
     *
     */
    public void iconify() {
        output.printLine("Iconify JInternalFrame\n    : " + toStringSource());
        output.printGolden("Iconify JInternalFrame \"" + getTitle() + "\"");
        checkIconified(false);
        makeComponentVisible();
        fDriver.iconify(this);
        if (getVerification()) {
            waitIcon(true);
        }
    }

    /**
     * Deiconifies frame. Note: frame should be iconified.
     *
     * @throws WrongInternalFrameStateException
     *
     */
    public void deiconify() {
        output.printLine("Deiconify JInternalFrame\n    : " + toStringSource());
        output.printGolden("Deiconify JInternalFrame \"" + getTitle() + "\"");
        checkIconified(true);
        fDriver.deiconify(this);
        if (getVerification()) {
            waitIcon(false);
        }
    }

    /**
     * Maximizes frame. Note: frame should not be iconified.
     *
     * @throws WrongInternalFrameStateException
     */
    public void maximize() {
        output.printLine("Maximize JInternalFrame\n    : " + toStringSource());
        output.printGolden("Maximize JInternalFrame \"" + getTitle() + "\"");
        checkIconified(false);
        makeComponentVisible();
        fDriver.maximize(this);
        if (getVerification()) {
            waitMaximum(true);
        }
    }

    /**
     * Demaximizes frame. Note: frame should not be iconified.
     *
     * @throws WrongInternalFrameStateException
     */
    public void demaximize() {
        output.printLine("Demaximize JInternalFrame\n    : " + toStringSource());
        output.printGolden("Demaximize JInternalFrame \"" + getTitle() + "\"");
        checkIconified(false);
        makeComponentVisible();
        fDriver.demaximize(this);
        if (getVerification()) {
            waitMaximum(false);
        }
    }

    /**
     * Moves frame to new location. Note: frame should not be iconified.
     *
     * @param x X coordinate of a new frame location.
     * @param y Y coordinate of a new frame location.
     * @throws WrongInternalFrameStateException
     */
    public void move(int x, int y) {
        checkIconified(false);
        output.printLine("Move JInternalFrame to ("
                + Integer.toString(x) + ","
                + Integer.toString(y) + ")"
                + " position\n    : " + toStringSource());
        output.printGolden("Move " + getTitle()
                + " JInternalFrame to ("
                + Integer.toString(x) + ","
                + Integer.toString(y) + ")"
                + " position");
        checkIconified(false);
        wDriver.move(this, x, y);
        if (getVerification()) {
            waitComponentLocation(new Point(x, y));
        }
    }

    /**
     * Resizes frame. Note: frame should not be iconified.
     *
     * @param width New frame width.
     * @param height New frame height.
     * @throws WrongInternalFrameStateException
     */
    public void resize(int width, int height) {
        output.printLine("Resize JInternalFrame to ("
                + Integer.toString(width) + ","
                + Integer.toString(height) + ")"
                + " size\n    : " + toStringSource());
        output.printGolden("Resize " + getTitle()
                + " JInternalFrame to ("
                + Integer.toString(width) + ","
                + Integer.toString(height) + ")"
                + " size");
        checkIconified(false);
        wDriver.resize(this, width, height);
        if (getVerification()) {
            waitComponentSize(new Dimension(width, height));
        }
    }

    /**
     * Activates frame. Note: frame should not be iconified.
     *
     * @throws WrongInternalFrameStateException
     */
    public void activate() {
        checkIconified(false);
        wDriver.activate(this);
        if (getVerification()) {
            waitActivate(true);
        }
    }

    /**
     * Closes the frame.
     */
    public void close() {
        checkIconified(false);
        wDriver.requestClose(this);
        if (getVerification()) {
            waitClosed();
        }
    }

    /**
     * Scrolls to internal frame's rectangle.
     *
     * @param x Horizontal rectangle coordinate
     * @param y Vertical rectangle coordinate
     * @param width rectangle width
     * @param height rectangle height
     *
     */
    public void scrollToRectangle(int x, int y, int width, int height) {
        output.printTrace("Scroll desktop pane to make \"" + getTitle() + "\" internal frame visible");
        output.printGolden("Scroll desktop pane to make \"" + getTitle() + "\" internal frame visible");
        makeComponentVisible();
        //try to find JScrollPane under.
        JScrollPane scroll;
        if (isIcon()) {
            scroll
                    = (JScrollPane) iconOperator.getContainer(new JScrollPaneOperator.JScrollPaneFinder(ComponentSearcher.
                            getTrueChooser("JScrollPane")));
        } else {
            scroll
                    = (JScrollPane) getContainer(new JScrollPaneOperator.JScrollPaneFinder(ComponentSearcher.
                            getTrueChooser("JScrollPane")));
        }
        if (scroll == null) {
            return;
        }
        JScrollPaneOperator scroller = new JScrollPaneOperator(scroll);
        scroller.copyEnvironment(this);
        scroller.setVisualizer(new EmptyVisualizer());
        scroller.scrollToComponentRectangle(isIcon() ? iconOperator.getSource() : getSource(),
                x, y, width, height);
    }

    /**
     * Scrolls to internal frame's rectangle.
     *
     * @param rect a rectangle to scroll to.
     */
    public void scrollToRectangle(Rectangle rect) {
        scrollToRectangle(rect.x, rect.y, rect.width, rect.height);
    }

    /**
     * Scrolls to internal frame.
     *
     */
    public void scrollToFrame() {
        if (isIcon()) {
            scrollToRectangle(0, 0, iconOperator.getWidth(), iconOperator.getHeight());
        } else {
            scrollToRectangle(0, 0, getWidth(), getHeight());
        }
    }

    /**
     * Waits for a minimize button inside the title pane.
     *
     * @return a button operator
     */
    public JButtonOperator getMinimizeButton() {
        initOperators();
        return minOper;
    }

    /**
     * Waits for a maximize button inside the title pane.
     *
     * @return a button operator
     */
    public JButtonOperator getMaximizeButton() {
        initOperators();
        return maxOper;
    }

    /**
     * Waits for a close button inside the title pane.
     *
     * @return a button operator
     */
    public JButtonOperator getCloseButton() {
        initOperators();
        return closeOper;
    }

    public JButtonOperator getPopupButton() {
        initOperators();
        return popupButtonOper;
    }

    /**
     * Waits for the title pane.
     *
     * @return a button operator
     */
    public ContainerOperator<?> getTitleOperator() {
        initOperators();
        return titleOperator;
    }

    /**
     * Creates an operator for an desktop icon.
     *
     * @return an icon operator.
     */
    public JDesktopIconOperator getIconOperator() {
        if(Platform.isOSX()) {
            initIconOperator();
        } else {
            initOperators();
        }
        return iconOperator;
    }

    /**
     * Waits for the frame to be iconified or deiconified.
     *
     * @param isIconified whether the frame needs to be iconified or deiconified.
     */
    public void waitIcon(final boolean isIconified) {
        waitStateOnQueue(new ComponentChooser() {
            @Override
            public boolean checkComponent(Component comp) {
                return isIcon() == isIconified;
            }

            @Override
            public String getDescription() {
                return "Internal Frame is " + (isIconified ? "iconified" : "deiconified");
            }

            @Override
            public String toString() {
                return "JInternalFrameOperator.waitIcon.ComponentChooser{description = " + getDescription() + '}';
            }
        });
    }

    /**
     * Waits for the frame to be activated or deactivated.
     *
     * @param isActivate whether the frame needs to be activated or deactivated.
     */
    public void waitActivate(final boolean isActivate) {
        waitStateOnQueue(new ComponentChooser() {
            @Override
            public boolean checkComponent(Component comp) {
                return isSelected() == isActivate;
            }

            @Override
            public String getDescription() {
                return "Internal Frame is " + (isActivate ? "activated" : "deactivated");
            }

            @Override
            public String toString() {
                return "JInternalFrameOperator.waitActivate.ComponentChooser{description = " + getDescription() + '}';
            }
        });
    }

    /**
     * Waits for the frame to be closed.
     */
    public void waitClosed() {
        waitStateOnQueue(new ComponentChooser() {
            @Override
            public boolean checkComponent(Component comp) {
                return isClosed();
            }

            @Override
            public String getDescription() {
                return "Internal Frame is closed";
            }

            @Override
            public String toString() {
                return "JInternalFrameOperator.waitClosed.ComponentChooser{description = " + getDescription() + '}';
            }
        });
    }

    /**
     * Waits for the frame to be maximized or demaximized.
     *
     * @param isMaximum whether the frame needs to be maximized or demaximized.
     */
    public void waitMaximum(final boolean isMaximum) {
        waitStateOnQueue(new ComponentChooser() {
            @Override
            public boolean checkComponent(Component comp) {
                return isMaximum() == isMaximum;
            }

            @Override
            public String getDescription() {
                return "Internal Frame is " + (isMaximum ? "maximizied" : "demaximizied");
            }

            @Override
            public String toString() {
                return "JInternalFrameOperator.waitMaximum.ComponentChooser{description = " + getDescription() + '}';
            }
        });
    }

    /**
     * Returns information about component.
     */
    @Override
    public Hashtable<String, Object> getDump() {
        Hashtable<String, Object> result = super.getDump();
        result.put(TITLE_DPROP, ((JInternalFrame) getSource()).getTitle());
        String state = STATE_NORMAL_DPROP_VALUE;
        if (((JInternalFrame) getSource()).isClosed()) {
            state = STATE_CLOSED_DPROP_VALUE;
        } else if (((JInternalFrame) getSource()).isIcon()) {
            state = STATE_ICONIFIED_DPROP_VALUE;
        } else if (((JInternalFrame) getSource()).isMaximum()) {
            state = STATE_MAXIMAZED_DPROP_VALUE;
        }
        result.put(STATE_DPROP, state);
        result.put(IS_RESIZABLE_DPROP, ((JInternalFrame) getSource()).isResizable() ? "true" : "false");
        result.put(IS_SELECTED_DPROP, ((JInternalFrame) getSource()).isSelected() ? "true" : "false");
        return result;
    }

    ////////////////////////////////////////////////////////
    //Mapping                                             //
    /**
     * Maps
     * {@code JInternalFrame.addInternalFrameListener(InternalFrameListener)}
     * through queue
     */
    public void addInternalFrameListener(final InternalFrameListener internalFrameListener) {
        runMapping(new MapVoidAction("addInternalFrameListener") {
            @Override
            public void map() {
                ((JInternalFrame) getSource()).addInternalFrameListener(internalFrameListener);
            }
        });
    }

    /**
     * Maps {@code JInternalFrame.dispose()} through queue
     */
    public void dispose() {
        runMapping(new MapVoidAction("dispose") {
            @Override
            public void map() {
                ((JInternalFrame) getSource()).dispose();
            }
        });
    }

    /**
     * Maps {@code JInternalFrame.getContentPane()} through queue
     */
    public Container getContentPane() {
        return (runMapping(new MapAction<Container>("getContentPane") {
            @Override
            public Container map() {
                return ((JInternalFrame) getSource()).getContentPane();
            }
        }));
    }

    /**
     * Maps {@code JInternalFrame.getDefaultCloseOperation()} through queue
     */
    public int getDefaultCloseOperation() {
        return (runMapping(new MapIntegerAction("getDefaultCloseOperation") {
            @Override
            public int map() {
                return ((JInternalFrame) getSource()).getDefaultCloseOperation();
            }
        }));
    }

    /**
     * Maps {@code JInternalFrame.getDesktopIcon()} through queue
     */
    public JDesktopIcon getDesktopIcon() {
        return (runMapping(new MapAction<JDesktopIcon>("getDesktopIcon") {
            @Override
            public JDesktopIcon map() {
                return ((JInternalFrame) getSource()).getDesktopIcon();
            }
        }));
    }

    /**
     * Maps {@code JInternalFrame.getDesktopPane()} through queue
     */
    public JDesktopPane getDesktopPane() {
        return (runMapping(new MapAction<JDesktopPane>("getDesktopPane") {
            @Override
            public JDesktopPane map() {
                return ((JInternalFrame) getSource()).getDesktopPane();
            }
        }));
    }

    /**
     * Maps {@code JInternalFrame.getFrameIcon()} through queue
     */
    public Icon getFrameIcon() {
        return (runMapping(new MapAction<Icon>("getFrameIcon") {
            @Override
            public Icon map() {
                return ((JInternalFrame) getSource()).getFrameIcon();
            }
        }));
    }

    /**
     * Maps {@code JInternalFrame.getGlassPane()} through queue
     */
    public Component getGlassPane() {
        return (runMapping(new MapAction<Component>("getGlassPane") {
            @Override
            public Component map() {
                return ((JInternalFrame) getSource()).getGlassPane();
            }
        }));
    }

    /**
     * Maps {@code JInternalFrame.getJMenuBar()} through queue
     */
    public JMenuBar getJMenuBar() {
        return (runMapping(new MapAction<JMenuBar>("getJMenuBar") {
            @Override
            public JMenuBar map() {
                return ((JInternalFrame) getSource()).getJMenuBar();
            }
        }));
    }

    /**
     * Maps {@code JInternalFrame.getLayer()} through queue
     */
    public int getLayer() {
        return (runMapping(new MapIntegerAction("getLayer") {
            @Override
            public int map() {
                return ((JInternalFrame) getSource()).getLayer();
            }
        }));
    }

    /**
     * Maps {@code JInternalFrame.getLayeredPane()} through queue
     */
    public JLayeredPane getLayeredPane() {
        return (runMapping(new MapAction<JLayeredPane>("getLayeredPane") {
            @Override
            public JLayeredPane map() {
                return ((JInternalFrame) getSource()).getLayeredPane();
            }
        }));
    }

    /**
     * Maps {@code JInternalFrame.getTitle()} through queue
     */
    public String getTitle() {
        return (runMapping(new MapAction<String>("getTitle") {
            @Override
            public String map() {
                return ((JInternalFrame) getSource()).getTitle();
            }
        }));
    }

    /**
     * Maps {@code JInternalFrame.getUI()} through queue
     */
    public InternalFrameUI getUI() {
        return (runMapping(new MapAction<InternalFrameUI>("getUI") {
            @Override
            public InternalFrameUI map() {
                return ((JInternalFrame) getSource()).getUI();
            }
        }));
    }

    /**
     * Maps {@code JInternalFrame.getWarningString()} through queue
     */
    public String getWarningString() {
        return (runMapping(new MapAction<String>("getWarningString") {
            @Override
            public String map() {
                return ((JInternalFrame) getSource()).getWarningString();
            }
        }));
    }

    /**
     * Maps {@code JInternalFrame.isClosable()} through queue
     */
    public boolean isClosable() {
        return (runMapping(new MapBooleanAction("isClosable") {
            @Override
            public boolean map() {
                return ((JInternalFrame) getSource()).isClosable();
            }
        }));
    }

    /**
     * Maps {@code JInternalFrame.isClosed()} through queue
     */
    public boolean isClosed() {
        return (runMapping(new MapBooleanAction("isClosed") {
            @Override
            public boolean map() {
                return ((JInternalFrame) getSource()).isClosed();
            }
        }));
    }

    /**
     * Maps {@code JInternalFrame.isIcon()} through queue
     */
    public boolean isIcon() {
        return (runMapping(new MapBooleanAction("isIcon") {
            @Override
            public boolean map() {
                return ((JInternalFrame) getSource()).isIcon();
            }
        }));
    }

    /**
     * Maps {@code JInternalFrame.isIconifiable()} through queue
     */
    public boolean isIconifiable() {
        return (runMapping(new MapBooleanAction("isIconifiable") {
            @Override
            public boolean map() {
                return ((JInternalFrame) getSource()).isIconifiable();
            }
        }));
    }

    /**
     * Maps {@code JInternalFrame.isMaximizable()} through queue
     */
    public boolean isMaximizable() {
        return (runMapping(new MapBooleanAction("isMaximizable") {
            @Override
            public boolean map() {
                return ((JInternalFrame) getSource()).isMaximizable();
            }
        }));
    }

    /**
     * Maps {@code JInternalFrame.isMaximum()} through queue
     */
    public boolean isMaximum() {
        return (runMapping(new MapBooleanAction("isMaximum") {
            @Override
            public boolean map() {
                return ((JInternalFrame) getSource()).isMaximum();
            }
        }));
    }

    /**
     * Maps {@code JInternalFrame.isResizable()} through queue
     */
    public boolean isResizable() {
        return (runMapping(new MapBooleanAction("isResizable") {
            @Override
            public boolean map() {
                return ((JInternalFrame) getSource()).isResizable();
            }
        }));
    }

    /**
     * Maps {@code JInternalFrame.isSelected()} through queue
     */
    public boolean isSelected() {
        return (runMapping(new MapBooleanAction("isSelected") {
            @Override
            public boolean map() {
                return ((JInternalFrame) getSource()).isSelected();
            }
        }));
    }

    /**
     * Maps {@code JInternalFrame.moveToBack()} through queue
     */
    public void moveToBack() {
        runMapping(new MapVoidAction("moveToBack") {
            @Override
            public void map() {
                ((JInternalFrame) getSource()).moveToBack();
            }
        });
    }

    /**
     * Maps {@code JInternalFrame.moveToFront()} through queue
     */
    public void moveToFront() {
        runMapping(new MapVoidAction("moveToFront") {
            @Override
            public void map() {
                ((JInternalFrame) getSource()).moveToFront();
            }
        });
    }

    /**
     * Maps {@code JInternalFrame.pack()} through queue
     */
    public void pack() {
        runMapping(new MapVoidAction("pack") {
            @Override
            public void map() {
                ((JInternalFrame) getSource()).pack();
            }
        });
    }

    /**
     * Maps
     * {@code JInternalFrame.removeInternalFrameListener(InternalFrameListener)}
     * through queue
     */
    public void removeInternalFrameListener(final InternalFrameListener internalFrameListener) {
        runMapping(new MapVoidAction("removeInternalFrameListener") {
            @Override
            public void map() {
                ((JInternalFrame) getSource()).removeInternalFrameListener(internalFrameListener);
            }
        });
    }

    /**
     * Maps {@code JInternalFrame.setClosable(boolean)} through queue
     */
    public void setClosable(final boolean b) {
        runMapping(new MapVoidAction("setClosable") {
            @Override
            public void map() {
                ((JInternalFrame) getSource()).setClosable(b);
            }
        });
    }

    /**
     * Maps {@code JInternalFrame.setClosed(boolean)} through queue
     */
    public void setClosed(final boolean b) {
        runMapping(new MapVoidAction("setClosed") {
            @Override
            public void map() throws PropertyVetoException {
                ((JInternalFrame) getSource()).setClosed(b);
            }
        });
    }

    /**
     * Maps {@code JInternalFrame.setContentPane(Container)} through queue
     */
    public void setContentPane(final Container container) {
        runMapping(new MapVoidAction("setContentPane") {
            @Override
            public void map() {
                ((JInternalFrame) getSource()).setContentPane(container);
            }
        });
    }

    /**
     * Maps {@code JInternalFrame.setDefaultCloseOperation(int)} through queue
     */
    public void setDefaultCloseOperation(final int i) {
        runMapping(new MapVoidAction("setDefaultCloseOperation") {
            @Override
            public void map() {
                ((JInternalFrame) getSource()).setDefaultCloseOperation(i);
            }
        });
    }

    /**
     * Maps {@code JInternalFrame.setDesktopIcon(JDesktopIcon)} through queue
     */
    public void setDesktopIcon(final JDesktopIcon jDesktopIcon) {
        runMapping(new MapVoidAction("setDesktopIcon") {
            @Override
            public void map() {
                ((JInternalFrame) getSource()).setDesktopIcon(jDesktopIcon);
            }
        });
    }

    /**
     * Maps {@code JInternalFrame.setFrameIcon(Icon)} through queue
     */
    public void setFrameIcon(final Icon icon) {
        runMapping(new MapVoidAction("setFrameIcon") {
            @Override
            public void map() {
                ((JInternalFrame) getSource()).setFrameIcon(icon);
            }
        });
    }

    /**
     * Maps {@code JInternalFrame.setGlassPane(Component)} through queue
     */
    public void setGlassPane(final Component component) {
        runMapping(new MapVoidAction("setGlassPane") {
            @Override
            public void map() {
                ((JInternalFrame) getSource()).setGlassPane(component);
            }
        });
    }

    /**
     * Maps {@code JInternalFrame.setIcon(boolean)} through queue
     */
    public void setIcon(final boolean b) {
        runMapping(new MapVoidAction("setIcon") {
            @Override
            public void map() throws PropertyVetoException {
                ((JInternalFrame) getSource()).setIcon(b);
            }
        });
    }

    /**
     * Maps {@code JInternalFrame.setIconifiable(boolean)} through queue
     */
    public void setIconifiable(final boolean b) {
        runMapping(new MapVoidAction("setIconifiable") {
            @Override
            public void map() {
                ((JInternalFrame) getSource()).setIconifiable(b);
            }
        });
    }

    /**
     * Maps {@code JInternalFrame.setJMenuBar(JMenuBar)} through queue
     */
    public void setJMenuBar(final JMenuBar jMenuBar) {
        runMapping(new MapVoidAction("setJMenuBar") {
            @Override
            public void map() {
                ((JInternalFrame) getSource()).setJMenuBar(jMenuBar);
            }
        });
    }

    /**
     * Maps {@code JInternalFrame.setLayer(Integer)} through queue
     */
    public void setLayer(final Integer integer) {
        runMapping(new MapVoidAction("setLayer") {
            @Override
            public void map() {
                ((JInternalFrame) getSource()).setLayer(integer);
            }
        });
    }

    /**
     * Maps {@code JInternalFrame.setLayeredPane(JLayeredPane)} through queue
     */
    public void setLayeredPane(final JLayeredPane jLayeredPane) {
        runMapping(new MapVoidAction("setLayeredPane") {
            @Override
            public void map() {
                ((JInternalFrame) getSource()).setLayeredPane(jLayeredPane);
            }
        });
    }

    /**
     * Maps {@code JInternalFrame.setMaximizable(boolean)} through queue
     */
    public void setMaximizable(final boolean b) {
        runMapping(new MapVoidAction("setMaximizable") {
            @Override
            public void map() {
                ((JInternalFrame) getSource()).setMaximizable(b);
            }
        });
    }

    /**
     * Maps {@code JInternalFrame.setMaximum(boolean)} through queue
     */
    public void setMaximum(final boolean b) {
        runMapping(new MapVoidAction("setMaximum") {
            @Override
            public void map() throws PropertyVetoException {
                ((JInternalFrame) getSource()).setMaximum(b);
            }
        });
    }

    /**
     * Maps {@code JInternalFrame.setResizable(boolean)} through queue
     */
    public void setResizable(final boolean b) {
        runMapping(new MapVoidAction("setResizable") {
            @Override
            public void map() {
                ((JInternalFrame) getSource()).setResizable(b);
            }
        });
    }

    /**
     * Maps {@code JInternalFrame.setSelected(boolean)} through queue
     */
    public void setSelected(final boolean b) {
        runMapping(new MapVoidAction("setSelected") {
            @Override
            public void map() throws PropertyVetoException {
                ((JInternalFrame) getSource()).setSelected(b);
            }
        });
    }

    /**
     * Maps {@code JInternalFrame.setTitle(String)} through queue
     */
    public void setTitle(final String string) {
        runMapping(new MapVoidAction("setTitle") {
            @Override
            public void map() {
                ((JInternalFrame) getSource()).setTitle(string);
            }
        });
    }

    /**
     * Maps {@code JInternalFrame.setUI(InternalFrameUI)} through queue
     */
    public void setUI(final InternalFrameUI internalFrameUI) {
        runMapping(new MapVoidAction("setUI") {
            @Override
            public void map() {
                ((JInternalFrame) getSource()).setUI(internalFrameUI);
            }
        });
    }

    /**
     * Maps {@code JInternalFrame.toBack()} through queue
     */
    public void toBack() {
        runMapping(new MapVoidAction("toBack") {
            @Override
            public void map() {
                ((JInternalFrame) getSource()).toBack();
            }
        });
    }

    /**
     * Maps {@code JInternalFrame.toFront()} through queue
     */
    public void toFront() {
        runMapping(new MapVoidAction("toFront") {
            @Override
            public void map() {
                ((JInternalFrame) getSource()).toFront();
            }
        });
    }

    //End of mapping                                      //
    ////////////////////////////////////////////////////////
    /**
     * Uses InternalframeDriver to get a title pane.
     *
     * @return a title pane.
     */
    protected Container findTitlePane() {
        return (Container) iDriver.getTitlePane(this);
    }

    /**
     * Initialize icon operator
     */
    protected void initIconOperator() {
        iconOperator = new JDesktopIconOperator(((JInternalFrame) getSource()).getDesktopIcon());
        iconOperator.copyEnvironment(this);
    }

    /**
     * Initiaites suboperators.
     */
    protected void initOperators() {
        initIconOperator();
        if(Platform.isOSX()) {
            throw new UnsupportedOperationException(
                    "Jemmy doesn't support getting or initializing title"
                    + " related operators on Mac OSx");
        } else {
            Container titlePane = findTitlePane();
            if (!isIcon() && titlePane != null) {
                if (titleOperator == null) {
                    titleOperator = new ContainerOperator<>(titlePane);
                    if (getContainer(new ComponentChooser() {
                        @Override
                        public boolean checkComponent(Component comp) {
                            return comp instanceof JDesktopPane;
                        }

                    @Override
                    public String getDescription() {
                        return "Desktop pane";
                    }

                    @Override
                    public String toString() {
                        return "JInternalFrameOperator.initOperators.ComponentChooser{description = " + getDescription() + '}';
                    }
                }) != null) {
                    if(LookAndFeel.isMotif()) {
                        popupButtonOper = new JButtonOperator(titleOperator, 0);
                    } else {
                        minOper = new JButtonOperator(titleOperator,
                            new JComponentByTipFinder(MINIMIZE_BUTTON_TOOLTIP));
                        if (((JInternalFrame) getSource()).isMaximizable()) {
                            maxOper = new JButtonOperator(titleOperator,
                                new JComponentByTipFinder(MAXIMIZE_BUTTON_TOOLTIP));
                        } else {
                            maxOper = null;
                        }
                    }
                } else {
                    minOper = null;
                    maxOper = null;
                }
                if (isClosable()) {
                    if(!LookAndFeel.isMotif()) {
                        closeOper = new JButtonOperator(titleOperator,
                            new JComponentByTipFinder(CLOSE_BUTTON_TOOLTIP));
                    }
                } else {
                    closeOper = null;
                }
            }
        } else {
            titleOperator = null;
            minOper = null;
            maxOper = null;
            closeOper = null;
            }
        }
    }

    //throw exception if state is wrong
    private void checkIconified(boolean shouldBeIconified) {
        if (shouldBeIconified && !isIcon()
                || !shouldBeIconified && isIcon()) {
            throw (new WrongInternalFrameStateException("JInternal frame should "
                    + (shouldBeIconified ? "" : "not")
                    + " be iconified to produce this operation",
                    getSource()));
        }
    }

    private static JInternalFrame findOne(ContainerOperator<?> cont, String text, int index) {
        Component source = waitComponent(cont,
                new JInternalFrameByTitleFinder(text,
                        cont.getComparator()),
                index);
        if (source instanceof JInternalFrame) {
            return (JInternalFrame) source;
        } else if (source instanceof JInternalFrame.JDesktopIcon) {
            return ((JInternalFrame.JDesktopIcon) source).getInternalFrame();
        } else {
            throw (new TimeoutExpiredException("No internal frame was found"));
        }
    }

    /**
     * Exception can be throwht if as a result of an attempt to produce
     * operation for the frame in incorrect state. Like activate iconified
     * frame, for example.
     */
    public static class WrongInternalFrameStateException extends JemmyInputException {

        private static final long serialVersionUID = 42L;

        /**
         * Constructs a JInternalFrameOperator$WrongInternalFrameStateException
         * object.
         *
         * @param message an exception message.
         * @param comp an internal frame.
         */
        public WrongInternalFrameStateException(String message, Component comp) {
            super(message, comp);
        }
    }

    /**
     * Allows to find component by title.
     */
    public static class JInternalFrameByTitleFinder implements ComponentChooser {

        String label;
        StringComparator comparator;

        /**
         * Constructs JInternalFrameByTitleFinder.
         *
         * @param lb a text pattern
         * @param comparator specifies string comparision algorithm.
         */
        public JInternalFrameByTitleFinder(String lb, StringComparator comparator) {
            label = lb;
            this.comparator = comparator;
        }

        /**
         * Constructs JInternalFrameByTitleFinder.
         *
         * @param lb a text pattern
         */
        public JInternalFrameByTitleFinder(String lb) {
            this(lb, Operator.getDefaultStringComparator());
        }

        @Override
        public boolean checkComponent(Component comp) {
            if (comp instanceof JInternalFrame || comp instanceof JInternalFrame.JDesktopIcon) {
                JInternalFrame frame = null;
                if (comp instanceof JInternalFrame) {
                    frame = (JInternalFrame) comp;
                } else {
                    JDesktopIconOperator io = new JDesktopIconOperator((JInternalFrame.JDesktopIcon) comp);
                    frame = io.getInternalFrame();
                }
                if (frame.getTitle() != null) {
                    return (comparator.equals(frame.getTitle(),
                            label));
                }
            }
            return false;
        }

        @Override
        public String getDescription() {
            return "JInternalFrame with title \"" + label + "\"";
        }

        @Override
        public String toString() {
            return "JInternalFrameByTitleFinder{" + "label=" + label + ", comparator=" + comparator + '}';
        }
    }

    /**
     * Class to operate with javax.swing.JInternalFrame.JDesktopIconOperator
     * component.
     */
    public static class JDesktopIconOperator extends JComponentOperator
            implements Outputable, Timeoutable {

        private TestOut output;
        private Timeouts timeouts;

        /**
         * Constructs JDesktopIconOperator.
         *
         * @param b a component
         */
        public JDesktopIconOperator(JInternalFrame.JDesktopIcon b) {
            super(b);
            setOutput(JemmyProperties.getCurrentOutput());
            setTimeouts(JemmyProperties.getCurrentTimeouts());
        }

        @Override
        public void setOutput(TestOut out) {
            output = out;
            super.setOutput(output.createErrorOutput());
        }

        @Override
        public TestOut getOutput() {
            return output;
        }

        @Override
        public void setTimeouts(Timeouts times) {
            timeouts = times;
            super.setTimeouts(timeouts);
        }

        @Override
        public Timeouts getTimeouts() {
            return timeouts;
        }

        /**
         * Creates an operator for the correspondent internal frame.
         *
         * @return an operator.
         */
        public JInternalFrame getInternalFrame() {
            return (runMapping(new MapAction<JInternalFrame>("getInternalFrame") {
                @Override
                public JInternalFrame map() {
                    return ((JInternalFrame.JDesktopIcon) getSource()).getInternalFrame();
                }
            }));
        }

        /**
         * Pushs the deiconifying button.
         */
        public void pushButton() {
            this.clickMouse(2);
        }
    }

    /**
     * Checks component type.
     */
    public static class JInternalFrameFinder implements ComponentChooser {

        ComponentChooser sf = null;

        /**
         * Constructs JInternalFrameFinder.
         *
         * @param sf other searching criteria.
         */
        public JInternalFrameFinder(ComponentChooser sf) {
            this.sf = sf;
        }

        /**
         * Constructs JInternalFrameFinder.
         */
        public JInternalFrameFinder() {
            this(ComponentSearcher.getTrueChooser("JInternalFrame or JInternalFrame.JDesktopIcon"));
        }

        @Override
        public boolean checkComponent(Component comp) {
            return ((comp instanceof JInternalFrame || comp instanceof JInternalFrame.JDesktopIcon)
                    && sf.checkComponent(comp));
        }

        @Override
        public String getDescription() {
            return sf.getDescription();
        }

        @Override
        public String toString() {
            return "JInternalFrameFinder{" + "sf=" + sf + '}';
        }
    }

}
