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
package org.netbeans.jemmy.util;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;

import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import org.netbeans.jemmy.JemmyException;
import org.netbeans.jemmy.JemmyInputException;
import org.netbeans.jemmy.JemmyProperties;
import org.netbeans.jemmy.TimeoutExpiredException;
import org.netbeans.jemmy.operators.ComponentOperator;
import org.netbeans.jemmy.operators.JDialogOperator;
import org.netbeans.jemmy.operators.JInternalFrameOperator;
import org.netbeans.jemmy.operators.JScrollPaneOperator;
import org.netbeans.jemmy.operators.JTabbedPaneOperator;
import org.netbeans.jemmy.operators.Operator;
import org.netbeans.jemmy.operators.WindowOperator;
import org.netbeans.jemmy.operators.Operator.ComponentVisualizer;

/**
 *
 * Used as component visualizer by default.
 *
 * @see
 * org.netbeans.jemmy.operators.Operator#setVisualizer(Operator.ComponentVisualizer)
 * @see org.netbeans.jemmy.operators.Operator.ComponentVisualizer
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 *
 */
public class DefaultVisualizer implements ComponentVisualizer, Cloneable {

    private boolean window = true;
    private boolean internalFrame = true;
    private boolean scroll = false;
    private boolean switchTab = false;
    private boolean modal = false;

    public DefaultVisualizer() {
    }

    /**
     * Forces vizualizer to check that component is on the top modal dialog or
     * no modal dialog displayed.
     *
     * @param yesOrNo If true, JemmyInputException will be throught if component
     * is not on the top modal dialog and a modal dialog is dislayed.
     */
    public void checkForModal(boolean yesOrNo) {
        modal = yesOrNo;
    }

    /**
     * Informs that a window contained component should be activated.
     *
     * @param yesOrNo true if windows need to be activated.
     */
    public void activateWindow(boolean yesOrNo) {
        window = yesOrNo;
    }

    /**
     * Informs that an internal frame contained component should be activated.
     *
     * @param yesOrNo true if internal frames need to be activated.
     */
    public void activateInternalFrame(boolean yesOrNo) {
        internalFrame = yesOrNo;
    }

    /**
     * Informs that scrolling should be made.
     *
     * @param yesOrNo true if scroll panes need to be scrolled.
     */
    public void scroll(boolean yesOrNo) {
        scroll = yesOrNo;
    }

    /**
     * Informs that tab switching should be made.
     *
     * @param yesOrNo true if tabbed panes need to be switched.
     */
    public void switchTab(boolean yesOrNo) {
        switchTab = yesOrNo;
    }

    /**
     * Returns true if window is active.
     *
     * @param winOper an operator representing the window.
     * @return true is window is active.
     */
    protected boolean isWindowActive(WindowOperator winOper) {
        return winOper.isFocused() && winOper.isActive();
    }

    /**
     * Performs an atomic window-activization precedure. A window is sopposed to
     * be prepared for the activization (i.e. put "to front").
     *
     * @param winOper an operator representing the window.
     */
    protected void makeWindowActive(WindowOperator winOper) {
        winOper.activate();
    }

    /**
     * Activates a window. Uses makeWindowActive if necessary.
     *
     * @param winOper an operator representing the window.
     * @see #makeWindowActive
     */
    protected void activate(WindowOperator winOper) {
        boolean active = isWindowActive(winOper);
        winOper.toFront();
        if (!active) {
            makeWindowActive(winOper);
        }
    }

    /**
     * Inits an internal frame.
     *
     * @param intOper an operator representing the frame.
     */
    protected void initInternalFrame(JInternalFrameOperator intOper) {
        if (!intOper.isSelected()) {
            intOper.activate();
        }
    }

    /**
     * Scrolls JScrollPane to make the component visible.
     *
     * @param scrollOper an operator representing a scroll pane.
     * @param target a component - target to be made visible.
     */
    protected void scroll(JScrollPaneOperator scrollOper, Component target) {
        if (!scrollOper.checkInside(target)) {
            scrollOper.scrollToComponent(target);
        }
    }

    /**
     * Switches tabs to make the component visible.
     *
     * @param tabOper an operator representing a tabbed pane.
     * @param target a component - target to be made visible.
     */
    protected void switchTab(JTabbedPaneOperator tabOper, Component target) {
        int tabInd = 0;
        for (int j = 0; j < tabOper.getTabCount(); j++) {
            if (target == tabOper.getComponentAt(j)) {
                tabInd = j;
                break;
            }
        }
        if (tabOper.getSelectedIndex() != tabInd) {
            tabOper.selectPage(tabInd);
        }
    }

    /**
     * Prepares the component for user input.
     *
     * @param compOper an operator representing the component.
     * @throws JemmyInputException
     * @see #checkForModal(boolean)
     */
    @Override
    public void makeVisible(ComponentOperator compOper) {
        try {
            if (modal) {
                Dialog modalDialog = JDialogOperator.getTopModalDialog();
                if (modalDialog != null
                        && compOper.getWindow() != modalDialog) {
                    throw (new JemmyInputException("Component is not on top modal dialog.",
                            compOper.getSource()));
                }
            }
            WindowOperator winOper = new WindowOperator(compOper.getWindow());
            if (window) {
                winOper.copyEnvironment(compOper);
                winOper.setVisualizer(new EmptyVisualizer());
                activate(winOper);
            }
            if (internalFrame && compOper instanceof JInternalFrameOperator) {
                initInternalFrame((JInternalFrameOperator) compOper);
            }
            Container[] conts = compOper.getContainers();
            for (int i = conts.length - 1; i >= 0; i--) {
                if (internalFrame && conts[i] instanceof JInternalFrame) {
                    JInternalFrameOperator intOper = new JInternalFrameOperator((JInternalFrame) conts[i]);
                    intOper.copyEnvironment(compOper);
                    intOper.setVisualizer(new EmptyVisualizer());
                    initInternalFrame(intOper);
                } else if (scroll && conts[i] instanceof JScrollPane) {
                    JScrollPaneOperator scrollOper = new JScrollPaneOperator((JScrollPane) conts[i]);
                    scrollOper.copyEnvironment(compOper);
                    scrollOper.setVisualizer(new EmptyVisualizer());
                    scroll(scrollOper, compOper.getSource());
                } else if (switchTab && conts[i] instanceof JTabbedPane) {
                    JTabbedPaneOperator tabOper = new JTabbedPaneOperator((JTabbedPane) conts[i]);
                    tabOper.copyEnvironment(compOper);
                    tabOper.setVisualizer(new EmptyVisualizer());
                    switchTab(tabOper, i == 0 ? compOper.getSource() : conts[i - 1]);
                }
            }
        } catch (TimeoutExpiredException e) {
            JemmyProperties.getProperties().getOutput().printStackTrace(e);
        }
    }

    /**
     * Creates an exact copy of this visualizer.
     *
     * @return new instance.
     */
    public DefaultVisualizer cloneThis() {
        try {
            return (DefaultVisualizer) super.clone();
        } catch (CloneNotSupportedException e) {
            //that's impossible
            throw (new JemmyException("Even impossible happens :)", e));
        }
    }

}
