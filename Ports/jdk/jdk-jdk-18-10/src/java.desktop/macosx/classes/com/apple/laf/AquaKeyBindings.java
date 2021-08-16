/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.event.ActionEvent;
import java.util.*;

import javax.swing.*;
import javax.swing.UIDefaults.LazyValue;
import javax.swing.text.*;
import javax.swing.text.DefaultEditorKit.DefaultKeyTypedAction;

import com.apple.laf.AquaUtils.RecyclableSingleton;
import com.apple.laf.AquaUtils.RecyclableSingletonFromDefaultConstructor;

public class AquaKeyBindings {
    private static final RecyclableSingleton<AquaKeyBindings> instance = new RecyclableSingletonFromDefaultConstructor<AquaKeyBindings>(AquaKeyBindings.class);
    static AquaKeyBindings instance() {
        return instance.get();
    }

    final DefaultKeyTypedAction defaultKeyTypedAction = new DefaultKeyTypedAction();
    void setDefaultAction(final String keymapName) {
        final javax.swing.text.Keymap map = JTextComponent.getKeymap(keymapName);
        map.setDefaultAction(defaultKeyTypedAction);
    }

    static final String upMultilineAction = "aqua-move-up";
    static final String downMultilineAction = "aqua-move-down";
    static final String pageUpMultiline = "aqua-page-up";
    static final String pageDownMultiline = "aqua-page-down";

    final String[] commonTextEditorBindings = {
        "ENTER", JTextField.notifyAction,
        "COPY", DefaultEditorKit.copyAction,
        "CUT", DefaultEditorKit.cutAction,
        "PASTE", DefaultEditorKit.pasteAction,
        "meta A", DefaultEditorKit.selectAllAction,
        "meta C", DefaultEditorKit.copyAction,
        "meta V", DefaultEditorKit.pasteAction,
        "meta X", DefaultEditorKit.cutAction,
        "meta BACK_SLASH", "unselect",

        "DELETE", DefaultEditorKit.deleteNextCharAction,
        "alt DELETE", "delete-next-word",
        "BACK_SPACE", DefaultEditorKit.deletePrevCharAction,
        "shift BACK_SPACE", DefaultEditorKit.deletePrevCharAction,
        "alt BACK_SPACE", "delete-previous-word",

        "LEFT", DefaultEditorKit.backwardAction,
        "KP_LEFT", DefaultEditorKit.backwardAction,
        "RIGHT", DefaultEditorKit.forwardAction,
        "KP_RIGHT", DefaultEditorKit.forwardAction,
        "shift LEFT", DefaultEditorKit.selectionBackwardAction,
        "shift KP_LEFT", DefaultEditorKit.selectionBackwardAction,
        "shift RIGHT", DefaultEditorKit.selectionForwardAction,
        "shift KP_RIGHT", DefaultEditorKit.selectionForwardAction,
        "meta LEFT", DefaultEditorKit.beginLineAction,
        "meta KP_LEFT", DefaultEditorKit.beginLineAction,
        "meta RIGHT", DefaultEditorKit.endLineAction,
        "meta KP_RIGHT", DefaultEditorKit.endLineAction,
        "shift meta LEFT", DefaultEditorKit.selectionBeginLineAction,
        "shift meta KP_LEFT", DefaultEditorKit.selectionBeginLineAction,
        "shift meta RIGHT", DefaultEditorKit.selectionEndLineAction,
        "shift meta KP_RIGHT", DefaultEditorKit.selectionEndLineAction,
        "alt LEFT", DefaultEditorKit.previousWordAction,
        "alt KP_LEFT", DefaultEditorKit.previousWordAction,
        "alt RIGHT", DefaultEditorKit.nextWordAction,
        "alt KP_RIGHT", DefaultEditorKit.nextWordAction,
        "shift alt LEFT", DefaultEditorKit.selectionPreviousWordAction,
        "shift alt KP_LEFT", DefaultEditorKit.selectionPreviousWordAction,
        "shift alt RIGHT", DefaultEditorKit.selectionNextWordAction,
        "shift alt KP_RIGHT", DefaultEditorKit.selectionNextWordAction,

        "control A", DefaultEditorKit.beginLineAction,
        "control B", DefaultEditorKit.backwardAction,
        "control D", DefaultEditorKit.deleteNextCharAction,
        "control E", DefaultEditorKit.endLineAction,
        "control F", DefaultEditorKit.forwardAction,
        "control H", DefaultEditorKit.deletePrevCharAction,
        "control W", "delete-previous-word",
        "control shift O", "toggle-componentOrientation",

        "END", DefaultEditorKit.endAction,
        "HOME", DefaultEditorKit.beginAction,
        "shift END", DefaultEditorKit.selectionEndAction,
        "shift HOME", DefaultEditorKit.selectionBeginAction,

        "PAGE_DOWN", pageDownMultiline,
        "PAGE_UP", pageUpMultiline,
        "shift PAGE_DOWN", "selection-page-down",
        "shift PAGE_UP", "selection-page-up",
        "meta shift PAGE_DOWN", "selection-page-right",
        "meta shift PAGE_UP", "selection-page-left",

        "meta DOWN", DefaultEditorKit.endAction,
        "meta KP_DOWN", DefaultEditorKit.endAction,
        "meta UP", DefaultEditorKit.beginAction,
        "meta KP_UP", DefaultEditorKit.beginAction,
        "shift meta DOWN", DefaultEditorKit.selectionEndAction,
        "shift meta KP_DOWN", DefaultEditorKit.selectionEndAction,
        "shift meta UP", DefaultEditorKit.selectionBeginAction,
        "shift meta KP_UP", DefaultEditorKit.selectionBeginAction,
    };

