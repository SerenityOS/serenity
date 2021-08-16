/*
 * Copyright (c) 2005, 2014, Oracle and/or its affiliates. All rights reserved.
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

/*
 * <p>These classes are designed to be used while the
 * corresponding <code>LookAndFeel</code> class has been installed
 * (<code>UIManager.setLookAndFeel(new <i>XXX</i>LookAndFeel())</code>).
 * Using them while a different <code>LookAndFeel</code> is installed
 * may produce unexpected results, including exceptions.
 * Additionally, changing the <code>LookAndFeel</code>
 * maintained by the <code>UIManager</code> without updating the
 * corresponding <code>ComponentUI</code> of any
 * <code>JComponent</code>s may also produce unexpected results,
 * such as the wrong colors showing up, and is generally not
 * encouraged.
 *
 */

package com.sun.java.swing.plaf.windows;

import java.awt.*;
import java.util.*;

import javax.swing.*;

import sun.awt.windows.ThemeReader;

/**
 * Implements Windows Parts and their States and Properties for the Windows Look and Feel.
 *
 * See http://msdn.microsoft.com/library/default.asp?url=/library/en-us/shellcc/platform/commctls/userex/topics/partsandstates.asp
 * See tmschema.h (or vssym32.h & vsstyle.h for MS Vista)
 *
 * @author Leif Samuelsson
 */
class TMSchema {

    /**
     * An enumeration of the various Windows controls (also known as
     * components, or top-level parts)
     */
    public static enum Control {
        BUTTON,
        COMBOBOX,
        EDIT,
        HEADER,
        LISTBOX,
        LISTVIEW,
        MENU,
        PROGRESS,
        REBAR,
        SCROLLBAR,
        SPIN,
        TAB,
        TOOLBAR,
        TRACKBAR,
        TREEVIEW,
        WINDOW
    }


    /**
     * An enumeration of the Windows compoent parts
     */
    public static enum Part {
        MENU (Control.MENU, 0), // Special case, not in native
        MP_BARBACKGROUND   (Control.MENU, 7),
        MP_BARITEM         (Control.MENU, 8),
        MP_POPUPBACKGROUND (Control.MENU, 9),
        MP_POPUPBORDERS    (Control.MENU, 10),
        MP_POPUPCHECK      (Control.MENU, 11),
        MP_POPUPCHECKBACKGROUND (Control.MENU, 12),
        MP_POPUPGUTTER     (Control.MENU, 13),
        MP_POPUPITEM       (Control.MENU, 14),
        MP_POPUPSEPARATOR  (Control.MENU, 15),
        MP_POPUPSUBMENU    (Control.MENU, 16),

        BP_PUSHBUTTON (Control.BUTTON, 1),
        BP_RADIOBUTTON(Control.BUTTON, 2),
        BP_CHECKBOX   (Control.BUTTON, 3),
        BP_GROUPBOX   (Control.BUTTON, 4),

        CP_COMBOBOX      (Control.COMBOBOX, 0),
        CP_DROPDOWNBUTTON(Control.COMBOBOX, 1),
        CP_BACKGROUND    (Control.COMBOBOX, 2),
        CP_TRANSPARENTBACKGROUND (Control.COMBOBOX, 3),
        CP_BORDER                (Control.COMBOBOX, 4),
        CP_READONLY              (Control.COMBOBOX, 5),
        CP_DROPDOWNBUTTONRIGHT   (Control.COMBOBOX, 6),
        CP_DROPDOWNBUTTONLEFT    (Control.COMBOBOX, 7),
        CP_CUEBANNER             (Control.COMBOBOX, 8),


        EP_EDIT    (Control.EDIT, 0),
        EP_EDITTEXT(Control.EDIT, 1),

        HP_HEADERITEM(Control.HEADER,      1),
        HP_HEADERSORTARROW(Control.HEADER, 4),

        LBP_LISTBOX(Control.LISTBOX, 0),

