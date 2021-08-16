/*
 * Copyright (c) 2003, 2005, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Graphics;
import java.util.LinkedList;

import sun.swing.plaf.synth.DefaultSynthStyle;

/**
 * ParsedSynthStyle are the SynthStyle's that SynthParser creates.
 *
 * @author Scott Violet
 */
class ParsedSynthStyle extends DefaultSynthStyle {
    private static SynthPainter DELEGATING_PAINTER_INSTANCE = new
                        DelegatingPainter();
    private PainterInfo[] _painters;

    private static PainterInfo[] mergePainterInfo(PainterInfo[] old,
                                                  PainterInfo[] newPI) {
        if (old == null) {
            return newPI;
        }
        if (newPI == null) {
            return old;
        }
        int oldLength = old.length;
        int newLength = newPI.length;
        int dups = 0;
        PainterInfo[] merged = new PainterInfo[oldLength + newLength];
        System.arraycopy(old, 0, merged, 0, oldLength);
        for (int newCounter = 0; newCounter < newLength; newCounter++) {
            boolean found = false;
            for (int oldCounter = 0; oldCounter < oldLength - dups;
                     oldCounter++) {
                if (newPI[newCounter].equalsPainter(old[oldCounter])) {
                    merged[oldCounter] = newPI[newCounter];
                    dups++;
                    found = true;
                    break;
                }
            }
            if (!found) {
                merged[oldLength + newCounter - dups] = newPI[newCounter];
            }
        }
        if (dups > 0) {
            PainterInfo[] tmp = merged;
            merged = new PainterInfo[merged.length - dups];
            System.arraycopy(tmp, 0, merged, 0, merged.length);
        }
        return merged;
    }


    public ParsedSynthStyle() {
    }

    public ParsedSynthStyle(DefaultSynthStyle style) {
        super(style);
        if (style instanceof ParsedSynthStyle) {
            ParsedSynthStyle pStyle = (ParsedSynthStyle)style;

            if (pStyle._painters != null) {
                _painters = pStyle._painters;
            }
        }
    }

    public SynthPainter getPainter(SynthContext ss) {
        return DELEGATING_PAINTER_INSTANCE;
    }

    public void setPainters(PainterInfo[] info) {
        _painters = info;
    }

    public DefaultSynthStyle addTo(DefaultSynthStyle style) {
        if (!(style instanceof ParsedSynthStyle)) {
            style = new ParsedSynthStyle(style);
        }
        ParsedSynthStyle pStyle = (ParsedSynthStyle)super.addTo(style);
        pStyle._painters = mergePainterInfo(pStyle._painters, _painters);
        return pStyle;
    }

    private SynthPainter getBestPainter(SynthContext context, String method,
                                        int direction) {
        // Check the state info first
        StateInfo info = (StateInfo)getStateInfo(context.getComponentState());
        SynthPainter painter;
        if (info != null) {
            if ((painter = getBestPainter(info.getPainters(), method,
                                          direction)) != null) {
                return painter;
            }
        }
        if ((painter = getBestPainter(_painters, method, direction)) != null) {
            return painter;
        }
        return SynthPainter.NULL_PAINTER;
    }

    private SynthPainter getBestPainter(PainterInfo[] info, String method,
                                        int direction) {
        if (info != null) {
            // Painter specified with no method
            SynthPainter nullPainter = null;
            // Painter specified for this method
            SynthPainter methodPainter = null;

            for (int counter = info.length - 1; counter >= 0; counter--) {
                PainterInfo pi = info[counter];

                if (pi.getMethod() == method) {
                    if (pi.getDirection() == direction) {
                        return pi.getPainter();
                    }
                    else if (methodPainter == null &&pi.getDirection() == -1) {
                        methodPainter = pi.getPainter();
                    }
                }
                else if (nullPainter == null && pi.getMethod() == null) {
                    nullPainter = pi.getPainter();
                }
            }
            if (methodPainter != null) {
                return methodPainter;
            }
            return nullPainter;
        }
        return null;
    }

    public String toString() {
        StringBuilder text = new StringBuilder(super.toString());
        if (_painters != null) {
            text.append(",painters=[");
            for (int i = 0; i < +_painters.length; i++) {
                text.append(_painters[i].toString());
            }
            text.append("]");
        }
        return text.toString();
    }


    static class StateInfo extends DefaultSynthStyle.StateInfo {
        private PainterInfo[] _painterInfo;

        public StateInfo() {
        }

        public StateInfo(DefaultSynthStyle.StateInfo info) {
            super(info);
            if (info instanceof StateInfo) {
                _painterInfo = ((StateInfo)info)._painterInfo;
            }
        }

        public void setPainters(PainterInfo[] painterInfo) {
            _painterInfo = painterInfo;
        }

        public PainterInfo[] getPainters() {
            return _painterInfo;
        }

        public Object clone() {
            return new StateInfo(this);
        }

        public DefaultSynthStyle.StateInfo addTo(
                           DefaultSynthStyle.StateInfo info) {
            if (!(info instanceof StateInfo)) {
                info = new StateInfo(info);
            }
            else {
                info = super.addTo(info);
                StateInfo si = (StateInfo)info;
                si._painterInfo = mergePainterInfo(si._painterInfo,
                                                   _painterInfo);
            }
            return info;
        }

        public String toString() {
            StringBuilder text = new StringBuilder(super.toString());
            text.append(",painters=[");
            if (_painterInfo != null) {
                for (int i = 0; i < +_painterInfo.length; i++) {
                    text.append("    ").append(_painterInfo[i].toString());
                }
            }
            text.append("]");
            return text.toString();
        }
    }


    static class PainterInfo {
        private String _method;
        private SynthPainter _painter;
        private int _direction;

        PainterInfo(String method, SynthPainter painter, int direction) {
            if (method != null) {
                _method = method.intern();
            }
            _painter = painter;
            _direction = direction;
        }

        void addPainter(SynthPainter painter) {
            if (!(_painter instanceof AggregatePainter)) {
                _painter = new AggregatePainter(_painter);
            }

            ((AggregatePainter) _painter).addPainter(painter);
        }

        String getMethod() {
            return _method;
        }

        SynthPainter getPainter() {
            return _painter;
        }

        int getDirection() {
            return _direction;
        }

        boolean equalsPainter(PainterInfo info) {
            return (_method == info._method && _direction == info._direction);
        }

        public String toString() {
            return "PainterInfo {method=" + _method + ",direction=" +
                _direction + ",painter=" + _painter +"}";
        }
    }

    private static class AggregatePainter extends SynthPainter {
        private java.util.List<SynthPainter> painters;

        AggregatePainter(SynthPainter painter) {
            painters = new LinkedList<SynthPainter>();
            painters.add(painter);
        }

        void addPainter(SynthPainter painter) {
            if (painter != null) {
                painters.add(painter);
            }
        }

        public void paintArrowButtonBackground(SynthContext context, Graphics g,
                                               int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintArrowButtonBackground(context, g, x, y, w, h);
            }
        }

