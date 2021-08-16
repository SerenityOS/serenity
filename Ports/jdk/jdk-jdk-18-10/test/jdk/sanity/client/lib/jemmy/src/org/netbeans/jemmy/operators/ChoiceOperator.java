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

import java.awt.Choice;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ItemListener;
import java.util.Hashtable;

import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.JemmyException;
import org.netbeans.jemmy.Outputable;
import org.netbeans.jemmy.TestOut;
import org.netbeans.jemmy.TimeoutExpiredException;
import org.netbeans.jemmy.drivers.DriverManager;
import org.netbeans.jemmy.drivers.ListDriver;

/**
 *
 * <BR><BR>Timeouts used: <BR>
 * ButtonOperator.PushButtonTimeout - time between choice pressing and
 * releasing<BR>
 * ComponentOperator.WaitComponentTimeout - time to wait choice displayed <BR>
 * ComponentOperator.WaitComponentEnabledTimeout - time to wait choice enabled
 * <BR>.
 *
 * @see org.netbeans.jemmy.Timeouts
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 *
 */
public class ChoiceOperator extends ComponentOperator implements Outputable {

    /**
     * Identifier for a selected item property.
     *
     * @see #getDump
     */
    public static final String SELECTED_ITEM_DPROP = "Selected item";

    /**
     * Identifier for a items properties.
     *
     * @see #getDump
     */
    public static final String ITEM_PREFIX_DPROP = "Item";

    private TestOut output;
    private ListDriver driver;

    /**
     * Constructor.
     *
     * @param b a component
     */
    public ChoiceOperator(Choice b) {
        super(b);
        driver = DriverManager.getListDriver(getClass());
    }

    /**
     * Constructs a ChoiceOperator object.
     *
     * @param cont container
     * @param chooser a component chooser specifying searching criteria.
     * @param index an index between appropriate ones.
     */
    public ChoiceOperator(ContainerOperator<?> cont, ComponentChooser chooser, int index) {
        this((Choice) cont.
                waitSubComponent(new ChoiceFinder(chooser),
                        index));
        copyEnvironment(cont);
    }

