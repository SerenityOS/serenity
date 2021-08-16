/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.event.*;
import java.awt.peer.TrayIconPeer;
import sun.awt.AppContext;
import sun.awt.SunToolkit;
import sun.awt.AWTAccessor;
import sun.awt.HeadlessToolkit;
import java.util.EventObject;
import java.security.AccessControlContext;
import java.security.AccessController;

/**
 * A {@code TrayIcon} object represents a tray icon that can be
 * added to the {@link SystemTray system tray}. A
 * {@code TrayIcon} can have a tooltip (text), an image, a popup
 * menu, and a set of listeners associated with it.
 *
 * <p>A {@code TrayIcon} can generate various {@link MouseEvent
 * MouseEvents} and supports adding corresponding listeners to receive
 * notification of these events.  {@code TrayIcon} processes some
 * of the events by itself.  For example, by default, when the
 * right-mouse click is performed on the {@code TrayIcon} it
 * displays the specified popup menu.  When the mouse hovers
 * over the {@code TrayIcon} the tooltip is displayed (this behaviour is
 * platform dependent).
 *
 * <p><strong>Note:</strong> When the {@code MouseEvent} is
 * dispatched to its registered listeners its {@code component}
 * property will be set to {@code null}.  (See {@link
 * java.awt.event.ComponentEvent#getComponent}) The
 * {@code source} property will be set to this
 * {@code TrayIcon}. (See {@link
 * java.util.EventObject#getSource})
 *
 * <p><b>Note:</b> A well-behaved {@link TrayIcon} implementation
 * will assign different gestures to showing a popup menu and
 * selecting a tray icon.
 *
 * <p>A {@code TrayIcon} can generate an {@link ActionEvent
 * ActionEvent}.  On some platforms, this occurs when the user selects
 * the tray icon using either the mouse or keyboard.
 *
 * <p>If a SecurityManager is installed, the AWTPermission
 * {@code accessSystemTray} must be granted in order to create
 * a {@code TrayIcon}. Otherwise the constructor will throw a
 * SecurityException.
 *
 * <p> See the {@link SystemTray} class overview for an example on how
 * to use the {@code TrayIcon} API.
 *
 * @implNote
 * When the {@systemProperty apple.awt.enableTemplateImages} property is
 * set, all images associated with instances of this class are treated
 * as template images by the native desktop system. This means all color
 * information is discarded, and the image is adapted automatically to
 * be visible when desktop theme and/or colors change. This property
 * only affects MacOSX.
 *
 * @since 1.6
 * @see SystemTray#add
 * @see java.awt.event.ComponentEvent#getComponent
 * @see java.util.EventObject#getSource
 *
 * @author Bino George
 * @author Denis Mikhalkin
 * @author Sharon Zakhour
 * @author Anton Tarasov
 */
public class TrayIcon {

    private Image image;
    private String tooltip;
    private PopupMenu popup;
    private boolean autosize;
    private int id;
    private String actionCommand;

    private transient TrayIconPeer peer;

    transient MouseListener mouseListener;
    transient MouseMotionListener mouseMotionListener;
    transient ActionListener actionListener;

    /*
     * The tray icon's AccessControlContext.
     *
     * Unlike the acc in Component, this field is made final
     * because TrayIcon is not serializable.
     */
    @SuppressWarnings("removal")
    private final AccessControlContext acc = AccessController.getContext();

    /*
     * Returns the acc this tray icon was constructed with.
     */
    @SuppressWarnings("removal")
    final AccessControlContext getAccessControlContext() {
        if (acc == null) {
            throw new SecurityException("TrayIcon is missing AccessControlContext");
        }
        return acc;
    }

    static {
        Toolkit.loadLibraries();
        if (!GraphicsEnvironment.isHeadless()) {
            initIDs();
        }

        AWTAccessor.setTrayIconAccessor(
            new AWTAccessor.TrayIconAccessor() {
                public void addNotify(TrayIcon trayIcon) throws AWTException {
                    trayIcon.addNotify();
                }
                public void removeNotify(TrayIcon trayIcon) {
                    trayIcon.removeNotify();
                }
            });
    }

