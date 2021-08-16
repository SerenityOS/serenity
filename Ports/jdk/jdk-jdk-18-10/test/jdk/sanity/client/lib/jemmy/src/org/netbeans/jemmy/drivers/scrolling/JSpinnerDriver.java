/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
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
package org.netbeans.jemmy.drivers.scrolling;

import javax.swing.SwingConstants;

import org.netbeans.jemmy.drivers.LightSupportiveDriver;
import org.netbeans.jemmy.drivers.ScrollDriver;
import org.netbeans.jemmy.operators.ComponentOperator;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JSpinnerOperator;

/**
 * A scroll driver serving JSpinner component.
 *
 * @author Alexandre Iline(alexandre.iline@oracle.com)
 */
public class JSpinnerDriver extends LightSupportiveDriver implements ScrollDriver {

    /**
     * Constructs a JSpinnerDriver object.
     */
    public JSpinnerDriver() {
        super(new String[]{"org.netbeans.jemmy.operators.JSpinnerOperator"});
    }

    @Override
    public void scrollToMinimum(final ComponentOperator oper, int orientation) {
        Object minimum = ((JSpinnerOperator) oper).getMinimum();
        if (minimum == null) {
            throw (new JSpinnerOperator.SpinnerModelException("Impossible to get a minimum of JSpinner model.", oper.getSource()));
        }
        scroll(oper, new ScrollAdjuster() {
            @Override
            public int getScrollOrientation() {
                return SwingConstants.VERTICAL;
            }

            @Override
            public String getDescription() {
                return "Spin to minimum";
            }

            @Override
            public int getScrollDirection() {
                if (((JSpinnerOperator) oper).getModel().getPreviousValue() != null) {
                    return ScrollAdjuster.DECREASE_SCROLL_DIRECTION;
                } else {
                    return ScrollAdjuster.DO_NOT_TOUCH_SCROLL_DIRECTION;
                }
            }

            @Override
            public String toString() {
                return "scrollToMinimum.ScrollAdjuster{description = " + getDescription() + '}';
            }
        });
    }

    @Override
    public void scrollToMaximum(final ComponentOperator oper, int orientation) {
        Object maximum = ((JSpinnerOperator) oper).getMaximum();
        if (maximum == null) {
            throw (new JSpinnerOperator.SpinnerModelException("Impossible to get a maximum of JSpinner model.", oper.getSource()));
        }
        scroll(oper, new ScrollAdjuster() {
            @Override
            public int getScrollOrientation() {
                return SwingConstants.VERTICAL;
            }

            @Override
            public String getDescription() {
                return "Spin to maximum";
            }

            @Override
            public int getScrollDirection() {
                if (((JSpinnerOperator) oper).getModel().getNextValue() != null) {
                    return ScrollAdjuster.INCREASE_SCROLL_DIRECTION;
                } else {
                    return ScrollAdjuster.DO_NOT_TOUCH_SCROLL_DIRECTION;
                }
            }

            @Override
            public String toString() {
                return "scrollToMaximum.ScrollAdjuster{description = " + getDescription() + '}';
            }
        });
    }

    @Override
    public void scroll(ComponentOperator oper, ScrollAdjuster adj) {
        JButtonOperator increaseButton = ((JSpinnerOperator) oper).getIncreaseOperator();
        JButtonOperator decreaseButton = ((JSpinnerOperator) oper).getDecreaseOperator();
        if (adj.getScrollDirection() == ScrollAdjuster.DO_NOT_TOUCH_SCROLL_DIRECTION) {
            return;
        }
        int originalDirection = adj.getScrollDirection();
        while (adj.getScrollDirection() == originalDirection) {
            if (originalDirection == ScrollAdjuster.INCREASE_SCROLL_DIRECTION) {
                increaseButton.push();
            } else {
                decreaseButton.push();
            }
        }
    }
}
