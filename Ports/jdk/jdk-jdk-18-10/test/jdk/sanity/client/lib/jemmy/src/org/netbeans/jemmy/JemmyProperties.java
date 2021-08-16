/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.netbeans.jemmy;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Stack;
import java.util.StringTokenizer;

import org.netbeans.jemmy.drivers.APIDriverInstaller;
import org.netbeans.jemmy.drivers.DefaultDriverInstaller;
import org.netbeans.jemmy.drivers.DriverInstaller;
import org.netbeans.jemmy.drivers.InputDriverInstaller;
import org.netbeans.jemmy.explorer.GUIBrowser;
import org.netbeans.jemmy.util.Platform;

/**
 *
 * Keeps default Jemmy properties.
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 *
 */
public class JemmyProperties {

    /**
     * The event queue model mask.
     *
     * @see #getCurrentDispatchingModel()
     * @see #setCurrentDispatchingModel(int)
     */
    public static final int QUEUE_MODEL_MASK = 1;

    /**
     * The robot using model mask.
     *
     * @see #getCurrentDispatchingModel()
     * @see #setCurrentDispatchingModel(int)
     */
    public static final int ROBOT_MODEL_MASK = 2;

    /**
     * Event shorcutting model mask. Should not be used together with robot
     * mask.
     *
     * @see #getCurrentDispatchingModel()
     * @see #setCurrentDispatchingModel(int)
     */
    public static final int SHORTCUT_MODEL_MASK = 4;

    /**
     * The robot using model mask.
     *
     * @see #getCurrentDispatchingModel()
     * @see #setCurrentDispatchingModel(int)
     */
    public static final int SMOOTH_ROBOT_MODEL_MASK = 8;

    private static final int DEFAULT_DRAG_AND_DROP_STEP_LENGTH = 100;
    private static final Stack<JemmyProperties> propStack = new Stack<>();

    Hashtable<String, Object> properties;

    /**
     *
     */
    protected JemmyProperties() {
        super();
        properties = new Hashtable<>();
        setProperty("timeouts", new Timeouts());
        setProperty("output", new TestOut());
        setProperty("resources", new BundleManager());
        setProperty("binding.map", new DefaultCharBindingMap());
        setProperty("dispatching.model", getDefaultDispatchingModel());
        setProperty("drag_and_drop.step_length", DEFAULT_DRAG_AND_DROP_STEP_LENGTH);
    }

    /**
     * Returns major version (like 1.0).
     *
     * @return a String representing the major version value.
     */
    public static String getMajorVersion() {
        return (extractValue(getProperties().getClass().
                getClassLoader().getResourceAsStream("org/netbeans/jemmy/version_info"),
                "Jemmy-MajorVersion"));
    }

    /**
     * Returns minor version (like 1).
     *
     * @return a String representing the minor version value.
     */
    public static String getMinorVersion() {
        return (extractValue(getProperties().getClass().
                getClassLoader().getResourceAsStream("org/netbeans/jemmy/version_info"),
                "Jemmy-MinorVersion"));
    }

    /**
     * Returns build (like 20011231 (yyyymmdd)).
     *
     * @return a String representing the build value.
     */
    public static String getBuild() {
        return (extractValue(getProperties().getClass().
                getClassLoader().getResourceAsStream("org/netbeans/jemmy/version_info"),
                "Jemmy-Build"));
    }

    /**
     * Returns full version string (like 1.0.1-20011231).
     *
     * @return a String representing the full version value.
     */
    public static String getFullVersion() {
        return (getMajorVersion() + "."
                + getMinorVersion() + "-"
                + getBuild());
    }

    /**
     * Returns version string (like 1.0.1).
     *
     * @return a String representing the short version value.
     */
    public static String getVersion() {
        return (getMajorVersion() + "."
                + getMinorVersion());
    }

    /**
     * Creates a copy of the current JemmyProperties object and pushes it into
     * the properties stack.
     *
     * @return New current properties.
     */
    public static JemmyProperties push() {
        return push(getProperties().cloneThis());
    }

