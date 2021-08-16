/*
 * Copyright (c) 1998, 2013, Oracle and/or its affiliates. All rights reserved.
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
import java.text.BreakIterator;
import javax.swing.event.DocumentEvent;
import javax.swing.text.*;

/**
 * Displays the <dfn>inline element</dfn> styles
 * based upon css attributes.
 *
 * @author  Timothy Prinzing
 */
public class InlineView extends LabelView {

    /**
     * Constructs a new view wrapped on an element.
     *
     * @param elem the element
     */
    public InlineView(Element elem) {
        super(elem);
        StyleSheet sheet = getStyleSheet();
        attr = sheet.getViewAttributes(this);
    }

    /**
     * Gives notification that something was inserted into
     * the document in a location that this view is responsible for.
     * If either parameter is <code>null</code>, behavior of this method is
     * implementation dependent.
     *
     * @param e the change information from the associated document
     * @param a the current allocation of the view
     * @param f the factory to use to rebuild if the view has children
     * @since 1.5
     * @see View#insertUpdate
     */
    public void insertUpdate(DocumentEvent e, Shape a, ViewFactory f) {
        super.insertUpdate(e, a, f);
    }

    /**
     * Gives notification that something was removed from the document
     * in a location that this view is responsible for.
     * If either parameter is <code>null</code>, behavior of this method is
     * implementation dependent.
     *
     * @param e the change information from the associated document
     * @param a the current allocation of the view
     * @param f the factory to use to rebuild if the view has children
     * @since 1.5
     * @see View#removeUpdate
     */
    public void removeUpdate(DocumentEvent e, Shape a, ViewFactory f) {
        super.removeUpdate(e, a, f);
    }

    /**
     * Gives notification from the document that attributes were changed
     * in a location that this view is responsible for.
     *
     * @param e the change information from the associated document
     * @param a the current allocation of the view
     * @param f the factory to use to rebuild if the view has children
     * @see View#changedUpdate
     */
    public void changedUpdate(DocumentEvent e, Shape a, ViewFactory f) {
        super.changedUpdate(e, a, f);
        StyleSheet sheet = getStyleSheet();
        attr = sheet.getViewAttributes(this);
        preferenceChanged(null, true, true);
    }

    /**
     * Fetches the attributes to use when rendering.  This is
     * implemented to multiplex the attributes specified in the
     * model with a StyleSheet.
     */
    public AttributeSet getAttributes() {
        return attr;
    }

    /**
     * Determines how attractive a break opportunity in
     * this view is.  This can be used for determining which
     * view is the most attractive to call <code>breakView</code>
     * on in the process of formatting.  A view that represents
     * text that has whitespace in it might be more attractive
     * than a view that has no whitespace, for example.  The
     * higher the weight, the more attractive the break.  A
     * value equal to or lower than <code>BadBreakWeight</code>
     * should not be considered for a break.  A value greater
     * than or equal to <code>ForcedBreakWeight</code> should
     * be broken.
     * <p>
     * This is implemented to provide the default behavior
     * of returning <code>BadBreakWeight</code> unless the length
     * is greater than the length of the view in which case the
     * entire view represents the fragment.  Unless a view has
     * been written to support breaking behavior, it is not
     * attractive to try and break the view.  An example of
     * a view that does support breaking is <code>LabelView</code>.
     * An example of a view that uses break weight is
     * <code>ParagraphView</code>.
     *
     * @param axis may be either View.X_AXIS or View.Y_AXIS
     * @param pos the potential location of the start of the
     *   broken view &gt;= 0.  This may be useful for calculating tab
     *   positions.
     * @param len specifies the relative length from <em>pos</em>
     *   where a potential break is desired &gt;= 0.
     * @return the weight, which should be a value between
     *   ForcedBreakWeight and BadBreakWeight.
     * @see LabelView
     * @see ParagraphView
     * @see javax.swing.text.View#BadBreakWeight
     * @see javax.swing.text.View#GoodBreakWeight
     * @see javax.swing.text.View#ExcellentBreakWeight
     * @see javax.swing.text.View#ForcedBreakWeight
     */
    public int getBreakWeight(int axis, float pos, float len) {
        if (nowrap) {
            return BadBreakWeight;
        }
        return super.getBreakWeight(axis, pos, len);
    }

    /**
     * Tries to break this view on the given axis. Refer to
     * {@link javax.swing.text.View#breakView} for a complete
     * description of this method.
     * <p>Behavior of this method is unspecified in case <code>axis</code>
     * is neither <code>View.X_AXIS</code> nor <code>View.Y_AXIS</code>, and
     * in case <code>offset</code>, <code>pos</code>, or <code>len</code>
     * is null.
     *
     * @param axis may be either <code>View.X_AXIS</code> or
     *          <code>View.Y_AXIS</code>
     * @param offset the location in the document model
     *   that a broken fragment would occupy &gt;= 0.  This
     *   would be the starting offset of the fragment
     *   returned
     * @param pos the position along the axis that the
     *  broken view would occupy &gt;= 0.  This may be useful for
     *  things like tab calculations
     * @param len specifies the distance along the axis
     *  where a potential break is desired &gt;= 0
     * @return the fragment of the view that represents the
     *  given span.
     * @since 1.5
     * @see javax.swing.text.View#breakView
     */
    public View breakView(int axis, int offset, float pos, float len) {
        return super.breakView(axis, offset, pos, len);
    }


    /**
     * Set the cached properties from the attributes.
     */
    protected void setPropertiesFromAttributes() {
        super.setPropertiesFromAttributes();
        AttributeSet a = getAttributes();
        Object decor = a.getAttribute(CSS.Attribute.TEXT_DECORATION);
        boolean u = (decor != null) ?
          (decor.toString().indexOf("underline") >= 0) : false;
        setUnderline(u);
        boolean s = (decor != null) ?
          (decor.toString().indexOf("line-through") >= 0) : false;
        setStrikeThrough(s);
        Object vAlign = a.getAttribute(CSS.Attribute.VERTICAL_ALIGN);
        s = (vAlign != null) ? (vAlign.toString().indexOf("sup") >= 0) : false;
        setSuperscript(s);
        s = (vAlign != null) ? (vAlign.toString().indexOf("sub") >= 0) : false;
        setSubscript(s);

        Object whitespace = a.getAttribute(CSS.Attribute.WHITE_SPACE);
        if ((whitespace != null) && whitespace.equals("nowrap")) {
            nowrap = true;
        } else {
            nowrap = false;
        }

        HTMLDocument doc = (HTMLDocument)getDocument();
        // fetches background color from stylesheet if specified
        Color bg = doc.getBackground(a);
        if (bg != null) {
            setBackground(bg);
        }
    }

    /**
     * Convenient method to get the StyleSheet.
     *
     * @return the StyleSheet
     */
    protected StyleSheet getStyleSheet() {
        HTMLDocument doc = (HTMLDocument) getDocument();
        return doc.getStyleSheet();
    }

    private boolean nowrap;
    private AttributeSet attr;
}