    private TrayIcon()
      throws UnsupportedOperationException, HeadlessException, SecurityException
    {
        SystemTray.checkSystemTrayAllowed();
        if (GraphicsEnvironment.isHeadless()) {
            throw new HeadlessException();
        }
        if (!SystemTray.isSupported()) {
            throw new UnsupportedOperationException();
        }
        SunToolkit.insertTargetMapping(this, AppContext.getAppContext());
    }

    /**
     * Creates a {@code TrayIcon} with the specified image.
     *
     * @param image the {@code Image} to be used
     * @throws IllegalArgumentException if {@code image} is
     * {@code null}
     * @throws UnsupportedOperationException if the system tray isn't
     * supported by the current platform
     * @throws HeadlessException if
     * {@code GraphicsEnvironment.isHeadless()} returns {@code true}
     * @throws SecurityException if {@code accessSystemTray} permission
     * is not granted
     * @see SystemTray#add(TrayIcon)
     * @see TrayIcon#TrayIcon(Image, String, PopupMenu)
     * @see TrayIcon#TrayIcon(Image, String)
     * @see SecurityManager#checkPermission
     * @see AWTPermission
     */
    public TrayIcon(Image image) {
        this();
        if (image == null) {
            throw new IllegalArgumentException("creating TrayIcon with null Image");
        }
        setImage(image);
    }

    /**
     * Creates a {@code TrayIcon} with the specified image and
     * tooltip text. Tooltip may be not visible on some platforms.
     *
     * @param image the {@code Image} to be used
     * @param tooltip the string to be used as tooltip text; if the
     * value is {@code null} no tooltip is shown
     * @throws IllegalArgumentException if {@code image} is
     * {@code null}
     * @throws UnsupportedOperationException if the system tray isn't
     * supported by the current platform
     * @throws HeadlessException if
     * {@code GraphicsEnvironment.isHeadless()} returns {@code true}
     * @throws SecurityException if {@code accessSystemTray} permission
     * is not granted
     * @see SystemTray#add(TrayIcon)
     * @see TrayIcon#TrayIcon(Image)
     * @see TrayIcon#TrayIcon(Image, String, PopupMenu)
     * @see SecurityManager#checkPermission
     * @see AWTPermission
     */
    public TrayIcon(Image image, String tooltip) {
        this(image);
        setToolTip(tooltip);
    }

    /**
     * Creates a {@code TrayIcon} with the specified image,
     * tooltip and popup menu. Tooltip may be not visible on some platforms.
     *
     * @param image the {@code Image} to be used
     * @param tooltip the string to be used as tooltip text; if the
     * value is {@code null} no tooltip is shown
     * @param popup the menu to be used for the tray icon's popup
     * menu; if the value is {@code null} no popup menu is shown
     * @throws IllegalArgumentException if {@code image} is {@code null}
     * @throws UnsupportedOperationException if the system tray isn't
     * supported by the current platform
     * @throws HeadlessException if
     * {@code GraphicsEnvironment.isHeadless()} returns {@code true}
     * @throws SecurityException if {@code accessSystemTray} permission
     * is not granted
     * @see SystemTray#add(TrayIcon)
     * @see TrayIcon#TrayIcon(Image, String)
     * @see TrayIcon#TrayIcon(Image)
     * @see PopupMenu
     * @see MouseListener
     * @see #addMouseListener(MouseListener)
     * @see SecurityManager#checkPermission
     * @see AWTPermission
     */
    public TrayIcon(Image image, String tooltip, PopupMenu popup) {
        this(image, tooltip);
        setPopupMenu(popup);
    }

    /**
     * Sets the image for this {@code TrayIcon}.  The previous
     * tray icon image is discarded without calling the {@link
     * java.awt.Image#flush} method &#8212; you will need to call it
     * manually.
     *
     * <p> If the image represents an animated image, it will be
     * animated automatically.
     *
     * <p> See the {@link #setImageAutoSize(boolean)} property for
     * details on the size of the displayed image.
     *
     * <p> Calling this method with the same image that is currently
     * being used has no effect.
     *
     * @throws NullPointerException if {@code image} is {@code null}
     * @param image the non-null {@code Image} to be used
     * @see #getImage
     * @see Image
     * @see SystemTray#add(TrayIcon)
     * @see TrayIcon#TrayIcon(Image, String)
     */
    public void setImage(Image image) {
        if (image == null) {
            throw new NullPointerException("setting null Image");
        }
        this.image = image;

        TrayIconPeer peer = this.peer;
        if (peer != null) {
            peer.updateImage();
        }
    }

