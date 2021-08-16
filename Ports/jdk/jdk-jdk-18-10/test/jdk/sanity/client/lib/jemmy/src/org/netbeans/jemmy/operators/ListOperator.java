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
import java.awt.Dimension;
import java.awt.List;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.util.Hashtable;

import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.Outputable;
import org.netbeans.jemmy.TestOut;
import org.netbeans.jemmy.TimeoutExpiredException;
import org.netbeans.jemmy.drivers.DriverManager;
import org.netbeans.jemmy.drivers.MultiSelListDriver;

/**
 * <BR><BR>Timeouts used: <BR>
 * ComponentOperator.WaitComponentTimeout - time to wait component displayed
 * <BR>
 * ComponentOperator.WaitComponentEnabledTimeout - time to wait component
 * enabled <BR>.
 *
 * @see org.netbeans.jemmy.Timeouts
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 *
 */
public class ListOperator extends ComponentOperator
        implements Outputable {

    /**
     * Identifier for a "item" properties.
     *
     * @see #getDump
     */
    public static final String ITEM_PREFIX_DPROP = "Item";

    /**
     * Identifier for a "selected item" property.
     *
     * @see #getDump
     */
    public static final String SELECTED_ITEM_PREFIX_DPROP = "SelectedItem";

    private TestOut output;
    private MultiSelListDriver driver;

    /**
     * Constructor.
     *
     * @param b a component
     */
    public ListOperator(List b) {
        super(b);
        driver = DriverManager.getMultiSelListDriver(getClass());
    }

    /**
     * Constructs a ListOperator object.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     * @param index an index between appropriate ones.
     */
    public ListOperator(ContainerOperator<?> cont, ComponentChooser chooser, int index) {
        this((List) cont.
                waitSubComponent(new ListFinder(chooser),
                        index));
        copyEnvironment(cont);
    }

    /**
     * Constructs a ListOperator object.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     */
    public ListOperator(ContainerOperator<?> cont, ComponentChooser chooser) {
        this(cont, chooser, 0);
    }

    /**
     * Constructor. Waits item text first. Uses cont's timeout and output for
     * waiting and to init operator.
     *
     * @param cont a container
     * @param text Text of item which is currently selected.
     * @param itemIndex Item index.
     * @param index Ordinal component index.
     * @throws TimeoutExpiredException
     */
    public ListOperator(ContainerOperator<?> cont, String text, int itemIndex, int index) {
        this((List) waitComponent(cont,
                new ListByItemFinder(text, itemIndex,
                        cont.getComparator()),
                index));
        copyEnvironment(cont);
    }

    /**
     * Constructor. Waits component by selected item text first. Uses cont's
     * timeout and output for waiting and to init operator.
     *
     * @param cont a container
     * @param text Text of item which is currently selected.
     * @param index Ordinal component index.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public ListOperator(ContainerOperator<?> cont, String text, int index) {
        this(cont, text, -1, index);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont a container
     * @param text Text of item which is currently selected.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public ListOperator(ContainerOperator<?> cont, String text) {
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
    public ListOperator(ContainerOperator<?> cont, int index) {
        this((List) waitComponent(cont,
                new ListFinder(),
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
    public ListOperator(ContainerOperator<?> cont) {
        this(cont, 0);
    }

    /**
     * Searches List in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @param index Ordinal component index.
     * @return List instance or null if component was not found.
     */
    public static List findList(Container cont, ComponentChooser chooser, int index) {
        return (List) findComponent(cont, new ListFinder(chooser), index);
    }

    /**
     * Searches 0'th List in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @return List instance or null if component was not found.
     */
    public static List findList(Container cont, ComponentChooser chooser) {
        return findList(cont, chooser, 0);
    }

    @Override
    public void setOutput(TestOut output) {
        super.setOutput(output.createErrorOutput());
        this.output = output;
    }

    @Override
    public TestOut getOutput() {
        return output;
    }

    @Override
    public void copyEnvironment(Operator anotherOperator) {
        super.copyEnvironment(anotherOperator);
        driver
                = (MultiSelListDriver) DriverManager.
                getDriver(DriverManager.MULTISELLIST_DRIVER_ID,
                        getClass(),
                        anotherOperator.getProperties());
    }

    private int findItemIndex(String item, StringComparator comparator, int index) {
        int count = 0;
        for (int i = 0; i < getItemCount(); i++) {
            if (comparator.equals(getItem(i), item)) {
                if (count == index) {
                    return i;
                } else {
                    count++;
                }
            }
        }
        return -1;
    }

    /**
     * Searches an item index.
     *
     * @param item item text.
     * @param index an ordinal index between appropriate ones.
     * @return an index.
     */
    public int findItemIndex(String item, int index) {
        return findItemIndex(item, getComparator(), index);
    }

    /**
     * Searches an item index.
     *
     * @param item item text.
     * @return an index.
     */
    public int findItemIndex(String item) {
        return findItemIndex(item, 0);
    }

    private void selectItem(String item, StringComparator comparator, int index) {
        selectItem(findItemIndex(item, comparator, index));
    }

    /**
     * Selects an item.
     *
     * @param item item text.
     * @param index an ordinal index between appropriate ones.
     */
    public void selectItem(String item, int index) {
        selectItem(item, getComparator(), index);
    }

    /**
     * Selects an item.
     *
     * @param item item text.
     */
    public void selectItem(String item) {
        selectItem(item, 0);
    }

    /**
     * Selects an item.
     *
     * @param index an item index.
     */
    public void selectItem(int index) {
        output.printLine("Select " + Integer.toString(index) + "`th item in list\n    : "
                + toStringSource());
        output.printGolden("Select " + Integer.toString(index) + "`th item in list");
        driver.selectItem(this, index);
        if (getVerification()) {
            waitItemSelection(index, true);
        }
    }

    /**
     * Selects some items.
     *
     * @param from start selection index.
     * @param to end selection index.
     */
    public void selectItems(int from, int to) {
        output.printLine("Select items from " + Integer.toString(from)
                + "`th to " + Integer.toString(from) + "'th in list\n    : "
                + toStringSource());
        output.printGolden("Select items from " + Integer.toString(from)
                + "`th to " + Integer.toString(from) + "'th");
        driver.selectItems(this, new int[]{from, to});
        if (getVerification()) {
            waitItemsSelection(from, to, true);
        }
    }

    /**
     * Waits for items to be selected.
     *
     * @param from Start selection inex
     * @param to End selection inex
     * @param selected Selected (true) or unselected (false).
     */
    public void waitItemsSelection(final int from, final int to, final boolean selected) {
        getOutput().printLine("Wait items to be "
                + (selected ? "" : "un") + "selected in component \n    : "
                + toStringSource());
        getOutput().printGolden("Wait items to be "
                + (selected ? "" : "un") + "selected");
        waitState(new ComponentChooser() {
            @Override
            public boolean checkComponent(Component comp) {
                int[] indices = getSelectedIndexes();
                for (int indice : indices) {
                    if (indice < from
                            || indice > to) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public String getDescription() {
                return ("Items has been "
                        + (selected ? "" : "un") + "selected");
            }

            @Override
            public String toString() {
                return "ListOperator.waitItemsSelection.ComponentChooser{description = " + getDescription() + '}';
            }
        });
    }

    /**
     * Waits for item to be selected.
     *
     * @param itemIndex an item index to be selected.
     * @param selected Selected (true) or unselected (false).
     */
    public void waitItemSelection(final int itemIndex, final boolean selected) {
        waitItemsSelection(itemIndex, itemIndex, selected);
    }

    @Override
    public Hashtable<String, Object> getDump() {
        Hashtable<String, Object> result = super.getDump();
        addToDump(result, ITEM_PREFIX_DPROP, ((List) getSource()).getItems());
        addToDump(result, SELECTED_ITEM_PREFIX_DPROP, ((List) getSource()).getSelectedItems());
        return result;
    }

    ////////////////////////////////////////////////////////
    //Mapping                                             //
    /**
     * Maps {@code List.addActionListener(ActionListener)} through queue
     */
    public void addActionListener(final ActionListener actionListener) {
        runMapping(new MapVoidAction("addActionListener") {
            @Override
            public void map() {
                ((List) getSource()).addActionListener(actionListener);
            }
        });
    }

    /**
     * Maps {@code List.addItemListener(ItemListener)} through queue
     */
    public void addItemListener(final ItemListener itemListener) {
        runMapping(new MapVoidAction("addItemListener") {
            @Override
            public void map() {
                ((List) getSource()).addItemListener(itemListener);
            }
        });
    }

    /**
     * Maps {@code List.deselect(int)} through queue
     */
    public void deselect(final int i) {
        runMapping(new MapVoidAction("deselect") {
            @Override
            public void map() {
                ((List) getSource()).deselect(i);
            }
        });
    }

    /**
     * Maps {@code List.getItem(int)} through queue
     */
    public String getItem(final int i) {
        return (runMapping(new MapAction<String>("getItem") {
            @Override
            public String map() {
                return ((List) getSource()).getItem(i);
            }
        }));
    }

    /**
     * Maps {@code List.getItemCount()} through queue
     */
    public int getItemCount() {
        return (runMapping(new MapIntegerAction("getItemCount") {
            @Override
            public int map() {
                return ((List) getSource()).getItemCount();
            }
        }));
    }

    /**
     * Maps {@code List.getItems()} through queue
     */
    public String[] getItems() {
        return ((String[]) runMapping(new MapAction<Object>("getItems") {
            @Override
            public Object map() {
                return ((List) getSource()).getItems();
            }
        }));
    }

    /**
     * Maps {@code List.getMinimumSize(int)} through queue
     */
    public Dimension getMinimumSize(final int i) {
        return (runMapping(new MapAction<Dimension>("getMinimumSize") {
            @Override
            public Dimension map() {
                return ((List) getSource()).getMinimumSize(i);
            }
        }));
    }

    /**
     * Maps {@code List.getPreferredSize(int)} through queue
     */
    public Dimension getPreferredSize(final int i) {
        return (runMapping(new MapAction<Dimension>("getPreferredSize") {
            @Override
            public Dimension map() {
                return ((List) getSource()).getPreferredSize(i);
            }
        }));
    }

    /**
     * Maps {@code List.getRows()} through queue
     */
    public int getRows() {
        return (runMapping(new MapIntegerAction("getRows") {
            @Override
            public int map() {
                return ((List) getSource()).getRows();
            }
        }));
    }

    /**
     * Maps {@code List.getSelectedIndex()} through queue
     */
    public int getSelectedIndex() {
        return (runMapping(new MapIntegerAction("getSelectedIndex") {
            @Override
            public int map() {
                return ((List) getSource()).getSelectedIndex();
            }
        }));
    }

    /**
     * Maps {@code List.getSelectedIndexes()} through queue
     */
    public int[] getSelectedIndexes() {
        return ((int[]) runMapping(new MapAction<Object>("getSelectedIndexes") {
            @Override
            public Object map() {
                return ((List) getSource()).getSelectedIndexes();
            }
        }));
    }

    /**
     * Maps {@code List.getSelectedItem()} through queue
     */
    public String getSelectedItem() {
        return (runMapping(new MapAction<String>("getSelectedItem") {
            @Override
            public String map() {
                return ((List) getSource()).getSelectedItem();
            }
        }));
    }

    /**
     * Maps {@code List.getSelectedItems()} through queue
     */
    public String[] getSelectedItems() {
        return ((String[]) runMapping(new MapAction<Object>("getSelectedItems") {
            @Override
            public Object map() {
                return ((List) getSource()).getSelectedItems();
            }
        }));
    }

    /**
     * Maps {@code List.getSelectedObjects()} through queue
     */
    public Object[] getSelectedObjects() {
        return ((Object[]) runMapping(new MapAction<Object>("getSelectedObjects") {
            @Override
            public Object map() {
                return ((List) getSource()).getSelectedObjects();
            }
        }));
    }

    /**
     * Maps {@code List.getVisibleIndex()} through queue
     */
    public int getVisibleIndex() {
        return (runMapping(new MapIntegerAction("getVisibleIndex") {
            @Override
            public int map() {
                return ((List) getSource()).getVisibleIndex();
            }
        }));
    }

    /**
     * Maps {@code List.isIndexSelected(int)} through queue
     */
    public boolean isIndexSelected(final int i) {
        return (runMapping(new MapBooleanAction("isIndexSelected") {
            @Override
            public boolean map() {
                return ((List) getSource()).isIndexSelected(i);
            }
        }));
    }

    /**
     * Maps {@code List.isMultipleMode()} through queue
     */
    public boolean isMultipleMode() {
        return (runMapping(new MapBooleanAction("isMultipleMode") {
            @Override
            public boolean map() {
                return ((List) getSource()).isMultipleMode();
            }
        }));
    }

    /**
     * Maps {@code List.makeVisible(int)} through queue
     */
    public void makeVisible(final int i) {
        runMapping(new MapVoidAction("makeVisible") {
            @Override
            public void map() {
                ((List) getSource()).makeVisible(i);
            }
        });
    }

    /**
     * Maps {@code List.remove(int)} through queue
     */
    public void remove(final int i) {
        runMapping(new MapVoidAction("remove") {
            @Override
            public void map() {
                ((List) getSource()).remove(i);
            }
        });
    }

    /**
     * Maps {@code List.remove(String)} through queue
     */
    public void remove(final String string) {
        runMapping(new MapVoidAction("remove") {
            @Override
            public void map() {
                ((List) getSource()).remove(string);
            }
        });
    }

    /**
     * Maps {@code List.removeActionListener(ActionListener)} through queue
     */
    public void removeActionListener(final ActionListener actionListener) {
        runMapping(new MapVoidAction("removeActionListener") {
            @Override
            public void map() {
                ((List) getSource()).removeActionListener(actionListener);
            }
        });
    }

    /**
     * Maps {@code List.removeAll()} through queue
     */
    public void removeAll() {
        runMapping(new MapVoidAction("removeAll") {
            @Override
            public void map() {
                ((List) getSource()).removeAll();
            }
        });
    }

    /**
     * Maps {@code List.removeItemListener(ItemListener)} through queue
     */
    public void removeItemListener(final ItemListener itemListener) {
        runMapping(new MapVoidAction("removeItemListener") {
            @Override
            public void map() {
                ((List) getSource()).removeItemListener(itemListener);
            }
        });
    }

    /**
     * Maps {@code List.replaceItem(String, int)} through queue
     */
    public void replaceItem(final String string, final int i) {
        runMapping(new MapVoidAction("replaceItem") {
            @Override
            public void map() {
                ((List) getSource()).replaceItem(string, i);
            }
        });
    }

    /**
     * Maps {@code List.select(int)} through queue
     */
    public void select(final int i) {
        runMapping(new MapVoidAction("select") {
            @Override
            public void map() {
                ((List) getSource()).select(i);
            }
        });
    }

    /**
     * Maps {@code List.setMultipleMode(boolean)} through queue
     */
    public void setMultipleMode(final boolean b) {
        runMapping(new MapVoidAction("setMultipleMode") {
            @Override
            public void map() {
                ((List) getSource()).setMultipleMode(b);
            }
        });
    }

    //End of mapping                                      //
    ////////////////////////////////////////////////////////
    /**
     * Allows to find component by item text.
     */
    public static class ListByItemFinder implements ComponentChooser {

        String label;
        int itemIndex;
        StringComparator comparator;

        /**
         * Constructs ListByItemFinder.
         *
         * @param lb a text pattern
         * @param ii item index to check. If equal to -1, selected item is
         * checked.
         * @param comparator specifies string comparision algorithm.
         */
        public ListByItemFinder(String lb, int ii, StringComparator comparator) {
            label = lb;
            itemIndex = ii;
            this.comparator = comparator;
        }

        /**
         * Constructs ListByItemFinder.
         *
         * @param lb a text pattern
         * @param ii item index to check. If equal to -1, selected item is
         * checked.
         */
        public ListByItemFinder(String lb, int ii) {
            this(lb, ii, Operator.getDefaultStringComparator());
        }

        @Override
        public boolean checkComponent(Component comp) {
            if (comp instanceof List) {
                if (label == null) {
                    return true;
                }
                if (((List) comp).getItemCount() > itemIndex) {
                    int ii = itemIndex;
                    if (ii == -1) {
                        ii = ((List) comp).getSelectedIndex();
                        if (ii == -1) {
                            return false;
                        }
                    }
                    return (comparator.equals(((List) comp).getItem(ii),
                            label));
                }
            }
            return false;
        }

        @Override
        public String getDescription() {
            return ("List with text \"" + label + "\" in "
                    + itemIndex + "'th item");
        }

        @Override
        public String toString() {
            return "ListByItemFinder{" + "label=" + label + ", itemIndex=" + itemIndex + ", comparator=" + comparator + '}';
        }
    }

    /**
     * Checks component type.
     */
    public static class ListFinder extends Finder {

        /**
         * Constructs ListFinder.
         *
         * @param sf other searching criteria.
         */
        public ListFinder(ComponentChooser sf) {
            super(List.class, sf);
        }

        /**
         * Constructs ListFinder.
         */
        public ListFinder() {
            super(List.class);
        }
    }
}