    /**
     * Pops last pushed properties from the properties stack. If stack has just
     * one element, does nothing.
     *
     * @return Poped properties.
     */
    public static JemmyProperties pop() {
        JemmyProperties result = propStack.pop();
        if (propStack.isEmpty()) {
            propStack.push(result);
        }
        return result;
    }

    /**
     * Just like getProperties().getProperty(propertyName).
     *
     * @param propertyName a property key
     * @return a property value
     * @see #setCurrentProperty
     * @see #setCurrentTimeout
     */
    public static Object getCurrentProperty(String propertyName) {
        return getProperties().getProperty(propertyName);
    }

    /**
     * Just like getProperties().setProperty(propertyName, propertyValue).
     *
     * @param propertyName a property key
     * @param propertyValue a property value
     * @return previous property value
     * @see #getCurrentProperty
     * @see #getCurrentTimeout
     */
    public static Object setCurrentProperty(String propertyName, Object propertyValue) {
        return getProperties().setProperty(propertyName, propertyValue);
    }

    /**
     * Removes a property from current properties list.
     *
     * @param propertyName a property key.
     * @return previous property value
     */
    public static Object removeCurrentProperty(String propertyName) {
        return getProperties().removeProperty(propertyName);
    }

    /**
     * Returns the current key values.
     *
     * @return an array of Strings representing the current key values
     */
    public static String[] getCurrentKeys() {
        return getProperties().getKeys();
    }

    /**
     * Just like getProperties().getTimeouts().
     *
     * @return a Timeouts object representing the current timeouts.
     * @see #setCurrentTimeouts
     */
    public static Timeouts getCurrentTimeouts() {
        return getProperties().getTimeouts();
    }

    /**
     * Just like getProperties().setTimeouts(to).
     *
     * @param to New timeouts
     * @return old timeouts.
     * @see #getCurrentTimeouts
     */
    public static Timeouts setCurrentTimeouts(Timeouts to) {
        return getProperties().setTimeouts(to);
    }

    /**
     * Just like getProperties().getTimeouts().setTimeout(name, newValue).
     *
     * @param name a timeout name
     * @param newValue a timeout value
     * @return previous timeout value
     * @see #getCurrentTimeout
     */
    public static long setCurrentTimeout(String name, long newValue) {
        return getProperties().getTimeouts().setTimeout(name, newValue);
    }

    /**
     * Just like getProperties().getTimeouts().getTimeout(name).
     *
     * @param name a timeout name
     * @return a timeout value
     * @see #setCurrentTimeout
     */
    public static long getCurrentTimeout(String name) {
        return getProperties().getTimeouts().getTimeout(name);
    }

    /**
     * Just like getProperties().getTimeouts().initTimeout(name, newValue).
     *
     * @param name a timeout name
     * @param newValue a timeout value
     * @return a timeout value
     * @see #setCurrentTimeout
     */
    public static long initCurrentTimeout(String name, long newValue) {
        return getProperties().getTimeouts().initTimeout(name, newValue);
    }

    /**
     * Just like getProperties().getOutput().
     *
     * @return a TestOut object representing the current output.
     * @see #setCurrentOutput
     */
    public static TestOut getCurrentOutput() {
        return getProperties().getOutput();
    }

    /**
     * Just like getProperties().setOutput(out).
     *
     * @param out new output
     * @return a TestOut object representing the current output.
     * @see #getCurrentOutput
     */
    public static TestOut setCurrentOutput(TestOut out) {
        return getProperties().setOutput(out);
    }

    /**
     * Just like getProperties().getBundleManager().
     *
     * @return a BundleManager object representing the current bundle manager.
     * @see #setCurrentBundleManager
     */
    public static BundleManager getCurrentBundleManager() {
        return getProperties().getBundleManager();
    }

    /**
     * Just like getProperties().setBundleManager(resources).
     *
     * @param resources new BundleManager
     * @return a BundleManager object representing the current bundle manager.
     * @see #getCurrentBundleManager
     */
    public static BundleManager setCurrentBundleManager(BundleManager resources) {
        return getProperties().setBundleManager(resources);
    }

