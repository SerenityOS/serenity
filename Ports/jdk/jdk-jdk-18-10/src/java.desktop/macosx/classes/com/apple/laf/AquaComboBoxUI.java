/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleState;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.ComboBoxEditor;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JRootPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.ActionMapUIResource;
import javax.swing.plaf.ComboBoxUI;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.ListUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.ComboPopup;

import apple.laf.JRSUIConstants.Size;
import com.apple.laf.AquaUtilControlSize.Sizeable;
import com.apple.laf.AquaUtils.RecyclableSingleton;
import com.apple.laf.ClientPropertyApplicator.Property;

// Inspired by MetalComboBoxUI, which also has a combined text-and-arrow button for noneditables
public class AquaComboBoxUI extends BasicComboBoxUI implements Sizeable {
    static final String POPDOWN_CLIENT_PROPERTY_KEY = "JComboBox.isPopDown";
    static final String ISSQUARE_CLIENT_PROPERTY_KEY = "JComboBox.isSquare";

    public static ComponentUI createUI(final JComponent c) {
        return new AquaComboBoxUI();
    }

    private boolean wasOpaque;
    public void installUI(final JComponent c) {
        super.installUI(c);

        // this doesn't work right now, because the JComboBox.init() method calls
        // .setOpaque(false) directly, and doesn't allow the LaF to decided. Bad Sun!
        LookAndFeel.installProperty(c, "opaque", Boolean.FALSE);

        wasOpaque = c.isOpaque();
        c.setOpaque(false);
    }

    public void uninstallUI(final JComponent c) {
        c.setOpaque(wasOpaque);
        super.uninstallUI(c);
    }

    protected void installListeners() {
        super.installListeners();
        AquaUtilControlSize.addSizePropertyListener(comboBox);
    }

    protected void uninstallListeners() {
        AquaUtilControlSize.removeSizePropertyListener(comboBox);
        super.uninstallListeners();
    }

    protected void installComponents() {
        super.installComponents();

        // client properties must be applied after the components have been installed,
        // because isSquare and isPopdown are applied to the installed button
        getApplicator().attachAndApplyClientProperties(comboBox);
    }

    protected void uninstallComponents() {
        getApplicator().removeFrom(comboBox);
        // AquaButtonUI install some listeners to all parents, which means that
        // we need to uninstall UI here to remove those listeners, because after
        // we remove them from ComboBox we lost the latest reference to them,
        // and our standard uninstallUI machinery will not call them.
        arrowButton.getUI().uninstallUI(arrowButton);
        super.uninstallComponents();
    }

