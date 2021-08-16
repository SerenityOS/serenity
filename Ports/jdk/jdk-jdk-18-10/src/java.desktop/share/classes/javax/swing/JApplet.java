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

import java.applet.Applet;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.HeadlessException;
import java.awt.LayoutManager;
import java.beans.BeanProperty;
import java.beans.JavaBean;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;

/**
 * An extended version of <code>java.applet.Applet</code> that adds support for
 * the JFC/Swing component architecture.
 * You can find task-oriented documentation about using <code>JApplet</code>
 * in <em>The Java Tutorial</em>,
 * in the section
 * <a
 href="https://docs.oracle.com/javase/tutorial/uiswing/components/applet.html">How to Make Applets</a>.
 * <p>
 * The <code>JApplet</code> class is slightly incompatible with
 * <code>java.applet.Applet</code>.  <code>JApplet</code> contains a
 * <code>JRootPane</code> as its only child.  The <code>contentPane</code>
 * should be the parent of any children of the <code>JApplet</code>.
 * As a convenience, the {@code add}, {@code remove}, and {@code setLayout}
 * methods of this class are overridden, so that they delegate calls
 * to the corresponding methods of the {@code ContentPane}.
 * For example, you can add a child component to an applet as follows:
 * <pre>
 *       applet.add(child);
 * </pre>
 *
 * And the child will be added to the <code>contentPane</code>.
 * The <code>contentPane</code> will always be non-<code>null</code>.
 * Attempting to set it to <code>null</code> will cause the
 * <code>JApplet</code> to throw an exception. The default
 * <code>contentPane</code> will have a <code>BorderLayout</code>
 * manager set on it.
 * Refer to {@link javax.swing.RootPaneContainer}
 * for details on adding, removing and setting the <code>LayoutManager</code>
 * of a <code>JApplet</code>.
 * <p>
 * Please see the <code>JRootPane</code> documentation for a
 * complete description of the <code>contentPane</code>, <code>glassPane</code>,
 * and <code>layeredPane</code> properties.
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
 * @see javax.swing.RootPaneContainer
 *
 * @author Arnaud Weber
 * @since 1.2
 *
 * @deprecated The Applet API is deprecated, no replacement.
 */