    /**
     * Just like getProperties().getBundleManager().getResource(key).
     *
     * @param key a resource key.
     * @return a resource value
     */
    public static String getCurrentResource(String key) {
        return getProperties().getBundleManager().getResource(key);
    }

    /**
     * Just like getProperties().getBundleManager().getResource(bundleID, key).
     *
     * @param key a resource key.
     * @param bundleID a bundle ID
     * @return a resource value
     */
    public static String getCurrentResource(String bundleID, String key) {
        return getProperties().getBundleManager().getResource(bundleID, key);
    }

    /**
     * Just like getProperties().getCharBindingMap().
     *
     * @return a CharBindingMap object representing the current char binding
     * map.
     * @see #setCurrentCharBindingMap
     */
    public static CharBindingMap getCurrentCharBindingMap() {
        return getProperties().getCharBindingMap();
    }

    /**
     * Just like getProperties().setCharBindingMap(map).
     *
     * @param map new CharBindingMap.
     * @return old CharBindingMap object.
     * @see #getCurrentCharBindingMap
     */
    public static CharBindingMap setCurrentCharBindingMap(CharBindingMap map) {
        return getProperties().setCharBindingMap(map);
    }

    /**
     * Returns the current dispatching model.
     *
     * @return Event dispatching model.
     * @see #getDispatchingModel()
     * @see #setCurrentDispatchingModel(int)
     * @see #QUEUE_MODEL_MASK
     * @see #ROBOT_MODEL_MASK
     */
    public static int getCurrentDispatchingModel() {
        return getProperties().getDispatchingModel();
    }

    /**
     * Defines event dispatching model. If (model & ROBOT_MODEL_MASK) != 0
     * java.awt.Robot class is used to reproduce user actions, otherwise actions
     * are reproduced by event posting. If (model & QUEUE_MODEL_MASK) != 0
     * actions are reproduced through event queue.
     *
     * @param model New dispatching model value.
     * @return Previous dispatching model value.
     * @see #setDispatchingModel(int)
     * @see #getCurrentDispatchingModel()
     * @see #QUEUE_MODEL_MASK
     * @see #ROBOT_MODEL_MASK
     * @see #initDispatchingModel(boolean, boolean)
     * @see #initDispatchingModel()
     */
    public static int setCurrentDispatchingModel(int model) {
        return getProperties().setDispatchingModel(model);
    }

    /**
     * Returns default event dispatching model.
     *
     * @return QUEUE_MODEL_MASK
     * @see #setCurrentDispatchingModel(int)
     * @see #QUEUE_MODEL_MASK
     * @see #ROBOT_MODEL_MASK
     */
    public static int getDefaultDispatchingModel() {
        return SHORTCUT_MODEL_MASK | QUEUE_MODEL_MASK;
    }

    /**
     * Returns the current drag and drop step length value.
     *
     * @return Pixel count to move mouse during one drag'n'drop step.
     * @see #getDragAndDropStepLength()
     * @see #setCurrentDragAndDropStepLength(int)
     */
    public static int getCurrentDragAndDropStepLength() {
        return getProperties().getDragAndDropStepLength();
    }

    /**
     * Specifies the current drag and drop step length value.
     *
     * @param model Pixel count to move mouse during one drag'n'drop step.
     * @return Previous value.
     * @see #setDragAndDropStepLength(int)
     * @see #getCurrentDragAndDropStepLength()
     */
    public static int setCurrentDragAndDropStepLength(int model) {
        return getProperties().setDragAndDropStepLength(model);
    }

    /**
     * Peeks upper JemmyProperties instance from stack.
     *
     * @return a JemmyProperties object representing the properties value.
     */
    public static JemmyProperties getProperties() {
        if (propStack.empty()) {
            propStack.add(new JemmyProperties());
        }
        return propStack.peek();
    }