    protected ItemListener createItemListener() {
        return new ItemListener() {
            long lastBlink = 0L;
            public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() != ItemEvent.SELECTED) return;
                if (!popup.isVisible()) return;

                // sometimes, multiple selection changes can occur while the popup is up,
                // and blinking more than "once" (in a second) is not desirable
                final long now = System.currentTimeMillis();
                if (now - 1000 < lastBlink) return;
                lastBlink = now;

                final JList<Object> itemList = popup.getList();
                final ListUI listUI = itemList.getUI();
                if (!(listUI instanceof AquaListUI)) return;
                final AquaListUI aquaListUI = (AquaListUI)listUI;

                final int selectedIndex = comboBox.getSelectedIndex();
                final ListModel<Object> dataModel = itemList.getModel();
                if (dataModel == null) return;

                final Object value = dataModel.getElementAt(selectedIndex);
                AquaUtils.blinkMenu(new AquaUtils.Selectable() {
                    public void paintSelected(final boolean selected) {
                        aquaListUI.repaintCell(value, selectedIndex, selected);
                    }
                });
            }
        };
    }

    public void paint(final Graphics g, final JComponent c) {
        // this space intentionally left blank
    }

    protected ListCellRenderer<Object> createRenderer() {
        return new AquaComboBoxRenderer(comboBox);
    }

    protected ComboPopup createPopup() {
        return new AquaComboBoxPopup(comboBox);
    }

    protected JButton createArrowButton() {
        return new AquaComboBoxButton(this, comboBox, currentValuePane, listBox);
    }

    protected ComboBoxEditor createEditor() {
        return new AquaComboBoxEditor();
    }

    final class AquaComboBoxEditor extends BasicComboBoxEditor
            implements UIResource, DocumentListener {

        AquaComboBoxEditor() {
            super();
            editor = new AquaCustomComboTextField();
            editor.addFocusListener(this);
            editor.getDocument().addDocumentListener(this);
        }

        @Override
        public void changedUpdate(final DocumentEvent e) {
            editorTextChanged();
        }

        @Override
        public void insertUpdate(final DocumentEvent e) {
            editorTextChanged();
        }

        @Override
        public void removeUpdate(final DocumentEvent e) {
            editorTextChanged();
        }

        private void editorTextChanged() {
            if (!popup.isVisible()) return;

            final Object text = editor.getText();

            final ListModel<Object> model = listBox.getModel();
            final int items = model.getSize();
            for (int i = 0; i < items; i++) {
                final Object element = model.getElementAt(i);
                if (element == null) continue;

                final String asString = element.toString();
                if (asString == null || !asString.equals(text)) continue;

                popup.getList().setSelectedIndex(i);
                return;
            }

            popup.getList().clearSelection();
        }
    }

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    class AquaCustomComboTextField extends JTextField {
        @SuppressWarnings("serial") // anonymous class
        public AquaCustomComboTextField() {
            final InputMap inputMap = getInputMap();
            inputMap.put(KeyStroke.getKeyStroke("DOWN"), highlightNextAction);
            inputMap.put(KeyStroke.getKeyStroke("KP_DOWN"), highlightNextAction);
            inputMap.put(KeyStroke.getKeyStroke("UP"), highlightPreviousAction);
            inputMap.put(KeyStroke.getKeyStroke("KP_UP"), highlightPreviousAction);

            inputMap.put(KeyStroke.getKeyStroke("HOME"), highlightFirstAction);
            inputMap.put(KeyStroke.getKeyStroke("END"), highlightLastAction);
            inputMap.put(KeyStroke.getKeyStroke("PAGE_UP"), highlightPageUpAction);
            inputMap.put(KeyStroke.getKeyStroke("PAGE_DOWN"), highlightPageDownAction);

            final Action action = getActionMap().get(JTextField.notifyAction);
            inputMap.put(KeyStroke.getKeyStroke("ENTER"), new AbstractAction() {
                public void actionPerformed(final ActionEvent e) {
                    if (popup.isVisible()) {
                        triggerSelectionEvent(comboBox, e);

                        if (editor instanceof AquaCustomComboTextField) {
                            ((AquaCustomComboTextField)editor).selectAll();
                        }
                    } else {
                        action.actionPerformed(e);
                    }
                }
            });
        }

        // workaround for 4530952
        public void setText(final String s) {
            if (getText().equals(s)) {
                return;
            }
            super.setText(s);
        }
    }

    /**
     * This listener hides the popup when the focus is lost.  It also repaints
     * when focus is gained or lost.
     *
     * This override is necessary because the Basic L&F for the combo box is working
     * around a Solaris-only bug that we don't have on Mac OS X.  So, remove the lightweight
     * popup check here. rdar://Problem/3518582
     */
    protected FocusListener createFocusListener() {
        return new BasicComboBoxUI.FocusHandler() {
            @Override
            public void focusGained(FocusEvent e) {
                super.focusGained(e);

                if (arrowButton != null) {
                    arrowButton.repaint();
                }
            }

            @Override
            public void focusLost(final FocusEvent e) {
                hasFocus = false;
                if (!e.isTemporary()) {
                    setPopupVisible(comboBox, false);
                }
                comboBox.repaint();

                // Notify assistive technologies that the combo box lost focus
                final AccessibleContext ac = ((Accessible)comboBox).getAccessibleContext();
                if (ac != null) {
                    ac.firePropertyChange(AccessibleContext.ACCESSIBLE_STATE_PROPERTY, AccessibleState.FOCUSED, null);
                }

                if (arrowButton != null) {
                    arrowButton.repaint();
                }
            }
        };
    }

    protected void installKeyboardActions() {
        super.installKeyboardActions();

        ActionMap actionMap = new ActionMapUIResource();

        actionMap.put("aquaSelectNext", highlightNextAction);
        actionMap.put("aquaSelectPrevious", highlightPreviousAction);
        actionMap.put("enterPressed", triggerSelectionAction);
        actionMap.put("aquaSpacePressed", toggleSelectionAction);

        actionMap.put("aquaSelectHome", highlightFirstAction);
        actionMap.put("aquaSelectEnd", highlightLastAction);
        actionMap.put("aquaSelectPageUp", highlightPageUpAction);
        actionMap.put("aquaSelectPageDown", highlightPageDownAction);

        actionMap.put("aquaHidePopup", hideAction);

        SwingUtilities.replaceUIActionMap(comboBox, actionMap);
    }

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    private abstract class ComboBoxAction extends AbstractAction {
        public void actionPerformed(final ActionEvent e) {
            if (!comboBox.isEnabled() || !comboBox.isShowing()) {
                return;
            }

            if (comboBox.isPopupVisible()) {
                final AquaComboBoxUI ui = (AquaComboBoxUI)comboBox.getUI();
                performComboBoxAction(ui);
            } else {
                comboBox.setPopupVisible(true);
            }
        }

        abstract void performComboBoxAction(final AquaComboBoxUI ui);
    }

    /**
     * Hilight _but do not select_ the next item in the list.
     */
    @SuppressWarnings("serial") // anonymous class
    private Action highlightNextAction = new ComboBoxAction() {
        @Override
        public void performComboBoxAction(AquaComboBoxUI ui) {
            final int si = listBox.getSelectedIndex();

            if (si < comboBox.getModel().getSize() - 1) {
                listBox.setSelectedIndex(si + 1);
                listBox.ensureIndexIsVisible(si + 1);
            }
            comboBox.repaint();
        }
    };

    /**
     * Hilight _but do not select_ the previous item in the list.
     */
    @SuppressWarnings("serial") // anonymous class
    private Action highlightPreviousAction = new ComboBoxAction() {
        @Override
        void performComboBoxAction(final AquaComboBoxUI ui) {
            final int si = listBox.getSelectedIndex();
            if (si > 0) {
                listBox.setSelectedIndex(si - 1);
                listBox.ensureIndexIsVisible(si - 1);
            }
            comboBox.repaint();
        }
    };

    @SuppressWarnings("serial") // anonymous class
    private Action highlightFirstAction = new ComboBoxAction() {
        @Override
        void performComboBoxAction(final AquaComboBoxUI ui) {
            listBox.setSelectedIndex(0);
            listBox.ensureIndexIsVisible(0);
        }
    };

    @SuppressWarnings("serial") // anonymous class
    private Action highlightLastAction = new ComboBoxAction() {
        @Override
        void performComboBoxAction(final AquaComboBoxUI ui) {
            final int size = listBox.getModel().getSize();
            listBox.setSelectedIndex(size - 1);
            listBox.ensureIndexIsVisible(size - 1);
        }
    };

    @SuppressWarnings("serial") // anonymous class
    private Action highlightPageUpAction = new ComboBoxAction() {
        @Override
        void performComboBoxAction(final AquaComboBoxUI ui) {
            final int current = listBox.getSelectedIndex();
            final int first = listBox.getFirstVisibleIndex();

            if (current != first) {
                listBox.setSelectedIndex(first);
                return;
            }

            final int page = listBox.getVisibleRect().height / listBox.getCellBounds(0, 0).height;
            int target = first - page;
            if (target < 0) target = 0;

            listBox.ensureIndexIsVisible(target);
            listBox.setSelectedIndex(target);
        }
    };

    @SuppressWarnings("serial") // anonymous class
    private Action highlightPageDownAction = new ComboBoxAction() {
        @Override
        void performComboBoxAction(final AquaComboBoxUI ui) {
            final int current = listBox.getSelectedIndex();
            final int last = listBox.getLastVisibleIndex();

            if (current != last) {
                listBox.setSelectedIndex(last);
                return;
            }

            final int page = listBox.getVisibleRect().height / listBox.getCellBounds(0, 0).height;
            final int end = listBox.getModel().getSize() - 1;
            int target = last + page;
            if (target > end) target = end;

            listBox.ensureIndexIsVisible(target);
            listBox.setSelectedIndex(target);
        }
    };

    // For <rdar://problem/3759984> Java 1.4.2_5: Serializing Swing components not working
    // Inner classes were using a this reference and then trying to serialize the AquaComboBoxUI
    // We shouldn't do that. But we need to be able to get the popup from other classes, so we need
    // a public accessor.
    public ComboPopup getPopup() {
        return popup;
    }

    protected LayoutManager createLayoutManager() {
        return new AquaComboBoxLayoutManager();
    }

    class AquaComboBoxLayoutManager extends BasicComboBoxUI.ComboBoxLayoutManager {
        public void layoutContainer(final Container parent) {
            if (arrowButton != null && !comboBox.isEditable()) {
                final Insets insets = comboBox.getInsets();
                final int width = comboBox.getWidth();
                final int height = comboBox.getHeight();
                arrowButton.setBounds(insets.left, insets.top, width - (insets.left + insets.right), height - (insets.top + insets.bottom));
                return;
            }

            final JComboBox<?> cb = (JComboBox<?>) parent;
            final int width = cb.getWidth();
            final int height = cb.getHeight();

            final Insets insets = getInsets();
            final int buttonHeight = height - (insets.top + insets.bottom);
            final int buttonWidth = 20;

            if (arrowButton != null) {
                arrowButton.setBounds(width - (insets.right + buttonWidth), insets.top, buttonWidth, buttonHeight);
            }

            if (editor != null) {
                final Rectangle editorRect = rectangleForCurrentValue();
                editorRect.width += 4;
                editorRect.height += 1;
                editor.setBounds(editorRect);
            }
        }
    }

    // This is here because Sun can't use protected like they should!
    protected static final String IS_TABLE_CELL_EDITOR = "JComboBox.isTableCellEditor";

    protected static boolean isTableCellEditor(final JComponent c) {
        return Boolean.TRUE.equals(c.getClientProperty(AquaComboBoxUI.IS_TABLE_CELL_EDITOR));
    }

    protected static boolean isPopdown(final JComboBox<?> c) {
        return c.isEditable() || Boolean.TRUE.equals(c.getClientProperty(AquaComboBoxUI.POPDOWN_CLIENT_PROPERTY_KEY));
    }

    protected static void triggerSelectionEvent(final JComboBox<?> comboBox, final ActionEvent e) {
        if (!comboBox.isEnabled()) return;

        final AquaComboBoxUI aquaUi = (AquaComboBoxUI)comboBox.getUI();

        if (aquaUi.getPopup().getList().getSelectedIndex() < 0) {
            comboBox.setPopupVisible(false);
        }

        if (isTableCellEditor(comboBox)) {
            // Forces the selection of the list item if the combo box is in a JTable
            comboBox.setSelectedIndex(aquaUi.getPopup().getList().getSelectedIndex());
            return;
        }

        if (comboBox.isPopupVisible()) {
            comboBox.setSelectedIndex(aquaUi.getPopup().getList().getSelectedIndex());
            comboBox.setPopupVisible(false);
            return;
        }

        // Call the default button binding.
        // This is a pretty messy way of passing an event through to the root pane
        final JRootPane root = SwingUtilities.getRootPane(comboBox);
        if (root == null) return;

        final InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        final ActionMap am = root.getActionMap();
        if (im == null || am == null) return;

        final Object obj = im.get(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
        if (obj == null) return;

        final Action action = am.get(obj);
        if (action == null) return;

        action.actionPerformed(new ActionEvent(root, e.getID(), e.getActionCommand(), e.getWhen(), e.getModifiers()));
    }

    // This is somewhat messy.  The difference here from BasicComboBoxUI.EnterAction is that
    // arrow up or down does not automatically select the
    @SuppressWarnings("serial") // anonymous class
    private final Action triggerSelectionAction = new AbstractAction() {
        public void actionPerformed(final ActionEvent e) {
            triggerSelectionEvent((JComboBox)e.getSource(), e);
        }

        @Override
        public boolean isEnabled() {
            return comboBox.isPopupVisible() && super.isEnabled();
        }
    };

    @SuppressWarnings("serial") // anonymous class
    private static final Action toggleSelectionAction = new AbstractAction() {
        public void actionPerformed(final ActionEvent e) {
            final JComboBox<?> comboBox = (JComboBox<?>) e.getSource();
            if (!comboBox.isEnabled()) return;
            if (comboBox.isEditable()) return;

            final AquaComboBoxUI aquaUi = (AquaComboBoxUI)comboBox.getUI();

            if (comboBox.isPopupVisible()) {
                comboBox.setSelectedIndex(aquaUi.getPopup().getList().getSelectedIndex());
                comboBox.setPopupVisible(false);
                return;
            }

            comboBox.setPopupVisible(true);
        }
    };

    @SuppressWarnings("serial") // anonymous class
    private final Action hideAction = new AbstractAction() {
        @Override
        public void actionPerformed(final ActionEvent e) {
            final JComboBox<?> comboBox = (JComboBox<?>) e.getSource();
            comboBox.firePopupMenuCanceled();
            comboBox.setPopupVisible(false);
        }

        @Override
        public boolean isEnabled() {
            return comboBox.isPopupVisible() && super.isEnabled();
        }
    };

    public void applySizeFor(final JComponent c, final Size size) {
        if (arrowButton == null) return;
        final Border border = arrowButton.getBorder();
        if (!(border instanceof AquaButtonBorder)) return;
        final AquaButtonBorder aquaBorder = (AquaButtonBorder)border;
        arrowButton.setBorder(aquaBorder.deriveBorderForSize(size));
    }

    public Dimension getMinimumSize(final JComponent c) {
        if (!isMinimumSizeDirty) {
            return new Dimension(cachedMinimumSize);
        }

        final boolean editable = comboBox.isEditable();

        final Dimension size;
        if (!editable && arrowButton != null && arrowButton instanceof AquaComboBoxButton) {
            final AquaComboBoxButton button = (AquaComboBoxButton)arrowButton;
            final Insets buttonInsets = button.getInsets();
            //  Insets insets = comboBox.getInsets();
            final Insets insets = new Insets(0, 5, 0, 25);//comboBox.getInsets();

            size = getDisplaySize();
            size.width += insets.left + insets.right;
            size.width += buttonInsets.left + buttonInsets.right;
            size.width += buttonInsets.right + 10;
            size.height += insets.top + insets.bottom;
            size.height += buttonInsets.top + buttonInsets.bottom;
            // Min height = Height of arrow button plus 2 pixels fuzz above plus 2 below.  23 + 2 + 2
            size.height = Math.max(27, size.height);
        } else if (editable && arrowButton != null && editor != null) {
            size = super.getMinimumSize(c);
            final Insets margin = arrowButton.getMargin();
            size.height += margin.top + margin.bottom;
        } else {
            size = super.getMinimumSize(c);
        }

        final Border border = c.getBorder();
        if (border != null) {
            final Insets insets = border.getBorderInsets(c);
            size.height += insets.top + insets.bottom;
            size.width += insets.left + insets.right;
        }

        cachedMinimumSize.setSize(size.width, size.height);
        isMinimumSizeDirty = false;

        return new Dimension(cachedMinimumSize);
    }

    @SuppressWarnings("unchecked")
    private static final RecyclableSingleton<ClientPropertyApplicator<JComboBox<?>, AquaComboBoxUI>> APPLICATOR = new
            RecyclableSingleton<ClientPropertyApplicator<JComboBox<?>, AquaComboBoxUI>>() {
        @Override
        protected ClientPropertyApplicator<JComboBox<?>, AquaComboBoxUI> getInstance() {
            return new ClientPropertyApplicator<JComboBox<?>, AquaComboBoxUI>(
                new Property<AquaComboBoxUI>(AquaFocusHandler.FRAME_ACTIVE_PROPERTY) {
                    public void applyProperty(final AquaComboBoxUI target, final Object value) {
                        if (Boolean.FALSE.equals(value)) {
                            if (target.comboBox != null) target.comboBox.hidePopup();
                        }
                        if (target.listBox != null) target.listBox.repaint();
                    }
                },
                new Property<AquaComboBoxUI>("editable") {
                    public void applyProperty(final AquaComboBoxUI target, final Object value) {
                        if (target.comboBox == null) return;
                        target.comboBox.repaint();
                    }
                },
                new Property<AquaComboBoxUI>("background") {
                    public void applyProperty(final AquaComboBoxUI target, final Object value) {
                        final Color color = (Color)value;
                        if (target.arrowButton != null) target.arrowButton.setBackground(color);
                        if (target.listBox != null) target.listBox.setBackground(color);
                    }
                },
                new Property<AquaComboBoxUI>("foreground") {
                    public void applyProperty(final AquaComboBoxUI target, final Object value) {
                        final Color color = (Color)value;
                        if (target.arrowButton != null) target.arrowButton.setForeground(color);
                        if (target.listBox != null) target.listBox.setForeground(color);
                    }
                },
                new Property<AquaComboBoxUI>(POPDOWN_CLIENT_PROPERTY_KEY) {
                    public void applyProperty(final AquaComboBoxUI target, final Object value) {
                        if (!(target.arrowButton instanceof AquaComboBoxButton)) return;
                        ((AquaComboBoxButton)target.arrowButton).setIsPopDown(Boolean.TRUE.equals(value));
                    }
                },
                new Property<AquaComboBoxUI>(ISSQUARE_CLIENT_PROPERTY_KEY) {
                    public void applyProperty(final AquaComboBoxUI target, final Object value) {
                        if (!(target.arrowButton instanceof AquaComboBoxButton)) return;
                        ((AquaComboBoxButton)target.arrowButton).setIsSquare(Boolean.TRUE.equals(value));
                    }
                }
            ) {
                public AquaComboBoxUI convertJComponentToTarget(final JComboBox<?> combo) {
                    final ComboBoxUI comboUI = combo.getUI();
                    if (comboUI instanceof AquaComboBoxUI) return (AquaComboBoxUI)comboUI;
                    return null;
                }
            };
        }
    };
    static ClientPropertyApplicator<JComboBox<?>, AquaComboBoxUI> getApplicator() {
        return APPLICATOR.get();
    }
}
