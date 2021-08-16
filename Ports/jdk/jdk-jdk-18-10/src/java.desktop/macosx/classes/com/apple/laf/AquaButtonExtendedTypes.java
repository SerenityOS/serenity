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

import java.awt.Insets;
import java.util.*;

import javax.swing.*;
import javax.swing.border.Border;

import apple.laf.JRSUIConstants.*;

import com.apple.laf.AquaUtilControlSize.*;
import com.apple.laf.AquaUtils.RecyclableSingleton;

import static apple.laf.JRSUIConstants.FOCUS_SIZE;

/**
 * All the "magic numbers" in this class should go away once
 * <rdar://problem/4613866> "default font" and sizes for controls in Java Aqua Look and Feel
 * has been addressed, and we can cull widget metrics from the native system.
 */
public class AquaButtonExtendedTypes {
    protected static Border getBorderForPosition(final AbstractButton c, final Object type, final Object logicalPosition) {
        final String name = (logicalPosition == null ? (String)type : type + "-" + getRealPositionForLogicalPosition((String)logicalPosition, c.getComponentOrientation().isLeftToRight()));
        final TypeSpecifier specifier = getSpecifierByName(name);
        if (specifier == null) return null;

        final Border border = specifier.getBorder();
        if (!(border instanceof AquaBorder)) return border;

        return ((AquaBorder)border).deriveBorderForSize(AquaUtilControlSize.getUserSizeFrom(c));
    }

    protected static String getRealPositionForLogicalPosition(String logicalPosition, boolean leftToRight) {
        if (!leftToRight) {
            if ("first".equalsIgnoreCase(logicalPosition)) return "last";
            if ("last".equalsIgnoreCase(logicalPosition)) return "first";
        }
        return logicalPosition;
    }

    abstract static class TypeSpecifier {
        final String name;
        final boolean setIconFont;

        TypeSpecifier(final String name, final boolean setIconFont) {
            this.name = name; this.setIconFont = setIconFont;
        }

        abstract Border getBorder();
    }

    static class BorderDefinedTypeSpecifier extends TypeSpecifier {
        final AquaBorder border;

        BorderDefinedTypeSpecifier(final String name, final Widget widget, final SizeVariant variant) {
            this(name, widget, variant, 0, 0, 0, 0);
        }

        BorderDefinedTypeSpecifier(final String name, final Widget widget, final SizeVariant variant, final int smallW, final int smallH, final int miniW, final int miniH) {
            super(name, false);
            border = initBorder(widget, new SizeDescriptor(variant) {
                public SizeVariant deriveSmall(final SizeVariant v) {
                    v.alterMinSize(smallW, smallH);
                    return super.deriveSmall(v);
                }
                public SizeVariant deriveMini(final SizeVariant v) {
                    v.alterMinSize(miniW, miniH);
                    return super.deriveMini(v);
                }
            });
            patchUp(border.sizeDescriptor);
        }

        Border getBorder() { return border; }
        void patchUp(final SizeDescriptor descriptor) {}

        AquaBorder initBorder(final Widget widget, final SizeDescriptor desc) {
            return new AquaButtonBorder.Named(widget, desc);
        }
    }

    static class SegmentedBorderDefinedTypeSpecifier extends BorderDefinedTypeSpecifier {
        public SegmentedBorderDefinedTypeSpecifier(final String name, final Widget widget, final SegmentPosition position, final SizeVariant variant) {
            this(name, widget, position, variant, 0, 0, 0, 0);
        }

        public SegmentedBorderDefinedTypeSpecifier(final String name, final Widget widget, final SegmentPosition position, final SizeVariant variant, final int smallW, final int smallH, final int miniW, final int miniH) {
            super(name, widget, variant, smallW, smallH, miniW, miniH);
            border.painter.state.set(SegmentTrailingSeparator.YES);
            border.painter.state.set(position);
        }

        AquaBorder initBorder(final Widget widget, final SizeDescriptor desc) {
            return new SegmentedNamedBorder(widget, desc);
        }
    }

    public static class SegmentedNamedBorder extends AquaButtonBorder.Named {
        public SegmentedNamedBorder(final SegmentedNamedBorder sizeDescriptor) {
            super(sizeDescriptor);
        }

        public SegmentedNamedBorder(final Widget widget, final SizeDescriptor sizeDescriptor) {
            super(widget, sizeDescriptor);
        }

        protected boolean isSelectionPressing() {
            return false;
        }
    }

