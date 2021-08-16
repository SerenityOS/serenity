/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt.X11;

import java.awt.Color;
import java.awt.Font;
import java.awt.SystemColor;

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.border.*;
import javax.swing.text.DefaultEditorKit;

import javax.swing.plaf.basic.BasicBorders;
import com.sun.java.swing.plaf.motif.*;
import sun.awt.X11.XComponentPeer;

@SuppressWarnings({"serial", "removal"}) // JDK-implementation class
class XAWTLookAndFeel extends MotifLookAndFeel {

    /**
     * Load the SystemColors into the defaults table.  The keys
     * for SystemColor defaults are the same as the names of
     * the public fields in SystemColor.  If the table is being
     * created on a native Motif platform we use the SystemColor
     * values, otherwise we create color objects whose values match
     * the default CDE/Motif colors.
     */
    protected void initSystemColorDefaults(UIDefaults table) {
        String[] defaultSystemColors = {
            "desktop", "#005C5C", /* Color of the desktop background */
            "activeCaption", "#000080", /* Color for captions (title bars) when they are active. */
            "activeCaptionText", "#FFFFFF", /* Text color for text in captions (title bars). */
            "activeCaptionBorder", "#B24D7A", /* Border color for caption (title bar) window borders. */
            "inactiveCaption", "#AEB2C3", /* Color for captions (title bars) when not active. */
            "inactiveCaptionText", "#000000", /* Text color for text in inactive captions (title bars). */
            "inactiveCaptionBorder", "#AEB2C3", /* Border color for inactive caption (title bar) window borders. */
            "window", "#AEB2C3", /* Default color for the interior of windows */
            "windowBorder", "#AEB2C3", /* ??? */
            "windowText", "#000000", /* ??? */
            "menu", "#AEB2C3", /* ??? */
            "menuText", "#000000", /* ??? */
            "text", "#FFF7E9", /* Text background color */
            "textText", "#000000", /* Text foreground color */
            "textHighlight", "#000000", /* Text background color when selected */
            "textHighlightText", "#FFF7E9", /* Text color when selected */
            "textInactiveText", "#808080", /* Text color when disabled */
            "control", "#AEB2C3", /* Default color for controls (buttons, sliders, etc) */
            "controlText", "#000000", /* Default color for text in controls */
            "controlHighlight", "#DCDEE5", /* Highlight color for controls */
            "controlLtHighlight", "#DCDEE5", /* Light highlight color for controls */
            "controlShadow", "#63656F", /* Shadow color for controls */
            "controlLightShadow", "#9397A5", /* Shadow color for controls */
            "controlDkShadow", "#000000", /* Dark shadow color for controls */
            "scrollbar", "#AEB2C3", /* Scrollbar ??? color. PENDING(jeff) foreground? background? ?*/
            "info", "#FFF7E9", /* ??? */
            "infoText", "#000000"  /* ??? */
        };

        loadSystemColors(table, defaultSystemColors, true);
    }