        LBCP_BORDER_HSCROLL  (Control.LISTBOX, 1),
        LBCP_BORDER_HVSCROLL (Control.LISTBOX, 2),
        LBCP_BORDER_NOSCROLL (Control.LISTBOX, 3),
        LBCP_BORDER_VSCROLL  (Control.LISTBOX, 4),
        LBCP_ITEM            (Control.LISTBOX, 5),

        LVP_LISTVIEW(Control.LISTVIEW, 0),

        PP_PROGRESS (Control.PROGRESS, 0),
        PP_BAR      (Control.PROGRESS, 1),
        PP_BARVERT  (Control.PROGRESS, 2),
        PP_CHUNK    (Control.PROGRESS, 3),
        PP_CHUNKVERT(Control.PROGRESS, 4),

        RP_GRIPPER    (Control.REBAR, 1),
        RP_GRIPPERVERT(Control.REBAR, 2),

        SBP_SCROLLBAR     (Control.SCROLLBAR,  0),
        SBP_ARROWBTN      (Control.SCROLLBAR,  1),
        SBP_THUMBBTNHORZ  (Control.SCROLLBAR,  2),
        SBP_THUMBBTNVERT  (Control.SCROLLBAR,  3),
        SBP_LOWERTRACKHORZ(Control.SCROLLBAR,  4),
        SBP_UPPERTRACKHORZ(Control.SCROLLBAR,  5),
        SBP_LOWERTRACKVERT(Control.SCROLLBAR,  6),
        SBP_UPPERTRACKVERT(Control.SCROLLBAR,  7),
        SBP_GRIPPERHORZ   (Control.SCROLLBAR,  8),
        SBP_GRIPPERVERT   (Control.SCROLLBAR,  9),
        SBP_SIZEBOX       (Control.SCROLLBAR, 10),

        SPNP_UP  (Control.SPIN, 1),
        SPNP_DOWN(Control.SPIN, 2),

        TABP_TABITEM         (Control.TAB, 1),
        TABP_TABITEMLEFTEDGE (Control.TAB, 2),
        TABP_TABITEMRIGHTEDGE(Control.TAB, 3),
        TABP_PANE            (Control.TAB, 9),

        TP_TOOLBAR        (Control.TOOLBAR, 0),
        TP_BUTTON         (Control.TOOLBAR, 1),
        TP_SEPARATOR      (Control.TOOLBAR, 5),
        TP_SEPARATORVERT  (Control.TOOLBAR, 6),

        TKP_TRACK      (Control.TRACKBAR,  1),
        TKP_TRACKVERT  (Control.TRACKBAR,  2),
        TKP_THUMB      (Control.TRACKBAR,  3),
        TKP_THUMBBOTTOM(Control.TRACKBAR,  4),
        TKP_THUMBTOP   (Control.TRACKBAR,  5),
        TKP_THUMBVERT  (Control.TRACKBAR,  6),
        TKP_THUMBLEFT  (Control.TRACKBAR,  7),
        TKP_THUMBRIGHT (Control.TRACKBAR,  8),
        TKP_TICS       (Control.TRACKBAR,  9),
        TKP_TICSVERT   (Control.TRACKBAR, 10),

        TVP_TREEVIEW(Control.TREEVIEW, 0),
        TVP_GLYPH   (Control.TREEVIEW, 2),

        WP_WINDOW          (Control.WINDOW,  0),
        WP_CAPTION         (Control.WINDOW,  1),
        WP_MINCAPTION      (Control.WINDOW,  3),
        WP_MAXCAPTION      (Control.WINDOW,  5),
        WP_FRAMELEFT       (Control.WINDOW,  7),
        WP_FRAMERIGHT      (Control.WINDOW,  8),
        WP_FRAMEBOTTOM     (Control.WINDOW,  9),
        WP_SYSBUTTON       (Control.WINDOW, 13),
        WP_MDISYSBUTTON    (Control.WINDOW, 14),
        WP_MINBUTTON       (Control.WINDOW, 15),
        WP_MDIMINBUTTON    (Control.WINDOW, 16),
        WP_MAXBUTTON       (Control.WINDOW, 17),
        WP_CLOSEBUTTON     (Control.WINDOW, 18),
        WP_MDICLOSEBUTTON  (Control.WINDOW, 20),
        WP_RESTOREBUTTON   (Control.WINDOW, 21),
        WP_MDIRESTOREBUTTON(Control.WINDOW, 22);

