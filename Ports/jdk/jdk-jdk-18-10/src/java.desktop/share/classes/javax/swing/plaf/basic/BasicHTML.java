/*
 * Copyright (c) 1998, 2019, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing.plaf.basic;

import java.io.*;
import java.awt.*;
import java.net.URL;

import javax.accessibility.AccessibleContext;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

import sun.swing.SwingUtilities2;

/**
 * Support for providing html views for the swing components.
 * This translates a simple html string to a javax.swing.text.View
 * implementation that can render the html and provide the necessary
 * layout semantics.
 *
 * @author  Timothy Prinzing
 * @since 1.3
 */
public class BasicHTML {

    /**
     * Constructs a {@code BasicHTML}.
     */
    public BasicHTML() {}

    /**
     * Create an html renderer for the given component and
     * string of html.
     *
     * @param c a component
     * @param html an HTML string
     * @return an HTML renderer
     */
    public static View createHTMLView(JComponent c, String html) {
        BasicEditorKit kit = getFactory();
        Document doc = kit.createDefaultDocument(c.getFont(),
                                                 c.getForeground());
        Object base = c.getClientProperty(documentBaseKey);
        if (base instanceof URL) {
            ((HTMLDocument)doc).setBase((URL)base);
        }
        Reader r = new StringReader(html);
        try {
            kit.read(r, doc, 0);
        } catch (Throwable e) {
        }
        ViewFactory f = kit.getViewFactory();
        View hview = f.create(doc.getDefaultRootElement());
        View v = new Renderer(c, f, hview);
        return v;
    }

    /**
     * Returns the baseline for the html renderer.
     *
     * @param view the View to get the baseline for
     * @param w the width to get the baseline for
     * @param h the height to get the baseline for
     * @throws IllegalArgumentException if width or height is &lt; 0
     * @return baseline or a value &lt; 0 indicating there is no reasonable
     *                  baseline
     * @see java.awt.FontMetrics
     * @see javax.swing.JComponent#getBaseline(int,int)
     * @since 1.6
     */
    public static int getHTMLBaseline(View view, int w, int h) {
        if (w < 0 || h < 0) {
            throw new IllegalArgumentException(
                    "Width and height must be >= 0");
        }
        if (view instanceof Renderer) {
            return getBaseline(view.getView(0), w, h);
        }
        return -1;
    }

    /**
     * Gets the baseline for the specified component.  This digs out
     * the View client property, and if non-null the baseline is calculated
     * from it.  Otherwise the baseline is the value <code>y + ascent</code>.
     */
    static int getBaseline(JComponent c, int y, int ascent,
                                  int w, int h) {
        View view = (View)c.getClientProperty(BasicHTML.propertyKey);
        if (view != null) {
            int baseline = getHTMLBaseline(view, w, h);
            if (baseline < 0) {
                return baseline;
            }
            return y + baseline;
        }
        return y + ascent;
    }

    /**
     * Gets the baseline for the specified View.
     */
    static int getBaseline(View view, int w, int h) {
        if (hasParagraph(view)) {
            view.setSize(w, h);
            return getBaseline(view, new Rectangle(0, 0, w, h));
        }
        return -1;
    }

    private static int getBaseline(View view, Shape bounds) {
        if (view.getViewCount() == 0) {
            return -1;
        }
        AttributeSet attributes = view.getElement().getAttributes();
        Object name = null;
        if (attributes != null) {
            name = attributes.getAttribute(StyleConstants.NameAttribute);
        }
        int index = 0;
        if (name == HTML.Tag.HTML && view.getViewCount() > 1) {
            // For html on widgets the header is not visible, skip it.
            index++;
        }
        bounds = view.getChildAllocation(index, bounds);
        if (bounds == null) {
            return -1;
        }
        View child = view.getView(index);
        if (view instanceof javax.swing.text.ParagraphView) {
            Rectangle rect;
            if (bounds instanceof Rectangle) {
                rect = (Rectangle)bounds;
            }
            else {
                rect = bounds.getBounds();
            }
            return rect.y + (int)(rect.height *
                                  child.getAlignment(View.Y_AXIS));
        }
        return getBaseline(child, bounds);
    }

    private static boolean hasParagraph(View view) {
        if (view instanceof javax.swing.text.ParagraphView) {
            return true;
        }
        if (view.getViewCount() == 0) {
            return false;
        }
        AttributeSet attributes = view.getElement().getAttributes();
        Object name = null;
        if (attributes != null) {
            name = attributes.getAttribute(StyleConstants.NameAttribute);
        }
        int index = 0;
        if (name == HTML.Tag.HTML && view.getViewCount() > 1) {
            // For html on widgets the header is not visible, skip it.
            index = 1;
        }
        return hasParagraph(view.getView(index));
    }

