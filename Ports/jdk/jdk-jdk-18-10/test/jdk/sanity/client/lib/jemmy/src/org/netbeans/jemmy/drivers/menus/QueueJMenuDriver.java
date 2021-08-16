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

import java.awt.Component;
import java.awt.Window;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.MenuElement;

import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.JemmyException;
import org.netbeans.jemmy.QueueTool;
import org.netbeans.jemmy.Waitable;
import org.netbeans.jemmy.Waiter;
import org.netbeans.jemmy.drivers.DescriptablePathChooser;
import org.netbeans.jemmy.drivers.DriverManager;
import org.netbeans.jemmy.drivers.LightSupportiveDriver;
import org.netbeans.jemmy.drivers.MenuDriver;
import org.netbeans.jemmy.drivers.PathChooser;
import org.netbeans.jemmy.operators.ComponentOperator;
import org.netbeans.jemmy.operators.JMenuBarOperator;
import org.netbeans.jemmy.operators.JMenuItemOperator;
import org.netbeans.jemmy.operators.JMenuOperator;
import org.netbeans.jemmy.operators.JPopupMenuOperator;

/**
 *
 * 100% stable menu driver. Tries to do next steps during one action executed
 * through EventQueue:<br>
 * find showing window containing popup<br>
 * find showing popup<br>
 * find showing menuitem<br>
 * enter mouse into it<br>
 *
 * Repeats this action as many times as "JMenuOperator.WaitPopupTimeout" timeout
 * allows.
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 *
 */
public class QueueJMenuDriver extends LightSupportiveDriver implements MenuDriver {

    QueueTool queueTool;

    public QueueJMenuDriver() {
        super(new String[]{"org.netbeans.jemmy.operators.JMenuOperator",
            "org.netbeans.jemmy.operators.JMenuBarOperator",
            "org.netbeans.jemmy.operators.JPopupMenuOperator"});
        queueTool = new QueueTool();
    }

    @Override
    public Object pushMenu(final ComponentOperator oper, PathChooser chooser) {
        queueTool.setOutput(oper.getOutput().createErrorOutput());
        checkSupported(oper);
        JMenuItem result;
        OneReleaseAction action;
        if (oper instanceof JMenuBarOperator) {
            action = new OneReleaseAction(chooser, 0, oper, false) {
                @Override
                protected void pushAlone(JMenuItemOperator subMenuOper) {
                    if (subMenuOper.getSource() instanceof JMenu
                            && isMenuBarSelected((JMenuBar) oper.getSource())) {
                        DriverManager.getMouseDriver(subMenuOper).enterMouse(subMenuOper);
                    } else {
                        DriverManager.getButtonDriver(subMenuOper).push(subMenuOper);
                    }
                }

                @Override
                protected boolean inTheMiddle(JMenuOperator subMenuOper, boolean mousePressed) {
                    if (isMenuBarSelected((JMenuBar) oper.getSource())) {
                        DriverManager.getMouseDriver(subMenuOper).enterMouse(subMenuOper);
                        return false;
                    } else {
                        return super.inTheMiddle(subMenuOper, mousePressed);
                    }
                }

                @Override
                protected void process(MenuElement element) {
                    super.process(element);
                }

                @Override
                public MenuElement getMenuElement() {
                    return (MenuElement) oper.getSource();
                }
            };
        } else if (oper instanceof JPopupMenuOperator) {
            action = new OneReleaseAction(chooser, 0, oper, false) {
                @Override
                public MenuElement getMenuElement() {
                    return (MenuElement) oper.getSource();
                }
            };
        } else {
            DriverManager.getButtonDriver(oper).press(oper);
            action = new OneReleaseAction(chooser, 0, oper, false) {
                @Override
                public MenuElement launch() {
                    process((MenuElement) oper.getSource());
                    return (MenuElement) oper.getSource();
                }

                @Override
                public MenuElement getMenuElement() {
                    return null;
                }
            };
        }
        queueTool.waitEmpty(10);
        queueTool.waitEmpty(10);
        queueTool.waitEmpty(10);
        result = runAction(action, oper,
                oper.getTimeouts().getTimeout("ComponentOperator.WaitComponentTimeout"),
                (chooser instanceof DescriptablePathChooser)
                        ? ((DescriptablePathChooser) chooser).getDescription()
                        : "Menu pushing");
        if (result instanceof JMenu) {
            for (int i = 1; i < chooser.getDepth(); i++) {
                final JMenu menu = (JMenu) result;
                final ComponentChooser popupChooser = new PopupMenuChooser(menu);
                action = new OneReleaseAction(chooser, i, oper, action.mousePressed) {
                    @Override
                    public MenuElement getMenuElement() {
                        Window win = JPopupMenuOperator.findJPopupWindow(popupChooser);
                        if (win != null && win.isShowing()) {
                            return JPopupMenuOperator.findJPopupMenu(win, popupChooser);
                        } else {
                            return null;
                        }
                    }
                };
                result = runAction(action, oper,
                        oper.getTimeouts().getTimeout("JMenuOperator.WaitPopupTimeout"),
                        (chooser instanceof DescriptablePathChooser)
                                ? ((DescriptablePathChooser) chooser).getDescription()
                                : "Menu pushing");
            }
        }
        return result;
    }

