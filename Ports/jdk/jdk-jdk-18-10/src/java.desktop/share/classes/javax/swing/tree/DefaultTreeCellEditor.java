/*
 * Copyright (c) 1998, 2015, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.awt.event.*;
import java.beans.BeanProperty;
import java.util.EventObject;

/**
 * A <code>TreeCellEditor</code>. You need to supply an
 * instance of <code>DefaultTreeCellRenderer</code>
 * so that the icons can be obtained. You can optionally supply
 * a <code>TreeCellEditor</code> that will be layed out according
 * to the icon in the <code>DefaultTreeCellRenderer</code>.
 * If you do not supply a <code>TreeCellEditor</code>,
 * a <code>TextField</code> will be used. Editing is started
 * on a triple mouse click, or after a click, pause, click and
 * a delay of 1200 milliseconds.
 *<p>
 * <strong>Warning:</strong>
 * Serialized objects of this class will not be compatible with
 * future Swing releases. The current serialization support is
 * appropriate for short term storage or RMI between applications running
 * the same version of Swing.  As of 1.4, support for long term storage
 * of all JavaBeans
 * has been added to the <code>java.beans</code> package.
 * Please see {@link java.beans.XMLEncoder}.
 *
 * @see javax.swing.JTree
 *
 * @author Scott Violet
 */