@Deprecated(since = "9", forRemoval = true)
@JavaBean(defaultProperty = "JMenuBar", description = "Swing's Applet subclass.")
@SwingContainer(delegate = "getContentPane")
@SuppressWarnings({"serial", "removal"}) // Same-version serialization only
public class JApplet extends Applet implements Accessible,
                                               RootPaneContainer,
                               TransferHandler.HasGetTransferHandler
{
    /**
     * @see #getRootPane
     * @see #setRootPane
     */
    protected JRootPane rootPane;

    /**
     * If true then calls to <code>add</code> and <code>setLayout</code>
     * will be forwarded to the <code>contentPane</code>. This is initially
     * false, but is set to true when the <code>JApplet</code> is constructed.
     *
     * @see #isRootPaneCheckingEnabled
     * @see #setRootPaneCheckingEnabled
     * @see javax.swing.RootPaneContainer
     */
    protected boolean rootPaneCheckingEnabled = false;

    /**
     * The <code>TransferHandler</code> for this applet.
     */
    private TransferHandler transferHandler;

    /**
     * Creates a swing applet instance.
     * <p>
     * This constructor sets the component's locale property to the value
     * returned by <code>JComponent.getDefaultLocale</code>.
     *
     * @exception HeadlessException if GraphicsEnvironment.isHeadless()
     * returns true.
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see JComponent#getDefaultLocale
     */
    public JApplet() throws HeadlessException {
        super();
        // Check the timerQ and restart if necessary.
        TimerQueue q = TimerQueue.sharedInstance();
        if(q != null) {
            q.startIfNeeded();
        }

        /* Workaround for bug 4155072.  The shared double buffer image
         * may hang on to a reference to this applet; unfortunately
         * Image.getGraphics() will continue to call JApplet.getForeground()
         * and getBackground() even after this applet has been destroyed.
         * So we ensure that these properties are non-null here.
         */
        setForeground(Color.black);
        setBackground(Color.white);

        setLocale( JComponent.getDefaultLocale() );
        setLayout(new BorderLayout());
        setRootPane(createRootPane());
        setRootPaneCheckingEnabled(true);

        setFocusTraversalPolicyProvider(true);
        sun.awt.SunToolkit.checkAndSetPolicy(this);

        enableEvents(AWTEvent.KEY_EVENT_MASK);
    }

    /**
     * Called by the constructor methods to create the default rootPane.
     *
     * @return  a new {@code JRootPane}
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
     * Note: When used with {@code JApplet}, {@code TransferHandler} only
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
     */
    public void update(Graphics g) {
        paint(g);
    }

   /**
    * Sets the menubar for this applet.
    * @param menuBar the menubar being placed in the applet
    *
    * @see #getJMenuBar
    */
    @BeanProperty(bound = false, hidden = true, description
            = "The menubar for accessing pulldown menus from this applet.")
    public void setJMenuBar(final JMenuBar menuBar) {
        getRootPane().setJMenuBar(menuBar);
    }

   /**
    * Returns the menubar set on this applet.
    *
    * @return the menubar set on this applet
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
     *        <code>JApplet</code>.
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
     * Returns the rootPane object for this applet.
     *
     * @see #setRootPane
     * @see RootPaneContainer#getRootPane
     */
    @BeanProperty(bound = false, hidden = true, description
            = "the RootPane object for this applet.")
    public JRootPane getRootPane() {
        return rootPane;
    }


    /**
     * Sets the rootPane property.  This method is called by the constructor.
     * @param root the rootPane object for this applet
     *
     * @see #getRootPane
     */
    protected void setRootPane(JRootPane root) {
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
     * Returns the contentPane object for this applet.
     *
     * @see #setContentPane
     * @see RootPaneContainer#getContentPane
     */
    public Container getContentPane() {
        return getRootPane().getContentPane();
    }

   /**
     * Sets the contentPane property.  This method is called by the constructor.
     * @param contentPane the contentPane object for this applet
     *
     * @exception java.awt.IllegalComponentStateException (a runtime
     *            exception) if the content pane parameter is null
     * @see #getContentPane
     * @see RootPaneContainer#setContentPane
     */
   @BeanProperty(bound = false, hidden = true, description
           = "The client area of the applet where child components are normally inserted.")
    public void setContentPane(Container contentPane) {
        getRootPane().setContentPane(contentPane);
    }

    /**
     * Returns the layeredPane object for this applet.
     *
     * @exception java.awt.IllegalComponentStateException (a runtime
     *            exception) if the layered pane parameter is null
     * @see #setLayeredPane
     * @see RootPaneContainer#getLayeredPane
     */
    public JLayeredPane getLayeredPane() {
        return getRootPane().getLayeredPane();
    }

    /**
     * Sets the layeredPane property.  This method is called by the constructor.
     * @param layeredPane the layeredPane object for this applet
     *
     * @see #getLayeredPane
     * @see RootPaneContainer#setLayeredPane
     */
    @BeanProperty(bound = false, hidden = true, description
            = "The pane which holds the various applet layers.")
    public void setLayeredPane(JLayeredPane layeredPane) {
        getRootPane().setLayeredPane(layeredPane);
    }

    /**
     * Returns the glassPane object for this applet.
     *
     * @see #setGlassPane
     * @see RootPaneContainer#getGlassPane
     */
    public Component getGlassPane() {
        return getRootPane().getGlassPane();
    }

    /**
     * Sets the glassPane property.
     * This method is called by the constructor.
     * @param glassPane the glassPane object for this applet
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
     * Returns a string representation of this JApplet. This method
     * is intended to be used only for debugging purposes, and the
     * content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not
     * be <code>null</code>.
     *
     * @return  a string representation of this JApplet.
     */
    protected String paramString() {
        String rootPaneString = (rootPane != null ?
                                 rootPane.toString() : "");
        String rootPaneCheckingEnabledString = (rootPaneCheckingEnabled ?
                                                "true" : "false");

        return super.paramString() +
        ",rootPane=" + rootPaneString +
        ",rootPaneCheckingEnabled=" + rootPaneCheckingEnabledString;
    }



/////////////////
// Accessibility support
////////////////

    /**
     * {@code AccessibleContext} associated with this {@code JApplet}
     */
    protected AccessibleContext accessibleContext = null;

    /**
     * Gets the AccessibleContext associated with this JApplet.
     * For JApplets, the AccessibleContext takes the form of an
     * AccessibleJApplet.
     * A new AccessibleJApplet instance is created if necessary.
     *
     * @return an AccessibleJApplet that serves as the
     *         AccessibleContext of this JApplet
     */
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJApplet();
        }
        return accessibleContext;
    }

    /**
     * This class implements accessibility support for the
     * <code>JApplet</code> class.
     */
    protected class AccessibleJApplet extends AccessibleApplet {

        /**
         * Constructs an {@code AccessibleJApplet}.
         */
        protected AccessibleJApplet() {}

        // everything moved to new parent, AccessibleApplet
    }
}
