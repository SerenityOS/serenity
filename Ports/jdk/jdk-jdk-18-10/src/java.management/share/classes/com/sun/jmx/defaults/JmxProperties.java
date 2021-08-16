/*
 * Copyright (c) 1999, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jmx.defaults;

import java.lang.System.Logger;

/**
 * This contains the property list defined for this
 * JMX implementation.
 *
 *
 * @since 1.5
 */
public class JmxProperties {

    // private constructor defined to "hide" the default public constructor
    private JmxProperties() {
    }

    // PUBLIC STATIC CONSTANTS
    //------------------------

    /**
     * References the property that specifies the directory where
     * the native libraries will be stored before the MLet Service
     * loads them into memory.
     * <p>
     * Property Name: <B>jmx.mlet.library.dir</B>
     */
    public static final String JMX_INITIAL_BUILDER =
            "javax.management.builder.initial";

    /**
     * References the property that specifies the directory where
     * the native libraries will be stored before the MLet Service
     * loads them into memory.
     * <p>
     * Property Name: <B>jmx.mlet.library.dir</B>
     */
    public static final String MLET_LIB_DIR = "jmx.mlet.library.dir";

    /**
     * References the property that specifies the full name of the JMX
     * specification implemented by this product.
     * <p>
     * Property Name: <B>jmx.specification.name</B>
     */
    public static final String JMX_SPEC_NAME = "jmx.specification.name";

    /**
     * References the property that specifies the version of the JMX
     * specification implemented by this product.
     * <p>
     * Property Name: <B>jmx.specification.version</B>
     */
    public static final String JMX_SPEC_VERSION = "jmx.specification.version";

    /**
     * References the property that specifies the vendor of the JMX
     * specification implemented by this product.
     * <p>
     * Property Name: <B>jmx.specification.vendor</B>
     */
    public static final String JMX_SPEC_VENDOR = "jmx.specification.vendor";

    /**
     * References the property that specifies the full name of this product
     * implementing the  JMX specification.
     * <p>
     * Property Name: <B>jmx.implementation.name</B>
     */
    public static final String JMX_IMPL_NAME = "jmx.implementation.name";

    /**
     * References the property that specifies the name of the vendor of this
     * product implementing the  JMX specification.
     * <p>
     * Property Name: <B>jmx.implementation.vendor</B>
     */
    public static final String JMX_IMPL_VENDOR = "jmx.implementation.vendor";

    /**
     * References the property that specifies the version of this product
     * implementing the  JMX specification.
     * <p>
     * Property Name: <B>jmx.implementation.version</B>
     */
    public static final String JMX_IMPL_VERSION = "jmx.implementation.version";

    /**
     * Logger name for MBean Server information.
     */
    public static final String MBEANSERVER_LOGGER_NAME =
            "javax.management.mbeanserver";

    /**
     * Logger for MBean Server information.
     */
    public static final Logger MBEANSERVER_LOGGER =
            System.getLogger(MBEANSERVER_LOGGER_NAME);

    /**
     * Logger name for MLet service information.
     */
    public static final String MLET_LOGGER_NAME =
            "javax.management.mlet";

    /**
     * Logger for MLet service information.
     */
    public static final Logger MLET_LOGGER =
            System.getLogger(MLET_LOGGER_NAME);

    /**
     * Logger name for Monitor information.
     */
    public static final String MONITOR_LOGGER_NAME =
            "javax.management.monitor";

    /**
     * Logger for Monitor information.
     */
    public static final Logger MONITOR_LOGGER =
            System.getLogger(MONITOR_LOGGER_NAME);

    /**
     * Logger name for Timer information.
     */
    public static final String TIMER_LOGGER_NAME =
            "javax.management.timer";

    /**
     * Logger for Timer information.
     */
    public static final Logger TIMER_LOGGER =
            System.getLogger(TIMER_LOGGER_NAME);

    /**
     * Logger name for Event Management information.
     */
    public static final String NOTIFICATION_LOGGER_NAME =
            "javax.management.notification";

    /**
     * Logger for Event Management information.
     */
    public static final Logger NOTIFICATION_LOGGER =
            System.getLogger(NOTIFICATION_LOGGER_NAME);

    /**
     * Logger name for Relation Service.
     */
    public static final String RELATION_LOGGER_NAME =
            "javax.management.relation";

    /**
     * Logger for Relation Service.
     */
    public static final Logger RELATION_LOGGER =
            System.getLogger(RELATION_LOGGER_NAME);

    /**
     * Logger name for Model MBean.
     */
    public static final String MODELMBEAN_LOGGER_NAME =
            "javax.management.modelmbean";

    /**
     * Logger for Model MBean.
     */
    public static final Logger MODELMBEAN_LOGGER =
            System.getLogger(MODELMBEAN_LOGGER_NAME);

    /**
     * Logger name for all other JMX classes.
     */
    public static final String MISC_LOGGER_NAME =
            "javax.management.misc";

    /**
     * Logger for all other JMX classes.
     */
    public static final Logger MISC_LOGGER =
            System.getLogger(MISC_LOGGER_NAME);
}
