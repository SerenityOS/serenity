/*
 * Copyright (c) 2005, 2008, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jmx.mbeanserver;

import static com.sun.jmx.mbeanserver.Util.*;

import java.util.Iterator;
import java.util.Set;

import javax.management.InstanceAlreadyExistsException;
import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

/**
 * Base class for MXBeans.
 *
 * @since 1.6
 */
public class MXBeanSupport extends MBeanSupport<ConvertingMethod> {

    /**
       <p>Construct an MXBean that wraps the given resource using the
       given MXBean interface.</p>

       @param resource the underlying resource for the new MXBean.

       @param mxbeanInterface the interface to be used to determine
       the MXBean's management interface.

       @param <T> a type parameter that allows the compiler to check
       that {@code resource} implements {@code mxbeanInterface},
       provided that {@code mxbeanInterface} is a class constant like
       {@code SomeMXBean.class}.

       @throws IllegalArgumentException if {@code resource} is null or
       if it does not implement the class {@code mxbeanInterface} or if
       that class is not a valid MXBean interface.
    */
    public <T> MXBeanSupport(T resource, Class<T> mxbeanInterface)
            throws NotCompliantMBeanException {
        super(resource, mxbeanInterface);
    }

    @Override
    MBeanIntrospector<ConvertingMethod> getMBeanIntrospector() {
        return MXBeanIntrospector.getInstance();
    }

    @Override
    Object getCookie() {
        return mxbeanLookup;
    }

    static <T> Class<? super T> findMXBeanInterface(Class<T> resourceClass) {
        if (resourceClass == null)
            throw new IllegalArgumentException("Null resource class");
        final Set<Class<?>> intfs = transitiveInterfaces(resourceClass);
        final Set<Class<?>> candidates = newSet();
        for (Class<?> intf : intfs) {
            if (JMX.isMXBeanInterface(intf))
                candidates.add(intf);
        }
    reduce:
        while (candidates.size() > 1) {
            for (Class<?> intf : candidates) {
                for (Iterator<Class<?>> it = candidates.iterator(); it.hasNext();
                    ) {
                    final Class<?> intf2 = it.next();
                    if (intf != intf2 && intf2.isAssignableFrom(intf)) {
                        it.remove();
                        continue reduce;
                    }
                }
            }
            final String msg =
                "Class " + resourceClass.getName() + " implements more than " +
                "one MXBean interface: " + candidates;
            throw new IllegalArgumentException(msg);
        }
        if (candidates.iterator().hasNext()) {
            return Util.cast(candidates.iterator().next());
        } else {
            final String msg =
                "Class " + resourceClass.getName() +
                " is not a JMX compliant MXBean";
            throw new IllegalArgumentException(msg);
        }
    }

    /* Return all interfaces inherited by this class, directly or
     * indirectly through the parent class and interfaces.
     */
    private static Set<Class<?>> transitiveInterfaces(Class<?> c) {
        Set<Class<?>> set = newSet();
        transitiveInterfaces(c, set);
        return set;
    }
    private static void transitiveInterfaces(Class<?> c, Set<Class<?>> intfs) {
        if (c == null)
            return;
        if (c.isInterface())
            intfs.add(c);
        transitiveInterfaces(c.getSuperclass(), intfs);
        for (Class<?> sup : c.getInterfaces())
            transitiveInterfaces(sup, intfs);
    }

    /*
     * The sequence of events for tracking inter-MXBean references is
     * relatively complicated.  We use the magical preRegister2 method
     * which the MBeanServer knows about.  The steps during registration
     * are:
     * (1) Call user preRegister, if any.  If exception, abandon.
     * (2) Call preRegister2 and hence this register method.  If exception,
     * call postRegister(false) and abandon.
     * (3) Try to register the MBean.  If exception, call registerFailed()
     * which will call the unregister method.  (Also call postRegister(false).)
     * (4) If we get this far, we can call postRegister(true).
     *
     * When we are wrapped in an instance of javax.management.StandardMBean,
     * things are simpler.  That class calls this method from its preRegister,
     * and propagates any exception.  There is no user preRegister in this case.
     * If this method succeeds but registration subsequently fails,
     * StandardMBean calls unregister from its postRegister(false) method.
     */
    @Override
    public void register(MBeanServer server, ObjectName name)
            throws InstanceAlreadyExistsException {
        if (name == null)
            throw new IllegalArgumentException("Null object name");
        // eventually we could have some logic to supply a default name

        synchronized (lock) {
            this.mxbeanLookup = MXBeanLookup.lookupFor(server);
            this.mxbeanLookup.addReference(name, getResource());
            this.objectName = name;
        }
    }

    @Override
    public void unregister() {
        synchronized (lock) {
            if (mxbeanLookup != null) {
                if (mxbeanLookup.removeReference(objectName, getResource()))
                    objectName = null;
            }
        }
    }
    private final Object lock = new Object(); // for mxbeanLookup and objectName

    private MXBeanLookup mxbeanLookup;
    private ObjectName objectName;
}
