/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.lwawt.macosx;

import java.awt.AWTEvent;
import java.awt.Button;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.PopupMenu;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.peer.TrayIconPeer;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.swing.Icon;
import javax.swing.UIManager;

import sun.awt.SunToolkit;

import static sun.awt.AWTAccessor.MenuComponentAccessor;
import static sun.awt.AWTAccessor.getMenuComponentAccessor;

public class CTrayIcon extends CFRetainedResource implements TrayIconPeer {
    private TrayIcon target;
    private PopupMenu popup;

    // In order to construct MouseEvent object, we need to specify a
    // Component target. Because TrayIcon isn't Component's subclass,
    // we use this dummy frame instead
    private final Frame dummyFrame;
    IconObserver observer = new IconObserver();

    // A bitmask that indicates what mouse buttons produce MOUSE_CLICKED events
    // on MOUSE_RELEASE. Click events are only generated if there were no drag
    // events between MOUSE_PRESSED and MOUSE_RELEASED for particular button
    private static int mouseClickButtons = 0;

    @SuppressWarnings("removal")
    private static final boolean useTemplateImages = AccessController.doPrivileged((PrivilegedAction<Boolean>)
        () -> Boolean.getBoolean("apple.awt.enableTemplateImages")
    );

    CTrayIcon(TrayIcon target) {
        super(0, true);

        this.target = target;
        this.popup = target.getPopupMenu();
        this.dummyFrame = new Frame();
        setPtr(createModel());

        //if no one else is creating the peer.
        checkAndCreatePopupPeer();
        updateImage();
    }

