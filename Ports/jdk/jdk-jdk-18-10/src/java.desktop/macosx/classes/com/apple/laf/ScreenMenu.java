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

package com.apple.laf;

import java.awt.*;
import java.awt.event.*;
import java.util.Hashtable;

import javax.swing.*;

import sun.awt.AWTAccessor;
import sun.awt.SunToolkit;
import sun.lwawt.LWToolkit;
import sun.lwawt.macosx.*;

@SuppressWarnings("serial") // JDK implementation class
final class ScreenMenu extends Menu
        implements ContainerListener, ComponentListener,
                   ScreenMenuPropertyHandler {

    static {
        loadAWTLibrary();
    }

    @SuppressWarnings("removal")
    private static void loadAWTLibrary() {
        java.security.AccessController.doPrivileged(
            new java.security.PrivilegedAction<Void>() {
                public Void run() {
                    System.loadLibrary("awt");
                    return null;
                }
            });
    }

    // screen menu stuff
    private static native long addMenuListeners(ScreenMenu listener, long nativeMenu);
    private static native void removeMenuListeners(long modelPtr);

    private transient long fModelPtr;

    private final Hashtable<Component, MenuItem> fItems;
    private final JMenu fInvoker;

    private Component fLastMouseEventTarget;
    private Rectangle fLastTargetRect;
    private volatile Rectangle[] fItemBounds;

    private ScreenMenuPropertyListener fPropertyListener;

    // Array of child hashes used to see if we need to recreate the Menu.
    private int[] childHashArray;

    ScreenMenu(final JMenu invoker) {
        super(invoker.getText());
        fInvoker = invoker;

        int count = fInvoker.getMenuComponentCount();
        if (count < 5) count = 5;
        fItems = new Hashtable<Component, MenuItem>(count);
        setEnabled(fInvoker.isEnabled());
        updateItems();
    }

    /**
     * Determine if we need to tear down the Menu and re-create it, since the contents may have changed in the Menu opened listener and
     * we do not get notified of it, because EDT is busy in our code. We only need to update if the menu contents have changed in some
     * way, such as the number of menu items, the text of the menuitems, icon, shortcut etc.
     */
    private static boolean needsUpdate(final Component[] items, final int[] childHashArray) {
      if (items == null || childHashArray == null) {
        return true;
      }
      if (childHashArray.length != items.length) {
       return true;
      }
      for (int i = 0; i < items.length; i++) {
          final int hashCode = getHashCode(items[i]);
          if (hashCode != childHashArray[i]) {
            return true;
          }
      }
      return false;
    }

    /**
     * Used to recreate the AWT based Menu structure that implements the Screen Menu.
     * Also computes hashcode and stores them so that we can compare them later in needsUpdate.
     */
    private void updateItems() {
        final int count = fInvoker.getMenuComponentCount();
        final Component[] items = fInvoker.getMenuComponents();
        if (needsUpdate(items, childHashArray)) {
            removeAll();
            fItems.clear();
            if (count <= 0) return;

            childHashArray = new int[count];
            for (int i = 0; i < count; i++) {
                addItem(items[i]);
                childHashArray[i] = getHashCode(items[i]);
            }
        }
    }

    /**
     * Callback from JavaMenuUpdater.m -- called when menu first opens
     */
    public void invokeOpenLater() {
        final JMenu invoker = fInvoker;
        if (invoker == null) {
            System.err.println("invoker is null!");
            return;
        }

        try {
            LWCToolkit.invokeAndWait(new Runnable() {
                public void run() {
                    invoker.setSelected(true);
                    invoker.validate();
                    updateItems();
                    fItemBounds = new Rectangle[invoker.getMenuComponentCount()];
                }
            }, invoker);
        } catch (final Exception e) {
            System.err.println(e);
            e.printStackTrace();
        }
    }

    /**
     * Callback from JavaMenuUpdater.m -- called when menu closes.
     */
    public void invokeMenuClosing() {
        final JMenu invoker = fInvoker;
        if (invoker == null) return;

        try {
            LWCToolkit.invokeAndWait(new Runnable() {
                public void run() {
                    invoker.setSelected(false);
                    // Null out the tracking rectangles and the array.
                    if (fItemBounds != null) {
                        for (int i = 0; i < fItemBounds.length; i++) {
                            fItemBounds[i] = null;
                        }
                    }
                    fItemBounds = null;
                }
            }, invoker);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Callback from JavaMenuUpdater.m -- called when menu item is hilighted.
     *
     * @param inWhichItem The menu item selected by the user. -1 if mouse moves off the menu.
     * @param itemRectTop
     * @param itemRectLeft
     * @param itemRectBottom
     * @param itemRectRight Tracking rectangle coordinates.
     */
    public void handleItemTargeted(final int inWhichItem, final int itemRectTop, final int itemRectLeft, final int itemRectBottom, final int itemRectRight) {
        if (fItemBounds == null || inWhichItem < 0 || inWhichItem > (fItemBounds.length - 1)) return;
        final Rectangle itemRect = new Rectangle(itemRectLeft, itemRectTop, itemRectRight - itemRectLeft, itemRectBottom - itemRectTop);
        fItemBounds[inWhichItem] = itemRect;
    }

    /**
     * Callback from JavaMenuUpdater.m -- called when mouse event happens on the menu.
     */
    public void handleMouseEvent(final int kind, final int x, final int y, final int modifiers, final long when) {
        if (kind == 0) return;
        if (fItemBounds == null) return;

        SunToolkit.executeOnEventHandlerThread(fInvoker, new Runnable() {
            @Override
            public void run() {
                Component target = null;
                Rectangle targetRect = null;
                for (int i = 0; i < fItemBounds.length; i++) {
                    final Rectangle testRect = fItemBounds[i];
                    if (testRect != null) {
                        if (testRect.contains(x, y)) {
                            target = fInvoker.getMenuComponent(i);
                            targetRect = testRect;
                            break;
                        }
                    }
                }
                if (target == null && fLastMouseEventTarget == null) return;

                // Send a mouseExited to the previously hilited item, if it wasn't 0.
                if (target != fLastMouseEventTarget) {
                    if (fLastMouseEventTarget != null) {
                        LWToolkit.postEvent(
                                new MouseEvent(fLastMouseEventTarget,
                                               MouseEvent.MOUSE_EXITED, when,
                                               modifiers, x - fLastTargetRect.x,
                                               y - fLastTargetRect.y, 0,
                                               false));
                    }
                    // Send a mouseEntered to the current hilited item, if it
                    // wasn't 0.
                    if (target != null) {
                        LWToolkit.postEvent(
                                new MouseEvent(target, MouseEvent.MOUSE_ENTERED,
                                               when, modifiers,
                                               x - targetRect.x,
                                               y - targetRect.y, 0, false));
                    }
                    fLastMouseEventTarget = target;
                    fLastTargetRect = targetRect;
                }
                // Post a mouse event to the current item.
                if (target == null) return;
                LWToolkit.postEvent(
                        new MouseEvent(target, kind, when, modifiers,
                                       x - targetRect.x, y - targetRect.y, 0,
                                       false));
            }
        });
    }

    @Override
    public void addNotify() {
        synchronized (getTreeLock()) {
            super.addNotify();
            if (fModelPtr == 0) {
                fInvoker.getPopupMenu().addContainerListener(this);
                fInvoker.addComponentListener(this);
                fPropertyListener = new ScreenMenuPropertyListener(this);
                fInvoker.addPropertyChangeListener(fPropertyListener);

                final Icon icon = fInvoker.getIcon();
                if (icon != null) {
                    setIcon(icon);
                }

                final String tooltipText = fInvoker.getToolTipText();
                if (tooltipText != null) {
                    setToolTipText(tooltipText);
                }
                final Object peer = AWTAccessor.getMenuComponentAccessor()
                                               .getPeer(this);
                if (peer instanceof CMenu) {
                    final CMenu menu = (CMenu) peer;
                    final long nativeMenu = menu.getNativeMenu();
                    fModelPtr = addMenuListeners(this, nativeMenu);
                }
            }
        }
    }

    @Override
    public void removeNotify() {
        synchronized (getTreeLock()) {
            // Call super so that the NSMenu has been removed, before we release
            // the delegate in removeMenuListeners
            super.removeNotify();
            fItems.clear();
            if (fModelPtr != 0) {
                removeMenuListeners(fModelPtr);
                fModelPtr = 0;
                fInvoker.getPopupMenu().removeContainerListener(this);
                fInvoker.removeComponentListener(this);
                fInvoker.removePropertyChangeListener(fPropertyListener);
            }
        }
    }

    /**
     * Invoked when a component has been added to the container.
     */
    @Override
    public void componentAdded(final ContainerEvent e) {
        addItem(e.getChild());
    }

    /**
     * Invoked when a component has been removed from the container.
     */
    @Override
    public void componentRemoved(final ContainerEvent e) {
        final Component child = e.getChild();
        final MenuItem sm = fItems.remove(child);
        if (sm == null) return;

        remove(sm);
    }

    /**
     * Invoked when the component's size changes.
     */
    @Override
    public void componentResized(final ComponentEvent e) {}

    /**
     * Invoked when the component's position changes.
     */
    @Override
    public void componentMoved(final ComponentEvent e) {}

    /**
     * Invoked when the component has been made visible.
     * See componentHidden - we should still have a MenuItem
     * it just isn't inserted
     */
    @Override
    public void componentShown(final ComponentEvent e) {
        setVisible(true);
    }

    /**
     * Invoked when the component has been made invisible.
     * MenuComponent.setVisible does nothing,
     * so we remove the ScreenMenuItem from the ScreenMenu
     * but leave it in fItems
     */
    @Override
    public void componentHidden(final ComponentEvent e) {
        setVisible(false);
    }

    private void setVisible(final boolean b) {
        // Tell our parent to add/remove us
        final MenuContainer parent = getParent();

        if (parent != null) {
            if (parent instanceof ScreenMenu) {
                final ScreenMenu sm = (ScreenMenu)parent;
                sm.setChildVisible(fInvoker, b);
            }
        }
    }

    @Override
    public void setChildVisible(final JMenuItem child, final boolean b) {
        fItems.remove(child);
        updateItems();
    }

    @Override
    public void setAccelerator(final KeyStroke ks) {}

    // only check and radio items can be indeterminate
    @Override
    public void setIndeterminate(boolean indeterminate) { }

    @Override
    public void setToolTipText(final String text) {
        Object peer = AWTAccessor.getMenuComponentAccessor().getPeer(this);
        if (!(peer instanceof CMenuItem)) return;

        final CMenuItem cmi = (CMenuItem)peer;
        cmi.setToolTipText(text);
    }

    @Override
    public void setIcon(final Icon i) {
        Object peer = AWTAccessor.getMenuComponentAccessor().getPeer(this);
        if (!(peer instanceof CMenuItem)) return;

        final CMenuItem cmi = (CMenuItem)peer;
        Image img = null;

        if (i != null) {
            if (i.getIconWidth() > 0 && i.getIconHeight() > 0) {
                img = AquaIcon.getImageForIcon(i);
            }
        }
        cmi.setImage(img);
    }


    /**
     * Gets a hashCode for a JMenu or JMenuItem or subclass so that we can compare for
     * changes in the Menu.
     */
    private static int getHashCode(final Component m) {
        int hashCode = m.hashCode();

        if (m instanceof JMenuItem) {
            final JMenuItem mi = (JMenuItem) m;

            final String text = mi.getText();
            if (text != null) hashCode ^= text.hashCode();

            final Icon icon = mi.getIcon();
            if (icon != null) hashCode ^= icon.hashCode();

            final Icon disabledIcon = mi.getDisabledIcon();
            if (disabledIcon != null) hashCode ^= disabledIcon.hashCode();

            final Action action = mi.getAction();
            if (action != null) hashCode ^= action.hashCode();

            final KeyStroke ks = mi.getAccelerator();
            if (ks != null) hashCode ^= ks.hashCode();

            hashCode ^= Boolean.valueOf(mi.isVisible()).hashCode();
            hashCode ^= Boolean.valueOf(mi.isEnabled()).hashCode();
            hashCode ^= Boolean.valueOf(mi.isSelected()).hashCode();

        } else if (m instanceof JSeparator) {
            hashCode ^= "-".hashCode();
        }

        return hashCode;
    }

    private void addItem(final Component m) {
        if (!m.isVisible()) return;
        MenuItem sm = fItems.get(m);

        if (sm == null) {
            if (m instanceof JMenu) {
                sm = new ScreenMenu((JMenu)m);
            } else if (m instanceof JCheckBoxMenuItem) {
                sm = new ScreenMenuItemCheckbox((JCheckBoxMenuItem)m);
            } else if (m instanceof JRadioButtonMenuItem) {
                sm = new ScreenMenuItemCheckbox((JRadioButtonMenuItem)m);
            } else if (m instanceof JMenuItem) {
                sm = new ScreenMenuItem((JMenuItem)m);
            } else if (m instanceof JPopupMenu.Separator || m instanceof JSeparator) {
                sm = new MenuItem("-"); // This is what java.awt.Menu.addSeparator does
            }

            // Only place the menu item in the hashtable if we just created it.
            if (sm != null) {
                fItems.put(m, sm);
            }
        }

        if (sm != null) {
            add(sm);
        }
    }
}
