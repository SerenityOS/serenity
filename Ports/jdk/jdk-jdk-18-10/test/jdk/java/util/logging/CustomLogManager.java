/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

import java.io.*;
import java.util.*;
import java.util.logging.*;

/*
 * Custom LogManager implementation to verify that the implementation delegates
 * to the LogManager subclass to register both system logger and user logger.
 *
 * The LogManager implementation is the one configuring the logger's property
 * such as level, handler, etc.
 */
public class CustomLogManager extends LogManager {
    static LogManager INSTANCE;
    Map<String,Logger> namedLoggers = new HashMap<>();
    Properties props = initConfig();
    public CustomLogManager() {
        if (INSTANCE != null) {
            throw new RuntimeException("CustomLogManager already created");
        }
        INSTANCE = this;
    }

    private boolean useParentHandlers(String loggerName) {
        String s = props.getProperty(loggerName + ".useParentHandlers");
        if (s == null)
            return true;   // default is true

        s = s.toLowerCase();
        if (s.equals("true") || s.equals("1")) {
           return true;
        } else if (s.equals("false") || s.equals("0")) {
           return false;
        }
        return true;
    }

    public synchronized boolean addLogger(Logger logger) {
        String name = logger.getName();
        if (namedLoggers.containsKey(name)) {
            return false;
        }
        namedLoggers.put(name, logger);
        // set level
        if (props.get(name + ".level") != null) {
            logger.setLevel(Level.parse(props.getProperty(name + ".level")));
        }
        // add handlers
        if (props.get(name + ".handlers") != null && logger.getHandlers().length == 0) {
            logger.addHandler(new CustomHandler());
        }
        if (!useParentHandlers(name)) {
            logger.setUseParentHandlers(false);
        }
        // add parent loggers
        int ix = 1;
        for (;;) {
            int ix2 = name.indexOf(".", ix);
            if (ix2 < 0) {
                break;
            }
            String pname = name.substring(0, ix2);
            if (props.get(pname + ".level") != null ||
                props.get(pname + ".handlers") != null) {
                // This pname has a level/handlers definition.
                // Make sure it exists.
                //
                // The test doesn't set the parent for simplicity.
                if (!namedLoggers.containsKey(pname)) {
                    Logger parent = Logger.getLogger(pname);
                    if (!useParentHandlers(pname)) {
                        parent.setUseParentHandlers(false);
                    }
                }
            }
            ix = ix2 + 1;
        }
        return true;
    }

    public synchronized Logger getLogger(String name) {
        return namedLoggers.get(name);
    }

    public synchronized Enumeration<String> getLoggerNames() {
        return Collections.enumeration(namedLoggers.keySet());
    }

    public String getProperty(String name) {
        return props.getProperty(name);
    }

    public void readConfiguration() {
        // do nothing
    }

    public void readConfiguration(InputStream ins) {
        // do nothing
    }

    private Properties initConfig() {
        Properties props = new Properties();
        props.put(".level", "CONFIG");
        props.put("CustomLogManagerTest.level", "WARNING");
        props.put("CustomLogManagerTest.handlers", "CustomLogManager$CustomHandler");
        props.put("SimpleLogManager.level", "INFO");
        props.put("SimpleLogManager.handlers", "CustomLogManager$CustomHandler");
        props.put("CustomLogManager$CustomHandler.level", "WARNING");
        props.put(".handlers", "CustomLogManager$CustomHandler");
        props.put("org.foo.bar.level", "SEVERE");
        props.put("org.foo.bar.useParentHandlers", "true");
        props.put("org.foo.handlers", "CustomLogManager$CustomHandler");
        props.put("org.foo.useParentHandlers", "false");
        props.put("org.openjdk.level", "SEVERE");
        props.put("org.openjdk.handlers", "CustomLogManager$CustomHandler");
        props.put("org.openjdk.core.level", "INFO");
        props.put("org.openjdk.core.useParentHandlers", "false");

        return props;
    }
    public static void checkLogger(String name) {
        checkLogger(name, null);
    }

    public static void checkLogger(String name, String resourceBundleName) {
        Logger logger = INSTANCE.getLogger(name);
        if (logger == null) {
            throw new RuntimeException("Logger \"" + name + "\" not exist");
        }
        System.out.format("Logger \"%s\" level=%s handlers=%s resourcebundle=%s useParentHandlers=%s%n",
            name, logger.getLevel(),
            Arrays.toString(logger.getHandlers()),
            logger.getResourceBundleName(),
            logger.getUseParentHandlers());
        String rb = logger.getResourceBundleName();
        if (rb != resourceBundleName && (rb == null || rb.equals(resourceBundleName))) {
            throw new RuntimeException("Logger \"" + name +
                "\" unexpected resource bundle: " + rb);
        }

        String value = INSTANCE.getProperty(name + ".level");
        String level = logger.getLevel() != null ? logger.getLevel().getName() : null;
        if (level != value && (level == null || level.equals(value))) {
            throw new RuntimeException("Logger \"" + name + "\" unexpected level: " + level);
        }

        Handler[] handlers = logger.getHandlers();
        String hdl = INSTANCE.getProperty(name + ".handlers");
        if ((hdl == null && handlers.length != 0) ||
            (hdl != null && handlers.length != 1)) {
            throw new RuntimeException("Logger \"" + name + "\" unexpected handler: " +
                Arrays.toString(handlers));
        }

        String s = INSTANCE.getProperty(name + ".useParentHandlers");
        boolean uph = (s != null && s.equals("false")) ? false : true;
        if (logger.getUseParentHandlers() != uph) {
            throw new RuntimeException("Logger \"" + name + "\" unexpected useParentHandlers: " +
                logger.getUseParentHandlers());
        }
        checkParents(name);
    }

    private static void checkParents(String name) {
        int ix = 1;
        for (;;) {
            int ix2 = name.indexOf(".", ix);
            if (ix2 < 0) {
                break;
            }
            String pname = name.substring(0, ix2);
            if (INSTANCE.getProperty(pname + ".level") != null ||
                INSTANCE.getProperty(pname + ".handlers") != null) {
                // This pname has a level/handlers definition.
                // Make sure it exists.
                checkLogger(pname);
            }
            ix = ix2 + 1;
        }
    }

    // only CustomLogManager can create an instance of CustomHandler
    private class CustomHandler extends StreamHandler {
    }
}