    /**
     * Returns the current image used for this {@code TrayIcon}.
     *
     * @return the image
     * @see #setImage(Image)
     * @see Image
     */
    public Image getImage() {
        return image;
    }

    /**
     * Sets the popup menu for this {@code TrayIcon}.  If
     * {@code popup} is {@code null}, no popup menu will be
     * associated with this {@code TrayIcon}.
     *
     * <p>Note that this {@code popup} must not be added to any
     * parent before or after it is set on the tray icon.  If you add
     * it to some parent, the {@code popup} may be removed from
     * that parent.
     *
     * <p>The {@code popup} can be set on one {@code TrayIcon} only.
     * Setting the same popup on multiple {@code TrayIcon}s will cause
     * an {@code IllegalArgumentException}.
     *
     * <p><strong>Note:</strong> Some platforms may not support
     * showing the user-specified popup menu component when the user
     * right-clicks the tray icon.  In this situation, either no menu
     * will be displayed or, on some systems, a native version of the
     * menu may be displayed.
     *
     * @throws IllegalArgumentException if the {@code popup} is already
     * set for another {@code TrayIcon}
     * @param popup a {@code PopupMenu} or {@code null} to
     * remove any popup menu
     * @see #getPopupMenu
     */
    public void setPopupMenu(PopupMenu popup) {
        if (popup == this.popup) {
            return;
        }
        synchronized (TrayIcon.class) {
            if (popup != null) {
                if (popup.isTrayIconPopup) {
                    throw new IllegalArgumentException("the PopupMenu is already set for another TrayIcon");
                }
                popup.isTrayIconPopup = true;
            }
            if (this.popup != null) {
                this.popup.isTrayIconPopup = false;
            }
            this.popup = popup;
        }
    }

    /**
     * Returns the popup menu associated with this {@code TrayIcon}.
     *
     * @return the popup menu or {@code null} if none exists
     * @see #setPopupMenu(PopupMenu)
     */
    public PopupMenu getPopupMenu() {
        return popup;
    }

    /**
     * Sets the tooltip string for this {@code TrayIcon}. The
     * tooltip is displayed automatically when the mouse hovers over
     * the icon.  Tooltip may be not visible on some platforms.
     * Setting the tooltip to {@code null} removes any tooltip text.
     *
     * When displayed, the tooltip string may be truncated on some platforms;
     * the number of characters that may be displayed is platform-dependent.
     *
     * @param tooltip the string for the tooltip; if the value is
     * {@code null} no tooltip is shown
     * @see #getToolTip
     */
    public void setToolTip(String tooltip) {
        this.tooltip = tooltip;

        TrayIconPeer peer = this.peer;
        if (peer != null) {
            peer.setToolTip(tooltip);
        }
    }

    /**
     * Returns the tooltip string associated with this
     * {@code TrayIcon}.
     *
     * @return the tooltip string or {@code null} if none exists
     * @see #setToolTip(String)
     */
    public String getToolTip() {
        return tooltip;
    }

    /**
     * Sets the auto-size property.  Auto-size determines whether the
     * tray image is automatically sized to fit the space allocated
     * for the image on the tray.  By default, the auto-size property
     * is set to {@code false}.
     *
     * <p> If auto-size is {@code false}, and the image size
     * doesn't match the tray icon space, the image is painted as-is
     * inside that space &#8212; if larger than the allocated space, it will
     * be cropped.
     *
     * <p> If auto-size is {@code true}, the image is stretched or shrunk to
     * fit the tray icon space.
     *
     * @param autosize {@code true} to auto-size the image,
     * {@code false} otherwise
     * @see #isImageAutoSize
     */
    public void setImageAutoSize(boolean autosize) {
        this.autosize = autosize;

        TrayIconPeer peer = this.peer;
        if (peer != null) {
            peer.updateImage();
        }
    }

    /**
     * Returns the value of the auto-size property.
     *
     * @return {@code true} if the image will be auto-sized,
     * {@code false} otherwise
     * @see #setImageAutoSize(boolean)
     */
    public boolean isImageAutoSize() {
        return autosize;
    }