    protected static TypeSpecifier getSpecifierByName(final String name) {
        return typeDefinitions.get().get(name);
    }

    private static final RecyclableSingleton<Map<String, TypeSpecifier>> typeDefinitions = new RecyclableSingleton<Map<String, TypeSpecifier>>() {
        protected Map<String, TypeSpecifier> getInstance() {
            return getAllTypes();
        }
    };

    protected static Map<String, TypeSpecifier> getAllTypes() {
        final Map<String, TypeSpecifier> specifiersByName = new HashMap<String, TypeSpecifier>();

        final Insets focusInsets = new Insets(FOCUS_SIZE, FOCUS_SIZE,
                                              FOCUS_SIZE, FOCUS_SIZE);

        final TypeSpecifier[] specifiers = {
            new TypeSpecifier("toolbar", true) {
                Border getBorder() { return AquaButtonBorder.getToolBarButtonBorder(); }
            },
            new TypeSpecifier("icon", true) {
                Border getBorder() { return AquaButtonBorder.getToggleButtonBorder(); }
            },
            new TypeSpecifier("text", false) {
                Border getBorder() { return UIManager.getBorder("Button.border"); }
            },
            new TypeSpecifier("toggle", false) {
                Border getBorder() { return AquaButtonBorder.getToggleButtonBorder(); }
            },
            new BorderDefinedTypeSpecifier("combobox", Widget.BUTTON_POP_DOWN, new SizeVariant().alterMargins(7, 10, 6, 30).alterInsets(1, 2, 0, 2).alterMinSize(0, 29), 0, -3, 0, -6) {
                void patchUp(final SizeDescriptor descriptor) { descriptor.small.alterMargins(0, 0, 0, -4); descriptor.mini.alterMargins(0, 0, 0, -6); }
            },
            new BorderDefinedTypeSpecifier("comboboxInternal", Widget.BUTTON_POP_DOWN, new SizeVariant().alterInsets(1, 2, 0, 2).alterMinSize(0, 29), 0, -3, 0, -6),
            new BorderDefinedTypeSpecifier("comboboxEndCap", Widget.BUTTON_COMBO_BOX, new SizeVariant().alterMargins(5, 10, 6, 10).alterInsets(1, 2, 0, 2).alterMinSize(0, 29), 0, -3, 0, -6){
                void patchUp(final SizeDescriptor descriptor) { border.painter.state.set(IndicatorOnly.YES); }
            },

            new BorderDefinedTypeSpecifier("square", Widget.BUTTON_BEVEL, new SizeVariant(16, 16).alterMargins(5, 7, 5, 7).replaceInsets(focusInsets)),
            new BorderDefinedTypeSpecifier("gradient", Widget.BUTTON_BEVEL_INSET, new SizeVariant(18, 18).alterMargins(8, 9, 8, 9).replaceInsets(focusInsets)) {
                void patchUp(SizeDescriptor descriptor) { descriptor.small.alterMargins(0, 0, 0, 0); }
            },
            new BorderDefinedTypeSpecifier("bevel", Widget.BUTTON_BEVEL_ROUND, new SizeVariant(22, 22).alterMargins(7, 8, 9, 8).alterInsets(0, 0, 0, 0)),

            new BorderDefinedTypeSpecifier("textured", Widget.BUTTON_PUSH_TEXTURED, new SizeVariant(28, 28).alterMargins(5, 10, 6, 10).alterInsets(1, 2, 0, 2)),
            new BorderDefinedTypeSpecifier("roundRect", Widget.BUTTON_PUSH_INSET, new SizeVariant(28, 28).alterMargins(4, 14, 4, 14).replaceInsets(focusInsets)),
            new BorderDefinedTypeSpecifier("recessed", Widget.BUTTON_PUSH_SCOPE, new SizeVariant(28, 28).alterMargins(4, 14, 4, 14).replaceInsets(focusInsets)),

            new BorderDefinedTypeSpecifier("well", Widget.FRAME_WELL, new SizeVariant(32, 32)),

            new BorderDefinedTypeSpecifier("help", Widget.BUTTON_ROUND_HELP, new SizeVariant().alterInsets(2, 0, 0, 0).alterMinSize(28, 28), -3, -3, -3, -3),
            new BorderDefinedTypeSpecifier("round", Widget.BUTTON_ROUND, new SizeVariant().alterInsets(2, 0, 0, 0).alterMinSize(28, 28), -3, -3, -3, -3),
            new BorderDefinedTypeSpecifier("texturedRound", Widget.BUTTON_ROUND_INSET, new SizeVariant().alterInsets(0, 0, 0, 0).alterMinSize(26, 26), -2, -2, 0, 0),

            new SegmentedBorderDefinedTypeSpecifier("segmented-first", Widget.BUTTON_SEGMENTED, SegmentPosition.FIRST, new SizeVariant().alterMargins(6, 16, 6, 10).alterInsets(2, 3, 2, 0).alterMinSize(0, 28), 0, -3, 0, -3),
            new SegmentedBorderDefinedTypeSpecifier("segmented-middle", Widget.BUTTON_SEGMENTED, SegmentPosition.MIDDLE, new SizeVariant().alterMargins(6, 9, 6, 10).alterInsets(2, 0, 2, 0).alterMinSize(0, 28), 0, -3, 0, -3),
            new SegmentedBorderDefinedTypeSpecifier("segmented-last", Widget.BUTTON_SEGMENTED, SegmentPosition.LAST, new SizeVariant().alterMargins(6, 9, 6, 16).alterInsets(2, 0, 2, 3).alterMinSize(0, 28), 0, -3, 0, -3),
            new SegmentedBorderDefinedTypeSpecifier("segmented-only", Widget.BUTTON_SEGMENTED, SegmentPosition.ONLY, new SizeVariant().alterMargins(6, 16, 6, 16).alterInsets(2, 3, 2, 3).alterMinSize(34, 28), 0, -3, 0, -3),

            new SegmentedBorderDefinedTypeSpecifier("segmentedRoundRect-first", Widget.BUTTON_SEGMENTED_INSET, SegmentPosition.FIRST, new SizeVariant().alterMargins(6, 12, 6, 8).alterInsets(2, 2, 2, 0).alterMinSize(0, 28), 0, -3, 0, -3),
            new SegmentedBorderDefinedTypeSpecifier("segmentedRoundRect-middle", Widget.BUTTON_SEGMENTED_INSET, SegmentPosition.MIDDLE, new SizeVariant().alterMargins(6, 8, 6, 8).alterInsets(2, 0, 2, 0).alterMinSize(0, 28), 0, -3, 0, -3),
            new SegmentedBorderDefinedTypeSpecifier("segmentedRoundRect-last", Widget.BUTTON_SEGMENTED_INSET, SegmentPosition.LAST, new SizeVariant().alterMargins(6, 8, 6, 12).alterInsets(2, 0, 2, 2).alterMinSize(0, 28), 0, -3, 0, -3),
            new SegmentedBorderDefinedTypeSpecifier("segmentedRoundRect-only", Widget.BUTTON_SEGMENTED_INSET, SegmentPosition.ONLY, new SizeVariant().alterMargins(6, 12, 6, 12).alterInsets(2, 2, 2, 2).alterMinSize(0, 28), 0, -3, 0, -3),

            new SegmentedBorderDefinedTypeSpecifier("segmentedTexturedRounded-first", Widget.BUTTON_SEGMENTED_SCURVE, SegmentPosition.FIRST, new SizeVariant().alterMargins(6, 12, 6, 8).alterInsets(2, 2, 2, 0).alterMinSize(0, 28), 0, -3, 0, -3),
            new SegmentedBorderDefinedTypeSpecifier("segmentedTexturedRounded-middle", Widget.BUTTON_SEGMENTED_SCURVE, SegmentPosition.MIDDLE, new SizeVariant().alterMargins(6, 8, 6, 8).alterInsets(2, 0, 2, 0).alterMinSize(0, 28), 0, -3, 0, -3),
            new SegmentedBorderDefinedTypeSpecifier("segmentedTexturedRounded-last", Widget.BUTTON_SEGMENTED_SCURVE, SegmentPosition.LAST, new SizeVariant().alterMargins(6, 8, 6, 12).alterInsets(2, 0, 2, 2).alterMinSize(0, 28), 0, -3, 0, -3),
            new SegmentedBorderDefinedTypeSpecifier("segmentedTexturedRounded-only", Widget.BUTTON_SEGMENTED_SCURVE, SegmentPosition.ONLY, new SizeVariant().alterMargins(6, 12, 6, 12).alterInsets(2, 2, 2, 2).alterMinSize(0, 28), 0, -3, 0, -3),

            new SegmentedBorderDefinedTypeSpecifier("segmentedTextured-first", Widget.BUTTON_SEGMENTED_TEXTURED, SegmentPosition.FIRST, new SizeVariant().alterMargins(6, 12, 6, 8).alterInsets(2, 3, 2, 0).alterMinSize(0, 28), 0, -3, 0, -3),
            new SegmentedBorderDefinedTypeSpecifier("segmentedTextured-middle", Widget.BUTTON_SEGMENTED_TEXTURED, SegmentPosition.MIDDLE, new SizeVariant().alterMargins(6, 8, 6, 8).alterInsets(2, 0, 2, 0).alterMinSize(0, 28), 0, -3, 0, -3),
            new SegmentedBorderDefinedTypeSpecifier("segmentedTextured-last", Widget.BUTTON_SEGMENTED_TEXTURED, SegmentPosition.LAST, new SizeVariant().alterMargins(6, 8, 6, 12).alterInsets(2, 0, 2, 3).alterMinSize(0, 28), 0, -3, 0, -3),
            new SegmentedBorderDefinedTypeSpecifier("segmentedTextured-only", Widget.BUTTON_SEGMENTED_TEXTURED, SegmentPosition.ONLY, new SizeVariant().alterMargins(6, 12, 6, 12).alterInsets(2, 3, 2, 3).alterMinSize(0, 28), 0, -3, 0, -3),

            new SegmentedBorderDefinedTypeSpecifier("segmentedCapsule-first", Widget.BUTTON_SEGMENTED_TOOLBAR, SegmentPosition.FIRST, new SizeVariant().alterMargins(6, 12, 6, 8).alterInsets(2, 2, 2, 0).alterMinSize(0, 28), 0, 0, 0, 0),
            new SegmentedBorderDefinedTypeSpecifier("segmentedCapsule-middle", Widget.BUTTON_SEGMENTED_TOOLBAR, SegmentPosition.MIDDLE, new SizeVariant().alterMargins(6, 8, 6, 8).alterInsets(2, 0, 2, 0).alterMinSize(0, 28), 0, 0, 0, 0),
            new SegmentedBorderDefinedTypeSpecifier("segmentedCapsule-last", Widget.BUTTON_SEGMENTED_TOOLBAR, SegmentPosition.LAST, new SizeVariant().alterMargins(6, 8, 6, 12).alterInsets(2, 0, 2, 2).alterMinSize(0, 28), 0, 0, 0, 0),
            new SegmentedBorderDefinedTypeSpecifier("segmentedCapsule-only", Widget.BUTTON_SEGMENTED_TOOLBAR, SegmentPosition.ONLY, new SizeVariant().alterMargins(6, 12, 6, 12).alterInsets(2, 2, 2, 2).alterMinSize(34, 28), 0, 0, 0, 0),

            new BorderDefinedTypeSpecifier("segmentedGradient-first", Widget.BUTTON_BEVEL_INSET, new SizeVariant(18, 18).alterMargins(4, 5, 4, 5).replaceInsets(new Insets(-2,-0,-2,-0))),
            new BorderDefinedTypeSpecifier("segmentedGradient-middle", Widget.BUTTON_BEVEL_INSET, new SizeVariant(18, 18).alterMargins(4, 5, 4, 5).replaceInsets(new Insets(-2,-1,-2,-0))),
            new BorderDefinedTypeSpecifier("segmentedGradient-last", Widget.BUTTON_BEVEL_INSET, new SizeVariant(18, 18).alterMargins(4, 5, 4, 5).replaceInsets(new Insets(-2,-1,-2,-0))),
            new BorderDefinedTypeSpecifier("segmentedGradient-only", Widget.BUTTON_BEVEL_INSET, new SizeVariant(18, 18).alterMargins(4, 5, 4, 5).replaceInsets(new Insets(-2,-1,-2,-1))),

            new BorderDefinedTypeSpecifier("disclosure", Widget.BUTTON_DISCLOSURE, new SizeVariant().alterMargins(10, 10, 10, 10).replaceInsets(focusInsets).alterMinSize(27, 27), -1, -1, -1, -1),

            //new BorderDefinedTypeSpecifier("disclosureTriangle", false, Widget.DISCLOSURE_TRIANGLE, new SizeVariant()),
            new BorderDefinedTypeSpecifier("scrollColumnSizer", Widget.SCROLL_COLUMN_SIZER, new SizeVariant(14, 14)),
        };

        for (final TypeSpecifier specifier : specifiers) {
            specifiersByName.put(specifier.name, specifier);
        }

        return specifiersByName;
    }
}
