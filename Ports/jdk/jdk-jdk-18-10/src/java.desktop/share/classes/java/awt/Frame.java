/*
 * Copyright (c) 1995, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.awt;

import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.peer.FramePeer;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Vector;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;

import sun.awt.AWTAccessor;
import sun.awt.SunToolkit;

/**
 * A {@code Frame} is a top-level window with a title and a border.
 * <p>
 * The size of the frame includes any area designated for the
 * border.  The dimensions of the border area may be obtained
 * using the {@code getInsets} method, however, since
 * these dimensions are platform-dependent, a valid insets
 * value cannot be obtained until the frame is made displayable
 * by either calling {@code pack} or {@code show}.
 * Since the border area is included in the overall size of the
 * frame, the border effectively obscures a portion of the frame,
 * constraining the area available for rendering and/or displaying
 * subcomponents to the rectangle which has an upper-left corner
 * location of {@code (insets.left, insets.top)}, and has a size of
 * {@code width - (insets.left + insets.right)} by
 * {@code height - (insets.top + insets.bottom)}.
 * <p>
 * The default layout for a frame is {@code BorderLayout}.
 * <p>
 * A frame may have its native decorations (i.e. {@code Frame}
 * and {@code Titlebar}) turned off
 * with {@code setUndecorated}. This can only be done while the frame
 * is not {@link Component#isDisplayable() displayable}.
 * <p>
 * In a multi-screen environment, you can create a {@code Frame}
 * on a different screen device by constructing the {@code Frame}
 * with {@link #Frame(GraphicsConfiguration)} or
 * {@link #Frame(String title, GraphicsConfiguration)}.  The
 * {@code GraphicsConfiguration} object is one of the
 * {@code GraphicsConfiguration} objects of the target screen
 * device.
 * <p>
 * In a virtual device multi-screen environment in which the desktop
 * area could span multiple physical screen devices, the bounds of all
 * configurations are relative to the virtual-coordinate system.  The
 * origin of the virtual-coordinate system is at the upper left-hand
 * corner of the primary physical screen.  Depending on the location
 * of the primary screen in the virtual device, negative coordinates
 * are possible, as shown in the following figure.
 * <p>
 * <img src="doc-files/MultiScreen.gif" alt="Diagram of virtual device
 * encompassing three physical screens and one primary physical screen. The
 * primary physical screen shows (0,0) coords while a different physical screen
 * shows (-80,-100) coords." style="margin: 7px 10px;">
 * <p>
 * In such an environment, when calling {@code setLocation},
 * you must pass a virtual coordinate to this method.  Similarly,
 * calling {@code getLocationOnScreen} on a {@code Frame}
 * returns virtual device coordinates.  Call the {@code getBounds}
 * method of a {@code GraphicsConfiguration} to find its origin in
 * the virtual coordinate system.
 * <p>
 * The following code sets the
 * location of the {@code Frame} at (10, 10) relative
 * to the origin of the physical screen of the corresponding
 * {@code GraphicsConfiguration}.  If the bounds of the
 * {@code GraphicsConfiguration} is not taken into account, the
 * {@code Frame} location would be set at (10, 10) relative to the
 * virtual-coordinate system and would appear on the primary physical
 * screen, which might be different from the physical screen of the
 * specified {@code GraphicsConfiguration}.
 *
 * <pre>
 *      Frame f = new Frame(GraphicsConfiguration gc);
 *      Rectangle bounds = gc.getBounds();
 *      f.setLocation(10 + bounds.x, 10 + bounds.y);
 * </pre>
 *
 * <p>
 * Frames are capable of generating the following types of
 * {@code WindowEvent}s:
 * <ul>
 * <li>{@code WINDOW_OPENED}
 * <li>{@code WINDOW_CLOSING}:
 *     <br>If the program doesn't
 *     explicitly hide or dispose the window while processing
 *     this event, the window close operation is canceled.
 * <li>{@code WINDOW_CLOSED}
 * <li>{@code WINDOW_ICONIFIED}
 * <li>{@code WINDOW_DEICONIFIED}
 * <li>{@code WINDOW_ACTIVATED}
 * <li>{@code WINDOW_DEACTIVATED}
 * <li>{@code WINDOW_GAINED_FOCUS}
 * <li>{@code WINDOW_LOST_FOCUS}
 * <li>{@code WINDOW_STATE_CHANGED}
 * </ul>
 *
 * @author      Sami Shaio
 * @see WindowEvent
 * @see Window#addWindowListener
 * @since       1.0
 */
public class Frame extends Window implements MenuContainer {

    /* Note: These are being obsoleted;  programs should use the Cursor class
     * variables going forward. See Cursor and Component.setCursor.
     */

   /**
    * @deprecated   replaced by {@code Cursor.DEFAULT_CURSOR}.
    */
    @Deprecated
    public static final int     DEFAULT_CURSOR                  = Cursor.DEFAULT_CURSOR;


   /**
    * @deprecated   replaced by {@code Cursor.CROSSHAIR_CURSOR}.
    */
    @Deprecated
    public static final int     CROSSHAIR_CURSOR                = Cursor.CROSSHAIR_CURSOR;