        private final Control control;
        private final int value;

        private Part(Control control, int value) {
            this.control = control;
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public String getControlName(Component component) {
            String str = "";
            if (component instanceof JComponent) {
                JComponent c = (JComponent)component;
                String subAppName = (String)c.getClientProperty("XPStyle.subAppName");
                if (subAppName != null) {
                    str = subAppName + "::";
                }
            }
            return str + control.toString();
        }

        public String toString() {
            return control.toString()+"."+name();
        }
    }


    /**
     * An enumeration of the possible component states
     */
    public static enum State {
        ACTIVE,
        ASSIST,
        BITMAP,
        CHECKED,
        CHECKEDDISABLED,
        CHECKEDHOT,
        CHECKEDNORMAL,
        CHECKEDPRESSED,
        CHECKMARKNORMAL,
        CHECKMARKDISABLED,
        BULLETNORMAL,
        BULLETDISABLED,
        CLOSED,
        DEFAULTED,
        DISABLED,
        DISABLEDHOT,
        DISABLEDPUSHED,
        DOWNDISABLED,
        DOWNHOT,
        DOWNNORMAL,
        DOWNPRESSED,
        FOCUSED,
        HOT,
        HOTCHECKED,
        ICONHOT,
        ICONNORMAL,
        ICONPRESSED,
        ICONSORTEDHOT,
        ICONSORTEDNORMAL,
        ICONSORTEDPRESSED,
        INACTIVE,
        INACTIVENORMAL,         // See note 1
        INACTIVEHOT,            // See note 1
        INACTIVEPUSHED,         // See note 1
        INACTIVEDISABLED,       // See note 1
        LEFTDISABLED,
        LEFTHOT,
        LEFTNORMAL,
        LEFTPRESSED,
        MIXEDDISABLED,
        MIXEDHOT,
        MIXEDNORMAL,
        MIXEDPRESSED,
        NORMAL,
        PRESSED,
        OPENED,
        PUSHED,
        READONLY,
        RIGHTDISABLED,
        RIGHTHOT,
        RIGHTNORMAL,
        RIGHTPRESSED,
        SELECTED,
        UNCHECKEDDISABLED,
        UNCHECKEDHOT,
        UNCHECKEDNORMAL,
        UNCHECKEDPRESSED,
        UPDISABLED,
        UPHOT,
        UPNORMAL,
        UPPRESSED,
        HOVER,
        UPHOVER,
        DOWNHOVER,
        LEFTHOVER,
        RIGHTHOVER,
        SORTEDDOWN,
        SORTEDHOT,
        SORTEDNORMAL,
        SORTEDPRESSED,
        SORTEDUP;


        /**
         * A map of allowed states for each Part
         */
        private static EnumMap<Part, State[]> stateMap;

