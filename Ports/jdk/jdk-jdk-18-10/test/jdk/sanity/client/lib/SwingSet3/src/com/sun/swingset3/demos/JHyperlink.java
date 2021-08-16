/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
package com.sun.swingset3.demos;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 *
 * @author aim
 */
//<snip>Create HTML hyperlink
//<snip>Create HTML image hyperlink
public class JHyperlink extends JButton {

    private static final BrowseAction defaultBrowseAction = new BrowseAction();

    private URI targetURI;
    private boolean visited;

    private final transient Rectangle viewRect = new Rectangle();
    private final transient Rectangle iconRect = new Rectangle();
    private final transient Rectangle textRect = new Rectangle();

    //remind(aim): lookup colors instead of hardcoding them
    private Color normalForeground;
    private Color activeForeground;
    private Color visitedForeground;
    private boolean drawUnderline = true;

    static {
        UIManager.put("Hyperlink.foreground", Color.blue);
        UIManager.put("Hyperlink.activeForeground", Color.red);
        UIManager.put("Hyperlink.visitedForeground", new Color(85, 145, 90));
    }

    /**
     * Creates a new instance of JHyperlink
     */
    public JHyperlink() {
        super();
        normalForeground = UIManager.getColor("Hyperlink.foreground");
        activeForeground = UIManager.getColor("Hyperlink.activeForeground");
        visitedForeground = UIManager.getColor("Hyperlink.visitedForeground");
        setBorderPainted(false);
        setContentAreaFilled(false);
        setForeground(normalForeground);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setMargin(new Insets(0, 0, 0, 0));
        setAction(defaultBrowseAction);
    }

    /**
     * Creates a new instance of JHyperlink
     */
    public JHyperlink(String text) {
        this();
        setText(text); // override the inheritence of the action's name
    }

    public JHyperlink(String text, String targetURI) throws URISyntaxException {
        this(text, new URI(targetURI));
    }

    public JHyperlink(String text, URI target) {
        this(text);
        setTarget(target);
    }

    public JHyperlink(String text, Action action) {
        this(text);
        setAction(action); // replaces default browse action
        setText(text); // override the inheritence of the action's name
    }

    public JHyperlink(String text, Icon icon) {
        this(text);
        setIcon(icon);
    }

    public JHyperlink(Icon icon, String targetURI) throws URISyntaxException {
        this(null, icon, targetURI);
    }

    public JHyperlink(String text, Icon icon, String targetURI) throws URISyntaxException {
        this(text, new URI(targetURI));
        setIcon(icon);
    }

    public JHyperlink(String text, Icon icon, URI target) {
        this(text);
        setIcon(icon);
        setTarget(target);
    }

    public void setTarget(URI target) {
        this.targetURI = target;
        setToolTipText(target.toASCIIString());
    }

    public URI getTarget() {
        return targetURI;
    }

    public void setVisited(boolean visited) {
        this.visited = visited;
    }

    public boolean isVisited() {
        return visited;
    }

    @Override
    public void setForeground(Color foreground) {
        normalForeground = foreground;
        super.setForeground(foreground);
    }

    public void setVisitedForeground(Color visited) {
        visitedForeground = visited;
    }

    public void setDrawUnderline(boolean drawUnderline) {
        this.drawUnderline = drawUnderline;
    }

    public boolean getDrawUnderline() {
        return drawUnderline;
    }

    @Override
    protected void paintComponent(Graphics g) {
        // Set the foreground on the fly to ensure the text is painted
        // with the proper color in super.paintComponent
        ButtonModel model = getModel();
        if (model.isArmed()) {
            super.setForeground(activeForeground);
        } else if (visited) {
            super.setForeground(visitedForeground);
        } else {
            super.setForeground(normalForeground);
        }
        super.paintComponent(g);

        if (drawUnderline) {
            Insets insets = getInsets();
            viewRect.x = insets.left;
            viewRect.y = insets.top;
            viewRect.width = getWidth() - insets.left - insets.right;
            viewRect.height = getHeight() - insets.top - insets.bottom;
            int baseline = getBaseline(viewRect.width, viewRect.height);

            iconRect.x = iconRect.y = iconRect.width = iconRect.height = 0;
            textRect.x = textRect.y = textRect.width = textRect.height = 0;
            SwingUtilities.layoutCompoundLabel(g.getFontMetrics(), getText(),
                    getIcon(), getVerticalAlignment(), getHorizontalAlignment(),
                    getVerticalTextPosition(), getHorizontalTextPosition(),
                    viewRect, iconRect, textRect, getIconTextGap());

            // getBaseline not returning correct results, so workaround for now
            if (UIManager.getLookAndFeel().getName().equals("Nimbus")) {
                baseline += 7;
            } else {
                baseline += 3;
            }

            g.setColor(getForeground());
            g.drawLine(textRect.x,
                    baseline,
                    textRect.x + textRect.width,
                    baseline);
        }

    }

    // This action is stateless and hence can be shared across hyperlinks
    private static class BrowseAction extends AbstractAction {

        public BrowseAction() {
            super();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JHyperlink hyperlink = (JHyperlink) e.getSource();

            URI targetURI = hyperlink.getTarget();
            if (targetURI != null) {
                try {
                    DemoUtilities.browse(targetURI);
                    hyperlink.setVisited(true);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    System.err.println(ex);
                }

            }
        }

    }
//</snip>
//</snip>

}
