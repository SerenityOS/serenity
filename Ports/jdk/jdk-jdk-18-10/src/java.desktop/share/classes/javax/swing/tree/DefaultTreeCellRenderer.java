/*
 * Copyright (c) 1997, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing.tree;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import javax.swing.plaf.BorderUIResource.EmptyBorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import sun.swing.DefaultLookup;
import sun.swing.SwingUtilities2;

/**
 * Displays an entry in a tree.
 * <code>DefaultTreeCellRenderer</code> is not opaque and
 * unless you subclass paint you should not change this.
 * See <a
 href="https://docs.oracle.com/javase/tutorial/uiswing/components/tree.html">How to Use Trees</a>
 * in <em>The Java Tutorial</em>
 * for examples of customizing node display using this class.
 * <p>
 * The set of icons and colors used by {@code DefaultTreeCellRenderer}
 * can be configured using the various setter methods. The value for
 * each property is initialized from the defaults table. When the
 * look and feel changes ({@code updateUI} is invoked), any properties
 * that have a value of type {@code UIResource} are refreshed from the
 * defaults table. The following table lists the mapping between
 * {@code DefaultTreeCellRenderer} property and defaults table key:
 *
 * <table class="striped">
 * <caption>Properties</caption>
 * <thead>
 *   <tr>
 *     <th scope="col">Property
 *     <th scope="col">Key
 *   </tr>
 * </thead>
 * <tbody>
 *   <tr>
 *     <th scope="row">"leafIcon"
 *     <td>"Tree.leafIcon"
 *   <tr>
 *     <th scope="row">"closedIcon"
 *     <td>"Tree.closedIcon"
 *   <tr>
 *     <th scope="row">"openIcon"
 *     <td>"Tree.openIcon"
 *   <tr>
 *     <th scope="row">"textSelectionColor"
 *     <td>"Tree.selectionForeground"
 *   <tr>
 *     <th scope="row">"textNonSelectionColor"
 *     <td>"Tree.textForeground"
 *   <tr>
 *     <th scope="row">"backgroundSelectionColor"
 *     <td>"Tree.selectionBackground"
 *   <tr>
 *     <th scope="row">"backgroundNonSelectionColor"
 *     <td>"Tree.textBackground"
 *   <tr>
 *     <th scope="row">"borderSelectionColor"
 *     <td>"Tree.selectionBorderColor"
 * </tbody>
 * </table>
 * <p>
 * <strong><a id="override">Implementation Note:</a></strong>
 * This class overrides
 * <code>invalidate</code>,
 * <code>validate</code>,
 * <code>revalidate</code>,
 * <code>repaint</code>,
 * and
 * <code>firePropertyChange</code>
 * solely to improve performance.
 * If not overridden, these frequently called methods would execute code paths
 * that are unnecessary for the default tree cell renderer.
 * If you write your own renderer,
 * take care to weigh the benefits and
 * drawbacks of overriding these methods.
 *
 * <p>
 * <strong>Warning:</strong>
 * Serialized objects of this class will not be compatible with
 * future Swing releases. The current serialization support is
 * appropriate for short term storage or RMI between applications running
 * the same version of Swing.  As of 1.4, support for long term storage
 * of all JavaBeans
 * has been added to the <code>java.beans</code> package.
 * Please see {@link java.beans.XMLEncoder}.
 *
 * @author Rob Davis
 * @author Ray Ryan
 * @author Scott Violet
 */
@SuppressWarnings("serial") // Same-version serialization only
public class DefaultTreeCellRenderer extends JLabel implements TreeCellRenderer
{
    /** Last tree the renderer was painted in. */
    private JTree tree;

    /** Is the value currently selected. */
    protected boolean selected;
    /** True if has focus. */
    protected boolean hasFocus;
    /** True if draws focus border around icon as well. */
    private boolean drawsFocusBorderAroundIcon;
    /** If true, a dashed line is drawn as the focus indicator. */
    private boolean drawDashedFocusIndicator;

    // If drawDashedFocusIndicator is true, the following are used.
    /**
     * Background color of the tree.
     */
    private Color treeBGColor;
    /**
     * Color to draw the focus indicator in, determined from the background.
     * color.
     */
    private Color focusBGColor;

    // Icons
    /** Icon used to show non-leaf nodes that aren't expanded. */
    protected transient Icon closedIcon;

