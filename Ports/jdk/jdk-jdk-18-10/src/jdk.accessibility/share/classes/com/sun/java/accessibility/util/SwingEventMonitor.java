/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;
import java.beans.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.tree.*;
import javax.swing.text.*;
import javax.swing.undo.*;
import javax.accessibility.*;


/**
 * <P>{@code SwingEventMonitor} extends {@link AWTEventMonitor} by adding a suite of
 * listeners conditionally installed on every Swing component instance
 * in the Java Virtual Machine.  The events captured by these listeners
 * are made available through a unified set of listeners supported by
 * {@code SwingEventMonitor}.  With this, all the individual events on each of the
 * AWT and Swing component instances are funneled into one set of listeners
 * broken down by category (see {@link EventID} for the categories).
 * <p>This class depends upon {@link EventQueueMonitor}, which provides the base
 * level support for capturing the top-level containers as they are created.
 * <p>Because this class extends {@code AWTEventMonitor}, it is not
 * necessary to use this class and {@code AWTEventMonitor} at the same time.
 * If you want to monitor both AWT and Swing components, you should
 * use just this class.
 *
 * @see AWTEventMonitor
 *
 */
public class SwingEventMonitor extends AWTEventMonitor {

    /**
     * The master list of all listeners registered by other classes.
     * This can only be publicly modified by calling the add or
     * remove listener methods in this class.
     */
    static protected final EventListenerList listenerList = new EventListenerList();

    /**
     * The actual listener that is installed on the component instances.
     * This listener calls the other registered listeners when an event
     * occurs.  By doing things this way, the actual number of listeners
     * installed on a component instance is drastically reduced.
     */
    static private final SwingEventListener swingListener = new SwingEventListener();

    /**
     * Constructs a {@code SwingEventMonitor}.
     */
    public SwingEventMonitor() {}

    /**
     * Adds the specified listener to receive all {@link EventID#ANCESTOR ANCESTOR}
     * events on each component instance in the Java Virtual Machine as they occur.
     * <P>Note: This listener is automatically added to all component
     * instances created after this method is called.  In addition, it
     * is only added to component instances that support this listener type.
     *
     * @param l the listener to add
     * @see #removeAncestorListener
     */
    static public void addAncestorListener(AncestorListener l) {
        if (listenerList.getListenerCount(AncestorListener.class) == 0) {
            swingListener.installListeners(EventID.ANCESTOR);
        }
        listenerList.add(AncestorListener.class, l);
    }

    /**
     * Removes the specified listener so it no longer receives
     * {@link EventID#ANCESTOR ANCESTOR} events when they occur.
     *
     * @param l the listener to remove
     * @see #addAncestorListener
     */
    static public void removeAncestorListener(AncestorListener l) {
        listenerList.remove(AncestorListener.class, l);
        if (listenerList.getListenerCount(AncestorListener.class) == 0) {
            swingListener.removeListeners(EventID.ANCESTOR);
        }
    }

    /**
     * Adds the specified listener to receive all {@link EventID#CARET CARET} events
     * on each component instance in the Java Virtual Machine as they occur.
     * <P>Note: This listener is automatically added to all component
     * instances created after this method is called.  In addition, it
     * is only added to component instances that support this listener type.
     *
     * @param l the listener to add
     * @see #removeCaretListener
     */
    static public void addCaretListener(CaretListener l) {
        if (listenerList.getListenerCount(CaretListener.class) == 0) {
            swingListener.installListeners(EventID.CARET);
        }
        listenerList.add(CaretListener.class, l);
    }

    /**
     * Removes the specified listener so it no longer receives
     * {@link EventID#CARET CARET} events when they occur.
     *
     * @param l the listener to remove
     * @see #addCaretListener
     */
    static public void removeCaretListener(CaretListener l) {
        listenerList.remove(CaretListener.class, l);
        if (listenerList.getListenerCount(CaretListener.class) == 0) {
            swingListener.removeListeners(EventID.CARET);
        }
    }

    /**
     * Adds the specified listener to receive all
     * {@link EventID#CELLEDITOR CELLEDITOR} events on each
     * component instance in the Java Virtual Machine as they occur.
     * <P>Note: This listener is automatically added to all component
     * instances created after this method is called.  In addition, it
     * is only added to component instances that support this listener type.
     *
     * @param l the listener to add
     * @see #removeCellEditorListener
     */
    static public void addCellEditorListener(CellEditorListener l) {
        if (listenerList.getListenerCount(CellEditorListener.class) == 0) {
            swingListener.installListeners(EventID.CELLEDITOR);
        }
        listenerList.add(CellEditorListener.class, l);
    }

    /**
     * Removes the specified listener so it no longer receives
     * {@link EventID#CELLEDITOR CELLEDITOR} events when they occur.
     *
     * @param l the listener to remove
     * @see #addCellEditorListener
     */
    static public void removeCellEditorListener(CellEditorListener l) {
        listenerList.remove(CellEditorListener.class, l);
        if (listenerList.getListenerCount(CellEditorListener.class) == 0) {
            swingListener.removeListeners(EventID.CELLEDITOR);
        }
    }

    /**
     * Adds the specified listener to receive all {@link EventID#CHANGE CHANGE}
     * events on each component instance in the Java Virtual Machine as they occur.
     * <P>Note: This listener is automatically added to all component
     * instances created after this method is called.  In addition, it
     * is only added to component instances that support this listener type.
     *
     * @param l the listener to add
     * @see #removeChangeListener
     */
    static public void addChangeListener(ChangeListener l) {
        if (listenerList.getListenerCount(ChangeListener.class) == 0) {
            swingListener.installListeners(EventID.CHANGE);
        }
        listenerList.add(ChangeListener.class, l);
    }