    /**
     * Check the given string to see if it should trigger the
     * html rendering logic in a non-text component that supports
     * html rendering.
     *
     * @param s a text
     * @return {@code true} if the given string should trigger the
     *         html rendering logic in a non-text component
     */
    public static boolean isHTMLString(String s) {
        if (s != null) {
            if ((s.length() >= 6) && (s.charAt(0) == '<') && (s.charAt(5) == '>')) {
                String tag = s.substring(1,5);
                return tag.equalsIgnoreCase(propertyKey);
            }
        }
        return false;
    }

    /**
     * Stash the HTML render for the given text into the client
     * properties of the given JComponent. If the given text is
     * <em>NOT HTML</em> the property will be cleared of any
     * renderer.
     * <p>
     * This method is useful for ComponentUI implementations
     * that are static (i.e. shared) and get their state
     * entirely from the JComponent.
     *
     * @param c a component
     * @param text a text
     */
    public static void updateRenderer(JComponent c, String text) {
        View value = null;
        View oldValue = (View)c.getClientProperty(BasicHTML.propertyKey);
        Boolean htmlDisabled = (Boolean) c.getClientProperty(htmlDisable);
        if (htmlDisabled != Boolean.TRUE && BasicHTML.isHTMLString(text)) {
            value = BasicHTML.createHTMLView(c, text);
        }
        if (value != oldValue && oldValue != null) {
            for (int i = 0; i < oldValue.getViewCount(); i++) {
                oldValue.getView(i).setParent(null);
            }
        }
        c.putClientProperty(BasicHTML.propertyKey, value);
        String currentAccessibleNameProperty =
            (String) c.getClientProperty(AccessibleContext.ACCESSIBLE_NAME_PROPERTY);
        String previousParsedText = null;
        if (currentAccessibleNameProperty != null && oldValue != null) {
            try {
                previousParsedText =
                    (oldValue.getDocument().getText(0, oldValue.getDocument().getLength())).strip();
            } catch (BadLocationException e) {
            }
        }

        // AccessibleContext.ACCESSIBLE_NAME_PROPERTY should be set from here only if,
        // 1. If AccessibleContext.ACCESSIBLE_NAME_PROPERTY was NOT set before
        //        i.e. currentAccessibleNameProperty is null. and,
        // 2. If AccessibleContext.ACCESSIBLE_NAME_PROPERTY was previously set from this method
        //        using the value.getDocument().getText().
        if (currentAccessibleNameProperty == null ||
                currentAccessibleNameProperty.equals(previousParsedText)) {
            String parsedText = null;
            if (value != null) {
                try {
                    parsedText =
                        (value.getDocument().getText(0, value.getDocument().getLength())).strip();
                } catch (BadLocationException e) {
                }
            }
            c.putClientProperty(AccessibleContext.ACCESSIBLE_NAME_PROPERTY, parsedText);
        }
    }

    /**
     * If this client property of a JComponent is set to Boolean.TRUE
     * the component's 'text' property is never treated as HTML.
     */
    private static final String htmlDisable = "html.disable";

    /**
     * Key to use for the html renderer when stored as a
     * client property of a JComponent.
     */
    public static final String propertyKey = "html";

    /**
     * Key stored as a client property to indicate the base that relative
     * references are resolved against. For example, lets say you keep
     * your images in the directory resources relative to the code path,
     * you would use the following the set the base:
     * <pre>
     *   jComponent.putClientProperty(documentBaseKey,
     *                                xxx.class.getResource("resources/"));
     * </pre>
     */
    public static final String documentBaseKey = "html.base";

    static BasicEditorKit getFactory() {
        if (basicHTMLFactory == null) {
            basicHTMLViewFactory = new BasicHTMLViewFactory();
            basicHTMLFactory = new BasicEditorKit();
        }
        return basicHTMLFactory;
    }

    /**
     * The source of the html renderers
     */
    private static BasicEditorKit basicHTMLFactory;

    /**
     * Creates the Views that visually represent the model.
     */
    private static ViewFactory basicHTMLViewFactory;

    /**
     * Overrides to the default stylesheet.  Should consider
     * just creating a completely fresh stylesheet.
     */
    private static final String styleChanges =
    "p { margin-top: 0; margin-bottom: 0; margin-left: 0; margin-right: 0 }" +
    "body { margin-top: 0; margin-bottom: 0; margin-left: 0; margin-right: 0 }";

    /**
     * The views produced for the ComponentUI implementations aren't
     * going to be edited and don't need full html support.  This kit
     * alters the HTMLEditorKit to try and trim things down a bit.
     * It does the following:
     * <ul>
     * <li>It doesn't produce Views for things like comments,
     * head, title, unknown tags, etc.
     * <li>It installs a different set of css settings from the default
     * provided by HTMLEditorKit.
     * </ul>
     */
    @SuppressWarnings("serial") // JDK-implementation class
    static class BasicEditorKit extends HTMLEditorKit {
        /** Shared base style for all documents created by us use. */
        private static StyleSheet defaultStyles;

