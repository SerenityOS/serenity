/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing.text.html;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.accessibility.*;
import java.text.BreakIterator;

/*
 * The AccessibleHTML class provide information about the contents
 * of a HTML document to assistive technologies.
 *
 * @author  Lynn Monsanto
 */
class AccessibleHTML implements Accessible {

    /**
     * The editor.
     */
    private JEditorPane editor;
    /**
     * Current model.
     */
    private Document model;
    /**
     * DocumentListener installed on the current model.
     */
    private DocumentListener docListener;
    /**
     * PropertyChangeListener installed on the editor
     */
    private PropertyChangeListener propChangeListener;
    /**
     * The root ElementInfo for the document
     */
    private ElementInfo rootElementInfo;
    /*
     * The root accessible context for the document
     */
    private RootHTMLAccessibleContext rootHTMLAccessibleContext;

    public AccessibleHTML(JEditorPane pane) {
        editor = pane;
        propChangeListener = new PropertyChangeHandler();
        setDocument(editor.getDocument());

        docListener = new DocumentHandler();
    }

    /**
     * Sets the document.
     */
    private void setDocument(Document document) {
        if (model != null) {
            model.removeDocumentListener(docListener);
        }
        if (editor != null) {
            editor.removePropertyChangeListener(propChangeListener);
        }
        this.model = document;
        if (model != null) {
            if (rootElementInfo != null) {
                rootElementInfo.invalidate(false);
            }
            buildInfo();
            model.addDocumentListener(docListener);
        }
        else {
            rootElementInfo = null;
        }
        if (editor != null) {
            editor.addPropertyChangeListener(propChangeListener);
        }
    }

    /**
     * Returns the Document currently presenting information for.
     */
    private Document getDocument() {
        return model;
    }

    /**
     * Returns the JEditorPane providing information for.
     */
    private JEditorPane getTextComponent() {
        return editor;
    }

    /**
     * Returns the ElementInfo representing the root Element.
     */
    private ElementInfo getRootInfo() {
        return rootElementInfo;
    }

    /**
     * Returns the root <code>View</code> associated with the current text
     * component.
     */
    private View getRootView() {
        return getTextComponent().getUI().getRootView(getTextComponent());
    }

    /**
     * Returns the bounds the root View will be rendered in.
     */
    private Rectangle getRootEditorRect() {
        Rectangle alloc = getTextComponent().getBounds();
        if ((alloc.width > 0) && (alloc.height > 0)) {
            alloc.x = alloc.y = 0;
            Insets insets = editor.getInsets();
            alloc.x += insets.left;
            alloc.y += insets.top;
            alloc.width -= insets.left + insets.right;
            alloc.height -= insets.top + insets.bottom;
            return alloc;
        }
        return null;
    }

    /**
     * If possible acquires a lock on the Document.  If a lock has been
     * obtained a key will be retured that should be passed to
     * <code>unlock</code>.
     */
    private Object lock() {
        Document document = getDocument();

        if (document instanceof AbstractDocument) {
            ((AbstractDocument)document).readLock();
            return document;
        }
        return null;
    }

    /**
     * Releases a lock previously obtained via <code>lock</code>.
     */
    private void unlock(Object key) {
        if (key != null) {
            ((AbstractDocument)key).readUnlock();
        }
    }

    /**
     * Rebuilds the information from the current info.
     */
    private void buildInfo() {
        Object lock = lock();

        try {
            Document doc = getDocument();
            Element root = doc.getDefaultRootElement();

            rootElementInfo = new ElementInfo(root);
            rootElementInfo.validate();
        } finally {
            unlock(lock);
        }
    }

    /*
     * Create an ElementInfo subclass based on the passed in Element.
     */
    ElementInfo createElementInfo(Element e, ElementInfo parent) {
        AttributeSet attrs = e.getAttributes();

        if (attrs != null) {
            Object name = attrs.getAttribute(StyleConstants.NameAttribute);

            if (name == HTML.Tag.IMG) {
                return new IconElementInfo(e, parent);
            }
            else if (name == HTML.Tag.CONTENT || name == HTML.Tag.CAPTION) {
                return new TextElementInfo(e, parent);
            }
            else if (name == HTML.Tag.TABLE) {
                return new TableElementInfo(e, parent);
            }
        }
        return null;
    }

    /**
     * Returns the root AccessibleContext for the document
     */
    public AccessibleContext getAccessibleContext() {
        if (rootHTMLAccessibleContext == null) {
            rootHTMLAccessibleContext =
                new RootHTMLAccessibleContext(rootElementInfo);
        }
        return rootHTMLAccessibleContext;
    }

    /*
     * The roow AccessibleContext for the document
     */
    private class RootHTMLAccessibleContext extends HTMLAccessibleContext {

        public RootHTMLAccessibleContext(ElementInfo elementInfo) {
            super(elementInfo);
        }

        /**
         * Gets the accessibleName property of this object.  The accessibleName
         * property of an object is a localized String that designates the purpose
         * of the object.  For example, the accessibleName property of a label
         * or button might be the text of the label or button itself.  In the
         * case of an object that doesn't display its name, the accessibleName
         * should still be set.  For example, in the case of a text field used
         * to enter the name of a city, the accessibleName for the en_US locale
         * could be 'city.'
         *
         * @return the localized name of the object; null if this
         * object does not have a name
         *
         * @see #setAccessibleName
         */
        public String getAccessibleName() {
            if (model != null) {
                return (String)model.getProperty(Document.TitleProperty);
            } else {
                return null;
            }
        }

        /**
         * Gets the accessibleDescription property of this object.  If this
         * property isn't set, returns the content type of this
         * <code>JEditorPane</code> instead (e.g. "plain/text", "html/text").
         *
         * @return the localized description of the object; <code>null</code>
         *      if this object does not have a description
         *
         * @see #setAccessibleName
         */
        public String getAccessibleDescription() {
            return editor.getContentType();
        }

