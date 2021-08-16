/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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

package sun.awt;

import java.io.*;

import java.util.*;
import sun.util.logging.PlatformLogger;

/*
 * Internal class that manages sun.awt.Debug settings.
 * Settings can be specified on a global, per-package,
 * or per-class level.
 *
 * Properties affecting the behaviour of the Debug class are
 * loaded from the awtdebug.properties file at class load
 * time. The properties file is assumed to be in the
 * user.home directory. A different file can be used
 * by setting the awtdebug.properties system property.
 *      e.g. java -Dawtdebug.properties=foo.properties
 *
 * Only properties beginning with 'awtdebug' have any
 * meaning-- all other properties are ignored.
 *
 * You can override the properties file by specifying
 * 'awtdebug' props as system properties on the command line.
 *      e.g. java -Dawtdebug.trace=true
 * Properties specific to a package or a class can be set
 * by qualifying the property names as follows:
 *      awtdebug.<property name>.<class or package name>
 * So for example, turning on tracing in the com.acme.Fubar
 * class would be done as follows:
 *      awtdebug.trace.com.acme.Fubar=true
 *
 * Class settings always override package settings, which in
 * turn override global settings.
 *
 * Addition from July, 2007.
 *
 * After the fix for 4638447 all the usage of DebugHelper
 * classes in Java code are replaced with the corresponding
 * Java Logging API calls. This file is now used only to
 * control native logging.
 *
 * To enable native logging you should set the following
 * system property to 'true': sun.awt.nativedebug. After
 * the native logging is enabled, the actual debug settings
 * are read the same way as described above (as before
 * the fix for 4638447).
 */
public final class DebugSettings {
    private static final PlatformLogger log = PlatformLogger.getLogger("sun.awt.debug.DebugSettings");

    /* standard debug property key names */
    static final String PREFIX = "awtdebug";
    static final String PROP_FILE = "properties";

    /* default property settings */
    private static final String[] DEFAULT_PROPS = {
        "awtdebug.assert=true",
        "awtdebug.trace=false",
        "awtdebug.on=true",
        "awtdebug.ctrace=false"
    };

    /* global instance of the settings object */
    private static final DebugSettings instance = new DebugSettings();

    private final Properties props = new Properties();

    static synchronized void init() {
        if (!instance.props.isEmpty()) {
            return;
        }
        NativeLibLoader.loadLibraries();
        instance.loadProperties();
        instance.loadNativeSettings();
    }

    public static DebugSettings getInstance() {
        return instance;
    }