    /**
     * Prints full version into standard output.
     *
     * @param argv Application args.
     */
    public static void main(String[] argv) {
        if (argv.length == 0) {
            System.out.println("Jemmy version : " + getVersion());
        } else if (argv.length == 1
                && argv[0].equals("-f")) {
            System.out.println("Jemmy full version : " + getFullVersion());
        } else if (argv.length > 0
                && argv[0].equals("-e")) {
            String[] newArgv = new String[argv.length - 1];
            System.arraycopy(argv, 1, newArgv, 0, argv.length - 1);
            GUIBrowser.main(newArgv);
        } else {
            System.out.println("Parameters: ");
            System.out.println("<no parameters> - report Jemmy version.");
            System.out.println("\"-f\" - report full jemmy version.");
        }
    }

    /**
     * Pushes properties stack.
     *
     * @param props a JemmyProperties instance to put into the stack head.
     * @return a JemmyProperties object.
     */
    protected static JemmyProperties push(JemmyProperties props) {
        return propStack.push(props);
    }

    static {
        setCurrentDispatchingModel(getDefaultDispatchingModel());
    }

    /**
     * Method to initialize timeouts and resources.
     *
     * @param prop_file File to get filenames from. <BR>
     * Can contain definition of variables TIMEOUTS_FILE - full path to timeouts
     * file, <BR>
     * RESOURCE_FILE - full path to resource file.
     * @see org.netbeans.jemmy.JemmyProperties#initProperties()
     */
    public void initProperties(String prop_file) {
        try {
            getOutput().printLine("Loading properties from " + prop_file + " file");
            Properties props = new Properties();
            try (FileInputStream fileStream = new FileInputStream(prop_file)) {
                props.load(fileStream);
            }
            if (props.getProperty("TIMEOUTS_FILE") != null
                    && !props.getProperty("TIMEOUTS_FILE").equals("")) {
                getOutput().printLine("Loading timeouts from " + props.getProperty("TIMEOUTS_FILE")
                        + " file");
                getTimeouts().loadDefaults(props.getProperty("TIMEOUTS_FILE"));
            }
            if (props.getProperty("RESOURCE_FILE") != null
                    && !props.getProperty("RESOURCE_FILE").equals("")) {
                getOutput().printLine("Loading resources from " + props.getProperty("RESOURCE_FILE")
                        + " file");
                getBundleManager().loadBundleFromFile(props.getProperty("RESOURCE_FILE"), "");
            }
        } catch (IOException e) {
            getOutput().printStackTrace(e);
        }
    }

    /**
     * Method to initialize timeouts and resources. <BR>
     * Uses jemmy.properties system property to find file.
     *
     * @see org.netbeans.jemmy.JemmyProperties#initProperties(String)
     */
    public void initProperties() {
        if (System.getProperty("jemmy.properties") != null
                && !System.getProperty("jemmy.properties").equals("")) {
            initProperties(System.getProperty("jemmy.properties"));
        } else {
            try {
                getTimeouts().load();
                getBundleManager().load();
            } catch (IOException e) {
                getOutput().printStackTrace(e);
            }
        }
    }

    /**
     * Initializes dispatching model.
     *
     * @param queue Notifies that event queue dispatching should be used.
     * @param robot Notifies that robot dispatching should be used.
     * @param shortcut Notifies that event shorcutting should be used.
     */
    public void initDispatchingModel(boolean queue, boolean robot, boolean shortcut) {
        initDispatchingModel(queue, robot, shortcut, false);
    }

