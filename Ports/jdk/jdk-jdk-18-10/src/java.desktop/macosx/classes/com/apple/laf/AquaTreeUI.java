/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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
import java.beans.*;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.*;

import com.apple.laf.AquaUtils.RecyclableSingleton;

import apple.laf.*;
import apple.laf.JRSUIConstants.*;
import apple.laf.JRSUIState.AnimationFrameState;

/**
 * AquaTreeUI supports the client property "value-add" system of customization See MetalTreeUI
 * This is heavily based on the 1.3.1 AquaTreeUI implementation.
 */
public class AquaTreeUI extends BasicTreeUI {

    // Create PLAF
    public static ComponentUI createUI(final JComponent c) {
        return new AquaTreeUI();
    }

    // Begin Line Stuff from Metal

    private static final String LINE_STYLE = "JTree.lineStyle";

    private static final String LEG_LINE_STYLE_STRING = "Angled";
    private static final String HORIZ_STYLE_STRING = "Horizontal";
    private static final String NO_STYLE_STRING = "None";

    private static final int LEG_LINE_STYLE = 2;
    private static final int HORIZ_LINE_STYLE = 1;
    private static final int NO_LINE_STYLE = 0;

    private int lineStyle = HORIZ_LINE_STYLE;
    private final PropertyChangeListener lineStyleListener = new LineListener();

    // mouse tracking state
    protected TreePath fTrackingPath;
    protected boolean fIsPressed = false;
    protected boolean fIsInBounds = false;
    protected int fAnimationFrame = -1;
    protected TreeArrowMouseInputHandler fMouseHandler;

    protected final AquaPainter<AnimationFrameState> painter = AquaPainter.create(JRSUIStateFactory.getDisclosureTriangle());

    public AquaTreeUI() {

    }

    public void installUI(final JComponent c) {
        super.installUI(c);

        final Object lineStyleFlag = c.getClientProperty(LINE_STYLE);
        decodeLineStyle(lineStyleFlag);
        c.addPropertyChangeListener(lineStyleListener);
    }

    public void uninstallUI(final JComponent c) {
        c.removePropertyChangeListener(lineStyleListener);
        super.uninstallUI(c);
    }

    /**
     * Creates the focus listener to repaint the focus ring
     */
    protected FocusListener createFocusListener() {
        return new AquaTreeUI.FocusHandler();
    }

    /**
     * this function converts between the string passed into the client property and the internal representation
     * (currently an int)
     */
    protected void decodeLineStyle(final Object lineStyleFlag) {
        if (lineStyleFlag == null || NO_STYLE_STRING.equals(lineStyleFlag)) {
            lineStyle = NO_LINE_STYLE; // default case
            return;
        }

        if (LEG_LINE_STYLE_STRING.equals(lineStyleFlag)) {
            lineStyle = LEG_LINE_STYLE;
        } else if (HORIZ_STYLE_STRING.equals(lineStyleFlag)) {
            lineStyle = HORIZ_LINE_STYLE;
        }
    }

    public TreePath getClosestPathForLocation(final JTree treeLocal, final int x, final int y) {
        if (treeLocal == null || treeState == null) return null;

        Insets i = treeLocal.getInsets();
        if (i == null) i = new Insets(0, 0, 0, 0);
        return treeState.getPathClosestTo(x - i.left, y - i.top);
    }

    public void paint(final Graphics g, final JComponent c) {
        super.paint(g, c);

        // Paint the lines
        if (lineStyle == HORIZ_LINE_STYLE && !largeModel) {
            paintHorizontalSeparators(g, c);
        }
    }

    protected void paintHorizontalSeparators(final Graphics g, final JComponent c) {
        g.setColor(UIManager.getColor("Tree.line"));

        final Rectangle clipBounds = g.getClipBounds();

        final int beginRow = getRowForPath(tree, getClosestPathForLocation(tree, 0, clipBounds.y));
        final int endRow = getRowForPath(tree, getClosestPathForLocation(tree, 0, clipBounds.y + clipBounds.height - 1));

        if (beginRow <= -1 || endRow <= -1) { return; }

        for (int i = beginRow; i <= endRow; ++i) {
            final TreePath path = getPathForRow(tree, i);

            if (path != null && path.getPathCount() == 2) {
                final Rectangle rowBounds = getPathBounds(tree, getPathForRow(tree, i));

                // Draw a line at the top
                if (rowBounds != null) g.drawLine(clipBounds.x, rowBounds.y, clipBounds.x + clipBounds.width, rowBounds.y);
            }
        }
    }

