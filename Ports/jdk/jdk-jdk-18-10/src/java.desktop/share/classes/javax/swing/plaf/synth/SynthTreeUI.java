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

package javax.swing.plaf.synth;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Enumeration;

import javax.swing.DefaultCellEditor;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.LookAndFeel;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 * Provides the Synth L&amp;F UI delegate for
 * {@link javax.swing.JTree}.
 *
 * @author Scott Violet
 * @since 1.7
 */
public class SynthTreeUI extends BasicTreeUI
                         implements PropertyChangeListener, SynthUI {
    private SynthStyle style;
    private SynthStyle cellStyle;

    private SynthContext paintContext;

    private boolean drawHorizontalLines;
    private boolean drawVerticalLines;

    private Object linesStyle;

    private int padding;

    private boolean useTreeColors;

    private Icon expandedIconWrapper = new ExpandedIconWrapper();

    /**
     *
     * Constructs a {@code SynthTreeUI}.
     */
    public SynthTreeUI() {}

    /**
     * Creates a new UI object for the given component.
     *
     * @param x component to create UI object for
     * @return the UI object
     */
    public static ComponentUI createUI(JComponent x) {
        return new SynthTreeUI();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Icon getExpandedIcon() {
        return expandedIconWrapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void installDefaults() {
        updateStyle(tree);
    }

    private void updateStyle(JTree tree) {
        SynthContext context = getContext(tree, ENABLED);
        SynthStyle oldStyle = style;

        style = SynthLookAndFeel.updateStyle(context, this);
        if (style != oldStyle) {
            Object value;

            setExpandedIcon(style.getIcon(context, "Tree.expandedIcon"));
            setCollapsedIcon(style.getIcon(context, "Tree.collapsedIcon"));

            setLeftChildIndent(style.getInt(context, "Tree.leftChildIndent",
                                            0));
            setRightChildIndent(style.getInt(context, "Tree.rightChildIndent",
                                             0));

            drawHorizontalLines = style.getBoolean(
                          context, "Tree.drawHorizontalLines",true);
            drawVerticalLines = style.getBoolean(
                        context, "Tree.drawVerticalLines", true);
            linesStyle = style.get(context, "Tree.linesStyle");

                value = style.get(context, "Tree.rowHeight");
                if (value != null) {
                    LookAndFeel.installProperty(tree, "rowHeight", value);
                }

                value = style.get(context, "Tree.scrollsOnExpand");
                LookAndFeel.installProperty(tree, "scrollsOnExpand",
                                                    value != null? value : Boolean.TRUE);

            padding = style.getInt(context, "Tree.padding", 0);

            largeModel = (tree.isLargeModel() && tree.getRowHeight() > 0);

            useTreeColors = style.getBoolean(context,
                                  "Tree.rendererUseTreeColors", true);

            Boolean showsRootHandles = style.getBoolean(
                    context, "Tree.showsRootHandles", Boolean.TRUE);
            LookAndFeel.installProperty(
                    tree, JTree.SHOWS_ROOT_HANDLES_PROPERTY, showsRootHandles);

            if (oldStyle != null) {
                uninstallKeyboardActions();
                installKeyboardActions();
            }
        }

        context = getContext(tree, Region.TREE_CELL, ENABLED);
        cellStyle = SynthLookAndFeel.updateStyle(context, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void installListeners() {
        super.installListeners();
        tree.addPropertyChangeListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SynthContext getContext(JComponent c) {
        return getContext(c, SynthLookAndFeel.getComponentState(c));
    }

    private SynthContext getContext(JComponent c, int state) {
        return SynthContext.getContext(c, style, state);
    }

    private SynthContext getContext(JComponent c, Region region) {
        return getContext(c, region, getComponentState(c, region));
    }

    private SynthContext getContext(JComponent c, Region region, int state) {
        return SynthContext.getContext(c, region, cellStyle, state);
    }

    private int getComponentState(JComponent c, Region region) {
        // Always treat the cell as selected, will be adjusted appropriately
        // when painted.
        return ENABLED | SELECTED;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected TreeCellEditor createDefaultCellEditor() {
        TreeCellRenderer renderer = tree.getCellRenderer();
        DefaultTreeCellEditor editor;

        if(renderer != null && (renderer instanceof DefaultTreeCellRenderer)) {
            editor = new SynthTreeCellEditor(tree, (DefaultTreeCellRenderer)
                                             renderer);
        }
        else {
            editor = new SynthTreeCellEditor(tree, null);
        }
        return editor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected TreeCellRenderer createDefaultCellRenderer() {
        return new SynthTreeCellRenderer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void uninstallDefaults() {
        SynthContext context = getContext(tree, ENABLED);

        style.uninstallDefaults(context);
        style = null;

        context = getContext(tree, Region.TREE_CELL, ENABLED);
        cellStyle.uninstallDefaults(context);
        cellStyle = null;


        if (tree.getTransferHandler() instanceof UIResource) {
            tree.setTransferHandler(null);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void uninstallListeners() {
        super.uninstallListeners();
        tree.removePropertyChangeListener(this);
    }

    /**
     * Notifies this UI delegate to repaint the specified component.
     * This method paints the component background, then calls
     * the {@link #paint(SynthContext,Graphics)} method.
     *
     * <p>In general, this method does not need to be overridden by subclasses.
     * All Look and Feel rendering code should reside in the {@code paint} method.
     *
     * @param g the {@code Graphics} object used for painting
     * @param c the component being painted
     * @see #paint(SynthContext,Graphics)
     */
    @Override
    public void update(Graphics g, JComponent c) {
        SynthContext context = getContext(c);

        SynthLookAndFeel.update(context, g);
        context.getPainter().paintTreeBackground(context,
                          g, 0, 0, c.getWidth(), c.getHeight());
        paint(context, g);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paintBorder(SynthContext context, Graphics g, int x,
                            int y, int w, int h) {
        context.getPainter().paintTreeBorder(context, g, x, y, w, h);
    }

    /**
     * Paints the specified component according to the Look and Feel.
     * <p>This method is not used by Synth Look and Feel.
     * Painting is handled by the {@link #paint(SynthContext,Graphics)} method.
     *
     * @param g the {@code Graphics} object used for painting
     * @param c the component being painted
     * @see #paint(SynthContext,Graphics)
     */
    @Override
    public void paint(Graphics g, JComponent c) {
        SynthContext context = getContext(c);

        paint(context, g);
    }

    /**
     * Paints the specified component.
     *
     * @param context context for the component being painted
     * @param g the {@code Graphics} object used for painting
     * @see #update(Graphics,JComponent)
     */
    protected void paint(SynthContext context, Graphics g) {
        paintContext = context;

        updateLeadSelectionRow();

        Rectangle paintBounds = g.getClipBounds();
        Insets insets = tree.getInsets();
        TreePath initialPath = getClosestPathForLocation(tree, 0,
                                                         paintBounds.y);
        Enumeration<?> paintingEnumerator = treeState.getVisiblePathsFrom
                                              (initialPath);
        int row = treeState.getRowForPath(initialPath);
        int endY = paintBounds.y + paintBounds.height;
        TreeModel treeModel = tree.getModel();
        SynthContext cellContext = getContext(tree, Region.TREE_CELL);

        drawingCache.clear();

        setHashColor(context.getStyle().getColor(context,
                                                ColorType.FOREGROUND));

        if (paintingEnumerator != null) {
            // First pass, draw the rows

            boolean done = false;
            boolean isExpanded;
            boolean hasBeenExpanded;
            boolean isLeaf;
            Rectangle rowBounds = new Rectangle(0, 0, tree.getWidth(),0);
            Rectangle bounds;
            TreePath path;
            TreeCellRenderer renderer = tree.getCellRenderer();
            DefaultTreeCellRenderer dtcr = (renderer instanceof
                       DefaultTreeCellRenderer) ? (DefaultTreeCellRenderer)
                       renderer : null;

            configureRenderer(cellContext);
            while (!done && paintingEnumerator.hasMoreElements()) {
                path = (TreePath)paintingEnumerator.nextElement();
                bounds = getPathBounds(tree, path);
                if ((path != null) && (bounds != null)) {
                    isLeaf = treeModel.isLeaf(path.getLastPathComponent());
                    if (isLeaf) {
                        isExpanded = hasBeenExpanded = false;
                    }
                    else {
                        isExpanded = treeState.getExpandedState(path);
                        hasBeenExpanded = tree.hasBeenExpanded(path);
                    }
                    rowBounds.y = bounds.y;
                    rowBounds.height = bounds.height;
                    paintRow(renderer, dtcr, context, cellContext, g,
                             paintBounds, insets, bounds, rowBounds, path,
                             row, isExpanded, hasBeenExpanded, isLeaf);
                    if ((bounds.y + bounds.height) >= endY) {
                        done = true;
                    }
                }
                else {
                    done = true;
                }
                row++;
            }

            // Draw the connecting lines and controls.
            // Find each parent and have them draw a line to their last child
            boolean rootVisible = tree.isRootVisible();
            TreePath parentPath = initialPath;
            parentPath = parentPath.getParentPath();
            while (parentPath != null) {
                paintVerticalPartOfLeg(g, paintBounds, insets, parentPath);
                drawingCache.put(parentPath, Boolean.TRUE);
                parentPath = parentPath.getParentPath();
            }
            done = false;
            paintingEnumerator = treeState.getVisiblePathsFrom(initialPath);
            while (!done && paintingEnumerator.hasMoreElements()) {
                path = (TreePath)paintingEnumerator.nextElement();
                bounds = getPathBounds(tree, path);
                if ((path != null) && (bounds != null)) {
                    isLeaf = treeModel.isLeaf(path.getLastPathComponent());
                    if (isLeaf) {
                        isExpanded = hasBeenExpanded = false;
                    }
                    else {
                        isExpanded = treeState.getExpandedState(path);
                        hasBeenExpanded = tree.hasBeenExpanded(path);
                    }
                    // See if the vertical line to the parent has been drawn.
                    parentPath = path.getParentPath();
                    if (parentPath != null) {
                        if (drawingCache.get(parentPath) == null) {
                            paintVerticalPartOfLeg(g, paintBounds, insets,
                                                   parentPath);
                            drawingCache.put(parentPath, Boolean.TRUE);
                        }
                        paintHorizontalPartOfLeg(g,
                                                 paintBounds, insets, bounds,
                                                 path, row, isExpanded,
                                                 hasBeenExpanded, isLeaf);
                    }
                    else if (rootVisible && row == 0) {
                        paintHorizontalPartOfLeg(g,
                                                 paintBounds, insets, bounds,
                                                 path, row, isExpanded,
                                                 hasBeenExpanded, isLeaf);
                    }
                    if (shouldPaintExpandControl(path, row, isExpanded,
                                                 hasBeenExpanded, isLeaf)) {
                        paintExpandControl(g, paintBounds,
                                           insets, bounds, path, row,
                                           isExpanded, hasBeenExpanded,isLeaf);
                    }
                    if ((bounds.y + bounds.height) >= endY) {
                        done = true;
                    }
                }
                else {
                    done = true;
                }
                row++;
            }
        }

        paintDropLine(g);

        // Empty out the renderer pane, allowing renderers to be gc'ed.
        rendererPane.removeAll();

        paintContext = null;
    }

    private void configureRenderer(SynthContext context) {
        TreeCellRenderer renderer = tree.getCellRenderer();

        if (renderer instanceof DefaultTreeCellRenderer) {
            DefaultTreeCellRenderer r = (DefaultTreeCellRenderer)renderer;
            SynthStyle style = context.getStyle();

            context.setComponentState(ENABLED | SELECTED);
            Color color = r.getTextSelectionColor();
            if (color == null || (color instanceof UIResource)) {
                r.setTextSelectionColor(style.getColor(
                                     context, ColorType.TEXT_FOREGROUND));
            }
            color = r.getBackgroundSelectionColor();
            if (color == null || (color instanceof UIResource)) {
                r.setBackgroundSelectionColor(style.getColor(
                                        context, ColorType.TEXT_BACKGROUND));
            }

            context.setComponentState(ENABLED);
            color = r.getTextNonSelectionColor();
            if (color == null || color instanceof UIResource) {
                r.setTextNonSelectionColor(style.getColorForState(
                                        context, ColorType.TEXT_FOREGROUND));
            }
            color = r.getBackgroundNonSelectionColor();
            if (color == null || color instanceof UIResource) {
                r.setBackgroundNonSelectionColor(style.getColorForState(
                                  context, ColorType.TEXT_BACKGROUND));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void paintHorizontalPartOfLeg(Graphics g, Rectangle clipBounds,
                                            Insets insets, Rectangle bounds,
                                            TreePath path, int row,
                                            boolean isExpanded,
                                            boolean hasBeenExpanded, boolean
                                            isLeaf) {
        if (drawHorizontalLines) {
            super.paintHorizontalPartOfLeg(g, clipBounds, insets, bounds,
                                           path, row, isExpanded,
                                           hasBeenExpanded, isLeaf);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void paintHorizontalLine(Graphics g, JComponent c, int y,
                                      int left, int right) {
        paintContext.getStyle().getGraphicsUtils(paintContext).drawLine(
            paintContext, "Tree.horizontalLine", g, left, y, right, y, linesStyle);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void paintVerticalPartOfLeg(Graphics g,
                                          Rectangle clipBounds, Insets insets,
                                          TreePath path) {
        if (drawVerticalLines) {
            super.paintVerticalPartOfLeg(g, clipBounds, insets, path);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void paintVerticalLine(Graphics g, JComponent c, int x, int top,
                                    int bottom) {
        paintContext.getStyle().getGraphicsUtils(paintContext).drawLine(
            paintContext, "Tree.verticalLine", g, x, top, x, bottom, linesStyle);
    }

    private void paintRow(TreeCellRenderer renderer,
               DefaultTreeCellRenderer dtcr, SynthContext treeContext,
               SynthContext cellContext, Graphics g, Rectangle clipBounds,
               Insets insets, Rectangle bounds, Rectangle rowBounds,
               TreePath path, int row, boolean isExpanded,
               boolean hasBeenExpanded, boolean isLeaf) {
        // Don't paint the renderer if editing this row.
        boolean selected = tree.isRowSelected(row);

        JTree.DropLocation dropLocation = tree.getDropLocation();
        boolean isDrop = dropLocation != null
                         && dropLocation.getChildIndex() == -1
                         && path == dropLocation.getPath();

        int state = ENABLED;
        if (selected || isDrop) {
            state |= SELECTED;
        }

        if (tree.isFocusOwner() && row == getLeadSelectionRow()) {
            state |= FOCUSED;
        }

        cellContext.setComponentState(state);

        if (dtcr != null && (dtcr.getBorderSelectionColor() instanceof
                             UIResource)) {
            dtcr.setBorderSelectionColor(style.getColor(
                                             cellContext, ColorType.FOCUS));
        }
        SynthLookAndFeel.updateSubregion(cellContext, g, rowBounds);
        cellContext.getPainter().paintTreeCellBackground(cellContext, g,
                    rowBounds.x, rowBounds.y, rowBounds.width,
                    rowBounds.height);
        cellContext.getPainter().paintTreeCellBorder(cellContext, g,
                    rowBounds.x, rowBounds.y, rowBounds.width,
                    rowBounds.height);
        if (editingComponent != null && editingRow == row) {
            return;
        }

        int leadIndex;

        if (tree.hasFocus()) {
            leadIndex = getLeadSelectionRow();
        }
        else {
            leadIndex = -1;
        }

        Component component = renderer.getTreeCellRendererComponent(
                         tree, path.getLastPathComponent(),
                         selected, isExpanded, isLeaf, row,
                         (leadIndex == row));

        rendererPane.paintComponent(g, component, tree, bounds.x, bounds.y,
                                    bounds.width, bounds.height, true);
    }

    private int findCenteredX(int x, int iconWidth) {
        return tree.getComponentOrientation().isLeftToRight()
               ? x - (int)Math.ceil(iconWidth / 2.0)
               : x - (int)Math.floor(iconWidth / 2.0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void paintExpandControl(Graphics g, Rectangle clipBounds,
            Insets insets, Rectangle bounds, TreePath path, int row,
            boolean isExpanded, boolean hasBeenExpanded, boolean isLeaf) {
        //modify the paintContext's state to match the state for the row
        //this is a hack in that it requires knowledge of the subsequent
        //method calls. The point is, the context used in drawCentered
        //should reflect the state of the row, not of the tree.
        boolean isSelected = tree.getSelectionModel().isPathSelected(path);
        int state = paintContext.getComponentState();
        if (isSelected) {
            paintContext.setComponentState(state | SynthConstants.SELECTED);
        }
        super.paintExpandControl(g, clipBounds, insets, bounds, path, row,
                isExpanded, hasBeenExpanded, isLeaf);
        paintContext.setComponentState(state);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void drawCentered(Component c, Graphics graphics, Icon icon,
                                int x, int y) {
        int w = SynthGraphicsUtils.getIconWidth(icon, paintContext);
        int h = SynthGraphicsUtils.getIconHeight(icon, paintContext);

        SynthGraphicsUtils.paintIcon(icon, paintContext, graphics,
                            findCenteredX(x, w),
                            y - h/2, w, h);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if (SynthLookAndFeel.shouldUpdateStyle(event)) {
            updateStyle((JTree)event.getSource());
        }

        if ("dropLocation" == event.getPropertyName()) {
            JTree.DropLocation oldValue = (JTree.DropLocation)event.getOldValue();
            repaintDropLocation(oldValue);
            repaintDropLocation(tree.getDropLocation());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void paintDropLine(Graphics g) {
        JTree.DropLocation loc = tree.getDropLocation();
        if (!isDropLine(loc)) {
            return;
        }

        Color c = (Color)style.get(paintContext, "Tree.dropLineColor");
        if (c != null) {
            g.setColor(c);
            Rectangle rect = getDropLineRect(loc);
            g.fillRect(rect.x, rect.y, rect.width, rect.height);
        }
    }

    private void repaintDropLocation(JTree.DropLocation loc) {
        if (loc == null) {
            return;
        }

        Rectangle r;

        if (isDropLine(loc)) {
            r = getDropLineRect(loc);
        } else {
            r = tree.getPathBounds(loc.getPath());
            if (r != null) {
                r.x = 0;
                r.width = tree.getWidth();
            }
        }

        if (r != null) {
            tree.repaint(r);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getRowX(int row, int depth) {
        return super.getRowX(row, depth) + padding;
    }

    /**
     * A subclass of {@code DefaultTreeCellRenderer} that implements
     * {@code UIResource}.
     */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    private class SynthTreeCellRenderer extends DefaultTreeCellRenderer
                               implements UIResource {
        SynthTreeCellRenderer() {
        }

        @Override
        public String getName() {
            return "Tree.cellRenderer";
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                      boolean sel,
                                                      boolean expanded,
                                                      boolean leaf, int row,
                                                      boolean hasFocus) {
            if (!useTreeColors && (sel || hasFocus)) {
                SynthLookAndFeel.setSelectedUI((SynthLabelUI)SynthLookAndFeel.
                             getUIOfType(getUI(), SynthLabelUI.class),
                                   sel, hasFocus, tree.isEnabled(), false);
            }
            else {
                SynthLookAndFeel.resetSelectedUI();
            }
            return super.getTreeCellRendererComponent(tree, value, sel,
                                                      expanded, leaf, row, hasFocus);
        }

        @Override
        public void paint(Graphics g) {
            paintComponent(g);
            if (hasFocus) {
                SynthContext context = getContext(tree, Region.TREE_CELL);

                if (context.getStyle() == null) {
                    assert false: "SynthTreeCellRenderer is being used " +
                        "outside of UI that created it";
                    return;
                }
                int imageOffset = 0;
                Icon currentI = getIcon();

                if(currentI != null && getText() != null) {
                    imageOffset = currentI.getIconWidth() +
                                          Math.max(0, getIconTextGap() - 1);
                }
                if (selected) {
                    context.setComponentState(ENABLED | SELECTED);
                }
                else {
                    context.setComponentState(ENABLED);
                }
                if(getComponentOrientation().isLeftToRight()) {
                    context.getPainter().paintTreeCellFocus(context, g,
                            imageOffset, 0, getWidth() - imageOffset,
                            getHeight());
                }
                else {
                    context.getPainter().paintTreeCellFocus(context, g,
                            0, 0, getWidth() - imageOffset, getHeight());
                }
            }
            SynthLookAndFeel.resetSelectedUI();
        }
    }


    private static class SynthTreeCellEditor extends DefaultTreeCellEditor {
        public SynthTreeCellEditor(JTree tree,
                                   DefaultTreeCellRenderer renderer) {
            super(tree, renderer);
            setBorderSelectionColor(null);
        }

        @Override
        protected TreeCellEditor createTreeCellEditor() {
            @SuppressWarnings("serial") // anonymous class
            JTextField tf = new JTextField() {
                @Override
                public String getName() {
                    return "Tree.cellEditor";
                }
            };
            DefaultCellEditor editor = new DefaultCellEditor(tf);

            // One click to edit.
            editor.setClickCountToStart(1);
            return editor;
        }
    }

    //
    // BasicTreeUI directly uses expandIcon outside of the Synth methods.
    // To get the correct context we return an instance of this that fetches
    // the SynthContext as needed.
    //
    private class ExpandedIconWrapper implements SynthIcon {
        public void paintIcon(SynthContext context, Graphics g, int x,
                              int y, int w, int h) {
            if (context == null) {
                context = getContext(tree);
                SynthGraphicsUtils.paintIcon(expandedIcon, context, g, x, y, w, h);
            }
            else {
                SynthGraphicsUtils.paintIcon(expandedIcon, context, g, x, y, w, h);
            }
        }

        public int getIconWidth(SynthContext context) {
            int width;
            if (context == null) {
                context = getContext(tree);
                width = SynthGraphicsUtils.getIconWidth(expandedIcon, context);
            }
            else {
                width = SynthGraphicsUtils.getIconWidth(expandedIcon, context);
            }
            return width;
        }

        public int getIconHeight(SynthContext context) {
            int height;
            if (context == null) {
                context = getContext(tree);
                height = SynthGraphicsUtils.getIconHeight(expandedIcon, context);
            }
            else {
                height = SynthGraphicsUtils.getIconHeight(expandedIcon, context);
            }
            return height;
        }
    }
}
