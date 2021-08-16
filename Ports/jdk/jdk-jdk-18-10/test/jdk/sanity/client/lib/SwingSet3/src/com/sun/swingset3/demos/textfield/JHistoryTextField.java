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
package com.sun.swingset3.demos.textfield;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * JHistoryTextField
 *
 * @author Pavel Porvatov
 */
public class JHistoryTextField extends JTextField {

    private static final int MAX_VISIBLE_ROWS = 8;

    private final List<String> history = new ArrayList<String>();

    private final JPopupMenu popup = new JPopupMenu() {
        @Override
        public Dimension getPreferredSize() {
            Dimension dimension = super.getPreferredSize();

            dimension.width = JHistoryTextField.this.getWidth();

            return dimension;
        }
    };

    private final JList<String> list = new JList<>(new DefaultListModel<>());

    private String userText;

    private boolean notificationDenied;

    public JHistoryTextField() {
        JScrollPane scrollPane = new JScrollPane(list,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setHorizontalScrollBar(null);
        scrollPane.setBorder(null);

        list.setFocusable(false);

        popup.add(scrollPane);
        popup.setFocusable(false);
        popup.setBorder(new LineBorder(Color.BLACK, 1));

        getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onTextChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onTextChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                onTextChanged();
            }
        });

        list.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int index = list.locationToIndex(e.getPoint());

                if (index >= 0 && list.getSelectedIndex() != index) {
                    list.setSelectedIndex(index);
                }
            }
        });

        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    setTextWithoutNotification(list.getSelectedValue());

                    popup.setVisible(false);
                }
            }
        });

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                popup.setVisible(false);
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (popup.isShowing()) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_UP: {
                            changeListSelectedIndex(-1);

                            break;
                        }

                        case KeyEvent.VK_PAGE_UP: {
                            changeListSelectedIndex(-list.getVisibleRowCount());

                            break;
                        }

                        case KeyEvent.VK_DOWN: {
                            changeListSelectedIndex(1);

                            break;
                        }

                        case KeyEvent.VK_PAGE_DOWN: {
                            changeListSelectedIndex(list.getVisibleRowCount());

                            break;
                        }

                        case KeyEvent.VK_ESCAPE: {
                            popup.setVisible(false);

                            setTextWithoutNotification(userText);

                            break;
                        }

                        case KeyEvent.VK_ENTER:
                        case KeyEvent.VK_LEFT:
                        case KeyEvent.VK_RIGHT: {
                            popup.setVisible(false);

                            break;
                        }
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN
                        || e.getKeyCode() == KeyEvent.VK_UP
                        || e.getKeyCode() == KeyEvent.VK_PAGE_UP
                        || e.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {
                    userText = getText();

                    showFilteredHistory();
                }
            }
        });
    }

    private void changeListSelectedIndex(int delta) {
        int size = list.getModel().getSize();
        int index = list.getSelectedIndex();

        int newIndex;

        if (index < 0) {
            newIndex = delta > 0 ? 0 : size - 1;
        } else {
            newIndex = index + delta;
        }

        if (newIndex >= size || newIndex < 0) {
            newIndex = newIndex < 0 ? 0 : size - 1;

            if (index == newIndex) {
                newIndex = -1;
            }
        }

        if (newIndex < 0) {
            list.getSelectionModel().clearSelection();
            list.ensureIndexIsVisible(0);

            setTextWithoutNotification(userText);
        } else {
            list.setSelectedIndex(newIndex);
            list.ensureIndexIsVisible(newIndex);

            setTextWithoutNotification(list.getSelectedValue());
        }
    }

    private void setTextWithoutNotification(String text) {
        notificationDenied = true;

        try {
            setText(text);
        } finally {
            notificationDenied = false;
        }
    }

    private void onTextChanged() {
        if (!notificationDenied) {
            userText = getText();

            showFilteredHistory();
        }
    }

    private void showFilteredHistory() {
        list.getSelectionModel().clearSelection();

        DefaultListModel<String> model = (DefaultListModel<String>) list.getModel();

        model.clear();

        for (String s : history) {
            if (s.contains(userText)) {
                model.addElement(s);
            }
        }

        int size = model.size();

        if (size == 0) {
            popup.setVisible(false);
        } else {
            list.setVisibleRowCount(size < MAX_VISIBLE_ROWS ? size : MAX_VISIBLE_ROWS);

            popup.pack();

            if (!popup.isShowing()) {
                popup.show(JHistoryTextField.this, 0, getHeight());
            }
        }
    }

    public List<String> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public void setHistory(List<? extends String> history) {
        this.history.clear();
        this.history.addAll(history);
    }
}