    private CPopupMenu checkAndCreatePopupPeer() {
        CPopupMenu menuPeer = null;
        if (popup != null) {
            try {
                final MenuComponentAccessor acc = getMenuComponentAccessor();
                menuPeer = acc.getPeer(popup);
                if (menuPeer == null) {
                    popup.addNotify();
                    menuPeer = acc.getPeer(popup);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return menuPeer;
    }

    private long createModel() {
        return nativeCreate();
    }

    private native long nativeCreate();

    //invocation from the AWTTrayIcon.m
    public long getPopupMenuModel() {
        PopupMenu newPopup = target.getPopupMenu();

        if (popup == newPopup) {
            if (popup == null) {
                return 0L;
            }
        } else {
            if (newPopup != null) {
                if (popup != null) {
                    popup.removeNotify();
                    popup = newPopup;
                } else {
                    popup = newPopup;
                }
            } else {
                return 0L;
            }
        }

        // This method is executed on Appkit, so if ptr is not zero means that,
        // it is still not deallocated(even if we call NSApp postRunnableEvent)
        // and sent CFRelease to the native queue
        return checkAndCreatePopupPeer().ptr;
    }

    /**
     * We display tray icon message as a small dialog with OK button.
     * This is lame, but JDK 1.6 does basically the same. There is a new
     * kind of window in Lion, NSPopover, so perhaps it could be used it
     * to implement better looking notifications.
     */
    public void displayMessage(final String caption, final String text,
                               final String messageType) {
        // obtain icon to show along the message
        Icon icon = getIconForMessageType(messageType);
        CImage cimage = null;
        if (icon != null) {
            BufferedImage image = scaleIcon(icon, 0.75);
            cimage = CImage.getCreator().createFromImage(image, null);
        }
        if (cimage != null) {
            cimage.execute(imagePtr -> {
                execute(ptr -> nativeShowNotification(ptr, caption, text,
                                                      imagePtr));
            });
        } else {
            execute(ptr -> nativeShowNotification(ptr, caption, text, 0));
        }
    }

    @Override
    public void dispose() {
        dummyFrame.dispose();

        if (popup != null) {
            popup.removeNotify();
        }

        LWCToolkit.targetDisposedPeer(target, this);
        target = null;

        super.dispose();
    }

    @Override
    public void setToolTip(String tooltip) {
        execute(ptr -> nativeSetToolTip(ptr, tooltip));
    }

    //adds tooltip to the NSStatusBar's NSButton.
    private native void nativeSetToolTip(long trayIconModel, String tooltip);

    @Override
    public void showPopupMenu(int x, int y) {
        //Not used. The popupmenu is shown from the native code.
    }

    @Override
    public void updateImage() {

        Image image = target.getImage();
        if (image != null) {
            updateNativeImage(image);
        }
    }

    void updateNativeImage(Image image) {
        MediaTracker tracker = new MediaTracker(new Button(""));
        tracker.addImage(image, 0);
        try {
            tracker.waitForAll();
        } catch (InterruptedException ignore) { }

        if (image.getWidth(null) <= 0 ||
            image.getHeight(null) <= 0)
        {
            return;
        }

        CImage cimage = CImage.getCreator().createFromImage(image, observer);
        boolean imageAutoSize = target.isImageAutoSize();
        if (cimage != null) {
            cimage.execute(imagePtr -> {
                execute(ptr -> {
                    setNativeImage(ptr, imagePtr, imageAutoSize, useTemplateImages);
                });
            });
        }
    }

    private native void setNativeImage(final long model, final long nsimage, final boolean autosize, final boolean template);

    private void postEvent(final AWTEvent event) {
        SunToolkit.executeOnEventHandlerThread(target, new Runnable() {
            public void run() {
                SunToolkit.postEvent(SunToolkit.targetToAppContext(target), event);
            }
        });
    }

    //invocation from the AWTTrayIcon.m
    private void handleMouseEvent(NSEvent nsEvent) {
        int buttonNumber = nsEvent.getButtonNumber();
        final SunToolkit tk = (SunToolkit)Toolkit.getDefaultToolkit();
        if ((buttonNumber > 2 && !tk.areExtraMouseButtonsEnabled())
                || buttonNumber > tk.getNumberOfButtons() - 1) {
            return;
        }

        int jeventType = NSEvent.nsToJavaEventType(nsEvent.getType());

        int jbuttonNumber = MouseEvent.NOBUTTON;
        int jclickCount = 0;
        if (jeventType != MouseEvent.MOUSE_MOVED) {
            jbuttonNumber = NSEvent.nsToJavaButton(buttonNumber);
            jclickCount = nsEvent.getClickCount();
        }

        int jmodifiers = NSEvent.nsToJavaModifiers(
                nsEvent.getModifierFlags());
        boolean isPopupTrigger = NSEvent.isPopupTrigger(jmodifiers);

        int eventButtonMask = (jbuttonNumber > 0)?
                MouseEvent.getMaskForButton(jbuttonNumber) : 0;
        long when = System.currentTimeMillis();

        if (jeventType == MouseEvent.MOUSE_PRESSED) {
            mouseClickButtons |= eventButtonMask;
        } else if (jeventType == MouseEvent.MOUSE_DRAGGED) {
            mouseClickButtons = 0;
        }

        // The MouseEvent's coordinates are relative to screen
        int absX = nsEvent.getAbsX();
        int absY = nsEvent.getAbsY();

        MouseEvent mouseEvent = new MouseEvent(dummyFrame, jeventType, when,
                jmodifiers, absX, absY, absX, absY, jclickCount, isPopupTrigger,
                jbuttonNumber);
        mouseEvent.setSource(target);
        postEvent(mouseEvent);

        // fire ACTION event
        if (jeventType == MouseEvent.MOUSE_PRESSED && isPopupTrigger) {
            final String cmd = target.getActionCommand();
            final ActionEvent event = new ActionEvent(target,
                    ActionEvent.ACTION_PERFORMED, cmd);
            postEvent(event);
        }

        // synthesize CLICKED event
        if (jeventType == MouseEvent.MOUSE_RELEASED) {
            if ((mouseClickButtons & eventButtonMask) != 0) {
                MouseEvent clickEvent = new MouseEvent(dummyFrame,
                        MouseEvent.MOUSE_CLICKED, when, jmodifiers, absX, absY,
                        absX, absY, jclickCount, isPopupTrigger, jbuttonNumber);
                clickEvent.setSource(target);
                postEvent(clickEvent);
            }

            mouseClickButtons &= ~eventButtonMask;
        }
    }

    private native void nativeShowNotification(long trayIconModel,
                                               String caption, String text,
                                               long nsimage);

    /**
     * Used by the automated tests.
     */
    private native Point2D nativeGetIconLocation(long trayIconModel);

    /**
     * Scales an icon using specified scale factor
     *
     * @param icon        icon to scale
     * @param scaleFactor scale factor to use
     * @return scaled icon as BuffedredImage
     */
    private static BufferedImage scaleIcon(Icon icon, double scaleFactor) {
        if (icon == null) {
            return null;
        }

        int w = icon.getIconWidth();
        int h = icon.getIconHeight();

        GraphicsEnvironment ge =
                GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        GraphicsConfiguration gc = gd.getDefaultConfiguration();

        // convert icon into image
        BufferedImage iconImage = gc.createCompatibleImage(w, h,
                Transparency.TRANSLUCENT);
        Graphics2D g = iconImage.createGraphics();
        icon.paintIcon(null, g, 0, 0);
        g.dispose();

        // and scale it nicely
        int scaledW = (int) (w * scaleFactor);
        int scaledH = (int) (h * scaleFactor);
        BufferedImage scaledImage = gc.createCompatibleImage(scaledW, scaledH,
                Transparency.TRANSLUCENT);
        g = scaledImage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(iconImage, 0, 0, scaledW, scaledH, null);
        g.dispose();

        return scaledImage;
    }


    /**
     * Gets Aqua icon used in message dialog.
     */
    private static Icon getIconForMessageType(String messageType) {
        if (messageType.equals("ERROR")) {
            return UIManager.getIcon("OptionPane.errorIcon");
        } else if (messageType.equals("WARNING")) {
            return UIManager.getIcon("OptionPane.warningIcon");
        } else {
            // this is just an application icon
            return UIManager.getIcon("OptionPane.informationIcon");
        }
    }

    class IconObserver implements ImageObserver {
        @Override
        public boolean imageUpdate(Image image, int flags, int x, int y, int width, int height) {
            if (target == null || image != target.getImage()) //if the image has been changed
            {
                return false;
            }
            if ((flags & (ImageObserver.FRAMEBITS | ImageObserver.ALLBITS |
                          ImageObserver.WIDTH | ImageObserver.HEIGHT)) != 0)
            {
                SunToolkit.executeOnEventHandlerThread(target, new Runnable() {
                            public void run() {
                                updateNativeImage(image);
                            }
                        });
            }
            return (flags & ImageObserver.ALLBITS) == 0;
        }
    }
}

