/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.management.spi;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The PlatformMBeanProvider class defines the abstract service interface
 * that the {@link java.lang.management.ManagementFactory} will invoke to find,
 * load, and register Platform MBeans.
 *
 * ManagementFactory loads the {@linkplain ServiceLoader#loadInstalled(java.lang.Class)
 * installed providers} of this service interface and each provides the
 * {@linkplain PlatformComponent platform components} that defines MXBean
 * or DynamicMBean to be registered in the platform MBeanServer.
 *
 * A {@code PlatformMBeanProvider} will implement the {@code getPlatformComponentList()}
 * method to return the list of {@code PlatformComponents} it provides.
 */
public abstract class PlatformMBeanProvider {
    /**
     * {@code PlatformComponent} models MBeans of a management interface supported
     * by the platform.
     *
     * If a PlatformComponent models a singleton MBean, the {@link #getObjectNamePattern()
     * ObjectName pattern} must be the {@link
     * javax.management.ObjectName#getCanonicalName() canonical name} of that
     * singleton MBean. Otherwise, it must be an ObjectName pattern
     * that can be used to query the MBeans for this
     * PlatformComponent registered in a {@code MBeanServer}.
     * <br>
     * The {@link #getObjectNamePattern() ObjectName pattern} serves as a unique
     * key for identifying the instance of PlatformComponent. It is thus illegal
     * for a given {@link PlatformMBeanProvider} to export several instance of
     * PlatformComponent with the same
     * {@link #getObjectNamePattern() ObjectName pattern} string.
     * <br>
     * If two different provider instances export a PlatformComponent for the
     * same ObjectName pattern, only the PlatformComponent instance of the first
     * provider will be taken into account.
     *
     * @param <T> The higher level interface for which the MBeans modeled by
     * this object should be recognized. For instance, for the {@link
     *        java.lang.management.ManagementFactory#getOperatingSystemMXBean()
     *        Operating System MXBean}, this should be {@link
     *        java.lang.management.OperatingSystemMXBean
     *        java.lang.management.OperatingSystemMXBean}.
     */
    public interface PlatformComponent<T> {
        /**
         * Returns the names of the management interfaces implemented by the
         * MBeans modeled by this {@code PlatformComponent}.
         *
         * @implNote
         * When {@link java.lang.management.ManagementFactory#getPlatformMXBean(java.lang.Class)
         * ManagementFactory.getPlatformMXBean(mxbeanInterface)} or {@link
         * java.lang.management.ManagementFactory#getPlatformMXBeans(java.lang.Class)
         * ManagementFactory.getPlatformMXBeans(mxbeanInterface)} are invoked,
         * this PlatformComponent instance will match only if the name of the
         * given {@code mxbeanInterface} is found in this list.
         *
         * @return the names of the management interfaces exported by the MBeans
         * modeled by this object.
         */
        public Set<String> mbeanInterfaceNames();

        /**
         * A map from ObjectName string to the MBean instance this
         * {@code PlatformComponent} creates.
         *
         * @implNote
         * If {@link #shouldRegister()} is {@code true}, this method
         * will be called when the {@link java.lang.management.ManagementFactory
         * #getPlatformMBeanServer() Platform MBeanServer} is initialized.
         * By default, this method will also be called by {@link
         * #getMBeans(java.lang.Class)}, when {@link
         * java.lang.management.ManagementFactory#getPlatformMXBean(java.lang.Class)
         * ManagementFactory.getPlatformMXBean(mxbeanInterface)} or {@link
         * java.lang.management.ManagementFactory#getPlatformMXBeans(java.lang.Class)
         * ManagementFactory.getPlatformMXBeans(mxbeanInterface)} are invoked,
         * and when the name of the given {@code mxbeanInterface} is contained
         * in the names of management interfaces returned by {@link
         * #mbeanInterfaceNames()}.
         *
         * @return A map with, for each MBean, the ObjectName string as key
         *         and the MBean as value.
         */
        public Map<String, T> nameToMBeanMap();