   /**
    * @deprecated   replaced by {@code Cursor.TEXT_CURSOR}.
    */
    @Deprecated
    public static final int     TEXT_CURSOR                     = Cursor.TEXT_CURSOR;

   /**
    * @deprecated   replaced by {@code Cursor.WAIT_CURSOR}.
    */
    @Deprecated
    public static final int     WAIT_CURSOR                     = Cursor.WAIT_CURSOR;

   /**
    * @deprecated   replaced by {@code Cursor.SW_RESIZE_CURSOR}.
    */
    @Deprecated
    public static final int     SW_RESIZE_CURSOR                = Cursor.SW_RESIZE_CURSOR;

   /**
    * @deprecated   replaced by {@code Cursor.SE_RESIZE_CURSOR}.
    */
    @Deprecated
    public static final int     SE_RESIZE_CURSOR                = Cursor.SE_RESIZE_CURSOR;

   /**
    * @deprecated   replaced by {@code Cursor.NW_RESIZE_CURSOR}.
    */
    @Deprecated
    public static final int     NW_RESIZE_CURSOR                = Cursor.NW_RESIZE_CURSOR;

   /**
    * @deprecated   replaced by {@code Cursor.NE_RESIZE_CURSOR}.
    */
    @Deprecated
    public static final int     NE_RESIZE_CURSOR                = Cursor.NE_RESIZE_CURSOR;

   /**
    * @deprecated   replaced by {@code Cursor.N_RESIZE_CURSOR}.
    */
    @Deprecated
    public static final int     N_RESIZE_CURSOR                 = Cursor.N_RESIZE_CURSOR;

   /**
    * @deprecated   replaced by {@code Cursor.S_RESIZE_CURSOR}.
    */
    @Deprecated
    public static final int     S_RESIZE_CURSOR                 = Cursor.S_RESIZE_CURSOR;

   /**
    * @deprecated   replaced by {@code Cursor.W_RESIZE_CURSOR}.
    */
    @Deprecated
    public static final int     W_RESIZE_CURSOR                 = Cursor.W_RESIZE_CURSOR;

   /**
    * @deprecated   replaced by {@code Cursor.E_RESIZE_CURSOR}.
    */
    @Deprecated
    public static final int     E_RESIZE_CURSOR                 = Cursor.E_RESIZE_CURSOR;

   /**
    * @deprecated   replaced by {@code Cursor.HAND_CURSOR}.
    */
    @Deprecated
    public static final int     HAND_CURSOR                     = Cursor.HAND_CURSOR;

   /**
    * @deprecated   replaced by {@code Cursor.MOVE_CURSOR}.
    */
    @Deprecated
    public static final int     MOVE_CURSOR                     = Cursor.MOVE_CURSOR;


    /**
     * Frame is in the "normal" state.  This symbolic constant names a
     * frame state with all state bits cleared.
     * @see #setExtendedState(int)
     * @see #getExtendedState
     */
    public static final int NORMAL = 0;

    /**
     * This state bit indicates that frame is iconified.
     * @see #setExtendedState(int)
     * @see #getExtendedState
     */
    public static final int ICONIFIED = 1;

    /**
     * This state bit indicates that frame is maximized in the
     * horizontal direction.
     * @see #setExtendedState(int)
     * @see #getExtendedState
     * @since 1.4
     */
    public static final int MAXIMIZED_HORIZ = 2;

    /**
     * This state bit indicates that frame is maximized in the
     * vertical direction.
     * @see #setExtendedState(int)
     * @see #getExtendedState
     * @since 1.4
     */
    public static final int MAXIMIZED_VERT = 4;

    /**
     * This state bit mask indicates that frame is fully maximized
     * (that is both horizontally and vertically).  It is just a
     * convenience alias for
     * <code>MAXIMIZED_VERT&nbsp;|&nbsp;MAXIMIZED_HORIZ</code>.
     *
     * <p>Note that the correct test for frame being fully maximized is
     * <pre>
     *     (state &amp; Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH
     * </pre>
     *
     * <p>To test is frame is maximized in <em>some</em> direction use
     * <pre>
     *     (state &amp; Frame.MAXIMIZED_BOTH) != 0
     * </pre>
     *
     * @see #setExtendedState(int)
     * @see #getExtendedState
     * @since 1.4
     */
    public static final int MAXIMIZED_BOTH = MAXIMIZED_VERT | MAXIMIZED_HORIZ;

    /**
     * Maximized bounds for this frame.
     * @see     #setMaximizedBounds(Rectangle)
     * @see     #getMaximizedBounds
     * @serial
     * @since 1.4
     */
    Rectangle maximizedBounds;


    /**
     * This is the title of the frame.  It can be changed
     * at any time.  {@code title} can be null and if
     * this is the case the {@code title} = "".
     *
     * @serial
     * @see #getTitle
     * @see #setTitle(String)
     */
    String      title = "Untitled";

    /**
     * The frames menubar.  If {@code menuBar} = null
     * the frame will not have a menubar.
     *
     * @serial
     * @see #getMenuBar
     * @see #setMenuBar(MenuBar)
     */
    MenuBar     menuBar;

