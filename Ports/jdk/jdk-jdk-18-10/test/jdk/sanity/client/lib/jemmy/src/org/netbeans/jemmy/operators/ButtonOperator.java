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

import java.awt.Button;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionListener;
import java.util.Hashtable;

import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.Outputable;
import org.netbeans.jemmy.TestOut;
import org.netbeans.jemmy.TimeoutExpiredException;
import org.netbeans.jemmy.Timeoutable;
import org.netbeans.jemmy.Timeouts;
import org.netbeans.jemmy.drivers.ButtonDriver;
import org.netbeans.jemmy.drivers.DriverManager;

/**
 *
 * <BR><BR>Timeouts used: <BR>
 * ButtonOperator.PushButtonTimeout - time between button pressing and
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
public class ButtonOperator extends ComponentOperator
        implements Timeoutable, Outputable {

    /**
     * Identifier for a label property.
     *
     * @see #getDump
     */
    public static final String TEXT_DPROP = "Label";

    private final static long PUSH_BUTTON_TIMEOUT = 0;

    private Timeouts timeouts;
    private TestOut output;

    ButtonDriver driver;

    /**
     * Constructor.
     *
     * @param b The {@code java.awt.Button} managed by this instance.
     */
    public ButtonOperator(Button b) {
        super(b);
        driver = DriverManager.getButtonDriver(getClass());
    }

    /**
     * Constructs a ButtonOperator object.
     *
     * @param cont container
     * @param chooser a component chooser specifying searching criteria.
     * @param index an index between appropriate ones.
     */
    public ButtonOperator(ContainerOperator<?> cont, ComponentChooser chooser, int index) {
        this((Button) cont.
                waitSubComponent(new ButtonFinder(chooser),
                        index));
        copyEnvironment(cont);
    }

    /**
     * Constructs a ButtonOperator object.
     *
     * @param cont container
     * @param chooser a component chooser specifying searching criteria.
     */
    public ButtonOperator(ContainerOperator<?> cont, ComponentChooser chooser) {
        this(cont, chooser, 0);
    }

    /**
     * Constructor. Waits for a component in a container to show. The component
     * is identified as the {@code index+1}'th {@code java.awt.Button}
     * that shows, lies below the container in the display containment
     * hierarchy, and that has the desired text. Uses cont's timeout and output
     * for waiting and to init this operator.
     *
     * @param cont The operator for a container containing the sought for
     * button.
     * @param text Button text.
     * @param index Ordinal component index. The first component has
     * {@code index} 0.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public ButtonOperator(ContainerOperator<?> cont, String text, int index) {
        this((Button) waitComponent(cont,
                new ButtonByLabelFinder(text,
                        cont.getComparator()),
                index));
        copyEnvironment(cont);
    }

    /**
     * Constructor. Waits for a component in a container to show. The component
     * is identified as the first {@code java.awt.Button} that shows, lies
     * below the container in the display containment hierarchy, and that has
     * the desired text. Uses cont's timeout and output for waiting and to init
     * this operator.
     *
     * @param cont The operator for a container containing the sought for
     * button.
     * @param text Button text.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public ButtonOperator(ContainerOperator<?> cont, String text) {
        this(cont, text, 0);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont The operator for a container containing the sought for
     * button.
     * @param index Ordinal component index.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public ButtonOperator(ContainerOperator<?> cont, int index) {
        this((Button) waitComponent(cont,
                new ButtonFinder(),
                index));
        copyEnvironment(cont);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont The operator for a container containing the sought for
     * button.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public ButtonOperator(ContainerOperator<?> cont) {
        this(cont, 0);
    }

    /**
     * Searches Button in a container.
     *
     * @param cont Container in which to search for the component. The container
     * lies above the component in the display containment hierarchy. The
     * containment need not be direct.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation,
     * defining and applying search criteria.
     * @param index Ordinal component index. The first {@code index} is 0.
     * @return Button instance or null if component was not found.
     */
    public static Button findButton(Container cont, ComponentChooser chooser, int index) {
        return (Button) findComponent(cont, new ButtonFinder(chooser), index);
    }

    /**
     * Searches for the first Button in a container.
     *
     * @param cont Container in which to search for the component. The container
     * lies above the component in the display containment hierarchy. The
     * containment need not be direct.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation,
     * defining and applying search criteria.
     * @return Button instance or null if component was not found.
     */
    public static Button findButton(Container cont, ComponentChooser chooser) {
        return findButton(cont, chooser, 0);
    }

    /**
     * Searches Button by text.
     *
     * @param cont Container to search component in.
     * @param text Button text. If null, contents is not checked.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @param index Ordinal component index.
     * @return Button instance or null if component was not found.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     */
    public static Button findButton(Container cont, String text, boolean ce, boolean ccs, int index) {
        return findButton(cont, new ButtonByLabelFinder(text, new DefaultStringComparator(ce, ccs)), index);
    }

    /**
     * Searches Button by text.
     *
     * @param cont Container to search component in.
     * @param text Button text. If null, contents is not checked.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @return Button instance or null if component was not found.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     */
    public static Button findButton(Container cont, String text, boolean ce, boolean ccs) {
        return findButton(cont, text, ce, ccs, 0);
    }

    /**
     * Waits Button in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @param index Ordinal component index.
     * @return Button instance.
     * @throws TimeoutExpiredException
     */
    public static Button waitButton(Container cont, ComponentChooser chooser, int index) {
        return (Button) waitComponent(cont, new ButtonFinder(chooser), index);
    }

    /**
     * Waits 0'th Button in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @return Button instance.
     * @throws TimeoutExpiredException
     */
    public static Button waitButton(Container cont, ComponentChooser chooser) {
        return waitButton(cont, chooser, 0);
    }

    /**
     * Waits Button by text.
     *
     * @param cont Container to search component in.
     * @param text Button text. If null, contents is not checked.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @param index Ordinal component index.
     * @return Button instance.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public static Button waitButton(Container cont, String text, boolean ce, boolean ccs, int index) {
        return waitButton(cont, new ButtonByLabelFinder(text, new DefaultStringComparator(ce, ccs)), index);
    }

    /**
     * Waits Button by text.
     *
     * @param cont Container to search component in.
     * @param text Button text. If null, contents is not checked.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @return Button instance.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public static Button waitButton(Container cont, String text, boolean ce, boolean ccs) {
        return waitButton(cont, text, ce, ccs, 0);
    }

    static {
        Timeouts.initDefault("ButtonOperator.PushButtonTimeout", PUSH_BUTTON_TIMEOUT);
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
                = (ButtonDriver) DriverManager.
                getDriver(DriverManager.BUTTON_DRIVER_ID,
                        getClass(),
                        anotherOperator.getProperties());
    }

    /**
     * Pushes the button by mouse click.
     *
     * @throws TimeoutExpiredException
     */
    public void push() {
        output.printLine("Push button\n    :" + toStringSource());
        output.printGolden("Push button");
        driver.push(this);
    }

    /**
     * Runs {@code push()} method in a separate thread.
     */
    public void pushNoBlock() {
        produceNoBlocking(new NoBlockingAction<Void, Void>("Button pushing") {
            @Override
            public Void doAction(Void param) {
                push();
                return null;
            }
        });
    }

    /**
     * Press the button by mouse.
     *
     * @throws TimeoutExpiredException
     */
    public void press() {
        output.printLine("Press button\n    :" + toStringSource());
        output.printGolden("Press button");
        driver.press(this);
    }

    /**
     * Releases the button by mouse.
     *
     * @throws TimeoutExpiredException
     */
    public void release() {
        output.printLine("Release button\n    :" + toStringSource());
        output.printGolden("Release button");
        driver.press(this);
    }

    /**
     * Returns information about component.
     */
    @Override
    public Hashtable<String, Object> getDump() {
        Hashtable<String, Object> result = super.getDump();
        if (((Button) getSource()).getLabel() != null) {
            result.put(TEXT_DPROP, ((Button) getSource()).getLabel());
        }
        return result;
    }

    ////////////////////////////////////////////////////////
    //Mapping                                             //
    /**
     * Maps {@code Button.addActionListener(ActionListener)} through queue
     */
    public void addActionListener(final ActionListener actionListener) {
        runMapping(new MapVoidAction("addActionListener") {
            @Override
            public void map() {
                ((Button) getSource()).addActionListener(actionListener);
            }
        });
    }

    /**
     * Maps {@code Button.getActionCommand()} through queue
     */
    public String getActionCommand() {
        return (runMapping(new MapAction<String>("getActionCommand") {
            @Override
            public String map() {
                return ((Button) getSource()).getActionCommand();
            }
        }));
    }

    /**
     * Maps {@code Button.getLabel()} through queue
     */
    public String getLabel() {
        return (runMapping(new MapAction<String>("getLabel") {
            @Override
            public String map() {
                return ((Button) getSource()).getLabel();
            }
        }));
    }

    /**
     * Maps {@code Button.removeActionListener(ActionListener)} through queue
     */
    public void removeActionListener(final ActionListener actionListener) {
        runMapping(new MapVoidAction("removeActionListener") {
            @Override
            public void map() {
                ((Button) getSource()).removeActionListener(actionListener);
            }
        });
    }

    /**
     * Maps {@code Button.setActionCommand(String)} through queue
     */
    public void setActionCommand(final String string) {
        runMapping(new MapVoidAction("setActionCommand") {
            @Override
            public void map() {
                ((Button) getSource()).setActionCommand(string);
            }
        });
    }

    /**
     * Maps {@code Button.setLabel(String)} through queue
     */
    public void setLabel(final String string) {
        runMapping(new MapVoidAction("setLabel") {
            @Override
            public void map() {
                ((Button) getSource()).setLabel(string);
            }
        });
    }

    //End of mapping                                      //
    ////////////////////////////////////////////////////////
    /**
     * Allows to find component by label.
     */
    public static class ButtonByLabelFinder implements ComponentChooser {

        String label;
        StringComparator comparator;

        /**
         * Constructs ButtonByLabelFinder.
         *
         * @param lb a text pattern
         * @param comparator specifies string comparision algorithm.
         */
        public ButtonByLabelFinder(String lb, StringComparator comparator) {
            label = lb;
            this.comparator = comparator;
        }

        /**
         * Constructs ButtonByLabelFinder.
         *
         * @param lb a text pattern
         */
        public ButtonByLabelFinder(String lb) {
            this(lb, Operator.getDefaultStringComparator());
        }

        @Override
        public boolean checkComponent(Component comp) {
            if (comp instanceof Button) {
                if (((Button) comp).getLabel() != null) {
                    return (comparator.equals(((Button) comp).getLabel(),
                            label));
                }
            }
            return false;
        }

        @Override
        public String getDescription() {
            return "Button with label \"" + label + "\"";
        }

        @Override
        public String toString() {
            return "ButtonByLabelFinder{" + "label=" + label + ", comparator=" + comparator + '}';
        }
    }

    /**
     * Checks component type.
     */
    public static class ButtonFinder extends Finder {

        /**
         * Constructs AbstractButtonFinder.
         *
         * @param sf other searching criteria.
         */
        public ButtonFinder(ComponentChooser sf) {
            super(Button.class, sf);
        }

        /**
         * Constructs AbstractButtonFinder.
         */
        public ButtonFinder() {
            super(Button.class);
        }
    }
}