    /**
     * Adds the specified mouse listener to receive mouse events from
     * this {@code TrayIcon}.  Calling this method with a
     * {@code null} value has no effect.
     *
     * <p><b>Note</b>: The {@code MouseEvent}'s coordinates (received
     * from the {@code TrayIcon}) are relative to the screen, not the
     * {@code TrayIcon}.
     *
     * <p> <b>Note: </b>The {@code MOUSE_ENTERED} and
     * {@code MOUSE_EXITED} mouse events are not supported.
     * <p>Refer to <a href="doc-files/AWTThreadIssues.html#ListenersThreads"
     * >AWT Threading Issues</a> for details on AWT's threading model.
     *
     * @param    listener the mouse listener
     * @see      java.awt.event.MouseEvent
     * @see      java.awt.event.MouseListener
     * @see      #removeMouseListener(MouseListener)
     * @see      #getMouseListeners
     */
    public synchronized void addMouseListener(MouseListener listener) {
        if (listener == null) {
            return;
        }
        mouseListener = AWTEventMulticaster.add(mouseListener, listener);
    }

    /**
     * Removes the specified mouse listener.  Calling this method with
     * {@code null} or an invalid value has no effect.
     * <p>Refer to <a href="doc-files/AWTThreadIssues.html#ListenersThreads"
     * >AWT Threading Issues</a> for details on AWT's threading model.
     *
     * @param    listener   the mouse listener
     * @see      java.awt.event.MouseEvent
     * @see      java.awt.event.MouseListener
     * @see      #addMouseListener(MouseListener)
     * @see      #getMouseListeners
     */
    public synchronized void removeMouseListener(MouseListener listener) {
        if (listener == null) {
            return;
        }
        mouseListener = AWTEventMulticaster.remove(mouseListener, listener);
    }

    /**
     * Returns an array of all the mouse listeners
     * registered on this {@code TrayIcon}.
     *
     * @return all of the {@code MouseListeners} registered on
     * this {@code TrayIcon} or an empty array if no mouse
     * listeners are currently registered
     *
     * @see      #addMouseListener(MouseListener)
     * @see      #removeMouseListener(MouseListener)
     * @see      java.awt.event.MouseListener
     */
    public synchronized MouseListener[] getMouseListeners() {
        return AWTEventMulticaster.getListeners(mouseListener, MouseListener.class);
    }

    /**
     * Adds the specified mouse listener to receive mouse-motion
     * events from this {@code TrayIcon}.  Calling this method
     * with a {@code null} value has no effect.
     *
     * <p><b>Note</b>: The {@code MouseEvent}'s coordinates (received
     * from the {@code TrayIcon}) are relative to the screen, not the
     * {@code TrayIcon}.
     *
     * <p> <b>Note: </b>The {@code MOUSE_DRAGGED} mouse event is not supported.
     * <p>Refer to <a href="doc-files/AWTThreadIssues.html#ListenersThreads"
     * >AWT Threading Issues</a> for details on AWT's threading model.
     *
     * @param    listener   the mouse listener
     * @see      java.awt.event.MouseEvent
     * @see      java.awt.event.MouseMotionListener
     * @see      #removeMouseMotionListener(MouseMotionListener)
     * @see      #getMouseMotionListeners
     */
    public synchronized void addMouseMotionListener(MouseMotionListener listener) {
        if (listener == null) {
            return;
        }
        mouseMotionListener = AWTEventMulticaster.add(mouseMotionListener, listener);
    }

    /**
     * Removes the specified mouse-motion listener.  Calling this method with
     * {@code null} or an invalid value has no effect.
     * <p>Refer to <a href="doc-files/AWTThreadIssues.html#ListenersThreads"
     * >AWT Threading Issues</a> for details on AWT's threading model.
     *
     * @param    listener   the mouse listener
     * @see      java.awt.event.MouseEvent
     * @see      java.awt.event.MouseMotionListener
     * @see      #addMouseMotionListener(MouseMotionListener)
     * @see      #getMouseMotionListeners
     */
    public synchronized void removeMouseMotionListener(MouseMotionListener listener) {
        if (listener == null) {
            return;
        }
        mouseMotionListener = AWTEventMulticaster.remove(mouseMotionListener, listener);
    }

