/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

package apple.laf;

import apple.laf.JRSUIConstants.*;
import apple.laf.JRSUIState.*;

public class JRSUIStateFactory {
    public static JRSUIState getSliderTrack() {
        return new JRSUIState(Widget.SLIDER.apply(NoIndicator.YES.apply(0)));
    }

    public static JRSUIState getSliderThumb() {
        return new JRSUIState(Widget.SLIDER_THUMB.apply(0));
    }

    public static JRSUIState getSpinnerArrows() {
        return new JRSUIState(Widget.BUTTON_LITTLE_ARROWS.apply(0));
    }

    public static JRSUIState getSplitPaneDivider() {
        return new JRSUIState(Widget.DIVIDER_SPLITTER.apply(0));
    }

    public static JRSUIState getTab() {
        return new JRSUIState(Widget.TAB.apply(SegmentTrailingSeparator.YES.apply(0)));
    }

    public static AnimationFrameState getDisclosureTriangle() {
        return new AnimationFrameState(Widget.DISCLOSURE_TRIANGLE.apply(0), 0);
    }

    public static ScrollBarState getScrollBar() {
        return new ScrollBarState(Widget.SCROLL_BAR.apply(0), 0, 0, 0);
    }

    public static TitleBarHeightState getTitleBar() {
        return new TitleBarHeightState(Widget.WINDOW_FRAME.apply(0), 0);
    }

    public static ValueState getProgressBar() {
        return new ValueState(0, 0);
    }

    public static ValueState getLabeledButton() {
        return new ValueState(0, 0);
    }
}