    /**
     * This field indicates whether the frame is resizable.
     * This property can be changed at any time.
     * {@code resizable} will be true if the frame is
     * resizable, otherwise it will be false.
     *
     * @serial
     * @see #isResizable()
     */
    boolean     resizable = true;

    /**
     * This field indicates whether the frame is undecorated.
     * This property can only be changed while the frame is not displayable.
     * {@code undecorated} will be true if the frame is
     * undecorated, otherwise it will be false.
     *
     * @serial
     * @see #setUndecorated(boolean)
     * @see #isUndecorated()
     * @see Component#isDisplayable()
     * @since 1.4
     */
    boolean undecorated = false;

    /**
     * {@code mbManagement} is only used by the Motif implementation.
     *
     * @serial
     */
    boolean     mbManagement = false;   /* used only by the Motif impl. */

    /**
     * The bitwise mask of frame state constants.
     */
    // XXX: uwe: abuse old field for now
    // will need to take care of serialization
    private int state = NORMAL;

    /**
     * The Windows owned by the Frame.
     * Note: in 1.2 this has been superseded by Window.ownedWindowList
     *
     * @serial
     * @see java.awt.Window#ownedWindowList
     */
    Vector<Window> ownedWindows;

    private static final String base = "frame";
    private static int nameCounter = 0;

    /**
     * Use serialVersionUID from JDK 1.1 for interoperability.
     */
     @Serial
     private static final long serialVersionUID = 2673458971256075116L;

    static {
        /* ensure that the necessary native libraries are loaded */
        Toolkit.loadLibraries();
        if (!GraphicsEnvironment.isHeadless()) {
            initIDs();
        }
    }

    /**
     * Constructs a new instance of {@code Frame} that is
     * initially invisible.  The title of the {@code Frame}
     * is empty.
     * @exception HeadlessException when
     *     {@code GraphicsEnvironment.isHeadless()} returns {@code true}
     * @see java.awt.GraphicsEnvironment#isHeadless()
     * @see Component#setSize
     * @see Component#setVisible(boolean)
     */
    public Frame() throws HeadlessException {
        this("");
    }

    /**
     * Constructs a new, initially invisible {@code Frame} with the
     * specified {@code GraphicsConfiguration}.
     *
     * @param gc the {@code GraphicsConfiguration}
     * of the target screen device. If {@code gc}
     * is {@code null}, the system default
     * {@code GraphicsConfiguration} is assumed.
     * @exception IllegalArgumentException if
     * {@code gc} is not from a screen device.
     * @exception HeadlessException when
     *     {@code GraphicsEnvironment.isHeadless()} returns {@code true}
     * @see java.awt.GraphicsEnvironment#isHeadless()
     * @since     1.3
     */
    public Frame(GraphicsConfiguration gc) {
        this("", gc);
    }

    /**
     * Constructs a new, initially invisible {@code Frame} object
     * with the specified title.
     * @param title the title to be displayed in the frame's border.
     *              A {@code null} value
     *              is treated as an empty string, "".
     * @exception HeadlessException when
     *     {@code GraphicsEnvironment.isHeadless()} returns {@code true}
     * @see java.awt.GraphicsEnvironment#isHeadless()
     * @see java.awt.Component#setSize
     * @see java.awt.Component#setVisible(boolean)
     * @see java.awt.GraphicsConfiguration#getBounds
     */
    public Frame(String title) throws HeadlessException {
        init(title, null);
    }

    /**
     * Constructs a new, initially invisible {@code Frame} object
     * with the specified title and a
     * {@code GraphicsConfiguration}.
     * @param title the title to be displayed in the frame's border.
     *              A {@code null} value
     *              is treated as an empty string, "".
     * @param gc the {@code GraphicsConfiguration}
     * of the target screen device.  If {@code gc} is
     * {@code null}, the system default
     * {@code GraphicsConfiguration} is assumed.
     * @exception IllegalArgumentException if {@code gc}
     * is not from a screen device.
     * @exception HeadlessException when
     *     {@code GraphicsEnvironment.isHeadless()} returns {@code true}
     * @see java.awt.GraphicsEnvironment#isHeadless()
     * @see java.awt.Component#setSize
     * @see java.awt.Component#setVisible(boolean)
     * @see java.awt.GraphicsConfiguration#getBounds
     * @since 1.3
     */
    public Frame(String title, GraphicsConfiguration gc) {
        super(gc);
        init(title, gc);
    }

    private void init(String title, GraphicsConfiguration gc) {
        this.title = title;
        SunToolkit.checkAndSetPolicy(this);
    }

    /**
     * Construct a name for this component.  Called by getName() when the
     * name is null.
     */
    String constructComponentName() {
        synchronized (Frame.class) {
            return base + nameCounter++;
        }
    }