    LateBoundInputMap getTextFieldInputMap() {
        return new LateBoundInputMap(new SimpleBinding(commonTextEditorBindings), new SimpleBinding(new String[] {
            "DOWN", DefaultEditorKit.endLineAction,
            "KP_DOWN", DefaultEditorKit.endLineAction,
            "UP", DefaultEditorKit.beginLineAction,
            "KP_UP", DefaultEditorKit.beginLineAction,
            "shift DOWN", DefaultEditorKit.selectionEndLineAction,
            "shift KP_DOWN", DefaultEditorKit.selectionEndLineAction,
            "shift UP", DefaultEditorKit.selectionBeginLineAction,
            "shift KP_UP", DefaultEditorKit.selectionBeginLineAction,

            "control P", DefaultEditorKit.beginAction,
            "control N", DefaultEditorKit.endAction,
            "control V", DefaultEditorKit.endAction,
        }));
    }

    LateBoundInputMap getPasswordFieldInputMap() {
        return new LateBoundInputMap(new SimpleBinding(getTextFieldInputMap().getBindings()),
                // nullify all the bindings that may discover space characters in the text
                new SimpleBinding(new String[] {
                        "alt LEFT", null,
                        "alt KP_LEFT", null,
                        "alt RIGHT", null,
                        "alt KP_RIGHT", null,
                        "shift alt LEFT", null,
                        "shift alt KP_LEFT", null,
                        "shift alt RIGHT", null,
                        "shift alt KP_RIGHT", null,
                }));
    }

    LateBoundInputMap getMultiLineTextInputMap() {
        return new LateBoundInputMap(new SimpleBinding(commonTextEditorBindings), new SimpleBinding(new String[] {
            "ENTER", DefaultEditorKit.insertBreakAction,
            "DOWN", downMultilineAction,
            "KP_DOWN", downMultilineAction,
            "UP", upMultilineAction,
            "KP_UP", upMultilineAction,
            "shift DOWN", DefaultEditorKit.selectionDownAction,
            "shift KP_DOWN", DefaultEditorKit.selectionDownAction,
            "shift UP", DefaultEditorKit.selectionUpAction,
            "shift KP_UP", DefaultEditorKit.selectionUpAction,
            "alt shift DOWN", DefaultEditorKit.selectionEndParagraphAction,
            "alt shift KP_DOWN", DefaultEditorKit.selectionEndParagraphAction,
            "alt shift UP", DefaultEditorKit.selectionBeginParagraphAction,
            "alt shift KP_UP", DefaultEditorKit.selectionBeginParagraphAction,

            "control P", DefaultEditorKit.upAction,
            "control N", DefaultEditorKit.downAction,
            "control V", pageDownMultiline,

            "TAB", DefaultEditorKit.insertTabAction,
            "meta SPACE", "activate-link-action",
            "meta T", "next-link-action",
            "meta shift T", "previous-link-action",

            "END", DefaultEditorKit.endAction,
            "HOME", DefaultEditorKit.beginAction,
            "shift END", DefaultEditorKit.selectionEndAction,
            "shift HOME", DefaultEditorKit.selectionBeginAction,

            "PAGE_DOWN", pageDownMultiline,
            "PAGE_UP", pageUpMultiline,
            "shift PAGE_DOWN", "selection-page-down",
            "shift PAGE_UP", "selection-page-up",
            "meta shift PAGE_DOWN", "selection-page-right",
            "meta shift PAGE_UP", "selection-page-left",
        }));
    }