    /**
     * Initializes dispatching model.
     *
     * @param queue Notifies that event queue dispatching should be used.
     * @param robot Notifies that robot dispatching should be used.
     * @param shortcut Notifies that event shorcutting should be used.
     */
    public void initDispatchingModel(boolean queue, boolean robot, boolean shortcut, boolean smooth) {
        int model = getDefaultDispatchingModel();
        getOutput().print("Reproduce user actions ");
        if (queue) {
            model = QUEUE_MODEL_MASK;
            getOutput().printLine("through event queue.");
        } else {
            model = model - (model & QUEUE_MODEL_MASK);
            getOutput().printLine("directly.");
        }
        getOutput().print("Use ");
        if (robot) {
            model = model | ROBOT_MODEL_MASK;
            getOutput().print("java.awt.Robot class");
        } else {
            model = model - (model & ROBOT_MODEL_MASK);
            getOutput().print("event dispatching");
        }
        if (smooth) {
            model = model | SMOOTH_ROBOT_MODEL_MASK;
        } else {
            model = model - (model & SMOOTH_ROBOT_MODEL_MASK);
        }
        getOutput().printLine(" to reproduce user actions");
        if (shortcut) {
            model = model | SHORTCUT_MODEL_MASK;
            getOutput().print("Shortcut");
        } else {
            model = model - (model & SHORTCUT_MODEL_MASK);
            getOutput().print("Dispatch");
        }
        getOutput().printLine(" test events");
        setDispatchingModel(model);
    }

    /**
     * Initializes dispatching model.
     *
     * @param queue Notifies that event queue dispatching should be used.
     * @param robot Notifies that robot dispatching should be used.
     */
    public void initDispatchingModel(boolean queue, boolean robot) {
        this.initDispatchingModel(queue, robot, false);
    }

    /**
     * Initializes dispatching model. Uses "jemmy.queue_dispatching" and
     * "jemmy.robot_dispatching" system properties to determine what model
     * should be used. Possible values for the both properties: <BR>
     * "off" - switch mode off. <BR>
     * "on" - switch mode on. <BR>
     * "" - use default value.
     *
     * @see #getDefaultDispatchingModel()
     */
    public void initDispatchingModel() {
        boolean qmask = ((getDefaultDispatchingModel() & QUEUE_MODEL_MASK) != 0);
        boolean rmask = ((getDefaultDispatchingModel() & ROBOT_MODEL_MASK) != 0);
        boolean srmask = ((getDefaultDispatchingModel() & SMOOTH_ROBOT_MODEL_MASK) != 0);
        boolean smask = ((getDefaultDispatchingModel() & SHORTCUT_MODEL_MASK) != 0);
        if (System.getProperty("jemmy.queue_dispatching") != null
                && !System.getProperty("jemmy.queue_dispatching").equals("")) {
            qmask = System.getProperty("jemmy.queue_dispatching").equals("on");
        }
        if (System.getProperty("jemmy.robot_dispatching") != null
                && !System.getProperty("jemmy.robot_dispatching").equals("")) {
            rmask = System.getProperty("jemmy.robot_dispatching").equals("on");
        }
        if (System.getProperty("jemmy.smooth_robot_dispatching") != null
                && !System.getProperty("jemmy.smooth_robot_dispatching").equals("")) {
            srmask = System.getProperty("jemmy.smooth_robot_dispatching").equals("on");
        }
        if (System.getProperty("jemmy.shortcut_events") != null
                && !System.getProperty("jemmy.shortcut_events").equals("")) {
            smask = System.getProperty("jemmy.shortcut_events").equals("on");
        }
        initDispatchingModel(qmask, rmask, smask, srmask);
    }

    /**
     * Inits properties and dispatching model from system environment variables.
     *
     * @see #initProperties()
     * @see #initDispatchingModel()
     */
    public void init() {
        initProperties();
        initDispatchingModel();
    }

    /**
     * Returns timeouts.
     *
     * @return the Timeouts value.
     * @see #setTimeouts
     */
    public Timeouts getTimeouts() {
        return (Timeouts) getProperty("timeouts");
    }

    /**
     * Changes timeouts.
     *
     * @param to new timeouts.
     * @return old timeouts.
     * @see #getTimeouts
     */
    public Timeouts setTimeouts(Timeouts to) {
        return (Timeouts) setProperty("timeouts", to);
    }

    /**
     * Changes a timeouts value.
     *
     * @param name Timeout name
     * @param newValue New timeout value
     * @return previous timeout value
     * @see #getTimeout
     */
    public long setTimeout(String name, long newValue) {
        return getTimeouts().setTimeout(name, newValue);
    }