    protected void initComponentDefaults(UIDefaults table) {
        super.initComponentDefaults(table);

        FontUIResource dialogPlain12 = new FontUIResource(Font.DIALOG,
                                                          Font.PLAIN, 12);
        FontUIResource sansSerifPlain12 = new FontUIResource(Font.SANS_SERIF,
                                                             Font.PLAIN, 12);
        FontUIResource monospacedPlain12 = new FontUIResource(Font.MONOSPACED,
                                                              Font.PLAIN, 12);
        ColorUIResource red = new ColorUIResource(Color.red);
        ColorUIResource black = new ColorUIResource(Color.black);
        ColorUIResource white = new ColorUIResource(Color.white);
        ColorUIResource lightGray = new ColorUIResource(Color.lightGray);
        ColorUIResource controlDarker =  new ColorUIResource(SystemColor.controlDkShadow);

        Color back = table.getColor("control");
        Color[] colors  = XComponentPeer.getSystemColors();
        Color scrollBarBackground = colors[XComponentPeer.BACKGROUND_COLOR];
        Color trackColor = new Color(MotifColorUtilities.calculateSelectFromBackground(scrollBarBackground.getRed(), scrollBarBackground.getGreen(), scrollBarBackground.getBlue()));
        Border loweredBevelBorder = new MotifBorders.BevelBorder(false,
                                                                 table.getColor("controlShadow"),
                                                                 table.getColor("controlLtHighlight"));

        Border raisedBevelBorder = new MotifBorders.BevelBorder(true,
                                                                table.getColor("controlShadow"),
                                                                table.getColor("controlLtHighlight"));

        Border marginBorder = new BasicBorders.MarginBorder();

        Border focusBorder = new MotifBorders.FocusBorder(
            table.getColor("control"),
            table.getColor("activeCaptionBorder"));


        Border focusBevelBorder = new BorderUIResource.CompoundBorderUIResource(
            focusBorder,
            loweredBevelBorder);

        Border textFieldBorder = new BorderUIResource.CompoundBorderUIResource(
            focusBevelBorder,
            marginBorder);

        // *** Text

        Object fieldInputMap = new UIDefaults.LazyInputMap(new Object[] {
            "COPY", DefaultEditorKit.copyAction,
            "PASTE", DefaultEditorKit.pasteAction,
            "CUT", DefaultEditorKit.cutAction,
            "control C", DefaultEditorKit.copyAction,
            "control V", DefaultEditorKit.pasteAction,
            "control X", DefaultEditorKit.cutAction,
            "control INSERT", DefaultEditorKit.copyAction,
            "shift INSERT", DefaultEditorKit.pasteAction,
            "shift DELETE", DefaultEditorKit.cutAction,
            "control F", DefaultEditorKit.forwardAction,
            "control B", DefaultEditorKit.backwardAction,
            "control D", DefaultEditorKit.deleteNextCharAction,
            "typed \010", DefaultEditorKit.deletePrevCharAction,
            "DELETE", DefaultEditorKit.deleteNextCharAction,
            "RIGHT", DefaultEditorKit.forwardAction,
            "LEFT", DefaultEditorKit.backwardAction,
            "KP_RIGHT", DefaultEditorKit.forwardAction,
            "KP_LEFT", DefaultEditorKit.backwardAction,
            "shift LEFT", DefaultEditorKit.selectionBackwardAction,
            "shift RIGHT", DefaultEditorKit.selectionForwardAction,
            "control LEFT", DefaultEditorKit.previousWordAction,
            "control RIGHT", DefaultEditorKit.nextWordAction,
            "control shift LEFT", DefaultEditorKit.selectionPreviousWordAction,
            "control shift RIGHT", DefaultEditorKit.selectionNextWordAction,
            "control SLASH", DefaultEditorKit.selectAllAction,
            "HOME", DefaultEditorKit.beginLineAction,
            "END", DefaultEditorKit.endLineAction,
            "shift HOME", DefaultEditorKit.selectionBeginLineAction,
            "shift END", DefaultEditorKit.selectionEndLineAction,
            "control BACK_SLASH", "unselect",
            "ENTER", JTextField.notifyAction,
            "control shift O", "toggle-componentOrientation"
        });

        Object passwordInputMap = new UIDefaults.LazyInputMap(new Object[] {
            "COPY", DefaultEditorKit.copyAction,
            "PASTE", DefaultEditorKit.pasteAction,
            "CUT", DefaultEditorKit.cutAction,
            "control C", DefaultEditorKit.copyAction,
            "control V", DefaultEditorKit.pasteAction,
            "control X", DefaultEditorKit.cutAction,
            "control INSERT", DefaultEditorKit.copyAction,
            "shift INSERT", DefaultEditorKit.pasteAction,
            "shift DELETE", DefaultEditorKit.cutAction,
            "control F", DefaultEditorKit.forwardAction,
            "control B", DefaultEditorKit.backwardAction,
            "control D", DefaultEditorKit.deleteNextCharAction,
            "typed \010", DefaultEditorKit.deletePrevCharAction,
            "DELETE", DefaultEditorKit.deleteNextCharAction,
            "RIGHT", DefaultEditorKit.forwardAction,
            "LEFT", DefaultEditorKit.backwardAction,
            "KP_RIGHT", DefaultEditorKit.forwardAction,
            "KP_LEFT", DefaultEditorKit.backwardAction,
            "shift LEFT", DefaultEditorKit.selectionBackwardAction,
            "shift RIGHT", DefaultEditorKit.selectionForwardAction,
            "control LEFT", DefaultEditorKit.beginLineAction,
            "control RIGHT", DefaultEditorKit.endLineAction,
            "control shift LEFT", DefaultEditorKit.selectionBeginLineAction,
            "control shift RIGHT", DefaultEditorKit.selectionEndLineAction,
            "control SLASH", DefaultEditorKit.selectAllAction,
            "HOME", DefaultEditorKit.beginLineAction,
            "END", DefaultEditorKit.endLineAction,
            "shift HOME", DefaultEditorKit.selectionBeginLineAction,
            "shift END", DefaultEditorKit.selectionEndLineAction,
            "control BACK_SLASH", "unselect",
            "ENTER", JTextField.notifyAction,
            "control shift O", "toggle-componentOrientation"
        });

        Object multilineInputMap = new UIDefaults.LazyInputMap(new Object[] {
            "COPY", DefaultEditorKit.copyAction,
            "PASTE", DefaultEditorKit.pasteAction,
            "CUT", DefaultEditorKit.cutAction,
            "control C", DefaultEditorKit.copyAction,
            "control V", DefaultEditorKit.pasteAction,
            "control X", DefaultEditorKit.cutAction,
            "control INSERT", DefaultEditorKit.copyAction,
            "shift INSERT", DefaultEditorKit.pasteAction,
            "shift DELETE", DefaultEditorKit.cutAction,
            "control F", DefaultEditorKit.forwardAction,
            "control B", DefaultEditorKit.backwardAction,
            "control D", DefaultEditorKit.deleteNextCharAction,
            "typed \010", DefaultEditorKit.deletePrevCharAction,
            "DELETE", DefaultEditorKit.deleteNextCharAction,
            "RIGHT", DefaultEditorKit.forwardAction,
            "LEFT", DefaultEditorKit.backwardAction,
            "KP_RIGHT", DefaultEditorKit.forwardAction,
            "KP_LEFT", DefaultEditorKit.backwardAction,
            "shift LEFT", DefaultEditorKit.selectionBackwardAction,
            "shift RIGHT", DefaultEditorKit.selectionForwardAction,
            "control LEFT", DefaultEditorKit.previousWordAction,
            "control RIGHT", DefaultEditorKit.nextWordAction,
            "control shift LEFT", DefaultEditorKit.selectionPreviousWordAction,
            "control shift RIGHT", DefaultEditorKit.selectionNextWordAction,
            "control SLASH", DefaultEditorKit.selectAllAction,
            "HOME", DefaultEditorKit.beginLineAction,
            "END", DefaultEditorKit.endLineAction,
            "shift HOME", DefaultEditorKit.selectionBeginLineAction,
            "shift END", DefaultEditorKit.selectionEndLineAction,

            "control N", DefaultEditorKit.downAction,
            "control P", DefaultEditorKit.upAction,
            "UP", DefaultEditorKit.upAction,
            "DOWN", DefaultEditorKit.downAction,
            "KP_UP", DefaultEditorKit.upAction,
            "KP_DOWN", DefaultEditorKit.downAction,
            "PAGE_UP", DefaultEditorKit.pageUpAction,
            "PAGE_DOWN", DefaultEditorKit.pageDownAction,
            "shift PAGE_UP", "selection-page-up",
            "shift PAGE_DOWN", "selection-page-down",
            "ctrl shift PAGE_UP", "selection-page-left",
            "ctrl shift PAGE_DOWN", "selection-page-right",
            "shift UP", DefaultEditorKit.selectionUpAction,
            "shift DOWN", DefaultEditorKit.selectionDownAction,
            "shift KP_UP", DefaultEditorKit.selectionUpAction,
            "shift KP_DOWN", DefaultEditorKit.selectionDownAction,
            "ENTER", DefaultEditorKit.insertBreakAction,
            "TAB", DefaultEditorKit.insertTabAction,
            "control BACK_SLASH", "unselect",
            "control HOME", DefaultEditorKit.beginAction,
            "control END", DefaultEditorKit.endAction,
            "control shift HOME", DefaultEditorKit.selectionBeginAction,
            "control shift END", DefaultEditorKit.selectionEndAction,
            "control T", "next-link-action",
            "control shift T", "previous-link-action",
            "control SPACE", "activate-link-action",
            "control shift O", "toggle-componentOrientation"
        });

        Object sliderFocusInsets = new InsetsUIResource( 0, 0, 0, 0 );

        Object[] defaults = {

            "ScrollBar.background", scrollBarBackground,
            "ScrollBar.foreground", table.get("control"),
            "ScrollBar.track", trackColor,
            "ScrollBar.trackHighlight", trackColor,
            "ScrollBar.thumb", scrollBarBackground,
            "ScrollBar.thumbHighlight", table.get("controlHighlight") ,
            "ScrollBar.thumbDarkShadow", table.get("controlDkShadow"),
            "ScrollBar.thumbShadow", table.get("controlShadow"),
            "ScrollBar.border", loweredBevelBorder,
            "ScrollBar.allowsAbsolutePositioning", Boolean.TRUE,
            "ScrollBar.defaultWidth", Integer.valueOf(17),
            "ScrollBar.focusInputMap",
            new UIDefaults.LazyInputMap(new Object[] {
                "RIGHT", "positiveUnitIncrement",
                "KP_RIGHT", "positiveUnitIncrement",
                "DOWN", "positiveUnitIncrement",
                "KP_DOWN", "positiveUnitIncrement",
                "PAGE_DOWN", "positiveBlockIncrement",
                "ctrl PAGE_DOWN", "positiveBlockIncrement",
                "LEFT", "negativeUnitIncrement",
                "KP_LEFT", "negativeUnitIncrement",
                "UP", "negativeUnitIncrement",
                "KP_UP", "negativeUnitIncrement",
                "PAGE_UP", "negativeBlockIncrement",
                "ctrl PAGE_UP", "negativeBlockIncrement",
                "HOME", "minScroll",
                "END", "maxScroll"
            }),

            "ScrollPane.font", dialogPlain12,
            "ScrollPane.background", scrollBarBackground,
            "ScrollPane.foreground", table.get("controlText"),
            "ScrollPane.border", null,
            "ScrollPane.viewportBorder", loweredBevelBorder,
            "ScrollPane.ancestorInputMap",
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
            }),

