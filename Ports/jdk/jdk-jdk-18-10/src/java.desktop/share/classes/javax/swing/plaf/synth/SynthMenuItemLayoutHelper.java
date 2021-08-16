/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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

import sun.swing.StringUIClientPropertyKey;
import sun.swing.MenuItemLayoutHelper;

import javax.swing.*;
import javax.swing.text.View;
import java.awt.*;

/**
 * Calculates preferred size and layouts synth menu items.
 *
 * All JMenuItems (and JMenus) include enough space for the insets
 * plus one or more elements.  When we say "label" below, we mean
 * "icon and/or text."
 *
 * Cases to consider for SynthMenuItemUI (visualized here in a
 * LTR orientation; the RTL case would be reversed):
 *                   label
 *      check icon + label
 *      check icon + label + accelerator
 *                   label + accelerator
 *
 * Cases to consider for SynthMenuUI (again visualized here in a
 * LTR orientation):
 *                   label + arrow
 *
 * Note that in the above scenarios, accelerator and arrow icon are
 * mutually exclusive.  This means that if a popup menu contains a mix
 * of JMenus and JMenuItems, we only need to allow enough space for
 * max(maxAccelerator, maxArrow), and both accelerators and arrow icons
 * can occupy the same "column" of space in the menu.
 */
class SynthMenuItemLayoutHelper extends MenuItemLayoutHelper {

    public static final StringUIClientPropertyKey MAX_ACC_OR_ARROW_WIDTH =
            new StringUIClientPropertyKey("maxAccOrArrowWidth");

    public static final ColumnAlignment LTR_ALIGNMENT_1 =
            new ColumnAlignment(
                    SwingConstants.LEFT,
                    SwingConstants.LEFT,
                    SwingConstants.LEFT,
                    SwingConstants.RIGHT,
                    SwingConstants.RIGHT
            );
    public static final ColumnAlignment LTR_ALIGNMENT_2 =
            new ColumnAlignment(
                    SwingConstants.LEFT,
                    SwingConstants.LEFT,
                    SwingConstants.LEFT,
                    SwingConstants.LEFT,
                    SwingConstants.RIGHT
            );
    public static final ColumnAlignment RTL_ALIGNMENT_1 =
            new ColumnAlignment(
                    SwingConstants.RIGHT,
                    SwingConstants.RIGHT,
                    SwingConstants.RIGHT,
                    SwingConstants.LEFT,
                    SwingConstants.LEFT
            );
    public static final ColumnAlignment RTL_ALIGNMENT_2 =
            new ColumnAlignment(
                    SwingConstants.RIGHT,
                    SwingConstants.RIGHT,
                    SwingConstants.RIGHT,
                    SwingConstants.RIGHT,
                    SwingConstants.LEFT
            );

    private SynthContext context;
    private SynthContext accContext;
    private SynthStyle style;
    private SynthStyle accStyle;
    private SynthGraphicsUtils gu;
    private SynthGraphicsUtils accGu;
    private boolean alignAcceleratorText;
    private int maxAccOrArrowWidth;

    public SynthMenuItemLayoutHelper(SynthContext context, SynthContext accContext,
                                     JMenuItem mi, Icon checkIcon, Icon arrowIcon,
                                     Rectangle viewRect, int gap, String accDelimiter,
                                     boolean isLeftToRight, boolean useCheckAndArrow,
                                     String propertyPrefix) {
        this.context = context;
        this.accContext = accContext;
        this.style = context.getStyle();
        this.accStyle = accContext.getStyle();
        this.gu = style.getGraphicsUtils(context);
        this.accGu = accStyle.getGraphicsUtils(accContext);
        this.alignAcceleratorText = getAlignAcceleratorText(propertyPrefix);
        reset(mi, checkIcon, arrowIcon, viewRect, gap, accDelimiter,
              isLeftToRight, style.getFont(context), accStyle.getFont(accContext),
              useCheckAndArrow, propertyPrefix);
        setLeadingGap(0);
    }

    private boolean getAlignAcceleratorText(String propertyPrefix) {
        return style.getBoolean(context,
                propertyPrefix + ".alignAcceleratorText", true);
    }