    private JMenuItem runAction(final OneReleaseAction action, ComponentOperator env, long waitingTime, final String description) {
        Waiter<MenuElement, Void> waiter = new Waiter<>(new Waitable<MenuElement, Void>() {
            @Override
            public MenuElement actionProduced(Void param) {
                return queueTool.invokeSmoothly(action);
            }

            @Override
            public String getDescription() {
                return description;
            }

            @Override
            public String toString() {
                return "runAction.Waiter{description = " + getDescription() + '}';
            }
        });
        waiter.setOutput(env.getOutput().createErrorOutput());
        waiter.setTimeouts(env.getTimeouts().cloneThis());
        waiter.getTimeouts().setTimeout("Waiter.WaitingTime",
                waitingTime);
        waiter.getTimeouts().setTimeout("Waiter.TimeDelta", 100);
        //1.5 workaround
        if (System.getProperty("java.specification.version").compareTo("1.4") > 0) {
            queueTool.setOutput(env.getOutput().createErrorOutput());
            queueTool.waitEmpty(10);
            queueTool.waitEmpty(10);
            queueTool.waitEmpty(10);
        }
        //end of 1.5 workaround
        try {
            return (JMenuItem) waiter.waitAction(null);
        } catch (InterruptedException e) {
            action.stop();
            throw (new JemmyException("Waiting has been interrupted", e));
        }
    }

    private boolean isMenuBarSelected(JMenuBar bar) {
        MenuElement[] subElements = bar.getSubElements();
        for (MenuElement subElement : subElements) {
            if (subElement instanceof JMenu
                    && ((JMenu) subElement).isPopupMenuVisible()) {
                return true;
            }
        }
        return false;
    }

    private abstract class OneReleaseAction extends QueueTool.QueueAction<MenuElement> {

        PathChooser chooser;
        int depth;
        ComponentOperator env;
        boolean mousePressed = false;
        private boolean stopped = false;

        public OneReleaseAction(PathChooser chooser, int depth, ComponentOperator env, boolean mousePressed) {
            super("Menu pushing");
            this.chooser = chooser;
            this.depth = depth;
            this.env = env;
            this.mousePressed = mousePressed;
        }

        protected void pushAlone(JMenuItemOperator subMenuOper) {
            DriverManager.getButtonDriver(subMenuOper).push(subMenuOper);
        }

        protected void pushLast(JMenuItemOperator subMenuOper, boolean mousePressed) {
            DriverManager.getMouseDriver(subMenuOper).enterMouse(subMenuOper);
            DriverManager.getButtonDriver(subMenuOper).release(subMenuOper);
        }

        protected boolean inTheMiddle(JMenuOperator subMenuOper, boolean mousePressed) {
            if (!subMenuOper.isPopupMenuVisible()) {
                if (!mousePressed) {
                    DriverManager.getMouseDriver(subMenuOper).enterMouse(subMenuOper);
                    DriverManager.getButtonDriver(subMenuOper).press(subMenuOper);
                } else {
                    DriverManager.getMouseDriver(subMenuOper).enterMouse(subMenuOper);
                }
                return true;
            }
            return mousePressed;
        }

        protected void process(MenuElement element) {
            if (depth == chooser.getDepth() - 1) {
                JMenuItemOperator subMenuOper = new JMenuItemOperator((JMenuItem) element);
                subMenuOper.copyEnvironment(env);
                if (depth == 0) {
                    pushAlone(subMenuOper);
                } else {
                    pushLast(subMenuOper, mousePressed);
                }
            } else if (element instanceof JMenu) {
                JMenuOperator subMenuOper = new JMenuOperator((JMenu) element);
                subMenuOper.copyEnvironment(env);
                mousePressed = inTheMiddle(subMenuOper, mousePressed);
            } else {
                throw (new JemmyException("Menu path too long"));
            }
        }

        @Override
        public MenuElement launch() {
            MenuElement element = getMenuElement();
            if (element != null) {
                MenuElement[] subElements = element.getSubElements();
                for (MenuElement subElement : subElements) {
                    if (((Component) subElement).isShowing()
                            && ((Component) subElement).isEnabled()
                            && chooser.checkPathComponent(depth, subElement)) {
                        process(subElement);
                        return subElement;
                    }
                    if (stopped) {
                        return null;
                    }
                }
            }
            return null;
        }

        public abstract MenuElement getMenuElement();

        private void stop() {
            stopped = true;
        }
    }

    private static class PopupMenuChooser implements ComponentChooser {

        JMenu menu;

        public PopupMenuChooser(JMenu menu) {
            this.menu = menu;
        }

        @Override
        public boolean checkComponent(Component comp) {
            return (comp == menu.getPopupMenu()
                    && comp.isShowing() && comp.isEnabled());
        }

        @Override
        public String getDescription() {
            return menu.getText() + "'s popup";
        }

        @Override
        public String toString() {
            return "PopupMenuChooser{" + "menu=" + menu + '}';
        }
    }

}
