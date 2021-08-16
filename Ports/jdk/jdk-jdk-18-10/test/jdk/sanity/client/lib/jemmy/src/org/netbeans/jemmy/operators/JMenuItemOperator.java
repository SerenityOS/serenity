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
package org.netbeans.jemmy.operators;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Hashtable;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.event.MenuDragMouseEvent;
import javax.swing.event.MenuDragMouseListener;
import javax.swing.event.MenuKeyEvent;
import javax.swing.event.MenuKeyListener;
import javax.swing.plaf.MenuItemUI;

import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.JemmyProperties;
import org.netbeans.jemmy.Outputable;
import org.netbeans.jemmy.TestOut;
import org.netbeans.jemmy.TimeoutExpiredException;
import org.netbeans.jemmy.Timeoutable;
import org.netbeans.jemmy.Timeouts;
import org.netbeans.jemmy.util.EmptyVisualizer;

/**
 *
 * <BR><BR>Timeouts used: <BR>
 * JMenuItemOperator.PushMenuTimeout - time between button pressing and
 * releasing<BR>
 * ComponentOperator.WaitComponentTimeout - time to wait button displayed <BR>
 * ComponentOperator.WaitComponentEnabledTimeout - time to wait button enabled
 * <BR>.
 *
 * @see org.netbeans.jemmy.Timeouts
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 *
 */
