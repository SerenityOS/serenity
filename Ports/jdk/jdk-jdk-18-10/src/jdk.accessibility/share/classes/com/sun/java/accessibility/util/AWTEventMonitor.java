/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.java.accessibility.util;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import sun.awt.AWTPermissions;

/**
 * <P>The {@code AWTEventMonitor} implements a suite of listeners that are
 * conditionally installed on every AWT component instance in the Java
 * Virtual Machine.  The events captured by these listeners are made
 * available through a unified set of listeners supported by {@code AWTEventMonitor}.
 * With this, all the individual events on each of the AWT component
 * instances are funneled into one set of listeners broken down by category
 * (see {@link EventID} for the categories).
 * <p>This class depends upon {@link EventQueueMonitor}, which provides the base
 * level support for capturing the top-level containers as they are created.
 */

public class AWTEventMonitor {

    /**
     * Constructs an {@code AWTEventMonitor}.
     */
    public AWTEventMonitor() {}

    static private Component componentWithFocus = null;

    // Low-level listeners
    static private ComponentListener componentListener = null;
    static private ContainerListener containerListener = null;
    static private FocusListener focusListener = null;
    static private KeyListener keyListener = null;
    static private MouseListener mouseListener = null;
    static private MouseMotionListener mouseMotionListener = null;
    static private WindowListener windowListener = null;

    // Semantic listeners
    static private ActionListener actionListener = null;
    static private AdjustmentListener adjustmentListener = null;
    static private ItemListener itemListener = null;
    static private TextListener textListener = null;

    /**
     * The actual listener that is installed on the component instances.
     * This listener calls the other registered listeners when an event
     * occurs.  By doing things this way, the actual number of listeners
     * installed on a component instance is drastically reduced.
     */
    static private final AWTEventsListener awtListener = new AWTEventsListener();

    /**
     * Returns the component that currently has keyboard focus.  The return
     * value can be null.
     *
     * @return the component that has keyboard focus
     */
    static public Component getComponentWithFocus() {
        return componentWithFocus;
    }