    protected void calcWidthsAndHeights() {
        // iconRect
        if (getIcon() != null) {
            getIconSize().setWidth(SynthGraphicsUtils.getIconWidth(getIcon(), context));
            getIconSize().setHeight(SynthGraphicsUtils.getIconHeight(getIcon(), context));
        }

        // accRect
        if (!getAccText().isEmpty()) {
             getAccSize().setWidth(accGu.computeStringWidth(getAccContext(),
                    getAccFontMetrics().getFont(), getAccFontMetrics(),
                    getAccText()));
            getAccSize().setHeight(getAccFontMetrics().getHeight());
        }

        // textRect
        if (getText() == null) {
            setText("");
        } else if (!getText().isEmpty()) {
            if (getHtmlView() != null) {
                // Text is HTML
                getTextSize().setWidth(
                        (int) getHtmlView().getPreferredSpan(View.X_AXIS));
                getTextSize().setHeight(
                        (int) getHtmlView().getPreferredSpan(View.Y_AXIS));
            } else {
                // Text isn't HTML
                getTextSize().setWidth(gu.computeStringWidth(context,
                        getFontMetrics().getFont(), getFontMetrics(),
                        getText()));
                getTextSize().setHeight(getFontMetrics().getHeight());
            }
        }

        if (useCheckAndArrow()) {
            // checkIcon
            if (getCheckIcon() != null) {
                getCheckSize().setWidth(
                        SynthGraphicsUtils.getIconWidth(getCheckIcon(), context));
                getCheckSize().setHeight(
                        SynthGraphicsUtils.getIconHeight(getCheckIcon(), context));
            }
            // arrowRect
            if (getArrowIcon() != null) {
                getArrowSize().setWidth(
                        SynthGraphicsUtils.getIconWidth(getArrowIcon(), context));
                getArrowSize().setHeight(
                        SynthGraphicsUtils.getIconHeight(getArrowIcon(), context));
            }
        }

        // labelRect
        if (isColumnLayout()) {
            getLabelSize().setWidth(getIconSize().getWidth()
                    + getTextSize().getWidth() + getGap());
            getLabelSize().setHeight(MenuItemLayoutHelper.max(
                    getCheckSize().getHeight(),
                    getIconSize().getHeight(),
                    getTextSize().getHeight(),
                    getAccSize().getHeight(),
                    getArrowSize().getHeight()));
        } else {
            Rectangle textRect = new Rectangle();
            Rectangle iconRect = new Rectangle();
            gu.layoutText(context, getFontMetrics(), getText(), getIcon(),
                    getHorizontalAlignment(), getVerticalAlignment(),
                    getHorizontalTextPosition(), getVerticalTextPosition(),
                    getViewRect(), iconRect, textRect, getGap());
            textRect.width += getLeftTextExtraWidth();
            Rectangle labelRect = iconRect.union(textRect);
            getLabelSize().setHeight(labelRect.height);
            getLabelSize().setWidth(labelRect.width);
        }
    }

    protected void calcMaxWidths() {
        calcMaxWidth(getCheckSize(), MAX_CHECK_WIDTH);
        maxAccOrArrowWidth =
                calcMaxValue(MAX_ACC_OR_ARROW_WIDTH, getArrowSize().getWidth());
        maxAccOrArrowWidth =
                calcMaxValue(MAX_ACC_OR_ARROW_WIDTH, getAccSize().getWidth());

        if (isColumnLayout()) {
            calcMaxWidth(getIconSize(), MAX_ICON_WIDTH);
            calcMaxWidth(getTextSize(), MAX_TEXT_WIDTH);
            int curGap = getGap();
            if ((getIconSize().getMaxWidth() == 0)
                    || (getTextSize().getMaxWidth() == 0)) {
                curGap = 0;
            }
            getLabelSize().setMaxWidth(
                    calcMaxValue(MAX_LABEL_WIDTH, getIconSize().getMaxWidth()
                            + getTextSize().getMaxWidth() + curGap));
        } else {
            // We shouldn't use current icon and text widths
            // in maximal widths calculation for complex layout.
            getIconSize().setMaxWidth(getParentIntProperty(
                    MAX_ICON_WIDTH));
            calcMaxWidth(getLabelSize(), MAX_LABEL_WIDTH);
            // If maxLabelWidth is wider
            // than the widest icon + the widest text + gap,
            // we should update the maximal text witdh
            int candidateTextWidth = getLabelSize().getMaxWidth() -
                    getIconSize().getMaxWidth();
            if (getIconSize().getMaxWidth() > 0) {
                candidateTextWidth -= getGap();
            }
            getTextSize().setMaxWidth(calcMaxValue(
                    MAX_TEXT_WIDTH, candidateTextWidth));
        }
    }

    public SynthContext getContext() {
        return context;
    }

    public SynthContext getAccContext() {
        return accContext;
    }

    public SynthStyle getStyle() {
        return style;
    }

    public SynthStyle getAccStyle() {
        return accStyle;
    }

    public SynthGraphicsUtils getGraphicsUtils() {
        return gu;
    }

    public SynthGraphicsUtils getAccGraphicsUtils() {
        return accGu;
    }

    public boolean alignAcceleratorText() {
        return alignAcceleratorText;
    }

    public int getMaxAccOrArrowWidth() {
        return maxAccOrArrowWidth;
    }

    protected void prepareForLayout(LayoutResult lr) {
        lr.getCheckRect().width = getCheckSize().getMaxWidth();
        // An item can have an arrow or a check icon at once
        if (useCheckAndArrow() && (!"".equals(getAccText()))) {
            lr.getAccRect().width = maxAccOrArrowWidth;
        } else {
            lr.getArrowRect().width = maxAccOrArrowWidth;
        }
    }

    public ColumnAlignment getLTRColumnAlignment() {
        if (alignAcceleratorText()) {
            return LTR_ALIGNMENT_2;
        } else {
            return LTR_ALIGNMENT_1;
        }
    }

    public ColumnAlignment getRTLColumnAlignment() {
        if (alignAcceleratorText()) {
            return RTL_ALIGNMENT_2;
        } else {
            return RTL_ALIGNMENT_1;
        }
    }

    protected void layoutIconAndTextInLabelRect(LayoutResult lr) {
        lr.setTextRect(new Rectangle());
        lr.setIconRect(new Rectangle());
        gu.layoutText(context, getFontMetrics(), getText(), getIcon(),
                getHorizontalAlignment(), getVerticalAlignment(),
                getHorizontalTextPosition(), getVerticalTextPosition(),
                lr.getLabelRect(), lr.getIconRect(), lr.getTextRect(), getGap());
    }
}
