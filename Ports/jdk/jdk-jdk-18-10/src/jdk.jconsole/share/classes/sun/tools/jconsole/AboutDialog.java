/*
 * Copyright (c) 2006, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.tools.jconsole;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyVetoException;
import java.net.URI;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import javax.swing.text.View;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.NORTH;
import static java.awt.BorderLayout.SOUTH;

import static sun.tools.jconsole.Utilities.setAccessibleDescription;
import static sun.tools.jconsole.Utilities.setAccessibleName;

@SuppressWarnings("serial")
public class AboutDialog extends InternalDialog {

    private static final Color textColor     = new Color(87,   88,  89);
    private static final Color bgColor       = new Color(232, 237, 241);
    private static final Color borderColor   = Color.black;

    private Icon mastheadIcon =
        new MastheadIcon(Messages.HELP_ABOUT_DIALOG_MASTHEAD_TITLE);

    private static AboutDialog aboutDialog;

    private JLabel statusBar;
    private Action closeAction;
    private JEditorPane helpLink;
    private final String urlStr = getOnlineDocUrl();

    public AboutDialog(JConsole jConsole) {
        super(jConsole, Messages.HELP_ABOUT_DIALOG_TITLE, false);

        setAccessibleDescription(this, Messages.HELP_ABOUT_DIALOG_ACCESSIBLE_DESCRIPTION);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setResizable(false);
        JComponent cp = (JComponent)getContentPane();

        createActions();

        JLabel mastheadLabel = new JLabel(mastheadIcon);
        setAccessibleName(mastheadLabel,
                Messages.HELP_ABOUT_DIALOG_MASTHEAD_ACCESSIBLE_NAME);

        JPanel mainPanel = new TPanel(0, 0);
        mainPanel.add(mastheadLabel, NORTH);

        String jConsoleVersion = Version.getVersion();
        String vmName = System.getProperty("java.vm.name");
        String vmVersion = System.getProperty("java.vm.version");
        String locUrlStr = urlStr;
        if (isBrowseSupported()) {
            locUrlStr = "<a style='color:#35556b' href=\"" + locUrlStr + "\">" + locUrlStr + "</a>";
        }

        JPanel infoAndLogoPanel = new JPanel(new BorderLayout(10, 10));
        infoAndLogoPanel.setBackground(bgColor);

        String colorStr = String.format("%06x", textColor.getRGB() & 0xFFFFFF);
        helpLink = new JEditorPane("text/html",
                                "<html><font color=#"+ colorStr + ">" +
                        Resources.format(Messages.HELP_ABOUT_DIALOG_JCONSOLE_VERSION, jConsoleVersion) +
                "<p>" + Resources.format(Messages.HELP_ABOUT_DIALOG_JAVA_VERSION, (vmName +", "+ vmVersion)) +
                "<p>" + locUrlStr + "</html>");

        helpLink.setOpaque(false);
        helpLink.setEditable(false);
        helpLink.setForeground(textColor);

        mainPanel.setBorder(BorderFactory.createLineBorder(borderColor));
        infoAndLogoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        helpLink.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if ((e.getKeyCode() == KeyEvent.VK_ENTER) || (e.getKeyCode() == KeyEvent.VK_SPACE)) {
                    browse(urlStr);
                    e.consume();
                }
            }
        });

        helpLink.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                highlight();
            }
            @Override
            public void focusLost(FocusEvent e) {
                removeHighlights();
            }
        });

        helpLink.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    browse(e.getDescription());
                }
            }
        });
        infoAndLogoPanel.add(helpLink, NORTH);

        ImageIcon brandLogoIcon = new ImageIcon(getClass().getResource("resources/brandlogo.png"));
        JLabel brandLogo = new JLabel(brandLogoIcon, JLabel.LEADING);

        JButton closeButton = new JButton(closeAction);

        JPanel bottomPanel = new TPanel(0, 0);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        buttonPanel.setOpaque(false);

        mainPanel.add(infoAndLogoPanel, CENTER);
        cp.add(bottomPanel, SOUTH);

        infoAndLogoPanel.add(brandLogo, SOUTH);

        buttonPanel.setBorder(new EmptyBorder(2, 12, 2, 12));
        buttonPanel.add(closeButton);
        bottomPanel.add(buttonPanel, NORTH);

        statusBar = new JLabel(" ");
        bottomPanel.add(statusBar, SOUTH);

        cp.add(mainPanel, NORTH);

        pack();
        setLocationRelativeTo(jConsole);
        Utilities.updateTransparency(this);
    }

    public void highlight() {
        try {
            removeHighlights();

            Highlighter hilite = helpLink.getHighlighter();
            Document doc = helpLink.getDocument();
            String text = doc.getText(0, doc.getLength());
            int pos = text.indexOf(urlStr, 0);
            hilite.addHighlight(pos, pos + urlStr.length(), new HighlightPainter());
        } catch (BadLocationException e) {
            // ignore
        }
    }

    public void removeHighlights() {
        Highlighter hilite = helpLink.getHighlighter();
        hilite.removeAllHighlights();
    }

    public void showDialog() {
        statusBar.setText(" ");
        setVisible(true);
        try {
            // Bring to front of other dialogs
            setSelected(true);
        } catch (PropertyVetoException e) {
            // ignore
        }
    }

    private static AboutDialog getAboutDialog(JConsole jConsole) {
        if (aboutDialog == null) {
            aboutDialog = new AboutDialog(jConsole);
        }
        return aboutDialog;
    }

    static void showAboutDialog(JConsole jConsole) {
        getAboutDialog(jConsole).showDialog();
    }

    static void browseUserGuide(JConsole jConsole) {
        getAboutDialog(jConsole).browse(getOnlineDocUrl());
    }

    static boolean isBrowseSupported() {
        return (Desktop.isDesktopSupported() &&
                Desktop.getDesktop().isSupported(Desktop.Action.BROWSE));
    }

    void browse(String urlStr) {
        try {
            Desktop.getDesktop().browse(new URI(urlStr));
        } catch (Exception ex) {
            showDialog();
            statusBar.setText(ex.getLocalizedMessage());
            if (JConsole.isDebug()) {
                ex.printStackTrace();
            }
        }
    }

    private void createActions() {
        closeAction = new AbstractAction(Messages.CLOSE) {
            public void actionPerformed(ActionEvent ev) {
                setVisible(false);
                statusBar.setText("");
            }
        };
    }

    private static String getOnlineDocUrl() {
        String version = Integer.toString(Runtime.version().feature());
        return Resources.format(Messages.HELP_ABOUT_DIALOG_USER_GUIDE_LINK_URL,
                                version);
    }

    private static class TPanel extends JPanel {
        TPanel(int hgap, int vgap) {
            super(new BorderLayout(hgap, vgap));
            setOpaque(false);
        }
    }

    private static class HighlightPainter
            extends DefaultHighlighter.DefaultHighlightPainter {

        public HighlightPainter() {
            super(null);
        }
        @Override
        public Shape paintLayer(Graphics g, int offs0, int offs1, Shape bounds,
                                JTextComponent c, View view) {
            g.setColor(c.getSelectionColor());
            Rectangle alloc;

            if (bounds instanceof Rectangle) {
                alloc = (Rectangle)bounds;
            } else {
                alloc = bounds.getBounds();
            }

            Graphics2D g2d = (Graphics2D)g;

            float[] dash = { 2F, 2F };
            Stroke dashedStroke = new BasicStroke(1F, BasicStroke.CAP_SQUARE,
            BasicStroke.JOIN_MITER, 3F, dash, 0F);
            g2d.fill(dashedStroke.createStrokedShape(
                     new Rectangle2D.Float(alloc.x, alloc.y,
                                           alloc.width - 1, alloc.height - 1)));
            return alloc;
        }
    }
}