        private static synchronized void initStates() {
            stateMap = new EnumMap<Part, State[]>(Part.class);

            stateMap.put(Part.EP_EDITTEXT,
                       new State[] {
                        NORMAL, HOT, SELECTED, DISABLED, FOCUSED, READONLY, ASSIST
            });

            stateMap.put(Part.BP_PUSHBUTTON,
                       new State[] { NORMAL, HOT, PRESSED, DISABLED, DEFAULTED });

            stateMap.put(Part.BP_RADIOBUTTON,
                       new State[] {
                        UNCHECKEDNORMAL, UNCHECKEDHOT, UNCHECKEDPRESSED, UNCHECKEDDISABLED,
                        CHECKEDNORMAL,   CHECKEDHOT,   CHECKEDPRESSED,   CHECKEDDISABLED
            });

            stateMap.put(Part.BP_CHECKBOX,
                       new State[] {
                        UNCHECKEDNORMAL, UNCHECKEDHOT, UNCHECKEDPRESSED, UNCHECKEDDISABLED,
                        CHECKEDNORMAL,   CHECKEDHOT,   CHECKEDPRESSED,   CHECKEDDISABLED,
                        MIXEDNORMAL,     MIXEDHOT,     MIXEDPRESSED,     MIXEDDISABLED
            });

            State[] comboBoxStates = new State[] { NORMAL, HOT, PRESSED, DISABLED };
            stateMap.put(Part.CP_COMBOBOX, comboBoxStates);
            stateMap.put(Part.CP_DROPDOWNBUTTON, comboBoxStates);
            stateMap.put(Part.CP_BACKGROUND, comboBoxStates);
            stateMap.put(Part.CP_TRANSPARENTBACKGROUND, comboBoxStates);
            stateMap.put(Part.CP_BORDER, comboBoxStates);
            stateMap.put(Part.CP_READONLY, comboBoxStates);
            stateMap.put(Part.CP_DROPDOWNBUTTONRIGHT, comboBoxStates);
            stateMap.put(Part.CP_DROPDOWNBUTTONLEFT, comboBoxStates);
            stateMap.put(Part.CP_CUEBANNER, comboBoxStates);

            stateMap.put(Part.HP_HEADERITEM, new State[] { NORMAL, HOT, PRESSED,
                          SORTEDNORMAL, SORTEDHOT, SORTEDPRESSED,
                          ICONNORMAL, ICONHOT, ICONPRESSED,
                          ICONSORTEDNORMAL, ICONSORTEDHOT, ICONSORTEDPRESSED });

            stateMap.put(Part.HP_HEADERSORTARROW,
                         new State[] {SORTEDDOWN, SORTEDUP});

            State[] listBoxStates = new State[] { NORMAL, PRESSED, HOT, DISABLED};
            stateMap.put(Part.LBCP_BORDER_HSCROLL, listBoxStates);
            stateMap.put(Part.LBCP_BORDER_HVSCROLL, listBoxStates);
            stateMap.put(Part.LBCP_BORDER_NOSCROLL, listBoxStates);
            stateMap.put(Part.LBCP_BORDER_VSCROLL, listBoxStates);

            State[] scrollBarStates = new State[] { NORMAL, HOT, PRESSED, DISABLED, HOVER };
            stateMap.put(Part.SBP_SCROLLBAR,    scrollBarStates);
            stateMap.put(Part.SBP_THUMBBTNVERT, scrollBarStates);
            stateMap.put(Part.SBP_THUMBBTNHORZ, scrollBarStates);
            stateMap.put(Part.SBP_GRIPPERVERT,  scrollBarStates);
            stateMap.put(Part.SBP_GRIPPERHORZ,  scrollBarStates);

            stateMap.put(Part.SBP_ARROWBTN,
                       new State[] {
                UPNORMAL,    UPHOT,     UPPRESSED,    UPDISABLED,
                DOWNNORMAL,  DOWNHOT,   DOWNPRESSED,  DOWNDISABLED,
                LEFTNORMAL,  LEFTHOT,   LEFTPRESSED,  LEFTDISABLED,
                RIGHTNORMAL, RIGHTHOT,  RIGHTPRESSED, RIGHTDISABLED,
                UPHOVER,     DOWNHOVER, LEFTHOVER,    RIGHTHOVER
            });


            State[] spinnerStates = new State[] { NORMAL, HOT, PRESSED, DISABLED };
            stateMap.put(Part.SPNP_UP,   spinnerStates);
            stateMap.put(Part.SPNP_DOWN, spinnerStates);

            stateMap.put(Part.TVP_GLYPH, new State[] { CLOSED, OPENED });

            State[] frameButtonStates = new State[] {
                        NORMAL,         HOT,         PUSHED,         DISABLED,  // See note 1
                        INACTIVENORMAL, INACTIVEHOT, INACTIVEPUSHED, INACTIVEDISABLED,
            };
            // Note 1: The INACTIVE frame button states apply when the frame
            //         is inactive. They are not defined in tmschema.h

            // Fix for 6316538: Vista has five frame button states
            if (ThemeReader.getInt(Control.WINDOW.toString(),
                                   Part.WP_CLOSEBUTTON.getValue(), 1,
                                   Prop.IMAGECOUNT.getValue()) == 10) {
                frameButtonStates = new State[] {
                        NORMAL,         HOT,         PUSHED,         DISABLED,         null,
                        INACTIVENORMAL, INACTIVEHOT, INACTIVEPUSHED, INACTIVEDISABLED, null
                };
            }

            stateMap.put(Part.WP_MINBUTTON,     frameButtonStates);
            stateMap.put(Part.WP_MAXBUTTON,     frameButtonStates);
            stateMap.put(Part.WP_RESTOREBUTTON, frameButtonStates);
            stateMap.put(Part.WP_CLOSEBUTTON,   frameButtonStates);

            // States for Slider (trackbar)
            stateMap.put(Part.TKP_TRACK,     new State[] { NORMAL });
            stateMap.put(Part.TKP_TRACKVERT, new State[] { NORMAL });

            State[] sliderThumbStates =
                new State[] { NORMAL, HOT, PRESSED, FOCUSED, DISABLED };
            stateMap.put(Part.TKP_THUMB,       sliderThumbStates);
            stateMap.put(Part.TKP_THUMBBOTTOM, sliderThumbStates);
            stateMap.put(Part.TKP_THUMBTOP,    sliderThumbStates);
            stateMap.put(Part.TKP_THUMBVERT,   sliderThumbStates);
            stateMap.put(Part.TKP_THUMBRIGHT,  sliderThumbStates);

            // States for Tabs
            State[] tabStates = new State[] { NORMAL, HOT, SELECTED, DISABLED, FOCUSED };
            stateMap.put(Part.TABP_TABITEM,          tabStates);
            stateMap.put(Part.TABP_TABITEMLEFTEDGE,  tabStates);
            stateMap.put(Part.TABP_TABITEMRIGHTEDGE, tabStates);


            stateMap.put(Part.TP_BUTTON,
                       new State[] {
                        NORMAL, HOT, PRESSED, DISABLED, CHECKED, HOTCHECKED
            });

            State[] frameStates = new State[] { ACTIVE, INACTIVE };
            stateMap.put(Part.WP_WINDOW,      frameStates);
            stateMap.put(Part.WP_FRAMELEFT,   frameStates);
            stateMap.put(Part.WP_FRAMERIGHT,  frameStates);
            stateMap.put(Part.WP_FRAMEBOTTOM, frameStates);

            State[] captionStates = new State[] { ACTIVE, INACTIVE, DISABLED };
            stateMap.put(Part.WP_CAPTION,    captionStates);
            stateMap.put(Part.WP_MINCAPTION, captionStates);
            stateMap.put(Part.WP_MAXCAPTION, captionStates);

            stateMap.put(Part.MP_BARBACKGROUND,
                         new State[] { ACTIVE, INACTIVE });
            stateMap.put(Part.MP_BARITEM,
                         new State[] { NORMAL, HOT, PUSHED,
                                       DISABLED, DISABLEDHOT, DISABLEDPUSHED });
            stateMap.put(Part.MP_POPUPCHECK,
                         new State[] { CHECKMARKNORMAL, CHECKMARKDISABLED,
                                       BULLETNORMAL, BULLETDISABLED });
            stateMap.put(Part.MP_POPUPCHECKBACKGROUND,
                         new State[] { DISABLEDPUSHED, NORMAL, BITMAP });
            stateMap.put(Part.MP_POPUPITEM,
                         new State[] { NORMAL, HOT, DISABLED, DISABLEDHOT });
            stateMap.put(Part.MP_POPUPSUBMENU,
                         new State[] { NORMAL, DISABLED });

        }


