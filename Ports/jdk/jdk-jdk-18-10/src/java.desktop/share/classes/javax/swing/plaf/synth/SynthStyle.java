/*
 * Copyright (c) 2002, 2008, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicLookAndFeel;
import javax.swing.text.DefaultEditorKit;
import java.util.HashMap;
import java.util.Map;
import javax.swing.text.JTextComponent;
import sun.swing.SwingUtilities2;

/**
 * <code>SynthStyle</code> is a set of style properties.
 * Each <code>SynthUI</code> references at least one
 * <code>SynthStyle</code> that is obtained using a
 * <code>SynthStyleFactory</code>. You typically don't need to interact with
 * this class directly, rather you will load a
 * <a href="doc-files/synthFileFormat.html">Synth File Format file</a> into
 * <code>SynthLookAndFeel</code> that will create a set of SynthStyles.
 *
 * @see SynthLookAndFeel
 * @see SynthStyleFactory
 *
 * @since 1.5
 * @author Scott Violet
 */
public abstract class SynthStyle {
    /**
     * Contains the default values for certain properties.
     */
    private static Map<Object, Object> DEFAULT_VALUES;

    /**
     * Shared SynthGraphics.
     */
    private static final SynthGraphicsUtils SYNTH_GRAPHICS =
                              new SynthGraphicsUtils();