    /**
     * Returns a timeouts value.
     *
     * @param name Timeout name
     * @return a timeout value
     * @see #setTimeout
     */
    public long getTimeout(String name) {
        return getTimeouts().getTimeout(name);
    }

    /**
     * Inits a timeouts value.
     *
     * @param name Timeout name
     * @param newValue New timeout value
     * @return a timeout value
     */
    public long initTimeout(String name, long newValue) {
        return getTimeouts().initTimeout(name, newValue);
    }

    /**
     * Returns output.
     *
     * @return a TestOut object representing the output value
     * @see #setOutput
     */
    public TestOut getOutput() {
        return (TestOut) getProperty("output");
    }

    /**
     * Changes output.
     *
     * @param out new output.
     * @return old output.
     * @see #getOutput
     */
    public TestOut setOutput(TestOut out) {
        return (TestOut) setProperty("output", out);
    }

    /**
     * Returns bundle manager.
     *
     * @return a BundleManager object representing the bundle manager value.
     * @see #setBundleManager
     */
    public BundleManager getBundleManager() {
        return (BundleManager) getProperty("resources");
    }

    /**
     * Changes bundle manager.
     *
     * @param resources new bundle manager.
     * @return old bundle manager
     * @see #getBundleManager
     */
    public BundleManager setBundleManager(BundleManager resources) {
        return (BundleManager) setProperty("resources", resources);
    }

    /**
     * Returns resource value.
     *
     * @param key Resource key.
     * @return resource value
     */
    public String getResource(String key) {
        return getBundleManager().getResource(key);
    }

    /**
     * Returns resource value from the specified bundle.
     *
     * @param bundleID Id of a bundle to get resource from.
     * @param key Resource key.
     * @return resource value
     */
    public String getResource(String bundleID, String key) {
        return getBundleManager().getResource(bundleID, key);
    }

    /**
     * Returns char binding map.
     *
     * @return the char binding map.
     * @see #setCharBindingMap
     */
    public CharBindingMap getCharBindingMap() {
        return (CharBindingMap) getProperty("binding.map");
    }

    /**
     * Changes char binding map.
     *
     * @param map new char binding map.
     * @return old char binding map.
     * @see #getCharBindingMap
     */
    public CharBindingMap setCharBindingMap(CharBindingMap map) {
        return (CharBindingMap) setProperty("binding.map", map);
    }

    /**
     * Returns the dispatching model.
     *
     * @return Event dispatching model.
     * @see #getCurrentDispatchingModel()
     * @see #setDispatchingModel(int)
     * @see #QUEUE_MODEL_MASK
     * @see #ROBOT_MODEL_MASK
     */
    public int getDispatchingModel() {
        return (Integer) getProperty("dispatching.model");
    }

    private static DriverInstaller getDriverInstaller(int model) {
        String name = System.getProperty("jemmy.drivers.installer");
        DriverInstaller installer = null;
        try {
            if (name != null && !(name.length() == 0)) {
                installer = (DriverInstaller) new ClassReference(name).newInstance(null, null);
            }
        } catch (ClassNotFoundException
                | IllegalAccessException
                | NoSuchMethodException
                | InstantiationException
                | InvocationTargetException e) {
            getCurrentOutput().printLine("Cannot init driver installer:");
            getCurrentOutput().printStackTrace(e);
        }
        if (installer == null) {
            if (Platform.isOSX()) {
                installer = new APIDriverInstaller((model & SHORTCUT_MODEL_MASK) != 0);
            } else {
                installer = new DefaultDriverInstaller((model & SHORTCUT_MODEL_MASK) != 0);
            }
        }
        getCurrentOutput().printLine("Using " + installer.getClass().getName() + " driver installer");
        return installer;
    }

    /**
     * Specifies the dispatching model value.
     *
     * @param model New dispatching model value.
     * @return Previous dispatching model value.
     * @see #setCurrentDispatchingModel(int)
     * @see #getDispatchingModel()
     * @see #QUEUE_MODEL_MASK
     * @see #ROBOT_MODEL_MASK
     */
    public int setDispatchingModel(int model) {
        new InputDriverInstaller((model & ROBOT_MODEL_MASK) == 0, (model & SMOOTH_ROBOT_MODEL_MASK) != 0).install();
        getDriverInstaller(model).install();
        return (Integer) setProperty("dispatching.model", model);
    }

