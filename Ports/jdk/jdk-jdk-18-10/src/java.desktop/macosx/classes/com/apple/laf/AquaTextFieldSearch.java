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
import javax.swing.event.*;
import javax.swing.plaf.TextUI;
import javax.swing.text.JTextComponent;

import apple.laf.JRSUIConstants.*;

import com.apple.laf.AquaIcon.DynamicallySizingJRSUIIcon;
import com.apple.laf.AquaUtilControlSize.*;
import com.apple.laf.AquaUtils.*;

public class AquaTextFieldSearch {
    private static final String VARIANT_KEY = "JTextField.variant";
    private static final String SEARCH_VARIANT_VALUE = "search";

    private static final String FIND_POPUP_KEY = "JTextField.Search.FindPopup";
    private static final String FIND_ACTION_KEY = "JTextField.Search.FindAction";
    private static final String CANCEL_ACTION_KEY = "JTextField.Search.CancelAction";
    private static final String PROMPT_KEY = "JTextField.Search.Prompt";

    private static final SearchFieldPropertyListener SEARCH_FIELD_PROPERTY_LISTENER = new SearchFieldPropertyListener();
    protected static void installSearchFieldListener(final JTextComponent c) {
        c.addPropertyChangeListener(SEARCH_FIELD_PROPERTY_LISTENER);
    }

    protected static void uninstallSearchFieldListener(final JTextComponent c) {
        c.removePropertyChangeListener(SEARCH_FIELD_PROPERTY_LISTENER);
    }

    static class SearchFieldPropertyListener implements PropertyChangeListener {
        public void propertyChange(final PropertyChangeEvent evt) {
            final Object source = evt.getSource();
            if (!(source instanceof JTextComponent)) return;

            final String propertyName = evt.getPropertyName();
            if (!VARIANT_KEY.equals(propertyName) &&
                !FIND_POPUP_KEY.equals(propertyName) &&
                !FIND_ACTION_KEY.equals(propertyName) &&
                !CANCEL_ACTION_KEY.equals(propertyName) &&
                !PROMPT_KEY.equals(propertyName)) {
                return;
            }

            final JTextComponent c = (JTextComponent)source;
            if (wantsToBeASearchField(c)) {
                uninstallSearchField(c);
                installSearchField(c);
            } else {
                uninstallSearchField(c);
            }
        }
    }

    protected static boolean wantsToBeASearchField(final JTextComponent c) {
        return SEARCH_VARIANT_VALUE.equals(c.getClientProperty(VARIANT_KEY));
    }

    protected static boolean hasPopupMenu(final JTextComponent c) {
        return (c.getClientProperty(FIND_POPUP_KEY) instanceof JPopupMenu);
    }

    private static final RecyclableSingleton<SearchFieldBorder> instance = new RecyclableSingletonFromDefaultConstructor<SearchFieldBorder>(SearchFieldBorder.class);
    public static SearchFieldBorder getSearchTextFieldBorder() {
        return instance.get();
    }

    protected static void installSearchField(final JTextComponent c) {
        final SearchFieldBorder border = getSearchTextFieldBorder();
        c.setBorder(border);
        c.setLayout(border.getCustomLayout());
        c.add(getFindButton(c), BorderLayout.WEST);
        c.add(getCancelButton(c), BorderLayout.EAST);
        c.add(getPromptLabel(c), BorderLayout.CENTER);

        final TextUI ui = c.getUI();
        if (ui instanceof AquaTextFieldUI) {
            ((AquaTextFieldUI)ui).setPaintingDelegate(border);
        }
    }

    protected static void uninstallSearchField(final JTextComponent c) {
        c.setBorder(UIManager.getBorder("TextField.border"));
        c.removeAll();

        final TextUI ui = c.getUI();
        if (ui instanceof AquaTextFieldUI) {
            ((AquaTextFieldUI)ui).setPaintingDelegate(null);
        }
    }