    /**
     * Returns an array of all the mouse-motion listeners
     * registered on this {@code TrayIcon}.
     *
     * @return all of the {@code MouseInputListeners} registered on
     * this {@code TrayIcon} or an empty array if no mouse
     * listeners are currently registered
     *
     * @see      #addMouseMotionListener(MouseMotionListener)
     * @see      #removeMouseMotionListener(MouseMotionListener)
     * @see      java.awt.event.MouseMotionListener
     */
    public synchronized MouseMotionListener[] getMouseMotionListeners() {
        return AWTEventMulticaster.getListeners(mouseMotionListener, MouseMotionListener.class);
    }

    /**
     * Returns the command name of the action event fired by this tray icon.
     *
     * @return the action command name, or {@code null} if none exists
     * @see #addActionListener(ActionListener)
     * @see #setActionCommand(String)
     */
    public String getActionCommand() {
        return actionCommand;
    }

    /**
     * Sets the command name for the action event fired by this tray
     * icon.  By default, this action command is set to
     * {@code null}.
     *
     * @param command  a string used to set the tray icon's
     *                 action command.
     * @see java.awt.event.ActionEvent
     * @see #addActionListener(ActionListener)
     * @see #getActionCommand
     */
    public void setActionCommand(String command) {
        actionCommand = command;
    }

    /**
     * Adds the specified action listener to receive
     * {@code ActionEvent}s from this {@code TrayIcon}.
     * Action events usually occur when a user selects the tray icon,
     * using either the mouse or keyboard.  The conditions in which
     * action events are generated are platform-dependent.
     *
     * <p>Calling this method with a {@code null} value has no
     * effect.
     * <p>Refer to <a href="doc-files/AWTThreadIssues.html#ListenersThreads"
     * >AWT Threading Issues</a> for details on AWT's threading model.
     *
     * @param         listener the action listener
     * @see           #removeActionListener
     * @see           #getActionListeners
     * @see           java.awt.event.ActionListener
     * @see #setActionCommand(String)
     */
    public synchronized void addActionListener(ActionListener listener) {
        if (listener == null) {
            return;
        }
        actionListener = AWTEventMulticaster.add(actionListener, listener);
    }

    /**
     * Removes the specified action listener.  Calling this method with
     * {@code null} or an invalid value has no effect.
     * <p>Refer to <a href="doc-files/AWTThreadIssues.html#ListenersThreads"
     * >AWT Threading Issues</a> for details on AWT's threading model.
     *
     * @param    listener   the action listener
     * @see      java.awt.event.ActionEvent
     * @see      java.awt.event.ActionListener
     * @see      #addActionListener(ActionListener)
     * @see      #getActionListeners
     * @see #setActionCommand(String)
     */
    public synchronized void removeActionListener(ActionListener listener) {
        if (listener == null) {
            return;
        }
        actionListener = AWTEventMulticaster.remove(actionListener, listener);
    }

    /**
     * Returns an array of all the action listeners
     * registered on this {@code TrayIcon}.
     *
     * @return all of the {@code ActionListeners} registered on
     * this {@code TrayIcon} or an empty array if no action
     * listeners are currently registered
     *
     * @see      #addActionListener(ActionListener)
     * @see      #removeActionListener(ActionListener)
     * @see      java.awt.event.ActionListener
     */
    public synchronized ActionListener[] getActionListeners() {
        return AWTEventMulticaster.getListeners(actionListener, ActionListener.class);
    }

    /**
     * The message type determines which icon will be displayed in the
     * caption of the message, and a possible system sound a message
     * may generate upon showing.
     *
     * @see TrayIcon
     * @see TrayIcon#displayMessage(String, String, MessageType)
     * @since 1.6
     */
    public enum MessageType {
        /** An error message */
        ERROR,
        /** A warning message */
        WARNING,
        /** An information message */
        INFO,
        /** Simple message */
        NONE
    };