        /**
         * An ObjectName pattern uniquely identifies the MBeans
         * modeled by this {@code PlatformComponent}.
         * If this instance models a singleton MBean, this must be
         * the {@link
         * javax.management.ObjectName#getCanonicalName() canonical name}
         * of that singleton MBean.
         *
         * @return An ObjectName pattern uniquely identifies the MBeans
         * modeled by this instance.
         */
        public String getObjectNamePattern();

        /**
         * Returns {@code true} if this {@code PlatformComponent} models
         * a singleton MBean. By default, {@code true} is assumed.
         *
         * @return {@code true} if this instance models a singleton MBean.
         */
        public default boolean isSingleton() {
            return true;
        }

        /**
         * Returns {@code true} if the MBeans modeled by this {@code PlatformComponent}
         * should automatically be registered in the {@link
         * java.lang.management.ManagementFactory#getPlatformMBeanServer()
         * Platform MBeanServer}.  By default, {@code true} is assumed.
         *
         * @return {@code true} if the MBeans modeled by this instance should
         * automatically be registered in the Platform MBeanServer.
         */
        public default boolean shouldRegister() {
            return true;
        }

        /**
         * The set of interfaces implemented by the MBeans modeled
         * by this {@code PlatformComponent}.
         *
         * @implNote
         * {@link java.lang.management.ManagementFactory#getPlatformManagementInterfaces()
         * ManagementFactory.getPlatformManagementInterfaces()} calls this
         * method to find the management interfaces supported by the platform.
         *
         * @return The set of interfaces implemented by the MBeans modeled
         *   by this instance
         */
        public Set<Class<? extends T>> mbeanInterfaces();

        /**
         * Return the list of MBeans that implement the given {@code mbeanIntf}
         * modeled by this {@code PlatformComponent}. This method returns an
         * empty list if no MBean implements the given {@code mbeanIntf}.
         *
         * @implNote This method will be called when {@link
         * java.lang.management.ManagementFactory#getPlatformMXBean(java.lang.Class)
         * ManagementFactory.getPlatformMXBean(mbeanIntf)} or {@link
         * java.lang.management.ManagementFactory#getPlatformMXBeans(java.lang.Class)
         * ManagementFactory.getPlatformMXBeans(mbeanIntf)} are invoked.
         * By default it first checks whether the specified {@code mbeanIntf}
         * name is contained in the returned list from the {@link #mbeanInterfaceNames()}
         * method. If yes, it proceeds and calls
         * {@link #mbeans().values()} and filters out all
         * MBeans which are not instances of the given {@code mbeanIntf}.
         * Otherwise, it returns an empty list.
         *
         * @param mbeanIntf A management interface.
         * @return A (possibly empty) list of MBeans implementing the given
         *         {@code mbeanIntf}.
         */
        public default <I> List<? extends I> getMBeans(Class<I> mbeanIntf) {
            List<I> list;

            if (!mbeanInterfaceNames().contains(mbeanIntf.getName())) {
                list = Collections.emptyList();
            } else {
                list = nameToMBeanMap().values().stream()
                        .filter(mbeanIntf::isInstance)
                        .map(mbeanIntf::cast)
                        .collect(Collectors.toList());
            }
            return list;
        }
    }

    /**
     * Instantiates a new PlatformMBeanProvider.
     *
     * @throws SecurityException if the subclass (and calling code) does not
     *    have {@code RuntimePermission("sun.management.spi.PlatformMBeanProvider.subclass")}
     */
    protected PlatformMBeanProvider () {
        this(checkSubclassPermission());
    }

    private PlatformMBeanProvider(Void unused) {
    }

    /**
     * Returns a list of PlatformComponent instances describing the Platform
     * MBeans provided by this provider.
     *
     * @return a list of PlatformComponent instances describing the Platform
     * MBeans provided by this provider.
     */
    public abstract List<PlatformComponent<?>> getPlatformComponentList();

    private static Void checkSubclassPermission() {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission(PlatformMBeanProvider.class.getName()+".subclass"));
        }
        return null;
    }
}
