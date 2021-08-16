/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.LayoutManager;
import java.awt.event.WindowEvent;

import java.beans.JavaBean;
import java.beans.BeanProperty;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;

/**
 * An extended version of <code>java.awt.Frame</code> that adds support for
 * the JFC/Swing component architecture.
 * You can find task-oriented documentation about using <code>JFrame</code>
 * in <em>The Java Tutorial</em>, in the section
 * <a
 href="https://docs.oracle.com/javase/tutorial/uiswing/components/frame.html">How to Make Frames</a>.
 *
 * <p>
 * The <code>JFrame</code> class is slightly incompatible with <code>Frame</code>.
 * Like all other JFC/Swing top-level containers,
 * a <code>JFrame</code> contains a <code>JRootPane</code> as its only child.
 * The <b>content pane</b> provided by the root pane should,
 * as a rule, contain
 * all the non-menu components displayed by the <code>JFrame</code>.
 * This is different from the AWT <code>Frame</code> case.
 * As a convenience, the {@code add}, {@code remove}, and {@code setLayout}
 * methods of this class are overridden, so that they delegate calls
 * to the corresponding methods of the {@code ContentPane}.
 * For example, you can add a child component to a frame as follows:
 * <pre>
 *       frame.add(child);
 * </pre>
 * And the child will be added to the contentPane.
 * The content pane will
 * always be non-null. Attempting to set it to null will cause the JFrame
 * to throw an exception. The default content pane will have a BorderLayout
 * manager set on it.
 * Refer to {@link javax.swing.RootPaneContainer}
 * for details on adding, removing and setting the <code>LayoutManager</code>
 * of a <code>JFrame</code>.
 * <p>
 * Unlike a <code>Frame</code>, a <code>JFrame</code> has some notion of how to
 * respond when the user attempts to close the window. The default behavior
 * is to simply hide the JFrame when the user closes the window. To change the
 * default behavior, you invoke the method
 * {@link #setDefaultCloseOperation}.
 * To make the <code>JFrame</code> behave the same as a <code>Frame</code>
 * instance, use
 * <code>setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE)</code>.
 * <p>
 * For more information on content panes
 * and other features that root panes provide,
 * see <a
 href="https://docs.oracle.com/javase/tutorial/uiswing/components/toplevel.html">Using Top-Level Containers</a> in <em>The Java Tutorial</em>.
 * <p>
 * In a multi-screen environment, you can create a <code>JFrame</code>
 * on a different screen device.  See {@link java.awt.Frame} for more
 * information.
 * <p>
 * <strong>Warning:</strong> Swing is not thread safe. For more
 * information see <a
 * href="package-summary.html#threading">Swing's Threading
 * Policy</a>.
 * <p>
 * <strong>Warning:</strong>
 * Serialized objects of this class will not be compatible with
 * future Swing releases. The current serialization support is
 * appropriate for short term storage or RMI between applications running
 * the same version of Swing.  As of 1.4, support for long term storage
 * of all JavaBeans
 * has been added to the <code>java.beans</code> package.
 * Please see {@link java.beans.XMLEncoder}.
 *
 * @see JRootPane
 * @see #setDefaultCloseOperation
 * @see java.awt.event.WindowListener#windowClosing
 * @see javax.swing.RootPaneContainer
 *
 * @author Jeff Dinkins
 * @author Georges Saab
 * @author David Kloba
 * @since 1.2
 */
