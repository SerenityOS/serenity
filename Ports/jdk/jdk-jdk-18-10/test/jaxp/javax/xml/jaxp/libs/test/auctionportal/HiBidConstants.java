/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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
package test.auctionportal;

import static jaxp.library.JAXPTestUtilities.getPathByClassName;

/**
 * This is the Base test class provide basic support for Auction portal test.
 */
public class HiBidConstants {
    /**
     * XML source file directory.
     */
    public static final String XML_DIR = getPathByClassName(HiBidConstants.class, "content");

    /**
     * Golden validation files directory.
     */
    public static final String GOLDEN_DIR = getPathByClassName(HiBidConstants.class, "golden");

    /**
     * Name space for account operation.
     */
    public static final String PORTAL_ACCOUNT_NS = "http://www.auctionportal.org/Accounts";

    /**
     * JAXP schema language property name.
     */
    public static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

    /**
     * JAXP schema source property name.
     */
    public static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";

    /**
     * Name of system property JDK entity expansion limit
     */
    public static final String SP_ENTITY_EXPANSION_LIMIT = "jdk.xml.entityExpansionLimit";

    /**
     * Name of system property JDK maxOccur limit
     */
    public static final String SP_MAX_OCCUR_LIMIT = "jdk.xml.maxOccurLimit";
}