    LateBoundInputMap getFormattedTextFieldInputMap() {
        return new LateBoundInputMap(getTextFieldInputMap(), new SimpleBinding(new String[] {
            "UP", "increment",
            "KP_UP", "increment",
            "DOWN", "decrement",
            "KP_DOWN", "decrement",

            "ESCAPE", "reset-field-edit",
        }));
    }

    LateBoundInputMap getComboBoxInputMap() {
        return new LateBoundInputMap(new SimpleBinding(new String[] {
            "ESCAPE", "aquaHidePopup",
            "PAGE_UP", "aquaSelectPageUp",
            "PAGE_DOWN", "aquaSelectPageDown",
            "HOME", "aquaSelectHome",
            "END", "aquaSelectEnd",
            "ENTER", "enterPressed",
            "UP", "aquaSelectPrevious",
            "KP_UP", "aquaSelectPrevious",
            "DOWN", "aquaSelectNext",
            "KP_DOWN", "aquaSelectNext",
            "SPACE", "aquaSpacePressed" // "spacePopup"
        }));
    }

    LateBoundInputMap getListInputMap() {
        return new LateBoundInputMap(new SimpleBinding(new String[] {
            "meta C", "copy",
            "meta V", "paste",
            "meta X", "cut",
            "COPY", "copy",
            "PASTE", "paste",
            "CUT", "cut",
            "UP", "selectPreviousRow",
            "KP_UP", "selectPreviousRow",
            "shift UP", "selectPreviousRowExtendSelection",
            "shift KP_UP", "selectPreviousRowExtendSelection",
            "DOWN", "selectNextRow",
            "KP_DOWN", "selectNextRow",
            "shift DOWN", "selectNextRowExtendSelection",
            "shift KP_DOWN", "selectNextRowExtendSelection",
            "LEFT", "selectPreviousColumn",
            "KP_LEFT", "selectPreviousColumn",
            "shift LEFT", "selectPreviousColumnExtendSelection",
            "shift KP_LEFT", "selectPreviousColumnExtendSelection",
            "RIGHT", "selectNextColumn",
            "KP_RIGHT", "selectNextColumn",
            "shift RIGHT", "selectNextColumnExtendSelection",
            "shift KP_RIGHT", "selectNextColumnExtendSelection",
            "meta A", "selectAll",

            // aquaHome and aquaEnd are new actions that just move the view so the first or last item is visible.
            "HOME", "aquaHome",
            "shift HOME", "selectFirstRowExtendSelection",
            "END", "aquaEnd",
            "shift END", "selectLastRowExtendSelection",

            // Unmodified PAGE_UP and PAGE_DOWN are handled by their scroll pane, if any.
            "shift PAGE_UP", "scrollUpExtendSelection",
            "shift PAGE_DOWN", "scrollDownExtendSelection"
        }));
    }

    LateBoundInputMap getScrollBarInputMap() {
        return new LateBoundInputMap(new SimpleBinding(new String[] {
            "RIGHT", "positiveUnitIncrement",
            "KP_RIGHT", "positiveUnitIncrement",
            "DOWN", "positiveUnitIncrement",
            "KP_DOWN", "positiveUnitIncrement",
            "PAGE_DOWN", "positiveBlockIncrement",
            "LEFT", "negativeUnitIncrement",
            "KP_LEFT", "negativeUnitIncrement",
            "UP", "negativeUnitIncrement",
            "KP_UP", "negativeUnitIncrement",
            "PAGE_UP", "negativeBlockIncrement",
            "HOME", "minScroll",
            "END", "maxScroll"
        }));
    }