            "FormattedTextField.focusInputMap",
            new UIDefaults.LazyInputMap(new Object[] {
                "ctrl C", DefaultEditorKit.copyAction,
                "ctrl V", DefaultEditorKit.pasteAction,
                "ctrl X", DefaultEditorKit.cutAction,
                "COPY", DefaultEditorKit.copyAction,
                "PASTE", DefaultEditorKit.pasteAction,
                "CUT", DefaultEditorKit.cutAction,
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
                "typed \010", DefaultEditorKit.deletePrevCharAction,
                "DELETE", DefaultEditorKit.deleteNextCharAction,
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
            }),

            "TextField.caretForeground", black,
            "TextField.caretBlinkRate", Integer.valueOf(500),
            "TextField.inactiveForeground", table.get("textInactiveText"),
            "TextField.selectionBackground", table.get("textHighlight"),
            "TextField.selectionForeground", table.get("textHighlightText"),
            "TextField.background", table.get("window"),
            "TextField.foreground", table.get("textText"),
            "TextField.font", sansSerifPlain12,
            "TextField.border", textFieldBorder,
            "TextField.focusInputMap", fieldInputMap,

            "PasswordField.caretForeground", black,
            "PasswordField.caretBlinkRate", Integer.valueOf(500),
            "PasswordField.inactiveForeground", table.get("textInactiveText"),
            "PasswordField.selectionBackground", table.get("textHighlight"),
            "PasswordField.selectionForeground", table.get("textHighlightText"),
            "PasswordField.background", table.get("window"),
            "PasswordField.foreground", table.get("textText"),
            "PasswordField.font", sansSerifPlain12,
            "PasswordField.border", textFieldBorder,
            "PasswordField.focusInputMap", passwordInputMap,

            "TextArea.caretForeground", black,
            "TextArea.caretBlinkRate", Integer.valueOf(500),
            "TextArea.inactiveForeground", table.get("textInactiveText"),
            "TextArea.selectionBackground", table.get("textHighlight"),
            "TextArea.selectionForeground", table.get("textHighlightText"),
            "TextArea.background", table.get("window"),
            "TextArea.foreground", table.get("textText"),
            "TextArea.font", monospacedPlain12,
            "TextArea.border", marginBorder,
            "TextArea.focusInputMap", multilineInputMap
        };

        table.putDefaults(defaults);
    }
}