    /*
     * Check permissions
     */
    static private void checkInstallPermission() {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(AWTPermissions.ALL_AWT_EVENTS_PERMISSION);
        }
    }

    /**
     * Adds the specified listener to receive all {@link EventID#COMPONENT COMPONENT}
     * events on each component instance in the Java Virtual Machine as they occur.
     * <P>Note: this listener is automatically added to all component
     * instances created after this method is called.  In addition, it
     * is only added to component instances that support this listener type.
     *
     * @param l the listener to add
     * @see #removeComponentListener
     */
    static public void addComponentListener(ComponentListener l) {
        if (componentListener == null) {
            checkInstallPermission();
            awtListener.installListeners(EventID.COMPONENT);
        }
        componentListener = AWTEventMulticaster.add(componentListener, l);
    }

    /**
     * Removes the specified listener so it no longer receives
     * {@link EventID#COMPONENT COMPONENT} events when they occur.
     *
     * @param l the listener to remove
     * @see #addComponentListener
     */
    static public void removeComponentListener(ComponentListener l) {
        componentListener = AWTEventMulticaster.remove(componentListener, l);
        if (componentListener == null) {
            awtListener.removeListeners(EventID.COMPONENT);
        }
    }

    /**
     * Adds the specified listener to receive all {@link EventID#CONTAINER CONTAINER}
     * events on each component instance in the Java Virtual Machine as they occur.
     * <P>Note: this listener is automatically added to all component
     * instances created after this method is called.  In addition, it
     * is only added to component instances that support this listener type.
     *
     * @param l the listener to add
     * @see #removeContainerListener
     */
    static public void addContainerListener(ContainerListener l) {
        containerListener = AWTEventMulticaster.add(containerListener, l);
    }

    /**
     * Removes the specified listener so it no longer receives
     * {@link EventID#CONTAINER CONTAINER} events when they occur.
     *
     * @param l the listener to remove
     * @see #addContainerListener
     */
    static public void removeContainerListener(ContainerListener l) {
        containerListener = AWTEventMulticaster.remove(containerListener, l);
    }

    /**
     * Adds the specified listener to receive all {@link EventID#FOCUS FOCUS} events
     * on each component instance in the Java Virtual Machine when they occur.
     * <P>Note: this listener is automatically added to all component
     * instances created after this method is called.  In addition, it
     * is only added to component instances that support this listener type.
     *
     * @param l the listener to add
     * @see #removeFocusListener
     */
    static public void addFocusListener(FocusListener l) {
        focusListener = AWTEventMulticaster.add(focusListener, l);
    }

    /**
     * Removes the specified listener so it no longer receives {@link EventID#FOCUS FOCUS}
     * events when they occur.
     *
     * @param l the listener to remove
     * @see #addFocusListener
     */
    static public void removeFocusListener(FocusListener l) {
        focusListener = AWTEventMulticaster.remove(focusListener, l);
    }

    /**
     * Adds the specified listener to receive all {@link EventID#KEY KEY} events on each
     * component instance in the Java Virtual Machine when they occur.
     * <P>Note: this listener is automatically added to all component
     * instances created after this method is called.  In addition, it
     * is only added to component instances that support this listener type.
     *
     * @param l the listener to add
     * @see #removeKeyListener
     */
    static public void addKeyListener(KeyListener l) {
        if (keyListener == null) {
            checkInstallPermission();
            awtListener.installListeners(EventID.KEY);
        }
        keyListener = AWTEventMulticaster.add(keyListener, l);
    }

    /**
     * Removes the specified listener so it no longer receives {@link EventID#KEY KEY}
     * events when they occur.
     *
     * @param l the listener to remove
     * @see #addKeyListener
     */
    static public void removeKeyListener(KeyListener l) {
        keyListener = AWTEventMulticaster.remove(keyListener, l);
        if (keyListener == null)  {
            awtListener.removeListeners(EventID.KEY);
        }
    }

    /**
     * Adds the specified listener to receive all {@link EventID#MOUSE MOUSE} events
     * on each component instance in the Java Virtual Machine when they occur.
     * <P>Note: this listener is automatically added to all component
     * instances created after this method is called.  In addition, it
     * is only added to component instances that support this listener type.
     *
     * @param l the listener to add
     * @see #removeMouseListener
     */
    static public void addMouseListener(MouseListener l) {
        if (mouseListener == null) {
            checkInstallPermission();
            awtListener.installListeners(EventID.MOUSE);
        }
        mouseListener = AWTEventMulticaster.add(mouseListener, l);
    }

    /**
     * Removes the specified listener so it no longer receives
     * {@link EventID#MOUSE MOUSE} events when they occur.
     *
     * @param l the listener to remove
     * @see #addMouseListener
     */
    static public void removeMouseListener(MouseListener l) {
        mouseListener = AWTEventMulticaster.remove(mouseListener, l);
        if (mouseListener == null) {
            awtListener.removeListeners(EventID.MOUSE);
        }
    }

    /**
     * Adds the specified listener to receive all mouse {@link EventID#MOTION MOTION}
     * events on each component instance in the Java Virtual Machine when they occur.
     * <P>Note: this listener is automatically added to all component
     * instances created after this method is called.  In addition, it
     * is only added to component instances that support this listener type.
     *
     * @param l the listener to add
     * @see #removeMouseMotionListener
     */
    static public void addMouseMotionListener(MouseMotionListener l) {
        if (mouseMotionListener == null) {
            checkInstallPermission();
            awtListener.installListeners(EventID.MOTION);
        }
        mouseMotionListener = AWTEventMulticaster.add(mouseMotionListener, l);
    }

    /**
     * Removes the specified listener so it no longer receives
     * {@link EventID#MOTION MOTION} events when they occur.
     *
     * @param l the listener to remove
     * @see #addMouseMotionListener
     */
    static public void removeMouseMotionListener(MouseMotionListener l) {
        mouseMotionListener = AWTEventMulticaster.remove(mouseMotionListener, l);
        if (mouseMotionListener == null) {
            awtListener.removeListeners(EventID.MOTION);
        }
    }

    /**
     * Adds the specified listener to receive all {@link EventID#WINDOW WINDOW}
     * events on each component instance in the Java Virtual Machine when they occur.
     * <P>Note: this listener is automatically added to all component
     * instances created after this method is called.  In addition, it
     * is only added to component instances that support this listener type.
     *
     * @param l the listener to add
     * @see #removeWindowListener
     */
    static public void addWindowListener(WindowListener l) {
        if (windowListener == null) {
            checkInstallPermission();
            awtListener.installListeners(EventID.WINDOW);
        }
        windowListener = AWTEventMulticaster.add(windowListener, l);
    }

    /**
     * Removes the specified listener so it no longer receives
     * {@link EventID#WINDOW WINDOW} events when they occur.
     *
     * @param l the listener to remove
     * @see #addWindowListener
     */
    static public void removeWindowListener(WindowListener l) {
        windowListener = AWTEventMulticaster.remove(windowListener, l);
        if (windowListener == null) {
            awtListener.removeListeners(EventID.WINDOW);
        }
    }

    /**
     * Adds the specified listener to receive all {@link EventID#ACTION ACTION}
     * events on each component instance in the Java Virtual Machine when they occur.
     * <P>Note: This listener is automatically added to all component
     * instances created after this method is called.  In addition, it
     * is only added to component instances that support this listener type.
     *
     * @param l the listener to add
     * @see #removeActionListener
     */
    static public void addActionListener(ActionListener l) {
        if (actionListener == null) {
            checkInstallPermission();
            awtListener.installListeners(EventID.ACTION);
        }
        actionListener = AWTEventMulticaster.add(actionListener, l);
    }

    /**
     * Removes the specified listener so it no longer receives
     * {@link EventID#ACTION ACTION} events when they occur.
     *
     * @param l the listener to remove
     * @see #addActionListener
     */
    static public void removeActionListener(ActionListener l) {
        actionListener = AWTEventMulticaster.remove(actionListener, l);
        if (actionListener == null) {
            awtListener.removeListeners(EventID.ACTION);
        }
    }

    /**
     * Adds the specified listener to receive all
     * {@link EventID#ADJUSTMENT ADJUSTMENT} events on each component instance
     * in the Java Virtual Machine when they occur.
     * <P>Note: this listener is automatically added to all component
     * instances created after this method is called.  In addition, it
     * is only added to component instances that support this listener type.
     *
     * @param l the listener to add
     * @see #removeAdjustmentListener
     */
    static public void addAdjustmentListener(AdjustmentListener l) {
        if (adjustmentListener == null) {
            checkInstallPermission();
            awtListener.installListeners(EventID.ADJUSTMENT);
        }
        adjustmentListener = AWTEventMulticaster.add(adjustmentListener, l);
    }

    /**
     * Removes the specified listener so it no longer receives
     * {@link EventID#ADJUSTMENT ADJUSTMENT} events when they occur.
     *
     * @param l the listener to remove
     * @see #addAdjustmentListener
     */
    static public void removeAdjustmentListener(AdjustmentListener l) {
        adjustmentListener = AWTEventMulticaster.remove(adjustmentListener, l);
        if (adjustmentListener == null) {
            awtListener.removeListeners(EventID.ADJUSTMENT);
        }
    }

    /**
     * Adds the specified listener to receive all {@link EventID#ITEM ITEM} events
     * on each component instance in the Java Virtual Machine when they occur.
     * <P>Note: this listener is automatically added to all component
     * instances created after this method is called.  In addition, it
     * is only added to component instances that support this listener type.
     *
     * @param l the listener to add
     * @see #removeItemListener
     */
    static public void addItemListener(ItemListener l) {
        if (itemListener == null) {
            checkInstallPermission();
            awtListener.installListeners(EventID.ITEM);
        }
        itemListener = AWTEventMulticaster.add(itemListener, l);
    }

    /**
     * Removes the specified listener so it no longer receives {@link EventID#ITEM ITEM}
     * events when they occur.
     *
     * @param l the listener to remove
     * @see #addItemListener
     */
    static public void removeItemListener(ItemListener l) {
        itemListener = AWTEventMulticaster.remove(itemListener, l);
        if (itemListener == null) {
            awtListener.removeListeners(EventID.ITEM);
        }
    }

    /**
     * Adds the specified listener to receive all {@link EventID#TEXT TEXT} events
     * on each component instance in the Java Virtual Machine when they occur.
     * <P>Note: this listener is automatically added to all component
     * instances created after this method is called.  In addition, it
     * is only added to component instances that support this listener type.
     *
     * @param l the listener to add
     * @see #removeTextListener
     */
    static public void addTextListener(TextListener l) {
        if (textListener == null) {
            checkInstallPermission();
            awtListener.installListeners(EventID.TEXT);
        }
        textListener = AWTEventMulticaster.add(textListener, l);
    }

    /**
     * Removes the specified listener so it no longer receives {@link EventID#TEXT TEXT}
     * events when they occur.
     *
     * @param l the listener to remove
     * @see #addTextListener
     */
    static public void removeTextListener(TextListener l) {
        textListener = AWTEventMulticaster.remove(textListener, l);
        if (textListener == null) {
            awtListener.removeListeners(EventID.TEXT);
        }
    }


    /**
     * AWTEventsListener is the class that does all the work for AWTEventMonitor.
     * It is not intended for use by any other class except AWTEventMonitor.
     *
     */

    static class AWTEventsListener implements TopLevelWindowListener,
        ActionListener, AdjustmentListener, ComponentListener,
        ContainerListener, FocusListener, ItemListener, KeyListener,
        MouseListener, MouseMotionListener, TextListener, WindowListener,
        ChangeListener {

        /**
         * internal variables for Action introspection
         */
        private java.lang.Class<?>[] actionListeners;
        private java.lang.reflect.Method removeActionMethod;
        private java.lang.reflect.Method addActionMethod;
        private java.lang.Object[] actionArgs;

        /**
         * internal variables for Item introspection
         */
        private java.lang.Class<?>[] itemListeners;
        private java.lang.reflect.Method removeItemMethod;
        private java.lang.reflect.Method addItemMethod;
        private java.lang.Object[] itemArgs;

        /**
         * internal variables for Text introspection
         */
        private java.lang.Class<?>[] textListeners;
        private java.lang.reflect.Method removeTextMethod;
        private java.lang.reflect.Method addTextMethod;
        private java.lang.Object[] textArgs;

        /**
         * internal variables for Window introspection
         */
        private java.lang.Class<?>[] windowListeners;
        private java.lang.reflect.Method removeWindowMethod;
        private java.lang.reflect.Method addWindowMethod;
        private java.lang.Object[] windowArgs;

        /**
         * Create a new instance of this class and install it on each component
         * instance in the virtual machine that supports any of the currently
         * registered listeners in AWTEventMonitor.  Also registers itself
         * as a TopLevelWindowListener with EventQueueMonitor so it can
         * automatically add new listeners to new components.
         *
         * @see EventQueueMonitor
         * @see AWTEventMonitor
         */
        public AWTEventsListener() {
            initializeIntrospection();
            installListeners();
            MenuSelectionManager.defaultManager().addChangeListener(this);
            EventQueueMonitor.addTopLevelWindowListener(this);
        }

        /**
         * Set up all of the variables needed for introspection
         */
        private boolean initializeIntrospection() {
            actionListeners = new java.lang.Class<?>[1];
            actionArgs = new java.lang.Object[1];
            actionListeners[0] = java.awt.event.ActionListener.class;
            actionArgs[0] = this;

            itemListeners = new java.lang.Class<?>[1];
            itemArgs = new java.lang.Object[1];
            itemListeners[0] = java.awt.event.ItemListener.class;
            itemArgs[0] = this;

            textListeners = new java.lang.Class<?>[1];
            textArgs = new java.lang.Object[1];
            textListeners[0] = java.awt.event.TextListener.class;
            textArgs[0] = this;

            windowListeners = new java.lang.Class<?>[1];
            windowArgs = new java.lang.Object[1];
            windowListeners[0] = java.awt.event.WindowListener.class;
            windowArgs[0] = this;

            return true;
        }

        /**
         * Installs all currently registered listeners on all components based
         * upon the current topLevelWindows cached by EventQueueMonitor.
         *
         * @see EventQueueMonitor
         * @see AWTEventMonitor
         */
        protected void installListeners() {
            Window[] topLevelWindows = EventQueueMonitor.getTopLevelWindows();
            if (topLevelWindows != null) {
                for (int i = 0; i < topLevelWindows.length; i++) {
                    installListeners(topLevelWindows[i]);
                }
            }
        }

        /**
         * Installs listeners for the given event ID on all components based
         * upon the current topLevelWindows cached by EventQueueMonitor.
         *
         * @param eventID the event ID
         * @see EventID
         */
        protected void installListeners(int eventID) {
            Window[] topLevelWindows = EventQueueMonitor.getTopLevelWindows();
            if (topLevelWindows != null) {
                for (int i = 0; i < topLevelWindows.length; i++) {
                    installListeners(topLevelWindows[i], eventID);
                }
            }
        }

        /**
         * Installs all currently registered listeners to just the component.
         * @param c the component to add listeners to
         */
        protected void installListeners(Component c) {

            // Container and focus listeners are always installed for our own use.
            //
            installListeners(c,EventID.CONTAINER);
            installListeners(c,EventID.FOCUS);

            // conditionally install low-level listeners
            //
            if (AWTEventMonitor.componentListener != null) {
                installListeners(c,EventID.COMPONENT);
            }
            if (AWTEventMonitor.keyListener != null) {
                installListeners(c,EventID.KEY);
            }
            if (AWTEventMonitor.mouseListener != null) {
                installListeners(c,EventID.MOUSE);
            }
            if (AWTEventMonitor.mouseMotionListener != null) {
                installListeners(c,EventID.MOTION);
            }
            if (AWTEventMonitor.windowListener != null) {
                installListeners(c,EventID.WINDOW);
            }

            // conditionally install Semantic listeners
            //
            if (AWTEventMonitor.actionListener != null) {
                installListeners(c,EventID.ACTION);
            }
            if (AWTEventMonitor.adjustmentListener != null) {
                installListeners(c,EventID.ADJUSTMENT);
            }
            if (AWTEventMonitor.itemListener != null) {
                installListeners(c,EventID.ITEM);
            }
            if (AWTEventMonitor.textListener != null) {
                installListeners(c,EventID.TEXT);
            }
        }

        public void stateChanged(ChangeEvent e) {
            processFocusGained();
        }

        private void processFocusGained() {
            Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            if (focusOwner == null) {
                return;
            }
            MenuSelectionManager.defaultManager().removeChangeListener(this);
            MenuSelectionManager.defaultManager().addChangeListener(this);

            // Only menus and popup selections are handled by the JRootPane.
            if (focusOwner instanceof JRootPane) {
                MenuElement [] path =
                    MenuSelectionManager.defaultManager().getSelectedPath();
                if (path.length > 1) {
                    Component penult = path[path.length-2].getComponent();
                    Component last = path[path.length-1].getComponent();

                    if (last instanceof JPopupMenu ||
                        last instanceof JMenu) {
                        // This is a popup with nothing in the popup
                        // selected. The menu itself is selected.
                        componentWithFocus = last;
                    } else if (penult instanceof JPopupMenu) {
                        // This is a popup with an item selected
                        componentWithFocus = penult;
                    }
                }
            } else {
                // The focus owner has the selection.
                componentWithFocus = focusOwner;
            }
        }

        /**
         * Installs the given listener on the component and any of its children.
         * As a precaution, it always attempts to remove itself as a listener
         * first so it's always guaranteed to have installed itself just once.
         *
         * @param c the component to add listeners to
         * @param eventID the eventID to add listeners for
         * @see EventID
         */
        protected void installListeners(Component c, int eventID) {

            // install the appropriate listener hook into this component
            //
            switch (eventID) {

            case EventID.ACTION:
                try {
                    removeActionMethod = c.getClass().getMethod(
                        "removeActionListener", actionListeners);
                    addActionMethod = c.getClass().getMethod(
                        "addActionListener", actionListeners);
                    try {
                        removeActionMethod.invoke(c, actionArgs);
                        addActionMethod.invoke(c, actionArgs);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        System.out.println("Exception: " + e.toString());
                    } catch (IllegalAccessException e) {
                        System.out.println("Exception: " + e.toString());
                    }
                } catch (NoSuchMethodException e) {
                    // System.out.println("Exception: " + e.toString());
                } catch (SecurityException e) {
                    System.out.println("Exception: " + e.toString());
                }
                break;

            case EventID.ADJUSTMENT:
                if (c instanceof Adjustable) {
                    ((Adjustable) c).removeAdjustmentListener(this);
                    ((Adjustable) c).addAdjustmentListener(this);
                }
                break;

            case EventID.COMPONENT:
                c.removeComponentListener(this);
                c.addComponentListener(this);
                break;

            case EventID.CONTAINER:
                if (c instanceof Container) {
                    ((Container) c).removeContainerListener(this);
                    ((Container) c).addContainerListener(this);
                }
                break;

            case EventID.FOCUS:
                c.removeFocusListener(this);
                c.addFocusListener(this);
                processFocusGained();
                break;

            case EventID.ITEM:
                try {
                    removeItemMethod = c.getClass().getMethod(
                        "removeItemListener", itemListeners);
                    addItemMethod = c.getClass().getMethod(
                        "addItemListener", itemListeners);
                    try {
                        removeItemMethod.invoke(c, itemArgs);
                        addItemMethod.invoke(c, itemArgs);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        System.out.println("Exception: " + e.toString());
                    } catch (IllegalAccessException e) {
                        System.out.println("Exception: " + e.toString());
                    }
                } catch (NoSuchMethodException e) {
                    // System.out.println("Exception: " + e.toString());
                } catch (SecurityException e) {
                    System.out.println("Exception: " + e.toString());
                }
                // [PK] CheckboxMenuItem isn't a component but it does
                // implement Interface ItemSelectable!!
                // if (c instanceof CheckboxMenuItem) {
                //     ((CheckboxMenuItem) c).removeItemListener(this);
                //     ((CheckboxMenuItem) c).addItemListener(this);
                break;

            case EventID.KEY:
                c.removeKeyListener(this);
                c.addKeyListener(this);
                break;

            case EventID.MOUSE:
                c.removeMouseListener(this);
                c.addMouseListener(this);
                break;

            case EventID.MOTION:
                c.removeMouseMotionListener(this);
                c.addMouseMotionListener(this);
                break;

            case EventID.TEXT:
                try {
                    removeTextMethod = c.getClass().getMethod(
                        "removeTextListener", textListeners);
                    addTextMethod = c.getClass().getMethod(
                        "addTextListener", textListeners);
                    try {
                        removeTextMethod.invoke(c, textArgs);
                        addTextMethod.invoke(c, textArgs);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        System.out.println("Exception: " + e.toString());
                    } catch (IllegalAccessException e) {
                        System.out.println("Exception: " + e.toString());
                    }
                } catch (NoSuchMethodException e) {
                    // System.out.println("Exception: " + e.toString());
                } catch (SecurityException e) {
                    System.out.println("Exception: " + e.toString());
                }
                break;

            case EventID.WINDOW:
                try {
                    removeWindowMethod = c.getClass().getMethod(
                        "removeWindowListener", windowListeners);
                    addWindowMethod = c.getClass().getMethod(
                        "addWindowListener", windowListeners);
                    try {
                        removeWindowMethod.invoke(c, windowArgs);
                        addWindowMethod.invoke(c, windowArgs);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        System.out.println("Exception: " + e.toString());
                    } catch (IllegalAccessException e) {
                        System.out.println("Exception: " + e.toString());
                    }
                } catch (NoSuchMethodException e) {
                    // System.out.println("Exception: " + e.toString());
                } catch (SecurityException e) {
                    System.out.println("Exception: " + e.toString());
                }
                break;

            // Don't bother recursing the children if this isn't going to
            // accomplish anything.
            //
            default:
                return;
            }

            // if this component is a container, recurse through children
            //
            if (c instanceof Container) {
                int count = ((Container) c).getComponentCount();
                for (int i = 0; i < count; i++) {
                    installListeners(((Container) c).getComponent(i), eventID);
                }
            }
        }

        /**
         * Removes all listeners for the given event ID on all components based
         * upon the topLevelWindows cached by EventQueueMonitor.
         *
         * @param eventID the event ID
         * @see EventID
         */
        protected void removeListeners(int eventID) {
            Window[] topLevelWindows = EventQueueMonitor.getTopLevelWindows();
            if (topLevelWindows != null) {
                for (int i = 0; i < topLevelWindows.length; i++) {
                    removeListeners(topLevelWindows[i], eventID);
                }
            }
        }

        /**
         * Removes all listeners for the given component and all its children.
         * @param c the component
         */
        protected void removeListeners(Component c) {

            // conditionally remove low-level listeners
            //
            if (AWTEventMonitor.componentListener != null) {
                removeListeners(c,EventID.COMPONENT);
            }
            if (AWTEventMonitor.keyListener != null) {
                removeListeners(c,EventID.KEY);
            }
            if (AWTEventMonitor.mouseListener != null) {
                removeListeners(c,EventID.MOUSE);
            }
            if (AWTEventMonitor.mouseMotionListener != null) {
                removeListeners(c,EventID.MOTION);
            }
            if (AWTEventMonitor.windowListener != null) {
                removeListeners(c,EventID.WINDOW);
            }

            // Remove semantic listeners
            //
            if (AWTEventMonitor.actionListener != null) {
                removeListeners(c,EventID.ACTION);
            }
            if (AWTEventMonitor.adjustmentListener != null) {
                removeListeners(c,EventID.ADJUSTMENT);
            }
            if (AWTEventMonitor.itemListener != null) {
                removeListeners(c,EventID.ITEM);
            }
            if (AWTEventMonitor.textListener != null) {
                removeListeners(c,EventID.TEXT);
            }
        }

        /**
         * Removes all listeners for the event ID from the component and all
         * of its children.
         *
         * @param c the component to remove listeners from
         * @see EventID
         */
        protected void removeListeners(Component c, int eventID) {

            // remove the appropriate listener hook into this component
            //
            switch (eventID) {

            case EventID.ACTION:
                try {
                    removeActionMethod = c.getClass().getMethod(
                        "removeActionListener",
                        actionListeners);
                    try {
                        removeActionMethod.invoke(c, actionArgs);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        System.out.println("Exception: " + e.toString());
                    } catch (IllegalAccessException e) {
                        System.out.println("Exception: " + e.toString());
                    }
                } catch (NoSuchMethodException e) {
                    // System.out.println("Exception: " + e.toString());
                } catch (SecurityException e) {
                    System.out.println("Exception: " + e.toString());
                }
                break;

            case EventID.ADJUSTMENT:
                if (c instanceof Adjustable) {
                    ((Adjustable) c).removeAdjustmentListener(this);
                }
                break;

            case EventID.COMPONENT:
                c.removeComponentListener(this);
                break;

            // Never remove these because we're always interested in them
            // for our own use.
            //case EventID.CONTAINER:
            //    if (c instanceof Container) {
            //        ((Container) c).removeContainerListener(this);
            //    }
            //    break;
            //
            //case EventID.FOCUS:
            //    c.removeFocusListener(this);
            //    break;

            case EventID.ITEM:
                try {
                    removeItemMethod = c.getClass().getMethod(
                        "removeItemListener", itemListeners);
                    try {
                        removeItemMethod.invoke(c, itemArgs);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        System.out.println("Exception: " + e.toString());
                    } catch (IllegalAccessException e) {
                        System.out.println("Exception: " + e.toString());
                    }
                } catch (NoSuchMethodException e) {
                    // System.out.println("Exception: " + e.toString());
                } catch (SecurityException e) {
                    System.out.println("Exception: " + e.toString());
                }
                // [PK] CheckboxMenuItem isn't a component but it does
                // implement Interface ItemSelectable!!
                // if (c instanceof CheckboxMenuItem) {
                //     ((CheckboxMenuItem) c).removeItemListener(this);
                break;

            case EventID.KEY:
                c.removeKeyListener(this);
                break;

            case EventID.MOUSE:
                c.removeMouseListener(this);
                break;

            case EventID.MOTION:
                c.removeMouseMotionListener(this);
                break;

            case EventID.TEXT:
                try {
                    removeTextMethod = c.getClass().getMethod(
                        "removeTextListener", textListeners);
                    try {
                        removeTextMethod.invoke(c, textArgs);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        System.out.println("Exception: " + e.toString());
                    } catch (IllegalAccessException e) {
                        System.out.println("Exception: " + e.toString());
                    }
                } catch (NoSuchMethodException e) {
                    // System.out.println("Exception: " + e.toString());
                } catch (SecurityException e) {
                    System.out.println("Exception: " + e.toString());
                }
                break;

            case EventID.WINDOW:
                try {
                    removeWindowMethod = c.getClass().getMethod(
                        "removeWindowListener", windowListeners);
                    try {
                        removeWindowMethod.invoke(c, windowArgs);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        System.out.println("Exception: " + e.toString());
                    } catch (IllegalAccessException e) {
                        System.out.println("Exception: " + e.toString());
                    }
                } catch (NoSuchMethodException e) {
                    // System.out.println("Exception: " + e.toString());
                } catch (SecurityException e) {
                    System.out.println("Exception: " + e.toString());
                }
                break;

            default:
                return;
            }

            if (c instanceof Container) {
                int count = ((Container) c).getComponentCount();
                for (int i = 0; i < count; i++) {
                    removeListeners(((Container) c).getComponent(i), eventID);
                }
            }
        }

        /********************************************************************/
        /*                                                                  */
        /* Listener Interface Methods                                       */
        /*                                                                  */
        /********************************************************************/

        /* TopLevelWindow Methods ***************************************/

        /**
         * Called when top level window is created.
         *
         * @see EventQueueMonitor
         * @see EventQueueMonitor#addTopLevelWindowListener
         */
        public void topLevelWindowCreated(Window w) {
            installListeners(w);
        }

        /**
         * Called when top level window is destroyed.
         *
         * @see EventQueueMonitor
         * @see EventQueueMonitor#addTopLevelWindowListener
         */
        public void topLevelWindowDestroyed(Window w) {
        }

        /* ActionListener Methods ***************************************/

        /**
         * Called when an action is performed.
         *
         * @see AWTEventMonitor#addActionListener
         */
        public void actionPerformed(ActionEvent e) {
            if (AWTEventMonitor.actionListener != null) {
                AWTEventMonitor.actionListener.actionPerformed(e);
            }
        }

        /* AdjustmentListener Methods ***********************************/

        /**
         * Called when an adjustment is made.
         *
         * @see AWTEventMonitor#addAdjustmentListener
         */
        public void adjustmentValueChanged(AdjustmentEvent e) {
            if (AWTEventMonitor.adjustmentListener != null) {
                AWTEventMonitor.adjustmentListener.adjustmentValueChanged(e);
            }
        }

        /* ComponentListener Methods ************************************/

        /**
         * Called when a component is hidden.
         *
         * @see AWTEventMonitor#addComponentListener
         */
        public void componentHidden(ComponentEvent e) {
            if (AWTEventMonitor.componentListener != null) {
                AWTEventMonitor.componentListener.componentHidden(e);
            }
        }

        /**
         * Called when a component is moved.
         *
         * @see AWTEventMonitor#addComponentListener
         */
        public void componentMoved(ComponentEvent e) {
            if (AWTEventMonitor.componentListener != null) {
                AWTEventMonitor.componentListener.componentMoved(e);
            }
        }

        /**
         * Called when a component is resized.
         *
         * @see AWTEventMonitor#addComponentListener
         */
        public void componentResized(ComponentEvent e) {
            if (AWTEventMonitor.componentListener != null) {
                AWTEventMonitor.componentListener.componentResized(e);
            }
        }

        /**
         * Called when a component is shown.
         *
         * @see AWTEventMonitor#addComponentListener
         */
        public void componentShown(ComponentEvent e) {
            if (AWTEventMonitor.componentListener != null) {
                AWTEventMonitor.componentListener.componentShown(e);
            }
        }

        /* ContainerListener Methods ************************************/

        /**
         * Called when a component is added to a container.
         *
         * @see AWTEventMonitor#addContainerListener
         */
        public void componentAdded(ContainerEvent e) {
            installListeners(e.getChild());
            if (AWTEventMonitor.containerListener != null) {
                AWTEventMonitor.containerListener.componentAdded(e);
            }
        }

        /**
         * Called when a component is removed from a container.
         *
         * @see AWTEventMonitor#addContainerListener
         */
        public void componentRemoved(ContainerEvent e) {
            removeListeners(e.getChild());
            if (AWTEventMonitor.containerListener != null) {
                AWTEventMonitor.containerListener.componentRemoved(e);
            }
        }

        /* FocusListener Methods ****************************************/

        /**
         * Called when a component gains keyboard focus.
         *
         * @see AWTEventMonitor#addFocusListener
         */
        public void focusGained(FocusEvent e) {
            AWTEventMonitor.componentWithFocus = (Component) e.getSource();
            if (AWTEventMonitor.focusListener != null) {
                AWTEventMonitor.focusListener.focusGained(e);
            }
        }

        /**
         * Called when a component loses keyboard focus.
         *
         * @see AWTEventMonitor#addFocusListener
         */
        public void focusLost(FocusEvent e) {
            AWTEventMonitor.componentWithFocus = null;
            if (AWTEventMonitor.focusListener != null) {
                AWTEventMonitor.focusListener.focusLost(e);
            }
        }

        /* ItemListener Methods *****************************************/

        /**
         * Called when an item's state changes.
         *
         * @see AWTEventMonitor#addItemListener
         */
        public void itemStateChanged(ItemEvent e) {
            if (AWTEventMonitor.itemListener != null) {
                AWTEventMonitor.itemListener.itemStateChanged(e);
            }
        }

        /* KeyListener Methods ******************************************/

        /**
         * Called when a key is pressed.
         *
         * @see AWTEventMonitor#addKeyListener
         */
        public void keyPressed(KeyEvent e) {
            if (AWTEventMonitor.keyListener != null) {
                AWTEventMonitor.keyListener.keyPressed(e);
            }
        }

        /**
         * Called when a key is typed.
         *
         * @see AWTEventMonitor#addKeyListener
         */
        public void keyReleased(KeyEvent e) {
            if (AWTEventMonitor.keyListener != null) {
                AWTEventMonitor.keyListener.keyReleased(e);
            }
        }

        /**
         * Called when a key is released.
         *
         * @see AWTEventMonitor#addKeyListener
         */
        public void keyTyped(KeyEvent e) {
            if (AWTEventMonitor.keyListener != null) {
                AWTEventMonitor.keyListener.keyTyped(e);
            }
        }

        /* MouseListener Methods ****************************************/

        /**
         * Called when the mouse is clicked.
         *
         * @see AWTEventMonitor#addMouseListener
         */
        public void mouseClicked(MouseEvent e) {
            if (AWTEventMonitor.mouseListener != null) {
                AWTEventMonitor.mouseListener.mouseClicked(e);
            }
        }

        /**
         * Called when the mouse enters a component.
         *
         * @see AWTEventMonitor#addMouseListener
         */
        public void mouseEntered(MouseEvent e) {
            if (AWTEventMonitor.mouseListener != null) {
                AWTEventMonitor.mouseListener.mouseEntered(e);
            }
        }

        /**
         * Called when the mouse leaves a component.
         *
         * @see AWTEventMonitor#addMouseListener
         */
        public void mouseExited(MouseEvent e) {
            if (AWTEventMonitor.mouseListener != null) {
                AWTEventMonitor.mouseListener.mouseExited(e);
            }
        }

        /**
         * Called when the mouse is pressed.
         *
         * @see AWTEventMonitor#addMouseListener
         */
        public void mousePressed(MouseEvent e) {
            if (AWTEventMonitor.mouseListener != null) {
                AWTEventMonitor.mouseListener.mousePressed(e);
            }
        }

        /**
         * Called when the mouse is released.
         *
         * @see AWTEventMonitor#addMouseListener
         */
        public void mouseReleased(MouseEvent e) {
            if (AWTEventMonitor.mouseListener != null) {
                AWTEventMonitor.mouseListener.mouseReleased(e);
            }
        }

        /* MouseMotionListener Methods **********************************/

        /**
         * Called when the mouse is dragged.
         *
         * @see AWTEventMonitor#addMouseMotionListener
         */
        public void mouseDragged(MouseEvent e) {
            if (AWTEventMonitor.mouseMotionListener != null) {
                AWTEventMonitor.mouseMotionListener.mouseDragged(e);
            }
        }

        /**
         * Called when the mouse is moved.
         *
         * @see AWTEventMonitor#addMouseMotionListener
         */
        public void mouseMoved(MouseEvent e) {
            if (AWTEventMonitor.mouseMotionListener != null) {
                AWTEventMonitor.mouseMotionListener.mouseMoved(e);
            }
        }

        /* TextListener Methods *****************************************/

        /**
         * Called when a component's text value changed.
         *
         * @see AWTEventMonitor#addTextListener
         */
        public void textValueChanged(TextEvent e) {
            if (AWTEventMonitor.textListener != null) {
                AWTEventMonitor.textListener.textValueChanged(e);
            }
        }

        /* WindowListener Methods ***************************************/

        /**
         * Called when a window is opened.
         *
         * @see AWTEventMonitor#addWindowListener
         */
        public void windowOpened(WindowEvent e) {
            if (AWTEventMonitor.windowListener != null) {
                AWTEventMonitor.windowListener.windowOpened(e);
            }
        }

        /**
         * Called when a window is in the process of closing.
         *
         * @see AWTEventMonitor#addWindowListener
         */
        public void windowClosing(WindowEvent e) {
            if (AWTEventMonitor.windowListener != null) {
                AWTEventMonitor.windowListener.windowClosing(e);
            }
        }

        /**
         * Called when a window is closed.
         *
         * @see AWTEventMonitor#addWindowListener
         */
        public void windowClosed(WindowEvent e) {
            if (AWTEventMonitor.windowListener != null) {
                AWTEventMonitor.windowListener.windowClosed(e);
            }
        }

        /**
         * Called when a window is iconified.
         *
         * @see AWTEventMonitor#addWindowListener
         */
        public void windowIconified(WindowEvent e) {
            if (AWTEventMonitor.windowListener != null) {
                AWTEventMonitor.windowListener.windowIconified(e);
            }
        }

        /**
         * Called when a window is deiconified.
         *
         * @see AWTEventMonitor#addWindowListener
         */
        public void windowDeiconified(WindowEvent e) {
            if (AWTEventMonitor.windowListener != null) {
                AWTEventMonitor.windowListener.windowDeiconified(e);
            }
        }

        /**
         * Called when a window is activated.
         *
         * @see AWTEventMonitor#addWindowListener
         */
        public void windowActivated(WindowEvent e) {
            if (AWTEventMonitor.windowListener != null) {
                AWTEventMonitor.windowListener.windowActivated(e);
            }
        }

        /**
         * Called when a window is deactivated.
         *
         * @see AWTEventMonitor#addWindowListener
         */
        public void windowDeactivated(WindowEvent e) {
            if (AWTEventMonitor.windowListener != null) {
                AWTEventMonitor.windowListener.windowDeactivated(e);
            }
        }
    }
}