    LateBoundInputMap getScrollBarRightToLeftInputMap() {
        return new LateBoundInputMap(new SimpleBinding(new String[] {
            "RIGHT", "negativeUnitIncrement",
            "KP_RIGHT", "negativeUnitIncrement",
            "LEFT", "positiveUnitIncrement",
            "KP_LEFT", "positiveUnitIncrement"
        }));
    }

    LateBoundInputMap getScrollPaneInputMap() {
        return new LateBoundInputMap(new SimpleBinding(new String[] {
            "RIGHT", "unitScrollRight",
            "KP_RIGHT", "unitScrollRight",
            "DOWN", "unitScrollDown",
            "KP_DOWN", "unitScrollDown",
            "LEFT", "unitScrollLeft",
            "KP_LEFT", "unitScrollLeft",
            "UP", "unitScrollUp",
            "KP_UP", "unitScrollUp",
            "PAGE_UP", "scrollUp",
            "PAGE_DOWN", "scrollDown",
            "HOME", "scrollHome",
            "END", "scrollEnd"
        }));
    }

    LateBoundInputMap getSliderInputMap() {
        return new LateBoundInputMap(new SimpleBinding(new String[] {
            "RIGHT", "positiveUnitIncrement",
            "KP_RIGHT", "positiveUnitIncrement",
            "DOWN", "negativeUnitIncrement",
            "KP_DOWN", "negativeUnitIncrement",
            "PAGE_DOWN", "negativeBlockIncrement",
            "LEFT", "negativeUnitIncrement",
            "KP_LEFT", "negativeUnitIncrement",
            "UP", "positiveUnitIncrement",
            "KP_UP", "positiveUnitIncrement",
            "PAGE_UP", "positiveBlockIncrement",
            "HOME", "minScroll",
            "END", "maxScroll"
        }));
    }

    LateBoundInputMap getSliderRightToLeftInputMap() {
        return new LateBoundInputMap(new SimpleBinding(new String[] {
            "RIGHT", "negativeUnitIncrement",
            "KP_RIGHT", "negativeUnitIncrement",
            "LEFT", "positiveUnitIncrement",
            "KP_LEFT", "positiveUnitIncrement"
        }));
    }

    LateBoundInputMap getSpinnerInputMap() {
        return new LateBoundInputMap(new SimpleBinding(new String[] {
            "UP", "increment",
            "KP_UP", "increment",
            "DOWN", "decrement",
            "KP_DOWN", "decrement"
        }));
    }

    LateBoundInputMap getTableInputMap() {
        return new LateBoundInputMap(new SimpleBinding(new String[] {
            "meta C", "copy",
            "meta V", "paste",
            "meta X", "cut",
            "COPY", "copy",
            "PASTE", "paste",
            "CUT", "cut",
            "RIGHT", "selectNextColumn",
            "KP_RIGHT", "selectNextColumn",
            "LEFT", "selectPreviousColumn",
            "KP_LEFT", "selectPreviousColumn",
            "DOWN", "selectNextRow",
            "KP_DOWN", "selectNextRow",
            "UP", "selectPreviousRow",
            "KP_UP", "selectPreviousRow",
            "shift RIGHT", "selectNextColumnExtendSelection",
            "shift KP_RIGHT", "selectNextColumnExtendSelection",
            "shift LEFT", "selectPreviousColumnExtendSelection",
            "shift KP_LEFT", "selectPreviousColumnExtendSelection",
            "shift DOWN", "selectNextRowExtendSelection",
            "shift KP_DOWN", "selectNextRowExtendSelection",
            "shift UP", "selectPreviousRowExtendSelection",
            "shift KP_UP", "selectPreviousRowExtendSelection",
            "PAGE_UP", "scrollUpChangeSelection",
            "PAGE_DOWN", "scrollDownChangeSelection",
            "HOME", "selectFirstColumn",
            "END", "selectLastColumn",
            "shift PAGE_UP", "scrollUpExtendSelection",
            "shift PAGE_DOWN", "scrollDownExtendSelection",
            "shift HOME", "selectFirstColumnExtendSelection",
            "shift END", "selectLastColumnExtendSelection",
            "TAB", "selectNextColumnCell",
            "shift TAB", "selectPreviousColumnCell",
            "meta A", "selectAll",
            "ESCAPE", "cancel",
            "ENTER", "selectNextRowCell",
            "shift ENTER", "selectPreviousRowCell",
            "alt TAB", "focusHeader",
            "alt shift TAB", "focusHeader"
        }));
    }