public class JMenuItemOperator extends AbstractButtonOperator
        implements Timeoutable, Outputable {

    private final static long PUSH_MENU_TIMEOUT = 0;

    private Timeouts timeouts;
    private TestOut output;

    /**
     * Constructor.
     *
     * @param item a component
     */
    public JMenuItemOperator(JMenuItem item) {
        super(item);
        setTimeouts(JemmyProperties.getProperties().getTimeouts());
        setOutput(JemmyProperties.getProperties().getOutput());
    }

    /**
     * Constructs a JMenuItemOperator object.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     * @param index an index between appropriate ones.
     */
    public JMenuItemOperator(ContainerOperator<?> cont, ComponentChooser chooser, int index) {
        this((JMenuItem) cont.
                waitSubComponent(new JMenuItemFinder(chooser),
                        index));
        copyEnvironment(cont);
    }

    /**
     * Constructs a JMenuItemOperator object.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     */
    public JMenuItemOperator(ContainerOperator<?> cont, ComponentChooser chooser) {
        this(cont, chooser, 0);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont a container
     * @param text Button text.
     * @param index Ordinal component index.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public JMenuItemOperator(ContainerOperator<?> cont, String text, int index) {
        this((JMenuItem) waitComponent(cont,
                new JMenuItemByLabelFinder(text,
                        cont.getComparator()),
                index));
        setTimeouts(cont.getTimeouts());
        setOutput(cont.getOutput());
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont a container
     * @param text Button text.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public JMenuItemOperator(ContainerOperator<?> cont, String text) {
        this(cont, text, 0);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont a container
     * @param index Ordinal component index.
     * @throws TimeoutExpiredException
     */
    public JMenuItemOperator(ContainerOperator<?> cont, int index) {
        this((JMenuItem) waitComponent(cont,
                new JMenuItemFinder(),
                index));
        copyEnvironment(cont);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont a container
     * @throws TimeoutExpiredException
     */
    public JMenuItemOperator(ContainerOperator<?> cont) {
        this(cont, 0);
    }

    /**
     * Searches JMenuItem in container.
     *
     * @param menu Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @param index Ordinal component index.
     * @return JMenuItem instance or null if component was not found.
     */
    public static JMenuItem findJMenuItem(Container menu, ComponentChooser chooser, int index) {
        return (JMenuItem) findComponent(menu, new JMenuItemFinder(chooser), index);
    }

    /**
     * Searches 0'th JMenuItem in container.
     *
     * @param menu Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @return JMenuItem instance or null if component was not found.
     */
    public static JMenuItem findJMenuItem(Container menu, ComponentChooser chooser) {
        return findJMenuItem(menu, chooser, 0);
    }

    /**
     * Searches JMenuItem by text.
     *
     * @param menu Container to search component in.
     * @param text Button text. If null, contents is not checked.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @param index Ordinal component index.
     * @return JMenuItem instance or null if component was not found.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     */
    public static JMenuItem findJMenuItem(Container menu, String text, boolean ce, boolean ccs, int index) {
        return (findJMenuItem(menu,
                new JMenuItemByLabelFinder(text,
                        new DefaultStringComparator(ce,
                                ccs)),
                index));
    }

    /**
     * Searches JMenuItem by text.
     *
     * @param menu Container to search component in.
     * @param text Button text. If null, contents is not checked.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @return JMenuItem instance or null if component was not found.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     */
    public static JMenuItem findJMenuItem(Container menu, String text, boolean ce, boolean ccs) {
        return findJMenuItem(menu, text, ce, ccs, 0);
    }

    /**
     * Waits JMenuItem in container.
     *
     * @param menu Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @param index Ordinal component index.
     * @return JMenuItem instance.
     * @throws TimeoutExpiredException
     */
    public static JMenuItem waitJMenuItem(Container menu, ComponentChooser chooser, int index) {
        return (JMenuItem) waitComponent(menu, new JMenuItemFinder(chooser), index);
    }

    /**
     * Waits 0'th JMenuItem in container.
     *
     * @param menu Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @return JMenuItem instance.
     * @throws TimeoutExpiredException
     */
    public static JMenuItem waitJMenuItem(Container menu, ComponentChooser chooser) {
        return waitJMenuItem(menu, chooser, 0);
    }

    /**
     * Waits JMenuItem by text.
     *
     * @param menu Container to search component in.
     * @param text Button text. If null, contents is not checked.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @param index Ordinal component index.
     * @return JMenuItem instance.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public static JMenuItem waitJMenuItem(Container menu, String text, boolean ce, boolean ccs, int index) {
        return (waitJMenuItem(menu,
                new JMenuItemByLabelFinder(text,
                        new DefaultStringComparator(ce, ccs)),
                index));
    }

    /**
     * Waits JMenuItem by text.
     *
     * @param menu Container to search component in.
     * @param text Button text. If null, contents is not checked.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @return JMenuItem instance.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public static JMenuItem waitJMenuItem(Container menu, String text, boolean ce, boolean ccs) {
        return waitJMenuItem(menu, text, ce, ccs, 0);
    }

    static {
        Timeouts.initDefault("JMenuItemOperator.PushMenuTimeout", PUSH_MENU_TIMEOUT);
    }

    @Override
    public void setTimeouts(Timeouts timeouts) {
        super.setTimeouts(timeouts);
        this.timeouts = timeouts;
    }

    @Override
    public Timeouts getTimeouts() {
        return timeouts;
    }

    @Override
    public void setOutput(TestOut out) {
        super.setOutput(out);
        output = out;
    }

    @Override
    public TestOut getOutput() {
        return output;
    }

    @Override
    public Hashtable<String, Object> getDump() {
        Hashtable<String, Object> result = super.getDump();
        result.remove(AbstractButtonOperator.IS_SELECTED_DPROP);
        return result;
    }

    /**
     * Push this menu item.
     */
    @Override
    public void push() {
        setVisualizer(new EmptyVisualizer());
        super.push();
    }

    /**
     * Push this menu item and no block further execution.
     */
    @Override
    public void pushNoBlock() {
        setVisualizer(new EmptyVisualizer());
        super.pushNoBlock();
    }

    ////////////////////////////////////////////////////////
    //Mapping                                             //
    /**
     * Maps
     * {@code JMenuItem.addMenuDragMouseListener(MenuDragMouseListener)}
     * through queue
     */
    public void addMenuDragMouseListener(final MenuDragMouseListener menuDragMouseListener) {
        runMapping(new MapVoidAction("addMenuDragMouseListener") {
            @Override
            public void map() {
                ((JMenuItem) getSource()).addMenuDragMouseListener(menuDragMouseListener);
            }
        });
    }

    /**
     * Maps {@code JMenuItem.addMenuKeyListener(MenuKeyListener)} through queue
     */
    public void addMenuKeyListener(final MenuKeyListener menuKeyListener) {
        runMapping(new MapVoidAction("addMenuKeyListener") {
            @Override
            public void map() {
                ((JMenuItem) getSource()).addMenuKeyListener(menuKeyListener);
            }
        });
    }

    /**
     * Maps {@code JMenuItem.getAccelerator()} through queue
     */
    public KeyStroke getAccelerator() {
        return (runMapping(new MapAction<KeyStroke>("getAccelerator") {
            @Override
            public KeyStroke map() {
                return ((JMenuItem) getSource()).getAccelerator();
            }
        }));
    }

    /**
     * Maps {@code JMenuItem.getComponent()} through queue
     */
    public Component getComponent() {
        return (runMapping(new MapAction<Component>("getComponent") {
            @Override
            public Component map() {
                return ((JMenuItem) getSource()).getComponent();
            }
        }));
    }

    /**
     * Maps {@code JMenuItem.getSubElements()} through queue
     */
    public MenuElement[] getSubElements() {
        return ((MenuElement[]) runMapping(new MapAction<Object>("getSubElements") {
            @Override
            public Object map() {
                return ((JMenuItem) getSource()).getSubElements();
            }
        }));
    }

    /**
     * Maps {@code JMenuItem.isArmed()} through queue
     */
    public boolean isArmed() {
        return (runMapping(new MapBooleanAction("isArmed") {
            @Override
            public boolean map() {
                return ((JMenuItem) getSource()).isArmed();
            }
        }));
    }

    /**
     * Maps {@code JMenuItem.menuSelectionChanged(boolean)} through queue
     */
    public void menuSelectionChanged(final boolean b) {
        runMapping(new MapVoidAction("menuSelectionChanged") {
            @Override
            public void map() {
                ((JMenuItem) getSource()).menuSelectionChanged(b);
            }
        });
    }

    /**
     * Maps
     * {@code JMenuItem.processKeyEvent(KeyEvent, MenuElement[], MenuSelectionManager)}
     * through queue
     */
    public void processKeyEvent(final KeyEvent keyEvent, final MenuElement[] menuElement, final MenuSelectionManager menuSelectionManager) {
        runMapping(new MapVoidAction("processKeyEvent") {
            @Override
            public void map() {
                ((JMenuItem) getSource()).processKeyEvent(keyEvent, menuElement, menuSelectionManager);
            }
        });
    }

    /**
     * Maps {@code JMenuItem.processMenuDragMouseEvent(MenuDragMouseEvent)}
     * through queue
     */
    public void processMenuDragMouseEvent(final MenuDragMouseEvent menuDragMouseEvent) {
        runMapping(new MapVoidAction("processMenuDragMouseEvent") {
            @Override
            public void map() {
                ((JMenuItem) getSource()).processMenuDragMouseEvent(menuDragMouseEvent);
            }
        });
    }

    /**
     * Maps {@code JMenuItem.processMenuKeyEvent(MenuKeyEvent)} through queue
     */
    public void processMenuKeyEvent(final MenuKeyEvent menuKeyEvent) {
        runMapping(new MapVoidAction("processMenuKeyEvent") {
            @Override
            public void map() {
                ((JMenuItem) getSource()).processMenuKeyEvent(menuKeyEvent);
            }
        });
    }

    /**
     * Maps
     * {@code JMenuItem.processMouseEvent(MouseEvent, MenuElement[], MenuSelectionManager)}
     * through queue
     */
    public void processMouseEvent(final MouseEvent mouseEvent, final MenuElement[] menuElement, final MenuSelectionManager menuSelectionManager) {
        runMapping(new MapVoidAction("processMouseEvent") {
            @Override
            public void map() {
                ((JMenuItem) getSource()).processMouseEvent(mouseEvent, menuElement, menuSelectionManager);
            }
        });
    }

    /**
     * Maps
     * {@code JMenuItem.removeMenuDragMouseListener(MenuDragMouseListener)}
     * through queue
     */
    public void removeMenuDragMouseListener(final MenuDragMouseListener menuDragMouseListener) {
        runMapping(new MapVoidAction("removeMenuDragMouseListener") {
            @Override
            public void map() {
                ((JMenuItem) getSource()).removeMenuDragMouseListener(menuDragMouseListener);
            }
        });
    }

    /**
     * Maps {@code JMenuItem.removeMenuKeyListener(MenuKeyListener)}
     * through queue
     */
    public void removeMenuKeyListener(final MenuKeyListener menuKeyListener) {
        runMapping(new MapVoidAction("removeMenuKeyListener") {
            @Override
            public void map() {
                ((JMenuItem) getSource()).removeMenuKeyListener(menuKeyListener);
            }
        });
    }

    /**
     * Maps {@code JMenuItem.setAccelerator(KeyStroke)} through queue
     */
    public void setAccelerator(final KeyStroke keyStroke) {
        runMapping(new MapVoidAction("setAccelerator") {
            @Override
            public void map() {
                ((JMenuItem) getSource()).setAccelerator(keyStroke);
            }
        });
    }

    /**
     * Maps {@code JMenuItem.setArmed(boolean)} through queue
     */
    public void setArmed(final boolean b) {
        runMapping(new MapVoidAction("setArmed") {
            @Override
            public void map() {
                ((JMenuItem) getSource()).setArmed(b);
            }
        });
    }

    /**
     * Maps {@code JMenuItem.setUI(MenuItemUI)} through queue
     */
    public void setUI(final MenuItemUI menuItemUI) {
        runMapping(new MapVoidAction("setUI") {
            @Override
            public void map() {
                ((JMenuItem) getSource()).setUI(menuItemUI);
            }
        });
    }

    //End of mapping                                      //
    ////////////////////////////////////////////////////////
    /**
     * Prepares the button to click.
     */
    protected void prepareToClick() {
        output.printLine("Push menu item\n    :" + toStringSource());
        output.printGolden("Push menu item");
        Timeouts times = timeouts.cloneThis();
        times.setTimeout("AbstractButtonOperator.PushButtonTimeout",
                timeouts.getTimeout("JMenuItemOperator.PushMenuTimeout"));
        super.setTimeouts(times);
        super.setOutput(output.createErrorOutput());
    }

    static JMenuItemOperator[] getMenuItems(Object[] elements, Operator env) {
        int size = 0;
        for (Object element1 : elements) {
            if (element1 instanceof JMenuItem) {
                size++;
            }
        }
        JMenuItemOperator[] result = new JMenuItemOperator[size];
        int index = 0;
        for (Object element : elements) {
            if (element instanceof JMenuItem) {
                result[index] = new JMenuItemOperator((JMenuItem) element);
                result[index].copyEnvironment(env);
                index++;
            }
        }
        return result;
    }

    static JMenuItemOperator[] getMenuItems(MenuElement parent, Operator env) {
        return getMenuItems(parent.getSubElements(), env);
    }

    static JMenuItemOperator[] getMenuItems(JMenu parent, Operator env) {
        return getMenuItems(parent.getMenuComponents(), env);
    }

    static ComponentChooser[] createChoosers(String[] names, StringComparator comparator) {
        ComponentChooser[] choosers = new ComponentChooser[names.length];
        for (int i = 0; i < choosers.length; i++) {
            choosers[i] = new JMenuItemOperator.JMenuItemByLabelFinder(names[i], comparator);
        }
        return choosers;
    }

    /**
     * Allows to find component by text.
     */
    public static class JMenuItemByLabelFinder implements ComponentChooser {

        String label;
        StringComparator comparator;

        /**
         * Constructs JMenuItemByLabelFinder.
         *
         * @param lb a text pattern
         * @param comparator specifies string comparision algorithm.
         */
        public JMenuItemByLabelFinder(String lb, StringComparator comparator) {
            label = lb;
            this.comparator = comparator;
        }

        /**
         * Constructs JMenuItemByLabelFinder.
         *
         * @param lb a text pattern
         */
        public JMenuItemByLabelFinder(String lb) {
            this(lb, Operator.getDefaultStringComparator());
        }

        @Override
        public boolean checkComponent(Component comp) {
            if (comp instanceof JMenuItem) {
                if (((JMenuItem) comp).getText() != null) {
                    return (comparator.equals(((JMenuItem) comp).getText(),
                            label));
                }
            }
            return false;
        }

        @Override
        public String getDescription() {
            return "JMenuItem with text \"" + label + "\"";
        }

        @Override
        public String toString() {
            return "JMenuItemByLabelFinder{" + "label=" + label + ", comparator=" + comparator + '}';
        }
    }

    /**
     * Checks component type.
     */
    public static class JMenuItemFinder extends Finder {

        /**
         * Constructs JMenuItemFinder.
         *
         * @param sf other searching criteria.
         */
        public JMenuItemFinder(ComponentChooser sf) {
            super(JMenuItem.class, sf);
        }

        /**
         * Constructs JMenuItemFinder.
         */
        public JMenuItemFinder() {
            super(JMenuItem.class);
        }
    }
}