    /** Icon used to show leaf nodes. */
    protected transient Icon leafIcon;

    /** Icon used to show non-leaf nodes that are expanded. */
    protected transient Icon openIcon;

    // Colors
    /** Color to use for the foreground for selected nodes. */
    protected Color textSelectionColor;

    /** Color to use for the foreground for non-selected nodes. */
    protected Color textNonSelectionColor;

    /** Color to use for the background when a node is selected. */
    protected Color backgroundSelectionColor;

    /** Color to use for the background when the node isn't selected. */
    protected Color backgroundNonSelectionColor;

    /** Color to use for the focus indicator when the node has focus. */
    protected Color borderSelectionColor;

    private boolean isDropCell;
    private boolean fillBackground;

    /**
     * Set to true after the constructor has run.
     */
    private boolean inited;

    /**
     * Creates a {@code DefaultTreeCellRenderer}. Icons and text color are
     * determined from the {@code UIManager}.
     */
    public DefaultTreeCellRenderer() {
        inited = true;
    }

    /**
     * {@inheritDoc}
     *
     * @since 1.7
     */
    public void updateUI() {
        super.updateUI();
        // To avoid invoking new methods from the constructor, the
        // inited field is first checked. If inited is false, the constructor
        // has not run and there is no point in checking the value. As
        // all look and feels have a non-null value for these properties,
        // a null value means the developer has specifically set it to
        // null. As such, if the value is null, this does not reset the
        // value.
        if (!inited || (getLeafIcon() instanceof UIResource)) {
            setLeafIcon(DefaultLookup.getIcon(this, ui, "Tree.leafIcon"));
        }
        if (!inited || (getClosedIcon() instanceof UIResource)) {
            setClosedIcon(DefaultLookup.getIcon(this, ui, "Tree.closedIcon"));
        }
        if (!inited || (getOpenIcon() instanceof UIResource)) {
            setOpenIcon(DefaultLookup.getIcon(this, ui, "Tree.openIcon"));
        }
        if (!inited || (getTextSelectionColor() instanceof UIResource)) {
            setTextSelectionColor(
                    DefaultLookup.getColor(this, ui, "Tree.selectionForeground"));
        }
        if (!inited || (getTextNonSelectionColor() instanceof UIResource)) {
            setTextNonSelectionColor(
                    DefaultLookup.getColor(this, ui, "Tree.textForeground"));
        }
        if (!inited || (getBackgroundSelectionColor() instanceof UIResource)) {
            setBackgroundSelectionColor(
                    DefaultLookup.getColor(this, ui, "Tree.selectionBackground"));
        }
        if (!inited ||
                (getBackgroundNonSelectionColor() instanceof UIResource)) {
            setBackgroundNonSelectionColor(
                    DefaultLookup.getColor(this, ui, "Tree.textBackground"));
        }
        if (!inited || (getBorderSelectionColor() instanceof UIResource)) {
            setBorderSelectionColor(
                    DefaultLookup.getColor(this, ui, "Tree.selectionBorderColor"));
        }
        drawsFocusBorderAroundIcon = DefaultLookup.getBoolean(
                this, ui, "Tree.drawsFocusBorderAroundIcon", false);
        drawDashedFocusIndicator = DefaultLookup.getBoolean(
                this, ui, "Tree.drawDashedFocusIndicator", false);

        fillBackground = DefaultLookup.getBoolean(this, ui, "Tree.rendererFillBackground", true);
        if (!inited || getBorder() instanceof UIResource)  {
            Insets margins = DefaultLookup.getInsets(this, ui, "Tree.rendererMargins");
            if (margins != null) {
                setBorder(new EmptyBorderUIResource(margins));
            } else {
                setBorder(new EmptyBorderUIResource(0, 0, 0, 0));
            }
        }

        setName("Tree.cellRenderer");
    }


    /**
      * Returns the default icon, for the current laf, that is used to
      * represent non-leaf nodes that are expanded.
      *
      * @return the default icon, for the current laf, that is used to
      *         represent non-leaf nodes that are expanded.
      */
    public Icon getDefaultOpenIcon() {
        return DefaultLookup.getIcon(this, ui, "Tree.openIcon");
    }

    /**
      * Returns the default icon, for the current laf, that is used to
      * represent non-leaf nodes that are not expanded.
      *
      * @return the default icon, for the current laf, that is used to
      *         represent non-leaf nodes that are not expanded.
      */
    public Icon getDefaultClosedIcon() {
        return DefaultLookup.getIcon(this, ui, "Tree.closedIcon");
    }