    /**
     * Makes this Frame displayable by connecting it to
     * a native screen resource.  Making a frame displayable will
     * cause any of its children to be made displayable.
     * This method is called internally by the toolkit and should
     * not be called directly by programs.
     * @see Component#isDisplayable
     * @see #removeNotify
     */
    public void addNotify() {
        synchronized (getTreeLock()) {
            if (peer == null) {
                peer = getComponentFactory().createFrame(this);
            }
            FramePeer p = (FramePeer)peer;
            MenuBar menuBar = this.menuBar;
            if (menuBar != null) {
                mbManagement = true;
                menuBar.addNotify();
                p.setMenuBar(menuBar);
            }
            p.setMaximizedBounds(maximizedBounds);
            super.addNotify();
        }
    }

    /**
     * Gets the title of the frame.  The title is displayed in the
     * frame's border.
     * @return    the title of this frame, or an empty string ("")
     *                if this frame doesn't have a title.
     * @see       #setTitle(String)
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title for this frame to the specified string.
     * @param title the title to be displayed in the frame's border.
     *              A {@code null} value
     *              is treated as an empty string, "".
     * @see      #getTitle
     */
    public void setTitle(String title) {
        String oldTitle = this.title;
        if (title == null) {
            title = "";
        }


        synchronized(this) {
            this.title = title;
            FramePeer peer = (FramePeer)this.peer;
            if (peer != null) {
                peer.setTitle(title);
            }
        }
        firePropertyChange("title", oldTitle, title);
    }