public class DefaultTreeCellEditor implements ActionListener, TreeCellEditor,
            TreeSelectionListener {
    /** Editor handling the editing. */
    protected TreeCellEditor               realEditor;

    /** Renderer, used to get border and offsets from. */
    protected DefaultTreeCellRenderer      renderer;

    /** Editing container, will contain the <code>editorComponent</code>. */
    protected Container                    editingContainer;

    /**
     * Component used in editing, obtained from the
     * <code>editingContainer</code>.
     */
    protected transient Component          editingComponent;

    /**
     * As of Java 2 platform v1.4 this field should no longer be used. If
     * you wish to provide similar behavior you should directly override
     * <code>isCellEditable</code>.
     */
    protected boolean                      canEdit;

    /**
     * Used in editing. Indicates x position to place
     * <code>editingComponent</code>.
     */
    protected transient int                offset;

    /** <code>JTree</code> instance listening too. */
    protected transient JTree              tree;

    /** Last path that was selected. */
    protected transient TreePath           lastPath;

    /** Used before starting the editing session. */
    protected transient Timer              timer;

    /**
     * Row that was last passed into
     * <code>getTreeCellEditorComponent</code>.
     */
    protected transient int                lastRow;

    /** True if the border selection color should be drawn. */
    protected Color                        borderSelectionColor;

    /** Icon to use when editing. */
    protected transient Icon               editingIcon;

    /**
     * Font to paint with, <code>null</code> indicates
     * font of renderer is to be used.
     */
    protected Font                         font;


    /**
     * Constructs a <code>DefaultTreeCellEditor</code>
     * object for a JTree using the specified renderer and
     * a default editor. (Use this constructor for normal editing.)
     *
     * @param tree      a <code>JTree</code> object
     * @param renderer  a <code>DefaultTreeCellRenderer</code> object
     */
    public DefaultTreeCellEditor(JTree tree,
                                 DefaultTreeCellRenderer renderer) {
        this(tree, renderer, null);
    }

    /**
     * Constructs a <code>DefaultTreeCellEditor</code>
     * object for a <code>JTree</code> using the
     * specified renderer and the specified editor. (Use this constructor
     * for specialized editing.)
     *
     * @param tree      a <code>JTree</code> object
     * @param renderer  a <code>DefaultTreeCellRenderer</code> object
     * @param editor    a <code>TreeCellEditor</code> object
     */
    public DefaultTreeCellEditor(JTree tree, DefaultTreeCellRenderer renderer,
                                 TreeCellEditor editor) {
        this.renderer = renderer;
        realEditor = editor;
        if(realEditor == null)
            realEditor = createTreeCellEditor();
        editingContainer = createContainer();
        setTree(tree);
        setBorderSelectionColor(UIManager.getColor
                                ("Tree.editorBorderSelectionColor"));
    }

    /**
      * Sets the color to use for the border.
      * @param newColor the new border color
      */
    public void setBorderSelectionColor(Color newColor) {
        borderSelectionColor = newColor;
    }

    /**
      * Returns the color the border is drawn.
      * @return the border selection color
      */
    public Color getBorderSelectionColor() {
        return borderSelectionColor;
    }

    /**
     * Sets the font to edit with. <code>null</code> indicates
     * the renderers font should be used. This will NOT
     * override any font you have set in the editor
     * the receiver was instantiated with. If <code>null</code>
     * for an editor was passed in a default editor will be
     * created that will pick up this font.
     *
     * @param font  the editing <code>Font</code>
     * @see #getFont
     */
    public void setFont(Font font) {
        this.font = font;
    }

    /**
     * Gets the font used for editing.
     *
     * @return the editing <code>Font</code>
     * @see #setFont
     */
    public Font getFont() {
        return font;
    }

    //
    // TreeCellEditor
    //

    /**
     * Configures the editor.  Passed onto the <code>realEditor</code>.
     */
    public Component getTreeCellEditorComponent(JTree tree, Object value,
                                                boolean isSelected,
                                                boolean expanded,
                                                boolean leaf, int row) {
        setTree(tree);
        lastRow = row;
        determineOffset(tree, value, isSelected, expanded, leaf, row);

        if (editingComponent != null) {
            editingContainer.remove(editingComponent);
        }
        editingComponent = realEditor.getTreeCellEditorComponent(tree, value,
                                        isSelected, expanded,leaf, row);


        // this is kept for backwards compatibility but isn't really needed
        // with the current BasicTreeUI implementation.
        TreePath        newPath = tree.getPathForRow(row);

        canEdit = (lastPath != null && newPath != null &&
                   lastPath.equals(newPath));

        Font            font = getFont();

        if(font == null) {
            if(renderer != null)
                font = renderer.getFont();
            if(font == null)
                font = tree.getFont();
        }
        editingContainer.setFont(font);
        prepareForEditing();
        return editingContainer;
    }

    /**
     * Returns the value currently being edited.
     * @return the value currently being edited
     */
    public Object getCellEditorValue() {
        return realEditor.getCellEditorValue();
    }

    /**
     * If the <code>realEditor</code> returns true to this
     * message, <code>prepareForEditing</code>
     * is messaged and true is returned.
     */
    public boolean isCellEditable(EventObject event) {
        boolean            retValue = false;
        boolean            editable = false;

        if (event != null) {
            if (event.getSource() instanceof JTree) {
                setTree((JTree)event.getSource());
                if (event instanceof MouseEvent) {
                    TreePath path = tree.getPathForLocation(
                                         ((MouseEvent)event).getX(),
                                         ((MouseEvent)event).getY());
                    editable = (lastPath != null && path != null &&
                               lastPath.equals(path));
                    if (path!=null) {
                        lastRow = tree.getRowForPath(path);
                        Object value = path.getLastPathComponent();
                        boolean isSelected = tree.isRowSelected(lastRow);
                        boolean expanded = tree.isExpanded(path);
                        TreeModel treeModel = tree.getModel();
                        boolean leaf = treeModel.isLeaf(value);
                        determineOffset(tree, value, isSelected,
                                        expanded, leaf, lastRow);
                    }
                }
            }
        }
        if(!realEditor.isCellEditable(event))
            return false;
        if(canEditImmediately(event))
            retValue = true;
        else if(editable && shouldStartEditingTimer(event)) {
            startEditingTimer();
        }
        else if(timer != null && timer.isRunning())
            timer.stop();
        if(retValue)
            prepareForEditing();
        return retValue;
    }

    /**
     * Messages the <code>realEditor</code> for the return value.
     */
    public boolean shouldSelectCell(EventObject event) {
        return realEditor.shouldSelectCell(event);
    }

    /**
     * If the <code>realEditor</code> will allow editing to stop,
     * the <code>realEditor</code> is removed and true is returned,
     * otherwise false is returned.
     */
    public boolean stopCellEditing() {
        if(realEditor.stopCellEditing()) {
            cleanupAfterEditing();
            return true;
        }
        return false;
    }

    /**
     * Messages <code>cancelCellEditing</code> to the
     * <code>realEditor</code> and removes it from this instance.
     */
    public void cancelCellEditing() {
        realEditor.cancelCellEditing();
        cleanupAfterEditing();
    }

    /**
     * Adds the <code>CellEditorListener</code>.
     * @param l the listener to be added
     */
    public void addCellEditorListener(CellEditorListener l) {
        realEditor.addCellEditorListener(l);
    }

    /**
      * Removes the previously added <code>CellEditorListener</code>.
      * @param l the listener to be removed
      */
    public void removeCellEditorListener(CellEditorListener l) {
        realEditor.removeCellEditorListener(l);
    }

    /**
     * Returns an array of all the <code>CellEditorListener</code>s added
     * to this DefaultTreeCellEditor with addCellEditorListener().
     *
     * @return all of the <code>CellEditorListener</code>s added or an empty
     *         array if no listeners have been added
     * @since 1.4
     */
    public CellEditorListener[] getCellEditorListeners() {
        return ((DefaultCellEditor)realEditor).getCellEditorListeners();
    }

    //
    // TreeSelectionListener
    //

    /**
     * Resets <code>lastPath</code>.
     */
    public void valueChanged(TreeSelectionEvent e) {
        if(tree != null) {
            if(tree.getSelectionCount() == 1)
                lastPath = tree.getSelectionPath();
            else
                lastPath = null;
        }
        if(timer != null) {
            timer.stop();
        }
    }

    //
    // ActionListener (for Timer).
    //

    /**
     * Messaged when the timer fires, this will start the editing
     * session.
     */
    public void actionPerformed(ActionEvent e) {
        if(tree != null && lastPath != null) {
            tree.startEditingAtPath(lastPath);
        }
    }

    //
    // Local methods
    //

    /**
     * Sets the tree currently editing for. This is needed to add
     * a selection listener.
     * @param newTree the new tree to be edited
     */
    protected void setTree(JTree newTree) {
        if(tree != newTree) {
            if(tree != null)
                tree.removeTreeSelectionListener(this);
            tree = newTree;
            if(tree != null)
                tree.addTreeSelectionListener(this);
            if(timer != null) {
                timer.stop();
            }
        }
    }

    /**
     * Returns true if <code>event</code> is a <code>MouseEvent</code>
     * and the click count is 1.
     *
     * @param event the event being studied
     * @return whether {@code event} should starts the editing timer
     */
    protected boolean shouldStartEditingTimer(EventObject event) {
        if((event instanceof MouseEvent) &&
            SwingUtilities.isLeftMouseButton((MouseEvent)event)) {
            MouseEvent        me = (MouseEvent)event;

            return (me.getClickCount() == 1 &&
                    inHitRegion(me.getX(), me.getY()));
        }
        return false;
    }

    /**
     * Starts the editing timer.
     */
    protected void startEditingTimer() {
        if(timer == null) {
            timer = new Timer(1200, this);
            timer.setRepeats(false);
        }
        timer.start();
    }

    /**
     * Returns true if <code>event</code> is <code>null</code>,
     * or it is a <code>MouseEvent</code> with a click count &gt; 2
     * and <code>inHitRegion</code> returns true.
     *
     * @param event the event being studied
     * @return whether editing can be started for the given {@code event}
     */
    protected boolean canEditImmediately(EventObject event) {
        if((event instanceof MouseEvent) &&
           SwingUtilities.isLeftMouseButton((MouseEvent)event)) {
            MouseEvent       me = (MouseEvent)event;

            return ((me.getClickCount() > 2) &&
                    inHitRegion(me.getX(), me.getY()));
        }
        return (event == null);
    }

    /**
     * Returns true if the passed in location is a valid mouse location
     * to start editing from. This is implemented to return false if
     * <code>x</code> is &lt;= the width of the icon and icon gap displayed
     * by the renderer. In other words this returns true if the user
     * clicks over the text part displayed by the renderer, and false
     * otherwise.
     * @param x the x-coordinate of the point
     * @param y the y-coordinate of the point
     * @return true if the passed in location is a valid mouse location
     */
    protected boolean inHitRegion(int x, int y) {
        if(lastRow != -1 && tree != null) {
            Rectangle bounds = tree.getRowBounds(lastRow);
            ComponentOrientation treeOrientation = tree.getComponentOrientation();

            if ( treeOrientation.isLeftToRight() ) {
                if (bounds != null && x <= (bounds.x + offset) &&
                    offset < (bounds.width - 5)) {
                    return false;
                }
            } else if ( bounds != null &&
                        ( x >= (bounds.x+bounds.width-offset+5) ||
                          x <= (bounds.x + 5) ) &&
                        offset < (bounds.width - 5) ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determine the offset.
     * @param tree      a <code>JTree</code> object
     * @param value a value
     * @param isSelected selection status
     * @param expanded expansion status
     * @param leaf leaf status
     * @param row current row
     */
    protected void determineOffset(JTree tree, Object value,
                                   boolean isSelected, boolean expanded,
                                   boolean leaf, int row) {
        if(renderer != null) {
            if(leaf)
                editingIcon = renderer.getLeafIcon();
            else if(expanded)
                editingIcon = renderer.getOpenIcon();
            else
                editingIcon = renderer.getClosedIcon();
            if(editingIcon != null)
                offset = renderer.getIconTextGap() +
                         editingIcon.getIconWidth();
            else
                offset = renderer.getIconTextGap();
        }
        else {
            editingIcon = null;
            offset = 0;
        }
    }

    /**
     * Invoked just before editing is to start. Will add the
     * <code>editingComponent</code> to the
     * <code>editingContainer</code>.
     */
    protected void prepareForEditing() {
        if (editingComponent != null) {
            editingContainer.add(editingComponent);
        }
    }

    /**
     * Creates the container to manage placement of
     * <code>editingComponent</code>.
     *
     * @return new Container object
     */
    protected Container createContainer() {
        return new EditorContainer();
    }

    /**
     * This is invoked if a <code>TreeCellEditor</code>
     * is not supplied in the constructor.
     * It returns a <code>TextField</code> editor.
     * @return a new <code>TextField</code> editor
     */
    protected TreeCellEditor createTreeCellEditor() {
        Border              aBorder = UIManager.getBorder("Tree.editorBorder");
        @SuppressWarnings("serial") // Safe: outer class is non-serializable
        DefaultCellEditor   editor = new DefaultCellEditor
            (new DefaultTextField(aBorder)) {
            public boolean shouldSelectCell(EventObject event) {
                boolean retValue = super.shouldSelectCell(event);
                return retValue;
            }
        };

        // One click to edit.
        editor.setClickCountToStart(1);
        return editor;
    }

    /**
     * Cleans up any state after editing has completed. Removes the
     * <code>editingComponent</code> the <code>editingContainer</code>.
     */
    private void cleanupAfterEditing() {
        if (editingComponent != null) {
            editingContainer.remove(editingComponent);
        }
        editingComponent = null;
    }

    /**
     * <code>TextField</code> used when no editor is supplied.
     * This textfield locks into the border it is constructed with.
     * It also prefers its parents font over its font. And if the
     * renderer is not <code>null</code> and no font
     * has been specified the preferred height is that of the renderer.
     */
    @SuppressWarnings("serial") // Safe: outer class is non-serializable
    public class DefaultTextField extends JTextField {
        /** Border to use. */
        protected Border         border;

        /**
         * Constructs a
         * <code>DefaultTreeCellEditor.DefaultTextField</code> object.
         *
         * @param border  a <code>Border</code> object
         * @since 1.4
         */
        public DefaultTextField(Border border) {
            setBorder(border);
        }

        /**
         * Sets the border of this component.<p>
         * This is a bound property.
         *
         * @param border the border to be rendered for this component
         * @see Border
         * @see CompoundBorder
         */
        @BeanProperty(preferred = true, visualUpdate = true, description
                = "The component's border.")
        public void setBorder(Border border) {
            super.setBorder(border);
            this.border = border;
        }

        /**
         * Overrides <code>JComponent.getBorder</code> to
         * returns the current border.
         */
        public Border getBorder() {
            return border;
        }

        // implements java.awt.MenuContainer
        public Font getFont() {
            Font     font = super.getFont();

            // Prefer the parent containers font if our font is a
            // FontUIResource
            if(font instanceof FontUIResource) {
                Container     parent = getParent();

                if(parent != null && parent.getFont() != null)
                    font = parent.getFont();
            }
            return font;
        }

        /**
         * Overrides <code>JTextField.getPreferredSize</code> to
         * return the preferred size based on current font, if set,
         * or else use renderer's font.
         * @return a <code>Dimension</code> object containing
         *   the preferred size
         */
        public Dimension getPreferredSize() {
            Dimension      size = super.getPreferredSize();

            // If not font has been set, prefer the renderers height.
            if(renderer != null &&
               DefaultTreeCellEditor.this.getFont() == null) {
                Dimension     rSize = renderer.getPreferredSize();

                size.height = rSize.height;
            }
            return size;
        }
    }


    /**
     * Container responsible for placing the <code>editingComponent</code>.
     */
    @SuppressWarnings("serial") // Safe: outer class is non-serializable
    public class EditorContainer extends Container {
        /**
         * Constructs an <code>EditorContainer</code> object.
         */
        public EditorContainer() {
            setLayout(null);
        }

        // This should not be used. It will be removed when new API is
        // allowed.
        /**
         * Do not use.
         */
        public void EditorContainer() {
            setLayout(null);
        }

        /**
         * Overrides <code>Container.paint</code> to paint the node's
         * icon and use the selection color for the background.
         */
        public void paint(Graphics g) {
            int width = getWidth();
            int height = getHeight();

            // Then the icon.
            if(editingIcon != null) {
                int yLoc = calculateIconY(editingIcon);

                if (getComponentOrientation().isLeftToRight()) {
                    editingIcon.paintIcon(this, g, 0, yLoc);
                } else {
                    editingIcon.paintIcon(
                            this, g, width - editingIcon.getIconWidth(),
                            yLoc);
                }
            }

            // Border selection color
            Color       background = getBorderSelectionColor();
            if(background != null) {
                g.setColor(background);
                g.drawRect(0, 0, width - 1, height - 1);
            }
            super.paint(g);
        }

        /**
         * Lays out this <code>Container</code>.  If editing,
         * the editor will be placed at
         * <code>offset</code> in the x direction and 0 for y.
         */
        public void doLayout() {
            if(editingComponent != null) {
                int width = getWidth();
                int height = getHeight();
                if (getComponentOrientation().isLeftToRight()) {
                    editingComponent.setBounds(
                            offset, 0, width - offset, height);
                } else {
                    editingComponent.setBounds(
                        0, 0, width - offset, height);
                }
            }
        }

        /**
         * Calculate the y location for the icon.
         */
        private int calculateIconY(Icon icon) {
            // To make sure the icon position matches that of the
            // renderer, use the same algorithm as JLabel
            // (SwingUtilities.layoutCompoundLabel).
            int iconHeight = icon.getIconHeight();
            int textHeight = editingComponent.getFontMetrics(
                editingComponent.getFont()).getHeight();
            int textY = iconHeight / 2 - textHeight / 2;
            int totalY = Math.min(0, textY);
            int totalHeight = Math.max(iconHeight, textY + textHeight) -
                totalY;
            return getHeight() / 2 - (totalY + (totalHeight / 2));
        }

        /**
         * Returns the preferred size for the <code>Container</code>.
         * This will be at least preferred size of the editor plus
         * <code>offset</code>.
         * @return a <code>Dimension</code> containing the preferred
         *   size for the <code>Container</code>; if
         *   <code>editingComponent</code> is <code>null</code> the
         *   <code>Dimension</code> returned is 0, 0
         */
        public Dimension getPreferredSize() {
            if(editingComponent != null) {
                Dimension         pSize = editingComponent.getPreferredSize();

                pSize.width += offset + 5;

                Dimension         rSize = (renderer != null) ?
                                          renderer.getPreferredSize() : null;

                if(rSize != null)
                    pSize.height = Math.max(pSize.height, rSize.height);
                if(editingIcon != null)
                    pSize.height = Math.max(pSize.height,
                                            editingIcon.getIconHeight());

                // Make sure width is at least 100.
                pSize.width = Math.max(pSize.width, 100);
                return pSize;
            }
            return new Dimension(0, 0);
        }
    }
}
