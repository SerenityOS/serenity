/*
 * Copyright (c) 1997, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

/**
 *
 * Class to store and process a set of timeout values.
 *
 * @see #setDefault(String, long)
 * @see #getDefault(String)
 * @see #setTimeout(String, long)
 * @see #getTimeout(String)
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 */
public class Timeouts {

    private static final long DELTA_TIME = 100;
    private static final Timeouts defaults;
    private static double timeoutsScale = -1;

    private final Hashtable<String, Long> timeouts;

    /**
     * Creates empty Timeouts object.
     */
    public Timeouts() {
        super();
        timeouts = new Hashtable<>();
        setTimeout("Timeouts.DeltaTimeout", DELTA_TIME);
        try {
            load();
        } catch (IOException ignored) {
        }
    }

    /**
     * Stores default timeout value.
     *
     * @param name Timeout name.
     * @param newValue Timeout value.
     * @see #getDefault(String)
     * @see #initDefault(String, long)
     * @see #containsDefault(String)
     */
    public static void setDefault(String name, long newValue) {
        defaults.setTimeout(name, newValue);
    }

    /**
     * Sets default timeout value if it was not set before.
     *
     * @param name Timeout name.
     * @param newValue Timeout value.
     * @see #setDefault(String, long)
     * @see #getDefault(String)
     * @see #containsDefault(String)
     */
    public static void initDefault(String name, long newValue) {
        defaults.initTimeout(name, newValue);
    }

    /**
     * Gets default timeout value.
     *
     * @param name Timeout name.
     * @return Timeout value or -1 if timeout is not defined.
     * @see #setDefault(String, long)
     * @see #initDefault(String, long)
     * @see #containsDefault(String)
     */
    public static long getDefault(String name) {
        return defaults.getTimeout(name);
    }

    /**
     * Check that default timeout value was defined.
     *
     * @param name Timeout name.
     * @return True if timeout has been defined, false otherwise.
     * @see #setDefault(String, long)
     * @see #getDefault(String)
     * @see #initDefault(String, long)
     */
    public static boolean containsDefault(String name) {
        return defaults.contains(name);
    }

    static {
        defaults = new Timeouts();
    }

    /**
     * Loads default timeouts values.
     *
     * @param stream Stream to load timeouts from.
     * @see org.netbeans.jemmy.Timeouts#loadDefaults(String)
     * @see org.netbeans.jemmy.Timeouts#loadDefaults()
     * @exception IOException
     */
    public void loadDefaults(InputStream stream)
            throws IOException {
        defaults.load(stream);
    }

    /**
     * Loads default timeouts values from file.
     *
     * @param fileName File to load timeouts from.
     * @see org.netbeans.jemmy.Timeouts#loadDefaults(InputStream)
     * @see org.netbeans.jemmy.Timeouts#loadDefaults(String)
     * @exception IOException
     * @exception FileNotFoundException
     */
    public void loadDefaults(String fileName)
            throws FileNotFoundException, IOException {
        defaults.load(fileName);
    }

    /**
     * Loads default timeouts values. Uses jemmy.timeouts system property to get
     * timeouts file.
     *
     * @see org.netbeans.jemmy.Timeouts#loadDefaults(InputStream)
     * @see org.netbeans.jemmy.Timeouts#loadDefaults(String)
     * @exception IOException
     * @exception FileNotFoundException
     */
    public void loadDefaults()
            throws FileNotFoundException, IOException {
        defaults.load();
    }

    /**
     * Creates Timeout new object by name and getTimeout(name) value.
     *
     * @param name Timeout name.
     * @return a Timeout instance.
     */
    public Timeout create(String name) {
        return new Timeout(name, getTimeout(name));
    }

    /**
     * Create timeout for "Timeouts.DeltaTimeout" name.
     *
     * @return a Timeout instance.
     */
    public Timeout createDelta() {
        return create("Timeouts.DeltaTimeout");
    }

    /**
     * Checks if timeout has already been defined in this timeout instance.
     *
     * @param name Timeout name.
     * @return True if timeout has been defined, false otherwise.
     * @see #containsDefault(String)
     */
    public boolean contains(String name) {
        return timeouts.containsKey(name);
    }

    /**
     * Sets new timeout value.
     *
     * @param name Timeout name.
     * @param newValue Timeout value.
     * @return old timeout value
     * @see #getTimeout
     */
    public long setTimeout(String name, long newValue) {
        long oldValue = -1;
        if (contains(name)) {
            oldValue = getTimeout(name);
        }
        timeouts.put(name, newValue);
        return oldValue;
    }

    /**
     * Gets timeout value. It timeout was not defined in this instance, returns
     * default timeout value.
     *
     * @param name Timeout name.
     * @return Timeout value.
     * @see #getDefault(String)
     * @see #setTimeout
     */
    public long getTimeout(String name) {
        long timeout;
        if (contains(name) && timeouts.get(name) != null) {
            timeout = timeouts.get(name);
            timeout = (long) ((double) timeout * getTimeoutsScale());
        } else if (this != defaults) {
            timeout = getDefault(name);
        } else {
            timeout = -1;
        }
        return timeout;
    }

