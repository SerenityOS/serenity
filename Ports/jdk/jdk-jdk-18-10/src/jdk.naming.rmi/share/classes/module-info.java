/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Provides the implementation of the RMI Java Naming provider.
 *
 * @implNote
 * The following implementation specific system properties are supported by the
 * default RMI Naming Service Provider implementation in the JDK:
 * <ul>
 *     <li>{@systemProperty jdk.jndi.object.factoriesFilter}:
 *          <br>The value of this system property defines a filter used by
 *          the JNDI runtime implementation to control the set of object factory classes which will
 *          be allowed to instantiate objects from object references returned by naming/directory systems.
 *          The factory class named by the reference instance will be matched against this filter.
 *          The filter property supports pattern-based filter syntax with the same format as
 *          {@link java.io.ObjectInputFilter.Config#createFilter(String) jdk.serialFilter}.
 *          This property can also be specified as a {@linkplain java.security.Security security property}.
 *          This property is also supported by the <a href="{@docRoot}/java.naming/module-summary.html">default
 *          LDAP Naming Service Provider</a>.
 *          <br>The default value allows any object factory class specified by the reference
 *          instance to recreate the referenced object.
 *     </li>
 * </ul>
 * @provides javax.naming.spi.InitialContextFactory
 * @moduleGraph
 * @since 9
 */
module jdk.naming.rmi {
    requires java.naming;
    requires java.rmi;

    // temporary export until NamingManager.getURLContext uses services
    exports com.sun.jndi.url.rmi to java.naming;
    exports com.sun.jndi.rmi.registry to java.rmi;

    provides javax.naming.spi.InitialContextFactory with
        com.sun.jndi.rmi.registry.RegistryContextFactory;

}