        public void paintArrowButtonBorder(SynthContext context, Graphics g,
                                           int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintArrowButtonBorder(context, g, x, y, w, h);
            }
        }

        public void paintArrowButtonForeground(SynthContext context, Graphics g,
                                               int x, int y, int w, int h,
                                               int direction) {
            for (SynthPainter painter: painters) {
                painter.paintArrowButtonForeground(context, g,
                                                   x, y, w, h, direction);
            }
        }

        public void paintButtonBackground(SynthContext context, Graphics g,
                                          int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintButtonBackground(context, g, x, y, w, h);
            }
        }

        public void paintButtonBorder(SynthContext context, Graphics g,
                                      int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintButtonBorder(context, g, x, y, w, h);
            }
        }

        public void paintCheckBoxMenuItemBackground(SynthContext context,
                                                    Graphics g, int x, int y,
                                                    int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintCheckBoxMenuItemBackground(context, g, x, y, w, h);
            }
        }

        public void paintCheckBoxMenuItemBorder(SynthContext context,
                                                Graphics g, int x, int y,
                                                int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintCheckBoxMenuItemBorder(context, g, x, y, w, h);
            }
        }

        public void paintCheckBoxBackground(SynthContext context, Graphics g,
                                            int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintCheckBoxBackground(context, g, x, y, w, h);
            }
        }

        public void paintCheckBoxBorder(SynthContext context, Graphics g,
                                        int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintCheckBoxBorder(context, g, x, y, w, h);
            }
        }

        public void paintColorChooserBackground(SynthContext context,
                                                Graphics g, int x, int y,
                                                int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintColorChooserBackground(context, g, x, y, w, h);
            }
        }

        public void paintColorChooserBorder(SynthContext context, Graphics g,
                                            int x, int y,
                                            int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintColorChooserBorder(context, g, x, y, w, h);
            }
        }

        public void paintComboBoxBackground(SynthContext context, Graphics g,
                                            int x, int y,
                                            int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintComboBoxBackground(context, g, x, y, w, h);
            }
        }

        public void paintComboBoxBorder(SynthContext context, Graphics g,
                                        int x, int y,
                                        int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintComboBoxBorder(context, g, x, y, w, h);
            }
        }

        public void paintDesktopIconBackground(SynthContext context, Graphics g,
                                               int x, int y,
                                               int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintDesktopIconBackground(context, g, x, y, w, h);
            }
        }

        public void paintDesktopIconBorder(SynthContext context, Graphics g,
                                           int x, int y,
                                           int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintDesktopIconBorder(context, g, x, y, w, h);
            }
        }

        public void paintDesktopPaneBackground(SynthContext context, Graphics g,
                                               int x, int y,
                                               int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintDesktopPaneBackground(context, g, x, y, w, h);
            }
        }

        public void paintDesktopPaneBorder(SynthContext context, Graphics g,
                                           int x, int y,
                                           int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintDesktopPaneBorder(context, g, x, y, w, h);
            }
        }

        public void paintEditorPaneBackground(SynthContext context, Graphics g,
                                              int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintEditorPaneBackground(context, g, x, y, w, h);
            }
        }

        public void paintEditorPaneBorder(SynthContext context, Graphics g,
                                          int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintEditorPaneBorder(context, g, x, y, w, h);
            }
        }

        public void paintFileChooserBackground(SynthContext context, Graphics g,
                                               int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintFileChooserBackground(context, g, x, y, w, h);
            }
        }

        public void paintFileChooserBorder(SynthContext context, Graphics g,
                                           int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintFileChooserBorder(context, g, x, y, w, h);
            }
        }

        public void paintFormattedTextFieldBackground(SynthContext context,
                                                      Graphics g, int x, int y,
                                                      int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintFormattedTextFieldBackground(context, g, x, y, w, h);
            }
        }

        public void paintFormattedTextFieldBorder(SynthContext context,
                                                  Graphics g, int x, int y,
                                                  int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintFormattedTextFieldBorder(context, g, x, y, w, h);
            }
        }

        public void paintInternalFrameTitlePaneBackground(SynthContext context,
                                                          Graphics g,
                                                          int x, int y,
                                                          int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintInternalFrameTitlePaneBackground(context, g,
                                                              x, y, w, h);
            }
        }

        public void paintInternalFrameTitlePaneBorder(SynthContext context,
                                                      Graphics g,
                                                      int x, int y,
                                                      int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintInternalFrameTitlePaneBorder(context, g,
                                                          x, y, w, h);
            }
        }

        public void paintInternalFrameBackground(SynthContext context,
                                                 Graphics g, int x, int y,
                                                 int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintInternalFrameBackground(context, g, x, y, w, h);
            }
        }

        public void paintInternalFrameBorder(SynthContext context, Graphics g,
                                             int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintInternalFrameBorder(context, g, x, y, w, h);
            }
        }

        public void paintLabelBackground(SynthContext context, Graphics g,
                                         int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintLabelBackground(context, g, x, y, w, h);
            }
        }

        public void paintLabelBorder(SynthContext context, Graphics g,
                                     int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintLabelBorder(context, g, x, y, w, h);
            }
        }

        public void paintListBackground(SynthContext context, Graphics g,
                                        int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintListBackground(context, g, x, y, w, h);
            }
        }

        public void paintListBorder(SynthContext context, Graphics g,
                                    int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintListBorder(context, g, x, y, w, h);
            }
        }

        public void paintMenuBarBackground(SynthContext context, Graphics g,
                                           int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintMenuBarBackground(context, g, x, y, w, h);
            }
        }

        public void paintMenuBarBorder(SynthContext context, Graphics g,
                                       int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintMenuBarBorder(context, g, x, y, w, h);
            }
        }

        public void paintMenuItemBackground(SynthContext context, Graphics g,
                                            int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintMenuItemBackground(context, g, x, y, w, h);
            }
        }

        public void paintMenuItemBorder(SynthContext context, Graphics g,
                                        int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintMenuItemBorder(context, g, x, y, w, h);
            }
        }

        public void paintMenuBackground(SynthContext context, Graphics g,
                                        int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintMenuBackground(context, g, x, y, w, h);
            }
        }

        public void paintMenuBorder(SynthContext context, Graphics g,
                                    int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintMenuBorder(context, g, x, y, w, h);
            }
        }

        public void paintOptionPaneBackground(SynthContext context, Graphics g,
                                              int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintOptionPaneBackground(context, g, x, y, w, h);
            }
        }

        public void paintOptionPaneBorder(SynthContext context, Graphics g,
                                          int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintOptionPaneBorder(context, g, x, y, w, h);
            }
        }

        public void paintPanelBackground(SynthContext context, Graphics g,
                                         int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintPanelBackground(context, g, x, y, w, h);
            }
        }

        public void paintPanelBorder(SynthContext context, Graphics g,
                                     int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintPanelBorder(context, g, x, y, w, h);
            }
        }

        public void paintPasswordFieldBackground(SynthContext context,
                                                 Graphics g,
                                                 int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintPasswordFieldBackground(context, g, x, y, w, h);
            }
        }

        public void paintPasswordFieldBorder(SynthContext context, Graphics g,
                                             int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintPasswordFieldBorder(context, g, x, y, w, h);
            }
        }

        public void paintPopupMenuBackground(SynthContext context, Graphics g,
                                             int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintPopupMenuBackground(context, g, x, y, w, h);
            }
        }

        public void paintPopupMenuBorder(SynthContext context, Graphics g,
                                         int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintPopupMenuBorder(context, g, x, y, w, h);
            }
        }

        public void paintProgressBarBackground(SynthContext context, Graphics g,
                                               int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintProgressBarBackground(context, g, x, y, w, h);
            }
        }

        public void paintProgressBarBackground(SynthContext context, Graphics g,
                                               int x, int y, int w, int h,
                                               int orientation) {
            for (SynthPainter painter: painters) {
                painter.paintProgressBarBackground(context, g, x, y, w, h,
                                                   orientation);
            }
        }

        public void paintProgressBarBorder(SynthContext context, Graphics g,
                                           int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintProgressBarBorder(context, g, x, y, w, h);
            }
        }

        public void paintProgressBarBorder(SynthContext context, Graphics g,
                                           int x, int y, int w, int h,
                                           int orientation) {
            for (SynthPainter painter: painters) {
                painter.paintProgressBarBorder(context, g, x, y, w, h,
                                               orientation);
            }
        }

        public void paintProgressBarForeground(SynthContext context, Graphics g,
                                               int x, int y, int w, int h,
                                               int orientation) {
            for (SynthPainter painter: painters) {
                painter.paintProgressBarForeground(context, g,
                                                   x, y, w, h, orientation);
            }
        }

        public void paintRadioButtonMenuItemBackground(SynthContext context,
                                                       Graphics g,
                                                       int x, int y,
                                                       int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintRadioButtonMenuItemBackground(context, g,
                                                           x, y, w, h);
            }
        }

        public void paintRadioButtonMenuItemBorder(SynthContext context,
                                                   Graphics g, int x, int y,
                                                   int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintRadioButtonMenuItemBorder(context, g, x, y, w, h);
            }
        }

        public void paintRadioButtonBackground(SynthContext context, Graphics g,
                                               int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintRadioButtonBackground(context, g, x, y, w, h);
            }
        }

        public void paintRadioButtonBorder(SynthContext context, Graphics g,
                                           int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintRadioButtonBorder(context, g, x, y, w, h);
            }
        }

        public void paintRootPaneBackground(SynthContext context, Graphics g,
                                            int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintRootPaneBackground(context, g, x, y, w, h);
            }
        }

        public void paintRootPaneBorder(SynthContext context, Graphics g,
                                        int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintRootPaneBorder(context, g, x, y, w, h);
            }
        }

        public void paintScrollBarBackground(SynthContext context, Graphics g,
                                             int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintScrollBarBackground(context, g, x, y, w, h);
            }
        }

        public void paintScrollBarBackground(SynthContext context, Graphics g,
                                             int x, int y,
                                             int w, int h, int orientation) {
            for (SynthPainter painter: painters) {
                painter.paintScrollBarBackground(context, g, x, y, w, h,
                                                 orientation);
            }
        }

        public void paintScrollBarBorder(SynthContext context, Graphics g,
                                         int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintScrollBarBorder(context, g, x, y, w, h);
            }
        }

        public void paintScrollBarBorder(SynthContext context, Graphics g,
                                         int x, int y, int w, int h,
                                         int orientation) {
            for (SynthPainter painter: painters) {
                painter.paintScrollBarBorder(context, g, x, y, w, h,
                                             orientation);
            }
        }

        public void paintScrollBarThumbBackground(SynthContext context,
                                                  Graphics g, int x, int y,
                                                  int w, int h, int orientation) {
            for (SynthPainter painter: painters) {
                painter.paintScrollBarThumbBackground(context, g, x, y, w, h,
                                                      orientation);
            }
        }

        public void paintScrollBarThumbBorder(SynthContext context, Graphics g,
                                              int x, int y, int w, int h,
                                              int orientation) {
            for (SynthPainter painter: painters) {
                painter.paintScrollBarThumbBorder(context, g, x, y, w, h,
                                                  orientation);
            }
        }

        public void paintScrollBarTrackBackground(SynthContext context,
                                                  Graphics g, int x, int y,
                                                  int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintScrollBarTrackBackground(context, g, x, y, w, h);
            }
        }

        public void paintScrollBarTrackBackground(SynthContext context,
                                                  Graphics g, int x, int y,
                                                  int w, int h,
                                                  int orientation) {
            for (SynthPainter painter: painters) {
                painter.paintScrollBarTrackBackground(context, g, x, y, w, h,
                                                      orientation);
            }
        }

        public void paintScrollBarTrackBorder(SynthContext context, Graphics g,
                                              int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintScrollBarTrackBorder(context, g, x, y, w, h);
            }
        }

        public void paintScrollBarTrackBorder(SynthContext context, Graphics g,
                                              int x, int y, int w, int h,
                                              int orientation) {
            for (SynthPainter painter: painters) {
                painter.paintScrollBarTrackBorder(context, g, x, y, w, h,
                                                  orientation);
            }
        }

        public void paintScrollPaneBackground(SynthContext context, Graphics g,
                                              int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintScrollPaneBackground(context, g, x, y, w, h);
            }
        }

        public void paintScrollPaneBorder(SynthContext context, Graphics g,
                                          int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintScrollPaneBorder(context, g, x, y, w, h);
            }
        }

        public void paintSeparatorBackground(SynthContext context, Graphics g,
                                             int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintSeparatorBackground(context, g, x, y, w, h);
            }
        }

        public void paintSeparatorBackground(SynthContext context, Graphics g,
                                             int x, int y, int w, int h,
                                             int orientation) {
            for (SynthPainter painter: painters) {
                painter.paintSeparatorBackground(context, g, x, y, w, h, orientation);
            }
        }

        public void paintSeparatorBorder(SynthContext context, Graphics g,
                                         int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintSeparatorBorder(context, g, x, y, w, h);
            }
        }

        public void paintSeparatorBorder(SynthContext context, Graphics g,
                                         int x, int y, int w, int h, int orientation) {
            for (SynthPainter painter: painters) {
                painter.paintSeparatorBorder(context, g, x, y, w, h, orientation);
            }
        }

        public void paintSeparatorForeground(SynthContext context, Graphics g,
                                             int x, int y, int w, int h,
                                             int orientation) {
            for (SynthPainter painter: painters) {
                painter.paintSeparatorForeground(context, g,
                                                 x, y, w, h, orientation);
            }
        }

        public void paintSliderBackground(SynthContext context, Graphics g,
                                          int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintSliderBackground(context, g, x, y, w, h);
            }
        }

        public void paintSliderBackground(SynthContext context, Graphics g,
                                          int x, int y, int w, int h,
                                          int orientation) {
            for (SynthPainter painter: painters) {
                painter.paintSliderBackground(context, g, x, y, w, h, orientation);
            }
        }

        public void paintSliderBorder(SynthContext context, Graphics g,
                                      int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintSliderBorder(context, g, x, y, w, h);
            }
        }

        public void paintSliderBorder(SynthContext context, Graphics g,
                                      int x, int y, int w, int h,
                                      int orientation) {
            for (SynthPainter painter: painters) {
                painter.paintSliderBorder(context, g, x, y, w, h, orientation);
            }
        }

        public void paintSliderThumbBackground(SynthContext context, Graphics g,
                                               int x, int y, int w, int h,
                                               int orientation) {
            for (SynthPainter painter: painters) {
                painter.paintSliderThumbBackground(context, g,
                                                   x, y, w, h, orientation);
            }
        }

        public void paintSliderThumbBorder(SynthContext context, Graphics g,
                                           int x, int y, int w, int h,
                                           int orientation) {
            for (SynthPainter painter: painters) {
                painter.paintSliderThumbBorder(context, g,
                                               x, y, w, h, orientation);
            }
        }

        public void paintSliderTrackBackground(SynthContext context, Graphics g,
                                               int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintSliderTrackBackground(context, g, x, y, w, h);
            }
        }

        public void paintSliderTrackBackground(SynthContext context, Graphics g,
                                               int x, int y, int w, int h,
                                               int orientation) {
            for (SynthPainter painter: painters) {
                painter.paintSliderTrackBackground(context, g, x, y, w, h,
                                                   orientation);
            }
        }

        public void paintSliderTrackBorder(SynthContext context, Graphics g,
                                           int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintSliderTrackBorder(context, g, x, y, w, h);
            }
        }

        public void paintSliderTrackBorder(SynthContext context, Graphics g,
                                           int x, int y, int w, int h,
                                           int orientation) {
            for (SynthPainter painter: painters) {
                painter.paintSliderTrackBorder(context, g, x, y, w, h,
                                               orientation);
            }
        }

        public void paintSpinnerBackground(SynthContext context, Graphics g,
                                           int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintSpinnerBackground(context, g, x, y, w, h);
            }
        }

        public void paintSpinnerBorder(SynthContext context, Graphics g,
                                       int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintSpinnerBorder(context, g, x, y, w, h);
            }
        }

        public void paintSplitPaneDividerBackground(SynthContext context,
                                                    Graphics g, int x, int y,
                                                    int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintSplitPaneDividerBackground(context, g, x, y, w, h);
            }
        }

        public void paintSplitPaneDividerBackground(SynthContext context,
                                                    Graphics g, int x, int y,
                                                    int w, int h,
                                                    int orientation) {
            for (SynthPainter painter: painters) {
                painter.paintSplitPaneDividerBackground(context, g, x, y, w, h,
                                                        orientation);
            }
        }

        public void paintSplitPaneDividerForeground(SynthContext context,
                                                    Graphics g, int x, int y,
                                                    int w, int h,
                                                    int orientation) {
            for (SynthPainter painter: painters) {
                painter.paintSplitPaneDividerForeground(context, g,
                                                        x, y, w, h,
                                                        orientation);
            }
        }

        public void paintSplitPaneDragDivider(SynthContext context, Graphics g,
                                              int x, int y,
                                              int w, int h, int orientation) {
            for (SynthPainter painter: painters) {
                painter.paintSplitPaneDragDivider(context, g,
                                                  x, y, w, h, orientation);
            }
        }

        public void paintSplitPaneBackground(SynthContext context, Graphics g,
                                             int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintSplitPaneBackground(context, g, x, y, w, h);
            }
        }

        public void paintSplitPaneBorder(SynthContext context, Graphics g,
                                         int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintSplitPaneBorder(context, g, x, y, w, h);
            }
        }

        public void paintTabbedPaneBackground(SynthContext context, Graphics g,
                                              int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintTabbedPaneBackground(context, g, x, y, w, h);
            }
        }

        public void paintTabbedPaneBorder(SynthContext context, Graphics g,
                                          int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintTabbedPaneBorder(context, g, x, y, w, h);
            }
        }

        public void paintTabbedPaneTabAreaBackground(SynthContext context,
                                                     Graphics g, int x, int y,
                                                     int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintTabbedPaneTabAreaBackground(context, g, x, y, w, h);
            }
        }

        public void paintTabbedPaneTabAreaBackground(SynthContext context,
                                                     Graphics g, int x, int y,
                                                     int w, int h,
                                                     int orientation) {
            for (SynthPainter painter: painters) {
                painter.paintTabbedPaneTabAreaBackground(context, g, x, y, w, h,
                                                         orientation);
            }
        }

        public void paintTabbedPaneTabAreaBorder(SynthContext context,
                                                 Graphics g, int x, int y,
                                                 int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintTabbedPaneTabAreaBorder(context, g, x, y, w, h);
            }
        }

        public void paintTabbedPaneTabAreaBorder(SynthContext context,
                                                 Graphics g, int x, int y,
                                                 int w, int h, int orientation) {
            for (SynthPainter painter: painters) {
                painter.paintTabbedPaneTabAreaBorder(context, g, x, y, w, h,
                                                     orientation);
            }
        }

        public void paintTabbedPaneTabBackground(SynthContext context,
                                                 Graphics g, int x, int y,
                                                 int w, int h, int tabIndex) {
            for (SynthPainter painter: painters) {
                painter.paintTabbedPaneTabBackground(context, g,
                                                     x, y, w, h, tabIndex);
            }
        }

        public void paintTabbedPaneTabBackground(SynthContext context,
                                                 Graphics g, int x, int y,
                                                 int w, int h, int tabIndex,
                                                 int orientation) {
            for (SynthPainter painter: painters) {
                painter.paintTabbedPaneTabBackground(context, g,
                                                     x, y, w, h, tabIndex,
                                                     orientation);
            }
        }

        public void paintTabbedPaneTabBorder(SynthContext context, Graphics g,
                                             int x, int y, int w, int h,
                                             int tabIndex) {
            for (SynthPainter painter: painters) {
                painter.paintTabbedPaneTabBorder(context, g,
                                                 x, y, w, h, tabIndex);
            }
        }

        public void paintTabbedPaneTabBorder(SynthContext context, Graphics g,
                                             int x, int y, int w, int h,
                                             int tabIndex, int orientation) {
            for (SynthPainter painter: painters) {
                painter.paintTabbedPaneTabBorder(context, g,
                                                 x, y, w, h, tabIndex,
                                                 orientation);
            }
        }

        public void paintTabbedPaneContentBackground(SynthContext context,
                                                     Graphics g, int x, int y,
                                                     int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintTabbedPaneContentBackground(context, g, x, y, w, h);
            }
        }

        public void paintTabbedPaneContentBorder(SynthContext context,
                                                 Graphics g, int x, int y,
                                                 int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintTabbedPaneContentBorder(context, g, x, y, w, h);
            }
        }

        public void paintTableHeaderBackground(SynthContext context, Graphics g,
                                               int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintTableHeaderBackground(context, g, x, y, w, h);
            }
        }

        public void paintTableHeaderBorder(SynthContext context, Graphics g,
                                           int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintTableHeaderBorder(context, g, x, y, w, h);
            }
        }

        public void paintTableBackground(SynthContext context, Graphics g,
                                         int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintTableBackground(context, g, x, y, w, h);
            }
        }

        public void paintTableBorder(SynthContext context, Graphics g,
                                     int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintTableBorder(context, g, x, y, w, h);
            }
        }

        public void paintTextAreaBackground(SynthContext context, Graphics g,
                                            int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintTextAreaBackground(context, g, x, y, w, h);
            }
        }

        public void paintTextAreaBorder(SynthContext context, Graphics g,
                                        int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintTextAreaBorder(context, g, x, y, w, h);
            }
        }

        public void paintTextPaneBackground(SynthContext context, Graphics g,
                                            int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintTextPaneBackground(context, g, x, y, w, h);
            }
        }

        public void paintTextPaneBorder(SynthContext context, Graphics g,
                                        int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintTextPaneBorder(context, g, x, y, w, h);
            }
        }

        public void paintTextFieldBackground(SynthContext context, Graphics g,
                                             int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintTextFieldBackground(context, g, x, y, w, h);
            }
        }

        public void paintTextFieldBorder(SynthContext context, Graphics g,
                                         int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintTextFieldBorder(context, g, x, y, w, h);
            }
        }

        public void paintToggleButtonBackground(SynthContext context,
                                                Graphics g, int x, int y,
                                                int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintToggleButtonBackground(context, g, x, y, w, h);
            }
        }

        public void paintToggleButtonBorder(SynthContext context,
                                            Graphics g, int x, int y,
                                            int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintToggleButtonBorder(context, g, x, y, w, h);
            }
        }

        public void paintToolBarBackground(SynthContext context, Graphics g,
                                           int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintToolBarBackground(context, g, x, y, w, h);
            }
        }

        public void paintToolBarBackground(SynthContext context, Graphics g,
                                           int x, int y, int w, int h,
                                           int orientation) {
            for (SynthPainter painter: painters) {
                painter.paintToolBarBackground(context, g, x, y, w, h,
                                               orientation);
            }
        }

        public void paintToolBarBorder(SynthContext context, Graphics g,
                                       int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintToolBarBorder(context, g, x, y, w, h);
            }
        }

        public void paintToolBarBorder(SynthContext context, Graphics g,
                                       int x, int y, int w, int h,
                                       int orientation) {
            for (SynthPainter painter: painters) {
                painter.paintToolBarBorder(context, g, x, y, w, h, orientation);
            }
        }

        public void paintToolBarContentBackground(SynthContext context,
                                                  Graphics g, int x, int y,
                                                  int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintToolBarContentBackground(context, g, x, y, w, h);
            }
        }

        public void paintToolBarContentBackground(SynthContext context,
                                                  Graphics g, int x, int y,
                                                  int w, int h,
                                                  int orientation) {
            for (SynthPainter painter: painters) {
                painter.paintToolBarContentBackground(context, g, x, y, w, h,
                                                      orientation);
            }
        }

        public void paintToolBarContentBorder(SynthContext context, Graphics g,
                                              int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintToolBarContentBorder(context, g, x, y, w, h);
            }
        }

        public void paintToolBarContentBorder(SynthContext context, Graphics g,
                                              int x, int y, int w, int h,
                                              int orientation) {
            for (SynthPainter painter: painters) {
                painter.paintToolBarContentBorder(context, g, x, y, w, h,
                                                  orientation);
            }
        }

        public void paintToolBarDragWindowBackground(SynthContext context,
                                                     Graphics g, int x, int y,
                                                     int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintToolBarDragWindowBackground(context, g, x, y, w, h);
            }
        }

        public void paintToolBarDragWindowBackground(SynthContext context,
                                                     Graphics g, int x, int y,
                                                     int w, int h,
                                                     int orientation) {
            for (SynthPainter painter: painters) {
                painter.paintToolBarDragWindowBackground(context, g, x, y, w, h,
                                                         orientation);
            }
        }

        public void paintToolBarDragWindowBorder(SynthContext context,
                                                 Graphics g, int x, int y,
                                                 int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintToolBarDragWindowBorder(context, g, x, y, w, h);
            }
        }

        public void paintToolBarDragWindowBorder(SynthContext context,
                                                 Graphics g, int x, int y,
                                                 int w, int h,
                                                 int orientation) {
            for (SynthPainter painter: painters) {
                painter.paintToolBarDragWindowBorder(context, g, x, y, w, h,
                                                     orientation);
            }
        }

        public void paintToolTipBackground(SynthContext context,
                                           Graphics g, int x, int y,
                                           int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintToolTipBackground(context, g, x, y, w, h);
            }
        }

        public void paintToolTipBorder(SynthContext context, Graphics g,
                                       int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintToolTipBorder(context, g, x, y, w, h);
            }
        }

        public void paintTreeBackground(SynthContext context, Graphics g,
                                        int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintTreeBackground(context, g, x, y, w, h);
            }
        }

        public void paintTreeBorder(SynthContext context, Graphics g,
                                    int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintTreeBorder(context, g, x, y, w, h);
            }
        }

        public void paintTreeCellBackground(SynthContext context, Graphics g,
                                            int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintTreeCellBackground(context, g, x, y, w, h);
            }
        }

        public void paintTreeCellBorder(SynthContext context, Graphics g,
                                        int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintTreeCellBorder(context, g, x, y, w, h);
            }
        }

        public void paintTreeCellFocus(SynthContext context, Graphics g,
                                       int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintTreeCellFocus(context, g, x, y, w, h);
            }
        }

        public void paintViewportBackground(SynthContext context, Graphics g,
                                            int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintViewportBackground(context, g, x, y, w, h);
            }
        }

        public void paintViewportBorder(SynthContext context, Graphics g,
                                        int x, int y, int w, int h) {
            for (SynthPainter painter: painters) {
                painter.paintViewportBorder(context, g, x, y, w, h);
            }
        }
    }

    private static class DelegatingPainter extends SynthPainter {
        private static SynthPainter getPainter(SynthContext context,
                                               String method, int direction) {
            return ((ParsedSynthStyle)context.getStyle()).getBestPainter(
                               context, method, direction);
        }

        public void paintArrowButtonBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "arrowbuttonbackground", -1).
                paintArrowButtonBackground(context, g, x, y, w, h);
        }

        public void paintArrowButtonBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "arrowbuttonborder", -1).
                paintArrowButtonBorder(context, g, x, y, w, h);
        }

        public void paintArrowButtonForeground(SynthContext context,
                     Graphics g, int x, int y, int w, int h, int direction) {
            getPainter(context, "arrowbuttonforeground", direction).
                paintArrowButtonForeground(context, g, x, y, w, h, direction);
        }

        public void paintButtonBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "buttonbackground", -1).
                paintButtonBackground(context, g, x, y, w, h);
        }

        public void paintButtonBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "buttonborder", -1).
                paintButtonBorder(context, g, x, y, w, h);
        }

        public void paintCheckBoxMenuItemBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "checkboxmenuitembackground", -1).
                paintCheckBoxMenuItemBackground(context, g, x, y, w, h);
        }

        public void paintCheckBoxMenuItemBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "checkboxmenuitemborder", -1).
                paintCheckBoxMenuItemBorder(context, g, x, y, w, h);
        }

        public void paintCheckBoxBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "checkboxbackground", -1).
                paintCheckBoxBackground(context, g, x, y, w, h);
        }

        public void paintCheckBoxBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "checkboxborder", -1).
                paintCheckBoxBorder(context, g, x, y, w, h);
        }

        public void paintColorChooserBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "colorchooserbackground", -1).
                paintColorChooserBackground(context, g, x, y, w, h);
        }

        public void paintColorChooserBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "colorchooserborder", -1).
                paintColorChooserBorder(context, g, x, y, w, h);
        }

        public void paintComboBoxBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "comboboxbackground", -1).
                paintComboBoxBackground(context, g, x, y, w, h);
        }

        public void paintComboBoxBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "comboboxborder", -1).
                paintComboBoxBorder(context, g, x, y, w, h);
        }

        public void paintDesktopIconBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "desktopiconbackground", -1).
                paintDesktopIconBackground(context, g, x, y, w, h);
        }

        public void paintDesktopIconBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "desktopiconborder", -1).
                paintDesktopIconBorder(context, g, x, y, w, h);
        }

        public void paintDesktopPaneBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "desktoppanebackground", -1).
                paintDesktopPaneBackground(context, g, x, y, w, h);
        }

        public void paintDesktopPaneBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "desktoppaneborder", -1).
                paintDesktopPaneBorder(context, g, x, y, w, h);
        }

        public void paintEditorPaneBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "editorpanebackground", -1).
                paintEditorPaneBackground(context, g, x, y, w, h);
        }

        public void paintEditorPaneBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "editorpaneborder", -1).
                paintEditorPaneBorder(context, g, x, y, w, h);
        }

        public void paintFileChooserBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "filechooserbackground", -1).
                paintFileChooserBackground(context, g, x, y, w, h);
        }

        public void paintFileChooserBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "filechooserborder", -1).
                paintFileChooserBorder(context, g, x, y, w, h);
        }

        public void paintFormattedTextFieldBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "formattedtextfieldbackground", -1).
                paintFormattedTextFieldBackground(context, g, x, y, w, h);
        }

        public void paintFormattedTextFieldBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "formattedtextfieldborder", -1).
                paintFormattedTextFieldBorder(context, g, x, y, w, h);
        }

        public void paintInternalFrameTitlePaneBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "internalframetitlepanebackground", -1).
                paintInternalFrameTitlePaneBackground(context, g, x, y, w, h);
        }

        public void paintInternalFrameTitlePaneBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "internalframetitlepaneborder", -1).
                paintInternalFrameTitlePaneBorder(context, g, x, y, w, h);
        }

        public void paintInternalFrameBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "internalframebackground", -1).
                paintInternalFrameBackground(context, g, x, y, w, h);
        }

        public void paintInternalFrameBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "internalframeborder", -1).
                paintInternalFrameBorder(context, g, x, y, w, h);
        }

        public void paintLabelBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "labelbackground", -1).
                paintLabelBackground(context, g, x, y, w, h);
        }

        public void paintLabelBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "labelborder", -1).
                paintLabelBorder(context, g, x, y, w, h);
        }

        public void paintListBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "listbackground", -1).
                paintListBackground(context, g, x, y, w, h);
        }

        public void paintListBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "listborder", -1).
                paintListBorder(context, g, x, y, w, h);
        }

        public void paintMenuBarBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "menubarbackground", -1).
                paintMenuBarBackground(context, g, x, y, w, h);
        }

        public void paintMenuBarBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "menubarborder", -1).
                paintMenuBarBorder(context, g, x, y, w, h);
        }

        public void paintMenuItemBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "menuitembackground", -1).
                paintMenuItemBackground(context, g, x, y, w, h);
        }

        public void paintMenuItemBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "menuitemborder", -1).
                paintMenuItemBorder(context, g, x, y, w, h);
        }

        public void paintMenuBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "menubackground", -1).
                paintMenuBackground(context, g, x, y, w, h);
        }

        public void paintMenuBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "menuborder", -1).
                paintMenuBorder(context, g, x, y, w, h);
        }

        public void paintOptionPaneBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "optionpanebackground", -1).
                paintOptionPaneBackground(context, g, x, y, w, h);
        }

        public void paintOptionPaneBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "optionpaneborder", -1).
                paintOptionPaneBorder(context, g, x, y, w, h);
        }

        public void paintPanelBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "panelbackground", -1).
                paintPanelBackground(context, g, x, y, w, h);
        }

        public void paintPanelBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "panelborder", -1).
                paintPanelBorder(context, g, x, y, w, h);
        }

        public void paintPasswordFieldBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "passwordfieldbackground", -1).
                paintPasswordFieldBackground(context, g, x, y, w, h);
        }

        public void paintPasswordFieldBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "passwordfieldborder", -1).
                paintPasswordFieldBorder(context, g, x, y, w, h);
        }

        public void paintPopupMenuBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "popupmenubackground", -1).
                paintPopupMenuBackground(context, g, x, y, w, h);
        }

        public void paintPopupMenuBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "popupmenuborder", -1).
                paintPopupMenuBorder(context, g, x, y, w, h);
        }

        public void paintProgressBarBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "progressbarbackground", -1).
                paintProgressBarBackground(context, g, x, y, w, h);
        }

        public void paintProgressBarBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h, int direction) {
            getPainter(context, "progressbarbackground", direction).
                paintProgressBarBackground(context, g, x, y, w, h, direction);
        }

        public void paintProgressBarBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "progressbarborder", -1).
                paintProgressBarBorder(context, g, x, y, w, h);
        }

        public void paintProgressBarBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h, int direction) {
            getPainter(context, "progressbarborder", direction).
                paintProgressBarBorder(context, g, x, y, w, h, direction);
        }

        public void paintProgressBarForeground(SynthContext context,
                     Graphics g, int x, int y, int w, int h, int direction) {
            getPainter(context, "progressbarforeground", direction).
                paintProgressBarForeground(context, g, x, y, w, h, direction);
        }

        public void paintRadioButtonMenuItemBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "radiobuttonmenuitembackground", -1).
                paintRadioButtonMenuItemBackground(context, g, x, y, w, h);
        }

        public void paintRadioButtonMenuItemBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "radiobuttonmenuitemborder", -1).
                paintRadioButtonMenuItemBorder(context, g, x, y, w, h);
        }

        public void paintRadioButtonBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "radiobuttonbackground", -1).
                paintRadioButtonBackground(context, g, x, y, w, h);
        }

        public void paintRadioButtonBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "radiobuttonborder", -1).
                paintRadioButtonBorder(context, g, x, y, w, h);
        }

        public void paintRootPaneBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "rootpanebackground", -1).
                paintRootPaneBackground(context, g, x, y, w, h);
        }

        public void paintRootPaneBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "rootpaneborder", -1).
                paintRootPaneBorder(context, g, x, y, w, h);
        }

        public void paintScrollBarBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "scrollbarbackground", -1).
               paintScrollBarBackground(context, g, x, y, w, h);
        }

        public void paintScrollBarBackground(SynthContext context,
                      Graphics g, int x, int y, int w, int h, int direction) {
            getPainter(context, "scrollbarbackground", direction).
                paintScrollBarBackground(context, g, x, y, w, h, direction);
        }


        public void paintScrollBarBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "scrollbarborder", -1).
                paintScrollBarBorder(context, g, x, y, w, h);
        }

        public void paintScrollBarBorder(SynthContext context,
                                         Graphics g, int x, int y, int w, int h,
                                         int orientation) {
            getPainter(context, "scrollbarborder", orientation).
                paintScrollBarBorder(context, g, x, y, w, h, orientation);
        }

        public void paintScrollBarThumbBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h, int direction) {
            getPainter(context, "scrollbarthumbbackground", direction).
                paintScrollBarThumbBackground(context, g, x, y, w, h, direction);
        }

        public void paintScrollBarThumbBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h, int direction) {
            getPainter(context, "scrollbarthumbborder", direction).
                paintScrollBarThumbBorder(context, g, x, y, w, h, direction);
        }

        public void paintScrollBarTrackBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "scrollbartrackbackground", -1).
                paintScrollBarTrackBackground(context, g, x, y, w, h);
        }

        public void paintScrollBarTrackBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h, int direction) {
             getPainter(context, "scrollbartrackbackground", direction).
                 paintScrollBarTrackBackground(context, g, x, y, w, h, direction);
         }

        public void paintScrollBarTrackBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "scrollbartrackborder", -1).
                paintScrollBarTrackBorder(context, g, x, y, w, h);
        }

        public void paintScrollBarTrackBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h, int orientation) {
            getPainter(context, "scrollbartrackborder", orientation).
                paintScrollBarTrackBorder(context, g, x, y, w, h, orientation);
        }

        public void paintScrollPaneBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "scrollpanebackground", -1).
                paintScrollPaneBackground(context, g, x, y, w, h);
        }

        public void paintScrollPaneBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "scrollpaneborder", -1).
                paintScrollPaneBorder(context, g, x, y, w, h);
        }

        public void paintSeparatorBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "separatorbackground", -1).
                paintSeparatorBackground(context, g, x, y, w, h);
        }

        public void paintSeparatorBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h, int orientation) {
            getPainter(context, "separatorbackground", orientation).
                paintSeparatorBackground(context, g, x, y, w, h, orientation);
        }

        public void paintSeparatorBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "separatorborder", -1).
                paintSeparatorBorder(context, g, x, y, w, h);
        }

        public void paintSeparatorBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h, int orientation) {
            getPainter(context, "separatorborder", orientation).
                paintSeparatorBorder(context, g, x, y, w, h, orientation);
        }

        public void paintSeparatorForeground(SynthContext context,
                     Graphics g, int x, int y, int w, int h, int direction) {
            getPainter(context, "separatorforeground", direction).
                paintSeparatorForeground(context, g, x, y, w, h, direction);
        }

        public void paintSliderBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "sliderbackground", -1).
                paintSliderBackground(context, g, x, y, w, h);
        }

        public void paintSliderBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h, int direction) {
            getPainter(context, "sliderbackground", direction).
            paintSliderBackground(context, g, x, y, w, h, direction);
        }

        public void paintSliderBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "sliderborder", -1).
                paintSliderBorder(context, g, x, y, w, h);
        }

        public void paintSliderBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h, int direction) {
            getPainter(context, "sliderborder", direction).
                paintSliderBorder(context, g, x, y, w, h, direction);
        }

        public void paintSliderThumbBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h, int direction) {
            getPainter(context, "sliderthumbbackground", direction).
                paintSliderThumbBackground(context, g, x, y, w, h, direction);
        }

        public void paintSliderThumbBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h, int direction) {
            getPainter(context, "sliderthumbborder", direction).
                paintSliderThumbBorder(context, g, x, y, w, h, direction);
        }

        public void paintSliderTrackBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "slidertrackbackground", -1).
                paintSliderTrackBackground(context, g, x, y, w, h);
        }

        public void paintSliderTrackBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h, int direction) {
            getPainter(context, "slidertrackbackground", direction).
                paintSliderTrackBackground(context, g, x, y, w, h, direction);
        }

        public void paintSliderTrackBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "slidertrackborder", -1).
                paintSliderTrackBorder(context, g, x, y, w, h);
        }

        public void paintSliderTrackBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h, int direction) {
            getPainter(context, "slidertrackborder", direction).
            paintSliderTrackBorder(context, g, x, y, w, h, direction);
        }

        public void paintSpinnerBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "spinnerbackground", -1).
                paintSpinnerBackground(context, g, x, y, w, h);
        }

        public void paintSpinnerBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "spinnerborder", -1).
                paintSpinnerBorder(context, g, x, y, w, h);
        }

        public void paintSplitPaneDividerBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "splitpanedividerbackground", -1).
                paintSplitPaneDividerBackground(context, g, x, y, w, h);
        }

        public void paintSplitPaneDividerBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h, int orientation) {
            getPainter(context, "splitpanedividerbackground", orientation).
            paintSplitPaneDividerBackground(context, g, x, y, w, h, orientation);
        }

        public void paintSplitPaneDividerForeground(SynthContext context,
                     Graphics g, int x, int y, int w, int h, int direction) {
            getPainter(context, "splitpanedividerforeground", direction).
                paintSplitPaneDividerForeground(context, g,
                                                x, y, w, h, direction);
        }

        public void paintSplitPaneDragDivider(SynthContext context,
                     Graphics g, int x, int y, int w, int h, int direction) {
            getPainter(context, "splitpanedragdivider", direction).
                paintSplitPaneDragDivider(context, g, x, y, w, h, direction);
        }

        public void paintSplitPaneBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "splitpanebackground", -1).
                paintSplitPaneBackground(context, g, x, y, w, h);
        }

        public void paintSplitPaneBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "splitpaneborder", -1).
                paintSplitPaneBorder(context, g, x, y, w, h);
        }

        public void paintTabbedPaneBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "tabbedpanebackground", -1).
                paintTabbedPaneBackground(context, g, x, y, w, h);
        }

        public void paintTabbedPaneBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "tabbedpaneborder", -1).
                paintTabbedPaneBorder(context, g, x, y, w, h);
        }

        public void paintTabbedPaneTabAreaBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "tabbedpanetabareabackground", -1).
                paintTabbedPaneTabAreaBackground(context, g, x, y, w, h);
        }

        public void paintTabbedPaneTabAreaBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h, int orientation) {
            getPainter(context, "tabbedpanetabareabackground", orientation).
                paintTabbedPaneTabAreaBackground(context, g, x, y, w, h,
                                                 orientation);
        }

        public void paintTabbedPaneTabAreaBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "tabbedpanetabareaborder", -1).
                paintTabbedPaneTabAreaBorder(context, g, x, y, w, h);
        }

        public void paintTabbedPaneTabAreaBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h, int orientation) {
            getPainter(context, "tabbedpanetabareaborder", orientation).
                paintTabbedPaneTabAreaBorder(context, g, x, y, w, h,
                                             orientation);
        }

        public void paintTabbedPaneTabBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h, int direction) {
            getPainter(context, "tabbedpanetabbackground", -1).
                paintTabbedPaneTabBackground(context, g, x, y, w, h, direction);
        }

        public void paintTabbedPaneTabBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h, int tabIndex,
                     int direction) {
            getPainter(context, "tabbedpanetabbackground", direction).
                paintTabbedPaneTabBackground(context, g, x, y, w, h, tabIndex,
                                             direction);
        }

        public void paintTabbedPaneTabBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h, int direction) {
            getPainter(context, "tabbedpanetabborder", -1).
                paintTabbedPaneTabBorder(context, g, x, y, w, h, direction);
        }

        public void paintTabbedPaneTabBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h, int tabIndex,
                     int direction) {
            getPainter(context, "tabbedpanetabborder", direction).
                paintTabbedPaneTabBorder(context, g, x, y, w, h, tabIndex,
                                         direction);
        }

        public void paintTabbedPaneContentBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "tabbedpanecontentbackground", -1).
                paintTabbedPaneContentBackground(context, g, x, y, w, h);
        }

        public void paintTabbedPaneContentBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "tabbedpanecontentborder", -1).
                paintTabbedPaneContentBorder(context, g, x, y, w, h);
        }

        public void paintTableHeaderBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "tableheaderbackground", -1).
                paintTableHeaderBackground(context, g, x, y, w, h);
        }

        public void paintTableHeaderBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "tableheaderborder", -1).
                paintTableHeaderBorder(context, g, x, y, w, h);
        }

        public void paintTableBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "tablebackground", -1).
                paintTableBackground(context, g, x, y, w, h);
        }

        public void paintTableBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "tableborder", -1).
                paintTableBorder(context, g, x, y, w, h);
        }

        public void paintTextAreaBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "textareabackground", -1).
                paintTextAreaBackground(context, g, x, y, w, h);
        }

        public void paintTextAreaBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "textareaborder", -1).
                paintTextAreaBorder(context, g, x, y, w, h);
        }

        public void paintTextPaneBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "textpanebackground", -1).
                paintTextPaneBackground(context, g, x, y, w, h);
        }

        public void paintTextPaneBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "textpaneborder", -1).
                paintTextPaneBorder(context, g, x, y, w, h);
        }

        public void paintTextFieldBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "textfieldbackground", -1).
                paintTextFieldBackground(context, g, x, y, w, h);
        }

        public void paintTextFieldBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "textfieldborder", -1).
                paintTextFieldBorder(context, g, x, y, w, h);
        }

        public void paintToggleButtonBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "togglebuttonbackground", -1).
                paintToggleButtonBackground(context, g, x, y, w, h);
        }

        public void paintToggleButtonBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "togglebuttonborder", -1).
                paintToggleButtonBorder(context, g, x, y, w, h);
        }

        public void paintToolBarBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "toolbarbackground", -1).
                paintToolBarBackground(context, g, x, y, w, h);
        }

        public void paintToolBarBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h, int orientation) {
            getPainter(context, "toolbarbackground", orientation).
                paintToolBarBackground(context, g, x, y, w, h, orientation);
        }

        public void paintToolBarBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "toolbarborder", -1).
                paintToolBarBorder(context, g, x, y, w, h);
        }

        public void paintToolBarBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h, int orientation) {
            getPainter(context, "toolbarborder", orientation).
                paintToolBarBorder(context, g, x, y, w, h, orientation);
        }

        public void paintToolBarContentBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "toolbarcontentbackground", -1).
                paintToolBarContentBackground(context, g, x, y, w, h);
        }

        public void paintToolBarContentBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h, int orientation) {
            getPainter(context, "toolbarcontentbackground", orientation).
                paintToolBarContentBackground(context, g, x, y, w, h, orientation);
        }

        public void paintToolBarContentBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "toolbarcontentborder", -1).
                paintToolBarContentBorder(context, g, x, y, w, h);
        }

        public void paintToolBarContentBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h, int orientation) {
            getPainter(context, "toolbarcontentborder", orientation).
                paintToolBarContentBorder(context, g, x, y, w, h, orientation);
        }

        public void paintToolBarDragWindowBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "toolbardragwindowbackground", -1).
                paintToolBarDragWindowBackground(context, g, x, y, w, h);
        }

        public void paintToolBarDragWindowBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h, int orientation) {
            getPainter(context, "toolbardragwindowbackground", orientation).
                paintToolBarDragWindowBackground(context, g, x, y, w, h, orientation);
        }

        public void paintToolBarDragWindowBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "toolbardragwindowborder", -1).
                paintToolBarDragWindowBorder(context, g, x, y, w, h);
        }

        public void paintToolBarDragWindowBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h, int orientation) {
            getPainter(context, "toolbardragwindowborder", orientation).
                paintToolBarDragWindowBorder(context, g, x, y, w, h, orientation);
        }

        public void paintToolTipBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "tooltipbackground", -1).
                paintToolTipBackground(context, g, x, y, w, h);
        }

        public void paintToolTipBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "tooltipborder", -1).
                paintToolTipBorder(context, g, x, y, w, h);
        }

        public void paintTreeBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "treebackground", -1).
                paintTreeBackground(context, g, x, y, w, h);
        }

        public void paintTreeBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "treeborder", -1).
                paintTreeBorder(context, g, x, y, w, h);
        }

        public void paintTreeCellBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "treecellbackground", -1).
                paintTreeCellBackground(context, g, x, y, w, h);
        }

        public void paintTreeCellBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "treecellborder", -1).
                paintTreeCellBorder(context, g, x, y, w, h);
        }

        public void paintTreeCellFocus(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "treecellfocus", -1).
                paintTreeCellFocus(context, g, x, y, w, h);
        }

        public void paintViewportBackground(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "viewportbackground", -1).
                paintViewportBackground(context, g, x, y, w, h);
        }

        public void paintViewportBorder(SynthContext context,
                     Graphics g, int x, int y, int w, int h) {
            getPainter(context, "viewportborder", -1).
                paintViewportBorder(context, g, x, y, w, h);
        }
    }
}
