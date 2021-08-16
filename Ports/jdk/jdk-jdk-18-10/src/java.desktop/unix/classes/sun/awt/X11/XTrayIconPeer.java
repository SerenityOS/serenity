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

package sun.awt.X11;

import java.awt.*;
import java.awt.event.*;
import java.awt.peer.TrayIconPeer;
import sun.awt.*;
import java.awt.image.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.lang.reflect.InvocationTargetException;
import sun.util.logging.PlatformLogger;

public class XTrayIconPeer implements TrayIconPeer,
       InfoWindow.Balloon.LiveArguments,
       InfoWindow.Tooltip.LiveArguments
{
    private static final PlatformLogger ctrLog = PlatformLogger.getLogger("sun.awt.X11.XTrayIconPeer.centering");

    TrayIcon target;
    TrayIconEventProxy eventProxy;
    XTrayIconEmbeddedFrame eframe;
    TrayIconCanvas canvas;
    InfoWindow.Balloon balloon;
    InfoWindow.Tooltip tooltip;
    PopupMenu popup;
    String tooltipString;
    boolean isTrayIconDisplayed;
    long eframeParentID;
    final XEventDispatcher parentXED, eframeXED;

    static final XEventDispatcher dummyXED = new XEventDispatcher() {
            public void dispatchEvent(XEvent ev) {}
        };

    volatile boolean isDisposed;

    boolean isParentWindowLocated;
    int old_x, old_y;
    int ex_width, ex_height;

    static final int TRAY_ICON_WIDTH = 24;
    static final int TRAY_ICON_HEIGHT = 24;

    @SuppressWarnings("removal")
    XTrayIconPeer(TrayIcon target)
      throws AWTException
    {
        this.target = target;

        eventProxy = new TrayIconEventProxy(this);

        canvas = new TrayIconCanvas(target, TRAY_ICON_WIDTH, TRAY_ICON_HEIGHT);

        eframe = new XTrayIconEmbeddedFrame();

        eframe.setSize(TRAY_ICON_WIDTH, TRAY_ICON_HEIGHT);
        eframe.add(canvas);

        // Fix for 6317038: as EmbeddedFrame is instance of Frame, it is blocked
        // by modal dialogs, but in the case of TrayIcon it shouldn't. So we
        // set ModalExclusion property on it.
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                eframe.setModalExclusionType(Dialog.ModalExclusionType.TOOLKIT_EXCLUDE);
                return null;
            }
        });


        if (XWM.getWMID() != XWM.METACITY_WM) {
            parentXED = dummyXED; // We don't like to leave it 'null'.
        } else {
            parentXED = new XEventDispatcher() {
                // It's executed under AWTLock.
                public void dispatchEvent(XEvent ev) {
                    if (isDisposed() || ev.get_type() != XConstants.ConfigureNotify) {
                        return;
                    }

                    XConfigureEvent ce = ev.get_xconfigure();

                    if (ctrLog.isLoggable(PlatformLogger.Level.FINE)) {
                        ctrLog.fine("ConfigureNotify on parent of {0}: {1}x{2}+{3}+{4} (old: {5}+{6})",
                                XTrayIconPeer.this, ce.get_width(), ce.get_height(),
                                ce.get_x(), ce.get_y(), old_x, old_y);
                    }

                    // A workaround for Gnome/Metacity (it doesn't affect the behaviour on KDE).
                    // On Metacity the EmbeddedFrame's parent window bounds are larger
                    // than TrayIcon size required (that is we need a square but a rectangle
                    // is provided by the Panel Notification Area). The parent's background color
                    // differs from the Panel's one. To hide the background we resize parent
                    // window so that it fits the EmbeddedFrame.
                    // However due to resizing the parent window it loses centering in the Panel.
                    // We center it when discovering that some of its side is of size greater
                    // than the fixed value. Centering is being done by "X" (when the parent's width
                    // is greater) and by "Y" (when the parent's height is greater).

                    // Actually we need this workaround until we could detect taskbar color.

                    if (ce.get_height() != TRAY_ICON_HEIGHT && ce.get_width() != TRAY_ICON_WIDTH) {

                        // If both the height and the width differ from the fixed size then WM
                        // must level at least one side to the fixed size. For some reason it may take
                        // a few hops (even after reparenting) and we have to skip the intermediate ones.
                        if (ctrLog.isLoggable(PlatformLogger.Level.FINE)) {
                            ctrLog.fine("ConfigureNotify on parent of {0}. Skipping as intermediate resizing.",
                                    XTrayIconPeer.this);
                        }
                        return;

                    } else if (ce.get_height() > TRAY_ICON_HEIGHT) {

                        if (ctrLog.isLoggable(PlatformLogger.Level.FINE)) {
                            ctrLog.fine("ConfigureNotify on parent of {0}. Centering by \"Y\".",
                                    XTrayIconPeer.this);
                        }

                        XlibWrapper.XMoveResizeWindow(XToolkit.getDisplay(), eframeParentID,
                                                      ce.get_x(),
                                                      ce.get_y()+ce.get_height()/2-TRAY_ICON_HEIGHT/2,
                                                      TRAY_ICON_WIDTH,
                                                      TRAY_ICON_HEIGHT);
                        ex_height = ce.get_height();
                        ex_width = 0;

                    } else if (ce.get_width() > TRAY_ICON_WIDTH) {

                        if (ctrLog.isLoggable(PlatformLogger.Level.FINE)) {
                            ctrLog.fine("ConfigureNotify on parent of {0}. Centering by \"X\".",
                                    XTrayIconPeer.this);
                        }

                        XlibWrapper.XMoveResizeWindow(XToolkit.getDisplay(), eframeParentID,
                                                      ce.get_x()+ce.get_width()/2 - TRAY_ICON_WIDTH/2,
                                                      ce.get_y(),
                                                      TRAY_ICON_WIDTH,
                                                      TRAY_ICON_HEIGHT);
                        ex_width = ce.get_width();
                        ex_height = 0;

                    } else if (isParentWindowLocated && ce.get_x() != old_x && ce.get_y() != old_y) {
                        // If moving by both "X" and "Y".
                        // When some tray icon gets removed from the tray, a Java icon may be repositioned.
                        // In this case the parent window also lose centering. We have to restore it.

                        if (ex_height != 0) {

                            if (ctrLog.isLoggable(PlatformLogger.Level.FINE)) {
                                ctrLog.fine("ConfigureNotify on parent of {0}. Move detected. Centering by \"Y\".",
                                        XTrayIconPeer.this);
                            }

                            XlibWrapper.XMoveWindow(XToolkit.getDisplay(), eframeParentID,
                                                    ce.get_x(),
                                                    ce.get_y() + ex_height/2 - TRAY_ICON_HEIGHT/2);

                        } else if (ex_width != 0) {

                            if (ctrLog.isLoggable(PlatformLogger.Level.FINE)) {
                                ctrLog.fine("ConfigureNotify on parent of {0}. Move detected. Centering by \"X\".",
                                        XTrayIconPeer.this);
                            }

                            XlibWrapper.XMoveWindow(XToolkit.getDisplay(), eframeParentID,
                                                    ce.get_x() + ex_width/2 - TRAY_ICON_WIDTH/2,
                                                    ce.get_y());
                        } else {
                            if (ctrLog.isLoggable(PlatformLogger.Level.FINE)) {
                                ctrLog.fine("ConfigureNotify on parent of {0}. Move detected. Skipping.",
                                        XTrayIconPeer.this);
                            }
                        }
                    }
                    old_x = ce.get_x();
                    old_y = ce.get_y();
                    isParentWindowLocated = true;
                }
            };
        }
        eframeXED = new XEventDispatcher() {
                // It's executed under AWTLock.
                XTrayIconPeer xtiPeer = XTrayIconPeer.this;

                public void dispatchEvent(XEvent ev) {
                    if (isDisposed() || ev.get_type() != XConstants.ReparentNotify) {
                        return;
                    }

                    XReparentEvent re = ev.get_xreparent();
                    eframeParentID = re.get_parent();

                    if (eframeParentID == XToolkit.getDefaultRootWindow()) {

                        if (isTrayIconDisplayed) { // most likely Notification Area was removed
                            SunToolkit.executeOnEventHandlerThread(xtiPeer.target, new Runnable() {
                                    public void run() {
                                        SystemTray.getSystemTray().remove(xtiPeer.target);
                                    }
                                });
                        }
                        return;
                    }

                    if (!isTrayIconDisplayed) {
                        addXED(eframeParentID, parentXED, XConstants.StructureNotifyMask);

                        isTrayIconDisplayed = true;
                        XToolkit.awtLockNotifyAll();
                    }
                }
            };

        addXED(getWindow(), eframeXED, XConstants.StructureNotifyMask);

        XSystemTrayPeer.getPeerInstance().addTrayIcon(this); // throws AWTException

        // Wait till the EmbeddedFrame is reparented
        long start = System.currentTimeMillis();
        final long PERIOD = XToolkit.getTrayIconDisplayTimeout();
        XToolkit.awtLock();
        try {
            while (!isTrayIconDisplayed) {
                try {
                    XToolkit.awtLockWait(PERIOD);
                } catch (InterruptedException e) {
                    break;
                }
                if (System.currentTimeMillis() - start > PERIOD) {
                    break;
                }
            }
        } finally {
            XToolkit.awtUnlock();
        }

        // This is unlikely to happen.
        if (!isTrayIconDisplayed || eframeParentID == 0 ||
            eframeParentID == XToolkit.getDefaultRootWindow())
        {
            throw new AWTException("TrayIcon couldn't be displayed.");
        }

        eframe.setVisible(true);
        updateImage();

        balloon = new InfoWindow.Balloon(eframe, target, this);
        tooltip = new InfoWindow.Tooltip(eframe, target, this);

        addListeners();
    }

    public void dispose() {
        if (SunToolkit.isDispatchThreadForAppContext(target)) {
            disposeOnEDT();
        } else {
            try {
                SunToolkit.executeOnEDTAndWait(target, new Runnable() {
                        public void run() {
                            disposeOnEDT();
                        }
                    });
            } catch (InterruptedException ie) {
            } catch (InvocationTargetException ite) {}
        }
    }

    private void disposeOnEDT() {
        // All actions that is to be synchronized with disposal
        // should be executed either under AWTLock, or on EDT.
        // isDisposed value must be checked.
        XToolkit.awtLock();
        isDisposed = true;
        XToolkit.awtUnlock();

        removeXED(getWindow(), eframeXED);
        removeXED(eframeParentID, parentXED);
        removeListeners();
        eframe.realDispose();
        balloon.dispose();
        tooltip.dispose();
        isTrayIconDisplayed = false;
        canvas.dispose();
        canvas = null;
        popup = null;
        balloon = null;
        tooltip = null;
        XToolkit.targetDisposedPeer(target, this);
        target = null;
        eframe = null;
    }

    public static void suppressWarningString(Window w) {
        AWTAccessor.getWindowAccessor().setTrayIconWindow(w, true);
    }

    public void setToolTip(String tooltip) {
        tooltipString = tooltip;
    }

    public String getTooltipString() {
        return tooltipString;
    }

    public void updateImage() {
        Runnable r = new Runnable() {
                public void run() {
                    canvas.updateImage(target.getImage());
                }
            };

        if (!SunToolkit.isDispatchThreadForAppContext(target)) {
            SunToolkit.executeOnEventHandlerThread(target, r);
        } else {
            r.run();
        }
    }

    public void displayMessage(String caption, String text, String messageType) {
        Point loc = getLocationOnScreen();
        Rectangle screen = eframe.getGraphicsConfiguration().getBounds();

        // Check if the tray icon is in the bounds of a screen.
        if (!(loc.x < screen.x || loc.x >= screen.x + screen.width ||
              loc.y < screen.y || loc.y >= screen.y + screen.height))
        {
            balloon.display(caption, text, messageType);
        }
    }

    // It's synchronized with disposal by EDT.
    public void showPopupMenu(int x, int y) {
        if (isDisposed())
            return;

        assert SunToolkit.isDispatchThreadForAppContext(target);

        PopupMenu newPopup = target.getPopupMenu();
        if (popup != newPopup) {
            if (popup != null) {
                eframe.remove(popup);
            }
            if (newPopup != null) {
                eframe.add(newPopup);
            }
            popup = newPopup;
        }

        if (popup != null) {
            final XBaseWindow peer = AWTAccessor.getComponentAccessor()
                                                .getPeer(eframe);
            Point loc = peer.toLocal(new Point(x, y));
            popup.show(eframe, loc.x, loc.y);
        }
    }


    // ******************************************************************
    // ******************************************************************


    private void addXED(long window, XEventDispatcher xed, long mask) {
        if (window == 0) {
            return;
        }
        XToolkit.awtLock();
        try {
            XlibWrapper.XSelectInput(XToolkit.getDisplay(), window, mask);
        } finally {
            XToolkit.awtUnlock();
        }
        XToolkit.addEventDispatcher(window, xed);
    }

    private void removeXED(long window, XEventDispatcher xed) {
        if (window == 0) {
            return;
        }
        XToolkit.awtLock();
        try {
            XToolkit.removeEventDispatcher(window, xed);
        } finally {
            XToolkit.awtUnlock();
        }
    }

    // Private method for testing purposes.
    private Point getLocationOnScreen() {
        return eframe.getLocationOnScreen();
    }

    public Rectangle getBounds() {
        Point loc = getLocationOnScreen();
        return new Rectangle(loc.x, loc.y, loc.x + TRAY_ICON_WIDTH, loc.y + TRAY_ICON_HEIGHT);
    }

    void addListeners() {
        canvas.addMouseListener(eventProxy);
        canvas.addMouseMotionListener(eventProxy);
        eframe.addMouseListener(eventProxy);
    }

    void removeListeners() {
        canvas.removeMouseListener(eventProxy);
        canvas.removeMouseMotionListener(eventProxy);
        eframe.removeMouseListener(eventProxy);
    }

    long getWindow() {
        return AWTAccessor.getComponentAccessor()
                          .<XEmbeddedFramePeer>getPeer(eframe).getWindow();
    }

    public boolean isDisposed() {
        return isDisposed;
    }

    public String getActionCommand() {
        return target.getActionCommand();
    }

    static class TrayIconEventProxy implements MouseListener, MouseMotionListener {
        XTrayIconPeer xtiPeer;

        TrayIconEventProxy(XTrayIconPeer xtiPeer) {
            this.xtiPeer = xtiPeer;
        }

        public void handleEvent(MouseEvent e) {
            //prevent DRAG events from being posted with TrayIcon source(CR 6565779)
            if (e.getID() == MouseEvent.MOUSE_DRAGGED) {
                return;
            }

            // Event handling is synchronized with disposal by EDT.
            if (xtiPeer.isDisposed()) {
                return;
            }
            Point coord = XBaseWindow.toOtherWindow(xtiPeer.getWindow(),
                                                    XToolkit.getDefaultRootWindow(),
                                                    e.getX(), e.getY());

            if (e.isPopupTrigger()) {
                xtiPeer.showPopupMenu(coord.x, coord.y);
            }

            e.translatePoint(coord.x - e.getX(), coord.y - e.getY());
            // This is a hack in order to set non-Component source to MouseEvent
            // instance.
            // In some cases this could lead to unpredictable result (e.g. when
            // other class tries to cast source field to Component).
            // We already filter DRAG events out (CR 6565779).
            e.setSource(xtiPeer.target);
            XToolkit.postEvent(XToolkit.targetToAppContext(e.getSource()), e);
        }
        @SuppressWarnings("deprecation")
        public void mouseClicked(MouseEvent e) {
            if ((e.getClickCount() == 1 || xtiPeer.balloon.isVisible()) &&
                e.getButton() == MouseEvent.BUTTON1)
            {
                ActionEvent aev = new ActionEvent(xtiPeer.target, ActionEvent.ACTION_PERFORMED,
                                                  xtiPeer.target.getActionCommand(), e.getWhen(),
                                                  e.getModifiers());
                XToolkit.postEvent(XToolkit.targetToAppContext(aev.getSource()), aev);
            }
            if (xtiPeer.balloon.isVisible()) {
                xtiPeer.balloon.hide();
            }
            handleEvent(e);
        }
        public void mouseEntered(MouseEvent e) {
            xtiPeer.tooltip.enter();
            handleEvent(e);
        }
        public void mouseExited(MouseEvent e) {
            xtiPeer.tooltip.exit();
            handleEvent(e);
        }
        public void mousePressed(MouseEvent e) {
            handleEvent(e);
        }
        public void mouseReleased(MouseEvent e) {
            handleEvent(e);
        }
        public void mouseDragged(MouseEvent e) {
            handleEvent(e);
        }
        public void mouseMoved(MouseEvent e) {
            handleEvent(e);
        }
    }

    // ***************************************
    // Special embedded frame for tray icon
    // ***************************************

    @SuppressWarnings("serial") // JDK-implementation class
    private static class XTrayIconEmbeddedFrame extends XEmbeddedFrame {
        public XTrayIconEmbeddedFrame(){
            super(XToolkit.getDefaultRootWindow(), true, true);
        }

        public boolean isUndecorated() {
            return true;
        }

        public boolean isResizable() {
            return false;
        }

        // embedded frame for tray icon shouldn't be disposed by anyone except tray icon
        public void dispose(){
        }

        public void realDispose(){
            super.dispose();
        }
    };

    // ***************************************
    // Classes for painting an image on canvas
    // ***************************************

    @SuppressWarnings("serial") // JDK-implementation class
    static class TrayIconCanvas extends IconCanvas {
        TrayIcon target;
        boolean autosize;

        TrayIconCanvas(TrayIcon target, int width, int height) {
            super(width, height);
            this.target = target;
        }

        // Invoke on EDT.
        protected void repaintImage(boolean doClear) {
            boolean old_autosize = autosize;
            autosize = target.isImageAutoSize();

            curW = autosize ? width : image.getWidth(observer);
            curH = autosize ? height : image.getHeight(observer);

            super.repaintImage(doClear || (old_autosize != autosize));
        }

        public void dispose() {
            super.dispose();
            target = null;
        }
    }

    @SuppressWarnings("serial") // JDK-implementation class
    public static class IconCanvas extends Canvas {
        volatile Image image;
        IconObserver observer;
        int width, height;
        int curW, curH;

        IconCanvas(int width, int height) {
            this.width = curW = width;
            this.height = curH = height;
        }

        // Invoke on EDT.
        public void updateImage(Image image) {
            this.image = image;
            if (observer == null) {
                observer = new IconObserver();
            }
            repaintImage(true);
        }

        public void dispose() {
            observer = null;
        }

        // Invoke on EDT.
        protected void repaintImage(boolean doClear) {
            Graphics g = getGraphics();
            if (g != null) {
                try {
                    if (isVisible()) {
                        if (doClear) {
                            update(g);
                        } else {
                            paint(g);
                        }
                    }
                } finally {
                    g.dispose();
                }
            }
        }

        // Invoke on EDT.
        public void paint(Graphics g) {
            if (g != null && curW > 0 && curH > 0) {
                BufferedImage bufImage = new BufferedImage(curW, curH, BufferedImage.TYPE_INT_ARGB);
                Graphics2D gr = bufImage.createGraphics();
                if (gr != null) {
                    try {
                        gr.setColor(getBackground());
                        gr.fillRect(0, 0, curW, curH);
                        gr.drawImage(image, 0, 0, curW, curH, observer);
                        gr.dispose();

                        g.drawImage(bufImage, 0, 0, curW, curH, null);
                    } finally {
                        gr.dispose();
                    }
                }
            }
        }

        class IconObserver implements ImageObserver {
            public boolean imageUpdate(final Image image, final int flags, int x, int y, int width, int height) {
                if (image != IconCanvas.this.image || // if the image has been changed
                    !IconCanvas.this.isVisible())
                {
                    return false;
                }
                if ((flags & (ImageObserver.FRAMEBITS | ImageObserver.ALLBITS |
                              ImageObserver.WIDTH | ImageObserver.HEIGHT)) != 0)
                {
                    SunToolkit.executeOnEventHandlerThread(IconCanvas.this, new Runnable() {
                            public void run() {
                                repaintImage(false);
                            }
                        });
                }
                return (flags & ImageObserver.ALLBITS) == 0;
            }
        }
    }
}