    /**
     * Removes the specified listener so it no longer receives
     * {@link EventID#CHANGE CHANGE} events when they occur.
     *
     * @param l the listener to remove
     * @see #addChangeListener
     */
    static public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
        if (listenerList.getListenerCount(ChangeListener.class) == 0) {
            swingListener.removeListeners(EventID.CHANGE);
        }
    }

    /**
     * Adds the specified listener to receive all {@link EventID#COLUMNMODEL COLUMNMODEL}
     * events on each component instance in the Java Virtual Machine as they occur.
     * <P>Note: This listener is automatically added to all component
     * instances created after this method is called.  In addition, it
     * is only added to component instances that support this listener type.
     *
     * @param l the listener to add
     * @see #removeColumnModelListener
     */
    static public void addColumnModelListener(TableColumnModelListener l) {
        if (listenerList.getListenerCount(TableColumnModelListener.class) == 0) {
            swingListener.installListeners(EventID.COLUMNMODEL);
        }
        listenerList.add(TableColumnModelListener.class, l);
    }

    /**
     * Removes the specified listener so it no longer receives
     * {@link EventID#COLUMNMODEL COLUMNMODEL} events when they occur.
     *
     * @param l the listener to remove
     * @see #addColumnModelListener
     */
    static public void removeColumnModelListener(TableColumnModelListener l) {
        listenerList.remove(TableColumnModelListener.class, l);
        if (listenerList.getListenerCount(TableColumnModelListener.class) == 0) {
            swingListener.removeListeners(EventID.COLUMNMODEL);
        }
    }

    /**
     * Adds the specified listener to receive all {@link EventID#DOCUMENT DOCUMENT}
     * events on each component instance in the Java Virtual Machine as they occur.
     * <P>Note: This listener is automatically added to all component
     * instances created after this method is called.  In addition, it
     * is only added to component instances that support this listener type.
     *
     * @param l the listener to add
     * @see #removeDocumentListener
     */
    static public void addDocumentListener(DocumentListener l) {
        if (listenerList.getListenerCount(DocumentListener.class) == 0) {
            swingListener.installListeners(EventID.DOCUMENT);
        }
        listenerList.add(DocumentListener.class, l);
    }

    /**
     * Removes the specified listener so it no longer receives
     * {@link EventID#DOCUMENT DOCUMENT} events when they occur.
     *
     * @param l the listener to remove
     * @see #addDocumentListener
     */
    static public void removeDocumentListener(DocumentListener l) {
        listenerList.remove(DocumentListener.class, l);
        if (listenerList.getListenerCount(DocumentListener.class) == 0) {
            swingListener.removeListeners(EventID.DOCUMENT);
        }
    }

    /**
     * Adds the specified listener to receive all {@link EventID#LISTDATA LISTDATA}
     * events on each component instance in the Java Virtual Machine as they occur.
     * <P>Note: This listener is automatically added to all component
     * instances created after this method is called.  In addition, it
     * is only added to component instances that support this listener type.
     *
     * @param l the listener to add
     * @see #removeListDataListener
     */
    static public void addListDataListener(ListDataListener l) {
        if (listenerList.getListenerCount(ListDataListener.class) == 0) {
            swingListener.installListeners(EventID.LISTDATA);
        }
        listenerList.add(ListDataListener.class, l);
    }

    /**
     * Removes the specified listener so it no longer receives
     * {@link EventID#LISTDATA LISTDATA} events when they occur.
     *
     * @param l the listener to remove
     * @see #addListDataListener
     */
    static public void removeListDataListener(ListDataListener l) {
        listenerList.remove(ListDataListener.class, l);
        if (listenerList.getListenerCount(ListDataListener.class) == 0) {
            swingListener.removeListeners(EventID.LISTDATA);
        }
    }

    /**
     * Adds the specified listener to receive all {@link EventID#LISTSELECTION LISTSELECTION}
     * events on each component instance in the Java Virtual Machine as they occur.
     * <P>Note: This listener is automatically added to all component
     * instances created after this method is called.  In addition, it
     * is only added to component instances that support this listener type.
     *
     * @param l the listener to add
     * @see #removeListSelectionListener
     */
    static public void addListSelectionListener(ListSelectionListener l) {
        if (listenerList.getListenerCount(ListSelectionListener.class) == 0) {
            swingListener.installListeners(EventID.LISTSELECTION);
        }
        listenerList.add(ListSelectionListener.class, l);
    }

    /**
     * Removes the specified listener so it no longer receives
     * {@link EventID#LISTSELECTION LISTSELECTION} events when they occur.
     *
     * @param l the listener to remove
     * @see #addListSelectionListener
     */
    static public void removeListSelectionListener(ListSelectionListener l) {
        listenerList.remove(ListSelectionListener.class, l);
        if (listenerList.getListenerCount(ListSelectionListener.class) == 0) {
            swingListener.removeListeners(EventID.LISTSELECTION);
        }
    }

    /**
     * Adds the specified listener to receive all {@link EventID#MENU MENU} events
     * on each component instance in the Java Virtual Machine as they occur.
     * <P>Note: This listener is automatically added to all component
     * instances created after this method is called.  In addition, it
     * is only added to component instances that support this listener type.
     *
     * @param l the listener to add
     * @see #removeMenuListener
     */
    static public void addMenuListener(MenuListener l) {
        if (listenerList.getListenerCount(MenuListener.class) == 0) {
            swingListener.installListeners(EventID.MENU);
        }
        listenerList.add(MenuListener.class, l);
    }

    /**
     * Removes the specified listener so it no longer receives
     * {@link EventID#MENU MENU} events when they occur.
     *
     * @param l the listener to remove
     * @see #addMenuListener
     */
    static public void removeMenuListener(MenuListener l) {
        listenerList.remove(MenuListener.class, l);
        if (listenerList.getListenerCount(MenuListener.class) == 0) {
            swingListener.removeListeners(EventID.MENU);
        }
    }

    /**
     * Adds the specified listener to receive all {@link EventID#POPUPMENU POPUPMENU}
     * events on each component instance in the Java Virtual Machine as they occur.
     * <P>Note: This listener is automatically added to all component
     * instances created after this method is called.  In addition, it
     * is only added to component instances that support this listener type.
     *
     * @param l the listener to add
     * @see #removePopupMenuListener
     */
    static public void addPopupMenuListener(PopupMenuListener l) {
        if (listenerList.getListenerCount(PopupMenuListener.class) == 0) {
            swingListener.installListeners(EventID.POPUPMENU);
        }
        listenerList.add(PopupMenuListener.class, l);
    }

    /**
     * Removes the specified listener so it no longer receives
     * {@link EventID#POPUPMENU POPUPMENU} events when they occur.
     *
     * @param l the listener to remove
     * @see #addPopupMenuListener
     */
    static public void removePopupMenuListener(PopupMenuListener l) {
        listenerList.remove(PopupMenuListener.class, l);
        if (listenerList.getListenerCount(PopupMenuListener.class) == 0) {
            swingListener.removeListeners(EventID.POPUPMENU);
        }
    }

    /**
     * Adds the specified listener to receive all {@link EventID#TABLEMODEL TABLEMODEL}
     * events on each component instance in the Java Virtual Machine as they occur.
     * <P>Note: This listener is automatically added to all component
     * instances created after this method is called.  In addition, it
     * is only added to component instances that support this listener type.
     *
     * @param l the listener to add
     * @see #removeTableModelListener
     */
    static public void addTableModelListener(TableModelListener l) {
        if (listenerList.getListenerCount(TableModelListener.class) == 0) {
            swingListener.installListeners(EventID.TABLEMODEL);
        }
        listenerList.add(TableModelListener.class, l);
    }

    /**
     * Removes the specified listener so it no longer receives
     * {@link EventID#TABLEMODEL TABLEMODEL} events when they occur.
     *
     * @param l the listener to remove
     * @see #addTableModelListener
     */
    static public void removeTableModelListener(TableModelListener l) {
        listenerList.remove(TableModelListener.class, l);
        if (listenerList.getListenerCount(TableModelListener.class) == 0) {
            swingListener.removeListeners(EventID.TABLEMODEL);
        }
    }

    /**
     * Adds the specified listener to receive all {@link EventID#TREEEXPANSION TREEEXPANSION}
     * events on each component instance in the Java Virtual Machine as they occur.
     * <P>Note: This listener is automatically added to all component
     * instances created after this method is called.  In addition, it
     * is only added to component instances that support this listener type.
     *
     * @param l the listener to add
     * @see #removeTreeExpansionListener
     */
    static public void addTreeExpansionListener(TreeExpansionListener l) {
        if (listenerList.getListenerCount(TreeExpansionListener.class) == 0) {
            swingListener.installListeners(EventID.TREEEXPANSION);
        }
        listenerList.add(TreeExpansionListener.class, l);
    }

    /**
     * Removes the specified listener so it no longer receives
     * {@link EventID#TREEEXPANSION TREEEXPANSION} events when they occur.
     *
     * @param l the listener to remove
     * @see #addTreeExpansionListener
     */
    static public void removeTreeExpansionListener(TreeExpansionListener l) {
        listenerList.remove(TreeExpansionListener.class, l);
        if (listenerList.getListenerCount(TreeExpansionListener.class) == 0) {
            swingListener.removeListeners(EventID.TREEEXPANSION);
        }
    }

    /**
     * Adds the specified listener to receive all {@link EventID#TREEMODEL TREEMODEL}
     * events on each component instance in the Java Virtual Machine as they occur.
     * <P>Note: This listener is automatically added to all component
     * instances created after this method is called.  In addition, it
     * is only added to component instances that support this listener type.
     *
     * @param l the listener to add
     * @see #removeTreeModelListener
     */
    static public void addTreeModelListener(TreeModelListener l) {
        if (listenerList.getListenerCount(TreeModelListener.class) == 0) {
            swingListener.installListeners(EventID.TREEMODEL);
        }
        listenerList.add(TreeModelListener.class, l);
    }

    /**
     * Removes the specified listener so it no longer receives
     * {@link EventID#TREEMODEL TREEMODEL} events when they occur.
     *
     * @param l the listener to remove
     * @see #addTreeModelListener
     */
    static public void removeTreeModelListener(TreeModelListener l) {
        listenerList.remove(TreeModelListener.class, l);
        if (listenerList.getListenerCount(TreeModelListener.class) == 0) {
            swingListener.removeListeners(EventID.TREEMODEL);
        }
    }

    /**
     * Adds the specified listener to receive all {@link EventID#TREESELECTION TREESELECTION}
     * events on each component instance in the Java Virtual Machine as they occur.
     * <P>Note: This listener is automatically added to all component
     * instances created after this method is called.  In addition, it
     * is only added to component instances that support this listener type.
     *
     * @param l the listener to add
     * @see #removeTreeSelectionListener
     */
    static public void addTreeSelectionListener(TreeSelectionListener l) {
        if (listenerList.getListenerCount(TreeSelectionListener.class) == 0) {
            swingListener.installListeners(EventID.TREESELECTION);
        }
        listenerList.add(TreeSelectionListener.class, l);
    }

    /**
     * Removes the specified listener so it no longer receives
     * {@link EventID#TREESELECTION TREESELECTION} events when they occur.
     * @see #addTreeSelectionListener
     * @param l the listener to remove
     */
    static public void removeTreeSelectionListener(TreeSelectionListener l) {
        listenerList.remove(TreeSelectionListener.class, l);
        if (listenerList.getListenerCount(TreeSelectionListener.class) == 0) {
            swingListener.removeListeners(EventID.TREESELECTION);
        }
    }

    /**
     * Adds the specified listener to receive all {@link EventID#UNDOABLEEDIT UNDOABLEEDIT}
     * events on each component instance in the Java Virtual Machine as they occur.
     * <P>Note: This listener is automatically added to all component
     * instances created after this method is called.  In addition, it
     * is only added to component instances that support this listener type.
     *
     * @param l the listener to add
     * @see #removeUndoableEditListener
     */
    static public void addUndoableEditListener(UndoableEditListener l) {
        if (listenerList.getListenerCount(UndoableEditListener.class) == 0) {
            swingListener.installListeners(EventID.UNDOABLEEDIT);
        }
        listenerList.add(UndoableEditListener.class, l);
    }

    /**
     * Removes the specified listener so it no longer receives
     * {@link EventID#UNDOABLEEDIT UNDOABLEEDIT} events when they occur.
     *
     * @param l the listener to remove
     * @see #addUndoableEditListener
     */
    static public void removeUndoableEditListener(UndoableEditListener l) {
        listenerList.remove(UndoableEditListener.class, l);
        if (listenerList.getListenerCount(UndoableEditListener.class) == 0) {
            swingListener.removeListeners(EventID.UNDOABLEEDIT);
        }
    }

    /**
     * Adds the specified listener to receive all {@link EventID#INTERNALFRAME INTERNALFRAME}
     * events on each component instance in the Java Virtual Machine as they occur.
     * <P>Note: This listener is automatically added to all component
     * instances created after this method is called.  In addition, it
     * is only added to component instances that support this listener type.
     *
     * @param l the listener to add
     * @see #removeInternalFrameListener
     */
    static public void addInternalFrameListener(InternalFrameListener l) {
        if (listenerList.getListenerCount(InternalFrameListener.class) == 0) {
            swingListener.installListeners(EventID.INTERNALFRAME);
        }
        listenerList.add(InternalFrameListener.class, l);
    }

    /**
     * Removes the specified listener so it no longer receives
     * {@link EventID#INTERNALFRAME INTERNALFRAME} events when they occur.
     *
     * @param l the listener to remove
     * @see #addInternalFrameListener
     */
    static public void removeInternalFrameListener(InternalFrameListener l) {
        listenerList.remove(InternalFrameListener.class, l);
        if (listenerList.getListenerCount(InternalFrameListener.class) == 0) {
            swingListener.removeListeners(EventID.INTERNALFRAME);
        }
    }

    /**
     * Adds the specified listener to receive all {@link EventID#PROPERTYCHANGE PROPERTYCHANGE}
     * events on each component instance in the Java Virtual Machine as they occur.
     * <P>Note: This listener is automatically added to all component
     * instances created after this method is called.  In addition, it
     * is only added to component instances that support this listener type.
     *
     * @param l the listener to add
     * @see #removePropertyChangeListener
     */
    static public void addPropertyChangeListener(PropertyChangeListener l) {
        if (listenerList.getListenerCount(PropertyChangeListener.class) == 0) {
            swingListener.installListeners(EventID.PROPERTYCHANGE);
        }
        listenerList.add(PropertyChangeListener.class, l);
    }

    /**
     * Removes the specified listener so it no longer receives
     * {@link EventID#PROPERTYCHANGE PROPERTYCHANGE} events when they occur.
     * @see #addPropertyChangeListener
     * @param l the listener to remove
     */
    static public void removePropertyChangeListener(PropertyChangeListener l) {
        listenerList.remove(PropertyChangeListener.class, l);
        if (listenerList.getListenerCount(PropertyChangeListener.class) == 0) {
            swingListener.removeListeners(EventID.PROPERTYCHANGE);
        }
    }

    /**
     * Adds the specified listener to receive all {@link EventID#VETOABLECHANGE VETOABLECHANGE}
     * events on each component instance in the Java Virtual Machine as they occur.
     * <P>Note: This listener is automatically added to all component
     * instances created after this method is called.  In addition, it
     * is only added to component instances that support this listener type.
     *
     * @param l the listener to add
     * @see #removeVetoableChangeListener
     */
    static public void addVetoableChangeListener(VetoableChangeListener l) {
        if (listenerList.getListenerCount(VetoableChangeListener.class) == 0) {
            swingListener.installListeners(EventID.VETOABLECHANGE);
        }
        listenerList.add(VetoableChangeListener.class, l);
    }

    /**
     * Removes the specified listener so it no longer receives
     * {@link EventID#VETOABLECHANGE VETOABLECHANGE} events when they occur.
     *
     * @param l the listener to remove
     * @see #addVetoableChangeListener
     */
    static public void removeVetoableChangeListener(VetoableChangeListener l) {
        listenerList.remove(VetoableChangeListener.class, l);
        if (listenerList.getListenerCount(VetoableChangeListener.class) == 0) {
            swingListener.removeListeners(EventID.VETOABLECHANGE);
        }
    }


    /**
     * SwingEventListener is the class that does all the work for
     * SwingEventMonitor.  It is not intended for use by any other class
     * except SwingEventMonitor.
     *
     */
    static class SwingEventListener extends AWTEventsListener
            implements AncestorListener, CaretListener, CellEditorListener,
            ChangeListener, DocumentListener, ListDataListener,
            ListSelectionListener, MenuListener, PopupMenuListener,
            TableColumnModelListener, TableModelListener, TreeExpansionListener,
            TreeModelListener, TreeSelectionListener, UndoableEditListener,
            InternalFrameListener,
            PropertyChangeListener, VetoableChangeListener {

        /**
         * internal variables for Caret introspection
         */
        private java.lang.Class<?>[] caretListeners;
        private java.lang.reflect.Method removeCaretMethod;
        private java.lang.reflect.Method addCaretMethod;
        private java.lang.Object[] caretArgs;

        /**
         * internal variables for CellEditor introspection
         */
        private java.lang.Class<?>[] cellEditorListeners;
        private java.lang.reflect.Method removeCellEditorMethod;
        private java.lang.reflect.Method addCellEditorMethod;
        private java.lang.Object[] cellEditorArgs;
        private java.lang.reflect.Method getCellEditorMethod;

        /**
         * internal variables for Change introspection
         */
        private java.lang.Class<?>[] changeListeners;
        private java.lang.reflect.Method removeChangeMethod;
        private java.lang.reflect.Method addChangeMethod;
        private java.lang.Object[] changeArgs;

        /**
         * internal variable for ColumnModel introspection
         */
        private java.lang.reflect.Method getColumnModelMethod;

        /**
         * internal variables for Document introspection
         */
        private java.lang.Class<?>[] documentListeners;
        private java.lang.reflect.Method removeDocumentMethod;
        private java.lang.reflect.Method addDocumentMethod;
        private java.lang.Object[] documentArgs;
        private java.lang.reflect.Method getDocumentMethod;

        /**
         * internal variable for ListData, Table, and Tree introspection
         */
        private java.lang.reflect.Method getModelMethod;

        /**
         * internal variables for ListSelection introspection
         */
        private java.lang.Class<?>[] listSelectionListeners;
        private java.lang.reflect.Method removeListSelectionMethod;
        private java.lang.reflect.Method addListSelectionMethod;
        private java.lang.Object[] listSelectionArgs;
        private java.lang.reflect.Method getSelectionModelMethod;

        /**
         * internal variables for Menu introspection
         */
        private java.lang.Class<?>[] menuListeners;
        private java.lang.reflect.Method removeMenuMethod;
        private java.lang.reflect.Method addMenuMethod;
        private java.lang.Object[] menuArgs;

        /**
         * internal variables for PopupMenu introspection
         */
        private java.lang.Class<?>[] popupMenuListeners;
        private java.lang.reflect.Method removePopupMenuMethod;
        private java.lang.reflect.Method addPopupMenuMethod;
        private java.lang.Object[] popupMenuArgs;
        private java.lang.reflect.Method getPopupMenuMethod;

        /**
         * internal variables for TreeExpansion introspection
         */
        private java.lang.Class<?>[] treeExpansionListeners;
        private java.lang.reflect.Method removeTreeExpansionMethod;
        private java.lang.reflect.Method addTreeExpansionMethod;
        private java.lang.Object[] treeExpansionArgs;

        /**
         * internal variables for TreeSelection introspection
         */
        private java.lang.Class<?>[] treeSelectionListeners;
        private java.lang.reflect.Method removeTreeSelectionMethod;
        private java.lang.reflect.Method addTreeSelectionMethod;
        private java.lang.Object[] treeSelectionArgs;

        /**
         * internal variables for UndoableEdit introspection
         */
        private java.lang.Class<?>[] undoableEditListeners;
        private java.lang.reflect.Method removeUndoableEditMethod;
        private java.lang.reflect.Method addUndoableEditMethod;
        private java.lang.Object[] undoableEditArgs;

        /**
         * internal variables for InternalFrame introspection
         */
        private java.lang.Class<?>[] internalFrameListeners;
        private java.lang.reflect.Method removeInternalFrameMethod;
        private java.lang.reflect.Method addInternalFrameMethod;
        private java.lang.Object[] internalFrameArgs;

        /**
         * internal variables for PropertyChange introspection
         */
        private java.lang.Class<?>[] propertyChangeListeners;
        private java.lang.reflect.Method removePropertyChangeMethod;
        private java.lang.reflect.Method addPropertyChangeMethod;
        private java.lang.Object[] propertyChangeArgs;

        /**
         * internal variables for a variety of change introspections
         */
        private java.lang.Class<?>[] nullClass;
        private java.lang.Object[] nullArgs;

        /**
         * Create a new instance of this class and install it on each component
         * instance in the virtual machine that supports any of the currently
         * registered listeners in SwingEventMonitor.  Also registers itself
         * as a TopLevelWindowListener with EventQueueMonitor so it can
         * automatically add new listeners to new components.
         * @see EventQueueMonitor
         * @see SwingEventMonitor
         */
        public SwingEventListener() {
            initializeIntrospection();
            installListeners();
            EventQueueMonitor.addTopLevelWindowListener(this);
        }

        /**
         * Set up all of the variables needed for introspection
         */
        private boolean initializeIntrospection() {
            caretListeners = new java.lang.Class<?>[1];
            caretArgs = new java.lang.Object[1];
            caretListeners[0] = javax.swing.event.CaretListener.class;
            caretArgs[0] = this;

            cellEditorListeners = new java.lang.Class<?>[1];
            cellEditorArgs = new java.lang.Object[1];
            cellEditorListeners[0] = javax.swing.event.CellEditorListener.class;
            cellEditorArgs[0] = this;

            changeListeners = new java.lang.Class<?>[1];
            changeArgs = new java.lang.Object[1];
            changeListeners[0] = javax.swing.event.ChangeListener.class;
            changeArgs[0] = this;

            documentListeners = new java.lang.Class<?>[1];
            documentArgs = new java.lang.Object[1];
            documentListeners[0] = javax.swing.event.DocumentListener.class;
            documentArgs[0] = this;

            listSelectionListeners = new java.lang.Class<?>[1];
            listSelectionArgs = new java.lang.Object[1];
            listSelectionListeners[0] = javax.swing.event.ListSelectionListener.class;
            listSelectionArgs[0] = this;

            menuListeners = new java.lang.Class<?>[1];
            menuArgs = new java.lang.Object[1];
            menuListeners[0] = javax.swing.event.MenuListener.class;
            menuArgs[0] = this;

            popupMenuListeners = new java.lang.Class<?>[1];
            popupMenuArgs = new java.lang.Object[1];
            popupMenuListeners[0] = javax.swing.event.PopupMenuListener.class;
            popupMenuArgs[0] = this;

            treeExpansionListeners = new java.lang.Class<?>[1];
            treeExpansionArgs = new java.lang.Object[1];
            treeExpansionListeners[0] = javax.swing.event.TreeExpansionListener.class;
            treeExpansionArgs[0] = this;

            treeSelectionListeners = new java.lang.Class<?>[1];
            treeSelectionArgs = new java.lang.Object[1];
            treeSelectionListeners[0] = javax.swing.event.TreeSelectionListener.class;
            treeSelectionArgs[0] = this;

            undoableEditListeners = new java.lang.Class<?>[1];
            undoableEditArgs = new java.lang.Object[1];
            undoableEditListeners[0] = javax.swing.event.UndoableEditListener.class;
            undoableEditArgs[0] = this;

            internalFrameListeners = new java.lang.Class<?>[1];
            internalFrameArgs = new java.lang.Object[1];
            internalFrameListeners[0] = javax.swing.event.InternalFrameListener.class;
            internalFrameArgs[0] = this;

            nullClass = new java.lang.Class<?>[0];
            nullArgs = new java.lang.Object[0];

            propertyChangeListeners = new java.lang.Class<?>[1];
            propertyChangeArgs = new java.lang.Object[1];
            propertyChangeListeners[0] = java.beans.PropertyChangeListener.class;
            propertyChangeArgs[0] = this;

            return true;
        }

        /**
         * Installs all appropriate Swing listeners to just the component.
         * Also calls super (AWTEventsListener.installListeners()) to install
         * the requested AWT listeners.
         * @param c the component to add listeners to
         */
        protected void installListeners(Component c) {

            // This SwingEventListener needs to be notified when a new
            // Swing component has been added so it can add Swing listeners
            // to these components.  As a result, we always need a Container
            // listener on every Container.
            //
            installListeners(c,EventID.CONTAINER);

            // conditionally install Swing listeners
            //
            if (SwingEventMonitor.listenerList.getListenerCount(AncestorListener.class) > 0) {
                installListeners(c,EventID.ANCESTOR);
            }
            if (SwingEventMonitor.listenerList.getListenerCount(CaretListener.class) > 0) {
                installListeners(c,EventID.CARET);
            }
            if (SwingEventMonitor.listenerList.getListenerCount(CellEditorListener.class) > 0) {
                installListeners(c,EventID.CELLEDITOR);
            }
            if (SwingEventMonitor.listenerList.getListenerCount(ChangeListener.class) > 0) {
                installListeners(c,EventID.CHANGE);
            }
            if (SwingEventMonitor.listenerList.getListenerCount(TableColumnModelListener.class) > 0) {
                installListeners(c,EventID.COLUMNMODEL);
            }
            if (SwingEventMonitor.listenerList.getListenerCount(DocumentListener.class) > 0) {
                installListeners(c,EventID.DOCUMENT);
            }
            if (SwingEventMonitor.listenerList.getListenerCount(ListDataListener.class) > 0) {
                installListeners(c,EventID.LISTDATA);
            }
            if (SwingEventMonitor.listenerList.getListenerCount(ListSelectionListener.class) > 0) {
                installListeners(c,EventID.LISTSELECTION);
            }
            if (SwingEventMonitor.listenerList.getListenerCount(MenuListener.class) > 0) {
                installListeners(c,EventID.MENU);
            }
            if (SwingEventMonitor.listenerList.getListenerCount(PopupMenuListener.class) > 0) {
                installListeners(c,EventID.POPUPMENU);
            }
            if (SwingEventMonitor.listenerList.getListenerCount(TableModelListener.class) > 0) {
                installListeners(c,EventID.TABLEMODEL);
            }
            if (SwingEventMonitor.listenerList.getListenerCount(TreeExpansionListener.class) > 0) {
                installListeners(c,EventID.TREEEXPANSION);
            }
            if (SwingEventMonitor.listenerList.getListenerCount(TreeModelListener.class) > 0) {
                installListeners(c,EventID.TREEMODEL);
            }
            if (SwingEventMonitor.listenerList.getListenerCount(TreeSelectionListener.class) > 0) {
                installListeners(c,EventID.TREESELECTION);
            }
            if (SwingEventMonitor.listenerList.getListenerCount(UndoableEditListener.class) > 0) {
                installListeners(c,EventID.UNDOABLEEDIT);
            }
            if (SwingEventMonitor.listenerList.getListenerCount(InternalFrameListener.class) > 0) {
                installListeners(c,EventID.INTERNALFRAME);
            }

            // Conditionally install Beans listeners
            //
            if (SwingEventMonitor.listenerList.getListenerCount(PropertyChangeListener.class) > 0) {
                installListeners(c,EventID.PROPERTYCHANGE);
            }
            if (SwingEventMonitor.listenerList.getListenerCount(VetoableChangeListener.class) > 0) {
                installListeners(c,EventID.VETOABLECHANGE);
            }

            // Now install the AWT listeners if needed.
            //
            super.installListeners(c);
        }

        /**
         * Installs all appropriate Swing listeners to the component and all its
         * children.  As a precaution, it always attempts to remove itself as
         * a listener first so we're always guaranteed it will installed itself
         * just once.
         * @param c the component to add listeners to
         * @param eventID the eventID to add listeners for
         */
        protected void installListeners(Component c, int eventID) {

            // install the appropriate listener hook into this component
            //
            switch (eventID) {

            case EventID.CONTAINER:
                if (c instanceof Container) {
                    ((Container) c).removeContainerListener(this);
                    ((Container) c).addContainerListener(this);
                }
                break;

            case EventID.ANCESTOR:
                if (c instanceof JComponent) {
                    ((JComponent) c).removeAncestorListener(this);
                    ((JComponent) c).addAncestorListener(this);
                }
                break;

            case EventID.CARET:
                try {
                    removeCaretMethod = c.getClass().getMethod(
                        "removeCaretListener", caretListeners);
                    addCaretMethod = c.getClass().getMethod(
                        "addCaretListener", caretListeners);
                    try {
                        removeCaretMethod.invoke(c, caretArgs);
                        addCaretMethod.invoke(c, caretArgs);
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

            case EventID.CELLEDITOR:
                //  Look for components which support the getCellEditor method
                //  (e.g. JTable, JTree)
                //
                try {
                    getCellEditorMethod = c.getClass().getMethod(
                        "getCellEditorMethod", nullClass);
                    try {
                        Object o = getCellEditorMethod.invoke(c, nullArgs);
                        if (o != null && o instanceof CellEditor) {
                            ((CellEditor) o).removeCellEditorListener(this);
                            ((CellEditor) o).addCellEditorListener(this);
                        }
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

                //  Look for components which support CellEditor listeners
                //  (no current example)
                //
                try {
                    removeCellEditorMethod = c.getClass().getMethod(
                        "removeCellEditorListener", cellEditorListeners);
                    addCellEditorMethod = c.getClass().getMethod(
                        "addCellEditorListener", cellEditorListeners);
                    try {
                        removeCellEditorMethod.invoke(c, cellEditorArgs);
                        addCellEditorMethod.invoke(c, cellEditorArgs);
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

            case EventID.CHANGE:
    //  [[[FIXME:  Need to add support for Style, StyleContext  -pk]]]

                //  Look for components which support Change listeners
                //  (e.g. AbstractButton, Caret, JProgressBar, JSlider,
                //   JTabbedpane, JTextComponent, JViewport)
                //
                try {
                    removeChangeMethod = c.getClass().getMethod(
                        "removeChangeListener", changeListeners);
                    addChangeMethod = c.getClass().getMethod(
                        "addChangeListener", changeListeners);
                    try {
                        removeChangeMethod.invoke(c, changeArgs);
                        addChangeMethod.invoke(c, changeArgs);
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

                //  Look for components which support the getModel method
                //  whose model supports Change listeners
                //  (e.g. BoundedRangeModel, ButtonModel, SingleSelectionModel)
                //
                try {
                    getModelMethod = c.getClass().getMethod(
                        "getModel", nullClass);
                    try {
                        Object o = getModelMethod.invoke(c, nullArgs);
                        if (o != null) {
                            removeChangeMethod = o.getClass().getMethod(
                                "removeChangeListener", changeListeners);
                            addChangeMethod = o.getClass().getMethod(
                                "addChangeListener", changeListeners);
                            removeChangeMethod.invoke(o, changeArgs);
                            addChangeMethod.invoke(o, changeArgs);
                        }
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

            case EventID.COLUMNMODEL:
                try {
                    getColumnModelMethod = c.getClass().getMethod(
                        "getTableColumnModel", nullClass);
                    try {
                        Object o = getColumnModelMethod.invoke(c, nullArgs);
                        if (o != null && o instanceof TableColumnModel) {
                            ((TableColumnModel) o).removeColumnModelListener(this);
                            ((TableColumnModel) o).addColumnModelListener(this);
                        }
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

            case EventID.DOCUMENT:
                //  Look for components which support the getDocument method
                //  (e.g. JTextComponent)
                //
                try {
                    getDocumentMethod = c.getClass().getMethod(
                        "getDocument", nullClass);
                    try {
                        Object o = getDocumentMethod.invoke(c, nullArgs);
                        if (o != null && o instanceof Document) {
                            ((Document) o).removeDocumentListener(this);
                            ((Document) o).addDocumentListener(this);
                        }
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

                //  Look for components which support Document listeners
                //  (no current example)
                //
                try {
                    removeDocumentMethod = c.getClass().getMethod(
                        "removeDocumentListener", documentListeners);
                    addDocumentMethod = c.getClass().getMethod(
                        "addDocumentListener", documentListeners);
                    try {
                        removeDocumentMethod.invoke(c, documentArgs);
                        addDocumentMethod.invoke(c, documentArgs);
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
                //  Add the monitor as a PropertyChangeListener for document
                //  change events from text components.
                //
                if (c instanceof JTextComponent) {
                    try {
                        removePropertyChangeMethod = c.getClass().getMethod(
                            "removePropertyChangeListener",
                            propertyChangeListeners);
                        addPropertyChangeMethod = c.getClass().getMethod(
                            "addPropertyChangeListener",
                            propertyChangeListeners);
                        try {
                            removePropertyChangeMethod.invoke(c,
                                propertyChangeArgs);
                            addPropertyChangeMethod.invoke(c,
                                propertyChangeArgs);
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
                }
                break;

            case EventID.LISTDATA:
            case EventID.TABLEMODEL:
            case EventID.TREEMODEL:
                try {
                    getModelMethod = c.getClass().getMethod(
                        "getModel", nullClass);
                    try {
                        Object o = getModelMethod.invoke(c, nullArgs);
                        if (o != null) {
                            if (eventID == EventID.LISTDATA &&
                                o instanceof ListModel) {
                                ((ListModel) o).removeListDataListener(this);
                                ((ListModel) o).addListDataListener(this);
                            } else if (eventID == EventID.TABLEMODEL &&
                                o instanceof TableModel) {
                                ((TableModel) o).removeTableModelListener(this);
                                ((TableModel) o).addTableModelListener(this);
                            } else if (
                                o instanceof TreeModel) {
                                ((TreeModel) o).removeTreeModelListener(this);
                                ((TreeModel) o).addTreeModelListener(this);
                            }
                        }
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

            case EventID.LISTSELECTION:
                //  Look for components which support ListSelectionListeners
                //  (e.g. JList)
                //
                try {
                    removeListSelectionMethod = c.getClass().getMethod(
                        "removeListSelectionListener", listSelectionListeners);
                    addListSelectionMethod = c.getClass().getMethod(
                        "addListSelectionListener", listSelectionListeners);
                    try {
                        removeListSelectionMethod.invoke(c, listSelectionArgs);
                        addListSelectionMethod.invoke(c, listSelectionArgs);
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

                //  Look for selection models which support ListSelectionListeners
                //  (e.g. JTable's selection model)
                //
                try {
                    getSelectionModelMethod = c.getClass().getMethod(
                        "getSelectionModel", nullClass);
                    try {
                        Object o = getSelectionModelMethod.invoke(c, nullArgs);
                        if (o != null && o instanceof ListSelectionModel) {
                            ((ListSelectionModel) o).removeListSelectionListener(this);
                            ((ListSelectionModel) o).addListSelectionListener(this);
                        }
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

            case EventID.MENU:
                try {
                    removeMenuMethod = c.getClass().getMethod(
                        "removeMenuListener", menuListeners);
                    addMenuMethod = c.getClass().getMethod(
                        "addMenuListener", menuListeners);
                    try {
                        removeMenuMethod.invoke(c, menuArgs);
                        addMenuMethod.invoke(c, menuArgs);
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

            case EventID.POPUPMENU:
                //  Look for components which support PopupMenuListeners
                //  (e.g. JPopupMenu)
                //
                try {
                    removePopupMenuMethod = c.getClass().getMethod(
                        "removePopupMenuListener", popupMenuListeners);
                    addPopupMenuMethod = c.getClass().getMethod(
                        "addPopupMenuListener", popupMenuListeners);
                    try {
                        removePopupMenuMethod.invoke(c, popupMenuArgs);
                        addPopupMenuMethod.invoke(c, popupMenuArgs);
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

                //  Look for components which support getPopupMenu
                //  (e.g. JMenu)
                //
                try {
                    getPopupMenuMethod = c.getClass().getMethod(
                        "getPopupMenu", nullClass);
                    try {
                        Object o = getPopupMenuMethod.invoke(c, nullArgs);
                        if (o != null) {
                            removePopupMenuMethod = o.getClass().getMethod(
                                "removePopupMenuListener", popupMenuListeners);
                            addPopupMenuMethod = o.getClass().getMethod(
                                "addPopupMenuListener", popupMenuListeners);
                            removePopupMenuMethod.invoke(o, popupMenuArgs);
                            addPopupMenuMethod.invoke(o, popupMenuArgs);
                        }
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

            case EventID.TREEEXPANSION:
                try {
                    removeTreeExpansionMethod = c.getClass().getMethod(
                        "removeTreeExpansionListener", treeExpansionListeners);
                    addTreeExpansionMethod = c.getClass().getMethod(
                        "addTreeExpansionListener", treeExpansionListeners);
                    try {
                        removeTreeExpansionMethod.invoke(c, treeExpansionArgs);
                        addTreeExpansionMethod.invoke(c, treeExpansionArgs);
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

            case EventID.TREESELECTION:
                try {
                    removeTreeSelectionMethod = c.getClass().getMethod(
                        "removeTreeSelectionListener", treeSelectionListeners);
                    addTreeSelectionMethod = c.getClass().getMethod(
                        "addTreeSelectionListener", treeSelectionListeners);
                    try {
                        removeTreeSelectionMethod.invoke(c, treeSelectionArgs);
                        addTreeSelectionMethod.invoke(c, treeSelectionArgs);
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

            case EventID.UNDOABLEEDIT:
                //  Look for components which support the getDocument method
                //  (e.g. JTextComponent)
                //
                try {
                    getDocumentMethod = c.getClass().getMethod(
                        "getDocument", nullClass);
                    try {
                        Object o = getDocumentMethod.invoke(c, nullArgs);
                        if (o != null && o instanceof Document) {
                            ((Document) o).removeUndoableEditListener(this);
                            ((Document) o).addUndoableEditListener(this);
                        }
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

                //  Look for components which support UndoableEdit listeners
                //  (no current example)
                //
                try {
                    removeUndoableEditMethod = c.getClass().getMethod(
                        "removeUndoableEditListener", undoableEditListeners);
                    addUndoableEditMethod = c.getClass().getMethod(
                        "addUndoableEditListener", undoableEditListeners);
                    try {
                        removeUndoableEditMethod.invoke(c, undoableEditArgs);
                        addUndoableEditMethod.invoke(c, undoableEditArgs);
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

            case EventID.INTERNALFRAME:
                //  Look for components which support InternalFrame listeners
                //  (e.g. JInternalFrame)
                //
              try {
                    removeInternalFrameMethod = c.getClass().getMethod(
                        "removeInternalFrameListener", internalFrameListeners);
                    addInternalFrameMethod = c.getClass().getMethod(
                        "addInternalFrameListener", internalFrameListeners);
                    try {
                        removeInternalFrameMethod.invoke(c, internalFrameArgs);
                        addInternalFrameMethod.invoke(c, internalFrameArgs);
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

            case EventID.PROPERTYCHANGE:
                //  Look for components which support PropertyChange listeners
                //  (e.g. JComponent)
                //
                try {
                    removePropertyChangeMethod = c.getClass().getMethod(
                        "removePropertyChangeListener", propertyChangeListeners);
                    addPropertyChangeMethod = c.getClass().getMethod(
                        "addPropertyChangeListener", propertyChangeListeners);
                    try {
                        removePropertyChangeMethod.invoke(c, propertyChangeArgs);
                        addPropertyChangeMethod.invoke(c, propertyChangeArgs);
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

                //  Look for components which support the getSelectionModel method
                //  (e.g. JTextComponent)
                //
                try {
                    getSelectionModelMethod = c.getClass().getMethod(
                        "getSelectionModel", nullClass);
                    try {
                        Object o = getSelectionModelMethod.invoke(c, nullArgs);
                        if (o != null && o instanceof TreeSelectionModel) {
                            ((TreeSelectionModel) o).removePropertyChangeListener(this);
                            ((TreeSelectionModel) o).addPropertyChangeListener(this);
                        }
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

            case EventID.VETOABLECHANGE:
                if (c instanceof JComponent) {
                    ((JComponent) c).removeVetoableChangeListener(this);
                    ((JComponent) c).addVetoableChangeListener(this);
                }
                break;

            // Don't bother recursing the children if this isn't going to
            // accomplish anything.
            //
            default:
                return;
            }

            if (c instanceof Container) {
                int count = ((Container) c).getComponentCount();
                for (int i = 0; i < count; i++) {
                    installListeners(((Container) c).getComponent(i), eventID);
                }
            }
        }

        /**
         * Removes all listeners for the given component and all its children.
         * @param c the component
         */
        protected void removeListeners(Component c) {

            // conditionaly remove the Swing listeners
            //
            if (SwingEventMonitor.listenerList.getListenerCount(AncestorListener.class) > 0) {
                removeListeners(c,EventID.ANCESTOR);
            }
            if (SwingEventMonitor.listenerList.getListenerCount(CaretListener.class) > 0) {
                removeListeners(c,EventID.CARET);
            }
            if (SwingEventMonitor.listenerList.getListenerCount(CellEditorListener.class) > 0) {
                removeListeners(c,EventID.CELLEDITOR);
            }
            if (SwingEventMonitor.listenerList.getListenerCount(ChangeListener.class) > 0) {
                removeListeners(c,EventID.CHANGE);
            }
            if (SwingEventMonitor.listenerList.getListenerCount(TableColumnModelListener.class) > 0) {
                removeListeners(c,EventID.COLUMNMODEL);
            }
            if (SwingEventMonitor.listenerList.getListenerCount(DocumentListener.class) > 0) {
                removeListeners(c,EventID.DOCUMENT);
            }
            if (SwingEventMonitor.listenerList.getListenerCount(ListDataListener.class) > 0) {
                removeListeners(c,EventID.LISTDATA);
            }
            if (SwingEventMonitor.listenerList.getListenerCount(ListSelectionListener.class) > 0) {
                removeListeners(c,EventID.LISTSELECTION);
            }
            if (SwingEventMonitor.listenerList.getListenerCount(MenuListener.class) > 0) {
                removeListeners(c,EventID.MENU);
            }
            if (SwingEventMonitor.listenerList.getListenerCount(PopupMenuListener.class) > 0) {
                removeListeners(c,EventID.POPUPMENU);
            }
            if (SwingEventMonitor.listenerList.getListenerCount(TableModelListener.class) > 0) {
                removeListeners(c,EventID.TABLEMODEL);
            }
            if (SwingEventMonitor.listenerList.getListenerCount(TreeExpansionListener.class) > 0) {
                removeListeners(c,EventID.TREEEXPANSION);
            }
            if (SwingEventMonitor.listenerList.getListenerCount(TreeModelListener.class) > 0) {
                removeListeners(c,EventID.TREEMODEL);
            }
            if (SwingEventMonitor.listenerList.getListenerCount(TreeSelectionListener.class) > 0) {
                removeListeners(c,EventID.TREESELECTION);
            }
            if (SwingEventMonitor.listenerList.getListenerCount(UndoableEditListener.class) > 0) {
                removeListeners(c,EventID.UNDOABLEEDIT);
            }
            if (SwingEventMonitor.listenerList.getListenerCount(InternalFrameListener.class) > 0) {
                removeListeners(c,EventID.INTERNALFRAME);
            }

            // conditionaly remove the beans listeners
            //
            if (SwingEventMonitor.listenerList.getListenerCount(PropertyChangeListener.class) > 0) {
                removeListeners(c,EventID.PROPERTYCHANGE);
            }
            if (SwingEventMonitor.listenerList.getListenerCount(VetoableChangeListener.class) > 0) {
                removeListeners(c,EventID.VETOABLECHANGE);
            }

            // Now remove the AWT listeners if needed.
            //
            super.removeListeners(c);
        }

        /**
         * Removes all Swing listeners for the event ID from the component and
         * all of its children.
         * @param c the component to remove listeners from
         */
        protected void removeListeners(Component c, int eventID) {

            // remove the appropriate listener hook into this component
            //
            switch (eventID) {

            case EventID.CONTAINER:
                //Never remove these because we're always interested in them
                // for our own use.
                break;

            case EventID.ANCESTOR:
                if (c instanceof JComponent) {
                    ((JComponent) c).removeAncestorListener(this);
                }
                break;

            case EventID.CARET:
                try {
                    removeCaretMethod = c.getClass().getMethod(
                        "removeCaretListener", caretListeners);
                    try {
                        removeCaretMethod.invoke(c, caretArgs);
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

            case EventID.CELLEDITOR:
                //  Look for components which support the getCellEditor method
                //  (e.g. JTable, JTree)
                //
                try {
                    getCellEditorMethod = c.getClass().getMethod(
                        "getCellEditorMethod", nullClass);
                    try {
                        Object o = getCellEditorMethod.invoke(c, nullArgs);
                        if (o != null && o instanceof CellEditor) {
                            ((CellEditor) o).removeCellEditorListener(this);
                        }
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

                //  Look for components which support CellEditor listeners
                //  (no current example)
                //
                try {
                    removeCellEditorMethod = c.getClass().getMethod(
                        "removeCellEditorListener", cellEditorListeners);
                    try {
                        removeCellEditorMethod.invoke(c, cellEditorArgs);
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

            case EventID.CHANGE:
    //  [[[FIXME:  Need to add support for Style, StyleContext -pk ]]]

                //  Look for components which support Change listeners
                //  (e.g. AbstractButton, Caret, JProgressBar, JSlider,
                //   JTabbedpane, JTextComponent, JViewport)
                //
                try {
                    removeChangeMethod = c.getClass().getMethod(
                        "removeChangeListener", changeListeners);
                    try {
                        removeChangeMethod.invoke(c, changeArgs);
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

                //  Look for components which support the getModel method
                //  whose model supports Change listeners
                //  (e.g. BoundedRangeModel, ButtonModel, SingleSelectionModel)
                //
                try {
                    getModelMethod = c.getClass().getMethod(
                        "getModel", nullClass);
                    try {
                        Object o = getModelMethod.invoke(c, nullArgs);
                        if (o != null) {
                            removeChangeMethod = o.getClass().getMethod(
                                "removeChangeListener", changeListeners);
                            removeChangeMethod.invoke(o, changeArgs);
                        }
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

            case EventID.COLUMNMODEL:
                try {
                    getColumnModelMethod = c.getClass().getMethod(
                        "getTableColumnModel", nullClass);
                    try {
                        Object o = getColumnModelMethod.invoke(c, nullArgs);
                        if (o != null && o instanceof TableColumnModel) {
                            ((TableColumnModel) o).removeColumnModelListener(this);
                        }
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

            case EventID.DOCUMENT:
                //  Look for components which support the getDocument method
                //  (e.g. JTextComponent)
                //
                try {
                    getDocumentMethod = c.getClass().getMethod(
                        "getDocument", nullClass);
                    try {
                        Object o = getDocumentMethod.invoke(c, nullArgs);
                        if (o != null && o instanceof Document) {
                            ((Document) o).removeDocumentListener(this);
                        }
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

                //  Look for components which support Document listeners
                //  (no current example)
                //
                try {
                    removeDocumentMethod = c.getClass().getMethod(
                        "removeDocumentListener", documentListeners);
                    try {
                        removeDocumentMethod.invoke(c, documentArgs);
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

            case EventID.LISTDATA:
            case EventID.TABLEMODEL:
            case EventID.TREEMODEL:
                try {
                    getModelMethod = c.getClass().getMethod(
                        "getModel", nullClass);
                    try {
                        Object o = getModelMethod.invoke(c, nullArgs);
                        if (o != null) {
                            if (eventID == EventID.LISTDATA &&
                                o instanceof ListModel) {
                                ((ListModel) o).removeListDataListener(this);
                            } else if (eventID == EventID.TABLEMODEL &&
                                o instanceof TableModel) {
                                ((TableModel) o).removeTableModelListener(this);
                            } else if (
                                o instanceof TreeModel) {
                                ((TreeModel) o).removeTreeModelListener(this);
                            }
                        }
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

            case EventID.LISTSELECTION:
                //  Look for components which support ListSelectionListeners
                //  (e.g. JList)
                //
                try {
                    removeListSelectionMethod = c.getClass().getMethod(
                        "removeListSelectionListener", listSelectionListeners);
                    try {
                        removeListSelectionMethod.invoke(c, listSelectionArgs);
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

                // Look for selection models which support
                // ListSelectionListeners (e.g. JTable's selection model)
                //
                try {
                    getSelectionModelMethod = c.getClass().getMethod(
                        "getSelectionModel", nullClass);
                    try {
                        Object o = getSelectionModelMethod.invoke(c, nullArgs);
                        if (o != null && o instanceof ListSelectionModel) {
                            ((ListSelectionModel) o).removeListSelectionListener(this);
                        }
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

            case EventID.MENU:
                try {
                    removeMenuMethod = c.getClass().getMethod(
                        "removeMenuListener", menuListeners);
                    try {
                        removeMenuMethod.invoke(c, menuArgs);
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

            case EventID.POPUPMENU:
                //  Look for components which support PopupMenuListeners
                //  (e.g. JPopupMenu)
                //
                try {
                    removePopupMenuMethod = c.getClass().getMethod(
                        "removePopupMenuListener", popupMenuListeners);
                    try {
                        removePopupMenuMethod.invoke(c, popupMenuArgs);
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

                //  Look for components which support getPopupMenu
                //  (e.g. JMenu)
                //
                try {
                    getPopupMenuMethod = c.getClass().getMethod(
                        "getPopupMenu", nullClass);
                    try {
                        Object o = getPopupMenuMethod.invoke(c, nullArgs);
                        if (o != null) {
                            removePopupMenuMethod = o.getClass().getMethod(
                                "removePopupMenuListener", popupMenuListeners);
                            removePopupMenuMethod.invoke(o, popupMenuArgs);
                        }
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

            case EventID.TREEEXPANSION:
                try {
                    removeTreeExpansionMethod = c.getClass().getMethod(
                        "removeTreeExpansionListener", treeExpansionListeners);
                    try {
                        removeTreeExpansionMethod.invoke(c, treeExpansionArgs);
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

            case EventID.TREESELECTION:
                try {
                    removeTreeSelectionMethod = c.getClass().getMethod(
                        "removeTreeSelectionListener", treeSelectionListeners);
                    try {
                        removeTreeSelectionMethod.invoke(c, treeSelectionArgs);
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

            case EventID.UNDOABLEEDIT:
                //  Look for components which support the getDocument method
                //  (e.g. JTextComponent)
                //
                try {
                    getDocumentMethod = c.getClass().getMethod(
                        "getDocument", nullClass);
                    try {
                        Object o = getDocumentMethod.invoke(c, nullArgs);
                        if (o != null && o instanceof Document) {
                            ((Document) o).removeUndoableEditListener(this);
                        }
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

                //  Look for components which support UndoableEdit listeners
                //  (no current example)
                //
                try {
                    removeUndoableEditMethod = c.getClass().getMethod(
                        "removeUndoableEditListener", undoableEditListeners);
                    try {
                        removeUndoableEditMethod.invoke(c, undoableEditArgs);
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

            case EventID.INTERNALFRAME:
              try {
                    removeInternalFrameMethod = c.getClass().getMethod(
                        "removeInternalFrameListener", internalFrameListeners);
                    try {
                        removeInternalFrameMethod.invoke(c, internalFrameArgs);
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

            case EventID.PROPERTYCHANGE:
                //  Look for components which support PropertyChange listeners
                //  (e.g. JComponent)
                //
                try {
                    removePropertyChangeMethod = c.getClass().getMethod(
                        "removePropertyChangeListener", propertyChangeListeners);
                    try {
                        removePropertyChangeMethod.invoke(c, propertyChangeArgs);
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

                // Look for components which support the getSelectionModel
                // method (e.g. JTextComponent)
                //
                try {
                    getSelectionModelMethod = c.getClass().getMethod(
                        "getSelectionModel", nullClass);
                    try {
                        Object o = getSelectionModelMethod.invoke(c, nullArgs);
                        if (o != null && o instanceof TreeSelectionModel) {
                            ((TreeSelectionModel) o).removePropertyChangeListener(this);
                        }
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

            case EventID.VETOABLECHANGE:
                if (c instanceof JComponent) {
                    ((JComponent) c).removeVetoableChangeListener(this);
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

        /* ContainerListener Methods ************************************/

        public void componentAdded(ContainerEvent e) {
            installListeners(e.getChild());
        }
        public void componentRemoved(ContainerEvent e) {
            removeListeners(e.getChild());
        }

        /* AncestorListener Methods ******************************************/

        public void ancestorAdded(AncestorEvent e) {
            Object[] listeners = SwingEventMonitor.listenerList.getListenerList();
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==AncestorListener.class) {
                    ((AncestorListener)listeners[i+1]).ancestorAdded(e);
                }
            }
        }

        public void ancestorRemoved(AncestorEvent e) {
            Object[] listeners = SwingEventMonitor.listenerList.getListenerList();
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==AncestorListener.class) {
                    ((AncestorListener)listeners[i+1]).ancestorRemoved(e);
                }
            }
        }

        public void ancestorMoved(AncestorEvent e) {
            Object[] listeners = SwingEventMonitor.listenerList.getListenerList();
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==AncestorListener.class) {
                    ((AncestorListener)listeners[i+1]).ancestorMoved(e);
                }
            }
        }

        /* CaretListener Methods ******************************************/

        public void caretUpdate(CaretEvent e) {
            Object[] listeners = SwingEventMonitor.listenerList.getListenerList();
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==CaretListener.class) {
                    ((CaretListener)listeners[i+1]).caretUpdate(e);
                }
            }
        }

        /* CellEditorListener Methods *****************************************/

        public void editingStopped(ChangeEvent e) {
            Object[] listeners = SwingEventMonitor.listenerList.getListenerList();
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==CellEditorListener.class) {
                    ((CellEditorListener)listeners[i+1]).editingStopped(e);
                }
            }
        }

        public void editingCanceled(ChangeEvent e) {
            Object[] listeners = SwingEventMonitor.listenerList.getListenerList();
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==CellEditorListener.class) {
                    ((CellEditorListener)listeners[i+1]).editingCanceled(e);
                }
            }
        }

        /* ChangeListener Methods *****************************************/

        public void stateChanged(ChangeEvent e) {
            Object[] listeners = SwingEventMonitor.listenerList.getListenerList();
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==ChangeListener.class) {
                    ((ChangeListener)listeners[i+1]).stateChanged(e);
                }
            }
        }

        /* TableColumnModelListener Methods *******************************/

        public void columnAdded(TableColumnModelEvent e) {
            Object[] listeners = SwingEventMonitor.listenerList.getListenerList();
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==TableColumnModelListener.class) {
                    ((TableColumnModelListener)listeners[i+1]).columnAdded(e);
                }
            }
        }
        public void columnMarginChanged(ChangeEvent e) {
            Object[] listeners = SwingEventMonitor.listenerList.getListenerList();
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==TableColumnModelListener.class) {
                    ((TableColumnModelListener)listeners[i+1]).columnMarginChanged(e);
                }
            }
        }
        public void columnMoved(TableColumnModelEvent e) {
            Object[] listeners = SwingEventMonitor.listenerList.getListenerList();
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==TableColumnModelListener.class) {
                    ((TableColumnModelListener)listeners[i+1]).columnMoved(e);
                }
            }
        }
        public void columnRemoved(TableColumnModelEvent e) {
            Object[] listeners = SwingEventMonitor.listenerList.getListenerList();
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==TableColumnModelListener.class) {
                    ((TableColumnModelListener)listeners[i+1]).columnRemoved(e);
                }
            }
        }
        public void columnSelectionChanged(ListSelectionEvent e) {
            Object[] listeners = SwingEventMonitor.listenerList.getListenerList();
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==TableColumnModelListener.class) {
                    ((TableColumnModelListener)listeners[i+1]).columnSelectionChanged(e);
                }
            }
        }

        /* DocumentListener Methods **************************************/

        public void changedUpdate(DocumentEvent e) {
            Object[] listeners = SwingEventMonitor.listenerList.getListenerList();
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==DocumentListener.class) {
                    ((DocumentListener)listeners[i+1]).changedUpdate(e);
                }
            }
        }
        public void insertUpdate(DocumentEvent e) {
            Object[] listeners = SwingEventMonitor.listenerList.getListenerList();
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==DocumentListener.class) {
                    ((DocumentListener)listeners[i+1]).insertUpdate(e);
                }
            }
        }
        public void removeUpdate(DocumentEvent e) {
            Object[] listeners = SwingEventMonitor.listenerList.getListenerList();
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==DocumentListener.class) {
                    ((DocumentListener)listeners[i+1]).removeUpdate(e);
                }
            }
        }

        /* ListDataListener Methods *****************************************/

        public void contentsChanged(ListDataEvent e) {
            Object[] listeners = SwingEventMonitor.listenerList.getListenerList();
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==ListDataListener.class) {
                    ((ListDataListener)listeners[i+1]).contentsChanged(e);
                }
            }
        }
        public void intervalAdded(ListDataEvent e) {
            Object[] listeners = SwingEventMonitor.listenerList.getListenerList();
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==ListDataListener.class) {
                    ((ListDataListener)listeners[i+1]).intervalAdded(e);
                }
            }
        }
        public void intervalRemoved(ListDataEvent e) {
            Object[] listeners = SwingEventMonitor.listenerList.getListenerList();
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==ListDataListener.class) {
                    ((ListDataListener)listeners[i+1]).intervalRemoved(e);
                }
            }
        }

        /* ListSelectionListener Methods ***********************************/

        public void valueChanged(ListSelectionEvent e) {
            Object[] listeners = SwingEventMonitor.listenerList.getListenerList();
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==ListSelectionListener.class) {
                    ((ListSelectionListener)listeners[i+1]).valueChanged(e);
                }
            }
        }

        /* MenuListener Methods *****************************************/

        public void menuCanceled(MenuEvent e) {
            Object[] listeners = SwingEventMonitor.listenerList.getListenerList();
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==MenuListener.class) {
                    ((MenuListener)listeners[i+1]).menuCanceled(e);
                }
            }
        }
        public void menuDeselected(MenuEvent e) {
            Object[] listeners = SwingEventMonitor.listenerList.getListenerList();
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==MenuListener.class) {
                    ((MenuListener)listeners[i+1]).menuDeselected(e);
                }
            }
        }
        public void menuSelected(MenuEvent e) {
            Object[] listeners = SwingEventMonitor.listenerList.getListenerList();
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==MenuListener.class) {
                    ((MenuListener)listeners[i+1]).menuSelected(e);
                }
            }
        }

        /* PopupMenuListener Methods **************************************/

        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            Object[] listeners = SwingEventMonitor.listenerList.getListenerList();
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==PopupMenuListener.class) {
                    ((PopupMenuListener)listeners[i+1]).popupMenuWillBecomeVisible(e);
                }
            }
        }

        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            Object[] listeners = SwingEventMonitor.listenerList.getListenerList();
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==PopupMenuListener.class) {
                    ((PopupMenuListener)listeners[i+1]).popupMenuWillBecomeInvisible(e);
                }
            }
        }

        public void popupMenuCanceled(PopupMenuEvent e) {
            Object[] listeners = SwingEventMonitor.listenerList.getListenerList();
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==PopupMenuListener.class) {
                    ((PopupMenuListener)listeners[i+1]).popupMenuCanceled(e);
                }
            }
        }

        /* TableModelListener Methods **************************************/

        public void tableChanged(TableModelEvent e) {
            Object[] listeners = SwingEventMonitor.listenerList.getListenerList();
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==TableModelListener.class) {
                    ((TableModelListener)listeners[i+1]).tableChanged(e);
                }
            }
        }

        /* TreeExpansionListener Methods **********************************/

        public void treeCollapsed(TreeExpansionEvent e) {
            Object[] listeners = SwingEventMonitor.listenerList.getListenerList();
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==TreeExpansionListener.class) {
                    ((TreeExpansionListener)listeners[i+1]).treeCollapsed(e);
                }
            }
        }
        public void treeExpanded(TreeExpansionEvent e) {
            Object[] listeners = SwingEventMonitor.listenerList.getListenerList();
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==TreeExpansionListener.class) {
                    ((TreeExpansionListener)listeners[i+1]).treeExpanded(e);
                }
            }
        }

        /* TreeModelListener Methods **********************************/

        public void treeNodesChanged(TreeModelEvent e) {
            Object[] listeners = SwingEventMonitor.listenerList.getListenerList();
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==TreeModelListener.class) {
                    ((TreeModelListener)listeners[i+1]).treeNodesChanged(e);
                }
            }
        }
        public void treeNodesInserted(TreeModelEvent e) {
            Object[] listeners = SwingEventMonitor.listenerList.getListenerList();
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==TreeModelListener.class) {
                    ((TreeModelListener)listeners[i+1]).treeNodesInserted(e);
                }
            }
        }
        public void treeNodesRemoved(TreeModelEvent e) {
            Object[] listeners = SwingEventMonitor.listenerList.getListenerList();
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==TreeModelListener.class) {
                    ((TreeModelListener)listeners[i+1]).treeNodesRemoved(e);
                }
            }
        }
        public void treeStructureChanged(TreeModelEvent e) {
            Object[] listeners = SwingEventMonitor.listenerList.getListenerList();
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==TreeModelListener.class) {
                    ((TreeModelListener)listeners[i+1]).treeStructureChanged(e);
                }
            }
        }

        /* TreeSelectionListener Methods ***********************************/

        public void valueChanged(TreeSelectionEvent e) {
            Object[] listeners = SwingEventMonitor.listenerList.getListenerList();
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==TreeSelectionListener.class) {
                    ((TreeSelectionListener)listeners[i+1]).valueChanged(e);
                }
            }
        }

        /* UndoableEditListener Methods **************************************/

        public void undoableEditHappened(UndoableEditEvent e) {
            Object[] listeners = SwingEventMonitor.listenerList.getListenerList();
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==UndoableEditListener.class) {
                    ((UndoableEditListener)listeners[i+1]).undoableEditHappened(e);
                }
            }
        }

        /* InternalFrame Methods **********************************/

        public void internalFrameOpened(InternalFrameEvent e) {
            Object[] listeners = SwingEventMonitor.listenerList.getListenerList();
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==InternalFrameListener.class) {
                    ((InternalFrameListener)listeners[i+1]).internalFrameOpened(e);
                }
            }
        }

        public void internalFrameActivated(InternalFrameEvent e) {
            Object[] listeners = SwingEventMonitor.listenerList.getListenerList();
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==InternalFrameListener.class) {
                    ((InternalFrameListener)listeners[i+1]).internalFrameActivated(e);
                }
            }
        }

        public void internalFrameDeactivated(InternalFrameEvent e) {
            Object[] listeners = SwingEventMonitor.listenerList.getListenerList();
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==InternalFrameListener.class) {
                    ((InternalFrameListener)listeners[i+1]).internalFrameDeactivated(e);
                }
            }
        }

        public void internalFrameIconified(InternalFrameEvent e) {
            Object[] listeners = SwingEventMonitor.listenerList.getListenerList();
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==InternalFrameListener.class) {
                    ((InternalFrameListener)listeners[i+1]).internalFrameIconified(e);
                }
            }
        }

        public void internalFrameDeiconified(InternalFrameEvent e) {
            Object[] listeners = SwingEventMonitor.listenerList.getListenerList();
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==InternalFrameListener.class) {
                    ((InternalFrameListener)listeners[i+1]).internalFrameDeiconified(e);
                }
            }
        }

        public void internalFrameClosing(InternalFrameEvent e) {
            Object[] listeners = SwingEventMonitor.listenerList.getListenerList();
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==InternalFrameListener.class) {
                    ((InternalFrameListener)listeners[i+1]).internalFrameClosing(e);
                }
            }
        }

        public void internalFrameClosed(InternalFrameEvent e) {
            Object[] listeners = SwingEventMonitor.listenerList.getListenerList();
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==InternalFrameListener.class) {
                    ((InternalFrameListener)listeners[i+1]).internalFrameClosed(e);
                }
            }
        }

        /* PropertyChangeListener Methods **********************************/

        public void propertyChange(PropertyChangeEvent e) {
            Object[] listeners = SwingEventMonitor.listenerList.getListenerList();
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==PropertyChangeListener.class) {
                ((PropertyChangeListener)listeners[i+1]).propertyChange(e);
                }
            }
            // Re-add the monitor as a DocumentChangeListener if
            // the document changed in the text component.
            if (e.getSource() instanceof JTextComponent) {
                Document c = ((JTextComponent)e.getSource()).getDocument();
                if (c == null) {
                    return;
                }
                try {
                    removeDocumentMethod = c.getClass().getMethod(
                        "removeDocumentListener", documentListeners);
                    addDocumentMethod = c.getClass().getMethod(
                        "addDocumentListener", documentListeners);
                    try {
                        removeDocumentMethod.invoke(c, documentArgs);
                        addDocumentMethod.invoke(c, documentArgs);
                    } catch (java.lang.reflect.InvocationTargetException e2) {
                        System.out.println("Exception: " + e2.toString());
                    } catch (IllegalAccessException e2) {
                        System.out.println("Exception: " + e2.toString());
                    }
                } catch (NoSuchMethodException e2) {
                    // System.out.println("Exception: " + e2.toString());
                } catch (SecurityException e2) {
                    System.out.println("Exception: " + e2.toString());
                }
            }

        }

        /* VetoableChangeListener Methods **********************************/

        public void vetoableChange(PropertyChangeEvent e)
                throws PropertyVetoException {
            Object[] listeners = SwingEventMonitor.listenerList.getListenerList();
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==VetoableChangeListener.class) {
                    ((VetoableChangeListener)listeners[i+1]).vetoableChange(e);
                }
            }
        }
    }
}