    /*
     * Load debug properties from file, then override
     * with any command line specified properties
     */
    @SuppressWarnings("removal")
    private synchronized void loadProperties() {
        // setup initial properties
        java.security.AccessController.doPrivileged(
            new java.security.PrivilegedAction<Void>() {
                public Void run() {
                    loadDefaultProperties();
                    loadFileProperties();
                    loadSystemProperties();
                    return null;
                }
            });

        // echo the initial property settings to stdout
        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine("DebugSettings:\n{0}", this);
        }
    }

    public String toString() {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        PrintStream pout = new PrintStream(bout);
        for (String key : props.stringPropertyNames()) {
            String value = props.getProperty(key, "");
            pout.println(key + " = " + value);
        }
        return new String(bout.toByteArray());
    }

    /*
     * Sets up default property values
     */
    @SuppressWarnings("deprecation")
    private void loadDefaultProperties() {
        // is there a more inefficient way to setup default properties?
        // maybe, but this has got to be close to 100% non-optimal
        try {
            for ( int nprop = 0; nprop < DEFAULT_PROPS.length; nprop++ ) {
                StringBufferInputStream in = new StringBufferInputStream(DEFAULT_PROPS[nprop]);
                props.load(in);
                in.close();
            }
        } catch(IOException ioe) {
        }
    }

    /*
     * load properties from file, overriding defaults
     */
    private void loadFileProperties() {
        String          propPath;
        Properties      fileProps;

        // check if the user specified a particular settings file
        propPath = System.getProperty(PREFIX + "." + PROP_FILE, "");
        if (propPath.isEmpty()) {
        // otherwise get it from the user's home directory
            propPath = System.getProperty("user.home", "") +
                        File.separator +
                        PREFIX + "." + PROP_FILE;
        }

        File    propFile = new File(propPath);
        try {
            println("Reading debug settings from '" + propFile.getCanonicalPath() + "'...");
            FileInputStream     fin = new FileInputStream(propFile);
            props.load(fin);
            fin.close();
        } catch ( FileNotFoundException fne ) {
            println("Did not find settings file.");
        } catch ( IOException ioe ) {
            println("Problem reading settings, IOException: " + ioe.getMessage());
        }
    }

    /*
     * load properties from system props (command line spec'd usually),
     * overriding default or file properties
     */
    private void loadSystemProperties() {
        // override file properties with system properties
        Properties sysProps = System.getProperties();
        for (String key : sysProps.stringPropertyNames()) {
            String value = sysProps.getProperty(key,"");
            // copy any "awtdebug" properties over
            if ( key.startsWith(PREFIX) ) {
                props.setProperty(key, value);
            }
        }
    }

    /**
     * Gets named boolean property
     * @param key       Name of property
     * @param defval    Default value if property does not exist
     * @return boolean value of the named property
     */
    public synchronized boolean getBoolean(String key, boolean defval) {
        String  value = getString(key, String.valueOf(defval));
        return value.equalsIgnoreCase("true");
    }

    /**
     * Gets named integer property
     * @param key       Name of property
     * @param defval    Default value if property does not exist
     * @return integer value of the named property
     */
    public synchronized int getInt(String key, int defval) {
        String  value = getString(key, String.valueOf(defval));
        return Integer.parseInt(value);
    }

    /**
     * Gets named String property
     * @param key       Name of property
     * @param defval    Default value if property does not exist
     * @return string value of the named property
     */
    public synchronized String getString(String key, String defval) {
        String  actualKeyName = PREFIX + "." + key;
        String  value = props.getProperty(actualKeyName, defval);
        //println(actualKeyName+"="+value);
        return value;
    }

    private synchronized List<String> getPropertyNames() {
        List<String> propNames = new LinkedList<>();
        // remove global prefix from property names
        for (String propName : props.stringPropertyNames()) {
            propName = propName.substring(PREFIX.length()+1);
            propNames.add(propName);
        }
        return propNames;
    }

    private void println(Object object) {
        if (log.isLoggable(PlatformLogger.Level.FINER)) {
            log.finer(object.toString());
        }
    }

    private static final String PROP_CTRACE = "ctrace";
    private static final int PROP_CTRACE_LEN = PROP_CTRACE.length();

    private synchronized native void setCTracingOn(boolean enabled);
    private synchronized native void setCTracingOn(boolean enabled, String file);
    private synchronized native void setCTracingOn(boolean enabled, String file, int line);

    private void loadNativeSettings() {
        boolean        ctracingOn;

        ctracingOn = getBoolean(PROP_CTRACE, false);
        setCTracingOn(ctracingOn);

        //
        // Filter out file/line ctrace properties from debug settings
        //
        List<String> traces = new LinkedList<>();

        for (String key : getPropertyNames()) {
            if (key.startsWith(PROP_CTRACE) && key.length() > PROP_CTRACE_LEN) {
                traces.add(key);
            }
        }

        // sort traces list so file-level traces will be before line-level ones
        Collections.sort(traces);

        //
        // Setup the trace points
        //
        for (String key : traces) {
            String        trace = key.substring(PROP_CTRACE_LEN+1);
            String        filespec;
            String        linespec;
            int           delim= trace.indexOf('@');
            boolean       enabled;

            // parse out the filename and linenumber from the property name
            filespec = delim != -1 ? trace.substring(0, delim) : trace;
            linespec = delim != -1 ? trace.substring(delim+1) : "";
            enabled = getBoolean(key, false);
            //System.out.println("Key="+key+", File="+filespec+", Line="+linespec+", Enabled="+enabled);

            if ( linespec.length() == 0 ) {
            // set file specific trace setting
                    setCTracingOn(enabled, filespec);
            } else {
            // set line specific trace setting
                int        linenum = Integer.parseInt(linespec, 10);
                setCTracingOn(enabled, filespec, linenum);
            }
        }
    }
}