    /**
     * Displays a popup message near the tray icon.  The message will
     * disappear after a time or if the user clicks on it.  Clicking
     * on the message may trigger an {@code ActionEvent}.
     *
     * <p>Either the caption or the text may be {@code null}, but an
     * {@code NullPointerException} is thrown if both are
     * {@code null}.
     *
     * When displayed, the caption or text strings may be truncated on
     * some platforms; the number of characters that may be displayed is
     * platform-dependent.
     *
     * <p><strong>Note:</strong> Some platforms may not support
     * showing a message.
     *
     * @param caption the caption displayed above the text, usually in
     * bold; may be {@code null}
     * @param text the text displayed for the particular message; may be
     * {@code null}
     * @param messageType an enum indicating the message type
     * @throws NullPointerException if both {@code caption}
     * and {@code text} are {@code null}
     */
    public void displayMessage(String caption, String text, MessageType messageType) {
        if (caption == null && text == null) {
            throw new NullPointerException("displaying the message with both caption and text being null");
        }

        TrayIconPeer peer = this.peer;
        if (peer != null) {
            peer.displayMessage(caption, text, messageType.name());
        }
    }

    /**
     * Returns the size, in pixels, of the space that the tray icon
     * occupies in the system tray.  For the tray icon that is not yet
     * added to the system tray, the returned size is equal to the
     * result of the {@link SystemTray#getTrayIconSize}.
     *
     * @return the size of the tray icon, in pixels
     * @see TrayIcon#setImageAutoSize(boolean)
     * @see java.awt.Image
     * @see TrayIcon#getSize()
     */
    public Dimension getSize() {
        return SystemTray.getSystemTray().getTrayIconSize();
    }

    // ****************************************************************
    // ****************************************************************

    void addNotify()
      throws AWTException
    {
        synchronized (this) {
            if (peer == null) {
                Toolkit toolkit = Toolkit.getDefaultToolkit();
                if (toolkit instanceof SunToolkit) {
                    peer = ((SunToolkit)Toolkit.getDefaultToolkit()).createTrayIcon(this);
                } else if (toolkit instanceof HeadlessToolkit) {
                    peer = ((HeadlessToolkit)Toolkit.getDefaultToolkit()).createTrayIcon(this);
                }
            }
        }
        peer.setToolTip(tooltip);
    }

    void removeNotify() {
        TrayIconPeer p = null;
        synchronized (this) {
            p = peer;
            peer = null;
            if (popup != null) {
                popup.removeNotify();
            }
        }
        if (p != null) {
            p.dispose();
        }
    }

    void setID(int id) {
        this.id = id;
    }

    int getID(){
        return id;
    }

    void dispatchEvent(AWTEvent e) {
        EventQueue.setCurrentEventAndMostRecentTime(e);
        Toolkit.getDefaultToolkit().notifyAWTEventListeners(e);
        processEvent(e);
    }

    void processEvent(AWTEvent e) {
        if (e instanceof MouseEvent) {
            switch(e.getID()) {
            case MouseEvent.MOUSE_PRESSED:
            case MouseEvent.MOUSE_RELEASED:
            case MouseEvent.MOUSE_CLICKED:
                processMouseEvent((MouseEvent)e);
                break;
            case MouseEvent.MOUSE_MOVED:
                processMouseMotionEvent((MouseEvent)e);
                break;
            default:
                return;
            }
        } else if (e instanceof ActionEvent) {
            processActionEvent((ActionEvent)e);
        }
    }

    void processMouseEvent(MouseEvent e) {
        MouseListener listener = mouseListener;

        if (listener != null) {
            int id = e.getID();
            switch(id) {
            case MouseEvent.MOUSE_PRESSED:
                listener.mousePressed(e);
                break;
            case MouseEvent.MOUSE_RELEASED:
                listener.mouseReleased(e);
                break;
            case MouseEvent.MOUSE_CLICKED:
                listener.mouseClicked(e);
                break;
            default:
                return;
            }
        }
    }

    void processMouseMotionEvent(MouseEvent e) {
        MouseMotionListener listener = mouseMotionListener;
        if (listener != null &&
            e.getID() == MouseEvent.MOUSE_MOVED)
        {
            listener.mouseMoved(e);
        }
    }

    void processActionEvent(ActionEvent e) {
        ActionListener listener = actionListener;
        if (listener != null) {
            listener.actionPerformed(e);
        }
    }

    private static native void initIDs();
}