    /**
     * Constructs a ChoiceOperator object.
     *
     * @param cont container
     * @param chooser a component chooser specifying searching criteria.
     */
    public ChoiceOperator(ContainerOperator<?> cont, ComponentChooser chooser) {
        this(cont, chooser, 0);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont container
     * @param text Choice text.
     * @param index Ordinal component index.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public ChoiceOperator(ContainerOperator<?> cont, String text, int index) {
        this((Choice) waitComponent(cont,
                new ChoiceBySelectedItemFinder(text,
                        cont.getComparator()),
                index));
        copyEnvironment(cont);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont container
     * @param text Choice text.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public ChoiceOperator(ContainerOperator<?> cont, String text) {
        this(cont, text, 0);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont container
     * @param index Ordinal component index.
     * @throws TimeoutExpiredException
     */
    public ChoiceOperator(ContainerOperator<?> cont, int index) {
        this((Choice) waitComponent(cont,
                new ChoiceFinder(),
                index));
        copyEnvironment(cont);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont container
     * @throws TimeoutExpiredException
     */
    public ChoiceOperator(ContainerOperator<?> cont) {
        this(cont, 0);
    }

    /**
     * Searches Choice in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @param index Ordinal component index.
     * @return Choice instance or null if component was not found.
     */
    public static Choice findChoice(Container cont, ComponentChooser chooser, int index) {
        return (Choice) findComponent(cont, new ChoiceFinder(chooser), index);
    }

    /**
     * Searches 0'th Choice in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @return Choice instance or null if component was not found.
     */
    public static Choice findChoice(Container cont, ComponentChooser chooser) {
        return findChoice(cont, chooser, 0);
    }

    /**
     * Searches Choice by text.
     *
     * @param cont Container to search component in.
     * @param text Choice text. If null, contents is not checked.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @param index Ordinal component index.
     * @return Choice instance or null if component was not found.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     */
    public static Choice findChoice(Container cont, String text, boolean ce, boolean ccs, int index) {
        return (findChoice(cont,
                new ChoiceBySelectedItemFinder(text,
                        new DefaultStringComparator(ce, ccs)),
                index));
    }

    /**
     * Searches Choice by text.
     *
     * @param cont Container to search component in.
     * @param text Choice text. If null, contents is not checked.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @return Choice instance or null if component was not found.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     */
    public static Choice findChoice(Container cont, String text, boolean ce, boolean ccs) {
        return findChoice(cont, text, ce, ccs, 0);
    }

    /**
     * Waits Choice in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @param index Ordinal component index.
     * @return Choice instance.
     * @throws TimeoutExpiredException
     */
    public static Choice waitChoice(Container cont, ComponentChooser chooser, int index) {
        return (Choice) waitComponent(cont, new ChoiceFinder(chooser), index);
    }

    /**
     * Waits 0'th Choice in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @return Choice instance.
     * @throws TimeoutExpiredException
     */
    public static Choice waitChoice(Container cont, ComponentChooser chooser) {
        return waitChoice(cont, chooser, 0);
    }

    /**
     * Waits Choice by text.
     *
     * @param cont Container to search component in.
     * @param text Choice text. If null, contents is not checked.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @param index Ordinal component index.
     * @return Choice instance.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public static Choice waitChoice(Container cont, String text, boolean ce, boolean ccs, int index) {
        return (waitChoice(cont,
                new ChoiceBySelectedItemFinder(text,
                        new DefaultStringComparator(ce, ccs)),
                index));
    }

    /**
     * Waits Choice by text.
     *
     * @param cont Container to search component in.
     * @param text Choice text. If null, contents is not checked.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @return Choice instance.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public static Choice waitChoice(Container cont, String text, boolean ce, boolean ccs) {
        return waitChoice(cont, text, ce, ccs, 0);
    }

    @Override
    public void setOutput(TestOut out) {
        output = out;
        super.setOutput(output.createErrorOutput());
    }

    @Override
    public TestOut getOutput() {
        return output;
    }

    @Override
    public void copyEnvironment(Operator anotherOperator) {
        super.copyEnvironment(anotherOperator);
        driver
                = (ListDriver) DriverManager.
                getDriver(DriverManager.LIST_DRIVER_ID,
                        getClass(),
                        anotherOperator.getProperties());
    }

    /**
     * Finds an item between choice items.
     *
     * @param item a text pattern.
     * @param index an ordinal index between appropriate items.
     * @return an item index.
     */
    public int findItemIndex(String item, int index) {
        return findItemIndex(item, getComparator(), index);
    }

    /**
     * Finds an item between choice items.
     *
     * @param item a text pattern.
     * @return an item index.
     */
    public int findItemIndex(String item) {
        return findItemIndex(item, 0);
    }

    /**
     * Selects an item by text.
     *
     * @param item a text pattern.
     * @param index an ordinal index between appropriate items.
     */
    public void selectItem(String item, int index) {
        selectItem(item, getComparator(), index);
    }

    /**
     * Selects an item by text.
     *
     * @param item a text pattern.
     */
    public void selectItem(String item) {
        selectItem(item, 0);
    }

    /**
     * Selects an item by index.
     *
     * @param index an item index.
     */
    public void selectItem(int index) {
        output.printLine("Select " + Integer.toString(index) + "`th item in combobox\n    : "
                + toStringSource());
        output.printGolden("Select " + Integer.toString(index) + "`th item in combobox");
        makeComponentVisible();
        try {
            waitComponentEnabled();
        } catch (InterruptedException e) {
            throw (new JemmyException("Interrupted!", e));
        }
        driver.selectItem(this, index);
        if (getVerification()) {
            waitItemSelected(index);
        }
    }

    /**
     * Waits for item to be selected.
     *
     * @param index Item index.
     */
    public void waitItemSelected(final int index) {
        getOutput().printLine("Wait " + Integer.toString(index)
                + "'th item to be selected in component \n    : "
                + toStringSource());
        getOutput().printGolden("Wait " + Integer.toString(index)
                + "'th item to be selected");
        waitState(new ComponentChooser() {
            @Override
            public boolean checkComponent(Component comp) {
                return getSelectedIndex() == index;
            }

            @Override
            public String getDescription() {
                return "Has " + Integer.toString(index) + "'th item selected";
            }

            @Override
            public String toString() {
                return "ChoiceOperator.waitItemSelected.ComponentChooser{description = " + getDescription() + '}';
            }
        });
    }

    /**
     * Returns information about component.
     */
    @Override
    public Hashtable<String, Object> getDump() {
        Hashtable<String, Object> result = super.getDump();
        if (((Choice) getSource()).getSelectedItem() != null) {
            result.put(SELECTED_ITEM_DPROP, ((Choice) getSource()).getSelectedItem());
        }
        String[] items = new String[((Choice) getSource()).getItemCount()];
        for (int i = 0; i < ((Choice) getSource()).getItemCount(); i++) {
            items[i] = ((Choice) getSource()).getItem(i);
        }
        addToDump(result, ITEM_PREFIX_DPROP, items);
        return result;
    }

    ////////////////////////////////////////////////////////
    //Mapping                                             //
    /**
     * Maps {@code Choice.add(String)} through queue
     */
    public void add(final String item) {
        runMapping(new MapVoidAction("add") {
            @Override
            public void map() {
                ((Choice) getSource()).add(item);
            }
        });
    }

    /**
     * Maps {@code Choice.addItemListener(ItemListener)} through queue
     */
    public void addItemListener(final ItemListener itemListener) {
        runMapping(new MapVoidAction("addItemListener") {
            @Override
            public void map() {
                ((Choice) getSource()).addItemListener(itemListener);
            }
        });
    }

    /**
     * Maps {@code Choice.addNotify()} through queue
     */
    @Override
    public void addNotify() {
        runMapping(new MapVoidAction("addNotify") {
            @Override
            public void map() {
                getSource().addNotify();
            }
        });
    }

    /**
     * Maps {@code Choice.getItem(int)} through queue
     */
    public String getItem(final int index) {
        return (runMapping(new MapAction<String>("getItem") {
            @Override
            public String map() {
                return ((Choice) getSource()).getItem(index);
            }
        }));
    }

    /**
     * Maps {@code Choice.getItemCount()} through queue
     */
    public int getItemCount() {
        return (runMapping(new MapIntegerAction("getItemCount") {
            @Override
            public int map() {
                return ((Choice) getSource()).getItemCount();
            }
        }));
    }

    /**
     * Maps {@code Choice.getSelectedIndex()} through queue
     */
    public int getSelectedIndex() {
        return (runMapping(new MapIntegerAction("getSelectedIndex") {
            @Override
            public int map() {
                return ((Choice) getSource()).getSelectedIndex();
            }
        }));
    }

    /**
     * Maps {@code Choice.getSelectedItem()} through queue
     */
    public String getSelectedItem() {
        return (runMapping(new MapAction<String>("getSelectedItem") {
            @Override
            public String map() {
                return ((Choice) getSource()).getSelectedItem();
            }
        }));
    }

    /**
     * Maps {@code Choice.insert(String)} through queue
     */
    public void insert(final String item, final int index) {
        runMapping(new MapVoidAction("insert") {
            @Override
            public void map() {
                ((Choice) getSource()).insert(item, index);
            }
        });
    }

    /**
     * Maps {@code Choice.remove(int)} through queue
     */
    public void remove(final int position) {
        runMapping(new MapVoidAction("remove") {
            @Override
            public void map() {
                ((Choice) getSource()).remove(position);
            }
        });
    }

    /**
     * Maps {@code Choice.remove(String)} through queue
     */
    public void remove(final String item) {
        runMapping(new MapVoidAction("remove") {
            @Override
            public void map() {
                ((Choice) getSource()).remove(item);
            }
        });
    }

    /**
     * Maps {@code Choice.removeAll()} through queue
     */
    public void removeAll() {
        runMapping(new MapVoidAction("removeAll") {
            @Override
            public void map() {
                ((Choice) getSource()).removeAll();
            }
        });
    }

    /**
     * Maps {@code Choice.removeItemListener(ItemListener)} through queue
     */
    public void removeItemListener(final ItemListener itemListener) {
        runMapping(new MapVoidAction("removeItemListener") {
            @Override
            public void map() {
                ((Choice) getSource()).removeItemListener(itemListener);
            }
        });
    }

    /**
     * Maps {@code Choice.select(int)} through queue
     */
    public void select(final int pos) {
        runMapping(new MapVoidAction("select") {
            @Override
            public void map() {
                ((Choice) getSource()).select(pos);
            }
        });
    }

    /**
     * Maps {@code Choice.select(String)} through queue
     */
    public void setState(final String str) {
        runMapping(new MapVoidAction("select") {
            @Override
            public void map() {
                ((Choice) getSource()).select(str);
            }
        });
    }

    //End of mapping                                      //
    ////////////////////////////////////////////////////////
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

    private void selectItem(String item, StringComparator comparator, int index) {
        selectItem(findItemIndex(item, comparator, index));
    }

    /**
     * Allows to find component by label.
     */
    public static class ChoiceBySelectedItemFinder implements ComponentChooser {

        String label;
        StringComparator comparator;

        /**
         * Constructs ChoiceBySelectedItemFinder.
         *
         * @param lb a text pattern
         * @param comparator specifies string comparision algorithm.
         */
        public ChoiceBySelectedItemFinder(String lb, StringComparator comparator) {
            label = lb;
            this.comparator = comparator;
        }

        /**
         * Constructs ChoiceBySelectedItemFinder.
         *
         * @param lb a text pattern
         */
        public ChoiceBySelectedItemFinder(String lb) {
            this(lb, Operator.getDefaultStringComparator());
        }

        @Override
        public boolean checkComponent(Component comp) {
            if (comp instanceof Choice) {
                if (((Choice) comp).getSelectedItem() != null) {
                    return (comparator.equals(((Choice) comp).getSelectedItem(),
                            label));
                }
            }
            return false;
        }

        @Override
        public String getDescription() {
            return "Choice with label \"" + label + "\"";
        }

        @Override
        public String toString() {
            return "ChoiceBySelectedItemFinder{" + "label=" + label + ", comparator=" + comparator + '}';
        }
    }

    /**
     * Checks component type.
     */
    public static class ChoiceFinder extends Finder {

        /**
         * Constructs ChoiceFinder.
         *
         * @param sf other searching criteria.
         */
        public ChoiceFinder(ComponentChooser sf) {
            super(Choice.class, sf);
        }

        /**
         * Constructs ChoiceFinder.
         */
        public ChoiceFinder() {
            super(Choice.class);
        }
    }
}
