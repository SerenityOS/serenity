/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.java.accessibility.util.internal.*;
import java.beans.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
// Do not import Swing classes.  This module is intended to work
// with both Swing and AWT.
// import javax.swing.*;
import javax.accessibility.*;

/**
 * <p>The {@code Translator} class provides a translation to interface
 * {@link javax.accessibility.Accessible Accessible}
 * for objects that do not implement interface {@code Accessible}.  Assistive
 * technologies can use the {@link #getAccessible getAccessible} class method of
 * {@code Translator} to obtain an object that implements interface {@code Accessible}.
 * If the object passed in already implements interface {@code Accessible},
 * {@code getAccessible} merely returns the object.
 *
 * <p>An example of how an assistive technology might use the {@code Translator}
 * class is as follows:
 *
 * <PRE>
 *    Accessible accessible = Translator.getAccessible(someObj);
 *    // obtain information from the 'accessible' object.
 * </PRE>
 *
 * <P>Note:  This implementation is missing many things and is not a recommended way
 * to implement accessibility features for a toolkit.  Instead of relying upon this
 * code, a toolkit's components should implement interface {@code Accessible} directly.
 */
public class Translator extends AccessibleContext
        implements Accessible, AccessibleComponent {

    /** The source object needing translating. */
    protected Object source;

    /**
     * Find a translator for this class.  If one doesn't exist for this
     * class explicitly, try its superclass and so on.
     *
     * @param c a Class
     * @return the {@code Translator} Class for the Class passed in
     */
    protected static Class<?> getTranslatorClass(Class<?> c) {
        Class<?> t = null;
        if (c == null) {
            return null;
        }
        switch (c.getSimpleName()) {
            case "Button":
                t = ButtonTranslator.class;
                break;
            case "Checkbox":
                t = CheckboxTranslator.class;
                break;
            case "Label":
                t = LabelTranslator.class;
                break;
            case "List":
                t = ListTranslator.class;
                break;
            case "TextComponent":
                t = TextComponentTranslator.class;
                break;
        }
        if (t != null) {
            return t;
        } else {
            return getTranslatorClass(c.getSuperclass());
        }
    }

    /**
     * Obtain an object that implements interface {@code Accessible}.  If the object
     * passed in already implements interface {@code Accessible}, {@code getAccessible}
     * merely returns the object.
     *
     * @param o an Object; if a null is passed in a null is returned
     * @return an {@code Object}, possibly the {@code Object} passed in, that
     *     implements the {@code Accessible} interface for the {@code Object}
     *     which was passed in
     */
    public static Accessible getAccessible(Object o) {
        Accessible a = null;

        if (o == null) {
            return null;
        }
        if (o instanceof Accessible) {
            a = (Accessible)o;
        } else {
            Class<?> translatorClass = getTranslatorClass(o.getClass());
            if (translatorClass != null) {
                try {
                    @SuppressWarnings("deprecation")
                    Translator t = (Translator)translatorClass.newInstance();
                    t.setSource(o);
                    a = t;
                } catch (Exception e) {
                }
            }
        }
        if (a == null) {
            a = new Translator(o);
        }
        return a;
    }

    /**
     * Create a new {@code Translator}.  You must call the {@link #setSource setSource}
     * method to set the object to be translated after calling this constructor.
     */
    public Translator() {
    }

    /**
     * Create a new {@code Translator} with the source object o.
     *
     * @param o the Component that does not implement interface
     *     {@link javax.accessibility.Accessible Accessible}
     */
    public Translator(Object o) {
        source = o;
    }

    /**
     * Get the source {@code Object} of the {@code Translator}.
     *
     * @return the source {@code Object} of the {@code Translator}
     */
    public Object getSource() {
        return source;
    }

    /**
     * Set the source object of the {@code Translator}.
     *
     * @param o the Component that does not implement interface Accessible
     */
    public void setSource(Object o) {
        source = o;
    }

    /**
     * Returns true if this object is the same as the one passed in.
     *
     * @param o the {@code Object} to check against
     * @return true if this is the same object
     */
    public boolean equals(Object o) {
        if (o instanceof Translator) {
            return java.util.Objects.equals(source, o);
        } else {
            return false;
        }
    }

    /**
     * Return hashcode.
     *
     * @return hashcode
     */
    public int hashCode() {
        return java.util.Objects.hashCode(source);
    }


// Accessible methods

    /**
     * Returns this object.
     */
    public AccessibleContext getAccessibleContext() {
        return this;
    }

// AccessibleContext methods

    /**
     * Get the accessible name of this object.
     *
     * @return the localized name of the object; can be null if this object
     *     does not have a name
     */
    public String getAccessibleName() {
        if (source instanceof MenuItem) {
            return ((MenuItem) source).getLabel();
        } else if (source instanceof Component) {
            return ((Component) source).getName();
        } else {
            return null;
        }
    }

    /**
     * Set the name of this object.
     */
    public void setAccessibleName(String s) {
        if (source instanceof MenuItem) {
            ((MenuItem) source).setLabel(s);
        } else if (source instanceof Component) {
            ((Component) source).setName(s);
        }
    }

    /**
     * Get the accessible description of this object.
     *
     * @return the description of the object; can be null if this object does
     * not have a description
     */
    public String getAccessibleDescription() {
        return null;
    }

    /**
     * Set the accessible description of this object.
     *
     * @param s the new localized description of the object
     */
    public void setAccessibleDescription(String s) {
    }

    /**
     * Get the role of this object.
     *
     * @return an instance of AccessibleRole describing the role of the object
     */
    public AccessibleRole getAccessibleRole() {
        return AccessibleRole.UNKNOWN;
    }


    /**
     * Get the state of this object, given an already populated state.
     * This method is intended for use by subclasses so they don't have
     * to check for everything.
     *
     * @return an instance of {@code AccessibleStateSet}
     *     containing the current state of the object
     */
    public AccessibleStateSet getAccessibleStateSet() {
        AccessibleStateSet states = new AccessibleStateSet();
        if (source instanceof Component) {
            Component c = (Component) source;
            for (Container p = c.getParent(); p != null; p = p.getParent()) {
                if (p instanceof Window) {
                    if (((Window)p).getFocusOwner() == c) {
                        states.add(AccessibleState.FOCUSED);
                    }
                }
            }
        }
        if (isEnabled()) {
            states.add(AccessibleState.ENABLED);
        }
        if (isFocusTraversable()) {
            states.add(AccessibleState.FOCUSABLE);
        }
        if (source instanceof MenuItem) {
            states.add(AccessibleState.FOCUSABLE);
        }
        return states;
    }

    /**
     * Get the accessible parent of this object.
     *
     * @return the accessible parent of this object; can be null if this
     *     object does not have an accessible parent
     */
    public Accessible getAccessibleParent() {
        if (accessibleParent != null) {
            return accessibleParent;
        } else if (source instanceof Component) {
            return getAccessible(((Component) source).getParent());
        } else {
            return null;
        }
    }

    /**
     * Get the index of this object in its accessible parent.
     *
     * @return -1 of this object does not have an accessible parent; otherwise,
     * the index of the child in its accessible parent
     */
    public int getAccessibleIndexInParent() {
        if (source instanceof Component) {
            Container parent = ((Component) source).getParent();
            if (parent != null) {
                Component[] ca = parent.getComponents();
                for (int i = 0; i < ca.length; i++) {
                    if (source.equals(ca[i])) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    /**
     * Returns the number of accessible children in the object.
     *
     * @return the number of accessible children in the object
     */
    public int getAccessibleChildrenCount() {
        if (source instanceof Container) {
            Component[] children = ((Container) source).getComponents();
            int count = 0;
            for (int i = 0; i < children.length; i++) {
                Accessible a = getAccessible(children[i]);
                if (a != null) {
                    count++;
                }
            }
            return count;
        } else {
            return 0;
        }
    }

    /**
     * Return the nth accessible child of the object.
     *
     * @param i zero-based index of child
     * @return the nth accessible child of the object
     */
    public Accessible getAccessibleChild(int i) {
        if (source instanceof Container) {
            Component[] children = ((Container) source).getComponents();
            int count = 0;

            for (int j = 0; j < children.length; j++) {
                Accessible a = getAccessible(children[j]);
                if (a != null) {
                    if (count == i) {
                        AccessibleContext ac = a.getAccessibleContext();
                        if (ac != null) {
                            ac.setAccessibleParent(this);
                        }
                        return a;
                    } else {
                        count++;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Gets the {@code Locale} of the component. If the component does not have a
     * locale, the locale of its parent is returned.
     *
     * @return the {@code Locale} of the object
     */
    public Locale getLocale() throws IllegalComponentStateException {
        if (source instanceof Component) {
            return ((Component) source).getLocale();
        } else {
            return null;
        }
    }

    /**
     * Add a {@code PropertyChangeListener} to the listener list.  The listener
     * is registered for all properties.
     */
    public void addPropertyChangeListener(PropertyChangeListener l) {
    }

    /**
     * Remove the {@code PropertyChangeListener} from the listener list.
     */
    public void removePropertyChangeListener(PropertyChangeListener l) {
    }

// AccessibleComponent methods

    /**
     * Get the background {@code Color} of this object.
     *
     * @return if supported, the background {@code Color} of the object;
     *     otherwise, null
     *
     */
    public Color getBackground() {
        if (source instanceof Component) { // MenuComponent doesn't do background
            return ((Component) source).getBackground();
        } else {
            return null;
        }
    }

    /**
     * Set the background {@code Color} of this object.
     *
     * @param c the new {@code Color} for the background
     */
    public void setBackground(Color c) {
        if (source instanceof Component) { // MenuComponent doesn't do background
            ((Component) source).setBackground(c);
        }
    }

    /**
     * Get the foreground {@code Color} of this object.
     *
     * @return if supported, the foreground {@code Color} of the object; otherwise, null
     */
    public Color getForeground() {
        if (source instanceof Component) { // MenuComponent doesn't do foreground
            return ((Component) source).getForeground();
        } else {
            return null;
        }
    }

    /**
     * Set the foreground {@code Color} of this object.
     *
     * @param c the new {@code Color} for the foreground
     */
    public void setForeground(Color c) {
        if (source instanceof Component) { // MenuComponent doesn't do foreground
            ((Component) source).setForeground(c);
        }
    }

    /**
     * Get the {@code Cursor} of this object.
     *
     * @return if supported, the Cursor of the object; otherwise, null
     */
    public Cursor getCursor() {
        if (source instanceof Component) { // MenuComponent doesn't do cursor
            return ((Component) source).getCursor();
        } else {
            return null;
        }
    }

    /**
     * Set the {@code Cursor} of this object.
     * @param c the new {@code Cursor} for the object
     */
    public void setCursor(Cursor c) {
        if (source instanceof Component) { // MenuComponent doesn't do cursor
            ((Component) source).setCursor(c);
        }
    }

    /**
     * Get the {@code Font} of this object.
     *
     * @return if supported, the {@code Font} for the object; otherwise, null
     */
    public Font getFont() {
        if (source instanceof Component) {
            return ((Component) source).getFont();
        } else if (source instanceof MenuComponent) {
            return ((MenuComponent) source).getFont();
        } else {
            return null;
        }
    }

    /**
     * Set the {@code Font} of this object.
     *
     * @param f the new {@code Font} for the object
     */
    public void setFont(Font f) {
        if (source instanceof Component) {
            ((Component) source).setFont(f);
        } else if (source instanceof MenuComponent) {
            ((MenuComponent) source).setFont(f);
        }
    }

    /**
     * Get the {@code FontMetrics} of this object.
     *
     * @param f the {@code Font}
     * @return if supported, the {@code FontMetrics} the object; otherwise, null
     * @see #getFont
     */
    public FontMetrics getFontMetrics(Font f) {
        if (source instanceof Component) {
            return ((Component) source).getFontMetrics(f);
        } else {
            return null;
        }
    }

    /**
     * Determine if the object is enabled.
     *
     * @return true if object is enabled; otherwise, false
     */
    public boolean isEnabled() {
        if (source instanceof Component) {
            return ((Component) source).isEnabled();
        } else if (source instanceof MenuItem) {
            return ((MenuItem) source).isEnabled();
        } else {
            return true;
        }
    }

    /**
     * Set the enabled state of the object.
     *
     * @param b if true, enables this object; otherwise, disables it
     */
    public void setEnabled(boolean b) {
        if (source instanceof Component) {
            ((Component) source).setEnabled(b);
        } else if (source instanceof MenuItem) {
            ((MenuItem) source).setEnabled(b);
        }
    }

    /**
     * Determine if the object is visible.
     *
     * @return true if object is visible; otherwise, false
     */
    public boolean isVisible() {
        if (source instanceof Component) {
            return ((Component) source).isVisible();
        } else {
            return false;
        }
    }

    /**
     * Set the visible state of the object.
     *
     * @param b if true, shows this object; otherwise, hides it
     */
    public void setVisible(boolean b) {
        if (source instanceof Component) {
            ((Component) source).setVisible(b);
        }
    }

    /**
     * Determine if the object is showing.  This is determined by checking
     * the visibility of the object and ancestors of the object.
     *
     * @return true if object is showing; otherwise, false
     */
    public boolean isShowing() {
        if (source instanceof Component) {
            return ((Component) source).isShowing();
        } else {
            return false;
        }
    }

    /**
     * Checks whether the specified {@code Point} is within this
     * object's bounds, where the {@code Point} is relative to the coordinate
     * system of the object.
     *
     * @param p the {@code Point} relative to the coordinate system of the object
     * @return true if object contains {@code Point}; otherwise false
     */
    public boolean contains(Point p) {
        if (source instanceof Component) {
            return ((Component) source).contains(p);
        } else {
            return false;
        }
    }

    /**
     * Returns the location of the object on the screen.
     *
     * @return location of object on screen; can be null if this object
     *     is not on the screen
     */
    public Point getLocationOnScreen() {
        if (source instanceof Component) {
            return ((Component) source).getLocationOnScreen();
        } else {
            return null;
        }
    }

    /**
     * Returns the location of the object relative to parent.
     *
     * @return location of object relative to parent; can be null if
     *     this object or its parent are not on the screen
     */
    public Point getLocation() {
        if (source instanceof Component) {
            return ((Component) source).getLocation();
        } else {
            return null;
        }
    }

    /**
     * Sets the location of the object relative to parent.
     */
    public void setLocation(Point p) {
        if (source instanceof Component) {
            ((Component) source).setLocation(p);
        }
    }

    /**
     * Returns the current bounds of this object.
     *
     * @return current bounds of object; can be null if this object
     *     is not on the screen
     */
    public Rectangle getBounds() {
        if (source instanceof Component) {
            return ((Component) source).getBounds();
        } else {
            return null;
        }
    }

    /**
     * Sets the current bounds of this object.
     */
    public void setBounds(Rectangle r) {
        if (source instanceof Component) {
            ((Component) source).setBounds(r);
        }
    }

    /**
     * Returns the current size of this object.
     *
     * @return current size of object; can be null if this object is
     *     not on the screen
     */
    public Dimension getSize() {
        if (source instanceof Component) {
            return ((Component) source).getSize();
        } else {
            return null;
        }
    }

    /**
     * Sets the current size of this object.
     */
    public void setSize(Dimension d) {
        if (source instanceof Component) {
            ((Component) source).setSize(d);
        }
    }

    /**
     * Returns the accessible child contained at the local coordinate
     * Point, if one exists.
     *
     * @return the Accessible at the specified location, if it exists
     */
    public Accessible getAccessibleAt(Point p) {
        if (source instanceof Component) {
            Component c = ((Component) source).getComponentAt(p);
            if (c != null) {
                return (getAccessible(c));
            }
        }
        return null;
    }

    /**
     * Returns whether this object can accept focus or not.
     *
     * @return true if object can accept focus; otherwise false
     */
    @SuppressWarnings("deprecation")
    public boolean isFocusTraversable() {
        if (source instanceof Component) {
            return ((Component) source).isFocusTraversable();
        } else {
            return false;
        }
    }

    /**
     * Requests focus for this object.
     */
    public void requestFocus() {
        if (source instanceof Component) {
            ((Component) source).requestFocus();
        }
    }

    /**
     * Adds the specified {@code FocusListener} to receive focus events from
     * this component.
     *
     * @param l the focus listener
     */
    public synchronized void addFocusListener(FocusListener l) {
        if (source instanceof Component) {
            ((Component) source).addFocusListener(l);
        }
    }

    /**
     * Removes the specified focus listener so it no longer receives focus
     * events from this component.
     *
     * @param l the focus listener; this method performs no function, nor does it
     *     throw an exception if the listener specified was not previously added
     *     to this component; if listener is null, no exception is thrown and no
     *     action is performed.
     */
    public synchronized void removeFocusListener(FocusListener l) {
        if (source instanceof Component) {
            ((Component) source).removeFocusListener(l);
        }
    }
}