    protected void paintVerticalPartOfLeg(final Graphics g, final Rectangle clipBounds, final Insets insets, final TreePath path) {
        if (lineStyle == LEG_LINE_STYLE) {
            super.paintVerticalPartOfLeg(g, clipBounds, insets, path);
        }
    }

    protected void paintHorizontalPartOfLeg(final Graphics g, final Rectangle clipBounds, final Insets insets, final Rectangle bounds, final TreePath path, final int row, final boolean isExpanded, final boolean hasBeenExpanded, final boolean isLeaf) {
        if (lineStyle == LEG_LINE_STYLE) {
            super.paintHorizontalPartOfLeg(g, clipBounds, insets, bounds, path, row, isExpanded, hasBeenExpanded, isLeaf);
        }
    }

    /** This class listens for changes in line style */
    class LineListener implements PropertyChangeListener {
        public void propertyChange(final PropertyChangeEvent e) {
            final String name = e.getPropertyName();
            if (name.equals(LINE_STYLE)) {
                decodeLineStyle(e.getNewValue());
            }
        }
    }

    /**
     * Paints the expand (toggle) part of a row. The receiver should NOT modify {@code clipBounds}, or
     * {@code insets}.
     */
    protected void paintExpandControl(final Graphics g, final Rectangle clipBounds, final Insets insets, final Rectangle bounds, final TreePath path, final int row, final boolean isExpanded, final boolean hasBeenExpanded, final boolean isLeaf) {
        final Object value = path.getLastPathComponent();

        // Draw icons if not a leaf and either hasn't been loaded,
        // or the model child count is > 0.
        if (isLeaf || (hasBeenExpanded && treeModel.getChildCount(value) <= 0)) return;

        final boolean isLeftToRight = AquaUtils.isLeftToRight(tree); // Basic knows, but keeps it private

        final State state = getState(path);

        // if we are not animating, do the expected thing, and use the icon
        // also, if there is a custom (non-LaF defined) icon - just use that instead
        if (fAnimationFrame == -1 && state != State.PRESSED) {
            super.paintExpandControl(g, clipBounds, insets, bounds, path, row, isExpanded, hasBeenExpanded, isLeaf);
            return;
        }

        // Both icons are the same size
        final Icon icon = isExpanded ? getExpandedIcon() : getCollapsedIcon();
        if (!(icon instanceof UIResource)) {
            super.paintExpandControl(g, clipBounds, insets, bounds, path, row, isExpanded, hasBeenExpanded, isLeaf);
            return;
        }

        // if painting a right-to-left knob, we ensure that we are only painting when
        // the clipbounds rect is set to the exact size of the knob, and positioned correctly
        // (this code is not the same as metal)
        int middleXOfKnob;
        if (isLeftToRight) {
            middleXOfKnob = bounds.x - (getRightChildIndent() - 1);
        } else {
            middleXOfKnob = clipBounds.x + clipBounds.width / 2;
        }

        // Center vertically
        final int middleYOfKnob = bounds.y + (bounds.height / 2);

        final int x = middleXOfKnob - icon.getIconWidth() / 2;
        final int y = middleYOfKnob - icon.getIconHeight() / 2;
        final int height = icon.getIconHeight(); // use the icon height so we don't get drift  we modify the bounds (by changing row height)
        final int width = 20; // this is a hardcoded value from our default icon (since we are only at this point for animation)

        setupPainter(state, isExpanded, isLeftToRight);
        painter.paint(g, tree, x, y, width, height);
    }

    @Override
    public Icon getCollapsedIcon() {
        final Icon icon = super.getCollapsedIcon();
        if (AquaUtils.isLeftToRight(tree)) return icon;
        if (!(icon instanceof UIResource)) return icon;
        return UIManager.getIcon("Tree.rightToLeftCollapsedIcon");
    }