        /**
         * Overriden to return our own slimmed down style sheet.
         */
        public StyleSheet getStyleSheet() {
            if (defaultStyles == null) {
                defaultStyles = new StyleSheet();
                StringReader r = new StringReader(styleChanges);
                try {
                    defaultStyles.loadRules(r, null);
                } catch (Throwable e) {
                    // don't want to die in static initialization...
                    // just display things wrong.
                }
                r.close();
                defaultStyles.addStyleSheet(super.getStyleSheet());
            }
            return defaultStyles;
        }

        /**
         * Sets the async policy to flush everything in one chunk, and
         * to not display unknown tags.
         */
        public Document createDefaultDocument(Font defaultFont,
                                              Color foreground) {
            StyleSheet styles = getStyleSheet();
            StyleSheet ss = new StyleSheet();
            ss.addStyleSheet(styles);
            BasicDocument doc = new BasicDocument(ss, defaultFont, foreground);
            doc.setAsynchronousLoadPriority(Integer.MAX_VALUE);
            doc.setPreservesUnknownTags(false);
            return doc;
        }

        /**
         * Returns the ViewFactory that is used to make sure the Views don't
         * load in the background.
         */
        public ViewFactory getViewFactory() {
            return basicHTMLViewFactory;
        }
    }


    /**
     * BasicHTMLViewFactory extends HTMLFactory to force images to be loaded
     * synchronously.
     */
    static class BasicHTMLViewFactory extends HTMLEditorKit.HTMLFactory {
        public View create(Element elem) {
            View view = super.create(elem);

            if (view instanceof ImageView) {
                ((ImageView)view).setLoadsSynchronously(true);
            }
            return view;
        }
    }


    /**
     * The subclass of HTMLDocument that is used as the model. getForeground
     * is overridden to return the foreground property from the Component this
     * was created for.
     */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    static class BasicDocument extends HTMLDocument {
        /** The host, that is where we are rendering. */
        // private JComponent host;

        BasicDocument(StyleSheet s, Font defaultFont, Color foreground) {
            super(s);
            setPreservesUnknownTags(false);
            setFontAndColor(defaultFont, foreground);
        }

        /**
         * Sets the default font and default color. These are set by
         * adding a rule for the body that specifies the font and color.
         * This allows the html to override these should it wish to have
         * a custom font or color.
         */
        private void setFontAndColor(Font font, Color fg) {
            getStyleSheet().addRule(sun.swing.SwingUtilities2.
                                    displayPropertiesToCSS(font,fg));
        }
    }


    /**
     * Root text view that acts as an HTML renderer.
     */
    static class Renderer extends View {

        Renderer(JComponent c, ViewFactory f, View v) {
            super(null);
            host = c;
            factory = f;
            view = v;
            view.setParent(this);
            // initially layout to the preferred size
            setSize(view.getPreferredSpan(X_AXIS), view.getPreferredSpan(Y_AXIS));
        }

        /**
         * Fetches the attributes to use when rendering.  At the root
         * level there are no attributes.  If an attribute is resolved
         * up the view hierarchy this is the end of the line.
         */
        public AttributeSet getAttributes() {
            return null;
        }

        /**
         * Determines the preferred span for this view along an axis.
         *
         * @param axis may be either X_AXIS or Y_AXIS
         * @return the span the view would like to be rendered into.
         *         Typically the view is told to render into the span
         *         that is returned, although there is no guarantee.
         *         The parent may choose to resize or break the view.
         */
        public float getPreferredSpan(int axis) {
            if (axis == X_AXIS) {
                // width currently laid out to
                return width;
            }
            return view.getPreferredSpan(axis);
        }

        /**
         * Determines the minimum span for this view along an axis.
         *
         * @param axis may be either X_AXIS or Y_AXIS
         * @return the span the view would like to be rendered into.
         *         Typically the view is told to render into the span
         *         that is returned, although there is no guarantee.
         *         The parent may choose to resize or break the view.
         */
        public float getMinimumSpan(int axis) {
            return view.getMinimumSpan(axis);
        }

        /**
         * Determines the maximum span for this view along an axis.
         *
         * @param axis may be either X_AXIS or Y_AXIS
         * @return the span the view would like to be rendered into.
         *         Typically the view is told to render into the span
         *         that is returned, although there is no guarantee.
         *         The parent may choose to resize or break the view.
         */
        public float getMaximumSpan(int axis) {
            return Integer.MAX_VALUE;
        }

