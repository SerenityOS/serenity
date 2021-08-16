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
package org.netbeans.jemmy.drivers.menus;

import java.awt.event.KeyEvent;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.MenuElement;

import org.netbeans.jemmy.JemmyException;
import org.netbeans.jemmy.Timeout;
import org.netbeans.jemmy.TimeoutExpiredException;
import org.netbeans.jemmy.drivers.DescriptablePathChooser;
import org.netbeans.jemmy.drivers.MenuDriver;
import org.netbeans.jemmy.drivers.PathChooser;
import org.netbeans.jemmy.drivers.input.RobotDriver;
import org.netbeans.jemmy.operators.ComponentOperator;
import org.netbeans.jemmy.operators.Operator;

/**
 *
 * @author shura
 */
public class AppleMenuDriver extends RobotDriver implements MenuDriver {

    /**
     * Creates a new instance of AppleMenuDriver
     */
    public AppleMenuDriver() {
        super(new Timeout("apple.system.menu.delay", 100),
                new String[]{"org.netbeans.jemmy.operators.JMenuBarOperator"});
    }

    @Override
    public Object pushMenu(ComponentOperator oper, PathChooser chooser) {
        Timeout maxTime = oper.getTimeouts().create("ComponentOperator.WaitComponentTimeout");
        JMenuBar bar = (JMenuBar) (oper.getSource());
        activateMenu(bar);
        MenuElement menuObject;
        maxTime.start();
        while (!chooser.checkPathComponent(0, (menuObject = getSelectedElement(bar)))) {
            pressKey(KeyEvent.VK_RIGHT, 0);
            releaseKey(KeyEvent.VK_RIGHT, 0);
            if (maxTime.expired()) {
                throw (new TimeoutExpiredException("AppleMenuDriver: can not find an appropriate menu!"));
            }
        }
        for (int depth = 1; depth < chooser.getDepth(); depth++) {
            // TODO - wait for menu item
            int elementIndex = getDesiredElementIndex(menuObject, chooser, depth);
            if (elementIndex == -1) {
                throw (new JemmyException("Unable to find menu (menuitem): " + ((DescriptablePathChooser) chooser).getDescription()));
            }
            for (int i = ((depth == 1) ? 0 : 1); i <= elementIndex; i++) {
                pressKey(KeyEvent.VK_DOWN, 0);
                releaseKey(KeyEvent.VK_DOWN, 0);
            }
            if (depth == chooser.getDepth() - 1) {
                pressKey(KeyEvent.VK_ENTER, 0);
                releaseKey(KeyEvent.VK_ENTER, 0);
                return null;
            } else {
                pressKey(KeyEvent.VK_RIGHT, 0);
                releaseKey(KeyEvent.VK_RIGHT, 0);
                menuObject = menuObject.getSubElements()[0].getSubElements()[elementIndex];
            }
        }
        return menuObject;
    }

    private void activateMenu(JMenuBar bar) {
        if (getSelectedElement(bar) == null) {
            tryToActivate();
            if (getSelectedElement(bar) == null) {
                tryToActivate();
            }
        }
    }

    private void tryToActivate() {
        moveMouse(0, 0);
        pressMouse(Operator.getDefaultMouseButton(), 0);
        releaseMouse(Operator.getDefaultMouseButton(), 0);
        pressKey(KeyEvent.VK_RIGHT, 0);
        releaseKey(KeyEvent.VK_RIGHT, 0);
        pressKey(KeyEvent.VK_RIGHT, 0);
        releaseKey(KeyEvent.VK_RIGHT, 0);
    }

    private static MenuElement getSelectedElement(MenuElement bar) {
        MenuElement[] subElements = bar.getSubElements();
        for (MenuElement subElement : subElements) {
            if (subElement instanceof JMenu
                    && ((JMenu) subElement).isSelected()) {
                return subElement;
            } else if (subElement instanceof JMenuItem
                    && ((JMenuItem) subElement).isSelected()) {
                return subElement;
            }
        }
        return null;
    }

    private static int getDesiredElementIndex(MenuElement bar, PathChooser chooser, int depth) {
        MenuElement[] subElements = bar.getSubElements()[0].getSubElements();
        int realIndex = 0;
        for (MenuElement subElement : subElements) {
            // do not count invisible and disabled menu items
            if (subElement instanceof JMenuItem && ((JMenuItem) subElement).isVisible() && ((JMenuItem) subElement).isEnabled()) {
                if (chooser.checkPathComponent(depth, subElement)) {
                    return realIndex;
                }
                realIndex++;
            }
        }
        return -1;
    }
}