    protected void setupPainter(State state, final boolean isExpanded, final boolean leftToRight) {
        if (!fIsInBounds && state == State.PRESSED) state = State.ACTIVE;

        painter.state.set(state);
        if (JRSUIUtils.Tree.useLegacyTreeKnobs()) {
            if (fAnimationFrame == -1) {
                painter.state.set(isExpanded ? Direction.DOWN : Direction.RIGHT);
            } else {
                painter.state.set(Direction.NONE);
                painter.state.setAnimationFrame(fAnimationFrame - 1);
            }
        } else {
            painter.state.set(getDirection(isExpanded, leftToRight));
            painter.state.setAnimationFrame(fAnimationFrame);
        }
    }

    protected Direction getDirection(final boolean isExpanded, final boolean isLeftToRight) {
        if (isExpanded && (fAnimationFrame == -1)) return Direction.DOWN;
        return isLeftToRight ? Direction.RIGHT : Direction.LEFT;
    }

    protected State getState(final TreePath path) {
        if (!tree.isEnabled()) return State.DISABLED;
        if (fIsPressed) {
            if (fTrackingPath.equals(path)) return State.PRESSED;
        }
        return State.ACTIVE;
    }

    /**
     * Misnamed - this is called on mousePressed Macs shouldn't react till mouseReleased
     * We install a motion handler that gets removed after.
     * See super.MouseInputHandler & super.startEditing for why
     */
    protected void handleExpandControlClick(final TreePath path, final int mouseX, final int mouseY) {
        fMouseHandler = new TreeArrowMouseInputHandler(path);
    }

    /**
     * Returning true signifies a mouse event on the node should toggle the selection of only the row under mouse.
     */
    protected boolean isToggleSelectionEvent(final MouseEvent event) {
        return SwingUtilities.isLeftMouseButton(event) && event.isMetaDown();
    }

    class FocusHandler extends BasicTreeUI.FocusHandler {
        public void focusGained(final FocusEvent e) {
            super.focusGained(e);
            AquaBorder.repaintBorder(tree);
        }

        public void focusLost(final FocusEvent e) {
            super.focusLost(e);
            AquaBorder.repaintBorder(tree);
        }
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new MacPropertyChangeHandler();
    }

    public class MacPropertyChangeHandler extends PropertyChangeHandler {
        public void propertyChange(final PropertyChangeEvent e) {
            final String prop = e.getPropertyName();
            if (prop.equals(AquaFocusHandler.FRAME_ACTIVE_PROPERTY)) {
                AquaBorder.repaintBorder(tree);
                AquaFocusHandler.swapSelectionColors("Tree", tree, e.getNewValue());
            } else {
                super.propertyChange(e);
            }
        }
    }

    /**
     * TreeArrowMouseInputHandler handles passing all mouse events the way a Mac should - hilite/dehilite on enter/exit,
     * only perform the action if released in arrow.
     *
     * Just like super.MouseInputHandler, this is removed once it's not needed, so they won't clash with each other
     */
    // The Adapters take care of defining all the empties
    class TreeArrowMouseInputHandler extends MouseInputAdapter {
        protected Rectangle fPathBounds = new Rectangle();

        // Values needed for paintOneControl
        protected boolean fIsLeaf, fIsExpanded, fHasBeenExpanded;
        protected Rectangle fBounds, fVisibleRect;
        int fTrackingRow;
        Insets fInsets;
        Color fBackground;

        TreeArrowMouseInputHandler(final TreePath path) {
            fTrackingPath = path;
            fIsPressed = true;
            fIsInBounds = true;
            this.fPathBounds = getPathArrowBounds(path);
            tree.addMouseListener(this);
            tree.addMouseMotionListener(this);
            fBackground = tree.getBackground();
            if (!tree.isOpaque()) {
                final Component p = tree.getParent();
                if (p != null) fBackground = p.getBackground();
            }

            // Set up values needed to paint the triangle - see
            // BasicTreeUI.paint
            fVisibleRect = tree.getVisibleRect();
            fInsets = tree.getInsets();

            if (fInsets == null) fInsets = new Insets(0, 0, 0, 0);
            fIsLeaf = treeModel.isLeaf(path.getLastPathComponent());
            if (fIsLeaf) fIsExpanded = fHasBeenExpanded = false;
            else {
                fIsExpanded = treeState.getExpandedState(path);
                fHasBeenExpanded = tree.hasBeenExpanded(path);
            }
            final Rectangle boundsBuffer = new Rectangle();
            fBounds = treeState.getBounds(fTrackingPath, boundsBuffer);
            fBounds.x += fInsets.left;
            fBounds.y += fInsets.top;
            fTrackingRow = getRowForPath(fTrackingPath);

            paintOneControl();
        }