    /**
     * Returns the drag and drop step length value.
     *
     * @return Pixel count to move mouse during one drag'n'drop step.
     * @see #getCurrentDragAndDropStepLength()
     * @see #setDragAndDropStepLength(int)
     */
    public int getDragAndDropStepLength() {
        return (Integer) getProperty("drag_and_drop.step_length");
    }

    /**
     * Specifies the drag and drop step length value.
     *
     * @param length Pixel count to move mouse during one drag'n'drop step.
     * @return Previous value.
     * @see #setCurrentDragAndDropStepLength(int)
     * @see #getDragAndDropStepLength()
     */
    public int setDragAndDropStepLength(int length) {
        return (Integer) setProperty("drag_and_drop.step_length", length);
    }

    /**
     * Checks if "name" propery currently has a value.
     *
     * @param name Property name. Should by unique.
     * @return true if property was defined.
     * @see #setProperty(String, Object)
     * @see #getProperty(String)
     */
    public boolean contains(String name) {
        return properties.containsKey(name);
    }

    /**
     * Saves object as a static link to be used by other objects.
     *
     * @param name Property name. Should by unique.
     * @param newValue Property value.
     * @return Previous value of "name" property.
     * @see #setCurrentProperty(String, Object)
     * @see #getProperty(String)
     * @see #contains(String)
     */
    public Object setProperty(String name, Object newValue) {
        Object oldValue = null;
        if (contains(name)) {
            oldValue = properties.get(name);
            properties.remove(name);
        }
        properties.put(name, newValue);
        return oldValue;
    }

    /**
     * Returns the property value.
     *
     * @param name Property name. Should by unique.
     * @return Property value stored by setProperty(String, Object) method.
     * @see #getCurrentProperty(String)
     * @see #setProperty(String, Object)
     * @see #contains(String)
     */
    public Object getProperty(String name) {
        if (contains(name)) {
            return properties.get(name);
        } else {
            return null;
        }
    }

    /**
     * Removes the property.
     *
     * @param name A name of the property to be removed.
     * @return previous property value
     */
    public Object removeProperty(String name) {
        if (contains(name)) {
            return properties.remove(name);
        } else {
            return null;
        }
    }

    /**
     * Returns the key values.
     *
     * @return an array of Strings representing the key values.
     */
    public String[] getKeys() {
        Enumeration<String> keys = properties.keys();
        String[] result = new String[properties.size()];
        int i = 0;
        while (keys.hasMoreElements()) {
            result[i] = keys.nextElement();
            i++;
        }
        return result;
    }

    /**
     * Copy all properties from this instance into another.
     *
     * @param properties a JemmyProperties instance to copy properties into.
     */
    public void copyTo(JemmyProperties properties) {
        String[] keys = getKeys();
        for (String key : keys) {
            properties.setProperty(key, getProperty(key));
        }
        //some should be cloned
        properties.setTimeouts(getTimeouts().cloneThis());
        properties.setBundleManager(getBundleManager().cloneThis());
    }

    /**
     * Creates an exact copy on this instance.
     *
     * @return new JemmyProperties object.
     */
    protected JemmyProperties cloneThis() {
        JemmyProperties result = new JemmyProperties();
        copyTo(result);
        return result;
    }

    private static String extractValue(InputStream stream, String varName) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            StringTokenizer token;
            String nextLine;
            while ((nextLine = reader.readLine()) != null) {
                token = new StringTokenizer(nextLine, ":");
                String nextToken = token.nextToken();
                if (nextToken.trim().equals(varName)) {
                    return token.nextToken().trim();
                }
            }
            return "";
        } catch (IOException e) {
            getCurrentOutput().printStackTrace(e);
            return "";
        }
    }

}
