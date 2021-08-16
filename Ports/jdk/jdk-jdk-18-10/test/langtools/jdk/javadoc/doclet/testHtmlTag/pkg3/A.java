/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

package pkg3;

import java.io.Serializable;

public class A {
   /**
     * <p>
     * Factory that creates new <code>javax.xml.datatype</code>
     * <code>Object</code>s that map XML to/from Java <code>Object</code>s.</p>
     *
     * <p id="DatatypeFactory.newInstance">
     * A new instance of the <code>DatatypeFactory</code> is created through the
     * {@link #newInstance()} method that uses the following implementation
     * resolution mechanisms to determine an implementation:</p>
     * <ol>
     * <li>
     * If the system property specified by {@link #DATATYPEFACTORY_PROPERTY},
     * "<code>javax.xml.datatype.DatatypeFactory</code>", exists, a class with
     * the name of the property value is instantiated. Any Exception thrown
     * during the instantiation process is wrapped as a
     * {@link IllegalStateException}.
     * </li>
     * <li>
     * If the file ${JAVA_HOME}/lib/jaxp.properties exists, it is loaded in a
     * {@link java.util.Properties} <code>Object</code>. The
     * <code>Properties</code> <code>Object </code> is then queried for the
     * property as documented in the prior step and processed as documented in
     * the prior step.
     * </li>
     * <li>
     * Uses the service-provider loading facilities, defined by the
     * {@link java.util.ServiceLoader} class, to attempt to locate and load an
     * implementation of the service using the {@linkplain
     *     java.util.ServiceLoader#load(java.lang.Class) default loading mechanism}:
     * the service-provider loading facility will use the {@linkplain
     *     java.lang.Thread#getContextClassLoader() current thread's context class loader}
     * to attempt to load the service. If the context class loader is null, the {@linkplain
     *     ClassLoader#getSystemClassLoader() system class loader} will be used.
     * <br>
     * In case of {@link java.util.ServiceConfigurationError service configuration error} a
     * {@link javax.xml.datatype.DatatypeConfigurationException} will be thrown.
     * </li>
     * <li>
     * The final mechanism is to attempt to instantiate the <code>Class</code>
     * specified by {@link #DATATYPEFACTORY_IMPLEMENTATION_CLASS}. Any Exception
     * thrown during the instantiation process is wrapped as a
     * {@link IllegalStateException}.
     * </li>
     * </ol>
     */
    public static class DatatypeFactory {
        /**
         * some def.
         */
        public static String DATATYPEFACTORY_IMPLEMENTATION_CLASS = "NOTHING";

        /**
         * some def.
         */
        public static String DATATYPEFACTORY_PROPERTY = "NOTHING";

        /**
         * <p>Obtain a new instance of a <code>DatatypeFactory</code>.</p>
         *
         * <p>The implementation resolution mechanisms are <a href="#DatatypeFactory.newInstance">defined</a> in this
         * <code>Class</code>'s documentation.</p>
         *
         * @return New instance of a <code>DatatypeFactory</code>
         *
         * @throws Exception If the implementation is not
         *   available or cannot be instantiated.
         *
         * @see #newInstance(String factoryClassName, ClassLoader classLoader)
         */
        public static DatatypeFactory newInstance() throws Exception {
            return null;
        }

        /**
         * <p>Obtain a new instance of a <code>DatatypeFactory</code>.</p>
         *
         * <p>The implementation resolution mechanisms are <a href="#DatatypeFactory.newInstance">defined</a> in this
         * <code>Class</code>'s documentation.</p>
         *
         * @param factoryClassName fe
         * @param classLoader fi
         * @return fo
         * @throws Exception If the implementation is not
         *   available or cannot be instantiated.
         */
        public static DatatypeFactory newInstance(String factoryClassName, ClassLoader classLoader) throws Exception {
            return null;
        }
    }

    /**
     * An activation descriptor contains the information necessary to activate
     * an object: <ul>
     * <li> the object's group identifier,
     * <li> the object's fully-qualified class name,
     * <li> the object's code location (the location of the class), a codebase
     * URL path,
     * <li> the object's restart "mode", and,
     * <li> a "marshalled" object that can contain object specific
     * initialization data. </ul>
     *
     * <p>
     * A descriptor registered with the activation system can be used to
     * recreate/activate the object specified by the descriptor. The
     * <code>MarshalledObject</code> in the object's descriptor is passed as the
     * second argument to the remote object's constructor for object to use
     * during reinitialization/activation.
     */
    public class ActivationDesc implements Serializable {}

    /**
     * The identifier for a registered activation group serves several purposes:
     * <ul>
     * <li>identifies the group uniquely within the activation system, and
     * <li>contains a reference to the group's activation system so that the
     * group can contact its activation system when necessary.</ul><p>
     *
     * The <code>ActivationGroupID</code> is returned from the call to
     * <code>ActivationSystem.registerGroup</code> and is used to identify the
     * group within the activation system. This group id is passed as one of the
     * arguments to the activation group's special constructor when an
     * activation group is created/recreated.
     */
    public class ActivationGroupID implements java.io.Serializable {}

}