        public static synchronized int getValue(Part part, State state) {
            if (stateMap == null) {
                initStates();
            }

            Enum<?>[] states = stateMap.get(part);
            if (states != null) {
                for (int i = 0; i < states.length; i++) {
                    if (state == states[i]) {
                        return i + 1;
                    }
                }
            }

            if (state == null || state == State.NORMAL) {
                return 1;
            }

            return 0;
        }

    }


    /**
     * An enumeration of the possible component attributes and the
     * corresponding value type
     */
    public static enum Prop {
        COLOR(Color.class,                204),
        SIZE(Dimension.class,             207),

        FLATMENUS(Boolean.class,         1001),

        BORDERONLY(Boolean.class,        2203), // only draw the border area of the image

        IMAGECOUNT(Integer.class,        2401), // the number of state images in an imagefile
        BORDERSIZE(Integer.class,        2403), // the size of the border line for bgtype=BorderFill

        PROGRESSCHUNKSIZE(Integer.class, 2411), // size of progress control chunks
        PROGRESSSPACESIZE(Integer.class, 2412), // size of progress control spaces

        TEXTSHADOWOFFSET(Point.class,    3402), // where char shadows are drawn, relative to orig. chars

        NORMALSIZE(Dimension.class,      3409), // size of dest rect that exactly source