    LateBoundInputMap getTableRightToLeftInputMap() {
        return new LateBoundInputMap(new SimpleBinding(new String[] {
            "RIGHT", "selectPreviousColumn",
            "KP_RIGHT", "selectPreviousColumn",
            "LEFT", "selectNextColumn",
            "KP_LEFT", "selectNextColumn",
            "shift RIGHT", "selectPreviousColumnExtendSelection",
            "shift KP_RIGHT", "selectPreviousColumnExtendSelection",
            "shift LEFT", "selectNextColumnExtendSelection",
            "shift KP_LEFT", "selectNextColumnExtendSelection",
            "ctrl PAGE_UP", "scrollRightChangeSelection",
            "ctrl PAGE_DOWN", "scrollLeftChangeSelection",
            "ctrl shift PAGE_UP", "scrollRightExtendSelection",
            "ctrl shift PAGE_DOWN", "scrollLeftExtendSelection"
        }));
    }

    LateBoundInputMap getTreeInputMap() {
        return new LateBoundInputMap(new SimpleBinding(new String[] {
            "meta C", "copy",
            "meta V", "paste",
            "meta X", "cut",
            "COPY", "copy",
            "PASTE", "paste",
            "CUT", "cut",
            "UP", "selectPrevious",
            "KP_UP", "selectPrevious",
            "shift UP", "selectPreviousExtendSelection",
            "shift KP_UP", "selectPreviousExtendSelection",
            "DOWN", "selectNext",
            "KP_DOWN", "selectNext",
            "shift DOWN", "selectNextExtendSelection",
            "shift KP_DOWN", "selectNextExtendSelection",
            "RIGHT", "aquaExpandNode",
            "KP_RIGHT", "aquaExpandNode",
            "LEFT", "aquaCollapseNode",
            "KP_LEFT", "aquaCollapseNode",
            "shift RIGHT", "aquaExpandNode",
            "shift KP_RIGHT", "aquaExpandNode",
            "shift LEFT", "aquaCollapseNode",
            "shift KP_LEFT", "aquaCollapseNode",
            "ctrl LEFT", "aquaCollapseNode",
            "ctrl KP_LEFT", "aquaCollapseNode",
            "ctrl RIGHT", "aquaExpandNode",
            "ctrl KP_RIGHT", "aquaExpandNode",
            "alt RIGHT", "aquaFullyExpandNode",
            "alt KP_RIGHT", "aquaFullyExpandNode",
            "alt LEFT", "aquaFullyCollapseNode",
            "alt KP_LEFT", "aquaFullyCollapseNode",
            "meta A", "selectAll",
            "RETURN", "startEditing"
        }));
    }

    LateBoundInputMap getTreeRightToLeftInputMap() {
        return new LateBoundInputMap(new SimpleBinding(new String[] {
            "RIGHT", "aquaCollapseNode",
            "KP_RIGHT", "aquaCollapseNode",
            "LEFT", "aquaExpandNode",
            "KP_LEFT", "aquaExpandNode",
            "shift RIGHT", "aquaCollapseNode",
            "shift KP_RIGHT", "aquaCollapseNode",
            "shift LEFT", "aquaExpandNode",
            "shift KP_LEFT", "aquaExpandNode",
            "ctrl LEFT", "aquaExpandNode",
            "ctrl KP_LEFT", "aquaExpandNode",
            "ctrl RIGHT", "aquaCollapseNode",
            "ctrl KP_RIGHT", "aquaCollapseNode"
        }));
    }

    // common interface between a string array, and a dynamic provider of string arrays ;-)
    interface BindingsProvider {
        public String[] getBindings();
    }

    // wraps basic string arrays
    static class SimpleBinding implements BindingsProvider {
        final String[] bindings;
        public SimpleBinding(final String[] bindings) { this.bindings = bindings; }
        public String[] getBindings() { return bindings; }
    }

    // patches all providers together at the moment the UIManager needs the real InputMap
    static class LateBoundInputMap implements LazyValue, BindingsProvider {
        private final BindingsProvider[] providerList;
        private String[] mergedBindings;

        public LateBoundInputMap(final BindingsProvider ... providerList) {
            this.providerList = providerList;
        }