        /**
         * Gets the role of this object.  The role of the object is the generic
         * purpose or use of the class of this object.  For example, the role
         * of a push button is AccessibleRole.PUSH_BUTTON.  The roles in
         * AccessibleRole are provided so component developers can pick from
         * a set of predefined roles.  This enables assistive technologies to
         * provide a consistent interface to various tweaked subclasses of
         * components (e.g., use AccessibleRole.PUSH_BUTTON for all components
         * that act like a push button) as well as distinguish between subclasses
         * that behave differently (e.g., AccessibleRole.CHECK_BOX for check boxes
         * and AccessibleRole.RADIO_BUTTON for radio buttons).
         * <p>Note that the AccessibleRole class is also extensible, so
         * custom component developers can define their own AccessibleRole's
         * if the set of predefined roles is inadequate.
         *
         * @return an instance of AccessibleRole describing the role of the object
         * @see AccessibleRole
         */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.TEXT;
        }
    }

    /*
     * Base AccessibleContext class for HTML elements
     */
    protected abstract class HTMLAccessibleContext extends AccessibleContext
        implements Accessible, AccessibleComponent {

        protected ElementInfo elementInfo;

        public HTMLAccessibleContext(ElementInfo elementInfo) {
            this.elementInfo = elementInfo;
        }

        // begin AccessibleContext implementation ...
        public AccessibleContext getAccessibleContext() {
            return this;
        }

        /**
         * Gets the state set of this object.
         *
         * @return an instance of AccessibleStateSet describing the states
         * of the object
         * @see AccessibleStateSet
         */
        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet states = new AccessibleStateSet();
            Component comp = getTextComponent();

            if (comp.isEnabled()) {
                states.add(AccessibleState.ENABLED);
            }
            if (comp instanceof JTextComponent &&
                ((JTextComponent)comp).isEditable()) {

                states.add(AccessibleState.EDITABLE);
                states.add(AccessibleState.FOCUSABLE);
            }
            if (comp.isVisible()) {
                states.add(AccessibleState.VISIBLE);
            }
            if (comp.isShowing()) {
                states.add(AccessibleState.SHOWING);
            }
            return states;
        }

        /**
         * Gets the 0-based index of this object in its accessible parent.
         *
         * @return the 0-based index of this object in its parent; -1 if this
         * object does not have an accessible parent.
         *
         * @see #getAccessibleParent
         * @see #getAccessibleChildrenCount
         * @see #getAccessibleChild
         */
        public int getAccessibleIndexInParent() {
            return elementInfo.getIndexInParent();
        }

        /**
         * Returns the number of accessible children of the object.
         *
         * @return the number of accessible children of the object.
         */
        public int getAccessibleChildrenCount() {
            return elementInfo.getChildCount();
        }

        /**
         * Returns the specified Accessible child of the object.  The Accessible
         * children of an Accessible object are zero-based, so the first child
         * of an Accessible child is at index 0, the second child is at index 1,
         * and so on.
         *
         * @param i zero-based index of child
         * @return the Accessible child of the object
         * @see #getAccessibleChildrenCount
         */
        public Accessible getAccessibleChild(int i) {
            ElementInfo childInfo = elementInfo.getChild(i);
            if (childInfo != null && childInfo instanceof Accessible) {
                return (Accessible)childInfo;
            } else {
                return null;
            }
        }

        /**
         * Gets the locale of the component. If the component does not have a
         * locale, then the locale of its parent is returned.
         *
         * @return this component's locale.  If this component does not have
         * a locale, the locale of its parent is returned.
         *
         * @exception IllegalComponentStateException
         * If the Component does not have its own locale and has not yet been
         * added to a containment hierarchy such that the locale can be
         * determined from the containing parent.
         */
        public Locale getLocale() throws IllegalComponentStateException {
            return editor.getLocale();
        }
        // ... end AccessibleContext implementation

        // begin AccessibleComponent implementation ...
        public AccessibleComponent getAccessibleComponent() {
            return this;
        }

        /**
         * Gets the background color of this object.
         *
         * @return the background color, if supported, of the object;
         * otherwise, null
         * @see #setBackground
         */
        public Color getBackground() {
            return getTextComponent().getBackground();
        }

        /**
         * Sets the background color of this object.
         *
         * @param c the new Color for the background
         * @see #setBackground
         */
        public void setBackground(Color c) {
            getTextComponent().setBackground(c);
        }

        /**
         * Gets the foreground color of this object.
         *
         * @return the foreground color, if supported, of the object;
         * otherwise, null
         * @see #setForeground
         */
        public Color getForeground() {
            return getTextComponent().getForeground();
        }

        /**
         * Sets the foreground color of this object.
         *
         * @param c the new Color for the foreground
         * @see #getForeground
         */
        public void setForeground(Color c) {
            getTextComponent().setForeground(c);
        }

        /**
         * Gets the Cursor of this object.
         *
         * @return the Cursor, if supported, of the object; otherwise, null
         * @see #setCursor
         */
        public Cursor getCursor() {
            return getTextComponent().getCursor();
        }

        /**
         * Sets the Cursor of this object.
         *
         * @param cursor the new Cursor for the object
         * @see #getCursor
         */
        public void setCursor(Cursor cursor) {
            getTextComponent().setCursor(cursor);
        }

        /**
         * Gets the Font of this object.
         *
         * @return the Font,if supported, for the object; otherwise, null
         * @see #setFont
         */
        public Font getFont() {
            return getTextComponent().getFont();
        }

        /**
         * Sets the Font of this object.
         *
         * @param f the new Font for the object
         * @see #getFont
         */
        public void setFont(Font f) {
            getTextComponent().setFont(f);
        }

        /**
         * Gets the FontMetrics of this object.
         *
         * @param f the Font
         * @return the FontMetrics, if supported, the object; otherwise, null
         * @see #getFont
         */
        public FontMetrics getFontMetrics(Font f) {
            return getTextComponent().getFontMetrics(f);
        }

        /**
         * Determines if the object is enabled.  Objects that are enabled
         * will also have the AccessibleState.ENABLED state set in their
         * AccessibleStateSets.
         *
         * @return true if object is enabled; otherwise, false
         * @see #setEnabled
         * @see AccessibleContext#getAccessibleStateSet
         * @see AccessibleState#ENABLED
         * @see AccessibleStateSet
         */
        public boolean isEnabled() {
            return getTextComponent().isEnabled();
        }

        /**
         * Sets the enabled state of the object.
         *
         * @param b if true, enables this object; otherwise, disables it
         * @see #isEnabled
         */
        public void setEnabled(boolean b) {
            getTextComponent().setEnabled(b);
        }

        /**
         * Determines if the object is visible.  Note: this means that the
         * object intends to be visible; however, it may not be
         * showing on the screen because one of the objects that this object
         * is contained by is currently not visible.  To determine if an object
         * is showing on the screen, use isShowing().
         * <p>Objects that are visible will also have the
         * AccessibleState.VISIBLE state set in their AccessibleStateSets.
         *
         * @return true if object is visible; otherwise, false
         * @see #setVisible
         * @see AccessibleContext#getAccessibleStateSet
         * @see AccessibleState#VISIBLE
         * @see AccessibleStateSet
         */
        public boolean isVisible() {
            return getTextComponent().isVisible();
        }

        /**
         * Sets the visible state of the object.
         *
         * @param b if true, shows this object; otherwise, hides it
         * @see #isVisible
         */
        public void setVisible(boolean b) {
            getTextComponent().setVisible(b);
        }

        /**
         * Determines if the object is showing.  This is determined by checking
         * the visibility of the object and its ancestors.
         * Note: this
         * will return true even if the object is obscured by another (for
         * example, it is underneath a menu that was pulled down).
         *
         * @return true if object is showing; otherwise, false
         */
        public boolean isShowing() {
            return getTextComponent().isShowing();
        }

        /**
         * Checks whether the specified point is within this object's bounds,
         * where the point's x and y coordinates are defined to be relative
         * to the coordinate system of the object.
         *
         * @param p the Point relative to the coordinate system of the object
         * @return true if object contains Point; otherwise false
         * @see #getBounds
         */
        public boolean contains(Point p) {
            Rectangle r = getBounds();
            if (r != null) {
                return r.contains(p.x, p.y);
            } else {
                return false;
            }
        }

        /**
         * Returns the location of the object on the screen.
         *
         * @return the location of the object on screen; null if this object
         * is not on the screen
         * @see #getBounds
         * @see #getLocation
         */
        public Point getLocationOnScreen() {
            Point editorLocation = getTextComponent().getLocationOnScreen();
            Rectangle r = getBounds();
            if (r != null) {
                return new Point(editorLocation.x + r.x,
                                 editorLocation.y + r.y);
            } else {
                return null;
            }
        }

        /**
         * Gets the location of the object relative to the parent in the form
         * of a point specifying the object's top-left corner in the screen's
         * coordinate space.
         *
         * @return An instance of Point representing the top-left corner of the
         * object's bounds in the coordinate space of the screen; null if
         * this object or its parent are not on the screen
         * @see #getBounds
         * @see #getLocationOnScreen
         */
        public Point getLocation() {
            Rectangle r = getBounds();
            if (r != null) {
                return new Point(r.x, r.y);
            } else {
                return null;
            }
        }

        /**
         * Sets the location of the object relative to the parent.
         * @param p the new position for the top-left corner
         * @see #getLocation
         */
        public void setLocation(Point p) {
        }

        /**
         * Gets the bounds of this object in the form of a Rectangle object.
         * The bounds specify this object's width, height, and location
         * relative to its parent.
         *
         * @return A rectangle indicating this component's bounds; null if
         * this object is not on the screen.
         * @see #contains
         */
        public Rectangle getBounds() {
            return elementInfo.getBounds();
        }

        /**
         * Sets the bounds of this object in the form of a Rectangle object.
         * The bounds specify this object's width, height, and location
         * relative to its parent.
         *
         * @param r rectangle indicating this component's bounds
         * @see #getBounds
         */
        public void setBounds(Rectangle r) {
        }

        /**
         * Returns the size of this object in the form of a Dimension object.
         * The height field of the Dimension object contains this object's
         * height, and the width field of the Dimension object contains this
         * object's width.
         *
         * @return A Dimension object that indicates the size of this component;
         * null if this object is not on the screen
         * @see #setSize
         */
        public Dimension getSize() {
            Rectangle r = getBounds();
            if (r != null) {
                return new Dimension(r.width, r.height);
            } else {
                return null;
            }
        }

        /**
         * Resizes this object so that it has width and height.
         *
         * @param d The dimension specifying the new size of the object.
         * @see #getSize
         */
        public void setSize(Dimension d) {
            Component comp = getTextComponent();
            comp.setSize(d);
        }

        /**
         * Returns the Accessible child, if one exists, contained at the local
         * coordinate Point.
         *
         * @param p The point relative to the coordinate system of this object.
         * @return the Accessible, if it exists, at the specified location;
         * otherwise null
         */
        public Accessible getAccessibleAt(Point p) {
            ElementInfo innerMostElement = getElementInfoAt(rootElementInfo, p);
            if (innerMostElement instanceof Accessible) {
                return (Accessible)innerMostElement;
            } else {
                return null;
            }
        }

        private ElementInfo getElementInfoAt(ElementInfo elementInfo, Point p) {
            if (elementInfo.getBounds() == null) {
                return null;
            }
            if (elementInfo.getChildCount() == 0 &&
                elementInfo.getBounds().contains(p)) {
                return elementInfo;

            } else {
                if (elementInfo instanceof TableElementInfo) {
                    // Handle table caption as a special case since it's the
                    // only table child that is not a table row.
                    ElementInfo captionInfo =
                        ((TableElementInfo)elementInfo).getCaptionInfo();
                    if (captionInfo != null) {
                        Rectangle bounds = captionInfo.getBounds();
                        if (bounds != null && bounds.contains(p)) {
                            return captionInfo;
                        }
                    }
                }
                for (int i = 0; i < elementInfo.getChildCount(); i++)
{
                    ElementInfo childInfo = elementInfo.getChild(i);
                    ElementInfo retValue = getElementInfoAt(childInfo, p);
                    if (retValue != null) {
                        return retValue;
                    }
                }
            }
            return null;
        }

        /**
         * Returns whether this object can accept focus or not.   Objects that
         * can accept focus will also have the AccessibleState.FOCUSABLE state
         * set in their AccessibleStateSets.
         *
         * @return true if object can accept focus; otherwise false
         * @see AccessibleContext#getAccessibleStateSet
         * @see AccessibleState#FOCUSABLE
         * @see AccessibleState#FOCUSED
         * @see AccessibleStateSet
         */
        public boolean isFocusTraversable() {
            Component comp = getTextComponent();
            if (comp instanceof JTextComponent) {
                if (((JTextComponent)comp).isEditable()) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Requests focus for this object.  If this object cannot accept focus,
         * nothing will happen.  Otherwise, the object will attempt to take
         * focus.
         * @see #isFocusTraversable
         */
        public void requestFocus() {
            // TIGER - 4856191
            if (! isFocusTraversable()) {
                return;
            }

            Component comp = getTextComponent();
            if (comp instanceof JTextComponent) {

                comp.requestFocusInWindow();

                try {
                    if (elementInfo.validateIfNecessary()) {
                        // set the caret position to the start of this component
                        Element elem = elementInfo.getElement();
                        ((JTextComponent)comp).setCaretPosition(elem.getStartOffset());

                        // fire a AccessibleState.FOCUSED property change event
                        AccessibleContext ac = editor.getAccessibleContext();
                        PropertyChangeEvent pce = new PropertyChangeEvent(this,
                            AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                            null, AccessibleState.FOCUSED);
                        ac.firePropertyChange(
                            AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                            null, pce);
                    }
                } catch (IllegalArgumentException e) {
                    // don't fire property change event
                }
            }
        }

        /**
         * Adds the specified focus listener to receive focus events from this
         * component.
         *
         * @param l the focus listener
         * @see #removeFocusListener
         */
        public void addFocusListener(FocusListener l) {
            getTextComponent().addFocusListener(l);
        }

        /**
         * Removes the specified focus listener so it no longer receives focus
         * events from this component.
         *
         * @param l the focus listener
         * @see #addFocusListener
         */
        public void removeFocusListener(FocusListener l) {
            getTextComponent().removeFocusListener(l);
        }
        // ... end AccessibleComponent implementation
    } // ... end HTMLAccessibleContext



    /*
     * ElementInfo for text
     */
    class TextElementInfo extends ElementInfo implements Accessible {

        TextElementInfo(Element element, ElementInfo parent) {
            super(element, parent);
        }

        // begin AccessibleText implementation ...
        private AccessibleContext accessibleContext;

        public AccessibleContext getAccessibleContext() {
            if (accessibleContext == null) {
                accessibleContext = new TextAccessibleContext(this);
            }
            return accessibleContext;
        }

        /*
         * AccessibleContext for text elements
         */
        public class TextAccessibleContext extends HTMLAccessibleContext
            implements AccessibleText {

            public TextAccessibleContext(ElementInfo elementInfo) {
                super(elementInfo);
            }

            public AccessibleText getAccessibleText() {
                return this;
            }

            /**
             * Gets the accessibleName property of this object.  The accessibleName
             * property of an object is a localized String that designates the purpose
             * of the object.  For example, the accessibleName property of a label
             * or button might be the text of the label or button itself.  In the
             * case of an object that doesn't display its name, the accessibleName
             * should still be set.  For example, in the case of a text field used
             * to enter the name of a city, the accessibleName for the en_US locale
             * could be 'city.'
             *
             * @return the localized name of the object; null if this
             * object does not have a name
             *
             * @see #setAccessibleName
             */
            public String getAccessibleName() {
                if (model != null) {
                    return (String)model.getProperty(Document.TitleProperty);
                } else {
                    return null;
                }
            }

            /**
             * Gets the accessibleDescription property of this object.  If this
             * property isn't set, returns the content type of this
             * <code>JEditorPane</code> instead (e.g. "plain/text", "html/text").
             *
             * @return the localized description of the object; <code>null</code>
             *  if this object does not have a description
             *
             * @see #setAccessibleName
             */
            public String getAccessibleDescription() {
                return editor.getContentType();
            }

            /**
             * Gets the role of this object.  The role of the object is the generic
             * purpose or use of the class of this object.  For example, the role
             * of a push button is AccessibleRole.PUSH_BUTTON.  The roles in
             * AccessibleRole are provided so component developers can pick from
             * a set of predefined roles.  This enables assistive technologies to
             * provide a consistent interface to various tweaked subclasses of
             * components (e.g., use AccessibleRole.PUSH_BUTTON for all components
             * that act like a push button) as well as distinguish between subclasses
             * that behave differently (e.g., AccessibleRole.CHECK_BOX for check boxes
             * and AccessibleRole.RADIO_BUTTON for radio buttons).
             * <p>Note that the AccessibleRole class is also extensible, so
             * custom component developers can define their own AccessibleRole's
             * if the set of predefined roles is inadequate.
             *
             * @return an instance of AccessibleRole describing the role of the object
             * @see AccessibleRole
             */
            public AccessibleRole getAccessibleRole() {
                return AccessibleRole.TEXT;
            }

            /**
             * Given a point in local coordinates, return the zero-based index
             * of the character under that Point.  If the point is invalid,
             * this method returns -1.
             *
             * @param p the Point in local coordinates
             * @return the zero-based index of the character under Point p; if
             * Point is invalid returns -1.
             */
            @SuppressWarnings("deprecation")
            public int getIndexAtPoint(Point p) {
                View v = getView();
                if (v != null) {
                    return v.viewToModel(p.x, p.y, getBounds());
                } else {
                    return -1;
                }
            }

            /**
             * Determine the bounding box of the character at the given
             * index into the string.  The bounds are returned in local
             * coordinates.  If the index is invalid an empty rectangle is
             * returned.
             *
             * @param i the index into the String
             * @return the screen coordinates of the character's the bounding box,
             * if index is invalid returns an empty rectangle.
             */
            @SuppressWarnings("deprecation")
            public Rectangle getCharacterBounds(int i) {
                try {
                    return editor.getUI().modelToView(editor, i);
                } catch (BadLocationException e) {
                    return null;
                }
            }

            /**
             * Return the number of characters (valid indicies)
             *
             * @return the number of characters
             */
            public int getCharCount() {
                if (validateIfNecessary()) {
                    Element elem = elementInfo.getElement();
                    return elem.getEndOffset() - elem.getStartOffset();
                }
                return 0;
            }

            /**
             * Return the zero-based offset of the caret.
             *
             * Note: That to the right of the caret will have the same index
             * value as the offset (the caret is between two characters).
             * @return the zero-based offset of the caret.
             */
            public int getCaretPosition() {
                View v = getView();
                if (v == null) {
                    return -1;
                }
                Container c = v.getContainer();
                if (c == null) {
                    return -1;
                }
                if (c instanceof JTextComponent) {
                    return ((JTextComponent)c).getCaretPosition();
                } else {
                    return -1;
                }
            }

            /**
             * IndexedSegment extends Segment adding the offset into the
             * the model the <code>Segment</code> was asked for.
             */
            private class IndexedSegment extends Segment {
                /**
                 * Offset into the model that the position represents.
                 */
                public int modelOffset;
            }

            public String getAtIndex(int part, int index) {
                return getAtIndex(part, index, 0);
            }


            public String getAfterIndex(int part, int index) {
                return getAtIndex(part, index, 1);
            }

            public String getBeforeIndex(int part, int index) {
                return getAtIndex(part, index, -1);
            }

            /**
             * Gets the word, sentence, or character at <code>index</code>.
             * If <code>direction</code> is non-null this will find the
             * next/previous word/sentence/character.
             */
            private String getAtIndex(int part, int index, int direction) {
                if (model instanceof AbstractDocument) {
                    ((AbstractDocument)model).readLock();
                }
                try {
                    if (index < 0 || index >= model.getLength()) {
                        return null;
                    }
                    switch (part) {
                    case AccessibleText.CHARACTER:
                        if (index + direction < model.getLength() &&
                            index + direction >= 0) {
                            return model.getText(index + direction, 1);
                        }
                        break;


                    case AccessibleText.WORD:
                    case AccessibleText.SENTENCE:
                        IndexedSegment seg = getSegmentAt(part, index);
                        if (seg != null) {
                            if (direction != 0) {
                                int next;


                                if (direction < 0) {
                                    next = seg.modelOffset - 1;
                                }
                                else {
                                    next = seg.modelOffset + direction * seg.count;
                                }
                                if (next >= 0 && next <= model.getLength()) {
                                    seg = getSegmentAt(part, next);
                                }
                                else {
                                    seg = null;
                                }
                            }
                            if (seg != null) {
                                return new String(seg.array, seg.offset,
                                                  seg.count);
                            }
                        }
                        break;

                    default:
                        break;
                    }
                } catch (BadLocationException e) {
                } finally {
                    if (model instanceof AbstractDocument) {
                        ((AbstractDocument)model).readUnlock();
                    }
                }
                return null;
            }

            /*
             * Returns the paragraph element for the specified index.
             */
            private Element getParagraphElement(int index) {
                if (model instanceof PlainDocument ) {
                    PlainDocument sdoc = (PlainDocument)model;
                    return sdoc.getParagraphElement(index);
                } else if (model instanceof StyledDocument) {
                    StyledDocument sdoc = (StyledDocument)model;
                    return sdoc.getParagraphElement(index);
                } else {
                    Element para;
                    for (para = model.getDefaultRootElement(); ! para.isLeaf(); ) {
                        int pos = para.getElementIndex(index);
                        para = para.getElement(pos);
                    }
                    if (para == null) {
                        return null;
                    }
                    return para.getParentElement();
                }
            }

            /*
             * Returns a <code>Segment</code> containing the paragraph text
             * at <code>index</code>, or null if <code>index</code> isn't
             * valid.
             */
            private IndexedSegment getParagraphElementText(int index)
                throws BadLocationException {
                Element para = getParagraphElement(index);


                if (para != null) {
                    IndexedSegment segment = new IndexedSegment();
                    try {
                        int length = para.getEndOffset() - para.getStartOffset();
                        model.getText(para.getStartOffset(), length, segment);
                    } catch (BadLocationException e) {
                        return null;
                    }
                    segment.modelOffset = para.getStartOffset();
                    return segment;
                }
                return null;
            }


            /**
             * Returns the Segment at <code>index</code> representing either
             * the paragraph or sentence as identified by <code>part</code>, or
             * null if a valid paragraph/sentence can't be found. The offset
             * will point to the start of the word/sentence in the array, and
             * the modelOffset will point to the location of the word/sentence
             * in the model.
             */
            private IndexedSegment getSegmentAt(int part, int index)
                throws BadLocationException {

                IndexedSegment seg = getParagraphElementText(index);
                if (seg == null) {
                    return null;
                }
                BreakIterator iterator;
                switch (part) {
                case AccessibleText.WORD:
                    iterator = BreakIterator.getWordInstance(getLocale());
                    break;
                case AccessibleText.SENTENCE:
                    iterator = BreakIterator.getSentenceInstance(getLocale());
                    break;
                default:
                    return null;
                }
                seg.first();
                iterator.setText(seg);
                int end = iterator.following(index - seg.modelOffset + seg.offset);
                if (end == BreakIterator.DONE) {
                    return null;
                }
                if (end > seg.offset + seg.count) {
                    return null;
                }
                int begin = iterator.previous();
                if (begin == BreakIterator.DONE ||
                    begin >= seg.offset + seg.count) {
                    return null;
                }
                seg.modelOffset = seg.modelOffset + begin - seg.offset;
                seg.offset = begin;
                seg.count = end - begin;
                return seg;
            }

            /**
             * Return the AttributeSet for a given character at a given index
             *
             * @param i the zero-based index into the text
             * @return the AttributeSet of the character
             */
            public AttributeSet getCharacterAttribute(int i) {
                if (model instanceof StyledDocument) {
                    StyledDocument doc = (StyledDocument)model;
                    Element elem = doc.getCharacterElement(i);
                    if (elem != null) {
                        return elem.getAttributes();
                    }
                }
                return null;
            }

            /**
             * Returns the start offset within the selected text.
             * If there is no selection, but there is
             * a caret, the start and end offsets will be the same.
             *
             * @return the index into the text of the start of the selection
             */
            public int getSelectionStart() {
                return editor.getSelectionStart();
            }

            /**
             * Returns the end offset within the selected text.
             * If there is no selection, but there is
             * a caret, the start and end offsets will be the same.
             *
             * @return the index into the text of the end of the selection
             */
            public int getSelectionEnd() {
                return editor.getSelectionEnd();
            }

            /**
             * Returns the portion of the text that is selected.
             *
             * @return the String portion of the text that is selected
             */
            public String getSelectedText() {
                return editor.getSelectedText();
            }

            /*
             * Returns the text substring starting at the specified
             * offset with the specified length.
             */
            private String getText(int offset, int length)
                throws BadLocationException {

                if (model != null && model instanceof StyledDocument) {
                    StyledDocument doc = (StyledDocument)model;
                    return model.getText(offset, length);
                } else {
                    return null;
                }
            }
        }
    }

    /*
     * ElementInfo for images
     */
    private class IconElementInfo extends ElementInfo implements Accessible {

        private int width = -1;
        private int height = -1;

        IconElementInfo(Element element, ElementInfo parent) {
            super(element, parent);
        }

        protected void invalidate(boolean first) {
            super.invalidate(first);
            width = height = -1;
        }

        private int getImageSize(Object key) {
            if (validateIfNecessary()) {
                int size = getIntAttr(getAttributes(), key, -1);

                if (size == -1) {
                    View v = getView();

                    size = 0;
                    if (v instanceof ImageView) {
                        Image img = ((ImageView)v).getImage();
                        if (img != null) {
                            if (key == HTML.Attribute.WIDTH) {
                                size = img.getWidth(null);
                            }
                            else {
                                size = img.getHeight(null);
                            }
                        }
                    }
                }
                return size;
            }
            return 0;
        }

        // begin AccessibleIcon implementation ...
        private AccessibleContext accessibleContext;

        public AccessibleContext getAccessibleContext() {
            if (accessibleContext == null) {
                accessibleContext = new IconAccessibleContext(this);
            }
            return accessibleContext;
        }

        /*
         * AccessibleContext for images
         */
        protected class IconAccessibleContext extends HTMLAccessibleContext
            implements AccessibleIcon  {

            public IconAccessibleContext(ElementInfo elementInfo) {
                super(elementInfo);
            }

            /**
             * Gets the accessibleName property of this object.  The accessibleName
             * property of an object is a localized String that designates the purpose
             * of the object.  For example, the accessibleName property of a label
             * or button might be the text of the label or button itself.  In the
             * case of an object that doesn't display its name, the accessibleName
             * should still be set.  For example, in the case of a text field used
             * to enter the name of a city, the accessibleName for the en_US locale
             * could be 'city.'
             *
             * @return the localized name of the object; null if this
             * object does not have a name
             *
             * @see #setAccessibleName
             */
            public String getAccessibleName() {
                return getAccessibleIconDescription();
            }

            /**
             * Gets the accessibleDescription property of this object.  If this
             * property isn't set, returns the content type of this
             * <code>JEditorPane</code> instead (e.g. "plain/text", "html/text").
             *
             * @return the localized description of the object; <code>null</code>
             *  if this object does not have a description
             *
             * @see #setAccessibleName
             */
            public String getAccessibleDescription() {
                return editor.getContentType();
            }

            /**
             * Gets the role of this object.  The role of the object is the generic
             * purpose or use of the class of this object.  For example, the role
             * of a push button is AccessibleRole.PUSH_BUTTON.  The roles in
             * AccessibleRole are provided so component developers can pick from
             * a set of predefined roles.  This enables assistive technologies to
             * provide a consistent interface to various tweaked subclasses of
             * components (e.g., use AccessibleRole.PUSH_BUTTON for all components
             * that act like a push button) as well as distinguish between subclasses
             * that behave differently (e.g., AccessibleRole.CHECK_BOX for check boxes
             * and AccessibleRole.RADIO_BUTTON for radio buttons).
             * <p>Note that the AccessibleRole class is also extensible, so
             * custom component developers can define their own AccessibleRole's
             * if the set of predefined roles is inadequate.
             *
             * @return an instance of AccessibleRole describing the role of the object
             * @see AccessibleRole
             */
            public AccessibleRole getAccessibleRole() {
                return AccessibleRole.ICON;
            }

            public AccessibleIcon [] getAccessibleIcon() {
                AccessibleIcon [] icons = new AccessibleIcon[1];
                icons[0] = this;
                return icons;
            }

            /**
             * Gets the description of the icon.  This is meant to be a brief
             * textual description of the object.  For example, it might be
             * presented to a blind user to give an indication of the purpose
             * of the icon.
             *
             * @return the description of the icon
             */
            public String getAccessibleIconDescription() {
                return ((ImageView)getView()).getAltText();
            }

            /**
             * Sets the description of the icon.  This is meant to be a brief
             * textual description of the object.  For example, it might be
             * presented to a blind user to give an indication of the purpose
             * of the icon.
             *
             * @param description the description of the icon
             */
            public void setAccessibleIconDescription(String description) {
            }

            /**
             * Gets the width of the icon
             *
             * @return the width of the icon.
             */
            public int getAccessibleIconWidth() {
                if (width == -1) {
                    width = getImageSize(HTML.Attribute.WIDTH);
                }
                return width;
            }

            /**
             * Gets the height of the icon
             *
             * @return the height of the icon.
             */
            public int getAccessibleIconHeight() {
                if (height == -1) {
                    height = getImageSize(HTML.Attribute.HEIGHT);
                }
                return height;
            }
        }
        // ... end AccessibleIconImplementation
    }


    /**
     * TableElementInfo encapsulates information about a HTML.Tag.TABLE.
     * To make access fast it crates a grid containing the children to
     * allow for access by row, column. TableElementInfo will contain
     * TableRowElementInfos, which will contain TableCellElementInfos.
     * Any time one of the rows or columns becomes invalid the table is
     * invalidated.  This is because any time one of the child attributes
     * changes the size of the grid may have changed.
     */
    private class TableElementInfo extends ElementInfo
        implements Accessible {

        protected ElementInfo caption;

        /**
         * Allocation of the table by row x column. There may be holes (eg
         * nulls) depending upon the html, any cell that has a rowspan/colspan
         * > 1 will be contained multiple times in the grid.
         */
        private TableCellElementInfo[][] grid;


        TableElementInfo(Element e, ElementInfo parent) {
            super(e, parent);
        }

        public ElementInfo getCaptionInfo() {
            return caption;
        }

        /**
         * Overriden to update the grid when validating.
         */
        protected void validate() {
            super.validate();
            updateGrid();
        }

        /**
         * Overriden to only alloc instances of TableRowElementInfos.
         */
        protected void loadChildren(Element e) {

            for (int counter = 0; counter < e.getElementCount(); counter++) {
                Element child = e.getElement(counter);
                AttributeSet attrs = child.getAttributes();

                if (attrs.getAttribute(StyleConstants.NameAttribute) ==
                                       HTML.Tag.TR) {
                    addChild(new TableRowElementInfo(child, this, counter));

                } else if (attrs.getAttribute(StyleConstants.NameAttribute) ==
                                       HTML.Tag.CAPTION) {
                    // Handle captions as a special case since all other
                    // children are table rows.
                    caption = createElementInfo(child, this);
                }
            }
        }

        /**
         * Updates the grid.
         */
        private void updateGrid() {
            // Determine the max row/col count.
            int delta = 0;
            int maxCols = 0;
            int rows;
            for (int counter = 0; counter < getChildCount(); counter++) {
                TableRowElementInfo row = getRow(counter);
                int prev = 0;
                for (int y = 0; y < delta; y++) {
                    prev = Math.max(prev, getRow(counter - y - 1).
                                    getColumnCount(y + 2));
                }
                delta = Math.max(row.getRowCount(), delta);
                delta--;
                maxCols = Math.max(maxCols, row.getColumnCount() + prev);
            }
            rows = getChildCount() + delta;

            // Alloc
            grid = new TableCellElementInfo[rows][];
            for (int counter = 0; counter < rows; counter++) {
                grid[counter] = new TableCellElementInfo[maxCols];
            }
            // Update
            for (int counter = 0; counter < rows; counter++) {
                getRow(counter).updateGrid(counter);
            }
        }

        /**
         * Returns the TableCellElementInfo at the specified index.
         */
        public TableRowElementInfo getRow(int index) {
            return (TableRowElementInfo)getChild(index);
        }

        /**
         * Returns the TableCellElementInfo by row and column.
         */
        public TableCellElementInfo getCell(int r, int c) {
            if (validateIfNecessary() && r < grid.length &&
                                         c < grid[0].length) {
                return grid[r][c];
            }
            return null;
        }

        /**
         * Returns the rowspan of the specified entry.
         */
        public int getRowExtentAt(int r, int c) {
            TableCellElementInfo cell = getCell(r, c);

            if (cell != null) {
                int rows = cell.getRowCount();
                int delta = 1;

                while ((r - delta) >= 0 && grid[r - delta][c] == cell) {
                    delta++;
                }
                return rows - delta + 1;
            }
            return 0;
        }

        /**
         * Returns the colspan of the specified entry.
         */
        public int getColumnExtentAt(int r, int c) {
            TableCellElementInfo cell = getCell(r, c);

            if (cell != null) {
                int cols = cell.getColumnCount();
                int delta = 1;

                while ((c - delta) >= 0 && grid[r][c - delta] == cell) {
                    delta++;
                }
                return cols - delta + 1;
            }
            return 0;
        }

        /**
         * Returns the number of rows in the table.
         */
        public int getRowCount() {
            if (validateIfNecessary()) {
                return grid.length;
            }
            return 0;
        }

        /**
         * Returns the number of columns in the table.
         */
        public int getColumnCount() {
            if (validateIfNecessary() && grid.length > 0) {
                return grid[0].length;
            }
            return 0;
        }

        // begin AccessibleTable implementation ...
        private AccessibleContext accessibleContext;

        public AccessibleContext getAccessibleContext() {
            if (accessibleContext == null) {
                accessibleContext = new TableAccessibleContext(this);
            }
            return accessibleContext;
        }

        /*
         * AccessibleContext for tables
         */
        public class TableAccessibleContext extends HTMLAccessibleContext
            implements AccessibleTable {

            private AccessibleHeadersTable rowHeadersTable;

            public TableAccessibleContext(ElementInfo elementInfo) {
                super(elementInfo);
            }

            /**
             * Gets the accessibleName property of this object.  The accessibleName
             * property of an object is a localized String that designates the purpose
             * of the object.  For example, the accessibleName property of a label
             * or button might be the text of the label or button itself.  In the
             * case of an object that doesn't display its name, the accessibleName
             * should still be set.  For example, in the case of a text field used
             * to enter the name of a city, the accessibleName for the en_US locale
             * could be 'city.'
             *
             * @return the localized name of the object; null if this
             * object does not have a name
             *
             * @see #setAccessibleName
             */
            public String getAccessibleName() {
                // return the role of the object
                return getAccessibleRole().toString();
            }

            /**
             * Gets the accessibleDescription property of this object.  If this
             * property isn't set, returns the content type of this
             * <code>JEditorPane</code> instead (e.g. "plain/text", "html/text").
             *
             * @return the localized description of the object; <code>null</code>
             *  if this object does not have a description
             *
             * @see #setAccessibleName
             */
            public String getAccessibleDescription() {
                return editor.getContentType();
            }

            /**
             * Gets the role of this object.  The role of the object is the generic
             * purpose or use of the class of this object.  For example, the role
             * of a push button is AccessibleRole.PUSH_BUTTON.  The roles in
             * AccessibleRole are provided so component developers can pick from
             * a set of predefined roles.  This enables assistive technologies to
             * provide a consistent interface to various tweaked subclasses of
             * components (e.g., use AccessibleRole.PUSH_BUTTON for all components
             * that act like a push button) as well as distinguish between subclasses
             * that behave differently (e.g., AccessibleRole.CHECK_BOX for check boxes
             * and AccessibleRole.RADIO_BUTTON for radio buttons).
             * <p>Note that the AccessibleRole class is also extensible, so
             * custom component developers can define their own AccessibleRole's
             * if the set of predefined roles is inadequate.
             *
             * @return an instance of AccessibleRole describing the role of the object
             * @see AccessibleRole
             */
            public AccessibleRole getAccessibleRole() {
                return AccessibleRole.TABLE;
            }

            /**
             * Gets the 0-based index of this object in its accessible parent.
             *
             * @return the 0-based index of this object in its parent; -1 if this
             * object does not have an accessible parent.
             *
             * @see #getAccessibleParent
             * @see #getAccessibleChildrenCount
             * @gsee #getAccessibleChild
             */
            public int getAccessibleIndexInParent() {
                return elementInfo.getIndexInParent();
            }

            /**
             * Returns the number of accessible children of the object.
             *
             * @return the number of accessible children of the object.
             */
            public int getAccessibleChildrenCount() {
                return ((TableElementInfo)elementInfo).getRowCount() *
                    ((TableElementInfo)elementInfo).getColumnCount();
            }

            /**
             * Returns the specified Accessible child of the object.  The Accessible
             * children of an Accessible object are zero-based, so the first child
             * of an Accessible child is at index 0, the second child is at index 1,
             * and so on.
             *
             * @param i zero-based index of child
             * @return the Accessible child of the object
             * @see #getAccessibleChildrenCount
             */
            public Accessible getAccessibleChild(int i) {
                int rowCount = ((TableElementInfo)elementInfo).getRowCount();
                int columnCount = ((TableElementInfo)elementInfo).getColumnCount();
                int r = i / rowCount;
                int c = i % columnCount;
                if (r < 0 || r >= rowCount || c < 0 || c >= columnCount) {
                    return null;
                } else {
                    return getAccessibleAt(r, c);
                }
            }

            public AccessibleTable getAccessibleTable() {
                return this;
            }

            /**
             * Returns the caption for the table.
             *
             * @return the caption for the table
             */
            public Accessible getAccessibleCaption() {
                ElementInfo captionInfo = getCaptionInfo();
                if (captionInfo instanceof Accessible) {
                    return (Accessible)caption;
                } else {
                    return null;
                }
            }

            /**
             * Sets the caption for the table.
             *
             * @param a the caption for the table
             */
            public void setAccessibleCaption(Accessible a) {
            }

            /**
             * Returns the summary description of the table.
             *
             * @return the summary description of the table
             */
            public Accessible getAccessibleSummary() {
                return null;
            }

            /**
             * Sets the summary description of the table
             *
             * @param a the summary description of the table
             */
            public void setAccessibleSummary(Accessible a) {
            }

            /**
             * Returns the number of rows in the table.
             *
             * @return the number of rows in the table
             */
            public int getAccessibleRowCount() {
                return ((TableElementInfo)elementInfo).getRowCount();
            }

            /**
             * Returns the number of columns in the table.
             *
             * @return the number of columns in the table
             */
            public int getAccessibleColumnCount() {
                return ((TableElementInfo)elementInfo).getColumnCount();
            }

            /**
             * Returns the Accessible at a specified row and column
             * in the table.
             *
             * @param r zero-based row of the table
             * @param c zero-based column of the table
             * @return the Accessible at the specified row and column
             */
            public Accessible getAccessibleAt(int r, int c) {
                TableCellElementInfo cellInfo = getCell(r, c);
                if (cellInfo != null) {
                    return cellInfo.getAccessible();
                } else {
                    return null;
                }
            }

            /**
             * Returns the number of rows occupied by the Accessible at
             * a specified row and column in the table.
             *
             * @return the number of rows occupied by the Accessible at a
             * given specified (row, column)
             */
            public int getAccessibleRowExtentAt(int r, int c) {
                return ((TableElementInfo)elementInfo).getRowExtentAt(r, c);
            }

            /**
             * Returns the number of columns occupied by the Accessible at
             * a specified row and column in the table.
             *
             * @return the number of columns occupied by the Accessible at a
             * given specified row and column
             */
            public int getAccessibleColumnExtentAt(int r, int c) {
                return ((TableElementInfo)elementInfo).getColumnExtentAt(r, c);
            }

            /**
             * Returns the row headers as an AccessibleTable.
             *
             * @return an AccessibleTable representing the row
             * headers
             */
            public AccessibleTable getAccessibleRowHeader() {
                return rowHeadersTable;
            }

            /**
             * Sets the row headers.
             *
             * @param table an AccessibleTable representing the
             * row headers
             */
            public void setAccessibleRowHeader(AccessibleTable table) {
            }

            /**
             * Returns the column headers as an AccessibleTable.
             *
             * @return an AccessibleTable representing the column
             * headers
             */
            public AccessibleTable getAccessibleColumnHeader() {
                return null;
            }

            /**
             * Sets the column headers.
             *
             * @param table an AccessibleTable representing the
             * column headers
             */
            public void setAccessibleColumnHeader(AccessibleTable table) {
            }

            /**
             * Returns the description of the specified row in the table.
             *
             * @param r zero-based row of the table
             * @return the description of the row
             */
            public Accessible getAccessibleRowDescription(int r) {
                return null;
            }

            /**
             * Sets the description text of the specified row of the table.
             *
             * @param r zero-based row of the table
             * @param a the description of the row
             */
            public void setAccessibleRowDescription(int r, Accessible a) {
            }

            /**
             * Returns the description text of the specified column in the table.
             *
             * @param c zero-based column of the table
             * @return the text description of the column
             */
            public Accessible getAccessibleColumnDescription(int c) {
                return null;
            }

            /**
             * Sets the description text of the specified column in the table.
             *
             * @param c zero-based column of the table
             * @param a the text description of the column
             */
            public void setAccessibleColumnDescription(int c, Accessible a) {
            }

            /**
             * Returns a boolean value indicating whether the accessible at
             * a specified row and column is selected.
             *
             * @param r zero-based row of the table
             * @param c zero-based column of the table
             * @return the boolean value true if the accessible at the
             * row and column is selected. Otherwise, the boolean value
             * false
             */
            public boolean isAccessibleSelected(int r, int c) {
                if (validateIfNecessary()) {
                    if (r < 0 || r >= getAccessibleRowCount() ||
                        c < 0 || c >= getAccessibleColumnCount()) {
                        return false;
                    }
                    TableCellElementInfo cell = getCell(r, c);
                    if (cell != null) {
                        Element elem = cell.getElement();
                        int start = elem.getStartOffset();
                        int end = elem.getEndOffset();
                        return start >= editor.getSelectionStart() &&
                            end <= editor.getSelectionEnd();
                    }
                }
                return false;
            }

            /**
             * Returns a boolean value indicating whether the specified row
             * is selected.
             *
             * @param r zero-based row of the table
             * @return the boolean value true if the specified row is selected.
             * Otherwise, false.
             */
            public boolean isAccessibleRowSelected(int r) {
                if (validateIfNecessary()) {
                    if (r < 0 || r >= getAccessibleRowCount()) {
                        return false;
                    }
                    int nColumns = getAccessibleColumnCount();

                    TableCellElementInfo startCell = getCell(r, 0);
                    if (startCell == null) {
                        return false;
                    }
                    int start = startCell.getElement().getStartOffset();

                    TableCellElementInfo endCell = getCell(r, nColumns-1);
                    if (endCell == null) {
                        return false;
                    }
                    int end = endCell.getElement().getEndOffset();

                    return start >= editor.getSelectionStart() &&
                        end <= editor.getSelectionEnd();
                }
                return false;
            }

            /**
             * Returns a boolean value indicating whether the specified column
             * is selected.
             *
             * @param c zero-based column of the table
             * @return the boolean value true if the specified column is selected.
             * Otherwise, false.
             */
            public boolean isAccessibleColumnSelected(int c) {
                if (validateIfNecessary()) {
                    if (c < 0 || c >= getAccessibleColumnCount()) {
                        return false;
                    }
                    int nRows = getAccessibleRowCount();

                    TableCellElementInfo startCell = getCell(0, c);
                    if (startCell == null) {
                        return false;
                    }
                    int start = startCell.getElement().getStartOffset();

                    TableCellElementInfo endCell = getCell(nRows-1, c);
                    if (endCell == null) {
                        return false;
                    }
                    int end = endCell.getElement().getEndOffset();
                    return start >= editor.getSelectionStart() &&
                        end <= editor.getSelectionEnd();
                }
                return false;
            }

            /**
             * Returns the selected rows in a table.
             *
             * @return an array of selected rows where each element is a
             * zero-based row of the table
             */
            public int [] getSelectedAccessibleRows() {
                if (validateIfNecessary()) {
                    int nRows = getAccessibleRowCount();
                    Vector<Integer> vec = new Vector<Integer>();

                    for (int i = 0; i < nRows; i++) {
                        if (isAccessibleRowSelected(i)) {
                            vec.addElement(Integer.valueOf(i));
                        }
                    }
                    int[] retval = new int[vec.size()];
                    for (int i = 0; i < retval.length; i++) {
                        retval[i] = vec.elementAt(i).intValue();
                    }
                    return retval;
                }
                return new int[0];
            }

            /**
             * Returns the selected columns in a table.
             *
             * @return an array of selected columns where each element is a
             * zero-based column of the table
             */
            public int [] getSelectedAccessibleColumns() {
                if (validateIfNecessary()) {
                    int nColumns = getAccessibleRowCount();
                    Vector<Integer> vec = new Vector<Integer>();

                    for (int i = 0; i < nColumns; i++) {
                        if (isAccessibleColumnSelected(i)) {
                            vec.addElement(Integer.valueOf(i));
                        }
                    }
                    int[] retval = new int[vec.size()];
                    for (int i = 0; i < retval.length; i++) {
                        retval[i] = vec.elementAt(i).intValue();
                    }
                    return retval;
                }
                return new int[0];
            }

            // begin AccessibleExtendedTable implementation -------------

            /**
             * Returns the row number of an index in the table.
             *
             * @param index the zero-based index in the table
             * @return the zero-based row of the table if one exists;
             * otherwise -1.
             */
            public int getAccessibleRow(int index) {
                if (validateIfNecessary()) {
                    int numCells = getAccessibleColumnCount() *
                        getAccessibleRowCount();
                    if (index >= numCells) {
                        return -1;
                    } else {
                        return index / getAccessibleColumnCount();
                    }
                }
                return -1;
            }

            /**
             * Returns the column number of an index in the table.
             *
             * @param index the zero-based index in the table
             * @return the zero-based column of the table if one exists;
             * otherwise -1.
             */
            public int getAccessibleColumn(int index) {
                if (validateIfNecessary()) {
                    int numCells = getAccessibleColumnCount() *
                        getAccessibleRowCount();
                    if (index >= numCells) {
                        return -1;
                    } else {
                        return index % getAccessibleColumnCount();
                    }
                }
                return -1;
            }

            /**
             * Returns the index at a row and column in the table.
             *
             * @param r zero-based row of the table
             * @param c zero-based column of the table
             * @return the zero-based index in the table if one exists;
             * otherwise -1.
             */
            public int getAccessibleIndex(int r, int c) {
                if (validateIfNecessary()) {
                    if (r >= getAccessibleRowCount() ||
                        c >= getAccessibleColumnCount()) {
                        return -1;
                    } else {
                        return r * getAccessibleColumnCount() + c;
                    }
                }
                return -1;
            }

            /**
             * Returns the row header at a row in a table.
             * @param r zero-based row of the table
             *
             * @return a String representing the row header
             * if one exists; otherwise null.
             */
            public String getAccessibleRowHeader(int r) {
                if (validateIfNecessary()) {
                    TableCellElementInfo cellInfo = getCell(r, 0);
                    if (cellInfo.isHeaderCell()) {
                        View v = cellInfo.getView();
                        if (v != null && model != null) {
                            try {
                                return model.getText(v.getStartOffset(),
                                                     v.getEndOffset() -
                                                     v.getStartOffset());
                            } catch (BadLocationException e) {
                                return null;
                            }
                        }
                    }
                }
                return null;
            }

            /**
             * Returns the column header at a column in a table.
             * @param c zero-based column of the table
             *
             * @return a String representing the column header
             * if one exists; otherwise null.
             */
            public String getAccessibleColumnHeader(int c) {
                if (validateIfNecessary()) {
                    TableCellElementInfo cellInfo = getCell(0, c);
                    if (cellInfo.isHeaderCell()) {
                        View v = cellInfo.getView();
                        if (v != null && model != null) {
                            try {
                                return model.getText(v.getStartOffset(),
                                                     v.getEndOffset() -
                                                     v.getStartOffset());
                            } catch (BadLocationException e) {
                                return null;
                            }
                        }
                    }
                }
                return null;
            }

            public void addRowHeader(TableCellElementInfo cellInfo, int rowNumber) {
                if (rowHeadersTable == null) {
                    rowHeadersTable = new AccessibleHeadersTable();
                }
                rowHeadersTable.addHeader(cellInfo, rowNumber);
            }
            // end of AccessibleExtendedTable implementation ------------

            protected class AccessibleHeadersTable implements AccessibleTable {

                // Header information is modeled as a Hashtable of
                // ArrayLists where each Hashtable entry represents
                // a row containing one or more headers.
                private Hashtable<Integer, ArrayList<TableCellElementInfo>> headers =
                        new Hashtable<Integer, ArrayList<TableCellElementInfo>>();
                private int rowCount = 0;
                private int columnCount = 0;

                public void addHeader(TableCellElementInfo cellInfo, int rowNumber) {
                    Integer rowInteger = Integer.valueOf(rowNumber);
                    ArrayList<TableCellElementInfo> list = headers.get(rowInteger);
                    if (list == null) {
                        list = new ArrayList<TableCellElementInfo>();
                        headers.put(rowInteger, list);
                    }
                    list.add(cellInfo);
                }

                /**
                 * Returns the caption for the table.
                 *
                 * @return the caption for the table
                 */
                public Accessible getAccessibleCaption() {
                    return null;
                }

                /**
                 * Sets the caption for the table.
                 *
                 * @param a the caption for the table
                 */
                public void setAccessibleCaption(Accessible a) {
                }

                /**
                 * Returns the summary description of the table.
                 *
                 * @return the summary description of the table
                 */
                public Accessible getAccessibleSummary() {
                    return null;
                }

                /**
                 * Sets the summary description of the table
                 *
                 * @param a the summary description of the table
                 */
                public void setAccessibleSummary(Accessible a) {
                }

                /**
                 * Returns the number of rows in the table.
                 *
                 * @return the number of rows in the table
                 */
                public int getAccessibleRowCount() {
                    return rowCount;
                }

                /**
                 * Returns the number of columns in the table.
                 *
                 * @return the number of columns in the table
                 */
                public int getAccessibleColumnCount() {
                    return columnCount;
                }

                private TableCellElementInfo getElementInfoAt(int r, int c) {
                    ArrayList<TableCellElementInfo> list = headers.get(Integer.valueOf(r));
                    if (list != null) {
                        return list.get(c);
                    } else {
                        return null;
                    }
                }

                /**
                 * Returns the Accessible at a specified row and column
                 * in the table.
                 *
                 * @param r zero-based row of the table
                 * @param c zero-based column of the table
                 * @return the Accessible at the specified row and column
                 */
                public Accessible getAccessibleAt(int r, int c) {
                    ElementInfo elementInfo = getElementInfoAt(r, c);
                    if (elementInfo instanceof Accessible) {
                        return (Accessible)elementInfo;
                    } else {
                        return null;
                    }
                }

                /**
                 * Returns the number of rows occupied by the Accessible at
                 * a specified row and column in the table.
                 *
                 * @return the number of rows occupied by the Accessible at a
                 * given specified (row, column)
                 */
                public int getAccessibleRowExtentAt(int r, int c) {
                    TableCellElementInfo elementInfo = getElementInfoAt(r, c);
                    if (elementInfo != null) {
                        return elementInfo.getRowCount();
                    } else {
                        return 0;
                    }
                }

                /**
                 * Returns the number of columns occupied by the Accessible at
                 * a specified row and column in the table.
                 *
                 * @return the number of columns occupied by the Accessible at a
                 * given specified row and column
                 */
                public int getAccessibleColumnExtentAt(int r, int c) {
                    TableCellElementInfo elementInfo = getElementInfoAt(r, c);
                    if (elementInfo != null) {
                        return elementInfo.getRowCount();
                    } else {
                        return 0;
                    }
                }

                /**
                 * Returns the row headers as an AccessibleTable.
                 *
                 * @return an AccessibleTable representing the row
                 * headers
                 */
                public AccessibleTable getAccessibleRowHeader() {
                    return null;
                }

                /**
                 * Sets the row headers.
                 *
                 * @param table an AccessibleTable representing the
                 * row headers
                 */
                public void setAccessibleRowHeader(AccessibleTable table) {
                }

                /**
                 * Returns the column headers as an AccessibleTable.
                 *
                 * @return an AccessibleTable representing the column
                 * headers
                 */
                public AccessibleTable getAccessibleColumnHeader() {
                    return null;
                }

                /**
                 * Sets the column headers.
                 *
                 * @param table an AccessibleTable representing the
                 * column headers
                 */
                public void setAccessibleColumnHeader(AccessibleTable table) {
                }

                /**
                 * Returns the description of the specified row in the table.
                 *
                 * @param r zero-based row of the table
                 * @return the description of the row
                 */
                public Accessible getAccessibleRowDescription(int r) {
                    return null;
                }

                /**
                 * Sets the description text of the specified row of the table.
                 *
                 * @param r zero-based row of the table
                 * @param a the description of the row
                 */
                public void setAccessibleRowDescription(int r, Accessible a) {
                }

                /**
                 * Returns the description text of the specified column in the table.
                 *
                 * @param c zero-based column of the table
                 * @return the text description of the column
                 */
                public Accessible getAccessibleColumnDescription(int c) {
                    return null;
                }

                /**
                 * Sets the description text of the specified column in the table.
                 *
                 * @param c zero-based column of the table
                 * @param a the text description of the column
                 */
                public void setAccessibleColumnDescription(int c, Accessible a) {
                }

                /**
                 * Returns a boolean value indicating whether the accessible at
                 * a specified row and column is selected.
                 *
                 * @param r zero-based row of the table
                 * @param c zero-based column of the table
                 * @return the boolean value true if the accessible at the
                 * row and column is selected. Otherwise, the boolean value
                 * false
                 */
                public boolean isAccessibleSelected(int r, int c) {
                    return false;
                }

                /**
                 * Returns a boolean value indicating whether the specified row
                 * is selected.
                 *
                 * @param r zero-based row of the table
                 * @return the boolean value true if the specified row is selected.
                 * Otherwise, false.
                 */
                public boolean isAccessibleRowSelected(int r) {
                    return false;
                }

                /**
                 * Returns a boolean value indicating whether the specified column
                 * is selected.
                 *
                 * @param c zero-based column of the table
                 * @return the boolean value true if the specified column is selected.
                 * Otherwise, false.
                 */
                public boolean isAccessibleColumnSelected(int c) {
                    return false;
                }

                /**
                 * Returns the selected rows in a table.
                 *
                 * @return an array of selected rows where each element is a
                 * zero-based row of the table
                 */
                public int [] getSelectedAccessibleRows() {
                    return new int [0];
                }

                /**
                 * Returns the selected columns in a table.
                 *
                 * @return an array of selected columns where each element is a
                 * zero-based column of the table
                 */
                public int [] getSelectedAccessibleColumns() {
                    return new int [0];
                }
            }
        } // ... end AccessibleHeadersTable

        /*
         * ElementInfo for table rows
         */
        private class TableRowElementInfo extends ElementInfo {

            private TableElementInfo parent;
            private int rowNumber;

            TableRowElementInfo(Element e, TableElementInfo parent, int rowNumber) {
                super(e, parent);
                this.parent = parent;
                this.rowNumber = rowNumber;
            }

            protected void loadChildren(Element e) {
                for (int x = 0; x < e.getElementCount(); x++) {
                    AttributeSet attrs = e.getElement(x).getAttributes();

                    if (attrs.getAttribute(StyleConstants.NameAttribute) ==
                            HTML.Tag.TH) {
                        TableCellElementInfo headerElementInfo =
                            new TableCellElementInfo(e.getElement(x), this, true);
                        addChild(headerElementInfo);

                        AccessibleTable at =
                            parent.getAccessibleContext().getAccessibleTable();
                        TableAccessibleContext tableElement =
                            (TableAccessibleContext)at;
                        tableElement.addRowHeader(headerElementInfo, rowNumber);

                    } else if (attrs.getAttribute(StyleConstants.NameAttribute) ==
                            HTML.Tag.TD) {
                        addChild(new TableCellElementInfo(e.getElement(x), this,
                                                          false));
                    }
                }
            }

            /**
             * Returns the max of the rowspans of the cells in this row.
             */
            public int getRowCount() {
                int rowCount = 1;
                if (validateIfNecessary()) {
                    for (int counter = 0; counter < getChildCount();
                         counter++) {

                        TableCellElementInfo cell = (TableCellElementInfo)
                                                    getChild(counter);

                        if (cell.validateIfNecessary()) {
                            rowCount = Math.max(rowCount, cell.getRowCount());
                        }
                    }
                }
                return rowCount;
            }

            /**
             * Returns the sum of the column spans of the individual
             * cells in this row.
             */
            public int getColumnCount() {
                int colCount = 0;
                if (validateIfNecessary()) {
                    for (int counter = 0; counter < getChildCount();
                         counter++) {
                        TableCellElementInfo cell = (TableCellElementInfo)
                                                    getChild(counter);

                        if (cell.validateIfNecessary()) {
                            colCount += cell.getColumnCount();
                        }
                    }
                }
                return colCount;
            }

            /**
             * Overriden to invalidate the table as well as
             * TableRowElementInfo.
             */
            protected void invalidate(boolean first) {
                super.invalidate(first);
                getParent().invalidate(true);
            }

            /**
             * Places the TableCellElementInfos for this element in
             * the grid.
             */
            private void updateGrid(int row) {
                if (validateIfNecessary()) {
                    boolean emptyRow = false;

                    while (!emptyRow) {
                        for (int counter = 0; counter < grid[row].length;
                                 counter++) {
                            if (grid[row][counter] == null) {
                                emptyRow = true;
                                break;
                            }
                        }
                        if (!emptyRow) {
                            row++;
                        }
                    }
                    for (int col = 0, counter = 0; counter < getChildCount();
                             counter++) {
                        TableCellElementInfo cell = (TableCellElementInfo)
                                                    getChild(counter);

                        while (grid[row][col] != null) {
                            col++;
                        }
                        for (int rowCount = cell.getRowCount() - 1;
                             rowCount >= 0; rowCount--) {
                            for (int colCount = cell.getColumnCount() - 1;
                                 colCount >= 0; colCount--) {
                                grid[row + rowCount][col + colCount] = cell;
                            }
                        }
                        col += cell.getColumnCount();
                    }
                }
            }

            /**
             * Returns the column count of the number of columns that have
             * a rowcount >= rowspan.
             */
            private int getColumnCount(int rowspan) {
                if (validateIfNecessary()) {
                    int cols = 0;
                    for (int counter = 0; counter < getChildCount();
                         counter++) {
                        TableCellElementInfo cell = (TableCellElementInfo)
                                                    getChild(counter);

                        if (cell.getRowCount() >= rowspan) {
                            cols += cell.getColumnCount();
                        }
                    }
                    return cols;
                }
                return 0;
            }
        }

        /**
         * TableCellElementInfo is used to represents the cells of
         * the table.
         */
        private class TableCellElementInfo extends ElementInfo {

            private Accessible accessible;
            private boolean isHeaderCell;

            TableCellElementInfo(Element e, ElementInfo parent) {
                super(e, parent);
                this.isHeaderCell = false;
            }

            TableCellElementInfo(Element e, ElementInfo parent,
                                 boolean isHeaderCell) {
                super(e, parent);
                this.isHeaderCell = isHeaderCell;
            }

            /*
             * Returns whether this table cell is a header
             */
            public boolean isHeaderCell() {
                return this.isHeaderCell;
            }

            /*
             * Returns the Accessible representing this table cell
             */
            public Accessible getAccessible() {
                accessible = null;
                getAccessible(this);
                return accessible;
            }

            /*
             * Gets the outermost Accessible in the table cell
             */
            private void getAccessible(ElementInfo elementInfo) {
                if (elementInfo instanceof Accessible) {
                    accessible = (Accessible)elementInfo;
                } else {
                    for (int i = 0; i < elementInfo.getChildCount(); i++) {
                        getAccessible(elementInfo.getChild(i));
                    }
                }
            }

            /**
             * Returns the rowspan attribute.
             */
            public int getRowCount() {
                if (validateIfNecessary()) {
                    return Math.max(1, getIntAttr(getAttributes(),
                                                  HTML.Attribute.ROWSPAN, 1));
                }
                return 0;
            }

            /**
             * Returns the colspan attribute.
             */
            public int getColumnCount() {
                if (validateIfNecessary()) {
                    return Math.max(1, getIntAttr(getAttributes(),
                                                  HTML.Attribute.COLSPAN, 1));
                }
                return 0;
            }

            /**
             * Overriden to invalidate the TableRowElementInfo as well as
             * the TableCellElementInfo.
             */
            protected void invalidate(boolean first) {
                super.invalidate(first);
                getParent().invalidate(true);
            }
        }
    }


    /**
     * ElementInfo provides a slim down view of an Element.  Each ElementInfo
     * can have any number of child ElementInfos that are not necessarily
     * direct children of the Element. As the Document changes various
     * ElementInfos become invalidated. Before accessing a particular portion
     * of an ElementInfo you should make sure it is valid by invoking
     * <code>validateIfNecessary</code>, this will return true if
     * successful, on the other hand a false return value indicates the
     * ElementInfo is not valid and can never become valid again (usually
     * the result of the Element the ElementInfo encapsulates being removed).
     */
    private class ElementInfo {

        /**
         * The children of this ElementInfo.
         */
        private ArrayList<ElementInfo> children;
        /**
         * The Element this ElementInfo is providing information for.
         */
        private Element element;
        /**
         * The parent ElementInfo, will be null for the root.
         */
        private ElementInfo parent;
        /**
         * Indicates the validity of the ElementInfo.
         */
        private boolean isValid;
        /**
         * Indicates if the ElementInfo can become valid.
         */
        private boolean canBeValid;


        /**
         * Creates the root ElementInfo.
         */
        ElementInfo(Element element) {
            this(element, null);
        }

        /**
         * Creates an ElementInfo representing <code>element</code> with
         * the specified parent.
         */
        ElementInfo(Element element, ElementInfo parent) {
            this.element = element;
            this.parent = parent;
            isValid = false;
            canBeValid = true;
        }

        /**
         * Validates the receiver. This recreates the children as well. This
         * will be invoked within a <code>readLock</code>. If this is overriden
         * it MUST invoke supers implementation first!
         */
        protected void validate() {
            isValid = true;
            loadChildren(getElement());
        }

        /**
         * Recreates the direct children of <code>info</code>.
         */
        protected void loadChildren(Element parent) {
            if (!parent.isLeaf()) {
                for (int counter = 0, maxCounter = parent.getElementCount();
                    counter < maxCounter; counter++) {
                    Element e = parent.getElement(counter);
                    ElementInfo childInfo = createElementInfo(e, this);

                    if (childInfo != null) {
                        addChild(childInfo);
                    }
                    else {
                        loadChildren(e);
                    }
                }
            }
        }

        /**
         * Returns the index of the child in the parent, or -1 for the
         * root or if the parent isn't valid.
         */
        public int getIndexInParent() {
            if (parent == null || !parent.isValid()) {
                return -1;
            }
            return parent.indexOf(this);
        }

        /**
         * Returns the Element this <code>ElementInfo</code> represents.
         */
        public Element getElement() {
            return element;
        }

        /**
         * Returns the parent of this Element, or null for the root.
         */
        public ElementInfo getParent() {
            return parent;
        }

        /**
         * Returns the index of the specified child, or -1 if
         * <code>child</code> isn't a valid child.
         */
        public int indexOf(ElementInfo child) {
            ArrayList<ElementInfo> children = this.children;

            if (children != null) {
                return children.indexOf(child);
            }
            return -1;
        }

        /**
         * Returns the child ElementInfo at <code>index</code>, or null
         * if <code>index</code> isn't a valid index.
         */
        public ElementInfo getChild(int index) {
            if (validateIfNecessary()) {
                ArrayList<ElementInfo> children = this.children;

                if (children != null && index >= 0 &&
                                        index < children.size()) {
                    return children.get(index);
                }
            }
            return null;
        }

        /**
         * Returns the number of children the ElementInfo contains.
         */
        public int getChildCount() {
            validateIfNecessary();
            return (children == null) ? 0 : children.size();
        }

        /**
         * Adds a new child to this ElementInfo.
         */
        protected void addChild(ElementInfo child) {
            if (children == null) {
                children = new ArrayList<ElementInfo>();
            }
            children.add(child);
        }

        /**
         * Returns the View corresponding to this ElementInfo, or null
         * if the ElementInfo can't be validated.
         */
        protected View getView() {
            if (!validateIfNecessary()) {
                return null;
            }
            Object lock = lock();
            try {
                View rootView = getRootView();
                Element e = getElement();
                int start = e.getStartOffset();

                if (rootView != null) {
                    return getView(rootView, e, start);
                }
                return null;
            } finally {
                unlock(lock);
            }
        }

        /**
         * Returns the Bounds for this ElementInfo, or null
         * if the ElementInfo can't be validated.
         */
        public Rectangle getBounds() {
            if (!validateIfNecessary()) {
                return null;
            }
            Object lock = lock();
            try {
                Rectangle bounds = getRootEditorRect();
                View rootView = getRootView();
                Element e = getElement();

                if (bounds != null && rootView != null) {
                    try {
                        return rootView.modelToView(e.getStartOffset(),
                                                    Position.Bias.Forward,
                                                    e.getEndOffset(),
                                                    Position.Bias.Backward,
                                                    bounds).getBounds();
                    } catch (BadLocationException ble) { }
                }
            } finally {
                unlock(lock);
            }
            return null;
        }

        /**
         * Returns true if this ElementInfo is valid.
         */
        protected boolean isValid() {
            return isValid;
        }

        /**
         * Returns the AttributeSet associated with the Element, this will
         * return null if the ElementInfo can't be validated.
         */
        protected AttributeSet getAttributes() {
            if (validateIfNecessary()) {
                return getElement().getAttributes();
            }
            return null;
        }

        /**
         * Returns the AttributeSet associated with the View that is
         * representing this Element, this will
         * return null if the ElementInfo can't be validated.
         */
        protected AttributeSet getViewAttributes() {
            if (validateIfNecessary()) {
                View view = getView();

                if (view != null) {
                    return view.getElement().getAttributes();
                }
                return getElement().getAttributes();
            }
            return null;
        }

        /**
         * Convenience method for getting an integer attribute from the passed
         * in AttributeSet.
         */
        protected int getIntAttr(AttributeSet attrs, Object key, int deflt) {
            if (attrs != null && attrs.isDefined(key)) {
                int i;
                String val = (String)attrs.getAttribute(key);
                if (val == null) {
                    i = deflt;
                }
                else {
                    try {
                        i = Math.max(0, Integer.parseInt(val));
                    } catch (NumberFormatException x) {
                        i = deflt;
                    }
                }
                return i;
            }
            return deflt;
        }

        /**
         * Validates the ElementInfo if necessary.  Some ElementInfos may
         * never be valid again.  You should check <code>isValid</code> before
         * using one.  This will reload the children and invoke
         * <code>validate</code> if the ElementInfo is invalid and can become
         * valid again. This will return true if the receiver is valid.
         */
        protected boolean validateIfNecessary() {
            if (!isValid() && canBeValid) {
                children = null;
                Object lock = lock();

                try {
                    validate();
                } finally {
                    unlock(lock);
                }
            }
            return isValid();
        }

        /**
         * Invalidates the ElementInfo. Subclasses should override this
         * if they need to reset state once invalid.
         */
        protected void invalidate(boolean first) {
            if (!isValid()) {
                if (canBeValid && !first) {
                    canBeValid = false;
                }
                return;
            }
            isValid = false;
            canBeValid = first;
            if (children != null) {
                for (ElementInfo child : children) {
                    child.invalidate(false);
                }
                children = null;
            }
        }

        private View getView(View parent, Element e, int start) {
            if (parent.getElement() == e) {
                return parent;
            }
            int index = parent.getViewIndex(start, Position.Bias.Forward);

            if (index != -1 && index < parent.getViewCount()) {
                return getView(parent.getView(index), e, start);
            }
            return null;
        }

        private int getClosestInfoIndex(int index) {
            for (int counter = 0; counter < getChildCount(); counter++) {
                ElementInfo info = getChild(counter);

                if (index < info.getElement().getEndOffset() ||
                    index == info.getElement().getStartOffset()) {
                    return counter;
                }
            }
            return -1;
        }

        private void update(DocumentEvent e) {
            if (!isValid()) {
                return;
            }
            ElementInfo parent = getParent();
            Element element = getElement();

            do {
                DocumentEvent.ElementChange ec = e.getChange(element);
                if (ec != null) {
                    if (element == getElement()) {
                        // One of our children changed.
                        invalidate(true);
                    }
                    else if (parent != null) {
                        parent.invalidate(parent == getRootInfo());
                    }
                    return;
                }
                element = element.getParentElement();
            } while (parent != null && element != null &&
                     element != parent.getElement());

            if (getChildCount() > 0) {
                Element elem = getElement();
                int pos = e.getOffset();
                int index0 = getClosestInfoIndex(pos);
                if (index0 == -1 &&
                    e.getType() == DocumentEvent.EventType.REMOVE &&
                    pos >= elem.getEndOffset()) {
                    // Event beyond our offsets. We may have represented this,
                    // that is the remove may have removed one of our child
                    // Elements that represented this, so, we should foward
                    // to last element.
                    index0 = getChildCount() - 1;
                }
                ElementInfo info = (index0 >= 0) ? getChild(index0) : null;
                if (info != null &&
                    (info.getElement().getStartOffset() == pos) && (pos > 0)) {
                    // If at a boundary, forward the event to the previous
                    // ElementInfo too.
                    index0 = Math.max(index0 - 1, 0);
                }
                int index1;
                if (e.getType() != DocumentEvent.EventType.REMOVE) {
                    index1 = getClosestInfoIndex(pos + e.getLength());
                    if (index1 < 0) {
                        index1 = getChildCount() - 1;
                    }
                }
                else {
                    index1 = index0;
                    // A remove may result in empty elements.
                    while ((index1 + 1) < getChildCount() &&
                           getChild(index1 + 1).getElement().getEndOffset() ==
                           getChild(index1 + 1).getElement().getStartOffset()){
                        index1++;
                    }
                }
                index0 = Math.max(index0, 0);
                // The check for isValid is here as in the process of
                // forwarding update our child may invalidate us.
                for (int i = index0; i <= index1 && isValid(); i++) {
                    getChild(i).update(e);
                }
            }
        }
    }

    /**
     * DocumentListener installed on the current Document.  Will invoke
     * <code>update</code> on the <code>RootInfo</code> in response to
     * any event.
     */
    private class DocumentHandler implements DocumentListener {
        public void insertUpdate(DocumentEvent e) {
            getRootInfo().update(e);
        }
        public void removeUpdate(DocumentEvent e) {
            getRootInfo().update(e);
        }
        public void changedUpdate(DocumentEvent e) {
            getRootInfo().update(e);
        }
    }

    /*
     * PropertyChangeListener installed on the editor.
     */
    private class PropertyChangeHandler implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals("document")) {
                // handle the document change
                setDocument(editor.getDocument());
            }
        }
    }
}