    /**
     * Gets "Timeouts.DeltaTimeout" timeout value.
     *
     * @return Timeout value.
     * @see #getDefault(String)
     */
    public long getDeltaTimeout() {
        return getTimeout("Timeouts.DeltaTimeout");
    }

    /**
     * Sets timeout value if it was not set before.
     *
     * @param name Timeout name.
     * @param newValue Timeout value.
     * @return old timeout value
     */
    public long initTimeout(String name, long newValue) {
        long result = getTimeout(name);
        if (!contains(name)) {
            setTimeout(name, newValue);
        }
        return result;
    }

    /**
     * Creates a copy of the current timeouts set.
     *
     * @return A copy.
     */
    public Timeouts cloneThis() {
        Timeouts t = new Timeouts();
        Enumeration<String> e = timeouts.keys();
        String name = "";
        while (e.hasMoreElements()) {
            name = e.nextElement();
            t.setTimeout(name,
                    getTimeout(name));
        }
        return t;
    }

    /**
     * Sleeps for the "name" timeout value. Can throw InterruptedException if
     * current thread was interrupted.
     *
     * @param name Timeout name.
     * @exception InterruptedException
     */
    public void eSleep(String name) throws InterruptedException {
        if (contains(name)
                || defaults.contains(name)) {
            Thread.sleep(getTimeout(name));
        }
    }

    /**
     * Sleeps for the "name" timeout value. Does not throw InterruptedException
     * anyway.
     *
     * @param name Timeout name.
     */
    public void sleep(String name) {
        create(name).sleep();
    }

    /**
     * Prints all defined timeouts.
     *
     * @param pw PrintWriter to print into.
     */
    public void print(PrintWriter pw) {
        Enumeration<String> e = timeouts.keys();
        String name = "";
        while (e.hasMoreElements()) {
            name = e.nextElement();
            pw.println(name + " = " + Long.toString(getTimeout(name)));
        }
        pw.println("Default values:");
        e = defaults.timeouts.keys();
        name = "";
        while (e.hasMoreElements()) {
            name = e.nextElement();
            if (!contains(name)) {
                pw.println(name + " = " + Long.toString(getDefault(name)));
            }
        }
    }

    /**
     * Prins all defined timeouts.
     *
     * @param ps PrintStream to print into.
     */
    public void print(PrintStream ps) {
        print(new PrintWriter(ps));
    }

    /**
     * Loads timeouts values.
     *
     * @param stream Stream to load timeouts from.
     * @see org.netbeans.jemmy.Timeouts#load(String)
     * @see org.netbeans.jemmy.Timeouts#load()
     * @exception IOException
     */
    public void load(InputStream stream)
            throws IOException {
        Properties props = new Properties();
        props.load(stream);
        Enumeration<?> propNames = props.propertyNames();
        long propValue;
        String propName = null;
        while (propNames.hasMoreElements()) {
            propName = (String) propNames.nextElement();
            propValue = Long.parseLong(props.getProperty(propName));
            setTimeout(propName, propValue);
        }
    }

    /**
     * Loads timeouts values from file.
     *
     * @param fileName File to load timeouts from.
     * @see org.netbeans.jemmy.Timeouts#load(InputStream)
     * @see org.netbeans.jemmy.Timeouts#load(String)
     * @exception IOException
     * @exception FileNotFoundException
     */
    public void load(String fileName)
            throws FileNotFoundException, IOException {
        try (FileInputStream fileInputStream = new FileInputStream(fileName)) {
            load(fileInputStream);
        }
    }

    /**
     * Loads timeouts values. Uses jemmy.timeouts system property to get
     * timeouts file.
     *
     * @see org.netbeans.jemmy.Timeouts#load(InputStream)
     * @see org.netbeans.jemmy.Timeouts#load(String)
     * @exception IOException
     * @exception FileNotFoundException
     */
    public void load()
            throws FileNotFoundException, IOException {
        if (System.getProperty("jemmy.timeouts") != null
                && !System.getProperty("jemmy.timeouts").equals("")) {
            load(System.getProperty("jemmy.timeouts"));
        }
    }

    /**
     * Loads debug timeouts values.
     *
     * @exception IOException
     */
    public void loadDebugTimeouts() throws IOException {
        load(getClass().getClassLoader().getResourceAsStream("org/netbeans/jemmy/debug.timeouts"));
    }

    /**
     * Get timeouts scale. Uses jemmy.timeouts.scale system property to get the
     * value.
     *
     * @return timeouts scale or 1 if the property is not set.
     */
    public static double getTimeoutsScale() {
        if (timeoutsScale == -1) {
            String s = System.getProperty("jemmy.timeouts.scale", "1");
            try {
                timeoutsScale = Double.parseDouble(s);
            } catch (NumberFormatException e) {
                timeoutsScale = 1;
            }
        }
        if (timeoutsScale < 0) {
            timeoutsScale = 1;
        }
        return timeoutsScale;
    }

    /**
     * This method is designed to be used by unit test for testing purpose.
     */
    static void resetTimeoutScale() {
        timeoutsScale = -1;
    }
}