@JavaBean(defaultProperty = "JMenuBar", description = "A toplevel window which can be minimized to an icon.")
@SwingContainer(delegate = "getContentPane")
@SuppressWarnings("serial") // Same-version serialization only
public class JFrame  extends Frame implements WindowConstants,
                                              Accessible,
                                              RootPaneContainer,
                              TransferHandler.HasGetTransferHandler
{
    /**
     * Key into the AppContext, used to check if should provide decorations
     * by default.
     */
    private static final Object defaultLookAndFeelDecoratedKey =
            new StringBuffer("JFrame.defaultLookAndFeelDecorated");

    private int defaultCloseOperation = HIDE_ON_CLOSE;

    /**
     * The <code>TransferHandler</code> for this frame.
     */
    private TransferHandler transferHandler;

    /**
     * The <code>JRootPane</code> instance that manages the
     * <code>contentPane</code>
     * and optional <code>menuBar</code> for this frame, as well as the
     * <code>glassPane</code>.
     *
     * @see JRootPane
     * @see RootPaneContainer
     */
    protected JRootPane rootPane;

    /**
     * If true then calls to <code>add</code> and <code>setLayout</code>
     * will be forwarded to the <code>contentPane</code>. This is initially
     * false, but is set to true when the <code>JFrame</code> is constructed.
     *
     * @see #isRootPaneCheckingEnabled
     * @see #setRootPaneCheckingEnabled
     * @see javax.swing.RootPaneContainer
     */
    protected boolean rootPaneCheckingEnabled = false;


    /**
     * Constructs a new frame that is initially invisible.
     * <p>
     * This constructor sets the component's locale property to the value
     * returned by <code>JComponent.getDefaultLocale</code>.
     *
     * @exception HeadlessException if GraphicsEnvironment.isHeadless()
     * returns true.
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see Component#setSize
     * @see Component#setVisible
     * @see JComponent#getDefaultLocale
     */
    public JFrame() throws HeadlessException {
        super();
        frameInit();
    }

    /**
     * Creates a <code>Frame</code> in the specified
     * <code>GraphicsConfiguration</code> of
     * a screen device and a blank title.
     * <p>
     * This constructor sets the component's locale property to the value
     * returned by <code>JComponent.getDefaultLocale</code>.
     *
     * @param gc the <code>GraphicsConfiguration</code> that is used
     *          to construct the new <code>Frame</code>;
     *          if <code>gc</code> is <code>null</code>, the system
     *          default <code>GraphicsConfiguration</code> is assumed
     * @exception IllegalArgumentException if <code>gc</code> is not from
     *          a screen device.  This exception is always thrown when
     *      GraphicsEnvironment.isHeadless() returns true.
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see JComponent#getDefaultLocale
     * @since     1.3
     */
    public JFrame(GraphicsConfiguration gc) {
        super(gc);
        frameInit();
    }

    /**
     * Creates a new, initially invisible <code>Frame</code> with the
     * specified title.
     * <p>
     * This constructor sets the component's locale property to the value
     * returned by <code>JComponent.getDefaultLocale</code>.
     *
     * @param title the title for the frame
     * @exception HeadlessException if GraphicsEnvironment.isHeadless()
     * returns true.
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see Component#setSize
     * @see Component#setVisible
     * @see JComponent#getDefaultLocale
     */
    public JFrame(String title) throws HeadlessException {
        super(title);
        frameInit();
    }

    /**
     * Creates a <code>JFrame</code> with the specified title and the
     * specified <code>GraphicsConfiguration</code> of a screen device.
     * <p>
     * This constructor sets the component's locale property to the value
     * returned by <code>JComponent.getDefaultLocale</code>.
     *
     * @param title the title to be displayed in the
     *          frame's border. A <code>null</code> value is treated as
     *          an empty string, "".
     * @param gc the <code>GraphicsConfiguration</code> that is used
     *          to construct the new <code>JFrame</code> with;
     *          if <code>gc</code> is <code>null</code>, the system
     *          default <code>GraphicsConfiguration</code> is assumed
     * @exception IllegalArgumentException if <code>gc</code> is not from
     *          a screen device.  This exception is always thrown when
     *      GraphicsEnvironment.isHeadless() returns true.
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see JComponent#getDefaultLocale
     * @since     1.3
     */
    public JFrame(String title, GraphicsConfiguration gc) {
        super(title, gc);
        frameInit();
    }

    /** Called by the constructors to init the <code>JFrame</code> properly. */
    protected void frameInit() {
        enableEvents(AWTEvent.KEY_EVENT_MASK | AWTEvent.WINDOW_EVENT_MASK);
        setLocale( JComponent.getDefaultLocale() );
        setRootPane(createRootPane());
        setBackground(UIManager.getColor("control"));
        setRootPaneCheckingEnabled(true);
        if (JFrame.isDefaultLookAndFeelDecorated()) {
            boolean supportsWindowDecorations =
            UIManager.getLookAndFeel().getSupportsWindowDecorations();
            if (supportsWindowDecorations) {
                setUndecorated(true);
                getRootPane().setWindowDecorationStyle(JRootPane.FRAME);
            }
        }
        sun.awt.SunToolkit.checkAndSetPolicy(this);
    }

    /**
     * Called by the constructor methods to create the default
     * <code>rootPane</code>.
     *
     * @return a new {@code JRootPane}
     */
    protected JRootPane createRootPane() {
        JRootPane rp = new JRootPane();
        // NOTE: this uses setOpaque vs LookAndFeel.installProperty as there
        // is NO reason for the RootPane not to be opaque. For painting to
        // work the contentPane must be opaque, therefor the RootPane can
        // also be opaque.
        rp.setOpaque(true);
        return rp;
    }

    /**
     * Processes window events occurring on this component.
     * Hides the window or disposes of it, as specified by the setting
     * of the <code>defaultCloseOperation</code> property.
     *
     * @param  e  the window event
     * @see    #setDefaultCloseOperation
     * @see    java.awt.Window#processWindowEvent
     */
    protected void processWindowEvent(final WindowEvent e) {
        super.processWindowEvent(e);

        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            switch (defaultCloseOperation) {
                case HIDE_ON_CLOSE:
                    setVisible(false);
                    break;
                case DISPOSE_ON_CLOSE:
                    dispose();
                    break;
                case EXIT_ON_CLOSE:
                    // This needs to match the checkExit call in
                    // setDefaultCloseOperation
                    System.exit(0);
                    break;
                case DO_NOTHING_ON_CLOSE:
                default:
            }
        }
    }

    /**
     * Sets the operation that will happen by default when
     * the user initiates a "close" on this frame.
     * You must specify one of the following choices:
     * <br><br>
     * <ul>
     * <li><code>DO_NOTHING_ON_CLOSE</code>
     * (defined in <code>WindowConstants</code>):
     * Don't do anything; require the
     * program to handle the operation in the <code>windowClosing</code>
     * method of a registered <code>WindowListener</code> object.
     *
     * <li><code>HIDE_ON_CLOSE</code>
     * (defined in <code>WindowConstants</code>):
     * Automatically hide the frame after
     * invoking any registered <code>WindowListener</code>
     * objects.
     *
     * <li><code>DISPOSE_ON_CLOSE</code>
     * (defined in <code>WindowConstants</code>):
     * Automatically hide and dispose the
     * frame after invoking any registered <code>WindowListener</code>
     * objects.
     *
     * <li><code>EXIT_ON_CLOSE</code>
     * (defined in <code>WindowConstants</code>):
     * Exit the application using the <code>System</code>
     * <code>exit</code> method.  Use this only in applications.
     * </ul>
     * <p>
     * The value is set to <code>HIDE_ON_CLOSE</code> by default. Changes
     * to the value of this property cause the firing of a property
     * change event, with property name "defaultCloseOperation".
     * <p>
     * <b>Note</b>: When the last displayable window within the
     * Java virtual machine (VM) is disposed of, the VM may
     * terminate.  See <a href="../../java/awt/doc-files/AWTThreadIssues.html">
     * AWT Threading Issues</a> for more information.
     *
     * @param operation the operation which should be performed when the
     *        user closes the frame
     * @exception IllegalArgumentException if defaultCloseOperation value
     *             isn't one of the above valid values
     * @see #addWindowListener
     * @see #getDefaultCloseOperation
     * @see WindowConstants
     * @throws  SecurityException
     *        if <code>EXIT_ON_CLOSE</code> has been specified and the
     *        <code>SecurityManager</code> will
     *        not allow the caller to invoke <code>System.exit</code>
     * @see        java.lang.Runtime#exit(int)
     */
    @BeanProperty(preferred = true, enumerationValues = {
            "WindowConstants.DO_NOTHING_ON_CLOSE",
            "WindowConstants.HIDE_ON_CLOSE",
            "WindowConstants.DISPOSE_ON_CLOSE",
            "WindowConstants.EXIT_ON_CLOSE"}, description
            = "The frame's default close operation.")
    public void setDefaultCloseOperation(int operation) {
        if (operation != DO_NOTHING_ON_CLOSE &&
            operation != HIDE_ON_CLOSE &&
            operation != DISPOSE_ON_CLOSE &&
            operation != EXIT_ON_CLOSE) {
            throw new IllegalArgumentException("defaultCloseOperation must be"
                    + " one of: DO_NOTHING_ON_CLOSE, HIDE_ON_CLOSE,"
                    + " DISPOSE_ON_CLOSE, or EXIT_ON_CLOSE");
        }

        if (operation == EXIT_ON_CLOSE) {
            @SuppressWarnings("removal")
            SecurityManager security = System.getSecurityManager();
            if (security != null) {
                security.checkExit(0);
            }
        }
        if (this.defaultCloseOperation != operation) {
            int oldValue = this.defaultCloseOperation;
            this.defaultCloseOperation = operation;
            firePropertyChange("defaultCloseOperation", oldValue, operation);
        }
    }


   /**
    * Returns the operation that occurs when the user
    * initiates a "close" on this frame.
    *
    * @return an integer indicating the window-close operation
    * @see #setDefaultCloseOperation
    */
    public int getDefaultCloseOperation() {
        return defaultCloseOperation;
    }

    /**
     * Sets the {@code transferHandler} property, which is a mechanism to
     * support transfer of data into this component. Use {@code null}
     * if the component does not support data transfer operations.
     * <p>
     * If the system property {@code suppressSwingDropSupport} is {@code false}
     * (the default) and the current drop target on this component is either
     * {@code null} or not a user-set drop target, this method will change the
     * drop target as follows: If {@code newHandler} is {@code null} it will
     * clear the drop target. If not {@code null} it will install a new
     * {@code DropTarget}.
     * <p>
     * Note: When used with {@code JFrame}, {@code TransferHandler} only
     * provides data import capability, as the data export related methods
     * are currently typed to {@code JComponent}.
     * <p>
     * Please see
     * <a href="https://docs.oracle.com/javase/tutorial/uiswing/dnd/index.html">
     * How to Use Drag and Drop and Data Transfer</a>, a section in
     * <em>The Java Tutorial</em>, for more information.
     *
     * @param newHandler the new {@code TransferHandler}
     *
     * @see TransferHandler
     * @see #getTransferHandler
     * @see java.awt.Component#setDropTarget
     * @since 1.6
     */
    @BeanProperty(hidden = true, description
            = "Mechanism for transfer of data into the component")
    public void setTransferHandler(TransferHandler newHandler) {
        TransferHandler oldHandler = transferHandler;
        transferHandler = newHandler;
        SwingUtilities.installSwingDropTargetAsNecessary(this, transferHandler);
        firePropertyChange("transferHandler", oldHandler, newHandler);
    }

    /**
     * Gets the <code>transferHandler</code> property.
     *
     * @return the value of the <code>transferHandler</code> property
     *
     * @see TransferHandler
     * @see #setTransferHandler
     * @since 1.6
     */
    public TransferHandler getTransferHandler() {
        return transferHandler;
    }

    /**
     * Just calls <code>paint(g)</code>.  This method was overridden to
     * prevent an unnecessary call to clear the background.
     *
     * @param g the Graphics context in which to paint
     */
    public void update(Graphics g) {
        paint(g);
    }

   /**
    * Sets the menubar for this frame.
    * @param menubar the menubar being placed in the frame
    *
    * @see #getJMenuBar
    */
    @BeanProperty(bound = false, hidden = true, description
            = "The menubar for accessing pulldown menus from this frame.")
    public void setJMenuBar(final JMenuBar menubar) {
        getRootPane().setJMenuBar(menubar);
    }

   /**
    * Returns the menubar set on this frame.
    * @return the menubar for this frame
    *
    * @see #setJMenuBar
    */
    public JMenuBar getJMenuBar() {
        return getRootPane().getJMenuBar();
    }

    /**
     * Returns whether calls to <code>add</code> and
     * <code>setLayout</code> are forwarded to the <code>contentPane</code>.
     *
     * @return true if <code>add</code> and <code>setLayout</code>
     *         are forwarded; false otherwise
     *
     * @see #addImpl
     * @see #setLayout
     * @see #setRootPaneCheckingEnabled
     * @see javax.swing.RootPaneContainer
     */
    protected boolean isRootPaneCheckingEnabled() {
        return rootPaneCheckingEnabled;
    }


    /**
     * Sets whether calls to <code>add</code> and
     * <code>setLayout</code> are forwarded to the <code>contentPane</code>.
     *
     * @param enabled  true if <code>add</code> and <code>setLayout</code>
     *        are forwarded, false if they should operate directly on the
     *        <code>JFrame</code>.
     *
     * @see #addImpl
     * @see #setLayout
     * @see #isRootPaneCheckingEnabled
     * @see javax.swing.RootPaneContainer
     */
    @BeanProperty(hidden = true, description
            = "Whether the add and setLayout methods are forwarded")
    protected void setRootPaneCheckingEnabled(boolean enabled) {
        rootPaneCheckingEnabled = enabled;
    }


    /**
     * Adds the specified child <code>Component</code>.
     * This method is overridden to conditionally forward calls to the
     * <code>contentPane</code>.
     * By default, children are added to the <code>contentPane</code> instead
     * of the frame, refer to {@link javax.swing.RootPaneContainer} for
     * details.
     *
     * @param comp the component to be enhanced
     * @param constraints the constraints to be respected
     * @param index the index
     * @exception IllegalArgumentException if <code>index</code> is invalid
     * @exception IllegalArgumentException if adding the container's parent
     *                  to itself
     * @exception IllegalArgumentException if adding a window to a container
     *
     * @see #setRootPaneCheckingEnabled
     * @see javax.swing.RootPaneContainer
     */
    protected void addImpl(Component comp, Object constraints, int index)
    {
        if(isRootPaneCheckingEnabled()) {
            getContentPane().add(comp, constraints, index);
        }
        else {
            super.addImpl(comp, constraints, index);
        }
    }

    /**
     * Removes the specified component from the container. If
     * <code>comp</code> is not the <code>rootPane</code>, this will forward
     * the call to the <code>contentPane</code>. This will do nothing if
     * <code>comp</code> is not a child of the <code>JFrame</code> or
     * <code>contentPane</code>.
     *
     * @param comp the component to be removed
     * @throws NullPointerException if <code>comp</code> is null
     * @see #add
     * @see javax.swing.RootPaneContainer
     */
    public void remove(Component comp) {
        if (comp == rootPane) {
            super.remove(comp);
        } else {
            getContentPane().remove(comp);
        }
    }


    /**
     * Sets the <code>LayoutManager</code>.
     * Overridden to conditionally forward the call to the
     * <code>contentPane</code>.
     * Refer to {@link javax.swing.RootPaneContainer} for
     * more information.
     *
     * @param manager the <code>LayoutManager</code>
     * @see #setRootPaneCheckingEnabled
     * @see javax.swing.RootPaneContainer
     */
    public void setLayout(LayoutManager manager) {
        if(isRootPaneCheckingEnabled()) {
            getContentPane().setLayout(manager);
        }
        else {
            super.setLayout(manager);
        }
    }


    /**
     * Returns the <code>rootPane</code> object for this frame.
     * @return the <code>rootPane</code> property
     *
     * @see #setRootPane
     * @see RootPaneContainer#getRootPane
     */
    @BeanProperty(bound = false, hidden = true, description
            = "the RootPane object for this frame.")
    public JRootPane getRootPane() {
        return rootPane;
    }


    /**
     * Sets the <code>rootPane</code> property.
     * This method is called by the constructor.
     * @param root the <code>rootPane</code> object for this frame
     *
     * @see #getRootPane
     */
    protected void setRootPane(JRootPane root)
    {
        if(rootPane != null) {
            remove(rootPane);
        }
        rootPane = root;
        if(rootPane != null) {
            boolean checkingEnabled = isRootPaneCheckingEnabled();
            try {
                setRootPaneCheckingEnabled(false);
                add(rootPane, BorderLayout.CENTER);
            }
            finally {
                setRootPaneCheckingEnabled(checkingEnabled);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setIconImage(Image image) {
        super.setIconImage(image);
    }

    /**
     * Returns the <code>contentPane</code> object for this frame.
     * @return the <code>contentPane</code> property
     *
     * @see #setContentPane
     * @see RootPaneContainer#getContentPane
     */
    public Container getContentPane() {
        return getRootPane().getContentPane();
    }

    /**
     * Sets the <code>contentPane</code> property.
     * This method is called by the constructor.
     * <p>
     * Swing's painting architecture requires an opaque <code>JComponent</code>
     * in the containment hierarchy. This is typically provided by the
     * content pane. If you replace the content pane it is recommended you
     * replace it with an opaque <code>JComponent</code>.
     *
     * @param contentPane the <code>contentPane</code> object for this frame
     *
     * @exception java.awt.IllegalComponentStateException (a runtime
     *            exception) if the content pane parameter is <code>null</code>
     * @see #getContentPane
     * @see RootPaneContainer#setContentPane
     * @see JRootPane
     */
    @BeanProperty(bound = false, hidden = true, description
            = "The client area of the frame where child components are normally inserted.")
    public void setContentPane(Container contentPane) {
        getRootPane().setContentPane(contentPane);
    }

    /**
     * Returns the <code>layeredPane</code> object for this frame.
     * @return the <code>layeredPane</code> property
     *
     * @see #setLayeredPane
     * @see RootPaneContainer#getLayeredPane
     */
    public JLayeredPane getLayeredPane() {
        return getRootPane().getLayeredPane();
    }

    /**
     * Sets the <code>layeredPane</code> property.
     * This method is called by the constructor.
     * @param layeredPane the <code>layeredPane</code> object for this frame
     *
     * @exception java.awt.IllegalComponentStateException (a runtime
     *            exception) if the layered pane parameter is <code>null</code>
     * @see #getLayeredPane
     * @see RootPaneContainer#setLayeredPane
     */
    @BeanProperty(bound = false, hidden = true, description
            = "The pane that holds the various frame layers.")
    public void setLayeredPane(JLayeredPane layeredPane) {
        getRootPane().setLayeredPane(layeredPane);
    }

    /**
     * Returns the <code>glassPane</code> object for this frame.
     * @return the <code>glassPane</code> property
     *
     * @see #setGlassPane
     * @see RootPaneContainer#getGlassPane
     */
    public Component getGlassPane() {
        return getRootPane().getGlassPane();
    }

    /**
     * Sets the <code>glassPane</code> property.
     * This method is called by the constructor.
     * @param glassPane the <code>glassPane</code> object for this frame
     *
     * @see #getGlassPane
     * @see RootPaneContainer#setGlassPane
     */
    @BeanProperty(bound = false, hidden = true, description
            = "A transparent pane used for menu rendering.")
    public void setGlassPane(Component glassPane) {
        getRootPane().setGlassPane(glassPane);
    }

    /**
     * {@inheritDoc}
     *
     * @since 1.6
     */
    @BeanProperty(bound = false)
    public Graphics getGraphics() {
        JComponent.getGraphicsInvoked(this);
        return super.getGraphics();
    }

    /**
     * Repaints the specified rectangle of this component within
     * <code>time</code> milliseconds.  Refer to <code>RepaintManager</code>
     * for details on how the repaint is handled.
     *
     * @param     time   maximum time in milliseconds before update
     * @param     x    the <i>x</i> coordinate
     * @param     y    the <i>y</i> coordinate
     * @param     width    the width
     * @param     height   the height
     * @see       RepaintManager
     * @since     1.6
     */
    public void repaint(long time, int x, int y, int width, int height) {
        if (RepaintManager.HANDLE_TOP_LEVEL_PAINT) {
            RepaintManager.currentManager(this).addDirtyRegion(
                              this, x, y, width, height);
        }
        else {
            super.repaint(time, x, y, width, height);
        }
    }

    /**
     * Provides a hint as to whether or not newly created <code>JFrame</code>s
     * should have their Window decorations (such as borders, widgets to
     * close the window, title...) provided by the current look
     * and feel. If <code>defaultLookAndFeelDecorated</code> is true,
     * the current <code>LookAndFeel</code> supports providing window
     * decorations, and the current window manager supports undecorated
     * windows, then newly created <code>JFrame</code>s will have their
     * Window decorations provided by the current <code>LookAndFeel</code>.
     * Otherwise, newly created <code>JFrame</code>s will have their
     * Window decorations provided by the current window manager.
     * <p>
     * You can get the same effect on a single JFrame by doing the following:
     * <pre>
     *    JFrame frame = new JFrame();
     *    frame.setUndecorated(true);
     *    frame.getRootPane().setWindowDecorationStyle(JRootPane.FRAME);
     * </pre>
     *
     * @param defaultLookAndFeelDecorated A hint as to whether or not current
     *        look and feel should provide window decorations
     * @see javax.swing.LookAndFeel#getSupportsWindowDecorations
     * @since 1.4
     */
    public static void setDefaultLookAndFeelDecorated(boolean defaultLookAndFeelDecorated) {
        if (defaultLookAndFeelDecorated) {
            SwingUtilities.appContextPut(defaultLookAndFeelDecoratedKey, Boolean.TRUE);
        } else {
            SwingUtilities.appContextPut(defaultLookAndFeelDecoratedKey, Boolean.FALSE);
        }
    }


    /**
     * Returns true if newly created <code>JFrame</code>s should have their
     * Window decorations provided by the current look and feel. This is only
     * a hint, as certain look and feels may not support this feature.
     *
     * @return true if look and feel should provide Window decorations.
     * @since 1.4
     */
    public static boolean isDefaultLookAndFeelDecorated() {
        Boolean defaultLookAndFeelDecorated =
            (Boolean) SwingUtilities.appContextGet(defaultLookAndFeelDecoratedKey);
        if (defaultLookAndFeelDecorated == null) {
            defaultLookAndFeelDecorated = Boolean.FALSE;
        }
        return defaultLookAndFeelDecorated.booleanValue();
    }

    /**
     * Returns a string representation of this <code>JFrame</code>.
     * This method
     * is intended to be used only for debugging purposes, and the
     * content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not
     * be <code>null</code>.
     *
     * @return  a string representation of this <code>JFrame</code>
     */
    protected String paramString() {
        String defaultCloseOperationString;
        if (defaultCloseOperation == HIDE_ON_CLOSE) {
            defaultCloseOperationString = "HIDE_ON_CLOSE";
        } else if (defaultCloseOperation == DISPOSE_ON_CLOSE) {
            defaultCloseOperationString = "DISPOSE_ON_CLOSE";
        } else if (defaultCloseOperation == DO_NOTHING_ON_CLOSE) {
            defaultCloseOperationString = "DO_NOTHING_ON_CLOSE";
        } else if (defaultCloseOperation == EXIT_ON_CLOSE) {
            defaultCloseOperationString = "EXIT_ON_CLOSE";
        } else defaultCloseOperationString = "";
        String rootPaneString = (rootPane != null ?
                                 rootPane.toString() : "");
        String rootPaneCheckingEnabledString = (rootPaneCheckingEnabled ?
                                                "true" : "false");

        return super.paramString() +
        ",defaultCloseOperation=" + defaultCloseOperationString +
        ",rootPane=" + rootPaneString +
        ",rootPaneCheckingEnabled=" + rootPaneCheckingEnabledString;
    }



/////////////////
// Accessibility support
////////////////

    /**
     * The accessible context property.
     */
    protected AccessibleContext accessibleContext = null;

    /**
     * Gets the AccessibleContext associated with this JFrame.
     * For JFrames, the AccessibleContext takes the form of an
     * AccessibleJFrame.
     * A new AccessibleJFrame instance is created if necessary.
     *
     * @return an AccessibleJFrame that serves as the
     *         AccessibleContext of this JFrame
     */
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJFrame();
        }
        return accessibleContext;
    }

    /**
     * This class implements accessibility support for the
     * <code>JFrame</code> class.  It provides an implementation of the
     * Java Accessibility API appropriate to frame user-interface
     * elements.
     */
    protected class AccessibleJFrame extends AccessibleAWTFrame {

        /**
         * Constructs an {@code AccessibleJFrame}.
         */
        protected AccessibleJFrame() {}

        // AccessibleContext methods
        /**
         * Get the accessible name of this object.
         *
         * @return the localized name of the object -- can be null if this
         * object does not have a name
         */
        public String getAccessibleName() {
            if (accessibleName != null) {
                return accessibleName;
            } else {
                if (getTitle() == null) {
                    return super.getAccessibleName();
                } else {
                    return getTitle();
                }
            }
        }

        /**
         * Get the state of this object.
         *
         * @return an instance of AccessibleStateSet containing the current
         * state set of the object
         * @see AccessibleState
         */
        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet states = super.getAccessibleStateSet();

            if (isResizable()) {
                states.add(AccessibleState.RESIZABLE);
            }
            if (getFocusOwner() != null) {
                states.add(AccessibleState.ACTIVE);
            }
            // FIXME:  [[[WDW - should also return ICONIFIED and ICONIFIABLE
            // if we can ever figure these out]]]
            return states;
        }
    } // inner class AccessibleJFrame
}