        public void mouseDragged(final MouseEvent e) {
            fIsInBounds = fPathBounds.contains(e.getX(), e.getY());
                paintOneControl();
            }

        @Override
        public void mouseExited(MouseEvent e) {
            fIsInBounds = fPathBounds.contains(e.getX(), e.getY());
            paintOneControl();
        }

        public void mouseReleased(final MouseEvent e) {
            if (tree == null) return;

            if (fIsPressed) {
                final boolean wasInBounds = fIsInBounds;

                fIsPressed = false;
                fIsInBounds = false;

                if (wasInBounds) {
                    fIsExpanded = !fIsExpanded;
                    paintAnimation(fIsExpanded);
                    if (e.isAltDown()) {
                        if (fIsExpanded) {
                            expandNode(fTrackingRow, true);
                        } else {
                            collapseNode(fTrackingRow, true);
                        }
                    } else {
                        toggleExpandState(fTrackingPath);
                    }
                }
            }
            fTrackingPath = null;
            removeFromSource();
        }

        protected void paintAnimation(final boolean expanding) {
            if (expanding) {
                paintAnimationFrame(1);
                paintAnimationFrame(2);
                paintAnimationFrame(3);
            } else {
                paintAnimationFrame(3);
                paintAnimationFrame(2);
                paintAnimationFrame(1);
            }
            fAnimationFrame = -1;
        }

        protected void paintAnimationFrame(final int frame) {
            fAnimationFrame = frame;
            paintOneControl();
            try { Thread.sleep(20); } catch (final InterruptedException e) { }
        }

        // Utility to paint just one widget while it's being tracked
        // Just doing "repaint" runs into problems if someone does "translate" on the graphics
        // (ie, Sun's JTreeTable example, which is used by Moneydance - see Radar 2697837)
        void paintOneControl() {
            if (tree == null) return;
            final Graphics g = tree.getGraphics();
            if (g == null) {
                // i.e. source is not displayable
                return;
            }

            try {
                g.setClip(fVisibleRect);
                // If we ever wanted a callback for drawing the arrow between
                // transition stages
                // the code between here and paintExpandControl would be it
                g.setColor(fBackground);
                g.fillRect(fPathBounds.x, fPathBounds.y, fPathBounds.width, fPathBounds.height);

                // if there is no tracking path, we don't need to paint anything
                if (fTrackingPath == null) return;

                // draw the vertical line to the parent
                final TreePath parentPath = fTrackingPath.getParentPath();
                if (parentPath != null) {
                    paintVerticalPartOfLeg(g, fPathBounds, fInsets, parentPath);
                    paintHorizontalPartOfLeg(g, fPathBounds, fInsets, fBounds, fTrackingPath, fTrackingRow, fIsExpanded, fHasBeenExpanded, fIsLeaf);
                } else if (isRootVisible() && fTrackingRow == 0) {
                    paintHorizontalPartOfLeg(g, fPathBounds, fInsets, fBounds, fTrackingPath, fTrackingRow, fIsExpanded, fHasBeenExpanded, fIsLeaf);
                }
                paintExpandControl(g, fPathBounds, fInsets, fBounds, fTrackingPath, fTrackingRow, fIsExpanded, fHasBeenExpanded, fIsLeaf);
            } finally {
                g.dispose();
            }
        }

        protected void removeFromSource() {
            tree.removeMouseListener(this);
            tree.removeMouseMotionListener(this);
            }
        }

    protected int getRowForPath(final TreePath path) {
        return treeState.getRowForPath(path);
    }