    // The "magnifying glass" icon that sometimes has a downward pointing triangle next to it
    // if a popup has been assigned to it. It does not appear to have a pressed state.
    protected static DynamicallySizingJRSUIIcon getFindIcon(final JTextComponent text) {
        return (text.getClientProperty(FIND_POPUP_KEY) == null) ?
            new DynamicallySizingJRSUIIcon(new SizeDescriptor(new SizeVariant(25, 22).alterMargins(0, 4, 0, -5))) {
                public void initJRSUIState() {
                    painter.state.set(Widget.BUTTON_SEARCH_FIELD_FIND);
                }
            }
        :
            new DynamicallySizingJRSUIIcon(new SizeDescriptor(new SizeVariant(25, 22).alterMargins(0, 4, 0, 2))) {
                public void initJRSUIState() {
                    painter.state.set(Widget.BUTTON_SEARCH_FIELD_FIND);
                }
            }
        ;
    }

    // The "X in a circle" that only shows up when there is text in the search field.
    protected static DynamicallySizingJRSUIIcon getCancelIcon() {
        return new DynamicallySizingJRSUIIcon(new SizeDescriptor(new SizeVariant(22, 22).alterMargins(0, 0, 0, 4))) {
            public void initJRSUIState() {
                painter.state.set(Widget.BUTTON_SEARCH_FIELD_CANCEL);
            }
        };
    }

    protected static State getState(final JButton b) {
        if (!AquaFocusHandler.isActive(b)) return State.INACTIVE;
        if (b.getModel().isPressed()) return State.PRESSED;
        return State.ACTIVE;
    }