    /**
      * Returns the default icon, for the current laf, that is used to
      * represent leaf nodes.
      *
      * @return the default icon, for the current laf, that is used to
      *         represent leaf nodes.
      */
    public Icon getDefaultLeafIcon() {
        return DefaultLookup.getIcon(this, ui, "Tree.leafIcon");
    }

    /**
      * Sets the icon used to represent non-leaf nodes that are expanded.
      *
      * @param newIcon the icon to be used for expanded non-leaf nodes
      */
    public void setOpenIcon(Icon newIcon) {
        openIcon = newIcon;
    }

    /**
      * Returns the icon used to represent non-leaf nodes that are expanded.
      *
      * @return the icon used to represent non-leaf nodes that are expanded
      */
    public Icon getOpenIcon() {
        return openIcon;
    }

    /**
      * Sets the icon used to represent non-leaf nodes that are not expanded.
      *
      * @param newIcon the icon to be used for not expanded non-leaf nodes
      */
    public void setClosedIcon(Icon newIcon) {
        closedIcon = newIcon;
    }

    /**
      * Returns the icon used to represent non-leaf nodes that are not
      * expanded.
      *
      * @return the icon used to represent non-leaf nodes that are not
      *         expanded
      */
    public Icon getClosedIcon() {
        return closedIcon;
    }

    /**
      * Sets the icon used to represent leaf nodes.
      *
      * @param newIcon icon to be used for leaf nodes
      */
    public void setLeafIcon(Icon newIcon) {
        leafIcon = newIcon;
    }

    /**
      * Returns the icon used to represent leaf nodes.
      *
      * @return the icon used to represent leaf nodes
      */
    public Icon getLeafIcon() {
        return leafIcon;
    }

    /**
      * Sets the color the text is drawn with when the node is selected.
      *
      * @param newColor color to be used for text when the node is selected
      */
    public void setTextSelectionColor(Color newColor) {
        textSelectionColor = newColor;
    }

    /**
      * Returns the color the text is drawn with when the node is selected.
      *
      * @return the color the text is drawn with when the node is selected
      */
    public Color getTextSelectionColor() {
        return textSelectionColor;
    }

    /**
      * Sets the color the text is drawn with when the node isn't selected.
      *
      * @param newColor color to be used for text when the node isn't selected
      */
    public void setTextNonSelectionColor(Color newColor) {
        textNonSelectionColor = newColor;
    }

    /**
      * Returns the color the text is drawn with when the node isn't selected.
      *
      * @return the color the text is drawn with when the node isn't selected.
      */
    public Color getTextNonSelectionColor() {
        return textNonSelectionColor;
    }

    /**
      * Sets the color to use for the background if node is selected.
      *
      * @param newColor to be used for the background if the node is selected
      */
    public void setBackgroundSelectionColor(Color newColor) {
        backgroundSelectionColor = newColor;
    }


    /**
      * Returns the color to use for the background if node is selected.
      *
      * @return the color to use for the background if node is selected
      */
    public Color getBackgroundSelectionColor() {
        return backgroundSelectionColor;
    }

    /**
      * Sets the background color to be used for non selected nodes.
      *
      * @param newColor color to be used for the background for non selected nodes
      */
    public void setBackgroundNonSelectionColor(Color newColor) {
        backgroundNonSelectionColor = newColor;
    }

    /**
      * Returns the background color to be used for non selected nodes.
      *
      * @return the background color to be used for non selected nodes.
      */
    public Color getBackgroundNonSelectionColor() {
        return backgroundNonSelectionColor;
    }

    /**
      * Sets the color to use for the border.
      *
      * @param newColor color to be used for the border
      */
    public void setBorderSelectionColor(Color newColor) {
        borderSelectionColor = newColor;
    }

    /**
      * Returns the color the border is drawn.
      *
      * @return the color the border is drawn
      */
    public Color getBorderSelectionColor() {
        return borderSelectionColor;
    }

    /**
     * Subclassed to map <code>FontUIResource</code>s to null. If
     * <code>font</code> is null, or a <code>FontUIResource</code>, this
     * has the effect of letting the font of the JTree show
     * through. On the other hand, if <code>font</code> is non-null, and not
     * a <code>FontUIResource</code>, the font becomes <code>font</code>.
     */
    public void setFont(Font font) {
        if(font instanceof FontUIResource)
            font = null;
        super.setFont(font);
    }