    /**
     * Adds the default values that we know about to DEFAULT_VALUES.
     */
    private static void populateDefaultValues() {
        Object buttonMap = new UIDefaults.LazyInputMap(new Object[] {
                          "SPACE", "pressed",
                 "released SPACE", "released"
        });
        DEFAULT_VALUES.put("Button.focusInputMap", buttonMap);
        DEFAULT_VALUES.put("CheckBox.focusInputMap", buttonMap);
        DEFAULT_VALUES.put("RadioButton.focusInputMap", buttonMap);
        DEFAULT_VALUES.put("ToggleButton.focusInputMap", buttonMap);
        DEFAULT_VALUES.put("SynthArrowButton.focusInputMap", buttonMap);
        DEFAULT_VALUES.put("List.dropLineColor", Color.BLACK);
        DEFAULT_VALUES.put("Tree.dropLineColor", Color.BLACK);
        DEFAULT_VALUES.put("Table.dropLineColor", Color.BLACK);
        DEFAULT_VALUES.put("Table.dropLineShortColor", Color.RED);

        Object multilineInputMap = new UIDefaults.LazyInputMap(new Object[] {
                           "ctrl C", DefaultEditorKit.copyAction,
                           "ctrl V", DefaultEditorKit.pasteAction,
                           "ctrl X", DefaultEditorKit.cutAction,
                             "COPY", DefaultEditorKit.copyAction,
                            "PASTE", DefaultEditorKit.pasteAction,
                              "CUT", DefaultEditorKit.cutAction,
                   "control INSERT", DefaultEditorKit.copyAction,
                     "shift INSERT", DefaultEditorKit.pasteAction,
                     "shift DELETE", DefaultEditorKit.cutAction,
                       "shift LEFT", DefaultEditorKit.selectionBackwardAction,
                    "shift KP_LEFT", DefaultEditorKit.selectionBackwardAction,
                      "shift RIGHT", DefaultEditorKit.selectionForwardAction,
                   "shift KP_RIGHT", DefaultEditorKit.selectionForwardAction,
                        "ctrl LEFT", DefaultEditorKit.previousWordAction,
                     "ctrl KP_LEFT", DefaultEditorKit.previousWordAction,
                       "ctrl RIGHT", DefaultEditorKit.nextWordAction,
                    "ctrl KP_RIGHT", DefaultEditorKit.nextWordAction,
                  "ctrl shift LEFT", DefaultEditorKit.selectionPreviousWordAction,
               "ctrl shift KP_LEFT", DefaultEditorKit.selectionPreviousWordAction,
                 "ctrl shift RIGHT", DefaultEditorKit.selectionNextWordAction,
              "ctrl shift KP_RIGHT", DefaultEditorKit.selectionNextWordAction,
                           "ctrl A", DefaultEditorKit.selectAllAction,
                             "HOME", DefaultEditorKit.beginLineAction,
                              "END", DefaultEditorKit.endLineAction,
                       "shift HOME", DefaultEditorKit.selectionBeginLineAction,
                        "shift END", DefaultEditorKit.selectionEndLineAction,

                               "UP", DefaultEditorKit.upAction,
                            "KP_UP", DefaultEditorKit.upAction,
                             "DOWN", DefaultEditorKit.downAction,
                          "KP_DOWN", DefaultEditorKit.downAction,
                          "PAGE_UP", DefaultEditorKit.pageUpAction,
                        "PAGE_DOWN", DefaultEditorKit.pageDownAction,
                    "shift PAGE_UP", "selection-page-up",
                  "shift PAGE_DOWN", "selection-page-down",
               "ctrl shift PAGE_UP", "selection-page-left",
             "ctrl shift PAGE_DOWN", "selection-page-right",
                         "shift UP", DefaultEditorKit.selectionUpAction,
                      "shift KP_UP", DefaultEditorKit.selectionUpAction,
                       "shift DOWN", DefaultEditorKit.selectionDownAction,
                    "shift KP_DOWN", DefaultEditorKit.selectionDownAction,
                            "ENTER", DefaultEditorKit.insertBreakAction,
                       "BACK_SPACE", DefaultEditorKit.deletePrevCharAction,
                 "shift BACK_SPACE", DefaultEditorKit.deletePrevCharAction,
                           "ctrl H", DefaultEditorKit.deletePrevCharAction,
                           "DELETE", DefaultEditorKit.deleteNextCharAction,
                      "ctrl DELETE", DefaultEditorKit.deleteNextWordAction,
                  "ctrl BACK_SPACE", DefaultEditorKit.deletePrevWordAction,
                            "RIGHT", DefaultEditorKit.forwardAction,
                             "LEFT", DefaultEditorKit.backwardAction,
                         "KP_RIGHT", DefaultEditorKit.forwardAction,
                          "KP_LEFT", DefaultEditorKit.backwardAction,
                              "TAB", DefaultEditorKit.insertTabAction,
                  "ctrl BACK_SLASH", "unselect"/*DefaultEditorKit.unselectAction*/,
                        "ctrl HOME", DefaultEditorKit.beginAction,
                         "ctrl END", DefaultEditorKit.endAction,
                  "ctrl shift HOME", DefaultEditorKit.selectionBeginAction,
                   "ctrl shift END", DefaultEditorKit.selectionEndAction,
                           "ctrl T", "next-link-action",
                     "ctrl shift T", "previous-link-action",
                       "ctrl SPACE", "activate-link-action",
                   "control shift O", "toggle-componentOrientation"/*DefaultEditorKit.toggleComponentOrientation*/
        });
        DEFAULT_VALUES.put("EditorPane.focusInputMap", multilineInputMap);
        DEFAULT_VALUES.put("TextArea.focusInputMap", multilineInputMap);
        DEFAULT_VALUES.put("TextPane.focusInputMap", multilineInputMap);

        Object fieldInputMap = new UIDefaults.LazyInputMap(new Object[] {
                           "ctrl C", DefaultEditorKit.copyAction,
                           "ctrl V", DefaultEditorKit.pasteAction,
                           "ctrl X", DefaultEditorKit.cutAction,
                             "COPY", DefaultEditorKit.copyAction,
                            "PASTE", DefaultEditorKit.pasteAction,
                              "CUT", DefaultEditorKit.cutAction,
                   "control INSERT", DefaultEditorKit.copyAction,
                     "shift INSERT", DefaultEditorKit.pasteAction,
                     "shift DELETE", DefaultEditorKit.cutAction,
                       "shift LEFT", DefaultEditorKit.selectionBackwardAction,
                    "shift KP_LEFT", DefaultEditorKit.selectionBackwardAction,
                      "shift RIGHT", DefaultEditorKit.selectionForwardAction,
                   "shift KP_RIGHT", DefaultEditorKit.selectionForwardAction,
                        "ctrl LEFT", DefaultEditorKit.previousWordAction,
                     "ctrl KP_LEFT", DefaultEditorKit.previousWordAction,
                       "ctrl RIGHT", DefaultEditorKit.nextWordAction,
                    "ctrl KP_RIGHT", DefaultEditorKit.nextWordAction,
                  "ctrl shift LEFT", DefaultEditorKit.selectionPreviousWordAction,
               "ctrl shift KP_LEFT", DefaultEditorKit.selectionPreviousWordAction,
                 "ctrl shift RIGHT", DefaultEditorKit.selectionNextWordAction,
              "ctrl shift KP_RIGHT", DefaultEditorKit.selectionNextWordAction,
                           "ctrl A", DefaultEditorKit.selectAllAction,
                             "HOME", DefaultEditorKit.beginLineAction,
                              "END", DefaultEditorKit.endLineAction,
                       "shift HOME", DefaultEditorKit.selectionBeginLineAction,
                        "shift END", DefaultEditorKit.selectionEndLineAction,
                       "BACK_SPACE", DefaultEditorKit.deletePrevCharAction,
                 "shift BACK_SPACE", DefaultEditorKit.deletePrevCharAction,
                           "ctrl H", DefaultEditorKit.deletePrevCharAction,
                           "DELETE", DefaultEditorKit.deleteNextCharAction,
                      "ctrl DELETE", DefaultEditorKit.deleteNextWordAction,
                  "ctrl BACK_SPACE", DefaultEditorKit.deletePrevWordAction,
                            "RIGHT", DefaultEditorKit.forwardAction,
                             "LEFT", DefaultEditorKit.backwardAction,
                         "KP_RIGHT", DefaultEditorKit.forwardAction,
                          "KP_LEFT", DefaultEditorKit.backwardAction,
                            "ENTER", JTextField.notifyAction,
                  "ctrl BACK_SLASH", "unselect"/*DefaultEditorKit.unselectAction*/,
                   "control shift O", "toggle-componentOrientation"/*DefaultEditorKit.toggleComponentOrientation*/
        });
        DEFAULT_VALUES.put("TextField.focusInputMap", fieldInputMap);
        DEFAULT_VALUES.put("PasswordField.focusInputMap", fieldInputMap);


        DEFAULT_VALUES.put("ComboBox.ancestorInputMap",
                  new UIDefaults.LazyInputMap(new Object[] {
                     "ESCAPE", "hidePopup",
                    "PAGE_UP", "pageUpPassThrough",
                  "PAGE_DOWN", "pageDownPassThrough",
                       "HOME", "homePassThrough",
                        "END", "endPassThrough",
                       "DOWN", "selectNext",
                    "KP_DOWN", "selectNext",
                   "alt DOWN", "togglePopup",
                "alt KP_DOWN", "togglePopup",
                     "alt UP", "togglePopup",
                  "alt KP_UP", "togglePopup",
                      "SPACE", "spacePopup",
                     "ENTER", "enterPressed",
                         "UP", "selectPrevious",
                      "KP_UP", "selectPrevious"
                  }));

        DEFAULT_VALUES.put("Desktop.ancestorInputMap",
               new UIDefaults.LazyInputMap(new Object[] {
                 "ctrl F5", "restore",
                 "ctrl F4", "close",
                 "ctrl F7", "move",
                 "ctrl F8", "resize",
                   "RIGHT", "right",
                "KP_RIGHT", "right",
             "shift RIGHT", "shrinkRight",
          "shift KP_RIGHT", "shrinkRight",
                    "LEFT", "left",
                 "KP_LEFT", "left",
              "shift LEFT", "shrinkLeft",
           "shift KP_LEFT", "shrinkLeft",
                      "UP", "up",
                   "KP_UP", "up",
                "shift UP", "shrinkUp",
             "shift KP_UP", "shrinkUp",
                    "DOWN", "down",
                 "KP_DOWN", "down",
              "shift DOWN", "shrinkDown",
           "shift KP_DOWN", "shrinkDown",
                  "ESCAPE", "escape",
                 "ctrl F9", "minimize",
                "ctrl F10", "maximize",
                 "ctrl F6", "selectNextFrame",
                "ctrl TAB", "selectNextFrame",
             "ctrl alt F6", "selectNextFrame",
       "shift ctrl alt F6", "selectPreviousFrame",
                "ctrl F12", "navigateNext",
           "shift ctrl F12", "navigatePrevious"
               }));

        DEFAULT_VALUES.put("FileChooser.ancestorInputMap",
               new UIDefaults.LazyInputMap(new Object[] {
                     "ESCAPE", "cancelSelection",
                     "F2", "editFileName",
                     "F5", "refresh",
                     "BACK_SPACE", "Go Up",
                     "ENTER", "approveSelection",
                "ctrl ENTER", "approveSelection"
               }));

        DEFAULT_VALUES.put("FormattedTextField.focusInputMap",
              new UIDefaults.LazyInputMap(new Object[] {
                           "ctrl C", DefaultEditorKit.copyAction,
                           "ctrl V", DefaultEditorKit.pasteAction,
                           "ctrl X", DefaultEditorKit.cutAction,
                             "COPY", DefaultEditorKit.copyAction,
                            "PASTE", DefaultEditorKit.pasteAction,
                              "CUT", DefaultEditorKit.cutAction,
                   "control INSERT", DefaultEditorKit.copyAction,
                     "shift INSERT", DefaultEditorKit.pasteAction,
                     "shift DELETE", DefaultEditorKit.cutAction,
                       "shift LEFT", DefaultEditorKit.selectionBackwardAction,
                    "shift KP_LEFT", DefaultEditorKit.selectionBackwardAction,
                      "shift RIGHT", DefaultEditorKit.selectionForwardAction,
                   "shift KP_RIGHT", DefaultEditorKit.selectionForwardAction,
                        "ctrl LEFT", DefaultEditorKit.previousWordAction,
                     "ctrl KP_LEFT", DefaultEditorKit.previousWordAction,
                       "ctrl RIGHT", DefaultEditorKit.nextWordAction,
                    "ctrl KP_RIGHT", DefaultEditorKit.nextWordAction,
                  "ctrl shift LEFT", DefaultEditorKit.selectionPreviousWordAction,
               "ctrl shift KP_LEFT", DefaultEditorKit.selectionPreviousWordAction,
                 "ctrl shift RIGHT", DefaultEditorKit.selectionNextWordAction,
              "ctrl shift KP_RIGHT", DefaultEditorKit.selectionNextWordAction,
                           "ctrl A", DefaultEditorKit.selectAllAction,
                             "HOME", DefaultEditorKit.beginLineAction,
                              "END", DefaultEditorKit.endLineAction,
                       "shift HOME", DefaultEditorKit.selectionBeginLineAction,
                        "shift END", DefaultEditorKit.selectionEndLineAction,
                       "BACK_SPACE", DefaultEditorKit.deletePrevCharAction,
                 "shift BACK_SPACE", DefaultEditorKit.deletePrevCharAction,
                           "ctrl H", DefaultEditorKit.deletePrevCharAction,
                           "DELETE", DefaultEditorKit.deleteNextCharAction,
                      "ctrl DELETE", DefaultEditorKit.deleteNextWordAction,
                  "ctrl BACK_SPACE", DefaultEditorKit.deletePrevWordAction,
                            "RIGHT", DefaultEditorKit.forwardAction,
                             "LEFT", DefaultEditorKit.backwardAction,
                         "KP_RIGHT", DefaultEditorKit.forwardAction,
                          "KP_LEFT", DefaultEditorKit.backwardAction,
                            "ENTER", JTextField.notifyAction,
                  "ctrl BACK_SLASH", "unselect",
                   "control shift O", "toggle-componentOrientation",
                           "ESCAPE", "reset-field-edit",
                               "UP", "increment",
                            "KP_UP", "increment",
                             "DOWN", "decrement",
                          "KP_DOWN", "decrement",
              }));

        DEFAULT_VALUES.put("InternalFrame.icon",
                           SwingUtilities2.makeIcon(BasicLookAndFeel.class,
                                                    BasicLookAndFeel.class,
                                                    "icons/JavaCup16.png"));

        DEFAULT_VALUES.put("InternalFrame.windowBindings",
            new Object[] {
              "shift ESCAPE", "showSystemMenu",
                "ctrl SPACE", "showSystemMenu",
              "ESCAPE", "hideSystemMenu"});

        DEFAULT_VALUES.put("List.focusInputMap",
               new UIDefaults.LazyInputMap(new Object[] {
                           "ctrl C", "copy",
                           "ctrl V", "paste",
                           "ctrl X", "cut",
                             "COPY", "copy",
                            "PASTE", "paste",
                              "CUT", "cut",
                   "control INSERT", "copy",
                     "shift INSERT", "paste",
                     "shift DELETE", "cut",
                               "UP", "selectPreviousRow",
                            "KP_UP", "selectPreviousRow",
                         "shift UP", "selectPreviousRowExtendSelection",
                      "shift KP_UP", "selectPreviousRowExtendSelection",
                    "ctrl shift UP", "selectPreviousRowExtendSelection",
                 "ctrl shift KP_UP", "selectPreviousRowExtendSelection",
                          "ctrl UP", "selectPreviousRowChangeLead",
                       "ctrl KP_UP", "selectPreviousRowChangeLead",
                             "DOWN", "selectNextRow",
                          "KP_DOWN", "selectNextRow",
                       "shift DOWN", "selectNextRowExtendSelection",
                    "shift KP_DOWN", "selectNextRowExtendSelection",
                  "ctrl shift DOWN", "selectNextRowExtendSelection",
               "ctrl shift KP_DOWN", "selectNextRowExtendSelection",
                        "ctrl DOWN", "selectNextRowChangeLead",
                     "ctrl KP_DOWN", "selectNextRowChangeLead",
                             "LEFT", "selectPreviousColumn",
                          "KP_LEFT", "selectPreviousColumn",
                       "shift LEFT", "selectPreviousColumnExtendSelection",
                    "shift KP_LEFT", "selectPreviousColumnExtendSelection",
                  "ctrl shift LEFT", "selectPreviousColumnExtendSelection",
               "ctrl shift KP_LEFT", "selectPreviousColumnExtendSelection",
                        "ctrl LEFT", "selectPreviousColumnChangeLead",
                     "ctrl KP_LEFT", "selectPreviousColumnChangeLead",
                            "RIGHT", "selectNextColumn",
                         "KP_RIGHT", "selectNextColumn",
                      "shift RIGHT", "selectNextColumnExtendSelection",
                   "shift KP_RIGHT", "selectNextColumnExtendSelection",
                 "ctrl shift RIGHT", "selectNextColumnExtendSelection",
              "ctrl shift KP_RIGHT", "selectNextColumnExtendSelection",
                       "ctrl RIGHT", "selectNextColumnChangeLead",
                    "ctrl KP_RIGHT", "selectNextColumnChangeLead",
                             "HOME", "selectFirstRow",
                       "shift HOME", "selectFirstRowExtendSelection",
                  "ctrl shift HOME", "selectFirstRowExtendSelection",
                        "ctrl HOME", "selectFirstRowChangeLead",
                              "END", "selectLastRow",
                        "shift END", "selectLastRowExtendSelection",
                   "ctrl shift END", "selectLastRowExtendSelection",
                         "ctrl END", "selectLastRowChangeLead",
                          "PAGE_UP", "scrollUp",
                    "shift PAGE_UP", "scrollUpExtendSelection",
               "ctrl shift PAGE_UP", "scrollUpExtendSelection",
                     "ctrl PAGE_UP", "scrollUpChangeLead",
                        "PAGE_DOWN", "scrollDown",
                  "shift PAGE_DOWN", "scrollDownExtendSelection",
             "ctrl shift PAGE_DOWN", "scrollDownExtendSelection",
                   "ctrl PAGE_DOWN", "scrollDownChangeLead",
                           "ctrl A", "selectAll",
                       "ctrl SLASH", "selectAll",
                  "ctrl BACK_SLASH", "clearSelection",
                            "SPACE", "addToSelection",
                       "ctrl SPACE", "toggleAndAnchor",
                      "shift SPACE", "extendTo",
                 "ctrl shift SPACE", "moveSelectionTo"
               }));

        DEFAULT_VALUES.put("List.focusInputMap.RightToLeft",
               new UIDefaults.LazyInputMap(new Object[] {
                             "LEFT", "selectNextColumn",
                          "KP_LEFT", "selectNextColumn",
                       "shift LEFT", "selectNextColumnExtendSelection",
                    "shift KP_LEFT", "selectNextColumnExtendSelection",
                  "ctrl shift LEFT", "selectNextColumnExtendSelection",
               "ctrl shift KP_LEFT", "selectNextColumnExtendSelection",
                        "ctrl LEFT", "selectNextColumnChangeLead",
                     "ctrl KP_LEFT", "selectNextColumnChangeLead",
                            "RIGHT", "selectPreviousColumn",
                         "KP_RIGHT", "selectPreviousColumn",
                      "shift RIGHT", "selectPreviousColumnExtendSelection",
                   "shift KP_RIGHT", "selectPreviousColumnExtendSelection",
                 "ctrl shift RIGHT", "selectPreviousColumnExtendSelection",
              "ctrl shift KP_RIGHT", "selectPreviousColumnExtendSelection",
                       "ctrl RIGHT", "selectPreviousColumnChangeLead",
                    "ctrl KP_RIGHT", "selectPreviousColumnChangeLead",
               }));

        DEFAULT_VALUES.put("MenuBar.windowBindings",
                                new Object[] { "F10", "takeFocus" });

        DEFAULT_VALUES.put("OptionPane.windowBindings",
                 new Object[] { "ESCAPE", "close" });

        DEFAULT_VALUES.put("RootPane.defaultButtonWindowKeyBindings",
                 new Object[] {
                             "ENTER", "press",
                    "released ENTER", "release",
                        "ctrl ENTER", "press",
               "ctrl released ENTER", "release"
                 });

        DEFAULT_VALUES.put("RootPane.ancestorInputMap",
               new UIDefaults.LazyInputMap(new Object[] {
                    "shift F10", "postPopup"
               }));

        DEFAULT_VALUES.put("ScrollBar.anecstorInputMap",
               new UIDefaults.LazyInputMap(new Object[] {
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

        DEFAULT_VALUES.put("ScrollBar.ancestorInputMap.RightToLeft",
               new UIDefaults.LazyInputMap(new Object[] {
                       "RIGHT", "negativeUnitIncrement",
                    "KP_RIGHT", "negativeUnitIncrement",
                        "LEFT", "positiveUnitIncrement",
                     "KP_LEFT", "positiveUnitIncrement",
               }));

        DEFAULT_VALUES.put("ScrollPane.ancestorInputMap",
               new UIDefaults.LazyInputMap(new Object[] {
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
                    "ctrl PAGE_UP", "scrollLeft",
                  "ctrl PAGE_DOWN", "scrollRight",
                       "ctrl HOME", "scrollHome",
                        "ctrl END", "scrollEnd"
               }));
        DEFAULT_VALUES.put("ScrollPane.ancestorInputMap.RightToLeft",
               new UIDefaults.LazyInputMap(new Object[] {
                    "ctrl PAGE_UP", "scrollRight",
                  "ctrl PAGE_DOWN", "scrollLeft",
               }));

        DEFAULT_VALUES.put("SplitPane.ancestorInputMap",
               new UIDefaults.LazyInputMap(new Object[] {
                        "UP", "negativeIncrement",
                      "DOWN", "positiveIncrement",
                      "LEFT", "negativeIncrement",
                     "RIGHT", "positiveIncrement",
                     "KP_UP", "negativeIncrement",
                   "KP_DOWN", "positiveIncrement",
                   "KP_LEFT", "negativeIncrement",
                  "KP_RIGHT", "positiveIncrement",
                      "HOME", "selectMin",
                       "END", "selectMax",
                        "F8", "startResize",
                        "F6", "toggleFocus",
                  "ctrl TAB", "focusOutForward",
            "ctrl shift TAB", "focusOutBackward"
               }));

        DEFAULT_VALUES.put("Spinner.ancestorInputMap",
               new UIDefaults.LazyInputMap(new Object[] {
                          "UP", "increment",
                       "KP_UP", "increment",
                        "DOWN", "decrement",
                     "KP_DOWN", "decrement"
               }));

        DEFAULT_VALUES.put("Slider.focusInputMap",
               new UIDefaults.LazyInputMap(new Object[] {
                       "RIGHT", "positiveUnitIncrement",
                    "KP_RIGHT", "positiveUnitIncrement",
                        "DOWN", "negativeUnitIncrement",
                     "KP_DOWN", "negativeUnitIncrement",
                   "PAGE_DOWN", "negativeBlockIncrement",
              "ctrl PAGE_DOWN", "negativeBlockIncrement",
                        "LEFT", "negativeUnitIncrement",
                     "KP_LEFT", "negativeUnitIncrement",
                          "UP", "positiveUnitIncrement",
                       "KP_UP", "positiveUnitIncrement",
                     "PAGE_UP", "positiveBlockIncrement",
                "ctrl PAGE_UP", "positiveBlockIncrement",
                        "HOME", "minScroll",
                         "END", "maxScroll"
               }));

        DEFAULT_VALUES.put("Slider.focusInputMap.RightToLeft",
               new UIDefaults.LazyInputMap(new Object[] {
                       "RIGHT", "negativeUnitIncrement",
                    "KP_RIGHT", "negativeUnitIncrement",
                        "LEFT", "positiveUnitIncrement",
                     "KP_LEFT", "positiveUnitIncrement",
               }));

        DEFAULT_VALUES.put("TabbedPane.ancestorInputMap",
               new UIDefaults.LazyInputMap(new Object[] {
                   "ctrl PAGE_DOWN", "navigatePageDown",
                     "ctrl PAGE_UP", "navigatePageUp",
                          "ctrl UP", "requestFocus",
                       "ctrl KP_UP", "requestFocus",
               }));

        DEFAULT_VALUES.put("TabbedPane.focusInputMap",
              new UIDefaults.LazyInputMap(new Object[] {
                         "RIGHT", "navigateRight",
                      "KP_RIGHT", "navigateRight",
                          "LEFT", "navigateLeft",
                       "KP_LEFT", "navigateLeft",
                            "UP", "navigateUp",
                         "KP_UP", "navigateUp",
                          "DOWN", "navigateDown",
                       "KP_DOWN", "navigateDown",
                     "ctrl DOWN", "requestFocusForVisibleComponent",
                  "ctrl KP_DOWN", "requestFocusForVisibleComponent",
              }));

        DEFAULT_VALUES.put("Table.ancestorInputMap",
               new UIDefaults.LazyInputMap(new Object[] {
                               "ctrl C", "copy",
                               "ctrl V", "paste",
                               "ctrl X", "cut",
                                 "COPY", "copy",
                                "PASTE", "paste",
                                  "CUT", "cut",
                       "control INSERT", "copy",
                         "shift INSERT", "paste",
                         "shift DELETE", "cut",
                                "RIGHT", "selectNextColumn",
                             "KP_RIGHT", "selectNextColumn",
                          "shift RIGHT", "selectNextColumnExtendSelection",
                       "shift KP_RIGHT", "selectNextColumnExtendSelection",
                     "ctrl shift RIGHT", "selectNextColumnExtendSelection",
                  "ctrl shift KP_RIGHT", "selectNextColumnExtendSelection",
                           "ctrl RIGHT", "selectNextColumnChangeLead",
                        "ctrl KP_RIGHT", "selectNextColumnChangeLead",
                                 "LEFT", "selectPreviousColumn",
                              "KP_LEFT", "selectPreviousColumn",
                           "shift LEFT", "selectPreviousColumnExtendSelection",
                        "shift KP_LEFT", "selectPreviousColumnExtendSelection",
                      "ctrl shift LEFT", "selectPreviousColumnExtendSelection",
                   "ctrl shift KP_LEFT", "selectPreviousColumnExtendSelection",
                            "ctrl LEFT", "selectPreviousColumnChangeLead",
                         "ctrl KP_LEFT", "selectPreviousColumnChangeLead",
                                 "DOWN", "selectNextRow",
                              "KP_DOWN", "selectNextRow",
                           "shift DOWN", "selectNextRowExtendSelection",
                        "shift KP_DOWN", "selectNextRowExtendSelection",
                      "ctrl shift DOWN", "selectNextRowExtendSelection",
                   "ctrl shift KP_DOWN", "selectNextRowExtendSelection",
                            "ctrl DOWN", "selectNextRowChangeLead",
                         "ctrl KP_DOWN", "selectNextRowChangeLead",
                                   "UP", "selectPreviousRow",
                                "KP_UP", "selectPreviousRow",
                             "shift UP", "selectPreviousRowExtendSelection",
                          "shift KP_UP", "selectPreviousRowExtendSelection",
                        "ctrl shift UP", "selectPreviousRowExtendSelection",
                     "ctrl shift KP_UP", "selectPreviousRowExtendSelection",
                              "ctrl UP", "selectPreviousRowChangeLead",
                           "ctrl KP_UP", "selectPreviousRowChangeLead",
                                 "HOME", "selectFirstColumn",
                           "shift HOME", "selectFirstColumnExtendSelection",
                      "ctrl shift HOME", "selectFirstRowExtendSelection",
                            "ctrl HOME", "selectFirstRow",
                                  "END", "selectLastColumn",
                            "shift END", "selectLastColumnExtendSelection",
                       "ctrl shift END", "selectLastRowExtendSelection",
                             "ctrl END", "selectLastRow",
                              "PAGE_UP", "scrollUpChangeSelection",
                        "shift PAGE_UP", "scrollUpExtendSelection",
                   "ctrl shift PAGE_UP", "scrollLeftExtendSelection",
                         "ctrl PAGE_UP", "scrollLeftChangeSelection",
                            "PAGE_DOWN", "scrollDownChangeSelection",
                      "shift PAGE_DOWN", "scrollDownExtendSelection",
                 "ctrl shift PAGE_DOWN", "scrollRightExtendSelection",
                       "ctrl PAGE_DOWN", "scrollRightChangeSelection",
                                  "TAB", "selectNextColumnCell",
                            "shift TAB", "selectPreviousColumnCell",
                                "ENTER", "selectNextRowCell",
                          "shift ENTER", "selectPreviousRowCell",
                               "ctrl A", "selectAll",
                           "ctrl SLASH", "selectAll",
                      "ctrl BACK_SLASH", "clearSelection",
                               "ESCAPE", "cancel",
                                   "F2", "startEditing",
                                "SPACE", "addToSelection",
                           "ctrl SPACE", "toggleAndAnchor",
                          "shift SPACE", "extendTo",
                     "ctrl shift SPACE", "moveSelectionTo",
                                   "F8", "focusHeader"
               }));

       DEFAULT_VALUES.put("TableHeader.ancestorInputMap",
               new UIDefaults.LazyInputMap(new Object[] {
                                "SPACE", "toggleSortOrder",
                                 "LEFT", "selectColumnToLeft",
                              "KP_LEFT", "selectColumnToLeft",
                                "RIGHT", "selectColumnToRight",
                             "KP_RIGHT", "selectColumnToRight",
                             "alt LEFT", "moveColumnLeft",
                          "alt KP_LEFT", "moveColumnLeft",
                            "alt RIGHT", "moveColumnRight",
                         "alt KP_RIGHT", "moveColumnRight",
                       "alt shift LEFT", "resizeLeft",
                    "alt shift KP_LEFT", "resizeLeft",
                      "alt shift RIGHT", "resizeRight",
                   "alt shift KP_RIGHT", "resizeRight",
                               "ESCAPE", "focusTable",
               }));

        DEFAULT_VALUES.put("Tree.ancestorInputMap",
               new UIDefaults.LazyInputMap(new Object[] {
                     "ESCAPE", "cancel"
               }));
        DEFAULT_VALUES.put("Tree.focusInputMap",
               new UIDefaults.LazyInputMap(new Object[] {
                                    "ADD", "expand",
                               "SUBTRACT", "collapse",
                                 "ctrl C", "copy",
                                 "ctrl V", "paste",
                                 "ctrl X", "cut",
                                   "COPY", "copy",
                                  "PASTE", "paste",
                                    "CUT", "cut",
                         "control INSERT", "copy",
                           "shift INSERT", "paste",
                           "shift DELETE", "cut",
                                     "UP", "selectPrevious",
                                  "KP_UP", "selectPrevious",
                               "shift UP", "selectPreviousExtendSelection",
                            "shift KP_UP", "selectPreviousExtendSelection",
                          "ctrl shift UP", "selectPreviousExtendSelection",
                       "ctrl shift KP_UP", "selectPreviousExtendSelection",
                                "ctrl UP", "selectPreviousChangeLead",
                             "ctrl KP_UP", "selectPreviousChangeLead",
                                   "DOWN", "selectNext",
                                "KP_DOWN", "selectNext",
                             "shift DOWN", "selectNextExtendSelection",
                          "shift KP_DOWN", "selectNextExtendSelection",
                        "ctrl shift DOWN", "selectNextExtendSelection",
                     "ctrl shift KP_DOWN", "selectNextExtendSelection",
                              "ctrl DOWN", "selectNextChangeLead",
                           "ctrl KP_DOWN", "selectNextChangeLead",
                                  "RIGHT", "selectChild",
                               "KP_RIGHT", "selectChild",
                                   "LEFT", "selectParent",
                                "KP_LEFT", "selectParent",
                                "PAGE_UP", "scrollUpChangeSelection",
                          "shift PAGE_UP", "scrollUpExtendSelection",
                     "ctrl shift PAGE_UP", "scrollUpExtendSelection",
                           "ctrl PAGE_UP", "scrollUpChangeLead",
                              "PAGE_DOWN", "scrollDownChangeSelection",
                        "shift PAGE_DOWN", "scrollDownExtendSelection",
                   "ctrl shift PAGE_DOWN", "scrollDownExtendSelection",
                         "ctrl PAGE_DOWN", "scrollDownChangeLead",
                                   "HOME", "selectFirst",
                             "shift HOME", "selectFirstExtendSelection",
                        "ctrl shift HOME", "selectFirstExtendSelection",
                              "ctrl HOME", "selectFirstChangeLead",
                                    "END", "selectLast",
                              "shift END", "selectLastExtendSelection",
                         "ctrl shift END", "selectLastExtendSelection",
                               "ctrl END", "selectLastChangeLead",
                                     "F2", "startEditing",
                                 "ctrl A", "selectAll",
                             "ctrl SLASH", "selectAll",
                        "ctrl BACK_SLASH", "clearSelection",
                              "ctrl LEFT", "scrollLeft",
                           "ctrl KP_LEFT", "scrollLeft",
                             "ctrl RIGHT", "scrollRight",
                          "ctrl KP_RIGHT", "scrollRight",
                                  "SPACE", "addToSelection",
                             "ctrl SPACE", "toggleAndAnchor",
                            "shift SPACE", "extendTo",
                       "ctrl shift SPACE", "moveSelectionTo"
               }));
        DEFAULT_VALUES.put("Tree.focusInputMap.RightToLeft",
               new UIDefaults.LazyInputMap(new Object[] {
                                  "RIGHT", "selectParent",
                               "KP_RIGHT", "selectParent",
                                   "LEFT", "selectChild",
                                "KP_LEFT", "selectChild",
               }));
    }

    /**
     * Returns the default value for the specified property, or null if there
     * is no default for the specified value.
     */
    private static Object getDefaultValue(Object key) {
        synchronized(SynthStyle.class) {
            if (DEFAULT_VALUES == null) {
                DEFAULT_VALUES = new HashMap<Object, Object>();
                populateDefaultValues();
            }
            Object value = DEFAULT_VALUES.get(key);
            if (value instanceof UIDefaults.LazyValue) {
                value = ((UIDefaults.LazyValue)value).createValue(null);
                DEFAULT_VALUES.put(key, value);
            }
            return value;
        }
    }

    /**
     * Constructs a SynthStyle.
     */
    public SynthStyle() {
    }

    /**
     * Returns the <code>SynthGraphicUtils</code> for the specified context.
     *
     * @param context SynthContext identifying requester
     * @return SynthGraphicsUtils
     */
    public SynthGraphicsUtils getGraphicsUtils(SynthContext context) {
        return SYNTH_GRAPHICS;
    }

    /**
     * Returns the color for the specified state. This gives precedence to
     * foreground and background of the <code>JComponent</code>. If the
     * <code>Color</code> from the <code>JComponent</code> is not appropriate,
     * or not used, this will invoke <code>getColorForState</code>. Subclasses
     * should generally not have to override this, instead override
     * {@link #getColorForState}.
     *
     * @param context SynthContext identifying requester
     * @param type Type of color being requested.
     * @return Color
     */
    public Color getColor(SynthContext context, ColorType type) {
        JComponent c = context.getComponent();
        Region id = context.getRegion();

        if ((context.getComponentState() & SynthConstants.DISABLED) != 0) {
            //This component is disabled, so return the disabled color.
            //In some cases this means ignoring the color specified by the
            //developer on the component. In other cases it means using a
            //specified disabledTextColor, such as on JTextComponents.
            //For example, JLabel doesn't specify a disabled color that the
            //developer can set, yet it should have a disabled color to the
            //text when the label is disabled. This code allows for that.
            if (c instanceof JTextComponent) {
                JTextComponent txt = (JTextComponent)c;
                Color disabledColor = txt.getDisabledTextColor();
                if (disabledColor == null || disabledColor instanceof UIResource) {
                    return getColorForState(context, type);
                }
            } else if ((c instanceof JLabel || c instanceof JMenuItem) &&
                            (type == ColorType.FOREGROUND ||
                             type == ColorType.TEXT_FOREGROUND)) {
                return getColorForState(context, type);
            }
        }

        // If the developer has specified a color, prefer it. Otherwise, get
        // the color for the state.
        Color color = null;
        if (!id.isSubregion()) {
            if (type == ColorType.BACKGROUND) {
                color = c.getBackground();
            }
            else if (type == ColorType.FOREGROUND) {
                color = c.getForeground();
            }
            else if (type == ColorType.TEXT_FOREGROUND) {
                color = c.getForeground();
            }
        }

        if (color == null || color instanceof UIResource) {
            // Then use what we've locally defined
            color = getColorForState(context, type);
        }

        if (color == null) {
            // No color, fallback to that of the widget.
            if (type == ColorType.BACKGROUND ||
                        type == ColorType.TEXT_BACKGROUND) {
                return c.getBackground();
            }
            else if (type == ColorType.FOREGROUND ||
                     type == ColorType.TEXT_FOREGROUND) {
                return c.getForeground();
            }
        }
        return color;
    }

    /**
     * Returns the color for the specified state. This should NOT call any
     * methods on the <code>JComponent</code>.
     *
     * @param context SynthContext identifying requester
     * @param type Type of color being requested.
     * @return Color to render with
     */
    protected abstract Color getColorForState(SynthContext context,
                                              ColorType type);

    /**
     * Returns the Font for the specified state. This redirects to the
     * <code>JComponent</code> from the <code>context</code> as necessary.
     * If this does not redirect
     * to the JComponent {@link #getFontForState} is invoked.
     *
     * @param context SynthContext identifying requester
     * @return Font to render with
     */
    public Font getFont(SynthContext context) {
        JComponent c = context.getComponent();
        if (context.getComponentState() == SynthConstants.ENABLED) {
            return c.getFont();
        }
        Font cFont = c.getFont();
        if (cFont != null && !(cFont instanceof UIResource)) {
            return cFont;
        }
        return getFontForState(context);
    }

    /**
     * Returns the font for the specified state. This should NOT call any
     * method on the <code>JComponent</code>.
     *
     * @param context SynthContext identifying requester
     * @return Font to render with
     */
    protected abstract Font getFontForState(SynthContext context);

    /**
     * Returns the Insets that are used to calculate sizing information.
     *
     * @param context SynthContext identifying requester
     * @param insets Insets to place return value in.
     * @return Sizing Insets.
     */
    public Insets getInsets(SynthContext context, Insets insets) {
        if (insets == null) {
            insets = new Insets(0, 0, 0, 0);
        }
        insets.top = insets.bottom = insets.left = insets.right = 0;
        return insets;
    }

    /**
     * Returns the <code>SynthPainter</code> that will be used for painting.
     * This may return null.
     *
     * @param context SynthContext identifying requester
     * @return SynthPainter to use
     */
    public SynthPainter getPainter(SynthContext context) {
        return null;
    }

    /**
     * Returns true if the region is opaque.
     *
     * @param context SynthContext identifying requester
     * @return true if region is opaque.
     */
    public boolean isOpaque(SynthContext context) {
        return true;
    }

    /**
     * Getter for a region specific style property.
     *
     * @param context SynthContext identifying requester
     * @param key Property being requested.
     * @return Value of the named property
     */
    public Object get(SynthContext context, Object key) {
        return getDefaultValue(key);
    }

    void installDefaults(SynthContext context, SynthUI ui) {
        // Special case the Border as this will likely change when the LAF
        // can have more control over this.
        if (!context.isSubregion()) {
            JComponent c = context.getComponent();
            Border border = c.getBorder();

            if (border == null || border instanceof UIResource) {
                c.setBorder(new SynthBorder(ui, getInsets(context, null)));
            }
        }
        installDefaults(context);
    }

    /**
     * Installs the necessary state from this Style on the
     * <code>JComponent</code> from <code>context</code>.
     *
     * @param context SynthContext identifying component to install properties
     *        to.
     */
    public void installDefaults(SynthContext context) {
        if (!context.isSubregion()) {
            JComponent c = context.getComponent();
            Region region = context.getRegion();
            Font font = c.getFont();

            if (font == null || (font instanceof UIResource)) {
                c.setFont(getFontForState(context));
            }
            Color background = c.getBackground();
            if (background == null || (background instanceof UIResource)) {
                c.setBackground(getColorForState(context,
                                                 ColorType.BACKGROUND));
            }
            Color foreground = c.getForeground();
            if (foreground == null || (foreground instanceof UIResource)) {
                c.setForeground(getColorForState(context,
                         ColorType.FOREGROUND));
            }
            LookAndFeel.installProperty(c, "opaque", Boolean.valueOf(isOpaque(context)));
        }
    }

    /**
     * Uninstalls any state that this style installed on
     * the <code>JComponent</code> from <code>context</code>.
     * <p>
     * Styles should NOT depend upon this being called, in certain cases
     * it may never be called.
     *
     * @param context SynthContext identifying component to install properties
     *        to.
     */
    public void uninstallDefaults(SynthContext context) {
        if (!context.isSubregion()) {
            // NOTE: because getForeground, getBackground and getFont will look
            // at the parent Container, if we set them to null it may
            // mean we they return a non-null and non-UIResource value
            // preventing install from correctly settings its colors/font. For
            // this reason we do not uninstall the fg/bg/font.

            JComponent c = context.getComponent();
            Border border = c.getBorder();

            if (border instanceof UIResource) {
                c.setBorder(null);
            }
        }
    }

    /**
     * Convenience method to get a specific style property whose value is
     * a <code>Number</code>. If the value is a <code>Number</code>,
     * <code>intValue</code> is returned, otherwise <code>defaultValue</code>
     * is returned.
     *
     * @param context SynthContext identifying requester
     * @param key Property being requested.
     * @param defaultValue Value to return if the property has not been
     *        specified, or is not a Number
     * @return Value of the named property
     */
    public int getInt(SynthContext context, Object key, int defaultValue) {
        Object value = get(context, key);

        if (value instanceof Number) {
            return ((Number)value).intValue();
        }
        return defaultValue;
    }

    /**
     * Convenience method to get a specific style property whose value is
     * an Boolean.
     *
     * @param context SynthContext identifying requester
     * @param key Property being requested.
     * @param defaultValue Value to return if the property has not been
     *        specified, or is not a Boolean
     * @return Value of the named property
     */
    public boolean getBoolean(SynthContext context, Object key,
                              boolean defaultValue) {
        Object value = get(context, key);

        if (value instanceof Boolean) {
            return ((Boolean)value).booleanValue();
        }
        return defaultValue;
    }

    /**
     * Convenience method to get a specific style property whose value is
     * an Icon.
     *
     * @param context SynthContext identifying requester
     * @param key Property being requested.
     * @return Value of the named property, or null if not specified
     */
    public Icon getIcon(SynthContext context, Object key) {
        Object value = get(context, key);

        if (value instanceof Icon) {
            return (Icon)value;
        }
        return null;
    }

    /**
     * Convenience method to get a specific style property whose value is
     * a String.
     *
     * @param context SynthContext identifying requester
     * @param key Property being requested.
     * @param defaultValue Value to return if the property has not been
     *        specified, or is not a String
     * @return Value of the named property
     */
    public String getString(SynthContext context, Object key,
                              String defaultValue) {
        Object value = get(context, key);

        if (value instanceof String) {
            return (String)value;
        }
        return defaultValue;
    }
}