    /**
     * see isLocationInExpandControl for bounds calc
     */
    protected Rectangle getPathArrowBounds(final TreePath path) {
        final Rectangle bounds = getPathBounds(tree, path); // Gives us the y values, but x is adjusted for the contents
        final Insets i = tree.getInsets();

        if (getExpandedIcon() != null) bounds.width = getExpandedIcon().getIconWidth();
        else bounds.width = 8;

        int boxLeftX = (i != null) ? i.left : 0;
        if (AquaUtils.isLeftToRight(tree)) {
            boxLeftX += (((path.getPathCount() + depthOffset - 2) * totalChildIndent) + getLeftChildIndent()) - bounds.width / 2;
        } else {
            boxLeftX += tree.getWidth() - 1 - ((path.getPathCount() - 2 + depthOffset) * totalChildIndent) - getLeftChildIndent() - bounds.width / 2;
        }
        bounds.x = boxLeftX;
        return bounds;
    }

    protected void installKeyboardActions() {
        super.installKeyboardActions();
        tree.getActionMap().put("aquaExpandNode", new KeyboardExpandCollapseAction(true, false));
        tree.getActionMap().put("aquaCollapseNode", new KeyboardExpandCollapseAction(false, false));
        tree.getActionMap().put("aquaFullyExpandNode", new KeyboardExpandCollapseAction(true, true));
        tree.getActionMap().put("aquaFullyCollapseNode", new KeyboardExpandCollapseAction(false, true));
    }

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    class KeyboardExpandCollapseAction extends AbstractAction {
        /**
         * Determines direction to traverse, 1 means expand, -1 means collapse.
         */
        final boolean expand;
        final boolean recursive;

        /**
         * True if the selection is reset, false means only the lead path changes.
         */
        public KeyboardExpandCollapseAction(final boolean expand, final boolean recursive) {
            this.expand = expand;
            this.recursive = recursive;
        }

        public void actionPerformed(final ActionEvent e) {
            if (tree == null || 0 > getRowCount(tree)) return;

            final TreePath[] selectionPaths = tree.getSelectionPaths();
            if (selectionPaths == null) return;

            for (int i = selectionPaths.length - 1; i >= 0; i--) {
                final TreePath path = selectionPaths[i];

                /*
                 * Try and expand the node, otherwise go to next node.
                 */
                if (expand) {
                    expandNode(tree.getRowForPath(path), recursive);
                    continue;
                }
                // else collapse

                // in the special case where there is only one row selected,
                // we want to do what the Cocoa does, and select the parent
                if (selectionPaths.length == 1 && tree.isCollapsed(path)) {
                    final TreePath parentPath = path.getParentPath();
                    if (parentPath != null && (!(parentPath.getParentPath() == null) || tree.isRootVisible())) {
                        tree.scrollPathToVisible(parentPath);
                        tree.setSelectionPath(parentPath);
                    }
                    continue;
                }

                collapseNode(tree.getRowForPath(path), recursive);
            }
        }

        public boolean isEnabled() {
            return (tree != null && tree.isEnabled());
        }
    }

    void expandNode(final int row, final boolean recursive) {
        final TreePath path = getPathForRow(tree, row);
        if (path == null) return;

        tree.expandPath(path);
        if (!recursive) return;

        expandAllNodes(path, row + 1);
    }

    void expandAllNodes(final TreePath parent, final int initialRow) {
        for (int i = initialRow; true; i++) {
            final TreePath path = getPathForRow(tree, i);
            if (!parent.isDescendant(path)) return;

            tree.expandPath(path);
        }
    }

    void collapseNode(final int row, final boolean recursive) {
        final TreePath path = getPathForRow(tree, row);
        if (path == null) return;

        if (recursive) {
            collapseAllNodes(path, row + 1);
        }

        tree.collapsePath(path);
    }

    void collapseAllNodes(final TreePath parent, final int initialRow) {
        int lastRow = -1;
        for (int i = initialRow; lastRow == -1; i++) {
            final TreePath path = getPathForRow(tree, i);
            if (!parent.isDescendant(path)) {
                lastRow = i - 1;
            }
        }

        for (int i = lastRow; i >= initialRow; i--) {
            final TreePath path = getPathForRow(tree, i);
            tree.collapsePath(path);
        }
    }
}