    /**
     * Gets the font of this component.
     * @return this component's font; if a font has not been set
     * for this component, the font of its parent is returned
     */
    public Font getFont() {
        Font font = super.getFont();

        if (font == null && tree != null) {
            // Strive to return a non-null value, otherwise the html support
            // will typically pick up the wrong font in certain situations.
            font = tree.getFont();
        }
        return font;
    }

    /**
     * Subclassed to map <code>ColorUIResource</code>s to null. If
     * <code>color</code> is null, or a <code>ColorUIResource</code>, this
     * has the effect of letting the background color of the JTree show
     * through. On the other hand, if <code>color</code> is non-null, and not
     * a <code>ColorUIResource</code>, the background becomes
     * <code>color</code>.
     */
    public void setBackground(Color color) {
        if(color instanceof ColorUIResource)
            color = null;
        super.setBackground(color);
    }

    /**
      * Configures the renderer based on the passed in components.
      * The value is set from messaging the tree with
      * <code>convertValueToText</code>, which ultimately invokes
      * <code>toString</code> on <code>value</code>.
      * The foreground color is set based on the selection and the icon
      * is set based on the <code>leaf</code> and <code>expanded</code>
      * parameters.
      */
    public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                  boolean sel,
                                                  boolean expanded,
                                                  boolean leaf, int row,
                                                  boolean hasFocus) {
        String         stringValue = tree.convertValueToText(value, sel,
                                          expanded, leaf, row, hasFocus);

        this.tree = tree;
        this.hasFocus = hasFocus;
        setText(stringValue);

        Color fg = null;
        isDropCell = false;

        JTree.DropLocation dropLocation = tree.getDropLocation();
        if (dropLocation != null
                && dropLocation.getChildIndex() == -1
                && tree.getRowForPath(dropLocation.getPath()) == row) {

            Color col = DefaultLookup.getColor(this, ui, "Tree.dropCellForeground");
            if (col != null) {
                fg = col;
            } else {
                fg = getTextSelectionColor();
            }

            isDropCell = true;
        } else if (sel) {
            fg = getTextSelectionColor();
        } else {
            fg = getTextNonSelectionColor();
        }

        setForeground(fg);

        Icon icon = null;
        if (leaf) {
            icon = getLeafIcon();
        } else if (expanded) {
            icon = getOpenIcon();
        } else {
            icon = getClosedIcon();
        }

        if (!tree.isEnabled()) {
            setEnabled(false);
            LookAndFeel laf = UIManager.getLookAndFeel();
            Icon disabledIcon = laf.getDisabledIcon(tree, icon);
            if (disabledIcon != null) icon = disabledIcon;
            setDisabledIcon(icon);
        } else {
            setEnabled(true);
            setIcon(icon);
        }
        setComponentOrientation(tree.getComponentOrientation());

        selected = sel;

        return this;
    }

    /**
      * Paints the value.  The background is filled based on selected.
      */
    public void paint(Graphics g) {
        Color bColor;

        if (isDropCell) {
            bColor = DefaultLookup.getColor(this, ui, "Tree.dropCellBackground");
            if (bColor == null) {
                bColor = getBackgroundSelectionColor();
            }
        } else if (selected) {
            bColor = getBackgroundSelectionColor();
        } else {
            bColor = getBackgroundNonSelectionColor();
            if (bColor == null) {
                bColor = getBackground();
            }
        }

        int imageOffset = -1;
        if (bColor != null && fillBackground) {
            imageOffset = getLabelStart();
            g.setColor(bColor);
            if(getComponentOrientation().isLeftToRight()) {
                g.fillRect(imageOffset, 0, getWidth() - imageOffset,
                           getHeight());
            } else {
                g.fillRect(0, 0, getWidth() - imageOffset,
                           getHeight());
            }
        }

        if (hasFocus) {
            if (drawsFocusBorderAroundIcon) {
                imageOffset = 0;
            }
            else if (imageOffset == -1) {
                imageOffset = getLabelStart();
            }
            if(getComponentOrientation().isLeftToRight()) {
                paintFocus(g, imageOffset, 0, getWidth() - imageOffset,
                           getHeight(), bColor);
            } else {
                paintFocus(g, 0, 0, getWidth() - imageOffset, getHeight(), bColor);
            }
        }
        super.paint(g);
    }

    private void paintFocus(Graphics g, int x, int y, int w, int h, Color notColor) {
        Color       bsColor = getBorderSelectionColor();

        if (bsColor != null && (selected || !drawDashedFocusIndicator)) {
            g.setColor(bsColor);
            g.drawRect(x, y, w - 1, h - 1);
        }
        if (drawDashedFocusIndicator && notColor != null) {
            if (treeBGColor != notColor) {
                treeBGColor = notColor;
                focusBGColor = new Color(~notColor.getRGB());
            }
            g.setColor(focusBGColor);
            BasicGraphicsUtils.drawDashedRect(g, x, y, w, h);
        }
    }

    private int getLabelStart() {
        Icon currentI = getIcon();
        if(currentI != null && getText() != null) {
            return currentI.getIconWidth() + Math.max(0, getIconTextGap() - 1);
        }
        return 0;
    }

    /**
     * Overrides <code>JComponent.getPreferredSize</code> to
     * return slightly wider preferred size value.
     */
    public Dimension getPreferredSize() {
        Dimension        retDimension = super.getPreferredSize();

        if(retDimension != null)
            retDimension = new Dimension(retDimension.width + 3,
                                         retDimension.height);
        return retDimension;
    }

   /**
    * Overridden for performance reasons.
    * See the <a href="#override">Implementation Note</a>
    * for more information.
    */
    public void validate() {}

   /**
    * Overridden for performance reasons.
    * See the <a href="#override">Implementation Note</a>
    * for more information.
    *
    * @since 1.5
    */
    public void invalidate() {}

   /**
    * Overridden for performance reasons.
    * See the <a href="#override">Implementation Note</a>
    * for more information.
    */
    public void revalidate() {}

   /**
    * Overridden for performance reasons.
    * See the <a href="#override">Implementation Note</a>
    * for more information.
    */
    public void repaint(long tm, int x, int y, int width, int height) {}

   /**
    * Overridden for performance reasons.
    * See the <a href="#override">Implementation Note</a>
    * for more information.
    */
    public void repaint(Rectangle r) {}

   /**
    * Overridden for performance reasons.
    * See the <a href="#override">Implementation Note</a>
    * for more information.
    *
    * @since 1.5
    */
    public void repaint() {}

   /**
    * Overridden for performance reasons.
    * See the <a href="#override">Implementation Note</a>
    * for more information.
    */
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        // Strings get interned...
        if (propertyName == "text"
            || ((SwingUtilities2.isScaleChanged(propertyName, oldValue, newValue)
                    || propertyName == "font" || propertyName == "foreground")
                && oldValue != newValue
                && getClientProperty(javax.swing.plaf.basic.BasicHTML.propertyKey) != null)) {

            super.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

   /**
    * Overridden for performance reasons.
    * See the <a href="#override">Implementation Note</a>
    * for more information.
    */
    public void firePropertyChange(String propertyName, byte oldValue, byte newValue) {}

   /**
    * Overridden for performance reasons.
    * See the <a href="#override">Implementation Note</a>
    * for more information.
    */
    public void firePropertyChange(String propertyName, char oldValue, char newValue) {}

   /**
    * Overridden for performance reasons.
    * See the <a href="#override">Implementation Note</a>
    * for more information.
    */
    public void firePropertyChange(String propertyName, short oldValue, short newValue) {}

   /**
    * Overridden for performance reasons.
    * See the <a href="#override">Implementation Note</a>
    * for more information.
    */
    public void firePropertyChange(String propertyName, int oldValue, int newValue) {}

   /**
    * Overridden for performance reasons.
    * See the <a href="#override">Implementation Note</a>
    * for more information.
    */
    public void firePropertyChange(String propertyName, long oldValue, long newValue) {}

   /**
    * Overridden for performance reasons.
    * See the <a href="#override">Implementation Note</a>
    * for more information.
    */
    public void firePropertyChange(String propertyName, float oldValue, float newValue) {}

   /**
    * Overridden for performance reasons.
    * See the <a href="#override">Implementation Note</a>
    * for more information.
    */
    public void firePropertyChange(String propertyName, double oldValue, double newValue) {}

   /**
    * Overridden for performance reasons.
    * See the <a href="#override">Implementation Note</a>
    * for more information.
    */
    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {}

}