    /**
     * Returns the image to be displayed as the icon for this frame.
     * <p>
     * This method is obsolete and kept for backward compatibility
     * only. Use {@link Window#getIconImages Window.getIconImages()} instead.
     * <p>
     * If a list of several images was specified as a Window's icon,
     * this method will return the first item of the list.
     *
     * @return    the icon image for this frame, or {@code null}
     *                    if this frame doesn't have an icon image.
     * @see       #setIconImage(Image)
     * @see       Window#getIconImages()
     * @see       Window#setIconImages
     */
    public Image getIconImage() {
        java.util.List<Image> icons = this.icons;
        if (icons != null) {
            if (icons.size() > 0) {
                return icons.get(0);
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void setIconImage(Image image) {
        super.setIconImage(image);
    }

    /**
     * Gets the menu bar for this frame.
     * @return    the menu bar for this frame, or {@code null}
     *                   if this frame doesn't have a menu bar.
     * @see       #setMenuBar(MenuBar)
     */
    public MenuBar getMenuBar() {
        return menuBar;
    }

    /**
     * Sets the menu bar for this frame to the specified menu bar.
     * @param     mb the menu bar being set.
     *            If this parameter is {@code null} then any
     *            existing menu bar on this frame is removed.
     * @see       #getMenuBar
     */
    public void setMenuBar(MenuBar mb) {
        synchronized (getTreeLock()) {
            if (menuBar == mb) {
                return;
            }
            if ((mb != null) && (mb.parent != null)) {
                mb.parent.remove(mb);
            }
            if (menuBar != null) {
                remove(menuBar);
            }
            menuBar = mb;
            if (menuBar != null) {
                menuBar.parent = this;

                FramePeer peer = (FramePeer)this.peer;
                if (peer != null) {
                    mbManagement = true;
                    menuBar.addNotify();
                    invalidateIfValid();
                    peer.setMenuBar(menuBar);
                }
            }
        }
    }

    /**
     * Indicates whether this frame is resizable by the user.
     * By default, all frames are initially resizable.
     * @return    {@code true} if the user can resize this frame;
     *                        {@code false} otherwise.
     * @see       java.awt.Frame#setResizable(boolean)
     */
    public boolean isResizable() {
        return resizable;
    }

    /**
     * Sets whether this frame is resizable by the user.
     * @param    resizable   {@code true} if this frame is resizable;
     *                       {@code false} otherwise.
     * @see      java.awt.Frame#isResizable
     */
    public void setResizable(boolean resizable) {
        boolean oldResizable = this.resizable;
        boolean testvalid = false;

        synchronized (this) {
            this.resizable = resizable;
            FramePeer peer = (FramePeer)this.peer;
            if (peer != null) {
                peer.setResizable(resizable);
                testvalid = true;
            }
        }

        // On some platforms, changing the resizable state affects
        // the insets of the Frame. If we could, we'd call invalidate()
        // from the peer, but we need to guarantee that we're not holding
        // the Frame lock when we call invalidate().
        if (testvalid) {
            invalidateIfValid();
        }
        firePropertyChange("resizable", oldResizable, resizable);
    }


    /**
     * Sets the state of this frame (obsolete).
     * <p>
     * In older versions of JDK a frame state could only be NORMAL or
     * ICONIFIED.  Since JDK 1.4 set of supported frame states is
     * expanded and frame state is represented as a bitwise mask.
     * <p>
     * For compatibility with applications developed
     * earlier this method still accepts
     * {@code Frame.NORMAL} and
     * {@code Frame.ICONIFIED} only.  The iconic
     * state of the frame is only changed, other aspects
     * of frame state are not affected by this method. If
     * the state passed to this method is neither {@code
     * Frame.NORMAL} nor {@code Frame.ICONIFIED} the
     * method performs no actions at all.
     * <p>Note that if the state is not supported on a
     * given platform, neither the state nor the return
     * value of the {@link #getState} method will be
     * changed. The application may determine whether a
     * specific state is supported via the {@link
     * java.awt.Toolkit#isFrameStateSupported} method.
     * <p><b>If the frame is currently visible on the
     * screen</b> (the {@link #isShowing} method returns
     * {@code true}), the developer should examine the
     * return value of the  {@link
     * java.awt.event.WindowEvent#getNewState} method of
     * the {@code WindowEvent} received through the
     * {@link java.awt.event.WindowStateListener} to
     * determine that the state has actually been
     * changed.
     * <p><b>If the frame is not visible on the
     * screen</b>, the events may or may not be
     * generated.  In this case the developer may assume
     * that the state changes immediately after this
     * method returns.  Later, when the {@code
     * setVisible(true)} method is invoked, the frame
     * will attempt to apply this state. Receiving any
     * {@link
     * java.awt.event.WindowEvent#WINDOW_STATE_CHANGED}
     * events is not guaranteed in this case also.
     *
     * @param state either {@code Frame.NORMAL} or
     *     {@code Frame.ICONIFIED}.
     * @see #setExtendedState(int)
     * @see java.awt.Window#addWindowStateListener
     */
    public synchronized void setState(int state) {
        int current = getExtendedState();
        if (state == ICONIFIED && (current & ICONIFIED) == 0) {
            setExtendedState(current | ICONIFIED);
        }
        else if (state == NORMAL && (current & ICONIFIED) != 0) {
            setExtendedState(current & ~ICONIFIED);
        }
    }

    /**
     * Sets the state of this frame. The state is
     * represented as a bitwise mask.
     * <ul>
     * <li>{@code NORMAL}
     * <br>Indicates that no state bits are set.
     * <li>{@code ICONIFIED}
     * <li>{@code MAXIMIZED_HORIZ}
     * <li>{@code MAXIMIZED_VERT}
     * <li>{@code MAXIMIZED_BOTH}
     * <br>Concatenates {@code MAXIMIZED_HORIZ}
     * and {@code MAXIMIZED_VERT}.
     * </ul>
     * <p>Note that if the state is not supported on a
     * given platform, neither the state nor the return
     * value of the {@link #getExtendedState} method will
     * be changed. The application may determine whether
     * a specific state is supported via the {@link
     * java.awt.Toolkit#isFrameStateSupported} method.
     * <p><b>If the frame is currently visible on the
     * screen</b> (the {@link #isShowing} method returns
     * {@code true}), the developer should examine the
     * return value of the {@link
     * java.awt.event.WindowEvent#getNewState} method of
     * the {@code WindowEvent} received through the
     * {@link java.awt.event.WindowStateListener} to
     * determine that the state has actually been
     * changed.
     * <p><b>If the frame is not visible on the
     * screen</b>, the events may or may not be
     * generated.  In this case the developer may assume
     * that the state changes immediately after this
     * method returns.  Later, when the {@code
     * setVisible(true)} method is invoked, the frame
     * will attempt to apply this state. Receiving any
     * {@link
     * java.awt.event.WindowEvent#WINDOW_STATE_CHANGED}
     * events is not guaranteed in this case also.
     *
     * @param state a bitwise mask of frame state constants
     * @since   1.4
     * @see java.awt.Window#addWindowStateListener
     */
    public void setExtendedState(int state) {
        if ( !isFrameStateSupported( state ) ) {
            return;
        }
        synchronized (getObjectLock()) {
            this.state = state;
        }
        // peer.setState must be called outside of object lock
        // synchronization block to avoid possible deadlock
        FramePeer peer = (FramePeer)this.peer;
        if (peer != null) {
            peer.setState(state);
        }
    }
    private boolean isFrameStateSupported(int state) {
        if( !getToolkit().isFrameStateSupported( state ) ) {
            // * Toolkit.isFrameStateSupported returns always false
            // on compound state even if all parts are supported;
            // * if part of state is not supported, state is not supported;
            // * MAXIMIZED_BOTH is not a compound state.
            if( ((state & ICONIFIED) != 0) &&
                !getToolkit().isFrameStateSupported( ICONIFIED )) {
                return false;
            }else {
                state &= ~ICONIFIED;
            }
            return getToolkit().isFrameStateSupported( state );
        }
        return true;
    }

    /**
     * Gets the state of this frame (obsolete).
     * <p>
     * In older versions of JDK a frame state could only be NORMAL or
     * ICONIFIED.  Since JDK 1.4 set of supported frame states is
     * expanded and frame state is represented as a bitwise mask.
     * <p>
     * For compatibility with old programs this method still returns
     * {@code Frame.NORMAL} and {@code Frame.ICONIFIED} but
     * it only reports the iconic state of the frame, other aspects of
     * frame state are not reported by this method.
     *
     * @return  {@code Frame.NORMAL} or {@code Frame.ICONIFIED}.
     * @see     #setState(int)
     * @see     #getExtendedState
     */
    public synchronized int getState() {
        return (getExtendedState() & ICONIFIED) != 0 ? ICONIFIED : NORMAL;
    }


    /**
     * Gets the state of this frame. The state is
     * represented as a bitwise mask.
     * <ul>
     * <li>{@code NORMAL}
     * <br>Indicates that no state bits are set.
     * <li>{@code ICONIFIED}
     * <li>{@code MAXIMIZED_HORIZ}
     * <li>{@code MAXIMIZED_VERT}
     * <li>{@code MAXIMIZED_BOTH}
     * <br>Concatenates {@code MAXIMIZED_HORIZ}
     * and {@code MAXIMIZED_VERT}.
     * </ul>
     *
     * @return  a bitwise mask of frame state constants
     * @see     #setExtendedState(int)
     * @since 1.4
     */
    public int getExtendedState() {
        synchronized (getObjectLock()) {
            return state;
        }
    }

    static {
        AWTAccessor.setFrameAccessor(
            new AWTAccessor.FrameAccessor() {
                public void setExtendedState(Frame frame, int state) {
                    synchronized(frame.getObjectLock()) {
                        frame.state = state;
                    }
                }
                public int getExtendedState(Frame frame) {
                    synchronized(frame.getObjectLock()) {
                        return frame.state;
                    }
                }
                public Rectangle getMaximizedBounds(Frame frame) {
                    synchronized(frame.getObjectLock()) {
                        return frame.maximizedBounds;
                    }
                }
            }
        );
    }

    /**
     * Sets the maximized bounds for this frame.
     * <p>
     * When a frame is in maximized state the system supplies some
     * defaults bounds.  This method allows some or all of those
     * system supplied values to be overridden.
     * <p>
     * If {@code bounds} is {@code null}, accept bounds
     * supplied by the system.  If non-{@code null} you can
     * override some of the system supplied values while accepting
     * others by setting those fields you want to accept from system
     * to {@code Integer.MAX_VALUE}.
     * <p>
     * Note, the given maximized bounds are used as a hint for the native
     * system, because the underlying platform may not support setting the
     * location and/or size of the maximized windows.  If that is the case, the
     * provided values do not affect the appearance of the frame in the
     * maximized state.
     *
     * @param bounds  bounds for the maximized state
     * @see #getMaximizedBounds()
     * @since 1.4
     */
    public void setMaximizedBounds(Rectangle bounds) {
        synchronized(getObjectLock()) {
            this.maximizedBounds = bounds;
        }
        FramePeer peer = (FramePeer)this.peer;
        if (peer != null) {
            peer.setMaximizedBounds(bounds);
        }
    }

    /**
     * Gets maximized bounds for this frame.
     * Some fields may contain {@code Integer.MAX_VALUE} to indicate
     * that system supplied values for this field must be used.
     *
     * @return  maximized bounds for this frame;  may be {@code null}
     * @see     #setMaximizedBounds(Rectangle)
     * @since   1.4
     */
    public Rectangle getMaximizedBounds() {
        synchronized(getObjectLock()) {
            return maximizedBounds;
        }
    }


    /**
     * Disables or enables decorations for this frame.
     * <p>
     * This method can only be called while the frame is not displayable. To
     * make this frame decorated, it must be opaque and have the default shape,
     * otherwise the {@code IllegalComponentStateException} will be thrown.
     * Refer to {@link Window#setShape}, {@link Window#setOpacity} and {@link
     * Window#setBackground} for details
     *
     * @param  undecorated {@code true} if no frame decorations are to be
     *         enabled; {@code false} if frame decorations are to be enabled
     *
     * @throws IllegalComponentStateException if the frame is displayable
     * @throws IllegalComponentStateException if {@code undecorated} is
     *      {@code false}, and this frame does not have the default shape
     * @throws IllegalComponentStateException if {@code undecorated} is
     *      {@code false}, and this frame opacity is less than {@code 1.0f}
     * @throws IllegalComponentStateException if {@code undecorated} is
     *      {@code false}, and the alpha value of this frame background
     *      color is less than {@code 1.0f}
     *
     * @see    #isUndecorated
     * @see    Component#isDisplayable
     * @see    Window#getShape
     * @see    Window#getOpacity
     * @see    Window#getBackground
     * @see    javax.swing.JFrame#setDefaultLookAndFeelDecorated(boolean)
     *
     * @since 1.4
     */
    public void setUndecorated(boolean undecorated) {
        /* Make sure we don't run in the middle of peer creation.*/
        synchronized (getTreeLock()) {
            if (isDisplayable()) {
                throw new IllegalComponentStateException("The frame is displayable.");
            }
            if (!undecorated) {
                if (getOpacity() < 1.0f) {
                    throw new IllegalComponentStateException("The frame is not opaque");
                }
                if (getShape() != null) {
                    throw new IllegalComponentStateException("The frame does not have a default shape");
                }
                Color bg = getBackground();
                if ((bg != null) && (bg.getAlpha() < 255)) {
                    throw new IllegalComponentStateException("The frame background color is not opaque");
                }
            }
            this.undecorated = undecorated;
        }
    }

    /**
     * Indicates whether this frame is undecorated.
     * By default, all frames are initially decorated.
     * @return    {@code true} if frame is undecorated;
     *                        {@code false} otherwise.
     * @see       java.awt.Frame#setUndecorated(boolean)
     * @since 1.4
     */
    public boolean isUndecorated() {
        return undecorated;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOpacity(float opacity) {
        synchronized (getTreeLock()) {
            if ((opacity < 1.0f) && !isUndecorated()) {
                throw new IllegalComponentStateException("The frame is decorated");
            }
            super.setOpacity(opacity);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setShape(Shape shape) {
        synchronized (getTreeLock()) {
            if ((shape != null) && !isUndecorated()) {
                throw new IllegalComponentStateException("The frame is decorated");
            }
            super.setShape(shape);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBackground(Color bgColor) {
        synchronized (getTreeLock()) {
            if ((bgColor != null) && (bgColor.getAlpha() < 255) && !isUndecorated()) {
                throw new IllegalComponentStateException("The frame is decorated");
            }
            super.setBackground(bgColor);
        }
    }

    /**
     * Removes the specified menu bar from this frame.
     * @param    m   the menu component to remove.
     *           If {@code m} is {@code null}, then
     *           no action is taken
     */
    public void remove(MenuComponent m) {
        if (m == null) {
            return;
        }
        synchronized (getTreeLock()) {
            if (m == menuBar) {
                menuBar = null;
                FramePeer peer = (FramePeer)this.peer;
                if (peer != null) {
                    mbManagement = true;
                    invalidateIfValid();
                    peer.setMenuBar(null);
                    m.removeNotify();
                }
                m.parent = null;
            } else {
                super.remove(m);
            }
        }
    }

    /**
     * Makes this Frame undisplayable by removing its connection
     * to its native screen resource. Making a Frame undisplayable
     * will cause any of its children to be made undisplayable.
     * This method is called by the toolkit internally and should
     * not be called directly by programs.
     * @see Component#isDisplayable
     * @see #addNotify
     */
    public void removeNotify() {
        synchronized (getTreeLock()) {
            FramePeer peer = (FramePeer)this.peer;
            if (peer != null) {
                // get the latest Frame state before disposing
                getState();

                if (menuBar != null) {
                    mbManagement = true;
                    peer.setMenuBar(null);
                    menuBar.removeNotify();
                }
            }
            super.removeNotify();
        }
    }

    void postProcessKeyEvent(KeyEvent e) {
        if (menuBar != null && menuBar.handleShortcut(e)) {
            e.consume();
            return;
        }
        super.postProcessKeyEvent(e);
    }

    /**
     * Returns a string representing the state of this {@code Frame}.
     * This method is intended to be used only for debugging purposes, and the
     * content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not be
     * {@code null}.
     *
     * @return the parameter string of this frame
     */
    protected String paramString() {
        String str = super.paramString();
        if (title != null) {
            str += ",title=" + title;
        }
        if (resizable) {
            str += ",resizable";
        }
        int state = getExtendedState();
        if (state == NORMAL) {
            str += ",normal";
        }
        else {
            if ((state & ICONIFIED) != 0) {
                str += ",iconified";
            }
            if ((state & MAXIMIZED_BOTH) == MAXIMIZED_BOTH) {
                str += ",maximized";
            }
            else if ((state & MAXIMIZED_HORIZ) != 0) {
                str += ",maximized_horiz";
            }
            else if ((state & MAXIMIZED_VERT) != 0) {
                str += ",maximized_vert";
            }
        }
        return str;
    }

    /**
     * Sets the cursor for this frame to the specified type.
     *
     * @param  cursorType the cursor type
     * @deprecated As of JDK version 1.1,
     * replaced by {@code Component.setCursor(Cursor)}.
     */
    @Deprecated
    public void setCursor(int cursorType) {
        if (cursorType < DEFAULT_CURSOR || cursorType > MOVE_CURSOR) {
            throw new IllegalArgumentException("illegal cursor type");
        }
        setCursor(Cursor.getPredefinedCursor(cursorType));
    }

    /**
     * @deprecated As of JDK version 1.1,
     * replaced by {@code Component.getCursor()}.
     * @return the cursor type for this frame
     */
    @Deprecated
    public int getCursorType() {
        return (getCursor().getType());
    }

    /**
     * Returns an array of all {@code Frame}s created by this application.
     * If called from an applet, the array includes only the {@code Frame}s
     * accessible by that applet.
     * <p>
     * <b>Warning:</b> this method may return system created frames, such
     * as a shared, hidden frame which is used by Swing. Applications
     * should not assume the existence of these frames, nor should an
     * application assume anything about these frames such as component
     * positions, {@code LayoutManager}s or serialization.
     * <p>
     * <b>Note</b>: To obtain a list of all ownerless windows, including
     * ownerless {@code Dialog}s (introduced in release 1.6), use {@link
     * Window#getOwnerlessWindows Window.getOwnerlessWindows}.
     *
     * @return the array of all {@code Frame}s created by this application
     *
     * @see Window#getWindows()
     * @see Window#getOwnerlessWindows
     *
     * @since 1.2
     */
    public static Frame[] getFrames() {
        Window[] allWindows = Window.getWindows();

        int frameCount = 0;
        for (Window w : allWindows) {
            if (w instanceof Frame) {
                frameCount++;
            }
        }

        Frame[] frames = new Frame[frameCount];
        int c = 0;
        for (Window w : allWindows) {
            if (w instanceof Frame) {
                frames[c++] = (Frame)w;
            }
        }

        return frames;
    }

    /* Serialization support.  If there's a MenuBar we restore
     * its (transient) parent field here.  Likewise for top level
     * windows that are "owned" by this frame.
     */

    /**
     * {@code Frame}'s Serialized Data Version.
     *
     * @serial
     */
    private int frameSerializedDataVersion = 1;

    /**
     * Writes default serializable fields to stream.  Writes
     * an optional serializable icon {@code Image}, which is
     * available as of 1.4.
     *
     * @param  s the {@code ObjectOutputStream} to write
     * @throws IOException if an I/O error occurs
     * @serialData an optional icon {@code Image}
     * @see java.awt.Image
     * @see #getIconImage
     * @see #setIconImage(Image)
     * @see #readObject(ObjectInputStream)
     */
    @Serial
    private void writeObject(ObjectOutputStream s)
      throws IOException
    {
        s.defaultWriteObject();
        if (icons != null && icons.size() > 0) {
            Image icon1 = icons.get(0);
            if (icon1 instanceof Serializable) {
                s.writeObject(icon1);
                return;
            }
        }
        s.writeObject(null);
    }

    /**
     * Reads the {@code ObjectInputStream}.  Tries
     * to read an icon {@code Image}, which is optional
     * data available as of 1.4.  If an icon {@code Image}
     * is not available, but anything other than an EOF
     * is detected, an {@code OptionalDataException}
     * will be thrown.
     * Unrecognized keys or values will be ignored.
     *
     * @param  s the {@code ObjectInputStream} to read
     * @throws ClassNotFoundException if the class of a serialized object could
     *         not be found
     * @throws java.io.OptionalDataException if an icon {@code Image} is not
     *         available, but anything other than an EOF is detected
     * @throws HeadlessException if {@code GraphicsEnvironment.isHeadless()}
     *         returns {@code true}
     * @see java.awt.GraphicsEnvironment#isHeadless()
     * @see java.awt.Image
     * @see #getIconImage
     * @see #setIconImage(Image)
     * @see #writeObject(ObjectOutputStream)
     */
    @Serial
    private void readObject(ObjectInputStream s)
      throws ClassNotFoundException, IOException, HeadlessException
    {
      // HeadlessException is thrown by Window's readObject
      s.defaultReadObject();
      try {
          Image icon = (Image) s.readObject();
          if (icons == null) {
              icons = new ArrayList<Image>();
              icons.add(icon);
          }
      } catch (java.io.OptionalDataException e) {
          // pre-1.4 instances will not have this optional data.
          // 1.6 and later instances serialize icons in the Window class
          // e.eof will be true to indicate that there is no more
          // data available for this object.

          // If e.eof is not true, throw the exception as it
          // might have been caused by unrelated reasons.
          if (!e.eof) {
              throw (e);
          }
      }

      if (menuBar != null)
        menuBar.parent = this;

      // Ensure 1.1 serialized Frames can read & hook-up
      // owned windows properly
      //
      if (ownedWindows != null) {
          for (int i = 0; i < ownedWindows.size(); i++) {
              connectOwnedWindow(ownedWindows.elementAt(i));
          }
          ownedWindows = null;
      }
    }

    /**
     * Initialize JNI field and method IDs
     */
    private static native void initIDs();

    /*
     * --- Accessibility Support ---
     *
     */

    /**
     * Gets the AccessibleContext associated with this Frame.
     * For frames, the AccessibleContext takes the form of an
     * AccessibleAWTFrame.
     * A new AccessibleAWTFrame instance is created if necessary.
     *
     * @return an AccessibleAWTFrame that serves as the
     *         AccessibleContext of this Frame
     * @since 1.3
     */
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleAWTFrame();
        }
        return accessibleContext;
    }

    /**
     * This class implements accessibility support for the
     * {@code Frame} class.  It provides an implementation of the
     * Java Accessibility API appropriate to frame user-interface elements.
     * @since 1.3
     */
    protected class AccessibleAWTFrame extends AccessibleAWTWindow
    {
        /**
         * Use serialVersionUID from JDK 1.3 for interoperability.
         */
        @Serial
        private static final long serialVersionUID = -6172960752956030250L;

        /**
         * Constructs an {@code AccessibleAWTFrame}.
         */
        protected AccessibleAWTFrame() {}

        /**
         * Get the role of this object.
         *
         * @return an instance of AccessibleRole describing the role of the
         * object
         * @see AccessibleRole
         */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.FRAME;
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
            if (getFocusOwner() != null) {
                states.add(AccessibleState.ACTIVE);
            }
            if (isResizable()) {
                states.add(AccessibleState.RESIZABLE);
            }
            return states;
        }


    } // inner class AccessibleAWTFrame

}
