/*
 * Copyright (c) 1999, 2008, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Used for storing default values used by JMX services.
 *
 * @since 1.5
 */
public class ServiceName {

    // private constructor defined to "hide" the default public constructor
    private ServiceName() {
    }

    /**
     * The object name of the MBeanServer delegate object
     * <BR>
     * The value is <CODE>JMImplementation:type=MBeanServerDelegate</CODE>.
     */
    public static final String DELEGATE =
        "JMImplementation:type=MBeanServerDelegate" ;

    /**
     * The default key properties for registering the class loader of the
     * MLet service.
     * <BR>
     * The value is <CODE>type=MLet</CODE>.
     */
    public static final String MLET = "type=MLet";

    /**
     * The default domain.
     * <BR>
     * The value is <CODE>DefaultDomain</CODE>.
     */
    public static final String DOMAIN = "DefaultDomain";

    /**
     * The name of the JMX specification implemented by this product.
     * <BR>
     * The value is <CODE>Java Management Extensions</CODE>.
     */
    public static final String JMX_SPEC_NAME = "Java Management Extensions";

    /**
     * The version of the JMX specification implemented by this product.
     * <BR>
     * The value is <CODE>1.4</CODE>.
     */
    public static final String JMX_SPEC_VERSION = "1.4";

    /**
     * The vendor of the JMX specification implemented by this product.
     * <BR>
     * The value is <CODE>Oracle Corporation</CODE>.
     */
    public static final String JMX_SPEC_VENDOR = "Oracle Corporation";

    /**
     * The name of this product implementing the  JMX specification.
     * <BR>
     * The value is <CODE>JMX</CODE>.
     */
    public static final String JMX_IMPL_NAME = "JMX";

    /**
     * The name of the vendor of this product implementing the
     * JMX specification.
     * <BR>
     * The value is <CODE>Oracle Corporation</CODE>.
     */
    public static final String JMX_IMPL_VENDOR = "Oracle Corporation";
}