        SIZINGMARGINS ( Insets.class,    3601), // margins used for 9-grid sizing
        CONTENTMARGINS(Insets.class,     3602), // margins that define where content can be placed
        CAPTIONMARGINS(Insets.class,     3603), // margins that define where caption text can be placed

        BORDERCOLOR(Color.class,         3801), // color of borders for BorderFill
        FILLCOLOR  (  Color.class,       3802), // color of bg fill
        TEXTCOLOR  (  Color.class,       3803), // color text is drawn in

        TEXTSHADOWCOLOR(Color.class,     3818), // color of text shadow

        BGTYPE(Integer.class,            4001), // basic drawing type for each part

        TEXTSHADOWTYPE(Integer.class,    4010), // type of shadow to draw with text

        TRANSITIONDURATIONS(Integer.class, 6000);

        private final Class<?> type;
        private final int value;

        private Prop(Class<?> type, int value) {
            this.type     = type;
            this.value    = value;
        }

        public int getValue() {
            return value;
        }

        public String toString() {
            return name()+"["+type.getName()+"] = "+value;
        }
    }


    /**
     * An enumeration of attribute values for some Props
     */
    public static enum TypeEnum {
        BT_IMAGEFILE (Prop.BGTYPE, "imagefile",  0),
        BT_BORDERFILL(Prop.BGTYPE, "borderfill", 1),

        TST_NONE(Prop.TEXTSHADOWTYPE, "none", 0),
        TST_SINGLE(Prop.TEXTSHADOWTYPE, "single", 1),
        TST_CONTINUOUS(Prop.TEXTSHADOWTYPE, "continuous", 2);


        private TypeEnum(Prop prop, String enumName, int value) {
            this.prop = prop;
            this.enumName = enumName;
            this.value = value;
        }

        private final Prop prop;
        private final String enumName;
        private final int value;

        public String toString() {
            return prop+"="+enumName+"="+value;
        }

        String getName() {
            return enumName;
        }


        static TypeEnum getTypeEnum(Prop prop, int enumval) {
            for (TypeEnum e : TypeEnum.values()) {
                if (e.prop == prop && e.value == enumval) {
                    return e;
                }
            }
            return null;
        }
    }
}