    protected static JButton createButton(final JTextComponent c, final DynamicallySizingJRSUIIcon icon) {
        final JButton b = new JButton()
//        {
//            public void paint(Graphics g) {
//                super.paint(g);
//
//                g.setColor(Color.green);
//                g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
//            }
//        }
        ;

        final Insets i = icon.sizeVariant.margins;
        b.setBorder(BorderFactory.createEmptyBorder(i.top, i.left, i.bottom, i.right));

        b.setIcon(icon);
        b.setBorderPainted(false);
        b.setFocusable(false);
        b.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        b.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                icon.painter.state.set(getState(b));
            }
        });
        b.addMouseListener(new MouseAdapter() {
            public void mousePressed(final MouseEvent e) {
                c.requestFocusInWindow();
            }
        });

        return b;
    }

    protected static JButton getFindButton(final JTextComponent c) {
        final DynamicallySizingJRSUIIcon findIcon = getFindIcon(c);
        final JButton b = createButton(c, findIcon);
        b.setName("find");

        final Object findPopup = c.getClientProperty(FIND_POPUP_KEY);
        if (findPopup instanceof JPopupMenu) {
            // if we have a popup, indicate that in the icon
            findIcon.painter.state.set(Variant.MENU_GLYPH);

            b.addMouseListener(new MouseAdapter() {
                public void mousePressed(final MouseEvent e) {
                    ((JPopupMenu)findPopup).show(b, 8, b.getHeight() - 2);
                    c.requestFocusInWindow();
                    c.repaint();
                }
            });
        }

        final Object findAction = c.getClientProperty(FIND_ACTION_KEY);
        if (findAction instanceof ActionListener) {
            b.addActionListener((ActionListener)findAction);
        }

        return b;
    }

    private static Component getPromptLabel(final JTextComponent c) {
        final JLabel label = new JLabel();
        label.setForeground(UIManager.getColor("TextField.inactiveForeground"));

        c.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(final DocumentEvent e) { updatePromptLabel(label, c); }
            public void insertUpdate(final DocumentEvent e) { updatePromptLabel(label, c); }
            public void removeUpdate(final DocumentEvent e) { updatePromptLabel(label, c); }
        });
        c.addFocusListener(new FocusAdapter() {
            public void focusGained(final FocusEvent e) { updatePromptLabel(label, c); }
            public void focusLost(final FocusEvent e) { updatePromptLabel(label, c); }
        });
        updatePromptLabel(label, c);

        return label;
    }

    static void updatePromptLabel(final JLabel label, final JTextComponent text) {
        if (SwingUtilities.isEventDispatchThread()) {
            updatePromptLabelOnEDT(label, text);
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() { updatePromptLabelOnEDT(label, text); }
            });
        }
    }

    static void updatePromptLabelOnEDT(final JLabel label, final JTextComponent text) {
        String promptText = " ";
        if (!text.hasFocus() && "".equals(text.getText())) {
            final Object prompt = text.getClientProperty(PROMPT_KEY);
            if (prompt != null) promptText = prompt.toString();
        }
        label.setText(promptText);
    }

    @SuppressWarnings("serial") // anonymous class inside
    protected static JButton getCancelButton(final JTextComponent c) {
        final JButton b = createButton(c, getCancelIcon());
        b.setName("cancel");

        final Object cancelAction = c.getClientProperty(CANCEL_ACTION_KEY);
        if (cancelAction instanceof ActionListener) {
            b.addActionListener((ActionListener)cancelAction);
        }

        b.addActionListener(new AbstractAction("cancel") {
            public void actionPerformed(final ActionEvent e) {
                c.setText("");
            }
        });

        c.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(final DocumentEvent e) { updateCancelIcon(b, c); }
            public void insertUpdate(final DocumentEvent e) { updateCancelIcon(b, c); }
            public void removeUpdate(final DocumentEvent e) { updateCancelIcon(b, c); }
        });

        updateCancelIcon(b, c);
        return b;
    }

    // <rdar://problem/6444328> JTextField.variant=search: not thread-safe
    static void updateCancelIcon(final JButton button, final JTextComponent text) {
        if (SwingUtilities.isEventDispatchThread()) {
            updateCancelIconOnEDT(button, text);
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() { updateCancelIconOnEDT(button, text); }
            });
        }
    }

    static void updateCancelIconOnEDT(final JButton button, final JTextComponent text) {
        button.setVisible(!"".equals(text.getText()));
    }

    // subclass of normal text border, because we still want all the normal text field behaviors
    static class SearchFieldBorder extends AquaTextFieldBorder implements JComponentPainter {
        protected boolean reallyPaintBorder;

        public SearchFieldBorder() {
            super(new SizeDescriptor(new SizeVariant().alterMargins(6, 31, 6, 24).alterInsets(3, 3, 3, 3)));
            painter.state.set(Widget.FRAME_TEXT_FIELD_ROUND);
        }

        public SearchFieldBorder(final SearchFieldBorder other) {
            super(other);
        }

        public void paint(final JComponent c, final Graphics g, final int x, final int y, final int w, final int h) {
            reallyPaintBorder = true;
            paintBorder(c, g, x, y, w, h);
            reallyPaintBorder = false;
        }

        // apparently without adjusting for odd height pixels, the search field "wobbles" relative to it's contents
        public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int width, final int height) {
            if (!reallyPaintBorder) return;
            super.paintBorder(c, g, x, y - (height % 2), width, height);
        }

        public Insets getBorderInsets(final Component c) {
            if (doingLayout) return new Insets(0, 0, 0, 0);

            if (!hasPopupMenu((JTextComponent)c)) {
                return new Insets(sizeVariant.margins.top, sizeVariant.margins.left - 7, sizeVariant.margins.bottom, sizeVariant.margins.right);
            }

            return sizeVariant.margins;
        }

        protected boolean doingLayout;
        @SuppressWarnings("serial") // anonymous class inside
        protected LayoutManager getCustomLayout() {
            // unfortunately, the default behavior of BorderLayout, which accommodates for margins
            // is not what we want, so we "turn off margins" for layout for layout out our buttons
            return new BorderLayout(0, 0) {
                public void layoutContainer(final Container target) {
                    doingLayout = true;
                    super.layoutContainer(target);
                    doingLayout = false;
                }
            };
        }
    }
}