        public Object createValue(final UIDefaults table) {
            return LookAndFeel.makeInputMap(getBindings());
        }

        public String[] getBindings() {
            if (mergedBindings != null) return mergedBindings;

            final String[][] bindingsList = new String[providerList.length][];
            int size = 0;
            for (int i = 0; i < providerList.length; i++) {
                bindingsList[i] = providerList[i].getBindings();
                size += bindingsList[i].length;
            }

            if (bindingsList.length == 1) {
                return mergedBindings = bindingsList[0];
            }

            final ArrayList<String> unifiedList = new ArrayList<String>(size);
            Collections.addAll(unifiedList, bindingsList[0]); // System.arrayCopy() the first set

            for (int i = 1; i < providerList.length; i++) {
                mergeBindings(unifiedList, bindingsList[i]);
            }

            return mergedBindings = unifiedList.toArray(new String[unifiedList.size()]);
        }

        static void mergeBindings(final ArrayList<String> unifiedList, final String[] overrides) {
            for (int i = 0; i < overrides.length; i+=2) {
                final String key = overrides[i];
                final String value = overrides[i+1];

                final int keyIndex = unifiedList.indexOf(key);
                if (keyIndex == -1) {
                    unifiedList.add(key);
                    unifiedList.add(value);
                } else {
                    unifiedList.set(keyIndex, key);
                    unifiedList.set(keyIndex + 1, value);
                }
            }
        }
    }

    void installAquaUpDownActions(final JTextComponent component) {
        final ActionMap actionMap = component.getActionMap();
        actionMap.put(upMultilineAction, moveUpMultilineAction);
        actionMap.put(downMultilineAction, moveDownMultilineAction);
        actionMap.put(pageUpMultiline, pageUpMultilineAction);
        actionMap.put(pageDownMultiline, pageDownMultilineAction);
    }

    // extracted and adapted from DefaultEditorKit in 1.6
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    abstract static class DeleteWordAction extends TextAction {
        public DeleteWordAction(final String name) { super(name); }

        public void actionPerformed(final ActionEvent e) {
            if (e == null) return;

            final JTextComponent target = getTextComponent(e);
            if (target == null) return;

            if (!target.isEditable() || !target.isEnabled()) {
                UIManager.getLookAndFeel().provideErrorFeedback(target);
                return;
            }

            try {
                final int start = target.getSelectionStart();
                final Element line = Utilities.getParagraphElement(target, start);
                final int end = getEnd(target, line, start);

                final int offs = Math.min(start, end);
                final int len = Math.abs(end - start);
                if (offs >= 0) {
                    target.getDocument().remove(offs, len);
                    return;
                }
            } catch (final BadLocationException ignore) {}
            UIManager.getLookAndFeel().provideErrorFeedback(target);
        }

        abstract int getEnd(final JTextComponent target, final Element line, final int start) throws BadLocationException;
    }

    final TextAction moveUpMultilineAction = new AquaMultilineAction(upMultilineAction, DefaultEditorKit.upAction, DefaultEditorKit.beginAction);
    final TextAction moveDownMultilineAction = new AquaMultilineAction(downMultilineAction, DefaultEditorKit.downAction, DefaultEditorKit.endAction);
    final TextAction pageUpMultilineAction = new AquaMultilineAction(pageUpMultiline, DefaultEditorKit.pageUpAction, DefaultEditorKit.beginAction);
    final TextAction pageDownMultilineAction = new AquaMultilineAction(pageDownMultiline, DefaultEditorKit.pageDownAction, DefaultEditorKit.endAction);

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    static class AquaMultilineAction extends TextAction {
        final String targetActionName;
        final String proxyActionName;

        public AquaMultilineAction(final String actionName, final String targetActionName, final String proxyActionName) {
            super(actionName);
            this.targetActionName = targetActionName;
            this.proxyActionName = proxyActionName;
        }

        public void actionPerformed(final ActionEvent e) {
            final JTextComponent c = getTextComponent(e);
            final ActionMap actionMap = c.getActionMap();
            final Action targetAction = actionMap.get(targetActionName);

            final int startPosition = c.getCaretPosition();
            targetAction.actionPerformed(e);
            if (startPosition != c.getCaretPosition()) return;

            final Action proxyAction = actionMap.get(proxyActionName);
            proxyAction.actionPerformed(e);
        }
    }
}
