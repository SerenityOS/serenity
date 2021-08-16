/*
 * Copyright (c) 1997, 2011, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * This source code is provided to illustrate the usage of a given feature
 * or technique and has been deliberately simplified. Additional steps
 * required for a production-quality application, such as security checks,
 * input validation and proper error handling, might not be present in
 * this sample code.
 */



import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.Component;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import javax.swing.UIManager;


@SuppressWarnings("serial")
public class SampleTreeCellRenderer extends JLabel implements TreeCellRenderer {

    /** Font used if the string to be displayed isn't a font. */
    protected static Font defaultFont;
    /** Icon to use when the item is collapsed. */
    protected static ImageIcon collapsedIcon;
    /** Icon to use when the item is expanded. */
    protected static ImageIcon expandedIcon;
    /** Color to use for the background when selected. */
    protected static final Color SELECTED_BACKGROUND_COLOR;

    static {
        if ("Nimbus".equals(UIManager.getLookAndFeel().getName())) {
            SELECTED_BACKGROUND_COLOR = new Color(0, 0,
                0, 0);
        } else {
            SELECTED_BACKGROUND_COLOR = Color.YELLOW;
        }
        try {
            defaultFont = new Font("SansSerif", 0, 12);
        } catch (Exception e) {
        }
        try {
            collapsedIcon = new ImageIcon(SampleTreeCellRenderer.class.
                    getResource("/resources/images/collapsed.gif"));
            expandedIcon = new ImageIcon(SampleTreeCellRenderer.class.
                    getResource("/resources/images/expanded.gif"));
        } catch (Exception e) {
            System.out.println("Couldn't load images: " + e);
        }
    }
    /** Whether or not the item that was last configured is selected. */
    protected boolean selected;

    /**
     * This is messaged from JTree whenever it needs to get the size
     * of the component or it wants to draw it.
     * This attempts to set the font based on value, which will be
     * a TreeNode.
     */
    public Component getTreeCellRendererComponent(JTree tree, Object value,
            boolean selected, boolean expanded,
            boolean leaf, int row,
            boolean hasFocus) {
        String stringValue = tree.convertValueToText(value, selected,
                expanded, leaf, row, hasFocus);

        /* Set the text. */
        setText(stringValue);
        /* Tooltips used by the tree. */
        setToolTipText(stringValue);

        /* Set the image. */
        if (expanded) {
            setIcon(expandedIcon);
        } else if (!leaf) {
            setIcon(collapsedIcon);
        } else {
            setIcon(null);
        }

        /* Set the color and the font based on the SampleData userObject. */
        SampleData userObject = (SampleData) ((DefaultMutableTreeNode) value).
                getUserObject();
        if (hasFocus) {
            setForeground(UIManager.getColor("Tree.selectionForeground"));
        } else {
            setForeground(userObject.getColor());
        }
        if (userObject.getFont() == null) {
            setFont(defaultFont);
        } else {
            setFont(userObject.getFont());
        }

        /* Update the selected flag for the next paint. */
        this.selected = selected;

        return this;
    }

    /**
     * paint is subclassed to draw the background correctly.  JLabel
     * currently does not allow backgrounds other than white, and it
     * will also fill behind the icon.  Something that isn't desirable.
     */
    @Override
    public void paint(Graphics g) {
        Color bColor;
        Icon currentI = getIcon();

        if (selected) {
            bColor = SELECTED_BACKGROUND_COLOR;
        } else if (getParent() != null) /* Pick background color up from parent (which will come from
        the JTree we're contained in). */ {
            bColor = getParent().getBackground();
        } else {
            bColor = getBackground();
        }
        g.setColor(bColor);
        if (currentI != null && getText() != null) {
            int offset = (currentI.getIconWidth() + getIconTextGap());

            if (getComponentOrientation().isLeftToRight()) {
                g.fillRect(offset, 0, getWidth() - 1 - offset,
                        getHeight() - 1);
            } else {
                g.fillRect(0, 0, getWidth() - 1 - offset, getHeight() - 1);
            }
        } else {
            g.fillRect(0, 0, getWidth() - 1, getHeight() - 1);
        }
        super.paint(g);
    }
}