        /**
         * Specifies that a preference has changed.
         * Child views can call this on the parent to indicate that
         * the preference has changed.  The root view routes this to
         * invalidate on the hosting component.
         * <p>
         * This can be called on a different thread from the
         * event dispatching thread and is basically unsafe to
         * propagate into the component.  To make this safe,
         * the operation is transferred over to the event dispatching
         * thread for completion.  It is a design goal that all view
         * methods be safe to call without concern for concurrency,
         * and this behavior helps make that true.
         *
         * @param child the child view
         * @param width true if the width preference has changed
         * @param height true if the height preference has changed
         */
        public void preferenceChanged(View child, boolean width, boolean height) {
            host.revalidate();
            host.repaint();
        }

        /**
         * Determines the desired alignment for this view along an axis.
         *
         * @param axis may be either X_AXIS or Y_AXIS
         * @return the desired alignment, where 0.0 indicates the origin
         *     and 1.0 the full span away from the origin
         */
        public float getAlignment(int axis) {
            return view.getAlignment(axis);
        }

        /**
         * Renders the view.
         *
         * @param g the graphics context
         * @param allocation the region to render into
         */
        public void paint(Graphics g, Shape allocation) {
            Rectangle alloc = allocation.getBounds();
            view.setSize(alloc.width, alloc.height);
            view.paint(g, allocation);
        }

        /**
         * Sets the view parent.
         *
         * @param parent the parent view
         */
        public void setParent(View parent) {
            throw new Error("Can't set parent on root view");
        }

        /**
         * Returns the number of views in this view.  Since
         * this view simply wraps the root of the view hierarchy
         * it has exactly one child.
         *
         * @return the number of views
         * @see #getView
         */
        public int getViewCount() {
            return 1;
        }

        /**
         * Gets the n-th view in this container.
         *
         * @param n the number of the view to get
         * @return the view
         */
        public View getView(int n) {
            return view;
        }

        /**
         * Provides a mapping from the document model coordinate space
         * to the coordinate space of the view mapped to it.
         *
         * @param pos the position to convert
         * @param a the allocated region to render into
         * @return the bounding box of the given position
         */
        public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
            return view.modelToView(pos, a, b);
        }

        /**
         * Provides a mapping from the document model coordinate space
         * to the coordinate space of the view mapped to it.
         *
         * @param p0 the position to convert >= 0
         * @param b0 the bias toward the previous character or the
         *  next character represented by p0, in case the
         *  position is a boundary of two views.
         * @param p1 the position to convert >= 0
         * @param b1 the bias toward the previous character or the
         *  next character represented by p1, in case the
         *  position is a boundary of two views.
         * @param a the allocated region to render into
         * @return the bounding box of the given position is returned
         * @exception BadLocationException  if the given position does
         *   not represent a valid location in the associated document
         * @exception IllegalArgumentException for an invalid bias argument
         * @see View#viewToModel
         */
        public Shape modelToView(int p0, Position.Bias b0, int p1,
                                 Position.Bias b1, Shape a) throws BadLocationException {
            return view.modelToView(p0, b0, p1, b1, a);
        }

        /**
         * Provides a mapping from the view coordinate space to the logical
         * coordinate space of the model.
         *
         * @param x x coordinate of the view location to convert
         * @param y y coordinate of the view location to convert
         * @param a the allocated region to render into
         * @return the location within the model that best represents the
         *    given point in the view
         */
        public int viewToModel(float x, float y, Shape a, Position.Bias[] bias) {
            return view.viewToModel(x, y, a, bias);
        }

        /**
         * Returns the document model underlying the view.
         *
         * @return the model
         */
        public Document getDocument() {
            return view.getDocument();
        }

        /**
         * Returns the starting offset into the model for this view.
         *
         * @return the starting offset
         */
        public int getStartOffset() {
            return view.getStartOffset();
        }

        /**
         * Returns the ending offset into the model for this view.
         *
         * @return the ending offset
         */
        public int getEndOffset() {
            return view.getEndOffset();
        }

        /**
         * Gets the element that this view is mapped to.
         *
         * @return the view
         */
        public Element getElement() {
            return view.getElement();
        }

        /**
         * Sets the view size.
         *
         * @param width the width
         * @param height the height
         */
        public void setSize(float width, float height) {
            this.width = (int) width;
            view.setSize(width, height);
        }

        /**
         * Fetches the container hosting the view.  This is useful for
         * things like scheduling a repaint, finding out the host
         * components font, etc.  The default implementation
         * of this is to forward the query to the parent view.
         *
         * @return the container
         */
        public Container getContainer() {
            return host;
        }

        /**
         * Fetches the factory to be used for building the
         * various view fragments that make up the view that
         * represents the model.  This is what determines
         * how the model will be represented.  This is implemented
         * to fetch the factory provided by the associated
         * EditorKit.
         *
         * @return the factory
         */
        public ViewFactory getViewFactory() {
            return factory;
        }

        private int width;
        private View view;
        private ViewFactory factory;
        private JComponent host;

    }
}
