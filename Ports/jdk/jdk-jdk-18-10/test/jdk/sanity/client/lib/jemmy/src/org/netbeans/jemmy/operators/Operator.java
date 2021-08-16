/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.event.InputEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import org.netbeans.jemmy.Action;
import org.netbeans.jemmy.ActionProducer;
import org.netbeans.jemmy.CharBindingMap;
import org.netbeans.jemmy.ClassReference;
import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.ComponentSearcher;
import org.netbeans.jemmy.JemmyException;
import org.netbeans.jemmy.JemmyProperties;
import org.netbeans.jemmy.Outputable;
import org.netbeans.jemmy.QueueTool;
import org.netbeans.jemmy.TestOut;
import org.netbeans.jemmy.TimeoutExpiredException;
import org.netbeans.jemmy.Timeoutable;
import org.netbeans.jemmy.Timeouts;
import org.netbeans.jemmy.Waitable;
import org.netbeans.jemmy.Waiter;
import org.netbeans.jemmy.util.DefaultVisualizer;
import org.netbeans.jemmy.util.MouseVisualizer;
import org.netbeans.jemmy.util.Platform;

/**
 * Keeps all environment and low-level methods.
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 */
public abstract class Operator
        implements Timeoutable, Outputable {

    /**
     * Identifier for a "class" property.
     *
     * @see #getDump
     */
    public static final String CLASS_DPROP = "Class";

    /**
     * Identifier for a "toString" property.
     *
     * @see #getDump
     */
    public static final String TO_STRING_DPROP = "toString";

    private static Vector<String> operatorPkgs;

    private Timeouts timeouts;
    private TestOut output;
    private CharBindingMap map;
    private ComponentVisualizer visualizer;
    private StringComparator comparator;
    private PathParser parser;
    private QueueTool queueTool;
    private boolean verification = false;
    private JemmyProperties properties;

    /**
     * Inits environment.
     */
    public Operator() {
        super();
        initEnvironment();
    }

    /**
     * Specifies an object to be used by default to prepare component. Each new
     * operator created after the method using will have defined visualizer.
     * Default implementation is org.netbeans.jemmy.util.DefaultVisualizer
     * class.
     *
     * @param visualizer ComponentVisualizer implementation
     * @return previous value
     * @see #setVisualizer(Operator.ComponentVisualizer)
     * @see #getDefaultComponentVisualizer()
     * @see org.netbeans.jemmy.util.DefaultVisualizer
     */
    public static ComponentVisualizer setDefaultComponentVisualizer(ComponentVisualizer visualizer) {
        return ((ComponentVisualizer) JemmyProperties.
                setCurrentProperty("ComponentOperator.ComponentVisualizer", visualizer));
    }

    /**
     * Returns an object to be used by default to prepare component.
     *
     * @return Object is used by default to prepare component
     * @see #getVisualizer()
     * @see #setDefaultComponentVisualizer(Operator.ComponentVisualizer)
     */
    public static ComponentVisualizer getDefaultComponentVisualizer() {
        return ((ComponentVisualizer) JemmyProperties.
                getCurrentProperty("ComponentOperator.ComponentVisualizer"));
    }

    /**
     * Defines string comparator to be assigned in constructor.
     *
     * @param comparator the comparator to be used by default.
     * @return previous value.
     * @see #getDefaultStringComparator()
     * @see Operator.StringComparator
     */
    public static StringComparator setDefaultStringComparator(StringComparator comparator) {
        return ((StringComparator) JemmyProperties.
                setCurrentProperty("ComponentOperator.StringComparator", comparator));
    }

    /**
     * Returns string comparator used to init operators.
     *
     * @return the comparator used by default.
     * @see #setDefaultStringComparator(Operator.StringComparator)
     * @see Operator.StringComparator
     */
    public static StringComparator getDefaultStringComparator() {
        return ((StringComparator) JemmyProperties.
                getCurrentProperty("ComponentOperator.StringComparator"));
    }

    /**
     * Specifies an object used for parsing of path-like strings.
     *
     * @param parser the parser.
     * @return a previous value.
     * @see Operator.PathParser
     * @see #getDefaultPathParser
     */
    public static PathParser setDefaultPathParser(PathParser parser) {
        return ((PathParser) JemmyProperties.
                setCurrentProperty("ComponentOperator.PathParser", parser));
    }

    /**
     * Returns an object used for parsing of path-like strings.
     *
     * @return a parser used by default.
     * @see Operator.PathParser
     * @see #setDefaultPathParser
     */
    public static PathParser getDefaultPathParser() {
        return ((PathParser) JemmyProperties.
                getCurrentProperty("ComponentOperator.PathParser"));
    }

    /**
     * Defines whether newly created operators should perform operation
     * verifications by default.
     *
     * @param verification a verification mode to be used by default.
     * @return a previous value.
     * @see #getDefaultVerification()
     * @see #setVerification(boolean)
     */
    public static boolean setDefaultVerification(boolean verification) {
        Boolean oldValue = (Boolean) (JemmyProperties.
                setCurrentProperty("Operator.Verification",
                        verification ? Boolean.TRUE : Boolean.FALSE));
        return (oldValue != null) ? oldValue : false;
    }

    /**
     * Says whether newly created operators perform operations verifications by
     * default.
     *
     * @return a verification mode used by default.
     * @see #setDefaultVerification(boolean)
     * @see #getVerification()
     */
    public static boolean getDefaultVerification() {
        return ((Boolean) (JemmyProperties.
                getCurrentProperty("Operator.Verification")));
    }

    /**
     * Compares caption (button text, window title, ...) with a sample text.
     *
     * @param caption String to be compared with match. Method returns false, if
     * parameter is null.
     * @param match Sample to compare with. Method returns true, if parameter is
     * null.
     * @param ce Compare exactly. If true, text can be a substring of caption.
     * @param ccs Compare case sensitively. If true, both text and caption are
     * converted to upper case before comparison.
     * @return true is the captions matched the match.
     * @see #isCaptionEqual
     * @deprecated use another methods with the same name.
     */
    @Deprecated
    public static boolean isCaptionEqual(String caption, String match, boolean ce, boolean ccs) {
        return new DefaultStringComparator(ce, ccs).equals(caption, match);
    }

    /**
     * Compares caption (button text, window title, ...) with a sample text.
     *
     * @param caption String to be compared with match
     * @param match Sample to compare with
     * @param comparator StringComparator instance.
     * @return true is the captions matched the match.
     * @see #isCaptionEqual
     */
    public static boolean isCaptionEqual(String caption, String match, StringComparator comparator) {
        return comparator.equals(caption, match);
    }

    /**
     * Returns default mouse button mask.
     *
     * @return {@code InputEvent.BUTTON*_MASK} field value
     */
    public static int getDefaultMouseButton() {
        return InputEvent.BUTTON1_MASK;
    }

    /**
     * Returns mask of mouse button which used to popup expanding.
     * (InputEvent.BUTTON3_MASK)
     *
     * @return {@code InputEvent.BUTTON*_MASK} field value
     */
    public static int getPopupMouseButton() {
        return InputEvent.BUTTON3_MASK;
    }

    /**
     * Creates operator for component. Tries to find class with "operator
     * package"."class name"Operator name, where "operator package" is a package
     * from operator packages list, and "class name" is the name of class or one
     * of its superclasses.
     *
     * @param comp Component to create operator for.
     * @return a new operator with default environment.
     * @see #addOperatorPackage(String)
     */
    public static ComponentOperator createOperator(Component comp) {
        //hack!
        try {
            Class<?> cclass = Class.forName("java.awt.Component");
            Class<?> compClass = comp.getClass();
            ComponentOperator result;
            do {
                if ((result = createOperator(comp, compClass)) != null) {
                    return result;
                }
            } while (cclass.isAssignableFrom(compClass = compClass.getSuperclass()));
        } catch (ClassNotFoundException ignored) {
        }
        return null;
    }

    /**
     * Adds package to the list of packages containing operators. <BR>
     * "org.netbeans.jemmy.operators" is in the list by default.
     *
     * @param pkgName Package name.
     * @see #createOperator(Component)
     */
    public static void addOperatorPackage(String pkgName) {
        operatorPkgs.add(pkgName);
    }

    /**
     * Returns an operator containing default environment.
     *
     * @return an empty operator (not having any component source) having
     * default environment.
     */
    public static Operator getEnvironmentOperator() {
        return new NullOperator();
    }

    static {
        //init visualizer depending on OS:
        //Linux - new MouseVisualizer(MouseVisualizer.TOP, 0.5, 10, false)
        //solaris - new MouseVisualizer()
        //others - new DefaultVisualizer()
        if (Platform.isLinux()) {
            setDefaultComponentVisualizer(new MouseVisualizer(MouseVisualizer.TOP, 0.5, 10, false));
        } else if (Platform.isSolaris()) {
            setDefaultComponentVisualizer(new MouseVisualizer());
        } else {
            setDefaultComponentVisualizer(new DefaultVisualizer());
        }
        operatorPkgs = new Vector<>();
        setDefaultStringComparator(new DefaultStringComparator(false, false));
        setDefaultPathParser(new DefaultPathParser("|"));
        addOperatorPackage("org.netbeans.jemmy.operators");
        setDefaultVerification(true);
    }

    /**
     * Returns object operator is used for.
     *
     * @return an instance of java.awt.Component subclass which this operator
     * was created for.
     */
    public abstract Component getSource();

    ////////////////////////////////////////////////////////
    //Environment                                         //
    ////////////////////////////////////////////////////////
    /**
     * Returns QueueTool is used to work with queue.
     *
     * @return a QueueTool.
     */
    public QueueTool getQueueTool() {
        return queueTool;
    }

    /**
     * Copies all environment (output, timeouts, visualizer) from another
     * operator.
     *
     * @param anotherOperator an operator to copy the environment to.
     */
    public void copyEnvironment(Operator anotherOperator) {
        setTimeouts(anotherOperator.getTimeouts());
        setOutput(anotherOperator.getOutput());
        setVisualizer(anotherOperator.getVisualizer());
        setComparator(anotherOperator.getComparator());
        setVerification(anotherOperator.getVerification());
        setCharBindingMap(anotherOperator.getCharBindingMap());
        setProperties(anotherOperator.getProperties());
    }

    @Override
    public void setTimeouts(Timeouts timeouts) {
        this.timeouts = timeouts;
        queueTool.setTimeouts(timeouts);
    }

    @Override
    public Timeouts getTimeouts() {
        return timeouts;
    }

    /**
     * Returns component visualizer. Visualizer is used from from
     * makeComponentVisible() method.
     *
     * @return a visualizer assigned to this operator.
     * @see #getDefaultComponentVisualizer()
     * @see #setVisualizer(Operator.ComponentVisualizer)
     */
    public ComponentVisualizer getVisualizer() {
        return visualizer;
    }

    /**
     * Changes component visualizer. Visualizer is used from from
     * makeComponentVisible() method.
     *
     * @param vo a visualizer to assign to this operator.
     * @see #setDefaultComponentVisualizer(Operator.ComponentVisualizer)
     * @see #getVisualizer()
     */
    public void setVisualizer(ComponentVisualizer vo) {
        visualizer = vo;
    }

    /**
     * Returns a JemmyProperty object assigned to this operator.
     *
     * @return a JemmyProperty object got from the top of property stack or from
     * another operator by copyuing environment.
     * @see #setProperties
     */
    public JemmyProperties getProperties() {
        return properties;
    }

    /**
     * Assigns a JemmyProperty object to this operator.
     *
     * @param properties a properties to assign to this operator.
     * @return previously assigned properties.
     * @see #getProperties
     */
    public JemmyProperties setProperties(JemmyProperties properties) {
        JemmyProperties oldProperties = getProperties();
        this.properties = properties;
        return oldProperties;
    }

    /**
     * Defines CharBindingMap.
     *
     * @param map a CharBindingMap to use for keyboard operations.
     * @see org.netbeans.jemmy.CharBindingMap
     * @see
     * org.netbeans.jemmy.JemmyProperties#setCurrentCharBindingMap(CharBindingMap)
     * @see #getCharBindingMap
     */
    public void setCharBindingMap(CharBindingMap map) {
        this.map = map;
    }

    /**
     * Returns CharBindingMap used for keyboard operations.
     *
     * @return a map assigned to this object.
     * @see #setCharBindingMap
     */
    public CharBindingMap getCharBindingMap() {
        return map;
    }

    @Override
    public void setOutput(TestOut out) {
        output = out;
        queueTool.setOutput(output.createErrorOutput());
    }

    @Override
    public TestOut getOutput() {
        return output;
    }

    /**
     * Returns object which is used for string comparison.
     *
     * @return a comparator assigned to this operator.
     * @see org.netbeans.jemmy.operators.Operator.StringComparator
     * @see org.netbeans.jemmy.operators.Operator.DefaultStringComparator
     * @see #setComparator
     */
    public StringComparator getComparator() {
        return comparator;
    }

    /**
     * Defines object which is used for string comparison.
     *
     * @param comparator a comparator to use for string comparision.
     * @see org.netbeans.jemmy.operators.Operator.StringComparator
     * @see org.netbeans.jemmy.operators.Operator.DefaultStringComparator
     * @see #getComparator
     */
    public void setComparator(StringComparator comparator) {
        this.comparator = comparator;
    }

    /**
     * Returns object which is used for parsing of path-like strings.
     *
     * @return a comparator assigned to this operator.
     * @see #setPathParser
     */
    public PathParser getPathParser() {
        return parser;
    }

    /**
     * Specifies object which is used for parsing of path-like strings.
     *
     * @param parser a parser to use for path parsing.
     * @see #getPathParser
     */
    public void setPathParser(PathParser parser) {
        this.parser = parser;
    }

    /**
     * Defines whether operator should perform operation verifications.
     *
     * @param verification new value.
     * @return old value
     * @see #setDefaultVerification(boolean)
     * @see #getDefaultVerification()
     * @see #getVerification()
     */
    public boolean setVerification(boolean verification) {
        boolean oldValue = this.verification;
        this.verification = verification;
        return oldValue;
    }

    /**
     * Says whether operator performs operation verifications.
     *
     * @return old value
     * @see #setDefaultVerification(boolean)
     * @see #getDefaultVerification()
     * @see #setVerification(boolean)
     */
    public boolean getVerification() {
        return verification;
    }

    ////////////////////////////////////////////////////////
    //Util                                                //
    ////////////////////////////////////////////////////////
    /**
     * Creates new array which has all elements from first array, except last
     * element.
     *
     * @param path an original array
     * @return new array
     */
    public String[] getParentPath(String path[]) {
        if (path.length > 1) {
            String[] ppath = new String[path.length - 1];
            System.arraycopy(path, 0, ppath, 0, ppath.length);
            return ppath;
        } else {
            return new String[0];
        }
    }

    public ComponentChooser[] getParentPath(ComponentChooser path[]) {
        if (path.length > 1) {
            ComponentChooser[] ppath = new ComponentChooser[path.length - 1];
            System.arraycopy(path, 0, ppath, 0, ppath.length);
            return ppath;
        } else {
            return new ComponentChooser[0];
        }
    }

    /**
     * Parses a string to a string array using a PathParser assigned to this
     * operator.
     *
     * @param path an original string
     * @return created String array.
     */
    public String[] parseString(String path) {
        return getPathParser().parse(path);
    }

    /**
     * Parses strings like "1|2|3" into arrays {"1", "2", "3"}.
     *
     * @param path an original string
     * @param delim a delimiter string
     * @return created String array.
     */
    public String[] parseString(String path, String delim) {
        return new DefaultPathParser(delim).parse(path);
    }

    /**
     * Returns key code to be pressed for character typing.
     *
     * @param c Character to be typed.
     * @return a value of one of the {@code KeyEvent.VK_*} fields.
     * @see org.netbeans.jemmy.CharBindingMap
     */
    public int getCharKey(char c) {
        return map.getCharKey(c);
    }

    /**
     * Returns modifiers mask for character typing.
     *
     * @param c Character to be typed.
     * @return a combination of {@code InputEvent.*_MASK} fields.
     * @see org.netbeans.jemmy.CharBindingMap
     */
    public int getCharModifiers(char c) {
        return map.getCharModifiers(c);
    }

    /**
     * Returns key codes to by pressed for characters typing.
     *
     * @param c Characters to be typed.
     * @return an array of {@code KeyEvent.VK_*} values.
     * @see org.netbeans.jemmy.CharBindingMap
     */
    public int[] getCharsKeys(char[] c) {
        int[] result = new int[c.length];
        for (int i = 0; i < c.length; i++) {
            result[i] = getCharKey(c[i]);
        }
        return result;
    }

    /**
     * Returns modifiers masks for characters typing.
     *
     * @param c Characters to be typed.
     * @return an array of a combination of {@code InputEvent.*_MASK}
     * fields.
     * @see org.netbeans.jemmy.CharBindingMap
     */
    public int[] getCharsModifiers(char[] c) {
        int[] result = new int[c.length];
        for (int i = 0; i < c.length; i++) {
            result[i] = getCharModifiers(c[i]);
        }
        return result;
    }

    /**
     * Returns key codes to by pressed for the string typing.
     *
     * @param s String to be typed.
     * @return an array of {@code KeyEvent.VK_*} values.
     * @see org.netbeans.jemmy.CharBindingMap
     */
    public int[] getCharsKeys(String s) {
        return getCharsKeys(s.toCharArray());
    }

    /**
     * Returns modifiers masks for the string typing.
     *
     * @param s String to be typed.
     * @return an array of a combination of {@code InputEvent.*_MASK}
     * fields.
     * @see org.netbeans.jemmy.CharBindingMap
     */
    public int[] getCharsModifiers(String s) {
        return getCharsModifiers(s.toCharArray());
    }

    /**
     * Compares string using getComparator StringComparator.
     *
     * @param caption a caption
     * @param match a pattern
     * @return true if {@code caption} and {@code match} match
     * @see #isCaptionEqual
     */
    public boolean isCaptionEqual(String caption, String match) {
        return comparator.equals(caption, match);
    }

    /**
     * Prints component information into operator output.
     */
    public void printDump() {
        Hashtable<String, Object> result = getDump();
        Object[] keys = result.keySet().toArray();
        for (int i = 0; i < result.size(); i++) {
            output.printLine(keys[i]
                    + " = "
                    + result.get(keys[i]));
        }
    }

    /**
     * Returns information about component. All records marked by simbolic
     * constants defined in public static final {@code *_DPROP} fields for
     * each operator type.
     *
     * @return a Hashtable containing name-value pairs.
     */
    public Hashtable<String, Object> getDump() {
        Hashtable<String, Object> result = new Hashtable<>();
        result.put(CLASS_DPROP, getSource().getClass().getName());
        result.put(TO_STRING_DPROP, getSource().toString());
        return result;
    }

    /**
     * Waits a state specified by a ComponentChooser instance.
     *
     * @param state a ComponentChooser defining the state criteria.
     * @throws TimeoutExpiredException if the state has not achieved in a value
     * defined by {@code "ComponentOperator.WaitStateTimeout"}
     */
    public void waitState(final ComponentChooser state) {
        waitState(new Waitable<String, Void>() {
            @Override
            public String actionProduced(Void obj) {
                return state.checkComponent(getSource()) ? "" : null;
            }

            @Override
            public String getDescription() {
                return "Wait \"" + state.getDescription()
                        + "\" state to be reached";
            }

            @Override
            public String toString() {
                return "Operator.waitState.Waitable{description = " + getDescription() + '}';
            }
        });
    }

    public <R> R waitState(Waitable<R, Void> waitable) {
        Waiter<R, Void> stateWaiter = new Waiter<>(waitable);
        stateWaiter.setTimeoutsToCloneOf(getTimeouts(),
                "ComponentOperator.WaitStateTimeout");
        stateWaiter.setOutput(getOutput().createErrorOutput());
        try {
            return stateWaiter.waitAction(null);
        } catch (InterruptedException e) {
            throw new JemmyException(
                    "Waiting of \"" + waitable.getDescription()
                            + "\" state has been interrupted!");
        }
    }

    /**
     * Waits a state specified by a ComponentChooser instance on EDT queue.
     *
     * @param state a ComponentChooser defining the state criteria.
     * @throws TimeoutExpiredException if the state has not achieved in a value
     * defined by {@code "ComponentOperator.WaitStateTimeout"}
     */
    public void waitStateOnQueue(final ComponentChooser state) {
        waitState(new ComponentChooser() {
            @Override
            public boolean checkComponent(Component comp) {
                return (boolean) (queueTool.invokeSmoothly(
                        new QueueTool.QueueAction<Object>("checkComponent") {
                            @Override
                            public final Object launch() throws Exception {
                                return state.checkComponent(comp);
                            }
                        }));
            }

            @Override
            public String getDescription() {
                return state.getDescription();
            }
        });
    }

    ////////////////////////////////////////////////////////
    //Mapping                                             //
    ////////////////////////////////////////////////////////
    /**
     * Performs an operation with time control.
     *
     * @param action an action to execute.
     * @param param an action parameters.
     * @param actionTimeOrigin is a timeout name to use for waiting for the
     * action to be finished.
     * @return an action result.
     */
    protected <R, P> R produceTimeRestricted(Action<R, P> action, final P param,
            String actionTimeOrigin) {
        ActionProducer<R, P> producer = new ActionProducer<>(action);
        producer.setOutput(getOutput().createErrorOutput());
        producer.setTimeouts(getTimeouts().cloneThis());
        producer.getTimeouts().setTimeout("ActionProducer.MaxActionTime",
                getTimeouts().getTimeout(actionTimeOrigin));
        try {
            R result = producer.produceAction(param, actionTimeOrigin);
            Throwable exception = producer.getException();
            if (exception != null) {
                if (exception instanceof JemmyException) {
                    throw ((JemmyException) exception);
                } else {
                    throw (new JemmyException("Exception during " + action.getDescription(),
                            exception));
                }
            }
            return result;
        } catch (InterruptedException e) {
            throw (new JemmyException("Interrupted!", e));
        }
    }

    /**
     * Performs an operation with time control.
     *
     * @param action an action to execute.
     * @param actionTimeOrigin is a timeout name to use for waiting for the
     * action to be finished.
     * @return an action result.
     */
    protected <R, P> R produceTimeRestricted(Action<R, P> action, String actionTimeOrigin) {
        return produceTimeRestricted(action, null, actionTimeOrigin);
    }

    /**
     * Performs an operation without time control.
     *
     * @param action an action to execute.
     * @param param an action parameters.
     */
    protected <R, P> void produceNoBlocking(NoBlockingAction<R, P> action, P param) {
        try {
            ActionProducer<R, P> noBlockingProducer = new ActionProducer<>(action, false);
            noBlockingProducer.setOutput(output.createErrorOutput());
            noBlockingProducer.setTimeouts(timeouts);
            noBlockingProducer.produceAction(param, null);
        } catch (InterruptedException e) {
            throw (new JemmyException("Exception during \""
                    + action.getDescription()
                    + "\" execution",
                    e));
        }
        if (action.exception != null) {
            throw (new JemmyException("Exception during nonblocking \""
                    + action.getDescription() + "\"",
                    action.exception));
        }
    }

    /**
     * Performs an operation without time control.
     *
     * @param action an action to execute.
     */
    protected void produceNoBlocking(NoBlockingAction<?, ?> action) {
        produceNoBlocking(action, null);
    }

    /**
     * Equivalent to {@code getQueue().lock();}.
     */
    protected void lockQueue() {
        queueTool.lock();
    }

    /**
     * Equivalent to {@code getQueue().unlock();}.
     */
    protected void unlockQueue() {
        queueTool.unlock();
    }

    /**
     * Unlocks Queue and then throw exception.
     *
     * @param e an exception to be thrown.
     */
    protected void unlockAndThrow(Exception e) {
        unlockQueue();
        throw (new JemmyException("Exception during queue locking", e));
    }

    /**
     * To map nonprimitive type component's method.
     *
     * @param action a mapping action.
     * @return an action result.
     * @see Operator.MapAction
     */
    protected <R> R runMapping(MapAction<R> action) {
        return runMappingPrimitive(action);
    }

    /**
     * To map char component's method.
     *
     * @param action a mapping action.
     * @return an action result.
     * @see #runMapping(Operator.MapAction)
     * @see Operator.MapCharacterAction
     */
    protected char runMapping(MapCharacterAction action) {
        return (Character) runMappingPrimitive(action);
    }

    /**
     * To map byte component's method.
     *
     * @param action a mapping action.
     * @return an action result.
     * @see #runMapping(Operator.MapAction)
     * @see Operator.MapByteAction
     */
    protected byte runMapping(MapByteAction action) {
        return (Byte) runMappingPrimitive(action);
    }

    /**
     * To map int component's method.
     *
     * @param action a mapping action.
     * @return an action result.
     * @see #runMapping(Operator.MapAction)
     * @see Operator.MapIntegerAction
     */
    protected int runMapping(MapIntegerAction action) {
        return (Integer) runMappingPrimitive(action);
    }

    /**
     * To map long component's method.
     *
     * @param action a mapping action.
     * @return an action result.
     * @see #runMapping(Operator.MapAction)
     * @see Operator.MapLongAction
     */
    protected long runMapping(MapLongAction action) {
        return (Long) runMappingPrimitive(action);
    }

    /**
     * To map float component's method.
     *
     * @param action a mapping action.
     * @return an action result.
     * @see #runMapping(Operator.MapAction)
     * @see Operator.MapFloatAction
     */
    protected float runMapping(MapFloatAction action) {
        return (Float) runMappingPrimitive(action);
    }

    /**
     * To map double component's method.
     *
     * @param action a mapping action.
     * @return an action result.
     * @see #runMapping(Operator.MapAction)
     * @see Operator.MapDoubleAction
     */
    protected double runMapping(MapDoubleAction action) {
        return (Double) runMappingPrimitive(action);
    }

    /**
     * To map boolean component's method.
     *
     * @param action a mapping action.
     * @return an action result.
     * @see #runMapping(Operator.MapAction)
     * @see Operator.MapBooleanAction
     */
    protected boolean runMapping(MapBooleanAction action) {
        return (Boolean) runMappingPrimitive(action);
    }

    /**
     * To map void component's method.
     *
     * @param action a mapping action.
     * @see #runMapping(Operator.MapAction)
     * @see Operator.MapVoidAction
     */
    protected void runMapping(MapVoidAction action) {
        runMappingPrimitive(action);
    }

    /**
     * Adds array of objects to dump hashtable. Is used for multiple properties
     * such as list items and tree nodes.
     *
     * @param table a table to add properties to.
     * @param title property names prefix. Property names are constructed by
     * adding a number to the prefix:
     * {@code title + "_" + Iteger.toString("ordinal index")}
     * @param items an array of property values.
     * @return an array of property names (with added numbers).
     */
    protected String[] addToDump(Hashtable<String, Object> table, String title, Object[] items) {
        String[] names = createNames(title + "_", items.length);
        for (int i = 0; i < items.length; i++) {
            table.put(names[i], items[i].toString());
        }
        return names;
    }

    /**
     * Adds two dimentional array of objects to dump hashtable. Is used for
     * multiple properties such as table cells.
     *
     * @param table a table to add properties to.
     * @param title property names prefix. Property names are constructed by
     * adding two numbers to the prefix:
     * {@code title + "_" + Iteger.toString("row index") + "_" + Iteger.toString("column index")}
     * @param items an array of property values.
     * @return an array of property names (with added numbers).
     */
    protected String[] addToDump(Hashtable<String, Object> table, String title, Object[][] items) {
        String[] names = createNames(title + "_", items.length);
        for (int i = 0; i < items.length; i++) {
            addToDump(table, names[i], items[i]);
        }
        return names;
    }
    ////////////////////////////////////////////////////////
    //Private                                             //
    ////////////////////////////////////////////////////////

    private <R> R runMappingPrimitive(QueueTool.QueueAction<R> action) {
        return queueTool.invokeSmoothly(action);
    }

    private String[] createNames(String title, int count) {
        String[] result = new String[count];
        int indexLength = Integer.toString(count).length();
        StringBuilder zeroStringB = new StringBuilder(indexLength);
        for (int i = 0; i < indexLength; i++) {
            zeroStringB.append('0');
        }
        String zeroString = zeroStringB.toString();
        for (int i = 0; i < count; i++) {
            String indexString = Integer.toString(i);
            result[i] = title
                    + zeroString.substring(0, indexLength - indexString.length())
                    + indexString;
        }
        return result;
    }

    private static ComponentOperator createOperator(Component comp, Class<?> compClass) {
        StringTokenizer token = new StringTokenizer(compClass.getName(), ".");
        String className = "";
        while (token.hasMoreTokens()) {
            className = token.nextToken();
        }
        Object[] params = {comp};
        Class<?>[] param_classes = {compClass};
        String operatorPackage;
        for (String operatorPkg : operatorPkgs) {
            operatorPackage = operatorPkg;
            try {
                return ((ComponentOperator) new ClassReference(operatorPackage + "."
                        + className + "Operator").
                        newInstance(params, param_classes));
            } catch (ClassNotFoundException ignored) {
            } catch (InvocationTargetException ignored) {
            } catch (NoSuchMethodException ignored) {
            } catch (IllegalAccessException ignored) {
            } catch (InstantiationException ignored) {
            }
        }
        return null;
    }

    private void initEnvironment() {
        queueTool = new QueueTool();
        setTimeouts(JemmyProperties.getProperties().getTimeouts());
        setOutput(JemmyProperties.getProperties().getOutput());
        setCharBindingMap(JemmyProperties.getProperties().getCharBindingMap());
        setVisualizer(getDefaultComponentVisualizer());
        setComparator(getDefaultStringComparator());
        setVerification(getDefaultVerification());
        setProperties(JemmyProperties.getProperties());
        setPathParser(getDefaultPathParser());
    }

    /**
     * Returns toString() result from component of this operator. It calls
     * {@link #getSource}.toString() in dispatch thread.
     *
     * @return toString() result from component of this operator.
     */
    public String toStringSource() {
        return runMapping(new MapAction<String>("getSource().toString()") {
            @Override
            public String map() {
                return getSource().toString();
            }
        });
    }

    /**
     * Interface used to make component visible & ready to to make operations
     * with.
     */
    public interface ComponentVisualizer {

        /**
         * Prepares component for a user input.
         *
         * @param compOper Operator asking for necessary actions.
         */
        public void makeVisible(ComponentOperator compOper);
    }

    /**
     * Interface to compare string resources like labels, button text, ... with
     * match. <BR>
     */
    public interface StringComparator {

        /**
         * Imlementation must return true if strings are equal.
         *
         * @param caption a text to compare with pattern.
         * @param match a pattern
         * @return true if text and pattern matches.
         */
        public boolean equals(String caption, String match);
    }

    /**
     * Default StringComparator implementation.
     */
    public static class DefaultStringComparator implements StringComparator {

        boolean ce;
        boolean ccs;

        /**
         * Constructs a DefaultStringComparator object.
         *
         * @param ce Compare exactly. If false, text can be a substring of
         * caption.
         * @param ccs Compare case sensitively.
         */
        public DefaultStringComparator(boolean ce, boolean ccs) {
            this.ce = ce;
            this.ccs = ccs;
        }

        /**
         * Compares a caption with a match using switched passed into
         * constructor.
         *
         * @param caption String to be compared with match. Method returns
         * false, if parameter is null.
         * @param match Sample to compare with. Method returns true, if
         * parameter is null.
         * @return true if text and pattern matches.
         */
        @Override
        public boolean equals(String caption, String match) {
            if (match == null) {
                return true;
            }
            if (caption == null) {
                return false;
            }
            String c, t;
            if (!ccs) {
                c = caption.toUpperCase();
                t = match.toUpperCase();
            } else {
                c = caption;
                t = match;
            }
            if (ce) {
                return c.equals(t);
            } else {
                return c.contains(t);
            }
        }
    }

    /**
     * Used for parsing of path-like strings.
     */
    public interface PathParser {

        /**
         * Parses a string to a String array.
         *
         * @param path a String to parse.
         * @return a parsed array.
         */
        public String[] parse(String path);
    }

    /**
     * Used for parsing of path-like strings where path components are separated
     * by a string-separator: "drive|directory|subdirectory|file".
     */
    public static class DefaultPathParser implements PathParser {

        String separator;

        /**
         * Constructs a DefaultPathParser object.
         *
         * @param separator a string used as separator.
         */
        public DefaultPathParser(String separator) {
            this.separator = separator;
        }

        @Override
        public String[] parse(String path) {
            if (path.length() > 0) {
                Vector<String> parsed = new Vector<>();
                int position = 0;
                int sepIndex = 0;
                while ((sepIndex = path.indexOf(separator, position)) != -1) {
                    parsed.add(path.substring(position, sepIndex));
                    position = sepIndex + separator.length();
                }
                parsed.add(path.substring(position));
                String[] result = new String[parsed.size()];
                for (int i = 0; i < parsed.size(); i++) {
                    result[i] = parsed.get(i);
                }
                return result;
            } else {
                return new String[0];
            }
        }
    }

    /**
     * Allows to bind a component by a component type.
     */
    public static class Finder implements ComponentChooser {

        Class<?> clz;
        ComponentChooser subchooser;

        /**
         * Constructs Finder.
         *
         * @param clz a component class.
         * @param subchooser other searching criteria.
         */
        public Finder(Class<?> clz, ComponentChooser subchooser) {
            this.clz = clz;
            this.subchooser = subchooser;
        }

        /**
         * Constructs Finder.
         *
         * @param clz a component class.
         */
        public Finder(Class<?> clz) {
            this(clz, ComponentSearcher.getTrueChooser("Any " + clz.getName()));
        }

        @Override
        public boolean checkComponent(Component comp) {
            if (clz.isInstance(comp)) {
                return subchooser.checkComponent(comp);
            }
            return false;
        }

        @Override
        public String getDescription() {
            return subchooser.getDescription();
        }

        @Override
        public String toString() {
            return "Finder{" + "clz=" + clz + ", subchooser=" + subchooser + '}';
        }
    }

    /**
     * Can be used to make nonblocking operation implementation. Typical
     * scenario is: <BR>
     * produceNoBlocking(new NoBlockingAction("Button pushing") {<BR>
     * public Object doAction(Object param) {<BR>
     * push();<BR>
     * return null;<BR>
     * }<BR>
     * });<BR>
     */
    protected abstract class NoBlockingAction<R, P> implements Action<R, P> {

        String description;
        Exception exception;

        /**
         * Constructs a NoBlockingAction object.
         *
         * @param description an action description.
         */
        public NoBlockingAction(String description) {
            this.description = description;
            exception = null;
        }

        @Override
        public final R launch(P param) {
            R result = null;
            try {
                result = doAction(param);
            } catch (Exception e) {
                exception = e;
            }
            return result;
        }

        /**
         * Performs a mapping action.
         *
         * @param param an action parameter.
         * @return an action result.
         */
        public abstract R doAction(P param);

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return "NoBlockingAction{" + "description=" + description + ", exception=" + exception + '}';
        }

        /**
         * Specifies the exception.
         *
         * @param e an exception.
         * @see #getException
         */
        protected void setException(Exception e) {
            exception = e;
        }

        /**
         * Returns an exception occurred during the action execution.
         *
         * @return an exception.
         * @see #setException
         */
        public Exception getException() {
            return exception;
        }
    }

    /**
     * Can be used to simplify non-primitive type component's methods mapping.
     * Like this: <BR>
     * public Color getBackground() { <BR>
     * return((Color)runMapping(new MapAction("getBackground") { <BR>
     * public Object map() { <BR>
     * return ((Component)getSource()).getBackground(); <BR>
     * } <BR>
     * })); <BR>
     * } <BR>
     *
     * @see #runMapping(Operator.MapAction)
     */
    protected abstract class MapAction<R> extends QueueTool.QueueAction<R> {

        /**
         * Constructs a MapAction object.
         *
         * @param description an action description.
         */
        public MapAction(String description) {
            super(description);
        }

        @Override
        public final R launch() throws Exception {
            return map();
        }

        /**
         * Executes a map action.
         *
         * @return an action result.
         * @throws Exception
         */
        public abstract R map() throws Exception;
    }

    /**
     * Can be used to simplify char component's methods mapping.
     *
     * @see #runMapping(Operator.MapCharacterAction)
     */
    protected abstract class MapCharacterAction extends QueueTool.QueueAction<Object> {

        /**
         * Constructs a MapCharacterAction object.
         *
         * @param description an action description.
         */
        public MapCharacterAction(String description) {
            super(description);
        }

        @Override
        public final Object launch() throws Exception {
            return map();
        }

        /**
         * Executes a map action.
         *
         * @return an action result.
         * @throws Exception
         */
        public abstract char map() throws Exception;
    }

    /**
     * Can be used to simplify byte component's methods mapping.
     *
     * @see #runMapping(Operator.MapByteAction)
     */
    protected abstract class MapByteAction extends QueueTool.QueueAction<Object> {

        /**
         * Constructs a MapByteAction object.
         *
         * @param description an action description.
         */
        public MapByteAction(String description) {
            super(description);
        }

        @Override
        public final Object launch() throws Exception {
            return map();
        }

        /**
         * Executes a map action.
         *
         * @return an action result.
         * @throws Exception
         */
        public abstract byte map() throws Exception;
    }

    /**
     * Can be used to simplify int component's methods mapping.
     *
     * @see #runMapping(Operator.MapIntegerAction)
     */
    protected abstract class MapIntegerAction extends QueueTool.QueueAction<Object> {

        /**
         * Constructs a MapIntegerAction object.
         *
         * @param description an action description.
         */
        public MapIntegerAction(String description) {
            super(description);
        }

        @Override
        public final Object launch() throws Exception {
            return map();
        }

        /**
         * Executes a map action.
         *
         * @return an action result.
         * @throws Exception
         */
        public abstract int map() throws Exception;
    }

    /**
     * Can be used to simplify long component's methods mapping.
     *
     * @see #runMapping(Operator.MapLongAction)
     */
    protected abstract class MapLongAction extends QueueTool.QueueAction<Object> {

        /**
         * Constructs a MapLongAction object.
         *
         * @param description an action description.
         */
        public MapLongAction(String description) {
            super(description);
        }

        @Override
        public final Object launch() throws Exception {
            return map();
        }

        /**
         * Executes a map action.
         *
         * @return an action result.
         * @throws Exception
         */
        public abstract long map() throws Exception;
    }

    /**
     * Can be used to simplify float component's methods mapping.
     *
     * @see #runMapping(Operator.MapFloatAction)
     */
    protected abstract class MapFloatAction extends QueueTool.QueueAction<Object> {

        /**
         * Constructs a MapFloatAction object.
         *
         * @param description an action description.
         */
        public MapFloatAction(String description) {
            super(description);
        }

        @Override
        public final Object launch() throws Exception {
            return map();
        }

        /**
         * Executes a map action.
         *
         * @return an action result.
         * @throws Exception
         */
        public abstract float map() throws Exception;
    }

    /**
     * Can be used to simplify double component's methods mapping.
     *
     * @see #runMapping(Operator.MapDoubleAction)
     */
    protected abstract class MapDoubleAction extends QueueTool.QueueAction<Object> {

        /**
         * Constructs a MapDoubleAction object.
         *
         * @param description an action description.
         */
        public MapDoubleAction(String description) {
            super(description);
        }

        @Override
        public final Object launch() throws Exception {
            return map();
        }

        /**
         * Executes a map action.
         *
         * @return an action result.
         * @throws Exception
         */
        public abstract double map() throws Exception;
    }

    /**
     * Can be used to simplify boolean component's methods mapping.
     *
     * @see #runMapping(Operator.MapBooleanAction)
     */
    protected abstract class MapBooleanAction extends QueueTool.QueueAction<Object> {

        /**
         * Constructs a MapBooleanAction object.
         *
         * @param description an action description.
         */
        public MapBooleanAction(String description) {
            super(description);
        }

        @Override
        public final Object launch() throws Exception {
            return map() ? Boolean.TRUE : Boolean.FALSE;
        }

        /**
         * Executes a map action.
         *
         * @return an action result.
         * @throws Exception
         */
        public abstract boolean map() throws Exception;
    }

    /**
     * Can be used to simplify void component's methods mapping.
     *
     * @see #runMapping(Operator.MapVoidAction)
     */
    protected abstract class MapVoidAction extends QueueTool.QueueAction<Object> {

        /**
         * Constructs a MapVoidAction object.
         *
         * @param description an action description.
         */
        public MapVoidAction(String description) {
            super(description);
        }

        @Override
        public final Object launch() throws Exception {
            map();
            return null;
        }

        /**
         * Executes a map action.
         *
         * @throws Exception
         */
        public abstract void map() throws Exception;
    }

    private static class NullOperator extends Operator {

        public NullOperator() {
            super();
        }

        @Override
        public Component getSource() {
            return null;
        }
    }
}
